# Kế hoạch: làm lại Story Mode theo cốt truyện mới

> Cốt truyện: [cot-truyen.md](cot-truyen.md) · Mẫu boss: [boss-template.md](boss-template.md)
> Nhánh làm việc: `story-mode-moi`

## 1. Mục tiêu

1. Thay toàn bộ nội dung truyện trong `StoryMode.java` bằng cốt truyện mới "Biển Hạch Tâm" (6 hồi + chế độ Zombie).
2. Hình truyện vẫn là **tranh comic phong cách anime** giống hình cũ. Bỏ các hình có Godzilla, Ghidorah, Mothra khỏi truyện.
3. Khi chiếu truyện: **hình phủ toàn màn hình**, lời thoại nằm đè lên đáy hình, **nút "Bỏ qua" ở góc trên bên phải**.

**Không làm trong đợt này:** đổi luật chơi của các màn, làm boss mới trong chế độ Titan (việc đó theo `boss-template.md`), lồng tiếng.

## 2. Hiện trạng (đã kiểm tra trong code)

| Hạng mục | Hiện tại |
|---|---|
| Số đoạn truyện | 26 đoạn mở màn (màn 1, 6, mỗi màn boss 5/10/15/20 của mỗi chương, màn đầu mỗi chương) + 5 đoạn kết chương (20, 40, 60, 80, 100) |
| Nơi chứa nội dung | `StoryMode.java` (khoảng 1.040 dòng): 7 màn viết tay riêng (1, 6, 21, 41, 61, 81, 100), các màn còn lại sinh từ mẫu chung (`buildBossMilestoneIntro`, `buildRegularStageEpisode`) |
| Cách hiển thị | `StoryComicOverlay`: dạng **thẻ** gồm tiêu đề, dải hình nhỏ, khung tranh, hộp thoại. Đã có nút **"Bỏ Qua (ESC)"**, Tự chạy, Trước/Sau, phím Space/Enter/←/→/ESC |
| Vùng hiển thị | Gắn **trong** `GamePanel` (vùng bàn cờ) và `StageSelectPanel`, **không** phủ thanh tiêu đề của cửa sổ |
| Hình | 16 tranh 960×540 JPG + 3 chân dung 320×320, nằm trong `resources/story/`. Đường dẫn khai báo trong `SpriteAssets.storyArtPath` |
| Thiếu hình | `getStoryArt` trả `null`, màn truyện vẫn chạy nhưng không có tranh (không bị crash) |
| Boss trong các màn boss | `CampaignMode` (dòng 424) xoay vòng Godzilla → Ghidorah → Mothra; màn 100 luôn là Ghidorah |

## 3. Các phần việc

### Phần A: Nội dung truyện (`StoryMode.java`)
- **Giữ nguyên** các hàm public mà file khác đang gọi: `hasAutoIntroCutscene`, `hasPostVictoryEpilogue`, `chapterIntroEpisode`, `introEpisodeForStage`, `epilogueEpisodeForStage`, `artForChapter`, `moodForChapter`, và các kiểu `StoryEpisode`, `StoryFrame`, `MotionStyle`, `ParticleMood`. Nhờ vậy `GamePanel` và `StageSelectPanel` không phải sửa phần gọi truyện.
- **Nhân vật (`StorySpeaker`):** giữ `KAI`, `LYRA`, `VEGA`, `NARRATOR`, `MEMORY`, `TITAN`; **thay `MOTHRA` bằng `LUMIRA`**; **thêm `RASKOV`**.
- **Viết tay 12 đoạn chính**, các màn khác vẫn sinh từ mẫu nhưng đổi câu chữ theo chương:

| Màn | Đoạn truyện | Hồi |
|---|---|---|
| 1 | Hải chiến Tân Hải và Vel'Kar bùng nổ | 0 |
| 6 | Lyra nghe tín hiệu lạ dưới Rãnh Vực Sâu | 0 |
| 20 *(kết)* | Raskov thả bom "Mặt Trời Đen" | 0 |
| 21 | Godzilla thức tỉnh | 1 |
| 40 *(kết)* | Hai phe ngừng bắn; Vega: "tế bào này không chết" | 1 |
| 41 | Thiên thạch rơi, King Ghidorah xuất hiện | 2 |
| 50 | Godzilla đánh King Ghidorah, tế bào văng lên vũ trụ | 2 |
| 60 *(kết)* | SpaceGodzilla ra đời; Vega đề xuất Lõi Phân Rã Ω | 3 |
| 61 | Nghĩa địa tàu chìm sống lại (Zombie) | Zombie |
| 70 | Trận AURORA-07; Lyra gửi mật mã cho anh trai | Zombie |
| 80 *(kết)* | Destoroyah bị diệt; Godzilla nuốt năng lượng rồi biến mất | 4 |
| 81 | Liên minh hai nước, Mothra xuất hiện | 5 |
| 100 + kết | Godzilla Ultima 4 giai đoạn; Mothra ru nó ngủ; ký hiệp ước | 5 |

