package battleship.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import static battleship.ui.KaijuSkillFx.block;
import static battleship.ui.KaijuSkillFx.clamp;
import static battleship.ui.KaijuSkillFx.clampF;
import static battleship.ui.KaijuSkillFx.glow;
import static battleship.ui.KaijuSkillFx.line;
import static battleship.ui.KaijuSkillFx.shard;

/**
 * Cảnh xuất hiện (POV cậu bé, 4,8s) riêng cho 3 boss mới, theo khuôn 4 nhịp của Godzilla trong {@link CombatFxOverlay}:
 * nhịp 1 (0–0,28) boss tới · nhịp 2 (0,28–0,55) tụ lực trên thân · nhịp 3 (0,55–0,83) đòn quét qua thành phố ·
 * nhịp 4 thẻ tên (CombatFxOverlay vẽ). CombatFxOverlay lo khung, phố, cậu bé, HUD; lớp này vẽ phần riêng của từng boss.
 */
final class KaijuEntranceFx {
    private KaijuEntranceFx() { }

    static boolean has(TitanMode.TitanBoss b) {
        return b == TitanMode.TitanBoss.SPACEGODZILLA || b == TitanMode.TitanBoss.DESTOROYAH || b == TitanMode.TitanBoss.ULTIMA;
    }

    /** Độ rung máy quay theo nhịp riêng: SG đáp đất, DS trồi lên ầm ì, Ultima tiến hóa giật từng cơn. */
    static double tremor(TitanMode.TitanBoss b, double t) {
        double amp = (t >= 0.55 && t <= 0.83) ? 8.0 : 0.0;
        double[] hits = switch (b) {
            case SPACEGODZILLA -> new double[]{0.30};
            case DESTOROYAH -> new double[]{0.40, 0.48};
            default -> new double[]{0.10, 0.18, 0.26, 0.31};
        };
        for (double h : hits) {
            double dt = Math.abs(t - h);
            if (dt < 0.05) amp = Math.max(amp, (1.0 - dt / 0.05) * (h >= 0.30 ? 13.0 : 8.0));
        }
        if (b == TitanMode.TitanBoss.DESTOROYAH && t < 0.34) amp = Math.max(amp, t / 0.34 * 7.0);
        return amp;
    }

    /** Bầu trời riêng (vẽ sau nền chung, trước thân boss). */
    static void sky(Graphics2D g, TitanMode.TitanBoss b, int w, int bandY, int bandH, int waterY, double t, float alpha, long now) {
        switch (b) {
            case SPACEGODZILLA -> {      // trời vũ trụ tím: sao và dải cực quang tinh thể
                comp(g, alpha * 0.55f);
                g.setPaint(new GradientPaint(0, bandY, new Color(0x1E0B3A), 0, waterY, new Color(0x3B0764)));
                g.fillRect(0, bandY, w, waterY - bandY);
                Random r = new Random(7);
                for (int i = 0; i < 60; i++) {
                    float tw = (float) (0.5 + 0.5 * Math.sin(now * 0.006 + i));
                    block(g, r.nextInt(w), bandY + r.nextInt(Math.max(1, waterY - bandY)), 2, Color.WHITE, alpha * 0.7f * tw);
                }
                for (int k = 0; k < 3; k++) {
                    double y0 = bandY + bandH * (0.10 + k * 0.08);
                    for (int x = 0; x < w; x += 14) {
                        double y = y0 + Math.sin(x * 0.01 + now * 0.002 + k) * 18;
                        block(g, x, y, 10, k == 1 ? new Color(0x38BDF8) : new Color(0xC084FC), alpha * 0.18f);
                    }
                }
            }
            case DESTOROYAH -> {         // sương đỏ dâng từ mặt biển, biển sủi bọt
                double fog = clamp(t / 0.4);
                comp(g, alpha * 0.6f * (float) fog);
                g.setPaint(new GradientPaint(0, bandY, new Color(0, 0, 0, 0), 0, waterY, new Color(0x7F1D1D)));
                g.fillRect(0, bandY, w, waterY - bandY);
                Random r = new Random(11);
                for (int i = 0; i < 40; i++) {
                    double rise = ((now * 0.0006 + r.nextDouble()) % 1.0);
                    block(g, r.nextInt(w), waterY - rise * bandH * 0.25, 4 + r.nextInt(4), new Color(0xF87171), alpha * (float) ((1 - rise) * 0.6));
                }
            }
            default -> {                 // Ultima: bão biển, đáy biển rực cam đang sôi, chớp xa
                comp(g, alpha * 0.5f);
                g.setPaint(new GradientPaint(0, bandY, new Color(0x0F172A), 0, waterY, new Color(0x134E4A)));
                g.fillRect(0, bandY, w, waterY - bandY);
                glow(g, new Color(0xF97316), w * 0.5, waterY + bandH * 0.08, bandH * (0.25 + 0.25 * clamp(t / 0.3)), alpha * 0.5f);
                if (Math.sin(now * 0.013) > 0.93) {
                    comp(g, alpha * 0.25f);
                    g.setColor(new Color(0xE0F2FE));
                    g.fillRect(0, bandY, w, waterY - bandY);
                }
            }
        }
    }

