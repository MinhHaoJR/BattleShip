package battleship.ui;

import battleship.model.Board;
import battleship.model.Ship;
import battleship.model.ShipType;

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
 * Special "VS Zombie" mode specification: 5 Blender 3D Zombie Boss Ship classes,
 * each paired with its own unique necromantic/bio-hazard active skill,
 * plus the shared tactical ModePill selector.
 */
public final class ZombieMode {
    public enum GameMode {
        CLASSIC("Hải Chiến Cổ Điển", "Chế độ: Hải Chiến", new Color(0x38BDF8)),
        ZOMBIE("Đại Chiến Zombie (5 Boss)", "Chế độ: VS Zombie", new Color(0x22C55E)),
        TITAN("Đại Chiến Boss Titan", "Chế độ: VS Titan", new Color(0xF97316));

        public final String displayName;
        public final String shortLabel;
        public final Color accent;

        GameMode(String displayName, String shortLabel, Color accent) {
            this.displayName = displayName;
            this.shortLabel = shortLabel;
            this.accent = accent;
        }
    }

    public enum ZombieClass {
        PLAGUE_DESTROYER(
                ShipType.DESTROYER,
                "Khu Trục Xác Sống",
                "Phun Axit Ăn Mòn",
                "Axit 1×2",
                "Khai hỏa bình axit ăn mòn phá hủy 2 ô liền kề.",
                new Color(0x22C55E),
                SpriteAssets.FxType.SONAR
        ),
        BLIGHT_SUBMARINE(
                ShipType.SUBMARINE,
                "Tàu Ngầm Bào Tử",
                "Ngư Lôi Bào Tử Độc",
                "Bào Tử 1×3",
                "Phóng thủy lôi sinh học nổ dọc theo 3 ô liên tiếp.",
                new Color(0x10B981),
                SpriteAssets.FxType.SONAR
        ),
        NECRO_CRUISER(
                ShipType.CRUISER,
                "Tuần Dương Hồn Ma",
                "Ấn Nguyền Hút Hồn",
                "Nguyền -1 Mana",
                "Giáng sét tử linh chữ X (5 ô) & hút 1 Hải Năng nếu xuyên giáp.",
                new Color(0xA855F7),
                SpriteAssets.FxType.LIGHTNING
        ),
        BLOOD_BATTLESHIP(
                ShipType.BATTLESHIP,
                "Thiết Giáp Huyết Thú",
                "Đại Pháo Huyết Bộc",
                "Huyết Pháo +5",
                "3 tháp pháo huyết ngục dội đạn ma thuật hình dấu + (5 ô).",
                new Color(0xEF4444),
                SpriteAssets.FxType.FIRE
        ),
        ABYSS_CARRIER(
                ShipType.CARRIER,
                "Mẫu Hạm Chúa Quỷ",
                "Bầy Dơi Địa Ngục",
                "Bầy Dơi 3×3",
                "Triệu hồi phi đội Dơi Xương oanh tạc toàn vùng 3×3 (9 ô).",
                new Color(0xF43F5E),
                SpriteAssets.FxType.FIRE
        );

        public final ShipType shipType;
        public final String title;
        public final String skillName;
        public final String chipLabel;
        public final String description;
        public final Color accentColor;
        public final SpriteAssets.FxType impactFx;

        ZombieClass(ShipType shipType, String title, String skillName, String chipLabel,
                    String description, Color accentColor, SpriteAssets.FxType impactFx) {
            this.shipType = shipType;
            this.title = title;
            this.skillName = skillName;
            this.chipLabel = chipLabel;
            this.description = description;
            this.accentColor = accentColor;
            this.impactFx = impactFx;
        }
    }

    private static volatile GameMode activeMode = GameMode.CLASSIC;

    private ZombieMode() { }

    public static GameMode getGameMode() { return activeMode; }

    public static void setGameMode(GameMode mode) {
        activeMode = mode != null ? mode : GameMode.CLASSIC;
    }

    public static GameMode cycleGameMode() {
        GameMode[] all = GameMode.values();
        activeMode = all[(activeMode.ordinal() + 1) % all.length];
        return activeMode;
    }

    public static boolean isZombieMode() { return activeMode == GameMode.ZOMBIE; }

    public static boolean isTitanMode() { return activeMode == GameMode.TITAN; }

    public static ZombieClass forShipType(ShipType type) {
        if (type == null) return ZombieClass.PLAGUE_DESTROYER;
        for (ZombieClass zc : ZombieClass.values()) {
            if (zc.shipType == type) return zc;
        }
        return ZombieClass.PLAGUE_DESTROYER;
    }

