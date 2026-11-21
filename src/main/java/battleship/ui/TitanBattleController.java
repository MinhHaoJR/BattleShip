package battleship.ui;

import battleship.model.Board;
import battleship.model.CellState;
import battleship.model.Ship;
import battleship.model.ShipType;
import battleship.model.ShotResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Self-contained UI-layer Hybrid Real-Time Tactical Boss Raid controller for VS Titan mode.
 * Coordinates multi-part Boss HP, breakable limbs, Boss repositioning/submerging,
 * active ATB Cast Bar & Telegraphs, Stagger interrupts, Fleet Dodge/Rotate,
 * Wind Push drift, Persistent Hazards, and Shield Parry without touching model/logic packages.
 */
public final class TitanBattleController {

    public record PlayerStrikeOutcome(
            ShotResult aggregateResult,
            List<int[]> hitCells,
            List<int[]> missCells,
            List<int[]> weakpointHits,
            int[] burnCell,
            List<int[]> sonarCells,
            int totalRemainingHp,
            boolean enragedJustTriggered,
            boolean bossDefeated
    ) {
        public List<int[]> sonarRevealedCells() { return sonarCells; }
    }

    public record RaidStrikeInfo(
            int damageDealt,
            TitanMode.TitanPart primaryPart,
            TitanMode.TitanPart brokenPartNow,
            boolean staggerTriggered,
            boolean submergedRevealed,
            boolean submergedMitigated,
            int staggerAdded
    ) { }

    public record TitanSkillOutcome(
            TitanMode.TitanSkill skill,
            int targetRow,
            int targetCol,
            List<int[]> targetCells,
            List<int[]> hitCells,
            List<int[]> missCells,
            List<int[]> shieldBlockedCells,
            List<ShipType> newlySunkShips,
            boolean enraged,
            boolean playerDefeated
    ) { }

    public record FleetBuffSummary(
            boolean carrierAlive,
            boolean battleshipAlive,
            boolean cruiserAlive,
            boolean submarineAlive,
            boolean destroyerAlive
    ) {
        public double airstrikeDamageMult() { return carrierAlive ? 1.20 : 1.0; }
        public double cooldownSpeedMult() { return carrierAlive ? 0.88 : 1.0; }
        public double shieldDrainMult() { return battleshipAlive ? 0.80 : 1.0; }
        public double manaRegenMult() { return cruiserAlive ? 1.22 : 1.0; }
        public boolean ignoreSubmergedPenalty() { return submarineAlive; }
        public double staggerPowerMult() { return submarineAlive ? 1.22 : 1.0; }
        public double cannonDamageMult() { return destroyerAlive ? 1.18 : 1.0; }
        public double dodgeCooldownMult() { return destroyerAlive ? 0.72 : 1.0; }
    }

    public record HazardTile(
            int row,
            int col,
            TitanMode.HazardType type,
            long remainingMs
    ) { }

    public record RaidTickResult(
            TitanMode.TitanSkill castReadyToFire,
            int castTargetRow,
            int castTargetCol,
            List<int[]> castTargetCells,
            int burnDotDamage,
            int hazardShipHits,
            List<int[]> hazardDamageCells,
            boolean bossMoved,
            boolean bossSubmergedNow,
            boolean bossSurfacedNow,
            boolean bossDefeatedByDot,
            String windDriftMessage
    ) { }

    public record RaidDebriefSummary(
            TitanMode.TitanBoss boss,
            TitanMode.TitanDifficulty difficulty,
            boolean won,
            String rank,
            int score,
            int totalDamage,
            int partsBroken,
            int staggers,
            int parries,
            int reactions,
            int elapsedSeconds,
            int aliveShips
    ) { }

    public enum ArenaPropKind {
        COVER_REEF,
        CURRENT_LANE,
        SONAR_BUOY,
        LAVA_FISSURE,
        MANA_PYLON,
        ARCANE_LEYNODE,
        GRAVITY_NODE,
        BEACON_SUPPLY,
        BEACON_MANA,
        BEACON_SHIELD
    }

    public enum ArenaEventKind {
        REEF_BLOCKED,
        REEF_CRUMBLED,
        BEACON_PICKED,
        PYLON_SIPHONED,
        CURRENT_BOOST,
        SONAR_PULSE,
        LEYNODE_BUFF,
        FISSURE_ERUPTED,
        GRAVITY_SHATTER,
        DIRECTIVE_STARTED,
        DIRECTIVE_COMPLETED,
        ULTIMA_EVOLVED,
        ULTIMA_RAGE
    }

    public record ArenaProp(
            int id,
            int row,
            int col,
            ArenaPropKind kind,
            int hp,
            int maxHp,
            long timerMs,
            long cooldownMs,
            boolean active
    ) { }

    public record ArenaFeedbackEvent(
            ArenaEventKind kind,
            int row,
            int col,
            String label,
            int manaDelta,
            int superDelta,
            int shieldDelta,
            int healCells,
            List<int[]> sonarCells,
            TitanMode.DirectiveType directive
    ) { }

    public record DirectiveState(
            TitanMode.DirectiveType type,
            long remainingMs,
            long totalMs,
            boolean active
    ) { }

    private OceanMapBackdrop.MapTheme stageTheme = OceanMapBackdrop.MapTheme.NAVAL_CITADEL;
    private boolean arenaMechanicsEnabled = true;
    private final List<ArenaProp> arenaProps = new ArrayList<>();
    private int nextPropId = 1;
    private long beaconSpawnCooldownMs = 8000L;
    private final List<ArenaFeedbackEvent> pendingArenaEvents = new ArrayList<>();
    private final EnumSet<TitanMode.RaidRelic> activeRelics = EnumSet.noneOf(TitanMode.RaidRelic.class);
    private DirectiveState activeDirective;
    private int directivesIssuedCount;
    private long fireCatalystMs;
    private boolean leynodeBuffActive;
    private long leynodePulseAccumMs;

    private final Random rng = new Random();
    private TitanMode.TitanBoss boss;
    private TitanMode.TitanDifficulty difficulty = TitanMode.TitanDifficulty.NORMAL;
    private int bossMaxHp;
    private int bossHp;
    private boolean enraged;
    private int enrageBarrierHp;
    private int enrageBarrierMaxHp;
    private int bossOffR;
    private int bossOffC;

    private final EnumMap<TitanMode.TitanPart, Integer> partHp = new EnumMap<>(TitanMode.TitanPart.class);
    private final EnumMap<TitanMode.TitanPart, Integer> partMaxHp = new EnumMap<>(TitanMode.TitanPart.class);
    private final EnumMap<TitanMode.TitanPart, Boolean> partBroken = new EnumMap<>(TitanMode.TitanPart.class);
    private final List<TitanMode.TitanPart> lastNewlyBrokenParts = new ArrayList<>();

    // Boss ATB / Status / Stealth / Cataclysm DPS check state
    private TitanMode.TitanSkill pendingSkill;
    private int telegraphRow = 4;
    private int telegraphCol = 4;
    private List<int[]> telegraphCells = List.of();
    private long castElapsedMs;
    private long castTotalMs;
    private long recoveryRemainingMs = 2100L;
    private long idlePositionMs;
    private long submergedMs;
    private long submergedCooldownMs = 14_000L;
    private int skillsSinceMove;
    private boolean dpsCheckActive;
    private boolean dpsCheckDone;
    private boolean dpsCheckInterrupted;

    private int staggerGauge;
    private int staggerThreshold;
    private long staggeredMs;
    private long staggerDecayAccumMs;
    private long antiMissileCooldownMs = 3600L;
    private long chillMs;
    private long exposedMs;
    private int burnStacks;
    private long burnMs;
    private long burnTickAccumMs;

    private int totalDamageDealt;
    private int partsBrokenCount;
    private int staggersCount;
    private int parriesCount;
    private int reactionsCount;
    private long elapsedRaidMs;
    private String lastReactionLabel = "";
    private boolean lastCastWasDpsCheck;

    private final CellState[][] playerGrid = new CellState[Board.SIZE][Board.SIZE];
    private final CellState[][] bossGrid = new CellState[Board.SIZE][Board.SIZE];
    private final int[][] superArmorGrid = new int[Board.SIZE][Board.SIZE];
    private final List<Ship> playerShips = new ArrayList<>();
    private final List<int[]> lastArmorAbsorbedCells = new ArrayList<>();
    private final List<int[]> lastReefBlockedCells = new ArrayList<>();
    private final List<HazardTile> activeHazards = new ArrayList<>();
    private final List<TitanMode.TitanSkill> skillBag = new ArrayList<>();
    private final List<int[]> recentBossTargets = new ArrayList<>();
    private static final int RECENT_BOSS_TARGET_LIMIT = 4;
    private TitanMode.TitanSkill lastPickedSkill;
    private int bossCastSeq;
    private boolean skillBagEnraged;
    /** Giai đoạn hiện tại của Godzilla Ultima (1–4); boss khác luôn là 1. */
    private int ultimaStage = 1;
    /** ★ của form Ultima hiện tại đã mở (form mất 1/3 phần máu của nó). */
    private boolean ultimaStarOpen;
    private boolean phase2DebutPending;
    private long hazardTickAccumMs;
    private long moveCooldownMs;

    private RaidStrikeInfo lastStrikeInfo = new RaidStrikeInfo(0, null, null, false, false, false, 0);

    public TitanBattleController(List<Ship> initialPlayerFleet, TitanMode.TitanBoss initialBoss) {
        this(initialPlayerFleet, initialBoss, TitanMode.TitanDifficulty.NORMAL);
    }

    public TitanBattleController(
            List<Ship> initialPlayerFleet,
            TitanMode.TitanBoss initialBoss,
            TitanMode.TitanDifficulty initialDifficulty
    ) {
        this.boss = initialBoss != null ? initialBoss : TitanMode.TitanBoss.GODZILLA;
        this.difficulty = initialDifficulty != null ? initialDifficulty : TitanMode.TitanDifficulty.NORMAL;
        resetFleet(initialPlayerFleet);
        resetBossOnly(this.boss);
    }

    public TitanMode.TitanBoss getBoss() { return boss; }
    public TitanMode.TitanDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(TitanMode.TitanDifficulty diff) {
        this.difficulty = diff != null ? diff : TitanMode.TitanDifficulty.NORMAL;
    }
    public CellState[][] getPlayerGrid() { return playerGrid; }
    public CellState[][] getBossGrid() { return bossGrid; }
    public List<Ship> getPlayerShips() { return playerShips; }
    public int[][] getSuperArmorGrid() { return superArmorGrid; }
    public int getRemainingHp() { return Math.max(0, bossHp); }
    public int getMaxHp() { return Math.max(1, bossMaxHp); }
    public int getEnrageBarrierHp() { return Math.max(0, enrageBarrierHp); }
    public int getEnrageBarrierMaxHp() { return Math.max(0, enrageBarrierMaxHp); }

    /** Adjusts Titan boss & part HP to match a Campaign Milestone stage target while preserving part ratios. */
    public void applyCampaignMaxHp(int customMaxHp) {
        if (customMaxHp <= 0) return;
        this.bossMaxHp = Math.max(80, customMaxHp);
        this.bossHp = this.bossMaxHp;
        initPart(TitanMode.TitanPart.HEAD, Math.max(18, (int) Math.round(bossMaxHp * 0.23)));
        initPart(TitanMode.TitanPart.CORE_FIN, Math.max(18, (int) Math.round(bossMaxHp * 0.24)));
        initPart(TitanMode.TitanPart.BODY_TAIL, Math.max(18, (int) Math.round(bossMaxHp * 0.26)));
    }

    /** Scales Boss HP, Parts, and Stagger threshold for 2-Player Co-op Boss Raid balance. */
    public void applyCoopBossScale(double hpMultiplier) {
        double mult = Math.max(1.1, Math.min(3.0, hpMultiplier));
        int scaled = Math.max(120, (int) Math.round(bossMaxHp * mult));
        applyCampaignMaxHp(scaled);
        this.staggerThreshold = Math.max(24, (int) Math.round(
                TitanMode.staggerThresholdFor(boss, enraged) * difficulty.staggerThresholdMult * 1.25));
    }

    /** Re-seeds the controller RNG & regenerates Boss/Arena layout deterministically (for LAN Co-op sync). */
    public void reseedForCoop(long seed, double coopHpMultiplier) {
        this.rng.setSeed(seed);
        resetBossOnly(this.boss);
        if (coopHpMultiplier > 1.05) {
            applyCoopBossScale(coopHpMultiplier);
        }
    }

    /** Authoritative Host -> Client snapshot reconciliation to prevent HP/position drift in LAN Co-op. */
    public void applyAuthoritativeSnapshot(
            int hp,
            boolean hostEnraged,
            boolean submerged,
            int offR,
            int offC,
            int headHp,
            int coreHp,
            int tailHp
    ) {
        this.bossHp = Math.max(0, Math.min(this.bossMaxHp, hp));
        this.enraged = hostEnraged;
        if (!hostEnraged) {
            this.enrageBarrierHp = 0;
            this.enrageBarrierMaxHp = 0;
        } else if (this.enrageBarrierMaxHp == 0) {
            this.enrageBarrierMaxHp = computeEnrageBarrierMax();
            this.enrageBarrierHp = this.enrageBarrierMaxHp;
        }
        if (!submerged) {
            this.submergedMs = 0L;
        } else if (this.submergedMs <= 0L) {
            this.submergedMs = 4500L;
        }
        if (this.bossOffR != offR || this.bossOffC != offC) {
            this.bossOffR = Math.max(-2, Math.min(2, offR));
            this.bossOffC = Math.max(-2, Math.min(2, offC));
            syncBossGridSilhouette();
        }
        partHp.put(TitanMode.TitanPart.HEAD, Math.max(0, headHp));
        partBroken.put(TitanMode.TitanPart.HEAD, headHp <= 0);
        partHp.put(TitanMode.TitanPart.CORE_FIN, Math.max(0, coreHp));
        partBroken.put(TitanMode.TitanPart.CORE_FIN, coreHp <= 0);
        partHp.put(TitanMode.TitanPart.BODY_TAIL, Math.max(0, tailHp));
        partBroken.put(TitanMode.TitanPart.BODY_TAIL, tailHp <= 0);
    }
    public boolean isEnraged() { return enraged; }
    public int getBossOffR() { return bossOffR; }
    public int getBossOffC() { return bossOffC; }
    public RaidStrikeInfo getLastStrikeInfo() { return lastStrikeInfo; }

