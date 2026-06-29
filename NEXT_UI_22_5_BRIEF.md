# Next UI Brief: 22.5

## Purpose

Add meaningful local font assets that make the editor and font chooser feel less generic, without padding the APK with useless files.

## 5.3 Implementation Scope

- Add Space Grotesk, Share Tech Mono, and Silkscreen font files from Google Fonts.
- Wire `slim`, `terminal`, and `pixel` IDs to those bundled resources.
- Preserve safe fallbacks if font resources fail to load.
- Update editor/font preview call sites to use context-aware resource font loading.
- Add a concise OFL notice file.

## Codex Review Notes

- Accepted the bundled font wiring.
- Confirmed editor rendering and font preview rows use the context-aware font loaders.
- Confirmed the font files are real product assets, not filler bloat.
