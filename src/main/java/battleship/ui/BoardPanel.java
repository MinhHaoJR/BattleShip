package battleship.ui;

import battleship.model.Board;
import battleship.model.CellState;
import battleship.model.Ship;
import battleship.model.ShipType;

import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/** A reusable, keyboard-accessible 10x10 naval chart with Blender 3D ships and FX animations. */
public class BoardPanel extends JPanel {
    private static final int FX_TICK_MS = 22;
    private static final int SHIELD_IDLE_TICK_MS = 38;
    private static final double FX_DURATION_MS = 560.0;
    private static final double SHIP_FIRE_DURATION_MS = 500.0;
    private static final double SHIP_HIT_DURATION_MS = 520.0;
    private static final double SHIELD_BLOCK_DURATION_MS = 460.0;

    private Board board;
    private CellState[][] cellStateOverride;
    private String title;
    private boolean showShips = true;
    private boolean zombieFleet;
    private TitanMode.TitanBoss titanBoss;
    private int titanHp = 20;
    private int titanMaxHp = 20;
    private boolean titanEnraged;
    private long titanChargeUntilMs;
    private long titanHitFlashUntilMs;
    private long titanEmergenceStartMs;
    private long titanEmergenceUntilMs;
    private Color titanHitFlashColor = UiTokens.ACCENT_SIGNAL;
    private final List<WaterDrop> titanWaterDrops = new ArrayList<>();
    private final Random titanWaterRng = new Random(77L);
    private long lastWaterDropSpawnMs;
    private boolean interactive;
    private int hoveredRow = -1;
    private int hoveredCol = -1;
    private int focusedRow;
    private int focusedCol;
    private int previewRow = -1;
    private int previewCol = -1;
    private int previewLength;
    private ShipType previewShipType;
    private boolean previewHorizontal;
    private boolean previewValid;
    private int lastShotRow = -1;
    private int lastShotCol = -1;
    private List<int[]> targetPreviewCells = List.of();
    private Color targetPreviewColor = UiTokens.ACCENT_SIGNAL;
    private List<int[]> revealedCells = List.of();
    public static final int SHIELD_MAX_ENERGY = 100;
    public record ShieldAbsorptionResult(int absorbedDamage, int remainingEnergy, boolean shattered) { }

    private List<int[]> shieldedCells = List.of();
    private boolean fullMapShield;
    private long fullMapShieldStartMs;
    private int shieldEnergy = 0;
    private long shieldShatterUntilMs;
    private static final double FULL_SHIELD_RISE_MS = 850.0;
    private int superShipRow = -1;
    private int superShipCol = -1;
    private int superShipLength = 0;
    private boolean superShipHorizontal = true;
    private long superShipStartMs;
    private final int[][] superArmorGrid = new int[Board.SIZE][Board.SIZE];
    private BufferedImage fullShieldHexCache;
    private int shieldCacheGridSize = -1;
    private List<Ship> knownShips = List.of();
    private final List<ActiveEffect> activeEffects = new ArrayList<>();
    private final List<ShipFireAnim> shipFires = new ArrayList<>();
    private final List<ShipHitAnim> shipHits = new ArrayList<>();
    private final List<ShieldBlockAnim> shieldBlocks = new ArrayList<>();
    private final List<ShieldCrack> shieldCracks = new ArrayList<>();
    private final List<ImpactZone> impactZones = new ArrayList<>();
    private final Timer fxTimer;
    private double shakeAmplitude;
    private double shakePhase;
    private long waterBlowStartMs;
    private long waterBlowUntilMs;
    private double waterBlowVx;
    private double waterBlowVy;
    private Color waterBlowTint = new Color(0x38BDF8);
    private int waterBlowIntensity = 1;
    private BufferedImage staticGridCache;
    private int cacheW = -1;
    private int cacheH = -1;
    private double cacheScaleX = 1.0;
    private double cacheScaleY = 1.0;
    private int cacheHoverRow = Integer.MIN_VALUE;
    private int cacheHoverCol = Integer.MIN_VALUE;
    private boolean cacheShowShips;
    private boolean cacheZombieFleet;
    private TitanMode.TitanBoss cacheTitanBoss;
    private OceanMapBackdrop.MapTheme cacheMapTheme;
    private String cacheTitle;
    private boolean ambientStageDriven;
    private final byte[] cacheCells = new byte[Board.SIZE * Board.SIZE];
    private final byte[] scratchCells = new byte[Board.SIZE * Board.SIZE];
    private BiConsumer<Integer, Integer> onCellClick;
    private BiConsumer<Integer, Integer> onCellHover;

    public BoardPanel(Board board) {
        this(board, "Bảng Tọa Độ 10×10", true);
    }

    public BoardPanel(Board board, String title, boolean showShips) {
        this.board = board;
        this.title = title == null ? "" : title;
        this.showShips = showShips;
        SpriteAssets.preloadEssential();
        setOpaque(false);
        setBackground(UiTokens.BG_SURFACE);
        setFocusable(true);
        setPreferredSize(new Dimension(UiTokens.BOARD_PREFERRED_WIDTH, UiTokens.BOARD_PREFERRED_HEIGHT));
        fxTimer = new Timer(FX_TICK_MS, event -> advanceEffects());
        addHierarchyListener(event -> {
            if (isShowing()) {
                ensureFxTimerState();
            } else {
                fxTimer.stop();
            }
        });
        installMouseHandling();
        installKeyboardHandling();
        updateCursor();
    }

    /**
     * When true, idle shield and impact-zone pulse frames piggyback on the stage backdrop timer
     * so separate 38 ms board timers do not multiply repaints across the layered stage.
     */
    public void setAmbientStageDriven(boolean ambientStageDriven) {
        this.ambientStageDriven = ambientStageDriven;
        ensureFxTimerState();
    }

    private void ensureFxTimerState() {
        long now = System.currentTimeMillis();
        boolean hasActive = !activeEffects.isEmpty() || !shipFires.isEmpty()
                || !shipHits.isEmpty() || !shieldBlocks.isEmpty() || shakeAmplitude > 0.25
                || titanChargeUntilMs > now || titanHitFlashUntilMs > now
                || titanEmergenceUntilMs > now || !titanWaterDrops.isEmpty()
                || waterBlowUntilMs > now
                || shieldShatterUntilMs > now
                || (fullMapShield && (now - fullMapShieldStartMs) < FULL_SHIELD_RISE_MS)
                || (superShipRow >= 0 && (now - superShipStartMs) < 1200L);
        boolean hasAmbientZones = fullMapShield || !shieldedCells.isEmpty() || !impactZones.isEmpty()
                || titanBoss != null || superShipRow >= 0;
        if (hasActive) {
            fxTimer.setDelay(FX_TICK_MS);
            if (!fxTimer.isRunning()) fxTimer.start();
        } else if (hasAmbientZones && !ambientStageDriven && isShowing()) {
            fxTimer.setDelay(SHIELD_IDLE_TICK_MS);
            if (!fxTimer.isRunning()) fxTimer.start();
        } else {
            fxTimer.stop();
        }
    }

    private void advanceEffects() {
        long now = System.currentTimeMillis();
        for (int i = activeEffects.size() - 1; i >= 0; i--) {
            ActiveEffect effect = activeEffects.get(i);
            long elapsed = now - effect.startMs;
            if (elapsed < 0) continue;
            double progress = elapsed / FX_DURATION_MS;
            if (progress >= 1.0) {
                activeEffects.remove(i);
            } else {
                effect.progress = progress;
                effect.frame = Math.min(SpriteAssets.fxFrameCount() - 1,
                        (int) (progress * SpriteAssets.fxFrameCount()));
            }
        }
        shipFires.removeIf(anim -> (now - anim.startMs) >= SHIP_FIRE_DURATION_MS);
        shipHits.removeIf(anim -> (now - anim.startMs) >= SHIP_HIT_DURATION_MS);
        shieldBlocks.removeIf(anim -> (now - anim.startMs) >= SHIELD_BLOCK_DURATION_MS);

        if (shakeAmplitude > 0.25) {
            shakeAmplitude *= 0.78;
            shakePhase += 2.3;
        } else {
            shakeAmplitude = 0.0;
        }
        updateTitanWaterDrops(now);
        ensureFxTimerState();
        repaint();
    }

    private void updateTitanWaterDrops(long now) {
        if (titanBoss != null) {
            Layout layout = computeLayout();
            int[] bounds = computeTitanBounds(layout, now);
            double waterRatio = titanWaterRatio(titanBoss);
            double waterY = bounds[1] + bounds[3] * waterRatio;
            boolean surging = now < titanEmergenceUntilMs || now < titanChargeUntilMs;
            long cadence = surging ? 28L : 140L;
            if (now - lastWaterDropSpawnMs >= cadence) {
                lastWaterDropSpawnMs = now;
                int count = surging ? 4 : 1;
                spawnTitanWaterDropsAt(bounds[0], bounds[2], waterY, count, surging ? 1.15 : 0.45, now);
            }
        }
        for (int i = titanWaterDrops.size() - 1; i >= 0; i--) {
            WaterDrop d = titanWaterDrops.get(i);
            d.x += d.vx;
            d.y += d.vy;
            d.vy += 0.36;
            if (now >= d.endMs || (d.vy > 0 && d.y > d.waterY + d.sinkLimit)) {
                titanWaterDrops.remove(i);
            }
        }
    }

    private void spawnTitanWaterDropsAt(double bx, double bw, double waterY, int count, double power, long now) {
        if (bw <= 8) return;
        for (int i = 0; i < count; i++) {
            if (titanWaterDrops.size() >= 140) break;
            double px = bx + bw * (0.14 + titanWaterRng.nextDouble() * 0.72);
            double fromCenter = (px - (bx + bw * 0.50)) / Math.max(1.0, bw * 0.38);
            double vx = fromCenter * (1.2 + titanWaterRng.nextDouble() * 2.2) * power;
            double vy = -(2.4 + titanWaterRng.nextDouble() * 4.4) * power;
            double sz = 2.8 + titanWaterRng.nextDouble() * (power > 0.9 ? 4.4 : 2.8);
            double sinkLimit = 5.0 + titanWaterRng.nextDouble() * 8.0;
            boolean foam = titanWaterRng.nextDouble() < 0.58;
            titanWaterDrops.add(new WaterDrop(px, waterY, vx, vy, sz, waterY, sinkLimit, foam,
                    now + 520L + titanWaterRng.nextInt(420)));
        }
    }

