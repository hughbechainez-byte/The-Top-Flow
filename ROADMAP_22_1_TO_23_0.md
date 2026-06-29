# The Top Flow 22.1 to 23.0 Roadmap

## Direction

The 23.0 target is a visibly transformed, premium Android songwriting app: true OLED black, stronger neon/glow capability, richer typography/assets, smoother gestures, better sheets/menus, and a staged Compose-led UI foundation. Preserve note storage, `RhymeEngine`, recording/playback, and install behavior unless a version-specific task explicitly changes them.

## Architecture Decision

Use a staged Compose-led rebuild instead of a one-shot rewrite. The live app is still mostly one large Java view tree, while Compose/Material 3 are already enabled. Move visible layers into Compose in controlled increments so typing, keyboard behavior, rhyme suggestions, local notes, recording, playback, and updates keep working through each release.

## Version Plan

### 22.1
- Purpose: multi-version update foundation.
- Visual change: update chooser sheet when multiple newer builds exist.
- Architecture change: manifest parser accepts both legacy single update and new `versions[]`.
- Likely files: `MainActivity.java`, `appcast.json`, `releases/appcast.json`, docs.
- Risk: medium.
- Recommended model: 5.3 implementation, Codex release/appcast.
- 5.3 task: implement parser and chooser UI only; do not publish.
- Codex review: legacy manifest works, multiple versions sort correctly, selected APK URL installs.
- Release: bump/build/temp.sh/update JSONBlob and local appcasts/docs/commit.

### 22.2
- Purpose: Compose shell foundation.
- Visual change: true OLED root, premium top command area, bottom dock.
- Architecture change: Compose shell hosts preserved Java workflows.
- Likely files: `MainActivity.java`, new Compose shell files, `TopFlowUiFoundation.kt`.
- Risk: high.
- Recommended model: Codex architecture with 5.3 scoped components.
- 5.3 task: create shell components without changing core behavior.
- Codex review: no typing/storage/rhyme regression; release build renders.
- Release: bump/build/appcast/temp.sh/docs/commit.

### 22.3
- Purpose: premium Notes home.
- Visual change: recent-note cards, better empty state, active-note visuals.
- Architecture change: migrate or bridge Notes list into the new UI layer.
- Likely files: Notes Compose file, `MainActivity.java`, resources.
- Risk: high.
- Recommended model: 5.3 under Codex review.
- 5.3 task: rebuild Notes surface without storage changes.
- Codex review: create/open notes work; long titles truncate cleanly.
- Release: bump/build/appcast/temp.sh/docs/commit.

### 22.4
- Purpose: premium editor surface.
- Visual change: higher-end writing surface and typography hierarchy.
- Architecture change: Compose/editor bridge where practical.
- Likely files: editor Compose file, `MainActivity.java`, resources.
- Risk: high.
- Recommended model: Codex-led, 5.3 narrow implementation.
- 5.3 task: wrap current editor behavior in premium surface.
- Codex review: long-note typing, keyboard, cursor, and rhyme row remain stable.
- Release: bump/build/rhyme check/appcast/temp.sh/docs/commit.

### 22.5
- Purpose: real font and asset library.
- Visual change: richer typography and premium local visual resources.
- Architecture change: centralized font registry/assets.
- Likely files: `res/font`, assets, font menu, UI tokens.
- Risk: medium.
- Recommended model: 5.3.
- 5.3 task: add meaningful font assets and previews.
- Codex review: APK size grows for real assets only; crisp font rendering.
- Release: bump/build/appcast/temp.sh/docs/commit.

### 22.6
- Purpose: sheets and menus rebuild.
- Visual change: premium command surfaces with blur/dim backdrop.
- Architecture change: Compose sheet host where practical.
- Likely files: sheet/menu Compose files, `MainActivity.java`.
- Risk: high.
- Recommended model: 5.3 components, Codex integration.
- 5.3 task: rebuild Main Menu, Style, Font, Glow, and Update sheets.
- Codex review: forgiving dismissal, no clipped content, no action/dismiss collision.
- Release: bump/build/appcast/temp.sh/docs/commit.

### 22.7
- Purpose: gesture and motion system.
- Visual change: fluid transitions and stronger swipe affordances.
- Architecture change: central motion constants/easing; hardware-friendly animation.
- Likely files: motion helpers, Compose UI, `MainActivity.java`.
- Risk: medium-high.
- Recommended model: Codex motion plan, 5.3 implementation.
- 5.3 task: centralize transitions and gesture thresholds.
- Codex review: no typing jank; gestures are forgiving.
- Release: bump/build/appcast/temp.sh/docs/commit.

### 22.8
- Purpose: settings and personalization overhaul.
- Visual change: preview-first controls for colors, glow, fonts, rhyme settings.
- Architecture change: clearer UI models around settings state.
- Likely files: settings UI, note model touchpoints, Compose settings files.
- Risk: medium.
- Recommended model: 5.3.
- 5.3 task: rebuild personalization/settings screens.
- Codex review: persistence works; controls are not cramped.
- Release: bump/build/appcast/temp.sh/docs/commit.

### 22.9
- Purpose: splash, preload, and visual identity.
- Visual change: premium startup/preload and empty states.
- Architecture change: Compose preload state wired to existing rhyme load callbacks.
- Likely files: startup splash, rhyme load UI, assets.
- Risk: medium.
- Recommended model: 5.3.
- 5.3 task: implement preload/onboarding visuals without rhyme scoring changes.
- Codex review: startup does not block; preload clears reliably.
- Release: bump/build/rhyme check/appcast/temp.sh/docs/commit.

### 23.0
- Purpose: final flagship polish and acceptance.
- Visual change: cohesive OLED neon app, materially different from 22.0.
- Architecture change: stabilize Compose-led shell and justified Java bridges.
- Likely files: UI files, docs, appcast, release records.
- Risk: high.
- Recommended model: Codex final integration; 5.3 targeted fixes only.
- 5.3 task: fix review-identified polish/layout/validation issues.
- Codex review: premium look, no grainy effects, no clipped text, fast typing, smooth sheets/gestures, update chooser works.
- Release: bump/build/appcast/temp.sh/final report/commit.
