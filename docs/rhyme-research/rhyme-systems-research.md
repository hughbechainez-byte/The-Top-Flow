# Advanced Hip-Hop Rhyming Systems Research

Research date: 2026-07-04

## Executive Finding

The most useful rhyming systems for The Top Flow are not generic "find words ending like this" dictionaries. Hip-hop quality comes from phonetic scoring, stress, internal rhyme, multisyllabic/mosaic phrases, slang and OOV handling, and context-aware ranking.

The strongest open-source implementation found is Rapsody's `backend/analyzer.py`, because it works at syllable level, detects line-end multis, internal multis, stressed single-syllable rhymes, uses CMUdict plus G2P, and ships validation notes against MCFlow. The strongest academic foundation is Hirjee and Brown's ISMIR 2009 rap rhyme detector, because it models imperfect/internal rap rhyme using probabilistic vowel/coda/stress scoring and local alignment. Closed systems like Datamuse/RhymeZone/RhymeBrain/RapPad are useful benchmarks for scope and ranking, but cannot be embedded locally as-is.

The recommended direction is a hybrid local engine:

- Keep CMUdict and the current app's fast offline cache idea.
- Add Rapsody-style syllable analysis and multi-run detection.
- Add Hirjee-style learned phoneme similarity scoring.
- Add a Datamuse/RhymeZone-like local candidate graph with frequency, phrase, syllable, and context metadata.
- Add a fallback G2P path for slang, invented words, and misspellings.

## What "Advanced" Means For Hip-Hop

An advanced hip-hop rhyme system should support:

- Exact rhyme: same ARPAbet tail from stressed vowel to end.
- Near/slant rhyme: same or compatible vowel nucleus with compatible coda classes.
- Assonance: shared vowel sequence even when codas differ.
- Consonance/alliteration: shared consonant shape when useful.
- Internal rhyme: rhyme inside a line, not only line endings.
- Multisyllabic rhyme: matching 2 to 6 syllable runs.
- Mosaic/cross-word rhyme: phrase-to-word and phrase-to-phrase matches, such as "battery" / "battle me".
- Slang and performance spelling: "runnin", "tryna", "finna", dropped g, dialectal endings.
- OOV handling: generated phones for names, invented words, and uncommon terms.
- Context ranking: favor words that fit the current note, line, topic, part of speech, frequency, and prior rhyme family.
- Instant local latency: lookup is precomputed and memory resident or memory mapped.

## Systems Inspected

### 1. Hirjee and Brown: Automatic Detection of Internal and Imperfect Rhymes in Rap Lyrics

Type: academic system, not found as open source.

Why it matters:

- It is specifically about rap, not poetry in general.
- It targets imperfect and internal rhymes.
- It treats rhyme as phonetic sequence alignment, not spelling.
- It accounts for slang and hip-hop vernacular.

Mechanics found:

- Lyrics are converted into syllable sequences.
- CMUdict is augmented with slang and hip-hop vernacular, including endings like "-in" and "-a".
- Unknown words use text-to-phoneme rules.
- Each syllable is reduced to relevant rhyme features: vowel nucleus, coda, and stress.
- A log-odds scoring matrix is trained from known rap rhymes.
- Vowels, consonants, unmatched consonants, and stress each contribute to a combined syllable score.
- The detector uses a local-alignment-like method to find the closest preceding rhyming syllable and phrase in current or previous lines.
- The paper reports that the probabilistic method significantly outperforms simpler rules-based approaches.

Important limitations:

- It is a detector/analyzer, not a suggestion dictionary.
- It needs training data to build scoring matrices.
- Some "false positives" were actually real rhymes that human annotations missed or ignored, which shows the evaluation target is partly subjective.
- Reported sensitivity/specificity on a manually annotated test set is only just above 60 percent at the best overall point. That is useful for analysis, but raw detection cannot be treated as a perfect oracle.

What to steal:

- Trainable vowel/coda/stress score matrices.
- Syllable-level local alignment.
- Forward/backward extension around stressed anchors.
- Specific handling for polysyllabic and mosaic rhymes.
- Pronunciation augmentation for rap spelling.

Sources:

