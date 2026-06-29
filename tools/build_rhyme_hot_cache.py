#!/usr/bin/env python3
"""Build The Top Flow's shipped default rhyme hot caches.

The caches store real ranked rhyme suggestions for safe default paths:
Balanced strictness, exactOnly=false, includeSlang=true, no removed words, and
maxCandidates matching the runtime fast/expanded request. Fast rows store the
top six results; expanded rows store the top twelve.
"""

from pathlib import Path
import argparse
import os
import sys

import rhyme_quality_check as rq


ROOT = Path(__file__).resolve().parents[1]
INDEX_PATH = ROOT / "app" / "src" / "main" / "assets" / "rhyme_index.tsv"
FAST_OUT_PATH = ROOT / "app" / "src" / "main" / "assets" / "rhyme_hot_cache.tfcache"
EXPANDED_OUT_PATH = ROOT / "app" / "src" / "main" / "assets" / "rhyme_expanded_hot_cache.tfcache"

INDEX_WORD_LIMIT = 360
FAST_CACHE_LIMIT = 6
FAST_CACHE_MAX_CANDIDATES = 360
EXPANDED_CACHE_LIMIT = 12
EXPANDED_CACHE_MAX_CANDIDATES = 720
DEFAULT_FAST_TARGET_ROWS = 30000
DEFAULT_EXPANDED_TARGET_ROWS = 20000

WORD_FIRST_SEEN = {}
WORD_SYLLABLES = {}
_ORIGINAL_CMU_RHYME_INFOS = rq.cmu_rhyme_infos
_RHYME_INFO_CACHE = {}
_CANDIDATE_WORD_CACHE = {}
_COMMON_BIAS_CACHE = {}
_NEAR_SLANG_CACHE = {}
_SLANG_VARIANT_CACHE = {}
_COMMON_BIAS_BY_WORD = {}

for _index, _common in enumerate(rq.COMMON_RHYME_WORDS):
    _word = rq.normalize_word(_common)
    if _word and _word not in _COMMON_BIAS_BY_WORD:
        _COMMON_BIAS_BY_WORD[_word] = max(48, 124 - _index)


def cached_cmu_rhyme_infos(word):
    normalized = rq.normalize_word(word)
    cached = _RHYME_INFO_CACHE.get(normalized)
    if cached is not None:
        return cached
    infos = _ORIGINAL_CMU_RHYME_INFOS(normalized)
    _RHYME_INFO_CACHE[normalized] = infos
    return infos


rq.cmu_rhyme_infos = cached_cmu_rhyme_infos


def cached_candidate_rhyme_word(candidate):
    cached = _CANDIDATE_WORD_CACHE.get(candidate)
    if cached is not None:
        return cached
    parts = (candidate or "").lower().replace("\t", " ").split()
    out = ""
    for part in reversed(parts):
        word = rq.normalize_word(part)
        if word:
            out = word
            break
    if not out:
        out = rq.normalize_word(candidate)
    _CANDIDATE_WORD_CACHE[candidate] = out
    return out


def cached_common_rhyme_bias(candidate):
    cached = _COMMON_BIAS_CACHE.get(candidate)
    if cached is not None:
        return cached
    word = cached_candidate_rhyme_word(candidate)
    value = 101 if word == "about" else _COMMON_BIAS_BY_WORD.get(word, 0)
    _COMMON_BIAS_CACHE[candidate] = value
    return value


def cached_near_slang_family(a, b):
    key = (rq.normalize_word(a), rq.normalize_word(b))
    cached = _NEAR_SLANG_CACHE.get(key)
    if cached is not None:
        return cached
    x = rq.hip_hop_near_tail(key[0])
    y = rq.hip_hop_near_tail(key[1])
    value = bool(x and x == y)
    _NEAR_SLANG_CACHE[key] = value
    return value


def cached_slang_variant_pair(a, b):
    key = (rq.normalize_word(a), rq.normalize_word(b))
    cached = _SLANG_VARIANT_CACHE.get(key)
    if cached is not None:
        return cached
    x = rq.slang_variant_key(key[0])
    y = rq.slang_variant_key(key[1])
    value = bool(x and x == y and key[0] != key[1])
    _SLANG_VARIANT_CACHE[key] = value
    return value


rq.candidate_rhyme_word = cached_candidate_rhyme_word
rq.common_rhyme_bias = cached_common_rhyme_bias
rq.near_slang_family = cached_near_slang_family
rq.slang_variant_pair = cached_slang_variant_pair


def add_index(index, key, word):
    if not key:
        return
    words = index.setdefault(key, [])
    if len(words) < INDEX_WORD_LIMIT and word not in words:
        words.append(word)


