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
    private static final Pattern BATTERY_LEVEL = Pattern.compile("^\\s*level:\\s*(\\d+)\\s*$", Pattern.MULTILINE);
    private static final Pattern BATTERY_SCALE = Pattern.compile("^\\s*scale:\\s*(\\d+)\\s*$", Pattern.MULTILINE);
    private final DisplayInfoParser displayInfoParser = new DisplayInfoParser();
    private final DeviceTypeDetector deviceTypeDetector = new DeviceTypeDetector();
    private final DeviceMarketingNameResolver marketingNameResolver = new DeviceMarketingNameResolver();

    public DeviceDetails parse(
            Device device,
            String getpropOutput,
            String meminfoOutput,
            String batteryOutput,
            String storageOutput,
            String featuresOutput,
            String wmSizeOutput,
            String wmDensityOutput,
            String displayOutput,
            String darkModeOutput,
            String screenOffTimeoutOutput) {
        Map<String, String> properties = parseProperties(getpropOutput);
        MemoryStats memoryStats = parseMemory(meminfoOutput);
        int batteryLevelPercent = parseBatteryLevel(batteryOutput);
        StorageStats storageStats = parseStorage(storageOutput);
        DisplayInfo displayInfo = displayInfoParser.parse(
                properties,
                wmSizeOutput,
                wmDensityOutput,
                displayOutput,
                darkModeOutput,
                screenOffTimeoutOutput);
        DeviceType deviceType = deviceTypeDetector.detect(properties, featuresOutput, displayInfo);

        String manufacturer = firstNonBlank(
                properties.get("ro.product.manufacturer"),
                properties.get("ro.product.brand"));
        String brand = firstNonBlank(
                properties.get("ro.product.brand"),
                manufacturer);
        String model = firstNonBlank(
                properties.get("ro.product.model"),
                device.model());
        String codename = firstNonBlank(
                properties.get("ro.product.device"),
                device.device());
        String productName = firstNonBlank(
                properties.get("ro.product.name"),
                device.product());
        String marketingName = buildMarketingNameValue(properties, manufacturer, brand, model, codename);
        String androidVersion = firstNonBlank(
                properties.get("ro.build.version.release"),
                properties.get("ro.build.version.release_or_codename"));
        String apiLevel = properties.get("ro.build.version.sdk");
        String soc = buildSocValue(properties);
        String architecture = buildArchitectureValue(properties);

        return new DeviceDetails(
                device.serial(),
                device.state(),
                safeValue(manufacturer),
                safeValue(brand),
                safeValue(model),
                safeValue(marketingName),
                safeValue(codename),
                safeValue(productName),
                safeValue(androidVersion),
                safeValue(apiLevel),
                safeValue(soc),
                safeValue(architecture),
                deviceType,
                displayInfo,
                memoryStats.totalMb(),
                memoryStats.usedMb(),
                batteryLevelPercent,
                storageStats.totalMb(),
                storageStats.usedMb());
    }

    public DeviceDetails fromDevice(Device device) {
        return new DeviceDetails(
                device.serial(),
                device.state(),
                "-",
                "-",
                safeValue(device.model()),
                safeValue(resolveFallbackMarketingName(device.model(), device.device())),
                safeValue(device.device()),
                safeValue(device.product()),
                "-",
                "-",
                "-",
                "-",
                DeviceType.UNKNOWN,
                DisplayInfo.empty(),
                -1,
                -1,
                -1,
                -1,
                -1);
    }

    private String buildMarketingNameValue(
            Map<String, String> properties,
            String manufacturer,
            String brand,
            String model,
            String codename) {
        String propertyMarketName = firstNonBlank(
                properties.get("ro.product.marketname"),
                properties.get("ro.vendor.product.display"),
                properties.get("ro.product.vendor.marketname"),
                properties.get("ro.config.marketing_name"));
        if (!isBlank(propertyMarketName)) {
            return propertyMarketName;
        }

        DeviceMarketingNameResolver.Resolution resolved = marketingNameResolver.resolve(model, codename);
        if (!isBlank(resolved.marketingName())) {
            return resolved.marketingName();
        }

        if (looksLikeMarketingName(model, manufacturer, brand)) {
            return model;
        }

        return model;
    }

    private String resolveFallbackMarketingName(String model, String codename) {
        DeviceMarketingNameResolver.Resolution resolved = marketingNameResolver.resolve(model, codename);
        if (!isBlank(resolved.marketingName())) {
            return resolved.marketingName();
        }
        return model;
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

    private int parseBatteryLevel(String batteryOutput) {
        long level = findLongValue(BATTERY_LEVEL, batteryOutput);
        long scale = findLongValue(BATTERY_SCALE, batteryOutput);
        if (level < 0 || scale <= 0) {
            return -1;
        }
        return (int) Math.max(0, Math.min(100, Math.round((level * 100.0) / scale)));
    }

    private StorageStats parseStorage(String storageOutput) {
        if (storageOutput == null || storageOutput.isBlank()) {
            return new StorageStats(-1, -1);
        }

        StorageStats fallback = new StorageStats(-1, -1);
        for (String rawLine : storageOutput.lines().toList()) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank() || line.startsWith("Filesystem")) {
                continue;
            }

            String[] tokens = line.split("\\s+");
            if (tokens.length < 4) {
                continue;
            }

            try {
                long totalKb = Long.parseLong(tokens[1]);
                long usedKb = Long.parseLong(tokens[2]);
                long totalMb = Math.round(totalKb / 1024.0);
                long usedMb = Math.round(usedKb / 1024.0);
                fallback = new StorageStats(totalMb, usedMb);

                String mountPoint = tokens[tokens.length - 1];
                if ("/data".equals(mountPoint)
                        || mountPoint.startsWith("/data/")
                        || "/storage/emulated".equals(mountPoint)
                        || mountPoint.startsWith("/storage/emulated/")) {
                    return fallback;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return fallback;
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

    private String buildArchitectureValue(Map<String, String> properties) {
        String abiList = firstNonBlank(
                properties.get("ro.product.cpu.abilist64"),
                properties.get("ro.product.cpu.abilist"),
                properties.get("ro.product.cpu.abi"));
        if (isBlank(abiList)) {
            return "-";
        }
        return abiList.replace(",", ", ");
    }

    private boolean looksLikeMarketingName(String model, String manufacturer, String brand) {
        if (isBlank(model)) {
            return false;
        }

        String normalized = model.trim();
        if (normalized.length() <= 2) {
            return false;
        }

        if (normalized.matches(".*\\s+.*")) {
            return true;
        }

        String normalizedManufacturer = firstNonBlank(manufacturer, brand);
        if (!isBlank(normalizedManufacturer) && normalized.regionMatches(true, 0, normalizedManufacturer, 0,
                Math.min(normalized.length(), normalizedManufacturer.length()))) {
            return true;
        }

        return !normalized.matches("^[A-Z]{1,5}[-_ ]?[A-Z0-9]{2,}.*$");
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

    private record StorageStats(long totalMb, long usedMb) {
    }
}
