package com.adbmanager.logic.model;

import java.security.SecureRandom;
import java.util.Objects;

public record WirelessPairingQrPayload(
        String serviceName,
        String password,
        String qrPayload) {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    public WirelessPairingQrPayload {
        serviceName = Objects.requireNonNullElse(serviceName, "");
        password = Objects.requireNonNullElse(password, "");
        qrPayload = Objects.requireNonNullElse(qrPayload, "");
    }

    public static WirelessPairingQrPayload random() {
        String serviceName = "studio-" + randomToken(8);
        String password = randomToken(12);
        String payload = "WIFI:T:ADB;S:" + serviceName + ";P:" + password + ";;";
        return new WirelessPairingQrPayload(serviceName, password, payload);
    }

    private static String randomToken(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(ALPHANUMERIC[RANDOM.nextInt(ALPHANUMERIC.length)]);
        }
        return builder.toString();
    }
}
