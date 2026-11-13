package battleship.ui;

import battleship.model.ShipType;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads and caches pre-rendered Blender 3D sprites for ships, weapons, and battle effects. */
public final class SpriteAssets {
    public enum FxType {
        HIT("hit"),
        MISS("miss"),
        FIRE("fire"),
        LIGHTNING("lightning"),
        SONAR("sonar"),
        FROST("frost");

        public final String prefix;
        FxType(String prefix) { this.prefix = prefix; }
    }

    public enum DragonSpecies {
        ARCANE("dragon", "Hải Long Cổ Ngữ"),
        SAKURA("dragon_sakura", "Rồng Hoa Anh Đào"),
        SOLAR("dragon_solar", "Hỏa Long Mặt Trời"),
        STORM("dragon_storm", "Lôi Long Bão Tố"),
        FROST("dragon_frost", "Băng Long Cực Quang"),
        ZOMBIE("dragon_zombie", "Rồng Xương Dịch Hạch");

        public final String prefix;
        public final String displayName;
        DragonSpecies(String prefix, String displayName) {
            this.prefix = prefix;
            this.displayName = displayName;
        }
    }

    private static final int FX_FRAME_COUNT = 8;
    private static final int DRAGON_FRAME_COUNT = 4;
    private static final int MUZZLE_FRAME_COUNT = 4;
    private static final Map<ShipType, BufferedImage> SHIP_H = new EnumMap<>(ShipType.class);
    private static final Map<ShipType, BufferedImage> SHIP_V = new EnumMap<>(ShipType.class);
    private static final Map<ShipType, BufferedImage> SHIP_SHADOW_H = new EnumMap<>(ShipType.class);
    private static final Map<ShipType, BufferedImage> SHIP_SHADOW_V = new EnumMap<>(ShipType.class);
    private static final Map<ShipType, BufferedImage> ZOMBIE_SHIP_H = new EnumMap<>(ShipType.class);
    private static final Map<ShipType, BufferedImage> ZOMBIE_SHIP_V = new EnumMap<>(ShipType.class);
    private static final Map<ShipType, BufferedImage> ZOMBIE_SHADOW_H = new EnumMap<>(ShipType.class);
    private static final Map<ShipType, BufferedImage> ZOMBIE_SHADOW_V = new EnumMap<>(ShipType.class);
    private static final Map<CombatLoadout.WeaponOption, BufferedImage> WEAPONS =
            new EnumMap<>(CombatLoadout.WeaponOption.class);
    private static final Map<CombatLoadout.WeaponOption, BufferedImage> PROJECTILES =
            new EnumMap<>(CombatLoadout.WeaponOption.class);
    private static final Map<ZombieMode.ZombieClass, BufferedImage> ZOMBIE_PROJECTILES =
            new EnumMap<>(ZombieMode.ZombieClass.class);
    private static final Map<TitanMode.TitanSkill, BufferedImage> TITAN_SKILLS =
            new EnumMap<>(TitanMode.TitanSkill.class);
    private static BufferedImage titanGzIdle;
    private static BufferedImage titanGzCharge;
    private static BufferedImage titanGzBurning;
    private static BufferedImage titanGzIdleShadow;
    private static BufferedImage titanGzBurningShadow;
    private static BufferedImage titanKgIdle;
    private static BufferedImage titanKgStorm;
    private static BufferedImage titanKgIdleShadow;
    private static BufferedImage titanKgStormShadow;
    private static BufferedImage titanMtIdle;
    private static BufferedImage titanMtEnrage;
    private static BufferedImage titanMtIdleShadow;
    private static BufferedImage titanMtEnrageShadow;
    private static BufferedImage povBoyForeground;
    private static BufferedImage povCityBuildings;
    private static BufferedImage godzillaRampageVoxel;
    private static BufferedImage ghidorahRampageVoxel;
    private static BufferedImage mothraRampageVoxel;
    private static BufferedImage superShipH;
    private static BufferedImage superShipV;
    private static BufferedImage superShipShadowH;
    private static BufferedImage superShipShadowV;
    private static BufferedImage superShipHangar;
    private static BufferedImage superShipWeapons;
    private static BufferedImage superShipIcon;
    private static final Map<CombatLoadout.MagicOption, BufferedImage> MAGIC_RUNES =
            new EnumMap<>(CombatLoadout.MagicOption.class);
    private static final Map<FxType, BufferedImage[]> FX_FRAMES = new EnumMap<>(FxType.class);
    private static final Map<DragonSpecies, BufferedImage[]> DRAGON_FRAMES_BY_SPECIES =
            new EnumMap<>(DragonSpecies.class);
    private static final Map<DragonSpecies, BufferedImage[]> DRAGON_SHADOWS_BY_SPECIES =
            new EnumMap<>(DragonSpecies.class);
    private static final BufferedImage[] DRAGON_FRAMES = new BufferedImage[DRAGON_FRAME_COUNT];
    private static final BufferedImage[] DRAGON_SHADOWS = new BufferedImage[DRAGON_FRAME_COUNT];
    private static final BufferedImage[] MUZZLE_FRAMES = new BufferedImage[MUZZLE_FRAME_COUNT];
    private static BufferedImage magicShield;
    private static final Map<String, BufferedImage> MAP_PROPS = Collections.synchronizedMap(new java.util.HashMap<>());
    private static final java.util.Set<DragonSpecies> LOADED_DRAGONS = Collections.synchronizedSet(java.util.EnumSet.noneOf(DragonSpecies.class));
    private static final Object LOCK_SHIPS = new Object();
    private static final Object LOCK_ZOMBIE_SHIPS = new Object();
    private static final Object LOCK_WEAPONS = new Object();
    private static final Object LOCK_PROJECTILES = new Object();
    private static final Object LOCK_ZOMBIE_PROJECTILES = new Object();
    private static final Object LOCK_TITANS = new Object();
    private static final Object LOCK_RUNES = new Object();
    private static final Object LOCK_FX = new Object();
    private static final Object LOCK_SHIELD_MUZZLE = new Object();
    private static final Object LOCK_DRAGONS = new Object();
    private static final Object LOCK_PROPS = new Object();

