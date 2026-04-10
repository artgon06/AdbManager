package com.adbmanager.logic.model;

import java.util.Objects;

public record AppPermission(
        String name,
        boolean granted,
        boolean changeable,
        boolean runtime,
        String flags,
        String appOp) {

    public AppPermission {
        name = Objects.requireNonNullElse(name, "");
        flags = Objects.requireNonNullElse(flags, "");
        appOp = Objects.requireNonNullElse(appOp, "");
    }

    public boolean fixed() {
        return flags.contains("SYSTEM_FIXED") || flags.contains("POLICY_FIXED");
    }

    public boolean appOpControlled() {
        return !appOp.isBlank();
    }
}
