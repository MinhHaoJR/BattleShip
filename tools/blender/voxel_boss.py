"""Dựng sprite voxel cho 3 boss mới trong Blender (chạy bên trong Blender, cần numpy có sẵn của Blender).

Mỗi con là một lưới khối 3D (numpy) -> một mesh tô màu theo mặt -> render Workbench trực giao,
nền trong suốt, nhìn chéo 3/4, quay mặt sang trái (giống godzilla_idle.png). Hậu kỳ (phóng to x3,
viền tối) làm ở tools/hau_ky_sprite.py.

Dùng: trong Blender chạy exec(open(<file>).read()); render_all(OUT_DIR)
"""
import math

import bpy
import bmesh
import numpy as np

GX, GY, GZ = 76, 52, 62          # lưới: X = hướng mặt (đầu ở +X, tới x=35 để mõm form I/II không bị cắt), Y = ngang (±26 cho sải cánh), Z = cao (tới 61 cho sừng, cánh)
OX, OY = 40, 26                  # gốc toạ độ trong lưới
PAL = {}                         # tên màu -> (chỉ số, rgb)


def col(name, hexv=None):
    if name not in PAL:
        h = hexv.lstrip("#")
        srgb = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
        lin = tuple(c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in srgb)
        PAL[name] = (len(PAL) + 1, lin)   # Blender hiểu màu thuộc tính là linear
    return PAL[name][0]


class Vox:
    def __init__(self, scale=1.0):
        """scale > 1: cùng kích thước mô hình nhưng voxel nhỏ hơn (mịn hơn), toạ độ vẫn tính theo đơn vị mô hình."""
        self.s, self.ox, self.oy = scale, int(OX * scale), int(OY * scale)
        n = (int(GX * scale), int(GY * scale), int(GZ * scale))
        self.g = np.zeros(n, dtype=np.int16)
        x, y, z = np.meshgrid((np.arange(n[0]) - self.ox) / scale, (np.arange(n[1]) - self.oy) / scale,
                              np.arange(n[2]) / scale, indexing="ij")
        self.x, self.y, self.z = x, y, z

    def put(self, mask, c, only_empty=False, only_filled=False):
        if only_empty:
            mask = mask & (self.g == 0)
        if only_filled:
            mask = mask & (self.g != 0)
        self.g[mask] = c

    def ell(self, cx, cy, cz, rx, ry, rz, c, **kw):
        m = ((self.x - cx) / rx) ** 2 + ((self.y - cy) / ry) ** 2 + ((self.z - cz) / rz) ** 2 <= 1
        self.put(m, c, **kw)

    def capsule(self, p0, p1, r0, r1, c, **kw):
        p0, p1 = np.array(p0, float), np.array(p1, float)
        d = p1 - p0
        L2 = max(1e-6, d @ d)
        t = ((self.x - p0[0]) * d[0] + (self.y - p0[1]) * d[1] + (self.z - p0[2]) * d[2]) / L2
        t = np.clip(t, 0, 1)
        px, py, pz = p0[0] + t * d[0], p0[1] + t * d[1], p0[2] + t * d[2]
        r = r0 + (r1 - r0) * t
        m = (self.x - px) ** 2 + (self.y - py) ** 2 + (self.z - pz) ** 2 <= r ** 2
        self.put(m, c, **kw)

    def chain(self, pts, r0, r1, c, **kw):
        n = len(pts) - 1
        for i in range(n):
            self.capsule(pts[i], pts[i + 1], r0 + (r1 - r0) * i / n, r0 + (r1 - r0) * (i + 1) / n, c, **kw)

    def spike(self, base, direction, h, r, c, **kw):
        d = np.array(direction, float)
        d /= np.linalg.norm(d)
        tip = np.array(base, float) + d * h
        self.capsule(base, tip, r, 0.35, c, **kw)

    def box(self, x0, x1, y0, y1, z0, z1, c, **kw):
        m = (self.x >= x0) & (self.x <= x1) & (self.y >= y0) & (self.y <= y1) & (self.z >= z0) & (self.z <= z1)
        self.put(m, c, **kw)

    def mirror_y(self, fn):
        fn(1)
        fn(-1)


# ---------------- khung kaiju chung ----------------

def kaiju_biped(v, body, belly, dark, claw, eye, *, bulk=1.0, head_len=1.0, tail_curve=4, lean=0.0):
    """Kaiju đứng 2 chân kiểu phim quái vật: thân to, chân cột, tay ngắn, đầu nhỏ, đuôi dài."""
    b = bulk
    v.ell(-1, 0, 15, 7.5 * b, 7 * b, 10, body)                       # thân
    v.ell(0, 0, 23, 6 * b, 6 * b, 6, body)                           # ngực
    v.ell(3, 0, 16, 3.5, 5 * b, 8, belly, only_filled=False)         # bụng
    v.box(4, 9, -3, 3, 9, 22, belly, only_filled=True)
    for s in (1, -1):
        v.ell(0, s * 5.5 * b, 7, 4.2 * b, 3.6, 6, body)              # đùi
        v.box(-2, 4, s * 5.5 * b - 3, s * 5.5 * b + 3, 0, 4, dark)   # bàn chân
        for k in (-1, 1):
            v.spike((4, s * 5.5 * b + k * 1.6, 1), (1, 0, -0.1), 2.5, 0.9, claw)
        v.capsule((3, s * 6.2 * b, 22), (7, s * 6.5 * b, 18), 1.8, 1.4, body)   # tay
        v.spike((7.5, s * 6.5 * b, 18), (1, 0, -0.6), 2.2, 0.7, claw)
    hx = 9 * head_len
    v.capsule((3 + lean, 0, 25), (hx - 2, 0, 29), 4.2 * b, 3.2, body)    # cổ
    v.ell(hx, 0, 30, 4.5 * head_len, 3.2, 3.0, body)                  # sọ
    v.ell(hx + 2.5, 0, 28.6, 3.0 * head_len, 2.6, 1.6, dark)         # hàm
    for s in (1, -1):
        v.box(int(hx + 1), int(hx + 2), s * 2 - (0 if s > 0 else 0), s * 2, 31, 31, eye)
        v.box(int(hx + 1), int(hx + 2), s * 3, s * 3, 31, 31, eye)
    v.box(int(hx + 3), int(hx + 5), -2, 2, 29, 29, claw)              # răng
    tail = [(-6, 0, 10), (-13, 0, 6), (-20, tail_curve * 0.4, 3), (-27, tail_curve, 2), (-32, tail_curve * 1.6, 2)]
    v.chain(tail, 5.5 * b, 1.4, body)
    return tail


def dorsal(v, pts, heights, c_out, c_in, tilt=(-0.35, 0, 1)):
    """Hàng gai lưng 3 dãy, gai giữa cao nhất."""
    for (x, y, z), h in zip(pts, heights):
        v.spike((x, y, z), tilt, h, max(1.0, h * 0.28), c_out)
        v.spike((x, y, z + 1), tilt, h * 0.7, max(0.8, h * 0.18), c_in)
        for s in (1, -1):
            v.spike((x + 0.5, y + s * 1.8, z - 1), (tilt[0], s * 0.35, 1), h * 0.6, max(0.8, h * 0.2), c_out)


def back_line(x0, x1, n, zf):
    return [(x0 + (x1 - x0) * i / (n - 1), 0, zf(x0 + (x1 - x0) * i / (n - 1))) for i in range(n)]


# ---------------- 3 boss ----------------

