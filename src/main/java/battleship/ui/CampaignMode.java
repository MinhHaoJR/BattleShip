package battleship.ui;

import battleship.model.ShipType;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * 100-Stage Campaign & Minigame Catalog ("Chiến Dịch 1–100").
 * Splits 100 deterministic stages across 5 OceanMapBackdrop chapters (20 stages each),
 * cycling through Naval Skirmish, Limited-Ammo Sniper, Timed Blitz, Sonar Fog,
 * Zombie Admiral Assault, and milestone 3D Titan Kaiju Boss raids every 5 stages.
 */
public final class CampaignMode {
    public static final int TOTAL_STAGES = 100;
    public static final int STAGES_PER_CHAPTER = 20;
    public static final int MAX_TOTAL_STARS = TOTAL_STAGES * 3;

    public enum StageRuleType {
        SKIRMISH(
                "Hải Chiến",
                "Đấu hạm 10×10 chiến thuật — tiêu diệt toàn bộ hạm đội địch ít hao tổn nhất.",
                "stage_node_skirmish",
                new Color(0x38BDF8)
        ),
        SNIPER_AMMO(
                "Bắn Tỉa",
                "Mini-game Giới Hạn Đạn: hạ sạch hạm đội mục tiêu trước khi cạn đạn.",
                "stage_node_minigame",
                new Color(0xFBBF24)
        ),
        BLITZ_TIMED(
                "Đếm Giờ",
                "Mini-game Tốc Độ: đánh chìm hạm đội địch trước khi đồng hồ về 00:00.",
                "stage_node_minigame",
                new Color(0xF97316)
        ),
        SONAR_FOG(
                "Sonar Mù",
                "Mini-game Quét Radar: tận dụng tín hiệu Sonar ban đầu & Hải Năng dồi dào.",
                "stage_node_minigame",
                new Color(0x2DD4BF)
        ),
        ZOMBIE_ASSAULT(
                "Zombie",
                "Đột kích hạm đội Xác Sống mang kỹ năng Axit, Bào Tử & Bầy Dơi!",
                "stage_node_zombie",
                new Color(0x22C55E)
        ),
        TITAN_MILESTONE(
                "Boss Titan",
                "Ải Trùm Kaiju 3D (Arena V2, Ngắt Chiêu & Khiên 3D) mỗi 5 màn!",
                "stage_node_boss",
                new Color(0xEF4444)
        );

        public final String shortLabel;
        public final String description;
        public final String spriteKey;
        public final Color accent;

        StageRuleType(String shortLabel, String description, String spriteKey, Color accent) {
            this.shortLabel = shortLabel;
            this.description = description;
            this.spriteKey = spriteKey;
            this.accent = accent;
        }
    }

    public enum CampaignChapter {
        // Thứ tự theo cốt truyện "Biển Hạch Tâm"; giữ tên hằng cũ vì file khác gọi theo tên.
        CH1_NAVAL(
                0,
                "C1 Hải Chiến",
                "Chương I · Chiến Tranh Biển Aether",
                OceanMapBackdrop.MapTheme.NAVAL_CITADEL,
                1, 20,
                new Color(0x38BDF8),
                "Dòng Hải Lưu & Phao Sonar — Liên Minh Tân Hải đối đầu hạm đội Vel'Kar."
        ),
        CH3_APOCALYPSE(
                1,
                "C2 Tận Thế",
                "Chương II · Mặt Trời Đen",
                OceanMapBackdrop.MapTheme.APOCALYPSE,
                21, 40,
                new Color(0xF97316),
                "Khe Dung Nham (+8% ST Lửa & Đốt AoE) — biển sôi sau bom hạch, Godzilla thức tỉnh."
        ),
        CH4_GALAXY(
                2,
                "C3 Ngân Hà",
                "Chương III · Thiên Thạch Tinh Thể",
                OceanMapBackdrop.MapTheme.GALAXY_COSMOS,
                41, 60,
                new Color(0xA855F7),
                "Trọng Lực Thấp (Lướt 2 ô) & Đá Pha Lê — King Ghidorah rơi xuống, SpaceGodzilla ra đời."
        ),
        CH5_ATLANTIS(
                3,
                "C4 Atlantis",
                "Chương IV · Nghĩa Địa Tàu Chìm",
                OceanMapBackdrop.MapTheme.ABYSSAL_ATLANTIS,
                61, 80,
                new Color(0x2DD4BF),
                "Trụ Cổ Ngữ (+35% Khoan & Giảm 35% ST Boss) — hạm đội xác sống và bầy Destoroyah."
        ),
        CH2_FANTASY(
                4,
                "C5 Kết Giới",
                "Chương V · Kết Giới Mothra",
                OceanMapBackdrop.MapTheme.FANTASY_REALM,
                81, 100,
                new Color(0xF472B6),
                "2 Vòng Kết Giới (-35% CD & +Khiên/s) — Mothra che chở trận cuối, Màn 100!"
        );

