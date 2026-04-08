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
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

import com.adbmanager.logic.AdbModel;
import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.view.Messages;
import com.adbmanager.view.swing.AppTheme;
import com.adbmanager.view.swing.MainFrame;

public class SwingController {

    private final AdbModel model;
    private final MainFrame view;
    private boolean syncingDeviceSelector;
    private boolean loadingDevices;
    private String currentSelectedSerial;

    public SwingController(AdbModel model, MainFrame view) {
        this.model = model;
        this.view = view;
    }

    public void start() {
        bindEvents();
        view.setSelectedTheme(AppTheme.LIGHT);
        view.setTheme(AppTheme.LIGHT);
        view.showHomeScreen();
        view.showWindow();
        refreshDevices();
    }

    private void bindEvents() {
        view.setDeviceSelectionAction(event -> onDeviceSelected());
        view.setCaptureAction(event -> captureScreenshot());
        view.setSaveCaptureAction(event -> saveScreenshot());
        view.setRefreshAction(event -> refreshDevices());
        view.setHomeAction(event -> view.showHomeScreen());
        view.setSettingsAction(event -> view.showSettingsScreen());
        view.setThemeChangeAction(event -> applyThemeSelection());
        view.setRepositoryAction(event -> openRepository());
        view.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent event) {
                refreshDevices();
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
                    handleError("No se pudieron cargar los dispositivos.", e);
                    view.setDevices(List.of(), null);
                    view.clearDeviceDetails();
                    view.clearScreenshot();
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
                    handleError("No se pudo seleccionar el dispositivo.", e);
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
                    throw new IllegalStateException("La captura devuelta por adb no tiene un formato de imagen valido.");
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
                    handleError("No se pudo hacer la captura.", e);
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
            view.showError("No hay ninguna captura para guardar.");
            return;
        }

        File outputFile = view.chooseScreenshotDestination();
        if (outputFile == null) {
            return;
        }

        try {
            ImageIO.write(screenshot, "png", outputFile);
            view.showInfo("Captura guardada en:\n" + outputFile.getAbsolutePath());
        } catch (IOException e) {
            handleError("No se pudo guardar la captura.", e);
        }
    }

    private void applyThemeSelection() {
        view.setTheme(view.getSelectedTheme());
    }

    private void openRepository() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(Messages.REPOSITORY_URL));
                return;
            }
        } catch (Exception e) {
            handleError("No se pudo abrir el repositorio.", e);
            return;
        }

        view.showInfo(Messages.REPOSITORY_URL);
    }

    private void updateDevicePresentation(Device selectedDevice, DeviceDetails details) {
        currentSelectedSerial = selectedDevice == null ? null : selectedDevice.serial();

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
    }

    private boolean isCaptureAvailable(Device selectedDevice) {
        return selectedDevice != null && Messages.STATUS_CONNECTED.equals(selectedDevice.state());
    }

    private void handleError(String message, Exception exception) {
        String detail = exception.getMessage();
        if (detail == null && exception.getCause() != null) {
            detail = exception.getCause().getMessage();
        }

        if (detail == null || detail.isBlank()) {
            view.showError(message);
        } else {
            view.showError(message + "\n\n" + detail);
        }
    }

    private record RefreshState(List<Device> devices, Device selectedDevice, DeviceDetails details) {
    }
}
