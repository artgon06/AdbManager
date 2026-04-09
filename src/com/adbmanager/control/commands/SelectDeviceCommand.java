package com.adbmanager.control.commands;

import com.adbmanager.control.CommandResult;
import com.adbmanager.exceptions.CommandExecuteException;
import com.adbmanager.exceptions.CommandParseException;
import com.adbmanager.logic.AdbModel;
import com.adbmanager.logic.model.Device;
import com.adbmanager.view.Messages;

public class SelectDeviceCommand extends AbstractCommand {

    private final String selector;

    public SelectDeviceCommand() {
        this(null);
    }

    private SelectDeviceCommand(String selector) {
        super("select", "s", Messages.text("command.select.details"), Messages.text("command.select.usage"));
        this.selector = selector;
    }

    @Override
    public Command parse(String[] words) throws CommandParseException {
        if (!matchCommandName(words[0])) {
            return null;
        }

        if (words.length != 2) {
            throw new CommandParseException(Messages.text("command.select.usage"));
        }

        return new SelectDeviceCommand(words[1]);
    }

    @Override
    public CommandResult execute(AdbModel model) throws CommandExecuteException {
        try {
            model.refreshDevices();
            Device selectedDevice = selectDevice(model);
            return CommandResult.ok(Messages.format("device.select.success", selectedDevice.serial()));
        } catch (IllegalArgumentException e) {
            throw new CommandExecuteException(e.getMessage(), e);
        } catch (Exception e) {
            throw new CommandExecuteException(Messages.text("error.device.select"), e);
        }
    }

    private Device selectDevice(AdbModel model) {
        Integer selectedIndex = parseIndex(selector);
        if (selectedIndex != null) {
            return model.selectDeviceByIndex(selectedIndex);
        }

        return model.selectDeviceBySerial(selector);
    }

    private Integer parseIndex(String value) {
        try {
            int parsedIndex = Integer.parseInt(value);
            if (parsedIndex <= 0) {
                throw new IllegalArgumentException(Messages.text("error.device.indexPositive"));
            }
            return parsedIndex - 1;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
