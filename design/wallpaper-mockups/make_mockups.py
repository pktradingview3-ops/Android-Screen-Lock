"""
Generates static lock-screen wallpaper mockups (1080x2400, phone ratio).
Black background, white bold time. Text is rendered with a real font so
the preview matches what the app will draw with its own font engine.

Run:  python make_mockups.py   (needs Pillow; DejaVu Sans Bold is used)
"""
import datetime
import os

import matplotlib
from PIL import Image, ImageDraw, ImageFont

W, H = 1080, 2400
OUT = os.path.dirname(os.path.abspath(__file__))
FONT = os.path.join(os.path.dirname(matplotlib.__file__), "mpl-data", "fonts", "ttf", "DejaVuSans-Bold.ttf")

now = datetime.datetime(2026, 10, 8, 20, 7)  # sample time (matches the user's reference style)
HH, MM = now.strftime("%H"), now.strftime("%M")
DATE_LINE = now.strftime("%a, %d %b %Y").upper()  # THU, 08 OCT 2026


def font(size):
    return ImageFont.truetype(FONT, size)


def text_layer(text, size, condense=1.0, tracking=0):
    """White text on transparent layer. condense<1 squeezes, >1 stretches horizontally."""
    f = font(size)
    # measure
    tmp = ImageDraw.Draw(Image.new("RGBA", (1, 1)))
    widths = [tmp.textbbox((0, 0), ch, font=f)[2] for ch in text]
    total_w = sum(widths) + tracking * (len(text) - 1)
    bbox = tmp.textbbox((0, 0), text, font=f)
    h = bbox[3] - bbox[1] + 20
    layer = Image.new("RGBA", (int(total_w) + 20, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    x = 10
    for ch, w in zip(text, widths):
        d.text((x, 10 - bbox[1]), ch, font=f, fill=(255, 255, 255, 255))
        x += w + tracking
    if condense != 1.0:
        layer = layer.resize((max(1, int(layer.width * condense)), layer.height), Image.LANCZOS)
    return layer


def paste_center(canvas, layer, cx, cy):
    canvas.alpha_composite(layer, (int(cx - layer.width / 2), int(cy - layer.height / 2)))


def paste_left(canvas, layer, x, cy):
    canvas.alpha_composite(layer, (int(x), int(cy - layer.height / 2)))


def new_canvas():
    return Image.new("RGBA", (W, H), (0, 0, 0, 255))


def save(img, name):
    img.convert("RGB").save(os.path.join(OUT, name), "PNG", optimize=True)
    print("saved", name)


# 1. Vertical stacked, left aligned (closest to the reference layout), white only
def v1():
    c = new_canvas()
    paste_left(c, text_layer(DATE_LINE, 56, tracking=4), 120, 820)
    paste_left(c, text_layer(HH, 640), 110, 1120)
    paste_left(c, text_layer(MM, 640), 110, 1700)
    save(c, "01-vertical-stack-left.png")


# 2. Vertical stacked, centered, heavy, with a thin rule between hour and minute
def v2():
    c = new_canvas()
    paste_center(c, text_layer(HH, 560), W / 2, 1040)
    rule = Image.new("RGBA", (W - 300, 10), (255, 255, 255, 255))
    c.alpha_composite(rule, (150, 1270))
    paste_center(c, text_layer(MM, 560), W / 2, 1520)
    save(c, "02-vertical-stack-center.png")


# 3. Vertical condensed tall digits, right aligned
def v3():
    c = new_canvas()
    hh = text_layer(HH, 820, condense=0.62)
    mm = text_layer(MM, 820, condense=0.62)
    x = W - 110
    paste_left(c, hh, x - hh.width, 900)
    paste_left(c, mm, x - mm.width, 1620)
    paste_left(c, text_layer(DATE_LINE, 48, tracking=3), 110, 300)
    save(c, "03-vertical-condensed-right.png")


# 4. Horizontal single line, centered, with date above
def h1():
    c = new_canvas()
    paste_center(c, text_layer(DATE_LINE, 52, tracking=4), W / 2, 1000)
    line = text_layer(f"{HH}:{MM}", 330)
    paste_center(c, line, W / 2, 1200)
    save(c, "04-horizontal-single-line.png")


# 5. Horizontal wide (extended) with hour/minute separated by a dot, and a bottom accent rule
def h2():
    c = new_canvas()
    hh = text_layer(HH, 280, condense=1.05)
    mm = text_layer(MM, 280, condense=1.05)
    dot_size = 60
    gap = 40
    total = hh.width + gap + dot_size + gap + mm.width
    x0 = (W - total) / 2
    cy = 1200
    paste_left(c, hh, x0, cy)
    dot_x = x0 + hh.width + gap
    d = ImageDraw.Draw(c)
    d.ellipse((dot_x, cy - 10, dot_x + dot_size, cy + 50), fill=(255, 255, 255, 255))
    paste_left(c, mm, dot_x + dot_size + gap, cy)
    d.rectangle((W / 2 - 140, 1720, W / 2 + 140, 1728), fill=(255, 255, 255, 255))
    save(c, "05-horizontal-wide-dot.png")


if __name__ == "__main__":
    v1()
    v2()
    v3()
    h1()
    h2()
