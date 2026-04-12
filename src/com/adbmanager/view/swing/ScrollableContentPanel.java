package com.adbmanager.view.swing;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Scrollable;

public class ScrollableContentPanel extends JPanel implements Scrollable {

    private final int unitIncrement;
    private final int blockIncrement;
    private final boolean trackViewportWidth;
    private final boolean trackViewportHeight;

    public ScrollableContentPanel() {
        this(24, 96, true, false);
    }

    public ScrollableContentPanel(
            int unitIncrement,
            int blockIncrement,
            boolean trackViewportWidth,
            boolean trackViewportHeight) {
        this.unitIncrement = Math.max(1, unitIncrement);
        this.blockIncrement = Math.max(this.unitIncrement, blockIncrement);
        this.trackViewportWidth = trackViewportWidth;
        this.trackViewportHeight = trackViewportHeight;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return unitIncrement;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return blockIncrement;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return trackViewportWidth;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return trackViewportHeight;
    }
}
