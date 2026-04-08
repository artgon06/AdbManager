package com.adbmanager.logic;

import java.util.List;
import java.util.Optional;

import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.DeviceDetails;

public interface AdbModel {
    void refreshDevices() throws Exception;
    List<Device> getDevices();
    Optional<Device> getSelectedDevice();
    Device selectDeviceByIndex(int index);
    Device selectDeviceBySerial(String serial);
    Optional<DeviceDetails> getSelectedDeviceDetails() throws Exception;
    byte[] captureSelectedDeviceScreenshot() throws Exception;
}
