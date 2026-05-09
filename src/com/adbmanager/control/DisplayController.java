package com.adbmanager.control;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import javax.swing.SwingWorker;

import com.adbmanager.logic.ScrcpyService.ScrcpyUpdateResult;
import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.logic.model.InstalledApp;
import com.adbmanager.logic.model.ScrcpyCamera;
import com.adbmanager.logic.model.ScrcpyLaunchRequest;
import com.adbmanager.logic.model.ScrcpyStatus;
import com.adbmanager.view.Messages;
import com.adbmanager.view.swing.MainFrame;
import com.adbmanager.view.swing.SettingsPanel;

final class DisplayController {

    @FunctionalInterface
    private interface DisplayTask {
        void run() throws Exception;
    }

    private final SwingControllerContext context;
    private final BiConsumer<Device, DeviceDetails> devicePresentationUpdater;

    DisplayController(
            SwingControllerContext context,
            BiConsumer<Device, DeviceDetails> devicePresentationUpdater) {
        this.context = context;
        this.devicePresentationUpdater = devicePresentationUpdater;
    }

    void resetState() {
        state().scrcpyApplicationsLoadedSerial = null;
        state().scrcpyCamerasLoadedSerial = null;
    }

    void clearViewState() {
        view().setScrcpyAvailableApps(List.of());
        view().setScrcpyAvailableCameras(List.of());
    }

    void showScreen() {
        view().showDisplayScreen();
    }

    void showMirroringScreen() {
        view().showMirroringScreen();
        refreshVisibleState();
    }

    void refreshVisibleState() {
        refreshScrcpyStatus(false);
        if (view().shouldLoadScrcpyApplications()) {
            loadScrcpyApplications();
        }
        if (view().usesScrcpyCameraSource()) {
            loadScrcpyCameras(false);
        }
    }

    void onScrcpyStartAppToggle() {
        context.saveUserConfigSafely();
        state().scrcpyApplicationsLoadedSerial = null;
        if (view().shouldLoadScrcpyApplications()) {
            loadScrcpyApplications();
        }
    }

    void onScrcpyTargetChanged() {
        context.saveUserConfigSafely();
        state().scrcpyCamerasLoadedSerial = null;
        if (view().usesScrcpyCameraSource()) {
            loadScrcpyCameras(false);
        }
    }

    void reloadApplicationsIfVisible() {
        state().scrcpyApplicationsLoadedSerial = null;
        if (view().isMirroringScreenVisible() && view().shouldLoadScrcpyApplications()) {
            loadScrcpyApplications();
        }
    }

    void prepareScrcpy() {
        if (state().preparingScrcpy) {
            return;
        }

        state().preparingScrcpy = true;
        context.updateScrcpyBusyState();
        view().setScrcpyUpdateIndicatorState(SettingsPanel.ScrcpyUpdateIndicatorState.LOADING);
        view().setScrcpyFeedback(Messages.text("scrcpy.feedback.checkingUpdates"), false);

        new SwingWorker<ScrcpyUpdateResult, Void>() {
            @Override
            protected ScrcpyUpdateResult doInBackground() throws Exception {
                return context.scrcpyService.installOrUpdate();
            }

            @Override
            protected void done() {
                try {
                    ScrcpyUpdateResult result = get();
                    view().setScrcpyStatus(result.status());
                    if (result.alreadyLatest()) {
                        view().setScrcpyUpdateIndicatorState(SettingsPanel.ScrcpyUpdateIndicatorState.SUCCESS);
                        view().setScrcpyFeedback(Messages.format(
                                "scrcpy.feedback.latest",
                                result.latestVersion().isBlank() ? result.status().version() : result.latestVersion()), false);
                    } else if (result.updated()) {
                        view().setScrcpyUpdateIndicatorState(SettingsPanel.ScrcpyUpdateIndicatorState.SUCCESS);
                        view().setScrcpyFeedback(Messages.format(
                                "scrcpy.feedback.updated",
                                result.status().version()), false);
                    } else {
                        view().setScrcpyUpdateIndicatorState(SettingsPanel.ScrcpyUpdateIndicatorState.SUCCESS);
                        view().setScrcpyFeedback(Messages.text("scrcpy.feedback.prepared"), false);
                    }
                    if (view().usesScrcpyCameraSource()) {
                        loadScrcpyCameras(true);
                    }
                } catch (Exception exception) {
                    view().setScrcpyUpdateIndicatorState(SettingsPanel.ScrcpyUpdateIndicatorState.ERROR);
                    context.handleError(Messages.text("error.scrcpy.prepare"), exception);
                    view().setScrcpyFeedback(
                            context.extractErrorMessage(exception, Messages.text("error.scrcpy.prepare")),
                            true);
                } finally {
                    DisplayController.this.state().preparingScrcpy = false;
                    context.updateScrcpyBusyState();
                }
            }
        }.execute();
    }

    void refreshScrcpyCameras(boolean showErrors) {
        loadScrcpyCameras(showErrors);
    }

