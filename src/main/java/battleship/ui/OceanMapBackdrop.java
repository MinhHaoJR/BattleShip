package battleship.ui;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scenic 4-Season Celestial × Modern-Naval × Arcane-Magic ocean map painter.
 * Renders Blender 3D Sun & Moon arcs, 4-season oceanic palettes (Spring sakura,
 * Summer sun-glints, Autumn sunset leaves, Winter aurora & snow), rolling 3D wave crests,
 * Blender 3D storm thunderheads with forked lightning strikes, arcane leylines,
 * and patrolling wing-flapping 3D dragons without ever blocking mouse interaction.
 */
public final class OceanMapBackdrop {
    public enum MapTheme {
        NAVAL_CITADEL("Hải Vực · Đại Dương", "Map: Hải Vực", new Color(0x38BDF8)),
        FANTASY_REALM("Fantasy · Thần Giới", "Map: Fantasy", new Color(0xF472B6)),
        APOCALYPSE("Tận Thế · Biển Lửa", "Map: Tận Thế", new Color(0xF97316)),
        GALAXY_COSMOS("Galaxy · Vũ Trụ", "Map: Galaxy", new Color(0xA855F7)),
        ABYSSAL_ATLANTIS("Atlantis · Vực Sâu", "Map: Atlantis", new Color(0x2DD4BF));

        public final String displayName;
        public final String shortLabel;
        public final Color accent;

        MapTheme(String displayName, String shortLabel, Color accent) {
            this.displayName = displayName;
            this.shortLabel = shortLabel;
            this.accent = accent;
        }
    }

    public enum Season {
        AUTO("Tự động (4 Mùa)", "Mùa: Tự động", new Color(0x38BDF8)),
        SPRING("Xuân · Hoa Anh Đào", "Mùa: Xuân", new Color(0xF472B6)),
        SUMMER("Hạ · Nắng Vàng", "Mùa: Hạ", new Color(0xFBBF24)),
        AUTUMN("Thu · Hoàng Hôn", "Mùa: Thu", new Color(0xFB923C)),
        WINTER("Đông · Cực Quang", "Mùa: Đông", new Color(0x67E8F9));

        public final String displayName;
        public final String shortLabel;
        public final Color accent;

        Season(String displayName, String shortLabel, Color accent) {
            this.displayName = displayName;
            this.shortLabel = shortLabel;
            this.accent = accent;
        }
    }

    public enum WeatherMode {
        AUTO("Thời tiết: Tự động", "Thời tiết: Tự động", new Color(0x94A3B8)),
        CLEAR("Trời Quang & Trăng Sao", "Thời tiết: Quang", new Color(0xFACC15)),
        STORM("Bão Tố & Sấm Chớp", "Thời tiết: Bão Tố", new Color(0x38BDF8));

        public final String displayName;
        public final String shortLabel;
        public final Color accent;

        WeatherMode(String displayName, String shortLabel, Color accent) {
            this.displayName = displayName;
            this.shortLabel = shortLabel;
            this.accent = accent;
        }
    }

    private static final Color LEYLINE_CYAN = new Color(0x38BDF8);
    private static final Color LEYLINE_GOLD = new Color(0xFBBF24);
    private static final double SEASON_PERIOD_SEC = 16.0;
    private static final long ORIGIN_NANOS = System.nanoTime();

    private static volatile MapTheme selectedMapTheme = MapTheme.NAVAL_CITADEL;
    private static volatile Season selectedSeason = Season.AUTO;
    private static volatile WeatherMode selectedWeather = WeatherMode.AUTO;

    private OceanMapBackdrop() { }

    public static MapTheme getMapTheme() { return selectedMapTheme; }
    public static void setMapTheme(MapTheme theme) { selectedMapTheme = theme != null ? theme : MapTheme.NAVAL_CITADEL; }
    public static MapTheme cycleMapTheme() {
        MapTheme[] vals = MapTheme.values();
        selectedMapTheme = vals[(selectedMapTheme.ordinal() + 1) % vals.length];
        return selectedMapTheme;
    }

    public static Color gridCellFill(boolean hover) {
        return switch (selectedMapTheme) {
            case NAVAL_CITADEL -> hover ? new Color(24, 62, 102, 188) : new Color(11, 30, 52, 148);
            case FANTASY_REALM -> hover ? new Color(54, 34, 98, 184) : new Color(20, 16, 48, 134);
            case APOCALYPSE -> hover ? new Color(78, 26, 16, 184) : new Color(24, 11, 12, 130);
            case GALAXY_COSMOS -> hover ? new Color(56, 26, 106, 184) : new Color(9, 10, 30, 122);
            case ABYSSAL_ATLANTIS -> hover ? new Color(14, 78, 88, 186) : new Color(6, 28, 36, 136);
        };
    }

    public static Color gridLineColor() {
        return switch (selectedMapTheme) {
            case NAVAL_CITADEL -> new Color(56, 189, 248, 56);
            case FANTASY_REALM -> new Color(244, 114, 182, 62);
            case APOCALYPSE -> new Color(249, 115, 22, 66);
            case GALAXY_COSMOS -> new Color(168, 85, 247, 68);
            case ABYSSAL_ATLANTIS -> new Color(45, 212, 191, 64);
        };
    }

    public static Color boardBorderColor(boolean zombieFleet) {
        if (zombieFleet) return new Color(34, 197, 94, 118);
        return switch (selectedMapTheme) {
            case NAVAL_CITADEL -> new Color(56, 189, 248, 92);
            case FANTASY_REALM -> new Color(244, 114, 182, 105);
            case APOCALYPSE -> new Color(249, 115, 22, 112);
            case GALAXY_COSMOS -> new Color(168, 85, 247, 116);
            case ABYSSAL_ATLANTIS -> new Color(45, 212, 191, 108);
        };
    }

    public static Season getSeason() { return selectedSeason; }
    public static void setSeason(Season season) { selectedSeason = season != null ? season : Season.AUTO; }
    public static Season cycleSeason() {
        Season[] vals = Season.values();
        selectedSeason = vals[(selectedSeason.ordinal() + 1) % vals.length];
        return selectedSeason;
    }

    public static WeatherMode getWeatherMode() { return selectedWeather; }
    public static void setWeatherMode(WeatherMode mode) { selectedWeather = mode != null ? mode : WeatherMode.AUTO; }
    public static WeatherMode cycleWeatherMode() {
        WeatherMode[] vals = WeatherMode.values();
        selectedWeather = vals[(selectedWeather.ordinal() + 1) % vals.length];
        return selectedWeather;
    }

    public static double elapsedSeconds() {
        return (System.nanoTime() - ORIGIN_NANOS) / 1_000_000_000.0;
    }

    private static volatile long lastTriggeredLightningCycle = -1L;
    private static volatile long surgeStartMs = 0L;
    private static volatile long surgeUntilMs = 0L;
    private static volatile double surgeWindX = 0.0;
    private static volatile double surgeWindY = 0.0;
    private static volatile Color surgeTint = new Color(0x38BDF8);
    private static volatile int surgeIntensity = 1;

    /** Triggers a live environmental ocean wind & wave surge driven by a Titan Boss. */
    public static void triggerTitanEnvironmentSurge(double windX, double windY, Color tint, int intensity, long durationMs) {
        long now = System.currentTimeMillis();
        surgeStartMs = now;
        surgeUntilMs = now + Math.max(600L, durationMs);
        surgeWindX = windX;
        surgeWindY = windY;
        surgeTint = tint != null ? tint : new Color(0x38BDF8);
        surgeIntensity = Math.max(1, Math.min(3, intensity));
    }

    /** Clears any active Titan environmental surge (e.g., when switching modes or resetting). */
    public static void clearTitanEnvironmentSurge() {
        surgeUntilMs = 0L;
        surgeWindX = 0.0;
        surgeWindY = 0.0;
        surgeIntensity = 0;
    }

    /** Allocation-free query of active Titan environmental wind/wave surge [0..1]. */
    public static double sampleSurgeStrength() {
        long until = surgeUntilMs;
        long start = surgeStartMs;
        if (until <= start) return 0.0;
        long now = System.currentTimeMillis();
        if (now >= until) return 0.0;
        double p = Math.max(0.0, Math.min(1.0, (now - start) / (double) (until - start)));
        double attack = Math.min(1.0, p / 0.14);
        double decay = p > 0.45 ? Math.max(0.0, 1.0 - (p - 0.45) / 0.55) : 1.0;
        return attack * decay;
    }

    /** Returns active surge wind vector {windX, windY, strength}. */
    public static double[] sampleWindVector() {
        double s = sampleSurgeStrength();
        return new double[]{surgeWindX * s, surgeWindY * s, s};
    }

    /** Allocation-free query of current storm intensity [0..1] for weather-reactive boards and audio. */
    public static double sampleStormLevel(double t) {
        return switch (selectedWeather) {
            case CLEAR -> 0.0;
            case STORM -> 1.0;
            case AUTO -> {
                double phase = (t / SEASON_PERIOD_SEC) % 4.0;
                double winterW = selectedSeason == Season.WINTER ? 1.0
                        : (selectedSeason == Season.AUTO && ((int) Math.floor(phase) % 4 == 3) ? 1.0 : 0.0);
                double autumnW = selectedSeason == Season.AUTUMN ? 1.0
                        : (selectedSeason == Season.AUTO && ((int) Math.floor(phase) % 4 == 2) ? 1.0 : 0.0);
                double wave = 0.5 + 0.5 * Math.sin(t * 0.21 - 0.8);
                double bias = autumnW * 0.20 + winterW * 0.16;
                double raw = Math.max(0.0, (wave + bias - 0.58) / 0.42);
                yield Math.min(1.0, raw);
            }
        };
    }

    /** Allocation-free query of current lightning flash glare [0..1]. */
    public static double sampleLightningFlash(double t) {
        double storm = sampleStormLevel(t);
        if (storm <= 0.28) return 0.0;
        double slot = t % 3.4;
        return slot < 0.26 ? Math.sin((slot / 0.26) * Math.PI) * storm : 0.0;
    }

    /** Allocation-free query of active Winter intensity [0..1]. */
    public static double sampleWinterWeight(double t) {
        if (selectedSeason == Season.WINTER) return 1.0;
        if (selectedSeason != Season.AUTO) return 0.0;
        double phase = (t / SEASON_PERIOD_SEC) % 4.0;
        return ((int) Math.floor(phase) % 4 == 3) ? 0.85 : 0.0;
    }

    /** Allocation-free query of active Summer intensity [0..1]. */
    public static double sampleSummerWeight(double t) {
        if (selectedSeason == Season.SUMMER) return 1.0;
        if (selectedSeason != Season.AUTO) return 0.0;
        double phase = (t / SEASON_PERIOD_SEC) % 4.0;
        return ((int) Math.floor(phase) % 4 == 1) ? 0.85 : 0.0;
    }

    /** Synchronizes weather ambient loops and lightning thunder SFX when a live window stage paints. */
    public static void syncLiveEnvironmentAudio(double t) {
        double storm = sampleStormLevel(t);
        SoundEngine.updateAtmosphereState(selectedMapTheme, selectedSeason, storm);
        if (storm > 0.28) {
            double slot = t % 3.4;
            long cycle = (long) Math.floor(t / 3.4);
            if (slot < 0.30 && cycle != lastTriggeredLightningCycle) {
                lastTriggeredLightningCycle = cycle;
                SoundEngine.playThunderStrike(storm);
            }
        }
    }

