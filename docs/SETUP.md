# PHANTOM — setup guide

## PC side (the GATE / body)

The PC runs a thin FastAPI service with no intelligence — it only screenshots
and executes actions.

```bash
pip install -r requirements.txt
python -m phantom.pc
```

It prints the LAN URL to give the phone, e.g. `http://192.168.1.50:8765`.

Environment overrides:

| Variable | Default | Meaning |
|---|---|---|
| `PHANTOM_PC_HOST` | `0.0.0.0` | bind address |
| `PHANTOM_PC_PORT` | `8765` | port |
| `PHANTOM_PC_ALLOW_COMMANDS` | `1` | allow `POST /command` shell execution |
| `PHANTOM_TOKEN` | _(generated)_ | bearer token; auto-generated if unset. The gate prints a pairing code/QR carrying URL + token. |
| `PHANTOM_DISCOVERY` | `1` | advertise the gate over mDNS so phones can auto-find it (`pip install zeroconf`); set `0` to disable. |

> **Security:** the gate can move your mouse, type, and (if enabled) run shell
> commands. Run it only on a trusted LAN. Set `PHANTOM_PC_ALLOW_COMMANDS=0` to
> disable shell commands.

## Phone side (the BRAIN), in Termux

The phone hosts the vision model and the LAM loop.

### Fast path — one-line installer
After installing Termux (step 1 below), run:
```bash
curl -fsSL https://raw.githubusercontent.com/matanamiga/amiramnet/main/installer/termux_install.sh | bash
```
This installs Python + Ollama + the vision model, fetches Phantom, and adds a
`phantom` command. Then just:
```bash
export PHANTOM_PC_URL=http://<pc-gate-url>
phantom "your goal in plain language"
```
The manual steps below are the same thing, broken out.

### 1. Install Termux (from F-Droid, **not** Play Store)
https://f-droid.org/packages/com.termux/

### 2. Base packages
```bash
termux-setup-storage
pkg update && pkg upgrade -y
pkg install python git -y
pip install requests pydantic
```

### 3. Install Ollama + a vision model
```bash
pkg install ollama
ollama serve &

# choose by RAM:
ollama pull qwen2.5-vl:3b    # ~4 GB RAM
ollama pull qwen2.5-vl:7b    # ~6 GB RAM (recommended)
ollama pull minicpm-v:8b     # ~7 GB RAM (best)
```

### 4. Get PHANTOM and run the brain
```bash
git clone <this-repo> && cd amiramnet
export PHANTOM_PC_URL="http://192.168.1.50:8765"   # the PC's printed URL
export PHANTOM_MODEL="qwen2.5-vl:7b"
python -m phantom.phone "your goal in plain language"
```

### Keep Android from killing it
```bash
termux-wake-lock
# Settings → Apps → Termux → Battery → Unrestricted
```

Tune for the phone with env vars:

| Variable | Default | Meaning |
|---|---|---|
| `PHANTOM_MODEL` | `qwen2.5-vl:7b` | Ollama vision model |
| `PHANTOM_NUM_THREADS` | `4` | CPU threads for Ollama (lower = cooler phone) |
| `PHANTOM_MEMORY_WINDOW` | `4` | steps kept in the sliding-window memory |
| `PHANTOM_MAX_STEPS` | `20` | safety cap per goal |
| `PHANTOM_SCREENSHOT_SCALE` | `0.75` | downscale screenshots before sending |
| `PHANTOM_TOKEN` | _(empty)_ | bearer token; must match the gate's (from its pairing code) |
