#!/usr/bin/env python3
"""Download FR/EN frequency word lists and write compact TSV assets."""

from __future__ import annotations

import re
import ssl
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app" / "src" / "main" / "assets" / "dict"
LIMIT = 60_000

# FrequencyWords (CC-BY-SA-4.0 / OpenSubtitles). Fallback-friendly plain text: "word count"
SOURCES = {
    "fr": "https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/fr/fr_full.txt",
    "en": "https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/en/en_full.txt",
}

WORD_RE = re.compile(r"^[^\W\d_]+(?:['’-][^\W\d_]+)*$", re.UNICODE)


def fetch(url: str) -> str:
    ctx = ssl.create_default_context()
    req = urllib.request.Request(url, headers={"User-Agent": "KothaKey/1.0"})
    with urllib.request.urlopen(req, context=ctx, timeout=120) as resp:
        return resp.read().decode("utf-8", errors="replace")


def parse(text: str) -> list[tuple[str, int]]:
    out: list[tuple[str, int]] = []
    seen: set[str] = set()
    for raw in text.splitlines():
        raw = raw.strip()
        if not raw or raw.startswith("#"):
            continue
        parts = raw.split()
        if len(parts) < 2:
            continue
        word, freq_s = parts[0], parts[-1]
        word = word.replace("’", "'")
        if not WORD_RE.match(word):
            continue
        key = word.casefold()
        if key in seen:
            continue
        try:
            freq = int(freq_s)
        except ValueError:
            continue
        if freq <= 0:
            continue
        seen.add(key)
        out.append((word, freq))
        if len(out) >= LIMIT:
            break
    return out


def write_tsv(lang: str, rows: list[tuple[str, int]]) -> Path:
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / f"{lang}.tsv"
    with path.open("w", encoding="utf-8", newline="\n") as fh:
        for word, freq in rows:
            fh.write(f"{word}\t{freq}\n")
    return path


def main() -> int:
    for lang, url in SOURCES.items():
        print(f"fetch {lang}: {url}", flush=True)
        text = fetch(url)
        rows = parse(text)
        path = write_tsv(lang, rows)
        print(f"  wrote {path} ({len(rows)} words, {path.stat().st_size} bytes)", flush=True)
        if len(rows) < 10_000:
            print(f"  WARNING: {lang} list is thin", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
