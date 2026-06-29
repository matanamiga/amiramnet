#!/usr/bin/env bash
# Start the AMIRANET brain (phone). Run inside Termux next to `ollama serve`.
# Usage: AMIRANET_PC_URL=http://<pc-ip>:8765 ./scripts/run_phone.sh "your goal"
set -euo pipefail
cd "$(dirname "$0")/.."
: "${AMIRANET_PC_URL:?set AMIRANET_PC_URL to the PC gate URL (it prints one on start)}"
exec python3 -m amiranet.phone "${@:-"Say hello on screen."}"
