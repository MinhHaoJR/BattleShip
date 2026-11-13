package battleship.logic;

import battleship.model.Board;
import battleship.model.ShipType;
import battleship.model.ShotResult;
import org.junit.jupiter.api.Test;

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
