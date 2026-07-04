#!/usr/bin/env python3
"""Validate V2 rhyme lexicon assets and seed coverage."""

import csv
import re
import struct
import zlib
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RHYME_ASSET = ROOT / "app" / "src" / "main" / "assets" / "rhyme_lexicon_v2.tflex"
RHYME_ENGINE_PATH = ROOT / "app" / "src" / "main" / "java" / "com" / "davehq" / "thetopflow" / "RhymeEngine.java"
SLANG_PATH = ROOT / "tools" / "rhyme_sources" / "hiphop_slang.tsv"

HEADER_STRUCT = struct.Struct("<6s H I I I I I I")
ROW_STRUCT = struct.Struct("<III I H I H H")
MAGIC = b"TFLEX2"
VERSION = 2


def parse_java_list(source: str, name: str):
    pattern = re.compile(rf"(?s)private\\s+static\\s+final\\s+String\\[\\]\\s+{re.escape(name)}\\s*=\\s*\\{{(.*?)\\}};")
    match = pattern.search(source)
    if not match:
        return []
    block = match.group(1)
    return re.findall(r'"([^"\\\\]*(?:\\\\.[^"\\\\]*)*)"', block)


def normalize_word(word: str) -> str:
    return re.sub(r"[^a-z']+", "", (word or "").lower()).strip("'")


def load_required_seed_words() -> set[str]:
    required = set()
    if SLANG_PATH.exists():
        with SLANG_PATH.open("r", encoding="utf-8", newline="") as handle:
            for row in csv.DictReader(handle, delimiter="\t"):
                value = normalize_word(row.get("normalized") or row.get("surface") or "")
                if value:
                    required.add(value)
    engine = RHYME_ENGINE_PATH.read_text(encoding="utf-8", errors="ignore")
    for word in parse_java_list(engine, "COMMON_RHYME_WORDS"):
        value = normalize_word(word)
        if value:
            required.add(value)
    return required


def read_cstring(data: bytes, offset: int) -> str:
    if offset < 0 or offset >= len(data):
        return ""
    end = data.find(b"\0", offset)
    if end < 0:
        end = len(data)
    return data[offset:end].decode("utf-8", errors="ignore")


def validate(path: Path) -> int:
    failures = []
    if not path.exists():
        print(f"FAIL: asset missing at {path}")
        return 1

    data = path.read_bytes()
    if len(data) < HEADER_STRUCT.size:
        print("FAIL: asset too small")
        return 1

    magic, version, row_count, rows_offset, strings_offset, stress_offset, flags_offset, expected_checksum = HEADER_STRUCT.unpack(data[:HEADER_STRUCT.size])
    if magic != MAGIC:
        failures.append("magic header mismatch")
    if version != VERSION:
        failures.append(f"unsupported version {version}")
    if row_count < 100000:
        failures.append(f"row count too small ({row_count})")
    if rows_offset >= len(data) or strings_offset >= len(data):
        failures.append("invalid section offsets")
    if stress_offset < 0 or flags_offset < 0:
        failures.append("invalid section offsets")
    if not (rows_offset <= strings_offset <= len(data)):
        failures.append("invalid section offset order")
    if not (strings_offset <= stress_offset <= len(data) and strings_offset <= flags_offset <= len(data)):
        failures.append("invalid section offset order")

    if not failures:
        computed = 0
        payload = bytearray(data)
        checksum_offset = HEADER_STRUCT.size - 4
        payload[checksum_offset : checksum_offset + 4] = b"\0\0\0\0"
        computed = zlib.crc32(payload) & 0xFFFFFFFF
        if computed != expected_checksum:
            failures.append(f"checksum mismatch expected={expected_checksum} actual={computed}")

    required = load_required_seed_words()
    if not failures and required:
        if rows_offset > len(data):
            failures.append("strings offset invalid")
        else:
            seen = set()
            cursor = rows_offset
            for _ in range(row_count):
                if cursor + ROW_STRUCT.size > len(data):
                    failures.append("truncated row table")
                    break
                _, norm_offset, _, _, _, stress_rel_offset, _, _ = ROW_STRUCT.unpack_from(data, cursor)
                if strings_offset <= norm_offset < len(data):
                    seen.add(read_cstring(data, norm_offset))
                if strings_offset <= stress_rel_offset < len(data):
                    _ = read_cstring(data, stress_rel_offset)
                cursor += ROW_STRUCT.size
            missing = sorted(word for word in required if word not in seen)
            if missing:
                failures.append(f"required seeds missing: {', '.join(missing[:40])}")

    if failures:
        print("FAIL:")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print(f"PASS: {path} rows={row_count}")
    return 0


def main() -> int:
    return validate(RHYME_ASSET)


if __name__ == "__main__":
    sys.exit(main())
