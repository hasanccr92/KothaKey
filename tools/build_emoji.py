#!/usr/bin/env python3
"""Build Gboard-like emoji + keyword assets from Unicode emoji-test and CLDR."""

from __future__ import annotations

import re
import ssl
import sys
import urllib.request
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app" / "src" / "main" / "assets" / "emoji"

EMOJI_TEST = "https://www.unicode.org/Public/emoji/15.1/emoji-test.txt"
CLDR = {
    "en": "https://raw.githubusercontent.com/unicode-org/cldr/release-45/common/annotations/en.xml",
    "fr": "https://raw.githubusercontent.com/unicode-org/cldr/release-45/common/annotations/fr.xml",
}
CLDR_DERIVED = {
    "en": "https://raw.githubusercontent.com/unicode-org/cldr/release-45/common/annotationsDerived/en.xml",
    "fr": "https://raw.githubusercontent.com/unicode-org/cldr/release-45/common/annotationsDerived/fr.xml",
}

GROUP_MAP = {
    "Smileys & Emotion": "smileys",
    "People & Body": "people",
    "Animals & Nature": "animals",
    "Food & Drink": "food",
    "Travel & Places": "travel",
    "Activities": "activities",
    "Objects": "objects",
    "Symbols": "symbols",
    "Flags": "flags",
}

SKIN_TONES = ("\U0001f3fb", "\U0001f3fc", "\U0001f3fd", "\U0001f3fe", "\U0001f3ff")
SKIP_GROUPS = {"Component"}


def fetch(url: str) -> bytes:
    ctx = ssl.create_default_context()
    req = urllib.request.Request(url, headers={"User-Agent": "KothaKey/1.0"})
    with urllib.request.urlopen(req, context=ctx, timeout=120) as resp:
        return resp.read()


def parse_emoji_test(text: str) -> list[tuple[str, str, list[str]]]:
    """Return (category, emoji, skin_variants) for fully-qualified base emoji."""
    group = ""
    bases: list[tuple[str, str]] = []
    variants: dict[str, list[str]] = defaultdict(list)
    line_re = re.compile(r"^([0-9A-F ]+)\s+;\s+fully-qualified\s+#\s+(\S+)")
    for raw in text.splitlines():
        if raw.startswith("# group:"):
            group = raw.split(":", 1)[1].strip()
            continue
        if group in SKIP_GROUPS or group not in GROUP_MAP:
            continue
        m = line_re.match(raw)
        if not m:
            continue
        codes = m.group(1).split()
        emoji = m.group(2)
        if any(c in {"1F3FB", "1F3FC", "1F3FD", "1F3FE", "1F3FF"} for c in codes):
            base_codes = [c for c in codes if c not in {"1F3FB", "1F3FC", "1F3FD", "1F3FE", "1F3FF"}]
            base = "".join(chr(int(c, 16)) for c in base_codes)
            variants[base].append(emoji)
            continue
        bases.append((GROUP_MAP[group], emoji))

    out: list[tuple[str, str, list[str]]] = []
    seen: set[str] = set()
    for cat, emoji in bases:
        if emoji in seen:
            continue
        seen.add(emoji)
        skins = variants.get(emoji, [])
        out.append((cat, emoji, skins))
    return out


def parse_annotations(xml_bytes: bytes) -> dict[str, list[str]]:
    root = ET.fromstring(xml_bytes)
    ns = ""
    if root.tag.startswith("{"):
        ns = root.tag.split("}")[0] + "}"
    result: dict[str, set[str]] = defaultdict(set)
    for ann in root.iter(f"{ns}annotation"):
        cp = ann.get("cp")
        if not cp or ann.get("type") == "tts":
            continue
        text = (ann.text or "").strip()
        for part in text.split("|"):
            word = part.strip().casefold()
            word = word.replace("’", "'")
            if word:
                result[cp].add(word)
    return {k: sorted(v) for k, v in result.items()}


def write_emoji(rows: list[tuple[str, str, list[str]]]) -> Path:
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / "emoji.tsv"
    with path.open("w", encoding="utf-8", newline="\n") as fh:
        for cat, emoji, skins in rows:
            fh.write(f"{cat}\t{emoji}\t{','.join(skins)}\n")
    return path


def write_keywords(lang: str, annotations: dict[str, list[str]], emoji_set: set[str]) -> Path:
    path = OUT / f"keywords_{lang}.tsv"
    with path.open("w", encoding="utf-8", newline="\n") as fh:
        for emoji in sorted(emoji_set):
            words = annotations.get(emoji)
            if not words:
                continue
            fh.write(f"{emoji}\t{','.join(words)}\n")
    return path


def main() -> int:
    print("fetch emoji-test", flush=True)
    test = fetch(EMOJI_TEST).decode("utf-8", errors="replace")
    rows = parse_emoji_test(test)
    path = write_emoji(rows)
    emoji_set = {e for _, e, _ in rows}
    print(f"  wrote {path} ({len(rows)} emoji)", flush=True)

    for lang, url in CLDR.items():
        print(f"fetch annotations {lang}", flush=True)
        ann = parse_annotations(fetch(url))
        try:
            derived = parse_annotations(fetch(CLDR_DERIVED[lang]))
            for k, v in derived.items():
                ann.setdefault(k, v)
        except Exception as exc:
            print(f"  derived skip: {exc}", flush=True)
        kpath = write_keywords(lang, ann, emoji_set)
        print(f"  wrote {kpath} ({kpath.stat().st_size} bytes)", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
