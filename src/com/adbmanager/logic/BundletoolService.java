package com.adbmanager.logic;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.adbmanager.logic.model.BundletoolDeviceSpec;

public final class BundletoolService {

    private static final Duration TOOL_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(90);
    private static final URI LATEST_RELEASE_URI = URI.create("https://api.github.com/repos/google/bundletool/releases/latest");
    private static final Pattern RELEASE_TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern RELEASE_ASSET_PATTERN = Pattern.compile(
            "\"name\"\\s*:\\s*\"([^\"]+)\"(?:(?!\"name\"\\s*:).)*?\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"",
            Pattern.DOTALL);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public Path ensureAvailable() throws Exception {
        Path managedJar = managedJar();
        if (Files.isRegularFile(managedJar)) {
            return managedJar;
        }

        ReleaseAsset asset = fetchLatestReleaseAsset();
        Files.createDirectories(bundletoolHomeDirectory());
        Path downloadTarget = bundletoolHomeDirectory().resolve(asset.fileName());
        downloadAsset(asset, downloadTarget);
        Files.move(downloadTarget, managedJar, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(managedVersionFile(), asset.tag(), StandardCharsets.UTF_8);
        cleanupOldJars(managedJar);
        return managedJar;
    }

    public Path buildDeviceApksFromBundle(
            Path bundleFile,
            BundletoolDeviceSpec deviceSpec,
            Path workingDirectory,
            Consumer<String> logConsumer) throws Exception {
        Path deviceSpecFile = writeDeviceSpec(deviceSpec, workingDirectory);
        Path outputApks = workingDirectory.resolve(stripExtension(bundleFile.getFileName().toString()) + ".apks");
        runBundletool(
                List.of(
                        "build-apks",
                        "--bundle=" + bundleFile.toAbsolutePath().normalize(),
                        "--output=" + outputApks.toAbsolutePath().normalize(),
                        "--device-spec=" + deviceSpecFile.toAbsolutePath().normalize(),
                        "--overwrite"),
                logConsumer);
        return outputApks;
    }

    public List<Path> extractDeviceApks(
            Path apksArchive,
            BundletoolDeviceSpec deviceSpec,
            Path workingDirectory,
            Consumer<String> logConsumer) throws Exception {
        Path deviceSpecFile = writeDeviceSpec(deviceSpec, workingDirectory);
        Path outputDirectory = workingDirectory.resolve("extracted-apks");
        Files.createDirectories(outputDirectory);
        runBundletool(
                List.of(
                        "extract-apks",
                        "--apks=" + apksArchive.toAbsolutePath().normalize(),
                        "--output-dir=" + outputDirectory.toAbsolutePath().normalize(),
                        "--device-spec=" + deviceSpecFile.toAbsolutePath().normalize()),
                logConsumer);

        try (Stream<Path> files = Files.walk(outputDirectory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".apk"))
                    .sorted()
                    .toList();
        }
    }

    private void runBundletool(List<String> arguments, Consumer<String> logConsumer) throws Exception {
        Path javaBinary = resolveJavaBinary();
        Path bundletoolJar = ensureAvailable();

        List<String> command = new ArrayList<>();
        command.add(javaBinary.toString());
        command.add("-jar");
        command.add(bundletoolJar.toAbsolutePath().normalize().toString());
        command.addAll(arguments);

        String output = runCommand(command, TOOL_TIMEOUT);
        if (logConsumer != null && output != null && !output.isBlank()) {
            for (String line : output.split("\\R")) {
                String normalized = line == null ? "" : line.trim();
                if (!normalized.isBlank()) {
                    logConsumer.accept(normalized);
                }
            }
        }
    }

    private Path writeDeviceSpec(BundletoolDeviceSpec deviceSpec, Path workingDirectory) throws IOException {
        Files.createDirectories(workingDirectory);
        Path deviceSpecFile = workingDirectory.resolve("device-spec.json");
        Files.write(deviceSpecFile, deviceSpec.toJsonBytes());
        return deviceSpecFile;
    }

    private ReleaseAsset fetchLatestReleaseAsset() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(LATEST_RELEASE_URI)
                .GET()
                .timeout(DOWNLOAD_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ADB-Manager")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new Exception("No se pudo consultar el último release oficial de bundletool (HTTP "
                    + response.statusCode() + ").");
        }

        String body = response.body() == null ? "" : response.body();
        Matcher tagMatcher = RELEASE_TAG_PATTERN.matcher(body);
        String tag = tagMatcher.find() ? tagMatcher.group(1).trim() : "";

        Matcher assetMatcher = RELEASE_ASSET_PATTERN.matcher(body);
        while (assetMatcher.find()) {
            String assetName = assetMatcher.group(1).trim();
            String downloadUrl = assetMatcher.group(2).replace("\\/", "/");
            if (assetName.toLowerCase(Locale.ROOT).startsWith("bundletool-all-")
                    && assetName.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return new ReleaseAsset(tag, assetName, URI.create(downloadUrl));
            }
        }

        throw new Exception("No se encontró el JAR oficial de bundletool en el último release.");
    }

    private void downloadAsset(ReleaseAsset asset, Path targetFile) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(asset.downloadUri())
                .GET()
                .timeout(DOWNLOAD_TIMEOUT)
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "ADB-Manager")
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new Exception("No se pudo descargar bundletool desde GitHub (HTTP " + response.statusCode() + ").");
        }

        try (InputStream inputStream = response.body()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String runCommand(List<String> command, Duration timeout) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try (InputStream inputStream = process.getInputStream()) {
            boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            String output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (!finished) {
                process.destroyForcibly();
                throw new Exception("bundletool superó el tiempo máximo de espera.");
            }
            if (process.exitValue() != 0) {
                throw new Exception(output == null || output.isBlank() ? "bundletool devolvió un error." : output.trim());
            }
            return output;
        } finally {
            process.getOutputStream().close();
        }
    }

    private Path resolveJavaBinary() {
        Path javaHome = Path.of(System.getProperty("java.home", ""));
        Path windowsJava = javaHome.resolve("bin").resolve("java.exe");
        if (Files.isRegularFile(windowsJava)) {
            return windowsJava;
        }

        Path genericJava = javaHome.resolve("bin").resolve("java");
        if (Files.isRegularFile(genericJava)) {
            return genericJava;
        }

        return Path.of("java");
    }

    private String stripExtension(String fileName) {
        int separatorIndex = fileName.lastIndexOf('.');
        return separatorIndex <= 0 ? fileName : fileName.substring(0, separatorIndex);
    }

    private Path bundletoolHomeDirectory() {
        return Path.of(System.getProperty("user.home"), ".adbmanager", "tools", "bundletool");
    }

    private Path managedJar() {
        return bundletoolHomeDirectory().resolve("bundletool-all.jar");
    }

    private Path managedVersionFile() {
        return bundletoolHomeDirectory().resolve("version.txt");
    }

    private void cleanupOldJars(Path keepJar) throws IOException {
        Files.createDirectories(bundletoolHomeDirectory());
        try (Stream<Path> files = Files.list(bundletoolHomeDirectory())) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .filter(path -> !path.equals(keepJar))
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private record ReleaseAsset(String tag, String fileName, URI downloadUri) {
    }
}
