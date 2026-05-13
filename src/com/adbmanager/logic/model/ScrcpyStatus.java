package com.adbmanager.logic.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Objects;

public record ScrcpyStatus(
        boolean available,
        boolean managedInstallation,
        String version,
        String executablePath) {

    private static final Pattern VERSION_NUMBER_PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d+))?");

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

    public boolean supportsFlexDisplay() {
        if (!available) {
            return false;
        }

        Matcher matcher = VERSION_NUMBER_PATTERN.matcher(version == null ? "" : version);
        if (!matcher.find()) {
            return false;
        }

        int major = parseVersionPart(matcher.group(1));
        int minor = parseVersionPart(matcher.group(2));
        return major > 4 || (major == 4 && minor >= 0);
    }

    private static int parseVersionPart(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
