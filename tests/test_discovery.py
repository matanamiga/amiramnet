"""Tests for mDNS gate advertising (no network I/O)."""

from __future__ import annotations

import pytest

pytest.importorskip("zeroconf")

from phantom.pc import discovery


def test_service_info_carries_url_and_token():
    info = discovery.build_info("192.168.1.50", 8765, "tok-xyz", name="Test Gate")

    assert info.type == discovery.SERVICE_TYPE
    assert info.name == f"Test Gate.{discovery.SERVICE_TYPE}"
    assert info.port == 8765
    # TXT properties expose the URL + token the phone needs to pair.
    props = info.properties
    assert props[b"url"] == b"http://192.168.1.50:8765"
    assert props[b"token"] == b"tok-xyz"
