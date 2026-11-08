package battleship.logic;

import battleship.model.Board;
import battleship.model.ShotResult;

/** Một ván Người vs Máy. UI chỉ nói chuyện với lớp này và 2 Board. */
public class Game {
    public final Board playerBoard;
    public final Board computerBoard;
    private final ComputerPlayer computer = new ComputerPlayer();

    /** playerBoard đã được người chơi đặt đủ tàu; bàn của máy tự đặt ngẫu nhiên. */
    public Game(Board playerBoard) {
        this.playerBoard = playerBoard;
        this.computerBoard = new Board();
        // TODO (logic): computerBoard.placeRandomly();
    }

    /** Người chơi bắn vào bàn máy. */
    public ShotResult playerFire(int row, int col) {
        throw new UnsupportedOperationException("TODO (logic): playerFire");
    }

    /** Máy bắn một phát vào bàn người chơi. */
    public Shot computerTurn() {
        throw new UnsupportedOperationException("TODO (logic): computerTurn");
    }

    public boolean isOver() {
        throw new UnsupportedOperationException("TODO (logic): isOver");
    }

    public boolean playerWon() {
        throw new UnsupportedOperationException("TODO (logic): playerWon");
    }
}
