package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.view.Messages;

public class MainFrame extends JFrame {

    private static final String HOME_TAB = "home";
    private static final String SETTINGS_TAB = "settings";
    private static final int TOP_BAR_HEIGHT = 56;

    private final java.awt.CardLayout cardLayout = new java.awt.CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JPanel topBar = new JPanel(new BorderLayout());
    private final JPanel navigationTabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JPanel deviceSelectorPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
    private final ButtonGroup navigationGroup = new ButtonGroup();
    private final DeviceComboBoxRenderer deviceRenderer = new DeviceComboBoxRenderer();
    private final JComboBox<Device> deviceSelector = new JComboBox<>();
    private final JLabel deviceLabel = new JLabel("Dispositivo:");
    private final JToggleButton homeButton = new JToggleButton();
    private final JToggleButton settingsButton = new JToggleButton();
    private final JButton refreshButton = new JButton();
    private final HomePanel homePanel = new HomePanel();
    private final SettingsPanel settingsPanel = new SettingsPanel();
    private AppTheme currentTheme = AppTheme.LIGHT;

    public MainFrame() {
        super(Messages.APP_NAME + " " + Messages.VERSION);
        buildFrame();
        setTheme(AppTheme.LIGHT);
    }

    public void showWindow() {
        setVisible(true);
    }

    public void setCaptureAction(ActionListener actionListener) {
        homePanel.setCaptureAction(actionListener);
    }

    public void setSaveCaptureAction(ActionListener actionListener) {
        homePanel.setSaveCaptureAction(actionListener);
    }

    public void setDeviceSelectionAction(ActionListener actionListener) {
        deviceSelector.addActionListener(actionListener);
    }

    public void setHomeAction(ActionListener actionListener) {
        homeButton.addActionListener(actionListener);
    }

    public void setSettingsAction(ActionListener actionListener) {
        settingsButton.addActionListener(actionListener);
    }

    public void setRefreshAction(ActionListener actionListener) {
        refreshButton.addActionListener(actionListener);
    }

    public void setThemeChangeAction(ActionListener actionListener) {
        settingsPanel.setThemeChangeAction(actionListener);
    }

    public void setRepositoryAction(ActionListener actionListener) {
        settingsPanel.setRepositoryAction(actionListener);
    }

    public String getSelectedDeviceSerial() {
        Device selectedDevice = (Device) deviceSelector.getSelectedItem();
        return selectedDevice == null ? null : selectedDevice.serial();
    }

    public AppTheme getSelectedTheme() {
        return settingsPanel.getSelectedTheme();
    }

    public void setSelectedTheme(AppTheme theme) {
        settingsPanel.setSelectedTheme(theme);
    }

    public void showHomeScreen() {
        cardLayout.show(contentPanel, HOME_TAB);
        homeButton.setSelected(true);
        settingsButton.setSelected(false);
        updateNavigationStyles();
    }

    public void showSettingsScreen() {
        cardLayout.show(contentPanel, SETTINGS_TAB);
        settingsButton.setSelected(true);
        homeButton.setSelected(false);
        updateNavigationStyles();
    }

    public void setDevices(List<Device> devices, String selectedSerial) {
        DefaultComboBoxModel<Device> comboBoxModel = new DefaultComboBoxModel<>();
        for (Device device : devices) {
            comboBoxModel.addElement(device);
        }

        deviceSelector.setModel(comboBoxModel);
        deviceSelector.setRenderer(deviceRenderer);

        if (selectedSerial == null) {
            deviceSelector.setSelectedItem(null);
            return;
        }

        for (int i = 0; i < comboBoxModel.getSize(); i++) {
            Device device = comboBoxModel.getElementAt(i);
            if (device.serial().equals(selectedSerial)) {
                deviceSelector.setSelectedItem(device);
                return;
            }
        }
    }

    public void setDeviceDetails(DeviceDetails details) {
        homePanel.setDeviceDetails(details);
    }

    public void clearDeviceDetails() {
        homePanel.clearDeviceDetails();
    }

    public void setScreenshot(BufferedImage image) {
        homePanel.setScreenshot(image);
    }

    public void clearScreenshot() {
        homePanel.clearScreenshot();
    }

    public BufferedImage getCurrentScreenshot() {
        return homePanel.getCurrentScreenshot();
    }

