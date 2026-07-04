#!/usr/bin/env python3

import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
VIEW_MODEL = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "davehq" / "thetopflow" / "NotesViewModel.kt"
ENGINE2 = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "davehq" / "thetopflow" / "rhyme" / "RhymeEngine2.kt"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def main() -> None:
    view_model = VIEW_MODEL.read_text(encoding="utf-8")
    engine2 = ENGINE2.read_text(encoding="utf-8")
    require("import android.util.Log" in view_model, "route telemetry must log trace rows")
    require("private val rhymeRouteMetrics = RhymeRouteMetrics()" in view_model, "route metrics holder is missing")
    require("rhyme_trace stage=route source=$source" in view_model, "route trace must include source")
    require("v2Count=$v2Count fallbackCount=$fallbackCount count=$finalCount ms=$elapsedMs" in view_model, "route trace must include counts and timing")
    require("routeTotal=${snapshot.total} routeV2=${snapshot.v2} routeFallback=${snapshot.fallback} routeEmpty=${snapshot.empty} avgMs=${snapshot.averageMs}" in view_model, "route trace must include rolling counters")
    require("private class RhymeRouteMetrics" in view_model, "route metrics class is missing")
    require("@Synchronized" in view_model, "route metric updates must be synchronized")
    require("source == \"v2\"" in view_model, "route metrics must count V2 hits")
    require("source.contains(\"fallback\")" in view_model, "route metrics must count fallback hits")
    require("chars=${word.length}" in view_model, "route trace must avoid logging the raw rhyme word")
    require("stage=v2_suggest chars=${normalized.length}" in engine2, "V2 suggest trace must avoid logging the raw normalized word")
    require("word=$normalized" not in engine2, "V2 suggest trace must not log raw words")
    print("rhyme_route_telemetry valid: source counters and sanitized timing logs are wired")


if __name__ == "__main__":
    main()
