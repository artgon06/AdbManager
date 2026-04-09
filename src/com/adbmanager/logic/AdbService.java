package com.adbmanager.logic;

import java.util.List;
import java.util.Optional;

import com.adbmanager.logic.client.AdbBinaryResult;
import com.adbmanager.logic.client.AdbClient;
import com.adbmanager.logic.client.AdbResult;
import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.logic.model.DeviceDetailsParser;
import com.adbmanager.logic.model.DeviceParser;
import com.adbmanager.view.Messages;

public class AdbService implements AdbModel {

    private final AdbClient client;
    private final DeviceParser parser = new DeviceParser();
    private final DeviceDetailsParser detailsParser = new DeviceDetailsParser();
    private List<Device> devices = List.of();
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
        selectedDeviceSerial = selectedDevice.serial();
        return selectedDevice;
    }

    @Override
    public Device selectDeviceBySerial(String serial) {
        Device selectedDevice = devices.stream()
                .filter(device -> device.serial().equals(serial))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(Messages.format("error.deviceNotFound", serial)));

        selectedDeviceSerial = selectedDevice.serial();
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

        return Optional.of(detailsParser.parse(
                device,
                propertiesResult.output(),
                memoryResult.output(),
                featuresResult.output(),
                sizeResult.output(),
                densityResult.output(),
                displayResult.output()));
    }

    @Override
    public byte[] captureSelectedDeviceScreenshot() throws Exception {
        Device device = getSelectedDevice()
                .orElseThrow(() -> new IllegalStateException(Messages.text("error.capture.selectDevice")));

        if (!Messages.STATUS_CONNECTED.equals(device.state())) {
            throw new IllegalStateException(Messages.text("error.capture.deviceDisconnected"));
        }

        AdbBinaryResult screenshotResult = client.runBinaryForSerial(
                device.serial(),
                List.of("exec-out", "screencap", "-p"));

        if (!screenshotResult.ok()) {
            throw new Exception("adb -s " + device.serial() + " exec-out screencap -p failed.");
        }

        return screenshotResult.output();
    }

    private void syncSelectedDevice() {
        if (selectedDeviceSerial == null) {
            return;
        }

        boolean selectedDeviceStillPresent = devices.stream()
                .anyMatch(device -> device.serial().equals(selectedDeviceSerial));

        if (!selectedDeviceStillPresent) {
            selectedDeviceSerial = null;
        }
    }
}
