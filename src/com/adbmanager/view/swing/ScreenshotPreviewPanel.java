package com.adbmanager.view.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import com.adbmanager.view.Messages;

public class ScreenshotPreviewPanel extends JPanel {

    private static final int LABEL_GUTTER_WIDTH = 30;
    private static final int LABEL_TOP_PADDING = 8;
    private static final int SCREENSHOT_CORNER_RADIUS = 8;
    private static final int DEFAULT_PREVIEW_HEIGHT = 620;
    private static final int DEFAULT_PREVIEW_WIDTH = 520;
    private static final int MAX_SCREENSHOT_WIDTH = 420;

    private BufferedImage screenshot;
    private AppTheme theme = AppTheme.LIGHT;

    public ScreenshotPreviewPanel() {
        setPreferredSize(new Dimension(DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT));
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
    }

    public void setScreenshot(BufferedImage screenshot) {
        if (this.screenshot != null && this.screenshot != screenshot) {
            this.screenshot.flush();
        }
        this.screenshot = screenshot;
        repaint();
    }

    public void clearScreenshot() {
        if (screenshot != null) {
            screenshot.flush();
        }
        screenshot = null;
        repaint();
    }

    public void refreshTexts() {
        repaint();
    }

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        setBackground(theme.background());
        refreshTexts();
    }

    @Override
    public Dimension getPreferredSize() {
        return stablePreviewSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return stablePreviewSize();
    }

    public int preferredWidthForHeight(int availableHeight) {
        return preferredScreenshotWidth(Math.max(1, availableHeight)) + LABEL_GUTTER_WIDTH;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        Rectangle previewBounds = previewBounds();

        if (screenshot == null) {
            drawPlaceholder(g2d, previewBounds);
        } else {
            drawScreenshot(g2d, previewBounds);
        }
        drawSideTitle(g2d, previewBounds);

        g2d.dispose();
    }

    private Rectangle previewBounds() {
        return new Rectangle(0, 0, Math.max(1, getWidth() - LABEL_GUTTER_WIDTH), getHeight());
    }

    private Dimension stablePreviewSize() {
        int screenshotWidth = preferredScreenshotWidth(DEFAULT_PREVIEW_HEIGHT);
        return new Dimension(screenshotWidth + LABEL_GUTTER_WIDTH, DEFAULT_PREVIEW_HEIGHT);
    }

    private int preferredScreenshotWidth(int availableHeight) {
        if (screenshot == null || screenshot.getWidth() <= 0 || screenshot.getHeight() <= 0) {
            return DEFAULT_PREVIEW_WIDTH - LABEL_GUTTER_WIDTH;
        }
        int scaledWidth = (int) Math.round((availableHeight / (double) screenshot.getHeight()) * screenshot.getWidth());
        if (screenshot.getWidth() > screenshot.getHeight()) {
            return Math.max(1, Math.min(MAX_SCREENSHOT_WIDTH, scaledWidth));
        }
        return Math.max(1, scaledWidth);
    }

    private void drawPlaceholder(Graphics2D g2d, Rectangle previewBounds) {
        g2d.setColor(theme.placeholderBackground());
        g2d.fillRoundRect(
                previewBounds.x,
                previewBounds.y,
                previewBounds.width,
                previewBounds.height,
                18,
                18);

        g2d.setColor(theme.border());
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(
                previewBounds.x,
                previewBounds.y,
                previewBounds.width,
                previewBounds.height,
                18,
                18);

        g2d.setColor(theme.placeholderForeground());
        g2d.setFont(new Font("Inter", Font.BOLD, 22));
        drawCenteredString(g2d, Messages.text("home.preview.empty.title"), previewBounds, previewBounds.y + (previewBounds.height / 2) - 10);

        g2d.setFont(new Font("Inter", Font.PLAIN, 15));
        drawCenteredString(g2d, Messages.text("home.preview.empty.subtitle"), previewBounds, previewBounds.y + (previewBounds.height / 2) + 24);
    }

    private void drawScreenshot(Graphics2D g2d, Rectangle previewBounds) {
        double scale = previewBounds.height / (double) screenshot.getHeight();
        int drawHeight = Math.max(1, previewBounds.height);
        int drawWidth = Math.max(1, (int) Math.round(screenshot.getWidth() * scale));

        if (drawWidth > previewBounds.width) {
            scale = previewBounds.width / (double) screenshot.getWidth();
            drawWidth = Math.max(1, previewBounds.width);
            drawHeight = Math.max(1, (int) Math.round(screenshot.getHeight() * scale));
        }

        int x = previewBounds.x + previewBounds.width - drawWidth;
        int y = previewBounds.y;

        Shape previousClip = g2d.getClip();
        g2d.setClip(new RoundRectangle2D.Float(x, y, drawWidth, drawHeight, SCREENSHOT_CORNER_RADIUS, SCREENSHOT_CORNER_RADIUS));
        g2d.drawImage(screenshot, x, y, drawWidth, drawHeight, null);
        g2d.setClip(previousClip);
    }

    private void drawSideTitle(Graphics2D g2d, Rectangle previewBounds) {
        String title = Messages.text("home.preview.title").toUpperCase();
        Graphics2D rotated = (Graphics2D) g2d.create();
        rotated.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        rotated.setFont(new Font("JetBrains Mono Medium", Font.PLAIN, 11));
        rotated.setColor(new Color(
                theme.textSecondary().getRed(),
                theme.textSecondary().getGreen(),
                theme.textSecondary().getBlue(),
                190));
        int textWidth = rotated.getFontMetrics().stringWidth(title);
        int textAscent = rotated.getFontMetrics().getAscent();
        int textDescent = rotated.getFontMetrics().getDescent();
        Rectangle gutterBounds = new Rectangle(
                previewBounds.x + previewBounds.width,
                previewBounds.y,
                LABEL_GUTTER_WIDTH,
                previewBounds.height);

        double centerX = gutterBounds.getCenterX();
        double centerY = gutterBounds.y + LABEL_TOP_PADDING + (textWidth / 2d);
        rotated.translate(centerX, centerY);
        rotated.rotate(Math.PI / 2d);
        int drawX = -textWidth / 2;
        int drawY = (textAscent - textDescent) / 2;
        rotated.drawString(title, drawX, drawY);
        rotated.dispose();
    }

    private void drawCenteredString(Graphics2D g2d, String text, Rectangle bounds, int baselineY) {
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int x = bounds.x + ((bounds.width - textWidth) / 2);
        g2d.drawString(text, x, baselineY);
    }
}
