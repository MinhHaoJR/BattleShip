package battleship.ui;

import java.awt.Color;
import java.util.List;
import java.util.Set;

/**
 * Story Mode catalog ("Biên Niên Sử Hải Đồ Aether — Cốt Truyện 5 Chương · 100 Màn")
 * binding narrative episodes, anime comic keyframes, character dialogue bubbles,
 * Ken-Burns camera cues, and ambient particle moods to CampaignMode stages 1..100.
 */
public final class StoryMode {
    public enum StorySpeaker {
        KAI("Đô Đốc Kai", "Chỉ Huy Hạm Đội Aether", "story_portrait_kai", new Color(0x38BDF8)),
        LYRA("Thuyền Phó Lyra", "Sĩ Quan Sonar & Kết Giới", "story_portrait_lyra", new Color(0x2DD4BF)),
        VEGA("Kỹ Sư Vega", "Trưởng Lò Hyperion Ω", "story_portrait_vega", new Color(0xF59E0B)),
        TITAN("Cộng Hưởng Thái Cổ", "Sóng Âm Hải Thú", null, new Color(0xEF4444)),
        MOTHRA("Nữ Hoàng Mothra", "Thánh Linh Bảo Hộ Cuối Cùng", "story_mothra_queen", new Color(0x6EE7B7)),
        MEMORY("Tiếng Vọng Đáy Sâu", "Hộp Đen Hạm Đội Mất Tích", "story_sad_sunken_fleet", new Color(0x94A3B8)),
        NARRATOR("Nhật Ký Hải Trình", "Biên Niên Sử Chiến Dịch", null, new Color(0xFDE68A));

        public final String displayName;
        public final String roleTitle;
        public final String portraitKey;
        public final Color accent;

        StorySpeaker(String displayName, String roleTitle, String portraitKey, Color accent) {
            this.displayName = displayName;
            this.roleTitle = roleTitle;
            this.portraitKey = portraitKey;
            this.accent = accent;
        }
    }

    public enum MotionStyle {
        PAN_RIGHT,
        PAN_LEFT,
        ZOOM_IN,
        ZOOM_OUT,
        DRAMATIC_SHAKE
    }

    public enum ParticleMood {
        CALM_SPRAY,
        ZOMBIE_SPORE,
        ATOMIC_SPARKS,
        SAKURA_MAGIC,
        LAVA_EMBERS,
        GALAXY_STARS,
        ATLANTIS_BUBBLES,
        STORM_LIGHTNING,
        GOLDEN_DAWN,
        RAIN_TEARS,
        MEMORY_MOTES
    }

    public record StoryFrame(
            String artKey,
            StorySpeaker speaker,
            String sfxBadge,
            String headline,
            String dialogue,
            String caption,
            MotionStyle motion,
            ParticleMood mood,
            Color panelAccent
    ) {}

    public record StoryEpisode(
            String id,
            String chapterTag,
            String title,
            String logline,
            boolean epilogue,
            List<StoryFrame> frames
    ) {
        public StoryFrame leadFrame() {
            return frames.isEmpty() ? null : frames.get(0);
        }
    }

    public static final String ART_CH1_HARBOR = "story_ch1_harbor";
    public static final String ART_CH1_ZOMBIE = "story_ch1_zombie";
    public static final String ART_CH1_GODZILLA = "story_ch1_godzilla";
    public static final String ART_CH2_FANTASY = "story_ch2_fantasy";
    public static final String ART_CH3_APOCALYPSE = "story_ch3_apocalypse";
    public static final String ART_CH4_GALAXY = "story_ch4_galaxy";
    public static final String ART_CH5_ATLANTIS = "story_ch5_atlantis";
    public static final String ART_MOTHRA_QUEEN = "story_mothra_queen";
    public static final String ART_FINALE_GHIDORAH = "story_finale_ghidorah";
    public static final String ART_EPILOGUE_VICTORY = "story_epilogue_victory";
    public static final String ART_SAD_SUNKEN_FLEET = "story_sad_sunken_fleet";
    public static final String ART_SAD_MOTHRA_FAREWELL = "story_sad_mothra_farewell";
    public static final String ART_SAD_VEGA_CONFESSION = "story_sad_vega_confession";
    public static final String ART_SAD_LYRA_TEARS = "story_sad_lyra_tears";
    public static final String ART_SAD_ATLANTIS_ELEGY = "story_sad_atlantis_elegy";
    public static final String ART_SAD_MEMORIAL_DAWN = "story_sad_memorial_dawn";

    private static final Set<Integer> KEY_INTRO_STAGES = Set.of(
            1, 5, 6, 10, 15, 20,
            21, 25, 30, 35, 40,
            41, 45, 50, 55, 60,
            61, 65, 70, 75, 80,
            81, 85, 90, 95, 100
    );

    private static final Set<Integer> EPILOGUE_STAGES = Set.of(20, 40, 60, 80, 100);

    private StoryMode() { }

    public static boolean hasAutoIntroCutscene(int stageNumber) {
        return KEY_INTRO_STAGES.contains(stageNumber);
    }

    public static boolean hasPostVictoryEpilogue(int stageNumber) {
        return EPILOGUE_STAGES.contains(stageNumber);
    }

    public static StoryEpisode chapterIntroEpisode(CampaignMode.CampaignChapter chapter) {
        if (chapter == null) chapter = CampaignMode.CampaignChapter.CH1_NAVAL;
        return switch (chapter) {
            case CH1_NAVAL -> introEpisodeForStage(CampaignMode.stage(1));
            case CH2_FANTASY -> introEpisodeForStage(CampaignMode.stage(21));
            case CH3_APOCALYPSE -> introEpisodeForStage(CampaignMode.stage(41));
            case CH4_GALAXY -> introEpisodeForStage(CampaignMode.stage(61));
            case CH5_ATLANTIS -> introEpisodeForStage(CampaignMode.stage(81));
        };
    }

