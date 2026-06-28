# The Top Flow 21.x Rebuild A - UI Foundation

Last updated: 2026-06-28

## Reference Inputs

- Pulled through WSL Debian ADB from `/storage/emulated/0/codexreferences`.
- Current app screenshot shows oversized outlines, wrapped dock labels, heavy green/yellow styling, and low visual density.
- Target render collection points toward dark indigo/OLED surfaces, compact cards, mint accents, bottom navigation rhythm, restrained typography, and clear hierarchy.

The pulled images are local references only and are not intended for release packaging.

## Rebuild A Scope Completed

- Added Kotlin and Jetpack Compose support.
- Added Material 3 dependencies and dynamic dark color support.
- Added Compose UI foundation components: theme, studio screen, toolbar, dock, floating panels, rhyme strip, rhyme pills, and font preview rows.
- Added Java-native visual helper `TopFlowUiKit` so existing Java screens can migrate gradually.
- Added 21.x resource tokens for OLED canvas, floating panels, bottom dock, mint controls, quiet controls, editor page, and blur scrim.
- Added Gradle memory settings required for Compose/D8 release builds.

## Not Done Yet

- No notes, recording, playback, storage, or rhyme behavior was rewritten in Rebuild A.
- The current runtime UI is not fully migrated to Compose yet.
- Font preview UI is scaffolded, not wired into the live settings flow yet.
- The pulled Pixel reference images were inspected but not committed.

## Rebuild B Scope Completed

- Wired existing Java surfaces to the Rebuild A Java-native UI kit.
- Reworked shared buttons, media cards, editor card, and rhyme chips to use 21.x quiet controls and floating panels.
- Reworked the rhyme suggestion row to use the 21.x panel treatment while preserving offline engine behavior.
- Added live font preview rows in the font picker so each option displays in its own typeface before selection.
- Kept notes, local persistence, recording, playback, and rhyme scoring behavior intact.

## Rebuild C Scope Completed

- Moved the live command dock to the bottom to better match the target-render navigation rhythm.
- Tightened the top bar into a compact floating studio header.
- Shifted live surfaces toward OLED/indigo/mint 21.x tokens.
- Reworked note rows, editor fields, panels, and bottom sheets away from heavy neon/outline styling.
- Softened the backdrop gradients and sheet scrim.
- Preserved current notes, recording, playback, settings, update, and offline rhyme behavior.

## Next Section

Release should package this phased rebuild only after device review.
