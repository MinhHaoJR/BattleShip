package battleship.ui;

import battleship.model.Board;
import battleship.model.CellState;
import battleship.model.Ship;
import battleship.model.ShipType;
import battleship.model.ShotResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure UI-side deterministic encounter engine for Campaign 1–100 non-Titan stages
 * (Skirmish, Sniper Ammo Limit, Timed Blitz, Sonar Fog, and Zombie Admiral Assault).
 * Supports 2..5 enemy ship puzzles, all 4 weapons & 4 magics, shields, and 1★–3★ scoring.
 */
public final class CampaignBattleController {
    private static final int[][] ORTHO = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    public record StrikeOutcome(
            ShotResult aggregateResult,
            List<int[]> hitCells,
            List<int[]> missCells,
            List<ShipType> newlySunkShips,
            ShipType lastHitType,
            int[] burnCell,
            List<int[]> sonarRevealedCells
    ) { }

    public record AiShotOutcome(
            int row,
            int col,
            ShotResult result,
            ShipType sunkShip,
            boolean absorbedByArmor
    ) { }

    public record CampaignDebriefSummary(
            CampaignMode.CampaignStage stage,
            boolean won,
            String failReason,
            int starsEarned,
            int bestStars,
            int shotsUsed,
            int shotLimit,
            int hitsScored,
            int hullPct,
            int elapsedSec,
            int remainingSec,
            int score
    ) {
        public String rank() {
            if (!won) return "F";
            return switch (starsEarned) {
                case 3 -> "S";
                case 2 -> "A";
                default -> "B";
            };
        }
    }

    private final CampaignMode.CampaignStage stage;
    private final List<Ship> initialPlayerShips;
    private final CellState[][] playerGrid = new CellState[Board.SIZE][Board.SIZE];
    private final CellState[][] enemyGrid = new CellState[Board.SIZE][Board.SIZE];
    private final int[][] playerSuperArmor = new int[Board.SIZE][Board.SIZE];
    private final List<Ship> playerShips = new ArrayList<>();
    private final List<Ship> enemyShips = new ArrayList<>();
    private final Set<ShipType> sunkPlayerTypes = EnumSet.noneOf(ShipType.class);
    private final Set<ShipType> sunkEnemyTypes = EnumSet.noneOf(ShipType.class);
    private final List<int[]> initialSonarHints = new ArrayList<>();

    private final boolean[][] aiTried = new boolean[Board.SIZE][Board.SIZE];
    private final Deque<int[]> aiHuntQueue = new ArrayDeque<>();
    private final Random aiRng;

    private int shotsUsed;
    private int hitsScored;
    private int playerTotalCells;

    public CampaignBattleController(List<Ship> initialPlayerShips, CampaignMode.CampaignStage stage) {
        this.stage = stage != null ? stage : CampaignMode.stage(1);
        this.initialPlayerShips = initialPlayerShips == null ? List.of() : List.copyOf(initialPlayerShips);
        this.aiRng = new Random(this.stage.seed() ^ 0x5A5A_1357L);
        reset();
    }

