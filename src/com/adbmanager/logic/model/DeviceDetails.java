package com.adbmanager.logic.model;

import java.util.Objects;
import java.util.Locale;

public record DeviceDetails(
        String serial,
        String state,
        String manufacturer,
        String brand,
        String model,
        String codename,
        String productName,
        String androidVersion,
        String apiLevel,
        String soc,
        DeviceType deviceType,
        DisplayInfo displayInfo,
        long totalRamMb,
        long usedRamMb) {

    public DeviceDetails {
        deviceType = deviceType == null ? DeviceType.UNKNOWN : deviceType;
        displayInfo = Objects.requireNonNullElse(displayInfo, DisplayInfo.empty());
    }

    public boolean hasRamInfo() {
        return totalRamMb > 0;
    }

    public String totalRamLabel() {
        return formatMemory(totalRamMb);
    }

    public String usedRamLabel() {
        return formatMemory(usedRamMb);
    }

    public int ramUsagePercent() {
        if (!hasRamInfo()) {
            return 0;
        }
        return (int) Math.max(0, Math.min(100, Math.round((usedRamMb * 100.0) / totalRamMb)));
    }

    private String formatMemory(long memoryMb) {
        if (memoryMb <= 0) {
            return "-";
        }

        if (memoryMb >= 1024) {
            return String.format(Locale.US, "%.1f GB", memoryMb / 1024.0);
        }

        return memoryMb + " MB";
    }
}
