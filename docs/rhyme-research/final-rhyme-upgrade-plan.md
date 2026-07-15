# Final Rhyme Upgrade Report And Implementation Plan

Research date: 2026-07-04

## Final Summary

The research found no single rhyming system that can be integrated directly and satisfy the goal: complete local phone operation with near-instant results. The best path is to build The Top Flow's own local engine from the best mechanics:

- Current Top Flow: keep the offline-first Android foundation, CMUdict asset, prepared indexes, hot caches, background loading, and conservative fallbacks.
- Rapsody: add syllable-level stressed rhyme logic, coda/vowel classes, internal multis, line-end multis, function-word handling, and OOV tiering.
- Hirjee/Brown: add rap-specific phoneme similarity scoring with vowel/coda/stress matrices and local-alignment thinking.
- Datamuse/RhymeZone: replicate the local shape of the product, not the API: large vocabulary, phrases, frequency, syllables, pronunciation metadata, context ranking, and clear rhyme categories.
- Auto Rap Highlighter: borrow pronunciation-variant selection and feature-distance ideas for offline scoring/build tools.
- RhymeBrain: borrow score bands, pronunciation confidence, and frequency fields.
- DeepRapper: borrow N-gram rhyme and reverse-ending orientation for phrase suggestions.

The implementation should happen across 5 major versions, with .1 and .2 stabilization passes after each major version. Each major version must preserve notes, playback, recording, settings, appcast, saved data, and existing offline rhyme behavior until the replacement path is proven.

## Global Rules For All Versions

- Do not remove `RhymeEngine.java` until version 29.0 is accepted.
- Do not alter note persistence format except through tested migrations.
- Do not block the main thread with asset reads, phoneme parsing, G2P, or full-note scans.
- Do not require network for core rhyme features.
- Keep every release buildable with `assembleRelease`.
- Keep `tools/rhyme_quality_check.py` passing.
- Add new checks before changing runtime behavior.
- Keep release notes/appcast updates outside the engine work until a release packaging pass.
- Any new asset format must include magic, version, checksum, and compatibility guard.
- Any new result ranking must support a legacy fallback switch.

## Version 25.0 - Rhyme Engine V2 Data Foundation

Goal: add the offline build pipeline and binary asset reader without changing user-visible rhyme behavior by default.

### 25.0 Tasks

1. Add `tools/build_rhyme_engine_v2.py`.
2. Make it read `app/src/main/assets/cmudict.dict`.
3. Make it read existing `RhymeEngine.java` common words/phrases or move those lists to a shared generated source/input file.
4. Add a curated input file `tools/rhyme_sources/hiphop_slang.tsv` with columns:
   - `surface`
   - `normalized`
   - `phones`
   - `source`
   - `confidence`
   - `notes`
5. Seed slang with the current engine's covered terms plus common rap spellings:
   - `runnin`
   - `movin`
   - `pullin`
   - `tryna`
   - `finna`
   - `imma`
   - `ima`
   - `gimme`
   - `lemme`
   - `nah`
   - `naw`
   - `thang`
   - `homie`
   - `shorty`
6. Generate `build/rhyme-v2-debug/lexicon_debug.tsv`.
7. Generate binary `app/src/main/assets/rhyme_lexicon_v2.tflex`.
8. Add binary asset header:
   - magic `TFLEX2`
   - version `2`
   - row count
   - section offsets
   - checksum
9. Store each lexicon row:
   - id
   - normalized string offset
   - display string offset
   - phones string offset
   - syllable count
   - stress pattern offset
   - flags
   - confidence
10. Add `app/src/main/kotlin/com/davehq/thetopflow/rhyme/RhymeAssetStore.kt`.
11. Implement `RhymeAssetStore.openMappedAsset(name: String)`.
12. Use `AssetManager.openFd` for uncompressed assets.
13. Fall back to stream copy into `filesDir/rhyme-assets/` only if `openFd` fails.
14. Add extension list to `app/build.gradle`:
   - `tflex`
   - `tfrhy`
   - `tfcand`
   - `tfscore`
   - `tfphrase`
   - `tfg2p`
   - `tflite`
   - `ort`
15. Add `RhymeAssetHeaderTest` local JVM test if the project has unit-test wiring; otherwise add a Python validation script first.
16. Add `tools/validate_rhyme_assets_v2.py`.
17. Add validation to check:
   - asset exists
   - magic is valid
   - version is valid
   - checksum matches
   - row count > 100,000
   - required seed words exist
18. Do not route UI suggestions to V2 yet.

### 25.0 Acceptance

- `tools/validate_rhyme_assets_v2.py` passes.
- Existing `tools/rhyme_quality_check.py` passes.
- Release build still packages old and new assets.
- App behavior is unchanged unless a debug flag is enabled.

