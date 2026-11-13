package battleship.ui;

import java.awt.Color;

/** UI-side names and previews for weapon and magic selections. */
public final class CombatLoadout {
    public static final int STARTING_MANA = 2;
    public static final int MAX_MANA = 5;
    public static final int SUPER_MAX_MANA = 7;

    private CombatLoadout() { }

    public enum WeaponOption {
        CANNON("Pháo Hạm", 0, "Bắn một ô."),
        TORPEDO("Ngư Lôi", 2, "Bắn 3 ô thẳng theo hướng đã chọn."),
        MISSILE_CROSS("Tên Lửa +", 3, "Đánh 5 ô theo hình chữ thập."),
        AIRSTRIKE("Không Kích", 3, "Oanh tạc vùng 3×3 và để lại vùng va chạm liên hoàn.");

        public final String label;
        public final int manaCost;
        public final String description;

        WeaponOption(String label, int manaCost, String description) {
            this.label = label;
            this.manaCost = manaCost;
            this.description = description;
        }
    }

    public enum MagicOption {
        NONE("Thuần", 0, UiTokens.ACCENT_SIGNAL, "Không ép phép, khai hỏa thường."),
        FIRE("Lửa", 2, UiTokens.MAGIC_FIRE, "Trúng tàu sẽ cháy lan thêm 1 ô kề."),
        LIGHTNING("Sét", 2, UiTokens.MAGIC_LIGHTNING, "Trúng ít nhất 1 ô tàu được bắn thêm lượt."),
        ARCANE_SONAR("Cổ Ngữ", 1, UiTokens.MAGIC_SONAR, "Quét radar 3×3 quanh tâm."),
        FROST("Băng", 2, UiTokens.MAGIC_FROST, "Đóng băng hệ thống ngắm của máy 1 lượt.");

        public final String label;
        public final int manaCost;
        public final Color accentColor;
        public final String description;

        MagicOption(String label, int manaCost, Color accentColor, String description) {
            this.label = label;
            this.manaCost = manaCost;
            this.accentColor = accentColor;
            this.description = description;
        }
    }
}
