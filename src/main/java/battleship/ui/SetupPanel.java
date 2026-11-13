package battleship.ui;

import battleship.model.Board;
import battleship.model.CellState;
import battleship.model.Ship;
import battleship.model.ShipType;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Fleet deployment screen. Logic TODOs are reported inline and never escape the EDT. */
public class SetupPanel extends JPanel {
    private Board playerBoard = new Board();
    private final BoardPanel boardPanel = new BoardPanel(playerBoard, "Hạm đội của bạn", true);
    private final EnumSet<ShipType> placedShips = EnumSet.noneOf(ShipType.class);
    private final List<Ship> placedShipList = new ArrayList<>();
    private final Map<ShipType, ShipRow> shipRows = new EnumMap<>(ShipType.class);
    private final JLabel statusLabel = new JLabel("Chọn tàu rồi chọn ô bắt đầu trên hải đồ.");
    private final JLabel countLabel = new JLabel();
    private final TacticalButton rotateButton = new TacticalButton("Xoay: Ngang", false);
    private final TacticalButton startButton = new TacticalButton("Bắt đầu chiến đấu", true);
    private final TacticalButton sandboxButton = new TacticalButton("Mở Sandbox (Test Tất Cả)", false);
    private final OceanMapBackdrop.AtmospherePill mapPill =
            new OceanMapBackdrop.AtmospherePill(OceanMapBackdrop.AtmospherePill.Kind.MAP);
    private final OceanMapBackdrop.AtmospherePill seasonPill =
            new OceanMapBackdrop.AtmospherePill(OceanMapBackdrop.AtmospherePill.Kind.SEASON);
    private final OceanMapBackdrop.AtmospherePill weatherPill =
            new OceanMapBackdrop.AtmospherePill(OceanMapBackdrop.AtmospherePill.Kind.WEATHER);
    private final ZombieMode.ModePill modePill = new ZombieMode.ModePill();
    private final TitanMode.TitanPill titanPill = new TitanMode.TitanPill();
    private JPanel titanRow;
    private final Consumer<Board> onStartGame;
    private Consumer<List<Ship>> onStartSandbox;
    private Consumer<List<Ship>> onStartTitan;
    private ShipType selectedType = ShipType.values()[0];
    private boolean horizontal = true;

