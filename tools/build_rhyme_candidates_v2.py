#!/usr/bin/env python3

import argparse
import json
import pathlib
import re
import struct
import zlib

MAGIC = b"TFCAND2"
VERSION = 2
HEADER_SIZE = 32
ROW_SIZE = 56
MAX_CANDIDATES = 12

HIP_HOP_ALIAS_ROWS = {
    "cuz": "cause",
    "cos": "cause",
    "coz": "cause",
    "flexing": "flexin",
    "grinding": "grindin",
    "luv": "love",
    "rapping": "rappin",
    "shining": "shinin",
    "shorty": "shawty",
    "spitting": "spittin",
    "stacking": "stackin",
    "wanting": "wanna",
}

PREFERRED_HIP_HOP_ALIAS_ROWS = {
    "flexing",
    "grinding",
    "rapping",
    "shining",
    "shorty",
    "spitting",
    "stacking",
}

# High-frequency function words can be phonetically valid while making poor
# writing prompts (for example, love -> of). Keep useful pronouns and verbs
# out of this set; block only weak connective words.
LOW_VALUE_FUNCTION_WORDS = {
    "a", "an", "and", "as", "at", "by", "for", "from", "in", "into",
    "of", "on", "or", "the", "to", "with", "au", "aux", "beau", "beaux",
    "ar", "are", "bleau", "gov",
}

HIP_HOP_PHRASE_ENDING_ROWS = {
    "action": ["back in", "stackin", "captain", "traction", "fashion", "passion", "latin", "mashin", "reaction", "satisfaction", "distraction", "everlasting"],
    "bars": ["stars", "cars", "mars", "scars", "barz", "guitars", "jars", "ours", "powers", "flowers", "towers", "hours"],
    "battery": ["battle me", "strategy", "actually", "casually", "happily", "tragedy", "faculty", "majesty", "blasphemy", "anatomy", "lavishly", "naturally"],
    "beat": ["heat", "street", "feet", "seat", "sweet", "meet", "feat", "repeat", "elite", "concrete", "heartbeat", "backbeat"],
    "cigar": ["disregard", "boulevard", "guitar", "superstar", "avatar", "fallen star", "racing car", "shooting star", "work of art", "from the heart", "play your part", "raise the bar"],
    "cap": ["rap", "trap", "snap", "clap", "strap", "map", "lap", "gap", "no cap", "backpack", "black cap", "nightcap"],
    "inside": ["outside", "ride", "slide", "wide", "side", "hide", "pride", "beside", "collide", "divide", "vibe", "alive"],
    "lit": ["hit", "sit", "bit", "fit", "quit", "split", "legit", "slit", "kit", "wit", "grit", "shit"],
    "me": ["free", "see", "be", "key", "three", "tree", "agree", "on me", "for me", "with me", "homie", "lowkey"],
    "money": ["on me", "honey", "sunny", "funny", "run it", "from me", "come see", "one deep", "trust me", "love me", "nothing", "company"],
    "outside": ["inside", "ride", "slide", "wide", "side", "hide", "pride", "beside", "collide", "divide", "vibe", "alive"],
    "real": ["feel", "deal", "steel", "wheel", "appeal", "conceal", "reveal", "heal", "for real", "keep it real", "surreal", "peel"],
    "tonight": ["night", "light", "right", "fight", "write", "bright", "sight", "alright", "all night", "tonite", "spotlight", "moonlight"],
}


def normalize_word(word: str) -> str:
    return re.sub(r"[^a-z']", "", (word or "").lower()).strip("'")


def candidate_tail(candidate: str) -> str:
    return normalize_word((candidate or "").rsplit(" ", 1)[-1])


CURATED_CANDIDATES = {
    candidate_tail(value)
    for values in HIP_HOP_PHRASE_ENDING_ROWS.values()
    for value in values
}
CURATED_CANDIDATES.update(HIP_HOP_ALIAS_ROWS)
CURATED_CANDIDATES.update(HIP_HOP_ALIAS_ROWS.values())
CURATED_CANDIDATES.update({
    "runnin", "movin", "pullin", "tryna", "finna", "imma", "ima", "gimme",
    "lemme", "nah", "naw", "thang", "homie", "shorty",
})


