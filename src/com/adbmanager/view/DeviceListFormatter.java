package com.adbmanager.view;

import java.util.List;
import java.util.Optional;

import com.adbmanager.logic.model.Device;

public class DeviceListFormatter {

    public String format(List<Device> devices, Optional<Device> selectedDevice) {
        String selectedSerial = selectedDevice.map(Device::serial).orElse(null);
        StringBuilder sb = new StringBuilder();

        sb.append(Messages.format("devices.count", devices.size())).append("\n");

        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            boolean isSelected = device.serial().equals(selectedSerial);

            sb.append(i + 1)
                    .append(") ")
                    .append(device.serial())
                    .append(": ")
                    .append(Messages.stateLabel(device.state()));

            if (isSelected) {
                sb.append(Messages.text("devices.selectedMarker"));
            }

            sb.append("\n");

            if (Messages.STATUS_CONNECTED.equals(device.state())) {
                sb.append("\t")
                        .append(Messages.text("device.field.model"))
                        .append(": ")
                        .append(device.model())
                        .append("\n")
                        .append("\t")
                        .append(Messages.text("device.field.codename"))
                        .append(": ")
                        .append(device.device())
                        .append("\n");
            } else {
                sb.append("\t").append(Messages.text("devices.notConnectedInfo")).append("\n");
            }
        }

        sb.append(Messages.text("devices.selected"));
        if (selectedDevice.isPresent()) {
            Device device = selectedDevice.get();
            sb.append(findDevicePosition(devices, device) + 1)
                    .append(") ")
                    .append(device.serial());
        } else {
            sb.append(Messages.text("common.none"));
        }

        return sb.toString();
    }

    private int findDevicePosition(List<Device> devices, Device selectedDevice) {
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).serial().equals(selectedDevice.serial())) {
                return i;
            }
        }
        return -1;
    }
}
