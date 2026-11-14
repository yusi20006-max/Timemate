#!/usr/bin/env python3
"""Build the bundled Tehran 1405 prayer-time asset from the official UT PDF.

Requires poppler-utils (pdfinfo and pdftotext).
"""
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import re
import subprocess
from pathlib import Path

MONTH_DAYS = [31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29]
EVENT_ORDER = ["fajr", "sunrise", "dhuhr", "sunset", "maghrib", "midnight"]
START_DATE = "2026-03-21"
EXPECTED_SOURCE_SHA256 = "2035e940740b28f5f2722274c64b1fff6bcf9892de77fcd3f662a027effa3540"
TIME_RE = re.compile(r"(?<!\d)(\d{1,2}:\d{2})(?!\d)")


def run(*args: str) -> str:
    return subprocess.check_output(args, text=True)


def to_minutes(value: str) -> int:
    hour, minute = map(int, value.split(":"))
    if not (0 <= hour < 24 and 0 <= minute < 60):
        raise ValueError(f"Invalid time: {value}")
    return hour * 60 + minute


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--pdf", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    if args.pdf.name != "Tehran1405.pdf":
        raise SystemExit("Name the official source file Tehran1405.pdf before generating the asset.")

    info = run("pdfinfo", str(args.pdf))
    page_match = re.search(r"^Pages:\s+(\d+)$", info, re.MULTILINE)
    if not page_match or int(page_match.group(1)) != 12:
        raise SystemExit("Official Tehran 1405 source must contain exactly 12 monthly pages.")

    source_sha256 = hashlib.sha256(args.pdf.read_bytes()).hexdigest()
    if source_sha256 != EXPECTED_SOURCE_SHA256:
        raise SystemExit("The PDF fingerprint does not match the reviewed University of Tehran source.")

    days: list[str] = []
    for page, expected_count in enumerate(MONTH_DAYS, start=1):
        text = run("pdftotext", "-f", str(page), "-l", str(page), "-layout", str(args.pdf), "-")
        if "1405" not in text:
            raise SystemExit(f"Page {page} does not identify the 1405 calendar year.")

        page_days: list[int] = []
        for line in text.splitlines():
            times = TIME_RE.findall(line)
            if len(times) != 6:
                continue
            # PDF text extraction is left-to-right: midnight, maghrib, sunset,
            # dhuhr, sunrise, fajr. Store the canonical event order instead.
            midnight, maghrib, sunset, dhuhr, sunrise, fajr = times
            numbers = re.findall(r"\d+", line)
            day = int(numbers[-1])
            canonical = [fajr, sunrise, dhuhr, sunset, maghrib, midnight]
            minutes = [to_minutes(value) for value in canonical]
            if any(later <= earlier for earlier, later in zip(minutes, minutes[1:])):
                raise SystemExit(f"Invalid event order on page {page}, day {day}: {canonical}")
            if minutes[-1] <= minutes[-2]:
                raise SystemExit(f"Midnight must follow Maghrib on page {page}, day {day}.")
            if day != len(page_days) + 1:
                raise SystemExit(f"Unexpected day sequence on page {page}: found day {day}.")
            page_days.append(day)
            days.append(",".join(str(value) for value in minutes))

        if len(page_days) != expected_count:
            raise SystemExit(
                f"Page {page}: expected {expected_count} days, extracted {len(page_days)}."
            )

    if len(days) != 365:
        raise SystemExit(f"Expected 365 daily records, extracted {len(days)}.")

    payload = {
        "schemaVersion": 1,
        "cityId": "tehran",
        "persianYear": 1405,
        "timeZone": "Asia/Tehran",
        "startDate": START_DATE,
        "eventOrder": EVENT_ORDER,
        "source": {
            "publisher": "Geophysics Institute, University of Tehran",
            "document": "Tehran prayer times 1405",
            "filename": args.pdf.name,
            "sha256": source_sha256,
        },
        "days": days,
    }
    encoded = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(gzip.compress(encoded, compresslevel=9, mtime=0))
    print(f"Validated {len(days)} Tehran days; wrote {args.output}")


if __name__ == "__main__":
    main()
