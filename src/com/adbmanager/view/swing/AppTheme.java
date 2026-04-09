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

    // Window background and the outer canvas behind cards and content sections.
    private final Color background;
    // Main surfaces such as the top bar, settings cards, summary card, and combo popups.
    private final Color surface;
    // Elevated control surfaces such as combo box fields, RAM card, and selected top-bar tabs.
    private final Color secondarySurface;
    // Shared stroke color for section outlines, combo borders, popup borders, and separators.
    private final Color border;
    // Primary text for values, titles, buttons, and combo box content.
    private final Color textPrimary;
    // Secondary text for labels, muted text, and inactive icons.
    private final Color textSecondary;
    // Highlight background for selected rows in combo box popups.
    private final Color selectionBackground;
    // Text/icon color shown on top of selectionBackground.
    private final Color selectionForeground;
    // Accent color for primary actions, active icons, links, and selected navigation state.
    private final Color actionBackground;
    // Text color shown on top of actionBackground buttons.
    private final Color actionForeground;
    // Border color for disabled controls and low-emphasis states.
    private final Color disabledBorder;
    // Placeholder surface for empty screenshot previews.
    private final Color placeholderBackground;
    // Placeholder text color for empty screenshot previews.
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
