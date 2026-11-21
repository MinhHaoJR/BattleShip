"""Nhập ảnh truyện vào game: cắt về 16:9, thu về tối đa 1920x1080 (không phóng to), nén JPG chất lượng cao, đặt đúng tên file.

Cách dùng (cần Python 3 và Pillow: pip install pillow):
    python tools/nhap_anh_truyen.py anh_moi/                 # cả thư mục
    python tools/nhap_anh_truyen.py orykron_rise.png lumira.webp

Tên file ảnh nguồn phải là khoá của khung hình, có hoặc không có tiền tố "story_"
(vd. orykron_rise.png, story_necrosys.jpg). Danh sách khoá đọc thẳng từ StoryMode.java.
"""
import io
import re
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
STORY_MODE = ROOT / "src/main/java/battleship/ui/StoryMode.java"
OUT_DIR = ROOT / "src/main/resources/story"
SCENE = (1920, 1080)
PORTRAIT = (320, 320)
MAX_BYTES = 700 * 1024  # ưu tiên nét; 20 ảnh vẫn chỉ ~14 MB
IMAGE_EXT = {".png", ".jpg", ".jpeg", ".webp", ".bmp"}


def known_keys():
    keys = set(re.findall(r'= "(story_\w+)"', STORY_MODE.read_text(encoding="utf-8")))
    return keys | {"story_portrait_raskov"}


def cover(img, size):
    """Cắt giữa cho đúng tỉ lệ rồi thu nhỏ về size; ảnh nhỏ hơn thì giữ cỡ gốc (game tự co giãn khi vẽ)."""
    w, h = img.size
    tw, th = size
    if w < tw or h < th:
        k = min(w / tw, h / th)
        tw, th = int(tw * k), int(th * k)
    scale = max(tw / w, th / h)
    nw, nh = round(w * scale), round(h * scale)
    img = img.resize((nw, nh), Image.LANCZOS)
    left, top = (nw - tw) // 2, (nh - th) // 2
    return img.crop((left, top, left + tw, top + th))


def jpeg_under_limit(img):
    for q in range(95, 74, -3):
        buf = io.BytesIO()
        img.save(buf, "JPEG", quality=q, optimize=True, progressive=True)
        if buf.tell() <= MAX_BYTES:
            return buf.getvalue(), q
    return buf.getvalue(), q


def import_one(src, keys):
    key = src.stem if src.stem.startswith("story_") else "story_" + src.stem
    if key not in keys:
        return f"BỎ QUA {src.name}: không có khung hình tên '{key}'"
    img = Image.open(src).convert("RGB")
    size = PORTRAIT if key.startswith("story_portrait_") else SCENE
    notes = []
    if img.width < size[0] or img.height < size[1]:
        notes.append(f"ảnh gốc {img.width}x{img.height} nhỏ hơn {size[0]}x{size[1]}: giữ cỡ gốc, không phóng to")
    if size == SCENE and abs(img.width / img.height - 16 / 9) > 0.02:
        notes.append("không phải 16:9, đã cắt bớt ở giữa")
    data, q = jpeg_under_limit(cover(img, size))
    if len(data) > MAX_BYTES:
        notes.append(f"vẫn {len(data) // 1024} KB sau khi nén")
    (OUT_DIR / f"{key}.jpg").write_bytes(data)
    out = Image.open(io.BytesIO(data)).size
    msg = f"OK  {src.name} -> {key}.jpg ({out[0]}x{out[1]}, {len(data) // 1024} KB, chất lượng {q})"
    return msg + "".join(f"\n    lưu ý: {n}" for n in notes)


def main(args):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")  # console Windows mặc định không in được tiếng Việt
    if not args:
        print(__doc__)
        return 1
    keys = known_keys()
    files = []
    for a in map(Path, args):
        files += sorted(p for p in a.iterdir() if p.suffix.lower() in IMAGE_EXT) if a.is_dir() else [a]
    for f in files:
        print(import_one(f, keys))
    missing = sorted(k for k in keys if not (OUT_DIR / f"{k}.jpg").exists())
    if missing:
        print("Còn thiếu file:", ", ".join(missing))
    print("Nhớ xem lại ở cửa sổ nhỏ nhất (980x840): chỉ khoảng 2/3 giữa ảnh còn hiện, boss phải nằm trong đó.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
