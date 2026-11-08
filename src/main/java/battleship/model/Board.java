package battleship.model;

/**
 * Bàn cờ 10x10 của một người chơi.
 * Chữ ký các phương thức public là "hợp đồng" giữa logic và UI: muốn đổi phải báo cả nhóm.
 */
public class Board {
    public static final int SIZE = 10;

    private final CellState[][] cells = new CellState[SIZE][SIZE];

    public Board() {
        for (CellState[] row : cells) java.util.Arrays.fill(row, CellState.EMPTY);
    }

    /** Trạng thái ô để UI vẽ. Đã chạy được ngay để UI làm song song. */
    public CellState getCell(int row, int col) {
        return cells[row][col];
    }

    /** Đặt tàu. Trả false nếu ra ngoài bàn, chồng tàu khác, hoặc loại tàu này đã đặt rồi. */
    public boolean placeShip(ShipType type, int row, int col, boolean horizontal) {
        throw new UnsupportedOperationException("TODO (logic): placeShip");
    }

    /** Xóa hết tàu rồi đặt ngẫu nhiên đủ 5 tàu trong ShipType. */
    public void placeRandomly() {
        throw new UnsupportedOperationException("TODO (logic): placeRandomly");
    }

    /** Bắn vào ô (row, col). */
    public ShotResult fire(int row, int col) {
        throw new UnsupportedOperationException("TODO (logic): fire");
    }

    /** True khi mọi tàu đã đặt đều bị chìm. */
    public boolean allSunk() {
        throw new UnsupportedOperationException("TODO (logic): allSunk");
    }
}
