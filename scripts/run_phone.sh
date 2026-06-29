#!/usr/bin/env bash
# Start the PHANTOM brain (phone). Run inside Termux next to `ollama serve`.
# Usage: PHANTOM_PC_URL=http://<pc-ip>:8765 ./scripts/run_phone.sh "your goal"
set -euo pipefail
cd "$(dirname "$0")/.."
: "${PHANTOM_PC_URL:?set PHANTOM_PC_URL to the PC gate URL (it prints one on start)}"
exec python3 -m phantom.phone "${@:-"Say hello on screen."}"
