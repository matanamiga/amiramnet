# PHANTOM — architecture

## The split
PHANTOM is a **local DLAM**: a Large Action Model whose brain lives on the
phone and whose hands are a remote PC. The design mirrors Rabbit r1 / DLAM but
inverts the trust model — Rabbit thinks in *its* cloud; PHANTOM thinks on
*your* phone.

| Concern | Lives on |
|---|---|
| Vision (seeing the screen) | **phone** (Ollama / Qwen2.5-VL) |
| Planning & decisions (the LAM loop) | **phone** |
| Memory | **phone** (sliding window) |
| Screen capture | PC gate |
| Mouse / keyboard / shell execution | PC gate |
| Any intelligence | **phone only** — the PC has none |

## The loop (`phantom/phone/agent.py`)
```
for step in 1..max_steps:
    screenshot = pc.screenshot(scale)          # observe
    decision   = llm.generate(system, prompt, images=[screenshot])
    action     = parse_decision(decision)       # {"thought","action":{...}}
    if action is "done":      return summary
    if action is "screenshot": continue          # re-observe
    pc.do_action(action)                         # act
    memory.add(...)                              # remember (bounded)
```
The model is told to reply with a single JSON tool-call. It acts **by sight**:
coordinates are pixels on the screenshot it was shown.

## The contract (`phantom/common/protocol.py`)
One pydantic module defines the entire phone↔PC wire format, so both sides stay
in sync:

```
GET  /info        -> PcInfo
GET  /screenshot  -> Screenshot          (base64 PNG)
POST /action      -> ActionResult         (Action = click|move|type|key|scroll|wait)
POST /command     -> CommandResult
```

## Pluggable brain (`phantom/llm/`)
The agent depends only on `LLMProvider.generate(system, prompt, images_b64)`.
- `OllamaProvider` — default, local, zero cost.
- `MockProvider` — deterministic, for tests and `--demo`.

Swapping the reasoning engine never touches agent logic.

## On-device constraints (handled by design)
- **RAM / Low-Memory-Killer** → `termux-wake-lock`, unrestricted battery.
- **Context growth** → `SlidingMemory` keeps only the last N steps.
- **Thermal / freeze** → `PHANTOM_NUM_THREADS` caps Ollama CPU threads;
  screenshots are downscaled before transport.
