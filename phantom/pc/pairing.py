"""Pairing helpers: encode the gate's URL + token so the phone can connect.

The gate prints a compact ``pairing code`` (and, if the optional ``qrcode``
package is installed, a scannable terminal QR). The phone decodes it to fill in
the PC URL and bearer token in one step — no manual typing.
"""

from __future__ import annotations

import base64
import json


def encode_pairing(url: str, token: str, model: str = "") -> str:
    """Return a URL-safe base64 pairing code carrying url/token/model."""
    payload = {"url": url, "token": token}
    if model:
        payload["model"] = model
    raw = json.dumps(payload, separators=(",", ":")).encode("utf-8")
    return "PHANTOM:" + base64.urlsafe_b64encode(raw).decode("ascii")


def decode_pairing(code: str) -> dict:
    """Inverse of :func:`encode_pairing`. Raises ValueError on bad input."""
    code = code.strip()
    if code.startswith("PHANTOM:"):
        code = code[len("PHANTOM:") :]
    try:
        raw = base64.urlsafe_b64decode(code.encode("ascii"))
        data = json.loads(raw)
    except Exception as exc:  # noqa: BLE001
        raise ValueError(f"invalid pairing code: {exc}") from exc
    if "url" not in data or "token" not in data:
        raise ValueError("pairing code missing url/token")
    return data


def render_qr(code: str) -> str | None:
    """Return an ASCII QR for the pairing code, or None if qrcode is absent."""
    try:
        import qrcode  # optional dependency
    except ImportError:
        return None
    qr = qrcode.QRCode(border=1)
    qr.add_data(code)
    qr.make(fit=True)
    import io

    buf = io.StringIO()
    qr.print_ascii(out=buf)
    return buf.getvalue()
