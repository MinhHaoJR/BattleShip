package battleship.ui;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
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
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen interactive anime/comic storyboard cutscene player ("Tranh Hoạt Hình Cốt Truyện").
 * Renders 16:9 comic art frames with Ken Burns camera motion, comic-book inked borders,
 * atmospheric animated particles, bouncing SFX starburst stamps, expressive crew portraits,
 * typewriter dialogue bubbles, mini filmstrip thumbnails, and Prev / Next / Auto / Skip controls.
 */
public final class StoryComicOverlay extends JPanel {
    private static final double CHARS_PER_SEC = 58.0;
    private static final double AUTO_ADVANCE_PAUSE_SEC = 2.6;

    private final Timer animTimer;
    private final List<Rectangle> thumbRects = new ArrayList<>();
    private final Rectangle prevBtnRect = new Rectangle();
    private final Rectangle nextBtnRect = new Rectangle();
    private final Rectangle autoBtnRect = new Rectangle();
    private final Rectangle skipBtnRect = new Rectangle();
    private final Rectangle comicStageRect = new Rectangle();
    private final Rectangle dialogueBoxRect = new Rectangle();

    private StoryMode.StoryEpisode currentEpisode;
    private Runnable onFinishedCallback;
    private int frameIndex = 0;
    private long overlayOpenedMs;
    private long frameStartedMs;
    private long typewriterDoneMs;
    private boolean autoPlay = false;
    private int hoveredThumb = -1;
    private String hoveredControl = null;

    public StoryComicOverlay() {
        setOpaque(false);
        setVisible(false);
        setFocusable(true);
        setBorder(BorderFactory.createEmptyBorder());

        animTimer = new Timer(33, e -> tickAnimation());

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!isVisible() || currentEpisode == null) return;
                requestFocusInWindow();
                int mx = e.getX();
                int my = e.getY();