def crystal(v, base, d, h, r, cols):
    """Trụ pha lê nhọn chuyển màu theo đoạn: cols = (gốc, giữa, đỉnh)."""
    base, d = np.array(base, float), np.array(d, float) / np.linalg.norm(d)
    for (f0, f1, r0, r1), c in zip(((0, 0.35, r, r * 0.8), (0.35, 0.6, r * 0.8, r * 0.55), (0.6, 1.0, r * 0.55, 0.3)), cols):
        v.capsule(base + d * h * f0, base + d * h * f1, r0, r1, c)


def spacegodzilla(enraged=False):
    """Theo bản thiết kế voxel của Hảo: thân xanh navy đậm, bụng ngực đỏ rượu chia đốt, hai cụm trụ pha lê lớn trên vai
    (gốc tím đỏ -> hồng -> đỉnh trắng xanh), vương miện pha lê vàng cam trên đầu, mắt đỏ, hàm há răng trắng, đốm xanh
    phát sáng trên vai, vuốt trắng, đuôi có hàng pha lê trắng và mũi pha lê tím. enraged: pha lê chuyển xanh lam rực."""
    E = "E" if enraged else ""
    v = Vox(1.6)
    body, body2, dark = col("sg_body", "#24305a"), col("sg_body2", "#18213f"), col("sg_dark", "#0c1124")
    belly, bline = col("sg_belly", "#8c2a45"), col("sg_bline", "#5e1a2e")
    claw, gum = col("sg_claw", "#eef2ff"), col("sg_gum", "#4a0f22")
    eye, mouth = col("sg_eyeglow" + E, "#ff3b3b" if not enraged else "#ff8a8a"), col("sg_mouthglow", "#c2185b")
    xb = col("sg_xtal" + E, "#8b2a6b" if not enraged else "#1d4ed8")                 # gốc pha lê (có bóng)
    xm = col("sg_cr_mid" + E, "#e07ab8" if not enraged else "#38bdf8")                # giữa pha lê (phát sáng)
    xt = col("sg_cr_tip" + E, "#e6f0ff" if not enraged else "#e0faff")                # đỉnh pha lê
    xp = col("sg_cr_purple" + E, "#a855f7" if not enraged else "#22d3ee")
    gold, gold2 = col("sg_crown", "#f5a524"), col("sg_cr_crown", "#ffd27a")
    spot = col("sg_glowspot" + E, "#60a5fa" if not enraged else "#a5f3fc")
    for s in (1, -1):
        v.ell(-2, s * 7.5, 11, 7, 5.5, 9, body)                                    # đùi cột to
        v.capsule((-1, s * 8.5, 6), (1, s * 9, 2), 4.4, 3.8, body)                # cẳng chân
        v.ell(4, s * 9, 1.4, 5.5, 3.8, 2, body)                                    # bàn chân
        for k in (-1.5, 0, 1.5):
            v.spike((8.5, s * 9 + k * 1.2, 1.2), (1, k * 0.15, -0.15), 3.2, 1.1, claw)   # móng chân trắng
    v.ell(-2, 0, 18, 9.5, 9, 11.5, body)                                           # thân
    v.ell(2, 0, 26, 8, 8.5, 7.5, body)                                             # ngực rộng
    v.capsule((6.5, 0, 7), (8.5, 0, 30), 4.6, 4.2, belly, only_filled=True)       # bụng ngực đỏ rượu
    v.capsule((7.5, 0, 7), (9.5, 0, 30), 4.6, 4.2, belly, only_filled=True)
    stripes(v, belly, bline, "z", 3)
    for s in (1, -1):
        v.ell(1, s * 8, 29, 5, 4.5, 4.5, body)                                     # vai u
        v.capsule((2, s * 9, 27), (6, s * 12.5, 22), 3.1, 2.7, body)              # tay giang ra
        v.capsule((6, s * 12.5, 22), (10.5, s * 12.5, 25.5), 2.7, 2.2, body)
        for k in range(4):
            sp = -0.45 + k * 0.3
            v.spike((11, s * (12.5 + sp * 2), 25.5 + sp), (1, s * sp, 0.3 + sp * 0.5), 3.6 - abs(sp), 0.8, claw)   # vuốt trắng
    v.capsule((2, 0, 29), (6, 0, 34), 5.5, 4.8, body)                              # cổ
    # đầu dựng riêng rồi xoay nghiêng: hàm há răng trắng, mắt đỏ, vương miện pha lê vàng cam
    hv = Vox(1.6)
    gz_head(hv, (8, 0, 37), 0.85, 8, body, body2, dark, gum, mouth, eye, claw)
    for s in (1, -1):
        hv.ell(10.8, s * 3.3, 38.6, 1.2, 0.9, 0.7, eye)                            # mắt đỏ to
    for d, h in (((0, 0, 1), 4.5), ((0.5, 0.5, 1), 3.2), ((0.5, -0.5, 1), 3.2), ((-0.5, 0.6, 1), 3.4),
                 ((-0.5, -0.6, 1), 3.4), ((0.9, 0, 0.7), 2.8)):
        crystal(hv, (8.5, 0, 40.2), d, h, 1.1, (gold, gold, gold2))               # vương miện pha lê
    merge_yawed(v, hv, (5, 0, 33), -30)
    tail = [(-9, 0, 11), (-17, 0, 6), (-25, 4, 4), (-32, 9, 5), (-36, 13, 9), (-37, 16, 14)]
    v.chain(tail, 6.5, 1.2, body)
    rng = np.random.default_rng(5)
    v.g[(v.g == body) & (rng.random(v.g.shape) < 0.1)] = body2                     # vân vảy sẫm
    v.g[(v.g == body) & (np.floor(v.x + v.z * 2) % 7 == 0) & (rng.random(v.g.shape) < 0.5)] = body2   # thớ cơ chéo
    for s in (1, -1):                                                               # hai cụm trụ pha lê lớn trên vai
        crystal(v, (0, s * 8.5, 31), (-0.2, s * 0.45, 1), 18, 3.6, (xb, xm, xt))
        crystal(v, (-3, s * 7, 30), (-0.45, s * 0.35, 1), 13, 2.6, (xb, xm, xt))
        crystal(v, (2.5, s * 10, 29), (0.15, s * 0.7, 1), 9, 2.0, (xb, xm, xt))
        crystal(v, (-1, s * 10.5, 28), (-0.2, s * 0.9, 0.8), 7, 1.6, (xb, xp, xt))
        for k in range(3):
            v.ell(3 - k * 2, s * 9.8, 27 - k * 1.5, 0.9, 0.6, 0.9, spot)           # đốm xanh phát sáng trên vai
    for (x, z, h) in ((-8, 32, 9), (-10.5, 28, 12), (-12, 23, 11), (-13, 18, 10), (-13.5, 13, 8)):
        crystal(v, (x, 0, z), (-0.45, 0, 1), h, 1.6, (xb, xm, xt))                 # pha lê dọc lưng
        for s in (1, -1):
            crystal(v, (x + 0.5, s * 3, z - 1), (-0.45, s * 0.5, 1), h * 0.6, 1.1, (xb, xp, xt))
    for i, p in enumerate(tail[1:-1]):
        crystal(v, (p[0], p[1], p[2] + 4.5 - i * 0.6), (-0.3, 0.1, 1), 4.2 - i * 0.6, 1.0, (claw, xt, xt))   # pha lê trắng dọc đuôi
    for d in ((0.2, 0.3, 1), (-0.4, 0.6, 0.7), (0.5, 0.8, 0.6)):
        crystal(v, tail[-1], d, 4, 1.0, (xp, xp, xt))                              # mũi đuôi pha lê tím
    return v


