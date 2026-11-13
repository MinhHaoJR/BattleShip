package battleship.ui;

import battleship.model.Board;
import battleship.model.ShipType;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Transparent 60-FPS layered overlay rendering Weapon × Magic skill choreography:
 * ornate 3D launch seals, V-formation stealth squadrons, MIRV splitting warheads,
 * torpedo hydro-wakes, sky-to-sea lightning pillars, radial impact blooms,
 * floating combat callouts, and patrolling Blender 3D Arcane Dragons.
 */
public class CombatFxOverlay extends JComponent {
    private static final int ACTIVE_TICK_MS = 16;
    private static final int IDLE_TICK_MS = 40;

    private static final AlphaComposite[] ALPHA_CACHE = new AlphaComposite[101];
    private static final BasicStroke[] ROUND_STROKES = new BasicStroke[49];
    static {
        for (int i = 0; i <= 100; i++) {
            ALPHA_CACHE[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i / 100f);
        }
        for (int i = 0; i <= 48; i++) {
            ROUND_STROKES[i] = new BasicStroke(Math.max(0.5f, i * 0.5f), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
        }
    }
    private static final Map<Long, BufferedImage> GLOW_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<Long, BufferedImage>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, BufferedImage> eldest) {
                    return size() > 192;
                }
            });

    private enum ParticleKind { SPARK, SMOKE, FROST, ORB, BUBBLE }

    private final Object fxLock = new Object();
    private final List<ActiveStrike> strikes = new ArrayList<>();
    private final List<TrailParticle> particles = new ArrayList<>();
    private final List<ShockwaveRing> rings = new ArrayList<>();
    private final List<LightningBolt> bolts = new ArrayList<>();
    private final List<ImpactBloom> blooms = new ArrayList<>();
    private final List<ShieldFlare> shieldFlares = new ArrayList<>();
    private final List<ShieldShatterAnim> shieldShatters = new ArrayList<>();
    private final List<FloatingCallout> callouts = new ArrayList<>();
    private final List<FleetShieldAnim> fleetShields = new ArrayList<>();
    private final List<BossIntroAnim> bossIntros = new ArrayList<>();
    private final List<BossCityRampageAnim> bossRampages = new ArrayList<>();
    private final List<SuperShipCutsceneAnim> superCutscenes = new ArrayList<>();
    private final List<HyperionBeamAnim> hyperionBeams = new ArrayList<>();
    private final List<EnvironmentWaveAnim> envWaves = new ArrayList<>();
    private final Random rng = new Random();
    private final Timer timer;

    public CombatFxOverlay() {
        setOpaque(false);
        timer = new Timer(IDLE_TICK_MS, event -> stepFrame());
        addHierarchyListener(event -> {
            if (isShowing()) {
                if (!timer.isRunning()) timer.start();
            } else {
                timer.stop();
            }
        });
    }

    /** Never intercept mouse clicks/hovers intended for the boards underneath. */
    @Override public boolean contains(int x, int y) {
        return false;
    }

    /** Launches a single-cell strike animation. */
    public void launchStrike(
            BoardPanel fromBoard,
            BoardPanel toBoard,
            int targetRow,
            int targetCol,
            CombatLoadout.WeaponOption weapon,
            CombatLoadout.MagicOption magic,
            boolean lethalHit,
            Runnable onImpact
    ) {
        launchStrike(fromBoard, toBoard, targetRow, targetCol,
                List.of(new int[]{targetRow, targetCol}), weapon, magic, lethalHit, onImpact);
    }

    /**
     * Launches a multi-cell Weapon × Magic skill choreography from {@code fromBoard} to {@code toBoard}.
     * Synchronizes ship recoil, turret muzzle blasts, and projectile launch according to skill size.
     */
    public void launchStrike(
            BoardPanel fromBoard,
            BoardPanel toBoard,
            int targetRow,
            int targetCol,
            List<int[]> targetCells,
            CombatLoadout.WeaponOption weapon,
            CombatLoadout.MagicOption magic,
            boolean lethalHit,
            Runnable onImpact
    ) {
        if (!SwingUtilities.isEventDispatchThread()) {
            List<int[]> cellsSnapshot = targetCells == null ? null : List.copyOf(targetCells);
            SwingUtilities.invokeLater(() -> launchStrike(
                    fromBoard, toBoard, targetRow, targetCol, cellsSnapshot, weapon, magic, lethalHit, onImpact));
            return;
        }
        if (fromBoard == null || toBoard == null) {
            if (onImpact != null) onImpact.run();
            return;
        }
        CombatLoadout.WeaponOption w = weapon != null ? weapon : CombatLoadout.WeaponOption.CANNON;
        CombatLoadout.MagicOption m = magic != null ? magic : CombatLoadout.MagicOption.NONE;

        BoardPanel.ShipLaunchInfo launchInfo = fromBoard.getLauncherForSkill(w, m, this);
        Point origin = launchInfo.muzzlePoint();
        Point target = toBoard.getCellCenterIn(targetRow, targetCol, this);
        double aimAngle = Math.atan2(target.y - origin.y, target.x - origin.x);
        fromBoard.triggerShipFire(launchInfo, aimAngle, w, m);

        List<Point> cellTargets = new ArrayList<>();
        if (targetCells != null) {
            for (int[] cell : targetCells) {
                if (cell != null && cell.length >= 2) {
                    cellTargets.add(toBoard.getCellCenterIn(cell[0], cell[1], this));
                }
            }
        }
        if (cellTargets.isEmpty()) cellTargets.add(target);
        int cellSize = Math.max(24, toBoard.getCellSize());

        long durationMs = switch (w) {
            case CANNON -> 390L;
            case TORPEDO -> 470L;
            case MISSILE_CROSS -> 490L;
            case AIRSTRIKE -> 580L;
        };

        boolean shieldBlocked = toBoard.isCellShielded(targetRow, targetCol);
        synchronized (fxLock) {
            strikes.add(new ActiveStrike(origin, target, cellTargets, cellSize, w, m, null, lethalHit, shieldBlocked, durationMs, onImpact));

            double shipRingScale = switch (launchInfo.shipType()) {
                case CARRIER -> 1.85;
                case BATTLESHIP -> 1.62;
                case CRUISER, SUBMARINE -> 1.32;
                case DESTROYER -> 1.05;
            };
            rings.add(new ShockwaveRing(origin.x, origin.y, cellSize * shipRingScale, m.accentColor, 340L));
            blooms.add(new ImpactBloom(origin.x, origin.y, cellSize * (shipRingScale + 0.15), m.accentColor, 320L));
            spawnBurst(origin.x, origin.y, w == CombatLoadout.WeaponOption.CANNON ? 14 : 20, m, w, true);
            if (w == CombatLoadout.WeaponOption.CANNON) {
                spawnCannonGunsmoke(origin.x, origin.y, aimAngle, cellSize);
            }

            String shipBadge = shipClassCallout(launchInfo.shipType());
            if (shipBadge != null) {
                callouts.add(new FloatingCallout(origin.x, origin.y + cellSize * 0.65, shipBadge, m.accentColor, 480L, 12.0));
            }
        }

        timer.setDelay(ACTIVE_TICK_MS);
        if (!timer.isRunning()) timer.start();
        repaint();
    }

    /** Launches a signature Blender 3D Zombie Boss Ship skill attack from {@code fromBoard} to {@code toBoard}. */
    public void launchZombieStrike(
            BoardPanel fromBoard,
            BoardPanel toBoard,
            ZombieMode.ZombieClass zombieClass,
            int targetRow,
            int targetCol,
            List<int[]> targetCells,
            boolean lethalHit,
            Runnable onImpact
    ) {
        if (!SwingUtilities.isEventDispatchThread()) {
            List<int[]> cellsSnapshot = targetCells == null ? null : List.copyOf(targetCells);
            SwingUtilities.invokeLater(() -> launchZombieStrike(
                    fromBoard, toBoard, zombieClass, targetRow, targetCol, cellsSnapshot, lethalHit, onImpact));
            return;
        }
        if (fromBoard == null || toBoard == null || zombieClass == null) {
            if (onImpact != null) onImpact.run();
            return;
        }
        CombatLoadout.WeaponOption w = switch (zombieClass) {
            case ABYSS_CARRIER -> CombatLoadout.WeaponOption.AIRSTRIKE;
            case BLOOD_BATTLESHIP -> CombatLoadout.WeaponOption.MISSILE_CROSS;
            case BLIGHT_SUBMARINE -> CombatLoadout.WeaponOption.TORPEDO;
            default -> CombatLoadout.WeaponOption.CANNON;
        };
        CombatLoadout.MagicOption m = switch (zombieClass) {
            case NECRO_CRUISER -> CombatLoadout.MagicOption.LIGHTNING;
            case PLAGUE_DESTROYER, BLIGHT_SUBMARINE -> CombatLoadout.MagicOption.ARCANE_SONAR;
            default -> CombatLoadout.MagicOption.FIRE;
        };

        BoardPanel.ShipLaunchInfo launchInfo = fromBoard.getLauncherForZombie(zombieClass, this);
        Point origin = launchInfo.muzzlePoint();
        Point target = toBoard.getCellCenterIn(targetRow, targetCol, this);
        double aimAngle = Math.atan2(target.y - origin.y, target.x - origin.x);
        fromBoard.triggerShipFire(launchInfo, aimAngle, w, m, zombieClass.accentColor);

        List<Point> cellTargets = new ArrayList<>();
        if (targetCells != null) {
            for (int[] cell : targetCells) {
                if (cell != null && cell.length >= 2) {
                    cellTargets.add(toBoard.getCellCenterIn(cell[0], cell[1], this));
                }
            }
        }
        if (cellTargets.isEmpty()) cellTargets.add(target);
        int cellSize = Math.max(24, toBoard.getCellSize());

        long durationMs = switch (zombieClass) {
            case PLAGUE_DESTROYER -> 430L;
            case BLIGHT_SUBMARINE -> 490L;
            case NECRO_CRUISER -> 470L;
            case BLOOD_BATTLESHIP -> 520L;
            case ABYSS_CARRIER -> 600L;
        };

        boolean shieldBlocked = toBoard.isCellShielded(targetRow, targetCol);
        synchronized (fxLock) {
            strikes.add(new ActiveStrike(origin, target, cellTargets, cellSize, w, m, zombieClass, lethalHit, shieldBlocked, durationMs, onImpact));

            double shipRingScale = 1.15 + zombieClass.shipType.length * 0.16;
            rings.add(new ShockwaveRing(origin.x, origin.y, cellSize * shipRingScale, zombieClass.accentColor, 360L));
            blooms.add(new ImpactBloom(origin.x, origin.y, cellSize * (shipRingScale + 0.2), zombieClass.accentColor, 340L));
            spawnBurstColor(origin.x, origin.y, 20, zombieClass.accentColor, true);
            callouts.add(new FloatingCallout(origin.x, origin.y + cellSize * 0.65,
                    zombieClass.title + ": " + zombieClass.skillName, zombieClass.accentColor, 620L, 12.0));
        }

        timer.setDelay(ACTIVE_TICK_MS);
        if (!timer.isRunning()) timer.start();
        repaint();
    }

    /**
     * Launches a movie-accurate 3D Voxel Titan Boss signature attack from {@code fromTitanBoard}
     * to {@code toPlayerBoard}, including Cherenkov Atomic Plasma beams, 3-head Golden Gravity
     * lightning streams, Burning Thermonuclear Pulses, and Omega Gravity Vortices.
     */
    public void launchTitanSkill(
            BoardPanel fromTitanBoard,
            BoardPanel toPlayerBoard,
            TitanMode.TitanSkill skill,
            boolean enraged,
            int targetRow,
            int targetCol,
            List<int[]> targetCells,
            boolean lethalHit,
            Runnable onImpact
    ) {
        if (!SwingUtilities.isEventDispatchThread()) {
            List<int[]> cellsSnapshot = targetCells == null ? null : List.copyOf(targetCells);
            SwingUtilities.invokeLater(() -> launchTitanSkill(
                    fromTitanBoard, toPlayerBoard, skill, enraged, targetRow, targetCol, cellsSnapshot, lethalHit, onImpact));
            return;
        }
        if (fromTitanBoard == null || toPlayerBoard == null || skill == null) {
            if (onImpact != null) onImpact.run();
            return;
        }

        CombatLoadout.WeaponOption w = switch (skill) {
            case GZ_TAIL_SLAM, MT_SILK_WEB -> CombatLoadout.WeaponOption.CANNON;
            case GZ_ATOMIC_BREATH, GZ_SPIRAL_RAY, MT_WING_GALE -> CombatLoadout.WeaponOption.TORPEDO;
            case GZ_THERMO_PULSE, GZ_MELTDOWN_RIFT, KG_OMEGA_VORTEX, KG_CATACLYSM_GRAVITY, MT_PRISM_RAIN, MT_SOLAR_SUPERNOVA -> CombatLoadout.WeaponOption.AIRSTRIKE;
            case KG_TRIPLE_GRAVITY, KG_HURRICANE_WING, KG_ALPHA_STORM, MT_GOD_RAY -> CombatLoadout.WeaponOption.MISSILE_CROSS;
        };
        CombatLoadout.MagicOption m = switch (skill) {
            case GZ_TAIL_SLAM, GZ_ATOMIC_BREATH, MT_WING_GALE, MT_GOD_RAY -> CombatLoadout.MagicOption.ARCANE_SONAR;
            case MT_SILK_WEB -> CombatLoadout.MagicOption.FROST;
            case GZ_THERMO_PULSE, GZ_SPIRAL_RAY, GZ_MELTDOWN_RIFT, MT_SOLAR_SUPERNOVA -> CombatLoadout.MagicOption.FIRE;
            case KG_TRIPLE_GRAVITY, KG_HURRICANE_WING, KG_OMEGA_VORTEX, KG_ALPHA_STORM, KG_CATACLYSM_GRAVITY, MT_PRISM_RAIN -> CombatLoadout.MagicOption.LIGHTNING;
        };

        int primaryPart = switch (skill) {
            case GZ_TAIL_SLAM -> 2;
            case GZ_ATOMIC_BREATH, GZ_SPIRAL_RAY, MT_GOD_RAY -> 0;
            case GZ_THERMO_PULSE, GZ_MELTDOWN_RIFT, MT_WING_GALE, MT_PRISM_RAIN -> 1;
            case KG_TRIPLE_GRAVITY, KG_ALPHA_STORM -> 2;
            case MT_SILK_WEB, MT_SOLAR_SUPERNOVA -> 3;
            case KG_HURRICANE_WING -> 4;
            case KG_OMEGA_VORTEX, KG_CATACLYSM_GRAVITY -> 0;
        };
        Point origin = fromTitanBoard.getTitanLaunchPointIn(skill.boss, primaryPart, this);
        List<Point> extraOrigins = new ArrayList<>();
        if (skill == TitanMode.TitanSkill.KG_TRIPLE_GRAVITY
                || skill == TitanMode.TitanSkill.KG_OMEGA_VORTEX
                || skill == TitanMode.TitanSkill.KG_ALPHA_STORM
                || skill == TitanMode.TitanSkill.KG_CATACLYSM_GRAVITY) {
            extraOrigins.add(fromTitanBoard.getTitanLaunchPointIn(TitanMode.TitanBoss.GHIDORAH, 1, this));
            extraOrigins.add(fromTitanBoard.getTitanLaunchPointIn(TitanMode.TitanBoss.GHIDORAH, 2, this));
            extraOrigins.add(fromTitanBoard.getTitanLaunchPointIn(TitanMode.TitanBoss.GHIDORAH, 3, this));
        } else if (skill == TitanMode.TitanSkill.GZ_ATOMIC_BREATH || skill == TitanMode.TitanSkill.GZ_SPIRAL_RAY) {
            // Sequential bottom-to-top dorsal fin ignition nodes!
            extraOrigins.addAll(fromTitanBoard.getGodzillaDorsalPointsIn(this));
        } else if (skill.boss == TitanMode.TitanBoss.MOTHRA) {
            extraOrigins.add(fromTitanBoard.getTitanLaunchPointIn(TitanMode.TitanBoss.MOTHRA, 1, this));
            extraOrigins.add(fromTitanBoard.getTitanLaunchPointIn(TitanMode.TitanBoss.MOTHRA, 2, this));
            extraOrigins.add(fromTitanBoard.getTitanLaunchPointIn(TitanMode.TitanBoss.MOTHRA, 3, this));
        }
        Point target = toPlayerBoard.getCellCenterIn(targetRow, targetCol, this);

        List<Point> cellTargets = new ArrayList<>();
        if (targetCells != null) {
            for (int[] cell : targetCells) {
                if (cell != null && cell.length >= 2) {
                    cellTargets.add(toPlayerBoard.getCellCenterIn(cell[0], cell[1], this));
                }
            }
        }
        if (cellTargets.isEmpty()) cellTargets.add(target);
        int cellSize = Math.max(24, toPlayerBoard.getCellSize());

        long chargeMs = switch (skill) {
            case GZ_ATOMIC_BREATH, GZ_SPIRAL_RAY -> 1580L;
            case GZ_THERMO_PULSE, GZ_MELTDOWN_RIFT, MT_SOLAR_SUPERNOVA -> 1120L;
            case KG_OMEGA_VORTEX, KG_ALPHA_STORM, KG_CATACLYSM_GRAVITY, MT_GOD_RAY, MT_PRISM_RAIN -> 980L;
            default -> 680L;
        };
        fromTitanBoard.triggerTitanCharge(chargeMs);

        long durationMs = switch (skill) {
            case GZ_TAIL_SLAM -> 940L;
            case GZ_ATOMIC_BREATH -> 1850L;
            case GZ_SPIRAL_RAY -> 1920L;
            case GZ_THERMO_PULSE -> 1480L;
            case GZ_MELTDOWN_RIFT -> 1620L;
            case KG_TRIPLE_GRAVITY -> 760L;
            case KG_HURRICANE_WING -> 700L;
            case KG_OMEGA_VORTEX -> 860L;
            case KG_ALPHA_STORM -> 1080L;
            case KG_CATACLYSM_GRAVITY -> 1220L;
            case MT_WING_GALE -> 820L;
            case MT_GOD_RAY -> 1280L;
            case MT_SILK_WEB -> 920L;
            case MT_PRISM_RAIN -> 1160L;
            case MT_SOLAR_SUPERNOVA -> 1560L;
        };

        Color fxAccent = (enraged && skill == TitanMode.TitanSkill.GZ_THERMO_PULSE)
                ? new Color(0xFB923C)
                : (enraged && skill.boss == TitanMode.TitanBoss.MOTHRA && skill == TitanMode.TitanSkill.MT_WING_GALE
                        ? new Color(0xFDE047)
                        : skill.accentColor);

        boolean shieldBlocked = toPlayerBoard.isCellShielded(targetRow, targetCol);
        synchronized (fxLock) {
            strikes.add(new ActiveStrike(
                    origin, target, cellTargets, cellSize, w, m, null, skill, enraged, extraOrigins,
                    lethalHit, shieldBlocked, durationMs, onImpact));

            double scale = skill.leavesImpactZone ? 2.85 : 2.25;
            if (skill == TitanMode.TitanSkill.GZ_ATOMIC_BREATH || skill == TitanMode.TitanSkill.GZ_SPIRAL_RAY) {
                if (!extraOrigins.isEmpty()) {
                    Point lowDorsal = extraOrigins.get(0);
                    Color pulseCol = skill == TitanMode.TitanSkill.GZ_SPIRAL_RAY
                            ? new Color(0xEF4444) : new Color(0x22D3EE);
                    rings.add(new ShockwaveRing(lowDorsal.x, lowDorsal.y, cellSize * 1.65, pulseCol, 360L));
                    blooms.add(new ImpactBloom(lowDorsal.x, lowDorsal.y, cellSize * 1.4, pulseCol, 380L));
                }
                spawnBurstColor(origin.x, origin.y, 20, fxAccent, true);
            } else {
                rings.add(new ShockwaveRing(origin.x, origin.y, cellSize * scale, fxAccent, 460L));
                rings.add(new ShockwaveRing(origin.x, origin.y, cellSize * (scale * 0.62), Color.WHITE, 320L));
                blooms.add(new ImpactBloom(origin.x, origin.y, cellSize * (scale + 0.35), fxAccent, 460L));
                spawnBurstColor(origin.x, origin.y, 28, fxAccent, true);
                if (skill.boss == TitanMode.TitanBoss.GHIDORAH || skill.boss == TitanMode.TitanBoss.MOTHRA) {
                    Color partCol = skill.boss == TitanMode.TitanBoss.MOTHRA ? new Color(0x6EE7B7) : new Color(0xFACC15);
                    for (Point hp : extraOrigins) {
                        blooms.add(new ImpactBloom(hp.x, hp.y, cellSize * 1.15, partCol, 340L));
                        spawnBurstColor(hp.x, hp.y, 10, partCol, true);
                    }
                }
            }
            callouts.add(new FloatingCallout(
                    origin.x, origin.y + cellSize * 0.85,
                    skill.boss.shortLabel + ": " + skill.skillName,
                    fxAccent, 920L, 14.0));
        }

        timer.setDelay(ACTIVE_TICK_MS);
        if (!timer.isRunning()) timer.start();
        repaint();
    }

    /** Launches the 4-Act cinema cutscene where humanity fabricates, arms & launches the Last-Hope Super Ship. */
    public void launchSuperShipCutscene(BoardPanel playerBoard, Runnable onCompleted) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> launchSuperShipCutscene(playerBoard, onCompleted));
            return;
        }
        Point dockDest;
        int cellSize = 36;
        if (playerBoard != null) {
            cellSize = Math.max(24, playerBoard.getCellSize());
            List<BoardPanel.AliveShipInfo> alive = playerBoard.getAliveShips(this);
            dockDest = !alive.isEmpty() ? alive.get(0).center() : playerBoard.getCellCenterIn(4, 4, this);
        } else {
            dockDest = new Point(Math.max(180, getWidth() / 4), Math.max(200, getHeight() / 2));
        }
        synchronized (fxLock) {
            superCutscenes.clear();
            superCutscenes.add(new SuperShipCutsceneAnim(dockDest, cellSize, 5300L, onCompleted));
        }
        timer.setDelay(ACTIVE_TICK_MS);
        if (!timer.isRunning()) timer.start();
        repaint();
    }

    public boolean isSuperShipCutsceneActive() {
        synchronized (fxLock) {
            return !superCutscenes.isEmpty();
        }
    }

    public void setSuperShipCutsceneElapsedMsForTest(long elapsedMs) {
        synchronized (fxLock) {
            long targetStart = System.currentTimeMillis() - Math.max(0L, elapsedMs);
            for (SuperShipCutsceneAnim sc : superCutscenes) {
                sc.startMs = targetStart;
            }
        }
        repaint();
    }

    /** Fires the awakened Super Ship's Hyperion Orbital Beam + Salvo across a 3x3 target zone. */
    public void launchHyperionCannon(
            BoardPanel playerBoard,
            BoardPanel targetBoard,
            int targetRow,
            int targetCol,
            List<int[]> impactCells,
            boolean lethalHit,
            Runnable onImpact
    ) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> launchHyperionCannon(
                    playerBoard, targetBoard, targetRow, targetCol, impactCells, lethalHit, onImpact));
            return;
        }
        Point origin = playerBoard != null
                ? playerBoard.getSuperShipLaunchInfo(this).muzzlePoint()
                : new Point(160, Math.max(180, getHeight() / 2));
        int cellSize = targetBoard != null ? Math.max(24, targetBoard.getCellSize()) : 34;
        Point target = targetBoard != null
                ? targetBoard.getCellCenterIn(targetRow, targetCol, this)
                : new Point(Math.max(400, (getWidth() * 3) / 4), Math.max(180, getHeight() / 2));
        List<Point> targets = new ArrayList<>();
        if (targetBoard != null && impactCells != null) {
            for (int[] c : impactCells) {
                if (c != null && c.length >= 2) {
                    targets.add(targetBoard.getCellCenterIn(c[0], c[1], this));
                }
            }
        }
        if (targets.isEmpty()) targets.add(target);

        synchronized (fxLock) {
            hyperionBeams.add(new HyperionBeamAnim(origin, target, targets, cellSize, lethalHit, 960L, onImpact));
            rings.add(new ShockwaveRing(origin.x, origin.y, cellSize * 2.4, new Color(0x38BDF8), 380L));
            rings.add(new ShockwaveRing(origin.x, origin.y, cellSize * 1.4, new Color(0xFACC15), 280L));
            blooms.add(new ImpactBloom(origin.x, origin.y, cellSize * 2.2, new Color(0x7DD3FC), 380L));
            spawnBurstColor(origin.x, origin.y, 24, new Color(0x38BDF8), true);
            callouts.add(new FloatingCallout(origin.x, Math.max(40, origin.y - cellSize),
                    "HYPERION RAILCANNON!", new Color(0xFDE047), 900L, -16.0));
        }
        timer.setDelay(ACTIVE_TICK_MS);
        if (!timer.isRunning()) timer.start();
        repaint();
    }

    /**
     * Triggers the dramatic Final-Boss encounter intro: ocean breach emergence on the Titan board,
     * scrolling threat hazard tapes, lightning arcs, and a center-screen Boss Cut-In portrait card.
     */
    /** Launches the Boy-POV City Rampage Cutscene leading into the Titan boss encounter. */
    public void launchTitanBossIntro(BoardPanel titanBoard, TitanMode.TitanBoss boss) {
        launchBossCityRampageCutscene(titanBoard, boss, null);
    }

    /** Plays the 4.8s 1st-person Boy POV monster city destruction cutscene before revealing the Titan battlefield. */
    public void launchBossCityRampageCutscene(
            BoardPanel titanBoard,
            TitanMode.TitanBoss boss,
            Runnable onCompleted
    ) {
        TitanMode.TitanBoss chosen = boss != null ? boss : TitanMode.TitanBoss.GODZILLA;
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> launchBossCityRampageCutscene(titanBoard, chosen, onCompleted));
            return;
        }
        Point center;
        int cellSize = 36;
        if (titanBoard != null) {
            titanBoard.triggerTitanEmergence(1800L);
            cellSize = Math.max(24, titanBoard.getCellSize());
            center = titanBoard.getTitanLaunchPointIn(chosen, 1, this);
        } else {
            center = new Point(Math.max(200, (getWidth() * 3) / 4), Math.max(200, getHeight() / 2));
        }
        synchronized (fxLock) {
            bossRampages.clear();
            bossIntros.clear();
            bossRampages.add(new BossCityRampageAnim(chosen, center, cellSize, 4800L, onCompleted));
            rings.add(new ShockwaveRing(center.x, center.y, cellSize * 4.4, chosen.accentColor, 620L));
        }
        timer.setDelay(ACTIVE_TICK_MS);
        if (!timer.isRunning()) timer.start();
        repaint();
    }

    public boolean isBossCityRampageActive() {
        synchronized (fxLock) {
            return !bossRampages.isEmpty();
        }
    }

    public void setBossRampageElapsedMsForTest(long elapsedMs) {
        synchronized (fxLock) {
            long targetStart = System.currentTimeMillis() - Math.max(0L, elapsedMs);
            for (BossCityRampageAnim anim : bossRampages) {
                anim.startMs = targetStart;
            }
        }
        repaint();
    }

    /** Triggers visible full-arena directional wind streaks & mist gusts when a Titan alters the ocean environment. */
    public void triggerEnvironmentWave(Color tint, double vx, double vy, long durationMs) {
        Runnable add = () -> {
            synchronized (fxLock) {
                envWaves.add(new EnvironmentWaveAnim(
                        tint != null ? tint : new Color(0x38BDF8), vx, vy, Math.max(900L, durationMs)));
                if (timer.getDelay() != ACTIVE_TICK_MS) timer.setDelay(ACTIVE_TICK_MS);
                if (!timer.isRunning()) timer.start();
            }
            repaint();
        };
        if (SwingUtilities.isEventDispatchThread()) add.run();
        else SwingUtilities.invokeLater(add);
    }

    /** Triggers the fiery Phase 2 "Cuồng Bạo" Enrage Transformation cut-in when a Titan drops below 50% HP. */
    public void launchTitanEnrageCutIn(BoardPanel titanBoard, TitanMode.TitanBoss boss) {
        launchBossCutInInternal(titanBoard, boss, true);
    }

    private void launchBossCutInInternal(BoardPanel titanBoard, TitanMode.TitanBoss boss, boolean enrageTransform) {
        TitanMode.TitanBoss chosen = boss != null ? boss : TitanMode.TitanBoss.GODZILLA;
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> launchBossCutInInternal(titanBoard, chosen, enrageTransform));
            return;
        }
        Point center;
        int cellSize = 36;
        if (titanBoard != null) {
            titanBoard.triggerTitanEmergence(enrageTransform ? 1400L : 1650L);
            cellSize = Math.max(24, titanBoard.getCellSize());
            center = titanBoard.getTitanLaunchPointIn(chosen, 1, this);
        } else {
            center = new Point(Math.max(200, (getWidth() * 3) / 4), Math.max(200, getHeight() / 2));
        }
        Color burstCol = enrageTransform ? chosen.enrageColor : chosen.accentColor;
        synchronized (fxLock) {
            bossIntros.clear();
            bossIntros.add(new BossIntroAnim(chosen, center, cellSize, enrageTransform ? 2250L : 2400L, enrageTransform));
            rings.add(new ShockwaveRing(center.x, center.y, cellSize * 4.8, burstCol, 680L));
            rings.add(new ShockwaveRing(center.x, center.y, cellSize * 3.3, Color.WHITE, 460L));
            blooms.add(new ImpactBloom(center.x, center.y, cellSize * 4.0, burstCol, 640L));
            spawnBurstColor(center.x, center.y, enrageTransform ? 44 : 34, burstCol, true);
        }
        timer.setDelay(ACTIVE_TICK_MS);
        if (!timer.isRunning()) timer.start();
        repaint();
    }

    /** Triggers a crystalline 3D Magic Shield deflection flare at the given point in overlay coordinates. */
    public void triggerShieldDeflect(Point targetPt) {
        triggerShieldDeflect(targetPt, -1, 100, 0, null);
    }

    /** Triggers a heavy Magic Shield impact flare showing absorbed energy damage & remaining dome capacity. */
    public void triggerShieldDeflect(
            Point targetPt,
            int remainingEnergy,
            int maxEnergy,
            int damage,
            Color impactAccent
    ) {
        if (targetPt == null) return;
        if (!SwingUtilities.isEventDispatchThread()) {
            Point ptCopy = new Point(targetPt);
            SwingUtilities.invokeLater(() -> triggerShieldDeflect(ptCopy, remainingEnergy, maxEnergy, damage, impactAccent));
            return;
        }
        double ratio = remainingEnergy >= 0 && maxEnergy > 0 ? remainingEnergy / (double) maxEnergy : 1.0;
        Color domeTint = ratio > 0.60 ? UiTokens.SHIELD_TINT
                : (ratio > 0.30 ? new Color(0xFBBF24) : new Color(0xF87171));
        Color burstCol = impactAccent != null ? impactAccent : domeTint;
        double rad = damage >= 40 ? 74.0 : (damage >= 25 ? 62.0 : 50.0);
        String msg = (remainingEnergy >= 0 && damage > 0)
                ? ("KHIÊN CHẮN ĐÒN! (-" + damage + "% · còn " + remainingEnergy + "%)")
                : "KHIÊN PHÉP CHẮN ĐÒN!";
        synchronized (fxLock) {
            shieldFlares.add(new ShieldFlare(targetPt.x, targetPt.y, rad, 580L, damage, domeTint));
            rings.add(new ShockwaveRing(targetPt.x, targetPt.y, rad * 1.45, burstCol, 480L));
            rings.add(new ShockwaveRing(targetPt.x, targetPt.y, rad * 1.05, Color.WHITE, 310L));
            blooms.add(new ImpactBloom(targetPt.x, targetPt.y, rad * 1.55, domeTint, 460L));
            spawnBurstColor(targetPt.x, targetPt.y, damage >= 35 ? 24 : 14, domeTint, true);
            callouts.add(new FloatingCallout(targetPt.x, targetPt.y - 20.0, msg, domeTint, 880L));
        }
        timer.setDelay(ACTIVE_TICK_MS);
        if (!timer.isRunning()) timer.start();
        repaint();
    }

    /** Explodes the player's depleted Magic Shield dome into 48 spinning voxel crystal shards across the left arena. */
    public void triggerShieldShatter(BoardPanel board) {
        if (board == null) return;
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> triggerShieldShatter(board));
            return;
        }
        Point topL = board.getCellCenterIn(0, 0, this);
        Point botR = board.getCellCenterIn(Board.SIZE - 1, Board.SIZE - 1, this);
        Point center = new Point((topL.x + botR.x) / 2, (topL.y + botR.y) / 2);
        int cellSize = Math.max(24, board.getCellSize());
        int gw = Math.max(120, Math.abs(botR.x - topL.x) + cellSize);
        int gh = Math.max(120, Math.abs(botR.y - topL.y) + cellSize);
        int gx = center.x - gw / 2;
        int gy = center.y - gh / 2;

        synchronized (fxLock) {
            shieldShatters.add(new ShieldShatterAnim(gx, gy, gw, gh, center, 1350L, rng));
            rings.add(new ShockwaveRing(center.x, center.y, gw * 0.62, new Color(0xF87171), 620L));
            rings.add(new ShockwaveRing(center.x, center.y, gw * 0.48, UiTokens.SHIELD_TINT, 520L));
            rings.add(new ShockwaveRing(center.x, center.y, gw * 0.36, Color.WHITE, 360L));
            blooms.add(new ImpactBloom(center.x, center.y, gw * 0.52, new Color(0xF87171), 540L));
            spawnBurstColor(center.x, center.y, 44, new Color(0x38BDF8), true);
            spawnBurstColor(center.x, center.y, 26, new Color(0xFBBF24), true);
            callouts.add(new FloatingCallout(center.x, gy + gh * 0.25,
                    "KHIÊN PHÉP VỠ VỤN!! (0%)", new Color(0xF87171), 1300L, -34.0));
        }
        timer.setDelay(ACTIVE_TICK_MS);
        if (!timer.isRunning()) timer.start();
        repaint();
    }

    /**
     * Orchestrates the 5-ship Arcane Magic Circle ritual on the allied board followed by a
     * left-to-right anime manga cut-in slide panel ("Khiên Phép!") and full-map shield erection.
     */
    public void launchFleetShieldActivation(
            BoardPanel alliedBoard,
            List<BoardPanel.ShipLaunchInfo> alliedShips,
            Runnable onBarrierErected
    ) {
        if (!SwingUtilities.isEventDispatchThread()) {
            List<BoardPanel.ShipLaunchInfo> snapshot = alliedShips == null ? List.of() : List.copyOf(alliedShips);
            SwingUtilities.invokeLater(() -> launchFleetShieldActivation(alliedBoard, snapshot, onBarrierErected));
            return;
        }
        if (alliedBoard == null) {
            if (onBarrierErected != null) onBarrierErected.run();
            return;
        }
        int cellSize = Math.max(24, alliedBoard.getCellSize());
        Point topLeft = alliedBoard.getCellCenterIn(0, 0, this);
        Point botRight = alliedBoard.getCellCenterIn(9, 9, this);
        Point boardCenter = new Point((topLeft.x + botRight.x) / 2, (topLeft.y + botRight.y) / 2);

        List<ShipSealNode> nodes = new ArrayList<>();
        if (alliedShips != null && !alliedShips.isEmpty()) {
            int idx = 0;
            for (BoardPanel.ShipLaunchInfo info : alliedShips) {
                if (info == null || info.muzzlePoint() == null) continue;
                int len = info.shipType() != null ? info.shipType().length : Math.max(2, info.length());
                nodes.add(new ShipSealNode(info.muzzlePoint(), info.shipType(), len, idx * 0.042));
                idx++;
            }
        }
        if (nodes.isEmpty()) {
            int[][] fallbacks = {{2, 2}, {2, 7}, {5, 5}, {7, 2}, {7, 7}};
            ShipType[] types = {ShipType.CARRIER, ShipType.BATTLESHIP, ShipType.CRUISER, ShipType.SUBMARINE, ShipType.DESTROYER};
            for (int i = 0; i < fallbacks.length; i++) {
                Point p = alliedBoard.getCellCenterIn(fallbacks[i][0], fallbacks[i][1], this);
                nodes.add(new ShipSealNode(p, types[i], types[i].length, i * 0.042));
            }
        }

        Point[] corners = {
                new Point(topLeft.x - cellSize / 2, topLeft.y - cellSize / 2),
                new Point(botRight.x + cellSize / 2, topLeft.y - cellSize / 2),
                new Point(botRight.x + cellSize / 2, botRight.y + cellSize / 2),
                new Point(topLeft.x - cellSize / 2, botRight.y + cellSize / 2)
        };

        synchronized (fxLock) {
            fleetShields.clear();
            FleetShieldAnim anim = new FleetShieldAnim(boardCenter, corners, cellSize, nodes, 2400L, onBarrierErected);
            fleetShields.add(anim);
            for (ShipSealNode n : nodes) {
                rings.add(new ShockwaveRing(n.pt.x, n.pt.y, cellSize * (1.2 + n.shipLength * 0.14), UiTokens.SHIELD_TINT, 480L));
                blooms.add(new ImpactBloom(n.pt.x, n.pt.y, cellSize * 1.45, new Color(0x67E8F9), 520L));
            }
        }

        timer.setDelay(ACTIVE_TICK_MS);
        if (!timer.isRunning()) timer.start();
        repaint();
    }

    private static String shipClassCallout(ShipType type) {
        if (type == null) return null;
        return switch (type) {
            case DESTROYER -> "Khu Trục 2★: Khai Pháo";
            case SUBMARINE -> "Tàu Ngầm 3★: Ngư Lôi";
            case CRUISER -> "Tuần Dương 3★: Pháo Phép";
            case BATTLESHIP -> "Thiết Giáp 4★: Loạt Tên Lửa";
            case CARRIER -> "Mẫu Hạm 5★: Xuất Kích";
        };
    }

    private void spawnCannonGunsmoke(double ox, double oy, double aimAngle, int cellSize) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < 12; i++) {
            double spread = (rng.nextDouble() - 0.5) * 1.25;
            boolean isSpark = (i % 3 == 0);
            double speed = (isSpark ? 2.4 : 1.1) + rng.nextDouble() * (isSpark ? 3.4 : 2.1);
            double vx = Math.cos(aimAngle + spread) * speed;
            double vy = Math.sin(aimAngle + spread) * speed - (isSpark ? 0.15 : 0.45);
            Color col = isSpark ? new Color(0xFDBA74) : new Color(0x94A3B8);
            double sz = isSpark ? (3.5 + rng.nextDouble() * 2.5) : (8.0 + rng.nextDouble() * (cellSize * 0.22));
            ParticleKind kind = isSpark ? ParticleKind.SPARK : ParticleKind.SMOKE;
            particles.add(new TrailParticle(ox, oy, vx, vy, sz, col, now, (isSpark ? 260L : 460L) + rng.nextInt(180),
                    CombatLoadout.MagicOption.NONE, kind));
        }
    }

    private void stepFrame() {
        List<Runnable> callbacks = null;
        boolean idle;
        synchronized (fxLock) {
            if (!isShowing() && isSceneIdle()) {
                timer.stop();
                return;
            }
            long now = System.currentTimeMillis();

            for (int i = strikes.size() - 1; i >= 0; i--) {
                ActiveStrike s = strikes.get(i);
                double t = (now - s.startMs) / (double) s.durationMs;

                // Mid-flight special events
                if (!s.splitTriggered && s.weapon == CombatLoadout.WeaponOption.MISSILE_CROSS && t >= 0.54) {
                    s.splitTriggered = true;
                    Point2 splitPt = computePosition(s, 0.54);
                    rings.add(new ShockwaveRing(splitPt.x, splitPt.y, s.cellSize * 1.15, Color.WHITE, 260L));
                    rings.add(new ShockwaveRing(splitPt.x, splitPt.y, s.cellSize * 0.72,
                            s.magic == CombatLoadout.MagicOption.NONE ? UiTokens.ACCENT_SIGNAL : s.magic.accentColor, 220L));
                    blooms.add(new ImpactBloom(splitPt.x, splitPt.y, s.cellSize * 1.05, Color.WHITE, 200L));
                    spawnBurst(splitPt.x, splitPt.y, 16, s.magic, s.weapon, true);
                }

                if (t >= 1.0) {
                    if (!s.impactFired) {
                        s.impactFired = true;
                        triggerImpactVisuals(s);
                        if (s.onImpact != null) {
                            if (callbacks == null) callbacks = new ArrayList<>(2);
                            callbacks.add(s.onImpact);
                        }
                    }
                    if (t >= 1.28) {
                        strikes.remove(i);
                    }
                    continue;
                }

                Point2 pos = computePosition(s, t);
                s.history.add(new TrailSample(pos.x, pos.y, t));
                if (s.history.size() > 16) s.history.remove(0);

                if (now - s.lastParticleMs >= 16L) {
                    s.lastParticleMs = now;
                    if (s.titanSkill == TitanMode.TitanSkill.GZ_ATOMIC_BREATH
                            || s.titanSkill == TitanMode.TitanSkill.GZ_SPIRAL_RAY) {
                        boolean spiral = s.titanSkill == TitanMode.TitanSkill.GZ_SPIRAL_RAY;
                        if (t < 0.38 && !s.extraOrigins.isEmpty()) {
                            int spineIdx = Math.min(s.extraOrigins.size() - 1, (int) ((t / 0.38) * s.extraOrigins.size()));
                            Point sp = s.extraOrigins.get(spineIdx);
                            spawnBurstColor(sp.x, sp.y, spiral ? 3 : 2,
                                    spiral ? new Color(0xEF4444) : new Color(0x67E8F9), true);
                        } else if (t >= 0.38 && t <= 1.02) {
                            Point2 tip = computeAtomicBeamTip(s, t);
                            Color beamParticleCol = spiral ? new Color(0xEF4444)
                                    : (s.titanEnraged ? new Color(0xFB923C) : new Color(0x22D3EE));
                            for (int k = 0; k < 3; k++) {
                                double ang = rng.nextDouble() * Math.PI * 2.0;
                                double spd = 2.0 + rng.nextDouble() * 4.2;
                                particles.add(new TrailParticle(tip.x, tip.y,
                                        Math.cos(ang) * spd, Math.sin(ang) * spd - 1.4,
                                        3.8 + rng.nextDouble() * 4.2,
                                        k == 0 ? (spiral ? new Color(0xFACC15) : Color.WHITE) : beamParticleCol,
                                        now, 260L + rng.nextInt(140), CombatLoadout.MagicOption.LIGHTNING,
                                        k == 2 ? ParticleKind.SMOKE : ParticleKind.SPARK));
                            }
                        }
                    } else if (s.titanSkill == TitanMode.TitanSkill.GZ_TAIL_SLAM) {
                        // Falling physical ocean water droplets trailing the breaching Kaiju tail whip
                        for (int k = 0; k < 3; k++) {
                            particles.add(new TrailParticle(
                                    pos.x + (rng.nextDouble() - 0.5) * s.cellSize * 1.1,
                                    pos.y + (rng.nextDouble() - 0.5) * s.cellSize * 0.6,
                                    (rng.nextDouble() - 0.5) * 2.6,
                                    1.2 + rng.nextDouble() * 3.0,
                                    4.0 + rng.nextDouble() * 4.0,
                                    k == 0 ? Color.WHITE : new Color(0x38BDF8),
                                    now, 320L + rng.nextInt(180), CombatLoadout.MagicOption.ARCANE_SONAR, ParticleKind.BUBBLE));
                        }
                    } else {
                        spawnFlightParticle(s, pos.x, pos.y, t);
                    }
                }
            }

            Iterator<TrailParticle> pit = particles.iterator();
            while (pit.hasNext()) {
                TrailParticle p = pit.next();
                if (now >= p.endMs) {
                    pit.remove();
                } else {
                    p.x += p.vx;
                    p.y += p.vy;
                    if (p.kind == ParticleKind.SMOKE) {
                        p.vy -= 0.04;
                        p.vx *= 0.92;
                        p.vy *= 0.92;
                    } else if (p.kind == ParticleKind.SPARK) {
                        p.vx *= 0.91;
                        p.vy *= 0.91;
                    } else {
                        if (p.magic == CombatLoadout.MagicOption.FIRE) p.vy -= 0.08;
                        p.vx *= 0.94;
                        p.vy *= 0.94;
                    }
                }
            }

            rings.removeIf(r -> now >= r.endMs);
            bolts.removeIf(b -> now >= b.endMs);
            blooms.removeIf(b -> now >= b.endMs);
            shieldFlares.removeIf(sf -> now >= sf.endMs);
            Iterator<ShieldShatterAnim> ssIt = shieldShatters.iterator();
            while (ssIt.hasNext()) {
                ShieldShatterAnim anim = ssIt.next();
                if (now >= anim.endMs) {
                    ssIt.remove();
                    continue;
                }
                for (CrystalShard sh : anim.shards) {
                    sh.x += sh.vx;
                    sh.y += sh.vy;
                    sh.vy += 0.16;
                    sh.rot += sh.vrot;
                }
            }
            callouts.removeIf(c -> now >= c.endMs);

            Iterator<FleetShieldAnim> fsIt = fleetShields.iterator();
            while (fsIt.hasNext()) {
                FleetShieldAnim anim = fsIt.next();
                double t = (now - anim.startMs) / (double) anim.durationMs;
                if (t < 0.48 && now - anim.lastParticleMs >= 32L) {
                    anim.lastParticleMs = now;
                    for (ShipSealNode n : anim.nodes) {
                        if (t < n.stagger) continue;
                        for (int k = 0; k < 2; k++) {
                            double ang = rng.nextDouble() * Math.PI * 2.0;
                            double rad = anim.cellSize * (0.45 + rng.nextDouble() * 0.45);
                            double px = n.pt.x + Math.cos(ang) * rad;
                            double py = n.pt.y + Math.sin(ang) * rad;
                            Color c = (k == 0) ? new Color(0x67E8F9) : new Color(0xFDE047);
                            particles.add(new TrailParticle(px, py,
                                    Math.cos(ang + 1.2) * 0.9, -1.1 - rng.nextDouble() * 1.1,
                                    3.6 + rng.nextDouble() * 2.6, c, now, 320L + rng.nextInt(140),
                                    CombatLoadout.MagicOption.FROST,
                                    (k == 0) ? ParticleKind.ORB : ParticleKind.FROST));
                        }
                    }
                }
                if (!anim.barrierTriggered && t >= 0.45) {
                    anim.barrierTriggered = true;
                    rings.add(new ShockwaveRing(anim.boardCenter.x, anim.boardCenter.y,
                            anim.cellSize * 5.6, UiTokens.SHIELD_TINT, 640L));
                    rings.add(new ShockwaveRing(anim.boardCenter.x, anim.boardCenter.y,
                            anim.cellSize * 4.2, Color.WHITE, 460L));
                    blooms.add(new ImpactBloom(anim.boardCenter.x, anim.boardCenter.y,
                            anim.cellSize * 4.6, new Color(0x38BDF8), 580L));
                    for (Point cp : anim.boardCorners) {
                        rings.add(new ShockwaveRing(cp.x, cp.y, anim.cellSize * 1.3, new Color(0xFDE047), 440L));
                        blooms.add(new ImpactBloom(cp.x, cp.y, anim.cellSize * 1.2, new Color(0x67E8F9), 420L));
                    }
                    for (ShipSealNode n : anim.nodes) {
                        rings.add(new ShockwaveRing(n.pt.x, n.pt.y, anim.cellSize * 1.45, Color.WHITE, 400L));
                    }
                    if (anim.onBarrierErected != null) {
                        if (callbacks == null) callbacks = new ArrayList<>(2);
                        callbacks.add(anim.onBarrierErected);
                    }
                }
                if (!anim.bannerCalloutTriggered && t >= 0.76) {
                    anim.bannerCalloutTriggered = true;
                    callouts.add(new FloatingCallout(
                            anim.boardCenter.x, anim.boardCorners[0].y + anim.cellSize * 1.25,
                            "TOÀN HẠM ĐỘI · KHIÊN TOÀN BẢN ĐỒ!", UiTokens.SHIELD_TINT, 1050L, -20.0));
                }
                if (t >= 1.0) {
                    if (!anim.barrierTriggered) {
                        anim.barrierTriggered = true;
                        if (anim.onBarrierErected != null) {
                            if (callbacks == null) callbacks = new ArrayList<>(2);
                            callbacks.add(anim.onBarrierErected);
                        }
                    }
                    fsIt.remove();
                }
            }

            Iterator<BossIntroAnim> biIt = bossIntros.iterator();
            while (biIt.hasNext()) {
                BossIntroAnim bi = biIt.next();
                double t = (now - bi.startMs) / (double) bi.durationMs;
                if (t < 0.85 && now - bi.lastSparkMs >= 55L) {
                    bi.lastSparkMs = now;
                    int bx = bi.bossCenter.x + rng.nextInt(Math.max(40, bi.cellSize * 5)) - bi.cellSize * 2;
                    int by = bi.bossCenter.y + rng.nextInt(Math.max(40, bi.cellSize * 4)) - bi.cellSize * 2;
                    bolts.add(new LightningBolt(
                            bx + (rng.nextDouble() - 0.5) * 80.0, Math.max(0, by - bi.cellSize * 3),
                            bx, by, bi.boss.accentColor, 2.8f, 240L, rng));
                    spawnBurstColor(bi.bossCenter.x, bi.bossCenter.y + bi.cellSize, 3, new Color(0xBAE6FD), true);
                }
                if (!bi.shatterTriggered && t >= 0.84) {
                    bi.shatterTriggered = true;
                    rings.add(new ShockwaveRing(bi.bossCenter.x, bi.bossCenter.y, bi.cellSize * 5.2, bi.boss.accentColor, 540L));
                    rings.add(new ShockwaveRing(bi.bossCenter.x, bi.bossCenter.y, bi.cellSize * 3.8, Color.WHITE, 380L));
                    spawnBurstColor(bi.bossCenter.x, bi.bossCenter.y, 28, bi.boss.accentColor, false);
                }
                if (t >= 1.0) {
                    biIt.remove();
                }
            }

            Iterator<BossCityRampageAnim> brIt = bossRampages.iterator();
            while (brIt.hasNext()) {
                BossCityRampageAnim br = brIt.next();
                double t = (now - br.startMs) / (double) br.durationMs;
                if (now - br.lastFxMs >= 75L && t < 0.86) {
                    br.lastFxMs = now;
                    int ow = Math.max(600, getWidth());
                    int oh = Math.max(480, getHeight());
                    int bandH = Math.min(oh - 76, Math.max(360, (int) (oh * 0.68)));
                    int bandY = (oh - bandH) / 2;
                    if (t >= 0.28 && t < 0.82) {
                        int sx = (int) (ow * (0.28 + rng.nextDouble() * 0.44));
                        int sy = bandY + (int) (bandH * (0.30 + rng.nextDouble() * 0.45));
                        Color sparkCol = switch (br.boss) {
                            case GODZILLA -> t < 0.55 ? new Color(0x38BDF8) : new Color(0xFB923C);
                            case MOTHRA -> t < 0.55 ? new Color(0x34D399) : new Color(0xFDE047);
                            case GHIDORAH -> new Color(0xFDE047);
                        };
                        spawnBurstColor(sx, sy, 5, sparkCol, true);
                    }
                }
                if (!br.beamBurstTriggered && t >= 0.55) {
                    br.beamBurstTriggered = true;
                    int ow = Math.max(600, getWidth());
                    int oh = Math.max(480, getHeight());
                    int cx = ow / 2;
                    int cy = oh / 2 - 20;
                    Color bCol = switch (br.boss) {
                        case GODZILLA -> new Color(0x22D3EE);
                        case MOTHRA -> new Color(0x34D399);
                        case GHIDORAH -> new Color(0xFACC15);
                    };
                    rings.add(new ShockwaveRing(cx, cy, br.cellSize * 5.2, bCol, 640L));
                    rings.add(new ShockwaveRing(cx, cy, br.cellSize * 3.4, Color.WHITE, 440L));
                    spawnBurstColor(cx, cy, 32, bCol, true);
                }
                if (!br.finaleSlamTriggered && t >= 0.83) {
                    br.finaleSlamTriggered = true;
                    rings.add(new ShockwaveRing(br.bossCenter.x, br.bossCenter.y, br.cellSize * 5.0, br.boss.accentColor, 560L));
                    blooms.add(new ImpactBloom(br.bossCenter.x, br.bossCenter.y, br.cellSize * 3.8, br.boss.accentColor, 520L));
                }
                if (!br.completedTriggered && t >= 0.96) {
                    br.completedTriggered = true;
                    if (br.onCompleted != null) {
                        if (callbacks == null) callbacks = new ArrayList<>(2);
                        callbacks.add(br.onCompleted);
                    }
                }
                if (t >= 1.0) {
                    if (!br.completedTriggered) {
                        br.completedTriggered = true;
                        if (br.onCompleted != null) {
                            if (callbacks == null) callbacks = new ArrayList<>(2);
                            callbacks.add(br.onCompleted);
                        }
                    }
                    brIt.remove();
                }
            }

            Iterator<HyperionBeamAnim> hbIt = hyperionBeams.iterator();
            while (hbIt.hasNext()) {
                HyperionBeamAnim hb = hbIt.next();
                double t = (now - hb.startMs) / (double) hb.durationMs;
                if (t < 0.72 && now - hb.lastSparkMs >= 38L) {
                    hb.lastSparkMs = now;
                    int tx = hb.target.x + rng.nextInt(Math.max(24, hb.cellSize * 2)) - hb.cellSize;
                    int ty = hb.target.y + rng.nextInt(Math.max(24, hb.cellSize * 2)) - hb.cellSize;
                    spawnBurstColor(tx, ty, 5, rng.nextBoolean() ? new Color(0x38BDF8) : new Color(0xFACC15), true);
                }
                if (!hb.impactTriggered && t >= 0.34) {
                    hb.impactTriggered = true;
                    rings.add(new ShockwaveRing(hb.target.x, hb.target.y, hb.cellSize * 4.4, new Color(0xFACC15), 540L));
                    rings.add(new ShockwaveRing(hb.target.x, hb.target.y, hb.cellSize * 3.1, new Color(0x38BDF8), 440L));
                    rings.add(new ShockwaveRing(hb.target.x, hb.target.y, hb.cellSize * 1.9, Color.WHITE, 300L));
                    blooms.add(new ImpactBloom(hb.target.x, hb.target.y, hb.cellSize * 3.8, new Color(0x7DD3FC), 520L));
                    spawnBurstColor(hb.target.x, hb.target.y, 40, new Color(0xFDE047), true);
                    for (Point tp : hb.cellTargets) {
                        bolts.add(new LightningBolt(tp.x + (rng.nextDouble() - 0.5) * 28.0, Math.max(0, tp.y - hb.cellSize * 3),
                                tp.x, tp.y, new Color(0x38BDF8), 2.6f, 260L, rng));
                        blooms.add(new ImpactBloom(tp.x, tp.y, hb.cellSize * 1.35, new Color(0x38BDF8), 320L));
                    }
                    callouts.add(new FloatingCallout(hb.target.x, Math.max(40, hb.target.y - hb.cellSize * 1.3),
                            hb.lethalHit ? "HỦY DIỆT HYPERION · CHOÁNG!" : "HYPERION NOVA · CHOÁNG!",
                            new Color(0x67E8F9), 1150L, -18.0));
                    if (hb.onImpact != null) {
                        if (callbacks == null) callbacks = new ArrayList<>(2);
                        callbacks.add(hb.onImpact);
                    }
                }
                if (t >= 1.0) {
                    if (!hb.impactTriggered) {
                        hb.impactTriggered = true;
                        if (hb.onImpact != null) {
                            if (callbacks == null) callbacks = new ArrayList<>(2);
                            callbacks.add(hb.onImpact);
                        }
                    }
                    hbIt.remove();
                }
            }

            Iterator<SuperShipCutsceneAnim> scIt = superCutscenes.iterator();
            while (scIt.hasNext()) {
                SuperShipCutsceneAnim sc = scIt.next();
                double t = (now - sc.startMs) / (double) sc.durationMs;
                if (t >= 0.25 && t <= 0.52 && now - sc.lastSparkMs >= 42L) {
                    sc.lastSparkMs = now;
                    int ow = Math.max(600, getWidth());
                    int oh = Math.max(500, getHeight());
                    double weldP = (t - 0.25) / 0.27;
                    int hullW = Math.min((int) (ow * 0.56), 540);
                    int wx = (ow - hullW) / 2 + (int) Math.round(weldP * hullW);
                    int wy = oh / 2 + rng.nextInt(44) - 22;
                    spawnBurstColor(wx, wy, 6, rng.nextBoolean() ? new Color(0xFDE047) : new Color(0x38BDF8), true);
                }
                if (!sc.coreRingTriggered && t >= 0.39) {
                    sc.coreRingTriggered = true;
                    int cx = Math.max(300, getWidth() / 2);
                    int cy = Math.max(250, getHeight() / 2);
                    rings.add(new ShockwaveRing(cx, cy, sc.cellSize * 5.4, new Color(0x38BDF8), 580L));
                    rings.add(new ShockwaveRing(cx, cy, sc.cellSize * 3.6, new Color(0xFACC15), 420L));
                }
                if (!sc.weaponSnapTriggered && t >= 0.66) {
                    sc.weaponSnapTriggered = true;
                    int cx = Math.max(300, getWidth() / 2);
                    int cy = Math.max(250, getHeight() / 2);
                    rings.add(new ShockwaveRing(cx, cy - 28, sc.cellSize * 4.2, new Color(0xFACC15), 480L));
                    spawnBurstColor(cx, cy - 26, 26, new Color(0xFDE047), true);
                }
                if (!sc.launchBlastTriggered && t >= 0.78) {
                    sc.launchBlastTriggered = true;
                    int cx = Math.max(300, getWidth() / 2);
                    int cy = Math.max(250, getHeight() / 2);
                    rings.add(new ShockwaveRing(cx - 110, cy, sc.cellSize * 4.8, new Color(0x38BDF8), 560L));
                    spawnBurstColor(cx - 120, cy, 34, new Color(0x7DD3FC), true);
                }
                if (!sc.completedTriggered && t >= 0.94) {
                    sc.completedTriggered = true;
                    rings.add(new ShockwaveRing(sc.dockDest.x, sc.dockDest.y, sc.cellSize * 4.2, new Color(0xFDE047), 560L));
                    blooms.add(new ImpactBloom(sc.dockDest.x, sc.dockDest.y, sc.cellSize * 3.6, new Color(0x38BDF8), 520L));
                    callouts.add(new FloatingCallout(sc.dockDest.x, Math.max(48, sc.dockDest.y - sc.cellSize),
                            "SIÊU TÀU AETHER ARK · XUẤT KÍCH!", new Color(0xFDE047), 1250L, -18.0));
                    if (sc.onCompleted != null) {
                        if (callbacks == null) callbacks = new ArrayList<>(2);
                        callbacks.add(sc.onCompleted);
                    }
                }
                if (t >= 1.0) {
                    if (!sc.completedTriggered) {
                        sc.completedTriggered = true;
                        if (sc.onCompleted != null) {
                            if (callbacks == null) callbacks = new ArrayList<>(2);
                            callbacks.add(sc.onCompleted);
                        }
                    }
                    scIt.remove();
                }
            }

            envWaves.removeIf(ew -> (now - ew.startMs) >= ew.durationMs);

            idle = isSceneIdle();
        }
        if (callbacks != null) {
            for (Runnable cb : callbacks) cb.run();
        }
        int desiredDelay = idle ? IDLE_TICK_MS : ACTIVE_TICK_MS;
        if (timer.getDelay() != desiredDelay) timer.setDelay(desiredDelay);
        if (idle && !isShowing()) {
            timer.stop();
            return;
        }
        repaint();
    }

    private boolean isSceneIdle() {
        synchronized (fxLock) {
            return strikes.isEmpty() && particles.isEmpty() && rings.isEmpty()
                    && bolts.isEmpty() && blooms.isEmpty() && shieldFlares.isEmpty()
                    && shieldShatters.isEmpty()
                    && callouts.isEmpty() && fleetShields.isEmpty() && bossIntros.isEmpty()
                    && bossRampages.isEmpty() && envWaves.isEmpty()
                    && hyperionBeams.isEmpty() && superCutscenes.isEmpty();
        }
    }

    private void triggerImpactVisuals(ActiveStrike s) {
        if (s.shieldBlocked) {
            if (s.onImpact == null) {
                triggerShieldDeflect(s.target);
            }
            spawnBurst(s.target.x, s.target.y, 18, CombatLoadout.MagicOption.ARCANE_SONAR, s.weapon, false);
            return;
        }

        boolean waterMiss = (s.zombieClass == null && s.titanSkill == null && !s.lethalHit
                && s.magic == CombatLoadout.MagicOption.NONE
                && s.weapon == CombatLoadout.WeaponOption.CANNON);
        Color ringCol = s.titanSkill != null
                ? s.titanSkill.accentColor
                : (s.zombieClass != null
                        ? s.zombieClass.accentColor
                        : (s.magic == CombatLoadout.MagicOption.NONE
                                ? (s.lethalHit ? UiTokens.HIT_CRIMSON : new Color(0xBAE6FD))
                                : s.magic.accentColor));
        double maxR = s.titanSkill != null
                ? s.cellSize * (s.titanSkill.leavesImpactZone ? 3.60 : 2.65)
                : switch (s.weapon) {
                    case AIRSTRIKE -> s.cellSize * 3.15;
                    case MISSILE_CROSS -> s.cellSize * 2.45;
                    case TORPEDO -> s.cellSize * 2.00;
                    case CANNON -> s.cellSize * 1.65;
                };

        blooms.add(new ImpactBloom(s.target.x, s.target.y, maxR * 1.38, ringCol, 500L));
        if (!waterMiss) {
            blooms.add(new ImpactBloom(s.target.x, s.target.y, maxR * 0.56, Color.WHITE, 220L));
        }
        rings.add(new ShockwaveRing(s.target.x, s.target.y, maxR, ringCol, 480L));
        if (s.titanSkill != null || s.magic != CombatLoadout.MagicOption.NONE || s.weapon != CombatLoadout.WeaponOption.CANNON || s.lethalHit) {
            rings.add(new ShockwaveRing(s.target.x, s.target.y, maxR * 0.68, Color.WHITE, 280L));
        } else {
            // Second gentle sea splash ripple on cannon miss
            rings.add(new ShockwaveRing(s.target.x, s.target.y, maxR * 0.58, new Color(0xE0F2FE), 320L));
        }

        // Titan Boss signature finishers & chain bolts
        if (s.titanSkill != null) {
            if (s.titanSkill.boss == TitanMode.TitanBoss.GHIDORAH) {
                Color beamCol = switch (s.titanSkill) {
                    case KG_OMEGA_VORTEX, KG_CATACLYSM_GRAVITY -> new Color(0xC084FC);
                    case KG_ALPHA_STORM -> new Color(0xF59E0B);
                    default -> new Color(0xFACC15);
                };
                for (int i = 0; i < Math.min(s.cellTargets.size(), 9); i++) {
                    Point pt = s.cellTargets.get(i);
                    bolts.add(new LightningBolt(
                            pt.x + (rng.nextDouble() - 0.5) * 44.0, 0,
                            pt.x, pt.y, beamCol, s.titanSkill.phase2Only ? 3.9f : 3.3f, 440L + i * 26L, rng));
                    if (s.titanSkill == TitanMode.TitanSkill.KG_OMEGA_VORTEX
                            || s.titanSkill == TitanMode.TitanSkill.KG_ALPHA_STORM
                            || s.titanSkill == TitanMode.TitanSkill.KG_CATACLYSM_GRAVITY) {
                        bolts.add(new LightningBolt(s.target.x, s.target.y, pt.x, pt.y, new Color(0xFDE047), 2.4f, 380L, rng));
                    }
                }
                if (s.titanSkill == TitanMode.TitanSkill.KG_CATACLYSM_GRAVITY) {
                    rings.add(new ShockwaveRing(s.target.x, s.target.y, maxR * 1.48, new Color(0xC084FC), 700L));
                }
            } else if (s.titanSkill == TitanMode.TitanSkill.GZ_ATOMIC_BREATH || s.titanSkill == TitanMode.TitanSkill.GZ_SPIRAL_RAY) {
                boolean spiral = s.titanSkill == TitanMode.TitanSkill.GZ_SPIRAL_RAY;
                Color ringC = spiral ? new Color(0xEF4444) : new Color(0x67E8F9);
                Color bloomC = spiral ? new Color(0xF59E0B) : new Color(0x22D3EE);
                rings.add(new ShockwaveRing(s.target.x, s.target.y, maxR * (spiral ? 1.32 : 1.12), ringC, 600L));
                for (Point pt : s.cellTargets) {
                    blooms.add(new ImpactBloom(pt.x, pt.y, s.cellSize * 1.45, bloomC, 460L));
                }
            } else if (s.titanSkill == TitanMode.TitanSkill.GZ_THERMO_PULSE || s.titanSkill == TitanMode.TitanSkill.GZ_MELTDOWN_RIFT) {
                boolean meltdown = s.titanSkill == TitanMode.TitanSkill.GZ_MELTDOWN_RIFT;
                rings.add(new ShockwaveRing(s.target.x, s.target.y, maxR * (meltdown ? 1.68 : 1.45), new Color(0xFDE047), 700L));
                rings.add(new ShockwaveRing(s.target.x, s.target.y, maxR * (meltdown ? 1.25 : 1.05),
                        meltdown ? new Color(0xEF4444) : new Color(0xEA580C), 580L));
                if (meltdown) {
                    for (Point pt : s.cellTargets) {
                        blooms.add(new ImpactBloom(pt.x, pt.y, s.cellSize * 1.4, new Color(0xF97316), 460L));
                    }
                }
            } else if (s.titanSkill.boss == TitanMode.TitanBoss.MOTHRA) {
                boolean nova = s.titanSkill == TitanMode.TitanSkill.MT_SOLAR_SUPERNOVA;
                Color ringC = switch (s.titanSkill) {
                    case MT_SOLAR_SUPERNOVA -> new Color(0xFB7185);
                    case MT_GOD_RAY -> new Color(0xFDE047);
                    case MT_PRISM_RAIN -> new Color(0x38BDF8);
                    case MT_SILK_WEB -> new Color(0xA7F3D0);
                    default -> new Color(0x2DD4BF);
                };
                rings.add(new ShockwaveRing(s.target.x, s.target.y, maxR * (nova ? 1.65 : 1.18), ringC, 620L));
                rings.add(new ShockwaveRing(s.target.x, s.target.y, maxR * 0.85, new Color(0xFEF08A), 440L));
                for (Point pt : s.cellTargets) {
                    blooms.add(new ImpactBloom(pt.x, pt.y, s.cellSize * (nova ? 1.65 : 1.25), ringC, 460L));
                    spawnBurstColor(pt.x, pt.y, nova ? 12 : 7, ringC, true);
                }
            } else if (s.titanSkill == TitanMode.TitanSkill.GZ_TAIL_SLAM) {
                // Erupt twin crashing Tsunami ocean geysers across the 1x4 slam strip!
                rings.add(new ShockwaveRing(s.target.x, s.target.y, maxR * 1.25, new Color(0xBAE6FD), 580L));
                for (Point pt : s.cellTargets) {
                    blooms.add(new ImpactBloom(pt.x, pt.y, s.cellSize * 1.45, new Color(0x38BDF8), 420L));
                    for (int k = 0; k < 8; k++) {
                        particles.add(new TrailParticle(
                                pt.x + (rng.nextDouble() - 0.5) * s.cellSize * 0.7,
                                pt.y,
                                (rng.nextDouble() - 0.5) * 3.8,
                                -3.2 - rng.nextDouble() * 5.6,
                                5.0 + rng.nextDouble() * 5.0,
                                k % 2 == 0 ? Color.WHITE : new Color(0x7DD3FC),
                                System.currentTimeMillis(), 440L + rng.nextInt(180),
                                CombatLoadout.MagicOption.ARCANE_SONAR, ParticleKind.BUBBLE));
                    }
                }
            }
        } else if (s.magic == CombatLoadout.MagicOption.LIGHTNING) {
            // Lightning combo unleashes a Sky-to-Sea Thunder Pillar + Chain Forks across target cells
            bolts.add(new LightningBolt(s.target.x + (rng.nextDouble() - 0.5) * 32.0, 0, s.target.x, s.target.y,
                    new Color(0xE0F2FE), 3.8f, 360L, rng));
            bolts.add(new LightningBolt(s.target.x + (rng.nextDouble() - 0.5) * 16.0, 0, s.target.x, s.target.y,
                    new Color(0xA855F7), 2.4f, 320L, rng));
            for (int i = 0; i < Math.min(s.cellTargets.size(), 6); i++) {
                Point pt = s.cellTargets.get(i);
                if (pt.distance(s.target) > 4) {
                    bolts.add(new LightningBolt(s.target.x, s.target.y, pt.x, pt.y,
                            new Color(0x38BDF8), 2.2f, 310L, rng));
                }
            }
        }

        // Multi-cell AoE shockwaves + secondary blooms on AoE/Ultimates
        for (int i = 0; i < s.cellTargets.size(); i++) {
            Point pt = s.cellTargets.get(i);
            if (pt.distance(s.target) > 4) {
                rings.add(new ShockwaveRing(pt.x, pt.y, s.cellSize * 1.05, ringCol, 340L + i * 25L));
                if ((s.weapon == CombatLoadout.WeaponOption.AIRSTRIKE || s.titanSkill != null) && i % 2 == 0) {
                    blooms.add(new ImpactBloom(pt.x, pt.y, s.cellSize * 1.15, ringCol, 320L + i * 20L));
                }
            }
        }

        int burstCount = s.titanSkill != null ? 48 : (s.weapon == CombatLoadout.WeaponOption.CANNON ? 26 : 38);
        if (s.titanSkill != null) {
            spawnBurstColor(s.target.x, s.target.y, burstCount, s.titanSkill.accentColor, false);
        } else if (s.zombieClass != null) {
            spawnBurstColor(s.target.x, s.target.y, burstCount, s.zombieClass.accentColor, false);
        } else {
            spawnBurst(s.target.x, s.target.y, burstCount, s.magic, s.weapon, false, waterMiss);
        }

        // Tactical floating HUD callout badge
        String text = resolveCalloutText(s);
        if (text != null) {
            callouts.add(new FloatingCallout(s.target.x, s.target.y - s.cellSize * 0.4, text, ringCol, 780L));
        }
    }

    private static String resolveCalloutText(ActiveStrike s) {
        if (s.titanSkill != null) {
            return s.titanSkill.skillName.toUpperCase() + "!";
        }
        if (s.zombieClass != null) {
            return s.zombieClass.skillName.toUpperCase() + "!";
        }
        if (s.magic != CombatLoadout.MagicOption.NONE) {
            return switch (s.magic) {
                case FIRE -> "HỎA NGỤC!";
                case LIGHTNING -> "THIÊN LÔI!";
                case ARCANE_SONAR -> "QUÉT CỔ NGỮ!";
                case FROST -> "BĂNG PHONG!";
                case NONE -> null;
            };
        }
        return switch (s.weapon) {
            case AIRSTRIKE -> "KHÔNG KÍCH!";
            case MISSILE_CROSS -> "ĐA ĐẦU ĐẠN!";
            case TORPEDO -> "NGƯ LÔI!";
            case CANNON -> s.lethalHit ? "TRÚNG ĐÍCH!" : null;
        };
    }

    private void spawnFlightParticle(ActiveStrike s, double x, double y, double t) {
        long now = System.currentTimeMillis();
        double spread = s.weapon == CombatLoadout.WeaponOption.AIRSTRIKE ? 2.4 : 1.3;
        double vx = (rng.nextDouble() - 0.5) * spread;
        double vy = (rng.nextDouble() - 0.5) * spread;
        Color c = s.titanSkill != null
                ? (rng.nextBoolean() ? s.titanSkill.accentColor : Color.WHITE)
                : (s.zombieClass != null
                        ? (rng.nextBoolean() ? s.zombieClass.accentColor : Color.WHITE)
                        : particleColor(s.magic, s.weapon, rng.nextBoolean()));
        long life = 230L + rng.nextInt(170);
        double size = 3.4 + rng.nextDouble() * 4.8;
        ParticleKind kind = defaultKindFor(s.magic, s.weapon);
        particles.add(new TrailParticle(x, y, vx, vy, size, c, now, life, s.magic, kind));

        // Normal Cannonball volumetric smoke & ember trail
        if (s.weapon == CombatLoadout.WeaponOption.CANNON && s.magic == CombatLoadout.MagicOption.NONE) {
            boolean isSpark = rng.nextInt(3) == 0;
            Color smoke = isSpark ? new Color(0xFBBF24) : new Color(0xCBD5E1);
            double smSize = isSpark ? 3.8 : 7.0 + rng.nextDouble() * 4.8;
            particles.add(new TrailParticle(x, y, vx * 0.65, vy * 0.65 - 0.25, smSize, smoke,
                    now, 290L + rng.nextInt(140), s.magic, isSpark ? ParticleKind.SPARK : ParticleKind.SMOKE));
        }

        // Wingtip twin trails + jet exhaust sparks for Stealth Airstrike Squadron
        if (s.weapon == CombatLoadout.WeaponOption.AIRSTRIKE && t > 0.05 && t < 0.95) {
            Color wc = s.magic == CombatLoadout.MagicOption.NONE ? new Color(0x93C5FD) : s.magic.accentColor;
            particles.add(new TrailParticle(x - 10, y - 16, -0.4, -0.25, 4.2, wc, now, 220L, s.magic, ParticleKind.ORB));
            particles.add(new TrailParticle(x - 10, y + 16, -0.4, 0.25, 4.2, wc, now, 220L, s.magic, ParticleKind.ORB));
        }
    }

    private static ParticleKind defaultKindFor(CombatLoadout.MagicOption magic, CombatLoadout.WeaponOption weapon) {
        if (magic == CombatLoadout.MagicOption.FROST) return ParticleKind.FROST;
        if (weapon == CombatLoadout.WeaponOption.TORPEDO && magic == CombatLoadout.MagicOption.NONE) return ParticleKind.BUBBLE;
        return ParticleKind.SPARK;
    }

    private void spawnBurstColor(double x, double y, int count, Color accent, boolean launch) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            double ang = rng.nextDouble() * Math.PI * 2.0;
            double speed = (launch ? 2.2 : 3.2) + rng.nextDouble() * (launch ? 2.6 : 5.0);
            double vx = Math.cos(ang) * speed;
            double vy = Math.sin(ang) * speed;
            boolean spark = (i % 2 == 0);
            boolean smoke = (!launch && i % 5 == 0);
            Color c = (i % 3 == 0) ? Color.WHITE : (smoke ? new Color(0x64748B) : accent);
            ParticleKind kind = smoke ? ParticleKind.SMOKE : (spark ? ParticleKind.SPARK : ParticleKind.ORB);
            long life = (launch ? 260L : 400L) + rng.nextInt(190);
            double size = smoke ? (10.0 + rng.nextDouble() * 8.0) : ((launch ? 3.2 : 4.2) + rng.nextDouble() * 5.2);
            particles.add(new TrailParticle(x, y, vx, vy, size, c, now, life, CombatLoadout.MagicOption.NONE, kind));
        }
    }

    private void spawnBurst(double x, double y, int count, CombatLoadout.MagicOption magic,
                            CombatLoadout.WeaponOption weapon, boolean launch) {
        spawnBurst(x, y, count, magic, weapon, launch, false);
    }

    private void spawnBurst(double x, double y, int count, CombatLoadout.MagicOption magic,
                            CombatLoadout.WeaponOption weapon, boolean launch, boolean waterMiss) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            double ang = rng.nextDouble() * Math.PI * 2.0;
            double speed = (launch ? 2.2 : 3.1) + rng.nextDouble() * (launch ? 2.6 : 5.1);
            double vx = Math.cos(ang) * speed;
            double vy = Math.sin(ang) * speed - (waterMiss ? 0.65 : 0.0);
            ParticleKind kind;
            Color c;
            double size;
            if (waterMiss) {
                kind = (i % 3 == 0) ? ParticleKind.BUBBLE : ParticleKind.ORB;
                c = (i % 2 == 0) ? Color.WHITE : new Color(0x7DD3FC);
                size = 3.8 + rng.nextDouble() * 4.5;
            } else if (!launch && magic != CombatLoadout.MagicOption.FROST && magic != CombatLoadout.MagicOption.ARCANE_SONAR && i % 5 == 0) {
                kind = ParticleKind.SMOKE;
                c = new Color(0x64748B);
                size = 10.0 + rng.nextDouble() * 7.5;
            } else {
                kind = (magic == CombatLoadout.MagicOption.FROST && i % 2 == 0)
                        ? ParticleKind.FROST
                        : ((i % 2 == 0) ? ParticleKind.SPARK : ParticleKind.ORB);
                c = (i % 4 == 0) ? Color.WHITE : particleColor(magic, weapon, i % 2 == 0);
                size = (launch ? 3.4 : 4.2) + rng.nextDouble() * 5.2;
            }
            long life = (launch ? 260L : 400L) + rng.nextInt(190);
            particles.add(new TrailParticle(x, y, vx, vy, size, c, now, life, magic, kind));
        }
    }

    private static Color particleColor(CombatLoadout.MagicOption magic, CombatLoadout.WeaponOption weapon, boolean alt) {
        return switch (magic) {
            case FIRE -> alt ? UiTokens.MAGIC_FIRE : new Color(0xFDE047);
            case LIGHTNING -> alt ? new Color(0x38BDF8) : new Color(0xC084FC);
            case ARCANE_SONAR -> alt ? UiTokens.MAGIC_SONAR : new Color(0x67E8F9);
            case FROST -> alt ? UiTokens.MAGIC_FROST : new Color(0xE0F2FE);
            case NONE -> weapon == CombatLoadout.WeaponOption.TORPEDO
                    ? (alt ? new Color(0x7DD3FC) : Color.WHITE)
                    : (alt ? UiTokens.ACCENT_SIGNAL : new Color(0xFCA5A5));
        };
    }

    private static Point2 computeAtomicBeamTip(ActiveStrike s, double t) {
        if (t < 0.38) {
            return new Point2(s.origin.x, s.origin.y);
        }
        List<Point> targets = s.cellTargets;
        Point first = (targets != null && !targets.isEmpty()) ? targets.get(0) : s.target;
        if (t < 0.50) {
            double reach = Math.min(1.0, (t - 0.38) / 0.12);
            double ease = 1.0 - Math.pow(1.0 - reach, 2.2);
            return new Point2(s.origin.x + (first.x - s.origin.x) * ease,
                    s.origin.y + (first.y - s.origin.y) * ease);
        }
        if (targets == null || targets.size() <= 1) {
            return new Point2(s.target.x, s.target.y);
        }
        double sweep = Math.max(0.0, Math.min(1.0, (t - 0.50) / 0.50));
        double idxF = sweep * (targets.size() - 1);
        int i0 = Math.min(targets.size() - 1, (int) Math.floor(idxF));
        int i1 = Math.min(targets.size() - 1, i0 + 1);
        double frac = idxF - i0;
        Point p0 = targets.get(i0);
        Point p1 = targets.get(i1);
        return new Point2(p0.x + (p1.x - p0.x) * frac, p0.y + (p1.y - p0.y) * frac);
    }

    private static Point2 computePosition(ActiveStrike s, double rawT) {
        double t = Math.max(0.0, Math.min(1.0, rawT));
        double dx = s.target.x - s.origin.x;
        double dy = s.target.y - s.origin.y;
        double dist = Math.max(1.0, Math.hypot(dx, dy));

        if (s.titanSkill != null) {
            if (s.titanSkill == TitanMode.TitanSkill.GZ_ATOMIC_BREATH || s.titanSkill == TitanMode.TitanSkill.GZ_SPIRAL_RAY) {
                return computeAtomicBeamTip(s, t);
            }
            if (s.titanSkill == TitanMode.TitanSkill.GZ_TAIL_SLAM) {
                double whipEase = Math.pow(t, 1.45);
                double x = s.origin.x + dx * whipEase;
                double y = s.origin.y + dy * whipEase - Math.sin(t * Math.PI) * Math.min(175.0, dist * 0.40);
                return new Point2(x, y);
            }
            double ease = 1.0 - Math.pow(1.0 - t, 1.65);
            double x = s.origin.x + dx * ease;
            double y = s.origin.y + dy * ease - Math.sin(t * Math.PI) * Math.min(64.0, dist * 0.14);
            return new Point2(x, y);
        }

        double ease = 1.0 - Math.pow(1.0 - t, 1.85);
        double x = s.origin.x + dx * ease;
        double y = s.origin.y + dy * ease;

        switch (s.weapon) {
            case CANNON -> {
                double arc = Math.sin(t * Math.PI) * Math.min(86.0, dist * 0.20);
                y -= arc;
            }
            case MISSILE_CROSS -> {
                double curve = Math.sin(t * Math.PI) * Math.min(72.0, dist * 0.16);
                y -= curve + Math.sin(t * Math.PI * 3.0) * 6.5 * (1.0 - t);
            }
            case AIRSTRIKE -> {
                double overshoot = 1.32;
                double jt = t * overshoot - 0.16;
                x = s.origin.x + dx * jt;
                y = s.origin.y + dy * jt - Math.sin(Math.min(1.0, t) * Math.PI) * 38.0;
            }
            case TORPEDO -> {
                double nx = -dy / dist;
                double ny = dx / dist;
                double weave = Math.sin(t * Math.PI * 4.0) * 3.2 * (1.0 - t);
                x += nx * weave;
                y += ny * weave;
            }
        }
        return new Point2(x, y);
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Ambient atmospheric weather particles & 3D wing-flapping Arcane Dragons
        double sceneTime = OceanMapBackdrop.elapsedSeconds();
        if (isShowing()) OceanMapBackdrop.syncLiveEnvironmentAudio(sceneTime);
        OceanMapBackdrop.paintAtmosphericWeather(g, getWidth(), getHeight(), sceneTime);
        OceanMapBackdrop.paintPatrollingDragons(g, getWidth(), getHeight(), sceneTime);

        if (isSceneIdle()) {
            g.dispose();
            return;
        }

        long now = System.currentTimeMillis();
        synchronized (fxLock) {
            // 0. Subtle ambient radial impact blooms (cached stamps behind foreground pixel blocks)
            for (ImpactBloom b : blooms) {
                double p = Math.max(0.0, Math.min(1.0, (now - b.startMs) / (double) b.durationMs));
                float alpha = (float) Math.max(0.0, (1.0 - p) * 0.56);
                double r = Math.max(10.0, b.maxRadius * (0.45 + 0.55 * Math.sin(p * Math.PI * 0.5)));
                drawGlow(g, b.color, b.x, b.y, r, alpha, !Color.WHITE.equals(b.color));
            }

            // 1. Voxel pixel-stepped shockwave rings
            for (ShockwaveRing r : rings) {
                double p = Math.max(0.0, Math.min(1.0, (now - r.startMs) / (double) r.durationMs));
                float alpha = (float) Math.max(0.0, (1.0 - p) * 0.88);
                double rad = 8.0 + r.maxRadius * (1.0 - Math.pow(1.0 - p, 2.2));
                int px = Math.max(2, (int) Math.round(4.4 * (1.0 - p * 0.65)));
                g.setComposite(alphaComp(alpha));
                g.setColor(r.color);
                SpriteAssets.drawVoxelRing(g, r.x, r.y, rad, px, false);
            }

            // 1.5 Allied 5-Ship Arcane Magic Circles & Pentagram Ley-Lines
            for (FleetShieldAnim anim : fleetShields) {
                drawFleetShieldCircles(g, anim, now);
            }

            // 2. Jagged Sky-to-Sea & Chain Lightning Bolts + Secondary Branches + Pixel Corona Nodes
            for (LightningBolt b : bolts) {
                double p = Math.max(0.0, Math.min(1.0, (now - b.startMs) / (double) b.durationMs));
                float alpha = (float) Math.max(0.0, 1.0 - p);
                drawGlow(g, b.color, b.endX, b.endY, 32.0 + (1.0 - p) * 16.0, alpha * 0.62f, false);
                g.setComposite(alphaComp(alpha * 0.34f));
                g.setColor(b.color);
                g.setStroke(roundStroke(b.thickness * 3.1f));
                g.draw(b.path);
                g.setComposite(alphaComp(alpha * 0.74f));
                g.setStroke(roundStroke(b.thickness * 1.9f));
                g.draw(b.path);
                g.draw(b.branches);
                g.setComposite(alphaComp(alpha));
                g.setColor(Color.WHITE);
                g.setStroke(roundStroke(Math.max(1.2f, b.thickness * 0.78f)));
                g.draw(b.path);
                g.draw(b.branches);
                SpriteAssets.drawVoxelCube(g, (int) Math.round(b.endX), (int) Math.round(b.endY), 6, Color.WHITE, b.color);
            }

            // 3. Voxel Sparks, Cube Smoke Puffs, Pixel Frost Shards, Square Bubbles & Glowing Voxel Orbs
            for (TrailParticle p : particles) {
                double prog = Math.max(0.0, Math.min(1.0, (now - p.startMs) / (double) (p.endMs - p.startMs)));
                float alpha = (float) Math.max(0.0, 1.0 - prog);
                int cx = (int) Math.round(p.x);
                int cy = (int) Math.round(p.y);
                if (p.kind == ParticleKind.SMOKE) {
                    int cubeSz = Math.max(4, ((int) Math.round(p.size * (0.75 + prog * 1.05)) / 2) * 2);
                    float smAlpha = (float) (Math.sin(Math.min(1.0, prog * 1.1) * Math.PI) * 0.62);
                    drawGlow(g, p.color, p.x, p.y, cubeSz * 0.9, smAlpha * 0.32f, true);
                    g.setComposite(alphaComp(smAlpha));
                    SpriteAssets.drawVoxelCube(g, cx, cy, cubeSz, p.color, new Color(0xE2E8F0));
                } else if (p.kind == ParticleKind.SPARK) {
                    double curLen = Math.max(4.0, p.size * (1.2 - prog * 0.4) + Math.hypot(p.vx, p.vy) * 1.6);
                    double spd = Math.hypot(p.vx, p.vy);
                    double ux = spd > 0.05 ? p.vx / spd : 1.0;
                    double uy = spd > 0.05 ? p.vy / spd : 0.0;
                    int blockSz = Math.max(2, (int) Math.round(p.size * 0.45));
                    g.setComposite(alphaComp(alpha * 0.80f));
                    g.setColor(p.color);
                    for (int step = 1; step <= 3; step++) {
                        int bx = (int) Math.round(p.x - ux * (curLen * step * 0.30));
                        int by = (int) Math.round(p.y - uy * (curLen * step * 0.30));
                        int tSz = Math.max(2, blockSz - (step > 1 ? 1 : 0));
                        g.fillRect(bx - tSz / 2, by - tSz / 2, tSz, tSz);
                    }
                    g.setComposite(alphaComp(alpha));
                    int headSz = Math.max(3, blockSz + 1);
                    SpriteAssets.drawVoxelCube(g, cx, cy, headSz, Color.WHITE, p.color);
                } else if (p.kind == ParticleKind.FROST) {
                    int r = Math.max(3, (int) Math.round(p.size * (1.0 - prog * 0.40) * 0.8));
                    boolean diag = (((int) (prog * 6.0 + (p.startMs % 7))) & 1) == 1;
                    g.setComposite(alphaComp(alpha * 0.92f));
                    g.setColor(p.color);
                    if (diag) {
                        g.fillRect(cx - 1, cy - 1, 3, 3);
                        g.fillRect(cx - r, cy - r, 2, 2);
                        g.fillRect(cx + r - 1, cy - r, 2, 2);
                        g.fillRect(cx - r, cy + r - 1, 2, 2);
                        g.fillRect(cx + r - 1, cy + r - 1, 2, 2);
                    } else {
                        g.fillRect(cx - r, cy - 1, r * 2 + 1, 2);
                        g.fillRect(cx - 1, cy - r, 2, r * 2 + 1);
                    }
                    g.setColor(Color.WHITE);
                    g.fillRect(cx - 1, cy - 1, 2, 2);
                } else if (p.kind == ParticleKind.BUBBLE) {
                    int isz = Math.max(4, ((int) Math.round(p.size * (0.85 + prog * 0.45)) / 2) * 2);
                    int ix = cx - isz / 2;
                    int iy = cy - isz / 2;
                    g.setComposite(alphaComp(alpha * 0.80f));
                    g.setColor(p.color);
                    g.drawRect(ix, iy, isz, isz);
                    g.setColor(Color.WHITE);
                    g.fillRect(ix + 1, iy + 1, 2, 2);
                } else {
                    int curSz = Math.max(3, (int) Math.round(p.size * (1.0 - prog * 0.45) * 0.72));
                    drawGlow(g, p.color, p.x, p.y, Math.max(4.0, curSz * 1.45), alpha * 0.52f, false);
                    g.setComposite(alphaComp(alpha));
                    SpriteAssets.drawVoxelCube(g, cx, cy, curSz, p.color, Color.WHITE);
                    int inner = Math.max(2, curSz / 2);
                    g.setColor(Color.WHITE);
                    g.fillRect(cx - inner / 2, cy - inner / 2, inner, inner);
                }
            }

            // 4. Active Strikes: Launch Astrolabe Rune + Ribbon + Weapon-specific Choreography
            for (ActiveStrike s : strikes) {
                double t = Math.max(0.0, Math.min(1.28, (now - s.startMs) / (double) s.durationMs));
                BufferedImage rune = SpriteAssets.getMagicRune(s.magic);

                // Ornate 3D Astrolabe Launch Seal spinning cleanly at origin ship
                if (rune != null && t < 0.75) {
                    double lt = t / 0.75;
                    float runeAlpha = (float) Math.sin(Math.min(1.0, lt * 1.2) * Math.PI);
                    double scale = 0.72 + Math.sin(lt * Math.PI * 0.5) * 0.92;
                    int size = (int) Math.round(s.cellSize * 1.60 * scale);
                    drawRotatedCentered(g, rune, s.origin.x, s.origin.y, size, size, t * 4.2, runeAlpha * 0.95f);
                }

                // Target Lock Astrolabe Seal spinning over target zone + pixel-stepped tactical reticle
                if (rune != null && t > 0.24 && t <= 1.08) {
                    double tt = (t - 0.24) / 0.84;
                    float lockAlpha = (float) Math.min(0.92, Math.sin(tt * Math.PI) * 0.98);
                    int size = (int) Math.round(s.cellSize * (1.78 - 0.35 * tt));
                    drawRotatedCentered(g, rune, s.target.x, s.target.y, size, size, -t * 5.0, lockAlpha);
                    int lockR = (int) Math.round(size * 0.56);
                    g.setComposite(alphaComp(lockAlpha * 0.66f));
                    g.setColor(s.magic != CombatLoadout.MagicOption.NONE ? s.magic.accentColor : UiTokens.ACCENT_SIGNAL);
                    SpriteAssets.drawVoxelRing(g, s.target.x, s.target.y, lockR, 3, true);
                }

                // Torpedo V-shaped surface hydro-wake behind projectile
                if (s.weapon == CombatLoadout.WeaponOption.TORPEDO && s.history.size() >= 3 && t < 1.04) {
                    drawTorpedoHydroWake(g, s);
                }

                // Movie-accurate continuous beams / 3-head golden lightning streams for Titan Bosses
                if (s.titanSkill != null) {
                    drawTitanSkillChoreography(g, s, t, now);
                }

                // Glowing Trajectory Ribbon Trail
                if (s.history.size() >= 2 && t < 1.08
                        && s.titanSkill != TitanMode.TitanSkill.GZ_ATOMIC_BREATH
                        && s.titanSkill != TitanMode.TitanSkill.GZ_SPIRAL_RAY
                        && s.titanSkill != TitanMode.TitanSkill.GZ_MELTDOWN_RIFT
                        && s.titanSkill != TitanMode.TitanSkill.KG_ALPHA_STORM) {
                    drawTrajectoryRibbon(g, s);
                }

                // Weapon-specific 3D projectiles in flight
                if (t < 1.02) {
                    Point2 cur = computePosition(s, t);
                    Point2 prev = computePosition(s, Math.max(0.0, t - 0.03));
                    double dxA = cur.x - prev.x;
                    double dyA = cur.y - prev.y;
                    double angle = (Math.hypot(dxA, dyA) > 0.05)
                            ? Math.atan2(dyA, dxA)
                            : Math.atan2(s.target.y - s.origin.y, s.target.x - s.origin.x);

                    // Cannon / Airstrike ocean stepped pixel shadow reacting cleanly to arc altitude
                    if (s.titanSkill == null && (s.weapon == CombatLoadout.WeaponOption.CANNON || s.weapon == CombatLoadout.WeaponOption.AIRSTRIKE)) {
                        double gt = Math.min(1.0, t);
                        double alt = Math.sin(gt * Math.PI);
                        int gx = (int) Math.round(s.origin.x + (s.target.x - s.origin.x) * gt);
                        int gy = (int) Math.round(s.origin.y + (s.target.y - s.origin.y) * gt + 12.0 + alt * 14.0);
                        g.setComposite(alphaComp(0.28f - (float) (alt * 0.08)));
                        g.setColor(Color.BLACK);
                        double shScale = (s.weapon == CombatLoadout.WeaponOption.AIRSTRIKE ? 1.15 : 0.56) * (1.0 - alt * 0.16);
                        int shW = Math.max(6, ((int) (s.cellSize * shScale) / 2) * 2);
                        int shH = Math.max(4, shW / 2);
                        SpriteAssets.fillChamferedBox(g, gx - shW / 2, gy - shH / 2, shW, shH, Math.max(2, shH / 3));
                    }

                    // Multi-stop cached radial energy halo around projectile (no allocation)
                    Color aura = s.titanSkill != null
                            ? s.titanSkill.accentColor
                            : (s.zombieClass != null
                                    ? s.zombieClass.accentColor
                                    : (s.magic == CombatLoadout.MagicOption.NONE ? UiTokens.ACCENT_SIGNAL : s.magic.accentColor));
                    if (s.titanSkill != TitanMode.TitanSkill.GZ_ATOMIC_BREATH
                            && s.titanSkill != TitanMode.TitanSkill.GZ_SPIRAL_RAY
                            && s.titanSkill != TitanMode.TitanSkill.GZ_THERMO_PULSE
                            && s.titanSkill != TitanMode.TitanSkill.GZ_MELTDOWN_RIFT) {
                        double haloR = s.cellSize * (s.titanSkill != null ? 1.35 : (s.weapon == CombatLoadout.WeaponOption.AIRSTRIKE ? 1.15 : 0.72));
                        drawGlow(g, aura, cur.x, cur.y, haloR, 0.44f, true);
                    }

                    BufferedImage proj = s.titanSkill != null
                            ? SpriteAssets.getTitanSkillSprite(s.titanSkill)
                            : (s.zombieClass != null
                                    ? SpriteAssets.getZombieProjectileSprite(s.zombieClass)
                                    : SpriteAssets.getProjectileSprite(s.weapon));
                    if (proj != null) {
                        if (s.titanSkill == TitanMode.TitanSkill.GZ_ATOMIC_BREATH
                                || s.titanSkill == TitanMode.TitanSkill.GZ_SPIRAL_RAY) {
                            if (t >= 0.34 && t <= 0.62) {
                                double flareSz = s.cellSize * (2.3 - Math.abs(t - 0.42) * 3.0);
                                drawGlow(g, s.titanSkill == TitanMode.TitanSkill.GZ_SPIRAL_RAY
                                        ? new Color(0xF87171) : new Color(0x67E8F9), s.origin.x, s.origin.y, flareSz, 0.80f, false);
                            }
                        } else if (s.titanSkill == TitanMode.TitanSkill.GZ_THERMO_PULSE
                                || s.titanSkill == TitanMode.TitanSkill.GZ_MELTDOWN_RIFT) {
                            // Burning Supernova orb swells in chest core before unleashing dome/caldera wave
                            if (t < 0.42) {
                                double p = t / 0.42;
                                int cw = (int) Math.round(s.cellSize * (1.2 + p * 1.7));
                                drawGlow(g, aura, s.origin.x, s.origin.y, cw * 0.9, 0.85f, false);
                                drawRotatedCentered(g, proj, s.origin.x, s.origin.y, cw, cw, p * 3.2, 0.96f);
                            }
                        } else if (s.titanSkill == TitanMode.TitanSkill.KG_ALPHA_STORM) {
                            // Thunderhead vortex gathers above target board while shooting beams down
                            int cw = (int) Math.round(s.cellSize * (1.6 + Math.sin(t * Math.PI) * 0.65));
                            int vy = (int) Math.round(s.target.y - s.cellSize * 2.1);
                            drawGlow(g, new Color(0xFACC15), s.target.x, vy, cw * 0.95, 0.78f, false);
                            drawRotatedCentered(g, proj, s.target.x, vy, cw, cw, 0.0, 0.95f);
                        } else if (s.titanSkill == TitanMode.TitanSkill.GZ_TAIL_SLAM) {
                            // Colossal breaching Kaiju Tail Club arcing high & slamming onto the sea strip
                            double scaleTail = 2.65 + Math.sin(t * Math.PI) * 0.45;
                            int drawW = (int) Math.round(s.cellSize * scaleTail);
                            int drawH = (int) Math.round(drawW * (proj.getHeight() / (double) Math.max(1, proj.getWidth())));
                            double whipAng = -0.70 + t * 1.45;
                            drawGlow(g, new Color(0x38BDF8), cur.x, cur.y, s.cellSize * 0.95, 0.75f, false);
                            drawRotatedCentered(g, proj, cur.x, cur.y, drawW, drawH, whipAng, 1.0f);
                        } else if (s.titanSkill == TitanMode.TitanSkill.KG_TRIPLE_GRAVITY) {
                            // 3 synchronized golden Gravity Spearheads shooting from each Ghidorah head
                            int nHeads = Math.max(1, s.extraOrigins.size());
                            int nTgts = Math.max(1, s.cellTargets.size());
                            int subW = (int) Math.round(s.cellSize * 1.16);
                            int subH = (int) Math.round(subW * (proj.getHeight() / (double) Math.max(1, proj.getWidth())));
                            double gt = Math.min(1.0, t / 0.88);
                            for (int h = 0; h < Math.min(3, nHeads); h++) {
                                Point ho = s.extraOrigins.get(h);
                                Point ht = s.cellTargets.get((h * 2) % nTgts);
                                double hx = ho.x + (ht.x - ho.x) * gt;
                                double hy = ho.y + (ht.y - ho.y) * gt;
                                double ha = Math.atan2(ht.y - ho.y, ht.x - ho.x);
                                drawGlow(g, new Color(0xFACC15), hx, hy, s.cellSize * 0.52, 0.70f, false);
                                drawRotatedCentered(g, proj, hx, hy, subW, subH, ha, 1.0f);
                            }
                        } else if (s.titanSkill != null) {
                            double titanScale = (s.titanSkill == TitanMode.TitanSkill.KG_OMEGA_VORTEX
                                    || s.titanSkill == TitanMode.TitanSkill.KG_CATACLYSM_GRAVITY
                                    || s.titanSkill == TitanMode.TitanSkill.MT_SOLAR_SUPERNOVA)
                                    ? (1.48 + Math.sin(t * Math.PI) * 0.72)
                                    : 1.48;
                            int drawW = (int) Math.round(s.cellSize * titanScale);
                            int drawH = (int) Math.round(drawW * (proj.getHeight() / (double) Math.max(1, proj.getWidth())));
                            double drawAngle = (s.titanSkill == TitanMode.TitanSkill.KG_HURRICANE_WING
                                    || s.titanSkill == TitanMode.TitanSkill.KG_OMEGA_VORTEX
                                    || s.titanSkill == TitanMode.TitanSkill.KG_CATACLYSM_GRAVITY
                                    || s.titanSkill == TitanMode.TitanSkill.MT_SILK_WEB
                                    || s.titanSkill == TitanMode.TitanSkill.MT_SOLAR_SUPERNOVA)
                                    ? t * 8.0 : angle;
                            drawGlow(g, aura, cur.x, cur.y, s.cellSize * 0.65, 0.72f, false);
                            drawRotatedCentered(g, proj, cur.x, cur.y, drawW, drawH, drawAngle, 1.0f);
                        } else if (s.weapon == CombatLoadout.WeaponOption.AIRSTRIKE) {
                            // 3-Jet V-Formation Squadron + Twin Engine Afterburners!
                            double nx = -Math.sin(angle);
                            double ny = Math.cos(angle);
                            double backX = -Math.cos(angle);
                            double backY = -Math.sin(angle);
                            int leadW = (int) Math.round(s.cellSize * 1.98);
                            int leadH = (int) Math.round(leadW * (proj.getHeight() / (double) Math.max(1, proj.getWidth())));
                            int wingW = (int) Math.round(leadW * 0.74);
                            int wingH = (int) Math.round(leadH * 0.74);
                            double wingBack = s.cellSize * 0.85;
                            double wingSpread = s.cellSize * 1.12;
                            double w1x = cur.x + backX * wingBack + nx * wingSpread;
                            double w1y = cur.y + backY * wingBack + ny * wingSpread;
                            double w2x = cur.x + backX * wingBack - nx * wingSpread;
                            double w2y = cur.y + backY * wingBack - ny * wingSpread;
                            drawGlow(g, aura, w1x + backX * wingW * 0.42, w1y + backY * wingW * 0.42, s.cellSize * 0.38, 0.65f, false);
                            drawGlow(g, aura, w2x + backX * wingW * 0.42, w2y + backY * wingW * 0.42, s.cellSize * 0.38, 0.65f, false);
                            drawGlow(g, Color.WHITE, cur.x + backX * leadW * 0.42, cur.y + backY * leadW * 0.42, s.cellSize * 0.48, 0.75f, false);
                            drawRotatedCentered(g, proj, w1x, w1y, wingW, wingH, angle, 0.92f);
                            drawRotatedCentered(g, proj, w2x, w2y, wingW, wingH, angle, 0.92f);
                            drawRotatedCentered(g, proj, cur.x, cur.y, leadW, leadH, angle, 1.0f);

                            // Blazing plasma voxel warhead streaks dropping onto target grid during flyover
                            if (t > 0.42 && t < 0.96) {
                                double bt = (t - 0.42) / 0.54;
                                g.setStroke(roundStroke(2.2f));
                                for (int i = 0; i < Math.min(6, s.cellTargets.size()); i++) {
                                    Point tgt = s.cellTargets.get((i * 2) % s.cellTargets.size());
                                    double bx = cur.x + (tgt.x - cur.x) * bt;
                                    double by = cur.y + (tgt.y - cur.y) * bt;
                                    double tailT = Math.max(0.0, bt - 0.14);
                                    double tx = cur.x + (tgt.x - cur.x) * tailT;
                                    double ty = cur.y + (tgt.y - cur.y) * tailT;
                                    drawGlow(g, aura, bx, by, 10, 0.68f, false);
                                    g.setComposite(alphaComp(0.84f));
                                    g.setColor(aura);
                                    g.drawLine((int) tx, (int) ty, (int) bx, (int) by);
                                    SpriteAssets.drawVoxelCube(g, (int) Math.round(bx), (int) Math.round(by), 6, Color.WHITE, aura);
                                }
                            }
                        } else if (s.weapon == CombatLoadout.WeaponOption.MISSILE_CROSS && t >= 0.54) {
                            // MIRV Sub-Warhead Fan Split along smooth parabolic arcs toward Cross Cells
                            double st = (t - 0.54) / 0.46;
                            Point2 splitPt = computePosition(s, 0.54);
                            int subW = (int) Math.round(s.cellSize * 0.88);
                            int subH = (int) Math.round(subW * (proj.getHeight() / (double) Math.max(1, proj.getWidth())));
                            for (Point ct : s.cellTargets) {
                                double mx = splitPt.x + (ct.x - splitPt.x) * st;
                                double my = splitPt.y + (ct.y - splitPt.y) * st - Math.sin(st * Math.PI) * 24.0;
                                double prevSt = Math.max(0.0, st - 0.10);
                                double px = splitPt.x + (ct.x - splitPt.x) * prevSt;
                                double py = splitPt.y + (ct.y - splitPt.y) * prevSt - Math.sin(prevSt * Math.PI) * 24.0;
                                double ma = Math.atan2(my - py, mx - px);
                                g.setComposite(alphaComp(0.32f));
                                g.setColor(aura);
                                g.setStroke(roundStroke(5.5f));
                                g.drawLine((int) splitPt.x, (int) splitPt.y, (int) mx, (int) my);
                                g.setComposite(alphaComp(0.75f));
                                g.setColor(Color.WHITE);
                                g.setStroke(roundStroke(1.8f));
                                g.drawLine((int) px, (int) py, (int) mx, (int) my);
                                drawGlow(g, aura, px, py, s.cellSize * 0.38, 0.58f, false);
                                drawRotatedCentered(g, proj, mx, my, subW, subH, ma, 1.0f);
                            }
                        } else {
                            double projScale = s.weapon == CombatLoadout.WeaponOption.CANNON
                                    ? (1.06 + Math.sin(t * Math.PI) * 0.42) : 1.0;
                            int drawW = (int) Math.round(s.cellSize * 1.28 * projScale);
                            int drawH = (int) Math.round(drawW * (proj.getHeight() / (double) Math.max(1, proj.getWidth())));
                            // Directional tail afterburner glow behind missile / cannonball
                            double tailX = cur.x - Math.cos(angle) * drawW * 0.34;
                            double tailY = cur.y - Math.sin(angle) * drawW * 0.34;
                            drawGlow(g, aura, tailX, tailY, s.cellSize * 0.42, 0.64f, false);
                            drawRotatedCentered(g, proj, cur.x, cur.y, drawW, drawH, angle, 1.0f);
                        }
                    }
                }
            }

            // 4.5 Magic Shield Deflection Flares (Voxel Hex barrier + Shield Sprite + Fracture Stress Spikes)
            BufferedImage shieldSprite = SpriteAssets.getMagicShield();
            for (ShieldFlare sf : shieldFlares) {
                double p = Math.max(0.0, Math.min(1.0, (now - sf.startMs) / (double) sf.durationMs));
                float alpha = (float) Math.max(0.0, 1.0 - p);
                int sz = (int) Math.round(sf.radius * (1.28 + p * 0.98));
                drawGlow(g, sf.tint, sf.x, sf.y, sz * 0.86, alpha * 0.64f, false);
                if (shieldSprite != null) {
                    drawRotatedCentered(g, shieldSprite, sf.x, sf.y, sz, sz, p * 0.85, alpha * 0.94f);
                }
                g.setComposite(alphaComp(alpha * 0.92f));
                g.setColor(new Color(0xE0F2FE));
                SpriteAssets.drawVoxelRing(g, sf.x, sf.y, sz * 0.56, sf.damage >= 35 ? 4 : 3, false);

                // Radiating crystal stress fracture bolts upon heavy shield impact
                if (sf.damage >= 18) {
                    int arms = sf.damage >= 38 ? 8 : 5;
                    g.setStroke(roundStroke(2.2f));
                    for (int a = 0; a < arms; a++) {
                        double ang = a * (Math.PI * 2.0 / arms) + 0.35;
                        double r1 = sz * (0.28 + 0.14 * Math.sin(a * 2.3));
                        double r2 = sz * (0.62 + 0.25 * p);
                        int mx = (int) Math.round(sf.x + Math.cos(ang + 0.16) * r1);
                        int my = (int) Math.round(sf.y + Math.sin(ang + 0.16) * r1);
                        int ex = (int) Math.round(sf.x + Math.cos(ang) * r2);
                        int ey = (int) Math.round(sf.y + Math.sin(ang) * r2);
                        g.setColor(sf.damage >= 40 ? new Color(0xF59E0B) : sf.tint);
                        g.drawLine((int) Math.round(sf.x), (int) Math.round(sf.y), mx, my);
                        g.drawLine(mx, my, ex, ey);
                    }
                }

                int[] hx = new int[6], hy = new int[6];
                double hexR = sz * 0.64;
                for (int k = 0; k < 6; k++) {
                    double a = p * 0.5 + k * (Math.PI / 3.0);
                    hx[k] = (int) Math.round(sf.x + Math.cos(a) * hexR);
                    hy[k] = (int) Math.round(sf.y + Math.sin(a) * hexR);
                    SpriteAssets.drawVoxelCube(g, hx[k], hy[k], 4, Color.WHITE, sf.tint);
                }
                g.setColor(new Color(0xBAE6FD));
                g.setStroke(roundStroke(1.5f));
                g.drawPolygon(hx, hy, 6);
            }

            // 4.8 Shattered Magic Shield Crystal Fragments flying across left board!
            for (ShieldShatterAnim anim : shieldShatters) {
                double p = Math.max(0.0, Math.min(1.0, (now - anim.startMs) / (double) anim.durationMs));
                float alpha = (float) Math.max(0.0, 1.0 - p * p);
                if (p < 0.38) {
                    float flashA = (float) Math.sin((p / 0.38) * Math.PI);
                    g.setComposite(alphaComp(flashA * 0.34f));
                    g.setColor(new Color(0xF87171));
                    g.fillRect(anim.gx, anim.gy, anim.gw, anim.gh);
                    // Expanding hexagonal shatter wave ring
                    double ringR = (0.15 + (p / 0.38) * 0.62) * anim.gw;
                    g.setComposite(alphaComp(flashA * 0.88f));
                    g.setColor(new Color(0xBAE6FD));
                    g.setStroke(roundStroke(2.8f));
                    int[] hx = new int[6], hy = new int[6];
                    for (int k = 0; k < 6; k++) {
                        double a = k * Math.PI / 3.0 + p * 0.4;
                        hx[k] = (int) Math.round(anim.center.x + Math.cos(a) * ringR);
                        hy[k] = (int) Math.round(anim.center.y + Math.sin(a) * ringR);
                    }
                    g.drawPolygon(hx, hy, 6);
                    g.setColor(Color.WHITE);
                    g.setStroke(roundStroke(2.2f));
                    for (int k = 0; k < 10; k++) {
                        double ang = k * (Math.PI * 0.2) + 0.15;
                        g.drawLine(anim.center.x, anim.center.y,
                                (int) Math.round(anim.center.x + Math.cos(ang) * anim.gw * 0.54),
                                (int) Math.round(anim.center.y + Math.sin(ang) * anim.gh * 0.54));
                    }
                }
                for (CrystalShard sh : anim.shards) {
                    g.setComposite(alphaComp(alpha * 0.94f));
                    int sz = Math.max(5, sh.size);
                    AffineTransform oldTx = g.getTransform();
                    g.translate(sh.x, sh.y);
                    g.rotate(sh.rot);
                    Polygon diamond = new Polygon(
                            new int[]{0, sz, 0, -sz / 2},
                            new int[]{-sz, 0, sz, sz / 3}, 4);
                    g.setColor(sh.color);
                    g.fillPolygon(diamond);
                    g.setColor(new Color(15, 23, 42, 210));
                    g.setStroke(roundStroke(1.3f));
                    g.drawPolygon(diamond);
                    g.setColor(Color.WHITE);
                    g.drawLine(0, -sz + 1, sz - 2, 0);
                    g.setTransform(oldTx);
                }
            }

            // 5. Floating HUD Impact Callout Toasts (crisp chamfered tactical voxel frame)
            for (FloatingCallout c : callouts) {
                double p = Math.max(0.0, Math.min(1.0, (now - c.startMs) / (double) c.durationMs));
                double ease = 1.0 - Math.pow(1.0 - p, 2.5);
                float alpha = (float) (p < 0.72 ? 1.0 : Math.max(0.0, 1.0 - (p - 0.72) / 0.28));
                double cy = c.y + ease * c.driftY;
                g.setFont(UiTokens.FONT_COORD);
                FontMetrics fm = g.getFontMetrics();
                int tw = fm.stringWidth(c.text);
                int bw = tw + 18;
                int bh = fm.getHeight() + 7;
                int bx = Math.max(10, Math.min(Math.max(10, getWidth() - bw - 10), (int) (c.x - bw / 2.0)));
                int by = Math.max(10, Math.min(Math.max(10, getHeight() - bh - 10), (int) (cy - bh / 2.0)));
                g.setComposite(alphaComp(alpha * 0.46f));
                g.setColor(Color.BLACK);
                SpriteAssets.fillChamferedBox(g, bx + 2, by + 3, bw, bh, 5);
                g.setComposite(alphaComp(alpha * 0.92f));
                g.setColor(new Color(8, 16, 30));
                SpriteAssets.fillChamferedBox(g, bx, by, bw, bh, 5);
                g.setColor(c.color);
                SpriteAssets.drawVoxelChamferBox(g, bx, by, bw, bh, 5, 2);
                g.setComposite(alphaComp(alpha * 0.28f));
                g.setColor(Color.WHITE);
                g.fillRect(bx + 6, by + 2, Math.max(4, bw - 12), 1);
                g.setComposite(alphaComp(alpha));
                g.setColor(Color.WHITE);
                g.drawString(c.text, bx + 9, by + fm.getAscent() + 3);
            }

            // 5.4 Environmental Wind & Wave Surge Sweeps
            for (EnvironmentWaveAnim ew : envWaves) {
                drawEnvironmentWave(g, ew, now);
            }

            // 5.5 Anime Manga Comic Cut-In Slide ("Khiên Phép!") sweeping left-to-right
            for (FleetShieldAnim anim : fleetShields) {
                drawShieldCutInBanner(g, anim, now);
            }

            // 5.6 Dramatic Final-Boss Encounter Cut-In Overlay & Boy-POV City Rampage
            for (BossIntroAnim anim : bossIntros) {
                drawBossIntroOverlay(g, anim, now);
            }
            for (BossCityRampageAnim br : bossRampages) {
                drawBossCityRampageCutscene(g, br, now);
            }

            // 5.7 Super Ship Hyperion Nova Beam Strikes
            for (HyperionBeamAnim hb : hyperionBeams) {
                drawHyperionBeam(g, hb, now);
            }

            // 5.8 4-Act Humanity Last-Hope Super Ship Cinema Cutscene
            for (SuperShipCutsceneAnim sc : superCutscenes) {
                drawSuperShipCutscene(g, sc, now);
            }
        }

        g.dispose();
    }

    private void drawTorpedoHydroWake(Graphics2D g, ActiveStrike s) {
        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        int n = s.history.size();
        for (int i = 1; i < n; i++) {
            TrailSample a = s.history.get(i - 1);
            TrailSample b = s.history.get(i);
            double dx = b.x - a.x;
            double dy = b.y - a.y;
            double len = Math.max(1.0, Math.hypot(dx, dy));
            double nx = -dy / len;
            double ny = dx / len;
            float frac = i / (float) n;
            double spread = (1.0 - frac) * 15.0 + 3.0;
            // Soft churning outer wake
            g.setComposite(alphaComp(frac * 0.24f));
            g.setColor(new Color(0x38BDF8));
            g.setStroke(roundStroke(4.5f));
            g.drawLine((int) (a.x + nx * spread), (int) (a.y + ny * spread),
                    (int) (b.x + nx * spread * 0.72), (int) (b.y + ny * spread * 0.72));
            g.drawLine((int) (a.x - nx * spread), (int) (a.y - ny * spread),
                    (int) (b.x - nx * spread * 0.72), (int) (b.y - ny * spread * 0.72));
            // Crisp white foam crests
            g.setComposite(alphaComp(frac * 0.62f));
            g.setColor(new Color(0xE0F2FE));
            g.setStroke(roundStroke(2.0f));
            g.drawLine((int) (a.x + nx * spread), (int) (a.y + ny * spread),
                    (int) (b.x + nx * spread * 0.72), (int) (b.y + ny * spread * 0.72));
            g.drawLine((int) (a.x - nx * spread), (int) (a.y - ny * spread),
                    (int) (b.x - nx * spread * 0.72), (int) (b.y - ny * spread * 0.72));
        }
        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private void drawTitanSkillChoreography(Graphics2D g, ActiveStrike s, double t, long now) {
        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        if (s.titanSkill == TitanMode.TitanSkill.GZ_ATOMIC_BREATH || s.titanSkill == TitanMode.TitanSkill.GZ_SPIRAL_RAY) {
            boolean spiral = s.titanSkill == TitanMode.TitanSkill.GZ_SPIRAL_RAY;
            Color outer = spiral ? new Color(0xB91C1C) : (s.titanEnraged ? new Color(0xEA580C) : new Color(0x0284C7));
            Color mid = spiral ? new Color(0xEF4444) : (s.titanEnraged ? new Color(0xFB923C) : new Color(0x22D3EE));
            Color hotCore = spiral ? new Color(0xFEF08A) : (s.titanEnraged ? new Color(0xFEF08A) : new Color(0xECFEFF));

            // Phase 1 (t in [0.00, 0.42]): Movie-accurate Sequential Dorsal Spine Ignition + Jaw Singularity!
            int dorsalCount = s.extraOrigins.size();
            double dorsalAlphaScale = t < 0.42 ? 1.0 : Math.max(0.0, 1.0 - (t - 0.42) / 0.28);
            if (dorsalAlphaScale > 0.02 && dorsalCount > 0) {
                for (int i = 0; i < dorsalCount; i++) {
                    double startT = i * 0.075;
                    if (t < startT) continue;
                    double nodeP = Math.min(1.0, (t - startT) / 0.14);
                    Point dp = s.extraOrigins.get(i);
                    double pulse = 0.88 + 0.18 * Math.sin(now * 0.04 + i);
                    double rad = s.cellSize * (0.95 + nodeP * 0.72) * pulse;
                    drawGlow(g, mid, dp.x, dp.y, rad, (float) (0.76 * nodeP * dorsalAlphaScale), true);
                    g.setComposite(alphaComp(0.88 * nodeP * dorsalAlphaScale));
                    g.setColor(Color.WHITE);
                    SpriteAssets.drawVoxelRing(g, dp.x, dp.y, s.cellSize * (0.35 + nodeP * 0.40), 3, true);
                    SpriteAssets.drawVoxelCube(g, dp.x, dp.y, 6, Color.WHITE, mid);

                    Point nextPt = (i + 1 < dorsalCount) ? s.extraOrigins.get(i + 1) : s.origin;
                    if (t >= startT + 0.04) {
                        drawJaggedEnergyArc(g, dp.x, dp.y, nextPt.x, nextPt.y, mid, hotCore,
                                (float) (0.85 * dorsalAlphaScale), now + i * 37L);
                    }
                }
            }

            if (t < 0.44) {
                double mP = Math.min(1.0, Math.max(0.0, t / 0.38));
                double mRad = s.cellSize * (0.45 + Math.sin(mP * Math.PI * 0.5) * 1.15);
                drawGlow(g, mid, s.origin.x, s.origin.y, mRad * 1.6, (float) (0.88 * mP), false);
                g.setComposite(alphaComp(0.95 * mP));
                g.setColor(hotCore);
                SpriteAssets.drawVoxelRing(g, s.origin.x, s.origin.y, mRad * 0.65, 3, true);
                SpriteAssets.drawVoxelCube(g, s.origin.x, s.origin.y, Math.max(6, (int) (mRad * 0.36)), Color.WHITE, mid);
            }

            // Phase 2 (t >= 0.38): Volumetric 4-layer Cherenkov / Crimson Spiral Atomic Laser Beam sweeping target!
            if (t >= 0.38) {
                Point2 tip = computeAtomicBeamTip(s, Math.min(1.0, t));
                float beamAlpha = t < 1.0 ? 1.0f : (float) Math.max(0.0, 1.0 - (t - 1.0) / 0.28);
                double pulse = 0.90 + 0.20 * Math.sin(now * 0.055);
                int tx = (int) Math.round(tip.x);
                int ty = (int) Math.round(tip.y);

                drawGlow(g, mid, s.origin.x, s.origin.y, s.cellSize * 1.95 * pulse, 0.88f * beamAlpha, true);

                g.setComposite(alphaComp(0.32f * beamAlpha));
                g.setColor(outer);
                g.setStroke(roundStroke((float) ((spiral ? 34.0 : 30.0) * pulse)));
                g.drawLine(s.origin.x, s.origin.y, tx, ty);

                g.setComposite(alphaComp(0.86f * beamAlpha));
                g.setColor(mid);
                g.setStroke(roundStroke((float) (15.5 * pulse)));
                g.drawLine(s.origin.x, s.origin.y, tx, ty);

                g.setComposite(alphaComp(0.92f * beamAlpha));
                g.setColor(hotCore);
                g.setStroke(roundStroke((float) (8.0 * pulse)));
                g.drawLine(s.origin.x, s.origin.y, tx, ty);

                g.setComposite(alphaComp(0.98f * beamAlpha));
                g.setColor(Color.WHITE);
                g.setStroke(roundStroke((float) (3.8 * pulse)));
                g.drawLine(s.origin.x, s.origin.y, tx, ty);

                double dx = tip.x - s.origin.x;
                double dy = tip.y - s.origin.y;
                double dist = Math.max(1.0, Math.hypot(dx, dy));
                double nx = -dy / dist;
                double ny = dx / dist;

                if (spiral) {
                    // Counter-rotating Golden & Crimson Spiral Plasma Coils twisting tightly down the Laser Lance!
                    Path2D helixA = new Path2D.Double();
                    Path2D helixB = new Path2D.Double();
                    int segs = 38;
                    for (int k = 0; k <= segs; k++) {
                        double u = k / (double) segs;
                        double amp = 22.0 * Math.sin(u * Math.PI * 0.88 + 0.25) + 8.0;
                        double phaseAng = u * 22.0 - now * 0.026;
                        double offA = Math.sin(phaseAng) * amp;
                        double offB = -offA;
                        double hxA = s.origin.x + dx * u + nx * offA;
                        double hyA = s.origin.y + dy * u + ny * offA;
                        double hxB = s.origin.x + dx * u + nx * offB;
                        double hyB = s.origin.y + dy * u + ny * offB;
                        if (k == 0) {
                            helixA.moveTo(hxA, hyA);
                            helixB.moveTo(hxB, hyB);
                        } else {
                            helixA.lineTo(hxA, hyA);
                            helixB.lineTo(hxB, hyB);
                        }
                    }
                    g.setComposite(alphaComp(0.90f * beamAlpha));
                    g.setColor(new Color(0xFACC15));
                    g.setStroke(roundStroke(4.4f));
                    g.draw(helixA);
                    g.setColor(new Color(0xFB7185));
                    g.setStroke(roundStroke(3.6f));
                    g.draw(helixB);
                } else {
                    double phase = (now * 0.008) % 1.0;
                    for (int r = 0; r < 6; r++) {
                        double f = (r / 6.0 + phase / 6.0);
                        if (f > 0.96) continue;
                        int rx = (int) Math.round(s.origin.x + dx * f);
                        int ry = (int) Math.round(s.origin.y + dy * f);
                        g.setComposite(alphaComp(0.82f * beamAlpha));
                        g.setColor(mid);
                        SpriteAssets.drawVoxelRing(g, rx, ry, s.cellSize * (0.34 + 0.16 * f), 3, true);
                        double off = Math.sin(f * 20.0 - now * 0.032) * 14.0;
                        SpriteAssets.drawVoxelCube(g, (int) Math.round(rx + nx * off), (int) Math.round(ry + ny * off), 5, Color.WHITE, mid);
                    }
                }

                drawGlow(g, mid, tip.x, tip.y, s.cellSize * 1.90 * pulse, 0.88f * beamAlpha, false);
                g.setComposite(alphaComp(0.85f * beamAlpha));
                g.setColor(Color.WHITE);
                SpriteAssets.drawVoxelRing(g, tx, ty, s.cellSize * (0.55 + 0.35 * (pulse - 0.8)), 3, true);
                if (t >= 0.50 && s.cellTargets.size() > 1) {
                    double sweep = Math.min(1.0, (t - 0.50) / 0.50);
                    int lit = Math.min(s.cellTargets.size(), 1 + (int) Math.floor(sweep * (s.cellTargets.size() - 1)));
                    for (int ci = 0; ci < lit; ci++) {
                        Point cp = s.cellTargets.get(ci);
                        drawGlow(g, mid, cp.x, cp.y, s.cellSize * 0.95, 0.48f * beamAlpha, false);
                    }
                }
            }
        } else if (s.titanSkill == TitanMode.TitanSkill.GZ_MELTDOWN_RIFT) {
            // Blazing magma core fissure lines snapping from Burning Godzilla into 5x5 caldera star + magma geysers!
            float alpha = (float) Math.max(0.0, 1.0 - Math.max(0.0, (t - 0.82) / 0.46));
            double reach = Math.min(1.0, t / 0.72);
            double ex = s.origin.x + (s.target.x - s.origin.x) * reach;
            double ey = s.origin.y + (s.target.y - s.origin.y) * reach;
            drawJaggedEnergyArc(g, s.origin.x, s.origin.y, ex, ey,
                    new Color(0xEA580C), new Color(0xFEF08A), alpha * 0.94f, now);
            if (t >= 0.35) {
                double starP = Math.min(1.0, (t - 0.35) / 0.55);
                for (Point pt : s.cellTargets) {
                    int fx = (int) Math.round(s.target.x + (pt.x - s.target.x) * starP);
                    int fy = (int) Math.round(s.target.y + (pt.y - s.target.y) * starP);
                    drawJaggedEnergyArc(g, s.target.x, s.target.y, fx, fy,
                            new Color(0xEF4444), new Color(0xFACC15), alpha * 0.85f, now + pt.x);
                    if (t >= 0.62) {
                        double plumeH = s.cellSize * (0.45 + Math.sin(Math.min(1.0, (t - 0.62) / 0.55) * Math.PI) * 1.9);
                        g.setComposite(alphaComp(0.75f * alpha));
                        g.setColor(new Color(0xF97316));
                        g.fillRect(pt.x - s.cellSize / 5, (int) (pt.y - plumeH), Math.max(6, (s.cellSize * 2) / 5), (int) plumeH);
                        g.setColor(new Color(0xFEF08A));
                        g.fillRect(pt.x - Math.max(2, s.cellSize / 10), (int) (pt.y - plumeH * 0.85), Math.max(3, s.cellSize / 5), (int) (plumeH * 0.8));
                    }
                }
            }
        } else if (s.titanSkill == TitanMode.TitanSkill.KG_ALPHA_STORM && s.extraOrigins.size() >= 3) {
            // Phase 2 Alpha Storm: 3 heads unite lightning into a storm apex, then slam down onto all 7 cells!
            float alpha = (float) Math.max(0.0, 1.0 - Math.max(0.0, (t - 0.82) / 0.45));
            Point apex = new Point(s.target.x, (int) Math.round(s.target.y - s.cellSize * 2.1));
            double gatherP = Math.min(1.0, t / 0.45);
            for (int h = 0; h < 3; h++) {
                Point ho = s.extraOrigins.get(h);
                double ax = ho.x + (apex.x - ho.x) * gatherP;
                double ay = ho.y + (apex.y - ho.y) * gatherP;
                drawJaggedEnergyArc(g, ho.x, ho.y, ax, ay,
                        h == 1 ? new Color(0xA855F7) : new Color(0xFACC15), Color.WHITE, alpha * 0.92f, now + h * 19L);
            }
            if (t >= 0.42) {
                double dropP = Math.min(1.0, (t - 0.42) / 0.45);
                for (int ci = 0; ci < s.cellTargets.size(); ci++) {
                    Point ct = s.cellTargets.get(ci);
                    double cx2 = apex.x + (ct.x - apex.x) * dropP;
                    double cy2 = apex.y + (ct.y - apex.y) * dropP;
                    drawJaggedEnergyArc(g, apex.x, apex.y, cx2, cy2,
                            (ci % 2 == 0) ? new Color(0xF59E0B) : new Color(0xC084FC), Color.WHITE, alpha * 0.95f, now + ci * 29L);
                }
            }
        } else if (s.titanSkill == TitanMode.TitanSkill.KG_CATACLYSM_GRAVITY) {
            // Phase 2 Cataclysm Gravity: rotating black-hole accretion ring above board hurling 9 orbital meteors!
            float alpha = (float) Math.max(0.0, 1.0 - Math.max(0.0, (t - 0.82) / 0.46));
            int holeY = (int) Math.round(s.target.y - s.cellSize * 2.25);
            double holeR = s.cellSize * (0.95 + 0.65 * Math.sin(Math.min(1.0, t) * Math.PI));
            drawGlow(g, new Color(0x9333EA), s.target.x, holeY, holeR * 1.85, 0.80f * alpha, true);
            g.setComposite(alphaComp(0.90f * alpha));
            g.setColor(new Color(0x1E1B4B));
            g.fillOval((int) Math.round(s.target.x - holeR * 0.55), (int) Math.round(holeY - holeR * 0.55),
                    (int) Math.round(holeR * 1.1), (int) Math.round(holeR * 1.1));
            g.setColor(new Color(0xFACC15));
            SpriteAssets.drawVoxelRing(g, s.target.x, holeY, holeR, 4, true);
            g.setColor(new Color(0xC084FC));
            SpriteAssets.drawVoxelRing(g, s.target.x, holeY, holeR * 0.72, 3, true);
            if (t >= 0.25) {
                double mP = Math.min(1.0, (t - 0.25) / 0.72);
                for (int ci = 0; ci < s.cellTargets.size(); ci++) {
                    Point ct = s.cellTargets.get(ci);
                    double mx = s.target.x + (ct.x - s.target.x) * mP;
                    double my = holeY + (ct.y - holeY) * mP;
                    g.setComposite(alphaComp(0.84f * alpha));
                    g.setColor(ci % 2 == 0 ? new Color(0xFACC15) : new Color(0xC084FC));
                    g.setStroke(roundStroke(3.0f));
                    g.drawLine((int) Math.round(s.target.x), holeY, (int) Math.round(mx), (int) Math.round(my));
                    SpriteAssets.drawVoxelCube(g, (int) Math.round(mx), (int) Math.round(my), 6,
                            Color.WHITE, ci % 2 == 0 ? new Color(0xF59E0B) : new Color(0xA855F7));
                }
            }
        } else if (s.titanSkill == TitanMode.TitanSkill.GZ_TAIL_SLAM) {
            // Giant tsunami wave & ocean shadow telegraph beneath incoming Kaiju tail whip
            if (t >= 0.65) {
                double slamP = Math.min(1.0, (t - 0.65) / 0.63);
                float waveAlpha = (float) Math.max(0.0, 1.0 - Math.max(0.0, (t - 1.0) / 0.28));
                for (Point pt : s.cellTargets) {
                    double plumeH = s.cellSize * (0.6 + Math.sin(Math.min(1.0, slamP) * Math.PI * 0.85) * 2.35);
                    g.setComposite(alphaComp(0.70f * waveAlpha));
                    g.setColor(new Color(0x38BDF8));
                    g.fillRoundRect((int) (pt.x - s.cellSize * 0.28), (int) (pt.y - plumeH),
                            (int) (s.cellSize * 0.56), (int) plumeH, 8, 8);
                    g.setColor(Color.WHITE);
                    g.fillRect((int) (pt.x - s.cellSize * 0.16), (int) (pt.y - plumeH * 0.88),
                            Math.max(4, (int) (s.cellSize * 0.32)), (int) (plumeH * 0.82));
                }
            }
        } else if (s.titanSkill == TitanMode.TitanSkill.KG_TRIPLE_GRAVITY && s.extraOrigins.size() >= 3) {
            // 3 Jagged Golden Gravity Lightning Streams fired from King Ghidorah's 3 Heads
            double reach = Math.min(1.0, t / 0.85);
            float alpha = t < 1.0 ? 0.96f : (float) Math.max(0.0, 1.0 - (t - 1.0) / 0.28);
            int nTgts = Math.max(1, s.cellTargets.size());
            Color gold = new Color(0xFACC15);
            for (int h = 0; h < 3; h++) {
                Point ho = s.extraOrigins.get(h);
                Point ht = s.cellTargets.get((h * 2) % nTgts);
                double ex = ho.x + (ht.x - ho.x) * reach;
                double ey = ho.y + (ht.y - ho.y) * reach;
                double dx = ex - ho.x;
                double dy = ey - ho.y;
                double len = Math.max(1.0, Math.hypot(dx, dy));
                double nx = -dy / len;
                double ny = dx / len;

                Path2D p = new Path2D.Double();
                p.moveTo(ho.x, ho.y);
                int segs = 10;
                for (int k = 1; k < segs; k++) {
                    double frac = k / (double) segs;
                    double wave = Math.sin(frac * 18.0 + now * 0.03 + h * 2.1) * 9.5;
                    p.lineTo(ho.x + dx * frac + nx * wave, ho.y + dy * frac + ny * wave);
                }
                p.lineTo(ex, ey);

                g.setComposite(alphaComp(0.32f * alpha));
                g.setColor(gold);
                g.setStroke(roundStroke(10.0f));
                g.draw(p);
                g.setComposite(alphaComp(0.84f * alpha));
                g.setColor(new Color(0xFDE047));
                g.setStroke(roundStroke(4.2f));
                g.draw(p);
                g.setComposite(alphaComp(alpha));
                g.setColor(Color.WHITE);
                g.setStroke(roundStroke(1.8f));
                g.draw(p);
            }
        } else if (s.titanSkill == TitanMode.TitanSkill.GZ_THERMO_PULSE) {
            // Burning Godzilla Supernova Thermonuclear Shockwave Wall sweeping across arena into 3x3 target zone!
            float alpha = (float) Math.max(0.0, 1.0 - Math.max(0.0, (t - 0.78) / 0.50));
            double span = Math.max(s.cellSize * 5.0, s.origin.distance(s.target) * 1.08);
            double waveFrac = Math.max(0.0, Math.min(1.0, (t - 0.22) / 0.78));
            double rad = s.cellSize * 0.8 + waveFrac * span;
            if (t >= 0.20) {
                g.setComposite(alphaComp(0.26f * alpha));
                g.setColor(new Color(0xEA580C));
                g.setStroke(roundStroke(18.0f));
                g.drawOval((int) Math.round(s.origin.x - rad), (int) Math.round(s.origin.y - rad),
                        (int) Math.round(rad * 2.0), (int) Math.round(rad * 2.0));
                g.setComposite(alphaComp(0.82f * alpha));
                g.setColor(new Color(0xFB923C));
                SpriteAssets.drawVoxelRing(g, s.origin.x, s.origin.y, rad, 5, false);
                g.setComposite(alphaComp(0.68f * alpha));
                g.setColor(new Color(0xFDE047));
                SpriteAssets.drawVoxelRing(g, s.origin.x, s.origin.y, rad * 0.74, 4, true);
                // Solar blast expanding right onto the target center too
                double tgtR = s.cellSize * (0.4 + Math.max(0.0, (t - 0.65) / 0.45) * 2.6);
                g.setColor(new Color(0xF97316));
                SpriteAssets.drawVoxelRing(g, s.target.x, s.target.y, tgtR, 4, true);
            }
        } else if (s.titanSkill == TitanMode.TitanSkill.MT_GOD_RAY) {
            // Celestial Golden-Emerald God Ray parting sky & piercing the 1x5 target corridor!
            float alpha = (float) Math.max(0.0, 1.0 - Math.max(0.0, (t - 0.80) / 0.46));
            double reach = Math.min(1.0, t / 0.42);
            int skyY = Math.max(0, s.target.y - s.cellSize * 8);
            double curY = skyY + (s.target.y - skyY) * reach;
            drawGlow(g, new Color(0xFDE047), s.target.x, skyY + 18, s.cellSize * 2.2, 0.75f * alpha, true);
            g.setComposite(alphaComp(0.32f * alpha));
            g.setColor(new Color(0xFDE047));
            g.setStroke(roundStroke(28.0f));
            g.drawLine(s.target.x, skyY, s.target.x, (int) Math.round(curY));
            g.setComposite(alphaComp(0.74f * alpha));
            g.setColor(new Color(0xA7F3D0));
            g.setStroke(roundStroke(12.0f));
            g.drawLine(s.target.x, skyY, s.target.x, (int) Math.round(curY));
            g.setComposite(alphaComp(0.96f * alpha));
            g.setColor(Color.WHITE);
            g.setStroke(roundStroke(4.5f));
            g.drawLine(s.target.x, skyY, s.target.x, (int) Math.round(curY));
            if (t >= 0.35) {
                for (Point pt : s.cellTargets) {
                    drawGlow(g, new Color(0xFDE047), pt.x, pt.y, s.cellSize * 0.92, 0.72f * alpha, true);
                    g.setComposite(alphaComp(0.55f * alpha));
                    g.setColor(new Color(0xFEF08A));
                    g.setStroke(roundStroke(3.2f));
                    g.drawLine(pt.x, Math.max(0, pt.y - s.cellSize * 5), pt.x, pt.y);
                }
            }
        } else if (s.titanSkill == TitanMode.TitanSkill.MT_WING_GALE) {
            // Twin crescent wind pressure waves swept from Mothra's wings into the player sector
            float alpha = (float) Math.max(0.0, 1.0 - Math.max(0.0, (t - 0.74) / 0.48));
            for (int wv = 0; wv < 2; wv++) {
                double wt = Math.max(0.0, Math.min(1.0, (t - wv * 0.16) / 0.78));
                double ax = s.origin.x + (s.target.x - s.origin.x) * wt;
                double ay = s.origin.y + (s.target.y - s.origin.y) * wt;
                double rad = s.cellSize * (1.2 + wt * 2.2);
                g.setComposite(alphaComp(0.70f * alpha));
                g.setColor(wv == 0 ? new Color(0x2DD4BF) : new Color(0xBAE6FD));
                g.setStroke(roundStroke(wv == 0 ? 7.5f : 4.0f));
                g.drawArc((int) Math.round(ax - rad), (int) Math.round(ay - rad),
                        (int) Math.round(rad * 2.0), (int) Math.round(rad * 2.0), 120, 120);
            }
        } else if (s.titanSkill == TitanMode.TitanSkill.MT_SILK_WEB) {
            // Glowing astral silk tether strands lattice-wrapping the 3x3 target grid
            float alpha = (float) Math.max(0.0, 1.0 - Math.max(0.0, (t - 0.78) / 0.48));
            double reach = Math.min(1.0, t / 0.62);
            g.setComposite(alphaComp(0.84f * alpha));
            for (int i = 0; i < s.cellTargets.size(); i++) {
                Point pt = s.cellTargets.get(i);
                int ex = (int) Math.round(s.origin.x + (pt.x - s.origin.x) * reach);
                int ey = (int) Math.round(s.origin.y + (pt.y - s.origin.y) * reach);
                g.setColor(i % 2 == 0 ? new Color(0xA7F3D0) : new Color(0xFEF08A));
                g.setStroke(roundStroke(2.2f));
                g.drawLine(s.origin.x, s.origin.y, ex, ey);
                if (t >= 0.55) {
                    Point next = s.cellTargets.get((i + 2) % s.cellTargets.size());
                    g.setColor(new Color(0xECFDF5));
                    g.setStroke(roundStroke(1.8f));
                    g.drawLine(pt.x, pt.y, next.x, next.y);
                }
            }
        } else if (s.titanSkill == TitanMode.TitanSkill.MT_PRISM_RAIN) {
            // Prismatic rainbow wing-pollen crystal spears raining onto all 7 cells
            float alpha = (float) Math.max(0.0, 1.0 - Math.max(0.0, (t - 0.80) / 0.45));
            Color[] prismCols = {new Color(0x38BDF8), new Color(0xFDE047), new Color(0xF472B6), new Color(0x34D399)};
            double fallP = Math.min(1.0, t / 0.78);
            for (int i = 0; i < s.cellTargets.size(); i++) {
                Point pt = s.cellTargets.get(i);
                int topY = Math.max(0, pt.y - s.cellSize * 4);
                int py = (int) Math.round(topY + (pt.y - topY) * fallP);
                Color col = prismCols[i % prismCols.length];
                drawGlow(g, col, pt.x, py, s.cellSize * 0.58, 0.72f * alpha, false);
                g.setComposite(alphaComp(0.88f * alpha));
                g.setColor(col);
                g.setStroke(roundStroke(3.2f));
                g.drawLine(pt.x, Math.max(topY, py - s.cellSize), pt.x, py);
                SpriteAssets.drawVoxelCube(g, pt.x, py, 5, Color.WHITE, col);
            }
        } else if (s.titanSkill == TitanMode.TitanSkill.MT_SOLAR_SUPERNOVA) {
            // Phase 2 Ultimate: Solar Mothra blazing sun-core expanding across whole arena!
            float alpha = (float) Math.max(0.0, 1.0 - Math.max(0.0, (t - 0.82) / 0.46));
            double expand = Math.max(0.0, Math.min(1.0, (t - 0.16) / 0.76));
            double span = s.origin.distance(s.target) * 0.96;
            double rad = s.cellSize * 0.9 + expand * span;
            drawGlow(g, new Color(0xFB7185), s.origin.x, s.origin.y, s.cellSize * (1.8 + expand * 2.4), 0.78f * alpha, true);
            drawGlow(g, new Color(0xFDE047), s.target.x, s.target.y, s.cellSize * (0.9 + expand * 3.0), 0.76f * alpha, true);
            g.setComposite(alphaComp(0.82f * alpha));
            g.setColor(new Color(0xFDE047));
            SpriteAssets.drawVoxelRing(g, s.origin.x, s.origin.y, rad, 5, true);
            g.setColor(new Color(0xFB7185));
            SpriteAssets.drawVoxelRing(g, s.target.x, s.target.y, s.cellSize * (0.6 + expand * 2.8), 4, true);
            for (Point pt : s.cellTargets) {
                int rx = (int) Math.round(s.target.x + (pt.x - s.target.x) * expand);
                int ry = (int) Math.round(s.target.y + (pt.y - s.target.y) * expand);
                g.setComposite(alphaComp(0.85f * alpha));
                g.setColor(new Color(0xFEF08A));
                g.setStroke(roundStroke(2.6f));
                g.drawLine(s.target.x, s.target.y, rx, ry);
            }
        }
        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private static void drawJaggedEnergyArc(Graphics2D g, double x1, double y1, double x2, double y2,
                                            Color glowCol, Color coreCol, float alpha, long seed) {
        Path2D arc = new Path2D.Double();
        arc.moveTo(x1, y1);
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.max(1.0, Math.hypot(dx, dy));
        double nx = -dy / len;
        double ny = dx / len;
        int segs = 5;
        for (int k = 1; k < segs; k++) {
            double u = k / (double) segs;
            double jitter = Math.sin(seed * 0.13 + k * 2.7) * 10.0;
            arc.lineTo(x1 + dx * u + nx * jitter, y1 + dy * u + ny * jitter);
        }
        arc.lineTo(x2, y2);
        g.setComposite(alphaComp(0.42 * alpha));
        g.setColor(glowCol);
        g.setStroke(roundStroke(7.0f));
        g.draw(arc);
        g.setComposite(alphaComp(0.95 * alpha));
        g.setColor(coreCol);
        g.setStroke(roundStroke(2.2f));
        g.draw(arc);
    }

    private void drawBossIntroOverlay(Graphics2D g, BossIntroAnim anim, long now) {
        double t = Math.max(0.0, Math.min(1.0, (now - anim.startMs) / (double) anim.durationMs));
        if (t <= 0.0 || t >= 1.0) return;

        int W = getWidth();
        int H = getHeight();
        if (W < 320 || H < 240) return;

        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        Shape prevClip = g.getClip();
        Object prevAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float envAlpha;
        if (t < 0.12) envAlpha = (float) (t / 0.12);
        else if (t < 0.84) envAlpha = 1.0f;
        else envAlpha = (float) Math.max(0.0, 1.0 - (t - 0.84) / 0.16);

        Color bossCol = anim.enrageTransform ? anim.boss.enrageColor : anim.boss.accentColor;
        Color warnRed = anim.enrageTransform ? new Color(0xF97316) : new Color(0xEF4444);
        Color warnAmber = new Color(0xFACC15);

        // 1. Theatrical Dark Threat Vignette + Pulsing Red/Boss Alert Frame
        float strobe = (float) (0.55 + 0.45 * Math.sin(t * Math.PI * 10.0));
        g.setComposite(alphaComp((anim.enrageTransform ? 0.52f : 0.46f) * envAlpha));
        g.setColor(anim.enrageTransform ? new Color(18, 4, 12) : new Color(2, 6, 16));
        g.fillRect(0, 0, W, H);
        g.setComposite(alphaComp((0.26f + 0.18f * strobe) * envAlpha));
        g.setColor(anim.enrageTransform ? bossCol : warnRed);
        g.setStroke(roundStroke(10.0f));
        g.drawRect(5, 5, W - 10, H - 10);

        // 2. Top & Bottom Scrolling Hazard Warning Tapes ("CẢNH BÁO · BOSS TỐI THƯỢNG" / "PHASE 2 CUỒNG BẠO")
        int tapeH = Math.max(24, Math.min(32, H / 26));
        int topY = (int) Math.round(t < 0.14 ? (-tapeH * (1.0 - t / 0.14)) : 0.0);
        int botY = H - tapeH + (int) Math.round(t < 0.14 ? (tapeH * (1.0 - t / 0.14)) : 0.0);
        int scrollPx = (int) Math.round(t * 260.0);
        drawWarningTapeBar(g, 0, topY, W, tapeH, scrollPx, warnRed, warnAmber, envAlpha,
                anim.enrageTransform
                        ? "BÁO ĐỘNG ĐỎ · PHASE 2 CUỒNG BẠO THỨC TỈNH · MỞ KHÓA 5 KỸ NĂNG HỦY DIỆT"
                        : "CẢNH BÁO · ĐẠI HỌA TITAN CẤP Ω · PHÁT HIỆN THỰC THỂ HỦY DIỆT");
        drawWarningTapeBar(g, 0, botY, W, tapeH, -scrollPx, bossCol, warnAmber, envAlpha,
                anim.enrageTransform
                        ? switch (anim.boss) {
                            case GODZILLA -> "BURNING GODZILLA · TIA XOẮN ĐỎ & VỰC DUNG NHAM ĐÃ SẴN SÀNG";
                            case GHIDORAH -> "SUPERCELL GHIDORAH · SIÊU BÃO SÉT & HỐ ĐEN TRỌNG LỰC ĐÃ MỞ";
                            case MOTHRA -> "SOLAR MOTHRA · MƯA PHẤN LĂNG KÍNH & SIÊU TÂN TINH Ω ĐÃ MỞ";
                        }
                        : "BOSS CUỐI: " + anim.boss.displayName.toUpperCase() + " · ĐIỂM YẾU ×2 ST");

        // 3. Dramatic Slanted Manga/Cinema Boss Title Cut-In Card slamming from Right to Center!
        if (t >= 0.14 && t <= 0.90) {
            double cP = (t - 0.14) / 0.76;
            int cardW = Math.max(480, Math.min(720, (int) Math.round(W * 0.72)));
            int cardH = Math.max(150, Math.min(196, (int) Math.round(H * 0.26)));
            int skew = Math.max(28, (int) Math.round(cardH * 0.24));
            int anchorY = (H - cardH) / 2;

            double xStart = W + skew + 40.0;
            double xCenter = (W - cardW) * 0.5;
            double xEnd = -cardW - skew - 40.0;
            double cx;
            double cy = anchorY;
            float cardAlpha;
            if (cP < 0.22) {
                double u = cP / 0.22;
                double ease = 1.0 - Math.pow(1.0 - u, 2.8);
                cx = xStart + (xCenter + 18.0 - xStart) * ease;
                cardAlpha = (float) Math.min(1.0, u * 2.4);
            } else if (cP < 0.80) {
                double u = (cP - 0.22) / 0.58;
                cx = xCenter + 18.0 - u * 36.0;
                double tremor = (u < 0.18) ? Math.sin(u * 110.0) * 4.5 : 0.0;
                cy += tremor;
                cardAlpha = 1.0f;
            } else {
                double u = (cP - 0.80) / 0.20;
                double ease = u * u * u;
                cx = (xCenter - 18.0) + (xEnd - (xCenter - 18.0)) * ease;
                cardAlpha = (float) Math.max(0.0, 1.0 - u * 1.15);
            }

            if (cardAlpha > 0.02f) {
                int ix = (int) Math.round(cx);
                int iy = (int) Math.round(cy);

                Polygon cardPoly = new Polygon(
                        new int[]{ix + skew, ix + cardW + skew, ix + cardW, ix},
                        new int[]{iy, iy, iy + cardH, iy + cardH},
                        4);
                Polygon shadowPoly = new Polygon(
                        new int[]{ix + skew + 10, ix + cardW + skew + 10, ix + cardW + 10, ix + 10},
                        new int[]{iy + 12, iy + 12, iy + cardH + 12, iy + cardH + 12},
                        4);

                g.setComposite(alphaComp(0.68f * cardAlpha));
                g.setColor(Color.BLACK);
                g.fillPolygon(shadowPoly);

                g.setComposite(alphaComp(0.45f * cardAlpha));
                g.setColor(bossCol);
                g.setStroke(roundStroke(11.0f));
                g.drawPolygon(cardPoly);

                g.setClip(cardPoly);
                g.setComposite(alphaComp(0.95f * cardAlpha));
                g.setPaint(new GradientPaint(ix, iy,
                        anim.enrageTransform ? new Color(28, 8, 18) : new Color(10, 16, 30),
                        ix + cardW, iy + cardH,
                        switch (anim.boss) {
                            case GODZILLA -> anim.enrageTransform ? new Color(84, 16, 10) : new Color(8, 34, 62);
                            case GHIDORAH -> anim.enrageTransform ? new Color(54, 14, 72) : new Color(45, 28, 6);
                            case MOTHRA -> anim.enrageTransform ? new Color(68, 42, 8) : new Color(8, 44, 38);
                        }));
                g.fillPolygon(cardPoly);

                // Manga speed slash diagonals inside cut-in frame
                g.setComposite(alphaComp(0.24f * cardAlpha));
                g.setColor(bossCol);
                for (int sl = -3; sl <= 10; sl++) {
                    int sx1 = ix + sl * 68;
                    g.setStroke(roundStroke(sl % 2 == 0 ? 4.0f : 2.0f));
                    g.drawLine(sx1, iy + cardH, sx1 + skew + 90, iy);
                }

                // Giant close-up Voxel Titan Boss Portrait on the right side of the cut-in frame!
                BufferedImage bossPortrait = SpriteAssets.getTitanSprite(anim.boss, true, anim.enrageTransform);
                if (bossPortrait != null) {
                    int pSz = (int) Math.round(cardH * 1.24);
                    int px = ix + cardW - pSz - 8;
                    int py = iy + (cardH - pSz) / 2 + 8;
                    drawGlow(g, bossCol, px + pSz * 0.5, py + pSz * 0.45, pSz * 0.65, 0.72f * cardAlpha, true);
                    g.setComposite(alphaComp(cardAlpha));
                    SpriteAssets.drawSharp(g, bossPortrait, px, py, pSz, pSz);
                }

                // Bold Boss Encounter / Phase 2 Transformation Typography on left side
                int txBase = ix + skew + 24;
                g.setComposite(alphaComp(cardAlpha));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                g.setColor(warnRed);
                g.drawString(anim.enrageTransform
                        ? "PHASE 2 · CUỒNG BẠO THỨC TỈNH!"
                        : "THỰC THỂ BOSS CUỐI XUẤT HIỆN", txBase, iy + 32);

                int titleSz = Math.max(24, Math.min(36, cardH / 4));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, titleSz));
                String bossHeadline = anim.enrageTransform
                        ? switch (anim.boss) {
                            case GODZILLA -> "BURNING GODZILLA";
                            case GHIDORAH -> "ALPHA GHIDORAH";
                            case MOTHRA -> "SOLAR MOTHRA";
                        }
                        : switch (anim.boss) {
                            case GODZILLA -> "GODZILLA";
                            case GHIDORAH -> "KING GHIDORAH";
                            case MOTHRA -> "MOTHRA";
                        };
                g.setColor(new Color(0, 0, 0, 210));
                g.drawString(bossHeadline, txBase + 3, iy + 78 + 3);
                g.setColor(Color.WHITE);
                g.drawString(bossHeadline, txBase, iy + 78);

                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
                String sub = anim.enrageTransform
                        ? switch (anim.boss) {
                            case GODZILLA -> "QUÁ TẢI HẠT NHÂN · TIA XOẮN ĐỎ & VỰC DUNG NHAM";
                            case GHIDORAH -> "TRIỆU HỒI SIÊU BÃO · HỐ ĐEN TRỌNG LỰC Ω";
                            case MOTHRA -> "NỮ HOÀNG BÙNG CHÁY · MƯA PHẤN & SIÊU TÂN TINH";
                        }
                        : switch (anim.boss) {
                            case GODZILLA -> "VUA QUÁI VẬT · HƠI THỞ NGUYÊN TỬ";
                            case GHIDORAH -> "HOÀNG ĐẾ BÃO TỐ · TAM ĐẦU LÔI ĐIỆN";
                            case MOTHRA -> "NỮ HOÀNG THIÊN NHIÊN · CUỒNG PHONG & THÁNH QUANG";
                        };
                g.setColor(bossCol.brighter());
                g.drawString(sub, txBase, iy + 106);

                g.setFont(UiTokens.FONT_COORD);
                g.setColor(new Color(0xE2E8F0));
                g.drawString(anim.enrageTransform
                        ? "HP <= 50% · BỘ 5 KỸ NĂNG HỦY DIỆT ĐÃ KÍCH HOẠT!"
                        : "HP 20 · PHÁ ĐIỂM YẾU ×2 ST · CẢNH BÁO SÓNG THẦN", txBase, iy + cardH - 22);

                g.setClip(prevClip);

                // Crisp Neon Double Border around slanted cut-in card
                g.setComposite(alphaComp(cardAlpha));
                g.setColor(Color.WHITE);
                g.setStroke(roundStroke(3.2f));
                g.drawPolygon(cardPoly);
                g.setColor(bossCol);
                g.setStroke(roundStroke(1.8f));
                g.drawPolygon(cardPoly);
            }
        }

        // 4. Final white horizontal shatter slash at t >= 0.82
        if (t >= 0.82 && t <= 0.98) {
            double sP = (t - 0.82) / 0.16;
            float sA = (float) Math.sin(sP * Math.PI);
            g.setComposite(alphaComp(0.92f * sA));
            g.setColor(Color.WHITE);
            g.setStroke(roundStroke((float) (6.0 * (1.0 - sP) + 2.0)));
            g.drawLine(0, H / 2 + 12, W, H / 2 - 12);
        }

        g.setClip(prevClip);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA);
        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private static void drawWarningTapeBar(Graphics2D g, int x, int y, int w, int h, int scrollPx,
                                           Color primary, Color stripeCol, float alpha, String label) {
        Shape oldClip = g.getClip();
        g.setClip(x, y, w, h);
        g.setComposite(alphaComp(0.92f * alpha));
        g.setColor(new Color(8, 12, 22));
        g.fillRect(x, y, w, h);

        int step = 34;
        int off = Math.floorMod(scrollPx, step);
        g.setColor(primary);
        g.setComposite(alphaComp(0.32f * alpha));
        for (int sx = -step + off; sx < w + step; sx += step) {
            Polygon p = new Polygon(
                    new int[]{sx, sx + 14, sx + 14 + h, sx + h},
                    new int[]{y, y, y + h, y + h}, 4);
            g.fillPolygon(p);
        }
        g.setComposite(alphaComp(0.88f * alpha));
        g.setColor(stripeCol);
        g.fillRect(x, y + h - 2, w, 2);
        g.fillRect(x, y, w, 2);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int tx = x + Math.max(12, (w - fm.stringWidth(label)) / 2);
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g.setComposite(alphaComp(alpha));
        g.setColor(Color.BLACK);
        g.drawString(label, tx + 1, ty + 1);
        g.setColor(Color.WHITE);
        g.drawString(label, tx, ty);
        g.setClip(oldClip);
    }

    private void drawTrajectoryRibbon(Graphics2D g, ActiveStrike s) {
        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        Color base = s.titanSkill != null
                ? s.titanSkill.accentColor
                : (s.zombieClass != null
                        ? s.zombieClass.accentColor
                        : (s.magic == CombatLoadout.MagicOption.NONE
                                ? (s.weapon == CombatLoadout.WeaponOption.TORPEDO ? new Color(0x7DD3FC) : UiTokens.ACCENT_SIGNAL)
                                : s.magic.accentColor));

        int n = s.history.size();
        for (int i = 1; i < n; i++) {
            TrailSample a = s.history.get(i - 1);
            TrailSample b = s.history.get(i);
            float frac = i / (float) n;
            // Outer atmospheric glow ribbon
            g.setComposite(alphaComp(frac * 0.28f));
            g.setColor(base);
            g.setStroke(roundStroke(3.0f + frac * 10.5f));
            g.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);
            // Mid vivid energy ribbon
            g.setComposite(alphaComp(frac * 0.76f));
            g.setStroke(roundStroke(2.0f + frac * 5.6f));
            g.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);
            // Hot incandescent core
            g.setComposite(alphaComp(frac * 0.90f));
            g.setColor(Color.WHITE);
            g.setStroke(roundStroke(1.0f + frac * 2.2f));
            g.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);

            if (s.magic == CombatLoadout.MagicOption.LIGHTNING && i % 2 == 0) {
                double mx = (a.x + b.x) * 0.5 + (rng.nextDouble() - 0.5) * 18.0;
                double my = (a.y + b.y) * 0.5 + (rng.nextDouble() - 0.5) * 18.0;
                g.setColor(s.titanSkill != null ? new Color(0xFACC15) : new Color(0xA855F7));
                g.setStroke(roundStroke(1.8f));
                g.drawLine((int) a.x, (int) a.y, (int) mx, (int) my);
                g.drawLine((int) mx, (int) my, (int) b.x, (int) b.y);
            } else if (s.zombieClass != null || s.titanSkill != null) {
                double dx = b.x - a.x;
                double dy = b.y - a.y;
                double len = Math.max(1.0, Math.hypot(dx, dy));
                double nx = -dy / len, ny = dx / len;
                double w1 = Math.sin(i * 1.1) * 7.5;
                double w2 = Math.sin((i + 1) * 1.1) * 7.5;
                g.setComposite(alphaComp(frac * 0.58f));
                g.setColor(base);
                g.setStroke(roundStroke(1.8f));
                g.drawLine((int) (a.x + nx * w1), (int) (a.y + ny * w1),
                        (int) (b.x + nx * w2), (int) (b.y + ny * w2));
            }
        }
        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private static void drawRotatedCentered(Graphics2D g, BufferedImage img, double cx, double cy,
                                            int w, int h, double angleRad, float alpha) {
        if (img == null || w <= 0 || h <= 0 || alpha <= 0.01f) return;
        Composite prevComp = g.getComposite();
        AffineTransform prevTx = g.getTransform();
        g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
        g.translate(cx, cy);
        g.rotate(angleRad);
        SpriteAssets.drawSharp(g, img, -w / 2, -h / 2, w, h);
        g.setTransform(prevTx);
        g.setComposite(prevComp);
    }

    private record Point2(double x, double y) { }
    private record TrailSample(double x, double y, double t) { }

    private static final class ActiveStrike {
        final Point origin;
        final Point target;
        final List<Point> cellTargets;
        final int cellSize;
        final CombatLoadout.WeaponOption weapon;
        final CombatLoadout.MagicOption magic;
        final ZombieMode.ZombieClass zombieClass;
        final TitanMode.TitanSkill titanSkill;
        final boolean titanEnraged;
        final List<Point> extraOrigins;
        final boolean lethalHit;
        final boolean shieldBlocked;
        final long startMs;
        final long durationMs;
        final Runnable onImpact;
        final List<TrailSample> history = new ArrayList<>();
        long lastParticleMs;
        boolean splitTriggered;
        boolean impactFired;

        ActiveStrike(Point origin, Point target, List<Point> cellTargets, int cellSize,
                     CombatLoadout.WeaponOption weapon, CombatLoadout.MagicOption magic,
                     ZombieMode.ZombieClass zombieClass,
                     boolean lethalHit, boolean shieldBlocked, long durationMs, Runnable onImpact) {
            this(origin, target, cellTargets, cellSize, weapon, magic, zombieClass,
                    null, false, List.of(), lethalHit, shieldBlocked, durationMs, onImpact);
        }

        ActiveStrike(Point origin, Point target, List<Point> cellTargets, int cellSize,
                     CombatLoadout.WeaponOption weapon, CombatLoadout.MagicOption magic,
                     ZombieMode.ZombieClass zombieClass, TitanMode.TitanSkill titanSkill,
                     boolean titanEnraged, List<Point> extraOrigins,
                     boolean lethalHit, boolean shieldBlocked, long durationMs, Runnable onImpact) {
            this.origin = origin;
            this.target = target;
            this.cellTargets = cellTargets;
            this.cellSize = cellSize;
            this.weapon = weapon;
            this.magic = magic;
            this.zombieClass = zombieClass;
            this.titanSkill = titanSkill;
            this.titanEnraged = titanEnraged;
            this.extraOrigins = extraOrigins != null ? extraOrigins : List.of();
            this.lethalHit = lethalHit;
            this.shieldBlocked = shieldBlocked;
            this.startMs = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.onImpact = onImpact;
        }
    }

    private static AlphaComposite alphaComp(double alpha) {
        return ALPHA_CACHE[Math.max(0, Math.min(100, (int) Math.round(alpha * 100.0)))];
    }

    private static BasicStroke roundStroke(float width) {
        return ROUND_STROKES[Math.max(1, Math.min(48, Math.round(width * 2f)))];
    }

    private static BufferedImage getGlowStamp(Color c, int radiusPx, boolean soft) {
        int r = Math.max(8, Math.min(144, ((radiusPx + 3) / 4) * 4));
        int rgb = (c != null ? c.getRGB() : 0xFFFFFF) & 0x00FFFFFF;
        long key = (((long) r) << 25) | (soft ? (1L << 24) : 0L) | rgb;
        BufferedImage cached = GLOW_CACHE.get(key);
        if (cached != null) return cached;

        int size = r * 2 + 2;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cr = (rgb >> 16) & 0xFF;
        int cg = (rgb >> 8) & 0xFF;
        int cb = rgb & 0xFF;
        float center = size * 0.5f;
        float[] dist = soft ? new float[]{0f, 0.48f, 1f} : new float[]{0f, 0.25f, 0.62f, 1f};
        Color[] cols = soft
                ? new Color[]{new Color(cr, cg, cb, 155), new Color(cr, cg, cb, 62), new Color(cr, cg, cb, 0)}
                : new Color[]{
                        new Color(Math.min(255, cr + 165), Math.min(255, cg + 165), Math.min(255, cb + 165), 245),
                        new Color(cr, cg, cb, 210),
                        new Color(cr, cg, cb, 68),
                        new Color(cr, cg, cb, 0)};
        g2.setPaint(new RadialGradientPaint(center, center, r, dist, cols));
        g2.fillRect(0, 0, size, size);
        g2.dispose();
        GLOW_CACHE.put(key, img);
        return img;
    }

    private static void drawGlow(Graphics2D g, Color c, double cx, double cy, double radius, float alpha, boolean soft) {
        if (alpha <= 0.01f || radius <= 1.0) return;
        int r = (int) Math.round(radius);
        BufferedImage stamp = getGlowStamp(c, r, soft);
        g.setComposite(alphaComp(alpha));
        g.drawImage(stamp, (int) Math.round(cx - r), (int) Math.round(cy - r), r * 2, r * 2, null);
    }

    private static final class TrailParticle {
        double x, y, vx, vy;
        final double size;
        final Color color;
        final long startMs, endMs;
        final CombatLoadout.MagicOption magic;
        final ParticleKind kind;

        TrailParticle(double x, double y, double vx, double vy, double size, Color color,
                      long startMs, long lifeMs, CombatLoadout.MagicOption magic, ParticleKind kind) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.startMs = startMs;
            this.endMs = startMs + lifeMs;
            this.magic = magic;
            this.kind = kind;
        }
    }

    private static final class ShockwaveRing {
        final double x, y, maxRadius;
        final Color color;
        final long startMs, endMs, durationMs;

        ShockwaveRing(double x, double y, double maxRadius, Color color, long durationMs) {
            this.x = x;
            this.y = y;
            this.maxRadius = maxRadius;
            this.color = color;
            this.startMs = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.endMs = this.startMs + durationMs;
        }
    }

    private static final class ImpactBloom {
        final double x, y, maxRadius;
        final Color color;
        final long startMs, endMs, durationMs;

        ImpactBloom(double x, double y, double maxRadius, Color color, long durationMs) {
            this.x = x;
            this.y = y;
            this.maxRadius = maxRadius;
            this.color = color;
            this.startMs = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.endMs = this.startMs + durationMs;
        }
    }

    private static final class LightningBolt {
        final Path2D path = new Path2D.Double();
        final Path2D branches = new Path2D.Double();
        final double endX, endY;
        final Color color;
        final float thickness;
        final long startMs, endMs, durationMs;

        LightningBolt(double x1, double y1, double x2, double y2, Color color, float thickness, long durationMs, Random rng) {
            this.endX = x2;
            this.endY = y2;
            this.color = color;
            this.thickness = thickness;
            this.startMs = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.endMs = this.startMs + durationMs;
            int segs = 8;
            double dx = x2 - x1;
            double dy = y2 - y1;
            double len = Math.max(1.0, Math.hypot(dx, dy));
            double nx = -dy / len;
            double ny = dx / len;
            path.moveTo(x1, y1);
            for (int i = 1; i < segs; i++) {
                double frac = i / (double) segs;
                double jit = (rng.nextDouble() - 0.5) * Math.min(36.0, len * 0.24);
                double vx = x1 + dx * frac + nx * jit;
                double vy = y1 + dy * frac + ny * jit;
                path.lineTo(vx, vy);
                if ((i == 2 || i == 4 || i == 6) && len > 28.0) {
                    double sign = (i % 4 == 0 ? 1.0 : -1.0);
                    double bLen = len * (0.14 + rng.nextDouble() * 0.14);
                    double mx = vx + nx * sign * bLen * 0.55 + dx * 0.04;
                    double my = vy + ny * sign * bLen * 0.55 + dy * 0.06;
                    double ex = mx + nx * sign * bLen * 0.45 + (rng.nextDouble() - 0.5) * 14.0;
                    double ey = my + dy * 0.08 + rng.nextDouble() * 16.0;
                    branches.moveTo(vx, vy);
                    branches.lineTo(mx, my);
                    branches.lineTo(ex, ey);
                }
            }
            path.lineTo(x2, y2);
        }
    }

    private static final class ShieldFlare {
        final double x, y, radius;
        final long startMs, endMs, durationMs;
        final int damage;
        final Color tint;

        ShieldFlare(double x, double y, double radius, long durationMs, int damage, Color tint) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.startMs = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.endMs = this.startMs + durationMs;
            this.damage = damage;
            this.tint = tint != null ? tint : UiTokens.SHIELD_TINT;
        }
    }

    private static final class CrystalShard {
        double x, y, vx, vy, rot, vrot;
        final int size;
        final Color color;

        CrystalShard(double x, double y, double vx, double vy, double rot, double vrot, int size, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.rot = rot;
            this.vrot = vrot;
            this.size = size;
            this.color = color;
        }
    }

    private static final class ShieldShatterAnim {
        final int gx, gy, gw, gh;
        final Point center;
        final long startMs, endMs, durationMs;
        final List<CrystalShard> shards = new ArrayList<>();

        ShieldShatterAnim(int gx, int gy, int gw, int gh, Point center, long durationMs, Random rng) {
            this.gx = gx;
            this.gy = gy;
            this.gw = gw;
            this.gh = gh;
            this.center = center;
            this.startMs = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.endMs = this.startMs + durationMs;
            Color[] palette = {
                    new Color(0x38BDF8), new Color(0xBAE6FD), new Color(0xFBBF24),
                    new Color(0xF87171), new Color(0x67E8F9)
            };
            for (int i = 0; i < 48; i++) {
                double sx = gx + rng.nextDouble() * gw;
                double sy = gy + rng.nextDouble() * gh;
                double ang = Math.atan2(sy - center.y, sx - center.x) + (rng.nextDouble() - 0.5) * 0.6;
                double spd = 2.2 + rng.nextDouble() * 6.0;
                shards.add(new CrystalShard(
                        sx, sy,
                        Math.cos(ang) * spd, Math.sin(ang) * spd - 2.6,
                        rng.nextDouble() * Math.PI * 2.0,
                        (rng.nextDouble() - 0.5) * 0.28,
                        5 + rng.nextInt(8),
                        palette[i % palette.length]));
            }
        }
    }

    private static final class FloatingCallout {
        final double x, y, driftY;
        final String text;
        final Color color;
        final long startMs, endMs, durationMs;

        FloatingCallout(double x, double y, String text, Color color, long durationMs) {
            this(x, y, text, color, durationMs, -30.0);
        }

        FloatingCallout(double x, double y, String text, Color color, long durationMs, double driftY) {
            this.x = x;
            this.y = y;
            this.driftY = driftY;
            this.text = text;
            this.color = color;
            this.startMs = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.endMs = this.startMs + durationMs;
        }
    }

    private void drawFleetShieldCircles(Graphics2D g, FleetShieldAnim anim, long now) {
        double t = Math.max(0.0, Math.min(1.0, (now - anim.startMs) / (double) anim.durationMs));
        float masterFade = (float) (t < 0.72 ? 1.0 : Math.max(0.0, 1.0 - (t - 0.72) / 0.28));
        if (masterFade <= 0.01f) return;

        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        Object prevAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Inter-ship Arcane Ley-Line Constellation connecting all 5 allied ships & board core
        int n = anim.nodes.size();
        if (n >= 2 && t >= 0.14) {
            double leyProg = Math.min(1.0, (t - 0.14) / 0.26);
            double converge = t >= 0.34 ? Math.min(1.0, (t - 0.34) / 0.22) : 0.0;
            for (int i = 0; i < n; i++) {
                ShipSealNode a = anim.nodes.get(i);
                ShipSealNode b = anim.nodes.get((i + 1) % n);
                double ex = a.pt.x + (b.pt.x - a.pt.x) * leyProg;
                double ey = a.pt.y + (b.pt.y - a.pt.y) * leyProg;

                g.setComposite(alphaComp(masterFade * 0.28f));
                g.setColor(new Color(0x0EA5E9));
                g.setStroke(roundStroke(6.0f));
                g.drawLine(a.pt.x, a.pt.y, (int) Math.round(ex), (int) Math.round(ey));

                g.setComposite(alphaComp(masterFade * 0.86f));
                g.setColor(new Color(0xBAE6FD));
                g.setStroke(roundStroke(2.2f));
                g.drawLine(a.pt.x, a.pt.y, (int) Math.round(ex), (int) Math.round(ey));

                // Orbiting mana pulse sliding along ley-line
                double pulseU = ((now * 0.0024 + i * 0.21) % 1.0) * leyProg;
                int px = (int) Math.round(a.pt.x + (b.pt.x - a.pt.x) * pulseU);
                int py = (int) Math.round(a.pt.y + (b.pt.y - a.pt.y) * pulseU);
                g.setComposite(alphaComp(masterFade));
                SpriteAssets.drawVoxelCube(g, px, py, 5, new Color(0xFEF08A), new Color(0x38BDF8));

                if (converge > 0.01) {
                    double cx = a.pt.x + (anim.boardCenter.x - a.pt.x) * converge;
                    double cy = a.pt.y + (anim.boardCenter.y - a.pt.y) * converge;
                    g.setComposite(alphaComp(masterFade * 0.42f));
                    g.setColor(new Color(0xFDE047));
                    g.setStroke(roundStroke(2.4f));
                    g.drawLine(a.pt.x, a.pt.y, (int) Math.round(cx), (int) Math.round(cy));
                }
            }
        }

        // 2. Per-ship dual-layer Arcane Magic Circles + Hexagram Sacred Seals
        BufferedImage arcaneRune = SpriteAssets.getMagicRune(CombatLoadout.MagicOption.ARCANE_SONAR);
        BufferedImage frostRune = SpriteAssets.getMagicRune(CombatLoadout.MagicOption.FROST);
        BufferedImage shieldOrb = SpriteAssets.getMagicShield();

        for (ShipSealNode node : anim.nodes) {
            double lt = Math.max(0.0, Math.min(1.0, (t - node.stagger) / 0.42));
            if (lt <= 0.01) continue;
            double popEase = 1.0 - Math.pow(1.0 - lt, 3.0) * (1.0 - Math.sin(lt * Math.PI) * 0.16);
            double rad = anim.cellSize * (0.94 + node.shipLength * 0.15) * Math.max(0.18, popEase);
            float alpha = (float) (Math.min(1.0, lt * 1.75) * masterFade);

            drawGlow(g, new Color(0x38BDF8), node.pt.x, node.pt.y, rad * 1.30, alpha * 0.60f, true);
            int outerSz = Math.max(36, (int) Math.round(rad * 2.18));
            int innerSz = Math.max(26, (int) Math.round(rad * 1.56));
            drawRotatedCentered(g, arcaneRune, node.pt.x, node.pt.y, outerSz, outerSz,
                    t * 5.2 + node.stagger * 8.0, alpha * 0.90f);
            drawRotatedCentered(g, frostRune, node.pt.x, node.pt.y, innerSz, innerSz,
                    -t * 6.4 - node.stagger * 5.0, alpha * 0.80f);

            drawHexagramSeal(g, node.pt.x, node.pt.y, rad * 0.88, t * 2.6 + node.stagger * 6.0, alpha);

            if (shieldOrb != null && t < 0.78) {
                float orbAlpha = (float) (alpha * (t < 0.56 ? 0.88 : Math.max(0.0, 1.0 - (t - 0.56) / 0.22)));
                int orbSz = Math.max(18, (int) Math.round(anim.cellSize * 0.72));
                int liftY = (int) Math.round(node.pt.y - lt * anim.cellSize * 0.26);
                g.setComposite(alphaComp(orbAlpha));
                SpriteAssets.drawSharp(g, shieldOrb, node.pt.x - orbSz / 2, liftY - orbSz / 2, orbSz, orbSz);
            }
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA != null ? prevAA : RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private static void drawHexagramSeal(Graphics2D g, double cx, double cy, double rad, double rot, float alpha) {
        g.setComposite(alphaComp(alpha * 0.88f));
        g.setColor(new Color(0xE0F2FE));
        g.setStroke(roundStroke(1.8f));
        int ir = Math.max(10, (int) Math.round(rad));
        g.drawOval((int) Math.round(cx - ir), (int) Math.round(cy - ir), ir * 2, ir * 2);

        double innerR = rad * 0.82;
        for (int tri = 0; tri < 2; tri++) {
            double baseAng = rot + (tri == 0 ? 0.0 : Math.PI / 3.0);
            int[] px = new int[3];
            int[] py = new int[3];
            for (int v = 0; v < 3; v++) {
                double a = baseAng + v * (Math.PI * 2.0 / 3.0);
                px[v] = (int) Math.round(cx + Math.cos(a) * innerR);
                py[v] = (int) Math.round(cy + Math.sin(a) * innerR);
            }
            g.setColor(tri == 0 ? new Color(0x67E8F9) : new Color(0xFDE047));
            g.setStroke(roundStroke(tri == 0 ? 2.0f : 1.6f));
            g.drawPolygon(px, py, 3);
        }

        for (int v = 0; v < 6; v++) {
            double a = rot + v * (Math.PI / 3.0);
            int vx = (int) Math.round(cx + Math.cos(a) * rad);
            int vy = (int) Math.round(cy + Math.sin(a) * rad);
            SpriteAssets.drawVoxelCube(g, vx, vy, 4,
                    (v % 2 == 0) ? new Color(0xFEF08A) : Color.WHITE, new Color(0x0EA5E9));
        }
    }

    private void drawShieldCutInBanner(Graphics2D g, FleetShieldAnim anim, long now) {
        double t = Math.max(0.0, Math.min(1.0, (now - anim.startMs) / (double) anim.durationMs));
        if (t < 0.02 || t > 0.98) return;
        double p = Math.max(0.0, Math.min(1.0, (t - 0.02) / 0.96));

        int W = getWidth();
        int H = getHeight();
        if (W < 280 || H < 220) return;

        int bw = Math.max(500, Math.min(720, (int) Math.round(W * 0.70)));
        int bh = Math.max(132, Math.min(156, (int) Math.round(H * 0.24)));
        int skew = Math.max(24, (int) Math.round(bh * 0.22));
        int anchorY = Math.max(44, Math.min((int) Math.round(H * 0.34), H - bh - 44));

        double xStart = -bw * 0.92;
        double xHoldL = (W - bw) * 0.28;
        double xHoldR = (W - bw) * 0.68;
        double xEnd = W + skew + 50.0;

        double bx;
        double by;
        float alpha;
        if (p < 0.24) {
            double u = p / 0.24;
            double ease = 1.0 - Math.pow(1.0 - u, 2.4);
            bx = xStart + (xHoldL - xStart) * ease;
            by = anchorY + (1.0 - ease) * 96.0;
            alpha = (float) Math.min(1.0, u * 2.4);
        } else if (p < 0.78) {
            double u = (p - 0.24) / 0.54;
            bx = xHoldL + (xHoldR - xHoldL) * u;
            by = anchorY - u * 14.0;
            alpha = 1.0f;
        } else {
            double u = (p - 0.78) / 0.22;
            double ease = u * u * u;
            bx = xHoldR + (xEnd - xHoldR) * ease;
            by = anchorY - 14.0 - ease * 64.0;
            alpha = (float) Math.max(0.0, 1.0 - Math.max(0.0, u - 0.35) / 0.65);
        }
        if (alpha <= 0.02f) return;

        int ix = (int) Math.round(bx);
        int iy = (int) Math.round(by);

        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        Shape prevClip = g.getClip();
        Paint prevPaint = g.getPaint();
        Object prevAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Sweeping comic speed trails behind the entering/exiting cut-in panel
        if (p < 0.26 || p > 0.74) {
            float trailAlpha = (float) ((p < 0.26 ? (1.0 - p / 0.26) : (p - 0.74) / 0.26) * 0.65 * alpha);
            g.setComposite(alphaComp(trailAlpha));
            for (int k = 0; k < 5; k++) {
                int ly = iy + 14 + k * (bh / 5);
                int lx1 = ix - 90 - (k % 2) * 55;
                int lx2 = ix + bw + skew + 70 + (k % 3) * 40;
                g.setColor(k % 2 == 0 ? new Color(0x38BDF8) : new Color(0xE0F2FE));
                g.setStroke(roundStroke(k % 2 == 0 ? 3.5f : 2.0f));
                g.drawLine(lx1, ly, lx2, ly - 10);
            }
        }

        Polygon panelPoly = new Polygon(
                new int[]{ix + skew, ix + bw + skew, ix + bw, ix},
                new int[]{iy, iy, iy + bh, iy + bh},
                4
        );
        Polygon shadowPoly = new Polygon(
                new int[]{ix + skew + 8, ix + bw + skew + 8, ix + bw + 8, ix + 8},
                new int[]{iy + 10, iy + 10, iy + bh + 10, iy + bh + 10},
                4
        );

        g.setComposite(alphaComp(alpha * 0.56f));
        g.setColor(new Color(2, 6, 18));
        g.fillPolygon(shadowPoly);

        g.setComposite(alphaComp(alpha * 0.32f));
        g.setColor(new Color(0x38BDF8));
        g.setStroke(roundStroke(9.0f));
        g.drawPolygon(panelPoly);

        // Clip panel interior
        g.setClip(panelPoly);
        g.setComposite(alphaComp(alpha * 0.97f));
        g.setPaint(new GradientPaint(
                ix, iy, new Color(0x040B1E),
                ix + bw, iy + bh, new Color(0x134E88)));
        g.fillPolygon(panelPoly);

        // Ben-Day manga comic halftone dots in corners
        g.setComposite(alphaComp(alpha * 0.16f));
        g.setColor(new Color(0x67E8F9));
        for (int dy = iy + 8; dy < iy + bh - 6; dy += 12) {
            for (int dx = ix + 8; dx < ix + bw + skew - 8; dx += 14) {
                boolean nearCorner = (dx < ix + 95 && dy < iy + 55) || (dx > ix + bw - 95 && dy > iy + bh - 55);
                if (nearCorner) g.fillOval(dx, dy, 3, 3);
            }
        }

        // Fast horizontal manga speed-lines rushing left-to-right
        double scroll = (now * 0.42) % 140.0;
        for (int row = 0; row < 12; row++) {
            int sy = iy + 8 + row * (bh / 12);
            int len = 70 + ((row * 53) % 130);
            int sx = (int) (ix - 60 + ((row * 97 + scroll * 1.4) % (bw + skew + 90)));
            g.setComposite(alphaComp(alpha * (row % 3 == 0 ? 0.24f : 0.13f)));
            g.setColor(row % 4 == 0 ? Color.WHITE : new Color(0x7DD3FC));
            g.setStroke(roundStroke(row % 3 == 0 ? 2.2f : 1.2f));
            g.drawLine(sx, sy, sx + len, sy - 6);
        }

        // Giant subtle rotating Arcane Rune watermark behind the 5 ships
        int leftZoneX = ix + skew + 12;
        int fleetZoneW = (int) Math.round((bw - skew) * 0.56);
        int rightZoneX = leftZoneX + fleetZoneW + 10;
        int rightZoneW = Math.max(150, (ix + bw - 10) - rightZoneX);

        BufferedImage bgRune = SpriteAssets.getMagicRune(CombatLoadout.MagicOption.ARCANE_SONAR);
        drawRotatedCentered(g, bgRune, leftZoneX + fleetZoneW * 0.50, iy + bh * 0.62,
                (int) (bh * 1.15), (int) (bh * 1.15), now * 0.0022, alpha * 0.34f);

        // ALL 5 MINIATURE ALLIED SHIPS IN HEROIC V-ECHELON + COLLECTIVE "KHIÊN PHÉP!" BUBBLES
        ShipType[] echelonTypes = {
                ShipType.CARRIER, ShipType.BATTLESHIP,
                ShipType.CRUISER, ShipType.SUBMARINE, ShipType.DESTROYER
        };
        double[] fxFrac = {0.28, 0.74, 0.18, 0.51, 0.83};
        double[] fyFrac = {0.53, 0.53, 0.81, 0.82, 0.81};
        int unitW = Math.max(12, Math.min(18, (int) Math.round(fleetZoneW * 0.048)));

        for (int i = 0; i < echelonTypes.length; i++) {
            ShipType st = echelonTypes[i];
            int scx = (int) Math.round(leftZoneX + fleetZoneW * fxFrac[i]);
            int scy = (int) Math.round(iy + bh * fyFrac[i] + Math.sin(t * 16.0 + i * 1.3) * 1.6);
            drawComicMiniShip(g, st, scx, scy, unitW, now * 0.004 + i * 0.9, alpha, bgRune);
        }

        // Comic Shout Speech Bubble directly over all 5 mini ships ("5 TÀU: KHIÊN PHÉP!!")
        int bubbleX = leftZoneX + (int) Math.round(fleetZoneW * 0.14);
        int bubbleY = iy + 7;
        int bubbleW = Math.max(180, (int) Math.round(fleetZoneW * 0.72));
        int bubbleH = Math.max(28, (int) Math.round(bh * 0.22));
        drawSpeechBubble(g, bubbleX, bubbleY, bubbleW, bubbleH, "5 TÀU: KHIÊN PHÉP!!",
                new Color(0x0369A1), alpha,
                leftZoneX + (int) (fleetZoneW * 0.26), iy + (int) (bh * 0.43),
                leftZoneX + (int) (fleetZoneW * 0.51), iy + (int) (bh * 0.70),
                leftZoneX + (int) (fleetZoneW * 0.73), iy + (int) (bh * 0.43));

        // Diagonal glowing energy divider between Fleet & Hero Text
        int divTopX = rightZoneX + 8;
        int divBotX = rightZoneX - 10;
        g.setComposite(alphaComp(alpha * 0.45f));
        g.setColor(new Color(0x38BDF8));
        g.setStroke(roundStroke(6.0f));
        g.drawLine(divTopX, iy + 4, divBotX, iy + bh - 4);
        g.setComposite(alphaComp(alpha * 0.92f));
        g.setColor(Color.WHITE);
        g.setStroke(roundStroke(2.0f));
        g.drawLine(divTopX, iy + 4, divBotX, iy + bh - 4);

        // RIGHT ZONE: Floating 3D Shield Emblem + Comic Burst + Extruded "KHIÊN PHÉP!" + Subtitle Pill
        int rCenterX = rightZoneX + rightZoneW / 2;
        int rCenterY = iy + bh / 2;
        drawGlow(g, new Color(0x38BDF8), rCenterX + 12, rCenterY - 6, rightZoneW * 0.48, alpha * 0.62f, true);

        // Subtle manga starburst behind hero title
        g.setComposite(alphaComp(alpha * 0.24f));
        g.setColor(new Color(0xFEF08A));
        g.setStroke(roundStroke(1.6f));
        for (int ray = 0; ray < 10; ray++) {
            double ang = ray * (Math.PI / 5.0) + t * 0.9;
            int rx1 = rCenterX - 10 + (int) Math.round(Math.cos(ang) * 28.0);
            int ry1 = rCenterY - 8 + (int) Math.round(Math.sin(ang) * 18.0);
            int rx2 = rCenterX - 10 + (int) Math.round(Math.cos(ang) * 92.0);
            int ry2 = rCenterY - 8 + (int) Math.round(Math.sin(ang) * 56.0);
            g.drawLine(rx1, ry1, rx2, ry2);
        }

        BufferedImage crest = SpriteAssets.getMagicShield();
        if (crest != null) {
            int crestSz = Math.max(38, Math.min(48, (int) Math.round(bh * 0.34)));
            int crestX = rightZoneX + rightZoneW - crestSz - 8;
            int crestY = iy + 10;
            int ccx = crestX + crestSz / 2;
            int ccy = crestY + crestSz / 2;
            drawGlow(g, new Color(0xBAE6FD), ccx, ccy, crestSz * 0.85, alpha * 0.72f, true);
            g.setComposite(alphaComp(alpha * 0.98f));
            SpriteAssets.drawSharp(g, crest, crestX, crestY, crestSz, crestSz);
            SpriteAssets.drawVoxelCube(g, crestX + crestSz - 3, crestY + 5, 4, Color.WHITE, new Color(0xFEF08A));
            SpriteAssets.drawVoxelCube(g, crestX + 4, crestY + crestSz - 4, 4, Color.WHITE, new Color(0x38BDF8));
        }

        // Bold Hero Display Header: "KHIÊN PHÉP!"
        String title = "KHIÊN PHÉP!";
        int titleFontSize = Math.max(22, Math.min(31, (int) Math.round(rightZoneW * 0.125)));
        Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, titleFontSize);
        g.setFont(titleFont);
        FontMetrics titleFm = g.getFontMetrics();
        int maxTitleW = rightZoneW - 44;
        while (titleFont.getSize() > 16 && titleFm.stringWidth(title) > maxTitleW) {
            titleFont = titleFont.deriveFont((float) (titleFont.getSize() - 2));
            g.setFont(titleFont);
            titleFm = g.getFontMetrics();
        }
        int titleX = rightZoneX + 12;
        int titleY = iy + (int) Math.round(bh * 0.52);

        g.setComposite(alphaComp(alpha));
        g.setColor(new Color(0x020617));
        g.drawString(title, titleX + 3, titleY + 3);
        g.setColor(new Color(0x0284C7));
        g.drawString(title, titleX + 1, titleY + 1);
        g.setPaint(new GradientPaint(titleX, titleY - titleFm.getAscent(), new Color(0xFEF08A),
                titleX, titleY, Color.WHITE));
        g.drawString(title, titleX, titleY);

        // Crisp Sub-Badge underneath title
        String subText = "5 CHIẾN HẠM · KHIÊN TOÀN BẢN ĐỒ";
        Font subFont = new Font(Font.SANS_SERIF, Font.BOLD, 11);
        g.setFont(subFont);
        FontMetrics sfm = g.getFontMetrics();
        while (subFont.getSize() > 9 && sfm.stringWidth(subText) > rightZoneW - 22) {
            subFont = subFont.deriveFont((float) (subFont.getSize() - 1));
            g.setFont(subFont);
            sfm = g.getFontMetrics();
        }
        int subPillW = Math.min(rightZoneW - 10, sfm.stringWidth(subText) + 18);
        int subPillH = sfm.getHeight() + 8;
        int subPillX = rightZoneX + 10;
        int subPillY = titleY + 14;
        g.setPaint(null);
        g.setComposite(alphaComp(alpha * 0.90f));
        g.setColor(new Color(0x06152B));
        g.fillRoundRect(subPillX, subPillY, subPillW, subPillH, 8, 8);
        g.setColor(new Color(0x38BDF8));
        g.setStroke(roundStroke(1.6f));
        g.drawRoundRect(subPillX, subPillY, subPillW, subPillH, 8, 8);
        g.setColor(new Color(0xE0F2FE));
        g.drawString(subText, subPillX + 9, subPillY + sfm.getAscent() + 4);

        // Sweeping diagonal comic highlight glint across glass
        double glintFrac = Math.max(0.0, Math.min(1.0, (p - 0.14) / 0.52));
        if (glintFrac > 0.0 && glintFrac < 1.0) {
            int gx = ix + (int) Math.round(glintFrac * (bw + 90)) - 45;
            Polygon glintPoly = new Polygon(
                    new int[]{gx + 26, gx + 54, gx + 14, gx - 14},
                    new int[]{iy, iy, iy + bh, iy + bh},
                    4
            );
            g.setComposite(alphaComp(alpha * 0.18f));
            g.setColor(Color.WHITE);
            g.fillPolygon(glintPoly);
        }

        // Unclip & render sharp double manga frame border + 4 corner studs
        g.setClip(prevClip);
        g.setPaint(prevPaint);
        g.setComposite(alphaComp(alpha));
        g.setColor(new Color(0x0EA5E9));
        g.setStroke(roundStroke(5.5f));
        g.drawPolygon(panelPoly);
        g.setColor(new Color(0xF8FAFC));
        g.setStroke(roundStroke(3.0f));
        g.drawPolygon(panelPoly);

        for (int c = 0; c < 4; c++) {
            SpriteAssets.drawVoxelCube(g, panelPoly.xpoints[c], panelPoly.ypoints[c], 5,
                    (c % 2 == 0) ? new Color(0xFEF08A) : Color.WHITE, new Color(0x38BDF8));
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA != null ? prevAA : RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private void drawComicMiniShip(
            Graphics2D g,
            ShipType type,
            int cx,
            int cy,
            int unitW,
            double rot,
            float alpha,
            BufferedImage miniRune
    ) {
        int spriteH = Math.max(18, (int) Math.round(unitW * 1.18));
        int spriteW = Math.max(spriteH, type.length * unitW);
        int runeDiam = Math.max(spriteH + 14, (int) Math.round(spriteW * 0.85));

        drawGlow(g, new Color(0x38BDF8), cx, cy + 2, runeDiam * 0.62, alpha * 0.45f, true);
        drawRotatedCentered(g, miniRune, cx, cy + 2, runeDiam, (int) Math.round(runeDiam * 0.52), rot, alpha * 0.78f);

        g.setComposite(alphaComp(alpha * 0.85f));
        g.setColor(new Color(0x67E8F9));
        g.setStroke(roundStroke(1.3f));
        g.drawOval(cx - runeDiam / 2, cy + 2 - runeDiam / 4, runeDiam, Math.max(10, runeDiam / 2));

        BufferedImage shadow = SpriteAssets.getShipShadow(type, true);
        if (shadow != null) {
            g.setComposite(alphaComp(alpha * 0.45f));
            SpriteAssets.drawSharp(g, shadow, cx - spriteW / 2 + 2, cy - spriteH / 2 + 4, spriteW, spriteH);
        }
        BufferedImage shipImg = SpriteAssets.getShipSprite(type, true);
        if (shipImg != null) {
            g.setComposite(alphaComp(alpha));
            SpriteAssets.drawSharp(g, shipImg, cx - spriteW / 2, cy - spriteH / 2, spriteW, spriteH);
        } else {
            g.setComposite(alphaComp(alpha));
            g.setColor(new Color(0x94A3B8));
            g.fillRoundRect(cx - spriteW / 2, cy - spriteH / 2, spriteW, spriteH, 6, 6);
        }
        SpriteAssets.drawVoxelCube(g, cx, cy - spriteH / 2 - 2, 3, Color.WHITE, new Color(0x38BDF8));
    }

    private static void drawSpeechBubble(
            Graphics2D g,
            int x,
            int y,
            int w,
            int h,
            String text,
            Color textColor,
            float alpha,
            int tail1X,
            int tail1Y,
            int tail2X,
            int tail2Y,
            int tail3X,
            int tail3Y
    ) {
        g.setComposite(alphaComp(alpha * 0.97f));
        Polygon t1 = new Polygon(
                new int[]{x + w / 5 - 5, x + w / 5 + 6, tail1X},
                new int[]{y + h - 3, y + h - 3, tail1Y},
                3
        );
        Polygon t2 = new Polygon(
                new int[]{x + w / 2 - 5, x + w / 2 + 5, tail2X},
                new int[]{y + h - 3, y + h - 3, tail2Y},
                3
        );
        Polygon t3 = new Polygon(
                new int[]{x + (w * 4) / 5 - 6, x + (w * 4) / 5 + 5, tail3X},
                new int[]{y + h - 3, y + h - 3, tail3Y},
                3
        );
        g.setColor(new Color(0xF8FAFC));
        g.fillPolygon(t1);
        g.fillPolygon(t2);
        g.fillPolygon(t3);
        g.fillRoundRect(x, y, w, h, 14, 14);

        g.setColor(new Color(0x0B1320));
        g.setStroke(roundStroke(2.2f));
        g.drawPolygon(t1);
        g.drawPolygon(t2);
        g.drawPolygon(t3);
        g.drawRoundRect(x, y, w, h, 14, 14);

        // Clean interior patch hiding pointer seams
        g.setColor(new Color(0xF8FAFC));
        g.fillRoundRect(x + 2, y + 2, w - 4, h - 4, 11, 11);

        Font f = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        int tx = x + Math.max(8, (w - fm.stringWidth(text)) / 2);
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(new Color(0x0B1320));
        g.drawString(text, tx + 1, ty + 1);
        g.setColor(textColor);
        g.drawString(text, tx, ty);
    }

    private record ShipSealNode(Point pt, ShipType shipType, int shipLength, double stagger) { }

    private static final class FleetShieldAnim {
        final Point boardCenter;
        final Point[] boardCorners;
        final int cellSize;
        final List<ShipSealNode> nodes;
        final long startMs;
        final long durationMs;
        final Runnable onBarrierErected;
        long lastParticleMs;
        boolean barrierTriggered;
        boolean bannerCalloutTriggered;

        FleetShieldAnim(
                Point boardCenter,
                Point[] boardCorners,
                int cellSize,
                List<ShipSealNode> nodes,
                long durationMs,
                Runnable onBarrierErected
        ) {
            this.boardCenter = boardCenter;
            this.boardCorners = boardCorners;
            this.cellSize = cellSize;
            this.nodes = nodes;
            this.startMs = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.onBarrierErected = onBarrierErected;
        }
    }

    private static final class BossIntroAnim {
        final TitanMode.TitanBoss boss;
        final Point bossCenter;
        final int cellSize;
        final long startMs;
        final long durationMs;
        final boolean enrageTransform;
        long lastSparkMs;
        boolean shatterTriggered;

        BossIntroAnim(TitanMode.TitanBoss boss, Point bossCenter, int cellSize, long durationMs, boolean enrageTransform) {
            this.boss = boss;
            this.bossCenter = bossCenter;
            this.cellSize = cellSize;
            this.startMs = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.enrageTransform = enrageTransform;
        }
    }

    private BufferedImage superHoloCache;

    private BufferedImage getSuperHoloHangarCache() {
        if (superHoloCache != null) return superHoloCache;
        BufferedImage raw = SpriteAssets.getSuperShipHangar();
        if (raw == null) return null;
        int w = raw.getWidth();
        int h = raw.getHeight();
        BufferedImage holo = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            boolean scanBright = (y % 4 == 0);
            for (int x = 0; x < w; x++) {
                int argb = raw.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a < 18) continue;
                int r = (argb >>> 16) & 0xFF;
                int gg = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                int lum = (r + gg + b) / 3;
                int outA = Math.min(245, (int) (a * (scanBright ? 0.92 : 0.56)));
                int cr = scanBright ? 140 : 40;
                int cg = Math.min(255, (scanBright ? 245 : 195) + lum / 6);
                int cb = 255;
                holo.setRGB(x, y, (outA << 24) | (cr << 16) | (cg << 8) | cb);
            }
        }
        superHoloCache = holo;
        return holo;
    }

    private void drawHyperionBeam(Graphics2D g, HyperionBeamAnim hb, long now) {
        double t = clamp01((now - hb.startMs) / (double) hb.durationMs);
        double envelope = t < 0.25 ? (t / 0.25) : Math.max(0.0, 1.0 - (t - 0.25) / 0.75);
        float alpha = (float) clamp01(envelope);
        if (alpha <= 0.01f) return;

        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();

        // Orbital drops coming down cleanly onto secondary cells in the 3x3 impact zone
        double orbT = clamp01((t - 0.14) / 0.36);
        if (orbT > 0.0 && orbT < 1.0) {
            for (int i = 0; i < hb.cellTargets.size(); i++) {
                Point tp = hb.cellTargets.get(i);
                double stagger = (i % 4) * 0.08;
                double lp = clamp01((t - 0.10 - stagger) / 0.28);
                if (lp <= 0.0 || lp >= 1.0) continue;
                double sx = tp.x - 110.0 * (1.0 - lp);
                double sy = tp.y - 320.0 * (1.0 - lp);
                g.setComposite(alphaComp((float) (0.78 * (1.0 - lp * 0.3))));
                g.setColor(new Color(0x38BDF8));
                g.setStroke(roundStroke(4.2f));
                g.drawLine((int) sx, (int) sy, tp.x, tp.y);
                g.setColor(new Color(0xFDE047));
                g.setStroke(roundStroke(1.8f));
                g.drawLine((int) sx, (int) sy, tp.x, tp.y);
            }
        }

        // Main Hyperion Railbeam from Super Ship bow -> Target 3x3 center
        double reach = clamp01(t / 0.30);
        int ex = (int) Math.round(hb.origin.x + (hb.target.x - hb.origin.x) * reach);
        int ey = (int) Math.round(hb.origin.y + (hb.target.y - hb.origin.y) * reach);

        float beamWidthOuter = (float) (hb.cellSize * 0.56 * (0.55 + 0.45 * envelope));
        g.setComposite(alphaComp(alpha * 0.48f));
        g.setColor(new Color(0x0EA5E9));
        g.setStroke(roundStroke(beamWidthOuter));
        g.drawLine(hb.origin.x, hb.origin.y, ex, ey);

        g.setComposite(alphaComp(alpha * 0.85f));
        g.setColor(new Color(0xFACC15));
        g.setStroke(roundStroke(Math.max(4f, beamWidthOuter * 0.46f)));
        g.drawLine(hb.origin.x, hb.origin.y, ex, ey);

        g.setComposite(alphaComp(alpha * 0.96f));
        g.setColor(Color.WHITE);
        g.setStroke(roundStroke(Math.max(2f, beamWidthOuter * 0.20f)));
        g.drawLine(hb.origin.x, hb.origin.y, ex, ey);

        drawGlow(g, new Color(0x38BDF8), ex, ey, hb.cellSize * 1.45, alpha * 0.85f, true);
        drawGlow(g, new Color(0xFDE047), hb.origin.x, hb.origin.y, hb.cellSize * 1.15, alpha * 0.80f, true);

        g.setStroke(prevStroke);
        g.setComposite(prevComp);
    }

    private void drawSuperShipCutscene(Graphics2D g, SuperShipCutsceneAnim sc, long now) {
        double t = clamp01((now - sc.startMs) / (double) sc.durationMs);
        int w = Math.max(640, getWidth());
        int h = Math.max(480, getHeight());

        double globalAlpha = t < 0.05 ? (t / 0.05) : (t > 0.92 ? Math.max(0.0, (1.0 - t) / 0.08) : 1.0);
        float alpha = (float) clamp01(globalAlpha);
        if (alpha <= 0.01f) return;

        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        Object prevAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Shape prevClip = g.getClip();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Widescreen cinematic dock theater band + deep backdrop dim
        int bandH = Math.min(h - 90, Math.max(330, (int) (h * 0.62)));
        int bandY = (h - bandH) / 2;

        g.setComposite(alphaComp(alpha * 0.62f));
        g.setColor(new Color(2, 6, 16));
        g.fillRect(0, 0, w, h);

        // Act IV opens into a radiant golden dawn sunrise over rushing ocean waves; Acts I-III live inside the secret dock
        double act4 = clamp01((t - 0.76) / 0.24);
        g.setComposite(alphaComp(alpha * 0.96f));
        Paint bgPaint;
        if (act4 > 0.0) {
            double skyP = Math.min(1.0, act4 * 1.45);
            Color topSky = blendColor(new Color(0x040B19), new Color(0x0369A1), skyP);
            Color horizon = blendColor(new Color(0x081B33), new Color(0xF59E0B), skyP * 0.85);
            bgPaint = new GradientPaint(0, bandY, topSky, 0, bandY + bandH, horizon);
        } else {
            bgPaint = new GradientPaint(0, bandY, new Color(0x040B19), 0, bandY + bandH, new Color(0x081B33));
        }
        g.setPaint(bgPaint);
        g.fillRect(0, bandY, w, bandH);

        // Clip inside cinema band cleanly
        g.setClip(0, bandY, w, bandH);

        // Submarine dock grid / CAD perspective floor & underwater spotlights
        if (act4 < 0.7) {
            float dockAlpha = (float) (alpha * (1.0 - act4) * 0.34);
            g.setComposite(alphaComp(dockAlpha));
            g.setColor(new Color(0x0EA5E9));
            g.setStroke(roundStroke(1.0f));
            for (int gx = 0; gx < w; gx += 42) {
                g.drawLine(gx, bandY, gx, bandY + bandH);
            }
            for (int gy = bandY; gy < bandY + bandH; gy += 36) {
                g.drawLine(0, gy, w, gy);
            }
            // Dock ceiling steel gantries & searchlight beams
            g.setComposite(alphaComp((float) (alpha * (1.0 - act4) * 0.24)));
            g.setColor(new Color(0x38BDF8));
            for (int sx : new int[]{w / 4, w / 2, (w * 3) / 4}) {
                Polygon cone = new Polygon(
                        new int[]{sx - 28, sx + 28, sx + 120, sx - 120},
                        new int[]{bandY, bandY, bandY + bandH, bandY + bandH}, 4);
                g.fillPolygon(cone);
            }
        }

        // Act IV Ocean waves & radiant sunrise flare
        if (act4 > 0.0) {
            int seaY = bandY + (int) (bandH * 0.60);
            double seaVis = Math.min(1.0, act4 * 1.65);
            g.setComposite(alphaComp((float) (alpha * seaVis)));
            drawGlow(g, new Color(0xFDE047), (int) (w * 0.78), seaY - 10, bandH * 0.48, (float) (alpha * seaVis * 0.85), true);
            g.setPaint(new GradientPaint(0, seaY, new Color(0x0284C7), 0, bandY + bandH, new Color(0x062040)));
            g.fillRect(0, seaY, w, bandY + bandH - seaY);
            g.setColor(new Color(0xE0F2FE));
            g.setStroke(roundStroke(2.2f));
            double scroll = (now * 0.28) % 90.0;
            for (int row = 0; row < 5; row++) {
                int wy = seaY + 12 + row * ((bandY + bandH - seaY) / 5);
                for (int wx = (int) -scroll; wx < w + 90; wx += 90) {
                    g.drawArc(wx, wy, 58, 12, 12, 148);
                }
            }
        }

        // Compute ship hangar placement & Act IV forward launch motion
        BufferedImage heroImg = SpriteAssets.getSuperShipHangar();
        BufferedImage holoImg = getSuperHoloHangarCache();
        int shipW = Math.min((int) (w * 0.56), 540);
        int shipH = (int) Math.round(shipW * 0.50);
        int baseX = (w - shipW) / 2;
        int baseY = bandY + (bandH - shipH) / 2 + 6;

        double bob = Math.sin(now * 0.008) * 3.0;
        int shipX = baseX;
        int shipY = (int) Math.round(baseY + bob);
        if (act4 > 0.0) {
            double thrustCurve = act4 * act4;
            shipX = (int) Math.round(baseX + thrustCurve * (w * 0.36));
            shipY = (int) Math.round(baseY - thrustCurve * 16.0 + bob);
        }

        // Drydock steel keel blocks & launch rail beneath ship during Acts I-III
        if (act4 < 0.5) {
            float cradleAlpha = (float) (alpha * (1.0 - act4 * 2.0) * 0.72);
            int railY = baseY + (int) (shipH * 0.84);
            g.setComposite(alphaComp(cradleAlpha));
            g.setColor(new Color(0x1E293B));
            g.fillRoundRect(baseX + 24, railY, shipW - 48, 8, 4, 4);
            g.setColor(new Color(0x38BDF8));
            g.setStroke(roundStroke(1.4f));
            g.drawLine(baseX + 24, railY, baseX + shipW - 24, railY);
            for (int p = 1; p <= 4; p++) {
                int px = baseX + (shipW * p) / 5;
                g.setColor(new Color(0x334155));
                g.fillRect(px - 6, railY - 14, 12, 16);
                g.setColor(new Color(0xFACC15));
                g.fillRect(px - 4, railY - 15, 8, 3);
            }
        }

        // Act IV Cobalt Ion Thrusters & Foaming Bow Spray around the speeding hull
        if (act4 > 0.02) {
            double power = clamp01(act4 * 2.2);
            int sternX = shipX + (int) (shipW * 0.14);
            int sternY = shipY + (int) (shipH * 0.54);
            int plumeLen = (int) Math.round(shipW * (0.38 + 0.36 * power) + Math.sin(now * 0.04) * 14);
            drawGlow(g, new Color(0x38BDF8), sternX, sternY, plumeLen * 0.65, (float) (alpha * 0.85), true);
            Polygon outerFlame = new Polygon(
                    new int[]{sternX, sternX - plumeLen, sternX - plumeLen / 2, sternX},
                    new int[]{sternY - 26, sternY - 4, sternY + 22, sternY + 24}, 4);
            g.setComposite(alphaComp((float) (alpha * 0.90)));
            g.setColor(new Color(0x38BDF8));
            g.fillPolygon(outerFlame);
            Polygon innerFlame = new Polygon(
                    new int[]{sternX, sternX - (int) (plumeLen * 0.68), sternX},
                    new int[]{sternY - 12, sternY, sternY + 12}, 3);
            g.setColor(Color.WHITE);
            g.fillPolygon(innerFlame);
            // White ocean bow spray ribbon under waterline
            g.setComposite(alphaComp((float) (alpha * 0.78)));
            g.setColor(Color.WHITE);
            g.setStroke(roundStroke(3.0f));
            int bowX = shipX + (int) (shipW * 0.86);
            int waterY = shipY + (int) (shipH * 0.76);
            g.drawLine(bowX, waterY, shipX + (int) (shipW * 0.18), waterY + 16);
            g.drawLine(bowX - 18, waterY + 6, shipX + (int) (shipW * 0.08), waterY + 24);
        }

        // Render Ship across the 4 Acts:
        // Act I (0..0.25): Blueprint wireframe hologram laser scan reveal + CAD callout specs
        // Act II (0.25..0.52): Welding sweep converts Hologram -> Metallic Voxel Dreadnought + Aether Core burst
        // Act III & IV (0.52..1.00): Full 3D Voxel Dreadnought + descending Hyperion Weapon Pods
        if (t < 0.25) {
            double scan = clamp01(t / 0.22);
            int visH = (int) Math.round(shipH * scan);
            Shape actClip = g.getClip();
            g.setClip(shipX, shipY, shipW, Math.max(1, visH));
            if (holoImg != null) {
                g.setComposite(alphaComp(alpha * 0.94f));
                SpriteAssets.drawSharp(g, holoImg, shipX, shipY, shipW, shipH);
            }
            g.setClip(actClip);
            int sy = shipY + visH;
            g.setComposite(alphaComp(alpha));
            g.setColor(new Color(0x67E8F9));
            g.setStroke(roundStroke(2.8f));
            g.drawLine(shipX - 30, sy, shipX + shipW + 30, sy);
            drawGlow(g, new Color(0x38BDF8), shipX + shipW / 2, sy, shipW * 0.35, alpha * 0.55f, true);
            g.setColor(new Color(0x38BDF8));
            g.setStroke(roundStroke(1.5f));
            g.drawRect(shipX - 10, shipY - 8, shipW + 20, shipH + 16);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
            g.drawString("LEN: 320m  |  ARMOR: NANITE+2", shipX - 6, shipY - 14);
            g.drawString("CORE: AETHER-X  |  SHIELD: 100%", shipX + shipW - 210, shipY + shipH + 22);
        } else if (t < 0.52) {
            double weld = clamp01((t - 0.25) / 0.25);
            int weldX = shipX + (int) Math.round(shipW * weld);
            Shape actClip = g.getClip();
            // Right un-built side: Blueprint Hologram
            if (holoImg != null) {
                g.setClip(weldX, shipY - 20, Math.max(1, shipX + shipW - weldX), shipH + 40);
                g.setComposite(alphaComp(alpha * 0.78f));
                SpriteAssets.drawSharp(g, holoImg, shipX, shipY, shipW, shipH);
            }
            // Left fabricated side: Full 3D Voxel Armor Hull
            if (heroImg != null) {
                g.setClip(shipX - 20, shipY - 20, Math.max(1, weldX - shipX + 20), shipH + 40);
                g.setComposite(alphaComp(alpha));
                SpriteAssets.drawSharp(g, heroImg, shipX, shipY, shipW, shipH);
            }
            g.setClip(actClip);

            // Overhead robotic gantry & glowing plasma welding beam at weldX
            if (weld < 0.98) {
                g.setComposite(alphaComp(alpha * 0.92f));
                g.setColor(new Color(0x334155));
                g.fillRect(weldX - 8, bandY, 16, Math.max(10, shipY - bandY + 12));
                g.setColor(new Color(0x38BDF8));
                g.setStroke(roundStroke(5.5f));
                g.drawLine(weldX, bandY + 18, weldX, shipY + shipH / 2 + 16);
                g.setColor(new Color(0xFDE047));
                g.setStroke(roundStroke(2.2f));
                g.drawLine(weldX, bandY + 18, weldX, shipY + shipH / 2 + 16);
                drawGlow(g, new Color(0xFDE047), weldX, shipY + shipH / 2, 42, alpha * 0.9f, true);
            }
            // Central Aether Reactor Ignition Flash when t >= 0.38
            if (t >= 0.38) {
                double pulse = Math.sin(((t - 0.38) / 0.14) * Math.PI);
                int coreX = shipX + (int) (shipW * 0.50);
                int coreY = shipY + (int) (shipH * 0.44);
                drawGlow(g, new Color(0x38BDF8), coreX, coreY, shipW * 0.28, (float) (alpha * clamp01(pulse) * 0.9), true);
            }
        } else {
            if (heroImg != null) {
                g.setComposite(alphaComp(alpha));
                SpriteAssets.drawSharp(g, heroImg, shipX, shipY, shipW, shipH);
            }
            // Act III: Weapon pods descending & clamping onto dorsal mounts
            if (t < 0.76) {
                double armP = clamp01((t - 0.52) / 0.18);
                double eased = 1.0 - Math.pow(1.0 - armP, 3);
                BufferedImage weap = SpriteAssets.getSuperShipWeapons();
                int mountSize = Math.max(76, Math.min(126, shipW / 4));
                int[] mountXs = {shipX + (int) (shipW * 0.34) - mountSize / 2, shipX + (int) (shipW * 0.62) - mountSize / 2};
                int[] destYs = {shipY + (int) (shipH * 0.18) - mountSize / 2, shipY + (int) (shipH * 0.24) - mountSize / 2};
                for (int i = 0; i < 2; i++) {
                    int wy = (int) Math.round(bandY - 40 + (destYs[i] - (bandY - 40)) * eased);
                    // Guide rails + crimson/gold target crosshairs
                    g.setComposite(alphaComp(alpha * 0.72f));
                    g.setColor(new Color(0xFACC15));
                    g.setStroke(roundStroke(1.6f));
                    g.drawLine(mountXs[i] + mountSize / 2, bandY, mountXs[i] + mountSize / 2, destYs[i] + mountSize / 2);
                    g.drawOval(mountXs[i] + mountSize / 4, destYs[i] + mountSize / 4, mountSize / 2, mountSize / 2);
                    if (weap != null) {
                        g.setComposite(alphaComp(alpha));
                        SpriteAssets.drawSharp(g, weap, mountXs[i], wy, mountSize, mountSize);
                    }
                    if (armP > 0.82) {
                        drawGlow(g, new Color(0xFDE047), mountXs[i] + mountSize / 2, destYs[i] + mountSize / 2,
                                mountSize * 0.7, (float) (alpha * (1.0 - (armP - 0.82) / 0.18)), true);
                    }
                }
            }
        }

        // Act IV vertical blast-door shutters parting from center when t >= 0.74 && t < 0.88
        if (t >= 0.74 && t < 0.88) {
            double doorOpen = clamp01((t - 0.74) / 0.12);
            int doorH = (int) Math.round((bandH / 2.0) * (1.0 - doorOpen));
            if (doorH > 2) {
                g.setComposite(alphaComp(alpha * 0.88f));
                g.setColor(new Color(0x0F172A));
                g.fillRect(0, bandY, w, doorH);
                g.fillRect(0, bandY + bandH - doorH, w, doorH);
                g.setColor(new Color(0xF59E0B));
                g.setStroke(roundStroke(3.0f));
                g.drawLine(0, bandY + doorH, w, bandY + doorH);
                g.drawLine(0, bandY + bandH - doorH, w, bandY + bandH - doorH);
            }
        }

        g.setClip(prevClip);

        // Cinema Top & Bottom Gold-Cyan Threat Rails + Bilingual Telemetry HUD
        g.setComposite(alphaComp(alpha));
        g.setColor(new Color(0x38BDF8));
        g.setStroke(roundStroke(2.6f));
        g.drawLine(0, bandY, w, bandY);
        g.setColor(new Color(0xFACC15));
        g.drawLine(0, bandY + bandH, w, bandY + bandH);

        String stageTag;
        String stageTitle;
        double progressPct;
        if (t < 0.25) {
            stageTag = "GIAI ĐOẠN 1/4 · HỒ SƠ MẬT AETHER ARK";
            stageTitle = "KHỞI ĐỘNG XƯỞNG NGẦM & DỰNG BẢN VẼ HOLOGRAM";
            progressPct = (t / 0.25) * 28.0;
        } else if (t < 0.52) {
            stageTag = "GIAI ĐOẠN 2/4 · HÀN GIÁP NANITE & LÒ PHẢN ỨNG";
            stageTitle = "CHẾ TẠO THÂN TÀU SIÊU HẠNG — LÕI AETHER ĐANG NẠP";
            progressPct = 28.0 + ((t - 0.25) / 0.27) * 42.0;
        } else if (t < 0.76) {
            stageTag = "GIAI ĐOẠN 3/4 · LẮP RÁP HỎA LỰC TỐI THƯỢNG";
            stageTitle = "TRANG BỊ ĐẠI PHÁO HYPERION & KHIÊN PHÉP TOÀN LỰC";
            progressPct = 70.0 + ((t - 0.52) / 0.24) * 26.0;
        } else {
            stageTag = "GIAI ĐOẠN 4/4 · AETHER ARK — XUẤT KÍCH!";
            stageTitle = "NIỀM HY VỌNG CUỐI CÙNG CỦA NHÂN LOẠI — KHỞI HÀNH!";
            progressPct = 100.0;
        }

        // Top Stage Tag Pill
        Font tagFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        g.setFont(tagFont);
        FontMetrics tfm = g.getFontMetrics();
        int tagW = tfm.stringWidth(stageTag) + 34;
        int tagX = (w - tagW) / 2;
        int tagY = bandY + 14;
        g.setColor(new Color(10, 22, 46, 232));
        g.fillRoundRect(tagX, tagY, tagW, 24, 12, 12);
        g.setColor(new Color(0x38BDF8));
        g.setStroke(roundStroke(1.5f));
        g.drawRoundRect(tagX, tagY, tagW, 24, 12, 12);
        g.setColor(new Color(0x67E8F9));
        g.drawString(stageTag, tagX + 17, tagY + 17);

        // Bottom Title & Fabrication Gauge Bar
        Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(15, Math.min(22, w / 46)));
        g.setFont(titleFont);
        FontMetrics mfm = g.getFontMetrics();
        int titleX = (w - mfm.stringWidth(stageTitle)) / 2;
        int titleY = bandY + bandH - 36;
        g.setColor(new Color(2, 6, 18));
        g.drawString(stageTitle, titleX + 2, titleY + 2);
        g.setColor(t >= 0.76 ? new Color(0xFDE047) : Color.WHITE);
        g.drawString(stageTitle, titleX, titleY);

        int barW = Math.min(460, w - 120);
        int barX = (w - barW) / 2;
        int barY = bandY + bandH - 22;
        g.setColor(new Color(15, 23, 42, 220));
        g.fillRoundRect(barX, barY, barW, 8, 6, 6);
        g.setColor(t >= 0.76 ? new Color(0xFACC15) : new Color(0x38BDF8));
        g.fillRoundRect(barX, barY, (int) Math.round(barW * (progressPct / 100.0)), 8, 6, 6);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA != null ? prevAA : RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setStroke(prevStroke);
        g.setComposite(prevComp);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static Color blendColor(Color a, Color b, double ratio) {
        double k = clamp01(ratio);
        int r = (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * k);
        int gr = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k);
        int bl = (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, gr)), Math.max(0, Math.min(255, bl)));
    }

    private static final class SuperShipCutsceneAnim {
        final Point dockDest;
        final int cellSize;
        long startMs;
        final long durationMs;
        final Runnable onCompleted;
        long lastSparkMs;
        boolean coreRingTriggered;
        boolean weaponSnapTriggered;
        boolean launchBlastTriggered;
        boolean completedTriggered;

        SuperShipCutsceneAnim(Point dockDest, int cellSize, long durationMs, Runnable onCompleted) {
            this.dockDest = dockDest;
            this.cellSize = cellSize;
            this.startMs = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.onCompleted = onCompleted;
        }
    }

    private static final class HyperionBeamAnim {
        final Point origin;
        final Point target;
        final List<Point> cellTargets;
        final int cellSize;
        final boolean lethalHit;
        final long startMs;
        final long durationMs;
        final Runnable onImpact;
        long lastSparkMs;
        boolean impactTriggered;

        HyperionBeamAnim(
                Point origin,
                Point target,
                List<Point> cellTargets,
                int cellSize,
                boolean lethalHit,
                long durationMs,
                Runnable onImpact
        ) {
            this.origin = origin;
            this.target = target;
            this.cellTargets = cellTargets;
            this.cellSize = cellSize;
            this.lethalHit = lethalHit;
            this.startMs = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.onImpact = onImpact;
        }
    }

    private void drawBossCityRampageCutscene(Graphics2D g, BossCityRampageAnim br, long now) {
        double t = clamp01((now - br.startMs) / (double) br.durationMs);
        int w = Math.max(640, getWidth());
        int h = Math.max(480, getHeight());

        double globalAlpha = t < 0.04 ? (t / 0.04) : (t > 0.93 ? Math.max(0.0, (1.0 - t) / 0.07) : 1.0);
        float alpha = (float) clamp01(globalAlpha);
        if (alpha <= 0.01f) return;

        boolean isGhidorah = (br.boss == TitanMode.TitanBoss.GHIDORAH);
        boolean isMothra = (br.boss == TitanMode.TitanBoss.MOTHRA);
        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        Object prevAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Shape prevClip = g.getClip();
        AffineTransform prevTx = g.getTransform();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int bandH = Math.min(h - 76, Math.max(360, (int) (h * 0.68)));
        int bandY = (h - bandH) / 2;

        // 0. Deep theatre dimmer + top/bottom alert letterbox tapes
        g.setComposite(alphaComp(alpha * 0.66f));
        g.setColor(new Color(2, 6, 16));
        g.fillRect(0, 0, w, h);

        Color accent = br.boss.accentColor;
        Color warnCol = isGhidorah ? new Color(0xFACC15) : (isMothra ? new Color(0x34D399) : new Color(0xEF4444));
        int tapeH = 22;
        String topTapeMsg = switch (br.boss) {
            case GHIDORAH -> "CẢNH BÁO SIÊU BÃO CẤP Ω · KING GHIDORAH ĐANG TÀN PHÁ TRUNG TÂM THÀNH PHỐ";
            case MOTHRA -> "HIỆN TƯỢNG THIÊN NHIÊN CẤP Ω · MOTHRA THỨC TỈNH TỪ KÉN ÁNH SÁNG GIỮA BÃO";
            case GODZILLA -> "BÁO ĐỘNG HẠT NHÂN · GODZILLA ĐANG ĐỔ BỘ PHÁ HỦY ĐÔ THỊ VEN BIỂN";
        };
        drawWarningTapeBar(g, 0, Math.max(0, bandY - tapeH), w, tapeH, (int) (t * 280), warnCol, new Color(0xFACC15), alpha,
                topTapeMsg);
        drawWarningTapeBar(g, 0, Math.min(h - tapeH, bandY + bandH), w, tapeH, -(int) (t * 240), accent, new Color(0xFACC15), alpha,
                "CAMERA NHÂN CHỨNG TRỰC TIẾP · POV ĐƯỜNG PHỐ · CHUẨN BỊ ĐỐI ĐẦU HẢI ĐỘI TITAN");

        g.setClip(0, bandY, w, bandH);

        // 1. Handheld street-level tremor & impact jolt computation
        double stompAmp = 0.0;
        if (br.boss == TitanMode.TitanBoss.GODZILLA) {
            for (double st : new double[]{0.08, 0.19, 0.29, 0.42}) {
                double dt = Math.abs(t - st);
                if (dt < 0.045) stompAmp = Math.max(stompAmp, (1.0 - dt / 0.045) * 11.0);
            }
            if (t >= 0.55 && t <= 0.83) stompAmp = Math.max(stompAmp, 8.5);
        } else if (isGhidorah) {
            for (double st : new double[]{0.30, 0.43, 0.56, 0.68, 0.78}) {
                double dt = Math.abs(t - st);
                if (dt < 0.055) stompAmp = Math.max(stompAmp, (1.0 - dt / 0.055) * 9.5);
            }
            if (t >= 0.54 && t <= 0.84) stompAmp = Math.max(stompAmp, 7.0);
        } else {
            // Mothra wingbeat turbulence & cloud-parting shockwaves
            for (double st : new double[]{0.28, 0.46, 0.62, 0.76}) {
                double dt = Math.abs(t - st);
                if (dt < 0.06) stompAmp = Math.max(stompAmp, (1.0 - dt / 0.06) * 7.8);
            }
        }
        double shakeX = Math.sin(now * 0.067) * (1.4 + stompAmp * 0.72);
        double shakeY = Math.cos(now * 0.089) * (1.0 + stompAmp);

        // 2. Apocalyptic Per-Boss City Sky & Searchlights / Supercell Clouds / Aurora God Rays
        g.setComposite(alphaComp(alpha * 0.98f));
        Color skyTop;
        Color skyBot;
        if (isMothra) {
            double auroraMix = clamp01((t - 0.34) / 0.46);
            skyTop = blendColor(new Color(0x061923), new Color(0x0F3D3E), auroraMix);
            skyBot = blendColor(new Color(0x0E3A4A), new Color(0xD97706), auroraMix * 0.62);
        } else if (!isGhidorah) {
            double fireMix = clamp01((t - 0.30) / 0.52);
            skyTop = blendColor(new Color(0x061224), new Color(0x1C0E1E), fireMix);
            skyBot = blendColor(new Color(0x123252), new Color(0x9A3412), fireMix * 0.85);
        } else {
            double stormPulse = (t >= 0.54 && t <= 0.84) ? (0.35 + 0.65 * Math.abs(Math.sin(t * 42.0))) : 0.18;
            skyTop = blendColor(new Color(0x041214), new Color(0x132F2B), stormPulse * 0.65);
            skyBot = blendColor(new Color(0x0D2428), new Color(0x854D0E), stormPulse * 0.55);
        }
        g.setPaint(new GradientPaint(0, bandY, skyTop, 0, bandY + bandH, skyBot));
        g.fillRect(0, bandY, w, bandH);

        // Distant glowing harbor water strip behind seawall
        int waterY = bandY + (int) (bandH * 0.72);
        g.setPaint(new GradientPaint(0, waterY, isMothra ? new Color(0x0F766E) : new Color(0x0C4A6E), 0, bandY + bandH, new Color(0x020617)));
        g.fillRect(0, waterY, w, bandY + bandH - waterY);

        // Searchlights (Godzilla), Storm Vortex (Ghidorah), or Glowing Aurora Cocoon Halo (Mothra)
        if (isMothra) {
            double cocoonGlow = clamp01(t / 0.35);
            drawGlow(g, new Color(0x34D399), w * 0.50, waterY - 30, 140.0 + cocoonGlow * 90.0, alpha * 0.55f, true);
            for (int p = 0; p < 16; p++) {
                double rise = ((now * 0.08 + p * 37.0) % (bandH * 0.85));
                int px = (int) (w * (0.14 + ((p * 29) % 72) / 100.0) + Math.sin(now * 0.004 + p) * 24.0);
                int py = (int) (bandY + bandH - rise);
                g.setComposite(alphaComp(alpha * 0.52f));
                g.setColor(p % 2 == 0 ? new Color(0x6EE7B7) : new Color(0xFEF08A));
                g.fillOval(px, py, 6, 6);
            }
        } else if (!isGhidorah) {
            double sweepA = Math.sin(t * 7.2) * 0.36;
            for (int i = 0; i < 2; i++) {
                int bx = (int) (w * (i == 0 ? 0.24 : 0.78));
                int topX = bx + (int) Math.round(Math.sin(sweepA + i * 1.4) * (w * 0.20));
                g.setComposite(alphaComp((float) (alpha * 0.16 * (1.0 - clamp01((t - 0.72) / 0.28)))));
                g.setColor(t < 0.42 ? new Color(0xE0F2FE) : new Color(0x38BDF8));
                Polygon cone = new Polygon(new int[]{bx - 16, bx + 16, topX + 78, topX - 78},
                        new int[]{bandY + bandH, bandY + bandH, bandY, bandY}, 4);
                g.fillPolygon(cone);
            }
            if (t < 0.34) {
                float sirenFlash = (float) Math.max(0.0, Math.sin(t * Math.PI * 12.0)) * alpha * 0.14f;
                g.setComposite(alphaComp(sirenFlash));
                g.setColor(new Color(0xEF4444));
                g.fillRect(0, bandY, w, bandH);
            }
        } else {
            g.setComposite(alphaComp(alpha * 0.42f));
            for (int c = 0; c < 6; c++) {
                int cx = (int) ((w * (0.14 + c * 0.16) + Math.sin(t * 4.5 + c) * 28.0));
                int cy = bandY + 26 + (c % 2) * 24;
                g.setColor(c % 2 == 0 ? new Color(0x0F3D38) : new Color(0x1E293B));
                g.fillOval(cx - 110, cy - 34, 220, 68);
            }
            if (t >= 0.52 && t <= 0.85 && Math.sin(t * 58.0) > 0.62) {
                g.setComposite(alphaComp(alpha * 0.22f));
                g.setColor(new Color(0xFEF08A));
                g.fillRect(0, bandY, w, bandH);
            }
        }

        // Apply handheld camera shake to city canyon, monster & beams!
        g.translate(shakeX, shakeY);

        // 3. Draw Colossal Monster Body Rising Behind Harbor Seawall & Between Skyscrapers
        double[] monsterRect = drawCityRampageMonsterBody(g, br, w, bandY, bandH, t, alpha, now);

        // 4. Downtown Skyscraper Canyon (with left/right side-tower blackout shading for Ghidorah only)
        BufferedImage cityImg = SpriteAssets.getPovCityBuildings();
        int cityH = (int) Math.round(bandH * 0.78);
        int cityW = Math.min(w + 24, (int) Math.round(cityH * 2.18));
        int cityX = (w - cityW) / 2;
        int cityY = bandY + bandH - cityH + 4;
        if (cityImg != null) {
            g.setComposite(alphaComp(alpha * 0.98f));
            SpriteAssets.drawSharp(g, cityImg, cityX, cityY, cityW, cityH);
            if (isGhidorah && t >= 0.20) {
                float blackout = (float) clamp01((t - 0.20) / 0.14) * 0.48f * alpha;
                g.setComposite(alphaComp(blackout));
                g.setColor(new Color(4, 9, 16));
                int flankW = (int) (cityW * 0.34);
                g.fillRect(cityX, cityY, flankW, cityH);
                g.fillRect(cityX + cityW - flankW, cityY, flankW, cityH);
            }
        }

        // 4.5 Blinding Atomic Breath / Triple Gravity Lightning & Destruction Blasts SLICING IN FRONT of Skyscrapers!
        drawCityRampageMonsterBeams(g, br, monsterRect, w, bandY, bandH, t, alpha, now);
        drawCityRampageDestructionFX(g, br, w, bandY, bandH, t, alpha, now);

        // 5. Reset shake slightly less on immediate foreground street ledge + Young Boy POV!
        g.setTransform(prevTx);
        g.translate(shakeX * 0.62, shakeY * 0.62);
        drawCityRampageForegroundAndBoy(g, br.boss, w, bandY, bandH, t, alpha, now);
        g.setTransform(prevTx);

        // 6. Slanted Boss Title Finale Card Slam (Beat 4: t >= 0.80)
        if (t >= 0.80) {
            drawCityRampageTitleSlam(g, br, w, bandY, bandH, (t - 0.80) / 0.20, alpha);
        }

        g.setClip(prevClip);

        // 7. First-Person Boy POV Witness Camera Finder & Vietnamese Dialogue Overlay
        drawCityRampageBoyHud(g, br, w, bandY, bandH, t, alpha, stompAmp, now);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA != null ? prevAA : RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setStroke(prevStroke);
        g.setComposite(prevComp);
    }

    private double[] drawCityRampageMonsterBody(
            Graphics2D g,
            BossCityRampageAnim br,
            int w,
            int bandY,
            int bandH,
            double t,
            float alpha,
            long now
    ) {
        if (br.boss == TitanMode.TitanBoss.GODZILLA) {
            BufferedImage gz = SpriteAssets.getGodzillaRampageSprite();
            double enter = clamp01(t / 0.52);
            double easeScale = 0.58 + 0.48 * Math.pow(enter, 0.78);
            double stompBob = (t < 0.46) ? Math.abs(Math.sin(t * 24.0)) * -10.0 : Math.sin(now * 0.008) * 3.0;
            int spriteH = (int) Math.round(bandH * 0.88 * easeScale);
            int spriteW = (int) Math.round(spriteH * (640.0 / 540.0));
            int gx = (w - spriteW) / 2 - (int) (w * 0.04 * (1.0 - enter));
            int gy = bandY + bandH - (int) (spriteH * 0.90) + (int) Math.round(stompBob);

            if (t >= 0.28) {
                double chg = clamp01((t - 0.28) / 0.27);
                float gInt = (float) ((0.35 + 0.60 * chg) * alpha);
                drawGlow(g, new Color(0x06B6D4), gx + spriteW * 0.58, gy + spriteH * 0.42, spriteW * (0.45 + 0.18 * chg), gInt, true);
                if (t < 0.56) {
                    for (int k = 0; k < 5; k++) {
                        double kPhase = clamp01((chg - k * 0.14) / 0.22);
                        if (kPhase > 0.01) {
                            double fx = gx + spriteW * (0.64 - k * 0.02);
                            double fy = gy + spriteH * (0.62 - k * 0.09);
                            drawGlow(g, new Color(0x67E8F9), fx, fy, 36.0 + kPhase * 28.0, (float) (kPhase * alpha * 0.95), true);
                        }
                    }
                }
            }
            if (gz != null) {
                g.setComposite(alphaComp(alpha * (float) clamp01(t / 0.08)));
                SpriteAssets.drawSharp(g, gz, gx, gy, spriteW, spriteH);
            }
            return new double[]{gx, gy, spriteW, spriteH};
        } else if (br.boss == TitanMode.TitanBoss.MOTHRA) {
            BufferedImage mt = SpriteAssets.getMothraRampageSprite();
            double enter = clamp01(t / 0.48);
            double ease = 1.0 - Math.pow(1.0 - enter, 2.3);
            double hover = Math.sin(now * 0.010) * 8.0;
            int spriteH = (int) Math.round(bandH * 0.82 * (0.56 + 0.44 * ease));
            int spriteW = (int) Math.round(spriteH * (760.0 / 540.0));
            int mx = (w - spriteW) / 2;
            int my = (int) Math.round((bandY + bandH * 0.42) + ((bandY + bandH * 0.04) - (bandY + bandH * 0.42)) * ease + hover);

            if (t >= 0.22) {
                double aura = clamp01((t - 0.22) / 0.30);
                drawGlow(g, new Color(0x34D399), mx + spriteW * 0.5, my + spriteH * 0.44, spriteW * (0.42 + 0.16 * aura), (float) (aura * 0.78 * alpha), true);
                drawGlow(g, new Color(0xFDE047), mx + spriteW * 0.5, my + spriteH * 0.40, spriteW * 0.28, (float) (aura * 0.65 * alpha), true);
            }
            if (mt != null) {
                g.setComposite(alphaComp(alpha * (float) clamp01(t / 0.10)));
                SpriteAssets.drawSharp(g, mt, mx, my, spriteW, spriteH);
            }
            return new double[]{mx, my, spriteW, spriteH};
        } else {
            BufferedImage kg = SpriteAssets.getGhidorahRampageSprite();
            double enter = clamp01(t / 0.46);
            double ease = 1.0 - Math.pow(1.0 - enter, 2.2);
            double flap = Math.sin(now * 0.011) * 9.0;
            int spriteH = (int) Math.round(bandH * 0.86 * (0.62 + 0.40 * ease));
            int spriteW = (int) Math.round(spriteH * (760.0 / 540.0));
            int kx = (w - spriteW) / 2;
            int ky = (int) Math.round((bandY - spriteH * 0.45) + (bandY + bandH * 0.04 - (bandY - spriteH * 0.45)) * ease + flap);

            if (t >= 0.24) {
                double aura = clamp01((t - 0.24) / 0.28);
                drawGlow(g, new Color(0xFACC15), kx + spriteW * 0.5, ky + spriteH * 0.45, spriteW * (0.44 + 0.16 * aura), (float) (aura * 0.82 * alpha), true);
            }
            if (kg != null) {
                g.setComposite(alphaComp(alpha * (float) clamp01(t / 0.10)));
                SpriteAssets.drawSharp(g, kg, kx, ky, spriteW, spriteH);
            }
            return new double[]{kx, ky, spriteW, spriteH};
        }
    }

    private void drawCityRampageMonsterBeams(
            Graphics2D g,
            BossCityRampageAnim br,
            double[] mRect,
            int w,
            int bandY,
            int bandH,
            double t,
            float alpha,
            long now
    ) {
        double mx = mRect[0];
        double my = mRect[1];
        double mw = mRect[2];
        double mh = mRect[3];
        if (br.boss == TitanMode.TitanBoss.GODZILLA) {
            if (t >= 0.28 && t < 0.54) {
                double p = clamp01((t - 0.28) / 0.26);
                double jawX = mx + mw * 0.27;
                double jawY = my + mh * 0.22;
                drawGlow(g, new Color(0x38BDF8), jawX, jawY, 22.0 + p * 44.0, (float) (p * alpha * 0.92), true);
            }
            // Beat 3: BLINDING ATOMIC BREATH JET tearing in front of skyscrapers left-to-right!
            if (t >= 0.54 && t <= 0.83) {
                double bp = clamp01((t - 0.54) / 0.28);
                double sweep = clamp01(bp * 1.15);
                double jawX = mx + mw * 0.27;
                double jawY = my + mh * 0.22;
                double endX = w * (0.14 + 0.74 * sweep);
                double endY = bandY + bandH * (0.64 - 0.28 * Math.sin(sweep * Math.PI));

                drawGlow(g, new Color(0x67E8F9), jawX, jawY, 72.0, alpha * 0.98f, true);
                g.setComposite(alphaComp(alpha * 0.92f));
                g.setColor(new Color(0x0284C7));
                g.setStroke(roundStroke(34.0f));
                g.drawLine((int) jawX, (int) jawY, (int) endX, (int) endY);

                g.setColor(new Color(0x22D3EE));
                g.setStroke(roundStroke(19.0f));
                g.drawLine((int) jawX, (int) jawY, (int) endX, (int) endY);

                g.setColor(new Color(0xECFEFF));
                g.setStroke(roundStroke(8.0f));
                g.drawLine((int) jawX, (int) jawY, (int) endX, (int) endY);

                // Skyscraper slice fireball & scorching molten plasma crater at beam impact!
                drawGlow(g, new Color(0xF97316), endX, endY, 96.0, alpha * 0.96f, true);
                drawGlow(g, new Color(0xFDE047), endX, endY, 54.0, alpha * 0.96f, true);
                drawGlow(g, Color.WHITE, endX, endY, 28.0, alpha * 0.98f, true);
            }
        } else if (br.boss == TitanMode.TitanBoss.MOTHRA) {
            double eyeL_x = mx + mw * 0.26, eyeL_y = my + mh * 0.34;
            double eyeR_x = mx + mw * 0.74, eyeR_y = my + mh * 0.34;
            double headX = mx + mw * 0.50, headY = my + mh * 0.36;
            if (t >= 0.26 && t < 0.55) {
                double p = clamp01((t - 0.26) / 0.29);
                drawGlow(g, new Color(0x6EE7B7), eyeL_x, eyeL_y, 26 + p * 38, (float) (p * alpha * 0.92), true);
                drawGlow(g, new Color(0x6EE7B7), eyeR_x, eyeR_y, 26 + p * 38, (float) (p * alpha * 0.92), true);
                drawGlow(g, new Color(0xFDE047), headX, headY, 22 + p * 34, (float) (p * alpha * 0.95), true);
            }
            // Beat 3: BLINDING GOD RAYS parting the storm clouds & illuminating city canyon IN FRONT of towers!
            if (t >= 0.52 && t <= 0.85) {
                double rayP = clamp01((t - 0.52) / 0.30);
                double[][] rayTargets = {{w * 0.22, bandY + bandH * 0.70}, {w * 0.50, bandY + bandH * 0.75}, {w * 0.78, bandY + bandH * 0.70}};
                for (int i = 0; i < 3; i++) {
                    double tx = rayTargets[i][0];
                    double ty = bandY + (rayTargets[i][1] - bandY) * rayP;
                    g.setComposite(alphaComp(alpha * 0.30f));
                    g.setColor(new Color(0xFDE047));
                    g.setStroke(roundStroke(44.0f));
                    g.drawLine((int) headX, (int) (bandY + 4), (int) tx, (int) ty);
                    g.setComposite(alphaComp(alpha * 0.62f));
                    g.setColor(new Color(0xA7F3D0));
                    g.setStroke(roundStroke(16.0f));
                    g.drawLine((int) headX, (int) (bandY + 4), (int) tx, (int) ty);
                    g.setComposite(alphaComp(alpha * 0.95f));
                    g.setColor(Color.WHITE);
                    g.setStroke(roundStroke(5.0f));
                    g.drawLine((int) headX, (int) (bandY + 4), (int) tx, (int) ty);
                    drawGlow(g, new Color(0xFDE047), tx, ty, 64.0, alpha * 0.90f, true);
                }
            }
        } else {
            double[][] heads = {
                    {mx + mw * 0.22, my + mh * 0.34},
                    {mx + mw * 0.45, my + mh * 0.16},
                    {mx + mw * 0.66, my + mh * 0.36}
            };
            // Beat 2: Golden lightning crackling across all 3 heads in full visibility!
            if (t >= 0.30 && t < 0.55) {
                for (double[] hp : heads) {
                    drawGlow(g, new Color(0xFEF08A), hp[0], hp[1], 38.0, alpha * 0.94f, true);
                }
                drawRampageLightningLine(g, heads[0][0], heads[0][1], heads[1][0], heads[1][1], new Color(0xFDE047), alpha * 0.96f, 5, now / 50L);
                drawRampageLightningLine(g, heads[1][0], heads[1][1], heads[2][0], heads[2][1], new Color(0xFACC15), alpha * 0.96f, 5, now / 50L + 17L);
            }
            // Beat 3: TRIPLE GRAVITY LIGHTNING BEAMS scorching all 3 zones in FRONT of towers!
            if (t >= 0.54 && t <= 0.83) {
                double[][] targets = {
                        {w * 0.17, bandY + bandH * 0.58},
                        {w * 0.50, bandY + bandH * 0.78},
                        {w * 0.83, bandY + bandH * 0.56}
                };
                for (int i = 0; i < 3; i++) {
                    drawGlow(g, new Color(0xFEF08A), heads[i][0], heads[i][1], 48.0, alpha * 0.96f, true);
                    drawRampageLightningLine(g, heads[i][0], heads[i][1], targets[i][0], targets[i][1], new Color(0xF59E0B), alpha * 0.92f, 11, now / 45L + i * 31L);
                    drawRampageLightningLine(g, heads[i][0], heads[i][1], targets[i][0], targets[i][1], Color.WHITE, alpha * 0.98f, 4.5f, now / 45L + i * 31L);
                    drawGlow(g, new Color(0xF59E0B), targets[i][0], targets[i][1], 88.0, alpha * 0.95f, true);
                    drawGlow(g, Color.WHITE, targets[i][0], targets[i][1], 36.0, alpha * 0.98f, true);
                }
            }
        }
    }

    private void drawRampageLightningLine(
            Graphics2D g,
            double x0,
            double y0,
            double x1,
            double y1,
            Color col,
            float alpha,
            float strokeW,
            long seed
    ) {
        g.setComposite(alphaComp(alpha));
        g.setColor(col);
        g.setStroke(roundStroke(strokeW));
        java.util.Random r = new java.util.Random(seed);
        int segs = 9;
        double px = x0;
        double py = y0;
        for (int s = 1; s <= segs; s++) {
            double f = s / (double) segs;
            double nx = x0 + (x1 - x0) * f + (s == segs ? 0.0 : (r.nextDouble() - 0.5) * 34.0);
            double ny = y0 + (y1 - y0) * f + (s == segs ? 0.0 : (r.nextDouble() - 0.5) * 26.0);
            g.drawLine((int) px, (int) py, (int) nx, (int) ny);
            px = nx;
            py = ny;
        }
    }

    private void drawCityRampageDestructionFX(
            Graphics2D g,
            BossCityRampageAnim br,
            int w,
            int bandY,
            int bandH,
            double t,
            float alpha,
            long now
    ) {
        if (br.boss == TitanMode.TitanBoss.MOTHRA) {
            // Luminous emerald-gold Mothra pollen sparkles & gale streaks swirling down the avenue
            if (t >= 0.26 && t <= 0.92) {
                for (int b = 0; b < 24; b++) {
                    double bx = w * (0.08 + ((b * 41 + now * 0.14) % 86) / 100.0);
                    double by = bandY + bandH * (0.14 + ((b * 31) % 72) / 100.0) + Math.sin(now * 0.008 + b) * 14.0;
                    g.setComposite(alphaComp(alpha * 0.78f));
                    g.setColor(b % 3 == 0 ? new Color(0xFDE047) : (b % 2 == 0 ? new Color(0x34D399) : new Color(0xE0F2FE)));
                    int sz = 5 + (b % 3) * 2;
                    g.fillRect((int) bx, (int) by, sz, sz);
                }
            }
            return;
        }
        boolean isGhidorah = (br.boss == TitanMode.TitanBoss.GHIDORAH);
        // Falling/exploding voxel rubble & fireballs around hit skyscrapers (Beat 2 & Beat 3)
        if (t >= 0.34 && t <= 0.90) {
            double p = clamp01((t - 0.34) / 0.56);
            int fireSites = isGhidorah ? 3 : 4;
            double[] siteX = {0.16, 0.28, 0.74, 0.86};
            double[] siteY = {0.58, 0.44, 0.48, 0.62};
            for (int i = 0; i < fireSites; i++) {
                double pulse = 0.55 + 0.45 * Math.sin(now * 0.015 + i * 2.1);
                double fx = w * siteX[i];
                double fy = bandY + bandH * siteY[i];
                Color glowC = isGhidorah ? new Color(0xFACC15) : new Color(0xFB923C);
                drawGlow(g, glowC, fx, fy, 42.0 + 34.0 * p * pulse, (float) (alpha * 0.72 * Math.min(1.0, p * 2.2)), true);
            }
            // Flying voxel concrete blocks & fiery debris shards
            for (int b = 0; b < 18; b++) {
                double bx = w * (0.12 + ((b * 43) % 76) / 100.0);
                double lift = isGhidorah ? (-((now * 0.14 + b * 29) % 140.0)) : (((now * 0.18 + b * 37) % 150.0) - 35.0);
                double by = bandY + bandH * (0.36 + (b % 5) * 0.08) + lift;
                if (by >= bandY && by <= bandY + bandH) {
                    g.setComposite(alphaComp(alpha * 0.86f));
                    g.setColor(b % 3 == 0 ? (isGhidorah ? new Color(0xFEF08A) : new Color(0x38BDF8))
                            : (b % 2 == 0 ? new Color(0x475569) : new Color(0xFB923C)));
                    int sz = 5 + (b % 4) * 2;
                    g.fillRect((int) bx, (int) by, sz, sz);
                }
            }
        }
    }

    private void drawCityRampageForegroundAndBoy(
            Graphics2D g,
            TitanMode.TitanBoss boss,
            int w,
            int bandY,
            int bandH,
            double t,
            float alpha,
            long now
    ) {
        // Pouring wind-slanted rain for Ghidorah/Mothra storm or glowing atomic ash embers for Godzilla!
        if (boss == TitanMode.TitanBoss.GHIDORAH || boss == TitanMode.TitanBoss.MOTHRA) {
            boolean mothraClearing = (boss == TitanMode.TitanBoss.MOTHRA && t > 0.56);
            float rainA = alpha * (mothraClearing ? Math.max(0.10f, (float) (0.45 * (1.0 - (t - 0.56) / 0.35))) : 0.48f);
            g.setComposite(alphaComp(rainA));
            g.setColor(boss == TitanMode.TitanBoss.MOTHRA ? new Color(0xA7F3D0) : new Color(0xBAE6FD));
            g.setStroke(roundStroke(1.5f));
            double phase = (now * 0.55) % 90.0;
            for (int r = 0; r < 52; r++) {
                int rx = (int) ((r * 37 + phase * 1.6) % (w + 140)) - 60;
                int ry = bandY + (int) ((r * 59 + phase * 2.4) % (bandH + 40)) - 20;
                g.drawLine(rx, ry, rx - 22, ry + 32);
            }
        } else if (t >= 0.25) {
            float emberAlpha = (float) (alpha * clamp01((t - 0.25) / 0.20) * 0.72);
            g.setComposite(alphaComp(emberAlpha));
            for (int e = 0; e < 26; e++) {
                int ex = (int) ((e * 61 + now * 0.11 + Math.sin(now * 0.005 + e) * 18.0) % w);
                int ey = bandY + bandH - (int) ((e * 47 + now * 0.09) % bandH);
                g.setColor(e % 3 == 0 ? new Color(0x38BDF8) : new Color(0xFB923C));
                g.fillRect(ex, ey, 4, 4);
            }
        }

        // Foreground Stalled Car + Guardrail + Young Boy Looking Upward (Lower-Left Street Overpass)
        BufferedImage boyImg = SpriteAssets.getPovBoyForeground();
        if (boyImg != null) {
            int boyH = Math.max(115, Math.min(182, (int) (bandH * 0.40)));
            int boyW = (int) Math.round(boyH * (480.0 / 280.0));
            int bx = 12;
            int by = bandY + bandH - boyH + 6;
            // Warm car hazard light blink + smartphone screen illumination
            if (Math.sin(now * 0.016) > 0.1) {
                drawGlow(g, new Color(0xF59E0B), bx + boyW * 0.18, by + boyH * 0.74, 34.0, alpha * 0.72f, true);
            }
            Color faceGlow = switch (boss) {
                case GHIDORAH -> new Color(0xFEF08A);
                case MOTHRA -> new Color(0x6EE7B7);
                case GODZILLA -> new Color(0x38BDF8);
            };
            drawGlow(g, faceGlow, bx + boyW * 0.62, by + boyH * 0.24, 38.0, alpha * 0.50f, true);
            g.setComposite(alphaComp(alpha));
            SpriteAssets.drawSharp(g, boyImg, bx, by, boyW, boyH);
        }
    }

    private void drawCityRampageTitleSlam(
            Graphics2D g,
            BossCityRampageAnim br,
            int w,
            int bandY,
            int bandH,
            double p,
            float alpha
    ) {
        double ease = 1.0 - Math.pow(1.0 - clamp01(p * 1.45), 3.0);
        int cardW = Math.min(w - 80, 620);
        int cardH = 128;
        int skew = 26;
        int ix = (int) Math.round(w + 40 + ((w - cardW) * 0.5 - (w + 40)) * ease);
        int iy = bandY + (bandH - cardH) / 2 - 12;
        Color col = br.boss.accentColor;

        Polygon poly = new Polygon(new int[]{ix + skew, ix + cardW + skew, ix + cardW, ix},
                new int[]{iy, iy, iy + cardH, iy + cardH}, 4);
        g.setComposite(alphaComp(alpha * 0.94f));
        Color gradEnd = switch (br.boss) {
            case GODZILLA -> new Color(8, 42, 74);
            case GHIDORAH -> new Color(56, 36, 6);
            case MOTHRA -> new Color(8, 52, 42);
        };
        g.setPaint(new GradientPaint(ix, iy, new Color(8, 14, 28), ix + cardW, iy + cardH, gradEnd));
        g.fillPolygon(poly);
        g.setColor(col);
        g.setStroke(roundStroke(3.0f));
        g.drawPolygon(poly);

        BufferedImage portrait = SpriteAssets.getTitanSprite(br.boss, false, false);
        if (portrait != null) {
            int pSz = 138;
            SpriteAssets.drawSharp(g, portrait, ix + cardW - pSz - 12, iy - 6, pSz, pSz);
        }
        int tx = ix + skew + 22;
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.setColor(br.boss == TitanMode.TitanBoss.MOTHRA ? new Color(0x6EE7B7) : new Color(0xF87171));
        g.drawString("ĐẠI HỌA TITAN CẤP Ω · ĐANG TIẾN VÀO HẢI PHẬN", tx, iy + 30);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 30));
        g.setColor(Color.WHITE);
        String title = switch (br.boss) {
            case GODZILLA -> "GODZILLA";
            case GHIDORAH -> "KING GHIDORAH";
            case MOTHRA -> "MOTHRA";
        };
        g.drawString(title, tx, iy + 68);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        g.setColor(col.brighter());
        String sub = switch (br.boss) {
            case GODZILLA -> "VUA QUÁI VẬT · HƠI THỞ NGUYÊN TỬ";
            case GHIDORAH -> "HOÀNG ĐẾ BÃO TỐ · TAM ĐẦU LÔI ĐIỆN";
            case MOTHRA -> "NỮ HOÀNG THIÊN NHIÊN · THÁNH QUANG RẼ MÂY";
        };
        g.drawString(sub, tx, iy + 96);
    }

    private void drawCityRampageBoyHud(
            Graphics2D g,
            BossCityRampageAnim br,
            int w,
            int bandY,
            int bandH,
            double t,
            float alpha,
            double tremor,
            long now
    ) {
        Color hudCol = br.boss.accentColor;

        // Camera Viewfinder L-Brackets inside 1st-Person POV frame
        g.setComposite(alphaComp(alpha * 0.85f));
        g.setColor(hudCol);
        g.setStroke(roundStroke(2.2f));
        int m = 14;
        int arm = 18;
        int top = bandY + m;
        int bot = bandY + bandH - m;
        int lft = m;
        int rgt = w - m;
        g.drawLine(lft, top, lft + arm, top); g.drawLine(lft, top, lft, top + arm);
        g.drawLine(rgt - arm, top, rgt, top); g.drawLine(rgt, top, rgt, top + arm);
        g.drawLine(lft, bot, lft + arm, bot); g.drawLine(lft, bot - arm, lft, bot);
        g.drawLine(rgt - arm, bot, rgt, bot); g.drawLine(rgt, bot - arm, rgt, bot);

        // Top-Left Eyewitness REC pill & tremor seismograph bars
        int recX = lft + 10;
        int recY = top + 8;
        g.setColor(new Color(8, 16, 32, 215));
        g.fillRoundRect(recX, recY, 224, 26, 12, 12);
        g.setColor(hudCol);
        g.setStroke(roundStroke(1.3f));
        g.drawRoundRect(recX, recY, 224, 26, 12, 12);
        if ((now / 320L) % 2L == 0L) {
            g.setColor(new Color(0xEF4444));
            g.fillOval(recX + 10, recY + 8, 9, 9);
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.setColor(Color.WHITE);
        g.drawString("REC · POV CẬU BÉ · CHẤN ĐỘNG", recX + 24, recY + 17);

        // Micro seismograph bars beside REC
        for (int b = 0; b < 5; b++) {
            int bh = Math.max(4, Math.min(18, (int) (5 + tremor * 1.1 + ((now / 45L + b * 3) % 7))));
            g.setColor(bh > 12 ? new Color(0xEF4444) : hudCol);
            g.fillRect(recX + 232 + b * 5, recY + 22 - bh, 3, bh);
        }

        // Boy Witness Eyewitness Dialogue Caption anchored cleanly at Bottom-Right beside the boy foreground!
        String quote;
        if (br.boss == TitanMode.TitanBoss.GODZILLA) {
            if (t < 0.28) quote = "Cậu bé: \"Cái bóng khổng lồ gì đang bước vào đại lộ vậy?! Mặt đất rung dữ quá!!\"";
            else if (t < 0.55) quote = "Cậu bé: \"Vây lưng nó phát sáng xanh rực hết rồi... chạy mau lên!!\"";
            else if (t < 0.83) quote = "Cậu bé: \"KHÔNG!! Nó khạc tia hủy diệt xé toạc các tòa nhà rồi!!\"";
            else quote = "Cậu bé: \"Tàu chiến của ta ơi... xin hãy chặn đứng Godzilla!!\"";
        } else if (br.boss == TitanMode.TitanBoss.MOTHRA) {
            if (t < 0.28) quote = "Cậu bé: \"Bầu trời bỗng phát sáng xanh ngọc... cái kén khổng lồ trên vịnh vừa nứt ra!!\"";
            else if (t < 0.55) quote = "Cậu bé: \"Đẹp quá... Đôi cánh ánh sáng của Mothra che kín cả đường chân trời luôn!!\"";
            else if (t < 0.83) quote = "Cậu bé: \"Cơn bão bị rẽ tan rồi! Cuồng phong phấn hoa đang thổi tung mặt biển!!\"";
            else quote = "Cậu bé: \"Nữ hoàng Mothra đang bay ra đại dương... hạm đội mau giữ vững đội hình!!\"";
        } else {
            if (t < 0.28) quote = "Cậu bé: \"Bão đen ở đâu ập tới vậy?! Cả thành phố mất điện hết rồi!!\"";
            else if (t < 0.55) quote = "Cậu bé: \"RỒNG VÀNG BA ĐẦU?! Đôi cánh nó thổi bay cả tháp thép kìa!!\"";
            else if (t < 0.83) quote = "Cậu bé: \"Trời ơi!! Ba tia sét trọng lực đang nghiền nát cả khu phố!!\"";
            else quote = "Cậu bé: \"King Ghidorah tiến ra vịnh rồi... hạm đội mau nghênh chiến!!\"";
        }
        int boyReservedW = Math.min(310, Math.max(210, (int) (w * 0.29)));
        int pillW = Math.min(w - boyReservedW - 34, 620);
        int pillH = 44;
        int pillX = Math.max(boyReservedW + 14, w - pillW - 22);
        int pillY = bandY + bandH - pillH - 16;

        g.setComposite(alphaComp(alpha * 0.92f));
        g.setColor(new Color(7, 13, 26, 232));
        g.fillRoundRect(pillX, pillY, pillW, pillH, 14, 14);
        g.setColor(hudCol);
        g.setStroke(roundStroke(1.6f));
        g.drawRoundRect(pillX, pillY, pillW, pillH, 14, 14);

        Font qFont = new Font(Font.SANS_SERIF, Font.BOLD, w < 1040 ? 12 : 14);
        g.setFont(qFont);
        FontMetrics fm = g.getFontMetrics();
        int qx = pillX + 16;
        int qy = pillY + ((pillH - fm.getHeight()) / 2) + fm.getAscent();
        g.setColor(Color.WHITE);
        // Clip string inside pill neatly so it never overflows on narrow windows
        Shape innerClip = g.getClip();
        g.clipRect(pillX + 10, pillY + 2, pillW - 20, pillH - 4);
        g.drawString(quote, qx, qy);
        g.setClip(innerClip);
    }

    private static final class BossCityRampageAnim {
        final TitanMode.TitanBoss boss;
        final Point bossCenter;
        final int cellSize;
        long startMs;
        final long durationMs;
        final Runnable onCompleted;
        long lastFxMs;
        boolean beamBurstTriggered;
        boolean finaleSlamTriggered;
        boolean completedTriggered;

        BossCityRampageAnim(
                TitanMode.TitanBoss boss,
                Point bossCenter,
                int cellSize,
                long durationMs,
                Runnable onCompleted
        ) {
            this.boss = boss;
            this.bossCenter = bossCenter;
            this.cellSize = cellSize;
            this.startMs = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.onCompleted = onCompleted;
        }
    }

    private void drawEnvironmentWave(Graphics2D g, EnvironmentWaveAnim ew, long now) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 40 || h <= 40) return;
        double p = clamp01((now - ew.startMs) / (double) ew.durationMs);
        double env = Math.min(1.0, p / 0.14) * (p > 0.45 ? Math.max(0.0, 1.0 - (p - 0.45) / 0.55) : 1.0);
        if (env <= 0.02) return;

        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        double dirX = ew.vx >= 0 ? 1.0 : -1.0;

        for (int i = 0; i < 14; i++) {
            double cycle = (p * (1.35 + (i % 3) * 0.22) + i * 0.09) % 1.0;
            double travelX = dirX >= 0 ? cycle : (1.0 - cycle);
            int x0 = (int) Math.round(travelX * (w + 260) - 130);
            int y0 = (int) Math.round(h * (0.12 + ((i * 19) % 76) / 100.0) + Math.sin(p * 11.0 + i) * 14.0);
            int len = 90 + (i % 4) * 45;
            int dy = (int) Math.round(ew.vy * 18.0);
            float alpha = (float) (env * Math.sin(cycle * Math.PI) * 0.34);
            if (alpha > 0.02f) {
                g.setComposite(alphaComp(alpha));
                g.setColor(i % 3 == 0 ? Color.WHITE : ew.tint);
                g.setStroke(roundStroke(i % 2 == 0 ? 3.2f : 2.0f));
                g.drawLine(x0, y0, x0 + (int) (dirX * len), y0 + dy);
            }
        }

        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private static final class EnvironmentWaveAnim {
        final Color tint;
        final double vx;
        final double vy;
        final long startMs;
        final long durationMs;

        EnvironmentWaveAnim(Color tint, double vx, double vy, long durationMs) {
            this.tint = tint;
            this.vx = vx;
            this.vy = vy;
            this.startMs = System.currentTimeMillis();
            this.durationMs = durationMs;
        }
    }
}