    public static List<int[]> skillTargetCells(ZombieClass zc, int row, int col, boolean horizontal) {
        List<int[]> cells = new ArrayList<>();
        if (zc == null) {
            addCell(cells, row, col);
            return cells;
        }
        switch (zc) {
            case PLAGUE_DESTROYER -> {
                addCell(cells, row, col);
                addCell(cells, row + (horizontal ? 0 : 1), col + (horizontal ? 1 : 0));
            }
            case BLIGHT_SUBMARINE -> {
                for (int d = -1; d <= 1; d++) {
                    addCell(cells, row + (horizontal ? 0 : d), col + (horizontal ? d : 0));
                }
            }
            case NECRO_CRUISER -> {
                addCell(cells, row, col);
                addCell(cells, row - 1, col - 1);
                addCell(cells, row - 1, col + 1);
                addCell(cells, row + 1, col - 1);
                addCell(cells, row + 1, col + 1);
            }
            case BLOOD_BATTLESHIP -> {
                addCell(cells, row, col);
                addCell(cells, row - 1, col);
                addCell(cells, row + 1, col);
                addCell(cells, row, col - 1);
                addCell(cells, row, col + 1);
            }
            case ABYSS_CARRIER -> {
                for (int r = row - 1; r <= row + 1; r++) {
                    for (int c = col - 1; c <= col + 1; c++) {
                        addCell(cells, r, c);
                    }
                }
            }
        }
        return cells;
    }

    /** Generates a distinct, non-overlapping 5-ship Zombie Boss formation on the enemy 10x10 chart. */
    public static List<Ship> generateZombieShowcaseFleet() {
        return List.of(
                new Ship(ShipType.CARRIER, 1, 2, true),
                new Ship(ShipType.BATTLESHIP, 3, 1, true),
                new Ship(ShipType.CRUISER, 5, 5, true),
                new Ship(ShipType.SUBMARINE, 5, 1, false),
                new Ship(ShipType.DESTROYER, 8, 6, true)
        );
    }

    private static void addCell(List<int[]> cells, int r, int c) {
        if (r >= 0 && r < Board.SIZE && c >= 0 && c < Board.SIZE) {
            cells.add(new int[]{r, c});
        }
    }

    /** Frosted-glass pill toggling between Classic Naval Battle and VS Zombie Special Mode. */
    public static final class ModePill extends JButton {
        private Color accent = UiTokens.ACCENT_SIGNAL;

        public ModePill() {
            setFont(UiTokens.FONT_META);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            FontMetrics fm = getFontMetrics(UiTokens.FONT_META);
            int maxW = 70;
            for (GameMode gm : GameMode.values()) maxW = Math.max(maxW, fm.stringWidth(gm.shortLabel));
            Dimension pref = new Dimension(maxW + 30, UiTokens.CHIP_HEIGHT);
            setPreferredSize(pref);
            setMinimumSize(pref);
            sync();
        }

        public void sync() {
            GameMode gm = getGameMode();
            setText(gm.shortLabel);
            this.accent = gm.accent;
            setToolTipText("Chuyển chế độ (Hải Chiến Cổ Điển / VS Zombie / Đấu Boss Titan) · Đang chọn: " + gm.displayName);
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
            boolean z = isZombieMode();
            boolean tt = isTitanMode();
            boolean special = z || tt;

            Color topBg = pressed ? new Color(15, 23, 42, 244)
                    : (z ? (hover ? new Color(22, 68, 48, 232) : new Color(16, 48, 36, 218))
                         : (tt ? (hover ? new Color(72, 34, 18, 234) : new Color(52, 24, 14, 220))
                               : (hover ? new Color(34, 49, 75, 224) : new Color(20, 32, 52, 206))));
            Color botBg = pressed ? new Color(10, 17, 30, 248)
                    : (z ? (hover ? new Color(14, 46, 34, 238) : new Color(10, 34, 26, 224))
                         : (tt ? (hover ? new Color(46, 20, 10, 240) : new Color(34, 15, 8, 226))
                               : (hover ? new Color(24, 37, 59, 230) : new Color(14, 23, 39, 214))));
            g.setPaint(new GradientPaint(0, 1, topBg, 0, h - 1, botBg));
            g.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

            g.setColor((hover || special)
                    ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), hover ? 225 : 155)
                    : new Color(255, 255, 255, 48));
            g.setStroke(new BasicStroke((hover || special) ? 1.3f : 1.0f));
            g.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

            if (hasFocus()) {
                g.setColor(UiTokens.FOCUS_RING);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(3, 3, w - 7, h - 7, arc, arc);
            }

            int dotX = 11;
            int dotY = h / 2;
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));
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