def add_phones(word, phones, dictionary=True):
    values = rq.cmu_phones.setdefault(word, [])
    if phones not in values:
        values.append(phones)
    if dictionary:
        rq.cmu_dictionary_words.add(word)


def add_entry_from_phones(word, phones, dictionary=True):
    add_phones(word, phones, dictionary)
    rhyme = rq.cmu_rhyme_part_from_phones(phones)
    if rhyme:
        add_index(rq.cmu_rhyme_index, rq.rhyme_key_from_phones(rhyme), word)
    info = rq.phone_rhyme_info(phones)
    if info:
        add_index(rq.cmu_family_index, info["family_key"], word)


def load_prepared_index():
    rq.cmu_phones.clear()
    rq.cmu_rhyme_index.clear()
    rq.cmu_family_index.clear()
    rq.cmu_dictionary_words.clear()
    WORD_FIRST_SEEN.clear()
    WORD_SYLLABLES.clear()
    _RHYME_INFO_CACHE.clear()
    _CANDIDATE_WORD_CACHE.clear()
    _COMMON_BIAS_CACHE.clear()
    _NEAR_SLANG_CACHE.clear()
    _SLANG_VARIANT_CACHE.clear()

    with INDEX_PATH.open("r", encoding="utf-8") as handle:
        for index, line in enumerate(handle):
            if not line or line.startswith("#"):
                continue
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 7:
                continue
            word = rq.normalize_word(parts[0])
            phones = parts[1].strip()
            rhyme_key = parts[2].strip()
            family_key = parts[5].strip()
            if not word or not phones:
                continue
            if word not in WORD_FIRST_SEEN:
                WORD_FIRST_SEEN[word] = index
            try:
                syllables = int(parts[6])
            except ValueError:
                syllables = 4
            WORD_SYLLABLES[word] = min(WORD_SYLLABLES.get(word, syllables), syllables)
            add_phones(word, phones, True)
            add_index(rq.cmu_rhyme_index, rhyme_key, word)
            add_index(rq.cmu_family_index, family_key, word)

    add_entry_from_phones("your", "Y AO1 R", True)
    add_entry_from_phones("yours", "Y AO1 R Z", True)
    for word in rq.COMMON_RHYME_WORDS:
        normalized = rq.normalize_word(word)
        phones = rq.slang_phones(normalized)
        if normalized and phones and normalized not in rq.cmu_dictionary_words:
            add_entry_from_phones(normalized, phones, True)


def candidate_pool_for(base, base_infos):
    pool = {}
    if not base_infos:
        return pool
    for info in base_infos:
        for word in rq.cmu_rhyme_index.get(info["rhyme_key"], []):
            rq.add_candidate(pool, word, rq.BUCKET_EXACT)
        for word in rq.cmu_family_index.get(info["family_key"], []):
            rq.add_candidate(pool, word, rq.BUCKET_NEAR)
    for word in rq.COMMON_RHYME_WORDS:
        normalized = rq.normalize_word(word)
        if not normalized or normalized == base:
            continue
        relation = rq.cmu_relation(base_infos, rq.cmu_rhyme_infos(normalized))
        if relation >= 0:
            rq.add_candidate(pool, word, relation)
        elif rq.near_slang_family(base, normalized):
            rq.add_candidate(pool, word, rq.BUCKET_SLANT)
    for phrase in rq.COMMON_RHYME_PHRASES:
        rhyme_word = rq.candidate_rhyme_word(phrase)
        if not rhyme_word or rhyme_word == base:
            continue
        relation = rq.cmu_relation(base_infos, rq.cmu_rhyme_infos(rhyme_word))
        if relation in (rq.BUCKET_EXACT, rq.BUCKET_NEAR) or rq.near_slang_family(base, rhyme_word):
            rq.add_candidate(pool, phrase, rq.BUCKET_PHRASE)
    return pool


def score_default(base, candidate, bucket, base_infos):
    rhyme_word = rq.candidate_rhyme_word(candidate)
    if not rhyme_word or rhyme_word == base:
        return 0
    candidate_infos = rq.cmu_rhyme_infos(rhyme_word)
    if base_infos and candidate_infos:
        value = rq.best_cmu_rhyme_score(base, rhyme_word, base_infos, candidate_infos, bucket)
        return value if value >= 112 else 0
    if base_infos or candidate_infos:
        if not rq.near_slang_family(base, rhyme_word):
            return 0
        value = 116 - rq.bucket_penalty(bucket)
        return value if value >= 112 else 0
    return 0


