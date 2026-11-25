"""Resize logo.jpeg into Android launcher densities with NEAREST resampling."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "logo.jpeg"
RES = ROOT / "app" / "src" / "main" / "res"

SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Adaptive-icon foreground is typically larger; keep crisp pixel art.
FG_SIZES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}


def main() -> None:
    src = Image.open(SRC).convert("RGBA")
    for folder, size in SIZES.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        icon = src.resize((size, size), Image.NEAREST)
        icon.save(out_dir / "ic_launcher.png")
        icon.save(out_dir / "ic_launcher_round.png")
        print(f"{folder}: {size}x{size}")

    for folder, size in FG_SIZES.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        fg = src.resize((size, size), Image.NEAREST)
        fg.save(out_dir / "ic_launcher_foreground.png")
        print(f"{folder}/foreground: {size}x{size}")

    play = src.resize((512, 512), Image.NEAREST)
    play_path = ROOT / "playstore_icon.png"
    play.save(play_path)
    print(f"playstore: {play_path}")


if __name__ == "__main__":
    main()