- **Thứ tự chương:** cốt truyện cần thứ tự Hải chiến → Tận thế → Ngân hà → Atlantis → Thần giới, còn `CampaignChapter` đang là Hải chiến → Thần giới → Tận thế → Ngân hà → Atlantis. **Cần quyết định (mục 6, câu 1).**

### Phần B: Ghép boss vào màn boss (`CampaignMode`)
Boss mới chưa có, nên tạm dùng boss cũ "đóng thế", theo đúng gợi ý trong `boss-template.md`:

| Chương | Boss trong truyện | Boss cũ đóng thế |
|---|---|---|
| Hải chiến | Soái hạm Raskov | Không có Titan, chỉ là màn hải chiến khó |
| Tận thế | Godzilla | `GODZILLA` |
| Ngân hà | King Ghidorah, SpaceGodzilla | `GHIDORAH`, `GODZILLA` |
| Atlantis | AURORA-07, Destoroyah | Màn Zombie, `MOTHRA` |
| Thần giới | Godzilla Ultima | `GODZILLA` ở độ khó Tuyệt Diệt |

Chỉ sửa cách chọn boss ở dòng 424 thành một bảng theo chương. Khi có boss mới thì thay tên trong bảng là xong.

### Phần C: Hiển thị toàn màn hình (`StoryComicOverlay` + nơi gắn)
1. **Gắn lên toàn cửa sổ:** đưa `StoryComicOverlay` lên `MainFrame.getLayeredPane()` ở lớp `POPUP_LAYER`, kích thước bằng cả cửa sổ. `GamePanel` và `StageSelectPanel` gọi chung một overlay này thay vì mỗi nơi tự tạo một cái.
2. **Bố cục mới:**
```
┌──────────────────────────────────────────────┐
│ CHƯƠNG II · TẬP 1               [Bỏ qua ▸▸] │ ← chữ nhỏ trên nền mờ
│                                              │
│          TRANH COMIC PHỦ KÍN MÀN HÌNH        │ ← tỉ lệ "cover", hiệu ứng Ken Burns giữ nguyên
│                                              │
│ ▓▓▓▓▓▓▓▓▓▓▓ dải tối mờ dần ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
│ [chân dung] TÊN NHÂN VẬT                     │
│             Lời thoại chạy từng chữ…   ● ● ○ │ ← chấm = số khung tranh
└──────────────────────────────────────────────┘
```
3. **Bỏ** dải hình nhỏ và khung thẻ. **Giữ** hiệu ứng hạt, rung màn hình, chữ chạy, Tự chạy, các phím tắt.
4. **Nút "Bỏ qua"** luôn hiện ở góc trên phải, đủ lớn để bấm (tối thiểu 44×32 px), có viền tương phản để đọc được trên mọi tranh. ESC vẫn bỏ qua được.
5. **Bấm vào bất kỳ đâu trên tranh** để sang khung tiếp theo (nếu chữ đang chạy thì hiện hết chữ trước).

### Phần D: Hình comic mới
**Quy cách:** JPG **1920×1080** (to hơn hình cũ vì giờ phủ toàn màn hình), dung lượng dưới 400 KB, đặt trong `resources/story/` và khai báo trong `SpriteAssets.storyArtPath`.
**Vùng an toàn:** cửa sổ game gần với tỉ lệ 4:3 (1160×900), nên tranh 16:9 sẽ bị **cắt hai bên**. Nhân vật và quái vật phải nằm trong **khoảng 1440 px ở giữa**.
**Phong cách (từ hình cũ):** anime/comic, nét mực đen đậm, tô màu cel, màu bão hòa, ánh sáng phát quang mạnh (xanh lam, cam lửa, tím), bầu trời và sóng biển nhiều chi tiết.

**Trạng thái:** đã có đủ 20 ảnh trong `resources/story/` (render 3D tạm bằng Blender). 16 ảnh cảnh cũ đã xoá; chỉ giữ 3 chân dung Kai, Lyra, Vega. Muốn thay ảnh nào thì **ghi đè đúng tên file**, không cần sửa code (`SpriteAssets.storyArtPath` đọc thẳng `/story/<key>.jpg`). Ảnh thay vào phải là ảnh nhóm tự vẽ hoặc có quyền sử dụng.

**20 khung hình, 4 ảnh mỗi chương** (mỗi chương chỉ dùng 4 ảnh của mình; `StoryMode.chapterArts`):

