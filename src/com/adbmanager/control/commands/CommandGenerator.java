package com.adbmanager.control.commands;

import com.adbmanager.exceptions.CommandParseException;

public class CommandGenerator {

    private static final Command[] AVAILABLE = {
            new ListDevicesCommand(),
            new SelectDeviceCommand(),
            new HelpCommand(),
            new ExitCommand()
    };

    public static Command parse(String[] words) throws CommandParseException {
        for (Command c : AVAILABLE) {
            Command parsed = c.parse(words);
            if (parsed != null) return parsed;
        }
        throw new CommandParseException("Comando desconocido: " + String.join(" ", words));
    }

    public static String helpText() {
        StringBuilder sb = new StringBuilder();
        for (Command c : AVAILABLE) {
            sb.append(c.helpText()).append("\n\n");
        }
        return sb.toString();
    }
}
