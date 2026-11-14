#!/usr/bin/env python3
"""Validate the compressed Tehran 1405 asset without external dependencies."""
from __future__ import annotations

import gzip
import json
from pathlib import Path

ASSET = Path("app/src/main/assets/prayer-times/tehran-1405.json.gz")
EXPECTED_PDF_SHA256 = "2035e940740b28f5f2722274c64b1fff6bcf9892de77fcd3f662a027effa3540"
EXPECTED_FIRST = [283, 367, 732, 1097, 1115, 1409]
EXPECTED_LAST = [285, 369, 732, 1096, 1114, 1410]


def main() -> None:
    payload = json.loads(gzip.decompress(ASSET.read_bytes()))
    assert payload["schemaVersion"] == 1
    assert payload["cityId"] == "tehran"
    assert payload["persianYear"] == 1405
    assert payload["timeZone"] == "Asia/Tehran"
    assert payload["startDate"] == "2026-03-21"
    assert payload["eventOrder"] == [
        "fajr", "sunrise", "dhuhr", "sunset", "maghrib", "midnight"
    ]
    assert payload["source"]["filename"] == "Tehran1405.pdf"
    assert payload["source"]["sha256"] == EXPECTED_PDF_SHA256

    days = payload["days"]
    assert len(days) == 365
    for index, encoded in enumerate(days, start=1):
        values = [int(value) for value in encoded.split(",")]
        assert len(values) == 6, f"day {index}: expected six events"
        assert all(0 <= value < 1440 for value in values), f"day {index}: invalid time"
        assert all(a < b for a, b in zip(values, values[1:])), f"day {index}: invalid order"

    assert [int(value) for value in days[0].split(",")] == EXPECTED_FIRST
    assert [int(value) for value in days[-1].split(",")] == EXPECTED_LAST
    print("Validated bundled Tehran 1405 dataset: 365 days, six events per day.")


if __name__ == "__main__":
    main()
