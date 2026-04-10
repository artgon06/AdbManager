package com.adbmanager.logic.model;

import java.util.Locale;
import java.util.Objects;

public record InstalledApp(
        String packageName,
        String displayName,
        String apkPath,
        long storageBytes,
        boolean systemApp,
        boolean disabled) {

    public InstalledApp {
        packageName = Objects.requireNonNullElse(packageName, "");
        displayName = normalizeDisplayName(packageName, displayName, apkPath);
        apkPath = Objects.requireNonNullElse(apkPath, "");
        storageBytes = Math.max(0L, storageBytes);
    }

    public boolean userApp() {
        return !systemApp;
    }

    public boolean enabled() {
        return !disabled;
    }

    public String storageLabel() {
        return formatBytes(storageBytes);
    }

    public InstalledApp withDisplayName(String newDisplayName) {
        return new InstalledApp(packageName, newDisplayName, apkPath, storageBytes, systemApp, disabled);
    }

    public InstalledApp withStorageBytes(long newStorageBytes) {
        return new InstalledApp(packageName, displayName, apkPath, newStorageBytes, systemApp, disabled);
    }

    public InstalledApp withFlags(boolean newSystemApp, boolean newDisabled) {
        return new InstalledApp(packageName, displayName, apkPath, storageBytes, newSystemApp, newDisabled);
    }

    public String defaultApkFileName() {
        return packageName.isBlank() ? "app.apk" : packageName + ".apk";
    }

    public static String formatBytes(long bytes) {
        if (bytes <= 0L) {
            return "-";
        }

        double kilobytes = bytes / 1024.0;
        if (kilobytes < 1024) {
            return String.format(Locale.US, "%.0f KB", kilobytes);
        }

        double megabytes = kilobytes / 1024.0;
        if (megabytes < 1024) {
            return String.format(Locale.US, "%.1f MB", megabytes);
        }

        double gigabytes = megabytes / 1024.0;
        return String.format(Locale.US, "%.2f GB", gigabytes);
    }

    private static String normalizeDisplayName(String packageName, String displayName, String apkPath) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }

        String candidate = deriveNameFromApkPath(apkPath);
        if (!candidate.isBlank() && !looksLikePackageContainer(candidate, packageName)) {
            return candidate;
        }

        if (packageName == null || packageName.isBlank()) {
            return "App";
        }

        String fallback = humanizePackageName(packageName);
        if (fallback.isBlank()) {
            fallback = packageName;
        }

        return humanizeIdentifier(fallback);
    }

    private static String deriveNameFromApkPath(String apkPath) {
        if (apkPath == null || apkPath.isBlank()) {
            return "";
        }

        String normalizedPath = apkPath.replace('\\', '/').trim();
        if (normalizedPath.isBlank()) {
            return "";
        }

        String[] segments = normalizedPath.split("/");
        if (segments.length == 0) {
            return "";
        }

        String fileName = segments[segments.length - 1];
        if (!fileName.equalsIgnoreCase("base.apk")
                && !fileName.startsWith("split_")
                && fileName.toLowerCase(Locale.ROOT).endsWith(".apk")) {
            return humanizeIdentifier(fileName.substring(0, fileName.length() - 4));
        }

        if (segments.length < 2) {
            return "";
        }

        String folderName = segments[segments.length - 2];
        if (folderName.contains("-")) {
            folderName = folderName.substring(0, folderName.indexOf('-'));
        }
        if (folderName.contains("==")) {
            return "";
        }

        return humanizeIdentifier(folderName);
    }

    private static boolean looksLikePackageContainer(String candidate, String packageName) {
        if (candidate == null || candidate.isBlank() || packageName == null || packageName.isBlank()) {
            return false;
        }

        String trimmed = candidate.trim().toLowerCase(Locale.ROOT);
        return trimmed.startsWith("com ")
                || trimmed.startsWith("org ")
                || trimmed.startsWith("net ")
                || trimmed.startsWith("io ")
                || trimmed.startsWith("dev ");
    }

    private static String humanizePackageName(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return "";
        }

        String[] segments = packageName.trim().split("\\.");
        int startIndex = 0;
        if (segments.length > 1 && isCommonTopLevelSegment(segments[0])) {
            startIndex = 1;
        }

        StringBuilder builder = new StringBuilder();
        for (int index = startIndex; index < segments.length; index++) {
            String segment = segments[index];
            if (segment == null || segment.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(segment);
        }
        return builder.toString();
    }

    private static boolean isCommonTopLevelSegment(String segment) {
        String value = segment == null ? "" : segment.toLowerCase(Locale.ROOT);
        return value.equals("com")
                || value.equals("org")
                || value.equals("net")
                || value.equals("io")
                || value.equals("dev")
                || value.equals("app");
    }

    private static String humanizeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = value.trim()
                .replace('_', ' ')
                .replace('-', ' ')
                .replace('.', ' ')
                .replaceAll("(?<=[a-z])(?=[A-Z])", " ")
                .replaceAll("(?<=[A-Z])(?=[A-Z][a-z])", " ")
                .replaceAll("(?<=[A-Za-z])(?=\\d)", " ")
                .replaceAll("(?<=\\d)(?=[A-Za-z])", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isBlank()) {
            return "";
        }

        String[] words = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            if (isAcronym(word)) {
                builder.append(word.toUpperCase(Locale.ROOT));
            } else {
                builder.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    builder.append(word.substring(1));
                }
            }
        }
        return builder.toString();
    }

    private static boolean isAcronym(String value) {
        if (value == null || value.isBlank() || value.length() <= 1) {
            return false;
        }

        int uppercaseOrDigitCount = 0;
        for (char character : value.toCharArray()) {
            if (Character.isUpperCase(character) || Character.isDigit(character)) {
                uppercaseOrDigitCount++;
            }
        }
        return uppercaseOrDigitCount == value.length();
    }
}
