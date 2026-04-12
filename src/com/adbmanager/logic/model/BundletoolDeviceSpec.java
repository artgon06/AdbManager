package com.adbmanager.logic.model;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public record BundletoolDeviceSpec(
        List<String> supportedAbis,
        List<String> supportedLocales,
        int screenDensity,
        int sdkVersion) {

    public BundletoolDeviceSpec {
        supportedAbis = supportedAbis == null
                ? List.of()
                : supportedAbis.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .toList();
        supportedLocales = supportedLocales == null
                ? List.of()
                : supportedLocales.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .toList();
        screenDensity = Math.max(0, screenDensity);
        sdkVersion = Math.max(0, sdkVersion);
    }

    public byte[] toJsonBytes() {
        String json = "{\n"
                + "  \"supportedAbis\": " + toJsonArray(supportedAbis) + ",\n"
                + "  \"supportedLocales\": " + toJsonArray(supportedLocales) + ",\n"
                + "  \"screenDensity\": " + screenDensity + ",\n"
                + "  \"sdkVersion\": " + sdkVersion + "\n"
                + "}\n";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private String toJsonArray(List<String> values) {
        if (values.isEmpty()) {
            return "[]";
        }

        return values.stream()
                .map(value -> "\"" + escape(value) + "\"")
                .reduce((left, right) -> left + ", " + right)
                .map(result -> "[" + result + "]")
                .orElse("[]");
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
