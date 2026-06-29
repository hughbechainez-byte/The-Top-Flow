# Known Issues

## Confirmed Bugs

- 23.0 / versionCode 56 has a blocker startup crash from the Compose backdrop path: `ViewTreeLifecycleOwner not found from android.widget.FrameLayout`. See `V23_CRASH_LOG.md`. A lifecycle-safe Compose host fix candidate is implemented and release-builds locally, but needs live device validation before 23.1 is published.
- Prior 21.1 device logs showed 5-16s rhyme generation and main-thread jank symptoms before the 21.2 performance pass.
- 21.2 still lagged on Pixel 10 Pro; automated rhyme timing did not cover the UI-thread Rhyme button and editor hot paths.

## Suspected Bugs

- 21.3 still needs Pixel 10 Pro validation to confirm Rhyme button latency, keyboard smoothness, and popup churn.

## Blocked Work

- Durable update hosting is still needed; JSONBlob/temp.sh/tmpfiles are temporary.

## Future Investigations

- Review 21.3 `rhyme_trace` logs after repeated Rhyme button taps and rapid caret movement.
- Confirm 21.3 visual quality against fresh device screenshots.
- Validate keyboard/rhyme row behavior on long notes after async expanded-rhyme changes.
- Validate note color, font size, note glow, and swipe-down bottom sheets.
- Check recording and playback controls after the UI rebuild.
