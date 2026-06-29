"""Tests for the PC gate — all run headlessly (no real display / pyautogui)."""

from __future__ import annotations

import sys
import types

import pytest

from phantom.common.config import PcConfig
from phantom.common.protocol import (
    ClickAction,
    CommandRequest,
    KeyAction,
    TypeAction,
    WaitAction,
)
from phantom.pc import actions


def _fake_pyautogui(calls: list):
    mod = types.SimpleNamespace(
        FAILSAFE=True,
        click=lambda **kw: calls.append(("click", kw)),
        moveTo=lambda x, y: calls.append(("moveTo", (x, y))),
        write=lambda text, interval=0.0: calls.append(("write", text)),
        press=lambda k: calls.append(("press", k)),
        hotkey=lambda *keys: calls.append(("hotkey", keys)),
        scroll=lambda n: calls.append(("scroll", n)),
    )
    return mod


def test_wait_action_needs_no_display():
    res = actions.execute(WaitAction(seconds=0))
    assert res.ok


def test_actions_dispatch(monkeypatch):
    calls: list = []
    monkeypatch.setitem(sys.modules, "pyautogui", _fake_pyautogui(calls))

    assert actions.execute(ClickAction(x=10, y=20, button="left", clicks=2)).ok
    assert actions.execute(TypeAction(text="hi")).ok
    assert actions.execute(KeyAction(keys=["ctrl", "c"])).ok
    assert actions.execute(KeyAction(keys=["enter"])).ok

    names = [c[0] for c in calls]
    assert names == ["click", "write", "hotkey", "press"]
    assert calls[0][1]["clicks"] == 2


def test_action_failure_is_graceful(monkeypatch):
    boom = types.SimpleNamespace(
        FAILSAFE=False,
        click=lambda **kw: (_ for _ in ()).throw(RuntimeError("no display")),
    )
    monkeypatch.setitem(sys.modules, "pyautogui", boom)
    res = actions.execute(ClickAction(x=1, y=1))
    assert not res.ok
    assert "RuntimeError" in res.detail


def test_run_command_disabled():
    res = actions.run_command(CommandRequest(command="echo hi"), allow=False)
    assert not res.ok
    assert "disabled" in res.stderr


def test_run_command_allowed():
    res = actions.run_command(
        CommandRequest(command=f'{sys.executable} -c "print(\'phantom-ok\')"'),
        allow=True,
    )
    assert res.ok
    assert "phantom-ok" in res.stdout


def test_worker_endpoints():
    fastapi_testclient = pytest.importorskip("fastapi.testclient")
    from phantom.pc.worker import create_app

    client = fastapi_testclient.TestClient(
        create_app(PcConfig(allow_commands=False, token="testtok"))
    )
    auth = {"Authorization": "Bearer testtok"}

    # Open endpoints need no token.
    assert client.get("/health").json() == {"ok": True}
    info = client.get("/info").json()
    assert set(info) >= {"hostname", "platform", "screen_width", "agent_version"}

    body = {"action": {"type": "wait", "seconds": 0}}
    res = client.post("/action", json=body, headers=auth).json()
    assert res["ok"] is True

    cmd = client.post("/command", json={"command": "echo hi", "timeout": 5}, headers=auth).json()
    assert cmd["ok"] is False  # commands disabled in this config


def test_worker_requires_token():
    fastapi_testclient = pytest.importorskip("fastapi.testclient")
    from phantom.pc.worker import create_app

    client = fastapi_testclient.TestClient(create_app(PcConfig(token="secret")))

    # No / wrong token is rejected on sensitive endpoints.
    assert client.post("/action", json={"action": {"type": "wait", "seconds": 0}}).status_code == 401
    bad = {"Authorization": "Bearer nope"}
    assert client.post("/action", json={"action": {"type": "wait", "seconds": 0}}, headers=bad).status_code == 401
    # Open endpoints stay reachable.
    assert client.get("/health").status_code == 200


def test_gate_generates_token_when_unset():
    from phantom.pc.worker import create_app

    app = create_app(PcConfig(token=""))
    assert app.state.config.token  # a token was generated


def test_pairing_roundtrip():
    from phantom.pc import pairing

    code = pairing.encode_pairing("http://192.168.1.5:8765", "abc123", "qwen2.5-vl:7b")
    assert code.startswith("PHANTOM:")
    data = pairing.decode_pairing(code)
    assert data == {"url": "http://192.168.1.5:8765", "token": "abc123", "model": "qwen2.5-vl:7b"}

    with pytest.raises(ValueError):
        pairing.decode_pairing("not-a-valid-code!!")
