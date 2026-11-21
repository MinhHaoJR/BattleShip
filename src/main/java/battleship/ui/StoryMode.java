package battleship.ui;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Story Mode catalog ("Biển Hạch Tâm" — Cốt Truyện 5 Chương · 100 Màn)
 * binding narrative episodes, anime comic keyframes, character dialogue bubbles,
 * Ken-Burns camera cues, and ambient particle moods to CampaignMode stages 1..100.
 * Nội dung theo docs/cot-truyen.md; boss trong gameplay chỉ "đóng thế" (xem CampaignMode.storyBossName).
 */
public final class StoryMode {
    public enum StorySpeaker {
        KAI("Đô Đốc Kai", "Chỉ Huy Liên Minh Tân Hải", "story_portrait_kai", new Color(0x38BDF8)),
        LYRA("Thuyền Phó Lyra", "Sĩ Quan Sonar", "story_portrait_lyra", new Color(0x2DD4BF)),
        VEGA("Kỹ Sư Vega", "Trưởng Lò Vũ Khí", "story_portrait_vega", new Color(0xF59E0B)),
        RASKOV("Đô Đốc Raskov", "Chỉ Huy Hạm Đội Vel'Kar", "story_portrait_raskov", new Color(0xDC2626)),
        TITAN("Cộng Hưởng Thái Cổ", "Sóng Âm Hải Thú", null, new Color(0xEF4444)),
        LUMIRA("Mothra", "Nữ Hoàng Ánh Sáng · Hải Thú Bảo Hộ", "story_lumira", new Color(0x6EE7B7)),
        MEMORY("Tiếng Vọng Đáy Sâu", "Hộp Đen Hạm Đội Mất Tích", "story_ghost_fleet", new Color(0x94A3B8)),
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

    // Tranh cốt truyện "Biển Hạch Tâm": 4 tranh riêng mỗi chương, file /story/<key>.jpg (render từ Blender)
    public static final String ART_WAR_CLASH = "story_war_clash";
    public static final String ART_RASKOV_FLAGSHIP = "story_raskov_flagship";
    public static final String ART_GHOST_FLEET = "story_ghost_fleet";
    public static final String ART_BLACK_SUN = "story_black_sun";
    public static final String ART_ORYKRON_RISE = "story_orykron_rise";
    public static final String ART_ORYKRON_BEAM = "story_orykron_beam";
    public static final String ART_ORYKRON_FEED = "story_orykron_feed";
    public static final String ART_ORYKRON_SINK = "story_orykron_sink";
    public static final String ART_METEOR_FALL = "story_meteor_fall";
    public static final String ART_VESPERAX = "story_vesperax";
    public static final String ART_TITAN_CLASH = "story_titan_clash";
    public static final String ART_ORYKRON_SIGMA = "story_orykron_sigma";
    public static final String ART_OMEGA_CORE = "story_omega_core";
    public static final String ART_ZOMBIE_FLEET = "story_zombie_fleet";
    public static final String ART_AURORA07 = "story_aurora07";
    public static final String ART_NECROSYS = "story_necrosys";
    public static final String ART_LUMIRA = "story_lumira";
    public static final String ART_ULTIMA_AWAKEN = "story_ultima_awaken";
    public static final String ART_ULTIMA_VOID = "story_ultima_void";
    public static final String ART_ULTIMA = "story_ultima";
    public static final String ART_PORTRAIT_RASKOV = "story_portrait_raskov";

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
        return introEpisodeForStage(CampaignMode.stage(chapter.startStage));
    }

    private static StoryFrame frame(String art, StorySpeaker speaker, String sfx, String headline, String dialogue,
                                    String caption, MotionStyle motion, ParticleMood mood, int rgb) {
        return new StoryFrame(art, speaker, sfx, headline, dialogue, caption, motion, mood, new Color(rgb));
    }

    private static StoryEpisode episode(String id, String tag, String title, String logline, boolean epilogue,
                                        StoryFrame... frames) {
        return new StoryEpisode(id, tag, title, logline, epilogue, List.of(frames));
    }

    private static String bossCaption(CampaignMode.CampaignStage st) {
        return st.title() + " · " + CampaignMode.storyBossName(st.stageNumber())
                + " (" + st.bossHpOverride() + " HP · " + st.bossDifficulty().label + ")";
    }