    private static final BasicStroke STROKE_1 = new BasicStroke(1f);
    private static final BasicStroke STROKE_1_25 = new BasicStroke(1.25f);
    private static final BasicStroke STROKE_1_5_ROUND = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE_1_8_ROUND = new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE_2_4_ROUND = new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE_4_5_ROUND = new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE_7_0_ROUND = new BasicStroke(7.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private static final class BakedBackdrop {
        BufferedImage terrainImage;
        BufferedImage image;
        VolatileImage vramImage;
        int w;
        int h;
        int devW;
        int devH;
        double scaleX = 1.0;
        double scaleY = 1.0;
        boolean dualBoard;
        MapTheme theme;
        Season season;
        WeatherMode weather;
        boolean zombie;
        double terrainT = Double.NaN;
        double bakedT = Double.NaN;
    }

    private static final BakedBackdrop BAKED_SINGLE = new BakedBackdrop();
    private static final BakedBackdrop BAKED_DUAL = new BakedBackdrop();
    private static final Map<Long, BufferedImage> HALO_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Long, BufferedImage> eldest) {
                    return size() > 128;
                }
            });

    private static BufferedImage getHaloStamp(Color col, int radius, int peakAlpha) {
        if (radius <= 0) return null;
        int qRadius = Math.max(4, ((radius + 2) / 4) * 4);
        int quantizedAlpha = Math.max(0, Math.min(255, (peakAlpha / 8) * 8));
        long key = (((long) (col.getRGB() & 0xFFFFFF)) << 20) | (((long) qRadius) << 8) | quantizedAlpha;
        BufferedImage stamp = HALO_CACHE.get(key);
        if (stamp != null) return stamp;
        int d = qRadius * 2;
        stamp = new BufferedImage(d, d, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D hg = stamp.createGraphics();
        hg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hg.setPaint(new RadialGradientPaint(
                qRadius, qRadius, qRadius,
                new float[]{0.0f, 0.55f, 1.0f},
                new Color[]{
                        new Color(col.getRed(), col.getGreen(), col.getBlue(), quantizedAlpha),
                        new Color(col.getRed(), col.getGreen(), col.getBlue(), quantizedAlpha / 3),
                        new Color(col.getRed(), col.getGreen(), col.getBlue(), 0)
                }
        ));
        hg.fillOval(0, 0, d, d);
        hg.dispose();
        HALO_CACHE.put(key, stamp);
        return stamp;
    }

    /** Paints the layered 5-theme celestial & storm ocean map with device-DPI offscreen & VRAM caching. */
    public static void paintOceanMap(Graphics2D g, int w, int h, double t, boolean dualBoardLayout) {
        if (w <= 0 || h <= 0) return;
        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        Paint prevPaint = g.getPaint();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        Atmosphere atmo = resolveAtmosphere(t);
        MapTheme theme = selectedMapTheme;
        boolean zombie = ZombieMode.isZombieMode();

        AffineTransform rootTx = g.getTransform();
        double sx = 1.0;
        double sy = 1.0;
        if (rootTx.getShearX() == 0.0 && rootTx.getShearY() == 0.0 && rootTx.getScaleX() > 0 && rootTx.getScaleY() > 0) {
            sx = rootTx.getScaleX();
            sy = rootTx.getScaleY();
        }
        int devW = Math.max(1, (int) Math.round(w * sx));
        int devH = Math.max(1, (int) Math.round(h * sy));

        BakedBackdrop cache = dualBoardLayout ? BAKED_DUAL : BAKED_SINGLE;
        boolean sizeOrScaleChanged = cache.image == null
                || cache.devW != devW || cache.devH != devH
                || Math.abs(cache.scaleX - sx) > 1e-3 || Math.abs(cache.scaleY - sy) > 1e-3;
        boolean configChanged = sizeOrScaleChanged
                || cache.w != w || cache.h != h
                || cache.dualBoard != dualBoardLayout
                || cache.theme != theme
                || cache.season != selectedSeason
                || cache.weather != selectedWeather
                || cache.zombie != zombie;

        boolean terrainExpired = configChanged
                || cache.terrainImage == null
                || Double.isNaN(cache.terrainT)
                || ((selectedSeason == Season.AUTO || selectedWeather == WeatherMode.AUTO)
                        && Math.abs(t - cache.terrainT) >= 2.0);

        if (terrainExpired) {
            if (cache.terrainImage == null || cache.devW != devW || cache.devH != devH) {
                cache.terrainImage = new BufferedImage(devW, devH, BufferedImage.TYPE_INT_ARGB_PRE);
            }
            Graphics2D tg = cache.terrainImage.createGraphics();
            tg.scale(sx, sy);
            tg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            tg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            paintStaticTerrainBase(tg, w, h, t, dualBoardLayout, atmo, theme);
            tg.dispose();
            cache.terrainT = t;
        }

        boolean needsRebake = terrainExpired
                || Double.isNaN(cache.bakedT)
                || Math.abs(t - cache.bakedT) >= 0.85;

        if (needsRebake) {
            if (cache.image == null || cache.devW != devW || cache.devH != devH) {
                cache.image = new BufferedImage(devW, devH, BufferedImage.TYPE_INT_ARGB_PRE);
            }
            Graphics2D bg = cache.image.createGraphics();
            bg.setComposite(AlphaComposite.Src);
            bg.drawImage(cache.terrainImage, 0, 0, null);
            bg.scale(sx, sy);
            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            bg.setComposite(AlphaComposite.SrcOver);
            paintDynamicMidFoundation(bg, w, h, t, dualBoardLayout, atmo, theme);
            bg.dispose();
            cache.w = w;
            cache.h = h;
            cache.devW = devW;
            cache.devH = devH;
            cache.scaleX = sx;
            cache.scaleY = sy;
            cache.dualBoard = dualBoardLayout;
            cache.theme = theme;
            cache.season = selectedSeason;
            cache.weather = selectedWeather;
            cache.zombie = zombie;
            cache.bakedT = t;
        }

        // Fast 1:1 native device-aligned blit (VRAM VolatileImage when available, zero HiDPI resample)
        g.setComposite(AlphaComposite.SrcOver);
        GraphicsConfiguration gc = GraphicsEnvironment.isHeadless() ? null : g.getDeviceConfiguration();
        if (gc != null) {
            if (cache.vramImage == null || cache.vramImage.getWidth() != devW || cache.vramImage.getHeight() != devH) {
                if (cache.vramImage != null) cache.vramImage.flush();
                cache.vramImage = gc.createCompatibleVolatileImage(devW, devH, Transparency.OPAQUE);
            }
            int status = cache.vramImage.validate(gc);
            if (status == VolatileImage.IMAGE_INCOMPATIBLE) {
                cache.vramImage.flush();
                cache.vramImage = gc.createCompatibleVolatileImage(devW, devH, Transparency.OPAQUE);
                status = cache.vramImage.validate(gc);
            }
            if (needsRebake || status == VolatileImage.IMAGE_RESTORED) {
                Graphics2D vg = cache.vramImage.createGraphics();
                vg.setComposite(AlphaComposite.Src);
                vg.drawImage(cache.image, 0, 0, null);
                vg.dispose();
            }
            if (!cache.vramImage.contentsLost()) {
                SpriteAssets.drawDeviceAligned(g, cache.vramImage, 0, 0);
            } else {
                SpriteAssets.drawDeviceAligned(g, cache.image, 0, 0);
            }
        } else {
            SpriteAssets.drawDeviceAligned(g, cache.image, 0, 0);
        }

        if (atmo.lightningFlash > 0.04) {
            g.setComposite(AlphaComposite.SrcOver.derive((float) (atmo.lightningFlash * 0.22)));
            g.setColor(new Color(0x476C9B));
            g.fillRect(0, 0, w, h);
        }

        float cx = w * 0.5f;
        float cy = h * 0.5f;
        int minDim = Math.min(w, h);

        // Live rolling surface swells + Leyline pulses + radar sweep cone
        paintRollingWavesAndCrests(g, w, h, t, atmo, theme);

        int leftX = (int) (w * (dualBoardLayout ? 0.055 : 0.052));
        int leftY = (int) (h * (dualBoardLayout ? 0.078 : 0.062));
        int rightX = (int) (w * (dualBoardLayout ? 0.945 : 0.948));
        int rightY = (int) (h * (dualBoardLayout ? 0.078 : 0.062));
        int anchorTopX = (int) (w * (dualBoardLayout ? 0.500 : 0.052));
        int anchorTopY = (int) (h * (dualBoardLayout ? 0.285 : 0.935));
        int anchorBotX = (int) (w * (dualBoardLayout ? 0.500 : 0.948));
        int anchorBotY = (int) (h * (dualBoardLayout ? 0.875 : 0.935));
        paintLeylinePulses(g, leftX, leftY, cx, cy, atmo.leyPrimary, t, 0.0);
        paintLeylinePulses(g, rightX, rightY, cx, cy, atmo.leySecondary, t, 0.35);
        paintLeylinePulses(g, anchorBotX, anchorBotY, cx, cy, LEYLINE_GOLD, t, 0.65);
        paintLeylinePulses(g, cx, cy, anchorTopX, anchorTopY, atmo.leyPrimary, t, 0.18);

        paintArcaneCompassSweep(g, cx, cy, Math.max(85, minDim * 0.21), t, atmo);

        g.setPaint(prevPaint);
        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private static void paintStaticTerrainBase(
            Graphics2D g, int w, int h, double t, boolean dualBoardLayout, Atmosphere atmo, MapTheme theme) {
        // 1. Theme & Seasonal Ocean Base + Zenith Glow (cached until theme/season/weather changes)
        Color topCol = atmo.storm > 0.1 ? lerpColor(atmo.skyEdge, new Color(0x030812), atmo.storm * 0.62) : atmo.skyEdge;
        Color midCol = atmo.storm > 0.1 ? lerpColor(atmo.oceanMid, new Color(0x081526), atmo.storm * 0.50) : atmo.oceanMid;
        g.setPaint(new GradientPaint(0, 0, topCol, 0, h, midCol));
        g.fillRect(0, 0, w, h);

        float cx = w * 0.5f;
        float cy = h * 0.5f;
        float rad = Math.max(120f, Math.max(w, h) * 0.64f);
        g.setPaint(new RadialGradientPaint(
                cx, cy, rad,
                new float[]{0.0f, 0.48f, 1.0f},
                new Color[]{atmo.oceanChannel, midCol, topCol}
        ));
        g.fillRect(0, 0, w, h);

        // Theme-specific subterranean / cosmic / caustic strata below waves
        paintThemeBackdropStrata(g, w, h, t, theme, atmo);

        // Tactical chart grid / star-grid & concentric sonar rings
        g.setComposite(AlphaComposite.SrcOver.derive(0.11f));
        g.setColor(atmo.leyPrimary);
        g.setStroke(STROKE_1);
        int step = 72;
        for (int x = step / 2; x < w; x += step) g.drawLine(x, 0, x, h);
        for (int y = step / 2; y < h; y += step) g.drawLine(0, y, w, y);

        int minDim = Math.min(w, h);
        for (int ring = 1; ring <= 4; ring++) {
            int r = (int) (minDim * (0.14 * ring));
            g.setComposite(AlphaComposite.SrcOver.derive(ring % 2 == 0 ? 0.13f : 0.08f));
            g.drawOval((int) (cx - r), (int) (cy - r), r * 2, r * 2);
        }

        // Static Leyline curved conduits
        int leftX = (int) (w * (dualBoardLayout ? 0.055 : 0.052));
        int leftY = (int) (h * (dualBoardLayout ? 0.078 : 0.062));
        int rightX = (int) (w * (dualBoardLayout ? 0.945 : 0.948));
        int rightY = (int) (h * (dualBoardLayout ? 0.078 : 0.062));
        int anchorTopX = (int) (w * (dualBoardLayout ? 0.500 : 0.052));
        int anchorTopY = (int) (h * (dualBoardLayout ? 0.285 : 0.935));
        int anchorBotX = (int) (w * (dualBoardLayout ? 0.500 : 0.948));
        int anchorBotY = (int) (h * (dualBoardLayout ? 0.875 : 0.935));

        paintLeylineCurveOnly(g, leftX, leftY, cx, cy, atmo.leyPrimary);
        paintLeylineCurveOnly(g, rightX, rightY, cx, cy, atmo.leySecondary);
        paintLeylineCurveOnly(g, anchorBotX, anchorBotY, cx, cy, LEYLINE_GOLD);
        paintLeylineCurveOnly(g, cx, cy, anchorTopX, anchorTopY, atmo.leyPrimary);
    }

    private static void paintDynamicMidFoundation(
            Graphics2D g, int w, int h, double t, boolean dualBoardLayout, Atmosphere atmo, MapTheme theme) {
        if (theme == MapTheme.GALAXY_COSMOS) {
            for (int s = 0; s < 85; s++) {
                double tw = 0.35 + 0.65 * Math.abs(Math.sin(t * (0.9 + (s % 5) * 0.25) + s * 1.7));
                int sx = (int) ((s * 173.7 + 41.0) % Math.max(1, w));
                int sy = (int) ((s * 97.3 + 29.0) % Math.max(1, h));
                int sr = (s % 7 == 0) ? 3 : 2;
                g.setComposite(AlphaComposite.SrcOver.derive((float) (tw * 0.85)));
                g.setColor(s % 3 == 0 ? new Color(0xE0F2FE) : (s % 3 == 1 ? new Color(0xF5D0FE) : Color.WHITE));
                g.fillOval(sx, sy, sr, sr);
            }
        }

        // Winter Aurora Borealis shimmer ribbons near upper ocean
        if (atmo.winterWeight > 0.08 && theme != MapTheme.APOCALYPSE) {
            paintAuroraRibbons(g, w, h, t, (float) (atmo.winterWeight * (1.0 - atmo.storm * 0.45)));
        }

        float cx = w * 0.5f;
        float cy = h * 0.5f;
        int minDim = Math.min(w, h);
        int leftX = (int) (w * (dualBoardLayout ? 0.055 : 0.052));
        int leftY = (int) (h * (dualBoardLayout ? 0.078 : 0.062));
        int rightX = (int) (w * (dualBoardLayout ? 0.945 : 0.948));
        int rightY = (int) (h * (dualBoardLayout ? 0.078 : 0.062));
        int anchorTopX = (int) (w * (dualBoardLayout ? 0.500 : 0.052));
        int anchorTopY = (int) (h * (dualBoardLayout ? 0.285 : 0.935));
        int anchorBotX = (int) (w * (dualBoardLayout ? 0.500 : 0.948));
        int anchorBotY = (int) (h * (dualBoardLayout ? 0.875 : 0.935));

        int isleSize = dualBoardLayout
                ? Math.max(80, Math.min(98, (int) (minDim * 0.142)))
                : Math.max(74, Math.min(92, (int) (minDim * 0.120)));
        int beaconSize = Math.max(46, Math.min(62, (int) (minDim * 0.086)));

        String leftKey = switch (theme) {
            case NAVAL_CITADEL -> "fortress";
            case FANTASY_REALM -> "fantasy_citadel";
            case APOCALYPSE -> "apoc_volcano";
            case GALAXY_COSMOS -> "galaxy_blackhole";
            case ABYSSAL_ATLANTIS -> "atlantis_temple";
        };
        String rightKey = switch (theme) {
            case NAVAL_CITADEL -> "arcane";
            case FANTASY_REALM -> "fantasy_sanctuary";
            case APOCALYPSE -> "apoc_ruin";
            case GALAXY_COSMOS -> "galaxy_planet";
            case ABYSSAL_ATLANTIS -> "atlantis_reef";
        };
        String straitKey = switch (theme) {
            case NAVAL_CITADEL -> "beacon";
            case FANTASY_REALM -> "fantasy_portal";
            case APOCALYPSE -> "apoc_meteor";
            case GALAXY_COSMOS -> "galaxy_station";
            case ABYSSAL_ATLANTIS -> "atlantis_trident";
        };

        paintIslandWithAura(g, SpriteAssets.getMapProp(leftKey), leftX, leftY, isleSize,
                atmo.leySecondary, t, theme == MapTheme.NAVAL_CITADEL, 0.94f, atmo.storm);
        paintIslandWithAura(g, SpriteAssets.getMapProp(rightKey), rightX, rightY, isleSize,
                atmo.leyPrimary, t, false, 0.95f, atmo.storm);
        paintBeacon(g, SpriteAssets.getMapProp(straitKey), anchorTopX, anchorTopY, beaconSize, t, 0.0, atmo.leyPrimary, atmo.storm);
        paintBeacon(g, SpriteAssets.getMapProp(straitKey), anchorBotX, anchorBotY, beaconSize, t, 1.7, atmo.leySecondary, atmo.storm);

        // Bake slow-rotating Celestial Crown + Arcane Compass Rose base (~4.2 Hz refresh)
        paintCelestialCrown(g, w, h, t, dualBoardLayout, atmo, theme);
        paintArcaneCompassBase(g, cx, cy, Math.max(85, minDim * 0.21), t, atmo);

        if (atmo.storm > 0.04) {
            paintStormCloudsAndLightning(g, w, h, t, dualBoardLayout, atmo);
        }
    }

    private static void paintThemeBackdropStrata(Graphics2D g, int w, int h, double t, MapTheme theme, Atmosphere atmo) {
        Paint prevP = g.getPaint();
        switch (theme) {
            case GALAXY_COSMOS -> {
                float[][] lobes = {
                        {w * 0.28f, h * 0.34f, w * 0.42f, 0.24f},
                        {w * 0.72f, h * 0.62f, w * 0.45f, 0.22f},
                        {w * 0.50f, h * 0.48f, w * 0.36f, 0.18f}
                };
                Color[] cA = {new Color(0x9333EA), new Color(0x06B6D4), new Color(0xEC4899)};
                for (int i = 0; i < lobes.length; i++) {
                    int lr = (int) Math.max(80, lobes[i][2]);
                    int peak = (int) (78 * (lobes[i][3] / 0.24));
                    BufferedImage neb = getHaloStamp(cA[i], Math.min(180, lr / 3), peak);
                    if (neb != null) {
                        g.setComposite(AlphaComposite.SrcOver);
                        g.drawImage(neb, (int) (lobes[i][0] - lr), (int) (lobes[i][1] - lr), lr * 2, lr * 2, null);
                    }
                }
            }
            case APOCALYPSE -> {
                for (int f = 0; f < 6; f++) {
                    double pulse = 0.55 + 0.45 * Math.sin(t * 1.8 + f * 1.1);
                    Path2D crack = new Path2D.Double();
                    double x0 = (f % 2 == 0) ? 0 : w;
                    double dir = (f % 2 == 0) ? 1 : -1;
                    double y0 = h * (0.14 + f * 0.14);
                    crack.moveTo(x0, y0);
                    for (int k = 1; k <= 6; k++) {
                        double cxp = x0 + dir * (k * w * 0.16);
                        double cyp = y0 + Math.sin(f * 3.1 + k * 2.2) * 32.0;
                        crack.lineTo(cxp, cyp);
                    }
                    g.setComposite(AlphaComposite.SrcOver.derive((float) (0.18 * pulse)));
                    g.setColor(new Color(0xEA580C));
                    g.setStroke(new BasicStroke(6.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(crack);
                    g.setComposite(AlphaComposite.SrcOver.derive((float) (0.48 * pulse)));
                    g.setColor(new Color(0xFDE047));
                    g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(crack);
                }
                g.setComposite(AlphaComposite.SrcOver);
                g.setPaint(new GradientPaint(0, h * 0.65f, new Color(249, 115, 22, 0), 0, h, new Color(220, 38, 38, 65)));
                g.fillRect(0, (int) (h * 0.65f), w, h);
            }
            case FANTASY_REALM -> {
                float[][] pools = {{w * 0.25f, h * 0.30f}, {w * 0.76f, h * 0.70f}};
                Color[] cols = {new Color(0xEC4899), new Color(0x6366F1)};
                for (int i = 0; i < 2; i++) {
                    int pr = (int) Math.max(90, w * 0.36f);
                    BufferedImage pool = getHaloStamp(cols[i], Math.min(180, pr / 3), 52);
                    if (pool != null) {
                        g.setComposite(AlphaComposite.SrcOver);
                        g.drawImage(pool, (int) (pools[i][0] - pr), (int) (pools[i][1] - pr), pr * 2, pr * 2, null);
                    }
                }
            }
            case ABYSSAL_ATLANTIS -> {
                g.setComposite(AlphaComposite.SrcOver.derive(0.08f));
                g.setColor(new Color(0x5EEAD4));
                for (int r = 0; r < 5; r++) {
                    double bx = w * (0.12 + r * 0.19) + Math.sin(t * 0.4 + r) * 22.0;
                    Path2D ray = new Path2D.Double();
                    ray.moveTo(bx - 18, 0);
                    ray.lineTo(bx + 26, 0);
                    ray.lineTo(bx + 110, h);
                    ray.lineTo(bx + 18, h);
                    ray.closePath();
                    g.fill(ray);
                }
            }
            case NAVAL_CITADEL -> { }
        }
        g.setPaint(prevP);
    }

    /**
     * Paints seasonal atmospheric particles (cherry petals, summer sun motes, autumn leaves,
     * winter snow, and storm driving rain + sheet flash) in the transparent foreground overlay.
     */
    public static void paintAtmosphericWeather(Graphics2D g, int w, int h, double t) {
        if (w <= 40 || h <= 40) return;
        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        Atmosphere atmo = resolveAtmosphere(t);

        // Sky-to-sea lightning strike hitting the map surface + electrified ocean ripples
        if (atmo.storm > 0.28) {
            double slot = t % 3.4;
            double cycle = Math.floor(t / 3.4);
            double xBase = w * (0.20 + ((cycle * 37.0) % 60.0) / 100.0);
            double strikeY = h * (0.52 + ((cycle * 19.0) % 30.0) / 100.0);

            if (atmo.lightningFlash > 0.04) {
                g.setComposite(AlphaComposite.SrcOver.derive((float) (atmo.lightningFlash * 0.16)));
                g.setColor(selectedMapTheme == MapTheme.APOCALYPSE ? new Color(0xFDBA74) : new Color(0xBAE6FD));
                g.fillRect(0, 0, w, h);

                if (atmo.lightningFlash > 0.06) {
                    double yTop = h * 0.04;
                    Path2D bolt = new Path2D.Double();
                    Path2D fork = new Path2D.Double();
                    bolt.moveTo(xBase, yTop);
                    double branchX = xBase, branchY = yTop;
                    double curX = xBase;
                    for (int seg = 1; seg <= 7; seg++) {
                        double frac = seg / 7.0;
                        double cy = yTop + (strikeY - yTop) * frac;
                        curX = xBase + (seg == 7 ? 0.0 : Math.sin(cycle * 9.3 + seg * 2.7) * 30.0);
                        bolt.lineTo(curX, cy);
                        if (seg == 3) {
                            branchX = curX;
                            branchY = cy;
                        }
                    }
                    fork.moveTo(branchX, branchY);
                    for (int seg = 1; seg <= 3; seg++) {
                        fork.lineTo(branchX + seg * 24.0 + Math.cos(cycle + seg) * 12.0,
                                branchY + seg * ((strikeY - branchY) * 0.26));
                    }

                    Color arcHue = selectedMapTheme == MapTheme.APOCALYPSE ? new Color(0xFB923C)
                            : (selectedMapTheme == MapTheme.GALAXY_COSMOS ? new Color(0xC084FC) : new Color(0x38BDF8));
                    float fAlpha = (float) Math.min(1.0, atmo.lightningFlash * 1.15);
                    g.setComposite(AlphaComposite.SrcOver.derive(fAlpha * 0.54f));
                    g.setColor(arcHue);
                    g.setStroke(STROKE_7_0_ROUND);
                    g.draw(bolt);
                    g.setStroke(STROKE_4_5_ROUND);
                    g.draw(fork);

                    g.setComposite(AlphaComposite.SrcOver.derive(fAlpha * 0.96f));
                    g.setColor(new Color(0xF0F9FF));
                    g.setStroke(STROKE_2_4_ROUND);
                    g.draw(bolt);
                    g.setStroke(STROKE_1_5_ROUND);
                    g.draw(fork);

                    // Electrified impact core at ocean surface strike point
                    int flashR = (int) (18 + 20 * atmo.lightningFlash);
                    g.setComposite(AlphaComposite.SrcOver.derive(fAlpha * 0.72f));
                    g.setColor(arcHue);
                    g.fillOval((int) xBase - flashR, (int) strikeY - flashR / 2, flashR * 2, flashR);
                    g.setColor(Color.WHITE);
                    g.fillOval((int) xBase - 8, (int) strikeY - 4, 16, 8);
                }
            }

            // Expanding electrified water shockwave ripples persisting slightly after strike
            if (slot < 0.95) {
                double rp = slot / 0.95;
                float rAlpha = (float) Math.max(0.0, (1.0 - rp) * 0.55 * atmo.storm);
                if (rAlpha > 0.02f) {
                    int rx = (int) (14 + rp * 64);
                    int ry = Math.max(8, rx / 2);
                    g.setComposite(AlphaComposite.SrcOver.derive(rAlpha));
                    g.setStroke(STROKE_1_8_ROUND);
                    g.setColor(selectedMapTheme == MapTheme.APOCALYPSE ? new Color(0xFDE047) : new Color(0x7DD3FC));
                    g.drawOval((int) xBase - rx, (int) strikeY - ry, rx * 2, ry * 2);
                    if (rp > 0.16) {
                        int rx2 = (int) (8 + (rp - 0.16) * 52);
                        g.drawOval((int) xBase - rx2, (int) strikeY - rx2 / 4, rx2 * 2, rx2 / 2);
                    }
                }
            }
        }

        // Storm wind mist sweeping across map horizon
        double surgeStrength = sampleSurgeStrength();
        if (atmo.storm > 0.22 || surgeStrength > 0.04) {
            g.setStroke(STROKE_2_4_ROUND);
            int mistCount = 4 + (surgeStrength > 0.04 ? 3 : 0);
            double windDirX = surgeStrength > 0.04 ? (surgeWindX >= 0 ? 1.0 : -1.0) : 1.0;
            double mistSpeed = 1.0 + surgeStrength * 1.9;
            for (int m = 0; m < mistCount; m++) {
                double drift = (t * (0.28 + m * 0.045) * mistSpeed + m * 0.21) % 1.0;
                double fracX = windDirX >= 0 ? drift : (1.0 - drift);
                int mx = (int) (fracX * (w + 260)) - 220;
                int my = (int) (h * (0.14 + m * 0.12) + Math.sin(t * 1.4 + m) * 14 + surgeWindY * surgeStrength * 18.0);
                float baseStr = (float) Math.max(atmo.storm * 0.11, surgeStrength * 0.16);
                float ma = (float) (Math.sin(drift * Math.PI) * baseStr);
                if (ma > 0.01f) {
                    g.setComposite(AlphaComposite.SrcOver.derive(ma));
                    g.setColor(surgeStrength > 0.22 ? surgeTint : new Color(0xBAE6FD));
                    g.fillRoundRect(mx, my, 190 + (m % 2) * 50, 14, 14, 14);
                }
            }
            if (surgeStrength > 0.06) {
                // High-velocity horizontal gale ribbons & blown ocean spindrift across screen
                int galeCount = 10 + surgeIntensity * 5;
                g.setStroke(STROKE_1_8_ROUND);
                for (int k = 0; k < galeCount; k++) {
                    double gp = (t * (1.25 + (k % 4) * 0.28) + k * 0.19) % 1.0;
                    double gxFrac = surgeWindX >= 0 ? gp : (1.0 - gp);
                    double gx = gxFrac * (w + 220) - 110;
                    double gy = h * (0.08 + ((k * 17) % 84) / 100.0) + surgeWindY * 22.0 * Math.sin(gp * Math.PI);
                    double len = 48 + (k % 4) * 28 + Math.abs(surgeWindX) * 12.0;
                    double dx = (surgeWindX >= 0 ? 1 : -1) * len;
                    double dy = surgeWindY * 14.0;
                    float ga = (float) (Math.sin(gp * Math.PI) * 0.26 * surgeStrength);
                    if (ga > 0.02f) {
                        g.setComposite(AlphaComposite.SrcOver.derive(ga));
                        g.setColor(k % 3 == 0 ? Color.WHITE : surgeTint);
                        g.drawLine((int) gx, (int) gy, (int) (gx + dx), (int) (gy + dy));
                    }
                }
            }
        }

        // Spring: Cherry Blossom Petals
        if (atmo.springWeight > 0.08) {
            double alphaMul = atmo.springWeight * (1.0 - atmo.storm * 0.45);
            for (int i = 0; i < 24; i++) {
                double seed = i * 19.37;
                double u = (t * (0.045 + (i % 4) * 0.01) + i * 0.041) % 1.0;
                double x = ((seed * 37.0 + t * (28.0 + (i % 3) * 10.0) + Math.sin(t * 1.8 + seed) * 26.0) % (w + 60)) - 30;
                if (x < -30) x += w + 60;
                double y = u * (h + 40) - 20;
                g.setComposite(AlphaComposite.SrcOver.derive((float) (0.58 * alphaMul)));
                g.setColor(i % 2 == 0 ? new Color(0xF9A8D4) : new Color(0xFCE7F3));
                AffineTransform prevTx = g.getTransform();
                g.translate(x, y);
                g.rotate(t * 1.4 + seed);
                g.fillOval(-5, -2, 10, 5);
                g.setTransform(prevTx);
            }
        }

        // Summer: Warm Golden Sea-Glints & Sunbeams
        if (atmo.summerWeight > 0.08) {
            double alphaMul = atmo.summerWeight * (1.0 - atmo.storm * 0.7);
            for (int i = 0; i < 18; i++) {
                double pulse = Math.max(0.0, Math.sin(t * 2.2 + i * 1.3));
                if (pulse <= 0.15) continue;
                double x = ((i * 97.3 + Math.sin(t * 0.5 + i) * 18.0) % Math.max(1, w - 40)) + 20;
                double y = ((i * 61.7 + Math.cos(t * 0.4 + i) * 14.0) % Math.max(1, h - 40)) + 20;
                g.setComposite(AlphaComposite.SrcOver.derive((float) (pulse * 0.34 * alphaMul)));
                g.setColor(new Color(0xFDE68A));
                int sz = 4 + (i % 3) * 2;
                g.drawLine((int) x - sz, (int) y, (int) x + sz, (int) y);
                g.drawLine((int) x, (int) y - sz, (int) x, (int) y + sz);
            }
        }

        // Autumn: Drifting Maple Leaves
        if (atmo.autumnWeight > 0.08) {
            double alphaMul = atmo.autumnWeight;
            for (int i = 0; i < 20; i++) {
                double seed = i * 23.11;
                double u = (t * (0.05 + (i % 3) * 0.012) + i * 0.05) % 1.0;
                double x = ((seed * 41.0 + t * (36.0 + atmo.storm * 45.0) + Math.sin(t * 2.1 + seed) * 24.0) % (w + 60)) - 30;
                if (x < -30) x += w + 60;
                double y = u * (h + 40) - 20;
                g.setComposite(AlphaComposite.SrcOver.derive((float) (0.58 * alphaMul)));
                g.setColor(i % 3 == 0 ? new Color(0xFB923C) : (i % 3 == 1 ? new Color(0xF59E0B) : new Color(0xF87171)));
                AffineTransform prevTx = g.getTransform();
                g.translate(x, y);
                g.rotate(t * 1.9 + seed);
                Path2D leaf = new Path2D.Double();
                leaf.moveTo(0, -6);
                leaf.lineTo(5, 0);
                leaf.lineTo(0, 6);
                leaf.lineTo(-5, 0);
                leaf.closePath();
                g.fill(leaf);
                g.setTransform(prevTx);
            }
        }

        // Winter: Soft Drifting Snowflakes
        if (atmo.winterWeight > 0.08) {
            double alphaMul = atmo.winterWeight;
            for (int i = 0; i < 34; i++) {
                double seed = i * 17.7;
                double u = (t * (0.06 + (i % 4) * 0.014) + i * 0.029) % 1.0;
                double x = ((seed * 53.0 + Math.sin(t * 1.2 + seed) * 32.0 + atmo.storm * t * 35.0) % (w + 40)) - 20;
                if (x < -20) x += w + 40;
                double y = u * (h + 20) - 10;
                int r = 2 + (i % 3);
                g.setComposite(AlphaComposite.SrcOver.derive((float) (0.68 * alphaMul)));
                Color flakeCol = i % 4 == 0 ? new Color(0xBAE6FD) : Color.WHITE;
                SpriteAssets.drawVoxelCube(g, (int) x, (int) y, r + 1, flakeCol, Color.WHITE);
            }
        }

        // Storm: Wind-Driven Diagonal Rain Streaks
        if (atmo.storm > 0.06) {
            int dropCount = (int) Math.round(18 + atmo.storm * 42);
            g.setStroke(STROKE_1_25);
            g.setColor(new Color(0xBAE6FD));
            for (int i = 0; i < dropCount; i++) {
                double seed = i * 31.7;
                double speed = 1.45 + (i % 4) * 0.22;
                double phase = (t * speed + i * 0.073) % 1.0;
                double rx = ((seed * 67.0 + phase * w * 0.35) % (w + 80)) - 40;
                double ry = phase * (h + 60) - 30;
                float alpha = (float) (0.24 + (i % 3) * 0.09) * (float) atmo.storm;
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                g.drawLine((int) rx, (int) ry, (int) (rx - 14), (int) (ry + 28));
            }
        }

        // Zombie Mode: Drifting glowing voxel bio-spores & necrotic embers rising across abyssal sea
        if (ZombieMode.isZombieMode()) {
            for (int i = 0; i < 20; i++) {
                double seed = i * 29.1;
                double phase = (t * (0.08 + (i % 3) * 0.025) + i * 0.05) % 1.0;
                double zx = ((seed * 47.0 + Math.sin(t * 1.4 + seed) * 26.0) % Math.max(1, w - 40)) + 20;
                double zy = h * (1.0 - phase);
                int r = 3 + (i % 3);
                g.setComposite(AlphaComposite.SrcOver.derive((float) (Math.sin(phase * Math.PI) * 0.56)));
                Color sporeCol = i % 3 == 0 ? new Color(0xF43F5E) : new Color(0x22C55E);
                SpriteAssets.drawVoxelCube(g, (int) zx, (int) zy, r + 1, sporeCol, new Color(0xFEF08A));
            }
        }

        // Theme-specific foreground atmosphere particles (voxel fairies, volcanic embers, shooting stars, pearls)
        paintThemeForegroundParticles(g, w, h, t, selectedMapTheme);

        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private static void paintThemeForegroundParticles(Graphics2D g, int w, int h, double t, MapTheme theme) {
        switch (theme) {
            case FANTASY_REALM -> {
                for (int i = 0; i < 18; i++) {
                    double fx = ((i * 89.3 + t * 14.0 + Math.sin(t * 1.5 + i) * 32.0) % Math.max(1, w - 30)) + 15;
                    double fy = ((i * 53.7 + Math.cos(t * 1.2 + i * 1.9) * 28.0) % Math.max(1, h - 30)) + 15;
                    double pulse = 0.4 + 0.6 * Math.abs(Math.sin(t * 2.6 + i * 1.4));
                    Color fc = (i % 2 == 0) ? new Color(0xF472B6) : new Color(0x67E8F9);
                    g.setComposite(AlphaComposite.SrcOver.derive((float) (0.34 * pulse)));
                    g.setColor(fc);
                    g.fillRect((int) fx - 4, (int) fy - 4, 8, 8);
                    g.setComposite(AlphaComposite.SrcOver.derive((float) (0.85 * pulse)));
                    g.setColor(Color.WHITE);
                    g.fillRect((int) fx - 2, (int) fy - 2, 4, 4);
                }
            }
            case APOCALYPSE -> {
                // Hot glowing volcanic voxel embers rising upward + dark ash cubes
                for (int i = 0; i < 28; i++) {
                    double seed = i * 37.3;
                    double phase = (t * (0.11 + (i % 4) * 0.035) + i * 0.04) % 1.0;
                    double ex = ((seed * 51.0 + t * 22.0 + Math.sin(t * 2.4 + seed) * 24.0) % Math.max(1, w + 40)) - 20;
                    if (ex < 0) ex += w;
                    double ey = h * (1.0 - phase);
                    int er = 3 + (i % 3);
                    float a = (float) (Math.sin(phase * Math.PI) * (i % 3 == 0 ? 0.82 : 0.66));
                    g.setComposite(AlphaComposite.SrcOver.derive(a));
                    Color col = i % 4 == 0 ? new Color(0xFDE047) : (i % 4 == 3 ? new Color(0x374151) : new Color(0xFB923C));
                    Color rim = (i % 4 == 3) ? null : Color.WHITE;
                    SpriteAssets.drawVoxelCube(g, (int) ex, (int) ey, er, col, rim);
                }
            }
            case GALAXY_COSMOS -> {
                // Periodic diagonal shooting meteors streaking cleanly across cosmic sky
                g.setStroke(STROKE_1_8_ROUND);
                for (int m = 0; m < 3; m++) {
                    double cycle = (t * 0.24 + m * 0.34) % 1.0;
                    if (cycle > 0.35) continue;
                    double k = cycle / 0.35;
                    double x1 = w * (0.15 + m * 0.32 + k * 0.34);
                    double y1 = h * (0.06 + m * 0.14 + k * 0.24);
                    double x0 = x1 - 42;
                    double y0 = y1 - 28;
                    g.setComposite(AlphaComposite.SrcOver.derive((float) (Math.sin(k * Math.PI) * 0.80)));
                    Color mc = m % 2 == 0 ? new Color(0x38BDF8) : new Color(0xE879F9);
                    g.setColor(mc);
                    g.drawLine((int) x0, (int) y0, (int) x1, (int) y1);
                    SpriteAssets.drawVoxelCube(g, (int) x1, (int) y1, 5, Color.WHITE, mc);
                }
            }
            case ABYSSAL_ATLANTIS -> {
                // Rising glowing abyssal square pixel bubbles & pearlescent cubes
                for (int b = 0; b < 22; b++) {
                    double phase = (t * (0.075 + (b % 3) * 0.02) + b * 0.045) % 1.0;
                    double bx = ((b * 67.3 + Math.sin(t * 1.6 + b) * 14.0) % Math.max(1, w - 30)) + 15;
                    double by = h * (1.0 - phase);
                    int r = 4 + (b % 4) * 2;
                    int ix = (int) bx - r / 2;
                    int iy = (int) by - r / 2;
                    g.setComposite(AlphaComposite.SrcOver.derive((float) (Math.sin(phase * Math.PI) * 0.56)));
                    g.setColor(b % 2 == 0 ? new Color(0x5EEAD4) : new Color(0xA5F3FC));
                    g.drawRect(ix, iy, r, r);
                    g.setColor(Color.WHITE);
                    g.fillRect(ix + 1, iy + 1, 2, 2);
                }
            }
            case NAVAL_CITADEL -> { }
        }
    }

    private static void paintAuroraRibbons(Graphics2D g, int w, int h, double t, float strength) {
        if (strength <= 0.02f) return;
        for (int b = 0; b < 2; b++) {
            Path2D ribbon = new Path2D.Double();
            double baseY = h * (0.10 + b * 0.07);
            ribbon.moveTo(-20, baseY);
            for (int x = 0; x <= w + 40; x += 48) {
                double wy = baseY + Math.sin(x * 0.008 + t * (0.45 + b * 0.15) + b * 1.8) * 24.0;
                ribbon.lineTo(x, wy);
            }
            g.setComposite(AlphaComposite.SrcOver.derive(0.14f * strength));
            g.setColor(b == 0 ? new Color(0x34D399) : new Color(0x38BDF8));
            g.setStroke(new BasicStroke(24f - b * 6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(ribbon);
        }
    }

    private static void paintCelestialCrown(Graphics2D g, int w, int h, double t, boolean dualBoardLayout, Atmosphere atmo, MapTheme theme) {
        double orbitX = Math.sin(t * 0.12) * (dualBoardLayout ? w * 0.022 : w * 0.026);
        double cx = (dualBoardLayout ? w * 0.500 : w * 0.725) + orbitX;
        double cy = h * (dualBoardLayout ? 0.085 : 0.056) + Math.cos(t * 0.12) * 3.5;
        int minDim = Math.min(w, h);
        int celestialSize = dualBoardLayout
                ? Math.max(58, Math.min(82, (int) (minDim * 0.11)))
                : Math.max(48, Math.min(64, (int) (minDim * 0.082)));

        double dimFactor = 1.0 - atmo.storm * 0.52;
        Paint prev = g.getPaint();

        // Thematic celestial special: Galaxy Quasar / Apocalypse Blood Eclipse / Normal Sun+Moon
        if (theme == MapTheme.GALAXY_COSMOS) {
            int haloR = (int) (celestialSize * 1.50);
            BufferedImage halo = getHaloStamp(new Color(0xC084FC), haloR, 168);
            if (halo != null) {
                g.setComposite(AlphaComposite.SrcOver);
                g.drawImage(halo, (int) (cx - haloR), (int) (cy - haloR), null);
            }
            BufferedImage bhImg = SpriteAssets.getMapProp("galaxy_blackhole");
            if (bhImg != null) {
                drawRotatedCentered(g, bhImg, cx, cy, (int) (celestialSize * 1.15), (int) (celestialSize * 1.15), t * 0.25, 0.94f);
            }
            g.setPaint(prev);
            return;
        }

        if (theme == MapTheme.APOCALYPSE) {
            int haloR = (int) (celestialSize * 1.40);
            BufferedImage halo = getHaloStamp(new Color(0xEF4444), haloR, 160);
            if (halo != null) {
                g.setComposite(AlphaComposite.SrcOver);
                g.drawImage(halo, (int) (cx - haloR), (int) (cy - haloR), null);
            }
            BufferedImage moonImg = SpriteAssets.getMapProp("moon");
            if (moonImg != null) {
                drawRotatedCentered(g, moonImg, cx, cy, celestialSize, celestialSize, Math.sin(t * 0.35) * 0.08, 0.94f);
            }
            // Eclipse dark silhouette core over moon
            g.setComposite(AlphaComposite.SrcOver.derive(0.68f));
            g.setColor(new Color(0x160707));
            int ecR = (int) (celestialSize * 0.31);
            g.fillOval((int) (cx - ecR), (int) (cy - ecR), ecR * 2, ecR * 2);
            g.setPaint(prev);
            return;
        }

        // Sun (Spring / Summer / Sunset Autumn)
        if (atmo.sunWeight > 0.04) {
            float sAlpha = (float) (atmo.sunWeight * dimFactor);
            int haloR = (int) (celestialSize * 1.30);
            Color glowCol = theme == MapTheme.FANTASY_REALM ? new Color(0xF472B6)
                    : (theme == MapTheme.ABYSSAL_ATLANTIS ? new Color(0x2DD4BF)
                    : (atmo.autumnWeight > 0.3 ? new Color(0xF97316) : new Color(0xFBBF24)));
            BufferedImage halo = getHaloStamp(glowCol, haloR, (int) (115 * sAlpha));
            if (halo != null) {
                g.setComposite(AlphaComposite.SrcOver);
                g.drawImage(halo, (int) (cx - haloR), (int) (cy - haloR), null);
            }

            if (dualBoardLayout) {
                g.setComposite(AlphaComposite.SrcOver.derive(0.18f * sAlpha));
                g.setColor(theme == MapTheme.ABYSSAL_ATLANTIS ? new Color(0x5EEAD4) : new Color(0xFDE68A));
                for (int r = 0; r < 6; r++) {
                    double ry = cy + celestialSize * 0.8 + r * 36.0;
                    double rw = 34.0 - r * 3.5 + Math.sin(t * 2.2 + r) * 7.0;
                    g.fillRoundRect((int) (cx - rw / 2), (int) ry, (int) rw, 4, 4, 4);
                }
            }

            BufferedImage sunImg = SpriteAssets.getMapProp("sun");
            if (sunImg != null) {
                drawRotatedCentered(g, sunImg, cx, cy, celestialSize, celestialSize, t * 0.14, sAlpha * 0.96f);
            }
        }

        // Moon (Autumn / Winter)
        if (atmo.moonWeight > 0.04) {
            float mAlpha = (float) (atmo.moonWeight * dimFactor);
            double mx = cx + (atmo.sunWeight > 0.1 ? celestialSize * 0.55 : 0.0);
            int haloR = (int) (celestialSize * 1.15);
            BufferedImage halo = getHaloStamp(new Color(0x93C5FD), haloR, (int) (96 * mAlpha));
            if (halo != null) {
                g.setComposite(AlphaComposite.SrcOver);
                g.drawImage(halo, (int) (mx - haloR), (int) (cy - haloR), null);
            }

            if (dualBoardLayout) {
                g.setComposite(AlphaComposite.SrcOver.derive(0.16f * mAlpha));
                g.setColor(new Color(0xE0F2FE));
                for (int r = 0; r < 6; r++) {
                    double ry = cy + celestialSize * 0.8 + r * 36.0;
                    double rw = 26.0 - r * 2.5 + Math.sin(t * 1.9 + r) * 6.0;
                    g.fillRoundRect((int) (mx - rw / 2), (int) ry, (int) rw, 3, 3, 3);
                }
            }

            BufferedImage moonImg = SpriteAssets.getMapProp("moon");
            if (moonImg != null) {
                drawRotatedCentered(g, moonImg, mx, cy, celestialSize, celestialSize, Math.sin(t * 0.35) * 0.08, mAlpha * 0.96f);
            }
        }
        g.setPaint(prev);
    }

    private static void paintRollingWavesAndCrests(Graphics2D g, int w, int h, double t, Atmosphere atmo, MapTheme theme) {
        double surge = sampleSurgeStrength();
        double speedMul = 1.0 + atmo.storm * 1.15 + surge * (0.85 + Math.abs(surgeWindX) * 0.25);
        double ampMul = 1.0 + atmo.storm * 0.85 + surge * 0.70;
        double windDir = (surge > 0.04 && surgeWindX < 0) ? -1.0 : 1.0;

        // Animated swells / cosmic orbit rings / lava heat waves (lightweight 3-wave pass)
        g.setStroke(atmo.storm > 0.5 || surge > 0.3 ? STROKE_1_8_ROUND : (atmo.storm > 0.15 ? STROKE_1_5_ROUND : STROKE_1_25));
        Path2D.Float wave = new Path2D.Float(Path2D.WIND_NON_ZERO, 16);
        int waveCount = surge > 0.25 ? 4 : 3;
        for (int i = 0; i < waveCount; i++) {
            double phase = (t * 0.13 * speedMul + i * (1.0 / waveCount)) % 1.0;
            int y = (int) (phase * (h + 120)) - 60;
            float waveAlpha = (float) (Math.sin(phase * Math.PI) * (0.12 + atmo.storm * 0.10 + surge * 0.14));
            if (waveAlpha <= 0.005f) continue;
            g.setComposite(AlphaComposite.SrcOver.derive(waveAlpha));
            g.setColor(surge > 0.35 && i % 2 == 1 ? surgeTint : (i % 2 == 0 ? atmo.leyPrimary : atmo.leySecondary));
            wave.reset();
            wave.moveTo(-20, y);
            for (int x = 0; x <= w + 60; x += 100) {
                double wy = y + Math.sin(x * 0.013 - windDir * t * (0.9 * speedMul) + i) * 13.0 * ampMul;
                wave.lineTo(x, wy);
            }
            g.draw(wave);
        }

        // For ocean/water maps, sculpt foaming 3D wave crests across strait (skip surface foam in deep space / lava sea)
        if (theme == MapTheme.GALAXY_COSMOS) return;

        BufferedImage crestImg = SpriteAssets.getMapProp("wave_crest");
        if (crestImg != null) {
            int crestCount = atmo.storm > 0.3 || surge > 0.2 ? (surge > 0.35 ? 6 : 4) : 2;
            for (int i = 0; i < crestCount; i++) {
                double travel = (t * (0.055 + (i % 3) * 0.015) * speedMul + i * 0.21) % 1.0;
                double tDir = windDir >= 0 ? travel : (1.0 - travel);
                double cx = (i % 3 == 0 && surge < 0.25)
                        ? w * (0.485 + 0.025 * Math.sin(t * 0.7 + i))
                        : tDir * (w + 180) - 90;
                double cy = (i % 3 == 0 && surge < 0.25)
                        ? h * ((0.18 + ((i * 23 + t * 8) % 68) / 100.0))
                        : h * (0.14 + ((i * 37) % 76) / 100.0) + Math.sin(t * 1.9 + i) * (5.0 + atmo.storm * 7.0 + surge * 8.0);
                int cw = Math.max(48, ((int) Math.round(78 + (i % 3) * 18 + atmo.storm * 24 + surge * 26) + 4) / 8 * 8);
                int ch = cw / 2;
                float alpha = (float) (((i % 3 == 0 && surge < 0.25) ? 0.60 : Math.sin(travel * Math.PI))
                        * (0.24 + atmo.storm * 0.34 + surge * 0.32));
                if (theme == MapTheme.APOCALYPSE) alpha *= 0.45f; // subtle heat waves
                if (alpha > 0.02f) {
                    g.setComposite(AlphaComposite.SrcOver.derive(Math.min(0.88f, alpha)));
                    SpriteAssets.drawSharp(g, crestImg, (int) cx - cw / 2, (int) cy - ch / 2, cw, ch);
                }
            }
        }
    }

    private static void paintStormCloudsAndLightning(Graphics2D g, int w, int h, double t, boolean dualBoardLayout, Atmosphere atmo) {
        BufferedImage cloudImg = SpriteAssets.getMapProp("storm_cloud");
        double s = atmo.storm;

        // 3D Thunderhead Storm Clouds drifting along top horizons (framing titles cleanly)
        if (cloudImg != null) {
            double[][] anchors = dualBoardLayout
                    ? new double[][]{{0.14, 0.058}, {0.37, 0.052}, {0.63, 0.052}, {0.86, 0.058}}
                    : new double[][]{{0.18, 0.055}, {0.82, 0.055}};
            for (int i = 0; i < anchors.length; i++) {
                double drift = Math.sin(t * (0.22 + i * 0.04) + i * 1.7) * 26.0;
                double cx = w * anchors[i][0] + drift;
                double cy = h * anchors[i][1] + Math.cos(t * 0.3 + i) * 3.0;
                int cw = (int) (116 + (i % 2) * 20);
                g.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(0.86, s * 0.80)));
                SpriteAssets.drawSharp(g, cloudImg, (int) cx - cw / 2, (int) cy - cw / 2, cw, cw);
            }
        }
    }

    private static Atmosphere resolveAtmosphere(double t) {
        double phase = (t / SEASON_PERIOD_SEC) % 4.0;
        double springW, summerW, autumnW, winterW;
        Color skyEdge, oceanMid, oceanChannel, leyPrimary, leySecondary;
        double sunWeight, moonWeight;

        if (selectedSeason == Season.AUTO) {
            int idx = (int) Math.floor(phase) % 4;
            int next = (idx + 1) % 4;
            double frac = phase - Math.floor(phase);
            double blend = frac < 0.72 ? 0.0 : (0.5 - 0.5 * Math.cos(((frac - 0.72) / 0.28) * Math.PI));

            double[] weights = new double[4];
            weights[idx] = 1.0 - blend;
            weights[next] += blend;
            springW = weights[0];
            summerW = weights[1];
            autumnW = weights[2];
            winterW = weights[3];

            SeasonSpec a = seasonSpec(idx);
            SeasonSpec b = seasonSpec(next);
            skyEdge = lerpColor(a.skyEdge, b.skyEdge, blend);
            oceanMid = lerpColor(a.oceanMid, b.oceanMid, blend);
            oceanChannel = lerpColor(a.oceanChannel, b.oceanChannel, blend);
            leyPrimary = lerpColor(a.leyA, b.leyA, blend);
            leySecondary = lerpColor(a.leyB, b.leyB, blend);
            sunWeight = a.sun * (1.0 - blend) + b.sun * blend;
            moonWeight = a.moon * (1.0 - blend) + b.moon * blend;
        } else {
            int fixed = switch (selectedSeason) {
                case SPRING -> 0;
                case SUMMER -> 1;
                case AUTUMN -> 2;
                case WINTER -> 3;
                case AUTO -> 0;
            };
            springW = fixed == 0 ? 1.0 : 0.0;
            summerW = fixed == 1 ? 1.0 : 0.0;
            autumnW = fixed == 2 ? 1.0 : 0.0;
            winterW = fixed == 3 ? 1.0 : 0.0;
            SeasonSpec s = seasonSpec(fixed);
            skyEdge = s.skyEdge;
            oceanMid = s.oceanMid;
            oceanChannel = s.oceanChannel;
            leyPrimary = s.leyA;
            leySecondary = s.leyB;
            sunWeight = s.sun;
            moonWeight = s.moon;
        }

        double storm = switch (selectedWeather) {
            case CLEAR -> 0.0;
            case STORM -> 1.0;
            case AUTO -> {
                double wave = 0.5 + 0.5 * Math.sin(t * 0.21 - 0.8);
                double bias = autumnW * 0.20 + winterW * 0.16;
                double raw = Math.max(0.0, (wave + bias - 0.58) / 0.42);
                yield Math.min(1.0, raw);
            }
        };

        double lightningFlash = 0.0;
        if (storm > 0.28) {
            double slot = t % 3.4;
            if (slot < 0.26) {
                lightningFlash = Math.sin((slot / 0.26) * Math.PI) * storm;
            }
        }

        switch (selectedMapTheme) {
            case FANTASY_REALM -> {
                skyEdge = lerpColor(skyEdge, new Color(0x0D1438), 0.72);
                oceanMid = lerpColor(oceanMid, new Color(0x1D1E54), 0.72);
                oceanChannel = lerpColor(oceanChannel, new Color(0x2B286E), 0.72);
                leyPrimary = lerpColor(leyPrimary, new Color(0xF472B6), 0.78);
                leySecondary = lerpColor(leySecondary, new Color(0x22D3EE), 0.68);
            }
            case APOCALYPSE -> {
                skyEdge = lerpColor(skyEdge, new Color(0x0D0507), 0.85);
                oceanMid = lerpColor(oceanMid, new Color(0x220B0A), 0.85);
                oceanChannel = lerpColor(oceanChannel, new Color(0x3B140A), 0.85);
                leyPrimary = lerpColor(leyPrimary, new Color(0xF97316), 0.86);
                leySecondary = lerpColor(leySecondary, new Color(0xEF4444), 0.82);
            }
            case GALAXY_COSMOS -> {
                skyEdge = new Color(0x030410);
                oceanMid = new Color(0x0A0826);
                oceanChannel = new Color(0x181040);
                leyPrimary = new Color(0xC084FC);
                leySecondary = new Color(0x38BDF8);
            }
            case ABYSSAL_ATLANTIS -> {
                skyEdge = lerpColor(skyEdge, new Color(0x03131A), 0.80);
                oceanMid = lerpColor(oceanMid, new Color(0x062A36), 0.80);
                oceanChannel = lerpColor(oceanChannel, new Color(0x0D4552), 0.80);
                leyPrimary = new Color(0x2DD4BF);
                leySecondary = new Color(0xA855F7);
            }
            case NAVAL_CITADEL -> { }
        }

        if (ZombieMode.isZombieMode()) {
            skyEdge = lerpColor(skyEdge, new Color(0x061510), 0.55);
            oceanMid = lerpColor(oceanMid, new Color(0x0A241A), 0.50);
            oceanChannel = lerpColor(oceanChannel, new Color(0x133827), 0.45);
            leyPrimary = lerpColor(leyPrimary, new Color(0x22C55E), 0.72);
            leySecondary = lerpColor(leySecondary, new Color(0xF43F5E), 0.65);
        }

        return new Atmosphere(
                skyEdge, oceanMid, oceanChannel, leyPrimary, leySecondary,
                springW, summerW, autumnW, winterW,
                sunWeight, moonWeight, storm, lightningFlash
        );
    }

    private static SeasonSpec seasonSpec(int idx) {
        return switch (idx) {
            case 0 -> new SeasonSpec( // Spring: Emerald-Sapphire & Blossom Rose
                    new Color(0x06192C), new Color(0x0D2C46), new Color(0x164266),
                    new Color(0x38BDF8), new Color(0xF472B6), 0.90, 0.10);
            case 1 -> new SeasonSpec( // Summer: Bright Azure & Warm Solar Gold
                    new Color(0x071D35), new Color(0x103558), new Color(0x19507A),
                    new Color(0x38BDF8), new Color(0xFBBF24), 1.00, 0.00);
            case 2 -> new SeasonSpec( // Autumn: Dusk Plum-Navy & Sunset Ember
                    new Color(0x131126), new Color(0x1E2344), new Color(0x2B335C),
                    new Color(0xFB923C), new Color(0xF43F5E), 0.42, 0.62);
            default -> new SeasonSpec( // Winter: Polar Night & Glacial Aurora Teal
                    new Color(0x040914), new Color(0x0A1C32), new Color(0x112E4C),
                    new Color(0x22D3EE), new Color(0x34D399), 0.00, 1.00);
        };
    }

    private static Color lerpColor(Color a, Color b, double t) {
        double k = Math.max(0.0, Math.min(1.0, t));
        int r = (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * k);
        int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k);
        int bl = (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, bl)));
    }

    private record SeasonSpec(
            Color skyEdge, Color oceanMid, Color oceanChannel,
            Color leyA, Color leyB, double sun, double moon
    ) { }

    private record Atmosphere(
            Color skyEdge, Color oceanMid, Color oceanChannel,
            Color leyPrimary, Color leySecondary,
            double springWeight, double summerWeight, double autumnWeight, double winterWeight,
            double sunWeight, double moonWeight, double storm, double lightningFlash
    ) { }

    private static void paintLeylineCurveOnly(Graphics2D g, double x1, double y1, double x2, double y2, Color color) {
        double mx = (x1 + x2) * 0.5 + (y2 - y1) * 0.14;
        double my = (y1 + y2) * 0.5 - (x2 - x1) * 0.14;
        Path2D curve = new Path2D.Double();
        curve.moveTo(x1, y1);
        curve.quadTo(mx, my, x2, y2);
        g.setComposite(AlphaComposite.SrcOver.derive(0.16f));
        g.setColor(color);
        g.setStroke(STROKE_2_4_ROUND);
        g.draw(curve);
        g.setComposite(AlphaComposite.SrcOver.derive(0.28f));
        g.setStroke(STROKE_1_25);
        g.draw(curve);
    }

    private static void paintLeylinePulses(Graphics2D g, double x1, double y1, double x2, double y2,
                                           Color color, double t, double phaseOffset) {
        double mx = (x1 + x2) * 0.5 + (y2 - y1) * 0.14;
        double my = (y1 + y2) * 0.5 - (x2 - x1) * 0.14;
        double u = (t * 0.22 + phaseOffset) % 1.0;
        double iu = 1.0 - u;
        double px = iu * iu * x1 + 2 * iu * u * mx + u * u * x2;
        double py = iu * iu * y1 + 2 * iu * u * my + u * u * y2;
        g.setComposite(AlphaComposite.SrcOver.derive(0.55f));
        g.setColor(color);
        g.fillOval((int) px - 5, (int) py - 5, 10, 10);
        g.setComposite(AlphaComposite.SrcOver.derive(0.90f));
        g.setColor(Color.WHITE);
        g.fillOval((int) px - 2, (int) py - 2, 4, 4);
    }

    private static void paintArcaneCompassBase(Graphics2D g, double cx, double cy, double r, double t, Atmosphere atmo) {
        BufferedImage rune = SpriteAssets.getMagicRune(CombatLoadout.MagicOption.ARCANE_SONAR);
        if (rune != null) {
            int d = Math.max(32, ((int) (r * 2.15) + 4) / 8 * 8);
            drawRotatedCentered(g, rune, cx, cy, d, d, t * 0.16, 0.18f);
        }

        g.setComposite(AlphaComposite.SrcOver.derive(0.24f));
        g.setStroke(STROKE_1_25);
        g.setColor(atmo.leyPrimary);
        g.drawOval((int) (cx - r * 0.78), (int) (cy - r * 0.78), (int) (r * 1.56), (int) (r * 1.56));
        g.setColor(atmo.leySecondary);
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI * 0.25 + t * 0.05;
            double len = (i % 2 == 0) ? r * 0.88 : r * 0.56;
            g.drawLine((int) cx, (int) cy, (int) (cx + Math.cos(ang) * len), (int) (cy + Math.sin(ang) * len));
        }
    }

    private static void paintArcaneCompassSweep(Graphics2D g, double cx, double cy, double r, double t, Atmosphere atmo) {
        double sweepDeg = (t * 42.0) % 360.0;
        g.setComposite(AlphaComposite.SrcOver.derive(0.11f));
        g.setColor(atmo.leyPrimary);
        g.fill(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, -sweepDeg, 42.0, Arc2D.PIE));
    }

    private static void paintIslandWithAura(Graphics2D g, BufferedImage img, int cx, int cy, int size,
                                            Color auraColor, double t, boolean radarSweep, float alpha, double storm) {
        double pulse = 0.88 + 0.12 * Math.sin(t * (1.8 + storm * 1.4));
        int haloR = (int) Math.max(12.0, size * (0.64 + storm * 0.14) * pulse);
        BufferedImage halo = getHaloStamp(auraColor, haloR, (int) (52 + storm * 28));
        if (halo != null) {
            g.setComposite(AlphaComposite.SrcOver);
            g.drawImage(halo, cx - haloR, cy - haloR, null);
        }

        int ringR = (int) (size * (0.53 + storm * 0.06));
        g.setComposite(AlphaComposite.SrcOver.derive((float) (0.28 + storm * 0.14)));
        g.setColor(auraColor);
        SpriteAssets.drawVoxelRing(g, cx, cy, ringR, 2, false);

        if (radarSweep || storm > 0.35) {
            double deg = (t * (65.0 + storm * 45.0)) % 360.0;
            g.setComposite(AlphaComposite.SrcOver.derive((float) (0.16 + storm * 0.10)));
            g.setColor(radarSweep ? LEYLINE_CYAN : auraColor);
            g.fill(new Arc2D.Double(cx - ringR * 0.78, cy - ringR * 0.78, ringR * 1.56, ringR * 1.56, -deg, 48.0, Arc2D.PIE));
        }

        if (img != null) {
            g.setComposite(AlphaComposite.SrcOver.derive(alpha));
            SpriteAssets.drawSharp(g, img, cx - size / 2, cy - size / 2, size, size);
        }
    }

    private static void paintBeacon(Graphics2D g, BufferedImage img, int cx, int cy, int size,
                                    double t, double phase, Color color, double storm) {
        double wave = (t * (0.45 + storm * 0.35) + phase) % 1.0;
        int rippleR = (int) (size * (0.42 + wave * (0.55 + storm * 0.28)));
        g.setComposite(AlphaComposite.SrcOver.derive((float) ((1.0 - wave) * (0.46 + storm * 0.22))));
        g.setColor(color);
        SpriteAssets.drawVoxelRing(g, cx, cy, rippleR, 2, false);

        // Weather-reactive lighthouse / arcane searchlight sweep when storm intensifies
        if (storm > 0.20) {
            double beamDeg = (t * 58.0 + phase * 110.0) % 360.0;
            double beamR = size * (1.2 + storm * 0.9);
            g.setComposite(AlphaComposite.SrcOver.derive((float) (0.14 * storm)));
            g.setColor(color);
            g.fill(new Arc2D.Double(cx - beamR, cy - beamR, beamR * 2, beamR * 2, -beamDeg, 36.0, Arc2D.PIE));
        }

        if (img != null) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.86f));
            SpriteAssets.drawSharp(g, img, cx - size / 2, cy - size / 2, size, size);
        }
    }

    private record DragonSpec(
            SpriteAssets.DragonSpecies species,
            Color auraColor,
            Color sparkColor
    ) { }

    private record DragonPair(DragonSpec main, DragonSpec scout) { }

    private static DragonSpec dragonSpec(SpriteAssets.DragonSpecies species) {
        return switch (species) {
            case SAKURA -> new DragonSpec(species, new Color(0xF472B6), new Color(0xFCE7F3));
            case SOLAR -> new DragonSpec(species, new Color(0xF97316), new Color(0xFDE047));
            case STORM -> new DragonSpec(species, new Color(0x38BDF8), new Color(0xFACC15));
            case FROST -> new DragonSpec(species, new Color(0x22D3EE), new Color(0x6EE7B7));
            case ZOMBIE -> new DragonSpec(species, new Color(0x22C55E), new Color(0xF43F5E));
            case ARCANE -> new DragonSpec(species, new Color(0x22D3EE), new Color(0xA5F3FC));
        };
    }

    private static DragonPair resolveMapDragons(Atmosphere atmo) {
        boolean stormy = atmo.storm > 0.45;
        if (ZombieMode.isZombieMode()) {
            SpriteAssets.DragonSpecies scout = stormy ? SpriteAssets.DragonSpecies.STORM : SpriteAssets.DragonSpecies.ZOMBIE;
            return new DragonPair(dragonSpec(SpriteAssets.DragonSpecies.ZOMBIE), dragonSpec(scout));
        }

        return switch (selectedMapTheme) {
            case FANTASY_REALM -> new DragonPair(
                    dragonSpec(SpriteAssets.DragonSpecies.SAKURA),
                    dragonSpec(stormy ? SpriteAssets.DragonSpecies.STORM : SpriteAssets.DragonSpecies.ARCANE));
            case APOCALYPSE -> new DragonPair(
                    dragonSpec(SpriteAssets.DragonSpecies.SOLAR),
                    dragonSpec(stormy ? SpriteAssets.DragonSpecies.STORM : SpriteAssets.DragonSpecies.ZOMBIE));
            case GALAXY_COSMOS -> new DragonPair(
                    dragonSpec(SpriteAssets.DragonSpecies.ARCANE),
                    dragonSpec(stormy ? SpriteAssets.DragonSpecies.STORM : SpriteAssets.DragonSpecies.SOLAR));
            case ABYSSAL_ATLANTIS -> new DragonPair(
                    dragonSpec(SpriteAssets.DragonSpecies.FROST),
                    dragonSpec(stormy ? SpriteAssets.DragonSpecies.STORM : SpriteAssets.DragonSpecies.ARCANE));
            case NAVAL_CITADEL -> {
                double maxW = atmo.springWeight;
                int domSeason = 0;
                if (atmo.summerWeight > maxW) { maxW = atmo.summerWeight; domSeason = 1; }
                if (atmo.autumnWeight > maxW) { maxW = atmo.autumnWeight; domSeason = 2; }
                if (atmo.winterWeight > maxW) { domSeason = 3; }

                SpriteAssets.DragonSpecies mainSpecies = switch (domSeason) {
                    case 0 -> SpriteAssets.DragonSpecies.SAKURA;
                    case 1 -> SpriteAssets.DragonSpecies.SOLAR;
                    case 2 -> SpriteAssets.DragonSpecies.STORM;
                    default -> SpriteAssets.DragonSpecies.FROST;
                };
                SpriteAssets.DragonSpecies scoutSpecies;
                if (domSeason == 2) {
                    scoutSpecies = stormy ? SpriteAssets.DragonSpecies.ARCANE : SpriteAssets.DragonSpecies.SOLAR;
                } else {
                    scoutSpecies = stormy ? SpriteAssets.DragonSpecies.STORM : SpriteAssets.DragonSpecies.ARCANE;
                }
                yield new DragonPair(dragonSpec(mainSpecies), dragonSpec(scoutSpecies));
            }
        };
    }

    /** Human-readable summary of the dragon species patrolling the currently selected map. */
    public static String activeDragonSummary() {
        Atmosphere atmo = resolveAtmosphere(elapsedSeconds());
        DragonPair pair = resolveMapDragons(atmo);
        if (pair.main.species == pair.scout.species) {
            return pair.main.species.displayName;
        }
        return pair.main.species.displayName + " & " + pair.scout.species.displayName;
    }

    /** Paints patrolling Blender 3D Dragons tailored to the active map season, storm, and mode. */
    public static void paintPatrollingDragons(Graphics2D g, int w, int h, double t) {
        if (w <= 40 || h <= 40 || SpriteAssets.dragonFrameCount() <= 0) return;

        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        Atmosphere atmo = resolveAtmosphere(t);
        DragonPair pair = resolveMapDragons(atmo);

        int minDim = Math.min(w, h);
        int mainSize = Math.max(80, Math.min(126, (int) (minDim * 0.148)));
        int scoutSize = Math.max(58, Math.min(90, (int) (minDim * 0.106)));

        paintSingleDragon(g, w, h, t, pair.scout.species,
                0.24, 1.45, 0.48, 0.38, 0.34, 0.22, 0.35,
                scoutSize, 1.15f, 0.88f, pair.scout.auraColor, pair.scout.sparkColor);

        paintSingleDragon(g, w, h, t, pair.main.species,
                0.31, 0.15, 0.50, 0.40, 0.52, 0.24, 0.65,
                mainSize, 1.00f, 0.96f, pair.main.auraColor, pair.main.sparkColor);

        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private static void paintSingleDragon(
            Graphics2D g, int w, int h, double t,
            SpriteAssets.DragonSpecies species,
            double speed, double phase,
            double cxFrac, double rxFrac,
            double cyFrac, double ryFrac,
            double yFreqMul, int baseSize,
            float flapRateMul, float alpha,
            Color auraColor, Color sparkColor
    ) {
        double dt = 0.04;
        double angNow = t * speed + phase;
        double angPrev = (t - dt) * speed + phase;

        double x = w * (cxFrac + rxFrac * Math.sin(angNow));
        double y = h * (cyFrac + ryFrac * Math.sin(angNow * 2.0 + yFreqMul));
        double px = w * (cxFrac + rxFrac * Math.sin(angPrev));
        double py = h * (cyFrac + ryFrac * Math.sin(angPrev * 2.0 + yFreqMul));
        double heading = Math.atan2(y - py, x - px);

        double cycle = (t * 1.45 * flapRateMul + phase) % 3.0;
        int frameIdx;
        if (cycle > 2.25) {
            frameIdx = 0;
        } else {
            double beat = (cycle / 2.25) * 2.0;
            int step = ((int) (beat * 4.0)) % 4;
            frameIdx = switch (step) {
                case 0 -> 0;
                case 1 -> 1;
                case 2 -> 2;
                default -> 3;
            };
        }

        double altitudeScale = 1.0 + 0.055 * Math.sin(angNow * 2.0 + 0.5);
        // Quantize cached raster size so wobble never evicts SCALED_CACHE entries on every frame
        int cachedSize = Math.max(24, ((baseSize + 3) / 4) * 4);

        for (int m = 1; m <= 7; m++) {
            double mt = t - m * 0.09;
            double ma = mt * speed + phase;
            double mx = w * (cxFrac + rxFrac * Math.sin(ma)) + Math.sin(m * 2.1 + t * 5.0) * 6.0;
            double my = h * (cyFrac + ryFrac * Math.sin(ma * 2.0 + yFreqMul)) + Math.cos(m * 1.7 + t * 4.0) * 5.0;
            float mAlpha = Math.max(0.02f, (1.0f - m / 8.0f) * 0.48f * alpha);
            int mr = Math.max(2, (int) ((8 - m) * 0.95));
            g.setComposite(AlphaComposite.SrcOver.derive(mAlpha));
            g.setColor(m % 2 == 0 ? auraColor : sparkColor);
            if (species == SpriteAssets.DragonSpecies.SAKURA) {
                AffineTransform prevTx = g.getTransform();
                g.translate(mx, my);
                g.rotate(t * 2.0 + m);
                g.fillRect(-mr, -mr / 2, mr * 2, Math.max(2, mr));
                g.setTransform(prevTx);
            } else if (species == SpriteAssets.DragonSpecies.FROST) {
                int r = Math.max(2, mr);
                int ix = (int) mx, iy = (int) my;
                g.fillRect(ix - r, iy, r * 2 + 1, 2);
                g.fillRect(ix, iy - r, 2, r * 2 + 1);
            } else if (species == SpriteAssets.DragonSpecies.STORM && m <= 5) {
                g.setStroke(STROKE_1_25);
                g.drawLine((int) mx - mr, (int) my - mr / 2, (int) mx + mr, (int) my + mr / 2);
            } else {
                SpriteAssets.drawVoxelCube(g, (int) mx, (int) my, Math.max(3, mr),
                        m % 2 == 0 ? auraColor : sparkColor, Color.WHITE);
            }
        }

        BufferedImage shadow = SpriteAssets.getDragonShadow(species, frameIdx);
        if (shadow != null) {
            int shOffX = Math.max(18, cachedSize / 4);
            int shOffY = Math.max(24, cachedSize / 3);
            int shadowBase = Math.max(16, ((int) Math.round(cachedSize * 0.88) + 3) / 4 * 4);
            drawRotatedCentered(g, shadow, x + shOffX, y + shOffY,
                    shadowBase, shadowBase, heading, altitudeScale, 0.30f * alpha);
        }

        int haloR = Math.max(10, ((int) (cachedSize * 0.50f) + 3) / 4 * 4);
        BufferedImage halo = getHaloStamp(auraColor, haloR, (int) (60 * alpha));
        if (halo != null) {
            g.setComposite(AlphaComposite.SrcOver);
            g.drawImage(halo, (int) (x - haloR), (int) (y - haloR), null);
        }

        BufferedImage dragonImg = SpriteAssets.getDragonFrame(species, frameIdx);
        if (dragonImg != null) {
            drawRotatedCentered(g, dragonImg, x, y, cachedSize, cachedSize, heading, altitudeScale, alpha);
        }
    }

    private static void drawRotatedCentered(Graphics2D g, BufferedImage img, double cx, double cy,
                                            int baseW, int baseH, double angleRad, float alpha) {
        drawRotatedCentered(g, img, cx, cy, baseW, baseH, angleRad, 1.0, alpha);
    }

    private static void drawRotatedCentered(Graphics2D g, BufferedImage img, double cx, double cy,
                                            int baseW, int baseH, double angleRad, double scaleMul, float alpha) {
        if (img == null || baseW <= 0 || baseH <= 0 || alpha <= 0.01f) return;
        AffineTransform prevTx = g.getTransform();
        double devScale = Math.hypot(prevTx.getScaleX(), prevTx.getShearY());
        int devW = Math.max(1, (int) Math.round(baseW * Math.max(1.0, devScale)));
        int devH = Math.max(1, (int) Math.round(baseH * Math.max(1.0, devScale)));
        BufferedImage cached = SpriteAssets.getScaled(img, devW, devH);
        if (cached == null) return;
        g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
        g.translate(cx, cy);
        g.rotate(angleRad);
        if (Math.abs(scaleMul - 1.0) > 0.002) {
            g.scale(scaleMul, scaleMul);
        }
        g.drawImage(cached, -baseW / 2, -baseH / 2, baseW, baseH, null);
        g.setTransform(prevTx);
    }

    /**
     * Lightweight transparent, click-through overlay rendering atmospheric weather particles
     * and patrolling 3D dragons over any layered stage.
     */
    public static final class DragonAmbientOverlay extends JComponent {
        private final Timer timer;

        public DragonAmbientOverlay() {
            setOpaque(false);
            timer = new Timer(40, e -> {
                if (isShowing()) {
                    repaint();
                } else {
                    ((Timer) e.getSource()).stop();
                }
            });
            addHierarchyListener(e -> {
                if (isShowing()) {
                    if (!timer.isRunning()) timer.start();
                } else {
                    timer.stop();
                }
            });
        }

        @Override public boolean contains(int x, int y) { return false; }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            double t = elapsedSeconds();
            if (isShowing()) syncLiveEnvironmentAudio(t);
            paintAtmosphericWeather(g, getWidth(), getHeight(), t);
            paintPatrollingDragons(g, getWidth(), getHeight(), t);
            g.dispose();
        }
    }

    /**
     * Frosted-glass tactical control pill for switching 5 Map Themes, 4 Seasons & Weather states
     * without layout shift (fixed to the widest label in its mode).
     */
    public static final class AtmospherePill extends JButton {
        public enum Kind { MAP, SEASON, WEATHER }

        private final Kind kind;
        private Color accent = UiTokens.ACCENT_SIGNAL;

        public AtmospherePill(Kind kind) {
            this.kind = kind;
            setFont(UiTokens.FONT_META);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            FontMetrics fm = getFontMetrics(UiTokens.FONT_META);
            int maxW = 64;
            switch (kind) {
                case MAP -> { for (MapTheme t : MapTheme.values()) maxW = Math.max(maxW, fm.stringWidth(t.shortLabel)); }
                case SEASON -> { for (Season s : Season.values()) maxW = Math.max(maxW, fm.stringWidth(s.shortLabel)); }
                case WEATHER -> { for (WeatherMode m : WeatherMode.values()) maxW = Math.max(maxW, fm.stringWidth(m.shortLabel)); }
            }
            Dimension pref = new Dimension(maxW + 30, UiTokens.CHIP_HEIGHT);
            setPreferredSize(pref);
            setMinimumSize(pref);
            sync();
        }

        public void sync() {
            String dragons = activeDragonSummary();
            switch (kind) {
                case MAP -> {
                    MapTheme t = getMapTheme();
                    setText(t.shortLabel);
                    this.accent = t.accent;
                    setToolTipText("Đổi Bản Đồ 3D (5 Map) · Đang chọn: " + t.displayName + " · Rồng tuần tra: " + dragons);
                }
                case SEASON -> {
                    Season s = getSeason();
                    setText(s.shortLabel);
                    this.accent = s.accent;
                    setToolTipText("Đổi Mùa 4 Mùa · Đang chọn: " + s.displayName + " · Rồng tuần tra: " + dragons);
                }
                case WEATHER -> {
                    WeatherMode m = getWeatherMode();
                    setText(m.shortLabel);
                    this.accent = m.accent;
                    setToolTipText("Đổi Thời Tiết · Đang chọn: " + m.displayName + " · Rồng tuần tra: " + dragons);
                }
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int arc = UiTokens.BORDER_RADIUS;
            boolean pressed = getModel().isPressed();
            boolean hover = getModel().isRollover();

            Color topBg = pressed ? new Color(15, 23, 42, 240)
                    : (hover ? new Color(34, 49, 75, 224) : new Color(20, 32, 52, 206));
            Color botBg = pressed ? new Color(10, 17, 30, 244)
                    : (hover ? new Color(24, 37, 59, 230) : new Color(14, 23, 39, 214));
            g.setPaint(new GradientPaint(0, 1, topBg, 0, h - 1, botBg));
            g.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

            // Frosted rim border + subtle seasonal accent tint
            g.setColor(hover
                    ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 210)
                    : new Color(255, 255, 255, 48));
            g.setStroke(new BasicStroke(hover ? 1.3f : 1.0f));
            g.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

            if (hasFocus()) {
                g.setColor(UiTokens.FOCUS_RING);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(3, 3, w - 7, h - 7, arc, arc);
            }

            // Luminous seasonal status orb (clean geometric vector indicator)
            int dotX = 11;
            int dotY = h / 2;
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80));
            g.fillOval(dotX - 5, dotY - 5, 10, 10);
            g.setColor(accent);
            g.fillOval(dotX - 3, dotY - 3, 6, 6);

            g.setFont(getFont());
            g.setColor(UiTokens.TEXT_PRIMARY);
            FontMetrics fm = g.getFontMetrics();
            int textX = 20 + Math.max(0, (w - 24 - fm.stringWidth(getText())) / 2);
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(getText(), textX, textY);
            g.dispose();
        }
    }
}
