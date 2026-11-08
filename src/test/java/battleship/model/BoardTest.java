package battleship.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Đặc tả hành vi của Board. Người làm logic viết code cho đến khi các test này qua. */
class BoardTest {

    @Test
    void newBoardIsEmpty() {
        Board b = new Board();
        assertEquals(CellState.EMPTY, b.getCell(0, 0));
        assertEquals(CellState.EMPTY, b.getCell(9, 9));
    }

    @Test
    void rejectsShipOutsideBoard() {
        Board b = new Board();
        assertFalse(b.placeShip(ShipType.CARRIER, 0, 6, true)); // cột 6..10, vượt cạnh phải
        assertFalse(b.placeShip(ShipType.CARRIER, 6, 0, false));
        assertFalse(b.placeShip(ShipType.DESTROYER, -1, 0, true));
    }

    @Test
    void rejectsOverlappingShips() {
        Board b = new Board();
        assertTrue(b.placeShip(ShipType.CARRIER, 0, 0, true));
        assertFalse(b.placeShip(ShipType.DESTROYER, 0, 4, false));
        assertEquals(CellState.SHIP, b.getCell(0, 4));
    }

    @Test
    void fireMissHitSunkAndRepeat() {
        Board b = new Board();
        b.placeShip(ShipType.DESTROYER, 2, 2, true); // ô (2,2) và (2,3)
        assertEquals(ShotResult.MISS, b.fire(5, 5));
        assertEquals(CellState.MISS, b.getCell(5, 5));
        assertEquals(ShotResult.HIT, b.fire(2, 2));
        assertEquals(ShotResult.ALREADY_SHOT, b.fire(2, 2));
        assertFalse(b.allSunk());
        assertEquals(ShotResult.SUNK, b.fire(2, 3));
        assertTrue(b.allSunk());
    }

    @Test
    void placeRandomlyPlacesAllShips() {
        Board b = new Board();
        b.placeRandomly();
        int shipCells = 0;
        for (int r = 0; r < Board.SIZE; r++)
            for (int c = 0; c < Board.SIZE; c++)
                if (b.getCell(r, c) == CellState.SHIP) shipCells++;
        assertEquals(5 + 4 + 3 + 3 + 2, shipCells);
    }
}