    public File chooseScreenshotDestination() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar captura");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Imagen PNG", "png"));
        fileChooser.setSelectedFile(new File(defaultScreenshotName()));

        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File selectedFile = fileChooser.getSelectedFile();
        if (!selectedFile.getName().toLowerCase().endsWith(".png")) {
            File parentDirectory = selectedFile.getParentFile();
            selectedFile = parentDirectory == null
                    ? new File(selectedFile.getName() + ".png")
                    : new File(parentDirectory, selectedFile.getName() + ".png");
        }

        return selectedFile;
    }

    public void setCaptureEnabled(boolean enabled) {
        homePanel.setCaptureEnabled(enabled);
    }

    public void setSaveCaptureEnabled(boolean enabled) {
        homePanel.setSaveCaptureEnabled(enabled);
    }

    public void setDeviceSelectorEnabled(boolean enabled) {
        deviceSelector.setEnabled(enabled);
    }

    public void setRefreshEnabled(boolean enabled) {
        refreshButton.setEnabled(enabled);
        styleRefreshButton();
    }

    public void setTheme(AppTheme theme) {
        currentTheme = theme;
        settingsPanel.setSelectedTheme(theme);
        getContentPane().setBackground(theme.background());
        contentPanel.setBackground(theme.background());

        topBar.setBackground(theme.surface());
        topBar.setPreferredSize(new Dimension(0, TOP_BAR_HEIGHT));
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, theme.border()));
        navigationTabsPanel.setBackground(theme.surface());
        deviceSelectorPanel.setBackground(theme.surface());

        deviceLabel.setForeground(theme.textSecondary());
        deviceLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        deviceSelector.setBackground(theme.surface());
        deviceSelector.setForeground(theme.textPrimary());
        deviceSelector.setPreferredSize(new Dimension(360, 36));
        deviceSelector.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        deviceRenderer.applyTheme(theme);

        homePanel.applyTheme(theme);
        settingsPanel.applyTheme(theme);
        styleNavigationButton(homeButton);
        styleNavigationButton(settingsButton);
        styleRefreshButton();

        revalidate();
        repaint();
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Informacion", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void buildFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1240, 820));
        setSize(new Dimension(1440, 900));
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());

        addTopBar();
        addContent();
    }

    private void addTopBar() {
        configureNavigationButton(homeButton, "Inicio", ToolbarIcon.Type.HOME);
        configureNavigationButton(settingsButton, "Ajustes", ToolbarIcon.Type.SETTINGS);
        configureRefreshButton();

        navigationGroup.add(homeButton);
        navigationGroup.add(settingsButton);
        navigationTabsPanel.add(homeButton);
        navigationTabsPanel.add(settingsButton);

        deviceSelectorPanel.add(refreshButton);
        deviceSelectorPanel.add(deviceLabel);
        deviceSelectorPanel.add(deviceSelector);

        topBar.add(navigationTabsPanel, BorderLayout.WEST);
        topBar.add(deviceSelectorPanel, BorderLayout.EAST);

        getContentPane().add(topBar, BorderLayout.NORTH);
    }

    private void addContent() {
        contentPanel.add(homePanel, HOME_TAB);
        contentPanel.add(settingsPanel, SETTINGS_TAB);
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        showHomeScreen();
    }

    private void configureNavigationButton(JToggleButton button, String tooltip, ToolbarIcon.Type iconType) {
        button.putClientProperty("iconType", iconType);
        button.setFocusable(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        button.setPreferredSize(new Dimension(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT));
        button.setToolTipText(tooltip);
        button.setText("");
        button.addChangeListener(event -> updateNavigationStyles());
    }

    private void configureRefreshButton() {
        refreshButton.setFocusable(false);
        refreshButton.setFocusPainted(false);
        refreshButton.setToolTipText("Refrescar dispositivos");
        refreshButton.setPreferredSize(new Dimension(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT));
        refreshButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
    }

    private void updateNavigationStyles() {
        styleNavigationButton(homeButton);
        styleNavigationButton(settingsButton);
    }

    private void styleNavigationButton(JToggleButton button) {
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(currentTheme.surface());
        button.setForeground(button.isSelected() ? currentTheme.actionBackground() : currentTheme.textSecondary());
        button.setBorder(BorderFactory.createMatteBorder(
                0,
                0,
                3,
                0,
                button.isSelected() ? currentTheme.actionBackground() : currentTheme.surface()));
        button.setIcon(new ToolbarIcon(
                (ToolbarIcon.Type) button.getClientProperty("iconType"),
                18,
                button.getForeground()));
    }

    private void styleRefreshButton() {
        refreshButton.setOpaque(true);
        refreshButton.setContentAreaFilled(true);
        refreshButton.setBackground(currentTheme.surface());
        refreshButton.setForeground(refreshButton.isEnabled() ? currentTheme.textPrimary() : currentTheme.textSecondary());
        refreshButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        refreshButton.setIcon(new ToolbarIcon(ToolbarIcon.Type.REFRESH, 18, refreshButton.getForeground()));
    }

    private String defaultScreenshotName() {
        return "adb-captura-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".png";
    }
}