        public final int index;
        public final String shortTab;
        public final String fullTitle;
        public final OceanMapBackdrop.MapTheme theme;
        public final int startStage;
        public final int endStage;
        public final Color accent;
        public final String subtitle;

        CampaignChapter(
                int index,
                String shortTab,
                String fullTitle,
                OceanMapBackdrop.MapTheme theme,
                int startStage,
                int endStage,
                Color accent,
                String subtitle
        ) {
            this.index = index;
            this.shortTab = shortTab;
            this.fullTitle = fullTitle;
            this.theme = theme;
            this.startStage = startStage;
            this.endStage = endStage;
            this.accent = accent;
            this.subtitle = subtitle;
        }

        public static CampaignChapter byIndex(int index) {
            CampaignChapter[] vals = values();
            int clamped = Math.max(0, Math.min(vals.length - 1, index));
            return vals[clamped];
        }
    }

    public record CampaignStage(
            int stageNumber,
            CampaignChapter chapter,
            int indexInChapter,
            String title,
            StageRuleType ruleType,
            OceanMapBackdrop.Season season,
            OceanMapBackdrop.WeatherMode weather,
            List<ShipType> enemyFleetTypes,
            int shotLimit,
            int timeLimitSec,
            int initialMana,
            int initialSonarHints,
            TitanMode.TitanBoss boss,
            TitanMode.TitanDifficulty bossDifficulty,
            int bossHpOverride,
            int parShots,
            String ruleSummary,
            String threeStarCondition,
            long seed
    ) {
        public boolean isBossStage() { return ruleType == StageRuleType.TITAN_MILESTONE; }
    }

    private static final String[] THEATRE_NAMES = {
            "Vịnh Xanh", "Eo Bão", "Mũi Đá Ngầm", "Pháo Đài Cửa Biển",
            "Vành Đai Tro", "Biển Miệng Núi", "Lò Nung Dung Nham", "Rãnh Vực Sâu",
            "Vành Đai Sao", "Tinh Vân Xoáy", "Hố Đen Tím", "Trạm Quỹ Đạo",
            "Nghĩa Địa Tàu Chìm", "Đền Đinh Ba", "Vực San Hô", "Thành Đô Chìm",
            "Vùng Trời Hồng", "Thác Ánh Sao", "Hồ Phù Thủy", "Thánh Điện Rồng"
    };

    private static final List<CampaignStage> STAGES = buildAllStages();
    private static final int[] BEST_STARS = new int[TOTAL_STAGES + 1];
    private static final int[] BEST_SCORES = new int[TOTAL_STAGES + 1];

    private static boolean sequentialUnlockOnly = false;
    private static int highestUnlockedStage = 1;
    private static boolean loadedPrefs = false;
    private static boolean forceInMemoryOnly = false;

    private CampaignMode() { }

    public static List<CampaignStage> allStages() {
        return STAGES;
    }

    public static CampaignStage stage(int stageNumber) {
        int clamped = Math.max(1, Math.min(TOTAL_STAGES, stageNumber));
        return STAGES.get(clamped - 1);
    }

    public static List<CampaignStage> stagesForChapter(int chapterIndex) {
        int idx = Math.max(0, Math.min(CampaignChapter.values().length - 1, chapterIndex));
        int from = idx * STAGES_PER_CHAPTER;
        return STAGES.subList(from, from + STAGES_PER_CHAPTER);
    }

    public static CampaignChapter chapterOf(int stageNumber) {
        return stage(stageNumber).chapter();
    }

    public static synchronized boolean isSequentialUnlockOnly() {
        ensureLoaded();
        return sequentialUnlockOnly;
    }

