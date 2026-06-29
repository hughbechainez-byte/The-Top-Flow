# Next UI Brief: 22.1

## Purpose

Build the update foundation required for the 22.1 to 23.0 run. The 22.0 appcast line must be able to advertise future 22.x and 23.0 builds, and 22.1+ clients must be able to choose among several available APKs.

## 5.3 Implementation Scope

- Preserve the legacy appcast shape: `versionCode`, `versionName`, `notes`, and `apkUrl`.
- Add support for a multi-version `versions[]` list.
- Filter out versions at or below the installed `BuildConfig.VERSION_CODE`.
- Sort available updates by descending version code.
- Keep the single-update flow simple.
- Add an OLED bottom-sheet chooser when several newer versions are available.

## Codex Review Notes

- Accepted the parser and chooser implementation.
- Adjusted the chooser card spacing so multiple update choices read as separate premium rows instead of one card with internal dividers.
- Release appcasts should include both top-level latest fields for 22.0 clients and `versions[]` for 22.1+ clients.
