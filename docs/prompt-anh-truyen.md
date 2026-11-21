# 20 prompt tạo ảnh truyện (dùng với ChatGPT)

**Cách dùng**
- Mỗi lần dán một prompt (cả khối) vào ChatGPT.
- Tải ảnh về và đặt tên theo dòng **File** (ví dụ `orykron_rise.png`).
- Bỏ tất cả ảnh vào một thư mục, rồi chạy `python tools/nhap_anh_truyen.py thu_muc_anh/`.

**Lưu ý chung**
- Mỗi prompt đã có sẵn phần phong cách chung ở cuối, nên ảnh giữa các chương trông đồng bộ.
- Prompt mô tả quái vật bằng hình dáng theo truyện, không gọi tên Toho, vì công cụ tạo ảnh thường từ chối tên nhân vật có bản quyền.
- Muốn quái vật giống nhau qua các ảnh: tạo ảnh đầu tiên của mỗi con trước. Ở các ảnh sau, gửi kèm ảnh đó và ghi "same creature as the reference image".
- Nếu boss bị cắt ở mép, yêu cầu ChatGPT: "move the monster to the center, keep the sides as background only".

---

## Chương I · Hải chiến

### 1. File: `war_clash`
```text
Sunset naval battle on a vast open sea. On the left, a fleet of sleek grey-blue modern warships with glowing cyan hull stripes fires at the right side. On the right, a massive dark crimson-and-black enemy flagship with red glowing lights, surrounded by smaller crimson warships, returns fire. Orange tracer rounds and blue energy shells cross the middle of the frame, muzzle flashes, splashes and smoke. Blazing orange and red sky with dark storm clouds. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

### 2. File: `raskov_flagship`
```text
Low-angle view from the waterline of a colossal dark crimson-and-black battleship flagship plowing through a violent storm at night. Its main turrets fire with huge orange muzzle flashes, red warning lights glow along the hull, and purple lightning splits the sky behind it. On the high command bridge, the small silhouette of an old admiral in a grey-red military coat looks down. Massive waves and sea spray. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

### 3. File: `ghost_fleet`
```text
Night sea covered in eerie green mist. Wrecked warships from two navies, some grey-blue and some dark crimson, rise from the water half-sunken and tilted, hulls rusted and covered in glowing green spores and moss. Their side lights still flicker and their turrets slowly turn on their own, with no crew aboard. Floating green glowing particles everywhere. Haunting and sad mood. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

### 4. File: `black_sun`
```text
A secret nuclear bomb explodes deep under the ocean at night: a gigantic column of glowing cyan-white water shoots hundreds of meters into the sky, a dome of light bulges on the sea surface, and rings of shockwaves spread outward. In the foreground, a dark crimson enemy battleship and grey-blue warships are tossed and tilted by the waves. The sea boils with light. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

## Chương II · Godzilla

> Quái vật chính của chương: thằn lằn biển khổng lồ đứng hai chân, vảy đen như đá núi lửa, khe nứt phát sáng xanh lam như dung nham lạnh, gai lưng sắc phát sáng, đuôi dài có gai xương xoắn như mũi khoan.

### 5. File: `orykron_rise`
```text
A colossal upright reptilian sea titan rises from a sea of fire, roaring at the sky. Black volcanic-rock scales cracked with glowing cyan fissures like cold lava, rows of sharp glowing pale-blue spines along its back, a long tail ending in twisted drill-like bone spikes, glowing cyan eyes and mouth. Lava and burning water around it, ash clouds in a red-black sky. Tiny grey-blue warships in the foreground fire tracer rounds at it, showing its enormous scale. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

### 6. File: `orykron_beam`
```text
At night on a dark blue ocean, a colossal upright reptilian sea titan with black cracked volcanic scales and glowing cyan fissures fires a massive beam of cyan-white energy from its mouth. The beam slices across a line of warships (grey-blue and dark crimson) on the right, causing explosions and towering splashes. Its back spines glow brightly blue as it charges. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

