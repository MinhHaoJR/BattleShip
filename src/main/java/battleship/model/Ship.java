package battleship.model;

/** Một con tàu đã đặt trên bàn cờ. Người làm logic tự thêm trường/phương thức nếu cần. */
public class Ship {
    public final ShipType type;
    public final int row;
    public final int col;
    public final boolean horizontal;

    public Ship(ShipType type, int row, int col, boolean horizontal) {
        this.type = type;
        this.row = row;
        this.col = col;
        this.horizontal = horizontal;
    }
}
