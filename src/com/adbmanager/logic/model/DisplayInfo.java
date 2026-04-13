package com.adbmanager.logic.model;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record DisplayInfo(
        Integer widthPx,
        Integer heightPx,
        Integer physicalWidthPx,
        Integer physicalHeightPx,
        Integer densityDpi,
        Integer physicalDensityDpi,
        Integer smallestWidthDp,
        Integer screenOffTimeoutMs,
        Boolean darkModeEnabled,
        Double refreshRateHz,
        List<Double> supportedRefreshRatesHz) {

    public DisplayInfo {
        refreshRateHz = refreshRateHz == null ? null : normalize(refreshRateHz);
        supportedRefreshRatesHz = supportedRefreshRatesHz == null
                ? List.of()
                : supportedRefreshRatesHz.stream()
                        .filter(Objects::nonNull)
                        .map(this::normalize)
                        .distinct()
                        .sorted(Comparator.naturalOrder())
                        .toList();
    }

    public static DisplayInfo empty() {
        return new DisplayInfo(null, null, null, null, null, null, null, null, null, null, List.of());
    }

    public boolean hasResolution() {
        return widthPx != null && heightPx != null;
    }

    public boolean hasPhysicalResolution() {
        return physicalWidthPx != null && physicalHeightPx != null;
    }

    public boolean hasDensity() {
        return densityDpi != null;
    }

    public boolean hasPhysicalDensity() {
        return physicalDensityDpi != null;
    }

    public boolean hasSmallestWidth() {
        return smallestWidthDp != null;
    }

    public boolean hasRefreshRate() {
        return refreshRateHz != null;
    }

    public boolean hasScreenOffTimeout() {
        return screenOffTimeoutMs != null && screenOffTimeoutMs > 0;
    }

    public boolean hasDarkModeState() {
        return darkModeEnabled != null;
    }

    public boolean hasSupportedRefreshRates() {
        return !supportedRefreshRatesHz.isEmpty();
    }

    public String resolutionLabel() {
        return hasResolution() ? widthPx + " x " + heightPx + " px" : "-";
    }

    public String physicalResolutionLabel() {
        return hasPhysicalResolution() ? physicalWidthPx + " x " + physicalHeightPx + " px" : "-";
    }

    public String densityLabel() {
        return hasDensity() ? densityDpi + " dpi" : "-";
    }

    public String physicalDensityLabel() {
        return hasPhysicalDensity() ? physicalDensityDpi + " dpi" : "-";
    }

    public String smallestWidthLabel() {
        return hasSmallestWidth() ? smallestWidthDp + " dp" : "-";
    }

    public String refreshRateLabel() {
        return hasRefreshRate() ? formatRate(refreshRateHz) : "-";
    }

    public String screenOffTimeoutLabel() {
        if (!hasScreenOffTimeout()) {
            return "-";
        }

        if (screenOffTimeoutMs % 60000 == 0) {
            long minutes = screenOffTimeoutMs / 60000L;
            return (screenOffTimeoutMs / 1000L) + " s (" + minutes + " min)";
        }

        if (screenOffTimeoutMs % 1000 == 0) {
            long seconds = screenOffTimeoutMs / 1000L;
            return seconds + " s";
        }

        return formatSeconds(screenOffTimeoutMs / 1000d) + " s";
    }

    public String supportedRefreshRatesLabel() {
        if (!hasSupportedRefreshRates()) {
            return "-";
        }

        return supportedRefreshRatesHz.stream()
                .map(this::formatRate)
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    public String darkModeLabel() {
        if (!hasDarkModeState()) {
            return "-";
        }
        return Boolean.TRUE.equals(darkModeEnabled) ? "on" : "off";
    }

    public String aspectRatioLabel() {
        return aspectRatioLabel(widthPx, heightPx);
    }

    public String physicalAspectRatioLabel() {
        return aspectRatioLabel(physicalWidthPx, physicalHeightPx);
    }

    public int effectiveSmallestSidePx() {
        if (!hasResolution()) {
            return 0;
        }
        return Math.min(widthPx, heightPx);
    }

    public int physicalSmallestSidePx() {
        if (!hasPhysicalResolution()) {
            return 0;
        }
        return Math.min(physicalWidthPx, physicalHeightPx);
    }

    public static String aspectRatioLabel(Integer widthPx, Integer heightPx) {
        if (widthPx == null || heightPx == null || widthPx <= 0 || heightPx <= 0) {
            return "-";
        }

        int gcd = gcd(widthPx, heightPx);
        return (widthPx / gcd) + ":" + (heightPx / gcd);
    }

    private String formatRate(double rate) {
        double rounded = Math.rint(rate);
        if (Math.abs(rate - rounded) < 0.05d) {
            return String.format(Locale.US, "%.0f Hz", rounded);
        }
        return String.format(Locale.US, "%.2f Hz", rate);
    }

    private String formatSeconds(double seconds) {
        double rounded = Math.rint(seconds);
        if (Math.abs(seconds - rounded) < 0.001d) {
            return String.format(Locale.US, "%.0f", rounded);
        }
        return String.format(Locale.US, "%.3f", seconds).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private double normalize(double rate) {
        return Math.round(rate * 100.0d) / 100.0d;
    }

    private static int gcd(int left, int right) {
        int a = Math.abs(left);
        int b = Math.abs(right);
        while (b != 0) {
            int tmp = a % b;
            a = b;
            b = tmp;
        }
        return a == 0 ? 1 : a;
    }
}