## Version 25.1 - Data Reader Fix Pass

Goal: fix binary loading, checksum, and Gradle packaging issues found after 25.0.

### 25.1 Tasks

1. Install a debug APK on Pixel/emulator and verify `openFd` works for `rhyme_lexicon_v2.tflex`.
2. Log asset load time and mapped byte size under `rhyme_trace`.
3. Ensure no `IOException` from compressed assets.
4. If any `openFd` failure occurs, fix `noCompress` before adding runtime copy fallback.
5. Add a check in app startup that validates header only, not full parse.
6. Ensure validation runs off main thread.
7. Add regression test for corrupt magic and unsupported version.

### 25.1 Acceptance

- App starts with V2 asset present.
- Header validation is background-only.
- No user-visible rhyme behavior changes.

## Version 25.2 - Data Quality Fix Pass

Goal: clean lexicon quality before using it.

### 25.2 Tasks

1. Add `tools/rhyme_v2_golden_queries.tsv`.
2. Include at minimum:
   - `my`
   - `try`
   - `time`
   - `out`
   - `yours`
   - `hover`
   - `cover`
   - `moving`
   - `running`
   - `cigar`
   - `battery`
3. Add validation for syllable counts and phones on seed slang.
4. Add duplicate-normalized-word detection.
5. Add pronunciation override report.
6. Document every external data source and license in `app/src/main/assets/THIRD_PARTY_NOTICES.txt`.

### 25.2 Acceptance

- Lexicon debug report is reviewable.
- No unknown license source is included in committed assets.

## Version 26.0 - Rhyme Scoring V2

Goal: add a new scorer that can produce exact, near, slant, and assonance candidates from V2 data while old UI still has fallback.

### 26.0 Tasks

1. Add `tools/build_rhyme_score_matrix_v2.py`.
2. Start with hand-tuned matrices based on Rapsody/Hirjee mechanics:
   - exact vowel match: high positive
   - compatible vowel class: medium positive
   - incompatible vowel: strong negative
   - exact coda match: high positive
   - coda class match: medium positive
   - fricative class match: small positive
   - nasal class match: medium positive
   - r-color mismatch: penalty
   - stress mismatch: penalty
3. Output `app/src/main/assets/rhyme_matrix_v2.tfscore`.
4. Add `tools/build_rhyme_candidates_v2.py`.
5. Generate candidate lists for each base ID:
   - perfect
   - near
   - slant
   - assonance
   - consonance
6. Cap each bucket initially:
   - perfect: 512
   - near: 512
   - slant: 512
   - assonance: 256
   - consonance: 128
7. Write `app/src/main/assets/rhyme_candidates_v2.tfcand`.
8. Add `RhymeScorer.kt`.
9. Add `RhymeCandidate.kt`, `RhymeBucket.kt`, `RhymeStrictness.kt`.
10. Add `RhymeEngine2.kt`.
11. `RhymeEngine2.suggest()` must:
   - normalize active word
   - map word to ID
   - read bucket offsets
   - merge buckets
   - filter removed words
   - dedupe
   - return top candidates
12. Add a debug-only comparison path:
   - old engine top 8
   - new engine top 8
   - log overlap and top differences
13. Keep production UI using old engine by default.
14. Add a local flag in code, not settings UI yet:
   - `private const val USE_RHYME_ENGINE_V2 = false`
15. Extend `tools/rhyme_quality_check.py` or add `tools/rhyme_quality_check_v2.py`.
16. Add golden assertions:
   - `my` top exact includes `try`, `fly`, `sky`, `high`, `why`
   - `out` top exact includes `bout`, `clout`, `shout`, `doubt`, `about`
   - `yours` includes `soars`, `pores`, `doors`, `floors`
   - `running` includes `runnin`
   - `moving` includes `proving`, `grooving`
   - bad pairs are absent from top row.

### 26.0 Acceptance

- V2 quality check passes.
- V2 can run in debug comparison without UI regressions.
- Main thread remains clean.
- Old engine remains default.

## Version 26.1 - Scoring Rework Pass

Goal: tune V2 ranking against golden outputs.

### 26.1 Tasks

1. Review debug comparison logs for 100 common words.
2. Tune bucket ordering:
   - perfect first
   - near second
   - slant third
   - assonance only when expanded or loose
3. Add frequency bias placeholder even if frequency data is basic.
4. Add pronunciation confidence penalty.
5. Add exact-only behavior.
6. Add strictness behavior:
   - Strict: perfect + high near only
   - Balanced: perfect + near + selected slant
   - Loose: all buckets
