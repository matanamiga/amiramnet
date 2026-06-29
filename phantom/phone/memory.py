"""Sliding-window memory.

Keeps only the last N steps in the prompt so context (and phone RAM) stay
bounded as a task runs long — the documented constraint for on-device agents.
"""

from __future__ import annotations

import json
from collections import deque
from dataclasses import dataclass


@dataclass
class Step:
    thought: str
    action: dict
    result: str


class SlidingMemory:
    def __init__(self, window: int = 4) -> None:
        self.window = max(1, window)
        self._steps: deque[Step] = deque(maxlen=self.window)

    def add(self, thought: str, action: dict, result: str) -> None:
        self._steps.append(Step(thought=thought, action=action, result=result))

    def render(self) -> str:
        """Compact textual history of the most recent steps for the prompt."""
        if not self._steps:
            return "(no actions yet)"
        lines = []
        for i, s in enumerate(self._steps, 1):
            lines.append(
                f"{i}. thought: {s.thought}\n"
                f"   action: {json.dumps(s.action, ensure_ascii=False)}\n"
                f"   result: {s.result}"
            )
        return "\n".join(lines)

    def __len__(self) -> int:
        return len(self._steps)
