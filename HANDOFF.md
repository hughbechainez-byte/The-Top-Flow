# Handoff

## Project Summary

The Top Flow is a native Android songwriting notes app. It currently appears to be a single-activity Java app with programmatic UI, local JSON note storage, pronunciation-first rhyme suggestions, song playback, voice recording, styling controls, and update checks.

## Build / Run

Known build command from `README.md`:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$env:ANDROID_HOME="$PWD\android-sdk"
$env:ANDROID_SDK_ROOT="$PWD\android-sdk"
tools\gradle-8.10.2\bin\gradle.bat assembleRelease
```

Latest verified build: 20.3 / versionCode 32 via `assembleRelease`.

## Important Files

- `README.md`: basic project name, current local build, release build command, and update hosting notes.
- `settings.gradle`: Gradle project name is `The Top Flow`; includes `:app`.
- `build.gradle`: Android application plugin version.
- `app/build.gradle`: Android app module configuration, package namespace, SDK versions, signing config, version properties, and update manifest URL.
- `app/src/main/AndroidManifest.xml`: permissions, app label/icon/theme, file provider, and launcher activity.
- `app/src/main/java/com/davehq/thetopflow/MainActivity.java`: main app UI and behavior, including notes, async pronunciation-first rhyme suggestions, recordings, song playback, update checks, persistence, and styling.
- `app/src/main/assets/cmudict.dict`: CMU pronunciation dictionary used by rhyme suggestions.
- `tools/rhyme_quality_check.py`: focused rhyme regression check for the 20.x rhyme engine, including real writing examples for `my/try`, `yours`, slang, phrase, and weak-match exclusions.
- `appcast.json` and `releases/appcast.json`: update manifest files.

## Current Next Task

Device-test 20.3 with long notes, rapid typing, caret movement, keyboard show/hide, and rhyme row on/off. Then add more multi-syllable phrase-tail examples and decide how expanded-mode slants should be exposed.

## Assumptions

- The app is intended to stay lightweight and native.
- Local note data is stored as `notes.json` in app private files.
- Rhyme suggestion behavior should continue improving without disturbing notes, recordings, styling, song playback, or updates.
- Version 20.3 APK is published at `https://temp.sh/aHHxm/the-top-flow-20.3.apk`.

## Warnings / Unknowns

- The Git worktree already had modified and untracked files before this framework setup.
- `MainActivity.java` is large, so future changes should use targeted searches and focused edits.
- Rhyme checks now run through `python tools\rhyme_quality_check.py`; 20.3 also passed a release build.
- 20.3 moves fast rhyme-row generation off the UI thread with debounce, stale-job cancellation, an LRU query cache, bounded fast-row candidate scoring, and timing logs.
- Release signing and APK artifacts exist locally; avoid touching them unless the task is explicitly about releases.