    void chooseScrcpyRecordingPath() {
        File outputFile = view().chooseScrcpyRecordingDestination();
        if (outputFile != null) {
            view().setScrcpyRecordPath(outputFile.getAbsolutePath());
            context.saveUserConfigSafely();
        }
    }

    void launchScrcpy() {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isDisplayAvailable(selectedDevice)) {
            view().showError(Messages.text("error.scrcpy.deviceRequired"));
            return;
        }

        ScrcpyLaunchRequest request = view().getScrcpyLaunchRequest();
        if (request.usesVirtualDisplay() && request.hasPartialVirtualDisplaySize()) {
            view().showError(Messages.text("error.scrcpy.virtualSize"));
            return;
        }
        if (request.usesCameraSource() && request.hasPartialCameraSize()) {
            view().showError(Messages.text("error.scrcpy.cameraSize"));
            return;
        }
        if (!request.usesCameraSource() && request.startAppEnabled() && !request.hasStartApp()) {
            view().showError(Messages.text("error.scrcpy.startApp"));
            return;
        }

        if (request.recordEnabled() && !request.hasRecordPath()) {
            File outputFile = view().chooseScrcpyRecordingDestination();
            if (outputFile == null) {
                return;
            }
            view().setScrcpyRecordPath(outputFile.getAbsolutePath());
            request = view().getScrcpyLaunchRequest();
        }

        ScrcpyLaunchRequest launchRequest = request;
        String requestedSerial = selectedDevice.serial();
        context.saveUserConfigSafely();
        state().launchingScrcpy = true;
        context.updateScrcpyBusyState();
        view().setScrcpyFeedback(Messages.text("scrcpy.feedback.launching"), false);

