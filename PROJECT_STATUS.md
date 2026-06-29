# Project Status

Last updated: 2026-06-29

## App Name

The Top Flow, also referred to as Top Flow / Rhyming Notes.

## Purpose

Native Android songwriting notes app for GrapheneOS/Android. The app helps capture lyrics, attach song/voice references, and surface rhyme suggestions while writing.

## Current Known Features

- Local note list and lyric editor.
- Per-note title, body, font, note color, text color, and accent color.
- Per-note editor font size, note glow toggle, and glow strength.
- Resource-backed design system foundation: colors, dimensions, typography styles, panel/button/chip/editor drawables, and theme tokens.
- Existing Java UI helpers consume the design system for major surfaces, buttons, chips, sheets, spacing, typography, and editor presentation.
- 21.0 studio shell with a distinct top toolbar, global dock, premium editor/rhyme panel surfaces, polished note list, and empty-note state.
- Rebuild A foundation after 21.0: Kotlin, Compose, Material 3, dynamic dark theme support, Java-native UI kit helpers, and 21.x resource tokens are present for the next UI migration stage.
- Rebuild B wires current Java notes/media/rhyme/font surfaces into the 21.x UI kit, including live font preview rows and rebuilt rhyme row/card controls.
- Rebuild C moves the command dock to the bottom, tightens the top bar, shifts live screens to OLED/indigo/mint surfaces, and softens sheets/backdrop styling.
- Rhyme suggestion chips near the cursor, backed by a dedicated `RhymeEngine`, generated offline `rhyme_index.tsv`, pronunciation-first CMU ranking, quality buckets, curated writing examples, async fast-row generation, loading-state gating, and fallback phonetic heuristics for genuine unknowns.
- 21.2 rhyme hot path caches pronunciation/family info, avoids full-note internal-rhyme scanning for fast-row suggestions, bounds expanded candidate scoring, and avoids dismissing/recreating the caret popup on cache misses.
- 21.3 moves expanded rhyme button lookup off the UI thread, defers full-note body copying until draft save, reuses caret popup measurements/chips, disables expensive editor text services where safe, and adds `rhyme_trace` runtime logs.
- 21.4 removes the dark-blue middle panel layer, keeps the shell true OLED black, adds a real note halo behind the editor card, makes bottom-sheet drag dismissal more forgiving, dims/scales the background behind sheets, expands offline font choices, and prewarms common rhyme caches during startup.
- 21.5 keeps bottom-sheet handles/headers visible while long sheet content scrolls, adds active feedback to the bottom dock, and makes the currently open note visibly selected in the Notes list.
- 21.6 through 22.0 continue the UI-alpha run with blur-backed sheets, solid OLED surfaces, bidirectional editor/Notes edge gestures, velocity-aware swipe completion, swipe rail affordances, compact recent-note context, and a cleaner Main Menu command surface.
- 22.1 adds multi-version update discovery with legacy appcast fallback support and an OLED chooser sheet when several newer APKs are available.
- 22.2 begins the Compose-led premium shell foundation with a Java-callable Compose backdrop layer behind the existing workflows, using true OLED black plus crisp neon studio linework instead of blurred or grainy effects.
- 22.3 rebuilds the Notes home presentation into a premium session dashboard with stronger current-session context, richer note cards, accent rails, compact metadata/previews, and view-built signal markers.
- 22.4 upgrades the editor into a premium Draft Studio surface with editor chrome, accent rail, compact session metadata, note-accent signal detail, and refined title/body field surfaces while preserving typing and rhyme hot paths.
- 22.5 adds meaningful bundled font assets: Space Grotesk, Share Tech Mono, and Silkscreen from Google Fonts under OFL, wired into editor rendering and font previews with fallback-safe loading.
- 22.6 rebuilds bottom sheets and menus as premium OLED command panels with stronger fixed handles/headers, command-row menu surfaces, refined style/font/glow sheets, and cleaner update chooser cards while preserving sheet behavior.
- 22.7 tunes the gesture and motion system with larger edge hitboxes, shared swipe completion helpers, faster sheet/dock/tap motion, and consistent workflow animation paths while preserving editor focus guards.
- 22.8 upgrades settings and personalization into preview-first OLED sheets with richer color, font, font-size, glow, and rhyme-setting controls while preserving saved settings behavior.
- 22.9 upgrades startup and preload visual identity with a premium OLED launch surface, crisp signal rails, refined preload status treatment, and unchanged rhyme preload behavior.
- 23.0 completes the milestone with cohesive global chrome, live context status for Notes/Editor, a refreshed header signal rail, and final dock polish while preserving app behavior.
- 23.1 makes the Compose backdrop lifecycle-safe by moving `MainActivity` to `ComponentActivity`, explicitly attaching view-tree owners to manual hosts, using lifecycle-based Compose disposal, and removing the blue radar/grid/waveform backdrop so the root returns to pure OLED black.
- 23.2 adds a shared motion foundation by centralizing tap, selection, panel, sheet, dock, swipe, and startup timing constants behind one eased workflow animator. It slows major transitions toward the Pixel-style visual direction while preserving quick tap response and existing gesture thresholds.
- 23.3 rebuilds modal/menu surfaces around pure-black OLED command panels, accent rails, thin neon separators, current-session context, and a darker black scrim across Main Menu, update chooser, settings rows, font/style rows, and rhyme settings controls.
- 23.4 rebuilds the Notes dashboard around the same pure-black OLED command language with a current-session header, flatter note cards, fixed signal slots, thin accent rails, and stable long-title/preview ellipsizing.
- 23.5 brings the editor into the pure-black command language with current-session title/meta chrome, a fixed accent rail/signal block, black editor/media surfaces, sharper rhyme popup/chips, expanded-rhyme context panels, and note-open selection motion.
- 23.6 polishes dock and gesture visuals with a fixed-height pure-black bottom dock, stronger active neon feedback, accent-aware icon/stroke updates, refreshed swipe rails, and panel/dock continuity without changing gesture thresholds.
- 23.7 adds a shipped offline rhyme hot-cache asset for default fast suggestions, loads it on the existing rhyme background thread, and serves it only for safe Balanced/default fast cases while all non-default settings and expanded/context paths fall back to the existing scorer.
- 23.8 clears the functional APK-size gate with runtime-used rhyme acceleration data: an uncompressed prepared index tried before TSV fallback and a default expanded-rhyme hot cache, both loaded on the existing rhyme background path with scorer fallbacks preserved.
- 23.9 performs final acceptance polish by converting remaining generic chrome/card surfaces to the pure-black command-surface language and adds a local QA plan for future laptop-based crash, screenshot, and jank validation without running emulator/device tests yet.
- 24.0 completes the pure-black OLED UI foundation milestone with the 23.1-23.9 UI, motion, dock, menu, Notes, editor, and rhyme acceleration work packaged as the final release line.
- Compose and Material 3 are enabled, but the live app is still mostly a Java view tree; the 22.1 to 23.0 milestone should progressively move visible shell, notes, editor, sheets, settings, and preload surfaces into a premium Compose-led interface while preserving storage/rhyme/media behavior.
- Rhyme settings for strictness, maximum suggestions, rhyme row visibility, exact-only mode, slang inclusion, and removed suggestions.
- Song attachment and playback controls.
- Voice recording, playback, rename, and save-to-disk support.
- App update checks through an online appcast manifest.

