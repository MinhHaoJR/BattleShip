# Mẫu thiết kế boss (dựng từ 3 boss cũ)

> Boss cũ (Godzilla, Ghidorah, Mothra) **giữ nguyên trong game**, dùng làm mẫu.
> Boss mới (Orykron, Vesperax, Orykron-Σ, Necrosys, Orykron Tối Thượng, xem [cot-truyen.md](cot-truyen.md)) đi theo đúng cấu trúc dưới đây, dựng trên Blender.

**Nguyên tắc để ít tốn công nhất:** mỗi chiêu của boss mới **mượn "kiểu" của một chiêu cũ** (vùng đánh, hiệu ứng, rung màn hình, vùng nguy hiểm). Như vậy chỉ cần làm **hình + âm thanh + số liệu**, không phải viết thêm code vẽ hiệu ứng. Hiệu ứng chiêu hiện được vẽ bằng code trong `CombatFxOverlay` (khoảng 220 chỗ gọi theo tên chiêu), viết mới rất tốn công.

---

## 1. Một boss gồm những gì

| Thành phần | Nằm ở đâu trong code | Boss cũ đang dùng |
|---|---|---|
| Tên, màu chính, màu cuồng bạo, máu | `TitanMode.TitanBoss` | Máu 240–280 |
| **5 chiêu**: 3 chiêu thường + 2 chiêu cuồng bạo (giai đoạn 2, khi máu ≤ 50%) | `TitanMode.TitanSkill` | Mỗi boss 5 chiêu |
| Vùng đánh của mỗi chiêu | `TitanMode.skillTargetCells` | Xem mục 3 |
| Thời gian vận chiêu, phần cơ thể tụ lực, vùng nguy hiểm để lại | `skillCastDurationMs`, `chargingPartForSkill`, `hazardTypeForSkill`, `impactZoneMagic` | 2,6–3,4 giây; khi cuồng bạo nhanh hơn 22% |
| Chiêu tối thượng (kiểm tra sát thương) | `cataclysmSkillFor` | Chiêu ★ Ω cuối cùng |
| Hitbox **20 ô** chia 3 phần + **5 điểm yếu** | `basePartLayout`, `generateWeakpointCells` | Đầu ×1,35 sát thương, Lõi/Vây/Cánh ×1,22, Thân/Đuôi ×0,92 |
| Ngưỡng choáng | `staggerThresholdFor` | 82–96, cuồng bạo +28 |
| Chiêu chặn tên lửa (thường + cuồng bạo) | `antiMissileSpec` | Tên chiêu, dòng chữ, 2 màu |
| Môi trường khi xuất hiện, khi cuồng bạo, khi dùng từng chiêu | `environmentForEntrance/Enrage/Skill` | Rung, gió, màu nước, thời tiết, mùa, bản đồ |
| Thứ tự trong Boss Rush | `nextBossInRush` | Godzilla → Ghidorah → Mothra |
| Hình | `SpriteAssets` | Mục 4 |
| Âm thanh | `SoundEngine` | Mục 5 |

---

## 2. Bảng chiêu của 3 boss mẫu

Cột **Kiểu** là tên dùng để boss mới mượn lại.

