package battleship.ui;

import battleship.model.Board;
import battleship.model.Ship;
import battleship.model.ShipType;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Animated Title & Command Lobby ("Sảnh Chỉ Huy") shown on game startup.
 * Features live OceanMapBackdrop rendering, Blender 3D mode crests, dynamic 3D Kaiju/Fleet
 * showcase synchronized with the selected mode/boss/stage, tactical arena preview, and 1-click launch.
 */
public final class HomeScreenPanel extends JPanel {
    public enum LobbyChoice {
        CAMPAIGN(
                "CỐT TRUYỆN 1–100",
                "Chế độ Story 5 Chương kèm tranh hoạt hình, Mini-game, Zombie & 20 ải Boss Titan 3★.",
                "STORY · 100 MÀN",
                "home_badge_campaign",
                new Color(0xFBBF24)
        ),
        COOP(
                "CO-OP BẠN BÈ",
                "Cùng bạn bè đánh Boss Titan (LAN · 2P Chung Máy · AI) & Song Long Hợp Kích.",
                "MỚI · 2P LAN",
                "home_badge_coop",
                new Color(0x34D399)
        ),
        TITAN(
                "VS BOSS TITAN",
                "Đột kích Kaiju 3D (Godzilla · Ghidorah · Mothra), Ngắt Chiêu & Arena V2.",
                "ARENA V2",
                "home_badge_titan",
                new Color(0xF97316)
        ),
        ZOMBIE(
                "ĐẠI CHIẾN ZOMBIE",
                "Đối đầu 5 chiến hạm Xác Sống sở hữu kỹ năng axit, bào tử & thiên thạch.",
                "5 BOSS HẠM",
                "home_badge_zombie",
                new Color(0x22C55E)
        ),
        CLASSIC(
                "HẢI CHIẾN CỔ ĐIỂN",
                "Bàn cờ 10×10 chiến thuật kết hợp 4 Vũ Khí & 4 Phép Nguyên Tố.",
                "10×10",
                "home_badge_classic",
                new Color(0x38BDF8)
        ),
        SANDBOX(
                "SANDBOX TỰ DO",
                "Thử mọi kỹ năng Boss, Siêu Tàu Hyperion, Khiên 3D & 5 bản đồ động.",
                "HẢI NĂNG ∞",
                "home_badge_sandbox",
                new Color(0xA855F7)
        );

        public final String title;
        public final String description;
        public final String badge;
        public final String spriteKey;
        public final Color accent;

        LobbyChoice(String title, String description, String badge, String spriteKey, Color accent) {
            this.title = title;
            this.description = description;
            this.badge = badge;
            this.spriteKey = spriteKey;
            this.accent = accent;
        }
    }

    private final JLayeredPane layeredStage;
    private final JPanel mainContent = new JPanel(new BorderLayout(0, 12));
    private final GuideModalOverlay guideOverlay = new GuideModalOverlay();

    private final Map<LobbyChoice, ModeCard> modeCards = new EnumMap<>(LobbyChoice.class);
    private final Map<OceanMapBackdrop.MapTheme, MapChipButton> mapChips = new EnumMap<>(OceanMapBackdrop.MapTheme.class);
    private final CardLayout configLayout = new CardLayout();
    private final JPanel configCardPanel = new JPanel(configLayout);

    private final TitanMode.TitanPill titanPill = new TitanMode.TitanPill(true);
    private final TitanMode.DifficultyPill diffPill = new TitanMode.DifficultyPill();
    private final TitanMode.RushPill rushPill = new TitanMode.RushPill();
    private final OceanMapBackdrop.AtmospherePill seasonPill =
            new OceanMapBackdrop.AtmospherePill(OceanMapBackdrop.AtmospherePill.Kind.SEASON);
    private final OceanMapBackdrop.AtmospherePill weatherPill =
            new OceanMapBackdrop.AtmospherePill(OceanMapBackdrop.AtmospherePill.Kind.WEATHER);
    private final OceanMapBackdrop.AtmospherePill mapPill =
            new OceanMapBackdrop.AtmospherePill(OceanMapBackdrop.AtmospherePill.Kind.MAP);

    private final TacticalButton quickLaunchButton = new TacticalButton("Vào Trận Nhanh (Tự Xếp Tàu)", true);
    private final TacticalButton setupButton = new TacticalButton("Triển Khai Hạm Đội (Tự Xếp Tàu)", false);
    private final ShowcaseStageCanvas showcaseCanvas = new ShowcaseStageCanvas();
    private final HeroEmblemCanvas emblemCanvas = new HeroEmblemCanvas();
    private final JLabel activeStageTitle = new JLabel();
    private final WrappedTextLabel activeStageDesc = new WrappedTextLabel(new Font(Font.SANS_SERIF, Font.PLAIN, 12), UiTokens.TEXT_PRIMARY);
    private final JLabel campaignProgressLabel = new JLabel();

    private LobbyChoice selectedChoice = LobbyChoice.TITAN;
    private Timer animTimer;
    private final Random rng = new Random();

    private Runnable onOpenSetup;
    private Consumer<LobbyChoice> onQuickBattle;
    private Runnable onStartSandbox;
    private Runnable onOpenCampaign;
    private Runnable onOpenCoop;

    public HomeScreenPanel() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(UiTokens.BG_CANVAS);

        selectedChoice = choiceFromGameMode(ZombieMode.getGameMode());
        OceanMapBackdrop.setTitanPhase2(false);

        layeredStage = new JLayeredPane() {
            @Override public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                mainContent.setBounds(0, 0, w, h);
                guideOverlay.setBounds(0, 0, w, h);
            }

            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                double t = OceanMapBackdrop.elapsedSeconds();
                OceanMapBackdrop.paintOceanMap(g, getWidth(), getHeight(), t, false);
                OceanMapBackdrop.paintAtmosphericWeather(g, getWidth(), getHeight(), t);
                OceanMapBackdrop.paintPatrollingDragons(g, getWidth(), getHeight(), t);

