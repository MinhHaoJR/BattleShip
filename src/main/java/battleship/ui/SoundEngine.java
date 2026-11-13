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
        SETUP("Mưu Lược", "audio/bgm_setup.wav", -4.5f),
        NAVAL("Đại Dương", "audio/bgm_naval.wav", -3.0f),
        FANTASY("Thần Giới", "audio/bgm_fantasy.wav", -3.0f),
        APOCALYPSE("Tận Thế", "audio/bgm_apocalypse.wav", -3.5f),
        GALAXY("Vũ Trụ", "audio/bgm_galaxy.wav", -3.0f),
        ATLANTIS("Atlantis", "audio/bgm_atlantis.wav", -3.0f),
        ZOMBIE("Zombie", "audio/bgm_zombie.wav", -3.5f),
        TITAN("Titan Boss", "audio/bgm_titan.wav", -3.4f),
        TITAN_GHIDORAH("Ghidorah", "audio/bgm_titan_ghidorah.wav", -3.4f),
        TITAN_MOTHRA("Mothra", "audio/bgm_titan_mothra.wav", -3.4f),
        TITAN_ENRAGE("Cuồng Bạo", "audio/bgm_titan_enrage.wav", -2.8f);

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
        UI_CLICK("audio/sfx_ui_click.wav", -6.0f, 2),
        SHIP_PLACE("audio/sfx_ship_place.wav", -2.0f, 2),
        FIRE_CANNON("audio/sfx_fire_cannon.wav", -1.0f, 2),
        FIRE_TORPEDO("audio/sfx_fire_torpedo.wav", -1.0f, 2),
        FIRE_MISSILE("audio/sfx_fire_missile.wav", -1.0f, 2),
        FIRE_AIRSTRIKE("audio/sfx_fire_airstrike.wav", -1.0f, 2),
        MAGIC_FIRE("audio/sfx_magic_fire.wav", -1.5f, 2),
        MAGIC_LIGHTNING("audio/sfx_magic_lightning.wav", -1.5f, 2),
        MAGIC_SONAR("audio/sfx_magic_sonar.wav", -2.5f, 2),
        MAGIC_FROST("audio/sfx_magic_frost.wav", -2.0f, 2),
        HIT("audio/sfx_hit.wav", -0.5f, 3),
        MISS("audio/sfx_miss.wav", -2.5f, 3),
        SUNK("audio/sfx_sunk.wav", 0.0f, 2),
        SHIELD_BLOCK("audio/sfx_shield_block.wav", -1.5f, 2),
        SHIELD_ACTIVATE("audio/sfx_shield_activate.wav", -0.5f, 2),
        ZOMBIE_ROAR("audio/sfx_zombie_roar.wav", -1.5f, 2),
        TITAN_GODZILLA_ROAR("audio/sfx_titan_godzilla_roar.wav", 1.2f, 2),
        TITAN_ATOMIC_BREATH("audio/sfx_titan_atomic_breath.wav", 1.5f, 2),
        TITAN_TAIL_SLAM("audio/sfx_titan_tail_slam.wav", 1.4f, 2),
        TITAN_PULSE("audio/sfx_titan_pulse.wav", 1.5f, 2),
        TITAN_ENRAGE_TRANSFORM("audio/sfx_titan_enrage_transform.wav", 1.5f, 1),
        TITAN_SPIRAL_RAY("audio/sfx_titan_spiral_ray.wav", 1.5f, 2),
        TITAN_MELTDOWN_RIFT("audio/sfx_titan_meltdown_rift.wav", 1.5f, 2),
        TITAN_GHIDORAH_ROAR("audio/sfx_titan_ghidorah_roar.wav", 1.0f, 2),
        TITAN_GRAVITY_BEAM("audio/sfx_titan_gravity_beam.wav", 1.3f, 2),
        TITAN_HURRICANE_WING("audio/sfx_titan_hurricane_wing.wav", 1.2f, 2),
        TITAN_ALPHA_STORM("audio/sfx_titan_alpha_storm.wav", 1.4f, 2),
        TITAN_CATACLYSM("audio/sfx_titan_cataclysm.wav", 1.4f, 2),
        TITAN_MOTHRA_ROAR("audio/sfx_titan_mothra_roar.wav", 1.1f, 2),
        TITAN_MOTHRA_GALE("audio/sfx_titan_mothra_gale.wav", 1.3f, 2),
        TITAN_MOTHRA_GODRAY("audio/sfx_titan_mothra_godray.wav", 1.3f, 2),
        TITAN_MOTHRA_SILK("audio/sfx_titan_mothra_silk.wav", 1.1f, 2),
        TITAN_MOTHRA_PRISM("audio/sfx_titan_mothra_prism.wav", 1.2f, 2),
        TITAN_MOTHRA_NOVA("audio/sfx_titan_mothra_nova.wav", 1.5f, 2),
        TITAN_BOSS_INTRO("audio/sfx_titan_boss_intro.wav", 1.4f, 1),
        BOSS_POV_GODZILLA("audio/sfx_boss_pov_godzilla.wav", 1.5f, 1),
        BOSS_POV_GHIDORAH("audio/sfx_boss_pov_ghidorah.wav", 1.5f, 1),
        BOSS_POV_MOTHRA("audio/sfx_boss_pov_mothra.wav", 1.5f, 1),
        SHIELD_CRACK("audio/sfx_shield_crack.wav", 1.0f, 3),
        SHIELD_SHATTER("audio/sfx_shield_shatter.wav", 1.5f, 2),
        SUPERSHIP_READY("audio/sfx_supership_ready.wav", 1.0f, 1),
        SUPERSHIP_CUTSCENE("audio/sfx_supership_cutscene.wav", 1.5f, 1),
        SUPERSHIP_HYPERCANNON("audio/sfx_supership_hypercannon.wav", 1.5f, 2),
        VICTORY("audio/sfx_victory.wav", -1.0f, 1),
        DEFEAT("audio/sfx_defeat.wav", -1.5f, 1),
        THUNDER_CRACK("audio/sfx_thunder_crack.wav", -0.5f, 2),
        THUNDER_RUMBLE("audio/sfx_thunder_rumble.wav", -2.5f, 2),
        WIND_GUST("audio/sfx_wind_gust.wav", -4.5f, 2),
        DRAGON_CRY("audio/sfx_dragon_cry.wav", -5.0f, 2),
        WORLD_BEACON("audio/sfx_world_beacon.wav", -4.5f, 2),
        WORLD_VOLCANO("audio/sfx_world_volcano.wav", -4.0f, 2),
        WORLD_PORTAL("audio/sfx_world_portal.wav", -4.5f, 2),
        WORLD_METEOR("audio/sfx_world_meteor.wav", -4.5f, 2);

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
        OCEAN_CALM("audio/amb_ocean_calm.wav", -8.0f),
        STORM_GALE("audio/amb_storm_gale.wav", -6.5f),
        FANTASY_GLADE("audio/amb_fantasy_glade.wav", -8.0f),
        APOCALYPSE_CALDERA("audio/amb_apocalypse_caldera.wav", -7.5f),
        GALAXY_COSMOS("audio/amb_galaxy_cosmos.wav", -8.5f),
        ATLANTIS_ABYSS("audio/amb_atlantis_abyss.wav", -7.5f);

        public final String resourcePath;
        public final float baseGainDb;

        AmbientBed(String resourcePath, float baseGainDb) {
            this.resourcePath = resourcePath;
            this.baseGainDb = baseGainDb;
        }
    }

    private record PcmData(AudioFormat format, byte[] pcm) { }

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
        if (!environmentAudioActive || !sfxEnabled || audioDeviceFailed) return;
        long now = System.currentTimeMillis();
        if ((now - lastThunderMs) < 1900L) return;
        lastThunderMs = now;
        boolean closeCrack = intensity >= 0.58;
        AUDIO_EXEC.execute(() -> {
            triggerSfxInternal(closeCrack ? Sfx.THUNDER_CRACK : Sfx.THUNDER_RUMBLE, closeCrack ? 0.0f : -1.5f);
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
        AUDIO_EXEC.execute(() -> transitionToTrack(target));
        notifyListeners();
    }

    /** Re-evaluates soundtrack for the current screen after map theme or game mode changes. */
    public static void refreshThemeMusic() {
        syncMusicState(inBattle);
    }

    /** Preloads core audio assets asynchronously without blocking UI startup. */
    public static void warmUpAsync() {
        AUDIO_EXEC.execute(() -> {
            if (audioDeviceFailed) return;
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
                AUDIO_EXEC.schedule(() -> triggerSfxInternal(mSfx), 55, TimeUnit.MILLISECONDS);
            }
        }
    }

    public static void playZombieLaunch() {
        playSfx(Sfx.ZOMBIE_ROAR);
    }

    public static void playTitanRoar(TitanMode.TitanBoss boss) {
        if (boss == null) return;
        Sfx roar = switch (boss) {
            case GHIDORAH -> Sfx.TITAN_GHIDORAH_ROAR;
            case MOTHRA -> Sfx.TITAN_MOTHRA_ROAR;
            case GODZILLA -> Sfx.TITAN_GODZILLA_ROAR;
        };
        playSfx(roar);
    }

    public static void playTitanBossIntro(TitanMode.TitanBoss boss) {
        if (!sfxEnabled || audioDeviceFailed) return;
        TitanMode.TitanBoss chosen = boss == null ? TitanMode.TitanBoss.GODZILLA : boss;
        Sfx povTrack = switch (chosen) {
            case GHIDORAH -> Sfx.BOSS_POV_GHIDORAH;
            case MOTHRA -> Sfx.BOSS_POV_MOTHRA;
            case GODZILLA -> Sfx.BOSS_POV_GODZILLA;
        };
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

    public static void playTitanSkill(TitanMode.TitanBoss boss, TitanMode.TitanSkill skill) {
        final TitanMode.TitanBoss roarBoss = boss != null ? boss : TitanMode.TitanBoss.GODZILLA;
        if (skill == null || !sfxEnabled || audioDeviceFailed) return;
        if (skill == TitanMode.TitanSkill.GZ_ATOMIC_BREATH || skill == TitanMode.TitanSkill.GZ_SPIRAL_RAY) {
            triggerSfxInternal(skill == TitanMode.TitanSkill.GZ_SPIRAL_RAY
                    ? Sfx.TITAN_SPIRAL_RAY : Sfx.TITAN_ATOMIC_BREATH);
            AUDIO_EXEC.schedule(() -> playTitanRoar(roarBoss), 480, TimeUnit.MILLISECONDS);
            return;
        }
        playTitanRoar(roarBoss);
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
        };
        AUDIO_EXEC.schedule(() -> triggerSfxInternal(skillSfx), 110, TimeUnit.MILLISECONDS);
    }

    /** Plays wind gust and optional storm thunder rumble when a Titan actively alters map weather or blows water. */
    public static void playEnvironmentShift(boolean heavyStorm) {
        if (!sfxEnabled || audioDeviceFailed) return;
        triggerSfxInternal(Sfx.WIND_GUST);
        if (heavyStorm) {
            AUDIO_EXEC.schedule(() -> triggerSfxInternal(Sfx.THUNDER_RUMBLE), 160, TimeUnit.MILLISECONDS);
        }
    }

    public static void playShieldActivate() {
        playSfx(Sfx.SHIELD_ACTIVATE);
    }

    public static void playShieldCrack(int remainingEnergy) {
        if (!sfxEnabled || audioDeviceFailed) return;
        triggerSfxInternal(Sfx.SHIELD_CRACK);
        if (remainingEnergy <= 35) {
            AUDIO_EXEC.schedule(() -> triggerSfxInternal(Sfx.SHIELD_BLOCK), 75, TimeUnit.MILLISECONDS);
        }
    }

    public static void playShieldShatter() {
        if (!sfxEnabled || audioDeviceFailed) return;
        triggerSfxInternal(Sfx.SHIELD_SHATTER);
        AUDIO_EXEC.schedule(() -> triggerSfxInternal(Sfx.SUNK), 90, TimeUnit.MILLISECONDS);
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

    public static void playSuperShipHyperCannon() {
        playSfx(Sfx.SUPERSHIP_HYPERCANNON);
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
        if (!musicEnabled || audioDeviceFailed) return;
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

    private static void triggerSfxInternal(Sfx sfx) {
        triggerSfxInternal(sfx, 0.0f);
    }

    private static void triggerSfxInternal(Sfx sfx, float extraGainDb) {
        if (!sfxEnabled || audioDeviceFailed) return;
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
        if (!environmentAudioActive || !sfxEnabled || audioDeviceFailed) {
            stopAmbientLoopsInternal();
            return;
        }
        AmbientBed desiredBed = resolveAmbientBed(activeEnvTheme);
        if (currentLoadedAmb != desiredBed || currentAmbClip == null || !currentAmbClip.isOpen()) {
            closeClipQuietly(currentAmbClip);
            currentAmbClip = null;
            currentLoadedAmb = desiredBed;
            PcmData pcm = loadAmbData(desiredBed);
            if (pcm != null) {
                try {
                    currentAmbClip = AudioSystem.getClip();
                    currentAmbClip.open(pcm.format, pcm.pcm, 0, pcm.pcm.length);
                    setGainLinear(currentAmbClip, desiredBed.baseGainDb, (float) Math.max(0.45, 1.0 - activeStormLevel * 0.40));
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
            setGainLinear(currentAmbClip, desiredBed.baseGainDb, (float) Math.max(0.45, 1.0 - activeStormLevel * 0.40));
        }

        if (activeStormLevel > 0.05) {
            if (stormAmbClip == null || !stormAmbClip.isOpen()) {
                PcmData stormPcm = loadAmbData(AmbientBed.STORM_GALE);
                if (stormPcm != null) {
                    try {
                        stormAmbClip = AudioSystem.getClip();
                        stormAmbClip.open(stormPcm.format, stormPcm.pcm, 0, stormPcm.pcm.length);
                        setGainLinear(stormAmbClip, AmbientBed.STORM_GALE.baseGainDb, (float) activeStormLevel);
                        stormAmbClip.loop(Clip.LOOP_CONTINUOUSLY);
                    } catch (LineUnavailableException | IllegalArgumentException | SecurityException ex) {
                        closeClipQuietly(stormAmbClip);
                        stormAmbClip = null;
                    }
                }
            } else {
                if (!stormAmbClip.isRunning()) stormAmbClip.loop(Clip.LOOP_CONTINUOUSLY);
                setGainLinear(stormAmbClip, AmbientBed.STORM_GALE.baseGainDb, (float) activeStormLevel);
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
        if (!environmentAudioActive || !sfxEnabled || audioDeviceFailed) return;
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
