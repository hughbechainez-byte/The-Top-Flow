#!/usr/bin/env python3
"""Build The Top Flow V2 rhyme lexicon assets."""

from __future__ import annotations

import argparse
import csv
import re
import struct
import sys
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSET_SOURCE_ROOT = ROOT / "app" / "src" / "main" / "assets"
CMU_DICT_PATH = ASSET_SOURCE_ROOT / "cmudict.dict"
RHYME_ENGINE_PATH = ROOT / "app" / "src" / "main" / "java" / "com" / "davehq" / "thetopflow" / "RhymeEngine.java"
SLANG_PATH = ROOT / "tools" / "rhyme_sources" / "hiphop_slang.tsv"
LEGACY_INDEX_PATH = ASSET_SOURCE_ROOT / "rhyme_index.tsv"
OUTPUT_DIR = ROOT / "build" / "rhyme-v2-debug"
DEBUG_PATH = OUTPUT_DIR / "lexicon_debug.tsv"
ASSET_PATH = ASSET_SOURCE_ROOT / "rhyme_lexicon_v2.tflex"

MAGIC = b"TFLEX2"
VERSION = 2
HEADER_STRUCT = struct.Struct("<6s H I I I I I I")
ROW_STRUCT = struct.Struct("<III I H I H H")

VOWEL_RE = re.compile(r"[012]$")


def parse_java_list(source: str, name: str) -> list[str]:
    pattern = re.compile(rf"(?s)private\\s+static\\s+final\\s+String\\[\\]\\s+{re.escape(name)}\\s*=\\s*\\{{(.*?)\\}};")
    match = pattern.search(source)
    if not match:
        return []
    block = match.group(1)
    return re.findall(r'"([^"\\\\]*(?:\\\\.[^"\\\\]*)*)"', block)


def normalize_word(word: str) -> str:
    return re.sub(r"[^a-z']+", "", (word or "").lower()).strip("'")


def parse_cmu_dict(path: Path) -> list[tuple[str, str, int, str]]:
    out = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith(";;;"):
            continue
        parts = line.strip().split(None, 1)
        if len(parts) != 2:
            continue
        raw_word = parts[0]
        phones = parts[1].strip()
        if not raw_word:
            continue
        normalized = raw_word.split("(", 1)[0].strip()
        if not normalized or not re.fullmatch(r"[A-Za-z']+", normalized):
            continue
        word = normalize_word(normalized)
        if not word:
            continue
        out.append((word, phones, max(1, count_syllables(phones)), stress_pattern(phones)))
    return out


def count_syllables(phones: str) -> int:
    return max(1, sum(1 for token in phones.split() if token and VOWEL_RE.search(token)))


def stress_pattern(phones: str) -> str:
    marks = []
    for token in phones.split():
        if len(token) < 3:
            continue
        if token[-1] in "012":
            marks.append(token[-1])
    if not marks:
        return "0"
    return "".join(marks)


def parse_legacy_index() -> list[tuple[str, str]]:
    out = []
    for line in LEGACY_INDEX_PATH.read_text(encoding="utf-8", errors="ignore").splitlines():
        if not line or line.startswith("#"):
            continue
        parts = line.split("\t", 3)
        if len(parts) < 2:
            continue
        word = normalize_word(parts[0])
        phones = parts[1].strip()
        if not word or not phones:
            continue
        out.append((word, phones))
    return out


def load_slang_entries(path: Path) -> list[tuple[str, str, int, str]]:
    out = []
    if not path.exists():
        return out
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        for row in reader:
            word = normalize_word(row.get("normalized", ""))
            phones = (row.get("phones") or "").strip()
            confidence = parse_confidence(row.get("confidence"))
            if not word or not phones:
                continue
            out.append((word, phones, count_syllables(phones), confidence))
    return out


def parse_confidence(value: str | None) -> float:
    if not value:
        return 0.5
    try:
        return float(value)
    except ValueError:
        return 0.5


def load_phrase_sources(text: str, phrase_list_name: str) -> list[str]:
    phrases = parse_java_list(text, phrase_list_name)
    return [normalize_word(entry) for entry in phrases]