7. Add removed-suggestion filtering parity with old engine.

### 26.1 Acceptance

- V2 top rows are not worse than old engine for existing regression cases.
- Exact-only and removed suggestions work.

## Version 26.2 - Runtime Performance Fix Pass

Goal: make V2 fast enough to become default later.

### 26.2 Tasks

1. Add microbenchmark script for `RhymeEngine2.suggest()`.
2. Add Android Macrobenchmark route for:
   - open editor
   - type word
   - observe rhyme row update
3. Add `RhymeResultCache` with LRU cache:
   - key: normalized word, options, removed hash, context fingerprint
   - value: immutable candidate list
4. Prewarm:
   - current old common rhyme words
   - last opened note active word
   - first 200 common words from V2 lexicon
5. Ensure V2 expanded query is cancellable.

### 26.2 Acceptance

- Default fast query P95 <= 16 ms after ready in local benchmark.
- Expanded query P95 <= 50 ms after ready.

## Version 27.0 - Multisyllabic And Phrase Suggestions

Goal: add real hip-hop multis and phrase candidates locally.

### 27.0 Tasks

1. Add `tools/rhyme_sources/phrases_seed.tsv`.
2. Seed with existing common phrases and curated writing phrases.
3. Add phrase input format:
   - `surface`
   - `normalized`
   - `phones`
   - `syllables`
   - `final_word`
   - `source`
   - `confidence`
4. Extend builder to generate phrase phones from word phones.
5. Add phrase-final keys for last 2, 3, 4, 5, and 6 syllables.
6. Add `rhyme_phrases_v2.tfphrase`.
7. Add multisyllabic word keys:
   - final 2 syllable vowel+coda sequence
   - final 3 syllable vowel sequence
   - final 4-6 syllable vowel sequence for phrase search
8. Add candidate buckets:
   - `MULTI`
   - `PHRASE`
9. Add query extraction:
   - active word
   - active two-word phrase before cursor
   - active line ending up to 6 syllables
10. Add `RhymeEngine2.suggestPhrases()`.
11. Expanded sheet should show phrase/multi sections.
12. Fast row can include at most 1 phrase by default, unless phrase mode is active.
13. Add golden cases:
   - `cigar` finds internal/multi-like candidates such as `disregard` family.
   - `battery` can find `battle me` if phrase seed includes it.
   - phrase results do not bury exact single-word rhymes in fast row.

### 27.0 Acceptance

- Expanded suggestions include useful multis/phrases.
- Fast row remains clean and not phrase-heavy.
- Old engine fallback still available.

## Version 27.1 - Multis Ranking Fix Pass

Goal: stop weak multis and phrase spam.

### 27.1 Tasks

1. Add penalties for phrases with low confidence.
2. Add penalties for too-rare phrases in fast mode.
3. Require at least 2 matching syllables for `MULTI`.
4. Require at least 2 matching final syllables for phrase suggestions.
5. In fast row, require phrase score to exceed best single-word slant score by margin.
6. Add tests where phrases must not outrank strong exact rhymes.
7. Add duplicate final-word dedupe.

### 27.1 Acceptance

- Phrase results feel intentional.
- Existing exact rhyme rows remain stable.

## Version 27.2 - Expanded Rhyme UI Fix Pass

Goal: make grouped results usable without clutter.

### 27.2 Tasks

1. Update expanded rhyme sheet to consume `RhymeCandidate`.
2. Render grouped sections only when non-empty.
3. Preserve current visual style.
4. Add chip click behavior identical to existing suggestion insertion if that exists, or same display behavior if insertion is not supported.
5. Add debug build long-press/click metadata view only if safe.
6. Screenshot test expanded sheet with:
   - exact only
   - mixed exact/slant
   - phrase section
   - empty state

### 27.2 Acceptance

- No overlapping text.
- Existing rhyme row still works.
- Expanded sheet remains responsive.

## Version 28.0 - Local Context And In-Note Rhyme Intelligence

Goal: add active-note rhyme awareness without per-keystroke full-note scans.

### 28.0 Tasks

1. Add `RhymeContextAnalyzer.kt`.
2. Maintain context per selected note:
   - normalized tokens
   - phone IDs
   - syllable summaries
   - line boundaries
   - recent rhyme family keys
   - context fingerprint
3. Update context incrementally after body edits with debounce.
4. Do not analyze notes over existing long-note limit until optimized.
5. Add `IN_NOTE` candidate bucket.
6. Boost candidates matching:
   - active line family
   - previous 4 lines
   - current hook/repeated phrase
7. Penalize direct repetition unless repetition is exact same rhyme-family anchor and not same surface.
8. Add near-miss detection:
   - same vowel but coda one class away
   - same coda but vowel compatible