    /** Thân boss xuất hiện và tụ lực; trả về khung sprite {x, y, w, h} để đòn nhịp 3 bám theo. */
    static double[] body(Graphics2D g, TitanMode.TitanBoss b, int w, int bandY, int bandH, double t, float alpha, long now) {
        int waterY = bandY + (int) (bandH * 0.72);
        return switch (b) {
            case SPACEGODZILLA -> spaceGodzilla(g, w, bandY, bandH, waterY, t, alpha, now);
            case DESTOROYAH -> destoroyah(g, w, bandY, bandH, waterY, t, alpha, now);
            default -> ultima(g, w, bandY, bandH, waterY, t, alpha, now);
        };
    }

    private static double[] place(BufferedImage img, int w, int bandY, int bandH, double scale, double dy) {
        double aspect = img != null ? img.getWidth() / (double) img.getHeight() : 1.4;
        int h = (int) Math.round(bandH * 0.86 * scale);
        int wd = (int) Math.round(h * aspect);
        return new double[]{(w - wd) / 2.0, bandY + bandH - h * 0.90 + dy, wd, h};
    }

    private static void draw(Graphics2D g, BufferedImage img, double[] r, float alpha) {
        if (img == null || alpha <= 0.01f) return;
        comp(g, alpha);
        SpriteAssets.drawSharp(g, img, (int) r[0], (int) r[1], (int) r[2], (int) r[3]);
    }

    /** SG: thiên thạch tinh thể rơi từ quỹ đạo, nổ vỏ, rồi cột vai và vương miện tụ sáng. */
    private static double[] spaceGodzilla(Graphics2D g, int w, int bandY, int bandH, int waterY, double t, float alpha, long now) {
        BufferedImage img = SpriteAssets.getRampageSprite(TitanMode.TitanBoss.SPACEGODZILLA);
        double land = 0.30;
        double scale = t < land ? 0.75 : 0.75 + 0.25 * clamp((t - land) / 0.12);
        double[] r = place(img, w, bandY, bandH, scale, Math.sin(now * 0.006) * 3);
        if (t < land) {
            double f = Math.pow(clamp(t / land), 1.6);
            double sx = w * 0.95, sy = bandY - 40, ex = w * 0.5, ey = waterY - bandH * 0.25;
            double x = sx + (ex - sx) * f, y = sy + (ey - sy) * f;
            for (int k = 1; k <= 14; k++) {
                double u = Math.max(0, f - k * 0.025);
                shard(g, sx + (ex - sx) * u, sy + (ey - sy) * u, 10 - k * 0.5, 22 - k, -0.9, new Color(0xC084FC), Color.WHITE, alpha * (1f - k / 15f));
            }
            glow(g, new Color(0xE9D5FF), x, y, 60 + 40 * f, alpha);
            shard(g, x, y, 46, 90, -0.9, new Color(0xA855F7), Color.WHITE, alpha);
            return r;
        }
        double burst = clamp((t - land) / 0.15);
        if (burst < 1) {
            for (int i = 0; i < 16; i++) {
                double ang = i * Math.PI / 8;
                double d = bandH * 0.5 * burst;
                shard(g, w * 0.5 + Math.cos(ang) * d * 1.4, waterY - bandH * 0.25 + Math.sin(ang) * d, 12, 26, ang, new Color(0xC084FC), Color.WHITE, alpha * (float) (1 - burst));
            }
            glow(g, Color.WHITE, w * 0.5, waterY - bandH * 0.25, bandH * 0.4 * (1 - burst), alpha);
        }
        draw(g, img, r, alpha);
        if (t >= 0.30 && t < 0.62) {
            double c = clamp((t - 0.30) / 0.25);
            double[][] pts = {{0.62, 0.22}, {0.47, 0.12}, {0.40, 0.20}};
            for (int i = 0; i < pts.length; i++) {
                double k = clamp(c * 1.4 - i * 0.2);
                glow(g, i == 2 ? new Color(0xFDBA74) : new Color(0xE9D5FF), r[0] + r[2] * pts[i][0], r[1] + r[3] * pts[i][1], 20 + 40 * k, alpha * (float) k);
            }
            for (int i = 0; i < 10; i++) {
                double ang = now * 0.002 + i * Math.PI / 5;
                shard(g, r[0] + r[2] * 0.5 + Math.cos(ang) * r[2] * 0.45, r[1] + r[3] * 0.4 + Math.sin(ang) * r[3] * 0.18,
                        8, 18, ang, new Color(0xC084FC), Color.WHITE, alpha * (float) c);
            }
        }
        return r;
    }

