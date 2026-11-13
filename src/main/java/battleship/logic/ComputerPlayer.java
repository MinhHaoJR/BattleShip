package battleship.logic;

import battleship.model.Board;
import battleship.model.ShotResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * AI của máy, 2 chế độ:
 * - Tìm: bắn ngẫu nhiên các ô (row + col) chẵn, kiểu bàn cờ vua. Tàu ngắn nhất dài 2 nên tàu nào cũng chạm ít nhất 1 ô chẵn.
 * - Săn: bắn trúng thì đẩy 4 ô xung quanh vào ngăn xếp, bắn lần lượt cho đến khi tàu chìm.
 */
public class ComputerPlayer {
    private static final int[][] NEIGHBORS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private final boolean[][] tried = new boolean[Board.SIZE][Board.SIZE];
    private final Deque<int[]> targets = new ArrayDeque<>();
    private final Random random;

    public ComputerPlayer() {
        this(new Random());
    }

    /** Cho test truyền Random có seed cố định. */
    public ComputerPlayer(Random random) {
        this.random = random;
    }

    /** Chọn ô tiếp theo để bắn: trả về {row, col}, không bao giờ trùng ô đã bắn. */
    public int[] nextTarget() {
        while (!targets.isEmpty()) {
            int[] t = targets.pop();
            if (!tried[t[0]][t[1]]) return mark(t);
        }
        List<int[]> even = new ArrayList<>(), all = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (tried[r][c]) continue;
                all.add(new int[]{r, c});
                if ((r + c) % 2 == 0) even.add(new int[]{r, c});
            }
        }
        List<int[]> pool = even.isEmpty() ? all : even;
        if (pool.isEmpty()) throw new IllegalStateException("Đã bắn hết bàn cờ");
        return mark(pool.get(random.nextInt(pool.size())));
    }

    /** Game gọi sau mỗi phát bắn của máy để AI cập nhật chế độ săn. */
    public void onResult(Shot shot) {
        if (shot.result() == ShotResult.HIT) {
            for (int[] d : NEIGHBORS) {
                int r = shot.row() + d[0], c = shot.col() + d[1];
                if (Board.inBounds(r, c) && !tried[r][c]) targets.push(new int[]{r, c});
            }
        } else if (shot.result() == ShotResult.SUNK) {
            // shortcut: quên luôn các ô trúng của tàu khác nằm sát tàu vừa chìm; chế độ tìm vẫn sẽ bắn ra chúng, chỉ tốn thêm vài phát
            targets.clear();
        }
    }

    private int[] mark(int[] t) {
        tried[t[0]][t[1]] = true;
        return t;
    }
}
