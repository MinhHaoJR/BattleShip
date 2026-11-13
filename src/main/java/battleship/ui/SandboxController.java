package battleship.ui;

import battleship.model.Board;
import battleship.model.CellState;
import battleship.model.Ship;
import battleship.model.ShipType;
import battleship.model.ShotResult;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Pure UI-side 10x10 combat simulator enabling full interactive Sandbox testing without logic TODOs. */
public class SandboxController {
    private final CellState[][] playerGrid = new CellState[Board.SIZE][Board.SIZE];
    private final CellState[][] enemyGrid = new CellState[Board.SIZE][Board.SIZE];
    private final List<Ship> playerShips = new ArrayList<>();
    private final List<Ship> enemyShips = new ArrayList<>();
    private final Set<ShipType> sunkPlayerTypes = EnumSet.noneOf(ShipType.class);
    private final Set<ShipType> sunkEnemyTypes = EnumSet.noneOf(ShipType.class);
    private final Random random = new Random();
    private final List<Ship> initialPlayerShips;
    private final int[][] playerSuperArmor = new int[Board.SIZE][Board.SIZE];

    public record StrikeOutcome(
            ShotResult aggregateResult,
            List<int[]> hitCells,
            List<int[]> missCells,
            List<ShipType> newlySunkShips,
            ShipType lastHitType,
            int[] burnCell,
            List<int[]> sonarRevealedCells
    ) { }

    public SandboxController(List<Ship> initialPlayerShips) {
        this.initialPlayerShips = initialPlayerShips == null ? List.of() : List.copyOf(initialPlayerShips);
        reset();
    }

