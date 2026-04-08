package com.adbmanager.view.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

public class ToolbarIcon implements Icon {

    public enum Type {
        HOME,
        REFRESH,
        SETTINGS
    }

    private final Type type;
    private final int size;
    private final Color color;

    public ToolbarIcon(Type type, int size, Color color) {
        this.type = type;
        this.size = size;
        this.color = color;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.translate(x, y);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(Math.max(1.7f, size / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (type) {
            case HOME -> paintHome(g2d);
            case REFRESH -> paintRefresh(g2d);
            case SETTINGS -> paintSettings(g2d);
        }

        g2d.dispose();
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    private void paintHome(Graphics2D g2d) {
        int roofTop = size / 5;
        int roofBottom = size / 2;
        int left = size / 5;
        int right = size - left;
        int bodyBottom = size - size / 6;

        g2d.drawLine(left, roofBottom, size / 2, roofTop);
        g2d.drawLine(size / 2, roofTop, right, roofBottom);
        g2d.drawLine(left + 1, roofBottom, left + 1, bodyBottom);
        g2d.drawLine(right - 1, roofBottom, right - 1, bodyBottom);
        g2d.drawLine(left + 1, bodyBottom, right - 1, bodyBottom);

        int doorLeft = size / 2 - size / 10;
        int doorTop = size / 2 + size / 6;
        g2d.drawLine(doorLeft, bodyBottom, doorLeft, doorTop);
        g2d.drawLine(size - doorLeft, bodyBottom, size - doorLeft, doorTop);
    }

    private void paintRefresh(Graphics2D g2d) {
        int padding = size / 6;
        int arcSize = size - (padding * 2);
        g2d.drawArc(padding, padding, arcSize, arcSize, 35, 270);

        int arrowX = size - padding - 1;
        int arrowY = size / 2 - size / 5;
        g2d.drawLine(arrowX, arrowY, arrowX - size / 5, arrowY);
        g2d.drawLine(arrowX, arrowY, arrowX - size / 9, arrowY + size / 5);
    }

    private void paintSettings(Graphics2D g2d) {
        int center = size / 2;
        int outerRadius = size / 3;
        int innerRadius = size / 7;

        g2d.drawOval(center - innerRadius, center - innerRadius, innerRadius * 2, innerRadius * 2);

        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * 60);
            int innerX = center + (int) Math.round(Math.cos(angle) * innerRadius * 1.8);
            int innerY = center + (int) Math.round(Math.sin(angle) * innerRadius * 1.8);
            int outerX = center + (int) Math.round(Math.cos(angle) * outerRadius);
            int outerY = center + (int) Math.round(Math.sin(angle) * outerRadius);
            g2d.drawLine(innerX, innerY, outerX, outerY);
        }
    }
}