    public static StoryEpisode introEpisodeForStage(CampaignMode.CampaignStage stage) {
        CampaignMode.CampaignStage st = stage != null ? stage : CampaignMode.stage(1);
        int num = st.stageNumber();

        if (num == 1) {
            return new StoryEpisode(
                    "ep_stage_1_opening",
                    "CHƯƠNG I · TẬP 1",
                    "Bình Minh Vịnh Tân Hải & Tín Hiệu Hạm Đội Mất Tích",
                    "Sonar bắt được mã cầu cứu từ chiến hạm AURORA-07 đã chìm bảy năm trước dưới đáy Vịnh Tân Hải.",
                    false,
                    List.of(
                            new StoryFrame(
                                    ART_CH1_HARBOR,
                                    StorySpeaker.NARRATOR,
                                    "BẢY NĂM SAU",
                                    "VỊNH TÂN HẢI · CHIẾN HẠM AURORA ĐƯỢC TRỤC VỚT",
                                    "Nắng mai hắt lên mũi tàu mang tên AURORA — chiếc duy nhất còn nguyên vẹn sau thảm họa bảy năm trước. Đô đốc Kai vẫn đứng trên boong chờ người không trở lại.",
                                    "Ngày đó, Hạm Đội Tiên Phong Thứ 7 đã lao vào tâm bão để che chắn cho cả thành phố Tân Hải.",
                                    MotionStyle.ZOOM_IN,
                                    ParticleMood.CALM_SPRAY,
                                    new Color(0x38BDF8)
                            ),
                            new StoryFrame(
                                    ART_SAD_SUNKEN_FLEET,
                                    StorySpeaker.LYRA,
                                    "TÍN HIỆU MA!",
                                    "MẬT MÃ MORSE TỪ NGHĨA ĐỊA TÀU CHÌM",
                                    "Đô đốc… Sonar tầng sâu vừa bắt được nhịp gõ quen thuộc: 'AURORA-07 · XIN ĐỪNG QUÊN CHÚNG TÔI'. Chiến hạm của anh Minh… đang dịch chuyển dưới đáy vực!",
                                    "Lyra siết chặt tay lên gọng kính holographic — nơi mảnh kính vỡ bảy năm trước đã cướp đi một nửa ánh sáng của cô.",
                                    MotionStyle.PAN_RIGHT,
                                    ParticleMood.MEMORY_MOTES,
                                    new Color(0x2DD4BF)
                            ),
                            new StoryFrame(
                                    ART_CH1_GODZILLA,
                                    StorySpeaker.VEGA,
                                    "BÁO ĐỘNG ĐỎ!",
                                    "NHỊP TIM PHÓNG XẠ DƯỚI LÒNG VỊNH",
                                    "Không phải tàu cứu hộ đâu sếp! Xác chiến hạm đang bị kéo trồi lên từ vực thẳm, và bên dưới chúng là một Quái Thú mang vết sẹo lò phản ứng quen thuộc…",
                                    "Vega cúi đầu, bàn tay vô thức nắm chặt tấm thẻ bài bạc giấu trong túi áo kỹ sư.",
                                    MotionStyle.PAN_LEFT,
                                    ParticleMood.ATOMIC_SPARKS,
                                    new Color(0xF59E0B)
                            ),
                            new StoryFrame(
                                    ART_CH1_HARBOR,
                                    StorySpeaker.KAI,
                                    "RA KHƠI!",
                                    "ĐƯA ANH EM HẠM ĐỘI THỨ 7 TRỞ VỀ",
                                    "Dù bảy năm đã qua hay đáy biển chỉ còn tro tàn… Toàn hạm đội giữ vững đội hình 10×10! Hôm nay chúng ta ra khơi để đưa mọi người về yên nghỉ.",
                                    "Nhiệm vụ Màn 1/100: Đánh chìm lực lượng tiền tiêu và mở đường ra nghĩa địa biển.",
                                    MotionStyle.ZOOM_OUT,
                                    ParticleMood.RAIN_TEARS,
                                    new Color(0x38BDF8)
                            )
                    )
            );
        }

        if (num == 6) {
            return new StoryEpisode(
                    "ep_stage_6_zombie",
                    "CHƯƠNG I · TẬP 6",
                    "Hạm Đội Xác Sống · Lời Cầu Siêu Dưới Mưa Đêm",
                    "Những chiến hạm Zombie trồi lên từ sương mù hóa ra chính là đồng đội đã ngã xuống năm xưa.",
                    false,
                    List.of(
                            new StoryFrame(
                                    ART_SAD_SUNKEN_FLEET,
                                    StorySpeaker.LYRA,
                                    "RÈÈÈ… CẢNH BÁO!",
                                    "NHỮNG CON TÀU ĐEO SỐ HIỆU QUEN THUỘC",
                                    "Đô đốc nhìn qua ống nhòm đi… Lớp san hô phát quang xanh bám trên thân tàu ma kia mang phù hiệu 'HẢI ÂU-03' và 'AURORA-07' của Hạm Đội Thứ 7!",
                                    "Cơn mưa đêm trút xuống bến cảng câm lặng — tiếng còi tàu rỉ sét vang lên như tiếng nấc từ đáy vực.",
                                    MotionStyle.ZOOM_IN,
                                    ParticleMood.RAIN_TEARS,
                                    new Color(0x22C55E)
                            ),
                            new StoryFrame(
                                    ART_CH1_ZOMBIE,
                                    StorySpeaker.MEMORY,
                                    "KÝ ỨC VỌNG VỀ",
                                    "LINH HỒN BỊ GIAM GIỮ TRONG THÉP LẠNH",
                                    "Bào tử Vực Sâu đã khóa ý thức thủy thủ trong lớp giáp mục nát, buộc những người từng bảo vệ quê hương phải chĩa mũi pháo về Tân Hải Thành.",
                                    "Không có lời đầu hàng nào phát ra được — chỉ còn nỗi đau đóng băng dưới làn sương lục.",
                                    MotionStyle.PAN_RIGHT,
                                    ParticleMood.MEMORY_MOTES,
                                    new Color(0x4ADE80)
                            ),
                            new StoryFrame(
                                    ART_CH1_ZOMBIE,
                                    StorySpeaker.VEGA,
                                    "XÈÈÈ… AXIT!",
                                    "CẨN THẬN PHẢN ĐÒN HỆ XÁC SỐNG",
                                    "Đau lòng thật sếp ạ, nhưng thân tàu chúng ngấm đầy bào tử Axit và Lời Nguyền! Mỗi phát đạn đánh trúng đều có thể khiến chúng phát nổ phản kích!",
                                    "Hạm Đội Zombie sẵn sàng tung chiêu Axit, Thiên Thạch và Nguyền Rủa ngay khi trúng đạn.",
                                    MotionStyle.DRAMATIC_SHAKE,
                                    ParticleMood.ZOMBIE_SPORE,
                                    new Color(0x22C55E)
                            ),
                            new StoryFrame(
                                    ART_SAD_SUNKEN_FLEET,
                                    StorySpeaker.KAI,
                                    "AN NGHỈ ĐI!",
                                    "GIẢI THOÁT CHO ĐỒNG ĐỘI NĂM XƯA",
                                    "Đừng để họ làm nô lệ cho đáy vực thêm một đêm nào nữa! Bắn chìm tất cả… hãy xem từng phát đại pháo hôm nay như phát súng tiễn anh em yên nghỉ!",
                                    "Màn 6/100: Phá giải lời nguyền của Hạm Đội Xác Sống.",
                                    MotionStyle.ZOOM_OUT,
                                    ParticleMood.RAIN_TEARS,
                                    new Color(0x38BDF8)
                            )
                    )
            );
        }

        if (num == 21) {
            return new StoryEpisode(
                    "ep_ch2_fantasy",
                    "CHƯƠNG II · KHỞI HÀNH",
                    "Cổng Trời Thần Giới · Khu Rừng Tro Hoa Bất Tử",
                    "Vùng trời hoa anh đào thực chất là kết giới mỏng manh được dệt từ sinh lực của Nữ Hoàng Mothra.",
                    false,
                    List.of(
                            new StoryFrame(
                                    ART_CH2_FANTASY,
                                    StorySpeaker.NARRATOR,
                                    "KHÔNG GIAN ĐẢO BAY",
                                    "THÁNH ĐỊA LƠ LỬNG TRÊN TẦNG MÂY LỆ",
                                    "Bước qua cột sáng nơi vết thương biển, hạm đội đặt chân lên vùng trời huyền ảo phủ đầy cánh hoa trắng — kết tinh ký ức của những thủy thủ lạc biển.",
                                    "Chương II (Màn 21–40): Thánh Địa Kết Giới Của Nữ Hoàng Thần Giới.",
                                    MotionStyle.ZOOM_OUT,
                                    ParticleMood.SAKURA_MAGIC,
                                    new Color(0xF472B6)
                            ),
                            new StoryFrame(
                                    ART_MOTHRA_QUEEN,
                                    StorySpeaker.MOTHRA,
                                    "TIẾNG VỖ CÁNH",
                                    "LỜI CHÀO TỪ NỮ HOÀNG MOTHRA",
                                    "Hỡi những thủy thủ mang đầy vết sẹo… Ta là Mothra. Đôi cánh này dệt Vòng Kết Giới suốt ngàn năm chỉ để giữ độc tố không gian khỏi rơi xuống thế gian.",
                                    "Ánh sáng ngọc bích bao bọc con tàu — dịu dàng mà mong manh như hơi thở cuối.",
                                    MotionStyle.PAN_RIGHT,
                                    ParticleMood.SAKURA_MAGIC,
                                    new Color(0x34D399)
                            ),
                            new StoryFrame(
                                    ART_SAD_MOTHRA_FAREWELL,
                                    StorySpeaker.LYRA,
                                    "SỰ THẬT ĐAU LÒNG!",
                                    "KẾT GIỚI ĐANG RÚT CẠN SINH MỆNH NỮ HOÀNG",
                                    "Khoan đã Đô đốc! Máy quét cho thấy mỗi Vòng Kết Giới giảm 35% hồi chiêu và hồi khiên cho tàu ta… đều đang đốt cháy chính đôi cánh của Mothra!",
                                    "Ngài vừa thử thách lòng nhân hậu của nhân loại, vừa gồng mình chống đỡ bầu trời đang nứt.",
                                    MotionStyle.ZOOM_IN,
                                    ParticleMood.MEMORY_MOTES,
                                    new Color(0xE879F9)
                            )
                    )
            );
        }

        if (num == 41) {
            return new StoryEpisode(
                    "ep_ch3_apocalypse",
                    "CHƯƠNG III · TẬN THẾ",
                    "Biển Lửa Tận Thế · Lời Thú Tội Dưới Buồng Máy",
                    "Giữa vành đai dung nham thiêu đốt, kỹ sư Vega cuối cùng cũng nói ra tội lỗi bảy năm trước.",
                    false,
                    List.of(
                            new StoryFrame(
                                    ART_CH3_APOCALYPSE,
                                    StorySpeaker.NARRATOR,
                                    "RẦM RẦM!!",
                                    "VÀNH ĐAI TRO TÀN & KHE DUNG NHAM",
                                    "Bầu trời đỏ rực như vết thương hở. Các Khe Dung Nham tăng +8% sát thương Lửa và cháy lan AoE, nhưng hơi nóng đủ làm vỏ tàu thường rạn nứt.",
                                    "Chương III (Màn 41–60): Lò Nung Nơi Ký Ức Tội Lỗi Bị Chôn Vùi.",
                                    MotionStyle.DRAMATIC_SHAKE,
                                    ParticleMood.LAVA_EMBERS,
                                    new Color(0xF97316)
                            ),
                            new StoryFrame(
                                    ART_SAD_VEGA_CONFESSION,
                                    StorySpeaker.VEGA,
                                    "LỜI THÚ TỘI",
                                    "TẤM ẢNH RÁCH TRONG BUỒNG LÕI HYPERION",
                                    "Sếp ơi… nhìn tấm thẻ bài rỉ sét này đi. Bảy năm trước chính chú là kỹ sư siết van lò phản ứng cấm, gây ra vụ nổ cướp mất anh Minh và Hạm Đội Thứ 7…",
                                    "Trong căn hầm đầy tro than, bàn tay thô ráp của Vega run lên bên tấm ảnh tập thể đã nhòe.",
                                    MotionStyle.PAN_LEFT,
                                    ParticleMood.MEMORY_MOTES,
                                    new Color(0xFB923C)
                            ),
                            new StoryFrame(
                                    ART_SAD_VEGA_CONFESSION,
                                    StorySpeaker.VEGA,
                                    "NỢ MÁU NĂM XƯA",
                                    "NỤ CƯỜI SUỐT BẢY NĂM CHỈ ĐỂ CHUỘC TỘI",
                                    "Đêm nào bước vào buồng máy chú cũng thấy bóng anh em năm xưa đứng sau lưng. Chú giữ bản vẽ lõi Hyperion Ω… để một ngày tự tay kết thúc cơn ác mộng này!",
                                    "Tiếng lò phản ứng gầm nhẹ như tiếng thở dài giấu kín suốt bảy mùa bão.",
                                    MotionStyle.ZOOM_IN,
                                    ParticleMood.RAIN_TEARS,
                                    new Color(0xF59E0B)
                            ),
                            new StoryFrame(
                                    ART_CH3_APOCALYPSE,
                                    StorySpeaker.KAI,
                                    "ĐỦ RỒI VEGA!",
                                    "KHÔNG AI PHẢI GÁNH TỘI MỘT MÌNH",
                                    "Nghe ta nói đây: anh Minh chưa từng trách chú, và ta cũng vậy. Hãy sống sót vượt qua Biển Lửa này rồi cùng nhau khép chặt cánh cửa địa ngục!",
                                    "Màn 41/100: Siết chặt đội ngũ, xuyên qua địa ngục dung nham.",
                                    MotionStyle.ZOOM_OUT,
                                    ParticleMood.LAVA_EMBERS,
                                    new Color(0xF97316)
                            )
                    )
            );
        }

        if (num == 61) {
            return new StoryEpisode(
                    "ep_ch4_galaxy",
                    "CHƯƠNG IV · NGÂN HÀ",
                    "Biển Sao Ngân Hà · Ánh Sáng Tắt Dần Nơi Kính Ngắm",
                    "Giữa dòng tinh vân Zero-G lạnh lẽo, từ trường Hố Đen Tím âm thầm cướp đi phần thị lực cuối của Lyra.",
                    false,
                    List.of(
                            new StoryFrame(
                                    ART_CH4_GALAXY,
                                    StorySpeaker.NARRATOR,
                                    "BƯỚC NHẢY WARP",
                                    "HẢI TRÌNH KHÔNG TRỌNG LỰC QUANH HỐ ĐEN TÍM",
                                    "Cột địa nhiệt phóng cả hạm đội vượt tầng khí quyển ra dòng tinh vân Zero-G, nơi tàu lướt xa 2 ô mỗi lượt và Đá Pha Lê nạp đầy Hải Năng.",
                                    "Chương IV (Màn 61–80): Vùng Lặng Nơi Ánh Sao Đóng Băng.",
                                    MotionStyle.ZOOM_OUT,
                                    ParticleMood.GALAXY_STARS,
                                    new Color(0xA855F7)
                            ),
                            new StoryFrame(
                                    ART_SAD_SUNKEN_FLEET,
                                    StorySpeaker.MEMORY,
                                    "SÓNG VÔ TUYẾN CỔ",
                                    "TIẾNG VỌNG TRÔI DẠT GIỮA CHÂN KHÔNG",
                                    "Tín hiệu SOS từ hàng trăm chiến hạm mất tích suốt nhiều thế kỷ vẫn vang vọng giữa dải ngân hà — toàn bộ đều là lời nhắn gửi về nhà chưa từng có hồi âm.",
                                    "Không gian không có không khí để mang tiếng khóc, chỉ có máy thu Sonar lắng nghe thay.",
                                    MotionStyle.PAN_LEFT,
                                    ParticleMood.MEMORY_MOTES,
                                    new Color(0xC084FC)
                            ),
                            new StoryFrame(
                                    ART_SAD_LYRA_TEARS,
                                    StorySpeaker.LYRA,
                                    "KÍNH NGẮM RẠN VỠ",
                                    "ÁNH SÁNG TẮT DẦN NHƯNG KHÔNG DÁM NÓI",
                                    "Từ trường Ghidorah ăn mòn dây thần kinh thị giác khiến mắt phải của Lyra mờ dần từng nhịp… Cô cắn môi lau vội giọt lệ, không muốn Đô đốc lo lắng.",
                                    "Đằng sau ô cửa kính buồng lái, Trái Đất phủ bão đêm chỉ còn là viên ngọc xanh mong manh.",
                                    MotionStyle.ZOOM_IN,
                                    ParticleMood.RAIN_TEARS,
                                    new Color(0x38BDF8)
                            ),
                            new StoryFrame(
                                    ART_CH4_GALAXY,
                                    StorySpeaker.KAI,
                                    "GIỮ VỮNG LA BÀN!",
                                    "PHÁ VÒNG VÂY TINH VÂN VỀ ATLANTIS",
                                    "Nạp năng lượng pha lê cho tháp Sonar của Lyra! Phá tan đội hình phong tỏa ngoài quỹ đạo — chúng ta sắp tìm ra nguồn cơn của mọi bi kịch rồi!",
                                    "Màn 61/100: Băng qua Hố Đen Tím hướng về tọa độ cuối cùng.",
                                    MotionStyle.PAN_RIGHT,
                                    ParticleMood.GALAXY_STARS,
                                    new Color(0xA855F7)
                            )
                    )
            );
        }

        if (num == 81) {
            return new StoryEpisode(
                    "ep_ch5_atlantis",
                    "CHƯƠNG V · ATLANTIS",
                    "Atlantis Vực Sâu · Bi Ca Của Những Tượng Đá Hóa Thạch",
                    "Trụ Cổ Ngữ giảm 35% sát thương Boss chính là linh hồn tự nguyện hóa đá của vương quốc cổ xưa.",
                    false,
                    List.of(
                            new StoryFrame(
                                    ART_CH5_ATLANTIS,
                                    StorySpeaker.NARRATOR,
                                    "BIỂN RẼ ĐÔI",
                                    "THÀNH ĐÔ CHÌM TRONG IM LẶNG NGÀN NĂM",
                                    "Đại dương mở toang hai phía như bức tường pha lê khổng lồ, để lộ Đền Đinh Ba Vàng rực rỡ ánh ngọc lam u tịch dưới tận cùng đáy vực.",
                                    "Chương V (Màn 81–100): Hồi Kết Huyền Thoại Của Chiến Dịch.",
                                    MotionStyle.ZOOM_OUT,
                                    ParticleMood.ATLANTIS_BUBBLES,
                                    new Color(0x2DD4BF)
                            ),
                            new StoryFrame(
                                    ART_SAD_ATLANTIS_ELEGY,
                                    StorySpeaker.MEMORY,
                                    "BI CA ATLANTIS",
                                    "NHỮNG VỊ HỘ VỆ TỰ KHÓA TRÁI TIM VÀO ĐÁ",
                                    "Mỗi Trụ Cổ Ngữ che chắn 35% sát thương Boss đều từng là một công dân Atlantis — họ tự nguyện hóa đá giữa vực sâu để trẻ thơ mặt đất được thấy mặt trời.",
                                    "Những pho tượng cúi đầu vĩnh viễn bên xác tàu vỡ, ánh sáng linh hồn nhẹ nhàng bao lấy hạm đội.",
                                    MotionStyle.PAN_RIGHT,
                                    ParticleMood.MEMORY_MOTES,
                                    new Color(0x14B8A6)
                            ),
                            new StoryFrame(
                                    ART_SAD_VEGA_CONFESSION,
                                    StorySpeaker.VEGA,
                                    "QUYẾT ĐỊNH THẦM LẶNG",
                                    "TẦN SỐ CỔ NGỮ KHỚP VỚI LÕI HYPERION Ω",
                                    "Thưa ngài Kai… Các Cổ Ngữ vừa đồng bộ với khoang Hyperion Ω. Nếu phong ấn nứt ở Màn 100, chỉ có kích nổ thủ công từ bên trong mới đủ sức xuyên phá.",
                                    "Vega khẽ mỉm cười thanh thản — lần này cậu không còn run tay như bảy năm trước nữa.",
                                    MotionStyle.PAN_LEFT,
                                    ParticleMood.RAIN_TEARS,
                                    new Color(0xF59E0B)
                            ),
                            new StoryFrame(
                                    ART_CH5_ATLANTIS,
                                    StorySpeaker.KAI,
                                    "20 ẢI CUỐI CÙNG!",
                                    "ĐỪNG PHỤ CÁI GIÁ CỦA ÁNH BÌNH MINH",
                                    "Hãy để di nguyện Atlantis dẫn lối! Toàn hạm đội tiến sâu từ Màn 81 tới Màn 100 — không ai được phép quên những người đã nằm lại dưới lớp sóng này!",
                                    "Sẵn sàng đối đầu định mệnh tại trung tâm Đền Đinh Ba.",
                                    MotionStyle.ZOOM_IN,
                                    ParticleMood.ATLANTIS_BUBBLES,
                                    new Color(0x2DD4BF)
                            )
                    )
            );
        }

        if (num == 100) {
            return new StoryEpisode(
                    "ep_stage_100_finale",
                    "ĐẠI CHIẾN TỐI THƯỢNG · MÀN 100",
                    "Đêm Đen Atlantis · Lời Hứa Trước Giờ Tận Thế",
                    "Phong ấn ngàn năm nứt vỡ; ba sinh mạng, một Siêu Tàu Hyperion Ω và lời hứa cuối cùng với đại dương.",
                    false,
                    List.of(
                            new StoryFrame(
                                    ART_FINALE_GHIDORAH,
                                    StorySpeaker.TITAN,
                                    "GRÀOOOOO!!",
                                    "TAM ĐẦU KIM LONG XÉ NÁT PHONG ẤN NGÀN NĂM",
                                    "Cây Đinh Ba Atlantis nứt đôi! King Ghidorah Chế Độ Tận Thế (490 HP) vươn ba đầu vàng xé toạc mây đen, giáng bão từ trường Alpha nhấn chìm bầu trời!",
                                    "Hệ thống phòng thủ Tam Đầu Lôi kích hoạt — sẵn sàng chặn mọi tên lửa tầm thường.",
                                    MotionStyle.DRAMATIC_SHAKE,
                                    ParticleMood.STORM_LIGHTNING,
                                    new Color(0xFACC15)
                            ),
                            new StoryFrame(
                                    ART_SAD_ATLANTIS_ELEGY,
                                    StorySpeaker.MEMORY,
                                    "DI NGUYỆN CỔ XƯA",
                                    "NHỮNG TƯỢNG ĐÁ CÚI ĐẦU TRAO TRỌN ÁNH SÁNG",
                                    "Khi pho tượng gác đền cuối cùng rạn vỡ, toàn bộ ma lực ngàn năm nơi Trụ Cổ Ngữ hóa thành dòng sáng lam chảy thẳng vào thân tàu Hyperion Ω!",
                                    "Hàng nghìn linh hồn Atlantis lặng lẽ nâng đỡ con tàu cuối cùng của nhân loại.",
                                    MotionStyle.PAN_RIGHT,
                                    ParticleMood.MEMORY_MOTES,
                                    new Color(0x2DD4BF)
                            ),
                            new StoryFrame(
                                    ART_SAD_LYRA_TEARS,
                                    StorySpeaker.LYRA,
                                    "TRÁI TIM THAY ĐÔI MẮT",
                                    "LỜI THÌ THẦM BÊN DẢI BĂNG TRẮNG",
                                    "Đô đốc… dù không còn thấy gì nữa, đôi tai em vẫn nghe rõ từng nhịp sét của nó! Em đã khóa chết tọa độ đầu Ghidorah cho Đại Pháo HYPERION Ω rồi!",
                                    "Lyra mỉm cười giữa màn tối vĩnh viễn — giọt lệ ấm rơi xuống bảng điều khiển Sonar.",
                                    MotionStyle.PAN_LEFT,
                                    ParticleMood.RAIN_TEARS,
                                    new Color(0x38BDF8)
                            ),
                            new StoryFrame(
                                    ART_CH5_ATLANTIS,
                                    StorySpeaker.KAI,
                                    "TOÀN LỰC KHAI HỎA!",
                                    "VÌ NHỮNG NGƯỜI Ở LẠI ĐÁY SÂU — MÀN 100/100!",
                                    "Vì anh Minh, vì Nữ Hoàng Mothra, vì cả Atlantis ngàn năm im lặng… Phá vỡ ĐẦU Ghidorah, kích hoạt HYPERION Ω và mang bình minh về Tân Hải!",
                                    "Nhiệm vụ Cuối: Hạ gục King Ghidorah tại Màn 100/100.",
                                    MotionStyle.ZOOM_IN,
                                    ParticleMood.STORM_LIGHTNING,
                                    new Color(0xEF4444)
                            )
                    )
            );
        }

        if (st.isBossStage()) {
            return buildBossMilestoneIntro(st);
        }

        return buildRegularStageEpisode(st);
    }

