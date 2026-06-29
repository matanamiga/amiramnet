"""Screen capture for the PC worker.

Uses ``mss`` to grab the primary monitor and Pillow to (optionally) downscale
and PNG-encode it. Heavy/display-dependent imports happen lazily inside the
functions so this module imports cleanly on a headless box (e.g. under test).
"""

from __future__ import annotations

import base64
import io

from ..common.protocol import Screenshot


def screen_size() -> tuple[int, int]:
    """Return ``(width, height)`` of the primary monitor in pixels."""
    import mss

    with mss.mss() as sct:
        mon = sct.monitors[1]  # index 0 is the "all monitors" virtual screen
        return int(mon["width"]), int(mon["height"])


def capture(scale: float = 1.0) -> Screenshot:
    """Capture the primary monitor as a base64 PNG ``Screenshot``.

    Args:
        scale: Multiplier applied to both dimensions before encoding. Values
            below ``1.0`` shrink the image (cheaper to transport / for the
            phone's vision model); ``1.0`` keeps native resolution.
    """
    import mss
    from PIL import Image

    with mss.mss() as sct:
        mon = sct.monitors[1]
        raw = sct.grab(mon)

    img = Image.frombytes("RGB", raw.size, raw.bgra, "raw", "BGRX")

    if scale != 1.0:
        new_w = max(1, int(img.width * scale))
        new_h = max(1, int(img.height * scale))
        img = img.resize((new_w, new_h), Image.LANCZOS)

    buf = io.BytesIO()
    img.save(buf, format="PNG")
    image_b64 = base64.b64encode(buf.getvalue()).decode("ascii")

    return Screenshot(
        image_b64=image_b64,
        format="png",
        width=img.width,
        height=img.height,
    )
