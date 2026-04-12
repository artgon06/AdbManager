package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicButtonUI;

import com.adbmanager.logic.model.AppInstallRequest;
import com.adbmanager.view.Messages;

public class AppInstallDialog extends JDialog {

    private final JLabel titleLabel = new JLabel();
    private final WrappingTextArea subtitleLabel = new WrappingTextArea();
    private final JLabel filesLabel = new JLabel();
    private final JTextArea filesArea = new JTextArea();
    private final JScrollPane filesScrollPane = new JScrollPane(filesArea);
    private final JButton browseButton = new JButton();

    private final JLabel optionsLabel = new JLabel();
    private final JCheckBox replaceExistingCheckBox = new JCheckBox();
    private final JCheckBox grantPermissionsCheckBox = new JCheckBox();
    private final JCheckBox allowTestPackagesCheckBox = new JCheckBox();
    private final JCheckBox bypassLowTargetSdkCheckBox = new JCheckBox();
    private final WrappingTextArea optionsNoteLabel = new WrappingTextArea();

    private final JLabel statusLabel = new JLabel();
    private final JLabel logLabel = new JLabel();
    private final JTextArea logArea = new JTextArea();
    private final JScrollPane logScrollPane = new JScrollPane(logArea);

    private final JButton cancelButton = new JButton();
    private final JButton installButton = new JButton();

    private final List<JPanel> surfacePanels = new ArrayList<>();
    private final List<Path> selectedFiles = new ArrayList<>();
    private AppTheme theme = AppTheme.LIGHT;
    private boolean busy;

    public AppInstallDialog(JFrame owner) {
        super(owner, true);
        buildDialog();
        bindEvents();
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
        updateActionState();
    }

    public void setInstallAction(ActionListener listener) {
        installButton.addActionListener(listener);
    }

    public void open() {
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    public void closeIfAllowed() {
        if (!busy) {
            setVisible(false);
        }
    }

    public void clearSelection() {
        selectedFiles.clear();
        filesArea.setText("");
        clearLog();
        showStatus("", false);
        updateActionState();
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        updateActionState();
    }

    public boolean isBusy() {
        return busy;
    }

    public AppInstallRequest getInstallRequest() {
        return new AppInstallRequest(
                List.copyOf(selectedFiles),
                replaceExistingCheckBox.isSelected(),
                grantPermissionsCheckBox.isSelected(),
                allowTestPackagesCheckBox.isSelected(),
                bypassLowTargetSdkCheckBox.isSelected());
    }

    public void appendLog(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        if (!logArea.getText().isBlank()) {
            logArea.append(System.lineSeparator());
        }
        logArea.append(message.trim());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void clearLog() {
        logArea.setText("");
        logArea.setCaretPosition(0);
    }

    public void showStatus(String message, boolean error) {
        String normalized = message == null ? "" : message.trim();
        statusLabel.putClientProperty("error", error);
        statusLabel.setText(normalized);
        statusLabel.setVisible(!normalized.isBlank());
        applyTheme(theme);
    }

    public void refreshTexts() {
        setTitle(Messages.text("apps.install.title"));
        titleLabel.setText(Messages.text("apps.install.title"));
        subtitleLabel.setText(Messages.text("apps.install.subtitle"));
        filesLabel.setText(Messages.text("apps.install.files"));
        browseButton.setText(Messages.text("apps.install.browse"));
        optionsLabel.setText(Messages.text("apps.install.options"));
        replaceExistingCheckBox.setText(Messages.text("apps.install.option.replace"));
        grantPermissionsCheckBox.setText(Messages.text("apps.install.option.grant"));
        allowTestPackagesCheckBox.setText(Messages.text("apps.install.option.test"));
        bypassLowTargetSdkCheckBox.setText(Messages.text("apps.install.option.bypass"));
        optionsNoteLabel.setText(Messages.text("apps.install.note"));
        logLabel.setText(Messages.text("apps.install.log"));
        cancelButton.setText(Messages.text("common.cancel"));
        installButton.setText(Messages.text("apps.install.action"));
        if (selectedFiles.isEmpty()) {
            filesArea.setText(Messages.text("apps.install.files.empty"));
        }
    }

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        getContentPane().setBackground(theme.background());

        titleLabel.setForeground(theme.textPrimary());
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));

