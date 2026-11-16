package battleship.logic;

import battleship.model.Board;
import battleship.model.CellState;
import battleship.model.ShipType;
import battleship.model.ShotResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    private static Board readyBoard() {
        Board b = new Board();
        b.placeRandomly();
        return b;
    }

    @Test
    void rejectsBoardWithoutAllShips() {
        Board b = new Board();
        b.placeShip(ShipType.CARRIER, 0, 0, true);
        assertThrows(IllegalArgumentException.class, () -> new Game(b));
    }

    @Test
    void turnsAlternate() {
        Game g = new Game(readyBoard());
        assertTrue(g.isPlayerTurn());
        g.playerFire(0, 0);
        assertFalse(g.isPlayerTurn());
        assertThrows(IllegalStateException.class, () -> g.playerFire(0, 1));
        g.computerTurn();
        assertTrue(g.isPlayerTurn());
        assertThrows(IllegalStateException.class, g::computerTurn);
    }

    @Test
    void repeatedShotKeepsTurn() {
        Game g = new Game(readyBoard());
        g.playerFire(0, 0);
        g.computerTurn();
        assertEquals(ShotResult.ALREADY_SHOT, g.playerFire(0, 0));
        assertTrue(g.isPlayerTurn());
    }

    @Test
    void comboFiresAllCellsAndReturnsStrongestResult() {
        Game g = new Game(readyBoard());
        // bắn cả hàng 0 bằng 1 combo: mọi ô phải bị bắn, kết quả là mạnh nhất trong hàng
        List<int[]> row0 = new ArrayList<>();
        for (int c = 0; c < Board.SIZE; c++) row0.add(new int[]{0, c});
        ShotResult expected = ShotResult.MISS;
        for (int c = 0; c < Board.SIZE; c++) {
            if (g.computerBoard.getCell(0, c) == CellState.SHIP) expected = ShotResult.HIT;
        }
        ShotResult result = g.useCombo(row0);
        if (expected == ShotResult.HIT) assertTrue(result == ShotResult.HIT || result == ShotResult.SUNK);
        else assertEquals(ShotResult.MISS, result);
        for (int c = 0; c < Board.SIZE; c++) {
            CellState s = g.computerBoard.getCell(0, c);
            assertTrue(s == CellState.HIT || s == CellState.MISS);
        }
        assertFalse(g.isPlayerTurn());
    }

    @Test
    void comboOnAlreadyShotCellsKeepsTurn() {
        Game g = new Game(readyBoard());
        g.playerFire(0, 0);
        g.computerTurn();
        assertEquals(ShotResult.ALREADY_SHOT, g.useCombo(List.of(new int[]{0, 0})));
        assertEquals(ShotResult.ALREADY_SHOT, g.useCombo(List.of()));
        assertTrue(g.isPlayerTurn());
    }

    @Test
    void comboReportsSunk() {
        Game g = new Game(readyBoard());
        // tìm 1 tàu của máy rồi bắn trọn nó trong một combo
        List<int[]> shipCells = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++)
            for (int c = 0; c < Board.SIZE; c++)
                if (g.computerBoard.getCell(r, c) == CellState.SHIP) shipCells.add(new int[]{r, c});
        assertEquals(ShotResult.SUNK, g.useCombo(shipCells));
        assertTrue(g.isOver());
        assertTrue(g.playerWon());
    }

    @Test
    void fullGameEndsWithAWinner() {
        Game g = new Game(readyBoard(), new ComputerPlayer(new Random(5)));
        for (int i = 0; i < Board.SIZE * Board.SIZE && !g.isOver(); i++) {
            g.playerFire(i / Board.SIZE, i % Board.SIZE);
            if (!g.isOver()) g.computerTurn();
        }
        assertTrue(g.isOver());
        assertEquals(g.computerBoard.allSunk(), g.playerWon());
        assertThrows(IllegalStateException.class, () -> g.playerFire(0, 0));
    }
}
