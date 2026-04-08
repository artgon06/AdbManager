package com.adbmanager.control;

import java.io.IOException;

import com.adbmanager.control.commands.Command;
import com.adbmanager.control.commands.CommandGenerator;
import com.adbmanager.control.commands.ListDevicesCommand;
import com.adbmanager.exceptions.CommandException;
import com.adbmanager.logic.AdbModel;
import com.adbmanager.view.ConsoleView;

public class Controller {

    private final AdbModel model;
    private final ConsoleView view;

    public Controller(AdbModel model, ConsoleView view) {
        this.model = model;
        this.view = view;
    }

    public void run() {
        boolean running = true;
        view.showWelcome();
        showInitialDevices();

        while (running) {
            try {
                view.showPrompt();
                String line = view.readLine();
                if (line == null) {
                    break;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] words = line.split("\\s+");

                Command cmd = CommandGenerator.parse(words);
                CommandResult result = cmd.execute(model);

                if (result.output() != null) {
                    view.show(result.output());
                }
                if (result.exitRequested()) {
                    running = false;
                }
            } catch (CommandException e) {
                showCommandError(e);
            } catch (IOException e) {
                view.showError("Error leyendo de consola: " + e.getMessage());
                running = false;
            } catch (Exception e) {
                view.showError("Error al ejecutar el comando: " + e.getMessage());
                running = false;
            }
        }
    }

    private void showInitialDevices() {
        try {
            CommandResult result = new ListDevicesCommand().execute(model);
            if (result.output() != null) {
                view.show(result.output());
            }
        } catch (CommandException e) {
            showCommandError(e);
        }
    }

    private void showCommandError(CommandException e) {
        view.showError(e.getMessage());

        Throwable cause = e.getCause();
        while (cause != null) {
            view.showError(cause.getMessage());
            cause = cause.getCause();
        }
    }
}
