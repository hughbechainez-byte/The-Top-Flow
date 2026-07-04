#!/usr/bin/env python3

import pathlib
import re
import struct

ROOT = pathlib.Path(__file__).resolve().parents[1]
ENGINE = ROOT / "app" / "src" / "main" / "java" / "com" / "davehq" / "thetopflow" / "RhymeEngine.java"
VIEW_MODEL = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "davehq" / "thetopflow" / "NotesViewModel.kt"
FAST_CACHE = ROOT / "app" / "src" / "main" / "assets" / "rhyme_fast_cache_v2.tfcache"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def read_text(path: pathlib.Path) -> str:
    require(path.exists(), f"missing file: {path}")
    return path.read_text(encoding="utf-8")


def constant_int(source: str, name: str) -> int:
    match = re.search(rf"{name}\s*=\s*(\d+)", source)
    require(match is not None, f"missing constant {name}")
    return int(match.group(1))


def binary_row_count(path: pathlib.Path) -> int:
    require(path.exists(), f"missing fast cache asset: {path}")
    data = path.read_bytes()
    require(data[:8] == b"TFCACHE2", "fast cache has wrong magic")
    version = struct.unpack_from("<H", data, 8)[0]
    require(version == 2, f"unsupported fast cache version {version}")
    return struct.unpack_from("<I", data, 10)[0]


def main() -> None:
    engine = read_text(ENGINE)
    view_model = read_text(VIEW_MODEL)
    max_candidates = constant_int(engine, "FAST_HOT_CACHE_MAX_CANDIDATES")
    cache_limit = constant_int(engine, "FAST_HOT_CACHE_LIMIT")
    require(max_candidates == 360, f"unexpected FAST_HOT_CACHE_MAX_CANDIDATES={max_candidates}")
    require(cache_limit == 6, f"unexpected FAST_HOT_CACHE_LIMIT={cache_limit}")
    require("limit > FAST_HOT_CACHE_LIMIT" not in engine, "fast cache eligibility rejects editor request size")
    require("binaryRows=\" + fastCacheStore.rowCount()" in engine, "fast-ready trace must include binaryRows")
    require("rhymeEngine.suggest(" in view_model, "ViewModel does not call rhymeEngine.suggest")
    require(re.search(r"rhymeEngine\.suggest\(\s*word,\s*8,\s*360,", view_model, re.S), "default fast-row suggest call must stay word, 8, 360")
    require('RhymeEngine.Options("Balanced", false, true, emptySet(), body)' in view_model, "default fast-row options must stay cache eligible")
    rows = binary_row_count(FAST_CACHE)
    require(rows >= 30_000, f"fast cache row count too low: {rows}")
    print(f"rhyme fast contract valid: request=8/360 cacheLimit={cache_limit} rows={rows}")


if __name__ == "__main__":
    main()
