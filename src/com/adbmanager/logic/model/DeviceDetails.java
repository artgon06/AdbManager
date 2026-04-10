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
        String architecture,
        DeviceType deviceType,
        DisplayInfo displayInfo,
        long totalRamMb,
        long usedRamMb,
        int batteryLevelPercent,
        long totalStorageMb,
        long usedStorageMb) {

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

    public boolean hasBatteryInfo() {
        return batteryLevelPercent >= 0;
    }

    public String batteryLabel() {
        return hasBatteryInfo() ? batteryLevelPercent + "%" : "-";
    }

    public boolean hasStorageInfo() {
        return totalStorageMb > 0;
    }

    public String totalStorageLabel() {
        return formatCapacity(totalStorageMb);
    }

    public String usedStorageLabel() {
        return formatCapacity(usedStorageMb);
    }

    public int storageUsagePercent() {
        if (!hasStorageInfo()) {
            return 0;
        }
        return (int) Math.max(0, Math.min(100, Math.round((usedStorageMb * 100.0) / totalStorageMb)));
    }

    private String formatCapacity(long capacityMb) {
        if (capacityMb <= 0) {
            return "-";
        }

        if (capacityMb >= 1024L * 1024L) {
            return String.format(Locale.US, "%.2f TB", capacityMb / (1024.0 * 1024.0));
        }

        if (capacityMb >= 1024) {
            return String.format(Locale.US, "%.1f GB", capacityMb / 1024.0);
        }

        return capacityMb + " MB";
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
