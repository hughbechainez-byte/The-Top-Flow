#!/usr/bin/env python3

import argparse
import pathlib
import statistics
import sys
import time

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

import validate_rhyme_candidates_v2 as candidates_v2

DEFAULT_QUERIES = [
    "time", "out", "running", "moving", "finna", "tryna", "gonna", "wanna",
    "spittin", "rappin", "stackin", "flexin", "grindin", "shinin", "thang",
    "yours", "hover", "cover", "near", "flow",
]


def percentile(values: list[float], pct: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    index = min(len(ordered) - 1, int(round((pct / 100.0) * (len(ordered) - 1))))
    return ordered[index]


def main() -> None:
    parser = argparse.ArgumentParser(description="Benchmark V2 binary candidate lookup.")
    parser.add_argument("--asset", default="app/src/main/assets/rhyme_candidates_v2.tfcand")
    parser.add_argument("--iterations", type=int, default=2_000)
    args = parser.parse_args()
    rows = candidates_v2.parse(pathlib.Path(args.asset))
    timings = []
    missing = set()
    for index in range(args.iterations):
        query = DEFAULT_QUERIES[index % len(DEFAULT_QUERIES)]
        start = time.perf_counter_ns()
        result = rows.get(query, [])
        elapsed_ms = (time.perf_counter_ns() - start) / 1_000_000.0
        timings.append(elapsed_ms)
        if len(result) < 4:
            missing.add(query)
    if missing:
        raise SystemExit(f"missing benchmark rows: {sorted(missing)}")
    p50 = statistics.median(timings)
    p95 = percentile(timings, 95)
    p99 = percentile(timings, 99)
    print(
        "rhyme_candidates_v2 benchmark "
        f"queries={len(DEFAULT_QUERIES)} iterations={args.iterations} "
        f"p50={p50:.6f}ms p95={p95:.6f}ms p99={p99:.6f}ms max={max(timings):.6f}ms"
    )


if __name__ == "__main__":
    main()
