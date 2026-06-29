"""Execute actions on the PC — the "body" with no intelligence of its own.

Every function here just carries out an instruction decided by the phone's
brain. ``pyautogui`` is imported lazily so the module loads on a headless box
(tests monkeypatch a fake ``pyautogui`` into ``sys.modules``).
"""

from __future__ import annotations

import subprocess
import time

from ..common.protocol import (
    Action,
    ActionResult,
    ClickAction,
    CommandRequest,
    CommandResult,
    KeyAction,
    MoveAction,
    ScrollAction,
    TypeAction,
    WaitAction,
)


def execute(action: Action) -> ActionResult:
    """Carry out a single brain-decided action. Never raises — failures come
    back as ``ActionResult(ok=False, detail=...)`` so the brain can react."""
    try:
        if isinstance(action, WaitAction):
            time.sleep(max(0.0, action.seconds))
            return ActionResult(ok=True, detail=f"waited {action.seconds}s")

        import pyautogui

        # Don't let a corner-of-screen move abort the whole run.
        pyautogui.FAILSAFE = False

        if isinstance(action, ClickAction):
            pyautogui.click(x=action.x, y=action.y, clicks=action.clicks, button=action.button)
            return ActionResult(ok=True, detail=f"clicked {action.button} @ {action.x},{action.y}")

        if isinstance(action, MoveAction):
            pyautogui.moveTo(action.x, action.y)
            return ActionResult(ok=True, detail=f"moved @ {action.x},{action.y}")

        if isinstance(action, TypeAction):
            pyautogui.write(action.text, interval=0.01)
            return ActionResult(ok=True, detail=f"typed {len(action.text)} chars")

        if isinstance(action, KeyAction):
            if len(action.keys) == 1:
                pyautogui.press(action.keys[0])
            else:
                pyautogui.hotkey(*action.keys)
            return ActionResult(ok=True, detail=f"pressed {'+'.join(action.keys)}")

        if isinstance(action, ScrollAction):
            if action.dy:
                pyautogui.scroll(action.dy)
            if action.dx and hasattr(pyautogui, "hscroll"):
                pyautogui.hscroll(action.dx)
            return ActionResult(ok=True, detail=f"scrolled dx={action.dx} dy={action.dy}")

        return ActionResult(ok=False, detail=f"unknown action: {type(action).__name__}")

    except Exception as exc:  # noqa: BLE001 — body must never crash the gate
        return ActionResult(ok=False, detail=f"{type(exc).__name__}: {exc}")


def run_command(req: CommandRequest, allow: bool) -> CommandResult:
    """Run a shell command on the PC. Disabled unless ``allow`` is True."""
    if not allow:
        return CommandResult(ok=False, stderr="commands disabled", exit_code=126)
    try:
        proc = subprocess.run(
            req.command,
            shell=True,
            capture_output=True,
            text=True,
            timeout=req.timeout,
        )
        return CommandResult(
            ok=proc.returncode == 0,
            stdout=proc.stdout,
            stderr=proc.stderr,
            exit_code=proc.returncode,
        )
    except subprocess.TimeoutExpired:
        return CommandResult(ok=False, stderr="timeout", exit_code=124)
    except Exception as exc:  # noqa: BLE001
        return CommandResult(ok=False, stderr=f"{type(exc).__name__}: {exc}", exit_code=1)