def merge_yawed(v, h, pivot, yaw):
    """Chép các voxel của lưới h vào v sau khi xoay quanh trục đứng qua pivot một góc yaw (độ, âm = quay về -Y)."""
    c, sn = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    dx, dy = v.x - pivot[0], v.y - pivot[1]
    sx, sy = c * dx + sn * dy + pivot[0], -sn * dx + c * dy + pivot[1]      # xoay ngược để lấy mẫu từ h
    ix, iy = np.rint(sx * h.s).astype(int) + h.ox, np.rint(sy * h.s).astype(int) + h.oy
    iz = np.rint(v.z * h.s).astype(int)
    ok = (ix >= 0) & (ix < h.g.shape[0]) & (iy >= 0) & (iy < h.g.shape[1]) & (iz < h.g.shape[2])
    val = np.zeros_like(v.g)
    val[ok] = h.g[ix[ok], iy[ok], iz[ok]]
    v.g[val > 0] = val[val > 0]


def destoroyah(enraged=False):
    """Theo bản thiết kế voxel của Hảo: thân tím đen phủ vảy đỏ thẫm, cánh dơi đỏ lớn khung xương tối mép răng cưa,
    lõi ngực hình hoa vàng cam 8 cánh, sừng vàng cam trên trán, mào gai đỏ, vuốt ngà ở tay - gối - chân, đuôi dài có gai.
    enraged: lõi ngực và màng cánh rực sáng hơn."""
    E = "E" if enraged else ""
    v = Vox(1.6)
    body, body2, dark = col("ds_body", "#45304a"), col("ds_body2", "#2f2133"), col("ds_dark", "#1a1018")
    sc, sc2 = col("ds_scale", "#a3172b"), col("ds_scale2", "#d42a2a")
    claw, eye, horn = col("ds_claw", "#f1e3c8"), col("ds_eyeglow", "#ff9a1a"), col("ds_horn", "#ff8a1f")
    horn_tip = col("ds_horntip", "#ffe6b0")
    gum, mouth = col("ds_gum", "#5a0c18"), col("ds_mouthglow", "#e0302a")
    core = col("ds_core" + E, "#ffc21a" if not enraged else "#fff36b")
    core2 = col("ds_core2" + E, "#ff8c1a" if not enraged else "#ffd23f")
    memb = col("ds_wing" + E, "#c81e1e" if not enraged else "#ef4444")
    vein = col("ds_vein" + E, "#8e1420" if not enraged else "#b91c1c")
    bone = col("ds_bone", "#3b2236")
    # dáng đứng thẳng nghênh chiến: thân dựng đứng ưỡn ngực, đầu ngẩng cao trên vai, hai tay giơ vuốt, cánh dang cao
    for s in (1, -1):                                                              # chân khuỵu gối dạng rộng, chắc
        v.ell(-2, s * 7.5, 15, 6, 5, 6.5, body)                                   # hông - đùi trên
        v.capsule((-2, s * 7.5, 15), (3.5, s * 9.5, 9), 5, 4.2, body)            # đùi chĩa tới gối
        v.capsule((3.5, s * 9.5, 9), (0.5, s * 10, 3), 4, 3.3, sc)               # ống chân gập về sau (đỏ thẫm)
        v.ell(3.5, s * 10, 1.4, 5.2, 3.8, 1.9, sc)                                # bàn chân
        for k in (-1, 0, 1):
            v.spike((8, s * 10 + k * 1.7, 1.2), (1, k * 0.25, -0.15), 3.4, 1.1, claw)   # móng chân
        v.spike((6.5, s * 9.5, 9.5), (1, s * 0.15, -0.25), 5, 1.8, claw)        # gai ngà to ở gối
        v.spike((4, s * 10.5, 6), (1, s * 0.3, -0.5), 2.8, 1.0, claw)           # gai ống chân
        v.spike((2.5, s * 11, 3.5), (0.8, s * 0.6, -0.3), 2.4, 0.9, claw)       # gai cổ chân
        v.spike((-1, s * 10.8, 4), (-0.3, s * 1, -0.2), 2.2, 0.9, claw)
    v.capsule((-3, 0, 13), (-1, 0, 30), 8.5, 7.5, body)                           # thân dựng đứng
    v.ell(1, 0, 29, 7, 8, 7, body)                                                # ngực ưỡn
    for s in (1, -1):
        v.ell(0, s * 8, 32, 4.5, 4, 4, body)                                      # vai
        for k in range(3):
            v.spike((-2 + k * 1.5, s * (8.5 + k * 0.3), 35), (-0.3, s * 0.5, 1), 4 - k, 1.2, sc2)   # gai vai đỏ
        # tay co khuỷu, đưa ra trước ngực, bàn tay xoè 4 vuốt ngà dài như sắp chộp
        v.capsule((0, s * 9.5, 31), (3, s * 13.5, 24.5), 3.0, 2.6, body)         # cánh tay trên, khuỷu giang ra
        v.capsule((3, s * 14, 24.5), (10, s * 13.5, 28.5), 2.6, 2.1, sc)     # cẳng tay đỏ thẫm chĩa tới trước
        v.ell(10.8, s * 13.3, 28.8, 1.9, 2.1, 1.7, sc)                            # bàn tay
        v.spike((2, s * 14.5, 24), (-1, s * 0.4, -0.3), 2.8, 1.0, sc2)          # gai khuỷu
        for k in range(4):
            sp = -0.45 + k * 0.3
            v.spike((11.5, s * (13.3 + sp * 2.2), 28.8 + sp), (1, s * sp, 0.25 + sp * 0.5), 5 - abs(sp) * 1.5, 0.85, claw)
    v.capsule((0, 0, 33), (3, 0, 40), 5, 4.4, body)                               # cổ dựng
    hv = Vox(1.6)                                                                 # đầu dựng riêng rồi xoay nghiêng
    gz_head(hv, (5, 0, 42), 0.85, 15, body, body2, dark, gum, mouth, eye, claw)
    # mặt theo bản thiết kế khuôn mặt: sừng mõm lớn dựng cao chuyển đỏ -> cam -> kem, sừng lưỡi liềm hai bên,
    # nanh ngà cong ở khoé miệng, viền gai đỏ tua tủa quanh đầu, mắt cam dữ
    # mũi theo bản thiết kế: mõm thuôn dài, sống mũi vảy gai đỏ xếp lớp, nếp nhăn gầm hai bên, lỗ mũi ở chóp,
    # sừng mọc từ phía trước sống mũi chĩa chéo tới - lên, gốc sừng có gai răng cưa
    hv.capsule((9, 0, 45.2), (15.5, 0, 44.4), 2.4, 1.5, body)                     # sống mũi kéo dài, thuôn
    for i, t in enumerate(np.linspace(0, 1, 6)):
        x, z = 8 + 5 * t, 47 - 0.9 * t
        hv.spike((x, 0, z), (-0.5, 0, 1), 1.9 - 0.6 * t, 0.8, sc2 if i % 2 else sc)   # vảy gai sống mũi
    for s in (1, -1):
        for k in range(3):                                                         # nếp nhăn gầm hai bên mõm
            hv.capsule((9 + k * 1.6, s * 3.0, 44.6 - k * 0.2), (10.5 + k * 1.6, s * 2.4, 42.6 - k * 0.2), 0.45, 0.4, sc2,
                      only_filled=True)
        hv.ell(15.2, s * 1.1, 44.0, 0.6, 0.5, 0.5, dark)                            # lỗ mũi
    hb, hd = np.array((12.8, 0, 46.2)), np.array((0.5, 0, 1)) / np.linalg.norm((0.5, 0, 1))
    for (f0, f1, r0, r1, c) in ((0, 0.4, 2.4, 1.8, sc2), (0.4, 0.75, 1.8, 1.0, horn), (0.75, 1.0, 1.0, 0.3, horn_tip)):
        hv.capsule(hb + hd * 13 * f0, hb + hd * 13 * f1, r0, r1, c)
    for s in (1, -1):
        hv.spike(hb + (-0.5, s * 1.6, -0.3), (-0.3, s * 0.8, 0.7), 2.2, 0.8, sc2)   # gai răng cưa ở gốc sừng
    for s in (1, -1):
        hv.chain([(1.5, s * 4.2, 44), (0, s * 7, 46.5), (0, s * 8.2, 50), (1.5, s * 7, 52.5)], 1.4, 0.35, sc2)   # sừng lưỡi liềm
        hv.chain([(6, s * 3.6, 38.5), (8, s * 4.7, 37), (9.6, s * 4.4, 39.2)], 1.05, 0.35, claw)             # nanh ngà cong
        for base, d, ln in (((3, s * 4.6, 40.5), (-0.5, s * 1, 0.2), 4.2), ((3.5, s * 3.8, 36), (-0.4, s * 1, -0.6), 3.6),
                            ((1.5, s * 2.5, 46), (-0.6, s * 0.5, 1), 4.8), ((0, s * 3.8, 44), (-1, s * 0.8, 0.6), 4.4),
                            ((1, s * 4.8, 37.5), (-0.6, s * 1, -0.1), 3.8), ((-1, s * 5, 41.5), (-1, s * 0.7, 0.2), 4.0),
                            ((6, s * 4.2, 43.5), (-0.2, s * 1, 0.5), 2.6), ((7, s * 3.2, 35.5), (0, s * 1, -0.7), 2.4)):
            hv.spike(base, d, ln, 1.1, sc2)                                        # viền gai đỏ quanh đầu
        hv.spike((9.2, s * 3.6, 43.2), (-1, s * 0.4, 0.55), 2.2, 0.7, sc)         # gờ mày nhọn hất ngược
        hv.ell(7.6, s * 3.4, 44.1, 1.3, 1.0, 0.8, eye)                             # mắt cam to
    merge_yawed(v, hv, (3, 0, 40), -35)                                           # quay mặt 35° cho thấy mõm và sừng nghiêng
    tail = [(-8, 0, 10), (-15, 0, 5), (-23, 2, 3), (-30, 7, 3), (-35, 12, 6), (-35, 16, 11)]
    v.chain(tail, 6, 1.0, body)
    for d in ((0.5, 0.6, 1), (-0.4, 0.9, 0.6)):
        v.spike(tail[-1], d, 3.2, 1.0, sc2)                                       # mũi đuôi chẻ đôi như càng
    # vảy đỏ thẫm lốm đốm khắp thân, tay, chân, đuôi
    rng = np.random.default_rng(7)
    for _ in range(1800):                                                       # vảy nhỏ dày như bản thiết kế
        x, y, z = rng.uniform(-36, 14), rng.uniform(-13, 13), rng.uniform(1, 48)
        c = (sc, sc2, body2)[rng.choice(3, p=(0.55, 0.25, 0.2))]
        m = ((v.x - x) / 1.25) ** 2 + ((v.y - y) / 1.1) ** 2 + ((v.z - z) / 0.9) ** 2 <= 1
        v.g[m & (v.g == body)] = c                                                # chỉ tô lên da, không đè vuốt/gai
    v.g[(v.g == body) & (rng.random(v.g.shape) < 0.08)] = body2
    # lõi ngực hình hoa 8 cánh
    for i in range(8):
        a = i * math.pi / 4
        v.capsule((8, 0, 27), (7.6, math.cos(a) * 3.6, 27 + math.sin(a) * 3.6), 1.3, 0.8, core if i % 2 else core2)
    v.ell(8.6, 0, 27, 1.0, 1.3, 1.3, core2)
    dorsal(v, [(-7, 0, 36), (-8.5, 0, 31), (-9.5, 0, 26), (-10.5, 0, 21), (-11.5, 0, 16), (-16, 0, 10), (-22, 0, 6.5),
               (-28, 2, 5), (-33, 7, 5.5)], [4, 4.5, 4.5, 4, 3.5, 3, 2.5, 2, 2], sc2, sc)
    # cánh dơi: xương tối + màng đỏ có gân, mép sau lượn răng cưa giữa các ngón
    for s in (1, -1):
        root, el = np.array((-4, s * 6, 34.0)), np.array((-6, s * 12, 48.0))       # cánh dang cao rộng sang hai bên
        tips = [np.array(t, float) for t in ((-5, s * 16, 59), (-8, s * 23, 57), (-11, s * 25.5, 47),
                                               (-11, s * 25, 35), (-10, s * 19, 25))]
        for a, b in [(el, tips[i]) for i in range(len(tips))]:
            v.capsule(a, b, 0.9, 0.4, bone)
        v.capsule(root, el, 1.5, 1.1, bone)
        v.spike(el, (0.3, s * 0.2, 1), 2.6, 0.8, claw)                            # móng ngón cái cánh
        tris = [(el, tips[i], tips[i + 1]) for i in range(len(tips) - 1)] + [(root, el, tips[-1])]
        for ti, (o, a, b) in enumerate(tris):
            for u in np.linspace(0, 1, 70):
                for w in np.linspace(0.05, 1, 70):
                    if ti < len(tris) - 1 and w > 1 - 0.16 * math.sin(math.pi * u):
                        continue                                                   # mép lượn răng cưa
                    p = o + (a + (b - a) * u - o) * w
                    ix, iy, iz = int(round(p[0] * v.s)) + v.ox, int(round(p[1] * v.s)) + v.oy, int(round(p[2] * v.s))
                    if 0 <= ix < v.g.shape[0] and 0 <= iy < v.g.shape[1] and 0 <= iz < v.g.shape[2] and v.g[ix, iy, iz] == 0:
                        v.g[ix, iy, iz] = vein if int(w * 9) % 3 == 0 else memb
    return v


