package com.adbmanager.control;

import java.awt.Desktop;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import com.adbmanager.logic.AdbModel;
import com.adbmanager.logic.ScrcpyService;
import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.logic.model.DevicePowerAction;
import com.adbmanager.logic.model.UserConfig;
import com.adbmanager.view.Messages;
import com.adbmanager.view.Messages.Language;
import com.adbmanager.view.swing.MainFrame;
import com.adbmanager.view.swing.WirelessConnectionDialog;

public class SwingController {

    private final SwingControllerContext context;
    private final ApplicationsController applicationsController;
    private final FilesController filesController;
    private final DisplayController displayController;
    private final ControlController controlController;
    private final SystemController systemController;
    private final WirelessController wirelessController;

    public SwingController(AdbModel model, ScrcpyService scrcpyService, MainFrame view) {
        this.context = new SwingControllerContext(model, scrcpyService, view);
        this.displayController = new DisplayController(context, this::updateDevicePresentation);
        this.applicationsController = new ApplicationsController(context, displayController::reloadApplicationsIfVisible);
        this.filesController = new FilesController(context);
        this.controlController = new ControlController(context);
        this.systemController = new SystemController(context);
        this.wirelessController = new WirelessController(context, this::refreshDevices);
    }

    public void start() {
        UserConfig userConfig = context.loadUserConfig();
        Messages.setLanguage(userConfig.language());
        view().setSelectedLanguage(userConfig.language());
        view().setLanguage(userConfig.language());
        view().setSelectedTheme(userConfig.theme());
        view().setTheme(userConfig.theme());
        state().autoRefreshOnFocus = userConfig.autoRefreshOnFocus();
        view().setAutoRefreshOnFocusSelected(state().autoRefreshOnFocus);
        view().setUseCustomAdbPathSelected(userConfig.useCustomAdbPath());
        view().setCustomAdbPath(userConfig.customAdbPath());
        view().setScrcpyLaunchRequest(userConfig.scrcpyLaunchRequest());
        context.cleanupScrcpyLogsSafely();

        bindEvents();
        applicationsController.clearViewState();
        controlController.clearViewState();
        systemController.clearViewState();
        filesController.clearViewState();
        displayController.clearViewState();
        view().setScrcpyDeviceAvailable(false);
        view().setSystemDeviceAvailable(false);
        view().setControlDeviceAvailable(false);
        view().setFilesDeviceAvailable(false);
        view().setPowerActionsEnabled(false);
        view().setTcpipEnabled(false);
        view().setSystemBusy(false);
        view().setControlBusy(false);
        view().setFilesBusy(false);
        view().setApplicationsEnabled(false);
        view().setApplicationActionsEnabled(false);
        view().setSystemStatus("", false);
        view().setControlStatus("", false);
        view().setFilesStatus("", false);
        view().setFilesTransferCancelable(false);
        view().clearDeviceDetails();
        view().clearScreenshot();
        view().showHomeScreen();
        view().showWindow();
        context.saveUserConfigSafely();
        refreshDevices();
    }