    public static StoryEpisode epilogueEpisodeForStage(CampaignMode.CampaignStage stage) {
        CampaignMode.CampaignStage st = stage != null ? stage : CampaignMode.stage(20);
        int num = st.stageNumber();
        return switch (num) {
            case 20 -> new StoryEpisode(
                    "ep_after_20",
                    "HỒI KẾT CHƯƠNG I",
                    "Tiếng Khóc Của Vua Biển & Ánh Sáng Trên Tầng Mây",
                    "Sau lớp giáp nguyên tử vỡ nát, Godzilla để lộ vết thương lò phản ứng cũ và khẽ khép mi mắt dưới mưa.",
                    true,
                    List.of(
                            new StoryFrame(
                                    ART_CH1_GODZILLA,
                                    StorySpeaker.KAI,
                                    "VẾT SẸO NĂM XƯA",
                                    "KẺ THÙ HÓA RA CŨNG CHỈ LÀ NẠN NHÂN",
                                    "Khi lớp vảy phát sáng tắt dần, Kai bàng hoàng nhận ra vết cháy hình lõi lò phản ứng khắc sâu trên ngực Godzilla — vết thương từ bảy năm trước.",
                                    "Nó không gầm lên thù hận, chỉ lặng lẽ nhìn xác những con tàu như người gác mộ cô độc.",
                                    MotionStyle.ZOOM_OUT,
                                    ParticleMood.RAIN_TEARS,
                                    new Color(0x38BDF8)
                            ),
                            new StoryFrame(
                                    ART_SAD_SUNKEN_FLEET,
                                    StorySpeaker.MEMORY,
                                    "LỜI AI ĐIẾU",
                                    "NHỊP THỞ CUỐI DƯỚI CƠN MƯA VỊNH TÂN HẢI",
                                    "Godzilla thu tia nguyên tử, cất một tiếng rống trầm dài như lời cầu siêu cho Hạm Đội Thứ 7, rồi lặn sâu vào bóng tối đại dương mang theo nỗi cô đơn.",
                                    "Cơn mưa Tân Hải dịu lại — nhưng trong lòng Vega vết đau tội lỗi mới thực sự rỉ máu.",
                                    MotionStyle.PAN_LEFT,
                                    ParticleMood.MEMORY_MOTES,
                                    new Color(0x94A3B8)
                            ),
                            new StoryFrame(
                                    ART_CH2_FANTASY,
                                    StorySpeaker.LYRA,
                                    "CỔNG TRỜI MỞ RA!",
                                    "MẢNH TINH THỂ DẪN LỐI LÊN CHƯƠNG II: THẦN GIỚI",
                                    "Từ đáy mắt biển vừa dịu sóng, một cột ánh sáng hồng bừng lên xuyên tầng mây! Muốn tìm cách sửa sai cho thế giới, ta phải tiến tới Thần Giới (Màn 21).",
                                    "Mở khóa Chương II · Thần Giới Fantasy (Màn 21–40).",
                                    MotionStyle.PAN_RIGHT,
                                    ParticleMood.SAKURA_MAGIC,
                                    new Color(0xF472B6)
                            )
                    )
            );
            case 40 -> new StoryEpisode(
                    "ep_after_40",
                    "HỒI KẾT CHƯƠNG II",
                    "Đôi Cánh Hóa Bụi Vàng · Sự Hy Sinh Của Mothra",
                    "Mothra tự tan thành triệu lông vũ ánh sáng để bọc giáp cho hạm đội trước khe nứt Biển Lửa.",
                    true,
                    List.of(
                            new StoryFrame(
                                    ART_MOTHRA_QUEEN,
                                    StorySpeaker.MOTHRA,
                                    "KẾT GIỚI NỨT TOÁC",
                                    "KHÔNG CÒN THỜI GIAN THỬ THÁCH LÒNG NGƯỜI",
                                    "Hơi nóng từ địa ngục phía nam đã xuyên thủng tầng mây! Các ngươi đã chứng minh được sự bao dung với Godzilla — giờ đến lượt ta giữ lời thề hộ vệ.",
                                    "Đôi cánh ngọc bích bắt đầu rạn nứt thành vô số đốm sáng mong manh.",
                                    MotionStyle.ZOOM_OUT,
                                    ParticleMood.SAKURA_MAGIC,
                                    new Color(0xF472B6)
                            ),
                            new StoryFrame(
                                    ART_SAD_MOTHRA_FAREWELL,
                                    StorySpeaker.MOTHRA,
                                    "VĨNH BIỆT…",
                                    "TAN THÀNH HÀNG TRIỆU LÔNG VŨ ÁNH SÁNG",
                                    "Hãy giữ lấy phấn tơ cuối cùng của ta, hỡi Đô đốc. Khi cánh này tan hết, hào quang sẽ che thân tàu các ngươi khỏi bão lửa… Đừng để nước mắt làm chệch tay lái.",
                                    "Nữ Hoàng Mothra mỉm cười trong mưa cánh hoa vàng rồi tan vào gió biển Thần Giới.",
                                    MotionStyle.PAN_RIGHT,
                                    ParticleMood.MEMORY_MOTES,
                                    new Color(0x34D399)
                            ),
                            new StoryFrame(
                                    ART_CH3_APOCALYPSE,
                                    StorySpeaker.KAI,
                                    "LỜI THỀ KHẮC GHI!",
                                    "TIẾN VÀO CHƯƠNG III: TẬN THẾ BIỂN LỬA (41–60)",
                                    "Ánh sáng cuối cùng của Mothra đang ôm lấy lớp thép tàu Aurora… Vega nâng khiên nhiệt! Chúng ta bước vào Biển Lửa để không phụ sự hy sinh cao cả ấy!",
                                    "Mở khóa Chương III · Tận Thế Biển Lửa (Màn 41–60).",
                                    MotionStyle.ZOOM_IN,
                                    ParticleMood.LAVA_EMBERS,
                                    new Color(0xF97316)
                            )
                    )
            );
            case 60 -> new StoryEpisode(
                    "ep_after_60",
                    "HỒI KẾT CHƯƠNG III",
                    "Cột Lửa Xé Trời · Hành Trình Rời Bỏ Mặt Đất",
                    "Cột địa nhiệt phóng hạm đội vượt tầng mây, để lại Trái Đất nhỏ bé run rẩy giữa bão tro.",
                    true,
                    List.of(
                            new StoryFrame(
                                    ART_CH3_APOCALYPSE,
                                    StorySpeaker.VEGA,
                                    "BỨT TỐC LÊN TRỜI!",
                                    "VỤ NỔ LÒ ĐỊA NHIỆT ĐẨY HẠM ĐỘI VÀO QUỸ ĐẠO",
                                    "Lõi dung nham dưới Biển Lửa vừa kích phát cột plasma khổng lồ! Động cơ Hyperion bám theo luồng khí nóng phóng thẳng xuyên tầng khí quyển!",
                                    "Bầu trời tro bụi dần lùi lại dưới mạn tàu khi trọng lực bắt đầu tan biến.",
                                    MotionStyle.PAN_LEFT,
                                    ParticleMood.LAVA_EMBERS,
                                    new Color(0xF97316)
                            ),
                            new StoryFrame(
                                    ART_SAD_LYRA_TEARS,
                                    StorySpeaker.LYRA,
                                    "QUÊ HƯƠNG PHÍA DƯỚI",
                                    "TRÁI ĐẤT MỎNG MANH TRONG CƠN BÃO TỐ",
                                    "Qua ô cửa kính, Tân Hải và cả đại dương chỉ còn là vệt sáng nhỏ nhoi quằn quại dưới mây bão… Liệu lúc trở về, có còn ai đứng đợi trên cầu cảng không?",
                                    "Giọt nước mắt lơ lửng trong khoang không trọng lực, phản chiếu hình hài Trái Đất rạn nứt.",
                                    MotionStyle.ZOOM_IN,
                                    ParticleMood.RAIN_TEARS,
                                    new Color(0x38BDF8)
                            ),
                            new StoryFrame(
                                    ART_CH4_GALAXY,
                                    StorySpeaker.KAI,
                                    "ĐỪNG QUAY ĐẦU!",
                                    "CHƯƠNG IV · BIỂN SAO GALAXY (MÀN 61–80)",
                                    "Rồi sẽ có ngày chúng ta trở về! Phía trước là Biển Sao Vũ Trụ và Hố Đen Tím — nơi nguồn phát tín hiệu của Tam Đầu Kim Long đang ẩn náu!",
                                    "Mở khóa Chương IV · Galaxy Vũ Trụ (Màn 61–80).",
                                    MotionStyle.ZOOM_OUT,
                                    ParticleMood.GALAXY_STARS,
                                    new Color(0xA855F7)
                            )
                    )
            );
            case 80 -> new StoryEpisode(
                    "ep_after_80",
                    "HỒI KẾT CHƯƠNG IV",
                    "Đôi Mắt Tắt Ánh Sáng · Tiếng Gọi Từ Atlantis Chết Chóc",
                    "Lyra đánh đổi phần thị lực cuối cùng để giải mã tọa độ Ghidorah ngay dưới lòng Atlantis.",
                    true,
                    List.of(
                            new StoryFrame(
                                    ART_SAD_LYRA_TEARS,
                                    StorySpeaker.LYRA,
                                    "KÍNH NGẮM VỤT TẮT!",
                                    "ĐỔI ÁNH SÁNG CUỐI ĐỂ KHÓA CHẾT TỌA ĐỘ",
                                    "Màn hình holographic vỡ vụn thành đốm sáng xanh… Đô đốc ơi, em không nhìn thấy gì nữa rồi, nhưng bằng thính giác… em nghe thấy trái tim Ghidorah đập ở Atlantis!",
                                    "Thùy trán thấm mồ hôi lạnh — người hoa tiêu trung thành nhất vẫn giữ chặt tay trên radar.",
                                    MotionStyle.PAN_RIGHT,
                                    ParticleMood.RAIN_TEARS,
                                    new Color(0x38BDF8)
                            ),
                            new StoryFrame(
                                    ART_SAD_ATLANTIS_ELEGY,
                                    StorySpeaker.MEMORY,
                                    "SỰ THẬT NGÀN NĂM",
                                    "VƯƠNG QUỐC TỰ CHÔN MÌNH ĐỂ GIỮ QUÁI THÚ",
                                    "Từ thuở bình minh nhân loại, dân tộc Atlantis đã hiến thân hóa đá thành Trụ Cổ Ngữ để giam giữ Tam Đầu Long dưới vực sâu — nhưng phong ấn đang rạn vỡ từng giờ.",
                                    "Những tiếng hát cổ vang qua đáy biển nhắc nhở món nợ của hàng ngàn năm bình yên.",
                                    MotionStyle.PAN_LEFT,
                                    ParticleMood.MEMORY_MOTES,
                                    new Color(0x2DD4BF)
                            ),
                            new StoryFrame(
                                    ART_CH5_ATLANTIS,
                                    StorySpeaker.KAI,
                                    "TRỞ VỀ ĐÁY VỰC!",
                                    "CHƯƠNG V · ATLANTIS VỰC SÂU (MÀN 81–100)",
                                    "Băng dải lụa trắng cho Lyra đi Vega… Cả hạm đội quay mũi về Trái Đất! 20 màn cuối tại Atlantis sẽ định đoạt số phận của cả quá khứ lẫn tương lai!",
                                    "Mở khóa Chương V · Atlantis Vực Sâu (Màn 81–100).",
                                    MotionStyle.ZOOM_IN,
                                    ParticleMood.ATLANTIS_BUBBLES,
                                    new Color(0x2DD4BF)
                            )
                    )
            );
            case 100 -> new StoryEpisode(
                    "ep_after_100_true_ending",
                    "HỒI KẾT TỐI THƯỢNG · TRUE ENDING",
                    "Ánh Nắng Khắc Trên Bia Nước · Khúc Tưởng Niệm Hạm Đội Thứ 7",
                    "Vega khóa cửa buồng lõi Hyperion Ω sửa lỗi năm xưa; bình minh trở lại trên những đóa cúc trắng.",
                    true,
                    List.of(
                            new StoryFrame(
                                    ART_SAD_VEGA_CONFESSION,
                                    StorySpeaker.VEGA,
                                    "CHỐT CỬA BUỒNG MÁY!",
                                    "LẦN NÀY CHÚ TỰ TAY SỬA SAI CHO NĂM XƯA",
                                    "Xin lỗi nhé sếp Kai, cô Lyra… Mạch điều khiển từ xa cháy rồi. Chú khóa cửa lõi Hyperion Ω từ bên trong đây — lần này hãy để chú đưa anh Minh về!",
                                    "Tiếng chốt sắt sập xuống, sau lưng Vega là tấm ảnh tập thể rách của Hạm Đội Thứ 7 sáng rực ánh lam.",
                                    MotionStyle.ZOOM_IN,
                                    ParticleMood.MEMORY_MOTES,
                                    new Color(0xF59E0B)
                            ),
                            new StoryFrame(
                                    ART_FINALE_GHIDORAH,
                                    StorySpeaker.TITAN,
                                    "OÀMMM!! ÁNH SÁNG!",
                                    "HYPERION Ω XUYÊN THỦNG TRÁI TIM BÃO TỐ",
                                    "Tia năng lượng cực đại bùng lên hòa cùng bụi phấn vàng còn sót của Mothra và tiếng gầm vọng xa của Godzilla — xé nát Ghidorah trong bầu trời mở toang!",
                                    "Lớp mây bão đen kịt suốt tám năm bỗng tan rã thành những dải nắng vàng rực rỡ.",
                                    MotionStyle.DRAMATIC_SHAKE,
                                    ParticleMood.GOLDEN_DAWN,
                                    new Color(0xFACC15)
                            ),
                            new StoryFrame(
                                    ART_SAD_MOTHRA_FAREWELL,
                                    StorySpeaker.MEMORY,
                                    "VỀ CÙNG ĐẠI DƯƠNG",
                                    "NHỮNG CÁNH BƯỚM ĐƯA NGƯỜI Ở LẠI VÀO GIẤC NGỦ ÊM",
                                    "Giữa khoảnh khắc lặng thinh sau vụ nổ không còn khói lửa, tấm thẻ bài bạc và những cánh lông vũ ngọc bích lững lờ trôi xuống mặt biển êm đềm.",
                                    "Không còn lời nguyền Zombie, không còn tiếng rống cô độc — mọi linh hồn đều được giải thoát.",
                                    MotionStyle.ZOOM_OUT,
                                    ParticleMood.MEMORY_MOTES,
                                    new Color(0x34D399)
                            ),
                            new StoryFrame(
                                    ART_EPILOGUE_VICTORY,
                                    StorySpeaker.KAI,
                                    "CHÀO HẢI QUÂN!",
                                    "TÀU AURORA TRỞ VỀ BẾN CẢNG TÂN HẢI",
                                    "Tiếng chuông cảng vang lên sau tám năm câm lặng. Các con tàu sống sót kéo còi chào dài… Chiến thắng hôm nay thấm đẫm máu và nước mắt của những người không về.",
                                    "Đoàn tàu rẽ sóng vàng trở về bến xưa, mang theo lời hứa giữ bình yên mãi mãi.",
                                    MotionStyle.PAN_RIGHT,
                                    ParticleMood.GOLDEN_DAWN,
                                    new Color(0x38BDF8)
                            ),
                            new StoryFrame(
                                    ART_SAD_MEMORIAL_DAWN,
                                    StorySpeaker.LYRA,
                                    "HOA CÚC TRẮNG",
                                    "BÌNH MINH ĐẸP RỒI… CÁC ANH YÊN NGHỈ NHÉ",
                                    "Sáng nay nắng ấm lắm Đô đốc ạ… Em không nhìn thấy nữa, nhưng em nghe sóng hát êm đềm lắm. Cám ơn Vega, anh Minh, Mothra và Atlantis… Biển đã bình yên rồi.",
                                    "— HẾT CHÍNH TRUYỆN (100/100 MÀN) · CẢM ƠN ĐÔ ĐỐC ĐÃ ĐI CÙNG HẠM ĐỘI AETHER —",
                                    MotionStyle.ZOOM_IN,
                                    ParticleMood.RAIN_TEARS,
                                    new Color(0xFACC15)
                            )
                    )
            );
            default -> buildRegularStageEpisode(st);
        };
    }

