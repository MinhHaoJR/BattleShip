"""Vẽ icon pixel 96x96 cho 18 chiêu của 3 boss mới (vẽ 32x32 rồi phóng x3, viền tối như icon cũ).

    python tools/tao_icon_skill.py

Ghi vào src/main/resources/sprites/titans/skill_<chiêu>.png (vd. skill_sg_corona_beam.png).
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

DEST = Path(__file__).resolve().parent.parent / "src/main/resources/sprites/titans"
N = 32

# chiêu -> (kiểu hình, màu chính, màu sáng)
SKILLS = {
    "SG_CRYSTAL_MISSILE": ("shards", "#c084fc", "#f5f3ff"),
    "SG_GRAVITY_TORNADO": ("tornado", "#8b5cf6", "#ddd6fe"),
    "SG_CORONA_BEAM": ("spiral", "#38bdf8", "#e0f2fe"),
    "SG_CRYSTAL_FORTRESS": ("pillars", "#e879f9", "#fdf4ff"),
    "SG_COSMIC_NOVA": ("nova", "#f0abfc", "#ffffff"),
    "DS_MICRO_OXYGEN": ("beam", "#f472b6", "#fdf2f8"),
    "DS_HORN_SLASH": ("slash", "#fbbf24", "#fffbeb"),
    "DS_SWARM_SPLIT": ("swarm", "#ef4444", "#fecaca"),
    "DS_OXYGEN_MIST": ("mist", "#7f1d1d", "#f87171"),
    "DS_DESTRUCTION_OMEGA": ("nova", "#fde047", "#fff7ed"),
    "UL1_GILL_SPRAY": ("vspray", "#ff7b39", "#ffe14d"),
    "UL1_FIN_DIVE": ("finwake", "#e2ccd2", "#e0f2fe"),
    "UL1_FLUKE_GEYSER": ("geyser", "#7dd3fc", "#ffffff"),
    "UL2_RAMPAGE_CHARGE": ("charge", "#b4471c", "#ffcf4a"),
    "UL2_LAVA_VENTS": ("lavavent", "#ff7a1a", "#fde047"),
    "UL2_SCALE_SHRAPNEL": ("scales", "#cf6a32", "#ff7a1a"),
    "UL3_CLAW_REND": ("claws", "#5eead4", "#ffffff"),
    "UL3_SONIC_ROAR": ("sonic", "#7ff5ff", "#ffffff"),
    "UL3_SPINE_LANCE": ("lances", "#c3c8d2", "#7ff5ff"),
    "UL4_CRIMSON_BREATH": ("breath", "#ff3b30", "#fde047"),
    "UL4_SKYFIRE": ("skyfire", "#ff5a36", "#ffe4e1"),
    "UL4_FINAL_ROAR": ("diamond", "#ef4444", "#ffe4e1"),
    "UL1_RIPTIDE_COIL": ("whirl", "#38bdf8", "#e0f2fe"),
    "UL1_SCALD_TORRENT": ("steam", "#ffb36b", "#fff7ed"),
    "UL2_MAGMA_QUAKE": ("magring", "#ff5a1f", "#fde047"),
    "UL2_MOLTEN_STAMPEDE": ("stomp", "#e8552a", "#fde047"),
    "UL3_GLACIER_BREATH": ("icebeam", "#67e8f9", "#ffffff"),
    "UL3_FROST_CROWN": ("icering", "#a5f3fc", "#ffffff"),
    "UL4_TAIL_CRUSH": ("tailarc", "#b91c1c", "#fca5a5"),
    "UL4_BLOOD_SPIRAL": ("helix", "#dc2626", "#ffe4e1"),
}


def diamond(d, cx, cy, w, h, fill, core):
    d.polygon([(cx, cy - h), (cx + w, cy), (cx, cy + h), (cx - w, cy)], fill=fill)
    d.polygon([(cx, cy - h * 0.6), (cx + w * 0.4, cy), (cx, cy + h * 0.6), (cx - w * 0.4, cy)], fill=core)


def draw(kind, c, hot):
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if kind == "shards":
        for cx, cy in ((9, 10), (20, 7), (15, 19), (25, 20)):
            diamond(d, cx, cy, 3, 6, c, hot)
    elif kind == "tornado":
        for i in range(7):
            w = 13 - i * 1.6
            d.ellipse((16 - w, 4 + i * 3.6, 16 + w, 8 + i * 3.6), outline=c if i % 2 else hot, width=2)
    elif kind in ("spiral", "beam"):
        d.rectangle((3, 13, 29, 18), fill=c)
        d.rectangle((3, 15, 29, 16), fill=hot)
        if kind == "spiral":
            for x in range(3, 30, 2):
                y = 15.5 + math.sin(x * 0.7) * 6
                d.rectangle((x, y - 1, x + 1, y), fill=hot)
        d.ellipse((1, 11, 8, 20), fill=hot)
    elif kind == "pillars":
        for cx, h in ((8, 10), (16, 14), (24, 9)):
            d.polygon([(cx - 4, 28), (cx - 3, 28 - h), (cx, 28 - h - 4), (cx + 3, 28 - h), (cx + 4, 28)], fill=c)
            d.line([(cx, 27), (cx, 28 - h - 2)], fill=hot, width=1)
    elif kind == "nova":
        for i in range(12):
            a = i * math.pi / 6
            d.line([(16, 16), (16 + math.cos(a) * 15, 16 + math.sin(a) * 15)], fill=c if i % 2 else hot, width=2)
        d.ellipse((9, 9, 23, 23), fill=c)
        d.ellipse((12, 12, 20, 20), fill=hot)
    elif kind == "slash":
        d.arc((2, 2, 30, 30), 200, 340, fill=c, width=5)
        d.arc((4, 4, 28, 28), 210, 330, fill=hot, width=2)
    elif kind == "swarm":
        for cx, cy in ((7, 8), (19, 6), (12, 16), (24, 15), (8, 25), (20, 25)):
            d.rectangle((cx - 3, cy - 2, cx + 3, cy + 2), fill=c)
            d.rectangle((cx - 1, cy - 3, cx + 1, cy - 2), fill=hot)
            d.point([(cx - 4, cy + 2), (cx + 4, cy + 2)], fill=c)
    elif kind == "mist":
        for cx, cy, r in ((11, 18, 8), (20, 14, 9), (16, 22, 7)):
            d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=c)
        d.ellipse((13, 13, 19, 19), fill=hot)
    elif kind == "bite":
        d.pieslice((2, 4, 30, 28), 20, 340, fill=c)
        for x in range(18, 30, 4):
            d.polygon([(x, 9), (x + 2, 13), (x + 4, 9)], fill=hot)
            d.polygon([(x, 23), (x + 2, 19), (x + 4, 23)], fill=hot)
    elif kind == "wave":
        for k in range(3):
            d.arc((2 + k * 3, 6 + k * 5, 30, 34 + k * 5), 200, 320, fill=c if k % 2 else hot, width=3)
    elif kind == "magma":
        d.rectangle((2, 22, 30, 29), fill=c)
        for cx, cy, r in ((9, 17, 3), (17, 12, 4), (24, 18, 3)):
            d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=hot)
    elif kind == "vspray":
        for sx in (-1, 1):
            for k in range(6):
                x, y = 6 + k * 4, 16 + sx * k * 2.2
                d.rectangle((x - 2, y - 2, x + 2, y + 2), fill=hot if k < 2 else c)
        d.rectangle((2, 13, 7, 19), fill=c)
    elif kind == "finwake":
        d.line([(2, 24), (9, 18), (16, 24), (23, 18), (30, 24)], fill=hot, width=3)
        for x in (8, 14, 20):
            d.polygon([(x - 3, 20), (x, 8), (x + 3, 20)], fill=c)
    elif kind == "geyser":
        d.rectangle((12, 6, 20, 28), fill=c)
        d.rectangle((14, 6, 18, 28), fill=hot)
        d.ellipse((8, 2, 24, 12), fill=hot)
        d.rectangle((4, 26, 28, 30), fill=c)
    elif kind == "charge":
        d.polygon([(4, 16), (22, 8), (30, 16), (22, 24)], fill=c)
        d.polygon([(22, 10), (31, 6), (26, 14)], fill=hot)
        for x in (4, 9, 14):
            d.rectangle((x - 3, 26, x, 29), fill="#8b6b4a")
    elif kind == "lavavent":
        d.rectangle((2, 24, 30, 30), fill="#3a1208")
        for cx in (9, 22):
            d.rectangle((cx - 2, 8, cx + 2, 25), fill=c)
            d.rectangle((cx - 1, 4, cx + 1, 10), fill=hot)
    elif kind == "scales":
        for cx, cy, a in ((9, 10, 0), (21, 9, 1), (15, 20, 0), (25, 22, 1)):
            box = (cx - 5, cy - 3, cx + 5, cy + 3) if a == 0 else (cx - 3, cy - 5, cx + 3, cy + 5)
            d.rectangle(box, fill=c)
            d.line([(box[0], cy), (box[2], cy)], fill=hot)
    elif kind == "claws":
        for k in range(3):
            d.line([(6 + k * 8, 4), (12 + k * 8, 28)], fill=c, width=4)
            d.line([(6 + k * 8, 4), (12 + k * 8, 28)], fill=hot, width=1)
    elif kind == "sonic":
        for k in range(3):
            d.arc((2 - k * 4, 4 + k * 2, 22 + k * 8, 28 - k * 2), 300, 60, fill=c if k % 2 else hot, width=3)
        d.ellipse((2, 13, 8, 19), fill=hot)
    elif kind == "lances":
        for a0 in (45, 135, 225, 315):
            rad = math.radians(a0)
            x1, y1 = 16 + math.cos(rad) * 14, 16 + math.sin(rad) * 14
            d.line([(16, 16), (x1, y1)], fill=c, width=4)
            d.line([(16, 16), (x1, y1)], fill=hot, width=1)
    elif kind == "breath":
        d.polygon([(2, 14), (30, 6), (30, 26), (2, 18)], fill=c)
        d.polygon([(2, 15), (30, 12), (30, 20), (2, 17)], fill=hot)
    elif kind == "skyfire":
        for x in (7, 16, 25):
            d.rectangle((x - 2, 0, x + 2, 26), fill=c)
            d.rectangle((x - 1, 0, x + 1, 26), fill=hot)
            d.ellipse((x - 4, 24, x + 4, 31), fill=c)
    elif kind == "diamond":
        for r in (14, 9, 4):
            d.polygon([(16, 16 - r), (16 + r, 16), (16, 16 + r), (16 - r, 16)], outline=c if r != 9 else hot, width=2)
        d.rectangle((14, 14, 18, 18), fill=hot)
    elif kind == "whirl":
        for arm in range(3):
            for k in range(14):
                u = k / 13
                a = arm * 2.094 + u * 4.6
                r = 13 * (1 - u) + 1
                d.rectangle((16 + math.cos(a) * r - 1, 16 + math.sin(a) * r * 0.7 - 1, 16 + math.cos(a) * r + 1, 16 + math.sin(a) * r * 0.7 + 1),
                            fill=hot if k % 3 == 0 else c)
    elif kind == "steam":
        for y0 in (10, 20):
            d.rectangle((3, y0 - 2, 29, y0 + 2), fill=c)
            for x in range(6, 30, 6):
                d.ellipse((x - 3, y0 - 8, x + 3, y0 - 2), fill=hot)
    elif kind == "magring":
        d.ellipse((3, 7, 29, 25), outline="#3a1208", width=4)
        d.ellipse((4, 8, 28, 24), outline=c, width=2)
        for a in range(0, 360, 45):
            r = math.radians(a)
            d.line([(16, 16), (16 + math.cos(r) * 11, 16 + math.sin(r) * 7)], fill=c, width=1)
        d.rectangle((14, 4, 18, 14), fill=hot)
    elif kind == "stomp":
        for i, (x, y) in enumerate(((5, 22), (12, 12), (19, 22), (26, 12))):
            d.ellipse((x - 4, y - 3, x + 4, y + 3), fill="#2a0e05", outline=c)
            d.point([(x - 2, y - 4), (x, y - 4), (x + 2, y - 4)], fill=hot)
    elif kind == "icebeam":
        d.polygon([(2, 14), (24, 12), (30, 6), (30, 26), (24, 20), (2, 18)], fill=c)
        d.rectangle((2, 15, 28, 17), fill=hot)
        for x in (12, 20):
            d.polygon([(x - 2, 12), (x, 4), (x + 2, 12)], fill=hot)
    elif kind == "icering":
        for i in range(8):
            a = i * math.pi / 4
            x, y = 16 + math.cos(a) * 11, 18 + math.sin(a) * 9
            d.polygon([(x - 2, y + 3), (x, y - 7), (x + 2, y + 3)], fill=c if i % 2 else hot)
        d.ellipse((13, 15, 19, 21), fill=hot)
    elif kind == "tailarc":
        d.arc((2, 4, 30, 34), 190, 350, fill="#450a0a", width=6)
        d.arc((2, 4, 30, 34), 190, 350, fill=c, width=3)
        for x, y in ((26, 9), (22, 6), (17, 5)):
            d.polygon([(x - 2, y + 2), (x, y - 4), (x + 2, y + 2)], fill=hot)
    elif kind == "helix":
        for x in range(2, 24):
            y1 = 16 + math.sin(x * 0.55) * 6
            y2 = 16 - math.sin(x * 0.55) * 6
            d.rectangle((x, y1 - 1, x + 1, y1 + 1), fill=c)
            d.rectangle((x, y2 - 1, x + 1, y2 + 1), fill="#450a0a")
        for dx, dy in ((0, -6), (6, 0), (0, 6), (-6, 0)):
            d.line([(26, 16), (26 + dx, 16 + dy)], fill=hot, width=3)
    elif kind == "halo":
        for k, cc in enumerate(("#ef4444", "#f59e0b", "#22d3ee", "#a855f7")):
            r = 14 - k * 3
            d.ellipse((16 - r, 16 - r, 16 + r, 16 + r), outline=cc, width=2)
        d.ellipse((13, 13, 19, 19), fill=hot)
    return im


def outline(im):
    a = im.getchannel("A").point(lambda v: 255 if v > 0 else 0)
    ring = a.filter(ImageFilter.MaxFilter(3))
    base = Image.new("RGBA", im.size, (0, 0, 0, 0))
    base.paste(Image.new("RGBA", im.size, (12, 10, 20, 255)), mask=ring)
    base.alpha_composite(im)
    return base


def main():
    for key, (kind, c, hot) in SKILLS.items():
        im = outline(draw(kind, c, hot)).resize((N * 3, N * 3), Image.NEAREST)
        im.save(DEST / f"skill_{key.lower()}.png")
        print("ghi", f"skill_{key.lower()}.png")


if __name__ == "__main__":
    main()
