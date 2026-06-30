"""Advertise the gate on the LAN via mDNS so phones can find it automatically.

Registers a ``_phantom._tcp`` service whose TXT record carries the gate URL and
bearer token, so a Phantom app on the same Wi‑Fi can discover and pair with
zero typing. Optional: if the ``zeroconf`` package is absent, advertising is a
no-op and the user can still pair via the printed code / QR.

Per the project's threat model, LAN discovery is a *convenience*: only run the
gate on a trusted network. Set ``PHANTOM_DISCOVERY=0`` to disable it.
"""

from __future__ import annotations

import socket

SERVICE_TYPE = "_phantom._tcp.local."


class Advertiser:
    """Handle for an active mDNS registration; call :meth:`close` to stop."""

    def __init__(self, zc, info) -> None:
        self._zc = zc
        self._info = info

    def close(self) -> None:
        try:
            self._zc.unregister_service(self._info)
            self._zc.close()
        except Exception:  # noqa: BLE001 — best-effort teardown
            pass


def build_info(ip: str, port: int, token: str, name: str = "Phantom Gate"):
    """Build the mDNS ServiceInfo (no network I/O). Requires zeroconf."""
    from zeroconf import ServiceInfo

    return ServiceInfo(
        SERVICE_TYPE,
        f"{name}.{SERVICE_TYPE}",
        addresses=[socket.inet_aton(ip)],
        port=port,
        properties={
            b"url": f"http://{ip}:{port}".encode(),
            b"token": token.encode(),
        },
    )


def advertise(ip: str, port: int, token: str, name: str = "Phantom Gate") -> Advertiser | None:
    """Register the gate over mDNS. Returns None if zeroconf isn't installed."""
    try:
        from zeroconf import Zeroconf
    except ImportError:
        return None

    info = build_info(ip, port, token, name)
    zc = Zeroconf()
    zc.register_service(info)
    return Advertiser(zc, info)