    public static StoryEpisode introEpisodeForStage(CampaignMode.CampaignStage stage) {
        CampaignMode.CampaignStage st = stage != null ? stage : CampaignMode.stage(1);
        int num = st.stageNumber();

        switch (num) {
            case 1 -> {
                return episode("ep_stage_1_opening", "CHƯƠNG I · TẬP 1",
                        "Chiến Tranh Biển Aether · Tiếng Pháo Đầu Tiên",
                        "Liên Minh Tân Hải và Đế Chế Vel'Kar tranh nhau mỏ năng lượng dưới Rãnh Vực Sâu — và biển Aether bốc cháy.",
                        false,
                        frame(ART_WAR_CLASH, StorySpeaker.NARRATOR, "BIỂN AETHER",
                                "HAI HẠM ĐỘI · MỘT MỎ NĂNG LƯỢNG",
                                "Dưới Rãnh Vực Sâu là mỏ năng lượng đủ thắp sáng cả quần đảo trăm năm. Liên Minh Tân Hải và Đế Chế Vel'Kar đều muốn nó — và không bên nào chịu nhường.",
                                "Hoàng hôn đỏ rực, cờ hai phe tung bay trên những cột buồm thép.",
                                MotionStyle.ZOOM_IN, ParticleMood.CALM_SPRAY, 0x38BDF8),
                        frame(ART_RASKOV_FLAGSHIP, StorySpeaker.RASKOV, "KÊNH VEL'KAR",
                                "TỐI HẬU THƯ CỦA ĐÔ ĐỐC RASKOV",
                                "Hạm đội Tân Hải, đây là Đô Đốc Raskov. Rút khỏi Rãnh Vực Sâu trước khi mặt trời lặn. Vel'Kar có những vũ khí mà các người chưa từng thấy.",
                                "Giọng nói già nua, bình thản — như người đã quen ra lệnh cho cả biển.",
                                MotionStyle.PAN_LEFT, ParticleMood.STORM_LIGHTNING, 0xDC2626),
                        frame(ART_RASKOV_FLAGSHIP, StorySpeaker.VEGA, "PHÁO ĐÃ NẠP!",
                                "LÒ VŨ KHÍ SẴN SÀNG",
                                "Pháo chính nạp xong rồi sếp! Tàu Vel'Kar bọc giáp dày nhưng xoay trở chậm. Đừng để chúng áp sát đội hình là được.",
                                "Vega lau vết dầu máy trên tay, mắt không rời bảng đo áp suất.",
                                MotionStyle.PAN_RIGHT, ParticleMood.ATOMIC_SPARKS, 0xF59E0B),
                        frame(ART_WAR_CLASH, StorySpeaker.KAI, "RA KHƠI!",
                                "GIỮ VỮNG ĐỘI HÌNH 10×10",
                                "Toàn hạm đội vào vị trí! Chúng ta không bắn trước, nhưng nếu Vel'Kar khai hỏa thì không ai được lùi một hải lý nào.",
                                "Nhiệm vụ Màn 1/100: Đẩy lùi đội tiên phong Vel'Kar khỏi Vịnh Tân Hải.",
                                MotionStyle.ZOOM_OUT, ParticleMood.CALM_SPRAY, 0x38BDF8));
            }
            case 6 -> {
                return episode("ep_stage_6_signal", "CHƯƠNG I · TẬP 6",
                        "Tiếng Thở Dưới Rãnh Vực Sâu",
                        "Lyra bắt được một tín hiệu lạ dưới đáy biển, đúng lúc xác tàu chìm của trận trước trồi lên.",
                        false,
                        frame(ART_GHOST_FLEET, StorySpeaker.LYRA, "TÍT… TÍT…",
                                "TÍN HIỆU LẠ DƯỚI RÃNH VỰC SÂU",
                                "Đô đốc… Sonar bắt được một nhịp đập rất chậm, rất sâu. Không phải tàu ngầm, cũng không phải cá voi. Nó giống… tiếng thở.",
                                "Lyra ép tai nghe sát hơn, gương mặt tái đi dưới ánh đèn sonar xanh.",
                                MotionStyle.ZOOM_IN, ParticleMood.MEMORY_MOTES, 0x2DD4BF),
                        frame(ART_GHOST_FLEET, StorySpeaker.NARRATOR, "SƯƠNG XANH",
                                "XÁC TÀU CỦA TRẬN TRƯỚC TRỒI LÊN",
                                "Dòng nước lạ từ Rãnh Vực Sâu kéo những xác tàu vừa chìm tuần trước trồi lên mặt biển. Pháo vẫn nạp đạn, máy vẫn chạy — nhưng trên boong không còn ai.",
                                "Có chiếc mang cờ Vel'Kar, có chiếc mang cờ Tân Hải.",
                                MotionStyle.PAN_RIGHT, ParticleMood.ZOMBIE_SPORE, 0x22C55E),
                        frame(ART_GHOST_FLEET, StorySpeaker.VEGA, "CẨN THẬN!",
                                "THÂN TÀU NHIỄM THỨ GÌ ĐÓ",
                                "Vỏ thép của chúng phủ một lớp nhớt phát sáng, bắn trúng là phun axit ngược lại! Sếp ơi, thứ dưới đáy kia đang làm gì với xác tàu vậy?",
                                "Hạm đội xác sống phản đòn bằng Axit, Bào Tử và Bầy Dơi khi trúng đạn.",
                                MotionStyle.DRAMATIC_SHAKE, ParticleMood.ZOMBIE_SPORE, 0x22C55E),
                        frame(ART_WAR_CLASH, StorySpeaker.KAI, "AN NGHỈ ĐI!",
                                "TIỄN HỌ VỀ ĐÁY BIỂN",
                                "Tàu ta hay tàu địch, họ đã chiến đấu xong rồi. Bắn chìm tất cả. Còn Lyra, ghi lại tín hiệu đó — ta có linh cảm xấu về Rãnh Vực Sâu.",
                                "Màn 6/100: Đánh chìm đội tàu ma trồi lên từ sương xanh.",
                                MotionStyle.ZOOM_OUT, ParticleMood.RAIN_TEARS, 0x38BDF8));
            }
            case 21 -> {
                return episode("ep_ch2_orykron", "CHƯƠNG II · KHỞI HÀNH",
                        "Mặt Trời Đen · Godzilla Thức Tỉnh",
                        "Sinh vật cổ dưới Rãnh Vực Sâu hấp thụ phóng xạ của bom Mặt Trời Đen và trồi lên mặt biển.",
                        false,
                        frame(ART_ORYKRON_RISE, StorySpeaker.NARRATOR, "BA NGÀY SAU",
                                "BIỂN AETHER HÓA BIỂN LỬA",
                                "Mây phóng xạ phủ kín bầu trời, mưa tro rơi xuống những con tàu còn sống sót. Khe dung nham mở ra khắp đáy biển quanh Rãnh Vực Sâu.",
                                "Chương II (Màn 21–40): Mặt Trời Đen.",
                                MotionStyle.ZOOM_OUT, ParticleMood.LAVA_EMBERS, 0xF97316),
                        frame(ART_ORYKRON_RISE, StorySpeaker.TITAN, "GRÀOOOOO!!",
                                "GODZILLA · VUA QUÁI VẬT",
                                "Một con thằn lằn biển khổng lồ xé sóng đứng dậy. Vảy đen nứt toác, các khe nứt phát sáng xanh lam như dung nham lạnh, đuôi gai xương xoắn như mũi khoan.",
                                "Nó đã ngủ dưới Rãnh Vực Sâu hàng vạn năm. Quả bom vừa đánh thức nó.",
                                MotionStyle.DRAMATIC_SHAKE, ParticleMood.ATOMIC_SPARKS, 0x38BDF8),
                        frame(ART_ORYKRON_RISE, StorySpeaker.VEGA, "CHỈ SỐ VƯỢT NGƯỠNG!",
                                "NÓ ĐANG ĂN PHÓNG XẠ",
                                "Máy đo phóng xạ quanh nó tụt dần về không… Sếp, nó đang hút sạch phóng xạ của Mặt Trời Đen! Càng nhiều bom, nó càng lớn!",
                                "Giọng Vega run lên lần đầu tiên kể từ ngày chiến tranh nổ ra.",
                                MotionStyle.PAN_LEFT, ParticleMood.ATOMIC_SPARKS, 0xF59E0B),
                        frame(ART_ORYKRON_BEAM, StorySpeaker.KAI, "KHÔNG LÙI!",
                                "BẢO VỆ TÂN HẢI",
                                "Phía sau chúng ta là cả thành phố. Không cần biết ai tạo ra nó — hôm nay nó sẽ không bước lên bờ!",
                                "Màn 21/100: Vượt Biển Lửa, chặn đường Godzilla tiến về Tân Hải.",
                                MotionStyle.ZOOM_IN, ParticleMood.LAVA_EMBERS, 0xF97316));
            }
            case 41 -> {
                return episode("ep_ch3_vesperax", "CHƯƠNG III · KHỞI HÀNH",
                        "Thiên Thạch Tinh Thể · King Ghidorah",
                        "Năng lượng hạch của Godzilla hút một thiên thạch tinh thể rơi xuống biển — bên trong có thứ đang ngủ đông.",
                        false,
                        frame(ART_METEOR_FALL, StorySpeaker.NARRATOR, "ĐÊM KHÔNG TRĂNG",
                                "BẦU TRỜI NỨT RA MỘT VỆT TÍM",
                                "Hai tuần sau ngày ngừng bắn, trạm quỹ đạo phát hiện một thiên thạch tinh thể đổi hướng, lao thẳng về biển Aether như bị thứ gì đó kéo xuống.",
                                "Chương III (Màn 41–60): Thiên Thạch Tinh Thể.",
                                MotionStyle.ZOOM_OUT, ParticleMood.GALAXY_STARS, 0xA855F7),
                        frame(ART_METEOR_FALL, StorySpeaker.LYRA, "VA CHẠM!",
                                "THỨ GÌ ĐÓ BỊ HÚT VỀ",
                                "Quỹ đạo của nó cong đúng về phía Rãnh Vực Sâu… Đô đốc, năng lượng hạch của Godzilla đang hút thiên thạch như nam châm!",
                                "Mặt biển đêm bừng sáng tím trong một khắc rồi tối sầm.",
                                MotionStyle.DRAMATIC_SHAKE, ParticleMood.STORM_LIGHTNING, 0x2DD4BF),
                        frame(ART_VESPERAX, StorySpeaker.TITAN, "XÌÌÌÌ…!!",
                                "KING GHIDORAH · RỒNG VÀNG BA ĐẦU",
                                "Từ lòng thiên thạch vỡ, một con rồng vàng ba đầu dang đôi cánh khổng lồ bay lên. Ba cái cổ dài uốn lượn, sấm sét vàng tuôn ra từ cả ba miệng.",
                                "Mỗi lần nó rít lên, la bàn của cả hạm đội lại quay loạn.",
                                MotionStyle.PAN_RIGHT, ParticleMood.GALAXY_STARS, 0xA855F7),
                        frame(ART_VESPERAX, StorySpeaker.KAI, "TOÀN HẠM ĐỘI!",
                                "THÊM MỘT CON QUÁI VẬT TỪ TRÊN TRỜI",
                                "Vừa sống sót qua một con, giờ lại thêm một con từ trên trời. Lyra, bám theo nó. Vega, tìm điểm yếu. Biển này không phải sân chơi của chúng!",
                                "Màn 41/100: Truy theo dấu King Ghidorah giữa mưa thiên thạch.",
                                MotionStyle.ZOOM_IN, ParticleMood.GALAXY_STARS, 0xA855F7));
            }
            case 50 -> {
                return episode("ep_stage_50_clash", "CHƯƠNG III · ẢI TRÙM 50",
                        "Hai Titan Va Chạm",
                        "Godzilla trồi lên giành lãnh thổ với King Ghidorah; vụ nổ va chạm thổi tế bào Godzilla lên quỹ đạo.",
                        false,
                        frame(ART_TITAN_CLASH, StorySpeaker.TITAN, "GRÀOOO!! XÌÌÌ!!",
                                "GODZILLA TRỒI LÊN GIÀNH LÃNH THỔ",
                                "Godzilla xé mặt biển lao vào King Ghidorah. Tia nguyên tử xanh lam đâm thẳng vào lớp vảy vàng — bầu trời đêm sáng như ban ngày.",
                                "Hai con quái vật quấn lấy nhau giữa biển, sóng cao hơn cả cột buồm.",
                                MotionStyle.DRAMATIC_SHAKE, ParticleMood.STORM_LIGHTNING, 0xA855F7),
                        frame(ART_TITAN_CLASH, StorySpeaker.VEGA, "BẤT THƯỜNG!",
                                "TẾ BÀO BỊ THỔI LÊN QUỸ ĐẠO",
                                "Vụ nổ va chạm vừa thổi một mảng tế bào của Godzilla lên tận quỹ đạo! Còn bào tử thiên thạch thì đang rơi như mưa xuống… nghĩa địa tàu chìm.",
                                "Vega ghi vội tọa độ, linh cảm có điều tồi tệ sắp đến.",
                                MotionStyle.PAN_LEFT, ParticleMood.GALAXY_STARS, 0xF59E0B),
                        frame(ART_VESPERAX, StorySpeaker.LYRA, "GIÁP NGỰC NỨT!",
                                "CƠ HỘI DUY NHẤT",
                                "Godzilla vừa đánh nứt giáp ngực của King Ghidorah! Bắn trúng ĐẦU ngay bây giờ thì khiên chặn tên lửa của nó sẽ sập!",
                                bossCaption(st),
                                MotionStyle.ZOOM_IN, ParticleMood.STORM_LIGHTNING, 0x2DD4BF),
                        frame(ART_TITAN_CLASH, StorySpeaker.KAI, "KHAI HỎA!",
                                "HẠ KING GHIDORAH TRƯỚC KHI GODZILLA QUAY LẠI",
                                "Raskov, nếu ông còn muốn có quê hương thì cho tàu của ông bắn cùng hướng với ta! Toàn hạm đội, nhắm vào ba cái đầu!",
                                "Nhiệm vụ Màn 50: Hạ gục King Ghidorah.",
                                MotionStyle.ZOOM_OUT, ParticleMood.GALAXY_STARS, 0xEF4444));
            }
            case 61 -> {
                return episode("ep_ch4_graveyard", "CHƯƠNG IV · KHỞI HÀNH",
                        "Nghĩa Địa Tàu Chìm Sống Lại",
                        "Oxygen Destroyer diệt được SpaceGodzilla, nhưng bào tử thiên thạch dưới nghĩa địa tàu chìm đã đánh thức hạm đội xác sống.",
                        false,
                        frame(ART_OMEGA_CORE, StorySpeaker.NARRATOR, "OXYGEN DESTROYER KÍCH HOẠT",
                                "MỘT TIA SÁNG TRẮNG XÉ BẦU TRỜI",
                                "Oxygen Destroyer được phóng lên quỹ đạo. SpaceGodzilla tan thành bụi tinh thể chỉ trong một khắc. Cả hạm đội reo hò — trừ Vega.",
                                "Phần dư của lõi rơi xuống biển như tuyết trắng.",
                                MotionStyle.ZOOM_OUT, ParticleMood.GOLDEN_DAWN, 0xF59E0B),
                        frame(ART_OMEGA_CORE, StorySpeaker.NARRATOR, "ĐÁY BIỂN SÂU",
                                "NGHĨA ĐỊA CỦA MỌI CUỘC CHIẾN",
                                "Gần tàn tích Atlantis là nghĩa địa tàu chìm của cuộc chiến Aether. Bào tử thiên thạch rơi xuống từ trận Màn 50 đã bám kín từng xác tàu, từng bộ quân phục.",
                                "Chương IV (Màn 61–80): Nghĩa Địa Tàu Chìm.",
                                MotionStyle.PAN_RIGHT, ParticleMood.ATLANTIS_BUBBLES, 0x2DD4BF),
                        frame(ART_ZOMBIE_FLEET, StorySpeaker.LYRA, "TÍN HIỆU CŨ!",
                                "NHỮNG CON TÀU TRỒI LÊN",
                                "Sonar đầy tín hiệu… Xác tàu đang trồi lên, cả tàu ta lẫn tàu Vel'Kar. Đèn mạn vẫn sáng, pháo vẫn xoay. Chúng đang hướng về phía chúng ta.",
                                "Khu Trục Dịch Hạch, Tàu Ngầm Thối Rữa, Tuần Dương Tử Linh, Thiết Giáp Huyết Hải…",
                                MotionStyle.DRAMATIC_SHAKE, ParticleMood.ZOMBIE_SPORE, 0x22C55E),
                        frame(ART_ZOMBIE_FLEET, StorySpeaker.KAI, "GIỮ VỮNG TAY LÁI!",
                                "BẮN VÀO NHỮNG NGƯỜI TỪNG LÀ ĐỒNG ĐỘI",
                                "Họ đã chết một lần vì cuộc chiến này. Đừng để bào tử bắt họ chết thêm lần nữa. Toàn hạm đội — tiễn họ về!",
                                "Màn 61/100: Phá vòng vây của hạm đội xác sống.",
                                MotionStyle.ZOOM_IN, ParticleMood.RAIN_TEARS, 0x38BDF8));
            }
            case 70 -> {
                return episode("ep_stage_70_aurora", "CHƯƠNG IV · TẬP 70",
                        "AURORA-07 · Anh Về Nhà Rồi",
                        "Mẫu hạm xác sống AURORA-07 là tàu của anh trai Lyra; cô phát lại mật mã Morse cũ của anh.",
                        false,
                        frame(ART_AURORA07, StorySpeaker.NARRATOR, "MẪU HẠM VỰC THẲM",
                                "AURORA-07 TRỒI LÊN TỪ ĐÁY SÂU",
                                "Chiếc mẫu hạm lớn nhất nghĩa địa phá mặt nước, thân phủ kín bào tử phát sáng. Trên mạn tàu, số hiệu AURORA-07 vẫn còn đọc được.",
                                "Mẫu Hạm Vực Thẳm — tàu chỉ huy của cả hạm đội xác sống.",
                                MotionStyle.ZOOM_IN, ParticleMood.ZOMBIE_SPORE, 0x22C55E),
                        frame(ART_AURORA07, StorySpeaker.LYRA, "…ANH ƠI?",
                                "TÀU CỦA ANH TRAI LYRA",
                                "Đó là tàu của anh em… Anh ấy chìm cùng AURORA-07 trong trận đầu tiên của cuộc chiến. Em vẫn nhớ mật mã Morse anh dạy em hồi nhỏ.",
                                "Bàn tay Lyra run lên trên phím phát tín hiệu.",
                                MotionStyle.PAN_LEFT, ParticleMood.RAIN_TEARS, 0x2DD4BF),
                        frame(ART_AURORA07, StorySpeaker.MEMORY, "TÍC… TÍC TÍC…",
                                "MẬT MÃ CŨ VỌNG XUỐNG ĐÁY BIỂN",
                                "Lyra phát lại đoạn mật mã cũ. Một khắc im lặng. Rồi từ AURORA-07, đèn tín hiệu nhấp nháy đáp lại — đúng nhịp mà chỉ hai anh em biết.",
                                "Pháo của mẫu hạm khựng lại. Lyra thì thầm: \"Anh về nhà rồi.\"",
                                MotionStyle.ZOOM_IN, ParticleMood.MEMORY_MOTES, 0x94A3B8),
                        frame(ART_AURORA07, StorySpeaker.KAI, "LẦN CUỐI",
                                "GIÚP ANH ẤY NGỦ YÊN",
                                "Bào tử vẫn điều khiển đội hộ tống. Hạ hết chúng, để AURORA-07 tự chọn đường về đáy biển. Lyra… ta ở ngay đây.",
                                "Nhiệm vụ Màn 70: Đánh chìm đội hộ tống của AURORA-07.",
                                MotionStyle.ZOOM_OUT, ParticleMood.RAIN_TEARS, 0x38BDF8));
            }
            case 81 -> {
                return episode("ep_ch5_lumira", "CHƯƠNG V · KHỞI HÀNH",
                        "Kết Giới Mothra · Hai Lá Cờ Một Hướng",
                        "Liên Minh Tân Hải và Vel'Kar bắt tay; Mothra, hải thú bảo hộ cổ xưa, bay lên che chở hạm đội.",
                        false,
                        frame(ART_LUMIRA, StorySpeaker.RASKOV, "KÊNH CHUNG",
                                "HẠM ĐỘI VEL'KAR XIN THAM CHIẾN",
                                "Đô Đốc Kai, ta đã tạo ra con quái vật này bằng một mệnh lệnh. Hãy để hạm đội Vel'Kar chiến đấu bên cạnh các người — ít nhất một lần, cho đúng.",
                                "Tàu hai phe xếp chung một đội hình lần đầu tiên trong lịch sử biển Aether.",
                                MotionStyle.ZOOM_OUT, ParticleMood.CALM_SPRAY, 0xDC2626),
                        frame(ART_LUMIRA, StorySpeaker.NARRATOR, "ÁNH SÁNG DƯỚI NƯỚC",
                                "BIỂN SÁNG LÊN TỪ BÊN DƯỚI",
                                "Gần Rãnh Vực Sâu, mặt biển bỗng phát sáng dịu như có ngàn ngọn đèn lồng chìm bên dưới. Những vòng kết giới mờ ảo nổi lên quanh hạm đội.",
                                "Chương V (Màn 81–100): Kết Giới Mothra.",
                                MotionStyle.PAN_RIGHT, ParticleMood.SAKURA_MAGIC, 0xF472B6),
                        frame(ART_LUMIRA, StorySpeaker.LUMIRA, "…",
                                "MOTHRA · NỮ HOÀNG ÁNH SÁNG",
                                "Ta đã canh giấc ngủ của nó từ trước khi các ngươi biết đóng thuyền. Các ngươi đánh thức nó bằng lửa. Giờ hãy cùng ta ru nó ngủ lại.",
                                "Kết Giới của Mothra giảm hồi chiêu và hồi khiên cho tàu đồng minh.",
                                MotionStyle.ZOOM_IN, ParticleMood.SAKURA_MAGIC, 0x6EE7B7),
                        frame(ART_LUMIRA, StorySpeaker.KAI, "CÙNG MỘT HƯỚNG!",
                                "20 ẢI CUỐI CÙNG",
                                "Tân Hải, Vel'Kar, và một sinh vật già hơn cả biển. Nếu sau trận này chúng ta còn bắn nhau, thì chúng ta không xứng được cứu. Tiến lên!",
                                "Màn 81/100: Hộ tống Mothra tiến về Rãnh Vực Sâu.",
                                MotionStyle.ZOOM_IN, ParticleMood.GOLDEN_DAWN, 0x38BDF8));
            }
            case 100 -> {
                return episode("ep_stage_100_finale", "ĐẠI CHIẾN TỐI THƯỢNG · MÀN 100",
                        "Godzilla Ultima",
                        "Giai đoạn cuối: vảy đỏ thẫm, hào quang nhiều màu, mang chiêu của mọi quái vật trước nó.",
                        false,
                        frame(ART_ULTIMA, StorySpeaker.TITAN, "GRÀOOOOOOO!!",
                                "GIAI ĐOẠN IV · TỐI THƯỢNG",
                                "Vảy Godzilla chuyển sang đỏ thẫm, vầng hào quang nhiều màu bừng lên sau lưng, đuôi phân rã như Destoroyah. Nó mang trong mình mọi thứ con người từng ném xuống biển.",
                                bossCaption(st),
                                MotionStyle.DRAMATIC_SHAKE, ParticleMood.STORM_LIGHTNING, 0xDC2626),
                        frame(ART_ULTIMA_VOID, StorySpeaker.LUMIRA, "KHIÊN ÁNH SÁNG",
                                "CHÚT ÁNH SÁNG CUỐI CÙNG",
                                "Ta sẽ giữ khiên cho các ngươi đến khi ánh sáng của ta cạn. Đừng giết nó… chỉ cần khiến nó mỏi mệt. Phần còn lại để ta.",
                                "Đôi cánh ánh sáng của Mothra dang rộng che cả hai hạm đội.",
                                MotionStyle.PAN_RIGHT, ParticleMood.SAKURA_MAGIC, 0x6EE7B7),
                        frame(ART_ULTIMA, StorySpeaker.RASKOV, "VEL'KAR KHAI HỎA!",
                                "SÁT CÁNH CÙNG TÂN HẢI",
                                "Toàn hạm đội Vel'Kar, bắn theo tọa độ của Tân Hải! Hôm nay chúng ta sửa lại mệnh lệnh tồi tệ nhất đời ta.",
                                "Tàu Vel'Kar áp sát sườn trái, pháo chính xoay cùng một hướng.",
                                MotionStyle.PAN_LEFT, ParticleMood.STORM_LIGHTNING, 0xDC2626),
                        frame(ART_ULTIMA, StorySpeaker.KAI, "TOÀN LỰC!",
                                "VÌ BIỂN AETHER — MÀN 100/100",
                                "Lyra, khóa tọa độ ĐẦU. Vega, dồn hết năng lượng cho pháo chính. Không phải để giết nó — để kết thúc thứ mà chính chúng ta đã bắt đầu!",
                                "Nhiệm vụ Cuối: Đánh bại Godzilla Ultima tại Màn 100/100.",
                                MotionStyle.ZOOM_IN, ParticleMood.GOLDEN_DAWN, 0xEF4444));
            }
            default -> { }
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
            case 20 -> episode("ep_after_20", "HỒI KẾT CHƯƠNG I",
                    "Mặt Trời Đen",
                    "Bị dồn đến đường cùng, Raskov ra lệnh thả bom hạch thử nghiệm xuống Rãnh Vực Sâu.",
                    true,
                    frame(ART_RASKOV_FLAGSHIP, StorySpeaker.RASKOV, "LỆNH TỐI MẬT",
                            "KÍCH HOẠT \"MẶT TRỜI ĐEN\"",
                            "Soái hạm của ta đã chìm, nhưng Vel'Kar chưa thua. Thả \"Mặt Trời Đen\" xuống Rãnh Vực Sâu. Nếu Vel'Kar không có được mỏ năng lượng, thì Tân Hải cũng đừng hòng.",
                            "Một tàu ngầm Vel'Kar lặng lẽ lặn xuống, mang theo quả bom chưa từng được thử.",
                            MotionStyle.ZOOM_IN, ParticleMood.STORM_LIGHTNING, 0xDC2626),
                    frame(ART_BLACK_SUN, StorySpeaker.NARRATOR, "ẦMMMMM!!",
                            "BIỂN SÔI, CỘT NƯỚC PHÁT SÁNG XANH",
                            "Một quầng sáng xanh nở ra dưới đáy biển. Cột nước cao hàng trăm mét vọt lên, sóng xung kích hất nghiêng cả hai hạm đội như những chiếc lá.",
                            "Mặt biển sôi sùng sục suốt nhiều giờ liền.",
                            MotionStyle.DRAMATIC_SHAKE, ParticleMood.ATOMIC_SPARKS, 0x38BDF8),
                    frame(ART_BLACK_SUN, StorySpeaker.LYRA, "IM LẶNG…",
                            "CÓ THỨ GÌ ĐÓ ĐANG THỞ",
                            "Sonar… Đô đốc, nhịp đập dưới đó không tắt, nó còn mạnh hơn trước. Có thứ gì đó đang thở dưới Rãnh Vực Sâu — và nó vừa thức dậy.",
                            "Mở khóa Chương II · Mặt Trời Đen (Màn 21–40).",
                            MotionStyle.PAN_LEFT, ParticleMood.MEMORY_MOTES, 0x2DD4BF));
            case 40 -> episode("ep_after_40", "HỒI KẾT CHƯƠNG II",
                    "Ngừng Bắn Dưới Mưa Tro",
                    "Godzilla chìm xuống nhưng chưa chết; hai hạm đội buộc phải tạm ngừng bắn.",
                    true,
                    frame(ART_ORYKRON_SINK, StorySpeaker.NARRATOR, "RẦMMM…",
                            "HẠCH LONG CHÌM XUỐNG VỰC",
                            "Các khe sáng trên lưng Godzilla tắt dần. Nó gầm một tiếng trầm dài rồi lặn xuống Rãnh Vực Sâu, để lại xoáy nước rộng cả hải lý.",
                            "Không ai dám chắc nó đã chết.",
                            MotionStyle.ZOOM_OUT, ParticleMood.ATOMIC_SPARKS, 0x38BDF8),
                    frame(ART_ORYKRON_SINK, StorySpeaker.RASKOV, "KÊNH VEL'KAR",
                            "LỜI ĐỀ NGHỊ NGỪNG BẮN",
                            "Đô Đốc Kai… hạm đội của ta còn chưa tới một nửa. Ta đề nghị ngừng bắn — không phải vì hòa bình, mà vì cả hai ta đều không đủ sức đánh thêm trận nào nữa.",
                            "Lần đầu tiên, hai lá cờ được hạ xuống cùng lúc.",
                            MotionStyle.PAN_RIGHT, ParticleMood.RAIN_TEARS, 0xDC2626),
                    frame(ART_ORYKRON_FEED, StorySpeaker.VEGA, "KẾT QUẢ XÉT NGHIỆM",
                            "TẾ BÀO NÀY KHÔNG CHẾT",
                            "Tôi đã đốt, đã chiếu xạ, đã đông lạnh mẫu máu của nó… Sếp, tế bào này không chết. Nó chỉ chờ.",
                            "Mở khóa Chương III · Thiên Thạch Tinh Thể (Màn 41–60).",
                            MotionStyle.ZOOM_IN, ParticleMood.MEMORY_MOTES, 0xF59E0B));
            case 60 -> episode("ep_after_60", "HỒI KẾT CHƯƠNG III",
                    "Oxygen Destroyer",
                    "SpaceGodzilla gọi Godzilla gốc như gọi anh em; Vega đề xuất siêu vũ khí cuối cùng.",
                    true,
                    frame(ART_ORYKRON_SIGMA, StorySpeaker.TITAN, "UUUUÔÔÔ…",
                            "SPACEGODZILLA GỌI ANH EM CỦA NÓ",
                            "Bị thương nặng, SpaceGodzilla ngửa cổ gầm một tiếng dài xuống biển sâu. Từ Rãnh Vực Sâu, một tiếng gầm khác đáp lại.",
                            "Hai cột tinh thể trên vai SpaceGodzilla rung lên cùng nhịp với tiếng gầm dưới đáy biển.",
                            MotionStyle.DRAMATIC_SHAKE, ParticleMood.GALAXY_STARS, 0xA855F7),
                    frame(ART_ORYKRON_SIGMA, StorySpeaker.VEGA, "DỰ ÁN TỐI MẬT",
                            "OXYGEN DESTROYER",
                            "Nếu hai con hợp sức thì không gì cản nổi. Tôi có một thiết kế… Oxygen Destroyer. Nó hủy oxy và xé mọi tế bào sống ở cấp phân tử, kể cả tế bào không chịu chết.",
                            "Lõi phát sáng trên tay Vega hắt lên gương mặt căng thẳng của cô.",
                            MotionStyle.ZOOM_IN, ParticleMood.ATOMIC_SPARKS, 0xF59E0B),
                    frame(ART_ORYKRON_SIGMA, StorySpeaker.KAI, "QUYẾT ĐỊNH",
                            "DÙNG NÓ",
                            "Làm đi, Vega. Ta chịu trách nhiệm.",
                            "Kai không nhìn vào lõi sáng — ông nhìn ra biển.",
                            MotionStyle.PAN_RIGHT, ParticleMood.MEMORY_MOTES, 0x38BDF8),
                    frame(ART_TITAN_CLASH, StorySpeaker.RASKOV, "LỜI CẢNH BÁO",
                            "NGƯỜI TỪNG THẢ BOM",
                            "Tôi cũng từng nghĩ quả bom của mình sẽ kết thúc chiến tranh, Đô Đốc Kai. Hãy nhìn xem nó đã bắt đầu thứ gì.",
                            "Mở khóa Chương IV · Nghĩa Địa Tàu Chìm (Màn 61–80).",
                            MotionStyle.ZOOM_OUT, ParticleMood.RAIN_TEARS, 0xDC2626));
            case 80 -> episode("ep_after_80", "HỒI KẾT CHƯƠNG IV",
                    "Godzilla Nuốt Trọn Mọi Thứ",
                    "Destoroyah bị diệt, nhưng Godzilla nuốt hết phần năng lượng còn lại rồi biến mất xuống rãnh sâu nhất.",
                    true,
                    frame(ART_NECROSYS, StorySpeaker.NARRATOR, "RẮC… RẮC…",
                            "BẦY PHÂN RÃ TAN RÃ",
                            "Destoroyah vỡ ra thành hàng vạn mảnh giáp đỏ sẫm, chìm dần xuống đáy. Sương Oxy Đen tan đi, ánh sáng lần đầu chạm tới nghĩa địa tàu chìm.",
                            "Hạm đội xác sống cũng ngừng cử động theo.",
                            MotionStyle.ZOOM_OUT, ParticleMood.ATLANTIS_BUBBLES, 0xDC2626),
                    frame(ART_NECROSYS, StorySpeaker.VEGA, "XOẸT…",
                            "BẢN THIẾT KẾ CUỐI CÙNG",
                            "Tôi đã lặp lại đúng sai lầm của Mặt Trời Đen. Bản thiết kế Oxygen Destroyer… không ai được dùng nó nữa, kể cả tôi.",
                            "Vega tự tay xé bản thiết kế, thả từng mảnh xuống biển.",
                            MotionStyle.ZOOM_IN, ParticleMood.RAIN_TEARS, 0xF59E0B),
                    frame(ART_NECROSYS, StorySpeaker.LYRA, "BÁO ĐỘNG!",
                            "NÓ ĐANG NUỐT HẾT",
                            "Sonar… Godzilla! Nó đang hút năng lượng còn sót của SpaceGodzilla, King Ghidorah và cả Destoroyah! Nó lặn xuống rãnh sâu nhất — và nó lớn hơn bao giờ hết.",
                            "Mở khóa Chương V · Kết Giới Mothra (Màn 81–100).",
                            MotionStyle.DRAMATIC_SHAKE, ParticleMood.ATOMIC_SPARKS, 0x2DD4BF));
            case 100 -> episode("ep_after_100_true_ending", "HỒI KẾT TỐI THƯỢNG · TRUE ENDING",
                    "Biển Chỉ Nhớ",
                    "Mothra ru Godzilla ngủ lại dưới Rãnh Vực Sâu; Tân Hải và Vel'Kar ký hiệp ước cấm vũ khí hạch.",
                    true,
                    frame(ART_ULTIMA, StorySpeaker.NARRATOR, "IM LẶNG…",
                            "HẠCH LONG KHUỴU XUỐNG",
                            "Hào quang quanh Godzilla tắt dần từng lớp. Nó không còn gầm nữa, chỉ thở chậm và nặng, như một sinh vật già đã quá mệt.",
                            "Godzilla bị đánh bại — nhưng không bị giết.",
                            MotionStyle.ZOOM_OUT, ParticleMood.MEMORY_MOTES, 0xDC2626),
                    frame(ART_LUMIRA, StorySpeaker.LUMIRA, "KHÚC RU",
                            "ÁNH SÁNG CUỐI CÙNG CỦA MOTHRA",
                            "Ngủ đi, đứa trẻ của biển sâu. Lần này sẽ không ai đánh thức ngươi nữa.",
                            "Mothra rắc phấn sáng lên Godzilla, đưa nó chìm dần xuống Rãnh Vực Sâu, rồi tan thành ngàn đốm sáng trên mặt nước.",
                            MotionStyle.PAN_RIGHT, ParticleMood.SAKURA_MAGIC, 0x6EE7B7),
                    frame(ART_LUMIRA, StorySpeaker.RASKOV, "HIỆP ƯỚC AETHER",
                            "HAI CHỮ KÝ TRÊN MỘT BOONG TÀU",
                            "Từ hôm nay, không một vũ khí hạch nào được thả xuống biển Aether nữa. Ta ký đầu tiên — vì ta là người đã thả quả đầu tiên.",
                            "Hiệp ước được ký trên boong tàu Tân Hải, giữa hai hàng thủy thủ của cả hai phe.",
                            MotionStyle.PAN_LEFT, ParticleMood.GOLDEN_DAWN, 0xFACC15),
                    frame(ART_LUMIRA, StorySpeaker.KAI, "BÌNH MINH",
                            "BIỂN KHÔNG THUỘC VỀ AI",
                            "Biển không thuộc về ai cả. Nó chỉ nhớ những gì ta đã ném xuống.",
                            "— HẾT CHÍNH TRUYỆN (100/100 MÀN) · CẢM ƠN ĐÔ ĐỐC ĐÃ ĐI CÙNG HẠM ĐỘI AETHER —",
                            MotionStyle.ZOOM_IN, ParticleMood.GOLDEN_DAWN, 0x38BDF8));
            default -> buildRegularStageEpisode(st);
        };
    }

