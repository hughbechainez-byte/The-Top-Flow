# The Top Flow

Native Android note app for songwriting on GrapheneOS/Android.

Current local build: `21.0`.
Current milestone: `21.0` bottom-up rebuild.

## Project Record

Future Codex sessions should start with:

- `PROJECT_STATUS.md`
- `CODING_RULES.md`
- `HANDOFF.md`
- `CHANGELOG.md`

## Build

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$env:ANDROID_HOME="$PWD\android-sdk"
$env:ANDROID_SDK_ROOT="$PWD\android-sdk"
tools\gradle-8.10.2\bin\gradle.bat assembleRelease
```

## Update Hosting

The app checks this temporary manifest:

`https://jsonblob.com/api/jsonBlob/019f0d91-7b07-768c-a38a-dacd0a9b84df`

Publish a newer signed APK and update that JSON with a higher `versionCode`. The original intended GitLab raw manifest remains suitable once GitLab credentials are available:

`https://gitlab.com/davehq/the-top-flow/-/raw/the-top-flow/appcast.json`
