"""End-to-end test: a real Agent + PcClient driving a live gate over HTTP.

Unlike the in-process FastAPI TestClient tests, this starts a real uvicorn
server on a socket so the phone's `requests`-based PcClient, the token header,
and the full agent loop are all exercised against a live gate. Screen capture
and action execution are stubbed (headless CI has no display), but everything
between the brain's decision and the gate's response is the real code path.
"""

from __future__ import annotations

import socket
import threading
import time

import pytest

requests = pytest.importorskip("requests")
pytest.importorskip("uvicorn")
pytest.importorskip("fastapi")

from phantom.common.config import PcConfig, PhoneConfig
from phantom.common.protocol import ActionResult, Screenshot
from phantom.llm import MockProvider
from phantom.phone.agent import Agent


def _free_port() -> int:
    s = socket.socket()
    s.bind(("127.0.0.1", 0))
    port = s.getsockname()[1]
    s.close()
    return port


def test_agent_drives_live_gate(monkeypatch):
    import uvicorn

    from phantom.pc import actions, screen

    # Stub the bits that need a real display / input device.
    recorded: list = []
    monkeypatch.setattr(
        screen, "capture",
        lambda scale=1.0: Screenshot(image_b64="aGVsbG8=", format="png", width=8, height=8),
    )
    monkeypatch.setattr(
        actions, "execute",
        lambda action: recorded.append(action) or ActionResult(ok=True, detail="stubbed"),
    )

    from phantom.pc.worker import create_app

    port = _free_port()
    app = create_app(PcConfig(token="tok-e2e", allow_commands=False))
    server = uvicorn.Server(uvicorn.Config(app, host="127.0.0.1", port=port, log_level="warning"))
    thread = threading.Thread(target=server.run, daemon=True)
    thread.start()
    try:
        base = f"http://127.0.0.1:{port}"
        # Wait for readiness.
        for _ in range(100):
            try:
                if requests.get(f"{base}/health", timeout=0.5).status_code == 200:
                    break
            except requests.RequestException:
                time.sleep(0.05)
        else:
            pytest.fail("gate did not start")

        # The gate must reject an unauthenticated sensitive call.
        assert requests.get(f"{base}/screenshot", timeout=2).status_code == 401

        # Drive the real agent (mock brain: type a greeting, then done) against it.
        config = PhoneConfig(pc_url=base, token="tok-e2e", provider="mock", max_steps=5)
        agent = Agent(config, llm=MockProvider())
        summary = agent.run("greet the user")

        assert "greeting" in summary.lower()
        # The 'type' action really traversed HTTP + token + the gate to actions.execute.
        assert any(getattr(a, "type", None) == "type" for a in recorded)
    finally:
        server.should_exit = True
        thread.join(timeout=5)
