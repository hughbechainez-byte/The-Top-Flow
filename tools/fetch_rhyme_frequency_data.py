#!/usr/bin/env python3
"""Fetch the build-time-only, CC BY-SA wordfreq ranking source."""

import pathlib
import urllib.request

SOURCE_URL = "https://raw.githubusercontent.com/aparrish/wordfreq-en-25000/main/wordfreq-en-25000-log.json"
OUTPUT = pathlib.Path("build/rhyme-v2-debug/wordfreq-en-25000-log.json")


def main() -> None:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    urllib.request.urlretrieve(SOURCE_URL, OUTPUT)
    print(f"wrote {OUTPUT}")


if __name__ == "__main__":
    main()
