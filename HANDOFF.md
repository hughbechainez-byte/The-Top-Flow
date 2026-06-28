# Handoff

## Project Summary

The Top Flow is a native Android songwriting notes app. It currently appears to be a single-activity Java app with programmatic premium dark UI, local JSON note storage, pronunciation-first rhyme suggestions, song playback, voice recording, styling controls, update checks, resource-backed design-system integration, and lightweight premium polish assets.

## Build / Run

Known build command from `README.md`:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$env:ANDROID_HOME="$PWD\android-sdk"
$env:ANDROID_SDK_ROOT="$PWD\android-sdk"
tools\gradle-8.10.2\bin\gradle.bat assembleRelease
```

Latest verified build: 20.5 / versionCode 34 via `assembleRelease`.

## Important Files

- `README.md`: basic project name, current local build, release build command, and update hosting notes.
- `settings.gradle`: Gradle project name is `The Top Flow`; includes `:app`.
- `build.gradle`: Android application plugin version.
- `app/build.gradle`: Android app module configuration, package namespace, SDK versions, signing config, version properties, and update manifest URL.
- `app/src/main/AndroidManifest.xml`: permissions, app label/icon/theme, file provider, and launcher activity.
- `app/src/main/java/com/davehq/thetopflow/MainActivity.java`: main app UI and behavior, including notes, design-system-backed surfaces/buttons/chips, async pronunciation-first rhyme suggestions, recordings, song playback, update checks, persistence, and styling.
- `app/src/main/res/values/colors.xml`, `dimens.xml`, `styles.xml`, and `drawable/*`: reusable design system foundation, pressed states, panel/button/chip backgrounds, and lightweight button icons.
- `app/src/main/assets/cmudict.dict`: CMU pronunciation dictionary used by rhyme suggestions.
- `tools/rhyme_quality_check.py`: focused rhyme regression check for the 20.x rhyme engine, including real writing examples for `my/try`, `yours`, slang, phrase, and weak-match exclusions.
- `appcast.json` and `releases/appcast.json`: update manifest files.

## Current Next Task

Proceed to the next milestone planning after release is published.

## Assumptions

- The app is intended to stay lightweight and native.
- Local note data is stored as `notes.json` in app private files.
- Rhyme suggestion behavior should continue improving without disturbing notes, recordings, styling, song playback, or updates.
- Version 20.5 APK is published at `https://temp.sh/yBcCC/the-top-flow-20.5.apk`.

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
- Release signing and APK artifacts exist locally; avoid touching them unless the task is explicitly about releases.
