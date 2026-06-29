# The Top Flow 23.1 to 24.0 Roadmap

## Research Findings

### What makes Pixel/AOSP UI feel smooth

The practical foundation is not a single visual effect. It is a stack:

- Keep every frame inside the refresh-rate budget. On a 120 Hz display, the app has about 8.3 ms per frame, so animation paths must avoid layout, text measurement, disk I/O, or heavy allocation while moving.
- Animate GPU-friendly properties: alpha, translation, scale, and clipped/drawn transforms. Avoid per-frame layout rebuilds, background re-creation, and full text scans.
- Use predictable motion specs. Pixel/Material motion relies on consistent easing, spring/fling behavior, and coherent durations rather than random fast tweens.
- Keep composition stable. Compose and Views both need stable trees; recreation during gestures or typing creates jank.
- Ship Baseline Profiles and measure jank locally. Modern Android quality depends on startup/profile-guided compilation, Macrobenchmark, JankStats, and repeatable screenshot checks.

Primary references:

- Android rendering performance: https://developer.android.com/topic/performance/rendering
- Android vitals slow/frozen frames: https://developer.android.com/topic/performance/vitals/render
- Jetpack Compose performance: https://developer.android.com/develop/ui/compose/performance
- Macrobenchmark and Baseline Profiles: https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
- JankStats: https://developer.android.com/topic/performance/jankstats
- Android DynamicAnimation: https://developer.android.com/develop/ui/views/animations/spring-animation
- Material motion: https://m3.material.io/styles/motion/overview

### Open-source notes app patterns

Reference apps reviewed:

- Joplin: mature cross-platform notes app, React Native/mobile plus desktop/web ecosystem. Strong sync/plugins, heavier architecture.
  - Repo: https://github.com/laurent22/joplin
- Notesnook: open-source encrypted notes suite, cross-platform TypeScript/React Native/Electron ecosystem. Strong product system, heavier architecture.
  - Repo: https://github.com/streetwriters/notesnook
- Markor: native Android markdown/notes editor, published APK about 12 MB in the latest GitHub release checked locally.
  - Repo: https://github.com/gsantner/markor
- Quillpad: native Android notes app, latest GitHub release APK about 5.2 MB checked locally.
  - Repo: https://github.com/quillpad/quillpad

Decision:

Do not copy a cross-platform notes stack. The Top Flow's vision is a performance-sensitive songwriting/rhyme tool, so a native Android foundation is still the right base. The 24.0 push should move visible UI toward a Compose-led, measured motion engine while keeping Java behavior only as the storage/rhyme/media bridge until it is safe to migrate more.

## Architecture Direction

24.0 should replace the 22.x "decorated backdrop" direction with a pure OLED black stage. The only thing allowed to visually disturb black is active neon glow, note color, or motion feedback. The blue radar/tech background must go.

Core design engine requirements:

- Pure black root and panel foundations.
- Central motion spec for durations, easing, and spring/fling constants.
- Reusable motion helpers that prefer translation, alpha, and scale.
- Stable shell and sheet hosts that do not recreate large child trees during gestures.
- Baseline Profile and local performance test scaffolding prepared for emulator/device validation.
- Meaningful APK growth from real systems: UI runtime/animation support, local visual assets that are actually used, and faster offline rhyme data structures.

APK size target:

- 23.0 APK: about 9.4 MB.
- 24.0 minimum target: at least about 18.9 MB.
- Size growth must be justified by actual runtime systems or assets:
  - Motion/animation runtime if it powers visible UI.
  - Local neon/glow asset atlas if used by shell/sheets/editor.
  - Precomputed offline rhyme lookup data if it reduces startup or query latency.
  - Test/profile modules do not count if they are not shipped in release APK.

## Version Plan

### 23.1

Purpose:
Publish the working Compose lifecycle host fix as the first stable 23.x continuation and remove the blue radar/tech backdrop.

Implementation:
- Keep the existing Compose lifecycle fix candidate.
- Replace the current Compose backdrop art with pure black plus only minimal active neon edge/glow accents, or remove the decorative backdrop entirely.
- Keep Compose host stable; no legacy fallback unless startup breaks again.

