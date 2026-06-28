# Changelog

## 2026-06-28

- Completed Rebuild B by wiring current Java notes/media/rhyme/font surfaces into the 21.x UI kit, including live font previews, 21.x card/control styling, and rebuilt rhyme row visuals without changing core behavior.
- Completed Rebuild A foundation after 21.0 with Kotlin, Compose, Material 3, dynamic dark theme support, Java-native UI kit helpers, 21.x resource tokens, and local Pixel reference capture through WSL Debian ADB.
- Released 21.0 with a dedicated offline `RhymeEngine`, generated pronunciation/rhyme index asset, indexed rhyme lookup/cache readiness, a distinct studio toolbar/dock/editor shell, and rebuilt rhyme panel styling.
- Released 20.6 with CMU-loading rhyme row gating, stricter stale/fallback protection, curated `out` rhyme validation and ordering, refined editor surface styling, disabled/loading chip state, and an empty-note state.
- Released 20.5 as the published build for the 20.5 milestone after phases 20.5A, 20.5B, and 20.5C.
- Completed 20.5C premium polish with resource-backed pressed states and lightweight button icons while preserving behavior.
- Completed 20.5B UI integration by wiring design-system resources into existing Java surfaces, typography, buttons, chips, sheets, and spacing without changing app behavior.
- Created 20.5A design system foundation with reusable colors, dimensions, typography styles, panel/button/chip drawables, and theme tokens without changing app behavior.
- Released 20.4 with a premium dark studio UI pass: layered native panels, refined gradients/strokes, polished controls, toned-down backdrop, improved editor/list spacing, and pill-shaped rhyme chips without changing rhyme scoring.
- Released 20.3 with async fast-row rhyme generation, debounce/cancel handling, cached query results, bounded fast-row scoring, reduced popup rebuilds, and lightweight timing logs to address editor freezes.

## 2026-06-27

- Released 20.2 with stricter hip-hop rhyme buckets, stronger curated exact-rhyme ranking, AY/EY fallback separation for `my/try`, and `yours` pronunciation tuning.
- Released 20.1 with a focused rhyme regression check, stronger same-stem slang variants, unknown `-ing` pronunciation handling, and curated rhyme candidate tuning.
- Released 20.0 with pronunciation-first rhyme ranking using CMU stressed-vowel, coda, syllable, and exact/near/phrase/fallback buckets.
- Updated appcast metadata and release APK for in-app update delivery.
- Created Brobro Development Framework project record files: `PROJECT_STATUS.md`, `CODING_RULES.md`, and `HANDOFF.md`.
- Added initial changelog entry for project-record initialization.
