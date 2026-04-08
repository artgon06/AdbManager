package com.adbmanager;

import java.time.Duration;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.adbmanager.control.Controller;
import com.adbmanager.control.SwingController;
import com.adbmanager.logic.AdbModel;
import com.adbmanager.logic.AdbService;
import com.adbmanager.logic.client.AdbClient;
import com.adbmanager.view.ConsoleView;
import com.adbmanager.view.swing.MainFrame;

public class App {

    public static void main(String[] args) {
        AdbModel model = new AdbService(new AdbClient("adb", Duration.ofSeconds(60)));

        if (args.length > 0 && "--cli".equalsIgnoreCase(args[0])) {
            new Controller(model, new ConsoleView()).run();
            return;
        }

        SwingUtilities.invokeLater(() -> launchSwing(model));
    }

    private static void launchSwing(AdbModel model) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        MainFrame frame = new MainFrame();
        new SwingController(model, frame).start();
    }
}
