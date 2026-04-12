package com.adbmanager.logic.model;

public record KeyboardInputMethod(
        String id,
        boolean enabled,
        boolean selected) {

    public KeyboardInputMethod {
        id = id == null ? "" : id.trim();
    }

    public String displayLabel() {
        return id.isBlank() ? "-" : id;
    }

    @Override
    public String toString() {
        return displayLabel();
    }
}