    /** DS: trồi lên khỏi mặt biển giữa sương đỏ, rồi lõi ngực nở 8 cánh và sừng sáng. */
    private static double[] destoroyah(Graphics2D g, int w, int bandY, int bandH, int waterY, double t, float alpha, long now) {
        BufferedImage img = SpriteAssets.getRampageSprite(TitanMode.TitanBoss.DESTOROYAH);
        double rise = 1 - Math.pow(1 - clamp(t / 0.34), 2.2);
        double[] r0 = place(img, w, bandY, bandH, 1.0, 0);
        double[] r = {r0[0], r0[1] + r0[3] * 0.75 * (1 - rise) + Math.sin(now * 0.007) * 3, r0[2], r0[3]};
        draw(g, img, r, alpha);
        if (rise < 1) {                  // nước đổ xuống khỏi thân khi trồi lên
            for (int i = 0; i < 20; i++) {
                double x = r[0] + r[2] * (0.25 + (i * 0.037) % 0.5);
                double fall = ((now * 0.002 + i * 0.13) % 1.0);
                block(g, x, waterY - fall * bandH * 0.2, 5, new Color(0xBAE6FD), alpha * (float) (1 - rise));
            }
        }
        if (t >= 0.30 && t < 0.62) {
            double c = clamp((t - 0.30) / 0.22);
            double cx = r[0] + r[2] * 0.42, cy = r[1] + r[3] * 0.55;
            for (int i = 0; i < 8; i++) {
                double k = clamp(c * 1.6 - i * 0.08);
                double ang = i * Math.PI / 4 - Math.PI / 2;
                double d = r[3] * (0.02 + 0.05 * k);
                shard(g, cx + Math.cos(ang) * d, cy + Math.sin(ang) * d, 9, 22 * k + 2, ang + Math.PI / 2,
                        i % 2 == 0 ? new Color(0xFDE047) : new Color(0xFF8C1A), Color.WHITE, alpha * (float) k);
            }
            glow(g, new Color(0xFDE047), cx, cy, 20 + 50 * c, alpha * (float) c);
            glow(g, new Color(0xFFA51F), r[0] + r[2] * 0.33, r[1] + r[3] * 0.17, 14 + 30 * c, alpha * (float) c);
        }
        return r;
    }