        new SwingWorker<ScrcpyStatus, Void>() {
            @Override
            protected ScrcpyStatus doInBackground() throws Exception {
                context.scrcpyService.launch(requestedSerial, launchRequest);
                return context.scrcpyService.getStatus();
            }

            @Override
            protected void done() {
                try {
                    ScrcpyStatus status = get();
                    view().setScrcpyStatus(status);
                    view().setScrcpyFeedback(Messages.format("scrcpy.feedback.launched", requestedSerial), false);
                } catch (Exception exception) {
                    context.handleError(Messages.text("error.scrcpy.launch"), exception);
                    view().setScrcpyFeedback(
                            context.extractErrorMessage(exception, Messages.text("error.scrcpy.launch")),
                            true);
                } finally {
                    DisplayController.this.state().launchingScrcpy = false;
                    context.updateScrcpyBusyState();
                }
            }
        }.execute();
    }

    void applyDisplayOverride() {
        Integer width = view().getRequestedDisplayWidth();
        Integer height = view().getRequestedDisplayHeight();
        Integer density = view().getRequestedDisplayDensity();
        Integer timeout = view().getRequestedDisplayScreenOffTimeout();
        String timeoutLabel = view().getRequestedDisplayScreenOffTimeoutLabel();
        boolean hasTimeoutInput = view().hasRequestedDisplayScreenOffTimeout();

        if (width == null || height == null || density == null) {
            view().showError(Messages.text("error.display.invalidInput"));
            return;
        }

        if (hasTimeoutInput && timeout == null) {
            view().showError(Messages.text("error.display.invalidTimeout"));
            return;
        }

        runDisplayCommand(
                Messages.text("error.display.apply"),
                hasTimeoutInput
                        ? Messages.format("info.display.updatedWithTimeout", width + "x" + height, density, timeoutLabel)
                        : Messages.format("info.display.updated", width + "x" + height, density),
                () -> {
                    model().setSelectedDeviceDisplay(width, height, density);
                    if (hasTimeoutInput) {
                        model().setSelectedDeviceScreenOffTimeout(timeout);
                    }
                });
    }

    void resetDisplayOverride() {
        runDisplayCommand(
                Messages.text("error.display.reset"),
                Messages.text("info.display.reset"),
                model()::resetSelectedDeviceDisplay);
    }

    void toggleDeviceDarkMode() {
        boolean enabled = view().isDeviceDarkModeSelected();
        runDisplayCommand(
                Messages.text("error.display.darkMode"),
                null,
                () -> model().setSelectedDeviceDarkMode(enabled));
    }

    private void refreshScrcpyStatus(boolean showErrors) {
        if (state().loadingScrcpyStatus) {
            return;
        }

        state().loadingScrcpyStatus = true;
        context.updateScrcpyBusyState();
        view().setScrcpyFeedback(Messages.text("scrcpy.feedback.statusLoading"), false);

        new SwingWorker<ScrcpyStatus, Void>() {
            @Override
            protected ScrcpyStatus doInBackground() throws Exception {
                return context.scrcpyService.getStatus();
            }

            @Override
            protected void done() {
                try {
                    ScrcpyStatus status = get();
                    view().setScrcpyStatus(status);
                    view().setScrcpyFeedback(Messages.text(status.available()
                            ? "scrcpy.feedback.ready"
                            : "scrcpy.feedback.missing"), false);
                } catch (Exception exception) {
                    view().setScrcpyStatus(ScrcpyStatus.missing());
                    view().setScrcpyFeedback(
                            context.extractErrorMessage(exception, Messages.text("error.scrcpy.status")),
                            true);
                    if (showErrors) {
                        context.handleError(Messages.text("error.scrcpy.status"), exception);
                    }
                } finally {
                    DisplayController.this.state().loadingScrcpyStatus = false;
                    context.updateScrcpyBusyState();
                }
            }
        }.execute();
    }

    private void loadScrcpyApplications() {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isDisplayAvailable(selectedDevice)) {
            state().scrcpyApplicationsLoadedSerial = null;
            view().setScrcpyAvailableApps(List.of());
            return;
        }

        String requestedSerial = selectedDevice.serial();
        if (state().loadingScrcpyApplications
                || Objects.equals(state().scrcpyApplicationsLoadedSerial, requestedSerial)) {
            return;
        }

        state().loadingScrcpyApplications = true;
        context.updateScrcpyBusyState();
        view().setScrcpyFeedback(Messages.text("scrcpy.feedback.loadingApps"), false);

        new SwingWorker<List<InstalledApp>, Void>() {
            @Override
            protected List<InstalledApp> doInBackground() throws Exception {
                return model().getSelectedDeviceApplications();
            }

            @Override
            protected void done() {
                try {
                    List<InstalledApp> applications = get();
                    if (!Objects.equals(requestedSerial, DisplayController.this.state().currentSelectedSerial)) {
                        return;
                    }
                    DisplayController.this.state().scrcpyApplicationsLoadedSerial = requestedSerial;
                    view().setScrcpyAvailableApps(applications);
                    view().setScrcpyFeedback(Messages.text("scrcpy.feedback.appsReady"), false);
                } catch (Exception exception) {
                    view().setScrcpyFeedback(
                            context.extractErrorMessage(exception, Messages.text("error.scrcpy.apps")),
                            true);
                } finally {
                    DisplayController.this.state().loadingScrcpyApplications = false;
                    context.updateScrcpyBusyState();
                }
            }
        }.execute();
    }

    private void loadScrcpyCameras(boolean showErrors) {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isDisplayAvailable(selectedDevice)) {
            state().scrcpyCamerasLoadedSerial = null;
            view().setScrcpyAvailableCameras(List.of());
            return;
        }

        String requestedSerial = selectedDevice.serial();
        if (state().loadingScrcpyCameras
                || (!showErrors && Objects.equals(state().scrcpyCamerasLoadedSerial, requestedSerial))) {
            return;
        }

        state().loadingScrcpyCameras = true;
        context.updateScrcpyBusyState();
        view().setScrcpyFeedback(Messages.text("scrcpy.feedback.loadingCameras"), false);

        new SwingWorker<List<ScrcpyCamera>, Void>() {
            @Override
            protected List<ScrcpyCamera> doInBackground() throws Exception {
                return context.scrcpyService.listCameras(requestedSerial);
            }

            @Override
            protected void done() {
                try {
                    List<ScrcpyCamera> cameras = get();
                    if (!Objects.equals(requestedSerial, DisplayController.this.state().currentSelectedSerial)) {
                        return;
                    }
                    DisplayController.this.state().scrcpyCamerasLoadedSerial = requestedSerial;
                    view().setScrcpyAvailableCameras(cameras);
                    view().setScrcpyFeedback(Messages.text(cameras.isEmpty()
                            ? "scrcpy.feedback.noCameras"
                            : "scrcpy.feedback.camerasReady"), false);
                } catch (Exception exception) {
                    view().setScrcpyAvailableCameras(List.of());
                    view().setScrcpyFeedback(
                            context.extractErrorMessage(exception, Messages.text("error.scrcpy.cameras")),
                            true);
                    if (showErrors) {
                        context.handleError(Messages.text("error.scrcpy.cameras"), exception);
                    }
                } finally {
                    DisplayController.this.state().loadingScrcpyCameras = false;
                    context.updateScrcpyBusyState();
                }
            }
        }.execute();
    }

    private void runDisplayCommand(String errorMessage, String successMessage, DisplayTask task) {
        view().setDisplayControlsEnabled(false);

        new SwingWorker<DeviceDetails, Void>() {
            @Override
            protected DeviceDetails doInBackground() throws Exception {
                task.run();
                return model().getSelectedDeviceDetails().orElse(null);
            }

            @Override
            protected void done() {
                try {
                    DeviceDetails details = get();
                    Device selectedDevice = model().getSelectedDevice().orElse(null);
                    devicePresentationUpdater.accept(selectedDevice, details);
                    if (successMessage != null && !successMessage.isBlank()) {
                        view().showInfo(successMessage);
                    }
                } catch (Exception exception) {
                    context.handleError(errorMessage, exception);
                    Device selectedDevice = model().getSelectedDevice().orElse(null);
                    view().setDisplayControlsEnabled(context.isDisplayAvailable(selectedDevice));
                }
            }
        }.execute();
    }

    private SwingControllerState state() {
        return context.state;
    }

    private MainFrame view() {
        return context.view;
    }

    private com.adbmanager.logic.AdbModel model() {
        return context.model;
    }
}
