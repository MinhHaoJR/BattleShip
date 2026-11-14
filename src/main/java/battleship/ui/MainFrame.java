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

/** Main application window with home lobby, setup, and battle screens managed by a CardLayout. */
public class MainFrame extends JFrame {
    public static final String SCREEN_HOME = "home";
    public static final String SCREEN_CAMPAIGN = "campaign";
    public static final String SCREEN_COOP = "coop";
    public static final String SCREEN_SETUP = "setup";
    public static final String SCREEN_GAME = "game";

    private final CardLayout screenLayout = new CardLayout();
    private final JPanel screenCards = new JPanel(screenLayout);
    private final HeaderBar headerBar = new HeaderBar();
    private final HomeScreenPanel homePanel = new HomeScreenPanel();
    private final StageSelectPanel campaignPanel = new StageSelectPanel();
    private final CoopLobbyPanel coopPanel = new CoopLobbyPanel(this::openHome, this::startCoopBattle);
    private SetupPanel setupPanel;
    private GamePanel gamePanel;

    public MainFrame() {
        super("BattleShip — Hải Đồ Tác Chiến");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(UiTokens.BG_CANVAS);
        setLayout(new BorderLayout());
        headerBar.setOnHomeClick(this::openHome);
        add(headerBar, BorderLayout.NORTH);
        screenCards.setOpaque(true);
        screenCards.setBackground(UiTokens.BG_CANVAS);
        homePanel.setOnOpenSetup(this::openSetup);
        homePanel.setOnQuickBattle(this::startQuickBattle);
        homePanel.setOnStartSandbox(() -> startSandbox(homePanel.buildQuickRandomFleet(null)));
        homePanel.setOnOpenCampaign(this::openCampaignSelect);
        homePanel.setOnOpenCoop(this::openCoopLobby);
        campaignPanel.setOnLaunchQuick(stage -> startCampaignBattle(stage, homePanel.buildQuickRandomFleet(null)));
        campaignPanel.setOnOpenSetup(this::openCampaignSetup);
        setupPanel = createSetupPanel();
        screenCards.add(homePanel, SCREEN_HOME);
        screenCards.add(campaignPanel, SCREEN_CAMPAIGN);
        screenCards.add(coopPanel, SCREEN_COOP);
        screenCards.add(setupPanel, SCREEN_SETUP);
        add(screenCards, BorderLayout.CENTER);
        showScreen(SCREEN_HOME);
        headerBar.setPhase("SẢNH CHỈ HUY");
        headerBar.setHomeButtonVisible(false);
        setMinimumSize(new Dimension(UiTokens.FRAME_MIN_WIDTH, UiTokens.FRAME_MIN_HEIGHT));
        setSize(UiTokens.FRAME_WIDTH, UiTokens.FRAME_HEIGHT);
        setLocationRelativeTo(null);
        SoundEngine.warmUpAsync();
        SpriteAssets.warmUpAsync();
        homePanel.applyChoiceQuiet(HomeScreenPanel.LobbyChoice.TITAN);
        SoundEngine.syncMusicState(false);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { SoundEngine.setEnvironmentAudioActive(true); }
            @Override public void windowDeiconified(WindowEvent e) { SoundEngine.setEnvironmentAudioActive(true); }
            @Override public void windowIconified(WindowEvent e) { SoundEngine.setEnvironmentAudioActive(false); }
            @Override public void windowClosing(WindowEvent e) {
                SoundEngine.setEnvironmentAudioActive(false);
                coopPanel.shutdownSession();
            }
            @Override public void windowClosed(WindowEvent e) {
                SoundEngine.setEnvironmentAudioActive(false);
                coopPanel.shutdownSession();
            }
        });
    }

    private SetupPanel createSetupPanel() {
        SetupPanel panel = new SetupPanel(this::startGame);
        panel.setOnStartSandbox(this::startSandbox);
        panel.setOnStartTitan(this::startTitanBattle);
        panel.setOnStartCampaign(this::startCampaignBattle);
        panel.setOnBackHome(this::openHome);
        return panel;
    }

    public HomeScreenPanel getHomePanel() { return homePanel; }

    public StageSelectPanel getCampaignPanel() { return campaignPanel; }

    public CoopLobbyPanel getCoopPanel() { return coopPanel; }

    public SetupPanel getSetupPanel() { return setupPanel; }

    public GamePanel getGamePanel() { return gamePanel; }

    public void openHome() {
        TitanMode.setTitanEnraged(false);
        OceanMapBackdrop.setTitanPhase2(false);
        coopPanel.getSession().closeNetworkOnly();
        if (gamePanel != null) {
            screenCards.remove(gamePanel);
            gamePanel = null;
        }
        if (setupPanel != null) {
            setupPanel.setPendingCampaignStage(null);
        }
        homePanel.setGuideOpen(false);
        homePanel.syncState();
        headerBar.setPhase("SẢNH CHỈ HUY");
        headerBar.configureBack("← Sảnh", "Quay về Sảnh Chỉ Huy", this::openHome, false);
        showScreen(SCREEN_HOME);
        SoundEngine.syncMusicState(false);
    }

    public void openCoopLobby() {
        TitanMode.setTitanEnraged(false);
        OceanMapBackdrop.setTitanPhase2(false);
        if (gamePanel != null) {
            screenCards.remove(gamePanel);
            gamePanel = null;
        }
        coopPanel.activateLobby(false);
        headerBar.setPhase("PHÒNG RAID CO-OP BẠN BÈ");
        headerBar.configureBack("← Sảnh", "Quay về Sảnh Chỉ Huy", this::openHome, true);
        showScreen(SCREEN_COOP);
        SoundEngine.syncMusicState(false);
    }

    public void startCoopBattle(CoopSession session, List<Ship> sharedFleet) {
        CoopSession active = session != null ? session : coopPanel.getSession();
        TitanMode.setTitanEnraged(false);
        if (gamePanel != null) screenCards.remove(gamePanel);
        gamePanel = GamePanel.createCoopTitanBattle(sharedFleet, active, this::openCoopLobby);
        screenCards.add(gamePanel, SCREEN_GAME);
        showScreen(SCREEN_GAME);
        CoopSession.CoopConfig cfg = active.getConfig();
        headerBar.setPhase("CO-OP · " + cfg.boss().shortLabel.toUpperCase());
        headerBar.configureBack("← Co-op", "Quay về Phòng Raid Co-op", this::openCoopLobby, true);
        SoundEngine.syncMusicState(true);
    }

    public void openCampaignSelect() {
        TitanMode.setTitanEnraged(false);
        OceanMapBackdrop.setTitanPhase2(false);
        if (gamePanel != null) {
            screenCards.remove(gamePanel);
            gamePanel = null;
        }
        if (setupPanel != null) {
            setupPanel.setPendingCampaignStage(null);
        }
        campaignPanel.refreshFromProgress();
        headerBar.setPhase("CHIẾN DỊCH 1–100");
        headerBar.configureBack("← Sảnh", "Quay về Sảnh Chỉ Huy", this::openHome, true);
        showScreen(SCREEN_CAMPAIGN);
        SoundEngine.syncMusicState(false);
    }

    public void openCampaignSetup(CampaignMode.CampaignStage stage) {
        CampaignMode.CampaignStage target = stage != null ? stage : CampaignMode.stage(1);
        TitanMode.setTitanEnraged(false);
        OceanMapBackdrop.setTitanPhase2(false);
        if (gamePanel != null) {
            screenCards.remove(gamePanel);
            gamePanel = null;
        }
        if (setupPanel != null) screenCards.remove(setupPanel);
        setupPanel = createSetupPanel();
        setupPanel.setPendingCampaignStage(target);
        screenCards.add(setupPanel, SCREEN_SETUP);
        headerBar.setPhase("XẾP TÀU · MÀN " + target.stageNumber());
        headerBar.configureBack("← Bản Đồ", "Quay lại Bản Đồ Chiến Dịch 1–100", this::openCampaignSelect, true);
        showScreen(SCREEN_SETUP);
        SoundEngine.syncMusicState(false);
    }

    public void startCampaignBattle(CampaignMode.CampaignStage stage, List<Ship> fleet) {
        CampaignMode.CampaignStage active = stage != null ? stage : CampaignMode.stage(1);
        List<Ship> safeFleet = (fleet != null && !fleet.isEmpty()) ? fleet : homePanel.buildQuickRandomFleet(null);
        TitanMode.setTitanEnraged(false);
        OceanMapBackdrop.setTitanPhase2(false);
        if (gamePanel != null) {
            screenCards.remove(gamePanel);
            gamePanel = null;
        }
        campaignPanel.selectStage(active.stageNumber());
        gamePanel = GamePanel.createCampaignBattle(
                safeFleet,
                active,
                nextStage -> startCampaignBattle(nextStage, homePanel.buildQuickRandomFleet(null)),
                this::openCampaignSelect);
        screenCards.add(gamePanel, SCREEN_GAME);
        showScreen(SCREEN_GAME);
        headerBar.setPhase("MÀN " + active.stageNumber() + "/100  ·  " + active.ruleType().shortLabel.toUpperCase());
        headerBar.configureBack("← Bản Đồ", "Quay lại Bản Đồ Chiến Dịch 1–100", this::openCampaignSelect, true);
        SoundEngine.syncMusicState(true);
    }

    public void openSetup() {
        startNewSetup();
    }

    private void startQuickBattle(HomeScreenPanel.LobbyChoice choice) {
        if (choice == HomeScreenPanel.LobbyChoice.CAMPAIGN) {
            CampaignMode.CampaignStage next = CampaignMode.stage(CampaignMode.getHighestUnlockedStage());
            startCampaignBattle(next, homePanel.buildQuickRandomFleet(null));
            return;
        }
        if (choice == HomeScreenPanel.LobbyChoice.COOP) {
            CoopSession quickCoop = coopPanel.getSession();
            quickCoop.configureOfflineMode(CoopSession.CoopTransport.AI_ALLY);
            quickCoop.updateHostConfig(new CoopSession.CoopConfig(
                    TitanMode.getActiveBoss(),
                    TitanMode.getDifficulty(),
                    OceanMapBackdrop.getMapTheme(),
                    OceanMapBackdrop.getSeason(),
                    OceanMapBackdrop.getWeatherMode(),
                    "Đô Đốc P1",
                    "Vega AI",
                    CoopSession.DEFAULT_COOP_HP_PERCENT
            ));
            List<Ship> fleet = quickCoop.launchMatchAsHost(homePanel.buildQuickRandomFleet(null));
            startCoopBattle(quickCoop, fleet);
            return;
        }
        if (choice == HomeScreenPanel.LobbyChoice.SANDBOX) {
            startSandbox(homePanel.buildQuickRandomFleet(null));
            return;
        }
        if (choice == HomeScreenPanel.LobbyChoice.TITAN) {
            List<Ship> fleet = homePanel.buildQuickRandomFleet(null);
            startTitanBattle(fleet);
            return;
        }
        Board board = new Board();
        List<Ship> fleet = homePanel.buildQuickRandomFleet(board);
        try {
            Game game = new Game(board);
            if (gamePanel != null) screenCards.remove(gamePanel);
            gamePanel = new GamePanel(game, this::startNewSetup);
            gamePanel.setPlayerKnownShips(fleet);
            screenCards.add(gamePanel, SCREEN_GAME);
            showScreen(SCREEN_GAME);
            headerBar.setPhase(choice == HomeScreenPanel.LobbyChoice.ZOMBIE ? "ĐẠI CHIẾN ZOMBIE" : "GIAO TRANH");
            headerBar.configureBack("← Sảnh", "Quay về Sảnh Chỉ Huy", this::openHome, true);
            SoundEngine.syncMusicState(true);
        } catch (UnsupportedOperationException exception) {
            openSetup();
            setupPanel.showStatus("Chưa thể khởi tạo ván (TODO bên module logic).");
        }
    }

    public void startTitanBattle(List<Ship> initialPlayerShips) {
        TitanMode.TitanBoss boss = TitanMode.getActiveBoss();
        TitanMode.setTitanEnraged(false);
        if (gamePanel != null) screenCards.remove(gamePanel);
        gamePanel = GamePanel.createTitanBattle(initialPlayerShips, boss, this::startNewSetup);
        screenCards.add(gamePanel, SCREEN_GAME);
        showScreen(SCREEN_GAME);
        headerBar.setPhase("BOSS TITAN  ·  " + boss.shortLabel.toUpperCase());
        headerBar.configureBack("← Sảnh", "Quay về Sảnh Chỉ Huy", this::openHome, true);
        SoundEngine.syncMusicState(true);
    }

    public void startSandbox(List<Ship> initialPlayerShips) {
        if (gamePanel != null) screenCards.remove(gamePanel);
        gamePanel = GamePanel.createSandbox(initialPlayerShips, this::startNewSetup);
        screenCards.add(gamePanel, SCREEN_GAME);
        showScreen(SCREEN_GAME);
        headerBar.setPhase("SANDBOX  ·  TỰ DO TEST");
        headerBar.configureBack("← Sảnh", "Quay về Sảnh Chỉ Huy", this::openHome, true);
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
            headerBar.setPhase(ZombieMode.isZombieMode() ? "ĐẠI CHIẾN ZOMBIE" : "GIAO TRANH");
            headerBar.configureBack("← Sảnh", "Quay về Sảnh Chỉ Huy", this::openHome, true);
            SoundEngine.syncMusicState(true);
        } catch (UnsupportedOperationException exception) {
            setupPanel.showStatus("Chưa thể khởi tạo ván (TODO bên module logic).");
        }
    }

    private void startNewSetup() {
        TitanMode.setTitanEnraged(false);
        OceanMapBackdrop.setTitanPhase2(false);
        if (gamePanel != null) {
            screenCards.remove(gamePanel);
            gamePanel = null;
        }
        if (setupPanel != null) screenCards.remove(setupPanel);
        setupPanel = createSetupPanel();
        setupPanel.setPendingCampaignStage(null);
        screenCards.add(setupPanel, SCREEN_SETUP);
        headerBar.setPhase("TRIỂN KHAI HẠM ĐỘI");
        headerBar.configureBack("← Sảnh", "Quay về Sảnh Chỉ Huy", this::openHome, true);
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
        private final TacticalButton homeBtn = new TacticalButton("← Sảnh", false);
        private Runnable onHomeClick;

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
            Dimension homeDim = new Dimension(92, 26);
            homeBtn.setPreferredSize(homeDim);
            homeBtn.setMinimumSize(homeDim);
            homeBtn.setMaximumSize(homeDim);
            homeBtn.setToolTipText("Quay về Sảnh Chỉ Huy");
            homeBtn.addActionListener(e -> {
                SoundEngine.playUiClick();
                if (onHomeClick != null) onHomeClick.run();
            });
            homeBtn.setVisible(false);
            musicPill.addActionListener(e -> {
                SoundEngine.playUiClick();
                SoundEngine.toggleMusic();
            });
            sfxPill.addActionListener(e -> SoundEngine.toggleSfx());
            phase.setFont(UiTokens.FONT_META);
            phase.setForeground(UiTokens.TEXT_MUTED);
            right.add(homeBtn);
            right.add(Box.createHorizontalStrut(8));
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

        private void setOnHomeClick(Runnable r) { this.onHomeClick = r; }

        private void setHomeButtonVisible(boolean visible) {
            homeBtn.setVisible(visible);
            revalidate();
            repaint();
        }

        private void configureBack(String label, String tooltip, Runnable action, boolean visible) {
            homeBtn.setText(label);
            homeBtn.setToolTipText(tooltip);
            String safeLabel = label != null ? label : "";
            int textW = homeBtn.getFontMetrics(homeBtn.getFont()).stringWidth(safeLabel);
            Dimension dim = new Dimension(Math.max(92, textW + 30), 26);
            homeBtn.setPreferredSize(dim);
            homeBtn.setMinimumSize(dim);
            homeBtn.setMaximumSize(dim);
            this.onHomeClick = action;
            homeBtn.setVisible(visible);
            revalidate();
            repaint();
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
