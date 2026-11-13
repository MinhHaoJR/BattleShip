package battleship.ui;

import battleship.logic.Game;
import battleship.logic.Shot;
import battleship.model.Board;
import battleship.model.CellState;
import battleship.model.Ship;
import battleship.model.ShipType;
import battleship.model.ShotResult;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Two-board battle screen with a resource-aware weapon and magic loadout. */
public class GamePanel extends JPanel {
    private final Game game;
    private final boolean sandboxMode;
    private final SandboxController sandbox;
    private final Runnable onNewGame;
    private final BoardPanel playerBoardPanel;
    private final BoardPanel computerBoardPanel;
    private final CombatFxOverlay fxOverlay = new CombatFxOverlay();
    private final JLabel message = new JLabel("Lượt của bạn — Chọn một ô trên hải đồ địch để khai hỏa.");
    private final JLabel manaLabel = new JLabel();
    private final JLabel costLabel = new JLabel();
    private final JLabel comboLabel = new JLabel();
    private final JLabel shotsLabel = new JLabel();
    private final TacticalButton rotateButton = new TacticalButton("Xoay (R)", false);
    private final TacticalButton shieldDemoButton = new TacticalButton("Khiên: Tắt", false);
    private final TacticalButton superShipButton = new TacticalButton("Siêu Tàu: 0%", false);
    private final ZombieMode.ModePill modePill = new ZombieMode.ModePill();
    private final OceanMapBackdrop.AtmospherePill mapPill =
            new OceanMapBackdrop.AtmospherePill(OceanMapBackdrop.AtmospherePill.Kind.MAP);
    private final OceanMapBackdrop.AtmospherePill seasonPill =
            new OceanMapBackdrop.AtmospherePill(OceanMapBackdrop.AtmospherePill.Kind.SEASON);
    private final OceanMapBackdrop.AtmospherePill weatherPill =
            new OceanMapBackdrop.AtmospherePill(OceanMapBackdrop.AtmospherePill.Kind.WEATHER);
    private final JLabel dragonBannerLabel = new JLabel();
    private final ManaPips manaPips = new ManaPips();
    private final List<LoadoutChip> weaponChips = new ArrayList<>();
    private final List<LoadoutChip> magicChips = new ArrayList<>();
    private final List<LoadoutChip> zombieChips = new ArrayList<>();
    private final List<LoadoutChip> titanChips = new ArrayList<>();
    private final TitanMode.TitanPill titanPill = new TitanMode.TitanPill();
    private JPanel zombieRow;
    private JPanel titanRow;
    private final boolean titanBattleMode;
    private final TitanBattleController titan;
    private CombatLoadout.WeaponOption selectedWeapon = CombatLoadout.WeaponOption.CANNON;
    private CombatLoadout.MagicOption selectedMagic = CombatLoadout.MagicOption.NONE;
    private ZombieMode.ZombieClass selectedZombie = ZombieMode.ZombieClass.PLAGUE_DESTROYER;
    private TitanMode.TitanSkill selectedTitanSkill = TitanMode.TitanSkill.GZ_ATOMIC_BREATH;
    private boolean torpedoHorizontal = true;
    private boolean shieldPreviewEnabled;
    private boolean shieldShatteredState;
    private int mana = CombatLoadout.STARTING_MANA;
    private boolean turnLocked;
    private boolean gameOver;
    private boolean titanIntroPendingInitial;
    private final List<int[]> revealedEnemyCells = new ArrayList<>();
    private boolean sandboxRevealEnemy;
    private boolean sandboxEnemyCounter = true;
    private int enemyFrozenTurns;
    private int fleetSpirit;
    private boolean superShipActive;
    private boolean superShipCutscenePlaying;
    private boolean superShipReadyAnnounced;
    private boolean hyperCannonArmed;
    private int hyperCannonCooldown;
    private TacticalButton sandboxRevealBtn;
    private TacticalButton sandboxCounterBtn;

    public GamePanel(Game game, Runnable onNewGame) {
        this(Objects.requireNonNull(game, "game"), onNewGame, false, false, null, null);
    }

    public static GamePanel createSandbox(List<Ship> playerShips, Runnable onNewGame) {
        return new GamePanel(null, onNewGame, true, false, playerShips, null);
    }

    public static GamePanel createTitanBattle(List<Ship> playerShips, TitanMode.TitanBoss boss, Runnable onNewGame) {
        TitanMode.TitanBoss chosen = boss != null ? boss : TitanMode.TitanBoss.GODZILLA;
        TitanMode.setActiveBoss(chosen);
        ZombieMode.setGameMode(ZombieMode.GameMode.TITAN);
        return new GamePanel(null, onNewGame, false, true, playerShips, chosen);
    }

