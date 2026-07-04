#!/usr/bin/env python3

import argparse
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


def normalize_word(word: str) -> str:
    return re.sub(r"[^a-z']", "", (word or "").lower()).strip("'")


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


def merged_rows(default_source: pathlib.Path, expanded_source: pathlib.Path):
    rows = parse_hot_cache(default_source)
    if expanded_source.exists():
        expanded = parse_hot_cache(expanded_source)
        for word, candidates in expanded.items():
            if word in rows and len(candidates) > len(rows[word]):
                rows[word] = candidates
    apply_hip_hop_alias_rows(rows)
    return sorted(rows.items(), key=lambda item: item[0])


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
    args = parser.parse_args()
    rows = merged_rows(pathlib.Path(args.source), pathlib.Path(args.expanded_source))
    if len(rows) < 30_000:
        raise SystemExit(f"too few candidate rows: {len(rows)}")
    count = build_binary(rows, pathlib.Path(args.output), pathlib.Path(args.debug_tsv))
    print(f"wrote {count} V2 candidate rows to {args.output}")


if __name__ == "__main__":
    main()
