# Next UI Brief: 23.8

## Summary

23.7 added a real shipped default fast-rhyme hot cache and a conservative loader. The APK grew from about 9.4 MB to about 10.7 MB, which is useful but still short of Dave's required 24.0 size gate.

## Review Notes

- Do not add filler. Any additional size must be runtime-used rhyme/index data or shipped UI assets/systems.
- Keep the 23.7 hot-cache eligibility conservative.
- Preserve all existing rhyme quality checks and UI behavior.

## Suggested 23.8 Implementations

1. Add a larger runtime-used rhyme acceleration asset, such as a no-compressed prepared index/cache that `RhymeEngine` actually reads before falling back to TSV parsing.
2. If UI assets are added, use them only for crisp active neon emphasis and keep the background pure black.
3. Verify the release APK is moving toward the required 2x size target while remaining explainable as useful data/assets.