| Boss | Chiêu (enum) | GĐ | Kiểu / vùng đánh | Sát thương khiên | Vùng nguy hiểm | Phần tụ lực | Vận chiêu |
|---|---|---|---|---|---|---|---|
| Godzilla | `GZ_TAIL_SLAM` Quật Đuôi | 1 | **Quét 1×4** | 38 | – | Thân/Đuôi | 2,6 s |
| | `GZ_ATOMIC_BREATH` Hơi Thở | 1 | **Tia 1×5** | 44 | Điện | Đầu | 3,1 s |
| | `GZ_THERMO_PULSE` Xung Nhiệt | 1 | **Nổ 3×3** | 50 | Cháy | Lõi | 3,3 s |
| | `GZ_SPIRAL_RAY` ★ Tia Xoắn | 2 | **Tia chữ T 7 ô** | 58 | Cháy | Đầu | 2,95 s |
| | `GZ_MELTDOWN_RIFT` ★ Ω | 2 | **Sao 9 ô** | 68 | Cháy | Lõi | 3,4 s |
| Ghidorah | `KG_TRIPLE_GRAVITY` Tam Đầu | 1 | **Tỏa 5 điểm** | 40 | – | Đầu | 3,1 s |
| | `KG_HURRICANE_WING` Bão Cánh | 1 | **Chữ X 5 ô** | 38 | – | Lõi | 2,6 s |
| | `KG_OMEGA_VORTEX` Xoáy | 1 | **Nổ 3×3** | 50 | Điện | Lõi | 3,3 s |
| | `KG_ALPHA_STORM` ★ Siêu Bão | 2 | **Răng cưa 7 ô** | 58 | Điện | Đầu | 2,95 s |
| | `KG_CATACLYSM_GRAVITY` ★ Ω | 2 | **Thiên thạch 9 ô** | 68 | Điện | Lõi | 3,4 s |
| Mothra | `MT_WING_GALE` Gió Cánh | 1 | **Nón 5 ô** | 38 | – | Lõi | 2,6 s |
| | `MT_GOD_RAY` Thánh Quang | 1 | **Tia 1×5** | 45 | Điện | Đầu | 3,1 s |
| | `MT_SILK_WEB` Tơ Kén | 1 | **Nổ 3×3** | 48 | Làm chậm | Thân/Đuôi | 3,1 s |
| | `MT_PRISM_RAIN` ★ Phấn Hoa | 2 | **Răng cưa 7 ô** | 58 | Điện | Lõi | 2,95 s |
| | `MT_SOLAR_SUPERNOVA` ★ Ω | 2 | **Sao 9 ô** | 68 | Cháy | Lõi | 3,4 s |

**Quy luật rút ra:** chiêu thường có sát thương khiên 38–50, chiêu cuồng bạo 58, chiêu Ω 68. Chiêu Ω luôn là chiêu cuối và luôn để lại vùng nguy hiểm.

---

## 3. Thư viện vùng đánh có sẵn

`X` = ô bị đánh, `O` = tâm (ô người chơi/AI chọn).

```
Quét 1×4     Tia 1×5       Nổ 3×3     Sao 9 ô       Chữ X 5 ô
 XOXX        XXOXX         XXX        . . X . .     X . X
                           XOX        . X . X .     . O .
                           XXX        X . O . X     X . X
                                      . X . X .
                                      . . X . .

Tia chữ T 7  Nón 5 ô       Răng cưa 7    Thiên thạch 9    Tỏa 5 điểm
 X . . . .   X X           . X . X .     X . . . X        (tâm + 4 ô cách xa,
 X X O X X   O .           X . O . X     . . X . .         luôn nằm trong bàn)
 X . . . .   X X           . X . X .     . X O X .
                                         . . X . .
                                         X . . . X
```

Boss mới nên dùng lại các vùng này. Nếu thật sự cần vùng mới (ví dụ "chia bầy" của Necrosys), chỉ thêm **một** nhánh `case` vào `skillTargetCells`.

---

## 4. Hình: quy cách render từ Blender

**Phong cách chung (nhìn từ hình cũ):** voxel (khối vuông kiểu Minecraft), **nền trong suốt**, góc nhìn chéo 3/4, quay mặt **sang trái**, có viền tối 1–2 px, tô màu phẳng, ít đổ bóng.

### Danh sách file cho MỖI boss

