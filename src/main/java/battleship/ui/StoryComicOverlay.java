package battleship.ui;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Full-window anime/comic cutscene player ("Tranh Hoạt Hình Cốt Truyện").
 * While an episode plays, the overlay mounts itself on the host window's root layered pane
 * (POPUP_LAYER) so it covers the whole frame, header bar included, and swallows all input.
 * Art is drawn with COVER scaling + Ken Burns motion; dialogue sits on a dark bottom band.
 */
public final class StoryComicOverlay extends JPanel {
    private static final double CHARS_PER_SEC = 58.0;
    private static final double AUTO_ADVANCE_PAUSE_SEC = 2.6;
    private static final int TOP_STRIP_H = 64;
    private static final int GUTTER = 24;

    private final JComponent host;
    private final Timer animTimer;
    private final Rectangle autoBtnRect = new Rectangle();
    private final Rectangle skipBtnRect = new Rectangle();
    private final ComponentAdapter followLayeredPaneSize = new ComponentAdapter() {
        @Override public void componentResized(ComponentEvent e) {
            if (mountedOn != null) setBounds(0, 0, mountedOn.getWidth(), mountedOn.getHeight());
        }
    };

    private JLayeredPane mountedOn;
    private StoryMode.StoryEpisode currentEpisode;
    private Runnable onFinishedCallback;
    private int frameIndex = 0;
    private long overlayOpenedMs;
    private long frameStartedMs;
    private long typewriterDoneMs;
    private boolean autoPlay = false;
    private String hoveredControl = null;

