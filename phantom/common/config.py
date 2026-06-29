"""Configuration loaded from environment variables (with sane defaults).

Every setting can be overridden via an ``PHANTOM_*`` environment variable so
the same code runs unchanged in Termux on the phone and on a laptop.
"""

from __future__ import annotations

import os
from dataclasses import dataclass


def _env(name: str, default: str) -> str:
    return os.environ.get(name, default)


@dataclass
class PcConfig:
    """Settings for the PC worker ("body")."""

    host: str = _env("PHANTOM_PC_HOST", "0.0.0.0")
    port: int = int(_env("PHANTOM_PC_PORT", "8765"))
    # Allow shell commands via POST /command. Off by default for safety.
    allow_commands: bool = _env("PHANTOM_PC_ALLOW_COMMANDS", "1") == "1"


@dataclass
class PhoneConfig:
    """Settings for the phone agent ("brain")."""

    pc_url: str = _env("PHANTOM_PC_URL", "http://127.0.0.1:8765")
    ollama_url: str = _env("PHANTOM_OLLAMA_URL", "http://127.0.0.1:11434")
    model: str = _env("PHANTOM_MODEL", "qwen2.5-vl:7b")
    provider: str = _env("PHANTOM_PROVIDER", "ollama")
    # Sliding-window memory: how many past steps to keep in the prompt.
    memory_window: int = int(_env("PHANTOM_MEMORY_WINDOW", "4"))
    # Limit Ollama CPU threads so the phone does not freeze while thinking.
    num_threads: int = int(_env("PHANTOM_NUM_THREADS", "4"))
    max_steps: int = int(_env("PHANTOM_MAX_STEPS", "20"))
    # Downscale screenshots before sending to the model to save RAM / latency.
    screenshot_scale: float = float(_env("PHANTOM_SCREENSHOT_SCALE", "0.75"))
    request_timeout: float = float(_env("PHANTOM_REQUEST_TIMEOUT", "120"))