def load_frequency_ranks(path: pathlib.Path) -> dict[str, int]:
    with path.open("r", encoding="utf-8") as handle:
        rows = json.load(handle)
    ranks = {}
    for rank, row in enumerate(rows):
        if not isinstance(row, list) or not row:
            continue
        word = normalize_word(str(row[0]))
        if word and word not in ranks:
            ranks[word] = rank
    if len(ranks) < 20_000:
        raise RuntimeError("frequency source contains too few English words")
    return ranks


def candidate_quality(candidate: str, frequency_ranks: dict[str, int]) -> float | None:
    tail = candidate_tail(candidate)
    if not tail or tail in LOW_VALUE_FUNCTION_WORDS:
        return None
    if tail in CURATED_CANDIDATES:
        return float(len(frequency_ranks) + 1)
    rank = frequency_ranks.get(tail)
    return None if rank is None else float(len(frequency_ranks) - rank)


def rank_candidates(base: str, candidates: list[str], frequency_ranks: dict[str, int]) -> list[str]:
    ranked = []
    seen = {normalize_word(base)}
    for index, candidate in enumerate(candidates):
        tail = candidate_tail(candidate)
        quality = candidate_quality(candidate, frequency_ranks)
        candidate_key = normalize_word(candidate)
        if not tail or not candidate_key or candidate_key in seen or quality is None:
            continue
        seen.add(candidate_key)
        ranked.append((candidate, quality, index))
    # The fast-cache order already reflects the app's rhyme scorer. Frequency
    # is a quality gate, not a replacement for phonetic/rap-specific ranking.
    ranked.sort(key=lambda item: (item[2], item[0]))
    return [candidate for candidate, _, _ in ranked[:MAX_CANDIDATES]]


def parse_hot_cache(path: pathlib.Path):
    rows = {}
    with path.open("r", encoding="utf-8") as handle:
        header = handle.readline()
        if "strictness=Balanced" not in header or "includeSlang=true" not in header:
            raise RuntimeError("source cache is not the default balanced slang cache")
        for line in handle:
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 5:
                continue
            word = normalize_word(parts[0])
            candidates = [item.strip() for item in parts[1:1 + MAX_CANDIDATES] if item.strip()]
            if word and len(candidates) >= 4:
                rows[word] = candidates
    return rows


def merged_rows(default_source: pathlib.Path, expanded_source: pathlib.Path, frequency_ranks: dict[str, int]):
    rows = parse_hot_cache(default_source)
    if expanded_source.exists():
        expanded = parse_hot_cache(expanded_source)
        for word, candidates in expanded.items():
            if word in rows and len(candidates) > len(rows[word]):
                rows[word] = candidates
    apply_hip_hop_alias_rows(rows)
    apply_hip_hop_phrase_rows(rows)
    quality_rows = []
    for word, candidates in rows.items():
        ranked = rank_candidates(word, candidates, frequency_ranks)
        if ranked:
            quality_rows.append((word, ranked))
    return sorted(quality_rows, key=lambda item: item[0])


def apply_hip_hop_alias_rows(rows: dict[str, list[str]]) -> None:
    for alias, canonical in HIP_HOP_ALIAS_ROWS.items():
        if canonical not in rows:
            continue
        if alias in rows and alias not in PREFERRED_HIP_HOP_ALIAS_ROWS:
            continue
        suggestions = []
        seen = {alias}
        if canonical not in seen:
            suggestions.append(canonical)
            seen.add(canonical)
        for candidate in rows[canonical]:
            candidate_key = normalize_word(candidate)
            if not candidate_key or candidate_key in seen:
                continue
            suggestions.append(candidate)
            seen.add(candidate_key)
            if len(suggestions) >= MAX_CANDIDATES:
                break
        if len(suggestions) >= 4:
            rows[alias] = suggestions


