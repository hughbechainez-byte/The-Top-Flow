#!/usr/bin/env python3

import argparse
import pathlib
import struct
import zlib

REQUIRED = ["my", "try", "time", "out", "running", "moving", "runnin", "movin"]
MAX_SUGGESTIONS = 6


def read_string(data: bytes, start: int) -> str:
    end = start
    while end < len(data) and data[end] != 0:
        end += 1
    return data[start:end].decode("utf-8")


def parse_binary(path: pathlib.Path):
    data = path.read_bytes()
    if len(data) < 32:
        raise RuntimeError("cache too small")
    if data[:8] != b"TFCACHE2":
        raise RuntimeError("bad magic")
    version = struct.unpack_from("<H", data, 8)[0]
    if version != 2:
        raise RuntimeError(f"unsupported version {version}")
    row_count = struct.unpack_from("<I", data, 10)[0]
    row_table_offset = struct.unpack_from("<I", data, 14)[0]
    string_table_offset = struct.unpack_from("<I", data, 18)[0]
    checksum = struct.unpack_from("<I", data, 22)[0]
    expected = zlib.crc32(data[:22] + b"\x00\x00\x00\x00" + data[26:]) & 0xFFFFFFFF
    if checksum != expected:
        raise RuntimeError(f"checksum mismatch expected={expected} got={checksum}")
    if row_count <= 0:
        raise RuntimeError("no rows")
    if row_table_offset < 32:
        raise RuntimeError("invalid row table offset")
    if string_table_offset <= row_table_offset:
        raise RuntimeError("invalid string table offset")
    if row_table_offset + (row_count * 32) > string_table_offset:
        raise RuntimeError("row table exceeds string table")
    if string_table_offset >= len(data):
        raise RuntimeError("invalid string table offset")

    rows = {}
    for index in range(row_count):
        row_offset = row_table_offset + (index * 32)
        if row_offset + 32 > string_table_offset:
            raise RuntimeError(f"row {index} exceeds row table")
        word_offset = struct.unpack_from("<I", data, row_offset)[0]
        suggestion_count = struct.unpack_from("<H", data, row_offset + 4)[0]
        if suggestion_count < 4:
            raise RuntimeError(f"row {index} has low suggestion_count {suggestion_count}")
        if word_offset < string_table_offset or word_offset >= len(data):
            raise RuntimeError(f"row {index} word offset out of range")
        word = read_string(data, word_offset)
        if not word:
            raise RuntimeError(f"row {index} has empty word")
        suggestions = []
        for i in range(MAX_SUGGESTIONS):
            suggestion_offset = struct.unpack_from("<I", data, row_offset + 8 + (i * 4))[0]
            if suggestion_count == i:
                break
            if suggestion_offset < string_table_offset or suggestion_offset >= len(data):
                raise RuntimeError(f"row {index} suggestion offset out of range")
            suggestion = read_string(data, suggestion_offset)
            if suggestion:
                suggestions.append(suggestion)
            else:
                raise RuntimeError(f"row {index} empty suggestion at slot {i}")
        rows[word] = suggestions[:suggestion_count]
    return rows


def main():
    parser = argparse.ArgumentParser(description="Validate binary rhyme fast-cache V2.")
    parser.add_argument(
        "--asset",
        default="app/src/main/assets/rhyme_fast_cache_v2.tfcache",
        help="binary asset path",
    )
    args = parser.parse_args()
    path = pathlib.Path(args.asset)
    if not path.exists():
        raise SystemExit(f"missing binary asset: {path}")
    rows = parse_binary(path)
    for word in REQUIRED:
        value = rows.get(word)
        if not value:
            raise SystemExit(f"missing required word: {word}")
        if len(value) < 4:
            raise SystemExit(f"word {word} has too few suggestions: {len(value)}")
    print(f"rhyme_fast_cache_v2 valid with {len(rows)} rows")


if __name__ == "__main__":
    main()
