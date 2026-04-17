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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.adbmanager.logic.model.ScrcpyCamera;
import com.adbmanager.logic.model.ScrcpyLaunchRequest;
import com.adbmanager.logic.model.ScrcpyStatus;

public final class ScrcpyService {

    private static final Duration TOOL_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(90);
    private static final URI LATEST_RELEASE_URI = URI.create("https://api.github.com/repos/Genymobile/scrcpy/releases/latest");
    private static final Pattern VERSION_PATTERN = Pattern.compile("(?im)^scrcpy\\s+v?([^\\s]+)");
    private static final Pattern RELEASE_TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern RELEASE_ASSET_PATTERN = Pattern.compile(
            "\"name\"\\s*:\\s*\"([^\"]+)\"(?:(?!\"name\"\\s*:).)*?\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"",
            Pattern.DOTALL);
    private static final Pattern CAMERA_PREFIX_PATTERN = Pattern.compile("^(?:\\[[^\\]]+\\]\\s*)?(?:[A-Z]+:\\s*)?(.*)$");
    private static final Pattern CAMERA_COLON_PATTERN = Pattern.compile("^(\\d+)\\s*[:=-]\\s*(.+)$");
    private static final Pattern CAMERA_SPACE_PATTERN = Pattern.compile("^(\\d+)\\s+(.+)$");
    private static final Pattern CAMERA_ID_PATTERN = Pattern.compile("--camera-id=([^\\s]+)");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public ScrcpyStatus getStatus() throws Exception {
        cleanupExistingLaunchLogs();
        Path managedExecutable = managedExecutable();
        if (Files.isRegularFile(managedExecutable)) {
            return describeExecutable(managedExecutable, true);
        }

        Path systemExecutable = resolveSystemExecutable();
        if (systemExecutable != null) {
            return describeExecutable(systemExecutable, false);
        }

        return ScrcpyStatus.missing();
    }

    public ScrcpyStatus ensureAvailable() throws Exception {
        ScrcpyStatus currentStatus = getStatus();
        if (currentStatus.available()) {
            return currentStatus;
        }

        ReleaseAsset asset = fetchLatestReleaseAsset();
        Path installationDirectory = installRelease(asset);
        return describeExecutable(installationDirectory.resolve("scrcpy.exe"), true);
    }

    public List<ScrcpyCamera> listCameras(String serial) throws Exception {
        ScrcpyStatus status = ensureAvailable();
        Path executable = Path.of(status.executablePath());
        CommandResult result = runScrcpyCommand(
                executable,
                List.of("-s", serial, "--list-cameras"),
                TOOL_TIMEOUT);
        if (!result.ok()) {
            throw new Exception("scrcpy --list-cameras failed:\n" + result.output());
        }

        Map<String, ScrcpyCamera> camerasById = new LinkedHashMap<>();
        for (String rawLine : result.output().split("\\R")) {
            String normalizedLine = normalizeCameraLine(rawLine);
            if (normalizedLine.isBlank()) {
                continue;
            }

            Matcher explicitIdMatcher = CAMERA_ID_PATTERN.matcher(normalizedLine);
            if (explicitIdMatcher.find()) {
                String id = explicitIdMatcher.group(1).trim();
                camerasById.putIfAbsent(id, new ScrcpyCamera(id, normalizedLine));
                continue;
            }

            Matcher colonMatcher = CAMERA_COLON_PATTERN.matcher(normalizedLine);
            if (colonMatcher.matches()) {
                String id = colonMatcher.group(1).trim();
                String label = colonMatcher.group(2).trim();
                camerasById.putIfAbsent(id, new ScrcpyCamera(id, label));
                continue;
            }

            Matcher spaceMatcher = CAMERA_SPACE_PATTERN.matcher(normalizedLine);
            if (spaceMatcher.matches()) {
                String id = spaceMatcher.group(1).trim();
                String label = spaceMatcher.group(2).trim();
                camerasById.putIfAbsent(id, new ScrcpyCamera(id, label));
            }
        }

        return List.copyOf(camerasById.values());
    }

