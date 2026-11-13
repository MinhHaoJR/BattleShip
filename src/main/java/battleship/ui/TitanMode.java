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
                20
        ),
        GHIDORAH(
                "King Ghidorah · Tam Đầu Kim Long",
                "Boss: Ghidorah",
                new Color(0xFACC15),
                new Color(0xA855F7),
                20
        ),
        MOTHRA(
                "Mothra · Nữ Hoàng Thiên Nhiên",
                "Boss: Mothra",
                new Color(0x34D399),
                new Color(0xFDE047),
                20
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
                30
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
                35
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
                40
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
                48
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
                55
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
                32
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
                30
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
                40
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
                48
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
                55
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
                30
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
                36
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
                38
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
                48
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
                55
        );

        public final TitanBoss boss;
        public final String skillName;
        public final String chipLabel;
        public final String description;
        public final Color accentColor;
        public final boolean leavesImpactZone;
        public final boolean beamStyle;
        public final boolean phase2Only;
        public final int shieldDamage;

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
        }
    }

    private static volatile TitanBoss activeBoss = TitanBoss.GODZILLA;
    private static volatile boolean titanEnraged = false;

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

    public static boolean isTitanEnraged() { return titanEnraged; }

    public static void setTitanEnraged(boolean enraged) {
        titanEnraged = enraged;
    }

    public static List<TitanSkill> skillsFor(TitanBoss boss) {
        TitanBoss b = boss != null ? boss : activeBoss;
        List<TitanSkill> out = new ArrayList<>(5);
        for (TitanSkill skill : TitanSkill.values()) {
            if (skill.boss == b) out.add(skill);
        }
        return out;
    }

    public static List<TitanSkill> skillsFor(TitanBoss boss, boolean enraged) {
        TitanBoss b = boss != null ? boss : activeBoss;
        List<TitanSkill> out = new ArrayList<>(5);
        for (TitanSkill skill : TitanSkill.values()) {
            if (skill.boss == b && (enraged || !skill.phase2Only)) out.add(skill);
        }
        return out;
    }

    public static CombatLoadout.MagicOption impactZoneMagic(TitanSkill skill) {
        if (skill == null) return CombatLoadout.MagicOption.FIRE;
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
                // 3-head diverging gravity strikes across 5 tactical cells
                addCell(cells, row, col);
                addCell(cells, row - 2, col - 1);
                addCell(cells, row + 2, col - 1);
                addCell(cells, row - 1, col + 2);
                addCell(cells, row + 1, col + 2);
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
        }
        if (cells.isEmpty()) addCell(cells, row, col);
        return cells;
    }

    /** Returns the 20 anatomical colossus hitbox cells on the right 10x10 Titan Arena board. */
    public static List<int[]> generateHitboxCells(TitanBoss boss) {
        if (boss == TitanBoss.GHIDORAH) {
            return List.of(
                    // 3 Dragon Heads & Necks (left-facing tri-crown in cols B-D, rows 3-5)
                    new int[]{3, 2}, new int[]{4, 1}, new int[]{4, 2}, new int[]{5, 2}, new int[]{4, 3},
                    // Armored Golden Chest / Torso (cols D-E, rows 5-8)
                    new int[]{5, 3}, new int[]{5, 4}, new int[]{6, 3}, new int[]{6, 4}, new int[]{7, 4}, new int[]{8, 4},
                    // Spread Twin Golden Wings (cols E-H, rows 2-6)
                    new int[]{2, 5}, new int[]{3, 5}, new int[]{3, 6},
                    new int[]{4, 5}, new int[]{4, 6}, new int[]{5, 5}, new int[]{5, 6},
                    // Twin Tails (cols G-H, rows 6-7)
                    new int[]{6, 7}, new int[]{7, 7}
            );
        }
        if (boss == TitanBoss.MOTHRA) {
            // 20-cell Mothra butterfly silhouette hovering slightly left-facing
            return List.of(
                    // Head & Luminous Eyes (cols C-D, rows 4-5)
                    new int[]{4, 2}, new int[]{4, 3}, new int[]{5, 3},
                    // Thorax & Abdomen Core (cols E-F, rows 4-6)
                    new int[]{4, 4}, new int[]{5, 4}, new int[]{6, 4}, new int[]{5, 5}, new int[]{6, 5},
                    // Towering Upper Luminous Forewings (rows 1-3, cols E-H)
                    new int[]{1, 6}, new int[]{1, 7}, new int[]{2, 5}, new int[]{2, 6},
                    new int[]{2, 7}, new int[]{3, 4}, new int[]{3, 5}, new int[]{3, 6},
                    // Swept Hindwings & Wingtips (rows 4,6,7)
                    new int[]{4, 5}, new int[]{4, 6}, new int[]{6, 6}, new int[]{7, 5}
            );
        }
        // GODZILLA 20-cell kaiju silhouette (Head B3, Dorsal Atomic Fins D-E 2-5, Chest & Legs B-E 4-8, Tail F-H 6-7)
        return List.of(
                // Head & Jaws (weakpoint crown)
                new int[]{2, 1}, new int[]{2, 2}, new int[]{3, 2},
                // Dorsal Atomic Spines (weakpoints along back ridge)
                new int[]{2, 4}, new int[]{3, 4}, new int[]{4, 4}, new int[]{5, 4}, new int[]{4, 5},
                // Armored Torso & Pillar Legs
                new int[]{3, 3}, new int[]{4, 2}, new int[]{4, 3},
                new int[]{5, 2}, new int[]{5, 3},
                new int[]{6, 2}, new int[]{6, 3},
                new int[]{7, 3}, new int[]{7, 4},
                // Sweeping Spiked Tail
                new int[]{6, 5}, new int[]{6, 6}, new int[]{7, 7}
        );
    }

    /** Returns high-value anatomical weakpoints (Heads / Dorsal Reactor / Wing Eyespots) for critical glow markers & Sonar reveals. */
    public static List<int[]> generateWeakpointCells(TitanBoss boss) {
        if (boss == TitanBoss.GHIDORAH) {
            return List.of(new int[]{3, 2}, new int[]{4, 1}, new int[]{5, 2}, new int[]{4, 3}, new int[]{5, 4});
        }
        if (boss == TitanBoss.MOTHRA) {
            return List.of(new int[]{4, 2}, new int[]{4, 4}, new int[]{2, 6}, new int[]{3, 5}, new int[]{5, 5});
        }
        return List.of(new int[]{2, 1}, new int[]{2, 2}, new int[]{2, 4}, new int[]{3, 4}, new int[]{4, 4});
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
        };
    }

    public static EnvironmentAction environmentForSkill(TitanSkill skill) {
        if (skill == null) {
            return environmentForEntrance(activeBoss);
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

        public TitanPill() {
            setFont(UiTokens.FONT_META);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            FontMetrics fm = getFontMetrics(UiTokens.FONT_META);
            int maxW = 70;
            for (TitanBoss tb : TitanBoss.values()) maxW = Math.max(maxW, fm.stringWidth(tb.shortLabel));
            Dimension pref = new Dimension(maxW + 24, UiTokens.CHIP_HEIGHT);
            setPreferredSize(pref);
            setMinimumSize(pref);
            sync();
        }

        public void sync() {
            sync(getActiveBoss());
        }

        public void sync(TitanBoss displayedBoss) {
            TitanBoss boss = displayedBoss != null ? displayedBoss : getActiveBoss();
            setText(boss.shortLabel);
            this.accent = boss.accentColor;
            setToolTipText("Đổi Boss Titan (Godzilla / King Ghidorah / Mothra) · Đang chọn: " + boss.displayName);
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
}