    public static synchronized void setSequentialUnlockOnly(boolean sequential) {
        ensureLoaded();
        sequentialUnlockOnly = sequential;
        persist();
    }

    public static synchronized boolean toggleSequentialUnlockOnly() {
        setSequentialUnlockOnly(!isSequentialUnlockOnly());
        return sequentialUnlockOnly;
    }

    public static synchronized boolean isUnlocked(int stageNumber) {
        ensureLoaded();
        if (stageNumber < 1 || stageNumber > TOTAL_STAGES) return false;
        return !sequentialUnlockOnly || stageNumber <= highestUnlockedStage;
    }

    public static synchronized int getHighestUnlockedStage() {
        ensureLoaded();
        return highestUnlockedStage;
    }

    public static synchronized int getStars(int stageNumber) {
        ensureLoaded();
        if (stageNumber < 1 || stageNumber > TOTAL_STAGES) return 0;
        return BEST_STARS[stageNumber];
    }

    public static synchronized int getBestScore(int stageNumber) {
        ensureLoaded();
        if (stageNumber < 1 || stageNumber > TOTAL_STAGES) return 0;
        return BEST_SCORES[stageNumber];
    }

    public static synchronized int totalStars() {
        ensureLoaded();
        int sum = 0;
        for (int i = 1; i <= TOTAL_STAGES; i++) sum += BEST_STARS[i];
        return sum;
    }

    public static synchronized int chapterStars(int chapterIndex) {
        ensureLoaded();
        int idx = Math.max(0, Math.min(CampaignChapter.values().length - 1, chapterIndex));
        int start = idx * STAGES_PER_CHAPTER + 1;
        int sum = 0;
        for (int s = start; s < start + STAGES_PER_CHAPTER; s++) sum += BEST_STARS[s];
        return sum;
    }

    public static synchronized int clearedCount() {
        ensureLoaded();
        int count = 0;
        for (int i = 1; i <= TOTAL_STAGES; i++) {
            if (BEST_STARS[i] > 0) count++;
        }
        return count;
    }

    public static synchronized void recordVictory(int stageNumber, int stars, int score) {
        ensureLoaded();
        if (stageNumber < 1 || stageNumber > TOTAL_STAGES) return;
        int clampedStars = Math.max(1, Math.min(3, stars));
        if (clampedStars > BEST_STARS[stageNumber]) BEST_STARS[stageNumber] = clampedStars;
        if (score > BEST_SCORES[stageNumber]) BEST_SCORES[stageNumber] = score;
        if (stageNumber >= highestUnlockedStage && stageNumber < TOTAL_STAGES) {
            highestUnlockedStage = stageNumber + 1;
        }
        persist();
    }

    public static synchronized void setForceInMemoryOnlyForTest(boolean inMemoryOnly) {
        forceInMemoryOnly = inMemoryOnly;
    }

    public static synchronized void resetProgressInMemory() {
        for (int i = 0; i <= TOTAL_STAGES; i++) {
            BEST_STARS[i] = 0;
            BEST_SCORES[i] = 0;
        }
        sequentialUnlockOnly = false;
        highestUnlockedStage = 1;
        loadedPrefs = true;
    }

    private static boolean shouldPersist() {
        if (forceInMemoryOnly) return false;
        if (Boolean.getBoolean("battleship.silent")) return false;
        return !"true".equalsIgnoreCase(System.getenv("BATTLESHIP_SILENT"));
    }

    private static void ensureLoaded() {
        if (loadedPrefs) return;
        loadedPrefs = true;
        if (!shouldPersist()) return;
        try {
            Preferences prefs = Preferences.userNodeForPackage(CampaignMode.class);
            sequentialUnlockOnly = prefs.getBoolean("campaign.sequentialOnly", false);
            highestUnlockedStage = Math.max(1, Math.min(TOTAL_STAGES, prefs.getInt("campaign.highestUnlocked", 1)));
            for (int i = 1; i <= TOTAL_STAGES; i++) {
                BEST_STARS[i] = Math.max(0, Math.min(3, prefs.getInt("campaign.star." + i, 0)));
                BEST_SCORES[i] = Math.max(0, prefs.getInt("campaign.score." + i, 0));
            }
        } catch (Throwable ignored) {
            // Fall back silently to clean in-memory state if OS prefs are unavailable
        }
    }

