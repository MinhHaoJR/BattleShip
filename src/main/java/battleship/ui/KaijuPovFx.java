package battleship.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Random;

import static battleship.ui.KaijuSkillFx.block;
import static battleship.ui.KaijuSkillFx.clamp;
import static battleship.ui.KaijuSkillFx.clampF;
import static battleship.ui.KaijuSkillFx.glow;
import static battleship.ui.KaijuSkillFx.line;
import static battleship.ui.KaijuSkillFx.shard;

/**
 * Cảnh phim POV 3 màn cho chiêu chủ lực của 3 boss mới, theo khuôn của Godzilla gốc trong {@link CombatFxOverlay}:
 * màn 1 tụ lực trên thân (mỗi boss/form một bộ phận riêng), màn 2 phóng chiêu, màn 3 slow-mo hậu quả.
 * CombatFxOverlay lo khung, phông, sprite boss, chuyển cảnh; lớp này chỉ vẽ phần riêng của từng boss.
 */
final class KaijuPovFx {
    private KaijuPovFx() { }

    /** Kiểu cảnh: SG, DS, U1..U4 (theo form Ultima của chiêu); null = boss cũ. */
    static String style(TitanMode.TitanSkill s) {
        if (s == null || s.spec == null) return null;
        return switch (s.boss) {
            case SPACEGODZILLA -> "SG";
            case DESTOROYAH -> "DS";
            case ULTIMA -> "U" + Math.max(1, s.ultimaStage);
            default -> null;
        };
    }

    /** Chiêu được mở cảnh POV: chiêu chủ lực thường + 2 chiêu ★ của mỗi boss/form (như Godzilla: tia + 2 ★). */
    static boolean isSignature(TitanMode.TitanSkill s) {
        if (style(s) == null) return false;
        return s.phase2Only || switch (s) {
            case SG_CORONA_BEAM, DS_MICRO_OXYGEN, UL1_GILL_SPRAY, UL2_RAMPAGE_CHARGE, UL3_SONIC_ROAR, UL4_CRIMSON_BREATH -> true;
            default -> false;
        };
    }

    static Color[] palette(TitanMode.TitanSkill s, boolean enraged) {
        return switch (style(s)) {
            case "SG" -> enraged
                    ? new Color[]{new Color(0x0C4A6E), new Color(0x38BDF8), new Color(0xE0FAFF)}
                    : new Color[]{new Color(0x3B0764), new Color(0xC084FC), new Color(0xF5E8FF)};
            case "DS" -> new Color[]{new Color(0x450A0A), new Color(0xEF4444), new Color(0xFDE047)};
            case "U1" -> new Color[]{new Color(0x4C0519), new Color(0xFF7B39), new Color(0xFFE4C4)};
            case "U2" -> new Color[]{new Color(0x431407), new Color(0xFF7A1A), new Color(0xFDE047)};
            case "U3" -> new Color[]{new Color(0x083344), new Color(0x5EEAD4), new Color(0xECFEFF)};
            default -> new Color[]{new Color(0x450A0A), new Color(0xFF3B30), new Color(0xFFE4E1)};
        };
    }

    static String topTape(TitanMode.TitanSkill s) {
        String name = s.skillName.toUpperCase();
        return switch (style(s)) {
            case "SG" -> "BÁO ĐỘNG QUỸ ĐẠO · " + name;
            case "DS" -> "CẢNH BÁO OXY HỦY DIỆT · " + name;
            default -> "GODZILLA ULTIMA · " + TitanMode.ultimaStageName(s.ultimaStage).toUpperCase() + " · " + name;
        };
    }

    static String bottomTape(TitanMode.TitanSkill s) {
        return switch (style(s)) {
            case "SG" -> "CỘT VAI TINH THỂ BÙNG SÁNG · TRỌNG LỰC ĐẢO CHIỀU · PHÁO ĐÀI TINH THỂ TRỒI LÊN";
            case "DS" -> "LÕI NGỰC NỞ TÁM CÁNH · OXY VI THỂ PHÓNG RA · PHÂN RÃ PHÂN TỬ";
            case "U1" -> "KHE MANG RỰC CAM · HƠI BỎNG PHUN TRÀO · CỘT NƯỚC DỰNG ĐỨNG";
            case "U2" -> "KHE NỨT DUNG NHAM LAN KHẮP THÂN · LAO TỚI · NÚI LỬA ĐÁY BIỂN";
            case "U3" -> "GAI LƯNG BĂNG LAM SÁNG TỪNG ĐỐT · SÓNG ÂM BĂNG HÀ · RỪNG GAI BĂNG";
            default -> "GAI LƯNG ĐỎ BỪNG TỪNG ĐỐT · TIA HUYẾT HẠCH · NẤM LỬA ĐỎ";
        };
    }

