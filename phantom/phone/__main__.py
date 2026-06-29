"""Run the phone brain: ``python -m phantom.phone "<goal>"``.

Use ``--demo`` to run the full loop with a mock model and a no-op PC, so you
can see the brain work with no Ollama, no phone, and no PC.
"""

from __future__ import annotations

import argparse
import logging

from ..common.config import PhoneConfig
from ..common.protocol import Action, ActionResult, Screenshot
from ..llm import build_provider
from .agent import Agent


class _NoopPcClient:
    """Stand-in PC for ``--demo``: blank screen, every action 'succeeds'."""

    def screenshot(self, scale: float = 1.0) -> Screenshot:
        # 1x1 transparent PNG.
        px = (
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42m" "NkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
        )
        return Screenshot(image_b64=px, format="png", width=1, height=1)

    def do_action(self, action: Action) -> ActionResult:
        print(f"   [PC] would execute: {action.model_dump()}")
        return ActionResult(ok=True, detail="noop")


def main() -> None:
    parser = argparse.ArgumentParser(prog="phantom.phone", description="PHANTOM brain")
    parser.add_argument("goal", nargs="?", default="Say hello on screen.")
    parser.add_argument("--demo", action="store_true", help="run offline with a mock model")
    args = parser.parse_args()

    logging.basicConfig(level=logging.INFO, format="%(message)s")
    config = PhoneConfig()

    if args.demo:
        agent = Agent(config, llm=build_provider("mock"), pc=_NoopPcClient())
    else:
        agent = Agent(config)

    summary = agent.run(args.goal)
    print(f"\n=== RESULT ===\n{summary}")


if __name__ == "__main__":
    main()
