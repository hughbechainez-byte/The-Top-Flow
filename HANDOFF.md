# Handoff

## Project Summary

The Top Flow is a native Android songwriting notes app. It is still a single-activity Java app for core workflows, but 21.0 extracts rhyme logic into a dedicated offline `RhymeEngine`, adds a generated pronunciation index asset, and replaces the visible shell with a more distinct premium studio toolbar/dock/editor/rhyme panel structure.

Post-21.0 Rebuild A adds the foundation for the next UI rebuild: Kotlin, Compose, Material 3, dynamic dark theming, 21.x resource tokens, and Java-native presentation helpers. Runtime workflows are not migrated yet.

## Build / Run

Known build command from `README.md`:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$env:ANDROID_HOME="$PWD\android-sdk"
$env:ANDROID_SDK_ROOT="$PWD\android-sdk"
tools\gradle-8.10.2\bin\gradle.bat assembleRelease
```

Latest verified build: 21.0 / versionCode 36 passed `tools\rhyme_quality_check.py` and `assembleRelease`.

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
- `tools/rhyme_quality_check.py`: focused rhyme regression check for the 20.x rhyme engine, including real writing examples for `my/try`, `yours`, `out`, slang, phrase, and weak-match exclusions.
- `appcast.json` and `releases/appcast.json`: update manifest files.

## Current Next Task

Wait for approval, then start Rebuild B: wire notes, recording, playback, font preview selection, and the all-offline rhyme bar into the new UI foundation.

## Assumptions

- The app is intended to stay lightweight and native.
- Local note data is stored as `notes.json` in app private files.
- Rhyme suggestion behavior should continue improving without disturbing notes, recordings, styling, song playback, or updates.
- Version 21.0 APK is published at `https://temp.sh/SWhnz/the-top-flow-21.0.apk`.
- Version 20.6 uses JSONBlob manifest `019f0d91-7b07-768c-a38a-dacd0a9b84df`; the previous JSONBlob manifest returned `Blob not found`. JSONBlob/temp.sh are temporary hosts, so durable update hosting is still needed.

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
- Release signing and APK artifacts exist locally; avoid touching them unless the task is explicitly about releases.
