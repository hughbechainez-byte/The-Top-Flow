# The Top Flow 22.0 UI Alpha Brief

## General Summary
The Top Flow is a native Android writing app for fast lyric/note capture, rhyme assistance, voice capture, and local-first songwriting. The 21.6-21.9 run pushed the app toward a crisp OLED-neon UI with solid high-resolution surfaces, blur-backed sheets, velocity-aware gestures, recent-session density, and cleaner animation state.

## Most Recent Update: 21.9
21.9 added a thin edge rail that tracks editor <-> Notes swipe progress, unified easing/hardware-layer animation hygiene, and cleaned the Notes menu copy/density. Senior review kicked back tracker cleanup and a bad rail color reference; those were corrected before packaging.

## Implement 22.0
Treat yourself as an ambitious junior coder working under a senior development lead. Make this a convergence pass, not a broad rewrite. Implement these three small but important items:

1. **Main Menu recent-session preview**
   Update the top `Menu` sheet so it includes a compact recent-session preview: the current note if available plus the next few recent notes, each with title and metadata. Rows should use the existing high-resolution OLED panel language, be single-line/ellipsized where needed, and open the selected note. Keep it useful as a menu, not a landing page.

2. **Icon-backed command polish**
   Use the existing vector icons in `app/src/main/res/drawable` to add crisp icon+text affordances to key commands where icons already exist: top Menu, `+ Note`, main menu commands, and dock items where it can be done without layout crowding. Tint icons consistently with the existing text/accent colors. Avoid new bitmap assets.

3. **Final blur and motion consistency sweep**
   Apply the shared workflow interpolator/layer-backed animation approach to any remaining sheet/backdrop/menu motion that still uses the older plain animation path. Ensure blur/dim restores cleanly after any sheet dismissal, menu command, or cancelled gesture. Do not add fuzzy shadows, grain, or low-resolution effects.

## Hard Requirements
- Bump defaults to `versionCode 46` and `versionName 22.0`.
- Update README current local build to `22.0`.
- Do not edit `appcast.json`, `releases/appcast.json`, JSONBlob, or any live update manifest.
- Preserve rhyme behavior.
- Run `python3 tools/rhyme_quality_check.py` if possible and report exactly what changed plus any issues.
