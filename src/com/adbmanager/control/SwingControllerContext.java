package com.adbmanager.control;

import java.io.IOException;
import java.util.Locale;

import com.adbmanager.logic.AdbModel;
import com.adbmanager.logic.ScrcpyService;
import com.adbmanager.logic.UserConfigService;
import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.UserConfig;
import com.adbmanager.view.Messages;
import com.adbmanager.view.swing.AppTheme;
import com.adbmanager.view.swing.MainFrame;

final class SwingControllerContext {

    final AdbModel model;
    final ScrcpyService scrcpyService;
    final UserConfigService userConfigService;
    final MainFrame view;
    final SwingControllerState state = new SwingControllerState();

    SwingControllerContext(AdbModel model, ScrcpyService scrcpyService, MainFrame view) {
        this.model = model;
        this.scrcpyService = scrcpyService;
        this.userConfigService = new UserConfigService();
        this.view = view;
    }

    boolean isCaptureAvailable(Device selectedDevice) {
        return selectedDevice != null && Messages.STATUS_CONNECTED.equals(selectedDevice.state());
    }

    boolean isApplicationsAvailable(Device selectedDevice) {
        return selectedDevice != null && Messages.STATUS_CONNECTED.equals(selectedDevice.state());
    }

    boolean isDisplayAvailable(Device selectedDevice) {
        return selectedDevice != null && Messages.STATUS_CONNECTED.equals(selectedDevice.state());
    }

    boolean isSystemAvailable(Device selectedDevice) {
        return selectedDevice != null && Messages.STATUS_CONNECTED.equals(selectedDevice.state());
    }

    boolean isControlAvailable(Device selectedDevice) {
        return selectedDevice != null && Messages.STATUS_CONNECTED.equals(selectedDevice.state());
    }

    boolean isFilesAvailable(Device selectedDevice) {
        return selectedDevice != null && Messages.STATUS_CONNECTED.equals(selectedDevice.state());
    }

    boolean isTcpipAvailable(Device selectedDevice) {
        return selectedDevice != null && Messages.STATUS_CONNECTED.equals(selectedDevice.state());
    }

    void updateSystemBusyState() {
        view.setSystemBusy(state.loadingDevices || state.loadingSystemState || state.applyingSystemAction);
    }

    void updateControlBusyState() {
        view.setControlBusy(state.loadingDevices || state.loadingControlState || state.applyingControlAction);
    }

    void updateFilesBusyState() {
        view.setFilesBusy(state.loadingDevices || state.loadingFiles || state.applyingFileAction);
    }

    void updateScrcpyBusyState() {
        view.setScrcpyBusy(
                state.loadingScrcpyStatus
                        || state.loadingScrcpyApplications
                        || state.loadingScrcpyCameras
                        || state.preparingScrcpy
                        || state.launchingScrcpy);
    }

    void handleError(String message, Exception exception) {
        String detail = extractErrorMessage(exception, null);
        if (detail == null || detail.isBlank()) {
            view.showError(message);
        } else {
            view.showError(message + "\n\n" + detail);
        }
    }

    String extractErrorMessage(Exception exception, String fallback) {
        String detail = exception == null ? null : exception.getMessage();
        if ((detail == null || detail.isBlank()) && exception != null && exception.getCause() != null) {
            detail = exception.getCause().getMessage();
        }
        if ((detail == null || detail.isBlank()) && fallback != null) {
            detail = fallback;
        }
        return detail;
    }

    void markPowerActionPending(String serial) {
        if (serial == null || serial.isBlank()) {
            return;
        }

        state.pendingPowerActionSerial = serial;
        state.pendingPowerActionUntilMs = System.currentTimeMillis() + 25000L;
    }

    void clearPowerActionPendingIfExpired() {
        if (state.pendingPowerActionSerial == null) {
            return;
        }

        if (System.currentTimeMillis() > state.pendingPowerActionUntilMs) {
            state.pendingPowerActionSerial = null;
            state.pendingPowerActionUntilMs = 0L;
        }
    }

    boolean isPowerActionPendingFor(String serial) {
        clearPowerActionPendingIfExpired();
        if (serial == null || serial.isBlank()) {
            return false;
        }

        return serial.equals(state.pendingPowerActionSerial)
                && System.currentTimeMillis() <= state.pendingPowerActionUntilMs;
    }

    boolean shouldSuppressApplicationLoadError(String serial, Exception exception) {
        if (!isPowerActionPendingFor(serial)) {
            return false;
        }

        String detail = extractErrorMessage(exception, "");
        String normalized = detail == null ? "" : detail.toLowerCase(Locale.ROOT);
        return normalized.contains("device offline")
                || normalized.contains("closed")
                || normalized.contains("disconnected")
                || normalized.contains("transport")
                || normalized.contains("not found")
                || normalized.contains("no devices/emulators")
                || normalized.contains("timed out")
                || normalized.contains("timeout");
    }

    UserConfig loadUserConfig() {
        try {
            return userConfigService.load();
        } catch (IOException exception) {
            return UserConfig.defaults(AppTheme.LIGHT, Messages.getLanguage());
        }
    }

    void saveUserConfigSafely() {
        try {
            userConfigService.save(buildUserConfig());
        } catch (IOException ignored) {
        }
    }

    void cleanupScrcpyLogsSafely() {
        try {
            scrcpyService.cleanupExistingLaunchLogs();
        } catch (IOException ignored) {
        }
    }

    UserConfig buildUserConfig() {
        return new UserConfig(
                view.getSelectedTheme(),
                view.getSelectedLanguage(),
                view.isAutoRefreshOnFocusSelected(),
                view.isUseCustomAdbPathSelected(),
                view.getCustomAdbPath(),
                view.getScrcpyLaunchRequest());
    }
}