    private void installMouseHandling() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent event) { updateHover(event.getX(), event.getY()); }
            @Override public void mouseDragged(MouseEvent event) { updateHover(event.getX(), event.getY()); }
            @Override public void mouseExited(MouseEvent event) { setHover(-1, -1); }
            @Override public void mouseClicked(MouseEvent event) {
                if (interactive && event.getButton() == MouseEvent.BUTTON1) {
                    int[] cell = cellAt(event.getX(), event.getY());
                    if (cell != null) {
                        requestFocusInWindow();
                        activateCell(cell[0], cell[1]);
                    }
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private void installKeyboardHandling() {
        bind("UP", "moveUp", 0, -1);
        bind("DOWN", "moveDown", 0, 1);
        bind("LEFT", "moveLeft", -1, 0);
        bind("RIGHT", "moveRight", 1, 0);
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "activateCell");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "activateCell");
        getActionMap().put("activateCell", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                if (interactive) activateCell(focusedRow, focusedCol);
            }
        });
    }

    private void bind(String key, String name, int dx, int dy) {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                focusedCol = clamp(focusedCol + dx, 0, Board.SIZE - 1);
                focusedRow = clamp(focusedRow + dy, 0, Board.SIZE - 1);
                setHover(focusedRow, focusedCol);
                repaint();
            }
        });
    }

    private void updateHover(int x, int y) {
        if (!interactive) return;
        int[] cell = cellAt(x, y);
        setHover(cell == null ? -1 : cell[0], cell == null ? -1 : cell[1]);
    }

    private void setHover(int row, int col) {
        if (hoveredRow == row && hoveredCol == col) return;
        hoveredRow = row;
        hoveredCol = col;
        if (row >= 0) {
            focusedRow = row;
            focusedCol = col;
            setToolTipText("Tọa độ đang ngắm: " + (char) ('A' + col) + (row + 1));
        } else {
            setToolTipText(null);
        }
        if (onCellHover != null) onCellHover.accept(row, col);
        repaint();
    }

    private void activateCell(int row, int col) {
        if (onCellClick != null) onCellClick.accept(row, col);
    }

    private int[] cellAt(int x, int y) {
        Layout layout = computeLayout();
        int col = (x - layout.gridX) / layout.cellSize;
        int row = (y - layout.gridY) / layout.cellSize;
        if (x < layout.gridX || y < layout.gridY || row < 0 || col < 0 || row >= Board.SIZE || col >= Board.SIZE) return null;
        return new int[]{row, col};
    }

    private Layout computeLayout() {
        int labels = UiTokens.BOARD_LABEL_GUTTER;
        int availableWidth = Math.max(0, getWidth() - labels - UiTokens.SPACE_MD * 2);
        int titleSpace = title.isBlank() ? 0 : UiTokens.SPACE_LG;
        int availableHeight = Math.max(0, getHeight() - labels - UiTokens.SPACE_MD * 2 - titleSpace);
        int cellSize = Math.max(1, Math.min(availableWidth / Board.SIZE, availableHeight / Board.SIZE));
        int gridSize = cellSize * Board.SIZE;
        int gridX = labels + (getWidth() - labels - gridSize) / 2;
        int gridY = titleSpace + labels + (getHeight() - titleSpace - labels - gridSize) / 2;
        return new Layout(cellSize, gridX, gridY);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (shakeAmplitude > 0.25) {
            int dx = (int) Math.round(Math.cos(shakePhase) * shakeAmplitude);
            int dy = (int) Math.round(Math.sin(shakePhase * 1.4) * shakeAmplitude);
            g.translate(dx, dy);
        }
        Layout layout = computeLayout();
        drawBoard(g, layout);
        g.dispose();
    }

    private void drawTitle(Graphics2D g) {
        if (title.isBlank()) return;
        g.setFont(UiTokens.FONT_SUBTITLE);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(title);
        int tx = (getWidth() - tw) / 2;
        int ty = UiTokens.SPACE_LG;
        g.setColor(new Color(9, 18, 32, 188));
        g.fillRoundRect(tx - 10, ty - fm.getAscent() - 2, tw + 20, fm.getHeight() + 4, 10, 10);
        g.setColor(new Color(56, 189, 248, 72));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(tx - 10, ty - fm.getAscent() - 2, tw + 20, fm.getHeight() + 4, 10, 10);
        g.setColor(UiTokens.TEXT_PRIMARY);
        g.drawString(title, tx, ty);
    }

    private void ensureStaticGridCache(Graphics2D targetG, Layout layout, boolean[][] coveredBySprite) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            staticGridCache = null;
            return;
        }
        double sx = 1.0;
        double sy = 1.0;
        AffineTransform targetTx = targetG.getTransform();
        if (targetTx.getShearX() == 0.0 && targetTx.getShearY() == 0.0
                && targetTx.getScaleX() > 0.0 && targetTx.getScaleY() > 0.0) {
            sx = targetTx.getScaleX();
            sy = targetTx.getScaleY();
        }
        int devW = Math.max(1, (int) Math.round(w * sx));
        int devH = Math.max(1, (int) Math.round(h * sy));
        int effHoverR = interactive ? hoveredRow : -1;
        int effHoverC = interactive ? hoveredCol : -1;
        OceanMapBackdrop.MapTheme theme = OceanMapBackdrop.getMapTheme();

        int idx = 0;
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                CellState st = cellStateAt(row, col);
                if (st == CellState.SHIP && !showShips) st = CellState.EMPTY;
                scratchCells[idx++] = (byte) ((st.ordinal() & 0xF) | (coveredBySprite[row][col] ? 0x10 : 0));
            }
        }

        boolean dirty = staticGridCache == null
                || cacheW != w
                || cacheH != h
                || Math.abs(cacheScaleX - sx) > 1e-3
                || Math.abs(cacheScaleY - sy) > 1e-3
                || cacheHoverRow != effHoverR
                || cacheHoverCol != effHoverC
                || cacheShowShips != showShips
                || cacheZombieFleet != zombieFleet
                || cacheTitanBoss != titanBoss
                || cacheMapTheme != theme
                || !Objects.equals(cacheTitle, title)
                || !Arrays.equals(cacheCells, scratchCells);
        if (!dirty) return;

        if (staticGridCache == null || staticGridCache.getWidth() != devW || staticGridCache.getHeight() != devH) {
            staticGridCache = new BufferedImage(devW, devH, BufferedImage.TYPE_INT_ARGB_PRE);
        }
        Graphics2D bg = staticGridCache.createGraphics();
        bg.setComposite(AlphaComposite.Clear);
        bg.fillRect(0, 0, devW, devH);
        bg.setComposite(AlphaComposite.SrcOver);
        if (Math.abs(sx - 1.0) > 1e-3 || Math.abs(sy - 1.0) > 1e-3) {
            bg.scale(sx, sy);
        }
        bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        bg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        bg.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        paintStaticGridLayer(bg, layout, coveredBySprite, effHoverR, effHoverC);
        bg.dispose();

        cacheW = w;
        cacheH = h;
        cacheScaleX = sx;
        cacheScaleY = sy;
        cacheHoverRow = effHoverR;
        cacheHoverCol = effHoverC;
        cacheShowShips = showShips;
        cacheZombieFleet = zombieFleet;
        cacheTitanBoss = titanBoss;
        cacheMapTheme = theme;
        cacheTitle = title;
        System.arraycopy(scratchCells, 0, cacheCells, 0, scratchCells.length);
    }

    private void paintStaticGridLayer(
            Graphics2D g, Layout layout, boolean[][] coveredBySprite, int effHoverRow, int effHoverCol) {
        drawTitle(g);
        int s = layout.cellSize;
        g.setFont(UiTokens.FONT_COORD);
        FontMetrics fm = g.getFontMetrics();
        for (int col = 0; col < Board.SIZE; col++) {
            int x = layout.gridX + col * s;
            boolean active = effHoverCol == col;
            g.setColor(active ? UiTokens.TEXT_PRIMARY : UiTokens.TEXT_MUTED);
            String label = String.valueOf((char) ('A' + col));
            g.drawString(label, x + (s - fm.stringWidth(label)) / 2, layout.gridY - UiTokens.SPACE_SM);
        }
        for (int row = 0; row < Board.SIZE; row++) {
            int y = layout.gridY + row * s;
            boolean active = effHoverRow == row;
            g.setColor(active ? UiTokens.TEXT_PRIMARY : UiTokens.TEXT_MUTED);
            String label = Integer.toString(row + 1);
            g.drawString(label, layout.gridX - UiTokens.SPACE_SM - fm.stringWidth(label), y + (s - fm.getHeight()) / 2 + fm.getAscent());
        }

        // Pass 1: Ocean grid base (suppressing fallback vector hull on cells covered by a 3D ship sprite)
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                int x = layout.gridX + col * s;
                int y = layout.gridY + row * s;
                CellState state = cellStateAt(row, col);
                if (state == CellState.SHIP && !showShips) state = CellState.EMPTY;
                boolean hover = row == effHoverRow && col == effHoverCol;
                if (coveredBySprite[row][col] && state == CellState.SHIP) {
                    drawCell(g, x, y, s, CellState.EMPTY, hover);
                } else if (coveredBySprite[row][col] && state == CellState.HIT) {
                    drawCell(g, x, y, s, CellState.EMPTY, hover);
                } else {
                    drawCell(g, x, y, s, state, hover);
                }
            }
        }

        // Pass 1b: Subtle deep-ocean radial glow + modern-arcane radar bezel & corner brackets
        int gridPx = s * Board.SIZE;
        Paint prevPaint = g.getPaint();
        g.setPaint(new RadialGradientPaint(
                layout.gridX + gridPx * 0.5f, layout.gridY + gridPx * 0.5f, Math.max(1f, gridPx * 0.72f),
                new float[]{0f, 0.72f, 1f},
                new Color[]{new Color(56, 189, 248, 22), new Color(15, 36, 59, 0), new Color(4, 10, 20, 74)}
        ));
        g.fillRect(layout.gridX, layout.gridY, gridPx, gridPx);
        g.setPaint(prevPaint);
        g.setColor(OceanMapBackdrop.boardBorderColor(zombieFleet));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(layout.gridX, layout.gridY, gridPx, gridPx);
        drawHudCornerBrackets(g, layout.gridX - 3, layout.gridY - 3, gridPx + 6, Math.max(10, s / 3));
    }

    private void drawBoard(Graphics2D g, Layout layout) {
        int s = layout.cellSize;
        boolean[][] coveredBySprite = new boolean[Board.SIZE][Board.SIZE];
        List<ShipSpan> shipSpans;
        if (titanBoss != null) {
            shipSpans = List.of();
            for (int[] hc : TitanMode.generateHitboxCells(titanBoss)) {
                if (hc[0] >= 0 && hc[0] < Board.SIZE && hc[1] >= 0 && hc[1] < Board.SIZE) {
                    coveredBySprite[hc[0]][hc[1]] = true;
                }
            }
        } else if (showShips) {
            shipSpans = resolveShipSpans(coveredBySprite);
        } else if (!knownShips.isEmpty()) {
            boolean[][] tmp = new boolean[Board.SIZE][Board.SIZE];
            List<ShipSpan> candidates = resolveShipSpans(tmp);
            shipSpans = new ArrayList<>();
            for (ShipSpan span : candidates) {
                if (!isSpanAlive(span)) {
                    shipSpans.add(span);
                    for (int i = 0; i < span.length; i++) {
                        int r = span.row + (span.horizontal ? 0 : i);
                        int c = span.col + (span.horizontal ? i : 0);
                        if (r >= 0 && r < Board.SIZE && c >= 0 && c < Board.SIZE) coveredBySprite[r][c] = true;
                    }
                }
            }
        } else {
            shipSpans = List.of();
        }

        ensureStaticGridCache(g, layout, coveredBySprite);
        if (staticGridCache != null) {
            SpriteAssets.drawDeviceAligned(g, staticGridCache, 0, 0);
        } else {
            paintStaticGridLayer(g, layout, coveredBySprite,
                    interactive ? hoveredRow : -1, interactive ? hoveredCol : -1);
        }

        long now = System.currentTimeMillis();

        // Pass 1c: Persistent 3x3 ultimate Impact Zone floor runes & looping Blender 3D aura
        if (!impactZones.isEmpty()) {
            drawImpactZoneFloors(g, layout, now);
        }

        // Pass 2: Floating ocean drop shadows + UHD Blender 3D ship sprites (with per-ship recoil & hit tremor!)
        if (titanBoss != null) {
            drawTitanBossEntity(g, layout, now);
        }
        for (ShipSpan span : shipSpans) {
            boolean superShip = isSuperShipSpan(span);
            BufferedImage sprite;
            BufferedImage shadow;
            if (superShip && SpriteAssets.getSuperShipSprite(span.horizontal) != null) {
                sprite = SpriteAssets.getSuperShipSprite(span.horizontal);
                shadow = SpriteAssets.getSuperShipShadow(span.horizontal);
            } else if (zombieFleet && span.type != null) {
                sprite = SpriteAssets.getZombieShipSprite(span.type, span.horizontal);
                shadow = SpriteAssets.getZombieShipShadow(span.type, span.horizontal);
            } else {
                sprite = span.type != null
                        ? SpriteAssets.getShipSprite(span.type, span.horizontal)
                        : SpriteAssets.getShipSpriteForLength(span.length, span.horizontal);
                shadow = span.type != null
                        ? SpriteAssets.getShipShadow(span.type, span.horizontal)
                        : SpriteAssets.getShipShadowForLength(span.length, span.horizontal);
            }
            if (sprite != null) {
                int[] animOff = computeShipOffset(span, now);
                int sx = layout.gridX + span.col * s + animOff[0];
                int sy = layout.gridY + span.row * s + animOff[1];
                int sw = span.horizontal ? span.length * s : s;
                int sh = span.horizontal ? s : span.length * s;
                int pad = Math.max(1, s / 16);
                if (superShip) {
                    drawSuperShipAuraAndWake(g, span, sx, sy, sw, sh, s, now);
                } else if (zombieFleet && span.type != null) {
                    Color zGlow = ZombieMode.forShipType(span.type).accentColor;
                    Composite prevC = g.getComposite();
                    g.setComposite(AlphaComposite.SrcOver.derive(0.24f));
                    g.setColor(zGlow);
                    g.fillRoundRect(sx, sy, sw, sh, s / 2, s / 2);
                    g.setComposite(prevC);
                }
                int wingExpand = superShip ? Math.max(3, s / 5) : 0;
                int lenExpand = superShip ? (span.length <= 2 ? Math.max(8, s / 2) : Math.max(4, s / 6)) : 0;
                int dx = sx + pad - (span.horizontal ? lenExpand / 2 : wingExpand);
                int dy = sy + pad - (span.horizontal ? wingExpand : lenExpand / 2);
                int dw = sw - pad * 2 + (span.horizontal ? lenExpand : wingExpand * 2);
                int dh = sh - pad * 2 + (span.horizontal ? wingExpand * 2 : lenExpand);
                if (shadow != null) {
                    int off = Math.max(2, s / 14);
                    g.drawImage(shadow, dx + off / 2, dy + off / 2, dw, dh, null);
                }
                SpriteAssets.drawSharp(g, sprite, dx, dy, dw, dh);
                drawShipImpactFlashAndLaunchDeck(g, span, sx, sy, sw, sh, s, now);
                if (superShip) {
                    drawSuperShipArmorPips(g, layout, span, s, now);
                }
            }
        }

        // Pass 3: Permanent HIT/MISS markers on top of 3D ships + previews + cursor
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                int x = layout.gridX + col * s;
                int y = layout.gridY + row * s;
                CellState state = cellStateAt(row, col);
                if (coveredBySprite[row][col] && state == CellState.HIT) {
                    Composite prev = g.getComposite();
                    g.setComposite(AlphaComposite.SrcOver.derive(0.55f));
                    g.setColor(UiTokens.STATE_HIT);
                    g.fillRect(x + 1, y + 1, s - 1, s - 1);
                    g.setComposite(prev);
                    drawHit(g, x + s / 2, y + s / 2, s);
                }
                if (containsCell(targetPreviewCells, row, col)) drawTargetPreview(g, x, y, s);
                if (containsCell(revealedCells, row, col)) drawRevealedCell(g, x, y, s);
                if (isPreviewCell(row, col)) drawPreview(g, x, y, s);
                if (row == lastShotRow && col == lastShotCol) {
                    g.setColor(UiTokens.ACCENT_SIGNAL);
                    g.setStroke(new BasicStroke(2f));
                    g.drawRoundRect(x + 3, y + 3, s - 6, s - 6, UiTokens.BORDER_RADIUS / 2, UiTokens.BORDER_RADIUS / 2);
                }
                if (hasFocus() && interactive && row == focusedRow && col == focusedCol) {
                    g.setColor(UiTokens.FOCUS_RING);
                    g.setStroke(new BasicStroke(2f));
                    g.drawRoundRect(x + 2, y + 2, s - 4, s - 4, UiTokens.RADIUS_CARD / 2, UiTokens.RADIUS_CARD / 2);
                }
            }
        }

        // Pass 3.5: Subtle weather reactivity on sea grid (raindrop ripples, lightning glare, winter frost glints)
        // + Active Boss physical water-blow foam streaks across board cells
        drawWeatherEffectsOnBoard(g, layout);
        if (waterBlowUntilMs > now) {
            drawWaterBlowOnBoard(g, layout, now);
        }

        // Pass 4: Ghost 3D ship sprite during SetupPanel placement preview
        drawGhostShipPreview(g, layout);

        // Pass 4.4: Animated 3x3 Impact Zone perimeter frame & elemental particle gleam
        if (!impactZones.isEmpty()) {
            drawImpactZonePerimeters(g, layout, now);
        }

        // Pass 4.5: Animated 3D Magic Shield Barrier (khiên phép thuật), Stress Cracks & Deflect Flares
        if (fullMapShield || !shieldedCells.isEmpty() || !shieldBlocks.isEmpty() || shieldShatterUntilMs > now) {
            drawMagicShieldOverlay(g, layout, shipSpans, now);
        }

        // Pass 4.6: Directional 3D Cannon Muzzle Blasts on firing ships
        if (!shipFires.isEmpty()) {
            drawMuzzleBlasts(g, layout, now);
        }

        // Pass 5: Active Blender 3D FX impact animations + layered shockwaves, rays, sparks & water plumes
        for (ActiveEffect effect : activeEffects) {
            if (now < effect.startMs) continue;
            double p = Math.max(0.0, Math.min(1.0, effect.progress));
            int cx = layout.gridX + effect.col * s + s / 2;
            int cy = layout.gridY + effect.row * s + s / 2;
            Composite prevComp = g.getComposite();
            Stroke prevStroke = g.getStroke();
            boolean isMiss = (effect.type == SpriteAssets.FxType.MISS);
            double seedBase = (effect.row * 13 + effect.col * 29 + (effect.startMs % 97)) * 0.67;

            // 1. Subtle bloom flash at impact epicenter (no per-frame Gradient allocations)
            if (p < 0.65) {
                double gp = p / 0.65;
                float gAlpha = (float) ((1.0 - gp) * (isMiss ? 0.42 : 0.66));
                Color bloomCol = isMiss ? new Color(0xBAE6FD) : effect.accent;
                drawGlow(g, bloomCol, cx, cy, s * (0.48 + gp * 0.72), gAlpha, isMiss);
            }

            // 2. Dual concentric voxel pixel-stepped shockwave rings
            float ringAlpha = (float) Math.max(0.0, (1.0 - p) * (isMiss ? 0.80 : 0.90));
            int ringR = (int) Math.round((0.24 + p * (isMiss ? 1.05 : 1.12)) * s);
            int ringPx = Math.max(2, (int) Math.round((isMiss ? 2.6 : 4.0) * (1.0 - p * 0.65)));
            g.setComposite(alphaComp(ringAlpha));
            g.setColor(isMiss ? new Color(0xE0F2FE) : effect.accent);
            SpriteAssets.drawVoxelRing(g, cx, cy, ringR, ringPx, false);

            if (p < 0.72) {
                double pInner = p / 0.72;
                float innerA = (float) ((1.0 - pInner) * (isMiss ? 0.72 : 0.86));
                int innerR = (int) Math.round((0.12 + pInner * 0.78) * s);
                g.setComposite(alphaComp(innerA));
                g.setColor(Color.WHITE);
                SpriteAssets.drawVoxelRing(g, cx, cy, innerR, 2, false);
            }

            // 3. Incandescent contact white-gold voxel flash or chunky voxel smoke cube
            if (!isMiss && p < 0.28) {
                float flashAlpha = (float) ((1.0 - p / 0.28) * 0.78);
                int flashR = (int) Math.round(s * (0.48 - p * 0.55));
                drawGlow(g, Color.WHITE, cx, cy, flashR, flashAlpha * 0.72f, false);
                int coreSz = Math.max(4, (int) Math.round(s * (0.34 - p * 0.40)));
                g.setComposite(alphaComp(flashAlpha));
                SpriteAssets.drawVoxelCube(g, cx, cy, coreSz, Color.WHITE, new Color(0xFEF08A));
            } else if ((effect.type == SpriteAssets.FxType.HIT || effect.type == SpriteAssets.FxType.FIRE) && p > 0.22) {
                double sp = (p - 0.22) / 0.78;
                float smokeAlpha = (float) (Math.sin(sp * Math.PI) * 0.52);
                int smX = (int) Math.round(cx - s * 0.08 * sp);
                int smY = (int) Math.round(cy - s * 0.34 * sp);
                int smSz = Math.max(6, (int) Math.round(s * (0.28 + sp * 0.38)));
                drawGlow(g, new Color(0x64748B), smX, smY, s * (0.34 + sp * 0.45), smokeAlpha * 0.35f, true);
                g.setComposite(alphaComp(smokeAlpha));
                SpriteAssets.drawVoxelCube(g, smX, smY, smSz, new Color(0x64748B), new Color(0x94A3B8));
            }

            // 4. Blender 3D Voxel FX sprite scaled with punchy impact pop-in
            BufferedImage frameImg = SpriteAssets.getFxFrame(effect.type, effect.frame);
            if (frameImg != null) {
                float spriteAlpha = (float) (p > 0.76 ? Math.max(0.0, 1.0 - (p - 0.76) / 0.24) : 1.0);
                g.setComposite(alphaComp(spriteAlpha));
                double scale = 1.30 + Math.sin(Math.min(1.0, p * 1.35) * Math.PI * 0.5) * 0.44;
                int fxSize = (int) Math.round(s * scale);
                SpriteAssets.drawSharp(g, frameImg, cx - fxSize / 2, cy - fxSize / 2, fxSize, fxSize);
            }

            // 5. Elemental signature & velocity voxel debris
            if (isMiss) {
                // Upward arcing chunky pixel water splash cubes
                for (int k = 0; k < 6; k++) {
                    double ka = seedBase + k * (Math.PI * 2.0 / 6.0);
                    double sxp = cx + Math.cos(ka) * (p * s * 0.56);
                    double syp = cy + Math.sin(ka) * (p * s * 0.26) - Math.sin(p * Math.PI) * (s * 0.46);
                    g.setComposite(alphaComp((float) ((1.0 - p) * 0.88)));
                    SpriteAssets.drawVoxelCube(g, (int) Math.round(sxp), (int) Math.round(syp),
                            4, new Color(0x7DD3FC), Color.WHITE);
                }
            } else {
                // High-speed radial voxel spark rays + hot pixel tip cubes
                g.setStroke(roundStroke((float) Math.max(1.0, 2.2 * (1.0 - p * 0.55))));
                int rays = 8;
                for (int k = 0; k < rays; k++) {
                    double ka = seedBase + k * (Math.PI * 2.0 / rays);
                    double r0 = s * (0.18 + p * 0.62);
                    double r1 = r0 + s * (0.14 + (1.0 - p) * 0.34);
                    g.setComposite(alphaComp((float) ((1.0 - p) * 0.88)));
                    Color rayCol = (k % 2 == 0) ? Color.WHITE : effect.accent;
                    g.setColor(rayCol);
                    int tx = (int) (cx + Math.cos(ka) * r1);
                    int ty = (int) (cy + Math.sin(ka) * r1);
                    g.drawLine((int) (cx + Math.cos(ka) * r0), (int) (cy + Math.sin(ka) * r0), tx, ty);
                    SpriteAssets.drawVoxelCube(g, tx, ty, 3, Color.WHITE, effect.accent);
                }

                if (effect.type == SpriteAssets.FxType.SONAR && p > 0.10) {
                    double p2 = (p - 0.10) / 0.90;
                    float a2 = (float) Math.max(0.0, (1.0 - p2) * 0.78);
                    int r2 = (int) Math.round((0.20 + p2 * 1.24) * s);
                    g.setComposite(alphaComp(a2));
                    g.setColor(UiTokens.MAGIC_SONAR);
                    SpriteAssets.drawVoxelRing(g, cx, cy, r2, 3, true);
                } else if (effect.type == SpriteAssets.FxType.LIGHTNING && p < 0.75) {
                    float a2 = (float) Math.max(0.0, (1.0 - p / 0.75) * 0.92);
                    g.setComposite(alphaComp(a2));
                    g.setStroke(roundStroke(2.0f));
                    double rot = p * 6.0 + seedBase;
                    for (int k = 0; k < 5; k++) {
                        double ka = rot + k * (Math.PI * 2.0 / 5.0);
                        int x1 = cx + (int) (Math.cos(ka) * s * 0.28);
                        int y1 = cy + (int) (Math.sin(ka) * s * 0.28);
                        int x2 = cx + (int) (Math.cos(ka + 0.34) * s * 0.56);
                        int y2 = cy + (int) (Math.sin(ka + 0.34) * s * 0.56);
                        int x3 = cx + (int) (Math.cos(ka - 0.14) * s * 0.82);
                        int y3 = cy + (int) (Math.sin(ka - 0.14) * s * 0.82);
                        g.setColor(k % 2 == 0 ? new Color(0xE0F2FE) : UiTokens.MAGIC_LIGHTNING);
                        g.drawLine(cx, cy, x1, y1);
                        g.drawLine(x1, y1, x2, y2);
                        g.drawLine(x2, y2, x3, y3);
                    }
                } else if (effect.type == SpriteAssets.FxType.FROST) {
                    // Spinning faceted 6-point ice crystal star
                    float a2 = (float) Math.max(0.0, (1.0 - p) * 0.85);
                    g.setComposite(alphaComp(a2));
                    g.setStroke(roundStroke(1.8f));
                    double spin = p * 2.2 + seedBase;
                    double arm = s * (0.28 + p * 0.62);
                    for (int k = 0; k < 6; k++) {
                        double ka = spin + k * (Math.PI / 3.0);
                        double bx = cx + Math.cos(ka) * arm * 0.58;
                        double by = cy + Math.sin(ka) * arm * 0.58;
                        double tx = cx + Math.cos(ka) * arm;
                        double ty = cy + Math.sin(ka) * arm;
                        g.setColor(k % 2 == 0 ? Color.WHITE : UiTokens.MAGIC_FROST);
                        g.drawLine(cx, cy, (int) tx, (int) ty);
                        g.drawLine((int) bx, (int) by,
                                (int) (bx + Math.cos(ka + 0.58) * arm * 0.32),
                                (int) (by + Math.sin(ka + 0.58) * arm * 0.32));
                        g.drawLine((int) bx, (int) by,
                                (int) (bx + Math.cos(ka - 0.58) * arm * 0.32),
                                (int) (by + Math.sin(ka - 0.58) * arm * 0.32));
                    }
                }
            }
            g.setComposite(prevComp);
            g.setStroke(prevStroke);
        }
    }

    private static final AlphaComposite[] ALPHA_CACHE = new AlphaComposite[101];
    private static final BasicStroke[] ROUND_STROKES = new BasicStroke[41];
    private static final BasicStroke[] MARCH_STROKES = new BasicStroke[15];
    static {
        for (int i = 0; i <= 100; i++) {
            ALPHA_CACHE[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i / 100f);
        }
        for (int i = 0; i <= 40; i++) {
            float w = Math.max(0.5f, i * 0.5f);
            ROUND_STROKES[i] = new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }
        for (int i = 0; i < MARCH_STROKES.length; i++) {
            MARCH_STROKES[i] = new BasicStroke(
                    1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 4f, new float[]{9f, 6f}, (float) i);
        }
    }
    private static final java.util.Map<Long, BufferedImage> GLOW_STAMP_CACHE =
            java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<Long, BufferedImage>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<Long, BufferedImage> eldest) {
                    return size() > 192;
                }
            });

    private static AlphaComposite alphaComp(double alpha) {
        int idx = Math.max(0, Math.min(100, (int) Math.round(alpha * 100.0)));
        return ALPHA_CACHE[idx];
    }

    private static BasicStroke roundStroke(float width) {
        int idx = Math.max(1, Math.min(40, Math.round(width * 2f)));
        return ROUND_STROKES[idx];
    }

    private static BufferedImage getGlowStamp(Color c, int radiusPx, boolean soft) {
        int r = Math.max(8, Math.min(128, ((radiusPx + 3) / 4) * 4));
        int rgb = (c != null ? c.getRGB() : 0xFFFFFF) & 0x00FFFFFF;
        long key = (((long) r) << 25) | (soft ? (1L << 24) : 0L) | rgb;
        BufferedImage cached = GLOW_STAMP_CACHE.get(key);
        if (cached != null) return cached;

        int size = r * 2 + 2;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cr = (rgb >> 16) & 0xFF;
        int cg = (rgb >> 8) & 0xFF;
        int cb = rgb & 0xFF;
        float center = size * 0.5f;
        float[] dist = soft ? new float[]{0f, 0.45f, 1f} : new float[]{0f, 0.24f, 0.60f, 1f};
        Color[] cols = soft
                ? new Color[]{
                        new Color(cr, cg, cb, 150),
                        new Color(cr, cg, cb, 68),
                        new Color(cr, cg, cb, 0)}
                : new Color[]{
                        new Color(Math.min(255, cr + 165), Math.min(255, cg + 165), Math.min(255, cb + 165), 245),
                        new Color(cr, cg, cb, 210),
                        new Color(cr, cg, cb, 72),
                        new Color(cr, cg, cb, 0)};
        g2.setPaint(new java.awt.RadialGradientPaint(center, center, r, dist, cols));
        g2.fillRect(0, 0, size, size);
        g2.dispose();
        GLOW_STAMP_CACHE.put(key, img);
        return img;
    }

    private static void drawGlow(Graphics2D g, Color c, double cx, double cy, double radius, float alpha, boolean soft) {
        if (alpha <= 0.01f || radius <= 1.0) return;
        int r = (int) Math.round(radius);
        BufferedImage stamp = getGlowStamp(c, r, soft);
        g.setComposite(alphaComp(alpha));
        g.drawImage(stamp, (int) Math.round(cx - r), (int) Math.round(cy - r), r * 2, r * 2, null);
    }

    private static final BasicStroke WEATHER_STROKE_THIN = new BasicStroke(1.2f);
    private static final BasicStroke WEATHER_STROKE_RIM = new BasicStroke(2.0f);

    private void drawWeatherEffectsOnBoard(Graphics2D g, Layout layout) {
        double t = OceanMapBackdrop.elapsedSeconds();
        double storm = OceanMapBackdrop.sampleStormLevel(t);
        double flash = OceanMapBackdrop.sampleLightningFlash(t);
        double winter = OceanMapBackdrop.sampleWinterWeight(t);
        double summer = OceanMapBackdrop.sampleSummerWeight(t);
        if (storm <= 0.16 && flash <= 0.06 && winter <= 0.45 && summer <= 0.45) return;

        int s = layout.cellSize;
        int gridPx = s * Board.SIZE;
        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();

        if (storm > 0.16) {
            // Subtle dark squall frame border + expanding raindrop splashes directly on sea grid
            g.setComposite(AlphaComposite.SrcOver.derive((float) (0.24 * storm)));
            g.setColor(new Color(0x0B243E));
            g.setStroke(WEATHER_STROKE_RIM);
            g.drawRect(layout.gridX + 1, layout.gridY + 1, gridPx - 2, gridPx - 2);

            g.setStroke(WEATHER_STROKE_THIN);
            g.setColor(new Color(0xBAE6FD));
            int boardSeed = showShips ? 17 : 53;
            for (int i = 0; i < 4; i++) {
                double cycle = (t * (1.25 + i * 0.22) + i * 0.37) % 1.0;
                int stepIdx = (int) Math.floor(t * (1.25 + i * 0.22) + i * 0.37);
                int r = Math.abs((stepIdx * 7 + i * 3 + boardSeed) % Board.SIZE);
                int c = Math.abs((stepIdx * 11 + i * 5 + boardSeed) % Board.SIZE);
                int rx = layout.gridX + c * s + s / 2;
                int ry = layout.gridY + r * s + s / 2;
                int rad = (int) Math.max(3, cycle * s * 0.44);
                float alpha = (float) ((1.0 - cycle) * 0.48 * storm);
                if (alpha > 0.02f) {
                    g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                    SpriteAssets.drawVoxelRing(g, rx, ry, rad, 2, false);
                }
            }
        }

        if (flash > 0.06) {
            // Brief lightning illumination glinting off the radar sea surface
            g.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(0.14, flash * 0.12)));
            g.setColor(new Color(0xE0F2FE));
            g.fillRect(layout.gridX, layout.gridY, gridPx, gridPx);
        } else if (winter > 0.45) {
            // Crisp frosty corner crystal glints
            g.setComposite(AlphaComposite.SrcOver.derive((float) (0.28 * winter)));
            g.setColor(new Color(0xA5F3FC));
            g.setStroke(WEATHER_STROKE_THIN);
            int arm = Math.max(8, s / 3);
            g.drawLine(layout.gridX - 1, layout.gridY - 1, layout.gridX + arm, layout.gridY - 1);
            g.drawLine(layout.gridX - 1, layout.gridY - 1, layout.gridX - 1, layout.gridY + arm);
            g.drawLine(layout.gridX + gridPx + 1, layout.gridY + gridPx + 1, layout.gridX + gridPx - arm, layout.gridY + gridPx + 1);
            g.drawLine(layout.gridX + gridPx + 1, layout.gridY + gridPx + 1, layout.gridX + gridPx + 1, layout.gridY + gridPx - arm);
        } else if (summer > 0.45 && storm < 0.15) {
            // Clear sunny sea caustics glinting gently across 3 cells
            g.setColor(new Color(0xFEF08A));
            for (int i = 0; i < 3; i++) {
                double pulse = Math.max(0.0, Math.sin(t * 2.1 + i * 2.1));
                if (pulse <= 0.3) continue;
                int cr = (i * 3 + (showShips ? 1 : 6)) % Board.SIZE;
                int cc = (i * 4 + (showShips ? 2 : 5)) % Board.SIZE;
                int gx = layout.gridX + cc * s + s / 2;
                int gy = layout.gridY + cr * s + s / 2;
                g.setComposite(AlphaComposite.SrcOver.derive((float) (pulse * 0.24 * summer)));
                g.drawLine(gx - 5, gy, gx + 5, gy);
                g.drawLine(gx, gy - 5, gx, gy + 5);
            }
        }

        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private void drawGhostShipPreview(Graphics2D g, Layout layout) {
        if (previewRow < 0 || previewCol < 0 || previewLength <= 0) return;
        BufferedImage sprite = previewShipType != null
                ? SpriteAssets.getShipSprite(previewShipType, previewHorizontal)
                : SpriteAssets.getShipSpriteForLength(previewLength, previewHorizontal);
        if (sprite == null) return;
        int endRow = previewRow + (previewHorizontal ? 0 : previewLength - 1);
        int endCol = previewCol + (previewHorizontal ? previewLength - 1 : 0);
        if (endRow >= Board.SIZE || endCol >= Board.SIZE) return;
        int s = layout.cellSize;
        int sx = layout.gridX + previewCol * s;
        int sy = layout.gridY + previewRow * s;
        int sw = previewHorizontal ? previewLength * s : s;
        int sh = previewHorizontal ? s : previewLength * s;
        Composite prev = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(previewValid ? 0.65f : 0.35f));
        SpriteAssets.drawSharp(g, sprite, sx + 2, sy + 2, sw - 4, sh - 4);
        g.setComposite(prev);
    }

    private List<ShipSpan> resolveShipSpans(boolean[][] covered) {
        List<ShipSpan> spans = new ArrayList<>();
        if (board == null && cellStateOverride == null) return spans;

        if (!knownShips.isEmpty()) {
            for (Ship ship : knownShips) {
                if (ship == null || ship.type == null) continue;
                boolean hasSprite = SpriteAssets.getShipSprite(ship.type, ship.horizontal) != null;
                if (!hasSprite) continue;
                spans.add(new ShipSpan(ship.type, ship.row, ship.col, ship.type.length, ship.horizontal));
                for (int i = 0; i < ship.type.length; i++) {
                    int r = ship.row + (ship.horizontal ? 0 : i);
                    int c = ship.col + (ship.horizontal ? i : 0);
                    if (r >= 0 && r < Board.SIZE && c >= 0 && c < Board.SIZE) covered[r][c] = true;
                }
            }
            return spans;
        }

        // Auto-detect contiguous horizontal/vertical SHIP runs (supports placeRandomly() without changing Board)
        boolean[][] visited = new boolean[Board.SIZE][Board.SIZE];
        int threeCount = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (visited[r][c] || cellStateAt(r, c) != CellState.SHIP) continue;
                int hLen = 1;
                while (c + hLen < Board.SIZE && !visited[r][c + hLen] && cellStateAt(r, c + hLen) == CellState.SHIP) {
                    hLen++;
                }
                int vLen = 1;
                while (r + vLen < Board.SIZE && !visited[r + vLen][c] && cellStateAt(r + vLen, c) == CellState.SHIP) {
                    vLen++;
                }
                if (hLen >= 2 && hLen >= vLen && hLen <= 5) {
                    ShipType inferred = inferShipType(hLen, threeCount);
                    if (hLen == 3) threeCount++;
                    spans.add(new ShipSpan(inferred, r, c, hLen, true));
                    for (int i = 0; i < hLen; i++) {
                        visited[r][c + i] = true;
                        covered[r][c + i] = true;
                    }
                } else if (vLen >= 2 && vLen <= 5) {
                    ShipType inferred = inferShipType(vLen, threeCount);
                    if (vLen == 3) threeCount++;
                    spans.add(new ShipSpan(inferred, r, c, vLen, false));
                    for (int i = 0; i < vLen; i++) {
                        visited[r + i][c] = true;
                        covered[r + i][c] = true;
                    }
                } else {
                    visited[r][c] = true;
                }
            }
        }
        return spans;
    }

    private static ShipType inferShipType(int len, int threeIndex) {
        return switch (len) {
            case 5 -> ShipType.CARRIER;
            case 4 -> ShipType.BATTLESHIP;
            case 3 -> threeIndex == 0 ? ShipType.CRUISER : ShipType.SUBMARINE;
            case 2 -> ShipType.DESTROYER;
            default -> null;
        };
    }

    private int[] computeShipOffset(ShipSpan span, long now) {
        double dx = 0.0;
        double dy = 0.0;
        for (ShipFireAnim anim : shipFires) {
            if (!anim.matches(span)) continue;
            double p = Math.max(0.0, Math.min(1.0, (now - anim.startMs) / SHIP_FIRE_DURATION_MS));
            double punch = Math.sin(Math.min(1.0, p * 2.2) * Math.PI * 0.5) * Math.pow(1.0 - p, 1.35);
            double maxKick = switch (span.length) {
                case 5 -> 7.5;
                case 4 -> 6.5;
                case 3 -> 4.8;
                default -> 3.6;
            };
            double kick = punch * maxKick;
            dx -= Math.cos(anim.aimAngleRad) * kick;
            dy -= Math.sin(anim.aimAngleRad) * kick;
        }
        for (ShipHitAnim anim : shipHits) {
            if (!spanContains(span, anim.row, anim.col)) continue;
            double p = Math.max(0.0, Math.min(1.0, (now - anim.startMs) / SHIP_HIT_DURATION_MS));
            double vib = Math.sin(p * Math.PI * 7.5) * (1.0 - p) * 4.5;
            if (span.horizontal) dy += vib;
            else dx += vib;
            dx += Math.cos(p * Math.PI * 5.0) * (1.0 - p) * 1.8;
        }
        return new int[]{(int) Math.round(dx), (int) Math.round(dy)};
    }

    private void drawShipImpactFlashAndLaunchDeck(Graphics2D g, ShipSpan span, int sx, int sy, int sw, int sh, int s, long now) {
        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();

        // Ship hit hull flash & glowing structural shock contour
        for (ShipHitAnim anim : shipHits) {
            if (!spanContains(span, anim.row, anim.col)) continue;
            double p = Math.max(0.0, Math.min(1.0, (now - anim.startMs) / SHIP_HIT_DURATION_MS));
            float a = (float) Math.max(0.0, (1.0 - p) * 0.68 * (0.55 + 0.45 * Math.sin(p * Math.PI * 5.0)));
            g.setComposite(alphaComp(a));
            g.setColor(anim.accent);
            g.fillRoundRect(sx + 2, sy + 2, sw - 4, sh - 4, s / 3, s / 3);
            g.setColor(Color.WHITE);
            g.setStroke(roundStroke(2.2f));
            g.drawRoundRect(sx + 2, sy + 2, sw - 4, sh - 4, s / 3, s / 3);
            break;
        }

        // Ship-class specific hull firing visual aura
        for (ShipFireAnim anim : shipFires) {
            if (!anim.matches(span)) continue;
            double p = Math.max(0.0, Math.min(1.0, (now - anim.startMs) / SHIP_FIRE_DURATION_MS));
            float fade = (float) Math.max(0.0, 1.0 - p);
            Color glow = anim.overrideColor != null
                    ? anim.overrideColor
                    : (anim.magic != CombatLoadout.MagicOption.NONE ? anim.magic.accentColor : UiTokens.ACCENT_SIGNAL);

            if (span.length >= 5) {
                // Carrier (5): illuminated catapult launch strips + hot core glow streaking down flight deck
                g.setComposite(alphaComp(fade * 0.88f));
                g.setColor(glow);
                g.setStroke(roundStroke(2.6f));
                double lead = Math.min(1.0, p * 1.35);
                if (span.horizontal) {
                    int cy = sy + sh / 2;
                    int xEnd = sx + (int) (sw * lead);
                    g.drawLine(sx + 6, cy - s / 7, xEnd, cy - s / 7);
                    g.drawLine(sx + 6, cy + s / 7, xEnd, cy + s / 7);
                    drawGlow(g, Color.WHITE, xEnd, cy, s * 0.48, fade * 0.72f, false);
                } else {
                    int cx = sx + sw / 2;
                    int yEnd = sy + (int) (sh * lead);
                    g.drawLine(cx - s / 7, sy + 6, cx - s / 7, yEnd);
                    g.drawLine(cx + s / 7, sy + 6, cx + s / 7, yEnd);
                    drawGlow(g, Color.WHITE, cx, yEnd, s * 0.48, fade * 0.72f, false);
                }
            } else if (anim.weapon == CombatLoadout.WeaponOption.TORPEDO || span.type == ShipType.SUBMARINE) {
                // Submarine (3): twin pulsing voxel sonar cavitation barriers around submerged hull
                Color subCol = anim.magic != CombatLoadout.MagicOption.NONE ? glow : new Color(0x67E8F9);
                g.setComposite(alphaComp(fade * 0.85f));
                g.setColor(subCol);
                int expand = (int) (p * s * 0.56);
                SpriteAssets.drawVoxelChamferBox(g, sx - expand / 2, sy - expand / 2, sw + expand, sh + expand, Math.max(4, s / 4), 2);
                g.setComposite(alphaComp(fade * 0.45f));
                g.setColor(Color.WHITE);
                int expand2 = (int) (p * s * 0.86);
                SpriteAssets.drawVoxelChamferBox(g, sx - expand2 / 2, sy - expand2 / 2, sw + expand2, sh + expand2, Math.max(4, s / 3), 2);
            } else if (span.length == 4) {
                // Battleship (4): heavy broadside voxel pressure shock-rings + turret glow
                int cx = sx + sw / 2;
                int cy = sy + sh / 2;
                drawGlow(g, glow, cx, cy, s * (0.65 + p * 0.45), fade * 0.36f, true);
                int rad = (int) (s * (0.45 + p * 1.55));
                g.setComposite(alphaComp(fade * 0.86f));
                g.setColor(glow);
                SpriteAssets.drawVoxelRing(g, cx, cy, rad, Math.max(2, (int) Math.round(3.2 * fade)), false);
            } else {
                // Cruiser (3) / Destroyer (2): crisp voxel deck charge ring
                int cx = sx + sw / 2;
                int cy = sy + sh / 2;
                int rad = (int) (s * (0.35 + p * 1.05));
                g.setComposite(alphaComp(fade * 0.80f));
                g.setColor(glow);
                SpriteAssets.drawVoxelRing(g, cx, cy, rad, Math.max(2, (int) Math.round(2.4 * fade)), false);
            }
        }

        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private void drawImpactZoneFloors(Graphics2D g, Layout layout, long now) {
        int s = layout.cellSize;
        double tSec = (now % 600_000L) / 1000.0;
        Composite prevComp = g.getComposite();
        int runeSz = Math.max(24, (int) Math.round(s * 2.55));
        int fxSz = Math.max(28, (int) Math.round(s * 2.65));
        int frameCount = Math.max(1, SpriteAssets.fxFrameCount());

        for (ImpactZone z : impactZones) {
            float fadeIn = (float) Math.min(1.0, Math.max(0.0, (now - z.startMs) / 360.0));
            // 1. Per-cell animated elemental shimmer & scorched/ionized wash
            for (int[] cell : z.cells) {
                int r = cell[0];
                int c = cell[1];
                int x = layout.gridX + c * s;
                int y = layout.gridY + r * s;
                double wave = 0.5 + 0.5 * Math.sin(tSec * 2.7 + r * 0.75 + c * 0.95);
                boolean center = (r == z.centerRow && c == z.centerCol);
                float cellAlpha = fadeIn * (float) ((center ? 0.24 : 0.12) + wave * 0.11);
                g.setComposite(alphaComp(cellAlpha));
                g.setColor(z.accent);
                g.fillRect(x + 1, y + 1, s - 2, s - 2);
            }

            int cx = layout.gridX + z.centerCol * s + s / 2;
            int cy = layout.gridY + z.centerRow * s + s / 2;

            // Soft central nexus caldera/arcane wash (cached stamp, zero allocation)
            float glowPulse = fadeIn * (float) (0.34 + 0.10 * Math.sin(tSec * 2.6 + z.centerCol));
            drawGlow(g, z.accent, cx, cy, s * 1.65, glowPulse, true);

            // Rotating radar sweep wedge inside ARCANE_SONAR zones
            if (z.magic == CombatLoadout.MagicOption.ARCANE_SONAR) {
                int sweepR = (int) Math.round(s * 1.38);
                int deg = (int) ((tSec * 145.0) % 360.0);
                g.setComposite(alphaComp(fadeIn * 0.22f));
                g.setColor(UiTokens.MAGIC_SONAR);
                g.fillArc(cx - sweepR, cy - sweepR, sweepR * 2, sweepR * 2, deg, 44);
            }

            // 2. Continuously rotating Blender 3D Magic Rune centered on the 3x3 zone
            CombatLoadout.MagicOption runeKey = z.magic != CombatLoadout.MagicOption.NONE
                    ? z.magic
                    : CombatLoadout.MagicOption.FIRE;
            BufferedImage runeImg = SpriteAssets.getMagicRune(runeKey);
            if (runeImg != null) {
                AffineTransform prevTx = g.getTransform();
                double angle = tSec * (z.zombieBoss ? -0.52 : 0.45) + (z.centerRow + z.centerCol) * 0.4;
                g.setComposite(alphaComp(fadeIn * 0.42f));
                g.translate(cx, cy);
                g.rotate(angle);
                SpriteAssets.drawSharp(g, runeImg, -runeSz / 2, -runeSz / 2, runeSz, runeSz);
                g.setTransform(prevTx);
            }

            // 3. Smoldering Blender 3D FX loop continuously animating across the crater
            int loopFrame = (int) ((now / 95L + z.centerRow * 3L + z.centerCol) % frameCount);
            BufferedImage fxImg = SpriteAssets.getFxFrame(z.fxType, loopFrame);
            if (fxImg != null) {
                double swell = 0.34 + 0.11 * Math.sin(tSec * 3.1 + z.centerRow);
                g.setComposite(alphaComp(fadeIn * (float) swell));
                SpriteAssets.drawSharp(g, fxImg, cx - fxSz / 2, cy - fxSz / 2, fxSz, fxSz);
            }
        }
        g.setComposite(prevComp);
    }

    private void drawImpactZonePerimeters(Graphics2D g, Layout layout, long now) {
        int s = layout.cellSize;
        double tSec = (now % 600_000L) / 1000.0;
        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();

        for (ImpactZone z : impactZones) {
            float fadeIn = (float) Math.min(1.0, Math.max(0.0, (now - z.startMs) / 360.0));
            int x0 = layout.gridX + z.minCol * s + 2;
            int y0 = layout.gridY + z.minRow * s + 2;
            int wBox = (z.maxCol - z.minCol + 1) * s - 4;
            int hBox = (z.maxRow - z.minRow + 1) * s - 4;
            int cx = x0 + wBox / 2;
            int cy = y0 + hBox / 2;

            // Soft perimeter glow + animated marching energy frame (cached quantized strokes)
            float pulse = (float) (0.72 + 0.24 * Math.sin(tSec * 3.4));
            g.setComposite(alphaComp(fadeIn * 0.22f * pulse));
            g.setColor(z.accent);
            g.setStroke(roundStroke(4.5f));
            g.drawRoundRect(x0, y0, wBox, hBox, 8, 8);

            int dashIdx = Math.floorMod((int) (tSec * 28.0), MARCH_STROKES.length);
            g.setComposite(alphaComp(fadeIn * 0.84f * pulse));
            g.setStroke(MARCH_STROKES[dashIdx]);
            g.drawRoundRect(x0, y0, wBox, hBox, 8, 8);

            // Corner target brackets
            int arm = Math.max(8, s / 3);
            g.setStroke(roundStroke(2.2f));
            g.setColor(Color.WHITE);
            g.setComposite(alphaComp(fadeIn * 0.62f));
            g.drawLine(x0, y0, x0 + arm, y0);
            g.drawLine(x0, y0, x0, y0 + arm);
            g.drawLine(x0 + wBox - arm, y0, x0 + wBox, y0);
            g.drawLine(x0 + wBox, y0, x0 + wBox, y0 + arm);
            g.drawLine(x0, y0 + hBox, x0 + arm, y0 + hBox);
            g.drawLine(x0, y0 + hBox - arm, x0, y0 + hBox);
            g.drawLine(x0 + wBox - arm, y0 + hBox, x0 + wBox, y0 + hBox);
            g.drawLine(x0 + wBox, y0 + hBox - arm, x0 + wBox, y0 + hBox);

            // Elemental looping voxel particles anchored inside the 3x3 zone
            if (z.magic == CombatLoadout.MagicOption.ARCANE_SONAR) {
                for (int k = 0; k < 2; k++) {
                    double pr = (tSec * 0.65 + k * 0.5) % 1.0;
                    int rr = (int) (Math.min(wBox, hBox) * 0.48 * pr);
                    g.setComposite(alphaComp(fadeIn * (float) ((1.0 - pr) * 0.78)));
                    g.setColor(UiTokens.MAGIC_SONAR);
                    SpriteAssets.drawVoxelRing(g, cx, cy, rr, 2, false);
                }
            } else if (z.magic == CombatLoadout.MagicOption.LIGHTNING) {
                g.setStroke(roundStroke(1.8f));
                for (int k = 0; k < 4; k++) {
                    double ang = tSec * 2.1 + k * Math.PI * 0.5;
                    double j = Math.sin(tSec * 19.0 + k * 2.7) * 0.28;
                    int mx = cx + (int) (Math.cos(ang + j) * wBox * 0.22);
                    int my = cy + (int) (Math.sin(ang + j) * hBox * 0.22);
                    int ex = cx + (int) (Math.cos(ang) * wBox * 0.43);
                    int ey = cy + (int) (Math.sin(ang) * hBox * 0.43);
                    g.setComposite(alphaComp(fadeIn * 0.78f));
                    g.setColor(k % 2 == 0 ? new Color(0xE0F2FE) : z.accent);
                    g.drawLine(cx, cy, mx, my);
                    g.drawLine(mx, my, ex, ey);
                    SpriteAssets.drawVoxelCube(g, ex, ey, 4, Color.WHITE, z.accent);
                }
            } else if (z.magic == CombatLoadout.MagicOption.FROST) {
                for (int k = 0; k < 6; k++) {
                    double ang = tSec * 0.9 + k * (Math.PI / 3.0);
                    double rad = Math.min(wBox, hBox) * (0.24 + 0.16 * Math.sin(tSec * 1.8 + k));
                    int px = cx + (int) (Math.cos(ang) * rad);
                    int py = cy + (int) (Math.sin(ang) * rad);
                    g.setComposite(alphaComp(fadeIn * 0.86f));
                    g.setColor(k % 2 == 0 ? Color.WHITE : UiTokens.MAGIC_FROST);
                    if ((k & 1) == 0) {
                        g.fillRect(px - 4, py - 1, 9, 2);
                        g.fillRect(px - 1, py - 4, 2, 9);
                    } else {
                        g.fillRect(px - 1, py - 1, 3, 3);
                        g.fillRect(px - 4, py - 4, 2, 2);
                        g.fillRect(px + 3, py - 4, 2, 2);
                        g.fillRect(px - 4, py + 3, 2, 2);
                        g.fillRect(px + 3, py + 3, 2, 2);
                    }
                }
            } else {
                // Rising glowing voxel embers with subtle halos for FIRE, AIRSTRIKE burn & ZOMBIE Abyss
                for (int k = 0; k < 8; k++) {
                    double cycle = (tSec * 0.55 + k * 0.125) % 1.0;
                    int px = x0 + (int) ((wBox * (0.12 + ((k * 37) % 76) / 100.0)) + Math.sin(tSec * 3.0 + k) * 7.0);
                    int py = y0 + hBox - (int) (cycle * hBox * 0.90);
                    float a = fadeIn * (float) (Math.sin(cycle * Math.PI) * 0.90);
                    drawGlow(g, z.accent, px, py, 8, a * 0.50f, false);
                    g.setComposite(alphaComp(a));
                    Color eCol = k % 2 == 0 ? new Color(0xFEF08A) : z.accent;
                    int sz = (k % 3 == 0) ? 5 : 4;
                    SpriteAssets.drawVoxelCube(g, px, py, sz, eCol, Color.WHITE);
                }
            }
        }
        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private BufferedImage ensureFullShieldHexCache(int gridPx, int cellSize) {
        if (fullShieldHexCache != null && shieldCacheGridSize == gridPx) {
            return fullShieldHexCache;
        }
        shieldCacheGridSize = gridPx;
        BufferedImage img = new BufferedImage(gridPx, gridPx, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // Subtle sapphire-aqua arcane dome fill gradient
        g2.setPaint(new GradientPaint(
                0, 0, new Color(56, 189, 248, 22),
                gridPx, gridPx, new Color(14, 165, 233, 36)));
        g2.fillRect(0, 0, gridPx, gridPx);

        // Crisp tiled hexagonal honeycomb lattice across all 10x10 cells
        double hexR = Math.max(12.0, cellSize * 0.56);
        double dx = hexR * 1.5;
        double dy = hexR * Math.sqrt(3.0);
        g2.setStroke(new BasicStroke(1.0f));
        int cols = (int) Math.ceil(gridPx / dx) + 2;
        int rows = (int) Math.ceil(gridPx / dy) + 2;
        for (int c = 0; c < cols; c++) {
            double cx = c * dx;
            for (int r = 0; r < rows; r++) {
                double cy = r * dy + ((c & 1) == 1 ? dy * 0.5 : 0.0);
                int[] hx = new int[6];
                int[] hy = new int[6];
                for (int k = 0; k < 6; k++) {
                    double a = k * (Math.PI / 3.0);
                    hx[k] = (int) Math.round(cx + Math.cos(a) * (hexR - 1.5));
                    hy[k] = (int) Math.round(cy + Math.sin(a) * (hexR - 1.5));
                }
                g2.setColor(new Color(56, 189, 248, 56));
                g2.drawPolygon(hx, hy, 6);
                g2.setColor(new Color(224, 242, 254, 68));
                g2.fillRect((int) Math.round(cx) - 1, (int) Math.round(cy) - 1, 2, 2);
            }
        }
        g2.dispose();
        fullShieldHexCache = img;
        return img;
    }

    private void drawMagicShieldOverlay(Graphics2D g, Layout layout, List<ShipSpan> shipSpans, long now) {
        int s = layout.cellSize;
        double tSec = (now % 600_000L) / 1000.0;
        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();

        BufferedImage shieldImg = SpriteAssets.getMagicShield();
        BufferedImage rune = SpriteAssets.getMagicRune(CombatLoadout.MagicOption.ARCANE_SONAR);
        BufferedImage runeFrost = SpriteAssets.getMagicRune(CombatLoadout.MagicOption.FROST);
        boolean activeShield = fullMapShield || !shieldedCells.isEmpty();
        int curEnergy = Math.max(0, Math.min(SHIELD_MAX_ENERGY, shieldEnergy));
        double energyRatio = activeShield ? (curEnergy > 0 ? curEnergy / (double) SHIELD_MAX_ENERGY : 1.0) : 0.0;
        Color domePrimary = energyRatio > 0.60 ? new Color(0x38BDF8)
                : (energyRatio > 0.30 ? new Color(0xFBBF24) : new Color(0xF87171));
        Color domeRim = energyRatio > 0.60 ? new Color(0xBAE6FD)
                : (energyRatio > 0.30 ? new Color(0xFDE68A) : new Color(0xFECACA));
        Color crackHot = energyRatio <= 0.30 ? new Color(0xEF4444) : new Color(0xF59E0B);

        // 0. Full-Map Arcane Crystalline Barrier Dome (covers all 10x10 ocean map cleanly)
        if (fullMapShield) {
            int gx = layout.gridX;
            int gy = layout.gridY;
            int gw = Board.SIZE * s;
            int gh = Board.SIZE * s;
            double riseT = Math.max(0.0, Math.min(1.0, (now - fullMapShieldStartMs) / FULL_SHIELD_RISE_MS));
            double ease = 1.0 - Math.pow(1.0 - riseT, 2.2);
            int curTop = gy + (int) Math.round(gh * (1.0 - ease));
            int curH = Math.max(1, gy + gh - curTop);

            Shape oldClip = g.getClip();
            g.clipRect(gx - 6, curTop - 6, gw + 12, curH + 12);

            BufferedImage hexDome = ensureFullShieldHexCache(gw, s);
            float flicker = energyRatio <= 0.30 ? (float) (0.72 + 0.26 * Math.sin(tSec * 14.0))
                    : (float) (0.76 + 0.18 * Math.sin(tSec * 2.4));
            g.setComposite(alphaComp(flicker));
            g.drawImage(hexDome, gx, gy, null);

            if (energyRatio <= 0.60) {
                g.setComposite(alphaComp(energyRatio <= 0.30 ? 0.16f : 0.10f));
                g.setColor(domePrimary);
                g.fillRect(gx, curTop, gw, curH);
            }

            // Travelling diagonal luminous energy wave sweeping across full-map barrier
            double wavePos = ((tSec * 0.28) % 1.4) - 0.2;
            int waveX = gx + (int) Math.round(wavePos * gw);
            g.setComposite(alphaComp(0.16f));
            g.setPaint(new GradientPaint(
                    waveX - s * 2, gy, new Color(domePrimary.getRed(), domePrimary.getGreen(), domePrimary.getBlue(), 0),
                    waveX, gy + gh / 2, new Color(domeRim.getRed(), domeRim.getGreen(), domeRim.getBlue(), 175), true));
            g.fillRect(gx, curTop, gw, curH);

            // Outer chamfered Forcefield Frame + Corner Arcane Emitter Pylons
            int ch = Math.max(10, s / 2);
            drawGlow(g, domePrimary, gx + gw * 0.5, gy + gh * 0.5, gw * 0.62, 0.17f, true);
            g.setComposite(alphaComp(0.46f + (float) (0.14 * Math.sin(tSec * 3.0))));
            g.setColor(domePrimary);
            SpriteAssets.drawVoxelChamferBox(g, gx - 3, gy - 3, gw + 6, gh + 6, ch, 4);
            g.setComposite(alphaComp(0.92f));
            g.setColor(domeRim);
            SpriteAssets.drawVoxelChamferBox(g, gx - 1, gy - 1, gw + 2, gh + 2, ch, 2);

            g.setClip(oldClip);

            // Rising laser curtain frontier while full-map dome erects upward
            if (riseT < 0.995) {
                g.setComposite(alphaComp(0.88f));
                g.setColor(new Color(0x38BDF8));
                g.setStroke(roundStroke(4.0f));
                g.drawLine(gx, curTop, gx + gw, curTop);
                g.setColor(Color.WHITE);
                g.setStroke(roundStroke(1.6f));
                g.drawLine(gx, curTop, gx + gw, curTop);
                for (int k = 0; k <= Board.SIZE; k += 2) {
                    int nx = gx + Math.min(gw, k * s);
                    SpriteAssets.drawVoxelCube(g, nx, curTop, 5, Color.WHITE, new Color(0x22D3EE));
                }
            }

            // 4 Corner Arcane Pylon Seals anchoring the full-map dome
            int[][] corners = {{gx + 8, gy + 8}, {gx + gw - 8, gy + 8}, {gx + 8, gy + gh - 8}, {gx + gw - 8, gy + gh - 8}};
            for (int ci = 0; ci < corners.length; ci++) {
                int cx = corners[ci][0];
                int cy = corners[ci][1];
                if (rune != null && ease > 0.25) {
                    AffineTransform prevTx = g.getTransform();
                    g.setComposite(alphaComp(0.68f));
                    g.translate(cx, cy);
                    g.rotate(tSec * (ci % 2 == 0 ? 1.4 : -1.4));
                    int rSz = Math.max(22, (int) Math.round(s * 0.95));
                    SpriteAssets.drawSharp(g, rune, -rSz / 2, -rSz / 2, rSz, rSz);
                    g.setTransform(prevTx);
                }
                if (shieldImg != null && ease > 0.4) {
                    g.setComposite(alphaComp(0.92f));
                    SpriteAssets.drawSharp(g, shieldImg, cx - 10, cy - 10, 20, 20);
                }
                SpriteAssets.drawVoxelCube(g, cx, cy, 4, Color.WHITE, new Color(0x38BDF8));
            }
        }

        // 1. Living Allied Ships: Spinning Magic Circle Seals + Cohesive Sapphire Aegis Dome
        int shipIndex = 0;
        for (ShipSpan span : shipSpans) {
            if (!isSpanAlive(span)) continue;
            boolean hasShield = fullMapShield;
            if (!hasShield) {
                for (int i = 0; i < span.length; i++) {
                    int r = span.row + (span.horizontal ? 0 : i);
                    int c = span.col + (span.horizontal ? i : 0);
                    if (containsCell(shieldedCells, r, c)) {
                        hasShield = true;
                        break;
                    }
                }
            }
            if (!hasShield) continue;
            int sx = layout.gridX + span.col * s;
            int sy = layout.gridY + span.row * s;
            int sw = span.horizontal ? span.length * s : s;
            int sh = span.horizontal ? s : span.length * s;
            int scx = sx + sw / 2;
            int scy = sy + sh / 2;

            // Spinning Arcane Magic Circle under/around each of the 5 allied ships
            BufferedImage activeRune = (shipIndex % 2 == 0 || runeFrost == null) ? rune : runeFrost;
            if (activeRune != null) {
                int sealSz = (int) Math.round(s * (1.85 + 0.14 * span.length + 0.06 * Math.sin(tSec * 2.5 + shipIndex)));
                AffineTransform prevTx = g.getTransform();
                g.setComposite(alphaComp(fullMapShield ? 0.72f : 0.44f));
                g.translate(scx, scy);
                g.rotate(tSec * ((shipIndex % 2 == 0) ? 0.85 : -0.85) + shipIndex * 0.6);
                SpriteAssets.drawSharp(g, activeRune, -sealSz / 2, -sealSz / 2, sealSz, sealSz);
                g.setTransform(prevTx);
            }
            if (fullMapShield) {
                g.setComposite(alphaComp(0.62f));
                g.setColor(new Color(0xBAE6FD));
                SpriteAssets.drawVoxelRing(g, scx, scy, s * (0.95 + 0.10 * span.length), 2, true);
            }

            int pad = Math.max(5, s / 5);
            int bx = sx - pad;
            int by = sy - pad;
            int bw = sw + pad * 2;
            int bh = sh + pad * 2;
            int chamfer = Math.max(6, s / 3);
            float pulse = (float) (fullMapShield ? 0.13 + 0.06 * Math.sin(tSec * 3.0 + shipIndex) : 0.16 + 0.08 * Math.sin(tSec * 3.0));
            g.setComposite(alphaComp(pulse));
            g.setColor(new Color(0x38BDF8));
            SpriteAssets.fillChamferedBox(g, bx, by, bw, bh, chamfer);
            if (shieldImg != null && !fullMapShield) {
                g.setComposite(alphaComp(0.42f + (float) (0.09 * Math.sin(tSec * 2.5))));
                SpriteAssets.drawSharp(g, shieldImg, bx, by, bw, bh);
            }
            double waveFrac = (tSec * 0.45 + shipIndex * 0.18) % 1.0;
            double shimmerX = bx + waveFrac * bw;
            double shimmerY = sy + sh * 0.5;
            drawGlow(g, new Color(0xE0F2FE), shimmerX, shimmerY, s * 0.65, 0.30f, true);

            g.setComposite(alphaComp(0.84f));
            g.setColor(new Color(0xBAE6FD));
            SpriteAssets.drawVoxelChamferBox(g, bx, by, bw, bh, chamfer, 2);
            SpriteAssets.drawVoxelCube(g, bx + chamfer, by, 4, Color.WHITE, new Color(0x38BDF8));
            SpriteAssets.drawVoxelCube(g, bx + bw - chamfer, by, 4, Color.WHITE, new Color(0x38BDF8));
            SpriteAssets.drawVoxelCube(g, bx + chamfer, by + bh, 4, Color.WHITE, new Color(0x38BDF8));
            SpriteAssets.drawVoxelCube(g, bx + bw - chamfer, by + bh, 4, Color.WHITE, new Color(0x38BDF8));
            shipIndex++;
        }

        // 2. Per-cell emitter hex nodes when using manual cell shields in Sandbox (kept clean when fullMapShield is active)
        if (!fullMapShield) {
            for (int i = 0; i < shieldedCells.size(); i++) {
                int[] cell = shieldedCells.get(i);
                int cx = layout.gridX + cell[1] * s + s / 2;
                int cy = layout.gridY + cell[0] * s + s / 2;
                double phase = tSec * 2.8 + i * 0.45;
                double breathe = 0.94 + 0.07 * Math.sin(phase);

                if (rune != null) {
                    int runeSz = (int) Math.round(s * 0.92 * breathe);
                    AffineTransform prevTx = g.getTransform();
                    g.setComposite(alphaComp(0.30f));
                    g.translate(cx, cy);
                    g.rotate(tSec * 0.9 + i * 0.3);
                    SpriteAssets.drawSharp(g, rune, -runeSz / 2, -runeSz / 2, runeSz, runeSz);
                    g.setTransform(prevTx);
                }

                if (shieldImg != null) {
                    int shSz = (int) Math.round(s * 0.80 * breathe);
                    g.setComposite(alphaComp(0.58f));
                    SpriteAssets.drawSharp(g, shieldImg, cx - shSz / 2, cy - shSz / 2, shSz, shSz);
                } else {
                    drawShieldedCell(g, layout.gridX + cell[1] * s, layout.gridY + cell[0] * s, s);
                }
            }
        }

        // 3. Persistent Stress Fracture Cracks across the Magic Shield Dome when absorbing strikes
        if (activeShield && !shieldCracks.isEmpty()) {
            Shape prevCrackClip = g.getClip();
            g.clipRect(layout.gridX - 2, layout.gridY - 2, Board.SIZE * s + 4, Board.SIZE * s + 4);
            for (int ci = 0; ci < shieldCracks.size(); ci++) {
                ShieldCrack sc = shieldCracks.get(ci);
                int ox = layout.gridX + sc.col * s + s / 2;
                int oy = layout.gridY + sc.row * s + s / 2;
                float agePulse = (float) (0.82 + 0.18 * Math.sin(tSec * 7.5 + ci * 1.3));
                drawGlow(g, crackHot, ox, oy, s * (0.75 + sc.severity * 0.22), 0.34f * agePulse, false);

                double reach = s * (0.68 + sc.severity * 0.34 + (1.0 - energyRatio) * 0.55);
                for (int b = 0; b < sc.angles.length; b++) {
                    double ang = sc.angles[b];
                    double len1 = reach * sc.lengths[b];
                    int x1 = ox + (int) Math.round(Math.cos(ang) * (len1 * 0.52) + Math.cos(ang + 1.57) * (sc.jags[b] * s * 0.16));
                    int y1 = oy + (int) Math.round(Math.sin(ang) * (len1 * 0.52) + Math.sin(ang + 1.57) * (sc.jags[b] * s * 0.16));
                    int x2 = ox + (int) Math.round(Math.cos(ang) * len1);
                    int y2 = oy + (int) Math.round(Math.sin(ang) * len1);

                    // Dark outer crystal fracture groove + glowing fault core
                    g.setComposite(alphaComp(0.68f * agePulse));
                    g.setColor(new Color(8, 14, 28));
                    g.setStroke(roundStroke(4.2f));
                    g.drawLine(ox, oy, x1, y1);
                    g.drawLine(x1, y1, x2, y2);

                    g.setComposite(alphaComp(0.88f * agePulse));
                    g.setColor(crackHot);
                    g.setStroke(roundStroke(2.2f));
                    g.drawLine(ox, oy, x1, y1);
                    g.drawLine(x1, y1, x2, y2);

                    g.setComposite(alphaComp(0.95f * agePulse));
                    g.setColor(Color.WHITE);
                    g.setStroke(roundStroke(1.05f));
                    g.drawLine(ox, oy, x1, y1);

                    // Forking micro-splitter off elbow vertex
                    double subAng = ang + ((b % 2 == 0) ? 0.55 : -0.55);
                    int sx2 = x1 + (int) Math.round(Math.cos(subAng) * (s * (0.34 + sc.severity * 0.12)));
                    int sy2 = y1 + (int) Math.round(Math.sin(subAng) * (s * (0.34 + sc.severity * 0.12)));
                    g.setComposite(alphaComp(0.76f * agePulse));
                    g.setColor(sc.accent);
                    g.setStroke(roundStroke(1.5f));
                    g.drawLine(x1, y1, sx2, sy2);
                }

                SpriteAssets.drawVoxelCube(g, ox, oy, 4 + sc.severity, Color.WHITE, crackHot);
                if (ci > 0) {
                    ShieldCrack prev = shieldCracks.get(ci - 1);
                    int px = layout.gridX + prev.col * s + s / 2;
                    int py = layout.gridY + prev.row * s + s / 2;
                    if (Math.hypot(px - ox, py - oy) <= s * 4.2 || energyRatio <= 0.50) {
                        g.setComposite(alphaComp(0.50f * agePulse));
                        g.setColor(crackHot);
                        g.setStroke(roundStroke(1.3f));
                        int mx = (ox + px) / 2 + ((ci % 2 == 0) ? 7 : -7);
                        int my = (oy + py) / 2 - 5;
                        g.drawLine(ox, oy, mx, my);
                        g.drawLine(mx, my, px, py);
                    }
                }
            }
            g.setClip(prevCrackClip);
        }

        // 3.5 On-dome Shield Energy Bar Badge in top-right corner of grid
        if (activeShield && s >= 22) {
            int gw = Board.SIZE * s;
            g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 10));
            String eTxt = "KHIÊN " + (int) Math.round(energyRatio * 100.0) + "%";
            FontMetrics eFm = g.getFontMetrics();
            int txtW = eFm.stringWidth(eTxt);
            int badgeW = Math.min(gw - 16, Math.max(106, txtW + 58));
            int badgeH = 19;
            int bx = layout.gridX + gw - badgeW - 6;
            int by = layout.gridY + 5;
            g.setComposite(alphaComp(0.86f));
            g.setColor(new Color(8, 16, 30));
            SpriteAssets.fillChamferedBox(g, bx, by, badgeW, badgeH, 5);
            g.setColor(domePrimary);
            SpriteAssets.drawVoxelChamferBox(g, bx, by, badgeW, badgeH, 5, 1);

            g.setColor(domeRim);
            g.drawString(eTxt, bx + 6, by + 13);

            int barX = bx + 6 + txtW + 6;
            int barW = Math.max(20, (bx + badgeW - 6) - barX);
            int barY = by + 6;
            int barH = 7;
            g.setColor(new Color(15, 23, 42));
            g.fillRect(barX, barY, barW, barH);
            int fillW = (int) Math.round(barW * energyRatio);
            if (fillW > 0) {
                g.setColor(domePrimary);
                g.fillRect(barX, barY, fillW, barH);
            }
        }

        // 3.8 Brief shattered barrier border flash immediately after energy drops to 0
        if (shieldShatterUntilMs > now) {
            double sp = Math.max(0.0, Math.min(1.0, (shieldShatterUntilMs - now) / 560.0));
            int gx = layout.gridX;
            int gy = layout.gridY;
            int gw = Board.SIZE * s;
            int gh = Board.SIZE * s;
            g.setComposite(alphaComp((float) (sp * 0.85)));
            g.setColor(new Color(0xF87171));
            SpriteAssets.drawVoxelChamferBox(g, gx - 2, gy - 2, gw + 4, gh + 4, Math.max(8, s / 2), 3);
        }

        // 4. Heavy Shield Deflection Burst when a projectile strikes the shield barrier
        for (ShieldBlockAnim blk : shieldBlocks) {
            double p = Math.max(0.0, Math.min(1.0, (now - blk.startMs) / SHIELD_BLOCK_DURATION_MS));
            double scaleI = 0.85 + blk.intensity * 0.34;
            int cx = layout.gridX + blk.col * s + s / 2;
            int cy = layout.gridY + blk.row * s + s / 2;
            float alpha = (float) Math.max(0.0, 1.0 - p);
            Color hitTint = blk.intensity >= 3 ? new Color(0xFB923C) : domePrimary;
            drawGlow(g, hitTint, cx, cy, s * (0.75 + p * 0.95) * scaleI, alpha * 0.72f, false);

            int rad = (int) Math.round(s * (0.48 + p * 1.65) * scaleI);
            g.setComposite(alphaComp(alpha * 0.94f));
            g.setColor(Color.WHITE);
            SpriteAssets.drawVoxelRing(g, cx, cy, rad, Math.max(2, (int) Math.round((3.8 + blk.intensity) * (1.0 - p))), false);
            g.setColor(hitTint);
            SpriteAssets.drawVoxelRing(g, cx, cy, Math.max(4, (int) (rad * 0.72)), 2, true);

            // Forceful radiating hex impact star lines
            g.setStroke(roundStroke((float) (2.2 * (1.0 - p * 0.5))));
            for (int r = 0; r < 6; r++) {
                double ang = p * 0.35 + r * (Math.PI / 3.0);
                int r0 = (int) Math.round(s * 0.18);
                int r1 = (int) Math.round(rad * 0.92);
                g.setColor(r % 2 == 0 ? Color.WHITE : hitTint);
                g.drawLine(cx + (int) Math.round(Math.cos(ang) * r0), cy + (int) Math.round(Math.sin(ang) * r0),
                        cx + (int) Math.round(Math.cos(ang) * r1), cy + (int) Math.round(Math.sin(ang) * r1));
            }

            g.setStroke(roundStroke(1.8f));
            double hexR = s * (0.40 + p * 1.15) * scaleI;
            int[] hx = new int[6];
            int[] hy = new int[6];
            for (int k = 0; k < 6; k++) {
                double a = p * 0.65 + k * (Math.PI / 3.0);
                hx[k] = cx + (int) Math.round(Math.cos(a) * hexR);
                hy[k] = cy + (int) Math.round(Math.sin(a) * hexR);
                SpriteAssets.drawVoxelCube(g, hx[k], hy[k], 4, Color.WHITE, hitTint);
            }
            g.setColor(domeRim);
            g.drawPolygon(hx, hy, 6);

            if (shieldImg != null) {
                int popSz = (int) Math.round(s * (1.35 + p * 0.80) * (0.9 + blk.intensity * 0.12));
                g.setComposite(alphaComp(alpha));
                SpriteAssets.drawSharp(g, shieldImg, cx - popSz / 2, cy - popSz / 2, popSz, popSz);
            }
        }

        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private void drawMuzzleBlasts(Graphics2D g, Layout layout, long now) {
        int s = layout.cellSize;
        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        for (ShipFireAnim anim : shipFires) {
            double p = Math.max(0.0, Math.min(1.0, (now - anim.startMs) / SHIP_FIRE_DURATION_MS));
            if (p >= 0.78) continue;
            double mProg = p / 0.78;
            int fIdx = Math.min(SpriteAssets.muzzleFrameCount() - 1,
                    (int) (mProg * SpriteAssets.muzzleFrameCount()));
            BufferedImage frame = SpriteAssets.getMuzzleFrame(fIdx);
            if (frame == null) continue;

            int cx = layout.gridX + anim.muzzleCol * s + s / 2;
            int cy = layout.gridY + anim.muzzleRow * s + s / 2;
            double tipX = cx + Math.cos(anim.aimAngleRad) * s * 0.34;
            double tipY = cy + Math.sin(anim.aimAngleRad) * s * 0.34;
            double scaleMul = switch (anim.length) {
                case 5 -> 2.15;
                case 4 -> 1.98;
                case 3 -> anim.weapon == CombatLoadout.WeaponOption.TORPEDO ? 1.28 : 1.62;
                default -> 1.38;
            };
            int size = (int) Math.round(s * scaleMul);
            float alpha = (float) Math.min(1.0, (1.0 - mProg) * 1.35);

            // Bright incandescent muzzle star core right at turret barrel (cached stamp, zero allocation)
            Color glow = anim.overrideColor != null
                    ? anim.overrideColor
                    : (anim.magic != CombatLoadout.MagicOption.NONE ? anim.magic.accentColor : new Color(0xFB923C));
            double glowR = s * scaleMul * 0.62;
            drawGlow(g, glow, tipX, tipY, glowR, alpha * 0.78f, false);
            drawGlow(g, Color.WHITE, tipX, tipY, glowR * 0.42, alpha * 0.75f, false);

            // Muzzle voxel overpressure concussion ring + chunky propellant smoke cube
            if (mProg < 0.68) {
                double rp = mProg / 0.68;
                double ringCx = tipX + Math.cos(anim.aimAngleRad) * s * (0.14 + rp * 0.48);
                double ringCy = tipY + Math.sin(anim.aimAngleRad) * s * (0.14 + rp * 0.48);
                int rRad = (int) Math.round(s * (0.16 + rp * 0.56));
                g.setComposite(alphaComp((1.0 - rp) * 0.78));
                g.setColor(new Color(0xFEF3C7));
                SpriteAssets.drawVoxelRing(g, ringCx, ringCy, rRad, Math.max(2, (int) Math.round(2.5 * (1.0 - rp))), false);
            }
            if (mProg > 0.16) {
                double sp = (mProg - 0.16) / 0.84;
                double smX = tipX + Math.cos(anim.aimAngleRad) * s * (0.28 + sp * 0.62);
                double smY = tipY + Math.sin(anim.aimAngleRad) * s * (0.28 + sp * 0.62) - sp * s * 0.18;
                float smAlpha = (float) (Math.sin(sp * Math.PI) * 0.56);
                drawGlow(g, new Color(0xCBD5E1), smX, smY, s * (0.30 + sp * 0.40), smAlpha * 0.36f, true);
                g.setComposite(alphaComp(smAlpha));
                SpriteAssets.drawVoxelCube(g, (int) Math.round(smX), (int) Math.round(smY),
                        Math.max(5, (int) Math.round(s * (0.24 + sp * 0.32))),
                        new Color(0xCBD5E1), Color.WHITE);
            }

            // Directional 5-ray spark star cone on early blast frames
            if (mProg < 0.65) {
                float spAlpha = (float) ((1.0 - mProg / 0.65) * 0.94);
                g.setComposite(alphaComp(spAlpha));
                g.setColor(new Color(255, 244, 200));
                g.setStroke(roundStroke(2.2f));
                double rayLen = s * scaleMul * 0.58 * (1.0 - mProg * 0.30);
                double[] offs = {-0.62, -0.26, 0.0, 0.26, 0.62};
                double[] lens = {0.65, 0.88, 1.08, 0.88, 0.65};
                for (int ri = 0; ri < offs.length; ri++) {
                    double ra = anim.aimAngleRad + offs[ri];
                    double rL = rayLen * lens[ri];
                    g.drawLine((int) tipX, (int) tipY,
                            (int) (tipX + Math.cos(ra) * rL), (int) (tipY + Math.sin(ra) * rL));
                }
            }

            AffineTransform prevTx = g.getTransform();
            g.setComposite(alphaComp(alpha));
            g.translate(cx + Math.cos(anim.aimAngleRad) * (s * 0.25), cy + Math.sin(anim.aimAngleRad) * (s * 0.25));
            g.rotate(anim.aimAngleRad);
            SpriteAssets.drawSharp(g, frame, -size / 4, -size / 2, size, size);
            g.setTransform(prevTx);
        }
        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private static boolean spanContains(ShipSpan span, int row, int col) {
        for (int i = 0; i < span.length; i++) {
            int r = span.row + (span.horizontal ? 0 : i);
            int c = span.col + (span.horizontal ? i : 0);
            if (r == row && c == col) return true;
        }
        return false;
    }

    private boolean isPreviewCell(int row, int col) {
        if (previewRow < 0 || previewCol < 0 || previewLength <= 0) return false;
        for (int i = 0; i < previewLength; i++) {
            int targetRow = previewRow + (previewHorizontal ? 0 : i);
            int targetCol = previewCol + (previewHorizontal ? i : 0);
            if (row == targetRow && col == targetCol) return true;
        }
        return false;
    }

    private void drawPreview(Graphics2D g, int x, int y, int size) {
        Composite originalComposite = g.getComposite();
        Stroke originalStroke = g.getStroke();
        g.setComposite(AlphaComposite.SrcOver.derive(0.30f));
        g.setColor(previewValid ? UiTokens.PREVIEW_VALID : UiTokens.PREVIEW_INVALID);
        g.fillRoundRect(x + 2, y + 2, size - 4, size - 4, UiTokens.BORDER_RADIUS, UiTokens.BORDER_RADIUS);
        g.setComposite(originalComposite);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                UiTokens.SPACE_SM, new float[]{UiTokens.SPACE_SM, UiTokens.SPACE_XS}, 0));
        g.drawRoundRect(x + 2, y + 2, size - 4, size - 4, UiTokens.BORDER_RADIUS, UiTokens.BORDER_RADIUS);
        g.setStroke(originalStroke);
    }

    private void drawTargetPreview(Graphics2D g, int x, int y, int size) {
        Composite originalComposite = g.getComposite();
        Stroke originalStroke = g.getStroke();
        g.setComposite(AlphaComposite.SrcOver.derive(0.26f));
        g.setColor(targetPreviewColor);
        g.fillRect(x + 1, y + 1, size - 1, size - 1);
        g.setComposite(originalComposite);
        g.setColor(targetPreviewColor);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(x + 2, y + 2, size - 4, size - 4);
        int mark = Math.max(UiTokens.SPACE_XS, size / 7);
        int cx = x + size / 2;
        int cy = y + size / 2;
        g.setStroke(new BasicStroke(1.25f));
        g.drawLine(cx - mark, cy, cx + mark, cy);
        g.drawLine(cx, cy - mark, cx, cy + mark);
        g.setStroke(originalStroke);
    }

    private static void drawRevealedCell(Graphics2D g, int x, int y, int size) {
        int corner = Math.max(UiTokens.SPACE_SM, size / 5);
        int inset = UiTokens.SPACE_XS;
        g.setColor(UiTokens.REVEALED_TINT);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(x + inset, y + inset, x + inset + corner, y + inset);
        g.drawLine(x + inset, y + inset, x + inset, y + inset + corner);
        g.drawLine(x + size - inset, y + inset, x + size - inset - corner, y + inset);
        g.drawLine(x + size - inset, y + inset, x + size - inset, y + inset + corner);
        g.drawLine(x + inset, y + size - inset, x + inset + corner, y + size - inset);
        g.drawLine(x + inset, y + size - inset, x + inset, y + size - inset - corner);
        g.drawLine(x + size - inset, y + size - inset, x + size - inset - corner, y + size - inset);
        g.drawLine(x + size - inset, y + size - inset, x + size - inset, y + size - inset - corner);
    }

    private static void drawShieldedCell(Graphics2D g, int x, int y, int size) {
        int inset = Math.max(UiTokens.SPACE_SM, size / 4);
        Path2D shield = new Path2D.Float();
        shield.moveTo(x + size / 2, y + inset);
        shield.lineTo(x + size - inset, y + size / 3);
        shield.lineTo(x + size - inset, y + size / 2);
        shield.quadTo(x + size / 2, y + size - inset, x + inset, y + size / 2);
        shield.lineTo(x + inset, y + size / 3);
        shield.closePath();
        g.setColor(UiTokens.SHIELD_TINT);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(shield);
    }

    private static boolean containsCell(List<int[]> cells, int row, int col) {
        for (int[] cell : cells) if (cell[0] == row && cell[1] == col) return true;
        return false;
    }

    private static List<int[]> copyValidCells(Collection<int[]> cells) {
        if (cells == null || cells.isEmpty()) return List.of();
        List<int[]> copy = new ArrayList<>();
        for (int[] cell : cells) {
            if (cell != null && cell.length >= 2 && cell[0] >= 0 && cell[0] < Board.SIZE
                    && cell[1] >= 0 && cell[1] < Board.SIZE) copy.add(new int[]{cell[0], cell[1]});
        }
        return List.copyOf(copy);
    }

    private static void drawHudCornerBrackets(Graphics2D g, int x, int y, int size, int arm) {
        Stroke prev = g.getStroke();
        g.setColor(new Color(251, 191, 36, 145));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        // Top-left
        g.drawLine(x, y, x + arm, y);
        g.drawLine(x, y, x, y + arm);
        // Top-right
        g.drawLine(x + size - arm, y, x + size, y);
        g.drawLine(x + size, y, x + size, y + arm);
        // Bottom-left
        g.drawLine(x, y + size, x + arm, y + size);
        g.drawLine(x, y + size - arm, x, y + size);
        // Bottom-right
        g.drawLine(x + size - arm, y + size, x + size, y + size);
        g.drawLine(x + size, y + size - arm, x + size, y + size);
        g.setStroke(prev);
    }

    static void drawCell(Graphics2D g, int x, int y, int size, CellState state, boolean hover) {
        Color fill = switch (state) {
            case EMPTY -> OceanMapBackdrop.gridCellFill(hover);
            case SHIP -> UiTokens.STATE_SHIP;
            case HIT -> UiTokens.STATE_HIT;
            case MISS -> UiTokens.STATE_MISS;
        };
        g.setColor(fill);
        g.fillRect(x, y, size, size);
        g.setColor(OceanMapBackdrop.gridLineColor());
        g.setStroke(new BasicStroke(1f));
        g.drawRect(x, y, size, size);
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int inset = Math.max(UiTokens.SPACE_XS, size / 6);
        switch (state) {
            case EMPTY -> {
                g.setColor(UiTokens.GRID_HAIRLINE);
                int dot = Math.max(2, size / 16);
                g.fillRect(centerX - dot / 2, centerY - dot / 2, dot, dot);
            }
            case SHIP -> drawShip(g, x, y, size, inset);
            case HIT -> drawHit(g, centerX, centerY, size);
            case MISS -> drawMiss(g, centerX, centerY, size);
        }
        if (hover && state != CellState.EMPTY) {
            g.setColor(UiTokens.FOCUS_RING);
            g.setStroke(new BasicStroke(2f));
            g.drawRect(x + 2, y + 2, size - 4, size - 4);
        }
    }

    private static void drawShip(Graphics2D g, int x, int y, int size, int inset) {
        Path2D hull = new Path2D.Float();
        hull.moveTo(x + inset, y + inset);
        hull.lineTo(x + size - inset, y + inset);
        hull.lineTo(x + size - inset, y + size - inset * 2);
        hull.lineTo(x + size / 2, y + size - inset);
        hull.lineTo(x + inset, y + size - inset * 2);
        hull.closePath();
        g.setColor(UiTokens.STATE_SHIP);
        g.fill(hull);
        g.setColor(UiTokens.STEEL_EDGE);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(hull);
        g.drawLine(x + size / 2, y + inset, x + size / 2, y + size / 2);
    }

    private static void drawHit(Graphics2D g, int cx, int cy, int size) {
        Composite prevComp = g.getComposite();
        int r = Math.max(UiTokens.SPACE_XS, size / 4);
        drawGlow(g, UiTokens.HIT_CRIMSON, cx, cy, r * 1.6, 0.45f, true);
        g.setComposite(prevComp);
        g.setColor(UiTokens.HIT_MARK);
        g.setStroke(roundStroke(2.8f));
        g.drawLine(cx - r, cy - r, cx + r, cy + r);
        g.drawLine(cx + r, cy - r, cx - r, cy + r);
        g.setColor(new Color(0xFEF08A));
        g.setStroke(roundStroke(1.2f));
        int ir = Math.max(2, (int) (r * 0.58));
        g.drawLine(cx - ir, cy - ir, cx + ir, cy + ir);
        g.drawLine(cx + ir, cy - ir, cx - ir, cy + ir);
        g.setColor(UiTokens.HIT_MARK);
        SpriteAssets.drawVoxelRing(g, cx, cy, r + 3, 2, false);
    }

    private static void drawMiss(Graphics2D g, int cx, int cy, int size) {
        Composite prevComp = g.getComposite();
        int r = Math.max(UiTokens.SPACE_SM, size / 4);
        g.setComposite(alphaComp(0.48f));
        g.setColor(new Color(0xBAE6FD));
        SpriteAssets.drawVoxelRing(g, cx, cy, r + 2, 2, true);
        g.setComposite(alphaComp(0.78f));
        g.setColor(UiTokens.STEEL_EDGE);
        SpriteAssets.drawVoxelRing(g, cx, cy, Math.max(3, r - 2), 2, false);
        int dot = Math.max(3, size / 11);
        SpriteAssets.drawVoxelCube(g, cx, cy, dot, new Color(0xE0F2FE), Color.WHITE);
        g.setComposite(prevComp);
    }

    private boolean isSuperShipSpan(ShipSpan span) {
        return superShipRow >= 0 && span != null
                && span.row == superShipRow && span.col == superShipCol
                && span.length == superShipLength && span.horizontal == superShipHorizontal;
    }

    private void drawSuperShipAuraAndWake(Graphics2D g, ShipSpan span, int sx, int sy, int sw, int sh, int s, long now) {
        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        double pulse = 0.5 + 0.5 * Math.sin(now * 0.0065);
        int pad = Math.max(4, s / 4);

        // Cyan-gold Aether aura underneath the Super Dreadnought
        g.setComposite(alphaComp((float) (0.20 + 0.14 * pulse)));
        g.setColor(new Color(45, 225, 255));
        g.fillRoundRect(sx - pad, sy - pad, sw + pad * 2, sh + pad * 2, s, s);
        g.setComposite(alphaComp((float) (0.48 + 0.25 * pulse)));
        g.setColor(new Color(255, 215, 90));
        g.setStroke(roundStroke(1.9f));
        g.drawRoundRect(sx - pad + 2, sy - pad + 2, sw + pad * 2 - 4, sh + pad * 2 - 4, s, s);

        // Cobalt ion thruster plumes behind stern
        int flameLen = (int) Math.round(s * (0.45 + 0.28 * pulse));
        int twinOff = Math.max(4, s / 4);
        g.setComposite(alphaComp(0.75f));
        if (span.horizontal) {
            int cx = sx - 1;
            int cy = sy + sh / 2;
            for (int sign : new int[]{-1, 1}) {
                int y = cy + sign * twinOff;
                java.awt.Polygon cone = new java.awt.Polygon(
                        new int[]{cx, cx - flameLen, cx - flameLen / 2, cx},
                        new int[]{y - 3, y, y + 3, y + 3}, 4);
                g.setColor(new Color(70, 235, 255));
                g.fillPolygon(cone);
            }
        } else {
            int cx = sx + sw / 2;
            int cy = sy + sh + 1;
            for (int sign : new int[]{-1, 1}) {
                int x = cx + sign * twinOff;
                java.awt.Polygon cone = new java.awt.Polygon(
                        new int[]{x - 3, x, x + 3, x + 3},
                        new int[]{cy, cy + flameLen, cy + flameLen / 2, cy}, 4);
                g.setColor(new Color(70, 235, 255));
                g.fillPolygon(cone);
            }
        }
        g.setStroke(prevStroke);
        g.setComposite(prevComp);
    }

    private void drawSuperShipArmorPips(Graphics2D g, Layout layout, ShipSpan span, int s, long now) {
        Composite prevComp = g.getComposite();
        int pipW = Math.max(5, s / 5);
        int pipH = Math.max(4, s / 8);
        double pulse = 0.65 + 0.35 * Math.sin(now * 0.008);
        for (int i = 0; i < span.length; i++) {
            int r = span.row + (span.horizontal ? 0 : i);
            int c = span.col + (span.horizontal ? i : 0);
            if (r < 0 || r >= Board.SIZE || c < 0 || c >= Board.SIZE) continue;
            int armor = superArmorGrid[r][c];
            if (armor <= 0 || cellStateAt(r, c) == CellState.HIT) continue;
            int x = layout.gridX + c * s + 3;
            int y = layout.gridY + r * s + 3;
            for (int p = 0; p < armor; p++) {
                int px = x + p * (pipW + 2);
                g.setComposite(alphaComp((float) (0.92 * pulse)));
                g.setColor(new Color(255, 214, 72));
                g.fillRoundRect(px, y, pipW, pipH, 2, 2);
                g.setColor(new Color(100, 245, 255));
                g.drawRoundRect(px, y, pipW, pipH, 2, 2);
            }
        }
        g.setComposite(prevComp);
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private record Layout(int cellSize, int gridX, int gridY) { }
    private record ShipSpan(ShipType type, int row, int col, int length, boolean horizontal) { }

    private static final class WaterDrop {
        double x, y, vx, vy;
        final double size, waterY, sinkLimit;
        final boolean foam;
        final long endMs;

        WaterDrop(double x, double y, double vx, double vy, double size,
                  double waterY, double sinkLimit, boolean foam, long endMs) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.waterY = waterY;
            this.sinkLimit = sinkLimit;
            this.foam = foam;
            this.endMs = endMs;
        }
    }

    public record ShipLaunchInfo(
            ShipType shipType,
            int row,
            int col,
            int length,
            boolean horizontal,
            int muzzleRow,
            int muzzleCol,
            Point muzzlePoint
    ) { }

    private static final class ShipFireAnim {
        final ShipType type;
        final int row, col, length;
        final boolean horizontal;
        final int muzzleRow, muzzleCol;
        final double aimAngleRad;
        final CombatLoadout.WeaponOption weapon;
        final CombatLoadout.MagicOption magic;
        final Color overrideColor;
        final long startMs = System.currentTimeMillis();

        ShipFireAnim(ShipLaunchInfo info, double aimAngleRad, CombatLoadout.WeaponOption weapon, CombatLoadout.MagicOption magic, Color overrideColor) {
            this.type = info.shipType;
            this.row = info.row;
            this.col = info.col;
            this.length = info.length;
            this.horizontal = info.horizontal;
            this.muzzleRow = info.muzzleRow;
            this.muzzleCol = info.muzzleCol;
            this.aimAngleRad = aimAngleRad;
            this.weapon = weapon != null ? weapon : CombatLoadout.WeaponOption.CANNON;
            this.magic = magic != null ? magic : CombatLoadout.MagicOption.NONE;
            this.overrideColor = overrideColor;
        }

        boolean matches(ShipSpan span) {
            if (span.row == row && span.col == col && span.length == length && span.horizontal == horizontal) return true;
            return type != null && span.type == type;
        }
    }

    private static final class ShipHitAnim {
        final int row, col;
        final Color accent;
        final long startMs = System.currentTimeMillis();

        ShipHitAnim(int row, int col, Color accent) {
            this.row = row;
            this.col = col;
            this.accent = accent != null ? accent : UiTokens.HIT_CRIMSON;
        }
    }

    private static final class ShieldBlockAnim {
        final int row, col, intensity;
        final long startMs = System.currentTimeMillis();

        ShieldBlockAnim(int row, int col, int intensity) {
            this.row = row;
            this.col = col;
            this.intensity = Math.max(1, Math.min(3, intensity));
        }
    }

    private static final class ShieldCrack {
        final int row, col, severity;
        final Color accent;
        final double[] angles;
        final double[] lengths;
        final double[] jags;

        ShieldCrack(int row, int col, int severity, Color accent, long seed) {
            this.row = row;
            this.col = col;
            this.severity = Math.max(1, Math.min(3, severity));
            this.accent = accent != null ? accent : new Color(0xFBBF24);
            int branches = 4 + this.severity;
            this.angles = new double[branches];
            this.lengths = new double[branches];
            this.jags = new double[branches];
            java.util.Random r = new java.util.Random(seed ^ (row * 313L + col * 97L));
            for (int i = 0; i < branches; i++) {
                this.angles[i] = (i * (Math.PI * 2.0 / branches)) + (r.nextDouble() - 0.5) * 0.42;
                this.lengths[i] = 0.72 + r.nextDouble() * 0.68;
                this.jags[i] = (r.nextDouble() - 0.5) * 1.4;
            }
        }
    }

    private static final class ImpactZone {
        final int centerRow;
        final int centerCol;
        final int minRow;
        final int maxRow;
        final int minCol;
        final int maxCol;
        final List<int[]> cells;
        final CombatLoadout.MagicOption magic;
        final SpriteAssets.FxType fxType;
        final Color accent;
        final boolean zombieBoss;
        final long startMs = System.currentTimeMillis();

        ImpactZone(int centerRow, int centerCol, List<int[]> cells,
                   CombatLoadout.MagicOption magic, Color customAccent, boolean zombieBoss) {
            this.centerRow = centerRow;
            this.centerCol = centerCol;
            this.cells = cells;
            this.magic = magic != null ? magic : CombatLoadout.MagicOption.NONE;
            this.zombieBoss = zombieBoss;
            this.fxType = switch (this.magic) {
                case FIRE -> SpriteAssets.FxType.FIRE;
                case LIGHTNING -> SpriteAssets.FxType.LIGHTNING;
                case ARCANE_SONAR -> SpriteAssets.FxType.SONAR;
                case FROST -> SpriteAssets.FxType.FROST;
                case NONE -> zombieBoss ? SpriteAssets.FxType.FIRE : SpriteAssets.FxType.HIT;
            };
            this.accent = customAccent != null ? customAccent
                    : (this.magic != CombatLoadout.MagicOption.NONE ? this.magic.accentColor : new Color(0xFB923C));
            int minR = centerRow, maxR = centerRow, minC = centerCol, maxC = centerCol;
            for (int[] c : cells) {
                minR = Math.min(minR, c[0]);
                maxR = Math.max(maxR, c[0]);
                minC = Math.min(minC, c[1]);
                maxC = Math.max(maxC, c[1]);
            }
            this.minRow = minR;
            this.maxRow = maxR;
            this.minCol = minC;
            this.maxCol = maxC;
        }
    }

    private static final class ActiveEffect {
        final SpriteAssets.FxType type;
        final int row;
        final int col;
        final Color accent;
        final long startMs;
        int frame;
        double progress;

        ActiveEffect(SpriteAssets.FxType type, int row, int col, Color accent, long delayMs) {
            this.type = type;
            this.row = row;
            this.col = col;
            this.accent = accent != null ? accent : defaultAccent(type);
            this.startMs = System.currentTimeMillis() + Math.max(0L, delayMs);
        }

        private static Color defaultAccent(SpriteAssets.FxType type) {
            return switch (type) {
                case FIRE -> UiTokens.MAGIC_FIRE;
                case LIGHTNING -> UiTokens.MAGIC_LIGHTNING;
                case SONAR -> UiTokens.MAGIC_SONAR;
                case FROST -> UiTokens.MAGIC_FROST;
                case MISS -> UiTokens.STEEL_EDGE;
                case HIT -> UiTokens.HIT_CRIMSON;
            };
        }
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        if (!interactive) setHover(-1, -1);
        updateCursor();
        repaint();
    }

    public int getHoveredRow() { return hoveredRow; }
    public int getHoveredCol() { return hoveredCol; }
    private void updateCursor() { setCursor(Cursor.getPredefinedCursor(interactive ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR)); }
    public void setShowShips(boolean showShips) { this.showShips = showShips; repaint(); }
    public void setTitle(String title) { this.title = title == null ? "" : title; repaint(); }
    public void setZombieFleet(boolean zombieFleet) { this.zombieFleet = zombieFleet; repaint(); }
    public boolean isZombieFleet() { return zombieFleet; }
    public void setBoard(Board board) {
        this.board = board;
        impactZones.clear();
        ensureFxTimerState();
        repaint();
    }

    public void setCellStateOverride(CellState[][] override) {
        this.cellStateOverride = override;
        staticGridCache = null;
        repaint();
    }

    public CellState[][] getCellStateOverride() {
        return cellStateOverride;
    }

    private CellState cellStateAt(int row, int col) {
        if (cellStateOverride != null && row >= 0 && row < Board.SIZE && col >= 0 && col < Board.SIZE) {
            CellState st = cellStateOverride[row][col];
            return st != null ? st : CellState.EMPTY;
        }
        return board == null ? CellState.EMPTY : board.getCell(row, col);
    }

    public void clearAllVisualState() {
        activeEffects.clear();
        shipFires.clear();
        shipHits.clear();
        shieldBlocks.clear();
        shieldCracks.clear();
        impactZones.clear();
        revealedCells = List.of();
        shieldedCells = List.of();
        fullMapShield = false;
        fullMapShieldStartMs = 0L;
        shieldEnergy = 0;
        shieldShatterUntilMs = 0L;
        superShipRow = -1;
        superShipCol = -1;
        superShipLength = 0;
        superShipHorizontal = true;
        superShipStartMs = 0L;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                superArmorGrid[r][c] = 0;
            }
        }
        targetPreviewCells = List.of();
        lastShotRow = -1;
        lastShotCol = -1;
        shakeAmplitude = 0;
        staticGridCache = null;
        ensureFxTimerState();
        repaint();
    }

    public void addImpactZone(int centerRow, int centerCol, Collection<int[]> cells,
                              CombatLoadout.MagicOption magic, Color accent, boolean zombieBoss) {
        List<int[]> valid = copyValidCells(cells);
        if (valid.isEmpty() && centerRow >= 0 && centerRow < Board.SIZE && centerCol >= 0 && centerCol < Board.SIZE) {
            valid = List.of(new int[]{centerRow, centerCol});
        }
        if (valid.isEmpty()) return;
        impactZones.removeIf(z -> z.centerRow == centerRow && z.centerCol == centerCol);
        while (impactZones.size() >= 4) {
            impactZones.remove(0);
        }
        impactZones.add(new ImpactZone(centerRow, centerCol, valid, magic, accent, zombieBoss));
        ensureFxTimerState();
        repaint();
    }

    public void clearImpactZones() {
        impactZones.clear();
        ensureFxTimerState();
        repaint();
    }

    public int getImpactZoneCount() { return impactZones.size(); }
    public void setOnCellClick(BiConsumer<Integer, Integer> listener) { this.onCellClick = listener; }
    public void setOnCellHover(BiConsumer<Integer, Integer> listener) { this.onCellHover = listener; }

    public void setShipPreview(int row, int col, int length, boolean horizontal, boolean valid) {
        setShipPreview(row, col, null, length, horizontal, valid);
    }

    public void setShipPreview(int row, int col, ShipType type, boolean horizontal, boolean valid) {
        setShipPreview(row, col, type, type == null ? 0 : type.length, horizontal, valid);
    }

    private void setShipPreview(int row, int col, ShipType type, int length, boolean horizontal, boolean valid) {
        previewRow = row;
        previewCol = col;
        previewShipType = type;
        previewLength = length;
        previewHorizontal = horizontal;
        previewValid = valid;
        repaint();
    }

    public void clearShipPreview() {
        previewRow = -1;
        previewCol = -1;
        previewShipType = null;
        previewLength = 0;
        repaint();
    }

    public void refreshPointerHover() {
        java.awt.Point point = getMousePosition();
        if (point != null) {
            hoveredRow = -1;
            hoveredCol = -1;
            updateHover(point.x, point.y);
        }
    }

    public void setLastShot(int row, int col) { lastShotRow = row; lastShotCol = col; repaint(); }
    public void clearLastShot() { lastShotRow = -1; lastShotCol = -1; repaint(); }
    public void setTargetPreview(List<int[]> cells, Color accentColor) {
        List<int[]> nextCells = copyValidCells(cells);
        Color nextColor = accentColor == null ? UiTokens.ACCENT_SIGNAL : accentColor;
        if (sameCellList(targetPreviewCells, nextCells) && Objects.equals(targetPreviewColor, nextColor)) {
            return;
        }
        targetPreviewCells = nextCells;
        targetPreviewColor = nextColor;
        repaint();
    }

    public void clearTargetPreview() {
        if (targetPreviewCells.isEmpty()) return;
        targetPreviewCells = List.of();
        repaint();
    }

    private static boolean sameCellList(List<int[]> a, List<int[]> b) {
        if (a == b) return true;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!Arrays.equals(a.get(i), b.get(i))) return false;
        }
        return true;
    }
    public void setRevealedCells(Collection<int[]> cells) { revealedCells = copyValidCells(cells); repaint(); }

    public void setShieldedCells(Collection<int[]> cells) {
        shieldedCells = copyValidCells(cells);
        if (shieldedCells.isEmpty()) {
            fullMapShield = false;
            shieldEnergy = 0;
            shieldCracks.clear();
        } else if (shieldEnergy <= 0) {
            shieldEnergy = SHIELD_MAX_ENERGY;
        }
        ensureFxTimerState();
        repaint();
    }

    public void setFullMapShield(boolean enabled, Collection<int[]> cells) {
        this.fullMapShield = enabled;
        this.fullMapShieldStartMs = enabled ? System.currentTimeMillis() : 0L;
        this.shieldedCells = enabled ? copyValidCells(cells) : List.of();
        if (enabled) {
            this.shieldEnergy = SHIELD_MAX_ENERGY;
            this.shieldCracks.clear();
            this.shieldShatterUntilMs = 0L;
        } else {
            this.shieldEnergy = 0;
            this.shieldCracks.clear();
        }
        ensureFxTimerState();
        repaint();
    }

    public boolean isFullMapShield() { return fullMapShield; }

    public int getShieldEnergy() { return shieldEnergy; }

    public void addShieldEnergy(int delta) {
        if ((!fullMapShield && shieldedCells.isEmpty()) || delta <= 0) return;
        shieldEnergy = Math.min(SHIELD_MAX_ENERGY, shieldEnergy + delta);
        for (int i = 0; i < 2 && !shieldCracks.isEmpty(); i++) {
            shieldCracks.remove(shieldCracks.size() - 1);
        }
        ensureFxTimerState();
        repaint();
    }

    public int getShieldCrackCount() { return shieldCracks.size(); }

    public List<int[]> getShieldedCells() { return shieldedCells; }

    public boolean isCellShielded(int row, int col) {
        if (row < 0 || row >= Board.SIZE || col < 0 || col >= Board.SIZE) return false;
        return fullMapShield || containsCell(shieldedCells, row, col);
    }

    /** Absorbs an incoming enemy hit into the active Magic Shield, stamping stress cracks and shattering at 0 energy. */
    public ShieldAbsorptionResult absorbShieldStrike(int row, int col, int energyDamage, Color impactAccent) {
        if (!fullMapShield && shieldedCells.isEmpty()) {
            return new ShieldAbsorptionResult(0, 0, false);
        }
        if (shieldEnergy <= 0) shieldEnergy = SHIELD_MAX_ENERGY;
        int dmg = Math.max(8, energyDamage);
        int intensity = dmg >= 45 ? 3 : (dmg >= 28 ? 2 : 1);
        triggerShieldBlock(row, col, intensity);

        int absorbed = Math.min(shieldEnergy, dmg);
        shieldEnergy = Math.max(0, shieldEnergy - dmg);
        addShieldCrackAt(row, col, intensity, impactAccent);
        if (dmg >= 38) {
            int altR = Math.max(0, Math.min(Board.SIZE - 1, row + (row < 5 ? 1 : -1)));
            int altC = Math.max(0, Math.min(Board.SIZE - 1, col + (col < 5 ? 1 : -1)));
            addShieldCrackAt(altR, altC, Math.max(1, intensity - 1), impactAccent);
        }

        if (shieldEnergy <= 0) {
            fullMapShield = false;
            shieldedCells = List.of();
            shieldCracks.clear();
            shieldShatterUntilMs = System.currentTimeMillis() + 560L;
            triggerShake(9);
            ensureFxTimerState();
            repaint();
            return new ShieldAbsorptionResult(absorbed, 0, true);
        }
        if (intensity >= 2) {
            triggerShake(intensity >= 3 ? 5 : 3);
        }
        ensureFxTimerState();
        repaint();
        return new ShieldAbsorptionResult(absorbed, shieldEnergy, false);
    }

    private void addShieldCrackAt(int row, int col, int severity, Color impactAccent) {
        int r = Math.max(0, Math.min(Board.SIZE - 1, row));
        int c = Math.max(0, Math.min(Board.SIZE - 1, col));
        while (shieldCracks.size() >= 18) {
            shieldCracks.remove(0);
        }
        shieldCracks.add(new ShieldCrack(r, c, severity, impactAccent, System.nanoTime() ^ shieldCracks.size() * 17L));
    }

    public record AliveShipInfo(
            ShipType shipType,
            int row,
            int col,
            int length,
            boolean horizontal,
            int aliveCells,
            Point center
    ) { }

    /** Returns surviving ships (at least 1 non-HIT cell) mapped in screen coordinates of targetSpace. */
    public List<AliveShipInfo> getAliveShips(Component targetSpace) {
        List<ShipSpan> all = resolveShipSpans(new boolean[Board.SIZE][Board.SIZE]);
        List<AliveShipInfo> result = new ArrayList<>();
        for (ShipSpan span : all) {
            int aliveCells = 0;
            for (int i = 0; i < span.length; i++) {
                int r = span.row + (span.horizontal ? 0 : i);
                int c = span.col + (span.horizontal ? i : 0);
                if (r >= 0 && r < Board.SIZE && c >= 0 && c < Board.SIZE && cellStateAt(r, c) != CellState.HIT) {
                    aliveCells++;
                }
            }
            if (aliveCells <= 0) continue;
            int er = span.row + (span.horizontal ? 0 : Math.max(0, span.length - 1));
            int ec = span.col + (span.horizontal ? Math.max(0, span.length - 1) : 0);
            Point mid = targetSpace != null
                    ? new Point(
                            (getCellCenterIn(span.row, span.col, targetSpace).x + getCellCenterIn(er, ec, targetSpace).x) / 2,
                            (getCellCenterIn(span.row, span.col, targetSpace).y + getCellCenterIn(er, ec, targetSpace).y) / 2)
                    : new Point(0, 0);
            ShipType t = span.type != null ? span.type : ShipType.CRUISER;
            result.add(new AliveShipInfo(t, span.row, span.col, span.length, span.horizontal, aliveCells, mid));
        }
        return result;
    }

    public int countAliveShips() {
        List<ShipSpan> all = resolveShipSpans(new boolean[Board.SIZE][Board.SIZE]);
        int count = 0;
        for (ShipSpan span : all) {
            if (isSpanAlive(span)) count++;
        }
        return count;
    }

    public void activateSuperShip(int row, int col, int length, boolean horizontal) {
        this.superShipRow = row;
        this.superShipCol = col;
        this.superShipLength = length;
        this.superShipHorizontal = horizontal;
        this.superShipStartMs = System.currentTimeMillis();
        this.staticGridCache = null;
        ensureFxTimerState();
        repaint();
    }

    public boolean hasSuperShip() {
        return superShipRow >= 0;
    }

    public void setSuperArmorGrid(int[][] armor) {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                superArmorGrid[r][c] = (armor != null && armor[r] != null) ? Math.max(0, armor[r][c]) : 0;
            }
        }
        repaint();
    }

    public ShipLaunchInfo getSuperShipLaunchInfo(Component targetSpace) {
        if (superShipRow >= 0 && superShipLength > 0) {
            int mr = superShipHorizontal ? superShipRow : superShipRow;
            int mc = superShipHorizontal ? Math.min(Board.SIZE - 1, superShipCol + superShipLength - 1) : superShipCol;
            Point pt = getCellCenterIn(mr, mc, targetSpace);
            return new ShipLaunchInfo(ShipType.CARRIER, superShipRow, superShipCol, superShipLength,
                    superShipHorizontal, mr, mc, pt);
        }
        return getLauncherForSkill(CombatLoadout.WeaponOption.CANNON, CombatLoadout.MagicOption.NONE, targetSpace);
    }

    /** Returns exact screen-space centers of all allied ships currently on the board (for 5-ship ritual FX). */
    public List<ShipLaunchInfo> getAlliedShipCentersIn(Component targetSpace) {
        List<ShipSpan> all = resolveShipSpans(new boolean[Board.SIZE][Board.SIZE]);
        List<ShipLaunchInfo> result = new ArrayList<>();
        for (ShipSpan span : all) {
            int er = span.row + (span.horizontal ? 0 : Math.max(0, span.length - 1));
            int ec = span.col + (span.horizontal ? Math.max(0, span.length - 1) : 0);
            Point a = getCellCenterIn(span.row, span.col, targetSpace);
            Point b = getCellCenterIn(er, ec, targetSpace);
            Point mid = new Point((a.x + b.x) / 2, (a.y + b.y) / 2);
            ShipType t = span.type != null ? span.type : ShipType.CRUISER;
            result.add(new ShipLaunchInfo(t, span.row, span.col, span.length, span.horizontal,
                    (span.row + er) / 2, (span.col + ec) / 2, mid));
        }
        return result;
    }

    public void triggerShieldBlock(int row, int col) {
        triggerShieldBlock(row, col, 1);
    }

    public void triggerShieldBlock(int row, int col, int intensity) {
        if (row < 0 || row >= Board.SIZE || col < 0 || col >= Board.SIZE) return;
        shieldBlocks.add(new ShieldBlockAnim(row, col, intensity));
        ensureFxTimerState();
        repaint();
    }

    public void triggerShipHitAt(int row, int col, Color accent) {
        if (row < 0 || row >= Board.SIZE || col < 0 || col >= Board.SIZE) return;
        shipHits.add(new ShipHitAnim(row, col, accent));
        ensureFxTimerState();
        repaint();
    }

    public void triggerShipFire(ShipLaunchInfo info, double aimAngleRad,
                                CombatLoadout.WeaponOption weapon, CombatLoadout.MagicOption magic) {
        triggerShipFire(info, aimAngleRad, weapon, magic, null);
    }

    public void triggerShipFire(ShipLaunchInfo info, double aimAngleRad,
                                CombatLoadout.WeaponOption weapon, CombatLoadout.MagicOption magic, Color overrideColor) {
        if (info == null) return;
        shipFires.add(new ShipFireAnim(info, aimAngleRad, weapon, magic, overrideColor));
        ensureFxTimerState();
        repaint();
    }

    public void setKnownShips(Collection<Ship> ships) {
        knownShips = ships == null ? List.of() : List.copyOf(ships);
        repaint();
    }

    public void playEffect(SpriteAssets.FxType type, int row, int col) {
        playEffect(type, row, col, null, 0L);
    }

    public void playEffect(SpriteAssets.FxType type, int row, int col, Color accent, long delayMs) {
        if (type == null || row < 0 || row >= Board.SIZE || col < 0 || col >= Board.SIZE) return;
        activeEffects.add(new ActiveEffect(type, row, col, accent, delayMs));
        if (type != SpriteAssets.FxType.MISS) {
            triggerShipHitAt(row, col, accent != null ? accent : ActiveEffect.defaultAccent(type));
        }
        ensureFxTimerState();
        repaint();
    }

    public void playEffects(SpriteAssets.FxType type, Collection<int[]> cells) {
        playEffects(type, cells, null, 0L);
    }

    public void playEffects(SpriteAssets.FxType type, Collection<int[]> cells, Color accent, long staggerMs) {
        if (type == null || cells == null || cells.isEmpty()) return;
        long delay = 0L;
        for (int[] cell : cells) {
            if (cell != null && cell.length >= 2 && cell[0] >= 0 && cell[0] < Board.SIZE
                    && cell[1] >= 0 && cell[1] < Board.SIZE) {
                activeEffects.add(new ActiveEffect(type, cell[0], cell[1], accent, delay));
                if (type != SpriteAssets.FxType.MISS && delay == 0L) {
                    triggerShipHitAt(cell[0], cell[1], accent != null ? accent : ActiveEffect.defaultAccent(type));
                }
                delay += Math.max(0L, staggerMs);
            }
        }
        ensureFxTimerState();
        repaint();
    }

    public void triggerShake(int intensityPx) {
        shakeAmplitude = Math.max(shakeAmplitude, Math.max(1.0, intensityPx));
        ensureFxTimerState();
    }

    /** Triggers directional ocean wind/wave whitecaps sweeping physically across this board's 10x10 water surface. */
    public void triggerWaterBlow(double vx, double vy, Color tint, int intensity, long durationMs) {
        long now = System.currentTimeMillis();
        this.waterBlowStartMs = now;
        this.waterBlowUntilMs = now + Math.max(450L, durationMs);
        this.waterBlowVx = vx;
        this.waterBlowVy = vy;
        this.waterBlowTint = tint != null ? tint : new Color(0x38BDF8);
        this.waterBlowIntensity = Math.max(1, Math.min(3, intensity));
        ensureFxTimerState();
        repaint();
    }

    private void drawWaterBlowOnBoard(Graphics2D g, Layout layout, long now) {
        if (waterBlowUntilMs <= waterBlowStartMs || now >= waterBlowUntilMs) return;
        double p = Math.max(0.0, Math.min(1.0, (now - waterBlowStartMs) / (double) (waterBlowUntilMs - waterBlowStartMs)));
        double fade = Math.min(1.0, p / 0.14) * (p > 0.45 ? Math.max(0.0, 1.0 - (p - 0.45) / 0.55) : 1.0);
        if (fade <= 0.02) return;

        int s = layout.cellSize;
        int gridPx = s * Board.SIZE;
        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        Shape prevClip = g.getClip();
        g.clipRect(layout.gridX, layout.gridY, gridPx, gridPx);

        double dirX = waterBlowVx >= 0 ? 1.0 : -1.0;
        double speed = 0.8 + Math.abs(waterBlowVx) * 0.35;
        int bands = 4 + waterBlowIntensity * 3;
        g.setStroke(new BasicStroke(Math.max(1.8f, s * 0.065f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < bands; i++) {
            double cycle = (p * speed * (1.25 + (i % 3) * 0.22) + i * 0.17) % 1.0;
            double travel = dirX >= 0 ? cycle : (1.0 - cycle);
            int x0 = layout.gridX + (int) Math.round(travel * (gridPx + s * 3)) - (int) (s * 1.5);
            int len = s * (2 + (i % 2));
            int y0 = layout.gridY + (int) Math.round(((i + 0.5) / bands) * gridPx + Math.sin(p * 12.0 + i) * (s * 0.22));
            int dy = (int) Math.round(waterBlowVy * s * 0.25);
            float alpha = (float) (fade * Math.sin(cycle * Math.PI) * (0.40 + waterBlowIntensity * 0.09));
            if (alpha > 0.02f) {
                g.setComposite(alphaComp(Math.min(0.85, alpha)));
                g.setColor(i % 2 == 0 ? new Color(0xF0F9FF) : waterBlowTint);
                g.drawLine(x0, y0, x0 + (int) (dirX * len), y0 + dy);
                // Sea spray foam droplets trailing the wave crest
                g.fillOval(x0 + (int) (dirX * len * 0.55) - 3, y0 + dy / 2 - 2, 6, 4);
            }
        }

        // Translucent wind-blown sea surf sheen sweeping across board
        g.setComposite(alphaComp(fade * (0.08 + waterBlowIntensity * 0.03)));
        g.setColor(waterBlowTint);
        g.fillRect(layout.gridX, layout.gridY, gridPx, gridPx);

        g.setClip(prevClip);
        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    public int getCellSize() {
        return computeLayout().cellSize;
    }

    public Point getCellCenterIn(int row, int col, Component targetSpace) {
        Layout layout = computeLayout();
        int r = clamp(row, 0, Board.SIZE - 1);
        int c = clamp(col, 0, Board.SIZE - 1);
        int cx = layout.gridX + c * layout.cellSize + layout.cellSize / 2;
        int cy = layout.gridY + r * layout.cellSize + layout.cellSize / 2;
        Point local = new Point(cx, cy);
        if (targetSpace == null) return local;
        return SwingUtilities.convertPoint(this, local, targetSpace);
    }

    public static ShipType preferredShipForSkill(CombatLoadout.WeaponOption weapon, CombatLoadout.MagicOption magic) {
        CombatLoadout.WeaponOption w = weapon != null ? weapon : CombatLoadout.WeaponOption.CANNON;
        CombatLoadout.MagicOption m = magic != null ? magic : CombatLoadout.MagicOption.NONE;
        return switch (w) {
            case AIRSTRIKE -> ShipType.CARRIER;
            case MISSILE_CROSS -> ShipType.BATTLESHIP;
            case TORPEDO -> ShipType.SUBMARINE;
            case CANNON -> m == CombatLoadout.MagicOption.NONE ? ShipType.DESTROYER : ShipType.CRUISER;
        };
    }

    public ShipLaunchInfo getLauncherForSkill(CombatLoadout.WeaponOption weapon, CombatLoadout.MagicOption magic, Component targetSpace) {
        ShipType preferred = preferredShipForSkill(weapon, magic);
        if (showShips) {
            List<ShipSpan> all = resolveShipSpans(new boolean[Board.SIZE][Board.SIZE]);
            List<ShipSpan> alive = new ArrayList<>();
            for (ShipSpan span : all) {
                if (isSpanAlive(span)) alive.add(span);
            }
            List<ShipSpan> pool = alive.isEmpty() ? all : alive;
            ShipSpan chosen = null;
            for (ShipSpan span : pool) {
                if (span.type == preferred) {
                    chosen = span;
                    break;
                }
            }
            if (chosen == null && !pool.isEmpty()) {
                int bestDiff = Integer.MAX_VALUE;
                for (ShipSpan span : pool) {
                    int diff = Math.abs(span.length - preferred.length);
                    if (diff < bestDiff) {
                        bestDiff = diff;
                        chosen = span;
                    }
                }
            }
            if (chosen != null) {
                int offset = weapon == CombatLoadout.WeaponOption.AIRSTRIKE
                        ? chosen.length / 2
                        : (chosen.horizontal ? Math.max(0, chosen.length - 1) : 0);
                int mr = clamp(chosen.row + (chosen.horizontal ? 0 : offset), 0, Board.SIZE - 1);
                int mc = clamp(chosen.col + (chosen.horizontal ? offset : 0), 0, Board.SIZE - 1);
                Point pt = getCellCenterIn(mr, mc, targetSpace);
                ShipType resolvedType = chosen.type != null ? chosen.type : preferred;
                return new ShipLaunchInfo(resolvedType, chosen.row, chosen.col, chosen.length, chosen.horizontal, mr, mc, pt);
            }
        }
        int mr = clamp(preferred.length == 2 ? 7 : (preferred.length == 5 ? 2 : 4), 0, Board.SIZE - 1);
        int mc = clamp(showShips ? 7 : 1, 0, Board.SIZE - 1);
        Point pt = getCellCenterIn(mr, mc, targetSpace);
        return new ShipLaunchInfo(preferred, mr, mc, preferred.length, true, mr, mc, pt);
    }

    public ShipLaunchInfo getLauncherForZombie(ZombieMode.ZombieClass zc, Component targetSpace) {
        ShipType preferred = zc != null ? zc.shipType : ShipType.DESTROYER;
        CombatLoadout.WeaponOption w = switch (preferred) {
            case CARRIER -> CombatLoadout.WeaponOption.AIRSTRIKE;
            case BATTLESHIP -> CombatLoadout.WeaponOption.MISSILE_CROSS;
            case SUBMARINE -> CombatLoadout.WeaponOption.TORPEDO;
            default -> CombatLoadout.WeaponOption.CANNON;
        };
        if (showShips || zombieFleet) {
            List<ShipSpan> all = resolveShipSpans(new boolean[Board.SIZE][Board.SIZE]);
            for (ShipSpan span : all) {
                if (span.type == preferred) {
                    int offset = w == CombatLoadout.WeaponOption.AIRSTRIKE
                            ? span.length / 2
                            : (span.horizontal ? 0 : Math.max(0, span.length - 1));
                    int mr = clamp(span.row + (span.horizontal ? 0 : offset), 0, Board.SIZE - 1);
                    int mc = clamp(span.col + (span.horizontal ? offset : 0), 0, Board.SIZE - 1);
                    Point pt = getCellCenterIn(mr, mc, targetSpace);
                    return new ShipLaunchInfo(preferred, span.row, span.col, span.length, span.horizontal, mr, mc, pt);
                }
            }
        }
        return getLauncherForSkill(w, CombatLoadout.MagicOption.NONE, targetSpace);
    }

    private boolean isSpanAlive(ShipSpan span) {
        if (board == null && cellStateOverride == null) return true;
        for (int i = 0; i < span.length; i++) {
            int r = span.row + (span.horizontal ? 0 : i);
            int c = span.col + (span.horizontal ? i : 0);
            if (r >= 0 && r < Board.SIZE && c >= 0 && c < Board.SIZE && cellStateAt(r, c) != CellState.HIT) {
                return true;
            }
        }
        return false;
    }

    public void setTitanBoss(TitanMode.TitanBoss boss, int hp, int maxHp, boolean enraged) {
        this.titanBoss = boss;
        this.titanHp = Math.max(0, hp);
        this.titanMaxHp = Math.max(1, maxHp);
        this.titanEnraged = enraged;
        this.staticGridCache = null;
        ensureFxTimerState();
        repaint();
    }

    public void updateTitanState(int hp, int maxHp, boolean enraged) {
        this.titanHp = Math.max(0, hp);
        this.titanMaxHp = Math.max(1, maxHp);
        this.titanEnraged = enraged;
        ensureFxTimerState();
        repaint();
    }

    public void triggerTitanCharge(long durationMs) {
        long now = System.currentTimeMillis();
        this.titanChargeUntilMs = now + Math.max(250L, durationMs);
        burstTitanWater(18, 0.95);
        ensureFxTimerState();
        repaint();
    }

    public void triggerTitanHitFlash(Color accent) {
        this.titanHitFlashColor = accent != null ? accent : UiTokens.ACCENT_SIGNAL;
        this.titanHitFlashUntilMs = System.currentTimeMillis() + 360L;
        burstTitanWater(14, 0.85);
        ensureFxTimerState();
        repaint();
    }

    public void triggerTitanEmergence(long durationMs) {
        long now = System.currentTimeMillis();
        this.titanEmergenceStartMs = now;
        this.titanEmergenceUntilMs = now + Math.max(500L, durationMs);
        triggerShake(11);
        burstTitanWater(38, 1.45);
        ensureFxTimerState();
        repaint();
    }

    private static double titanWaterRatio(TitanMode.TitanBoss boss) {
        if (boss == TitanMode.TitanBoss.GHIDORAH) return 0.8826;
        if (boss == TitanMode.TitanBoss.MOTHRA) return 0.8497;
        return 0.8215;
    }

    private void burstTitanWater(int count, double power) {
        if (titanBoss == null) return;
        Layout layout = computeLayout();
        if (layout.cellSize <= 4) return;
        long now = System.currentTimeMillis();
        int[] bounds = computeTitanBounds(layout, now);
        double waterRatio = titanWaterRatio(titanBoss);
        double waterY = bounds[1] + bounds[3] * waterRatio;
        spawnTitanWaterDropsAt(bounds[0], bounds[2], waterY, count, power, now);
    }

    private int[] computeTitanBounds(Layout layout, long now) {
        int s = layout.cellSize;
        double elapsed = OceanMapBackdrop.elapsedSeconds();
        boolean charging = now < titanChargeUntilMs;
        boolean hitFlash = now < titanHitFlashUntilMs;
        double bobY = Math.sin(elapsed * (charging ? 6.2 : 2.1)) * Math.max(2.0, s * 0.09);
        double lungeX = charging ? -Math.sin((titanChargeUntilMs - now) * 0.015) * s * 0.28 : 0.0;
        double shakeX = hitFlash ? Math.sin(now * 0.075) * 5.0 : 0.0;

        double surgeY = 0.0;
        if (now < titanEmergenceUntilMs && titanEmergenceUntilMs > titanEmergenceStartMs) {
            double p = Math.max(0.0, Math.min(1.0,
                    (now - titanEmergenceStartMs) / (double) (titanEmergenceUntilMs - titanEmergenceStartMs)));
            // Dramatic ocean breach surge: starts submerged deep underwater and crests with heavy rebound
            if (p < 0.72) {
                double u = p / 0.72;
                double easeOut = 1.0 - Math.pow(1.0 - u, 3.0);
                surgeY = (1.0 - easeOut) * (s * 6.4) - Math.sin(u * Math.PI) * (s * 0.35);
            } else {
                double u = (p - 0.72) / 0.28;
                surgeY = -Math.sin(u * Math.PI) * (s * 0.26);
            }
        }

        double yShift = (titanBoss == TitanMode.TitanBoss.GHIDORAH) ? -0.18
                : (titanBoss == TitanMode.TitanBoss.MOTHRA ? -0.22 : 0.08);
        int bw = (int) Math.round(s * 10.6);
        int bh = (int) Math.round(s * 10.6);
        int bx = (int) Math.round(layout.gridX - s * 0.35 + lungeX + shakeX);
        int by = (int) Math.round(layout.gridY + s * yShift + bobY + surgeY);
        return new int[]{bx, by, bw, bh};
    }

    public List<Point> getGodzillaDorsalPointsIn(Component targetSpace) {
        Layout layout = computeLayout();
        int[] bounds = computeTitanBounds(layout, System.currentTimeMillis());
        double[][] dorsalUv = {
            {0.7764, 0.6748}, // dorsal_low (bottom emergent fin)
            {0.7378, 0.5059}, // dorsal_mid
            {0.6692, 0.3475}, // dorsal_upper
            {0.5976, 0.1938}, // dorsal_top (skull crest fin)
        };
        List<Point> pts = new ArrayList<>(dorsalUv.length);
        for (double[] uv : dorsalUv) {
            Point local = new Point(
                    (int) Math.round(bounds[0] + uv[0] * bounds[2]),
                    (int) Math.round(bounds[1] + uv[1] * bounds[3]));
            pts.add(targetSpace == null ? local : SwingUtilities.convertPoint(this, local, targetSpace));
        }
        return pts;
    }

    public Point getTitanLaunchPointIn(TitanMode.TitanBoss boss, int headOrPartIndex, Component targetSpace) {
        Layout layout = computeLayout();
        int[] bounds = computeTitanBounds(layout, System.currentTimeMillis());
        double u;
        double v;
        if (boss == TitanMode.TitanBoss.GHIDORAH) {
            switch (headOrPartIndex) {
                case 1 -> { u = 0.2351; v = 0.5516; }   // Alpha Head
                case 2 -> { u = 0.3396; v = 0.7339; }   // Left Outer Head
                case 3 -> { u = 0.0942; v = 0.6542; }   // Foreground Head
                case 4 -> { u = 0.5449; v = 0.3519; }   // Hurricane Wing Vortex
                default -> { u = 0.2600; v = 0.5800; }  // Central Gravity Storm Core
            }
        } else if (boss == TitanMode.TitanBoss.MOTHRA) {
            // Mothra Blender projected [u, v] emitters: 0 = Mouth/Antenna, 1 = Wing L, 2 = Wing R, 3 = Thorax Core
            switch (headOrPartIndex) {
                case 1 -> { u = 0.7077; v = 0.4526; }   // Upper Bioluminescent Wing
                case 2 -> { u = 0.1308; v = 0.3105; }   // Foreground Wingtip
                case 3 -> { u = 0.4309; v = 0.6677; }   // Glowing Thorax / Silk Core
                default -> { u = 0.3307; v = 0.6596; }  // Mouth / God-Ray Focal Point
            }
        } else {
            // Godzilla Blender projected [u, v] emitters: 0 = Mouth, 1 = Chest Core, 2 = Ocean Tail Breach, 3..6 = Dorsal Spines
            switch (headOrPartIndex) {
                case 1 -> { u = 0.4695; v = 0.7591; }   // Burning Chest Core
                case 2 -> { u = 0.8400; v = 0.8600; }   // Tail Breach Wake
                case 3 -> { u = 0.5976; v = 0.1938; }   // Dorsal Top
                case 4 -> { u = 0.6692; v = 0.3475; }   // Dorsal Upper
                case 5 -> { u = 0.7378; v = 0.5059; }   // Dorsal Mid
                case 6 -> { u = 0.7764; v = 0.6748; }   // Dorsal Low
                default -> { u = 0.1683; v = 0.4195; }  // Roaring Mouth Cherenkov Focal Point
            }
        }
        Point local = new Point(
                (int) Math.round(bounds[0] + u * bounds[2]),
                (int) Math.round(bounds[1] + v * bounds[3]));
        return targetSpace == null ? local : SwingUtilities.convertPoint(this, local, targetSpace);
    }

    private void drawTitanBossEntity(Graphics2D g, Layout layout, long now) {
        TitanMode.TitanBoss boss = this.titanBoss;
        if (boss == null) return;
        int s = layout.cellSize;
        double elapsed = OceanMapBackdrop.elapsedSeconds();
        boolean charging = now < titanChargeUntilMs;
        boolean hitFlash = now < titanHitFlashUntilMs;
        boolean enraged = this.titanEnraged;
        Color auraCol = enraged ? boss.enrageColor : boss.accentColor;

        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        Shape prevClip = g.getClip();

        // 1. Anatomical hitbox tactical outlines & weakpoint diamonds on sea grid
        List<int[]> hitboxes = TitanMode.generateHitboxCells(boss);
        List<int[]> weakpoints = TitanMode.generateWeakpointCells(boss);
        for (int[] hc : hitboxes) {
            int cx = layout.gridX + hc[1] * s;
            int cy = layout.gridY + hc[0] * s;
            CellState st = cellStateAt(hc[0], hc[1]);
            boolean isWeak = containsCell(weakpoints, hc[0], hc[1]);
            if (st != CellState.HIT) {
                g.setComposite(alphaComp(isWeak ? 0.24 : 0.14));
                g.setColor(isWeak ? auraCol : new Color(0x94A3B8));
                g.fillRect(cx + 2, cy + 2, s - 4, s - 4);
                g.setComposite(alphaComp(isWeak ? 0.62 : 0.34));
                g.setStroke(roundStroke(isWeak ? 1.4f : 1.0f));
                g.drawRect(cx + 2, cy + 2, s - 5, s - 5);
                if (isWeak) {
                    int m = Math.max(3, s / 6);
                    SpriteAssets.drawVoxelCube(g, cx + s / 2, cy + s / 2, m, Color.WHITE, auraCol);
                }
            }
        }

        // 2. Overwhelming Semi-Submerged 3D Voxel Kaiju Boss Geometry
        int[] bounds = computeTitanBounds(layout, now);
        int bx = bounds[0];
        int by = bounds[1];
        int bw = bounds[2];
        int bh = bounds[3];
        double waterRatio = titanWaterRatio(boss);
        double waterX = bx + bw * 0.50;
        double waterY = by + bh * waterRatio;
        // Clamp visual waterline within lower arena when rising vertically from deep ocean during emergence
        double surfaceY = Math.min(layout.gridY + s * 9.35, waterY);

        // Layer A: Subsurface silhouette + 2.5D ocean displacement swells BEHIND sprite
        drawTitanWaterBehind(g, layout, bx, by, bw, bh, surfaceY, auraCol, enraged, charging, elapsed);

        // Boss Ambient / Enrage / Charge Aura Halo
        if (enraged || charging || hitFlash) {
            Color halo = hitFlash ? titanHitFlashColor : auraCol;
            float ha = hitFlash ? 0.62f : (charging ? 0.56f : 0.40f);
            drawGlow(g, halo, bx + bw * 0.46, by + bh * 0.44, bw * 0.48, ha, true);
        }

        // Clip lower submerged torso beneath sea surface plane when emerging upward from ocean depth
        int seaCutY = (int) Math.round(surfaceY + s * 0.30);
        Shape boardClip = new Rectangle(layout.gridX - s, layout.gridY - s,
                s * (Board.SIZE + 2), Math.max(s * 2, seaCutY - (layout.gridY - s)));
        g.clip(boardClip);

        BufferedImage shadow = SpriteAssets.getTitanShadow(boss, enraged);
        if (shadow != null) {
            g.setComposite(alphaComp(0.58));
            g.drawImage(shadow, bx + Math.max(4, s / 6), by + Math.max(6, s / 5), bw, bh, null);
        }

        BufferedImage sprite = SpriteAssets.getTitanSprite(boss, enraged, charging);
        if (sprite != null) {
            g.setComposite(alphaComp(titanHp <= 0 ? 0.36f : 1.0f));
            SpriteAssets.drawSharp(g, sprite, bx, by, bw, bh);
        }
        g.setClip(prevClip);

        // Live dorsal spine ignition glow along Godzilla's maple-leaf fins when charging!
        if (charging && boss == TitanMode.TitanBoss.GODZILLA) {
            double rem = Math.max(0.0, Math.min(1.0, 1.0 - (titanChargeUntilMs - now) / 900.0));
            double[][] dUv = {{0.7764, 0.6748}, {0.7378, 0.5059}, {0.6692, 0.3475}, {0.5976, 0.1938}};
            Color dCol = enraged ? new Color(0xFB923C) : new Color(0x22D3EE);
            for (int i = 0; i < dUv.length; i++) {
                double thresh = i * 0.16;
                if (rem >= thresh) {
                    double localP = Math.min(1.0, (rem - thresh) / 0.25);
                    double dx = bx + dUv[i][0] * bw;
                    double dy = by + dUv[i][1] * bh;
                    drawGlow(g, dCol, dx, dy, s * (0.95 + 0.35 * localP), (float) (0.72 * localP), false);
                }
            }
        }

        if (hitFlash) {
            drawGlow(g, Color.WHITE, bx + bw * 0.45, by + bh * 0.45, bw * 0.38, 0.56f, false);
        }

        // Layer B: Overlapping ocean surf collar + gravity ocean splash droplets IN FRONT OF waterline rim
        drawTitanWaterFront(g, layout, bx, by, bw, bh, surfaceY, auraCol, enraged, charging, elapsed, now);

        // 3. Crisp Compact Voxel Boss HP & Phase Bar inside row 0 headroom (never clips outside board!)
        int barW = Math.min(s * 8, layout.cellSize * 8 - 8);
        int barH = Math.max(15, Math.min(20, s / 2 + 2));
        int barX = layout.gridX + (s * Board.SIZE - barW) / 2;
        int barY = layout.gridY + Math.max(3, (s - barH) / 2);

        g.setComposite(alphaComp(0.88));
        g.setColor(new Color(8, 15, 29));
        SpriteAssets.fillChamferedBox(g, barX, barY, barW, barH, 4);
        g.setColor(auraCol);
        SpriteAssets.drawVoxelChamferBox(g, barX, barY, barW, barH, 4, 2);

        double ratio = Math.max(0.0, Math.min(1.0, titanHp / (double) Math.max(1, titanMaxHp)));
        int fillW = (int) Math.round((barW - 6) * ratio);
        if (fillW > 0) {
            g.setComposite(alphaComp(0.92));
            g.setColor(auraCol);
            SpriteAssets.fillChamferedBox(g, barX + 3, barY + 3, fillW, barH - 6, 2);
            g.setColor(Color.WHITE);
            g.setComposite(alphaComp(0.26));
            g.fillRect(barX + 4, barY + 4, Math.max(2, fillW - 2), 2);
        }
        g.setComposite(alphaComp(1.0));
        g.setFont(UiTokens.FONT_COORD);
        FontMetrics fm = g.getFontMetrics();
        String bossLabel = switch (boss) {
            case GODZILLA -> enraged ? "BURNING GODZILLA · " : "GODZILLA · ";
            case GHIDORAH -> enraged ? "STORM GHIDORAH · " : "KING GHIDORAH · ";
            case MOTHRA -> enraged ? "SOLAR MOTHRA · " : "MOTHRA · ";
        };
        String hpText = bossLabel + titanHp + "/" + titanMaxHp + " HP";
        int tx = barX + (barW - fm.stringWidth(hpText)) / 2;
        int ty = barY + (barH - fm.getHeight()) / 2 + fm.getAscent();
        g.setColor(new Color(0, 0, 0, 175));
        g.drawString(hpText, tx + 1, ty + 1);
        g.setColor(Color.WHITE);
        g.drawString(hpText, tx, ty);

        g.setComposite(prevComp);
        g.setStroke(prevStroke);
    }

    private void drawTitanWaterBehind(Graphics2D g, Layout layout, int bx, int by, int bw, int bh,
                                      double waterY, Color auraCol, boolean enraged, boolean charging, double elapsed) {
        double cx = bx + bw * 0.50;
        // 1. Deep underwater submerged body shadow descending below waterline
        int subW = (int) Math.round(bw * 0.62);
        int subH = (int) Math.round(layout.cellSize * 1.45);
        g.setComposite(alphaComp(0.46));
        g.setColor(new Color(2, 10, 24));
        g.fillOval((int) Math.round(cx - subW * 0.5), (int) Math.round(waterY - subH * 0.34), subW, subH);

        // Subsurface glowing thermal/Cherenkov radiation permeating ocean water
        if (charging || enraged) {
            drawGlow(g, auraCol, cx, waterY, bw * 0.38, charging ? 0.52f : 0.34f, true);
        }

        // 2. Three 2.5D elliptical ocean displacement wave rings radiating outward across waves
        for (int ring = 0; ring < 3; ring++) {
            double cycle = (elapsed * (charging ? 0.95 : 0.42) + ring * 0.33) % 1.0;
            double rx = bw * (0.28 + cycle * 0.34);
            double ry = rx * 0.25;
            float a = (float) ((1.0 - cycle) * (charging ? 0.58 : 0.38));
            g.setComposite(alphaComp(a));
            g.setColor(ring == 0 ? Color.WHITE : (enraged ? auraCol : new Color(0x7DD3FC)));
            g.setStroke(roundStroke(ring == 0 ? 2.4f : 1.6f));
            g.drawOval((int) Math.round(cx - rx), (int) Math.round(waterY - ry * 0.65),
                    (int) Math.round(rx * 2.0), (int) Math.round(ry * 2.0));
        }
    }

    private void drawTitanWaterFront(Graphics2D g, Layout layout, int bx, int by, int bw, int bh,
                                     double waterY, Color auraCol, boolean enraged, boolean charging,
                                     double elapsed, long now) {
        int s = layout.cellSize;
        double cx = bx + bw * 0.50;
        double spanW = bw * 0.66;
        double xLeft = cx - spanW * 0.5;
        double xRight = cx + spanW * 0.5;

        // 1. Overlapping sinusoidal ocean swell & white breaking surf collar IN FRONT OF submerged chest rim
        int steps = 16;
        int[] xs = new int[steps + 2];
        int[] ys = new int[steps + 2];
        for (int i = 0; i <= steps; i++) {
            double u = i / (double) steps;
            double arch = Math.sin(u * Math.PI);
            double wave = Math.sin(u * 11.0 + elapsed * (charging ? 7.0 : 3.2)) * (s * 0.085);
            xs[i] = (int) Math.round(xLeft + u * spanW);
            ys[i] = (int) Math.round(waterY - arch * (s * 0.14) + wave);
        }
        xs[steps + 1] = (int) Math.round(xRight);
        ys[steps + 1] = (int) Math.round(waterY + s * 0.36);

        // Stepped crisp voxel surf blocks lapping against lower torso
        for (int i = 0; i < steps; i++) {
            double u = i / (double) steps;
            double arch = Math.sin(u * Math.PI);
            int wx = xs[i];
            int wy = ys[i];
            int cellW = Math.max(4, (xs[i + 1] - wx) + 1);
            int h = Math.max(4, (int) Math.round(s * (0.14 + arch * 0.22)));
            g.setComposite(alphaComp(0.55));
            g.setColor(enraged ? new Color(0x9A3412) : new Color(0x0369A1));
            g.fillRect(wx, wy, cellW, h);
            g.setComposite(alphaComp(0.82));
            g.setColor(i % 3 == 0 ? Color.WHITE : (enraged ? new Color(0xFDBA74) : new Color(0xBAE6FD)));
            g.fillRect(wx, wy - 1, cellW, Math.max(2, s / 10));
        }

        // Front dynamic specular wave arc
        g.setComposite(alphaComp(0.68));
        g.setColor(enraged ? new Color(0xFDE047) : new Color(0xE0F2FE));
        g.setStroke(roundStroke(2.2f));
        g.drawPolyline(xs, ys, steps + 1);

        // 2. Physical gravity-driven water splash droplets bursting and falling onto waterline
        if (titanWaterDrops.isEmpty() && bw > 16) {
            spawnTitanWaterDropsAt(bx, bw, waterY, 12, 0.58, now);
        }
        for (WaterDrop d : titanWaterDrops) {
            double lifeFrac = Math.max(0.0, Math.min(1.0, (d.endMs - now) / 600.0));
            g.setComposite(alphaComp(Math.min(0.95, lifeFrac * 1.15)));
            int dx = (int) Math.round(d.x);
            int dy = (int) Math.round(d.y);
            int ds = Math.max(3, (int) Math.round(d.size));
            // Motion droplet tail + voxel cube crest
            g.setColor(d.foam ? Color.WHITE : (enraged ? new Color(0xFDBA74) : new Color(0x7DD3FC)));
            g.drawLine(dx, dy, (int) Math.round(dx - d.vx * 1.6), (int) Math.round(dy - d.vy * 1.6));
            SpriteAssets.drawVoxelCube(g, dx, dy, ds, Color.WHITE,
                    d.foam ? new Color(0xE0F2FE) : (enraged ? auraCol : new Color(0x38BDF8)));
            // Surface splash ripple when droplet hits waterline!
            if (d.vy > 0 && d.y >= d.waterY - 2.0) {
                g.setComposite(alphaComp(0.55));
                SpriteAssets.drawVoxelRing(g, dx, d.waterY, ds * 1.6, 2, false);
            }
        }
    }

    public Point getDefaultLauncherPointIn(CombatLoadout.WeaponOption weapon, Component targetSpace) {
        return getLauncherForSkill(weapon, CombatLoadout.MagicOption.NONE, targetSpace).muzzlePoint();
    }

    public void refresh() { repaint(); }
}