    private static volatile boolean shipsLoaded;
    private static volatile boolean zombieShipsLoaded;
    private static volatile boolean weaponsLoaded;
    private static volatile boolean projectilesLoaded;
    private static volatile boolean zombieProjectilesLoaded;
    private static volatile boolean titansLoaded;
    private static volatile boolean magicRunesLoaded;
    private static volatile boolean fxLoaded;
    private static volatile boolean shieldMuzzleLoaded;
    private static volatile boolean allMapPropsLoaded;
    private static volatile boolean warmingUp;

    private SpriteAssets() { }

    public static void warmUpAsync() {
        if (warmingUp) return;
        warmingUp = true;
        Thread t = new Thread(() -> {
            preloadEssential();
            preload();
        }, "battleship-sprite-warmup");
        t.setDaemon(true);
        t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
        t.start();
    }

    public static void preloadEssential() {
        ensureShipsLoaded();
    }

    public static void preload() {
        ensureShipsLoaded();
        ensureZombieShipsLoaded();
        ensureWeaponsLoaded();
        ensureProjectilesLoaded();
        ensureZombieProjectilesLoaded();
        ensureTitansLoaded();
        ensureMagicRunesLoaded();
        ensureFxLoaded();
        ensureShieldAndMuzzleLoaded();
        for (DragonSpecies sp : DragonSpecies.values()) ensureDragonLoaded(sp);
        ensureAllMapPropsLoaded();
    }

    private static void ensureShipsLoaded() {
        if (shipsLoaded) return;
        synchronized (LOCK_SHIPS) {
            if (shipsLoaded) return;
            loadShip(ShipType.CARRIER, "/sprites/ships/carrier.png");
            loadShip(ShipType.BATTLESHIP, "/sprites/ships/battleship.png");
            loadShip(ShipType.CRUISER, "/sprites/ships/cruiser.png");
            loadShip(ShipType.SUBMARINE, "/sprites/ships/submarine.png");
            loadShip(ShipType.DESTROYER, "/sprites/ships/destroyer.png");
            superShipH = readImage("/sprites/ships/supership.png");
            if (superShipH != null) {
                superShipV = rotate90Clockwise(superShipH);
                superShipShadowH = buildDropShadow(superShipH);
                superShipShadowV = buildDropShadow(superShipV);
            }
            superShipHangar = readImage("/sprites/ships/supership_hangar.png");
            superShipWeapons = readImage("/sprites/ships/supership_weapons.png");
            superShipIcon = readImage("/sprites/ships/supership_icon.png");
            shipsLoaded = true;
        }
    }

    private static void ensureZombieShipsLoaded() {
        if (zombieShipsLoaded) return;
        synchronized (LOCK_ZOMBIE_SHIPS) {
            if (zombieShipsLoaded) return;
            loadZombieShip(ShipType.CARRIER, "/sprites/zombies/zombie_carrier.png");
            loadZombieShip(ShipType.BATTLESHIP, "/sprites/zombies/zombie_battleship.png");
            loadZombieShip(ShipType.CRUISER, "/sprites/zombies/zombie_cruiser.png");
            loadZombieShip(ShipType.SUBMARINE, "/sprites/zombies/zombie_submarine.png");
            loadZombieShip(ShipType.DESTROYER, "/sprites/zombies/zombie_destroyer.png");
            zombieShipsLoaded = true;
        }
    }

    private static void ensureWeaponsLoaded() {
        if (weaponsLoaded) return;
        synchronized (LOCK_WEAPONS) {
            if (weaponsLoaded) return;
            WEAPONS.put(CombatLoadout.WeaponOption.CANNON, readImage("/sprites/weapons/cannon.png"));
            WEAPONS.put(CombatLoadout.WeaponOption.TORPEDO, readImage("/sprites/weapons/torpedo.png"));
            WEAPONS.put(CombatLoadout.WeaponOption.MISSILE_CROSS, readImage("/sprites/weapons/missile.png"));
            WEAPONS.put(CombatLoadout.WeaponOption.AIRSTRIKE, readImage("/sprites/weapons/airstrike.png"));
            weaponsLoaded = true;
        }
    }