    private static void persist() {
        if (!shouldPersist()) return;
        try {
            Preferences prefs = Preferences.userNodeForPackage(CampaignMode.class);
            prefs.putBoolean("campaign.sequentialOnly", sequentialUnlockOnly);
            prefs.putInt("campaign.highestUnlocked", highestUnlockedStage);
            for (int i = 1; i <= TOTAL_STAGES; i++) {
                if (BEST_STARS[i] > 0) prefs.putInt("campaign.star." + i, BEST_STARS[i]);
                if (BEST_SCORES[i] > 0) prefs.putInt("campaign.score." + i, BEST_SCORES[i]);
            }
        } catch (Throwable ignored) {
            // Non-fatal in locked-down sandboxes
        }
    }

    private static List<CampaignStage> buildAllStages() {
        List<CampaignStage> list = new ArrayList<>(TOTAL_STAGES);
        CampaignChapter[] chapters = CampaignChapter.values();
        OceanMapBackdrop.Season[] seasons = {
                OceanMapBackdrop.Season.SPRING,
                OceanMapBackdrop.Season.SUMMER,
                OceanMapBackdrop.Season.AUTUMN,
                OceanMapBackdrop.Season.WINTER
        };

        for (int num = 1; num <= TOTAL_STAGES; num++) {
            int chapIdx = (num - 1) / STAGES_PER_CHAPTER;
            int idxInChap = (num - 1) % STAGES_PER_CHAPTER; // 0..19
            int oneBasedInChap = idxInChap + 1;             // 1..20
            CampaignChapter chap = chapters[chapIdx];
            // Truyện: chương I chỉ có hải chiến (không Titan); màn 70 là trận AURORA-07 của hạm đội xác sống.
            StageRuleType rule = (chapIdx == 0 && oneBasedInChap % 5 == 0) ? StageRuleType.SKIRMISH
                    : (num == 70 ? StageRuleType.ZOMBIE_ASSAULT : resolveRuleForPos(oneBasedInChap));

            OceanMapBackdrop.Season season = seasons[(chapIdx + idxInChap / 5) % seasons.length];
            OceanMapBackdrop.WeatherMode weather = (rule == StageRuleType.SONAR_FOG
                    || rule == StageRuleType.TITAN_MILESTONE && oneBasedInChap == 20
                    || oneBasedInChap >= 16)
                    ? OceanMapBackdrop.WeatherMode.STORM
                    : OceanMapBackdrop.WeatherMode.CLEAR;

            List<ShipType> fleet = resolveEnemyFleet(rule, chapIdx, oneBasedInChap);
            int shipCells = totalCells(fleet);

            int shotLimit = 0;
            int timeLimitSec = 0;
            int initialMana = 6 + Math.min(4, chapIdx + (rule == StageRuleType.SONAR_FOG ? 2 : 0));
            int sonarHints = 0;
            int parShots = Math.max(12, shipCells + 14);
            TitanMode.TitanBoss boss = null;
            TitanMode.TitanDifficulty bossDiff = TitanMode.TitanDifficulty.NORMAL;
            int bossHp = 0;

            String title;
            String ruleSummary;
            String starCond;
            String zone = THEATRE_NAMES[(chapIdx * 4 + idxInChap / 5) % THEATRE_NAMES.length];

            switch (rule) {
                case SNIPER_AMMO -> {
                    int margin = Math.max(10, 18 - chapIdx * 2 - (oneBasedInChap / 6));
                    shotLimit = shipCells + margin;
                    parShots = Math.max(shipCells + 4, shotLimit - 5);
                    title = "Màn " + num + " · Bắn Tỉa " + zone;
                    ruleSummary = "Địch giấu " + fleet.size() + " tàu (" + shipCells + " ô). Bạn chỉ có tối đa "
                            + shotLimit + " phát bắn + combo Vũ Khí/Phép để hạ sạch.";
                    starCond = "★ Thắng trong " + shotLimit + " phát  ·  ★★ ≤ " + (parShots + 3) + " phát  ·  ★★★ ≤ "
                            + parShots + " phát & còn ≥60% tàu";
                }
                case BLITZ_TIMED -> {
                    timeLimitSec = Math.max(68, 108 - chapIdx * 7 - (oneBasedInChap >= 14 ? 8 : 0));
                    parShots = Math.max(14, shipCells + 12);
                    title = "Màn " + num + " · Chớp Nhoáng " + zone;
                    ruleSummary = "Đánh chìm " + fleet.size() + " tàu địch (" + shipCells + " ô) trong vòng "
                            + timeLimitSec + " giây trước khi hết giờ!";
                    starCond = "★ Thắng trước 00:00  ·  ★★ Còn ≥15s  ·  ★★★ Còn ≥30s & ≥60% tàu";
                }
                case SONAR_FOG -> {
                    sonarHints = Math.max(2, 4 - chapIdx / 2);
                    initialMana = CombatLoadout.MAX_MANA;
                    parShots = Math.max(14, shipCells + 11);
                    title = "Màn " + num + " · Sương Mù " + zone;
                    ruleSummary = "Biển bão mù sương nhưng Radar quét sẵn " + sonarHints
                            + " tọa độ tàu địch & khởi đầu Full " + initialMana + " Hải Năng!";
                    starCond = "★ Thắng màn  ·  ★★ ≤ " + (parShots + 4) + " phát  ·  ★★★ ≤ "
                            + parShots + " phát & ≥60% tàu";
                }
                case ZOMBIE_ASSAULT -> {
                    parShots = Math.max(18, shipCells + 14);
                    title = "Màn " + num + " · Hạm Zombie " + zone;
                    ruleSummary = "Đụng độ " + fleet.size() + " chiến hạm Xác Sống phản đòn bằng Axit, Bào Tử & Bầy Dơi!";
                    starCond = "★ Hạ hết hạm Zombie  ·  ★★ Còn ≥40% tàu  ·  ★★★ ≤ " + parShots + " phát & ≥60% tàu";
                }
                case TITAN_MILESTONE -> {
                    boss = standInBoss(chapIdx, oneBasedInChap);
                    bossDiff = num >= 80
                            ? TitanMode.TitanDifficulty.CATACLYSM
                            : (num >= 45 ? TitanMode.TitanDifficulty.NIGHTMARE : TitanMode.TitanDifficulty.NORMAL);
                    bossHp = Math.min(490, 165 + (num - 5) * 3 + (oneBasedInChap == 20 ? 35 : 0));
                    if (oneBasedInChap == 20 && num >= 40) {
                        timeLimitSec = Math.max(95, 135 - chapIdx * 6);
                    }
                    parShots = 24;
                    String bossShort = storyBossName(num).toUpperCase();
                    String climaxTag = (num == 100) ? "ĐẠI CHIẾN TỐI THƯỢNG" : ("Trùm Mốc " + num);
                    title = "Màn " + num + " · BOSS " + bossShort + " (" + climaxTag + ")";
                    ruleSummary = "Đối đầu " + storyBossName(num) + " (" + bossHp + " HP · " + bossDiff.label
                            + ") trên " + chap.theme.displayName
                            + (timeLimitSec > 0 ? " trong giới hạn " + timeLimitSec + "s." : "!");
                    starCond = "★ Hạ Boss Titan  ·  ★★ Ngắt chiêu/rụng chi + ≥40% tàu  ·  ★★★ ≥65% tàu & hạng A/S";
                }
                default -> {
                    parShots = Math.max(16, shipCells + 13);
                    title = "Màn " + num + " · Hải Chiến " + zone;
                    ruleSummary = "Giao tranh chiến thuật 10×10 cùng " + fleet.size() + " chiến hạm địch ("
                            + shipCells + " ô) trên vùng biển " + chap.theme.shortLabel + ".";
                    starCond = "★ Thắng màn  ·  ★★ ≤ " + (parShots + 5) + " phát  ·  ★★★ ≤ " + parShots + " phát & ≥60% tàu";
                }
            }

            long seed = 0xB477E300L + (long) num * 7919L;
            list.add(new CampaignStage(
                    num, chap, idxInChap, title, rule, season, weather, fleet,
                    shotLimit, timeLimitSec, initialMana, sonarHints,
                    boss, bossDiff, bossHp, parShots, ruleSummary, starCond, seed
            ));
        }
        return Collections.unmodifiableList(list);
    }

