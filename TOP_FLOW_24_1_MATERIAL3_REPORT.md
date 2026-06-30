# The Top Flow 24.1 Material 3 Compose Report

Date: 2026-06-29

## Summary

24.1 replaces the live note-taking host with a Kotlin `ComponentActivity` using `setContent` and a Material 3 Compose UI. The goal was to move the notes grid, search, editor, and visible note text onto a sharper Pixel-style foundation: real Compose text, stable lazy grid rendering, off-main work, debug jank labels, Baseline Profile generation, and screenshot coverage.

This build is packaged locally only. JSONBlob/appcast/update manifests were not changed.

## What Changed

- Added `MainActivity.kt` as the active Compose host with `enableEdgeToEdge`, lifecycle-aware state collection, `NotesTheme`, and debug-only JankStats.
- Added one `NotesTheme` built on Material 3 color scheme, typography, shapes, dynamic color, dark mode, and `sp` text sizing.
- Added immutable `NoteUi` / `RecordingUi` models and a `NotesViewModel` exposing `StateFlow<NotesUiState>`.
- Added a Compose notes grid/editor in `NotesScreens.kt`, using `LazyVerticalStaggeredGrid` with stable note keys/content types, `Text`, and `BasicTextField`.
- Kept storage compatible with the existing `notes.json` structure and moved note IO, search/sort, autosave, and rhyme lookup away from the main thread.
- Removed the old Java/View launcher files from active source and preserved them in `legacy/` for reference during the remaining feature ports.
- Added Macrobenchmark coverage for startup, note-grid scroll, open note, type text, and search notes.
- Generated and shipped `app/src/release/generated/baselineProfiles/baseline-prof.txt`.
- Added screenshot tests for note cards and editor in light/dark mode at font scale 1.0 and 1.3.

## Hard Requirement Status

- Requirements 1-14 are implemented in code and build/test infrastructure.
- Requirement 15 still needs Android Studio Layout Inspector on the target runtime to verify recomposition counts during scroll and typing.
- Physical 120 Hz Pixel frame-budget validation still needs to run on Dave's device; the local emulator was usable for screenshot tests and Baseline Profile generation but became unstable during later Macrobenchmark measurement attempts.

## Verification

- `assembleDebug`: passed.
- `assembleRelease`: passed cleanly after removing an optional invalid startup-profile artifact.
- `app:connectedDebugAndroidTest`: passed all 8 screenshot tests on the local Android 15 emulator before the emulator became unstable.
- `app:generateReleaseBaselineProfile`: generated the release Baseline Profile.
- Active source scan found no `drawText`, bitmap/canvas text, `RenderEffect`, blur, text scale transforms, `TextView`, or `EditText` usage in `app/src/main`.
- `tools\rhyme_quality_check.py`: passed.

## Package

- Release APK: `releases/the-top-flow-24.1-final.apk`
- Desktop copy: `C:\Users\blowb\Desktop\TopFlowUIalphabuilds\the-top-flow-24.1.apk`
- Size: 19,435,511 bytes
- SHA-256: `1095F445630F8546ABD8979F285AA985E8E237E8A04BA905AED655F2FE0C177C`

## Important Caveat

24.1 is a strong foundation for the polished Material 3 Pixel note-taking experience, but it is not a complete feature parity port of the full 24.0 Java UI. Recording/playback/settings/update flows remain preserved in legacy source and should be ported into Compose before this line is published as the appcast update.

The earlier repo file `releases/the-top-flow-24.1.apk` was locked by Windows during packaging and still contains the pre-clean APK. Use `releases/the-top-flow-24.1-final.apk` or the Desktop copy for the final 24.1 build.

## Next Steps

1. Run Layout Inspector recomposition checks for grid scroll and editor typing.
2. Run the Macrobenchmark suite on Dave's Pixel at 120 Hz and inspect JankStats labels for `NotesGrid`, `NoteEditor`, `Search`, and `CreateNote`.
3. Port recording/playback/settings/update surfaces into the Compose host without reintroducing bitmap text, blur around text, or text scaling transforms.
