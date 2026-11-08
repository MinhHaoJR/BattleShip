package battleship.logic;

/** AI của máy: bắn ngẫu nhiên, trúng thì chuyển sang "săn" các ô xung quanh. */
public class ComputerPlayer {

    /** Chọn ô tiếp theo để bắn: trả về {row, col}, không bao giờ trùng ô đã bắn. */
    public int[] nextTarget() {
        throw new UnsupportedOperationException("TODO (logic): nextTarget");
    }

    /** Game gọi sau mỗi phát bắn của máy để AI cập nhật chế độ săn. */
    public void onResult(Shot shot) {
        throw new UnsupportedOperationException("TODO (logic): onResult");
    }
}
