#!/usr/bin/env python3

import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
ENGINE = ROOT / "app" / "src" / "main" / "java" / "com" / "davehq" / "thetopflow" / "RhymeEngine.java"
V2_GUARD = ROOT / "tools" / "validate_rhyme_v2_wide_candidates.py"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def main() -> None:
    engine = ENGINE.read_text(encoding="utf-8")
    guard = V2_GUARD.read_text(encoding="utf-8")
    require("LEGACY_EXPANDED_TEXT_CACHE_ENABLED = false" in engine, "legacy expanded text cache must stay disabled")
    require("expanded_cache_skipped_v2_overlay" in engine, "skip trace must identify V2 overlay replacement")
    require("if (LEGACY_EXPANDED_TEXT_CACHE_ENABLED)" in engine, "expanded text load must be guarded")
    require("expandedHotCacheReady = false;" in engine, "expanded hot-cache readiness must be false when skipped")
    require("rhyme_expanded_hot_cache.tfcache" in guard, "V2 wide guard must prove expanded cache overlay remains wired")
    require("MAX_CANDIDATES = 12" in guard, "V2 wide guard must prove 12-candidate local rows remain wired")
    print("rhyme_legacy_expanded_bypass valid: legacy expanded text load is replaced by V2 overlay")


if __name__ == "__main__":
    main()
