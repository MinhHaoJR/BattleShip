package battleship.ui;

import battleship.model.Board;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Special "Titan Boss" mode specification: AI commands colossal Blender 3D Kaiju bosses
 * (Godzilla, King Ghidorah & Mothra), each possessing 5 signature movie-accurate active skills
 * (3 Phase 1 base skills + 2 ferocious Phase 2 "Cuồng Bạo" Enraged ultimates),
 * anatomical 20-cell colossus hitboxes, Phase 2 Enrage transitions (<=50% HP),
 * active environmental manipulation (ocean wind gusts, seismic tremors, Weather/Season/MapTheme shifts),
 * and persistent impact zones.
 */
public final class TitanMode {
    public enum TitanBoss {
        GODZILLA(
                "Godzilla · Vua Quái Vật",
                "Boss: Godzilla",
                new Color(0x38BDF8),
                new Color(0xF97316),
                260
        ),
        GHIDORAH(
                "King Ghidorah · Tam Đầu Kim Long",
                "Boss: Ghidorah",
                new Color(0xFACC15),
                new Color(0xA855F7),
                280
        ),
        MOTHRA(
                "Mothra · Nữ Hoàng Thiên Nhiên",
                "Boss: Mothra",
                new Color(0x34D399),
                new Color(0xFDE047),
                240
        ),
        SPACEGODZILLA(
                "SpaceGodzilla · Ác Long Tinh Thể",
                "Boss: SpaceGodzilla",
                new Color(0xA855F7),
                new Color(0x38BDF8),
                300
        ),
        DESTOROYAH(
                "Destoroyah · Quỷ Phân Rã Oxy",
                "Boss: Destoroyah",
                new Color(0xDC2626),
                new Color(0xFBBF24),
                290
        ),
        /** 4 giai đoạn theo máu (75/50/25%): Ấu Long → Thức Tỉnh → Hư Không → Tối Thượng. */
        ULTIMA(
                "Godzilla Ultima · Tối Thượng",
                "Boss: Godzilla Ultima",
                new Color(0xF97316),
                new Color(0xEF4444),
                340
        );

        public final String displayName;
        public final String shortLabel;
        public final Color accentColor;
        public final Color enrageColor;
        public final int maxHp;

        TitanBoss(String displayName, String shortLabel, Color accentColor, Color enrageColor, int maxHp) {
            this.displayName = displayName;
            this.shortLabel = shortLabel;
            this.accentColor = accentColor;
            this.enrageColor = enrageColor;
            this.maxHp = maxHp;
        }
    }

    public enum TitanDifficulty {
        NORMAL("Thường", 1.00, 1.24, 1.25, 1.00, UiTokens.ACCENT_SIGNAL),
        NIGHTMARE("Ác Mộng", 1.25, 1.48, 1.45, 1.35, UiTokens.STATUS_WARNING),
        CATACLYSM("Tuyệt Diệt", 1.50, 1.72, 1.70, 1.75, UiTokens.STATUS_DANGER);

        public final String label;
        public final double hpMult;
        public final double atbSpeedMult;
        public final double staggerThresholdMult;
        public final double scoreMult;
        public final Color accentColor;

        TitanDifficulty(String label, double hpMult, double atbSpeedMult,
                        double staggerThresholdMult, double scoreMult, Color accentColor) {
            this.label = label;
            this.hpMult = hpMult;
            this.atbSpeedMult = atbSpeedMult;
            this.staggerThresholdMult = staggerThresholdMult;
            this.scoreMult = scoreMult;
            this.accentColor = accentColor;
        }
    }

    /** Roguelite passive relics awarded between Boss Rush Raid encounters. */
    public enum RaidRelic {
        CAPACITOR("Tụ Siêu Tụ", "relic_capacitor", "-15% Hồi Chiêu Vũ Khí", new Color(0x22D3EE)),
        AEGIS("Lõi Aegis", "relic_aegis", "-22% Hao Khiên & +25% Nạp Khiên", new Color(0x38BDF8)),
        TECTONIC("Địa Chấn", "relic_tectonic", "+25% Lực Ngắt Chiêu & Sát Thương Bộ Phận", new Color(0xF59E0B)),
        CATALYST("Xúc Tác", "relic_catalyst", "+35% Sát Thương Phản Ứng & Cháy", new Color(0xFB7185)),
        HYPERION_CELL("Pin Hyperion", "relic_hyperion", "+30% Hồi Mana & +25% Nạp Siêu Tàu", new Color(0xA855F7)),
        BASTION("Pháo Đài Đảo", "relic_bastion", "Đá Chắn 3 HP & +1 Trạm Tiếp Tế", new Color(0x34D399));

        public final String title;
        public final String spriteKey;
        public final String summary;
        public final Color accentColor;

        RaidRelic(String title, String spriteKey, String summary, Color accentColor) {
            this.title = title;
            this.spriteKey = spriteKey;
            this.summary = summary;
            this.accentColor = accentColor;
        }
    }

    /** Dynamic mid-battle Tactical Directives rewarding active maneuvering & combo play. */
    public enum DirectiveType {
        STAGGER_WINDOW(
                "NGẮT CHIÊU ĐÚNG LÚC!",
                "Bắn Bộ Phận Tụ hoặc Ngắt Chiêu",
                "+2 Mana · +20% Siêu Tàu",
                new Color(0x22D3EE),
                11_000L
        ),
        BEACON_RUN(
                "THU THẬP TÍN HIỆU!",
                "Lái tàu lướt qua Trạm / Nút Năng Lượng",
                "+2 Mana · Vá 1 Ô · +15% Khiên",
                new Color(0x34D399),
                10_000L
        ),
        ELEMENTAL_COMBO(
                "PHẢN ỨNG NGUYÊN TỐ!",
                "Kích hoạt Tan Chảy / Sốc Nhiệt / Siêu Dẫn",
                "+3 Mana · +18 ST Chuẩn",
                new Color(0xF59E0B),
                12_000L
        ),
        WEAKPOINT_STRIKE(
                "OANH TẠC ĐIỂM YẾU!",
                "Bắn trúng Đầu hoặc Vây Lõi Boss",
                "+15% Siêu Tàu · -1.2s CD",
                new Color(0xFACC15),
                9_000L
        ),
        DODGE_DANCE(
                "NÉ ĐÒN CHUẨN XÁC!",
                "Tránh 0-hit chiêu Boss hoặc Đỡ Khiên Hoàn Hảo",
                "+2 Mana · +18% Siêu Tàu",
                new Color(0xA855F7),
                12_000L
        );

        public final String title;
        public final String objective;
        public final String rewardLabel;
        public final Color accentColor;
        public final long durationMs;

        DirectiveType(String title, String objective, String rewardLabel, Color accentColor, long durationMs) {
            this.title = title;
            this.objective = objective;
            this.rewardLabel = rewardLabel;
            this.accentColor = accentColor;
            this.durationMs = durationMs;
        }
    }

    /** Concise description of interactive map mechanics for the selected ocean stage theme. */
    public static String stageMechanicsSummary(OceanMapBackdrop.MapTheme theme) {
        if (theme == null) theme = OceanMapBackdrop.MapTheme.NAVAL_CITADEL;
        return switch (theme) {
            case NAVAL_CITADEL -> "Dòng Hải Lưu (-40% CD Né) + Phao Sonar (Quét 3 điểm yếu)";
            case APOCALYPSE -> "Khe Nham Thạch (Cảnh báo 1.5s -> Cháy & +25% ST Lửa)";
            case ABYSSAL_ATLANTIS -> "2 Trụ Mana Cổ Ngữ (+2 Mana & +10% Siêu Tàu khi lướt qua)";
            case FANTASY_REALM -> "2 Vòng Kết Giới (-35% CD Vũ Khí & +4% Khiên/s khi đậu lên)";
            case GALAXY_COSMOS -> "Trọng Lực Thấp (-35% CD Né, Lướt 2 ô) + Thiên Thạch Vỡ (+2 Mana)";
        };
    }

