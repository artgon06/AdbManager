package com.adbmanager.logic.model;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DeviceTypeDetector {

    private static final String FEATURE_AUTOMOTIVE = "android.hardware.type.automotive";
    private static final String FEATURE_DESKTOP = "android.hardware.type.pc";
    private static final String FEATURE_EMBEDDED = "android.hardware.type.embedded";
    private static final String FEATURE_FOLDABLE = "android.hardware.sensor.hinge_angle";
    private static final String FEATURE_LEANBACK = "android.software.leanback";
    private static final String FEATURE_LEANBACK_ONLY = "android.software.leanback_only";
    private static final String FEATURE_TELEVISION = "android.hardware.type.television";
    private static final String FEATURE_WATCH = "android.hardware.type.watch";

    public DeviceType detect(Map<String, String> properties, String featuresOutput, DisplayInfo displayInfo) {
        Set<String> features = parseFeatures(featuresOutput);
        String characteristics = properties.getOrDefault("ro.build.characteristics", "")
                .toLowerCase(Locale.ROOT);

        if (hasFeature(features, characteristics, FEATURE_WATCH, "watch")) {
            return DeviceType.WATCH;
        }
        if (hasFeature(features, characteristics, FEATURE_AUTOMOTIVE, "automotive")) {
            return DeviceType.AUTOMOTIVE;
        }
        if (features.contains(FEATURE_TELEVISION)
                || features.contains(FEATURE_LEANBACK)
                || features.contains(FEATURE_LEANBACK_ONLY)
                || characteristics.contains("tv")) {
            return DeviceType.TV;
        }
        if (hasFeature(features, characteristics, FEATURE_DESKTOP, "pc")) {
            return DeviceType.DESKTOP;
        }
        if (hasFeature(features, characteristics, FEATURE_EMBEDDED, "embedded")) {
            return DeviceType.EMBEDDED;
        }
        if (features.contains(FEATURE_FOLDABLE)) {
            return DeviceType.FOLDABLE;
        }
        if (displayInfo.hasSmallestWidth() && displayInfo.smallestWidthDp() >= 600) {
            return DeviceType.TABLET;
        }
        if (characteristics.contains("tablet")) {
            return DeviceType.TABLET;
        }
        if (displayInfo.hasSmallestWidth()) {
            return DeviceType.PHONE;
        }
        if (!features.isEmpty() || !characteristics.isBlank()) {
            return DeviceType.DEVICE;
        }
        return DeviceType.UNKNOWN;
    }

    private boolean hasFeature(Set<String> features, String characteristics, String featureName, String fallbackToken) {
        return features.contains(featureName) || characteristics.contains(fallbackToken);
    }

    private Set<String> parseFeatures(String featuresOutput) {
        Set<String> features = new HashSet<>();
        for (String line : safe(featuresOutput).lines().toList()) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("feature:")) {
                continue;
            }
            features.add(trimmed.substring("feature:".length()));
        }
        return features;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
