package com.adbmanager.view.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

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
        setBorder(BorderFactory.createLineBorder(theme.border(), 2));
        repaint();
    }

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        setBackground(theme.background());
        refreshTexts();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int inset = 24;
        int sideLabelWidth = 34;
        int availableWidth = getWidth() - (inset * 2) - sideLabelWidth;
        int availableHeight = getHeight() - (inset * 2);

        if (screenshot == null) {
            drawPlaceholder(g2d, inset, availableWidth, availableHeight);
        } else {
            drawScreenshot(g2d, inset, availableWidth, availableHeight);
        }
        drawSideTitle(g2d);

        g2d.dispose();
    }

    private void drawPlaceholder(Graphics2D g2d, int inset, int availableWidth, int availableHeight) {
        g2d.setColor(theme.placeholderBackground());
        g2d.fillRoundRect(inset, inset, availableWidth, availableHeight, 18, 18);

        g2d.setColor(theme.border());
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(inset, inset, availableWidth, availableHeight, 18, 18);

        g2d.setColor(theme.placeholderForeground());
        g2d.setFont(new Font("Inter", Font.BOLD, 22));
        drawCenteredString(g2d, Messages.text("home.preview.empty.title"), getWidth(), getHeight() / 2 - 10);

        g2d.setFont(new Font("Inter", Font.PLAIN, 15));
        drawCenteredString(g2d, Messages.text("home.preview.empty.subtitle"), getWidth(), getHeight() / 2 + 24);
    }

    private void drawScreenshot(Graphics2D g2d, int inset, int availableWidth, int availableHeight) {
        double widthScale = availableWidth / (double) screenshot.getWidth();
        double heightScale = availableHeight / (double) screenshot.getHeight();
        double scale = Math.min(widthScale, heightScale);

        int drawWidth = Math.max(1, (int) Math.round(screenshot.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(screenshot.getHeight() * scale));
        int x = 24 + (availableWidth - drawWidth) / 2;
        int y = (getHeight() - drawHeight) / 2;

        g2d.drawImage(screenshot, x, y, drawWidth, drawHeight, null);
        g2d.setColor(theme.border());
        g2d.drawRoundRect(x, y, drawWidth, drawHeight, 12, 12);
    }

    private void drawSideTitle(Graphics2D g2d) {
        String title = Messages.text("home.preview.title").toUpperCase();
        Graphics2D rotated = (Graphics2D) g2d.create();
        rotated.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        rotated.setFont(new Font("Inter", Font.BOLD, 11));
        rotated.setColor(new Color(
                theme.textSecondary().getRed(),
                theme.textSecondary().getGreen(),
                theme.textSecondary().getBlue(),
                190));
        int textWidth = rotated.getFontMetrics().stringWidth(title);
        rotated.rotate(Math.PI / 2d);
        rotated.drawString(title, 24, -getWidth() + 18 + ((getHeight() - textWidth) / 2));
        rotated.dispose();
    }

    private void drawCenteredString(Graphics2D g2d, String text, int width, int baselineY) {
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int x = (width - textWidth) / 2;
        g2d.drawString(text, x, baselineY);
    }
}