    public TitanMode.TitanSkill getPendingSkill() { return pendingSkill; }
    public int getTelegraphRow() { return telegraphRow; }
    public int getTelegraphCol() { return telegraphCol; }
    public List<int[]> getTelegraphCells() { return telegraphCells; }
    public long getCastElapsedMs() { return castElapsedMs; }
    public long getCastTotalMs() { return castTotalMs; }
    public double getCastProgress() {
        return (pendingSkill == null || castTotalMs <= 0) ? 0.0 : Math.min(1.0, castElapsedMs / (double) castTotalMs);
    }
    public int getStaggerGauge() { return staggerGauge; }
    public int getStaggerThreshold() { return Math.max(1, staggerThreshold); }
    public boolean isBossStaggered() { return staggeredMs > 0; }
    public long getStaggeredMs() { return staggeredMs; }
    public boolean isBossSubmerged() { return submergedMs > 0; }
    public long getSubmergedMs() { return submergedMs; }
    public boolean isBossChilled() { return chillMs > 0; }
    public boolean isBossExposed() { return exposedMs > 0; }
    public int getBurnStacks() { return burnMs > 0 ? burnStacks : 0; }
    public long getMoveCooldownMs() { return moveCooldownMs; }
    public List<HazardTile> getActiveHazards() { return List.copyOf(activeHazards); }
    public boolean isDpsCheckActive() { return dpsCheckActive; }
    public boolean isLastCastDpsCheck() { return lastCastWasDpsCheck; }
    public String getLastReactionLabel() { return lastReactionLabel; }
    public int getTotalDamageDealt() { return totalDamageDealt; }
    public int getStaggersCount() { return staggersCount; }
    public int getParriesCount() { return parriesCount; }
    public int getReactionsCount() { return reactionsCount; }
    public int getPartsBrokenCount() { return partsBrokenCount; }
    public long getElapsedRaidMs() { return elapsedRaidMs; }
    public boolean isWindPushDisabled() { return isPartBroken(TitanMode.TitanPart.BODY_TAIL); }
    public long getAntiMissileCooldownMs() { return Math.max(0L, antiMissileCooldownMs); }
    public boolean isAntiMissileReady() {
        return bossHp > 0 && staggeredMs <= 0 && submergedMs <= 0 && antiMissileCooldownMs <= 0L;
    }

    public record MissileInterceptOutcome(
            boolean intercepted,
            boolean partialPierce,
            TitanMode.AntiMissileSpec counterSpec,
            int staggerGainOnBoss,
            long nextCooldownMs
    ) { }

    /**
     * Evaluates whether the Titan Boss activates its form-specific anti-missile special counter-skill
     * (Phase 1 vs Awakened Phase 2: e.g. Godzilla Atomic Breath -> Thermo Nuclear Pulse) against heavy ordnance.
     */
    public MissileInterceptOutcome tryInterceptPlayerStrike(
            CombatLoadout.WeaponOption weapon,
            CombatLoadout.MagicOption magic,
            boolean ultimateWeapon
    ) {
        if (!isAntiMissileReady()) {
            return new MissileInterceptOutcome(false, false, null, 0, Math.max(0L, antiMissileCooldownMs));
        }
        boolean qualifies = ultimateWeapon
                || weapon == CombatLoadout.WeaponOption.MISSILE_CROSS
                || weapon == CombatLoadout.WeaponOption.AIRSTRIKE
                || (weapon == CombatLoadout.WeaponOption.TORPEDO
                && magic != null && magic != CombatLoadout.MagicOption.NONE);
        if (!qualifies) {
            return new MissileInterceptOutcome(false, false, null, 0, 0L);
        }
        boolean headBroken = isPartBroken(TitanMode.TitanPart.HEAD);
        if (headBroken && ultimateWeapon) {
            return new MissileInterceptOutcome(false, false, null, 0, Math.max(0L, antiMissileCooldownMs));
        }
        boolean partial = ultimateWeapon || headBroken;
        TitanMode.AntiMissileSpec spec = TitanMode.antiMissileSpec(boss, enraged);
        long baseCd = enraged ? 8200L : 11_800L;
        if (difficulty == TitanMode.TitanDifficulty.CATACLYSM) baseCd = Math.round(baseCd * 0.82);
        else if (difficulty == TitanMode.TitanDifficulty.NIGHTMARE) baseCd = Math.round(baseCd * 0.90);
        if (headBroken) baseCd = Math.round(baseCd * 1.45);
        antiMissileCooldownMs = baseCd;
        int staggerGain = partial ? 10 : 8;
        staggerGauge = Math.min(staggerThreshold, staggerGauge + staggerGain);
        return new MissileInterceptOutcome(true, partial, spec, staggerGain, antiMissileCooldownMs);
    }

    public boolean isPlayerShipInsideTelegraph() {
        if (pendingSkill == null || telegraphCells.isEmpty()) return false;
        for (int[] c : telegraphCells) {
            if (SandboxController.inBounds(c[0], c[1]) && playerGrid[c[0]][c[1]] == CellState.SHIP) {
                return true;
            }
        }
        return false;
    }

    public int repairFleetCells(int maxCells) {
        int repaired = 0;
        for (Ship s : intactPlayerShips()) {
            for (int i = 0; i < s.type.length && repaired < maxCells; i++) {
                int r = s.row + (s.horizontal ? 0 : i);
                int c = s.col + (s.horizontal ? i : 0);
                if (SandboxController.inBounds(r, c) && playerGrid[r][c] == CellState.HIT) {
                    playerGrid[r][c] = CellState.SHIP;
                    repaired++;
                }
            }
        }
        return repaired;
    }

    public RaidDebriefSummary buildDebriefSummary(boolean won) {
        int alive = intactPlayerShips().size();
        int elapsedSec = Math.max(1, (int) Math.round(elapsedRaidMs / 1000.0));
        int base = won ? 1000 : 220;
        int raw = base + totalDamageDealt * 2 + partsBrokenCount * 220 + staggersCount * 180
                + parriesCount * 260 + reactionsCount * 110 + alive * 240 - Math.min(600, elapsedSec * 4);
        int score = Math.max(50, (int) Math.round(raw * difficulty.scoreMult));
        String rank;
        if (!won) {
            rank = "C";
        } else if (alive >= 4 && (score >= 2800 || (parriesCount + staggersCount >= 3 && elapsedSec <= 95))) {
            rank = "S";
        } else if (alive >= 3 && score >= 1900) {
            rank = "A";
        } else {
            rank = "B";
        }
        return new RaidDebriefSummary(
                boss, difficulty, won, rank, score, totalDamageDealt,
                partsBrokenCount, staggersCount, parriesCount, reactionsCount, elapsedSec, alive);
    }

    public int getPartHp(TitanMode.TitanPart part) {
        return part == null ? 0 : Math.max(0, partHp.getOrDefault(part, 0));
    }

    public int getPartMaxHp(TitanMode.TitanPart part) {
        return part == null ? 1 : Math.max(1, partMaxHp.getOrDefault(part, 1));
    }

    public boolean isPartBroken(TitanMode.TitanPart part) {
        return part != null && Boolean.TRUE.equals(partBroken.get(part));
    }

    public List<TitanMode.TitanPart> consumeNewlyBrokenParts() {
        List<TitanMode.TitanPart> copy = new ArrayList<>(lastNewlyBrokenParts);
        lastNewlyBrokenParts.clear();
        return copy;
    }

    public List<int[]> consumeLastArmorAbsorbedCells() {
        List<int[]> copy = new ArrayList<>(lastArmorAbsorbedCells);
        lastArmorAbsorbedCells.clear();
        return copy;
    }

    public List<int[]> consumeLastReefBlockedCells() {
        List<int[]> copy = new ArrayList<>(lastReefBlockedCells);
        lastReefBlockedCells.clear();
        return copy;
    }