    private static void ensureProjectilesLoaded() {
        if (projectilesLoaded) return;
        synchronized (LOCK_PROJECTILES) {
            if (projectilesLoaded) return;
            PROJECTILES.put(CombatLoadout.WeaponOption.CANNON, readImage("/sprites/projectiles/cannon_proj.png"));
            PROJECTILES.put(CombatLoadout.WeaponOption.TORPEDO, readImage("/sprites/projectiles/torpedo_proj.png"));
            PROJECTILES.put(CombatLoadout.WeaponOption.MISSILE_CROSS, readImage("/sprites/projectiles/missile_proj.png"));
            PROJECTILES.put(CombatLoadout.WeaponOption.AIRSTRIKE, readImage("/sprites/projectiles/airstrike_proj.png"));
            projectilesLoaded = true;
        }
    }

    private static void ensureZombieProjectilesLoaded() {
        if (zombieProjectilesLoaded) return;
        synchronized (LOCK_ZOMBIE_PROJECTILES) {
            if (zombieProjectilesLoaded) return;
            ZOMBIE_PROJECTILES.put(ZombieMode.ZombieClass.PLAGUE_DESTROYER, readImage("/sprites/projectiles/zombie_acid_proj.png"));
            ZOMBIE_PROJECTILES.put(ZombieMode.ZombieClass.BLIGHT_SUBMARINE, readImage("/sprites/projectiles/zombie_spore_proj.png"));
            ZOMBIE_PROJECTILES.put(ZombieMode.ZombieClass.NECRO_CRUISER, readImage("/sprites/projectiles/zombie_curse_proj.png"));
            ZOMBIE_PROJECTILES.put(ZombieMode.ZombieClass.BLOOD_BATTLESHIP, readImage("/sprites/projectiles/zombie_meteor_proj.png"));
            ZOMBIE_PROJECTILES.put(ZombieMode.ZombieClass.ABYSS_CARRIER, readImage("/sprites/projectiles/zombie_swarm_proj.png"));
            zombieProjectilesLoaded = true;
        }
    }

    private static void ensureTitansLoaded() {
        if (titansLoaded) return;
        synchronized (LOCK_TITANS) {
            if (titansLoaded) return;
            titanGzIdle = readImage("/sprites/titans/godzilla_idle.png");
            titanGzCharge = readImage("/sprites/titans/godzilla_charge.png");
            titanGzBurning = readImage("/sprites/titans/godzilla_burning.png");
            if (titanGzIdle != null) titanGzIdleShadow = buildDropShadow(titanGzIdle);
            if (titanGzBurning != null) titanGzBurningShadow = buildDropShadow(titanGzBurning);

            titanKgIdle = readImage("/sprites/titans/ghidorah_idle.png");
            titanKgStorm = readImage("/sprites/titans/ghidorah_storm.png");
            if (titanKgIdle != null) titanKgIdleShadow = buildDropShadow(titanKgIdle);
            if (titanKgStorm != null) titanKgStormShadow = buildDropShadow(titanKgStorm);

            titanMtIdle = readImage("/sprites/titans/mothra_idle.png");
            titanMtEnrage = readImage("/sprites/titans/mothra_enrage.png");
            if (titanMtIdle != null) titanMtIdleShadow = buildDropShadow(titanMtIdle);
            if (titanMtEnrage != null) titanMtEnrageShadow = buildDropShadow(titanMtEnrage);

            TITAN_SKILLS.put(TitanMode.TitanSkill.GZ_TAIL_SLAM, readImage("/sprites/titans/skill_godzilla_tail.png"));
            TITAN_SKILLS.put(TitanMode.TitanSkill.GZ_ATOMIC_BREATH, readImage("/sprites/titans/skill_godzilla_atomic.png"));
            TITAN_SKILLS.put(TitanMode.TitanSkill.GZ_THERMO_PULSE, readImage("/sprites/titans/skill_godzilla_pulse.png"));
            TITAN_SKILLS.put(TitanMode.TitanSkill.GZ_SPIRAL_RAY, readImage("/sprites/titans/skill_godzilla_spiral.png"));
            TITAN_SKILLS.put(TitanMode.TitanSkill.GZ_MELTDOWN_RIFT, readImage("/sprites/titans/skill_godzilla_meltdown.png"));
            TITAN_SKILLS.put(TitanMode.TitanSkill.KG_TRIPLE_GRAVITY, readImage("/sprites/titans/skill_ghidorah_gravity.png"));
            TITAN_SKILLS.put(TitanMode.TitanSkill.KG_HURRICANE_WING, readImage("/sprites/titans/skill_ghidorah_hurricane.png"));
            TITAN_SKILLS.put(TitanMode.TitanSkill.KG_OMEGA_VORTEX, readImage("/sprites/titans/skill_ghidorah_omega.png"));
            TITAN_SKILLS.put(TitanMode.TitanSkill.KG_ALPHA_STORM, readImage("/sprites/titans/skill_ghidorah_alpha.png"));
            TITAN_SKILLS.put(TitanMode.TitanSkill.KG_CATACLYSM_GRAVITY, readImage("/sprites/titans/skill_ghidorah_cataclysm.png"));
            TITAN_SKILLS.put(TitanMode.TitanSkill.MT_WING_GALE, readImage("/sprites/titans/skill_mothra_gale.png"));
            TITAN_SKILLS.put(TitanMode.TitanSkill.MT_GOD_RAY, readImage("/sprites/titans/skill_mothra_godray.png"));
            TITAN_SKILLS.put(TitanMode.TitanSkill.MT_SILK_WEB, readImage("/sprites/titans/skill_mothra_silk.png"));
            TITAN_SKILLS.put(TitanMode.TitanSkill.MT_PRISM_RAIN, readImage("/sprites/titans/skill_mothra_prism.png"));
            TITAN_SKILLS.put(TitanMode.TitanSkill.MT_SOLAR_SUPERNOVA, readImage("/sprites/titans/skill_mothra_nova.png"));
            povBoyForeground = readImage("/sprites/titans/pov_boy_foreground.png");
            povCityBuildings = readImage("/sprites/titans/pov_city_buildings.png");
            godzillaRampageVoxel = readImage("/sprites/titans/godzilla_rampage_voxel.png");
            ghidorahRampageVoxel = readImage("/sprites/titans/ghidorah_rampage_voxel.png");
            mothraRampageVoxel = readImage("/sprites/titans/mothra_rampage_voxel.png");
            titansLoaded = true;
        }
    }