def stripes(v, color, line, axis="x", step=3):
    """Vạch ngang trên mảng bụng: các khối màu `color` nằm trên lát chia hết cho step đổi sang `line`."""
    coord = {"x": v.x, "z": v.z}[axis]
    v.g[(v.g == color) & (np.floor(coord) % step == 0)] = line


def fluke(v, tip, c, size=4):
    """Vây đuôi hình quạt ở mũi đuôi."""
    for dz, dx in ((1, -1), (0, -1.3), (-1, -1)):
        v.spike(tip, (dx, 0, dz), size, 0.9, c)


def gz_head(v, o, k, pitch, body, body2, dark, gum, mouth, eye, tooth):
    """Đầu gầm kiểu Godzilla (mẫu Ultima form IV): sọ rộng, mõm dài sống mũi phẳng, gờ mày nặng, hàm há rộng
    với 2 hàng răng nhọn tách rời + nanh, lợi sẫm, họng phát sáng. o = tâm sọ, k = tỉ lệ, pitch (độ) > 0 ngóc mõm lên."""
    c, sn = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    o = np.array(o, float)

    def D(x, y, z):
        return np.array((x * c - z * sn, y, x * sn + z * c))

    def P(x, y, z):
        return o + k * D(x, y, z)

    v.ell(*P(0, 0, 0), 6 * k, 5 * k, 4.4 * k, body)                                   # sọ
    v.capsule(P(1, 0, -0.5), P(11, 0, -1), 4.3 * k, 3.0 * k, body)                    # mõm trên
    v.ell(*P(6.5, 0, 1.2), 5 * k, 2.8 * k, 1.6 * k, body)                             # sống mũi phẳng
    v.capsule(P(-1, 0, -5), P(9, 0, -11.5), 3.4 * k, 2.3 * k, body2)                  # hàm dưới há sâu
    for s in (1, -1):                                                                  # lợi viền hai hàm cho răng nổi
        v.capsule(P(2, s * 3.0, -3.4), P(11, s * 2.1, -3.1), 0.9 * k, 0.7 * k, gum, only_filled=True)
        v.capsule(P(1, s * 2.4, -3.1), P(9.5, s * 1.6, -9.8), 0.9 * k, 0.7 * k, gum, only_filled=True)
    v.capsule(P(0, 0, -4), P(9, 0, -7), 2.0 * k, 1.2 * k, mouth, only_empty=True)     # họng lấp khe hàm
    v.capsule(P(-1, 0, -3), P(4, 0, -4), 2.4 * k, 1.8 * k, mouth)
    for i, t in enumerate(np.arange(0.12, 1.0, 0.13)):
        big = 1.5 if i == 5 else 1.0                                                   # răng nanh to phía trước
        for s in (1, -1):
            r_u, zc_u = 4.3 - 1.3 * t, -0.5 - 0.5 * t                                 # mép dưới mõm trên
            v.spike(P(1 + 10 * t, s * r_u * 0.68, zc_u - r_u * 0.7), D(0.15, 0, -1), 1.6 * big * k, 0.36 * k, tooth)
            r_l, zc_l = 3.4 - 1.1 * t, -5 - 6.5 * t                                   # mép trên hàm dưới
            v.spike(P(-0.5 + 9.5 * t, s * r_l * 0.62, zc_l + r_l * 0.72), D(0.1, 0, 1), 1.4 * big * k, 0.36 * k, tooth)
    for s in (1, -1):
        v.capsule(P(-0.5, s * 4.1, 3.3), P(5.5, s * 3.3, 2.6), 1.2 * k, 0.8 * k, body2)  # gờ mày nặng
        v.ell(*P(3.6, s * 3.75, 1.6), 1.1 * k, 0.7 * k, 0.6 * k, eye)                    # mắt dưới gờ mày
        v.spike(P(-1.5, s * 3.8, 2.5), D(-1, s * 0.5, 0.5), 3.2 * k, 0.9 * k, dark)     # gai má hất ra sau
        v.spike(P(-1.5, s * 3.2, -4.5), D(-1, s * 0.6, -0.3), 2.8 * k, 0.9 * k, dark)
        v.ell(*P(11.3, s * 1.2, 0.2), 0.5 * k, 0.4 * k, 0.4 * k, dark)                  # lỗ mũi


