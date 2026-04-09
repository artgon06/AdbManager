package com.adbmanager.control.commands;

import com.adbmanager.control.CommandResult;
import com.adbmanager.exceptions.CommandExecuteException;
import com.adbmanager.logic.AdbModel;
import com.adbmanager.view.DeviceListFormatter;
import com.adbmanager.view.Messages;

public class ListDevicesCommand extends NoParamsCommand {

    private final DeviceListFormatter formatter = new DeviceListFormatter();

    public ListDevicesCommand() {
        super("devices", "d", Messages.text("command.devices.details"), Messages.text("command.devices.usage"));
    }

    @Override
    public CommandResult execute(AdbModel model) throws CommandExecuteException {
        try {
            model.refreshDevices();
            return CommandResult.ok(formatter.format(model.getDevices(), model.getSelectedDevice()));
        } catch (Exception e) {
            throw new CommandExecuteException(Messages.text("error.devices.list"), e);
        }
    }
}