    public enum TitanSkill {
        GZ_TAIL_SLAM(
                TitanBoss.GODZILLA,
                "Quật Đuôi Địa Chấn",
                "Đuôi 1×4",
                "Godzilla quật đuôi gai quét 1×4 ô trên mặt biển, hất tung sóng xung kích.",
                new Color(0x38BDF8),
                false,
                false,
                false,
                38
        ),
        GZ_ATOMIC_BREATH(
                TitanBoss.GODZILLA,
                "Hơi Thở Nguyên Tử",
                "Nguyên Tử 1×5",
                "Tụ năng lượng vây lưng Cherenkov, bắn tia Plasma xuyên 1×5 ô + vùng phóng xạ.",
                new Color(0x22D3EE),
                true,
                true,
                false,
                44
        ),
        GZ_THERMO_PULSE(
                TitanBoss.GODZILLA,
                "Xung Nhiệt Hạch",
                "Nhiệt Hạch 3×3",
                "Burning Godzilla giải phóng xung nhiệt hạch 3×3 ô + vùng dung nham cháy.",
                new Color(0xF97316),
                true,
                false,
                false,
                50
        ),
        GZ_SPIRAL_RAY(
                TitanBoss.GODZILLA,
                "Tia Xoắn Đỏ Hủy Diệt",
                "★ Xoắn Đỏ 7",
                "[PHASE 2 CUỒNG BẠO] Tia Plasma xoắn ốc đỏ rực xuyên 1×5 + nổ chữ T 7 ô & cháy vùng.",
                new Color(0xEF4444),
                true,
                true,
                true,
                58
        ),
        GZ_MELTDOWN_RIFT(
                TitanBoss.GODZILLA,
                "Vực Thẳm Nhiệt Hạch Ω",
                "★ Dung Nham Ω",
                "[PHASE 2 CUỒNG BẠO] Quá tải lõi lò hạt nhân xé toạc đáy biển theo ngôi sao 9 ô dung nham.",
                new Color(0xFB7185),
                true,
                false,
                true,
                68
        ),
        KG_TRIPLE_GRAVITY(
                TitanBoss.GHIDORAH,
                "Tam Đầu Lôi Quang",
                "Tam Đầu 3 Tia",
                "Cả 3 đầu rồng vàng phóng 3 chùm tia trọng lực phân tán vào đội hình tàu.",
                new Color(0xFACC15),
                false,
                true,
                false,
                40
        ),
        KG_HURRICANE_WING(
                TitanBoss.GHIDORAH,
                "Bão Cánh Hủy Diệt",
                "Bão Cánh X",
                "Vỗ đôi cánh vàng khổng lồ tạo cuồng phong sấm sét hình chữ X (5 ô).",
                new Color(0xFDE047),
                false,
                false,
                false,
                38
        ),
        KG_OMEGA_VORTEX(
                TitanBoss.GHIDORAH,
                "Thiên Thạch Trọng Lực",
                "Xoáy Sét 3×3",
                "Alpha Call triệu hồi bão trọng lực vàng tím 3×3 ô + vùng xoáy điện từ dai dẳng.",
                new Color(0xA855F7),
                true,
                false,
                false,
                50
        ),
        KG_ALPHA_STORM(
                TitanBoss.GHIDORAH,
                "Siêu Bão Sét Alpha",
                "★ Siêu Bão 7",
                "[PHASE 2 CUỒNG BẠO] 3 đầu hội tụ giáng 7 cột sét vàng tím hủy diệt trên diện rộng.",
                new Color(0xF59E0B),
                true,
                true,
                true,
                58
        ),
        KG_CATACLYSM_GRAVITY(
                TitanBoss.GHIDORAH,
                "Đại Họa Trọng Lực Ω",
                "★ Hố Đen Ω",
                "[PHASE 2 CUỒNG BẠO] Mở hố đen trọng lực giáng 9 thiên thạch tàn phá toàn khu vực.",
                new Color(0xC084FC),
                true,
                false,
                true,
                68
        ),
        MT_WING_GALE(
                TitanBoss.MOTHRA,
                "Cuồng Phong Cánh Thần",
                "Gió Cánh 5",
                "Mothra vỗ đôi cánh ánh sáng thổi tung mặt biển theo hình nón 5 ô, dạt sóng cực mạnh.",
                new Color(0x2DD4BF),
                false,
                false,
                false,
                38
        ),
        MT_GOD_RAY(
                TitanBoss.MOTHRA,
                "Thánh Quang Rẽ Mây",
                "Quang Tia 1×5",
                "Mothra tách tầng mây bão giáng tia Thánh Quang vàng ngọc xuyên 1×5 ô + vùng quang năng.",
                new Color(0xFDE047),
                true,
                true,
                false,
                45
        ),
        MT_SILK_WEB(
                TitanBoss.MOTHRA,
                "Kén Tơ Thiên Hà",
                "Tơ Kén 3×3",
                "Phun tơ kén phát sáng trói chặt vùng biển 3×3 ô, đóng băng sóng gió trong khu vực.",
                new Color(0xA7F3D0),
                true,
                false,
                false,
                48
        ),
        MT_PRISM_RAIN(
                TitanBoss.MOTHRA,
                "Mưa Phấn Hoa Lăng Kính",
                "★ Phấn Hoa 7",
                "[PHASE 2 CUỒNG BẠO] Tung bão phấn hoa cầu vồng 7 ô biến đổi thời tiết & ăn mòn giáp tàu.",
                new Color(0x38BDF8),
                true,
                false,
                true,
                58
        ),
        MT_SOLAR_SUPERNOVA(
                TitanBoss.MOTHRA,
                "Siêu Tân Tinh Solar Mothra Ω",
                "★ Tân Tinh Ω",
                "[PHASE 2 CUỒNG BẠO] Hiến tế toàn bộ quang năng tạo vụ nổ mặt trời 9 ô làm rung chuyển đại dương.",
                new Color(0xFB7185),
                true,
                true,
                true,
                68
        ),

        // ===== SpaceGodzilla =====
        SG_CRYSTAL_MISSILE(TitanBoss.SPACEGODZILLA, "Tên Lửa Tinh Thể", "Tinh Thể ×5",
                "Bắn 5 mũi tinh thể vũ trụ từ cột vai, cắm xuống rải rác khắp đội hình.",
                new Color(0xC084FC), false, 40, 0, new SkillSpec(
                new int[][]{{0, 0}, {-2, -2}, {-2, 2}, {2, -1}, {1, 3}}, TitanPart.CORE_FIN, 3000L,
                HazardType.NONE, FxKind.CRYSTAL_RAIN)),
        SG_GRAVITY_TORNADO(TitanBoss.SPACEGODZILLA, "Lốc Trọng Lực", "Lốc Thoi 7",
                "Bay lên bằng trọng lực, xoáy một cơn lốc tím hút tàu vào tâm theo hình thoi 7 ô.",
                new Color(0x8B5CF6), false, 42, 0, new SkillSpec(
                new int[][]{{0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-2, 0}, {2, 0}}, TitanPart.CORE_FIN, 3200L,
                HazardType.SHOCK, FxKind.GRAVITY_TORNADO)),
        SG_CORONA_BEAM(TitanBoss.SPACEGODZILLA, "Tia Corona", "Corona 1×6",
                "Tia vũ trụ xoắn xanh tím bắn thẳng xuyên 1×6 ô, để lại vùng nhiễu điện.",
                new Color(0x38BDF8), false, 46, 0, new SkillSpec(
                new int[][]{{0, -2}, {0, -1}, {0, 0}, {0, 1}, {0, 2}, {0, 3}}, TitanPart.HEAD, 3100L,
                HazardType.SHOCK, FxKind.SPIRAL_BEAM)),
        SG_CRYSTAL_FORTRESS(TitanBoss.SPACEGODZILLA, "Pháo Đài Tinh Thể", "★ Pháo Đài 8",
                "[PHASE 2 CUỒNG BẠO] Dựng vòng 8 tháp tinh thể quanh mục tiêu, tháp nổ tung khi trồi lên.",
                new Color(0xE879F9), true, 58, 0, new SkillSpec(
                new int[][]{{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}}, TitanPart.CORE_FIN, 2950L,
                HazardType.SHOCK, FxKind.CRYSTAL_PILLARS)),
        SG_COSMIC_NOVA(TitanBoss.SPACEGODZILLA, "Siêu Tân Tinh Vũ Trụ Ω", "★ Tân Tinh Ω",
                "[PHASE 2 CUỒNG BẠO] Dồn năng lượng hai cột vai thành vụ nổ sao 13 ô, đốt cháy cả vùng biển.",
                new Color(0xF0ABFC), true, 68, 0, new SkillSpec(
                new int[][]{{0, 0}, {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}, {-2, 0}, {2, 0}, {0, -2}, {0, 2}},
                TitanPart.CORE_FIN, 3400L, HazardType.BURN, FxKind.CRYSTAL_RAIN, FxKind.NOVA)),