    private static StoryEpisode buildBossMilestoneIntro(CampaignMode.CampaignStage st) {
        int num = st.stageNumber();
        TitanMode.TitanBoss boss = st.boss() != null ? st.boss() : TitanMode.TitanBoss.GODZILLA;
        String bossArt = switch (boss) {
            case GODZILLA -> ART_CH1_GODZILLA;
            case MOTHRA -> ART_MOTHRA_QUEEN;
            case GHIDORAH -> ART_FINALE_GHIDORAH;
        };
        ParticleMood mood = switch (boss) {
            case GODZILLA -> ParticleMood.ATOMIC_SPARKS;
            case MOTHRA -> ParticleMood.SAKURA_MAGIC;
            case GHIDORAH -> ParticleMood.STORM_LIGHTNING;
        };
        String chapterArt = artForChapter(st.chapter());
        String introArt = milestoneBackdropArt(st, boss, chapterArt);
        String storyBeat = milestoneLoreDiscovery(num, st, boss);
        String bossTag = boss.shortLabel.replaceFirst("^Boss:\\s*", "").toUpperCase();
        String bossTactic = switch (boss) {
            case GODZILLA -> "Bức xạ hạt nhân đang tụ trong họng nó! Hãy bắn gãy bộ phận ĐẦU để ngắt Tia Nguyên Tử Chặn Tên Lửa, hoặc dùng Khiên chống đỡ lúc nó chuẩn bị phun!";
            case MOTHRA -> "Bụi phấn lăng kính của Nữ Hoàng tạo lớp Phấn Tơ Chặn Đạn và Hào Quang Solar! Đánh gãy điểm yếu ĐẦU để xuyên qua lớp kết giới mong manh đó!";
            case GHIDORAH -> "Áp suất sụt xuống tận đáy! Ba đầu rồng vàng đang hội tụ sấm sét Trọng Lực và Tam Đầu Lôi Chặn Tên Lửa — ưu tiên phá ĐẦU hoặc dùng HYPERION Ω!";
        };

        List<StoryFrame> frames = List.of(
                new StoryFrame(
                        introArt,
                        StorySpeaker.LYRA,
                        "MỐC TITAN " + num + "/100",
                        milestoneHeadline(num, st),
                        storyBeat,
                        st.title() + " · " + boss.displayName + " (" + st.bossHpOverride() + " HP · " + st.bossDifficulty().label + ")",
                        MotionStyle.PAN_RIGHT,
                        num >= 60 ? ParticleMood.MEMORY_MOTES : ParticleMood.RAIN_TEARS,
                        st.chapter().accent
                ),
                new StoryFrame(
                        bossArt,
                        StorySpeaker.VEGA,
                        bossTag + " XUẤT HIỆN!",
                        "PHÂN TÍCH ĐIỂM YẾU & KỸ NĂNG PHÒNG NGỰ",
                        bossTactic,
                        "Mẹo Chiến Thuật: Tấn công chuẩn xác bộ phận ĐẦU sẽ cắt đứt trạng thái Chống Tên Lửa của Boss!",
                        MotionStyle.DRAMATIC_SHAKE,
                        mood,
                        boss.accentColor
                ),
                new StoryFrame(
                        chapterArt.equals(introArt) ? milestoneClosingArt(boss) : chapterArt,
                        StorySpeaker.KAI,
                        "VÀO VỊ TRÍ CHIẾN ĐẤU!",
                        "KHÔNG ĐƯỢC LÙI BƯỚC TRƯỚC ĐỊNH MỆNH",
                        "Tận dụng địa hình cộng hưởng, giữ Khiên trước khi Quái Thú gồng đòn và tiến gần thêm một bước tới sự thật năm xưa!",
                        "Nhiệm vụ Màn " + num + ": Đánh bại " + boss.displayName + ".",
                        MotionStyle.ZOOM_IN,
                        mood,
                        new Color(0xEF4444)
                )
        );
        return new StoryEpisode(
                "ep_boss_" + num,
                st.chapter().shortTab.toUpperCase() + " · ẢI TRÙM " + num,
                "Đối Đầu " + boss.displayName + " (" + num + "/100)",
                "Biến cố cột mốc Màn " + num + ": chạm trán " + boss.displayName + " (" + st.bossHpOverride() + " HP).",
                false,
                frames
        );
    }

