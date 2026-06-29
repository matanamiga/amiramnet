"""The brain's action space + parsing of the model's tool-calls.

The LAM understands the screen visually and replies with a single JSON object:

    {"thought": "...", "action": {"type": "<name>", ...}}

Executable actions map to PC-gate protocol actions; two control actions
(``screenshot`` and ``done``) are handled by the agent itself.
"""

from __future__ import annotations

import json
import re

from ..common.protocol import (
    Action,
    ClickAction,
    KeyAction,
    MoveAction,
    ScrollAction,
    TypeAction,
    WaitAction,
)

TOOLS_DESCRIPTION = """\
You may use exactly one action per step. Reply with ONLY a JSON object:
{"thought": "<short reasoning>", "action": {"type": "<name>", ...}}

Actions that operate the computer:
- {"type": "click", "x": <int>, "y": <int>, "button": "left|right|middle", "clicks": <int>}
- {"type": "move", "x": <int>, "y": <int>}
- {"type": "type", "text": "<text to type>"}
- {"type": "key", "keys": ["ctrl", "c"]}            # a key or hotkey chord
- {"type": "scroll", "dx": <int>, "dy": <int>}
- {"type": "wait", "seconds": <number>}

Control actions:
- {"type": "screenshot"}                            # look at the screen again
- {"type": "done", "summary": "<what you accomplished>"}

Coordinates are pixels on the screenshot you are shown. Think step by step:
look at the screen, decide the single best next action toward the goal, and
emit it. When the goal is achieved, use "done".
"""

# Control action names handled by the agent rather than the PC gate.
CONTROL_ACTIONS = {"screenshot", "done"}


def parse_decision(text: str) -> dict:
    """Extract the JSON decision object from raw model text.

    Tolerates ```json fences and surrounding prose by scanning for the first
    balanced ``{...}`` block.
    """
    text = text.strip()

    fenced = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", text, re.DOTALL)
    if fenced:
        return json.loads(fenced.group(1))

    start = text.find("{")
    if start == -1:
        raise ValueError("no JSON object found in model output")

    depth = 0
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return json.loads(text[start : i + 1])
    raise ValueError("unbalanced JSON object in model output")


def to_protocol_action(action: dict) -> Action | None:
    """Convert an executable action dict to a protocol model.

    Returns ``None`` for control actions ('done', 'screenshot') and unknown
    types so the agent can route them itself.
    """
    a = dict(action)
    t = a.pop("type", None)
    if t in CONTROL_ACTIONS or t is None:
        return None
    builders = {
        "click": ClickAction,
        "move": MoveAction,
        "type": TypeAction,
        "key": KeyAction,
        "scroll": ScrollAction,
        "wait": WaitAction,
    }
    builder = builders.get(t)
    if builder is None:
        return None
    return builder(**a)