| File (thay `<boss>` bằng tên boss, chữ thường) | Kích thước | Dùng để | Mẫu cũ |
|---|---|---|---|
| `sprites/titans/<boss>_idle.png` | 288×288 | Tư thế đứng yên trên bàn cờ | `godzilla_idle.png` |
| `sprites/titans/<boss>_charge.png` | 288×288 | Đang tụ lực chiêu *(chỉ Godzilla có, tùy chọn)* | `godzilla_charge.png` |
| `sprites/titans/<boss>_enrage.png` | 288×288 | Giai đoạn 2, cuồng bạo, đổi màu sang `enrageColor` | `godzilla_burning.png`, `ghidorah_storm.png`, `mothra_enrage.png` |
| `sprites/titans/<boss>_rampage_voxel.png` | 640–760 × 540 | Toàn thân to, dùng trong cảnh cắt POV | `ghidorah_rampage_voxel.png` |
| `sprites/titans/skill_<boss>_<chiêu>.png` × 5 | 96×96 *(đạn/tia có thể to hơn, ví dụ 384×160)* | Hình đạn/tia của từng chiêu | `skill_godzilla_atomic.png` |
| `story/story_<boss>_*.jpg` | Theo hình truyện cũ | Khung truyện tranh trong cốt truyện | `story_ch1_godzilla.jpg` |

### Cài đặt Blender gợi ý
1. **Dựng voxel:** nặn hoặc dựng khối thô, sau đó thêm modifier **Remesh → Blocks** (Octree Depth khoảng 5–6) để ra dạng khối vuông. Hoặc dựng trong MagicaVoxel rồi nhập sang Blender.
2. **Camera:** chọn **Orthographic**. Xoay khoảng X 60°, Z −45° (nhìn chéo từ trên), boss quay mặt sang trái màn hình.
3. **Render:** Eevee; **Film → Transparent** bật; Color Management → View Transform **Standard** (để màu không bị nhạt). Độ phân giải đặt đúng bảng trên, Resolution % = 100.
4. **Viền tối:** bật **Freestyle**, độ dày 1,5–2 px, màu gần đen.
5. **Ánh sáng:** 1 đèn Sun chiếu chéo từ trên trái, cộng World màu xám nhạt để vùng tối không bị đen hẳn.
6. **Bản cuồng bạo:** nhân đôi file, chỉ đổi vật liệu phát sáng (Emission) sang `enrageColor`, render lại cùng camera để hai hình khớp nhau.
7. **Lưu 1 file `.blend` cho mỗi boss** trong `art/blender/<boss>.blend`, có sẵn camera cho từng loại hình, để lần sau render lại cho nhanh.

---

## 5. Âm thanh

**Định dạng hiện tại:** WAV 22.050 Hz, 16-bit, **stereo**.
**Đề xuất cho file mới:** WAV 22.050 Hz, 16-bit, **mono**. Nghe gần như không khác mà file nhẹ đi một nửa (repo đang nặng 112 MB, chủ yếu vì âm thanh).

| File cho MỖI boss | Độ dài mẫu | Dùng khi | Mẫu cũ |
|---|---|---|---|
| `audio/sfx_titan_<boss>_roar.wav` | 1,4–2,3 s | Boss xuất hiện, gầm | `sfx_titan_godzilla_roar.wav` |
| `audio/sfx_titan_<chiêu>.wav` × 5 | 1,0–2,5 s | Mỗi lần dùng chiêu | `sfx_titan_atomic_breath.wav` |
| `audio/sfx_boss_pov_<boss>.wav` | 4,8 s | Cảnh cắt POV boss xuất hiện | `sfx_boss_pov_godzilla.wav` |
| `audio/sfx_titan_skill_pov_<boss>.wav` | 2,7 s | Cảnh cắt POV khi tung chiêu Ω | `sfx_titan_skill_pov_godzilla.wav` |
| `audio/bgm_titan_<boss>.wav` *(tùy chọn)* | 60–120 s, lặp lại | Nhạc nền riêng | `bgm_titan_ghidorah.wav` |

**Dùng chung, không cần làm mới:** `sfx_titan_boss_intro.wav`, `sfx_titan_enrage_transform.wav`, `bgm_titan.wav`, `bgm_titan_enrage.wav`.

Cách làm nhanh tiếng gầm: ghép 2–3 tiếng động vật (sư tử, hải mã, voi), hạ pitch 30–50% trong Audacity, thêm Reverb. Chỉ dùng âm thanh tự thu hoặc có giấy phép CC0.

---

## 6. Phiếu thiết kế boss mới (điền cho từng con)

