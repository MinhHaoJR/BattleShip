package battleship.logic;

import battleship.model.Board;
import battleship.model.ShotResult;

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
        if (isOver() || !playerTurn) throw new IllegalStateException("Chưa tới lượt người chơi");
        ShotResult result = computerBoard.fire(row, col);
        if (result != ShotResult.ALREADY_SHOT) playerTurn = false;
        return result;
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
