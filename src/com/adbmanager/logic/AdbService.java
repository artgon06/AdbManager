package com.adbmanager.logic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;

import com.adbmanager.logic.client.AdbBinaryResult;
import com.adbmanager.logic.client.AdbClient;
import com.adbmanager.logic.client.AdbResult;
import com.adbmanager.logic.model.AppDetails;
import com.adbmanager.logic.model.AdbToolInfo;
import com.adbmanager.logic.model.AppDetailsParser;
import com.adbmanager.logic.model.AppListParser;
import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.logic.model.DeviceDetailsParser;
import com.adbmanager.logic.model.DeviceParser;
import com.adbmanager.logic.model.InstalledApp;
import com.adbmanager.view.Messages;

public class AdbService implements AdbModel {

    private static final String PRIMARY_USER_ID = "0";
    private static final int APK_SIZE_BATCH_SIZE = 24;
    private static final Duration LOCAL_TOOL_TIMEOUT = Duration.ofSeconds(20);
    private static final Pattern APP_OP_PATTERN = Pattern.compile("^([A-Z0-9_]+):\\s*(allow|ignore|deny|default)\\b");
    private static final Pattern BADGING_LABEL_PATTERN = Pattern.compile("^application-label(?:-[^:]+)?:'(.*?)'$");
    private static final Pattern BADGING_ICON_PATTERN = Pattern.compile("^application-icon-(\\d+):'(.*?)'$");
    private static final Pattern BADGING_FALLBACK_ICON_PATTERN = Pattern.compile("icon='(.*?)'");
    private static final Pattern RESOURCE_SPEC_PATTERN = Pattern.compile("spec resource 0x([0-9a-fA-F]+) [^:]+:([A-Za-z0-9_./-]+)");
    private static final Pattern RESOURCE_COLOR_VALUE_PATTERN = Pattern.compile(
            "resource 0x([0-9a-fA-F]+) [^:]+:color/[^:]+: t=0x[0-9a-fA-F]+ d=0x([0-9a-fA-F]+)");
    private static final Pattern XMLTREE_DRAWABLE_REFERENCE_PATTERN = Pattern.compile(
            "android:drawable\\([^)]*\\)=@0x([0-9a-fA-F]+)");
    private static final Pattern XMLTREE_VIEWPORT_WIDTH_PATTERN = Pattern.compile(
            "android:viewportWidth\\([^)]*\\)=\\(type 0x4\\)0x([0-9a-fA-F]+)");
    private static final Pattern XMLTREE_VIEWPORT_HEIGHT_PATTERN = Pattern.compile(
            "android:viewportHeight\\([^)]*\\)=\\(type 0x4\\)0x([0-9a-fA-F]+)");
    private static final Pattern XMLTREE_PATH_DATA_PATTERN = Pattern.compile(
            "android:pathData\\([^)]*\\)=\"(.*?)\"");
    private static final Pattern XMLTREE_FILL_COLOR_PATTERN = Pattern.compile(
            "android:fillColor\\([^)]*\\)(?:=@0x([0-9a-fA-F]+)|=\\(type 0x[0-9a-fA-F]+\\)0x([0-9a-fA-F]+)|=0x([0-9a-fA-F]+))");
    private static final Pattern ENABLE_COMPONENT_STATE_FAILURE_PATTERN = Pattern.compile(
            "Shell cannot change component state", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADB_VERSION_PATTERN = Pattern.compile(
            "^Android Debug Bridge version (.+)$", Pattern.MULTILINE);
    private static final Pattern ADB_INSTALLED_AS_PATTERN = Pattern.compile(
            "^Installed as (.+)$", Pattern.MULTILINE);
    private static final Pattern ADB_HELP_PAIR_PATTERN = Pattern.compile("(?m)^\\s*pair\\b");
    private static final Pattern ADB_HELP_MDNS_PATTERN = Pattern.compile("(?m)^\\s*mdns\\b");
    private static final Pattern MDNS_ENDPOINT_PATTERN = Pattern.compile("((?:\\d{1,3}\\.){3}\\d{1,3}):(\\d+)");

    private final AdbClient client;
    private final DeviceParser parser = new DeviceParser();
    private final DeviceDetailsParser detailsParser = new DeviceDetailsParser();
    private final AppListParser appListParser = new AppListParser();
    private final AppDetailsParser appDetailsParser = new AppDetailsParser();
    private final Map<String, ApplicationPresentation> applicationPresentationCache = new HashMap<>();
    private final Map<String, InstalledApp> applicationSummaryCache = new HashMap<>();
    private List<Device> devices = List.of();
    private List<InstalledApp> applications = List.of();
    private String selectedDeviceSerial;

    public AdbService(AdbClient client) {
        this.client = client;
    }

    @Override
    public void refreshDevices() throws Exception {
        AdbResult result = client.run(List.of("devices", "-l"));
        if (!result.ok()) {
            throw new Exception("adb devices -l failed:\n" + result.output());
        }

        devices = parser.parseDevices(result.output());
        syncSelectedDevice();
    }

    @Override
    public List<Device> getDevices() {
        return devices;
    }

    @Override
    public Optional<Device> getSelectedDevice() {
        if (selectedDeviceSerial == null) {
            return Optional.empty();
        }

        return devices.stream()
                .filter(device -> device.serial().equals(selectedDeviceSerial))
                .findFirst();
    }

    @Override
    public Device selectDeviceByIndex(int index) {
        if (index < 0 || index >= devices.size()) {
            throw new IllegalArgumentException(Messages.format("error.invalidDeviceIndex", index + 1));
        }

        Device selectedDevice = devices.get(index);
        updateSelectedDeviceSerial(selectedDevice.serial());
        return selectedDevice;
    }

    @Override
    public Device selectDeviceBySerial(String serial) {
        Device selectedDevice = devices.stream()
                .filter(device -> device.serial().equals(serial))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(Messages.format("error.deviceNotFound", serial)));

        updateSelectedDeviceSerial(selectedDevice.serial());
        return selectedDevice;
    }

    @Override
    public Optional<DeviceDetails> getSelectedDeviceDetails() throws Exception {
        Optional<Device> selectedDevice = getSelectedDevice();
        if (selectedDevice.isEmpty()) {
            return Optional.empty();
        }

        Device device = selectedDevice.get();
        if (!Messages.STATUS_CONNECTED.equals(device.state())) {
            return Optional.of(detailsParser.fromDevice(device));
        }

        AdbResult propertiesResult = client.runForSerial(device.serial(), List.of("shell", "getprop"));
        if (!propertiesResult.ok()) {
            throw new Exception("adb -s " + device.serial() + " shell getprop failed:\n" + propertiesResult.output());
        }

        AdbResult memoryResult = client.runForSerial(device.serial(), List.of("shell", "cat", "/proc/meminfo"));
        if (!memoryResult.ok()) {
            throw new Exception("adb -s " + device.serial() + " shell cat /proc/meminfo failed:\n" + memoryResult.output());
        }

        AdbResult batteryResult = client.runForSerial(device.serial(), List.of("shell", "dumpsys", "battery"));
        if (!batteryResult.ok()) {
            throw new Exception("adb -s " + device.serial() + " shell dumpsys battery failed:\n"
                    + batteryResult.output());
        }

        AdbResult storageResult = client.runForSerial(device.serial(), List.of("shell", "df", "-k", "/data"));
        if (!storageResult.ok()) {
            throw new Exception("adb -s " + device.serial() + " shell df -k /data failed:\n"
                    + storageResult.output());
        }

        AdbResult featuresResult = client.runForSerial(device.serial(), List.of("shell", "pm", "list", "features"));
        if (!featuresResult.ok()) {
            throw new Exception("adb -s " + device.serial() + " shell pm list features failed:\n" + featuresResult.output());
        }

        AdbResult sizeResult = client.runForSerial(device.serial(), List.of("shell", "wm", "size"));
        if (!sizeResult.ok()) {
            throw new Exception("adb -s " + device.serial() + " shell wm size failed:\n" + sizeResult.output());
        }

        AdbResult densityResult = client.runForSerial(device.serial(), List.of("shell", "wm", "density"));
        if (!densityResult.ok()) {
            throw new Exception("adb -s " + device.serial() + " shell wm density failed:\n" + densityResult.output());
        }

        AdbResult displayResult = client.runForSerial(device.serial(), List.of("shell", "dumpsys", "display"));
        if (!displayResult.ok()) {
            throw new Exception("adb -s " + device.serial() + " shell dumpsys display failed:\n" + displayResult.output());
        }

        AdbResult darkModeResult = client.runForSerial(
                device.serial(),
                List.of("shell", "settings", "get", "secure", "ui_night_mode"));
        if (!darkModeResult.ok()) {
            throw new Exception("adb -s " + device.serial() + " shell settings get secure ui_night_mode failed:\n"
                    + darkModeResult.output());
        }

        return Optional.of(detailsParser.parse(
                device,
                propertiesResult.output(),
                memoryResult.output(),
                batteryResult.output(),
                storageResult.output(),
                featuresResult.output(),
                sizeResult.output(),
                densityResult.output(),
                displayResult.output(),
                darkModeResult.output()));
    }

