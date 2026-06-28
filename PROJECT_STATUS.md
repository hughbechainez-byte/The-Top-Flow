# Project Status

Last updated: 2026-06-28

## App Name

The Top Flow, also referred to as Top Flow / Rhyming Notes.

## Purpose

Native Android songwriting notes app for GrapheneOS/Android. The app helps capture lyrics, attach song/voice references, and surface rhyme suggestions while writing.

## Current Known Features

- Local note list and lyric editor.
- Per-note title, body, font, note color, text color, and accent color.
- Resource-backed design system foundation: colors, dimensions, typography styles, panel/button/chip/editor drawables, and theme tokens.
- Existing Java UI helpers consume the design system for major surfaces, buttons, chips, sheets, spacing, typography, and editor presentation.
- Premium dark studio UI with layered native panels, polished controls, pill-shaped rhyme chips, editor surface refinement, and empty-note state.
- Rhyme suggestion chips near the cursor, backed by `cmudict.dict`, pronunciation-first CMU ranking, quality buckets, curated writing examples, async fast-row generation, loading-state gating, and fallback phonetic heuristics for genuine unknowns.
- Rhyme settings for strictness, maximum suggestions, rhyme row visibility, exact-only mode, slang inclusion, and removed suggestions.
- Song attachment and playback controls.
- Voice recording, playback, rename, and save-to-disk support.
- App update checks through an online appcast manifest.

## Current Known Issues

- Most behavior lives in one large `MainActivity.java`, so changes should be especially small and carefully scoped.
- The rhyming system still lives inside `MainActivity.java`, so future tuning should stay targeted and performance-aware or extract only focused helpers.
- A focused rhyme regression check exists at `tools/rhyme_quality_check.py`, including `my/try`, `yours`, `out`, slang, phrase, and known bad-match cases.
- In-app update metadata now points to the 20.x appcast, but install flow still needs device verification.
- The 20.6 appcast was recreated after the prior JSONBlob manifest expired; durable hosting is still needed.

## Current Development Priority

Preserve the faster 20.6 rhyme row and verify premium UI behavior on device.

## Next Milestone

Next: device validation and targeted extraction of rhyme/controller helpers if performance profiling shows a need.