    private GamePanel(
            Game game,
            Runnable onNewGame,
            boolean sandboxMode,
            boolean titanBattleMode,
            List<Ship> initialPlayerShips,
            TitanMode.TitanBoss initialBoss
    ) {
        this.game = game;
        this.sandboxMode = sandboxMode;
        this.titanBattleMode = titanBattleMode;
        this.sandbox = sandboxMode ? new SandboxController(initialPlayerShips) : null;
        this.titan = (titanBattleMode || sandboxMode)
                ? new TitanBattleController(
                        sandboxMode ? this.sandbox.getPlayerShips() : initialPlayerShips,
                        initialBoss != null ? initialBoss : TitanMode.getActiveBoss())
                : null;
        this.onNewGame = onNewGame;
        this.fleetSpirit = sandboxMode ? 100 : 0;
        if (sandboxMode || titanBattleMode) {
            this.mana = CombatLoadout.MAX_MANA;
        }
        setLayout(new BorderLayout(UiTokens.SPACE_MD, UiTokens.SPACE_MD));
        setOpaque(true);
        setBackground(UiTokens.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(UiTokens.SPACE_MD, UiTokens.SPACE_LG, UiTokens.SPACE_MD, UiTokens.SPACE_LG));

        add(createStatusBar(), BorderLayout.NORTH);
        JPanel boards = new JPanel(new GridLayout(1, 2, UiTokens.SPACE_LG, 0));
        boards.setOpaque(false);
        if (sandboxMode) {
            playerBoardPanel = new BoardPanel(null, "Hạm đội của bạn (Click ô = Khiên)", true);
            playerBoardPanel.setCellStateOverride(sandbox.getPlayerGrid());
            playerBoardPanel.setKnownShips(sandbox.getPlayerShips());
            playerBoardPanel.setInteractive(true);
            playerBoardPanel.setOnCellClick(this::toggleSandboxPlayerShieldCell);
            computerBoardPanel = new BoardPanel(null, "Vùng biển địch (Sandbox)", false);
            computerBoardPanel.setCellStateOverride(sandbox.getEnemyGrid());
            computerBoardPanel.setKnownShips(sandbox.getEnemyShips());
        } else if (titanBattleMode) {
            playerBoardPanel = new BoardPanel(null, "Hạm đội của bạn", true);
            playerBoardPanel.setCellStateOverride(titan.getPlayerGrid());
            playerBoardPanel.setKnownShips(titan.getPlayerShips());
            playerBoardPanel.setInteractive(false);
            computerBoardPanel = new BoardPanel(null, "Đấu Trường Titan", false);
            computerBoardPanel.setCellStateOverride(titan.getBossGrid());
            computerBoardPanel.setKnownShips(List.of());
        } else {
            playerBoardPanel = new BoardPanel(game.playerBoard, "Hạm đội của bạn", true);
            playerBoardPanel.setInteractive(false);
            computerBoardPanel = new BoardPanel(game.computerBoard, "Vùng biển địch", false);
        }
        playerBoardPanel.setAmbientStageDriven(true);
        computerBoardPanel.setAmbientStageDriven(true);
        computerBoardPanel.setInteractive(true);
        computerBoardPanel.setOnCellClick(this::fireAt);
        computerBoardPanel.setOnCellHover(this::updateTargetPreview);
        boards.add(playerBoardPanel);
        boards.add(computerBoardPanel);

        JLayeredPane boardStage = new JLayeredPane() {
            @Override public Dimension getPreferredSize() { return boards.getPreferredSize(); }
            @Override public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                boards.setBounds(0, 0, w, h);
                fxOverlay.setBounds(0, 0, w, h);
            }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                OceanMapBackdrop.paintOceanMap(g, getWidth(), getHeight(), OceanMapBackdrop.elapsedSeconds(), true);
                g.dispose();
            }
        };
        boardStage.setOpaque(true);
        boardStage.add(boards, JLayeredPane.DEFAULT_LAYER);
        boardStage.add(fxOverlay, JLayeredPane.PALETTE_LAYER);
        add(boardStage, BorderLayout.CENTER);
        add(createLoadoutBar(), BorderLayout.SOUTH);
        installTorpedoShortcut();
        applyGameMode(false);
        if (sandboxMode) {
            showMessage("SANDBOX SẴN SÀNG — Hải Năng ∞! Click hải đồ địch để bắn, click ô tàu mình để bật/tắt Khiên 3D.",
                    UiTokens.ACCENT_SIGNAL);
        } else if (titanBattleMode) {
            TitanMode.TitanBoss b = titan.getBoss();
            showMessage("ĐẠI CHIẾN TITAN — Hạ " + b.shortLabel + " (20 HP)! Phá điểm yếu x2 sát thương.",
                    b.accentColor);
            SoundEngine.refreshThemeMusic();
            titanIntroPendingInitial = true;
            fxOverlay.addHierarchyListener(e -> {
                if (titanIntroPendingInitial && fxOverlay.isShowing()) {
                    titanIntroPendingInitial = false;
                    SwingUtilities.invokeLater(() -> triggerTitanBossEntrance(titan.getBoss()));
                }
            });
        }
        refreshLoadout();
        refreshShots();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0x050C17), 0, getHeight(), new Color(0x0B1E34)));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(14, 24, 41, 224));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        bar.setOpaque(false);
        bar.setLayout(new javax.swing.BoxLayout(bar, javax.swing.BoxLayout.Y_AXIS));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 189, 248, 72), 1),
                BorderFactory.createEmptyBorder(UiTokens.SPACE_XS, UiTokens.SPACE_MD, UiTokens.SPACE_XS, UiTokens.SPACE_MD)));

        // Top Row: Tactical message + Combat action buttons
        JPanel topRow = new JPanel(new BorderLayout(UiTokens.SPACE_MD, 0));
        topRow.setOpaque(false);
        topRow.setAlignmentX(LEFT_ALIGNMENT);
        message.setFont(UiTokens.FONT_BODY);
        message.setForeground(UiTokens.TEXT_PRIMARY);
        message.setMinimumSize(new Dimension(80, 0));
        topRow.add(message, BorderLayout.CENTER);

        JPanel combatActions = new JPanel();
        combatActions.setLayout(new javax.swing.BoxLayout(combatActions, javax.swing.BoxLayout.X_AXIS));
        combatActions.setOpaque(false);
        FontMetrics sfm = getFontMetrics(UiTokens.FONT_BUTTON);
        int maxSuperW = 98;
        for (String lbl : new String[]{"Siêu Tàu: 100%", "★ SIÊU TÀU!", "Đang chế...", "★ Đại Pháo", "★ Khóa Mục Tiêu", "★ Pháo (2)"}) {
            maxSuperW = Math.max(maxSuperW, sfm.stringWidth(lbl) + UiTokens.BUTTON_PADDING_X * 2 + 10);
        }
        Dimension superPref = new Dimension(maxSuperW, UiTokens.CHIP_HEIGHT);
        superShipButton.setPreferredSize(superPref);
        superShipButton.setMinimumSize(superPref);
        superShipButton.setMaximumSize(superPref);
        superShipButton.addActionListener(event -> onSuperShipButtonClicked());
        updateSuperShipButtonUi();
        combatActions.add(superShipButton);
        combatActions.add(javax.swing.Box.createHorizontalStrut(5));

        int maxShieldW = 92;
        for (String lbl : new String[]{"Khiên: Tắt", "Khiên: 100%", "Khiên: Vỡ!"}) {
            maxShieldW = Math.max(maxShieldW, sfm.stringWidth(lbl) + UiTokens.BUTTON_PADDING_X * 2 + 10);
        }
        Dimension shieldPref = new Dimension(maxShieldW, UiTokens.CHIP_HEIGHT);
        shieldDemoButton.setPreferredSize(shieldPref);
        shieldDemoButton.setMinimumSize(shieldPref);
        shieldDemoButton.setMaximumSize(shieldPref);
        shieldDemoButton.setToolTipText("Bật/Tắt Khiên Phép Thuật 3D bảo vệ hạm đội (100% Năng Lượng)");
        shieldDemoButton.addActionListener(event -> toggleShieldPreview());
        updateShieldButtonUi();
        combatActions.add(shieldDemoButton);
        combatActions.add(javax.swing.Box.createHorizontalStrut(5));
        TacticalButton demoFxButton = new TacticalButton("Thử FX 3D", false);
        demoFxButton.setPreferredSize(new Dimension(demoFxButton.getPreferredSize().width, UiTokens.CHIP_HEIGHT));
        demoFxButton.setMinimumSize(demoFxButton.getPreferredSize());
        demoFxButton.setMaximumSize(demoFxButton.getPreferredSize());
        demoFxButton.setToolTipText("Xem trước hiệu ứng Blender 3D theo từng lớp tàu và kỹ năng đang chọn");
        demoFxButton.addActionListener(event -> previewSelectedFx());
        combatActions.add(demoFxButton);
        combatActions.add(javax.swing.Box.createHorizontalStrut(5));
        TacticalButton newGameButton = new TacticalButton(sandboxMode ? "Về Bố Trí" : "Ván mới", false);
        newGameButton.setPreferredSize(new Dimension(newGameButton.getPreferredSize().width, UiTokens.CHIP_HEIGHT));
        newGameButton.setMinimumSize(newGameButton.getPreferredSize());
        newGameButton.setMaximumSize(newGameButton.getPreferredSize());
        newGameButton.addActionListener(event -> requestNewGame());
        combatActions.add(newGameButton);
        topRow.add(combatActions, BorderLayout.EAST);

        // Second Row: 5 Map Themes, 4 Seasons, Weather, Game Mode + Active Patrolling Dragon badge
        JPanel worldRow = new JPanel(new BorderLayout(UiTokens.SPACE_SM, 0));
        worldRow.setOpaque(false);
        worldRow.setAlignmentX(LEFT_ALIGNMENT);

        JPanel worldPills = new JPanel();
        worldPills.setLayout(new javax.swing.BoxLayout(worldPills, javax.swing.BoxLayout.X_AXIS));
        worldPills.setOpaque(false);
        mapPill.addActionListener(event -> {
            OceanMapBackdrop.MapTheme mt = OceanMapBackdrop.cycleMapTheme();
            SoundEngine.playUiClick();
            SoundEngine.refreshThemeMusic();
            SoundEngine.playMapPropCue(mt);
            syncEnvironmentControls();
            showMessage("Bản đồ " + mt.displayName + " — " + OceanMapBackdrop.activeDragonSummary() + " tuần tra.", mt.accent);
            repaint();
        });
        worldPills.add(mapPill);
        worldPills.add(javax.swing.Box.createHorizontalStrut(5));
        seasonPill.addActionListener(event -> {
            OceanMapBackdrop.Season s = OceanMapBackdrop.cycleSeason();
            SoundEngine.playUiClick();
            SoundEngine.playSeasonTransitionCue(s);
            syncEnvironmentControls();
            showMessage(s.displayName + " — tác động gió & ánh sáng lên hải đồ · " + OceanMapBackdrop.activeDragonSummary() + ".", s.accent);
            repaint();
        });
        worldPills.add(seasonPill);
        worldPills.add(javax.swing.Box.createHorizontalStrut(5));
        weatherPill.addActionListener(event -> {
            OceanMapBackdrop.WeatherMode w = OceanMapBackdrop.cycleWeatherMode();
            double st = OceanMapBackdrop.sampleStormLevel(OceanMapBackdrop.elapsedSeconds());
            SoundEngine.playUiClick();
            SoundEngine.playWeatherTransitionCue(w, st);
            syncEnvironmentControls();
            String fxDesc = switch (w) {
                case STORM -> "Sấm chớp đánh xuống mặt biển, sóng dữ & mưa quất lên hải đồ!";
                case CLEAR -> "Trời quang mây tạnh, mặt biển lặng sóng & phản chiếu ánh sáng.";
                case AUTO -> "Chu kỳ thời tiết động — mây bão, sấm chớp & gió biển biến thiên.";
            };
            showMessage(w.displayName + " — " + fxDesc, w.accent);
            repaint();
        });
        worldPills.add(weatherPill);
        worldPills.add(javax.swing.Box.createHorizontalStrut(5));
        modePill.addActionListener(event -> {
            if (titanBattleMode) {
                TitanMode.TitanBoss b = TitanMode.cycleActiveBoss();
                SoundEngine.playUiClick();
                switchActiveTitanBoss(b);
                return;
            }
            ZombieMode.GameMode gm = ZombieMode.cycleGameMode();
            SoundEngine.playUiClick();
            SoundEngine.refreshThemeMusic();
            syncEnvironmentControls();
            applyGameMode(true);
            if (gm == ZombieMode.GameMode.TITAN) {
                triggerTitanBossEntrance(TitanMode.getActiveBoss());
            }
            showMessage("Chế độ: " + gm.displayName + " · " + OceanMapBackdrop.activeDragonSummary() + ".", gm.accent);
        });
        worldPills.add(modePill);
        worldRow.add(worldPills, BorderLayout.WEST);

        dragonBannerLabel.setFont(UiTokens.FONT_META);
        dragonBannerLabel.setForeground(UiTokens.TEXT_MUTED);
        dragonBannerLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        dragonBannerLabel.setMinimumSize(new Dimension(60, 0));
        worldRow.add(dragonBannerLabel, BorderLayout.CENTER);

        bar.add(topRow);
        bar.add(javax.swing.Box.createVerticalStrut(4));
        bar.add(worldRow);
        if (sandboxMode) {
            bar.add(javax.swing.Box.createVerticalStrut(4));
            bar.add(createSandboxControlRow());
        }
        syncEnvironmentControls();
        return bar;
    }

    private JPanel createSandboxControlRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel badge = new JLabel("SANDBOX · MANA ∞");
        badge.setFont(UiTokens.FONT_META);
        badge.setForeground(UiTokens.ACCENT_SIGNAL);
        badge.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 6));
        row.add(badge);

        sandboxRevealBtn = createSandboxBarButton("Soi Địch: Ẩn", "Ẩn/Hiện toàn bộ vị trí 5 tàu địch để kiểm chứng bắn trúng");
        sandboxRevealBtn.addActionListener(e -> toggleSandboxRevealEnemy());
        row.add(sandboxRevealBtn);

        sandboxCounterBtn = createSandboxBarButton("Địch Phản: Bật", "Bật/Tắt tự động AI hoặc Boss Zombie bắn trả sau lượt bạn");
        sandboxCounterBtn.addActionListener(e -> toggleSandboxEnemyCounter());
        row.add(sandboxCounterBtn);

        TacticalButton strikeNowBtn = createSandboxBarButton("Địch Bắn Ngay", "Kích hoạt 1 phát bắn phản công từ AI hoặc Boss Zombie ngay");
        strikeNowBtn.addActionListener(e -> triggerSandboxImmediateCounter());
        row.add(strikeNowBtn);

        TacticalButton testWinBtn = createSandboxBarButton("Thử Thắng", "Test trạng thái & nhạc Chiến Thắng");
        testWinBtn.addActionListener(e -> finishSandboxGame(true));
        row.add(testWinBtn);

        TacticalButton testLoseBtn = createSandboxBarButton("Thử Thua", "Test trạng thái & nhạc Thất Bại");
        testLoseBtn.addActionListener(e -> finishSandboxGame(false));
        row.add(testLoseBtn);

        TacticalButton resetBtn = createSandboxBarButton("Reset Sandbox", "Tạo hạm đội mới ngẫu nhiên, xóa hết phát bắn/FX/vùng 3×3");
        resetBtn.addActionListener(e -> resetSandbox());
        row.add(resetBtn);
        return row;
    }

    private static TacticalButton createSandboxBarButton(String text, String tooltip) {
        TacticalButton btn = new TacticalButton(text, false);
        btn.setFont(UiTokens.FONT_META);
        int width = Math.max(btn.getFontMetrics(UiTokens.FONT_META).stringWidth(text) + 22, 84);
        btn.setPreferredSize(new Dimension(width, UiTokens.CHIP_HEIGHT));
        btn.setToolTipText(tooltip);
        return btn;
    }

    private void syncEnvironmentControls() {
        mapPill.sync();
        seasonPill.sync();
        weatherPill.sync();
        modePill.sync();
        dragonBannerLabel.setText("Rồng: " + OceanMapBackdrop.activeDragonSummary());
        dragonBannerLabel.setForeground(OceanMapBackdrop.getMapTheme().accent);
    }

    private JPanel createLoadoutBar() {
        JPanel bar = new JPanel(new BorderLayout(UiTokens.SPACE_MD, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(14, 24, 41, 228));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 189, 248, 72), 1),
                BorderFactory.createEmptyBorder(UiTokens.SPACE_SM, UiTokens.SPACE_MD, UiTokens.SPACE_SM, UiTokens.SPACE_MD)));

        JPanel summary = new JPanel();
        summary.setOpaque(false);
        summary.setLayout(new javax.swing.BoxLayout(summary, javax.swing.BoxLayout.Y_AXIS));
        JPanel manaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTokens.SPACE_SM, 0));
        manaRow.setOpaque(false);
        manaRow.setAlignmentX(LEFT_ALIGNMENT);
        manaLabel.setFont(UiTokens.FONT_BODY);
        manaLabel.setForeground(UiTokens.TEXT_PRIMARY);
        manaRow.add(manaLabel);
        manaRow.add(manaPips);
        summary.add(manaRow);
        summary.add(javax.swing.Box.createVerticalStrut(UiTokens.SPACE_XS));
        costLabel.setFont(UiTokens.FONT_META);
        costLabel.setAlignmentX(LEFT_ALIGNMENT);
        summary.add(costLabel);
        comboLabel.setFont(UiTokens.FONT_META);
        comboLabel.setForeground(UiTokens.TEXT_MUTED);
        comboLabel.setAlignmentX(LEFT_ALIGNMENT);
        comboLabel.setMaximumSize(new Dimension(276, UiTokens.SPACE_LG * 2 + 8));
        comboLabel.setToolTipText("Mô tả hiệu ứng của tổ hợp đang chọn");
        summary.add(comboLabel);
        shotsLabel.setFont(UiTokens.FONT_META);
        shotsLabel.setForeground(UiTokens.TEXT_MUTED);
        shotsLabel.setAlignmentX(LEFT_ALIGNMENT);
        shotsLabel.setMinimumSize(new Dimension(252, 18));
        shotsLabel.setPreferredSize(new Dimension(262, 18));
        shotsLabel.setMaximumSize(new Dimension(276, 22));
        summary.add(shotsLabel);
        java.awt.Component summarySpacer = javax.swing.Box.createRigidArea(new Dimension(258, 0));
        if (summarySpacer instanceof javax.swing.JComponent jc) jc.setAlignmentX(LEFT_ALIGNMENT);
        summary.add(summarySpacer);
        bar.add(summary, BorderLayout.WEST);

        JPanel selectors = new JPanel();
        selectors.setOpaque(false);
        selectors.setLayout(new javax.swing.BoxLayout(selectors, javax.swing.BoxLayout.Y_AXIS));
        JPanel weaponRow = createWeaponRow();
        weaponRow.setAlignmentX(LEFT_ALIGNMENT);
        selectors.add(weaponRow);
        selectors.add(javax.swing.Box.createVerticalStrut(UiTokens.LOADOUT_GAP));
        JPanel magicRow = createMagicRow();
        magicRow.setAlignmentX(LEFT_ALIGNMENT);
        selectors.add(magicRow);
        zombieRow = createZombieSkillRow();
        zombieRow.setAlignmentX(LEFT_ALIGNMENT);
        zombieRow.setVisible(ZombieMode.isZombieMode());
        selectors.add(zombieRow);
        titanRow = createTitanSkillRow();
        titanRow.setAlignmentX(LEFT_ALIGNMENT);
        titanRow.setVisible(ZombieMode.isTitanMode());
        selectors.add(titanRow);
        bar.add(selectors, BorderLayout.CENTER);
        return bar;
    }

    private JPanel createTitanSkillRow() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new javax.swing.BoxLayout(wrapper, javax.swing.BoxLayout.Y_AXIS));
        wrapper.add(javax.swing.Box.createVerticalStrut(UiTokens.LOADOUT_GAP));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);

        titanPill.addActionListener(event -> {
            TitanMode.TitanBoss next = TitanMode.cycleActiveBoss();
            SoundEngine.playUiClick();
            switchActiveTitanBoss(next);
        });
        row.add(titanPill);

        List<TitanMode.TitanSkill> initialSkills = TitanMode.skillsFor(TitanMode.getActiveBoss());
        for (int i = 0; i < initialSkills.size(); i++) {
            final int slot = i;
            TitanMode.TitanSkill s = initialSkills.get(i);
            LoadoutChip chip = new LoadoutChip(s.chipLabel, s.accentColor, true, SpriteAssets.getTitanSkillSprite(s), true);
            chip.setToolTipText(s.skillName + " — " + s.description);
            chip.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent event) {
                    List<TitanMode.TitanSkill> current = TitanMode.skillsFor(
                            titan != null ? titan.getBoss() : TitanMode.getActiveBoss());
                    if (slot < current.size()) {
                        TitanMode.TitanSkill chosen = current.get(slot);
                        if (!sandboxMode && chosen.phase2Only && titan != null && !titan.isEnraged()) {
                            SoundEngine.playUiClick();
                            showMessage("Kỹ năng Phase 2 Cuồng Bạo (" + chosen.skillName
                                    + ") mở khóa khi Boss <= 50% HP (hoặc bật Sandbox để test ngay)!",
                                    UiTokens.STATUS_WARNING);
                            return;
                        }
                        selectedTitanSkill = chosen;
                        SoundEngine.playUiClick();
                        refreshLoadout();
                        triggerTitanSkillNow(chosen);
                    }
                }
            });
            titanChips.add(chip);
            row.add(chip);
        }
        wrapper.add(row);
        return wrapper;
    }

    private JPanel createZombieSkillRow() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new javax.swing.BoxLayout(wrapper, javax.swing.BoxLayout.Y_AXIS));
        wrapper.add(javax.swing.Box.createVerticalStrut(UiTokens.LOADOUT_GAP));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        for (ZombieMode.ZombieClass zc : ZombieMode.ZombieClass.values()) {
            String shortChip = switch (zc) {
                case PLAGUE_DESTROYER -> "Axit 1×2";
                case BLIGHT_SUBMARINE -> "Bào Tử 1×3";
                case NECRO_CRUISER -> "Hút Hồn";
                case BLOOD_BATTLESHIP -> "Huyết Pháo";
                case ABYSS_CARRIER -> "Bầy Dơi 3×3";
            };
            LoadoutChip chip = new LoadoutChip(shortChip, zc.accentColor, true, SpriteAssets.getZombieProjectileSprite(zc));
            chip.setToolTipText(zc.title + " · Skill: " + zc.skillName + " — " + zc.description);
            chip.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent event) {
                    selectedZombie = zc;
                    SoundEngine.playUiClick();
                    refreshLoadout();
                    if (sandboxMode) {
                        executeSandboxZombieBossTurn(zc);
                    } else {
                        int tr = 3 + (zc.ordinal() % 4);
                        int tc = 3 + ((zc.ordinal() * 2) % 4);
                        triggerZombieSkill(zc, tr, tc);
                    }
                }
            });
            zombieChips.add(chip);
            row.add(chip);
        }
        wrapper.add(row);
        return wrapper;
    }

    private JPanel createWeaponRow() {
        JPanel row = new JPanel();
        row.setLayout(new javax.swing.BoxLayout(row, javax.swing.BoxLayout.X_AXIS));
        row.setOpaque(false);
        for (CombatLoadout.WeaponOption option : CombatLoadout.WeaponOption.values()) {
            LoadoutChip chip = new LoadoutChip(option.label + " (" + option.manaCost + ")",
                    UiTokens.ACCENT_SIGNAL, false, SpriteAssets.getWeaponIcon(option));
            chip.setToolTipText(option.description);
            chip.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent event) {
                    selectedWeapon = option;
                    SoundEngine.playUiClick();
                    refreshLoadout();
                    updateTargetPreview(computerBoardPanel.getHoveredRow(), computerBoardPanel.getHoveredCol());
                }
            });
            weaponChips.add(chip);
            row.add(chip);
            row.add(javax.swing.Box.createHorizontalStrut(4));
        }
        Dimension rotDim = new Dimension(98, UiTokens.CHIP_HEIGHT);
        rotateButton.setPreferredSize(rotDim);
        rotateButton.setMinimumSize(rotDim);
        rotateButton.setMaximumSize(rotDim);
        rotateButton.addActionListener(event -> rotateTorpedo());
        row.add(rotateButton);
        return row;
    }

    private JPanel createMagicRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTokens.LOADOUT_GAP, 0));
        row.setOpaque(false);
        for (CombatLoadout.MagicOption option : CombatLoadout.MagicOption.values()) {
            LoadoutChip chip = new LoadoutChip(option.label + " (+" + option.manaCost + ")", option.accentColor, true, null);
            chip.setToolTipText(option.description);
            chip.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent event) {
                    selectedMagic = option;
                    SoundEngine.playUiClick();
                    refreshLoadout();
                    updateTargetPreview(computerBoardPanel.getHoveredRow(), computerBoardPanel.getHoveredCol());
                }
            });
            magicChips.add(chip);
            row.add(chip);
        }
        return row;
    }

    private void installTorpedoShortcut() {
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke('R'), "rotateTorpedo");
        getActionMap().put("rotateTorpedo", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                if (selectedWeapon == CombatLoadout.WeaponOption.TORPEDO) rotateTorpedo();
            }
        });
    }

    private void rotateTorpedo() {
        if (selectedWeapon != CombatLoadout.WeaponOption.TORPEDO) return;
        torpedoHorizontal = !torpedoHorizontal;
        SoundEngine.playUiClick();
        updateRotateButton();
        computerBoardPanel.refreshPointerHover();
    }

    private void updateTargetPreview(int row, int col) {
        if (row < 0 || col < 0) {
            computerBoardPanel.clearTargetPreview();
            if (!turnLocked && !gameOver) {
                if (hyperCannonArmed && superShipActive) {
                    showMessage("★ ĐẠI PHÁO HYPERION ĐÃ KHÓA NÒNG — Chọn ô trên hải đồ địch để bắn 3×3 (0 Mana + Choáng)!",
                            new Color(0xFDE047));
                } else {
                    showMessage("Lượt của bạn — Chọn một ô trên hải đồ địch để khai hỏa.", UiTokens.TEXT_PRIMARY);
                }
            }
            return;
        }
        if (hyperCannonArmed && superShipActive) {
            List<int[]> cells = cells3x3(row, col);
            computerBoardPanel.setTargetPreview(cells, new Color(0xFDE047));
            if (!turnLocked && !gameOver) {
                showMessage(coordinate(row, col) + " · ★ ĐẠI PHÁO HYPERION 3×3 · 0 Mana + Choáng 1 lượt!",
                        new Color(0xFDE047));
            }
            return;
        }
        List<int[]> cells = targetCells(row, col);
        computerBoardPanel.setTargetPreview(cells, selectedMagic.accentColor);
        int cost = currentManaCost();
        if (sandboxMode) {
            if (!turnLocked && !gameOver) {
                showMessage(coordinate(row, col) + " · " + selectedWeapon.label + " + " + selectedMagic.label
                        + " · Sandbox ∞", UiTokens.TEXT_PRIMARY);
            }
        } else if (cost > mana) {
            showMessage("Không đủ Hải Năng (cần " + cost + ", hiện có " + mana + ").", UiTokens.STATUS_DANGER);
        } else if (!turnLocked && !gameOver) {
            showMessage(coordinate(row, col) + " · " + selectedWeapon.label + " + " + selectedMagic.label
                    + " · " + cost + " Mana", UiTokens.TEXT_PRIMARY);
        }
    }

    private List<int[]> cells3x3(int row, int col) {
        List<int[]> cells = new ArrayList<>(9);
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                addCell(cells, row + dr, col + dc);
            }
        }
        return cells;
    }

    private List<int[]> targetCells(int row, int col) {
        List<int[]> cells = new ArrayList<>();
        switch (selectedWeapon) {
            case CANNON -> {
                cells.add(new int[]{row, col});
                if (superShipActive && (titanBattleMode || sandboxMode)) {
                    int adjCol = col + 1 < Board.SIZE ? col + 1 : col - 1;
                    addCell(cells, row, adjCol);
                }
            }
            case TORPEDO -> {
                for (int offset = -1; offset <= 1; offset++) addCell(cells,
                        row + (torpedoHorizontal ? 0 : offset), col + (torpedoHorizontal ? offset : 0));
            }
            case MISSILE_CROSS -> {
                addCell(cells, row, col);
                addCell(cells, row - 1, col);
                addCell(cells, row + 1, col);
                addCell(cells, row, col - 1);
                addCell(cells, row, col + 1);
            }
            case AIRSTRIKE -> {
                for (int targetRow = row - 1; targetRow <= row + 1; targetRow++) {
                    for (int targetCol = col - 1; targetCol <= col + 1; targetCol++) addCell(cells, targetRow, targetCol);
                }
            }
        }
        return cells;
    }

    private static void addCell(List<int[]> cells, int row, int col) {
        if (row >= 0 && row < Board.SIZE && col >= 0 && col < Board.SIZE) cells.add(new int[]{row, col});
    }

    private void fireAt(int row, int col) {
        if (gameOver || turnLocked || superShipCutscenePlaying) return;
        if (hyperCannonArmed && superShipActive) {
            executeHyperionCannon(row, col);
            return;
        }
        if (titanBattleMode || (sandboxMode && ZombieMode.isTitanMode())) {
            executeTitanPlayerStrike(row, col);
            return;
        }
        if (sandboxMode) {
            executeSandboxStrike(row, col);
            return;
        }
        int cost = currentManaCost();
        if (cost > mana) {
            showMessage("Không đủ Hải Năng (cần " + cost + ", hiện có " + mana + ").", UiTokens.STATUS_DANGER);
            return;
        }
        if (selectedWeapon == CombatLoadout.WeaponOption.CANNON && selectedMagic == CombatLoadout.MagicOption.NONE) {
            executeCannonShot(row, col);
        } else {
            executeComboStrike(row, col);
        }
    }

    private void executeTitanPlayerStrike(int row, int col) {
        if (titan == null) return;
        int cost = currentManaCost();
        if (!sandboxMode && cost > mana) {
            showMessage("Không đủ Hải Năng (cần " + cost + ", hiện có " + mana + ").", UiTokens.STATUS_DANGER);
            return;
        }
        CombatLoadout.WeaponOption weapon = selectedWeapon;
        CombatLoadout.MagicOption magic = selectedMagic;
        List<int[]> cells = targetCells(row, col);
        TitanBattleController.PlayerStrikeOutcome outcome = titan.fireAtTitan(row, col, cells, magic);
        if (outcome.aggregateResult() == ShotResult.ALREADY_SHOT) {
            showMessage("Ô/vùng " + coordinate(row, col) + " đã bắn rồi — hãy chọn vùng khác trên Boss!",
                    UiTokens.STATUS_WARNING);
            return;
        }
        turnLocked = true;
        if (sandboxMode) {
            mana = CombatLoadout.MAX_MANA;
        } else {
            mana = Math.max(0, mana - cost);
        }
        refreshLoadout();
        computerBoardPanel.clearTargetPreview();
        boolean anyHit = !outcome.hitCells().isEmpty();
        Color msgCol = magic != CombatLoadout.MagicOption.NONE ? magic.accentColor : titan.getBoss().accentColor;
        showMessage("Khai hỏa " + weapon.label + " + " + magic.label + " vào " + titan.getBoss().shortLabel
                + " (" + coordinate(row, col) + ")...", msgCol);
        SoundEngine.playWeaponMagicLaunch(weapon, magic);

        fxOverlay.launchStrike(playerBoardPanel, computerBoardPanel, row, col, cells, weapon, magic, anyHit, () -> {
            SoundEngine.playImpact(outcome.aggregateResult(), false);
            computerBoardPanel.setLastShot(row, col);
            if (!outcome.hitCells().isEmpty()) {
                computerBoardPanel.playEffects(
                        magic == CombatLoadout.MagicOption.NONE ? SpriteAssets.FxType.HIT : resolveFxForMagic(magic, ShotResult.HIT),
                        outcome.hitCells(),
                        magic == CombatLoadout.MagicOption.NONE ? UiTokens.HIT_CRIMSON : magic.accentColor,
                        42L);
                if (weapon == CombatLoadout.WeaponOption.AIRSTRIKE) {
                    computerBoardPanel.addImpactZone(row, col, cells, magic, null, false);
                }
                computerBoardPanel.triggerTitanHitFlash(!outcome.weakpointHits().isEmpty()
                        ? new Color(0xFDE047) : UiTokens.HIT_CRIMSON);
                computerBoardPanel.triggerShake(!outcome.weakpointHits().isEmpty() ? 7 : 4);
            } else {
                computerBoardPanel.playEffects(SpriteAssets.FxType.MISS, outcome.missCells(), UiTokens.STEEL_EDGE, 36L);
                if (magic != CombatLoadout.MagicOption.NONE) {
                    triggerHybridImpact(computerBoardPanel, row, col, cells, weapon, magic, outcome.aggregateResult());
                }
            }
            if (outcome.burnCell() != null) {
                computerBoardPanel.playEffect(SpriteAssets.FxType.FIRE, outcome.burnCell()[0], outcome.burnCell()[1],
                        UiTokens.MAGIC_FIRE, 180L);
            }
            if (!outcome.sonarCells().isEmpty()) {
                mergeRevealedCells(outcome.sonarCells());
                computerBoardPanel.setRevealedCells(revealedEnemyCells);
            }
            computerBoardPanel.updateTitanState(titan.getRemainingHp(), titan.getMaxHp(), titan.isEnraged());
            computerBoardPanel.setTitle(titan.isEnraged()
                    ? ("THỨC TỈNH: " + titan.getBoss().displayName)
                    : ("Boss Titan: " + titan.getBoss().displayName));
            if (!sandboxMode) {
                int cap = currentMaxMana();
                int regen = superShipActive ? (anyHit ? 3 : 2) : (anyHit ? 2 : 1);
                mana = Math.min(cap, mana + regen);
                if (anyHit) {
                    int spiritGain = Math.min(28, 12 + outcome.weakpointHits().size() * 10 + Math.max(0, outcome.hitCells().size() - 1) * 4);
                    gainFleetSpirit(spiritGain);
                }
            }
            if (superShipActive && hyperCannonCooldown > 0) hyperCannonCooldown--;
            computerBoardPanel.refresh();
            refreshLoadout();
            refreshShots();
            updateSuperShipButtonUi();

            if (outcome.bossDefeated()) {
                finishTitanBattle(true);
                return;
            }
            if (outcome.enragedJustTriggered()) {
                SoundEngine.playTitanEnrageTransform(titan.getBoss());
                computerBoardPanel.triggerTitanEmergence(1350L);
                computerBoardPanel.triggerTitanCharge(1550L);
                computerBoardPanel.triggerShake(9);
                fxOverlay.launchTitanEnrageCutIn(computerBoardPanel, titan.getBoss());
                applyTitanEnvironmentImpact(TitanMode.environmentForEnrage(titan.getBoss()), false);
            }

            StringBuilder st = new StringBuilder();
            if (outcome.enragedJustTriggered()) {
                st.append("PHASE 2 CUỒNG BẠO! ").append(titan.getBoss().displayName).append(" MỞ KHÓA 5 TUYỆT KỸ! ");
            } else if (!outcome.weakpointHits().isEmpty()) {
                st.append("CHÍ MẠNG ĐIỂM YẾU! (-").append(outcome.hitCells().size()).append(" HP Boss) ");
            } else if (anyHit) {
                st.append("Trúng Boss -").append(outcome.hitCells().size()).append(" HP! ");
            } else {
                st.append("Bắn trượt Boss! ");
            }

            if (magic == CombatLoadout.MagicOption.FROST && anyHit) {
                enemyFrozenTurns = 1;
            }
            if (magic == CombatLoadout.MagicOption.LIGHTNING && anyHit) {
                showMessage(st.toString().trim() + " Sét choáng — bạn được bắn tiếp!", UiTokens.MAGIC_LIGHTNING);
                turnLocked = false;
                return;
            }
            if (enemyFrozenTurns > 0) {
                enemyFrozenTurns--;
                showMessage(st.toString().trim() + " Boss bị Băng Phong 1 lượt!", UiTokens.MAGIC_FROST);
                turnLocked = false;
                return;
            }
            if (!sandboxMode || sandboxEnemyCounter) {
                Color statusCol = outcome.enragedJustTriggered()
                        ? titan.getBoss().enrageColor
                        : (anyHit ? UiTokens.STATUS_SUCCESS : UiTokens.TEXT_PRIMARY);
                showMessage(st.toString().trim() + " Boss chuẩn bị tung chiêu!", statusCol);
                int counterDelay = outcome.enragedJustTriggered() ? 1680 : 520;
                Timer delay = new Timer(counterDelay, e -> takeTitanCounterTurn(null));
                delay.setRepeats(false);
                delay.start();
            } else {
                showMessage(st.toString().trim(), anyHit ? UiTokens.STATUS_SUCCESS : UiTokens.TEXT_PRIMARY);
                turnLocked = false;
            }
        });
    }

    private void executeSandboxStrike(int row, int col) {
        CombatLoadout.WeaponOption weapon = selectedWeapon;
        CombatLoadout.MagicOption magic = selectedMagic;
        List<int[]> cells = targetCells(row, col);
        SandboxController.StrikeOutcome outcome = sandbox.fireStrike(true, row, col, cells, magic);
        if (outcome.aggregateResult() == ShotResult.ALREADY_SHOT) {
            showMessage("Ô/vùng " + coordinate(row, col) + " đã bắn rồi — hãy chọn mục tiêu khác hoặc bấm Reset Sandbox.",
                    UiTokens.STATUS_WARNING);
            return;
        }
        turnLocked = true;
        mana = currentMaxMana();
        if (superShipActive && hyperCannonCooldown > 0) hyperCannonCooldown--;
        computerBoardPanel.clearTargetPreview();
        boolean anyHit = !outcome.hitCells().isEmpty();
        Color msgColor = magic != CombatLoadout.MagicOption.NONE ? magic.accentColor : UiTokens.ACCENT_SIGNAL;
        showMessage("Sandbox khai hỏa " + weapon.label + " + " + magic.label + " vào " + coordinate(row, col) + "...",
                msgColor);
        SoundEngine.playWeaponMagicLaunch(weapon, magic);

        fxOverlay.launchStrike(playerBoardPanel, computerBoardPanel, row, col, cells, weapon, magic, anyHit, () -> {
            applySandboxPlayerImpact(row, col, cells, weapon, magic, outcome);
            afterSandboxPlayerStrike(magic, outcome, anyHit);
        });
    }

    private void applySandboxPlayerImpact(
            int row,
            int col,
            List<int[]> cells,
            CombatLoadout.WeaponOption weapon,
            CombatLoadout.MagicOption magic,
            SandboxController.StrikeOutcome outcome
    ) {
        SoundEngine.playImpact(outcome.aggregateResult(), false);
        computerBoardPanel.setLastShot(row, col);

        if (weapon == CombatLoadout.WeaponOption.CANNON && magic == CombatLoadout.MagicOption.NONE) {
            computerBoardPanel.playEffect(
                    outcome.aggregateResult() == ShotResult.MISS ? SpriteAssets.FxType.MISS : SpriteAssets.FxType.HIT,
                    row, col, outcome.aggregateResult() == ShotResult.MISS ? UiTokens.STEEL_EDGE : UiTokens.HIT_CRIMSON, 0L);
            if (!outcome.hitCells().isEmpty()) {
                computerBoardPanel.triggerShake(outcome.aggregateResult() == ShotResult.SUNK ? 6 : 4);
            }
        } else if (magic == CombatLoadout.MagicOption.NONE) {
            if (!outcome.hitCells().isEmpty()) {
                computerBoardPanel.playEffects(SpriteAssets.FxType.HIT, outcome.hitCells(), UiTokens.HIT_CRIMSON, 45L);
            }
            if (!outcome.missCells().isEmpty()) {
                computerBoardPanel.playEffects(SpriteAssets.FxType.MISS, outcome.missCells(), UiTokens.STEEL_EDGE, 40L);
            }
            if (weapon == CombatLoadout.WeaponOption.AIRSTRIKE) {
                computerBoardPanel.addImpactZone(row, col, cells, CombatLoadout.MagicOption.NONE, null, false);
            }
            int shake = switch (weapon) {
                case TORPEDO -> outcome.hitCells().isEmpty() ? 2 : 5;
                case MISSILE_CROSS -> outcome.hitCells().isEmpty() ? 3 : 6;
                case AIRSTRIKE -> 7;
                case CANNON -> 4;
            };
            computerBoardPanel.triggerShake(shake);
        } else {
            triggerHybridImpact(computerBoardPanel, row, col, cells, weapon, magic, outcome.aggregateResult());
            if (!outcome.hitCells().isEmpty()) {
                computerBoardPanel.playEffects(SpriteAssets.FxType.HIT, outcome.hitCells(), UiTokens.HIT_CRIMSON, 45L);
            }
        }

        if (outcome.burnCell() != null) {
            int br = outcome.burnCell()[0];
            int bc = outcome.burnCell()[1];
            computerBoardPanel.playEffect(SpriteAssets.FxType.FIRE, br, bc, UiTokens.MAGIC_FIRE, 180L);
            computerBoardPanel.playEffect(SpriteAssets.FxType.HIT, br, bc, UiTokens.HIT_CRIMSON, 230L);
        }
        if (!outcome.sonarRevealedCells().isEmpty()) {
            mergeRevealedCells(outcome.sonarRevealedCells());
            computerBoardPanel.setRevealedCells(revealedEnemyCells);
        }
        computerBoardPanel.refresh();
        refreshLoadout();
        refreshShots();
    }

    private void afterSandboxPlayerStrike(
            CombatLoadout.MagicOption magic,
            SandboxController.StrikeOutcome outcome,
            boolean anyHit
    ) {
        if (sandbox.areAllSunk(true)) {
            finishSandboxGame(true);
            return;
        }
        StringBuilder status = new StringBuilder();
        if (!outcome.newlySunkShips().isEmpty()) {
            status.append("ĐÁNH CHÌM ");
            for (int i = 0; i < outcome.newlySunkShips().size(); i++) {
                if (i > 0) status.append(", ");
                status.append(outcome.newlySunkShips().get(i).displayName);
            }
            status.append("! ");
        } else if (anyHit) {
            status.append("Trúng ").append(outcome.hitCells().size()).append(" ô tàu địch! ");
        } else {
            status.append("Bắn trượt xuống biển. ");
        }
        if (outcome.burnCell() != null) {
            status.append("Lửa cháy lan thêm ").append(coordinate(outcome.burnCell()[0], outcome.burnCell()[1])).append("! ");
        }
        if (!outcome.sonarRevealedCells().isEmpty()) {
            status.append("Cổ Ngữ quét lộ ").append(outcome.sonarRevealedCells().size()).append(" ô tàu! ");
        }
        if (magic == CombatLoadout.MagicOption.FROST && anyHit) {
            enemyFrozenTurns = 1;
            status.append("Băng đóng băng lượt phản công địch! ");
        }

        if (magic == CombatLoadout.MagicOption.LIGHTNING && anyHit) {
            status.append("Sét choáng — bạn được bắn tiếp ngay!");
            showMessage(status.toString().trim(), UiTokens.MAGIC_LIGHTNING);
            turnLocked = false;
            return;
        }
        if (enemyFrozenTurns > 0) {
            enemyFrozenTurns--;
            showMessage(status.toString().trim() + " (Địch đang bị đóng băng)", UiTokens.MAGIC_FROST);
            turnLocked = false;
            return;
        }
        if (sandboxEnemyCounter) {
            showMessage(status.toString().trim() + " Đang chờ địch phản công...",
                    anyHit ? UiTokens.STATUS_SUCCESS : UiTokens.TEXT_PRIMARY);
            Timer delay = new Timer(460, e -> takeSandboxCounterTurn(null));
            delay.setRepeats(false);
            delay.start();
        } else {
            showMessage(status.toString().trim(), anyHit ? UiTokens.STATUS_SUCCESS : UiTokens.TEXT_PRIMARY);
            turnLocked = false;
        }
    }

    private void mergeRevealedCells(List<int[]> newCells) {
        for (int[] nc : newCells) {
            boolean exists = false;
            for (int[] ex : revealedEnemyCells) {
                if (ex[0] == nc[0] && ex[1] == nc[1]) {
                    exists = true;
                    break;
                }
            }
            if (!exists) revealedEnemyCells.add(new int[]{nc[0], nc[1]});
        }
    }

    private void toggleSandboxPlayerShieldCell(int row, int col) {
        if (!sandboxMode || row < 0 || col < 0) return;
        List<int[]> updated = new ArrayList<>(playerBoardPanel.getShieldedCells());
        int removedIdx = -1;
        for (int i = 0; i < updated.size(); i++) {
            int[] c = updated.get(i);
            if (c[0] == row && c[1] == col) {
                removedIdx = i;
                break;
            }
        }
        boolean enabledNow;
        if (removedIdx >= 0) {
            updated.remove(removedIdx);
            enabledNow = false;
        } else {
            updated.add(new int[]{row, col});
            enabledNow = true;
        }
        playerBoardPanel.setFullMapShield(false, updated);
        shieldPreviewEnabled = !updated.isEmpty();
        shieldShatteredState = false;
        updateShieldButtonUi();
        SoundEngine.playUiClick();
        showMessage((enabledNow ? "Đã bật Khiên 3D tại " : "Đã tắt Khiên 3D tại ")
                + coordinate(row, col) + " (đang bảo vệ " + updated.size() + " ô).", UiTokens.SHIELD_TINT);
    }

    private void toggleSandboxRevealEnemy() {
        sandboxRevealEnemy = !sandboxRevealEnemy;
        SoundEngine.playUiClick();
        computerBoardPanel.setShowShips(sandboxRevealEnemy);
        if (sandboxRevealBtn != null) {
            sandboxRevealBtn.setText(sandboxRevealEnemy ? "Soi Địch: Hiện" : "Soi Địch: Ẩn");
        }
        if (ZombieMode.isTitanMode() && titan != null) {
            computerBoardPanel.setRevealedCells(sandboxRevealEnemy ? titan.listIntactBossCells() : List.of());
        }
        showMessage(sandboxRevealEnemy
                        ? (ZombieMode.isTitanMode()
                                ? "Đã HIỆN toàn bộ 20 ô cơ thể Boss Titan trên hải đồ!"
                                : "Đã HIỆN toàn bộ hạm đội địch trên hải đồ.")
                        : "Đã ẨN mục tiêu địch còn sống.",
                UiTokens.ACCENT_SIGNAL);
    }

    private void toggleSandboxEnemyCounter() {
        sandboxEnemyCounter = !sandboxEnemyCounter;
        SoundEngine.playUiClick();
        if (sandboxCounterBtn != null) {
            sandboxCounterBtn.setText(sandboxEnemyCounter ? "Địch Phản: Bật" : "Địch Phản: Tắt");
        }
        showMessage(sandboxEnemyCounter
                        ? "Địch tự động phản công sau mỗi lượt bạn bắn."
                        : "Đã TẮT tự động phản công — bạn có thể bắn liên tục để test kỹ năng.",
                UiTokens.ACCENT_SIGNAL);
    }

    private void triggerSandboxImmediateCounter() {
        if (!sandboxMode || turnLocked || gameOver) return;
        turnLocked = true;
        if (ZombieMode.isTitanMode()) {
            takeTitanCounterTurn(selectedTitanSkill);
        } else {
            takeSandboxCounterTurn(ZombieMode.isZombieMode() ? selectedZombie : null);
        }
    }

    public void triggerTitanBossEntrance(TitanMode.TitanBoss boss) {
        TitanMode.TitanBoss chosen = boss != null
                ? boss
                : (titan != null ? titan.getBoss() : TitanMode.getActiveBoss());
        titanIntroPendingInitial = false;
        if (!gameOver) {
            turnLocked = true;
        }
        SoundEngine.playTitanBossIntro(chosen);
        fxOverlay.launchBossCityRampageCutscene(computerBoardPanel, chosen, () -> {
            applyTitanEnvironmentImpact(TitanMode.environmentForEntrance(chosen), true);
            if (!gameOver && !superShipCutscenePlaying) {
                turnLocked = false;
                updateSuperShipButtonUi();
            }
        });
    }

    private void applyTitanEnvironmentImpact(TitanMode.EnvironmentAction env, boolean announceHud) {
        if (env == null) return;
        boolean envChanged = false;
        if (env.weather() != null && OceanMapBackdrop.getWeatherMode() != env.weather()) {
            OceanMapBackdrop.setWeatherMode(env.weather());
            envChanged = true;
        }
        if (env.season() != null && OceanMapBackdrop.getSeason() != env.season()) {
            OceanMapBackdrop.setSeason(env.season());
            envChanged = true;
        }
        if (env.mapTheme() != null && OceanMapBackdrop.getMapTheme() != env.mapTheme()) {
            OceanMapBackdrop.setMapTheme(env.mapTheme());
            SoundEngine.refreshThemeMusic();
            envChanged = true;
        }
        OceanMapBackdrop.triggerTitanEnvironmentSurge(
                env.windX(), env.windY(), env.waterTint(), env.waveIntensity(), env.durationMs());
        playerBoardPanel.triggerWaterBlow(env.windX(), env.windY(), env.waterTint(), env.waveIntensity(), env.durationMs());
        computerBoardPanel.triggerWaterBlow(env.windX(), env.windY(), env.waterTint(), env.waveIntensity(), env.durationMs());
        if (env.quakePx() > 0) {
            playerBoardPanel.triggerShake(Math.max(4, (int) Math.round(env.quakePx() * 0.65)));
            computerBoardPanel.triggerShake(env.quakePx());
        }
        fxOverlay.triggerEnvironmentWave(env.waterTint(), env.windX(), env.windY(), env.durationMs());
        SoundEngine.playEnvironmentShift(env.weather() == OceanMapBackdrop.WeatherMode.STORM || env.waveIntensity() >= 3);
        if (envChanged) {
            syncEnvironmentControls();
            applyGameMode(false);
        } else {
            playerBoardPanel.repaint();
            computerBoardPanel.repaint();
        }
        if (announceHud && env.hudMessage() != null && !env.hudMessage().isBlank()) {
            showMessage(env.hudMessage(), env.waterTint());
        }
    }

    private void switchActiveTitanBoss(TitanMode.TitanBoss boss) {
        TitanMode.setActiveBoss(boss);
        List<TitanMode.TitanSkill> arsenal = TitanMode.skillsFor(boss);
        if (!arsenal.isEmpty()) selectedTitanSkill = arsenal.get(0);
        if (titan != null) {
            if (gameOver) {
                titan.reset(boss);
                if (sandbox != null) sandbox.reset();
                gameOver = false;
                turnLocked = false;
                shieldPreviewEnabled = false;
                shieldShatteredState = false;
                fleetSpirit = sandboxMode ? 100 : 0;
                superShipActive = false;
                superShipCutscenePlaying = false;
                superShipReadyAnnounced = false;
                hyperCannonArmed = false;
                hyperCannonCooldown = 0;
                mana = sandboxMode || titanBattleMode ? CombatLoadout.MAX_MANA : CombatLoadout.STARTING_MANA;
                updateShieldButtonUi();
                updateSuperShipButtonUi();
                playerBoardPanel.clearAllVisualState();
                computerBoardPanel.setInteractive(true);
            } else {
                titan.switchBossOnly(boss);
            }
            computerBoardPanel.clearAllVisualState();
            computerBoardPanel.setCellStateOverride(titan.getBossGrid());
        }
        syncEnvironmentControls();
        applyGameMode(true);
        SoundEngine.refreshThemeMusic();
        refreshLoadout();
        refreshShots();
        showMessage("XUẤT HIỆN BOSS TITAN: " + boss.displayName + " — Sẵn sàng nghênh chiến!", boss.accentColor);
        triggerTitanBossEntrance(boss);
    }

    private void triggerTitanSkillNow(TitanMode.TitanSkill skill) {
        if (skill == null || turnLocked || gameOver) return;
        if (titan == null) {
            int r = 4;
            int c = 4;
            List<int[]> cells = TitanMode.skillTargetCells(skill, r, c, true);
            List<int[]> demoHits = new ArrayList<>();
            List<int[]> demoBlocked = new ArrayList<>();
            for (int[] cell : cells) {
                if (playerBoardPanel.isCellShielded(cell[0], cell[1])) demoBlocked.add(cell);
                else demoHits.add(cell);
            }
            SoundEngine.playTitanSkill(skill.boss, skill);
            TitanMode.EnvironmentAction env = TitanMode.environmentForSkill(skill);
            applyTitanEnvironmentImpact(env, true);
            fxOverlay.launchTitanSkill(computerBoardPanel, playerBoardPanel, skill, false, r, c, cells, !demoHits.isEmpty(), () -> {
                applyTitanSkillVisuals(skill, r, c, cells, demoHits, List.of(), demoBlocked);
            });
            return;
        }
        turnLocked = true;
        takeTitanCounterTurn(skill);
    }

    private void takeTitanCounterTurn(TitanMode.TitanSkill forcedSkill) {
        if (titan == null || gameOver) {
            turnLocked = false;
            return;
        }
        List<int[]> effectiveShields = (playerBoardPanel.isFullMapShield() || shieldPreviewEnabled)
                ? allBoardCells()
                : playerBoardPanel.getShieldedCells();
        TitanBattleController.TitanSkillOutcome out = titan.castTitanSkill(
                forcedSkill, effectiveShields);
        TitanMode.TitanSkill skill = out.skill();
        List<int[]> cells = out.targetCells();
        List<int[]> hitCells = new ArrayList<>(out.hitCells());
        List<int[]> missCells = new ArrayList<>(out.missCells());
        List<int[]> blockedCells = new ArrayList<>(out.shieldBlockedCells());
        List<int[]> armorCells = sandboxMode ? new ArrayList<>() : new ArrayList<>(titan.consumeLastArmorAbsorbedCells());
        if (sandboxMode && sandbox != null) {
            hitCells.clear();
            missCells.clear();
            for (int[] c : cells) {
                if (playerBoardPanel.isCellShielded(c[0], c[1])) continue;
                if (sandbox.consumePlayerArmor(c[0], c[1])) {
                    armorCells.add(c);
                    continue;
                }
                ShotResult res = sandbox.fireCell(false, c[0], c[1]);
                if (res == ShotResult.HIT || res == ShotResult.SUNK) hitCells.add(c);
                else if (res == ShotResult.MISS) missCells.add(c);
            }
        }
        selectedTitanSkill = skill;
        refreshLoadout();
        showMessage(skill.boss.shortLabel + " gầm vang — tung tuyệt kỹ " + skill.skillName + " vào "
                + coordinate(out.targetRow(), out.targetCol()) + "!", skill.accentColor);
        SoundEngine.playTitanSkill(skill.boss, skill);
        TitanMode.EnvironmentAction skillEnv = TitanMode.environmentForSkill(skill);
        applyTitanEnvironmentImpact(skillEnv, false);

        boolean anyHit = !hitCells.isEmpty();
        final List<int[]> absorbedArmorCells = List.copyOf(armorCells);
        fxOverlay.launchTitanSkill(
                computerBoardPanel, playerBoardPanel, skill, out.enraged(),
                out.targetRow(), out.targetCol(), cells, anyHit, () -> {
                    applyTitanSkillVisuals(skill, out.targetRow(), out.targetCol(), cells, hitCells, missCells, blockedCells);
                    if (!absorbedArmorCells.isEmpty()) {
                        playerBoardPanel.playEffects(SpriteAssets.FxType.SONAR, absorbedArmorCells, new Color(0xFACC15), 32L);
                        playerBoardPanel.triggerShake(3);
                        if (!anyHit && blockedCells.isEmpty()) {
                            SoundEngine.playImpact(ShotResult.HIT, false);
                            showMessage("Giáp Vàng Nanite Siêu Tàu chặn đứng " + absorbedArmorCells.size()
                                    + " đòn của " + skill.boss.shortLabel + "!", new Color(0xFDE047));
                        }
                    }
                    playerBoardPanel.setSuperArmorGrid(sandboxMode && sandbox != null
                            ? sandbox.getPlayerSuperArmor()
                            : titan.getSuperArmorGrid());
                    if (!blockedCells.isEmpty()) {
                        gainFleetSpirit(12 * Math.min(2, blockedCells.size()));
                    }
                    if (anyHit || !out.newlySunkShips().isEmpty()) {
                        gainFleetSpirit(hitCells.size() * 10 + out.newlySunkShips().size() * 20);
                    }
                    playerBoardPanel.refresh();
                    refreshShots();
                    checkSuperShipReadyCue();
                    boolean playerLost = sandboxMode
                            ? (sandbox != null && sandbox.areAllSunk(false))
                            : out.playerDefeated();
                    if (playerLost) {
                        if (sandboxMode) finishSandboxGame(false);
                        else finishTitanBattle(false);
                    } else {
                        turnLocked = false;
                    }
                });
    }

    private void applyTitanSkillVisuals(
            TitanMode.TitanSkill skill,
            int targetRow,
            int targetCol,
            List<int[]> allCells,
            List<int[]> hitCells,
            List<int[]> missCells,
            List<int[]> blockedCells
    ) {
        boolean anyBlocked = !blockedCells.isEmpty();
        boolean anyHit = !hitCells.isEmpty();
        playerBoardPanel.setLastShot(targetRow, targetCol);
        SpriteAssets.FxType fx = switch (skill) {
            case GZ_ATOMIC_BREATH, MT_WING_GALE, MT_GOD_RAY -> SpriteAssets.FxType.SONAR;
            case MT_SILK_WEB -> SpriteAssets.FxType.FROST;
            case GZ_THERMO_PULSE, GZ_SPIRAL_RAY, GZ_MELTDOWN_RIFT, MT_SOLAR_SUPERNOVA -> SpriteAssets.FxType.FIRE;
            case KG_TRIPLE_GRAVITY, KG_HURRICANE_WING, KG_OMEGA_VORTEX, KG_ALPHA_STORM, KG_CATACLYSM_GRAVITY, MT_PRISM_RAIN -> SpriteAssets.FxType.LIGHTNING;
            default -> SpriteAssets.FxType.HIT;
        };
        if (!hitCells.isEmpty()) {
            playerBoardPanel.playEffects(fx, hitCells, skill.accentColor, 42L);
            playerBoardPanel.playEffects(SpriteAssets.FxType.HIT, hitCells, UiTokens.HIT_CRIMSON, 64L);
        }
        if (!missCells.isEmpty()) {
            playerBoardPanel.playEffects(fx, missCells, skill.accentColor, 40L);
        }
        if (skill.leavesImpactZone && !(anyBlocked && !anyHit)) {
            playerBoardPanel.addImpactZone(
                    targetRow, targetCol, allCells,
                    TitanMode.impactZoneMagic(skill), skill.accentColor, true);
        }
        playerBoardPanel.triggerShake(skill.phase2Only ? 8 : (skill.leavesImpactZone ? 7 : 5));

        if (anyBlocked) {
            BoardPanel.ShieldAbsorptionResult sRes = handleShieldImpact(
                    targetRow, targetCol, skill.shieldDamage, skill.accentColor, skill.skillName);
            for (int[] bc : blockedCells) {
                if (bc[0] != targetRow || bc[1] != targetCol) {
                    playerBoardPanel.triggerShieldBlock(bc[0], bc[1], skill.shieldDamage >= 45 ? 3 : 2);
                }
            }
            if (!anyHit) return;
            if (!sRes.shattered()) {
                showMessage(skill.boss.shortLabel + " (" + skill.skillName + ") bị Khiên chắn "
                        + blockedCells.size() + " ô (còn " + sRes.remainingEnergy() + "% NL)!", skill.accentColor);
            }
            return;
        }
        SoundEngine.playImpact(anyHit ? ShotResult.HIT : ShotResult.MISS, false);
        if (anyHit) {
            showMessage(skill.boss.shortLabel + " đánh trúng " + hitCells.size() + " ô bằng " + skill.skillName
                    + (skill.leavesImpactZone ? " (+ Vùng Tác Động)!" : "!"), skill.accentColor);
        } else {
            showMessage(skill.boss.shortLabel + " quét trượt bằng " + skill.skillName + " tại "
                    + coordinate(targetRow, targetCol) + ".", skill.accentColor);
        }
    }

    private void finishTitanBattle(boolean won) {
        gameOver = true;
        turnLocked = true;
        computerBoardPanel.setInteractive(false);
        computerBoardPanel.clearTargetPreview();
        playerBoardPanel.refresh();
        computerBoardPanel.refresh();
        refreshShots();
        SoundEngine.playGameOver(won);
        TitanMode.TitanBoss b = titan != null ? titan.getBoss() : TitanMode.getActiveBoss();
        if (won) {
            showMessage("CHIẾN THẮNG! ĐÃ HẠ GỤC BOSS TITAN " + b.displayName.toUpperCase() + "!",
                    UiTokens.STATUS_SUCCESS);
        } else {
            showMessage("THẤT BẠI — Hạm đội bạn đã bị " + b.shortLabel + " tiêu diệt! Bấm 'Ván mới' hoặc đổi Boss để đấu lại.",
                    UiTokens.STATUS_DANGER);
        }
    }

    private void executeSandboxZombieBossTurn(ZombieMode.ZombieClass boss) {
        if (!sandboxMode || turnLocked || gameOver) return;
        turnLocked = true;
        takeSandboxCounterTurn(boss);
    }

    private void takeSandboxCounterTurn(ZombieMode.ZombieClass forcedBoss) {
        if (!sandboxMode || gameOver) {
            turnLocked = false;
            return;
        }
        int[] target = sandbox.pickAiTarget();
        if (target == null) {
            finishSandboxGame(false);
            return;
        }
        int tr = target[0];
        int tc = target[1];
        if (ZombieMode.isZombieMode()) {
            ZombieMode.ZombieClass[] classes = ZombieMode.ZombieClass.values();
            ZombieMode.ZombieClass zc = forcedBoss != null
                    ? forcedBoss
                    : classes[Math.max(0, sandbox.countShots(false) % classes.length)];
            selectedZombie = zc;
            refreshLoadout();
            List<int[]> cells = ZombieMode.skillTargetCells(zc, tr, tc, true);
            boolean centerShielded = playerBoardPanel.isCellShielded(tr, tc);
            showMessage(zc.title + " tung chiêu " + zc.skillName + " vào " + coordinate(tr, tc) + "...", zc.accentColor);
            SoundEngine.playZombieLaunch();
            fxOverlay.launchZombieStrike(computerBoardPanel, playerBoardPanel, zc, tr, tc, cells, !centerShielded, () -> {
                resolveSandboxZombieImpact(zc, tr, tc, cells);
            });
        } else {
            boolean shielded = playerBoardPanel.isCellShielded(tr, tc);
            boolean armored = !shielded && sandbox.consumePlayerArmor(tr, tc);
            ShotResult res = (shielded || armored) ? ShotResult.HIT : sandbox.fireCell(false, tr, tc);
            boolean hit = !shielded && !armored && (res == ShotResult.HIT || res == ShotResult.SUNK);
            showMessage("Địch đang phản công về " + coordinate(tr, tc) + "...", UiTokens.STATUS_WARNING);
            SoundEngine.playWeaponMagicLaunch(CombatLoadout.WeaponOption.CANNON, CombatLoadout.MagicOption.NONE);
            fxOverlay.launchStrike(computerBoardPanel, playerBoardPanel, tr, tc,
                    CombatLoadout.WeaponOption.CANNON, CombatLoadout.MagicOption.NONE, hit, () -> {
                        playerBoardPanel.setLastShot(tr, tc);
                        if (shielded) {
                            handleShieldImpact(tr, tc, 20, UiTokens.ACCENT_SIGNAL, "Pháo địch tại " + coordinate(tr, tc));
                        } else if (armored) {
                            SoundEngine.playImpact(ShotResult.HIT, false);
                            playerBoardPanel.playEffect(SpriteAssets.FxType.SONAR, tr, tc, new Color(0xFACC15), 0L);
                            playerBoardPanel.triggerShake(3);
                            playerBoardPanel.setSuperArmorGrid(sandbox.getPlayerSuperArmor());
                            showMessage("Giáp Vàng Nanite Siêu Tàu chặn đòn tại " + coordinate(tr, tc) + "!", new Color(0xFDE047));
                        } else {
                            SoundEngine.playImpact(res, false);
                            playerBoardPanel.playEffect(res == ShotResult.MISS ? SpriteAssets.FxType.MISS : SpriteAssets.FxType.HIT,
                                    tr, tc, hit ? UiTokens.HIT_CRIMSON : UiTokens.STEEL_EDGE, 0L);
                            if (hit) playerBoardPanel.triggerShake(res == ShotResult.SUNK ? 6 : 4);
                            showMessage("Địch bắn " + coordinate(tr, tc) + ": " + resultText(res) + ".", UiTokens.TEXT_PRIMARY);
                        }
                        playerBoardPanel.refresh();
                        refreshShots();
                        if (sandbox.areAllSunk(false)) {
                            finishSandboxGame(false);
                        } else {
                            turnLocked = false;
                        }
                    });
        }
    }

    private void resolveSandboxZombieImpact(ZombieMode.ZombieClass zc, int tr, int tc, List<int[]> cells) {
        List<int[]> hitCells = new ArrayList<>();
        List<int[]> missCells = new ArrayList<>();
        List<int[]> blockedCells = new ArrayList<>();
        List<int[]> armorCells = new ArrayList<>();
        ShotResult worst = ShotResult.MISS;
        for (int[] c : cells) {
            int r = c[0], col = c[1];
            if (playerBoardPanel.isCellShielded(r, col)) {
                blockedCells.add(c);
                continue;
            }
            if (sandbox.consumePlayerArmor(r, col)) {
                armorCells.add(c);
                continue;
            }
            ShotResult res = sandbox.fireCell(false, r, col);
            if (res == ShotResult.HIT || res == ShotResult.SUNK) {
                hitCells.add(c);
                if (res == ShotResult.SUNK) worst = ShotResult.SUNK;
                else if (worst != ShotResult.SUNK) worst = ShotResult.HIT;
            } else if (res == ShotResult.MISS) {
                missCells.add(c);
            }
        }
        boolean anyBlocked = !blockedCells.isEmpty();
        boolean anyHit = !hitCells.isEmpty();
        playerBoardPanel.setLastShot(tr, tc);
        if (!hitCells.isEmpty()) {
            playerBoardPanel.playEffects(zc.impactFx, hitCells, zc.accentColor, 48L);
        }
        if (!missCells.isEmpty()) {
            playerBoardPanel.playEffects(zc.impactFx, missCells, zc.accentColor, 48L);
        }
        if (!armorCells.isEmpty()) {
            playerBoardPanel.playEffects(SpriteAssets.FxType.SONAR, armorCells, new Color(0xFACC15), 36L);
            playerBoardPanel.setSuperArmorGrid(sandbox.getPlayerSuperArmor());
        }
        if (zc == ZombieMode.ZombieClass.ABYSS_CARRIER) {
            playerBoardPanel.addImpactZone(tr, tc, cells, CombatLoadout.MagicOption.FIRE, zc.accentColor, true);
        }
        if (anyBlocked) {
            BoardPanel.ShieldAbsorptionResult sRes = handleShieldImpact(
                    tr, tc, zombieShieldDamage(zc), zc.accentColor, zc.skillName);
            for (int[] bc : blockedCells) {
                if (bc[0] != tr || bc[1] != tc) playerBoardPanel.triggerShieldBlock(bc[0], bc[1], 2);
            }
            if (anyHit && !sRes.shattered()) {
                showMessage(zc.title + " dùng " + zc.skillName + " bắn trúng " + hitCells.size()
                        + " ô (Khiên chặn " + blockedCells.size() + " · còn " + sRes.remainingEnergy() + "% NL)!", zc.accentColor);
            }
        } else if (!armorCells.isEmpty() && !anyHit) {
            SoundEngine.playImpact(ShotResult.HIT, false);
            playerBoardPanel.triggerShake(3);
            showMessage("Giáp Vàng Nanite Siêu Tàu chặn " + armorCells.size() + " đòn của " + zc.title + "!",
                    new Color(0xFDE047));
        } else {
            SoundEngine.playImpact(anyHit ? worst : ShotResult.MISS, false);
            if (anyHit) {
                playerBoardPanel.triggerShake(zc == ZombieMode.ZombieClass.ABYSS_CARRIER
                        || zc == ZombieMode.ZombieClass.BLOOD_BATTLESHIP ? 6 : 4);
                showMessage(zc.title + " dùng " + zc.skillName + " bắn trúng " + hitCells.size() + " ô!", zc.accentColor);
            } else {
                showMessage(zc.title + " dùng " + zc.skillName + " bắn trượt tại " + coordinate(tr, tc) + ".", zc.accentColor);
            }
        }
        playerBoardPanel.refresh();
        refreshShots();

        if (sandbox.areAllSunk(false)) {
            finishSandboxGame(false);
        } else {
            turnLocked = false;
        }
    }

    private void finishSandboxGame(boolean won) {
        if (!sandboxMode) return;
        sandbox.forceSunkAll(won);
        gameOver = true;
        turnLocked = true;
        computerBoardPanel.setInteractive(false);
        computerBoardPanel.clearTargetPreview();
        playerBoardPanel.refresh();
        computerBoardPanel.refresh();
        refreshShots();
        SoundEngine.playGameOver(won);
        if (won) {
            showMessage("CHIẾN THẮNG (SANDBOX) — Toàn bộ hạm đội địch đã chìm! Bấm 'Reset Sandbox' để chơi lại.",
                    UiTokens.STATUS_SUCCESS);
        } else {
            showMessage("THẤT BẠI (SANDBOX) — Hạm đội bạn đã chìm! Bấm 'Reset Sandbox' để thử lại.",
                    UiTokens.STATUS_DANGER);
        }
    }

    private void resetSandbox() {
        if (!sandboxMode) return;
        SoundEngine.playUiClick();
        sandbox.reset();
        if (titan != null) titan.reset(TitanMode.getActiveBoss());
        gameOver = false;
        turnLocked = false;
        enemyFrozenTurns = 0;
        mana = CombatLoadout.MAX_MANA;
        fleetSpirit = 100;
        superShipActive = false;
        superShipCutscenePlaying = false;
        superShipReadyAnnounced = false;
        hyperCannonArmed = false;
        hyperCannonCooldown = 0;
        revealedEnemyCells.clear();
        shieldPreviewEnabled = false;
        shieldShatteredState = false;
        updateShieldButtonUi();
        updateSuperShipButtonUi();

        playerBoardPanel.clearAllVisualState();
        computerBoardPanel.clearAllVisualState();
        OceanMapBackdrop.clearTitanEnvironmentSurge();
        playerBoardPanel.setCellStateOverride(sandbox.getPlayerGrid());
        playerBoardPanel.setKnownShips(sandbox.getPlayerShips());
        playerBoardPanel.setInteractive(true);
        if (ZombieMode.isTitanMode() && titan != null) {
            computerBoardPanel.setCellStateOverride(titan.getBossGrid());
            computerBoardPanel.setKnownShips(List.of());
        } else {
            computerBoardPanel.setCellStateOverride(sandbox.getEnemyGrid());
            computerBoardPanel.setKnownShips(sandbox.getEnemyShips());
        }
        playerBoardPanel.setAmbientStageDriven(true);
        computerBoardPanel.setAmbientStageDriven(true);
        computerBoardPanel.setInteractive(true);

        applyGameMode(false);
        if (ZombieMode.isTitanMode()) {
            SoundEngine.refreshThemeMusic();
        }
        refreshLoadout();
        refreshShots();
        showMessage("Sandbox đã reset — Hạm đội mới sẵn sàng! Click bất kỳ ô địch nào để thử bắn.", UiTokens.ACCENT_SIGNAL);
    }

    private void executeCannonShot(int row, int col) {
        turnLocked = true;
        try {
            ShotResult result = game.playerFire(row, col);
            if (result == ShotResult.ALREADY_SHOT) {
                showMessage("Ô " + coordinate(row, col) + " đã bắn rồi, hãy chọn ô khác.", UiTokens.STATUS_WARNING);
                turnLocked = false;
                return;
            }
            if (result == null) {
                showMessage("Không nhận được kết quả phát bắn.", UiTokens.STATUS_WARNING);
                turnLocked = false;
                return;
            }
            computerBoardPanel.clearTargetPreview();
            boolean hit = result == ShotResult.HIT || result == ShotResult.SUNK;
            showMessage("Đang khai hỏa Pháo Hạm vào " + coordinate(row, col) + "...", UiTokens.ACCENT_SIGNAL);
            SoundEngine.playWeaponMagicLaunch(CombatLoadout.WeaponOption.CANNON, CombatLoadout.MagicOption.NONE);
            fxOverlay.launchStrike(playerBoardPanel, computerBoardPanel, row, col,
                    CombatLoadout.WeaponOption.CANNON, CombatLoadout.MagicOption.NONE, hit, () -> {
                        SoundEngine.playImpact(result, false);
                        computerBoardPanel.setLastShot(row, col);
                        computerBoardPanel.playEffect(
                                result == ShotResult.MISS ? SpriteAssets.FxType.MISS : SpriteAssets.FxType.HIT,
                                row, col, hit ? UiTokens.HIT_CRIMSON : UiTokens.STEEL_EDGE, 0L);
                        if (hit) computerBoardPanel.triggerShake(result == ShotResult.SUNK ? 6 : 4);
                        computerBoardPanel.refresh();
                        int cap = currentMaxMana();
                        int regen = superShipActive ? (hit ? 3 : 2) : (hit ? 2 : 1);
                        mana = Math.min(cap, mana + regen);
                        if (hit) {
                            gainFleetSpirit(result == ShotResult.SUNK ? 24 : 14);
                        }
                        if (superShipActive && hyperCannonCooldown > 0) hyperCannonCooldown--;
                        refreshLoadout();
                        updateSuperShipButtonUi();
                        showMessage("Bạn bắn " + coordinate(row, col) + ": " + resultText(result) + ".", UiTokens.TEXT_PRIMARY);
                        afterPlayerShot();
                    });
        } catch (UnsupportedOperationException exception) {
            showMessage("Logic bắn chưa sẵn sàng (TODO bên module logic).", UiTokens.STATUS_WARNING);
            turnLocked = false;
        }
    }

    private void executeComboStrike(int row, int col) {
        int totalCost = currentManaCost();
        if (totalCost > mana) {
            showMessage("Không đủ Hải Năng (cần " + totalCost + ", hiện có " + mana + ").", UiTokens.STATUS_DANGER);
            return;
        }
        int previousMana = mana;
        turnLocked = true;
        mana -= totalCost;
        refreshLoadout();
        try {
            // TODO (logic-contract): nối vào Game.useCombo(...) khi module logic sẵn sàng.
            ShotResult result = invokeComboStrike(row, col);
            if (result == null || result == ShotResult.ALREADY_SHOT) {
                mana = previousMana;
                refreshLoadout();
                showMessage(result == null ? "Không nhận được kết quả combo." : "Ô " + coordinate(row, col) + " đã bắn rồi.",
                        UiTokens.STATUS_WARNING);
                turnLocked = false;
                return;
            }
            CombatLoadout.WeaponOption weapon = selectedWeapon;
            CombatLoadout.MagicOption magic = selectedMagic;
            List<int[]> cells = targetCells(row, col);
            boolean hit = result == ShotResult.HIT || result == ShotResult.SUNK;
            computerBoardPanel.clearTargetPreview();
            showMessage("Đang khai hỏa " + weapon.label + " + " + magic.label + " vào " + coordinate(row, col) + "...",
                    magic.accentColor);
            SoundEngine.playWeaponMagicLaunch(weapon, magic);
            fxOverlay.launchStrike(playerBoardPanel, computerBoardPanel, row, col, cells, weapon, magic, hit, () -> {
                triggerHybridImpact(computerBoardPanel, row, col, cells, weapon, magic, result);
                computerBoardPanel.refresh();
                showMessage("Bạn bắn " + coordinate(row, col) + ": " + resultText(result) + ".", UiTokens.TEXT_PRIMARY);
                afterPlayerShot();
            });
        } catch (UnsupportedOperationException exception) {
            mana = previousMana;
            refreshLoadout();
            showMessage("Combo chưa sẵn sàng (TODO bên module logic); Hải Năng đã được hoàn lại.", UiTokens.STATUS_WARNING);
            turnLocked = false;
        }
    }

    private ShotResult invokeComboStrike(int row, int col) {
        // Logic has not published Game.useCombo(...) yet; keep this adapter as the only integration point.
        throw new UnsupportedOperationException("TODO (logic-contract): Game.useCombo");
    }

    private void afterPlayerShot() {
        refreshShots();
        if (checkGameOver()) return;
        Timer delay = new Timer(480, event -> takeComputerTurn());
        delay.setRepeats(false);
        delay.start();
    }

    private static List<int[]> allBoardCells() {
        List<int[]> all = new ArrayList<>(Board.SIZE * Board.SIZE);
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                all.add(new int[]{r, c});
            }
        }
        return all;
    }

    private static int zombieShieldDamage(ZombieMode.ZombieClass zc) {
        if (zc == null) return 24;
        return switch (zc) {
            case ABYSS_CARRIER -> 42;
            case BLOOD_BATTLESHIP -> 36;
            case NECRO_CRUISER -> 28;
            case BLIGHT_SUBMARINE -> 24;
            case PLAGUE_DESTROYER -> 22;
        };
    }

    private BoardPanel.ShieldAbsorptionResult handleShieldImpact(
            int row,
            int col,
            int energyDamage,
            Color impactAccent,
            String sourceName
    ) {
        BoardPanel.ShieldAbsorptionResult res = playerBoardPanel.absorbShieldStrike(row, col, energyDamage, impactAccent);
        java.awt.Point pt = playerBoardPanel.getCellCenterIn(row, col, fxOverlay);
        if (res.shattered()) {
            shieldPreviewEnabled = false;
            shieldShatteredState = true;
            updateShieldButtonUi();
            SoundEngine.playShieldShatter();
            fxOverlay.triggerShieldShatter(playerBoardPanel);
            showMessage("CẢNH BÁO! " + sourceName + " đã PHÁ VỠ KHIÊN PHÉP (0% năng lượng)!", UiTokens.STATUS_DANGER);
        } else {
            shieldPreviewEnabled = playerBoardPanel.isFullMapShield() || !playerBoardPanel.getShieldedCells().isEmpty();
            shieldShatteredState = false;
            updateShieldButtonUi();
            SoundEngine.playShieldCrack(res.remainingEnergy());
            fxOverlay.triggerShieldDeflect(pt, res.remainingEnergy(), BoardPanel.SHIELD_MAX_ENERGY,
                    res.absorbedDamage(), impactAccent);
            showMessage("Khiên Phép chắn " + sourceName + " (-" + res.absorbedDamage()
                    + "% NL · còn " + res.remainingEnergy() + "% · " + playerBoardPanel.getShieldCrackCount() + " vết nứt)!",
                    res.remainingEnergy() <= 30 ? UiTokens.STATUS_WARNING : UiTokens.SHIELD_TINT);
        }
        updateSuperShipButtonUi();
        return res;
    }

    private void updateShieldButtonUi() {
        boolean active = playerBoardPanel != null
                && (playerBoardPanel.isFullMapShield() || !playerBoardPanel.getShieldedCells().isEmpty());
        if (active) {
            shieldShatteredState = false;
            shieldPreviewEnabled = true;
            int e = Math.max(0, Math.min(BoardPanel.SHIELD_MAX_ENERGY, playerBoardPanel.getShieldEnergy()));
            double ratio = e / (double) BoardPanel.SHIELD_MAX_ENERGY;
            Color barCol = ratio > 0.60 ? UiTokens.SHIELD_TINT
                    : (ratio > 0.30 ? UiTokens.STATUS_WARNING : UiTokens.STATUS_DANGER);
            shieldDemoButton.setText("Khiên: " + e + "%");
            shieldDemoButton.setEnergyProgress(ratio, barCol, barCol);
            shieldDemoButton.setToolTipText("Khiên Phép 3D đang bật: " + e + "/100% NL · Vết nứt: "
                    + playerBoardPanel.getShieldCrackCount() + " (Click để tắt)");
        } else if (shieldShatteredState) {
            shieldPreviewEnabled = false;
            shieldDemoButton.setText("Khiên: Vỡ!");
            shieldDemoButton.setEnergyProgress(0.0, UiTokens.STATUS_DANGER, UiTokens.STATUS_DANGER);
            shieldDemoButton.setToolTipText("Khiên Phép đã VỠ VỤN do hết năng lượng! Click để triệu hồi lại Khiên 100%");
        } else {
            shieldPreviewEnabled = false;
            shieldDemoButton.setText("Khiên: Tắt");
            shieldDemoButton.setEnergyProgress(-1.0, UiTokens.SHIELD_TINT, null);
            shieldDemoButton.setToolTipText("Bật/Tắt Khiên Phép Thuật 3D (100% Năng Lượng)");
        }
    }

    private void toggleShieldPreview() {
        boolean currentlyOn = playerBoardPanel.isFullMapShield() || shieldPreviewEnabled;
        if (!currentlyOn) {
            shieldPreviewEnabled = true;
            shieldShatteredState = false;
            shieldDemoButton.setText("Khiên: 100%");
            shieldDemoButton.setEnergyProgress(1.0, UiTokens.SHIELD_TINT, UiTokens.SHIELD_TINT);
            List<int[]> shieldCells = new ArrayList<>();
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    CellState st = sandboxMode ? sandbox.getPlayerGrid()[r][c]
                            : (titanBattleMode && titan != null ? titan.getPlayerGrid()[r][c] : game.playerBoard.getCell(r, c));
                    if (st == CellState.SHIP) {
                        shieldCells.add(new int[]{r, c});
                    }
                }
            }
            if (shieldCells.isEmpty()) {
                shieldCells.add(new int[]{4, 3});
                shieldCells.add(new int[]{4, 4});
                shieldCells.add(new int[]{4, 5});
            }
            List<BoardPanel.ShipLaunchInfo> fleetShips = playerBoardPanel.getAlliedShipCentersIn(fxOverlay);
            SoundEngine.playShieldActivate();
            showMessage("5 tàu tụ Vòng Tròn Phép Thuật — Dựng Khiên TOÀN BẢN ĐỒ (100% NL)!", UiTokens.SHIELD_TINT);
            fxOverlay.launchFleetShieldActivation(playerBoardPanel, fleetShips, () -> {
                if (!shieldPreviewEnabled) return;
                playerBoardPanel.setFullMapShield(true, shieldCells);
                updateShieldButtonUi();
                showMessage("Khiên Phép TOÀN BẢN ĐỒ 100% NL đã dựng! Toàn bộ ô 10×10 & 5 tàu được bảo vệ.", UiTokens.SHIELD_TINT);
            });
        } else {
            SoundEngine.playUiClick();
            shieldPreviewEnabled = false;
            shieldShatteredState = false;
            playerBoardPanel.setFullMapShield(false, List.of());
            updateShieldButtonUi();
            showMessage("Đã tắt Khiên Phép Toàn Bản Đồ.", UiTokens.TEXT_MUTED);
        }
    }

    private void previewSelectedFx() {
        int r = computerBoardPanel.getHoveredRow() >= 0 ? computerBoardPanel.getHoveredRow() : 4;
        int c = computerBoardPanel.getHoveredCol() >= 0 ? computerBoardPanel.getHoveredCol() : 4;
        CombatLoadout.WeaponOption weapon = selectedWeapon;
        CombatLoadout.MagicOption magic = selectedMagic;
        List<int[]> cells = targetCells(r, c);
        showMessage("Khai hỏa thử: " + weapon.label + " × " + magic.label + " -> " + coordinate(r, c) + "...",
                magic.accentColor);
        SoundEngine.playWeaponMagicLaunch(weapon, magic);
        fxOverlay.launchStrike(playerBoardPanel, computerBoardPanel, r, c, cells, weapon, magic, true, () -> {
            triggerHybridImpact(computerBoardPanel, r, c, cells, weapon, magic, ShotResult.HIT);
            String zoneTag = weapon == CombatLoadout.WeaponOption.AIRSTRIKE ? " (+ vùng tác động 3×3)" : "";
            showMessage("Đã va chạm 3D" + zoneTag + ": " + weapon.label + " + " + magic.label + " tại " + coordinate(r, c) + ".",
                    magic.accentColor);
        });

        List<int[]> activeShields = playerBoardPanel.getShieldedCells();
        boolean fullShield = playerBoardPanel.isFullMapShield();
        if (ZombieMode.isTitanMode()) {
            TitanMode.TitanSkill skill = selectedTitanSkill;
            int tr = (!activeShields.isEmpty() && (shieldPreviewEnabled || fullShield)) ? activeShields.get(0)[0] : 4;
            int tc = (!activeShields.isEmpty() && (shieldPreviewEnabled || fullShield)) ? activeShields.get(0)[1] : 4;
            Timer tCounter = new Timer(520, e -> {
                List<int[]> tCells = TitanMode.skillTargetCells(skill, tr, tc, true);
                List<int[]> demoHits = new ArrayList<>();
                List<int[]> demoBlocked = new ArrayList<>();
                for (int[] cell : tCells) {
                    if (playerBoardPanel.isCellShielded(cell[0], cell[1])) demoBlocked.add(cell);
                    else demoHits.add(cell);
                }
                boolean enraged = titan != null && titan.isEnraged();
                SoundEngine.playTitanSkill(skill.boss, skill);
                fxOverlay.launchTitanSkill(computerBoardPanel, playerBoardPanel, skill, enraged, tr, tc, tCells, !demoHits.isEmpty(), () -> {
                    applyTitanSkillVisuals(skill, tr, tc, tCells, demoHits, List.of(), demoBlocked);
                });
            });
            tCounter.setRepeats(false);
            tCounter.start();
        } else if (ZombieMode.isZombieMode()) {
            ZombieMode.ZombieClass zc = selectedZombie;
            int zr = (!activeShields.isEmpty() && (shieldPreviewEnabled || fullShield)) ? activeShields.get(0)[0] : 4;
            int zcCol = (!activeShields.isEmpty() && (shieldPreviewEnabled || fullShield)) ? activeShields.get(0)[1] : 4;
            Timer zCounter = new Timer(500, e -> triggerZombieSkill(zc, zr, zcCol));
            zCounter.setRepeats(false);
            zCounter.start();
        } else if (shieldPreviewEnabled || fullShield) {
            int[] sc = !activeShields.isEmpty() ? activeShields.get(0) : new int[]{4, 4};
            Timer counterDemo = new Timer(460, e -> {
                SoundEngine.playWeaponMagicLaunch(CombatLoadout.WeaponOption.CANNON, CombatLoadout.MagicOption.NONE);
                fxOverlay.launchStrike(
                        computerBoardPanel, playerBoardPanel, sc[0], sc[1],
                        CombatLoadout.WeaponOption.CANNON, CombatLoadout.MagicOption.NONE, false, () -> {
                            handleShieldImpact(sc[0], sc[1], 20, UiTokens.ACCENT_SIGNAL,
                                    "Pháo thử nghiệm tại " + coordinate(sc[0], sc[1]));
                        });
            });
            counterDemo.setRepeats(false);
            counterDemo.start();
        }
    }

    private void applyGameMode(boolean announce) {
        boolean zombie = ZombieMode.isZombieMode();
        boolean titanMode = ZombieMode.isTitanMode() || titanBattleMode;
        modePill.sync();
        if (titanMode) {
            TitanMode.TitanBoss boss = titan != null ? titan.getBoss() : TitanMode.getActiveBoss();
            int hp = titan != null ? titan.getRemainingHp() : boss.maxHp;
            int maxHp = titan != null ? titan.getMaxHp() : boss.maxHp;
            boolean enraged = titan != null && titan.isEnraged();
            TitanMode.setTitanEnraged(enraged);
            computerBoardPanel.setZombieFleet(false);
            computerBoardPanel.setTitanBoss(boss, hp, maxHp, enraged);
            computerBoardPanel.setTitle(enraged ? ("THỨC TỈNH: " + boss.displayName) : ("Boss Titan: " + boss.displayName));
            if (titan != null) {
                computerBoardPanel.setCellStateOverride(titan.getBossGrid());
            }
            computerBoardPanel.setKnownShips(List.of());
            computerBoardPanel.setShowShips(false);
        } else if (sandboxMode) {
            TitanMode.setTitanEnraged(false);
            OceanMapBackdrop.clearTitanEnvironmentSurge();
            computerBoardPanel.setTitanBoss(null, 0, 0, false);
            computerBoardPanel.setCellStateOverride(sandbox.getEnemyGrid());
            computerBoardPanel.setZombieFleet(zombie);
            computerBoardPanel.setTitle(zombie ? "Hạm đội Zombie (Sandbox 5 Boss)" : "Vùng biển địch (Sandbox)");
            computerBoardPanel.setKnownShips(sandbox.getEnemyShips());
            computerBoardPanel.setShowShips(sandboxRevealEnemy);
        } else if (zombie) {
            TitanMode.setTitanEnraged(false);
            OceanMapBackdrop.clearTitanEnvironmentSurge();
            computerBoardPanel.setTitanBoss(null, 0, 0, false);
            computerBoardPanel.setZombieFleet(true);
            computerBoardPanel.setTitle("Hạm đội Zombie (5 Boss)");
            computerBoardPanel.setKnownShips(ZombieMode.generateZombieShowcaseFleet());
            computerBoardPanel.setShowShips(true);
        } else {
            TitanMode.setTitanEnraged(false);
            OceanMapBackdrop.clearTitanEnvironmentSurge();
            computerBoardPanel.setTitanBoss(null, 0, 0, false);
            computerBoardPanel.setZombieFleet(false);
            computerBoardPanel.setTitle("Vùng biển địch");
            computerBoardPanel.setKnownShips(List.of());
            computerBoardPanel.setShowShips(false);
        }
        if (zombieRow != null) zombieRow.setVisible(zombie);
        if (titanRow != null) titanRow.setVisible(titanMode);
        revalidate();
        repaint();
        if (announce) refreshLoadout();
    }

    private void triggerZombieSkill(ZombieMode.ZombieClass zc, int targetRow, int targetCol) {
        if (zc == null) return;
        List<int[]> cells = ZombieMode.skillTargetCells(zc, targetRow, targetCol, true);
        boolean shielded = playerBoardPanel.isCellShielded(targetRow, targetCol);
        showMessage(zc.title + " tung chiêu " + zc.skillName + " vào " + coordinate(targetRow, targetCol) + "...",
                zc.accentColor);
        SoundEngine.playZombieLaunch();
        fxOverlay.launchZombieStrike(computerBoardPanel, playerBoardPanel, zc, targetRow, targetCol, cells, !shielded, () -> {
            playerBoardPanel.setLastShot(targetRow, targetCol);
            if (shielded) {
                handleShieldImpact(targetRow, targetCol, zombieShieldDamage(zc), zc.accentColor, zc.skillName);
            } else {
                SoundEngine.playImpact(ShotResult.HIT, false);
                playerBoardPanel.playEffects(zc.impactFx, cells, zc.accentColor, 48L);
                if (zc == ZombieMode.ZombieClass.ABYSS_CARRIER) {
                    playerBoardPanel.addImpactZone(targetRow, targetCol, cells,
                            CombatLoadout.MagicOption.FIRE, zc.accentColor, true);
                }
                playerBoardPanel.triggerShake(zc == ZombieMode.ZombieClass.ABYSS_CARRIER
                        || zc == ZombieMode.ZombieClass.BLOOD_BATTLESHIP ? 6 : 4);
                if (zc == ZombieMode.ZombieClass.NECRO_CRUISER && mana > 0) {
                    mana = Math.max(0, mana - 1);
                    refreshLoadout();
                    showMessage(zc.title + " gây Ấn Nguyền & hút 1 Hải Năng tại " + coordinate(targetRow, targetCol) + "!",
                            zc.accentColor);
                } else {
                    showMessage(zc.title + " đánh trúng bằng " + zc.skillName + " tại " + coordinate(targetRow, targetCol) + "!",
                            zc.accentColor);
                }
            }
        });
    }

    private void triggerHybridImpact(
            BoardPanel targetBoard,
            int centerRow,
            int centerCol,
            List<int[]> cells,
            CombatLoadout.WeaponOption weapon,
            CombatLoadout.MagicOption magic,
            ShotResult result
    ) {
        SoundEngine.playImpact(result, false);
        targetBoard.setLastShot(centerRow, centerCol);
        SpriteAssets.FxType primaryFx = resolveFxForMagic(magic, result);
        long stagger = switch (weapon) {
            case CANNON -> 0L;
            case TORPEDO -> 65L;
            case MISSILE_CROSS -> 45L;
            case AIRSTRIKE -> 50L;
        };
        targetBoard.playEffects(primaryFx, cells, magic.accentColor, stagger);
        // Layer a kinetic blast alongside magic on center cell so modern weapon + magic fusion reads unmistakably
        if (magic != CombatLoadout.MagicOption.NONE && result != ShotResult.MISS) {
            targetBoard.playEffect(SpriteAssets.FxType.HIT, centerRow, centerCol, UiTokens.HIT_CRIMSON, 40L);
        }
        int shake = switch (weapon) {
            case CANNON -> result == ShotResult.MISS ? 0 : 4;
            case TORPEDO -> 5;
            case MISSILE_CROSS -> 6;
            case AIRSTRIKE -> 7;
        };
        if (weapon == CombatLoadout.WeaponOption.AIRSTRIKE) {
            targetBoard.addImpactZone(centerRow, centerCol, cells, magic, null, false);
        }
        if (shake > 0) targetBoard.triggerShake(shake);
    }

    private static SpriteAssets.FxType resolveFxForMagic(CombatLoadout.MagicOption magic, ShotResult result) {
        return switch (magic) {
            case FIRE -> SpriteAssets.FxType.FIRE;
            case LIGHTNING -> SpriteAssets.FxType.LIGHTNING;
            case ARCANE_SONAR -> SpriteAssets.FxType.SONAR;
            case FROST -> SpriteAssets.FxType.FROST;
            case NONE -> result == ShotResult.MISS ? SpriteAssets.FxType.MISS : SpriteAssets.FxType.HIT;
        };
    }

    public void setPlayerKnownShips(Collection<Ship> ships) {
        playerBoardPanel.setKnownShips(ships);
    }

    public void setPlayerShieldedCells(Collection<int[]> cells) {
        playerBoardPanel.setShieldedCells(cells);
    }

    private void takeComputerTurn() {
        final Shot aiShot;
        try {
            aiShot = game.computerTurn();
        } catch (UnsupportedOperationException exception) {
            showMessage("Logic lượt máy chưa sẵn sàng (TODO bên module logic).", UiTokens.STATUS_WARNING);
            turnLocked = false;
            return;
        }
        if (aiShot == null) {
            showMessage("Máy chưa trả về kết quả phát bắn.", UiTokens.STATUS_WARNING);
            turnLocked = false;
            return;
        }
        boolean aiHit = aiShot.result() == ShotResult.HIT || aiShot.result() == ShotResult.SUNK;
        boolean shielded = playerBoardPanel.isCellShielded(aiShot.row(), aiShot.col());
        Runnable finishAiImpact = () -> {
            playerBoardPanel.refresh();
            playerBoardPanel.setLastShot(aiShot.row(), aiShot.col());
            if (shielded) {
                handleShieldImpact(aiShot.row(), aiShot.col(), 20, UiTokens.ACCENT_SIGNAL,
                        "Pháo địch tại " + coordinate(aiShot.row(), aiShot.col()));
                gainFleetSpirit(14);
            } else {
                SoundEngine.playImpact(aiShot.result(), false);
                playerBoardPanel.playEffect(
                        aiShot.result() == ShotResult.MISS ? SpriteAssets.FxType.MISS : SpriteAssets.FxType.HIT,
                        aiShot.row(), aiShot.col(), aiHit ? UiTokens.HIT_CRIMSON : UiTokens.STEEL_EDGE, 0L);
                if (aiHit) {
                    playerBoardPanel.triggerShake(aiShot.result() == ShotResult.SUNK ? 6 : 4);
                    gainFleetSpirit(aiShot.result() == ShotResult.SUNK ? 28 : 14);
                }
                showMessage("Máy bắn " + coordinate(aiShot.row(), aiShot.col()) + ": " + resultText(aiShot.result()) + ".",
                        UiTokens.TEXT_PRIMARY);
            }
            refreshShots();
            checkSuperShipReadyCue();
            if (!checkGameOver()) turnLocked = false;
        };
        if (ZombieMode.isZombieMode()) {
            ZombieMode.ZombieClass[] classes = ZombieMode.ZombieClass.values();
            ZombieMode.ZombieClass zc = classes[Math.max(0, countShots(game.playerBoard) % classes.length)];
            selectedZombie = zc;
            refreshLoadout();
            showMessage(zc.title + " dùng " + zc.skillName + " bắn về " + coordinate(aiShot.row(), aiShot.col()) + "...",
                    zc.accentColor);
            SoundEngine.playZombieLaunch();
            fxOverlay.launchZombieStrike(computerBoardPanel, playerBoardPanel, zc, aiShot.row(), aiShot.col(),
                    List.of(new int[]{aiShot.row(), aiShot.col()}), !shielded && aiHit, () -> {
                        playerBoardPanel.refresh();
                        playerBoardPanel.setLastShot(aiShot.row(), aiShot.col());
                        if (shielded) {
                            handleShieldImpact(aiShot.row(), aiShot.col(), zombieShieldDamage(zc), zc.accentColor, zc.skillName);
                            gainFleetSpirit(14);
                        } else {
                            SoundEngine.playImpact(aiShot.result(), false);
                            SpriteAssets.FxType fx = aiShot.result() == ShotResult.MISS ? SpriteAssets.FxType.MISS : zc.impactFx;
                            playerBoardPanel.playEffect(fx, aiShot.row(), aiShot.col(),
                                    aiHit ? zc.accentColor : UiTokens.STEEL_EDGE, 0L);
                            if (zc == ZombieMode.ZombieClass.ABYSS_CARRIER) {
                                playerBoardPanel.addImpactZone(aiShot.row(), aiShot.col(),
                                        ZombieMode.skillTargetCells(zc, aiShot.row(), aiShot.col(), true),
                                        CombatLoadout.MagicOption.FIRE, zc.accentColor, true);
                            }
                            if (aiHit) {
                                playerBoardPanel.triggerShake(aiShot.result() == ShotResult.SUNK ? 6 : 4);
                                gainFleetSpirit(aiShot.result() == ShotResult.SUNK ? 28 : 14);
                            }
                            showMessage(zc.title + " (" + zc.skillName + ") bắn " + coordinate(aiShot.row(), aiShot.col())
                                    + ": " + resultText(aiShot.result()) + ".", zc.accentColor);
                        }
                        refreshShots();
                        checkSuperShipReadyCue();
                        if (!checkGameOver()) turnLocked = false;
                    });
        } else {
            showMessage("Địch đang khai hỏa về " + coordinate(aiShot.row(), aiShot.col()) + "...", UiTokens.STATUS_WARNING);
            SoundEngine.playWeaponMagicLaunch(CombatLoadout.WeaponOption.CANNON, CombatLoadout.MagicOption.NONE);
            fxOverlay.launchStrike(computerBoardPanel, playerBoardPanel, aiShot.row(), aiShot.col(),
                    CombatLoadout.WeaponOption.CANNON, CombatLoadout.MagicOption.NONE, aiHit, finishAiImpact);
        }
    }

    private boolean checkGameOver() {
        try {
            if (!game.isOver()) return false;
            gameOver = true;
            computerBoardPanel.setInteractive(false);
            computerBoardPanel.clearTargetPreview();
            try {
                boolean won = game.playerWon();
                SoundEngine.playGameOver(won);
                if (won) showMessage("Chiến thắng — hạm đội địch đã bị đánh chìm.", UiTokens.STATUS_SUCCESS);
                else showMessage("Thất bại — hạm đội của bạn đã bị đánh chìm.", UiTokens.STATUS_DANGER);
            } catch (UnsupportedOperationException exception) {
                showMessage("Ván đã kết thúc; kết quả chưa sẵn sàng (TODO bên module logic).", UiTokens.STATUS_WARNING);
            }
            turnLocked = true;
            return true;
        } catch (UnsupportedOperationException exception) {
            showMessage("Kiểm tra kết thúc ván chưa sẵn sàng (TODO bên module logic).", UiTokens.STATUS_WARNING);
            return false;
        }
    }

    private void requestNewGame() {
        if (onNewGame == null) return;
        if (!sandboxMode && !gameOver) {
            int answer = JOptionPane.showConfirmDialog(this, "Bắt đầu ván mới? Tiến trình hiện tại sẽ bị mất.",
                    "Xác nhận ván mới", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) return;
        }
        onNewGame.run();
    }

    private int currentManaCost() { return selectedWeapon.manaCost + selectedMagic.manaCost; }

    private void refreshLoadout() {
        int cost = currentManaCost();
        int cap = currentMaxMana();
        if (sandboxMode) {
            mana = cap;
            manaLabel.setText("Hải năng: " + cap + "/" + cap + " (∞)");
            manaPips.setMana(cap, cap);
            costLabel.setText("Chi phí: " + cost + " Mana (Sandbox ∞)");
            costLabel.setForeground(UiTokens.ACCENT_SIGNAL);
        } else {
            manaLabel.setText("Hải năng: " + mana + "/" + cap);
            manaPips.setMana(mana, cap);
            costLabel.setText("Chi phí: " + cost + " Mana");
            costLabel.setForeground(cost > mana ? UiTokens.STATUS_DANGER : UiTokens.TEXT_MUTED);
        }
        comboLabel.setText("<html><div style='width: 215px'>" + selectedWeapon.label + " + " + selectedMagic.label
                + " — " + selectedWeapon.description + " " + selectedMagic.description + "</div></html>");
        for (int index = 0; index < weaponChips.size(); index++) {
            weaponChips.get(index).setSelected(CombatLoadout.WeaponOption.values()[index] == selectedWeapon);
        }
        for (int index = 0; index < magicChips.size(); index++) {
            magicChips.get(index).setSelected(CombatLoadout.MagicOption.values()[index] == selectedMagic);
        }
        for (int index = 0; index < zombieChips.size(); index++) {
            zombieChips.get(index).setSelected(ZombieMode.ZombieClass.values()[index] == selectedZombie);
        }
        TitanMode.TitanBoss activeBoss = titan != null ? titan.getBoss() : TitanMode.getActiveBoss();
        titanPill.sync(activeBoss);
        List<TitanMode.TitanSkill> tSkills = TitanMode.skillsFor(activeBoss);
        if (selectedTitanSkill.boss != activeBoss && !tSkills.isEmpty()) {
            selectedTitanSkill = tSkills.get(0);
        }
        boolean bossEnraged = titan != null && titan.isEnraged();
        for (int index = 0; index < titanChips.size() && index < tSkills.size(); index++) {
            TitanMode.TitanSkill ts = tSkills.get(index);
            LoadoutChip chip = titanChips.get(index);
            boolean lockedP2 = !sandboxMode && ts.phase2Only && !bossEnraged;
            Color chipAccent = lockedP2 ? UiTokens.TEXT_MUTED : ts.accentColor;
            chip.updateDisplay(ts.chipLabel, chipAccent, SpriteAssets.getTitanSkillSprite(ts));
            chip.setToolTipText(lockedP2
                    ? (ts.skillName + " [CHỜ PHASE 2 <= 50% HP] — " + ts.description)
                    : (ts.skillName + " — " + ts.description));
            chip.setSelected(ts == selectedTitanSkill);
        }
        if (titanRow != null) {
            titanRow.revalidate();
        }
        rotateButton.setEnabled(selectedWeapon == CombatLoadout.WeaponOption.TORPEDO);
        updateRotateButton();
    }

    private void updateRotateButton() {
        rotateButton.setText("Xoay: " + (torpedoHorizontal ? "Ngang" : "Dọc"));
        rotateButton.setToolTipText("Xoay hướng Ngư Lôi (phím R)");
    }

    private void refreshShots() {
        if ((titanBattleMode || (sandboxMode && ZombieMode.isTitanMode())) && titan != null) {
            int alivePlayer = countAlivePlayerShips();
            shotsLabel.setText("Boss HP: " + titan.getRemainingHp() + "/" + titan.getMaxHp()
                    + " · Tàu còn: " + alivePlayer + "/5"
                    + (superShipActive ? " ★" : ""));
        } else if (sandboxMode) {
            shotsLabel.setText("Bạn: " + sandbox.countShots(true) + " (" + sandbox.countSunkShips(true)
                    + "/5) · Địch: " + sandbox.countShots(false) + " (" + sandbox.countSunkShips(false) + "/5)");
        } else {
            shotsLabel.setText("Phát bắn — Bạn: " + countShots(game.playerBoard) + " · Máy: " + countShots(game.computerBoard));
        }
    }

    private int countShots(Board board) {
        int count = 0;
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                CellState state = board.getCell(row, col);
                if (state == CellState.HIT || state == CellState.MISS) count++;
            }
        }
        return count;
    }

    private void showMessage(String text, Color color) {
        message.setText(text);
        message.setForeground(color);
    }

    private static String coordinate(int row, int col) { return "" + (char) ('A' + col) + (row + 1); }

    private static String resultText(ShotResult result) {
        return switch (result) {
            case MISS -> "Trượt";
            case HIT -> "Trúng đích";
            case SUNK -> "Đánh chìm tàu";
            case ALREADY_SHOT -> "Ô đã bắn";
        };
    }

    private int currentMaxMana() {
        return superShipActive ? CombatLoadout.SUPER_MAX_MANA : CombatLoadout.MAX_MANA;
    }

    public int countAlivePlayerShips() {
        return playerBoardPanel != null ? playerBoardPanel.countAliveShips() : 0;
    }

    public int getFleetSpirit() { return fleetSpirit; }
    public boolean isSuperShipActive() { return superShipActive; }
    public boolean isHyperCannonArmed() { return hyperCannonArmed; }
    public int getHyperCannonCooldown() { return hyperCannonCooldown; }

    public void clickSuperShipButtonForTest() {
        onSuperShipButtonClicked();
    }

    public void gainFleetSpirit(int delta) {
        if (superShipActive || delta <= 0) return;
        if (sandboxMode) {
            fleetSpirit = 100;
            updateSuperShipButtonUi();
            return;
        }
        fleetSpirit = Math.min(100, fleetSpirit + delta);
        checkSuperShipReadyCue();
    }

    private void checkSuperShipReadyCue() {
        updateSuperShipButtonUi();
        if (superShipActive || superShipCutscenePlaying || sandboxMode || gameOver) return;
        int alive = countAlivePlayerShips();
        if (fleetSpirit >= 100 && alive == 1 && !superShipReadyAnnounced) {
            superShipReadyAnnounced = true;
            SoundEngine.playSuperShipReady();
            showMessage("★ NIỀM HY VỌNG CUỐI CÙNG! Đủ 100% Nội Tại & còn đúng 1 tàu — Bấm '★ SIÊU TÀU!' ngay!",
                    new Color(0xFDE047));
        }
    }

    private void updateSuperShipButtonUi() {
        if (superShipCutscenePlaying) {
            superShipButton.setText("Đang chế...");
            superShipButton.setEnergyProgress(1.0, new Color(0x38BDF8), new Color(0xFDE047));
            superShipButton.setToolTipText("Đang chế tạo & khởi hành Siêu Tàu Hy Vọng Aether Ark!");
            return;
        }
        if (superShipActive) {
            if (hyperCannonArmed) {
                superShipButton.setText("★ Khóa Mục Tiêu");
                superShipButton.setEnergyProgress(1.0, new Color(0xFACC15), new Color(0xFDE047));
                superShipButton.setToolTipText("Click một ô trên hải đồ phải để phóng ĐẠI PHÁO HYPERION 3×3 (0 Mana + Choáng 1 lượt)");
            } else if (hyperCannonCooldown > 0 && !sandboxMode) {
                superShipButton.setText("★ Pháo (" + hyperCannonCooldown + ")");
                double ratio = (2.0 - Math.min(2, hyperCannonCooldown)) / 2.0;
                superShipButton.setEnergyProgress(ratio, new Color(0x38BDF8), UiTokens.BORDER_SUBTLE);
                superShipButton.setToolTipText("Đại Pháo Hyperion đang hồi: còn " + hyperCannonCooldown + " lượt bắn");
            } else {
                superShipButton.setText("★ Đại Pháo");
                superShipButton.setEnergyProgress(1.0, new Color(0x38BDF8), new Color(0xFACC15));
                superShipButton.setToolTipText("Tuyệt kỹ Siêu Tàu: Bắn Đại Pháo Hyperion 3×3 (0 Mana · Choáng Boss · +15% Khiên)");
            }
            return;
        }
        if (sandboxMode) {
            fleetSpirit = 100;
            superShipButton.setText("★ SIÊU TÀU!");
            superShipButton.setEnergyProgress(1.0, new Color(0xFACC15), new Color(0x38BDF8));
            superShipButton.setToolTipText("Sandbox: Click để xem Cutscene 4 hồi & thức tỉnh Siêu Tàu Hy Vọng ngay (giữ 1 tàu chủ lực)!");
            return;
        }
        int alive = countAlivePlayerShips();
        boolean dualReady = fleetSpirit >= 100 && alive == 1;
        double ratio = Math.max(0.0, Math.min(1.0, fleetSpirit / 100.0));
        if (dualReady) {
            superShipButton.setText("★ SIÊU TÀU!");
            superShipButton.setEnergyProgress(1.0, new Color(0xFACC15), new Color(0xFDE047));
            superShipButton.setToolTipText("ĐÃ ĐỦ ĐIỀU KIỆN (100% Nội Tại + còn 1 tàu)! Click để chế tạo & xuất kích Siêu Tàu Aether Ark!");
        } else {
            superShipButton.setText("Siêu Tàu: " + fleetSpirit + "%");
            Color barCol = fleetSpirit >= 100 ? new Color(0xF59E0B) : new Color(0x38BDF8);
            superShipButton.setEnergyProgress(ratio, barCol, fleetSpirit >= 100 ? new Color(0xF59E0B) : null);
            superShipButton.setToolTipText("Cần 100% Nội Tại (" + fleetSpirit + "%) VÀ còn đúng 1 tàu sống sót (hiện còn " + alive + " tàu).");
        }
    }

    private void onSuperShipButtonClicked() {
        if (gameOver || turnLocked || superShipCutscenePlaying) return;
        if (superShipActive) {
            if (!sandboxMode && hyperCannonCooldown > 0) {
                SoundEngine.playUiClick();
                showMessage("Đại Pháo Hyperion đang hồi năng lượng — cần bắn thêm " + hyperCannonCooldown + " lượt nữa!",
                        UiTokens.STATUS_WARNING);
                return;
            }
            hyperCannonArmed = !hyperCannonArmed;
            SoundEngine.playUiClick();
            updateSuperShipButtonUi();
            if (hyperCannonArmed) {
                showMessage("★ ĐẠI PHÁO HYPERION ĐÃ KHÓA NÒNG! Click mục tiêu trên hải đồ phải để phóng 3×3 (0 Mana + Choáng)!",
                        new Color(0xFDE047));
            } else {
                computerBoardPanel.clearTargetPreview();
                showMessage("Đã hủy khóa nòng Đại Pháo Hyperion.", UiTokens.TEXT_MUTED);
            }
            return;
        }

        int alive = countAlivePlayerShips();
        if (!sandboxMode && (fleetSpirit < 100 || alive != 1)) {
            SoundEngine.playUiClick();
            if (fleetSpirit < 100 && alive != 1) {
                showMessage("Chưa đủ điều kiện SIÊU TÀU! Cần 100% Nội Tại (" + fleetSpirit
                        + "%) VÀ còn đúng 1 tàu (hiện còn " + alive + " tàu).", UiTokens.STATUS_WARNING);
            } else if (fleetSpirit < 100) {
                showMessage("Còn 1 tàu rồi! Tiếp tục giao tranh/bật Khiên để tích đủ 100% Nội Tại (hiện " + fleetSpirit + "%).",
                        new Color(0x38BDF8));
            } else {
                showMessage("Nội Tại đã 100%! Siêu Tàu chỉ thức tỉnh khi hạm đội chỉ còn ĐÚNG 1 TÀU sống sót (hiện còn "
                        + alive + " tàu).", new Color(0xF59E0B));
            }
            return;
        }
        activateSuperShipFlow();
    }

    private void activateSuperShipFlow() {
        if (sandboxMode && sandbox != null) {
            sandbox.prepareLastStandSandbox();
            playerBoardPanel.refresh();
            refreshShots();
        }
        List<BoardPanel.AliveShipInfo> aliveShips = playerBoardPanel.getAliveShips(fxOverlay);
        if (aliveShips.isEmpty()) return;
        BoardPanel.AliveShipInfo keeper = aliveShips.get(0);

        turnLocked = true;
        superShipCutscenePlaying = true;
        hyperCannonArmed = false;
        updateSuperShipButtonUi();
        SoundEngine.playSuperShipCutscene();
        showMessage("★ DỰ ÁN AETHER ARK: CHẾ TẠO & KHỞI HÀNH NIỀM HY VỌNG CUỐI CÙNG!", new Color(0x38BDF8));

        fxOverlay.launchSuperShipCutscene(playerBoardPanel, () -> {
            superShipCutscenePlaying = false;
            superShipActive = true;
            hyperCannonCooldown = 0;
            hyperCannonArmed = false;

            playerBoardPanel.activateSuperShip(keeper.row(), keeper.col(), keeper.length(), keeper.horizontal());
            if (titanBattleMode && titan != null) {
                titan.activateSuperArmorForLastShip();
                playerBoardPanel.setSuperArmorGrid(titan.getSuperArmorGrid());
            } else if (sandboxMode && sandbox != null) {
                sandbox.activateSuperArmor();
                playerBoardPanel.setSuperArmorGrid(sandbox.getPlayerSuperArmor());
            }

            List<int[]> shieldCells = new ArrayList<>();
            for (int i = 0; i < keeper.length(); i++) {
                int r = keeper.row() + (keeper.horizontal() ? 0 : i);
                int c = keeper.col() + (keeper.horizontal() ? i : 0);
                if (r >= 0 && r < Board.SIZE && c >= 0 && c < Board.SIZE) {
                    shieldCells.add(new int[]{r, c});
                }
            }
            if (shieldCells.isEmpty()) shieldCells.add(new int[]{keeper.row(), keeper.col()});
            shieldPreviewEnabled = true;
            shieldShatteredState = false;
            playerBoardPanel.setFullMapShield(true, shieldCells);
            updateShieldButtonUi();

            mana = CombatLoadout.SUPER_MAX_MANA;
            playerBoardPanel.refresh();
            refreshLoadout();
            refreshShots();
            updateSuperShipButtonUi();
            turnLocked = false;
            showMessage("★ SIÊU TÀU AETHER ARK ĐÃ KHỞI HÀNH! 7/7 Mana · +100% Khiên · Giáp Vàng +2 · Sẵn sàng '★ Đại Pháo'!",
                    new Color(0xFDE047));
        });
    }

    private void executeHyperionCannon(int row, int col) {
        if (gameOver || turnLocked || !superShipActive) return;
        List<int[]> cells = cells3x3(row, col);
        hyperCannonArmed = false;
        computerBoardPanel.clearTargetPreview();

        if ((titanBattleMode || (sandboxMode && ZombieMode.isTitanMode())) && titan != null) {
            turnLocked = true;
            TitanBattleController.PlayerStrikeOutcome out = titan.fireHyperionUltimate(row, col, cells);
            SoundEngine.playSuperShipHyperCannon();
            showMessage("★ AETHER ARK KHAI HỎA ĐẠI PHÁO HYPERION VÀO " + titan.getBoss().shortLabel + "!",
                    new Color(0xFDE047));
            fxOverlay.launchHyperionCannon(playerBoardPanel, computerBoardPanel, row, col, cells, !out.hitCells().isEmpty(), () -> {
                SoundEngine.playImpact(out.aggregateResult(), false);
                computerBoardPanel.setLastShot(row, col);
                if (!out.hitCells().isEmpty()) {
                    computerBoardPanel.playEffects(SpriteAssets.FxType.FIRE, out.hitCells(), new Color(0xFDE047), 38L);
                    computerBoardPanel.triggerTitanHitFlash(new Color(0x38BDF8));
                } else {
                    computerBoardPanel.playEffects(SpriteAssets.FxType.MISS, out.missCells(), UiTokens.STEEL_EDGE, 30L);
                }
                computerBoardPanel.addImpactZone(row, col, cells, CombatLoadout.MagicOption.LIGHTNING, new Color(0x38BDF8), false);
                computerBoardPanel.triggerShake(9);
                if (!out.sonarCells().isEmpty()) {
                    mergeRevealedCells(out.sonarCells());
                    computerBoardPanel.setRevealedCells(revealedEnemyCells);
                }
                computerBoardPanel.updateTitanState(titan.getRemainingHp(), titan.getMaxHp(), titan.isEnraged());
                computerBoardPanel.setTitle(titan.isEnraged()
                        ? ("THỨC TỈNH: " + titan.getBoss().displayName)
                        : ("Boss Titan: " + titan.getBoss().displayName));
                hyperCannonCooldown = sandboxMode ? 0 : 2;
                playerBoardPanel.addShieldEnergy(15);
                updateShieldButtonUi();
                if (sandboxMode) mana = currentMaxMana();
                computerBoardPanel.refresh();
                refreshLoadout();
                refreshShots();
                updateSuperShipButtonUi();
                if (out.bossDefeated()) {
                    finishTitanBattle(true);
                    return;
                }
                if (out.enragedJustTriggered()) {
                    SoundEngine.playTitanEnrageTransform(titan.getBoss());
                    computerBoardPanel.triggerTitanEmergence(1350L);
                    computerBoardPanel.triggerTitanCharge(1550L);
                    fxOverlay.launchTitanEnrageCutIn(computerBoardPanel, titan.getBoss());
                    applyTitanEnvironmentImpact(TitanMode.environmentForEnrage(titan.getBoss()), false);
                }
                showMessage("★ HYPERION NOVA! -" + out.hitCells().size() + " HP Boss · +15% Khiên · Boss CHOÁNG 1 lượt!",
                        new Color(0x38BDF8));
                turnLocked = false;
            });
            return;
        }

        if (sandboxMode && sandbox != null) {
            turnLocked = true;
            SandboxController.StrikeOutcome out = sandbox.fireHyperionUltimate(row, col, cells);
            SoundEngine.playSuperShipHyperCannon();
            showMessage("★ SANDBOX HYPERION NOVA VÀO " + coordinate(row, col) + "!", new Color(0xFDE047));
            fxOverlay.launchHyperionCannon(playerBoardPanel, computerBoardPanel, row, col, cells, !out.hitCells().isEmpty(), () -> {
                applySandboxPlayerImpact(row, col, cells, CombatLoadout.WeaponOption.AIRSTRIKE, CombatLoadout.MagicOption.LIGHTNING, out);
                hyperCannonCooldown = 0;
                playerBoardPanel.addShieldEnergy(15);
                updateShieldButtonUi();
                mana = currentMaxMana();
                refreshLoadout();
                updateSuperShipButtonUi();
                if (sandbox.areAllSunk(true)) {
                    finishSandboxGame(true);
                    return;
                }
                showMessage("★ HYPERION NOVA! Trúng " + out.hitCells().size() + " ô · +15% Khiên · Địch CHOÁNG 1 lượt!",
                        new Color(0x38BDF8));
                turnLocked = false;
            });
            return;
        }

        if (game != null) {
            int fireR = row;
            int fireC = col;
            CellState st = game.computerBoard.getCell(fireR, fireC);
            if (st == CellState.HIT || st == CellState.MISS) {
                for (int[] c : cells) {
                    CellState cs = game.computerBoard.getCell(c[0], c[1]);
                    if (cs == CellState.EMPTY || cs == CellState.SHIP) {
                        fireR = c[0];
                        fireC = c[1];
                        break;
                    }
                }
            }
            turnLocked = true;
            ShotResult res = ShotResult.MISS;
            try {
                res = game.playerFire(fireR, fireC);
            } catch (UnsupportedOperationException ex) {
                turnLocked = false;
                updateSuperShipButtonUi();
                showMessage("Chức năng bắn chưa sẵn sàng trong chế độ Classic.", UiTokens.STATUS_WARNING);
                return;
            }
            final int shotR = fireR;
            final int shotC = fireC;
            final ShotResult finalRes = res != null ? res : ShotResult.MISS;
            boolean hit = finalRes == ShotResult.HIT || finalRes == ShotResult.SUNK;
            SoundEngine.playSuperShipHyperCannon();
            showMessage("★ AETHER ARK KHAI HỎA ĐẠI PHÁO HYPERION VÀO " + coordinate(shotR, shotC) + "!",
                    new Color(0xFDE047));
            fxOverlay.launchHyperionCannon(playerBoardPanel, computerBoardPanel, shotR, shotC, cells, hit, () -> {
                SoundEngine.playImpact(finalRes, false);
                computerBoardPanel.setLastShot(shotR, shotC);
                computerBoardPanel.addImpactZone(shotR, shotC, cells, CombatLoadout.MagicOption.LIGHTNING, new Color(0x38BDF8), false);
                computerBoardPanel.playEffect(hit ? SpriteAssets.FxType.FIRE : SpriteAssets.FxType.MISS,
                        shotR, shotC, hit ? new Color(0xFDE047) : UiTokens.STEEL_EDGE, 0L);
                computerBoardPanel.triggerShake(8);
                computerBoardPanel.refresh();
                hyperCannonCooldown = 2;
                playerBoardPanel.addShieldEnergy(15);
                updateShieldButtonUi();
                refreshLoadout();
                refreshShots();
                updateSuperShipButtonUi();
                if (checkGameOver()) return;
                showMessage("★ HYPERION NOVA (" + resultText(finalRes) + ") · +15% Khiên · Địch CHOÁNG 1 lượt!",
                        new Color(0x38BDF8));
                turnLocked = false;
            });
        }
    }

    private static final class ManaPips extends JPanel {
        private int mana;
        private int maxMana = CombatLoadout.MAX_MANA;
        private ManaPips() {
            setOpaque(false);
            setPreferredSize(new Dimension(CombatLoadout.SUPER_MAX_MANA * UiTokens.MANA_PIP_SIZE
                    + (CombatLoadout.SUPER_MAX_MANA - 1) * UiTokens.SPACE_XS, UiTokens.MANA_PIP_SIZE));
        }

        private void setMana(int mana, int maxMana) {
            this.maxMana = Math.max(CombatLoadout.MAX_MANA, Math.min(CombatLoadout.SUPER_MAX_MANA, maxMana));
            this.mana = Math.max(0, Math.min(this.maxMana, mana));
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int index = 0; index < maxMana; index++) {
                int x = index * (UiTokens.MANA_PIP_SIZE + UiTokens.SPACE_XS);
                boolean superPip = index >= CombatLoadout.MAX_MANA;
                Color fill = superPip ? new Color(0xFACC15) : UiTokens.MANA_FILL;
                g.setColor(index < mana ? fill : UiTokens.MANA_EMPTY);
                g.fillRoundRect(x, 0, UiTokens.MANA_PIP_SIZE, UiTokens.MANA_PIP_SIZE, UiTokens.SPACE_XS, UiTokens.SPACE_XS);
                g.setColor(superPip ? new Color(0x38BDF8) : UiTokens.BORDER_SUBTLE);
                g.setStroke(new BasicStroke(1f));
                g.drawRoundRect(x, 0, UiTokens.MANA_PIP_SIZE, UiTokens.MANA_PIP_SIZE, UiTokens.SPACE_XS, UiTokens.SPACE_XS);
            }
            g.dispose();
        }
    }

    private static final class LoadoutChip extends JPanel {
        private String label;
        private Color accent;
        private final boolean showAccent;
        private final boolean compact;
        private BufferedImage icon;
        private boolean selected;
        private boolean hovered;

        private LoadoutChip(String label, Color accent, boolean showAccent, BufferedImage icon) {
            this(label, accent, showAccent, icon, false);
        }

        private LoadoutChip(String label, Color accent, boolean showAccent, BufferedImage icon, boolean compact) {
            this.label = label;
            this.accent = accent;
            this.showAccent = showAccent;
            this.compact = compact;
            this.icon = icon;
            setOpaque(false);
            setFocusable(true);
            setFont(UiTokens.FONT_META);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            syncChipSize();
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent event) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent event) { hovered = false; repaint(); }
            });
        }

        private void syncChipSize() {
            int textWidth = getFontMetrics(UiTokens.FONT_META).stringWidth(label);
            int prefW;
            if (compact) {
                int iconSpace = icon != null ? 21 : 0;
                int leftSpace = (showAccent ? 5 : 4) + iconSpace + 4;
                prefW = Math.max(58, textWidth + leftSpace);
            } else {
                int iconSpace = icon != null ? 24 : 0;
                int leftSpace = (showAccent ? UiTokens.SPACE_LG : UiTokens.SPACE_SM) + iconSpace;
                prefW = Math.max(showAccent ? 72 : 96, textWidth + leftSpace);
            }
            Dimension d = new Dimension(prefW, UiTokens.CHIP_HEIGHT);
            setPreferredSize(d);
            setMinimumSize(d);
        }

        private void updateDisplay(String label, Color accent, BufferedImage icon) {
            this.label = label;
            this.accent = accent;
            this.icon = icon;
            syncChipSize();
            revalidate();
            repaint();
        }

        private void setSelected(boolean selected) { this.selected = selected; repaint(); }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            Color background = selected ? UiTokens.BG_ELEVATED : (hovered ? UiTokens.BTN_SECONDARY_HOVER : UiTokens.BG_SURFACE);
            g.setColor(background);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), UiTokens.BORDER_RADIUS, UiTokens.BORDER_RADIUS);
            g.setColor(selected ? accent : UiTokens.BORDER_SUBTLE);
            g.setStroke(new BasicStroke(selected ? 1.5f : 1f));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UiTokens.BORDER_RADIUS, UiTokens.BORDER_RADIUS);
            if (showAccent) {
                g.setColor(accent);
                g.fillRoundRect(0, UiTokens.SPACE_XS, UiTokens.SPACE_XS - 1,
                        getHeight() - UiTokens.SPACE_XS * 2, UiTokens.SPACE_XS, UiTokens.SPACE_XS);
            }
            if (hasFocus()) {
                g.setColor(UiTokens.FOCUS_RING);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(2, 2, getWidth() - UiTokens.SPACE_XS, getHeight() - UiTokens.SPACE_XS,
                        UiTokens.BORDER_RADIUS, UiTokens.BORDER_RADIUS);
            }
            int iconOffset = 0;
            if (icon != null) {
                int iconSize = compact ? 16 : 20;
                int ix = compact ? 4 : UiTokens.SPACE_SM;
                int iy = (getHeight() - iconSize) / 2;
                g.drawImage(icon, ix, iy, iconSize, iconSize, null);
                iconOffset = iconSize + (compact ? 2 : UiTokens.SPACE_XS);
            }
            g.setFont(UiTokens.FONT_META);
            g.setColor(UiTokens.TEXT_PRIMARY);
            FontMetrics metrics = g.getFontMetrics();
            int left = (showAccent ? (compact ? 4 : UiTokens.SPACE_SM) : UiTokens.SPACE_XS) + iconOffset;
            int textWidth = metrics.stringWidth(label);
            g.drawString(label, left + Math.max(0, (getWidth() - left - textWidth) / 2),
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }
}
