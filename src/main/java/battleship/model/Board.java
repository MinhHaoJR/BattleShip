package battleship.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Bàn cờ 10x10 của một người chơi.
 * Chữ ký các phương thức public là "hợp đồng" giữa logic và UI: muốn đổi phải báo cả nhóm.
 */
public class Board {
    public static final int SIZE = 10;

    private static final Random RANDOM = new Random();

    private final CellState[][] cells = new CellState[SIZE][SIZE];
    private final Ship[][] shipAt = new Ship[SIZE][SIZE];
    private final List<Ship> ships = new ArrayList<>();

    public Board() {
        clear();
    }

    /**
     * Trạng thái ô để UI vẽ.
     * Lưu ý UI: bàn của máy cũng trả SHIP ở ô có tàu, UI phải vẽ ô đó như EMPTY để không lộ tàu.
     */
    public CellState getCell(int row, int col) {
        return cells[row][col];
    }

    /** Xóa hết tàu và các phát bắn. */
    public void clear() {
        for (CellState[] row : cells) Arrays.fill(row, CellState.EMPTY);
        for (Ship[] row : shipAt) Arrays.fill(row, null);
        ships.clear();
    }

    /** Đặt tàu. Trả false nếu ra ngoài bàn, chồng tàu khác, hoặc loại tàu này đã đặt rồi. */
    public boolean placeShip(ShipType type, int row, int col, boolean horizontal) {
        if (hasShip(type)) return false;
        int dr = horizontal ? 0 : 1, dc = horizontal ? 1 : 0;
        int endRow = row + dr * (type.length - 1), endCol = col + dc * (type.length - 1);
        if (!inBounds(row, col) || !inBounds(endRow, endCol)) return false;
        for (int i = 0; i < type.length; i++) {
            if (shipAt[row + dr * i][col + dc * i] != null) return false;
        }
        Ship ship = new Ship(type, row, col, horizontal);
        for (int i = 0; i < type.length; i++) {
            shipAt[row + dr * i][col + dc * i] = ship;
            cells[row + dr * i][col + dc * i] = CellState.SHIP;
        }
        ships.add(ship);
        return true;
    }

    /** Xóa hết tàu rồi đặt ngẫu nhiên đủ 5 tàu trong ShipType. */
    public void placeRandomly() {
        clear();
        for (ShipType type : ShipType.values()) {
            // luôn tìm được chỗ: 17 ô tàu trên bàn 100 ô
            while (!placeShip(type, RANDOM.nextInt(SIZE), RANDOM.nextInt(SIZE), RANDOM.nextBoolean())) { }
        }
    }

    /** Bắn vào ô (row, col). Ô ngoài bàn sẽ ném ArrayIndexOutOfBoundsException. */
    public ShotResult fire(int row, int col) {
        switch (cells[row][col]) {
            case HIT, MISS:
                return ShotResult.ALREADY_SHOT;
            case EMPTY:
                cells[row][col] = CellState.MISS;
                return ShotResult.MISS;
            default:
                cells[row][col] = CellState.HIT;
                Ship ship = shipAt[row][col];
                ship.hit();
                return ship.isSunk() ? ShotResult.SUNK : ShotResult.HIT;
        }
    }

    /** True khi mọi tàu đã đặt đều bị chìm. */
    public boolean allSunk() {
        return ships.stream().allMatch(Ship::isSunk);
    }

    /** Loại tàu này đã được đặt chưa (UI dùng khi đặt tàu). */
    public boolean hasShip(ShipType type) {
        return ships.stream().anyMatch(s -> s.type == type);
    }

    /** Đã đặt đủ 5 tàu chưa: UI chỉ cho bấm "Bắt đầu" khi true. */
    public boolean allShipsPlaced() {
        return ships.size() == ShipType.values().length;
    }

    public static boolean inBounds(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }
}
