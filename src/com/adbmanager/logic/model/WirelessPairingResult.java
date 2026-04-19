package com.adbmanager.logic.model;

public record WirelessPairingResult(
        boolean connectedAutomatically,
        WirelessDebugEndpoint connectEndpoint) {

    public static WirelessPairingResult empty() {
        return new WirelessPairingResult(false, null);
    }

    public boolean hasConnectEndpoint() {
        return connectEndpoint != null && connectEndpoint.isValid();
    }
}
