#!/usr/bin/env python3
"""Generate Play Store graphics from the PEV PlugShare brand mark.

Outputs (also copies feature graphic to the gradle-play-publisher path):
  marketing/play/icon-512.png                       512x512 RGBA app icon
  marketing/play/feature-graphic-1024x500.png       1024x500 feature graphic
  app/src/main/play/listings/en-US/featureGraphic.png
  website/icon-512.png                              (replaces old 2-bit export)

Requires: rsvg-convert, Pillow. Downloads Montserrat/Open Sans (OFL) on first
run into marketing/.fonts-cache/ (gitignored).
"""
from __future__ import annotations

import io
import math
import subprocess
import sys
import urllib.request
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "marketing" / "play"
FONTS = ROOT / "marketing" / ".fonts-cache"

GREEN = (30, 130, 76)  # #1E824C brand green (matches launcher + site theme-color)
TAGLINE = (234, 245, 238)
SUBLINE = (206, 230, 215)

FONT_URLS = {
    "Montserrat.ttf": "https://github.com/google/fonts/raw/main/ofl/montserrat/Montserrat%5Bwght%5D.ttf",
    "OpenSans.ttf": "https://github.com/google/fonts/raw/main/ofl/opensans/OpenSans%5Bwdth%2Cwght%5D.ttf",
}

# Plug mark geometry, measured from the launcher icon (192x192 viewport).
PLUG_SHAPES = """
  <rect x="74" y="29" width="12" height="27" rx="2.5" fill="{mark}"/>
  <rect x="106" y="29" width="12" height="27" rx="2.5" fill="{mark}"/>
  <rect x="64" y="53" width="64" height="53" rx="5" fill="{mark}"/>
  <path d="M96.5 72 L90.5 88 L96.5 88 L92.5 105 L104 86 L98 86 Z" fill="{bolt}"/>
  <circle cx="96" cy="149" r="8.5" fill="{mark}"/>
"""

ICON_SVG = f"""<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 192 192">
  <circle cx="96" cy="96" r="96" fill="#1E824C"/>
  {PLUG_SHAPES.format(mark="#FFFFFF", bolt="#1E824C")}
</svg>
"""

RING_SVG = f"""<svg xmlns="http://www.w3.org/2000/svg" width="600" height="600" viewBox="0 0 192 192">
  <circle cx="96" cy="96" r="88" fill="none" stroke="#FFFFFF" stroke-width="13"/>
  <g transform="translate(96 96) scale(0.60) translate(-96 -93)">
    {PLUG_SHAPES.format(mark="#FFFFFF", bolt="#1E824C")}
  </g>
</svg>
"""


def fetch_fonts() -> None:
    FONTS.mkdir(parents=True, exist_ok=True)
    for name, url in FONT_URLS.items():
        path = FONTS / name
        if not path.exists():
            print(f"downloading {name}")
            urllib.request.urlretrieve(url, path)


TITLE = "PEV PlugShare"
SHORT_DESC = "Find and share charging spots for e-scooters, e-bikes and EUCs. Works offline."
FULL_DESC = """PEV PlugShare is the open-source Android app for finding and sharing charging spots for personal electric vehicles — e-scooters, e-bikes and electric unicycles (EUCs).

Whether you commute or tour on the weekend, PlugShare helps you find a power point before your battery runs out — and share the spots you discover with other riders.

FEATURES

• Browse charging locations as a list sorted by distance, with socket type, cost, access and availability visible at a glance
• Add and edit locations: socket type, number of sockets, opening hours, cost, public access and extra notes
• Take photos of charging spots — EXIF location metadata is extracted automatically
• AI-assisted descriptions: generate a clear, consistent description for any location
• Full offline support: everything works from the on-device database, no signal required
• Sync with OsmAnd and navigate turn-by-turn to a charging spot
• Optional cloud sync over OAuth 2.0 — your locations, wherever you sign in
• Favourites, categories and a full-screen photo gallery

OPEN SOURCE

The entire app lives on GitHub — no closed components. What you install is what's in the repository, and you can build it yourself.

PRIVACY

PlugShare works fully offline by default. Cloud sync is optional and uses standard OAuth 2.0.

LINKS

• Project website and direct APK download: https://pevplugshare.a42r.com
• Source code on GitHub: https://github.com/githuba42r/PEVPlugShare

Find a spot. Share a spot. Keep riding."""

# Source screenshots -> Play phone screenshots (9:16, 1080 wide, >=1000px short side)
SCREENSHOTS = [
    ("01-charging-locations.png", "locations-list.png"),
    ("02-location-details.png", "location-detail.png"),
    ("03-add-a-location.png", "add-location.png"),
    ("04-saved-and-settings.png", "drawer.png"),
]
SHOT_W, SHOT_H = 1080, 1920


def font(name: str, size: int, wght: int) -> ImageFont.FreeTypeFont:
    f = ImageFont.truetype(str(FONTS / name), size)
    try:
        f.set_variation_by_axes([wght])
    except Exception:
        try:
            f.set_variation_by_name("Bold" if wght >= 700 else "Regular")
        except Exception:
            pass
    return f


def render_svg(svg: str, width: int, height: int, ss: int = 4) -> Image.Image:
    """Render SVG at ss*x then downsample for smooth edges."""
    png = subprocess.run(
        ["rsvg-convert", "-w", str(width * ss), "-h", str(height * ss)],
        input=svg.encode(),
        capture_output=True,
        check=True,
    ).stdout
    im = Image.open(io.BytesIO(png)).convert("RGBA")
    if ss > 1:
        im = im.resize((width, height), Image.LANCZOS)
    return im


def make_icon() -> Image.Image:
    return render_svg(ICON_SVG, 512, 512)