    private static void ensureMagicRunesLoaded() {
        if (magicRunesLoaded) return;
        synchronized (LOCK_RUNES) {
            if (magicRunesLoaded) return;
            MAGIC_RUNES.put(CombatLoadout.MagicOption.NONE, readImage("/sprites/fx/rune_none.png"));
            MAGIC_RUNES.put(CombatLoadout.MagicOption.FIRE, readImage("/sprites/fx/rune_fire.png"));
            MAGIC_RUNES.put(CombatLoadout.MagicOption.LIGHTNING, readImage("/sprites/fx/rune_lightning.png"));
            MAGIC_RUNES.put(CombatLoadout.MagicOption.ARCANE_SONAR, readImage("/sprites/fx/rune_sonar.png"));
            MAGIC_RUNES.put(CombatLoadout.MagicOption.FROST, readImage("/sprites/fx/rune_frost.png"));
            magicRunesLoaded = true;
        }
    }

    private static void ensureFxLoaded() {
        if (fxLoaded) return;
        synchronized (LOCK_FX) {
            if (fxLoaded) return;
            for (FxType fx : FxType.values()) {
                BufferedImage[] frames = new BufferedImage[FX_FRAME_COUNT];
                for (int i = 0; i < FX_FRAME_COUNT; i++) {
                    frames[i] = readImage("/sprites/fx/" + fx.prefix + "_" + i + ".png");
                }
                FX_FRAMES.put(fx, frames);
            }
            fxLoaded = true;
        }
    }

    private static void ensureShieldAndMuzzleLoaded() {
        if (shieldMuzzleLoaded) return;
        synchronized (LOCK_SHIELD_MUZZLE) {
            if (shieldMuzzleLoaded) return;
            magicShield = readImage("/sprites/fx/magic_shield.png");
            for (int i = 0; i < MUZZLE_FRAME_COUNT; i++) {
                MUZZLE_FRAMES[i] = readImage("/sprites/fx/muzzle_" + i + ".png");
            }
            shieldMuzzleLoaded = true;
        }
    }

    private static void ensureDragonLoaded(DragonSpecies species) {
        DragonSpecies sp = species != null ? species : DragonSpecies.ARCANE;
        if (LOADED_DRAGONS.contains(sp)) return;
        synchronized (LOCK_DRAGONS) {
            if (LOADED_DRAGONS.contains(sp)) return;
            BufferedImage[] frames = new BufferedImage[DRAGON_FRAME_COUNT];
            BufferedImage[] shadows = new BufferedImage[DRAGON_FRAME_COUNT];
            for (int i = 0; i < DRAGON_FRAME_COUNT; i++) {
                BufferedImage frame = readImage("/sprites/map/" + sp.prefix + "_" + i + ".png");
                frames[i] = frame;
                if (frame != null) shadows[i] = buildDropShadow(frame);
            }
            DRAGON_FRAMES_BY_SPECIES.put(sp, frames);
            DRAGON_SHADOWS_BY_SPECIES.put(sp, shadows);
            if (sp == DragonSpecies.ARCANE) {
                System.arraycopy(frames, 0, DRAGON_FRAMES, 0, DRAGON_FRAME_COUNT);
                System.arraycopy(shadows, 0, DRAGON_SHADOWS, 0, DRAGON_FRAME_COUNT);
            }
            LOADED_DRAGONS.add(sp);
        }
    }

    private static void ensureMapPropLoaded(String key) {
        if (key == null || MAP_PROPS.containsKey(key)) return;
        synchronized (LOCK_PROPS) {
            if (MAP_PROPS.containsKey(key)) return;
            String path = mapPropPath(key);
            if (path != null) MAP_PROPS.put(key, readImage(path));
        }
    }

