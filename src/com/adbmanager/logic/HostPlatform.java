package com.adbmanager.logic;

import java.net.URI;
import java.util.List;
import java.util.Locale;

public record HostPlatform(OperatingSystem operatingSystem, String architecture) {

    private static final URI PLATFORM_TOOLS_WINDOWS_URI = URI.create(
            "https://dl.google.com/android/repository/platform-tools-latest-windows.zip");
    private static final URI PLATFORM_TOOLS_MACOS_URI = URI.create(
            "https://dl.google.com/android/repository/platform-tools-latest-darwin.zip");
    private static final URI PLATFORM_TOOLS_LINUX_URI = URI.create(
            "https://dl.google.com/android/repository/platform-tools-latest-linux.zip");

    public HostPlatform {
        operatingSystem = operatingSystem == null ? OperatingSystem.OTHER : operatingSystem;
        architecture = architecture == null ? "" : architecture;
    }

    public static HostPlatform current() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String architecture = normalizeArchitecture(System.getProperty("os.arch", ""));

        if (osName.contains("win")) {
            return new HostPlatform(OperatingSystem.WINDOWS, architecture);
        }
        if (osName.contains("mac") || osName.contains("darwin")) {
            return new HostPlatform(OperatingSystem.MACOS, architecture);
        }
        if (osName.contains("nux") || osName.contains("linux")) {
            return new HostPlatform(OperatingSystem.LINUX, architecture);
        }
        return new HostPlatform(OperatingSystem.OTHER, architecture);
    }

    private static String normalizeArchitecture(String rawArchitecture) {
        String normalized = rawArchitecture == null ? "" : rawArchitecture.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "x86_64", "amd64" -> "x86_64";
            case "aarch64", "arm64" -> "aarch64";
            default -> normalized;
        };
    }

    public boolean isWindows() {
        return operatingSystem == OperatingSystem.WINDOWS;
    }

    public boolean isMacos() {
        return operatingSystem == OperatingSystem.MACOS;
    }

    public boolean isLinux() {
        return operatingSystem == OperatingSystem.LINUX;
    }

    public boolean isUnixLike() {
        return isMacos() || isLinux();
    }

    public String executableName(String baseName) {
        return isWindows() ? baseName + ".exe" : baseName;
    }

    public List<String> lookupCommand(String executableName) {
        return isWindows() ? List.of("where", executableName) : List.of("which", executableName);
    }

    public URI platformToolsDownloadUri() {
        return switch (operatingSystem) {
            case WINDOWS -> PLATFORM_TOOLS_WINDOWS_URI;
            case MACOS -> PLATFORM_TOOLS_MACOS_URI;
            case LINUX -> PLATFORM_TOOLS_LINUX_URI;
            case OTHER -> throw new IllegalStateException(
                    "El sistema operativo actual no está soportado para descargar Android SDK Platform-Tools.");
        };
    }

    public String scrcpyAssetPrefix() {
        return switch (operatingSystem) {
            case WINDOWS -> "scrcpy-win64-";
            case MACOS -> switch (architecture) {
                case "aarch64" -> "scrcpy-macos-aarch64-";
                case "x86_64" -> "scrcpy-macos-x86_64-";
                default -> throw new IllegalStateException(
                        "No hay un paquete oficial de scrcpy para macOS con arquitectura " + architecture + ".");
            };
            case LINUX -> {
                if (!"x86_64".equals(architecture)) {
                    throw new IllegalStateException(
                            "No hay un paquete oficial de scrcpy para Linux con arquitectura " + architecture + ".");
                }
                yield "scrcpy-linux-x86_64-";
            }
            case OTHER -> throw new IllegalStateException(
                    "El sistema operativo actual no está soportado para descargar scrcpy.");
        };
    }

    public String scrcpyArchiveSuffix() {
        return isWindows() ? ".zip" : ".tar.gz";
    }

    public String aapt2MavenClassifier() {
        return switch (operatingSystem) {
            case WINDOWS -> "windows";
            case MACOS -> "osx";
            case LINUX -> "linux";
            case OTHER -> throw new IllegalStateException(
                    "El sistema operativo actual no está soportado para descargar AAPT2.");
        };
    }

    public enum OperatingSystem {
        WINDOWS,
        MACOS,
        LINUX,
        OTHER
    }
}
