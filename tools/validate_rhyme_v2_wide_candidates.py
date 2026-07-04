#!/usr/bin/env python3

import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
BUILDER = ROOT / "tools" / "build_rhyme_candidates_v2.py"
VALIDATOR = ROOT / "tools" / "validate_rhyme_candidates_v2.py"
ENGINE = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "davehq" / "thetopflow" / "rhyme" / "RhymeEngine2.kt"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def main() -> None:
    builder = BUILDER.read_text(encoding="utf-8")
    validator = VALIDATOR.read_text(encoding="utf-8")
    engine = ENGINE.read_text(encoding="utf-8")
    for name, source in [("builder", builder), ("validator", validator), ("engine", engine)]:
        require("ROW_SIZE = 56" in source, f"{name} must use 56-byte V2 candidate rows")
        require("MAX_CANDIDATES = 12" in source, f"{name} must support 12 V2 candidates")
    require("--expanded-source" in builder, "builder must merge the expanded 12-wide cache")
    require("rhyme_expanded_hot_cache.tfcache" in builder, "builder must default to the expanded cache overlay")
    require("REQUIRED_WIDE" in validator, "validator must enforce 8+ suggestions for important rows")
    require("len(rows[word]) < 8" in validator, "validator must fail when wide rows cannot fill the editor strip")
    print("rhyme_v2_wide_candidates valid: 12-wide V2 row format is wired")


if __name__ == "__main__":
    main()
