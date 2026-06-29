#!/usr/bin/env bash
# Start the AMIRANET PC gate (body). Run this on the computer to be controlled.
set -euo pipefail
cd "$(dirname "$0")/.."
python3 -m pip install -r requirements.txt >/dev/null 2>&1 || true
exec python3 -m amiranet.pc
