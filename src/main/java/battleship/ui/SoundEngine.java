package battleship.ui;

import battleship.model.ShotResult;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingUtilities;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Asynchronous adaptive soundtrack and combat SFX mixer built on pure Java SE AudioSystem.
 * Runs all clip loading, looping, and crossfades on a daemon thread so the Swing EDT never stalls.
 */
public final class SoundEngine {
    public enum BgmTrack {
        SETUP("Mưu Lược", "audio/bgm_setup.wav", -5.5f),
        NAVAL("Đại Dương", "audio/bgm_naval.wav", -5.0f),
        FANTASY("Thần Giới", "audio/bgm_fantasy.wav", -5.0f),
        APOCALYPSE("Tận Thế", "audio/bgm_apocalypse.wav", -5.2f),
        GALAXY("Vũ Trụ", "audio/bgm_galaxy.wav", -5.0f),
        ATLANTIS("Atlantis", "audio/bgm_atlantis.wav", -5.0f),
        ZOMBIE("Zombie", "audio/bgm_zombie.wav", -5.2f),
        TITAN("Titan Boss", "audio/bgm_titan.wav", -5.0f),
        TITAN_GHIDORAH("Ghidorah", "audio/bgm_titan_ghidorah.wav", -5.0f),
        TITAN_MOTHRA("Mothra", "audio/bgm_titan_mothra.wav", -5.0f),
        TITAN_ENRAGE("Cuồng Bạo", "audio/bgm_titan_enrage.wav", -4.5f);

        public final String shortLabel;
        public final String resourcePath;
        public final float baseGainDb;

        BgmTrack(String shortLabel, String resourcePath, float baseGainDb) {
            this.shortLabel = shortLabel;
            this.resourcePath = resourcePath;
            this.baseGainDb = baseGainDb;
        }
    }

