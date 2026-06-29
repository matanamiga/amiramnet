"""The PC "GATE" — a thin FastAPI app the phone brain connects to.

It holds ZERO intelligence: it only reports info, hands over screenshots, and
executes the exact action / command the brain decides. All reasoning lives on
the phone.
"""

from __future__ import annotations

import platform
import secrets
import socket

from fastapi import Depends, FastAPI, Header, HTTPException

from .. import __version__
from ..common.config import PcConfig
from ..common.protocol import (
    ActionEnvelope,
    ActionResult,
    CommandRequest,
    CommandResult,
    PcInfo,
    Screenshot,
)
from . import actions, screen


def create_app(config: PcConfig | None = None) -> FastAPI:
    config = config or PcConfig()
    # No token configured -> generate one so the gate is never unprotected.
    if not config.token:
        config.token = secrets.token_urlsafe(16)
    app = FastAPI(title="PHANTOM Gate", version=__version__)
    app.state.config = config

    def require_token(authorization: str | None = Header(default=None)) -> None:
        """Reject sensitive calls lacking a matching bearer token."""
        expected = f"Bearer {config.token}"
        if not authorization or not secrets.compare_digest(authorization, expected):
            raise HTTPException(status_code=401, detail="invalid or missing token")

    @app.get("/health")
    def health() -> dict:
        return {"ok": True}

    @app.get("/info", response_model=PcInfo)
    def info() -> PcInfo:
        try:
            w, h = screen.screen_size()
        except Exception:  # noqa: BLE001 — headless / no display
            w, h = 0, 0
        return PcInfo(
            hostname=socket.gethostname(),
            platform=platform.system(),
            screen_width=w,
            screen_height=h,
            agent_version=__version__,
        )

    @app.get("/screenshot", response_model=Screenshot, dependencies=[Depends(require_token)])
    def screenshot(scale: float = 1.0) -> Screenshot:
        return screen.capture(scale=scale)

    @app.post("/action", response_model=ActionResult, dependencies=[Depends(require_token)])
    def action(envelope: ActionEnvelope) -> ActionResult:
        return actions.execute(envelope.action)

    @app.post("/command", response_model=CommandResult, dependencies=[Depends(require_token)])
    def command(req: CommandRequest) -> CommandResult:
        return actions.run_command(req, allow=config.allow_commands)

    return app


# Module-level default app for `uvicorn phantom.pc.worker:app`.
app = create_app()
