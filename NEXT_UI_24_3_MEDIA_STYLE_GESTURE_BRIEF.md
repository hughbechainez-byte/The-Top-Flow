# The Top Flow 24.3 Media, Style, Gesture Restoration Brief

Date: 2026-06-30

## Role And Working Model

Codex 5.3 is the implementation owner for this pass. Treat the current assistant as the senior lead. Implement directly in the repository, keep changes focused, do not revert unrelated files, and preserve the 24.2 note data-safety protections.

## Goal

Restore the major user-facing features that were present in the older Java build but missing after the 24.1 Compose rewrite:

- Voice recording and recording playback.
- Attach and play an audio track for a note.
- Color, font, font-size, and accent-color editing with preview-first UI before committing.
- Fluid broad swipe gestures for common note navigation.
- Keyboard dismissal on button/menu actions, with menus and options remaining visible when the keyboard is open.

This release should be version `24.3`, version code `70`.

## Current Foundation

- Active host: `app/src/main/kotlin/com/davehq/thetopflow/MainActivity.kt`
- ViewModel/state: `app/src/main/kotlin/com/davehq/thetopflow/NotesViewModel.kt`
- Model/storage: `app/src/main/kotlin/com/davehq/thetopflow/data/NotesRepository.kt`
- Compose UI: `app/src/main/kotlin/com/davehq/thetopflow/ui/NotesScreens.kt`
- Theme/fonts: `app/src/main/kotlin/com/davehq/thetopflow/ui/NotesTheme.kt`
- Legacy reference: `legacy/MainActivity.java.disabled`

The current note JSON already contains the needed fields:

- `font`
- `fontSizeSp`
- `noteColor`
- `textColor`
- `accentColor`
- `songUri`
- `recordings[]`

Do not change the JSON schema unless absolutely necessary. Existing old notes must continue to parse.

## Non-Negotiable Safety Constraints

1. Do not weaken the 24.2 note save protections:
   - No save before notes have loaded.
   - Empty in-memory state must not overwrite an existing non-empty `notes.json`.
   - Existing non-empty `notes.json` must be copied to `notes.backup.json` before future saves.
   - Empty primary notes file should fall back to non-empty backup.
2. Do not perform note file IO on the main thread.
3. Do not store Android `MediaPlayer` or `MediaRecorder` objects in Compose UI functions.
4. Release/pause media cleanly on lifecycle pause/clear.
5. Keep text rendering in Compose `Text`/`BasicTextField`; no bitmap/canvas text.
6. Do not add scale effects to components containing text.
7. Avoid blur/offscreen compositing around text.
8. All note edits must save through the existing ViewModel/repository path.

## Implementation Plan

### 1. Version Bump

Update `app/build.gradle` defaults:

- `TOP_FLOW_VERSION_CODE` default: `70`
- `TOP_FLOW_VERSION_NAME` default: `24.3`

Leave `TOP_FLOW_DEBUGGABLE` default as `false`.

### 2. Media Runtime Layer

Add a Kotlin runtime helper, suggested file:

- `app/src/main/kotlin/com/davehq/thetopflow/TopFlowMediaController.kt`

Responsibilities:

- Own `MediaPlayer` for attached song playback.
- Own `MediaPlayer` for saved recording playback.
- Own `MediaRecorder` for active voice recording.
- Use `Application` or app context only.
- Expose a `StateFlow<MediaUiState>`.
- Provide functions:
  - `attachSong(uri: String, onPersisted: () -> Unit)` can be handled in ViewModel after Activity grants URI permission; controller only needs to play the URI.
  - `toggleSong(songUri: String)`
  - `seekSong(songUri: String, positionMs: Int)`
  - `startRecording(outputDir: File, optionalSongUri: String?)`
  - `stopRecording(save: Boolean): File?`
  - `playRecording(path: String)`
  - `stopRecordingPlayback()`
  - `pauseAll()`
  - `release()`
- Auto-start the attached song when recording starts, matching the older build behavior.
- Pause/stop song and recording playback so they do not overlap.
- Track:
  - `songPlaying`
  - `songPositionMs`
  - `songDurationMs`
  - `recordingActive`
  - `recordingElapsedMs`
  - `recordingPlaybackPath`
  - `recordingPlaybackActive`
  - `mediaErrorMessage`
- Run a lightweight ticker while audio is playing or recording, roughly every 500 ms.
- Use `MediaRecorder` API compatible with minSdk 26.
- Output recordings to `File(application.filesDir, "recordings")`, with names like `top-flow-yyyyMMdd-HHmmss.m4a`.

