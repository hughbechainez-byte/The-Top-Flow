# The Top Flow 21.9 UI Alpha Brief

## General Summary
The Top Flow is a native Android writing app focused on fast lyric/note capture, rhyme assistance, and a high-resolution OLED-neon interface. The current 21.x run is rebuilding the app around crisp text, restrained surfaces, responsive gestures, and menu interactions that feel modern without low-resolution effects.

## Most Recent Update: 21.8
21.8 added velocity-aware editor-to-Notes and Notes-to-editor swipes, shared settle/complete animation helpers, and a responsive current-session signal in the Notes menu header. It preserved rhyme behavior and kept appcast/manifest publishing untouched.

## Implement 21.9
Treat yourself as an ambitious junior coder working under a senior development lead. Make a focused 21.9 pass with these three small implementations:

1. **Crisp swipe affordance**
   Add a subtle high-resolution edge affordance during the editor <-> Notes swipes. It should be a thin OLED-neon rail or glow-like solid/vector View that tracks gesture progress and fades/reset cleanly on cancel or completion. Do not use bitmap blur, grain, shadows that look fuzzy, or layout-shifting text.

2. **Smoother workflow animation hygiene**
   Improve the existing panel/screen-swap/dock gesture animations by using one consistent easing/interpolator and temporary hardware-layer animation where appropriate. Cancelled gestures must restore alpha, scale, translation, and any affordance state. Keep this scoped to current Java UI unless a new system is clearly necessary.

3. **Main menu copy and density cleanup**
   Remove remaining visible instructional copy from the Notes/main menu area, especially empty-state text that explains what to tap. Replace it with state/status language only. Make title, preview, metadata, and status text single-line/ellipsized where needed so narrow Pixel widths cannot create overlap or rough text wrapping.

## Hard Requirements
- Bump defaults to `versionCode 45` and `versionName 21.9`.
- Update README current local build to `21.9`.
- Do not edit `appcast.json`, `releases/appcast.json`, JSONBlob, or any live update manifest.
- Preserve rhyme behavior.
- Run `python3 tools/rhyme_quality_check.py` if possible and report exactly what changed plus any issues.
