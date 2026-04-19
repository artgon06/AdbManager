package com.adbmanager.logic.model;

public record ControlState(
        int brightnessLevel,
        int brightnessMax,
        int mediaVolumeLevel,
        int mediaVolumeMax,
        DeviceSoundMode soundMode) {

    public ControlState {
        brightnessMax = Math.max(1, brightnessMax);
        mediaVolumeMax = Math.max(1, mediaVolumeMax);
        brightnessLevel = Math.max(0, Math.min(brightnessLevel, brightnessMax));
        mediaVolumeLevel = Math.max(0, Math.min(mediaVolumeLevel, mediaVolumeMax));
        soundMode = soundMode == null ? DeviceSoundMode.NORMAL : soundMode;
    }

    public static ControlState empty() {
        return new ControlState(0, 255, 0, 15, DeviceSoundMode.NORMAL);
    }
}