    public SetupPanel(Consumer<Board> onStartGame) {
        this.onStartGame = onStartGame;
        setLayout(new GridBagLayout());
        setOpaque(true);
        setBackground(UiTokens.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(UiTokens.SPACE_MD, UiTokens.SPACE_LG, UiTokens.SPACE_MD, UiTokens.SPACE_LG));
        boardPanel.setInteractive(true);
        boardPanel.setAmbientStageDriven(true);
        boardPanel.setOnCellHover(this::previewAt);
        boardPanel.setOnCellClick(this::placeAt);
        addBoardAndControls();
        installRotateShortcut();
        refreshControls();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0x050C17), 0, getHeight(), new Color(0x0B1E34)));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }

    private void addBoardAndControls() {
        OceanMapBackdrop.DragonAmbientOverlay dragonOverlay = new OceanMapBackdrop.DragonAmbientOverlay();
        javax.swing.JLayeredPane boardStage = new javax.swing.JLayeredPane() {
            @Override public Dimension getPreferredSize() { return boardPanel.getPreferredSize(); }
            @Override public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                boardPanel.setBounds(0, 0, w, h);
                dragonOverlay.setBounds(0, 0, w, h);
            }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                OceanMapBackdrop.paintOceanMap(g, getWidth(), getHeight(), OceanMapBackdrop.elapsedSeconds(), false);
                g.dispose();
            }
        };
        boardStage.setOpaque(true);
        boardStage.add(boardPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);
        boardStage.add(dragonOverlay, javax.swing.JLayeredPane.PALETTE_LAYER);

        GridBagConstraints boardConstraints = new GridBagConstraints();
        boardConstraints.gridx = 0;
        boardConstraints.gridy = 0;
        boardConstraints.weightx = 1;
        boardConstraints.weighty = 1;
        boardConstraints.fill = GridBagConstraints.BOTH;
        boardConstraints.insets = new Insets(0, 0, 0, UiTokens.SPACE_LG);
        add(boardStage, boardConstraints);

        JPanel controls = createControls();
        controls.setPreferredSize(new Dimension(UiTokens.CONTROL_PANEL_WIDTH, 0));
        GridBagConstraints controlsConstraints = new GridBagConstraints();
        controlsConstraints.gridx = 1;
        controlsConstraints.gridy = 0;
        controlsConstraints.weightx = 0;
        controlsConstraints.weighty = 1;
        controlsConstraints.fill = GridBagConstraints.VERTICAL;
        controlsConstraints.anchor = GridBagConstraints.CENTER;
        add(controls, controlsConstraints);
    }

    private JPanel createControls() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(14, 24, 41, 228));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 189, 248, 72), 1),
                BorderFactory.createEmptyBorder(UiTokens.SPACE_MD, UiTokens.SPACE_MD, UiTokens.SPACE_MD, UiTokens.SPACE_MD)));

        JLabel heading = new JLabel("TRIỂN KHAI HẠM ĐỘI");
        heading.setFont(UiTokens.FONT_SECTION);
        heading.setForeground(UiTokens.TEXT_PRIMARY);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(UiTokens.SPACE_XS));
        countLabel.setFont(UiTokens.FONT_META);
        countLabel.setForeground(UiTokens.TEXT_MUTED);
        panel.add(countLabel);
        panel.add(Box.createVerticalStrut(UiTokens.SPACE_SM));

        JPanel fleetList = new JPanel();
        fleetList.setLayout(new BoxLayout(fleetList, BoxLayout.Y_AXIS));
        fleetList.setOpaque(false);
        for (ShipType type : ShipType.values()) {
            ShipRow row = new ShipRow(type);
            MouseAdapter rowSelection = new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent event) {
                    if (!placedShips.contains(type)) {
                        selectedType = type;
                        status("Đã chọn " + type.displayName + ". Chọn vị trí trên lưới.", UiTokens.TEXT_MUTED);
                        refreshControls();
                        boardPanel.requestFocusInWindow();
                    }
                }
            };
            row.addMouseListener(rowSelection);
            row.name.addMouseListener(rowSelection);
            row.state.addMouseListener(rowSelection);
            row.mark.addMouseListener(rowSelection);
            shipRows.put(type, row);
            fleetList.add(row);
            fleetList.add(Box.createVerticalStrut(UiTokens.SPACE_XS));
        }
        panel.add(fleetList);
        panel.add(Box.createVerticalStrut(UiTokens.SPACE_SM));

        rotateButton.setAlignmentX(LEFT_ALIGNMENT);
        rotateButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTokens.BUTTON_HEIGHT));
        rotateButton.addActionListener(event -> toggleOrientation());
        panel.add(rotateButton);
        panel.add(Box.createVerticalStrut(UiTokens.SPACE_SM));
        TacticalButton randomButton = new TacticalButton("Đặt ngẫu nhiên", false);
        randomButton.setAlignmentX(LEFT_ALIGNMENT);
        randomButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTokens.BUTTON_HEIGHT));
        randomButton.addActionListener(event -> placeRandomly());
        panel.add(randomButton);
        panel.add(Box.createVerticalStrut(UiTokens.SPACE_SM));
        TacticalButton resetButton = new TacticalButton("Đặt lại", false);
        resetButton.setAlignmentX(LEFT_ALIGNMENT);
        resetButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTokens.BUTTON_HEIGHT));
        resetButton.addActionListener(event -> resetBoard());
        panel.add(resetButton);
        panel.add(Box.createVerticalStrut(UiTokens.SPACE_SM));

        JPanel envRow = new JPanel(new java.awt.GridLayout(1, 2, UiTokens.SPACE_SM, 0));
        envRow.setOpaque(false);
        envRow.setAlignmentX(LEFT_ALIGNMENT);
        envRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTokens.CHIP_HEIGHT));
        seasonPill.sync();
        seasonPill.addActionListener(event -> {
            OceanMapBackdrop.Season s = OceanMapBackdrop.cycleSeason();
            SoundEngine.playUiClick();
            SoundEngine.playSeasonTransitionCue(s);
            mapPill.sync();
            seasonPill.sync();
            weatherPill.sync();
            status(s.displayName + " · " + OceanMapBackdrop.activeDragonSummary(), s.accent);
            repaint();
        });
        weatherPill.sync();
        weatherPill.addActionListener(event -> {
            OceanMapBackdrop.WeatherMode w = OceanMapBackdrop.cycleWeatherMode();
            SoundEngine.playUiClick();
            SoundEngine.playWeatherTransitionCue(w, OceanMapBackdrop.sampleStormLevel(OceanMapBackdrop.elapsedSeconds()));
            mapPill.sync();
            weatherPill.sync();
            seasonPill.sync();
            status(w.displayName + " · " + OceanMapBackdrop.activeDragonSummary(), w.accent);
            repaint();
        });
        envRow.add(seasonPill);
        envRow.add(weatherPill);
        panel.add(envRow);
        panel.add(Box.createVerticalStrut(UiTokens.SPACE_XS));

        JPanel mapModeRow = new JPanel(new java.awt.GridLayout(1, 2, UiTokens.SPACE_SM, 0));
        mapModeRow.setOpaque(false);
        mapModeRow.setAlignmentX(LEFT_ALIGNMENT);
        mapModeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTokens.CHIP_HEIGHT));
        mapPill.sync();
        mapPill.addActionListener(event -> {
            OceanMapBackdrop.MapTheme mt = OceanMapBackdrop.cycleMapTheme();
            SoundEngine.playUiClick();
            SoundEngine.refreshThemeMusic();
            SoundEngine.playMapPropCue(mt);
            mapPill.sync();
            seasonPill.sync();
            weatherPill.sync();
            status(mt.displayName + " · " + OceanMapBackdrop.activeDragonSummary(), mt.accent);
            repaint();
        });
        modePill.sync();
        modePill.addActionListener(event -> {
            ZombieMode.GameMode gm = ZombieMode.cycleGameMode();
            SoundEngine.playUiClick();
            SoundEngine.refreshThemeMusic();
            if (gm == ZombieMode.GameMode.TITAN) {
                SoundEngine.playTitanRoar(TitanMode.getActiveBoss());
            }
            modePill.sync();
            titanPill.sync();
            mapPill.sync();
            seasonPill.sync();
            weatherPill.sync();
            refreshControls();
            status(gm.displayName + " · " + OceanMapBackdrop.activeDragonSummary(), gm.accent);
            repaint();
        });
        mapModeRow.add(mapPill);
        mapModeRow.add(modePill);
        panel.add(mapModeRow);

        titanRow = new JPanel(new BorderLayout());
        titanRow.setOpaque(false);
        titanRow.setAlignmentX(LEFT_ALIGNMENT);
        titanRow.setBorder(BorderFactory.createEmptyBorder(UiTokens.SPACE_XS, 0, 0, 0));
        titanRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTokens.CHIP_HEIGHT + UiTokens.SPACE_XS));
        titanPill.sync();
        titanPill.addActionListener(event -> {
            TitanMode.TitanBoss boss = TitanMode.cycleActiveBoss();
            SoundEngine.playUiClick();
            SoundEngine.refreshThemeMusic();
            SoundEngine.playTitanRoar(boss);
            titanPill.sync();
            refreshControls();
            status("Chọn Boss: " + boss.displayName, boss.accentColor);
            repaint();
        });
        titanRow.add(titanPill, BorderLayout.CENTER);
        titanRow.setVisible(ZombieMode.isTitanMode());
        panel.add(titanRow);
        panel.add(Box.createVerticalStrut(UiTokens.SPACE_SM));

        statusLabel.setFont(UiTokens.FONT_META);
        statusLabel.setForeground(UiTokens.TEXT_MUTED);
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        statusLabel.setPreferredSize(new Dimension(UiTokens.CONTROL_PANEL_WIDTH - UiTokens.SPACE_LG * 2, UiTokens.STATUS_HEIGHT));
        statusLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTokens.STATUS_HEIGHT));
        panel.add(statusLabel);
        panel.add(Box.createVerticalGlue());

        startButton.setAlignmentX(LEFT_ALIGNMENT);
        startButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTokens.BUTTON_HEIGHT));
        startButton.addActionListener(event -> {
            if (ZombieMode.isTitanMode() && onStartTitan != null) {
                SoundEngine.playUiClick();
                onStartTitan.accept(getPlacedShips());
                return;
            }
            if (placedShips.size() == ShipType.values().length && onStartGame != null) onStartGame.accept(playerBoard);
        });
        panel.add(startButton);
        panel.add(Box.createVerticalStrut(UiTokens.SPACE_XS));

        sandboxButton.setAlignmentX(LEFT_ALIGNMENT);
        sandboxButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTokens.BUTTON_HEIGHT));
        sandboxButton.setToolTipText("Vào ngay chế độ Sandbox để test đủ 4 vũ khí, 4 ma thuật, 5 map, 4 mùa, rồng, Zombie & âm thanh");
        sandboxButton.addActionListener(event -> {
            SoundEngine.playUiClick();
            if (onStartSandbox != null) onStartSandbox.accept(getPlacedShips());
        });
        panel.add(sandboxButton);
        return panel;
    }

    private void installRotateShortcut() {
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke('R'), "rotateFleet");
        getActionMap().put("rotateFleet", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { toggleOrientation(); }
        });
    }

    private void toggleOrientation() {
        horizontal = !horizontal;
        SoundEngine.playUiClick();
        rotateButton.setText("Xoay: " + (horizontal ? "Ngang" : "Dọc"));
        if (boardPanel.getMousePosition() != null) {
            boardPanel.refreshPointerHover();
        } else {
            status("Hướng tàu: " + (horizontal ? "Ngang" : "Dọc") + " (phím R để xoay).", UiTokens.TEXT_MUTED);
        }
    }

    private void previewAt(int row, int col) {
        if (row < 0 || col < 0 || selectedType == null) {
            boardPanel.clearShipPreview();
            return;
        }
        boolean valid = isValidPreview(selectedType, row, col, horizontal);
        boardPanel.setShipPreview(row, col, selectedType, horizontal, valid);
        String coordinate = "" + (char) ('A' + col) + (row + 1);
        status(coordinate + " · " + (horizontal ? "Ngang" : "Dọc") + " · " + (valid ? "Có thể đặt" : "Vị trí không hợp lệ"),
                valid ? UiTokens.TEXT_MUTED : UiTokens.STATUS_WARNING);
    }

    private boolean isValidPreview(ShipType type, int row, int col, boolean isHorizontal) {
        for (int offset = 0; offset < type.length; offset++) {
            int targetRow = row + (isHorizontal ? 0 : offset);
            int targetCol = col + (isHorizontal ? offset : 0);
            if (targetRow < 0 || targetRow >= Board.SIZE || targetCol < 0 || targetCol >= Board.SIZE) return false;
            if (playerBoard.getCell(targetRow, targetCol) != CellState.EMPTY) return false;
        }
        return true;
    }

    private void placeAt(int row, int col) {
        if (selectedType == null) {
            status("Đã triển khai đủ tàu.", UiTokens.STATUS_SUCCESS);
            return;
        }
        try {
            boolean placed = playerBoard.placeShip(selectedType, row, col, horizontal);
            if (!placed) {
                status("Không thể đặt tàu tại vị trí này.", UiTokens.STATUS_WARNING);
                return;
            }
            placedShips.add(selectedType);
            placedShipList.add(new Ship(selectedType, row, col, horizontal));
            SoundEngine.playShipPlace();
            boardPanel.setKnownShips(placedShipList);
            selectedType = nextUnplacedShip();
            boardPanel.clearShipPreview();
            boardPanel.refresh();
            status(selectedType == null ? "Hạm đội đã sẵn sàng." : "Đã đặt tàu. Chọn tàu tiếp theo.", UiTokens.STATUS_SUCCESS);
            refreshControls();
        } catch (UnsupportedOperationException exception) {
            status("Logic đặt tàu chưa sẵn sàng (TODO bên module logic).", UiTokens.STATUS_WARNING);
        }
    }

    private void placeRandomly() {
        try {
            playerBoard.placeRandomly();
            placedShips.addAll(EnumSet.allOf(ShipType.class));
            placedShipList.clear();
            SoundEngine.playShipPlace();
            boardPanel.setKnownShips(null);
            selectedType = null;
            boardPanel.clearShipPreview();
            boardPanel.refresh();
            status("Đã triển khai ngẫu nhiên đủ 5 tàu.", UiTokens.STATUS_SUCCESS);
            refreshControls();
        } catch (UnsupportedOperationException exception) {
            status("Logic đặt ngẫu nhiên chưa sẵn sàng (TODO bên module logic).", UiTokens.STATUS_WARNING);
        }
    }

    private void resetBoard() {
        playerBoard = new Board();
        placedShips.clear();
        placedShipList.clear();
        SoundEngine.playUiClick();
        boardPanel.setKnownShips(null);
        selectedType = ShipType.values()[0];
        horizontal = true;
        rotateButton.setText("Xoay: Ngang");
        boardPanel.setBoard(playerBoard);
        boardPanel.clearShipPreview();
        boardPanel.clearLastShot();
        boardPanel.refresh();
        status("Bàn cờ đã được làm trống.", UiTokens.TEXT_MUTED);
        refreshControls();
    }

    private ShipType nextUnplacedShip() {
        for (ShipType type : ShipType.values()) if (!placedShips.contains(type)) return type;
        return null;
    }

    private void refreshControls() {
        countLabel.setText("ĐÃ ĐẶT " + placedShips.size() + "/" + ShipType.values().length + " TÀU");
        for (Map.Entry<ShipType, ShipRow> entry : shipRows.entrySet()) {
            entry.getValue().update(placedShips.contains(entry.getKey()), entry.getKey() == selectedType);
        }
        boolean titan = ZombieMode.isTitanMode();
        if (titanRow != null) {
            titanRow.setVisible(titan);
            titanPill.sync();
        }
        if (titan) {
            TitanMode.TitanBoss b = TitanMode.getActiveBoss();
            startButton.setText("Đấu " + b.shortLabel);
            startButton.setToolTipText("Vào trận Đấu Boss Titan (" + b.displayName + "); tự xếp 5 tàu nếu bạn chưa đặt hết");
            startButton.setEnabled(true);
        } else {
            startButton.setText("Bắt đầu chiến đấu");
            startButton.setToolTipText(null);
            startButton.setEnabled(placedShips.size() == ShipType.values().length);
        }
        rotateButton.setText("Xoay: " + (horizontal ? "Ngang" : "Dọc"));
        revalidate();
    }

    private void status(String text, Color color) {
        statusLabel.setText("<html><div style='width: 270px'>" + text + "</div></html>");
        statusLabel.setForeground(color);
    }

    public List<Ship> getPlacedShips() { return List.copyOf(placedShipList); }

    public void setOnStartSandbox(Consumer<List<Ship>> listener) { this.onStartSandbox = listener; }

    public void setOnStartTitan(Consumer<List<Ship>> listener) { this.onStartTitan = listener; }

    public void showStatus(String text) { status(text, UiTokens.STATUS_WARNING); }

    private static final class ShipRow extends JPanel {
        private final ShipType type;
        private final JLabel name = new JLabel();
        private final JLabel state = new JLabel();
        private final ShipLengthMark mark;

        private ShipRow(ShipType type) {
            super(new BorderLayout(UiTokens.SPACE_SM, 0));
            this.type = type;
            this.mark = new ShipLengthMark(type);
            setPreferredSize(new Dimension(UiTokens.CONTROL_PANEL_WIDTH - UiTokens.SPACE_LG * 2, UiTokens.SHIP_CARD_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTokens.SHIP_CARD_HEIGHT));
            setBorder(BorderFactory.createEmptyBorder(UiTokens.SPACE_SM, UiTokens.SPACE_SM, UiTokens.SPACE_SM, UiTokens.SPACE_SM));
            JPanel labels = new JPanel();
            labels.setOpaque(false);
            labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
            name.setFont(UiTokens.FONT_BODY);
            name.setForeground(UiTokens.TEXT_PRIMARY);
            state.setFont(UiTokens.FONT_META);
            labels.add(name);
            labels.add(state);
            add(labels, BorderLayout.CENTER);
            add(mark, BorderLayout.EAST);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            update(false, false);
        }

        private void update(boolean placed, boolean selected) {
            name.setText(type.displayName);
            state.setText(placed ? "Đã đặt" : "Chờ đặt");
            state.setForeground(placed ? UiTokens.STATUS_SUCCESS : UiTokens.TEXT_MUTED);
            setBackground(selected ? UiTokens.BG_ELEVATED : UiTokens.BG_SURFACE);
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(selected ? UiTokens.ACCENT_SIGNAL : UiTokens.BORDER_SUBTLE, 1),
                    BorderFactory.createEmptyBorder(UiTokens.SPACE_SM, UiTokens.SPACE_SM, UiTokens.SPACE_SM, UiTokens.SPACE_SM)));
            mark.setForeground(placed ? UiTokens.TEXT_MUTED : UiTokens.STEEL_EDGE);
            mark.setPlaced(placed);
            repaint();
        }
    }

    private static final class ShipLengthMark extends JPanel {
        private final ShipType type;
        private boolean placed;

        private ShipLengthMark(ShipType type) {
            this.type = type;
            setOpaque(false);
            setPreferredSize(new Dimension(112, UiTokens.SPACE_LG));
        }

        private void setPlaced(boolean placed) { this.placed = placed; }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            BufferedImage sprite = SpriteAssets.getShipSprite(type, true);
            if (sprite != null) {
                int maxW = getWidth() - 4;
                int unit = Math.max(12, maxW / 5);
                int drawW = Math.min(maxW, type.length * unit);
                int drawH = Math.min(getHeight() - 2, Math.max(14, (int) Math.round(drawW * (sprite.getHeight() / (double) sprite.getWidth()))));
                int x = getWidth() - drawW;
                int y = (getHeight() - drawH) / 2;
                Composite prev = g.getComposite();
                g.setComposite(AlphaComposite.SrcOver.derive(placed ? 0.38f : 1.0f));
                SpriteAssets.drawSharp(g, sprite, x, y, drawW, drawH);
                g.setComposite(prev);
            } else {
                int cell = UiTokens.SPACE_SM;
                int gap = UiTokens.SPACE_XS;
                int y = (getHeight() - cell) / 2;
                g.setStroke(new BasicStroke(1f));
                for (int index = 0; index < type.length; index++) {
                    int x = getWidth() - (type.length * cell + (type.length - 1) * gap) + index * (cell + gap);
                    g.setColor(getForeground());
                    g.fillRoundRect(x, y, cell, cell, UiTokens.SPACE_XS, UiTokens.SPACE_XS);
                    g.setColor(UiTokens.STEEL_EDGE);
                    g.drawRoundRect(x, y, cell, cell, UiTokens.SPACE_XS, UiTokens.SPACE_XS);
                }
            }
            g.dispose();
        }
    }
}
