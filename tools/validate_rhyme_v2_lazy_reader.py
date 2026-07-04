#!/usr/bin/env python3

import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
ENGINE = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "davehq" / "thetopflow" / "rhyme" / "RhymeEngine2.kt"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def main() -> None:
    source = ENGINE.read_text(encoding="utf-8")
    require("Array<String>" not in source, "V2 reader must not allocate a full word array at load")
    require("rowOffsets" not in source, "V2 reader must not allocate row offset cache at load")
    require("rows.sortBy" not in source, "V2 reader must not sort all rows at load")
    require("while (low <= high)" in source, "V2 reader must binary-search the row table")
    require("val candidateWord = readString(data.getInt(rowOffset))" in source, "V2 lookup must read only probed words")
    require("private fun candidatesFromRow(rowOffset: Int, limit: Int)" in source, "V2 lookup must decode candidates only after a row match")
    print("rhyme_v2_lazy_reader valid: no load-time word table decode")


if __name__ == "__main__":
    main()