    /** Ultima: bóng form IV, III, II chớp dưới biển như đang tiến hóa, rồi form I vọt khỏi mặt nước và mang lửa rực. */
    private static double[] ultima(Graphics2D g, int w, int bandY, int bandH, int waterY, double t, float alpha, long now) {
        Color[] formGlow = {null, new Color(0xFF7B39), new Color(0xFF7A1A), new Color(0x5EEAD4), new Color(0xFF3B30)};
        int[] ghosts = {4, 3, 2};
        for (int i = 0; i < ghosts.length; i++) {
            double t0 = 0.05 + i * 0.08;
            double f = (t - t0) / 0.08;
            if (f < 0 || f > 1) continue;
            float a = alpha * (float) Math.sin(f * Math.PI) * 0.55f;
            BufferedImage gimg = SpriteAssets.getUltimaStageSprite(ghosts[i]);
            double[] gr = place(gimg, w, bandY, bandH, 0.85, bandH * 0.10);
            glow(g, formGlow[ghosts[i]], gr[0] + gr[2] * 0.5, gr[1] + gr[3] * 0.5, gr[3] * 0.5, a);
            draw(g, gimg, gr, a);
        }
        BufferedImage img = SpriteAssets.getUltimaStageSprite(1);
        double[] r0 = place(img, w, bandY, bandH, 0.95, -bandH * 0.16);   // thân thủy long nằm thấp: nâng lên khỏi dãy nhà
        if (t < 0.29) return r0;
        double up = 1 - Math.pow(1 - clamp((t - 0.29) / 0.12), 2.5);
        double[] r = {r0[0], r0[1] + r0[3] * 0.6 * (1 - up) + Math.sin(now * 0.006) * 3, r0[2], r0[3]};
        draw(g, img, r, alpha);
        if (t < 0.45) {                  // cột nước vỡ tung khi vọt lên
            double sp = clamp((t - 0.29) / 0.16);
            for (int i = 0; i < 7; i++) {
                double x = r[0] + r[2] * (0.1 + i * 0.13);
                double h = bandH * 0.35 * Math.sin(sp * Math.PI) * (0.6 + (i % 3) * 0.2);
                line(g, x, waterY, x, waterY - h, 16f, new Color(0x7DD3FC), alpha * 0.8f);
                line(g, x, waterY, x, waterY - h, 6f, Color.WHITE, alpha);
            }
        }
        if (t >= 0.36 && t < 0.62) {
            double c = clamp((t - 0.36) / 0.18);
            for (int k = 0; k < 4; k++) {    // khe mang trên cổ
                double kk = clamp(c * 1.5 - k * 0.15);
                glow(g, formGlow[1], r[0] + r[2] * (0.28 + k * 0.025), r[1] + r[3] * (0.42 + k * 0.025), 10 + 18 * kk, alpha * (float) kk);
            }
            for (int k = 0; k < 8; k++) {    // gai lưng cam sáng từ đầu ra đuôi
                double kk = clamp(c * 1.5 - k * 0.1);
                glow(g, new Color(0xFF9F5A), r[0] + r[2] * (0.42 + k * 0.065), r[1] + r[3] * 0.27, 12 + 16 * kk, alpha * (float) kk);
            }
        }
        return r;
    }

