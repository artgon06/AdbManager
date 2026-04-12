package com.adbmanager.logic.model;

import java.util.Objects;

public record ScrcpyStatus(
        boolean available,
        boolean managedInstallation,
        String version,
        String executablePath) {

    public ScrcpyStatus {
        version = Objects.requireNonNullElse(version, "-");
        executablePath = Objects.requireNonNullElse(executablePath, "");
    }

    public static ScrcpyStatus missing() {
        return new ScrcpyStatus(false, false, "-", "");
    }

    public String locationLabel() {
        return executablePath == null || executablePath.isBlank() ? "-" : executablePath;
    }
}