def apply_hip_hop_phrase_rows(rows: dict[str, list[str]]) -> None:
    for word, candidates in HIP_HOP_PHRASE_ENDING_ROWS.items():
        suggestions = []
        seen = {word}
        for candidate in candidates:
            candidate_key = normalize_word(candidate)
            if not candidate_key or candidate_key in seen:
                continue
            suggestions.append(candidate)
            seen.add(candidate_key)
            if len(suggestions) >= MAX_CANDIDATES:
                break
        if len(suggestions) >= 4:
            rows[word] = suggestions


def build_binary(rows, output: pathlib.Path, debug_tsv: pathlib.Path) -> int:
    row_count = len(rows)
    row_table_offset = HEADER_SIZE
    string_table = bytearray()
    row_table = bytearray()

    def append_string(value: str) -> int:
        offset = HEADER_SIZE + (row_count * ROW_SIZE) + len(string_table)
        string_table.extend(value.encode("utf-8"))
        string_table.append(0)
        return offset

    encoded_rows = []
    for word, candidates in rows:
        word_offset = append_string(word)
        candidate_offsets = [append_string(candidate) for candidate in candidates[:MAX_CANDIDATES]]
        encoded_rows.append((word_offset, len(candidate_offsets), candidate_offsets))

    for word_offset, candidate_count, candidate_offsets in encoded_rows:
        row = bytearray(ROW_SIZE)
        struct.pack_into("<I", row, 0, word_offset)
        struct.pack_into("<H", row, 4, candidate_count)
        struct.pack_into("<H", row, 6, 0)
        for index in range(MAX_CANDIDATES):
            offset = candidate_offsets[index] if index < len(candidate_offsets) else 0
            struct.pack_into("<I", row, 8 + (index * 4), offset)
        row_table.extend(row)

    body = bytearray(HEADER_SIZE + len(row_table) + len(string_table))
    body[row_table_offset:row_table_offset + len(row_table)] = row_table
    string_table_offset = row_table_offset + len(row_table)
    body[string_table_offset:] = string_table
    struct.pack_into("<8s", body, 0, MAGIC)
    struct.pack_into("<H", body, 8, VERSION)
    struct.pack_into("<I", body, 10, row_count)
    struct.pack_into("<I", body, 14, row_table_offset)
    struct.pack_into("<I", body, 18, string_table_offset)
    checksum = zlib.crc32(body[:22] + b"\x00\x00\x00\x00" + body[26:]) & 0xFFFFFFFF
    struct.pack_into("<I", body, 22, checksum)

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(body)
    debug_tsv.parent.mkdir(parents=True, exist_ok=True)
    with debug_tsv.open("w", encoding="utf-8", newline="") as debug:
        debug.write("word\tcount\t" + "\t".join(f"c{i + 1}" for i in range(MAX_CANDIDATES)) + "\n")
        for word, candidates in rows:
            debug.write("\t".join([word, str(len(candidates))] + candidates[:MAX_CANDIDATES]) + "\n")
    return row_count


def main() -> None:
    parser = argparse.ArgumentParser(description="Build V2 binary rhyme candidate table.")
    parser.add_argument("--source", default="app/src/main/assets/rhyme_hot_cache.tfcache")
    parser.add_argument("--expanded-source", default="app/src/main/assets/rhyme_expanded_hot_cache.tfcache")
    parser.add_argument("--output", default="app/src/main/assets/rhyme_candidates_v2.tfcand")
    parser.add_argument("--debug-tsv", default="build/rhyme-v2-debug/candidates_v2_debug.tsv")
    parser.add_argument("--frequency-json", default="build/rhyme-v2-debug/wordfreq-en-25000-log.json")
    args = parser.parse_args()
    frequency_ranks = load_frequency_ranks(pathlib.Path(args.frequency_json))
    rows = merged_rows(pathlib.Path(args.source), pathlib.Path(args.expanded_source), frequency_ranks)
    if len(rows) < 25_000:
        raise SystemExit(f"too few candidate rows: {len(rows)}")
    count = build_binary(rows, pathlib.Path(args.output), pathlib.Path(args.debug_tsv))
    print(f"wrote {count} V2 candidate rows to {args.output}")


if __name__ == "__main__":
    main()
