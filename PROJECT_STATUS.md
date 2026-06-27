# Project Status

Last updated: 2026-06-27

## App Name

The Top Flow, also referred to as Top Flow / Rhyming Notes.

## Purpose

Native Android songwriting notes app for GrapheneOS/Android. The app helps capture lyrics, attach song/voice references, and surface rhyme suggestions while writing.

## Current Known Features

- Local note list and lyric editor.
- Per-note title, body, font, note color, text color, and accent color.
- Rhyme suggestion chips near the cursor, backed by `cmudict.dict`, pronunciation-first CMU ranking, common rhyme lists, and fallback phonetic heuristics.
- Rhyme settings for strictness, maximum suggestions, rhyme row visibility, exact-only mode, slang inclusion, and removed suggestions.
- Song attachment and playback controls.
- Voice recording, playback, rename, and save-to-disk support.
- App update checks through an online appcast manifest.

## Current Known Issues

- Most behavior lives in one large `MainActivity.java`, so changes should be especially small and carefully scoped.
- The rhyming system still lives inside `MainActivity.java`, so future tuning should stay targeted.
- A focused rhyme regression check exists at `tools/rhyme_quality_check.py`.
- In-app update metadata now points to the 20.x appcast, but install flow still needs device verification.

## Current Development Priority

Keep tuning rhyme quality against the focused regression set before unrelated app work.

## Next Milestone

Expand rhyme QA with more real writing examples and tune ranking without changing the UI.