    public void launch(String serial, ScrcpyLaunchRequest request) throws Exception {
        ScrcpyStatus status = ensureAvailable();
        Path executable = Path.of(status.executablePath());
        List<String> command = buildLaunchCommand(executable, serial, request);
        Files.createDirectories(scrcpyHomeDirectory());
        Path logFile = scrcpyHomeDirectory().resolve("scrcpy-launch-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + ".log");
        cleanupOldLaunchLogs(logFile);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(executable.getParent().toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(logFile.toFile());

        Process process = processBuilder.start();
        boolean finishedQuickly = process.waitFor(1500L, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finishedQuickly) {
            return;
        }

        String output = safeReadString(logFile);
        if (process.exitValue() != 0) {
            throw new Exception("scrcpy failed:\n" + output);
        }
    }

    private List<String> buildLaunchCommand(Path executable, String serial, ScrcpyLaunchRequest request) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.add("-s");
        command.add(serial);

        if (request.fullscreen()) {
            command.add("--fullscreen");
        }

        if (request.shouldTurnScreenOff()) {
            command.add("--turn-screen-off");
        }

        switch (request.launchTarget()) {
            case VIRTUAL_DISPLAY -> {
                String newDisplayValue = buildNewDisplayValue(request);
                command.add(newDisplayValue.isBlank() ? "--new-display" : "--new-display=" + newDisplayValue);
            }
            case CAMERA -> {
                command.add("--video-source=camera");
                if (request.cameraId() != null && !request.cameraId().isBlank()) {
                    command.add("--camera-id=" + request.cameraId());
                }
                if (request.hasCameraSize()) {
                    command.add("--camera-size=" + request.cameraWidth() + "x" + request.cameraHeight());
                }
            }
            case DEVICE_DISPLAY -> {
            }
        }

        if (!request.usesCameraSource() && request.hasMaxSize()) {
            command.add("--max-size=" + request.maxSize());
        }

        if (request.hasMaxFps()) {
            command.add("--max-fps=" + formatFps(request.maxFps()));
        }

        if (request.audioSource().noAudio()) {
            command.add("--no-audio");
        } else if (request.audioSource().cliValue() != null) {
            command.add("--audio-source=" + request.audioSource().cliValue());
        }

        if (!request.usesCameraSource()) {
            if (request.readOnly()) {
                command.add("--no-control");
            } else {
                if (request.keyboardMode().cliValue() != null) {
                    command.add("--keyboard=" + request.keyboardMode().cliValue());
                }
                if (request.mouseMode().cliValue() != null) {
                    command.add("--mouse=" + request.mouseMode().cliValue());
                }
            }
        }

        if (!request.usesCameraSource() && request.hasStartApp()) {
            command.add("--start-app=" + request.startApp());
        }

        if (request.hasRecordPath()) {
            Path recordPath = Path.of(request.recordPath()).toAbsolutePath().normalize();
            Path parentDirectory = recordPath.getParent();
            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }
            command.add("--record=" + recordPath);
        }

        return command;
    }

    private String buildNewDisplayValue(ScrcpyLaunchRequest request) throws Exception {
        if (!request.hasVirtualDisplaySize()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(request.virtualDisplayWidth()).append("x").append(request.virtualDisplayHeight());
        if (request.hasVirtualDisplayDpi()) {
            builder.append("/").append(request.virtualDisplayDpi());
        }
        return builder.toString();
    }

    private String formatFps(Double maxFps) {
        double rounded = Math.rint(maxFps);
        if (Math.abs(maxFps - rounded) < 0.0001d) {
            return String.valueOf((long) rounded);
        }
        return String.format(Locale.US, "%.2f", maxFps);
    }

    private String normalizeCameraLine(String line) {
        if (line == null) {
            return "";
        }

        String trimmed = line.trim();
        if (trimmed.isBlank()) {
            return "";
        }

        Matcher matcher = CAMERA_PREFIX_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            trimmed = matcher.group(1).trim();
        }

        if (trimmed.isBlank()
                || trimmed.startsWith("scrcpy ")
                || trimmed.startsWith("List of")
                || trimmed.startsWith("Available")
                || trimmed.startsWith("*")) {
            return "";
        }

        return trimmed;
    }

    private ScrcpyStatus describeExecutable(Path executable, boolean managedInstallation) throws Exception {
        return new ScrcpyStatus(
                true,
                managedInstallation,
                detectVersion(executable),
                executable.toAbsolutePath().normalize().toString());
    }

    private String detectVersion(Path executable) throws Exception {
        CommandResult versionResult = runScrcpyCommand(executable, List.of("--version"), Duration.ofSeconds(5));
        if (!versionResult.ok()) {
            return "-";
        }

        Matcher matcher = VERSION_PATTERN.matcher(versionResult.output());
        return matcher.find() ? matcher.group(1).trim() : "-";
    }

    private Path resolveSystemExecutable() throws Exception {
        CommandResult result = runGenericCommand(List.of("where", "scrcpy"), Duration.ofSeconds(5), null);
        if (!result.ok()) {
            return null;
        }

        for (String rawLine : result.output().split("\\R")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (!line.isBlank()) {
                return Path.of(line);
            }
        }

        return null;
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
            throw new Exception("No se pudo consultar el último release oficial de scrcpy (HTTP "
                    + response.statusCode() + ").");
        }

        String responseBody = response.body() == null ? "" : response.body();
        Matcher tagMatcher = RELEASE_TAG_PATTERN.matcher(responseBody);
        String tag = tagMatcher.find() ? tagMatcher.group(1).trim() : "";

        Matcher assetMatcher = RELEASE_ASSET_PATTERN.matcher(responseBody);
        while (assetMatcher.find()) {
            String assetName = assetMatcher.group(1).trim();
            String downloadUrl = unescapeJson(assetMatcher.group(2));
            String normalizedName = assetName.toLowerCase(Locale.ROOT);
            if (normalizedName.startsWith("scrcpy-win64-") && normalizedName.endsWith(".zip")) {
                return new ReleaseAsset(tag, assetName, URI.create(downloadUrl));
            }
        }

