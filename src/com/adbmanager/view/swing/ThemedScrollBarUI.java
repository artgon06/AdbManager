package com.adbmanager.view.swing;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class ThemedScrollBarUI extends BasicScrollBarUI {

    private final AppTheme theme;

    public ThemedScrollBarUI(AppTheme theme) {
        this.theme = theme;
    }

    @Override
    protected void configureScrollBarColors() {
        thumbColor = theme.secondarySurface();
        trackColor = theme.surface();
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected void paintTrack(Graphics graphics, JComponent component, Rectangle trackBounds) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            g2d.setColor(theme.surface());
            g2d.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        } finally {
            g2d.dispose();
        }
    }

    @Override
    protected void paintThumb(Graphics graphics, JComponent component, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(isDragging ? theme.actionBackground() : theme.border());

            int arc = scrollbar.getOrientation() == JScrollBar.VERTICAL ? thumbBounds.width : thumbBounds.height;
            int insetX = scrollbar.getOrientation() == JScrollBar.VERTICAL ? 3 : 1;
            int insetY = scrollbar.getOrientation() == JScrollBar.VERTICAL ? 1 : 3;
            g2d.fillRoundRect(
                    thumbBounds.x + insetX,
                    thumbBounds.y + insetY,
                    Math.max(6, thumbBounds.width - (insetX * 2)),
                    Math.max(6, thumbBounds.height - (insetY * 2)),
                    arc,
                    arc);
        } finally {
            g2d.dispose();
        }
    }

    private JButton createZeroButton() {
        JButton button = new JButton();
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }
}
