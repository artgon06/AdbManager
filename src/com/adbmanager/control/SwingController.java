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
import java.util.concurrent.CancellationException;

import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.SwingWorker;

import com.adbmanager.logic.AdbModel;
import com.adbmanager.logic.ScrcpyService;
import com.adbmanager.logic.UserConfigService;
import com.adbmanager.logic.model.AppBackgroundMode;
import com.adbmanager.logic.model.AppDetails;
import com.adbmanager.logic.model.AdbToolInfo;
import com.adbmanager.logic.model.AppInstallRequest;
import com.adbmanager.logic.model.AppInstallResult;
import com.adbmanager.logic.model.ControlState;
import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.DeviceDirectoryListing;
import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.logic.model.DeviceFileEntry;
import com.adbmanager.logic.model.DevicePowerAction;
import com.adbmanager.logic.model.DeviceSoundMode;
import com.adbmanager.logic.model.FileTransferProgress;
import com.adbmanager.logic.model.InstalledApp;
import com.adbmanager.logic.model.ScrcpyCamera;
import com.adbmanager.logic.model.ScrcpyLaunchRequest;
import com.adbmanager.logic.model.ScrcpyStatus;
import com.adbmanager.logic.model.SystemState;
import com.adbmanager.logic.model.UserConfig;
import com.adbmanager.logic.model.WirelessEndpointDiscovery;
import com.adbmanager.logic.model.WirelessPairingResult;
import com.adbmanager.logic.model.WirelessPairingQrPayload;
import com.adbmanager.view.Messages;
import com.adbmanager.view.Messages.Language;
import com.adbmanager.view.swing.AppInstallDialog;
import com.adbmanager.view.swing.AppTheme;
import com.adbmanager.view.swing.MainFrame;
import com.adbmanager.view.swing.SimpleQrCodeGenerator;
import com.adbmanager.view.swing.WirelessConnectionDialog;

public class SwingController {

    private static final int MAX_BACKGROUND_APP_ENRICHMENTS = 50;
    private static final int VISIBLE_APP_ENRICHMENT_EXTRA_ROWS = 8;

    @FunctionalInterface
    private interface ApplicationTask {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface FileTransferTask {
        void run(java.util.function.Consumer<FileTransferProgress> progressCallback) throws Exception;
    }

    private final AdbModel model;
    private final ScrcpyService scrcpyService;
    private final UserConfigService userConfigService = new UserConfigService();
    private final MainFrame view;
    private boolean syncingDeviceSelector;
    private boolean loadingDevices;
    private boolean loadingApplications;
    private boolean loadingApplicationDetails;
    private boolean loadingScrcpyStatus;
    private boolean loadingScrcpyApplications;
    private boolean loadingScrcpyCameras;
    private boolean loadingSystemState;
    private boolean loadingControlState;
    private boolean loadingFiles;
    private boolean preparingScrcpy;
    private boolean launchingScrcpy;
    private boolean applyingSystemAction;
    private boolean applyingControlAction;
    private boolean applyingFileAction;
    private String currentSelectedSerial;
    private String applicationsLoadedSerial;
    private String filesLoadedSerial;
    private String scrcpyApplicationsLoadedSerial;
    private String scrcpyCamerasLoadedSerial;
    private String currentSelectedPackageName;
    private String currentFilesDirectory;
    private SwingWorker<Void, List<InstalledApp>> applicationEnrichmentWorker;
    private final Set<String> enrichedApplicationPackages = new HashSet<>();
    private final Set<String> pendingApplicationPackages = new LinkedHashSet<>();
    private int totalApplicationPackagesToEnrich;
    private boolean autoRefreshOnFocus = true;
    private WirelessPairingQrPayload currentQrPayload;
    private SwingWorker<Void, WirelessEndpointDiscovery> wirelessEndpointDiscoveryWorker;
    private SwingWorker<DeviceDirectoryListing, FileTransferProgress> filesTransferWorker;
    private boolean cancellingFileTransfer;
    private String pendingPowerActionSerial;
    private long pendingPowerActionUntilMs;

    public SwingController(AdbModel model, ScrcpyService scrcpyService, MainFrame view) {
        this.model = model;
        this.scrcpyService = scrcpyService;
        this.view = view;
    }

    public void start() {
        UserConfig userConfig = loadUserConfig();
        Messages.setLanguage(userConfig.language());
        view.setSelectedLanguage(userConfig.language());
        view.setLanguage(userConfig.language());
        view.setSelectedTheme(userConfig.theme());
        view.setTheme(userConfig.theme());
        autoRefreshOnFocus = userConfig.autoRefreshOnFocus();
        view.setAutoRefreshOnFocusSelected(autoRefreshOnFocus);
        view.setUseCustomAdbPathSelected(userConfig.useCustomAdbPath());
        view.setCustomAdbPath(userConfig.customAdbPath());
        view.setScrcpyLaunchRequest(userConfig.scrcpyLaunchRequest());
        cleanupScrcpyLogsSafely();

        bindEvents();
        view.setScrcpyDeviceAvailable(false);
        view.setSystemDeviceAvailable(false);
        view.setControlDeviceAvailable(false);
        view.setFilesDeviceAvailable(false);
        view.setPowerActionsEnabled(false);
        view.setTcpipEnabled(false);
        view.setSystemBusy(false);
        view.setControlBusy(false);
        view.setFilesBusy(false);
        view.clearSystemState();
        view.clearControlState();
        view.clearFilesListing();
        view.setSystemStatus("", false);
        view.setControlStatus("", false);
        view.setFilesStatus("", false);
        view.clearFilesTransferProgress();
        view.setFilesTransferCancelable(false);
        view.showHomeScreen();
        view.showWindow();
        saveUserConfigSafely();
        refreshDevices();
    }

