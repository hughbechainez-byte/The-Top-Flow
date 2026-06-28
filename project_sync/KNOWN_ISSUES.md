# Known Issues

## Confirmed Bugs

- Prior 21.1 device logs showed 5-16s rhyme generation and main-thread jank symptoms before the 21.2 performance pass.

## Suspected Bugs

- 21.2 still needs Pixel 10 Pro validation to confirm rhyme latency and keyboard smoothness on long notes.

## Blocked Work

- Durable update hosting is still needed; JSONBlob/temp.sh are temporary.

## Future Investigations

- Confirm 21.2 visual quality against fresh device screenshots.
- Validate keyboard/rhyme row behavior on long notes after the fast-row cache changes.
- Validate note color, font size, note glow, and swipe-down bottom sheets.
- Check recording and playback controls after the UI rebuild.
