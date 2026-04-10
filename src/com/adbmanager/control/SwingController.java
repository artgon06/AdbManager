package com.adbmanager.control;

import java.awt.Desktop;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

import com.adbmanager.logic.AdbModel;
import com.adbmanager.logic.model.AppDetails;
import com.adbmanager.logic.model.AdbToolInfo;
import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.logic.model.InstalledApp;
import com.adbmanager.logic.model.WirelessPairingQrPayload;
import com.adbmanager.view.Messages;
import com.adbmanager.view.Messages.Language;
import com.adbmanager.view.swing.AppTheme;
import com.adbmanager.view.swing.MainFrame;
import com.adbmanager.view.swing.SimpleQrCodeGenerator;
import com.adbmanager.view.swing.WirelessConnectionDialog;

public class SwingController {

    private static final int MAX_BACKGROUND_APP_ENRICHMENTS = 18;
    private static final int VISIBLE_APP_ENRICHMENT_EXTRA_ROWS = 8;

    @FunctionalInterface
    private interface ApplicationTask {
        void run() throws Exception;
    }

    private final AdbModel model;
    private final MainFrame view;
    private boolean syncingDeviceSelector;
    private boolean loadingDevices;
    private boolean loadingApplications;
    private boolean loadingApplicationDetails;
    private String currentSelectedSerial;
    private String applicationsLoadedSerial;
    private String currentSelectedPackageName;
    private SwingWorker<Void, InstalledApp> applicationEnrichmentWorker;
    private final Set<String> enrichedApplicationPackages = new HashSet<>();
    private final Set<String> pendingApplicationPackages = new LinkedHashSet<>();
    private boolean autoRefreshOnFocus = true;
    private WirelessPairingQrPayload currentQrPayload;

    public SwingController(AdbModel model, MainFrame view) {
        this.model = model;
        this.view = view;
    }

    public void start() {
        bindEvents();
        view.setSelectedLanguage(Messages.getLanguage());
        view.setLanguage(Messages.getLanguage());
        view.setSelectedTheme(AppTheme.LIGHT);
        view.setTheme(AppTheme.LIGHT);
        view.setAutoRefreshOnFocusSelected(true);
        view.showHomeScreen();
        view.showWindow();
        refreshDevices();
    }