    /** Boss Titan đánh ở các màn mốc của chương (chỉ số chương theo thứ tự truyện). */
    private static TitanMode.TitanBoss standInBoss(int chapIdx, int posInChapter) {
        return switch (chapIdx) {
            case 2 -> posInChapter < 20 ? TitanMode.TitanBoss.GHIDORAH : TitanMode.TitanBoss.SPACEGODZILLA;
            case 3 -> TitanMode.TitanBoss.DESTOROYAH;
            case 4 -> TitanMode.TitanBoss.ULTIMA;
            default -> TitanMode.TitanBoss.GODZILLA;
        };
    }

    /** Tên quái vật/đối thủ trong truyện ở các màn mốc (5, 10, 15, 20 của mỗi chương). */
    public static String storyBossName(int stageNumber) {
        int chapIdx = (stageNumber - 1) / STAGES_PER_CHAPTER;
        int pos = (stageNumber - 1) % STAGES_PER_CHAPTER + 1;
        return switch (chapIdx) {
            case 0 -> "Soái Hạm Raskov";
            case 1 -> "Godzilla";
            case 2 -> pos < 20 ? "King Ghidorah" : "SpaceGodzilla";
            case 3 -> pos == 10 ? "AURORA-07" : "Destoroyah";
            default -> "Godzilla Ultima";
        };
    }

