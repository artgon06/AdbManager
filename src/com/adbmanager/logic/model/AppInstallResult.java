package com.adbmanager.logic.model;

import java.util.List;
import java.util.Objects;

public record AppInstallResult(List<AppInstallItemResult> items) {

    public AppInstallResult {
        items = items == null
                ? List.of()
                : items.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }

    public int successCount() {
        return (int) items.stream().filter(AppInstallItemResult::success).count();
    }

    public int failureCount() {
        return (int) items.stream().filter(item -> !item.success()).count();
    }

    public boolean allSucceeded() {
        return !items.isEmpty() && failureCount() == 0;
    }
}
