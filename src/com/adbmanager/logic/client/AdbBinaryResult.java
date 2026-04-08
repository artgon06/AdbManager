package com.adbmanager.logic.client;

public record AdbBinaryResult(int exitCode, byte[] output) {
    public boolean ok() {
        return exitCode == 0;
    }
}