    private static StoryEpisode buildRegularStageEpisode(CampaignMode.CampaignStage st) {
        int num = st.stageNumber();
        boolean isZombie = st.ruleType() == CampaignMode.StageRuleType.ZOMBIE_ASSAULT;
        String primaryArt = isZombie ? ART_CH1_ZOMBIE : artForChapter(st.chapter());
        String reflectionArt = secondaryChapterArt(st.chapter(), isZombie);
        String finaleArt = tertiaryChapterArt(st.chapter(), num, primaryArt, reflectionArt);
        ParticleMood primaryMood = isZombie ? ParticleMood.ZOMBIE_SPORE : moodForChapter(st.chapter());

        StorySpeaker secondSpeaker = switch (st.ruleType()) {
            case SNIPER_AMMO, BLITZ_TIMED -> StorySpeaker.VEGA;
            case SONAR_FOG, ZOMBIE_ASSAULT -> StorySpeaker.LYRA;
            default -> chapterConfidant(st.chapter());
        };
        String emotionalBeat = chapterReflectionLine(st, num, isZombie);
        String tacticalOrder = switch (st.ruleType()) {
            case SNIPER_AMMO -> "Kho đạn đang cạn dần (chỉ còn " + st.shotLimit()
                    + " phát tối đa). Từng viên đạn đều quý như sinh mạng đồng đội — hãy kết hợp Phép & Vũ Khí thật chuẩn!";
            case BLITZ_TIMED -> "Đội tàu ma đang tháo chạy sau " + st.timeLimitSec()
                    + " giây! Áp sát bằng Airstrike và Sét liên hoàn trước khi dấu vết của Hạm Đội Thứ 7 biến mất!";
            case SONAR_FOG -> "Màn sương che lấp cả kính ngắm, nhưng Phao Sonar đã bắt được " + st.initialSonarHints()
                    + " điểm nóng cùng " + st.initialMana() + " Hải Năng sẵn sàng khai hỏa!";
            case ZOMBIE_ASSAULT -> "Những thân tàu nhiễm bào tử lại trồi lên từ vực sâu! Cẩn thận phản đòn Axit, Thiên Thạch và Nguyền Rủa để giải thoát cho họ!";
            default -> "Tuyến hải trình Màn " + num + " bị phong tỏa bởi " + st.enemyFleetTypes().size()
                    + " chiến hạm địch. Giữ vững đội hình bảo vệ hạm đội — không được mất thêm một con tàu nào nữa!";
        };

        List<StoryFrame> frames = List.of(
                new StoryFrame(
                        primaryArt,
                        StorySpeaker.NARRATOR,
                        "HỒ SƠ MÀN " + num + "/100",
                        st.title().toUpperCase(),
                        chapterAtmosphereLine(st, isZombie),
                        "Bối cảnh: " + st.chapter().theme.displayName + " · " + st.season().shortLabel + " · " + st.weather().shortLabel,
                        MotionStyle.PAN_RIGHT,
                        primaryMood,
                        st.chapter().accent
                ),
                new StoryFrame(
                        reflectionArt,
                        secondSpeaker,
                        "KÝ ỨC CHIẾN HẠM",
                        chapterMemoryHeadline(st.chapter(), isZombie),
                        emotionalBeat,
                        "Mạch Truyện: " + st.chapter().fullTitle,
                        MotionStyle.ZOOM_IN,
                        ParticleMood.MEMORY_MOTES,
                        new Color(0x94A3B8)
                ),
                new StoryFrame(
                        finaleArt,
                        StorySpeaker.KAI,
                        st.ruleType().shortLabel.toUpperCase(),
                        "CHỈ THỊ CHIẾN THUẬT TRƯỚC GIỜ G",
                        tacticalOrder,
                        st.threeStarCondition(),
                        MotionStyle.PAN_LEFT,
                        isZombie ? ParticleMood.RAIN_TEARS : primaryMood,
                        st.ruleType().accent
                )
        );
        return new StoryEpisode(
                "ep_stage_" + num,
                st.chapter().shortTab.toUpperCase() + " · MÀN " + num,
                st.title(),
                emotionalBeat,
                false,
                frames
        );
    }