    private static StoryEpisode buildBossMilestoneIntro(CampaignMode.CampaignStage st) {
        int num = st.stageNumber();
        TitanMode.TitanBoss boss = st.boss() != null ? st.boss() : TitanMode.TitanBoss.GODZILLA;
        String bossName = CampaignMode.storyBossName(num);
        CampaignMode.CampaignChapter chapter = st.chapter();
        List<String> arts = unlockedArts(chapter, num);
        String bossArt = switch (num) {
            case 25 -> ART_ORYKRON_RISE;
            case 30, 40 -> ART_ORYKRON_BEAM;
            case 35 -> ART_ORYKRON_FEED;
            case 45, 55 -> ART_VESPERAX;
            case 60 -> ART_ORYKRON_SIGMA;
            case 65 -> ART_OMEGA_CORE;
            case 75, 80 -> ART_NECROSYS;
            case 85 -> ART_LUMIRA;
            case 90 -> ART_ULTIMA_AWAKEN;
            case 95 -> ART_ULTIMA_VOID;
            default -> arts.get(arts.size() - 1);
        };
        ParticleMood mood = switch (chapter) {
            case CH1_NAVAL -> ParticleMood.CALM_SPRAY;
            case CH3_APOCALYPSE -> ParticleMood.ATOMIC_SPARKS;
            case CH4_GALAXY -> ParticleMood.GALAXY_STARS;
            case CH5_ATLANTIS -> ParticleMood.ZOMBIE_SPORE;
            case CH2_FANTASY -> ParticleMood.STORM_LIGHTNING;
        };
        String chapterArt = arts.get(num % arts.size());
        // Chiêu trong trận vẫn là của boss đóng thế, nên mẹo đánh chọn theo enum nhưng gọi bằng tên trong truyện.
        String bossTactic = switch (boss) {
            case GODZILLA -> "Năng lượng hạch đang dồn lên cổ họng " + bossName + "! Bắn gãy ĐẦU để ngắt tia nguyên tử chặn tên lửa, hoặc dựng Khiên lúc nó nạp đòn!";
            case GHIDORAH -> "Ba cái đầu của " + bossName + " hút cả trọng lực lẫn sấm sét quanh nó! Ưu tiên phá ĐẦU để vỡ khiên chặn tên lửa, rồi dồn hỏa lực vào thân!";
            case MOTHRA -> "Bầy " + bossName + " phủ Sương Oxy Đen chặn đạn và tự hồi phục! Đánh gãy ĐẦU để xé màn sương, rồi khoan sâu trước khi bầy kịp tách ra!";
            case SPACEGODZILLA -> "Hai cột tinh thể trên vai " + bossName + " là nguồn năng lượng của nó! Bắn vỡ ĐẦU để ngắt Tia Corona, và né vòng Pháo Đài Tinh Thể khi nó cuồng bạo!";
            case DESTOROYAH -> bossName + " tách bầy giáp xác và phun Tia Oxy Vi Thể! Bắn gãy ĐẦU để cắt sừng laser, đừng đậu tàu trong Sương Oxy Đen!";
            case ULTIMA -> bossName + " tiến hóa qua 4 giai đoạn theo máu (75/50/25%)! Mỗi giai đoạn đổi bộ chiêu — bắn ĐẦU để chặn tia sừng và hào quang!";
        };

        return episode("ep_boss_" + num,
                chapter.shortTab.toUpperCase() + " · ẢI TRÙM " + num,
                "Đối Đầu " + bossName + " (" + num + "/100)",
                "Biến cố cột mốc Màn " + num + ": chạm trán " + bossName + " (" + st.bossHpOverride() + " HP).",
                false,
                new StoryFrame(arts.get((num + 1) % arts.size()), StorySpeaker.LYRA, "MỐC TITAN " + num + "/100",
                        milestoneHeadline(num, st), milestoneLoreDiscovery(num, st), bossCaption(st),
                        MotionStyle.PAN_RIGHT, num >= 60 ? ParticleMood.MEMORY_MOTES : ParticleMood.RAIN_TEARS,
                        chapter.accent),
                new StoryFrame(bossArt, StorySpeaker.VEGA, bossName.toUpperCase() + " XUẤT HIỆN!",
                        "PHÂN TÍCH ĐIỂM YẾU & KỸ NĂNG PHÒNG NGỰ", bossTactic,
                        "Mẹo Chiến Thuật: Tấn công chuẩn xác bộ phận ĐẦU sẽ cắt đứt trạng thái Chống Tên Lửa của Boss!",
                        MotionStyle.DRAMATIC_SHAKE, mood, boss.accentColor),
                frame(chapterArt, StorySpeaker.KAI, "VÀO VỊ TRÍ CHIẾN ĐẤU!",
                        "KHÔNG ĐƯỢC LÙI BƯỚC",
                        "Tận dụng địa hình, giữ Khiên trước khi quái vật gồng đòn. Biển Aether không còn chỗ cho thêm một sai lầm nào nữa!",
                        "Nhiệm vụ Màn " + num + ": Đánh bại " + bossName + ".",
                        MotionStyle.ZOOM_IN, mood, 0xEF4444));
    }

