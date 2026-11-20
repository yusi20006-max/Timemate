#!/usr/bin/env python3
"""Verify AAPT's packaged prayer JSON asset in both APK variants."""
from __future__ import annotations

import json
import zipfile
from pathlib import Path

ASSET_PATH = "assets/prayer-times/tehran-1405.json"
DEBUG_APK = Path("app/build/outputs/apk/debug/app-debug.apk")
RELEASE_APK_DIR = Path("app/build/outputs/apk/release")
EXPECTED_SOURCE_SHA256 = "2035e940740b28f5f2722274c64b1fff6bcf9892de77fcd3f662a027effa3540"


def validate(apk: Path) -> None:
    with zipfile.ZipFile(apk) as archive:
        assert ASSET_PATH in archive.namelist(), (
            f"{apk}: expected Android-packaged asset {ASSET_PATH}"
        )
        payload = json.loads(archive.read(ASSET_PATH))
    assert payload["schemaVersion"] == 1
    assert payload["cityId"] == "tehran"
    assert payload["persianYear"] == 1405
    assert payload["timeZone"] == "Asia/Tehran"
    assert payload["source"]["sha256"] == EXPECTED_SOURCE_SHA256
    days = payload["days"]
    assert len(days) == 365
    for index, encoded in enumerate(days, start=1):
        values = [int(value) for value in encoded.split(",")]
        assert len(values) == 6, f"{apk}: day {index} must have six events"
        assert all(0 <= value < 1440 for value in values), f"{apk}: day {index} has invalid time"
        assert all(a < b for a, b in zip(values, values[1:])), f"{apk}: day {index} has invalid order"
    print(f"Validated {apk}: packaged Tehran JSON asset, 365 days.")


def main() -> None:
    if not DEBUG_APK.is_file():
        raise SystemExit(f"APK not found: {DEBUG_APK}")
    release_apks = sorted(RELEASE_APK_DIR.glob("app-release*.apk"))
    if len(release_apks) != 1:
        raise SystemExit(
            f"Expected exactly one release APK in {RELEASE_APK_DIR}, found: {release_apks}"
        )
    validate(DEBUG_APK)
    validate(release_apks[0])


if __name__ == "__main__":
    main()
