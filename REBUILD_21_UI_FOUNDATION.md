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

## Next Section

Rebuild B should wire the app's internal systems to the new foundation: editor state, note controls, recording controls, playback controls, font picker with real previews, and all-offline rhyme suggestion bar behavior.
