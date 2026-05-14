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
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.adbmanager.logic.model.AppUpdateInfo;

public final class AppUpdateService {

    private static final URI RELEASES_URI = URI.create(
            "https://api.github.com/repos/artgon06/AdbManager/releases?per_page=20");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern BODY_PATTERN = Pattern.compile("\"body\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
    private static final Pattern BOOL_PATTERN_TEMPLATE = Pattern.compile("\"%s\"\\s*:\\s*(true|false)");
    private static final Pattern ASSET_NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ASSET_URL_PATTERN = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ASSET_SIZE_PATTERN = Pattern.compile("\"size\"\\s*:\\s*(\\d+)");
    private static final Pattern ASSET_DIGEST_PATTERN = Pattern.compile("\"digest\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final HostPlatform hostPlatform;

    public AppUpdateService() {
        this(HostPlatform.current());
    }

    AppUpdateService(HostPlatform hostPlatform) {
        this.hostPlatform = hostPlatform;
    }

    public AppUpdateInfo checkForUpdates(String currentVersion, boolean force) throws Exception {
        if (!hostPlatform.isWindows()) {
            throw new UnsupportedOperationException("Las actualizaciones automáticas solo están disponibles en Windows por ahora.");
        }

        Release release = fetchBestRelease();
        ReleaseAsset asset = selectCompatibleAsset(release)
                .orElseThrow(() -> new Exception("No se encontró un instalador MSI compatible para Windows en el último release."));
        boolean newer = compareVersions(release.normalizedVersion(), normalizeVersion(currentVersion)) > 0;

        return new AppUpdateInfo(
                normalizeVersion(currentVersion),
                release.normalizedVersion(),
                newer || force,
                force,
                release.prerelease(),
                release.name(),
                release.body(),
                asset.name(),
                asset.size(),
                asset.digest(),
                asset.downloadUri());
    }

    public Path downloadInstaller(AppUpdateInfo updateInfo) throws Exception {
        if (updateInfo == null || !updateInfo.canInstall()) {
            throw new IllegalArgumentException("No hay un instalador disponible para descargar.");
        }

        Files.createDirectories(updatesDirectory());
        cleanupOldInstallers();

        String safeFileName = sanitizeFileName(updateInfo.assetName());
        Path targetFile = updatesDirectory().resolve(safeFileName);
        Path partialFile = updatesDirectory().resolve(safeFileName + ".download");

        HttpRequest request = HttpRequest.newBuilder(updateInfo.downloadUri())
                .GET()
                .timeout(Duration.ofMinutes(5))
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "ADB-Manager")
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new Exception("No se pudo descargar el instalador desde GitHub (HTTP " + response.statusCode() + ").");
        }

        try (InputStream inputStream = response.body()) {
            Files.copy(inputStream, partialFile, StandardCopyOption.REPLACE_EXISTING);
        }

        verifyDigestIfPresent(updateInfo, partialFile);
        Files.move(partialFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        return targetFile;
    }

    public boolean supportsCurrentPlatform() {
        return hostPlatform.isWindows();
    }

    public void launchInstallerAfterExit(Path installerPath) throws IOException {
        if (!hostPlatform.isWindows()) {
            throw new UnsupportedOperationException("Las actualizaciones automáticas solo están disponibles en Windows por ahora.");
        }
        if (installerPath == null || !Files.isRegularFile(installerPath)) {
            throw new IOException("No se encontró el instalador descargado.");
        }

        Files.createDirectories(updatesDirectory());
        Path helperScript = updatesDirectory().resolve("run-update.ps1");
        String script = """
                param(
                    [long] $PidToWait,
                    [string] $Installer
                )
                try {
                    Wait-Process -Id $PidToWait -Timeout 30 -ErrorAction SilentlyContinue
                } catch {
                }
                Start-Process -FilePath "msiexec.exe" -ArgumentList @("/i", $Installer, "/passive", "/norestart") -Wait
                """;
        Files.writeString(helperScript, script, StandardCharsets.UTF_8);

        new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                helperScript.toAbsolutePath().normalize().toString(),
                "-PidToWait",
                String.valueOf(ProcessHandle.current().pid()),
                "-Installer",
                installerPath.toAbsolutePath().normalize().toString())
                .redirectErrorStream(true)
                .start();
    }

