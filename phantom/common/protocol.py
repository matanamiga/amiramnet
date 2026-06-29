"""Wire protocol shared by the phone ("brain") and the PC ("body").

Both sides import these models so the HTTP contract stays in one place. The PC
worker exposes the endpoints; the phone's ``PcClient`` calls them.

Endpoints (all JSON over HTTP):

    GET  /info        -> PcInfo
    GET  /screenshot  -> Screenshot          (query: scale: float = 1.0)
    POST /action      -> ActionResult        (body: Action)
    POST /command     -> CommandResult        (body: CommandRequest)

The ``Action`` union is the set of things the brain can ask the body to do.
Each variant is discriminated by its ``type`` field so it round-trips cleanly
through JSON and through the LLM tool-calling layer.
"""

from __future__ import annotations

from typing import Literal, Union

from pydantic import BaseModel, Field

# --------------------------------------------------------------------------- #
# Device / screen description
# --------------------------------------------------------------------------- #


class PcInfo(BaseModel):
    """Static description of the PC the brain is driving."""

    hostname: str
    platform: str
    screen_width: int
    screen_height: int
    agent_version: str


class Screenshot(BaseModel):
    """A captured frame of the PC screen, base64-encoded for transport."""

    image_b64: str = Field(..., description="Base64-encoded image bytes.")
    format: str = Field("png", description="Image format, e.g. 'png'.")
    width: int
    height: int


# --------------------------------------------------------------------------- #
# Actions the brain can ask the body to perform
# --------------------------------------------------------------------------- #


class ClickAction(BaseModel):
    type: Literal["click"] = "click"
    x: int
    y: int
    button: Literal["left", "right", "middle"] = "left"
    clicks: int = 1


class MoveAction(BaseModel):
    type: Literal["move"] = "move"
    x: int
    y: int


class TypeAction(BaseModel):
    type: Literal["type"] = "type"
    text: str


class KeyAction(BaseModel):
    """Press a key or a hotkey chord, e.g. ['ctrl', 'c'] or ['enter']."""

    type: Literal["key"] = "key"
    keys: list[str]


class ScrollAction(BaseModel):
    type: Literal["scroll"] = "scroll"
    dx: int = 0
    dy: int = 0


class WaitAction(BaseModel):
    type: Literal["wait"] = "wait"
    seconds: float = 1.0


# Discriminated union of every executable action.
Action = Union[
    ClickAction,
    MoveAction,
    TypeAction,
    KeyAction,
    ScrollAction,
    WaitAction,
]


class ActionEnvelope(BaseModel):
    """Wrapper used as the POST /action body (gives pydantic a discriminator)."""

    action: Action = Field(..., discriminator="type")


class ActionResult(BaseModel):
    ok: bool
    detail: str = ""


# --------------------------------------------------------------------------- #
# Arbitrary shell commands
# --------------------------------------------------------------------------- #


class CommandRequest(BaseModel):
    command: str
    timeout: float = 30.0


class CommandResult(BaseModel):
    ok: bool
    stdout: str = ""
    stderr: str = ""
    exit_code: int = 0
