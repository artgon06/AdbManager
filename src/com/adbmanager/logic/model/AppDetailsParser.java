package com.adbmanager.logic.model;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppDetailsParser {

    private static final Pattern APP_LABEL_PATTERN = Pattern.compile("^\\s*application-label(?:-[^:]+)?:'?(.*?)'?\\s*$");
    private static final Pattern NON_LOCALIZED_LABEL_PATTERN = Pattern.compile("nonLocalizedLabel=([^\\s].*?)(?:\\s+[A-Za-z]+=|$)");
    private static final Pattern VERSION_NAME_PATTERN = Pattern.compile("\\bversionName=([^\\s]+)");
    private static final Pattern VERSION_CODE_PATTERN = Pattern.compile("\\bversionCode=([^\\s]+)");
    private static final Pattern TARGET_SDK_PATTERN = Pattern.compile("\\btargetSdk=([^\\s]+)");
    private static final Pattern MIN_SDK_PATTERN = Pattern.compile("\\bminSdk=([^\\s]+)");
    private static final Pattern INSTALLER_PATTERN = Pattern.compile("\\binstallerPackageName=([^\\s]+)");
    private static final Pattern DATA_DIR_PATTERN = Pattern.compile("\\bdataDir=([^\\s]+)");

    public AppDetails parse(
            InstalledApp baseApp,
            String dumpOutput,
            String apkPathsOutput,
            long codeSizeBytes,
            long dataSizeBytes,
            long cacheSizeBytes,
            Map<String, String> appOpsByName,
            BufferedImage iconImage) {
        String displayName = parseDisplayName(dumpOutput, baseApp.displayName());
        String sourceDir = parsePrimaryApkPath(apkPathsOutput, baseApp.apkPath());
        List<AppPermission> permissions = parsePermissions(dumpOutput, appOpsByName);

        return new AppDetails(
                baseApp,
                displayName,
                findFirst(dumpOutput, VERSION_NAME_PATTERN, "-"),
                findFirst(dumpOutput, VERSION_CODE_PATTERN, "-"),
                findFirst(dumpOutput, TARGET_SDK_PATTERN, "-"),
                findFirst(dumpOutput, MIN_SDK_PATTERN, "-"),
                findFirst(dumpOutput, INSTALLER_PATTERN, "-"),
                sourceDir,
                findFirst(dumpOutput, DATA_DIR_PATTERN, "-"),
                codeSizeBytes,
                dataSizeBytes,
                cacheSizeBytes,
                dumpOutput != null && dumpOutput.contains("DEBUGGABLE"),
                permissions,
                iconImage);
    }

    public String parseDisplayName(String dumpOutput, String fallback) {
        if (dumpOutput != null) {
            for (String line : dumpOutput.split("\\R")) {
                Matcher appLabelMatcher = APP_LABEL_PATTERN.matcher(line);
                if (appLabelMatcher.matches()) {
                    String value = sanitizeLabel(appLabelMatcher.group(1));
                    if (!value.isBlank()) {
                        return value;
                    }
                }

                Matcher nonLocalizedMatcher = NON_LOCALIZED_LABEL_PATTERN.matcher(line);
                if (nonLocalizedMatcher.find()) {
                    String value = sanitizeLabel(nonLocalizedMatcher.group(1));
                    if (!value.isBlank() && !"null".equalsIgnoreCase(value)) {
                        return value;
                    }
                }
            }
        }
        return fallback;
    }

    public String parsePrimaryApkPath(String apkPathsOutput, String fallback) {
        String firstPath = fallback;
        if (apkPathsOutput == null || apkPathsOutput.isBlank()) {
            return firstPath == null || firstPath.isBlank() ? "-" : firstPath;
        }

        for (String line : apkPathsOutput.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("package:")) {
                continue;
            }

            String path = trimmed.substring("package:".length()).trim();
            if (firstPath == null || firstPath.isBlank()) {
                firstPath = path;
            }
            if (path.endsWith("/base.apk")) {
                return path;
            }
        }

        return firstPath == null || firstPath.isBlank() ? "-" : firstPath;
    }

    public List<String> parseApkPaths(String apkPathsOutput) {
        List<String> paths = new ArrayList<>();
        if (apkPathsOutput == null || apkPathsOutput.isBlank()) {
            return paths;
        }

        for (String line : apkPathsOutput.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("package:")) {
                paths.add(trimmed.substring("package:".length()).trim());
            }
        }
        return paths;
    }

    public List<AppPermission> parsePermissions(String dumpOutput, Map<String, String> appOpsByName) {
        Map<String, PermissionStateBuilder> permissions = new LinkedHashMap<>();
        Section section = Section.NONE;

        if (dumpOutput == null || dumpOutput.isBlank()) {
            return List.of();
        }

        for (String line : dumpOutput.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if ("requested permissions:".equals(trimmed)) {
                section = Section.REQUESTED;
                continue;
            }
            if ("install permissions:".equals(trimmed)) {
                section = Section.INSTALL;
                continue;
            }
            if ("runtime permissions:".equals(trimmed)) {
                section = Section.RUNTIME;
                continue;
            }
            if (trimmed.endsWith(":") && !trimmed.contains("granted=") && !trimmed.contains(".permission.")) {
                section = Section.NONE;
                continue;
            }

            switch (section) {
                case REQUESTED -> parseRequestedPermission(trimmed, permissions);
                case INSTALL -> parsePermissionState(trimmed, permissions, false);
                case RUNTIME -> parsePermissionState(trimmed, permissions, true);
                case NONE -> {
                }
            }
        }

        List<AppPermission> result = new ArrayList<>();
        for (PermissionStateBuilder builder : permissions.values()) {
            builder.applyAppOp(appOpsByName);
            result.add(builder.build());
        }
        return result;
    }

    private void parseRequestedPermission(String trimmedLine, Map<String, PermissionStateBuilder> permissions) {
        String permissionName = extractPermissionName(trimmedLine);
        if (permissionName == null) {
            return;
        }
        permissions.computeIfAbsent(permissionName, PermissionStateBuilder::new);
    }

    private void parsePermissionState(
            String trimmedLine,
            Map<String, PermissionStateBuilder> permissions,
            boolean runtime) {
        String permissionName = extractPermissionName(trimmedLine);
        if (permissionName == null) {
            return;
        }

        PermissionStateBuilder builder = permissions.computeIfAbsent(permissionName, PermissionStateBuilder::new);
        builder.granted = trimmedLine.contains("granted=true");
        builder.runtime = runtime;
        builder.flags = extractFlags(trimmedLine);
        builder.changeable = runtime && !builder.flags.contains("SYSTEM_FIXED") && !builder.flags.contains("POLICY_FIXED");
    }

    private String extractPermissionName(String rawLine) {
        String permissionName = rawLine;
        int separatorIndex = permissionName.indexOf(':');
        if (separatorIndex >= 0) {
            permissionName = permissionName.substring(0, separatorIndex).trim();
        }

        if (!permissionName.contains(".permission.")) {
            return null;
        }

        return permissionName;
    }

    private String extractFlags(String line) {
        int start = line.indexOf("flags=[");
        if (start < 0) {
            return "";
        }

        int end = line.indexOf(']', start);
        if (end < 0) {
            return "";
        }

        return line.substring(start + "flags=[".length(), end).trim();
    }

    private String findFirst(String output, Pattern pattern, String fallback) {
        if (output == null || output.isBlank()) {
            return fallback;
        }

        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            return value.isEmpty() ? fallback : value;
        }
        return fallback;
    }

    private String sanitizeLabel(String value) {
        String sanitized = value == null ? "" : value.trim();
        if (sanitized.startsWith("'") && sanitized.endsWith("'") && sanitized.length() > 1) {
            sanitized = sanitized.substring(1, sanitized.length() - 1);
        }
        return sanitized.trim();
    }

    private enum Section {
        NONE,
        REQUESTED,
        INSTALL,
        RUNTIME
    }

    private static final class PermissionStateBuilder {
        private final String name;
        private boolean granted;
        private boolean changeable;
        private boolean runtime;
        private String flags = "";
        private String appOp = "";

        private PermissionStateBuilder(String name) {
            this.name = name;
        }

        private void applyAppOp(Map<String, String> appOpsByName) {
            if (appOpsByName == null || appOpsByName.isEmpty()) {
                return;
            }

            String shortName = permissionSuffix(name);
            if (shortName.isBlank()) {
                return;
            }

            String mode = appOpsByName.get(shortName);
            if (mode == null) {
                return;
            }

            appOp = shortName;
            if (!runtime) {
                granted = "allow".equalsIgnoreCase(mode);
                changeable = !isFixed();
            }
        }

        private boolean isFixed() {
            return flags.contains("SYSTEM_FIXED") || flags.contains("POLICY_FIXED");
        }

        private AppPermission build() {
            return new AppPermission(name, granted, changeable, runtime, flags, appOp);
        }
    }

    private static String permissionSuffix(String permissionName) {
        if (permissionName == null || permissionName.isBlank()) {
            return "";
        }

        int lastDot = permissionName.lastIndexOf('.');
        return lastDot >= 0 && lastDot + 1 < permissionName.length()
                ? permissionName.substring(lastDot + 1).trim()
                : permissionName.trim();
    }
}