                if (skipBtnRect.contains(mx, my)) {
                    SoundEngine.playUiClick();
                    skipCutscene();
                    return;
                }
                if (autoBtnRect.contains(mx, my)) {
                    SoundEngine.playUiClick();
                    autoPlay = !autoPlay;
                    typewriterDoneMs = System.currentTimeMillis();
                    repaint();
                    return;
                }
                if (prevBtnRect.contains(mx, my)) {
                    SoundEngine.playUiClick();
                    prevFrame();
                    return;
                }
                if (nextBtnRect.contains(mx, my)) {
                    SoundEngine.playUiClick();
                    advanceOrComplete();
                    return;
                }
                for (int i = 0; i < thumbRects.size(); i++) {
                    if (thumbRects.get(i).contains(mx, my)) {
                        SoundEngine.playUiClick();
                        jumpToFrame(i);
                        return;
                    }
                }
                if (comicStageRect.contains(mx, my) || dialogueBoxRect.contains(mx, my)) {
                    SoundEngine.playUiClick();
                    advanceOrComplete();
                }
            }

            @Override public void mouseMoved(MouseEvent e) {
                if (!isVisible()) return;
                int mx = e.getX();
                int my = e.getY();
                int newThumb = -1;
                for (int i = 0; i < thumbRects.size(); i++) {
                    if (thumbRects.get(i).contains(mx, my)) {
                        newThumb = i;
                        break;
                    }
                }
                String newCtrl = null;
                if (skipBtnRect.contains(mx, my)) newCtrl = "SKIP";
                else if (autoBtnRect.contains(mx, my)) newCtrl = "AUTO";
                else if (prevBtnRect.contains(mx, my)) newCtrl = "PREV";
                else if (nextBtnRect.contains(mx, my)) newCtrl = "NEXT";
                else if (dialogueBoxRect.contains(mx, my) || comicStageRect.contains(mx, my)) newCtrl = "ADVANCE";

                if (newThumb != hoveredThumb || !java.util.Objects.equals(newCtrl, hoveredControl)) {
                    hoveredThumb = newThumb;
                    hoveredControl = newCtrl;
                    setCursor(Cursor.getPredefinedCursor(
                            (newThumb >= 0 || newCtrl != null) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        installKeyBindings();
    }

    private void installKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "story_next");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "story_next");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "story_next");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "story_prev");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "story_skip");

        am.put("story_next", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (isVisible()) {
                    SoundEngine.playUiClick();
                    advanceOrComplete();
                }
            }
        });
        am.put("story_prev", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (isVisible()) {
                    SoundEngine.playUiClick();
                    prevFrame();
                }
            }
        });
        am.put("story_skip", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (isVisible()) {
                    SoundEngine.playUiClick();
                    skipCutscene();
                }
            }
        });
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
        this.hoveredThumb = -1;
        this.hoveredControl = null;
        setVisible(true);
        if (!animTimer.isRunning()) animTimer.start();
        requestFocusInWindow();
        revalidate();
        repaint();
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
        int clamped = Math.max(0, Math.min(currentEpisode.frames().size() - 1, index));
        this.frameIndex = clamped;
        this.frameStartedMs = System.currentTimeMillis();
        this.typewriterDoneMs = 0L;
        repaint();
    }

    public void prevFrame() {
        if (currentEpisode == null) return;
        if (frameIndex > 0) {
            jumpToFrame(frameIndex - 1);
        }
    }

    public void advanceOrComplete() {
        if (currentEpisode == null) {
            closeAndFinish();
            return;
        }
        StoryMode.StoryFrame f = currentFrame();
        if (f != null) {
            int revealed = revealedChars(f);
            if (revealed < f.dialogue().length()) {
                // First tap completes typewriter line immediately
                frameStartedMs = System.currentTimeMillis() - (long) ((f.dialogue().length() + 4) * 1000.0 / CHARS_PER_SEC);
                typewriterDoneMs = System.currentTimeMillis();
                repaint();
                return;
            }
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
        repaint();
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
        if (!isVisible() || currentEpisode == null) {
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
        if (!isVisible() || currentEpisode == null) return;
        StoryMode.StoryFrame frame = currentFrame();
        if (frame == null) return;

        int w = getWidth();
        int h = getHeight();
        if (w <= 40 || h <= 40) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        long now = System.currentTimeMillis();
        double totalSec = Math.max(0.0, (now - overlayOpenedMs) / 1000.0);
        double frameSec = Math.max(0.0, (now - frameStartedMs) / 1000.0);
        Color accent = frame.panelAccent() != null ? frame.panelAccent() : UiTokens.ACCENT_SIGNAL;

        // 1. Dark cinema backdrop with soft halftone vignette
        g2.setColor(new Color(4, 9, 20, 232));
        g2.fillRect(0, 0, w, h);
        g2.setPaint(new RadialGradientPaint(w * 0.5f, h * 0.44f, Math.max(220f, Math.max(w, h) * 0.58f),
                new float[]{0f, 1f},
                new Color[]{new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 48), new Color(2, 6, 16, 210)}));
        g2.fillRect(0, 0, w, h);

        // 2. Centered Comic Theater Card (maximizes stage room for full uncropped illustration)
        int padX = Math.max(14, (w - Math.min(w - 28, 1080)) / 2);
        int padY = Math.max(8, (h - Math.min(h - 16, 800)) / 2);
        int cardW = w - padX * 2;
        int cardH = h - padY * 2;

        g2.setColor(new Color(10, 17, 32, 246));
        g2.fillRoundRect(padX, padY, cardW, cardH, 18, 18);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 165));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(padX, padY, cardW - 1, cardH - 1, 18, 18);

        int innerX = padX + 16;
        int innerW = cardW - 32;

        // 3. Header Row: Episode Tag + Title + Logline + Controls (clean vertical clearance)
        int headY = padY + 10;
        int headH = 52;
        drawHeaderRow(g2, innerX, headY, innerW, headH, frame, accent);

        // 4. Mini Comic Filmstrip Row (distinct 16:9 mini scenes + labels, zero overlap)
        int stripY = headY + headH + 4;
        int stripH = (cardH < 590) ? 40 : 46;
        drawFilmstripRow(g2, innerX, stripY, innerW, stripH, accent);

        // 5. Dialogue Deck (bottom) - responsive so compact viewports preserve tall illustration stage
        int deckH = (cardH < 590) ? 132 : Math.min(168, Math.max(146, (int) (cardH * 0.23)));
        int deckY = padY + cardH - 10 - deckH;

        // 6. Main Animated Comic Illustration Panel (fits 100% full image cleanly!)
        int stageY = stripY + stripH + 6;
        int stageH = Math.max(160, deckY - stageY - 8);
        comicStageRect.setBounds(innerX, stageY, innerW, stageH);
        drawAnimatedComicPanel(g2, innerX, stageY, innerW, stageH, frame, totalSec, frameSec, accent);

        // 7. Dialogue Box & Navigation Controls
        dialogueBoxRect.setBounds(innerX, deckY, innerW, deckH);
        drawDialogueTheaterDeck(g2, innerX, deckY, innerW, deckH, frame, totalSec, accent);

        g2.dispose();
    }

    private void drawHeaderRow(
            Graphics2D g2, int x, int y, int w, int h, StoryMode.StoryFrame frame, Color accent
    ) {
        // Chapter pill
        String tag = currentEpisode.chapterTag();
        Font tagFont = new Font(Font.SANS_SERIF, Font.BOLD, 11);
        g2.setFont(tagFont);
        FontMetrics fmTag = g2.getFontMetrics();
        int pillW = fmTag.stringWidth(tag) + 20;
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 42));
        g2.fillRoundRect(x, y + 2, pillW, 24, 12, 12);
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(1.3f));
        g2.drawRoundRect(x, y + 2, pillW, 24, 12, 12);
        g2.drawString(tag, x + 10, y + 18);

        // Episode Title
        int titleX = x + pillW + 10;
        int rightCtrlW = 224;
        int titleMaxW = Math.max(120, w - (titleX - x) - rightCtrlW);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g2.setColor(UiTokens.TEXT_PRIMARY);
        String title = fitEllipsis(currentEpisode.title(), g2.getFontMetrics(), titleMaxW);
        g2.drawString(title, titleX, y + 20);

        // Subtle Subtitle logline with comfortable vertical spacing above filmstrip
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g2.setColor(UiTokens.TEXT_MUTED);
        String sub = fitEllipsis(currentEpisode.logline(), g2.getFontMetrics(), Math.max(120, w - rightCtrlW - 12));
        g2.drawString(sub, x, y + 43);

        // Top-right Controls: [Tự Chạy] + [Bỏ Qua (ESC)]
        int skipW = 104;
        int autoW = 104;
        skipBtnRect.setBounds(x + w - skipW, y + 1, skipW, 27);
        autoBtnRect.setBounds(skipBtnRect.x - 8 - autoW, y + 1, autoW, 27);

        drawPillControl(g2, autoBtnRect, autoPlay ? "Tự Chạy: BẬT" : "Tự Chạy: TẮT",
                autoPlay ? new Color(0x34D399) : new Color(0x94A3B8),
                autoPlay, "AUTO".equals(hoveredControl));
        drawPillControl(g2, skipBtnRect, "Bỏ Qua (ESC)", new Color(0xF87171),
                false, "SKIP".equals(hoveredControl));
    }

    private void drawFilmstripRow(Graphics2D g2, int x, int y, int w, int h, Color accent) {
        thumbRects.clear();
        List<StoryMode.StoryFrame> frames = currentEpisode.frames();
        int count = frames.size();
        int gap = 8;
        int thumbW = Math.min(220, Math.max(96, (w - gap * Math.max(0, count - 1)) / Math.max(1, count)));
        int totalW = count * thumbW + Math.max(0, count - 1) * gap;
        int startX = x + Math.max(0, (w - totalW) / 2);

        for (int i = 0; i < count; i++) {
            int tx = startX + i * (thumbW + gap);
            Rectangle tr = new Rectangle(tx, y + 1, thumbW, h - 2);
            thumbRects.add(tr);

            StoryMode.StoryFrame tf = frames.get(i);
            boolean active = (i == frameIndex);
            boolean visited = (i < frameIndex);
            boolean hover = (i == hoveredThumb);

            Shape oldClip = g2.getClip();
            g2.clip(new RoundRectangle2D.Float(tr.x, tr.y, tr.width, tr.height, 8, 8));
            g2.setColor(active ? new Color(18, 32, 58) : (visited ? new Color(13, 22, 40) : new Color(10, 16, 30)));
            g2.fillRect(tr.x, tr.y, tr.width, tr.height);

            // Clean uncropped 16:9 mini preview on left side of thumbnail card
            int previewH = tr.height - 6;
            int previewW = Math.min(tr.width / 2, (int) Math.round(previewH * (16.0 / 9.0)));
            int px = tr.x + 3;
            int py = tr.y + 3;
            BufferedImage img = SpriteAssets.getStoryArt(tf.artKey());
            if (img != null && previewW > 12 && previewH > 8) {
                g2.drawImage(img, px, py, previewW, previewH, null);
                g2.setColor(new Color(4, 10, 22, active ? 15 : 85));
                g2.fillRect(px, py, previewW, previewH);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), active ? 210 : 90));
                g2.drawRect(px, py, previewW - 1, previewH - 1);
            }

            // Right text block: crisp KHUNG i/N badge + speaker name
            int textX = (img != null && previewW > 12) ? (px + previewW + 8) : (tr.x + 8);
            int textMaxW = Math.max(24, tr.x + tr.width - textX - 6);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            String badge = "KHUNG " + (i + 1) + "/" + count;
            g2.setColor(active ? new Color(0xFDE047) : Color.WHITE);
            g2.drawString(fitEllipsis(badge, g2.getFontMetrics(), textMaxW), textX, tr.y + 15);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g2.setColor(new Color(203, 213, 225, active ? 255 : 210));
            String spName = fitEllipsis(tf.speaker().displayName, g2.getFontMetrics(), textMaxW);
            g2.drawString(spName, textX, tr.y + tr.height - 7);
            g2.setClip(oldClip);

            Color borderCol = active ? accent : (hover ? Color.WHITE : new Color(71, 85, 105, 190));
            g2.setColor(borderCol);
            g2.setStroke(new BasicStroke(active ? 2.2f : 1.1f));
            g2.drawRoundRect(tr.x, tr.y, tr.width - 1, tr.height - 1, 8, 8);
        }
    }

    private void drawAnimatedComicPanel(
            Graphics2D g2,
            int x, int y, int w, int h,
            StoryMode.StoryFrame frame,
            double totalSec,
            double frameSec,
            Color accent
    ) {
        // Outer white/cream comic book panel matte with crisp black comic ink outline
        g2.setColor(new Color(241, 245, 249, 235));
        g2.fillRoundRect(x - 3, y - 3, w + 6, h + 6, 14, 14);
        g2.setColor(new Color(6, 10, 20));
        g2.fillRoundRect(x, y, w, h, 10, 10);

        Shape oldClip = g2.getClip();
        g2.clip(new RoundRectangle2D.Float(x + 2, y + 2, w - 4, h - 4, 8, 8));

        // Reserve dedicated bottom ticker band so narration strip NEVER obscures characters in artwork
        boolean hasCaption = frame.caption() != null && !frame.caption().isBlank();
        int capBarH = hasCaption ? 30 : 0;
        int artX = x + 4;
        int artY = y + 4;
        int artW = Math.max(40, w - 8);
        int artH = Math.max(80, h - 8 - capBarH);

        // Subtle camera tremor on dramatic impact frames (clamped tiny offset inside matte)
        double shakeX = 0.0;
        double shakeY = 0.0;
        if (frame.motion() == StoryMode.MotionStyle.DRAMATIC_SHAKE && frameSec < 1.1) {
            double amp = (1.1 - frameSec) * 4.5;
            shakeX = Math.sin(frameSec * 52.0) * amp;
            shakeY = Math.cos(frameSec * 61.0) * amp * 0.65;
        }

        // Subtle breathing motion inside padded bounds (0% of artwork edges cropped off!)
        double p = Math.min(1.0, frameSec / 9.5);
        double ease = p * (2.0 - p);
        double breathe = switch (frame.motion()) {
            case ZOOM_IN -> 0.968 + 0.032 * ease;
            case ZOOM_OUT -> 1.0 - 0.028 * ease;
            case PAN_RIGHT, PAN_LEFT -> 0.985 + 0.015 * Math.sin(totalSec * 0.7);
            case DRAMATIC_SHAKE -> 0.992;
        };
        double driftX = switch (frame.motion()) {
            case PAN_RIGHT -> (0.5 - ease) * 8.0;
            case PAN_LEFT -> (ease - 0.5) * 8.0;
            default -> 0.0;
        };

        BufferedImage img = SpriteAssets.getStoryArt(frame.artKey());
        int fittedX = artX;
        int fittedY = artY;
        int fittedW = artW;
        int fittedH = artH;
        if (img != null) {
            // 1. Soft darkened atmospheric mirror backdrop filling left/right cinema theater wings
            double wingCover = Math.max(w / (double) img.getWidth(), artH / (double) img.getHeight());
            int cw = (int) Math.round(img.getWidth() * wingCover);
            int ch = (int) Math.round(img.getHeight() * wingCover);
            Composite prevComp = g2.getComposite();
            g2.setComposite(AlphaComposite.SrcOver.derive(0.20f));
            g2.drawImage(img, x + (w - cw) / 2, artY + (artH - ch) / 2, cw, ch, null);
            g2.setComposite(prevComp);
            g2.setPaint(new RadialGradientPaint(
                    x + w * 0.5f, artY + artH * 0.5f, Math.max(160f, w * 0.55f),
                    new float[]{0f, 1f},
                    new Color[]{new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 26), new Color(3, 7, 16, 205)}));
            g2.fillRect(x, y, w, h);

            // 2. Full uncropped illustration centered cleanly via CONTAIN scale (Math.min)
            double fullScale = Math.min(artW / (double) img.getWidth(), artH / (double) img.getHeight()) * breathe;
            int dw = Math.max(16, (int) Math.round(img.getWidth() * fullScale));
            int dh = Math.max(16, (int) Math.round(img.getHeight() * fullScale));
            int dx = artX + (artW - dw) / 2 + (int) Math.round(driftX + shakeX);
            int dy = artY + (artH - dh) / 2 + (int) Math.round(shakeY);
            fittedX = dx;
            fittedY = dy;
            fittedW = dw;
            fittedH = dh;

            // Theater matte drop shadow + full artwork + crisp thin rim around illustration
            g2.setColor(new Color(2, 6, 14, 195));
            g2.fillRoundRect(dx - 3, dy - 2, dw + 6, dh + 6, 8, 8);
            g2.drawImage(img, dx, dy, dw, dh, null);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 150));
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawRect(dx, dy, dw - 1, dh - 1);
        } else {
            g2.setPaint(new GradientPaint(x, y, new Color(16, 40, 74), x, y + h, new Color(7, 15, 32)));
            g2.fillRect(x, y, w, h);
        }

        // Animated atmospheric mood particles + manga speed lines
        drawComicAtmosphere(g2, x, artY, w, artH, frame.mood(), totalSec, frameSec, accent);

        // Top-Left Comic SFX Starburst Stamp placed in left cinema wing when space exists
        int wingSpace = fittedX - x;
        int sfxX = wingSpace >= 132 ? (x + 12) : (x + 10);
        int sfxY = artY + 8;
        drawSfxBurstBadge(g2, sfxX, sfxY, frame.sfxBadge(), accent, frameSec);

        // Dedicated Bottom Yellow Comic Narration Ticker Strip below the artwork (0% character overlap!)
        if (hasCaption) {
            int barTop = y + h - capBarH;
            g2.setColor(new Color(8, 13, 24, 235));
            g2.fillRect(x, barTop - 2, w, capBarH + 4);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110));
            g2.drawLine(x, barTop - 2, x + w, barTop - 2);

            Font capFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
            g2.setFont(capFont);
            FontMetrics fmCap = g2.getFontMetrics();
            String cap = fitEllipsis(frame.caption(), fmCap, w - 42);
            int boxW = Math.min(w - 24, fmCap.stringWidth(cap) + 26);
            int boxH = 24;
            int bx = x + Math.max(10, (w - boxW) / 2);
            int by = barTop + 3;

            g2.setColor(new Color(6, 10, 18, 220));
            g2.fillRect(bx + 2, by + 2, boxW, boxH);
            g2.setColor(new Color(0xFEF08A));
            g2.fillRect(bx, by, boxW, boxH);
            g2.setColor(new Color(15, 23, 42));
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawRect(bx, by, boxW, boxH);
            g2.drawString(cap, bx + 13, by + 16);
        }

        // Subtle panel-in flash fade during first 0.25s
        if (frameSec < 0.25) {
            int flashAlpha = (int) Math.round((1.0 - frameSec / 0.25) * 115);
            g2.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, flashAlpha))));
            g2.fillRect(x, y, w, h);
        }

        g2.setClip(oldClip);
        g2.setColor(new Color(15, 23, 42));
        g2.setStroke(new BasicStroke(2.6f));
        g2.drawRoundRect(x, y, w, h, 10, 10);
    }

    private void drawSfxBurstBadge(
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

    private void drawComicAtmosphere(
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

    private void drawDialogueTheaterDeck(
            Graphics2D g2, int x, int y, int w, int h, StoryMode.StoryFrame frame, double totalSec, Color accent
    ) {
        StoryMode.StorySpeaker speaker = frame.speaker();
        Color spCol = speaker.accent;
        boolean compact = h < 142;

        // Character Portrait Pedestal (Left, aspect-preserving cover fit inside circle)
        int portraitSz = Math.min(112, h - 14);
        int px = x + 6;
        int py = y + (h - portraitSz) / 2;
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

        // Comic Speech Bubble (Middle -> Right)
        int bubbleX = px + portraitSz + 18;
        int bubbleW = Math.max(240, w - (bubbleX - x));
        int navH = compact ? 29 : 32;
        int bubbleH = h - (navH + 10);
        int bubbleY = y + 2;

        // Bubble shadow + main crisp dark-navy comic panel box
        g2.setColor(new Color(4, 8, 18, 220));
        g2.fillRoundRect(bubbleX + 4, bubbleY + 4, bubbleW, bubbleH, 14, 14);
        g2.setPaint(new GradientPaint(bubbleX, bubbleY, new Color(17, 29, 52, 248),
                bubbleX, bubbleY + bubbleH, new Color(11, 20, 38, 248)));
        g2.fillRoundRect(bubbleX, bubbleY, bubbleW, bubbleH, 14, 14);

        // Speech tail pointing to character portrait
        Polygon tail = new Polygon(
                new int[]{bubbleX + 2, bubbleX - 14, bubbleX + 2},
                new int[]{bubbleY + 24, bubbleY + 35, bubbleY + 45},
                3);
        g2.fillPolygon(tail);

        g2.setColor(spCol);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(bubbleX, bubbleY, bubbleW, bubbleH, 14, 14);

        // Speaker Name + Role Badge + Headline inside Speech Bubble
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        g2.setColor(spCol);
        String speakerLine = speaker.displayName;
        int headBaseY = bubbleY + (compact ? 18 : 21);
        g2.drawString(speakerLine, bubbleX + 14, headBaseY);
        int nameW = g2.getFontMetrics().stringWidth(speakerLine);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g2.setColor(UiTokens.TEXT_MUTED);
        g2.drawString(fitEllipsis(" · " + speaker.roleTitle, g2.getFontMetrics(), Math.max(60, bubbleW - 32 - nameW)),
                bubbleX + 16 + nameW, headBaseY);

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g2.setColor(new Color(0xFDE047));
        int topicBaseY = bubbleY + (compact ? 35 : 40);
        g2.drawString(fitEllipsis(frame.headline(), g2.getFontMetrics(), bubbleW - 28), bubbleX + 14, topicBaseY);

        // Wrapped Dialogue Text (Typewriter animation!)
        int chars = revealedChars(frame);
        String visibleText = frame.dialogue().substring(0, Math.max(0, Math.min(frame.dialogue().length(), chars)));
        if (chars < frame.dialogue().length() && ((System.currentTimeMillis() / 220) % 2 == 0)) {
            visibleText += "▌";
        }
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, compact ? 13 : 14));
        g2.setColor(UiTokens.TEXT_PRIMARY);
        int bodyStartY = bubbleY + (compact ? 53 : 60);
        int lineH = compact ? 16 : 19;
        drawWrappedLines(g2, visibleText, bubbleX + 14, bodyStartY, bubbleW - 28, lineH, 3);

        // Navigation Footer Row under Speech Bubble: [← Khung Trước]   hint   [Khung Tiếp → / VÀO TRẬN!]
        int navY = bubbleY + bubbleH + 7;
        int btnW = 136;
        prevBtnRect.setBounds(bubbleX, navY, btnW, navH);
        nextBtnRect.setBounds(bubbleX + bubbleW - 188, navY, 188, navH);

        drawPillControl(g2, prevBtnRect, "← Khung Trước", frameIndex > 0 ? UiTokens.TEXT_PRIMARY : UiTokens.TEXT_MUTED,
                false, frameIndex > 0 && "PREV".equals(hoveredControl));

        boolean isLast = (frameIndex + 1 >= currentEpisode.frames().size());
        String nextText = (chars < frame.dialogue().length())
                ? "Hiện Hết Chữ ▸"
                : (isLast ? (currentEpisode.epilogue() ? "Hoàn Tất Kết Truyện ★" : "Vào Chiến Đấu ★")
                : ("Khung Tiếp (" + (frameIndex + 2) + "/" + currentEpisode.frames().size() + ") →"));
        drawPillControl(g2, nextBtnRect, nextText, isLast ? new Color(0xFACC15) : UiTokens.ACCENT_SIGNAL,
                true, "NEXT".equals(hoveredControl));

        // Center hint
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g2.setColor(UiTokens.TEXT_MUTED);
        String hint = "Bấm khung hình hoặc phím SPACE / → để tiếp tục";
        FontMetrics fmHint = g2.getFontMetrics();
        int hintSpace = nextBtnRect.x - (prevBtnRect.x + prevBtnRect.width);
        if (hintSpace > fmHint.stringWidth(hint) + 20) {
            int hx = prevBtnRect.x + prevBtnRect.width + (hintSpace - fmHint.stringWidth(hint)) / 2;
            g2.drawString(hint, hx, navY + 20);
        }
    }

    private static void drawPillControl(
            Graphics2D g2, Rectangle r, String text, Color accent, boolean primary, boolean hovered
    ) {
        int alphaBg = primary ? (hovered ? 230 : 195) : (hovered ? 120 : 72);
        g2.setColor(primary
                ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alphaBg)
                : new Color(24, 38, 66, alphaBg));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g2.setColor(hovered ? Color.WHITE : accent);
        g2.setStroke(new BasicStroke(hovered ? 1.8f : 1.2f));
        g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 10, 10);

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(primary ? new Color(8, 16, 32) : accent);
        int tx = r.x + (r.width - fm.stringWidth(text)) / 2;
        int ty = r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, tx, ty);
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

        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            String candidate = line.isEmpty() ? w : (line + " " + w);
            if (fm.stringWidth(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (linesDrawn >= maxLines - 1) {
                    String rem = fitEllipsis(line + " " + w, fm, maxWidth);
                    g2.drawString(rem, x, y);
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
