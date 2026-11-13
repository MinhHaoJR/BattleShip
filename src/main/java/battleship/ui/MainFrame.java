package battleship.ui;

import battleship.logic.Game;
import battleship.model.Board;
import battleship.model.Ship;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/** Main application window with setup and battle screens managed by a CardLayout. */
public class MainFrame extends JFrame {
    public static final String SCREEN_SETUP = "setup";
    public static final String SCREEN_GAME = "game";

    private final CardLayout screenLayout = new CardLayout();
    private final JPanel screenCards = new JPanel(screenLayout);
    private final HeaderBar headerBar = new HeaderBar();
    private SetupPanel setupPanel;
    private GamePanel gamePanel;

    public MainFrame() {
        super("BattleShip — Hải Đồ Tác Chiến");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(UiTokens.BG_CANVAS);
        setLayout(new BorderLayout());
        add(headerBar, BorderLayout.NORTH);
        screenCards.setOpaque(true);
        screenCards.setBackground(UiTokens.BG_CANVAS);
        setupPanel = createSetupPanel();
        screenCards.add(setupPanel, SCREEN_SETUP);
        add(screenCards, BorderLayout.CENTER);
        headerBar.setPhase("TRIỂN KHAI HẠM ĐỘI");
        setMinimumSize(new Dimension(UiTokens.FRAME_MIN_WIDTH, UiTokens.FRAME_MIN_HEIGHT));
        setSize(UiTokens.FRAME_WIDTH, UiTokens.FRAME_HEIGHT);
        setLocationRelativeTo(null);
        SoundEngine.warmUpAsync();
        SpriteAssets.warmUpAsync();
        SoundEngine.syncMusicState(false);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { SoundEngine.setEnvironmentAudioActive(true); }
            @Override public void windowDeiconified(WindowEvent e) { SoundEngine.setEnvironmentAudioActive(true); }
            @Override public void windowIconified(WindowEvent e) { SoundEngine.setEnvironmentAudioActive(false); }
            @Override public void windowClosing(WindowEvent e) { SoundEngine.setEnvironmentAudioActive(false); }
            @Override public void windowClosed(WindowEvent e) { SoundEngine.setEnvironmentAudioActive(false); }
        });
    }

    private SetupPanel createSetupPanel() {
        SetupPanel panel = new SetupPanel(this::startGame);
        panel.setOnStartSandbox(this::startSandbox);
        panel.setOnStartTitan(this::startTitanBattle);
        return panel;
    }

    public void startTitanBattle(List<Ship> initialPlayerShips) {
        TitanMode.TitanBoss boss = TitanMode.getActiveBoss();
        TitanMode.setTitanEnraged(false);
        if (gamePanel != null) screenCards.remove(gamePanel);
        gamePanel = GamePanel.createTitanBattle(initialPlayerShips, boss, this::startNewSetup);
        screenCards.add(gamePanel, SCREEN_GAME);
        showScreen(SCREEN_GAME);
        headerBar.setPhase("BOSS TITAN  ·  " + boss.shortLabel.toUpperCase());
        SoundEngine.syncMusicState(true);
    }

    public void startSandbox(List<Ship> initialPlayerShips) {
        if (gamePanel != null) screenCards.remove(gamePanel);
        gamePanel = GamePanel.createSandbox(initialPlayerShips, this::startNewSetup);
        screenCards.add(gamePanel, SCREEN_GAME);
        showScreen(SCREEN_GAME);
        headerBar.setPhase("SANDBOX  ·  TỰ DO TEST");
        SoundEngine.syncMusicState(true);
    }

    private void startGame(Board playerBoard) {
        try {
            Game game = new Game(playerBoard);
            if (gamePanel != null) screenCards.remove(gamePanel);
            gamePanel = new GamePanel(game, this::startNewSetup);
            gamePanel.setPlayerKnownShips(setupPanel.getPlacedShips());
            screenCards.add(gamePanel, SCREEN_GAME);
            showScreen(SCREEN_GAME);
            headerBar.setPhase("GIAO TRANH");
            SoundEngine.syncMusicState(true);
        } catch (UnsupportedOperationException exception) {
            setupPanel.showStatus("Chưa thể khởi tạo ván (TODO bên module logic).");
        }
    }

    private void startNewSetup() {
        TitanMode.setTitanEnraged(false);
        if (setupPanel != null) screenCards.remove(setupPanel);
        setupPanel = createSetupPanel();
        screenCards.add(setupPanel, SCREEN_SETUP);
        headerBar.setPhase("TRIỂN KHAI HẠM ĐỘI");
        showScreen(SCREEN_SETUP);
        SoundEngine.syncMusicState(false);
    }

    public void showScreen(String name) {
        screenLayout.show(screenCards, name);
    }

    private static final class HeaderBar extends JPanel {
        private final JLabel phase = new JLabel("", SwingConstants.RIGHT);
        private final AudioPill musicPill = new AudioPill(true);
        private final AudioPill sfxPill = new AudioPill(false);

        private HeaderBar() {
            super(new BorderLayout(UiTokens.SPACE_MD, 0));
            setOpaque(true);
            setBackground(UiTokens.BG_SURFACE);
            setPreferredSize(new Dimension(0, UiTokens.HEADER_HEIGHT));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, UiTokens.BORDER_SUBTLE),
                    BorderFactory.createEmptyBorder(0, UiTokens.SPACE_LG, 0, UiTokens.SPACE_LG)));

            JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTokens.SPACE_MD, UiTokens.SPACE_XS));
            brand.setOpaque(false);
            brand.add(new RadarMark());
            JLabel title = new JLabel("BattleShip");
            title.setFont(UiTokens.FONT_TITLE);
            title.setForeground(UiTokens.TEXT_PRIMARY);
            brand.add(title);
            add(brand, BorderLayout.WEST);

            JPanel right = new JPanel();
            right.setOpaque(false);
            right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
            musicPill.addActionListener(e -> {
                SoundEngine.playUiClick();
                SoundEngine.toggleMusic();
            });
            sfxPill.addActionListener(e -> SoundEngine.toggleSfx());
            phase.setFont(UiTokens.FONT_META);
            phase.setForeground(UiTokens.TEXT_MUTED);
            right.add(musicPill);
            right.add(Box.createHorizontalStrut(6));
            right.add(sfxPill);
            right.add(Box.createHorizontalStrut(14));
            right.add(phase);
            add(right, BorderLayout.EAST);

            SoundEngine.addListener(() -> {
                musicPill.sync();
                sfxPill.sync();
            });
            musicPill.sync();
            sfxPill.sync();
        }

        private void setPhase(String text) { phase.setText("HẢI ĐỒ TÁC CHIẾN  /  " + text); }
    }

    private static final class AudioPill extends JButton {
        private final boolean music;

        private AudioPill(boolean music) {
            this.music = music;
            Dimension fixed = new Dimension(music ? 146 : 82, 26);
            setPreferredSize(fixed);
            setMinimumSize(fixed);
            setMaximumSize(fixed);
            setFont(UiTokens.FONT_META);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        private void sync() {
            if (music) {
                boolean on = SoundEngine.isMusicEnabled();
                SoundEngine.BgmTrack tr = SoundEngine.getActiveTrack();
                setText(on ? "Nhạc: " + tr.shortLabel : "Nhạc: Tắt");
                setToolTipText("Bật/Tắt nhạc nền (" + tr.shortLabel + " — đổi theo Map hoặc chế độ Zombie)");
            } else {
                boolean on = SoundEngine.isSfxEnabled();
                setText(on ? "SFX: Bật" : "SFX: Tắt");
                setToolTipText("Bật/Tắt hiệu ứng SFX & âm thanh môi trường (sấm chớp, gió biển, rồng, map)");
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int arc = UiTokens.BORDER_RADIUS;
            boolean active = music ? SoundEngine.isMusicEnabled() : SoundEngine.isSfxEnabled();
            boolean hover = getModel().isRollover();
            Color accent = active ? UiTokens.ACCENT_SIGNAL : UiTokens.TEXT_MUTED;

            Color topBg = hover ? new Color(34, 49, 75, 224) : new Color(20, 32, 52, 206);
            Color botBg = hover ? new Color(24, 37, 59, 230) : new Color(14, 23, 39, 214);
            g.setPaint(new GradientPaint(0, 1, topBg, 0, h - 1, botBg));
            g.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);
            g.setColor(hover
                    ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 210)
                    : new Color(255, 255, 255, 48));
            g.setStroke(new BasicStroke(hover ? 1.3f : 1.0f));
            g.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

            // Clean vector audio equalizer / dot indicator
            g.setColor(accent);
            if (active) {
                g.fillRoundRect(8, h / 2 - 4, 2, 8, 2, 2);
                g.fillRoundRect(12, h / 2 - 6, 2, 12, 2, 2);
                g.fillRoundRect(16, h / 2 - 3, 2, 6, 2, 2);
            } else {
                g.drawLine(9, h / 2 - 3, 15, h / 2 + 3);
                g.drawLine(15, h / 2 - 3, 9, h / 2 + 3);
            }

            g.setFont(getFont());
            g.setColor(active ? UiTokens.TEXT_PRIMARY : UiTokens.TEXT_MUTED);
            FontMetrics fm = g.getFontMetrics();
            int textX = 22 + Math.max(0, (w - 26 - fm.stringWidth(getText())) / 2);
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(getText(), textX, textY);
            g.dispose();
        }
    }

    private static final class RadarMark extends JPanel {
        private RadarMark() {
            setOpaque(false);
            setPreferredSize(new Dimension(UiTokens.LOGO_SIZE, UiTokens.LOGO_SIZE));
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int inset = UiTokens.SPACE_XS;
            int diameter = Math.min(getWidth(), getHeight()) - inset * 2;
            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;
            g.setColor(UiTokens.TEXT_MUTED);
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(x, y, diameter, diameter);
            g.drawOval(x + diameter / 4, y + diameter / 4, diameter / 2, diameter / 2);
            g.drawLine(getWidth() / 2, y, getWidth() / 2, y + diameter);
            g.drawLine(x, getHeight() / 2, x + diameter, getHeight() / 2);
            g.setColor(UiTokens.ACCENT_SIGNAL);
            g.drawLine(getWidth() / 2, getHeight() / 2, x + diameter - UiTokens.SPACE_XS, y + UiTokens.SPACE_XS);
            g.fillOval(getWidth() / 2 - UiTokens.SPACE_XS / 2, getHeight() / 2 - UiTokens.SPACE_XS / 2,
                    UiTokens.SPACE_XS, UiTokens.SPACE_XS);
            g.dispose();
        }
    }
}
