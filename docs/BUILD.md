# Building the Phantom Gate executable

The **gate** (PC body) can be packaged into a single double-click executable so
a non-technical user never touches Python.

## Windows → `Phantom-Gate.exe`
```bat
scripts\build_gate.bat
```
Produces `dist\Phantom-Gate.exe`. Double-click it on the computer you want
Phantom to control; it prints the LAN URL to enter on the phone.

## Linux / macOS → `Phantom-Gate`
```bash
scripts/build_gate.sh
```
Produces `dist/Phantom-Gate`.

## How it works
Both scripts call **PyInstaller** with `--onefile`. `phantom.pc` imports
`mss`, `pyautogui`, and `PIL` *lazily* (so the code loads on headless
machines), which means PyInstaller's static analysis can't see them — they are
passed explicitly via `--hidden-import`. The entrypoint is
`installer/gate_entry.py`.

> Build on the **same OS** you will run on — PyInstaller does not
> cross-compile. Build the `.exe` on Windows.

> The phone brain is **not** packaged this way — it runs in Termux next to
> Ollama. See `docs/SETUP.md`. A native Android APK is a later roadmap phase.
