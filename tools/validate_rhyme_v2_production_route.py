#!/usr/bin/env python3

import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
VIEW_MODEL = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "davehq" / "thetopflow" / "NotesViewModel.kt"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def main() -> None:
    source = VIEW_MODEL.read_text(encoding="utf-8")
    require("import com.davehq.thetopflow.rhyme.RhymeEngine2" in source, "NotesViewModel must import RhymeEngine2")
    require("private val rhymeEngine2 = RhymeEngine2(application)" in source, "NotesViewModel must own a V2 engine")
    require("@Volatile private var rhymeEngine2Ready = false" in source, "V2 ready flag must be volatile")
    require("withContext(Dispatchers.IO) { rhymeEngine2.load() }" in source, "V2 asset load must run off the main thread")
    require("private fun isAnyRhymeEngineReady()" in source, "combined readiness helper is missing")
    require("rhymeEngine2Ready || rhymeEngine.isFastReady() || rhymeEngine.isReady()" in source, "readiness must include V2 and existing engines")
    require("private fun suggestRhymes(word: String, body: String): List<String>" in source, "production route helper is missing")
    require(re.search(r"rhymeEngine2\.suggest\(\s*word,\s*8\s*\)", source, re.S), "V2 route must request 8 editor suggestions")
    require("if (v2.size >= 4)" in source, "V2 route must require enough suggestions before bypassing fallback")
    require("rhymeEngine.suggest(" in source, "existing rhyme engine fallback must remain")
    require(re.search(r"rhymeEngine\.suggest\(\s*word,\s*8,\s*360,", source, re.S), "fallback must preserve default 8/360 request")
    require("return if (rhymeEngine2Ready) 20L" in source, "V2-ready editor delay should be lower than fast-cache delay")
    print("rhyme_v2_production_route valid: V2-first default route with fallback is wired")


if __name__ == "__main__":
    main()
