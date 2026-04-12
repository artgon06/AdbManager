package com.adbmanager.logic;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.adbmanager.logic.model.UserConfig;
import com.adbmanager.view.Messages;
import com.adbmanager.view.swing.AppTheme;

public final class AdbExecutableService {

    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(90);
    private static final URI PLATFORM_TOOLS_WINDOWS_URI = URI.create(
            "https://dl.google.com/android/repository/platform-tools-latest-windows.zip");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final UserConfigService userConfigService = new UserConfigService();

    public Path ensureAvailable() throws Exception {
        UserConfig config = loadUserConfig();
        if (config.useCustomAdbPath()) {
            return validateConfiguredPath(config.customAdbPath());
        }

        Path managedExecutable = managedExecutable();
        if (Files.isRegularFile(managedExecutable)) {
            return managedExecutable;
        }

        Path systemExecutable = resolveSystemExecutable();
        if (systemExecutable != null) {
            return systemExecutable;
        }

        return installPlatformTools();
    }

    private UserConfig loadUserConfig() {
        try {
            return userConfigService.read();
        } catch (IOException exception) {
            return UserConfig.defaults(AppTheme.LIGHT, Messages.getLanguage());
        }
    }

    private Path validateConfiguredPath(String configuredPath) throws Exception {
        String rawValue = configuredPath == null ? "" : configuredPath.trim();
        if (rawValue.isBlank()) {
            throw new Exception("La ruta personalizada de adb est\u00e1 vac\u00eda.");
        }

        String normalizedValue = stripWrappingQuotes(rawValue);
        Path candidate = Path.of(normalizedValue).toAbsolutePath().normalize();
        if (Files.isDirectory(candidate)) {
            Path adbExe = candidate.resolve("adb.exe");
            if (Files.isRegularFile(adbExe)) {
                return adbExe;
            }

            Path adbBinary = candidate.resolve("adb");
            if (Files.isRegularFile(adbBinary)) {
                return adbBinary;
            }
        }

        if (!Files.isRegularFile(candidate)) {
            throw new Exception("La ruta personalizada de adb no es v\u00e1lida: " + candidate);
        }

        return candidate;
    }

    private String stripWrappingQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    private Path resolveSystemExecutable() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(List.of("where", "adb"));
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        boolean finished = process.waitFor(5L, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return null;
        }

        String output;
        try (InputStream inputStream = process.getInputStream()) {
            output = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        if (process.exitValue() != 0) {
            return null;
        }

        for (String rawLine : output.split("\\R")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (!line.isBlank()) {
                Path candidate = Path.of(line).toAbsolutePath().normalize();
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private Path installPlatformTools() throws Exception {
        Files.createDirectories(adbHomeDirectory());
        Path downloadFile = Files.createTempFile(adbHomeDirectory(), "platform-tools-", ".zip");
        Path extractionDirectory = Files.createTempDirectory(adbHomeDirectory(), "platform-tools-extract-");

        try {
            downloadPlatformTools(downloadFile);
            extractZip(downloadFile, extractionDirectory);

            Path executable = extractionDirectory.resolve("platform-tools").resolve("adb.exe");
            if (!Files.isRegularFile(executable)) {
                throw new Exception("El paquete descargado de Platform-Tools no incluye adb.exe.");
            }

            Path targetDirectory = managedDirectory();
            deleteRecursively(targetDirectory);
            Files.move(extractionDirectory, targetDirectory, StandardCopyOption.REPLACE_EXISTING);
            return targetDirectory.resolve("platform-tools").resolve("adb.exe");
        } finally {
            Files.deleteIfExists(downloadFile);
            if (Files.exists(extractionDirectory)) {
                deleteRecursively(extractionDirectory);
            }
        }
    }

    private void downloadPlatformTools(Path targetFile) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(PLATFORM_TOOLS_WINDOWS_URI)
                .GET()
                .timeout(DOWNLOAD_TIMEOUT)
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "ADB-Manager")
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new Exception("No se pudo descargar Android SDK Platform-Tools (HTTP "
                    + response.statusCode() + ").");
        }

        try (InputStream inputStream = response.body()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void extractZip(Path zipFile, Path targetDirectory) throws Exception {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                Path targetFile = targetDirectory.resolve(entry.getName()).normalize();
                if (!targetFile.startsWith(targetDirectory)) {
                    throw new IOException("Entrada ZIP fuera del directorio de extracci\u00f3n: " + entry.getName());
                }

                Path parentDirectory = targetFile.getParent();
                if (parentDirectory != null) {
                    Files.createDirectories(parentDirectory);
                }
                Files.copy(zipInputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private Path adbHomeDirectory() {
        return Path.of(System.getProperty("user.home"), ".adbmanager", "tools", "adb");
    }

    private Path managedDirectory() {
        return adbHomeDirectory().resolve("managed");
    }

    private Path managedExecutable() {
        return managedDirectory().resolve("platform-tools").resolve("adb.exe");
    }

    private void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(candidate -> {
                        try {
                            Files.deleteIfExists(candidate);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    });
        } catch (RuntimeException exception) {
            if (exception.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw exception;
        }
    }
}
