# BattleShip (Java Swing)

Game Bắn Tàu: Người vs Máy, bàn 10×10, 5 tàu (5-4-3-3-2), bắn luân phiên.

## Chạy
```bash
mvn test          # chạy test
mvn package       # tạo jar
java -jar target/battleship-1.0-SNAPSHOT.jar
```
Hoặc mở thư mục bằng IntelliJ / NetBeans / VS Code (tự nhận Maven), chạy `battleship.Main`.

## Cấu trúc & phân công
| Gói | Nội dung | Người làm |
|---|---|---|
| `model/` | `ShipType`, `Ship`, `Board`, `ShotResult`, `CellState` | Hảo |
| `logic/` | `Game`, `ComputerPlayer`, `Shot` | Hảo |
| `ui/` + `Main` | `MainFrame`, `SetupPanel`, `GamePanel`, `BoardPanel` | Tâm |

**Hợp đồng:** chữ ký public của `Board` và `Game` là giao diện giữa logic và UI. Muốn đổi phải báo người kia.

**Phần logic đã xong** (nhánh `logic`). Những gì UI cần dùng:

| Gọi | Dùng khi |
|---|---|
| `board.placeShip(type, row, col, horizontal)` | Đặt tàu ở màn chuẩn bị. Trả `false` nếu đặt sai chỗ |
| `board.placeRandomly()` / `board.clear()` | Nút "Ngẫu nhiên" / "Đặt lại" |
| `board.hasShip(type)`, `board.allShipsPlaced()` | Làm mờ tàu đã đặt; chỉ bật nút "Bắt đầu" khi đủ 5 tàu |
| `board.getCell(row, col)` | Vẽ ô. **Bàn của máy: vẽ `SHIP` như `EMPTY`** để không lộ tàu |
| `new Game(playerBoard)` | Bắt đầu ván (ném lỗi nếu chưa đủ tàu) |
| `game.playerFire(row, col)` | Người bấm ô bên bàn máy. `ALREADY_SHOT` thì vẫn giữ lượt |
| `game.computerTurn()` | Gọi ngay sau lượt người (có thể dùng `javax.swing.Timer` cho chậm lại); trả về `Shot` để vẽ |
| `game.isPlayerTurn()`, `game.isOver()`, `game.playerWon()` | Khóa bàn khi chưa tới lượt, hiện màn thắng/thua |

AI: tìm kiểu ô bàn cờ (chỉ bắn ô `row + col` chẵn), trúng thì săn 4 ô xung quanh. Mô phỏng 10.000 ván: trung bình **64 phát** để đánh chìm hết tàu, so với khoảng 95 phát nếu bắn hoàn toàn ngẫu nhiên.

## Tiến độ
| Tuần | Hảo (Logic + AI) | Tâm (UI) |
|---|---|---|
| 1 | `Board`: đặt tàu, bắn, `allSunk`, `placeRandomly`. Làm cho `BoardTest` qua hết | `BoardPanel` vẽ lưới 10×10 từ `getCell`, `MainFrame` |
| 2 | `Game` (lượt, thắng/thua), `ComputerPlayer` (ngẫu nhiên + săn) + test | `SetupPanel` (đặt/xoay/ngẫu nhiên), `GamePanel` (2 bàn, click bắn) |
| 3 | Ghép code. AI checkerboard, sửa lỗi | Ghép code. Màn thắng/thua, chơi lại, thông báo; âm thanh nếu kịp |
| 4 | Kiểm thử, báo cáo: thiết kế & thuật toán | Kiểm thử, báo cáo: giao diện & HDSD, slide |

## Git
- `main` luôn chạy được. Làm trên nhánh `logic` / `ui`, gộp qua Pull Request, người kia review.
- Hảo chỉ sửa `model/`, `logic/`; Tâm chỉ sửa `ui/`, `Main.java`.