| Chương | File `story_<key>.jpg` | Nội dung khung hình | Hiện ở màn (truyện mở màn / kết chương; màn thường chỉ xoay vòng các ảnh đã xuất hiện, theo thứ tự 1→4 của chương — `StoryMode.ART_UNLOCK`) |
|---|---|---|---|
| I · Hải chiến | `war_clash` | Hoàng hôn đỏ, hai hạm đội bắn nhau. Bên trái: tàu Tân Hải xám xanh, đèn mạn xanh lam. Bên phải: tàu Vel'Kar đỏ đen. Vệt đạn và chớp nòng giữa hai bên | 1, 6 |
| | `raskov_flagship` | Soái hạm Vel'Kar khổng lồ, góc thấp từ mũi tàu, giữa bão sấm tím, pháo chính khai hỏa. Có thể thấy bóng Raskov trên đài chỉ huy | 1, kết 20 |
| | `ghost_fleet` | Đêm, sương xanh lục. Xác tàu của cả hai phe trồi lên, nghiêng ngả, phủ bào tử phát sáng, đèn mạn vẫn sáng | 6 (và chân dung "Tiếng Vọng") |
| | `black_sun` | Bom "Mặt Trời Đen" nổ dưới biển: cột nước phát sáng xanh lam cao hàng trăm mét, vòng sóng xung kích, tàu Raskov nghiêng ở tiền cảnh | kết 20 |
| II · Mặt Trời Đen | `orykron_rise` | Godzilla đứng dậy giữa biển lửa, gầm lên. Thân đen, khe vảy nứt phát sáng xanh lam, gai lưng sáng. Tàu Tân Hải nhỏ bé bắn lên | 21, 25, 30, 35 |
| | `orykron_beam` | Godzilla phun tia nguyên tử xanh lam cắt ngang hàng tàu hai phe, đêm xanh thẫm | 21, 25, 30, 40 |
| | `orykron_feed` | Godzilla ngửa cổ dưới mây nấm cam, dòng hạt phóng xạ xanh chảy vào người nó | 35, 40, kết 40 |
| | `orykron_sink` | Mưa tro xám. Godzilla chìm vào xoáy nước phát sáng, hai hạm đội hai bên hạ cờ đứng nhìn | kết 40 |
| III · Thiên thạch | `meteor_fall` | Đêm sao, thiên thạch tinh thể tím lao xuống biển kéo vệt sáng dài, trong lõi nứt thấy ba cái đầu của King Ghidorah | 41, 45, 60 |
| | `vesperax` | King Ghidorah, rồng vàng ba đầu, dang đôi cánh lớn bay trên biển đêm, phun sấm sét vàng xuống hạm đội | 41, 45, 50, 55, 60 |
| | `titan_clash` | Godzilla (trái) bắn tia xanh vào King Ghidorah (phải), vảy vàng tóe lửa. Tàu nhỏ dưới sóng lớn | 50, 55, kết 60 |
| | `orykron_sigma` | SpaceGodzilla ngoài vũ trụ giữa vành đai thiên thạch. Thân tím đen, hai cột tinh thể lớn trên vai, hành tinh biển xanh phía dưới | 60, kết 60 |
| IV · Nghĩa địa tàu | `omega_core` | Tia Oxygen Destroyer trắng giáng từ quỹ đạo. Dưới nghĩa địa tàu chìm, bầy giáp xác đỏ kết lại thành Destoroyah non | 61, 65, 75, 80 |
| | `zombie_fleet` | Hạm đội xác sống trồi lên trong sương đỏ, Destoroyah dang cánh lừng lững phía sau | 61, 65, 80 |
| | `aurora07` | Mẫu hạm AURORA-07 ngóc mũi phá mặt nước, số hiệu rõ trên thân. Tàu nhỏ của Lyra nháy đèn Morse, mẫu hạm nháy đáp | 70 |
| | `necrosys` | Destoroyah: quỷ giáp xác đỏ sẫm, cánh dơi lớn, sừng trên đầu, lõi ngực vàng rực, đứng trên nghĩa địa tàu | 75, 80, kết 80 |
| V · Kết Giới Mothra | `lumira` | Mothra dang đôi cánh ánh sáng khổng lồ trên biển đêm, rắc phấn sáng, vòng kết giới quanh tàu hai phe. Giai đoạn I của Godzilla (ấu long) lượn dưới mặt nước | 81, 85, 90, 95, kết 100 |
| | `ultima_awaken` | Giai đoạn II: Godzilla bốn chân vảy cam đỏ, gai lưng lớn, bò lên bãi đá ngầm. Vòng kết giới Mothra phía trên | 90 |
| | `ultima_void` | Giai đoạn III "Hư Không": bốn chân, thân xanh thép, sừng trắng, tinh thể tím mọc khắp người. Mothra dựng kết giới che tàu | 95, 100 |
| | `ultima` | Giai đoạn IV Tối Thượng: đứng hai chân, thân đen đỏ thẫm, hào quang nhiều màu sau lưng, sét đỏ | 100, kết 100 |

