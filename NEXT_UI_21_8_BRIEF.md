# The Top Flow 21.8 UI Brief

## App Summary

The Top Flow is a native Android songwriting notes app for GrapheneOS/Android. It keeps lyric notes offline, supports per-note styling, song attachment, voice capture, and an offline pronunciation-first rhyme engine with fast suggestions while typing.

## Most Recent Update

Version 21.7 started the recent-notes main menu overhaul: the Notes screen now has a crisp OLED command header, recent rows show local line/word metadata, menu text metrics were tightened, and the reverse gesture opens the current note from the Notes menu with a right-edge leftward swipe.

## Overall Direction Toward 22.0

Continue the UI overhaul toward a high-resolution minimalist OLED neon interface that feels quick and responsive at high refresh rates. Avoid grainy text, low-resolution strokes, noisy effects, or heavy layout churn. Gesture commands, premium menu presentation, crisp transitions, and a stronger recent-notes main menu are the north star.

## Three Small Implementations For 21.8

1. Make main workflow swipes velocity-aware.
   - Add velocity tracking to the editor-to-Notes and Notes-to-editor swipe gestures.
   - Trigger completion on either enough distance or a clear fast horizontal flick.
   - Preserve vertical-scroll conflict safety and typing safety.

2. Smooth panel settle and screen-swap transitions.
   - Make cancelled swipes settle back cleanly without leaving alpha, scale, or translation residue.
   - Make completed gestures and dock button screen changes use the same lightweight alpha/translation/scale language.
   - Keep animations short and high-refresh-friendly; no bitmap-heavy effects.

3. Add a compact current-session signal to the Notes menu.
   - Use existing local state only.
   - Add a non-instructional status chip or inline label showing the currently open note when one exists.
   - Keep text single-line, ellipsized, high contrast, and stable in narrow widths.

## Hard Rules

- Do not change rhyme scoring quality.
- Do not update JSONBlob or the live update manifest.
- Do not edit `appcast.json` or `releases/appcast.json`.
- Keep the app usable offline.
- Keep changes scoped to UI, local version metadata, and docs as needed.
- Bump local version to `21.8` and versionCode to `44`.
- Run `python tools/rhyme_quality_check.py`.
- Run `assembleRelease` if practical.