Review:
- Verify no `ViewTreeLifecycleOwner` crash path remains.
- Verify root/shell/background are black.
- Build, package, temp-host, publish appcast as 23.1.

### 23.2

Purpose:
Create the shared Pixel-grade motion foundation.

Implementation:
- Centralize durations/easing/spring constants.
- Add no-loop, hardware-friendly motion helpers for shell, sheets, dock, buttons, and panel swaps.
- Slow transitions slightly where useful for visual quality without increasing input latency.

Review:
- No per-keystroke heavy work.
- Gestures still respond immediately.
- Motion is coherent across Notes, Editor, Sheets, and Startup.

### 23.3

Purpose:
Rebuild bottom sheets and menus around a measured black OLED modal stage.

Implementation:
- Pure black scrim/dim/blur policy.
- Stronger sheet enter/exit choreography.
- Better menu spacing and action grouping.
- No nested cards and no default-looking rows.

Review:
- Sheet dismissal remains forgiving.
- Text does not clip.
- Backdrop remains black except neon/glow.

### 23.4

Purpose:
Notes home makeover on the pure-black system.

Implementation:
- More distinctive black/neon session list.
- Better active note treatment.
- Smoother Notes-to-Editor continuity.

Review:
- Create/open note behavior unchanged.
- Large note lists avoid heavy rebuild animations.
- Long titles/previews stay stable and ellipsized.

### 23.5

Purpose:
Editor motion and text fidelity pass.

Implementation:
- Refine editor focus transitions, cursor/rhyme popup anchoring, and title/body chrome.
- Preserve typing hot path.
- Improve high-refresh smoothness by keeping live editor updates minimal.

Review:
- Long-note typing must remain safe.
- No full-body scans on every keystroke.
- Editor text stays crisp with letter spacing 0.

### 23.6

Purpose:
Dock and gesture fluidity polish.

Implementation:
- Polish bottom dock active states as one stable high-refresh control system.
- Refresh swipe rail visuals to match the pure-black neon language.
- Keep panel/dock state transitions on the existing shared motion helpers.

Review:
- Gesture thresholds and start rules remain unchanged.
- Dock controls keep fixed dimensions and do not jump.

### 23.7

Purpose:
Offline rhyme preload speed, shipped data improvement, and functional APK growth.

Implementation:
- Add a meaningful precomputed lookup/cache asset that reduces common default fast-suggestion work.
- Keep scoring behavior stable and fall back to the existing scorer whenever settings differ.
- Configure release packaging so the shipped data meaningfully increases APK size without useless filler.

Review:
- Rhyme quality checks pass.
- Startup does not regress.
- APK growth is from usable data.

### 23.8

Purpose:
Functional APK-size gate with runtime-used rhyme acceleration data.

Implementation:
- Add uncompressed prepared rhyme/index data that `RhymeEngine` actually reads first.
- Add expanded/default rhyme cache data where it can safely fall back to the scorer.
- Keep TSV/scorer fallbacks intact.

Review:
- APK clears the 2x size target with useful shipped data.
- Rhyme quality checks pass.
- Larger assets remain runtime-used and explainable.

### 23.9

Purpose:
Final 24.0 candidate polish and local QA planning.

Implementation:
- Check whether the app is visually and technically upgraded enough.
- Document how to run local crash, screenshot, and jank testing without Dave's Pixel.
- Tighten docs and release records.

Review:
- APK remains at least 2x the 23.0 size by 24.0.
- Do not run emulator/device visual tests yet per Dave's instruction.
- No useless bloat.

### 24.0

Purpose:
Publish the completed 24.0 UI foundation milestone.

Implementation:
- Final smoke validation.
- Version bump, APK build/copy/upload.
- Appcast/JSONBlob update with versions 24.0 down to 22.1.
- Final report including research findings, local test strategy, APK-size explanation, and how close the result is to Dave's vision.

Review:
- Latest app pulls from the app.
- Desktop folder includes every 23.1 to 24.0 APK and final report.
- 5.3 is closed after completion.