def leaf_plate(v, cx, cz, h, cy, t, c, lean=0.45):
    """Bản gai hình lá nghiêng ra sau, mép răng cưa (cứ 3 tầng thu hẹp 1 nấc), dày |y - cy| <= t."""
    dz = v.z - cz
    half = (h - dz) * 0.36 + 0.6 - (np.floor(dz) % 3 == 2) * 0.9
    m = (np.abs(v.y - cy) <= t + 0.5 / v.s) & (dz >= 0) & (dz <= h) & (np.abs(v.x - cx + dz * lean) <= half)
    v.put(m, c)


def ultima(stage):
    """4 form theo tờ mẫu, tư thế hung hãn: I thủy long đỏ thẫm ngóc đầu, II bốn chân gỉ sắt lao tới,
    III xanh mòng két dựng người vồ mồi, IV xám đen đứng thẳng gầm trời."""
    v = Vox(1.6)                                           # voxel mịn hơn cho chi tiết răng, cơ bắp
    claw = col("u_claw", "#f4efe6")
    if stage == 1:
        # I: khuôn Godzilla của form II-IV (voxel mịn, đầu gầm 2 hàng răng) trên thân thủy long đỏ thẫm bò thấp:
        # cổ ngóc cao như rắn sắp đớp, khe mang cam phát sáng dọc cổ, vây chèo, đuôi vây quạt, gai hồng lõi cam
        body, body2, dark = col("u1_body", "#b8223a"), col("u1_body2", "#961a2f"), col("u1_dark", "#5e0c1c")
        belly, bline = col("u1_belly", "#e6d1a6"), col("u1_bline", "#a88a5c")
        sp_o, sp_g, sp_d = col("u1_sp", "#e2ccd2"), col("u1_spglow", "#ff9f5a"), col("u1_spside", "#b9a2a9")
        gill, mouth, eye = col("u1_glow", "#ff7b39"), col("u1_mouthglow", "#ffb36b"), col("u1_eyeglow", "#ffe14d")
        v.chain([(10, 0, 9), (2, 0, 9.5), (-8, 0, 9), (-18, 0, 8)], 4.8, 3.6, body)   # thân thuôn bò sát đất
        v.capsule((10, 0, 9), (15, 0, 17), 4.6, 3.9, body)                       # cổ ngóc cao như rắn
        v.capsule((15, 0, 17), (17, 0, 20), 3.9, 3.6, body)
        v.chain([(9, 0, 5.2), (0, 0, 5.6), (-12, 0, 5.0)], 3.0, 2.4, belly, only_filled=True)   # bụng kem
        v.capsule((10, 0, 6), (15, 0, 14), 2.6, 2.2, belly, only_filled=True)
        stripes(v, belly, bline, "x", 3)
        for s in (1, -1):
            for k in range(4):                                                    # khe mang phát sáng dọc cổ
                x, z = 11.5 + k * 1.3, 11 + k * 1.8
                v.capsule((x, s * 4.1, z - 0.9), (x + 0.4, s * 4.0, z + 0.9), 0.55, 0.55, gill)
            v.capsule((8, s * 3.5, 7), (12, s * 9, 3), 2.0, 1.4, body)           # vây trước vươn tới
            v.capsule((12, s * 9, 3), (15, s * 11, 1), 1.4, 0.9, dark)
            v.capsule((-8, s * 3.5, 7), (-11, s * 9, 3), 2.0, 1.4, body)         # vây sau quặt về sau
            v.capsule((-11, s * 9, 3), (-15, s * 11, 1), 1.4, 0.9, dark)
            for k in (-1, 0, 1):
                v.spike((15, s * 11, 1), (1, s * 0.3 + k * 0.4, 0), 3.0, 0.6, claw)
                v.spike((-15, s * 11, 1), (-1, s * 0.3 + k * 0.4, 0), 2.6, 0.6, claw)
        gz_head(v, (19, 0, 22), 0.85, 15, body, body2, dark, col("u1_gum", "#4a0a14"), mouth, eye, claw)
        tail = [(-18, 0, 8), (-26, 2, 6), (-32, 6, 7), (-35, 11, 11), (-34, 15, 16)]
        v.chain(tail, 3.6, 1.0, body)
        fluke(v, tail[-1], sp_o, 5)
        rng = np.random.default_rng(11)
        v.g[(v.g == body) & (rng.random(v.g.shape) < 0.07)] = body2               # vân da sẫm
        v.g[(v.g == body) & (np.floor(v.x + v.z * 2) % 7 == 0) & (rng.random(v.g.shape) < 0.5)] = body2   # thớ cơ chéo
        for cx, cz, h in ((12, 16, 4), (8, 13, 6), (4, 13, 7), (0, 13.5, 8), (-4, 13.5, 8), (-8, 13, 7.5),
                          (-12, 12.5, 7), (-16, 12, 6), (-20, 11, 5), (-25, 9, 4), (-30, 8.5, 3)):
            leaf_plate(v, cx, cz, h + 1, 0, 0, sp_g)                              # mũi gai cam
            leaf_plate(v, cx, cz, h, 0, 1, sp_o)
            leaf_plate(v, cx + 0.5, cz + 1, h * 0.55, 0, 1, sp_g)                 # lõi cam lộ hai mặt
            for s in (1, -1):
                leaf_plate(v, cx + 0.5, cz + 2, h * 0.6, s * 2.5, 0, sp_d)
                leaf_plate(v, cx + 0.8, cz + 3, h * 0.3, s * 2.5, 0, sp_g)
        return v
    if stage == 2:
        # II: khuôn Godzilla của form III/IV (voxel mịn, đầu gầm 2 hàng răng) trên thân 4 chân gỉ sắt đang lao tới:
        # lưng gù giáp vảy, khe dung nham phát sáng hai sườn, sừng chĩa tới để húc, gai lưng gỉ lõi cam rực
        body, body2, dark = col("u2_body", "#a83d16"), col("u2_body2", "#8a3010"), col("u2_dark", "#5a1d08")
        belly, bline = col("u2_belly", "#d9b184"), col("u2_bline", "#9a6f45")
        sp_o, sp_g, sp_d = col("u2_sp", "#7a2a0e"), col("u2_spglow", "#ffb347"), col("u2_spside", "#5e1f09")
        crack, mouth, eye = col("u2_crack", "#ff7a1a"), col("u2_mouthglow", "#ff4d1a"), col("u2_eyeglow", "#fff6a8")
        v.ell(0, 0, 13, 14, 9, 8, body)                                           # thân thùng
        v.ell(8, 0, 17, 7.5, 8.5, 6.5, body)                                      # u vai gồ cao
        v.ell(-9, 0, 14, 7, 8, 6.5, body)                                         # hông
        v.ell(2, 0, 7, 11, 6, 3, belly, only_filled=True)                         # bụng kem
        stripes(v, belly, bline, "x", 3)
        for sx, hx, rx in ((10, 14, 4.5), (-9, -3, 5.5)):                         # chân trước / chân sau (đùi to hơn)
            for s in (1, -1):
                v.ell(sx, s * 7.5, 11, rx, 3.8, 5.5, body)                                          # đùi
                v.capsule((sx, s * 8.5, 9), (hx - 2, s * 9.5, 2), 3.8, 3.2, body)                  # cẳng chân cột
                v.ell(hx, s * 9.5, 1.3, 4, 3, 1.6, body)                                                 # bàn chân
                for k in (-1, 0, 1):
                    v.spike((hx + 3.5, s * 9.5 + k * 1.4, 1), (1, k * 0.25, -0.1), 2.8, 0.8, claw)
        rng = np.random.default_rng(21)
        for _ in range(90):                                                       # giáp vảy gồ ghề trên lưng, sườn
            x, y, z = rng.uniform(-14, 13), rng.choice([-1, 1]) * rng.uniform(4, 8.5), rng.uniform(13, 22)
            v.ell(x, y, z, 1.6, 1.3, 1.1, dark if rng.random() < 0.4 else body2, only_filled=True)
        v.capsule((12, 0, 17), (19, 0, 19), 5.4, 4.8, body)                       # cổ dày chúi tới
        gz_head(v, (21, 0, 19.5), 0.95, 10, body, body2, dark, col("u2_gum", "#4a1006"), mouth, eye, claw)
        for s in (1, -1):
            v.spike((21, s * 3.2, 23), (0.8, s * 0.4, 0.75), 6, 1.5, claw)       # sừng chĩa tới để húc
            v.spike((18, s * 4, 22.5), (-0.7, s * 0.6, 0.5), 4, 1.1, sp_o)       # gờ sừng hất ra sau
        tail = [(-14, 0, 13), (-22, 0, 10), (-29, -4, 7), (-34, -10, 6), (-36, -15, 8)]
        v.chain(tail, 5.4, 1.4, body)
        fluke(v, tail[-1], sp_o, 5)
        v.g[(v.g == body) & (rng.random(v.g.shape) < 0.07)] = body2               # vân da sẫm
        v.g[(v.g == body) & (np.floor(v.x + v.z * 2) % 7 == 0) & (rng.random(v.g.shape) < 0.5)] = body2   # thớ cơ chéo
        v.g[(v.g == body) & (np.floor(v.z + v.x / 2) % 5 == 0) & (rng.random(v.g.shape) < 0.85)
            & (np.abs(v.y) > 5) & (v.z > 8) & (v.z < 20)] = crack                 # khe dung nham chạy chéo hai sườn
        for cx, cz, h in ((12, 21, 6), (8, 22, 9), (4, 21, 11), (0, 20, 12), (-4, 19, 12), (-8, 19, 10),
                          (-12, 18, 8), (-17, 13, 6), (-22, 10, 5), (-27, 7, 4), (-31, 6, 3)):
            leaf_plate(v, cx, cz, h + 1, 0, 0, sp_g)                              # mũi gai cam rực
            leaf_plate(v, cx, cz, h, 0, 1, sp_o)
            leaf_plate(v, cx + 0.5, cz + 1, h * 0.55, 0, 1, sp_g)                 # lõi cam rực lộ hai mặt
            for s in (1, -1):
                leaf_plate(v, cx + 0.5, cz + 2, h * 0.6, s * 3, 0, sp_d)
                leaf_plate(v, cx + 0.8, cz + 3, h * 0.3, s * 3, 0, sp_g)
        return v
    if stage == 3:
        # III: cùng khuôn Godzilla của form IV (voxel mịn, đầu gầm 2 hàng răng) nhưng thon, dựng người vồ mồi:
        # da xanh mòng két, bụng kem, hai tay giơ cao vuốt dài, đầu ngóc gầm họng băng lam, gai bạc lõi băng
        body, body2, dark = col("u3_body", "#1f8a8c"), col("u3_body2", "#166c6f"), col("u3_dark", "#0c4049")
        belly, bline = col("u3_belly", "#e3cfa2"), col("u3_bline", "#a68c5e")
        sp_o, sp_g, sp_d = col("u3_sp", "#c3c8d2"), col("u3_spglow", "#7ff5ff"), col("u3_spside", "#8f9aa6")
        mouth, eye = col("u3_mouthglow", "#9ffcff"), col("u3_eyeglow", "#ffe14d")
        for s in (1, -1):
            v.ell(-4, s * 6.5, 11, 6, 4.5, 8, body)                               # đùi khoẻ
            v.capsule((-3, s * 7, 7), (-1, s * 7.5, 2), 3.6, 3.0, body)          # cẳng chân
            v.ell(2, s * 7.5, 1.3, 4.5, 3, 1.8, body)                             # bàn chân
            for k in (-1, 0, 1):
                v.spike((5.5, s * 7.5 + k * 1.5, 1), (1, k * 0.25, -0.1), 2.8, 0.8, claw)
        v.capsule((-4, 0, 14), (1, 0, 28), 8.5, 7, body)                          # thân dựng gần thẳng
        v.ell(2, 0, 26, 7, 7.5, 7, body)                                          # ngực ưỡn
        v.capsule((4, 0, 9), (7, 0, 29), 4.5, 4, belly, only_filled=True)        # bụng kem
        v.capsule((5, 0, 9), (8, 0, 29), 4.5, 4, belly, only_filled=True)
        stripes(v, belly, bline, "z", 3)
        for s in (1, -1):
            v.ell(1, s * 6.5, 30, 4.5, 4, 4, body)                                # vai
            v.capsule((3, s * 8, 29), (8, s * 11, 31), 3.0, 2.6, body)           # tay giơ cao vồ tới
            v.capsule((8, s * 11, 31), (14, s * 11, 35), 2.6, 2.1, body)
            for k in (-1, 0, 1):
                v.spike((14.5, s * 11, 35), (1, k * 0.45, -0.35 + k * 0.35), 5.5, 0.9, claw)   # vuốt dài xoè
        v.capsule((2, 0, 31), (8, 0, 36), 5.4, 4.6, body)                         # cổ
        gz_head(v, (9, 0, 37.5), 0.9, 18, body, body2, dark, col("u3_gum", "#4a1626"), mouth, eye, claw)
        tail = [(-8, 0, 12), (-16, 0, 7), (-24, 5, 4), (-31, 10, 3), (-36, 15, 5)]
        v.chain(tail, 6.5, 1.8, body)
        rng = np.random.default_rng(33)
        v.g[(v.g == body) & (rng.random(v.g.shape) < 0.07)] = body2               # vân da sẫm
        v.g[(v.g == body) & (np.floor(v.x + v.z * 2) % 7 == 0) & (rng.random(v.g.shape) < 0.5)] = body2   # thớ cơ chéo
        for cx, cz, h in ((-12, 10, 8), (-11, 15, 11), (-9, 20, 13), (-7, 25, 13), (-4, 30, 11), (-1, 34, 8),
                          (-17, 8, 6), (-23, 5, 5), (-29, 4, 4), (-34, 5, 3)):
            leaf_plate(v, cx, cz, h + 1, 0, 0, sp_g)                              # mũi gai băng lam
            leaf_plate(v, cx, cz, h, 0, 1, sp_o)
            leaf_plate(v, cx + 0.5, cz + 1, h * 0.55, 0, 1, sp_g)                 # lõi băng lam lộ hai mặt
            for s in (1, -1):
                leaf_plate(v, cx + 0.5, cz + 2, h * 0.6, s * 3, 0, sp_d)
                leaf_plate(v, cx + 0.8, cz + 3, h * 0.3, s * 3, 0, sp_g)
        return v
    # IV: dáng Godzilla (khom, đầu chúi tới, mảng gai lưng dày) nhưng khối nhỏ hơn, bo cơ bắp, đang gầm:
    # thân xám đen vân sẫm, khe nứt đỏ âm ỉ, hàm há họng đỏ, tay xoè vuốt, gai bạc răng cưa lõi đỏ rực
    body, body2, dark = col("u4_body", "#454955"), col("u4_body2", "#363a44"), col("u4_dark", "#1b1c21")
    belly, bline = col("u4_belly", "#727782"), col("u4_bline", "#555a64")
    sp_o, sp_g = col("u4_sp", "#d3d6dd"), col("u4_spglow", "#ff3b30")
    mouth, eye = col("u4_mouthglow", "#ff4a2a"), col("u4_eyeglow", "#ffd23f")
    sp_d = col("u4_spside", "#9ca2ad")
    for s in (1, -1):
        v.ell(-2, s * 7, 10, 7, 5, 9, body)                                   # đùi vạm vỡ
        v.capsule((-1, s * 8, 6), (2, s * 8.5, 2), 4, 3.5, body)             # cẳng chân
        v.ell(4, s * 8.5, 1.5, 5, 3.5, 2, body)                              # bàn chân
        for k in (-1, 0, 1):
            v.spike((8, s * 8.5 + k * 1.6, 1), (1, k * 0.25, -0.1), 3, 0.9, claw)
    v.ell(-3, 0, 18, 10, 9, 12, body)                                         # thân khom đồ sộ
    v.ell(2, 0, 26, 8, 8.5, 8, body)                                          # ngực ưỡn
    v.capsule((7, 0, 8), (9, 0, 27), 4.5, 4, belly, only_filled=True)        # mảng bụng
    v.capsule((8, 0, 8), (10, 0, 27), 4.5, 4, belly, only_filled=True)
    stripes(v, belly, bline, "z", 3)
    for s in (1, -1):
        v.ell(1, s * 7, 29, 5, 4, 4.5, body)                                  # vai u
        v.capsule((4, s * 9, 25), (8, s * 11.5, 19), 3.1, 2.7, body)         # cánh tay vạm vỡ chìa ra trước ngực
        v.capsule((8, s * 11.5, 19), (13, s * 12, 21), 2.7, 2.1, body)
        for k in (-1, 0, 1):
            v.spike((13.5, s * 12, 21), (1, s * 0.15 + k * 0.4, 0.2 + k * 0.4), 4.2, 0.8, claw)   # vuốt xoè
    v.capsule((3, 0, 29), (11, 0, 33), 6.4, 5.4, body)                        # cổ dày chúi tới
    # đầu kiểu Godzilla: sọ rộng, mõm dài sống mũi phẳng, gờ mày nặng, hàm há rộng khoe 2 hàng răng nhọn
    gz_head(v, (12, 0, 35), 1.0, 0, body, body2, dark, col("u4_gum", "#6b1414"), mouth, eye, claw)
    tail = [(-10, 0, 12), (-18, 0, 6), (-26, 4, 3), (-34, 9, 2)]
    v.chain(tail, 7, 2, body)
    rng = np.random.default_rng(44)
    v.g[(v.g == body) & (rng.random(v.g.shape) < 0.07)] = body2                   # vân da sẫm
    v.g[(v.g == body) & (np.floor(v.x + v.z * 2) % 7 == 0) & (rng.random(v.g.shape) < 0.5)] = body2   # thớ cơ chéo

    for cx, cz, h in ((-14, 8, 8), (-12, 14, 11), (-10, 20, 14), (-7, 26, 15), (-3, 31, 12), (1, 35, 8),
                      (-19, 6, 6), (-25, 4, 5), (-31, 3, 3)):
        leaf_plate(v, cx, cz, h + 1, 0, 0, sp_g)                         # mũi gai đỏ lòi lên
        leaf_plate(v, cx, cz, h, 0, 1, sp_o)
        leaf_plate(v, cx + 0.5, cz + 1, h * 0.55, 0, 1, sp_g)            # lõi đỏ rực lộ ra hai mặt
        for s in (1, -1):
            leaf_plate(v, cx + 0.5, cz + 2, h * 0.6, s * 3, 0, sp_d)     # hàng gai phụ hai bên (bạc sẫm hơn), so le hàng giữa
            leaf_plate(v, cx + 0.8, cz + 3, h * 0.3, s * 3, 0, sp_g)
    return v

