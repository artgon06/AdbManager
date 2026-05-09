package com.adbmanager;

import java.time.Duration;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.adbmanager.control.SwingController;
import com.adbmanager.logic.AdbModel;
import com.adbmanager.logic.AdbExecutableService;
import com.adbmanager.logic.AdbService;
import com.adbmanager.logic.ScrcpyService;
import com.adbmanager.logic.client.AdbClient;
import com.adbmanager.view.swing.MainFrame;

public class App {

    public static void main(String[] args) {
        configureAwtWorkarounds();

        AdbExecutableService adbExecutableService = new AdbExecutableService();
        AdbModel model = new AdbService(new AdbClient(
                () -> adbExecutableService.ensureAvailable().toString(),
                Duration.ofSeconds(60)));

        SwingUtilities.invokeLater(() -> launchSwing(model, adbExecutableService));
    }

    private static void launchSwing(AdbModel model, AdbExecutableService adbExecutableService) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        MainFrame frame = new MainFrame();
        new SwingController(model, new ScrcpyService(adbExecutableService), frame).start();
    }

    private static void configureAwtWorkarounds() {
        System.setProperty("java.awt.im.style", "on-the-spot");

        Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (isWindowsInputMethodPeerException(throwable)) {
                return;
            }

            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable);
            } else {
                throwable.printStackTrace();
            }
        });
    }

    private static boolean isWindowsInputMethodPeerException(Throwable throwable) {
        if (!(throwable instanceof NullPointerException)
                || !"peer".equals(throwable.getMessage())) {
            return false;
        }

        for (StackTraceElement element : throwable.getStackTrace()) {
            if ("sun.awt.windows.WInputMethod".equals(element.getClassName())
                    && "openCandidateWindow".equals(element.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
