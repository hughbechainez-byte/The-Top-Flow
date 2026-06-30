# Handoff

## Project Summary

The Top Flow is a native Android songwriting notes app. The 24.1 active launcher is now a Kotlin `ComponentActivity` Compose note-taking host, while the previous Java UI is preserved under `legacy/` for reference during the remaining feature ports. The 21.0 rhyme logic remains extracted into a dedicated offline `RhymeEngine` with generated pronunciation/rhyme assets.

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

22.5 adds real bundled font assets from Google Fonts under OFL: Space Grotesk, Share Tech Mono, and Silkscreen. Existing font IDs now map `slim` to Space Grotesk, `terminal` to Share Tech Mono, and `pixel` to Silkscreen through fallback-safe resource loading. Editor rendering and font previews use the bundled fonts.

22.6 rebuilds sheets and menus into premium OLED command panels. It adds stronger fixed handle/header chrome, command-row menu surfaces, refined Style/Font/Glow/Font Size sheets, and cleaner update chooser cards. Swipe-down dismissal, tap-outside dismissal, scroll cap behavior, blur/dim backdrop, deferred menu actions, and command routing remain preserved.

22.7 tunes gesture and motion behavior. It increases edge swipe hitboxes, centralizes swipe completion/abort checks, quickens sheet/dock/tap feedback, and routes more motion through the shared workflow animator. Editor swipe remains disabled while title/body inputs are focused, and sheet dismissal safety remains preserved.

22.8 upgrades settings and personalization. Style, color, font, font-size, glow, and rhyme settings now use preview-first OLED/neon cards, live preview surfaces, larger touch targets, and clearer current-value treatments. Persistence keys, note style storage, rhyme settings, and update behavior remain unchanged.

22.9 upgrades startup and preload identity. The launch overlay now uses a true-OLED branded surface, crisp signal rails, refined preload status treatment, and the existing progress fill path. Rhyme preload timing, cache warming, status callbacks, startup dismissal, and forced-close behavior remain unchanged.

23.0 completes the 22.1 to 23.0 UI milestone with cohesive global chrome. The top shell now carries live Notes/Editor context status, a refreshed signal rail, version visibility, and the existing Menu action. The bottom dock now uses the shorter `Tune` label for settings while preserving icon mapping and command routing. Codex review removed a per-keystroke full-body status scan before release.

23.1 addresses the 23.0 startup crash recorded in `V23_CRASH_LOG.md` and begins the 23.1 to 24.0 pure-black motion foundation. `MainActivity` now extends `ComponentActivity`, manual root/popup hosts receive view-tree lifecycle/saved-state/ViewModel owners, and the Compose backdrop attaches owners before `setContent` with `DisposeOnViewTreeLifecycleDestroyed`. The old blue radar/grid/waveform backdrop has been removed; the Compose backdrop now renders pure black behind active UI surfaces. The original JSONBlob manifest disappeared with 404, so 23.1+ uses replacement JSONBlob `019f13c7-dc3f-7cf2-bf88-038a846852bd` until durable hosting is available.

23.2 adds the first motion-foundation pass for the 23.1 to 24.0 milestone. Tap and selection response stays fast, while panel swaps, sheets, dock state feedback, edge-swipe completion/restoration, and startup close/fill motion now share grouped constants and a smoother eased workflow animator. Gesture thresholds, storage, rhyme behavior, media, and update routing remain unchanged.

23.3 rebuilds the modal/menu visual language around pure OLED black. The Main Menu now has a current-session context panel, cleaner Recent sessions / Commands hierarchy, thin accent separators, black command rows with neon rails, and a darker black modal scrim. Update chooser, settings rows, font/style rows, and rhyme settings controls now share the same command-panel language.

23.4 carries that language into the Notes home/dashboard. The header now mirrors current-session context from the Main Menu, note cards are flatter pure-black OLED surfaces with thin accent rails and fixed signal slots, and long titles/previews use stable ellipsizing to reduce layout jump.

