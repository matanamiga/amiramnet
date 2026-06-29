"""The LAM loop: observe the screen, decide an action, act, repeat.

This is the brain. It runs on the phone next to a local vision model (Ollama /
Qwen2.5-VL) and drives the PC purely from what it *sees* — no app APIs, no
cloud. The PC gate only supplies screenshots and executes actions.
"""

from __future__ import annotations

import logging

from ..common.config import PhoneConfig
from ..llm import LLMProvider, build_provider
from . import tools
from .memory import SlidingMemory
from .pc_client import PcClient

log = logging.getLogger("phantom.agent")

SYSTEM_PROMPT = """\
You are PHANTOM, a local Large Action Model (LAM) running on a phone. You
operate a real computer on the user's behalf by looking at screenshots of its
screen and issuing UI actions — exactly like a person using a mouse and
keyboard. You do not call app APIs; you act by sight. Work autonomously toward
the user's goal, one action at a time, and recover when an action does not have
the effect you expected.

{tools}
"""


class Agent:
    def __init__(
        self,
        config: PhoneConfig,
        llm: LLMProvider | None = None,
        pc: PcClient | None = None,
    ) -> None:
        self.config = config
        self.pc = pc or PcClient(config.pc_url, config.request_timeout, config.token)
        self.memory = SlidingMemory(config.memory_window)
        if llm is not None:
            self.llm = llm
        elif config.provider == "ollama":
            self.llm = build_provider(
                "ollama",
                url=config.ollama_url,
                model=config.model,
                num_threads=config.num_threads,
                timeout=config.request_timeout,
            )
        else:
            self.llm = build_provider(config.provider)
        self._system = SYSTEM_PROMPT.format(tools=tools.TOOLS_DESCRIPTION)

    def _user_prompt(self, goal: str) -> str:
        return (
            f"GOAL: {goal}\n\n"
            f"Recent steps (most recent last):\n{self.memory.render()}\n\n"
            "Here is the current screen. Decide the single best next action."
        )

    def run(self, goal: str) -> str:
        """Drive the PC toward ``goal``; return a final summary."""
        for step in range(1, self.config.max_steps + 1):
            shot = self.pc.screenshot(self.config.screenshot_scale)
            try:
                resp = self.llm.generate(
                    self._system, self._user_prompt(goal), images_b64=[shot.image_b64]
                )
                decision = tools.parse_decision(resp.text)
            except Exception as exc:  # noqa: BLE001 — let the brain self-correct
                log.warning("step %d: bad decision (%s)", step, exc)
                self.memory.add("(parse error)", {}, f"error: {exc}")
                continue

            thought = str(decision.get("thought", ""))
            action = decision.get("action") or {}
            atype = action.get("type")
            log.info("step %d: %s -> %s", step, thought, atype)

            if atype == "done":
                summary = str(action.get("summary", "done"))
                self.memory.add(thought, action, "done")
                return summary
            if atype == "screenshot":
                self.memory.add(thought, action, "re-observed")
                continue

            proto = tools.to_protocol_action(action)
            if proto is None:
                self.memory.add(thought, action, f"unknown action type: {atype}")
                continue

            result = self.pc.do_action(proto)
            self.memory.add(thought, action, f"ok={result.ok} {result.detail}")

        return f"stopped after {self.config.max_steps} steps without an explicit 'done'"