        for (JPanel panel : surfacePanels) {
            panel.setBackground(theme.background());
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(theme.border(), 1),
                    BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        }

        subtitleLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 14), theme.textSecondary());
        optionsNoteLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 13), theme.textSecondary());

        for (JLabel label : List.of(filesLabel, optionsLabel, logLabel)) {
            label.setForeground(theme.textPrimary());
            label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        }

        styleTextArea(filesArea, false);
        styleTextArea(logArea, true);
        styleScrollPane(filesScrollPane);
        styleScrollPane(logScrollPane);

        styleCheckBox(replaceExistingCheckBox);
        styleCheckBox(grantPermissionsCheckBox);
        styleCheckBox(allowTestPackagesCheckBox);
        styleCheckBox(bypassLowTargetSdkCheckBox);

        styleButton(browseButton, false);
        styleButton(cancelButton, false);
        styleButton(installButton, true);

        statusLabel.setForeground(Boolean.TRUE.equals(statusLabel.getClientProperty("error"))
                ? new java.awt.Color(214, 80, 80)
                : theme.actionBackground());
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        repaint();
    }

    private void buildDialog() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(860, 680));
        setSize(new Dimension(920, 760));

        JPanel content = new JPanel();
        content.setOpaque(true);
        content.setBackground(theme.background());
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setVisible(false);

        content.add(titleLabel);
        content.add(Box.createVerticalStrut(8));
        content.add(subtitleLabel);
        content.add(Box.createVerticalStrut(20));
        content.add(buildFilesCard());
        content.add(Box.createVerticalStrut(16));
        content.add(buildOptionsCard());
        content.add(Box.createVerticalStrut(16));
        content.add(buildLogCard());
        content.add(Box.createVerticalStrut(12));
        content.add(statusLabel);
        content.add(Box.createVerticalStrut(14));
        content.add(buildActionsRow());

        setContentPane(content);
    }

    private JPanel buildFilesCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        filesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        browseButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        filesScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        filesScrollPane.setPreferredSize(new Dimension(0, 120));

        card.add(filesLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(filesScrollPane);
        card.add(Box.createVerticalStrut(10));
        card.add(browseButton);
        return card;
    }

    private JPanel buildOptionsCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        optionsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsNoteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsNoteLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel optionsGrid = new JPanel(new GridBagLayout());
        optionsGrid.setOpaque(false);
        addOption(optionsGrid, replaceExistingCheckBox, 0);
        addOption(optionsGrid, grantPermissionsCheckBox, 1);
        addOption(optionsGrid, allowTestPackagesCheckBox, 2);
        addOption(optionsGrid, bypassLowTargetSdkCheckBox, 3);

        card.add(optionsLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(optionsGrid);
        card.add(Box.createVerticalStrut(10));
        card.add(optionsNoteLabel);
        return card;
    }

    private JPanel buildLogCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        logLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        logScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        logScrollPane.setPreferredSize(new Dimension(0, 220));

        card.add(logLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(logScrollPane);
        return card;
    }

    private JPanel buildActionsRow() {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        cancelButton.setHorizontalAlignment(SwingConstants.CENTER);
        installButton.setHorizontalAlignment(SwingConstants.CENTER);
        row.add(cancelButton, BorderLayout.WEST);
        row.add(installButton, BorderLayout.CENTER);
        return row;
    }

    private JPanel createCard() {
        JPanel panel = new JPanel();
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setOpaque(true);
        surfacePanels.add(panel);
        return panel;
    }

    private void addOption(JPanel container, JCheckBox checkBox, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, row == 3 ? 0 : 8, 0);
        container.add(checkBox, constraints);
    }

    private void bindEvents() {
        replaceExistingCheckBox.setSelected(true);
        browseButton.addActionListener(event -> chooseFiles());
        cancelButton.addActionListener(event -> closeIfAllowed());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeIfAllowed();
            }
        });
        getRootPane().registerKeyboardAction(
                event -> closeIfAllowed(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void chooseFiles() {
        if (busy) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(Messages.text("apps.install.fileChooser.title"));
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter(
                Messages.text("apps.install.fileChooser.filter"),
                "apk",
                "apks",
                "apkm",
                "aab",
                "zip",
                "xapk"));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File[] files = chooser.getSelectedFiles();
        selectedFiles.clear();
        if (files != null) {
            for (File file : files) {
                if (file != null) {
                    selectedFiles.add(file.toPath().toAbsolutePath().normalize());
                }
            }
        }

        clearLog();
        showStatus("", false);
        refreshSelectedFilesText();
        updateActionState();
    }

    private void refreshSelectedFilesText() {
        if (selectedFiles.isEmpty()) {
            filesArea.setText(Messages.text("apps.install.files.empty"));
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (Path path : selectedFiles) {
            if (!builder.isEmpty()) {
                builder.append(System.lineSeparator());
            }
            builder.append(path.toString());
        }
        filesArea.setText(builder.toString());
        filesArea.setCaretPosition(0);
    }

    private void updateActionState() {
        browseButton.setEnabled(!busy);
        replaceExistingCheckBox.setEnabled(!busy);
        grantPermissionsCheckBox.setEnabled(!busy);
        allowTestPackagesCheckBox.setEnabled(!busy);
        bypassLowTargetSdkCheckBox.setEnabled(!busy);
        cancelButton.setEnabled(!busy);
        installButton.setEnabled(!busy && !selectedFiles.isEmpty());
        applyTheme(theme);
    }

    private void styleTextArea(JTextArea textArea, boolean monospace) {
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setOpaque(true);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setForeground(theme.textPrimary());
        textArea.setBackground(theme.secondarySurface());
        textArea.setCaretColor(theme.textPrimary());
        textArea.setDisabledTextColor(theme.textSecondary());
        textArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        textArea.setFont(new Font(monospace ? Font.MONOSPACED : Font.SANS_SERIF, Font.PLAIN, monospace ? 12 : 13));
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(theme.border(), 1));
        scrollPane.getViewport().setBackground(theme.secondarySurface());
        if (scrollPane.getVerticalScrollBar() != null) {
            scrollPane.getVerticalScrollBar().setUI(new ThemedScrollBarUI(theme));
            scrollPane.getVerticalScrollBar().setUnitIncrement(24);
            scrollPane.getVerticalScrollBar().setBlockIncrement(96);
        }
        if (scrollPane.getHorizontalScrollBar() != null) {
            scrollPane.getHorizontalScrollBar().setUI(new ThemedScrollBarUI(theme));
            scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        }
    }

    private void styleCheckBox(JCheckBox checkBox) {
        checkBox.setOpaque(true);
        checkBox.setBackground(theme.background());
        checkBox.setForeground(checkBox.isEnabled() ? theme.textPrimary() : theme.textSecondary());
        checkBox.setFocusPainted(false);
        checkBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    }

    private void styleButton(JButton button, boolean primary) {
        boolean enabled = button.isEnabled();
        boolean hovered = enabled && button.getModel().isRollover();
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setPreferredSize(new Dimension(primary ? 0 : 180, 40));

        if (enabled) {
            java.awt.Color background = primary
                    ? theme.actionBackground()
                    : ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.84d);
            if (hovered) {
                background = ThemeUtils.blend(background, theme.selectionBackground(), primary ? 0.18d : 0.22d);
            }
            button.setBackground(background);
            button.setForeground(primary ? theme.actionForeground() : theme.textPrimary());
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primary ? background : theme.border(), 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        } else {
            button.setBackground(theme.surface());
            button.setForeground(theme.textSecondary());
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(theme.disabledBorder(), 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        }
    }
}
