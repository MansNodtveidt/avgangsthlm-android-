#!/usr/bin/env python3
"""
process_icon.py  -  Crop AvgangSthlm.png to icon area and generate all
                    Android mipmap PNGs (plain + circular-mask round).

Source image has:
  * White outer padding all around
  * Rounded-square icon illustration (top portion)
  * White gap + "AvgangSthlm" text label (bottom portion -- discarded)
"""

import os
import glob as _glob
import numpy as np
from PIL import Image, ImageDraw

# --------------------------------------------------------------------------- #

SRC     = r"C:\Users\ns\Pictures\AvgångSthlm2.png"
HERE    = os.path.dirname(os.path.abspath(__file__))
RES_DIR = os.path.join(HERE, "app", "src", "main", "res")

DENSITY_SIZES = [
    ("mipmap-mdpi",     48),
    ("mipmap-hdpi",     72),
    ("mipmap-xhdpi",    96),
    ("mipmap-xxhdpi",  144),
    ("mipmap-xxxhdpi", 192),
]

# --------------------------------------------------------------------------- #

def find_icon_crop(img: Image.Image):
    """
    Return (left, top, right, bottom) tight bounding box of all non-white
    content pixels — no padding on any side.
    """
    rgb = np.array(img.convert("RGB"))
    h, w = rgb.shape[:2]

    # A pixel is "content" when at least one channel is below 238
    is_content = ~np.all(rgb >= 238, axis=2)
    row_counts  = is_content.sum(axis=1)
    col_counts  = is_content.sum(axis=0)

    content_cols = np.where(col_counts > 0)[0]
    content_rows = np.where(row_counts > 0)[0]
    if len(content_cols) == 0 or len(content_rows) == 0:
        return (0, 0, w, h)

    left   = int(content_cols[0])
    right  = int(content_cols[-1]) + 1
    top    = int(content_rows[0])
    bottom = int(content_rows[-1]) + 1

    print(f"  tight crop=({left}, {top}, {right}, {bottom})  "
          f"size={right-left}x{bottom-top}")

    return (left, top, right, bottom)


def circle_mask(size: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    return mask


def clean_mipmap_dirs():
    patterns = [
        "mipmap-*/ic_launcher.webp",
        "mipmap-*/ic_launcher_round.webp",
        "mipmap-*/ic_launcher.png",
        "mipmap-*/ic_launcher_round.png",
        "mipmap-anydpi-v26/ic_launcher.xml",
        "mipmap-anydpi-v26/ic_launcher_round.xml",
    ]
    for pattern in patterns:
        for path in _glob.glob(os.path.join(RES_DIR, pattern)):
            os.remove(path)
            print(f"  removed  {os.path.relpath(path, HERE)}")


def main():
    print("AvgangSthlm icon processor")
    print("-" * 28)

    # ── Load ─────────────────────────────────────────────────────────────────
    img = Image.open(SRC).convert("RGBA")
    print(f"\nSource:  {img.size[0]} x {img.size[1]} px")

    # ── Detect + crop icon area ───────────────────────────────────────────────
    bounds = find_icon_crop(img)
    print(f"Crop box: {bounds}")
    icon = img.crop(bounds)

    # Ensure exact square canvas (paste centred on white-transparent bg)
    w, h = icon.size
    if w != h:
        side = max(w, h)
        canvas = Image.new("RGBA", (side, side), (255, 255, 255, 0))
        canvas.paste(icon, ((side - w) // 2, (side - h) // 2))
        icon = canvas

    print(f"Icon canvas: {icon.size[0]} x {icon.size[1]} px")

    # ── Save 512 Play-Store preview ───────────────────────────────────────────
    preview = icon.resize((512, 512), Image.LANCZOS)
    preview_path = os.path.join(HERE, "ic_launcher_play_store_512.png")
    preview.save(preview_path, "PNG")
    print(f"Saved  ic_launcher_play_store_512.png")

    # ── Clean existing mipmap files ───────────────────────────────────────────
    print()
    clean_mipmap_dirs()

    # ── Generate all density sizes ────────────────────────────────────────────
    print("\nSaving mipmap PNGs ...")
    for dir_name, size in DENSITY_SIZES:
        out_dir = os.path.join(RES_DIR, dir_name)
        os.makedirs(out_dir, exist_ok=True)

        scaled = icon.resize((size, size), Image.LANCZOS)

        # ic_launcher.png – plain (system applies its own shape mask)
        scaled.save(os.path.join(out_dir, "ic_launcher.png"), "PNG")

        # ic_launcher_round.png – circular mask baked in
        round_img = scaled.copy()
        round_img.putalpha(circle_mask(size))
        round_img.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG")

        print(f"  {dir_name:<22} {size}x{size}")

    print("\nDone - sync Gradle and rebuild to pick up the new icons.")


if __name__ == "__main__":
    main()
