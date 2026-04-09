package com.adbmanager.control.commands;

import com.adbmanager.control.CommandResult;
import com.adbmanager.logic.AdbModel;
import com.adbmanager.view.Messages;

public class HelpCommand extends NoParamsCommand {

    public HelpCommand() {
        super("help", "h", Messages.text("command.help.details"), Messages.text("command.help.usage"));
    }

    @Override
    public CommandResult execute(AdbModel model) {
        return CommandResult.ok(CommandGenerator.helpText());
    }
}