23.5 carries the same language into the editor. Editor chrome now shows the active note title and compact draft metadata with a fixed accent rail/signal block, editor/media surfaces are pure black with accent strokes, rhyme popup/chips are sharper OLED command surfaces, expanded-rhyme sheets get a focused-word context panel, and opening note cards uses selection motion for continuity.

23.6 polishes the bottom dock and gesture affordances. The dock is now a fixed-height pure-black/neon control surface with accent-aware active strokes, icon tinting, type weight, and shared 23.2 dock motion. Edge-swipe rails now use the same neon rail language with alpha/scale feedback only; thresholds and gesture start rules are unchanged.

23.7 adds a real shipped offline rhyme hot cache. `RhymeEngine` loads `rhyme_hot_cache.tfcache` on the existing background load thread and serves it only for safe default fast suggestions: Balanced strictness, exactOnly false, includeSlang true, no removed rhymes, fast maxCandidates 360, and limits 4 through 6. Expanded rhymes, removed-suggestion cases, context-heavy paths, and non-default settings still use the existing scorer. The `.tfcache` asset is no-compressed in release packaging so APK growth comes from runtime-used data.

23.8 clears Dave's 24.0 size gate with more runtime-used rhyme data. `RhymeEngine` now tries uncompressed `rhyme_index_accel.tfindex` before falling back to `rhyme_index.tsv`, and it loads `rhyme_expanded_hot_cache.tfcache` for safe default expanded requests. Existing scorer/TSV fallbacks remain intact, and known regression words stay on the scorer for expanded requests. The 23.8 release APK is 19,451,623 bytes before final packaging copy/upload.

23.9 performs the final acceptance polish before 24.0. Remaining generic chrome/card surfaces in the global header and reusable card helpers now use the pure-black command-surface language instead of legacy floating panels, and `LOCAL_QA_24_PLAN.md` documents laptop-based crash, screenshot, jank, Macrobenchmark, Baseline Profile, and JankStats paths. Emulator/device visual tests were deliberately not run per Dave's instruction.

24.0 completes the 23.1 to 24.0 pure-black OLED UI foundation milestone. It packages the pure-black backdrop, shared motion foundation, menu/modal surfaces, Notes dashboard, editor/rhyme surfaces, dock/gesture polish, runtime-used rhyme acceleration assets, local QA plan, final Desktop report, and appcast publication as the completed release.

24.1 is a Material 3 Compose note-taking foundation build. `MainActivity.kt` owns the live `setContent` host, `NotesTheme` provides one Material 3 color/typography/shape system with dynamic dark support, `NotesScreens.kt` renders cards/search/editor/rhyme chips with Compose text and `BasicTextField`, and `NotesViewModel.kt` exposes immutable UI state through `StateFlow` while moving storage/search/rhyme work off the main thread. Macrobenchmark and screenshot test modules are present, and `app/src/release/generated/baselineProfiles/baseline-prof.txt` was generated from the release interactions. This build was packaged locally only; do not push JSONBlob/appcast until Dave directs it.

## Build / Run

