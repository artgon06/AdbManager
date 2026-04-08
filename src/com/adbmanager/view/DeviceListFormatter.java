package com.adbmanager.view;

import java.util.List;
import java.util.Optional;

import com.adbmanager.logic.model.Device;

public class DeviceListFormatter {

    public String format(List<Device> devices, Optional<Device> selectedDevice) {
        String selectedSerial = selectedDevice.map(Device::serial).orElse(null);
        StringBuilder sb = new StringBuilder();

        sb.append("Dispositivos conectados: ").append(devices.size()).append("\n");

        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            boolean isSelected = device.serial().equals(selectedSerial);

            sb.append(i + 1)
                    .append(") ")
                    .append(device.serial())
                    .append(": ")
                    .append(device.state());

            if (isSelected) {
                sb.append(" [seleccionado]");
            }

            sb.append("\n");

            if (Messages.STATUS_CONNECTED.equals(device.state())) {
                sb.append("\t").append(Messages.MODEL).append(": ").append(device.model()).append("\n")
                        .append("\t").append(Messages.CODENAME).append(": ").append(device.device()).append("\n");
            } else {
                sb.append("\t").append(Messages.ERROR_NOT_CONECTED).append("\n");
            }
        }

        sb.append("Dispositivo seleccionado: ");
        if (selectedDevice.isPresent()) {
            Device device = selectedDevice.get();
            sb.append(findDevicePosition(devices, device) + 1)
                    .append(") ")
                    .append(device.serial());
        } else {
            sb.append("ninguno");
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
