#!/usr/bin/env python3
"""
generate_icon.py  -  AvgangSthlm app icon generator v2

Design:
  * Gradient background: dark blue (#1a3a6b) top -> mid blue (#2d6ea8) ->
    green (#4a9e6b) -> gold (#f0c040) bottom  (sky + landscape)
  * Sveriges Riksdag silhouette  (left, solid dark navy)
  * Speed motion lines  (white, behind train)
  * Modern SL metro train  (center-right, white body + SL-blue stripe)
  * Rounded-square mask (~18% radius)

Usage:
    python generate_icon.py
"""

import os
import glob as _glob
from PIL import Image, ImageDraw

# --------------------------------------------------------------------------- #
#  Config                                                                      #
# --------------------------------------------------------------------------- #

MASTER = 512

DENSITY_SIZES = [
    ("mipmap-mdpi",     48),
    ("mipmap-hdpi",     72),
    ("mipmap-xhdpi",    96),
    ("mipmap-xxhdpi",  144),
    ("mipmap-xxxhdpi", 192),
]

HERE    = os.path.dirname(os.path.abspath(__file__))
RES_DIR = os.path.join(HERE, "app", "src", "main", "res")

# --------------------------------------------------------------------------- #
#  Colours                                                                     #
# --------------------------------------------------------------------------- #

DARK_BLUE   = (0x1a, 0x3a, 0x6b)          # #1a3a6b  – deep-sky top
MID_BLUE    = (0x2d, 0x6e, 0xa8)          # #2d6ea8  – horizon sky
HORIZON_GRN = (0x4a, 0x9e, 0x6b)          # #4a9e6b  – green ground
GOLD        = (0xf0, 0xc0, 0x40)          # #f0c040  – warm-gold base
NAVY        = (0x0c, 0x15, 0x30, 0xff)    # silhouette – solid dark navy
WHITE_BODY  = (0xf4, 0xf7, 0xff, 0xff)    # train body
SL_BLUE     = (0x1e, 0x5a, 0xb0, 0xff)    # SL stripe blue
WIN_CLR     = (0x90, 0xca, 0xf5, 0xee)    # train windows
WHEEL_CLR   = (0x0e, 0x14, 0x2e, 0xee)    # wheels
CAB_CLR     = (0xe4, 0xec, 0xf8, 0xff)    # cab face (right end)
HEADLIGHT   = (0xff, 0xf0, 0x80, 0xff)    # headlight rectangle

# --------------------------------------------------------------------------- #
#  Helpers                                                                     #
# --------------------------------------------------------------------------- #

def lerp3(c1, c2, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a + (b - a) * t) for a, b in zip(c1, c2))


def multi_lerp(y, s, stops):
    """Multi-stop vertical gradient.  stops = [(fraction, rgb), ...] sorted."""
    yf = y / max(s - 1, 1)
    for i in range(len(stops) - 1):
        f0, c0 = stops[i]
        f1, c1 = stops[i + 1]
        if yf <= f1:
            t = (yf - f0) / max(f1 - f0, 1e-9)
            return lerp3(c0, c1, t)
    return stops[-1][1]


def px(frac, s=MASTER):
    return int(frac * s)


def layer(s):
    return Image.new("RGBA", (s, s), (0, 0, 0, 0))


# --------------------------------------------------------------------------- #
#  Draw                                                                        #
# --------------------------------------------------------------------------- #