    /** Nhịp 3: đòn quét qua phố (vẽ trước mặt các toà nhà). */
    static void beams(Graphics2D g, TitanMode.TitanBoss b, double[] r, int w, int bandY, int bandH, double t, float alpha, long now) {
        if (t < 0.55 || t > 0.83) return;
        double sweep = clamp((t - 0.55) / 0.28 * 1.15);
        double endX = w * (0.14 + 0.74 * sweep);
        double endY = bandY + bandH * (0.64 - 0.28 * Math.sin(sweep * Math.PI));
        switch (b) {
            case SPACEGODZILLA -> {
                double mx = r[0] + r[2] * 0.33, my = r[1] + r[3] * 0.37;
                double dx = endX - mx, dy = endY - my, len = Math.max(1, Math.hypot(dx, dy)), nx = -dy / len, ny = dx / len;
                line(g, mx, my, endX, endY, 30f, new Color(0x3B0764), alpha * 0.5f);
                line(g, mx, my, endX, endY, 10f, Color.WHITE, alpha);
                for (int i = 0; i <= 40; i++) {
                    double u = i / 40.0;
                    for (int k = 0; k < 2; k++) {
                        double off = Math.sin(u * 16 - now * 0.03 + k * Math.PI) * 18;
                        block(g, mx + dx * u + nx * off, my + dy * u + ny * off, 6, k == 0 ? new Color(0xC084FC) : new Color(0x38BDF8), alpha);
                    }
                }
                for (int k = 0; k < 5; k++) {   // cột tinh thể mọc lên dọc đường quét
                    double sk = k / 4.0;
                    if (sweep < sk) continue;
                    double grow = clamp((sweep - sk) * 4);
                    double x = w * (0.14 + 0.74 * sk), y = bandY + bandH * (0.64 - 0.28 * Math.sin(sk * Math.PI));
                    shard(g, x, y - 40 * grow, 26, 90 * grow, 0, new Color(0xA855F7), Color.WHITE, alpha);
                }
                glow(g, new Color(0xE9D5FF), endX, endY, 70, alpha);
            }
            case DESTOROYAH -> {
                double mx = r[0] + r[2] * 0.35, my = r[1] + r[3] * 0.37;
                Random rr = new Random(now / 40);
                double px = mx, py = my;
                for (int i = 1; i <= 14; i++) {
                    double u = i / 14.0;
                    double jx = mx + (endX - mx) * u + (rr.nextDouble() - 0.5) * 26, jy = my + (endY - my) * u + (rr.nextDouble() - 0.5) * 26;
                    line(g, px, py, jx, jy, 22f, new Color(0x7F1D1D), alpha * 0.6f);
                    line(g, px, py, jx, jy, 10f, new Color(0xF472B6), alpha);
                    line(g, px, py, jx, jy, 3f, Color.WHITE, alpha);
                    px = jx; py = jy;
                }
                glow(g, new Color(0xF472B6), endX, endY, 80, alpha);
                if (t >= 0.70) {               // sừng chém chéo cả khung hình
                    double s = clamp((t - 0.70) / 0.06);
                    double x0 = w * 0.92, y0 = bandY + 10, x1 = w * 0.92 - (w * 0.84) * s, y1 = y0 + (bandH - 20) * s;
                    line(g, x0, y0, x1, y1, 18f, new Color(0xFBBF24), alpha * 0.5f);
                    line(g, x0, y0, x1, y1, 5f, new Color(0xFFFBEB), alpha);
                }
            }
            default -> {                       // Ultima form I: hai thác hơi bỏng xoè chữ V quét bến cảng
                double mx = r[0] + r[2] * 0.12, my = r[1] + r[3] * 0.42;
                for (int k = -1; k <= 1; k += 2) {
                    double tx = endX + k * 60, ty = endY + k * 30;
                    for (int i = 0; i < 24; i++) {
                        double u = ((now * 0.0018 + i / 24.0) % 1.0);
                        int sz = (int) (8 + u * 30);
                        block(g, mx + (tx - mx) * u, my + (ty - my) * u - Math.sin(u * Math.PI) * 20, sz,
                                i % 3 == 0 ? new Color(0xFDBA74) : Color.WHITE, alpha * (float) (0.95 - u * 0.45));
                    }
                }
                glow(g, new Color(0xFF7B39), mx, my, 50, alpha);
                glow(g, new Color(0xFFE4C4), endX, endY, 80, alpha);
            }
        }
    }

    /** Mảnh vỡ trong phố: tinh thể rơi (SG), hạt oxy tan rã bay lên (DS), bụi nước và tàn cam (Ultima). */
    static void debris(Graphics2D g, TitanMode.TitanBoss b, int w, int bandY, int bandH, double t, float alpha, long now) {
        if (t < 0.30 || t > 0.92) return;
        float a = alpha * (float) clamp((t - 0.30) / 0.1);
        Random r = new Random(b.ordinal() * 13L);
        for (int i = 0; i < 22; i++) {
            double x0 = w * (0.08 + r.nextDouble() * 0.84);
            double ph = ((now * 0.0005 + r.nextDouble()) % 1.0);
            switch (b) {
                case SPACEGODZILLA -> shard(g, x0 - ph * 60, bandY + ph * bandH, 6, 14, -0.5, new Color(0xC084FC), Color.WHITE, a * (float) (1 - ph * 0.5));
                case DESTOROYAH -> block(g, x0, bandY + bandH * (0.8 - ph * 0.7), 4 + (i % 3) * 2,
                        i % 3 == 0 ? new Color(0x3F1D1D) : new Color(0xF87171), a * (float) (1 - ph));
                default -> block(g, x0, bandY + bandH * (0.75 - ph * 0.5), 3 + (i % 3) * 2,
                        i % 2 == 0 ? new Color(0xE0F2FE) : new Color(0xFB923C), a * (float) (1 - ph));
            }
        }
    }

