package com.adbmanager.control;

import java.util.Objects;

import javax.swing.SwingWorker;

import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.SystemState;
import com.adbmanager.view.Messages;
import com.adbmanager.view.swing.MainFrame;

final class SystemController {

    @FunctionalInterface
    private interface SystemTask {
        void run() throws Exception;
    }

    private final SwingControllerContext context;

    SystemController(SwingControllerContext context) {
        this.context = context;
    }

    void clearViewState() {
        view().clearSystemState();
        view().setSystemStatus("", false);
    }

    void refreshState(boolean showErrors) {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isSystemAvailable(selectedDevice)) {
            view().setSystemDeviceAvailable(false);
            clearViewState();
            return;
        }

        String requestedSerial = selectedDevice.serial();
        if (state().loadingSystemState || (state().applyingSystemAction && !showErrors)) {
            return;
        }

        state().loadingSystemState = true;
        view().setSystemDeviceAvailable(true);
        view().setSystemStatus(Messages.text("system.status.loading"), false);
        context.updateSystemBusyState();

        new SwingWorker<SystemState, Void>() {
            @Override
            protected SystemState doInBackground() throws Exception {
                return model().getSelectedDeviceSystemState().orElse(SystemState.empty());
            }

            @Override
            protected void done() {
                try {
                    SystemState systemState = get();
                    if (!Objects.equals(requestedSerial, SystemController.this.state().currentSelectedSerial)) {
                        return;
                    }

                    view().setSystemState(systemState);
                    view().setSystemStatus(Messages.text("system.status.ready"), false);
                } catch (Exception exception) {
                    if (!Objects.equals(requestedSerial, SystemController.this.state().currentSelectedSerial)) {
                        return;
                    }

                    view().clearSystemState();
                    view().setSystemStatus(
                            context.extractErrorMessage(exception, Messages.text("error.system.load")),
                            true);
                    if (showErrors) {
                        context.handleError(Messages.text("error.system.load"), exception);
                    }
                } finally {
                    SystemController.this.state().loadingSystemState = false;
                    context.updateSystemBusyState();
                }
            }
        }.execute();
    }

    void createUser() {
        String userName = view().getNewSystemUserName();
        if (userName == null || userName.isBlank()) {
            view().showError(Messages.text("error.system.userNameRequired"));
            return;
        }

        runSystemCommand(
                Messages.text("error.system.userCreate"),
                Messages.format("info.system.userCreated", userName),
                () -> model().createSelectedDeviceUser(userName));
    }

    void switchUser() {
        Integer userId = selectedSystemUserId();
        if (userId == null) {
            return;
        }

        runSystemCommand(
                Messages.text("error.system.userSwitch"),
                Messages.format("info.system.userSwitched", userId),
                () -> model().switchSelectedDeviceUser(userId));
    }

    void deleteUser() {
        Integer userId = selectedSystemUserId();
        if (userId == null) {
            return;
        }

        if (!view().confirmAction(
                Messages.text("system.confirm.deleteUser.title"),
                Messages.format("system.confirm.deleteUser.message", userId))) {
            return;
        }

        runSystemCommand(
                Messages.text("error.system.userDelete"),
                Messages.format("info.system.userDeleted", userId),
                () -> model().removeSelectedDeviceUser(userId));
    }

    void applyAppLanguages() {
        boolean enabled = view().isShowAllAppLanguagesSelected();
        runSystemCommand(
                Messages.text("error.system.locales"),
                Messages.text("info.system.localesUpdated"),
                () -> model().setSelectedDeviceShowAllAppLanguages(enabled));
    }

    void applyGestures() {
        boolean enabled = view().isGesturalNavigationSelected();
        runSystemCommand(
                Messages.text("error.system.gestures"),
                Messages.text("info.system.gesturesUpdated"),
                () -> model().setSelectedDeviceGesturalNavigation(enabled));
    }

    void enableKeyboard() {
        String keyboardId = selectedKeyboardId();
        if (keyboardId == null) {
            return;
        }

        boolean enabled = view().isSelectedSystemKeyboardEnabled();
        runSystemCommand(
                Messages.text(enabled ? "error.system.keyboardDisable" : "error.system.keyboardEnable"),
                Messages.format(enabled ? "info.system.keyboardDisabled" : "info.system.keyboardEnabled", keyboardId),
                () -> model().setSelectedDeviceKeyboardEnabled(keyboardId, !enabled));
    }

    void setKeyboard() {
        String keyboardId = selectedKeyboardId();
        if (keyboardId == null) {
            return;
        }

        runSystemCommand(
                Messages.text("error.system.keyboardSelect"),
                Messages.format("info.system.keyboardSelected", keyboardId),
                () -> model().setSelectedDeviceKeyboard(keyboardId));
    }

    private Integer selectedSystemUserId() {
        Integer userId = view().getSelectedSystemUserId();
        if (userId == null) {
            view().showError(Messages.text("error.system.userRequired"));
            return null;
        }
        return userId;
    }

    private String selectedKeyboardId() {
        String keyboardId = view().getSelectedKeyboardId();
        if (keyboardId == null || keyboardId.isBlank()) {
            view().showError(Messages.text("error.system.keyboardRequired"));
            return null;
        }
        return keyboardId;
    }

    private void runSystemCommand(String errorMessage, String successMessage, SystemTask task) {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isSystemAvailable(selectedDevice)) {
            view().showError(Messages.text("error.system.deviceRequired"));
            return;
        }

        String requestedSerial = selectedDevice.serial();
        state().applyingSystemAction = true;
        view().setSystemStatus(Messages.text("system.status.loading"), false);
        context.updateSystemBusyState();

        new SwingWorker<SystemState, Void>() {
            @Override
            protected SystemState doInBackground() throws Exception {
                task.run();
                return model().getSelectedDeviceSystemState().orElse(SystemState.empty());
            }

            @Override
            protected void done() {
                try {
                    SystemState systemState = get();
                    if (!Objects.equals(requestedSerial, SystemController.this.state().currentSelectedSerial)) {
                        return;
                    }

                    view().setSystemState(systemState);
                    view().setSystemStatus(Messages.text("system.status.ready"), false);
                    if (successMessage != null && !successMessage.isBlank()) {
                        view().showInfo(successMessage);
                    }
                } catch (Exception exception) {
                    if (Objects.equals(requestedSerial, SystemController.this.state().currentSelectedSerial)) {
                        view().setSystemStatus(context.extractErrorMessage(exception, errorMessage), true);
                    }
                    context.handleError(errorMessage, exception);
                } finally {
                    SystemController.this.state().applyingSystemAction = false;
                    context.updateSystemBusyState();
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
