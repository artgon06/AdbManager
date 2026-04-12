package com.adbmanager.logic.model;

import java.util.Objects;

public record ScrcpyCamera(
        String id,
        String label) {

    public ScrcpyCamera {
        id = Objects.requireNonNullElse(id, "").trim();
        label = Objects.requireNonNullElse(label, "").trim();
    }

    public String displayLabel() {
        if (label.isBlank()) {
            return id.isBlank() ? "-" : id;
        }
        if (id.isBlank()) {
            return label;
        }
        return id + " · " + label;
    }

    @Override
    public String toString() {
        return displayLabel();
    }
}