    private static void ensureAllMapPropsLoaded() {
        if (allMapPropsLoaded) return;
        String[] keys = {
                "fortress", "arcane", "beacon", "sun", "moon", "storm_cloud", "wave_crest",
                "fantasy_citadel", "fantasy_sanctuary", "fantasy_portal",
                "apoc_volcano", "apoc_ruin", "apoc_meteor",
                "galaxy_blackhole", "galaxy_planet", "galaxy_station",
                "atlantis_temple", "atlantis_reef", "atlantis_trident"
        };
        for (String k : keys) {
            ensureMapPropLoaded(k);
        }
        allMapPropsLoaded = true;
    }

    private static String mapPropPath(String key) {
        return switch (key) {
            case "fortress" -> "/sprites/map/island_fortress.png";
            case "arcane" -> "/sprites/map/island_arcane.png";
            case "beacon" -> "/sprites/map/sea_beacon.png";
            case "sun" -> "/sprites/map/sun_3d.png";
            case "moon" -> "/sprites/map/moon_3d.png";
            case "storm_cloud" -> "/sprites/map/storm_cloud_3d.png";
            case "wave_crest" -> "/sprites/map/wave_crest_3d.png";
            case "fantasy_citadel" -> "/sprites/map/fantasy_citadel_3d.png";
            case "fantasy_sanctuary" -> "/sprites/map/fantasy_sanctuary_3d.png";
            case "fantasy_portal" -> "/sprites/map/fantasy_portal_3d.png";
            case "apoc_volcano" -> "/sprites/map/apoc_volcano_3d.png";
            case "apoc_ruin" -> "/sprites/map/apoc_ruin_3d.png";
            case "apoc_meteor" -> "/sprites/map/apoc_meteor_3d.png";
            case "galaxy_blackhole" -> "/sprites/map/galaxy_blackhole_3d.png";
            case "galaxy_planet" -> "/sprites/map/galaxy_planet_3d.png";
            case "galaxy_station" -> "/sprites/map/galaxy_station_3d.png";
            case "atlantis_temple" -> "/sprites/map/atlantis_temple_3d.png";
            case "atlantis_reef" -> "/sprites/map/atlantis_reef_3d.png";
            case "atlantis_trident" -> "/sprites/map/atlantis_trident_3d.png";
            default -> null;
        };
    }

    public static BufferedImage getMagicShield() {
        ensureShieldAndMuzzleLoaded();
        return magicShield;
    }

    public static BufferedImage getMuzzleFrame(int frameIndex) {
        ensureShieldAndMuzzleLoaded();
        if (frameIndex < 0 || frameIndex >= MUZZLE_FRAME_COUNT) return null;
        return MUZZLE_FRAMES[frameIndex];
    }

    public static int muzzleFrameCount() { return MUZZLE_FRAME_COUNT; }

    public static BufferedImage getDragonFrame(int frameIndex) {
        return getDragonFrame(DragonSpecies.ARCANE, frameIndex);
    }

    public static BufferedImage getDragonShadow(int frameIndex) {
        return getDragonShadow(DragonSpecies.ARCANE, frameIndex);
    }

    public static BufferedImage getDragonFrame(DragonSpecies species, int frameIndex) {
        DragonSpecies sp = species != null ? species : DragonSpecies.ARCANE;
        ensureDragonLoaded(sp);
        if (frameIndex < 0 || frameIndex >= DRAGON_FRAME_COUNT) return null;
        BufferedImage[] frames = DRAGON_FRAMES_BY_SPECIES.get(sp);
        return frames == null ? null : frames[frameIndex];
    }

    public static BufferedImage getDragonShadow(DragonSpecies species, int frameIndex) {
        DragonSpecies sp = species != null ? species : DragonSpecies.ARCANE;
        ensureDragonLoaded(sp);
        if (frameIndex < 0 || frameIndex >= DRAGON_FRAME_COUNT) return null;
        BufferedImage[] shadows = DRAGON_SHADOWS_BY_SPECIES.get(sp);
        return shadows == null ? null : shadows[frameIndex];
    }

    public static int dragonFrameCount() { return DRAGON_FRAME_COUNT; }

    public static BufferedImage getMapProp(String key) {
        if (key == null) return null;
        ensureMapPropLoaded(key);
        return MAP_PROPS.get(key);
    }

    public static BufferedImage getZombieShipSprite(ShipType type, boolean horizontal) {
        ensureZombieShipsLoaded();
        if (type == null) return null;
        return horizontal ? ZOMBIE_SHIP_H.get(type) : ZOMBIE_SHIP_V.get(type);
    }

    public static BufferedImage getZombieShipShadow(ShipType type, boolean horizontal) {
        ensureZombieShipsLoaded();
        if (type == null) return null;
        return horizontal ? ZOMBIE_SHADOW_H.get(type) : ZOMBIE_SHADOW_V.get(type);
    }

    public static BufferedImage getZombieProjectileSprite(ZombieMode.ZombieClass zc) {
        ensureZombieProjectilesLoaded();
        return zc == null ? null : ZOMBIE_PROJECTILES.get(zc);
    }

