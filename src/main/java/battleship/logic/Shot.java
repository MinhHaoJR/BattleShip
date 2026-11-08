package battleship.logic;

import battleship.model.ShotResult;

/** Một phát bắn: ô nào và kết quả ra sao. */
public record Shot(int row, int col, ShotResult result) {
}
