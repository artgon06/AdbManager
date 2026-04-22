package com.adbmanager.logic.model;

public record FileTransferProgress(
        long transferredBytes,
        long totalBytes,
        long bytesPerSecond,
        boolean indeterminate) {

    public FileTransferProgress {
        transferredBytes = Math.max(0L, transferredBytes);
        totalBytes = Math.max(0L, totalBytes);
        bytesPerSecond = Math.max(0L, bytesPerSecond);
    }

    public int percent() {
        if (indeterminate || totalBytes <= 0L) {
            return 0;
        }
        return (int) Math.max(0L, Math.min(100L, Math.round((transferredBytes * 100.0d) / totalBytes)));
    }
}
