package com.adbmanager.view.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

public class ThemedComboBoxUI extends BasicComboBoxUI {

    private final AppTheme theme;

    public ThemedComboBoxUI(AppTheme theme) {
        this.theme = theme;
    }

    @Override
    protected JButton createArrowButton() {
        JButton button = new JButton(new ArrowIcon(theme.textSecondary()));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(theme.secondarySurface());
        button.setForeground(theme.textSecondary());
        button.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, theme.border()));
        button.setFocusPainted(false);
        button.setFocusable(false);
        return button;
    }

    @Override
    public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds, boolean hasFocus) {
        graphics.setColor(comboBox.isEnabled() ? theme.secondarySurface() : theme.surface());
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    protected ComboPopup createPopup() {
        BasicComboPopup popup = new BasicComboPopup(comboBox) {
            @Override
            protected JScrollPane createScroller() {
                JScrollPane scroller = super.createScroller();
                scroller.getViewport().setBackground(theme.surface());
                scroller.setBorder(BorderFactory.createLineBorder(theme.border(), 1));
                return scroller;
            }
        };

        popup.setBorder(BorderFactory.createLineBorder(theme.border(), 1));
        popup.getList().setBackground(theme.surface());
        popup.getList().setForeground(theme.textPrimary());
        popup.getList().setSelectionBackground(theme.selectionBackground());
        popup.getList().setSelectionForeground(theme.selectionForeground());
        return popup;
    }

    public static void apply(JComponent comboBox, AppTheme theme) {
        comboBox.setBackground(theme.secondarySurface());
        comboBox.setForeground(theme.textPrimary());
        comboBox.setOpaque(true);
    }

    private static final class ArrowIcon implements Icon {

        private final Color color;

        private ArrowIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2d = (Graphics2D) graphics.create();
            g2d.translate(x, y);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);

            Polygon polygon = new Polygon(
                    new int[] { 1, 7, 13 },
                    new int[] { 3, 9, 3 },
                    3);
            g2d.fillPolygon(polygon);
            g2d.dispose();
        }

        @Override
        public int getIconWidth() {
            return 14;
        }

        @Override
        public int getIconHeight() {
            return 10;
        }
    }
}