    public void reset() {
        clearGrid(playerGrid);
        clearGrid(enemyGrid);
        playerShips.clear();
        enemyShips.clear();
        sunkPlayerTypes.clear();
        sunkEnemyTypes.clear();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                playerSuperArmor[r][c] = 0;
            }
        }

        if (!placeProvidedFleet(initialPlayerShips, playerGrid, playerShips)) {
            clearGrid(playerGrid);
            playerShips.clear();
            generateRandomFleet(playerGrid, playerShips, random);
        }
        generateRandomFleet(enemyGrid, enemyShips, random);
    }

    public CellState[][] getPlayerGrid() { return playerGrid; }
    public CellState[][] getEnemyGrid() { return enemyGrid; }
    public List<Ship> getPlayerShips() { return List.copyOf(playerShips); }
    public List<Ship> getEnemyShips() { return List.copyOf(enemyShips); }

    public ShotResult fireCell(boolean onEnemy, int row, int col) {
        if (!inBounds(row, col)) return ShotResult.MISS;
        CellState[][] grid = grid(onEnemy);
        List<Ship> fleet = fleet(onEnemy);
        Set<ShipType> sunk = sunkSet(onEnemy);

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

    public StrikeOutcome fireStrike(
            boolean onEnemy,
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
        int resolvedCount = 0;

        for (int[] cell : cells) {
            if (cell == null || cell.length < 2 || !inBounds(cell[0], cell[1])) continue;
            ShotResult res = fireCell(onEnemy, cell[0], cell[1]);
            if (res == ShotResult.ALREADY_SHOT) continue;
            resolvedCount++;
            if (res == ShotResult.HIT || res == ShotResult.SUNK) {
                hits.add(new int[]{cell[0], cell[1]});
                Ship owner = findShipAt(fleet(onEnemy), cell[0], cell[1]);
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

        if (resolvedCount == 0) {
            return new StrikeOutcome(ShotResult.ALREADY_SHOT, List.of(), List.of(), List.of(), null, null, List.of());
        }

        int[] burnCell = null;
        if (magic == CombatLoadout.MagicOption.FIRE && !hits.isEmpty()) {
            burnCell = findBurnAdjacentCell(onEnemy, hits);
            if (burnCell != null) {
                ShotResult burnRes = fireCell(onEnemy, burnCell[0], burnCell[1]);
                if (burnRes == ShotResult.HIT || burnRes == ShotResult.SUNK) {
                    hits.add(burnCell);
                    Ship owner = findShipAt(fleet(onEnemy), burnCell[0], burnCell[1]);
                    if (owner != null) {
                        lastHitType = owner.type;
                        if (burnRes == ShotResult.SUNK && !newlySunk.contains(owner.type)) {
                            newlySunk.add(owner.type);
                        }
                    }
                }
            }
        }

        List<int[]> sonarReveals = List.of();
        if (magic == CombatLoadout.MagicOption.ARCANE_SONAR) {
            sonarReveals = findIntactShipsInArea(onEnemy, centerRow, centerCol, 1);
        }

        ShotResult aggregate;
        if (!newlySunk.isEmpty()) {
            aggregate = ShotResult.SUNK;
        } else if (!hits.isEmpty()) {
            aggregate = ShotResult.HIT;
        } else {
            aggregate = ShotResult.MISS;
        }
        return new StrikeOutcome(aggregate, List.copyOf(hits), List.copyOf(misses),
                List.copyOf(newlySunk), lastHitType, burnCell, sonarReveals);
    }

    public List<int[]> findIntactShipsInArea(boolean onEnemy, int centerRow, int centerCol, int radius) {
        CellState[][] grid = grid(onEnemy);
        List<int[]> found = new ArrayList<>();
        for (int dr = -radius; dr <= radius; dr++) {
            for (int dc = -radius; dc <= radius; dc++) {
                int r = centerRow + dr;
                int c = centerCol + dc;
                if (inBounds(r, c) && grid[r][c] == CellState.SHIP) {
                    found.add(new int[]{r, c});
                }
            }
        }
        return List.copyOf(found);
    }

    public Ship findShipAt(boolean onEnemy, int row, int col) {
        return findShipAt(fleet(onEnemy), row, col);
    }

    public int countShots(boolean onEnemy) {
        CellState[][] grid = grid(onEnemy);
        int count = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (grid[r][c] == CellState.HIT || grid[r][c] == CellState.MISS) count++;
            }
        }
        return count;
    }

    public int countSunkShips(boolean onEnemy) {
        return sunkSet(onEnemy).size();
    }

    public boolean areAllSunk(boolean onEnemy) {
        List<Ship> fleet = fleet(onEnemy);
        if (fleet.isEmpty()) return false;
        CellState[][] grid = grid(onEnemy);
        for (Ship s : fleet) {
            if (!isShipSunk(grid, s)) return false;
        }
        return true;
    }

    /** Sandbox convenience helper: preserves 1 surviving player flagship and sinks the remaining escorts to enter Last Stand. */
    public Ship prepareLastStandSandbox() {
        Ship keeper = null;
        int aliveCount = 0;
        for (Ship s : playerShips) {
            if (!isShipSunk(playerGrid, s)) {
                aliveCount++;
                if (keeper == null) keeper = s;
            }
        }
        if (keeper == null && !playerShips.isEmpty()) {
            keeper = playerShips.get(0);
        }
        if (keeper == null) return null;

        if (aliveCount > 1 || aliveCount == 0) {
            sunkPlayerTypes.clear();
            for (Ship s : playerShips) {
                boolean keep = (s == keeper);
                for (int i = 0; i < s.type.length; i++) {
                    int r = s.row + (s.horizontal ? 0 : i);
                    int c = s.col + (s.horizontal ? i : 0);
                    if (inBounds(r, c)) {
                        playerGrid[r][c] = keep ? CellState.SHIP : CellState.HIT;
                    }
                }
                if (!keep) sunkPlayerTypes.add(s.type);
            }
        }
        return keeper;
    }

    public void activateSuperArmor() {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                playerSuperArmor[r][c] = 0;
            }
        }
        for (Ship s : playerShips) {
            if (isShipSunk(playerGrid, s)) continue;
            for (int i = 0; i < s.type.length; i++) {
                int r = s.row + (s.horizontal ? 0 : i);
                int c = s.col + (s.horizontal ? i : 0);
                if (inBounds(r, c) && playerGrid[r][c] == CellState.SHIP) {
                    playerSuperArmor[r][c] = 2;
                }
            }
        }
    }

    public int[][] getPlayerSuperArmor() {
        int[][] copy = new int[Board.SIZE][Board.SIZE];
        for (int r = 0; r < Board.SIZE; r++) {
            System.arraycopy(playerSuperArmor[r], 0, copy[r], 0, Board.SIZE);
        }
        return copy;
    }

    public boolean consumePlayerArmor(int row, int col) {
        if (inBounds(row, col) && playerGrid[row][col] == CellState.SHIP && playerSuperArmor[row][col] > 0) {
            playerSuperArmor[row][col]--;
            return true;
        }
        return false;
    }

    public StrikeOutcome fireHyperionUltimate(int centerRow, int centerCol, List<int[]> targetCells) {
        StrikeOutcome base = fireStrike(true, centerRow, centerCol, targetCells, CombatLoadout.MagicOption.ARCANE_SONAR);
        List<int[]> hits = new ArrayList<>(base.hitCells());
        List<ShipType> newlySunk = new ArrayList<>(base.newlySunkShips());
        ShipType lastType = base.lastHitType();
        if (!hits.isEmpty()) {
            int[] first = hits.get(0);
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] d : dirs) {
                int nr = first[0] + d[0];
                int nc = first[1] + d[1];
                if (inBounds(nr, nc) && enemyGrid[nr][nc] == CellState.SHIP) {
                    ShotResult res = fireCell(true, nr, nc);
                    if (res == ShotResult.HIT || res == ShotResult.SUNK) {
                        hits.add(new int[]{nr, nc});
                        Ship owner = findShipAt(enemyShips, nr, nc);
                        if (owner != null) {
                            lastType = owner.type;
                            if (res == ShotResult.SUNK && !newlySunk.contains(owner.type)) {
                                newlySunk.add(owner.type);
                            }
                        }
                    }
                    break;
                }
            }
        }
        ShotResult agg = !newlySunk.isEmpty() ? ShotResult.SUNK
                : (!hits.isEmpty() ? ShotResult.HIT : base.aggregateResult());
        return new StrikeOutcome(agg, List.copyOf(hits), base.missCells(),
                List.copyOf(newlySunk), lastType, base.burnCell(), base.sonarRevealedCells());
    }

    public int[] pickAiTarget() {
        CellState[][] grid = playerGrid;
        // First hunt orthogonal neighbors of unsunk HIT cells for lifelike AI pursuit
        List<int[]> followUps = new ArrayList<>();
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (grid[r][c] == CellState.HIT) {
                    Ship owner = findShipAt(playerShips, r, c);
                    if (owner != null && isShipSunk(grid, owner)) continue;
                    for (int[] d : dirs) {
                        int nr = r + d[0];
                        int nc = c + d[1];
                        if (inBounds(nr, nc) && isUnshot(grid[nr][nc])) {
                            followUps.add(new int[]{nr, nc});
                        }
                    }
                }
            }
        }
        if (!followUps.isEmpty()) {
            return followUps.get(random.nextInt(followUps.size()));
        }
        List<int[]> unshot = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (isUnshot(grid[r][c])) unshot.add(new int[]{r, c});
            }
        }
        if (unshot.isEmpty()) return null;
        return unshot.get(random.nextInt(unshot.size()));
    }

    public void forceSunkAll(boolean onEnemy) {
        CellState[][] grid = grid(onEnemy);
        List<Ship> fleet = fleet(onEnemy);
        Set<ShipType> sunk = sunkSet(onEnemy);
        for (Ship ship : fleet) {
            for (int i = 0; i < ship.type.length; i++) {
                int r = ship.row + (ship.horizontal ? 0 : i);
                int c = ship.col + (ship.horizontal ? i : 0);
                if (inBounds(r, c)) grid[r][c] = CellState.HIT;
            }
            sunk.add(ship.type);
        }
    }

    private int[] findBurnAdjacentCell(boolean onEnemy, List<int[]> hitCells) {
        CellState[][] grid = grid(onEnemy);
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] hit : hitCells) {
            for (int[] d : dirs) {
                int nr = hit[0] + d[0];
                int nc = hit[1] + d[1];
                if (inBounds(nr, nc) && grid[nr][nc] == CellState.SHIP) {
                    return new int[]{nr, nc};
                }
            }
        }
        return null;
    }

    private CellState[][] grid(boolean onEnemy) { return onEnemy ? enemyGrid : playerGrid; }
    private List<Ship> fleet(boolean onEnemy) { return onEnemy ? enemyShips : playerShips; }
    private Set<ShipType> sunkSet(boolean onEnemy) { return onEnemy ? sunkEnemyTypes : sunkPlayerTypes; }

    static void clearGrid(CellState[][] grid) {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                grid[r][c] = CellState.EMPTY;
            }
        }
    }

    static boolean inBounds(int r, int c) {
        return r >= 0 && r < Board.SIZE && c >= 0 && c < Board.SIZE;
    }

    static boolean isUnshot(CellState state) {
        return state == CellState.EMPTY || state == CellState.SHIP;
    }

    static boolean placeProvidedFleet(List<Ship> provided, CellState[][] grid, List<Ship> out) {
        if (provided == null || provided.size() != ShipType.values().length) return false;
        Set<ShipType> seen = EnumSet.noneOf(ShipType.class);
        for (Ship ship : provided) {
            if (ship == null || ship.type == null || !seen.add(ship.type)) return false;
            if (!canPlace(grid, ship.row, ship.col, ship.type.length, ship.horizontal)) return false;
            stampShip(grid, ship, out);
        }
        return true;
    }

    static void generateRandomFleet(CellState[][] grid, List<Ship> out, Random rng) {
        for (ShipType type : ShipType.values()) {
            boolean placed = false;
            for (int attempt = 0; attempt < 250 && !placed; attempt++) {
                boolean horizontal = rng.nextBoolean();
                int maxRow = horizontal ? Board.SIZE : Board.SIZE - type.length + 1;
                int maxCol = horizontal ? Board.SIZE - type.length + 1 : Board.SIZE;
                int r = rng.nextInt(Math.max(1, maxRow));
                int c = rng.nextInt(Math.max(1, maxCol));
                if (canPlace(grid, r, c, type.length, horizontal)) {
                    stampShip(grid, new Ship(type, r, c, horizontal), out);
                    placed = true;
                }
            }
            if (!placed) {
                outer:
                for (int r = 0; r < Board.SIZE; r++) {
                    for (int c = 0; c < Board.SIZE; c++) {
                        for (boolean h : new boolean[]{true, false}) {
                            if (canPlace(grid, r, c, type.length, h)) {
                                stampShip(grid, new Ship(type, r, c, h), out);
                                break outer;
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean canPlace(CellState[][] grid, int row, int col, int length, boolean horizontal) {
        for (int i = 0; i < length; i++) {
            int r = row + (horizontal ? 0 : i);
            int c = col + (horizontal ? i : 0);
            if (!inBounds(r, c) || grid[r][c] != CellState.EMPTY) return false;
        }
        return true;
    }

    private static void stampShip(CellState[][] grid, Ship ship, List<Ship> out) {
        for (int i = 0; i < ship.type.length; i++) {
            int r = ship.row + (ship.horizontal ? 0 : i);
            int c = ship.col + (ship.horizontal ? i : 0);
            grid[r][c] = CellState.SHIP;
        }
        out.add(ship);
    }

    static Ship findShipAt(List<Ship> fleet, int row, int col) {
        for (Ship ship : fleet) {
            for (int i = 0; i < ship.type.length; i++) {
                int r = ship.row + (ship.horizontal ? 0 : i);
                int c = ship.col + (ship.horizontal ? i : 0);
                if (r == row && c == col) return ship;
            }
        }
        return null;
    }

    static boolean isShipSunk(CellState[][] grid, Ship ship) {
        for (int i = 0; i < ship.type.length; i++) {
            int r = ship.row + (ship.horizontal ? 0 : i);
            int c = ship.col + (ship.horizontal ? i : 0);
            if (inBounds(r, c) && grid[r][c] != CellState.HIT) return false;
        }
        return true;
    }
}
