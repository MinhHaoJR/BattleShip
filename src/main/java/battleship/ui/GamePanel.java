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
import javax.swing.JTextArea;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

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
    private int bossDemoTargetSeq;
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
    private long hyperCannonCooldownMs;
    private final Timer raidTimer;
    private final EnumMap<CombatLoadout.WeaponOption, Long> weaponCooldownEndMs =
            new EnumMap<>(CombatLoadout.WeaponOption.class);
    private final JLabel fleetBuffLabel = new JLabel();
    private ShipType selectedDodgeShip;
    private boolean dodgeHorizontal = true;
    private double manaRegenFrac;
    private long shieldActivatedAtMs;
    private long lastRaidTickMs;
    private long raidPauseUntilMs;
    private boolean prevDpsCheckActive;
    private final boolean bossRushActive;
    private int rushStagesCleared;
    private int rushTotalScore;
    private final boolean campaignMode;
    private final CampaignMode.CampaignStage campaignStage;
    private final CampaignBattleController campaignCtrl;
    private final Consumer<CampaignMode.CampaignStage> onNextCampaignStage;
    private final Runnable onBackToCampaignMap;
    private final Timer campaignClockTimer;
    private Timer pendingCampaignAiTimer;
    private Timer pendingSandboxAiTimer;
    private Timer pendingComputerAiTimer;
    private long campaignStartedMs;
    private final boolean coopMode;
    private final CoopSession coopSession;
    private final Runnable onBackToCoopLobby;
    private CoopSession.CoopListener coopPacketListener;
    private final Timer coopAiTimer;
    private int coopSynergyPct;
    private int coopSynergyUsedCount;
    private int coopActiveTurnPlayer = 1;
    private int coopLastPingRow = -1;
    private int coopLastPingCol = -1;
    private long coopLastPingUntilMs;
    private long coopLastBossSyncMs;
    private TacticalButton coopSynergyButton;
    private final RaidModalPanel raidModalPanel = new RaidModalPanel();
    private final StoryComicOverlay storyOverlay = new StoryComicOverlay();
    private TacticalButton storyBattleButton;
    private TacticalButton raidGuideButton;
    private TacticalButton sandboxRevealBtn;
    private TacticalButton sandboxCounterBtn;

    public GamePanel(Game game, Runnable onNewGame) {
        this(Objects.requireNonNull(game, "game"), onNewGame, false, false, null, null, null, null, null, null, null);
    }

    public static GamePanel createSandbox(List<Ship> playerShips, Runnable onNewGame) {
        return new GamePanel(null, onNewGame, true, false, playerShips, null, null, null, null, null, null);
    }

    public static GamePanel createTitanBattle(List<Ship> playerShips, TitanMode.TitanBoss boss, Runnable onNewGame) {
        TitanMode.TitanBoss chosen = boss != null ? boss : TitanMode.TitanBoss.GODZILLA;
        TitanMode.setActiveBoss(chosen);
        ZombieMode.setGameMode(ZombieMode.GameMode.TITAN);
        return new GamePanel(null, onNewGame, false, true, playerShips, chosen, null, null, null, null, null);
    }

    public static GamePanel createCoopTitanBattle(
            List<Ship> playerShips,
            CoopSession session,
            Runnable onBackToCoopLobby
    ) {
        CoopSession active = session != null ? session : new CoopSession();
        CoopSession.CoopConfig cfg = active.getConfig();
        OceanMapBackdrop.setMapTheme(cfg.mapTheme());
        OceanMapBackdrop.setSeason(cfg.season());
        OceanMapBackdrop.setWeatherMode(cfg.weatherMode());
        TitanMode.setActiveBoss(cfg.boss());
        TitanMode.setDifficulty(cfg.difficulty());
        TitanMode.setBossRushEnabled(false);
        ZombieMode.setGameMode(ZombieMode.GameMode.TITAN);
        return new GamePanel(null, onBackToCoopLobby, false, true, playerShips, cfg.boss(),
                null, null, null, active, onBackToCoopLobby);
    }

    public static GamePanel createCampaignBattle(
            List<Ship> playerShips,
            CampaignMode.CampaignStage stage,
            Consumer<CampaignMode.CampaignStage> onNextStage,
            Runnable onBackToMap
    ) {
        CampaignMode.CampaignStage target = stage != null ? stage : CampaignMode.stage(1);
        OceanMapBackdrop.setMapTheme(target.chapter().theme);
        OceanMapBackdrop.setSeason(target.season());
        OceanMapBackdrop.setWeatherMode(target.weather());
        TitanMode.setBossRushEnabled(false);
        if (target.isBossStage()) {
            TitanMode.TitanBoss boss = target.boss() != null ? target.boss() : TitanMode.TitanBoss.GODZILLA;
            TitanMode.setActiveBoss(boss);
            TitanMode.setDifficulty(target.bossDifficulty());
            ZombieMode.setGameMode(ZombieMode.GameMode.TITAN);
            return new GamePanel(null, onBackToMap, false, true, playerShips, boss, target, onNextStage, onBackToMap, null, null);
        }
        ZombieMode.setGameMode(target.ruleType() == CampaignMode.StageRuleType.ZOMBIE_ASSAULT
                ? ZombieMode.GameMode.ZOMBIE
                : ZombieMode.GameMode.CLASSIC);
        return new GamePanel(null, onBackToMap, false, false, playerShips, null, target, onNextStage, onBackToMap, null, null);
    }

    private GamePanel(
            Game game,
            Runnable onNewGame,
            boolean sandboxMode,
            boolean titanBattleMode,
            List<Ship> initialPlayerShips,
            TitanMode.TitanBoss initialBoss,
            CampaignMode.CampaignStage campaignStage,
            Consumer<CampaignMode.CampaignStage> onNextCampaignStage,
            Runnable onBackToCampaignMap,
            CoopSession coopSession,
            Runnable onBackToCoopLobby
    ) {
        this.game = game;
        this.sandboxMode = sandboxMode;
        this.titanBattleMode = titanBattleMode;
        this.campaignStage = campaignStage;
        this.campaignMode = campaignStage != null;
        this.onNextCampaignStage = onNextCampaignStage;
        this.onBackToCampaignMap = onBackToCampaignMap;
        this.coopSession = coopSession;
        this.coopMode = coopSession != null && titanBattleMode;
        this.onBackToCoopLobby = onBackToCoopLobby;
        this.bossRushActive = !this.campaignMode && !this.coopMode && titanBattleMode && TitanMode.isBossRushEnabled();
        this.sandbox = sandboxMode ? new SandboxController(initialPlayerShips) : null;
        this.campaignCtrl = (this.campaignMode && !titanBattleMode)
                ? new CampaignBattleController(initialPlayerShips, campaignStage)
                : null;
        this.titan = (titanBattleMode || sandboxMode)
                ? new TitanBattleController(
                        sandboxMode ? this.sandbox.getPlayerShips() : initialPlayerShips,
                        initialBoss != null ? initialBoss : TitanMode.getActiveBoss(),
                        this.campaignMode
                                ? campaignStage.bossDifficulty()
                                : (titanBattleMode ? TitanMode.getDifficulty() : TitanMode.TitanDifficulty.NORMAL))
                : null;
        OceanMapBackdrop.setTitanPhase2(false);
        if (this.titan != null) {
            if (this.campaignMode && campaignStage.bossHpOverride() > 0) {
                this.titan.applyCampaignMaxHp(campaignStage.bossHpOverride());
            }
            this.titan.configureArenaStage(OceanMapBackdrop.getMapTheme(), titanBattleMode && !sandboxMode);
            if (this.coopMode) {
                long seed = coopSession.getActiveSeed() != 0L ? coopSession.getActiveSeed() : 202605L;
                double hpMult = coopSession.getConfig().coopHpPercent() / 100.0;
                this.titan.reseedForCoop(seed, hpMult);
            }
        }
        this.onNewGame = onNewGame;
        this.fleetSpirit = sandboxMode ? 100 : 0;
        if (this.campaignMode) {
            this.mana = campaignStage.initialMana();
        } else if (sandboxMode || titanBattleMode) {
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
            playerBoardPanel = new BoardPanel(null, "Hạm đội (Click/WASD)", true);
            playerBoardPanel.setCellStateOverride(titan.getPlayerGrid());
            playerBoardPanel.setKnownShips(titan.getPlayerShips());
            playerBoardPanel.setInteractive(true);
            playerBoardPanel.setOnCellClick((r, c) -> handleTitanPlayerBoardClick(r, c));
            playerBoardPanel.setOnCellHover((r, c) -> updateTitanDodgePreview(r, c));
            playerBoardPanel.setOnRightClick(() -> rotateSelectedDodgeShip());
            computerBoardPanel = new BoardPanel(null, "Đấu Trường Titan", false);
            computerBoardPanel.setCellStateOverride(titan.getBossGrid());
            computerBoardPanel.setKnownShips(List.of());
        } else if (campaignMode && campaignCtrl != null) {
            playerBoardPanel = new BoardPanel(null, "Hạm đội của bạn", true);
            playerBoardPanel.setCellStateOverride(campaignCtrl.getPlayerGrid());
            playerBoardPanel.setKnownShips(campaignCtrl.getPlayerShips());
            playerBoardPanel.setInteractive(false);
            computerBoardPanel = new BoardPanel(null, "Mục tiêu địch", false);
            computerBoardPanel.setCellStateOverride(campaignCtrl.getEnemyGrid());
            computerBoardPanel.setKnownShips(campaignCtrl.getEnemyShips());
            if (!campaignCtrl.getInitialSonarHints().isEmpty()) {
                mergeRevealedCells(campaignCtrl.getInitialSonarHints());
                computerBoardPanel.setRevealedCells(revealedEnemyCells);
            }
        } else {
            playerBoardPanel = new BoardPanel(game.playerBoard, "Hạm đội của bạn", true);
            playerBoardPanel.setInteractive(false);
            computerBoardPanel = new BoardPanel(game.computerBoard, "Vùng biển địch", false);
        }
        playerBoardPanel.setAmbientStageDriven(true);
        computerBoardPanel.setAmbientStageDriven(true);
        computerBoardPanel.setInteractive(true);
        computerBoardPanel.setOnCellClick(this::fireAt);
        computerBoardPanel.setOnRightCellClick(this::onBossBoardRightClick);
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
                raidModalPanel.setBounds(0, 0, w, h);
                storyOverlay.setBounds(0, 0, w, h);
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
        raidModalPanel.setVisible(false);
        boardStage.add(raidModalPanel, JLayeredPane.MODAL_LAYER);
        storyOverlay.setVisible(false);
        boardStage.add(storyOverlay, JLayeredPane.POPUP_LAYER);
        add(boardStage, BorderLayout.CENTER);
        add(createLoadoutBar(), BorderLayout.SOUTH);
        raidTimer = new Timer(35, e -> stepRaidTick());
        campaignClockTimer = new Timer(250, e -> stepCampaignClock());
        coopAiTimer = new Timer(3200, e -> stepCoopAiWingman());
        installTorpedoShortcut();
        applyGameMode(false);
        if (campaignMode) {
            campaignStartedMs = System.currentTimeMillis();
            syncCampaignHud();
            if (campaignStage.timeLimitSec() > 0 && !titanBattleMode) {
                campaignClockTimer.start();
            }
            if (!titanBattleMode) {
                showMessage(campaignStage.title() + " — " + campaignStage.ruleSummary(),
                        campaignStage.ruleType().accent);
                SoundEngine.refreshThemeMusic();
            }
            if (!SoundEngine.isSilentMode() && StoryMode.hasAutoIntroCutscene(campaignStage.stageNumber())) {
                turnLocked = true;
                openCampaignStoryCutscene(StoryMode.introEpisodeForStage(campaignStage));
            }
        }
        if (sandboxMode) {
            showMessage("SANDBOX SẴN SÀNG — Hải Năng ∞! Click hải đồ địch để bắn, click ô tàu mình để bật/tắt Khiên 3D.",
                    UiTokens.ACCENT_SIGNAL);
        } else if (titanBattleMode) {
            TitanMode.TitanBoss b = titan.getBoss();
            if (coopMode) {
                CoopSession.CoopConfig cfg = coopSession.getConfig();
                playerBoardPanel.setArenaProps(titan.getArenaProps());
                coopPacketListener = new CoopSession.CoopListener() {
                    @Override public void onPacketReceived(String cmd, String[] args) {
                        handleCoopNetworkPacket(cmd, args);
                    }
                    @Override public void onDisconnected(String reason) {
                        if (!gameOver) {
                            showMessage("MẤT KẾT NỐI CO-OP (" + reason + ") — Tiếp tục chiến đấu độc lập!",
                                    UiTokens.STATUS_WARNING);
                        }
                    }
                };
                coopSession.addListener(coopPacketListener);
                if (coopSession.getTransport() == CoopSession.CoopTransport.AI_ALLY) {
                    coopAiTimer.start();
                }
                showMessage("CO-OP RAID (" + cfg.p1Name() + " & " + cfg.p2Name() + ") — "
                        + b.shortLabel + " (" + titan.getMaxHp() + " HP)! Chuột phải Ping ô · Phím F phóng Hợp Kích!",
                        new Color(0x34D399));
            } else {
                showMessage("RAID TITAN — " + b.shortLabel + " (" + b.maxHp
                                + " HP)! Nhắm vùng đang tụ chiêu để NGẮT ĐÒN, dùng tàu né vùng đỏ & Parry Khiên!",
                        b.accentColor);
            }
            SoundEngine.refreshThemeMusic();
            lastRaidTickMs = System.currentTimeMillis();
            syncRaidBoardTelemetry();
            raidTimer.start();
            titanIntroPendingInitial = true;
            fxOverlay.addHierarchyListener(e -> {
                if (titanIntroPendingInitial && fxOverlay.isShowing() && !storyOverlay.isStoryActive()) {
                    titanIntroPendingInitial = false;
                    SwingUtilities.invokeLater(() -> triggerTitanBossEntrance(titan.getBoss()));
                }
            });
        }
        refreshLoadout();
        refreshShots();
    }

    @Override public void removeNotify() {
        stopAllTimers();
        OceanMapBackdrop.setTitanPhase2(false);
        fxOverlay.clearArenaHud();
        super.removeNotify();
    }

    private void stopAllTimers() {
        if (raidTimer != null) raidTimer.stop();
        if (campaignClockTimer != null) campaignClockTimer.stop();
        if (coopAiTimer != null) coopAiTimer.stop();
        if (coopSession != null && coopPacketListener != null) {
            coopSession.removeListener(coopPacketListener);
        }
        if (storyOverlay.isStoryActive()) {
            storyOverlay.skipCutscene();
        }
        stopPendingAiTimers();
    }

    private void stopPendingAiTimers() {
        if (pendingCampaignAiTimer != null) {
            pendingCampaignAiTimer.stop();
            pendingCampaignAiTimer = null;
        }
        if (pendingSandboxAiTimer != null) {
            pendingSandboxAiTimer.stop();
            pendingSandboxAiTimer = null;
        }
        if (pendingComputerAiTimer != null) {
            pendingComputerAiTimer.stop();
            pendingComputerAiTimer = null;
        }
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
        int maxSuperW = 92;
        for (String lbl : new String[]{"Siêu Tàu: 100%", "★ SIÊU TÀU!", "Đang chế...", "★ Đại Pháo", "★ Khóa Mục Tiêu", "★ Pháo (2)"}) {
            maxSuperW = Math.max(maxSuperW, sfm.stringWidth(lbl) + UiTokens.BUTTON_PADDING_X * 2 + 4);
        }
        Dimension superPref = new Dimension(maxSuperW, UiTokens.CHIP_HEIGHT);
        superShipButton.setPreferredSize(superPref);
        superShipButton.setMinimumSize(superPref);
        superShipButton.setMaximumSize(superPref);
        superShipButton.addActionListener(event -> onSuperShipButtonClicked());
        updateSuperShipButtonUi();
        combatActions.add(superShipButton);
        combatActions.add(javax.swing.Box.createHorizontalStrut(4));

        int maxShieldW = 86;
        for (String lbl : new String[]{"Khiên: Tắt", "Khiên: 100%", "Khiên: Vỡ!"}) {
            maxShieldW = Math.max(maxShieldW, sfm.stringWidth(lbl) + UiTokens.BUTTON_PADDING_X * 2 + 4);
        }
        Dimension shieldPref = new Dimension(maxShieldW, UiTokens.CHIP_HEIGHT);
        shieldDemoButton.setPreferredSize(shieldPref);
        shieldDemoButton.setMinimumSize(shieldPref);
        shieldDemoButton.setMaximumSize(shieldPref);
        shieldDemoButton.setToolTipText("Bật/Tắt Khiên Phép Thuật 3D bảo vệ hạm đội (100% Năng Lượng)");
        shieldDemoButton.addActionListener(event -> toggleShieldPreview());
        updateShieldButtonUi();
        if (coopMode) {
            int maxCoopW = 104;
            for (String lbl : new String[]{"Hợp Kích: 100%", "★ HỢP KÍCH [F]"}) {
                maxCoopW = Math.max(maxCoopW, sfm.stringWidth(lbl) + UiTokens.BUTTON_PADDING_X * 2 + 4);
            }
            coopSynergyButton = new TacticalButton("Hợp Kích: 0%", false);
            Dimension coopPref = new Dimension(maxCoopW, UiTokens.CHIP_HEIGHT);
            coopSynergyButton.setPreferredSize(coopPref);
            coopSynergyButton.setMinimumSize(coopPref);
            coopSynergyButton.setMaximumSize(coopPref);
            coopSynergyButton.setToolTipText("Phóng Song Long Hợp Kích 2 người (Phím F) khi đạt 100%");
            coopSynergyButton.addActionListener(event -> triggerCoopSynergyUltimate());
            combatActions.add(coopSynergyButton);
            combatActions.add(javax.swing.Box.createHorizontalStrut(4));
        }
        combatActions.add(shieldDemoButton);
        combatActions.add(javax.swing.Box.createHorizontalStrut(4));
        TacticalButton demoFxButton = new TacticalButton("Thử FX 3D", false);
        demoFxButton.setPreferredSize(new Dimension(demoFxButton.getPreferredSize().width, UiTokens.CHIP_HEIGHT));
        demoFxButton.setMinimumSize(demoFxButton.getPreferredSize());
        demoFxButton.setMaximumSize(demoFxButton.getPreferredSize());
        demoFxButton.setToolTipText("Xem trước hiệu ứng Blender 3D theo từng lớp tàu và kỹ năng đang chọn");
        demoFxButton.addActionListener(event -> previewSelectedFx());
        combatActions.add(demoFxButton);
        combatActions.add(javax.swing.Box.createHorizontalStrut(5));
        if (campaignMode && campaignStage != null) {
            storyBattleButton = new TacticalButton("Truyện", false);
            Dimension storyDim = new Dimension(storyBattleButton.getPreferredSize().width, UiTokens.CHIP_HEIGHT);
            storyBattleButton.setPreferredSize(storyDim);
            storyBattleButton.setMinimumSize(storyDim);
            storyBattleButton.setMaximumSize(storyDim);
            storyBattleButton.setToolTipText("Xem lại khung truyện tranh hoạt hình Cốt Truyện Màn " + campaignStage.stageNumber());
            storyBattleButton.addActionListener(e -> {
                SoundEngine.playUiClick();
                openCampaignStoryCutscene(StoryMode.introEpisodeForStage(campaignStage));
            });
            combatActions.add(storyBattleButton);
            combatActions.add(javax.swing.Box.createHorizontalStrut(4));
        }
        if (titanBattleMode && !sandboxMode) {
            raidGuideButton = new TacticalButton("?", false);
            Dimension guideDim = new Dimension(34, UiTokens.CHIP_HEIGHT);
            raidGuideButton.setPreferredSize(guideDim);
            raidGuideButton.setMinimumSize(guideDim);
            raidGuideButton.setMaximumSize(guideDim);
            raidGuideButton.setToolTipText("Hướng dẫn nhanh cơ chế Raid Boss Titan (Pause trận)");
            raidGuideButton.addActionListener(e -> toggleRaidGuideModal());
            combatActions.add(raidGuideButton);
            combatActions.add(javax.swing.Box.createHorizontalStrut(5));
        }
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
            if ((titanBattleMode && !sandboxMode) || campaignMode) {
                SoundEngine.playUiClick();
                showMessage("Bản đồ Đấu Trường đang khóa: " + OceanMapBackdrop.getMapTheme().displayName
                        + " — " + TitanMode.stageMechanicsSummary(OceanMapBackdrop.getMapTheme()),
                        OceanMapBackdrop.getMapTheme().accent);
                return;
            }
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
            if (campaignMode) {
                SoundEngine.playUiClick();
                showMessage("Mùa & thời tiết được khóa theo " + campaignStage.title(), campaignStage.ruleType().accent);
                return;
            }
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
            if (campaignMode) {
                SoundEngine.playUiClick();
                showMessage("Đang trong " + campaignStage.title() + " — chế độ đã khóa theo màn.", campaignStage.ruleType().accent);
                return;
            }
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
        boolean lockStageTheme = (titanBattleMode && !sandboxMode) || campaignMode;
        mapPill.setEnabled(!lockStageTheme);
        if (lockStageTheme) {
            OceanMapBackdrop.MapTheme st = OceanMapBackdrop.getMapTheme();
            mapPill.setToolTipText("Đấu Trường đã khóa (" + st.displayName + "): " + TitanMode.stageMechanicsSummary(st));
        }
        seasonPill.sync();
        seasonPill.setEnabled(!campaignMode);
        weatherPill.sync();
        modePill.sync();
        modePill.setEnabled(!campaignMode);
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
        shotsLabel.setMinimumSize(new Dimension(238, 18));
        shotsLabel.setPreferredSize(new Dimension(246, 18));
        shotsLabel.setMaximumSize(new Dimension(256, 22));
        summary.add(shotsLabel);
        fleetBuffLabel.setFont(UiTokens.FONT_COORD);
        fleetBuffLabel.setForeground(new Color(0x7DD3FC));
        fleetBuffLabel.setAlignmentX(LEFT_ALIGNMENT);
        fleetBuffLabel.setMinimumSize(new Dimension(238, 16));
        fleetBuffLabel.setPreferredSize(new Dimension(246, 16));
        fleetBuffLabel.setMaximumSize(new Dimension(256, 18));
        fleetBuffLabel.setVisible(titanBattleMode);
        summary.add(fleetBuffLabel);
        java.awt.Component summarySpacer = javax.swing.Box.createRigidArea(new Dimension(242, 0));
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
        inputMap.put(KeyStroke.getKeyStroke('R'), "rotateAction");
        getActionMap().put("rotateAction", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                if (titanBattleMode && selectedDodgeShip != null && playerBoardPanel.getHoveredRow() >= 0) {
                    rotateSelectedDodgeShip();
                } else if (selectedWeapon == CombatLoadout.WeaponOption.TORPEDO) {
                    rotateTorpedo();
                } else if (titanBattleMode && selectedDodgeShip != null) {
                    rotateSelectedDodgeShip();
                }
            }
        });
        String[][] wasd = {{"W", "-1", "0"}, {"S", "1", "0"}, {"A", "0", "-1"}, {"D", "0", "1"}};
        for (String[] k : wasd) {
            final int dr = Integer.parseInt(k[1]);
            final int dc = Integer.parseInt(k[2]);
            String actionName = "dodgeShip_" + k[0];
            inputMap.put(KeyStroke.getKeyStroke(k[0]), actionName);
            getActionMap().put(actionName, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                    stepSelectedShipBy(dr, dc);
                }
            });
        }
        inputMap.put(KeyStroke.getKeyStroke('F'), "coopSynergyAction");
        inputMap.put(KeyStroke.getKeyStroke('f'), "coopSynergyAction");
        getActionMap().put("coopSynergyAction", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                if (coopMode && !gameOver) {
                    triggerCoopSynergyUltimate();
                }
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
        return targetCellsFor(selectedWeapon, row, col);
    }

    private List<int[]> targetCellsFor(CombatLoadout.WeaponOption w, int row, int col) {
        List<int[]> cells = new ArrayList<>();
        switch (w != null ? w : CombatLoadout.WeaponOption.CANNON) {
            case CANNON -> {
                cells.add(new int[]{row, col});
                if (superShipActive && (titanBattleMode || sandboxMode || campaignCtrl != null)) {
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
        if (gameOver || superShipCutscenePlaying || fxOverlay.isBossCityRampageActive()
                || fxOverlay.isTitanSkillPovActive() || fxOverlay.isShipSkillCutsceneActive()) return;
        if (!titanBattleMode && turnLocked) return;
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
        if (campaignMode && campaignCtrl != null) {
            executeCampaignStrike(row, col);
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
        long now = System.currentTimeMillis();
        CombatLoadout.WeaponOption weapon = selectedWeapon;
        CombatLoadout.MagicOption magic = selectedMagic;
        if (titanBattleMode && !sandboxMode) {
            if (isRaidClockPaused()) return;
            long cdEnd = weaponCooldownEndMs.getOrDefault(weapon, 0L);
            if (now < cdEnd) {
                double rem = (cdEnd - now) / 1000.0;
                showMessage(weapon.label + " đang hồi chiêu (" + String.format(java.util.Locale.US, "%.1fs", rem) + ")!",
                        UiTokens.STATUS_WARNING);
                return;
            }
        }
        int cost = currentManaCost();
        if (!sandboxMode && cost > mana) {
            showMessage("Không đủ Hải Năng (cần " + cost + ", hiện có " + mana + ").", UiTokens.STATUS_DANGER);
            return;
        }
        // LAN Client forwards strike intent to Authoritative Host
        if (coopMode && coopSession != null && coopSession.isNetworkMode() && !coopSession.isHostSide()) {
            mana = Math.max(0, mana - cost);
            long cdMs = Math.round(TitanMode.weaponCooldownMs(weapon)
                    * titan.effectiveWeaponCooldownMult(superShipActive));
            weaponCooldownEndMs.put(weapon, now + cdMs);
            computerBoardPanel.clearTargetPreview();
            refreshLoadout();
            SoundEngine.playWeaponMagicLaunch(weapon, magic);
            coopSession.sendPacket("CMD_STRIKE", weapon.name(), magic.name(), row, col);
            showMessage("P2 (" + coopSession.getConfig().p2Name() + ") khai hỏa " + weapon.label + " tại "
                    + coordinate(row, col) + "...", new Color(0x34D399));
            return;
        }
        int shooter = coopMode
                ? (coopSession != null && coopSession.getTransport() == CoopSession.CoopTransport.HOTSEAT_2P
                        ? coopActiveTurnPlayer : 1)
                : 1;
        resolveAndAnimateTitanStrike(shooter, row, col, weapon, magic, targetCellsFor(weapon, row, col), true,
                coopMode && coopSession != null && coopSession.isNetworkMode());
        if (coopMode && coopSession != null && coopSession.getTransport() == CoopSession.CoopTransport.HOTSEAT_2P && !gameOver) {
            coopActiveTurnPlayer = (coopActiveTurnPlayer == 1) ? 2 : 1;
            mana = Math.min(currentMaxMana(), mana + 1);
            syncRaidBoardTelemetry();
            refreshLoadout();
        }
    }

    private void resolveAndAnimateTitanStrike(
            int shooterIdx,
            int row,
            int col,
            CombatLoadout.WeaponOption weapon,
            CombatLoadout.MagicOption magic,
            List<int[]> cells,
            boolean consumeManaAndCooldown,
            boolean broadcastFromHost
    ) {
        if (titan == null || gameOver) return;
        long now = System.currentTimeMillis();
        double interceptDamageScale = 1.0;
        TitanBattleController.MissileInterceptOutcome intercept = (titanBattleMode && !sandboxMode && consumeManaAndCooldown)
                ? titan.tryInterceptPlayerStrike(weapon, magic, false)
                : new TitanBattleController.MissileInterceptOutcome(false, false, null, 0, 0L);

        if (intercept.intercepted() && !intercept.partialPierce()) {
            int cost = (weapon != null ? weapon.manaCost : 0) + (magic != null ? magic.manaCost : 0);
            mana = Math.max(0, mana - cost);
            long cdMs = Math.round(TitanMode.weaponCooldownMs(weapon)
                    * titan.effectiveWeaponCooldownMult(superShipActive) * 0.72);
            weaponCooldownEndMs.put(weapon, now + cdMs);
            computerBoardPanel.clearTargetPreview();
            syncRaidBoardTelemetry();
            refreshLoadout();
            SoundEngine.playWeaponMagicLaunch(weapon, magic);
            SoundEngine.playTitanSkill(titan.getBoss(),
                    TitanMode.skillsFor(titan.getBoss(), titan.isEnraged()).get(0));
            TitanMode.AntiMissileSpec spec = intercept.counterSpec();
            String actorTag = coopMode && coopSession != null
                    ? (shooterIdx == 2 ? coopSession.getConfig().p2Name() : coopSession.getConfig().p1Name()) + ": "
                    : "";
            showMessage(actorTag + spec.bannerText() + " Đã bắn hạ tên lửa! (Mở cửa "
                    + String.format("%.1fs", intercept.nextCooldownMs() / 1000.0) + ")", spec.primaryColor());
            fxOverlay.launchBossMissileIntercept(
                    playerBoardPanel, computerBoardPanel, row, col, weapon, magic,
                    titan.getBoss(), titan.isEnraged(), spec, () -> {
                        computerBoardPanel.addFloatingPopup(2, 4,
                                spec.skillName().toUpperCase() + "!", spec.primaryColor(), true);
                        syncRaidBoardTelemetry();
                        refreshLoadout();
                    });
            return;
        }
        if (intercept.intercepted() && intercept.partialPierce()) {
            interceptDamageScale = 0.55;
            computerBoardPanel.addFloatingPopup(2, 4,
                    "★ " + intercept.counterSpec().skillName().toUpperCase() + " (CHỈ CHẶN 45%!) ★",
                    intercept.counterSpec().primaryColor(), true);
        }

        TitanBattleController.PlayerStrikeOutcome outcome =
                titan.fireAtTitan(row, col, cells, weapon, magic, superShipActive, interceptDamageScale);
        TitanBattleController.RaidStrikeInfo info = titan.getLastStrikeInfo();

        if (sandboxMode) {
            turnLocked = true;
            mana = CombatLoadout.MAX_MANA;
        } else if (consumeManaAndCooldown) {
            int cost = (weapon != null ? weapon.manaCost : 0) + (magic != null ? magic.manaCost : 0);
            mana = Math.max(0, mana - cost);
            long cdMs = Math.round(TitanMode.weaponCooldownMs(weapon)
                    * titan.effectiveWeaponCooldownMult(superShipActive));
            weaponCooldownEndMs.put(weapon, now + cdMs);
        }
        boolean anyHit = !outcome.hitCells().isEmpty();
        if (coopMode && anyHit) {
            applyCoopSynergyChargeFromHit(shooterIdx, row, col);
        }
        if (broadcastFromHost && coopSession != null) {
            coopSession.sendPacket("EVT_STRIKE", shooterIdx, weapon.name(), magic.name(), row, col,
                    info.damageDealt(), titan.getRemainingHp(), titan.isEnraged() ? 1 : 0,
                    outcome.bossDefeated() ? 1 : 0, coopSynergyPct);
            broadcastCoopSyncSnapshot();
        }
        processTitanArenaEvents();
        syncRaidBoardTelemetry();
        refreshLoadout();
        computerBoardPanel.clearTargetPreview();
        Color msgCol = shooterIdx == 2
                ? new Color(0x34D399)
                : (magic != CombatLoadout.MagicOption.NONE ? magic.accentColor : titan.getBoss().accentColor);
        String actorTag = coopMode && coopSession != null
                ? (shooterIdx == 2 ? coopSession.getConfig().p2Name() : coopSession.getConfig().p1Name()) + ": "
                : "";
        showMessage(actorTag + "Khai hỏa " + weapon.label + " + " + magic.label + " vào " + titan.getBoss().shortLabel
                + " (" + coordinate(row, col) + ")...", msgCol);
        SoundEngine.playWeaponMagicLaunch(weapon, magic);

        Runnable afterFx = () -> {
            SoundEngine.playImpact(outcome.aggregateResult(), false);
            computerBoardPanel.setLastShot(row, col);
            if (anyHit) {
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
                computerBoardPanel.triggerShake(info.brokenPartNow() != null ? 9 : (!outcome.weakpointHits().isEmpty() ? 7 : 4));

                String dmgTag = "-" + info.damageDealt()
                        + (info.submergedMitigated() ? " (LẶN)" : (!outcome.weakpointHits().isEmpty() ? "!" : ""));
                Color popCol = info.submergedMitigated() ? new Color(0x5EEAD4)
                        : (!outcome.weakpointHits().isEmpty() ? new Color(0xFDE047) : Color.WHITE);
                computerBoardPanel.addFloatingPopup(row, col, dmgTag, popCol, !outcome.weakpointHits().isEmpty());
            } else {
                computerBoardPanel.playEffects(SpriteAssets.FxType.MISS, outcome.missCells(), UiTokens.STEEL_EDGE, 36L);
                if (magic != CombatLoadout.MagicOption.NONE) {
                    triggerHybridImpact(computerBoardPanel, row, col, cells, weapon, magic, outcome.aggregateResult());
                }
                computerBoardPanel.addFloatingPopup(row, col, "HỤT", UiTokens.TEXT_MUTED, false);
            }
            if (outcome.burnCell() != null) {
                computerBoardPanel.playEffect(SpriteAssets.FxType.FIRE, outcome.burnCell()[0], outcome.burnCell()[1],
                        UiTokens.MAGIC_FIRE, 180L);
            }
            if (!outcome.sonarCells().isEmpty()) {
                mergeRevealedCells(outcome.sonarCells());
                computerBoardPanel.setRevealedCells(revealedEnemyCells);
            }
            if (info.submergedRevealed()) {
                computerBoardPanel.addFloatingPopup(row, col, "LỘ ×1.2!", new Color(0xFDE047), true);
            }
            handleNewlyBrokenParts(row, col);
            if (!titan.getLastReactionLabel().isEmpty()) {
                SoundEngine.playRaidElementalReaction();
                computerBoardPanel.addFloatingPopup(Math.max(1, row), Math.min(8, col + 1),
                        "★ " + titan.getLastReactionLabel(), new Color(0x38BDF8), true);
                playerBoardPanel.setHazardTiles(titan.getActiveHazards());
            }
            if (info.staggerTriggered()) {
                SoundEngine.playRaidStaggerBreak();
                computerBoardPanel.addFloatingPopup(Math.max(1, row - 1), Math.min(8, col + 1),
                        "★ NGẮT CHIÊU! CHOÁNG ★", new Color(0x22D3EE), true);
                playerBoardPanel.setTelegraphCells(List.of(), 0.0, UiTokens.STATUS_DANGER);
            }
            if (!sandboxMode) {
                if (anyHit && (info.staggerTriggered() || info.brokenPartNow() != null || outcome.weakpointHits().size() >= 2)) {
                    mana = Math.min(currentMaxMana(), mana + 1);
                }
                if (anyHit) {
                    int spiritGain = Math.min(20, 6 + outcome.weakpointHits().size() * 3 + (info.staggerTriggered() ? 8 : 0));
                    gainFleetSpirit(spiritGain);
                }
            }
            if (sandboxMode && superShipActive && hyperCannonCooldown > 0) hyperCannonCooldown--;
            syncRaidBoardTelemetry();
            refreshLoadout();
            refreshShots();
            updateSuperShipButtonUi();

            if (outcome.bossDefeated()) {
                finishTitanBattle(true);
                return;
            }
            if (outcome.enragedJustTriggered()) {
                raidPauseUntilMs = System.currentTimeMillis() + 1450L;
                SoundEngine.playTitanEnrageTransform(titan.getBoss());
                computerBoardPanel.triggerTitanEmergence(1350L);
                computerBoardPanel.triggerTitanCharge(1550L);
                computerBoardPanel.triggerShake(9);
                computerBoardPanel.addFloatingPopup(2, 4,
                        "★ GIÁP CUỒNG BẠO +" + titan.getEnrageBarrierHp() + "! ★",
                        new Color(0x38BDF8), true);
                fxOverlay.launchTitanEnrageCutIn(computerBoardPanel, titan.getBoss());
                applyTitanEnvironmentImpact(TitanMode.environmentForEnrage(titan.getBoss()), false);
            }

            StringBuilder st = new StringBuilder();
            if (outcome.enragedJustTriggered()) {
                st.append("PHASE 2 CUỒNG BẠO! ").append(titan.getBoss().displayName)
                        .append(" BẬT +").append(titan.getEnrageBarrierHp()).append(" GIÁP & 5 TUYỆT KỸ! ");
            } else if (info.brokenPartNow() != null) {
                String crippleDesc = switch (info.brokenPartNow()) {
                    case HEAD -> "Boss tụ chậm +30%, tia hẹp hơn & -25% sát thương khiên";
                    case CORE_FIN -> "Boss chậm -20% & giảm nửa vùng hazard";
                    case BODY_TAIL -> "Boss mất đẩy sóng & CẤM LẶN ẨN";
                };
                st.append("PHÁ VỠ ").append(info.brokenPartNow().label.toUpperCase())
                        .append("! ").append(crippleDesc).append(" (-").append(info.damageDealt()).append(" HP)! ");
            } else if (info.staggerTriggered()) {
                st.append("★ NGẮT CHIÊU BOSS thành công — CHOÁNG (+20% sát thương)! ");
            } else if (!titan.getLastReactionLabel().isEmpty()) {
                st.append("★ PHẢN ỨNG ").append(titan.getLastReactionLabel())
                        .append(" (-").append(info.damageDealt()).append(" HP)! ");
            } else if (anyHit) {
                st.append("Đánh trúng -").append(info.damageDealt()).append(" HP")
                        .append(info.primaryPart() != null ? " [" + info.primaryPart().label + "]" : "")
                        .append(info.submergedMitigated() ? " (Bị Lặn giảm giáp)" : "!");
            } else {
                st.append("Bắn trượt Boss — ngắm đúng hình thể Kaiju đang di chuyển! ");
            }

            if (titanBattleMode && !sandboxMode) {
                syncRaidBoardTelemetry();
                showMessage(st.toString().trim(),
                        info.staggerTriggered() ? new Color(0x22D3EE)
                                : (outcome.enragedJustTriggered() ? titan.getBoss().enrageColor : UiTokens.STATUS_SUCCESS));
                return;
            }

            // Sandbox Titan manual counter-turn behavior preserved
            if (magic == CombatLoadout.MagicOption.FROST && anyHit) enemyFrozenTurns = 1;
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
            if (sandboxEnemyCounter) {
                Color statusCol = outcome.enragedJustTriggered() ? titan.getBoss().enrageColor
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
        };

        if (coopMode && shooterIdx == 2) {
            fxOverlay.launchCoopWingmanStrike(playerBoardPanel, computerBoardPanel, row, col, cells, weapon, magic, anyHit, afterFx);
        } else {
            fxOverlay.launchStrike(playerBoardPanel, computerBoardPanel, row, col, cells, weapon, magic, anyHit, afterFx);
        }
    }

    private void onBossBoardRightClick(int row, int col) {
        if (!coopMode || gameOver || titan == null) return;
        SoundEngine.playUiClick();
        int senderIdx = coopSession != null ? coopSession.localPlayerIndex() : 1;
        applyCoopPing(senderIdx, row, col);
        if (coopSession != null && coopSession.isNetworkMode()) {
            coopSession.sendPacket("CMD_PING", senderIdx, row, col);
        }
    }

    private void applyCoopPing(int senderIdx, int row, int col) {
        if (row < 0 || row >= Board.SIZE || col < 0 || col >= Board.SIZE) return;
        this.coopLastPingRow = row;
        this.coopLastPingCol = col;
        this.coopLastPingUntilMs = System.currentTimeMillis() + 6500L;
        String senderTag = coopSession != null
                ? (senderIdx == 2 ? coopSession.getConfig().p2Name() : coopSession.getConfig().p1Name())
                : ("P" + senderIdx);
        Color pingCol = (senderIdx == 2) ? new Color(0x34D399) : new Color(0x38BDF8);
        fxOverlay.setCoopPingBeacon(computerBoardPanel, row, col, "PING · " + senderTag, pingCol, 6500L);
        showMessage("PING TỌA ĐỘ (" + coordinate(row, col) + ") từ " + senderTag
                + " — Bắn gần điểm Ping để +28% Hợp Kích!", pingCol);
    }

    private void applyCoopSynergyChargeFromHit(int shooterIdx, int row, int col) {
        long now = System.currentTimeMillis();
        boolean nearPing = now < coopLastPingUntilMs
                && Math.max(Math.abs(row - coopLastPingRow), Math.abs(col - coopLastPingCol)) <= 1;
        int gain = nearPing ? 28 : 16;
        coopSynergyPct = Math.min(100, coopSynergyPct + gain);
        if (nearPing) {
            computerBoardPanel.addFloatingPopup(Math.max(0, row - 1), col, "PHỐI HỢP +28%!", new Color(0x34D399), true);
        }
    }

    /** Triggers "Song Long Hợp Kích" 2-Player fusion blast at hovered Boss cell or Boss center. */
    public boolean triggerCoopSynergyUltimate() {
        if (!coopMode || gameOver || titan == null) return false;
        if (coopSynergyPct < 100) {
            showMessage("Hợp Kích " + coopSynergyPct + "%/100% — Bắn trúng Boss để nạp!",
                    UiTokens.STATUS_WARNING);
            return false;
        }
        int hr = computerBoardPanel.getHoveredRow();
        int hc = computerBoardPanel.getHoveredCol();
        int tr = (hr >= 0 && hr < Board.SIZE) ? hr : Math.max(0, Math.min(Board.SIZE - 1, 3 + titan.getBossOffR()));
        int tc = (hc >= 0 && hc < Board.SIZE) ? hc : Math.max(0, Math.min(Board.SIZE - 1, 4 + titan.getBossOffC()));

        if (coopSession != null && coopSession.isNetworkMode() && !coopSession.isHostSide()) {
            coopSession.sendPacket("CMD_SYNERGY", 2, tr, tc);
            showMessage("ĐÃ PHÓNG TÍN HIỆU SONG LONG HỢP KÍCH!", new Color(0xFDE047));
            return true;
        }
        executeCoopSynergyLocal(coopSession != null ? coopSession.localPlayerIndex() : 1, tr, tc,
                coopSession != null && coopSession.isNetworkMode());
        return true;
    }

    private void executeCoopSynergyLocal(int initiatorIdx, int centerRow, int centerCol, boolean broadcastFromHost) {
        if (!coopMode || gameOver || titan == null) return;
        coopSynergyPct = 0;
        coopSynergyUsedCount++;
        TitanBattleController.PlayerStrikeOutcome out = titan.executeCoopSynergyBlast(centerRow, centerCol);
        TitanBattleController.RaidStrikeInfo info = titan.getLastStrikeInfo();
        mana = Math.min(currentMaxMana(), mana + 2);
        titan.activateSuperArmorForLastShip();
        playerBoardPanel.setSuperArmorGrid(titan.getSuperArmorGrid());
        SoundEngine.playSuperShipHyperCannon();

        if (broadcastFromHost && coopSession != null) {
            coopSession.sendPacket("EVT_SYNERGY", initiatorIdx, centerRow, centerCol,
                    info.damageDealt(), titan.getRemainingHp());
            broadcastCoopSyncSnapshot();
        }
        List<int[]> blastCells = new ArrayList<>(out.hitCells());
        blastCells.addAll(out.missCells());
        syncRaidBoardTelemetry();
        refreshLoadout();

        fxOverlay.launchCoopSynergyStrike(playerBoardPanel, computerBoardPanel, centerRow, centerCol,
                blastCells, out.bossDefeated(), () -> {
                    SoundEngine.playRaidStaggerBreak();
                    computerBoardPanel.triggerShake(10);
                    computerBoardPanel.triggerTitanHitFlash(new Color(0x34D399));
                    computerBoardPanel.addFloatingPopup(centerRow, centerCol,
                            "-" + info.damageDealt() + " HỢP KÍCH!!", new Color(0xFDE047), true);
                    handleNewlyBrokenParts(centerRow, centerCol);
                    playerBoardPanel.setTelegraphCells(List.of(), 0.0, UiTokens.STATUS_DANGER);
                    syncRaidBoardTelemetry();
                    refreshShots();
                    if (out.bossDefeated()) {
                        finishTitanBattle(true);
                    } else if (out.enragedJustTriggered()) {
                        raidPauseUntilMs = System.currentTimeMillis() + 1450L;
                        SoundEngine.playTitanEnrageTransform(titan.getBoss());
                        fxOverlay.launchTitanEnrageCutIn(computerBoardPanel, titan.getBoss());
                    } else {
                        showMessage("★ SONG LONG HỢP KÍCH -" + info.damageDealt()
                                + " HP! Boss CHOÁNG & Hạm đội +2 Mana + Giáp!", new Color(0x34D399));
                    }
                });
    }

    private void stepCoopAiWingman() {
        if (!coopMode || gameOver || titan == null || isRaidClockPaused()) return;
        if (coopSession == null || coopSession.getTransport() != CoopSession.CoopTransport.AI_ALLY) return;
        if (titan.getRemainingHp() <= 0) return;

        int r = Math.max(0, Math.min(Board.SIZE - 1, 3 + titan.getBossOffR()));
        int c = Math.max(0, Math.min(Board.SIZE - 1, 4 + titan.getBossOffC()));
        List<int[]> weak = TitanMode.generateWeakpointCells(titan.getBoss(), titan.getBossOffR(), titan.getBossOffC());
        if (!weak.isEmpty()) {
            int[] pick = weak.get((int) (System.currentTimeMillis() % weak.size()));
            r = pick[0];
            c = pick[1];
        }
        if (coopSynergyPct >= 100 && (titan.isDpsCheckActive() || titan.getCastProgress() >= 0.45)) {
            executeCoopSynergyLocal(2, r, c, false);
            return;
        }
        if (System.currentTimeMillis() >= coopLastPingUntilMs) {
            applyCoopPing(2, r, c);
        }
        CombatLoadout.WeaponOption w = titan.isBossSubmerged()
                ? CombatLoadout.WeaponOption.CANNON
                : (titan.isDpsCheckActive() ? CombatLoadout.WeaponOption.AIRSTRIKE : CombatLoadout.WeaponOption.TORPEDO);
        CombatLoadout.MagicOption m = titan.isBossSubmerged()
                ? CombatLoadout.MagicOption.ARCANE_SONAR
                : (titan.isDpsCheckActive() ? CombatLoadout.MagicOption.LIGHTNING : CombatLoadout.MagicOption.FROST);
        coopActiveTurnPlayer = 2;
        resolveAndAnimateTitanStrike(2, r, c, w, m, targetCellsFor(w, r, c), false, false);
        coopActiveTurnPlayer = 1;
        syncRaidBoardTelemetry();
    }

    private void handleCoopNetworkPacket(String cmd, String[] args) {
        if (!coopMode || titan == null || gameOver) return;
        try {
            switch (cmd) {
                case "CMD_STRIKE" -> {
                    if (!coopSession.isHostSide() || args.length < 4) return;
                    CombatLoadout.WeaponOption w = CombatLoadout.WeaponOption.valueOf(args[0]);
                    CombatLoadout.MagicOption m = CombatLoadout.MagicOption.valueOf(args[1]);
                    int r = Integer.parseInt(args[2]);
                    int c = Integer.parseInt(args[3]);
                    resolveAndAnimateTitanStrike(2, r, c, w, m, targetCellsFor(w, r, c), false, true);
                }
                case "EVT_STRIKE" -> {
                    if (coopSession.isHostSide() || args.length < 10) return;
                    int shooter = Integer.parseInt(args[0]);
                    CombatLoadout.WeaponOption w = CombatLoadout.WeaponOption.valueOf(args[1]);
                    CombatLoadout.MagicOption m = CombatLoadout.MagicOption.valueOf(args[2]);
                    int r = Integer.parseInt(args[3]);
                    int c = Integer.parseInt(args[4]);
                    this.coopSynergyPct = Integer.parseInt(args[9]);
                    resolveAndAnimateTitanStrike(shooter, r, c, w, m, targetCellsFor(w, r, c), false, false);
                }
                case "CMD_PING" -> {
                    if (args.length >= 3) {
                        int sender = Integer.parseInt(args[0]);
                        int r = Integer.parseInt(args[1]);
                        int c = Integer.parseInt(args[2]);
                        applyCoopPing(sender, r, c);
                        if (coopSession.isHostSide()) {
                            coopSession.sendPacket("CMD_PING", sender, r, c);
                        }
                    }
                }
                case "CMD_SYNERGY" -> {
                    if (coopSession.isHostSide() && args.length >= 3 && coopSynergyPct >= 100) {
                        int initiator = Integer.parseInt(args[0]);
                        int r = Integer.parseInt(args[1]);
                        int c = Integer.parseInt(args[2]);
                        executeCoopSynergyLocal(initiator, r, c, true);
                    }
                }
                case "EVT_SYNERGY" -> {
                    if (!coopSession.isHostSide() && args.length >= 5) {
                        int initiator = Integer.parseInt(args[0]);
                        int r = Integer.parseInt(args[1]);
                        int c = Integer.parseInt(args[2]);
                        executeCoopSynergyLocal(initiator, r, c, false);
                    }
                }
                case "EVT_BOSS_CAST" -> {
                    if (!coopSession.isHostSide() && args.length >= 3) {
                        TitanMode.TitanSkill skill = TitanMode.TitanSkill.valueOf(args[0]);
                        int r = Integer.parseInt(args[1]);
                        int c = Integer.parseInt(args[2]);
                        executeRaidBossCast(skill, r, c, TitanMode.skillTargetCells(skill, r, c, false));
                    }
                }
                case "EVT_SYNC" -> {
                    if (!coopSession.isHostSide() && args.length >= 9) {
                        titan.applyAuthoritativeSnapshot(
                                Integer.parseInt(args[0]),
                                "1".equals(args[1]),
                                "1".equals(args[2]),
                                Integer.parseInt(args[3]),
                                Integer.parseInt(args[4]),
                                Integer.parseInt(args[5]),
                                Integer.parseInt(args[6]),
                                Integer.parseInt(args[7])
                        );
                        this.coopSynergyPct = Math.max(0, Math.min(100, Integer.parseInt(args[8])));
                        syncRaidBoardTelemetry();
                    }
                }
                case "EVT_END" -> {
                    if (!coopSession.isHostSide() && args.length >= 1) {
                        finishTitanBattle("1".equals(args[0]));
                    }
                }
            }
        } catch (Exception ignored) {
            // Protect UI thread from malformed transient packets
        }
    }

    private void broadcastCoopSyncSnapshot() {
        if (!coopMode || coopSession == null || !coopSession.isNetworkMode() || !coopSession.isHostSide() || titan == null) {
            return;
        }
        coopSession.sendPacket("EVT_SYNC",
                titan.getRemainingHp(),
                titan.isEnraged() ? 1 : 0,
                titan.isBossSubmerged() ? 1 : 0,
                titan.getBossOffR(),
                titan.getBossOffC(),
                titan.getPartHp(TitanMode.TitanPart.HEAD),
                titan.getPartHp(TitanMode.TitanPart.CORE_FIN),
                titan.getPartHp(TitanMode.TitanPart.BODY_TAIL),
                coopSynergyPct);
    }

    public boolean isCoopMode() { return coopMode; }
    public int getCoopSynergyPct() { return coopSynergyPct; }
    public int getCoopSynergyUsedCount() { return coopSynergyUsedCount; }
    public CoopSession getCoopSession() { return coopSession; }

    /** Ends active boss/super-ship cutscenes immediately so interactive or automated tests can act without delay. */
    public void skipCutscenesForTest() {
        fxOverlay.skipCutscenesForTest();
        raidPauseUntilMs = 0L;
        superShipCutscenePlaying = false;
        turnLocked = false;
        syncRaidBoardTelemetry();
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
        applyBoardImpactVisuals(
                row, col, cells, weapon, magic,
                outcome.aggregateResult(), outcome.hitCells(), outcome.missCells(),
                outcome.burnCell(), outcome.sonarRevealedCells());
    }

    private void applyBoardImpactVisuals(
            int row,
            int col,
            List<int[]> cells,
            CombatLoadout.WeaponOption weapon,
            CombatLoadout.MagicOption magic,
            ShotResult aggResult,
            List<int[]> hitCells,
            List<int[]> missCells,
            int[] burnCell,
            List<int[]> sonarRevealedCells
    ) {
        SoundEngine.playImpact(aggResult, false);
        computerBoardPanel.setLastShot(row, col);

        if (weapon == CombatLoadout.WeaponOption.CANNON && magic == CombatLoadout.MagicOption.NONE) {
            computerBoardPanel.playEffect(
                    aggResult == ShotResult.MISS ? SpriteAssets.FxType.MISS : SpriteAssets.FxType.HIT,
                    row, col, aggResult == ShotResult.MISS ? UiTokens.STEEL_EDGE : UiTokens.HIT_CRIMSON, 0L);
            if (!hitCells.isEmpty()) {
                computerBoardPanel.triggerShake(aggResult == ShotResult.SUNK ? 6 : 4);
            }
        } else if (magic == CombatLoadout.MagicOption.NONE) {
            if (!hitCells.isEmpty()) {
                computerBoardPanel.playEffects(SpriteAssets.FxType.HIT, hitCells, UiTokens.HIT_CRIMSON, 45L);
            }
            if (!missCells.isEmpty()) {
                computerBoardPanel.playEffects(SpriteAssets.FxType.MISS, missCells, UiTokens.STEEL_EDGE, 40L);
            }
            if (weapon == CombatLoadout.WeaponOption.AIRSTRIKE) {
                computerBoardPanel.addImpactZone(row, col, cells, CombatLoadout.MagicOption.NONE, null, false);
            }
            int shake = switch (weapon) {
                case TORPEDO -> hitCells.isEmpty() ? 2 : 5;
                case MISSILE_CROSS -> hitCells.isEmpty() ? 3 : 6;
                case AIRSTRIKE -> 7;
                case CANNON -> 4;
            };
            computerBoardPanel.triggerShake(shake);
        } else {
            triggerHybridImpact(computerBoardPanel, row, col, cells, weapon, magic, aggResult);
            if (!hitCells.isEmpty()) {
                computerBoardPanel.playEffects(SpriteAssets.FxType.HIT, hitCells, UiTokens.HIT_CRIMSON, 45L);
            }
        }

        if (burnCell != null) {
            int br = burnCell[0];
            int bc = burnCell[1];
            computerBoardPanel.playEffect(SpriteAssets.FxType.FIRE, br, bc, UiTokens.MAGIC_FIRE, 180L);
            computerBoardPanel.playEffect(SpriteAssets.FxType.HIT, br, bc, UiTokens.HIT_CRIMSON, 230L);
        }
        if (sonarRevealedCells != null && !sonarRevealedCells.isEmpty()) {
            mergeRevealedCells(sonarRevealedCells);
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
            if (pendingSandboxAiTimer != null) {
                pendingSandboxAiTimer.stop();
                pendingSandboxAiTimer = null;
            }
            pendingSandboxAiTimer = new Timer(460, e -> {
                pendingSandboxAiTimer = null;
                takeSandboxCounterTurn(null);
            });
            pendingSandboxAiTimer.setRepeats(false);
            pendingSandboxAiTimer.start();
        } else {
            showMessage(status.toString().trim(), anyHit ? UiTokens.STATUS_SUCCESS : UiTokens.TEXT_PRIMARY);
            turnLocked = false;
        }
    }

    private void executeCampaignStrike(int row, int col) {
        if (campaignCtrl == null || gameOver) return;
        if (campaignCtrl.getRemainingShots() <= 0) {
            showMessage("Đã hết đạn giới hạn!", UiTokens.STATUS_DANGER);
            finishCampaignBattle(false, "Đã dùng hết số phát bắn giới hạn!");
            return;
        }
        int cost = currentManaCost();
        if (cost > mana) {
            showMessage("Không đủ Hải Năng (cần " + cost + ", hiện có " + mana + ").", UiTokens.STATUS_DANGER);
            return;
        }
        CombatLoadout.WeaponOption weapon = selectedWeapon;
        CombatLoadout.MagicOption magic = selectedMagic;
        List<int[]> cells = targetCells(row, col);
        CampaignBattleController.StrikeOutcome outcome = campaignCtrl.firePlayerStrike(row, col, cells, magic);
        if (outcome.aggregateResult() == ShotResult.ALREADY_SHOT) {
            showMessage("Ô/vùng " + coordinate(row, col) + " đã bắn rồi — hãy chọn mục tiêu khác.",
                    UiTokens.STATUS_WARNING);
            return;
        }
        turnLocked = true;
        mana = Math.max(0, mana - cost);
        if (superShipActive && hyperCannonCooldown > 0) hyperCannonCooldown--;
        syncCampaignHud();
        computerBoardPanel.clearTargetPreview();
        boolean anyHit = !outcome.hitCells().isEmpty();
        Color msgCol = magic != CombatLoadout.MagicOption.NONE ? magic.accentColor : campaignStage.ruleType().accent;
        showMessage("Màn " + campaignStage.stageNumber() + ": khai hỏa " + weapon.label + " + " + magic.label
                + " vào " + coordinate(row, col) + "...", msgCol);
        SoundEngine.playWeaponMagicLaunch(weapon, magic);

        fxOverlay.launchStrike(playerBoardPanel, computerBoardPanel, row, col, cells, weapon, magic, anyHit, () -> {
            if (gameOver) return;
            applyBoardImpactVisuals(
                    row, col, cells, weapon, magic,
                    outcome.aggregateResult(), outcome.hitCells(), outcome.missCells(),
                    outcome.burnCell(), outcome.sonarRevealedCells());
            int cap = currentMaxMana();
            int regen = superShipActive ? (anyHit ? 3 : 2) : (anyHit ? 2 : 1);
            mana = Math.min(cap, mana + regen);
            if (anyHit) {
                gainFleetSpirit(!outcome.newlySunkShips().isEmpty() ? 24 : 14);
            }
            refreshLoadout();
            updateSuperShipButtonUi();
            syncCampaignHud();

            if (campaignCtrl.allEnemySunk()) {
                finishCampaignBattle(true, null);
                return;
            }
            if (campaignCtrl.isOutOfAmmo()) {
                finishCampaignBattle(false, "Hết " + campaignStage.shotLimit() + " phát bắn giới hạn!");
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
                status.append("Trúng ").append(outcome.hitCells().size()).append(" ô! ");
            } else {
                status.append("Trượt mục tiêu. ");
            }
            if (magic == CombatLoadout.MagicOption.FROST && anyHit) {
                enemyFrozenTurns = 1;
                status.append("Đóng băng địch 1 lượt! ");
            }
            if (magic == CombatLoadout.MagicOption.LIGHTNING && anyHit) {
                showMessage(status + "Sét choáng — bắn tiếp ngay!", UiTokens.MAGIC_LIGHTNING);
                turnLocked = false;
                return;
            }
            if (enemyFrozenTurns > 0) {
                enemyFrozenTurns--;
                showMessage(status + "(Địch đang bị đóng băng)", UiTokens.MAGIC_FROST);
                turnLocked = false;
                return;
            }
            showMessage(status + "Địch chuẩn bị phản công...",
                    anyHit ? UiTokens.STATUS_SUCCESS : UiTokens.TEXT_PRIMARY);
            if (pendingCampaignAiTimer != null) pendingCampaignAiTimer.stop();
            pendingCampaignAiTimer = new Timer(450, ev -> takeCampaignAiTurn());
            pendingCampaignAiTimer.setRepeats(false);
            pendingCampaignAiTimer.start();
        });
    }

    private void takeCampaignAiTurn() {
        if (!campaignMode || campaignCtrl == null || gameOver) {
            turnLocked = false;
            return;
        }
        int[] target = campaignCtrl.pickAiTargetCell();
        if (target == null) {
            turnLocked = false;
            return;
        }
        int tr = target[0];
        int tc = target[1];
        List<ZombieMode.ZombieClass> aliveZ = campaignStage.ruleType() == CampaignMode.StageRuleType.ZOMBIE_ASSAULT
                ? campaignCtrl.aliveEnemyZombieClasses()
                : List.of();
        if (!aliveZ.isEmpty() && campaignCtrl.getShotsUsed() % 2 == 1) {
            ZombieMode.ZombieClass zc = aliveZ.get(campaignCtrl.getShotsUsed() % aliveZ.size());
            List<int[]> cells = ZombieMode.skillTargetCells(zc, tr, tc, true);
            boolean shielded = playerBoardPanel.isCellShielded(tr, tc);
            showMessage(zc.title + " tung chiêu " + zc.skillName + " vào " + coordinate(tr, tc) + "...", zc.accentColor);
            SoundEngine.playZombieSkill(zc);
            fxOverlay.launchZombieStrike(computerBoardPanel, playerBoardPanel, zc, tr, tc, cells, !shielded, () -> {
                if (!gameOver) resolveCampaignZombieImpact(zc, tr, tc, cells);
            });
            return;
        }

        boolean shielded = playerBoardPanel.isCellShielded(tr, tc);
        CampaignBattleController.AiShotOutcome shot = campaignCtrl.fireAiAtPlayer(tr, tc, shielded);
        boolean hit = shot.result() == ShotResult.HIT || shot.result() == ShotResult.SUNK;
        showMessage("Địch khai hỏa vào " + coordinate(tr, tc) + "...", UiTokens.STATUS_WARNING);
        SoundEngine.playWeaponMagicLaunch(CombatLoadout.WeaponOption.CANNON, CombatLoadout.MagicOption.NONE);
        fxOverlay.launchStrike(computerBoardPanel, playerBoardPanel, tr, tc,
                CombatLoadout.WeaponOption.CANNON, CombatLoadout.MagicOption.NONE, hit && !shielded, () -> {
                    if (gameOver) return;
                    playerBoardPanel.setLastShot(tr, tc);
                    if (shielded) {
                        handleShieldImpact(tr, tc, 20, UiTokens.ACCENT_SIGNAL, "Pháo địch tại " + coordinate(tr, tc));
                    } else if (shot.absorbedByArmor()) {
                        SoundEngine.playImpact(ShotResult.HIT, false);
                        playerBoardPanel.playEffect(SpriteAssets.FxType.SONAR, tr, tc, new Color(0xFACC15), 0L);
                        playerBoardPanel.triggerShake(3);
                        showMessage("Giáp Vàng Siêu Tàu chặn đòn tại " + coordinate(tr, tc) + "!", new Color(0xFDE047));
                    } else {
                        SoundEngine.playImpact(shot.result(), false);
                        playerBoardPanel.playEffect(shot.result() == ShotResult.MISS ? SpriteAssets.FxType.MISS : SpriteAssets.FxType.HIT,
                                tr, tc, hit ? UiTokens.HIT_CRIMSON : UiTokens.STEEL_EDGE, 0L);
                        if (hit) {
                            playerBoardPanel.triggerShake(shot.result() == ShotResult.SUNK ? 6 : 4);
                            gainFleetSpirit(shot.result() == ShotResult.SUNK ? 18 : 10);
                        }
                        showMessage("Địch bắn " + coordinate(tr, tc) + ": " + resultText(shot.result()) + ".",
                                hit ? UiTokens.STATUS_DANGER : UiTokens.TEXT_PRIMARY);
                    }
                    playerBoardPanel.refresh();
                    refreshShots();
                    if (campaignCtrl.allPlayerSunk()) {
                        finishCampaignBattle(false, "Hạm đội của bạn đã bị đánh chìm!");
                    } else {
                        turnLocked = false;
                    }
                });
    }

    private void resolveCampaignZombieImpact(ZombieMode.ZombieClass zc, int tr, int tc, List<int[]> cells) {
        List<int[]> hitCells = new ArrayList<>();
        List<int[]> missCells = new ArrayList<>();
        List<int[]> blockedCells = new ArrayList<>();
        List<int[]> armorCells = new ArrayList<>();
        ShotResult worst = ShotResult.MISS;
        for (int[] c : cells) {
            boolean shield = playerBoardPanel.isCellShielded(c[0], c[1]);
            CampaignBattleController.AiShotOutcome out = campaignCtrl.fireAiAtPlayer(c[0], c[1], shield);
            if (shield) blockedCells.add(c);
            else if (out.absorbedByArmor()) armorCells.add(c);
            else if (out.result() == ShotResult.HIT || out.result() == ShotResult.SUNK) {
                hitCells.add(c);
                if (out.result() == ShotResult.SUNK) worst = ShotResult.SUNK;
                else if (worst != ShotResult.SUNK) worst = ShotResult.HIT;
            } else if (out.result() == ShotResult.MISS) {
                missCells.add(c);
            }
        }
        playerBoardPanel.setLastShot(tr, tc);
        if (!hitCells.isEmpty()) playerBoardPanel.playEffects(zc.impactFx, hitCells, zc.accentColor, 48L);
        if (!missCells.isEmpty()) playerBoardPanel.playEffects(zc.impactFx, missCells, zc.accentColor, 48L);
        if (!armorCells.isEmpty()) playerBoardPanel.playEffects(SpriteAssets.FxType.SONAR, armorCells, new Color(0xFACC15), 36L);
        if (!blockedCells.isEmpty()) {
            handleShieldImpact(tr, tc, zombieShieldDamage(zc), zc.accentColor, zc.skillName);
            for (int[] bc : blockedCells) {
                if (bc[0] != tr || bc[1] != tc) playerBoardPanel.triggerShieldBlock(bc[0], bc[1], 2);
            }
        } else {
            SoundEngine.playZombieImpact(zc, !hitCells.isEmpty() ? worst : ShotResult.MISS, false);
            if (!hitCells.isEmpty()) {
                playerBoardPanel.triggerShake(5);
                gainFleetSpirit(14);
                showMessage(zc.title + " dùng " + zc.skillName + " trúng " + hitCells.size() + " ô!", zc.accentColor);
            } else {
                showMessage(zc.title + " dùng " + zc.skillName + " bắn trượt!", zc.accentColor);
            }
        }
        playerBoardPanel.refresh();
        refreshShots();
        if (campaignCtrl.allPlayerSunk()) {
            finishCampaignBattle(false, "Hạm đội bị hạm đội Zombie tiêu diệt!");
        } else {
            turnLocked = false;
        }
    }

    private void finishCampaignBattle(boolean won, String failReason) {
        if (!campaignMode || campaignCtrl == null || gameOver) return;
        gameOver = true;
        turnLocked = true;
        stopAllTimers();
        playerBoardPanel.setInteractive(false);
        computerBoardPanel.setInteractive(false);
        computerBoardPanel.clearTargetPreview();
        computerBoardPanel.setShowShips(true);
        playerBoardPanel.refresh();
        computerBoardPanel.refresh();
        refreshShots();
        SoundEngine.playGameOver(won);

        int elapsed = campaignElapsedSeconds();
        int rem = campaignRemainingSeconds();
        CampaignBattleController.CampaignDebriefSummary raw =
                campaignCtrl.buildDebriefSummary(won, failReason, elapsed, rem);
        if (won) {
            CampaignMode.recordVictory(campaignStage.stageNumber(), raw.starsEarned(), raw.score());
            showMessage("HOÀN THÀNH " + campaignStage.title().toUpperCase() + " — ★ " + raw.starsEarned() + "/3!",
                    UiTokens.STATUS_SUCCESS);
        } else {
            showMessage("THẤT BẠI (" + campaignStage.title() + ") — "
                    + (failReason != null ? failReason : "Hãy thử lại chiến thuật khác!"), UiTokens.STATUS_DANGER);
        }
        syncCampaignHud();
        CampaignBattleController.CampaignDebriefSummary finalSum = new CampaignBattleController.CampaignDebriefSummary(
                raw.stage(), raw.won(), raw.failReason(), raw.starsEarned(),
                CampaignMode.getStars(campaignStage.stageNumber()),
                raw.shotsUsed(), raw.shotLimit(), raw.hitsScored(),
                raw.hullPct(), raw.elapsedSec(), raw.remainingSec(), raw.score());
        if (won && StoryMode.hasPostVictoryEpilogue(campaignStage.stageNumber())) {
            storyOverlay.showEpisode(StoryMode.epilogueEpisodeForStage(campaignStage),
                    () -> raidModalPanel.showCampaignDebrief(finalSum));
        } else {
            raidModalPanel.showCampaignDebrief(finalSum);
        }
    }

    private void stepCampaignClock() {
        if (!campaignMode || campaignStage == null || gameOver) {
            if (campaignClockTimer != null) campaignClockTimer.stop();
            return;
        }
        if (raidModalPanel.isVisible() || superShipCutscenePlaying
                || fxOverlay.isBossCityRampageActive()
                || fxOverlay.isTitanSkillPovActive()
                || fxOverlay.isShipSkillCutsceneActive()
                || storyOverlay.isStoryActive()) {
            campaignStartedMs += 250L;
            return;
        }
        syncCampaignHud();
        if (campaignStage.timeLimitSec() > 0 && campaignRemainingSeconds() <= 0) {
            finishCampaignBattle(false, "Hết thời gian " + campaignStage.timeLimitSec() + " giây!");
        }
    }

    private int campaignElapsedSeconds() {
        if (campaignStartedMs <= 0L) return 0;
        return Math.max(0, (int) ((System.currentTimeMillis() - campaignStartedMs) / 1000L));
    }

    private int campaignRemainingSeconds() {
        if (campaignStage == null || campaignStage.timeLimitSec() <= 0) return 0;
        return Math.max(0, campaignStage.timeLimitSec() - campaignElapsedSeconds());
    }

    private void syncCampaignHud() {
        if (!campaignMode || campaignStage == null) return;
        StringBuilder sb = new StringBuilder("M").append(campaignStage.stageNumber()).append("/100");
        double progress = -1.0;
        if (campaignStage.shotLimit() > 0 && campaignCtrl != null) {
            int remShots = campaignCtrl.getRemainingShots();
            sb.append(" · Đạn: ").append(remShots).append("/").append(campaignStage.shotLimit());
            progress = Math.max(0.0, Math.min(1.0, remShots / (double) campaignStage.shotLimit()));
        } else if (campaignStage.timeLimitSec() > 0) {
            int remSec = campaignRemainingSeconds();
            sb.append(String.format(" · ⏱ %02d:%02d", remSec / 60, remSec % 60));
            progress = Math.max(0.0, Math.min(1.0, remSec / (double) campaignStage.timeLimitSec()));
        } else if (campaignStage.initialSonarHints() > 0) {
            sb.append(" · Sonar Mù");
        } else {
            sb.append(" · ").append(campaignStage.ruleType().shortLabel);
        }
        fxOverlay.setCampaignHudTelemetry(true, sb.toString(), campaignStage.ruleType().accent, progress);
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
        fxOverlay.resetTitanSkillPovCooldown();
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
        if (env.mapTheme() != null && !(titanBattleMode && !sandboxMode)
                && OceanMapBackdrop.getMapTheme() != env.mapTheme()) {
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

    private boolean isRaidClockPaused() {
        return !titanBattleMode || gameOver || superShipCutscenePlaying
                || raidModalPanel.isVisible()
                || storyOverlay.isStoryActive()
                || fxOverlay.isBossCityRampageActive()
                || fxOverlay.isTitanSkillPovActive()
                || fxOverlay.isTitanFollowupActive()
                || fxOverlay.isShipSkillCutsceneActive()
                || fxOverlay.isSuperShipCutsceneActive()
                || System.currentTimeMillis() < raidPauseUntilMs;
    }

    private void playTitanSkillAudioForLaunch(TitanMode.TitanSkill skill) {
        if (skill == null) return;
        if (fxOverlay != null && fxOverlay.willPlayTitanSkillPov(skill)) {
            SoundEngine.playTitanSkillPov(skill.boss, skill);
        } else {
            SoundEngine.playTitanSkill(skill.boss, skill);
        }
    }

    private void stepRaidTick() {
        long now = System.currentTimeMillis();
        long dtMs = Math.max(1L, Math.min(100L, lastRaidTickMs > 0 ? (now - lastRaidTickMs) : 35L));
        lastRaidTickMs = now;
        if (!titanBattleMode || titan == null || gameOver) return;

        // Super Ship active weapon cooldown tick cleanly on each frame
        if (superShipActive && hyperCannonCooldownMs > 0 && !isRaidClockPaused()) {
            int prevSec = hyperCannonCooldown;
            hyperCannonCooldownMs = Math.max(0L, hyperCannonCooldownMs - dtMs);
            hyperCannonCooldown = hyperCannonCooldownMs <= 0L ? 0 : (int) Math.ceil(hyperCannonCooldownMs / 5000.0);
            if (hyperCannonCooldown != prevSec) updateSuperShipButtonUi();
        }
        refreshWeaponCooldownChips(now);

        if (isRaidClockPaused()) {
            if (campaignMode && campaignStage != null && campaignStage.timeLimitSec() > 0) {
                campaignStartedMs += dtMs;
            }
            return;
        }
        if (campaignMode && campaignStage != null) {
            syncCampaignHud();
            if (campaignStage.timeLimitSec() > 0 && campaignRemainingSeconds() <= 0) {
                finishTitanBattle(false);
                return;
            }
        }

        // 1. Continuous Mana Regen scaled by Cruiser Passive / Super Ship / Hyperion Relic
        int cap = currentMaxMana();
        if (mana < cap) {
            double ratePerSec = (superShipActive ? 0.82 : 0.42) * titan.effectiveManaRegenMult(superShipActive);
            manaRegenFrac += ratePerSec * (dtMs / 1000.0);
            if (manaRegenFrac >= 1.0) {
                int gain = (int) manaRegenFrac;
                manaRegenFrac -= gain;
                mana = Math.min(cap, mana + gain);
                refreshLoadout();
            }
        } else {
            manaRegenFrac = 0.0;
        }

        // 2. Step Boss ATB Cast Bar, Repositioning, Submerge Stealth, Burn, Arena Props & Directives
        boolean allowBossClock = !coopMode || coopSession == null || coopSession.isHostSide();
        TitanBattleController.RaidTickResult tick = titan.stepRaidSimulation(dtMs, allowBossClock);
        if (coopMode && allowBossClock && coopSession != null && coopSession.isNetworkMode()) {
            long nowSync = System.currentTimeMillis();
            if (tick.bossMoved() || tick.bossSubmergedNow() || tick.bossSurfacedNow() || nowSync - coopLastBossSyncMs >= 420L) {
                coopLastBossSyncMs = nowSync;
                broadcastCoopSyncSnapshot();
            }
        }
        processTitanArenaEvents();
        syncRaidBoardTelemetry();
        if (tick.burnDotDamage() > 0) {
            computerBoardPanel.addFloatingPopup(3 + titan.getBossOffR(), 4 + titan.getBossOffC(),
                    "-" + tick.burnDotDamage() + " CHÁY", UiTokens.MAGIC_FIRE, false);
        }
        if (tick.bossMoved()) {
            computerBoardPanel.setTitanBossOffset(titan.getBossOffR(), titan.getBossOffC());
            playerBoardPanel.addFloatingPopup(0, 0, "", UiTokens.TEXT_MUTED, false);
            if (!revealedEnemyCells.isEmpty() && selectedMagic == CombatLoadout.MagicOption.ARCANE_SONAR) {
                computerBoardPanel.setRevealedCells(TitanMode.generateWeakpointCells(
                        titan.getBoss(), titan.getBossOffR(), titan.getBossOffC()));
            }
        }
        if (tick.bossSubmergedNow()) {
            computerBoardPanel.addFloatingPopup(4, 4, "BOSS LẶN ẨN! Dùng Cổ Ngữ!", new Color(0x2DD4BF), true);
            showMessage("CẢNH BÁO: " + titan.getBoss().shortLabel + " đã LẶN SÂU! Bắn Cổ Ngữ (Sonar) để lộ ×1.4 sát thương!",
                    new Color(0x2DD4BF));
        } else if (tick.bossSurfacedNow()) {
            computerBoardPanel.addFloatingPopup(4, 4, "TRỒI LÊN!", titan.getBoss().accentColor, false);
        }
        if (tick.hazardShipHits() > 0) {
            for (int[] hc : tick.hazardDamageCells()) {
                playerBoardPanel.playEffect(SpriteAssets.FxType.FIRE, hc[0], hc[1], UiTokens.MAGIC_FIRE, 0L);
                playerBoardPanel.addFloatingPopup(hc[0], hc[1], "-VÙNG CHÁY!", UiTokens.STATUS_DANGER, false);
            }
            playerBoardPanel.refresh();
            refreshShots();
            checkSuperShipReadyCue();
            if (playerBoardPanel.countAliveShips() <= 0) {
                finishTitanBattle(false);
                return;
            }
        }
        if (tick.bossDefeatedByDot()) {
            syncRaidBoardTelemetry();
            refreshShots();
            finishTitanBattle(true);
            return;
        }
        if (tick.castReadyToFire() != null) {
            executeRaidBossCast(tick.castReadyToFire(), tick.castTargetRow(), tick.castTargetCol(), tick.castTargetCells());
        } else {
            boolean nowDpsCheck = titan.isDpsCheckActive();
            if (nowDpsCheck && !prevDpsCheckActive) {
                SoundEngine.playRaidTelegraphWarning();
                computerBoardPanel.triggerShake(6);
                computerBoardPanel.addFloatingPopup(2, 4, "⚠ TỤ ĐÒN HỦY DIỆT!", new Color(0xEF4444), true);
                showMessage("⚠ BÁO ĐỘNG ĐỎ! Boss đang tụ chiêu HỦY DIỆT — Dồn hỏa lực vào LÕI để NGẮT CHIÊU!",
                        new Color(0xEF4444));
            } else if (titan.getCastProgress() >= 0.45 && titan.isPlayerShipInsideTelegraph()) {
                SoundEngine.playRaidTelegraphWarning();
            }
            prevDpsCheckActive = nowDpsCheck;
        }

        syncRaidBoardTelemetry();
    }

    private void executeRaidBossCast(
            TitanMode.TitanSkill skill,
            int targetRow,
            int targetCol,
            List<int[]> cells
    ) {
        if (titan == null || gameOver) return;
        List<int[]> effCells = (cells == null || cells.isEmpty())
                ? TitanMode.skillTargetCells(skill, targetRow, targetCol, false)
                : cells;
        if (coopMode && coopSession != null && coopSession.isNetworkMode() && coopSession.isHostSide()) {
            coopSession.sendPacket("EVT_BOSS_CAST", skill.name(), targetRow, targetCol);
        }
        long now = System.currentTimeMillis();
        List<int[]> effectiveShields = (playerBoardPanel.getShieldEnergy() > 0
                && (playerBoardPanel.isFullMapShield() || !playerBoardPanel.getShieldedCells().isEmpty()))
                ? (playerBoardPanel.isFullMapShield() ? allBoardCells() : playerBoardPanel.getShieldedCells())
                : List.of();
        boolean parryWindow = !sandboxMode
                && !effectiveShields.isEmpty()
                && (now - shieldActivatedAtMs) <= 850L;

        TitanBattleController.TitanSkillOutcome out = titan.resolveSkillImpactAt(
                skill, targetRow, targetCol, effCells, effectiveShields);
        List<int[]> hitCells = new ArrayList<>(out.hitCells());
        List<int[]> missCells = new ArrayList<>(out.missCells());
        List<int[]> blockedCells = new ArrayList<>(out.shieldBlockedCells());
        List<int[]> consumedReefs = new ArrayList<>(titan.consumeLastReefBlockedCells());
        List<int[]> absorbedArmor = new ArrayList<>(titan.consumeLastArmorAbsorbedCells());

        selectedTitanSkill = skill;
        refreshLoadout();
        playTitanSkillAudioForLaunch(skill);
        TitanMode.EnvironmentAction skillEnv = TitanMode.environmentForSkill(skill);
        applyTitanEnvironmentImpact(skillEnv, false);

        boolean anyHit = !hitCells.isEmpty();
        final boolean isPerfectParry = parryWindow && !blockedCells.isEmpty();
        fxOverlay.launchTitanSkill(
                computerBoardPanel, playerBoardPanel, skill, out.enraged(),
                out.targetRow(), out.targetCol(), effCells, anyHit, () -> {
                    if (isPerfectParry) {
                        playerBoardPanel.addShieldEnergy(12);
                        updateShieldButtonUi();
                        titan.applyShieldParryCounter(15, 20);
                        gainFleetSpirit(14);
                        SoundEngine.playRaidShieldParry();
                        fxOverlay.triggerShieldDeflect(
                                playerBoardPanel.getCellCenterIn(targetRow, targetCol, fxOverlay),
                                playerBoardPanel.getShieldEnergy(), BoardPanel.SHIELD_MAX_ENERGY, 0, new Color(0xFDE047));
                        playerBoardPanel.triggerShieldBlock(targetRow, targetCol, 3);
                        playerBoardPanel.addFloatingPopup(targetRow, targetCol, "★ PARRY! PHẢN -15 HP ★", new Color(0xFDE047), true);
                        computerBoardPanel.addFloatingPopup(3, 4, "-15 PARRY!", new Color(0xFDE047), true);
                        showMessage("★ PHẢN ĐÒN KHIÊN HOÀN HẢO! +12% NL Khiên & Phản -15 HP / +20 Choáng lên Boss!",
                                new Color(0xFDE047));
                    } else {
                        if (titan.isLastCastDpsCheck()) {
                            computerBoardPanel.addFloatingPopup(2, 4, "★ HẤP THỤ +30 HP · +24 GIÁP ★",
                                    new Color(0xEF4444), true);
                        }
                        int scaledShieldDmg = skill.shieldDamage + (titan.isLastCastDpsCheck() ? 25 : 0);
                        if (titan.isPartBroken(TitanMode.TitanPart.HEAD)) {
                            scaledShieldDmg = Math.round(scaledShieldDmg * 0.75f);
                        }
                        scaledShieldDmg = (int) Math.round(scaledShieldDmg
                                * titan.effectiveShieldDrainMult(superShipActive));
                        applyTitanSkillVisuals(skill, out.targetRow(), out.targetCol(), effCells,
                                hitCells, missCells, blockedCells, scaledShieldDmg);
                        processTitanArenaEvents();
                        if (!consumedReefs.isEmpty()) {
                            playerBoardPanel.playEffects(SpriteAssets.FxType.SONAR, consumedReefs, new Color(0x34D399), 34L);
                            playerBoardPanel.addFloatingPopup(consumedReefs.get(0)[0], consumedReefs.get(0)[1],
                                    "ĐÁ CHẮN ĐỠ!", new Color(0x34D399), true);
                        }
                        if (!anyHit && blockedCells.isEmpty() && absorbedArmor.isEmpty() && consumedReefs.isEmpty()) {
                            playerBoardPanel.addFloatingPopup(targetRow, targetCol, "NÉ ĐƯỢC!", new Color(0x34D399), true);
                        }
                    }
                    if (!absorbedArmor.isEmpty()) {
                        playerBoardPanel.playEffects(SpriteAssets.FxType.SONAR, absorbedArmor, new Color(0xFACC15), 32L);
                        playerBoardPanel.triggerShake(3);
                        playerBoardPanel.setSuperArmorGrid(titan.getSuperArmorGrid());
                    }
                    if (!out.newlySunkShips().isEmpty()) {
                        playerBoardPanel.triggerShake(10);
                        for (ShipType sunkType : out.newlySunkShips()) {
                            Ship sunkShip = findPlayerShipByType(sunkType);
                            if (sunkShip != null) {
                                fxOverlay.triggerShipSunkExplosion(
                                        playerBoardPanel, sunkShip.row, sunkShip.col,
                                        sunkShip.type.length, sunkShip.horizontal, skill.accentColor);
                            }
                            playerBoardPanel.addFloatingPopup(targetRow, targetCol,
                                    "★ CHÌM " + sunkType.displayName.toUpperCase() + "! ★",
                                    new Color(0xEF4444), true);
                        }
                    }
                    if (!blockedCells.isEmpty() && !isPerfectParry) {
                        gainFleetSpirit(10 * Math.min(2, blockedCells.size()));
                    }
                    if (anyHit || !out.newlySunkShips().isEmpty()) {
                        gainFleetSpirit(hitCells.size() * 10 + out.newlySunkShips().size() * 20);
                    }
                    // Apply wind push AFTER impact explosion settles so ships never teleport away from the incoming beam!
                    int[] push = TitanMode.windPushDelta(skillEnv);
                    if (titan.isWindPushDisabled()) {
                        if (push[0] != 0 || push[1] != 0) {
                            playerBoardPanel.addFloatingPopup(4, 4, "ĐUÔI VỠ · MIỄN ĐẨY SÓNG!", new Color(0x34D399), false);
                        }
                    } else if (!out.playerDefeated()) {
                        List<ShipType> drifted = titan.applyWindDrift(push[0], push[1]);
                        if (!drifted.isEmpty()) {
                            syncSuperShipPositionOnBoard();
                            playerBoardPanel.setSuperArmorGrid(titan.getSuperArmorGrid());
                            playerBoardPanel.addFloatingPopup(4, 4, "GIÓ ĐẨY " + drifted.size() + " TÀU!", new Color(0x7DD3FC), true);
                        }
                    }
                    playerBoardPanel.refresh();
                    syncRaidBoardTelemetry();
                    refreshShots();
                    checkSuperShipReadyCue();
                    if (titan.getRemainingHp() <= 0) {
                        finishTitanBattle(true);
                    } else if (out.playerDefeated()) {
                        finishTitanBattle(false);
                    }
                });
    }

    private void handleTitanPlayerBoardClick(int row, int col) {
        if (!titanBattleMode || titan == null || isRaidClockPaused()) return;
        Ship atCell = SandboxController.findShipAt(titan.getPlayerShips(), row, col);
        if (atCell != null && !SandboxController.isShipSunk(titan.getPlayerGrid(), atCell)) {
            if (selectedDodgeShip != atCell.type) {
                selectedDodgeShip = atCell.type;
                dodgeHorizontal = atCell.horizontal;
                playerBoardPanel.setSelectedDodgeShip(selectedDodgeShip);
                SoundEngine.playUiClick();
                updateTitanDodgePreview(row, col);
                showMessage("Đã chọn " + atCell.type.displayName
                        + " — Click ô khác để NÉ ĐÒN, phím R / Chuột phải xoay, WASD lướt!", UiTokens.ACCENT_SIGNAL);
                return;
            }
        }
        if (selectedDodgeShip == null) {
            showMessage("Click lên một chiếc tàu còn sống trên hải đồ trái để chọn & né đòn Boss!", UiTokens.STATUS_WARNING);
            return;
        }
        Ship cur = findPlayerShipByType(selectedDodgeShip);
        if (cur == null || SandboxController.isShipSunk(titan.getPlayerGrid(), cur)) {
            selectedDodgeShip = null;
            playerBoardPanel.setSelectedDodgeShip(null);
            playerBoardPanel.clearShipPreview();
            return;
        }
        boolean horiz = dodgeHorizontal;
        boolean moved = titan.movePlayerShip(selectedDodgeShip, row, col, horiz, false, superShipActive);
        if (!moved && titan.getMoveCooldownMs() <= 0) {
            // If clicking tail segment in place with rotated angle, or minor edge clamp:
            int altR = Math.min(row, horiz ? Board.SIZE - 1 : Board.SIZE - selectedDodgeShip.length);
            int altC = Math.min(col, horiz ? Board.SIZE - selectedDodgeShip.length : Board.SIZE - 1);
            moved = titan.movePlayerShip(selectedDodgeShip, altR, altC, horiz, false, superShipActive);
        }
        if (moved) {
            SoundEngine.playUiClick();
            processTitanArenaEvents();
            syncSuperShipPositionOnBoard();
            playerBoardPanel.setSuperArmorGrid(titan.getSuperArmorGrid());
            playerBoardPanel.clearShipPreview();
            playerBoardPanel.playEffect(SpriteAssets.FxType.MISS, row, col, UiTokens.ACCENT_SIGNAL, 0L);
            playerBoardPanel.addFloatingPopup(row, col, "CƠ ĐỘNG!", new Color(0x38BDF8), false);
            playerBoardPanel.refresh();
            syncRaidBoardTelemetry();
            showMessage("Đã điều động " + selectedDodgeShip.displayName + " sang " + coordinate(row, col) + "!",
                    UiTokens.STATUS_SUCCESS);
        } else if (titan.getMoveCooldownMs() > 0) {
            showMessage("Đang hồi cơ động hạm đội ("
                    + String.format(java.util.Locale.US, "%.1fs", titan.getMoveCooldownMs() / 1000.0) + ")!",
                    UiTokens.STATUS_WARNING);
        } else {
            showMessage("Không thể di chuyển " + selectedDodgeShip.displayName + " vào "
                    + coordinate(row, col) + " (vướng mép hoặc tàu khác).", UiTokens.STATUS_DANGER);
        }
    }

    private void updateTitanDodgePreview(int row, int col) {
        if (!titanBattleMode || titan == null || selectedDodgeShip == null) return;
        if (row < 0 || col < 0) {
            playerBoardPanel.clearShipPreview();
            return;
        }
        boolean valid = titan.canPlaceRelocatedShip(selectedDodgeShip, row, col, dodgeHorizontal);
        playerBoardPanel.setShipPreview(row, col, selectedDodgeShip, dodgeHorizontal, valid);
    }

    private void rotateSelectedDodgeShip() {
        if (!titanBattleMode || titan == null || selectedDodgeShip == null) return;
        dodgeHorizontal = !dodgeHorizontal;
        SoundEngine.playUiClick();
        Ship cur = findPlayerShipByType(selectedDodgeShip);
        int hr = playerBoardPanel.getHoveredRow();
        int hc = playerBoardPanel.getHoveredCol();
        if (hr >= 0 && hc >= 0) {
            updateTitanDodgePreview(hr, hc);
        } else if (cur != null && titan.movePlayerShip(selectedDodgeShip, cur.row, cur.col, dodgeHorizontal, false, superShipActive)) {
            processTitanArenaEvents();
            syncSuperShipPositionOnBoard();
            playerBoardPanel.setSuperArmorGrid(titan.getSuperArmorGrid());
            playerBoardPanel.refresh();
            syncRaidBoardTelemetry();
        }
        showMessage("Đã xoay hướng né " + selectedDodgeShip.displayName + ": "
                + (dodgeHorizontal ? "Ngang" : "Dọc") + ".", UiTokens.ACCENT_SIGNAL);
    }

    private void stepSelectedShipBy(int dr, int dc) {
        if (!titanBattleMode || titan == null || isRaidClockPaused()) return;
        Ship target = selectedDodgeShip != null ? findPlayerShipByType(selectedDodgeShip) : null;
        if (target == null || SandboxController.isShipSunk(titan.getPlayerGrid(), target)) {
            for (Ship s : titan.getPlayerShips()) {
                if (!SandboxController.isShipSunk(titan.getPlayerGrid(), s)) {
                    target = s;
                    selectedDodgeShip = s.type;
                    dodgeHorizontal = s.horizontal;
                    playerBoardPanel.setSelectedDodgeShip(selectedDodgeShip);
                    break;
                }
            }
        }
        if (target == null) return;
        if (titan.stepShipMoveWithDash(target.type, dr, dc, superShipActive)) {
            SoundEngine.playUiClick();
            processTitanArenaEvents();
            Ship after = findPlayerShipByType(target.type);
            int fxR = after != null ? after.row : Math.max(0, Math.min(Board.SIZE - 1, target.row + dr));
            int fxC = after != null ? after.col : Math.max(0, Math.min(Board.SIZE - 1, target.col + dc));
            syncSuperShipPositionOnBoard();
            playerBoardPanel.setSuperArmorGrid(titan.getSuperArmorGrid());
            playerBoardPanel.playEffect(SpriteAssets.FxType.MISS, fxR, fxC, UiTokens.ACCENT_SIGNAL, 0L);
            playerBoardPanel.refresh();
            syncRaidBoardTelemetry();
        }
    }

    private Ship findPlayerShipByType(ShipType type) {
        if (titan == null || type == null) return null;
        for (Ship s : titan.getPlayerShips()) {
            if (s.type == type) return s;
        }
        return null;
    }

    private void syncSuperShipPositionOnBoard() {
        if (!superShipActive || titan == null) return;
        for (Ship s : titan.getPlayerShips()) {
            if (!SandboxController.isShipSunk(titan.getPlayerGrid(), s)) {
                playerBoardPanel.activateSuperShip(s.row, s.col, s.type.length, s.horizontal);
                break;
            }
        }
    }

    private void processTitanArenaEvents() {
        if (titan == null) return;
        List<TitanBattleController.ArenaFeedbackEvent> events = titan.consumeArenaEvents();
        if (events.isEmpty()) return;
        long now = System.currentTimeMillis();
        boolean boardChanged = false;
        boolean hudDirty = false;
        for (TitanBattleController.ArenaFeedbackEvent ev : events) {
            if (ev.manaDelta() != 0) {
                mana = Math.min(currentMaxMana(), Math.max(0, mana + ev.manaDelta()));
                hudDirty = true;
            }
            if (ev.superDelta() != 0) {
                gainFleetSpirit(ev.superDelta());
                hudDirty = true;
            }
            if (ev.shieldDelta() != 0) {
                playerBoardPanel.addShieldEnergy(ev.shieldDelta());
                updateShieldButtonUi();
            }
            if (ev.healCells() > 0) {
                boardChanged = true;
            }
            if (ev.sonarCells() != null && !ev.sonarCells().isEmpty()) {
                mergeRevealedCells(ev.sonarCells());
                computerBoardPanel.setRevealedCells(revealedEnemyCells);
            }
            switch (ev.kind()) {
                case REEF_BLOCKED -> {
                    playerBoardPanel.triggerShake(3);
                    playerBoardPanel.addFloatingPopup(ev.row(), ev.col(), ev.label(), new Color(0x94A3B8), false);
                }
                case REEF_CRUMBLED -> {
                    fxOverlay.launchReefCrumble(playerBoardPanel, ev.row(), ev.col(), false);
                    playerBoardPanel.addFloatingPopup(ev.row(), ev.col(), ev.label(), new Color(0xCBD5E1), true);
                    showMessage(ev.label(), new Color(0xCBD5E1));
                }
                case GRAVITY_SHATTER -> {
                    fxOverlay.launchReefCrumble(playerBoardPanel, ev.row(), ev.col(), true);
                    fxOverlay.launchArenaNodeBurst(playerBoardPanel, ev.row(), ev.col(), new Color(0xC084FC), ev.label());
                    showMessage(ev.label(), new Color(0xC084FC));
                }
                case BEACON_PICKED -> {
                    fxOverlay.launchArenaNodeBurst(playerBoardPanel, ev.row(), ev.col(), new Color(0x34D399), ev.label());
                    SoundEngine.playUiClick();
                    showMessage(ev.label(), new Color(0x34D399));
                }
                case PYLON_SIPHONED -> {
                    fxOverlay.launchArenaNodeBurst(playerBoardPanel, ev.row(), ev.col(), new Color(0x38BDF8), ev.label());
                    showMessage(ev.label(), new Color(0x38BDF8));
                }
                case CURRENT_BOOST ->
                    fxOverlay.launchArenaNodeBurst(playerBoardPanel, ev.row(), ev.col(), new Color(0x22D3EE), ev.label());
                case SONAR_PULSE -> {
                    fxOverlay.launchArenaNodeBurst(playerBoardPanel, ev.row(), ev.col(), new Color(0x5EEAD4), ev.label());
                    showMessage(ev.label() + " · Quét điểm yếu Boss!", new Color(0x5EEAD4));
                }
                case LEYNODE_BUFF ->
                    fxOverlay.launchArenaNodeBurst(playerBoardPanel, ev.row(), ev.col(), new Color(0xA78BFA), ev.label());
                case FISSURE_ERUPTED -> {
                    fxOverlay.launchArenaNodeBurst(playerBoardPanel, ev.row(), ev.col(), new Color(0xFB923C), ev.label());
                    playerBoardPanel.playEffect(SpriteAssets.FxType.FIRE, ev.row(), ev.col(), UiTokens.MAGIC_FIRE, 40L);
                }
                case DIRECTIVE_STARTED -> {
                    if (ev.directive() != null) {
                        showMessage("CHỈ THỊ CHIẾN THUẬT: " + ev.directive().title
                                + " (" + ev.directive().objective + ")", ev.directive().accentColor);
                    }
                }
                case DIRECTIVE_COMPLETED -> {
                    Color col = ev.directive() != null ? ev.directive().accentColor : new Color(0xFACC15);
                    if (ev.directive() == TitanMode.DirectiveType.WEAKPOINT_STRIKE) {
                        weaponCooldownEndMs.replaceAll((w, end) -> Math.max(now, end - 1200L));
                        refreshWeaponCooldownChips(now);
                    }
                    fxOverlay.triggerDirectiveCompletedFlash(ev.label(), col);
                    SoundEngine.playUiClick();
                    showMessage(ev.label()
                            + (ev.directive() != null ? (" · " + ev.directive().rewardLabel) : ""), col);
                    hudDirty = true;
                }
            }
        }
        playerBoardPanel.setArenaProps(titan.getArenaProps());
        if (boardChanged) {
            playerBoardPanel.refresh();
            refreshShots();
        }
        if (hudDirty) {
            refreshLoadout();
        }
    }

    private void syncRaidBoardTelemetry() {
        if (titan == null) return;
        boolean enragedNow = titan.isEnraged();
        if (enragedNow != OceanMapBackdrop.isTitanPhase2()) {
            OceanMapBackdrop.setTitanPhase2(enragedNow);
            playerBoardPanel.refresh();
            computerBoardPanel.refresh();
            repaint();
            if (enragedNow && titanBattleMode) {
                fxOverlay.launchPhase2ArenaMorph(titan.getStageTheme(), titan.getBoss().accentColor);
            }
        }
        computerBoardPanel.updateTitanState(titan.getRemainingHp(), titan.getMaxHp(), enragedNow);
        computerBoardPanel.setTitanBarrierState(titan.getEnrageBarrierHp(), titan.getEnrageBarrierMaxHp());
        computerBoardPanel.setTitanBossOffset(titan.getBossOffR(), titan.getBossOffC());
        TitanMode.TitanSkill pend = titan.getPendingSkill();
        TitanMode.TitanPart chargePart = pend != null ? TitanMode.chargingPartForSkill(pend) : null;
        computerBoardPanel.setTitanRaidState(
                titan.getCastProgress(),
                pend != null ? pend.skillName : "",
                chargePart,
                titan.getStaggerGauge(),
                titan.getStaggerThreshold(),
                titan.isBossStaggered(),
                titan.isBossSubmerged(),
                titan.isBossChilled(),
                titan.isBossExposed(),
                titan.getBurnStacks(),
                titan.isPartBroken(TitanMode.TitanPart.HEAD),
                titan.isPartBroken(TitanMode.TitanPart.CORE_FIN),
                titan.isPartBroken(TitanMode.TitanPart.BODY_TAIL));
        computerBoardPanel.setTitanPartDetails(
                titan.getPartHp(TitanMode.TitanPart.HEAD), titan.getPartMaxHp(TitanMode.TitanPart.HEAD),
                titan.getPartHp(TitanMode.TitanPart.CORE_FIN), titan.getPartMaxHp(TitanMode.TitanPart.CORE_FIN),
                titan.getPartHp(TitanMode.TitanPart.BODY_TAIL), titan.getPartMaxHp(TitanMode.TitanPart.BODY_TAIL),
                titan.isDpsCheckActive());
        computerBoardPanel.setTitanAntiMissileState(
                titan.isAntiMissileReady(),
                titan.getAntiMissileCooldownMs(),
                TitanMode.antiMissileSpec(titan.getBoss(), enragedNow).skillName());
        computerBoardPanel.setTitle(enragedNow
                ? ("THỨC TỈNH · " + titan.getBoss().shortLabel)
                : ("Đấu Trường · " + titan.getBoss().shortLabel));
        if (titanBattleMode) {
            playerBoardPanel.setTelegraphCells(
                    titan.getTelegraphCells(),
                    titan.getCastProgress(),
                    pend != null ? pend.accentColor : UiTokens.STATUS_DANGER);
            playerBoardPanel.setHazardTiles(titan.getActiveHazards());
            playerBoardPanel.setArenaProps(titan.getArenaProps());
        }
        fxOverlay.setArenaHudTelemetry(
                titanBattleMode && !sandboxMode,
                titan.getActiveDirective(),
                titan.getActiveRelics());
        if (coopMode && coopSession != null) {
            CoopSession.CoopConfig cfg = coopSession.getConfig();
            fxOverlay.setCoopHudTelemetry(
                    true,
                    cfg.p1Name(),
                    cfg.p2Name(),
                    coopSynergyPct,
                    coopSynergyPct >= 100,
                    coopActiveTurnPlayer);
            if (coopSynergyButton != null) {
                boolean ready = coopSynergyPct >= 100;
                Color synCol = ready ? new Color(0xFDE047) : new Color(0x34D399);
                coopSynergyButton.setText(ready ? "★ HỢP KÍCH [F]" : ("Hợp Kích: " + coopSynergyPct + "%"));
                coopSynergyButton.setEnergyProgress(coopSynergyPct / 100.0, synCol, new Color(0x38BDF8));
                coopSynergyButton.setEnabled(ready && !gameOver);
            }
        } else {
            fxOverlay.setCoopHudTelemetry(false, "", "", 0, false, 1);
        }
    }

    private void refreshWeaponCooldownChips(long now) {
        if (!titanBattleMode) return;
        CombatLoadout.WeaponOption[] opts = CombatLoadout.WeaponOption.values();
        TitanBattleController.FleetBuffSummary buffs = titan != null
                ? titan.computeFleetBuffs(superShipActive)
                : new TitanBattleController.FleetBuffSummary(true, true, true, true, true);
        double cdMult = titan != null ? titan.effectiveWeaponCooldownMult(superShipActive) : buffs.cooldownSpeedMult();
        for (int i = 0; i < weaponChips.size() && i < opts.length; i++) {
            CombatLoadout.WeaponOption w = opts[i];
            long endMs = weaponCooldownEndMs.getOrDefault(w, 0L);
            if (now < endMs) {
                long totalCd = Math.max(1L, Math.round(TitanMode.weaponCooldownMs(w) * cdMult));
                weaponChips.get(i).setCooldownProgress((endMs - now) / (double) totalCd);
            } else {
                weaponChips.get(i).setCooldownProgress(0.0);
            }
        }
    }

    private void switchActiveTitanBoss(TitanMode.TitanBoss boss) {
        TitanMode.setActiveBoss(boss);
        List<TitanMode.TitanSkill> arsenal = TitanMode.skillsFor(boss);
        if (!arsenal.isEmpty()) selectedTitanSkill = arsenal.get(0);
        if (titan != null) {
            raidModalPanel.setVisible(false);
            prevDpsCheckActive = false;
            weaponCooldownEndMs.clear();
            selectedDodgeShip = null;
            playerBoardPanel.setSelectedDodgeShip(null);
            playerBoardPanel.clearShipPreview();
            raidPauseUntilMs = 0L;
            hyperCannonCooldownMs = 0L;
            OceanMapBackdrop.setTitanPhase2(false);
            if (gameOver) {
                titan.clearRelics();
                titan.reset(boss);
                if (coopMode && coopSession != null) {
                    coopSynergyPct = 0;
                    coopSynergyUsedCount = 0;
                    coopActiveTurnPlayer = 1;
                    titan.reseedForCoop(coopSession.getActiveSeed() != 0L ? coopSession.getActiveSeed() : 202605L,
                            coopSession.getConfig().coopHpPercent() / 100.0);
                    if (coopSession.getTransport() == CoopSession.CoopTransport.AI_ALLY && coopAiTimer != null && !coopAiTimer.isRunning()) {
                        coopAiTimer.start();
                    }
                }
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
                playerBoardPanel.setInteractive(titanBattleMode);
                computerBoardPanel.setInteractive(true);
            } else {
                titan.switchBossOnly(boss);
            }
            titan.configureArenaStage(OceanMapBackdrop.getMapTheme(), titanBattleMode && !sandboxMode);
            computerBoardPanel.clearAllVisualState();
            computerBoardPanel.setCellStateOverride(titan.getBossGrid());
            syncRaidBoardTelemetry();
            if (titanBattleMode && raidTimer != null && !raidTimer.isRunning()) {
                lastRaidTickMs = System.currentTimeMillis();
                raidTimer.start();
            }
        }
        syncEnvironmentControls();
        applyGameMode(true);
        SoundEngine.refreshThemeMusic();
        refreshLoadout();
        refreshShots();
        showMessage("XUẤT HIỆN BOSS TITAN: " + boss.displayName + " — Sẵn sàng nghênh chiến!", boss.accentColor);
        triggerTitanBossEntrance(boss);
    }

    private int[] pickBossDemoTargetCell(List<int[]> activeShields, boolean fullShield) {
        if (activeShields != null && !activeShields.isEmpty() && (shieldPreviewEnabled || fullShield)) {
            int idx = Math.floorMod(bossDemoTargetSeq++, activeShields.size());
            return activeShields.get(idx);
        }
        int hr = playerBoardPanel.getHoveredRow();
        int hc = playerBoardPanel.getHoveredCol();
        if (hr >= 0 && hr < Board.SIZE && hc >= 0 && hc < Board.SIZE) {
            bossDemoTargetSeq++;
            return new int[]{hr, hc};
        }
        int[][] rotatingZones = {
                {2, 3}, {6, 7}, {3, 6}, {7, 2}, {1, 5}, {5, 4}, {4, 7}, {7, 6}
        };
        return rotatingZones[Math.floorMod(bossDemoTargetSeq++, rotatingZones.length)];
    }

    private void launchTitanDemoPreview(
            TitanMode.TitanSkill skill,
            int r,
            int c,
            boolean horiz,
            boolean enraged,
            boolean applyEnvFlash
    ) {
        List<int[]> cells = TitanMode.skillTargetCells(skill, r, c, horiz);
        List<int[]> demoHits = new ArrayList<>();
        List<int[]> demoMiss = new ArrayList<>();
        List<int[]> demoBlocked = new ArrayList<>();
        for (int[] cell : cells) {
            if (playerBoardPanel.isCellShielded(cell[0], cell[1])) {
                demoBlocked.add(cell);
            } else {
                CellState st = playerBoardPanel.getCellState(cell[0], cell[1]);
                if (st == CellState.SHIP || st == CellState.HIT) demoHits.add(cell);
                else demoMiss.add(cell);
            }
        }
        playTitanSkillAudioForLaunch(skill);
        if (applyEnvFlash) {
            TitanMode.EnvironmentAction env = TitanMode.environmentForSkill(skill);
            applyTitanEnvironmentImpact(env, true);
        }
        fxOverlay.launchTitanSkill(computerBoardPanel, playerBoardPanel, skill, enraged,
                r, c, cells, !demoHits.isEmpty(), () -> {
            applyTitanSkillVisuals(skill, r, c, cells, demoHits, demoMiss, demoBlocked);
        });
    }

    private void triggerTitanSkillNow(TitanMode.TitanSkill skill) {
        if (skill == null || turnLocked || gameOver) return;
        if (titan == null) {
            int[] rc = pickBossDemoTargetCell(null, false);
            int r = rc[0];
            int c = rc[1];
            boolean horiz = (bossDemoTargetSeq % 2 == 0);
            launchTitanDemoPreview(skill, r, c, horiz, false, true);
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
        List<int[]> effectiveShields = (playerBoardPanel.getShieldEnergy() > 0
                && (playerBoardPanel.isFullMapShield() || !playerBoardPanel.getShieldedCells().isEmpty()))
                ? (playerBoardPanel.isFullMapShield() ? allBoardCells() : playerBoardPanel.getShieldedCells())
                : List.of();
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
            blockedCells.clear();
            for (int[] c : cells) {
                if (playerBoardPanel.getShieldEnergy() > 0 && playerBoardPanel.isCellShielded(c[0], c[1])) {
                    blockedCells.add(c);
                    continue;
                }
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
        playTitanSkillAudioForLaunch(skill);
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
        applyTitanSkillVisuals(skill, targetRow, targetCol, allCells, hitCells, missCells, blockedCells,
                skill != null ? skill.shieldDamage : 24);
    }

    private void applyTitanSkillVisuals(
            TitanMode.TitanSkill skill,
            int targetRow,
            int targetCol,
            List<int[]> allCells,
            List<int[]> hitCells,
            List<int[]> missCells,
            List<int[]> blockedCells,
            int effectiveShieldDamage
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
            if (fx != SpriteAssets.FxType.HIT && fx != SpriteAssets.FxType.FIRE) {
                playerBoardPanel.playEffects(fx, hitCells, skill.accentColor, 36L);
            }
            playerBoardPanel.playEffects(SpriteAssets.FxType.HIT, hitCells, UiTokens.HIT_CRIMSON, 45L);
            playerBoardPanel.playEffects(SpriteAssets.FxType.FIRE, hitCells, new Color(0xFB923C), 55L);
            playerBoardPanel.recordBossCellScars(skill.boss, hitCells);
        }
        if (!missCells.isEmpty()) {
            playerBoardPanel.playEffects(fx, missCells, skill.accentColor, 40L);
            if (CombatFxOverlay.isSignatureTitanPovSkill(skill) || skill.leavesImpactZone) {
                playerBoardPanel.triggerBossTerrainAftermath(
                        skill, targetRow, targetCol, missCells, titan != null && titan.isEnraged());
            }
        }
        if (skill.leavesImpactZone && !(anyBlocked && !anyHit)) {
            playerBoardPanel.addImpactZone(
                    targetRow, targetCol, allCells,
                    TitanMode.impactZoneMagic(skill), skill.accentColor, true);
        }
        playerBoardPanel.triggerShake(anyHit ? (skill.phase2Only ? 9 : 7) : (skill.leavesImpactZone ? 6 : 4));

        if (anyBlocked && (playerBoardPanel.isFullMapShield() || !playerBoardPanel.getShieldedCells().isEmpty())) {
            BoardPanel.ShieldAbsorptionResult sRes = handleShieldImpact(
                    targetRow, targetCol, effectiveShieldDamage, skill.accentColor, skill.skillName);
            for (int[] bc : blockedCells) {
                if (bc[0] != targetRow || bc[1] != targetCol) {
                    playerBoardPanel.triggerShieldBlock(bc[0], bc[1], skill.shieldDamage >= 45 ? 3 : 2);
                }
            }
            if (!anyHit) return;
            if (!sRes.shattered()) {
                SoundEngine.playImpact(ShotResult.HIT, false);
                showMessage(skill.boss.shortLabel + " (" + skill.skillName + ") trúng " + hitCells.size()
                        + " ô · Khiên chắn " + blockedCells.size() + " ô (còn " + sRes.remainingEnergy() + "% NL)!", skill.accentColor);
                return;
            }
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
        stopAllTimers();
        playerBoardPanel.setInteractive(false);
        playerBoardPanel.setSelectedDodgeShip(null);
        playerBoardPanel.setTelegraphCells(List.of(), 0.0, UiTokens.STATUS_DANGER);
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
        if (coopMode && coopSession != null && coopSession.isNetworkMode() && coopSession.isHostSide()) {
            coopSession.sendPacket("EVT_END", won ? 1 : 0);
        }
        if (titanBattleMode && titan != null) {
            TitanBattleController.RaidDebriefSummary sum = titan.buildDebriefSummary(won);
            if (campaignMode && campaignStage != null) {
                int alive = sum.aliveShips();
                int stars = 0;
                if (won) {
                    boolean flawlessRank = "S".equals(sum.rank()) || (alive >= 3 && sum.staggers() + sum.partsBroken() >= 2);
                    stars = flawlessRank ? 3 : ((alive >= 2 || sum.staggers() >= 1) ? 2 : 1);
                    CampaignMode.recordVictory(campaignStage.stageNumber(), stars, sum.score());
                }
                syncCampaignHud();
                CampaignBattleController.CampaignDebriefSummary cSum = new CampaignBattleController.CampaignDebriefSummary(
                        campaignStage, won,
                        won ? null : "Hạm đội đã gục trước Boss " + b.shortLabel + "!",
                        stars, CampaignMode.getStars(campaignStage.stageNumber()),
                        sum.partsBroken(), 0, sum.totalDamage(),
                        alive * 20, sum.elapsedSeconds(), campaignRemainingSeconds(), sum.score());
                if (won && StoryMode.hasPostVictoryEpilogue(campaignStage.stageNumber())) {
                    storyOverlay.showEpisode(StoryMode.epilogueEpisodeForStage(campaignStage),
                            () -> raidModalPanel.showCampaignDebrief(cSum));
                } else {
                    raidModalPanel.showCampaignDebrief(cSum);
                }
                return;
            }
            TitanMode.TitanBoss nextRushBoss = (won && bossRushActive) ? TitanMode.nextBossInRush(b) : null;
            if (won && bossRushActive) {
                rushStagesCleared++;
                rushTotalScore += sum.score();
            }
            raidModalPanel.showDebrief(sum, nextRushBoss, bossRushActive, rushStagesCleared, rushTotalScore);
        }
    }

    private void openCampaignStoryCutscene(StoryMode.StoryEpisode episode) {
        if (episode == null) return;
        boolean wasLocked = turnLocked;
        turnLocked = true;
        storyOverlay.showEpisode(episode, () -> {
            if (gameOver) return;
            lastRaidTickMs = System.currentTimeMillis();
            if (titanIntroPendingInitial && titanBattleMode && titan != null && fxOverlay.isShowing()) {
                titanIntroPendingInitial = false;
                triggerTitanBossEntrance(titan.getBoss());
                return;
            }
            if (!fxOverlay.isBossCityRampageActive() && !superShipCutscenePlaying) {
                turnLocked = titanBattleMode ? false : (wasLocked && campaignMode ? false : wasLocked);
                updateSuperShipButtonUi();
            }
        });
    }

    public StoryComicOverlay getStoryOverlay() { return storyOverlay; }

    public void skipStoryCutsceneForTest() {
        if (storyOverlay.isStoryActive()) {
            storyOverlay.skipCutscene();
        }
    }

    private void toggleRaidGuideModal() {
        SoundEngine.playUiClick();
        if (raidModalPanel.isVisible() && raidModalPanel.isGuideMode()) {
            raidModalPanel.setVisible(false);
            lastRaidTickMs = System.currentTimeMillis();
        } else {
            raidModalPanel.showGuide();
        }
    }

    public boolean isRaidModalVisible() { return raidModalPanel.isVisible(); }

    public void showRaidGuideForTest() {
        raidModalPanel.showGuide();
    }

    public TitanBattleController getTitanController() { return titan; }

    public CampaignBattleController getCampaignController() { return campaignCtrl; }

    public boolean isCampaignMode() { return campaignMode; }

    public CampaignMode.CampaignStage getCampaignStage() { return campaignStage; }

    public void triggerCampaignDebriefForTest(boolean won) {
        if (!campaignMode) return;
        if (titanBattleMode) {
            finishTitanBattle(won);
        } else if (campaignCtrl != null) {
            if (won && !campaignCtrl.allEnemySunk()) {
                for (Ship s : new ArrayList<>(campaignCtrl.getEnemyShips())) {
                    for (int i = 0; i < s.type.length; i++) {
                        int r = s.row + (s.horizontal ? 0 : i);
                        int c = s.col + (s.horizontal ? i : 0);
                        campaignCtrl.firePlayerStrike(r, c, List.of(new int[]{r, c}), CombatLoadout.MagicOption.NONE);
                    }
                }
            }
            finishCampaignBattle(won, won ? null : "Thử nghiệm thất bại");
        }
    }

    private void restartCurrentTitan() {
        if (titan == null) return;
        SoundEngine.playUiClick();
        if (bossRushActive) {
            rushStagesCleared = 0;
            rushTotalScore = 0;
        }
        gameOver = true;
        switchActiveTitanBoss(titan.getBoss());
    }

    private void advanceBossRush(TitanMode.TitanBoss nextBoss, boolean repairHullPerk) {
        advanceBossRush(nextBoss, null, repairHullPerk);
    }

    private void advanceBossRush(TitanMode.TitanBoss nextBoss, TitanMode.RaidRelic chosenRelic, boolean repairHullPerk) {
        if (titan == null || nextBoss == null) return;
        SoundEngine.playUiClick();
        raidModalPanel.setVisible(false);
        if (chosenRelic != null) {
            titan.addRelic(chosenRelic);
        }
        String relicNote = chosenRelic != null ? (" · Di Vật [" + chosenRelic.title + "]") : "";
        String perkMsg;
        if (repairHullPerk) {
            int fixed = titan.repairFleetCells(2);
            perkMsg = "TIẾP TẾ: Vá " + fixed + " ô tàu, Khiên 100%" + relicNote + " — Tiến đánh " + nextBoss.shortLabel + "!";
        } else {
            fleetSpirit = Math.min(100, fleetSpirit + 30);
            mana = CombatLoadout.SUPER_MAX_MANA;
            perkMsg = "TIẾP TẾ: +30% Siêu Tàu, " + currentMaxMana() + " Mana & Khiên 100%" + relicNote + " — Tiến đánh " + nextBoss.shortLabel + "!";
        }
        gameOver = false;
        turnLocked = false;
        shieldPreviewEnabled = true;
        shieldShatteredState = false;
        shieldActivatedAtMs = 0L;
        playerBoardPanel.setInteractive(true);
        computerBoardPanel.setInteractive(true);
        switchActiveTitanBoss(nextBoss);
        playerBoardPanel.setCellStateOverride(titan.getPlayerGrid());
        playerBoardPanel.setFullMapShield(true, allBoardCells());
        playerBoardPanel.setArenaProps(titan.getArenaProps());
        playerBoardPanel.refresh();
        updateShieldButtonUi();
        updateSuperShipButtonUi();
        refreshLoadout();
        showMessage(perkMsg, chosenRelic != null ? chosenRelic.accentColor
                : (repairHullPerk ? UiTokens.STATUS_SUCCESS : new Color(0x38BDF8)));
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
            SoundEngine.playZombieSkill(zc);
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
            SoundEngine.playZombieImpact(zc, anyHit ? worst : ShotResult.MISS, false);
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
        stopPendingAiTimers();
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
            ShotResult result = game.useCombo(targetCells(row, col));
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
        } catch (IllegalStateException exception) {
            mana = previousMana;
            refreshLoadout();
            showMessage("Chưa tới lượt bắn; Hải Năng đã được hoàn lại.", UiTokens.STATUS_WARNING);
            turnLocked = false;
        }
    }

    private void afterPlayerShot() {
        refreshShots();
        if (checkGameOver()) return;
        if (pendingComputerAiTimer != null) {
            pendingComputerAiTimer.stop();
            pendingComputerAiTimer = null;
        }
        pendingComputerAiTimer = new Timer(480, event -> {
            pendingComputerAiTimer = null;
            takeComputerTurn();
        });
        pendingComputerAiTimer.setRepeats(false);
        pendingComputerAiTimer.start();
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
            shieldActivatedAtMs = System.currentTimeMillis();
            shieldDemoButton.setText("Khiên: 100%");
            shieldDemoButton.setEnergyProgress(1.0, UiTokens.SHIELD_TINT, UiTokens.SHIELD_TINT);
            List<int[]> shieldCells = new ArrayList<>();
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    CellState st = sandboxMode ? sandbox.getPlayerGrid()[r][c]
                            : (campaignCtrl != null ? campaignCtrl.getPlayerGrid()[r][c]
                            : (titanBattleMode && titan != null ? titan.getPlayerGrid()[r][c] : game.playerBoard.getCell(r, c)));
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
            int[] trc = pickBossDemoTargetCell(activeShields, fullShield);
            int tr = trc[0];
            int tc = trc[1];
            boolean tHoriz = (bossDemoTargetSeq % 2 == 0);
            Timer tCounter = new Timer(520, e -> {
                boolean enraged = titan != null && titan.isEnraged();
                launchTitanDemoPreview(skill, tr, tc, tHoriz, enraged, false);
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
                syncRaidBoardTelemetry();
            }
            computerBoardPanel.setKnownShips(List.of());
            computerBoardPanel.setShowShips(false);
        } else if (sandboxMode) {
            OceanMapBackdrop.setTitanPhase2(false);
            TitanMode.setTitanEnraged(false);
            OceanMapBackdrop.clearTitanEnvironmentSurge();
            fxOverlay.clearArenaHud();
            computerBoardPanel.setTitanBoss(null, 0, 0, false);
            computerBoardPanel.setCellStateOverride(sandbox.getEnemyGrid());
            computerBoardPanel.setZombieFleet(zombie);
            computerBoardPanel.setTitle(zombie ? "Hạm đội Zombie (Sandbox 5 Boss)" : "Vùng biển địch (Sandbox)");
            computerBoardPanel.setKnownShips(sandbox.getEnemyShips());
            computerBoardPanel.setShowShips(sandboxRevealEnemy);
        } else if (campaignMode && campaignCtrl != null) {
            OceanMapBackdrop.setTitanPhase2(false);
            TitanMode.setTitanEnraged(false);
            OceanMapBackdrop.clearTitanEnvironmentSurge();
            computerBoardPanel.setTitanBoss(null, 0, 0, false);
            computerBoardPanel.setCellStateOverride(campaignCtrl.getEnemyGrid());
            computerBoardPanel.setZombieFleet(zombie);
            computerBoardPanel.setTitle("Màn " + campaignStage.stageNumber() + " · " + campaignStage.ruleType().shortLabel);
            computerBoardPanel.setKnownShips(campaignCtrl.getEnemyShips());
            computerBoardPanel.setShowShips(false);
        } else if (zombie) {
            OceanMapBackdrop.setTitanPhase2(false);
            TitanMode.setTitanEnraged(false);
            OceanMapBackdrop.clearTitanEnvironmentSurge();
            fxOverlay.clearArenaHud();
            computerBoardPanel.setTitanBoss(null, 0, 0, false);
            computerBoardPanel.setZombieFleet(true);
            computerBoardPanel.setTitle("Hạm đội Zombie (5 Boss)");
            computerBoardPanel.setKnownShips(ZombieMode.generateZombieShowcaseFleet());
            computerBoardPanel.setShowShips(true);
        } else {
            OceanMapBackdrop.setTitanPhase2(false);
            TitanMode.setTitanEnraged(false);
            OceanMapBackdrop.clearTitanEnvironmentSurge();
            fxOverlay.clearArenaHud();
            computerBoardPanel.setTitanBoss(null, 0, 0, false);
            computerBoardPanel.setZombieFleet(false);
            computerBoardPanel.setTitle("Vùng biển địch");
            computerBoardPanel.setKnownShips(List.of());
            computerBoardPanel.setShowShips(false);
        }
        if (zombieRow != null) zombieRow.setVisible(zombie && !campaignMode);
        if (titanRow != null) titanRow.setVisible(titanMode && !campaignMode);
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
        SoundEngine.playZombieSkill(zc);
        fxOverlay.launchZombieStrike(computerBoardPanel, playerBoardPanel, zc, targetRow, targetCol, cells, !shielded, () -> {
            playerBoardPanel.setLastShot(targetRow, targetCol);
            if (shielded) {
                handleShieldImpact(targetRow, targetCol, zombieShieldDamage(zc), zc.accentColor, zc.skillName);
            } else {
                SoundEngine.playZombieImpact(zc, ShotResult.HIT, false);
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
            List<int[]> zCells = ZombieMode.skillTargetCells(zc, aiShot.row(), aiShot.col(), true);
            showMessage(zc.title + " dùng " + zc.skillName + " bắn về " + coordinate(aiShot.row(), aiShot.col()) + "...",
                    zc.accentColor);
            SoundEngine.playZombieSkill(zc);
            fxOverlay.launchZombieStrike(computerBoardPanel, playerBoardPanel, zc, aiShot.row(), aiShot.col(),
                    zCells, !shielded && aiHit, () -> {
                        playerBoardPanel.refresh();
                        playerBoardPanel.setLastShot(aiShot.row(), aiShot.col());
                        if (shielded) {
                            handleShieldImpact(aiShot.row(), aiShot.col(), zombieShieldDamage(zc), zc.accentColor, zc.skillName);
                            gainFleetSpirit(14);
                        } else {
                            SoundEngine.playZombieImpact(zc, aiShot.result(), false);
                            playerBoardPanel.playEffects(aiHit ? zc.impactFx : SpriteAssets.FxType.MISS, zCells,
                                    aiHit ? zc.accentColor : UiTokens.STEEL_EDGE, 42L);
                            if (zc == ZombieMode.ZombieClass.ABYSS_CARRIER) {
                                playerBoardPanel.addImpactZone(aiShot.row(), aiShot.col(),
                                        zCells,
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
            String msg = coopMode
                    ? "Quay về Phòng Co-op Bạn Bè? Trận Raid hiện tại sẽ dừng lại."
                    : (campaignMode
                            ? "Quay về Bản Đồ 1–100? Tiến trình hiện tại của màn sẽ mất."
                            : "Bắt đầu ván mới? Tiến trình hiện tại sẽ bị mất.");
            int answer = JOptionPane.showConfirmDialog(this, msg,
                    "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) return;
        }
        stopAllTimers();
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
        comboLabel.setText("<html><div style='width: 196px'>" + selectedWeapon.label + " + " + selectedMagic.label
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
        if (titanBattleMode && titan != null) {
            TitanBattleController.FleetBuffSummary fb = titan.computeFleetBuffs(superShipActive);
            List<String> shortTags = new ArrayList<>(5);
            List<String> tips = new ArrayList<>(5);
            if (fb.carrierAlive()) {
                shortTags.add("Hạm");
                tips.add("Hàng Không Mẫu Hạm: +30% sát thương Không Kích, -15% hồi chiêu");
            }
            if (fb.battleshipAlive()) {
                shortTags.add("Khiên");
                tips.add("Thiết Giáp Hạm: -25% hao năng lượng Khiên");
            }
            if (fb.cruiserAlive()) {
                shortTags.add("Mana");
                tips.add("Tuần Dương: +35% hồi Hải Năng liên tục");
            }
            if (fb.submarineAlive()) {
                shortTags.add("Ngầm");
                tips.add("Tàu Ngầm: +35% Choáng & Ngư Lôi xuyên Boss lặn");
            }
            if (fb.destroyerAlive()) {
                shortTags.add("Né");
                tips.add("Khu Trục: +25% Pháo Hạm & -35% hồi né đòn");
            }
            fleetBuffLabel.setText(shortTags.isEmpty() ? "Nội tại tàu: Mất hết" : "Nội tại: " + String.join("·", shortTags));
            fleetBuffLabel.setToolTipText(tips.isEmpty()
                    ? "Không còn tàu hộ tống sống sót"
                    : "<html>" + String.join("<br>", tips) + "</html>");
            fleetBuffLabel.setVisible(true);
        } else {
            fleetBuffLabel.setVisible(false);
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
        } else if (campaignMode && campaignCtrl != null) {
            int totalEnemy = Math.max(1, campaignCtrl.getEnemyShips().size());
            int sunkEnemy = 0;
            for (Ship s : campaignCtrl.getEnemyShips()) {
                boolean sunk = true;
                for (int i = 0; i < s.type.length; i++) {
                    int r = s.row + (s.horizontal ? 0 : i);
                    int c = s.col + (s.horizontal ? i : 0);
                    if (campaignCtrl.getEnemyGrid()[r][c] != CellState.HIT) {
                        sunk = false;
                        break;
                    }
                }
                if (sunk) sunkEnemy++;
            }
            String shotPart = campaignCtrl.getShotLimit() > 0
                    ? ("Đạn còn: " + campaignCtrl.getRemainingShots() + "/" + campaignCtrl.getShotLimit())
                    : ("Đã bắn: " + campaignCtrl.getShotsUsed());
            shotsLabel.setText("Màn " + campaignStage.stageNumber() + " · Chìm: " + sunkEnemy + "/" + totalEnemy + " · " + shotPart);
        } else if (game != null) {
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
        int scaled = titan != null
                ? Math.max(1, (int) Math.round(delta * titan.effectiveSuperGainMult()))
                : delta;
        fleetSpirit = Math.min(100, fleetSpirit + scaled);
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
            } else if (campaignMode && campaignCtrl != null) {
                campaignCtrl.applySuperShipArmorBoost();
                int[][] arm = new int[Board.SIZE][Board.SIZE];
                for (int r = 0; r < Board.SIZE; r++) {
                    for (int c = 0; c < Board.SIZE; c++) arm[r][c] = campaignCtrl.getPlayerSuperArmor(r, c);
                }
                playerBoardPanel.setSuperArmorGrid(arm);
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
            shieldActivatedAtMs = System.currentTimeMillis();
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
                hyperCannonCooldownMs = sandboxMode ? 0L : 10_000L;
                playerBoardPanel.addShieldEnergy(15);
                updateShieldButtonUi();
                if (sandboxMode) mana = currentMaxMana();
                handleNewlyBrokenParts(row, col);
                syncRaidBoardTelemetry();
                computerBoardPanel.addFloatingPopup(row, col,
                        "★ -" + titan.getLastStrikeInfo().damageDealt() + " HYPERION · CHOÁNG!", new Color(0xFDE047), true);
                computerBoardPanel.refresh();
                refreshLoadout();
                refreshShots();
                updateSuperShipButtonUi();
                if (out.bossDefeated()) {
                    finishTitanBattle(true);
                    return;
                }
                if (out.enragedJustTriggered()) {
                    raidPauseUntilMs = System.currentTimeMillis() + 1450L;
                    SoundEngine.playTitanEnrageTransform(titan.getBoss());
                    computerBoardPanel.triggerTitanEmergence(1350L);
                    computerBoardPanel.triggerTitanCharge(1550L);
                    fxOverlay.launchTitanEnrageCutIn(computerBoardPanel, titan.getBoss());
                    applyTitanEnvironmentImpact(TitanMode.environmentForEnrage(titan.getBoss()), false);
                }
                showMessage("★ HYPERION NOVA! -" + titan.getLastStrikeInfo().damageDealt()
                                + " HP Boss · +15% Khiên · Boss CHOÁNG 3.2s!",
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

        if (campaignMode && campaignCtrl != null) {
            turnLocked = true;
            CampaignBattleController.StrikeOutcome out =
                    campaignCtrl.firePlayerStrike(row, col, cells, CombatLoadout.MagicOption.LIGHTNING);
            SoundEngine.playSuperShipHyperCannon();
            showMessage("★ HYPERION NOVA MÀN " + campaignStage.stageNumber() + " TẠI " + coordinate(row, col) + "!",
                    new Color(0xFDE047));
            fxOverlay.launchHyperionCannon(playerBoardPanel, computerBoardPanel, row, col, cells, !out.hitCells().isEmpty(), () -> {
                if (gameOver) return;
                applyBoardImpactVisuals(
                        row, col, cells, CombatLoadout.WeaponOption.AIRSTRIKE, CombatLoadout.MagicOption.LIGHTNING,
                        out.aggregateResult(), out.hitCells(), out.missCells(),
                        out.burnCell(), out.sonarRevealedCells());
                hyperCannonCooldown = 2;
                playerBoardPanel.addShieldEnergy(15);
                updateShieldButtonUi();
                refreshLoadout();
                updateSuperShipButtonUi();
                syncCampaignHud();
                if (campaignCtrl.allEnemySunk()) {
                    finishCampaignBattle(true, null);
                    return;
                }
                if (campaignCtrl.isOutOfAmmo()) {
                    finishCampaignBattle(false, "Hết " + campaignStage.shotLimit() + " phát bắn giới hạn!");
                    return;
                }
                enemyFrozenTurns = Math.max(enemyFrozenTurns, 1);
                showMessage("★ HYPERION NOVA! Trúng " + out.hitCells().size() + " ô · Địch CHOÁNG 1 lượt!",
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
        private double cooldownProgress;

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
                int iconSpace = icon != null ? 18 : 0;
                int leftSpace = (showAccent ? 4 : 3) + iconSpace + 2;
                prefW = Math.max(54, textWidth + leftSpace);
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

        private void setCooldownProgress(double progress) {
            double clamped = Math.max(0.0, Math.min(1.0, progress));
            if (Math.abs(clamped - this.cooldownProgress) > 0.015) {
                this.cooldownProgress = clamped;
                repaint();
            }
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            Color background = selected ? UiTokens.BG_ELEVATED : (hovered ? UiTokens.BTN_SECONDARY_HOVER : UiTokens.BG_SURFACE);
            g.setColor(background);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), UiTokens.BORDER_RADIUS, UiTokens.BORDER_RADIUS);
            if (cooldownProgress > 0.01) {
                int cdW = (int) Math.round(getWidth() * cooldownProgress);
                g.setColor(new Color(4, 9, 18, 175));
                g.fillRoundRect(0, 0, cdW, getHeight(), UiTokens.BORDER_RADIUS, UiTokens.BORDER_RADIUS);
            }
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

    /** Non-blocking sci-fi glass overlay for In-Game Raid Quick Guide (?) and Post-Raid Debrief / Boss Rush Resupply. */
    private void handleNewlyBrokenParts(int anchorRow, int anchorCol) {
        if (titan == null) return;
        List<TitanMode.TitanPart> brokenList = titan.consumeNewlyBrokenParts();
        if (brokenList.isEmpty()) return;
        SoundEngine.playRaidPartBroken();
        int offset = 0;
        for (TitanMode.TitanPart part : brokenList) {
            computerBoardPanel.triggerPartBreakDetachment(part);
            String verb = switch (part) {
                case HEAD -> "★ RỤNG ĐẦU!";
                case CORE_FIN -> "★ GÃY VÂY / CÁNH!";
                case BODY_TAIL -> "★ ĐỨT THÂN / ĐUÔI!";
            };
            computerBoardPanel.addFloatingPopup(
                    Math.max(1, anchorRow - 1 + offset),
                    anchorCol,
                    verb,
                    new Color(0xFB923C),
                    true);
            offset++;
        }
    }

    private final class RaidModalPanel extends JPanel {
        private boolean guideMode;
        private final JLabel titleLabel = new JLabel();
        private final JLabel subtitleLabel = new JLabel();
        private final JLabel rankBadge = new JLabel("S");
        private final JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        private final JPanel bodyPanel = new JPanel();
        private final JPanel actionsRow = new JPanel();
        private Color cardAccent = UiTokens.ACCENT_SIGNAL;
        private final JPanel card;

        private RaidModalPanel() {
            super(new java.awt.GridBagLayout());
            setOpaque(false);
            card = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(8, 15, 29, 242));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.setColor(new Color(cardAccent.getRed(), cardAccent.getGreen(), cardAccent.getBlue(), 195));
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                    g2.dispose();
                }
            };
            card.setOpaque(false);
            card.setLayout(new BorderLayout(0, 12));
            card.setBorder(BorderFactory.createEmptyBorder(18, 22, 16, 22));
            card.setPreferredSize(new Dimension(600, 395));
            card.setMaximumSize(new Dimension(610, 420));

            JPanel header = new JPanel(new BorderLayout(12, 0));
            header.setOpaque(false);
            JPanel titles = new JPanel();
            titles.setOpaque(false);
            titles.setLayout(new javax.swing.BoxLayout(titles, javax.swing.BoxLayout.Y_AXIS));
            titleLabel.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 17));
            titleLabel.setForeground(UiTokens.TEXT_PRIMARY);
            subtitleLabel.setFont(UiTokens.FONT_META);
            subtitleLabel.setForeground(UiTokens.TEXT_MUTED);
            titles.add(titleLabel);
            titles.add(javax.swing.Box.createVerticalStrut(3));
            titles.add(subtitleLabel);
            header.add(titles, BorderLayout.CENTER);

            rankBadge.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 24));
            rankBadge.setForeground(new Color(0xFACC15));
            rankBadge.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            rankBadge.setPreferredSize(new Dimension(48, 42));
            rankBadge.setBorder(BorderFactory.createLineBorder(new Color(0xFACC15), 2));
            badgeWrap.setOpaque(false);
            badgeWrap.add(rankBadge);
            header.add(badgeWrap, BorderLayout.EAST);

            card.add(header, BorderLayout.NORTH);

            bodyPanel.setOpaque(false);
            card.add(bodyPanel, BorderLayout.CENTER);

            actionsRow.setOpaque(false);
            actionsRow.setLayout(new javax.swing.BoxLayout(actionsRow, javax.swing.BoxLayout.Y_AXIS));
            card.add(actionsRow, BorderLayout.SOUTH);

            add(card);
        }

        private void setActionRows(List<TacticalButton> primaryRow, List<TacticalButton> secondaryRow) {
            actionsRow.removeAll();
            if (primaryRow != null && !primaryRow.isEmpty()) {
                JPanel r1 = new JPanel(new java.awt.GridLayout(1, primaryRow.size(), 8, 0));
                r1.setOpaque(false);
                r1.setAlignmentX(LEFT_ALIGNMENT);
                r1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                for (TacticalButton b : primaryRow) {
                    b.setPreferredSize(new Dimension(120, 36));
                    r1.add(b);
                }
                actionsRow.add(r1);
            }
            if (secondaryRow != null && !secondaryRow.isEmpty()) {
                actionsRow.add(javax.swing.Box.createVerticalStrut(6));
                JPanel r2 = new JPanel(new java.awt.GridLayout(1, secondaryRow.size(), 8, 0));
                r2.setOpaque(false);
                r2.setAlignmentX(LEFT_ALIGNMENT);
                r2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
                for (TacticalButton b : secondaryRow) {
                    b.setPreferredSize(new Dimension(120, 34));
                    r2.add(b);
                }
                actionsRow.add(r2);
            }
        }

        private boolean isGuideMode() { return guideMode; }

        private void addGuideRule(String heading, Color accent, String detail) {
            JPanel row = new JPanel(new BorderLayout(0, 1));
            row.setOpaque(false);
            JLabel h = new JLabel(heading);
            h.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 12));
            h.setForeground(accent);
            javax.swing.JTextArea d = new javax.swing.JTextArea(detail);
            d.setOpaque(false);
            d.setEditable(false);
            d.setFocusable(false);
            d.setLineWrap(true);
            d.setWrapStyleWord(true);
            d.setBorder(null);
            d.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 12));
            d.setForeground(UiTokens.TEXT_PRIMARY);
            row.add(h, BorderLayout.NORTH);
            row.add(d, BorderLayout.CENTER);
            bodyPanel.add(row);
        }

        private JPanel makeStatCell(String label, String value, Color valColor) {
            JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            cell.setOpaque(false);
            JLabel k = new JLabel(label);
            k.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 12));
            k.setForeground(UiTokens.TEXT_MUTED);
            k.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
            JLabel v = new JLabel(value);
            v.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 12));
            v.setForeground(valColor);
            v.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
            cell.add(k);
            cell.add(v);
            return cell;
        }

        private void showGuide() {
            guideMode = true;
            cardAccent = new Color(0x38BDF8);
            card.setPreferredSize(new Dimension(600, 395));
            card.setMaximumSize(new Dimension(610, 420));
            titleLabel.setText("CẨM NANG RAID BOSS TITAN");
            subtitleLabel.setText("Tạm dừng đồng hồ chiến thuật — Nắm 5 quy tắc vàng để hạ gục Kaiju");
            rankBadge.setVisible(false);
            badgeWrap.setVisible(false);
            bodyPanel.removeAll();
            bodyPanel.setLayout(new java.awt.GridLayout(5, 1, 0, 4));
            addGuideRule("1. NÉ ĐÒN HẠM ĐỘI", new Color(0x38BDF8),
                    "Click tàu bên trái, bấm ô đích hoặc [W A S D] để né vùng đỏ; bấm [R / Chuột phải] xoay tàu.");
            addGuideRule("2. NGẮT CHIÊU (STAGGER)", new Color(0xFACC15),
                    "Khi Boss tụ chiêu, bắn bộ phận viền VÀNG (Đầu / Lõi / Thân) để Choáng 3.2s (+35% ST).");
            addGuideRule("3. SOI LẶN & RỤNG CHI", new Color(0x2DD4BF),
                    "Dùng Cổ Ngữ phá Lặn Ẩn (Lộ ×1.4 ST). Phá Đầu / Vây Lõi / Đuôi làm rụng chi & tước chiêu!");
            addGuideRule("4. PHẢN ỨNG NGUYÊN TỐ", new Color(0xF97316),
                    "Băng + Lửa kích Tan Chảy / Sốc Nhiệt; Sét kích Siêu Dẫn; Cổ Ngữ giải tĩnh điện.");
            addGuideRule("5. PARRY KHIÊN", new Color(0x4ADE80),
                    "Bật Khiên ngay trước khi Boss nổ (≤1.35s) để phản -18 HP & +25 Choáng!");
            TacticalButton resume = new TacticalButton("Tiếp tục chiến đấu", true);
            resume.addActionListener(e -> toggleRaidGuideModal());
            setActionRows(List.of(resume), List.of());
            revalidate();
            repaint();
            setVisible(true);
        }

        private void showDebrief(
                TitanBattleController.RaidDebriefSummary s,
                TitanMode.TitanBoss nextRushBoss,
                boolean rushMode,
                int stagesCleared,
                int cumulativeScore
        ) {
            guideMode = false;
            cardAccent = s.won() ? UiTokens.STATUS_SUCCESS : UiTokens.STATUS_DANGER;
            rankBadge.setVisible(true);
            badgeWrap.setVisible(true);
            rankBadge.setText(s.rank());
            Color rCol = "S".equals(s.rank()) ? new Color(0xFACC15)
                    : ("A".equals(s.rank()) ? new Color(0x38BDF8)
                    : ("B".equals(s.rank()) ? new Color(0x4ADE80) : new Color(0xF87171)));
            rankBadge.setForeground(rCol);
            rankBadge.setBorder(BorderFactory.createLineBorder(rCol, 2));

            if (nextRushBoss != null) {
                titleLabel.setText("VƯỢT ẢI " + stagesCleared + "/3 · HẠ " + s.boss().shortLabel.toUpperCase());
                subtitleLabel.setText("Boss Rush (" + s.difficulty().label + ") · Tiếp theo: " + nextRushBoss.displayName);
            } else if (s.won() && rushMode) {
                titleLabel.setText("CHIẾN DỊCH 3 TITAN TOÀN THẮNG!");
                subtitleLabel.setText("Hoàn tất toàn bộ 3 Boss (" + s.difficulty().label + ") · Tổng điểm: " + cumulativeScore);
            } else if (s.won()) {
                titleLabel.setText((coopMode ? "CO-OP CHIẾN THẮNG · " : "CHIẾN THẮNG · ") + s.boss().shortLabel.toUpperCase());
                subtitleLabel.setText((coopMode && coopSession != null
                        ? (coopSession.getConfig().p1Name() + " & " + coopSession.getConfig().p2Name()
                                + " · Hợp Kích: " + coopSynergyUsedCount + " lần · ")
                        : "Độ khó: " + s.difficulty().label + " · ") + "Điểm Raid: " + s.score());
            } else {
                titleLabel.setText((coopMode ? "CO-OP THẤT BẠI · " : "THẤT BẠI · ") + s.boss().shortLabel.toUpperCase());
                subtitleLabel.setText("Độ khó: " + s.difficulty().label + " · Điểm: " + s.score());
            }

            bodyPanel.removeAll();
            bodyPanel.setLayout(new BorderLayout(0, 12));
            JPanel statsGrid = new JPanel(new java.awt.GridLayout(3, 2, 16, 8));
            statsGrid.setOpaque(false);
            statsGrid.add(makeStatCell("• Tổng sát thương:", String.valueOf(s.totalDamage()), new Color(0xFDE047)));
            statsGrid.add(makeStatCell("• Bộ phận rụng vỡ:", s.partsBroken() + "/3", new Color(0xFB923C)));
            statsGrid.add(makeStatCell("• Ngắt chiêu (Stagger):", String.valueOf(s.staggers()), new Color(0x22D3EE)));
            statsGrid.add(makeStatCell("• Parry Hoàn hảo:", String.valueOf(s.parries()), new Color(0xFACC15)));
            statsGrid.add(makeStatCell("• Phản ứng Nguyên tố:", String.valueOf(s.reactions()), new Color(0x38BDF8)));
            statsGrid.add(makeStatCell("• Thời gian / Tàu còn:", s.elapsedSeconds() + "s · " + s.aliveShips() + "/5", UiTokens.TEXT_PRIMARY));
            bodyPanel.add(statsGrid, BorderLayout.NORTH);
            if (nextRushBoss != null) {
                card.setPreferredSize(new Dimension(640, 468));
                card.setMaximumSize(new Dimension(655, 488));
                JPanel rushSection = new JPanel(new BorderLayout(0, 8));
                rushSection.setOpaque(false);

                JPanel rushHint = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
                rushHint.setOpaque(false);
                JLabel p1 = new JLabel("CHỌN 1 DI VẬT + GÓI TIẾP TẾ");
                p1.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 12));
                p1.setForeground(new Color(0x4ADE80));
                p1.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
                JLabel p2 = new JLabel("trước khi giao chiến với");
                p2.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 12));
                p2.setForeground(new Color(0xA7F3D0));
                p2.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
                JLabel p3 = new JLabel(nextRushBoss.shortLabel + ":");
                p3.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 12));
                p3.setForeground(new Color(0xFDE047));
                p3.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
                rushHint.add(p1);
                rushHint.add(p2);
                rushHint.add(p3);
                rushSection.add(rushHint, BorderLayout.NORTH);

                List<TitanMode.RaidRelic> choices = titan != null ? titan.pickRelicChoices(3) : List.of();
                TitanMode.RaidRelic[] selectedRelic = { choices.isEmpty() ? null : choices.get(0) };
                if (!choices.isEmpty()) {
                    JPanel cardsGrid = new JPanel(new java.awt.GridLayout(1, Math.max(1, choices.size()), 10, 0));
                    cardsGrid.setOpaque(false);
                    Runnable[] refreshCardsRef = new Runnable[1];
                    refreshCardsRef[0] = () -> cardsGrid.repaint();
                    for (TitanMode.RaidRelic r : choices) {
                        cardsGrid.add(createRelicChoiceCard(r, selectedRelic, refreshCardsRef[0]));
                    }
                    rushSection.add(cardsGrid, BorderLayout.CENTER);
                }
                bodyPanel.add(rushSection, BorderLayout.CENTER);

                TacticalButton closeBtn = new TacticalButton("Xem Bàn", false);
                closeBtn.addActionListener(e -> raidModalPanel.setVisible(false));
                TacticalButton perkHeal = new TacticalButton("Tiếp Tế I: Vá 2 Ô & Khiên", true);
                perkHeal.addActionListener(e -> advanceBossRush(nextRushBoss, selectedRelic[0], true));
                TacticalButton perkPower = new TacticalButton("Tiếp Tế II: +30% Tàu & Full Mana", false);
                perkPower.addActionListener(e -> advanceBossRush(nextRushBoss, selectedRelic[0], false));
                setActionRows(List.of(perkHeal, perkPower), List.of(closeBtn));
            } else {
                card.setPreferredSize(new Dimension(600, 395));
                card.setMaximumSize(new Dimension(610, 420));
                TacticalButton closeBtn = new TacticalButton("Xem Bàn", false);
                closeBtn.addActionListener(e -> raidModalPanel.setVisible(false));
                List<TacticalButton> row1 = new ArrayList<>();
                if (!coopMode || coopSession == null || !coopSession.isNetworkMode()) {
                    TacticalButton retry = new TacticalButton("Đấu Lại", true);
                    retry.addActionListener(e -> restartCurrentTitan());
                    row1.add(retry);
                }
                if (s.won() && !coopMode) {
                    TitanMode.TitanBoss next = TitanMode.TitanBoss.values()[(s.boss().ordinal() + 1) % TitanMode.TitanBoss.values().length];
                    TacticalButton nextBtn = new TacticalButton("Thử " + next.shortLabel, false);
                    nextBtn.addActionListener(e -> switchActiveTitanBoss(next));
                    row1.add(nextBtn);
                }
                TacticalButton setupBtn = new TacticalButton(coopMode ? "Về Phòng Co-op" : "Về Bố Trí", false);
                setupBtn.addActionListener(e -> requestNewGame());
                row1.add(setupBtn);
                row1.add(closeBtn);
                setActionRows(row1, List.of());
            }
            revalidate();
            repaint();
            setVisible(true);
        }

        private void showCampaignDebrief(CampaignBattleController.CampaignDebriefSummary s) {
            guideMode = false;
            CampaignMode.CampaignStage st = s.stage();
            cardAccent = s.won() ? UiTokens.STATUS_SUCCESS : UiTokens.STATUS_DANGER;
            rankBadge.setVisible(true);
            badgeWrap.setVisible(true);
            rankBadge.setText(s.rank());
            Color rCol = "S".equals(s.rank()) ? new Color(0xFACC15)
                    : ("A".equals(s.rank()) ? new Color(0x38BDF8)
                    : ("B".equals(s.rank()) ? new Color(0x4ADE80) : new Color(0xF87171)));
            rankBadge.setForeground(rCol);
            rankBadge.setBorder(BorderFactory.createLineBorder(rCol, 2));

            titleLabel.setText(s.won()
                    ? ("HOÀN THÀNH MÀN " + st.stageNumber() + "/100")
                    : ("THẤT BẠI MÀN " + st.stageNumber() + "/100"));
            subtitleLabel.setText(s.won()
                    ? (st.title() + " · Điểm: " + s.score())
                    : (s.failReason() != null ? s.failReason() : st.title()));

            bodyPanel.removeAll();
            bodyPanel.setLayout(new BorderLayout(0, 10));
            card.setPreferredSize(new Dimension(620, 436));
            card.setMaximumSize(new Dimension(636, 456));

            // 3D Blender Star Strip
            JPanel starStrip = new JPanel() {
                @Override public Dimension getPreferredSize() { return new Dimension(0, 56); }
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    java.awt.image.BufferedImage gold = SpriteAssets.getMapProp("stage_star_gold");
                    java.awt.image.BufferedImage empty = SpriteAssets.getMapProp("stage_star_empty");
                    int starSize = 44;
                    int gap = 16;
                    int totalW = starSize * 3 + gap * 2;
                    int startX = (getWidth() - totalW) / 2;
                    for (int i = 0; i < 3; i++) {
                        java.awt.image.BufferedImage img = (i < s.starsEarned()) ? gold : empty;
                        int sx = startX + i * (starSize + gap);
                        int sy = (getHeight() - starSize) / 2;
                        if (img != null) {
                            g2.drawImage(img, sx, sy, starSize, starSize, null);
                        }
                    }
                    g2.dispose();
                }
            };
            starStrip.setOpaque(false);
            bodyPanel.add(starStrip, BorderLayout.NORTH);

            JPanel statsGrid = new JPanel(new java.awt.GridLayout(3, 2, 16, 8));
            statsGrid.setOpaque(false);
            statsGrid.add(makeStatCell("• Thể loại màn:", st.ruleType().shortLabel, st.ruleType().accent));
            statsGrid.add(makeStatCell("• Điểm kỷ lục:", String.valueOf(s.score()), new Color(0xFACC15)));
            statsGrid.add(makeStatCell("• Số phát bắn:", s.shotLimit() > 0 ? (s.shotsUsed() + "/" + s.shotLimit()) : String.valueOf(s.shotsUsed()), UiTokens.TEXT_PRIMARY));
            statsGrid.add(makeStatCell("• Máu hạm đội còn:", s.hullPct() + "%", new Color(0x4ADE80)));
            statsGrid.add(makeStatCell("• Thời gian chiến đấu:", s.elapsedSec() + "s" + (s.remainingSec() > 0 ? (" (dư " + s.remainingSec() + "s)") : ""), new Color(0x38BDF8)));
            statsGrid.add(makeStatCell("• Kỷ lục sao màn:", s.bestStars() + "/3★", new Color(0xFBBF24)));
            bodyPanel.add(statsGrid, BorderLayout.CENTER);

            List<TacticalButton> row1 = new ArrayList<>();
            if (s.won() && st.stageNumber() < CampaignMode.TOTAL_STAGES && onNextCampaignStage != null) {
                CampaignMode.CampaignStage nextStage = CampaignMode.stage(st.stageNumber() + 1);
                TacticalButton nextBtn = new TacticalButton("Màn Tiếp (" + nextStage.stageNumber() + ")", true);
                nextBtn.addActionListener(e -> {
                    SoundEngine.playUiClick();
                    onNextCampaignStage.accept(nextStage);
                });
                row1.add(nextBtn);
            }
            if (onNextCampaignStage != null) {
                TacticalButton retryBtn = new TacticalButton("Chơi Lại Màn " + st.stageNumber(), !s.won());
                retryBtn.addActionListener(e -> {
                    SoundEngine.playUiClick();
                    onNextCampaignStage.accept(st);
                });
                row1.add(retryBtn);
            }
            if (onBackToCampaignMap != null) {
                TacticalButton mapBtn = new TacticalButton("Bản Đồ 1–100", false);
                mapBtn.addActionListener(e -> {
                    SoundEngine.playUiClick();
                    stopAllTimers();
                    onBackToCampaignMap.run();
                });
                row1.add(mapBtn);
            }
            TacticalButton storyReplayBtn = new TacticalButton(
                    (s.won() && StoryMode.hasPostVictoryEpilogue(st.stageNumber())) ? "Xem Hồi Kết" : "Xem Truyện",
                    false);
            storyReplayBtn.addActionListener(e -> {
                SoundEngine.playUiClick();
                StoryMode.StoryEpisode ep = (s.won() && StoryMode.hasPostVictoryEpilogue(st.stageNumber()))
                        ? StoryMode.epilogueEpisodeForStage(st)
                        : StoryMode.introEpisodeForStage(st);
                raidModalPanel.setVisible(false);
                storyOverlay.showEpisode(ep, () -> raidModalPanel.setVisible(true));
            });
            TacticalButton closeBtn = new TacticalButton("Xem Bàn", false);
            closeBtn.addActionListener(e -> raidModalPanel.setVisible(false));
            row1.add(closeBtn);
            setActionRows(row1, List.of(storyReplayBtn));

            revalidate();
            repaint();
            setVisible(true);
        }

        private JPanel createRelicChoiceCard(
                TitanMode.RaidRelic relic,
                TitanMode.RaidRelic[] selectedHolder,
                Runnable onSelect
        ) {
            Color accent = relic.accentColor;
            JPanel cardPanel = new JPanel(new BorderLayout(0, 4)) {
                @Override protected void paintComponent(Graphics g) {
                    boolean active = selectedHolder[0] == relic;
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(active
                            ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 42)
                            : new Color(15, 25, 44, 215));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(active
                            ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 235)
                            : new Color(71, 85, 105, 150));
                    g2.setStroke(new BasicStroke(active ? 2.0f : 1.0f));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                    g2.dispose();
                }
            };
            cardPanel.setOpaque(false);
            cardPanel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            cardPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

            JPanel iconCanvas = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int sz = Math.min(getWidth(), getHeight()) - 2;
                    int x = (getWidth() - sz) / 2;
                    int y = (getHeight() - sz) / 2;
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
                    g2.fillOval(x, y, sz, sz);
                    java.awt.image.BufferedImage sprite = SpriteAssets.getMapProp(relic.spriteKey);
                    if (sprite != null) {
                        g2.drawImage(sprite, x + 2, y + 2, sz - 4, sz - 4, null);
                    }
                    g2.dispose();
                }
            };
            iconCanvas.setOpaque(false);
            iconCanvas.setPreferredSize(new Dimension(44, 42));

            JLabel nameLbl = new JLabel(relic.title, javax.swing.SwingConstants.CENTER);
            nameLbl.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 12));
            nameLbl.setForeground(accent);

            JPanel topBox = new JPanel(new BorderLayout(0, 3));
            topBox.setOpaque(false);
            topBox.add(iconCanvas, BorderLayout.CENTER);
            topBox.add(nameLbl, BorderLayout.SOUTH);
            cardPanel.add(topBox, BorderLayout.NORTH);

            JTextArea desc = new JTextArea(relic.summary);
            desc.setOpaque(false);
            desc.setEditable(false);
            desc.setFocusable(false);
            desc.setLineWrap(true);
            desc.setWrapStyleWord(true);
            desc.setBorder(null);
            desc.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 11));
            desc.setForeground(UiTokens.TEXT_PRIMARY);
            desc.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            cardPanel.add(desc, BorderLayout.CENTER);

            java.awt.event.MouseAdapter clickHandler = new java.awt.event.MouseAdapter() {
                @Override public void mousePressed(java.awt.event.MouseEvent e) {
                    selectedHolder[0] = relic;
                    SoundEngine.playUiClick();
                    if (onSelect != null) onSelect.run();
                }
            };
            cardPanel.addMouseListener(clickHandler);
            iconCanvas.addMouseListener(clickHandler);
            nameLbl.addMouseListener(clickHandler);
            desc.addMouseListener(clickHandler);
            return cardPanel;
        }
    }
}
