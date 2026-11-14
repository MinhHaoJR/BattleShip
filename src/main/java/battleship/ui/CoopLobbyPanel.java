package battleship.ui;

import battleship.model.Ship;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
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
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Dedicated Co-op Raid Room & Squad Configuration screen supporting LAN Host/Client,
 * 2-Player Hotseat, and AI Wingman solo co-op.
 */
public final class CoopLobbyPanel extends JPanel implements CoopSession.CoopListener {

    private static final Color BG_OUTER = new Color(0x060D1A);
    private static final Color PANEL_DARK = new Color(11, 22, 42, 228);
    private static final Color PANEL_INNER = new Color(15, 30, 56, 232);
    private static final Color BORDER_SUBTLE = new Color(0x1E3A5F);
    private static final Color TEXT_PRIMARY = new Color(0xF8FAFC);
    private static final Color TEXT_MUTED = new Color(0x94A3B8);
    private static final Color ACCENT_CYAN = new Color(0x38BDF8);
    private static final Color ACCENT_EMERALD = new Color(0x34D399);
    private static final Color ACCENT_GOLD = new Color(0xFBBF24);
    private static final Color ACCENT_CORAL = new Color(0xFB7185);

    private final CoopSession session = new CoopSession();
    private final Runnable onBackHome;
    private final BiConsumer<CoopSession, List<Ship>> onStartRaid;

    private final Map<CoopSession.CoopTransport, JButton> modeButtons = new EnumMap<>(CoopSession.CoopTransport.class);
    private JLabel headerStatusPill;

    // Network & Identity controls
    private JTextField hostIpField;
    private JTextField portField;
    private JTextField p1NameField;
    private JTextField p2NameField;
    private JButton netActionButton;
    private JButton copyIpButton;
    private JLabel netLedLabel;

    // P1 / P2 Squad cards
    private JLabel p1SlotNameLabel;
    private JLabel p1SlotBadgeLabel;
    private JLabel p2SlotNameLabel;
    private JLabel p2SlotBadgeLabel;

    // Chat box
    private JTextArea chatArea;
    private JTextField chatInputField;
    private JButton chatSendButton;

    // Right column raid setup
    private CoopDioramaCanvas dioramaCanvas;
    private JComboBox<TitanMode.TitanBoss> bossCombo;
    private JComboBox<TitanMode.TitanDifficulty> diffCombo;
    private JComboBox<OceanMapBackdrop.MapTheme> themeCombo;
    private JComboBox<OceanMapBackdrop.Season> seasonCombo;
    private JComboBox<OceanMapBackdrop.WeatherMode> weatherCombo;
    private JLabel configSyncNoteLabel;

    // Bottom launch bar
    private JButton launchOrReadyButton;
    private JButton rerollFleetButton;
    private JLabel launchHintLabel;

    private List<Ship> stagedFleet;
    private long stagedFleetSeed = System.nanoTime();
    private boolean updatingUi;
    private Timer animTimer;
    private double animTime;

    public CoopLobbyPanel(Runnable onBackHome, BiConsumer<CoopSession, List<Ship>> onStartRaid) {
        this.onBackHome = onBackHome;
        this.onStartRaid = onStartRaid;
        this.stagedFleet = CoopSession.generateDefaultFleet(stagedFleetSeed);

        setLayout(new BorderLayout(0, 8));
        setOpaque(true);
        setBackground(BG_OUTER);
        setBorder(new EmptyBorder(10, 14, 12, 14));

        add(buildTopHeader(), BorderLayout.NORTH);
        add(buildCenterSplit(), BorderLayout.CENTER);

        session.addListener(this);
        session.configureOfflineMode(CoopSession.CoopTransport.AI_ALLY);
        syncUiFromSession();

        animTimer = new Timer(33, e -> {
            animTime += 0.033;
            if (dioramaCanvas != null && isShowing()) {
                dioramaCanvas.repaint();
            }
        });
    }

    public CoopSession getSession() { return session; }

    public void activateLobby(boolean preferHostLan) {
        session.clearNonMatchListeners(this);
        if (preferHostLan) {
            session.startHosting(CoopSession.DEFAULT_PORT, cleanField(p1NameField.getText(), "Đô Đốc P1"));
        } else {
            syncUiFromSession();
        }
        startAnimIfVisible();
    }

    public void shutdownSession() {
        if (animTimer != null) animTimer.stop();
        session.close();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        startAnimIfVisible();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (animTimer != null) animTimer.stop();
    }

    private void startAnimIfVisible() {
        if (animTimer != null && !animTimer.isRunning()) {
            animTimer.start();
        }
    }

