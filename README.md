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
`Board.getCell()` đã chạy được, nên UI có thể vẽ bàn cờ ngay khi logic chưa xong.

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
