"""The PC "GATE" — a thin FastAPI app the phone brain connects to.

It holds ZERO intelligence: it only reports info, hands over screenshots, and
executes the exact action / command the brain decides. All reasoning lives on
the phone.
"""

from __future__ import annotations

import platform
import socket

from fastapi import FastAPI

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
    app = FastAPI(title="AMIRANET Gate", version=__version__)

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

    @app.get("/screenshot", response_model=Screenshot)
    def screenshot(scale: float = 1.0) -> Screenshot:
        return screen.capture(scale=scale)

    @app.post("/action", response_model=ActionResult)
    def action(envelope: ActionEnvelope) -> ActionResult:
        return actions.execute(envelope.action)

    @app.post("/command", response_model=CommandResult)
    def command(req: CommandRequest) -> CommandResult:
        return actions.run_command(req, allow=config.allow_commands)

    return app


# Module-level default app for `uvicorn amiranet.pc.worker:app`.
app = create_app()