def build_rows() -> list[dict[str, object]]:
    engine_text = RHYME_ENGINE_PATH.read_text(encoding="utf-8", errors="ignore")
    common_words = [normalize_word(item) for item in parse_java_list(engine_text, "COMMON_RHYME_WORDS")]
    common_phrases = parse_java_list(engine_text, "COMMON_RHYME_PHRASES")

    rows: dict[str, dict[str, object]] = {}
    seed_sources = {
        "rhyme_index.tsv": {word: True for word in []},
    }

    for word, phones, syllables, stress in parse_cmu_dict(CMU_DICT_PATH):
        rows[word] = {
            "word": word,
            "display": word,
            "phones": phones,
            "syllables": syllables,
            "stress": stress,
            "flags": 0,
            "confidence": 1.0,
        }

    for word, phones, syllables, confidence in load_slang_entries(SLANG_PATH):
        if word not in rows:
            rows[word] = {
                "word": word,
                "display": word,
                "phones": phones,
                "syllables": syllables,
                "stress": stress_pattern(phones),
                "flags": 1,
                "confidence": confidence,
            }
        else:
            rows[word]["flags"] = 1
            rows[word]["confidence"] = max(rows[word]["confidence"], confidence)

    for surface in common_words:
        if surface and surface not in rows:
            legacy = find_legacy_pronunciation(surface)
            if legacy:
                rows[surface] = {
                    "word": surface,
                    "display": surface,
                    "phones": legacy[1],
                    "syllables": count_syllables(legacy[1]),
                    "stress": stress_pattern(legacy[1]),
                    "flags": 0,
                    "confidence": 0.95,
                }
        elif surface in rows:
            rows[surface]["confidence"] = max(1.0, float(rows[surface]["confidence"]))

    legacy_index = {
        normalize_word(word): phones for word, phones in parse_legacy_index()
    }
    for phrase in common_phrases:
        normalized = normalize_word(phrase)
        if not normalized:
            continue
        if normalized in rows:
            continue
        final_word = normalize_word(phrase.split()[-1]) if " " in phrase else normalized
        if final_word in rows:
            rows[normalized] = {
                "word": normalized,
                "display": phrase.strip(),
                "phones": rows[final_word]["phones"],
                "syllables": rows[final_word]["syllables"],
                "stress": rows[final_word]["stress"],
                "flags": 2,
                "confidence": 0.72,
            }
        elif final_word in legacy_index:
            phones = legacy_index[final_word]
            rows[normalized] = {
                "word": normalized,
                "display": phrase.strip(),
                "phones": phones,
                "syllables": count_syllables(phones),
                "stress": stress_pattern(phones),
                "flags": 2,
                "confidence": 0.65,
            }

    out = []
    for index, item in enumerate(rows.values()):
        out.append({
            "id": index,
            "normalized": item["word"],
            "display": item["display"],
            "phones": item["phones"],
            "syllables": int(item["syllables"]),
            "stress": str(item["stress"]),
            "flags": int(item["flags"]),
            "confidence": min(65535, int(float(item["confidence"]) * 10000)),
        })
    return sorted(out, key=lambda item: str(item["normalized"]))


def find_legacy_pronunciation(word: str) -> tuple[str, str] | None:
    for normalized, phones in parse_legacy_index():
        if normalized == word:
            return normalized, phones
    return None


def add_string(strings: dict[str, int], blob: bytearray, text: str) -> int:
    if text in strings:
        return strings[text]
    offset = len(blob)
    encoded = (text + "\0").encode("utf-8")
    blob.extend(encoded)
    strings[text] = offset
    return offset


def encode_asset(rows: list[dict[str, object]]) -> tuple[bytes, int]:
    strings: dict[str, int] = {}
    string_blob = bytearray()
    row_blob = bytearray()

    def _add(value: str) -> int:
        return add_string(strings, string_blob, value)

    encoded_rows: list[tuple[dict[str, object], tuple[int, int, int, int]]] = []
    for row in rows:
        encoded_rows.append((
            row,
            (
                _add(str(row["normalized"])),
                _add(str(row["display"])),
                _add(str(row["phones"])),
                _add(str(row["stress"])),
            )
        ))

    rows_offset = HEADER_STRUCT.size
    strings_offset = rows_offset + len(rows) * ROW_STRUCT.size
    for row, (normalized_offset, display_offset, phones_offset, stress_offset) in encoded_rows:
        row_blob.extend(ROW_STRUCT.pack(
            int(row["id"]),
            strings_offset + normalized_offset,
            strings_offset + display_offset,
            strings_offset + phones_offset,
            int(row["syllables"]),
            strings_offset + stress_offset,
            int(row["flags"]),
            int(row["confidence"]) % 65536,
        ))

    stress_offset = strings_offset
    flags_offset = strings_offset
    payload = row_blob + string_blob
    checksum_placeholder = 0
    header = HEADER_STRUCT.pack(
        MAGIC,
        VERSION,
        len(rows),
        rows_offset,
        strings_offset,
        stress_offset,
        flags_offset,
        checksum_placeholder,
    )
    data = bytearray(header + payload)
    checksum = zlib.crc32(data) & 0xffffffff
    checksum_bytes = struct.pack("<I", checksum)
    checksum_offset = HEADER_STRUCT.size - 4
    data[checksum_offset : checksum_offset + 4] = checksum_bytes
    return bytes(data), checksum


def write_debug(rows: list[dict[str, object]], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write("id\tnormalized\tdisplay\tphones\tsyllables\tstress\tflags\tconfidence\n")
        for row in rows[:50000]:
            handle.write(
                f"{row['id']}\t{row['normalized']}\t{row['display']}\t"
                f"{row['phones']}\t{row['syllables']}\t{row['stress']}\t"
                f"{row['flags']}\t{row['confidence']}\n"
            )


def build(output: Path, debug_output: Path, limit: int | None = None) -> tuple[bytes, list[dict[str, object]]]:
    rows = build_rows()
    if limit:
        rows = rows[:limit]
    write_debug(rows, debug_output)
    data, _ = encode_asset(rows)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(data)
    return data, rows


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=ASSET_PATH)
    parser.add_argument("--debug-output", type=Path, default=DEBUG_PATH)
    parser.add_argument("--limit", type=int, default=None, help="Optional debug row limit; full build by default.")
    args = parser.parse_args(argv)

    data, rows = build(args.output, args.debug_output, args.limit)
    sample_words = ["my", "try", "time", "running", "runnin", "movin", "naw"]
    print(f"wrote {args.output}")
    print(f"bytes={len(data)} rows={len(rows)}")
    for sample in sample_words:
        row = next((item for item in rows if item["normalized"] == sample), None)
        print(f"{sample}: {bool(row)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