    private static String milestoneBackdropArt(CampaignMode.CampaignStage st, TitanMode.TitanBoss boss, String chapterArt) {
        return switch (st.chapter()) {
            case CH1_NAVAL -> ART_SAD_SUNKEN_FLEET;
            case CH2_FANTASY -> boss == TitanMode.TitanBoss.MOTHRA ? ART_SAD_MOTHRA_FAREWELL : ART_CH2_FANTASY;
            case CH3_APOCALYPSE -> ART_SAD_VEGA_CONFESSION;
            case CH4_GALAXY -> ART_SAD_LYRA_TEARS;
            case CH5_ATLANTIS -> ART_SAD_ATLANTIS_ELEGY;
        };
    }

    private static String milestoneClosingArt(TitanMode.TitanBoss boss) {
        return switch (boss) {
            case GODZILLA -> ART_CH1_HARBOR;
            case MOTHRA -> ART_SAD_MOTHRA_FAREWELL;
            case GHIDORAH -> ART_SAD_MEMORIAL_DAWN;
        };
    }

    private static String milestoneHeadline(int num, CampaignMode.CampaignStage st) {
        return switch (num) {
            case 5 -> "BÓNG HÌNH ĐẦU TIÊN TRONG CƠN MƯA TÂN HẢI";
            case 10 -> "DẤU VẾT BỨC XẠ TRÊN NGHĨA ĐỊA TÀU CHÌM";
            case 15 -> "TIẾNG GẦM GIỮA BÃO TỐ SẮP NỔ";
            case 20 -> "CHỦNG TỬ ĐẦU TIÊN · CỬA NGÕ THÁNH ĐỊA";
            case 40 -> "BÀI THỬ THÁCH CUỐI NƠI THÁNH ĐƯỜNG HOA RƠI";
            case 60 -> "TRÁI TIM LÒ NUNG DƯỚI ĐAI TRO TẬN THẾ";
            case 80 -> "KẺ GÁC CỔNG CHÂN TRỜI HỐ ĐEN";
            case 95 -> "ĐÊM TĨNH MỊCH TRƯỚC TRẬN CHUNG KẾT ATLANTIS";
            default -> "BIẾN CỐ CỘT MỐC TẠI " + st.chapter().shortTab.toUpperCase();
        };
    }