    private void bindEvents() {
        WirelessConnectionDialog wirelessDialog = view.getWirelessConnectionDialog();

        view.setDeviceSelectionAction(event -> onDeviceSelected());
        view.setCaptureAction(event -> captureScreenshot());
        view.setSaveCaptureAction(event -> saveScreenshot());
        view.setRefreshAction(event -> refreshDevices());
        view.setWirelessAssistantAction(event -> openWirelessAssistant());
        view.setTcpipAction(event -> connectSelectedUsbDeviceOverTcpip());
        view.setPowerAction(event -> executePowerAction(DevicePowerAction.fromActionCommand(event.getActionCommand())));
        view.setHomeAction(event -> view.showHomeScreen());
        view.setDisplayAction(event -> showDisplayScreen());
        view.setControlAction(event -> showControlScreen());
        view.setApplyDisplayAction(event -> applyDisplayOverride());
        view.setResetDisplayAction(event -> resetDisplayOverride());
        view.setDeviceDarkModeAction(event -> toggleDeviceDarkMode());
        view.setQuickControlKeyEventAction(event -> sendQuickControlKeyEvent(event.getActionCommand()));
        view.setControlTextAction(event -> sendControlTextInput());
        view.setControlBrightnessAction(event -> applyControlBrightness());
        view.setControlVolumeAction(event -> applyControlVolume());
        view.setControlSoundModeAction(event -> applyControlSoundMode());
        view.setControlTapAction(event -> applyControlTap());
        view.setControlSwipeAction(event -> applyControlSwipe());
        view.setControlKeyEventAction(event -> applyControlManualKeyEvent());
        view.setControlRawInputAction(event -> applyControlRawInputCommand());
        view.setPrepareScrcpyAction(event -> prepareScrcpy());
        view.setLaunchScrcpyAction(event -> launchScrcpy());
        view.setBrowseScrcpyRecordPathAction(event -> chooseScrcpyRecordingPath());
        view.setRefreshScrcpyCamerasAction(event -> loadScrcpyCameras(true));
        view.setScrcpyLaunchTargetChangeAction(event -> onScrcpyTargetChanged());
        view.setScrcpyStartAppToggleAction(event -> onScrcpyStartAppToggle());
        view.setAppsAction(event -> showAppsScreen());
        view.setFilesAction(event -> showFilesScreen());
        view.setSystemAction(event -> showSystemScreen());
        view.setSettingsAction(event -> view.showSettingsScreen());
        view.setThemeChangeAction(event -> applyThemeSelection());
        view.setLanguageChangeAction(event -> applyLanguageSelection());
        view.setAutoRefreshOnFocusChangeAction(event -> {
            autoRefreshOnFocus = view.isAutoRefreshOnFocusSelected();
            saveUserConfigSafely();
        });
        view.setUseCustomAdbPathChangeAction(event -> applyAdbPathSettings());
        view.setCustomAdbPathCommitAction(event -> applyAdbPathSettings());
        view.setCustomAdbPathBrowseAction(event -> browseCustomAdbPath());
        view.setRepositoryAction(event -> openRepository());
        view.setScrcpyRepositoryAction(event -> openUrl("https://github.com/Genymobile/scrcpy"));
        view.setDeviceCatalogAction(event -> openUrl("https://github.com/pbakondy/android-device-list"));
        view.setApplicationSelectionAction(this::onApplicationSelected);
        view.setApplicationPermissionToggleHandler(this::toggleApplicationPermission);
        view.setApplicationBackgroundModeChangeHandler(this::changeApplicationBackgroundMode);
        view.setOpenApplicationAction(event -> openSelectedApplication());
        view.setStopApplicationAction(event -> stopSelectedApplication());
        view.setUninstallApplicationAction(event -> uninstallSelectedApplication());
        view.setToggleApplicationEnabledAction(event -> toggleSelectedApplicationEnabled());
        view.setClearApplicationDataAction(event -> clearSelectedApplicationData());
        view.setClearApplicationCacheAction(event -> clearSelectedApplicationCache());
        view.setExportApplicationApkAction(event -> exportSelectedApplicationApk());
        view.setInstallApplicationsAction(event -> openApplicationInstallDialog());
        view.setFilesNavigateUpAction(event -> navigateToFilesParentDirectory());
        view.setFilesRefreshAction(event -> refreshFilesDirectory(true, true, view.getCurrentFilesDirectory()));
        view.setFilesPathSubmitAction(event -> refreshFilesDirectory(true, true, view.getEnteredFilesDirectory()));
        view.setFilesTransferCancelAction(event -> cancelFilesTransfer());
        view.setFilesCreateFolderAction(event -> createFilesDirectory());
        view.setFilesUploadAction(event -> uploadFilesToCurrentDirectory());
        view.setFilesDownloadAction(event -> downloadSelectedFiles());
        view.setFilesRenameAction(event -> renameSelectedFile());
        view.setFilesCopyAction(event -> copySelectedFile());
        view.setFilesDeleteAction(event -> deleteSelectedFiles());
        view.setFilesOpenDirectoryAction(this::openSelectedDirectory);
        view.setFilesDropHandler(this::uploadDroppedFiles);
        view.setRefreshSystemUsersAction(event -> refreshSystemState(true));
        view.setCreateSystemUserAction(event -> createSystemUser());
        view.setSwitchSystemUserAction(event -> switchSystemUser());
        view.setDeleteSystemUserAction(event -> deleteSystemUser());
        view.setApplySystemAppLanguagesAction(event -> applySystemAppLanguages());
        view.setApplySystemGesturesAction(event -> applySystemGestures());
        view.setRefreshSystemKeyboardsAction(event -> refreshSystemState(true));
        view.setEnableSystemKeyboardAction(event -> enableSystemKeyboard());
        view.setSetSystemKeyboardAction(event -> setSystemKeyboard());
        wirelessDialog.setConnectAction(event -> connectWirelessDevice());
        wirelessDialog.setPairCodeAction(event -> pairWirelessDeviceByCode());
        wirelessDialog.setGenerateQrAction(event -> generateWirelessQrPayload());
        wirelessDialog.setPairQrAction(event -> pairWirelessDeviceByQr());
        view.getAppInstallDialog().setInstallAction(event -> installSelectedPackages());
        view.setApplicationsViewportChangeAction(this::refreshVisibleApplicationSummaries);
        view.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent event) {
                if (autoRefreshOnFocus) {
                    refreshDevices();
                }
            }
        });
        view.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                saveUserConfigSafely();
            }
        });
    }

    private void refreshDevices() {
        clearPowerActionPendingIfExpired();
        if (loadingDevices) {
            return;
        }

        loadingDevices = true;
        updateSystemBusyState();
        updateControlBusyState();
        updateFilesBusyState();
        view.setDeviceSelectorEnabled(false);
        view.setCaptureEnabled(false);
        view.setRefreshEnabled(false);
        view.setPowerActionsEnabled(false);
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
                    filesLoadedSerial = null;
                    currentFilesDirectory = null;
                    view.setDevices(List.of(), null);
                    view.clearDeviceDetails();
                    view.clearSystemState();
                    view.clearScreenshot();
                    view.setScrcpyDeviceAvailable(false);
                    view.setSystemDeviceAvailable(false);
                    view.setControlDeviceAvailable(false);
                    view.setFilesDeviceAvailable(false);
                    view.setTcpipEnabled(false);
                    view.setSystemStatus("", false);
                    view.setControlStatus("", false);
                    view.setFilesStatus("", false);
                    view.clearFilesTransferProgress();
                    view.setFilesTransferCancelable(false);
                    view.setScrcpyAvailableApps(List.of());
                    view.setScrcpyAvailableCameras(List.of());
                    resetApplicationEnrichmentState();
                    view.setApplicationsLoading(false, "");
                    view.clearApplications();
                    view.clearApplicationDetails();
                    view.clearControlState();
                    view.clearFilesListing();
                    view.setSaveCaptureEnabled(false);
                } finally {
                    loadingDevices = false;
                    updateSystemBusyState();
                    updateControlBusyState();
                    updateFilesBusyState();
                    view.setDeviceSelectorEnabled(true);
                    view.setRefreshEnabled(true);
                    Device selectedDevice = model.getSelectedDevice().orElse(null);
                    view.setPowerActionsEnabled(isDisplayAvailable(selectedDevice));
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
        view.setSystemBusy(true);

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
                    updateSystemBusyState();
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
        saveUserConfigSafely();
    }

    private void applyLanguageSelection() {
        Language language = view.getSelectedLanguage();
        Messages.setLanguage(language);
        view.setLanguage(language);
        saveUserConfigSafely();
    }

    private void openRepository() {
        openUrl(Messages.repositoryUrl());
    }

    private void executePowerAction(DevicePowerAction action) {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isDisplayAvailable(selectedDevice)) {
            view.showError(Messages.text("error.power.deviceRequired"));
            return;
        }

        DevicePowerAction safeAction = action == null ? DevicePowerAction.REBOOT_ANDROID : action;
        String actionLabel = Messages.text(safeAction.messageKey());
        String deviceLabel = selectedDevice == null ? "-" : selectedDevice.serial();

        if (!view.confirmAction(
                Messages.text("power.confirm.title"),
                Messages.format("power.confirm.message", actionLabel, deviceLabel))) {
            return;
        }

        view.setPowerActionsEnabled(false);
        view.setRefreshEnabled(false);
        view.setDeviceSelectorEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                model.performSelectedDevicePowerAction(safeAction);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    markPowerActionPending(selectedDevice.serial());
                    view.showInfo(Messages.format("info.power.sent", actionLabel));
                    refreshDevices();
                    schedulePostPowerActionRefresh(4000);
                    schedulePostPowerActionRefresh(12000);
                } catch (Exception e) {
                    handleError(Messages.text("error.power.action"), e);
                    Device currentDevice = model.getSelectedDevice().orElse(null);
                    view.setPowerActionsEnabled(isDisplayAvailable(currentDevice));
                    view.setRefreshEnabled(true);
                    view.setDeviceSelectorEnabled(true);
                }
            }
        }.execute();
    }

    private void browseCustomAdbPath() {
        File selectedFile = view.chooseAdbExecutable();
        if (selectedFile == null) {
            return;
        }

        view.setCustomAdbPath(selectedFile.getAbsolutePath());
        applyAdbPathSettings();
    }

    private void applyAdbPathSettings() {
        saveUserConfigSafely();
        refreshDevices();
    }

    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception e) {
            handleError(Messages.text("error.repository.open"), e);
            return;
        }

        view.showInfo(url);
    }

    private void markPowerActionPending(String serial) {
        if (serial == null || serial.isBlank()) {
            return;
        }

        pendingPowerActionSerial = serial;
        pendingPowerActionUntilMs = System.currentTimeMillis() + 25000L;
    }

    private void clearPowerActionPendingIfExpired() {
        if (pendingPowerActionSerial == null) {
            return;
        }

        if (System.currentTimeMillis() > pendingPowerActionUntilMs) {
            pendingPowerActionSerial = null;
            pendingPowerActionUntilMs = 0L;
        }
    }

    private boolean isPowerActionPendingFor(String serial) {
        clearPowerActionPendingIfExpired();
        if (serial == null || serial.isBlank()) {
            return false;
        }

        return serial.equals(pendingPowerActionSerial) && System.currentTimeMillis() <= pendingPowerActionUntilMs;
    }

    private boolean shouldSuppressApplicationLoadError(String serial, Exception exception) {
        if (!isPowerActionPendingFor(serial)) {
            return false;
        }

        String detail = extractErrorMessage(exception, "");
        String normalized = detail == null ? "" : detail.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("device offline")
                || normalized.contains("closed")
                || normalized.contains("disconnected")
                || normalized.contains("transport")
                || normalized.contains("not found")
                || normalized.contains("no devices/emulators")
                || normalized.contains("timed out")
                || normalized.contains("timeout");
    }

    private void schedulePostPowerActionRefresh(int delayMs) {
        Timer timer = new Timer(delayMs, event -> {
            Timer source = (Timer) event.getSource();
            source.stop();
            if (pendingPowerActionSerial != null) {
                refreshDevices();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void updateDevicePresentation(Device selectedDevice, DeviceDetails details) {
        String previousSerial = currentSelectedSerial;
        currentSelectedSerial = selectedDevice == null ? null : selectedDevice.serial();
        if (pendingPowerActionSerial != null && !pendingPowerActionSerial.equals(currentSelectedSerial)) {
            pendingPowerActionSerial = null;
            pendingPowerActionUntilMs = 0L;
        }

        if (!Objects.equals(previousSerial, currentSelectedSerial)) {
            applicationsLoadedSerial = null;
            filesLoadedSerial = null;
            currentFilesDirectory = null;
            scrcpyApplicationsLoadedSerial = null;
            scrcpyCamerasLoadedSerial = null;
            currentSelectedPackageName = null;
            resetApplicationEnrichmentState();
            view.setApplicationsLoading(false, "");
            view.clearApplications();
            view.clearApplicationDetails();
            view.clearFilesListing();
            view.clearSystemState();
            view.clearControlState();
            view.setSystemStatus("", false);
            view.setControlStatus("", false);
            view.setFilesStatus("", false);
            view.clearFilesTransferProgress();
            view.setFilesTransferCancelable(false);
            view.setScrcpyAvailableApps(List.of());
            view.setScrcpyAvailableCameras(List.of());
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
        boolean displayAvailable = isDisplayAvailable(selectedDevice);
        view.setDisplayControlsEnabled(displayAvailable);
        view.setPowerActionsEnabled(displayAvailable);
        view.setTcpipEnabled(isTcpipAvailable(selectedDevice));
        view.setScrcpyDeviceAvailable(displayAvailable);
        boolean systemAvailable = isSystemAvailable(selectedDevice);
        view.setSystemDeviceAvailable(systemAvailable);
        boolean controlAvailable = isControlAvailable(selectedDevice);
        view.setControlDeviceAvailable(controlAvailable);
        boolean filesAvailable = isFilesAvailable(selectedDevice);
        view.setFilesDeviceAvailable(filesAvailable);
        updateFilesBusyState();

        if (!systemAvailable) {
            view.clearSystemState();
            view.setSystemStatus("", false);
        }
        if (!controlAvailable) {
            view.clearControlState();
            view.setControlStatus("", false);
        }
        if (!filesAvailable) {
            view.clearFilesListing();
            view.setFilesStatus("", false);
            view.clearFilesTransferProgress();
            view.setFilesTransferCancelable(false);
        }

        if (view.isAppsScreenVisible()) {
            ensureApplicationsLoaded();
        }

        if (view.isFilesScreenVisible() && filesAvailable) {
            refreshFilesDirectory(false, false, currentFilesDirectory);
        }

        if (view.isSystemScreenVisible() && systemAvailable) {
            refreshSystemState(false);
        }

        if (view.isControlScreenVisible() && controlAvailable) {
            refreshControlState(false);
        }

        if (view.isDisplayScreenVisible()) {
            refreshScrcpyStatus(false);
            if (view.shouldLoadScrcpyApplications()) {
                loadScrcpyApplications();
            }
            if (view.usesScrcpyCameraSource()) {
                loadScrcpyCameras(false);
            }
        }
    }

    private void showDisplayScreen() {
        view.showDisplayScreen();
        refreshScrcpyStatus(false);
        if (view.shouldLoadScrcpyApplications()) {
            loadScrcpyApplications();
        }
        if (view.usesScrcpyCameraSource()) {
            loadScrcpyCameras(false);
        }
    }

    private void showControlScreen() {
        view.showControlScreen();
        refreshControlState(false);
    }

    private void showAppsScreen() {
        view.showAppsScreen();
        ensureApplicationsLoaded();
    }

    private void showFilesScreen() {
        view.showFilesScreen();
        refreshFilesDirectory(false, false, currentFilesDirectory);
    }

    private void showSystemScreen() {
        view.showSystemScreen();
        refreshSystemState(false);
    }

    private void refreshFilesDirectory(boolean showErrors, boolean forceReload, String preferredPath) {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isFilesAvailable(selectedDevice)) {
            view.setFilesDeviceAvailable(false);
            view.clearFilesListing();
            view.setFilesStatus("", false);
            view.clearFilesTransferProgress();
            view.setFilesTransferCancelable(false);
            return;
        }

        if (loadingFiles || (applyingFileAction && !showErrors)) {
            return;
        }

        String requestedSerial = selectedDevice.serial();
        String targetPath = normalizePath(preferredPath == null || preferredPath.isBlank()
                ? currentFilesDirectory
                : preferredPath);

        if (!forceReload
                && Objects.equals(filesLoadedSerial, requestedSerial)
                && targetPath != null
                && Objects.equals(currentFilesDirectory, targetPath)) {
            view.setFilesDeviceAvailable(true);
            updateFilesBusyState();
            return;
        }

        loadingFiles = true;
        view.setFilesDeviceAvailable(true);
        view.setFilesStatus(Messages.text("files.status.loading"), false);
        view.clearFilesTransferProgress();
        view.setFilesTransferCancelable(false);
        updateFilesBusyState();

        new SwingWorker<DeviceDirectoryListing, Void>() {
            @Override
            protected DeviceDirectoryListing doInBackground() throws Exception {
                return model.listSelectedDeviceDirectory(targetPath);
            }

            @Override
            protected void done() {
                try {
                    DeviceDirectoryListing listing = get();
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return;
                    }

                    filesLoadedSerial = requestedSerial;
                    currentFilesDirectory = listing.currentPath();
                    view.setFilesListing(listing);
                    view.setFilesStatus(Messages.format("files.status.ready", listing.currentPath()), false);
                } catch (Exception e) {
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return;
                    }

                    view.setFilesStatus(extractErrorMessage(e, Messages.text("error.files.load")), true);
                    if (showErrors) {
                        handleError(Messages.text("error.files.load"), e);
                    }
                } finally {
                    loadingFiles = false;
                    updateFilesBusyState();
                }
            }
        }.execute();
    }

    private void navigateToFilesParentDirectory() {
        String parentPath = view.getParentFilesDirectory();
        if (parentPath != null && !parentPath.isBlank()) {
            refreshFilesDirectory(true, true, parentPath);
        }
    }

    private void openSelectedDirectory() {
        DeviceFileEntry selectedEntry = view.getSelectedFileEntry();
        if (selectedEntry != null && selectedEntry.directory()) {
            refreshFilesDirectory(false, true, selectedEntry.path());
        }
    }

    private void createFilesDirectory() {
        String currentDirectory = view.getCurrentFilesDirectory();
        if (currentDirectory == null || currentDirectory.isBlank()) {
            view.showError(Messages.text("error.files.deviceRequired"));
            return;
        }

        String directoryName = view.promptText(
                Messages.text("files.dialog.createFolder.title"),
                Messages.text("files.dialog.createFolder.message"),
                Messages.text("files.dialog.createFolder.default"));
        if (directoryName == null) {
            return;
        }

        runFilesCommand(
                Messages.text("error.files.createFolder"),
                Messages.format("files.status.folderCreated", directoryName.trim()),
                true,
                () -> model.createSelectedDeviceDirectory(currentDirectory, directoryName));
    }

    private void uploadFilesToCurrentDirectory() {
        List<File> localFiles = view.chooseFilesToUpload();
        if (localFiles == null || localFiles.isEmpty()) {
            return;
        }
        uploadLocalFiles(localFiles);
    }

    private void uploadDroppedFiles(List<File> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        uploadLocalFiles(files);
    }

    private void uploadLocalFiles(List<File> files) {
        String currentDirectory = view.getCurrentFilesDirectory();
        if (currentDirectory == null || currentDirectory.isBlank()) {
            view.showError(Messages.text("error.files.deviceRequired"));
            return;
        }

        runFilesTransferCommand(
                Messages.text("error.files.upload"),
                Messages.format("files.status.uploaded", files.size()),
                true,
                progress -> model.pushToSelectedDeviceDirectory(files, currentDirectory, progress));
    }

    private void downloadSelectedFiles() {
        List<String> selectedPaths = view.getSelectedFilePaths();
        if (selectedPaths.isEmpty()) {
            view.showError(Messages.text("error.files.noSelection"));
            return;
        }

        File destinationDirectory = view.chooseDownloadDirectory();
        if (destinationDirectory == null) {
            return;
        }

        runFilesTransferCommand(
                Messages.text("error.files.download"),
                Messages.format("files.status.downloaded", selectedPaths.size()),
                false,
                progress -> model.pullSelectedDevicePaths(selectedPaths, destinationDirectory, progress));
    }

    private void renameSelectedFile() {
        DeviceFileEntry selectedEntry = view.getSelectedFileEntry();
        if (selectedEntry == null) {
            view.showError(Messages.text("error.files.noSelection"));
            return;
        }

        String newName = view.promptText(
                Messages.text("files.dialog.rename.title"),
                Messages.text("files.dialog.rename.message"),
                selectedEntry.name());
        if (newName == null) {
            return;
        }

        runFilesCommand(
                Messages.text("error.files.rename"),
                Messages.format("files.status.renamed", newName.trim()),
                true,
                () -> model.renameSelectedDevicePath(selectedEntry.path(), newName));
    }

    private void copySelectedFile() {
        DeviceFileEntry selectedEntry = view.getSelectedFileEntry();
        if (selectedEntry == null) {
            view.showError(Messages.text("error.files.noSelection"));
            return;
        }

        String copyName = view.promptText(
                Messages.text("files.dialog.copy.title"),
                Messages.text("files.dialog.copy.message"),
                buildCopyNameSuggestion(selectedEntry));
        if (copyName == null) {
            return;
        }

        String parentPath = parentDirectoryOf(selectedEntry.path());
        String destinationPath = joinRemotePath(parentPath, copyName);
        runFilesCommand(
                Messages.text("error.files.copy"),
                Messages.format("files.status.copied", copyName.trim()),
                true,
                () -> model.copySelectedDevicePath(selectedEntry.path(), destinationPath));
    }

    private void deleteSelectedFiles() {
        List<DeviceFileEntry> selectedEntries = view.getSelectedFileEntries();
        if (selectedEntries.isEmpty()) {
            view.showError(Messages.text("error.files.noSelection"));
            return;
        }

        String confirmationMessage = selectedEntries.size() == 1
                ? Messages.format("files.confirm.delete.single", selectedEntries.get(0).name())
                : Messages.format("files.confirm.delete.multiple", selectedEntries.size());
        if (!view.confirmAction(Messages.text("files.confirm.delete.title"), confirmationMessage)) {
            return;
        }

        List<String> selectedPaths = selectedEntries.stream()
                .map(DeviceFileEntry::path)
                .filter(Objects::nonNull)
                .toList();

        runFilesCommand(
                Messages.text("error.files.delete"),
                Messages.format("files.status.deleted", selectedEntries.size()),
                true,
                () -> {
                    for (String path : selectedPaths) {
                        model.deleteSelectedDevicePath(path);
                    }
                });
    }

    private void runFilesCommand(
            String errorMessage,
            String successMessage,
            boolean refreshAfter,
            ApplicationTask task) {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isFilesAvailable(selectedDevice)) {
            view.showError(Messages.text("error.files.deviceRequired"));
            return;
        }

        String requestedSerial = selectedDevice.serial();
        String currentDirectory = normalizePath(view.getCurrentFilesDirectory());
        applyingFileAction = true;
        view.setFilesStatus(Messages.text("files.status.executing"), false);
        updateFilesBusyState();

        new SwingWorker<DeviceDirectoryListing, Void>() {
            @Override
            protected DeviceDirectoryListing doInBackground() throws Exception {
                task.run();
                if (refreshAfter) {
                    return model.listSelectedDeviceDirectory(currentDirectory);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    DeviceDirectoryListing listing = get();
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return;
                    }

                    if (refreshAfter && listing != null) {
                        filesLoadedSerial = requestedSerial;
                        currentFilesDirectory = listing.currentPath();
                        view.setFilesListing(listing);
                    }
                    view.setFilesStatus(successMessage, false);
                } catch (Exception e) {
                    if (Objects.equals(requestedSerial, currentSelectedSerial)) {
                        view.setFilesStatus(extractErrorMessage(e, errorMessage), true);
                        handleError(errorMessage, e);
                    }
                } finally {
                    applyingFileAction = false;
                    updateFilesBusyState();
                }
            }
        }.execute();
    }

    private void runFilesTransferCommand(
            String errorMessage,
            String successMessage,
            boolean refreshAfter,
            FileTransferTask task) {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isFilesAvailable(selectedDevice)) {
            view.showError(Messages.text("error.files.deviceRequired"));
            return;
        }

        String requestedSerial = selectedDevice.serial();
        String currentDirectory = normalizePath(view.getCurrentFilesDirectory());
        applyingFileAction = true;
        cancellingFileTransfer = false;
        view.setFilesStatus(Messages.text("files.status.executing"), false);
        view.setFilesTransferProgress(true, true, 0, Messages.text("files.status.executing"));
        view.setFilesTransferCancelable(true);
        updateFilesBusyState();

        filesTransferWorker = new SwingWorker<DeviceDirectoryListing, FileTransferProgress>() {
            @Override
            protected DeviceDirectoryListing doInBackground() throws Exception {
                task.run(this::publish);
                if (refreshAfter) {
                    return model.listSelectedDeviceDirectory(currentDirectory);
                }
                return null;
            }

            @Override
            protected void process(List<FileTransferProgress> chunks) {
                if (!Objects.equals(requestedSerial, currentSelectedSerial) || chunks == null || chunks.isEmpty()) {
                    return;
                }

                FileTransferProgress latestProgress = chunks.get(chunks.size() - 1);
                view.setFilesTransferProgress(
                        true,
                        latestProgress.indeterminate(),
                        latestProgress.percent(),
                        formatFilesTransferProgress(latestProgress));
            }

            @Override
            protected void done() {
                try {
                    DeviceDirectoryListing listing = get();
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return;
                    }

                    if (refreshAfter && listing != null) {
                        filesLoadedSerial = requestedSerial;
                        currentFilesDirectory = listing.currentPath();
                        view.setFilesListing(listing);
                    }
                    view.setFilesStatus(successMessage, false);
                    view.clearFilesTransferProgress();
                    view.setFilesTransferCancelable(false);
                } catch (CancellationException exception) {
                    if (Objects.equals(requestedSerial, currentSelectedSerial)) {
                        view.setFilesStatus(Messages.text("files.status.cancelled"), false);
                        view.clearFilesTransferProgress();
                        view.setFilesTransferCancelable(false);
                    }
                } catch (Exception e) {
                    if (Objects.equals(requestedSerial, currentSelectedSerial)) {
                        boolean cancelled = cancellingFileTransfer || isCancelled();
                        if (cancelled) {
                            view.setFilesStatus(Messages.text("files.status.cancelled"), false);
                        } else {
                            view.setFilesStatus(extractErrorMessage(e, errorMessage), true);
                            handleError(errorMessage, e);
                        }
                        view.clearFilesTransferProgress();
                        view.setFilesTransferCancelable(false);
                    }
                } finally {
                    filesTransferWorker = null;
                    cancellingFileTransfer = false;
                    applyingFileAction = false;
                    view.setFilesTransferCancelable(false);
                    updateFilesBusyState();
                }
            }
        };
        filesTransferWorker.execute();
    }

    private void cancelFilesTransfer() {
        if (filesTransferWorker == null || filesTransferWorker.isDone()) {
            view.setFilesTransferCancelable(false);
            return;
        }

        cancellingFileTransfer = true;
        view.setFilesTransferCancelable(false);
        view.setFilesStatus(Messages.text("files.status.canceling"), false);
        model.cancelCurrentFileTransfer();
        filesTransferWorker.cancel(true);
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

        if (isPowerActionPendingFor(selectedDevice.serial())) {
            resetApplicationEnrichmentState();
            view.setApplicationsLoading(true, Messages.text("apps.loading.afterPowerAction"));
            view.setApplicationsEnabled(false);
            view.setApplicationActionsEnabled(false);
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
        if (isPowerActionPendingFor(requestedSerial)) {
            resetApplicationEnrichmentState();
            view.setApplicationsLoading(true, Messages.text("apps.loading.afterPowerAction"));
            view.setApplicationsEnabled(false);
            view.setApplicationActionsEnabled(false);
            return;
        }
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
                        queueAllApplicationSummaries(applications);
                        startApplicationEnrichmentIfNeeded(requestedSerial);
                        String selectedPackage = view.getSelectedApplicationPackage();
                        if (selectedPackage != null && !selectedPackage.isBlank()) {
                            loadApplicationDetails(selectedPackage);
                        }
                    }
                } catch (Exception e) {
                    if (!shouldSuppressApplicationLoadError(requestedSerial, e)) {
                        handleError(Messages.text("error.apps.load"), e);
                    }
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

    private void changeApplicationBackgroundMode(String packageName, AppBackgroundMode mode) {
        runApplicationCommandAndReloadDetails(
                packageName,
                Messages.text("error.apps.backgroundMode"),
                () -> model.setSelectedDeviceApplicationBackgroundMode(packageName, mode));
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

    private void openApplicationInstallDialog() {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isApplicationsAvailable(selectedDevice)) {
            view.showError(Messages.text("error.apps.deviceRequired"));
            return;
        }

        AppInstallDialog dialog = view.getAppInstallDialog();
        if (!dialog.isBusy()) {
            dialog.clearLog();
            dialog.showStatus(Messages.text("apps.install.status.ready"), false);
        }
        dialog.open();
    }

    private void installSelectedPackages() {
        AppInstallDialog dialog = view.getAppInstallDialog();
        if (dialog.isBusy()) {
            return;
        }

        AppInstallRequest request = dialog.getInstallRequest();
        if (!request.hasInputs()) {
            dialog.showStatus(Messages.text("error.apps.install.noFiles"), true);
            return;
        }

        dialog.clearLog();
        dialog.showStatus(Messages.text("apps.install.status.installing"), false);
        dialog.setBusy(true);

        new SwingWorker<AppInstallResult, String>() {
            @Override
            protected AppInstallResult doInBackground() throws Exception {
                return model.installSelectedDevicePackages(request, this::publish);
            }

            @Override
            protected void process(List<String> chunks) {
                for (String chunk : chunks) {
                    dialog.appendLog(chunk);
                }
            }

            @Override
            protected void done() {
                try {
                    AppInstallResult result = get();
                    dialog.appendLog(Messages.format(
                            "apps.install.summary",
                            result.successCount(),
                            result.failureCount()));

                    boolean hasFailures = result.failureCount() > 0;
                    dialog.showStatus(
                            Messages.text(hasFailures
                                    ? "apps.install.status.completedWithIssues"
                                    : "apps.install.status.completed"),
                            hasFailures);

                    if (result.successCount() > 0) {
                        applicationsLoadedSerial = null;
                        scrcpyApplicationsLoadedSerial = null;
                        if (view.isAppsScreenVisible()) {
                            loadApplications();
                        } else if (view.isDisplayScreenVisible() && view.shouldLoadScrcpyApplications()) {
                            loadScrcpyApplications();
                        }
                    }
                } catch (Exception e) {
                    dialog.showStatus(extractErrorMessage(e, Messages.text("error.apps.install")), true);
                    dialog.appendLog(extractErrorMessage(e, Messages.text("error.apps.install")));
                } finally {
                    dialog.setBusy(false);
                }
            }
        }.execute();
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

    private void runApplicationCommandAndReloadDetails(
            String packageName,
            String errorMessage,
            ApplicationTask task) {
        if (packageName == null || packageName.isBlank()) {
            view.showError(Messages.text("error.apps.noSelection"));
            return;
        }

        String requestedSerial = currentSelectedSerial;
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
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return;
                    }
                    view.setApplicationsEnabled(true);
                    loadApplicationDetails(packageName);
                } catch (Exception e) {
                    handleError(errorMessage, e);
                    view.setApplicationsEnabled(true);
                    view.setApplicationActionsEnabled(view.getCurrentApplicationDetails() != null);
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

    private boolean isSystemAvailable(Device selectedDevice) {
        return selectedDevice != null && Messages.STATUS_CONNECTED.equals(selectedDevice.state());
    }

    private boolean isControlAvailable(Device selectedDevice) {
        return selectedDevice != null && Messages.STATUS_CONNECTED.equals(selectedDevice.state());
    }

    private boolean isFilesAvailable(Device selectedDevice) {
        return selectedDevice != null && Messages.STATUS_CONNECTED.equals(selectedDevice.state());
    }

    private void refreshControlState(boolean showErrors) {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isControlAvailable(selectedDevice)) {
            view.setControlDeviceAvailable(false);
            view.clearControlState();
            view.setControlStatus("", false);
            return;
        }

        String requestedSerial = selectedDevice.serial();
        if (loadingControlState || (applyingControlAction && !showErrors)) {
            return;
        }

        loadingControlState = true;
        view.setControlDeviceAvailable(true);
        view.setControlStatus(Messages.text("control.status.loading"), false);
        updateControlBusyState();

        new SwingWorker<ControlState, Void>() {
            @Override
            protected ControlState doInBackground() throws Exception {
                return model.getSelectedDeviceControlState().orElse(ControlState.empty());
            }

            @Override
            protected void done() {
                try {
                    ControlState state = get();
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return;
                    }
                    view.setControlState(state);
                    view.setControlStatus(Messages.text("control.status.ready"), false);
                } catch (Exception e) {
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return;
                    }
                    view.clearControlState();
                    view.setControlStatus(extractErrorMessage(e, Messages.text("error.control.load")), true);
                    if (showErrors) {
                        handleError(Messages.text("error.control.load"), e);
                    }
                } finally {
                    loadingControlState = false;
                    updateControlBusyState();
                }
            }
        }.execute();
    }

    private void sendQuickControlKeyEvent(String keyEvent) {
        runControlCommand(
                Messages.text("error.control.keyevent"),
                Messages.text("control.status.keyeventSent"),
                false,
                () -> model.sendSelectedDeviceKeyEvent(keyEvent));
    }

    private void sendControlTextInput() {
        String text = view.getControlTextInput();
        if (text == null || text.isBlank()) {
            view.showError(Messages.text("error.control.textRequired"));
            return;
        }

        runControlCommand(
                Messages.text("error.control.text"),
                Messages.text("control.status.textSent"),
                false,
                () -> model.sendSelectedDeviceTextInput(text));
    }

    private void applyControlBrightness() {
        int brightness = view.getControlBrightness();
        runControlCommand(
                Messages.text("error.control.brightness"),
                Messages.format("control.status.brightnessApplied", brightness),
                true,
                () -> model.setSelectedDeviceBrightness(brightness));
    }

    private void applyControlVolume() {
        int volume = view.getControlVolume();
        runControlCommand(
                Messages.text("error.control.volume"),
                Messages.format("control.status.volumeApplied", volume),
                true,
                () -> model.setSelectedDeviceMediaVolume(volume));
    }

    private void applyControlSoundMode() {
        DeviceSoundMode soundMode = view.getControlSoundMode();
        runControlCommand(
                Messages.text("error.control.soundMode"),
                Messages.format("control.status.soundModeApplied", Messages.text(soundMode.messageKey())),
                true,
                () -> model.setSelectedDeviceSoundMode(soundMode));
    }

    private void applyControlTap() {
        Integer x = view.getControlTapX();
        Integer y = view.getControlTapY();
        if (x == null || y == null) {
            view.showError(Messages.text("error.control.tap"));
            return;
        }

        runControlCommand(
                Messages.text("error.control.tap"),
                Messages.format("control.status.tapApplied", x, y),
                false,
                () -> model.tapSelectedDevice(x, y));
    }

    private void applyControlSwipe() {
        Integer x1 = view.getControlSwipeX1();
        Integer y1 = view.getControlSwipeY1();
        Integer x2 = view.getControlSwipeX2();
        Integer y2 = view.getControlSwipeY2();
        Integer durationMs = view.getControlSwipeDurationMs();
        if (x1 == null || y1 == null || x2 == null || y2 == null || durationMs == null) {
            view.showError(Messages.text("error.control.swipe"));
            return;
        }

        runControlCommand(
                Messages.text("error.control.swipe"),
                Messages.format("control.status.swipeApplied", x1, y1, x2, y2, durationMs),
                false,
                () -> model.swipeSelectedDevice(x1, y1, x2, y2, durationMs));
    }

    private void applyControlManualKeyEvent() {
        String keyEvent = view.getControlManualKeyEvent();
        if (keyEvent == null || keyEvent.isBlank()) {
            view.showError(Messages.text("error.control.keyeventRequired"));
            return;
        }

        runControlCommand(
                Messages.text("error.control.keyevent"),
                Messages.format("control.status.manualKeyeventApplied", keyEvent.trim()),
                false,
                () -> model.sendSelectedDeviceKeyEvent(keyEvent));
    }

    private void applyControlRawInputCommand() {
        String rawCommand = view.getControlRawInputCommand();
        if (rawCommand == null || rawCommand.isBlank()) {
            view.showError(Messages.text("error.control.rawInputRequired"));
            return;
        }

        runControlCommand(
                Messages.text("error.control.rawInput"),
                Messages.format("control.status.rawInputApplied", rawCommand.trim()),
                false,
                () -> model.runSelectedDeviceInputCommand(rawCommand));
    }

    private void runControlCommand(String errorMessage, String successMessage, boolean refreshAfter, ApplicationTask task) {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isControlAvailable(selectedDevice)) {
            view.showError(Messages.text("error.control.deviceRequired"));
            return;
        }

        String requestedSerial = selectedDevice.serial();
        applyingControlAction = true;
        view.setControlStatus(Messages.text("control.status.executing"), false);
        updateControlBusyState();

        new SwingWorker<ControlState, Void>() {
            @Override
            protected ControlState doInBackground() throws Exception {
                task.run();
                return refreshAfter ? model.getSelectedDeviceControlState().orElse(ControlState.empty()) : null;
            }

            @Override
            protected void done() {
                try {
                    ControlState state = get();
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return;
                    }
                    if (refreshAfter && state != null) {
                        view.setControlState(state);
                    }
                    view.setControlStatus(successMessage == null ? Messages.text("control.status.ready") : successMessage, false);
                } catch (Exception e) {
                    if (Objects.equals(requestedSerial, currentSelectedSerial)) {
                        view.setControlStatus(extractErrorMessage(e, errorMessage), true);
                    }
                } finally {
                    applyingControlAction = false;
                    updateControlBusyState();
                }
            }
        }.execute();
    }

    private void refreshSystemState(boolean showErrors) {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isSystemAvailable(selectedDevice)) {
            view.setSystemDeviceAvailable(false);
            view.clearSystemState();
            view.setSystemStatus("", false);
            return;
        }

        String requestedSerial = selectedDevice.serial();
        if (loadingSystemState || (applyingSystemAction && !showErrors)) {
            return;
        }

        loadingSystemState = true;
        view.setSystemDeviceAvailable(true);
        view.setSystemStatus(Messages.text("system.status.loading"), false);
        updateSystemBusyState();

        new SwingWorker<SystemState, Void>() {
            @Override
            protected SystemState doInBackground() throws Exception {
                return model.getSelectedDeviceSystemState().orElse(SystemState.empty());
            }

            @Override
            protected void done() {
                try {
                    SystemState state = get();
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return;
                    }

                    view.setSystemState(state);
                    view.setSystemStatus(Messages.text("system.status.ready"), false);
                } catch (Exception e) {
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return;
                    }

                    view.clearSystemState();
                    view.setSystemStatus(extractErrorMessage(e, Messages.text("error.system.load")), true);
                    if (showErrors) {
                        handleError(Messages.text("error.system.load"), e);
                    }
                } finally {
                    loadingSystemState = false;
                    updateSystemBusyState();
                }
            }
        }.execute();
    }

    private void createSystemUser() {
        String userName = view.getNewSystemUserName();
        if (userName == null || userName.isBlank()) {
            view.showError(Messages.text("error.system.userNameRequired"));
            return;
        }

        runSystemCommand(
                Messages.text("error.system.userCreate"),
                Messages.format("info.system.userCreated", userName),
                () -> model.createSelectedDeviceUser(userName));
    }

    private void switchSystemUser() {
        Integer userId = selectedSystemUserId();
        if (userId == null) {
            return;
        }

        runSystemCommand(
                Messages.text("error.system.userSwitch"),
                Messages.format("info.system.userSwitched", userId),
                () -> model.switchSelectedDeviceUser(userId));
    }

    private void deleteSystemUser() {
        Integer userId = selectedSystemUserId();
        if (userId == null) {
            return;
        }

        if (!view.confirmAction(
                Messages.text("system.confirm.deleteUser.title"),
                Messages.format("system.confirm.deleteUser.message", userId))) {
            return;
        }

        runSystemCommand(
                Messages.text("error.system.userDelete"),
                Messages.format("info.system.userDeleted", userId),
                () -> model.removeSelectedDeviceUser(userId));
    }

    private void updateFilesBusyState() {
        view.setFilesBusy(loadingDevices || loadingFiles || applyingFileAction);
    }

    private String formatFilesTransferProgress(FileTransferProgress progress) {
        if (progress == null) {
            return " ";
        }

        String transferred = InstalledApp.formatBytes(progress.transferredBytes());
        String total = progress.totalBytes() > 0L ? InstalledApp.formatBytes(progress.totalBytes()) : "?";
        String speed = progress.bytesPerSecond() > 0L
                ? InstalledApp.formatBytes(progress.bytesPerSecond()) + "/s"
                : "-";

        if (progress.indeterminate()) {
            return transferred + " · " + speed;
        }
        return progress.percent() + "% · " + transferred + " / " + total + " · " + speed;
    }

    private String normalizePath(String path) {
        if (path == null) {
            return null;
        }

        String normalized = path.trim().replace('\\', '/');
        if (normalized.isBlank()) {
            return null;
        }

        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String parentDirectoryOf(String path) {
        String normalized = normalizePath(path);
        if (normalized == null || normalized.isBlank() || "/".equals(normalized)) {
            return "/";
        }

        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        return normalized.substring(0, lastSlash);
    }

    private String joinRemotePath(String parentPath, String name) {
        String parent = normalizePath(parentPath);
        String child = name == null ? "" : name.trim();
        if (parent == null || parent.isBlank() || "/".equals(parent)) {
            return "/" + child;
        }
        return parent + "/" + child;
    }

    private String buildCopyNameSuggestion(DeviceFileEntry entry) {
        String baseName = entry == null ? "" : entry.name();
        if (baseName == null || baseName.isBlank()) {
            return Messages.text("files.copy.defaultName");
        }

        if (entry.directory()) {
            return Messages.format("files.copy.directoryName", baseName);
        }

        int lastDot = baseName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < baseName.length() - 1) {
            String prefix = baseName.substring(0, lastDot);
            String extension = baseName.substring(lastDot);
            return Messages.format("files.copy.fileName", prefix, extension);
        }

        return Messages.format("files.copy.directoryName", baseName);
    }

    private void applySystemAppLanguages() {
        boolean enabled = view.isShowAllAppLanguagesSelected();
        runSystemCommand(
                Messages.text("error.system.locales"),
                Messages.text("info.system.localesUpdated"),
                () -> model.setSelectedDeviceShowAllAppLanguages(enabled));
    }

    private void applySystemGestures() {
        boolean enabled = view.isGesturalNavigationSelected();
        runSystemCommand(
                Messages.text("error.system.gestures"),
                Messages.text("info.system.gesturesUpdated"),
                () -> model.setSelectedDeviceGesturalNavigation(enabled));
    }

    private void enableSystemKeyboard() {
        String keyboardId = selectedKeyboardId();
        if (keyboardId == null) {
            return;
        }

        boolean enabled = view.isSelectedSystemKeyboardEnabled();

        runSystemCommand(
                Messages.text(enabled ? "error.system.keyboardDisable" : "error.system.keyboardEnable"),
                Messages.format(enabled ? "info.system.keyboardDisabled" : "info.system.keyboardEnabled", keyboardId),
                () -> model.setSelectedDeviceKeyboardEnabled(keyboardId, !enabled));
    }

    private void setSystemKeyboard() {
        String keyboardId = selectedKeyboardId();
        if (keyboardId == null) {
            return;
        }

        runSystemCommand(
                Messages.text("error.system.keyboardSelect"),
                Messages.format("info.system.keyboardSelected", keyboardId),
                () -> model.setSelectedDeviceKeyboard(keyboardId));
    }

    private Integer selectedSystemUserId() {
        Integer userId = view.getSelectedSystemUserId();
        if (userId == null) {
            view.showError(Messages.text("error.system.userRequired"));
            return null;
        }
        return userId;
    }

    private String selectedKeyboardId() {
        String keyboardId = view.getSelectedKeyboardId();
        if (keyboardId == null || keyboardId.isBlank()) {
            view.showError(Messages.text("error.system.keyboardRequired"));
            return null;
        }
        return keyboardId;
    }

    private void updateSystemBusyState() {
        view.setSystemBusy(loadingDevices || loadingSystemState || applyingSystemAction);
    }

    private void updateControlBusyState() {
        view.setControlBusy(loadingDevices || loadingControlState || applyingControlAction);
    }

    private void runSystemCommand(String errorMessage, String successMessage, ApplicationTask task) {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isSystemAvailable(selectedDevice)) {
            view.showError(Messages.text("error.system.deviceRequired"));
            return;
        }

        String requestedSerial = selectedDevice.serial();
        applyingSystemAction = true;
        view.setSystemStatus(Messages.text("system.status.loading"), false);
        updateSystemBusyState();

        new SwingWorker<SystemState, Void>() {
            @Override
            protected SystemState doInBackground() throws Exception {
                task.run();
                return model.getSelectedDeviceSystemState().orElse(SystemState.empty());
            }

            @Override
            protected void done() {
                try {
                    SystemState state = get();
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return;
                    }

                    view.setSystemState(state);
                    view.setSystemStatus(Messages.text("system.status.ready"), false);
                    if (successMessage != null && !successMessage.isBlank()) {
                        view.showInfo(successMessage);
                    }
                } catch (Exception e) {
                    if (Objects.equals(requestedSerial, currentSelectedSerial)) {
                        view.setSystemStatus(extractErrorMessage(e, errorMessage), true);
                    }
                    handleError(errorMessage, e);
                } finally {
                    applyingSystemAction = false;
                    updateSystemBusyState();
                }
            }
        }.execute();
    }

    private void refreshScrcpyStatus(boolean showErrors) {
        if (loadingScrcpyStatus) {
            return;
        }

        loadingScrcpyStatus = true;
        updateScrcpyBusyState();
        view.setScrcpyFeedback(Messages.text("scrcpy.feedback.statusLoading"), false);

        new SwingWorker<ScrcpyStatus, Void>() {
            @Override
            protected ScrcpyStatus doInBackground() throws Exception {
                return scrcpyService.getStatus();
            }

            @Override
            protected void done() {
                try {
                    ScrcpyStatus status = get();
                    view.setScrcpyStatus(status);
                    view.setScrcpyFeedback(Messages.text(status.available()
                            ? "scrcpy.feedback.ready"
                            : "scrcpy.feedback.missing"), false);
                } catch (Exception e) {
                    view.setScrcpyStatus(ScrcpyStatus.missing());
                    view.setScrcpyFeedback(extractErrorMessage(e, Messages.text("error.scrcpy.status")), true);
                    if (showErrors) {
                        handleError(Messages.text("error.scrcpy.status"), e);
                    }
                } finally {
                    loadingScrcpyStatus = false;
                    updateScrcpyBusyState();
                }
            }
        }.execute();
    }

    private void prepareScrcpy() {
        if (preparingScrcpy) {
            return;
        }

        preparingScrcpy = true;
        updateScrcpyBusyState();
        view.setScrcpyFeedback(Messages.text("scrcpy.feedback.preparing"), false);

        new SwingWorker<ScrcpyStatus, Void>() {
            @Override
            protected ScrcpyStatus doInBackground() throws Exception {
                return scrcpyService.ensureAvailable();
            }

            @Override
            protected void done() {
                try {
                    ScrcpyStatus status = get();
                    view.setScrcpyStatus(status);
                    view.setScrcpyFeedback(Messages.text("scrcpy.feedback.prepared"), false);
                    if (view.usesScrcpyCameraSource()) {
                        loadScrcpyCameras(true);
                    }
                } catch (Exception e) {
                    handleError(Messages.text("error.scrcpy.prepare"), e);
                    view.setScrcpyFeedback(extractErrorMessage(e, Messages.text("error.scrcpy.prepare")), true);
                } finally {
                    preparingScrcpy = false;
                    updateScrcpyBusyState();
                }
            }
        }.execute();
    }

    private void onScrcpyStartAppToggle() {
        saveUserConfigSafely();
        if (view.shouldLoadScrcpyApplications()) {
            loadScrcpyApplications();
        }
    }

    private void onScrcpyTargetChanged() {
        saveUserConfigSafely();
        if (view.usesScrcpyCameraSource()) {
            loadScrcpyCameras(false);
        }
    }

    private void loadScrcpyApplications() {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isDisplayAvailable(selectedDevice)) {
            view.setScrcpyAvailableApps(List.of());
            return;
        }

        String requestedSerial = selectedDevice.serial();
        if (loadingScrcpyApplications || Objects.equals(scrcpyApplicationsLoadedSerial, requestedSerial)) {
            return;
        }

        loadingScrcpyApplications = true;
        updateScrcpyBusyState();
        view.setScrcpyFeedback(Messages.text("scrcpy.feedback.loadingApps"), false);

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
                    scrcpyApplicationsLoadedSerial = requestedSerial;
                    view.setScrcpyAvailableApps(applications);
                    view.setScrcpyFeedback(Messages.text("scrcpy.feedback.appsReady"), false);
                } catch (Exception e) {
                    view.setScrcpyFeedback(extractErrorMessage(e, Messages.text("error.scrcpy.apps")), true);
                } finally {
                    loadingScrcpyApplications = false;
                    updateScrcpyBusyState();
                }
            }
        }.execute();
    }

    private void loadScrcpyCameras(boolean showErrors) {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isDisplayAvailable(selectedDevice)) {
            view.setScrcpyAvailableCameras(List.of());
            return;
        }

        String requestedSerial = selectedDevice.serial();
        if (loadingScrcpyCameras || (!showErrors && Objects.equals(scrcpyCamerasLoadedSerial, requestedSerial))) {
            return;
        }

        loadingScrcpyCameras = true;
        updateScrcpyBusyState();
        view.setScrcpyFeedback(Messages.text("scrcpy.feedback.loadingCameras"), false);

        new SwingWorker<List<ScrcpyCamera>, Void>() {
            @Override
            protected List<ScrcpyCamera> doInBackground() throws Exception {
                return scrcpyService.listCameras(requestedSerial);
            }

            @Override
            protected void done() {
                try {
                    List<ScrcpyCamera> cameras = get();
                    if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                        return;
                    }
                    scrcpyCamerasLoadedSerial = requestedSerial;
                    view.setScrcpyAvailableCameras(cameras);
                    view.setScrcpyFeedback(Messages.text(cameras.isEmpty()
                            ? "scrcpy.feedback.noCameras"
                            : "scrcpy.feedback.camerasReady"), false);
                } catch (Exception e) {
                    view.setScrcpyAvailableCameras(List.of());
                    view.setScrcpyFeedback(extractErrorMessage(e, Messages.text("error.scrcpy.cameras")), true);
                    if (showErrors) {
                        handleError(Messages.text("error.scrcpy.cameras"), e);
                    }
                } finally {
                    loadingScrcpyCameras = false;
                    updateScrcpyBusyState();
                }
            }
        }.execute();
    }

    private void chooseScrcpyRecordingPath() {
        File outputFile = view.chooseScrcpyRecordingDestination();
        if (outputFile != null) {
            view.setScrcpyRecordPath(outputFile.getAbsolutePath());
            saveUserConfigSafely();
        }
    }

    private void launchScrcpy() {
        Device selectedDevice = model.getSelectedDevice().orElse(null);
        if (!isDisplayAvailable(selectedDevice)) {
            view.showError(Messages.text("error.scrcpy.deviceRequired"));
            return;
        }

        ScrcpyLaunchRequest request = view.getScrcpyLaunchRequest();
        if (request.usesVirtualDisplay() && request.hasPartialVirtualDisplaySize()) {
            view.showError(Messages.text("error.scrcpy.virtualSize"));
            return;
        }
        if (request.usesCameraSource() && request.hasPartialCameraSize()) {
            view.showError(Messages.text("error.scrcpy.cameraSize"));
            return;
        }
        if (!request.usesCameraSource() && request.startAppEnabled() && !request.hasStartApp()) {
            view.showError(Messages.text("error.scrcpy.startApp"));
            return;
        }

        if (request.recordEnabled() && !request.hasRecordPath()) {
            File outputFile = view.chooseScrcpyRecordingDestination();
            if (outputFile == null) {
                return;
            }
            view.setScrcpyRecordPath(outputFile.getAbsolutePath());
            request = view.getScrcpyLaunchRequest();
        }

        final ScrcpyLaunchRequest launchRequest = request;
        final String requestedSerial = selectedDevice.serial();
        saveUserConfigSafely();
        launchingScrcpy = true;
        updateScrcpyBusyState();
        view.setScrcpyFeedback(Messages.text("scrcpy.feedback.launching"), false);

        new SwingWorker<ScrcpyStatus, Void>() {
            @Override
            protected ScrcpyStatus doInBackground() throws Exception {
                scrcpyService.launch(requestedSerial, launchRequest);
                return scrcpyService.getStatus();
            }

            @Override
            protected void done() {
                try {
                    ScrcpyStatus status = get();
                    view.setScrcpyStatus(status);
                    view.setScrcpyFeedback(Messages.format("scrcpy.feedback.launched", requestedSerial), false);
                } catch (Exception e) {
                    handleError(Messages.text("error.scrcpy.launch"), e);
                    view.setScrcpyFeedback(extractErrorMessage(e, Messages.text("error.scrcpy.launch")), true);
                } finally {
                    launchingScrcpy = false;
                    updateScrcpyBusyState();
                }
            }
        }.execute();
    }

    private void updateScrcpyBusyState() {
        view.setScrcpyBusy(loadingScrcpyStatus
                || loadingScrcpyApplications
                || loadingScrcpyCameras
                || preparingScrcpy
                || launchingScrcpy);
    }

    private void applyDisplayOverride() {
        Integer width = view.getRequestedDisplayWidth();
        Integer height = view.getRequestedDisplayHeight();
        Integer density = view.getRequestedDisplayDensity();
        Integer timeout = view.getRequestedDisplayScreenOffTimeout();
        String timeoutLabel = view.getRequestedDisplayScreenOffTimeoutLabel();
        boolean hasTimeoutInput = view.hasRequestedDisplayScreenOffTimeout();

        if (width == null || height == null || density == null) {
            view.showError(Messages.text("error.display.invalidInput"));
            return;
        }

        if (hasTimeoutInput && timeout == null) {
            view.showError(Messages.text("error.display.invalidTimeout"));
            return;
        }

        runDisplayCommand(
                Messages.text("error.display.apply"),
                hasTimeoutInput
                        ? Messages.format("info.display.updatedWithTimeout", width + "x" + height, density, timeoutLabel)
                        : Messages.format("info.display.updated", width + "x" + height, density),
                () -> {
                    model.setSelectedDeviceDisplay(width, height, density);
                    if (hasTimeoutInput) {
                        model.setSelectedDeviceScreenOffTimeout(timeout);
                    }
                });
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
        dialog.resetSessionFields();
        dialog.setBusy(false);
        dialog.showStatus(Messages.text("wireless.status.loading"), false);
        dialog.open();
        startWirelessEndpointDiscovery();

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

        new SwingWorker<WirelessPairingResult, Void>() {
            @Override
            protected WirelessPairingResult doInBackground() throws Exception {
                return model.pairWirelessDevice(host, pairingPort, pairingCode);
            }

            @Override
            protected void done() {
                try {
                    WirelessPairingResult result = get();
                    applyWirelessPairingResult(dialog, result, Messages.text("wireless.status.paired"));
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

    private void connectSelectedUsbDeviceOverTcpip() {
        view.setTcpipEnabled(false);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return model.connectSelectedUsbDeviceOverTcpip(5555);
            }

            @Override
            protected void done() {
                try {
                    String endpoint = get();
                    view.showInfo(Messages.format("wireless.status.tcpipConnected", endpoint));
                    refreshDevices();
                } catch (Exception e) {
                    handleError(Messages.text("error.wireless.tcpip"), e);
                } finally {
                    Device selectedDevice = model.getSelectedDevice().orElse(null);
                    view.setTcpipEnabled(isTcpipAvailable(selectedDevice));
                }
            }
        }.execute();
    }

    private boolean isTcpipAvailable(Device selectedDevice) {
        if (selectedDevice == null || !Messages.STATUS_CONNECTED.equals(selectedDevice.state())) {
            return false;
        }

        String serial = selectedDevice.serial() == null ? "" : selectedDevice.serial().trim();
        return !serial.isBlank() && !serial.startsWith("emulator-") && !serial.contains(":");
    }

    private void generateWirelessQrPayload() {
        WirelessConnectionDialog dialog = view.getWirelessConnectionDialog();
        try {
            currentQrPayload = WirelessPairingQrPayload.random();
            dialog.setQrPayload(currentQrPayload, SimpleQrCodeGenerator.generate(currentQrPayload.qrPayload(), 7, 5));
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

        new SwingWorker<WirelessPairingResult, Void>() {
            @Override
            protected WirelessPairingResult doInBackground() throws Exception {
                return model.pairWirelessDeviceWithQr(currentQrPayload.serviceName(), currentQrPayload.password(), 45);
            }

            @Override
            protected void done() {
                try {
                    WirelessPairingResult result = get();
                    applyWirelessPairingResult(dialog, result, Messages.text("wireless.status.qrPaired"));
                } catch (Exception e) {
                    dialog.showStatus(extractErrorMessage(e, Messages.text("error.wireless.qrPair")), true);
                } finally {
                    dialog.setBusy(false);
                }
            }
        }.execute();
    }

    private void applyWirelessPairingResult(
            WirelessConnectionDialog dialog,
            WirelessPairingResult result,
            String connectedStatus) {
        if (result != null && result.hasConnectEndpoint()) {
            dialog.setConnectEndpoint(result.connectEndpoint().host(), result.connectEndpoint().port());
        }

        if (result != null && result.connectedAutomatically()) {
            dialog.showStatus(Messages.text("wireless.status.pairedConnected"), false);
            refreshDevices();
            return;
        }

        if (result != null && result.hasConnectEndpoint()) {
            dialog.showStatus(Messages.format(
                    "wireless.status.pairedManualConnect",
                    result.connectEndpoint().endpoint()), false);
            return;
        }

        dialog.showStatus(connectedStatus, false);
    }

    private void startWirelessEndpointDiscovery() {
        cancelWirelessEndpointDiscovery();
        WirelessConnectionDialog dialog = view.getWirelessConnectionDialog();

        wirelessEndpointDiscoveryWorker = new SwingWorker<>() {
            private WirelessEndpointDiscovery lastDiscovery = WirelessEndpointDiscovery.empty();

            @Override
            protected Void doInBackground() throws Exception {
                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
                while (!isCancelled() && dialog.isVisible() && System.nanoTime() < deadline) {
                    try {
                        WirelessEndpointDiscovery discovery = model.discoverWirelessEndpoints();
                        if (!Objects.equals(discovery, lastDiscovery)) {
                            lastDiscovery = discovery;
                            publish(discovery);
                        }
                    } catch (Exception ignored) {
                    }

                    Thread.sleep(1500L);
                }
                return null;
            }

            @Override
            protected void process(List<WirelessEndpointDiscovery> chunks) {
                if (!dialog.isVisible() || chunks == null || chunks.isEmpty()) {
                    return;
                }

                WirelessEndpointDiscovery latest = chunks.get(chunks.size() - 1);
                if (latest == null) {
                    return;
                }

                if (latest.hasPairingEndpoint()) {
                    dialog.suggestPairEndpoint(latest.pairingEndpoint().host(), latest.pairingEndpoint().port());
                }
                if (latest.hasConnectEndpoint()) {
                    dialog.suggestConnectEndpoint(latest.connectEndpoint().host(), latest.connectEndpoint().port());
                }
            }

            @Override
            protected void done() {
                if (wirelessEndpointDiscoveryWorker == this) {
                    wirelessEndpointDiscoveryWorker = null;
                }
            }
        };

        wirelessEndpointDiscoveryWorker.execute();
    }

    private void cancelWirelessEndpointDiscovery() {
        if (wirelessEndpointDiscoveryWorker != null) {
            wirelessEndpointDiscoveryWorker.cancel(true);
            wirelessEndpointDiscoveryWorker = null;
        }
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

        prioritizeApplicationSummaries(currentSelectedPackageName, view.getVisibleApplicationPackages(VISIBLE_APP_ENRICHMENT_EXTRA_ROWS));

        if (!loadingApplications) {
            startApplicationEnrichmentIfNeeded(selectedDevice.serial());
        }
    }

    private void queueAllApplicationSummaries(List<InstalledApp> applications) {
        resetApplicationEnrichmentState();
        totalApplicationPackagesToEnrich = applications == null ? 0 : applications.size();
        prioritizeApplicationSummaries(currentSelectedPackageName, view.getVisibleApplicationPackages(VISIBLE_APP_ENRICHMENT_EXTRA_ROWS));
        if (applications == null) {
            return;
        }
        for (InstalledApp application : applications) {
            if (application != null) {
                queueApplicationSummary(application.packageName());
            }
        }
    }

    private void prioritizeApplicationSummaries(String selectedPackageName, List<String> visiblePackages) {
        List<String> prioritizedPackages = new java.util.ArrayList<>();
        if (selectedPackageName != null && !selectedPackageName.isBlank()) {
            prioritizedPackages.add(selectedPackageName);
        }
        if (visiblePackages != null) {
            for (String packageName : visiblePackages) {
                if (packageName != null
                        && !packageName.isBlank()
                        && !prioritizedPackages.contains(packageName)) {
                    prioritizedPackages.add(packageName);
                }
            }
        }

        if (prioritizedPackages.isEmpty() || pendingApplicationPackages.isEmpty()) {
            for (String packageName : prioritizedPackages) {
                queueApplicationSummary(packageName);
            }
            return;
        }

        Set<String> reorderedPackages = new LinkedHashSet<>();
        for (String packageName : prioritizedPackages) {
            if (!enrichedApplicationPackages.contains(packageName)) {
                reorderedPackages.add(packageName);
            }
        }
        for (String packageName : pendingApplicationPackages) {
            if (!enrichedApplicationPackages.contains(packageName)) {
                reorderedPackages.add(packageName);
            }
        }
        pendingApplicationPackages.clear();
        pendingApplicationPackages.addAll(reorderedPackages);
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

        int initialLoadedCount = enrichedApplicationPackages.size();
        view.setApplicationsLoading(true, Messages.format(
                "apps.loading.metadata.progress",
                initialLoadedCount,
                Math.max(initialLoadedCount + packageNames.size(), totalApplicationPackagesToEnrich)));

        applicationEnrichmentWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                if (isCancelled() || !Objects.equals(requestedSerial, currentSelectedSerial)) {
                    return null;
                }

                try {
                    List<InstalledApp> applications = model.getSelectedDeviceApplicationSummaries(packageNames);
                    if (!isCancelled() && Objects.equals(requestedSerial, currentSelectedSerial)) {
                        publish(applications);
                    }
                } catch (Exception ignored) {
                }
                return null;
            }

            @Override
            protected void process(List<List<InstalledApp>> chunks) {
                if (!Objects.equals(requestedSerial, currentSelectedSerial)) {
                    return;
                }

                for (List<InstalledApp> chunk : chunks) {
                    List<InstalledApp> validApplications = chunk == null ? List.of() : chunk.stream()
                            .filter(Objects::nonNull)
                            .toList();
                    if (validApplications.isEmpty()) {
                        continue;
                    }

                    for (InstalledApp application : validApplications) {
                        enrichedApplicationPackages.add(application.packageName());
                        pendingApplicationPackages.remove(application.packageName());
                    }
                    view.updateApplications(validApplications);
                }

                view.setApplicationsLoading(true, Messages.format(
                        "apps.loading.metadata.progress",
                        enrichedApplicationPackages.size(),
                        Math.max(enrichedApplicationPackages.size() + pendingApplicationPackages.size(),
                                totalApplicationPackagesToEnrich)));
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
        totalApplicationPackagesToEnrich = 0;
    }

    private UserConfig loadUserConfig() {
        try {
            return userConfigService.load();
        } catch (IOException exception) {
            return UserConfig.defaults(AppTheme.LIGHT, Messages.getLanguage());
        }
    }

    private void saveUserConfigSafely() {
        try {
            userConfigService.save(buildUserConfig());
        } catch (IOException ignored) {
        }
    }

    private void cleanupScrcpyLogsSafely() {
        try {
            scrcpyService.cleanupExistingLaunchLogs();
        } catch (IOException ignored) {
        }
    }

    private UserConfig buildUserConfig() {
        return new UserConfig(
                view.getSelectedTheme(),
                view.getSelectedLanguage(),
                view.isAutoRefreshOnFocusSelected(),
                view.isUseCustomAdbPathSelected(),
                view.getCustomAdbPath(),
                view.getScrcpyLaunchRequest());
    }

    private record RefreshState(List<Device> devices, Device selectedDevice, DeviceDetails details) {
    }
}