    private static StoryEpisode buildRegularStageEpisode(CampaignMode.CampaignStage st) {
        int num = st.stageNumber();
        boolean isZombie = st.ruleType() == CampaignMode.StageRuleType.ZOMBIE_ASSAULT;
        // Xoay vòng trong các tranh của chương đã xuất hiện tới màn này (không lộ cảnh chưa tới).
        List<String> arts = unlockedArts(st.chapter(), num);
        String primaryArt = arts.get(num % arts.size());
        String reflectionArt = arts.get((num + 1) % arts.size());
        String finaleArt = arts.get((num + 2) % arts.size());
        ParticleMood primaryMood = isZombie ? ParticleMood.ZOMBIE_SPORE : moodForChapter(st.chapter());

        StorySpeaker secondSpeaker = switch (st.ruleType()) {
            case SNIPER_AMMO, BLITZ_TIMED -> StorySpeaker.VEGA;
            case SONAR_FOG, ZOMBIE_ASSAULT -> StorySpeaker.LYRA;
            default -> chapterConfidant(st.chapter());
        };
        String emotionalBeat = chapterReflectionLine(st, isZombie);
        String tacticalOrder = switch (st.ruleType()) {
            case SNIPER_AMMO -> "Kho đạn đang cạn dần (chỉ còn " + st.shotLimit()
                    + " phát tối đa). Từng viên đạn đều quý như sinh mạng đồng đội — hãy kết hợp Phép & Vũ Khí thật chuẩn!";
            case BLITZ_TIMED -> "Hạm đội địch sẽ rút khỏi tầm bắn sau " + st.timeLimitSec()
                    + " giây! Áp sát bằng Airstrike và Sét liên hoàn trước khi chúng thoát!";
            case SONAR_FOG -> "Màn sương che lấp cả kính ngắm, nhưng Phao Sonar đã bắt được " + st.initialSonarHints()
                    + " điểm nóng cùng " + st.initialMana() + " Hải Năng sẵn sàng khai hỏa!";
            case ZOMBIE_ASSAULT -> "Những thân tàu nhiễm bào tử lại trồi lên từ vực sâu! Cẩn thận phản đòn Axit, Thiên Thạch và Nguyền Rủa khi bắn trúng!";
            default -> "Tuyến hải trình Màn " + num + " bị phong tỏa bởi " + st.enemyFleetTypes().size()
                    + " chiến hạm địch. Giữ vững đội hình — không được mất thêm một con tàu nào nữa!";
        };

        return episode("ep_stage_" + num,
                st.chapter().shortTab.toUpperCase() + " · MÀN " + num,
                st.title(),
                emotionalBeat,
                false,
                new StoryFrame(primaryArt, StorySpeaker.NARRATOR, "HỒ SƠ MÀN " + num + "/100",
                        st.title().toUpperCase(), chapterAtmosphereLine(st, isZombie),
                        "Bối cảnh: " + st.chapter().theme.displayName + " · " + st.season().shortLabel + " · " + st.weather().shortLabel,
                        MotionStyle.PAN_RIGHT, primaryMood, st.chapter().accent),
                frame(reflectionArt, secondSpeaker, "NHẬT KÝ CHIẾN HẠM",
                        chapterMemoryHeadline(st.chapter(), isZombie), emotionalBeat,
                        "Mạch Truyện: " + st.chapter().fullTitle,
                        MotionStyle.ZOOM_IN, ParticleMood.MEMORY_MOTES, 0x94A3B8),
                new StoryFrame(finaleArt, StorySpeaker.KAI, st.ruleType().shortLabel.toUpperCase(),
                        "CHỈ THỊ CHIẾN THUẬT TRƯỚC GIỜ G", tacticalOrder, st.threeStarCondition(),
                        MotionStyle.PAN_LEFT, isZombie ? ParticleMood.RAIN_TEARS : primaryMood, st.ruleType().accent));
    }