# ---------------- dựng mesh + render ----------------

SHADE = {(0, 0, 1): 1.0, (0, 0, -1): 0.4, (1, 0, 0): 0.66, (-1, 0, 0): 0.5, (0, 1, 0): 0.82, (0, -1, 0): 0.55}
GLOW = ("cr_", "core", "eye", "crack", "_cr", "glow")


def build_mesh(v, name):
    for o in [o for o in bpy.data.objects if o.name.startswith("VX_")]:
        bpy.data.objects.remove(o, do_unlink=True)
    idx2rgb = {i: (n, rgb) for n, (i, rgb) in PAL.items()}
    bm = bmesh.new()
    cols = []
    filled = np.argwhere(v.g > 0)
    occ = v.g > 0
    for x, y, z in filled:
        cname, rgb = idx2rgb[int(v.g[x, y, z])]
        glow = any(k in cname for k in GLOW)
        for (dx, dy, dz), sh in SHADE.items():
            nx, ny, nz = x + dx, y + dy, z + dz
            if 0 <= nx < occ.shape[0] and 0 <= ny < occ.shape[1] and 0 <= nz < occ.shape[2] and occ[nx, ny, nz]:
                continue
            # 4 góc của mặt
            if dz:
                zz = z + (1 if dz > 0 else 0)
                quad = [(x, y, zz), (x + 1, y, zz), (x + 1, y + 1, zz), (x, y + 1, zz)]
            elif dx:
                xx = x + (1 if dx > 0 else 0)
                quad = [(xx, y, z), (xx, y + 1, z), (xx, y + 1, z + 1), (xx, y, z + 1)]
            else:
                yy = y + (1 if dy > 0 else 0)
                quad = [(x, yy, z), (x + 1, yy, z), (x + 1, yy, z + 1), (x, yy, z + 1)]
            vs = [bm.verts.new(((a - v.ox) / v.s, (b - v.oy) / v.s, c / v.s)) for a, b, c in quad]
            bm.faces.new(vs)
            k = 1.0 if glow else sh
            cols.append(tuple(min(1.0, ch * k) for ch in rgb) + (1.0,))
    me = bpy.data.meshes.new("VX_" + name)
    bm.to_mesh(me)
    bm.free()
    attr = me.color_attributes.new("Col", 'FLOAT_COLOR', 'CORNER')
    for poly in me.polygons:
        c = cols[poly.index]
        for li in poly.loop_indices:
            attr.data[li].color = c
    ob = bpy.data.objects.new("VX_" + name, me)
    scene().collection.objects.link(ob)
    return ob, (filled - np.array([v.ox, v.oy, 0]) + 0.5) / v.s


