package com.adbmanager.view.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

public final class ButtonStyler {

    private ButtonStyler() {
    }

    public static void applyStandard(JButton button, AppTheme theme, boolean prominent, boolean iconOnly,
            boolean hasIconAndText) {
        // sizing: normalize single-line buttons to 32px height while leaving
        // intentionally larger buttons (toolbars / large primary) unchanged.
        final int normalizedHeight = 32;
        if (iconOnly) {
            Dimension d = new Dimension(32, 32);
            button.setPreferredSize(d);
            button.setMinimumSize(d);
            button.setMaximumSize(d);
        } else {
            // keep width flexible, enforce exact normalized height for single-line
            // buttons so visual height is consistent across the app.
            Dimension pref = button.getPreferredSize();
            int width = pref.width <= 0 ? 0 : pref.width;
            Dimension enforced = new Dimension(width, normalizedHeight);
            button.setPreferredSize(enforced);
            button.setMinimumSize(new Dimension(0, normalizedHeight));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, normalizedHeight));
        }

        // padding
        if (iconOnly) {
            button.setMargin(new Insets(6, 6, 6, 6));
        } else if (!hasIconAndText) {
            // Text-only buttons: 6px horizontal padding as spec
            button.setMargin(new Insets(6, 6, 6, 6));
        } else {
            // Icon + text: left 6px, right 8px, gap 5px
            button.setMargin(new Insets(6, 6, 6, 8));
            button.setIconTextGap(5);
        }

        // font
        button.setFont(new java.awt.Font("Inter", java.awt.Font.BOLD, 13));

        // colors and gradients
        Color bg;
        Color fg;
        Color[] borderStops;
        if (prominent) {
            bg = new Color(0x16, 0x71, 0xF9);
            fg = new Color(0xF0, 0xF0, 0xF0);
            borderStops = new Color[] { new Color(0x40, 0x8C, 0xFF), new Color(0x19, 0x51, 0xA6),
                    new Color(0x40, 0x8C, 0xFF) };
        } else if (theme == AppTheme.LIGHT) {
            bg = new Color(0xE8, 0xE8, 0xE8);
            fg = new Color(0x10, 0x10, 0x10);
            borderStops = new Color[] { new Color(0xE0, 0xE0, 0xE0), new Color(0xC8, 0xC8, 0xC8),
                    new Color(0xE0, 0xE0, 0xE0) };
        } else {
            bg = new Color(0x20, 0x20, 0x20);
            fg = new Color(0xF0, 0xF0, 0xF0);
            borderStops = new Color[] { new Color(0x60, 0x60, 0x60), new Color(0x30, 0x30, 0x30),
                    new Color(0x60, 0x60, 0x60) };
        }

        button.setForeground(fg);

        // install custom UI that paints rounded background, gradient inner border and
        // subtle shadow
        button.setUI(new RoundedButtonUI(bg, fg, borderStops, 6));
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setOpaque(false);
    }

    private static final class RoundedButtonUI extends BasicButtonUI {
        private final Color background;
        @SuppressWarnings("unused")
        private final Color foreground;
        private final Color[] borderStops;
        private final int radius;

        RoundedButtonUI(Color background, Color foreground, Color[] borderStops, int radius) {
            this.background = background;
            this.foreground = foreground;
            this.borderStops = borderStops;
            this.radius = radius;
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            int w = c.getWidth();
            int h = c.getHeight();

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // compute arc (RoundRectangle2D expects arc width/height, so use radius*2)
            float arc = Math.max(2f, radius * 2f);

            // drop shadow
            g2.setColor(new Color(0, 0, 0, (int) (255 * 0.05)));
            RoundRectangle2D shadow = new RoundRectangle2D.Float(1, 1, w - 2, h - 2, arc, arc);
            g2.fill(shadow);

            // background
            g2.setColor(background);
            RoundRectangle2D outer = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arc, arc);
            g2.fill(outer);

            // inner gradient border: 3-stop gradient from top-left to bottom-right
            float[] fractions = { 0.0f, 0.5f, 1.0f };
            LinearGradientPaint lgp = new LinearGradientPaint(0, 0, w, h, fractions, borderStops);
            g2.setPaint(lgp);
            g2.setStroke(new BasicStroke(1f));
            RoundRectangle2D borderRect = new RoundRectangle2D.Float(0.5f, 0.5f, w - 2f, h - 2f, arc, arc);
            g2.draw(borderRect);

            g2.dispose();

            // Paint text and icon without disabled shadow
            paintButtonContents(g, b);
        }

        private void paintButtonContents(Graphics g, AbstractButton b) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Insets i = b.getInsets();
            int x = i.left;
            int y = i.top;
            int w = b.getWidth() - (i.right + i.left);
            int h = b.getHeight() - (i.top + i.bottom);

            Icon icon = b.getIcon();
            String text = b.getText();
            boolean hasIcon = icon != null;
            boolean hasText = text != null && !text.isEmpty();
            int gap = hasIcon && hasText ? 5 : 0;

            if (hasIcon && hasText) {
                // Both icon and text: layout horizontally (icon left, text right)
                FontMetrics fm = g2.getFontMetrics(b.getFont());
                int textWidth = fm.stringWidth(text);
                int iconWidth = icon.getIconWidth();
                int totalWidth = iconWidth + gap + textWidth;
                int startX = x + (w - totalWidth) / 2;

                // Paint icon
                icon.paintIcon(b, g2, startX, y + (h - icon.getIconHeight()) / 2);

                // Paint text
                g2.setFont(b.getFont());
                g2.setColor(b.getForeground());
                int textX = startX + iconWidth + gap;
                int textY = y + ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(text, textX, textY);
            } else if (hasIcon) {
                // Icon only: center it
                icon.paintIcon(b, g2, x + (w - icon.getIconWidth()) / 2, y + (h - icon.getIconHeight()) / 2);
            } else if (hasText) {
                // Text only: center it
                g2.setFont(b.getFont());
                g2.setColor(b.getForeground());
                FontMetrics fm = g2.getFontMetrics();
                int textX = x + (w - fm.stringWidth(text)) / 2;
                int textY = y + ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(text, textX, textY);
            }

            g2.dispose();
        }
    }
}
