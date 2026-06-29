# Next UI Brief: 22.3

## Purpose

Make the Notes home feel like a premium session dashboard instead of a basic list, while keeping note storage, creation, opening, and editor behavior unchanged.

## 5.3 Implementation Scope

- Rework the Notes command header into a stronger dashboard summary.
- Add active/open session treatment.
- Upgrade note rows with accent rails, compact metadata, lyric preview hierarchy, and view-built signal markers.
- Keep long titles/previews ellipsized.
- Preserve note creation and note opening behavior.

## Codex Review Notes

- Accepted the dashboard/header/card direction.
- Replaced the misleading decorative `Live` pill with a factual `Local` label.
- Tightened empty-state copy back to a minimal status.
- Changed signal jitter math to `Math.floorMod` to avoid negative bar heights from overflow/modulo edge cases.
