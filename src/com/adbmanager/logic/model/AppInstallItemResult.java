package com.adbmanager.logic.model;

public record AppInstallItemResult(
        String sourceLabel,
        boolean success,
        String message) {
}