    public enum Sfx {
        UI_CLICK("audio/sfx_ui_click.wav", -7.5f, 2),
        SHIP_PLACE("audio/sfx_ship_place.wav", -5.5f, 2),
        FIRE_CANNON("audio/sfx_fire_cannon.wav", -3.5f, 2),
        FIRE_TORPEDO("audio/sfx_fire_torpedo.wav", -3.5f, 2),
        FIRE_MISSILE("audio/sfx_fire_missile.wav", -3.8f, 2),
        FIRE_AIRSTRIKE("audio/sfx_fire_airstrike.wav", -3.8f, 2),
        MAGIC_FIRE("audio/sfx_magic_fire.wav", -6.5f, 2),
        MAGIC_LIGHTNING("audio/sfx_magic_lightning.wav", -6.5f, 2),
        MAGIC_SONAR("audio/sfx_magic_sonar.wav", -6.5f, 2),
        MAGIC_FROST("audio/sfx_magic_frost.wav", -6.5f, 2),
        HIT("audio/sfx_hit.wav", -3.0f, 3),
        MISS("audio/sfx_miss.wav", -6.5f, 3),
        SUNK("audio/sfx_sunk.wav", -2.5f, 2),
        SHIELD_BLOCK("audio/sfx_shield_block.wav", -5.5f, 2),
        SHIELD_ACTIVATE("audio/sfx_shield_activate.wav", -4.5f, 2),
        ZOMBIE_ROAR("audio/sfx_zombie_roar.wav", -4.0f, 2),
        ZOMBIE_SPIT("audio/sfx_zombie_spit.wav", -3.0f, 3),
        ZOMBIE_SPORE("audio/sfx_zombie_spore.wav", -3.5f, 3),
        ZOMBIE_CURSE("audio/sfx_zombie_curse.wav", -3.0f, 2),
        ZOMBIE_BLOOD_CANNON("audio/sfx_zombie_blood_cannon.wav", -2.5f, 2),
        ZOMBIE_BAT_SWARM("audio/sfx_zombie_bat_swarm.wav", -3.0f, 2),
        ZOMBIE_HIT("audio/sfx_zombie_hit.wav", -2.5f, 3),
        TITAN_GODZILLA_ROAR("audio/sfx_titan_godzilla_roar.wav", -1.5f, 2),
        TITAN_ATOMIC_BREATH("audio/sfx_titan_atomic_breath.wav", -3.0f, 2),
        TITAN_TAIL_SLAM("audio/sfx_titan_tail_slam.wav", -3.2f, 2),
        TITAN_PULSE("audio/sfx_titan_pulse.wav", -3.2f, 2),
        TITAN_ENRAGE_TRANSFORM("audio/sfx_titan_enrage_transform.wav", -2.5f, 1),
        TITAN_SPIRAL_RAY("audio/sfx_titan_spiral_ray.wav", -3.0f, 2),
        TITAN_MELTDOWN_RIFT("audio/sfx_titan_meltdown_rift.wav", -3.0f, 2),
        TITAN_GHIDORAH_ROAR("audio/sfx_titan_ghidorah_roar.wav", -1.5f, 2),
        TITAN_GRAVITY_BEAM("audio/sfx_titan_gravity_beam.wav", -3.2f, 2),
        TITAN_HURRICANE_WING("audio/sfx_titan_hurricane_wing.wav", -3.5f, 2),
        TITAN_ALPHA_STORM("audio/sfx_titan_alpha_storm.wav", -3.2f, 2),
        TITAN_CATACLYSM("audio/sfx_titan_cataclysm.wav", -3.0f, 2),
        TITAN_MOTHRA_ROAR("audio/sfx_titan_mothra_roar.wav", -1.8f, 2),
        TITAN_MOTHRA_GALE("audio/sfx_titan_mothra_gale.wav", -3.5f, 2),
        TITAN_MOTHRA_GODRAY("audio/sfx_titan_mothra_godray.wav", -3.5f, 2),
        TITAN_MOTHRA_SILK("audio/sfx_titan_mothra_silk.wav", -3.8f, 2),
        TITAN_MOTHRA_PRISM("audio/sfx_titan_mothra_prism.wav", -3.8f, 2),
        TITAN_MOTHRA_NOVA("audio/sfx_titan_mothra_nova.wav", -3.0f, 2),
        // 3 boss mới (âm thanh tổng hợp bằng tools/tao_am_thanh_boss.py)
        TITAN_SPACEGODZILLA_ROAR("audio/sfx_titan_spacegodzilla_roar.wav", -1.8f, 2),
        BOSS_POV_SPACEGODZILLA("audio/sfx_boss_pov_spacegodzilla.wav", -2.5f, 1),
        TITAN_DESTOROYAH_ROAR("audio/sfx_titan_destoroyah_roar.wav", -1.8f, 2),
        BOSS_POV_DESTOROYAH("audio/sfx_boss_pov_destoroyah.wav", -2.5f, 1),
        TITAN_ULTIMA_ROAR("audio/sfx_titan_ultima_roar.wav", -1.8f, 2),
        BOSS_POV_ULTIMA("audio/sfx_boss_pov_ultima.wav", -2.5f, 1),
        TITAN_SG_CRYSTAL_MISSILE("audio/sfx_titan_sg_crystal_missile.wav", -3.2f, 2),
        TITAN_SG_GRAVITY_TORNADO("audio/sfx_titan_sg_gravity_tornado.wav", -3.2f, 2),
        TITAN_SG_CORONA_BEAM("audio/sfx_titan_sg_corona_beam.wav", -3.2f, 2),
        TITAN_SG_CRYSTAL_FORTRESS("audio/sfx_titan_sg_crystal_fortress.wav", -3.2f, 2),
        TITAN_SG_COSMIC_NOVA("audio/sfx_titan_sg_cosmic_nova.wav", -3.2f, 2),
        TITAN_DS_MICRO_OXYGEN("audio/sfx_titan_ds_micro_oxygen.wav", -3.2f, 2),
        TITAN_DS_HORN_SLASH("audio/sfx_titan_ds_horn_slash.wav", -3.2f, 2),
        TITAN_DS_SWARM_SPLIT("audio/sfx_titan_ds_swarm_split.wav", -3.2f, 2),
        TITAN_DS_OXYGEN_MIST("audio/sfx_titan_ds_oxygen_mist.wav", -3.2f, 2),
        TITAN_DS_DESTRUCTION_OMEGA("audio/sfx_titan_ds_destruction_omega.wav", -3.2f, 2),
        TITAN_UL1_GILL_SPRAY("audio/sfx_titan_ul1_gill_spray.wav", -3.2f, 2),
        TITAN_UL1_FIN_DIVE("audio/sfx_titan_ul1_fin_dive.wav", -3.2f, 2),
        TITAN_UL1_FLUKE_GEYSER("audio/sfx_titan_ul1_fluke_geyser.wav", -3.2f, 2),
        TITAN_UL2_RAMPAGE_CHARGE("audio/sfx_titan_ul2_rampage_charge.wav", -3.2f, 2),
        TITAN_UL2_LAVA_VENTS("audio/sfx_titan_ul2_lava_vents.wav", -3.2f, 2),
        TITAN_UL2_SCALE_SHRAPNEL("audio/sfx_titan_ul2_scale_shrapnel.wav", -3.2f, 2),
        TITAN_UL3_CLAW_REND("audio/sfx_titan_ul3_claw_rend.wav", -3.2f, 2),
        TITAN_UL3_SONIC_ROAR("audio/sfx_titan_ul3_sonic_roar.wav", -3.2f, 2),
        TITAN_UL3_SPINE_LANCE("audio/sfx_titan_ul3_spine_lance.wav", -3.2f, 2),
        TITAN_UL4_CRIMSON_BREATH("audio/sfx_titan_ul4_crimson_breath.wav", -3.2f, 2),
        TITAN_UL4_SKYFIRE("audio/sfx_titan_ul4_skyfire.wav", -3.2f, 2),
        TITAN_UL4_FINAL_ROAR("audio/sfx_titan_ul4_final_roar.wav", -3.2f, 2),
        TITAN_UL1_RIPTIDE_COIL("audio/sfx_titan_ul1_riptide_coil.wav", -3.2f, 2),
        TITAN_UL1_SCALD_TORRENT("audio/sfx_titan_ul1_scald_torrent.wav", -3.2f, 2),
        TITAN_UL2_MAGMA_QUAKE("audio/sfx_titan_ul2_magma_quake.wav", -3.2f, 2),
        TITAN_UL2_MOLTEN_STAMPEDE("audio/sfx_titan_ul2_molten_stampede.wav", -3.2f, 2),
        TITAN_UL3_GLACIER_BREATH("audio/sfx_titan_ul3_glacier_breath.wav", -3.2f, 2),
        TITAN_UL3_FROST_CROWN("audio/sfx_titan_ul3_frost_crown.wav", -3.2f, 2),
        TITAN_UL4_TAIL_CRUSH("audio/sfx_titan_ul4_tail_crush.wav", -3.2f, 2),
        TITAN_UL4_BLOOD_SPIRAL("audio/sfx_titan_ul4_blood_spiral.wav", -3.2f, 2),
        TITAN_SKILL_POV_SPACEGODZILLA("audio/sfx_titan_skill_pov_spacegodzilla.wav", -1.4f, 1),
        TITAN_SKILL_POV_DESTOROYAH("audio/sfx_titan_skill_pov_destoroyah.wav", -1.4f, 1),
        TITAN_SKILL_POV_ULTIMA("audio/sfx_titan_skill_pov_ultima.wav", -1.4f, 1),
        TITAN_BOSS_INTRO("audio/sfx_titan_boss_intro.wav", -2.5f, 1),
        BOSS_POV_GODZILLA("audio/sfx_boss_pov_godzilla.wav", -2.5f, 1),
        BOSS_POV_GHIDORAH("audio/sfx_boss_pov_ghidorah.wav", -2.5f, 1),
        BOSS_POV_MOTHRA("audio/sfx_boss_pov_mothra.wav", -2.5f, 1),
        TITAN_SKILL_POV_GODZILLA("audio/sfx_titan_skill_pov_godzilla.wav", -1.4f, 1),
        TITAN_SKILL_POV_GHIDORAH("audio/sfx_titan_skill_pov_ghidorah.wav", -1.4f, 1),
        TITAN_SKILL_POV_MOTHRA("audio/sfx_titan_skill_pov_mothra.wav", -1.4f, 1),
        SHIELD_CRACK("audio/sfx_shield_crack.wav", -3.5f, 3),
        SHIELD_SHATTER("audio/sfx_shield_shatter.wav", -2.5f, 2),
        SUPERSHIP_READY("audio/sfx_supership_ready.wav", -3.5f, 1),
        SUPERSHIP_CUTSCENE("audio/sfx_supership_cutscene.wav", -2.5f, 1),
        SUPERSHIP_HYPERCANNON("audio/sfx_supership_hypercannon.wav", -1.5f, 2),
        VICTORY("audio/sfx_victory.wav", -3.5f, 1),
        DEFEAT("audio/sfx_defeat.wav", -3.8f, 1),
        THUNDER_CRACK("audio/sfx_thunder_crack.wav", -5.5f, 2),
        THUNDER_RUMBLE("audio/sfx_thunder_rumble.wav", -6.5f, 2),
        WIND_GUST("audio/sfx_wind_gust.wav", -8.0f, 2),
        DRAGON_CRY("audio/sfx_dragon_cry.wav", -9.5f, 2),
        WORLD_BEACON("audio/sfx_world_beacon.wav", -8.5f, 2),
        WORLD_VOLCANO("audio/sfx_world_volcano.wav", -8.5f, 2),
        WORLD_PORTAL("audio/sfx_world_portal.wav", -8.0f, 2),
        WORLD_METEOR("audio/sfx_world_meteor.wav", -8.0f, 2);

        public final String resourcePath;
        public final float baseGainDb;
        public final int poolSize;

        Sfx(String resourcePath, float baseGainDb, int poolSize) {
            this.resourcePath = resourcePath;
            this.baseGainDb = baseGainDb;
            this.poolSize = poolSize;
        }
    }

    public enum AmbientBed {
        OCEAN_CALM("audio/amb_ocean_calm.wav", -12.0f),
        STORM_GALE("audio/amb_storm_gale.wav", -10.5f),
        FANTASY_GLADE("audio/amb_fantasy_glade.wav", -12.0f),
        APOCALYPSE_CALDERA("audio/amb_apocalypse_caldera.wav", -11.5f),
        GALAXY_COSMOS("audio/amb_galaxy_cosmos.wav", -12.5f),
        ATLANTIS_ABYSS("audio/amb_atlantis_abyss.wav", -11.5f);

