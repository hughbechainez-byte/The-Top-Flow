# Handoff

## Project Summary

The Top Flow is a native Android songwriting notes app. It is still a single-activity Java app for core workflows, but 21.0 extracts rhyme logic into a dedicated offline `RhymeEngine`, adds a generated pronunciation index asset, and replaces the visible shell with a more distinct premium studio toolbar/dock/editor/rhyme panel structure.

Post-21.0 Rebuild A adds the foundation for the next UI rebuild: Kotlin, Compose, Material 3, dynamic dark theming, 21.x resource tokens, and Java-native presentation helpers. Rebuild B wires existing Java notes/media/rhyme/font surfaces into that foundation. Rebuild C applies the live composition pass: bottom dock, compact top bar, OLED/indigo/mint surfaces, quieter rows, softer backdrop, and polished sheets.

21.2 focuses on real device pain points: cached rhyme info/family scoring, fast-row cache stability, bounded expanded scoring, less caret-popup churn, true OLED black outer shell, no notebook editor lines, per-note font size/color/glow controls, and swipe-down bottom sheets.

21.3 fixes the remaining real-device lag path by making expanded rhyme button lookup asynchronous, deferring full-note body copies until draft save, reusing caret popup/chip work, opting the lyric editor out of expensive text services where safe, and adding `rhyme_trace` logs for every UI/background segment.

21.4 focuses on fluid OLED polish: it removes the dark-blue middle background layer, keeps the app shell true black, adds a real note-card halo behind the editor, makes bottom-sheet swipe dismissal forgiving across the upper sheet area, dims/scales the background while sheets are open, adds more offline font choices, and prewarms common rhyme caches during the startup screen.

21.5 is a focused UI continuation: long bottom-sheet content now scrolls under a fixed handle/header, bottom dock buttons show the active sheet/state, and the currently open note has clearer selected feedback when returning to the Notes list. This build is packaged for temp-host validation only; JSONBlob/appcast manifests were intentionally left unchanged.

21.6 through 22.0 continue the UI-alpha run with blur-backed sheets, solid OLED panels, bidirectional editor/Notes edge gestures, velocity-aware swipe completion, swipe rail affordances, compact recent-session previews, and a cleaner Main Menu command surface. These builds were packaged and temp-hosted only; JSONBlob/appcast manifests were intentionally left unchanged.

The active milestone is now 22.1 to 23.0: a premium UI transformation. The chosen architecture is a staged Compose-led shell migration rather than a one-shot rewrite. Preserve Java note storage, `RhymeEngine`, recording/playback, and install/update behavior while moving visible UI surfaces into richer Compose or Compose-hosted layers.

22.1 adds multi-version update discovery. The app now accepts both the legacy single-update appcast shape and a new `versions[]` shape. When multiple newer versions are available, it shows an OLED bottom-sheet chooser. Appcasts should keep top-level latest-version fields for older 22.0 clients and include `versions[]` for 22.1+ clients.

22.2 begins the Compose-led shell migration. A Java-callable Compose bridge now mounts a true-black premium studio backdrop behind the existing Java UI, with crisp neon rails, waveform marks, and fine grid details. Existing note, editor, rhyme, recording/playback, gesture, and update behavior remain preserved.

22.3 rebuilds the Notes home into a premium session dashboard. Notes now present stronger current-session context, richer cards, accent rails, compact metadata/previews, and crisp signal markers built from Views. Note creation, opening, persistence, editor behavior, rhyme, media, and update handling remain unchanged.

22.4 upgrades the editor presentation into a Draft Studio surface. It adds editor chrome, accent rail, compact session metadata, note-accent signal detail, and refined title/body field surfaces. Text watchers, save debounce, IME flags, cursor/rhyme popup behavior, storage, media, and updates remain preserved.

## Build / Run

