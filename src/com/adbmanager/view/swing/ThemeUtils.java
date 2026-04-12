package com.adbmanager.view.swing;

import java.awt.Color;

final class ThemeUtils {

    private ThemeUtils() {
    }

    static Color blend(Color base, Color overlay, double ratio) {
        double clampedRatio = Math.max(0d, Math.min(1d, ratio));
        int red = (int) Math.round((base.getRed() * (1d - clampedRatio)) + (overlay.getRed() * clampedRatio));
        int green = (int) Math.round((base.getGreen() * (1d - clampedRatio)) + (overlay.getGreen() * clampedRatio));
        int blue = (int) Math.round((base.getBlue() * (1d - clampedRatio)) + (overlay.getBlue() * clampedRatio));
        int alpha = (int) Math.round((base.getAlpha() * (1d - clampedRatio)) + (overlay.getAlpha() * clampedRatio));
        return new Color(red, green, blue, alpha);
    }
}