9. Add optional in-note section in expanded sheet.
10. Add tests with short lyric snippets.

### 28.0 Acceptance

- Context boosts improve ranking in test snippets.
- Typing remains smooth.
- No full note scan on every character.

## Version 28.1 - Context Performance Fix Pass

Goal: protect long notes and rapid typing.

### 28.1 Tasks

1. Add context analyzer cancellation.
2. Add generation IDs so stale context cannot publish.
3. Add max tokens per incremental pass.
4. Add fallback to last stable context if analyzer is busy.
5. Add metrics:
   - context update ms
   - tokens processed
   - cache hits
6. Macrobenchmark rapid typing in a long note.

### 28.1 Acceptance

- No visible typing freeze.
- Context analyzer never blocks save, playback, recording, or UI.

## Version 28.2 - In-Note Quality Fix Pass

Goal: reduce noisy context suggestions.

### 28.2 Tasks

1. Tune context boost weights.
2. Add bad-match tests from real notes if available.
3. Avoid suggesting the same word already used in the current line.
4. Favor candidates with same syllable count when matching a repeated cadence.
5. Add confidence indicators in debug logs.

### 28.2 Acceptance

- In-note suggestions help without dominating normal rhymes.
- Regression tests pass.

## Version 29.0 - OOV/G2P, Default Switch, And Release Hardening

Goal: make V2 the default engine and add robust local unknown-word handling.

### 29.0 Tasks

1. Decide if G2P model is needed after 25-28 data coverage.
2. If needed, train or select a small G2P model.
3. Export to `.tflite` first unless ONNX proves smaller/faster.
4. Add dependency only if model is accepted:
   - LiteRT/TFLite runtime, or ONNX Runtime Mobile.
5. Add `RhymeG2p.kt`.
6. Run G2P only when:
   - word not found
   - slang normalization fails
   - precomputed OOV table fails
   - user has paused or opened expanded suggestions
7. Cache G2P output in memory.
8. Mark candidates from G2P as lower confidence.
9. Flip `USE_RHYME_ENGINE_V2 = true`.
10. Keep emergency fallback:
   - if V2 asset validation fails, use old `RhymeEngine`.
11. Update `NotesViewModel` to consume structured candidates but expose strings to old UI where needed.
12. Add final Macrobenchmark coverage:
   - startup
   - editor open
   - first rhyme after preload
   - fast row while typing
   - expanded sheet
   - long note guard
13. Regenerate Baseline Profile including V2 paths.
14. Update `THIRD_PARTY_NOTICES.txt`.

### 29.0 Acceptance

- V2 is default.
- Old engine fallback works on asset validation failure.
- Offline mode works with no network.
- Fast query P95 <= 16 ms after ready.
- Expanded query P95 <= 50 ms after ready.
- Existing notes/playback/recording/settings/appcast unaffected.

## Version 29.1 - Device Fix Pass

Goal: fix real Pixel/emulator findings after default switch.

### 29.1 Tasks

1. Install on target phone.
2. Capture logs for:
   - engine load
   - first query
   - fast query
   - expanded query
   - G2P fallback
3. Fix asset mapping problems.
4. Fix memory spikes.
5. Fix stale result races.
6. Fix any UI jank in Compose.
7. Add regression tests for every fixed issue.

### 29.1 Acceptance

- No device crash.
- No repeatable typing freeze.
- No stale suggestions after rapid edits.

## Version 29.2 - Final Quality And Documentation Pass

Goal: lock the upgrade as a stable release foundation.

### 29.2 Tasks

1. Expand golden query set to at least 250 terms.
2. Add at least 50 hip-hop multi/phrase cases.
3. Add at least 50 negative tests.
4. Add `docs/rhyme-engine-v2.md` with:
   - asset format
   - build commands
   - runtime architecture
   - fallback rules
   - performance targets
   - test commands
5. Add release notes.
6. Build release APK.
7. Verify APK contains all required uncompressed assets.
8. Run:
   - old quality check
   - v2 quality check
   - asset validation
   - assembleRelease
   - Macrobenchmark if device is attached

### 29.2 Acceptance

- Coder can rebuild assets from docs.
- Rhyme V2 is default and documented.
- Release APK passes local checks.

## Final Implementation Order

1. Data and asset validation first.
2. Binary reader second.
3. V2 scorer in debug comparison third.
4. V2 default fast suggestions fourth.
5. Multis and phrases fifth.
6. Context analyzer sixth.
7. Optional G2P seventh.
8. Default switch and hardening last.

This order protects the app. It lets the coder build the advanced system while keeping a working local rhyme engine at every release.