        throw new Exception("No se encontró un paquete win64 oficial en el último release de scrcpy.");
    }

    private Path installRelease(ReleaseAsset asset) throws Exception {
        Files.createDirectories(scrcpyHomeDirectory());
        Path downloadFile = Files.createTempFile(scrcpyHomeDirectory(), "scrcpy-release-", ".zip");
        Path extractionDirectory = Files.createTempDirectory(scrcpyHomeDirectory(), "scrcpy-extract-");

        try {
            downloadAsset(asset, downloadFile);
            extractPortableZip(downloadFile, extractionDirectory);

            Path executable = extractionDirectory.resolve("scrcpy.exe");
            if (!Files.isRegularFile(executable)) {
                throw new Exception("El release descargado de scrcpy no incluye scrcpy.exe.");
            }

            Files.writeString(
                    extractionDirectory.resolve("version.txt"),
                    asset.tag() == null || asset.tag().isBlank() ? asset.fileName() : asset.tag(),
                    StandardCharsets.UTF_8);

            Path targetDirectory = managedDirectory();
            deleteRecursively(targetDirectory);
            Files.move(extractionDirectory, targetDirectory, StandardCopyOption.REPLACE_EXISTING);
            return targetDirectory;
        } finally {
            Files.deleteIfExists(downloadFile);
            if (Files.exists(extractionDirectory)) {
                deleteRecursively(extractionDirectory);
            }
        }
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
            throw new Exception("No se pudo descargar scrcpy desde GitHub (HTTP " + response.statusCode() + ").");
        }

        try (InputStream inputStream = response.body()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void extractPortableZip(Path zipFile, Path targetDirectory) throws Exception {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = stripTopLevelSegment(entry.getName());
                if (entryName.isBlank()) {
                    continue;
                }

                Path targetFile = targetDirectory.resolve(entryName).normalize();
                if (!targetFile.startsWith(targetDirectory)) {
                    throw new IOException("Entrada ZIP fuera del directorio de extracción: " + entry.getName());
                }

                Files.createDirectories(targetFile.getParent());
                Files.copy(zipInputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private String stripTopLevelSegment(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return "";
        }

        String normalized = entryName.replace('\\', '/');
        int separatorIndex = normalized.indexOf('/');
        return separatorIndex >= 0 ? normalized.substring(separatorIndex + 1) : normalized;
    }

    private Path scrcpyHomeDirectory() {
        return Path.of(System.getProperty("user.home"), ".adbmanager", "tools", "scrcpy");
    }

    private Path managedDirectory() {
        return scrcpyHomeDirectory().resolve("managed");
    }

    private Path managedExecutable() {
        return managedDirectory().resolve("scrcpy.exe");
    }

    private void cleanupOldLaunchLogs(Path latestLog) throws IOException {
        Files.createDirectories(scrcpyHomeDirectory());
        try (Stream<Path> files = Files.list(scrcpyHomeDirectory())) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("scrcpy-launch-"))
                    .filter(path -> !path.equals(latestLog))
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    public void cleanupExistingLaunchLogs() throws IOException {
        Files.createDirectories(scrcpyHomeDirectory());
        try (Stream<Path> files = Files.list(scrcpyHomeDirectory())) {
            List<Path> logs = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("scrcpy-launch-"))
                    .sorted((left, right) -> {
                        try {
                            return Files.getLastModifiedTime(right).compareTo(Files.getLastModifiedTime(left));
                        } catch (IOException exception) {
                            return right.getFileName().toString().compareTo(left.getFileName().toString());
                        }
                    })
                    .toList();

            for (int index = 1; index < logs.size(); index++) {
                try {
                    Files.deleteIfExists(logs.get(index));
                } catch (IOException ignored) {
                }
            }
        }
    }

    private CommandResult runScrcpyCommand(Path executable, List<String> args, Duration timeout) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(args);
        return runGenericCommand(command, timeout, executable.getParent());
    }

    private CommandResult runGenericCommand(List<String> command, Duration timeout, Path workingDirectory) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory.toFile());
        }

        Process process = processBuilder.start();
        try {
            boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
            }

            byte[] outputBytes;
            try (InputStream inputStream = process.getInputStream()) {
                outputBytes = inputStream.readAllBytes();
            }

            return new CommandResult(
                    finished ? process.exitValue() : -1,
                    new String(outputBytes, StandardCharsets.UTF_8));
        } finally {
            process.getOutputStream().close();
        }
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

    private String safeReadString(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

    private String unescapeJson(String value) {
        return value.replace("\\/", "/");
    }

    private record ReleaseAsset(String tag, String fileName, URI downloadUri) {
    }

    private record CommandResult(int exitCode, String output) {
        private boolean ok() {
            return exitCode == 0;
        }
    }
}