    private void bindEvents() {
        WirelessConnectionDialog wirelessDialog = view.getWirelessConnectionDialog();

        view.setDeviceSelectionAction(event -> onDeviceSelected());
        view.setCaptureAction(event -> captureScreenshot());
        view.setSaveCaptureAction(event -> saveScreenshot());
        view.setRefreshAction(event -> refreshDevices());
        view.setWirelessAssistantAction(event -> openWirelessAssistant());
        view.setHomeAction(event -> view.showHomeScreen());
        view.setDisplayAction(event -> view.showDisplayScreen());
        view.setApplyDisplayAction(event -> applyDisplayOverride());
        view.setResetDisplayAction(event -> resetDisplayOverride());
        view.setDeviceDarkModeAction(event -> toggleDeviceDarkMode());
        view.setAppsAction(event -> showAppsScreen());
        view.setSettingsAction(event -> view.showSettingsScreen());
        view.setThemeChangeAction(event -> applyThemeSelection());
        view.setLanguageChangeAction(event -> applyLanguageSelection());
        view.setAutoRefreshOnFocusChangeAction(event -> autoRefreshOnFocus = view.isAutoRefreshOnFocusSelected());
        view.setRepositoryAction(event -> openRepository());
        view.setApplicationSelectionAction(this::onApplicationSelected);
        view.setApplicationPermissionToggleHandler(this::toggleApplicationPermission);
        view.setOpenApplicationAction(event -> openSelectedApplication());
        view.setStopApplicationAction(event -> stopSelectedApplication());
        view.setUninstallApplicationAction(event -> uninstallSelectedApplication());
        view.setToggleApplicationEnabledAction(event -> toggleSelectedApplicationEnabled());
        view.setClearApplicationDataAction(event -> clearSelectedApplicationData());
        view.setClearApplicationCacheAction(event -> clearSelectedApplicationCache());
        view.setExportApplicationApkAction(event -> exportSelectedApplicationApk());
        wirelessDialog.setConnectAction(event -> connectWirelessDevice());
        wirelessDialog.setPairCodeAction(event -> pairWirelessDeviceByCode());
        wirelessDialog.setGenerateQrAction(event -> generateWirelessQrPayload());
        wirelessDialog.setPairQrAction(event -> pairWirelessDeviceByQr());
        view.setApplicationsViewportChangeAction(this::refreshVisibleApplicationSummaries);
        view.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent event) {
                if (autoRefreshOnFocus) {
                    refreshDevices();
                }
            }
        });
    }

    private void refreshDevices() {
        if (loadingDevices) {
            return;
        }

        loadingDevices = true;
        view.setDeviceSelectorEnabled(false);
        view.setCaptureEnabled(false);
        view.setRefreshEnabled(false);
        view.setApplicationsEnabled(false);
        view.setApplicationActionsEnabled(false);
        view.setDisplayControlsEnabled(false);

        new SwingWorker<RefreshState, Void>() {
            @Override
            protected RefreshState doInBackground() throws Exception {
                model.refreshDevices();
                List<Device> devices = model.getDevices();
                Device selectedDevice = ensureSelectedDevice(devices);
                Optional<DeviceDetails> details = model.getSelectedDeviceDetails();
                return new RefreshState(devices, selectedDevice, details.orElse(null));
            }

            @Override
            protected void done() {
                try {
                    RefreshState state = get();
                    applyRefreshState(state);
                } catch (Exception e) {
                    handleError(Messages.text("error.devices.load"), e);
                    view.setDevices(List.of(), null);
                    view.clearDeviceDetails();
                    view.clearScreenshot();
                    resetApplicationEnrichmentState();
                    view.setApplicationsLoading(false, "");
                    view.clearApplications();
                    view.clearApplicationDetails();
                    view.setSaveCaptureEnabled(false);
                } finally {
                    loadingDevices = false;
                    view.setDeviceSelectorEnabled(true);
                    view.setRefreshEnabled(true);
                }
            }
        }.execute();
    }

    private Device ensureSelectedDevice(List<Device> devices) {
        Optional<Device> currentSelection = model.getSelectedDevice();
        if (currentSelection.isPresent()) {
            return currentSelection.get();
        }

        Device defaultDevice = chooseDefaultDevice(devices);
        if (defaultDevice == null) {
            return null;
        }

        return model.selectDeviceBySerial(defaultDevice.serial());
    }

    private Device chooseDefaultDevice(List<Device> devices) {
        for (Device device : devices) {
            if (Messages.STATUS_CONNECTED.equals(device.state())) {
                return device;
            }
        }

        return devices.isEmpty() ? null : devices.get(0);
    }

    private void applyRefreshState(RefreshState state) {
        syncingDeviceSelector = true;
        try {
            String selectedSerial = state.selectedDevice() == null ? null : state.selectedDevice().serial();
            view.setDevices(state.devices(), selectedSerial);
        } finally {
            syncingDeviceSelector = false;
        }

        updateDevicePresentation(state.selectedDevice(), state.details());
    }

    private void onDeviceSelected() {
        if (syncingDeviceSelector || loadingDevices) {
            return;
        }

        String serial = view.getSelectedDeviceSerial();
        if (serial == null || serial.equals(currentSelectedSerial)) {
            return;
        }

        view.setDeviceSelectorEnabled(false);
        view.setCaptureEnabled(false);
        view.setDisplayControlsEnabled(false);

        new SwingWorker<DeviceDetails, Void>() {
            @Override
            protected DeviceDetails doInBackground() throws Exception {
                model.selectDeviceBySerial(serial);
                return model.getSelectedDeviceDetails().orElse(null);
            }

            @Override
            protected void done() {
                try {
                    DeviceDetails details = get();
                    Device selectedDevice = model.getSelectedDevice().orElse(null);
                    view.clearScreenshot();
                    updateDevicePresentation(selectedDevice, details);
                } catch (Exception e) {
                    handleError(Messages.text("error.device.select"), e);
                } finally {
                    view.setDeviceSelectorEnabled(true);
                }
            }
        }.execute();
    }

    private void captureScreenshot() {
        view.setCaptureEnabled(false);

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                byte[] screenshotBytes = model.captureSelectedDeviceScreenshot();
                BufferedImage screenshot = ImageIO.read(new ByteArrayInputStream(screenshotBytes));
                if (screenshot == null) {
                    throw new IllegalStateException(Messages.text("error.capture.invalidImage"));
                }
                return screenshot;
            }

            @Override
            protected void done() {
                try {
                    BufferedImage screenshot = get();
                    view.setScreenshot(screenshot);
                    view.setSaveCaptureEnabled(true);
                } catch (Exception e) {
                    handleError(Messages.text("error.capture"), e);
                } finally {
                    Device selectedDevice = model.getSelectedDevice().orElse(null);
                    view.setCaptureEnabled(isCaptureAvailable(selectedDevice));
                }
            }
        }.execute();
    }

    private void saveScreenshot() {
        BufferedImage screenshot = view.getCurrentScreenshot();
        if (screenshot == null) {
            view.showError(Messages.text("error.save.empty"));
            return;
        }

        File outputFile = view.chooseScreenshotDestination();
        if (outputFile == null) {
            return;
        }

        try {
            ImageIO.write(screenshot, "png", outputFile);
            view.showInfo(Messages.format("info.screenshot.saved", outputFile.getAbsolutePath()));
        } catch (IOException e) {
            handleError(Messages.text("error.save"), e);
        }
    }

    private void applyThemeSelection() {
        view.setTheme(view.getSelectedTheme());
    }

    private void applyLanguageSelection() {
        Language language = view.getSelectedLanguage();
        Messages.setLanguage(language);
        view.setLanguage(language);
    }

    private void openRepository() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(Messages.repositoryUrl()));
                return;
            }
        } catch (Exception e) {
            handleError(Messages.text("error.repository.open"), e);
            return;
        }

        view.showInfo(Messages.repositoryUrl());
    }

    private void updateDevicePresentation(Device selectedDevice, DeviceDetails details) {
        String previousSerial = currentSelectedSerial;
        currentSelectedSerial = selectedDevice == null ? null : selectedDevice.serial();

        if (!Objects.equals(previousSerial, currentSelectedSerial)) {
            applicationsLoadedSerial = null;
            currentSelectedPackageName = null;
            resetApplicationEnrichmentState();
            view.setApplicationsLoading(false, "");
            view.clearApplications();
            view.clearApplicationDetails();
        }

        if (details == null) {
            view.clearDeviceDetails();
        } else {
            view.setDeviceDetails(details);
        }

        if (selectedDevice == null) {
            view.clearScreenshot();
        }

        view.setCaptureEnabled(isCaptureAvailable(selectedDevice));
        view.setSaveCaptureEnabled(view.getCurrentScreenshot() != null);
        view.setApplicationsEnabled(isApplicationsAvailable(selectedDevice));
        view.setApplicationActionsEnabled(isApplicationsAvailable(selectedDevice)
                && view.getCurrentApplicationDetails() != null);
        view.setDisplayControlsEnabled(isDisplayAvailable(selectedDevice));

        if (view.isAppsScreenVisible()) {
            ensureApplicationsLoaded();
        }
    }

    private void showAppsScreen() {
        view.showAppsScreen();
        ensureApplicationsLoaded();
    }

    private void ensureApplicationsLoaded() {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isApplicationsAvailable(selectedDevice)) {
            resetApplicationEnrichmentState();
            view.setApplicationsLoading(false, "");
            view.clearApplications();
            view.clearApplicationDetails();
            view.setApplicationsEnabled(false);
            return;
        }

        if (loadingApplications) {
            return;
        }

        if (Objects.equals(applicationsLoadedSerial, selectedDevice.serial())) {
            view.setApplicationsEnabled(true);
            view.setApplicationActionsEnabled(view.getCurrentApplicationDetails() != null);
            return;
        }

        loadApplications();
    }

    private void loadApplications() {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isApplicationsAvailable(selectedDevice)) {
            resetApplicationEnrichmentState();
            view.setApplicationsLoading(false, "");
            view.clearApplications();
            view.clearApplicationDetails();
            view.setApplicationsEnabled(false);
            return;
        }

        String requestedSerial = selectedDevice.serial();
        String preferredPackage = currentSelectedPackageName;
        loadingApplications = true;
        resetApplicationEnrichmentState();
        view.setApplicationsLoading(true, Messages.text("apps.loading.list"));
        view.setApplicationsEnabled(false);
        view.setApplicationActionsEnabled(false);

        new SwingWorker<List<InstalledApp>, Void>() {
            @Override
            protected List<InstalledApp> doInBackground() throws Exception {
                return model.getSelectedDeviceApplications();
            }

            @Override
            protected void done() {
                try {
                    List<InstalledApp> applications = get();
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return;
                    }

                    applicationsLoadedSerial = requestedSerial;
                    view.setApplications(applications, preferredPackage);
                    if (applications.isEmpty()) {
                        view.setApplicationsLoading(false, "");
                        currentSelectedPackageName = null;
                        view.clearApplicationDetails();
                    } else {
                        refreshVisibleApplicationSummaries();
                        String selectedPackage = view.getSelectedApplicationPackage();
                        if (selectedPackage != null && !selectedPackage.isBlank()) {
                            loadApplicationDetails(selectedPackage);
                        }
                    }
                } catch (Exception e) {
                    handleError(Messages.text("error.apps.load"), e);
                    resetApplicationEnrichmentState();
                    view.setApplicationsLoading(false, "");
                    view.clearApplications();
                    view.clearApplicationDetails();
                    applicationsLoadedSerial = null;
                } finally {
                    loadingApplications = false;
                    if (Objects.equals(requestedSerial, currentSelectedSerial) && view.isAppsScreenVisible()) {
                        view.setApplicationsEnabled(true);
                        refreshVisibleApplicationSummaries();
                    }
                }
            }
        }.execute();
    }

    private void onApplicationSelected() {
        if (loadingApplications || loadingApplicationDetails) {
            return;
        }

        String packageName = view.getSelectedApplicationPackage();
        if (packageName == null || packageName.isBlank()) {
            currentSelectedPackageName = null;
            view.clearApplicationDetails();
            return;
        }

        if (packageName.equals(currentSelectedPackageName) && view.getCurrentApplicationDetails() != null) {
            return;
        }

        loadApplicationDetails(packageName);
    }

    private void loadApplicationDetails(String packageName) {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isApplicationsAvailable(selectedDevice)) {
            view.clearApplicationDetails();
            return;
        }

        String requestedSerial = selectedDevice.serial();
        String requestedPackage = packageName;
        currentSelectedPackageName = packageName;
        loadingApplicationDetails = true;
        view.setApplicationActionsEnabled(false);
        view.showApplicationDetailsLoading(requestedPackage);

        new SwingWorker<AppDetails, Void>() {
            @Override
            protected AppDetails doInBackground() throws Exception {
                return model.getSelectedDeviceApplicationDetails(requestedPackage);
            }

            @Override
            protected void done() {
                String selectedPackageNow = view.getSelectedApplicationPackage();
                try {
                    AppDetails details = get();
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)
                            || !Objects.equals(requestedPackage, selectedPackageNow)) {
                        return;
                    }

                    currentSelectedPackageName = details.app().packageName();
                    enrichedApplicationPackages.add(details.app().packageName());
                    pendingApplicationPackages.remove(details.app().packageName());
                    view.updateApplication(details.toListEntry().withFlags(
                            details.app().systemApp(),
                            details.app().disabled()));
                    view.setApplicationDetails(details);
                } catch (Exception e) {
                    if (Objects.equals(requestedPackage, selectedPackageNow)) {
                        handleError(Messages.text("error.apps.details"), e);
                        view.clearApplicationDetails();
                    }
                } finally {
                    loadingApplicationDetails = false;
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return;
                    }

                    String nextSelectedPackage = view.getSelectedApplicationPackage();
                    if (nextSelectedPackage != null
                            && !nextSelectedPackage.isBlank()
                            && !Objects.equals(nextSelectedPackage, requestedPackage)) {
                        loadApplicationDetails(nextSelectedPackage);
                        return;
                    }

                    view.setApplicationActionsEnabled(view.getCurrentApplicationDetails() != null);
                }
            }
        }.execute();
    }

    private void toggleApplicationPermission(String packageName, String permission, boolean granted) {
        runApplicationCommand(
                packageName,
                Messages.text("error.apps.permission"),
                null,
                true,
                () -> model.setSelectedDeviceApplicationPermission(packageName, permission, granted));
    }

    private void openSelectedApplication() {
        String packageName = selectedApplicationPackage();
        if (packageName == null) {
            return;
        }

        runApplicationCommand(
                packageName,
                Messages.text("error.apps.open"),
                null,
                false,
                () -> model.openSelectedDeviceApplication(packageName));
    }

    private void stopSelectedApplication() {
        String packageName = selectedApplicationPackage();
        if (packageName == null) {
            return;
        }

        runApplicationCommand(
                packageName,
                Messages.text("error.apps.stop"),
                null,
                false,
                () -> model.stopSelectedDeviceApplication(packageName));
    }

    private void uninstallSelectedApplication() {
        String packageName = selectedApplicationPackage();
        if (packageName == null) {
            return;
        }

        if (!view.confirmAction(
                Messages.text("apps.confirm.uninstall.title"),
                Messages.format("apps.confirm.uninstall.message", packageName))) {
            return;
        }

        runApplicationCommand(
                packageName,
                Messages.text("error.apps.uninstall"),
                Messages.format("info.app.uninstalled", packageName),
                true,
                () -> {
                    model.uninstallSelectedDeviceApplication(packageName);
                    currentSelectedPackageName = null;
                });
    }

    private void toggleSelectedApplicationEnabled() {
        AppDetails details = view.getCurrentApplicationDetails();
        if (details == null) {
            view.showError(Messages.text("error.apps.noSelection"));
            return;
        }

        boolean enable = details.app().disabled();
        if (!enable && !view.confirmAction(
                Messages.text("apps.confirm.disable.title"),
                Messages.format("apps.confirm.disable.message", details.displayName()))) {
            return;
        }

        runApplicationCommand(
                details.app().packageName(),
                Messages.text("error.apps.toggle"),
                Messages.format("info.app.stateUpdated", details.app().packageName()),
                true,
                () -> model.setSelectedDeviceApplicationEnabled(details.app().packageName(), enable));
    }

    private void clearSelectedApplicationData() {
        String packageName = selectedApplicationPackage();
        if (packageName == null) {
            return;
        }

        if (!view.confirmAction(
                Messages.text("apps.confirm.clearData.title"),
                Messages.format("apps.confirm.clearData.message", packageName))) {
            return;
        }

        runApplicationCommand(
                packageName,
                Messages.text("error.apps.clearData"),
                Messages.format("info.app.dataCleared", packageName),
                true,
                () -> model.clearSelectedDeviceApplicationData(packageName));
    }

    private void clearSelectedApplicationCache() {
        String packageName = selectedApplicationPackage();
        if (packageName == null) {
            return;
        }

        if (!view.confirmAction(
                Messages.text("apps.confirm.clearCache.title"),
                Messages.format("apps.confirm.clearCache.message", packageName))) {
            return;
        }

        runApplicationCommand(
                packageName,
                Messages.text("error.apps.clearCache"),
                Messages.format("info.app.cacheCleared", packageName),
                true,
                () -> model.clearSelectedDeviceApplicationCache(packageName));
    }

    private void exportSelectedApplicationApk() {
        AppDetails details = view.getCurrentApplicationDetails();
        if (details == null) {
            view.showError(Messages.text("error.apps.noSelection"));
            return;
        }

        File outputFile = view.chooseApkDestination(details.app().defaultApkFileName());
        if (outputFile == null) {
            return;
        }

        runApplicationCommand(
                details.app().packageName(),
                Messages.text("error.apps.export"),
                Messages.format("info.app.apk.saved", outputFile.getAbsolutePath()),
                false,
                () -> model.exportSelectedDeviceApplicationApk(details.app().packageName(), outputFile));
    }

    private void runApplicationCommand(
            String packageName,
            String errorMessage,
            String successMessage,
            boolean reloadApplications,
            ApplicationTask task) {
        if (packageName == null || packageName.isBlank()) {
            view.showError(Messages.text("error.apps.noSelection"));
            return;
        }

        view.setApplicationActionsEnabled(false);
        view.setApplicationsEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                task.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (successMessage != null && !successMessage.isBlank()) {
                        view.showInfo(successMessage);
                    }

                    if (reloadApplications) {
                        applicationsLoadedSerial = null;
                        loadApplications();
                    } else if (packageName.equals(view.getSelectedApplicationPackage())) {
                        view.setApplicationsEnabled(true);
                        view.setApplicationActionsEnabled(view.getCurrentApplicationDetails() != null);
                    }
                } catch (Exception e) {
                    handleError(errorMessage, e);
                    if (reloadApplications) {
                        applicationsLoadedSerial = null;
                        loadApplications();
                    } else {
                        view.setApplicationsEnabled(true);
                        view.setApplicationActionsEnabled(view.getCurrentApplicationDetails() != null);
                    }
                }
            }
        }.execute();
    }

    private String selectedApplicationPackage() {
        String packageName = view.getSelectedApplicationPackage();
        if (packageName == null || packageName.isBlank()) {
            view.showError(Messages.text("error.apps.noSelection"));
            return null;
        }
        return packageName;
    }

    private boolean isCaptureAvailable(Device selectedDevice) {
        return selectedDevice != null && Messages.STATUS_CONNECTED.equals(selectedDevice.state());
    }

    private boolean isApplicationsAvailable(Device selectedDevice) {
        return selectedDevice != null && Messages.STATUS_CONNECTED.equals(selectedDevice.state());
    }

    private boolean isDisplayAvailable(Device selectedDevice) {
        return selectedDevice != null && Messages.STATUS_CONNECTED.equals(selectedDevice.state());
    }

    private void applyDisplayOverride() {
        Integer width = view.getRequestedDisplayWidth();
        Integer height = view.getRequestedDisplayHeight();
        Integer density = view.getRequestedDisplayDensity();

        if (width == null || height == null || density == null) {
            view.showError(Messages.text("error.display.invalidInput"));
            return;
        }

        runDisplayCommand(
                Messages.text("error.display.apply"),
                Messages.format("info.display.updated", width + "x" + height, density),
                () -> model.setSelectedDeviceDisplay(width, height, density));
    }

    private void resetDisplayOverride() {
        runDisplayCommand(
                Messages.text("error.display.reset"),
                Messages.text("info.display.reset"),
                model::resetSelectedDeviceDisplay);
    }

    private void toggleDeviceDarkMode() {
        boolean enabled = view.isDeviceDarkModeSelected();
        runDisplayCommand(
                Messages.text("error.display.darkMode"),
                null,
                () -> model.setSelectedDeviceDarkMode(enabled));
    }

    private void openWirelessAssistant() {
        WirelessConnectionDialog dialog = view.getWirelessConnectionDialog();
        currentQrPayload = null;
        dialog.clearQrPayload();
        dialog.setBusy(false);
        dialog.showStatus(Messages.text("wireless.status.loading"), false);
        dialog.open();

        new SwingWorker<AdbToolInfo, Void>() {
            @Override
            protected AdbToolInfo doInBackground() throws Exception {
                return model.getAdbToolInfo();
            }

            @Override
            protected void done() {
                try {
                    AdbToolInfo toolInfo = get();
                    dialog.setToolInfo(toolInfo);
                    dialog.showStatus(Messages.text("wireless.status.ready"), false);
                } catch (Exception e) {
                    dialog.setToolInfo(new AdbToolInfo("-", "-", false, false));
                    dialog.showStatus(Messages.text("wireless.status.capabilitiesError"), true);
                }
            }
        }.execute();
    }

    private void pairWirelessDeviceByCode() {
        WirelessConnectionDialog dialog = view.getWirelessConnectionDialog();
        Integer pairingPort = dialog.getPairPort();
        String host = dialog.getPairHost();
        String pairingCode = dialog.getPairCode();

        if (pairingPort == null) {
            dialog.showStatus(Messages.text("error.wireless.invalidPort"), true);
            return;
        }

        dialog.showStatus(Messages.text("wireless.status.pairing"), false);
        dialog.setBusy(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                model.pairWirelessDevice(host, pairingPort, pairingCode);
                Thread.sleep(1200L);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    dialog.showStatus(Messages.text("wireless.status.paired"), false);
                    refreshDevices();
                } catch (Exception e) {
                    dialog.showStatus(extractErrorMessage(e, Messages.text("error.wireless.pair")), true);
                } finally {
                    dialog.setBusy(false);
                }
            }
        }.execute();
    }

    private void connectWirelessDevice() {
        WirelessConnectionDialog dialog = view.getWirelessConnectionDialog();
        Integer connectPort = dialog.getConnectPort();
        if (connectPort == null) {
            dialog.showStatus(Messages.text("error.wireless.invalidConnectPort"), true);
            return;
        }

        dialog.showStatus(Messages.text("wireless.status.connecting"), false);
        dialog.setBusy(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                model.connectWirelessDevice(dialog.getConnectHost(), connectPort);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    dialog.showStatus(Messages.text("wireless.status.connected"), false);
                    refreshDevices();
                } catch (Exception e) {
                    dialog.showStatus(extractErrorMessage(e, Messages.text("error.wireless.connect")), true);
                } finally {
                    dialog.setBusy(false);
                }
            }
        }.execute();
    }

    private void generateWirelessQrPayload() {
        WirelessConnectionDialog dialog = view.getWirelessConnectionDialog();
        try {
            currentQrPayload = WirelessPairingQrPayload.random();
            dialog.setQrPayload(currentQrPayload, SimpleQrCodeGenerator.generate(currentQrPayload.qrPayload(), 7, 4));
            dialog.showStatus(Messages.text("wireless.status.qrGenerated"), false);
        } catch (Exception e) {
            dialog.showStatus(extractErrorMessage(e, Messages.text("error.wireless.qrGenerate")), true);
        }
    }

    private void pairWirelessDeviceByQr() {
        WirelessConnectionDialog dialog = view.getWirelessConnectionDialog();
        if (currentQrPayload == null) {
            dialog.showStatus(Messages.text("error.wireless.qrPayload"), true);
            return;
        }

        dialog.showStatus(Messages.text("wireless.status.waitingForQr"), false);
        dialog.setBusy(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                model.pairWirelessDeviceWithQr(currentQrPayload.serviceName(), currentQrPayload.password(), 45);
                Thread.sleep(1200L);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    dialog.showStatus(Messages.text("wireless.status.qrPaired"), false);
                    refreshDevices();
                } catch (Exception e) {
                    dialog.showStatus(extractErrorMessage(e, Messages.text("error.wireless.qrPair")), true);
                } finally {
                    dialog.setBusy(false);
                }
            }
        }.execute();
    }

    private void runDisplayCommand(String errorMessage, String successMessage, ApplicationTask task) {
        view.setDisplayControlsEnabled(false);

        new SwingWorker<DeviceDetails, Void>() {
            @Override
            protected DeviceDetails doInBackground() throws Exception {
                task.run();
                return model.getSelectedDeviceDetails().orElse(null);
            }

            @Override
            protected void done() {
                try {
                    DeviceDetails details = get();
                    Device selectedDevice = model.getSelectedDevice().orElse(null);
                    updateDevicePresentation(selectedDevice, details);
                    if (successMessage != null && !successMessage.isBlank()) {
                        view.showInfo(successMessage);
                    }
                } catch (Exception e) {
                    handleError(errorMessage, e);
                    Device selectedDevice = model.getSelectedDevice().orElse(null);
                    view.setDisplayControlsEnabled(isDisplayAvailable(selectedDevice));
                }
            }
        }.execute();
    }

    private void handleError(String message, Exception exception) {
        String detail = extractErrorMessage(exception, null);
        if (detail == null || detail.isBlank()) {
            view.showError(message);
        } else {
            view.showError(message + "\n\n" + detail);
        }
    }

    private String extractErrorMessage(Exception exception, String fallback) {
        String detail = exception == null ? null : exception.getMessage();
        if ((detail == null || detail.isBlank()) && exception != null && exception.getCause() != null) {
            detail = exception.getCause().getMessage();
        }
        if ((detail == null || detail.isBlank()) && fallback != null) {
            detail = fallback;
        }
        return detail;
    }

    private void refreshVisibleApplicationSummaries() {
        if (!view.isAppsScreenVisible()) {
            return;
        }

        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isApplicationsAvailable(selectedDevice)
                || !Objects.equals(applicationsLoadedSerial, selectedDevice == null ? null : selectedDevice.serial())) {
            return;
        }

        if (currentSelectedPackageName != null && !currentSelectedPackageName.isBlank()) {
            queueApplicationSummary(currentSelectedPackageName);
        }

        for (String packageName : view.getVisibleApplicationPackages(VISIBLE_APP_ENRICHMENT_EXTRA_ROWS)) {
            queueApplicationSummary(packageName);
        }

        if (!loadingApplications) {
            startApplicationEnrichmentIfNeeded(selectedDevice.serial());
        }
    }

    private void queueApplicationSummary(String packageName) {
        if (packageName == null || packageName.isBlank() || enrichedApplicationPackages.contains(packageName)) {
            return;
        }

        pendingApplicationPackages.add(packageName);
    }

    private void startApplicationEnrichmentIfNeeded(String requestedSerial) {
        if (applicationEnrichmentWorker != null) {
            return;
        }

        if (pendingApplicationPackages.isEmpty()) {
            view.setApplicationsLoading(false, "");
            return;
        }

        List<String> packageNames = new java.util.ArrayList<>();
        while (!pendingApplicationPackages.isEmpty() && packageNames.size() < MAX_BACKGROUND_APP_ENRICHMENTS) {
            String packageName = pendingApplicationPackages.iterator().next();
            pendingApplicationPackages.remove(packageName);
            if (!enrichedApplicationPackages.contains(packageName)) {
                packageNames.add(packageName);
            }
        }

        if (packageNames.isEmpty()) {
            if (pendingApplicationPackages.isEmpty()) {
                view.setApplicationsLoading(false, "");
            } else {
                startApplicationEnrichmentIfNeeded(requestedSerial);
            }
            return;
        }

        final int total = packageNames.size();
        view.setApplicationsLoading(true, Messages.format("apps.loading.metadata.progress", 0, total));

        applicationEnrichmentWorker = new SwingWorker<>() {
            private int enrichedCount;

            @Override
            protected Void doInBackground() {
                for (String packageName : packageNames) {
                    if (isCancelled() || !Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return null;
                    }

                    try {
                        InstalledApp application = model.getSelectedDeviceApplicationSummary(packageName);
                        publish(application);
                    } catch (Exception ignored) {
                    }
                }
                return null;
            }

            @Override
            protected void process(List<InstalledApp> chunks) {
                if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                    return;
                }

                for (InstalledApp application : chunks) {
                    if (application != null) {
                        enrichedApplicationPackages.add(application.packageName());
                        pendingApplicationPackages.remove(application.packageName());
                        view.updateApplication(application);
                        enrichedCount++;
                    }
                }

                if (enrichedCount < total) {
                    view.setApplicationsLoading(true,
                            Messages.format("apps.loading.metadata.progress", enrichedCount, total));
                }
            }

            @Override
            protected void done() {
                if (applicationEnrichmentWorker != this) {
                    return;
                }

                applicationEnrichmentWorker = null;
                if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                    return;
                }

                if (!pendingApplicationPackages.isEmpty()) {
                    startApplicationEnrichmentIfNeeded(requestedSerial);
                    return;
                }

                view.setApplicationsLoading(false, "");
            }
        };

        applicationEnrichmentWorker.execute();
    }

    private void cancelApplicationEnrichment() {
        if (applicationEnrichmentWorker != null) {
            applicationEnrichmentWorker.cancel(true);
            applicationEnrichmentWorker = null;
        }
    }

    private void resetApplicationEnrichmentState() {
        cancelApplicationEnrichment();
        enrichedApplicationPackages.clear();
        pendingApplicationPackages.clear();
    }

    private record RefreshState(List<Device> devices, Device selectedDevice, DeviceDetails details) {
    }
}
