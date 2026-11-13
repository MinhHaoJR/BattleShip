package battleship.model;

/** Một con tàu đã đặt trên bàn cờ. */
public class Ship {
    public final ShipType type;
    public final int row;
    public final int col;
    public final boolean horizontal;
    private int hits;

    public Ship(ShipType type, int row, int col, boolean horizontal) {
        this.type = type;
        this.row = row;
        this.col = col;
        this.horizontal = horizontal;
    }

    void hit() {
        hits++;
    }

    public boolean isSunk() {
        return hits >= type.length;
    }
}
