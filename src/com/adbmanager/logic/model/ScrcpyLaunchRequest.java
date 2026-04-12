package com.adbmanager.logic.model;

import java.util.Objects;

public record ScrcpyLaunchRequest(
        LaunchTarget launchTarget,
        boolean fullscreen,
        Integer maxSize,
        Double maxFps,
        boolean recordEnabled,
        String recordPath,
        boolean startAppEnabled,
        String startApp,
        AudioSource audioSource,
        Integer virtualDisplayWidth,
        Integer virtualDisplayHeight,
        Integer virtualDisplayDpi,
        Integer cameraWidth,
        Integer cameraHeight,
        String cameraId,
        boolean readOnly,
        InputMode keyboardMode,
        InputMode mouseMode) {

    public ScrcpyLaunchRequest {
        launchTarget = Objects.requireNonNullElse(launchTarget, LaunchTarget.DEVICE_DISPLAY);
        recordPath = normalizeText(recordPath);
        startApp = normalizeText(startApp);
        audioSource = Objects.requireNonNullElse(audioSource, AudioSource.DEFAULT);
        cameraId = normalizeText(cameraId);
        keyboardMode = Objects.requireNonNullElse(keyboardMode, InputMode.DEFAULT);
        mouseMode = Objects.requireNonNullElse(mouseMode, InputMode.DEFAULT);
    }

    public boolean usesVirtualDisplay() {
        return launchTarget == LaunchTarget.VIRTUAL_DISPLAY;
    }

    public boolean usesCameraSource() {
        return launchTarget == LaunchTarget.CAMERA;
    }

    public boolean hasMaxSize() {
        return maxSize != null && maxSize > 0;
    }

    public boolean hasMaxFps() {
        return maxFps != null && maxFps > 0d;
    }

    public boolean hasRecordPath() {
        return recordEnabled && recordPath != null && !recordPath.isBlank();
    }

    public boolean hasStartApp() {
        return startAppEnabled && startApp != null && !startApp.isBlank();
    }

    public boolean hasVirtualDisplaySize() {
        return virtualDisplayWidth != null && virtualDisplayWidth > 0
                && virtualDisplayHeight != null && virtualDisplayHeight > 0;
    }

    public boolean hasPartialVirtualDisplaySize() {
        return (virtualDisplayWidth != null && virtualDisplayWidth > 0)
                ^ (virtualDisplayHeight != null && virtualDisplayHeight > 0);
    }

    public boolean hasVirtualDisplayDpi() {
        return virtualDisplayDpi != null && virtualDisplayDpi > 0;
    }

    public boolean hasCameraSize() {
        return cameraWidth != null && cameraWidth > 0
                && cameraHeight != null && cameraHeight > 0;
    }

    public boolean hasPartialCameraSize() {
        return (cameraWidth != null && cameraWidth > 0)
                ^ (cameraHeight != null && cameraHeight > 0);
    }

    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    public enum LaunchTarget {
        DEVICE_DISPLAY("scrcpy.target.display"),
        VIRTUAL_DISPLAY("scrcpy.target.virtual"),
        CAMERA("scrcpy.target.camera");

        private final String messageKey;

        LaunchTarget(String messageKey) {
            this.messageKey = messageKey;
        }

        public String messageKey() {
            return messageKey;
        }
    }

    public enum AudioSource {
        DEFAULT("scrcpy.audio.default", null, false),
        NONE("scrcpy.audio.none", null, true),
        OUTPUT("scrcpy.audio.output", "output", false),
        MIC("scrcpy.audio.mic", "mic", false);

        private final String messageKey;
        private final String cliValue;
        private final boolean noAudio;

        AudioSource(String messageKey, String cliValue, boolean noAudio) {
            this.messageKey = messageKey;
            this.cliValue = cliValue;
            this.noAudio = noAudio;
        }

        public String messageKey() {
            return messageKey;
        }

        public String cliValue() {
            return cliValue;
        }

        public boolean noAudio() {
            return noAudio;
        }
    }

    public enum InputMode {
        DEFAULT("scrcpy.input.default", null),
        SDK("scrcpy.input.sdk", "sdk"),
        UHID("scrcpy.input.uhid", "uhid"),
        AOA("scrcpy.input.aoa", "aoa"),
        DISABLED("scrcpy.input.disabled", "disabled");

        private final String messageKey;
        private final String cliValue;

        InputMode(String messageKey, String cliValue) {
            this.messageKey = messageKey;
            this.cliValue = cliValue;
        }

        public String messageKey() {
            return messageKey;
        }

        public String cliValue() {
            return cliValue;
        }
    }
}