        public final String resourcePath;
        public final float baseGainDb;

        AmbientBed(String resourcePath, float baseGainDb) {
            this.resourcePath = resourcePath;
            this.baseGainDb = baseGainDb;
        }
    }

    private record PcmData(AudioFormat format, byte[] pcm) { }

    /**
     * Suppresses physical speaker playback (BGM, ambient beds, SFX) when automated checks pass
     * {@code -Dbattleship.silent=true} or set {@code BATTLESHIP_SILENT=true}, while keeping WAV
     * decoding and logical audio state intact.
     */
    private static final boolean SILENT_MODE = Boolean.getBoolean("battleship.silent")
            || "true".equalsIgnoreCase(System.getenv("BATTLESHIP_SILENT"))
            || "1".equals(System.getenv("BATTLESHIP_SILENT"));

    private static final ScheduledExecutorService AUDIO_EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "BattleShip-Audio");
        t.setDaemon(true);
        return t;
    });

    private static final Map<BgmTrack, PcmData> BGM_DATA = new EnumMap<>(BgmTrack.class);
    private static final Map<AmbientBed, PcmData> AMB_DATA = new EnumMap<>(AmbientBed.class);
    private static final Map<Sfx, PcmData> SFX_DATA = new EnumMap<>(Sfx.class);
    private static final Map<Sfx, Clip[]> SFX_POOLS = new EnumMap<>(Sfx.class);
    private static final Map<Sfx, Integer> SFX_CURSOR = new EnumMap<>(Sfx.class);
    private static final Map<Sfx, Long> SFX_LAST_PLAYED_MS = new EnumMap<>(Sfx.class);
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private static volatile boolean musicEnabled = true;
    private static volatile boolean sfxEnabled = true;
    private static volatile boolean inBattle = false;
    private static volatile boolean environmentAudioActive = false;
    private static volatile boolean audioDeviceFailed = false;
    private static volatile BgmTrack activeTrack = BgmTrack.SETUP;
    private static volatile float duckDb = 0.0f;
    private static volatile OceanMapBackdrop.MapTheme lastSentTheme = OceanMapBackdrop.MapTheme.NAVAL_CITADEL;
    private static volatile OceanMapBackdrop.Season lastSentSeason = OceanMapBackdrop.Season.AUTO;
    private static volatile double lastSentStorm = -1.0;
    private static volatile long lastAtmoSendMs = 0L;
    private static volatile long lastThunderMs = 0L;

    private static Clip currentBgmClip;
    private static BgmTrack currentLoadedTrack;
    private static Clip fadingOutClip;
    private static ScheduledFuture<?> fadeFuture;
    private static ScheduledFuture<?> unduckFuture;
    private static float currentBgmLevel = 1.0f;

    private static Clip currentAmbClip;
    private static AmbientBed currentLoadedAmb;
    private static Clip stormAmbClip;
    private static double activeStormLevel = 0.0;
    private static OceanMapBackdrop.MapTheme activeEnvTheme = OceanMapBackdrop.MapTheme.NAVAL_CITADEL;
    private static OceanMapBackdrop.Season activeEnvSeason = OceanMapBackdrop.Season.AUTO;
    private static ScheduledFuture<?> envTickerFuture;
    private static int envEventCounter = 0;

    private SoundEngine() { }

    public static void addListener(Runnable listener) {
        if (listener != null) LISTENERS.add(listener);
    }

    public static void removeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    public static boolean isMusicEnabled() { return musicEnabled; }
    public static boolean isSfxEnabled() { return sfxEnabled; }
    public static boolean isSilentMode() { return SILENT_MODE; }
    public static BgmTrack getActiveTrack() { return activeTrack; }

    public static boolean toggleMusic() {
        musicEnabled = !musicEnabled;
        AUDIO_EXEC.execute(SoundEngine::applyMusicState);
        notifyListeners();
        return musicEnabled;
    }

    public static boolean toggleSfx() {
        sfxEnabled = !sfxEnabled;
        AUDIO_EXEC.execute(SoundEngine::syncAmbientLoopsInternal);
        if (sfxEnabled) playUiClick();
        notifyListeners();
        return sfxEnabled;
    }

    /** Enables or disables continuous ocean, weather, thunder, dragon, and world-object ambient audio. */
    public static void setEnvironmentAudioActive(boolean active) {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        environmentAudioActive = active;
        AUDIO_EXEC.execute(() -> {
            if (active) {
                ensureEnvTickerStarted();
                syncAmbientLoopsInternal();
            } else {
                stopEnvTicker();
                stopAmbientLoopsInternal();
            }
        });
    }

    /** Updates adaptive sea & storm ambient beds smoothly from real-time map atmosphere values. */
    public static void updateAtmosphereState(
            OceanMapBackdrop.MapTheme theme,
            OceanMapBackdrop.Season season,
            double stormLevel
    ) {
        if (!environmentAudioActive || !sfxEnabled || audioDeviceFailed) return;
        OceanMapBackdrop.MapTheme resolvedTheme = theme != null ? theme : OceanMapBackdrop.MapTheme.NAVAL_CITADEL;
        OceanMapBackdrop.Season resolvedSeason = season != null ? season : OceanMapBackdrop.Season.AUTO;
        double clampedStorm = Math.max(0.0, Math.min(1.0, stormLevel));
        long now = System.currentTimeMillis();
        if (resolvedTheme == lastSentTheme && resolvedSeason == lastSentSeason
                && Math.abs(clampedStorm - lastSentStorm) < 0.08 && (now - lastAtmoSendMs) < 500L) {
            return;
        }
        lastSentTheme = resolvedTheme;
        lastSentSeason = resolvedSeason;
        lastSentStorm = clampedStorm;
        lastAtmoSendMs = now;
        AUDIO_EXEC.execute(() -> {
            activeEnvTheme = resolvedTheme;
            activeEnvSeason = resolvedSeason;
            activeStormLevel = clampedStorm;
            syncAmbientLoopsInternal();
        });
    }

    /** Plays sky-to-sea thunder synchronized with lightning strikes on screen (cooldown debounced). */
    public static void playThunderStrike(double intensity) {
        if (!environmentAudioActive || !sfxEnabled || audioDeviceFailed || inBattle) return;
        long now = System.currentTimeMillis();
        if ((now - lastThunderMs) < 3500L) return;
        lastThunderMs = now;
        boolean closeCrack = intensity >= 0.58;
        AUDIO_EXEC.execute(() -> {
            triggerSfxInternal(closeCrack ? Sfx.THUNDER_CRACK : Sfx.THUNDER_RUMBLE, closeCrack ? -1.5f : -2.5f);
        });
    }

    /** Plays the signature environmental landmark cue when switching to a 3D map theme. */
    public static void playMapPropCue(OceanMapBackdrop.MapTheme theme) {
        if (theme == null || !sfxEnabled || audioDeviceFailed) return;
        Sfx sfx = switch (theme) {
            case NAVAL_CITADEL, ABYSSAL_ATLANTIS -> Sfx.WORLD_BEACON;
            case FANTASY_REALM -> Sfx.WORLD_PORTAL;
            case APOCALYPSE -> Sfx.WORLD_VOLCANO;
            case GALAXY_COSMOS -> Sfx.WORLD_METEOR;
        };
        AUDIO_EXEC.execute(() -> {
            activeEnvTheme = theme;
            syncAmbientLoopsInternal();
            triggerSfxInternal(sfx, -2.5f);
        });
    }

    /** Plays immediate acoustic response when cycling weather modes (Clear / Storm / Auto). */
    public static void playWeatherTransitionCue(OceanMapBackdrop.WeatherMode weather, double stormLevel) {
        if (!sfxEnabled || audioDeviceFailed) return;
        AUDIO_EXEC.execute(() -> {
            activeStormLevel = Math.max(0.0, Math.min(1.0, stormLevel));
            syncAmbientLoopsInternal();
            if (weather == OceanMapBackdrop.WeatherMode.STORM || stormLevel > 0.45) {
                triggerSfxInternal(Sfx.THUNDER_RUMBLE, -1.0f);
                AUDIO_EXEC.schedule(() -> triggerSfxInternal(Sfx.WIND_GUST, -1.5f), 220, TimeUnit.MILLISECONDS);
            } else {
                triggerSfxInternal(Sfx.WIND_GUST, -5.0f);
            }
        });
    }

    /** Plays subtle seasonal wind or arcane atmosphere chime when switching seasons. */
    public static void playSeasonTransitionCue(OceanMapBackdrop.Season season) {
        if (!sfxEnabled || audioDeviceFailed || season == null) return;
        AUDIO_EXEC.execute(() -> {
            activeEnvSeason = season;
            Sfx cue = switch (season) {
                case SPRING, SUMMER -> Sfx.WORLD_PORTAL;
                case AUTUMN, WINTER -> Sfx.WIND_GUST;
                case AUTO -> Sfx.WIND_GUST;
            };
            triggerSfxInternal(cue, -5.5f);
        });
    }

    /** Synchronizes the looping soundtrack to the current screen, map theme, and Zombie mode. */
    public static void syncMusicState(boolean battleScreen) {
        inBattle = battleScreen;
        BgmTrack target = resolveDesiredTrack();
        activeTrack = target;
        AUDIO_EXEC.execute(() -> {
            transitionToTrack(target);
            syncAmbientLoopsInternal();
        });
        notifyListeners();
    }

    /** Re-evaluates soundtrack for the current screen after map theme or game mode changes. */
    public static void refreshThemeMusic() {
        syncMusicState(inBattle);
    }

    /** Preloads core audio assets asynchronously without blocking UI startup. */
    public static void warmUpAsync() {
        if (SILENT_MODE || audioDeviceFailed) return;
        AUDIO_EXEC.execute(() -> {
            if (SILENT_MODE || audioDeviceFailed) return;
            for (Sfx sfx : List.of(
                    Sfx.UI_CLICK, Sfx.SHIP_PLACE, Sfx.FIRE_CANNON, Sfx.HIT, Sfx.MISS,
                    Sfx.THUNDER_CRACK, Sfx.THUNDER_RUMBLE, Sfx.WIND_GUST)) {
                loadSfxData(sfx);
            }
            loadAmbData(AmbientBed.OCEAN_CALM);
        });
    }

    public static void playUiClick() {
        playSfx(Sfx.UI_CLICK);
    }

    public static void playShipPlace() {
        playSfx(Sfx.SHIP_PLACE);
    }

    public static void playWeaponMagicLaunch(CombatLoadout.WeaponOption weapon, CombatLoadout.MagicOption magic) {
        if (weapon != null) {
            Sfx wSfx = switch (weapon) {
                case CANNON -> Sfx.FIRE_CANNON;
                case TORPEDO -> Sfx.FIRE_TORPEDO;
                case MISSILE_CROSS -> Sfx.FIRE_MISSILE;
                case AIRSTRIKE -> Sfx.FIRE_AIRSTRIKE;
            };
            playSfx(wSfx);
        }
        if (magic != null && magic != CombatLoadout.MagicOption.NONE) {
            Sfx mSfx = switch (magic) {
                case FIRE -> Sfx.MAGIC_FIRE;
                case LIGHTNING -> Sfx.MAGIC_LIGHTNING;
                case ARCANE_SONAR -> Sfx.MAGIC_SONAR;
                case FROST -> Sfx.MAGIC_FROST;
                case NONE -> null;
            };
            if (mSfx != null) {
                AUDIO_EXEC.schedule(() -> triggerSfxInternal(mSfx, -4.5f), 70, TimeUnit.MILLISECONDS);
            }
        }
    }

    public static void playZombieLaunch() {
        playSfx(Sfx.ZOMBIE_ROAR);
    }

    /**
     * Plays the distinctive class-specific Zombie skill launch SFX with optional layer accents.
     */
    public static void playZombieSkill(ZombieMode.ZombieClass zc) {
        if (SILENT_MODE || !sfxEnabled || audioDeviceFailed) return;
        if (zc == null) {
            playSfx(Sfx.ZOMBIE_ROAR);
            return;
        }
        Sfx primary = switch (zc) {
            case PLAGUE_DESTROYER -> Sfx.ZOMBIE_SPIT;
            case BLIGHT_SUBMARINE -> Sfx.ZOMBIE_SPORE;
            case NECRO_CRUISER -> Sfx.ZOMBIE_CURSE;
            case BLOOD_BATTLESHIP -> Sfx.ZOMBIE_BLOOD_CANNON;
            case ABYSS_CARRIER -> Sfx.ZOMBIE_BAT_SWARM;
        };
        playSfx(primary);
        if (zc == ZombieMode.ZombieClass.NECRO_CRUISER) {
            duckTemporarily(-3.5f, 700L);
        } else if (zc == ZombieMode.ZombieClass.BLOOD_BATTLESHIP
                || zc == ZombieMode.ZombieClass.ABYSS_CARRIER) {
            duckTemporarily(-4.5f, 850L);
            AUDIO_EXEC.schedule(() -> triggerSfxInternal(Sfx.ZOMBIE_ROAR, 0.55f), 110, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Plays the Zombie impact SFX (or shield-block / splash-miss) matching strike outcome.
     */
    public static void playZombieImpact(ZombieMode.ZombieClass zc, ShotResult result, boolean shielded) {
        if (SILENT_MODE || !sfxEnabled || audioDeviceFailed) return;
        if (shielded) {
            playSfx(Sfx.SHIELD_BLOCK);
            return;
        }
        if (result == null || result == ShotResult.MISS) {
            playSfx(Sfx.MISS);
            return;
        }
        playSfx(Sfx.ZOMBIE_HIT);
        if (result == ShotResult.SUNK) {
            duckTemporarily(-5.5f, 1150L);
            AUDIO_EXEC.schedule(() -> triggerSfxInternal(Sfx.SUNK, 0.92f), 90, TimeUnit.MILLISECONDS);
        } else if (zc == ZombieMode.ZombieClass.BLOOD_BATTLESHIP
                || zc == ZombieMode.ZombieClass.ABYSS_CARRIER) {
            AUDIO_EXEC.schedule(() -> triggerSfxInternal(Sfx.HIT, 0.65f), 55, TimeUnit.MILLISECONDS);
        }
    }

    /** Tiếng gầm theo quy tắc tên TITAN_&lt;BOSS&gt;_ROAR (đủ cho cả 6 boss). */
    private static Sfx roarFor(TitanMode.TitanBoss boss) {
        return Sfx.valueOf("TITAN_" + boss.name() + "_ROAR");
    }

    public static void playTitanRoar(TitanMode.TitanBoss boss) {
        if (boss == null) return;
        Sfx roar = roarFor(boss);
        duckTemporarily(-3.8f, 950L);
        playSfx(roar);
    }

    public static void playTitanBossIntro(TitanMode.TitanBoss boss) {
        if (!sfxEnabled || audioDeviceFailed) return;
        TitanMode.TitanBoss chosen = boss == null ? TitanMode.TitanBoss.GODZILLA : boss;
        Sfx povTrack = Sfx.valueOf("BOSS_POV_" + chosen.name());
        AUDIO_EXEC.execute(() -> {
            if (unduckFuture != null) unduckFuture.cancel(false);
            duckDb = -9.0f;
            updateCurrentBgmGain();
            triggerSfxInternal(povTrack);
            AUDIO_EXEC.schedule(() -> triggerSfxInternal(Sfx.TITAN_BOSS_INTRO), 4100, TimeUnit.MILLISECONDS);
            unduckFuture = AUDIO_EXEC.schedule(() -> {
                duckDb = 0.0f;
                updateCurrentBgmGain();
            }, 4950, TimeUnit.MILLISECONDS);
        });
    }

    public static void playTitanEnrageTransform(TitanMode.TitanBoss boss) {
        TitanMode.setTitanEnraged(true);
        TitanMode.TitanBoss safeBoss = boss == null ? TitanMode.TitanBoss.GODZILLA : boss;
        if (!sfxEnabled || audioDeviceFailed) {
            refreshThemeMusic();
            return;
        }
        BgmTrack target = resolveDesiredTrack();
        activeTrack = target;
        notifyListeners();
        AUDIO_EXEC.execute(() -> {
            if (unduckFuture != null) unduckFuture.cancel(false);
            duckDb = -7.5f;
            updateCurrentBgmGain();
            triggerSfxInternal(Sfx.TITAN_ENRAGE_TRANSFORM);
            transitionToTrack(target);
            AUDIO_EXEC.schedule(() -> playTitanRoar(safeBoss), 690, TimeUnit.MILLISECONDS);
            unduckFuture = AUDIO_EXEC.schedule(() -> {
                duckDb = 0.0f;
                updateCurrentBgmGain();
            }, 1450, TimeUnit.MILLISECONDS);
        });
    }

    private static volatile long lastSkillRoarMs = 0L;

    public static void playTitanSkill(TitanMode.TitanBoss boss, TitanMode.TitanSkill skill) {
        final TitanMode.TitanBoss roarBoss = boss != null ? boss : TitanMode.TitanBoss.GODZILLA;
        if (skill == null || !sfxEnabled || audioDeviceFailed) return;
        Sfx skillSfx = switch (skill) {
            case GZ_ATOMIC_BREATH -> Sfx.TITAN_ATOMIC_BREATH;
            case GZ_THERMO_PULSE -> Sfx.TITAN_PULSE;
            case GZ_TAIL_SLAM -> Sfx.TITAN_TAIL_SLAM;
            case GZ_SPIRAL_RAY -> Sfx.TITAN_SPIRAL_RAY;
            case GZ_MELTDOWN_RIFT -> Sfx.TITAN_MELTDOWN_RIFT;
            case KG_TRIPLE_GRAVITY -> Sfx.TITAN_GRAVITY_BEAM;
            case KG_HURRICANE_WING -> Sfx.TITAN_HURRICANE_WING;
            case KG_OMEGA_VORTEX -> Sfx.THUNDER_CRACK;
            case KG_ALPHA_STORM -> Sfx.TITAN_ALPHA_STORM;
            case KG_CATACLYSM_GRAVITY -> Sfx.TITAN_CATACLYSM;
            case MT_WING_GALE -> Sfx.TITAN_MOTHRA_GALE;
            case MT_GOD_RAY -> Sfx.TITAN_MOTHRA_GODRAY;
            case MT_SILK_WEB -> Sfx.TITAN_MOTHRA_SILK;
            case MT_PRISM_RAIN -> Sfx.TITAN_MOTHRA_PRISM;
            case MT_SOLAR_SUPERNOVA -> Sfx.TITAN_MOTHRA_NOVA;
            default -> Sfx.valueOf("TITAN_" + skill.name());
        };
        duckTemporarily(-4.0f, 950L);
        AUDIO_EXEC.execute(() -> triggerSfxInternal(skillSfx));
        // Only layer a restrained roar occasionally so two full-volume 2s SFX never clash
        if (skill != TitanMode.TitanSkill.GZ_ATOMIC_BREATH && skill != TitanMode.TitanSkill.GZ_SPIRAL_RAY) {
            long now = System.currentTimeMillis();
            if (now - lastSkillRoarMs >= 4500L) {
                lastSkillRoarMs = now;
                Sfx roar = roarFor(roarBoss);
                AUDIO_EXEC.schedule(() -> triggerSfxInternal(roar, -3.5f), 260, TimeUnit.MILLISECONDS);
            }
        }
    }

    /** Plays deeply ducked cinema-grade sound design synced to Titan Skill POV cutscenes (Minus One, Ghidorah, Mothra). */
    public static void playTitanSkillPov(TitanMode.TitanBoss boss, TitanMode.TitanSkill skill) {
        if (!sfxEnabled || audioDeviceFailed) return;
        TitanMode.TitanBoss safeBoss = boss != null ? boss : (skill != null ? skill.boss : TitanMode.TitanBoss.GODZILLA);
        Sfx povSfx = switch (safeBoss) {
            case GHIDORAH -> Sfx.TITAN_SKILL_POV_GHIDORAH;
            case MOTHRA -> Sfx.TITAN_SKILL_POV_MOTHRA;
            case GODZILLA -> Sfx.TITAN_SKILL_POV_GODZILLA;
            default -> Sfx.valueOf("TITAN_SKILL_POV_" + safeBoss.name());
        };
        // Duck BGM deeply so the brief dead-silence vacuum pause hits with theatrical contrast
        duckTemporarily(-7.8f, 2850L);
        AUDIO_EXEC.execute(() -> triggerSfxInternal(povSfx));
        if (skill != null && skill.phase2Only) {
            Sfx roar = roarFor(safeBoss);
            AUDIO_EXEC.schedule(() -> triggerSfxInternal(roar, -5.0f), 1180, TimeUnit.MILLISECONDS);
        }
    }

    /** Plays subtle wind shift when a Titan alters weather or blows water (without storm thunder spam). */
    public static void playEnvironmentShift(boolean heavyStorm) {
        if (!sfxEnabled || audioDeviceFailed) return;
        AUDIO_EXEC.execute(() -> triggerSfxInternal(heavyStorm ? Sfx.THUNDER_RUMBLE : Sfx.WIND_GUST, -4.5f));
    }

    public static void playShieldActivate() {
        playSfx(Sfx.SHIELD_ACTIVATE);
    }

    public static void playShieldCrack(int remainingEnergy) {
        if (!sfxEnabled || audioDeviceFailed) return;
        AUDIO_EXEC.execute(() -> triggerSfxInternal(Sfx.SHIELD_CRACK, remainingEnergy <= 35 ? -0.5f : -2.5f));
    }

    public static void playShieldShatter() {
        if (!sfxEnabled || audioDeviceFailed) return;
        duckTemporarily(-3.5f, 800L);
        AUDIO_EXEC.execute(() -> triggerSfxInternal(Sfx.SHIELD_SHATTER));
    }

    public static void playSuperShipReady() {
        playSfx(Sfx.SUPERSHIP_READY);
    }

    public static void playSuperShipCutscene() {
        if (!sfxEnabled || audioDeviceFailed) return;
        AUDIO_EXEC.execute(() -> {
            if (unduckFuture != null) unduckFuture.cancel(false);
            duckDb = -8.5f;
            updateCurrentBgmGain();
            triggerSfxInternal(Sfx.SUPERSHIP_CUTSCENE);
            unduckFuture = AUDIO_EXEC.schedule(() -> {
                duckDb = 0.0f;
                updateCurrentBgmGain();
            }, 5450, TimeUnit.MILLISECONDS);
        });
    }

    private static volatile long lastTelegraphWarnMs = 0L;

    public static void playSuperShipHyperCannon() {
        duckTemporarily(-6.5f, 2550L);
        playSfx(Sfx.SUPERSHIP_HYPERCANNON);
    }

    /** Restrained tactical sonar cue when a fleet vessel stands inside a red Telegraph warning zone. */
    public static void playRaidTelegraphWarning() {
        if (!sfxEnabled || audioDeviceFailed) return;
        long now = System.currentTimeMillis();
        if (now - lastTelegraphWarnMs < 3600L) return;
        lastTelegraphWarnMs = now;
        AUDIO_EXEC.execute(() -> triggerSfxInternal(Sfx.WORLD_BEACON, -4.5f));
    }

    /** Clean stagger break sound without stacking harsh lightning static on top. */
    public static void playRaidStaggerBreak() {
        if (!sfxEnabled || audioDeviceFailed) return;
        duckTemporarily(-4.0f, 750L);
        AUDIO_EXEC.execute(() -> triggerSfxInternal(Sfx.SHIELD_SHATTER, -1.5f));
    }

    /** Crisp two-note counter chime when performing a Timed Shield Parry. */
    public static void playRaidShieldParry() {
        if (!sfxEnabled || audioDeviceFailed) return;
        duckTemporarily(-3.5f, 650L);
        AUDIO_EXEC.execute(() -> {
            triggerSfxInternal(Sfx.SHIELD_ACTIVATE, -1.0f);
            AUDIO_EXEC.schedule(() -> triggerSfxInternal(Sfx.MAGIC_SONAR, -5.0f), 65, TimeUnit.MILLISECONDS);
        });
    }

    /** Heavy carapace armor break sound when destroying Boss HEAD, CORE_FIN, or BODY_TAIL. */
    public static void playRaidPartBroken() {
        if (!sfxEnabled || audioDeviceFailed) return;
        duckTemporarily(-4.0f, 800L);
        AUDIO_EXEC.execute(() -> {
            triggerSfxInternal(Sfx.SUNK, -1.5f);
            AUDIO_EXEC.schedule(() -> triggerSfxInternal(Sfx.SHIELD_CRACK, -5.0f), 85, TimeUnit.MILLISECONDS);
        });
    }

    /** Subtle reactive chord when triggering 2-way Elemental Chemistry (Melt, Thermal Shock, Superconduct). */
    public static void playRaidElementalReaction() {
        if (!sfxEnabled || audioDeviceFailed) return;
        AUDIO_EXEC.execute(() -> {
            triggerSfxInternal(Sfx.MAGIC_LIGHTNING, -3.5f);
            AUDIO_EXEC.schedule(() -> triggerSfxInternal(Sfx.MAGIC_FROST, -6.5f), 70, TimeUnit.MILLISECONDS);
        });
    }

    public static void playImpact(ShotResult result, boolean shielded) {
        if (shielded) {
            playSfx(Sfx.SHIELD_BLOCK);
            return;
        }
        if (result == null) {
            playSfx(Sfx.HIT);
            return;
        }
        switch (result) {
            case SUNK -> playSfx(Sfx.SUNK);
            case HIT -> playSfx(Sfx.HIT);
            case MISS -> playSfx(Sfx.MISS);
            case ALREADY_SHOT -> { }
        }
    }

    public static void playGameOver(boolean playerWon) {
        if (!sfxEnabled) return;
        AUDIO_EXEC.execute(() -> {
            if (unduckFuture != null) unduckFuture.cancel(false);
            duckDb = -9.0f;
            updateCurrentBgmGain();
            triggerSfxInternal(playerWon ? Sfx.VICTORY : Sfx.DEFEAT);
            unduckFuture = AUDIO_EXEC.schedule(() -> {
                duckDb = 0.0f;
                updateCurrentBgmGain();
            }, 2600, TimeUnit.MILLISECONDS);
        });
    }

    public static void playSfx(Sfx sfx) {
        if (sfx == null || !sfxEnabled || audioDeviceFailed) return;
        AUDIO_EXEC.execute(() -> triggerSfxInternal(sfx));
    }

    public static BgmTrack resolveDesiredTrack() {
        if (ZombieMode.isTitanMode()) {
            if (TitanMode.isTitanEnraged()) return BgmTrack.TITAN_ENRAGE;
            return switch (TitanMode.getActiveBoss()) {
                case GHIDORAH -> BgmTrack.TITAN_GHIDORAH;
                case MOTHRA -> BgmTrack.TITAN_MOTHRA;
                case GODZILLA -> BgmTrack.TITAN;
                default -> BgmTrack.TITAN;
            };
        }
        if (ZombieMode.isZombieMode()) return BgmTrack.ZOMBIE;
        OceanMapBackdrop.MapTheme theme = OceanMapBackdrop.getMapTheme();
        if (!inBattle && theme == OceanMapBackdrop.MapTheme.NAVAL_CITADEL) {
            return BgmTrack.SETUP;
        }
        return switch (theme) {
            case NAVAL_CITADEL -> BgmTrack.NAVAL;
            case FANTASY_REALM -> BgmTrack.FANTASY;
            case APOCALYPSE -> BgmTrack.APOCALYPSE;
            case GALAXY_COSMOS -> BgmTrack.GALAXY;
            case ABYSSAL_ATLANTIS -> BgmTrack.ATLANTIS;
        };
    }

    private static void applyMusicState() {
        if (!musicEnabled) {
            cancelFade();
            closeClipQuietly(fadingOutClip);
            fadingOutClip = null;
            if (currentBgmClip != null) {
                currentBgmClip.stop();
            }
        } else {
            BgmTrack target = resolveDesiredTrack();
            activeTrack = target;
            transitionToTrack(target);
        }
    }

    private static void transitionToTrack(BgmTrack target) {
        if (SILENT_MODE || !musicEnabled || audioDeviceFailed) return;
        if (currentLoadedTrack == target && currentBgmClip != null) {
            if (!currentBgmClip.isRunning()) {
                currentBgmLevel = 1.0f;
                updateCurrentBgmGain();
                currentBgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            }
            return;
        }

        PcmData pcm = loadBgmData(target);
        if (pcm == null) return;

        Clip nextClip;
        try {
            nextClip = AudioSystem.getClip();
            nextClip.open(pcm.format, pcm.pcm, 0, pcm.pcm.length);
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException ex) {
            audioDeviceFailed = true;
            return;
        }

        cancelFade();
        closeClipQuietly(fadingOutClip);
        fadingOutClip = currentBgmClip;
        final Clip prevClip = fadingOutClip;
        final BgmTrack prevTrack = currentLoadedTrack;

        currentBgmClip = nextClip;
        currentLoadedTrack = target;

        if (prevClip == null || !prevClip.isRunning()) {
            closeClipQuietly(prevClip);
            fadingOutClip = null;
            currentBgmLevel = 1.0f;
            updateCurrentBgmGain();
            nextClip.loop(Clip.LOOP_CONTINUOUSLY);
            return;
        }

        currentBgmLevel = 0.02f;
        setGainLinear(nextClip, target.baseGainDb + duckDb, 0.02f);
        nextClip.loop(Clip.LOOP_CONTINUOUSLY);

        final int totalSteps = 16;
        final int[] step = {0};
        fadeFuture = AUDIO_EXEC.scheduleAtFixedRate(() -> {
            step[0]++;
            float frac = Math.min(1.0f, step[0] / (float) totalSteps);
            currentBgmLevel = Math.max(0.02f, frac);
            updateCurrentBgmGain();
            if (prevTrack != null) {
                setGainLinear(prevClip, prevTrack.baseGainDb + duckDb, Math.max(0.01f, 1.0f - frac));
            }
            if (step[0] >= totalSteps) {
                closeClipQuietly(prevClip);
                if (fadingOutClip == prevClip) fadingOutClip = null;
                cancelFade();
            }
        }, 35, 35, TimeUnit.MILLISECONDS);
    }

    private static void cancelFade() {
        if (fadeFuture != null) {
            fadeFuture.cancel(false);
            fadeFuture = null;
        }
    }

    private static void updateCurrentBgmGain() {
        if (currentBgmClip != null && currentLoadedTrack != null) {
            setGainLinear(currentBgmClip, currentLoadedTrack.baseGainDb + duckDb, currentBgmLevel);
        }
    }

    private static void duckTemporarily(float targetDuckDb, long holdMs) {
        if (SILENT_MODE || !musicEnabled) return;
        AUDIO_EXEC.execute(() -> {
            if (unduckFuture != null && !unduckFuture.isDone() && duckDb < targetDuckDb) {
                return;
            }
            duckDb = targetDuckDb;
            updateCurrentBgmGain();
            if (unduckFuture != null) {
                unduckFuture.cancel(false);
            }
            unduckFuture = AUDIO_EXEC.schedule(() -> {
                duckDb = 0.0f;
                updateCurrentBgmGain();
            }, Math.max(250L, holdMs), TimeUnit.MILLISECONDS);
        });
    }

    private static long sfxCooldownMs(Sfx sfx) {
        return switch (sfx) {
            case WORLD_BEACON -> 3400L;
            case WORLD_VOLCANO, WORLD_PORTAL, WORLD_METEOR, DRAGON_CRY, WIND_GUST, THUNDER_CRACK, THUNDER_RUMBLE -> 2400L;
            case TITAN_SKILL_POV_GODZILLA, TITAN_SKILL_POV_GHIDORAH, TITAN_SKILL_POV_MOTHRA, TITAN_SKILL_POV_SPACEGODZILLA,
                 TITAN_SKILL_POV_DESTOROYAH, TITAN_SKILL_POV_ULTIMA, SUPERSHIP_HYPERCANNON -> 160L;
            case TITAN_GODZILLA_ROAR, TITAN_GHIDORAH_ROAR, TITAN_MOTHRA_ROAR, ZOMBIE_ROAR -> 1400L;
            case ZOMBIE_SPIT, ZOMBIE_SPORE, ZOMBIE_CURSE, ZOMBIE_BLOOD_CANNON, ZOMBIE_BAT_SWARM -> 110L;
            case ZOMBIE_HIT -> 95L;
            case HIT, MISS, SHIELD_BLOCK, SHIELD_CRACK, UI_CLICK,
                 FIRE_CANNON, FIRE_TORPEDO, FIRE_MISSILE, FIRE_AIRSTRIKE,
                 MAGIC_FIRE, MAGIC_LIGHTNING, MAGIC_SONAR, MAGIC_FROST -> 95L;
            default -> 70L;
        };
    }

    private static void triggerSfxInternal(Sfx sfx) {
        triggerSfxInternal(sfx, 0.0f);
    }

    private static void triggerSfxInternal(Sfx sfx, float extraGainDb) {
        if (SILENT_MODE || sfx == null || !sfxEnabled || audioDeviceFailed) return;
        long now = System.currentTimeMillis();
        Long last = SFX_LAST_PLAYED_MS.get(sfx);
        if (last != null && (now - last) < sfxCooldownMs(sfx)) {
            return;
        }
        SFX_LAST_PLAYED_MS.put(sfx, now);
        PcmData data = loadSfxData(sfx);
        if (data == null) return;

        Clip[] pool = SFX_POOLS.get(sfx);
        if (pool == null) {
            pool = new Clip[sfx.poolSize];
            SFX_POOLS.put(sfx, pool);
        }
        int cursor = SFX_CURSOR.getOrDefault(sfx, 0);
        SFX_CURSOR.put(sfx, (cursor + 1) % pool.length);

        Clip clip = pool[cursor];
        try {
            if (clip == null || !clip.isOpen()) {
                clip = AudioSystem.getClip();
                clip.open(data.format, data.pcm, 0, data.pcm.length);
                pool[cursor] = clip;
            } else {
                clip.stop();
                clip.setFramePosition(0);
            }
            setGainDb(clip, sfx.baseGainDb + extraGainDb);
            clip.start();
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException ex) {
            audioDeviceFailed = true;
        }
    }

    private static AmbientBed resolveAmbientBed(OceanMapBackdrop.MapTheme theme) {
        if (theme == null) return AmbientBed.OCEAN_CALM;
        return switch (theme) {
            case NAVAL_CITADEL -> AmbientBed.OCEAN_CALM;
            case FANTASY_REALM -> AmbientBed.FANTASY_GLADE;
            case APOCALYPSE -> AmbientBed.APOCALYPSE_CALDERA;
            case GALAXY_COSMOS -> AmbientBed.GALAXY_COSMOS;
            case ABYSSAL_ATLANTIS -> AmbientBed.ATLANTIS_ABYSS;
        };
    }

    private static void syncAmbientLoopsInternal() {
        if (SILENT_MODE || !environmentAudioActive || !sfxEnabled || audioDeviceFailed) {
            stopAmbientLoopsInternal();
            return;
        }
        AmbientBed desiredBed = resolveAmbientBed(activeEnvTheme);
        float battleAttenDb = inBattle ? -6.5f : 0.0f;
        if (currentLoadedAmb != desiredBed || currentAmbClip == null || !currentAmbClip.isOpen()) {
            closeClipQuietly(currentAmbClip);
            currentAmbClip = null;
            currentLoadedAmb = desiredBed;
            PcmData pcm = loadAmbData(desiredBed);
            if (pcm != null) {
                try {
                    currentAmbClip = AudioSystem.getClip();
                    currentAmbClip.open(pcm.format, pcm.pcm, 0, pcm.pcm.length);
                    setGainLinear(currentAmbClip, desiredBed.baseGainDb + battleAttenDb, (float) Math.max(0.45, 1.0 - activeStormLevel * 0.40));
                    currentAmbClip.loop(Clip.LOOP_CONTINUOUSLY);
                } catch (LineUnavailableException | IllegalArgumentException | SecurityException ex) {
                    closeClipQuietly(currentAmbClip);
                    currentAmbClip = null;
                    audioDeviceFailed = true;
                    return;
                }
            }
        } else {
            if (!currentAmbClip.isRunning()) currentAmbClip.loop(Clip.LOOP_CONTINUOUSLY);
            setGainLinear(currentAmbClip, desiredBed.baseGainDb + battleAttenDb, (float) Math.max(0.45, 1.0 - activeStormLevel * 0.40));
        }

        if (activeStormLevel > 0.05) {
            if (stormAmbClip == null || !stormAmbClip.isOpen()) {
                PcmData stormPcm = loadAmbData(AmbientBed.STORM_GALE);
                if (stormPcm != null) {
                    try {
                        stormAmbClip = AudioSystem.getClip();
                        stormAmbClip.open(stormPcm.format, stormPcm.pcm, 0, stormPcm.pcm.length);
                        setGainLinear(stormAmbClip, AmbientBed.STORM_GALE.baseGainDb + battleAttenDb, (float) activeStormLevel);
                        stormAmbClip.loop(Clip.LOOP_CONTINUOUSLY);
                    } catch (LineUnavailableException | IllegalArgumentException | SecurityException ex) {
                        closeClipQuietly(stormAmbClip);
                        stormAmbClip = null;
                    }
                }
            } else {
                if (!stormAmbClip.isRunning()) stormAmbClip.loop(Clip.LOOP_CONTINUOUSLY);
                setGainLinear(stormAmbClip, AmbientBed.STORM_GALE.baseGainDb + battleAttenDb, (float) activeStormLevel);
            }
        } else if (stormAmbClip != null && stormAmbClip.isRunning()) {
            stormAmbClip.stop();
        }
    }

    private static void stopAmbientLoopsInternal() {
        if (currentAmbClip != null && currentAmbClip.isRunning()) {
            currentAmbClip.stop();
        }
        if (stormAmbClip != null && stormAmbClip.isRunning()) {
            stormAmbClip.stop();
        }
    }

    private static void ensureEnvTickerStarted() {
        if (SILENT_MODE) return;
        if (envTickerFuture != null && !envTickerFuture.isCancelled()) return;
        envTickerFuture = AUDIO_EXEC.scheduleAtFixedRate(SoundEngine::tickAmbientEvents, 4500, 7500, TimeUnit.MILLISECONDS);
    }

    private static void stopEnvTicker() {
        if (envTickerFuture != null) {
            envTickerFuture.cancel(false);
            envTickerFuture = null;
        }
    }

    private static void tickAmbientEvents() {
        if (SILENT_MODE || !environmentAudioActive || !sfxEnabled || audioDeviceFailed || inBattle || ZombieMode.isTitanMode()) return;
        envEventCounter++;
        int slot = envEventCounter % 3;
        if (slot == 0) {
            triggerSfxInternal(Sfx.DRAGON_CRY, -3.5f);
        } else if (slot == 1) {
            Sfx propSfx = switch (activeEnvTheme) {
                case NAVAL_CITADEL, ABYSSAL_ATLANTIS -> Sfx.WORLD_BEACON;
                case FANTASY_REALM -> Sfx.WORLD_PORTAL;
                case APOCALYPSE -> Sfx.WORLD_VOLCANO;
                case GALAXY_COSMOS -> Sfx.WORLD_METEOR;
            };
            triggerSfxInternal(propSfx, -4.0f);
        } else if (activeStormLevel > 0.25 || activeEnvSeason == OceanMapBackdrop.Season.WINTER || activeEnvSeason == OceanMapBackdrop.Season.AUTUMN) {
            triggerSfxInternal(Sfx.WIND_GUST, activeStormLevel > 0.45 ? -2.0f : -5.5f);
        }
    }

    private static PcmData loadAmbData(AmbientBed bed) {
        PcmData cached = AMB_DATA.get(bed);
        if (cached != null) return cached;
        PcmData loaded = readWavResource(bed.resourcePath);
        if (loaded != null) AMB_DATA.put(bed, loaded);
        return loaded;
    }

    private static PcmData loadBgmData(BgmTrack track) {
        PcmData cached = BGM_DATA.get(track);
        if (cached != null) return cached;
        PcmData loaded = readWavResource(track.resourcePath);
        if (loaded != null) BGM_DATA.put(track, loaded);
        return loaded;
    }

    private static PcmData loadSfxData(Sfx sfx) {
        PcmData cached = SFX_DATA.get(sfx);
        if (cached != null) return cached;
        PcmData loaded = readWavResource(sfx.resourcePath);
        if (loaded != null) SFX_DATA.put(sfx, loaded);
        return loaded;
    }

    private static PcmData readWavResource(String path) {
        ClassLoader cl = SoundEngine.class.getClassLoader();
        try (InputStream raw = cl.getResourceAsStream(path)) {
            if (raw == null) return null;
            try (BufferedInputStream bis = new BufferedInputStream(raw);
                 AudioInputStream ais = AudioSystem.getAudioInputStream(bis)) {
                AudioFormat base = ais.getFormat();
                AudioFormat pcmFmt = (base.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && base.getSampleSizeInBits() == 16)
                        ? base
                        : new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, base.getSampleRate(), 16,
                                base.getChannels(), base.getChannels() * 2, base.getSampleRate(), false);
                AudioInputStream decoded = pcmFmt == base ? ais : AudioSystem.getAudioInputStream(pcmFmt, ais);
                ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(4096, ais.available()));
                byte[] buf = new byte[8192];
                int n;
                while ((n = decoded.read(buf)) >= 0) {
                    if (n > 0) out.write(buf, 0, n);
                }
                return new PcmData(pcmFmt, out.toByteArray());
            }
        } catch (UnsupportedAudioFileException | IOException ex) {
            return null;
        }
    }

    private static void setGainLinear(Clip clip, float baseDb, float linear01) {
        float clamped = Math.max(0.01f, Math.min(1.0f, linear01));
        float gainDb = baseDb + (float) (20.0 * Math.log10(clamped));
        setGainDb(clip, gainDb);
    }

    private static void setGainDb(Clip clip, float db) {
        if (clip == null || !clip.isOpen()) return;
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl ctrl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float clamped = Math.max(ctrl.getMinimum(), Math.min(ctrl.getMaximum(), db));
                ctrl.setValue(clamped);
            }
        } catch (IllegalArgumentException ignored) { }
    }

    private static void closeClipQuietly(Clip clip) {
        if (clip == null) return;
        try {
            clip.stop();
            clip.close();
        } catch (Exception ignored) { }
    }

    private static void notifyListeners() {
        Runnable task = () -> {
            for (Runnable r : LISTENERS) r.run();
        };
        if (SwingUtilities.isEventDispatchThread()) task.run();
        else SwingUtilities.invokeLater(task);
    }
}
