# The Top Flow

Native Android note app for songwriting on GrapheneOS/Android.

Current local build: `3.0`.

## Build

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$env:ANDROID_HOME="$PWD\android-sdk"
$env:ANDROID_SDK_ROOT="$PWD\android-sdk"
tools\gradle-8.10.2\bin\gradle.bat assembleRelease
```

## Update Hosting

The app checks this temporary manifest:

`https://jsonblob.com/api/jsonBlob/019ee1cd-9c9c-7c8b-977c-f890d6953d10`

Publish a newer signed APK and update that JSON with a higher `versionCode`. The original intended GitLab raw manifest remains suitable once GitLab credentials are available:

`https://gitlab.com/davehq/the-top-flow/-/raw/the-top-flow/appcast.json`