    private static StageRuleType resolveRuleForPos(int posInChapter) {
        if (posInChapter % 5 == 0) return StageRuleType.TITAN_MILESTONE;
        return switch (posInChapter) {
            case 3, 8, 13, 18 -> StageRuleType.SNIPER_AMMO;
            case 4, 9, 14, 19 -> StageRuleType.BLITZ_TIMED;
            case 2, 7, 17 -> StageRuleType.SONAR_FOG;
            case 6, 12, 16 -> StageRuleType.ZOMBIE_ASSAULT;
            default -> StageRuleType.SKIRMISH;
        };
    }

    private static List<ShipType> resolveEnemyFleet(StageRuleType rule, int chapIdx, int posInChapter) {
        if (rule == StageRuleType.TITAN_MILESTONE) return List.of();
        if (rule == StageRuleType.SNIPER_AMMO) {
            if (chapIdx == 0 && posInChapter <= 8) return List.of(ShipType.CRUISER, ShipType.DESTROYER);
            if (chapIdx <= 1) return List.of(ShipType.BATTLESHIP, ShipType.CRUISER, ShipType.DESTROYER);
            return List.of(ShipType.BATTLESHIP, ShipType.CRUISER, ShipType.SUBMARINE, ShipType.DESTROYER);
        }
        if (rule == StageRuleType.BLITZ_TIMED) {
            if (chapIdx == 0) return List.of(ShipType.CRUISER, ShipType.SUBMARINE, ShipType.DESTROYER);
            if (chapIdx <= 2) return List.of(ShipType.BATTLESHIP, ShipType.CRUISER, ShipType.SUBMARINE, ShipType.DESTROYER);
            return List.of(ShipType.values());
        }
        if (rule == StageRuleType.ZOMBIE_ASSAULT) {
            if (chapIdx == 0 && posInChapter < 10) {
                return List.of(ShipType.BATTLESHIP, ShipType.CRUISER, ShipType.SUBMARINE, ShipType.DESTROYER);
            }
            return List.of(ShipType.values());
        }
        if (rule == StageRuleType.SONAR_FOG) {
            if (chapIdx == 0) return List.of(ShipType.BATTLESHIP, ShipType.CRUISER, ShipType.DESTROYER);
            return List.of(ShipType.CARRIER, ShipType.BATTLESHIP, ShipType.CRUISER, ShipType.DESTROYER);
        }
        if (chapIdx == 0 && posInChapter % 5 == 0) return List.of(ShipType.values()); // màn mốc chương I: đủ hạm đội Vel'Kar
        // SKIRMISH scales smoothly from quick 3-ship starter (Stage 1) up to full 5-ship fleet
        if (chapIdx == 0 && posInChapter == 1) {
            return List.of(ShipType.CRUISER, ShipType.SUBMARINE, ShipType.DESTROYER);
        }
        if (chapIdx == 0) {
            return List.of(ShipType.BATTLESHIP, ShipType.CRUISER, ShipType.SUBMARINE, ShipType.DESTROYER);
        }
        return List.of(ShipType.values());
    }

    private static int totalCells(List<ShipType> types) {
        int total = 0;
        for (ShipType t : types) total += t.length;
        return total;
    }
}