    public static BufferedImage getTitanSprite(TitanMode.TitanBoss boss, boolean enraged, boolean charging) {
        ensureTitansLoaded();
        if (boss == TitanMode.TitanBoss.GHIDORAH) {
            return (enraged || charging) ? titanKgStorm : titanKgIdle;
        }
        if (boss == TitanMode.TitanBoss.MOTHRA) {
            return (enraged || charging) ? titanMtEnrage : titanMtIdle;
        }
        if (enraged && !charging) return titanGzBurning;
        if (charging && enraged) return titanGzBurning;
        if (charging) return titanGzCharge;
        return titanGzIdle;
    }

    public static BufferedImage getTitanShadow(TitanMode.TitanBoss boss, boolean enraged) {
        ensureTitansLoaded();
        if (boss == TitanMode.TitanBoss.GHIDORAH) {
            return enraged ? titanKgStormShadow : titanKgIdleShadow;
        }
        if (boss == TitanMode.TitanBoss.MOTHRA) {
            return enraged ? titanMtEnrageShadow : titanMtIdleShadow;
        }
        return enraged ? titanGzBurningShadow : titanGzIdleShadow;
    }

    public static BufferedImage getTitanSkillSprite(TitanMode.TitanSkill skill) {
        ensureTitansLoaded();
        return skill == null ? null : TITAN_SKILLS.get(skill);
    }

    public static BufferedImage getPovBoyForeground() {
        ensureTitansLoaded();
        return povBoyForeground;
    }

    public static BufferedImage getPovCityBuildings() {
        ensureTitansLoaded();
        return povCityBuildings;
    }

    public static BufferedImage getGodzillaRampageSprite() {
        ensureTitansLoaded();
        return godzillaRampageVoxel;
    }

    public static BufferedImage getGhidorahRampageSprite() {
        ensureTitansLoaded();
        return ghidorahRampageVoxel;
    }

    public static BufferedImage getMothraRampageSprite() {
        ensureTitansLoaded();
        return mothraRampageVoxel;
    }

    public static BufferedImage getShipSprite(ShipType type, boolean horizontal) {
        ensureShipsLoaded();
        if (type == null) return null;
        return horizontal ? SHIP_H.get(type) : SHIP_V.get(type);
    }

    public static BufferedImage getShipShadow(ShipType type, boolean horizontal) {
        ensureShipsLoaded();
        if (type == null) return null;
        return horizontal ? SHIP_SHADOW_H.get(type) : SHIP_SHADOW_V.get(type);
    }

    public static BufferedImage getSuperShipSprite(boolean horizontal) {
        ensureShipsLoaded();
        return horizontal ? superShipH : superShipV;
    }

    public static BufferedImage getSuperShipShadow(boolean horizontal) {
        ensureShipsLoaded();
        return horizontal ? superShipShadowH : superShipShadowV;
    }

    public static BufferedImage getSuperShipHangar() {
        ensureShipsLoaded();
        return superShipHangar;
    }

    public static BufferedImage getSuperShipWeapons() {
        ensureShipsLoaded();
        return superShipWeapons;
    }

    public static BufferedImage getSuperShipIcon() {
        ensureShipsLoaded();
        return superShipIcon;
    }

    public static BufferedImage getShipSpriteForLength(int length, boolean horizontal) {
        ShipType type = switch (length) {
            case 5 -> ShipType.CARRIER;
            case 4 -> ShipType.BATTLESHIP;
            case 3 -> ShipType.CRUISER;
            case 2 -> ShipType.DESTROYER;
            default -> null;
        };
        return getShipSprite(type, horizontal);
    }

    public static BufferedImage getShipShadowForLength(int length, boolean horizontal) {
        ShipType type = switch (length) {
            case 5 -> ShipType.CARRIER;
            case 4 -> ShipType.BATTLESHIP;
            case 3 -> ShipType.CRUISER;
            case 2 -> ShipType.DESTROYER;
            default -> null;
        };
        return getShipShadow(type, horizontal);
    }