    private static String milestoneHeadline(int num, CampaignMode.CampaignStage st) {
        return switch (num) {
            case 25 -> "VẢY ĐEN NỨT SÁNG XANH DƯỚI MẶT BIỂN SÔI";
            case 30 -> "TIA NGUYÊN TỬ XÉ ĐÔI ĐƯỜNG CHÂN TRỜI";
            case 35 -> "HAI HẠM ĐỘI CÙNG BỊ DỒN VÀO GÓC";
            case 40 -> "ĐỨNG CHẮN TRƯỚC CỬA RÃNH VỰC SÂU";
            case 45 -> "TINH THỂ TÍM ĐÂM XUYÊN QUA SÓNG";
            case 55 -> "CƠN THỊNH NỘ CUỐI CÙNG CỦA KING GHIDORAH";
            case 60 -> "BẢN SAO LAI GIỮA VÀNH ĐAI THIÊN THẠCH";
            case 65 -> "BẦY GIÁP XÁC ĐỎ SẪM DƯỚI NGHĨA ĐỊA TÀU";
            case 75 -> "BẦY ĐÀN MỌC CÁNH MÀNG, CÀNG NHƯ LƯỠI HÁI";
            case 80 -> "PHÂN RÃ HAY BỊ PHÂN RÃ";
            case 85 -> "GIAI ĐOẠN I · ẤU LONG DƯỚI LÀN NƯỚC";
            case 90 -> "GIAI ĐOẠN II · THỨC TỈNH";
            case 95 -> "GIAI ĐOẠN III · HƯ KHÔNG";
            default -> "BIẾN CỐ CỘT MỐC TẠI " + st.chapter().shortTab.toUpperCase();
        };
    }

