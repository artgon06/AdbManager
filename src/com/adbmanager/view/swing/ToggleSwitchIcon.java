package com.adbmanager.view.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

public class ToggleSwitchIcon implements Icon {

    private final AppTheme theme;
    private final boolean selected;
    private final boolean enabled;
    private final int width;
    private final int height;

    public ToggleSwitchIcon(AppTheme theme, boolean selected, boolean enabled) {
        this(theme, selected, enabled, 42, 24);
    }

    public ToggleSwitchIcon(AppTheme theme, boolean selected, boolean enabled, int width, int height) {
        this.theme = theme;
        this.selected = selected;
        this.enabled = enabled;
        this.width = width;
        this.height = height;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            g2d.translate(x, y);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color trackColor = !enabled
                    ? theme.secondarySurface()
                    : selected ? theme.actionBackground() : theme.secondarySurface();
            Color thumbColor = enabled ? theme.actionForeground() : theme.textSecondary();

            g2d.setColor(trackColor);
            g2d.fillRoundRect(0, 0, width, height, height, height);
            g2d.setColor(selected && enabled ? theme.actionBackground() : theme.border());
            g2d.drawRoundRect(0, 0, width - 1, height - 1, height, height);

            int thumbSize = height - 6;
            int thumbX = selected ? width - thumbSize - 3 : 3;
            g2d.setColor(thumbColor);
            g2d.fillOval(thumbX, 3, thumbSize, thumbSize);
        } finally {
            g2d.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
}