def suggest_default(base, limit, max_candidates):
    base = rq.normalize_word(base)
    if not base:
        return []
    base_infos = rq.cmu_rhyme_infos(base)
    candidates = [
        (word, bucket)
        for word, bucket in candidate_pool_for(base, base_infos).items()
    ]
    candidates.sort(key=lambda item: (item[1], -rq.common_rhyme_bias(item[0]), item[0]))

    matches = []
    scored = 0
    for candidate, bucket in candidates:
        rhyme_word = rq.candidate_rhyme_word(candidate)
        if rhyme_word == base:
            continue
        if scored >= max_candidates:
            break
        scored += 1
        value = score_default(base, candidate, bucket, base_infos)
        if value > 0:
            matches.append((candidate, value, bucket, rq.common_rhyme_bias(candidate)))

    matches.sort(key=lambda item: (-item[1], item[2], -item[3], item[0]))
    out = []
    for word, _, _, _ in matches:
        if word not in out:
            out.append(word)
        if len(out) >= limit:
            break
    return out


def base_word_rank(word, first_seen, common_rank):
    syllables = WORD_SYLLABLES.get(word, 4)
    common = common_rank.get(word, 999999)
    clean_bonus = 0 if "'" not in word else 1
    length_penalty = max(0, len(word) - 8)
    return (common, clean_bonus, syllables, length_penalty, len(word), first_seen.get(word, 999999), word)


def candidate_base_words():
    common_rank = {}
    ordered = []
    for item in rq.COMMON_RHYME_WORDS:
        word = rq.normalize_word(item)
        if word and word not in common_rank:
            common_rank[word] = len(common_rank)
            ordered.append(word)
    for phrase in rq.COMMON_RHYME_PHRASES:
        word = rq.candidate_rhyme_word(phrase)
        if word and word not in common_rank:
            common_rank[word] = len(common_rank)
            ordered.append(word)

    for word in rq.cmu_phones.keys():
        if word not in common_rank:
            if len(word) < 2 or len(word) > 14:
                continue
            if not word.replace("'", "").isalpha():
                continue
            ordered.append(word)

    seen = set()
    unique = []
    for word in ordered:
        if word in seen:
            continue
        seen.add(word)
        unique.append(word)
    unique.sort(key=lambda word: base_word_rank(word, WORD_FIRST_SEEN, common_rank))
    return unique


def build_cache(target_rows, limit, max_candidates):
    rows = []
    for word in candidate_base_words():
        suggestions = suggest_default(word, limit, max_candidates)
        if len(suggestions) < 4:
            continue
        rows.append((word, suggestions))
        if len(rows) >= target_rows:
            break
    return rows


def write_cache(rows, output_path, header, limit, max_candidates):
    header = (
        f"# {header}\t"
        "strictness=Balanced\texactOnly=false\tincludeSlang=true\t"
        f"maxCandidates={max_candidates}\tlimit={limit}\n"
    )
    with output_path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(header)
        for word, suggestions in rows:
            handle.write(word)
            for suggestion in suggestions[:limit]:
                handle.write("\t")
                handle.write(suggestion)
            handle.write("\n")


def build_and_write(label, output_path, target_rows, limit, max_candidates, header):
    rows = build_cache(max(1, target_rows), limit, max_candidates)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    write_cache(rows, output_path, header, limit, max_candidates)
    size = os.path.getsize(output_path)
    examples = {word: suggest_default(word, limit, max_candidates) for word in ["my", "out", "yours", "moving", "running"]}
    print(f"wrote {label} {output_path}")
    print(f"{label} rows={len(rows)} bytes={size}")
    for word, suggestions in examples.items():
        print(f"{label} {word}: {suggestions}")


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=["fast", "expanded", "both"], default="both")
    parser.add_argument("--target-rows", type=int, default=DEFAULT_FAST_TARGET_ROWS)
    parser.add_argument("--expanded-target-rows", type=int, default=DEFAULT_EXPANDED_TARGET_ROWS)
    parser.add_argument("--output", type=Path, default=FAST_OUT_PATH)
    parser.add_argument("--expanded-output", type=Path, default=EXPANDED_OUT_PATH)
    args = parser.parse_args(argv)

    load_prepared_index()
    if args.mode in ("fast", "both"):
        build_and_write(
            "fast",
            args.output,
            args.target_rows,
            FAST_CACHE_LIMIT,
            FAST_CACHE_MAX_CANDIDATES,
            "topflow-rhyme-hot-cache-v1",
        )
    if args.mode in ("expanded", "both"):
        build_and_write(
            "expanded",
            args.expanded_output,
            args.expanded_target_rows,
            EXPANDED_CACHE_LIMIT,
            EXPANDED_CACHE_MAX_CANDIDATES,
            "topflow-rhyme-expanded-hot-cache-v1",
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
