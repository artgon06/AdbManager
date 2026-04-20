package com.adbmanager.logic.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public record DeviceFileEntry(
        String name,
        String path,
        String rawType,
        boolean directory,
        long sizeBytes,
        long modifiedEpochSeconds) {

    private static final DateTimeFormatter MODIFIED_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public DeviceFileEntry {
        name = Objects.requireNonNullElse(name, "");
        path = Objects.requireNonNullElse(path, "");
        rawType = Objects.requireNonNullElse(rawType, "");
        sizeBytes = Math.max(0L, sizeBytes);
        modifiedEpochSeconds = Math.max(0L, modifiedEpochSeconds);
    }

    public boolean file() {
        return !directory;
    }

    public String sizeLabel() {
        return directory ? "-" : InstalledApp.formatBytes(sizeBytes);
    }

    public String modifiedLabel() {
        if (modifiedEpochSeconds <= 0L) {
            return "-";
        }
        return MODIFIED_FORMATTER.format(Instant.ofEpochSecond(modifiedEpochSeconds));
    }
}
