# The Top Flow 21.7 UI Brief

## App Summary

The Top Flow is a native Android songwriting notes app for GrapheneOS/Android. It keeps lyric notes offline, supports per-note styling, song attachment, voice capture, and an offline pronunciation-first rhyme engine with fast suggestions while typing.

## Most Recent Update

Version 21.6 added the UI-alpha foundation for higher-end motion and clarity: hardware-backed blur behind bottom sheets on Android 12+, solid OLED panel helpers with restrained neon strokes, and a conflict-safe left-edge swipe from the editor back to Notes.

## Overall Direction Toward 22.0

Continue the UI overhaul toward a high-resolution minimalist OLED neon interface that feels quick and responsive at high refresh rates. Avoid grainy text, low-resolution strokes, noisy effects, or heavy layout churn. Gesture commands, premium menu presentation, crisp transitions, and a stronger recent-notes main menu are the north star.

## Three Small Implementations For 21.7

1. Start the recent-notes main menu overhaul.
   - Replace the plain Notes header/add-button stack with a crisp OLED menu command header.
   - Present the existing note list as recent writing sessions using the current order; do not add storage timestamps yet.
   - Show compact useful metadata already available locally, such as lyric line count and word count.
   - Keep rows minimalist, high-contrast, and readable; no grain, noisy glow, or bitmap effects.

2. Add the reverse gesture for the main workflow.
   - Support a lightweight horizontal swipe from the Notes menu back into the currently open note when one exists.
   - Keep it conflict-safe with vertical scrolling.
   - Use only alpha, translation, and scale.

3. Tighten menu text hierarchy for crisp high-resolution presentation.
   - Ensure menu labels, note titles, previews, and metadata use `includeFontPadding(false)` and zero letter spacing where created programmatically.
   - Keep type sizes stable; do not scale font sizes with viewport width.
   - Avoid text shadows on persistent menu/editor UI.

## Hard Rules

- Do not change rhyme scoring quality.
- Do not update JSONBlob or the live update manifest.
- Do not edit `appcast.json` or `releases/appcast.json`.
- Keep the app usable offline.
- Keep changes scoped to UI, local version metadata, and docs as needed.
- Bump local version to `21.7` and versionCode to `43`.
- Run `python tools/rhyme_quality_check.py`.
- Run `assembleRelease` if practical.
