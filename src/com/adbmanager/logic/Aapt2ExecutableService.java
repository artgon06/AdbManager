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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class Aapt2ExecutableService {

    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(90);
    private static final URI METADATA_URI = URI.create(
            "https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/maven-metadata.xml");
    private static final String DOWNLOAD_BASE_URI = "https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/";
    private static final Pattern VERSION_PATTERN = Pattern.compile("<version>([^<]+)</version>");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final HostPlatform hostPlatform;

    public Aapt2ExecutableService() {
        this(HostPlatform.current());
    }

    Aapt2ExecutableService(HostPlatform hostPlatform) {
        this.hostPlatform = hostPlatform;
    }

    public Path ensureAvailable() throws Exception {
        Path managedExecutable = managedExecutable();
        if (Files.isRegularFile(managedExecutable)) {
            ensureExecutablePermission(managedExecutable);
            return managedExecutable;
        }

        Path systemExecutable = resolveSystemExecutable();
        if (systemExecutable != null) {
            return systemExecutable;
        }

        return installManagedAapt2();
    }

    private Path resolveSystemExecutable() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(hostPlatform.lookupCommand(hostPlatform.executableName("aapt2")));
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        boolean finished = process.waitFor(5L, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return null;
        }

        String output;
        try (InputStream inputStream = process.getInputStream()) {
            output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        if (process.exitValue() != 0) {
            return null;
        }

        for (String rawLine : output.split("\\R")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (!line.isBlank()) {
                Path candidate = Path.of(line).toAbsolutePath().normalize();
                if (Files.isRegularFile(candidate)) {
                    ensureExecutablePermission(candidate);
                    return candidate;
                }
            }
        }

        return null;
    }

    private Path installManagedAapt2() throws Exception {
        Files.createDirectories(aapt2HomeDirectory());
        Path downloadFile = Files.createTempFile(aapt2HomeDirectory(), "aapt2-", ".jar");
        Path extractionDirectory = Files.createTempDirectory(aapt2HomeDirectory(), "aapt2-extract-");

        try {
            String version = resolvePreferredVersion();
            downloadArtifact(version, downloadFile);
            extractArchive(downloadFile, extractionDirectory);

            Path executable = extractionDirectory.resolve(hostPlatform.executableName("aapt2"));
            if (!Files.isRegularFile(executable)) {
                throw new Exception("El paquete descargado de AAPT2 no incluye el binario esperado.");
            }
            ensureExecutablePermission(executable);

            Path targetDirectory = managedDirectory();
            deleteRecursively(targetDirectory);
            Files.move(extractionDirectory, targetDirectory, StandardCopyOption.REPLACE_EXISTING);
            Path managedExecutable = targetDirectory.resolve(hostPlatform.executableName("aapt2"));
            ensureExecutablePermission(managedExecutable);
            return managedExecutable;
        } finally {
            Files.deleteIfExists(downloadFile);
            if (Files.exists(extractionDirectory)) {
                deleteRecursively(extractionDirectory);
            }
        }
    }

    private String resolvePreferredVersion() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(METADATA_URI)
                .GET()
                .timeout(DOWNLOAD_TIMEOUT)
                .header("Accept", "application/xml")
                .header("User-Agent", "ADB-Manager")
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new Exception("No se pudo consultar la versión disponible de AAPT2 (HTTP "
                    + response.statusCode() + ").");
        }

        String metadata;
        try (InputStream inputStream = response.body()) {
            metadata = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        List<String> versions = new ArrayList<>();
        Matcher matcher = VERSION_PATTERN.matcher(metadata);
        while (matcher.find()) {
            String version = matcher.group(1).trim();
            if (!version.isBlank()) {
                versions.add(version);
            }
        }

        for (int index = versions.size() - 1; index >= 0; index--) {
            String version = versions.get(index);
            if (!isPrerelease(version)) {
                return version;
            }
        }

        if (!versions.isEmpty()) {
            return versions.get(versions.size() - 1);
        }

        throw new Exception("No se pudo determinar ninguna versión válida de AAPT2.");
    }

    private boolean isPrerelease(String version) {
        String normalized = version == null ? "" : version.toLowerCase();
        return normalized.contains("alpha") || normalized.contains("beta") || normalized.contains("rc");
    }

    private void downloadArtifact(String version, Path targetFile) throws Exception {
        String fileName = "aapt2-" + version + "-" + hostPlatform.aapt2MavenClassifier() + ".jar";
        URI downloadUri = URI.create(DOWNLOAD_BASE_URI + version + "/" + fileName);

        HttpRequest request = HttpRequest.newBuilder(downloadUri)
                .GET()
                .timeout(DOWNLOAD_TIMEOUT)
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "ADB-Manager")
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new Exception("No se pudo descargar AAPT2 desde Google Maven (HTTP "
                    + response.statusCode() + ").");
        }

        try (InputStream inputStream = response.body()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void extractArchive(Path archiveFile, Path targetDirectory) throws Exception {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(archiveFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                Path targetFile = targetDirectory.resolve(entry.getName()).normalize();
                if (!targetFile.startsWith(targetDirectory)) {
                    throw new IOException("Entrada ZIP fuera del directorio de extracción: " + entry.getName());
                }

                Path parentDirectory = targetFile.getParent();
                if (parentDirectory != null) {
                    Files.createDirectories(parentDirectory);
                }
                Files.copy(zipInputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private Path aapt2HomeDirectory() {
        return Path.of(System.getProperty("user.home"), ".adbmanager", "tools", "aapt2");
    }

    private Path managedDirectory() {
        return aapt2HomeDirectory().resolve("managed");
    }

    private Path managedExecutable() {
        return managedDirectory().resolve(hostPlatform.executableName("aapt2"));
    }

    private void ensureExecutablePermission(Path executable) throws IOException {
        if (hostPlatform.isUnixLike() && Files.isRegularFile(executable)) {
            executable.toFile().setExecutable(true, false);
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
}
