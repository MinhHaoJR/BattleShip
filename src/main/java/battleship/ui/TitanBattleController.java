package battleship.ui;

import battleship.model.Board;
import battleship.model.CellState;
import battleship.model.Ship;
import battleship.model.ShipType;
import battleship.model.ShotResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure UI-side 10x10 combat engine for Player Fleet (left) vs AI Titan Boss (right).
 * Coordinates 20-cell Titan anatomy hitboxes, weakpoint hits, Phase 2 "Cuồng Bạo" Enrage transitions,
 * and AI-cast 5-skill Titan Boss rotations (3 Phase 1 skills + 2 unlocked Phase 2 ultimates).
 */
public class TitanBattleController {
    private final CellState[][] playerGrid = new CellState[Board.SIZE][Board.SIZE];
    private final CellState[][] bossGrid = new CellState[Board.SIZE][Board.SIZE];
    private final boolean[][] bossBody = new boolean[Board.SIZE][Board.SIZE];
    private final boolean[][] bossWeakpoint = new boolean[Board.SIZE][Board.SIZE];
    private final List<Ship> playerShips = new ArrayList<>();
    private final Set<ShipType> sunkPlayerTypes = EnumSet.noneOf(ShipType.class);
    private final List<Ship> initialPlayerShips;
    private final Random random = new Random();
    private TitanMode.TitanBoss boss;
    private int skillRotationIndex;
    private boolean enragedAnnounced;
    private int playerShotCount;
    private int bossShotCount;
    private final int[][] superArmorGrid = new int[Board.SIZE][Board.SIZE];
    private final List<int[]> lastArmorAbsorbedCells = new ArrayList<>();

    public record PlayerStrikeOutcome(
            ShotResult aggregateResult,
            List<int[]> hitCells,
            List<int[]> weakpointHits,
            List<int[]> missCells,
            int[] burnCell,
            List<int[]> sonarCells,
            boolean enragedJustTriggered,
            boolean bossDefeated
    ) { }

    public record TitanSkillOutcome(
            TitanMode.TitanBoss boss,
            TitanMode.TitanSkill skill,
            boolean enraged,
            int targetRow,
            int targetCol,
            boolean horizontal,
            List<int[]> targetCells,
            List<int[]> hitCells,
            List<int[]> missCells,
            List<int[]> shieldBlockedCells,
            List<ShipType> newlySunkShips,
            boolean playerDefeated
    ) { }

    public TitanBattleController(List<Ship> initialPlayerShips, TitanMode.TitanBoss boss) {
        this.initialPlayerShips = initialPlayerShips == null ? List.of() : List.copyOf(initialPlayerShips);
        this.boss = boss != null ? boss : TitanMode.TitanBoss.GODZILLA;
        reset(this.boss);
    }

