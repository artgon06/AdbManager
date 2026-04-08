package com.adbmanager.control.commands;

import com.adbmanager.control.CommandResult;
import com.adbmanager.exceptions.CommandExecuteException;
import com.adbmanager.exceptions.CommandParseException;
import com.adbmanager.logic.AdbModel;
import com.adbmanager.logic.model.Device;

public class SelectDeviceCommand extends AbstractCommand {

    private final String selector;

    public SelectDeviceCommand() {
        this(null);
    }

    private SelectDeviceCommand(String selector) {
        super("select", "s", "Selecciona el dispositivo activo", "Uso: select <numero|serial>");
        this.selector = selector;
    }

    @Override
    public Command parse(String[] words) throws CommandParseException {
        if (!matchCommandName(words[0])) {
            return null;
        }

        if (words.length != 2) {
            throw new CommandParseException("Uso: select <numero|serial>");
        }

        return new SelectDeviceCommand(words[1]);
    }

    @Override
    public CommandResult execute(AdbModel model) throws CommandExecuteException {
        try {
            model.refreshDevices();
            Device selectedDevice = selectDevice(model);
            return CommandResult.ok("Dispositivo seleccionado: " + selectedDevice.serial());
        } catch (IllegalArgumentException e) {
            throw new CommandExecuteException(e.getMessage(), e);
        } catch (Exception e) {
            throw new CommandExecuteException("No se pudo seleccionar el dispositivo.", e);
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
                throw new IllegalArgumentException("El indice del dispositivo debe ser mayor que cero.");
            }
            return parsedIndex - 1;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
