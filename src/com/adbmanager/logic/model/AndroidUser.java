package com.adbmanager.logic.model;

public record AndroidUser(
        int id,
        String name,
        boolean current,
        boolean running) {

    public AndroidUser {
        name = name == null || name.isBlank() ? ("User " + id) : name.trim();
    }

    public String displayLabel() {
        return id + " - " + name;
    }

    @Override
    public String toString() {
        return displayLabel();
    }
}