    private Release fetchBestRelease() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(RELEASES_URI)
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ADB-Manager")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new Exception("No se pudieron consultar los releases de ADB Manager (HTTP "
                    + response.statusCode() + ").");
        }

        List<Release> releases = parseReleases(response.body() == null ? "" : response.body());
        return releases.stream()
                .filter(release -> !release.draft())
                .filter(release -> selectCompatibleAsset(release).isPresent())
                .max(Comparator.comparing(Release::normalizedVersion, this::compareVersions))
                .orElseThrow(() -> new Exception("No se encontró ningún release instalable para Windows."));
    }

    private List<Release> parseReleases(String body) {
        List<Release> releases = new ArrayList<>();
        Matcher matcher = TAG_PATTERN.matcher(body);
        List<Integer> starts = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
            tags.add(matcher.group(1));
        }

        for (int index = 0; index < starts.size(); index++) {
            int start = starts.get(index);
            int end = index + 1 < starts.size() ? starts.get(index + 1) : body.length();
            String section = body.substring(start, end);
            releases.add(new Release(
                    tags.get(index),
                    normalizeVersion(tags.get(index)),
                    extractFirst(NAME_PATTERN, section, tags.get(index)),
                    unescapeJson(extractFirst(BODY_PATTERN, section, "")),
                    extractBoolean(section, "draft"),
                    extractBoolean(section, "prerelease"),
                    parseAssets(section)));
        }
        return releases;
    }

    private List<ReleaseAsset> parseAssets(String section) {
        List<ReleaseAsset> assets = new ArrayList<>();
        Matcher nameMatcher = ASSET_NAME_PATTERN.matcher(section);
        while (nameMatcher.find()) {
            String assetSection = section.substring(nameMatcher.start(),
                    nextAssetStart(section, nameMatcher.end()));
            String name = nameMatcher.group(1);
            String url = extractFirst(ASSET_URL_PATTERN, assetSection, "");
            if (url.isBlank()) {
                continue;
            }
            long size = parseLong(extractFirst(ASSET_SIZE_PATTERN, assetSection, "0"));
            String digest = extractFirst(ASSET_DIGEST_PATTERN, assetSection, "");
            assets.add(new ReleaseAsset(name, URI.create(url.replace("\\/", "/")), size, digest));
        }
        return assets;
    }

    private int nextAssetStart(String section, int fromIndex) {
        Matcher matcher = ASSET_NAME_PATTERN.matcher(section);
        return matcher.find(fromIndex) ? matcher.start() : section.length();
    }

    private Optional<ReleaseAsset> selectCompatibleAsset(Release release) {
        if (!hostPlatform.isWindows()) {
            return Optional.empty();
        }

        String architecture = hostPlatform.architecture();
        return release.assets().stream()
                .filter(asset -> asset.name().toLowerCase(Locale.ROOT).endsWith(".msi"))
                .sorted(Comparator.comparingInt(asset -> assetScore(asset.name(), architecture)))
                .reduce((first, second) -> second);
    }

    private int assetScore(String assetName, String architecture) {
        String normalized = assetName.toLowerCase(Locale.ROOT);
        int score = 0;
        if (normalized.contains("windows") || normalized.contains("win")) {
            score += 4;
        }
        if (("x86_64".equals(architecture) || "amd64".equals(architecture)) && normalized.contains("x64")) {
            score += 3;
        }
        if ("aarch64".equals(architecture) && (normalized.contains("arm64") || normalized.contains("aarch64"))) {
            score += 3;
        }
        if (normalized.contains("adb") && normalized.contains("manager")) {
            score += 2;
        }
        return score;
    }

    private void verifyDigestIfPresent(AppUpdateInfo updateInfo, Path file) throws Exception {
        String digest = updateInfo.digest();
        if (digest == null || digest.isBlank() || !digest.toLowerCase(Locale.ROOT).startsWith("sha256:")) {
            return;
        }

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                sha256.update(buffer, 0, read);
            }
        }

        String expected = digest.substring("sha256:".length()).trim().toLowerCase(Locale.ROOT);
        String actual = HexFormat.of().formatHex(sha256.digest()).toLowerCase(Locale.ROOT);
        if (!expected.equals(actual)) {
            throw new Exception("La verificación SHA-256 del instalador descargado no coincide.");
        }
    }

    private int compareVersions(String left, String right) {
        int[] leftParts = parseVersionParts(left);
        int[] rightParts = parseVersionParts(right);
        int length = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < length; index++) {
            int leftValue = index < leftParts.length ? leftParts[index] : 0;
            int rightValue = index < rightParts.length ? rightParts[index] : 0;
            int comparison = Integer.compare(leftValue, rightValue);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private int[] parseVersionParts(String value) {
        String normalized = normalizeVersion(value);
        if (normalized.isBlank()) {
            return new int[] { 0 };
        }
        String[] rawParts = normalized.split("\\.");
        int[] parts = new int[rawParts.length];
        for (int index = 0; index < rawParts.length; index++) {
            parts[index] = (int) parseLong(rawParts[index].replaceAll("[^0-9].*$", ""));
        }
        return parts;
    }

    private String normalizeVersion(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }

    private String sanitizeFileName(String fileName) {
        String sanitized = fileName == null ? "ADB-Manager-update.msi" : fileName.trim();
        sanitized = sanitized.replaceAll("[\\\\/:*?\"<>|]", "-");
        return sanitized.isBlank() ? "ADB-Manager-update.msi" : sanitized;
    }

    private boolean extractBoolean(String section, String name) {
        return Boolean.parseBoolean(extractFirst(
                Pattern.compile(String.format(BOOL_PATTERN_TEMPLATE.pattern(), Pattern.quote(name))),
                section,
                "false"));
    }

    private String extractFirst(Pattern pattern, String text, String fallback) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value == null || value.isBlank() ? "0" : value.trim());
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private String unescapeJson(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\\\", "\\");
    }

    private Path updatesDirectory() {
        return Path.of(System.getProperty("user.home"), ".adbmanager", "updates");
    }

    private void cleanupOldInstallers() throws IOException {
        Files.createDirectories(updatesDirectory());
        try (Stream<Path> files = Files.list(updatesDirectory())) {
            files.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".msi") || name.endsWith(".download");
                    })
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private record Release(
            String tag,
            String normalizedVersion,
            String name,
            String body,
            boolean draft,
            boolean prerelease,
            List<ReleaseAsset> assets) {
    }

    private record ReleaseAsset(String name, URI downloadUri, long size, String digest) {
    }
}