    private static String milestoneLoreDiscovery(int num, CampaignMode.CampaignStage st, TitanMode.TitanBoss boss) {
        return switch (num) {
            case 5 -> "Giữa cơn mưa đêm phủ kín nghĩa địa chiến hạm, lớp sóng dâng cao 30m — sinh vật cổ đại đang tìm kiếm tần số lò phản ứng bảy năm trước!";
            case 10 -> "Radar phân tích thấy trên lưng Quái Thú găm đầy mảnh thép từ Hạm Đội Thứ 7. Nó không săn mồi… nó đang nổi giận vì nỗi đau chưa khép!";
            case 15 -> "Sóng xung kích xé rách bầu trời Tân Hải. Vega im lặng siết chặt nắm đấm khi bước sóng bức xạ trùng khớp hoàn toàn với vụ nổ năm xưa.";
            case 20 -> "Ác mộng bảy năm trước đứng sừng sững ngay trước mắt (" + st.bossHpOverride() + " HP). Phải đánh bại nó mới mở được cánh cổng tìm cách cứu thế giới!";
            case 40 -> "Đôi cánh Nữ Hoàng rớm ánh sáng tàn lụi giữa màn mưa hoa anh đào. Đây là bài kiểm tra cuối cùng để chứng minh nhân loại xứng đáng đi tiếp!";
            case 60 -> "Nhiệt độ buồng máy vượt ngưỡng đỏ! Nếu đánh bại hộ vệ lò lửa tại Màn 60, hạm đội sẽ mượn xung lực nổ vượt khỏi khí quyển!";
            case 80 -> "Kính ngắm của Lyra đã nứt đến vết cuối cùng. Đằng sau Quái Thú gác cổng Hố Đen này chính là tọa độ thềm Atlantis cổ xưa!";
            case 95 -> "Bốn bề Atlantis lặng ngắt trước giờ giông bão. Hàng trăm tượng đá hộ vệ dồn chút ánh sáng tàn cuối cùng dọn đường tới trận Màn 100.";
            default -> "Áp suất đại dương sụt mạnh! " + boss.displayName + " (" + st.bossHpOverride() + " HP · " + st.bossDifficulty().label
                    + ") đang lao thẳng vào đội hình hạm đội tại " + st.title() + "!";
        };
    }

