"""Rename KothaKey → KothaKey across sources (packages, classes, scripts)."""
from __future__ import annotations

import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKIP_DIRS = {
    "sdk",
    ".gradle",
    ".gradle-dist",
    "build",
    "avd",
    "__pycache__",
}
TEXT_EXTS = {
    ".java",
    ".xml",
    ".gradle",
    ".properties",
    ".bat",
    ".ps1",
    ".md",
    ".py",
    ".txt",
    ".tsv",
    ".json",
}

REPLACEMENTS = [
    ("com.kothakey", "com.kothakey"),
    ("KothaKeyImeService", "KothaKeyImeService"),
    ("KothaKeyKeyboardView", "KothaKeyKeyboardView"),
    ("KothaKeyPrefs", "KothaKeyPrefs"),
    ("KothaKeyTheme", "KothaKeyTheme"),
    ("Theme.KothaKey", "Theme.KothaKey"),
    ("KothaKey_API34", "KothaKey_API34"),
    ("KothaKey-debug.apk", "KothaKey-debug.apk"),
    ("KothaKey API 34", "KothaKey API 34"),
    ("kothakey_clipboard", "kothakey_clipboard"),
    ("kothakey-emoji-kw", "kothakey-emoji-kw"),
    ("kothakey-emoji", "kothakey-emoji"),
    ("kothakey-bn-dict", "kothakey-bn-dict"),
    ("kothakey-words", "kothakey-words"),
    ("KothaKey/1.0", "KothaKey/1.0"),
    ('"kothakey"', '"kothakey"'),
    ("F:\\keyboard\\KothaKey", "F:\\keyboard\\KothaKey"),
    ("F:/keyboard/KothaKey", "F:/keyboard/KothaKey"),
    ("KothaKey", "KothaKey"),
    ("kothakey", "kothakey"),
]

CLASS_RENAMES = {
    "KothaKeyImeService.java": "KothaKeyImeService.java",
    "KothaKeyPrefs.java": "KothaKeyPrefs.java",
    "KothaKeyTheme.java": "KothaKeyTheme.java",
    "KothaKeyKeyboardView.java": "KothaKeyKeyboardView.java",
}


def should_skip(path: Path) -> bool:
    parts = set(path.parts)
    if parts & SKIP_DIRS:
        return True
    if "app" in path.parts and "build" in path.parts:
        return True
    return False


def rewrite_text(path: Path) -> bool:
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return False
    original = text
    for old, new in REPLACEMENTS:
        text = text.replace(old, new)
    if text != original:
        path.write_text(text, encoding="utf-8", newline="\n")
        return True
    return False


def main() -> None:
    java_src = ROOT / "app" / "src" / "main" / "java"
    old_pkg = java_src / "com" / "kothakey"
    new_pkg = java_src / "com" / "kothakey"
    if old_pkg.exists():
        if new_pkg.exists():
            shutil.rmtree(new_pkg)
        new_pkg.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(old_pkg), str(new_pkg))
        print(f"moved package -> {new_pkg}")

    for path in sorted(ROOT.rglob("*")):
        if not path.is_file() or should_skip(path):
            continue
        if path.name in CLASS_RENAMES:
            dest = path.with_name(CLASS_RENAMES[path.name])
            path.rename(dest)
            path = dest
            print(f"renamed class file -> {dest.name}")
        if path.suffix.lower() in TEXT_EXTS or path.name in {
            "gradlew.bat",
            "install.bat",
            "start-emulator.bat",
            "setup-avd.bat",
            "README.md",
        }:
            if rewrite_text(path):
                print(f"updated {path.relative_to(ROOT)}")

    # AVD rename if present
    avd_home = ROOT / "avd"
    old_ini = avd_home / "KothaKey_API34.ini"
    new_ini = avd_home / "KothaKey_API34.ini"
    old_avd = avd_home / "KothaKey_API34.avd"
    new_avd = avd_home / "KothaKey_API34.avd"
    if old_avd.exists():
        if new_avd.exists():
            shutil.rmtree(new_avd)
        old_avd.rename(new_avd)
        print("renamed AVD directory")
    if old_ini.exists():
        text = old_ini.read_text(encoding="utf-8")
        for old, new in REPLACEMENTS:
            text = text.replace(old, new)
        new_ini.write_text(text, encoding="utf-8", newline="\n")
        old_ini.unlink()
        print("renamed AVD ini")
    elif new_ini.exists():
        text = new_ini.read_text(encoding="utf-8")
        for old, new in REPLACEMENTS:
            text = text.replace(old, new)
        new_ini.write_text(text, encoding="utf-8", newline="\n")

    if new_avd.exists():
        cfg = new_avd / "config.ini"
        if cfg.exists():
            text = cfg.read_text(encoding="utf-8")
            for old, new in REPLACEMENTS:
                text = text.replace(old, new)
            cfg.write_text(text, encoding="utf-8", newline="\n")

    old_apk = ROOT / "KothaKey-debug.apk"
    if old_apk.exists():
        old_apk.unlink()
        print("removed KothaKey-debug.apk")

    print("done (folder rename is separate)")


if __name__ == "__main__":
    main()
