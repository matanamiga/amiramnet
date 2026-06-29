"""Deterministic provider for tests and offline demos.

Returns scripted completions in order, so the agent loop can be exercised with
no Ollama, no phone, and no PC. Used by the test suite and by
``python -m phantom.phone --demo``.
"""

from __future__ import annotations

from phantom.llm.base import LLMProvider, LLMResponse


class MockProvider(LLMProvider):
    def __init__(self, scripted: list[str] | None = None) -> None:
        # Default script: type a greeting, then declare the task done.
        self.scripted = scripted or [
            '{"thought": "I will type a greeting.", '
            '"action": {"type": "type", "text": "hello from phantom"}}',
            '{"thought": "Task complete.", '
            '"action": {"type": "done", "summary": "Typed the greeting."}}',
        ]
        self._i = 0

    def generate(
        self,
        system: str,
        prompt: str,
        images_b64: list[str] | None = None,
    ) -> LLMResponse:
        if self._i < len(self.scripted):
            text = self.scripted[self._i]
            self._i += 1
        else:
            text = '{"thought": "No more steps.", "action": {"type": "done", "summary": "stop"}}'
        return LLMResponse(text=text, raw={"mock": True})