    private void bindEvents() {
        WirelessConnectionDialog wirelessDialog = view().getWirelessConnectionDialog();

        view().setDeviceSelectionAction(event -> onDeviceSelected());
        view().setCaptureAction(event -> captureScreenshot());
        view().setSaveCaptureAction(event -> saveScreenshot());
        view().setRefreshAction(event -> refreshDevices());
        view().setWirelessAssistantAction(event -> wirelessController.openAssistant());
        view().setTcpipAction(event -> wirelessController.connectSelectedUsbDeviceOverTcpip());
        view().setPowerAction(event -> executePowerAction(DevicePowerAction.fromActionCommand(event.getActionCommand())));
        view().setHomeAction(event -> view().showHomeScreen());
        view().setDisplayAction(event -> displayController.showScreen());
        view().setControlAction(event -> showControlScreen());
        view().setApplyDisplayAction(event -> displayController.applyDisplayOverride());
        view().setResetDisplayAction(event -> displayController.resetDisplayOverride());
        view().setDeviceDarkModeAction(event -> displayController.toggleDeviceDarkMode());
        view().setQuickControlKeyEventAction(event -> controlController.sendQuickControlKeyEvent(event.getActionCommand()));
        view().setControlTextAction(event -> controlController.sendControlTextInput());
        view().setControlBrightnessAction(event -> controlController.applyBrightness());
        view().setControlVolumeAction(event -> controlController.applyVolume());
        view().setControlSoundModeAction(event -> controlController.applySoundMode());
        view().setControlTapAction(event -> controlController.applyTap());
        view().setControlSwipeAction(event -> controlController.applySwipe());
        view().setControlKeyEventAction(event -> controlController.applyManualKeyEvent());
        view().setControlRawInputAction(event -> controlController.applyRawInputCommand());
        view().setPrepareScrcpyAction(event -> displayController.prepareScrcpy());
        view().setLaunchScrcpyAction(event -> displayController.launchScrcpy());
        view().setBrowseScrcpyRecordPathAction(event -> displayController.chooseScrcpyRecordingPath());
        view().setRefreshScrcpyCamerasAction(event -> displayController.refreshScrcpyCameras(true));
        view().setScrcpyLaunchTargetChangeAction(event -> displayController.onScrcpyTargetChanged());
        view().setScrcpyStartAppToggleAction(event -> displayController.onScrcpyStartAppToggle());
        view().setAppsAction(event -> showAppsScreen());
        view().setFilesAction(event -> showFilesScreen());
        view().setSystemAction(event -> showSystemScreen());
        view().setSettingsAction(event -> view().showSettingsScreen());
        view().setThemeChangeAction(event -> applyThemeSelection());
        view().setLanguageChangeAction(event -> applyLanguageSelection());
        view().setAutoRefreshOnFocusChangeAction(event -> {
            state().autoRefreshOnFocus = view().isAutoRefreshOnFocusSelected();
            context.saveUserConfigSafely();
        });
        view().setUseCustomAdbPathChangeAction(event -> applyAdbPathSettings());
        view().setCustomAdbPathCommitAction(event -> applyAdbPathSettings());
        view().setCustomAdbPathBrowseAction(event -> browseCustomAdbPath());
        view().setRepositoryAction(event -> openRepository());
        view().setScrcpyRepositoryAction(event -> openUrl("https://github.com/Genymobile/scrcpy"));
        view().setDeviceCatalogAction(event -> openUrl("https://github.com/pbakondy/android-device-list"));
        view().setApplicationSelectionAction(applicationsController::onApplicationSelected);
        view().setApplicationPermissionToggleHandler(applicationsController::toggleApplicationPermission);
        view().setApplicationBackgroundModeChangeHandler(applicationsController::changeApplicationBackgroundMode);
        view().setOpenApplicationAction(event -> applicationsController.openSelectedApplication());
        view().setStopApplicationAction(event -> applicationsController.stopSelectedApplication());
        view().setUninstallApplicationAction(event -> applicationsController.uninstallSelectedApplication());
        view().setToggleApplicationEnabledAction(event -> applicationsController.toggleSelectedApplicationEnabled());
        view().setClearApplicationDataAction(event -> applicationsController.clearSelectedApplicationData());
        view().setClearApplicationCacheAction(event -> applicationsController.clearSelectedApplicationCache());
        view().setExportApplicationApkAction(event -> applicationsController.exportSelectedApplicationApk());
        view().setInstallApplicationsAction(event -> applicationsController.openApplicationInstallDialog());
        view().setFilesNavigateUpAction(event -> filesController.navigateToParentDirectory());
        view().setFilesRefreshAction(event -> filesController.refreshDirectory(true, true, view().getCurrentFilesDirectory()));
        view().setFilesPathSubmitAction(event -> filesController.refreshDirectory(true, true, view().getEnteredFilesDirectory()));
        view().setFilesTransferCancelAction(event -> filesController.cancelTransfer());
        view().setFilesCreateFolderAction(event -> filesController.createDirectory());
        view().setFilesUploadAction(event -> filesController.uploadFilesToCurrentDirectory());
        view().setFilesDownloadAction(event -> filesController.downloadSelectedFiles());
        view().setFilesRenameAction(event -> filesController.renameSelectedFile());
        view().setFilesCopyAction(event -> filesController.copySelectedFile());
        view().setFilesDeleteAction(event -> filesController.deleteSelectedFiles());
        view().setFilesOpenDirectoryAction(filesController::openSelectedDirectory);
        view().setFilesDropHandler(filesController::uploadDroppedFiles);
        view().setRefreshSystemUsersAction(event -> systemController.refreshState(true));
        view().setCreateSystemUserAction(event -> systemController.createUser());
        view().setSwitchSystemUserAction(event -> systemController.switchUser());
        view().setDeleteSystemUserAction(event -> systemController.deleteUser());
        view().setApplySystemAppLanguagesAction(event -> systemController.applyAppLanguages());
        view().setApplySystemGesturesAction(event -> systemController.applyGestures());
        view().setRefreshSystemKeyboardsAction(event -> systemController.refreshState(true));
        view().setEnableSystemKeyboardAction(event -> systemController.enableKeyboard());
        view().setSetSystemKeyboardAction(event -> systemController.setKeyboard());
        wirelessDialog.setConnectAction(event -> wirelessController.connectWirelessDevice());
        wirelessDialog.setPairCodeAction(event -> wirelessController.pairDeviceByCode());
        wirelessDialog.setGenerateQrAction(event -> wirelessController.generateQrPayload());
        wirelessDialog.setPairQrAction(event -> wirelessController.pairDeviceByQr());
        view().getAppInstallDialog().setInstallAction(event -> applicationsController.installSelectedPackages());
        view().setApplicationsViewportChangeAction(applicationsController::refreshVisibleApplicationSummaries);
        view().addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent event) {
                if (SwingController.this.state().autoRefreshOnFocus) {
                    refreshDevices();
                }
            }
        });
        view().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                wirelessController.cancelEndpointDiscovery();
                context.saveUserConfigSafely();
            }
        });
    }

    private void refreshDevices() {
        context.clearPowerActionPendingIfExpired();
        if (state().loadingDevices) {
            return;
        }

        state().loadingDevices = true;
        context.updateSystemBusyState();
        context.updateControlBusyState();
        context.updateFilesBusyState();
        view().setDeviceSelectorEnabled(false);
        view().setCaptureEnabled(false);
        view().setRefreshEnabled(false);
        view().setPowerActionsEnabled(false);
        view().setApplicationsEnabled(false);
        view().setApplicationActionsEnabled(false);
        view().setDisplayControlsEnabled(false);

        new SwingWorker<RefreshState, Void>() {
            @Override
            protected RefreshState doInBackground() throws Exception {
                model().refreshDevices();
                List<Device> devices = model().getDevices();
                Device selectedDevice = ensureSelectedDevice(devices);
                Optional<DeviceDetails> details = model().getSelectedDeviceDetails();
                return new RefreshState(devices, selectedDevice, details.orElse(null));
            }

            @Override
            protected void done() {
                try {
                    applyRefreshState(get());
                } catch (Exception exception) {
                    handleRefreshFailure(exception);
                } finally {
                    SwingController.this.state().loadingDevices = false;
                    context.updateSystemBusyState();
                    context.updateControlBusyState();
                    context.updateFilesBusyState();
                    view().setDeviceSelectorEnabled(true);
                    view().setRefreshEnabled(true);
                    Device selectedDevice = model().getSelectedDevice().orElse(null);
                    view().setPowerActionsEnabled(context.isDisplayAvailable(selectedDevice));
                }
            }
        }.execute();
    }

    private Device ensureSelectedDevice(List<Device> devices) {
        Optional<Device> currentSelection = model().getSelectedDevice();
        if (currentSelection.isPresent()) {
            return currentSelection.get();
        }

        Device defaultDevice = chooseDefaultDevice(devices);
        if (defaultDevice == null) {
            return null;
        }

        return model().selectDeviceBySerial(defaultDevice.serial());
    }

    private Device chooseDefaultDevice(List<Device> devices) {
        for (Device device : devices) {
            if (Messages.STATUS_CONNECTED.equals(device.state())) {
                return device;
            }
        }

        return devices.isEmpty() ? null : devices.get(0);
    }

    private void applyRefreshState(RefreshState refreshState) {
        state().syncingDeviceSelector = true;
        try {
            String selectedSerial = refreshState.selectedDevice() == null
                    ? null
                    : refreshState.selectedDevice().serial();
            view().setDevices(refreshState.devices(), selectedSerial);
        } finally {
            state().syncingDeviceSelector = false;
        }

        updateDevicePresentation(refreshState.selectedDevice(), refreshState.details());
    }

    private void handleRefreshFailure(Exception exception) {
        context.handleError(Messages.text("error.devices.load"), exception);
        applicationsController.resetState();
        filesController.resetState();
        displayController.resetState();
        state().currentSelectedSerial = null;
        view().setDevices(List.of(), null);
        view().clearDeviceDetails();
        systemController.clearViewState();
        controlController.clearViewState();
        filesController.clearViewState();
        displayController.clearViewState();
        applicationsController.clearViewState();
        view().clearScreenshot();
        view().setScrcpyDeviceAvailable(false);
        view().setSystemDeviceAvailable(false);
        view().setControlDeviceAvailable(false);
        view().setFilesDeviceAvailable(false);
        view().setTcpipEnabled(false);
        view().setSaveCaptureEnabled(false);
    }

    private void onDeviceSelected() {
        if (state().syncingDeviceSelector || state().loadingDevices) {
            return;
        }

        String serial = view().getSelectedDeviceSerial();
        if (serial == null || serial.equals(state().currentSelectedSerial)) {
            return;
        }

        view().setDeviceSelectorEnabled(false);
        view().setCaptureEnabled(false);
        view().setDisplayControlsEnabled(false);
        view().setSystemBusy(true);

        new SwingWorker<DeviceDetails, Void>() {
            @Override
            protected DeviceDetails doInBackground() throws Exception {
                model().selectDeviceBySerial(serial);
                return model().getSelectedDeviceDetails().orElse(null);
            }

            @Override
            protected void done() {
                try {
                    DeviceDetails details = get();
                    Device selectedDevice = model().getSelectedDevice().orElse(null);
                    view().clearScreenshot();
                    updateDevicePresentation(selectedDevice, details);
                } catch (Exception exception) {
                    context.handleError(Messages.text("error.device.select"), exception);
                } finally {
                    view().setDeviceSelectorEnabled(true);
                    context.updateSystemBusyState();
                }
            }
        }.execute();
    }

    private void captureScreenshot() {
        view().setCaptureEnabled(false);

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                byte[] screenshotBytes = model().captureSelectedDeviceScreenshot();
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
                    view().setScreenshot(screenshot);
                    view().setSaveCaptureEnabled(true);
                } catch (Exception exception) {
                    context.handleError(Messages.text("error.capture"), exception);
                } finally {
                    Device selectedDevice = model().getSelectedDevice().orElse(null);
                    view().setCaptureEnabled(context.isCaptureAvailable(selectedDevice));
                }
            }
        }.execute();
    }

    private void saveScreenshot() {
        BufferedImage screenshot = view().getCurrentScreenshot();
        if (screenshot == null) {
            view().showError(Messages.text("error.save.empty"));
            return;
        }

        File outputFile = view().chooseScreenshotDestination();
        if (outputFile == null) {
            return;
        }

        try {
            ImageIO.write(screenshot, "png", outputFile);
            view().showInfo(Messages.format("info.screenshot.saved", outputFile.getAbsolutePath()));
        } catch (IOException exception) {
            context.handleError(Messages.text("error.save"), exception);
        }
    }

    private void applyThemeSelection() {
        view().setTheme(view().getSelectedTheme());
        context.saveUserConfigSafely();
    }

    private void applyLanguageSelection() {
        Language language = view().getSelectedLanguage();
        Messages.setLanguage(language);
        view().setLanguage(language);
        context.saveUserConfigSafely();
    }

    private void openRepository() {
        openUrl(Messages.repositoryUrl());
    }

    private void executePowerAction(DevicePowerAction action) {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isDisplayAvailable(selectedDevice)) {
            view().showError(Messages.text("error.power.deviceRequired"));
            return;
        }

        DevicePowerAction safeAction = action == null ? DevicePowerAction.REBOOT_ANDROID : action;
        String actionLabel = Messages.text(safeAction.messageKey());
        String deviceLabel = selectedDevice == null ? "-" : selectedDevice.serial();

        if (!view().confirmAction(
                Messages.text("power.confirm.title"),
                Messages.format("power.confirm.message", actionLabel, deviceLabel))) {
            return;
        }

        view().setPowerActionsEnabled(false);
        view().setRefreshEnabled(false);
        view().setDeviceSelectorEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                model().performSelectedDevicePowerAction(safeAction);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    context.markPowerActionPending(selectedDevice.serial());
                    view().showInfo(Messages.format("info.power.sent", actionLabel));
                    refreshDevices();
                    schedulePostPowerActionRefresh(4000);
                    schedulePostPowerActionRefresh(12000);
                } catch (Exception exception) {
                    context.handleError(Messages.text("error.power.action"), exception);
                    Device currentDevice = model().getSelectedDevice().orElse(null);
                    view().setPowerActionsEnabled(context.isDisplayAvailable(currentDevice));
                    view().setRefreshEnabled(true);
                    view().setDeviceSelectorEnabled(true);
                }
            }
        }.execute();
    }

    private void browseCustomAdbPath() {
        File selectedFile = view().chooseAdbExecutable();
        if (selectedFile == null) {
            return;
        }

        view().setCustomAdbPath(selectedFile.getAbsolutePath());
        applyAdbPathSettings();
    }

    private void applyAdbPathSettings() {
        context.saveUserConfigSafely();
        refreshDevices();
    }

    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception exception) {
            context.handleError(Messages.text("error.repository.open"), exception);
            return;
        }

        view().showInfo(url);
    }

    private void schedulePostPowerActionRefresh(int delayMs) {
        Timer timer = new Timer(delayMs, event -> {
            Timer source = (Timer) event.getSource();
            source.stop();
            if (state().pendingPowerActionSerial != null) {
                refreshDevices();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void updateDevicePresentation(Device selectedDevice, DeviceDetails details) {
        String previousSerial = state().currentSelectedSerial;
        state().currentSelectedSerial = selectedDevice == null ? null : selectedDevice.serial();
        if (state().pendingPowerActionSerial != null
                && !Objects.equals(state().pendingPowerActionSerial, state().currentSelectedSerial)) {
            state().pendingPowerActionSerial = null;
            state().pendingPowerActionUntilMs = 0L;
        }

        if (!Objects.equals(previousSerial, state().currentSelectedSerial)) {
            applicationsController.resetState();
            filesController.resetState();
            displayController.resetState();
            applicationsController.clearViewState();
            filesController.clearViewState();
            systemController.clearViewState();
            controlController.clearViewState();
            displayController.clearViewState();
        }

        if (details == null) {
            view().clearDeviceDetails();
        } else {
            view().setDeviceDetails(details);
        }

        if (selectedDevice == null) {
            view().clearScreenshot();
        }

        boolean applicationsAvailable = context.isApplicationsAvailable(selectedDevice);
        boolean displayAvailable = context.isDisplayAvailable(selectedDevice);
        boolean systemAvailable = context.isSystemAvailable(selectedDevice);
        boolean controlAvailable = context.isControlAvailable(selectedDevice);
        boolean filesAvailable = context.isFilesAvailable(selectedDevice);

        view().setCaptureEnabled(context.isCaptureAvailable(selectedDevice));
        view().setSaveCaptureEnabled(view().getCurrentScreenshot() != null);
        view().setApplicationsEnabled(applicationsAvailable);
        view().setApplicationActionsEnabled(applicationsAvailable && view().getCurrentApplicationDetails() != null);
        view().setDisplayControlsEnabled(displayAvailable);
        view().setPowerActionsEnabled(displayAvailable);
        view().setTcpipEnabled(context.isTcpipAvailable(selectedDevice));
        view().setScrcpyDeviceAvailable(displayAvailable);
        view().setSystemDeviceAvailable(systemAvailable);
        view().setControlDeviceAvailable(controlAvailable);
        view().setFilesDeviceAvailable(filesAvailable);
        context.updateFilesBusyState();

        if (!applicationsAvailable) {
            applicationsController.clearUnavailableState();
        }
        if (!displayAvailable) {
            displayController.clearViewState();
        }
        if (!systemAvailable) {
            systemController.clearViewState();
        }
        if (!controlAvailable) {
            controlController.clearViewState();
        }
        if (!filesAvailable) {
            filesController.clearUnavailableState();
        }

        if (view().isAppsScreenVisible()) {
            applicationsController.ensureApplicationsLoaded();
        }
        if (view().isFilesScreenVisible() && filesAvailable) {
            filesController.refreshDirectory(false, false, state().currentFilesDirectory);
        }
        if (view().isSystemScreenVisible() && systemAvailable) {
            systemController.refreshState(false);
        }
        if (view().isControlScreenVisible() && controlAvailable) {
            controlController.refreshState(false);
        }
        if (view().isDisplayScreenVisible()) {
            displayController.refreshVisibleState();
        }
    }

    private void showAppsScreen() {
        view().showAppsScreen();
        applicationsController.ensureApplicationsLoaded();
    }

    private void showFilesScreen() {
        view().showFilesScreen();
        filesController.refreshDirectory(false, false, state().currentFilesDirectory);
    }

    private void showSystemScreen() {
        view().showSystemScreen();
        systemController.refreshState(false);
    }

    private void showControlScreen() {
        view().showControlScreen();
        controlController.refreshState(false);
    }

    private SwingControllerState state() {
        return context.state;
    }

    private MainFrame view() {
        return context.view;
    }

    private AdbModel model() {
        return context.model;
    }

    private record RefreshState(List<Device> devices, Device selectedDevice, DeviceDetails details) {
    }
}
