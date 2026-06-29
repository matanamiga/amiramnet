"""PyInstaller entrypoint for the Phantom Gate (the PC body).

Bundled into a single executable (``Phantom-Gate.exe`` on Windows) so a
non-technical user can just double-click to start the gate on their computer.
"""

from phantom.pc.__main__ import main

if __name__ == "__main__":
    main()
