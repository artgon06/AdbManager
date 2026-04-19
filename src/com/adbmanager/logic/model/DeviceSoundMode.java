package com.adbmanager.logic.model;

public enum DeviceSoundMode {
    NORMAL("control.sound.mode.normal", "NORMAL"),
    VIBRATE("control.sound.mode.vibrate", "VIBRATE"),
    SILENT("control.sound.mode.silent", "SILENT");

    private final String messageKey;
    private final String adbValue;

    DeviceSoundMode(String messageKey, String adbValue) {
        this.messageKey = messageKey;
        this.adbValue = adbValue;
    }

    public String messageKey() {
        return messageKey;
    }

    public String adbValue() {
        return adbValue;
    }

    public static DeviceSoundMode fromCommand(String command) {
        if (command == null) {
            return NORMAL;
        }
        for (DeviceSoundMode mode : values()) {
            if (mode.name().equalsIgnoreCase(command) || mode.adbValue.equalsIgnoreCase(command)) {
                return mode;
            }
        }
        return NORMAL;
    }
}
