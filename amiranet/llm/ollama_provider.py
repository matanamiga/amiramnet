"""Local Ollama provider — the default brain (zero API cost).

Talks to an Ollama server (running in Termux on the phone, or anywhere) using
its ``/api/generate`` endpoint, which accepts a list of base64 images for
vision models such as ``qwen2.5-vl``.
"""

from __future__ import annotations

import requests

from amiranet.llm.base import LLMProvider, LLMResponse


class OllamaProvider(LLMProvider):
    def __init__(
        self,
        url: str = "http://127.0.0.1:11434",
        model: str = "qwen2.5-vl:7b",
        num_threads: int = 4,
        timeout: float = 120.0,
    ) -> None:
        self.url = url.rstrip("/")
        self.model = model
        self.num_threads = num_threads
        self.timeout = timeout

    def generate(
        self,
        system: str,
        prompt: str,
        images_b64: list[str] | None = None,
    ) -> LLMResponse:
        payload = {
            "model": self.model,
            "system": system,
            "prompt": prompt,
            "stream": False,
            # Limit CPU threads so the phone stays responsive while thinking.
            "options": {"num_thread": self.num_threads},
        }
        if images_b64:
            payload["images"] = images_b64

        resp = requests.post(
            f"{self.url}/api/generate", json=payload, timeout=self.timeout
        )
        resp.raise_for_status()
        data = resp.json()
        return LLMResponse(text=data.get("response", ""), raw=data)
