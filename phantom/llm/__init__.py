"""Pluggable LLM providers for the agent brain.

The agent only depends on the :class:`LLMProvider` interface, so the reasoning
engine can be swapped (local Ollama by default) without touching agent logic.
"""

from phantom.llm.base import LLMProvider, LLMResponse
from phantom.llm.mock_provider import MockProvider
from phantom.llm.ollama_provider import OllamaProvider

__all__ = ["LLMProvider", "LLMResponse", "OllamaProvider", "MockProvider"]


def build_provider(name: str, **kwargs) -> LLMProvider:
    """Factory: return a provider by name ('ollama' or 'mock')."""
    name = name.lower()
    if name == "ollama":
        return OllamaProvider(**kwargs)
    if name == "mock":
        return MockProvider(**kwargs)
    raise ValueError(f"unknown LLM provider: {name!r}")
