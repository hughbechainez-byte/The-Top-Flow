# The Top Flow 21.6 UI Brief

## App Summary

The Top Flow is a native Android songwriting notes app for GrapheneOS/Android. It keeps lyric notes offline, supports per-note styling, song attachment, voice capture, and an offline pronunciation-first rhyme engine with fast suggestions while typing.

## Most Recent Update

Version 21.5 made the 21.x UI more usable: long bottom-sheet content now scrolls while the handle/header stay visible, bottom dock buttons show active state feedback, and the currently open note is visibly selected in the Notes list.

## Overall Direction Toward 22.0

Continue the UI overhaul toward a high-resolution minimalist OLED neon interface that feels quick and responsive at high refresh rates. Avoid grainy text, low-resolution strokes, noisy effects, or heavy layout churn. Gesture commands, premium menu presentation, crisp transitions, and a stronger recent-notes main menu are the north star.

## Three Small Implementations For 21.6

1. Add a clean menu/sheet blur foundation.
   - When any bottom sheet/menu item is open, use a hardware-backed background blur on Android 12+ via `RenderEffect` where safe.
   - Keep the existing dim/scale fallback for older Android versions.
   - Always clear the blur when the sheet closes or during drag dismissal.

2. Sharpen core surfaces and text presentation.
   - Add reusable crisp OLED/neon surface helpers in `TopFlowUiKit` or nearby UI helpers.
   - Prefer solid, high-contrast OLED panels with restrained 1dp strokes over hazy or grain-like gradients.
   - Remove or avoid fuzzy text shadow effects on persistent UI; keep text anti-aliased, high contrast, and not negatively letter-spaced.

3. Add a first gesture command for the main workflow.
   - Support a lightweight horizontal swipe from the editor back to Notes.
   - Keep it conflict-safe with vertical scrolling and typing.
   - Use alpha/translation/scale only; do not add bitmap-heavy effects or change note storage.

## Hard Rules

- Do not change rhyme scoring quality.
- Do not update JSONBlob or the live update manifest.
- Do not edit `appcast.json` or `releases/appcast.json`.
- Keep the app usable offline.
- Keep changes scoped to UI, local version metadata, and docs as needed.
- Bump local version to `21.6` and versionCode to `42`.
- Run `python tools/rhyme_quality_check.py`.
- Run `assembleRelease` if practical.