                // Left-side subtle dark gradient scrim for crisp legibility over animated waves
                int w = getWidth();
                int h = getHeight();
                g.setPaint(new GradientPaint(
                        0, 0, new Color(5, 11, 22, 195),
                        Math.max(1, (int) (w * 0.62)), 0, new Color(5, 11, 22, 58)));
                g.fillRect(0, 0, w, h);
                g.dispose();
            }
        };
        layeredStage.setOpaque(true);

        mainContent.setOpaque(false);
        mainContent.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        mainContent.add(buildHeroHeader(), BorderLayout.NORTH);
        mainContent.add(buildCenterSplit(), BorderLayout.CENTER);

        guideOverlay.setVisible(false);
        layeredStage.add(mainContent, JLayeredPane.DEFAULT_LAYER);
        layeredStage.add(guideOverlay, JLayeredPane.MODAL_LAYER);
        add(layeredStage, BorderLayout.CENTER);

        installEscShortcut();
        animTimer = new Timer(42, e -> {
            if (isShowing()) {
                layeredStage.repaint();
            }
        });
        syncState();
    }

    @Override public void addNotify() {
        super.addNotify();
        if (animTimer != null && !animTimer.isRunning()) animTimer.start();
    }

    @Override public void removeNotify() {
        if (animTimer != null) animTimer.stop();
        super.removeNotify();
    }

    public LobbyChoice getSelectedChoice() { return selectedChoice; }

    public void setOnOpenSetup(Runnable onOpenSetup) { this.onOpenSetup = onOpenSetup; }

    public void setOnQuickBattle(Consumer<LobbyChoice> onQuickBattle) { this.onQuickBattle = onQuickBattle; }

    public void setOnStartSandbox(Runnable onStartSandbox) { this.onStartSandbox = onStartSandbox; }

    public void setOnOpenCampaign(Runnable onOpenCampaign) { this.onOpenCampaign = onOpenCampaign; }

    public void setOnOpenCoop(Runnable onOpenCoop) { this.onOpenCoop = onOpenCoop; }

    public boolean isGuideOpen() { return guideOverlay.isVisible(); }

    public void setGuideOpen(boolean visible) {
        guideOverlay.setVisible(visible);
        guideOverlay.repaint();
    }

    /** Builds a random non-overlapping 5-ship fleet and returns matching Board + List<Ship>. */
    public List<Ship> buildQuickRandomFleet(Board targetBoard) {
        Board board = targetBoard != null ? targetBoard : new Board();
        board.clear();
        List<Ship> fleet = new ArrayList<>();
        for (ShipType type : ShipType.values()) {
            for (int guard = 0; guard < 400; guard++) {
                int r = rng.nextInt(Board.SIZE);
                int c = rng.nextInt(Board.SIZE);
                boolean horiz = rng.nextBoolean();
                if (board.placeShip(type, r, c, horiz)) {
                    fleet.add(new Ship(type, r, c, horiz));
                    break;
                }
            }
        }
        return fleet;
    }

    /** Refreshes mode cards, pills, and Arena V2 stage preview to match global state. */
    public void syncState() {
        for (Map.Entry<LobbyChoice, ModeCard> entry : modeCards.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == selectedChoice);
        }
        OceanMapBackdrop.MapTheme activeTheme = OceanMapBackdrop.getMapTheme();
        for (Map.Entry<OceanMapBackdrop.MapTheme, MapChipButton> entry : mapChips.entrySet()) {
            entry.getValue().setSelectedTheme(entry.getKey() == activeTheme);
        }
        titanPill.sync();
        diffPill.sync();
        rushPill.sync();
        seasonPill.sync();
        weatherPill.sync();
        mapPill.sync();

        configLayout.show(configCardPanel,
                selectedChoice == LobbyChoice.TITAN ? "TITAN"
                        : (selectedChoice == LobbyChoice.CAMPAIGN ? "CAMPAIGN"
                        : (selectedChoice == LobbyChoice.COOP ? "COOP" : "ENV")));

        campaignProgressLabel.setText("★ " + CampaignMode.totalStars() + "/" + CampaignMode.MAX_TOTAL_STARS
                + " · Vượt " + CampaignMode.clearedCount() + "/" + CampaignMode.TOTAL_STAGES + " ải · Cốt Truyện 5 Chương");

        if (selectedChoice == LobbyChoice.CAMPAIGN) {
            int nextStage = CampaignMode.getHighestUnlockedStage();
            quickLaunchButton.setText("Mở Bản Đồ Story 1–100");
            quickLaunchButton.setToolTipText("Mở chế độ Cốt Truyện 5 Chương × 20 ải kèm tranh hoạt hình (1–100 màn)");
            setupButton.setText("Vào Nhanh Màn " + nextStage + " (Tự Xếp Tàu)");
            setupButton.setToolTipText("Vào thẳng Màn " + nextStage + " với hạm đội tự động");
        } else if (selectedChoice == LobbyChoice.COOP) {
            quickLaunchButton.setText("Mở Phòng Co-op Bạn Bè (LAN / 2P)");
            quickLaunchButton.setToolTipText("Tạo phòng LAN IP:Port, ghép bạn bè, 2P Chung Máy hoặc Đồng Minh AI");
            setupButton.setText("Vào Nhanh Co-op + Đồng Minh AI");
            setupButton.setToolTipText("Chiến đấu ngay cùng Phó Đô Đốc Vega AI & Song Long Hợp Kích");
        } else if (selectedChoice == LobbyChoice.SANDBOX) {
            quickLaunchButton.setText("Mở Sandbox (Test Tất Cả)");
            quickLaunchButton.setToolTipText("Mở Sandbox test ngay 4 vũ khí, 4 phép thuật, Siêu Tàu, Khiên & Boss");
            setupButton.setText("Triển Khai Hạm Đội (Tự Xếp Tàu)");
            setupButton.setToolTipText(null);
        } else if (selectedChoice == LobbyChoice.TITAN && TitanMode.isBossRushEnabled()) {
            quickLaunchButton.setText("Vào Boss Rush · " + TitanMode.getDifficulty().label);
            quickLaunchButton.setToolTipText("Tự xếp 5 tàu và khai chiến ngay chuỗi 3 Boss Titan + Chọn Di Vật!");
            setupButton.setText("Triển Khai Hạm Đội (Tự Xếp Tàu)");
            setupButton.setToolTipText(null);
        } else if (selectedChoice == LobbyChoice.TITAN) {
            quickLaunchButton.setText("Đấu Ngay · " + TitanMode.getActiveBoss().shortLabel);
            quickLaunchButton.setToolTipText("Tự xếp 5 tàu và vào ngay đấu trường " + activeTheme.displayName);
            setupButton.setText("Triển Khai Hạm Đội (Tự Xếp Tàu)");
            setupButton.setToolTipText(null);
        } else {
            quickLaunchButton.setText("Vào Trận Nhanh (Tự Xếp Tàu)");
            quickLaunchButton.setToolTipText("Tự động triển khai 5 chiến hạm và vào trận ngay");
            setupButton.setText("Triển Khai Hạm Đội (Tự Xếp Tàu)");
            setupButton.setToolTipText(null);
        }

        activeStageTitle.setText("ARENA V2 · " + activeTheme.displayName.toUpperCase());
        activeStageTitle.setForeground(activeTheme.accent);
        activeStageDesc.setText(TitanMode.stageMechanicsSummary(activeTheme));
        layeredStage.repaint();
    }

    public void applyChoiceQuiet(LobbyChoice choice) {
        selectedChoice = choice != null ? choice : LobbyChoice.TITAN;
        switch (selectedChoice) {
            case TITAN -> ZombieMode.setGameMode(ZombieMode.GameMode.TITAN);
            case ZOMBIE -> ZombieMode.setGameMode(ZombieMode.GameMode.ZOMBIE);
            case CLASSIC -> ZombieMode.setGameMode(ZombieMode.GameMode.CLASSIC);
            case CAMPAIGN, COOP, SANDBOX -> { }
        }
        syncState();
    }

    private static LobbyChoice choiceFromGameMode(ZombieMode.GameMode mode) {
        if (mode == null) return LobbyChoice.TITAN;
        return switch (mode) {
            case TITAN -> LobbyChoice.TITAN;
            case ZOMBIE -> LobbyChoice.ZOMBIE;
            case CLASSIC -> LobbyChoice.CLASSIC;
        };
    }

    private void selectLobbyChoice(LobbyChoice choice) {
        selectedChoice = choice != null ? choice : LobbyChoice.TITAN;
        switch (selectedChoice) {
            case TITAN -> ZombieMode.setGameMode(ZombieMode.GameMode.TITAN);
            case ZOMBIE -> ZombieMode.setGameMode(ZombieMode.GameMode.ZOMBIE);
            case CLASSIC -> ZombieMode.setGameMode(ZombieMode.GameMode.CLASSIC);
            case CAMPAIGN, COOP, SANDBOX -> { /* keeps current global mode until stage launch */ }
        }
        SoundEngine.playUiClick();
        SoundEngine.refreshThemeMusic();
        syncState();
    }

    private JPanel buildHeroHeader() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setOpaque(false);

        JPanel brandLeft = new JPanel(new BorderLayout(12, 0));
        brandLeft.setOpaque(false);
        emblemCanvas.setPreferredSize(new Dimension(70, 70));
        brandLeft.add(emblemCanvas, BorderLayout.WEST);

        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));

        JLabel mainTitle = new JLabel("BATTLESHIP");
        mainTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 31));
        mainTitle.setForeground(UiTokens.TEXT_PRIMARY);

        JLabel subTitle = new JLabel("HẢI ĐỒ TÁC CHIẾN  ·  KAIJU RAID ARENA V2");
        subTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        subTitle.setForeground(new Color(0x38BDF8));

        JLabel tagline = new JLabel("Siêu Tàu Hyperion · 3 Kaiju · 5 Đấu Trường V2");
        tagline.setFont(UiTokens.FONT_META);
        tagline.setForeground(UiTokens.TEXT_MUTED);

        titleStack.add(Box.createVerticalStrut(4));
        titleStack.add(mainTitle);
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(subTitle);
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(tagline);
        brandLeft.add(titleStack, BorderLayout.CENTER);
        bar.add(brandLeft, BorderLayout.WEST);

        JPanel rightControls = new JPanel();
        rightControls.setOpaque(false);
        rightControls.setLayout(new BoxLayout(rightControls, BoxLayout.Y_AXIS));

        JPanel arenaChipsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        arenaChipsRow.setOpaque(false);
        for (OceanMapBackdrop.MapTheme theme : OceanMapBackdrop.MapTheme.values()) {
            MapChipButton chip = new MapChipButton(theme);
            chip.addActionListener(e -> {
                OceanMapBackdrop.setMapTheme(theme);
                SoundEngine.playUiClick();
                SoundEngine.refreshThemeMusic();
                SoundEngine.playMapPropCue(theme);
                syncState();
            });
            mapChips.put(theme, chip);
            arenaChipsRow.add(chip);
        }

        TacticalButton guideBtn = new TacticalButton("Cẩm Nang", false);
        guideBtn.setPreferredSize(new Dimension(92, 28));
        guideBtn.addActionListener(e -> {
            SoundEngine.playUiClick();
            setGuideOpen(true);
        });
        arenaChipsRow.add(Box.createHorizontalStrut(4));
        arenaChipsRow.add(guideBtn);

        rightControls.add(Box.createVerticalStrut(12));
        rightControls.add(arenaChipsRow);
        bar.add(rightControls, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildCenterSplit() {
        JPanel split = new JPanel(new GridBagLayout());
        split.setOpaque(false);

        JPanel leftCol = buildLeftModeColumn();
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0;
        lc.gridy = 0;
        lc.weightx = 0.0;
        lc.weighty = 1.0;
        lc.fill = GridBagConstraints.BOTH;
        lc.insets = new Insets(0, 0, 0, 18);
        split.add(leftCol, lc);

        JPanel rightCol = buildRightShowcaseColumn();
        GridBagConstraints rc = new GridBagConstraints();
        rc.gridx = 1;
        rc.gridy = 0;
        rc.weightx = 1.0;
        rc.weighty = 1.0;
        rc.fill = GridBagConstraints.BOTH;
        split.add(rightCol, rc);

        return split;
    }

    private JPanel buildLeftModeColumn() {
        JPanel col = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(10, 19, 34, 224));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(56, 189, 248, 72));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                g2.dispose();
            }
        };
        col.setOpaque(false);
        col.setLayout(new BorderLayout(0, 6));
        col.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        col.setPreferredSize(new Dimension(414, 0));
        col.setMinimumSize(new Dimension(396, 0));

        JLabel header = new JLabel("CHỌN CHẾ ĐỘ CHIẾN ĐẤU");
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        header.setForeground(UiTokens.TEXT_PRIMARY);
        col.add(header, BorderLayout.NORTH);

        JPanel cardsList = new JPanel(new GridLayout(6, 1, 0, 4));
        cardsList.setOpaque(false);
        for (LobbyChoice choice : LobbyChoice.values()) {
            ModeCard card = new ModeCard(choice);
            card.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    selectLobbyChoice(choice);
                }
            });
            modeCards.put(choice, card);
            cardsList.add(card);
        }
        col.add(cardsList, BorderLayout.CENTER);

        JPanel bottomControls = new JPanel();
        bottomControls.setOpaque(false);
        bottomControls.setLayout(new BoxLayout(bottomControls, BoxLayout.Y_AXIS));

        // Contextual option bar (Titan Pills OR Env/Map Pills)
        configCardPanel.setOpaque(false);
        configCardPanel.setAlignmentX(LEFT_ALIGNMENT);
        configCardPanel.setPreferredSize(new Dimension(386, 36));
        configCardPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JPanel titanCfgRow = new JPanel(new BorderLayout(6, 0));
        titanCfgRow.setOpaque(false);
        titanPill.sync();
        titanPill.addActionListener(e -> {
            TitanMode.TitanBoss boss = TitanMode.cycleActiveBoss();
            SoundEngine.playUiClick();
            SoundEngine.refreshThemeMusic();
            SoundEngine.playTitanRoar(boss);
            syncState();
        });
        diffPill.sync();
        diffPill.addActionListener(e -> {
            TitanMode.cycleDifficulty();
            SoundEngine.playUiClick();
            syncState();
        });
        rushPill.sync();
        rushPill.addActionListener(e -> {
            boolean rush = TitanMode.toggleBossRush();
            if (rush) TitanMode.setActiveBoss(TitanMode.TitanBoss.GODZILLA);
            SoundEngine.playUiClick();
            syncState();
        });
        JPanel titanEast = new JPanel(new GridLayout(1, 2, 6, 0));
        titanEast.setOpaque(false);
        titanEast.add(diffPill);
        titanEast.add(rushPill);
        titanCfgRow.add(titanPill, BorderLayout.CENTER);
        titanCfgRow.add(titanEast, BorderLayout.EAST);

        JPanel envCfgRow = new JPanel(new GridLayout(1, 3, 6, 0));
        envCfgRow.setOpaque(false);
        seasonPill.sync();
        seasonPill.addActionListener(e -> {
            OceanMapBackdrop.Season s = OceanMapBackdrop.cycleSeason();
            SoundEngine.playUiClick();
            SoundEngine.playSeasonTransitionCue(s);
            syncState();
        });
        weatherPill.sync();
        weatherPill.addActionListener(e -> {
            OceanMapBackdrop.WeatherMode w = OceanMapBackdrop.cycleWeatherMode();
            SoundEngine.playUiClick();
            SoundEngine.playWeatherTransitionCue(w, OceanMapBackdrop.sampleStormLevel(OceanMapBackdrop.elapsedSeconds()));
            syncState();
        });
        mapPill.sync();
        mapPill.addActionListener(e -> {
            OceanMapBackdrop.MapTheme mt = OceanMapBackdrop.cycleMapTheme();
            SoundEngine.playUiClick();
            SoundEngine.refreshThemeMusic();
            SoundEngine.playMapPropCue(mt);
            syncState();
        });
        envCfgRow.add(mapPill);
        envCfgRow.add(seasonPill);
        envCfgRow.add(weatherPill);

        JPanel campaignCfgRow = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(22, 34, 56, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(251, 191, 36, 130));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        campaignCfgRow.setOpaque(false);
        campaignCfgRow.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        campaignProgressLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        campaignProgressLabel.setForeground(new Color(0xFCD34D));
        campaignProgressLabel.setHorizontalAlignment(SwingConstants.CENTER);
        campaignCfgRow.add(campaignProgressLabel, BorderLayout.CENTER);

        JPanel coopCfgRow = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8, 28, 30, 210));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(52, 211, 153, 140));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        coopCfgRow.setOpaque(false);
        coopCfgRow.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        JLabel coopCfgLbl = new JLabel("LAN IP:Port · 2P Chung Máy · Đồng Minh AI · Song Long Hợp Kích", SwingConstants.CENTER);
        coopCfgLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        coopCfgLbl.setForeground(new Color(0x6EE7B7));
        coopCfgRow.add(coopCfgLbl, BorderLayout.CENTER);

        configCardPanel.add(titanCfgRow, "TITAN");
        configCardPanel.add(envCfgRow, "ENV");
        configCardPanel.add(campaignCfgRow, "CAMPAIGN");
        configCardPanel.add(coopCfgRow, "COOP");
        bottomControls.add(configCardPanel);
        bottomControls.add(Box.createVerticalStrut(6));

        quickLaunchButton.setAlignmentX(LEFT_ALIGNMENT);
        quickLaunchButton.setPreferredSize(new Dimension(386, 38));
        quickLaunchButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        quickLaunchButton.addActionListener(e -> {
            SoundEngine.playUiClick();
            if (selectedChoice == LobbyChoice.CAMPAIGN) {
                if (onOpenCampaign != null) onOpenCampaign.run();
            } else if (selectedChoice == LobbyChoice.COOP) {
                if (onOpenCoop != null) onOpenCoop.run();
            } else if (selectedChoice == LobbyChoice.SANDBOX) {
                if (onStartSandbox != null) onStartSandbox.run();
            } else if (onQuickBattle != null) {
                onQuickBattle.accept(selectedChoice);
            }
        });
        bottomControls.add(quickLaunchButton);
        bottomControls.add(Box.createVerticalStrut(5));

        setupButton.setAlignmentX(LEFT_ALIGNMENT);
        setupButton.setPreferredSize(new Dimension(386, 32));
        setupButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        setupButton.addActionListener(e -> {
            SoundEngine.playUiClick();
            if ((selectedChoice == LobbyChoice.CAMPAIGN || selectedChoice == LobbyChoice.COOP) && onQuickBattle != null) {
                onQuickBattle.accept(selectedChoice);
            } else if (onOpenSetup != null) {
                onOpenSetup.run();
            }
        });
        bottomControls.add(setupButton);

        col.add(bottomControls, BorderLayout.SOUTH);
        return col;
    }

    private JPanel buildRightShowcaseColumn() {
        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setOpaque(false);

        right.add(showcaseCanvas, BorderLayout.CENTER);
        right.add(buildArenaPreviewCard(), BorderLayout.SOUTH);
        return right;
    }

    private JPanel buildArenaPreviewCard() {
        JPanel card = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                OceanMapBackdrop.MapTheme mt = OceanMapBackdrop.getMapTheme();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(10, 19, 34, 224));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(mt.accent.getRed(), mt.accent.getGreen(), mt.accent.getBlue(), 145));
                g2.setStroke(new BasicStroke(1.3f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(0, 114));
        card.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));

        JPanel textCol = new JPanel(new BorderLayout(0, 6));
        textCol.setOpaque(false);
        activeStageTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        textCol.add(activeStageTitle, BorderLayout.NORTH);
        textCol.add(activeStageDesc, BorderLayout.CENTER);
        card.add(textCol, BorderLayout.CENTER);

        JComponent propsPreview = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(176, 90); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                OceanMapBackdrop.MapTheme theme = OceanMapBackdrop.getMapTheme();
                String nodeKey = switch (theme) {
                    case NAVAL_CITADEL -> "sonar_buoy";
                    case APOCALYPSE -> "lava_fissure";
                    case ABYSSAL_ATLANTIS -> "atlantis_pylon";
                    case FANTASY_REALM -> "arcane_leynode";
                    case GALAXY_COSMOS -> "gravity_node";
                };
                String[] keys = {"cover_reef_intact", nodeKey, "beacon_mana"};
                String[] labels = {"Đá Chắn", "Nút Map", "Tiếp Tế"};
                int cellW = 54;
                int gap = 6;
                for (int i = 0; i < 3; i++) {
                    int x = i * (cellW + gap);
                    g2.setColor(new Color(16, 29, 50, 210));
                    g2.fillRoundRect(x, 2, cellW, 84, 10, 10);
                    g2.setColor(new Color(theme.accent.getRed(), theme.accent.getGreen(), theme.accent.getBlue(), 100));
                    g2.drawRoundRect(x, 2, cellW, 84, 10, 10);
                    BufferedImage img = SpriteAssets.getMapProp(keys[i]);
                    if (img != null) {
                        SpriteAssets.drawSharp(g2, img, x + 7, 8, 40, 40);
                    }
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                    g2.setColor(UiTokens.TEXT_MUTED);
                    FontMetrics fm = g2.getFontMetrics();
                    int tw = fm.stringWidth(labels[i]);
                    g2.drawString(labels[i], x + (cellW - tw) / 2, 74);
                }
                g2.dispose();
            }
        };
        card.add(propsPreview, BorderLayout.EAST);
        return card;
    }

    private void installEscShortcut() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "closeHomeGuide");
        getActionMap().put("closeHomeGuide", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isGuideOpen()) setGuideOpen(false);
            }
        });
    }

    private final class HeroEmblemCanvas extends JComponent {
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double t = OceanMapBackdrop.elapsedSeconds();
            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;
            int cy = h / 2;
            int rad = Math.min(w, h) / 2 - 4;

            g2.setPaint(new RadialGradientPaint(cx, cy, Math.max(8, rad),
                    new float[]{0f, 1f},
                    new Color[]{new Color(56, 189, 248, 86), new Color(10, 19, 34, 0)}));
            g2.fillOval(cx - rad, cy - rad, rad * 2, rad * 2);

            g2.setColor(new Color(56, 189, 248, 125));
            g2.setStroke(new BasicStroke(1.3f));
            g2.drawOval(cx - rad + 2, cy - rad + 2, rad * 2 - 4, rad * 2 - 4);
            int pulse = (int) (Math.sin(t * 2.4) * 3);
            g2.setColor(new Color(250, 204, 21, 95));
            g2.drawArc(cx - rad + 4, cy - rad + 4, rad * 2 - 8, rad * 2 - 8,
                    (int) ((t * 48) % 360), 110);

            BufferedImage emblem = SpriteAssets.getMapProp("home_emblem");
            if (emblem != null) {
                int sz = Math.min(w, h) - 8;
                SpriteAssets.drawSharp(g2, emblem, (w - sz) / 2, (h - sz) / 2 + pulse / 2, sz, sz);
            }
            g2.dispose();
        }
    }

    private static final class MapChipButton extends JButton {
        private final OceanMapBackdrop.MapTheme theme;
        private boolean selectedTheme;

        private MapChipButton(OceanMapBackdrop.MapTheme theme) {
            this.theme = theme;
            String label = switch (theme) {
                case NAVAL_CITADEL -> "Hải Vực";
                case FANTASY_REALM -> "Fantasy";
                case APOCALYPSE -> "Tận Thế";
                case GALAXY_COSMOS -> "Vũ Trụ";
                case ABYSSAL_ATLANTIS -> "Atlantis";
            };
            setText(label);
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            setPreferredSize(new Dimension(68, 28));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(theme.displayName + " — " + TitanMode.stageMechanicsSummary(theme));
        }

        private void setSelectedTheme(boolean sel) {
            this.selectedTheme = sel;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            boolean hover = getModel().isRollover();
            Color acc = theme.accent;
            g2.setColor(selectedTheme
                    ? new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 60)
                    : (hover ? new Color(25, 42, 68, 224) : new Color(13, 24, 42, 206)));
            g2.fillRoundRect(1, 1, w - 2, h - 2, 8, 8);
            g2.setColor(selectedTheme
                    ? acc
                    : (hover ? new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 160) : new Color(255, 255, 255, 48)));
            g2.setStroke(new BasicStroke(selectedTheme ? 1.6f : 1.0f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 8, 8);

            g2.setColor(acc);
            g2.fillOval(7, h / 2 - 3, 6, 6);
            g2.setFont(getFont());
            g2.setColor(selectedTheme ? Color.WHITE : UiTokens.TEXT_MUTED);
            FontMetrics fm = g2.getFontMetrics();
            int tx = 17 + Math.max(0, (w - 21 - fm.stringWidth(getText())) / 2);
            g2.drawString(getText(), tx, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private final class ModeCard extends JPanel {
        private final LobbyChoice choice;
        private boolean selected;
        private boolean hovered;
        private final JLabel badgeLbl = new JLabel();

        private ModeCard(LobbyChoice choice) {
            this.choice = choice;
            setOpaque(false);
            setLayout(new BorderLayout(9, 0));
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JComponent icon = new JComponent() {
                @Override public Dimension getPreferredSize() { return new Dimension(34, 34); }
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color c = choice.accent;
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), selected ? 48 : 22));
                    g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 9, 9);
                    BufferedImage sprite = SpriteAssets.getMapProp(choice.spriteKey);
                    if (sprite != null) {
                        int sz = Math.min(getWidth(), getHeight()) - 4;
                        SpriteAssets.drawSharp(g2, sprite, (getWidth() - sz) / 2, (getHeight() - sz) / 2, sz, sz);
                    }
                    g2.dispose();
                }
            };
            add(icon, BorderLayout.WEST);

            JPanel center = new JPanel(new BorderLayout(0, 2));
            center.setOpaque(false);
            JPanel topRow = new JPanel(new BorderLayout(6, 0));
            topRow.setOpaque(false);

            JLabel title = new JLabel(choice.title);
            title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            title.setForeground(UiTokens.TEXT_PRIMARY);
            topRow.add(title, BorderLayout.CENTER);

            badgeLbl.setText(choice.badge);
            badgeLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            badgeLbl.setForeground(choice.accent);
            badgeLbl.setHorizontalAlignment(SwingConstants.RIGHT);
            topRow.add(badgeLbl, BorderLayout.EAST);
            center.add(topRow, BorderLayout.NORTH);

            WrappedTextLabel desc = new WrappedTextLabel(new Font(Font.SANS_SERIF, Font.PLAIN, 10), UiTokens.TEXT_MUTED);
            desc.setText(choice.description);
            desc.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            center.add(desc, BorderLayout.CENTER);
            add(center, BorderLayout.CENTER);

            MouseAdapter forwardHover = new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { selectLobbyChoice(choice); }
            };
            addMouseListener(forwardHover);
            icon.addMouseListener(forwardHover);
            desc.addMouseListener(forwardHover);
            title.addMouseListener(forwardHover);
            badgeLbl.addMouseListener(forwardHover);
        }

        private void setSelected(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            Color acc = choice.accent;
            if (selected) {
                g2.setPaint(new GradientPaint(0, 0,
                        new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 52),
                        w, 0, new Color(18, 31, 52, 232)));
            } else {
                g2.setColor(hovered ? new Color(26, 40, 64, 224) : new Color(16, 27, 45, 206));
            }
            g2.fillRoundRect(0, 0, w, h, 10, 10);
            g2.setColor(selected
                    ? new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 235)
                    : (hovered ? new Color(148, 163, 184, 150) : new Color(56, 76, 108, 120)));
            g2.setStroke(new BasicStroke(selected ? 1.8f : 1.0f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 10, 10);
            if (selected) {
                g2.setColor(acc);
                g2.fillRoundRect(2, 6, 4, h - 12, 4, 4);
            }
            g2.dispose();
        }
    }

    private final class ShowcaseStageCanvas extends JComponent {
        private ShowcaseStageCanvas() { setOpaque(false); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            double t = OceanMapBackdrop.elapsedSeconds();
            Color acc = selectedChoice.accent;

            // Glass showcase frame
            g2.setPaint(new GradientPaint(0, 0, new Color(10, 19, 34, 175), 0, h, new Color(8, 16, 29, 208)));
            g2.fillRoundRect(0, 0, w, h, 14, 14);
            g2.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 110));
            g2.setStroke(new BasicStroke(1.3f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);

            // Center radar rings
            int rCx = (int) (w * 0.56);
            int rCy = (int) (h * 0.52);
            int maxR = Math.min(w, h) / 2 - 20;
            g2.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 34));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawOval(rCx - maxR, rCy - maxR, maxR * 2, maxR * 2);
            g2.drawOval(rCx - maxR * 2 / 3, rCy - maxR * 2 / 3, maxR * 4 / 3, maxR * 4 / 3);
            g2.drawLine(20, rCy, w - 20, rCy);
            g2.drawLine(rCx, 28, rCx, h - 24);

            float bobY = (float) (Math.sin(t * 2.2) * 6.0);
            float counterBob = (float) (Math.cos(t * 1.9) * 5.0);

            if (selectedChoice == LobbyChoice.CAMPAIGN) {
                CampaignMode.CampaignChapter[] chapters = CampaignMode.CampaignChapter.values();
                int routeY = (int) (h * 0.56);
                int padX = 52;
                int span = Math.max(80, w - padX * 2);

                // Dotted glowing naval campaign route across 5 chapters
                g2.setColor(new Color(251, 191, 36, 130));
                float dashPhase = Math.max(0f, 26f - (float) ((Math.abs(t) * 28.0) % 26.0));
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        0f, new float[]{7f, 6f}, dashPhase));
                for (int i = 0; i < chapters.length - 1; i++) {
                    int x1 = padX + (span * i) / 4;
                    int y1 = routeY + (i % 2 == 0 ? -24 : 24);
                    int x2 = padX + (span * (i + 1)) / 4;
                    int y2 = routeY + ((i + 1) % 2 == 0 ? -24 : 24);
                    g2.drawLine(x1, y1, x2, y2);
                }

                String[] nodeKeys = {
                        "stage_node_skirmish", "stage_node_minigame", "stage_node_zombie",
                        "stage_node_minigame", "stage_node_boss"
                };
                BufferedImage starGold = SpriteAssets.getMapProp("stage_star_gold");
                for (int i = 0; i < chapters.length; i++) {
                    CampaignMode.CampaignChapter ch = chapters[i];
                    int cx = padX + (span * i) / 4;
                    int cy = routeY + (i % 2 == 0 ? -24 : 24) + (int) (Math.sin(t * 2.1 + i) * 4);
                    g2.setColor(new Color(ch.accent.getRed(), ch.accent.getGreen(), ch.accent.getBlue(), 42));
                    g2.fillOval(cx - 28, cy - 28, 56, 56);
                    g2.setColor(ch.accent);
                    g2.setStroke(new BasicStroke(1.6f));
                    g2.drawOval(cx - 27, cy - 27, 54, 54);
                    BufferedImage nImg = SpriteAssets.getMapProp(nodeKeys[i]);
                    if (nImg != null) {
                        SpriteAssets.drawSharp(g2, nImg, cx - 22, cy - 22, 44, 44);
                    }
                    if (starGold != null) {
                        SpriteAssets.drawSharp(g2, starGold, cx - 8, cy + 28, 16, 16);
                    }
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                    g2.setColor(UiTokens.TEXT_PRIMARY);
                    String lbl = "C" + (ch.index + 1) + " · " + ch.startStage + "-" + ch.endStage;
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(lbl, cx - fm.stringWidth(lbl) / 2, cy - 33);
                }

                BufferedImage campaignCrest = SpriteAssets.getMapProp("home_badge_campaign");
                if (campaignCrest != null) {
                    SpriteAssets.drawSharp(g2, campaignCrest, w / 2 - 28, (int) (h * 0.25 + counterBob * 0.5f), 56, 56);
                }

                drawShowcaseBanner(g2, w, h,
                        "CHIẾN DỊCH 100 MÀN · 5 VÙNG BIỂN",
                        "Hải Chiến · Mini-game Đếm Ngược · Hạn Đạn · Radar · Zombie & 20 Boss Titan 3★",
                        acc);
            } else if (selectedChoice == LobbyChoice.COOP) {
                TitanMode.TitanBoss boss = TitanMode.getActiveBoss();
                double cx = w * 0.50;
                double cy = h * 0.56;
                // Dual P1 Cyan + P2 Emerald Fusion Beams into Boss
                g2.setStroke(new BasicStroke(6.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(56, 189, 248, 120));
                g2.drawLine(145, (int) (h * 0.43 + counterBob), (int) cx, (int) cy);
                g2.setColor(new Color(52, 211, 153, 120));
                g2.drawLine(155, (int) (h * 0.72 + bobY), (int) cx, (int) cy);
                g2.setColor(new Color(251, 191, 36, 185));
                g2.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine((int) cx, (int) cy, (int) (w * 0.77), (int) (h * 0.52 + bobY * 0.5f));
                g2.setStroke(new BasicStroke(2.2f));
                g2.setColor(Color.WHITE);
                g2.drawLine((int) cx, (int) cy, (int) (w * 0.77), (int) (h * 0.52 + bobY * 0.5f));

                BufferedImage core = SpriteAssets.getMapProp("coop_synergy_core");
                if (core != null) {
                    int sz = (int) (46 + 4 * Math.sin(t * 4.0));
                    SpriteAssets.drawSharp(g2, core, (int) cx - sz / 2, (int) cy - sz / 2, sz, sz);
                }
                BufferedImage p1Ship = SpriteAssets.getSuperShipSprite(true);
                if (p1Ship != null) {
                    SpriteAssets.drawSharp(g2, p1Ship, 22, (int) (h * 0.37 + counterBob), 156, 44);
                }
                BufferedImage p2Ship = SpriteAssets.getShipSprite(ShipType.CRUISER, true);
                if (p2Ship != null) {
                    SpriteAssets.drawSharp(g2, p2Ship, 34, (int) (h * 0.67 + bobY), 130, 38);
                }
                BufferedImage bossImg = SpriteAssets.getTitanSprite(boss, false, true);
                if (bossImg != null) {
                    int maxBox = Math.min((int) (w * 0.50), h - 92);
                    double ratio = bossImg.getWidth() / (double) Math.max(1, bossImg.getHeight());
                    int bw = maxBox;
                    int bh = (int) Math.round(bw / ratio);
                    SpriteAssets.drawSharp(g2, bossImg, w - bw - 18, (h - bh) / 2 + (int) bobY, bw, bh);
                }
                drawShowcaseBanner(g2, w, h,
                        "CO-OP BẠN BÈ · RAID BOSS TITAN (×175% HP)",
                        "Mạng LAN IP:Port · 2P Chung Máy · Đồng Minh AI · Song Long Hợp Kích & Ping",
                        acc);
            } else if (selectedChoice == LobbyChoice.TITAN) {
                TitanMode.TitanBoss boss = TitanMode.getActiveBoss();
                TitanMode.TitanDifficulty diff = TitanMode.getDifficulty();
                Color bAcc = boss.accentColor;

                // Glowing aura under Boss
                g2.setPaint(new RadialGradientPaint((float) (w * 0.68), (float) (h * 0.54), Math.max(40f, h * 0.40f),
                        new float[]{0f, 1f},
                        new Color[]{new Color(bAcc.getRed(), bAcc.getGreen(), bAcc.getBlue(), 90), new Color(0, 0, 0, 0)}));
                g2.fillOval((int) (w * 0.34), (int) (h * 0.14), (int) (w * 0.62), (int) (h * 0.78));

                // SuperShip Hyperion + Battleship facing off on the left
                BufferedImage superShip = SpriteAssets.getSuperShipSprite(true);
                if (superShip != null) {
                    int sw = Math.min(200, (int) (w * 0.38));
                    int sh = Math.max(44, (int) Math.round(sw * (superShip.getHeight() / (double) superShip.getWidth())));
                    SpriteAssets.drawSharp(g2, superShip, 22, (int) (h * 0.52 + counterBob) - sh / 2, sw, sh);
                }
                BufferedImage cruiser = SpriteAssets.getShipSprite(ShipType.CRUISER, true);
                if (cruiser != null) {
                    SpriteAssets.drawSharp(g2, cruiser, 38, (int) (h * 0.72 + bobY * 0.5f), 114, 34);
                }

                // Giant 3D Boss Sprite on the right
                BufferedImage bossImg = SpriteAssets.getTitanSprite(boss, ((int) (t * 0.4)) % 3 == 2, true);
                if (bossImg != null) {
                    int maxBox = Math.min((int) (w * 0.56), h - 86);
                    double ratio = bossImg.getWidth() / (double) Math.max(1, bossImg.getHeight());
                    int bw = maxBox;
                    int bh = (int) Math.round(bw / ratio);
                    if (bh > maxBox) {
                        bh = maxBox;
                        bw = (int) Math.round(bh * ratio);
                    }
                    int bx = w - bw - 18;
                    int by = (h - bh) / 2 + (int) bobY - 4;
                    SpriteAssets.drawSharp(g2, bossImg, bx, by, bw, bh);
                }

                int scaledHp = TitanMode.scaledMaxHp(boss, diff);
                drawShowcaseBanner(g2, w, h,
                        boss.displayName.toUpperCase(),
                        diff.label + " · " + scaledHp + " HP · 3 Bộ Phận Rụng · 5 Kỹ Năng"
                                + (TitanMode.isBossRushEnabled() ? " · BOSS RUSH 3 ẢI + DI VẬT" : ""),
                        bAcc);
            } else if (selectedChoice == LobbyChoice.ZOMBIE) {
                BufferedImage zb1 = SpriteAssets.getZombieShipSprite(ShipType.CARRIER, true);
                BufferedImage zb2 = SpriteAssets.getZombieShipSprite(ShipType.BATTLESHIP, true);
                BufferedImage zb3 = SpriteAssets.getZombieShipSprite(ShipType.DESTROYER, true);
                if (zb1 != null) SpriteAssets.drawSharp(g2, zb1, w - 250, (int) (h * 0.30 + bobY), 220, 62);
                if (zb2 != null) SpriteAssets.drawSharp(g2, zb2, w - 220, (int) (h * 0.52 + counterBob), 185, 54);
                if (zb3 != null) SpriteAssets.drawSharp(g2, zb3, w - 180, (int) (h * 0.71 + bobY * 0.6f), 130, 40);

                BufferedImage ally = SpriteAssets.getShipSprite(ShipType.BATTLESHIP, true);
                if (ally != null) SpriteAssets.drawSharp(g2, ally, 26, (int) (h * 0.48 + counterBob), 165, 50);

                drawShowcaseBanner(g2, w, h,
                        "HẠM ĐỘI XÁC SỐNG · 5 TƯỚNG ZOMBIE",
                        "Phun Axit · Ngư Lôi Bào Tử · Nguyền Rủa Hồn Ma · Thiên Thạch Máu",
                        acc);
            } else if (selectedChoice == LobbyChoice.CLASSIC) {
                BufferedImage carrier = SpriteAssets.getShipSprite(ShipType.CARRIER, true);
                BufferedImage bship = SpriteAssets.getShipSprite(ShipType.BATTLESHIP, true);
                if (carrier != null) SpriteAssets.drawSharp(g2, carrier, 28, (int) (h * 0.36 + bobY), 230, 62);
                if (bship != null) SpriteAssets.drawSharp(g2, bship, 52, (int) (h * 0.56 + counterBob), 184, 52);

                int ix = w - 220;
                int iy = (int) (h * 0.30);
                for (CombatLoadout.WeaponOption wp : CombatLoadout.WeaponOption.values()) {
                    BufferedImage wi = SpriteAssets.getWeaponIcon(wp);
                    if (wi != null) SpriteAssets.drawSharp(g2, wi, ix, iy, 44, 44);
                    ix += 50;
                }
                int mx = w - 220;
                int my = (int) (h * 0.50);
                for (CombatLoadout.MagicOption mg : CombatLoadout.MagicOption.values()) {
                    if (mg == CombatLoadout.MagicOption.NONE) continue;
                    BufferedImage mi = SpriteAssets.getMagicRune(mg);
                    if (mi != null) SpriteAssets.drawSharp(g2, mi, mx, my, 42, 42);
                    mx += 50;
                }

                drawShowcaseBanner(g2, w, h,
                        "HẢI CHIẾN CHIẾN THUẬT 10×10",
                        "Pháo Hạm · Ngư Lôi · Tên Lửa Chữ Thập · Không Kích · Kích Hoạt Nguyên Tố",
                        acc);
            } else {
                // SANDBOX
                BufferedImage superShip = SpriteAssets.getSuperShipSprite(true);
                BufferedImage shield = SpriteAssets.getMagicShield();
                if (shield != null) {
                    Composite prev = g2.getComposite();
                    g2.setComposite(AlphaComposite.SrcOver.derive(0.75f));
                    SpriteAssets.drawSharp(g2, shield, 44, (int) (h * 0.34 + bobY * 0.5f), 220, 128);
                    g2.setComposite(prev);
                }
                if (superShip != null) {
                    SpriteAssets.drawSharp(g2, superShip, 46, (int) (h * 0.45 + counterBob), 215, 62);
                }
                int bx = w - 170;
                TitanMode.TitanBoss[] bosses = TitanMode.TitanBoss.values();
                for (int i = 0; i < bosses.length; i++) {
                    BufferedImage bi = SpriteAssets.getTitanSprite(bosses[i], false, false);
                    if (bi != null) {
                        SpriteAssets.drawSharp(g2, bi, bx + (i % 2) * 58 - 40, (int) (h * 0.25 + i * 74 + bobY * 0.4f), 84, 72);
                    }
                }
                drawShowcaseBanner(g2, w, h,
                        "PHÒNG THÍ NGHIỆM SANDBOX · HẢI NĂNG ∞",
                        "Test trực tiếp toàn bộ Boss, Siêu Tàu Hyperion, Khiên 3D & 5 Bản Đồ",
                        acc);
            }

            g2.dispose();
        }

        private void drawShowcaseBanner(Graphics2D g2, int w, int h, String heading, String sub, Color accent) {
            g2.setColor(new Color(8, 15, 29, 214));
            g2.fillRoundRect(14, 14, w - 28, 52, 10, 10);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 150));
            g2.drawRoundRect(14, 14, w - 28, 52, 10, 10);

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g2.setColor(accent);
            g2.drawString(heading, 26, 35);

            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            g2.setColor(UiTokens.TEXT_PRIMARY);
            g2.drawString(sub, 26, 54);
        }
    }

    private final class GuideModalOverlay extends JPanel {
        private GuideModalOverlay() {
            super(new GridBagLayout());
            setOpaque(false);
            JPanel card = new JPanel(new BorderLayout(0, 12)) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(7, 14, 28, 245));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.setColor(new Color(56, 189, 248, 210));
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                    g2.dispose();
                }
            };
            card.setOpaque(false);
            card.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
            card.setPreferredSize(new Dimension(660, 476));
            card.setMaximumSize(new Dimension(680, 496));

            JLabel title = new JLabel("CẨM NANG HẢI ĐỒ & ĐẤU TRƯỜNG ARENA V2");
            title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
            title.setForeground(new Color(0x38BDF8));
            card.add(title, BorderLayout.NORTH);

            JPanel rows = new JPanel(new GridLayout(5, 1, 0, 8));
            rows.setOpaque(false);
            addRule(rows, "1. 5 BẢN ĐỒ CƠ CHẾ RIÊNG (KHÓA TRONG RAID)", new Color(0x22D3EE),
                    "Hải Vực (Hải Lưu & Phao Sonar) · Tận Thế (Khe Dung Nham) · Atlantis (Trụ Cổ Ngữ) · Fantasy (Leynode -35% CD & +Khiên) · Galaxy (Lướt 2 ô & Đá Pha Lê).");
            addRule(rows, "2. ĐÁ CHẮN 3D & TRẠM TIẾP TẾ NỔI", new Color(0x34D399),
                    "Đá Chắn tự đỡ đòn Boss (2 HP, 3 HP khi có Pháo Đài Đảo). Lái tàu qua Trạm nổi để vá ô tàu, hồi +3 Mana hoặc nạp +22% Khiên.");
            addRule(rows, "3. NGẮT CHIÊU BOSS & CHỈ THỊ CHIẾN THUẬT", new Color(0xFACC15),
                    "Bắn bộ phận viền VÀNG khi Boss tụ chiêu để Choáng + rụng bộ phận; hoàn thành Chỉ Thị giữa trận nhận Mana & Siêu Tàu.");
            addRule(rows, "4. DI VẬT ROGUELITE TRONG BOSS RUSH", new Color(0xA855F7),
                    "Vượt mỗi Boss trong Boss Rush được chọn 1 trong 3 Di Vật 3D cộng dồn suốt chiến dịch 3 ải.");
            addRule(rows, "5. PHÍM TẮT NHANH", UiTokens.TEXT_PRIMARY,
                    "[W A S D] lái tàu né vùng đỏ · [R] xoay tàu · [1-4] chọn Vũ Khí · [Esc] đóng bảng hướng dẫn.");
            card.add(rows, BorderLayout.CENTER);

            TacticalButton close = new TacticalButton("Đã Rõ · Quay Lại Sảnh", true);
            close.setPreferredSize(new Dimension(210, 38));
            close.addActionListener(e -> {
                SoundEngine.playUiClick();
                setGuideOpen(false);
            });
            JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            foot.setOpaque(false);
            foot.add(close);
            card.add(foot, BorderLayout.SOUTH);

            add(card);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (!card.getBounds().contains(e.getPoint())) setGuideOpen(false);
                }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(2, 6, 14, 175));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        private void addRule(JPanel host, String head, Color accent, String detail) {
            JPanel row = new JPanel(new BorderLayout(0, 2));
            row.setOpaque(false);
            JLabel h = new JLabel(head);
            h.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            h.setForeground(accent);
            WrappedTextLabel d = new WrappedTextLabel(new Font(Font.SANS_SERIF, Font.PLAIN, 12), UiTokens.TEXT_PRIMARY);
            d.setText(detail);
            row.add(h, BorderLayout.NORTH);
            row.add(d, BorderLayout.CENTER);
            host.add(row);
        }
    }

    /**
     * Multi-line word-wrapped text label rendered directly from live component width,
     * preventing JTextArea first-layout caching clips on compact Swing cards and modals.
     */
    static final class WrappedTextLabel extends JComponent {
        private final Font textFont;
        private Color textColor;
        private String text = "";

        WrappedTextLabel(Font textFont, Color textColor) {
            this.textFont = textFont;
            this.textColor = textColor;
            setOpaque(false);
        }

        void setTextColor(Color textColor) {
            this.textColor = textColor;
            repaint();
        }

        void setText(String text) {
            this.text = text != null ? text : "";
            revalidate();
            repaint();
        }

        private List<String> computeWrappedLines(FontMetrics fm, int maxWidth) {
            List<String> lines = new ArrayList<>();
            if (text.isEmpty() || maxWidth <= 20) return lines;
            String[] paragraphs = text.split("\\n");
            for (String para : paragraphs) {
                String trimmed = para.trim();
                if (trimmed.isEmpty()) {
                    lines.add("");
                    continue;
                }
                String[] words = trimmed.split("\\s+");
                StringBuilder current = new StringBuilder();
                for (String word : words) {
                    String candidate = current.isEmpty() ? word : current + " " + word;
                    if (fm.stringWidth(candidate) <= maxWidth) {
                        current.setLength(0);
                        current.append(candidate);
                    } else {
                        if (!current.isEmpty()) {
                            lines.add(current.toString());
                            current.setLength(0);
                        }
                        if (fm.stringWidth(word) <= maxWidth) {
                            current.append(word);
                        } else {
                            for (int i = 0; i < word.length(); i++) {
                                char c = word.charAt(i);
                                if (fm.stringWidth(current.toString() + c) > maxWidth && !current.isEmpty()) {
                                    lines.add(current.toString());
                                    current.setLength(0);
                                }
                                current.append(c);
                            }
                        }
                    }
                }
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
            }
            return lines;
        }

        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(textFont);
            int width = getWidth() > 40 ? getWidth() - 4 : 260;
            List<String> lines = computeWrappedLines(fm, width);
            int lineHeight = Math.max(14, fm.getAscent() + fm.getDescent() + 1);
            int h = Math.max(lineHeight, lines.size() * lineHeight + 2);
            return new Dimension(180, h);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (text.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(textFont);
            g2.setColor(textColor);
            FontMetrics fm = g2.getFontMetrics(textFont);
            int maxWidth = Math.max(40, getWidth() - 4);
            List<String> lines = computeWrappedLines(fm, maxWidth);
            int lineHeight = Math.max(14, fm.getAscent() + fm.getDescent() + 1);
            int y = fm.getAscent();
            for (String line : lines) {
                if (y - fm.getAscent() > getHeight()) break;
                g2.drawString(line, 0, y);
                y += lineHeight;
            }
            g2.dispose();
        }
    }
}