Còn thiếu: `story_portrait_raskov.jpg` (chân dung 320×320, đô đốc lớn tuổi, quân phục xám đỏ). Chưa có thì game tự mượn chân dung Kai; có file là tự dùng, không cần sửa code.

**Cách đưa ảnh vào game:** đặt tên ảnh theo khoá (vd. `orykron_rise.png`, `necrosys.webp`; có hay không có `story_` đều được), bỏ chung vào một thư mục rồi chạy:

```bash
python tools/nhap_anh_truyen.py thu_muc_anh/
```

Script tự cắt giữa về 16:9 (chân dung về 1:1), đổi cỡ 1920×1080 (chân dung 320×320), nén JPG dưới 400 KB và ghi đè vào `src/main/resources/story/`. Nó báo ảnh nào sai tên, ảnh gốc nào nhỏ hơn 1920×1080 (sẽ mờ) và khung nào còn thiếu. Cần Python 3 và Pillow (`pip install pillow`).

**Kiểm tra sau khi thay ảnh:** mở màn 1, 21, 41, 50, 61, 70, 81, 100 trong game, và xem ở cửa sổ nhỏ nhất (980×840) để chắc boss không bị cắt.

## 4. Phân công gợi ý

| Việc | Người | File |
|---|---|---|
| A. Viết lại nội dung truyện | Hảo | `StoryMode.java` |
| B. Ghép boss theo chương | Hảo | `CampaignMode.java` (1 chỗ) |
| C. Giao diện toàn màn hình | Tâm | `StoryComicOverlay.java`, `MainFrame.java`, `GamePanel.java`, `StageSelectPanel.java` |
| D. Làm 12 hình mới | Hảo + Tâm chia đôi | `resources/story/`, `SpriteAssets.java` |

Phần A/B và phần C sửa **các file khác nhau**, nên hai người làm song song được mà không xung đột.

## 5. Thứ tự làm và kiểm tra

1. **Chốt các câu hỏi ở mục 6.**
2. **A + B** (Hảo) song song với **C** (Tâm). Mỗi phần một Pull Request.
3. **D**: làm hình dần, mỗi lần xong vài hình thì đổi đường dẫn trong `storyArtPath`.
4. **Kiểm tra trước khi gộp:**
   - [ ] Chơi qua màn 1, 6, 21, 41, 50, 61, 70, 81, 100: truyện hiện đúng nội dung mới, không còn chữ Godzilla, Ghidorah, Mothra.
   - [ ] Thắng màn 20, 40, 60, 80, 100: đoạn kết chương hiện đúng.
   - [ ] Tranh phủ kín cửa sổ ở kích thước nhỏ nhất (980×840) và khi phóng to tối đa; nhân vật không bị cắt.
   - [ ] Nút "Bỏ qua" bấm được bằng chuột ở mọi khung; ESC cũng bỏ qua được; sau khi bỏ qua thì vào trận bình thường.
   - [ ] Click vào tranh: lần 1 hiện hết chữ, lần 2 sang khung tiếp.
   - [ ] Xóa thử một file hình: truyện vẫn chạy, không bị crash.
   - [ ] Lệnh tìm `grep -ri "godzilla\|ghidorah\|mothra" StoryMode.java` không còn kết quả nào.

## 6. Cần quyết định trước khi làm

> **Đã chốt (17/11/2026):** câu 1 chọn (a), đổi thứ tự chương. Câu 2: tạm dùng hình cũ làm chỗ giữ, tranh thật làm sau. Câu 3: làm phần C ngay trên nhánh `story-mode-moi`, nhớ báo Tâm.


1. **Thứ tự chương:** (a) đổi thứ tự chương trong `CampaignChapter` cho khớp truyện (Tận thế lên chương II…). Cách này làm luật đặc biệt và bản đồ của từng chương đổi chỗ theo. Hoặc (b) giữ thứ tự chương, viết truyện cho khớp bản đồ hiện có (chương II "Thần giới" thành nơi Mothra xuất hiện sớm). **Gợi ý: (a)**, vì truyện liền mạch hơn và chỉ phải đổi thứ tự trong 1 enum.
2. **Hình mới làm bằng Blender hay công cụ AI?** Ảnh hưởng thời gian của phần D.
3. **Ai làm phần C?** Phần này sửa vào các file giao diện Tâm đang làm. Nếu Hảo làm thì phải báo Tâm tạm dừng sửa 4 file đó.
