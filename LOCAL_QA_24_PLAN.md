# Local QA Plan for 24.0

This plan documents how to test crashes, visual fidelity, and motion quality on this laptop later without using Dave's Pixel. These emulator/device visual tests were not run during 23.9 per Dave's instruction.

## Current Local SDK State

- Present: `android-sdk/build-tools`, `android-sdk/cmdline-tools/latest`, `android-sdk/platform-tools/adb.exe`, `android-sdk/platforms`, and SDK licenses.
- Missing: `android-sdk/emulator`.
- Missing: `android-sdk/system-images`.
- Result: release builds and APK packaging work locally now, but emulator or Gradle Managed Device testing requires installing emulator and system-image packages first.

## Crash Smoke Path

1. Install SDK packages later with `sdkmanager`: `emulator`, a recent `platforms;android-35` if needed, and a matching system image such as `system-images;android-35;google_apis;x86_64`.
2. Create an AVD with `avdmanager` or configure a Gradle Managed Device.
3. Install the release APK with `adb install -r releases/the-top-flow-24.0.apk`.
4. Launch the app with `adb shell monkey -p com.davehq.thetopflow 1` or `adb shell am start`.
5. Capture `adb logcat` around launch, menu opening, note opening, editor typing, rhyme popup, expanded rhymes, settings, and update sheet.

## Visual Fidelity Path

- Emulator screenshot smoke: drive the app with `adb`, capture `adb exec-out screencap -p`, and compare screenshots for black background, no radar/grid imagery, crisp text, and no clipped controls.
- Roborazzi option: add JVM/Robolectric screenshot tests for key screens and compare golden images without a running emulator.
- Paparazzi option: add JVM-rendered snapshots for isolated Compose/View surfaces where exact Android framework behavior is not required.
- Manual screenshot targets: startup/preload, Notes dashboard, Editor, Main Menu, Style sheet, Rhyme Settings, expanded rhymes, update chooser, and bottom dock states.

## Jank and Motion Path

- Macrobenchmark module: add startup and interaction benchmarks for launch, Notes-to-Editor, sheet open/close, dock actions, and rhyme popup.
- Baseline Profile: generate and ship a baseline profile after benchmark flows are stable, so startup and hot UI paths compile earlier.
- JankStats: add debug/instrumented collection around shell/sheet/dock transitions to flag missed frame deadlines.
- Frame budget target: 120 Hz devices have about 8.3 ms per frame, so measured animation paths should avoid layout churn, disk I/O, and large text scans while moving.

## References

- Android rendering performance: https://developer.android.com/topic/performance/rendering
- Android vitals slow/frozen frames: https://developer.android.com/topic/performance/vitals/render
- Compose performance: https://developer.android.com/develop/ui/compose/performance
- Macrobenchmark and Baseline Profiles: https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
- JankStats: https://developer.android.com/topic/performance/jankstats
- Gradle Managed Devices: https://developer.android.com/studio/test/gradle-managed-devices
