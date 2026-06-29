# Next UI Brief: 23.7

## Summary

23.6 polished the bottom dock and swipe affordances into the same pure-black neon language used by Notes, Menu, and Editor surfaces. Gesture thresholds and behavior stayed unchanged.

## Review Notes

- The next APK-size increase must be functional, not filler.
- Preserve rhyme quality and all existing settings behavior.
- Hot-cache reads should be conservative and fall back to the existing scorer whenever settings or user removals make precomputed data unsafe.

## Suggested 23.7 Implementations

1. Generate a shipped offline hot-cache asset for common default fast rhyme suggestions using the existing rhyme quality logic.
2. Load the hot cache in `RhymeEngine` and serve it only for safe default fast-suggestion cases: default balanced settings, slang enabled, exact-only disabled, no removed suggestions, and matching limit/candidate profile.
3. Configure the cache asset so release APK growth is visible and justified by runtime data, while keeping fallback behavior intact for all non-default cases.
