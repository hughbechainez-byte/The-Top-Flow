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

This was not run during the documentation-only initialization.

Latest verified build: 20.0 / versionCode 29 via `assembleRelease`.

## Important Files

- `README.md`: basic project name, current local build, release build command, and update hosting notes.
- `settings.gradle`: Gradle project name is `The Top Flow`; includes `:app`.
- `build.gradle`: Android application plugin version.
- `app/build.gradle`: Android app module configuration, package namespace, SDK versions, signing config, version properties, and update manifest URL.
- `app/src/main/AndroidManifest.xml`: permissions, app label/icon/theme, file provider, and launcher activity.
- `app/src/main/java/com/davehq/thetopflow/MainActivity.java`: main app UI and behavior, including notes, pronunciation-first rhyme suggestions, recordings, song playback, update checks, persistence, and styling.
- `app/src/main/assets/cmudict.dict`: CMU pronunciation dictionary used by rhyme suggestions.
- `appcast.json` and `releases/appcast.json`: update manifest files.

## Current Next Task

Add a small rhyme regression/QA set and tune slant-rhyme ranking using real examples.

## Assumptions

- The app is intended to stay lightweight and native.
- Local note data is stored as `notes.json` in app private files.
- Rhyme suggestion behavior should continue improving without disturbing notes, recordings, styling, song playback, or updates.
- Version 20.0 APK is published at `https://temp.sh/YIMRS/the-top-flow-20.0.apk`.

## Warnings / Unknowns

- The Git worktree already had modified and untracked files before this framework setup.
- `MainActivity.java` is large, so future changes should use targeted searches and focused edits.
- No test suite was found in the inspected files; 20.0 was verified with a release build and a manual CMU signature sanity check.
- Release signing and APK artifacts exist locally; avoid touching them unless the task is explicitly about releases.