### 7. File: `orykron_feed`
```text
A colossal upright reptilian sea titan with black cracked volcanic scales and glowing cyan fissures tilts its head back under a huge orange mushroom cloud far away on the horizon. Streams of glowing cyan radioactive particles flow from the cloud into its body and back spines, which glow brighter. Hot orange sea, ash falling, a few small warships watching in fear. Ominous mood. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

### 8. File: `orykron_sink`
```text
Grey ash rain over a dark grey sea. In the center, a colossal reptilian sea titan with black cracked scales slowly sinks into a huge glowing cyan whirlpool, its fissures dimming, only its head and back spines still above water. On the left a fleet of grey-blue warships, on the right a fleet of dark crimson warships, both stopped, flags lowered, watching in silence. Somber, quiet mood. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

## Chương III · King Ghidorah và SpaceGodzilla

> Quái vật từ thiên thạch: rồng vàng ba đầu, cổ dài, sừng trên mỗi đầu, đôi cánh vàng lớn, vân tinh thể tím trên vảy, phun sấm sét vàng.
> Bản sao trên vũ trụ: cùng dáng với quái vật chương II nhưng vảy tím đen, tinh thể tím mọc ra từ vai và lưng.

### 9. File: `meteor_fall`
```text
Starry night over the ocean. A huge meteor of glowing violet crystal plunges toward the sea, trailing a long streak of golden and purple fire. Through the cracks in the meteor, a coiled golden dragon with three long necks and three horned heads is visible inside, its six eyes glowing, golden lightning crackling across the meteor surface. Below, small grey-blue warships look up as the sea glows gold and purple. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

### 10. File: `vesperax`
```text
A colossal golden three-headed dragon rises from the night ocean beside a shattered violet meteor. Its scales are bright metallic gold with glowing violet crystal veins, it has three long serpentine necks, each head has a horned crown and glowing eyes, and two huge golden wings spread wide behind it. Golden lightning bolts blast from all three mouths and strike the sea, making towering explosions of water. Small grey-blue warships below look tiny. Low-angle view, the three heads fill the upper half of the frame. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

### 11. File: `titan_clash`
```text
Two colossal monsters clash in the middle of a stormy night sea. On the left, an upright reptilian titan with black cracked scales and glowing cyan fissures fires a cyan energy beam. On the right, a golden three-headed dragon with spread golden wings fires golden lightning from its three mouths; the cyan beam hits its chest and golden scales and sparks explode into the sky where the two attacks collide. Waves taller than ship masts, tiny warships caught between them. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

### 12. File: `orykron_sigma`
```text
In outer space inside an asteroid belt, a colossal upright reptilian titan with dark violet-black scales and glowing purple fissures roars. Large translucent violet crystal formations grow out of its shoulders and back, glowing from within. Floating asteroids and crystal dust around it, a blue ocean planet curving below, stars and nebula behind. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

## Chương IV · Nghĩa địa tàu và Destoroyah

> Quái vật chính của chương: quỷ giáp xác đỏ sẫm đứng thẳng, sinh ra từ bầy giáp xác nhỏ. Mai nhiều đốt, càng như lưỡi hái, cánh màng như cánh dơi, sừng cong trên đầu, lõi màu hổ phách phát sáng ở ngực.

### 13. File: `omega_core`
```text
Night over a graveyard of sunken warships. Far in the distance, a pillar of pure white light from a superweapon strikes down from orbit into the sea. In the foreground, swarms of small dark-red crustaceans crawl out of the rusted wrecks and pile together into a huge breathing mass. Inside the mass, the shape of a much larger crimson crustacean creature with glowing amber eyes is starting to form. White glowing particles fall like snow. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

### 14. File: `zombie_fleet`
```text
A fleet of zombie warships rises from a blood-red misty sea at night: rusted hulls covered in glowing green spores, broken masts, lights flickering. Towering behind them in the red fog, a colossal crimson crustacean demon spreads huge bat-like membrane wings, its segmented carapace and curved horn silhouetted, a glowing amber core in its chest. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

