package com.adbmanager.logic.model;

public record WirelessEndpointDiscovery(
        WirelessDebugEndpoint pairingEndpoint,
        WirelessDebugEndpoint connectEndpoint) {

    public static WirelessEndpointDiscovery empty() {
        return new WirelessEndpointDiscovery(null, null);
    }

    public boolean hasPairingEndpoint() {
        return pairingEndpoint != null && pairingEndpoint.isValid();
    }

    public boolean hasConnectEndpoint() {
        return connectEndpoint != null && connectEndpoint.isValid();
    }
}