```
TÊN ENUM:            ORYKRON
Tên hiển thị:        "Orykron · Hạch Long"
Màu chính / cuồng bạo: #38BDF8 / #EF4444
Máu:                 260
Ngưỡng choáng:       88
Boss Rush:           đứng sau ______, trước ______
Hitbox 20 ô:         mượn hình của GODZILLA | GHIDORAH | MOTHRA | vẽ mới (ghi lưới 10×10)
5 điểm yếu:          ...

CHIÊU   Enum              Tên            GĐ   Mượn kiểu của     Khiên  Vùng nguy hiểm  Hình             Âm thanh
1       OR_TIDAL_SLAM     Sóng Thủy Triều 1   GZ_TAIL_SLAM      38     –               skill_orykron_tide.png   sfx_titan_or_tide.wav
2       OR_CORE_BEAM      Tia Hạch Tâm    1   GZ_ATOMIC_BREATH  44     Điện            ...              ...
3       ...                               1
4 ★     ...                               2   (răng cưa / chữ T)  58
5 ★ Ω   ...                               2   (sao 9 / thiên thạch) 68   bắt buộc có

Chặn tên lửa (thường / cuồng bạo): tên chiêu, dòng chữ, 2 màu
Môi trường khi xuất hiện / cuồng bạo: thời tiết, mùa, bản đồ, câu thông báo
```

**Gợi ý mượn kiểu cho các boss trong cốt truyện mới:**

| Boss mới | Mẫu gần nhất | Gợi ý |
|---|---|---|
| Orykron | Godzilla | Mượn gần hết bộ chiêu; đổi màu, tên, hình, tiếng |
| Vesperax | Ghidorah | Mượn bộ chiêu trọng lực, thiên thạch |
| Orykron-Σ | Godzilla + Ghidorah | Trộn: tia của Godzilla + thiên thạch của Ghidorah |
| Necrosys | Mothra | Mượn tơ kén (làm chậm) + răng cưa; chiêu "chia bầy" là chiêu duy nhất có thể cần vùng mới |
| Orykron Tối Thượng | Cả 3 | 4 giai đoạn **không có sẵn** trong code (hiện chỉ có 2): đây là phần duy nhất cần viết thêm logic |

---

## 7. Những chỗ phải sửa khi thêm 1 boss

Thông tin của mỗi boss đang nằm rải rác ở khoảng **14 chỗ** (`switch` và chuỗi `if`) trong `TitanMode`, cộng với `SpriteAssets` và `SoundEngine`. Sót chỗ nào thì:
- phần lớn sẽ **báo lỗi biên dịch** (các `switch` dạng `->` phải đủ mọi trường hợp), dễ phát hiện;
- nhưng `hazardTypeForSkill`, `impactZoneMagic`, `skillTargetCells`, `basePartLayout`, `generateWeakpointCells` có giá trị mặc định nên sẽ **âm thầm** dùng vùng 1 ô hoặc hitbox của Godzilla. Phải tự kiểm tra kỹ các chỗ này.

Danh sách cần sửa: `TitanBoss`, `TitanSkill`, `nextBossInRush`, `staggerThresholdFor`, `cataclysmSkillFor`, `antiMissileSpec`, `environmentForEntrance`, `environmentForEnrage`, `environmentForSkill`, `skillTargetCells`, `chargingPartForSkill`, `skillCastDurationMs`, `hazardTypeForSkill`, `impactZoneMagic`, `basePartLayout`, `generateWeakpointCells`; phần nạp hình trong `SpriteAssets` (khoảng dòng 243–278); các enum âm thanh trong `SoundEngine` (khoảng dòng 39–102); và hiệu ứng trong `CombatFxOverlay` (nếu chiêu không mượn kiểu cũ).

> **Đề xuất (chưa làm):** trước khi thêm 5 boss mới, nên gom toàn bộ số liệu của một chiêu vào chính `TitanSkill` (vùng đánh, thời gian vận chiêu, vùng nguy hiểm, **kiểu hiệu ứng mượn từ chiêu cũ**). Khi đó thêm một boss chỉ cần thêm dòng dữ liệu, không phải sửa 14 chỗ. Việc này đụng vào file của Tâm nên cần Tâm đồng ý.
