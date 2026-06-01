#!/usr/bin/env bash
# Regenerate Elenii drawable + mipmap assets from elenii-logo-assets/ (el10 symbol).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/elenii-logo-assets"
OUT="$ROOT/app/src/elenii/res"
DRAW="$OUT/drawable"
SYMBOL="$SRC/el10.png"
WORDMARK="$SRC/el1.png"

if [[ ! -f "$SYMBOL" ]]; then
  echo "Missing $SYMBOL" >&2
  exit 1
fi

mkdir -p "$DRAW"

# Welcome / Get started — full symbol (same asset users see on first screen).
sips -z 512 512 "$SYMBOL" --out "$DRAW/brand_welcome_logo.png" >/dev/null

# Launcher icons: same el10 symbol, inset so adaptive-icon crop matches welcome logo.
python3 - "$SYMBOL" "$DRAW" <<'PY'
import shutil
import sys
from pathlib import Path
from PIL import Image

symbol_path = Path(sys.argv[1])
draw_dir = Path(sys.argv[2])
symbol = Image.open(symbol_path).convert("RGBA")

CANVAS = 432
INSET_SCALE = 0.72  # matches welcome screen proportions inside launcher mask


def padded_symbol(canvas: int = CANVAS, scale: float = INSET_SCALE) -> Image.Image:
    target = int(canvas * scale)
    resized = symbol.resize((target, target), Image.Resampling.LANCZOS)
    out = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    offset = (canvas - target) // 2
    out.paste(resized, (offset, offset), resized)
    return out


launcher = padded_symbol()
launcher.save(draw_dir / "ic_launcher_foreground.png")
launcher.save(draw_dir / "elenii_launcher_foreground.png")

mono = launcher.copy()
px = mono.load()
w, h = mono.size
for y in range(h):
    for x in range(w):
        r, g, b, a = px[x, y]
        if a < 32:
            continue
        l = int(0.299 * r + 0.587 * g + 0.114 * b)
        px[x, y] = (l, l, l, a)
mono.save(draw_dir / "elenii_launcher_monochrome.png")

for folder, size in (
    ("mipmap-mdpi", 48),
    ("mipmap-hdpi", 72),
    ("mipmap-xhdpi", 96),
    ("mipmap-xxhdpi", 144),
    ("mipmap-xxxhdpi", 192),
):
    out_dir = draw_dir.parent / folder
    out_dir.mkdir(parents=True, exist_ok=True)
    icon = out_dir / "ic_launcher.png"
    padded_symbol(size, INSET_SCALE).save(icon)
    shutil.copy(icon, out_dir / "ic_launcher_round.png")
PY

sips -z 512 512 "$SYMBOL" --out "$DRAW/elenii_playstore_icon.png" >/dev/null
sips -z 800 800 "$SYMBOL" --out "$DRAW/elenii_splash_logo.png" >/dev/null
sips -z 512 512 "$SYMBOL" --out "$DRAW/elenii_symbol_light.png" >/dev/null
cp "$DRAW/elenii_symbol_light.png" "$DRAW/elenii_symbol_dark.png"
cp "$DRAW/elenii_splash_logo.png" "$DRAW/brand_splash_logo.png"

if [[ -f "$WORDMARK" ]]; then
  sips -z 400 1600 "$WORDMARK" --out "$DRAW/elenii_wordmark_light.png" >/dev/null
  cp "$DRAW/elenii_wordmark_light.png" "$DRAW/elenii_wordmark_dark.png"
  sips -z 300 1200 "$WORDMARK" --out "$DRAW/elenii_header_wordmark.png" >/dev/null
  cp "$DRAW/elenii_header_wordmark.png" "$DRAW/brand_header_wordmark.png"
fi

echo "Elenii assets written (symbol: el10.png; launcher matches brand_welcome_logo)"