- [Automatic Detection of Internal and Imperfect Rhymes in Rap Lyrics, ISMIR 2009](https://ismir2009.ismir.net/proceedings/OS8-1.pdf)

### 2. Rapsody

Type: open source rap rhyme and flow engine.

Code inspected:

- Temp clone: `Rapsody/backend/analyzer.py`
- Temp clone: `Rapsody/tests/validate_rhymes.py`
- Temp clone: `Rapsody/tests/mcflow_syl.py`
- Temp clone: `Rapsody/backend/requirements.txt`

Mechanics found:

- Uses `pronouncing` and CMUdict for known words.
- Uses `g2p_en` lazily for OOV terms.
- Adds custom rap spellings such as "imma", "finna", "tryna", "gimme", "lemme", "homie", "shorty", "thang".
- Converts words to syllables with vowel class, coda class, stress, and nucleus index.
- Groups consonants into near-rhyme coda classes: p/b, t/d, k/g, f/v, s/z, th/dh, sh/zh, ch/jh, nasals, etc.
- Treats fricative classes as compatible for slants.
- Handles r-coloring so rhotic vowels stay distinct.
- Excludes some function words from anchoring single rhymes, while still allowing them inside multiword runs.
- Detects line-end multisyllable rhymes by matching trailing vowel sequences across nearby lines.
- Detects internal multisyllable runs anywhere in a line when the vowel or vowel+coda sequence matches.
- Detects single-syllable rhymes by stressed vowel + coda key.
- Handles common feminine line-end IY endings.
- Builds rhyme groups with union-find.
- Returns line rhyme scheme, per-line syllable/word stats, rhyme density, flow rate, and coverage.

Strengths:

- Best inspected OSS mechanics for hip-hop analysis.
- Built around "every stressed syllable", not only final words.
- Directly models multis, internal runs, coda near-rhymes, and flow stats.
- Self-reported validation comments say roughly 78 percent recall and about 95 percent of flagged links are real rhymes.

Weaknesses:

- It is a lyric analyzer, not a candidate suggestion engine.
- It is Python, not Android/Kotlin.
- Its G2P dependency and Python runtime are not suitable for direct phone integration.
- It colors whole words because exact phoneme-to-letter mapping was unreliable.

What to steal:

- `V_CLASS`, `CODA_CLASS`, fricative near class, r-coloring.
- Syllable model: vowel, coda, stress, index.
- Separate mechanisms for line-end multis, internal multis, and single stressed rhymes.
- Union-find grouping for in-note rhyme analysis.
- OOV tiering: custom slang -> CMUdict -> G2P -> spelling fallback.

Sources:

- [Rapsody GitHub README](https://github.com/KapilSareen/Rapsody)
- Source code was inspected from a temporary local clone during this research.

### 3. Auto Rap Highlighter

Type: open source rap rhyme highlighter / thesis project.

Code inspected:

- Temp clone: `auto-rap-highlighter/src/lyrics2groups.py`
- Temp clone: `auto-rap-highlighter/src/clustering.py`
- Temp clone: `auto-rap-highlighter/src/linkage.py`
- Temp clone: `auto-rap-highlighter/src/groups.py`

Mechanics found:

- Uses CMUdict and `cmusphinx/g2p-seq2seq` for unknown words.
- Uses an ARPAbet syllabifier.
- Converts ARPAbet to ALINE IPA-like representations.
- Removes onset for rhyme comparison, focusing on rime.
- Uses ALINE phonetic distance for syllable similarity.
- Uses group-average linkage to cluster similar syllables.
- Iterates clustering multiple times.
- Selects the best pronunciation variant by how well it links into live rhyme groups.
- Limits "live groups" by nearby line distance, which prevents distant accidental merges.

Strengths:

- The pronunciation selection mechanism is valuable. Rap often uses the pronunciation that fits the rhyme.
- ALINE-style feature distance is more nuanced than exact coda matching.
- Clustering syllables can find families without hard-coded exact keys.

Weaknesses:

- It is heavier than needed for per-keystroke mobile suggestions.
- It requires Python and external model assets.
- It appears oriented toward highlighting a lyric text, not generating candidate suggestions.
- The setup is research-grade, not app-grade.

What to steal:

- Pronunciation variant selection based on local rhyme context.
- Feature-distance scoring for syllable pairs.
- Bounded live-line window.
- Iterative refinement as an offline build step, not as a hot runtime path.

Sources:

- [auto-rap-highlighter GitHub](https://github.com/DanielLoney/auto-rap-highlighter)
- Source code was inspected from a temporary local clone during this research.

### 4. RHYME-CTRL

Type: open source web app for rap rhyme highlighting and synchronized videos.

Code inspected:

- Temp clone: `RHYME-CTRL/rhyme_core.py`
- Temp clone: `RHYME-CTRL/app.py`
- Temp clone: `RHYME-CTRL/auto_align.py`

Mechanics found:

- Uses CMUdict through `pronouncing`.
- Extracts rhyme tail from the last stressed vowel to the end.
- Computes rhyme similarity in [0, 1].
- Weights stressed vowel match heavily, then tail suffix similarity, then a head-rhyme pattern for multi-syllable-ish matches.
- Greedily groups words into rhyme families.
- Adds locality penalty so nearby rhymes are favored.
- Filters groups smaller than a minimum size.
- Uses Whisper for audio alignment and word timestamps.

Strengths:

- Simple enough to port.
- Good UI-facing data model: token, phones, rhyme tail, surface split, head key, group.
- Locality penalty is useful for in-note rhyme family analysis.

Weaknesses:

- OOV handling is limited.
- Greedy grouping can lock early to weak prototypes.
- It does not produce a rich candidate dictionary.
- Its scoring is simpler than Rapsody or Hirjee/Brown.

What to steal:

- Token-level output shape for future in-note highlighting.
- Locality-aware grouping.
- Surface split fields for UI display if The Top Flow later highlights rhyme tails.

Sources:

- [RHYME-CTRL GitHub README](https://github.com/MunamWasi/RHYME-CTRL)
- Source code was inspected from a temporary local clone during this research.

### 5. multisyllabic-rhymer

Type: open source CLI rhyme finder.

Code inspected:

- Temp clone: `multisyllabic-rhymer/multisyllabic_rhymer.py`

Mechanics found:

- Uses `pronouncing`.
- Gets CMU phones for a word.
- Builds a rhyming tail from the first vowel onward, not only the final stressed vowel.
- Perfect rhyme search requires the tail at the end of the candidate.
- Internal rhyme search allows that tail anywhere in the candidate phones.
- Slant search replaces intermediate consonants with regex wildcards while preserving vowels and final consonants.
- Displays perfect, internal, and slant as mutually exclusive sets.

Strengths:

- Very easy to understand.
- Directly expands candidate pools for multisyllabic, internal, and slant rhymes.
- The wildcard idea is useful for offline candidate generation.

Weaknesses:

- It returns dictionary regex matches without modern ranking.
- It only uses the first CMU pronunciation.
- It has no slang/OOV path.
- It does not scale as a runtime algorithm for large mobile lexicons unless precomputed.

What to steal:

- Use regex/wildcard tail expansion offline to build slant candidate pools.
- Keep mutually exclusive categories so UI buckets are clear.

Sources:

- [multisyllabic-rhymer GitHub](https://github.com/Tareq62/multisyllabic-rhymer)
- Source code was inspected from a temporary local clone during this research.

### 6. pronouncing.py and CMUdict

Type: open source baseline pronunciation/rhyme library and dictionary.

Code inspected:

- Temp clone: `pronouncingpy/pronouncing/__init__.py`

Mechanics found:

- Parses CMUdict into word -> phones.
- Builds a rhyme lookup keyed by `rhyming_part`.
- `rhyming_part` returns phones from the last stressed vowel nearest the end to the end.
- `rhymes(word)` merges results for all pronunciations.
- `search(pattern)` supports regex over phone strings.
- Supports stress pattern search and syllable counting.

Strengths:

- Correct baseline for exact rhymes.
- Small, simple, proven.
- CMUdict is already in The Top Flow.

Weaknesses:

- Not hip-hop-specific.
- No near rhyme scoring.
- No OOV beyond dictionary.
- No frequency/context ranking.
- No internal/multisyllabic phrase generation unless built on top.

What to steal:

- Last-stressed-vowel exact key remains the default exact-rhyme key.
- Regex-style phone search belongs in the offline builder.

Sources:

- [pronouncing documentation](https://pronouncing.readthedocs.io/en/latest/)
- [pronouncingpy GitHub](https://github.com/aparrish/pronouncingpy)
- [CMUdict GitHub](https://github.com/cmusphinx/cmudict)

### 7. DeepRapper

Type: research rap generation model, open code in Microsoft Muzic.

Mechanics found:

- Transformer-based rap generation system.
- Models rhyme and rhythm together.
- Generates lyrics right-to-left so the end tokens that need to rhyme are generated first.
- Adds rhyme-related representations and an inference-time rhyme constraint.
- Inserts a beat token in lyric sequence for rhythm/beat modeling.
- Targets N-gram rhyme, because rap often rhymes multiple consecutive tokens at line endings.

Strengths:

- Useful proof that N-gram rhyme and rhythm should be modeled explicitly.
- Reverse-order generation is a strong idea for phrase suggestion, because phrase endings determine rhyme.

Weaknesses:

- It is a generative model, not a local rhyme dictionary.
- It is not an Android-ready suggestion engine.
- The paper's examples and pipeline are not directly aligned to The Top Flow's English note-taking use case.

What to steal:

- Phrase endings should be indexed first.
- For phrase suggestions, generate or retrieve by final 1-6 syllables before semantic/context ranking.
- Cadence and beat alignment are separate layers from rhyme quality.

Sources:

- [DeepRapper README in Microsoft Muzic](https://github.com/microsoft/muzic/blob/main/deeprapper/README.md)
- [DeepRapper paper](https://arxiv.org/pdf/2107.01875)

### 8. Datamuse and RhymeZone

Type: closed/API-backed lexical search and rhyming products.

Mechanics found:

- Datamuse is a word-finding API with constraints for meaning, spelling, sound, vocabulary, context, and lexical relationships.
- It exposes perfect rhymes, approximate rhymes, homophones, consonant match, syllable count, pronunciation, part of speech, frequency, topics, left context, and right context.
- Its default English vocabulary is large and includes multiword expressions.
- Datamuse says it uses CMUdict for phonetic data and Google Books Ngrams for corpus ranking.
- RhymeZone's advanced search for songwriters organizes "millions more words and phrases" into a sortable/filterable list by rhyme quality, popularity, meter, and other attributes.
- Datamuse says RhymeZone uses the API extensively for autocomplete, similar-sounding fallback, spelling correction, descriptive words, and related-word tabs.

Strengths:

- Best product benchmark for breadth, frequency ranking, phrase support, metadata, and context hints.
- The API shape maps well to what The Top Flow should support locally.

Weaknesses:

- Closed engine.
- Network API is incompatible with Dave's local-phone requirement.
- Datamuse will require an API key starting 2027-01-01 and has request limits.
- It cannot be embedded as a local engine.

What to steal:

- Candidate metadata: pronunciation, syllables, frequency, POS, tags.
- Query options: perfect, near, homophone, consonant, sounds-like, context/topic.
- Ranking recipe: rhyme quality first, then usage/frequency/context.
- Phrase vocabulary support.

Sources:

- [Datamuse API](https://www.datamuse.com/api/)
- [Datamuse users page](https://www.datamuse.com/api/users.html)
- [Microsoft Datamuse connector docs](https://learn.microsoft.com/en-us/connectors/datamuseip/)
- [RhymeZone advanced search](https://www.rhymezone.com/adv/)

### 9. RhymeBrain

Type: closed/API rhyming dictionary.

Mechanics found:

- `getRhymes` returns word, RhymeRank score, flags, syllable estimate, and frequency.
- Scores >= 300 are treated as perfect rhymes.
- Scores between 0 and 300 are near rhymes.
- Same-score results are ordered by most matching sounds.
- `getWordInfo` exposes ARPAbet pronunciation, IPA, frequency, and flags.
- Flags include whether pronunciation is known with confidence.

Strengths:

- Simple scoring threshold model is easy to explain to users.
- Confidence flag is important for generated pronunciations.

Weaknesses:

- Closed source.
- Network API.
- No evidence of hip-hop-specific internal/multisyllabic support beyond standard word rhymes.

What to steal:

- Numeric score bands.
- Pronunciation confidence flags.
- Frequency field in candidate objects.

Sources:

- [RhymeBrain API](https://rhymebrain.com/api.html)

### 10. RapPad

Type: closed web writing suite for rap.

Mechanics found:

- RapPad advertises a built-in rhyming dictionary, syllable counter, thesaurus, line generator, and attached instrumental/recording workflow.
- Its rhyming dictionary is positioned as fast, mobile optimized, and part of a rapper/songwriter suite.
- Datamuse lists RapPad as using the Datamuse API for contextual word suggestions.

Strengths:

- Strong product benchmark for workflow: rhymes belong beside lyrics, playback, and recording.

Weaknesses:

- No open code.
- Mechanics beyond Datamuse usage are not inspectable.
- Cannot be integrated locally.

What to steal:

- Combined writer workflow: rhyme suggestions, syllable counts, thesaurus/context, audio in one interface.

Sources:

- [RapPad homepage](https://www.rappad.co/)
- [RapPad rhyming dictionary](https://www.rappad.co/rhyming-dictionary)
- [Datamuse users page](https://www.datamuse.com/api/users.html)

## Overall Ranking For The Top Flow

1. Rapsody mechanics: best open-source hip-hop analysis code.
2. Hirjee/Brown mechanics: best rap-specific scoring model.
3. Datamuse/RhymeZone product scope: best benchmark for vocabulary, metadata, and ranking.
4. Auto Rap Highlighter: best open-source phonetic-distance/clustering idea.
5. RhymeBrain: useful score-band/API object model.
6. multisyllabic-rhymer: useful offline candidate expansion trick.
7. pronouncing.py/CMUdict: baseline exact rhyme and stress foundation.
8. DeepRapper: useful for future phrase/cadence generation, not immediate suggestions.
9. RHYME-CTRL: useful UI/token grouping shape.
10. RapPad: useful workflow benchmark, not technical source.

## Core Conclusion

No inspected system can be dropped into The Top Flow and satisfy "entire system works locally on the phone and appears almost instantaneously." The right plan is to build a purpose-built Android engine that precomputes the expensive work offline, ships larger local assets, memory maps those assets, and keeps per-keystroke runtime to cheap key lookup plus small reranking.
