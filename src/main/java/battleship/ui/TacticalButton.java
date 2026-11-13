package battleship.ui;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/** Flat, look-and-feel independent button for tactical screen actions. */
public class TacticalButton extends JButton {
    private final boolean primary;
    private double progressRatio = -1.0;
    private Color progressColor = UiTokens.SHIELD_TINT;
    private Color outlineOverride;

    public TacticalButton(String text, boolean primary) {
        super(text);
        this.primary = primary;
        setFont(UiTokens.FONT_BUTTON);
        setMargin(new Insets(0, UiTokens.BUTTON_PADDING_X, 0, UiTokens.BUTTON_PADDING_X));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension preferred = getPreferredSize();
        setPreferredSize(new Dimension(preferred.width + UiTokens.BUTTON_PADDING_X, UiTokens.BUTTON_HEIGHT));
        setMinimumSize(new Dimension(preferred.width / 2, UiTokens.BUTTON_HEIGHT));
    }

    /** Configures an embedded energy progress bar along the bottom edge of the button (ratio < 0 disables it). */
    public void setEnergyProgress(double ratio, Color barColor, Color borderColor) {
        this.progressRatio = ratio < 0.0 ? -1.0 : Math.max(0.0, Math.min(1.0, ratio));
        if (barColor != null) this.progressColor = barColor;
        this.outlineOverride = borderColor;
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int inset = UiTokens.SPACE_XS;
        int arc = UiTokens.BORDER_RADIUS;
        Color background;
        Color foreground;
        if (!isEnabled()) {
            background = UiTokens.BTN_DISABLED_BG;
            foreground = UiTokens.BTN_DISABLED_FG;
        } else if (getModel().isPressed()) {
            background = primary ? UiTokens.BTN_PRIMARY_PRESSED : UiTokens.BG_BOARD_EMPTY;
            foreground = primary ? UiTokens.BTN_PRIMARY_FG : UiTokens.TEXT_PRIMARY;
        } else if (getModel().isRollover()) {
            background = primary ? UiTokens.BTN_PRIMARY_HOVER : UiTokens.BTN_SECONDARY_HOVER;
            foreground = primary ? UiTokens.BTN_PRIMARY_FG : UiTokens.TEXT_PRIMARY;
        } else {
            background = primary ? UiTokens.BTN_PRIMARY_BG : UiTokens.BTN_SECONDARY_BG;
            foreground = primary ? UiTokens.BTN_PRIMARY_FG : UiTokens.TEXT_PRIMARY;
        }
        int w = getWidth() - inset * 2;
        int h = getHeight() - inset * 2;
        Color topColor = isEnabled() ? blend(background, Color.WHITE, primary ? 0.14f : 0.08f) : background;
        Color botColor = isEnabled() ? blend(background, Color.BLACK, 0.12f) : background;
        g.setPaint(new GradientPaint(0, inset, topColor, 0, inset + h, botColor));
        g.fillRoundRect(inset, inset, w, h, arc, arc);
        if (progressRatio >= 0.0 && w > 12) {
            int barX = inset + 5;
            int barW = w - 10;
            int barH = 3;
            int barY = inset + h - barH - 2;
            g.setColor(new Color(10, 18, 32, 220));
            g.fillRoundRect(barX, barY, barW, barH, 3, 3);
            int fillW = (int) Math.round(barW * progressRatio);
            if (fillW > 0) {
                g.setColor(progressColor);
                g.fillRoundRect(barX, barY, fillW, barH, 3, 3);
            }
        }
        if (isEnabled()) {
            Color edgeCol = outlineOverride != null ? outlineOverride
                    : (getModel().isRollover()
                            ? (primary ? new Color(255, 255, 255, 110) : UiTokens.ACCENT_SIGNAL)
                            : new Color(255, 255, 255, 32));
            g.setColor(edgeCol);
            g.setStroke(new BasicStroke(outlineOverride != null ? 1.25f : 1f));
            g.drawRoundRect(inset, inset, w, h, arc, arc);
        }
        if (hasFocus() && isEnabled()) {
            g.setColor(UiTokens.FOCUS_RING);
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(inset, inset, w, h, arc, arc);
        }
        g.setFont(getFont());
        g.setColor(foreground);
        java.awt.FontMetrics metrics = g.getFontMetrics();
        String text = getText();
        int textY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent() - (progressRatio >= 0.0 ? 1 : 0);
        g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, textY);
        g.dispose();
    }

    private static Color blend(Color base, Color tint, float amount) {
        int r = Math.round(base.getRed() * (1f - amount) + tint.getRed() * amount);
        int g = Math.round(base.getGreen() * (1f - amount) + tint.getGreen() * amount);
        int b = Math.round(base.getBlue() * (1f - amount) + tint.getBlue() * amount);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
    }
}
