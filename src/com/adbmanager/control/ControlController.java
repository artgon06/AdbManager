package com.adbmanager.control;

import java.util.Objects;

import javax.swing.SwingWorker;

import com.adbmanager.logic.model.ControlState;
import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.DeviceSoundMode;
import com.adbmanager.view.Messages;
import com.adbmanager.view.swing.MainFrame;

final class ControlController {

    @FunctionalInterface
    private interface ControlTask {
        void run() throws Exception;
    }

    private final SwingControllerContext context;

    ControlController(SwingControllerContext context) {
        this.context = context;
    }

    void clearViewState() {
        view().clearControlState();
        view().setControlStatus("", false);
    }

    void refreshState(boolean showErrors) {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isControlAvailable(selectedDevice)) {
            view().setControlDeviceAvailable(false);
            clearViewState();
            return;
        }

        String requestedSerial = selectedDevice.serial();
        if (state().loadingControlState || (state().applyingControlAction && !showErrors)) {
            return;
        }

        state().loadingControlState = true;
        view().setControlDeviceAvailable(true);
        view().setControlStatus(Messages.text("control.status.loading"), false);
        context.updateControlBusyState();

        new SwingWorker<ControlState, Void>() {
            @Override
            protected ControlState doInBackground() throws Exception {
                return model().getSelectedDeviceControlState().orElse(ControlState.empty());
            }

            @Override
            protected void done() {
                try {
                    ControlState controlState = get();
                    if (!Objects.equals(requestedSerial, ControlController.this.state().currentSelectedSerial)) {
                        return;
                    }
                    view().setControlState(controlState);
                    view().setControlStatus(Messages.text("control.status.ready"), false);
                } catch (Exception exception) {
                    if (!Objects.equals(requestedSerial, ControlController.this.state().currentSelectedSerial)) {
                        return;
                    }

                    view().clearControlState();
                    view().setControlStatus(
                            context.extractErrorMessage(exception, Messages.text("error.control.load")),
                            true);
                    if (showErrors) {
                        context.handleError(Messages.text("error.control.load"), exception);
                    }
                } finally {
                    ControlController.this.state().loadingControlState = false;
                    context.updateControlBusyState();
                }
            }
        }.execute();
    }

    void sendQuickControlKeyEvent(String keyEvent) {
        runControlCommand(
                Messages.text("error.control.keyevent"),
                Messages.text("control.status.keyeventSent"),
                false,
                () -> model().sendSelectedDeviceKeyEvent(keyEvent));
    }

    void sendControlTextInput() {
        String text = view().getControlTextInput();
        if (text == null || text.isBlank()) {
            view().showError(Messages.text("error.control.textRequired"));
            return;
        }

        runControlCommand(
                Messages.text("error.control.text"),
                Messages.text("control.status.textSent"),
                false,
                () -> model().sendSelectedDeviceTextInput(text));
    }

    void applyBrightness() {
        int brightness = view().getControlBrightness();
        runControlCommand(
                Messages.text("error.control.brightness"),
                Messages.format("control.status.brightnessApplied", brightness),
                true,
                () -> model().setSelectedDeviceBrightness(brightness));
    }

    void applyVolume() {
        int volume = view().getControlVolume();
        runControlCommand(
                Messages.text("error.control.volume"),
                Messages.format("control.status.volumeApplied", volume),
                true,
                () -> model().setSelectedDeviceMediaVolume(volume));
    }

    void applySoundMode() {
        DeviceSoundMode soundMode = view().getControlSoundMode();
        runControlCommand(
                Messages.text("error.control.soundMode"),
                Messages.format("control.status.soundModeApplied", Messages.text(soundMode.messageKey())),
                true,
                () -> model().setSelectedDeviceSoundMode(soundMode));
    }

    void applyTap() {
        Integer x = view().getControlTapX();
        Integer y = view().getControlTapY();
        if (x == null || y == null) {
            view().showError(Messages.text("error.control.tap"));
            return;
        }

        runControlCommand(
                Messages.text("error.control.tap"),
                Messages.format("control.status.tapApplied", x, y),
                false,
                () -> model().tapSelectedDevice(x, y));
    }

    void applySwipe() {
        Integer x1 = view().getControlSwipeX1();
        Integer y1 = view().getControlSwipeY1();
        Integer x2 = view().getControlSwipeX2();
        Integer y2 = view().getControlSwipeY2();
        Integer durationMs = view().getControlSwipeDurationMs();
        if (x1 == null || y1 == null || x2 == null || y2 == null || durationMs == null) {
            view().showError(Messages.text("error.control.swipe"));
            return;
        }

        runControlCommand(
                Messages.text("error.control.swipe"),
                Messages.format("control.status.swipeApplied", x1, y1, x2, y2, durationMs),
                false,
                () -> model().swipeSelectedDevice(x1, y1, x2, y2, durationMs));
    }

    void applyManualKeyEvent() {
        String keyEvent = view().getControlManualKeyEvent();
        if (keyEvent == null || keyEvent.isBlank()) {
            view().showError(Messages.text("error.control.keyeventRequired"));
            return;
        }

        runControlCommand(
                Messages.text("error.control.keyevent"),
                Messages.format("control.status.manualKeyeventApplied", keyEvent.trim()),
                false,
                () -> model().sendSelectedDeviceKeyEvent(keyEvent));
    }

    void applyRawInputCommand() {
        String rawCommand = view().getControlRawInputCommand();
        if (rawCommand == null || rawCommand.isBlank()) {
            view().showError(Messages.text("error.control.rawInputRequired"));
            return;
        }

        runControlCommand(
                Messages.text("error.control.rawInput"),
                Messages.format("control.status.rawInputApplied", rawCommand.trim()),
                false,
                () -> model().runSelectedDeviceInputCommand(rawCommand));
    }

    private void runControlCommand(
            String errorMessage,
            String successMessage,
            boolean refreshAfter,
            ControlTask task) {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isControlAvailable(selectedDevice)) {
            view().showError(Messages.text("error.control.deviceRequired"));
            return;
        }

        String requestedSerial = selectedDevice.serial();
        state().applyingControlAction = true;
        view().setControlStatus(Messages.text("control.status.executing"), false);
        context.updateControlBusyState();

        new SwingWorker<ControlState, Void>() {
            @Override
            protected ControlState doInBackground() throws Exception {
                task.run();
                return refreshAfter ? model().getSelectedDeviceControlState().orElse(ControlState.empty()) : null;
            }

            @Override
            protected void done() {
                try {
                    ControlState controlState = get();
                    if (!Objects.equals(requestedSerial, ControlController.this.state().currentSelectedSerial)) {
                        return;
                    }
                    if (refreshAfter && controlState != null) {
                        view().setControlState(controlState);
                    }
                    view().setControlStatus(
                            successMessage == null ? Messages.text("control.status.ready") : successMessage,
                            false);
                } catch (Exception exception) {
                    if (Objects.equals(requestedSerial, ControlController.this.state().currentSelectedSerial)) {
                        view().setControlStatus(context.extractErrorMessage(exception, errorMessage), true);
                    }
                } finally {
                    ControlController.this.state().applyingControlAction = false;
                    context.updateControlBusyState();
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