    /** Thời tiết tiền cảnh: bụi tinh thể lấp lánh (SG), dải sương đỏ sát đất (DS), mưa bão biển (Ultima). */
    static void weather(Graphics2D g, TitanMode.TitanBoss b, int w, int bandY, int bandH, double t, float alpha, long now) {
        switch (b) {
            case SPACEGODZILLA -> {
                Random r = new Random(3);
                for (int i = 0; i < 30; i++) {
                    float tw = (float) Math.max(0, Math.sin(now * 0.01 + i * 1.7));
                    block(g, r.nextInt(w), bandY + r.nextInt(bandH), 3, i % 2 == 0 ? Color.WHITE : new Color(0xE9D5FF), alpha * tw * 0.8f);
                }
            }
            case DESTOROYAH -> {
                for (int k = 0; k < 3; k++) {
                    double y = bandY + bandH * (0.82 + k * 0.05);
                    double shift = (now * 0.03 * (k + 1)) % w;
                    comp(g, alpha * 0.22f * (float) clamp(t / 0.3));
                    g.setColor(new Color(0xB91C1C));
                    g.fillOval((int) (shift - w * 0.6), (int) y, (int) (w * 0.9), 26);
                    g.fillOval((int) (shift + w * 0.3), (int) y, (int) (w * 0.9), 26);
                }
            }
            default -> {
                comp(g, alpha * 0.42f);
                g.setColor(new Color(0xBAE6FD));
                g.setStroke(new BasicStroke(1.5f));
                double phase = (now * 0.6) % 90.0;
                for (int i = 0; i < 48; i++) {
                    int rx = (int) ((i * 37 + phase * 1.6) % (w + 140)) - 60;
                    int ry = bandY + (int) ((i * 59 + phase * 2.4) % (bandH + 40)) - 20;
                    g.drawLine(rx, ry, rx - 14, ry + 34);
                }
            }
        }
    }

    static String quote(TitanMode.TitanBoss b, double t) {
        int beat = t < 0.28 ? 0 : t < 0.55 ? 1 : t < 0.83 ? 2 : 3;
        String[] q = switch (b) {
            case SPACEGODZILLA -> new String[]{
                    "Sao băng tím đang lao thẳng xuống vịnh?! Không... đó là một khối tinh thể khổng lồ!!",
                    "Nó phá vỏ tinh thể chui ra rồi! Hai cột vai đang sáng rực kìa!!",
                    "Tia xoắn tím xé ngang thành phố... tinh thể mọc lên khắp nơi!!",
                    "SpaceGodzilla đang hướng ra biển... hạm đội ơi, cố lên!!"};
            case DESTOROYAH -> new String[]{
                    "Mặt biển sủi bọt đỏ... sương đỏ đang tràn vào bờ!!",
                    "Con quỷ có cánh trồi lên rồi! Cái lõi trên ngực nó nở ra như bông hoa!!",
                    "Tia oxy làm các tòa nhà tan rã... cái sừng chém đôi cả tòa tháp!!",
                    "Destoroyah tiến ra vịnh rồi... hạm đội đừng để nó lại gần!!"};
            default -> new String[]{
                    "Dưới đáy biển có bóng gì đang đổi hình liên tục... bốn con quái vật?!",
                    "Không, chỉ là một con! Nó vọt lên khỏi mặt nước, khe mang rực cam!!",
                    "Hai luồng hơi bỏng quét qua bến cảng... nước biển sôi sùng sục rồi!!",
                    "Godzilla Ultima còn tiến hóa nữa... hạm đội phải hạ nó thật nhanh!!"};
        };
        return "Cậu bé: \"" + q[beat] + "\"";
    }

    private static void comp(Graphics2D g, float a) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(a)));
    }
}