    private JPanel buildTopHeader() {
        RoundedCard header = new RoundedCard(PANEL_DARK, BORDER_SUBTLE, 14);
        header.setLayout(new BorderLayout(12, 0));
        header.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JButton backBtn = createPillButton("‹ Sảnh Chính", new Color(0x1E293B), TEXT_PRIMARY, BORDER_SUBTLE);
        backBtn.setPreferredSize(new Dimension(112, 34));
        backBtn.addActionListener(e -> {
            SoundEngine.playUiClick();
            session.closeNetworkOnly();
            if (onBackHome != null) onBackHome.run();
        });
        left.add(backBtn);

        JLabel iconLbl = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                BufferedImage img = SpriteAssets.getMapProp("home_badge_coop");
                if (img != null) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                }
            }
        };
        iconLbl.setPreferredSize(new Dimension(36, 36));
        left.add(iconLbl);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("PHÒNG RAID CO-OP BẠN BÈ");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(TEXT_PRIMARY);
        JLabel sub = new JLabel("Hợp lực 2 Đô Đốc · Hủy diệt Boss Titan ×1.75 HP & Song Long Hợp Kích");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sub.setForeground(TEXT_MUTED);
        titleBox.add(title);
        titleBox.add(sub);
        left.add(titleBox);

        headerStatusPill = new JLabel("SẴN SÀNG", SwingConstants.CENTER);
        headerStatusPill.setFont(new Font("SansSerif", Font.BOLD, 11));
        headerStatusPill.setForeground(ACCENT_EMERALD);
        headerStatusPill.setOpaque(true);
        headerStatusPill.setBackground(new Color(16, 185, 129, 34));
        headerStatusPill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_EMERALD, 1, true),
                new EmptyBorder(5, 12, 5, 12)
        ));

        header.add(left, BorderLayout.WEST);
        header.add(headerStatusPill, BorderLayout.EAST);
        return header;
    }

    private JPanel buildCenterSplit() {
        JPanel split = new JPanel(new BorderLayout(12, 0));
        split.setOpaque(false);

        JPanel leftCol = buildLeftLobbyColumn();
        leftCol.setPreferredSize(new Dimension(426, 0));
        split.add(leftCol, BorderLayout.WEST);
        split.add(buildRightRaidColumn(), BorderLayout.CENTER);
        return split;
    }

    private JPanel buildLeftLobbyColumn() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        // 1. 4 Connection Mode Selector Cards (2x2)
        RoundedCard modeCard = new RoundedCard(PANEL_DARK, BORDER_SUBTLE, 14);
        modeCard.setLayout(new BorderLayout(0, 6));
        modeCard.setBorder(new EmptyBorder(9, 10, 9, 10));
        modeCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 122));

        JLabel mHead = new JLabel("1. CHỌN HÌNH THỨC GHÉP ĐỘI");
        mHead.setFont(new Font("SansSerif", Font.BOLD, 11));
        mHead.setForeground(ACCENT_CYAN);
        modeCard.add(mHead, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 7, 7));
        grid.setOpaque(false);
        for (CoopSession.CoopTransport t : CoopSession.CoopTransport.values()) {
            JButton btn = new JButton("<html><center><b>" + t.title + "</b></center></html>");
            btn.setFont(new Font("SansSerif", Font.BOLD, 12));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(0, 38));
            btn.addActionListener(e -> {
                SoundEngine.playUiClick();
                switchTransport(t);
            });
            modeButtons.put(t, btn);
            grid.add(btn);
        }
        modeCard.add(grid, BorderLayout.CENTER);
        col.add(modeCard);
        col.add(Box.createVerticalStrut(8));

        // 2. Network & Identity Card
        RoundedCard netCard = new RoundedCard(PANEL_DARK, BORDER_SUBTLE, 14);
        netCard.setLayout(new GridBagLayout());
        netCard.setBorder(new EmptyBorder(9, 10, 9, 10));
        netCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 152));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 3, 2, 3);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0; g.gridx = 0; g.gridwidth = 4; g.weightx = 1.0;
        JLabel netHead = new JLabel("2. PHÒNG MẠNG LAN & DANH TÍNH");
        netHead.setFont(new Font("SansSerif", Font.BOLD, 11));
        netHead.setForeground(ACCENT_CYAN);
        netCard.add(netHead, g);

        g.gridwidth = 1;
        g.gridy = 1; g.gridx = 0; g.weightx = 0.0;
        netCard.add(mutedLabel("IP Host:"), g);
        hostIpField = createDarkField(session.getBoundHostIp());
        g.gridx = 1; g.weightx = 0.65;
        netCard.add(hostIpField, g);
        g.gridx = 2; g.weightx = 0.0;
        netCard.add(mutedLabel("Cổng:"), g);
        portField = createDarkField(Integer.toString(CoopSession.DEFAULT_PORT));
        portField.setPreferredSize(new Dimension(62, 26));
        g.gridx = 3; g.weightx = 0.35;
        netCard.add(portField, g);

        g.gridy = 2; g.gridx = 0; g.weightx = 0.0;
        netCard.add(mutedLabel("Tên P1:"), g);
        p1NameField = createDarkField("Đô Đốc P1");
        p1NameField.addActionListener(e -> applyNameChanges());
        g.gridx = 1; g.weightx = 0.55;
        netCard.add(p1NameField, g);
        g.gridx = 2; g.weightx = 0.0;
        netCard.add(mutedLabel("Tên P2:"), g);
        p2NameField = createDarkField("Vega AI");
        p2NameField.addActionListener(e -> applyNameChanges());
        g.gridx = 3; g.weightx = 0.45;
        netCard.add(p2NameField, g);

        JPanel netBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        netBtns.setOpaque(false);
        netActionButton = createPillButton("Mở Phòng LAN", new Color(0x0F766E), TEXT_PRIMARY, ACCENT_EMERALD);
        netActionButton.setPreferredSize(new Dimension(136, 28));
        netActionButton.addActionListener(e -> handleNetworkAction());
        copyIpButton = createPillButton("Sao Chép IP", new Color(0x1E293B), ACCENT_CYAN, BORDER_SUBTLE);
        copyIpButton.setPreferredSize(new Dimension(108, 28));
        copyIpButton.addActionListener(e -> copyHostAddress());
        netLedLabel = new JLabel("● Offline");
        netLedLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        netLedLabel.setForeground(ACCENT_GOLD);
        netBtns.add(netActionButton);
        netBtns.add(copyIpButton);
        netBtns.add(netLedLabel);

        g.gridy = 3; g.gridx = 0; g.gridwidth = 4; g.weightx = 1.0;
        netCard.add(netBtns, g);

        col.add(netCard);
        col.add(Box.createVerticalStrut(8));

        // 3. Squad Slot Cards (P1 Cyan & P2 Emerald)
        JPanel slotsRow = new JPanel(new GridLayout(1, 2, 8, 0));
        slotsRow.setOpaque(false);
        slotsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));

        RoundedCard p1Card = new RoundedCard(PANEL_INNER, new Color(56, 189, 248, 150), 12);
        p1Card.setLayout(new BoxLayout(p1Card, BoxLayout.Y_AXIS));
        p1Card.setBorder(new EmptyBorder(8, 10, 8, 10));
        JLabel p1Role = new JLabel("P1 · CHỦ HẠM ĐỘI");
        p1Role.setFont(new Font("SansSerif", Font.BOLD, 10));
        p1Role.setForeground(ACCENT_CYAN);
        p1Role.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        p1SlotNameLabel = new JLabel("Đô Đốc P1");
        p1SlotNameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        p1SlotNameLabel.setForeground(TEXT_PRIMARY);
        p1SlotNameLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JLabel p1Ships = new JLabel("Mẫu Hạm · Thiết Giáp · Khu Trục");
        p1Ships.setFont(new Font("SansSerif", Font.PLAIN, 10));
        p1Ships.setForeground(TEXT_MUTED);
        p1Ships.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        p1SlotBadgeLabel = new JLabel("● SẴN SÀNG");
        p1SlotBadgeLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        p1SlotBadgeLabel.setForeground(ACCENT_EMERALD);
        p1SlotBadgeLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        p1Card.add(p1Role);
        p1Card.add(p1SlotNameLabel);
        p1Card.add(p1Ships);
        p1Card.add(Box.createVerticalStrut(3));
        p1Card.add(p1SlotBadgeLabel);

        RoundedCard p2Card = new RoundedCard(PANEL_INNER, new Color(52, 211, 153, 150), 12);
        p2Card.setLayout(new BoxLayout(p2Card, BoxLayout.Y_AXIS));
        p2Card.setBorder(new EmptyBorder(8, 10, 8, 10));
        JLabel p2Role = new JLabel("P2 · CHIẾN HỮU YỂM TRỢ");
        p2Role.setFont(new Font("SansSerif", Font.BOLD, 10));
        p2Role.setForeground(ACCENT_EMERALD);
        p2Role.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        p2SlotNameLabel = new JLabel("Vega AI");
        p2SlotNameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        p2SlotNameLabel.setForeground(TEXT_PRIMARY);
        p2SlotNameLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JLabel p2Ships = new JLabel("Tuần Dương · Tàu Ngầm · Ping");
        p2Ships.setFont(new Font("SansSerif", Font.PLAIN, 10));
        p2Ships.setForeground(TEXT_MUTED);
        p2Ships.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        p2SlotBadgeLabel = new JLabel("● AI SẴN SÀNG");
        p2SlotBadgeLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        p2SlotBadgeLabel.setForeground(ACCENT_GOLD);
        p2SlotBadgeLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        p2Card.add(p2Role);
        p2Card.add(p2SlotNameLabel);
        p2Card.add(p2Ships);
        p2Card.add(Box.createVerticalStrut(3));
        p2Card.add(p2SlotBadgeLabel);

        slotsRow.add(p1Card);
        slotsRow.add(p2Card);
        col.add(slotsRow);
        col.add(Box.createVerticalStrut(8));

        // 4. Tactical Comms / Chat Panel
        RoundedCard chatCard = new RoundedCard(PANEL_DARK, BORDER_SUBTLE, 14);
        chatCard.setLayout(new BorderLayout(0, 6));
        chatCard.setBorder(new EmptyBorder(9, 10, 9, 10));

        JLabel cHead = new JLabel("3. LIÊN LẠC CHIẾN THUẬT & NHẬT KÝ PHÒNG");
        cHead.setFont(new Font("SansSerif", Font.BOLD, 11));
        cHead.setForeground(ACCENT_CYAN);
        chatCard.add(cHead, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 11));
        chatArea.setBackground(new Color(0x071224));
        chatArea.setForeground(new Color(0xE2E8F0));
        chatArea.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane sp = new JScrollPane(chatArea);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE, 1));
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatCard.add(sp, BorderLayout.CENTER);

        JPanel chatBottom = new JPanel();
        chatBottom.setOpaque(false);
        chatBottom.setLayout(new BoxLayout(chatBottom, BoxLayout.Y_AXIS));

        JPanel quickRow = new JPanel(new GridLayout(1, 3, 5, 0));
        quickRow.setOpaque(false);
        for (String q : new String[]{"Tập trung Đầu Boss!", "Chuẩn bị Hợp Kích!", "Tôi lo Khiên!"}) {
            JButton qb = createPillButton(q, new Color(0x0F1F3A), ACCENT_GOLD, new Color(0x334155));
            qb.setFont(new Font("SansSerif", Font.PLAIN, 10));
            qb.setPreferredSize(new Dimension(0, 24));
            qb.addActionListener(e -> {
                SoundEngine.playUiClick();
                session.sendChat(q);
            });
            quickRow.add(qb);
        }
        chatBottom.add(quickRow);
        chatBottom.add(Box.createVerticalStrut(5));

        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setOpaque(false);
        chatInputField = createDarkField("");
        chatInputField.addActionListener(e -> sendChatFromField());
        chatSendButton = createPillButton("Gửi", new Color(0x0369A1), TEXT_PRIMARY, ACCENT_CYAN);
        chatSendButton.setPreferredSize(new Dimension(64, 26));
        chatSendButton.addActionListener(e -> sendChatFromField());
        inputRow.add(chatInputField, BorderLayout.CENTER);
        inputRow.add(chatSendButton, BorderLayout.EAST);
        chatBottom.add(inputRow);

        chatCard.add(chatBottom, BorderLayout.SOUTH);
        col.add(chatCard);
        return col;
    }

    private JPanel buildRightRaidColumn() {
        JPanel col = new JPanel(new BorderLayout(0, 8));
        col.setOpaque(false);

        // 1. Animated Dual-Player Boss Raid Diorama Preview
        RoundedCard previewCard = new RoundedCard(PANEL_DARK, BORDER_SUBTLE, 14);
        previewCard.setLayout(new BorderLayout(0, 0));
        previewCard.setBorder(new EmptyBorder(8, 8, 8, 8));
        dioramaCanvas = new CoopDioramaCanvas();
        dioramaCanvas.setPreferredSize(new Dimension(0, 224));
        previewCard.add(dioramaCanvas, BorderLayout.CENTER);
        col.add(previewCard, BorderLayout.NORTH);

        // 2. Boss & Battlefield Configurator + Song Long Hop Kich Info
        JPanel midStack = new JPanel();
        midStack.setOpaque(false);
        midStack.setLayout(new BoxLayout(midStack, BoxLayout.Y_AXIS));

        RoundedCard cfgCard = new RoundedCard(PANEL_DARK, BORDER_SUBTLE, 14);
        cfgCard.setLayout(new BorderLayout(0, 6));
        cfgCard.setBorder(new EmptyBorder(9, 12, 9, 12));

        JPanel cfgHeader = new JPanel(new BorderLayout());
        cfgHeader.setOpaque(false);
        JLabel cTitle = new JLabel("CẤU HÌNH CHIẾN TRƯỜNG RAID CO-OP");
        cTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        cTitle.setForeground(ACCENT_CYAN);
        configSyncNoteLabel = new JLabel("Host điều chỉnh Boss · HP Boss ×1.75");
        configSyncNoteLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        configSyncNoteLabel.setForeground(ACCENT_GOLD);
        cfgHeader.add(cTitle, BorderLayout.WEST);
        cfgHeader.add(configSyncNoteLabel, BorderLayout.EAST);
        cfgCard.add(cfgHeader, BorderLayout.NORTH);

        JPanel cfgGrid = new JPanel(new GridLayout(3, 2, 10, 6));
        cfgGrid.setOpaque(false);

        bossCombo = createStyledCombo(TitanMode.TitanBoss.values());
        diffCombo = createStyledCombo(TitanMode.TitanDifficulty.values());
        themeCombo = createStyledCombo(OceanMapBackdrop.MapTheme.values());
        seasonCombo = createStyledCombo(OceanMapBackdrop.Season.values());
        weatherCombo = createStyledCombo(OceanMapBackdrop.WeatherMode.values());

        bossCombo.addActionListener(e -> pushHostConfigIfOwner());
        diffCombo.addActionListener(e -> pushHostConfigIfOwner());
        themeCombo.addActionListener(e -> pushHostConfigIfOwner());
        seasonCombo.addActionListener(e -> pushHostConfigIfOwner());
        weatherCombo.addActionListener(e -> pushHostConfigIfOwner());

        cfgGrid.add(labeledCombo("Boss Titan:", bossCombo));
        cfgGrid.add(labeledCombo("Độ Khó:", diffCombo));
        cfgGrid.add(labeledCombo("Bản Đồ Arena:", themeCombo));
        cfgGrid.add(labeledCombo("Mùa Biển:", seasonCombo));
        cfgGrid.add(labeledCombo("Thời Tiết:", weatherCombo));

        JPanel hpScalePill = new RoundedCard(new Color(0x0B1E38), new Color(251, 191, 36, 120), 10);
        hpScalePill.setLayout(new BorderLayout(6, 0));
        hpScalePill.setBorder(new EmptyBorder(4, 9, 4, 9));
        JLabel hpLbl = new JLabel("Co-op 2P · Boss ×175% HP");
        hpLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        hpLbl.setForeground(ACCENT_GOLD);
        hpScalePill.add(hpLbl, BorderLayout.CENTER);
        cfgGrid.add(hpScalePill);

        cfgCard.add(cfgGrid, BorderLayout.CENTER);
        midStack.add(cfgCard);
        midStack.add(Box.createVerticalStrut(8));

        // 3. Song Long Hop Kich Synergy Explainer Card
        RoundedCard synCard = new RoundedCard(PANEL_INNER, new Color(52, 211, 153, 140), 14);
        synCard.setLayout(new BorderLayout(10, 0));
        synCard.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel coreIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                BufferedImage img = SpriteAssets.getMapProp("coop_synergy_core");
                if (img != null) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                }
            }
        };
        coreIcon.setPreferredSize(new Dimension(52, 52));
        synCard.add(coreIcon, BorderLayout.WEST);

        JPanel synText = new JPanel();
        synText.setOpaque(false);
        synText.setLayout(new BoxLayout(synText, BoxLayout.Y_AXIS));
        JLabel sTitle = new JLabel("KỸ NĂNG ĐỘC QUYỀN: SONG LONG HỢP KÍCH (PHÍM F)");
        sTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        sTitle.setForeground(ACCENT_EMERALD);
        HomeScreenPanel.WrappedTextLabel sDesc = new HomeScreenPanel.WrappedTextLabel(
                new Font("SansSerif", Font.PLAIN, 11),
                new Color(0xCBD5E1)
        );
        sDesc.setText(
                "Mỗi phát trúng Boss nạp +16% Hợp Kích (+12% thêm nếu bắn gần ô Đồng Đội vừa Ping Chuột Phải). "
                + "Khi đạt 100%, hai tàu hợp phóng Chùm Hủy Diệt 3×3 gây Choáng tức thì, nạp +2 Mana cho cả hai & +1 lớp Giáp Hạm Đội!"
        );
        synText.add(sTitle);
        synText.add(Box.createVerticalStrut(2));
        synText.add(sDesc);
        synCard.add(synText, BorderLayout.CENTER);
        midStack.add(synCard);

        col.add(midStack, BorderLayout.CENTER);

        // 4. Bottom Action Strip
        RoundedCard launchBar = new RoundedCard(PANEL_DARK, BORDER_SUBTLE, 14);
        launchBar.setLayout(new BorderLayout(10, 0));
        launchBar.setBorder(new EmptyBorder(9, 12, 9, 12));

        launchHintLabel = new JLabel("Vega AI sẵn sàng!");
        launchHintLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        launchHintLabel.setForeground(TEXT_MUTED);
        launchBar.add(launchHintLabel, BorderLayout.CENTER);

        JPanel ctaRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        ctaRight.setOpaque(false);
        rerollFleetButton = createPillButton("Đổi Đội Hình", new Color(0x1E293B), ACCENT_CYAN, BORDER_SUBTLE);
        rerollFleetButton.setPreferredSize(new Dimension(108, 36));
        rerollFleetButton.addActionListener(e -> {
            SoundEngine.playUiClick();
            stagedFleetSeed = System.nanoTime();
            stagedFleet = CoopSession.generateDefaultFleet(stagedFleetSeed);
            session.sendChat("Đã xáo trộn đội hình tàu chuẩn bị Raid!");
            if (dioramaCanvas != null) dioramaCanvas.repaint();
        });

        launchOrReadyButton = createPillButton("XUẤT KÍCH CO-OP", new Color(0x059669), TEXT_PRIMARY, ACCENT_EMERALD);
        launchOrReadyButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        launchOrReadyButton.setPreferredSize(new Dimension(184, 36));
        launchOrReadyButton.addActionListener(e -> handleLaunchOrReady());

        ctaRight.add(rerollFleetButton);
        ctaRight.add(launchOrReadyButton);
        launchBar.add(ctaRight, BorderLayout.EAST);

        col.add(launchBar, BorderLayout.SOUTH);
        return col;
    }

    public void selectTransport(CoopSession.CoopTransport t) {
        switchTransport(t);
    }

    private void switchTransport(CoopSession.CoopTransport next) {
        if (next == null) return;
        applyNameChanges();
        switch (next) {
            case LAN_HOST -> {
                int port = parsePortOrDefault(portField.getText());
                session.startHosting(port, cleanField(p1NameField.getText(), "Đô Đốc P1"));
            }
            case LAN_CLIENT -> {
                session.closeNetworkOnly();
                // Switch UI cleanly into Client join mode; user clicks "Vào Phòng" or connect helper
                int port = parsePortOrDefault(portField.getText());
                String host = cleanField(hostIpField.getText(), "127.0.0.1");
                session.connectToHost(host, port, cleanField(p2NameField.getText(), "Bạn Bè P2"));
            }
            case HOTSEAT_2P, AI_ALLY -> session.configureOfflineMode(next);
        }
        syncUiFromSession();
    }

    private void handleNetworkAction() {
        SoundEngine.playUiClick();
        applyNameChanges();
        CoopSession.CoopTransport t = session.getTransport();
        int port = parsePortOrDefault(portField.getText());
        if (t == CoopSession.CoopTransport.LAN_HOST) {
            session.startHosting(port, cleanField(p1NameField.getText(), "Đô Đốc P1"));
        } else if (t == CoopSession.CoopTransport.LAN_CLIENT) {
            session.connectToHost(hostIpField.getText(), port, cleanField(p2NameField.getText(), "Bạn Bè P2"));
        } else {
            session.startHosting(port, cleanField(p1NameField.getText(), "Đô Đốc P1"));
        }
        syncUiFromSession();
    }

    private void copyHostAddress() {
        SoundEngine.playUiClick();
        String addr = session.getJoinAddressString();
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(addr), null);
            session.sendChat("Đã chép địa chỉ phòng: " + addr);
        } catch (Exception ignored) {}
    }

    private void applyNameChanges() {
        if (updatingUi) return;
        CoopSession.CoopConfig cur = session.getConfig();
        String n1 = cleanField(p1NameField.getText(), cur.p1Name());
        String n2 = cleanField(p2NameField.getText(), cur.p2Name());
        session.updateHostConfig(new CoopSession.CoopConfig(
                cur.boss(), cur.difficulty(), cur.mapTheme(), cur.season(), cur.weatherMode(),
                n1, n2, cur.coopHpPercent()
        ));
    }

    private void pushHostConfigIfOwner() {
        if (updatingUi || !session.isHostSide()) return;
        CoopSession.CoopConfig cur = session.getConfig();
        session.updateHostConfig(new CoopSession.CoopConfig(
                (TitanMode.TitanBoss) bossCombo.getSelectedItem(),
                (TitanMode.TitanDifficulty) diffCombo.getSelectedItem(),
                (OceanMapBackdrop.MapTheme) themeCombo.getSelectedItem(),
                (OceanMapBackdrop.Season) seasonCombo.getSelectedItem(),
                (OceanMapBackdrop.WeatherMode) weatherCombo.getSelectedItem(),
                cleanField(p1NameField.getText(), cur.p1Name()),
                cleanField(p2NameField.getText(), cur.p2Name()),
                CoopSession.DEFAULT_COOP_HP_PERCENT
        ));
    }

    private void sendChatFromField() {
        String raw = chatInputField.getText();
        if (raw == null || raw.isBlank()) return;
        SoundEngine.playUiClick();
        chatInputField.setText("");
        session.sendChat(raw);
    }

    private void handleLaunchOrReady() {
        SoundEngine.playUiClick();
        applyNameChanges();
        if (session.getTransport() == CoopSession.CoopTransport.LAN_CLIENT) {
            if (session.isPeerConnected()) {
                session.setLocalReady(!session.isLocalReady());
            } else {
                handleNetworkAction();
            }
            return;
        }
        if (!session.canLaunchMatch()) return;
        List<Ship> fleet = session.launchMatchAsHost(stagedFleet);
        if (onStartRaid != null) {
            onStartRaid.accept(session, fleet);
        }
    }

    @Override
    public void onStateChanged() {
        syncUiFromSession();
    }

    @Override
    public void onChatMessage(String sender, String text) {
        StringBuilder sb = new StringBuilder();
        for (String line : session.getChatHistory()) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
        }
        chatArea.setText(sb.toString());
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    @Override
    public void onMatchStarted(long seed, CoopSession.CoopConfig cfg, List<Ship> sharedFleet) {
        syncUiFromSession();
        if (onStartRaid != null) {
            onStartRaid.accept(session, sharedFleet);
        }
    }

    @Override
    public void onDisconnected(String reason) {
        syncUiFromSession();
    }

    private void syncUiFromSession() {
        updatingUi = true;
        try {
            CoopSession.CoopTransport t = session.getTransport();
            CoopSession.CoopConfig cfg = session.getConfig();

            for (Map.Entry<CoopSession.CoopTransport, JButton> e : modeButtons.entrySet()) {
                boolean active = e.getKey() == t;
                JButton b = e.getValue();
                b.setBackground(active ? new Color(0x0E3A5C) : new Color(0x0C182E));
                b.setForeground(active ? TEXT_PRIMARY : TEXT_MUTED);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(active ? ACCENT_EMERALD : BORDER_SUBTLE, active ? 2 : 1, true),
                        new EmptyBorder(4, 6, 4, 6)
                ));
            }

            if (t == CoopSession.CoopTransport.LAN_HOST) {
                hostIpField.setText(session.getBoundHostIp());
                hostIpField.setEditable(false);
                portField.setText(Integer.toString(session.getBoundPort()));
                portField.setEditable(true);
                netActionButton.setText("Mở Lại Cổng");
                copyIpButton.setEnabled(true);
            } else if (t == CoopSession.CoopTransport.LAN_CLIENT) {
                if (!hostIpField.isFocusOwner() && hostIpField.getText().isBlank()) {
                    hostIpField.setText(session.getBoundHostIp());
                }
                hostIpField.setEditable(true);
                portField.setEditable(true);
                netActionButton.setText("Vào Phòng LAN");
                copyIpButton.setEnabled(false);
            } else {
                hostIpField.setEditable(false);
                portField.setEditable(false);
                netActionButton.setText("Chuyển sang LAN");
                copyIpButton.setEnabled(false);
            }

            if (!p1NameField.isFocusOwner()) p1NameField.setText(cfg.p1Name());
            if (!p2NameField.isFocusOwner()) p2NameField.setText(cfg.p2Name());
            p1NameField.setEditable(session.isHostSide());
            p2NameField.setEditable(t != CoopSession.CoopTransport.LAN_HOST);

            p1SlotNameLabel.setText(cfg.p1Name());
            p2SlotNameLabel.setText(cfg.p2Name());
            p1SlotBadgeLabel.setText("● SẴN SÀNG");
            p1SlotBadgeLabel.setForeground(ACCENT_EMERALD);

            boolean p2Ok = session.isPeerConnected() && (t != CoopSession.CoopTransport.LAN_CLIENT ? session.isPeerReady() : session.isLocalReady());
            if (t == CoopSession.CoopTransport.AI_ALLY) {
                p2SlotBadgeLabel.setText("● VEGA AI SẴN SÀNG");
                p2SlotBadgeLabel.setForeground(ACCENT_EMERALD);
            } else if (t == CoopSession.CoopTransport.HOTSEAT_2P) {
                p2SlotBadgeLabel.setText("● CHUNG MÁY P2");
                p2SlotBadgeLabel.setForeground(ACCENT_CYAN);
            } else if (!session.isPeerConnected()) {
                p2SlotBadgeLabel.setText("○ ĐANG CHỜ...");
                p2SlotBadgeLabel.setForeground(ACCENT_GOLD);
            } else if (p2Ok) {
                p2SlotBadgeLabel.setText("● ĐÃ SẴN SÀNG!");
                p2SlotBadgeLabel.setForeground(ACCENT_EMERALD);
            } else {
                p2SlotBadgeLabel.setText("● ĐÃ VÀO PHÒNG");
                p2SlotBadgeLabel.setForeground(ACCENT_CYAN);
            }

            netLedLabel.setText(session.isPeerConnected() ? "● Đã Nối" : "○ Chờ Nối");
            netLedLabel.setForeground(session.isPeerConnected() ? ACCENT_EMERALD : ACCENT_GOLD);

            headerStatusPill.setText(t.title.toUpperCase() + " · " + (session.isPeerConnected() ? "TRỰC TUYẾN" : "CHỜ BẠN"));
            Color pillCol = session.isPeerConnected() ? ACCENT_EMERALD : ACCENT_GOLD;
            headerStatusPill.setForeground(pillCol);
            headerStatusPill.setBackground(new Color(pillCol.getRed(), pillCol.getGreen(), pillCol.getBlue(), 34));
            headerStatusPill.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(pillCol, 1, true),
                    new EmptyBorder(5, 12, 5, 12)
            ));

            bossCombo.setSelectedItem(cfg.boss());
            diffCombo.setSelectedItem(cfg.difficulty());
            themeCombo.setSelectedItem(cfg.mapTheme());
            seasonCombo.setSelectedItem(cfg.season());
            weatherCombo.setSelectedItem(cfg.weatherMode());

            boolean canEditCfg = session.isHostSide();
            bossCombo.setEnabled(canEditCfg);
            diffCombo.setEnabled(canEditCfg);
            themeCombo.setEnabled(canEditCfg);
            seasonCombo.setEnabled(canEditCfg);
            weatherCombo.setEnabled(canEditCfg);
            rerollFleetButton.setEnabled(canEditCfg);

            int scaledHp = (int) Math.round(TitanMode.scaledMaxHp(cfg.boss(), cfg.difficulty()) * (cfg.coopHpPercent() / 100.0));
            configSyncNoteLabel.setText((canEditCfg ? "Host chỉnh · " : "Đồng bộ từ Host · ")
                    + "Boss HP: " + scaledHp);

            launchHintLabel.setText(session.getStatusText());
            if (t == CoopSession.CoopTransport.LAN_CLIENT) {
                if (!session.isPeerConnected()) {
                    launchOrReadyButton.setText("KẾT NỐI HOST");
                    launchOrReadyButton.setEnabled(true);
                    launchOrReadyButton.setBackground(new Color(0x0369A1));
                } else if (session.isLocalReady()) {
                    launchOrReadyButton.setText("ĐÃ SẴN SÀNG (HỦY)");
                    launchOrReadyButton.setEnabled(true);
                    launchOrReadyButton.setBackground(new Color(0xB45309));
                } else {
                    launchOrReadyButton.setText("SẴN SÀNG CHIẾN!");
                    launchOrReadyButton.setEnabled(true);
                    launchOrReadyButton.setBackground(new Color(0x059669));
                }
            } else {
                boolean readyToLaunch = session.canLaunchMatch();
                launchOrReadyButton.setText("XUẤT KÍCH CO-OP RAID");
                launchOrReadyButton.setEnabled(readyToLaunch);
                launchOrReadyButton.setBackground(readyToLaunch ? new Color(0x059669) : new Color(0x1E293B));
            }
            if (dioramaCanvas != null) dioramaCanvas.repaint();
        } finally {
            updatingUi = false;
        }
    }

    private static JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(TEXT_MUTED);
        return l;
    }

    private static JTextField createDarkField(String initial) {
        JTextField tf = new JTextField(initial);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tf.setBackground(new Color(0x071224));
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(ACCENT_CYAN);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                new EmptyBorder(3, 7, 3, 7)
        ));
        return tf;
    }

    private static <T> JComboBox<T> createStyledCombo(T[] items) {
        JComboBox<T> cb = new JComboBox<>(new DefaultComboBoxModel<>(items));
        cb.setFont(new Font("SansSerif", Font.BOLD, 11));
        cb.setBackground(new Color(0x0B1930));
        cb.setForeground(TEXT_PRIMARY);
        cb.setFocusable(false);
        return cb;
    }

    private static JPanel labeledCombo(String label, JComboBox<?> combo) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(TEXT_MUTED);
        l.setPreferredSize(new Dimension(88, 24));
        p.add(l, BorderLayout.WEST);
        p.add(combo, BorderLayout.CENTER);
        return p;
    }

    private static JButton createPillButton(String text, Color bg, Color fg, Color border) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        return b;
    }

    private static String cleanField(String raw, String fallback) {
        if (raw == null || raw.trim().isEmpty()) return fallback;
        return raw.trim();
    }

    private static int parsePortOrDefault(String raw) {
        if (raw == null || raw.trim().isEmpty()) return CoopSession.DEFAULT_PORT;
        try {
            int p = Integer.parseInt(raw.trim());
            return (p > 0 && p <= 65535) ? p : CoopSession.DEFAULT_PORT;
        } catch (NumberFormatException ignored) {
            return CoopSession.DEFAULT_PORT;
        }
    }

    /** Translucent obsidian card with crisp border radius. */
    private static final class RoundedCard extends JPanel {
        private final Color fill;
        private final Color stroke;
        private final int radius;

        RoundedCard(Color fill, Color stroke, int radius) {
            this.fill = fill;
            this.stroke = stroke;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, radius, radius));
            g2.setColor(stroke);
            g2.setStroke(new BasicStroke(1.1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Live animated 2-Player vs Boss Titan Raid Diorama showcasing dual Allied ships firing Song Long Hop Kich beams. */
    private final class CoopDioramaCanvas extends JPanel {
        CoopDioramaCanvas() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            if (w <= 4 || h <= 4) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            CoopSession.CoopConfig cfg = session.getConfig();
            OceanMapBackdrop.MapTheme prevTheme = OceanMapBackdrop.getMapTheme();
            OceanMapBackdrop.Season prevSeason = OceanMapBackdrop.getSeason();
            OceanMapBackdrop.WeatherMode prevWeather = OceanMapBackdrop.getWeatherMode();
            g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
            try {
                OceanMapBackdrop.setMapTheme(cfg.mapTheme());
                OceanMapBackdrop.setSeason(cfg.season());
                OceanMapBackdrop.setWeatherMode(cfg.weatherMode());
                OceanMapBackdrop.paintOceanMap(g2, w, h, animTime, false);
            } finally {
                OceanMapBackdrop.setMapTheme(prevTheme);
                OceanMapBackdrop.setSeason(prevSeason);
                OceanMapBackdrop.setWeatherMode(prevWeather);
            }

            // Soft vignette overlay for crystal HUD legibility
            g2.setPaint(new GradientPaint(0, 0, new Color(4, 10, 22, 130), 0, h, new Color(4, 10, 22, 60)));
            g2.fillRect(0, 0, w, h);

            int p1X = Math.round(w * 0.16f);
            int p1Y = Math.round(h * 0.44f + (float) Math.sin(animTime * 2.2) * 3.5f);
            int p2X = Math.round(w * 0.22f);
            int p2Y = Math.round(h * 0.72f + (float) Math.cos(animTime * 2.5) * 3.5f);

            int mergeX = Math.round(w * 0.54f);
            int mergeY = Math.round(h * 0.54f);
            int bossX = Math.round(w * 0.80f);
            int bossY = Math.round(h * 0.53f + (float) Math.sin(animTime * 1.8 + 1.0) * 5.0f);

            // Converging "Song Long Hop Kich" dual-color beams
            float pulse = 0.65f + 0.35f * (float) Math.sin(animTime * 4.5);
            QuadCurve2D c1 = new QuadCurve2D.Double(p1X + 44, p1Y, w * 0.36, h * 0.34, mergeX, mergeY);
            QuadCurve2D c2 = new QuadCurve2D.Double(p2X + 38, p2Y, w * 0.40, h * 0.76, mergeX, mergeY);

            g2.setStroke(new BasicStroke(7.5f * pulse, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(56, 189, 248, 85));
            g2.draw(c1);
            g2.setColor(new Color(52, 211, 153, 85));
            g2.draw(c2);

            g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(ACCENT_CYAN);
            g2.draw(c1);
            g2.setColor(ACCENT_EMERALD);
            g2.draw(c2);

            // Unified Gold-Plasma Beam from Convergence Sphere -> Boss Titan
            g2.setStroke(new BasicStroke(11f * pulse, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(251, 191, 36, 95));
            g2.drawLine(mergeX, mergeY, bossX - 18, bossY);
            g2.setStroke(new BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(0xFEF08A));
            g2.drawLine(mergeX, mergeY, bossX - 18, bossY);

            // 3D Synergy Core at convergence point
            BufferedImage coreImg = SpriteAssets.getMapProp("coop_synergy_core");
            if (coreImg != null) {
                int cSz = Math.round(44 + 6 * pulse);
                g2.drawImage(coreImg, mergeX - cSz / 2, mergeY - cSz / 2, cSz, cSz, null);
            }

            // Draw P1 Flagship & P2 Escort
            BufferedImage p1Ship = SpriteAssets.getSuperShipSprite(true);
            if (p1Ship == null) p1Ship = SpriteAssets.getShipSprite(battleship.model.ShipType.BATTLESHIP, true);
            if (p1Ship != null) {
                int sw = 122, sh = Math.max(36, sw * p1Ship.getHeight() / Math.max(1, p1Ship.getWidth()));
                g2.drawImage(p1Ship, p1X - sw / 2, p1Y - sh / 2, sw, sh, null);
            }
            BufferedImage p2Ship = SpriteAssets.getShipSprite(battleship.model.ShipType.CRUISER, true);
            if (p2Ship != null) {
                int sw = 98, sh = Math.max(30, sw * p2Ship.getHeight() / Math.max(1, p2Ship.getWidth()));
                g2.drawImage(p2Ship, p2X - sw / 2, p2Y - sh / 2, sw, sh, null);
            }

            // Draw Boss Titan + Ping Beacon on Boss weakspot
            BufferedImage bossImg = SpriteAssets.getTitanSprite(cfg.boss(), false, true);
            if (bossImg != null) {
                int bw = 152, bh = Math.max(88, bw * bossImg.getHeight() / Math.max(1, bossImg.getWidth()));
                g2.drawImage(bossImg, bossX - bw / 2, bossY - bh / 2, bw, bh, null);
            }
            BufferedImage pingImg = SpriteAssets.getMapProp("coop_ally_ping");
            if (pingImg != null) {
                int pSz = 34;
                int bob = (int) Math.round(Math.sin(animTime * 5.0) * 4.0);
                g2.drawImage(pingImg, bossX - 28 - pSz / 2, bossY - 46 + bob, pSz, pSz, null);
            }

            // Player Nameplate Badges
            drawTag(g2, 12, 12, "P1: " + cfg.p1Name(), ACCENT_CYAN);
            drawTag(g2, 12, h - 28, "P2: " + cfg.p2Name(), ACCENT_EMERALD);
            String bossBadge = cfg.boss().displayName + " · " + cfg.difficulty().label;
            FontMetrics fm = g2.getFontMetrics(new Font("SansSerif", Font.BOLD, 11));
            int bw = fm.stringWidth(bossBadge) + 20;
            drawTag(g2, w - bw - 12, 12, bossBadge, ACCENT_GOLD);

            g2.dispose();
        }

        private void drawTag(Graphics2D g2, int x, int y, String text, Color accent) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            int tw = g2.getFontMetrics().stringWidth(text) + 16;
            g2.setColor(new Color(8, 16, 32, 200));
            g2.fillRoundRect(x, y, tw, 20, 10, 10);
            g2.setColor(accent);
            g2.setStroke(new BasicStroke(1.1f));
            g2.drawRoundRect(x, y, tw, 20, 10, 10);
            g2.drawString(text, x + 8, y + 14);
        }
    }
}
