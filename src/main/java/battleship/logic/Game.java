package battleship.logic;

import battleship.model.Board;
import battleship.model.ShotResult;

import java.util.List;

/**
 * Một ván Người vs Máy. UI chỉ nói chuyện với lớp này và 2 Board.
 * Luật lượt: người bắn trước, sau đó luân phiên. Bắn lại ô cũ (ALREADY_SHOT) không mất lượt.
 */
public class Game {
    public final Board playerBoard;
    public final Board computerBoard;
    private final ComputerPlayer computer;
    private boolean playerTurn = true;

    /** playerBoard phải đã đặt đủ tàu; bàn của máy tự đặt ngẫu nhiên. */
    public Game(Board playerBoard) {
        this(playerBoard, new ComputerPlayer());
    }

    /** Cho test truyền AI có seed cố định. */
    public Game(Board playerBoard, ComputerPlayer computer) {
        if (!playerBoard.allShipsPlaced()) throw new IllegalArgumentException("Người chơi chưa đặt đủ tàu");
        this.playerBoard = playerBoard;
        this.computerBoard = new Board();
        this.computerBoard.placeRandomly();
        this.computer = computer;
    }

    /** Người chơi bắn vào bàn máy. */
    public ShotResult playerFire(int row, int col) {
        return useCombo(List.of(new int[]{row, col}));
    }

    /**
     * Người chơi bắn nhiều ô trong một lượt (vũ khí combo). Ô đã bắn rồi được bỏ qua.
     * Trả kết quả mạnh nhất (SUNK > HIT > MISS); ALREADY_SHOT nếu mọi ô đều đã bắn, khi đó không mất lượt.
     */
    public ShotResult useCombo(List<int[]> cells) {
        if (isOver() || !playerTurn) throw new IllegalStateException("Chưa tới lượt người chơi");
        ShotResult best = ShotResult.ALREADY_SHOT;
        for (int[] cell : cells) {
            ShotResult r = computerBoard.fire(cell[0], cell[1]);
            if (r != ShotResult.ALREADY_SHOT && (best == ShotResult.ALREADY_SHOT || r.compareTo(best) > 0)) best = r;
        }
        if (best != ShotResult.ALREADY_SHOT) playerTurn = false;
        return best;
    }

    /** Máy bắn một phát vào bàn người chơi. */
    public Shot computerTurn() {
        if (isOver() || playerTurn) throw new IllegalStateException("Chưa tới lượt máy");
        int[] t = computer.nextTarget();
        Shot shot = new Shot(t[0], t[1], playerBoard.fire(t[0], t[1]));
        computer.onResult(shot);
        playerTurn = true;
        return shot;
    }

    public boolean isPlayerTurn() {
        return playerTurn;
    }

    public boolean isOver() {
        return playerBoard.allSunk() || computerBoard.allSunk();
    }

    public boolean playerWon() {
        return computerBoard.allSunk();
    }
}
