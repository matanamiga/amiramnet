@echo off
REM Build the Phantom Gate into Phantom-Gate.exe (Windows).
setlocal
cd /d "%~dp0\.."

python -m pip install -q -r requirements.txt pyinstaller

REM phantom.pc imports mss / pyautogui / PIL lazily -> declare hidden imports.
pyinstaller --noconfirm --clean --onefile --name Phantom-Gate ^
  --collect-submodules uvicorn ^
  --collect-submodules phantom ^
  --hidden-import phantom.pc.worker ^
  --hidden-import mss ^
  --hidden-import pyautogui ^
  --hidden-import PIL ^
  installer\gate_entry.py

echo.
echo Built: dist\Phantom-Gate.exe   (double-click on the PC to control)
endlocal
