package com.adbmanager.control.commands;

import com.adbmanager.exceptions.CommandParseException;
import com.adbmanager.view.Messages;

public class CommandGenerator {

    private static final Command[] AVAILABLE = {
            new ListDevicesCommand(),
            new SelectDeviceCommand(),
            new HelpCommand(),
            new ExitCommand()
    };

    public static Command parse(String[] words) throws CommandParseException {
        for (Command command : AVAILABLE) {
            Command parsed = command.parse(words);
            if (parsed != null) {
                return parsed;
            }
        }
        throw new CommandParseException(Messages.format("error.unknownCommand", String.join(" ", words)));
    }

    public static String helpText() {
        StringBuilder sb = new StringBuilder();
        for (Command command : AVAILABLE) {
            sb.append(command.helpText()).append("\n\n");
        }
        return sb.toString();
    }
}
