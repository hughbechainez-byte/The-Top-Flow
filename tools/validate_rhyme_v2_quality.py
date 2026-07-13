#!/usr/bin/env python3

import argparse
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

import validate_rhyme_candidates_v2 as candidates_v2


def values(raw: str) -> list[str]:
    raw = raw.strip()
    if not raw:
        return []
    return [item.strip() for item in raw.split(",") if item.strip()]


def parse_golden(path: pathlib.Path) -> list[dict[str, list[str] | str]]:
    rows = []
    with path.open("r", encoding="utf-8") as handle:
        header = []
        for line in handle:
            line = line.strip()
            if not line or line.startswith("#"):
                if line.startswith("# query"):
                    header = [part.strip().lstrip("# ") for part in line.split("\t")]
                continue
            if not header:
                raise RuntimeError("golden file is missing header")
            parts = line.split("\t")
            if len(parts) != len(header):
                raise RuntimeError(f"bad golden row: {line}")
            item = dict(zip(header, parts))
            rows.append(
                {
                    "query": item["query"].strip(),
                    "include_any": values(item["include_any"]),
                    "top_any": values(item["top_any"]),
                    "exclude_all": values(item["exclude_all"]),
                }
            )
    return rows


def main() -> None:
    parser = argparse.ArgumentParser(description="Validate V2 rhyme candidate quality.")
    parser.add_argument("--asset", default="app/src/main/assets/rhyme_candidates_v2.tfcand")
    parser.add_argument("--golden", default="tools/rhyme_v2_golden_queries.tsv")
    args = parser.parse_args()
    rows = candidates_v2.parse(pathlib.Path(args.asset))
    golden_rows = parse_golden(pathlib.Path(args.golden))
    failures = []
    for row in golden_rows:
        query = str(row["query"])
        suggestions = rows.get(query, [])
        top_three = suggestions[:3]
        include_any = set(row["include_any"])
        top_any = set(row["top_any"])
        exclude_all = set(row["exclude_all"])
        if not suggestions:
            if row["include_any"] or row["top_any"]:
                failures.append(f"{query}: missing suggestions")
            continue
        if include_any and not include_any.intersection(suggestions):
            failures.append(f"{query}: expected one of {sorted(include_any)} in {suggestions}")
        if top_any and not top_any.intersection(top_three):
            failures.append(f"{query}: expected one of {sorted(top_any)} in top3 {top_three}")
        bad = exclude_all.intersection(suggestions)
        if bad:
            failures.append(f"{query}: excluded suggestions present {sorted(bad)} in {suggestions}")
    if failures:
        raise SystemExit("\n".join(failures))
    print(f"rhyme_v2_quality valid for {len(golden_rows)} golden queries")


if __name__ == "__main__":
    main()
