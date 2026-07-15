# Local Phone Integration Research For Advanced Rhyming

Research date: 2026-07-04

## Executive Finding

The Top Flow is already pointed in the right direction: it is native Android, uses a local `RhymeEngine`, ships CMUdict and prepared rhyme index/cache assets, loads rhyme data asynchronously, and queries rhymes off the main thread. The next jump should not be an API integration. It should be a new local binary rhyme engine that combines:

- A larger phonetic lexicon.
- Precomputed exact/near/slant/multisyllabic/phrase candidate lists.
- Rapsody-style syllable and coda-class analysis.
- Hirjee/Brown-style learned phoneme similarity scoring.
- A small optional local G2P path for OOV words.
- Hot caches for common words and active-note context.

App size is not the blocker. Runtime parsing and per-keystroke work are the blockers.

## Current App State

Relevant inspected files:

- `app/src/main/java/com/davehq/thetopflow/RhymeEngine.java`
- `app/src/main/kotlin/com/davehq/thetopflow/NotesViewModel.kt`
- `tools/build_rhyme_hot_cache.py`
- `tools/rhyme_quality_check.py`
- `app/build.gradle`
- `app/src/main/assets/cmudict.dict`
- `app/src/main/assets/rhyme_index.tsv`
- `app/src/main/assets/rhyme_index_accel.tfindex`
- `app/src/main/assets/rhyme_hot_cache.tfcache`
- `app/src/main/assets/rhyme_expanded_hot_cache.tfcache`

Current shipped rhyme assets:

| Asset | Size |
| --- | ---: |
| `cmudict.dict` | 3,618,488 bytes |
| `rhyme_index.tsv` | 7,177,358 bytes |
| `rhyme_index_accel.tfindex` | 7,177,358 bytes |
| `rhyme_hot_cache.tfcache` | 1,304,912 bytes |
| `rhyme_expanded_hot_cache.tfcache` | 1,545,362 bytes |

Current runtime mechanics:

- Android app: native Kotlin/Java, Compose host.
- `compileSdk 35`, `minSdk 26`.
- Gradle already uses `noCompress += ["tfcache", "tfindex"]`.
- `RhymeEngine.loadAsync` loads in a dedicated background thread.
- If prepared index exists, it loads `rhyme_index_accel.tfindex` or `rhyme_index.tsv`.
- It builds maps in memory: `phonesByWord`, `exactIndex`, `familyIndex`, `dictionaryWords`, hot caches, result cache, info cache, family cache.
- It derives exact keys from the CMU rhyme part: phones from final stressed vowel to end.
- It derives family keys from vowel and coda.
- It scores candidates into exact, near, slant, phrase, and fallback buckets.
- It has hand-coded slang/OOV heuristics mostly around `ing`, `in`, and a few words.
- Fast suggestions use hot cache only for safe default options.
- ViewModel delays rhyme update by 140 ms, extracts the active last word, and calls `rhymeEngine.suggest(word, 8, 360, Options(...))` on `Dispatchers.Default`.
- Long notes over 16,000 chars disable rhyme suggestions to protect typing.

Current strengths:

- Local-first and offline.
- Conservative fallbacks.
- Background loading.
- Hot caches already proven as the app's safest performance direction.
- Rhyme quality regression script exists.

Current gaps:

- Text TSV parsing still requires loading/parsing every row at startup.
- Candidate generation is based on exact/family index plus small common lists, not a full precomputed graph.
- Slant scoring is limited compared with Rapsody/Hirjee.
- No true multisyllabic phrase-to-word or phrase-to-phrase suggestion engine.
- No syllable-level in-note rhyme analyzer.
- OOV/G2P is very limited.
- No frequency/POS/semantic metadata.
- No pronunciation confidence.
- No memory-mapped binary index.
- Hot cache is narrow: Balanced/default only, small limits.
- Query object only returns strings, not categorized rich candidates.

## Local Integration Feasibility

### What Can Run Locally

These can run entirely on phone:

- CMUdict lookup.
- Slang normalization.
- ARPAbet syllabification.
- Exact/near/slant scoring.
- Multisyllabic and phrase candidate lookup.
- In-note internal rhyme grouping.
- Frequency/POS/context reranking if metadata is shipped.
- G2P inference if a small model is shipped as `.tflite` or `.ort`, or if OOV pronunciations are precomputed.

These should not run as network integrations:

- Datamuse/RhymeZone API.
- RhymeBrain API.
- RapPad internals.
- Any cloud LLM or hosted lyric service for core rhyme suggestions.

### What Must Be Precomputed

The expensive work should move to Python build tools:

- Parse CMUdict and added lexicons.
- Normalize slang variants.
- Generate syllables and stress.
- Generate exact rhyme keys.
- Generate vowel/coda family keys.
- Generate Rapsody-style vowel/coda classes.
- Generate Hirjee-style score features.
- Generate candidate pools per word for exact, near, slant, multi, phrase, and consonant families.
- Rank default suggestions.
- Build hot caches for the most common 50,000 to 200,000 base terms.
- Build phrase-end indexes for 2 to 6 syllable endings.
- Build metadata tables for frequency, POS, phrase length, slang, offensive flag, confidence, and source.

Runtime should only:

- Normalize the active token/phrase.
- Read candidate IDs from a memory-mapped index.
- Apply small option filters.
- Apply small context boosts.
- Return structured candidates.

## Proposed Local Engine Architecture

### New Runtime Package

Add Kotlin-first wrappers while keeping Java compatibility:

- `RhymeEngine2.kt`: public engine facade used by ViewModel.
- `RhymeQuery.kt`: query input and options.
- `RhymeCandidate.kt`: rich output model.
- `RhymeAssetStore.kt`: memory-mapped asset reader.
- `RhymeScorer.kt`: final fast score/rerank logic.
- `RhymeContextAnalyzer.kt`: incremental active-note analysis.
- `RhymeLegacyBridge.java` or adapter: optional fallback to existing `RhymeEngine`.

Keep the old `RhymeEngine.java` as a fallback until version 29.0 hardening is complete.

### Query Model

Use a structured query instead of string-only lookup:

```kotlin
data class RhymeQuery(
    val textBeforeCursor: String,
    val activeToken: String,
    val activePhrase: String?,
    val limit: Int,
    val expanded: Boolean,
    val strictness: RhymeStrictness,
    val includeSlang: Boolean,
    val exactOnly: Boolean,
    val includePhrases: Boolean,
    val includeMultis: Boolean,
    val removed: Set<String>,
    val contextFingerprint: Long
)
```

### Candidate Model

Return metadata so the UI and tests can reason about quality:

```kotlin
data class RhymeCandidate(
    val surface: String,
    val normalized: String,
    val phones: String,
    val syllables: Int,
    val bucket: RhymeBucket,
    val score: Int,
    val rhymeKey: String,
    val familyKey: String,
    val confidence: PronunciationConfidence,
    val source: RhymeSource,
    val flags: Int
)
```

Buckets:

- `PERFECT`
- `NEAR`
- `SLANT`
- `ASSONANCE`
- `CONSONANCE`
- `MULTI`
- `PHRASE`
- `IN_NOTE`
- `OOV_G2P`

### Binary Assets

Ship larger local binary assets. Suggested names:

| Asset | Purpose |
| --- | --- |
| `rhyme_lexicon_v2.tflex` | Word/phrase ID table, normalized text, surface text, phones, syllables, stress, flags, source. |
| `rhyme_keys_v2.tfrhy` | Exact, family, assonance, coda, consonant, and multi-syllable keys. |
| `rhyme_candidates_v2.tfcand` | Per-word candidate lists by bucket with precomputed score components. |
| `rhyme_hot_v2.tfcache` | Top N default suggestions for common words and common phrase tails. |
| `rhyme_matrix_v2.tfscore` | Learned or hand-tuned vowel/coda/stress scoring matrices. |
| `rhyme_phrases_v2.tfphrase` | Multiword expression index by final syllable sequence and final word. |
| `rhyme_oov_v2.tfg2p` | Optional compact generated-pronunciation table for common slang/OOV words. |
| `rhyme_g2p_v2.tflite` | Optional local G2P model, only used when all dictionaries fail. |

Important: keep these extensions uncompressed in Gradle:

```groovy
androidResources {
    noCompress += ["tfcache", "tfindex", "tflex", "tfrhy", "tfcand", "tfscore", "tfphrase", "tfg2p", "tflite", "ort"]
}
```

Android's `AssetManager.openFd` can open uncompressed assets by memory mapping them. The current app already uses `noCompress` for cache/index extensions, so this is aligned with the app's existing direction.

Source:

- [Android AssetManager.openFd docs](https://developer.android.com/reference/android/content/res/AssetManager)

### Suggested Binary Format

Use little-endian binary files with a header:

```text
magic: 8 bytes, e.g. TFRHY2\0\0
version: uint16
flags: uint16
rowCount: uint32
stringTableOffset: uint64
sectionCount: uint16
sectionDirectoryOffset: uint64
checksum: uint64
```

Reasons:

- Fast validation.
- Random access without parsing text.
- Can be read from `MappedByteBuffer`.
- Asset corruption is detectable.
- Future versions can add sections without breaking old readers.

Do not use JSON, TSV, or SQLite for the hottest phonetic lookup path. SQLite/FTS may be useful for slower search screens, but candidate lookup should be direct binary offsets.

SQLite FTS5 is useful for large text search, but it is not the best structure for phoneme-tail candidate lookup. It can be a secondary thesaurus/search layer if a future version wants local word search.

Source:

- [SQLite FTS5 overview](https://www.sqlite.org/fts5.html)

## Local G2P Options

Use a tiered OOV path:

1. Existing lexicon exact lookup.
2. Normalized slang lookup: `runnin` -> `running`, `tryna`, `finna`, `imma`, contractions.
3. Precomputed OOV/slang table shipped in `rhyme_oov_v2.tfg2p`.
4. Tiny local G2P model only when all else fails.
5. Old spelling heuristic fallback for safety.

Do not call a G2P model on every keystroke. Cache generated pronunciations by normalized token and only run after the user pauses or opens expanded results.

Possible local model runtimes:

- LiteRT/TensorFlow Lite: best Android-native path for a small custom model. Android docs show model assets, no-compress, interpreter initialization, and background-thread inference. Google now positions LiteRT as its high-performance on-device ML framework.
- ONNX Runtime Mobile: viable if the chosen G2P model exports better to ONNX/ORT. It is an Android mobile inference package but adds another runtime dependency.

Recommendation:

- Version 25-28 should not require a model. Precompute OOV where possible.
- Version 29 can add an optional tiny G2P model if deterministic lexicon coverage still misses too much slang or proper-noun usage.

Sources:

- [LiteRT overview](https://developers.google.com/edge/litert)
- [Android TensorFlow Lite codelab](https://developer.android.com/codelabs/digit-classifier-tflite)
- [Android NNAPI migration guidance](https://developer.android.com/ndk/guides/neuralnetworks/migration-guide)
- [ONNX Runtime Mobile docs](https://onnxruntime.ai/docs/get-started/with-mobile.html)

## Candidate Generation Strategy

### Offline Builder

Create `tools/build_rhyme_engine_v2.py`.

Inputs:

- Existing `cmudict.dict`.
- Existing common rhyme words and phrases.
- Curated hip-hop slang list.
- Public frequency list after license review.
- Public phrase/multiword list after license review.
- Optional manually curated rap phrase bank.
- Optional generated OOV pronunciations.

Outputs:

- Binary assets listed above.
- Text debug dumps for review.
- Golden query report.
- Asset checksum manifest.

Build steps:

1. Load CMUdict.
2. Normalize words and variants.
3. Add pronunciation overrides.
4. Add slang and performance spellings.
5. Add phrase inventory.
6. Generate phoneme IDs.
7. Syllabify each pronunciation.
8. Generate exact tail key from last stressed vowel.
9. Generate Rapsody coda/vowel class keys.
10. Generate assonance keys for vowel-only sequences.
11. Generate consonance keys for coda/alliteration support.
12. Generate phrase-final keys for final 1-6 syllables.
13. Score pairwise candidates within each key group.
14. Write top candidates per bucket per base ID.
15. Build common-word hot cache.
16. Run `tools/rhyme_quality_check.py`.
17. Run new v2 quality and speed checks.

### Runtime Lookup

For a normal active word:

1. Normalize active word.
2. Resolve base ID from lexicon hash.
3. If hot cache has exact option match, return immediately.
4. Else read candidate list offsets by bucket.
5. Merge buckets in configured order.
6. Filter removed words and exact-only settings.
7. Apply context boosts from active note.
8. Deduplicate by normalized surface and final rhyme word.
9. Return top `limit`.

For a phrase:

1. Extract last 2-6 syllables from active phrase or active line ending.
2. Read phrase-final candidate list from `rhyme_phrases_v2.tfphrase`.
3. Merge word candidates and phrase candidates.
4. Rank by multi-syllable coverage, stress match, frequency, and context.

For OOV:

1. Try slang normalization.
2. Try precomputed OOV table.
3. Try local G2P if available and not on hot keystroke.
4. Try spelling heuristic.
5. Cache result.

## Context-Aware Ranking

Add a small incremental note analyzer.

State per selected note:

- Recent tokens around cursor.
- Active line tokens.
- Last 8 lines' phoneme/syllable summaries.
- Rhyme families already used.
- Topic/frequency hints if metadata exists.
- Removed suggestions.

Boosts:

- Candidate matches active line's syllable count target.
- Candidate extends an in-note rhyme family.
- Candidate avoids repeating exact same word unless user allows repeats.
- Candidate has compatible part of speech if detectable.
- Candidate is common enough for the current mode.
- Phrase candidate has matching syllable count and final stress.

Penalties:

- Candidate is only spelling-similar but phonetically weak.
- Candidate is too rare in fast row.
- Candidate repeats a nearby word.
- Candidate is offensive if offensive filtering is on.
- Candidate conflicts with exact-only setting.

## UI Integration

Keep the first UI change conservative:

- Existing compact rhyme row still shows top suggestions.
- Expanded sheet can show grouped sections:
  - Perfect
  - Near
  - Slant
  - Multis
  - Phrases
  - In this note
- Each chip can carry a hidden debug tag in development builds: bucket, score, phones.
- No visible feature explanations in production UI.

Use the existing `NotesViewModel.scheduleRhymeUpdate` path first. Do not rewrite notes, playback, recording, settings, appcast, or saved data.

## Performance Targets

Targets for Pixel-class phone:

| Path | Target |
| --- | ---: |
| Cached fast query | P50 <= 5 ms |
| Default fast query after ready | P95 <= 16 ms |
| Expanded query | P95 <= 50 ms |
| Active-note context update | <= 2 ms per edit chunk after debounce |
| Cold engine load blocking main thread | 0 ms |
| Cold engine ready after app start | <= 800 ms target, not user-blocking |
| Rhyme row UI update after typing pause | <= 100 ms perceived |

Implementation rules:

- Never parse full TSV on the hot path.
- Never score all dictionary words on the hot path.
- Never scan full note body on each keystroke.
- Never call G2P synchronously from the UI update path.
- Do not rebuild popup/sheet structures if suggestions are identical.
- Keep result objects immutable for Compose stability.

Android performance support:

- Current project already has Macrobenchmark and Baseline Profile structure.
- Add rhyme preload, first suggestion, expanded suggestion, and long-note typing to Macrobenchmark.
- Baseline Profiles help AOT compile hot code paths and reduce startup/runtime jank. Startup Profiles can further improve DEX layout for startup paths.

Sources:

- [Android Baseline Profiles overview](https://developer.android.com/topic/performance/baselineprofiles/overview)
- [Android Startup Profiles docs](https://developer.android.com/topic/performance/startupprofiles/dex-layout-optimizations)

## Testing Plan

Extend `tools/rhyme_quality_check.py` instead of replacing it.

Add test groups:

- Exact classics: `my`, `try`, `time`, `out`, `yours`.
- Near/slant: `hover`/`lover`/`cover`, `booth`/`roof`/`masseuse`.
- Slang: `running`/`runnin`, `tryna`, `finna`, `imma`, `thang`.
- Multis: `any day`/`many ways`, `battery`/`battle me`, `cigar`/`disregard`.
- Internal: line snippets with same-family internal rhymes.
- Phrase ranking: phrases must not outrank very strong single-word exact rhymes unless expanded phrase mode is active.
- Bad matches: `my` must not bring `stay/play` in top exact row; `out` must not bring `near/clear`.
- OOV confidence: generated pronunciations marked lower confidence.
- Removed suggestions stay removed across engine versions.

Add Android tests:

- Engine asset validation.
- Golden result tests for top 8 and expanded 24.
- `NotesViewModel` cancellation test: stale body cannot publish stale suggestions.
- Long-note guard still works.
- Compose screenshot test for grouped expanded sheet.
- Macrobenchmark for typing with rhyme row.

## Risk Assessment

| Risk | Severity | Mitigation |
| --- | --- | --- |
| License risk from frequency/phrase corpora | High | Add license review gate before committing external corpora. Keep generated assets traceable. |
| Startup memory growth | Medium | Memory map assets, lazy-load sections, cap Java object materialization. |
| Bigger APK install/update size | Low per Dave's direction | Keep appcast/release path tested. Use uncompressed only where mmap needs it. |
| Slang/G2P wrong pronunciations | Medium | Confidence flags, overrides, regression tests, user-facing fallback remains old engine. |
| Ranking regressions | High | Keep old engine fallback through 29.0; golden tests per version. |
| UI jank | High | Main thread never reads assets or scores candidates. Macrobenchmark before release. |
| Overcomplicated first release | Medium | Ship data/reader first with behavior parity before changing results. |

## Integration Conclusion

This is feasible locally and should be done incrementally. The app should not try to embed Rapsody's Python code or call Datamuse/RhymeZone. It should rebuild the mechanics as an Android-native, precomputed, binary-asset engine. The largest wins will come from moving from runtime scoring/parsing to precomputed candidate graphs, then improving scoring quality with syllable-level and multi-phrase logic.

