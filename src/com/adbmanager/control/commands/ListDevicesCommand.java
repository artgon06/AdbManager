package com.adbmanager.control.commands;

import com.adbmanager.control.CommandResult;
import com.adbmanager.exceptions.CommandExecuteException;
import com.adbmanager.logic.AdbModel;
import com.adbmanager.view.DeviceListFormatter;

public class ListDevicesCommand extends NoParamsCommand {

    private final DeviceListFormatter formatter = new DeviceListFormatter();

    public ListDevicesCommand() {
        super("devices", "d", "Muestra los dispositivos detectados", "Uso: devices");
    }

    @Override
    public CommandResult execute(AdbModel model) throws CommandExecuteException {
        try {
            model.refreshDevices();
            return CommandResult.ok(formatter.format(model.getDevices(), model.getSelectedDevice()));
        } catch (Exception e) {
            throw new CommandExecuteException("No se pudo obtener la lista de dispositivos.", e);
        }
    }
}
