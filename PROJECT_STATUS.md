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
- No automated rhyme regression test suite exists yet.
- In-app update metadata now points to 20.0, but install flow still needs device verification.

## Current Development Priority

Validate the 20.0 pronunciation-first rhyme engine and add a small regression set for known bad/good rhyme pairs.

## Next Milestone

Add focused rhyme QA/regression coverage and tune slant-rhyme ranking from real writing examples.
