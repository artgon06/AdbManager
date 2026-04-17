package com.adbmanager.logic.model;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

public record AppDetails(
        InstalledApp app,
        String displayName,
        String versionName,
        String versionCode,
        String targetSdk,
        String minSdk,
        String installerPackage,
        String sourceDir,
        String dataDir,
        long codeSizeBytes,
        long dataSizeBytes,
        long cacheSizeBytes,
        boolean debuggable,
        AppBackgroundMode backgroundMode,
        List<AppPermission> permissions,
        BufferedImage iconImage) {

    public AppDetails {
        app = Objects.requireNonNull(app);
        displayName = normalize(displayName, app.displayName());
        versionName = normalize(versionName, "-");
        versionCode = normalize(versionCode, "-");
        targetSdk = normalize(targetSdk, "-");
        minSdk = normalize(minSdk, "-");
        installerPackage = normalize(installerPackage, "-");
        sourceDir = normalize(sourceDir, app.apkPath().isBlank() ? "-" : app.apkPath());
        dataDir = normalize(dataDir, "-");
        backgroundMode = backgroundMode == null ? AppBackgroundMode.OPTIMIZED : backgroundMode;
        permissions = List.copyOf(permissions == null ? List.of() : permissions);
    }

    public long totalStorageBytes() {
        long total = Math.max(0L, codeSizeBytes) + Math.max(0L, dataSizeBytes) + Math.max(0L, cacheSizeBytes);
        return total > 0L ? total : app.storageBytes();
    }

    public String totalStorageLabel() {
        return InstalledApp.formatBytes(totalStorageBytes());
    }

    public String codeSizeLabel() {
        return InstalledApp.formatBytes(codeSizeBytes);
    }

    public String dataSizeLabel() {
        return InstalledApp.formatBytes(dataSizeBytes);
    }

    public String cacheSizeLabel() {
        return InstalledApp.formatBytes(cacheSizeBytes);
    }

    public InstalledApp toListEntry() {
        return app.withDisplayName(displayName)
                .withStorageBytes(totalStorageBytes())
                .withIconImage(iconImage);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
