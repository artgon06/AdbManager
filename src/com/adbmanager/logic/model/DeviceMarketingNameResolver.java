package com.adbmanager.logic.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class DeviceMarketingNameResolver {

    private static final String MODELS_RESOURCE = "/com/adbmanager/logic/data/device-marketing-models.tsv";
    private static final String CODENAMES_RESOURCE = "/com/adbmanager/logic/data/device-marketing-codenames.tsv";

    private final Map<String, CatalogEntry> modelsByKey;
    private final Map<String, CatalogEntry> codenamesByKey;

    public DeviceMarketingNameResolver() {
        this.modelsByKey = loadCatalog(
                MODELS_RESOURCE,
                Path.of("src", "com", "adbmanager", "logic", "data", "device-marketing-models.tsv"),
                Path.of("bin", "com", "adbmanager", "logic", "data", "device-marketing-models.tsv"),
                Path.of("com", "adbmanager", "logic", "data", "device-marketing-models.tsv"));
        this.codenamesByKey = loadCatalog(
                CODENAMES_RESOURCE,
                Path.of("src", "com", "adbmanager", "logic", "data", "device-marketing-codenames.tsv"),
                Path.of("bin", "com", "adbmanager", "logic", "data", "device-marketing-codenames.tsv"),
                Path.of("com", "adbmanager", "logic", "data", "device-marketing-codenames.tsv"));
    }

    public Resolution resolve(String model, String codename) {
        CatalogEntry entry = lookup(modelsByKey, model);
        if (entry != null) {
            return new Resolution(entry.brand(), entry.marketingName());
        }

        entry = lookup(codenamesByKey, codename);
        if (entry != null) {
            return new Resolution(entry.brand(), entry.marketingName());
        }

        return Resolution.empty();
    }

    private CatalogEntry lookup(Map<String, CatalogEntry> catalog, String key) {
        String normalizedKey = normalizeKey(key);
        if (normalizedKey == null) {
            return null;
        }
        return catalog.get(normalizedKey);
    }

    private Map<String, CatalogEntry> loadCatalog(String resourcePath, Path... fallbacks) {
        try (BufferedReader reader = openCatalog(resourcePath, fallbacks)) {
            Map<String, CatalogEntry> catalog = new HashMap<>();
            if (reader == null) {
                return catalog;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                String[] tokens = line.split("\\t", 3);
                if (tokens.length < 3) {
                    continue;
                }

                String key = normalizeKey(tokens[0]);
                String brand = normalizeValue(tokens[1]);
                String marketingName = normalizeValue(tokens[2]);
                if (key == null || marketingName == null) {
                    continue;
                }

                catalog.putIfAbsent(key, new CatalogEntry(brand, marketingName));
            }

            return catalog;
        } catch (IOException exception) {
            return Map.of();
        }
    }

    private BufferedReader openCatalog(String resourcePath, Path... fallbacks) throws IOException {
        InputStream resourceStream = DeviceMarketingNameResolver.class.getResourceAsStream(resourcePath);
        if (resourceStream != null) {
            return new BufferedReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
        }

        for (Path fallback : fallbacks) {
            if (!Files.isRegularFile(fallback)) {
                continue;
            }
            return Files.newBufferedReader(fallback, StandardCharsets.UTF_8);
        }

        return null;
    }

    private String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record Resolution(String brand, String marketingName) {
        private static Resolution empty() {
            return new Resolution(null, null);
        }
    }

    private record CatalogEntry(String brand, String marketingName) {
    }
}
