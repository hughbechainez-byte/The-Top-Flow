# Project Status

Last updated: 2026-06-28

## App Name

The Top Flow, also referred to as Top Flow / Rhyming Notes.

## Purpose

Native Android songwriting notes app for GrapheneOS/Android. The app helps capture lyrics, attach song/voice references, and surface rhyme suggestions while writing.

## Current Known Features

- Local note list and lyric editor.
- Per-note title, body, font, note color, text color, and accent color.
- Per-note editor font size, note glow toggle, and glow strength.
- Resource-backed design system foundation: colors, dimensions, typography styles, panel/button/chip/editor drawables, and theme tokens.
- Existing Java UI helpers consume the design system for major surfaces, buttons, chips, sheets, spacing, typography, and editor presentation.
- 21.0 studio shell with a distinct top toolbar, global dock, premium editor/rhyme panel surfaces, polished note list, and empty-note state.
- Rebuild A foundation after 21.0: Kotlin, Compose, Material 3, dynamic dark theme support, Java-native UI kit helpers, and 21.x resource tokens are present for the next UI migration stage.
- Rebuild B wires current Java notes/media/rhyme/font surfaces into the 21.x UI kit, including live font preview rows and rebuilt rhyme row/card controls.
- Rebuild C moves the command dock to the bottom, tightens the top bar, shifts live screens to OLED/indigo/mint surfaces, and softens sheets/backdrop styling.
- Rhyme suggestion chips near the cursor, backed by a dedicated `RhymeEngine`, generated offline `rhyme_index.tsv`, pronunciation-first CMU ranking, quality buckets, curated writing examples, async fast-row generation, loading-state gating, and fallback phonetic heuristics for genuine unknowns.
- 21.2 rhyme hot path caches pronunciation/family info, avoids full-note internal-rhyme scanning for fast-row suggestions, bounds expanded candidate scoring, and avoids dismissing/recreating the caret popup on cache misses.
- 21.3 moves expanded rhyme button lookup off the UI thread, defers full-note body copying until draft save, reuses caret popup measurements/chips, disables expensive editor text services where safe, and adds `rhyme_trace` runtime logs.
- Rhyme settings for strictness, maximum suggestions, rhyme row visibility, exact-only mode, slang inclusion, and removed suggestions.
- Song attachment and playback controls.
- Voice recording, playback, rename, and save-to-disk support.
- App update checks through an online appcast manifest.

## Current Known Issues

- Most app behavior still lives in one large `MainActivity.java`, so changes outside rhyme/UI should remain carefully scoped.
- The 21.0 rhyme system is now extracted into `RhymeEngine.java`, but legacy rhyme helpers still remain in `MainActivity.java` and should be removed only after device validation.
- A focused rhyme regression check exists at `tools/rhyme_quality_check.py`, including `my/try`, `yours`, `out`, slang, phrase, and known bad-match cases.
- In-app update metadata points to the current JSONBlob appcast, but install flow still needs device verification on Pixel 10 Pro.
- JSONBlob/temp.sh are temporary hosts; durable update hosting is still needed.
- Rebuild B is still a Java-runtime bridge; the live app is not fully Compose yet.

## Current Development Priority

21.3 release packaging is complete; next priority is Pixel 10 Pro validation of rhyme-button latency, keyboard smoothness, and `rhyme_trace` log review.

## Next Milestone

Next: device validation and targeted follow-up polish from Pixel logs/screenshots.
