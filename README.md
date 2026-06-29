# PHANTOM

**A local DLAM — your phone is the brain, your PC is the hands.**

PHANTOM turns a phone into a local **Large Action Model (LAM)** that operates
your computer by *looking* at the screen and acting on it — like Rabbit r1 /
DLAM, but with the critical difference: **nothing goes to the cloud.** The
vision model runs locally on the phone (via Ollama), and everything stays
inside your own Wi-Fi.

```
┌─────────────────────────┐        Wi-Fi / HTTP        ┌──────────────────────────┐
│   📱 PHONE — the brain    │ ─────────────────────────► │   💻 PC — the GATE (body) │
│                          │  1. ask for a screenshot   │                          │
│   Ollama (Qwen2.5-VL)    │ ◄──── 2. screen image ──── │   mss → screen capture    │
│   VISION + LAM loop       │                            │                          │
│   decides next action     │  3. send action ─────────► │   pyautogui → click/type  │
│   sliding-window memory    │ ◄──── 4. result + screen ─ │   subprocess → commands   │
│   loops until done        │                            │   (ZERO intelligence)     │
└─────────────────────────┘                            └──────────────────────────┘
```

- **All intelligence on the phone.** Vision, planning, memory, the
  observe→decide→act loop. The PC holds none.
- **The PC is a dumb gate.** It only returns screenshots and executes the exact
  action it's told. No model, no decisions.
- **Local & private.** No cloud, no API keys, no passwords leaving your network.
  The brain runs on local Ollama (zero token cost).
- **Acts by sight, not APIs.** It works any app the way a person would — by
  looking and clicking.

## Quick start

### 1. PC (the gate)
```bash
pip install -r requirements.txt
python -m phantom.pc
# prints: http://<your-LAN-IP>:8765   ← put this in the phone
```

### 2. Phone (the brain), in Termux
See [`docs/SETUP.md`](docs/SETUP.md) for the full Termux + Ollama install.
```bash
export PHANTOM_PC_URL="http://192.168.1.50:8765"   # from step 1
export PHANTOM_MODEL="qwen2.5-vl:7b"
python -m phantom.phone "open the calculator and compute 12 * 9"
```

### Try it with no phone / no PC / no Ollama
```bash
python -m phantom.phone --demo
```

## Project layout
```
phantom/
  common/   protocol.py (phone↔PC contract) · config.py
  llm/      provider-agnostic vision LLM (Ollama default, mock for tests)
  pc/       the GATE: screen.py · actions.py · worker.py (FastAPI)
  phone/    the BRAIN: agent.py (LAM loop) · vision via llm · tools · memory
tests/      headless tests for both sides
```

## Roadmap
Built in phases, each a separate session (so it never strains usage limits):

- ✅ **Phase 0** — working Python core (phone LAM brain + PC gate, tested).
- ✅ **Phase 1** — package the gate into `Phantom-Gate.exe` (double-click). See
  [`docs/BUILD.md`](docs/BUILD.md).
- ✅ **Phase 2** — one-line Termux installer for the phone brain
  (`installer/termux_install.sh`).
- ✅ **Phase 3** — native Android APK shell (Kotlin) that drives the Termux
  brain. See [`docs/ANDROID.md`](docs/ANDROID.md).
- ✅ **Phase 4** — the LAM loop ported into the APK (Kotlin): talks straight to
  local Ollama + the gate, no Termux needed. "Run on-device" button.
- ✅ **Phase 5** — token-secured gate + one-step pairing: the gate prints a
  `PHANTOM:` code / QR; the app decodes it to fill URL + token.

## Tests
```bash
python -m pytest -q
```