    public void activateSuperArmorForLastShip() {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) superArmorGrid[r][c] = 0;
        }
        Ship survivor = findOnlyAliveShip();
        if (survivor == null) return;
        for (int i = 0; i < survivor.type.length; i++) {
            int r = survivor.row + (survivor.horizontal ? 0 : i);
            int c = survivor.col + (survivor.horizontal ? i : 0);
            if (SandboxController.inBounds(r, c) && playerGrid[r][c] == CellState.SHIP) {
                superArmorGrid[r][c] = 2;
            }
        }
    }

    public void reset(TitanMode.TitanBoss newBoss) {
        List<Ship> copy = new ArrayList<>();
        for (Ship s : playerShips) copy.add(new Ship(s.type, s.row, s.col, s.horizontal));
        resetFleet(copy);
        resetBossOnly(newBoss != null ? newBoss : this.boss);
    }

    public void switchBossOnly(TitanMode.TitanBoss newBoss) {
        resetBossOnly(newBoss != null ? newBoss : this.boss);
    }

    public void configureArenaStage(OceanMapBackdrop.MapTheme theme, boolean enabled) {
        this.stageTheme = theme != null ? theme : OceanMapBackdrop.MapTheme.NAVAL_CITADEL;
        this.arenaMechanicsEnabled = enabled;
        generateSafeArenaProps();
    }

    public OceanMapBackdrop.MapTheme getStageTheme() {
        return stageTheme;
    }

    public boolean isArenaMechanicsEnabled() {
        return arenaMechanicsEnabled;
    }

    public List<ArenaProp> getArenaProps() {
        return List.copyOf(arenaProps);
    }

    public List<ArenaFeedbackEvent> consumeArenaEvents() {
        if (pendingArenaEvents.isEmpty()) return List.of();
        List<ArenaFeedbackEvent> copy = new ArrayList<>(pendingArenaEvents);
        pendingArenaEvents.clear();
        return copy;
    }

    public DirectiveState getActiveDirective() {
        return activeDirective;
    }

    public boolean isLeynodeBuffActive() {
        return leynodeBuffActive;
    }

    public boolean isFireCatalystActive() {
        return fireCatalystMs > 0L;
    }

    public int getKeyboardMoveStepCells() {
        return (arenaMechanicsEnabled && stageTheme == OceanMapBackdrop.MapTheme.GALAXY_COSMOS) ? 2 : 1;
    }

    public EnumSet<TitanMode.RaidRelic> getActiveRelics() {
        return activeRelics.isEmpty()
                ? EnumSet.noneOf(TitanMode.RaidRelic.class)
                : EnumSet.copyOf(activeRelics);
    }

    public void addRelic(TitanMode.RaidRelic relic) {
        if (relic == null) return;
        activeRelics.add(relic);
        if (relic == TitanMode.RaidRelic.BASTION && arenaMechanicsEnabled) {
            for (int i = 0; i < arenaProps.size(); i++) {
                ArenaProp p = arenaProps.get(i);
                if (p.kind() == ArenaPropKind.COVER_REEF) {
                    arenaProps.set(i, new ArenaProp(p.id(), p.row(), p.col(), p.kind(),
                            Math.min(3, p.hp() + 1), 3, p.timerMs(), p.cooldownMs(), p.active()));
                }
            }
        }
    }

    public void clearRelics() {
        activeRelics.clear();
    }

    public List<TitanMode.RaidRelic> pickRelicChoices(int count) {
        List<TitanMode.RaidRelic> unowned = new ArrayList<>();
        List<TitanMode.RaidRelic> all = new ArrayList<>();
        for (TitanMode.RaidRelic r : TitanMode.RaidRelic.values()) {
            all.add(r);
            if (!activeRelics.contains(r)) unowned.add(r);
        }
        java.util.Collections.shuffle(unowned, rng);
        java.util.Collections.shuffle(all, rng);
        List<TitanMode.RaidRelic> out = new ArrayList<>();
        for (TitanMode.RaidRelic r : unowned) {
            if (out.size() >= count) break;
            out.add(r);
        }
        for (TitanMode.RaidRelic r : all) {
            if (out.size() >= count) break;
            if (!out.contains(r)) out.add(r);
        }
        return out;
    }

    public boolean triggerDirectiveForTest(TitanMode.DirectiveType forced) {
        TitanMode.DirectiveType type = forced != null ? forced : TitanMode.DirectiveType.WEAKPOINT_STRIKE;
        activeDirective = new DirectiveState(type, type.durationMs, type.durationMs, true);
        pendingArenaEvents.add(new ArenaFeedbackEvent(
                ArenaEventKind.DIRECTIVE_STARTED, 0, 0, type.title, 0, 0, 0, 0, List.of(), type));
        return true;
    }

    public void forceSpawnPropForTest(ArenaPropKind kind, int row, int col) {
        if (!SandboxController.inBounds(row, col) || kind == null) return;
        arenaProps.removeIf(p -> p.row() == row && p.col() == col);
        int hp = (kind == ArenaPropKind.COVER_REEF) ? (activeRelics.contains(TitanMode.RaidRelic.BASTION) ? 3 : 2) : 1;
        long timer = (kind == ArenaPropKind.LAVA_FISSURE) ? 1600L : 0L;
        arenaProps.add(new ArenaProp(nextPropId++, row, col, kind, hp, hp, timer, 0L, kind != ArenaPropKind.LAVA_FISSURE));
    }

    private void resetFleet(List<Ship> initialPlayerFleet) {
        SandboxController.clearGrid(playerGrid);
        playerShips.clear();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) superArmorGrid[r][c] = 0;
        }
        if (initialPlayerFleet != null && initialPlayerFleet.size() == ShipType.values().length
                && SandboxController.placeProvidedFleet(initialPlayerFleet, playerGrid, playerShips)) {
            // placed provided fleet
        } else {
            SandboxController.clearGrid(playerGrid);
            playerShips.clear();
            SandboxController.generateRandomFleet(playerGrid, playerShips, rng);
        }
        activeHazards.clear();
        hazardTickAccumMs = 0L;
        moveCooldownMs = 0L;
        totalDamageDealt = 0;
        partsBrokenCount = 0;
        staggersCount = 0;
        parriesCount = 0;
        reactionsCount = 0;
        elapsedRaidMs = 0L;
    }

    private void resetBossOnly(TitanMode.TitanBoss targetBoss) {
        this.boss = targetBoss;
        this.bossMaxHp = TitanMode.scaledMaxHp(targetBoss, difficulty);
        this.bossHp = bossMaxHp;
        this.enraged = false;
        this.enrageBarrierHp = 0;
        this.enrageBarrierMaxHp = 0;
        this.bossOffR = 0;
        this.bossOffC = 0;

        initPart(TitanMode.TitanPart.HEAD, Math.max(20, (int) Math.round(bossMaxHp * 0.23)));
        initPart(TitanMode.TitanPart.CORE_FIN, Math.max(20, (int) Math.round(bossMaxHp * 0.24)));
        initPart(TitanMode.TitanPart.BODY_TAIL, Math.max(20, (int) Math.round(bossMaxHp * 0.26)));
        lastNewlyBrokenParts.clear();

        pendingSkill = null;
        telegraphCells = List.of();
        castElapsedMs = 0L;
        castTotalMs = 0L;
        recoveryRemainingMs = Math.max(850L, Math.round(1450L / difficulty.atbSpeedMult));
        idlePositionMs = 0L;
        submergedMs = 0L;
        submergedCooldownMs = 12_000L;
        skillsSinceMove = 0;
        dpsCheckActive = false;
        dpsCheckDone = false;
        dpsCheckInterrupted = false;
        lastCastWasDpsCheck = false;

        staggerGauge = 0;
        staggerDecayAccumMs = 0L;
        staggerThreshold = computeEffectiveStaggerThreshold(false);
        staggeredMs = 0L;
        chillMs = 0L;
        exposedMs = 0L;
        burnStacks = 0;
        burnMs = 0L;
        burnTickAccumMs = 0L;
        lastReactionLabel = "";
        lastStrikeInfo = new RaidStrikeInfo(0, null, null, false, false, false, 0);
        skillBag.clear();
        recentBossTargets.clear();
        lastPickedSkill = null;
        bossCastSeq = 0;
        skillBagEnraged = false;
        ultimaStage = 1;
        ultimaStarOpen = false;
        TitanMode.setUltimaStage(1);
        phase2DebutPending = false;
        activeDirective = null;
        directivesIssuedCount = 0;
        fireCatalystMs = 0L;
        leynodeBuffActive = false;
        leynodePulseAccumMs = 0L;
        pendingArenaEvents.clear();

        syncBossGridSilhouette();
        generateSafeArenaProps();
    }

    private void initPart(TitanMode.TitanPart part, int hp) {
        partMaxHp.put(part, hp);
        partHp.put(part, hp);
        partBroken.put(part, false);
    }

    public double effectiveWeaponCooldownMult(boolean superShipActive) {
        FleetBuffSummary buffs = computeFleetBuffs(superShipActive);
        double mult = buffs.cooldownSpeedMult();
        if (activeRelics.contains(TitanMode.RaidRelic.CAPACITOR)) mult *= 0.85;
        if (leynodeBuffActive) mult *= 0.65;
        return mult;
    }

    public double effectiveShieldDrainMult(boolean superShipActive) {
        double mult = computeFleetBuffs(superShipActive).shieldDrainMult();
        if (activeRelics.contains(TitanMode.RaidRelic.AEGIS)) mult *= 0.78;
        return mult;
    }

    public double effectiveManaRegenMult(boolean superShipActive) {
        double mult = computeFleetBuffs(superShipActive).manaRegenMult();
        if (activeRelics.contains(TitanMode.RaidRelic.HYPERION_CELL)) mult *= 1.30;
        return mult;
    }

    public double effectiveSuperGainMult() {
        return activeRelics.contains(TitanMode.RaidRelic.HYPERION_CELL) ? 1.25 : 1.0;
    }

    public void forceGenerateSafeArenaProps() {
        generateSafeArenaProps();
    }

    private void generateSafeArenaProps() {
        arenaProps.clear();
        activeDirective = null;
        directivesIssuedCount = 0;
        fireCatalystMs = 0L;
        leynodeBuffActive = false;
        leynodePulseAccumMs = 0L;
        beaconSpawnCooldownMs = 16_000L;
        pendingArenaEvents.clear();
        if (!arenaMechanicsEnabled || playerShips.isEmpty()) return;

        List<int[]> emptyCells = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (playerGrid[r][c] == CellState.EMPTY && SandboxController.findShipAt(playerShips, r, c) == null) {
                    emptyCells.add(new int[]{r, c});
                }
            }
        }
        java.util.Collections.shuffle(emptyCells, rng);

        // 1. Place up to 3 Destructible Cover Reefs spaced Chebyshev >= 2, guaranteeing >= 3 placements per alive ship
        int reefMaxHp = activeRelics.contains(TitanMode.RaidRelic.BASTION) ? 3 : 2;
        int reefsPlaced = 0;
        for (int[] cell : emptyCells) {
            if (reefsPlaced >= 3) break;
            int r = cell[0], c = cell[1];
            boolean spaced = true;
            for (ArenaProp existing : arenaProps) {
                if (existing.kind() == ArenaPropKind.COVER_REEF
                        && Math.max(Math.abs(existing.row() - r), Math.abs(existing.col() - c)) < 2) {
                    spaced = false;
                    break;
                }
            }
            if (!spaced) continue;
            ArenaProp candidate = new ArenaProp(nextPropId++, r, c, ArenaPropKind.COVER_REEF,
                    reefMaxHp, reefMaxHp, 0L, 0L, true);
            arenaProps.add(candidate);
            if (allAliveShipsHaveMinPlacements(3)) {
                reefsPlaced++;
            } else {
                arenaProps.remove(arenaProps.size() - 1);
            }
        }

        // 2. Theme-Specific Interactive Stage Nodes
        List<int[]> freeNodes = collectFreeCells(false);
        java.util.Collections.shuffle(freeNodes, rng);
        switch (stageTheme) {
            case NAVAL_CITADEL -> {
                placeCurrentLaneStrip(freeNodes);
                freeNodes = collectFreeCells(false);
                java.util.Collections.shuffle(freeNodes, rng);
                if (!freeNodes.isEmpty()) {
                    int[] b = freeNodes.get(0);
                    arenaProps.add(new ArenaProp(nextPropId++, b[0], b[1], ArenaPropKind.SONAR_BUOY,
                            1, 1, 0L, 0L, true));
                }
            }
            case APOCALYPSE -> {
                int targetFissures = Math.min(3, freeNodes.size());
                int placed = 0;
                for (int[] f : freeNodes) {
                    if (placed >= targetFissures) break;
                    if (hasPropWithinChebyshev(f[0], f[1], 1)) continue;
                    arenaProps.add(new ArenaProp(nextPropId++, f[0], f[1], ArenaPropKind.LAVA_FISSURE,
                            1, 1, 3600L + placed * 2400L, 0L, false));
                    placed++;
                }
            }
            case ABYSSAL_ATLANTIS -> placeSpacedThemeNodes(freeNodes, ArenaPropKind.MANA_PYLON, 2);
            case FANTASY_REALM -> placeSpacedThemeNodes(freeNodes, ArenaPropKind.ARCANE_LEYNODE, 2);
            case GALAXY_COSMOS -> placeSpacedThemeNodes(freeNodes, ArenaPropKind.GRAVITY_NODE, 2);
        }

        // 3. Spawn initial Floating Beacons (max 2 normally, 3 with Bastion relic)
        int initialBeacons = activeRelics.contains(TitanMode.RaidRelic.BASTION) ? 3 : 2;
        for (int i = 0; i < initialBeacons; i++) {
            spawnFloatingBeacon();
        }
    }

    private void placeCurrentLaneStrip(List<int[]> freeNodes) {
        for (int[] start : freeNodes) {
            int r = start[0], c = start[1];
            for (boolean horiz : new boolean[]{true, false}) {
                List<int[]> strip = new ArrayList<>(3);
                boolean ok = true;
                for (int k = 0; k < 3; k++) {
                    int nr = r + (horiz ? 0 : k);
                    int nc = c + (horiz ? k : 0);
                    if (!SandboxController.inBounds(nr, nc) || hasBlockingCoverReef(nr, nc)) {
                        ok = false;
                        break;
                    }
                    strip.add(new int[]{nr, nc});
                }
                if (ok) {
                    for (int[] cell : strip) {
                        arenaProps.add(new ArenaProp(nextPropId++, cell[0], cell[1], ArenaPropKind.CURRENT_LANE,
                                1, 1, 0L, 0L, true));
                    }
                    return;
                }
            }
        }
    }

    private void placeSpacedThemeNodes(List<int[]> freeNodes, ArenaPropKind kind, int count) {
        int placed = 0;
        for (int[] cell : freeNodes) {
            if (placed >= count) break;
            if (hasPropWithinChebyshev(cell[0], cell[1], 1)) continue;
            arenaProps.add(new ArenaProp(nextPropId++, cell[0], cell[1], kind, 1, 1, 0L, 0L, true));
            placed++;
        }
    }

    private void spawnFloatingBeacon() {
        int maxBeacons = activeRelics.contains(TitanMode.RaidRelic.BASTION) ? 3 : 2;
        int curBeacons = countActiveBeacons();
        if (curBeacons >= maxBeacons) return;
        List<int[]> free = collectFreeCells(true);
        if (free.isEmpty()) free = collectFreeCells(false);
        if (free.isEmpty()) return;
        int[] chosen = free.get(rng.nextInt(free.size()));
        ArenaPropKind[] types = {ArenaPropKind.BEACON_MANA, ArenaPropKind.BEACON_SUPPLY, ArenaPropKind.BEACON_SHIELD};
        ArenaPropKind kind = types[(nextPropId + rng.nextInt(types.length)) % types.length];
        arenaProps.add(new ArenaProp(nextPropId++, chosen[0], chosen[1], kind, 1, 1, 0L, 0L, true));
    }

    private int countActiveBeacons() {
        int c = 0;
        for (ArenaProp p : arenaProps) {
            if (isBeaconKind(p.kind())) c++;
        }
        return c;
    }

    private static boolean isBeaconKind(ArenaPropKind k) {
        return k == ArenaPropKind.BEACON_SUPPLY || k == ArenaPropKind.BEACON_MANA || k == ArenaPropKind.BEACON_SHIELD;
    }

    private boolean hasPropWithinChebyshev(int r, int c, int dist) {
        for (ArenaProp p : arenaProps) {
            if (Math.max(Math.abs(p.row() - r), Math.abs(p.col() - c)) <= dist) return true;
        }
        return false;
    }

    private boolean hasBlockingCoverReef(int r, int c) {
        for (ArenaProp p : arenaProps) {
            if (p.kind() == ArenaPropKind.COVER_REEF && p.hp() > 0 && p.row() == r && p.col() == c) return true;
        }
        return false;
    }

    private List<int[]> collectFreeCells(boolean excludePlayerShips) {
        List<int[]> list = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (excludePlayerShips && (playerGrid[r][c] != CellState.EMPTY
                        || SandboxController.findShipAt(playerShips, r, c) != null)) continue;
                boolean busy = false;
                for (ArenaProp p : arenaProps) {
                    if (p.row() == r && p.col() == c) {
                        busy = true;
                        break;
                    }
                }
                if (!busy) list.add(new int[]{r, c});
            }
        }
        return list;
    }

    private boolean allAliveShipsHaveMinPlacements(int minCount) {
        for (Ship s : playerShips) {
            if (SandboxController.isShipSunk(playerGrid, s)) continue;
            int valid = 0;
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    if (canPlaceRelocatedShip(s.type, r, c, true)) valid++;
                    if (valid >= minCount) break;
                    if (canPlaceRelocatedShip(s.type, r, c, false)) valid++;
                    if (valid >= minCount) break;
                }
                if (valid >= minCount) break;
            }
            if (valid < minCount) return false;
        }
        return true;
    }

    private void stepArenaSystems(long dtMs) {
        if (fireCatalystMs > 0L) fireCatalystMs = Math.max(0L, fireCatalystMs - dtMs);

        if (activeDirective != null && activeDirective.active()) {
            long rem = activeDirective.remainingMs() - dtMs;
            activeDirective = rem > 0L
                    ? new DirectiveState(activeDirective.type(), rem, activeDirective.totalMs(), true)
                    : null;
        }
        if (arenaMechanicsEnabled && activeDirective == null && directivesIssuedCount < 2) {
            long thresholdMs = directivesIssuedCount == 0 ? 12_000L : 36_000L;
            if (elapsedRaidMs >= thresholdMs) {
                TitanMode.DirectiveType nextDir = pickSmartDirective();
                directivesIssuedCount++;
                activeDirective = new DirectiveState(nextDir, nextDir.durationMs, nextDir.durationMs, true);
                pendingArenaEvents.add(new ArenaFeedbackEvent(
                        ArenaEventKind.DIRECTIVE_STARTED, 0, 0, nextDir.title, 0, 0, 0, 0, List.of(), nextDir));
            }
        }
        if (!arenaMechanicsEnabled) return;

        boolean anyShipOnLeynode = false;
        for (int i = 0; i < arenaProps.size(); i++) {
            ArenaProp p = arenaProps.get(i);
            long cd = Math.max(0L, p.cooldownMs() - dtMs);
            if (p.kind() == ArenaPropKind.LAVA_FISSURE) {
                if (p.active()) {
                    long t = p.timerMs() - dtMs;
                    if (t <= 0L) {
                        arenaProps.set(i, new ArenaProp(p.id(), p.row(), p.col(), p.kind(), p.hp(), p.maxHp(),
                                6800L, 0L, false));
                    } else {
                        arenaProps.set(i, new ArenaProp(p.id(), p.row(), p.col(), p.kind(), p.hp(), p.maxHp(),
                                t, 0L, true));
                    }
                } else if (p.cooldownMs() > 0L) {
                    if (cd <= 0L) {
                        addOrRefreshHazard(p.row(), p.col(), TitanMode.HazardType.BURN, 2300L);
                        fireCatalystMs = Math.max(fireCatalystMs, 5500L);
                        arenaProps.set(i, new ArenaProp(p.id(), p.row(), p.col(), p.kind(), p.hp(), p.maxHp(),
                                2300L, 0L, true));
                        pendingArenaEvents.add(new ArenaFeedbackEvent(
                                ArenaEventKind.FISSURE_ERUPTED, p.row(), p.col(), "KHE DUNG NHAM!",
                                0, 0, 0, 0, List.of(), null));
                    } else {
                        arenaProps.set(i, new ArenaProp(p.id(), p.row(), p.col(), p.kind(), p.hp(), p.maxHp(),
                                0L, cd, false));
                    }
                } else {
                    long t = p.timerMs() - dtMs;
                    if (t <= 0L) {
                        arenaProps.set(i, new ArenaProp(p.id(), p.row(), p.col(), p.kind(), p.hp(), p.maxHp(),
                                0L, 1500L, false));
                    } else {
                        arenaProps.set(i, new ArenaProp(p.id(), p.row(), p.col(), p.kind(), p.hp(), p.maxHp(),
                                t, 0L, false));
                    }
                }
            } else {
                if (cd != p.cooldownMs()) {
                    arenaProps.set(i, new ArenaProp(p.id(), p.row(), p.col(), p.kind(), p.hp(), p.maxHp(),
                            p.timerMs(), cd, p.active()));
                }
                if (p.kind() == ArenaPropKind.ARCANE_LEYNODE && isAnyAliveShipAt(p.row(), p.col())) {
                    anyShipOnLeynode = true;
                }
            }
        }
        leynodeBuffActive = anyShipOnLeynode;
        if (leynodeBuffActive) {
            leynodePulseAccumMs += dtMs;
            if (leynodePulseAccumMs >= 1000L) {
                leynodePulseAccumMs -= 1000L;
                pendingArenaEvents.add(new ArenaFeedbackEvent(
                        ArenaEventKind.LEYNODE_BUFF, 0, 0, "KẾT GIỚI +4% KHIÊN",
                        0, 0, 4, 0, List.of(), null));
            }
        } else {
            leynodePulseAccumMs = 0L;
        }

        int maxBeacons = activeRelics.contains(TitanMode.RaidRelic.BASTION) ? 3 : 2;
        if (countActiveBeacons() < maxBeacons) {
            beaconSpawnCooldownMs -= dtMs;
            if (beaconSpawnCooldownMs <= 0L) {
                spawnFloatingBeacon();
                beaconSpawnCooldownMs = 16_000L;
            }
        } else {
            beaconSpawnCooldownMs = 16_000L;
        }
    }

    private boolean isAnyAliveShipAt(int r, int c) {
        Ship s = SandboxController.findShipAt(playerShips, r, c);
        return s != null && !SandboxController.isShipSunk(playerGrid, s);
    }

    private TitanMode.DirectiveType pickSmartDirective() {
        List<TitanMode.DirectiveType> pool = new ArrayList<>();
        if (pendingSkill != null) pool.add(TitanMode.DirectiveType.STAGGER_WINDOW);
        if (countActiveBeacons() > 0 || stageTheme == OceanMapBackdrop.MapTheme.ABYSSAL_ATLANTIS) {
            pool.add(TitanMode.DirectiveType.BEACON_RUN);
        }
        pool.add(TitanMode.DirectiveType.WEAKPOINT_STRIKE);
        pool.add(TitanMode.DirectiveType.ELEMENTAL_COMBO);
        pool.add(TitanMode.DirectiveType.DODGE_DANCE);
        return pool.get(rng.nextInt(pool.size()));
    }

    private boolean tryCompleteDirective(TitanMode.DirectiveType matchedType, int r, int c) {
        if (activeDirective == null || !activeDirective.active() || activeDirective.type() != matchedType) return false;
        activeDirective = null;
        int manaGain = 2;
        int superGain = 15;
        int shieldGain = 0;
        int healCells = 0;
        switch (matchedType) {
            case STAGGER_WINDOW -> {
                manaGain = 2;
                superGain = 20;
                staggerGauge = Math.min(staggerThreshold, staggerGauge + 15);
            }
            case BEACON_RUN -> {
                manaGain = 2;
                healCells = repairFleetCells(1);
                shieldGain = 15;
            }
            case ELEMENTAL_COMBO -> {
                manaGain = 3;
                superGain = 12;
                if (bossHp > 0) {
                    int trueDmg = 18;
                    totalDamageDealt += trueDmg;
                    bossHp = Math.max(0, bossHp - trueDmg);
                }
            }
            case WEAKPOINT_STRIKE -> {
                manaGain = 1;
                superGain = 15;
            }
            case DODGE_DANCE -> {
                manaGain = 2;
                superGain = 18;
            }
        }
        pendingArenaEvents.add(new ArenaFeedbackEvent(
                ArenaEventKind.DIRECTIVE_COMPLETED, r, c, "HOÀN THÀNH: " + matchedType.title,
                manaGain, superGain, shieldGain, healCells, List.of(), matchedType));
        return true;
    }

    private boolean absorbHitWithCoverReef(int r, int col, boolean cellHasLivingShip, Set<Integer> neighborUsedReefIds) {
        if (!arenaMechanicsEnabled) return false;
        for (int i = 0; i < arenaProps.size(); i++) {
            ArenaProp p = arenaProps.get(i);
            if (p.kind() != ArenaPropKind.COVER_REEF || p.hp() <= 0) continue;
            boolean direct = (p.row() == r && p.col() == col);
            boolean orthoAdjacent = cellHasLivingShip
                    && (Math.abs(p.row() - r) + Math.abs(p.col() - col) == 1)
                    && !neighborUsedReefIds.contains(p.id());
            if (!direct && !orthoAdjacent) continue;
            if (orthoAdjacent) neighborUsedReefIds.add(p.id());
            int nextHp = p.hp() - 1;
            if (nextHp <= 0) {
                arenaProps.remove(i);
                int manaBurst = (stageTheme == OceanMapBackdrop.MapTheme.GALAXY_COSMOS) ? 2 : 0;
                String lbl = manaBurst > 0 ? "VỠ THIÊN THẠCH +2 MANA!" : "ĐÁ CHẮN VỠ!";
                pendingArenaEvents.add(new ArenaFeedbackEvent(
                        manaBurst > 0 ? ArenaEventKind.GRAVITY_SHATTER : ArenaEventKind.REEF_CRUMBLED,
                        p.row(), p.col(), lbl, manaBurst, manaBurst > 0 ? 10 : 0, 0, 0, List.of(), null));
            } else {
                arenaProps.set(i, new ArenaProp(p.id(), p.row(), p.col(), p.kind(),
                        nextHp, p.maxHp(), p.timerMs(), p.cooldownMs(), true));
                pendingArenaEvents.add(new ArenaFeedbackEvent(
                        ArenaEventKind.REEF_BLOCKED, p.row(), p.col(), "ĐÁ CHẮN ĐỠ ĐÒN!",
                        0, 4, 0, 0, List.of(), null));
            }
            return true;
        }
        return false;
    }

    private void triggerPassThroughPropsForShip(Ship ship, boolean ignoreCooldown) {
        if (!arenaMechanicsEnabled || ship == null) return;
        boolean currentBoostApplied = false;
        for (int sIdx = 0; sIdx < ship.type.length; sIdx++) {
            int sr = ship.row + (ship.horizontal ? 0 : sIdx);
            int sc = ship.col + (ship.horizontal ? sIdx : 0);
            for (int i = arenaProps.size() - 1; i >= 0; i--) {
                ArenaProp p = arenaProps.get(i);
                if (p.row() != sr || p.col() != sc) continue;
                switch (p.kind()) {
                    case CURRENT_LANE -> {
                        if (!ignoreCooldown && !currentBoostApplied && p.cooldownMs() <= 0L) {
                            currentBoostApplied = true;
                            moveCooldownMs = Math.round(moveCooldownMs * 0.60);
                            arenaProps.set(i, new ArenaProp(p.id(), p.row(), p.col(), p.kind(),
                                    1, 1, 0L, 2500L, true));
                            pendingArenaEvents.add(new ArenaFeedbackEvent(
                                    ArenaEventKind.CURRENT_BOOST, sr, sc, "HẢI LƯU -40% CD!",
                                    0, 0, 0, 0, List.of(), null));
                        }
                    }
                    case SONAR_BUOY -> {
                        if (p.cooldownMs() <= 0L) {
                            arenaProps.set(i, new ArenaProp(p.id(), p.row(), p.col(), p.kind(),
                                    1, 1, 0L, 8000L, true));
                            List<int[]> weak = TitanMode.generateWeakpointCells(boss, bossOffR, bossOffC);
                            List<int[]> scan = weak.subList(0, Math.min(3, weak.size()));
                            pendingArenaEvents.add(new ArenaFeedbackEvent(
                                    ArenaEventKind.SONAR_PULSE, sr, sc, "PHAO SONAR +1 MANA!",
                                    1, 6, 0, 0, new ArrayList<>(scan), null));
                            tryCompleteDirective(TitanMode.DirectiveType.BEACON_RUN, sr, sc);
                        }
                    }
                    case MANA_PYLON -> {
                        if (p.cooldownMs() <= 0L) {
                            arenaProps.set(i, new ArenaProp(p.id(), p.row(), p.col(), p.kind(),
                                    1, 1, 0L, 8000L, true));
                            pendingArenaEvents.add(new ArenaFeedbackEvent(
                                    ArenaEventKind.PYLON_SIPHONED, sr, sc, "TRỤ CỔ NGỮ +2 MANA!",
                                    2, 10, 0, 0, List.of(), null));
                            tryCompleteDirective(TitanMode.DirectiveType.BEACON_RUN, sr, sc);
                        }
                    }
                    case GRAVITY_NODE -> {
                        if (p.cooldownMs() <= 0L) {
                            arenaProps.set(i, new ArenaProp(p.id(), p.row(), p.col(), p.kind(),
                                    1, 1, 0L, 7000L, true));
                            pendingArenaEvents.add(new ArenaFeedbackEvent(
                                    ArenaEventKind.PYLON_SIPHONED, sr, sc, "HỐ TRỌNG LỰC +1 MANA!",
                                    1, 12, 0, 0, List.of(), null));
                        }
                    }
                    case BEACON_SUPPLY -> {
                        arenaProps.remove(i);
                        int repaired = repairFleetCells(1);
                        if (repaired == 0 && SandboxController.inBounds(sr, sc)) {
                            superArmorGrid[sr][sc] = Math.min(2, superArmorGrid[sr][sc] + 1);
                        }
                        beaconSpawnCooldownMs = Math.min(beaconSpawnCooldownMs, 16_000L);
                        pendingArenaEvents.add(new ArenaFeedbackEvent(
                                ArenaEventKind.BEACON_PICKED, sr, sc,
                                repaired > 0 ? "TIẾP TẾ: VÁ +1 Ô!" : "TIẾP TẾ: +1 GIÁP!",
                                0, 10, 0, repaired, List.of(), null));
                        tryCompleteDirective(TitanMode.DirectiveType.BEACON_RUN, sr, sc);
                    }
                    case BEACON_MANA -> {
                        arenaProps.remove(i);
                        beaconSpawnCooldownMs = Math.min(beaconSpawnCooldownMs, 16_000L);
                        pendingArenaEvents.add(new ArenaFeedbackEvent(
                                ArenaEventKind.BEACON_PICKED, sr, sc, "NẠP HẢI NĂNG +3 MANA!",
                                3, 8, 0, 0, List.of(), null));
                        tryCompleteDirective(TitanMode.DirectiveType.BEACON_RUN, sr, sc);
                    }
                    case BEACON_SHIELD -> {
                        arenaProps.remove(i);
                        beaconSpawnCooldownMs = Math.min(beaconSpawnCooldownMs, 16_000L);
                        pendingArenaEvents.add(new ArenaFeedbackEvent(
                                ArenaEventKind.BEACON_PICKED, sr, sc, "NẠP KHIÊN +22%!",
                                0, 12, 22, 0, List.of(), null));
                        tryCompleteDirective(TitanMode.DirectiveType.BEACON_RUN, sr, sc);
                    }
                    default -> { }
                }
            }
        }
    }

    private void syncBossGridSilhouette() {
        SandboxController.clearGrid(bossGrid);
        for (int[] c : TitanMode.generateHitboxCells(boss, bossOffR, bossOffC)) {
            bossGrid[c[0]][c[1]] = CellState.SHIP;
        }
    }

    public List<int[]> listIntactBossCells() {
        return TitanMode.generateHitboxCells(boss, bossOffR, bossOffC);
    }

    public FleetBuffSummary computeFleetBuffs(boolean superShipActive) {
        if (superShipActive) {
            return new FleetBuffSummary(true, true, true, true, true);
        }
        boolean carrier = false;
        boolean battleship = false;
        boolean cruiser = false;
        boolean sub = false;
        boolean dd = false;
        for (Ship s : playerShips) {
            if (!SandboxController.isShipSunk(playerGrid, s)) {
                switch (s.type) {
                    case CARRIER -> carrier = true;
                    case BATTLESHIP -> battleship = true;
                    case CRUISER -> cruiser = true;
                    case SUBMARINE -> sub = true;
                    case DESTROYER -> dd = true;
                }
            }
        }
        return new FleetBuffSummary(carrier, battleship, cruiser, sub, dd);
    }

    /** Legacy 4-arg strike overload kept compatible for existing verification callers & sandbox helpers. */
    public PlayerStrikeOutcome fireAtTitan(
            int row,
            int col,
            List<int[]> cells,
            CombatLoadout.MagicOption magic
    ) {
        return fireAtTitan(row, col, cells, CombatLoadout.WeaponOption.CANNON, magic, false);
    }

    /** Repeatable numerical-damage Raid weapon strike with multi-part multipliers, breaks, status, & stagger interrupts. */
    public PlayerStrikeOutcome fireAtTitan(
            int row,
            int col,
            List<int[]> cells,
            CombatLoadout.WeaponOption weapon,
            CombatLoadout.MagicOption magic,
            boolean superShipActive
    ) {
        return fireAtTitan(row, col, cells, weapon, magic, superShipActive, 1.0);
    }

    /** Overload allowing mid-air Boss interception attenuation while preserving full status & part accounting. */
    public PlayerStrikeOutcome fireAtTitan(
            int row,
            int col,
            List<int[]> cells,
            CombatLoadout.WeaponOption weapon,
            CombatLoadout.MagicOption magic,
            boolean superShipActive,
            double interceptDamageMult
    ) {
        CombatLoadout.WeaponOption effWeapon = weapon != null ? weapon : CombatLoadout.WeaponOption.CANNON;
        CombatLoadout.MagicOption effMagic = magic != null ? magic : CombatLoadout.MagicOption.NONE;
        FleetBuffSummary buffs = computeFleetBuffs(superShipActive);

        List<int[]> targetList = (cells == null || cells.isEmpty()) ? List.of(new int[]{row, col}) : cells;
        List<int[]> hitCells = new ArrayList<>();
        List<int[]> missCells = new ArrayList<>();
        List<int[]> weakHits = new ArrayList<>();

        double totalRawDmg = 0.0;
        double totalPartRawDmg = 0.0;
        double totalStagger = 0.0;
        int baseCellDmg = TitanMode.weaponBaseCellDamage(effWeapon);
        TitanMode.TitanPart bestPart = null;
        TitanMode.TitanPart chargingPart = pendingSkill != null ? TitanMode.chargingPartForSkill(pendingSkill) : null;
        boolean hitChargingPart = false;
        EnumMap<TitanMode.TitanPart, Double> partRawMap = new EnumMap<>(TitanMode.TitanPart.class);
        lastNewlyBrokenParts.clear();

        int bossHitIdx = 0;
        for (int[] c : targetList) {
            int r = c[0], colIdx = c[1];
            if (!SandboxController.inBounds(r, colIdx)) continue;
            TitanMode.TitanPart part = TitanMode.partAtCell(boss, r, colIdx, bossOffR, bossOffC);
            if (part != null) {
                hitCells.add(new int[]{r, colIdx});
                if (part == TitanMode.TitanPart.HEAD || part == TitanMode.TitanPart.CORE_FIN) {
                    weakHits.add(new int[]{r, colIdx});
                }
                if (bestPart == null || (!isPartBroken(part) && (isPartBroken(bestPart) || part.damageMult > bestPart.damageMult))
                        || (isPartBroken(bestPart) && part.damageMult > bestPart.damageMult)) {
                    bestPart = part;
                }
                if (chargingPart == part) {
                    hitChargingPart = true;
                }
                double mult = part.damageMult;
                if (effWeapon == CombatLoadout.WeaponOption.CANNON) mult *= buffs.cannonDamageMult();
                if (effWeapon == CombatLoadout.WeaponOption.AIRSTRIKE) mult *= buffs.airstrikeDamageMult();
                double aoeFalloff = switch (bossHitIdx) {
                    case 0 -> 1.00;
                    case 1 -> 0.72;
                    case 2 -> 0.52;
                    case 3 -> 0.38;
                    default -> 0.26;
                };
                double partFalloff = bossHitIdx == 0 ? 1.00 : (bossHitIdx <= 2 ? 0.78 : 0.48);
                bossHitIdx++;
                double hpCellDmg = baseCellDmg * mult * aoeFalloff;
                double partCellDmg = baseCellDmg * mult * partFalloff;
                totalRawDmg += hpCellDmg;
                totalPartRawDmg += partCellDmg;
                if (!isPartBroken(part)) {
                    partRawMap.merge(part, partCellDmg, Double::sum);
                }
                totalStagger += (TitanMode.weaponStaggerPower(effWeapon, effMagic) / (double) Math.max(1, targetList.size()))
                        * part.staggerMult * buffs.staggerPowerMult();
            } else {
                missCells.add(new int[]{r, colIdx});
                if (bossGrid[r][colIdx] == CellState.EMPTY) {
                    bossGrid[r][colIdx] = CellState.MISS;
                }
            }
        }

        boolean anyHit = !hitCells.isEmpty();
        boolean submergedRevealed = false;
        boolean submergedMitigated = false;
        List<int[]> sonarRevealed = new ArrayList<>();
        lastReactionLabel = "";

        if (effMagic == CombatLoadout.MagicOption.ARCANE_SONAR && (anyHit || !missCells.isEmpty())) {
            sonarRevealed.addAll(TitanMode.generateWeakpointCells(boss, bossOffR, bossOffC));
            if (submergedMs > 0) {
                submergedMs = 0L;
                submergedRevealed = true;
            }
            exposedMs = Math.max(exposedMs, 4200L);
            if (removeHazardsOfType(TitanMode.HazardType.SHOCK, 2) > 0) {
                lastReactionLabel = "GIẢI TĨNH ĐIỆN!";
                reactionsCount++;
            }
        }

        int finalDamage = 0;
        TitanMode.TitanPart brokenNow = null;
        boolean staggerTriggered = false;
        int staggerAdded = 0;
        int[] burnCell = null;
        double bonusStaggerFromReaction = 0.0;

        if (anyHit && bossHp > 0) {
            double carapaceMult = switch (difficulty) {
                case CATACLYSM -> 0.78;
                case NIGHTMARE -> 0.84;
                case NORMAL -> 0.90;
            };
            totalRawDmg *= carapaceMult;
            if (enraged && staggeredMs <= 0 && exposedMs <= 0) {
                totalRawDmg *= 0.82;
            }
            if (superShipActive) totalRawDmg *= 1.18;
            if (exposedMs > 0) totalRawDmg *= 1.22;
            if (staggeredMs > 0) totalRawDmg *= 1.20;
            if (fireCatalystMs > 0 && effMagic == CombatLoadout.MagicOption.FIRE) totalRawDmg *= 1.15;
            if (interceptDamageMult > 0.0 && interceptDamageMult < 1.0) {
                totalRawDmg *= interceptDamageMult;
            }
            if (submergedMs > 0 && !(effWeapon == CombatLoadout.WeaponOption.TORPEDO && buffs.ignoreSubmergedPenalty())) {
                totalRawDmg *= 0.35;
                submergedMitigated = true;
            }

            // Two-Way Elemental Chemistry Reactions
            double catalystMult = activeRelics.contains(TitanMode.RaidRelic.CATALYST) ? 1.30 : 1.0;
            boolean consumedBurn = false;
            boolean consumedChill = false;
            if (effMagic == CombatLoadout.MagicOption.FROST && burnMs > 0 && burnStacks > 0) {
                int stacks = burnStacks;
                int bonus = (int) Math.round(7 * stacks * catalystMult);
                totalRawDmg += bonus;
                bonusStaggerFromReaction += 14.0;
                burnStacks = 0;
                burnMs = 0L;
                consumedBurn = true;
                removeHazardsOfType(TitanMode.HazardType.BURN, 2);
                lastReactionLabel = "TAN CHẢY +" + bonus + "!";
                reactionsCount++;
            } else if (effMagic == CombatLoadout.MagicOption.FIRE && chillMs > 0) {
                chillMs = 0L;
                consumedChill = true;
                int bonus = (int) Math.round(10.0 * catalystMult);
                totalRawDmg += bonus;
                bonusStaggerFromReaction += 12.0;
                exposedMs = Math.max(exposedMs, 2800L);
                removeHazardsOfType(TitanMode.HazardType.SILK_SLOW, 2);
                lastReactionLabel = "SỐC NHIỆT +" + bonus + "!";
                reactionsCount++;
            } else if (effMagic == CombatLoadout.MagicOption.LIGHTNING && (chillMs > 0 || exposedMs > 0)) {
                int bonus = (int) Math.round(9.0 * catalystMult);
                totalRawDmg += bonus;
                bonusStaggerFromReaction += 16.0;
                removeHazardsOfType(TitanMode.HazardType.SHOCK, 1);
                lastReactionLabel = "SIÊU DẪN +" + bonus + "!";
                reactionsCount++;
            } else if (effMagic == CombatLoadout.MagicOption.FIRE) {
                if (removeHazardsOfType(TitanMode.HazardType.SILK_SLOW, 2) > 0 && lastReactionLabel.isEmpty()) {
                    lastReactionLabel = "ĐỐT TƠ BẪY!";
                    reactionsCount++;
                }
            } else if (effMagic == CombatLoadout.MagicOption.FROST) {
                if (removeHazardsOfType(TitanMode.HazardType.BURN, 2) > 0 && lastReactionLabel.isEmpty()) {
                    lastReactionLabel = "DẬP VÙNG CHÁY!";
                    reactionsCount++;
                }
            }

            if (effMagic == CombatLoadout.MagicOption.FIRE && !consumedChill) {
                burnStacks = Math.min(3, burnStacks + 1);
                burnMs = Math.max(burnMs, 6000L);
                burnCell = hitCells.get(0);
            }
            if (effMagic == CombatLoadout.MagicOption.FROST && !consumedBurn) {
                chillMs = Math.max(chillMs, 4000L);
            }

            double hpBeforeMods = Math.max(1.0, totalPartRawDmg);
            int rawHpDmg = Math.max(1, (int) Math.round(totalRawDmg));

            // Distribute part HP damage across all intact anatomical parts hit by this strike
            double tectonicMult = activeRelics.contains(TitanMode.RaidRelic.TECTONIC) ? 1.25 : 1.0;
            if (!partRawMap.isEmpty()) {
                double intactRawSum = 0.0;
                for (double v : partRawMap.values()) intactRawSum += v;
                double partScale = (hpBeforeMods / Math.max(1.0, intactRawSum)) * tectonicMult;
                for (TitanMode.TitanPart candidate : TitanMode.TitanPart.values()) {
                    Double rawP = partRawMap.get(candidate);
                    if (rawP == null || rawP <= 0.0 || isPartBroken(candidate)) continue;
                    int partDmg = Math.max(1, (int) Math.round(rawP * Math.min(partScale, 1.35 * tectonicMult)));
                    int breakBonus = applyDamageToPart(candidate, partDmg);
                    if (breakBonus > 0) {
                        rawHpDmg += breakBonus;
                        if (brokenNow == null) brokenNow = candidate;
                    }
                }
            }

            finalDamage = absorbEnrageBarrier(rawHpDmg, effWeapon == CombatLoadout.WeaponOption.CANNON);

            staggerAdded = (int) Math.round((totalStagger * (hitChargingPart ? 1.38 : 0.85)
                    + (effMagic == CombatLoadout.MagicOption.LIGHTNING ? 11 : 0)
                    + bonusStaggerFromReaction) * tectonicMult);
            if (staggeredMs <= 0) {
                staggerGauge += staggerAdded;
                if (brokenNow == TitanMode.TitanPart.HEAD || brokenNow == TitanMode.TitanPart.CORE_FIN
                        || staggerGauge >= staggerThreshold) {
                    staggerTriggered = true;
                    triggerStaggerInterrupt();
                }
            }

            totalDamageDealt += rawHpDmg;
            bossHp = Math.max(0, bossHp - finalDamage);

            if (!weakHits.isEmpty()) {
                tryCompleteDirective(TitanMode.DirectiveType.WEAKPOINT_STRIKE, row, col);
            }
            if (hitChargingPart || staggerTriggered) {
                tryCompleteDirective(TitanMode.DirectiveType.STAGGER_WINDOW, row, col);
            }
            if (!lastReactionLabel.isEmpty()) {
                tryCompleteDirective(TitanMode.DirectiveType.ELEMENTAL_COMBO, row, col);
            }
        }

        boolean wasEnraged = enraged;
        checkPhaseThresholds();

        lastStrikeInfo = new RaidStrikeInfo(
                finalDamage, bestPart, brokenNow, staggerTriggered,
                submergedRevealed, submergedMitigated, staggerAdded);

        boolean defeated = bossHp <= 0;
        ShotResult agg = !anyHit ? ShotResult.MISS : (defeated ? ShotResult.SUNK : ShotResult.HIT);
        return new PlayerStrikeOutcome(
                agg, hitCells, missCells, weakHits, burnCell, sonarRevealed,
                bossHp, !wasEnraged && enraged, defeated);
    }

    /** Hyperion Nova 3x3 heavy ultimate dealing burst damage and forcing immediate Stagger Interrupt! */
    public PlayerStrikeOutcome fireHyperionUltimate(int row, int col, List<int[]> cells) {
        List<int[]> area = (cells == null || cells.isEmpty()) ? List.of(new int[]{row, col}) : cells;
        return resolveBurstAreaStrike(area, 28, 42, 8, 3, null);
    }

    /** Executes the 2-Player "Song Long Hợp Kích" fusion burst across a 3x3 convergence zone. */
    public PlayerStrikeOutcome executeCoopSynergyBlast(int centerRow, int centerCol) {
        List<int[]> area = new ArrayList<>(9);
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = centerRow + dr;
                int nc = centerCol + dc;
                if (SandboxController.inBounds(nr, nc)) {
                    area.add(new int[]{nr, nc});
                }
            }
        }
        return resolveBurstAreaStrike(area, 24, 44, 9, 4, "SONG LONG HỢP KÍCH!");
    }

    private PlayerStrikeOutcome resolveBurstAreaStrike(
            List<int[]> area,
            int missFloorDmg,
            int minHitDmg,
            int perHitDmg,
            int perWeakDmg,
            String reactionTag
    ) {
        List<int[]> hitCells = new ArrayList<>();
        List<int[]> missCells = new ArrayList<>();
        List<int[]> weakHits = new ArrayList<>();
        EnumMap<TitanMode.TitanPart, Double> partHitWeights = new EnumMap<>(TitanMode.TitanPart.class);
        lastNewlyBrokenParts.clear();

        for (int[] c : area) {
            if (c == null || c.length < 2 || !SandboxController.inBounds(c[0], c[1])) continue;
            TitanMode.TitanPart part = TitanMode.partAtCell(boss, c[0], c[1], bossOffR, bossOffC);
            if (part != null) {
                hitCells.add(new int[]{c[0], c[1]});
                if (part != TitanMode.TitanPart.BODY_TAIL) weakHits.add(new int[]{c[0], c[1]});
                if (!isPartBroken(part)) {
                    partHitWeights.merge(part, 1.0, Double::sum);
                }
            } else {
                missCells.add(new int[]{c[0], c[1]});
            }
        }
        int dmg = hitCells.isEmpty()
                ? missFloorDmg
                : Math.max(minHitDmg, hitCells.size() * perHitDmg + weakHits.size() * perWeakDmg);
        int hpDmg = absorbEnrageBarrier(dmg, true);
        boolean submergedRevealed = submergedMs > 0;
        submergedMs = 0L;
        exposedMs = Math.max(exposedMs, 3800L);
        if (reactionTag != null && !reactionTag.isBlank()) {
            lastReactionLabel = reactionTag;
            reactionsCount++;
        }
        triggerStaggerInterrupt();

        TitanMode.TitanPart brokenNow = null;
        if (!partHitWeights.isEmpty()) {
            double totalW = 0.0;
            for (double w : partHitWeights.values()) totalW += w;
            for (TitanMode.TitanPart p : TitanMode.TitanPart.values()) {
                Double w = partHitWeights.get(p);
                if (w == null || w <= 0.0) continue;
                int bonus = applyDamageToPart(p, Math.max(1, (int) Math.round(dmg * (w / Math.max(1.0, totalW)))));
                if (bonus > 0) {
                    hpDmg += bonus;
                    if (brokenNow == null) brokenNow = p;
                }
            }
        }

        totalDamageDealt += dmg;
        bossHp = Math.max(0, bossHp - hpDmg);
        boolean wasEnraged = enraged;
        checkPhaseThresholds();
        lastStrikeInfo = new RaidStrikeInfo(
                dmg, brokenNow != null ? brokenNow : TitanMode.TitanPart.CORE_FIN,
                brokenNow, true, submergedRevealed, false, staggerThreshold);
        boolean defeated = bossHp <= 0;
        ShotResult agg = defeated ? ShotResult.SUNK : (hitCells.isEmpty() ? ShotResult.MISS : ShotResult.HIT);
        return new PlayerStrikeOutcome(
                agg, hitCells, missCells, weakHits, null,
                TitanMode.generateWeakpointCells(boss, bossOffR, bossOffC),
                bossHp, !wasEnraged && enraged, defeated);
    }

    private int computeEnrageBarrierMax() {
        return Math.max(40, (int) Math.round(
                (56 + difficulty.ordinal() * 24) * (boss == TitanMode.TitanBoss.GHIDORAH ? 1.12 : 1.0)));
    }

    private int computeEffectiveStaggerThreshold(boolean p2) {
        double progressive = 1.0 + Math.min(0.60, staggersCount * 0.14);
        return Math.max(20, (int) Math.round(
                TitanMode.staggerThresholdFor(boss, p2) * difficulty.staggerThresholdMult * progressive));
    }

    /** Cuồng bạo ở 50% máu; Godzilla Ultima còn tiến hóa thêm ở 75% và 25%. */
    private void checkPhaseThresholds() {
        if (!enraged && bossHp > 0 && bossHp <= (bossMaxHp / 2)) {
            activatePhase2Enrage();
        }
        if (boss != TitanMode.TitanBoss.ULTIMA || bossHp <= 0) return;
        int stage = TitanMode.ultimaStageFor(bossHp, bossMaxHp);
        if (stage > ultimaStage) evolveUltima(stage);
        if (!ultimaStarOpen && TitanMode.ultimaStarOpenFor(bossHp, bossMaxHp)) {
            ultimaStarOpen = true;
            phase2DebutPending = true;      // tung ngay 2 chiêu ★ của form như Godzilla vào pha cuồng bạo
            skillBag.clear();
            pendingArenaEvents.add(new ArenaFeedbackEvent(ArenaEventKind.ULTIMA_RAGE, 4, 4,
                    "★ CUỒNG NỘ · " + TitanMode.ultimaStageName(ultimaStage).toUpperCase() + " MỞ 2 CHIÊU ★!",
                    0, 0, 0, 0, null, null));
        }
    }

    private void evolveUltima(int stage) {
        ultimaStage = stage;
        ultimaStarOpen = false;
        regrowParts();
        TitanMode.setUltimaStage(stage);
        skillBag.clear();
        // Đổi form thì huỷ chiêu đang vận của form cũ (giống bị ngắt chiêu)
        pendingSkill = null;
        telegraphCells = List.of();
        castElapsedMs = 0L;
        castTotalMs = 0L;
        recoveryRemainingMs = Math.max(recoveryRemainingMs, 1650L);
        pendingArenaEvents.add(new ArenaFeedbackEvent(ArenaEventKind.ULTIMA_EVOLVED, 4, 4,
                "TIẾN HÓA · GODZILLA ULTIMA " + TitanMode.ultimaStageName(stage).toUpperCase() + "!",
                0, 0, 0, 0, null, null));
    }

    public int getUltimaStage() { return ultimaStage; }

    /** Tiến hóa = cơ thể mới: bộ phận đã phá mọc lại đủ, sprite dạng mới không bị khuyết. */
    private void regrowParts() {
        for (TitanMode.TitanPart part : TitanMode.TitanPart.values()) {
            partHp.put(part, getPartMaxHp(part));
            partBroken.put(part, false);
        }
    }

    /** Chiêu ★ đã dùng được chưa: Ultima theo từng form, boss khác theo mốc cuồng bạo 50%. */
    public boolean isStarSkillsOpen() { return boss == TitanMode.TitanBoss.ULTIMA ? ultimaStarOpen : enraged; }

    private void activatePhase2Enrage() {
        enraged = true;
        if (boss == TitanMode.TitanBoss.SPACEGODZILLA || boss == TitanMode.TitanBoss.DESTOROYAH) {
            regrowParts();      // tiến hóa thành Crystal SpaceGodzilla / Perfect Destoroyah: thân mới nguyên vẹn
        }
        enrageBarrierMaxHp = computeEnrageBarrierMax();
        enrageBarrierHp = enrageBarrierMaxHp;
        antiMissileCooldownMs = 0L;
        staggerThreshold = computeEffectiveStaggerThreshold(true);
        phase2DebutPending = true;
        skillBag.clear();
        recoveryRemainingMs = Math.max(recoveryRemainingMs, 1650L);
    }

    private int absorbEnrageBarrier(int rawHpDamage, boolean bonusBarrierDamage) {
        if (rawHpDamage <= 0 || enrageBarrierHp <= 0) return rawHpDamage;
        int barrierHit = Math.max(1, (int) Math.round(rawHpDamage * (bonusBarrierDamage ? 1.35 : 0.78)));
        if (enrageBarrierHp > barrierHit) {
            enrageBarrierHp -= barrierHit;
            return Math.max(1, (int) Math.round(rawHpDamage * 0.22));
        }
        int absorbed = enrageBarrierHp;
        enrageBarrierHp = 0;
        exposedMs = Math.max(exposedMs, 2600L);
        if (staggeredMs <= 0) {
            staggerGauge = Math.min(staggerThreshold, staggerGauge + 18);
        }
        if (lastReactionLabel.isEmpty()) {
            lastReactionLabel = "VỠ GIÁP CUỒNG BẠO!";
        }
        return Math.max(1, rawHpDamage - absorbed);
    }

    private int applyDamageToPart(TitanMode.TitanPart part, int dmg) {
        if (part == null || dmg <= 0 || isPartBroken(part)) return 0;
        int curPart = partHp.getOrDefault(part, 0);
        int nextPart = Math.max(0, curPart - dmg);
        partHp.put(part, nextPart);
        if (nextPart > 0) return 0;
        partBroken.put(part, true);
        partsBrokenCount++;
        lastNewlyBrokenParts.add(part);
        if (part == TitanMode.TitanPart.BODY_TAIL) {
            submergedMs = 0L;
        }
        return switch (part) {
            case HEAD -> 14;
            case CORE_FIN -> 16;
            case BODY_TAIL -> 12;
        };
    }

    private void triggerStaggerInterrupt() {
        staggersCount++;
        boolean wasDpsCheck = dpsCheckActive;
        if (wasDpsCheck) {
            dpsCheckActive = false;
            dpsCheckDone = true;
            dpsCheckInterrupted = true;
            staggeredMs = 3000L;
            bossHp = Math.max(0, bossHp - 18);
            totalDamageDealt += 18;
        } else {
            staggeredMs = 2200L;
        }
        staggerGauge = 0;
        staggerDecayAccumMs = 0L;
        staggerThreshold = computeEffectiveStaggerThreshold(enraged);
        pendingSkill = null;
        telegraphCells = List.of();
        castElapsedMs = 0L;
        castTotalMs = 0L;
        recoveryRemainingMs = Math.max(recoveryRemainingMs, 1150L);
    }

    /** Advances the Hybrid Real-Time Raid clock: cast bars, stealth/reposition, DoTs, hazard ticks, move CD. */
    public RaidTickResult stepRaidSimulation(long dtMs, boolean allowBossClock) {
        if (dtMs <= 0 || bossHp <= 0) {
            return new RaidTickResult(null, telegraphRow, telegraphCol, telegraphCells,
                    0, 0, List.of(), false, false, false, false, null);
        }

        if (moveCooldownMs > 0) moveCooldownMs = Math.max(0L, moveCooldownMs - dtMs);
        if (allowBossClock) elapsedRaidMs += dtMs;

        // 1. Tick hazard floor durations & player fleet hazard DoT every 1400ms
        int hazardHits = 0;
        List<int[]> hazardCells = new ArrayList<>();
        for (int i = activeHazards.size() - 1; i >= 0; i--) {
            HazardTile h = activeHazards.get(i);
            long rem = h.remainingMs() - dtMs;
            if (rem <= 0) activeHazards.remove(i);
            else activeHazards.set(i, new HazardTile(h.row(), h.col(), h.type(), rem));
        }
        if (allowBossClock && !activeHazards.isEmpty()) {
            hazardTickAccumMs += dtMs;
            if (hazardTickAccumMs >= 1050L) {
                hazardTickAccumMs -= 1050L;
                for (HazardTile h : activeHazards) {
                    if (h.type() == TitanMode.HazardType.SILK_SLOW) continue;
                    if (playerGrid[h.row()][h.col()] == CellState.SHIP) {
                        if (consumePlayerArmorCell(h.row(), h.col())) {
                            hazardCells.add(new int[]{h.row(), h.col()});
                            continue;
                        }
                        if (rng.nextDouble() < 0.68) {
                            playerGrid[h.row()][h.col()] = CellState.HIT;
                            hazardHits++;
                            hazardCells.add(new int[]{h.row(), h.col()});
                        }
                    }
                }
            }
        }

        if (!allowBossClock) {
            return new RaidTickResult(null, telegraphRow, telegraphCol, telegraphCells,
                    0, hazardHits, hazardCells, false, false, false, false, null);
        }

        // 2. Boss Status Timers + Burn DoT + Anti-Missile Counter Cooldown
        if (chillMs > 0) chillMs = Math.max(0L, chillMs - dtMs);
        if (exposedMs > 0) exposedMs = Math.max(0L, exposedMs - dtMs);
        if (antiMissileCooldownMs > 0 && staggeredMs <= 0 && submergedMs <= 0) {
            antiMissileCooldownMs = Math.max(0L, antiMissileCooldownMs - dtMs);
        }

        int burnDmg = 0;
        if (burnMs > 0 && burnStacks > 0) {
            burnMs = Math.max(0L, burnMs - dtMs);
            burnTickAccumMs += dtMs;
            if (burnTickAccumMs >= 1000L) {
                burnTickAccumMs -= 1000L;
                burnDmg = 2 * burnStacks;
                totalDamageDealt += burnDmg;
                bossHp = Math.max(0, bossHp - burnDmg);
            }
            if (burnMs == 0) burnStacks = 0;
        }
        if (bossHp <= 0) {
            return new RaidTickResult(null, telegraphRow, telegraphCol, telegraphCells,
                    burnDmg, hazardHits, hazardCells, false, false, false, true, null);
        }

        // 3. Submerge / Reposition lifecycle (BODY_TAIL break disables submerging!)
        boolean bossMoved = false;
        stepArenaSystems(dtMs);
        if (burnDmg > 0 && (activeRelics.contains(TitanMode.RaidRelic.CATALYST) || fireCatalystMs > 0L) && bossHp > 0) {
            int bonusBurn = Math.max(1, (int) Math.round(burnDmg * 0.35));
            burnDmg += bonusBurn;
            totalDamageDealt += bonusBurn;
            bossHp = Math.max(0, bossHp - bonusBurn);
        }

        boolean submergedNow = false;
        boolean surfacedNow = false;

        if (submergedMs > 0) {
            submergedMs = Math.max(0L, submergedMs - dtMs);
            if (submergedMs == 0L) surfacedNow = true;
        } else if (!isPartBroken(TitanMode.TitanPart.BODY_TAIL)) {
            submergedCooldownMs -= dtMs;
            if (submergedCooldownMs <= 0 && pendingSkill == null && staggeredMs <= 0) {
                submergedMs = 7500L;
                submergedCooldownMs = enraged ? 13_500L : 17_000L;
                submergedNow = true;
                bossMoved = repositionBoss();
            }
        }

        idlePositionMs += dtMs;
        long shiftPeriod = isPartBroken(TitanMode.TitanPart.BODY_TAIL) ? 11_500L : 7_200L;
        if (idlePositionMs >= shiftPeriod && pendingSkill == null && staggeredMs <= 0) {
            idlePositionMs = 0L;
            if (repositionBoss()) bossMoved = true;
        }

        // 4. Stagger Recovery / Decay / ATB Cast Bar Progression
        if (staggeredMs > 0) {
            staggeredMs = Math.max(0L, staggeredMs - dtMs);
            return new RaidTickResult(null, telegraphRow, telegraphCol, telegraphCells,
                    burnDmg, hazardHits, hazardCells, bossMoved, submergedNow, surfacedNow, bossHp <= 0, null);
        }
        if (pendingSkill == null && staggerGauge > 0) {
            staggerDecayAccumMs += dtMs;
            if (staggerDecayAccumMs >= 1000L) {
                int steps = (int) (staggerDecayAccumMs / 1000L);
                staggerDecayAccumMs -= steps * 1000L;
                staggerGauge = Math.max(0, staggerGauge - steps * 4);
            }
        } else {
            staggerDecayAccumMs = 0L;
        }

        double speedMult = (chillMs > 0 ? 0.65 : 1.0) * difficulty.atbSpeedMult * (enraged ? 1.25 : 1.0);
        if (isPartBroken(TitanMode.TitanPart.CORE_FIN)) speedMult *= 0.80;
        long effectiveDt = Math.max(1L, Math.round(dtMs * speedMult));

        TitanMode.TitanSkill readySkill = null;
        int readyRow = telegraphRow;
        int readyCol = telegraphCol;
        List<int[]> readyCells = telegraphCells;

        if (pendingSkill == null) {
            recoveryRemainingMs -= effectiveDt;
            if (recoveryRemainingMs <= 0) {
                beginNextBossCast();
            }
        } else {
            castElapsedMs += effectiveDt;
            if (castElapsedMs >= castTotalMs) {
                readySkill = pendingSkill;
                readyRow = telegraphRow;
                readyCol = telegraphCol;
                readyCells = new ArrayList<>(telegraphCells);
                lastCastWasDpsCheck = dpsCheckActive;
                if (dpsCheckActive) {
                    dpsCheckDone = true;
                    dpsCheckActive = false;
                }
                pendingSkill = null;
                telegraphCells = List.of();
                castElapsedMs = 0L;
                castTotalMs = 0L;
                recoveryRemainingMs = enraged ? 2250L : 2950L;
                skillsSinceMove++;
                if (skillsSinceMove >= 2) {
                    skillsSinceMove = 0;
                    if (repositionBoss()) bossMoved = true;
                }
            }
        }

        return new RaidTickResult(
                readySkill, readyRow, readyCol, readyCells,
                burnDmg, hazardHits, hazardCells,
                bossMoved, submergedNow, surfacedNow, bossHp <= 0, null);
    }

    private void beginNextBossCast() {
        lastCastWasDpsCheck = false;
        bossCastSeq++;
        boolean headBroken = isPartBroken(TitanMode.TitanPart.HEAD);

        if (isStarSkillsOpen() && !dpsCheckDone && bossHp > 0 && bossHp <= (int) Math.round(bossMaxHp * 0.34)) {
            dpsCheckActive = true;
            pendingSkill = TitanMode.cataclysmSkillFor(boss);
            int[] target = pickPlayerTargetNearFleet(pendingSkill);
            telegraphRow = target != null ? target[0] : 4;
            telegraphCol = target != null ? target[1] : 4;
            recordRecentBossTarget(telegraphRow, telegraphCol);
            List<int[]> cells = TitanMode.cataclysmTargetCells(telegraphRow, telegraphCol);
            telegraphCells = headBroken ? trimCellsForHeadBreak(cells) : cells;
            castElapsedMs = 0L;
            castTotalMs = Math.round(6200L * (headBroken ? 1.30 : 1.0));
            return;
        }

        dpsCheckActive = false;
        pendingSkill = pickBossSkill();
        int[] target = pickPlayerTargetNearFleet(pendingSkill);
        telegraphRow = target != null ? target[0] : 4;
        telegraphCol = target != null ? target[1] : 4;
        recordRecentBossTarget(telegraphRow, telegraphCol);
        boolean horizontal = pickSkillHorizontalOrientation(pendingSkill, telegraphRow, telegraphCol);
        List<int[]> cells = TitanMode.skillTargetCells(pendingSkill, telegraphRow, telegraphCol, horizontal);
        telegraphCells = headBroken ? trimCellsForHeadBreak(cells) : cells;
        castElapsedMs = 0L;
        castTotalMs = Math.round(TitanMode.skillCastDurationMs(pendingSkill, enraged) * (headBroken ? 1.30 : 1.0));
    }

    private List<int[]> trimCellsForHeadBreak(List<int[]> raw) {
        if (raw == null || raw.size() < 6) return raw != null ? raw : List.of();
        List<int[]> trimmed = new ArrayList<>(raw);
        for (int k = 0; k < 2 && trimmed.size() > 4; k++) {
            trimmed.remove(trimmed.size() - 1);
        }
        return trimmed;
    }

    private int removeHazardsOfType(TitanMode.HazardType type, int maxRemove) {
        if (type == null || type == TitanMode.HazardType.NONE || maxRemove <= 0) return 0;
        int removed = 0;
        for (int i = activeHazards.size() - 1; i >= 0 && removed < maxRemove; i--) {
            if (activeHazards.get(i).type() == type) {
                activeHazards.remove(i);
                removed++;
            }
        }
        return removed;
    }

    /** Moves Boss silhouette to a new offset within [-1..+1] row, [-1..+2] col so all 20 cells stay on board. */
    public boolean repositionBoss() {
        int[][] candidates = {
                {0, 0}, {0, 1}, {-1, 0}, {1, 0}, {0, -1}, {-1, 1}, {1, 1}
        };
        List<int[]> valid = new ArrayList<>();
        for (int[] cand : candidates) {
            if (cand[0] == bossOffR && cand[1] == bossOffC) continue;
            if (isOffsetValidForBoss(cand[0], cand[1])) valid.add(cand);
        }
        if (valid.isEmpty()) return false;
        int[] chosen = valid.get(rng.nextInt(valid.size()));
        bossOffR = chosen[0];
        bossOffC = chosen[1];
        syncBossGridSilhouette();
        return true;
    }

    private boolean isOffsetValidForBoss(int dr, int dc) {
        for (int[] base : TitanMode.generateHitboxCells(boss, 0, 0)) {
            int nr = base[0] + dr;
            int nc = base[1] + dc;
            if (!SandboxController.inBounds(nr, nc)) return false;
        }
        return true;
    }

    /** Executes Boss Skill impact against current player positions (can miss if ships dodged out of telegraph!). */
    public TitanSkillOutcome castTitanSkill(
            TitanMode.TitanSkill forcedSkill,
            Collection<int[]> playerShieldCells
    ) {
        TitanMode.TitanSkill skill = forcedSkill != null ? forcedSkill : (pendingSkill != null ? pendingSkill : pickBossSkill());
        int targetRow;
        int targetCol;
        List<int[]> cells;
        if (forcedSkill == null && !telegraphCells.isEmpty()) {
            targetRow = telegraphRow;
            targetCol = telegraphCol;
            cells = telegraphCells;
        } else {
            bossCastSeq++;
            int[] target = pickPlayerTargetNearFleet(skill);
            targetRow = target != null ? target[0] : 4;
            targetCol = target != null ? target[1] : 4;
            recordRecentBossTarget(targetRow, targetCol);
            boolean horiz = pickSkillHorizontalOrientation(skill, targetRow, targetCol);
            cells = TitanMode.skillTargetCells(skill, targetRow, targetCol, horiz);
        }
        return resolveSkillImpactAt(skill, targetRow, targetCol, cells, playerShieldCells);
    }

    public TitanSkillOutcome resolveSkillImpactAt(
            TitanMode.TitanSkill skill,
            int targetRow,
            int targetCol,
            List<int[]> cells,
            Collection<int[]> playerShieldCells
    ) {
        List<Ship> intactBefore = intactPlayerShips();
        List<int[]> hitCells = new ArrayList<>();
        List<int[]> missCells = new ArrayList<>();
        List<int[]> shieldBlocked = new ArrayList<>();
        lastArmorAbsorbedCells.clear();
        lastReefBlockedCells.clear();
        Set<Integer> neighborReefsUsed = new HashSet<>();

        for (int[] c : cells) {
            int r = c[0];
            int col = c[1];
            if (!SandboxController.inBounds(r, col)) continue;
            if (containsCell(playerShieldCells, r, col)) {
                shieldBlocked.add(new int[]{r, col});
                continue;
            }
            if (consumePlayerArmorCell(r, col)) {
                lastArmorAbsorbedCells.add(new int[]{r, col});
                continue;
            }
            CellState prev = playerGrid[r][col];
            if (absorbHitWithCoverReef(r, col, prev == CellState.SHIP, neighborReefsUsed)) {
                lastReefBlockedCells.add(new int[]{r, col});
                continue;
            }
            for (int pi = 0; pi < arenaProps.size(); pi++) {
                ArenaProp ap = arenaProps.get(pi);
                if (ap.kind() == ArenaPropKind.SONAR_BUOY && ap.row() == r && ap.col() == col && ap.cooldownMs() <= 0L) {
                    arenaProps.set(pi, new ArenaProp(ap.id(), ap.row(), ap.col(), ap.kind(),
                            1, 1, 0L, 8000L, true));
                    List<int[]> weak = TitanMode.generateWeakpointCells(boss, bossOffR, bossOffC);
                    List<int[]> scan = weak.subList(0, Math.min(3, weak.size()));
                    pendingArenaEvents.add(new ArenaFeedbackEvent(
                            ArenaEventKind.SONAR_PULSE, r, col, "PHAO SONAR QUÉT!",
                            1, 6, 0, 0, new ArrayList<>(scan), null));
                }
            }
            if (prev == CellState.SHIP) {
                playerGrid[r][col] = CellState.HIT;
                hitCells.add(new int[]{r, col});
            } else if (prev == CellState.EMPTY) {
                playerGrid[r][col] = CellState.MISS;
                missCells.add(new int[]{r, col});
            }
        }
        if (hitCells.isEmpty()) {
            tryCompleteDirective(TitanMode.DirectiveType.DODGE_DANCE, targetRow, targetCol);
        }

        // Spawn persistent Hazard Tiles if skill leaves a zone and not fully blocked by shield
        TitanMode.HazardType hz = TitanMode.hazardTypeForSkill(skill);
        if (hz != TitanMode.HazardType.NONE && (shieldBlocked.isEmpty() || !hitCells.isEmpty())) {
            boolean coreBroken = isPartBroken(TitanMode.TitanPart.CORE_FIN);
            long dur = coreBroken ? 4000L : 8000L;
            int cap = coreBroken ? 4 : cells.size();
            int added = 0;
            for (int[] c : cells) {
                if (added >= cap) break;
                if (SandboxController.inBounds(c[0], c[1])) {
                    addOrRefreshHazard(c[0], c[1], hz, dur);
                    added++;
                }
            }
        }

        List<ShipType> newlySunk = new ArrayList<>();
        for (Ship s : intactBefore) {
            if (SandboxController.isShipSunk(playerGrid, s)) newlySunk.add(s.type);
        }
        if (lastCastWasDpsCheck && bossHp > 0) {
            bossHp = Math.min(bossMaxHp, bossHp + 30);
            int addedShield = 24;
            enrageBarrierMaxHp = Math.max(enrageBarrierMaxHp, enrageBarrierHp + addedShield);
            enrageBarrierHp = Math.min(enrageBarrierMaxHp, enrageBarrierHp + addedShield);
        }
        boolean playerLost = allPlayerShipsSunk();
        return new TitanSkillOutcome(
                skill, targetRow, targetCol, cells,
                hitCells, missCells, shieldBlocked, newlySunk, enraged, playerLost);
    }

    private void addOrRefreshHazard(int r, int c, TitanMode.HazardType type, long durationMs) {
        activeHazards.removeIf(h -> h.row() == r && h.col() == c);
        if (activeHazards.size() >= 18) activeHazards.remove(0);
        activeHazards.add(new HazardTile(r, c, type, durationMs));
    }

    public boolean isShipSlowedBySilk(Ship ship) {
        if (ship == null) return false;
        for (int i = 0; i < ship.type.length; i++) {
            int r = ship.row + (ship.horizontal ? 0 : i);
            int c = ship.col + (ship.horizontal ? i : 0);
            for (HazardTile h : activeHazards) {
                if (h.type() == TitanMode.HazardType.SILK_SLOW && h.row() == r && h.col() == c) return true;
            }
        }
        return false;
    }

    /** Moves/rotates an alive player ship while preserving existing HIT segments and Golden Super Armor. */
    public boolean movePlayerShip(ShipType type, int newRow, int newCol, boolean newHoriz, boolean ignoreCooldown, boolean superShipActive) {
        if (type == null || (!ignoreCooldown && moveCooldownMs > 0)) return false;
        int idx = -1;
        for (int i = 0; i < playerShips.size(); i++) {
            if (playerShips.get(i).type == type) {
                idx = i;
                break;
            }
        }
        if (idx < 0) return false;
        Ship old = playerShips.get(idx);
        if (SandboxController.isShipSunk(playerGrid, old)) return false;

        int len = old.type.length;
        // Validate new bounds and ensure no overlap with any OTHER ship in playerShips or solid Cover Reefs
        for (int i = 0; i < len; i++) {
            int nr = newRow + (newHoriz ? 0 : i);
            int nc = newCol + (newHoriz ? i : 0);
            if (!SandboxController.inBounds(nr, nc)) return false;
            if (hasBlockingCoverReef(nr, nc)) return false;
            Ship occupant = SandboxController.findShipAt(playerShips, nr, nc);
            if (occupant != null && occupant != old) return false;
        }
        if (old.row == newRow && old.col == newCol && old.horizontal == newHoriz) return false;

        boolean[] segDamaged = new boolean[len];
        int[] segArmor = new int[len];
        for (int i = 0; i < len; i++) {
            int or = old.row + (old.horizontal ? 0 : i);
            int oc = old.col + (old.horizontal ? i : 0);
            if (SandboxController.inBounds(or, oc)) {
                segDamaged[i] = (playerGrid[or][oc] == CellState.HIT);
                segArmor[i] = superArmorGrid[or][oc];
                playerGrid[or][oc] = CellState.EMPTY;
                superArmorGrid[or][oc] = 0;
            }
        }

        Ship updated = new Ship(old.type, newRow, newCol, newHoriz);
        playerShips.set(idx, updated);
        for (int i = 0; i < len; i++) {
            int nr = updated.row + (updated.horizontal ? 0 : i);
            int nc = updated.col + (updated.horizontal ? i : 0);
            playerGrid[nr][nc] = segDamaged[i] ? CellState.HIT : CellState.SHIP;
            superArmorGrid[nr][nc] = segArmor[i];
        }

        if (!ignoreCooldown) {
            FleetBuffSummary buffs = computeFleetBuffs(superShipActive);
            long baseCd = isShipSlowedBySilk(updated) ? 2350L : 1450L;
            double gravMult = (arenaMechanicsEnabled && stageTheme == OceanMapBackdrop.MapTheme.GALAXY_COSMOS) ? 0.65 : 1.0;
            moveCooldownMs = Math.round(baseCd * buffs.dodgeCooldownMult() * gravMult);
        }
        triggerPassThroughPropsForShip(updated, ignoreCooldown);
        return true;
    }

    /** Moves a ship by (dr, dc), attempting a 2-cell low-gravity dash in Galaxy Cosmos with automatic 1-cell fallback. */
    public boolean stepShipMoveWithDash(ShipType type, int dr, int dc, boolean superShipActive) {
        if (type == null || (dr == 0 && dc == 0)) return false;
        Ship cur = null;
        for (Ship s : playerShips) {
            if (s.type == type) {
                cur = s;
                break;
            }
        }
        if (cur == null) return false;
        int maxStep = getKeyboardMoveStepCells();
        for (int step = maxStep; step >= 1; step--) {
            if (movePlayerShip(type, cur.row + dr * step, cur.col + dc * step, cur.horizontal, false, superShipActive)) {
                return true;
            }
        }
        return false;
    }

    /** Checks whether moving/rotating a ship to (newRow, newCol, newHoriz) is geometrically legal. */
    public boolean canPlaceRelocatedShip(ShipType type, int newRow, int newCol, boolean newHoriz) {
        if (type == null) return false;
        Ship target = null;
        for (Ship s : playerShips) {
            if (s.type == type) {
                target = s;
                break;
            }
        }
        if (target == null || SandboxController.isShipSunk(playerGrid, target)) return false;
        for (int i = 0; i < type.length; i++) {
            int nr = newRow + (newHoriz ? 0 : i);
            int nc = newCol + (newHoriz ? i : 0);
            if (!SandboxController.inBounds(nr, nc)) return false;
            if (hasBlockingCoverReef(nr, nc)) return false;
            Ship occ = SandboxController.findShipAt(playerShips, nr, nc);
            if (occ != null && occ != target) return false;
        }
        return true;
    }

    /** Environmental gale drift: pushes 1-2 intact fleet vessels by Wind Vector (dr, dc) when legal. */
    public List<ShipType> applyWindDrift(int dr, int dc) {
        if (dr == 0 && dc == 0) return List.of();
        List<ShipType> shifted = new ArrayList<>();
        List<Ship> candidates = new ArrayList<>(intactPlayerShips());
        java.util.Collections.shuffle(candidates, rng);
        for (Ship s : candidates) {
            if (shifted.size() >= 2) break;
            if (isShipSlowedBySilk(s)) continue;
            if (movePlayerShip(s.type, s.row + dr, s.col + dc, s.horizontal, true, false)) {
                shifted.add(s.type);
            }
        }
        return shifted;
    }

    /** Rewards Timed Shield Parry by damaging Boss HP & adding Stagger directly. */
    public boolean applyShieldParryCounter(int bonusDamage, int bonusStagger) {
        if (bossHp <= 0) return false;
        parriesCount++;
        tryCompleteDirective(TitanMode.DirectiveType.DODGE_DANCE, 4, 4);
        int dealt = Math.max(1, bonusDamage);
        totalDamageDealt += dealt;
        bossHp = Math.max(0, bossHp - dealt);
        if (staggeredMs <= 0) {
            staggerGauge += Math.max(0, bonusStagger);
            if (staggerGauge >= staggerThreshold) {
                triggerStaggerInterrupt();
            }
        }
        checkPhaseThresholds();
        return true;
    }

    private Ship findOnlyAliveShip() {
        Ship last = null;
        int count = 0;
        for (Ship s : playerShips) {
            if (!SandboxController.isShipSunk(playerGrid, s)) {
                last = s;
                count++;
            }
        }
        return count == 1 ? last : null;
    }

    private boolean consumePlayerArmorCell(int row, int col) {
        if (!SandboxController.inBounds(row, col) || superArmorGrid[row][col] <= 0) return false;
        superArmorGrid[row][col]--;
        return true;
    }

    private TitanMode.TitanSkill pickBossSkill() {
        boolean starOpen = isStarSkillsOpen();
        if (skillBag.isEmpty() || skillBagEnraged != starOpen) {
            skillBag.clear();
            skillBagEnraged = starOpen;
            List<TitanMode.TitanSkill> p1 = new ArrayList<>();
            List<TitanMode.TitanSkill> p2 = new ArrayList<>();
            for (TitanMode.TitanSkill s : TitanMode.skillsFor(boss)) {
                if (s.ultimaStage != 0 && s.ultimaStage != ultimaStage) continue;
                if (!starOpen && s.phase2Only) continue;
                if (s.phase2Only) p2.add(s);
                else p1.add(s);
            }
            List<TitanMode.TitanSkill> pool = new ArrayList<>();
            if (starOpen && phase2DebutPending && !p2.isEmpty()) {
                // Debut each newly unlocked Phase-2 skill once on transition, then mix all skills freely.
                java.util.Collections.shuffle(p2, rng);
                java.util.Collections.shuffle(p1, rng);
                pool.addAll(p2);
                pool.addAll(p1);
                phase2DebutPending = false;
            } else {
                pool.addAll(p1);
                pool.addAll(p2);
                java.util.Collections.shuffle(pool, rng);
            }
            if (pool.size() > 1 && pool.get(0) == lastPickedSkill) {
                int swapIdx = 1 + rng.nextInt(pool.size() - 1);
                TitanMode.TitanSkill tmp = pool.get(0);
                pool.set(0, pool.get(swapIdx));
                pool.set(swapIdx, tmp);
            }
            skillBag.addAll(pool);
        }
        TitanMode.TitanSkill chosen = skillBag.remove(0);
        lastPickedSkill = chosen;
        return chosen;
    }

    private void recordRecentBossTarget(int row, int col) {
        recentBossTargets.add(new int[]{row, col});
        while (recentBossTargets.size() > RECENT_BOSS_TARGET_LIMIT) {
            recentBossTargets.remove(0);
        }
    }

    private boolean pickSkillHorizontalOrientation(TitanMode.TitanSkill skill, int row, int col) {
        int hNeighbors = 0;
        int vNeighbors = 0;
        if (SandboxController.inBounds(row, col - 1) && playerGrid[row][col - 1] == CellState.SHIP) hNeighbors++;
        if (SandboxController.inBounds(row, col + 1) && playerGrid[row][col + 1] == CellState.SHIP) hNeighbors++;
        if (SandboxController.inBounds(row - 1, col) && playerGrid[row - 1][col] == CellState.SHIP) vNeighbors++;
        if (SandboxController.inBounds(row + 1, col) && playerGrid[row + 1][col] == CellState.SHIP) vNeighbors++;
        if (hNeighbors > vNeighbors && rng.nextDouble() < 0.72) return true;
        if (vNeighbors > hNeighbors && rng.nextDouble() < 0.72) return false;
        return (bossCastSeq % 2 == 0) ^ rng.nextBoolean();
    }

    private int[] pickPlayerTargetNearFleet() {
        return pickPlayerTargetNearFleet(null);
    }

    private int[] pickPlayerTargetNearFleet(TitanMode.TitanSkill skill) {
        List<int[]> shipCells = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (playerGrid[r][c] == CellState.SHIP) shipCells.add(new int[]{r, c});
            }
        }
        if (shipCells.isEmpty()) {
            int bestR = rng.nextInt(Board.SIZE);
            int bestC = rng.nextInt(Board.SIZE);
            double bestEmpty = Double.NEGATIVE_INFINITY;
            for (int k = 0; k < 12; k++) {
                int r = 1 + rng.nextInt(Board.SIZE - 2);
                int c = 1 + rng.nextInt(Board.SIZE - 2);
                double s = scoreTargetSpreadPenalty(r, c) + rng.nextDouble() * 3.0;
                if (s > bestEmpty) {
                    bestEmpty = s;
                    bestR = r;
                    bestC = c;
                }
            }
            return new int[]{bestR, bestC};
        }

        List<int[]> candidates = new ArrayList<>(shipCells);
        // When few intact ships/cells remain, add flanking & swept sea sectors so Boss never camps 1 spot forever
        if (intactPlayerShips().size() <= 2 || shipCells.size() <= 6) {
            int[][] flankOffsets = {{-2, -1}, {-2, 1}, {2, -1}, {2, 1}, {-1, -2}, {1, -2}, {-1, 2}, {1, 2}};
            for (int[] sc : shipCells) {
                for (int[] off : flankOffsets) {
                    int nr = sc[0] + off[0];
                    int nc = sc[1] + off[1];
                    if (SandboxController.inBounds(nr, nc) && playerGrid[nr][nc] == CellState.EMPTY) {
                        candidates.add(new int[]{nr, nc});
                    }
                }
            }
            int[][] sectorSeeds = {{1, 2}, {1, 7}, {4, 2}, {5, 7}, {7, 2}, {8, 7}};
            for (int[] seed : sectorSeeds) {
                candidates.add(seed);
            }
        }

        record ScoredCell(int r, int c, double score) { }
        List<ScoredCell> scored = new ArrayList<>(candidates.size());
        int[] last = recentBossTargets.isEmpty() ? null : recentBossTargets.get(recentBossTargets.size() - 1);
        for (int[] cand : candidates) {
            int r = cand[0];
            int c = cand[1];
            boolean isShip = playerGrid[r][c] == CellState.SHIP;
            int adjShips = 0;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int nr = r + dr;
                    int nc = c + dc;
                    if (SandboxController.inBounds(nr, nc) && playerGrid[nr][nc] == CellState.SHIP) {
                        adjShips++;
                    }
                }
            }
            double sc = (isShip ? 9.0 : 2.5) + Math.min(2, adjShips) * 0.85;
            if (r >= 1 && r <= Board.SIZE - 2 && c >= 1 && c <= Board.SIZE - 2) {
                sc += 1.4;
            }
            if (last != null && ((r / 5 != last[0] / 5) || (c / 5 != last[1] / 5))) {
                sc += 3.6;
            }
            sc += scoreTargetSpreadPenalty(r, c);
            sc += rng.nextDouble() * 4.2;
            scored.add(new ScoredCell(r, c, sc));
        }
        scored.sort((a, b) -> Double.compare(b.score(), a.score()));
        double topScore = scored.get(0).score();
        int eligible = 1;
        while (eligible < Math.min(3, scored.size()) && (topScore - scored.get(eligible).score()) <= 3.5) {
            eligible++;
        }
        ScoredCell pick = scored.get(rng.nextInt(eligible));
        return new int[]{pick.r(), pick.c()};
    }

    private double scoreTargetSpreadPenalty(int r, int c) {
        double penalty = 0.0;
        int n = recentBossTargets.size();
        for (int i = 0; i < n; i++) {
            int[] prev = recentBossTargets.get(i);
            int ageFromOldest = i + 1;
            double weight = (double) ageFromOldest / Math.max(1, n); // most recent = 1.0
            int cheb = Math.max(Math.abs(r - prev[0]), Math.abs(c - prev[1]));
            if (cheb == 0) {
                penalty -= 38.0 * weight;
            } else if (cheb == 1) {
                penalty -= 20.0 * weight;
            } else if (cheb == 2) {
                penalty -= 8.5 * weight;
            }
        }
        return penalty;
    }

    private List<Ship> intactPlayerShips() {
        List<Ship> list = new ArrayList<>();
        for (Ship s : playerShips) {
            if (!SandboxController.isShipSunk(playerGrid, s)) list.add(s);
        }
        return list;
    }

    private boolean allPlayerShipsSunk() {
        return intactPlayerShips().isEmpty();
    }

    private static boolean containsCell(Collection<int[]> cells, int r, int c) {
        if (cells == null) return false;
        for (int[] cell : cells) {
            if (cell[0] == r && cell[1] == c) return true;
        }
        return false;
    }
}
