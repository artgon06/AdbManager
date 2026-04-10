package com.adbmanager.logic.model;

public record DisplayOverrideSuggestion(
        int widthPx,
        int heightPx,
        int densityDpi) {

    public String resolutionLabel() {
        return widthPx + " x " + heightPx;
    }

    public String commandLabel() {
        return resolutionLabel() + " · " + densityDpi + " dpi";
    }
}