    public void reset(TitanMode.TitanBoss newBoss) {
        this.boss = newBoss != null ? newBoss : TitanMode.TitanBoss.GODZILLA;
        SandboxController.clearGrid(playerGrid);
        SandboxController.clearGrid(bossGrid);
        playerShips.clear();
        sunkPlayerTypes.clear();
        skillRotationIndex = 0;
        enragedAnnounced = false;
        playerShotCount = 0;
        bossShotCount = 0;
        lastArmorAbsorbedCells.clear();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                superArmorGrid[r][c] = 0;
            }
        }

        if (!SandboxController.placeProvidedFleet(initialPlayerShips, playerGrid, playerShips)) {
            SandboxController.clearGrid(playerGrid);
            playerShips.clear();
            SandboxController.generateRandomFleet(playerGrid, playerShips, random);
        }
        rebuildBossHitbox();
    }

    public void switchBossOnly(TitanMode.TitanBoss newBoss) {
        this.boss = newBoss != null ? newBoss : TitanMode.TitanBoss.GODZILLA;
        enragedAnnounced = false;
        skillRotationIndex = 0;
        bossShotCount = 0;
        rebuildBossHitbox();
    }

    private void rebuildBossHitbox() {
        SandboxController.clearGrid(bossGrid);
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                bossBody[r][c] = false;
                bossWeakpoint[r][c] = false;
            }
        }
        for (int[] cell : TitanMode.generateHitboxCells(boss)) {
            if (SandboxController.inBounds(cell[0], cell[1])) {
                bossBody[cell[0]][cell[1]] = true;
                bossGrid[cell[0]][cell[1]] = CellState.SHIP;
            }
        }
        for (int[] wp : TitanMode.generateWeakpointCells(boss)) {
            if (SandboxController.inBounds(wp[0], wp[1])) {
                bossWeakpoint[wp[0]][wp[1]] = true;
            }
        }
    }

    public TitanMode.TitanBoss getBoss() { return boss; }
    public CellState[][] getPlayerGrid() { return playerGrid; }
    public CellState[][] getBossGrid() { return bossGrid; }
    public List<Ship> getPlayerShips() { return List.copyOf(playerShips); }
    public int getPlayerShotCount() { return playerShotCount; }
    public int getBossShotCount() { return bossShotCount; }
    public int countSunkPlayerShips() { return sunkPlayerTypes.size(); }

    public int getMaxHp() { return boss.maxHp; }

    public int getRemainingHp() {
        int hp = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (bossBody[r][c] && bossGrid[r][c] != CellState.HIT) hp++;
            }
        }
        return hp;
    }

    public boolean isEnraged() {
        return getRemainingHp() <= (getMaxHp() / 2);
    }

    public boolean isTitanDefeated() {
        return getRemainingHp() == 0;
    }

    public boolean isPlayerDefeated() {
        if (playerShips.isEmpty()) return false;
        for (Ship s : playerShips) {
            if (!SandboxController.isShipSunk(playerGrid, s)) return false;
        }
        return true;
    }

    public PlayerStrikeOutcome fireAtTitan(
            int centerRow,
            int centerCol,
            List<int[]> targetCells,
            CombatLoadout.MagicOption magic
    ) {
        List<int[]> cells = (targetCells == null || targetCells.isEmpty())
                ? List.of(new int[]{centerRow, centerCol})
                : targetCells;

        List<int[]> hits = new ArrayList<>();
        List<int[]> weakHits = new ArrayList<>();
        List<int[]> misses = new ArrayList<>();
        boolean anyFresh = false;
        boolean wasEnraged = isEnraged();

        for (int[] cell : cells) {
            int r = cell[0];
            int c = cell[1];
            if (!SandboxController.inBounds(r, c)) continue;
            CellState st = bossGrid[r][c];
            if (st == CellState.HIT || st == CellState.MISS) continue;
            anyFresh = true;
            if (bossBody[r][c]) {
                bossGrid[r][c] = CellState.HIT;
                hits.add(new int[]{r, c});
                if (bossWeakpoint[r][c]) {
                    weakHits.add(new int[]{r, c});
                    // Critical weakpoint strike fractures one intact adjacent Titan cell if available
                    int[] extra = findAdjacentIntactTitanCell(r, c);
                    if (extra != null) {
                        bossGrid[extra[0]][extra[1]] = CellState.HIT;
                        hits.add(extra);
                    }
                }
            } else {
                bossGrid[r][c] = CellState.MISS;
                misses.add(new int[]{r, c});
            }
        }

        if (!anyFresh) {
            return new PlayerStrikeOutcome(ShotResult.ALREADY_SHOT, hits, weakHits, misses, null, List.of(), false, isTitanDefeated());
        }
        playerShotCount++;

        int[] burnCell = null;
        if (magic == CombatLoadout.MagicOption.FIRE && !hits.isEmpty()) {
            burnCell = findAdjacentIntactTitanCell(hits.get(0)[0], hits.get(0)[1]);
            if (burnCell != null) {
                bossGrid[burnCell[0]][burnCell[1]] = CellState.HIT;
                hits.add(burnCell);
            }
        }

        List<int[]> sonarCells = new ArrayList<>();
        if (magic == CombatLoadout.MagicOption.ARCANE_SONAR) {
            for (int r = 0; r < Board.SIZE && sonarCells.size() < 4; r++) {
                for (int c = 0; c < Board.SIZE && sonarCells.size() < 4; c++) {
                    if (bossBody[r][c] && bossGrid[r][c] != CellState.HIT && bossWeakpoint[r][c]) {
                        sonarCells.add(new int[]{r, c});
                    }
                }
            }
            for (int r = 0; r < Board.SIZE && sonarCells.size() < 4; r++) {
                for (int c = 0; c < Board.SIZE && sonarCells.size() < 4; c++) {
                    if (bossBody[r][c] && bossGrid[r][c] != CellState.HIT && !bossWeakpoint[r][c]) {
                        sonarCells.add(new int[]{r, c});
                    }
                }
            }
        }

        boolean defeated = isTitanDefeated();
        boolean enrageNow = !wasEnraged && isEnraged() && !enragedAnnounced;
        if (enrageNow) enragedAnnounced = true;

        ShotResult agg = defeated ? ShotResult.SUNK : (!hits.isEmpty() ? ShotResult.HIT : ShotResult.MISS);
        return new PlayerStrikeOutcome(agg, hits, weakHits, misses, burnCell, sonarCells, enrageNow, defeated);
    }

    /** AI selects & executes one of its Phase 1 (3 skills) or Phase 2 Enraged (all 5 skills) movie abilities. */
    public TitanSkillOutcome castTitanSkill(
            TitanMode.TitanSkill forcedSkill,
            Collection<int[]> playerShields
    ) {
        boolean enraged = isEnraged();
        List<TitanMode.TitanSkill> phase1Pool = TitanMode.skillsFor(boss, false);
        List<TitanMode.TitanSkill> allPool = TitanMode.skillsFor(boss, true);
        TitanMode.TitanSkill skill;
        if (forcedSkill != null && forcedSkill.boss == boss) {
            skill = forcedSkill;
        } else if (enraged) {
            // Phase 2 Enraged Titan prioritizes devastating newly-unlocked Phase 2 ultimates alongside charged ultimates
            List<TitanMode.TitanSkill> p2Only = new ArrayList<>(2);
            for (TitanMode.TitanSkill s : allPool) {
                if (s.phase2Only) p2Only.add(s);
            }
            int roll = (skillRotationIndex++) % 4;
            if (roll == 0 && !p2Only.isEmpty()) {
                skill = p2Only.get(0);
            } else if (roll == 1 && p2Only.size() > 1) {
                skill = p2Only.get(1);
            } else if (roll == 2 && phase1Pool.size() > 2) {
                skill = phase1Pool.get(2);
            } else {
                skill = allPool.get(Math.min(allPool.size() - 1, Math.max(0, 1)));
            }
        } else {
            skill = phase1Pool.get((skillRotationIndex++) % Math.max(1, phase1Pool.size()));
        }

        int[] anchor = pickSmartPlayerAnchor();
        int tr = anchor != null ? anchor[0] : (2 + random.nextInt(6));
        int tc = anchor != null ? anchor[1] : (2 + random.nextInt(6));
        boolean horiz = pickSmartOrientation(tr, tc);

        List<int[]> cells = TitanMode.skillTargetCells(skill, tr, tc, horiz);
        List<int[]> hits = new ArrayList<>();
        List<int[]> misses = new ArrayList<>();
        List<int[]> blocked = new ArrayList<>();
        List<ShipType> newlySunk = new ArrayList<>();
        lastArmorAbsorbedCells.clear();

        for (int[] c : cells) {
            int r = c[0];
            int col = c[1];
            if (!SandboxController.inBounds(r, col)) continue;
            if (isCellShielded(playerShields, r, col)) {
                blocked.add(new int[]{r, col});
                continue;
            }
            CellState st = playerGrid[r][col];
            if (st == CellState.HIT || st == CellState.MISS) continue;
            if (st == CellState.SHIP) {
                if (superArmorGrid[r][col] > 0) {
                    superArmorGrid[r][col]--;
                    lastArmorAbsorbedCells.add(new int[]{r, col});
                    continue;
                }
                playerGrid[r][col] = CellState.HIT;
                hits.add(new int[]{r, col});
                Ship owner = SandboxController.findShipAt(playerShips, r, col);
                if (owner != null && SandboxController.isShipSunk(playerGrid, owner) && sunkPlayerTypes.add(owner.type)) {
                    newlySunk.add(owner.type);
                }
            } else {
                playerGrid[r][col] = CellState.MISS;
                misses.add(new int[]{r, col});
            }
        }
        bossShotCount++;

        return new TitanSkillOutcome(
                boss, skill, enraged, tr, tc, horiz,
                cells, hits, misses, blocked, newlySunk, isPlayerDefeated()
        );
    }

    public List<int[]> listIntactBossCells() {
        List<int[]> out = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (bossBody[r][c] && bossGrid[r][c] != CellState.HIT) out.add(new int[]{r, c});
            }
        }
        return out;
    }

    public int countAlivePlayerShips() {
        int alive = 0;
        for (Ship s : playerShips) {
            if (!SandboxController.isShipSunk(playerGrid, s)) alive++;
        }
        return alive;
    }

    public void activateSuperArmorForLastShip() {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                superArmorGrid[r][c] = 0;
            }
        }
        for (Ship s : playerShips) {
            if (SandboxController.isShipSunk(playerGrid, s)) continue;
            for (int i = 0; i < s.type.length; i++) {
                int r = s.row + (s.horizontal ? 0 : i);
                int c = s.col + (s.horizontal ? i : 0);
                if (SandboxController.inBounds(r, c) && playerGrid[r][c] == CellState.SHIP) {
                    superArmorGrid[r][c] = 2;
                }
            }
        }
    }

    public int[][] getSuperArmorGrid() {
        int[][] copy = new int[Board.SIZE][Board.SIZE];
        for (int r = 0; r < Board.SIZE; r++) {
            System.arraycopy(superArmorGrid[r], 0, copy[r], 0, Board.SIZE);
        }
        return copy;
    }

    public List<int[]> consumeLastArmorAbsorbedCells() {
        List<int[]> out = List.copyOf(lastArmorAbsorbedCells);
        lastArmorAbsorbedCells.clear();
        return out;
    }

    /** Fires the awakened Super Ship's Hyperion Orbital Beam (3x3 pierce + bonus adjacent armor break + weakpoint scan). */
    public PlayerStrikeOutcome fireHyperionUltimate(int centerRow, int centerCol, List<int[]> targetCells) {
        List<int[]> cells = (targetCells == null || targetCells.isEmpty())
                ? List.of(new int[]{centerRow, centerCol})
                : targetCells;
        List<int[]> hits = new ArrayList<>();
        List<int[]> weakHits = new ArrayList<>();
        List<int[]> misses = new ArrayList<>();
        boolean wasEnraged = isEnraged();

        for (int[] cell : cells) {
            if (cell == null || cell.length < 2) continue;
            int r = cell[0];
            int c = cell[1];
            if (!SandboxController.inBounds(r, c)) continue;
            if (bossBody[r][c] && bossGrid[r][c] != CellState.HIT) {
                bossGrid[r][c] = CellState.HIT;
                hits.add(new int[]{r, c});
                if (bossWeakpoint[r][c]) {
                    weakHits.add(new int[]{r, c});
                }
            } else if (!bossBody[r][c] && bossGrid[r][c] != CellState.MISS) {
                bossGrid[r][c] = CellState.MISS;
                misses.add(new int[]{r, c});
            }
        }
        if (!hits.isEmpty()) {
            int[] cleave = findAdjacentIntactTitanCell(hits.get(0)[0], hits.get(0)[1]);
            if (cleave != null) {
                bossGrid[cleave[0]][cleave[1]] = CellState.HIT;
                hits.add(cleave);
                if (bossWeakpoint[cleave[0]][cleave[1]]) {
                    weakHits.add(cleave);
                }
            }
        }
        playerShotCount++;

        List<int[]> sonarCells = new ArrayList<>();
        for (int r = 0; r < Board.SIZE && sonarCells.size() < 3; r++) {
            for (int c = 0; c < Board.SIZE && sonarCells.size() < 3; c++) {
                if (bossBody[r][c] && bossGrid[r][c] != CellState.HIT && bossWeakpoint[r][c]) {
                    sonarCells.add(new int[]{r, c});
                }
            }
        }

        boolean defeated = isTitanDefeated();
        boolean enrageNow = !wasEnraged && isEnraged() && !enragedAnnounced;
        if (enrageNow) enragedAnnounced = true;
        ShotResult agg = defeated ? ShotResult.SUNK : (!hits.isEmpty() ? ShotResult.HIT : ShotResult.MISS);
        return new PlayerStrikeOutcome(agg, hits, weakHits, misses, null, sonarCells, enrageNow, defeated);
    }

    private int[] findAdjacentIntactTitanCell(int r, int c) {
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : dirs) {
            int nr = r + d[0];
            int nc = c + d[1];
            if (SandboxController.inBounds(nr, nc) && bossBody[nr][nc] && bossGrid[nr][nc] != CellState.HIT) {
                return new int[]{nr, nc};
            }
        }
        return null;
    }

    private int[] pickSmartPlayerAnchor() {
        // First try to follow up near an injured unsunk player ship
        List<int[]> activeShipCells = new ArrayList<>();
        List<int[]> injuredFollowUps = new ArrayList<>();
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (playerGrid[r][c] == CellState.SHIP) {
                    activeShipCells.add(new int[]{r, c});
                } else if (playerGrid[r][c] == CellState.HIT) {
                    Ship s = SandboxController.findShipAt(playerShips, r, c);
                    if (s != null && !SandboxController.isShipSunk(playerGrid, s)) {
                        for (int[] d : dirs) {
                            int nr = r + d[0], nc = c + d[1];
                            if (SandboxController.inBounds(nr, nc) && playerGrid[nr][nc] == CellState.SHIP) {
                                injuredFollowUps.add(new int[]{nr, nc});
                            }
                        }
                    }
                }
            }
        }
        if (!injuredFollowUps.isEmpty() && random.nextInt(10) < 7) {
            return injuredFollowUps.get(random.nextInt(injuredFollowUps.size()));
        }
        // Smart Titan radar: 55% locks near an unhit ship sector, 45% sweeps random ocean sector
        if (!activeShipCells.isEmpty() && random.nextInt(10) < 6) {
            int[] s = activeShipCells.get(random.nextInt(activeShipCells.size()));
            int dr = random.nextInt(3) - 1;
            int dc = random.nextInt(3) - 1;
            return new int[]{Math.max(1, Math.min(8, s[0] + dr)), Math.max(1, Math.min(8, s[1] + dc))};
        }
        List<int[]> unshot = new ArrayList<>();
        for (int r = 1; r < Board.SIZE - 1; r++) {
            for (int c = 1; c < Board.SIZE - 1; c++) {
                if (SandboxController.isUnshot(playerGrid[r][c])) unshot.add(new int[]{r, c});
            }
        }
        if (!unshot.isEmpty()) return unshot.get(random.nextInt(unshot.size()));
        return activeShipCells.isEmpty() ? null : activeShipCells.get(random.nextInt(activeShipCells.size()));
    }

    private boolean pickSmartOrientation(int r, int c) {
        int hScore = 0, vScore = 0;
        for (int d = -2; d <= 2; d++) {
            if (SandboxController.inBounds(r, c + d) && SandboxController.isUnshot(playerGrid[r][c + d])) hScore++;
            if (SandboxController.inBounds(r + d, c) && SandboxController.isUnshot(playerGrid[r + d][c])) vScore++;
        }
        if (hScore == vScore) return random.nextBoolean();
        return hScore > vScore;
    }

    private static boolean isCellShielded(Collection<int[]> shields, int r, int c) {
        if (shields == null) return false;
        for (int[] s : shields) {
            if (s != null && s.length >= 2 && s[0] == r && s[1] == c) return true;
        }
        return false;
    }
}
