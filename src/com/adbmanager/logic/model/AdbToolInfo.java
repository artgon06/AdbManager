package com.adbmanager.logic.model;

import java.util.Objects;

public record AdbToolInfo(
        String version,
        String installedAs,
        boolean supportsPair,
        boolean supportsMdns) {

    public AdbToolInfo {
        version = Objects.requireNonNullElse(version, "-");
        installedAs = Objects.requireNonNullElse(installedAs, "-");
    }

    public boolean supportsQrPairing() {
        return supportsPair && supportsMdns;
    }
}