    private static String secondaryChapterArt(CampaignMode.CampaignChapter chapter, boolean isZombie) {
        if (isZombie) return ART_SAD_SUNKEN_FLEET;
        return switch (chapter) {
            case CH1_NAVAL -> ART_SAD_SUNKEN_FLEET;
            case CH2_FANTASY -> ART_SAD_MOTHRA_FAREWELL;
            case CH3_APOCALYPSE -> ART_SAD_VEGA_CONFESSION;
            case CH4_GALAXY -> ART_SAD_LYRA_TEARS;
            case CH5_ATLANTIS -> ART_SAD_ATLANTIS_ELEGY;
        };
    }

    private static String tertiaryChapterArt(CampaignMode.CampaignChapter chapter, int num, String a1, String a2) {
        String candidate = switch (chapter) {
            case CH1_NAVAL -> (num % 2 == 0) ? ART_CH1_HARBOR : ART_CH1_GODZILLA;
            case CH2_FANTASY -> ART_MOTHRA_QUEEN;
            case CH3_APOCALYPSE -> ART_SAD_LYRA_TEARS;
            case CH4_GALAXY -> ART_CH5_ATLANTIS;
            case CH5_ATLANTIS -> ART_FINALE_GHIDORAH;
        };
        if (candidate.equals(a1) || candidate.equals(a2)) {
            return ART_SAD_MEMORIAL_DAWN;
        }
        return candidate;
    }

    private static StorySpeaker chapterConfidant(CampaignMode.CampaignChapter chapter) {
        return switch (chapter) {
            case CH1_NAVAL -> StorySpeaker.KAI;
            case CH2_FANTASY -> StorySpeaker.MOTHRA;
            case CH3_APOCALYPSE -> StorySpeaker.VEGA;
            case CH4_GALAXY -> StorySpeaker.LYRA;
            case CH5_ATLANTIS -> StorySpeaker.MEMORY;
        };
    }

    private static String chapterAtmosphereLine(CampaignMode.CampaignStage st, boolean isZombie) {
        if (isZombie) {
            return "Làn sương lục bảo lại trùm xuống mạn tàu, nơi những chiến hạm đắm mang số hiệu đồng đội cũ âm thầm nhô lên giữa đêm.";
        }
        return switch (st.chapter()) {
            case CH1_NAVAL -> "Hải đồ " + st.title() + " phủ đầy mảnh vỡ trôi dạt — mỗi chấm sáng trên radar đều gợi nhắc về đêm mưa bảy năm trước.";
            case CH2_FANTASY -> "Cánh hoa anh đào Thần Giới rơi lả tả qua lan can thép như những lá thư tiễn biệt không tên của đại dương.";
            case CH3_APOCALYPSE -> "Tro bụi nóng rực lấp đầy không khí quanh " + st.title() + ", ánh lửa hắt lên gương mặt trầm mặc của đoàn thủy thủ.";
            case CH4_GALAXY -> "Khoảng không vũ trụ lặng câm không một tiếng sóng, chỉ có tín hiệu vô tuyến lạc trôi vương trên kính ngắm rạn nứt.";
            case CH5_ATLANTIS -> "Hai vách nước Atlantis dựng đứng hai bên, ánh sáng từ những pho tượng hộ vệ hóa đá soi bước hạm đội tiến vào vực sâu.";
        };
    }

    private static String chapterMemoryHeadline(CampaignMode.CampaignChapter chapter, boolean isZombie) {
        if (isZombie) return "NHỮNG BÓNG MA TỪ HẠM ĐỘI THỨ 7";
        return switch (chapter) {
            case CH1_NAVAL -> "NỖI ĐAU CHƯA NGUÔI Ở BẾN CẢNG CŨ";
            case CH2_FANTASY -> "PHÉP MÀU ĐỔI BẰNG SINH LỰC CỦA MOTHRA";
            case CH3_APOCALYPSE -> "GÁNH NẶNG LƯƠNG TÂM TRONG BUỒNG MÁY";
            case CH4_GALAXY -> "ÁNH SÁNG MỜ DẦN TRÊN CẶP KÍNH SONAR";
            case CH5_ATLANTIS -> "LỜI NGUYỆN TRƯỚC TƯỢNG ĐÁ NGÀN NĂM";
        };
    }

    private static String chapterReflectionLine(CampaignMode.CampaignStage st, int num, boolean isZombie) {
        if (isZombie) {
            return "Đừng nhìn vào số hiệu trên thân tàu kẻo run tay, Đô đốc… Hãy tiễn họ một đoạn đường yên bình khỏi lời nguyền đáy biển.";
        }
        return switch (st.chapter()) {
            case CH1_NAVAL -> "Bảy năm qua, từng mảnh thép vớt lên từ vùng biển này đều còn vương hơi lạnh của đêm Aurora-07 biến mất.";
            case CH2_FANTASY -> "Mỗi Vòng Kết Giới giúp hạm đội trụ vững đều làm ánh sáng trên đôi cánh Nữ Hoàng yếu đi thêm một phần… Đừng lãng phí phép màu này.";
            case CH3_APOCALYPSE -> "Vega khẽ lau muội than trên tấm thẻ bài bạc, thầm nhủ nếu đi hết chặng Màn " + num + ", cậu sẽ sửa lại mọi lỗi lầm năm xưa.";
            case CH4_GALAXY -> "Mắt phải đã tối sầm nhưng Lyra vẫn ngồi thẳng trước bảng Sonar: 'Đừng lo cho em, Đô đốc… em vẫn nghe rõ nhịp tim của cả hạm đội.'";
            case CH5_ATLANTIS -> "Những linh hồn Atlantis đã ngủ giữa vực sâu hàng ngàn năm chỉ để chờ một thế hệ dám đứng lên khép lại bóng tối.";
        };
    }

    public static String artForChapter(CampaignMode.CampaignChapter chapter) {
        if (chapter == null) return ART_CH1_HARBOR;
        return switch (chapter) {
            case CH1_NAVAL -> ART_CH1_HARBOR;
            case CH2_FANTASY -> ART_CH2_FANTASY;
            case CH3_APOCALYPSE -> ART_CH3_APOCALYPSE;
            case CH4_GALAXY -> ART_CH4_GALAXY;
            case CH5_ATLANTIS -> ART_CH5_ATLANTIS;
        };
    }

    public static ParticleMood moodForChapter(CampaignMode.CampaignChapter chapter) {
        if (chapter == null) return ParticleMood.CALM_SPRAY;
        return switch (chapter) {
            case CH1_NAVAL -> ParticleMood.CALM_SPRAY;
            case CH2_FANTASY -> ParticleMood.SAKURA_MAGIC;
            case CH3_APOCALYPSE -> ParticleMood.LAVA_EMBERS;
            case CH4_GALAXY -> ParticleMood.GALAXY_STARS;
            case CH5_ATLANTIS -> ParticleMood.ATLANTIS_BUBBLES;
        };
    }
}
