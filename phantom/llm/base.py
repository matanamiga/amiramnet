"""Provider interface for the vision-capable reasoning model."""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class LLMResponse:
    """A single completion from the model."""

    text: str
    raw: dict = field(default_factory=dict)


class LLMProvider:
    """Abstract base for a vision LLM.

    Implementations must support a multimodal prompt: a system instruction, a
    user message, and zero or more base64-encoded images (the PC screenshots).
    """

    def generate(
        self,
        system: str,
        prompt: str,
        images_b64: list[str] | None = None,
    ) -> LLMResponse:
        raise NotImplementedError