        // ===== Destoroyah =====
        DS_MICRO_OXYGEN(TitanBoss.DESTOROYAH, "Tia Oxy Vi Thể", "Oxy 1×5",
                "Phun tia oxy vi thể đỏ tím xuyên 1×5 ô, làm vỏ thép tàu rã ra từng mảng.",
                new Color(0xF472B6), false, 44, 0, new SkillSpec(
                new int[][]{{0, -2}, {0, -1}, {0, 0}, {0, 1}, {0, 2}}, TitanPart.HEAD, 3100L,
                HazardType.BURN, FxKind.BEAM)),
        DS_HORN_SLASH(TitanBoss.DESTOROYAH, "Sừng Laser Cắt", "Sừng Chéo 5",
                "Sừng trán phóng lưỡi laser vàng chém chéo 5 ô qua đội hình.",
                new Color(0xFBBF24), false, 40, 0, new SkillSpec(
                new int[][]{{-2, -2}, {-1, -1}, {0, 0}, {1, 1}, {2, 2}}, TitanPart.HEAD, 2600L,
                HazardType.NONE, FxKind.HORN_SLASH)),
        DS_SWARM_SPLIT(TitanBoss.DESTOROYAH, "Tách Bầy Giáp Xác", "Bầy Đàn 6",
                "Thân tách thành bầy giáp xác đỏ bò tràn 6 ô, cắn xé rồi để lại vết trói chậm.",
                new Color(0xEF4444), false, 42, 0, new SkillSpec(
                new int[][]{{0, 0}, {-1, -2}, {1, 2}, {-2, 1}, {2, -1}, {0, 3}}, TitanPart.BODY_TAIL, 3000L,
                HazardType.SILK_SLOW, FxKind.SWARM)),
        DS_OXYGEN_MIST(TitanBoss.DESTOROYAH, "Sương Oxy Đen", "★ Sương Đen 3×3",
                "[PHASE 2 CUỒNG BẠO] Thở ra màn sương oxy đen 3×3 ô, hút cạn dưỡng khí và trói chậm tàu.",
                new Color(0x7F1D1D), true, 58, 0, new SkillSpec(
                new int[][]{{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 0}, {0, 1}, {1, -1}, {1, 0}, {1, 1}}, TitanPart.CORE_FIN, 2950L,
                HazardType.SILK_SLOW, FxKind.MIST)),
        DS_DESTRUCTION_OMEGA(TitanBoss.DESTOROYAH, "Phân Rã Phân Tử Ω", "★ Phân Rã Ω",
                "[PHASE 2 CUỒNG BẠO] Lõi ngực bùng nổ, sóng phân rã chữ X 13 ô xé tan mọi phân tử.",
                new Color(0xFDE047), true, 68, 0, new SkillSpec(
                new int[][]{{0, 0}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}, {-2, -2}, {-2, 2}, {2, -2}, {2, 2}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}},
                TitanPart.CORE_FIN, 3400L, HazardType.BURN, FxKind.MIST, FxKind.NOVA)),

        // ===== Godzilla Ultima: mỗi form 5 chiêu theo khuôn Godzilla (3 thường + 2 ★ mở khi form đó mất 1/3 máu),
        // không mượn VFX/vùng đánh của boss khác =====
        // Form I · Thủy long đỏ thẫm (mang lửa, vây lưng, vây đuôi)
        UL1_GILL_SPRAY(TitanBoss.ULTIMA, "Phun Mang Lửa", "I · Mang Lửa V",
                "[FORM I · THỦY LONG] Khe mang rực cam phun hai luồng hơi nóng xoè hình chữ V 5 ô.",
                new Color(0xFF7B39), false, 38, 1, new SkillSpec(
                new int[][]{{0, 0}, {-1, 1}, {1, 1}, {-2, 2}, {2, 2}}, TitanPart.HEAD, 2600L,
                HazardType.NONE, FxKind.GILL_SPRAY)),
        UL1_FIN_DIVE(TitanBoss.ULTIMA, "Lặn Vây Rạch Sóng", "I · Vây Rạch 5",
                "[FORM I · THỦY LONG] Lặn sát mặt nước, hàng gai lưng rạch một đường zíc-zắc 5 ô qua đội hình.",
                new Color(0xE2CCD2), false, 40, 1, new SkillSpec(
                new int[][]{{0, -2}, {1, -1}, {0, 0}, {1, 1}, {0, 2}}, TitanPart.BODY_TAIL, 2800L,
                HazardType.NONE, FxKind.FIN_WAKE)),
        UL1_FLUKE_GEYSER(TitanBoss.ULTIMA, "Vây Đuôi Dựng Cột Nước", "I · Cột Nước 2×3",
                "[FORM I · THỦY LONG] Vây đuôi quạt xuống mặt biển, dựng cột nước nổ tung khối 2×3 ô.",
                new Color(0x7DD3FC), false, 42, 1, new SkillSpec(
                new int[][]{{0, -1}, {0, 0}, {0, 1}, {1, -1}, {1, 0}, {1, 1}}, TitanPart.BODY_TAIL, 3000L,
                HazardType.NONE, FxKind.GEYSER)),
        UL1_RIPTIDE_COIL(TitanBoss.ULTIMA, "Xoáy Cuộn Thủy Long", "★ I · Xoáy Cuộn 10",
                "[FORM I · ★ CUỒNG NỘ] Thân dài cuộn tròn quẫy nước, khoét xoáy nước hút tàu theo vòng xoắn 10 ô.",
                new Color(0x38BDF8), true, 50, 1, new SkillSpec(
                new int[][]{{0, 0}, {0, 1}, {1, 1}, {1, 0}, {1, -1}, {0, -1}, {-1, -1}, {-1, 0}, {-1, 1}, {-1, 2}}, TitanPart.BODY_TAIL, 3200L,
                HazardType.SILK_SLOW, FxKind.WHIRLPOOL)),
        UL1_SCALD_TORRENT(TitanBoss.ULTIMA, "Thác Hơi Bỏng", "★ I · Thác Hơi 8",
                "[FORM I · ★ CUỒNG NỘ] Mọi khe mang mở hết cỡ, hai thác hơi bỏng song song so le quét 8 ô.",
                new Color(0xFFB36B), true, 54, 1, new SkillSpec(
                new int[][]{{-1, -2}, {-1, -1}, {-1, 0}, {-1, 1}, {1, 0}, {1, 1}, {1, 2}, {1, 3}}, TitanPart.HEAD, 3300L,
                HazardType.BURN, FxKind.STEAM_JETS)),
        // Form II · Bốn chân gỉ sắt (húc, khe dung nham, giáp vảy)
        UL2_RAMPAGE_CHARGE(TitanBoss.ULTIMA, "Húc Xung Phong", "II · Húc 1×7",
                "[FORM II · BỐN CHÂN] Cúi đầu sừng gờ, lao thẳng giẫm nát một hàng 1×7 ô.",
                new Color(0xB4471C), false, 46, 2, new SkillSpec(
                new int[][]{{0, -3}, {0, -2}, {0, -1}, {0, 0}, {0, 1}, {0, 2}, {0, 3}}, TitanPart.HEAD, 2800L,
                HazardType.NONE, FxKind.CHARGE_TRAIL)),
        UL2_LAVA_VENTS(TitanBoss.ULTIMA, "Khe Dung Nham Phun Trào", "II · Dung Nham 6",
                "[FORM II · BỐN CHÂN] Các khe nứt trên lưng truyền nhiệt xuống đáy biển, 6 cột dung nham phun lên.",
                new Color(0xFF7A1A), false, 48, 2, new SkillSpec(
                new int[][]{{0, 0}, {-2, -1}, {-1, 2}, {1, -2}, {2, 1}, {0, -3}}, TitanPart.CORE_FIN, 3100L,
                HazardType.BURN, FxKind.LAVA_GEYSER)),
        UL2_SCALE_SHRAPNEL(TitanBoss.ULTIMA, "Bắn Giáp Vảy", "II · Giáp Vảy 5",
                "[FORM II · BỐN CHÂN] Rũ mình bắn những mảng giáp vảy nung đỏ quay tít xuống 5 ô.",
                new Color(0xCF6A32), false, 44, 2, new SkillSpec(
                new int[][]{{0, 0}, {-2, 0}, {2, 0}, {0, -2}, {0, 2}}, TitanPart.CORE_FIN, 2900L,
                HazardType.NONE, FxKind.SCALE_SHRAPNEL)),
        UL2_MAGMA_QUAKE(TitanBoss.ULTIMA, "Địa Chấn Dung Nham", "★ II · Vòng Nham 9",
                "[FORM II · ★ CUỒNG NỘ] Bốn chân dậm cùng lúc, vòng khe nứt dung nham rạn quanh tâm rồi phun 9 ô.",
                new Color(0xFF5A1F), true, 56, 2, new SkillSpec(
                new int[][]{{0, 0}, {-2, 0}, {2, 0}, {0, -2}, {0, 2}, {-2, -2}, {-2, 2}, {2, -2}, {2, 2}}, TitanPart.CORE_FIN, 3300L,
                HazardType.BURN, FxKind.MAGMA_RING)),
        UL2_MOLTEN_STAMPEDE(TitanBoss.ULTIMA, "Giẫm Nát Nham Thạch", "★ II · Vết Giẫm 7",
                "[FORM II · ★ CUỒNG NỘ] Phi nước đại xuyên đội hình, từng vết chân nung đỏ so le dập nát 7 ô.",
                new Color(0xE8552A), true, 58, 2, new SkillSpec(
                new int[][]{{-1, -3}, {1, -2}, {-1, -1}, {1, 0}, {-1, 1}, {1, 2}, {-1, 3}}, TitanPart.BODY_TAIL, 3200L,
                HazardType.NONE, FxKind.STOMP_TRAIL)),
        // Form III · Xanh mòng két (vuốt, tiếng gầm, gai lõi băng lam)
        UL3_CLAW_REND(TitanBoss.ULTIMA, "Vuốt Xé Ba Đường", "III · Vuốt 3×3",
                "[FORM III · DỰNG NGƯỜI] Hai tay vồ xuống, ba vết vuốt song song xé 9 ô.",
                new Color(0x5EEAD4), false, 52, 3, new SkillSpec(
                new int[][]{{-1, -2}, {0, -2}, {1, -2}, {-1, 0}, {0, 0}, {1, 0}, {-1, 2}, {0, 2}, {1, 2}}, TitanPart.BODY_TAIL, 2700L,
                HazardType.NONE, FxKind.CLAW_MARKS)),
        UL3_SONIC_ROAR(TitanBoss.ULTIMA, "Tiếng Gầm Băng Lam", "III · Sóng Âm 7",
                "[FORM III · DỰNG NGƯỜI] Ngửa cổ gầm, sóng âm băng lam lan hình nón 7 ô, làm đóng băng mặt sóng.",
                new Color(0x7FF5FF), false, 54, 3, new SkillSpec(
                new int[][]{{0, 0}, {-1, 1}, {0, 1}, {1, 1}, {-2, 2}, {0, 2}, {2, 2}}, TitanPart.HEAD, 3000L,
                HazardType.SILK_SLOW, FxKind.SONIC_ROAR)),
        UL3_SPINE_LANCE(TitanBoss.ULTIMA, "Thương Gai Lưng", "III · Thương Gai X",
                "[FORM III · DỰNG NGƯỜI] Gai lưng lõi băng lam bứt ra thành thương, cắm xuống hình chữ X 9 ô.",
                new Color(0xC3C8D2), false, 56, 3, new SkillSpec(
                new int[][]{{0, 0}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}, {-2, -2}, {-2, 2}, {2, -2}, {2, 2}}, TitanPart.CORE_FIN, 3000L,
                HazardType.SHOCK, FxKind.SPINE_LANCES)),
        UL3_GLACIER_BREATH(TitanBoss.ULTIMA, "Hơi Thở Băng Hà", "★ III · Băng Hà 11",
                "[FORM III · ★ CUỒNG NỘ] Gai lưng sáng lam từng đốt, phun luồng băng hà xuyên 1×7 toả rộng ở đầu, đóng băng 11 ô.",
                new Color(0x67E8F9), true, 60, 3, new SkillSpec(
                new int[][]{{0, -3}, {0, -2}, {0, -1}, {0, 0}, {0, 1}, {0, 2}, {0, 3}, {-1, 2}, {1, 2}, {-1, 3}, {1, 3}}, TitanPart.HEAD, 3300L,
                HazardType.SILK_SLOW, FxKind.ICE_BEAM)),
        UL3_FROST_CROWN(TitanBoss.ULTIMA, "Vương Miện Băng Gai", "★ III · Vòng Băng 8",
                "[FORM III · ★ CUỒNG NỘ] Đập tay xuống biển, vòng gai băng trồi lên hình thoi rỗng 8 ô quanh mục tiêu.",
                new Color(0xA5F3FC), true, 62, 3, new SkillSpec(
                new int[][]{{-2, 0}, {-1, -1}, {-1, 1}, {0, -2}, {0, 2}, {1, -1}, {1, 1}, {2, 0}}, TitanPart.CORE_FIN, 3200L,
                HazardType.SHOCK, FxKind.ICE_SPIKES)),
        // Form IV · Xám đen lõi đỏ (hơi thở huyết hạch, cột lửa trời, tiếng gầm cuối)
        UL4_CRIMSON_BREATH(TitanBoss.ULTIMA, "Hơi Thở Huyết Hạch", "IV · Huyết Hạch 2×4",
                "[FORM IV · TỐI THƯỢNG] Họng đỏ rực phun luồng hơi thở huyết hạch dày, quét dải 2×4 ô.",
                new Color(0xFF3B30), false, 60, 4, new SkillSpec(
                new int[][]{{0, -1}, {0, 0}, {0, 1}, {0, 2}, {1, -1}, {1, 0}, {1, 1}, {1, 2}}, TitanPart.HEAD, 3000L,
                HazardType.BURN, FxKind.CRIMSON_BREATH)),
        UL4_SKYFIRE(TitanBoss.ULTIMA, "Cột Lửa Trời", "IV · Lửa Trời 7",
                "[FORM IV · TỐI THƯỢNG] Gầm lên trời, 7 cột lửa đỏ giáng thẳng xuống biển.",
                new Color(0xFF5A36), false, 62, 4, new SkillSpec(
                new int[][]{{0, 0}, {-2, -2}, {-2, 2}, {2, -2}, {2, 2}, {0, -3}, {0, 3}}, TitanPart.CORE_FIN, 3100L,
                HazardType.BURN, FxKind.RED_PILLARS)),
        UL4_TAIL_CRUSH(TitanBoss.ULTIMA, "Quật Đuôi Huyết Hạch", "IV · Quật Đuôi 7",
                "[FORM IV · TỐI THƯỢNG] Xoay người quật chiếc đuôi gai đỏ, quét một cung cong 7 ô.",
                new Color(0xB91C1C), false, 58, 4, new SkillSpec(
                new int[][]{{1, -3}, {0, -2}, {0, -1}, {0, 0}, {0, 1}, {0, 2}, {1, 3}}, TitanPart.BODY_TAIL, 2800L,
                HazardType.NONE, FxKind.TAIL_ARC)),
        UL4_BLOOD_SPIRAL(TitanBoss.ULTIMA, "Tia Xoắn Huyết Hạch", "★ IV · Xoắn Huyết 9",
                "[FORM IV · ★ CUỒNG NỘ] Tia huyết hạch xoắn đôi xuyên 1×6, nổ thành chữ thập ở đầu tia, đốt 9 ô.",
                new Color(0xDC2626), true, 64, 4, new SkillSpec(
                new int[][]{{0, -2}, {0, -1}, {0, 0}, {0, 1}, {0, 2}, {0, 3}, {-1, 3}, {1, 3}, {0, 4}}, TitanPart.HEAD, 3300L,
                HazardType.BURN, FxKind.BLOOD_SPIRAL)),
        UL4_FINAL_ROAR(TitanBoss.ULTIMA, "Tối Thượng Ω · Gầm Huyết Hạch", "★ Tối Thượng Ω",
                "[FORM IV · ★ CUỒNG NỘ] Gai lưng bừng đỏ từng đốt, tiếng gầm cuối nổ thành vòng xung kích kim cương 13 ô.",
                new Color(0xEF4444), true, 68, 4, new SkillSpec(
                new int[][]{{0, 0}, {-3, 0}, {3, 0}, {0, -3}, {0, 3}, {-2, -1}, {-2, 1}, {2, -1}, {2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}},
                TitanPart.HEAD, 3400L, HazardType.BURN, FxKind.SPINE_CHARGE, FxKind.RED_SHOCKWAVE));

        public final TitanBoss boss;
        public final String skillName;
        public final String chipLabel;
        public final String description;
        public final Color accentColor;
        public final boolean leavesImpactZone;
        public final boolean beamStyle;
        public final boolean phase2Only;
        public final int shieldDamage;
        /** Chỉ boss mới có: dữ liệu vùng đánh/VFX; null với 15 chiêu cũ (vẫn đọc qua các switch). */
        public final SkillSpec spec;
        /** Giai đoạn của Godzilla Ultima (1–4) mà chiêu thuộc về; 0 = mọi giai đoạn. */
        public final int ultimaStage;

        TitanSkill(TitanBoss boss, String skillName, String chipLabel, String description, Color accentColor,
                   boolean phase2Only, int shieldDamage, int ultimaStage, SkillSpec spec) {
            this.boss = boss;
            this.skillName = skillName;
            this.chipLabel = chipLabel;
            this.description = description;
            this.accentColor = accentColor;
            this.leavesImpactZone = spec.hazard() != HazardType.NONE;
            this.beamStyle = spec.has(FxKind.BEAM) || spec.has(FxKind.SPIRAL_BEAM);
            this.phase2Only = phase2Only;
            this.shieldDamage = shieldDamage;
            this.ultimaStage = ultimaStage;
            this.spec = spec;
        }

        TitanSkill(
                TitanBoss boss,
                String skillName,
                String chipLabel,
                String description,
                Color accentColor,
                boolean leavesImpactZone,
                boolean beamStyle,
                boolean phase2Only,
                int shieldDamage
        ) {
            this.boss = boss;
            this.skillName = skillName;
            this.chipLabel = chipLabel;
            this.description = description;
            this.accentColor = accentColor;
            this.leavesImpactZone = leavesImpactZone;
            this.beamStyle = beamStyle;
            this.phase2Only = phase2Only;
            this.shieldDamage = shieldDamage;
            this.ultimaStage = 0;
            this.spec = null;
        }
    }

    /** Kiểu VFX của chiêu boss mới, vẽ trong {@link KaijuSkillFx}; một chiêu có thể ghép nhiều kiểu. */
    public enum FxKind {
        // SpaceGodzilla / Destoroyah
        BEAM, SPIRAL_BEAM, CRYSTAL_RAIN, CRYSTAL_PILLARS, GRAVITY_TORNADO, SWARM, MIST, HORN_SLASH, NOVA,
        // Godzilla Ultima (chỉ form của Ultima dùng)
        GILL_SPRAY, FIN_WAKE, GEYSER, CHARGE_TRAIL, LAVA_GEYSER, SCALE_SHRAPNEL,
        CLAW_MARKS, SONIC_ROAR, SPINE_LANCES, CRIMSON_BREATH, RED_PILLARS, SPINE_CHARGE, RED_SHOCKWAVE,
        // ★ của từng form Ultima
        WHIRLPOOL, STEAM_JETS, MAGMA_RING, STOMP_TRAIL, ICE_BEAM, ICE_SPIKES, TAIL_ARC, BLOOD_SPIRAL
    }

    /** pattern: các ô (dr, dc) khi đánh theo hàng ngang; theo cột thì đổi chỗ dr/dc. */
    public record SkillSpec(int[][] pattern, TitanPart chargePart, long castMs, HazardType hazard, FxKind... fx) {
        public boolean has(FxKind k) {
            for (FxKind f : fx) if (f == k) return true;
            return false;
        }
        public CombatLoadout.MagicOption zoneMagic() {
            return switch (hazard) {
                case SHOCK -> CombatLoadout.MagicOption.LIGHTNING;
                case SILK_SLOW -> CombatLoadout.MagicOption.FROST;
                default -> CombatLoadout.MagicOption.FIRE;
            };
        }
    }

    /** ★ của Ultima mở theo từng form (thay cho mốc cuồng bạo 50% của boss khác): khi form hiện tại đã mất 1/3 phần máu của nó. */
    public static boolean ultimaStarOpenFor(int hp, int maxHp) {
        if (maxHp <= 0) return false;
        int stage = ultimaStageFor(hp, maxHp);
        double top = 1.0 - 0.25 * (stage - 1);
        return hp / (double) maxHp <= top - 0.25 / 3.0;
    }

    /** Giai đoạn của Godzilla Ultima theo % máu còn lại: >75% I, >50% II, >25% III, còn lại IV. */
    public static int ultimaStageFor(int hp, int maxHp) {
        if (maxHp <= 0) return 1;
        double r = hp / (double) maxHp;
        return r > 0.75 ? 1 : r > 0.50 ? 2 : r > 0.25 ? 3 : 4;
    }

    private static volatile int ultimaStage = 1;

    /** Giai đoạn Ultima đang hiển thị (sprite đọc giá trị này). */
    public static int getUltimaStage() { return ultimaStage; }

    public static void setUltimaStage(int stage) { ultimaStage = Math.max(1, Math.min(4, stage)); }

    public static String ultimaStageName(int stage) {
        return switch (stage) {
            case 1 -> "GĐ I · Ấu Long";
            case 2 -> "GĐ II · Thức Tỉnh";
            case 3 -> "GĐ III · Hư Không";
            default -> "GĐ IV · Tối Thượng";
        };
    }

    private static volatile TitanBoss activeBoss = TitanBoss.GODZILLA;
    private static volatile boolean titanEnraged = false;
    private static volatile TitanDifficulty activeDifficulty = TitanDifficulty.NORMAL;
    private static volatile boolean bossRushEnabled = false;

    private TitanMode() { }

    public static TitanBoss getActiveBoss() { return activeBoss; }

    public static void setActiveBoss(TitanBoss boss) {
        activeBoss = boss != null ? boss : TitanBoss.GODZILLA;
        titanEnraged = false;
    }

    public static TitanBoss cycleActiveBoss() {
        TitanBoss[] values = TitanBoss.values();
        activeBoss = values[(activeBoss.ordinal() + 1) % values.length];
        titanEnraged = false;
        return activeBoss;
    }

    public static TitanDifficulty getDifficulty() { return activeDifficulty; }

    public static void setDifficulty(TitanDifficulty diff) {
        activeDifficulty = diff != null ? diff : TitanDifficulty.NORMAL;
    }

    public static TitanDifficulty cycleDifficulty() {
        TitanDifficulty[] vals = TitanDifficulty.values();
        activeDifficulty = vals[(activeDifficulty.ordinal() + 1) % vals.length];
        return activeDifficulty;
    }

    public static boolean isBossRushEnabled() { return bossRushEnabled; }

    public static void setBossRushEnabled(boolean enabled) { bossRushEnabled = enabled; }

    public static boolean toggleBossRush() {
        bossRushEnabled = !bossRushEnabled;
        return bossRushEnabled;
    }

    public static TitanBoss nextBossInRush(TitanBoss current) {
        if (current == null) return TitanBoss.GODZILLA;
        return switch (current) {
            case GODZILLA -> TitanBoss.GHIDORAH;
            case GHIDORAH -> TitanBoss.MOTHRA;
            case MOTHRA -> TitanBoss.SPACEGODZILLA;
            case SPACEGODZILLA -> TitanBoss.DESTOROYAH;
            case DESTOROYAH -> TitanBoss.ULTIMA;
            case ULTIMA -> null;
        };
    }

    public static int scaledMaxHp(TitanBoss boss, TitanDifficulty diff) {
        TitanBoss b = boss != null ? boss : activeBoss;
        TitanDifficulty d = diff != null ? diff : TitanDifficulty.NORMAL;
        return Math.max(1, (int) Math.round(b.maxHp * d.hpMult));
    }

    public static boolean isTitanEnraged() { return titanEnraged; }

    public static void setTitanEnraged(boolean enraged) {
        titanEnraged = enraged;
    }

    public static List<TitanSkill> skillsFor(TitanBoss boss) {
        TitanBoss b = boss != null ? boss : activeBoss;
        List<TitanSkill> out = new ArrayList<>(5);
        for (TitanSkill skill : TitanSkill.values()) {
            if (skill.boss == b && (skill.ultimaStage == 0 || skill.ultimaStage == ultimaStage)) out.add(skill);   // Ultima: chỉ 5 chiêu của form hiện tại
        }
        return out;
    }

    public static List<TitanSkill> skillsFor(TitanBoss boss, boolean enraged) {
        TitanBoss b = boss != null ? boss : activeBoss;
        List<TitanSkill> out = new ArrayList<>(5);
        for (TitanSkill skill : TitanSkill.values()) {
            if (skill.boss == b && (skill.ultimaStage == 0 || skill.ultimaStage == ultimaStage)
                    && (enraged || !skill.phase2Only)) out.add(skill);
        }
        return out;
    }

    public record AntiMissileSpec(
            String skillName,
            String bannerText,
            Color primaryColor,
            Color hotColor,
            boolean radialPulse
    ) { }

    public static AntiMissileSpec antiMissileSpec(TitanBoss boss, boolean enraged) {
        TitanBoss b = boss != null ? boss : activeBoss;
        return switch (b) {
            case GODZILLA -> enraged
                    ? new AntiMissileSpec("Thermo Xung Kích", "BURNING GODZILLA · THERMO ĐÁNH CHẶN!",
                    new Color(0xEA580C), new Color(0xFEF08A), true)
                    : new AntiMissileSpec("Tia Nguyên Tử Chặn", "GODZILLA · NĂNG LƯỢNG NGUYÊN TỬ ĐÁNH CHẶN!",
                    new Color(0x22D3EE), new Color(0xECFEFF), false);
            case GHIDORAH -> enraged
                    ? new AntiMissileSpec("Bão Từ Trường Alpha", "GHIDORAH THỨC TỈNH · BÃO TỪ TRƯỜNG CHẶN ĐẠN!",
                    new Color(0xC084FC), new Color(0xFDE047), true)
                    : new AntiMissileSpec("Tam Đầu Lôi Chặn", "GHIDORAH · TAM ĐẦU LÔI QUANG ĐÁNH CHẶN!",
                    new Color(0xFACC15), new Color(0xFEF9C3), false);
            case MOTHRA -> enraged
                    ? new AntiMissileSpec("Hào Quang Solar", "SOLAR MOTHRA · HÀO QUANG TRIỆT TIÊU TÊN LỬA!",
                    new Color(0xFB7185), new Color(0xFEF08A), true)
                    : new AntiMissileSpec("Phấn Tơ Chặn Đạn", "MOTHRA · LƯỚI TƠ PHẤN QUANG CHẶN TÊN LỬA!",
                    new Color(0x2DD4BF), new Color(0xE0F2FE), false);
            case SPACEGODZILLA -> enraged
                    ? new AntiMissileSpec("Lá Chắn Tinh Thể", "SPACEGODZILLA · LÁ CHẮN TINH THỂ NUỐT TÊN LỬA!",
                    new Color(0xE879F9), new Color(0xE0F2FE), true)
                    : new AntiMissileSpec("Trọng Lực Bẻ Đạn", "SPACEGODZILLA · TRỌNG LỰC BẺ CONG TÊN LỬA!",
                    new Color(0xA855F7), new Color(0xF5F3FF), false);
            case DESTOROYAH -> enraged
                    ? new AntiMissileSpec("Sương Oxy Chặn", "DESTOROYAH · SƯƠNG OXY ĐEN NUỐT TÊN LỬA!",
                    new Color(0x7F1D1D), new Color(0xFDE047), true)
                    : new AntiMissileSpec("Sừng Laser Chặn", "DESTOROYAH · SỪNG LASER CẮT ĐÔI TÊN LỬA!",
                    new Color(0xDC2626), new Color(0xFEF3C7), false);
            case ULTIMA -> enraged
                    ? new AntiMissileSpec("Hào Quang Tối Thượng", "GODZILLA ULTIMA · HÀO QUANG THIÊU RỤI TÊN LỬA!",
                    new Color(0xEF4444), new Color(0xFEF08A), true)
                    : new AntiMissileSpec("Gai Lưng Chặn", "GODZILLA ULTIMA · GAI LƯNG BẮN HẠ TÊN LỬA!",
                    new Color(0xF97316), new Color(0xFFEDD5), false);
        };
    }

    public static CombatLoadout.MagicOption impactZoneMagic(TitanSkill skill) {
        if (skill == null) return CombatLoadout.MagicOption.FIRE;
        if (skill.spec != null) return skill.spec.zoneMagic();
        return switch (skill) {
            case GZ_ATOMIC_BREATH, MT_GOD_RAY -> CombatLoadout.MagicOption.ARCANE_SONAR;
            case GZ_THERMO_PULSE, GZ_SPIRAL_RAY, GZ_MELTDOWN_RIFT, MT_SOLAR_SUPERNOVA -> CombatLoadout.MagicOption.FIRE;
            case MT_SILK_WEB -> CombatLoadout.MagicOption.FROST;
            case KG_OMEGA_VORTEX, KG_ALPHA_STORM, KG_CATACLYSM_GRAVITY, MT_PRISM_RAIN -> CombatLoadout.MagicOption.LIGHTNING;
            default -> CombatLoadout.MagicOption.FIRE;
        };
    }

    /** Computes the exact cell footprint affected when a Titan Boss casts one of its 5 movie skills. */
    public static List<int[]> skillTargetCells(TitanSkill skill, int row, int col, boolean horizontal) {
        List<int[]> cells = new ArrayList<>();
        if (skill == null) {
            addCell(cells, row, col);
            return cells;
        }
        if (skill.spec != null) {
            for (int[] d : skill.spec.pattern()) {
                addCell(cells, row + (horizontal ? d[0] : d[1]), col + (horizontal ? d[1] : d[0]));
            }
            if (cells.isEmpty()) addCell(cells, row, col);
            return cells;
        }
        switch (skill) {
            case GZ_TAIL_SLAM -> {
                for (int d = -1; d <= 2; d++) {
                    addCell(cells, row + (horizontal ? 0 : d), col + (horizontal ? d : 0));
                }
            }
            case GZ_ATOMIC_BREATH, MT_GOD_RAY -> {
                for (int d = -2; d <= 2; d++) {
                    addCell(cells, row + (horizontal ? 0 : d), col + (horizontal ? d : 0));
                }
            }
            case GZ_SPIRAL_RAY -> {
                // Piercing 1x5 spiral beam + T-blast wings at distal edge (7 cells)
                for (int d = -2; d <= 2; d++) {
                    addCell(cells, row + (horizontal ? 0 : d), col + (horizontal ? d : 0));
                }
                if (horizontal) {
                    addCell(cells, row - 1, col - 2);
                    addCell(cells, row + 1, col - 2);
                } else {
                    addCell(cells, row - 2, col - 1);
                    addCell(cells, row - 2, col + 1);
                }
            }
            case GZ_THERMO_PULSE, KG_OMEGA_VORTEX, MT_SILK_WEB -> {
                for (int r = row - 1; r <= row + 1; r++) {
                    for (int c = col - 1; c <= col + 1; c++) {
                        addCell(cells, r, c);
                    }
                }
            }
            case GZ_MELTDOWN_RIFT, MT_SOLAR_SUPERNOVA -> {
                // 5x5 volcanic/solar star rupture (9 cells: center + inner X + outer cardinal fissures)
                addCell(cells, row, col);
                addCell(cells, row - 1, col - 1);
                addCell(cells, row - 1, col + 1);
                addCell(cells, row + 1, col - 1);
                addCell(cells, row + 1, col + 1);
                addCell(cells, row - 2, col);
                addCell(cells, row + 2, col);
                addCell(cells, row, col - 2);
                addCell(cells, row, col + 2);
            }
            case KG_TRIPLE_GRAVITY -> {
                // 3-head diverging gravity strikes across 5 tactical cells (kept inside 10x10 edges)
                int cr = Math.max(1, Math.min(Board.SIZE - 2, row));
                int cc = Math.max(1, Math.min(Board.SIZE - 2, col));
                addCell(cells, row, col);
                addCell(cells, cr - 2, cc - 1);
                addCell(cells, cr + 2, cc - 1);
                addCell(cells, cr - 1, cc + 2);
                addCell(cells, cr + 1, cc + 2);
            }
            case KG_HURRICANE_WING -> {
                addCell(cells, row, col);
                addCell(cells, row - 1, col - 1);
                addCell(cells, row - 1, col + 1);
                addCell(cells, row + 1, col - 1);
                addCell(cells, row + 1, col + 1);
            }
            case MT_WING_GALE -> {
                // Sweeping wing-scythe pressure wave (5 cells blown toward player)
                addCell(cells, row, col);
                addCell(cells, row - 1, col);
                addCell(cells, row + 1, col);
                addCell(cells, row - 1, col + 1);
                addCell(cells, row + 1, col + 1);
            }
            case KG_ALPHA_STORM, MT_PRISM_RAIN -> {
                // 7-cell jagged thunder / prismatic pollen barrage across 5 columns
                addCell(cells, row, col);
                addCell(cells, row - 1, col - 1);
                addCell(cells, row + 1, col - 1);
                addCell(cells, row - 1, col + 1);
                addCell(cells, row + 1, col + 1);
                addCell(cells, row, col - 2);
                addCell(cells, row, col + 2);
            }
            case KG_CATACLYSM_GRAVITY -> {
                // Orbital gravity meteor barrage: 9 craters across a wide 5x5 sector
                addCell(cells, row, col);
                addCell(cells, row - 1, col);
                addCell(cells, row + 1, col);
                addCell(cells, row, col - 1);
                addCell(cells, row, col + 1);
                addCell(cells, row - 2, col - 2);
                addCell(cells, row - 2, col + 2);
                addCell(cells, row + 2, col - 2);
                addCell(cells, row + 2, col + 2);
            }
            default -> { }
        }
        if (cells.isEmpty()) addCell(cells, row, col);
        return cells;
    }

    /** Breakable anatomical Kaiju zones for Hybrid Real-Time Raid targeting and interrupts. */
    public enum TitanPart {
        HEAD("Đầu / Miệng", 1.35, 1.45),
        CORE_FIN("Lõi / Vây / Cánh", 1.22, 1.28),
        BODY_TAIL("Thân / Đuôi", 0.92, 0.85);

        public final String label;
        public final double damageMult;
        public final double staggerMult;

        TitanPart(String label, double damageMult, double staggerMult) {
            this.label = label;
            this.damageMult = damageMult;
            this.staggerMult = staggerMult;
        }
    }

    /** Environmental floor hazards spawned by Boss skills on the player's fleet grid. */
    public enum HazardType {
        NONE("Bình thường", UiTokens.TEXT_MUTED),
        BURN("Vùng Dung Nham", UiTokens.MAGIC_FIRE),
        SHOCK("Xoáy Điện Từ", UiTokens.MAGIC_LIGHTNING),
        SILK_SLOW("Tơ Kén Trói", UiTokens.MAGIC_FROST);

        public final String label;
        public final Color accent;

        HazardType(String label, Color accent) {
            this.label = label;
            this.accent = accent;
        }
    }

    private record PartCell(int r, int c, TitanPart part) { }

    private static List<PartCell> basePartLayout(TitanBoss boss) {
        if (boss == TitanBoss.GHIDORAH) {
            return List.of(
                    new PartCell(3, 2, TitanPart.HEAD), new PartCell(4, 1, TitanPart.HEAD),
                    new PartCell(4, 2, TitanPart.HEAD), new PartCell(5, 2, TitanPart.HEAD),
                    new PartCell(4, 3, TitanPart.HEAD),
                    new PartCell(5, 4, TitanPart.CORE_FIN), new PartCell(2, 5, TitanPart.CORE_FIN),
                    new PartCell(3, 5, TitanPart.CORE_FIN), new PartCell(3, 6, TitanPart.CORE_FIN),
                    new PartCell(4, 5, TitanPart.CORE_FIN), new PartCell(4, 6, TitanPart.CORE_FIN),
                    new PartCell(5, 5, TitanPart.CORE_FIN), new PartCell(5, 6, TitanPart.CORE_FIN),
                    new PartCell(5, 3, TitanPart.BODY_TAIL), new PartCell(6, 3, TitanPart.BODY_TAIL),
                    new PartCell(6, 4, TitanPart.BODY_TAIL), new PartCell(7, 4, TitanPart.BODY_TAIL),
                    new PartCell(8, 4, TitanPart.BODY_TAIL), new PartCell(6, 7, TitanPart.BODY_TAIL),
                    new PartCell(7, 7, TitanPart.BODY_TAIL)
            );
        }
        if (boss == TitanBoss.MOTHRA) {
            return List.of(
                    new PartCell(4, 2, TitanPart.HEAD), new PartCell(4, 3, TitanPart.HEAD),
                    new PartCell(5, 3, TitanPart.HEAD),
                    new PartCell(4, 4, TitanPart.CORE_FIN), new PartCell(5, 4, TitanPart.CORE_FIN),
                    new PartCell(5, 5, TitanPart.CORE_FIN), new PartCell(1, 6, TitanPart.CORE_FIN),
                    new PartCell(1, 7, TitanPart.CORE_FIN), new PartCell(2, 5, TitanPart.CORE_FIN),
                    new PartCell(2, 6, TitanPart.CORE_FIN), new PartCell(2, 7, TitanPart.CORE_FIN),
                    new PartCell(3, 4, TitanPart.CORE_FIN), new PartCell(3, 5, TitanPart.CORE_FIN),
                    new PartCell(3, 6, TitanPart.CORE_FIN),
                    new PartCell(6, 4, TitanPart.BODY_TAIL), new PartCell(6, 5, TitanPart.BODY_TAIL),
                    new PartCell(4, 5, TitanPart.BODY_TAIL), new PartCell(4, 6, TitanPart.BODY_TAIL),
                    new PartCell(6, 6, TitanPart.BODY_TAIL), new PartCell(7, 5, TitanPart.BODY_TAIL)
            );
        }
        return List.of(
                new PartCell(2, 1, TitanPart.HEAD), new PartCell(2, 2, TitanPart.HEAD),
                new PartCell(3, 2, TitanPart.HEAD),
                new PartCell(2, 4, TitanPart.CORE_FIN), new PartCell(3, 4, TitanPart.CORE_FIN),
                new PartCell(4, 4, TitanPart.CORE_FIN), new PartCell(5, 4, TitanPart.CORE_FIN),
                new PartCell(4, 5, TitanPart.CORE_FIN),
                new PartCell(3, 3, TitanPart.BODY_TAIL), new PartCell(4, 2, TitanPart.BODY_TAIL),
                new PartCell(4, 3, TitanPart.BODY_TAIL), new PartCell(5, 2, TitanPart.BODY_TAIL),
                new PartCell(5, 3, TitanPart.BODY_TAIL), new PartCell(6, 2, TitanPart.BODY_TAIL),
                new PartCell(6, 3, TitanPart.BODY_TAIL), new PartCell(7, 3, TitanPart.BODY_TAIL),
                new PartCell(7, 4, TitanPart.BODY_TAIL), new PartCell(6, 5, TitanPart.BODY_TAIL),
                new PartCell(6, 6, TitanPart.BODY_TAIL), new PartCell(7, 7, TitanPart.BODY_TAIL)
        );
    }

    /** Returns the 20 anatomical colossus hitbox cells on the right 10x10 Titan Arena board. */
    public static List<int[]> generateHitboxCells(TitanBoss boss) {
        return generateHitboxCells(boss, 0, 0);
    }

    /** Returns anatomical hitbox cells shifted by the Boss's current arena offset (offR, offC). */
    public static List<int[]> generateHitboxCells(TitanBoss boss, int offR, int offC) {
        List<PartCell> layout = basePartLayout(boss);
        List<int[]> cells = new ArrayList<>(layout.size());
        for (PartCell pc : layout) {
            addCell(cells, pc.r + offR, pc.c + offC);
        }
        return cells;
    }

    /** Returns all hitbox cells belonging to a specific TitanPart after arena offset shift. */
    public static List<int[]> partCells(TitanBoss boss, TitanPart part, int offR, int offC) {
        if (part == null) return List.of();
        List<int[]> cells = new ArrayList<>();
        for (PartCell pc : basePartLayout(boss)) {
            if (pc.part == part) addCell(cells, pc.r + offR, pc.c + offC);
        }
        return cells;
    }

    /** Identifies which anatomical part occupies arena cell (r, c) given Boss offset (offR, offC), or null if water. */
    public static TitanPart partAtCell(TitanBoss boss, int r, int c, int offR, int offC) {
        for (PartCell pc : basePartLayout(boss)) {
            if (pc.r + offR == r && pc.c + offC == c) return pc.part;
        }
        return null;
    }

    /** Returns high-value anatomical weakpoints (Heads / Dorsal Reactor / Wing Eyespots) for critical glow markers & Sonar reveals. */
    public static List<int[]> generateWeakpointCells(TitanBoss boss) {
        return generateWeakpointCells(boss, 0, 0);
    }

    public static List<int[]> generateWeakpointCells(TitanBoss boss, int offR, int offC) {
        List<int[]> base;
        if (boss == TitanBoss.GHIDORAH) {
            base = List.of(new int[]{3, 2}, new int[]{4, 1}, new int[]{5, 2}, new int[]{4, 3}, new int[]{5, 4});
        } else if (boss == TitanBoss.MOTHRA) {
            base = List.of(new int[]{4, 2}, new int[]{4, 4}, new int[]{2, 6}, new int[]{3, 5}, new int[]{5, 5});
        } else {
            base = List.of(new int[]{2, 1}, new int[]{2, 2}, new int[]{2, 4}, new int[]{3, 4}, new int[]{4, 4});
        }
        if (offR == 0 && offC == 0) return base;
        List<int[]> shifted = new ArrayList<>(base.size());
        for (int[] c : base) addCell(shifted, c[0] + offR, c[1] + offC);
        return shifted;
    }

    public static int weaponBaseCellDamage(CombatLoadout.WeaponOption w) {
        if (w == null) return 12;
        return switch (w) {
            case CANNON -> 12;
            case TORPEDO -> 11;
            case MISSILE_CROSS -> 9;
            case AIRSTRIKE -> 6;
        };
    }

    public static long weaponCooldownMs(CombatLoadout.WeaponOption w) {
        if (w == null) return 1150L;
        return switch (w) {
            case CANNON -> 1150L;
            case TORPEDO -> 3100L;
            case MISSILE_CROSS -> 4400L;
            case AIRSTRIKE -> 5600L;
        };
    }

    public static int weaponStaggerPower(CombatLoadout.WeaponOption w, CombatLoadout.MagicOption m) {
        int base = switch (w != null ? w : CombatLoadout.WeaponOption.CANNON) {
            case CANNON -> 10;
            case TORPEDO -> 18;
            case MISSILE_CROSS -> 22;
            case AIRSTRIKE -> 26;
        };
        if (m == CombatLoadout.MagicOption.LIGHTNING) base += 12;
        else if (m == CombatLoadout.MagicOption.FROST) base += 6;
        return base;
    }

    public static TitanPart chargingPartForSkill(TitanSkill skill) {
        if (skill == null) return TitanPart.HEAD;
        if (skill.spec != null) return skill.spec.chargePart();
        return switch (skill) {
            case GZ_ATOMIC_BREATH, GZ_SPIRAL_RAY, KG_TRIPLE_GRAVITY, KG_ALPHA_STORM, MT_GOD_RAY -> TitanPart.HEAD;
            case GZ_THERMO_PULSE, GZ_MELTDOWN_RIFT, KG_HURRICANE_WING, KG_OMEGA_VORTEX,
                    KG_CATACLYSM_GRAVITY, MT_WING_GALE, MT_PRISM_RAIN, MT_SOLAR_SUPERNOVA -> TitanPart.CORE_FIN;
            case GZ_TAIL_SLAM, MT_SILK_WEB -> TitanPart.BODY_TAIL;
            default -> TitanPart.HEAD;
        };
    }

    public static long skillCastDurationMs(TitanSkill skill, boolean enraged) {
        long base = skill != null && skill.spec != null ? skill.spec.castMs() : switch (skill != null ? skill : TitanSkill.GZ_TAIL_SLAM) {
            case GZ_TAIL_SLAM, KG_HURRICANE_WING, MT_WING_GALE -> 2600L;
            case GZ_ATOMIC_BREATH, KG_TRIPLE_GRAVITY, MT_GOD_RAY, MT_SILK_WEB -> 3100L;
            case GZ_THERMO_PULSE, KG_OMEGA_VORTEX -> 3300L;
            case GZ_SPIRAL_RAY, KG_ALPHA_STORM, MT_PRISM_RAIN -> 2950L;
            case GZ_MELTDOWN_RIFT, KG_CATACLYSM_GRAVITY, MT_SOLAR_SUPERNOVA -> 3400L;
            default -> 3000L;
        };
        return enraged ? Math.round(base * 0.78) : base;
    }

    public static int staggerThresholdFor(TitanBoss boss, boolean enraged) {
        int base = switch (boss != null ? boss : TitanBoss.GODZILLA) {
            case GHIDORAH -> 96;
            case MOTHRA -> 82;
            case GODZILLA -> 88;
            case SPACEGODZILLA -> 100;
            case DESTOROYAH -> 92;
            case ULTIMA -> 110;
        };
        return enraged ? base + 28 : base;
    }

    public static HazardType hazardTypeForSkill(TitanSkill skill) {
        if (skill == null || !skill.leavesImpactZone) return HazardType.NONE;
        if (skill.spec != null) return skill.spec.hazard();
        return switch (skill) {
            case GZ_THERMO_PULSE, GZ_SPIRAL_RAY, GZ_MELTDOWN_RIFT, MT_SOLAR_SUPERNOVA -> HazardType.BURN;
            case MT_SILK_WEB -> HazardType.SILK_SLOW;
            case KG_OMEGA_VORTEX, KG_ALPHA_STORM, KG_CATACLYSM_GRAVITY, MT_PRISM_RAIN, GZ_ATOMIC_BREATH, MT_GOD_RAY -> HazardType.SHOCK;
            default -> HazardType.NONE;
        };
    }

    public static int[] windPushDelta(EnvironmentAction env) {
        if (env == null || env.waveIntensity < 3) return new int[]{0, 0};
        int dr = Math.abs(env.windY) >= 0.9 ? (env.windY > 0 ? 1 : -1) : 0;
        int dc = Math.abs(env.windX) >= 3.8 ? (env.windX > 0 ? 1 : -1) : 0;
        return new int[]{dr, dc};
    }

    public static TitanSkill cataclysmSkillFor(TitanBoss boss) {
        return switch (boss != null ? boss : TitanBoss.GODZILLA) {
            case GHIDORAH -> TitanSkill.KG_CATACLYSM_GRAVITY;
            case MOTHRA -> TitanSkill.MT_SOLAR_SUPERNOVA;
            case GODZILLA -> TitanSkill.GZ_MELTDOWN_RIFT;
            case SPACEGODZILLA -> TitanSkill.SG_COSMIC_NOVA;
            case DESTOROYAH -> TitanSkill.DS_DESTRUCTION_OMEGA;
            case ULTIMA -> switch (ultimaStage) {      // chiêu ★ mạnh nhất của form đang đánh
                case 1 -> TitanSkill.UL1_SCALD_TORRENT;
                case 2 -> TitanSkill.UL2_MAGMA_QUAKE;
                case 3 -> TitanSkill.UL3_GLACIER_BREATH;
                default -> TitanSkill.UL4_FINAL_ROAR;
            };
        };
    }

    /** 13-cell star+cross cataclysm zone used during Phase 2 DPS Check Ultimates. */
    public static List<int[]> cataclysmTargetCells(int row, int col) {
        List<int[]> cells = new ArrayList<>(13);
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) addCell(cells, row + dr, col + dc);
        }
        addCell(cells, row - 2, col);
        addCell(cells, row + 2, col);
        addCell(cells, row, col - 2);
        addCell(cells, row, col + 2);
        return cells;
    }

    /** Environmental manipulation specification: physical ocean water blow, sea-floor quake, and dynamic weather/season/map shifts. */
    public record EnvironmentAction(
            int quakePx,
            double windX,
            double windY,
            Color waterTint,
            int waveIntensity,
            long durationMs,
            OceanMapBackdrop.WeatherMode weather,
            OceanMapBackdrop.Season season,
            OceanMapBackdrop.MapTheme mapTheme,
            String hudMessage
    ) { }

    public static EnvironmentAction environmentForEntrance(TitanBoss boss) {
        if (boss == null) boss = activeBoss;
        return switch (boss) {
            case GHIDORAH -> new EnvironmentAction(
                    12, -3.4, -0.9, new Color(0xFACC15), 2, 2400L,
                    OceanMapBackdrop.WeatherMode.STORM, null, OceanMapBackdrop.MapTheme.GALAXY_COSMOS,
                    "MÔI TRƯỜNG: King Ghidorah gọi siêu bão Bão Tố & gió giật cực mạnh!"
            );
            case MOTHRA -> new EnvironmentAction(
                    8, -3.6, -0.5, new Color(0x34D399), 2, 2200L,
                    OceanMapBackdrop.WeatherMode.STORM, OceanMapBackdrop.Season.SPRING, OceanMapBackdrop.MapTheme.ABYSSAL_ATLANTIS,
                    "MÔI TRƯỜNG: Mothra vỗ cánh thổi sóng bạc đầu & khuấy động biển Atlantis mùa Xuân!"
            );
            case GODZILLA -> new EnvironmentAction(
                    11, -2.5, 0.3, new Color(0x38BDF8), 2, 2200L,
                    OceanMapBackdrop.WeatherMode.STORM, null, null,
                    "MÔI TRƯỜNG: Godzilla trỗi dậy làm rung chuyển đáy biển & dâng bão tố!"
            );
            case SPACEGODZILLA -> new EnvironmentAction(
                    13, -3.0, -1.2, new Color(0xA855F7), 2, 2400L,
                    OceanMapBackdrop.WeatherMode.STORM, null, OceanMapBackdrop.MapTheme.GALAXY_COSMOS,
                    "MÔI TRƯỜNG: SpaceGodzilla giáng xuống từ quỹ đạo, tinh thể mọc khắp mặt biển!"
            );
            case DESTOROYAH -> new EnvironmentAction(
                    12, -2.8, 0.6, new Color(0xDC2626), 2, 2400L,
                    OceanMapBackdrop.WeatherMode.STORM, OceanMapBackdrop.Season.AUTUMN, OceanMapBackdrop.MapTheme.ABYSSAL_ATLANTIS,
                    "MÔI TRƯỜNG: Destoroyah trồi lên từ nghĩa địa tàu, nước biển đỏ sẫm và mất dần oxy!"
            );
            case ULTIMA -> new EnvironmentAction(
                    10, -2.2, 0.4, new Color(0xF97316), 2, 2200L,
                    OceanMapBackdrop.WeatherMode.CLEAR, null, OceanMapBackdrop.MapTheme.FANTASY_REALM,
                    "MÔI TRƯỜNG: Dưới Kết Giới Mothra, một ấu long khổng lồ lượn sát mặt nước…"
            );
        };
    }

    public static EnvironmentAction environmentForEnrage(TitanBoss boss) {
        if (boss == null) boss = activeBoss;
        return switch (boss) {
            case GHIDORAH -> new EnvironmentAction(
                    16, -4.8, -1.2, new Color(0xC084FC), 3, 2800L,
                    OceanMapBackdrop.WeatherMode.STORM, OceanMapBackdrop.Season.WINTER, OceanMapBackdrop.MapTheme.GALAXY_COSMOS,
                    "CUỒNG BẠO MÔI TRƯỜNG: Ghidorah xé toạc bầu trời thành Bão Vũ Trụ Đông giá!"
            );
            case MOTHRA -> new EnvironmentAction(
                    13, -4.2, -0.8, new Color(0xFDE047), 3, 2800L,
                    OceanMapBackdrop.WeatherMode.CLEAR, OceanMapBackdrop.Season.SPRING, OceanMapBackdrop.MapTheme.FANTASY_REALM,
                    "CUỒNG BẠO MÔI TRƯỜNG: Solar Mothra rẽ tan mây bão, khai mở Thần Giới rực sáng!"
            );
            case GODZILLA -> new EnvironmentAction(
                    15, -3.6, 0.4, new Color(0xFB7185), 3, 2800L,
                    OceanMapBackdrop.WeatherMode.STORM, OceanMapBackdrop.Season.SUMMER, OceanMapBackdrop.MapTheme.APOCALYPSE,
                    "CUỒNG BẠO MÔI TRƯỜNG: Burning Godzilla đun sôi đại dương, kích hoạt Biển Lửa!"
            );
            case SPACEGODZILLA -> new EnvironmentAction(
                    17, -4.6, -1.6, new Color(0x38BDF8), 3, 2800L,
                    OceanMapBackdrop.WeatherMode.STORM, OceanMapBackdrop.Season.WINTER, OceanMapBackdrop.MapTheme.GALAXY_COSMOS,
                    "CUỒNG BẠO MÔI TRƯỜNG: Hai cột vai SpaceGodzilla bùng sáng, trọng lực cả vùng đảo lộn!"
            );
            case DESTOROYAH -> new EnvironmentAction(
                    16, -4.0, 1.0, new Color(0xFBBF24), 3, 2800L,
                    OceanMapBackdrop.WeatherMode.STORM, OceanMapBackdrop.Season.AUTUMN, OceanMapBackdrop.MapTheme.APOCALYPSE,
                    "CUỒNG BẠO MÔI TRƯỜNG: Lõi ngực Destoroyah rực vàng, sương oxy đen phủ kín đại dương!"
            );
            case ULTIMA -> new EnvironmentAction(
                    17, -4.4, 0.6, new Color(0xA855F7), 3, 2800L,
                    OceanMapBackdrop.WeatherMode.STORM, OceanMapBackdrop.Season.WINTER, OceanMapBackdrop.MapTheme.GALAXY_COSMOS,
                    "TIẾN HÓA: Godzilla Ultima dựng người đứng dậy, gai lưng bừng sáng lõi băng lam!"
            );
        };
    }

    public static EnvironmentAction environmentForSkill(TitanSkill skill) {
        if (skill == null) {
            return environmentForEntrance(activeBoss);
        }
        if (skill.spec != null) {
            int quake = skill.phase2Only ? 15 : 10;
            return new EnvironmentAction(quake, -3.6, 0.4, skill.accentColor, skill.phase2Only ? 3 : 2, 2000L,
                    skill.phase2Only ? OceanMapBackdrop.WeatherMode.STORM : null, null, null,
                    "MÔI TRƯỜNG: " + skill.skillName + " làm mặt biển rung chuyển!");
        }
        return switch (skill) {
            case GZ_TAIL_SLAM -> new EnvironmentAction(
                    9, -3.2, 0.5, new Color(0x38BDF8), 2, 1600L,
                    null, null, null,
                    "MÔI TRƯỜNG: Quật đuôi tạo sóng thần dạt nước khắp 2 bàn!"
            );
            case GZ_ATOMIC_BREATH -> new EnvironmentAction(
                    11, -3.8, 0.2, new Color(0x22D3EE), 2, 1900L,
                    OceanMapBackdrop.WeatherMode.STORM, null, null,
                    "MÔI TRƯỜNG: Sóng xung kích Cherenkov cuộn trào Bão Tố!"
            );
            case GZ_THERMO_PULSE -> new EnvironmentAction(
                    12, -2.6, -0.8, new Color(0xF97316), 2, 2000L,
                    OceanMapBackdrop.WeatherMode.CLEAR, OceanMapBackdrop.Season.SUMMER, null,
                    "MÔI TRƯỜNG: Xung nhiệt hạch xua mây mù sang nắng nóng mùa Hạ!"
            );
            case GZ_SPIRAL_RAY -> new EnvironmentAction(
                    14, -4.2, 0.3, new Color(0xEF4444), 3, 2100L,
                    OceanMapBackdrop.WeatherMode.STORM, null, null,
                    "MÔI TRƯỜNG: Tia xoắn đỏ kích động áp thấp bão biển dữ dội!"
            );
            case GZ_MELTDOWN_RIFT -> new EnvironmentAction(
                    17, -4.5, 0.5, new Color(0xFB7185), 3, 2400L,
                    OceanMapBackdrop.WeatherMode.STORM, OceanMapBackdrop.Season.SUMMER, OceanMapBackdrop.MapTheme.APOCALYPSE,
                    "MÔI TRƯỜNG: Đáy biển nứt toác! Bản đồ chuyển hẳn sang Tận Thế Biển Lửa & Bão Tố!"
            );
            case KG_TRIPLE_GRAVITY -> new EnvironmentAction(
                    9, -2.9, 0.7, new Color(0xFACC15), 2, 1600L,
                    OceanMapBackdrop.WeatherMode.STORM, null, null,
                    "MÔI TRƯỜNG: Tia trọng lực kéo mây giông sấm chớp tràn mặt biển!"
            );
            case KG_HURRICANE_WING -> new EnvironmentAction(
                    11, -5.0, -1.1, new Color(0xFDE047), 3, 2100L,
                    OceanMapBackdrop.WeatherMode.STORM, null, null,
                    "MÔI TRƯỜNG: Cánh Ghidorah thổi bay mặt nước tạo sóng cuồn cuộn!"
            );
            case KG_OMEGA_VORTEX -> new EnvironmentAction(
                    13, -3.9, -1.5, new Color(0xA855F7), 3, 2000L,
                    OceanMapBackdrop.WeatherMode.STORM, null, null,
                    "MÔI TRƯỜNG: Xoáy điện từ làm mặt biển rung chuyển và dông bão gào thét!"
            );
            case KG_ALPHA_STORM -> new EnvironmentAction(
                    15, -4.6, 0.9, new Color(0xF59E0B), 3, 2200L,
                    OceanMapBackdrop.WeatherMode.STORM, OceanMapBackdrop.Season.WINTER, null,
                    "MÔI TRƯỜNG: Siêu Bão Sét hạ nhiệt độ đại dương xuống băng giá mùa Đông!"
            );
            case KG_CATACLYSM_GRAVITY -> new EnvironmentAction(
                    18, -5.2, -1.4, new Color(0xC084FC), 3, 2500L,
                    OceanMapBackdrop.WeatherMode.STORM, null, OceanMapBackdrop.MapTheme.GALAXY_COSMOS,
                    "MÔI TRƯỜNG: Trọng lực đảo lộn! Toàn vùng biến thành không gian Bão Vũ Trụ!"
            );
            case MT_WING_GALE -> new EnvironmentAction(
                    9, -4.9, -0.7, new Color(0x2DD4BF), 3, 2000L,
                    OceanMapBackdrop.WeatherMode.STORM, null, null,
                    "MÔI TRƯỜNG: Mothra vỗ cánh thần thổi tung sóng gió khắp mặt biển!"
            );
            case MT_GOD_RAY -> new EnvironmentAction(
                    8, -2.4, -1.2, new Color(0xFDE047), 2, 2100L,
                    OceanMapBackdrop.WeatherMode.CLEAR, OceanMapBackdrop.Season.SPRING, null,
                    "MÔI TRƯỜNG: Thánh Quang rẽ tan mây bão thành trời Quang Đãng mùa Xuân!"
            );
            case MT_SILK_WEB -> new EnvironmentAction(
                    7, -2.2, 0.4, new Color(0xA7F3D0), 2, 1800L,
                    null, OceanMapBackdrop.Season.WINTER, null,
                    "MÔI TRƯỜNG: Luồng tơ băng tuyết mang gió lạnh mùa Đông phủ khắp mặt sóng!"
            );
            case MT_PRISM_RAIN -> new EnvironmentAction(
                    12, -3.8, 0.8, new Color(0x38BDF8), 3, 2100L,
                    OceanMapBackdrop.WeatherMode.STORM, OceanMapBackdrop.Season.AUTUMN, null,
                    "MÔI TRƯỜNG: Bão phấn hoa chuyển mùa Thu lá bay & sóng dập dềnh!"
            );
            case MT_SOLAR_SUPERNOVA -> new EnvironmentAction(
                    16, -4.8, -0.9, new Color(0xFB7185), 3, 2500L,
                    OceanMapBackdrop.WeatherMode.CLEAR, OceanMapBackdrop.Season.SPRING, OceanMapBackdrop.MapTheme.FANTASY_REALM,
                    "MÔI TRƯỜNG: Siêu Tân Tinh xua tan giông tố, thắp bừng vùng biển Thần Giới!"
            );
            default -> environmentForEntrance(skill.boss);
        };
    }

    private static void addCell(List<int[]> cells, int r, int c) {
        if (r >= 0 && r < Board.SIZE && c >= 0 && c < Board.SIZE) {
            for (int[] ex : cells) {
                if (ex[0] == r && ex[1] == c) return;
            }
            cells.add(new int[]{r, c});
        }
    }

    /** Frosted-glass pill cycling between Godzilla, King Ghidorah, and Mothra Bosses without text clipping. */
    public static final class TitanPill extends JButton {
        private Color accent = UiTokens.ACCENT_SIGNAL;
        private final boolean compact;

        public TitanPill() {
            this(false);
        }

        public TitanPill(boolean compact) {
            this.compact = compact;
            setFont(UiTokens.FONT_META);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            FontMetrics fm = getFontMetrics(UiTokens.FONT_META);
            int maxW = compact ? 64 : 70;
            for (TitanBoss tb : TitanBoss.values()) maxW = Math.max(maxW, fm.stringWidth(pillText(tb)));
            Dimension pref = new Dimension(maxW + (compact ? 24 : 18), UiTokens.CHIP_HEIGHT);
            setPreferredSize(pref);
            setMinimumSize(pref);
            sync();
        }

        private String pillText(TitanBoss boss) {
            TitanBoss b = boss != null ? boss : TitanBoss.GODZILLA;
            if (!compact) return b.shortLabel;
            return switch (b) {
                case GHIDORAH -> "Ghidorah";
                case MOTHRA -> "Mothra";
                case GODZILLA -> "Godzilla";
                case SPACEGODZILLA -> "SpaceGZ";
                case DESTOROYAH -> "Destoroyah";
                case ULTIMA -> "Ultima";
            };
        }

        public void sync() {
            sync(getActiveBoss());
        }

        public void sync(TitanBoss displayedBoss) {
            TitanBoss boss = displayedBoss != null ? displayedBoss : getActiveBoss();
            setText(pillText(boss));
            this.accent = boss.accentColor;
            setToolTipText("Đổi Boss Titan (6 boss) · Đang chọn: " + boss.displayName);
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int arc = UiTokens.BORDER_RADIUS;
            boolean pressed = getModel().isPressed();
            boolean hover = getModel().isRollover();

            Color topBg = pressed ? new Color(15, 23, 42, 244)
                    : (hover ? new Color(36, 48, 72, 232) : new Color(20, 30, 50, 214));
            Color botBg = pressed ? new Color(10, 17, 30, 248)
                    : (hover ? new Color(24, 34, 54, 238) : new Color(14, 21, 36, 222));
            g.setPaint(new GradientPaint(0, 1, topBg, 0, h - 1, botBg));
            g.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), hover ? 230 : 165));
            g.setStroke(new BasicStroke(hover ? 1.4f : 1.1f));
            g.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

            if (hasFocus()) {
                g.setColor(UiTokens.FOCUS_RING);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(3, 3, w - 7, h - 7, arc, arc);
            }

            int dotX = 11;
            int dotY = h / 2;
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 95));
            g.fillOval(dotX - 5, dotY - 5, 10, 10);
            g.setColor(accent);
            g.fillOval(dotX - 3, dotY - 3, 6, 6);

            g.setFont(getFont());
            g.setColor(UiTokens.TEXT_PRIMARY);
            FontMetrics fm = g.getFontMetrics();
            int textX = 20 + Math.max(0, (w - 24 - fm.stringWidth(getText())) / 2);
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(getText(), textX, textY);
            g.dispose();
        }
    }

    private static void paintFrostedPill(JButton btn, Graphics graphics, Color accent) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = btn.getWidth();
        int h = btn.getHeight();
        int arc = UiTokens.BORDER_RADIUS;
        boolean pressed = btn.getModel().isPressed();
        boolean hover = btn.getModel().isRollover();

        Color topBg = pressed ? new Color(15, 23, 42, 244)
                : (hover ? new Color(36, 48, 72, 232) : new Color(20, 30, 50, 214));
        Color botBg = pressed ? new Color(10, 17, 30, 248)
                : (hover ? new Color(24, 34, 54, 238) : new Color(14, 21, 36, 222));
        g.setPaint(new GradientPaint(0, 1, topBg, 0, h - 1, botBg));
        g.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), hover ? 230 : 165));
        g.setStroke(new BasicStroke(hover ? 1.4f : 1.1f));
        g.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

        int dotX = 10;
        int dotY = h / 2;
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 95));
        g.fillOval(dotX - 4, dotY - 4, 8, 8);
        g.setColor(accent);
        g.fillOval(dotX - 3, dotY - 3, 6, 6);

        g.setFont(btn.getFont());
        g.setColor(UiTokens.TEXT_PRIMARY);
        FontMetrics fm = g.getFontMetrics();
        int textX = 18 + Math.max(0, (w - 22 - fm.stringWidth(btn.getText())) / 2);
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(btn.getText(), textX, textY);
        g.dispose();
    }

    /** Compact frosted pill cycling Raid Difficulty (Thường / Ác Mộng / Tuyệt Diệt). */
    public static final class DifficultyPill extends JButton {
        private Color accent = UiTokens.ACCENT_SIGNAL;

        public DifficultyPill() {
            setFont(UiTokens.FONT_META);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            FontMetrics fm = getFontMetrics(UiTokens.FONT_META);
            int maxW = 54;
            for (TitanDifficulty d : TitanDifficulty.values()) maxW = Math.max(maxW, fm.stringWidth(d.label));
            Dimension pref = new Dimension(maxW + 26, UiTokens.CHIP_HEIGHT);
            setPreferredSize(pref);
            setMinimumSize(pref);
            setMaximumSize(pref);
            sync();
        }

        public void sync() {
            TitanDifficulty d = getDifficulty();
            setText(d.label);
            this.accent = d.accentColor;
            setToolTipText("Độ khó Raid Boss (" + d.label + "): HP ×"
                    + d.hpMult + " · Tốc độ Boss ×" + d.atbSpeedMult + " · Điểm ×" + d.scoreMult);
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            paintFrostedPill(this, g, accent);
        }
    }

    /** Compact toggle pill between Single Boss Raid ("1 Boss") and 6-Boss Gauntlet ("6 Boss"). */
    public static final class RushPill extends JButton {
        private Color accent = UiTokens.STEEL_EDGE;

        public RushPill() {
            setFont(UiTokens.FONT_META);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            FontMetrics fm = getFontMetrics(UiTokens.FONT_META);
            int maxW = Math.max(fm.stringWidth("1 Boss"), fm.stringWidth("6 Boss"));
            Dimension pref = new Dimension(maxW + 28, UiTokens.CHIP_HEIGHT);
            setPreferredSize(pref);
            setMinimumSize(pref);
            setMaximumSize(pref);
            sync();
        }

        public void sync() {
            boolean rush = isBossRushEnabled();
            setText(rush ? "6 Boss" : "1 Boss");
            this.accent = rush ? new Color(0xA855F7) : UiTokens.STEEL_EDGE;
            setToolTipText(rush
                    ? "Chế độ Boss Rush Liên Hoàn (Godzilla → Ghidorah → Mothra → SpaceGodzilla → Destoroyah → Ultima) kèm Tiếp Tế & Xếp Hạng!"
                    : "Đấu 1 Boss đơn lẻ (Click để bật Boss Rush 6 Boss liên hoàn)");
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            paintFrostedPill(this, g, accent);
        }
    }
}
