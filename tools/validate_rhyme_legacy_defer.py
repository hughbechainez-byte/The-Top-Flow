#!/usr/bin/env python3

import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
ENGINE = ROOT / "app" / "src" / "main" / "java" / "com" / "davehq" / "thetopflow" / "RhymeEngine.java"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def main() -> None:
    source = ENGINE.read_text(encoding="utf-8")
    require("LEGACY_FULL_ENGINE_DEFER_MS = 1800L" in source, "legacy full engine defer must be 1800ms")
    require("private void deferLegacyFullEngineAfterFastReady()" in source, "defer helper is missing")
    require("if (!fastReady || LEGACY_FULL_ENGINE_DEFER_MS <= 0L) return;" in source, "defer must only run after fast readiness")
    require("rhyme_trace stage=legacy_full_defer ms=" in source, "defer trace is missing")
    require("Thread.sleep(LEGACY_FULL_ENGINE_DEFER_MS)" in source, "defer must sleep on the background loader thread")
    require("Thread.currentThread().interrupt()" in source, "defer interruption must preserve interrupt status")
    load_async = source[source.index("void loadAsync(LoadCallbacks callbacks)"):]
    require(load_async.index("callbacks.onFastReady();") < load_async.index("deferLegacyFullEngineAfterFastReady();"), "fast-ready callback must fire before legacy defer")
    require(load_async.index("deferLegacyFullEngineAfterFastReady();") < load_async.index("loadFullEngineAfterFastCache();"), "legacy defer must happen before full engine load")
    print("rhyme_legacy_defer valid: full legacy warmup waits after fast-ready callback")


if __name__ == "__main__":
    main()
