package com.adbmanager.logic.model;

import java.util.Locale;
import java.util.Map;

public enum AppBackgroundMode {
    UNRESTRICTED("apps.energy.unrestricted", 0, "allow", "allow"),
    OPTIMIZED("apps.energy.optimized", 1, "allow", "deny"),
    RESTRICTED("apps.energy.restricted", 2, "deny", "deny");

    private static final String RUN_ANY_IN_BACKGROUND = "RUN_ANY_IN_BACKGROUND";
    private static final String RUN_IN_BACKGROUND = "RUN_IN_BACKGROUND";

    private final String messageKey;
    private final int sliderValue;
    private final String runAnyMode;
    private final String runMode;

    AppBackgroundMode(String messageKey, int sliderValue, String runAnyMode, String runMode) {
        this.messageKey = messageKey;
        this.sliderValue = sliderValue;
        this.runAnyMode = runAnyMode;
        this.runMode = runMode;
    }

    public String messageKey() {
        return messageKey;
    }

    public int sliderValue() {
        return sliderValue;
    }

    public String runAnyMode() {
        return runAnyMode;
    }

    public String runMode() {
        return runMode;
    }

    public static AppBackgroundMode fromSliderValue(int sliderValue) {
        for (AppBackgroundMode mode : values()) {
            if (mode.sliderValue == sliderValue) {
                return mode;
            }
        }
        return OPTIMIZED;
    }

    public static AppBackgroundMode fromAppOps(Map<String, String> appOpsByName) {
        if (appOpsByName == null || appOpsByName.isEmpty()) {
            return OPTIMIZED;
        }

        String runAny = normalize(appOpsByName.get(RUN_ANY_IN_BACKGROUND));
        String run = normalize(appOpsByName.get(RUN_IN_BACKGROUND));
        boolean runAnyMissing = runAny.isBlank() || isDefault(runAny);
        boolean runMissing = run.isBlank() || isDefault(run);
        boolean runAnyAllow = isAllow(runAny);
        boolean runAllow = isAllow(run);
        boolean runAnyDenied = isDenied(runAny);
        boolean runDenied = isDenied(run);

        if (runAnyMissing && runMissing) {
            return OPTIMIZED;
        }
        if (runAnyAllow && (runAllow || runMissing)) {
            return UNRESTRICTED;
        }
        if (runAnyMissing && runAllow) {
            return UNRESTRICTED;
        }
        if (runAnyDenied && (runDenied || runMissing)) {
            return RESTRICTED;
        }
        if (runAnyDenied && runDenied) {
            return RESTRICTED;
        }
        return OPTIMIZED;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isAllow(String value) {
        return "allow".equals(value);
    }

    private static boolean isDefault(String value) {
        return "default".equals(value);
    }

    private static boolean isDenied(String value) {
        return "deny".equals(value) || "ignore".equals(value) || "errored".equals(value);
    }
}
