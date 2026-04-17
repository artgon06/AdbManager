package com.adbmanager.logic.model;

import java.util.Arrays;

public enum DevicePowerAction {
    POWER_OFF("power.action.powerOff", "power_off"),
    REBOOT_ANDROID("power.action.reboot", "reboot_android"),
    REBOOT_RECOVERY("power.action.recovery", "reboot_recovery"),
    REBOOT_BOOTLOADER("power.action.bootloader", "reboot_bootloader"),
    REBOOT_FASTBOOTD("power.action.fastbootd", "reboot_fastbootd"),
    REBOOT_DOWNLOAD("power.action.download", "reboot_download");

    private final String messageKey;
    private final String actionCommand;

    DevicePowerAction(String messageKey, String actionCommand) {
        this.messageKey = messageKey;
        this.actionCommand = actionCommand;
    }

    public String messageKey() {
        return messageKey;
    }

    public String actionCommand() {
        return actionCommand;
    }

    public static DevicePowerAction fromActionCommand(String actionCommand) {
        return Arrays.stream(values())
                .filter(action -> action.actionCommand.equals(actionCommand))
                .findFirst()
                .orElse(REBOOT_ANDROID);
    }
}
