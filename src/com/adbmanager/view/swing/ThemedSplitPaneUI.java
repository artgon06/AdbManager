package com.adbmanager.view.swing;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class ThemedSplitPaneUI extends BasicSplitPaneUI {

    private final AppTheme theme;

    public ThemedSplitPaneUI(AppTheme theme) {
        this.theme = theme;
    }

    @Override
    public void installUI(JComponent component) {
        super.installUI(component);
        component.setOpaque(true);
        component.setBackground(theme.background());
        component.setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    public BasicSplitPaneDivider createDefaultDivider() {
        BasicSplitPaneDivider divider = new BasicSplitPaneDivider(this) {
            @Override
            public void paint(Graphics graphics) {
                Graphics2D g2d = (Graphics2D) graphics.create();
                try {
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(theme.background());
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                } finally {
                    g2d.dispose();
                }
            }
        };
        divider.setBorder(BorderFactory.createEmptyBorder());
        divider.setBackground(theme.background());
        divider.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        return divider;
    }

}
