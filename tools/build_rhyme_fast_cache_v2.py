#!/usr/bin/env python3

import argparse
import pathlib
import struct
import zlib
import re


MAGIC = b"TFCACHE2"
VERSION = 2
MAX_SUGGESTIONS = 6
ROW_SIZE = 32
HEADER_SIZE = 32


def normalize_word(word: str) -> str:
    return re.sub(r"[^a-z']", "", word.lower())


def parse_text_cache(path: pathlib.Path):
    rows = []
    with path.open("r", encoding="utf-8") as text:
        header = text.readline().strip()
        if "strictness=Balanced" not in header:
            raise RuntimeError("missing strictness=Balanced in hot cache header")
        if "exactOnly=false" not in header:
            raise RuntimeError("missing exactOnly=false in hot cache header")
        if "includeSlang=true" not in header:
            raise RuntimeError("missing includeSlang=true in hot cache header")
        if "maxCandidates=360" not in header:
            raise RuntimeError("missing maxCandidates=360 in hot cache header")
        if "limit=6" not in header:
            raise RuntimeError("missing limit=6 in hot cache header")
        for line in text:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split("\t")
            if len(parts) < 2:
                continue
            word = normalize_word(parts[0])
            if not word:
                continue
            suggestions = [p.strip() for p in parts[1:] if p and p.strip()]
            suggestions = [s for s in suggestions[:MAX_SUGGESTIONS] if s]
            if len(suggestions) < 4:
                continue
            rows.append((word, suggestions))
    rows.sort(key=lambda item: item[0])
    return rows


def build_binary(rows, output: pathlib.Path, debug_tsv: pathlib.Path):
    row_count = len(rows)
    row_table_offset = HEADER_SIZE
    string_bytes = bytearray()
    rows_bytes = bytearray()

    def append_string(value: str) -> int:
        offset = HEADER_SIZE + (row_count * ROW_SIZE) + len(string_bytes)
        string_bytes.extend(value.encode("utf-8"))
        string_bytes.append(0)
        return offset

    row_offsets = []
    string_offsets = []

    for word, suggestions in rows:
        word_offset = append_string(word)
        suggestion_count = len(suggestions)
        row_offsets.append((word_offset, suggestion_count, [append_string(s) for s in suggestions]))
    for idx in range(row_count):
        word_offset, suggestion_count, suggestion_offsets = row_offsets[idx]
        row_bytes = bytearray(ROW_SIZE)
        struct.pack_into("<I", row_bytes, 0, word_offset)
        struct.pack_into("<H", row_bytes, 4, suggestion_count)
        struct.pack_into("<H", row_bytes, 6, 0)
        for i in range(MAX_SUGGESTIONS):
            offset_value = suggestion_offsets[i] if i < len(suggestion_offsets) else 0
            struct.pack_into("<I", row_bytes, 8 + (i * 4), offset_value)
        string_offsets.append(bytes(row_bytes))

    rows_bytes.extend(b"".join(string_offsets))

    body = bytearray(HEADER_SIZE + len(rows_bytes) + len(string_bytes))
    body[row_table_offset:row_table_offset + len(rows_bytes)] = rows_bytes
    body[row_table_offset + len(rows_bytes):] = string_bytes

    string_table_offset = row_table_offset + len(rows_bytes)
    struct.pack_into("<8s", body, 0, MAGIC)
    struct.pack_into("<H", body, 8, VERSION)
    struct.pack_into("<I", body, 10, row_count)
    struct.pack_into("<I", body, 14, row_table_offset)
    struct.pack_into("<I", body, 18, string_table_offset)
    struct.pack_into("<I", body, 26, 0)
    checksum = zlib.crc32(body[:22] + b"\x00\x00\x00\x00" + body[26:]) & 0xFFFFFFFF
    struct.pack_into("<I", body, 22, checksum)

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(body)

    debug_tsv.parent.mkdir(parents=True, exist_ok=True)
    with debug_tsv.open("w", encoding="utf-8", newline="") as out:
        out.write("word\tcount\t" + "\t".join(f"s{i + 1}" for i in range(MAX_SUGGESTIONS)) + "\n")
        for word, suggestions in rows:
            values = [word, str(len(suggestions))] + suggestions
            out.write("\t".join(values) + "\n")

    return row_count


def main():
    parser = argparse.ArgumentParser(description="Build binary rhyme fast-cache V2 asset.")
    parser.add_argument(
        "--source",
        default="app/src/main/assets/rhyme_hot_cache.tfcache",
        help="input text hot cache",
    )
    parser.add_argument(
        "--output",
        default="app/src/main/assets/rhyme_fast_cache_v2.tfcache",
        help="output binary fast cache asset",
    )
    parser.add_argument(
        "--debug-tsv",
        default="build/rhyme-v2-debug/fast_cache_debug.tsv",
        help="debug output path",
    )
    args = parser.parse_args()

    source = pathlib.Path(args.source)
    output = pathlib.Path(args.output)
    debug_tsv = pathlib.Path(args.debug_tsv)

    if not source.exists():
        raise SystemExit(f"source hot cache not found: {source}")

    rows = parse_text_cache(source)
    if not rows:
        raise SystemExit("no rows parsed from source cache")
    count = build_binary(rows, output, debug_tsv)
    print(f"wrote {count} rows to {output}")


if __name__ == "__main__":
    main()
