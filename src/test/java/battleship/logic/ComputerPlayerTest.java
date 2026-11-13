package battleship.logic;

import battleship.model.Board;
import battleship.model.ShotResult;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ComputerPlayerTest {

    @Test
    void neverRepeatsAndCoversWholeBoard() {
        ComputerPlayer ai = new ComputerPlayer(new Random(1));
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < Board.SIZE * Board.SIZE; i++) {
            int[] t = ai.nextTarget();
            assertTrue(seen.add(t[0] * Board.SIZE + t[1]), "trùng ô " + t[0] + "," + t[1]);
        }
        assertThrows(IllegalStateException.class, ai::nextTarget);
    }

    @Test
    void searchesEvenCellsFirst() {
        ComputerPlayer ai = new ComputerPlayer(new Random(2));
        for (int i = 0; i < 50; i++) {
            int[] t = ai.nextTarget();
            assertEquals(0, (t[0] + t[1]) % 2);
        }
    }

    @Test
    void huntsNeighboursAfterHit() {
        ComputerPlayer ai = new ComputerPlayer(new Random(3));
        int[] first = ai.nextTarget();
        ai.onResult(new Shot(first[0], first[1], ShotResult.HIT));
        int[] next = ai.nextTarget();
        assertEquals(1, Math.abs(next[0] - first[0]) + Math.abs(next[1] - first[1]));
    }

    @Test
    void stopsHuntingAfterSunk() {
        ComputerPlayer ai = new ComputerPlayer(new Random(4));
        int[] first = ai.nextTarget();
        ai.onResult(new Shot(first[0], first[1], ShotResult.HIT));
        ai.onResult(new Shot(first[0], first[1], ShotResult.SUNK));
        int[] next = ai.nextTarget();
        assertEquals(0, (next[0] + next[1]) % 2); // quay về chế độ tìm
    }
}
