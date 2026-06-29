#!/usr/bin/env bash
# Build the Phantom Gate into a single self-contained binary (Linux/macOS).
# On Windows use scripts\build_gate.bat to get Phantom-Gate.exe.
set -euo pipefail
cd "$(dirname "$0")/.."

python3 -m pip install -q -r requirements.txt pyinstaller

# phantom.pc imports mss / pyautogui / PIL *lazily*, so PyInstaller's static
# analysis can't see them — they must be declared as hidden imports.
pyinstaller --noconfirm --clean --onefile --name Phantom-Gate \
  --collect-submodules uvicorn \
  --collect-submodules phantom \
  --hidden-import phantom.pc.worker \
  --hidden-import mss \
  --hidden-import pyautogui \
  --hidden-import PIL \
  installer/gate_entry.py

echo
echo "Built: dist/Phantom-Gate   (run it on the computer you want to control)"