    @Override
    public AdbToolInfo getAdbToolInfo() throws Exception {
        AdbResult helpResult = client.run(List.of("help"));
        assertOk(helpResult, "adb help");
        String helpOutput = helpResult.output() == null ? "" : helpResult.output();
        return new AdbToolInfo(
                findFirstGroup(ADB_VERSION_PATTERN, helpOutput, "-"),
                findFirstGroup(ADB_INSTALLED_AS_PATTERN, helpOutput, "-"),
                ADB_HELP_PAIR_PATTERN.matcher(helpOutput).find(),
                ADB_HELP_MDNS_PATTERN.matcher(helpOutput).find());
    }

    @Override
    public byte[] captureSelectedDeviceScreenshot() throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.capture.selectDevice"),
                Messages.text("error.capture.deviceDisconnected"));

        AdbBinaryResult screenshotResult = client.runBinaryForSerial(
                device.serial(),
                List.of("exec-out", "screencap", "-p"));

        if (!screenshotResult.ok()) {
            throw new Exception("adb -s " + device.serial() + " exec-out screencap -p failed.");
        }

        return screenshotResult.output();
    }

    @Override
    public List<InstalledApp> getSelectedDeviceApplications() throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.apps.deviceRequired"),
                Messages.text("error.apps.deviceDisconnected"));

        AdbResult catalogResult = client.runForSerial(
                device.serial(),
                List.of("shell", "pm", "list", "packages", "-f", "--user", PRIMARY_USER_ID));
        AdbResult disabledResult = client.runForSerial(
                device.serial(),
                List.of("shell", "pm", "list", "packages", "-d", "--user", PRIMARY_USER_ID));
        AdbResult systemResult = client.runForSerial(
                device.serial(),
                List.of("shell", "pm", "list", "packages", "-s", "--user", PRIMARY_USER_ID));

        assertOk(catalogResult, "adb -s " + device.serial() + " shell pm list packages -f --user " + PRIMARY_USER_ID);
        assertOk(disabledResult, "adb -s " + device.serial() + " shell pm list packages -d --user " + PRIMARY_USER_ID);
        assertOk(systemResult, "adb -s " + device.serial() + " shell pm list packages -s --user " + PRIMARY_USER_ID);

        Map<String, Long> storageByPackage = measurePackageApkSizes(device.serial(), catalogResult.output());

        List<InstalledApp> loadedApplications = new ArrayList<>(appListParser.parseCatalog(
                catalogResult.output(),
                storageByPackage,
                appListParser.parsePackageSet(disabledResult.output()),
                appListParser.parsePackageSet(systemResult.output())));
        loadedApplications.replaceAll(this::applyCachedSummary);
        loadedApplications.sort(Comparator
                .comparing(InstalledApp::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(InstalledApp::packageName, String.CASE_INSENSITIVE_ORDER));

        applications = List.copyOf(loadedApplications);
        return applications;
    }

    @Override
    public InstalledApp getSelectedDeviceApplicationSummary(String packageName) throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.apps.deviceRequired"),
                Messages.text("error.apps.deviceDisconnected"));
        String targetPackage = requirePackageName(packageName);

        InstalledApp baseApp = applications.stream()
                .filter(application -> application.packageName().equals(targetPackage))
                .findFirst()
                .orElse(new InstalledApp(targetPackage, null, "", 0L, false, false));

        AdbResult dumpResult = client.runForSerial(device.serial(), List.of("shell", "dumpsys", "package", targetPackage));
        AdbResult pathResult = client.runForSerial(
                device.serial(),
                List.of("shell", "pm", "path", "--user", PRIMARY_USER_ID, targetPackage));

        assertOk(dumpResult, "adb -s " + device.serial() + " shell dumpsys package " + targetPackage);
        assertOk(pathResult, "adb -s " + device.serial() + " shell pm path --user " + PRIMARY_USER_ID + " " + targetPackage);

        List<String> apkPaths = appDetailsParser.parseApkPaths(pathResult.output());
        long codeSizeBytes = 0L;
        for (String apkPath : apkPaths) {
            long pathSize = measureRemotePathBytes(device.serial(), apkPath);
            if (pathSize > 0L) {
                codeSizeBytes += pathSize;
            }
        }

        AppDetails provisionalDetails = appDetailsParser.parse(
                baseApp,
                dumpResult.output(),
                pathResult.output(),
                codeSizeBytes,
                0L,
                0L,
                Map.of(),
                null);

        long dataDirectoryBytes = measureRemotePathBytes(device.serial(), provisionalDetails.dataDir());
        long cacheBytes = measureRemotePathBytes(
                device.serial(),
                "-".equals(provisionalDetails.dataDir()) ? "" : provisionalDetails.dataDir() + "/cache");
        long dataBytes = dataDirectoryBytes >= 0L
                ? Math.max(0L, dataDirectoryBytes - Math.max(0L, cacheBytes))
                : -1L;

        ApplicationPresentation presentation = resolveApplicationPresentation(
                device.serial(),
                targetPackage,
                provisionalDetails.sourceDir());

        AppDetails enrichedDetails = appDetailsParser.parse(
                baseApp,
                dumpResult.output(),
                pathResult.output(),
                codeSizeBytes,
                dataBytes,
                cacheBytes,
                Map.of(),
                null);

        InstalledApp updatedApplication = enrichedDetails.toListEntry().withFlags(baseApp.systemApp(), baseApp.disabled());
        if (!presentation.displayName().isBlank()) {
            updatedApplication = updatedApplication.withDisplayName(presentation.displayName());
        }

        cacheApplicationSummary(updatedApplication);
        replaceApplication(updatedApplication);
        return updatedApplication;
    }

    @Override
    public AppDetails getSelectedDeviceApplicationDetails(String packageName) throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.apps.deviceRequired"),
                Messages.text("error.apps.deviceDisconnected"));
        String targetPackage = requirePackageName(packageName);

        InstalledApp baseApp = applications.stream()
                .filter(application -> application.packageName().equals(targetPackage))
                .findFirst()
                .orElse(new InstalledApp(targetPackage, null, "", 0L, false, false));

        AdbResult dumpResult = client.runForSerial(device.serial(), List.of("shell", "dumpsys", "package", targetPackage));
        AdbResult pathResult = client.runForSerial(
                device.serial(),
                List.of("shell", "pm", "path", "--user", PRIMARY_USER_ID, targetPackage));
        AdbResult appOpsResult = client.runForSerial(
                device.serial(),
                List.of("shell", "cmd", "appops", "get", "--user", PRIMARY_USER_ID, targetPackage));

        assertOk(dumpResult, "adb -s " + device.serial() + " shell dumpsys package " + targetPackage);
        assertOk(pathResult, "adb -s " + device.serial() + " shell pm path --user " + PRIMARY_USER_ID + " " + targetPackage);

        List<String> apkPaths = appDetailsParser.parseApkPaths(pathResult.output());
        long codeSizeBytes = 0L;
        for (String apkPath : apkPaths) {
            long pathSize = measureRemotePathBytes(device.serial(), apkPath);
            if (pathSize > 0L) {
                codeSizeBytes += pathSize;
            }
        }

        AppDetails provisionalDetails = appDetailsParser.parse(
                baseApp,
                dumpResult.output(),
                pathResult.output(),
                codeSizeBytes,
                0L,
                0L,
                Map.of(),
                null);

        long dataDirectoryBytes = measureRemotePathBytes(device.serial(), provisionalDetails.dataDir());
        long cacheBytes = measureRemotePathBytes(
                device.serial(),
                "-".equals(provisionalDetails.dataDir()) ? "" : provisionalDetails.dataDir() + "/cache");
        long dataBytes = dataDirectoryBytes >= 0L
                ? Math.max(0L, dataDirectoryBytes - Math.max(0L, cacheBytes))
                : -1L;

        ApplicationPresentation presentation = resolveApplicationPresentation(
                device.serial(),
                targetPackage,
                provisionalDetails.sourceDir());
        InstalledApp displayedBaseApp = presentation.displayName().isBlank()
                ? baseApp
                : baseApp.withDisplayName(presentation.displayName());

        AppDetails details = appDetailsParser.parse(
                displayedBaseApp,
                dumpResult.output(),
                pathResult.output(),
                codeSizeBytes,
                dataBytes,
                cacheBytes,
                parseAppOps(appOpsResult.output()),
                presentation.iconImage());

        if (!presentation.displayName().isBlank() && !presentation.displayName().equals(details.displayName())) {
            details = new AppDetails(
                    details.app().withDisplayName(presentation.displayName()),
                    presentation.displayName(),
                    details.versionName(),
                    details.versionCode(),
                    details.targetSdk(),
                    details.minSdk(),
                    details.installerPackage(),
                    details.sourceDir(),
                    details.dataDir(),
                    details.codeSizeBytes(),
                    details.dataSizeBytes(),
                    details.cacheSizeBytes(),
                    details.debuggable(),
                    details.permissions(),
                    details.iconImage());
        }

        replaceApplication(details.toListEntry().withFlags(baseApp.systemApp(), baseApp.disabled()));
        return details;
    }

    @Override
    public void setSelectedDeviceApplicationPermission(String packageName, String permission, boolean granted) throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.apps.deviceRequired"),
                Messages.text("error.apps.deviceDisconnected"));

        String targetPackage = requirePackageName(packageName);
        String targetPermission = requirePackageName(permission);

        List<String> permissionCommand = List.of("shell", "pm", granted ? "grant" : "revoke", targetPackage, targetPermission);
        AdbResult permissionResult = client.runForSerial(device.serial(), permissionCommand);
        if (permissionResult.ok()) {
            return;
        }

        String appOp = permissionToAppOp(targetPermission);
        if (appOp.isBlank()) {
            throw new Exception("adb -s " + device.serial() + " " + String.join(" ", permissionCommand) + " failed:\n"
                    + permissionResult.output());
        }

        List<String> appOpsCommand = List.of(
                "shell",
                "cmd",
                "appops",
                "set",
                "--user",
                PRIMARY_USER_ID,
                targetPackage,
                appOp,
                granted ? "allow" : "default");
        AdbResult appOpsResult = client.runForSerial(device.serial(), appOpsCommand);
        assertOk(appOpsResult, "adb -s " + device.serial() + " " + String.join(" ", appOpsCommand));
    }

    @Override
    public void openSelectedDeviceApplication(String packageName) throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.apps.deviceRequired"),
                Messages.text("error.apps.deviceDisconnected"));

        String targetPackage = requirePackageName(packageName);
        try {
            String launchableComponent = resolveLaunchableActivityComponent(device.serial(), targetPackage);
            AdbResult result = client.runForSerial(
                    device.serial(),
                    List.of("shell", "am", "start", "--user", PRIMARY_USER_ID, "-n", launchableComponent));
            assertOk(result, "adb -s " + device.serial() + " shell am start --user " + PRIMARY_USER_ID + " -n "
                    + launchableComponent);
            return;
        } catch (Exception ignored) {
        }

        AdbResult fallbackResult = client.runForSerial(
                device.serial(),
                List.of("shell", "monkey", "-p", targetPackage, "-c", "android.intent.category.LAUNCHER",
                        "--dbg-no-events", "1"));
        assertOk(fallbackResult, "adb -s " + device.serial() + " shell monkey -p " + targetPackage);

        String output = fallbackResult.output() == null ? "" : fallbackResult.output().toLowerCase(Locale.ROOT);
        if (output.contains("aborted") || output.contains("no activities")) {
            throw new Exception(fallbackResult.output());
        }
    }

    @Override
    public void stopSelectedDeviceApplication(String packageName) throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.apps.deviceRequired"),
                Messages.text("error.apps.deviceDisconnected"));

        String targetPackage = requirePackageName(packageName);
        AdbResult result = client.runForSerial(
                device.serial(),
                List.of("shell", "am", "force-stop", targetPackage));
        assertOk(result, "adb -s " + device.serial() + " shell am force-stop " + targetPackage);
    }

    @Override
    public void uninstallSelectedDeviceApplication(String packageName) throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.apps.deviceRequired"),
                Messages.text("error.apps.deviceDisconnected"));

        String targetPackage = requirePackageName(packageName);
        AdbResult result = client.runForSerial(
                device.serial(),
                List.of("shell", "pm", "uninstall", "--user", PRIMARY_USER_ID, targetPackage));
        assertCommandSucceeded(
                result,
                "adb -s " + device.serial() + " shell pm uninstall --user " + PRIMARY_USER_ID + " " + targetPackage);
        applications = applications.stream()
                .filter(application -> !application.packageName().equals(targetPackage))
                .toList();
    }

    @Override
    public void setSelectedDeviceApplicationEnabled(String packageName, boolean enabled) throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.apps.deviceRequired"),
                Messages.text("error.apps.deviceDisconnected"));

        String targetPackage = requirePackageName(packageName);
        if (!enabled) {
            List<String> disableArgs = List.of("shell", "pm", "disable-user", "--user", PRIMARY_USER_ID, targetPackage);
            AdbResult disableResult = client.runForSerial(device.serial(), disableArgs);
            assertOk(disableResult, "adb -s " + device.serial() + " " + String.join(" ", disableArgs));
            return;
        }

        List<String> enableArgs = List.of("shell", "pm", "enable", "--user", PRIMARY_USER_ID, targetPackage);
        AdbResult enableResult = client.runForSerial(device.serial(), enableArgs);
        if (enableResult.ok()) {
            return;
        }

        String enableOutput = enableResult.output() == null ? "" : enableResult.output();
        if (!ENABLE_COMPONENT_STATE_FAILURE_PATTERN.matcher(enableOutput).find()) {
            throw new Exception("adb -s " + device.serial() + " " + String.join(" ", enableArgs) + " failed:\n"
                    + enableOutput);
        }

        // Some Samsung/One UI devices park rarely used apps in a disabled-until-used state
        // that shell cannot lift with pm enable, but launching the app reactivates it.
        openSelectedDeviceApplication(targetPackage);
        stopSelectedDeviceApplication(targetPackage);

        AdbResult disabledCheck = client.runForSerial(
                device.serial(),
                List.of("shell", "pm", "list", "packages", "-d", "--user", PRIMARY_USER_ID, targetPackage));
        assertOk(disabledCheck, "adb -s " + device.serial() + " shell pm list packages -d --user " + PRIMARY_USER_ID
                + " " + targetPackage);
        String disabledOutput = disabledCheck.output() == null ? "" : disabledCheck.output();
        if (disabledOutput.contains("package:" + targetPackage)) {
            throw new Exception("adb -s " + device.serial() + " " + String.join(" ", enableArgs) + " failed:\n"
                    + enableOutput);
        }
    }

    @Override
    public void clearSelectedDeviceApplicationData(String packageName) throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.apps.deviceRequired"),
                Messages.text("error.apps.deviceDisconnected"));

        String targetPackage = requirePackageName(packageName);
        AdbResult result = client.runForSerial(
                device.serial(),
                List.of("shell", "pm", "clear", targetPackage));
        assertCommandSucceeded(result, "adb -s " + device.serial() + " shell pm clear " + targetPackage);
    }

    @Override
    public void clearSelectedDeviceApplicationCache(String packageName) throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.apps.deviceRequired"),
                Messages.text("error.apps.deviceDisconnected"));

        String targetPackage = requirePackageName(packageName);
        AdbResult result = client.runForSerial(
                device.serial(),
                List.of("shell", "run-as", targetPackage, "sh", "-c", "rm -rf cache/* code_cache/* 2>/dev/null || true"));

        if (!result.ok()) {
            String output = result.output() == null ? "" : result.output().toLowerCase();
            if (output.contains("not debuggable")
                    || output.contains("package not debuggable")
                    || output.contains("run-as:")) {
                throw new IllegalStateException(Messages.text("error.apps.clearCacheUnsupported"));
            }
            throw new Exception("adb -s " + device.serial() + " shell run-as " + targetPackage
                    + " sh -c rm -rf cache/* code_cache/* failed:\n" + result.output());
        }
    }

    @Override
    public void exportSelectedDeviceApplicationApk(String packageName, File outputFile) throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.apps.deviceRequired"),
                Messages.text("error.apps.deviceDisconnected"));

        String targetPackage = requirePackageName(packageName);
        if (outputFile == null) {
            throw new IllegalArgumentException(Messages.text("error.apps.noSelection"));
        }

        AdbResult pathResult = client.runForSerial(
                device.serial(),
                List.of("shell", "pm", "path", "--user", PRIMARY_USER_ID, targetPackage));
        assertOk(pathResult, "adb -s " + device.serial() + " shell pm path --user " + PRIMARY_USER_ID + " " + targetPackage);

        String remotePath = appDetailsParser.parsePrimaryApkPath(pathResult.output(), "");
        if (remotePath.isBlank() || "-".equals(remotePath)) {
            throw new Exception("No APK path returned for " + targetPackage);
        }

        AdbResult pullResult = client.runForSerial(
                device.serial(),
                List.of("pull", remotePath, outputFile.getAbsolutePath()));
        assertOk(pullResult, "adb -s " + device.serial() + " pull " + remotePath + " " + outputFile.getAbsolutePath());
    }

    @Override
    public void setSelectedDeviceDisplay(int widthPx, int heightPx, int densityDpi) throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.display.deviceRequired"),
                Messages.text("error.display.deviceDisconnected"));

        if (widthPx <= 0 || heightPx <= 0 || densityDpi <= 0) {
            throw new IllegalArgumentException(Messages.text("error.display.invalidInput"));
        }

        AdbResult sizeResult = client.runForSerial(
                device.serial(),
                List.of("shell", "wm", "size", widthPx + "x" + heightPx));
        assertOk(sizeResult, "adb -s " + device.serial() + " shell wm size " + widthPx + "x" + heightPx);

        AdbResult densityResult = client.runForSerial(
                device.serial(),
                List.of("shell", "wm", "density", String.valueOf(densityDpi), "-r"));
        assertOk(densityResult, "adb -s " + device.serial() + " shell wm density " + densityDpi + " -r");
    }

    @Override
    public void resetSelectedDeviceDisplay() throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.display.deviceRequired"),
                Messages.text("error.display.deviceDisconnected"));

        AdbResult sizeResult = client.runForSerial(device.serial(), List.of("shell", "wm", "size", "reset"));
        assertOk(sizeResult, "adb -s " + device.serial() + " shell wm size reset");

        AdbResult densityResult = client.runForSerial(device.serial(), List.of("shell", "wm", "density", "reset"));
        assertOk(densityResult, "adb -s " + device.serial() + " shell wm density reset");
    }

    @Override
    public void setSelectedDeviceDarkMode(boolean enabled) throws Exception {
        Device device = requireConnectedSelectedDevice(
                Messages.text("error.display.deviceRequired"),
                Messages.text("error.display.deviceDisconnected"));

        AdbResult settingsResult = client.runForSerial(
                device.serial(),
                List.of("shell", "settings", "put", "secure", "ui_night_mode", enabled ? "2" : "1"));
        assertOk(settingsResult, "adb -s " + device.serial() + " shell settings put secure ui_night_mode "
                + (enabled ? "2" : "1"));

        client.runForSerial(
                device.serial(),
                List.of("shell", "cmd", "uimode", "night", enabled ? "yes" : "no"));
    }

    @Override
    public void pairWirelessDevice(String host, int pairingPort, String pairingCode) throws Exception {
        AdbToolInfo toolInfo = getAdbToolInfo();
        if (!toolInfo.supportsPair()) {
            throw new IllegalStateException(Messages.text("error.wireless.pairUnsupported"));
        }

        String endpoint = buildEndpoint(host, pairingPort);
        String secret = requireWirelessSecret(pairingCode);
        pairWirelessEndpoint(endpoint, secret);
    }

    @Override
    public void pairWirelessDeviceWithQr(String serviceName, String password, int timeoutSeconds) throws Exception {
        AdbToolInfo toolInfo = getAdbToolInfo();
        if (!toolInfo.supportsQrPairing()) {
            throw new IllegalStateException(Messages.text("error.wireless.qrUnsupported"));
        }

        String targetService = requireNonBlank(serviceName, Messages.text("error.wireless.qrPayload"));
        String secret = requireWirelessSecret(password);
        String endpoint = waitForPairingEndpoint(targetService, Math.max(5, timeoutSeconds));
        pairWirelessEndpoint(endpoint, secret);
    }

    @Override
    public void connectWirelessDevice(String host, int port) throws Exception {
        String endpoint = buildEndpoint(host, port);
        AdbResult connectResult = client.run(List.of("connect", endpoint));
        assertOk(connectResult, "adb connect " + endpoint);

        String output = connectResult.output() == null ? "" : connectResult.output().toLowerCase(Locale.ROOT);
        if (output.contains("failed") || output.contains("unable")) {
            throw new Exception("adb connect " + endpoint + " failed:\n" + connectResult.output());
        }
    }

    private InstalledApp applyCachedSummary(InstalledApp application) {
        if (application == null) {
            return null;
        }

        InstalledApp cachedSummary = applicationSummaryCache.get(presentationCacheKey(
                application.packageName(),
                application.apkPath()));
        if (cachedSummary != null) {
            return cachedSummary.withFlags(application.systemApp(), application.disabled());
        }

        ApplicationPresentation presentation = applicationPresentationCache.get(presentationCacheKey(
                application.packageName(),
                application.apkPath()));
        if (presentation == null || presentation.displayName().isBlank()) {
            return application;
        }

        return application.withDisplayName(presentation.displayName());
    }

    private void cacheApplicationSummary(InstalledApp application) {
        if (application == null) {
            return;
        }
        applicationSummaryCache.put(presentationCacheKey(application.packageName(), application.apkPath()), application);
    }

    private Map<String, String> parseAppOps(String output) {
        Map<String, String> appOpsByName = new HashMap<>();
        if (output == null || output.isBlank()) {
            return appOpsByName;
        }

        for (String line : output.split("\\R")) {
            Matcher matcher = APP_OP_PATTERN.matcher(line.trim());
            if (matcher.find()) {
                appOpsByName.put(matcher.group(1).trim(), matcher.group(2).trim().toLowerCase());
            }
        }

        return appOpsByName;
    }

    private String permissionToAppOp(String permissionName) {
        if (permissionName == null || permissionName.isBlank()) {
            return "";
        }

        int separator = permissionName.lastIndexOf('.');
        String suffix = separator >= 0 ? permissionName.substring(separator + 1) : permissionName;
        return suffix.trim();
    }

    private String resolveLaunchableActivityComponent(String serial, String packageName) throws Exception {
        AdbResult resolveResult = client.runForSerial(
                serial,
                List.of("shell", "cmd", "package", "resolve-activity", "--brief", "--user", PRIMARY_USER_ID, packageName));
        assertOk(resolveResult, "adb -s " + serial + " shell cmd package resolve-activity --brief --user "
                + PRIMARY_USER_ID + " " + packageName);

        for (String line : resolveResult.output().split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.contains("/") && !trimmed.startsWith("priority=")) {
                return trimmed;
            }
        }

        throw new Exception("No launchable activity found for " + packageName + ".\n" + resolveResult.output());
    }

    private ApplicationPresentation resolveApplicationPresentation(
            String serial,
            String packageName,
            String sourceDir) {
        String cacheKey = presentationCacheKey(packageName, sourceDir);
        if (applicationPresentationCache.containsKey(cacheKey)) {
            return applicationPresentationCache.get(cacheKey);
        }

        ApplicationPresentation presentation = ApplicationPresentation.empty();
        try {
            List<Path> aaptExecutables = locateAaptExecutables();
            if (aaptExecutables.isEmpty() || sourceDir == null || sourceDir.isBlank() || "-".equals(sourceDir)) {
                applicationPresentationCache.put(cacheKey, presentation);
                return presentation;
            }

            Path localApk = Files.createTempFile("adbmanager-", ".apk");
            try {
                AdbResult pullResult = client.runForSerial(serial, List.of("pull", sourceDir, localApk.toString()));
                assertOk(pullResult, "adb -s " + serial + " pull " + sourceDir + " " + localApk);
                for (Path aaptExecutable : aaptExecutables) {
                    presentation = loadPresentationFromApk(aaptExecutable, localApk);
                    if (!presentation.displayName().isBlank() || presentation.iconImage() != null) {
                        break;
                    }
                }
            } finally {
                Files.deleteIfExists(localApk);
            }
        } catch (Exception ignored) {
            presentation = ApplicationPresentation.empty();
        }

        applicationPresentationCache.put(cacheKey, presentation);
        return presentation;
    }

    private List<Path> locateAaptExecutables() {
        List<Path> candidates = new ArrayList<>();
        addSdkBuildToolsCandidates(candidates, System.getenv("ANDROID_HOME"));
        addSdkBuildToolsCandidates(candidates, System.getenv("ANDROID_SDK_ROOT"));

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            addSdkBuildToolsCandidates(candidates, Path.of(localAppData, "Android", "Sdk").toString());
        }

        return candidates.stream()
                .filter(Files::exists)
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    private void addSdkBuildToolsCandidates(List<Path> candidates, String sdkRoot) {
        if (sdkRoot == null || sdkRoot.isBlank()) {
            return;
        }

        Path buildToolsDirectory = Path.of(sdkRoot, "build-tools");
        if (!Files.isDirectory(buildToolsDirectory)) {
            return;
        }

        try (var stream = Files.list(buildToolsDirectory)) {
            stream.filter(Files::isDirectory)
                    .map(path -> path.resolve(windowsExecutable("aapt")))
                    .filter(Files::exists)
                    .forEach(candidates::add);
        } catch (IOException ignored) {
        }
    }

    private String windowsExecutable(String commandName) {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("win") ? commandName + ".exe" : commandName;
    }

    private ApplicationPresentation loadPresentationFromApk(Path aaptExecutable, Path localApk) throws Exception {
        String output = runLocalTool(aaptExecutable, List.of("dump", "badging", localApk.toString()));
        if (output == null || output.isBlank()) {
            return ApplicationPresentation.empty();
        }

        String label = "";
        String iconPath = "";
        int iconDensity = Integer.MIN_VALUE;
        for (String line : output.split("\\R")) {
            Matcher labelMatcher = BADGING_LABEL_PATTERN.matcher(line.trim());
            if (labelMatcher.matches() && label.isBlank()) {
                label = labelMatcher.group(1).trim();
            }

            Matcher iconMatcher = BADGING_ICON_PATTERN.matcher(line.trim());
            if (iconMatcher.matches()) {
                int density = Integer.parseInt(iconMatcher.group(1));
                String candidatePath = iconMatcher.group(2).trim();
                if (!candidatePath.isBlank() && density > iconDensity) {
                    iconDensity = density;
                    iconPath = candidatePath;
                }
                continue;
            }

            if (iconPath.isBlank()) {
                Matcher fallbackIconMatcher = BADGING_FALLBACK_ICON_PATTERN.matcher(line.trim());
                if (fallbackIconMatcher.find()) {
                    iconPath = fallbackIconMatcher.group(1).trim();
                }
            }
        }

        return new ApplicationPresentation(label, loadIconFromApk(aaptExecutable, localApk, iconPath));
    }

    private boolean isRasterAsset(String assetPath) {
        if (assetPath == null || assetPath.isBlank()) {
            return false;
        }

        String lowerPath = assetPath.toLowerCase();
        return lowerPath.endsWith(".png") || lowerPath.endsWith(".webp") || lowerPath.endsWith(".jpg")
                || lowerPath.endsWith(".jpeg");
    }

    private BufferedImage loadIconFromApk(Path aaptExecutable, Path apkPath, String entryName) throws Exception {
        if (entryName == null || entryName.isBlank()) {
            return null;
        }

        BufferedImage directImage = loadRasterIcon(apkPath, entryName);
        if (directImage != null) {
            return directImage;
        }

        BufferedImage rasterFallbackImage = loadBestMatchingRasterIcon(apkPath, entryName);
        if (rasterFallbackImage != null) {
            return rasterFallbackImage;
        }

        if (entryName.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            return renderXmlIcon(aaptExecutable, apkPath, entryName);
        }

        return null;
    }

    private BufferedImage loadRasterIcon(Path apkPath, String entryName) {
        if (!isRasterAsset(entryName)) {
            return null;
        }

        try (ZipFile zipFile = new ZipFile(apkPath.toFile())) {
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry == null) {
                return null;
            }

            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                return ImageIO.read(inputStream);
            }
        } catch (IOException ignored) {
            return null;
        }
    }

    private BufferedImage loadBestMatchingRasterIcon(Path apkPath, String entryName) {
        String baseName = baseName(entryName);
        if (baseName.isBlank()) {
            return null;
        }

        try (ZipFile zipFile = new ZipFile(apkPath.toFile())) {
            List<String> candidates = zipFile.stream()
                    .map(ZipEntry::getName)
                    .filter(name -> isMatchingRasterCandidate(name, baseName))
                    .sorted((left, right) -> Integer.compare(resourceDensityRank(right), resourceDensityRank(left)))
                    .toList();

            for (String candidate : candidates) {
                BufferedImage image = loadRasterIcon(apkPath, candidate);
                if (image != null) {
                    return image;
                }
            }
        } catch (IOException ignored) {
            return null;
        }

        return null;
    }

    private boolean isMatchingRasterCandidate(String entryName, String baseName) {
        if (!isRasterAsset(entryName)) {
            return false;
        }

        String lowerEntryName = entryName.toLowerCase(Locale.ROOT);
        String lowerBaseName = baseName.toLowerCase(Locale.ROOT);
        return lowerEntryName.endsWith("/" + lowerBaseName + ".png")
                || lowerEntryName.endsWith("/" + lowerBaseName + ".webp")
                || lowerEntryName.endsWith("/" + lowerBaseName + ".jpg")
                || lowerEntryName.endsWith("/" + lowerBaseName + ".jpeg");
    }

    private int resourceDensityRank(String entryName) {
        String lowerEntryName = entryName == null ? "" : entryName.toLowerCase(Locale.ROOT);
        if (lowerEntryName.contains("xxxhdpi")) {
            return 6;
        }
        if (lowerEntryName.contains("xxhdpi")) {
            return 5;
        }
        if (lowerEntryName.contains("xhdpi")) {
            return 4;
        }
        if (lowerEntryName.contains("hdpi")) {
            return 3;
        }
        if (lowerEntryName.contains("mdpi")) {
            return 2;
        }
        if (lowerEntryName.contains("ldpi")) {
            return 1;
        }
        return 0;
    }

    private BufferedImage renderXmlIcon(Path aaptExecutable, Path apkPath, String entryName) throws Exception {
        ResourceTable resourceTable = loadResourceTable(aaptExecutable, apkPath);
        String xmlTree = runLocalTool(aaptExecutable, List.of("dump", "xmltree", apkPath.toString(), entryName));
        if (xmlTree.isBlank()) {
            return null;
        }

        if (xmlTree.contains("E: adaptive-icon")) {
            return renderAdaptiveIcon(aaptExecutable, apkPath, xmlTree, resourceTable);
        }

        if (xmlTree.contains("E: vector")) {
            return renderVectorDrawable(aaptExecutable, apkPath, xmlTree, resourceTable);
        }

        return null;
    }

    private ResourceTable loadResourceTable(Path aaptExecutable, Path apkPath) throws Exception {
        String resourcesOutput = runLocalTool(aaptExecutable, List.of("dump", "resources", apkPath.toString()));
        Map<String, String> resourceNamesById = new HashMap<>();
        Map<String, Integer> colorValuesById = new HashMap<>();

        for (String line : resourcesOutput.split("\\R")) {
            Matcher specMatcher = RESOURCE_SPEC_PATTERN.matcher(line.trim());
            if (specMatcher.find()) {
                resourceNamesById.put(normalizeResourceId(specMatcher.group(1)), specMatcher.group(2).trim());
            }

            Matcher colorMatcher = RESOURCE_COLOR_VALUE_PATTERN.matcher(line.trim());
            if (colorMatcher.find()) {
                colorValuesById.put(
                        normalizeResourceId(colorMatcher.group(1)),
                        (int) Long.parseLong(colorMatcher.group(2), 16));
            }
        }

        return new ResourceTable(resourceNamesById, colorValuesById);
    }

    private BufferedImage renderAdaptiveIcon(
            Path aaptExecutable,
            Path apkPath,
            String xmlTree,
            ResourceTable resourceTable) throws Exception {
        List<String> drawableReferences = new ArrayList<>();
        for (String line : xmlTree.split("\\R")) {
            Matcher drawableMatcher = XMLTREE_DRAWABLE_REFERENCE_PATTERN.matcher(line.trim());
            if (drawableMatcher.find()) {
                drawableReferences.add(normalizeResourceId(drawableMatcher.group(1)));
            }
        }

        BufferedImage image = new BufferedImage(108, 108, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            if (!drawableReferences.isEmpty()) {
                Color backgroundColor = resolveColor(drawableReferences.get(0), resourceTable);
                if (backgroundColor != null) {
                    graphics.setColor(backgroundColor);
                    graphics.fillRoundRect(0, 0, image.getWidth(), image.getHeight(), 28, 28);
                } else {
                    BufferedImage backgroundImage = renderReferencedDrawable(
                            aaptExecutable,
                            apkPath,
                            drawableReferences.get(0),
                            resourceTable);
                    drawScaledImage(graphics, backgroundImage, image.getWidth(), image.getHeight());
                }
            }

            if (drawableReferences.size() >= 2) {
                BufferedImage foregroundImage = renderReferencedDrawable(
                        aaptExecutable,
                        apkPath,
                        drawableReferences.get(1),
                        resourceTable);
                drawScaledImage(graphics, foregroundImage, image.getWidth(), image.getHeight());
            }
        } finally {
            graphics.dispose();
        }

        return image;
    }

    private BufferedImage renderReferencedDrawable(
            Path aaptExecutable,
            Path apkPath,
            String resourceId,
            ResourceTable resourceTable) throws Exception {
        String resourceName = resourceTable.resourceNamesById().get(normalizeResourceId(resourceId));
        if (resourceName == null || resourceName.isBlank()) {
            return null;
        }

        String resourcePath = findResourcePath(apkPath, resourceName);
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }

        BufferedImage rasterImage = loadRasterIcon(apkPath, resourcePath);
        if (rasterImage != null) {
            return rasterImage;
        }

        if (resourcePath.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            return renderXmlIcon(aaptExecutable, apkPath, resourcePath);
        }

        return null;
    }

    private String findResourcePath(Path apkPath, String resourceName) {
        if (resourceName == null || resourceName.isBlank() || !resourceName.contains("/")) {
            return null;
        }

        String folder = resourceName.substring(0, resourceName.indexOf('/'));
        String name = resourceName.substring(resourceName.indexOf('/') + 1);
        try (ZipFile zipFile = new ZipFile(apkPath.toFile())) {
            return zipFile.stream()
                    .map(ZipEntry::getName)
                    .filter(entry -> entry.startsWith("res/" + folder))
                    .filter(entry -> {
                        String fileName = entry.substring(entry.lastIndexOf('/') + 1);
                        return fileName.equalsIgnoreCase(name + ".xml")
                                || fileName.equalsIgnoreCase(name + ".png")
                                || fileName.equalsIgnoreCase(name + ".webp")
                                || fileName.equalsIgnoreCase(name + ".jpg")
                                || fileName.equalsIgnoreCase(name + ".jpeg");
                    })
                    .sorted((left, right) -> Integer.compare(resourceDensityRank(right), resourceDensityRank(left)))
                    .findFirst()
                    .orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    private BufferedImage renderVectorDrawable(
            Path aaptExecutable,
            Path apkPath,
            String xmlTree,
            ResourceTable resourceTable) throws Exception {
        double viewportWidth = parseFloatBits(XMLTREE_VIEWPORT_WIDTH_PATTERN, xmlTree);
        double viewportHeight = parseFloatBits(XMLTREE_VIEWPORT_HEIGHT_PATTERN, xmlTree);
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return null;
        }

        List<VectorPathSpec> paths = new ArrayList<>();
        String[] lines = xmlTree.split("\\R");
        for (int index = 0; index < lines.length; index++) {
            String trimmedLine = lines[index].trim();
            Matcher pathMatcher = XMLTREE_PATH_DATA_PATTERN.matcher(trimmedLine);
            if (!pathMatcher.find()) {
                continue;
            }

            String pathData = pathMatcher.group(1).trim();
            Color fillColor = resolveFillColor(trimmedLine, resourceTable);
            for (int lookBehind = Math.max(0, index - 2); fillColor == null && lookBehind < index; lookBehind++) {
                fillColor = resolveFillColor(lines[lookBehind].trim(), resourceTable);
            }

            if (pathData.isBlank() || fillColor == null) {
                continue;
            }
            paths.add(new VectorPathSpec(pathData, fillColor));
        }

        if (paths.isEmpty()) {
            return null;
        }

        int imageSize = 108;
        BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.scale(imageSize / viewportWidth, imageSize / viewportHeight);

            for (VectorPathSpec path : paths) {
                graphics.setColor(path.fillColor());
                Shape shape = VectorPathParser.parse(path.pathData());
                graphics.fill(shape);
            }
        } finally {
            graphics.dispose();
        }

        return image;
    }

    private Color resolveFillColor(String line, ResourceTable resourceTable) {
        Matcher fillColorMatcher = XMLTREE_FILL_COLOR_PATTERN.matcher(line);
        if (!fillColorMatcher.find()) {
            return null;
        }

        if (fillColorMatcher.group(1) != null) {
            return resolveColor(fillColorMatcher.group(1), resourceTable);
        }

        String literalValue = fillColorMatcher.group(2) != null
                ? fillColorMatcher.group(2)
                : fillColorMatcher.group(3) != null ? fillColorMatcher.group(3) : fillColorMatcher.group(4);
        if (literalValue == null || literalValue.isBlank()) {
            return null;
        }

        return new Color((int) Long.parseLong(literalValue, 16), true);
    }

    private Color resolveColor(String resourceId, ResourceTable resourceTable) {
        Integer argb = resourceTable.colorValuesById().get(normalizeResourceId(resourceId));
        return argb == null ? null : new Color(argb, true);
    }

    private double parseFloatBits(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) {
            return 0d;
        }

        return Float.intBitsToFloat((int) Long.parseLong(matcher.group(1), 16));
    }

    private String normalizeResourceId(String resourceId) {
        String normalized = resourceId == null ? "" : resourceId.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("0x") ? normalized.substring(2) : normalized;
    }

    private String baseName(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return "";
        }

        String fileName = entryName.substring(entryName.lastIndexOf('/') + 1).trim();
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;
    }

    private void drawScaledImage(Graphics2D graphics, BufferedImage image, int width, int height) {
        if (image == null) {
            return;
        }
        graphics.drawImage(image, 0, 0, width, height, null);
    }

    private String runLocalTool(Path executable, List<String> arguments) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(arguments);

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> outputFuture = executor.submit(() -> {
                try (InputStream inputStream = process.getInputStream()) {
                    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            });

            boolean finished = process.waitFor(LOCAL_TOOL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return readToolOutput(outputFuture);
            }

            return outputFuture.get();
        } finally {
            executor.shutdownNow();
        }
    }

    private String readToolOutput(Future<String> outputFuture) {
        try {
            return outputFuture.get(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException | TimeoutException e) {
            return "";
        }
    }

    private String presentationCacheKey(String packageName, String sourceDir) {
        return packageName + "|" + (sourceDir == null ? "" : sourceDir);
    }

    private void syncSelectedDevice() {
        if (selectedDeviceSerial == null) {
            return;
        }

        boolean selectedDeviceStillPresent = devices.stream()
                .anyMatch(device -> device.serial().equals(selectedDeviceSerial));

        if (!selectedDeviceStillPresent) {
            selectedDeviceSerial = null;
            applications = List.of();
            applicationPresentationCache.clear();
            applicationSummaryCache.clear();
        }
    }

    private void updateSelectedDeviceSerial(String serial) {
        if (!Objects.equals(selectedDeviceSerial, serial)) {
            selectedDeviceSerial = serial;
            applications = List.of();
            applicationPresentationCache.clear();
            applicationSummaryCache.clear();
            return;
        }
        selectedDeviceSerial = serial;
    }

    private Device requireConnectedSelectedDevice(String missingSelectionMessage, String disconnectedMessage) {
        Device device = getSelectedDevice()
                .orElseThrow(() -> new IllegalStateException(missingSelectionMessage));
        if (!Messages.STATUS_CONNECTED.equals(device.state())) {
            throw new IllegalStateException(disconnectedMessage);
        }
        return device;
    }

    private String requirePackageName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(Messages.text("error.apps.noSelection"));
        }
        return value.trim();
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String requireWirelessSecret(String value) {
        return requireNonBlank(value, Messages.text("error.wireless.invalidSecret"));
    }

    private String buildEndpoint(String host, int port) {
        String normalizedHost = requireNonBlank(host, Messages.text("error.wireless.invalidHost"));
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException(Messages.text("error.wireless.invalidPort"));
        }
        return normalizedHost + ":" + port;
    }

    private void assertOk(AdbResult result, String commandDescription) throws Exception {
        if (!result.ok()) {
            throw new Exception(commandDescription + " failed:\n" + result.output());
        }
    }

    private void pairWirelessEndpoint(String endpoint, String secret) throws Exception {
        AdbResult pairResult = client.run(List.of("pair", endpoint, secret));
        if (!pairResult.ok()) {
            String output = pairResult.output() == null ? "" : pairResult.output();
            if (output.contains("Enter pairing code") || output.contains("Enter password")) {
                pairResult = client.run(List.of("pair", endpoint), secret + System.lineSeparator());
            }
        }

        assertOk(pairResult, "adb pair " + endpoint);
        String lowerOutput = pairResult.output() == null ? "" : pairResult.output().toLowerCase(Locale.ROOT);
        if (lowerOutput.contains("failed") || lowerOutput.contains("unable")) {
            throw new Exception("adb pair " + endpoint + " failed:\n" + pairResult.output());
        }
    }

    private String waitForPairingEndpoint(String serviceName, int timeoutSeconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline) {
            AdbResult mdnsResult = client.run(List.of("mdns", "services"));
            assertOk(mdnsResult, "adb mdns services");
            String endpoint = findPairingEndpoint(mdnsResult.output(), serviceName);
            if (endpoint != null) {
                return endpoint;
            }
            Thread.sleep(1500L);
        }

        throw new Exception(Messages.format("error.wireless.qrTimeout", serviceName));
    }

    private String findPairingEndpoint(String mdnsOutput, String serviceName) {
        if (mdnsOutput == null || mdnsOutput.isBlank()) {
            return null;
        }

        for (String rawLine : mdnsOutput.split("\\R")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank() || !line.contains(serviceName) || !line.contains("_adb-tls-pairing")) {
                continue;
            }

            Matcher matcher = MDNS_ENDPOINT_PATTERN.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    private String findFirstGroup(Pattern pattern, String input, String fallback) {
        Matcher matcher = pattern.matcher(input == null ? "" : input);
        return matcher.find() ? matcher.group(1).trim() : fallback;
    }

    private void assertCommandSucceeded(AdbResult result, String commandDescription) throws Exception {
        assertOk(result, commandDescription);
        String output = result.output() == null ? "" : result.output();
        if (!output.contains("Success")) {
            throw new Exception(commandDescription + " failed:\n" + output);
        }
    }

    private long measureRemotePathBytes(String serial, String path) throws Exception {
        if (path == null || path.isBlank() || "-".equals(path)) {
            return -1L;
        }

        AdbResult result = client.runForSerial(serial, List.of("shell", "du", "-k", path));
        String output = result.output() == null ? "" : result.output().trim();

        if (!result.ok()) {
            String lower = output.toLowerCase();
            if (lower.contains("permission denied") || lower.contains("no such file") || lower.contains("not found")) {
                return -1L;
            }
            throw new Exception("adb -s " + serial + " shell du -k " + path + " failed:\n" + output);
        }

        if (output.isBlank()) {
            return -1L;
        }

        String firstLine = output.split("\\R", 2)[0].trim();
        String rawValue = firstLine.split("\\s+", 2)[0];
        try {
            long kilobytes = Long.parseLong(rawValue);
            return Math.max(0L, kilobytes) * 1024L;
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }

    private void replaceApplication(InstalledApp updatedApplication) {
        cacheApplicationSummary(updatedApplication);
        List<InstalledApp> mutableApplications = new ArrayList<>(applications);
        for (int index = 0; index < mutableApplications.size(); index++) {
            if (mutableApplications.get(index).packageName().equals(updatedApplication.packageName())) {
                mutableApplications.set(index, updatedApplication);
                mutableApplications.sort(Comparator
                        .comparing(InstalledApp::displayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(InstalledApp::packageName, String.CASE_INSENSITIVE_ORDER));
                applications = List.copyOf(mutableApplications);
                return;
            }
        }

        mutableApplications.add(updatedApplication);
        mutableApplications.sort(Comparator
                .comparing(InstalledApp::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(InstalledApp::packageName, String.CASE_INSENSITIVE_ORDER));
        applications = List.copyOf(mutableApplications);
    }

    private Map<String, Long> measurePackageApkSizes(String serial, String catalogOutput) throws Exception {
        List<PackagePath> packagePaths = parsePackagePaths(catalogOutput);
        Map<String, Long> sizesByPackage = new HashMap<>();

        for (int start = 0; start < packagePaths.size(); start += APK_SIZE_BATCH_SIZE) {
            int end = Math.min(start + APK_SIZE_BATCH_SIZE, packagePaths.size());
            List<PackagePath> batch = packagePaths.subList(start, end);
            Map<String, Long> sizesByPath = measurePathsInBatch(serial, batch.stream().map(PackagePath::apkPath).toList());

            for (PackagePath packagePath : batch) {
                sizesByPackage.put(packagePath.packageName(), sizesByPath.getOrDefault(packagePath.apkPath(), 0L));
            }
        }

        return sizesByPackage;
    }

    private Map<String, Long> measurePathsInBatch(String serial, List<String> paths) throws Exception {
        Map<String, Long> sizesByPath = new HashMap<>();
        if (paths.isEmpty()) {
            return sizesByPath;
        }

        List<String> command = new ArrayList<>();
        command.add("shell");
        command.add("du");
        command.add("-k");
        command.addAll(paths);

        AdbResult result = client.runForSerial(serial, command);
        if (!result.ok()) {
            for (String path : paths) {
                long size = measureRemotePathBytes(serial, path);
                if (size > 0L) {
                    sizesByPath.put(path, size);
                }
            }
            return sizesByPath;
        }

        for (String line : result.output().split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.toLowerCase().contains("permission denied")) {
                continue;
            }

            String[] parts = trimmed.split("\\s+", 2);
            if (parts.length < 2) {
                continue;
            }

            try {
                sizesByPath.put(parts[1].trim(), Math.max(0L, Long.parseLong(parts[0])) * 1024L);
            } catch (NumberFormatException ignored) {
            }
        }

        return sizesByPath;
    }

    private List<PackagePath> parsePackagePaths(String catalogOutput) {
        List<PackagePath> packagePaths = new ArrayList<>();
        if (catalogOutput == null || catalogOutput.isBlank()) {
            return packagePaths;
        }

        for (String line : catalogOutput.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("package:") || !trimmed.contains("=")) {
                continue;
            }

            int separatorIndex = trimmed.lastIndexOf('=');
            packagePaths.add(new PackagePath(
                    trimmed.substring(separatorIndex + 1).trim(),
                    trimmed.substring("package:".length(), separatorIndex).trim()));
        }

        return packagePaths;
    }

    private record ResourceTable(Map<String, String> resourceNamesById, Map<String, Integer> colorValuesById) {
    }

    private record VectorPathSpec(String pathData, Color fillColor) {
    }

    private static final class VectorPathParser {

        private static final Pattern TOKEN_PATTERN = Pattern.compile(
                "[A-Za-z]|[-+]?(?:\\d*\\.\\d+|\\d+)(?:[eE][-+]?\\d+)?");

        private VectorPathParser() {
        }

        private static Shape parse(String pathData) {
            List<String> tokens = tokenize(pathData);
            Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);

            double currentX = 0d;
            double currentY = 0d;
            double startX = 0d;
            double startY = 0d;
            double lastControlX = 0d;
            double lastControlY = 0d;
            char command = ' ';
            int index = 0;

            while (index < tokens.size()) {
                String token = tokens.get(index);
                if (isCommand(token)) {
                    command = token.charAt(0);
                    index++;
                }

                switch (command) {
                    case 'M', 'm' -> {
                        boolean relative = command == 'm';
                        double x = nextNumber(tokens, index++);
                        double y = nextNumber(tokens, index++);
                        if (relative) {
                            x += currentX;
                            y += currentY;
                        }

                        path.moveTo(x, y);
                        currentX = x;
                        currentY = y;
                        startX = x;
                        startY = y;
                        lastControlX = x;
                        lastControlY = y;

                        while (hasNumber(tokens, index)) {
                            x = nextNumber(tokens, index++);
                            y = nextNumber(tokens, index++);
                            if (relative) {
                                x += currentX;
                                y += currentY;
                            }

                            path.lineTo(x, y);
                            currentX = x;
                            currentY = y;
                            lastControlX = x;
                            lastControlY = y;
                        }
                    }
                    case 'L', 'l' -> {
                        boolean relative = command == 'l';
                        while (hasNumber(tokens, index)) {
                            double x = nextNumber(tokens, index++);
                            double y = nextNumber(tokens, index++);
                            if (relative) {
                                x += currentX;
                                y += currentY;
                            }
                            path.lineTo(x, y);
                            currentX = x;
                            currentY = y;
                            lastControlX = x;
                            lastControlY = y;
                        }
                    }
                    case 'H', 'h' -> {
                        boolean relative = command == 'h';
                        while (hasNumber(tokens, index)) {
                            double x = nextNumber(tokens, index++);
                            if (relative) {
                                x += currentX;
                            }
                            path.lineTo(x, currentY);
                            currentX = x;
                            lastControlX = x;
                            lastControlY = currentY;
                        }
                    }
                    case 'V', 'v' -> {
                        boolean relative = command == 'v';
                        while (hasNumber(tokens, index)) {
                            double y = nextNumber(tokens, index++);
                            if (relative) {
                                y += currentY;
                            }
                            path.lineTo(currentX, y);
                            currentY = y;
                            lastControlX = currentX;
                            lastControlY = y;
                        }
                    }
                    case 'C', 'c' -> {
                        boolean relative = command == 'c';
                        while (hasNumber(tokens, index)) {
                            double x1 = nextNumber(tokens, index++);
                            double y1 = nextNumber(tokens, index++);
                            double x2 = nextNumber(tokens, index++);
                            double y2 = nextNumber(tokens, index++);
                            double x = nextNumber(tokens, index++);
                            double y = nextNumber(tokens, index++);

                            if (relative) {
                                x1 += currentX;
                                y1 += currentY;
                                x2 += currentX;
                                y2 += currentY;
                                x += currentX;
                                y += currentY;
                            }

                            path.curveTo(x1, y1, x2, y2, x, y);
                            currentX = x;
                            currentY = y;
                            lastControlX = x2;
                            lastControlY = y2;
                        }
                    }
                    case 'S', 's' -> {
                        boolean relative = command == 's';
                        while (hasNumber(tokens, index)) {
                            double reflectedX = (2 * currentX) - lastControlX;
                            double reflectedY = (2 * currentY) - lastControlY;
                            double x2 = nextNumber(tokens, index++);
                            double y2 = nextNumber(tokens, index++);
                            double x = nextNumber(tokens, index++);
                            double y = nextNumber(tokens, index++);

                            if (relative) {
                                x2 += currentX;
                                y2 += currentY;
                                x += currentX;
                                y += currentY;
                            }

                            path.curveTo(reflectedX, reflectedY, x2, y2, x, y);
                            currentX = x;
                            currentY = y;
                            lastControlX = x2;
                            lastControlY = y2;
                        }
                    }
                    case 'Z', 'z' -> {
                        path.closePath();
                        currentX = startX;
                        currentY = startY;
                        lastControlX = startX;
                        lastControlY = startY;
                    }
                    default -> throw new IllegalArgumentException("Unsupported SVG path command: " + command);
                }
            }

            return path;
        }

        private static List<String> tokenize(String pathData) {
            List<String> tokens = new ArrayList<>();
            Matcher matcher = TOKEN_PATTERN.matcher(pathData);
            while (matcher.find()) {
                tokens.add(matcher.group());
            }
            return tokens;
        }

        private static boolean isCommand(String token) {
            return token.length() == 1 && Character.isLetter(token.charAt(0));
        }

        private static boolean hasNumber(List<String> tokens, int index) {
            return index < tokens.size() && !isCommand(tokens.get(index));
        }

        private static double nextNumber(List<String> tokens, int index) {
            return Double.parseDouble(tokens.get(index));
        }
    }

    private record PackagePath(String packageName, String apkPath) {
    }

    private record ApplicationPresentation(String displayName, BufferedImage iconImage) {
        private static ApplicationPresentation empty() {
            return new ApplicationPresentation("", null);
        }
    }
}