Known build command from `README.md`:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$env:ANDROID_HOME="$PWD\android-sdk"
$env:ANDROID_SDK_ROOT="$PWD\android-sdk"
tools\gradle-8.10.2\bin\gradle.bat assembleRelease
```

Latest published build: 24.0 / versionCode 66 passed `tools\rhyme_quality_check.py` and `assembleRelease`.

Latest local-only build: 24.1 / versionCode 67 passed `assembleRelease`, `assembleDebug`, screenshot instrumentation tests on the local emulator, generated a release Baseline Profile, and passed the active-source banned-rendering scan.

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

Next: replace temporary hosting with durable hosting and run approved local emulator/screenshot/jank QA. Preserve note storage, media, gestures, update behavior, and existing release line compatibility.

## Assumptions

- The app is intended to stay lightweight and native.
- Local note data is stored as `notes.json` in app private files.
- Rhyme suggestion behavior should continue improving without disturbing notes, recordings, styling, song playback, or updates.
- Version 22.1 is the first build published to the 22.0 JSONBlob/appcast line and should be represented in both top-level latest fields and the `versions[]` list.
- Version 22.2 adds the first live Compose-backed UI layer and should remain in the multi-version appcast list after later 22.x releases.
- Version 22.3 upgrades the Notes home and should remain in the multi-version appcast list after later 22.x releases.
- Version 22.4 upgrades the editor surface and should remain in the multi-version appcast list after later 22.x releases.
- Version 22.5 adds bundled OFL font assets and should remain in the multi-version appcast list after later 22.x releases.
- Version 22.6 upgrades sheets/menus and should remain in the multi-version appcast list after later 22.x releases.
- Version 22.7 upgrades gesture/motion behavior and should remain in the multi-version appcast list after later 22.x releases.
- Version 22.8 upgrades settings/personalization and should remain in the multi-version appcast list after later 22.x releases.
- Version 22.9 upgrades startup/preload visual identity and should remain in the multi-version appcast list after later releases.
- Version 23.0 completes the premium UI milestone and should remain the top-level appcast latest until superseded.
- Version 23.1 supersedes 23.0 with the Compose lifecycle fix, a pure-black backdrop, and a replacement temporary JSONBlob manifest. Keep Compose; do not fall back to the legacy native backdrop.
- Version 23.2 adds the shared motion foundation. Keep the timing constants grouped and avoid changing gesture thresholds unless a future build has visual/test evidence.
- Version 23.3 unifies menu/modal sheets around pure-black OLED command surfaces. Keep modal backgrounds black; neon belongs on active rails, strokes, and text emphasis.
- Version 23.4 unifies the Notes dashboard with the menu/modal language. Keep note-card support elements fixed-width/fixed-height to avoid jumpy lists.
- Version 23.5 unifies editor/rhyme surfaces with the same language. Do not add body-text watcher work or per-keystroke scans when polishing editor UI.
- Version 23.6 unifies dock and swipe affordances with the pure-black neon rail language. Do not change gesture thresholds without explicit evidence.
- Version 23.7 adds the default fast-rhyme hot cache. Keep hot-cache eligibility conservative; fall back to the scorer whenever settings, removals, or request shape differ.
- Version 23.8 adds the uncompressed prepared index and expanded hot cache. Keep all new large assets runtime-used and preserve TSV/scorer fallbacks.
- Version 23.9 adds the local QA plan and final pure-black chrome cleanup. Do not run emulator/device tests unless Dave explicitly allows it.
- Version 24.0 completes the milestone. JSONBlob/temp.sh/tmpfiles are still temporary; durable update hosting remains the next operational risk.
- Version 22.0 APK is temp-hosted at `https://temp.sh/xXCOu/the-top-flow-22.0.apk`; it was not published through JSONBlob/appcast during the 21.6 to 22.0 UI-alpha run.
- Version 21.5 APK is temp-hosted for validation only at `https://temp.sh/ZIRmO/the-top-flow-21.5.apk`; do not publish it through JSONBlob/appcast until directed.
- Version 21.4 APK is published at `https://temp.sh/Jawft/the-top-flow-21.4.apk`.
- Version 21.4 uses JSONBlob manifest `019f0d91-7b07-768c-a38a-dacd0a9b84df`; JSONBlob/temp.sh/tmpfiles are temporary hosts, so durable update hosting is still needed.
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
- 22.6 should rebuild the sheet/menu presentation without changing command behavior.
- 22.7 should centralize and improve gesture/motion behavior without disturbing input hot paths.
- 22.8 should make settings/personalization feel preview-first while preserving existing option persistence.
- 22.9 should make startup, preload, and visual identity feel premium without blocking app entry or changing rhyme scoring.
- 23.0 should perform a cohesive acceptance pass over shell, notes, editor, sheets, settings, startup, typography, gestures, and release records.
- 23.2 should centralize motion specs and make transitions slower, smoother, and more coherent without adding input lag.
- Release signing and APK artifacts exist locally; avoid touching them unless the task is explicitly about releases.
