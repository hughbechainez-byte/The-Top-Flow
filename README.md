# The Top Flow

Native Android note app for songwriting on GrapheneOS/Android.

Current local build: `24.1`.
Current milestone: Material 3 Compose note-taking host foundation complete.
GitHub source: `https://github.com/hughbechainez-byte/The-Top-Flow`

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
.\gradlew.bat assembleRelease
```

## Update Hosting

Installed builds through 24.1 still check this temporary manifest:

`https://jsonblob.com/api/jsonBlob/019f13c7-dc3f-7cf2-bf88-038a846852bd`

24.2 should switch the primary appcast to the GitHub raw manifest:

`https://raw.githubusercontent.com/hughbechainez-byte/The-Top-Flow/main/appcast.json`

Future APKs should be attached to GitHub Releases and referenced from `appcast.json`.