def rounded_shadow(im: Image.Image, radius: int) -> tuple[Image.Image, tuple[int, int]]:
    """Soft drop shadow under a rounded-corner RGBA image. Returns (shadow, offset)."""
    mask = im.getchannel("A")
    shadow = Image.new("RGBA", im.size, (0, 0, 0, 0))
    solid = Image.new("RGBA", im.size, (0, 0, 0, 150))
    shadow.paste(solid, (0, 0), mask)
    shadow = shadow.filter(ImageFilter.GaussianBlur(14))
    offset = (0, 10)
    return shadow, offset


def make_feature_graphic() -> Image.Image:
    W, H = 1024, 500
    card = Image.new("RGB", (W, H), GREEN)

    # Phone screenshot (locations list), rounded, floating with a soft shadow
    shot_src = Image.open(ROOT / "website" / "images" / "locations-list.png").convert("RGB")
    # Drop the white status-bar strip at the top so the mockup starts on the app bar
    shot_src = shot_src.crop((0, 12, shot_src.width, shot_src.height))
    phone_h = 436
    phone_w = round(shot_src.width * phone_h / shot_src.height)
    phone = shot_src.resize((phone_w, phone_h), Image.LANCZOS).convert("RGBA")
    radius = 18
    mask = Image.new("L", phone.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, phone_w - 1, phone_h - 1], radius, fill=255)
    phone.putalpha(mask)
    px, py = W - phone_w - 88, 32
    shadow, (sx, sy) = rounded_shadow(phone, radius)
    card.paste(shadow, (px + sx, py + sy), shadow)
    card.paste(phone, (px, py), phone)

    draw = ImageDraw.Draw(card)

    # Ring logo (white outline + plug), matching the og-card treatment
    logo = render_svg(RING_SVG, 140, 140, ss=3)
    card.paste(logo, (64, 44), logo)

    pad = 64
    y = 220
    title_font = font("Montserrat.ttf", 56, 700)
    draw.text((pad, y), "PEV PlugShare", font=title_font, fill=(255, 255, 255))

    y += 78
    tag_font = font("OpenSans.ttf", 26, 400)
    for line in ("Find and share charging spots for", "e-scooters, e-bikes and EUCs."):
        draw.text((pad, y), line, font=tag_font, fill=TAGLINE)
        y += 36

    y += 18
    sub_font = font("OpenSans.ttf", 18, 400)
    draw.text(
        (pad, y),
        "Open-source Android app · works offline · OsmAnd sync",
        font=sub_font,
        fill=SUBLINE,
    )

    return card


def make_screenshots() -> list[Image.Image]:
    """Compose Play phone screenshots: app capture centred on white, 1080x1920."""
    out = []
    for _, src in SCREENSHOTS:
        im = Image.open(ROOT / "website" / "images" / src).convert("RGB")
        # Drop the white status-bar strip some captures start with
        top = 0
        while top < 24 and all(sum(im.getpixel((x, top))) > 740 for x in range(0, im.width, 40)):
            top += 1
        if top:
            im = im.crop((0, top, im.width, im.height))
        # Scale to width, then letterbox vertically on white
        w = SHOT_W
        h = round(im.height * w / im.width)
        im = im.resize((w, h), Image.LANCZOS)
        canvas = Image.new("RGB", (SHOT_W, SHOT_H), (255, 255, 255))
        y = (SHOT_H - h) // 2 if h < SHOT_H else 0
        if h > SHOT_H:
            im = im.crop((0, 0, w, SHOT_H))
            y = 0
        canvas.paste(im, (0, y))
        out.append(canvas)
    return out


def main() -> None:
    fetch_fonts()
    OUT.mkdir(parents=True, exist_ok=True)

    icon = make_icon()
    icon.save(OUT / "icon-512.png")

    feature = make_feature_graphic()
    feature.save(OUT / "feature-graphic-1024x500.png")

    # gradle-play-publisher picks up listings/.../featureGraphic.png automatically
    listings = ROOT / "app" / "src" / "main" / "play" / "listings" / "en-US"
    listings.mkdir(parents=True, exist_ok=True)
    feature.save(listings / "featureGraphic.png")

    # Replace the old 2-bit website export with the new render
    icon.save(ROOT / "website" / "icon-512.png")

    # Play listing texts (gradle-play-publisher picks these up automatically)
    assert len(TITLE) <= 30, f"title too long: {len(TITLE)}"
    assert len(SHORT_DESC) <= 80, f"short description too long: {len(SHORT_DESC)}"
    assert len(FULL_DESC) <= 4000, f"full description too long: {len(FULL_DESC)}"
    (listings / "title.txt").write_text(TITLE + "\n")
    (listings / "shortDescription.txt").write_text(SHORT_DESC + "\n")
    (listings / "fullDescription.txt").write_text(FULL_DESC + "\n")

    # Phone screenshots
    shots_dir = listings / "images" / "phoneScreenshots"
    shots_dir.mkdir(parents=True, exist_ok=True)
    shots = make_screenshots()
    (OUT / "screenshots").mkdir(parents=True, exist_ok=True)
    for (name, _), shot in zip(SCREENSHOTS, shots):
        shot.save(shots_dir / name)
        shot.save(OUT / "screenshots" / name)

    for p in [
        OUT / "icon-512.png",
        OUT / "feature-graphic-1024x500.png",
        listings / "featureGraphic.png",
        ROOT / "website" / "icon-512.png",
    ]:
        im = Image.open(p)
        print(f"{p.relative_to(ROOT)}: {im.size[0]}x{im.size[1]} {im.mode}")


if __name__ == "__main__":
    sys.exit(main())
