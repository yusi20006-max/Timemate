# Tehran prayer-time data (1405)

This directory contains the repeatable data-generation tool for the first offline prayer-time release. The initial release intentionally supports **Tehran only**.

## Authoritative source

- Publisher: Geophysics Institute, University of Tehran
- Document: *Tehran prayer times 1405*
- Original filename supplied for this release: `Tehran1405.pdf`
- PDF SHA-256: `2035e940740b28f5f2722274c64b1fff6bcf9892de77fcd3f662a027effa3540`
- Coverage: 1 Farvardin 1405 through 29 Esfand 1405 (365 days)
- Coordinates printed in the source: 35.70° N, 51.42° E
- Time zone: `Asia/Tehran`

The six daily events are stored in this exact order: Fajr, sunrise, Dhuhr, sunset, Maghrib, and religious midnight. Values are minute-of-day integers in the compressed JSON asset shipped in the Android APK.

## Regenerate the asset

Install Poppler utilities, then run:

```sh
python3 tools/prayer-times/build_tehran_1405.py \
  --pdf /path/to/Tehran1405.pdf \
  --output app/src/main/assets/prayer-times/tehran-1405.json.gz
```

The generator requires exactly 12 PDF pages, checks the 1405 year marker, validates the expected number and sequence of days per month, validates all six times and their chronological order, records the source PDF SHA-256, and writes deterministic gzip-compressed JSON.

Do not substitute third-party APIs or calculated times for the University of Tehran source. Additional cities and years must be introduced as separately sourced datasets with their own validation.

## Runtime behavior

The Android app reads this asset locally. It does not download PDFs, scrape the university website, or parse PDFs at runtime. The prayer screen and all five prayer widgets share the same repository and the same bundled dataset.
