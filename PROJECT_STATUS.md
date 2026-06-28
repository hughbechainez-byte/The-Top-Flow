# Project Status

Last updated: 2026-06-28

## App Name

The Top Flow, also referred to as Top Flow / Rhyming Notes.

## Purpose

Native Android songwriting notes app for GrapheneOS/Android. The app helps capture lyrics, attach song/voice references, and surface rhyme suggestions while writing.

## Current Known Features

- Local note list and lyric editor.
- Per-note title, body, font, note color, text color, and accent color.
- Premium dark studio UI with layered native panels, polished controls, and pill-shaped rhyme chips.
- Rhyme suggestion chips near the cursor, backed by `cmudict.dict`, pronunciation-first CMU ranking, quality buckets, curated writing examples, async fast-row generation, and fallback phonetic heuristics.
- Rhyme settings for strictness, maximum suggestions, rhyme row visibility, exact-only mode, slang inclusion, and removed suggestions.
- Song attachment and playback controls.
- Voice recording, playback, rename, and save-to-disk support.
- App update checks through an online appcast manifest.

## Current Known Issues

- Most behavior lives in one large `MainActivity.java`, so changes should be especially small and carefully scoped.
- The rhyming system still lives inside `MainActivity.java`, so future tuning should stay targeted and performance-aware.
- A focused rhyme regression check exists at `tools/rhyme_quality_check.py`, including `my/try`, `yours`, slang, phrase, and known bad-match cases.
- In-app update metadata now points to the 20.x appcast, but install flow still needs device verification.

## Current Development Priority

Preserve the smoother 20.3 rhyme row while validating the 20.4 premium UI on device.

## Next Milestone

Device-test 20.4 on Pixel 10 Pro, then add more multi-syllable phrase-tail examples and tune expanded-mode slants without changing the UI.
