package com.adbmanager.logic.model;

import java.util.List;
import java.util.Objects;

public record DeviceDirectoryListing(
        String currentPath,
        String parentPath,
        List<DeviceFileEntry> entries) {

    public DeviceDirectoryListing {
        currentPath = normalizePath(currentPath);
        parentPath = normalizePath(parentPath);
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public boolean hasParent() {
        return parentPath != null && !parentPath.isBlank() && !Objects.equals(parentPath, currentPath);
    }

    private static String normalizePath(String path) {
        if (path == null) {
            return null;
        }

        String normalized = path.trim().replace('\\', '/');
        if (normalized.isBlank()) {
            return null;
        }

        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
