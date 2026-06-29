# Next UI Brief: 22.7

## Purpose

Make gestures and motion feel smoother, faster, and more forgiving while preserving typing, sheet safety, and current command behavior.

## 5.3 Implementation Scope

- Tune tap/selection/sheet/dock/swipe motion constants.
- Increase edge-swipe hitboxes without making accidental navigation likely.
- Add shared helpers for swipe tracking, abort, completion, and sheet dismissal.
- Keep editor swipe disabled while title/body inputs are focused.
- Route more animation paths through the shared workflow animator.

## Codex Review Notes

- Accepted the shared gesture helpers and threshold tuning.
- Replaced a selection-release alpha reset that reused the scale constant with an explicit `1f` alpha reset.
- Confirmed no text watcher, keyboard/IME, update, storage, media, or rhyme behavior changed.
