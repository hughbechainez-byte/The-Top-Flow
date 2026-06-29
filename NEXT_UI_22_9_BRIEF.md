# Next UI Brief: 22.9

## Purpose

Make launch and preload feel like part of the premium OLED neon identity while preserving startup timing and rhyme preload behavior.

## 5.3 Implementation Scope

- Rebuild the startup overlay with a true-OLED branded surface.
- Add crisp signal rails and refined preload status treatment.
- Keep `splashFill` and existing `scaleX` progress calls compatible.
- Preserve `SPLASH_MIN_MS`, `SPLASH_MAX_MS`, preload callbacks, cache warming, and forced dismissal behavior.

## Codex Review Notes

- Accepted the launch surface rebuild and scoped splash helper methods in `MainActivity.java`.
- Changed the subtitle to a quieter product label.
- Removed fake percentage labels from the progress status.
- Reset launch title letter spacing to `0f` to keep text rendering crisp.