    private static final Map<ScaleKey, BufferedImage> SCALED_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(384, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<ScaleKey, BufferedImage> eldest) {
                    return size() > 896;
                }
            });

    private static final class ScaleKey {
        final BufferedImage img;
        final int w;
        final int h;

        ScaleKey(BufferedImage img, int w, int h) {
            this.img = img;
            this.w = w;
            this.h = h;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ScaleKey k)) return false;
            return img == k.img && w == k.w && h == k.h;
        }

        @Override public int hashCode() {
            return (System.identityHashCode(img) * 31 + w) * 31 + h;
        }
    }

    /** Multi-step bicubic downscale cached once per (sprite, width, height) in TYPE_INT_ARGB_PRE. */
    public static BufferedImage getScaled(BufferedImage img, int dstW, int dstH) {
        if (img == null || dstW <= 0 || dstH <= 0) return null;
        if (img.getWidth() == dstW && img.getHeight() == dstH && img.getType() == BufferedImage.TYPE_INT_ARGB_PRE) {
            return img;
        }
        ScaleKey key = new ScaleKey(img, dstW, dstH);
        BufferedImage cached = SCALED_CACHE.get(key);
        if (cached != null) return cached;

        BufferedImage current = img;
        int cw = current.getWidth();
        int ch = current.getHeight();
        while (cw > dstW * 2 && ch > dstH * 2) {
            int nw = Math.max(dstW, cw / 2);
            int nh = Math.max(dstH, ch / 2);
            BufferedImage half = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D hg = half.createGraphics();
            hg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            hg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            hg.drawImage(current, 0, 0, nw, nh, null);
            hg.dispose();
            current = half;
            cw = nw;
            ch = nh;
        }
        BufferedImage out = new BufferedImage(dstW, dstH, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D og = out.createGraphics();
        og.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        og.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        og.drawImage(current, 0, 0, dstW, dstH, null);
        og.dispose();
        SCALED_CACHE.put(key, out);
        return out;
    }

    /** Blits a pre-scaled device-resolution image at 1:1 native pixel alignment to avoid HiDPI resampling overhead. */
    public static void drawDeviceAligned(Graphics2D g, java.awt.Image img, int logicalX, int logicalY) {
        if (img == null) return;
        AffineTransform tx = g.getTransform();
        if (tx.getShearX() == 0.0 && tx.getShearY() == 0.0
                && (Math.abs(tx.getScaleX() - 1.0) > 1e-3 || Math.abs(tx.getScaleY() - 1.0) > 1e-3)) {
            int dx = (int) Math.round(tx.getTranslateX() + logicalX * tx.getScaleX());
            int dy = (int) Math.round(tx.getTranslateY() + logicalY * tx.getScaleY());
            Object prevInterp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setTransform(AffineTransform.getTranslateInstance(dx, dy));
            g.drawImage(img, 0, 0, null);
            g.setTransform(tx);
            if (prevInterp != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, prevInterp);
            return;
        }
        g.drawImage(img, logicalX, logicalY, null);
    }

    /** Returns true when Graphics2D has a pure translation+scale HiDPI transform without rotation/shear. */
    public static boolean isAxisAlignedScale(Graphics2D g) {
        AffineTransform tx = g.getTransform();
        return tx.getShearX() == 0.0 && tx.getShearY() == 0.0 && tx.getScaleX() > 0 && tx.getScaleY() > 0;
    }

    /** Blits a cached multi-step bicubic downscaled sprite matched cleanly to native device DPI without per-frame allocations. */
    public static void drawSharp(Graphics2D g, BufferedImage img, int x, int y, int dstW, int dstH) {
        if (img == null || dstW <= 0 || dstH <= 0) return;
        AffineTransform tx = g.getTransform();
        if (tx.getShearX() == 0.0 && tx.getShearY() == 0.0 && tx.getScaleX() > 0 && tx.getScaleY() > 0) {
            double sx = tx.getScaleX();
            double sy = tx.getScaleY();
            if (Math.abs(sx - 1.0) > 1e-3 || Math.abs(sy - 1.0) > 1e-3) {
                int devW = Math.max(1, (int) Math.round(dstW * sx));
                int devH = Math.max(1, (int) Math.round(dstH * sy));
                BufferedImage scaled = getScaled(img, devW, devH);
                if (scaled != null) {
                    drawDeviceAligned(g, scaled, x, y);
                }
                return;
            }
        }
        BufferedImage scaled = getScaled(img, dstW, dstH);
        if (scaled != null) {
            g.drawImage(scaled, x, y, null);
        }
    }

    public static BufferedImage getWeaponIcon(CombatLoadout.WeaponOption option) {
        ensureWeaponsLoaded();
        return option == null ? null : WEAPONS.get(option);
    }

    public static BufferedImage getProjectileSprite(CombatLoadout.WeaponOption option) {
        ensureProjectilesLoaded();
        return option == null ? null : PROJECTILES.get(option);
    }

    public static BufferedImage getMagicRune(CombatLoadout.MagicOption magic) {
        ensureMagicRunesLoaded();
        return magic == null ? null : MAGIC_RUNES.get(magic);
    }

    public static BufferedImage getFxFrame(FxType type, int frameIndex) {
        ensureFxLoaded();
        if (type == null || frameIndex < 0 || frameIndex >= FX_FRAME_COUNT) return null;
        BufferedImage[] frames = FX_FRAMES.get(type);
        return frames == null ? null : frames[frameIndex];
    }

    public static int fxFrameCount() { return FX_FRAME_COUNT; }

    private static void loadShip(ShipType type, String path) {
        BufferedImage horizontal = readImage(path);
        if (horizontal == null) return;
        BufferedImage vertical = rotate90Clockwise(horizontal);
        SHIP_H.put(type, horizontal);
        SHIP_V.put(type, vertical);
        SHIP_SHADOW_H.put(type, buildDropShadow(horizontal));
        SHIP_SHADOW_V.put(type, buildDropShadow(vertical));
    }

    private static void loadZombieShip(ShipType type, String path) {
        BufferedImage horizontal = readImage(path);
        if (horizontal == null) return;
        BufferedImage vertical = rotate90Clockwise(horizontal);
        ZOMBIE_SHIP_H.put(type, horizontal);
        ZOMBIE_SHIP_V.put(type, vertical);
        ZOMBIE_SHADOW_H.put(type, buildDropShadow(horizontal));
        ZOMBIE_SHADOW_V.put(type, buildDropShadow(vertical));
    }

    private static BufferedImage buildDropShadow(BufferedImage src) {
        int w = Math.max(1, src.getWidth() / 2);
        int h = Math.max(1, src.getHeight() / 2);
        BufferedImage shadow = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = shadow.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, w, h, null);
        g.setComposite(AlphaComposite.SrcIn);
        g.setColor(new Color(4, 10, 20, 125));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return shadow;
    }

    private static BufferedImage rotate90Clockwise(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage rotated = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = rotated.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        AffineTransform at = new AffineTransform();
        at.translate(h, 0);
        at.rotate(Math.PI / 2.0);
        g.drawImage(src, at, null);
        g.dispose();
        return rotated;
    }

    /**
     * Draws a crisp midpoint-rasterized pixel/voxel ring at {@code (cx, cy)} with zero allocations.
     */
    public static void drawVoxelRing(Graphics2D g, double cx, double cy, double radius, int pixelSize, boolean dashed) {
        int px = Math.max(2, pixelSize);
        int rSteps = Math.max(1, (int) Math.round(radius / px));
        int icx = (int) Math.round(cx) - px / 2;
        int icy = (int) Math.round(cy) - px / 2;
        int x = 0;
        int y = rSteps;
        int d = 1 - rSteps;
        int step = 0;
        while (x <= y) {
            if (!dashed || (step & 1) == 0) {
                int ox = x * px;
                int oy = y * px;
                g.fillRect(icx + ox, icy + oy, px, px);
                g.fillRect(icx - ox, icy + oy, px, px);
                g.fillRect(icx + ox, icy - oy, px, px);
                g.fillRect(icx - ox, icy - oy, px, px);
                if (x != y) {
                    g.fillRect(icx + oy, icy + ox, px, px);
                    g.fillRect(icx - oy, icy + ox, px, px);
                    g.fillRect(icx + oy, icy - ox, px, px);
                    g.fillRect(icx - oy, icy - ox, px, px);
                }
            }
            step++;
            if (d < 0) {
                d += 2 * x + 3;
            } else {
                d += 2 * (x - y) + 5;
                y--;
            }
            x++;
        }
    }

    /**
     * Draws a stepped voxel cube with an optional 1px top-left specular bevel highlight.
     */
    public static void drawVoxelCube(Graphics2D g, int cx, int cy, int size, Color fill, Color rimHighlight) {
        int sz = Math.max(2, size);
        int x = cx - sz / 2;
        int y = cy - sz / 2;
        if (fill != null) {
            g.setColor(fill);
            g.fillRect(x, y, sz, sz);
        }
        if (rimHighlight != null && sz >= 4) {
            int bevel = sz >= 8 ? 2 : 1;
            g.setColor(rimHighlight);
            g.fillRect(x, y, sz, bevel);
            g.fillRect(x, y, bevel, sz);
        }
    }

    /**
     * Fills an 8-vertex chamfered octagon/bevel box with uniform alpha compositing.
     */
    public static void fillChamferedBox(Graphics2D g, int x, int y, int w, int h, int chamfer) {
        int c = Math.max(2, Math.min(chamfer, Math.min(w, h) / 3));
        int[] xs = {x + c, x + w - c, x + w, x + w, x + w - c, x + c, x, x};
        int[] ys = {y, y, y + c, y + h - c, y + h, y + h, y + h - c, y + c};
        g.fillPolygon(xs, ys, 8);
    }

    /**
     * Draws a crisp voxel-stepped chamfered outline around {@code (x, y, w, h)}.
     */
    public static void drawVoxelChamferBox(Graphics2D g, int x, int y, int w, int h, int chamfer, int pixelSize) {
        int px = Math.max(2, pixelSize);
        int c = Math.max(px, Math.min(chamfer, Math.min(w, h) / 3));
        // Straight horizontal and vertical walls
        g.fillRect(x + c, y, Math.max(px, w - 2 * c), px);
        g.fillRect(x + c, y + h - px, Math.max(px, w - 2 * c), px);
        g.fillRect(x, y + c, px, Math.max(px, h - 2 * c));
        g.fillRect(x + w - px, y + c, px, Math.max(px, h - 2 * c));
        // Stepped diagonal corners
        int steps = Math.max(1, c / px);
        for (int i = 0; i <= steps; i++) {
            int dx = (i * c) / steps;
            int dy = ((steps - i) * c) / steps;
            g.fillRect(x + dx, y + dy, px, px);
            g.fillRect(x + w - px - dx, y + dy, px, px);
            g.fillRect(x + dx, y + h - px - dy, px, px);
            g.fillRect(x + w - px - dx, y + h - px - dy, px, px);
        }
    }

    private static BufferedImage readImage(String path) {
        try (InputStream stream = SpriteAssets.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            BufferedImage raw = ImageIO.read(stream);
            if (raw == null) return null;
            if (raw.getType() == BufferedImage.TYPE_INT_ARGB_PRE) return raw;
            BufferedImage pre = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D pg = pre.createGraphics();
            pg.drawImage(raw, 0, 0, null);
            pg.dispose();
            return pre;
        } catch (IOException ignored) {
            return null;
        }
    }
}