    private static String milestoneLoreDiscovery(int num, CampaignMode.CampaignStage st) {
        return switch (num) {
            case 25 -> "Biển quanh Rãnh Vực Sâu vẫn sôi sau quả bom Mặt Trời Đen. Sonar ghi được nhịp thở dài bốn mươi giây — Godzilla đang trồi lên lần đầu tiên!";
            case 30 -> "Một luồng sáng xanh lam quét ngang biển, cắt đôi cả tàu Tân Hải lẫn tàu Vel'Kar. Godzilla không chọn phe — nó chỉ trả lại thứ đã bị ném xuống.";
            case 35 -> "Raskov gửi tín hiệu xin tạm ngừng bắn. Lần đầu tiên từ khi chiến tranh nổ ra, hai hạm đội quay pháo về cùng một hướng.";
            case 40 -> "Godzilla (" + st.bossHpOverride() + " HP) dựng toàn bộ khe vảy phát sáng. Nếu nó vượt qua đây, cả cảng Tân Hải lẫn thủ đô Vel'Kar đều nằm trong tầm tia nguyên tử!";
            case 45 -> "King Ghidorah bay vòng quanh mảnh thiên thạch vỡ, ba cái đầu gầm rít cùng lúc. Mỗi lần nó rít lên, la bàn của cả hạm đội lại quay loạn.";
            case 55 -> "Ba cái đầu của King Ghidorah đều đã bị thương sau trận va chạm với Godzilla. Nhưng trên quỹ đạo, radar của Vega bắt được một khối tế bào xanh đang kết tinh màu tím…";
            case 60 -> "SpaceGodzilla (" + st.bossHpOverride() + " HP) dựng hai cột tinh thể trên vai và cất tiếng gọi xuống biển sâu. Nếu Godzilla gốc đáp lời, sẽ không còn gì cản nổi chúng!";
            case 65 -> "Phần dư của Oxygen Destroyer đã lắng xuống đáy biển. Hàng vạn con giáp xác đỏ sẫm bò ra từ xác tàu, kết lại thành một khối khổng lồ biết thở.";
            case 75 -> "Destoroyah đã hợp thể thành một quỷ giáp xác đỏ sẫm đứng thẳng, cánh dơi khổng lồ, sừng lớn trên đầu. Vega lặng người: mọi vết bỏng trên vỏ nó đều mang đúng tần số của Oxygen Destroyer do cô thiết kế.";
            case 80 -> "Destoroyah (" + st.bossHpOverride() + " HP) phủ Sương Oxy Đen khắp vực sâu. Vega đã tự tay hủy bản thiết kế Oxygen Destroyer — lần này hạm đội phải thắng bằng chính đôi tay mình.";
            case 85 -> "Chỉ có dải vây lưng phát sáng lướt qua mặt nước. Godzilla Ultima đang ẩn mình — nó chỉ lộ vị trí khi trúng đạn! Hạm đội Vel'Kar của Raskov đã vào đội hình bên sườn trái.";
            case 90 -> "Godzilla Ultima bò lên bãi đá ngầm bằng bốn chân, vảy chuyển sang cam đỏ, gai lưng mọc dày. Tia nguyên tử của nó giờ quét trọn một hàng tàu.";
            case 95 -> "Tinh thể tím của SpaceGodzilla mọc khắp thân Godzilla Ultima, sừng trắng nhô trên đầu. Mothra dang rộng đôi cánh ánh sáng, dựng khiên che cả hai hạm đội Tân Hải và Vel'Kar.";
            default -> "Áp suất đại dương sụt mạnh! " + CampaignMode.storyBossName(num) + " (" + st.bossHpOverride() + " HP · "
                    + st.bossDifficulty().label + ") đang lao thẳng vào đội hình hạm đội tại " + st.title() + "!";
        };
    }

