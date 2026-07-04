#!/usr/bin/env python3

import argparse
import pathlib
import struct
import zlib

MAGIC = b"TFCAND2\x00"
VERSION = 2
HEADER_SIZE = 32
ROW_SIZE = 56
MAX_CANDIDATES = 12
REQUIRED = ["time", "out", "running", "moving", "finna", "tryna", "spittin", "rappin", "thang"]
REQUIRED_WIDE = ["time", "out", "moving", "running", "my", "try", "yours", "hover", "cover"]
REQUIRED_HIP_HOP_ALIAS = {
    "spitting": "spittin",
    "rapping": "rappin",
    "stacking": "stackin",
    "flexing": "flexin",
    "grinding": "grindin",
    "shining": "shinin",
    "cuz": "cause",
    "luv": "love",
    "shorty": "shawty",
    "wanting": "wanna",
}
REQUIRED_PHRASE_ENDINGS = {
    "cap": ["rap", "trap", "no cap"],
    "real": ["feel", "for real", "keep it real"],
    "me": ["free", "on me", "homie"],
    "outside": ["inside", "ride", "slide"],
    "inside": ["outside", "ride", "slide"],
    "tonight": ["night", "all night", "moonlight"],
    "bars": ["stars", "barz", "guitars"],
}


def read_string(data: bytes, offset: int) -> str:
    end = offset
    while end < len(data) and data[end] != 0:
        end += 1
    return data[offset:end].decode("utf-8")


def parse(path: pathlib.Path):
    data = path.read_bytes()
    if len(data) < HEADER_SIZE:
        raise RuntimeError("candidate table too small")
    if data[:8] != MAGIC:
        raise RuntimeError("bad candidate magic")
    version = struct.unpack_from("<H", data, 8)[0]
    if version != VERSION:
        raise RuntimeError(f"unsupported candidate version {version}")
    row_count = struct.unpack_from("<I", data, 10)[0]
    row_table_offset = struct.unpack_from("<I", data, 14)[0]
    string_table_offset = struct.unpack_from("<I", data, 18)[0]
    checksum = struct.unpack_from("<I", data, 22)[0]
    expected = zlib.crc32(data[:22] + b"\x00\x00\x00\x00" + data[26:]) & 0xFFFFFFFF
    if checksum != expected:
        raise RuntimeError("candidate checksum mismatch")
    if row_count < 30_000:
        raise RuntimeError(f"candidate row count too low: {row_count}")
    if row_table_offset != HEADER_SIZE:
        raise RuntimeError("unexpected row table offset")
    if row_table_offset + (row_count * ROW_SIZE) > string_table_offset:
        raise RuntimeError("row table exceeds string table")
    rows = {}
    previous_word = ""
    for index in range(row_count):
        offset = row_table_offset + (index * ROW_SIZE)
        word_offset = struct.unpack_from("<I", data, offset)[0]
        count = struct.unpack_from("<H", data, offset + 4)[0]
        if count < 4 or count > MAX_CANDIDATES:
            raise RuntimeError(f"invalid candidate count {count} at row {index}")
        word = read_string(data, word_offset)
        if not word:
            raise RuntimeError(f"empty word at row {index}")
        if previous_word and word <= previous_word:
            raise RuntimeError(f"candidate rows are not strictly sorted at row {index}: {previous_word} >= {word}")
        previous_word = word
        candidates = []
        for candidate_index in range(count):
            candidate_offset = struct.unpack_from("<I", data, offset + 8 + (candidate_index * 4))[0]
            candidates.append(read_string(data, candidate_offset))
        rows[word] = candidates
    return rows


def main() -> None:
    parser = argparse.ArgumentParser(description="Validate V2 binary rhyme candidate table.")
    parser.add_argument("--asset", default="app/src/main/assets/rhyme_candidates_v2.tfcand")
    args = parser.parse_args()
    path = pathlib.Path(args.asset)
    if not path.exists():
        raise SystemExit(f"missing candidate asset: {path}")
    rows = parse(path)
    for word in REQUIRED:
        if word not in rows:
            raise SystemExit(f"missing candidate row: {word}")
        if len(rows[word]) < 4:
            raise SystemExit(f"candidate row too short for {word}")
    for word in REQUIRED_WIDE:
        if word not in rows:
            raise SystemExit(f"missing wide candidate row: {word}")
        if len(rows[word]) < 8:
            raise SystemExit(f"candidate row should provide at least 8 V2 suggestions for {word}: {rows[word]}")
    for alias, canonical in REQUIRED_HIP_HOP_ALIAS.items():
        if alias not in rows:
            raise SystemExit(f"missing hip-hop alias row: {alias}")
        if canonical not in rows[alias][:3]:
            raise SystemExit(f"alias row {alias} should include {canonical} in top3: {rows[alias]}")
    for word, required_candidates in REQUIRED_PHRASE_ENDINGS.items():
        if word not in rows:
            raise SystemExit(f"missing phrase-ending row: {word}")
        for candidate in required_candidates:
            if candidate not in rows[word]:
                raise SystemExit(f"phrase-ending row {word} missing {candidate}: {rows[word]}")
    print(f"rhyme_candidates_v2 valid with {len(rows)} rows")


if __name__ == "__main__":
    main()
