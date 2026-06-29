# The Top Flow 21.5 UI Brief

## App Summary

The Top Flow is a native Android songwriting notes app for GrapheneOS/Android. It keeps lyric notes offline, supports per-note styling, song attachment, voice capture, and an offline pronunciation-first rhyme engine with fast suggestions while typing.

## Most Recent Update

Version 21.4 made the 21.x UI more fluid and OLED-focused: it removed the dark-blue middle panel layer, kept the main shell true black, added a visible note-card halo, improved bottom-sheet swipe dismissal, dimmed/scaled the background behind sheets, expanded offline font choices, and prewarmed common rhyme caches during startup.

## Three Small Implementations For 21.5

1. Make bottom sheets scroll-safe on Pixel-sized screens.
   - Sheet content should not overflow off-screen.
   - Keep the drag handle/header visible.
   - Put long sheet content inside a capped-height scroll area.
   - Preserve tap-outside-to-dismiss and drag-to-dismiss.

2. Add active state feedback to the bottom dock.
   - Notes, Rhyme, Style, and Settings should visually respond when active/opened.
   - Keep animations lightweight: alpha, scale, translation only.
   - Do not make the dock taller or cause keyboard/rhyme row instability.

3. Improve selected-note feedback in the note list.
   - The currently open note should be visibly distinct when returning to Notes.
   - Use a subtle neon edge/halo or selected row state.
   - Do not change note storage or list ordering.

## Hard Rules

- Do not change rhyme scoring quality.
- Do not update JSONBlob or the live update manifest.
- Do not edit `appcast.json` or `releases/appcast.json`.
- Keep the app usable offline.
- Keep changes scoped to UI and local version/docs as needed.
- Run `python tools/rhyme_quality_check.py`.
- Run `assembleRelease` if practical.
