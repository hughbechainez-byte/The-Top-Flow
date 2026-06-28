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
- 21.0 studio shell with a distinct top toolbar, global dock, premium editor/rhyme panel surfaces, polished note list, and empty-note state.
- Rhyme suggestion chips near the cursor, backed by a dedicated `RhymeEngine`, generated offline `rhyme_index.tsv`, pronunciation-first CMU ranking, quality buckets, curated writing examples, async fast-row generation, loading-state gating, and fallback phonetic heuristics for genuine unknowns.
- Rhyme settings for strictness, maximum suggestions, rhyme row visibility, exact-only mode, slang inclusion, and removed suggestions.
- Song attachment and playback controls.
- Voice recording, playback, rename, and save-to-disk support.
- App update checks through an online appcast manifest.

## Current Known Issues

- Most app behavior still lives in one large `MainActivity.java`, so changes outside rhyme/UI should remain carefully scoped.
- The 21.0 rhyme system is now extracted into `RhymeEngine.java`, but legacy rhyme helpers still remain in `MainActivity.java` and should be removed only after device validation.
- A focused rhyme regression check exists at `tools/rhyme_quality_check.py`, including `my/try`, `yours`, `out`, slang, phrase, and known bad-match cases.
- In-app update metadata now points to the 20.x appcast, but install flow still needs device verification.
- The 20.6 appcast was recreated after the prior JSONBlob manifest expired; durable hosting is still needed.

## Current Development Priority

Validate the 21.0 bottom-up rebuild on Pixel 10 Pro, especially rhyme readiness, keyboard/caret behavior, playback controls, and recording controls.

## Next Milestone

Next: device validation, durable update hosting, and optional cleanup of unused legacy rhyme helpers in `MainActivity.java`.
