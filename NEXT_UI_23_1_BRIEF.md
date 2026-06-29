# Next UI Brief: 23.1

## Purpose

Start the 23.1 to 24.0 cycle by making the app background strictly OLED black and preserving the Compose lifecycle fix that supersedes the 23.0 startup crash path.

## 5.3 Implementation Scope

- Remove the radar/grid/waveform Compose backdrop art.
- Keep `TopFlowUiBackdropBridge` and view-tree owner attachment intact.
- Render only a pure black Compose surface behind the app.
- Avoid dependencies, assets, behavior changes, or animation loops.

## Codex Review Notes

- Accepted the pure-black `PremiumStudioBackdrop()` replacement.
- Confirmed lifecycle host code remains intact.
- Confirmed local rhyme checks and release build passed.
- Packaged and published 23.1 to the multi-version appcast line.