    private static StorySpeaker chapterConfidant(CampaignMode.CampaignChapter chapter) {
        return switch (chapter) {
            case CH1_NAVAL -> StorySpeaker.KAI;
            case CH3_APOCALYPSE -> StorySpeaker.VEGA;
            case CH4_GALAXY -> StorySpeaker.LYRA;
            case CH5_ATLANTIS -> StorySpeaker.MEMORY;
            case CH2_FANTASY -> StorySpeaker.LUMIRA;
        };
    }

    private static String chapterAtmosphereLine(CampaignMode.CampaignStage st, boolean isZombie) {
        if (isZombie) {
            return st.stageNumber() < 50
                    ? "Dòng nước lạ từ Rãnh Vực Sâu lại kéo xác tàu chìm trồi lên, mạn thép còn vương ánh xanh mờ như có ai đang thở bên trong."
                    : "Bào tử thiên thạch bám kín những xác tàu chìm, kéo chúng trồi lên giữa sương đêm — chiếc mang cờ Tân Hải, chiếc mang cờ Vel'Kar.";
        }
        return switch (st.chapter()) {
            case CH1_NAVAL -> "Hải đồ " + st.title() + " chi chít chấm đỏ của hạm đội Vel'Kar — mỏ năng lượng dưới Rãnh Vực Sâu đã biến biển Aether thành chiến trường.";
            case CH3_APOCALYPSE -> "Biển quanh " + st.title() + " vẫn sôi sau bom Mặt Trời Đen, hơi nước phóng xạ bốc lên che kín nửa bầu trời.";
            case CH4_GALAXY -> "Mưa thiên thạch tím rạch ngang trời đêm, từng mảnh tinh thể rơi xuống biển kéo theo vệt sáng lạnh lẽo.";
            case CH5_ATLANTIS -> "Hai vách nước Atlantis dựng đứng, bên dưới là nghĩa địa tàu chìm trải dài — nơi tàu của cả hai phe nằm cạnh nhau.";
            case CH2_FANTASY -> "Kết Giới Mothra tỏa ánh sáng dịu trên mặt biển, hạm đội Tân Hải và Vel'Kar xếp chung một đội hình.";
        };
    }

