package com.adbmanager.logic.model;

import java.net.URI;

public record AppUpdateInfo(
        String currentVersion,
        String latestVersion,
        boolean updateAvailable,
        boolean forced,
        boolean prerelease,
        String releaseName,
        String releaseNotes,
        String assetName,
        long assetSize,
        String digest,
        URI downloadUri) {

    public boolean canInstall() {
        return downloadUri != null && assetName != null && !assetName.isBlank();
    }
}