    static String phaseTag(TitanMode.TitanSkill s, double t) {
        String st = style(s);
        if (t < 0.35) return switch (st) {
            case "SG" -> "CAM 01 [CRYSTAL CHARGE] · CỘT VAI TINH THỂ BÙNG SÁNG!";
            case "DS" -> "CAM 01 [OXYGEN CORE] · LÕI NGỰC NỞ TỪNG CÁNH!";
            case "U1" -> "CAM 01 [GILL IGNITION] · KHE MANG RỰC CAM!";
            case "U2" -> "CAM 01 [MAGMA VEINS] · DUNG NHAM LAN KHẮP THÂN!";
            case "U3" -> "CAM 01 [FROST SPINES] · GAI LƯNG BĂNG LAM SÁNG TỪNG ĐỐT!";
            default -> "CAM 01 [BLOOD CORE] · GAI LƯNG ĐỎ BỪNG TỪNG ĐỐT!";
        };
        if (t < 0.425) return "!! TĨNH LẶNG TRƯỚC CƠN BÃO !!";
        if (t < 0.68) return "CAM 02 [PHÓNG CHIÊU] · " + s.skillName.toUpperCase() + "!";
        return switch (st) {
            case "SG" -> "CAM 03 [SLOW-MO] · PHÁO ĐÀI TINH THỂ & SIÊU TÂN TINH!";
            case "DS" -> "CAM 03 [SLOW-MO] · MÀN OXY ĐEN PHÂN RÃ HẠM ĐỘI!";
            case "U1" -> "CAM 03 [SLOW-MO] · CỘT NƯỚC & SÓNG THẦN VÒNG!";
            case "U2" -> "CAM 03 [SLOW-MO] · NÚI LỬA ĐÁY BIỂN PHUN TRÀO!";
            case "U3" -> "CAM 03 [SLOW-MO] · RỪNG GAI BĂNG & VÒM GIÁ LẠNH!";
            default -> "CAM 03 [SLOW-MO] · NẤM LỬA HUYẾT HẠCH & XUNG KÍCH KIM CƯƠNG!";
        };
    }

    static String callout(TitanMode.TitanSkill s) {
        return switch (style(s)) {
            case "SG" -> "SIÊU TINH THỂ VŨ TRỤ!";
            case "DS" -> "OXY HỦY DIỆT!";
            case "U1" -> "THỦY LONG NỔI GIẬN!";
            case "U2" -> "NÚI LỬA ĐÁY BIỂN!";
            case "U3" -> "BĂNG HÀ TỐI THƯỢNG!";
            default -> "HUYẾT HẠCH TỐI THƯỢNG!";
        };
    }