    private static String chapterMemoryHeadline(CampaignMode.CampaignChapter chapter, boolean isZombie) {
        if (isZombie) return "NHỮNG CON TÀU KHÔNG CHỊU NẰM YÊN";
        return switch (chapter) {
            case CH1_NAVAL -> "MỎ NĂNG LƯỢNG ĐÁNG GIÁ BAO NHIÊU MẠNG NGƯỜI";
            case CH3_APOCALYPSE -> "TẾ BÀO NÀY KHÔNG CHẾT";
            case CH4_GALAXY -> "THỨ ĐẾN TỪ NGOÀI BẦU TRỜI";
            case CH5_ATLANTIS -> "VŨ KHÍ MỚI, SAI LẦM CŨ";
            case CH2_FANTASY -> "ÁNH SÁNG CUỐI CÙNG CỦA BIỂN SÂU";
        };
    }

    private static String chapterReflectionLine(CampaignMode.CampaignStage st, boolean isZombie) {
        if (isZombie) {
            return "Đừng nhìn số hiệu trên thân tàu kẻo run tay, Đô đốc… Tàu Tân Hải hay Vel'Kar, giờ họ chỉ cần được về đáy biển yên lành.";
        }
        return switch (st.chapter()) {
            case CH1_NAVAL -> "Radio chặn được giọng Raskov: \"Rút khỏi Rãnh Vực Sâu, Tân Hải. Lần sau ta sẽ không dùng đạn thường nữa.\" Không ai trên boong nghĩ ông ta nói thật.";
            case CH3_APOCALYPSE -> "Mẫu vảy Godzilla dưới kính hiển vi vẫn đang lớn lên. Nó sống nhờ chính phóng xạ của chúng ta — mỗi quả bom là một bữa ăn.";
            case CH4_GALAXY -> "Dưới đáy biển, Godzilla đang gầm đáp lại tiếng rít của thứ đến từ trên trời. Sonar nghe rõ: hai con quái vật đang gọi nhau.";
            case CH5_ATLANTIS -> "Vega ôm bản thiết kế Oxygen Destroyer cả đêm không ngủ: \"Tôi đã làm đúng điều Raskov từng làm… chỉ là với một cái tên đẹp hơn.\"";
            case CH2_FANTASY -> "Ánh sáng của Mothra yếu dần sau mỗi trận. Nó đang dùng chính sự sống của mình để che chở cho cả hai hạm đội.";
        };
    }

    /** 4 tranh riêng của chương (enum giữ tên cũ, thứ tự truyện: I Hải chiến … V Kết Giới Mothra). */
    static List<String> chapterArts(CampaignMode.CampaignChapter chapter) {
        return switch (chapter == null ? CampaignMode.CampaignChapter.CH1_NAVAL : chapter) {
            case CH1_NAVAL -> List.of(ART_WAR_CLASH, ART_RASKOV_FLAGSHIP, ART_GHOST_FLEET, ART_BLACK_SUN);
            case CH3_APOCALYPSE -> List.of(ART_ORYKRON_RISE, ART_ORYKRON_BEAM, ART_ORYKRON_FEED, ART_ORYKRON_SINK);
            case CH4_GALAXY -> List.of(ART_METEOR_FALL, ART_VESPERAX, ART_TITAN_CLASH, ART_ORYKRON_SIGMA);
            case CH5_ATLANTIS -> List.of(ART_OMEGA_CORE, ART_ZOMBIE_FLEET, ART_AURORA07, ART_NECROSYS);
            case CH2_FANTASY -> List.of(ART_LUMIRA, ART_ULTIMA_AWAKEN, ART_ULTIMA_VOID, ART_ULTIMA);
        };
    }

    /** Màn đầu tiên mỗi tranh xuất hiện; khớp thứ tự ảnh 1–20. Tranh chỉ dùng ở hồi kết thì mở khoá sau màn đó. */
    private static final Map<String, Integer> ART_UNLOCK = Map.ofEntries(
            Map.entry(ART_WAR_CLASH, 1), Map.entry(ART_RASKOV_FLAGSHIP, 1), Map.entry(ART_GHOST_FLEET, 6), Map.entry(ART_BLACK_SUN, 21),
            Map.entry(ART_ORYKRON_RISE, 21), Map.entry(ART_ORYKRON_BEAM, 21), Map.entry(ART_ORYKRON_FEED, 35), Map.entry(ART_ORYKRON_SINK, 41),
            Map.entry(ART_METEOR_FALL, 41), Map.entry(ART_VESPERAX, 41), Map.entry(ART_TITAN_CLASH, 50), Map.entry(ART_ORYKRON_SIGMA, 60),
            Map.entry(ART_OMEGA_CORE, 61), Map.entry(ART_ZOMBIE_FLEET, 61), Map.entry(ART_AURORA07, 70), Map.entry(ART_NECROSYS, 75),
            Map.entry(ART_LUMIRA, 81), Map.entry(ART_ULTIMA_AWAKEN, 90), Map.entry(ART_ULTIMA_VOID, 95), Map.entry(ART_ULTIMA, 100));

    /** Tranh của chương đã xuất hiện tới màn num (luôn có ít nhất tranh đầu chương). */
    static List<String> unlockedArts(CampaignMode.CampaignChapter chapter, int num) {
        return chapterArts(chapter).stream().filter(a -> ART_UNLOCK.get(a) <= num).toList();
    }

    public static String artForChapter(CampaignMode.CampaignChapter chapter) {
        return chapterArts(chapter).get(0);
    }

    public static ParticleMood moodForChapter(CampaignMode.CampaignChapter chapter) {
        if (chapter == null) return ParticleMood.CALM_SPRAY;
        return switch (chapter) {
            case CH1_NAVAL -> ParticleMood.CALM_SPRAY;
            case CH3_APOCALYPSE -> ParticleMood.LAVA_EMBERS;
            case CH4_GALAXY -> ParticleMood.GALAXY_STARS;
            case CH5_ATLANTIS -> ParticleMood.ATLANTIS_BUBBLES;
            case CH2_FANTASY -> ParticleMood.SAKURA_MAGIC;
        };
    }
}