### 15. File: `aurora07`
```text
At night on a dark green sea, a gigantic zombie aircraft carrier bursts out of the water bow first, covered in glowing green spores, seaweed and rust, water pouring off its deck. The hull number "AURORA-07" is clearly painted on its side. In the foreground left, a small grey-blue warship flashes a signal lamp in Morse code, and a single light on the carrier's tower flashes back. Sad and emotional mood. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text other than the hull number, no logos, no speech bubbles.
```

### 16. File: `necrosys`
```text
A colossal upright crimson crustacean demon stands over a graveyard of sunken warships under a dark red sky. Segmented blood-red carapace with glowing orange cracks, scythe-like claws, huge bat-like membrane wings spread wide, a large curved horn on its head, a glowing amber core in its chest, black oxygen-killing mist pouring from its body. Embers float in the air. Menacing low-angle view. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

## Chương V · Mothra và Godzilla Ultima

> Đồng minh: bướm đêm khổng lồ làm từ ánh sáng, cánh trong suốt chuyển màu như cực quang, rắc phấn sáng.
> Boss tiến hóa 4 giai đoạn (cùng một quái vật):
> - I: ấu long bơi dưới nước.
> - II: bò bốn chân, vảy cam đỏ.
> - III: bốn chân, thân xanh thép, sừng trắng, tinh thể tím.
> - IV: đứng hai chân, thân đen đỏ thẫm, hào quang nhiều màu.

### 17. File: `lumira`
```text
At night above a calm dark teal sea, a colossal guardian moth made of light hovers with enormous translucent wings shimmering in aurora gradients of teal, pink and gold, scattering glowing scales like falling stars. Below, two fleets (grey-blue and dark crimson warships) sail together inside glowing rings of light on the water. Near the surface, the long dark-red back and fins of a young aquatic sea dragon glide beneath the waves. Hopeful, magical mood. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

### 18. File: `ultima_awaken`
```text
A colossal four-legged reptilian sea beast crawls onto a reef of dark rocks, with orange-red scales, glowing fissures, a long tail and a tall row of jagged pale spines along its back, its mouth glowing orange as it roars. Warships (grey-blue and dark crimson) fire at it from the foreground. High in the dark orange sky, a huge glowing teal ring of protective light hangs over the sea. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

### 19. File: `ultima_void`
```text
At night on a dark sea, a colossal heavy four-legged reptilian beast with steel-teal scales, a long curved white horn on its head and violet crystals growing all over its back and shoulders roars with a violet glow in its mouth. On the left, a giant guardian moth of light spreads its translucent aurora wings and projects glowing teal and pink barrier rings around a small fleet of grey-blue and dark crimson warships. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

### 20. File: `ultima`
```text
Final battle. A colossal upright reptilian titan in its ultimate form stands in a raging sea under a blood-red night sky: black-and-deep-crimson scales with glowing red fissures, a white horn, its tail dissolving into red particles. Behind its back blazes a huge halo of concentric rings in red, gold, cyan, violet and green. Red lightning strikes around it. Tiny warships of both fleets fire together from the foreground. Epic final-boss composition. Dramatic anime comic illustration, bold black ink outlines, cel shading, saturated colors, strong glowing light effects, detailed sky and ocean waves, cinematic. Wide 16:9 landscape, 1920x1080. Keep the main subject inside the central two-thirds of the frame. No text, no logos, no speech bubbles.
```

## Chân dung (tùy chọn)

### File: `portrait_raskov`
```text
Portrait of an old naval admiral in his sixties, stern and tired face, short grey beard, scar on his cheek, wearing a dark grey-and-crimson military coat with gold epaulettes, looking slightly to the side. Dark background with faint red light. Anime comic illustration, bold black ink outlines, cel shading. Square 1:1, head and shoulders centered. No text.
```