    public void reset() {
        clearGrid(playerGrid);
        clearGrid(enemyGrid);
        playerShips.clear();
        enemyShips.clear();
        sunkPlayerTypes.clear();
        sunkEnemyTypes.clear();
        initialSonarHints.clear();
        aiHuntQueue.clear();
        shotsUsed = 0;
        hitsScored = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                playerSuperArmor[r][c] = 0;
                aiTried[r][c] = false;
            }
        }

        if (!placeProvidedFleet(initialPlayerShips, playerGrid, playerShips)) {
            clearGrid(playerGrid);
            playerShips.clear();
            generateFleet(List.of(ShipType.values()), playerGrid, playerShips, new Random(stage.seed() ^ 0x112233L));
        }
        playerTotalCells = Math.max(1, countShipTotalCells(playerShips));

        List<ShipType> enemyTypes = stage.enemyFleetTypes().isEmpty()
                ? List.of(ShipType.CRUISER, ShipType.DESTROYER)
                : stage.enemyFleetTypes();
        generateFleet(enemyTypes, enemyGrid, enemyShips, new Random(stage.seed()));
        populateInitialSonarHints();
    }

    private void populateInitialSonarHints() {
        int want = stage.initialSonarHints();
        if (want <= 0 || enemyShips.isEmpty()) return;
        List<int[]> shipCells = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (enemyGrid[r][c] == CellState.SHIP) {
                    shipCells.add(new int[]{r, c});
                }
            }
        }
        Collections.shuffle(shipCells, new Random(stage.seed() + 97L));
        for (int i = 0; i < Math.min(want, shipCells.size()); i++) {
            initialSonarHints.add(shipCells.get(i));
        }
    }

    public CampaignMode.CampaignStage getStage() { return stage; }
    public CellState[][] getPlayerGrid() { return playerGrid; }
    public CellState[][] getEnemyGrid() { return enemyGrid; }
    public List<Ship> getPlayerShips() { return List.copyOf(playerShips); }
    public List<Ship> getEnemyShips() { return List.copyOf(enemyShips); }
    public List<int[]> getInitialSonarHints() { return List.copyOf(initialSonarHints); }

    public int getShotsUsed() { return shotsUsed; }
    public int getShotLimit() { return stage.shotLimit(); }
    public int getRemainingShots() {
        return stage.shotLimit() > 0 ? Math.max(0, stage.shotLimit() - shotsUsed) : Integer.MAX_VALUE;
    }
    public int getHitsScored() { return hitsScored; }

    public boolean allEnemySunk() {
        return !enemyShips.isEmpty() && sunkEnemyTypes.size() == enemyShips.size();
    }

    public boolean allPlayerSunk() {
        return !playerShips.isEmpty() && sunkPlayerTypes.size() == playerShips.size();
    }

    public boolean isOutOfAmmo() {
        return stage.shotLimit() > 0 && shotsUsed >= stage.shotLimit() && !allEnemySunk();
    }

    public int getPlayerHullPct() {
        int alive = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (playerGrid[r][c] == CellState.SHIP) alive++;
            }
        }
        return (int) Math.round((alive * 100.0) / Math.max(1, playerTotalCells));
    }

    public StrikeOutcome firePlayerStrike(
            int centerRow,
            int centerCol,
            List<int[]> targetCells,
            CombatLoadout.MagicOption magic
    ) {
        List<int[]> cells = (targetCells == null || targetCells.isEmpty())
                ? List.of(new int[]{centerRow, centerCol})
                : targetCells;
        List<int[]> hits = new ArrayList<>();
        List<int[]> misses = new ArrayList<>();
        List<ShipType> newlySunk = new ArrayList<>();
        ShipType lastHitType = null;
        int resolved = 0;

        for (int[] cell : cells) {
            if (cell == null || cell.length < 2 || !inBounds(cell[0], cell[1])) continue;
            ShotResult res = fireCell(true, cell[0], cell[1]);
            if (res == ShotResult.ALREADY_SHOT) continue;
            resolved++;
            if (res == ShotResult.HIT || res == ShotResult.SUNK) {
                hits.add(new int[]{cell[0], cell[1]});
                hitsScored++;
                Ship owner = findShipAt(enemyShips, cell[0], cell[1]);
                if (owner != null) {
                    lastHitType = owner.type;
                    if (res == ShotResult.SUNK && !newlySunk.contains(owner.type)) {
                        newlySunk.add(owner.type);
                    }
                }
            } else {
                misses.add(new int[]{cell[0], cell[1]});
            }
        }

        if (resolved == 0) {
            return new StrikeOutcome(ShotResult.ALREADY_SHOT, List.of(), List.of(), List.of(), null, null, List.of());
        }

        shotsUsed++;

        int[] burnCell = null;
        if (magic == CombatLoadout.MagicOption.FIRE && !hits.isEmpty()) {
            burnCell = findBurnAdjacentCell(hits);
            if (burnCell != null) {
                ShotResult burnRes = fireCell(true, burnCell[0], burnCell[1]);
                if (burnRes == ShotResult.HIT || burnRes == ShotResult.SUNK) {
                    hits.add(burnCell);
                    hitsScored++;
                    Ship owner = findShipAt(enemyShips, burnCell[0], burnCell[1]);
                    if (owner != null) {
                        lastHitType = owner.type;
                        if (burnRes == ShotResult.SUNK && !newlySunk.contains(owner.type)) {
                            newlySunk.add(owner.type);
                        }
                    }
                } else if (burnRes == ShotResult.MISS) {
                    misses.add(burnCell);
                }
            }
        }

        List<int[]> sonarReveals = List.of();
        if (magic == CombatLoadout.MagicOption.ARCANE_SONAR) {
            int[] ping = findNearestUnhitEnemyShipCell(centerRow, centerCol);
            if (ping != null) sonarReveals = List.of(ping);
        }

        ShotResult agg;
        if (!newlySunk.isEmpty()) agg = ShotResult.SUNK;
        else if (!hits.isEmpty()) agg = ShotResult.HIT;
        else agg = ShotResult.MISS;

        return new StrikeOutcome(agg, hits, misses, newlySunk, lastHitType, burnCell, sonarReveals);
    }

    public int[] pickAiTargetCell() {
        while (!aiHuntQueue.isEmpty()) {
            int[] cand = aiHuntQueue.pop();
            if (inBounds(cand[0], cand[1]) && !aiTried[cand[0]][cand[1]]) {
                return cand;
            }
        }
        List<int[]> parity = new ArrayList<>();
        List<int[]> fallback = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (aiTried[r][c]) continue;
                fallback.add(new int[]{r, c});
                if ((r + c) % 2 == 0) parity.add(new int[]{r, c});
            }
        }
        List<int[]> pool = !parity.isEmpty() ? parity : fallback;
        if (pool.isEmpty()) return null;
        return pool.get(aiRng.nextInt(pool.size()));
    }

    public AiShotOutcome fireAiAtPlayer(int row, int col, boolean shieldedByDome) {
        if (!inBounds(row, col)) return new AiShotOutcome(row, col, ShotResult.MISS, null, false);
        aiTried[row][col] = true;
        if (shieldedByDome) {
            return new AiShotOutcome(row, col, ShotResult.MISS, null, true);
        }
        if (playerSuperArmor[row][col] > 0) {
            playerSuperArmor[row][col]--;
            return new AiShotOutcome(row, col, ShotResult.MISS, null, true);
        }
        ShotResult res = fireCell(false, row, col);
        Ship sunk = null;
        if (res == ShotResult.HIT) {
            for (int[] d : ORTHO) {
                int nr = row + d[0];
                int nc = col + d[1];
                if (inBounds(nr, nc) && !aiTried[nr][nc]) {
                    aiHuntQueue.push(new int[]{nr, nc});
                }
            }
        } else if (res == ShotResult.SUNK) {
            sunk = findShipAt(playerShips, row, col);
            aiHuntQueue.clear();
        }
        return new AiShotOutcome(row, col, res, sunk == null ? null : sunk.type, false);
    }

    public List<ZombieMode.ZombieClass> aliveEnemyZombieClasses() {
        List<ZombieMode.ZombieClass> list = new ArrayList<>();
        for (Ship s : enemyShips) {
            if (!sunkEnemyTypes.contains(s.type)) {
                list.add(ZombieMode.forShipType(s.type));
            }
        }
        return list;
    }

    public int applySuperShipArmorBoost() {
        int armed = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (playerGrid[r][c] == CellState.SHIP) {
                    playerSuperArmor[r][c] = Math.max(playerSuperArmor[r][c], 1);
                    armed++;
                }
            }
        }
        return armed;
    }

    public int getPlayerSuperArmor(int r, int c) {
        return inBounds(r, c) ? playerSuperArmor[r][c] : 0;
    }

    public CampaignDebriefSummary buildDebriefSummary(boolean won, String failReason, int elapsedSec, int remainingSec) {
        int hullPct = getPlayerHullPct();
        int stars = 0;
        int score = 0;
        if (won) {
            stars = Math.max(1, computeStars(hullPct, remainingSec));
            score = Math.max(100,
                    stage.stageNumber() * 25
                            + hullPct * 12
                            + Math.max(0, 40 - shotsUsed) * 20
                            + Math.max(0, remainingSec) * 10
                            + stars * 200);
        }
        int bestStars = Math.max(stars, CampaignMode.getStars(stage.stageNumber()));
        return new CampaignDebriefSummary(
                stage, won, failReason, stars, bestStars,
                shotsUsed, stage.shotLimit(), hitsScored,
                hullPct, Math.max(0, elapsedSec), Math.max(0, remainingSec), score
        );
    }

    public int computeStars(int hullPct, int remainingSec) {
        if (!allEnemySunk()) return 0;
        if (stage.ruleType() == CampaignMode.StageRuleType.BLITZ_TIMED) {
            if (remainingSec >= 30 && hullPct >= 60) return 3;
            if (remainingSec >= 15 || hullPct >= 45) return 2;
            return 1;
        }
        int par = stage.parShots();
        if (shotsUsed <= par && hullPct >= 60) return 3;
        if (shotsUsed <= par + 5 || hullPct >= 40) return 2;
        return 1;
    }

    private ShotResult fireCell(boolean onEnemy, int row, int col) {
        if (!inBounds(row, col)) return ShotResult.MISS;
        CellState[][] grid = onEnemy ? enemyGrid : playerGrid;
        List<Ship> fleet = onEnemy ? enemyShips : playerShips;
        Set<ShipType> sunk = onEnemy ? sunkEnemyTypes : sunkPlayerTypes;

        CellState current = grid[row][col];
        if (current == CellState.HIT || current == CellState.MISS) {
            return ShotResult.ALREADY_SHOT;
        }
        if (current == CellState.SHIP) {
            grid[row][col] = CellState.HIT;
            Ship owner = findShipAt(fleet, row, col);
            if (owner != null && isShipSunk(grid, owner)) {
                sunk.add(owner.type);
                return ShotResult.SUNK;
            }
            return ShotResult.HIT;
        }
        grid[row][col] = CellState.MISS;
        return ShotResult.MISS;
    }

    private int[] findBurnAdjacentCell(List<int[]> hitCells) {
        for (int[] hit : hitCells) {
            for (int[] d : ORTHO) {
                int nr = hit[0] + d[0];
                int nc = hit[1] + d[1];
                if (inBounds(nr, nc) && enemyGrid[nr][nc] == CellState.SHIP) {
                    return new int[]{nr, nc};
                }
            }
        }
        return null;
    }

    private int[] findNearestUnhitEnemyShipCell(int centerRow, int centerCol) {
        int[] best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (enemyGrid[r][c] == CellState.SHIP) {
                    int dist = Math.abs(r - centerRow) + Math.abs(c - centerCol);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = new int[]{r, c};
                    }
                }
            }
        }
        return best;
    }

    private static int countShipTotalCells(List<Ship> ships) {
        int sum = 0;
        for (Ship s : ships) sum += s.type.length;
        return sum;
    }

    private static void clearGrid(CellState[][] grid) {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                grid[r][c] = CellState.EMPTY;
            }
        }
    }

    private static boolean placeProvidedFleet(List<Ship> provided, CellState[][] grid, List<Ship> dest) {
        if (provided == null || provided.size() != ShipType.values().length) return false;
        for (Ship s : provided) {
            if (s == null || !canPlace(grid, s.type, s.row, s.col, s.horizontal)) return false;
            markShip(grid, s);
            dest.add(s);
        }
        return true;
    }

    private static void generateFleet(List<ShipType> types, CellState[][] grid, List<Ship> dest, Random rng) {
        for (ShipType type : types) {
            for (int attempt = 0; attempt < 400; attempt++) {
                int r = rng.nextInt(Board.SIZE);
                int c = rng.nextInt(Board.SIZE);
                boolean horizontal = rng.nextBoolean();
                if (canPlace(grid, type, r, c, horizontal)) {
                    Ship ship = new Ship(type, r, c, horizontal);
                    markShip(grid, ship);
                    dest.add(ship);
                    break;
                }
            }
        }
    }

    private static boolean canPlace(CellState[][] grid, ShipType type, int r, int c, boolean horizontal) {
        for (int i = 0; i < type.length; i++) {
            int rr = r + (horizontal ? 0 : i);
            int cc = c + (horizontal ? i : 0);
            if (!inBounds(rr, cc) || grid[rr][cc] != CellState.EMPTY) return false;
        }
        return true;
    }

    private static void markShip(CellState[][] grid, Ship ship) {
        for (int i = 0; i < ship.type.length; i++) {
            int rr = ship.row + (ship.horizontal ? 0 : i);
            int cc = ship.col + (ship.horizontal ? i : 0);
            grid[rr][cc] = CellState.SHIP;
        }
    }

    private static Ship findShipAt(List<Ship> fleet, int row, int col) {
        for (Ship ship : fleet) {
            for (int i = 0; i < ship.type.length; i++) {
                int rr = ship.row + (ship.horizontal ? 0 : i);
                int cc = ship.col + (ship.horizontal ? i : 0);
                if (rr == row && cc == col) return ship;
            }
        }
        return null;
    }

    private static boolean isShipSunk(CellState[][] grid, Ship ship) {
        for (int i = 0; i < ship.type.length; i++) {
            int rr = ship.row + (ship.horizontal ? 0 : i);
            int cc = ship.col + (ship.horizontal ? i : 0);
            if (grid[rr][cc] != CellState.HIT) return false;
        }
        return true;
    }

    private static boolean inBounds(int r, int c) {
        return r >= 0 && r < Board.SIZE && c >= 0 && c < Board.SIZE;
    }
}
