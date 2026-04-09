package com.adbmanager.control.commands;

import com.adbmanager.exceptions.CommandParseException;
import com.adbmanager.view.Messages;

public abstract class NoParamsCommand extends AbstractCommand {

    protected NoParamsCommand(String name, String shortcut, String details, String help) {
        super(name, shortcut, details, help);
    }

    @Override
    public Command parse(String[] words) throws CommandParseException {
        if (words.length > 1 && matchCommandName(words[0])) {
            throw new CommandParseException(Messages.text("error.command.noParams"));
        }
        if (words.length == 1 && matchCommandName(words[0])) {
            return this;
        }
        return null;
    }
}