def draw_icon(s: int) -> Image.Image:
    img = layer(s)

    def p(f): return px(f, s)

    # ── 1. Gradient background ─────────────────────────────────────────────── #
    stops = [
        (0.00, DARK_BLUE),
        (0.42, MID_BLUE),
        (0.68, HORIZON_GRN),
        (1.00, GOLD),
    ]
    bg = layer(s)
    gd = ImageDraw.Draw(bg)
    for y in range(s):
        r, g, b = multi_lerp(y, s, stops)
        gd.line([(0, y), (s - 1, y)], fill=(r, g, b, 255))

    corner = px(0.18, s)
    msk = layer(s).convert("L")
    ImageDraw.Draw(msk).rounded_rectangle([0, 0, s-1, s-1], radius=corner, fill=255)
    bg.putalpha(msk)
    img = Image.alpha_composite(img, bg)

    # ── 2. Sveriges Riksdag silhouette  (left ~46 %) ───────────────────────── #
    rik = layer(s)
    rd  = ImageDraw.Draw(rik)

    def R(x1, y1, x2, y2):
        rd.rectangle([p(x1), p(y1), p(x2), p(y2)], fill=NAVY)

    def E(x1, y1, x2, y2):
        rd.ellipse([p(x1), p(y1), p(x2), p(y2)], fill=NAVY)

    def T(*pts):
        rd.polygon([(p(x), p(y)) for x, y in pts], fill=NAVY)

    # ground strip
    R(0.00, 0.87, 0.50, 1.00)
    # main facade block
    R(0.02, 0.71, 0.47, 0.87)
    # left wing tower body
    R(0.02, 0.62, 0.10, 0.71)
    # right wing tower body
    R(0.38, 0.62, 0.46, 0.71)
    # left wing dome
    E(0.010, 0.54, 0.108, 0.64)
    # right wing dome
    E(0.370, 0.54, 0.468, 0.64)
    # central dome drum (pedestal)
    R(0.165, 0.57, 0.313, 0.71)
    # main dome (tall ellipse)
    E(0.130, 0.38, 0.348, 0.60)
    # spire / lantern on top of dome
    T((0.222, 0.39), (0.256, 0.39), (0.239, 0.24))

    img = Image.alpha_composite(img, rik)

    # ── 3. Motion / speed lines (between building and train) ──────────────── #
    ml  = layer(s)
    mld = ImageDraw.Draw(ml)

    # lines end just before train rear (~x=0.38)
    x_end = p(0.38)
    specs = [
        # (y_frac, length_frac, width_frac, alpha)
        (0.525, 0.20, 0.008, 210),
        (0.550, 0.15, 0.007, 175),
        (0.577, 0.22, 0.008, 195),
        (0.605, 0.12, 0.006, 150),
    ]
    for fy, lf, wf, alpha in specs:
        ly  = p(fy)
        lw  = max(int(s * wf), 1)
        lx0 = x_end - p(lf)
        mld.line([(lx0, ly), (x_end, ly)], fill=(255, 255, 255, alpha), width=lw)

    img = Image.alpha_composite(img, ml)

    # ── 4. Metro / tunnelbana train  (center-right) ────────────────────────── #
    tx1, ty1 = p(0.39), p(0.46)
    tx2, ty2 = p(0.93), p(0.65)
    tw  = tx2 - tx1
    th  = ty2 - ty1
    r_b = max(int(th * 0.36), 1)
    s_h = int(th * 0.28)             # stripe height (bottom)

    # wheels (drawn first so body sits on top)
    wh_lyr = layer(s)
    wd     = ImageDraw.Draw(wh_lyr)
    wr     = max(int(s * 0.030), 2)
    wy_c   = ty2 + max(int(wr * 0.48), 1)
    for fx in (0.50, 0.66, 0.82):
        cx = p(fx)
        wd.ellipse([cx-wr, wy_c-wr, cx+wr, wy_c+wr], fill=WHEEL_CLR)
    img = Image.alpha_composite(img, wh_lyr)

    # body layers
    body = layer(s)
    bd   = ImageDraw.Draw(body)

    # white base
    bd.rectangle([tx1, ty1, tx2, ty2], fill=WHITE_BODY)

    # SL-blue stripe at bottom
    bd.rectangle([tx1, ty2 - s_h, tx2, ty2], fill=SL_BLUE)

    # windows (row just above stripe)
    wy1   = ty1 + max(int(th * 0.12), 1)
    wy2   = ty2 - s_h - max(int(th * 0.10), 1)
    wh_px = max(wy2 - wy1, 1)
    ww    = max(int(tw * 0.12), 2)
    wgap  = max(int(tw * 0.040), 1)
    wx0   = tx1 + max(int(tw * 0.055), 1)
    x_lim = tx2 - max(int(tw * 0.12), 1)   # leave room for cab
    for i in range(7):
        wx = wx0 + i * (ww + wgap)
        if wx + ww > x_lim:
            break
        bd.rounded_rectangle(
            [wx, wy1, wx + ww, wy2],
            radius=max(int(wh_px * 0.28), 1),
            fill=WIN_CLR,
        )

    # cab / driver face (right end)
    cab_w = max(int(tw * 0.09), 2)
    bd.rectangle([tx2 - cab_w, ty1, tx2, ty2], fill=CAB_CLR)

    # headlight (small yellow rectangle on cab, near bottom-right)
    hl_w = max(int(tw * 0.024), 1)
    hl_h = max(int(th * 0.20), 1)
    hl_y = ty2 - s_h - max(int(th * 0.34), 1)
    hl_x = tx2 - max(int(tw * 0.065), 1)
    bd.rectangle([hl_x, hl_y, hl_x + hl_w, hl_y + hl_h], fill=HEADLIGHT)

    # apply rounded-rect mask to train body
    bm = layer(s).convert("L")
    ImageDraw.Draw(bm).rounded_rectangle([tx1, ty1, tx2, ty2], radius=r_b, fill=255)
    body.putalpha(bm)
    img = Image.alpha_composite(img, body)

    return img


# --------------------------------------------------------------------------- #
#  Cleanup + output                                                            #
# --------------------------------------------------------------------------- #

def clean_mipmap_dirs():
    """Remove existing icon files that would conflict with our new PNGs."""
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
    print("AvgangSthlm icon generator v2")
    print("-" * 30)

    clean_mipmap_dirs()

    print("\nRendering master at 512x512 ...")
    master = draw_icon(MASTER)

    store_path = os.path.join(HERE, "ic_launcher_play_store_512.png")
    master.save(store_path, "PNG")
    print("  saved  ic_launcher_play_store_512.png")

    print("\nSaving mipmap PNGs ...")
    for dir_name, size in DENSITY_SIZES:
        out_dir = os.path.join(RES_DIR, dir_name)
        os.makedirs(out_dir, exist_ok=True)
        scaled = master.resize((size, size), Image.LANCZOS)
        for fname in ("ic_launcher.png", "ic_launcher_round.png"):
            path = os.path.join(out_dir, fname)
            scaled.save(path, "PNG")
        print(f"  {dir_name:<22} {size}x{size}")

    print("\nDone - sync Gradle and rebuild to pick up the new icons.")


if __name__ == "__main__":
    main()
