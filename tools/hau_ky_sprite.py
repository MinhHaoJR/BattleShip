"""Hậu kỳ sprite voxel render từ Blender: phóng to kiểu pixel và thêm viền tối như sprite boss cũ.

    python tools/hau_ky_sprite.py <thư_mục_render> [--copy]

Ảnh *_idle/_enrage/_stageN (96x96) -> 288x288; *_rampage (254x180) -> 762x540 -> cắt còn 760x540.
Ảnh mịn 144x144 / 380x270 phóng x2 -> 288x288 / 760x540.
--copy: chép kết quả vào src/main/resources/sprites/titans/ (rampage đổi tên thành *_rampage_voxel.png).
"""
import re
import sys
from pathlib import Path

from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
DEST = ROOT / "src/main/resources/sprites/titans"
OUTLINE = (10, 8, 16, 255)


def outline(im):
    a = im.getchannel("A").point(lambda v: 255 if v > 0 else 0)
    ring = a.filter(ImageFilter.MaxFilter(3))
    edge = Image.new("RGBA", im.size, OUTLINE)
    base = Image.new("RGBA", im.size, (0, 0, 0, 0))
    base.paste(edge, mask=ring)
    base.alpha_composite(im)
    return base


def process(src: Path):
    im = Image.open(src).convert("RGBA")
    im = outline(im)
    k = 2 if im.width in (144, 380) else 3       # sprite voxel mịn (form IV Ultima) render 144/380 rồi x2
    im = im.resize((im.width * k, im.height * k), Image.NEAREST)
    if "rampage" in src.stem and im.width == 762:
        im = im.crop((1, 0, 761, 540))
    return im


def main(args):
    if not args:
        print(__doc__)
        return 1
    folder, copy = Path(args[0]), "--copy" in args
    for src in sorted(folder.glob("*.png")):
        if not re.fullmatch(r"(spacegodzilla|destoroyah|ultima)_(idle|enrage|stage[1-4]|rampage)", src.stem):
            continue   # bỏ qua ảnh xem trước / ảnh đã hậu kỳ
        im = process(src)
        out = src.with_name(src.stem + "_final.png")
        im.save(out)
        if copy:
            name = src.stem.replace("_rampage", "_rampage_voxel") + ".png"
            im.save(DEST / name)
        print(src.name, "->", im.size)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
