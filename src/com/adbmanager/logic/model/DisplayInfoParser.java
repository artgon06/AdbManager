package com.adbmanager.logic.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisplayInfoParser {

    private static final Pattern PHYSICAL_SIZE = Pattern.compile("Physical size:\\s*(\\d+)x(\\d+)");
    private static final Pattern OVERRIDE_SIZE = Pattern.compile("Override size:\\s*(\\d+)x(\\d+)");
    private static final Pattern REAL_SIZE = Pattern.compile("real(?:=|\\s+)(\\d+)\\s*x\\s*(\\d+)");
    private static final Pattern PHYSICAL_DENSITY = Pattern.compile("Physical density:\\s*(\\d+)");
    private static final Pattern OVERRIDE_DENSITY = Pattern.compile("Override density:\\s*(\\d+)");
    private static final Pattern DISPLAY_DENSITY = Pattern.compile("density\\s*(?:=|\\s)(\\d+)\\b");
    private static final Pattern CURRENT_MODE_ID = Pattern.compile("\\bmode(?:Id)?\\s*(?:=|\\s)(\\d+)\\b");
    private static final Pattern SUPPORTED_MODE = Pattern.compile(
            "(?:Mode\\{|\\{)id=(\\d+).*?(?:fps=|refreshRate=)(\\d+(?:\\.\\d+)?)");
    private static final Pattern RENDER_FRAME_RATE = Pattern.compile("renderFrameRate\\s*(\\d+(?:\\.\\d+)?)");

    public DisplayInfo parse(
            Map<String, String> properties,
            String wmSizeOutput,
            String wmDensityOutput,
            String displayOutput,
            String darkModeOutput,
            String screenOffTimeoutOutput) {
        Size physicalSize = parseSize(PHYSICAL_SIZE, wmSizeOutput);
        Size overrideSize = parseSize(OVERRIDE_SIZE, wmSizeOutput);
        if (physicalSize == null) {
            physicalSize = parseSize(REAL_SIZE, displayOutput);
        }

        Integer physicalDensity = parseInteger(PHYSICAL_DENSITY, wmDensityOutput);
        Integer overrideDensity = parseInteger(OVERRIDE_DENSITY, wmDensityOutput);
        if (physicalDensity == null) {
            physicalDensity = parseInteger(DISPLAY_DENSITY, displayOutput);
        }
        if (physicalDensity == null) {
            physicalDensity = densityFromProperties(properties);
        }

        Size effectiveSize = overrideSize != null ? overrideSize : physicalSize;
        Integer densityDpi = overrideDensity != null ? overrideDensity : physicalDensity;
        Integer smallestWidthDp = calculateSmallestWidthDp(effectiveSize, densityDpi);
        Boolean darkModeEnabled = parseDarkModeState(darkModeOutput);
        Integer screenOffTimeoutMs = parseScreenOffTimeout(screenOffTimeoutOutput);
        RefreshInfo refreshInfo = parseRefreshInfo(displayOutput);

        return new DisplayInfo(
                effectiveSize == null ? null : effectiveSize.width(),
                effectiveSize == null ? null : effectiveSize.height(),
                physicalSize == null ? null : physicalSize.width(),
                physicalSize == null ? null : physicalSize.height(),
                densityDpi,
                physicalDensity,
                smallestWidthDp,
                screenOffTimeoutMs,
                darkModeEnabled,
                refreshInfo.currentRefreshRateHz(),
                refreshInfo.supportedRefreshRatesHz());
    }

    private Size parseSize(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(safe(content));
        if (!matcher.find()) {
            return null;
        }
        return new Size(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }

    private Integer parseInteger(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(safe(content));
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private Integer densityFromProperties(Map<String, String> properties) {
        String value = firstNonBlank(
                properties.get("qemu.sf.lcd_density"),
                properties.get("ro.sf.lcd_density"),
                properties.get("persist.sys.sf.lcd_density"));
        if (value == null) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer calculateSmallestWidthDp(Size size, Integer densityDpi) {
        if (size == null || densityDpi == null || densityDpi <= 0) {
            return null;
        }

        int smallestSidePx = Math.min(size.width(), size.height());
        return (int) Math.round((smallestSidePx * 160.0d) / densityDpi);
    }

    private RefreshInfo parseRefreshInfo(String displayOutput) {
        Matcher supportedModeMatcher = SUPPORTED_MODE.matcher(safe(displayOutput));
        Map<Integer, Double> refreshByModeId = new LinkedHashMap<>();
        List<Double> supportedRates = new ArrayList<>();

        while (supportedModeMatcher.find()) {
            Integer modeId = Integer.parseInt(supportedModeMatcher.group(1));
            Double refreshRate = Double.parseDouble(supportedModeMatcher.group(2));
            refreshByModeId.putIfAbsent(modeId, refreshRate);
            if (!supportedRates.contains(refreshRate)) {
                supportedRates.add(refreshRate);
            }
        }

        Double currentRefreshRate = parseDouble(RENDER_FRAME_RATE, displayOutput);
        if (currentRefreshRate == null) {
            Integer currentModeId = parseInteger(CURRENT_MODE_ID, displayOutput);
            if (currentModeId != null) {
                currentRefreshRate = refreshByModeId.get(currentModeId);
            }
        }
        if (currentRefreshRate == null && !supportedRates.isEmpty()) {
            currentRefreshRate = supportedRates.get(0);
        }

        return new RefreshInfo(currentRefreshRate, supportedRates);
    }

    private Boolean parseDarkModeState(String darkModeOutput) {
        String rawValue = safe(darkModeOutput).trim();
        if (rawValue.isBlank()) {
            return null;
        }

        return switch (rawValue) {
            case "2", "yes", "on" -> true;
            case "1", "no", "off" -> false;
            default -> null;
        };
    }

    private Integer parseScreenOffTimeout(String screenOffTimeoutOutput) {
        String rawValue = safe(screenOffTimeoutOutput).trim();
        if (rawValue.isBlank() || "null".equalsIgnoreCase(rawValue)) {
            return null;
        }

        try {
            int timeoutMs = Integer.parseInt(rawValue);
            return timeoutMs > 0 ? timeoutMs : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Double parseDouble(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(safe(content));
        if (!matcher.find()) {
            return null;
        }
        return Double.parseDouble(matcher.group(1));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record Size(int width, int height) {
    }

    private record RefreshInfo(Double currentRefreshRateHz, List<Double> supportedRefreshRatesHz) {
    }
}
