"""Run the PC gate: ``python -m phantom.pc``.

Prints the LAN address to put into the phone's ``PHANTOM_PC_URL`` and starts
the server.
"""

from __future__ import annotations

import socket

from ..common.config import PcConfig
from . import pairing
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
    # Build the app first so it fills in a generated token when none was set.
    app = create_app(config)
    config = app.state.config

    ip = _lan_ip()
    url = f"http://{ip}:{config.port}"
    code = pairing.encode_pairing(url, config.token, "")

    print("=" * 56)
    print("  PHANTOM GATE (PC body)")
    print(f"  URL:   {url}")
    print(f"  Token: {config.token}")
    print(f"  Commands enabled: {config.allow_commands}")
    print("-" * 56)
    print("  Scan this in the Phantom app to pair, or paste the code:")
    qr = pairing.render_qr(code)
    if qr:
        print(qr)
    else:
        print("  (install 'qrcode' for a scannable QR)")
    print(f"  Pairing code: {code}")
    print("=" * 56)

    import uvicorn

    uvicorn.run(app, host=config.host, port=config.port)


if __name__ == "__main__":
    main()