    /** @param host the panel that owns this cutscene; the overlay covers that panel's window. */
    public StoryComicOverlay(JComponent host) {
        this.host = host;
        setOpaque(true);
        setVisible(false);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        animTimer = new Timer(33, e -> tickAnimation());

        // Host shown later (cutscene requested from its constructor) -> mount then; host hidden -> end story.
        host.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !isStoryActive()) return;
            if (host.isShowing()) mount();
            else closeAndFinish();
        });

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!isStoryActive()) return;
                requestFocusInWindow();
                SoundEngine.playUiClick();
                if (skipBtnRect.contains(e.getPoint())) {
                    skipCutscene();
                } else if (autoBtnRect.contains(e.getPoint())) {
                    autoPlay = !autoPlay;
                    typewriterDoneMs = System.currentTimeMillis();
                    repaint();
                } else {
                    advanceOrComplete();
                }
            }

            @Override public void mouseMoved(MouseEvent e) {
                String ctrl = skipBtnRect.contains(e.getPoint()) ? "SKIP"
                        : autoBtnRect.contains(e.getPoint()) ? "AUTO" : null;
                if (!java.util.Objects.equals(ctrl, hoveredControl)) {
                    hoveredControl = ctrl;
                    setCursor(Cursor.getPredefinedCursor(ctrl != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }
        };
        // Registering every mouse listener type keeps clicks, drags and wheel off the screen underneath.
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
        installKeyBindings();
    }

    private void installKeyBindings() {
        for (int condition : new int[]{JComponent.WHEN_FOCUSED, JComponent.WHEN_IN_FOCUSED_WINDOW}) {
            InputMap im = getInputMap(condition);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "story_next");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "story_next");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "story_next");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "story_prev");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "story_skip");
        }
        ActionMap am = getActionMap();
        am.put("story_next", storyAction(this::advanceOrComplete));
        am.put("story_prev", storyAction(this::prevFrame));
        am.put("story_skip", storyAction(this::skipCutscene));

        // While focused, eat every other key so game shortcuts (WASD, R, F...) don't fire under the story.
        InputMap focused = getInputMap(JComponent.WHEN_FOCUSED);
        KeyAdapter swallow = new KeyAdapter() {
            private void eat(KeyEvent e) {
                if (focused.get(KeyStroke.getKeyStrokeForEvent(e)) == null) e.consume();
            }
            @Override public void keyPressed(KeyEvent e) { eat(e); }
            @Override public void keyReleased(KeyEvent e) { eat(e); }
            @Override public void keyTyped(KeyEvent e) { eat(e); }
        };
        addKeyListener(swallow);
    }

    private AbstractAction storyAction(Runnable action) {
        return new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!isStoryActive()) return;
                SoundEngine.playUiClick();
                action.run();
            }
        };
    }

    public void showEpisode(StoryMode.StoryEpisode episode, Runnable onFinished) {
        if (episode == null || episode.frames() == null || episode.frames().isEmpty()) {
            if (onFinished != null) onFinished.run();
            return;
        }
        this.currentEpisode = episode;
        this.onFinishedCallback = onFinished;
        this.frameIndex = 0;
        long now = System.currentTimeMillis();
        this.overlayOpenedMs = now;
        this.frameStartedMs = now;
        this.typewriterDoneMs = 0L;
        this.hoveredControl = null;
        setVisible(true);
        if (!animTimer.isRunning()) animTimer.start();
        mount();
    }

    /** Re-parents onto the host window's layered pane; no-op until the host is in a window. */
    private void mount() {
        JRootPane root = SwingUtilities.getRootPane(host);
        if (root == null || !host.isShowing()) return;
        JLayeredPane lp = root.getLayeredPane();
        if (mountedOn != lp) {
            unmount();
            lp.add(this, JLayeredPane.POPUP_LAYER);
            lp.addComponentListener(followLayeredPaneSize);
            mountedOn = lp;
        }
        setBounds(0, 0, lp.getWidth(), lp.getHeight());
        lp.revalidate();
        repaint();
        requestFocusInWindow();
    }

    private void unmount() {
        if (mountedOn == null) return;
        JLayeredPane lp = mountedOn;
        mountedOn = null;
        lp.removeComponentListener(followLayeredPaneSize);
        lp.remove(this);
        lp.repaint();
    }

    public boolean isStoryActive() {
        return isVisible() && currentEpisode != null;
    }

    public StoryMode.StoryEpisode getCurrentEpisode() {
        return currentEpisode;
    }

    public int getCurrentFrameIndex() {
        return frameIndex;
    }

    public void jumpToFrame(int index) {
        if (currentEpisode == null) return;
        this.frameIndex = Math.max(0, Math.min(currentEpisode.frames().size() - 1, index));
        this.frameStartedMs = System.currentTimeMillis();
        this.typewriterDoneMs = 0L;
        repaint();
    }

    public void prevFrame() {
        if (currentEpisode != null && frameIndex > 0) jumpToFrame(frameIndex - 1);
    }

    /** Reveals the rest of a typing line first; otherwise next frame, or finish after the last one. */
    public void advanceOrComplete() {
        if (currentEpisode == null) {
            closeAndFinish();
            return;
        }
        StoryMode.StoryFrame f = currentFrame();
        if (f != null && revealedChars(f) < f.dialogue().length()) {
            frameStartedMs = System.currentTimeMillis() - (long) ((f.dialogue().length() + 4) * 1000.0 / CHARS_PER_SEC);
            typewriterDoneMs = System.currentTimeMillis();
            repaint();
            return;
        }
        if (frameIndex + 1 < currentEpisode.frames().size()) {
            jumpToFrame(frameIndex + 1);
        } else {
            closeAndFinish();
        }
    }

    public void skipCutscene() {
        closeAndFinish();
    }

    private void closeAndFinish() {
        if (animTimer.isRunning()) animTimer.stop();
        Runnable cb = onFinishedCallback;
        onFinishedCallback = null;
        currentEpisode = null;
        setVisible(false);
        unmount();
        if (cb != null) cb.run();
    }

    @Override public void removeNotify() {
        if (animTimer.isRunning()) animTimer.stop();
        super.removeNotify();
    }

    private StoryMode.StoryFrame currentFrame() {
        if (currentEpisode == null || currentEpisode.frames().isEmpty()) return null;
        int idx = Math.max(0, Math.min(currentEpisode.frames().size() - 1, frameIndex));
        return currentEpisode.frames().get(idx);
    }

    private int revealedChars(StoryMode.StoryFrame frame) {
        if (frame == null) return 0;
        double elapsedSec = Math.max(0.0, (System.currentTimeMillis() - frameStartedMs) / 1000.0);
        return Math.min(frame.dialogue().length(), (int) Math.floor(elapsedSec * CHARS_PER_SEC));
    }

    private void tickAnimation() {
        if (!isStoryActive()) {
            if (animTimer.isRunning()) animTimer.stop();
            return;
        }
        StoryMode.StoryFrame f = currentFrame();
        if (f != null && revealedChars(f) >= f.dialogue().length()) {
            if (typewriterDoneMs == 0L) typewriterDoneMs = System.currentTimeMillis();
            if (autoPlay && (System.currentTimeMillis() - typewriterDoneMs) >= (long) (AUTO_ADVANCE_PAUSE_SEC * 1000)) {
                if (frameIndex + 1 < currentEpisode.frames().size()) {
                    jumpToFrame(frameIndex + 1);
                } else {
                    autoPlay = false;
                }
            }
        }
        repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        StoryMode.StoryFrame frame = currentFrame();
        if (frame == null || w <= 40 || h <= 40) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        long now = System.currentTimeMillis();
        double totalSec = Math.max(0.0, (now - overlayOpenedMs) / 1000.0);
        double frameSec = Math.max(0.0, (now - frameStartedMs) / 1000.0);
        Color accent = frame.panelAccent() != null ? frame.panelAccent() : UiTokens.ACCENT_SIGNAL;

        drawCoverArt(g2, w, h, frame, frameSec, accent);
        drawComicAtmosphere(g2, 0, 0, w, h, frame.mood(), totalSec, frameSec, accent);
        drawSfxBurstBadge(g2, GUTTER, TOP_STRIP_H + 8, frame.sfxBadge(), accent, frameSec);
        drawDialogueBand(g2, w, h, frame, totalSec);
        drawTopStrip(g2, w);

        // Panel-in flash during the first 0.25s of each frame
        if (frameSec < 0.25) {
            int flashAlpha = (int) Math.round((1.0 - frameSec / 0.25) * 90);
            g2.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, flashAlpha))));
            g2.fillRect(0, 0, w, h);
        }
        g2.dispose();
    }

    /** COVER-scaled art (never letterboxed) with gentle Ken Burns zoom/pan and impact shake. */
    private static void drawCoverArt(Graphics2D g2, int w, int h, StoryMode.StoryFrame frame, double frameSec, Color accent) {
        BufferedImage img = SpriteAssets.getStoryArt(frame.artKey());
        if (img == null) {
            g2.setPaint(new GradientPaint(0, 0, mix(accent, new Color(6, 10, 22), 0.55),
                    0, h, new Color(4, 7, 16)));
            g2.fillRect(0, 0, w, h);
            return;
        }
        double p = Math.min(1.0, frameSec / 9.5);
        double ease = p * (2.0 - p);
        // Small zoom range: 16:9 art is already side-cropped on a ~4:3 window, keep subjects in the safe area.
        double zoom = switch (frame.motion()) {
            case ZOOM_IN -> 1.02 + 0.05 * ease;
            case ZOOM_OUT -> 1.07 - 0.05 * ease;
            case PAN_RIGHT, PAN_LEFT -> 1.04;
            case DRAMATIC_SHAKE -> 1.05;
        };
        double scale = Math.max(w / (double) img.getWidth(), h / (double) img.getHeight()) * zoom;
        double dw = img.getWidth() * scale;
        double dh = img.getHeight() * scale;
        double slackX = (dw - w) / 2.0;
        double slackY = (dh - h) / 2.0;
        double panAmp = Math.min(slackX, w * 0.03);
        double offX = switch (frame.motion()) {
            case PAN_RIGHT -> (1.0 - 2.0 * ease) * panAmp;
            case PAN_LEFT -> (2.0 * ease - 1.0) * panAmp;
            default -> 0.0;
        };
        double offY = 0.0;
        if (frame.motion() == StoryMode.MotionStyle.DRAMATIC_SHAKE && frameSec < 1.1) {
            double amp = (1.1 - frameSec) * 6.0;
            offX += Math.sin(frameSec * 52.0) * amp;
            offY += Math.cos(frameSec * 61.0) * amp * 0.65;
        }
        offX = Math.max(-slackX, Math.min(slackX, offX));
        offY = Math.max(-slackY, Math.min(slackY, offY));
        int dx = (int) Math.floor(-slackX + offX);
        int dy = (int) Math.floor(-slackY + offY);
        g2.drawImage(img, dx, dy, (int) Math.ceil(dw) + 1, (int) Math.ceil(dh) + 1, null);
    }

    private void drawTopStrip(Graphics2D g2, int w) {
        g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 175), 0, TOP_STRIP_H, new Color(0, 0, 0, 0)));
        g2.fillRect(0, 0, w, TOP_STRIP_H);

        // Skip: top-right, large hit area, light text on dark pill with a light border so it reads on any art.
        Font skipFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
        Font hintFont = new Font(Font.SANS_SERIF, Font.BOLD, 10);
        String skipLabel = "Bỏ qua ▸▸";
        String skipHint = "ESC";
        int skipTextW = g2.getFontMetrics(skipFont).stringWidth(skipLabel);
        int hintW = g2.getFontMetrics(hintFont).stringWidth(skipHint) + 10;
        int skipW = Math.max(44, 16 + skipTextW + 8 + hintW + 12);
        skipBtnRect.setBounds(w - GUTTER - skipW, 12, skipW, 38);
        boolean skipHover = "SKIP".equals(hoveredControl);
        drawPill(g2, skipBtnRect, skipHover);
        g2.setFont(skipFont);
        g2.setColor(Color.WHITE);
        int baseY = skipBtnRect.y + (skipBtnRect.height + g2.getFontMetrics().getAscent()) / 2 - 2;
        g2.drawString(skipLabel, skipBtnRect.x + 16, baseY);
        int hx = skipBtnRect.x + 16 + skipTextW + 8;
        g2.setColor(new Color(255, 255, 255, 60));
        g2.fillRoundRect(hx, skipBtnRect.y + 11, hintW, 16, 6, 6);
        g2.setFont(hintFont);
        g2.setColor(new Color(226, 232, 240));
        g2.drawString(skipHint, hx + 5, skipBtnRect.y + 23);

        // Auto-play toggle: smaller pill left of Skip
        String autoLabel = autoPlay ? "Tự chạy: BẬT" : "Tự chạy: TẮT";
        Font autoFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        int autoW = g2.getFontMetrics(autoFont).stringWidth(autoLabel) + 24;
        autoBtnRect.setBounds(skipBtnRect.x - 10 - autoW, skipBtnRect.y + 4, autoW, 30);
        drawPill(g2, autoBtnRect, "AUTO".equals(hoveredControl));
        g2.setFont(autoFont);
        g2.setColor(autoPlay ? new Color(0x6EE7B7) : new Color(203, 213, 225));
        g2.drawString(autoLabel, autoBtnRect.x + 12, autoBtnRect.y + 20);

        // Chapter tag · title, left
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        String label = currentEpisode.chapterTag() + "  ·  " + currentEpisode.title();
        String fitted = fitEllipsis(label, fm, autoBtnRect.x - GUTTER - 16);
        g2.setColor(new Color(0, 0, 0, 140));
        g2.drawString(fitted, GUTTER + 1, 37);
        g2.setColor(new Color(241, 245, 249));
        g2.drawString(fitted, GUTTER, 36);
    }

    private static void drawPill(Graphics2D g2, Rectangle r, boolean hovered) {
        g2.setColor(hovered ? new Color(30, 41, 59, 235) : new Color(8, 12, 24, 205));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, r.height, r.height);
        g2.setColor(hovered ? Color.WHITE : new Color(255, 255, 255, 170));
        g2.setStroke(new BasicStroke(hovered ? 2f : 1.4f));
        g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, r.height, r.height);
    }

    /** Bottom band: fading dark gradient with portrait, speaker, typewriter dialogue, caption and frame dots. */
    private void drawDialogueBand(Graphics2D g2, int w, int h, StoryMode.StoryFrame frame, double totalSec) {
        int bandH = Math.max(240, Math.min(300, (int) (h * 0.32)));
        int bandTop = h - bandH;
        g2.setPaint(new GradientPaint(0, bandTop, new Color(3, 6, 14, 0), 0, bandTop + bandH * 0.35f, new Color(3, 6, 14, 235)));
        g2.fillRect(0, bandTop, w, bandH);

        StoryMode.StorySpeaker speaker = frame.speaker();
        Color spCol = speaker.accent;
        int dotsRowY = h - 22;
        int captionY = dotsRowY - 22;
        // Fixed rows (name, headline, 3 dialogue lines, caption) so the block never jumps between frames.
        int nameY = captionY - 115;

        // Portrait, centred on the text block
        int portraitSz = 104;
        int px = GUTTER;
        int py = nameY - 16 + (131 - portraitSz) / 2;
        int bob = (int) Math.round(Math.sin(totalSec * 3.1) * 2.0);
        g2.setPaint(new RadialGradientPaint(px + portraitSz / 2f, py + portraitSz / 2f, portraitSz * 0.72f,
                new float[]{0f, 1f},
                new Color[]{new Color(spCol.getRed(), spCol.getGreen(), spCol.getBlue(), 90), new Color(0, 0, 0, 0)}));
        g2.fillOval(px - 10, py - 10, portraitSz + 20, portraitSz + 20);
        Shape oldClip = g2.getClip();
        g2.clip(new Ellipse2D.Double(px, py + bob, portraitSz, portraitSz));
        BufferedImage portrait = speaker.portraitKey != null ? SpriteAssets.getStoryArt(speaker.portraitKey) : null;
        if (portrait != null) {
            double sc = Math.max(portraitSz / (double) portrait.getWidth(), portraitSz / (double) portrait.getHeight());
            int pw = (int) Math.round(portrait.getWidth() * sc);
            int ph = (int) Math.round(portrait.getHeight() * sc);
            g2.drawImage(portrait, px + (portraitSz - pw) / 2, py + bob + (portraitSz - ph) / 2, pw, ph, null);
        } else {
            g2.setPaint(new GradientPaint(px, py, new Color(18, 32, 58), px, py + portraitSz, new Color(9, 15, 30)));
            g2.fillRect(px, py + bob, portraitSz, portraitSz);
            BufferedImage emblem = SpriteAssets.getMapProp(speaker == StoryMode.StorySpeaker.TITAN ? "stage_node_boss" : "home_badge_campaign");
            if (emblem != null) {
                g2.drawImage(emblem, px + 14, py + bob + 14, portraitSz - 28, portraitSz - 28, null);
            }
        }
        g2.setClip(oldClip);
        g2.setColor(spCol);
        g2.setStroke(new BasicStroke(2.6f));
        g2.drawOval(px, py + bob, portraitSz, portraitSz);

        int tx = px + portraitSz + 22;
        int tw = Math.max(120, w - tx - GUTTER);
        int y = nameY;

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g2.setColor(spCol);
        g2.drawString(speaker.displayName, tx, y);
        int nameW = g2.getFontMetrics().stringWidth(speaker.displayName);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        g2.setColor(new Color(203, 213, 225));
        g2.drawString(fitEllipsis("  ·  " + speaker.roleTitle, g2.getFontMetrics(), tw - nameW), tx + nameW, y);

        y += 22;
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        g2.setColor(new Color(0xFDE047));
        g2.drawString(fitEllipsis(frame.headline(), g2.getFontMetrics(), tw), tx, y);

        int chars = revealedChars(frame);
        boolean typing = chars < frame.dialogue().length();
        String visibleText = frame.dialogue().substring(0, Math.max(0, chars));
        if (typing && ((System.currentTimeMillis() / 220) % 2 == 0)) visibleText += "▌";
        y += 25;
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        g2.setColor(UiTokens.TEXT_PRIMARY);
        drawWrappedLines(g2, visibleText, tx, y, tw, 22, 3);

        boolean hasCaption = frame.caption() != null && !frame.caption().isBlank();
        if (hasCaption) {
            g2.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 13));
            g2.setColor(new Color(0xFEF08A));
            g2.drawString(fitEllipsis("▍" + frame.caption(), g2.getFontMetrics(), tw), tx, captionY);
        }

        // Frame dots (right) + click hint left of them
        int count = currentEpisode.frames().size();
        int dotGap = 16;
        int dotsX = w - GUTTER - (count - 1) * dotGap - 9;
        for (int i = 0; i < count; i++) {
            int cx = dotsX + i * dotGap;
            if (i == frameIndex) {
                g2.setColor(Color.WHITE);
                g2.fillOval(cx - 1, dotsRowY - 5, 10, 10);
            } else {
                g2.setColor(i < frameIndex ? new Color(255, 255, 255, 150) : new Color(255, 255, 255, 0));
                g2.fillOval(cx, dotsRowY - 4, 8, 8);
                g2.setColor(new Color(255, 255, 255, 170));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(cx, dotsRowY - 4, 8, 8);
            }
        }
        boolean isLast = frameIndex + 1 >= count;
        String hint = typing ? "Bấm để hiện hết chữ"
                : isLast ? (currentEpisode.epilogue() ? "Bấm để kết thúc ★" : "Bấm để vào trận ★")
                : "Bấm để tiếp tục ▸";
        hint += "   ·   SPACE / →   ·   ← lùi";
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        FontMetrics fmHint = g2.getFontMetrics();
        int hintRight = dotsX - 16;
        String fittedHint = fitEllipsis(hint, fmHint, hintRight - tx);
        g2.setColor(new Color(203, 213, 225, 210));
        g2.drawString(fittedHint, hintRight - fmHint.stringWidth(fittedHint), dotsRowY + 4);
    }

    private static Color mix(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() * (1 - t) + b.getRed() * t),
                (int) Math.round(a.getGreen() * (1 - t) + b.getGreen() * t),
                (int) Math.round(a.getBlue() * (1 - t) + b.getBlue() * t));
    }

    private static void drawSfxBurstBadge(
            Graphics2D g2, int bx, int by, String text, Color accent, double frameSec
    ) {
        if (text == null || text.isBlank()) return;
        double pop = Math.min(1.0, frameSec / 0.32);
        double scale = pop < 0.7 ? (0.75 + 0.38 * (pop / 0.7)) : (1.13 - 0.13 * ((pop - 0.7) / 0.3));
        AffineTransform old = g2.getTransform();

        Font f = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        int bw = fm.stringWidth(text) + 28;
        int bh = 28;

        g2.translate(bx + bw * 0.5, by + bh * 0.5);
        g2.rotate(-0.045);
        g2.scale(scale, scale);

        // Jagged comic tag polygon
        int hw = bw / 2;
        int hh = bh / 2;
        Polygon poly = new Polygon(
                new int[]{-hw - 5, -hw + 6, 0, hw - 4, hw + 6, hw - 3, 0, -hw + 4},
                new int[]{-2, -hh - 3, -hh + 1, -hh - 3, 0, hh + 3, hh - 1, hh + 3},
                8);
        g2.setColor(new Color(6, 10, 18, 220));
        g2.translate(3, 3);
        g2.fillPolygon(poly);
        g2.translate(-3, -3);

        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 238));
        g2.fillPolygon(poly);
        g2.setColor(new Color(15, 23, 42));
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawPolygon(poly);

        g2.setColor(new Color(15, 23, 42));
        g2.drawString(text, -fm.stringWidth(text) / 2, 5);
        g2.setTransform(old);
    }

    private static void drawComicAtmosphere(
            Graphics2D g2,
            int x, int y, int w, int h,
            StoryMode.ParticleMood mood,
            double totalSec,
            double frameSec,
            Color accent
    ) {
        // Manga action speed lines around border edges for dramatic moments
        if (mood == StoryMode.ParticleMood.ATOMIC_SPARKS || mood == StoryMode.ParticleMood.STORM_LIGHTNING
                || mood == StoryMode.ParticleMood.LAVA_EMBERS) {
            g2.setColor(new Color(255, 255, 255, 38));
            g2.setStroke(new BasicStroke(1.3f));
            double cx = x + w * 0.5;
            double cy = y + h * 0.48;
            for (int i = 0; i < 28; i++) {
                double ang = (i / 28.0) * Math.PI * 2.0 + Math.sin(totalSec * 1.8 + i) * 0.03;
                double r0 = Math.min(w, h) * (0.44 + 0.06 * Math.sin(totalSec * 5.0 + i * 1.7));
                double r1 = Math.max(w, h) * 0.72;
                g2.drawLine(
                        (int) (cx + Math.cos(ang) * r0), (int) (cy + Math.sin(ang) * r0),
                        (int) (cx + Math.cos(ang) * r1), (int) (cy + Math.sin(ang) * r1));
            }
        }

        // Animated floating particles tailored to chapter mood
        int count = (mood == StoryMode.ParticleMood.RAIN_TEARS) ? 32 : 22;
        for (int i = 0; i < count; i++) {
            double speed = switch (mood) {
                case RAIN_TEARS -> 1.15;
                case LAVA_EMBERS -> 0.38;
                case MEMORY_MOTES -> 0.14;
                default -> 0.22;
            };
            double phase = (i * 0.37 + totalSec * speed) % 1.0;
            double px = x + ((i * 97 + (mood == StoryMode.ParticleMood.RAIN_TEARS ? -phase * 48.0 : Math.sin(totalSec * 1.6 + i) * 26.0)) % Math.max(1, w));
            if (px < x) px += w;
            double py = switch (mood) {
                case LAVA_EMBERS, ATLANTIS_BUBBLES, ATOMIC_SPARKS, MEMORY_MOTES -> y + h - phase * h;
                case SAKURA_MAGIC, GOLDEN_DAWN, RAIN_TEARS -> y + phase * h;
                default -> y + ((i * 61 + Math.cos(totalSec * 1.3 + i * 2) * 18.0) % Math.max(1, h));
            };
            if (py < y) py += h;

            float alpha = (float) Math.max(0.15, Math.min(0.85, Math.sin(phase * Math.PI) * 0.85));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
            switch (mood) {
                case SAKURA_MAGIC -> {
                    g2.setColor(new Color(0xF9A8D4));
                    g2.fillOval((int) px, (int) py, 7, 4);
                }
                case ZOMBIE_SPORE -> {
                    g2.setColor(new Color(0x4ADE80));
                    g2.fillOval((int) px, (int) py, 6, 6);
                }
                case LAVA_EMBERS -> {
                    g2.setColor(i % 2 == 0 ? new Color(0xFB923C) : new Color(0xFDE047));
                    g2.fillOval((int) px, (int) py, 5, 5);
                }
                case RAIN_TEARS -> {
                    g2.setColor(i % 4 == 0 ? new Color(0xBAE6FD) : new Color(0x94A3B8));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawLine((int) px, (int) py, (int) px - 5, (int) py + 14);
                }
                case MEMORY_MOTES -> {
                    g2.setColor(i % 2 == 0 ? new Color(0xFDE68A) : new Color(0x5EEAD4));
                    int sz = (i % 3 == 0) ? 6 : 4;
                    g2.fillOval((int) px - sz / 2, (int) py - sz / 2, sz, sz);
                }
                case GALAXY_STARS, GOLDEN_DAWN -> {
                    g2.setColor(mood == StoryMode.ParticleMood.GOLDEN_DAWN ? new Color(0xFDE68A) : new Color(0xE9D5FF));
                    int sz = (i % 3 == 0) ? 4 : 2;
                    g2.fillOval((int) px, (int) py, sz, sz);
                }
                case ATLANTIS_BUBBLES -> {
                    g2.setColor(new Color(0x5EEAD4));
                    g2.drawOval((int) px, (int) py, 7, 7);
                }
                default -> {
                    g2.setColor(accent);
                    g2.fillOval((int) px, (int) py, 4, 4);
                }
            }
            g2.setComposite(old);
        }

        // Occasional stylized lightning bolt silhouette in storm frames
        if (mood == StoryMode.ParticleMood.STORM_LIGHTNING && Math.sin(totalSec * 4.4) > 0.86) {
            int lx = x + (int) (w * (0.2 + 0.6 * ((int) (totalSec * 3) % 5) / 5.0));
            g2.setColor(new Color(253, 224, 71, 140));
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawPolyline(
                    new int[]{lx, lx - 14, lx + 8, lx - 10},
                    new int[]{y + 8, y + h / 3, y + h / 2, y + h - 42},
                    4);
        }
    }

    private static void drawWrappedLines(
            Graphics2D g2, String text, int x, int startY, int maxWidth, int lineHeight, int maxLines
    ) {
        if (text == null || text.isEmpty()) return;
        FontMetrics fm = g2.getFontMetrics();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int y = startY;
        int linesDrawn = 0;

        for (String w : words) {
            String candidate = line.isEmpty() ? w : (line + " " + w);
            if (fm.stringWidth(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (linesDrawn >= maxLines - 1) {
                    g2.drawString(fitEllipsis(line + " " + w, fm, maxWidth), x, y);
                    return;
                }
                g2.drawString(line.toString(), x, y);
                linesDrawn++;
                y += lineHeight;
                line.setLength(0);
                line.append(w);
            }
        }
        if (!line.isEmpty() && linesDrawn < maxLines) {
            g2.drawString(line.toString(), x, y);
        }
    }

    private static String fitEllipsis(String text, FontMetrics fm, int maxWidth) {
        if (text == null || maxWidth <= 12) return "";
        if (fm.stringWidth(text) <= maxWidth) return text;
        int end = text.length();
        while (end > 1 && fm.stringWidth(text.substring(0, end) + "…") > maxWidth) {
            end--;
        }
        return text.substring(0, Math.max(0, end)) + "…";
    }
}
