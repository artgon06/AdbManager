package com.adbmanager.logic.model;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceDetailsParser {

    private static final Pattern GETPROP_LINE = Pattern.compile("^\\[(.+?)\\]: \\[(.*?)\\]$");
    private static final Pattern MEM_TOTAL = Pattern.compile("^MemTotal:\\s+(\\d+)\\s+kB$", Pattern.MULTILINE);
    private static final Pattern MEM_AVAILABLE = Pattern.compile("^MemAvailable:\\s+(\\d+)\\s+kB$", Pattern.MULTILINE);
    private static final Pattern MEM_FREE = Pattern.compile("^MemFree:\\s+(\\d+)\\s+kB$", Pattern.MULTILINE);

    public DeviceDetails parse(Device device, String getpropOutput, String meminfoOutput) {
        Map<String, String> properties = parseProperties(getpropOutput);
        MemoryStats memoryStats = parseMemory(meminfoOutput);

        String manufacturer = firstNonBlank(
                properties.get("ro.product.manufacturer"),
                properties.get("ro.product.brand"));
        String brand = firstNonBlank(
                properties.get("ro.product.brand"),
                manufacturer);
        String model = firstNonBlank(
                properties.get("ro.product.marketname"),
                properties.get("ro.product.model"),
                device.model());
        String codename = firstNonBlank(
                properties.get("ro.product.device"),
                device.device());
        String productName = firstNonBlank(
                properties.get("ro.product.name"),
                device.product());
        String androidVersion = firstNonBlank(
                properties.get("ro.build.version.release"),
                properties.get("ro.build.version.release_or_codename"));
        String apiLevel = properties.get("ro.build.version.sdk");
        String soc = buildSocValue(properties);

        return new DeviceDetails(
                device.serial(),
                device.state(),
                safeValue(manufacturer),
                safeValue(brand),
                safeValue(model),
                safeValue(codename),
                safeValue(productName),
                safeValue(androidVersion),
                safeValue(apiLevel),
                safeValue(soc),
                memoryStats.totalMb(),
                memoryStats.usedMb());
    }

    public DeviceDetails fromDevice(Device device) {
        return new DeviceDetails(
                device.serial(),
                device.state(),
                "-",
                "-",
                safeValue(device.model()),
                safeValue(device.device()),
                safeValue(device.product()),
                "-",
                "-",
                "-",
                -1,
                -1);
    }

    private Map<String, String> parseProperties(String output) {
        Map<String, String> properties = new HashMap<>();

        for (String line : output.lines().toList()) {
            Matcher matcher = GETPROP_LINE.matcher(line.trim());
            if (!matcher.matches()) {
                continue;
            }

            properties.put(matcher.group(1), matcher.group(2));
        }

        return properties;
    }

    private MemoryStats parseMemory(String meminfoOutput) {
        long totalKb = findLongValue(MEM_TOTAL, meminfoOutput);
        long availableKb = findLongValue(MEM_AVAILABLE, meminfoOutput);

        if (availableKb < 0) {
            availableKb = findLongValue(MEM_FREE, meminfoOutput);
        }

        if (totalKb <= 0) {
            return new MemoryStats(-1, -1);
        }

        long totalMb = Math.round(totalKb / 1024.0);
        long availableMb = availableKb > 0 ? Math.round(availableKb / 1024.0) : -1;
        long usedMb = availableMb >= 0 ? Math.max(0, totalMb - availableMb) : -1;

        return new MemoryStats(totalMb, usedMb);
    }

    private long findLongValue(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return -1;
        }
        return Long.parseLong(matcher.group(1));
    }

    private String buildSocValue(Map<String, String> properties) {
        String socModel = firstNonBlank(properties.get("ro.soc.model"), properties.get("ro.board.platform"));
        String boardPlatform = properties.get("ro.board.platform");
        String hardware = properties.get("ro.hardware");

        if (isBlank(socModel) && isBlank(hardware)) {
            return "-";
        }

        if (!isBlank(boardPlatform) && !boardPlatform.equals(socModel)) {
            return socModel + " (" + boardPlatform + ")";
        }

        if (isBlank(socModel)) {
            return hardware;
        }

        if (!isBlank(hardware) && !hardware.equalsIgnoreCase(socModel) && !hardware.equalsIgnoreCase(boardPlatform)) {
            return socModel + " [" + hardware + "]";
        }

        return socModel;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String safeValue(String value) {
        return isBlank(value) ? "-" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record MemoryStats(long totalMb, long usedMb) {
    }
}