def scene():
    s = bpy.data.scenes.get("VoxelBoss") or bpy.data.scenes.new("VoxelBoss")
    s.render.engine = 'BLENDER_WORKBENCH'
    s.display.shading.light = 'FLAT'
    s.display.shading.color_type = 'VERTEX'
    s.display.render_aa = 'OFF'
    s.render.film_transparent = True
    s.view_settings.view_transform = 'Standard'
    s.render.image_settings.file_format = 'PNG'
    s.render.image_settings.color_mode = 'RGBA'
    return s


def camera(filled, res_x, res_y, margin=1.12, frame_min_x=None, elev=0.55):
    s = scene()
    cam = bpy.data.objects.get("VX_cam")
    if not cam:
        cam = bpy.data.objects.new("VX_cam", bpy.data.cameras.new("VX_cam"))
        s.collection.objects.link(cam)
    cam.data.type = 'ORTHO'
    pts = filled                               # tâm các voxel, đơn vị mô hình (build_mesh trả về)
    if frame_min_x is not None:                # căn khung theo thân, để đuôi chìa ra sau khung như sprite cũ
        pts = pts[pts[:, 0] >= frame_min_x]
    center = (pts.min(0) + pts.max(0)) / 2
    d = np.array([0.9, 0.75, elev])            # nhìn từ bên trái-trước-trên => đầu (+X) nằm bên trái ảnh; elev thấp = góc hất lên
    d /= np.linalg.norm(d)
    cam.location = tuple(center + d * 200)
    look = -d
    yaw = math.atan2(look[1], look[0])
    pitch = math.asin(look[2])
    cam.rotation_euler = (math.pi / 2 + pitch, 0, yaw - math.pi / 2)
    # ortho_scale vừa khít khối nhìn từ camera
    right = np.cross(look, [0, 0, 1]); right /= np.linalg.norm(right)
    up = np.cross(right, look)
    pr, pu = (pts - center) @ right, (pts - center) @ up
    w, h = (pr.max() - pr.min() + 2), (pu.max() - pu.min() + 2)
    aspect = res_x / res_y
    cam.data.ortho_scale = max(w, h * aspect) * margin
    cam.data.shift_x = ((pr.max() + pr.min()) / 2) / cam.data.ortho_scale
    cam.data.shift_y = ((pu.max() + pu.min()) / 2) / cam.data.ortho_scale
    s.camera = cam
    s.render.resolution_x, s.render.resolution_y, s.render.resolution_percentage = res_x, res_y, 100


