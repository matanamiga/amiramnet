"""Run the PC gate: ``python -m phantom.pc``.

Prints the LAN address to put into the phone's ``PHANTOM_PC_URL`` and starts
the server.
"""

from __future__ import annotations

import socket

from ..common.config import PcConfig
from .worker import create_app


def _lan_ip() -> str:
    """Best-effort local IP (no traffic actually sent)."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        return s.getsockname()[0]
    except Exception:  # noqa: BLE001
        return "127.0.0.1"
    finally:
        s.close()


def main() -> None:
    config = PcConfig()
    ip = _lan_ip()
    print("=" * 48)
    print("  PHANTOM GATE (PC body)")
    print(f"  Put this in the phone's PHANTOM_PC_URL:")
    print(f"      http://{ip}:{config.port}")
    print(f"  Commands enabled: {config.allow_commands}")
    print("=" * 48)

    import uvicorn

    uvicorn.run(create_app(config), host=config.host, port=config.port)


if __name__ == "__main__":
    main()
