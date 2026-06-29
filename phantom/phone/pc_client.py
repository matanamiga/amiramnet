"""HTTP client the brain uses to reach the PC gate."""

from __future__ import annotations

import requests

from ..common.protocol import (
    Action,
    ActionEnvelope,
    ActionResult,
    CommandResult,
    PcInfo,
    Screenshot,
)


class PcClient:
    """Thin wrapper over the PC gate's HTTP endpoints."""

    def __init__(self, base_url: str, timeout: float = 120.0) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def _get(self, path: str, **params) -> dict:
        r = requests.get(f"{self.base_url}{path}", params=params, timeout=self.timeout)
        if r.status_code != 200:
            raise RuntimeError(f"GET {path} -> {r.status_code}: {r.text[:200]}")
        return r.json()

    def _post(self, path: str, body: dict) -> dict:
        r = requests.post(f"{self.base_url}{path}", json=body, timeout=self.timeout)
        if r.status_code != 200:
            raise RuntimeError(f"POST {path} -> {r.status_code}: {r.text[:200]}")
        return r.json()

    def info(self) -> PcInfo:
        return PcInfo(**self._get("/info"))

    def screenshot(self, scale: float = 1.0) -> Screenshot:
        return Screenshot(**self._get("/screenshot", scale=scale))

    def do_action(self, action: Action) -> ActionResult:
        body = ActionEnvelope(action=action).model_dump()
        return ActionResult(**self._post("/action", body))

    def run_command(self, command: str, timeout: float = 30.0) -> CommandResult:
        return CommandResult(**self._post("/command", {"command": command, "timeout": timeout}))