## Current Known Issues

- Most app behavior still lives in one large `MainActivity.java`, so changes outside rhyme/UI should remain carefully scoped.
- The 21.0 rhyme system is now extracted into `RhymeEngine.java`, but legacy rhyme helpers still remain in `MainActivity.java` and should be removed only after device validation.
- A focused rhyme regression check exists at `tools/rhyme_quality_check.py`, including `my/try`, `yours`, `out`, slang, phrase, and known bad-match cases.
- In-app update metadata points to the current JSONBlob appcast, but install flow still needs device verification on Pixel 10 Pro.
- JSONBlob/temp.sh/tmpfiles are temporary hosts; durable update hosting is still needed. The original JSONBlob manifest returned 404 during 23.1 release work, so 23.1+ points at replacement temporary blob `019f13c7-dc3f-7cf2-bf88-038a846852bd`.
- Rebuild B is still a Java-runtime bridge; the live app is not fully Compose yet.
- Published 23.0 / versionCode 56 could crash on startup from `ViewTreeLifecycleOwner not found`; 23.1 supersedes it with the lifecycle host fix and pure-black backdrop.

## Current Development Priority

24.0 release packaging is complete on the replacement JSONBlob/appcast line. Current priority is post-24.0 durable hosting and measured local QA setup.

## Next Milestone

Next: replace temporary hosting with durable hosting and run the planned emulator/screenshot/jank QA once Dave approves.
