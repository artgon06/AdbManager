package com.adbmanager.logic.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record AppInstallRequest(
        List<Path> packageFiles,
        boolean replaceExisting,
        boolean grantRuntimePermissions,
        boolean allowTestPackages,
        boolean bypassLowTargetSdkBlock) {

    public AppInstallRequest {
        packageFiles = packageFiles == null
                ? List.of()
                : packageFiles.stream()
                        .filter(Objects::nonNull)
                        .map(path -> path.toAbsolutePath().normalize())
                        .distinct()
                        .toList();
    }

    public boolean hasInputs() {
        return !packageFiles.isEmpty();
    }
}
