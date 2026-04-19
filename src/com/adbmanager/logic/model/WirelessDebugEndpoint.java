package com.adbmanager.logic.model;

import java.util.Objects;

public record WirelessDebugEndpoint(
        String serviceName,
        String serviceType,
        String host,
        int port) {

    public WirelessDebugEndpoint {
        serviceName = Objects.requireNonNullElse(serviceName, "").trim();
        serviceType = Objects.requireNonNullElse(serviceType, "").trim();
        host = Objects.requireNonNullElse(host, "").trim();
        port = Math.max(0, port);
    }

    public boolean isValid() {
        return !host.isBlank() && port > 0;
    }

    public String endpoint() {
        return host + ":" + port;
    }
}
