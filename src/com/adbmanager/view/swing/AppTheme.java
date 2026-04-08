package com.adbmanager.view.swing;

import java.awt.Color;

public enum AppTheme {
    LIGHT(
            new Color(248, 249, 251),
            new Color(248, 249, 251),
            new Color(239, 243, 249),
            new Color(216, 223, 234),
            new Color(28, 32, 39),
            new Color(102, 111, 126),
            new Color(224, 236, 255),
            new Color(26, 32, 43),
            new Color(53, 111, 240),
            Color.WHITE,
            new Color(192, 201, 216),
            new Color(235, 239, 246),
            new Color(96, 105, 118)),
    DARK(
            new Color(20, 23, 29),
            new Color(24, 28, 34),
            new Color(34, 39, 47),
            new Color(70, 78, 91),
            new Color(236, 239, 244),
            new Color(160, 168, 180),
            new Color(42, 62, 106),
            new Color(236, 239, 244),
            new Color(96, 146, 255),
            Color.WHITE,
            new Color(72, 80, 94),
            new Color(44, 50, 60),
            new Color(184, 190, 201));

    private final Color background;
    private final Color surface;
    private final Color secondarySurface;
    private final Color border;
    private final Color textPrimary;
    private final Color textSecondary;
    private final Color selectionBackground;
    private final Color selectionForeground;
    private final Color actionBackground;
    private final Color actionForeground;
    private final Color disabledBorder;
    private final Color placeholderBackground;
    private final Color placeholderForeground;

    AppTheme(
            Color background,
            Color surface,
            Color secondarySurface,
            Color border,
            Color textPrimary,
            Color textSecondary,
            Color selectionBackground,
            Color selectionForeground,
            Color actionBackground,
            Color actionForeground,
            Color disabledBorder,
            Color placeholderBackground,
            Color placeholderForeground) {
        this.background = background;
        this.surface = surface;
        this.secondarySurface = secondarySurface;
        this.border = border;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.selectionBackground = selectionBackground;
        this.selectionForeground = selectionForeground;
        this.actionBackground = actionBackground;
        this.actionForeground = actionForeground;
        this.disabledBorder = disabledBorder;
        this.placeholderBackground = placeholderBackground;
        this.placeholderForeground = placeholderForeground;
    }

    public Color background() {
        return background;
    }

    public Color surface() {
        return surface;
    }

    public Color secondarySurface() {
        return secondarySurface;
    }

    public Color border() {
        return border;
    }

    public Color textPrimary() {
        return textPrimary;
    }

    public Color textSecondary() {
        return textSecondary;
    }

    public Color selectionBackground() {
        return selectionBackground;
    }

    public Color selectionForeground() {
        return selectionForeground;
    }

    public Color actionBackground() {
        return actionBackground;
    }

    public Color actionForeground() {
        return actionForeground;
    }

    public Color disabledBorder() {
        return disabledBorder;
    }

    public Color placeholderBackground() {
        return placeholderBackground;
    }

    public Color placeholderForeground() {
        return placeholderForeground;
    }

    public static AppTheme fromActionCommand(String actionCommand) {
        for (AppTheme theme : values()) {
            if (theme.name().equalsIgnoreCase(actionCommand)) {
                return theme;
            }
        }
        return LIGHT;
    }
}