### 3. ViewModel Media And Style Actions

Extend `NotesUiState` with `media: MediaUiState`.

Add ViewModel functions:

- `attachSong(uri: String)`
- `toggleSong()`
- `seekSong(positionMs: Int)`
- `startRecording()`
- `stopRecording(save: Boolean)`
- `playRecording(path: String)`
- `renameRecording(path: String, tag: String)`
- `exportRecording(path: String): Boolean`
- `updateNoteStyle(font: String, fontSizeSp: Int, noteColor: Int, textColor: Int, accentColor: Int)`
- `pauseMediaPlayback()`

Details:

- `attachSong` updates the selected note `songUri`, saves, and refreshes media state.
- `startRecording` should require a selected note. Permission is requested by Activity/UI before calling.
- `stopRecording(save = true)` should add a `RecordingUi(path, tag)` at the top of the selected note recordings list and append a body marker like `[Voice note: top-flow-...m4a]`.
- `stopRecording(save = false)` should delete the temp file if present.
- `renameRecording` updates only the tag for the matching recording path and saves.
- `exportRecording` should copy the recording to `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` under `Music/The Top Flow` on Android Q+; return false for unsupported/missing source.
- `updateNoteStyle` updates all style fields at once from the preview UI and saves.
- `pauseMediaPlayback` should be called by `MainActivity.onPause`.

### 4. Activity Result And Permission Wiring

Update `MainActivity.kt`:

- Register an `ActivityResultContracts.OpenDocument()` launcher for `audio/*`.
- On result:
  - Persist read permission using `takePersistableUriPermission`.
  - Call `notesViewModel.attachSong(uri.toString())`.
- Register an `ActivityResultContracts.RequestPermission()` launcher for `Manifest.permission.RECORD_AUDIO`.
- If permission is granted after request, call `notesViewModel.startRecording()`.
- Pass callbacks into `NotesRoute`:
  - `onAttachSong`
  - `onToggleSong`
  - `onSeekSong`
  - `onStartRecording`
  - `onStopRecording`
  - `onPlayRecording`
  - `onRenameRecording`
  - `onExportRecording`
  - `onApplyStyle`
- In `onPause`, call both `notesViewModel.flushPendingSave()` and `notesViewModel.pauseMediaPlayback()`.

### 5. Compose UI: Media Controls

Update `NotesScreens.kt`.

In the editor screen, add a polished media section below note metadata and before rhymes/body:

- Track card:
  - Shows attached/no attached state.
  - Buttons: Attach, Play/Pause.
  - Slider for seek when duration is known.
  - Time label `m:ss / m:ss`.
  - Disable play/seek if no song attached.
- Voice card:
  - Shows recording status.
  - Buttons:
    - Record
    - Stop + Save while recording
    - Cancel while recording
  - Saved recordings list:
    - tag/title
    - Play/Stop button
    - Rename action
    - Export action
  - Rename should use a Compose dialog with a text field, Cancel, Save.

UI rules:

- Hide keyboard/focus before every media/style/delete/navigation button action.
- Use `imePadding()` and/or `BringIntoViewRequester`/scroll so controls are not hidden behind keyboard.
- Keep panels compact and Material 3 Pixel-like, not bulky marketing cards.
- Use pure black background and crisp accent strokes.

### 6. Compose UI: Style Editor

Add a preview-first style editor, preferably a `ModalBottomSheet`.

Entry point:

- Add a `Style` or palette button in the editor toolbar.

Behavior:

- Opening the sheet copies the selected note style into local draft state.
- All controls update only the draft and preview.
- `Apply` calls `onApplyStyle(...)` once and dismisses.
- `Cancel` dismisses without changing the note.

Controls:

- Font selector:
  - Space Grotesk
  - Sans
  - Serif
  - Monospace
  - Share Tech Mono
  - Silkscreen
- Font size slider/input, clamped 14-28 sp.
- Color target selector:
  - Page
  - Text
  - Accent
- For selected color target:
  - Swatch grid with common useful colors.
  - RGB sliders or equivalent precise controls.
  - Hex readout.
- Preview:
  - Shows note title/body sample using draft `noteColor`, `textColor`, `accentColor`, `font`, and `fontSizeSp`.
  - Preview must be visible before choosing Apply.

Font implementation:

- Add a helper in `NotesTheme.kt` or a new file to map note font IDs to Compose `FontFamily`.
- Use bundled resources where available:
  - `space_grotesk`
  - `share_tech_mono_regular`
  - `silkscreen`
- Fall back safely to system `FontFamily.SansSerif`, `Serif`, and `Monospace`.
- Use the selected font family for note cards, editor title/body, and style preview.

### 7. Compose UI: Actual Style Application

Apply note style in real surfaces, not only preview:

- Note cards should show the note accent and a subtle page/color reference.
- Editor body/title surface should use:
  - note page color as the writing surface or accent panel.
  - note text color for editable text where contrast is reasonable.
  - note accent for rails/buttons/strokes.
  - selected font and font size.

Keep readability above decoration. If text color and page color are too close, use a fallback contrast color for placeholder/metadata but keep the user's chosen text color for editor content.

### 8. Fluid Swipe Gestures

Restore broad, intuitive swipe navigation:

- Editor:
  - A right swipe should close editor back to Notes.
  - It must not require pressing on a thin edge line.
  - Accept gesture starts from at least the left 40% of the editor or from any non-text chrome area.
- Notes grid:
  - A left swipe should reopen the most recently active note if one exists; otherwise open the newest note.
  - It must not require pressing a margin line.
- Gesture should ignore mostly vertical scrolls.
- Trigger if horizontal distance passes around 96 dp or velocity is high.
- Add subtle drag translation/rail feedback during the gesture, but do not use scale on text.
- Do not break vertical scrolling or text editing.

Suggested implementation:

- Add a reusable `Modifier.topFlowHorizontalSwipe(...)` helper in `NotesScreens.kt` or a small new UI file.
- Track horizontal drag only when `abs(dx) > abs(dy) * 1.25`.
- Use `offset { IntOffset(dragOffset.roundToInt(), 0) }` for transient translation, not `scale`.
- Reset offset if threshold is not met.

### 9. Keyboard And IME Safety

Add a reusable action wrapper in Compose:

- `val focusManager = LocalFocusManager.current`
- `val keyboard = LocalSoftwareKeyboardController.current`
- Before button/menu callbacks:
  - `focusManager.clearFocus(force = true)`
  - `keyboard?.hide()`

Use it for:

- Back/Notes
- Delete
- Style
- Attach
- Play/Pause
- Record/Stop/Cancel
- Rename/Export
- Apply/Cancel sheet actions
- New note
- Any bottom bar/menu action

Ensure the editor layout uses a vertically scrollable container plus `imePadding()` so media/style controls are not obscured by the keyboard.

### 10. Tests And Verification

Minimum required:

- `./gradlew.bat :app:assembleRelease "-PTOP_FLOW_VERSION_CODE=70" "-PTOP_FLOW_VERSION_NAME=24.3"`
- If unit/instrumented tests are not practical, at least run compile and lint-vital via assemble.
- Confirm APK exists and size is nonzero.
- Confirm manifest JSON validates after publication.

Manual/device sanity through WSL ADB if available:

- Install the release APK with `adb install -r`.
- Confirm package reports `versionName=24.3`, `versionCode=70`.
- Launch app.
- Confirm no startup crash.

### 11. Release Packaging

After senior review:

- Copy APK to `releases/the-top-flow-24.3-media-style-gestures.apk`.
- Commit code, appcast, and APK.
- Push to GitHub `the-top-flow` and `main`.
- Publish JSONBlob appcast:
  - `versionName`: `24.3`
  - `versionCode`: `70`
  - `apkUrl`: `https://raw.githubusercontent.com/hughbechainez-byte/The-Top-Flow/main/releases/the-top-flow-24.3-media-style-gestures.apk`
  - Insert 24.3 first in `versions[]`.
  - Keep 24.2 immediately after it.
- Verify raw APK URL returns HTTP 200.
- Verify JSONBlob returns 24.3 first.

## Acceptance Criteria

- A user on 24.2 can update to 24.3 from inside the app.
- 24.3 preserves note data and keeps 24.2 save protections.
- Existing notes with legacy `songUri` and `recordings` fields still parse.
- User can attach a song, play/pause it, and seek.
- User can record voice, save/discard it, play saved recordings, rename them, and export them.
- User can open a style editor, preview font/color/accent changes, cancel safely, or apply once.
- Swiping does not require exact edge hits.
- Buttons hide the keyboard before running actions.
- Editor controls are reachable when the keyboard is open.
- Release APK builds and installs without a crash.
