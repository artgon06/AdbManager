package com.adbmanager.view.swing;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.adbmanager.view.Messages;

public class ScreenshotPreviewPanel extends JPanel {

    private BufferedImage screenshot;
    private AppTheme theme = AppTheme.LIGHT;

    public ScreenshotPreviewPanel() {
        setPreferredSize(new Dimension(520, 620));
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
    }

    public void setScreenshot(BufferedImage screenshot) {
        this.screenshot = screenshot;
        repaint();
    }

    public void clearScreenshot() {
        screenshot = null;
        repaint();
    }

    public void refreshTexts() {
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(theme.border(), 2),
                Messages.text("home.preview.title"),
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 16),
                theme.textPrimary()));
        repaint();
    }

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        setBackground(theme.surface());
        refreshTexts();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int inset = 24;
        int availableWidth = getWidth() - (inset * 2);
        int availableHeight = getHeight() - (inset * 2) - 20;

        if (screenshot == null) {
            drawPlaceholder(g2d, inset, availableWidth, availableHeight);
        } else {
            drawScreenshot(g2d, inset, availableWidth, availableHeight);
        }

        g2d.dispose();
    }

    private void drawPlaceholder(Graphics2D g2d, int inset, int availableWidth, int availableHeight) {
        g2d.setColor(theme.placeholderBackground());
        g2d.fillRoundRect(inset, inset + 12, availableWidth, availableHeight, 18, 18);

        g2d.setColor(theme.border());
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(inset, inset + 12, availableWidth, availableHeight, 18, 18);

        g2d.setColor(theme.placeholderForeground());
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        drawCenteredString(g2d, Messages.text("home.preview.empty.title"), getWidth(), getHeight() / 2 - 10);

        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        drawCenteredString(g2d, Messages.text("home.preview.empty.subtitle"), getWidth(), getHeight() / 2 + 24);
    }

    private void drawScreenshot(Graphics2D g2d, int inset, int availableWidth, int availableHeight) {
        double widthScale = availableWidth / (double) screenshot.getWidth();
        double heightScale = availableHeight / (double) screenshot.getHeight();
        double scale = Math.min(widthScale, heightScale);

        int drawWidth = Math.max(1, (int) Math.round(screenshot.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(screenshot.getHeight() * scale));
        int x = (getWidth() - drawWidth) / 2;
        int y = (getHeight() - drawHeight) / 2 + 10;

        g2d.drawImage(screenshot, x, y, drawWidth, drawHeight, null);
        g2d.setColor(theme.border());
        g2d.drawRoundRect(x, y, drawWidth, drawHeight, 12, 12);
    }

    private void drawCenteredString(Graphics2D g2d, String text, int width, int baselineY) {
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int x = (width - textWidth) / 2;
        g2d.drawString(text, x, baselineY);
    }
}
