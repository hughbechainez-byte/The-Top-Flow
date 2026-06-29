# Changelog

## 2026-06-29

- Released 23.2 with a shared motion foundation: centralized timing/easing constants, smoother panel/sheet/dock/swipe/startup transitions, and quick tap feedback preserved for responsiveness.
- Released 23.1 with the Compose lifecycle host fix retained and the blue radar/grid/waveform backdrop removed, returning the app to a pure OLED black foundation behind active neon UI surfaces.
- Released 23.0 with cohesive global chrome, live Notes/Editor context status, a refreshed header signal rail, `Tune` dock polish, and final review fixes to avoid per-keystroke body scans.
- Released 22.9 with a premium OLED launch/preload surface, crisp startup signal rails, refined status/progress treatment, and review fixes that removed fake percentage labels while preserving preload behavior.
- Released 22.8 with preview-first settings and personalization sheets, richer color/font/glow controls, live preview surfaces, refined rhyme-setting cards, and review fixes for preview contrast and card spacing.
- Released 22.7 with tuned gesture thresholds, larger edge hitboxes, shared swipe completion helpers, faster sheet/dock/tap motion, and consistent workflow animation paths while preserving input safety.
- Released 22.6 with premium OLED sheet chrome, stronger fixed handles/headers, command-row menu surfaces, refined style/font/glow sheets, and cleaner update chooser cards while preserving sheet behavior.
- Released 22.5 with bundled OFL Google Fonts assets for Space Grotesk, Share Tech Mono, and Silkscreen, wired into editor rendering and font previews with fallback-safe loading.
- Released 22.4 with a premium Draft Studio editor chrome, accent rail, compact session metadata, note-accent signal detail, and refined title/body field surfaces while preserving input hot paths.
- Released 22.3 with a premium Notes home dashboard, stronger active-session treatment, richer note cards, accent rails, compact metadata/previews, and crisp view-built signal markers.
- Released 22.2 with a Compose-backed premium OLED studio backdrop mounted behind the existing Java workflows, adding crisp neon rails, waveform marks, and grid details without changing app behavior.
- Released 22.1 with multi-version update manifest parsing, legacy appcast fallback compatibility, and a premium OLED update chooser sheet for multiple available builds.
- Released 22.0 with a cleaner OLED Main Menu command surface, compact recent-session previews, deferred menu actions after dismiss motion, and shared workflow animation polish. APK was temp-hosted only; appcast/update manifests were not changed.
- Released 21.9 with swipe rail affordance polish, shared workflow motion across dock/panel interactions, tighter saved-session rows, cleaner empty-state copy, and gesture cleanup fixes. APK was temp-hosted only; appcast/update manifests were not changed.
- Released 21.8 with velocity-aware editor/Notes swipes, shared settle/complete gesture helpers, and current-session context in the Notes header. APK was temp-hosted only; appcast/update manifests were not changed.
- Released 21.7 with a cleaner Notes command header, metadata rows, and right-edge gesture return to the editor. APK was temp-hosted only; appcast/update manifests were not changed.
- Released 21.6 with blur-backed bottom sheets, editor left-edge swipe back to Notes, and solid OLED panel drawables to avoid grainy surface effects. APK was temp-hosted only; appcast/update manifests were not changed.
- Released 21.5 with scroll-safe bottom-sheet content, persistent sheet handle/header visibility, active bottom-dock feedback, and selected-note list feedback. The APK is temp-hosted only; appcast/update manifests were not changed.
- Released 21.4 with OLED UI polish, true black main panels, visible note-card halo glow, forgiving bottom-sheet swipe dismissal, dimmed sheet backdrop, expanded offline editor fonts, lightweight selection/menu animations, and startup rhyme cache prewarming with preload/first-query logs.

## 2026-06-28

- Released 21.3 with real-device lag fixes for the rhyme button/editor path: async expanded rhymes, deferred long-note body copying, editor text-service opt-outs, popup measurement reuse, chip view reuse, and detailed `rhyme_trace` runtime logs.
- Released 21.2 with rhyme hot-path caching, fast-row cache stabilization, bounded expanded rhyme scoring, reduced caret-popup churn, true OLED black shell, removed editor notebook lines, note font-size/color/glow controls, and swipe-down bottom sheets.
- Released 21.1 with the completed phased 21.x rebuild: Compose/Material 3 foundation, 21.x OLED/indigo/mint surfaces, bottom command dock, live font previews, rebuilt Java UI bridge surfaces, and preserved offline rhyme engine behavior.
- Completed Rebuild C with a bottom command dock, compact floating header, OLED/indigo/mint live surfaces, quieter note/editor/sheet composition, and preserved app behavior.
- Completed Rebuild B by wiring current Java notes/media/rhyme/font surfaces into the 21.x UI kit, including live font previews, 21.x card/control styling, and rebuilt rhyme row visuals without changing core behavior.
- Completed Rebuild A foundation after 21.0 with Kotlin, Compose, Material 3, dynamic dark theme support, Java-native UI kit helpers, 21.x resource tokens, and local Pixel reference capture through WSL Debian ADB.
- Released 21.0 with a dedicated offline `RhymeEngine`, generated pronunciation/rhyme index asset, indexed rhyme lookup/cache readiness, a distinct studio toolbar/dock/editor shell, and rebuilt rhyme panel styling.
- Released 20.6 with CMU-loading rhyme row gating, stricter stale/fallback protection, curated `out` rhyme validation and ordering, refined editor surface styling, disabled/loading chip state, and an empty-note state.
- Released 20.5 as the published build for the 20.5 milestone after phases 20.5A, 20.5B, and 20.5C.
- Completed 20.5C premium polish with resource-backed pressed states and lightweight button icons while preserving behavior.
- Completed 20.5B UI integration by wiring design-system resources into existing Java surfaces, typography, buttons, chips, sheets, and spacing without changing app behavior.
- Created 20.5A design system foundation with reusable colors, dimensions, typography styles, panel/button/chip drawables, and theme tokens without changing app behavior.
- Released 20.4 with a premium dark studio UI pass: layered native panels, refined gradients/strokes, polished controls, toned-down backdrop, improved editor/list spacing, and pill-shaped rhyme chips without changing rhyme scoring.
- Released 20.3 with async fast-row rhyme generation, debounce/cancel handling, cached query results, bounded fast-row scoring, reduced popup rebuilds, and lightweight timing logs to address editor freezes.

## 2026-06-27

- Released 20.2 with stricter hip-hop rhyme buckets, stronger curated exact-rhyme ranking, AY/EY fallback separation for `my/try`, and `yours` pronunciation tuning.
- Released 20.1 with a focused rhyme regression check, stronger same-stem slang variants, unknown `-ing` pronunciation handling, and curated rhyme candidate tuning.
- Released 20.0 with pronunciation-first rhyme ranking using CMU stressed-vowel, coda, syllable, and exact/near/phrase/fallback buckets.
- Updated appcast metadata and release APK for in-app update delivery.
- Created Brobro Development Framework project record files: `PROJECT_STATUS.md`, `CODING_RULES.md`, and `HANDOFF.md`.
- Added initial changelog entry for project-record initialization.