    /** Màn 1 · tụ lực trên thân. (cx, cy) tâm sprite, sz cạnh sprite, (mx, my) miệng, seaY mặt biển. */
    static void charge(Graphics2D g, TitanMode.TitanSkill s, double cx, double cy, double sz, double mx, double my, int seaY,
                       double chargeP, boolean vacuum, Color mid, Color hot, float alpha, long now) {
        Random r = new Random(s.ordinal() * 31L);
        switch (style(s)) {
            case "SG" -> {        // hai cụm cột vai lớn dần từng trụ, mảnh tinh thể bay quanh thân, tia nối về miệng
                double[][] shoulders = {{0.02, -0.28}, {-0.16, -0.33}};
                for (int side = 0; side < 2; side++) {
                    for (int k = 0; k < 3; k++) {
                        double f = clamp((chargeP - side * 0.12 - k * 0.14) / 0.3);
                        if (f <= 0) continue;
                        double x = cx + sz * (shoulders[side][0] + (k - 1) * 0.05), y = cy + sz * shoulders[side][1];
                        double h = sz * (0.16 + 0.06 * (1 - Math.abs(k - 1))) * f;
                        glow(g, mid, x, y - h * 0.6, sz * 0.09 * f, alpha * 0.9f);
                        shard(g, x, y - h / 2, sz * 0.06, h, (k - 1) * 0.25, mid, hot, alpha);
                        if (chargeP > 0.6) line(g, x, y - h, mx, my, 2f, hot, alpha * 0.55f * (float) clamp((chargeP - 0.6) / 0.3));
                    }
                }
                for (int i = 0; i < 12; i++) {
                    double ang = now * 0.0022 + i * Math.PI / 6;
                    double x = cx + Math.cos(ang) * sz * 0.62, y = cy + Math.sin(ang) * sz * 0.25;
                    shard(g, x, y, sz * 0.03, sz * 0.07, ang, mid, hot, alpha * (float) chargeP);
                }
            }
            case "DS" -> {        // lõi ngực nở từng cánh hoa, sừng mũi sáng, hạt oxy đỏ xoáy vào lõi
                double ox = cx - sz * 0.10, oy = cy + sz * 0.02;
                for (int i = 0; i < 24; i++) {
                    double u = ((now * 0.0009 + i / 24.0) % 1.0);
                    double ang = i * 0.9 + u * 6;
                    double rad = sz * 0.5 * (1 - u);
                    block(g, ox + Math.cos(ang) * rad, oy + Math.sin(ang) * rad * 0.7, 4, i % 2 == 0 ? mid : new Color(0xF472B6), alpha * (float) chargeP);
                }
                for (int i = 0; i < 8; i++) {
                    double f = clamp((chargeP - i * 0.07) / 0.3);
                    if (f <= 0) continue;
                    double ang = i * Math.PI / 4 - Math.PI / 2;
                    double d = sz * (0.03 + 0.06 * f);
                    shard(g, ox + Math.cos(ang) * d, oy + Math.sin(ang) * d, sz * 0.05, sz * 0.11 * f, ang + Math.PI / 2,
                            i % 2 == 0 ? hot : new Color(0xFF8C1A), Color.WHITE, alpha);
                }
                glow(g, hot, ox, oy, sz * (0.05 + 0.12 * chargeP), alpha);
                glow(g, new Color(0xFFA51F), mx + sz * 0.05, my - sz * 0.16, sz * 0.08 * chargeP, alpha);
                line(g, mx + sz * 0.05, my - sz * 0.10, mx + sz * 0.10, my - sz * 0.32, 3f, new Color(0xFFE6B0), alpha * (float) chargeP);
            }
            case "U1" -> {        // 4 khe mang cổ rực cam lần lượt, hơi bốc lên, giọt nước bay khỏi mặt biển
                for (int k = 0; k < 4; k++) {
                    double f = clamp((chargeP - k * 0.15) / 0.3);
                    if (f <= 0) continue;
                    double x = cx - sz * 0.20 + k * sz * 0.035, y = cy - sz * 0.10 + k * sz * 0.035;
                    line(g, x, y - sz * 0.03, x + sz * 0.01, y + sz * 0.03, 4f, hot, alpha * (float) f);
                    glow(g, mid, x, y, sz * 0.05 * f, alpha);
                    for (int p = 0; p < 3; p++) {
                        double rise = ((now * 0.0012 + p * 0.33 + k * 0.1) % 1.0);
                        block(g, x - sz * 0.02 - rise * sz * 0.05, y - rise * sz * 0.2, (int) (6 - rise * 4), Color.WHITE, alpha * (float) ((1 - rise) * f));
                    }
                }
                for (int d = 0; d < 14; d++) {
                    double lift = (chargeP * 48 + (vacuum ? 30 : 0)) * (0.4 + (d % 3) * 0.3);
                    block(g, cx - sz * 0.9 + d * sz * 0.09, seaY - lift, 5, new Color(0x7DD3FC), alpha * 0.7f);
                }
            }
            case "U2" -> {        // khe dung nham bò khắp thân từ tâm ra và nứt xuống mặt biển
                for (int k = 0; k < 7; k++) {
                    double ang = k * 0.9 + 0.3;
                    double x = cx, y = cy;
                    int steps = (int) (8 * chargeP);
                    for (int i = 0; i < steps; i++) {
                        double nx = x + Math.cos(ang + (r.nextDouble() - 0.5) * 1.2) * sz * 0.05;
                        double ny = y + Math.sin(ang + (r.nextDouble() - 0.5) * 1.2) * sz * 0.04;
                        line(g, x, y, nx, ny, 4f, new Color(0x3A1208), alpha);
                        line(g, x, y, nx, ny, 2f, i < 3 ? hot : mid, alpha);
                        x = nx; y = ny;
                    }
                }
                glow(g, mid, cx, cy, sz * 0.3 * chargeP, alpha * 0.6f);
                for (int k = 0; k < 5; k++) {
                    double x0 = cx - sz * 0.2 - k * sz * 0.16;
                    line(g, x0, seaY + 4, x0 - sz * 0.12 * chargeP, seaY + 10 + k * 3, 3f, mid, alpha * (float) chargeP);
                }
            }
            case "U3", "U4" -> {  // gai lưng sáng từng đốt từ đuôi lên đầu: III lõi băng + tuyết, IV đỏ + vòng nhịp tim
                boolean ice = "U3".equals(style(s));
                for (int i = 0; i < 8; i++) {
                    double f = clamp((chargeP - i * 0.08) / 0.25);
                    if (f <= 0 && !vacuum) continue;
                    double u = i / 7.0;
                    double x = cx + sz * (0.24 - u * 0.30), y = cy + sz * (0.12 - u * 0.42);
                    double h = sz * (0.07 + 0.05 * Math.sin(u * Math.PI)) * (vacuum ? 1.15 : f);
                    glow(g, mid, x, y - h * 0.5, sz * 0.06, alpha * 0.85f);
                    shard(g, x, y - h * 0.5, sz * 0.035, h, -0.45, ice ? new Color(0xC3C8D2) : new Color(0xD3D6DD), hot, alpha);
                    if (i < 7 && f > 0.5) {
                        double u2 = (i + 1) / 7.0;
                        line(g, x, y, cx + sz * (0.24 - u2 * 0.30), cy + sz * (0.12 - u2 * 0.42), 2f, mid, alpha * 0.6f);
                    }
                }
                if (ice) {
                    for (int k = 0; k < 18; k++) {
                        double fall = ((now * 0.0004 + k * 0.137) % 1.0);
                        block(g, cx - sz + (k * 0.11 % 1.0) * sz * 2, cy - sz * 0.6 + fall * sz * 1.2, 3, Color.WHITE, alpha * 0.7f * (float) chargeP);
                    }
                } else {
                    double beat = (now % 500L) / 500.0;
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(alpha * (float) (1 - beat) * (float) chargeP)));
                    g.setColor(mid);
                    g.setStroke(new BasicStroke(3f));
                    double rr = sz * (0.15 + beat * 0.5);
                    g.drawOval((int) (cx - rr), (int) (cy - rr * 0.8), (int) (rr * 2), (int) (rr * 1.6));
                }
            }
            default -> { }
        }
    }

    /** Màn 2 · phóng chiêu từ (fx, fy) tới đầu chiêu (ex, ey); p 0→1. */
    static void release(Graphics2D g, TitanMode.TitanSkill s, double fx, double fy, double ex, double ey, int seaY,
                        double p, Color dark, Color mid, Color hot, float alpha, long now) {
        double dx = ex - fx, dy = ey - fy, len = Math.max(1, Math.hypot(dx, dy));
        double nx = -dy / len, ny = dx / len;
        switch (style(s)) {
            case "SG" -> {        // tia corona: lõi trắng + hai dải xoắn tím/lam, mảnh tinh thể bay dọc tia
                line(g, fx, fy, ex, ey, 40f, dark, alpha * 0.4f);
                line(g, fx, fy, ex, ey, 14f, Color.WHITE, alpha);
                for (int i = 0; i <= 44; i++) {
                    double u = i / 44.0;
                    for (int k = 0; k < 2; k++) {
                        double off = Math.sin(u * 18 - now * 0.03 + k * Math.PI) * 26;
                        block(g, fx + dx * u + nx * off, fy + dy * u + ny * off, 7, k == 0 ? mid : new Color(0x38BDF8), alpha);
                    }
                }
                for (int i = 0; i < 8; i++) {
                    double u = ((now * 0.0016 + i / 8.0) % 1.0);
                    shard(g, fx + dx * u + nx * (i % 2 == 0 ? 46 : -46), fy + dy * u + ny * (i % 2 == 0 ? 46 : -46), 9, 22, Math.atan2(dy, dx) + Math.PI / 2, mid, hot, alpha);
                }
            }
            case "DS" -> {        // tia oxy vi thể răng cưa + vệt sừng chém chéo màn hình
                Random r = new Random(now / 40);
                double px = fx, py = fy;
                for (int i = 1; i <= 16; i++) {
                    double u = i / 16.0;
                    double jx = fx + dx * u + nx * (r.nextDouble() - 0.5) * 30, jy = fy + dy * u + ny * (r.nextDouble() - 0.5) * 30;
                    line(g, px, py, jx, jy, 30f, dark, alpha * 0.5f);
                    line(g, px, py, jx, jy, 14f, new Color(0xF472B6), alpha * 0.9f);
                    line(g, px, py, jx, jy, 5f, Color.WHITE, alpha);
                    px = jx; py = jy;
                }
                double sl = clamp(p / 0.6);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(alpha * (float) (1 - p * 0.6))));
                g.setColor(hot);
                g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int R = (int) (len * 0.9);
                g.drawArc((int) (ex - R), (int) (ey - R * 1.4), R * 2, R * 2, 200, (int) (-120 * sl));
            }
            case "U1" -> {        // hai thác hơi bỏng xoè chữ V từ miệng xuống biển
                for (int k = -1; k <= 1; k += 2) {
                    double tx = ex + nx * k * 70, ty = ey + ny * k * 70;
                    for (int i = 0; i < 26; i++) {
                        double u = ((now * 0.0018 + i / 26.0) % 1.0) * clamp(p / 0.25);
                        double x = fx + (tx - fx) * u, y = fy + (ty - fy) * u;
                        int sz = (int) (10 + u * 34);
                        block(g, x, y, sz, i % 3 == 0 ? new Color(0xFDBA74) : Color.WHITE, alpha * (float) (0.9 - u * 0.5));
                    }
                }
                glow(g, mid, fx, fy, 60, alpha);
            }
            case "U2" -> {        // sóng dung nham cuộn trên mặt biển từ chân boss tới mục tiêu, cột nham phun dọc đường
                double reach = clamp(p / 0.5);
                double x0 = fx, x1 = fx + (ex - fx) * reach;
                for (int i = 0; i < 18; i++) {
                    double u = i / 17.0;
                    double x = x0 + (x1 - x0) * u;
                    double h = 20 + 50 * Math.abs(Math.sin(u * 9 + now * 0.01)) * (1 - u * 0.3);
                    line(g, x, seaY, x, seaY - h, 14f, mid, alpha);
                    block(g, x, seaY - h, 10, new Color(0xFDE047), alpha);
                }
                line(g, x0, seaY + 2, x1, seaY + 2, 18f, new Color(0x3A1208), alpha);
                glow(g, hot, x1, seaY - 20, 70, alpha);
            }
            case "U3" -> {        // vòng sóng âm băng lan ra + tia băng hà dày có mảnh băng
                for (int k = 0; k < 6; k++) {
                    double f = ((p * 1.6 + k / 6.0) % 1.0);
                    double rad = len * f;
                    double ang = Math.toDegrees(Math.atan2(-dy, dx));
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(alpha * (float) (1 - f))));
                    g.setColor(k % 2 == 0 ? mid : Color.WHITE);
                    g.setStroke(new BasicStroke(5f));
                    g.drawArc((int) (fx - rad), (int) (fy - rad), (int) (rad * 2), (int) (rad * 2), (int) (ang - 30), 60);
                }
                line(g, fx, fy, ex, ey, 34f, new Color(0xCFFAFE), alpha * 0.4f);
                line(g, fx, fy, ex, ey, 16f, mid, alpha * 0.9f);
                line(g, fx, fy, ex, ey, 6f, Color.WHITE, alpha);
                for (int i = 0; i < 7; i++) {
                    double u = 0.2 + i * 0.11;
                    shard(g, fx + dx * u + nx * 18, fy + dy * u + ny * 18, 10, 26, Math.atan2(dy, dx), new Color(0xA5F3FC), Color.WHITE, alpha);
                }
            }
            default -> {          // IV: tia huyết hạch 4 lớp, ★ xoắn đôi thêm hai dải đỏ sẫm quấn quanh
                line(g, fx, fy, ex, ey, 62f, dark, alpha * 0.45f);
                line(g, fx, fy, ex, ey, 34f, mid, alpha * 0.85f);
                line(g, fx, fy, ex, ey, 16f, new Color(0xFF8A80), alpha);
                line(g, fx, fy, ex, ey, 7f, Color.WHITE, alpha);
                if (s == TitanMode.TitanSkill.UL4_BLOOD_SPIRAL || s == TitanMode.TitanSkill.UL4_FINAL_ROAR) {
                    for (int i = 0; i <= 40; i++) {
                        double u = i / 40.0;
                        for (int k = 0; k < 2; k++) {
                            double off = Math.sin(u * 20 - now * 0.035 + k * Math.PI) * 34;
                            block(g, fx + dx * u + nx * off, fy + dy * u + ny * off, 8, k == 0 ? hot : new Color(0x450A0A), alpha);
                        }
                    }
                }
            }
        }
        glow(g, hot, fx, fy, 50, alpha * 0.9f);
    }

    /** Màn 3 · hậu quả slow-mo quanh điểm trúng (ix, seaY); sp 0→1 đã làm chậm. */
    static void cataclysm(Graphics2D g, TitanMode.TitanSkill s, double ix, int seaY, int bw, int bh,
                          double sp, Color dark, Color mid, Color hot, float alpha, long now) {
        Random r = new Random(s.ordinal() * 97L);
        switch (style(s)) {
            case "SG" -> {        // pháo đài tinh thể trồi quanh tâm + siêu tân tinh
                for (int i = 0; i < 9; i++) {
                    double side = (i - 4) / 4.0;
                    double x = ix + side * bh * 0.55;
                    double h = bh * (0.22 + 0.18 * (1 - Math.abs(side))) * clamp(sp * 1.6 - Math.abs(side) * 0.3);
                    if (h <= 2) continue;
                    shard(g, x, seaY - h / 2, bh * 0.07, h, side * 0.18, mid, hot, alpha);
                }
                double sr = bh * (0.08 + 0.22 * sp);
                glow(g, mid, ix, seaY - bh * 0.3, sr * 2, alpha * 0.85f);
                glow(g, Color.WHITE, ix, seaY - bh * 0.3, sr, alpha);
                line(g, ix - sr * 2.4, seaY - bh * 0.3, ix + sr * 2.4, seaY - bh * 0.3, 5f, Color.WHITE, alpha);
                line(g, ix, seaY - bh * 0.3 - sr * 1.4, ix, seaY - bh * 0.3 + sr * 1.4, 5f, Color.WHITE, alpha);
            }
            case "DS" -> {        // vòm sương oxy đen đỏ nở ra, hạt bị phân rã bay lên, chớp chữ X
                double rr = bh * (0.15 + 0.4 * sp);
                glow(g, dark, ix, seaY - rr * 0.4, rr * 1.4, alpha * 0.95f);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(alpha * 0.8f)));
                g.setColor(mid);
                g.setStroke(new BasicStroke(4f));
                g.drawArc((int) (ix - rr * 1.3), (int) (seaY - rr * 1.2), (int) (rr * 2.6), (int) (rr * 2.4), 0, 180);
                for (int i = 0; i < 40; i++) {
                    double x = ix + (r.nextDouble() - 0.5) * rr * 2.4;
                    double y = seaY - r.nextDouble() * rr - sp * bh * 0.3 * r.nextDouble();
                    block(g, x, y, 4 + r.nextInt(4), i % 3 == 0 ? hot : new Color(0xF472B6), alpha * (float) (1 - sp * 0.6));
                }
                double xl = bh * (0.2 + 0.3 * sp);
                line(g, ix - xl, seaY - bh * 0.3 - xl, ix + xl, seaY - bh * 0.3 + xl, 7f, hot, alpha * (float) (1 - sp * 0.5));
                line(g, ix - xl, seaY - bh * 0.3 + xl, ix + xl, seaY - bh * 0.3 - xl, 7f, hot, alpha * (float) (1 - sp * 0.5));
            }
            case "U1" -> {        // cột nước khổng lồ + vòng sóng thần
                for (int i = 0; i < 5; i++) {
                    double x = ix + (i - 2) * bh * 0.2;
                    double h = bh * (0.3 + 0.25 * (i % 2)) * clamp(sp * 1.5);
                    line(g, x, seaY, x, seaY - h, 30f, new Color(0x7DD3FC), alpha * 0.9f);
                    line(g, x, seaY, x, seaY - h, 12f, Color.WHITE, alpha);
                    glow(g, Color.WHITE, x, seaY - h, 30, alpha);
                }
                double rr = bh * (0.2 + 0.6 * sp);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(alpha * (float) (1 - sp * 0.5))));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(6f));
                g.drawOval((int) (ix - rr * 1.6), (int) (seaY - rr * 0.25), (int) (rr * 3.2), (int) (rr * 0.5));
            }
            case "U2" -> {        // núi lửa đáy biển: cột dung nham + đá văng + vòng nứt
                for (int i = 0; i < 6; i++) {
                    double x = ix + (i - 2.5) * bh * 0.14;
                    double h = bh * (0.25 + 0.3 * r.nextDouble()) * clamp(sp * 1.4);
                    line(g, x, seaY, x, seaY - h, 22f, mid, alpha);
                    line(g, x, seaY, x, seaY - h, 8f, new Color(0xFDE047), alpha);
                }
                for (int i = 0; i < 16; i++) {
                    double ang = -Math.PI * r.nextDouble();
                    double d = bh * 0.6 * sp * (0.5 + r.nextDouble());
                    block(g, ix + Math.cos(ang) * d, seaY + Math.sin(ang) * d * 0.8, 8 + r.nextInt(6), new Color(0x2A0E05), alpha);
                }
                glow(g, hot, ix, seaY - bh * 0.1, bh * 0.25 * (0.5 + sp), alpha * 0.8f);
            }
            case "U3" -> {        // rừng gai băng trồi lên + vòm giá lạnh + tuyết
                glow(g, new Color(0xE0F2FE), ix, seaY - bh * 0.15, bh * (0.2 + 0.4 * sp), alpha * 0.6f);
                for (int i = 0; i < 14; i++) {
                    double x = ix + (r.nextDouble() - 0.5) * bh * 1.4;
                    double h = bh * (0.1 + 0.25 * r.nextDouble()) * clamp(sp * 1.8 - Math.abs(x - ix) / (bh * 1.2));
                    if (h <= 2) continue;
                    shard(g, x, seaY - h / 2, bh * 0.05, h, (r.nextDouble() - 0.5) * 0.5, mid, Color.WHITE, alpha);
                }
                for (int k = 0; k < 30; k++) {
                    double fall = ((now * 0.0005 + k * 0.071) % 1.0);
                    block(g, ix - bh + (k * 0.137 % 1.0) * bh * 2, seaY - bh * 0.7 + fall * bh * 0.7, 3, Color.WHITE, alpha * 0.8f);
                }
            }
            default -> {          // IV: vòng xung kích kim cương đỏ chồng lên nấm lửa (nấm do CombatFxOverlay vẽ)
                for (int k = 0; k < 3; k++) {
                    double rad = bh * (0.15 + sp * 0.7) * (1 - k * 0.2);
                    Polygon d = new Polygon();
                    d.addPoint((int) ix, (int) (seaY - rad * 0.6));
                    d.addPoint((int) (ix + rad * 1.5), seaY);
                    d.addPoint((int) ix, (int) (seaY + rad * 0.3));
                    d.addPoint((int) (ix - rad * 1.5), seaY);
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(alpha * (float) (1 - sp * 0.6) * (1f - k * 0.25f))));
                    g.setColor(k == 0 ? mid : hot);
                    g.setStroke(new BasicStroke(5f - k));
                    g.drawPolygon(d);
                }
            }
        }
    }
}
