package com.adbmanager.control.commands;

import com.adbmanager.control.CommandResult;
import com.adbmanager.logic.AdbModel;
import com.adbmanager.view.Messages;

public class ExitCommand extends NoParamsCommand {

    public ExitCommand() {
        super("exit", "e", Messages.text("command.exit.details"), Messages.text("command.exit.usage"));
    }

    @Override
    public CommandResult execute(AdbModel model) {
        return CommandResult.exit();
    }
}
