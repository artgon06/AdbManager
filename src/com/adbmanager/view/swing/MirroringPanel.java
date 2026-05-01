package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.adbmanager.view.Messages;

public class MirroringPanel extends JPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel scrcpyLabel = new JLabel("scrcpy");
    private final ScrcpyLauncherPanel scrcpyPanel;
    private AppTheme theme = AppTheme.LIGHT;

    public MirroringPanel(ScrcpyLauncherPanel scrcpyPanel) {
        this.scrcpyPanel = scrcpyPanel;
        buildPanel();
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
    }

    public void refreshTexts() {
        titleLabel.setText(Messages.text("mirroring.title"));
        scrcpyPanel.refreshTexts();
    }

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        setBackground(theme.background());
        titleLabel.setForeground(theme.textPrimary());
        titleLabel.setFont(new Font("Inter", Font.BOLD, 28));
        scrcpyLabel.setForeground(theme.textSecondary());
        scrcpyLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD | Font.ITALIC, 20));
        scrcpyPanel.applyTheme(theme);
        repaint();
    }

    private void buildPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
        headerPanel.setOpaque(false);
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createHorizontalStrut(12));
        headerPanel.add(scrcpyLabel);
        headerPanel.add(Box.createHorizontalGlue());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        add(headerPanel, BorderLayout.NORTH);
        add(scrcpyPanel, BorderLayout.CENTER);
    }
}