def render(v, name, out_dir, res=(96, 96), margin=1.04, frame_min_x=-14, elev=0.55):
    ob, filled = build_mesh(v, name)
    camera(filled, *res, margin=margin, frame_min_x=frame_min_x, elev=elev)
    s = scene()
    s.render.filepath = out_dir + name + ".png"
    bpy.ops.render.render(write_still=True, scene=s.name)
    return s.render.filepath


JOBS = [
    ("spacegodzilla_idle", lambda: spacegodzilla(False), {"res": (144, 144), "frame_min_x": -16, "elev": 0.38}),
    ("spacegodzilla_enrage", lambda: spacegodzilla(True), {"res": (144, 144), "frame_min_x": -16, "elev": 0.38}),
    ("destoroyah_idle", lambda: destoroyah(False), {"res": (144, 144), "frame_min_x": -20, "elev": 0.32}),
    ("destoroyah_enrage", lambda: destoroyah(True), {"res": (144, 144), "frame_min_x": -20, "elev": 0.32}),
    ("ultima_stage1", lambda: ultima(1), {"res": (144, 144), "frame_min_x": None, "elev": 0.4}),
    ("ultima_stage2", lambda: ultima(2), {"res": (144, 144), "frame_min_x": None, "elev": 0.4}),
    ("ultima_stage3", lambda: ultima(3), {"res": (144, 144), "frame_min_x": None, "elev": 0.38}),
    ("ultima_stage4", lambda: ultima(4), {"res": (144, 144), "frame_min_x": None, "elev": 0.42}),
    ("spacegodzilla_rampage", lambda: spacegodzilla(False), {"res": (380, 270), "margin": 1.02, "frame_min_x": -22, "elev": 0.35}),
    ("destoroyah_rampage", lambda: destoroyah(False), {"res": (380, 270), "margin": 1.02, "frame_min_x": -24, "elev": 0.3}),
    ("ultima_rampage", lambda: ultima(4), {"res": (380, 270), "margin": 1.02, "frame_min_x": -20, "elev": 0.4}),
]


def render_all(out_dir):
    return [render(fn(), name, out_dir, **kw) for name, fn, kw in JOBS]


def render_all_async(out_dir, log_path, only=None):
    """Render từng sprite trong timer của Blender, ghi tiến độ ra log_path."""
    import traceback
    jobs = list(JOBS)
    if only:
        jobs[:] = [j for j in jobs if j[0] in only]
    log = open(log_path, "w", encoding="utf-8")

    def step():
        if not jobs:
            log.write("ALLDONE\n")
            log.close()
            return None
        name, fn, kw = jobs.pop(0)
        try:
            render(fn(), name, out_dir, **kw)
            log.write("OK " + name + "\n")
        except Exception:
            log.write("ERR " + name + "\n" + traceback.format_exc())
        log.flush()
        return 0.05

    bpy.app.timers.register(step, first_interval=0.05)
