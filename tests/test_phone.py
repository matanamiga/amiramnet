"""Tests for the phone brain — no network, no Ollama, no PC."""

from __future__ import annotations

from amiranet.common.config import PhoneConfig
from amiranet.common.protocol import Action, ActionResult, Screenshot
from amiranet.llm import MockProvider
from amiranet.phone import tools
from amiranet.phone.agent import Agent
from amiranet.phone.memory import SlidingMemory


# --------------------------- memory --------------------------- #


def test_sliding_window_evicts_old():
    mem = SlidingMemory(window=2)
    mem.add("t1", {"type": "click"}, "ok")
    mem.add("t2", {"type": "type"}, "ok")
    mem.add("t3", {"type": "done"}, "done")
    assert len(mem) == 2
    rendered = mem.render()
    assert "t1" not in rendered
    assert "t2" in rendered and "t3" in rendered


# --------------------------- tools --------------------------- #


def test_parse_decision_fenced():
    text = 'reasoning...\n```json\n{"thought": "go", "action": {"type": "wait", "seconds": 1}}\n```'
    d = tools.parse_decision(text)
    assert d["action"]["type"] == "wait"


def test_parse_decision_inline_prose():
    text = 'Sure! {"thought": "x", "action": {"type": "type", "text": "{nested}"}} done'
    d = tools.parse_decision(text)
    assert d["action"]["text"] == "{nested}"


def test_to_protocol_action_maps_and_skips():
    assert isinstance(tools.to_protocol_action({"type": "click", "x": 1, "y": 2}), Action.__args__)
    assert isinstance(tools.to_protocol_action({"type": "type", "text": "hi"}), Action.__args__)
    assert tools.to_protocol_action({"type": "done", "summary": "s"}) is None
    assert tools.to_protocol_action({"type": "screenshot"}) is None
    assert tools.to_protocol_action({"type": "bogus"}) is None


# --------------------------- agent loop --------------------------- #


class _FakePc:
    def __init__(self) -> None:
        self.actions: list = []

    def screenshot(self, scale: float = 1.0) -> Screenshot:
        return Screenshot(image_b64="AA==", format="png", width=2, height=2)

    def do_action(self, action) -> ActionResult:
        self.actions.append(action)
        return ActionResult(ok=True, detail="ok")


def test_agent_loop_types_then_done():
    pc = _FakePc()
    agent = Agent(PhoneConfig(max_steps=5), llm=MockProvider(), pc=pc)
    summary = agent.run("type a greeting")
    assert "greeting" in summary.lower()
    # MockProvider's default script does one 'type' then 'done'.
    assert len(pc.actions) == 1
    assert pc.actions[0].type == "type"


def test_agent_respects_max_steps():
    # A model that never says 'done' must stop at max_steps.
    never_done = MockProvider(
        scripted=['{"thought": "again", "action": {"type": "wait", "seconds": 0}}'] * 50
    )
    pc = _FakePc()
    agent = Agent(PhoneConfig(max_steps=3), llm=never_done, pc=pc)
    summary = agent.run("loop forever")
    assert "stopped after 3 steps" in summary
    assert len(pc.actions) == 3
