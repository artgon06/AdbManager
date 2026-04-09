package com.adbmanager.logic.model;

public enum DeviceType {
    PHONE("device.type.phone"),
    TABLET("device.type.tablet"),
    FOLDABLE("device.type.foldable"),
    WATCH("device.type.watch"),
    TV("device.type.tv"),
    AUTOMOTIVE("device.type.automotive"),
    DESKTOP("device.type.desktop"),
    EMBEDDED("device.type.embedded"),
    DEVICE("device.type.device"),
    UNKNOWN("device.type.unknown");

    private final String messageKey;

    DeviceType(String messageKey) {
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
