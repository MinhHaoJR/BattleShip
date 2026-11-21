package battleship.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * VFX của chiêu 3 boss mới (SpaceGodzilla, Destoroyah, Godzilla Ultima), vẽ theo kiểu khối pixel như các hiệu ứng cũ.
 * Mỗi chiêu ghép một hoặc nhiều {@link TitanMode.FxKind}; trúng đích tại {@link #IMPACT_T} của thời lượng.
 * CombatFxOverlay giữ danh sách, gọi {@link #paint} mỗi khung và tự bắn onImpact khi tới lúc trúng.
 */
final class KaijuSkillFx {
    static final double IMPACT_T = 2.0 / 3.0;

    static final class Anim {
        final TitanMode.TitanSkill skill;
        final Point origin;
        final List<Point> cells;
        final Point center;
        final int cell;
        final Color main;
        final Color hot;
        final long startMs;
        final long durationMs;
        final Runnable onImpact;
        final long seed;
        boolean impactFired;

        Anim(TitanMode.TitanSkill skill, boolean enraged, Point origin, List<Point> cells, Point center,
             int cell, Runnable onImpact, long now) {
            this.skill = skill;
            this.origin = origin;
            this.cells = cells.isEmpty() ? List.of(center) : cells;
            this.center = center;
            this.cell = Math.max(18, cell);
            this.main = skill.accentColor;
            this.hot = enraged ? skill.boss.enrageColor : Color.WHITE;
            this.startMs = now;
            this.durationMs = Math.round(flightMs(skill) / IMPACT_T);
            this.onImpact = onImpact;
            this.seed = now ^ skill.ordinal() * 7919L;
        }

        /** 0 → 1 tới lúc trúng đích, rồi tiếp tục tới 1.5 khi hiệu ứng tan. */
        double phase(long now) {
            return (now - startMs) / (double) durationMs / IMPACT_T;
        }

        boolean done(long now) {
            return now - startMs >= durationMs;
        }
    }

    final List<Anim> anims = new ArrayList<>();

    static long flightMs(TitanMode.TitanSkill skill) {
        long ms = 900L;
        for (TitanMode.FxKind k : skill.spec.fx()) {
            ms = Math.max(ms, switch (k) {
                case BEAM, NOVA -> 1500L;
                case SPIRAL_BEAM -> 1700L;
                case CRYSTAL_RAIN, LAVA_GEYSER, SCALE_SHRAPNEL, RED_PILLARS -> 1200L;
                case CRYSTAL_PILLARS, GRAVITY_TORNADO, SWARM, MIST, SPINE_LANCES, CRIMSON_BREATH -> 1300L;
                case GILL_SPRAY, FIN_WAKE, GEYSER, CHARGE_TRAIL, SONIC_ROAR -> 1000L;
                case HORN_SLASH, CLAW_MARKS -> 900L;
                case SPINE_CHARGE, RED_SHOCKWAVE -> 1600L;
                case STEAM_JETS, STOMP_TRAIL -> 1100L;
                case WHIRLPOOL, MAGMA_RING -> 1300L;
                case ICE_SPIKES -> 1200L;
                case TAIL_ARC -> 1000L;
                case ICE_BEAM -> 1500L;
                case BLOOD_SPIRAL -> 1700L;
            });
        }
        return ms;
    }

    void paint(Graphics2D g, long now) {
        for (Anim a : anims) {
            double p = a.phase(now);
            if (p < 0) continue;
            for (TitanMode.FxKind k : a.skill.spec.fx()) {
                Graphics2D gg = (Graphics2D) g.create();
                switch (k) {
                    case BEAM -> beam(gg, a, p, false);
                    case SPIRAL_BEAM -> beam(gg, a, p, true);
                    case CRYSTAL_RAIN -> crystalRain(gg, a, p);
                    case CRYSTAL_PILLARS -> crystalPillars(gg, a, p);
                    case GRAVITY_TORNADO -> tornado(gg, a, p);
                    case SWARM -> swarm(gg, a, p);
                    case MIST -> mist(gg, a, p);
                    case HORN_SLASH -> hornSlash(gg, a, p);
                    case GILL_SPRAY -> gillSpray(gg, a, p);
                    case FIN_WAKE -> finWake(gg, a, p);
                    case GEYSER -> geyser(gg, a, p);
                    case CHARGE_TRAIL -> chargeTrail(gg, a, p);
                    case LAVA_GEYSER -> lavaGeyser(gg, a, p);
                    case SCALE_SHRAPNEL -> scaleShrapnel(gg, a, p);
                    case CLAW_MARKS -> clawMarks(gg, a, p);
                    case SONIC_ROAR -> sonicRoar(gg, a, p);
                    case SPINE_LANCES -> spineLances(gg, a, p);
                    case CRIMSON_BREATH -> crimsonBreath(gg, a, p);
                    case RED_PILLARS -> redPillars(gg, a, p);
                    case SPINE_CHARGE -> spineCharge(gg, a, p);
                    case RED_SHOCKWAVE -> redShockwave(gg, a, p);
                    case NOVA -> nova(gg, a, p);
                    case WHIRLPOOL -> whirlpool(gg, a, p);
                    case STEAM_JETS -> steamJets(gg, a, p);
                    case MAGMA_RING -> magmaRing(gg, a, p);
                    case STOMP_TRAIL -> stompTrail(gg, a, p);
                    case ICE_BEAM -> iceBeam(gg, a, p);
                    case ICE_SPIKES -> iceSpikes(gg, a, p);
                    case TAIL_ARC -> tailArc(gg, a, p);
                    case BLOOD_SPIRAL -> bloodSpiral(gg, a, p);
                }
                gg.dispose();
            }
        }
    }

    // ---------- từng kiểu hiệu ứng ----------

    /** Tụ cầu năng lượng ở miệng/lõi, rồi tia bắn tới ô xa nhất; bản xoắn có 2 dải xoắn quanh tia. */
    private static void beam(Graphics2D g, Anim a, double p, boolean spiral) {
        double charge = clamp(p / 0.4);
        if (p < 0.55) {
            int r = (int) (a.cell * (0.25 + charge * 0.55));
            glow(g, a.main, a.origin.x, a.origin.y, r * 2, 0.55f * (float) charge);
            block(g, a.origin.x, a.origin.y, r, a.hot, (float) charge);
        }
        if (p < 0.4 || p > 1.45) return;
        Point far = farthest(a.origin, a.cells);
        double grow = clamp((p - 0.4) / 0.25);
        double fade = p > 1.0 ? 1.0 - clamp((p - 1.0) / 0.45) : 1.0;
        double ex = a.origin.x + (far.x - a.origin.x) * grow;
        double ey = a.origin.y + (far.y - a.origin.y) * grow;
        float w = (float) (a.cell * (spiral ? 0.42 : 0.36) * (0.85 + 0.15 * Math.sin(p * 40)));
        line(g, a.origin.x, a.origin.y, ex, ey, w * 1.8f, a.main, 0.35f * (float) fade);
        line(g, a.origin.x, a.origin.y, ex, ey, w, a.main, 0.85f * (float) fade);
        line(g, a.origin.x, a.origin.y, ex, ey, w * 0.38f, a.hot, (float) fade);
        if (spiral) {
            double len = Math.hypot(ex - a.origin.x, ey - a.origin.y);
            double nx = -(ey - a.origin.y) / Math.max(1, len), ny = (ex - a.origin.x) / Math.max(1, len);
            for (int i = 0; i < 26; i++) {
                double f = i / 25.0;
                for (int s = 0; s < 2; s++) {
                    double ang = f * 14 + p * 22 + s * Math.PI;
                    double off = Math.sin(ang) * w * 1.3;
                    double x = a.origin.x + (ex - a.origin.x) * f + nx * off;
                    double y = a.origin.y + (ey - a.origin.y) * f + ny * off;
                    block(g, x, y, Math.max(2, (int) (w * 0.32)), s == 0 ? a.hot : a.main, (float) fade);
                }
            }
        }
        if (grow >= 1.0) {
            for (Point c : a.cells) glow(g, a.main, c.x, c.y, a.cell, 0.45f * (float) fade);
        }
    }

    /** Mũi tinh thể rơi chéo từ trời xuống từng ô, so le nhau. */
    private static void crystalRain(Graphics2D g, Anim a, double p) {
        Random r = new Random(a.seed);
        for (int i = 0; i < a.cells.size(); i++) {
            Point c = a.cells.get(i);
            double delay = 0.25 + r.nextDouble() * 0.2;
            double f = clamp((p - delay) / (1.0 - delay));
            if (f <= 0 || p > 1.25) continue;
            double sx = c.x + a.cell * 3.0, sy = c.y - a.cell * 6.0;
            double x = sx + (c.x - sx) * f, y = sy + (c.y - sy) * f;
            for (int k = 1; k <= 4; k++) {
                double tf = Math.max(0, f - k * 0.05);
                block(g, sx + (c.x - sx) * tf, sy + (c.y - sy) * tf, Math.max(2, a.cell / 9), a.main, 0.6f - k * 0.12f);
            }
            if (f < 1.0) shard(g, x, y, a.cell * 0.30, a.cell * 0.75, Math.atan2(c.y - sy, c.x - sx) + Math.PI / 2, a.main, a.hot, 1f);
            else glow(g, a.main, c.x, c.y, a.cell * 1.2, (float) (1.25 - p) * 2f);
        }
    }

    /** Tháp tinh thể trồi lên từ ô rồi vỡ vụn sau khi trúng. */
    private static void crystalPillars(Graphics2D g, Anim a, double p) {
        Random r = new Random(a.seed + 3);
        for (Point c : a.cells) {
            double rise = clamp((p - 0.45) / 0.55);
            if (rise <= 0) {
                glow(g, a.main, c.x, c.y, a.cell * 0.8, 0.35f * (float) clamp(p / 0.45));
                continue;
            }
            if (p <= 1.15) {
                double h = a.cell * (0.9 + r.nextDouble() * 0.6) * rise;
                shard(g, c.x, c.y - h / 2, a.cell * 0.32, h, 0, a.main, a.hot, 0.95f);
                shard(g, c.x - a.cell * 0.22, c.y - h / 3, a.cell * 0.18, h * 0.55, -0.35, a.main, a.hot, 0.85f);
            } else {
                double s = (p - 1.15) / 0.35;
                for (int k = 0; k < 6; k++) {
                    double ang = r.nextDouble() * Math.PI * 2;
                    block(g, c.x + Math.cos(ang) * a.cell * s, c.y - a.cell * 0.4 + Math.sin(ang) * a.cell * s,
                            Math.max(2, a.cell / 8), k % 2 == 0 ? a.main : a.hot, (float) (1 - s));
                }
            }
        }
    }

    /** Lốc xoáy trọng lực: khối nhỏ xoay vòng quanh tâm, bán kính co dần, các ô bị kéo về tâm. */
    private static void tornado(Graphics2D g, Anim a, double p) {
        if (p > 1.4) return;
        float fade = (float) (p > 1.0 ? 1 - (p - 1.0) / 0.4 : clamp(p / 0.2));
        double rad = a.cell * (2.6 - 1.6 * clamp(p));
        for (int i = 0; i < 40; i++) {
            double ang = i * 0.55 + p * 16;
            double rr = rad * (0.25 + (i % 10) / 10.0);
            double x = a.center.x + Math.cos(ang) * rr;
            double y = a.center.y + Math.sin(ang) * rr * 0.55 - (i % 10) * a.cell * 0.08;
            block(g, x, y, Math.max(2, a.cell / 10), i % 3 == 0 ? a.hot : a.main, fade);
        }
        for (Point c : a.cells) line(g, c.x, c.y, a.center.x, a.center.y, 2f, a.main, 0.35f * fade);
    }

    /** Bầy giáp xác đỏ bò từ thân boss tới từng ô theo đường cong. */
    private static void swarm(Graphics2D g, Anim a, double p) {
        Random r = new Random(a.seed + 5);
        for (Point c : a.cells) {
            for (int k = 0; k < 7; k++) {
                double delay = r.nextDouble() * 0.3;
                double f = clamp((p - delay) / (1.0 - delay));
                double bend = (r.nextDouble() - 0.5) * a.cell * 3;
                if (f <= 0 || p > 1.3) continue;
                double x = a.origin.x + (c.x - a.origin.x) * f + Math.sin(f * Math.PI) * bend;
                double y = a.origin.y + (c.y - a.origin.y) * f + Math.sin(f * 9 + k) * 2;
                int sz = Math.max(3, a.cell / 7);
                block(g, x, y, sz, a.main, 1f);
                block(g, x - sz * 0.6, y + sz * 0.5, Math.max(1, sz / 2), a.main.darker(), 1f);
                block(g, x + sz * 0.6, y + sz * 0.5, Math.max(1, sz / 2), a.main.darker(), 1f);
                block(g, x, y - sz * 0.3, Math.max(1, sz / 3), a.hot, 1f);
            }
        }
    }

    /** Sương đen đỏ lan ra trên các ô và cuộn xoáy chậm. */
    private static void mist(Graphics2D g, Anim a, double p) {
        float fade = (float) (p < 0.5 ? clamp(p / 0.5) : p > 1.1 ? 1 - (p - 1.1) / 0.4 : 1.0);
        Random r = new Random(a.seed + 7);
        for (Point c : a.cells) {
            for (int k = 0; k < 4; k++) {
                double ang = r.nextDouble() * Math.PI * 2 + p * 2;
                double d = a.cell * 0.35;
                glow(g, new Color(0x1C0A0A), c.x + Math.cos(ang) * d, c.y + Math.sin(ang) * d, a.cell * 1.3, 0.55f * fade);
            }
            glow(g, a.main, c.x, c.y, a.cell * 0.9, 0.35f * fade);
        }
    }

    /** Lưỡi laser từ sừng chém một vệt cong qua các ô. */
    private static void hornSlash(Graphics2D g, Anim a, double p) {
        if (p > 1.35) return;
        Point first = a.cells.get(0), last = a.cells.get(a.cells.size() - 1);
        double f = clamp((p - 0.25) / 0.75);
        float fade = (float) (p > 1.0 ? 1 - (p - 1.0) / 0.35 : 1.0);
        if (p < 0.3) glow(g, a.hot, a.origin.x, a.origin.y, a.cell * 1.2, (float) (p / 0.3));
        double hx = first.x + (last.x - first.x) * f, hy = first.y + (last.y - first.y) * f;
        line(g, first.x, first.y, hx, hy, a.cell * 0.30f, a.main, 0.55f * fade);
        line(g, first.x, first.y, hx, hy, a.cell * 0.12f, a.hot, fade);
        line(g, a.origin.x, a.origin.y, hx, hy, 2f, a.hot, 0.5f * fade * (float) (1 - f));
        glow(g, a.hot, hx, hy, a.cell * 0.8, fade);
    }

    // ---------- Godzilla Ultima: VFX riêng từng form ----------

    /** I · Hai luồng hơi nóng cam xoè chữ V từ khe mang, hạt lửa bay theo. */
    private static void gillSpray(Graphics2D g, Anim a, double p) {
        if (p > 1.3) return;
        float fade = (float) (p > 1.0 ? 1 - (p - 1.0) / 0.3 : 1.0);
        Random r = new Random(a.seed + 21);
        for (Point c : a.cells) {
            double f = clamp(p);
            for (int k = 0; k < 9; k++) {
                double lag = k * 0.06;
                double ff = clamp(f - lag);
                double x = a.origin.x + (c.x - a.origin.x) * ff + (r.nextDouble() - 0.5) * a.cell * 0.4;
                double y = a.origin.y + (c.y - a.origin.y) * ff + (r.nextDouble() - 0.5) * a.cell * 0.4 - Math.sin(ff * Math.PI) * a.cell * 0.6;
                block(g, x, y, Math.max(2, (int) (a.cell * (0.22 - k * 0.015))), k < 3 ? a.hot : a.main, fade * (1f - k * 0.08f));
            }
        }
        glow(g, a.main, a.origin.x, a.origin.y, a.cell * 1.1, 0.6f * fade);
    }

    /** I · Gai lưng rạch mặt nước theo đường zíc-zắc, để lại vệt bọt trắng. */
    private static void finWake(Graphics2D g, Anim a, double p) {
        if (p > 1.35) return;
        float fade = (float) (p > 1.0 ? 1 - (p - 1.0) / 0.35 : 1.0);
        int n = a.cells.size();
        double head = clamp(p) * (n - 1);
        for (int i = 0; i < n - 1; i++) {
            if (head <= i) break;
            Point c0 = a.cells.get(i), c1 = a.cells.get(i + 1);
            double f = Math.min(1, head - i);
            double x = c0.x + (c1.x - c0.x) * f, y = c0.y + (c1.y - c0.y) * f;
            line(g, c0.x, c0.y, x, y, a.cell * 0.32f, new Color(0xE0F2FE), 0.8f * fade);
            if (f < 1 || i == (int) head) {
                for (int k = 0; k < 4; k++) {   // vây lưng nhô khỏi nước
                    shard(g, x - k * a.cell * 0.18, y - a.cell * (0.35 + 0.12 * (3 - k)), a.cell * 0.16, a.cell * (0.4 + 0.1 * (3 - k)), 0, a.main, a.hot, fade);
                }
            }
        }
    }

    /** I · Cột nước dựng đứng ở từng ô rồi đổ xuống thành bọt. */
    private static void geyser(Graphics2D g, Anim a, double p) {
        if (p < 0.5 || p > 1.45) return;
        double up = clamp((p - 0.5) / 0.5);
        float fade = (float) (p > 1.15 ? 1 - (p - 1.15) / 0.3 : 1.0);
        for (Point c : a.cells) {
            double h = a.cell * 2.6 * Math.sin(up * Math.PI * 0.5);
            for (int k = 0; k < 8; k++) {
                double y = c.y - h * k / 7.0;
                double w = a.cell * (0.5 - k * 0.03) * (0.9 + 0.1 * Math.sin(p * 30 + k));
                block(g, c.x, y, (int) w, k % 2 == 0 ? new Color(0x7DD3FC) : Color.WHITE, 0.85f * fade);
            }
            block(g, c.x, c.y - h, (int) (a.cell * 0.6), Color.WHITE, fade);
        }
    }

    /** II · Bụi và vết giẫm chạy dọc hàng húc, đầu sừng lóe sáng ở đầu đường. */
    private static void chargeTrail(Graphics2D g, Anim a, double p) {
        if (p > 1.35) return;
        float fade = (float) (p > 1.0 ? 1 - (p - 1.0) / 0.35 : 1.0);
        Point first = a.cells.get(0), last = a.cells.get(a.cells.size() - 1);
        if (a.origin.distanceSq(last) < a.origin.distanceSq(first)) { Point t = first; first = last; last = t; }
        double f = clamp(p);
        double hx = first.x + (last.x - first.x) * f, hy = first.y + (last.y - first.y) * f;
        Random r = new Random(a.seed + 33);
        for (int k = 0; k < 18; k++) {
            double u = r.nextDouble() * f;
            double x = first.x + (last.x - first.x) * u, y = first.y + (last.y - first.y) * u;
            block(g, x + (r.nextDouble() - 0.5) * a.cell, y + (r.nextDouble() - 0.5) * a.cell * 0.6 - (f - u) * a.cell,
                    Math.max(3, a.cell / 5), new Color(0x8B6B4A), 0.7f * fade);
        }
        for (Point c : a.cells) {
            double u = (c.x - first.x) * (last.x - first.x) + (c.y - first.y) * (last.y - first.y);
            double len = Math.max(1, first.distanceSq(last));
            if (u / len <= f) block(g, c.x, c.y + a.cell * 0.2, (int) (a.cell * 0.5), new Color(0x3B1A0A), 0.8f * fade);
        }
        glow(g, a.hot, hx, hy, a.cell * 1.3, fade);
        shard(g, hx, hy - a.cell * 0.5, a.cell * 0.35, a.cell * 0.9, -0.6, a.main, a.hot, fade);
    }

    /** II · Khe nứt đỏ lan trên ô rồi phun cột dung nham kèm đá nóng. */
    private static void lavaGeyser(Graphics2D g, Anim a, double p) {
        Random r = new Random(a.seed + 44);
        for (Point c : a.cells) {
            if (p < 0.6) {
                float k = (float) clamp(p / 0.6);
                line(g, c.x - a.cell * 0.4, c.y + a.cell * 0.1, c.x + a.cell * 0.4 * k, c.y - a.cell * 0.15 * k, 3f, a.main, k);
                continue;
            }
            double up = clamp((p - 0.6) / 0.4);
            float fade = (float) (p > 1.15 ? 1 - (p - 1.15) / 0.35 : 1.0);
            double h = a.cell * 2.2 * up;
            for (int k = 0; k < 7; k++) {
                block(g, c.x + Math.sin(p * 20 + k) * 2, c.y - h * k / 6.0, Math.max(3, (int) (a.cell * (0.42 - k * 0.04))),
                        k < 2 ? new Color(0xFDE047) : a.main, fade);
            }
            for (int k = 0; k < 4; k++) {
                double ang = -Math.PI / 2 + (r.nextDouble() - 0.5) * 1.6;
                double d = a.cell * 1.5 * up;
                block(g, c.x + Math.cos(ang) * d, c.y - h + Math.sin(ang) * d + up * up * a.cell, Math.max(2, a.cell / 7), new Color(0x3A1208), fade);
            }
        }
    }

    /** II · Mảng giáp vảy nung đỏ quay tít bay vòng cung tới từng ô. */
    private static void scaleShrapnel(Graphics2D g, Anim a, double p) {
        if (p > 1.25) return;
        for (int i = 0; i < a.cells.size(); i++) {
            Point c = a.cells.get(i);
            double f = clamp((p - i * 0.05) / (1.0 - 0.2));
            if (f <= 0) continue;
            double x = a.origin.x + (c.x - a.origin.x) * f;
            double y = a.origin.y + (c.y - a.origin.y) * f - Math.sin(f * Math.PI) * a.cell * 2.2;
            double rot = f * 14 + i;
            int w = (int) (a.cell * 0.55), h = (int) (a.cell * 0.32);
            Graphics2D gg = (Graphics2D) g.create();
            gg.translate(x, y);
            gg.rotate(rot);
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            gg.setColor(a.main);
            gg.fillRect(-w / 2, -h / 2, w, h);
            gg.setColor(new Color(0xFF7A1A));
            gg.fillRect(-w / 2, -1, w, 2);
            gg.setColor(new Color(20, 8, 4));
            gg.drawRect(-w / 2, -h / 2, w, h);
            gg.dispose();
            block(g, x - (c.x - a.origin.x) * 0.03, y + 3, Math.max(2, a.cell / 9), new Color(0xFF7A1A), 0.6f);
        }
    }

    /** III · Ba vết vuốt xanh băng xé chéo xuống, lóe sáng rồi mờ dần. */
    private static void clawMarks(Graphics2D g, Anim a, double p) {
        if (p < 0.3 || p > 1.4) return;
        double f = clamp((p - 0.3) / 0.5);
        float fade = (float) (p > 1.0 ? 1 - (p - 1.0) / 0.4 : 1.0);
        double minX = Double.MAX_VALUE, maxX = -1, minY = Double.MAX_VALUE, maxY = -1;
        for (Point c : a.cells) { minX = Math.min(minX, c.x); maxX = Math.max(maxX, c.x); minY = Math.min(minY, c.y); maxY = Math.max(maxY, c.y); }
        for (int k = 0; k < 3; k++) {
            double x0 = minX + (maxX - minX) * k / 2.0 - a.cell * 0.4, y0 = minY - a.cell * 0.6;
            double x1 = x0 + a.cell * 0.8, y1 = maxY + a.cell * 0.6;
            double ex = x0 + (x1 - x0) * f, ey = y0 + (y1 - y0) * f;
            line(g, x0, y0, ex, ey, a.cell * 0.34f, a.main, 0.55f * fade);
            line(g, x0, y0, ex, ey, a.cell * 0.12f, Color.WHITE, fade);
        }
    }

    /** III · Các vòng sóng âm hình cung lan từ miệng ra, kèm tinh băng. */
    private static void sonicRoar(Graphics2D g, Anim a, double p) {
        if (p > 1.4) return;
        double ang = Math.toDegrees(Math.atan2(-(a.center.y - a.origin.y), a.center.x - a.origin.x));
        double dist = a.origin.distance(a.center) + a.cell * 1.5;
        for (int k = 0; k < 5; k++) {
            double f = clamp(p * 1.2 - k * 0.12);
            if (f <= 0 || f >= 1) continue;
            double rad = dist * f;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF((float) (1 - f) * 0.9f)));
            g.setColor(k % 2 == 0 ? a.main : Color.WHITE);
            g.setStroke(new BasicStroke(Math.max(3f, a.cell / 5f)));
            g.drawArc((int) (a.origin.x - rad), (int) (a.origin.y - rad), (int) (rad * 2), (int) (rad * 2), (int) (ang - 28), 56);
        }
        if (p > 0.95) for (Point c : a.cells) shard(g, c.x, c.y, a.cell * 0.3, a.cell * 0.3, Math.PI / 4, a.main, Color.WHITE, (float) clamp(1.4 - p) * 2f);
    }

    /** III · Gai lưng lõi băng lam bứt ra, bay thành thương cắm xuống từng ô. */
    private static void spineLances(Graphics2D g, Anim a, double p) {
        if (p > 1.3) return;
        for (int i = 0; i < a.cells.size(); i++) {
            Point c = a.cells.get(i);
            double f = clamp((p - 0.15 - i * 0.03) / 0.75);
            if (f <= 0) { glow(g, a.hot, a.origin.x, a.origin.y - a.cell * 0.8, a.cell * 0.6, 0.5f); continue; }
            double sx = a.origin.x - a.cell, sy = a.origin.y - a.cell * 3;
            double x = sx + (c.x - sx) * f, y = sy + (c.y - sy) * f - Math.sin(f * Math.PI) * a.cell * 1.6;
            double dir = Math.atan2(c.y - sy - Math.cos(f * Math.PI) * a.cell * 1.6, c.x - sx) + Math.PI / 2;
            if (f < 1) shard(g, x, y, a.cell * 0.22, a.cell * 1.0, dir, a.main, new Color(0x7FF5FF), 1f);
            else {
                shard(g, c.x, c.y - a.cell * 0.4, a.cell * 0.22, a.cell * 0.9, 0, a.main, new Color(0x7FF5FF), (float) clamp((1.3 - p) * 3));
                glow(g, new Color(0x7FF5FF), c.x, c.y, a.cell * 0.8, (float) clamp((1.3 - p) * 2));
            }
        }
    }

    /** IV · Luồng hơi thở đỏ dày, gợn sóng, tóe tàn lửa trên dải bị quét. */
    private static void crimsonBreath(Graphics2D g, Anim a, double p) {
        if (p < 0.2) { glow(g, a.main, a.origin.x, a.origin.y, a.cell * (0.6 + p * 3), 0.7f); return; }
        if (p > 1.4) return;
        float fade = (float) (p > 1.05 ? 1 - (p - 1.05) / 0.35 : 1.0);
        double f = clamp((p - 0.2) / 0.4);
        Random r = new Random(a.seed + 55 + (long) (p * 20));
        for (Point c : a.cells) {
            double ex = a.origin.x + (c.x - a.origin.x) * f, ey = a.origin.y + (c.y - a.origin.y) * f;
            line(g, a.origin.x, a.origin.y, ex, ey, a.cell * 0.55f, new Color(0x7F1D1D), 0.35f * fade);
            line(g, a.origin.x, a.origin.y, ex, ey, a.cell * 0.28f, a.main, 0.6f * fade);
            if (f >= 1) {
                glow(g, a.main, c.x, c.y, a.cell * 0.9, 0.55f * fade);
                for (int k = 0; k < 3; k++) block(g, c.x + (r.nextDouble() - 0.5) * a.cell, c.y - r.nextDouble() * a.cell,
                        Math.max(2, a.cell / 9), new Color(0xFDE047), fade);
            }
        }
    }

    /** IV · Cột lửa đỏ giáng thẳng từ trời xuống từng ô. */
    private static void redPillars(Graphics2D g, Anim a, double p) {
        for (int i = 0; i < a.cells.size(); i++) {
            Point c = a.cells.get(i);
            double t0 = 0.45 + i * 0.05;
            if (p < t0) {
                if (p > 0.2) glow(g, a.main, c.x, c.y, a.cell * 0.5, 0.35f);
                continue;
            }
            double f = clamp((p - t0) / 0.2);
            float fade = (float) (p > 1.1 ? 1 - (p - 1.1) / 0.3 : 1.0);
            double top = c.y - a.cell * 9;
            double bottom = top + (c.y - top) * f;
            line(g, c.x, top, c.x, bottom, a.cell * 0.5f, a.main, 0.5f * fade);
            line(g, c.x, top, c.x, bottom, a.cell * 0.18f, new Color(0xFFE4E1), fade);
            if (f >= 1) glow(g, a.main, c.x, c.y, a.cell * 1.1, 0.7f * fade);
        }
    }

    /** IV · Gai lưng bừng đỏ từng đốt từ đuôi lên đầu trước khi gầm. */
    private static void spineCharge(Graphics2D g, Anim a, double p) {
        if (p > 1.0) return;
        for (int k = 0; k < 9; k++) {
            double f = clamp(p * 1.4 - k * 0.1);
            if (f <= 0) continue;
            double x = a.origin.x + a.cell * (2.2 - k * 0.32), y = a.origin.y + a.cell * (2.6 - k * 0.38);
            glow(g, a.main, x, y, a.cell * 0.55, (float) f * 0.8f);
            block(g, x, y, Math.max(3, a.cell / 4), new Color(0xFFE4E1), (float) f);
        }
    }

    /** IV · Vòng xung kích đỏ hình kim cương nở ra từ tâm, kèm chớp ở các ô. */
    private static void redShockwave(Graphics2D g, Anim a, double p) {
        if (p < 0.85 || p > 1.5) return;
        double f = clamp((p - 0.85) / 0.6);
        float fade = (float) (1 - f);
        for (int k = 0; k < 3; k++) {
            double rad = a.cell * (1 + f * 4) * (1 - k * 0.22);
            Polygon d = new Polygon();
            d.addPoint((int) a.center.x, (int) (a.center.y - rad));
            d.addPoint((int) (a.center.x + rad), (int) a.center.y);
            d.addPoint((int) a.center.x, (int) (a.center.y + rad));
            d.addPoint((int) (a.center.x - rad), (int) a.center.y);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(fade * (0.9f - k * 0.25f))));
            g.setColor(k == 0 ? a.main : new Color(0xFFE4E1));
            g.setStroke(new BasicStroke(Math.max(3f, a.cell / 5f)));
            g.drawPolygon(d);
        }
        for (Point c : a.cells) glow(g, a.main, c.x, c.y, a.cell * 0.9, 0.6f * fade);
    }

    /** Chớp trắng và tia tỏa ra ở thời điểm trúng. */
    private static void nova(Graphics2D g, Anim a, double p) {
        if (p < 0.75) {
            glow(g, a.main, a.center.x, a.center.y, a.cell * 2.5 * clamp(p / 0.75), 0.4f);
            return;
        }
        double f = clamp((p - 0.95) / 0.5);
        float fade = (float) (1 - f);
        glow(g, a.hot, a.center.x, a.center.y, a.cell * (1.5 + f * 4), 0.85f * fade);
        for (int i = 0; i < 12; i++) {
            double ang = i * Math.PI / 6;
            double len = a.cell * (1.5 + f * 3.5);
            line(g, a.center.x, a.center.y, a.center.x + Math.cos(ang) * len, a.center.y + Math.sin(ang) * len * 0.7,
                    Math.max(2f, a.cell / 7f), i % 2 == 0 ? a.main : a.hot, fade);
        }
    }

    // ---------- ★ của từng form Ultima ----------

    /** I★ · Xoáy nước: ba nhánh xoắn bọt trắng quay quanh tâm, thu dần vào giữa, vòng bọt ở mép. */
    private static void whirlpool(Graphics2D g, Anim a, double p) {
        if (p > 1.45) return;
        float fade = (float) (p > 1.05 ? 1 - (p - 1.05) / 0.4 : clamp(p / 0.3));
        double rMax = a.cell * 2.3;
        for (int arm = 0; arm < 3; arm++) {
            for (int k = 0; k < 16; k++) {
                double u = k / 15.0;
                double ang = arm * 2 * Math.PI / 3 + u * 5.0 - p * 9.0;
                double rad = rMax * (1 - u) * (0.6 + 0.4 * clamp(p));
                block(g, a.center.x + Math.cos(ang) * rad, a.center.y + Math.sin(ang) * rad * 0.62,
                        Math.max(2, (int) (a.cell * (0.26 - u * 0.12))), k % 3 == 0 ? Color.WHITE : a.main, fade * (0.5f + 0.5f * (float) u));
            }
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(fade * 0.7f)));
        g.setColor(new Color(0xE0F2FE));
        g.setStroke(new BasicStroke(Math.max(2f, a.cell / 7f)));
        g.drawOval((int) (a.center.x - rMax), (int) (a.center.y - rMax * 0.62), (int) (rMax * 2), (int) (rMax * 1.24));
        glow(g, new Color(0x0C4A6E), a.center.x, a.center.y, a.cell * 0.9, 0.8f * fade);
        if (p > 0.9) for (Point c : a.cells) glow(g, a.main, c.x, c.y, a.cell * 0.6, 0.45f * fade);
    }

    /** I★ · Hai thác hơi bỏng song song so le: hai dải hơi trắng cam chảy dọc hai hàng ô, phồng bọt hơi. */
    private static void steamJets(Graphics2D g, Anim a, double p) {
        if (p > 1.4) return;
        float fade = (float) (p > 1.0 ? 1 - (p - 1.0) / 0.4 : 1.0);
        List<List<Point>> rows = new ArrayList<>();
        for (Point c : a.cells) {
            List<Point> row = null;
            for (List<Point> r0 : rows) if (Math.abs(r0.get(0).y - c.y) < a.cell / 2 && Math.abs(r0.get(0).x - c.x) < a.cell * 6) row = r0;
            if (row == null) { row = new ArrayList<>(); rows.add(row); }
            row.add(c);
        }
        Random r = new Random(a.seed + 66);
        for (int ri = 0; ri < rows.size(); ri++) {
            List<Point> row = rows.get(ri);
            row.sort((u, v) -> Double.compare(a.origin.distanceSq(u), a.origin.distanceSq(v)));
            double f = clamp((p - ri * 0.12) / 0.75);
            Point near = row.get(0), far = row.get(row.size() - 1);
            double ex = near.x + (far.x - near.x) * f, ey = near.y + (far.y - near.y) * f;
            line(g, a.origin.x, a.origin.y, near.x, near.y, a.cell * 0.2f, new Color(0xFFB36B), 0.45f * fade * (float) clamp(p * 3));
            line(g, near.x, near.y, ex, ey, a.cell * 0.62f, new Color(0xFFEDD5), 0.45f * fade);
            line(g, near.x, near.y, ex, ey, a.cell * 0.26f, a.main, 0.85f * fade);
            for (Point c : row) {
                double u = near.distance(c) / Math.max(1, near.distance(far));
                if (u > f + 1e-6) continue;
                for (int k = 0; k < 4; k++) {
                    double rise = ((p * 1.8 + k * 0.25 + r.nextDouble() * 0.2) % 1.0);
                    block(g, c.x + (r.nextDouble() - 0.5) * a.cell * 0.7, c.y - rise * a.cell * 1.4,
                            Math.max(3, (int) (a.cell * (0.34 - rise * 0.18))), k == 0 ? new Color(0xFDBA74) : Color.WHITE, fade * (float) (1 - rise));
                }
            }
        }
    }

    /** II★ · Vòng khe nứt dung nham rạn ra từ tâm, đến lúc trúng thì dung nham phun ở từng ô. */
    private static void magmaRing(Graphics2D g, Anim a, double p) {
        if (p > 1.45) return;
        float fade = (float) (p > 1.1 ? 1 - (p - 1.1) / 0.35 : 1.0);
        double rad = a.cell * 2.3 * clamp(p / 0.85);
        Polygon ring = new Polygon();
        Random r = new Random(a.seed + 77);
        for (int i = 0; i < 20; i++) {
            double ang = i * Math.PI / 10;
            double jag = rad * (0.85 + r.nextDouble() * 0.3);
            ring.addPoint((int) (a.center.x + Math.cos(ang) * jag), (int) (a.center.y + Math.sin(ang) * jag * 0.7));
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(fade)));
        g.setColor(new Color(0x3A1208));
        g.setStroke(new BasicStroke(Math.max(4f, a.cell / 4f)));
        g.drawPolygon(ring);
        g.setColor(a.main);
        g.setStroke(new BasicStroke(Math.max(2f, a.cell / 9f)));
        g.drawPolygon(ring);
        for (int i = 0; i < 8; i++) {                     // nứt hướng tâm
            double ang = i * Math.PI / 4 + 0.2;
            line(g, a.center.x, a.center.y, a.center.x + Math.cos(ang) * rad, a.center.y + Math.sin(ang) * rad * 0.7, 2.5f, a.main, 0.8f * fade);
        }
        if (p < 0.85) return;
        double up = clamp((p - 0.85) / 0.3);
        for (Point c : a.cells) {
            double h = a.cell * 1.8 * up;
            for (int k = 0; k < 5; k++) block(g, c.x, c.y - h * k / 4.0, Math.max(3, (int) (a.cell * (0.46 - k * 0.06))),
                    k < 2 ? new Color(0xFDE047) : a.main, fade);
            glow(g, a.main, c.x, c.y, a.cell * 0.8, 0.5f * fade);
        }
    }

    /** II★ · Vết chân nung đỏ dập xuống lần lượt dọc đường phi, mỗi vết toé bụi và tàn lửa. */
    private static void stompTrail(Graphics2D g, Anim a, double p) {
        if (p > 1.4) return;
        float fade = (float) (p > 1.0 ? 1 - (p - 1.0) / 0.4 : 1.0);
        List<Point> order = new ArrayList<>(a.cells);
        order.sort((u, v) -> Double.compare(a.origin.distanceSq(u), a.origin.distanceSq(v)));
        Random r = new Random(a.seed + 88);
        for (int i = 0; i < order.size(); i++) {
            Point c = order.get(i);
            double t0 = 0.15 + i * 0.11;
            if (p < t0) continue;
            double f = clamp((p - t0) / 0.15);
            int w = (int) (a.cell * 0.62), h = (int) (a.cell * 0.42);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(fade)));
            g.setColor(new Color(0x2A0E05));
            g.fillOval(c.x - w / 2, c.y - h / 2, w, h);
            g.setColor(new Color(0xFF7A1A));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(c.x - w / 2, c.y - h / 2, w, h);
            for (int k = -1; k <= 1; k++) block(g, c.x + k * w * 0.32, c.y - h * 0.55, Math.max(2, a.cell / 9), new Color(0xFDE047), fade);
            if (f < 1) for (int k = 0; k < 6; k++) {
                double ang = r.nextDouble() * Math.PI;
                block(g, c.x + Math.cos(ang) * a.cell * f, c.y - Math.sin(ang) * a.cell * 0.7 * f, Math.max(2, a.cell / 7),
                        k % 2 == 0 ? new Color(0x8B6B4A) : a.main, (float) (1 - f));
            }
        }
    }

    /** III★ · Luồng băng hà trắng lam bắn tới, mặt biển dọc đường kết tinh băng, đầu tia toả sương giá. */
    private static void iceBeam(Graphics2D g, Anim a, double p) {
        if (p < 0.35) { glow(g, a.main, a.origin.x, a.origin.y, a.cell * (0.4 + p * 2.6), 0.75f); return; }
        if (p > 1.45) return;
        float fade = (float) (p > 1.05 ? 1 - (p - 1.05) / 0.4 : 1.0);
        Point far = farthest(a.origin, a.cells);
        double grow = clamp((p - 0.35) / 0.3);
        double ex = a.origin.x + (far.x - a.origin.x) * grow, ey = a.origin.y + (far.y - a.origin.y) * grow;
        line(g, a.origin.x, a.origin.y, ex, ey, a.cell * 0.9f, new Color(0xCFFAFE), 0.3f * fade);
        line(g, a.origin.x, a.origin.y, ex, ey, a.cell * 0.45f, a.main, 0.8f * fade);
        line(g, a.origin.x, a.origin.y, ex, ey, a.cell * 0.15f, Color.WHITE, fade);
        if (grow < 1) return;
        Random r = new Random(a.seed + 99);
        for (Point c : a.cells) {
            for (int k = 0; k < 3; k++) shard(g, c.x + (r.nextDouble() - 0.5) * a.cell * 0.6, c.y - a.cell * 0.15,
                    a.cell * 0.18, a.cell * (0.45 + r.nextDouble() * 0.35), (r.nextDouble() - 0.5) * 0.8, new Color(0xA5F3FC), Color.WHITE, fade);
            glow(g, new Color(0xE0F2FE), c.x, c.y, a.cell * 0.7, 0.4f * fade);
        }
    }

    /** III★ · Gai băng cao trồi lên lần lượt quanh vòng thoi, vòng sương giá ở tâm. */
    private static void iceSpikes(Graphics2D g, Anim a, double p) {
        if (p > 1.45) return;
        float fade = (float) (p > 1.1 ? 1 - (p - 1.1) / 0.35 : 1.0);
        glow(g, a.main, a.center.x, a.center.y, a.cell * (0.6 + clamp(p) * 1.6), 0.35f * fade);
        List<Point> order = new ArrayList<>(a.cells);
        order.sort((u, v) -> Double.compare(Math.atan2(u.y - a.center.y, u.x - a.center.x), Math.atan2(v.y - a.center.y, v.x - a.center.x)));
        for (int i = 0; i < order.size(); i++) {
            Point c = order.get(i);
            double f = clamp((p - 0.35 - i * 0.06) / 0.25);
            if (f <= 0) continue;
            double hgt = a.cell * 1.5 * f;
            shard(g, c.x, c.y - hgt / 2, a.cell * 0.38, hgt, 0, a.main, Color.WHITE, fade);
            shard(g, c.x - a.cell * 0.25, c.y - hgt * 0.3, a.cell * 0.2, hgt * 0.6, -0.35, new Color(0x67E8F9), Color.WHITE, fade);
            if (f < 1) block(g, c.x, c.y, (int) (a.cell * 0.7 * (1 - f) + 2), Color.WHITE, (float) (1 - f));
        }
    }

    /** IV · Đuôi gai đỏ quét một cung qua các ô, để lại vệt chuyển động và bụi nước. */
    private static void tailArc(Graphics2D g, Anim a, double p) {
        if (p > 1.35) return;
        float fade = (float) (p > 1.0 ? 1 - (p - 1.0) / 0.35 : 1.0);
        int n = a.cells.size();
        double head = clamp((p - 0.1) / 0.8) * (n - 1);
        for (int i = 0; i < n - 1 && i < head; i++) {
            Point c0 = a.cells.get(i), c1 = a.cells.get(i + 1);
            double f = Math.min(1, head - i);
            double x = c0.x + (c1.x - c0.x) * f, y = c0.y + (c1.y - c0.y) * f;
            float age = (float) clamp(1 - (head - i) / 4.0);
            line(g, c0.x, c0.y, x, y, a.cell * 0.7f, new Color(0x450A0A), 0.5f * fade * age);
            line(g, c0.x, c0.y, x, y, a.cell * 0.3f, a.main, 0.9f * fade * age);
            if (i == (int) head || f < 1) {
                for (int k = 0; k < 3; k++) shard(g, x - k * a.cell * 0.2, y - a.cell * 0.3, a.cell * 0.18, a.cell * 0.55, -0.5, new Color(0xFCA5A5), Color.WHITE, fade);
            }
            block(g, c0.x, c0.y + a.cell * 0.3, (int) (a.cell * 0.5), new Color(0xE0F2FE), 0.5f * fade * age);
        }
    }

    /** IV★ · Hai dải huyết hạch xoắn vào nhau bay tới, nổ thành chữ thập đỏ ở đầu tia. */
    private static void bloodSpiral(Graphics2D g, Anim a, double p) {
        if (p < 0.4) { glow(g, a.main, a.origin.x, a.origin.y, a.cell * (0.5 + p * 2.5), 0.8f); return; }
        if (p > 1.5) return;
        float fade = (float) (p > 1.05 ? 1 - (p - 1.05) / 0.45 : 1.0);
        Point far = farthest(a.origin, a.cells);
        double grow = clamp((p - 0.4) / 0.35);
        double dx = far.x - a.origin.x, dy = far.y - a.origin.y, len = Math.max(1, Math.hypot(dx, dy));
        double nx = -dy / len, ny = dx / len;
        for (int i = 0; i <= 40; i++) {
            double f = i / 40.0 * grow;
            for (int sIdx = 0; sIdx < 2; sIdx++) {
                double off = Math.sin(f * 16 - p * 18 + sIdx * Math.PI) * a.cell * 0.55;
                block(g, a.origin.x + dx * f + nx * off, a.origin.y + dy * f + ny * off, Math.max(3, a.cell / 5),
                        sIdx == 0 ? a.main : new Color(0x450A0A), fade);
            }
        }
        if (grow < 1) return;
        double burst = clamp((p - 0.75) / 0.4);
        for (int k = 0; k < 4; k++) {
            double ang = k * Math.PI / 2;
            double L = a.cell * (0.6 + burst * 1.8);
            line(g, far.x, far.y, far.x + Math.cos(ang) * L, far.y + Math.sin(ang) * L, a.cell * 0.38f, a.main, fade);
            line(g, far.x, far.y, far.x + Math.cos(ang) * L, far.y + Math.sin(ang) * L, a.cell * 0.12f, new Color(0xFFE4E1), fade);
        }
        for (Point c : a.cells) glow(g, a.main, c.x, c.y, a.cell * 0.6, 0.5f * fade);
    }

    // ---------- nét vẽ cơ bản ----------

    static void block(Graphics2D g, double x, double y, int size, Color c, float alpha) {
        if (alpha <= 0.01f) return;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(alpha)));
        int s = Math.max(1, size);
        g.setColor(c);
        g.fillRect((int) (x - s / 2.0), (int) (y - s / 2.0), s, s);
    }

    static void glow(Graphics2D g, Color c, double x, double y, double radius, float alpha) {
        if (alpha <= 0.01f || radius < 1) return;
        for (int i = 3; i >= 1; i--) {
            double r = radius * i / 3.0;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(alpha * 0.28f)));
            g.setColor(c);
            g.fillOval((int) (x - r), (int) (y - r), (int) (r * 2), (int) (r * 2));
        }
    }

    static void line(Graphics2D g, double x1, double y1, double x2, double y2, float w, Color c, float alpha) {
        if (alpha <= 0.01f) return;
        Stroke old = g.getStroke();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(alpha)));
        g.setColor(c);
        g.setStroke(new BasicStroke(Math.max(1f, w), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        g.setStroke(old);
    }

    /** Mảnh tinh thể hình thoi dài, có lõi sáng. */
    static void shard(Graphics2D g, double cx, double cy, double w, double h, double rot, Color c, Color core, float alpha) {
        double cos = Math.cos(rot), sin = Math.sin(rot);
        double[][] pts = {{0, -h / 2}, {w / 2, 0}, {0, h / 2}, {-w / 2, 0}};
        Polygon poly = new Polygon(), in = new Polygon();
        for (double[] pt : pts) {
            poly.addPoint((int) (cx + pt[0] * cos - pt[1] * sin), (int) (cy + pt[0] * sin + pt[1] * cos));
            in.addPoint((int) (cx + pt[0] * 0.4 * cos - pt[1] * 0.6 * sin), (int) (cy + pt[0] * 0.4 * sin + pt[1] * 0.6 * cos));
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(alpha)));
        g.setColor(c);
        g.fillPolygon(poly);
        g.setColor(core);
        g.fillPolygon(in);
        g.setColor(new Color(15, 10, 30));
        g.drawPolygon(poly);
    }

    private static Point farthest(Point from, List<Point> pts) {
        Point best = pts.get(0);
        double bd = -1;
        for (Point q : pts) {
            double d = from.distanceSq(q);
            if (d > bd) { bd = d; best = q; }
        }
        return best;
    }

    static double clamp(double v) { return Math.max(0, Math.min(1, v)); }

    static float clampF(float v) { return Math.max(0f, Math.min(1f, v)); }
}