Known build command from `README.md`:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$env:ANDROID_HOME="$PWD\android-sdk"
$env:ANDROID_SDK_ROOT="$PWD\android-sdk"
tools\gradle-8.10.2\bin\gradle.bat assembleRelease
```

Latest verified build: 22.4 / versionCode 50 passed `tools\rhyme_quality_check.py` and `assembleRelease`.

## Important Files

- `README.md`: basic project name, current local build, release build command, and update hosting notes.
- `settings.gradle`: Gradle project name is `The Top Flow`; includes `:app`.
- `build.gradle`: Android application plugin version.
- `app/build.gradle`: Android app module configuration, package namespace, SDK versions, signing config, version properties, and current update manifest URL.
- `app/src/main/AndroidManifest.xml`: permissions, app label/icon/theme, file provider, and launcher activity.
- `app/src/main/java/com/davehq/thetopflow/MainActivity.java`: main app UI and behavior, including notes, the 21.0 studio shell/dock/editor/rhyme panel, async rhyme row calls, recordings, song playback, update checks, persistence, and styling.
- `app/src/main/java/com/davehq/thetopflow/RhymeEngine.java`: dedicated offline pronunciation-first rhyme engine with readiness state, indexed candidate lookup, scoring, result cache, slang handling, and fallback gating.
- `app/src/main/kotlin/com/davehq/thetopflow/ui/TopFlowUiFoundation.kt`: Compose/Material 3 design foundation and reusable future UI components.
- `app/src/main/java/com/davehq/thetopflow/TopFlowUiKit.java`: Java-native presentation helper for gradual migration from existing programmatic views.
- `app/src/main/assets/rhyme_index.tsv`: generated offline pronunciation/rhyme index derived from CMU to reduce runtime indexing work.
- `REBUILD_21_UI_FOUNDATION.md`: Rebuild A scope notes and next-section handoff.
- `app/src/main/res/values/colors.xml`, `dimens.xml`, `styles.xml`, and `drawable/*`: reusable design system foundation, pressed states, panel/button/chip backgrounds, and lightweight button icons.
- `app/src/main/assets/cmudict.dict`: CMU pronunciation dictionary used by rhyme suggestions.
- `tools/rhyme_quality_check.py`: focused rhyme regression and timing check for the rhyme engine, including `my/try`, `yours`, `out`, `downtown`, `eyesight`, `rol`, slang, phrase, and weak-match exclusions.
- `appcast.json` and `releases/appcast.json`: update manifest files.

## Current Next Task

Implement 22.5 real font and asset library. Add meaningful local resources only; do not bloat APK with useless files.

## Assumptions

- The app is intended to stay lightweight and native.
- Local note data is stored as `notes.json` in app private files.
- Rhyme suggestion behavior should continue improving without disturbing notes, recordings, styling, song playback, or updates.
- Version 22.1 is the first build published to the 22.0 JSONBlob/appcast line and should be represented in both top-level latest fields and the `versions[]` list.
- Version 22.2 adds the first live Compose-backed UI layer and should remain in the multi-version appcast list after later 22.x releases.
- Version 22.3 upgrades the Notes home and should remain in the multi-version appcast list after later 22.x releases.
- Version 22.4 upgrades the editor surface and should remain in the multi-version appcast list after later 22.x releases.
- Version 22.0 APK is temp-hosted at `https://temp.sh/xXCOu/the-top-flow-22.0.apk`; it was not published through JSONBlob/appcast during the 21.6 to 22.0 UI-alpha run.
- Version 21.5 APK is temp-hosted for validation only at `https://temp.sh/ZIRmO/the-top-flow-21.5.apk`; do not publish it through JSONBlob/appcast until directed.
- Version 21.4 APK is published at `https://temp.sh/Jawft/the-top-flow-21.4.apk`.
- Version 21.4 uses JSONBlob manifest `019f0d91-7b07-768c-a38a-dacd0a9b84df`; JSONBlob/temp.sh are temporary hosts, so durable update hosting is still needed.
- Version 21.3 APK was published at `https://temp.sh/SZvCg/the-top-flow-21.3.apk`.
- Version 21.2 APK was published at `https://temp.sh/nTEYR/the-top-flow-21.2.apk`.

## Warnings / Unknowns

- The Git worktree already had modified and untracked files before this framework setup.
- `MainActivity.java` is large, so future changes should use targeted searches and focused edits.
- Rhyme checks now run through `python tools\rhyme_quality_check.py`; 20.4 also passed a release build.
- 20.3 moves fast rhyme-row generation off the UI thread with debounce, stale-job cancellation, an LRU query cache, bounded fast-row candidate scoring, and timing logs.
- 20.4 is a UI-only polish release; do not treat it as a rhyme-engine change.
- 20.5A adds reusable design tokens and drawable assets only; the runtime UI still needs phase 20.5B integration.
- 20.5B wires those tokens into the existing Java UI helpers and repeated surfaces without changing rhyme, editor, storage, playback, or recording behavior.
- 20.5C adds visual polish only: resource-backed pressed states and lightweight button icons. It does not change app behavior.
- 20.5D is the release step for the 20.5 milestone.
- 20.6 blocks fallback suggestions during CMU/index loading, adds an explicit loading chip, tunes `out` rhyme ordering, and refreshes the editor/list presentation without changing app workflows.
- 21.0 extracts the rhyme engine, adds a generated offline rhyme index, and introduces a distinct studio toolbar/dock/editor shell. Legacy rhyme helper code remains in `MainActivity.java` until device validation confirms the extraction is stable.
- Rebuild A has passed `tools\rhyme_quality_check.py` and `assembleRelease`; the build needed higher Gradle heap/metaspace settings after adding Compose.
- Rebuild B has passed `tools\rhyme_quality_check.py` and `assembleRelease`; no release/appcast work was done.
- Rebuild C has passed `tools\rhyme_quality_check.py` and a clean `assembleRelease`; no release/appcast work was done.
- 21.1 release packaging updated version metadata, local/live appcast, temp.sh APK, and release artifact.
- 21.2 addresses rhyme latency and UX issues from device logs without changing notes, recording, playback, storage, or rhyme-quality rules.
- 21.3 addresses the UI-thread lag path left after 21.2; Pixel was not visible to WSL ADB during release, so fresh on-device `rhyme_trace` logs are still needed.
- 21.4 addresses OLED UI fluidity, bottom-sheet usability, and startup rhyme preload without changing rhyme scoring quality.
- 21.5 addresses sheet scroll safety, active dock feedback, and selected-note feedback without changing rhyme scoring quality or live update manifests.
- 21.6 through 22.0 address continued OLED UI polish, gesture discoverability, velocity-aware swipes, Main Menu recent-session context, and shared motion polish without publishing update manifests.
- 22.1 must convert update handling to support multiple available versions while preserving legacy single-version manifest compatibility.
- 22.2 should begin the visible Compose-led premium shell foundation while keeping the Java workflows intact.
- 22.3 should make the Notes home materially more premium while keeping note persistence unchanged.
- 22.4 should make the editor surface feel premium without touching input hot paths.
- 22.5 should add real fonts/assets with clear value and acceptable licensing.
- Release signing and APK artifacts exist locally; avoid touching them unless the task is explicitly about releases.
