package com.adbmanager.view.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

final class ThemedSliderUI extends BasicSliderUI {

    private static final int TRACK_HEIGHT = 18;
    private static final int THUMB_SIZE = 24;

    private final AppTheme theme;

    ThemedSliderUI(JSlider slider, AppTheme theme) {
        super(slider);
        this.theme = theme == null ? AppTheme.LIGHT : theme;
    }

    @Override
    public Dimension getPreferredHorizontalSize() {
        return new Dimension(220, 34);
    }

    @Override
    protected Dimension getThumbSize() {
        return new Dimension(THUMB_SIZE, THUMB_SIZE);
    }

    @Override
    public void paintTrack(Graphics graphics) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle track = trackRect;
        int y = track.y + (track.height - TRACK_HEIGHT) / 2;
        int x = track.x;
        int width = track.width;
        int arc = TRACK_HEIGHT;
        Color trackColor = slider.isEnabled()
                ? ThemeUtils.blend(theme.secondarySurface(), theme.background(), 0.18d)
                : theme.surface();
        g2d.setColor(trackColor);
        g2d.fillRoundRect(x, y, width, TRACK_HEIGHT, arc, arc);

        int fillEnd = thumbRect.x + thumbRect.width / 2;
        int fillWidth = Math.max(0, Math.min(width, fillEnd - x));
        Color fillColor = slider.isEnabled() ? theme.actionBackground() : theme.textSecondary();
        g2d.setColor(fillColor);
        g2d.fillRoundRect(x, y, fillWidth, TRACK_HEIGHT, arc, arc);

        g2d.setColor(slider.isEnabled() ? theme.border() : theme.disabledBorder());
        g2d.drawRoundRect(x, y, width - 1, TRACK_HEIGHT - 1, arc, arc);
        g2d.dispose();
    }

    @Override
    public void paintThumb(Graphics graphics) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = thumbRect.x;
        int y = thumbRect.y;
        int size = Math.min(thumbRect.width, thumbRect.height);
        Color thumbColor = slider.isEnabled() ? theme.actionForeground() : theme.secondarySurface();
        Color borderColor = slider.isEnabled() ? theme.actionBackground() : theme.disabledBorder();
        g2d.setColor(thumbColor);
        g2d.fillOval(x, y, size, size);
        g2d.setColor(borderColor);
        g2d.drawOval(x, y, size - 1, size - 1);
        g2d.dispose();
    }

    @Override
    public void paintFocus(Graphics graphics) {
        // The app uses visible hover/active states instead of the default dotted focus ring.
    }

    @Override
    public void installUI(JComponent component) {
        super.installUI(component);
        component.setOpaque(false);
    }
}
