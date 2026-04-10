package com.adbmanager.logic.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppListParser {

    public List<InstalledApp> parseCatalog(
            String output,
            Map<String, Long> storageByPackage,
            Set<String> disabledPackages,
            Set<String> systemPackages) {
        List<InstalledApp> applications = new ArrayList<>();
        if (output == null || output.isBlank()) {
            return applications;
        }

        for (String line : output.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || !trimmed.startsWith("package:") || !trimmed.contains("=")) {
                continue;
            }

            int separatorIndex = trimmed.lastIndexOf('=');
            String apkPath = trimmed.substring("package:".length(), separatorIndex).trim();
            String packageName = trimmed.substring(separatorIndex + 1).trim();
            long storageBytes = storageByPackage.getOrDefault(packageName, 0L);

            applications.add(new InstalledApp(
                    packageName,
                    null,
                    apkPath,
                    storageBytes,
                    systemPackages.contains(packageName),
                    disabledPackages.contains(packageName)));
        }

        return applications;
    }

    public Set<String> parsePackageSet(String output) {
        Set<String> packages = new LinkedHashSet<>();
        if (output == null || output.isBlank()) {
            return packages;
        }

        for (String line : output.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("Error:")) {
                continue;
            }

            if (trimmed.startsWith("package:")) {
                packages.add(trimmed.substring("package:".length()).trim());
            } else {
                packages.add(trimmed);
            }
        }

        return packages;
    }

    public Map<String, Long> mapStorageByPackage(List<InstalledApp> applications) {
        Map<String, Long> storageByPackage = new LinkedHashMap<>();
        for (InstalledApp application : applications) {
            storageByPackage.put(application.packageName(), application.storageBytes());
        }
        return storageByPackage;
    }
}
