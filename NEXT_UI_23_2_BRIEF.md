# Next UI Brief: 23.3

## Summary

23.2 established the shared motion foundation for the 23.1 to 24.0 pure-black UI run. Major panel, sheet, dock, swipe, and startup transitions now use grouped timing constants and a smoother eased animator, while tap and selection feedback stay quick.

## Review Notes

- Keep the background pure OLED black. Do not reintroduce blue radar, grid, waveform, or decorative image layers.
- Treat neon/glow as active UI emphasis only, not a background texture.
- Preserve note storage, rhyme lookup, recording/playback, update checks, and gesture thresholds.

## Suggested 23.3 Implementations

1. Rework the Main Menu sheet surface into a cleaner pure-black command panel with tighter hierarchy, crisper separators, and stronger current-note context.
2. Apply the same OLED command-panel treatment to the update chooser and settings entry surfaces so menu surfaces feel like one system.
3. Add subtle blur/dim consistency around modal states without increasing background brightness or adding texture.
