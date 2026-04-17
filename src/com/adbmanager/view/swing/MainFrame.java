package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;

import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.logic.model.DevicePowerAction;
import com.adbmanager.logic.model.AppDetails;
import com.adbmanager.logic.model.InstalledApp;
import com.adbmanager.logic.model.ScrcpyCamera;
import com.adbmanager.logic.model.ScrcpyLaunchRequest;
import com.adbmanager.logic.model.ScrcpyStatus;
import com.adbmanager.logic.model.SystemState;
import com.adbmanager.view.Messages;
import com.adbmanager.view.Messages.Language;

public class MainFrame extends JFrame {

    private static final String HOME_TAB = "home";
    private static final String DISPLAY_TAB = "display";
    private static final String APPS_TAB = "apps";
    private static final String SYSTEM_TAB = "system";
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
    private final JLabel deviceLabel = new JLabel();
    private final JToggleButton homeButton = new JToggleButton();
    private final JToggleButton displayButton = new JToggleButton();
    private final JToggleButton appsButton = new JToggleButton();
    private final JToggleButton systemButton = new JToggleButton();
    private final JToggleButton settingsButton = new JToggleButton();
    private final JButton powerButton = new JButton();
    private final JButton wirelessButton = new JButton("+");
    private final JButton refreshButton = new JButton();
    private final JPopupMenu powerMenu = new JPopupMenu();
    private final Map<DevicePowerAction, JMenuItem> powerMenuItems = new LinkedHashMap<>();
    private final HomePanel homePanel = new HomePanel();
    private final DisplayPanel displayPanel = new DisplayPanel();
    private final AppsPanel appsPanel = new AppsPanel();
    private final SystemPanel systemPanel = new SystemPanel();
    private final SettingsPanel settingsPanel = new SettingsPanel();
    private final WirelessConnectionDialog wirelessDialog = new WirelessConnectionDialog(this);
    private final AppInstallDialog appInstallDialog = new AppInstallDialog(this);
    private AppTheme currentTheme = AppTheme.LIGHT;

    public MainFrame() {
        super();
        buildFrame();
        setLanguage(Messages.getLanguage());
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

    public void setDisplayAction(ActionListener actionListener) {
        displayButton.addActionListener(actionListener);
    }

    public void setApplyDisplayAction(ActionListener actionListener) {
        displayPanel.setApplyDisplayAction(actionListener);
    }

    public void setResetDisplayAction(ActionListener actionListener) {
        displayPanel.setResetDisplayAction(actionListener);
    }

    public void setDeviceDarkModeAction(ActionListener actionListener) {
        displayPanel.setDeviceDarkModeAction(actionListener);
    }

    public void setPrepareScrcpyAction(ActionListener actionListener) {
        displayPanel.setPrepareScrcpyAction(actionListener);
    }

    public void setLaunchScrcpyAction(ActionListener actionListener) {
        displayPanel.setLaunchScrcpyAction(actionListener);
    }

    public void setBrowseScrcpyRecordPathAction(ActionListener actionListener) {
        displayPanel.setBrowseScrcpyRecordPathAction(actionListener);
    }

    public void setRefreshScrcpyCamerasAction(ActionListener actionListener) {
        displayPanel.setRefreshScrcpyCamerasAction(actionListener);
    }

    public void setScrcpyLaunchTargetChangeAction(ActionListener actionListener) {
        displayPanel.setScrcpyLaunchTargetChangeAction(actionListener);
    }

    public void setScrcpyStartAppToggleAction(ActionListener actionListener) {
        displayPanel.setScrcpyStartAppToggleAction(actionListener);
    }

    public void setAppsAction(ActionListener actionListener) {
        appsButton.addActionListener(actionListener);
    }

    public void setSystemAction(ActionListener actionListener) {
        systemButton.addActionListener(actionListener);
    }

    public void setRefreshAction(ActionListener actionListener) {
        refreshButton.addActionListener(actionListener);
    }

    public void setWirelessAssistantAction(ActionListener actionListener) {
        wirelessButton.addActionListener(actionListener);
    }

    public void setPowerAction(ActionListener actionListener) {
        ActionListener safeAction = actionListener == null ? event -> {
        } : actionListener;
        for (Map.Entry<DevicePowerAction, JMenuItem> entry : powerMenuItems.entrySet()) {
            JMenuItem menuItem = entry.getValue();
            for (ActionListener existingListener : menuItem.getActionListeners()) {
                menuItem.removeActionListener(existingListener);
            }
            menuItem.addActionListener(safeAction);
        }
    }

    public void setThemeChangeAction(ActionListener actionListener) {
        settingsPanel.setThemeChangeAction(actionListener);
    }

    public void setLanguageChangeAction(ActionListener actionListener) {
        settingsPanel.setLanguageChangeAction(actionListener);
    }

    public void setAutoRefreshOnFocusChangeAction(ActionListener actionListener) {
        settingsPanel.setAutoRefreshOnFocusChangeAction(actionListener);
    }

    public void setUseCustomAdbPathChangeAction(ActionListener actionListener) {
        settingsPanel.setUseCustomAdbPathChangeAction(actionListener);
    }

    public void setCustomAdbPathBrowseAction(ActionListener actionListener) {
        settingsPanel.setCustomAdbPathBrowseAction(actionListener);
    }

    public void setCustomAdbPathCommitAction(ActionListener actionListener) {
        settingsPanel.setCustomAdbPathCommitAction(actionListener);
    }

    public void setRepositoryAction(ActionListener actionListener) {
        settingsPanel.setRepositoryAction(actionListener);
    }

    public void setScrcpyRepositoryAction(ActionListener actionListener) {
        settingsPanel.setScrcpyRepositoryAction(actionListener);
    }

    public void setDeviceCatalogAction(ActionListener actionListener) {
        settingsPanel.setDeviceCatalogAction(actionListener);
    }

    public WirelessConnectionDialog getWirelessConnectionDialog() {
        return wirelessDialog;
    }

    public AppInstallDialog getAppInstallDialog() {
        return appInstallDialog;
    }

    public void setApplicationSelectionAction(Runnable action) {
        appsPanel.setApplicationSelectionAction(action);
    }

    public void setApplicationsViewportChangeAction(Runnable action) {
        appsPanel.setVisibleApplicationsChangedAction(action);
    }

    public void setApplicationPermissionToggleHandler(AppsPanel.PermissionToggleHandler handler) {
        appsPanel.setPermissionToggleHandler(handler);
    }

    public void setApplicationBackgroundModeChangeHandler(AppsPanel.BackgroundModeChangeHandler handler) {
        appsPanel.setBackgroundModeChangeHandler(handler);
    }

    public void setOpenApplicationAction(ActionListener actionListener) {
        appsPanel.setOpenAction(actionListener);
    }

    public void setStopApplicationAction(ActionListener actionListener) {
        appsPanel.setStopAction(actionListener);
    }

    public void setUninstallApplicationAction(ActionListener actionListener) {
        appsPanel.setUninstallAction(actionListener);
    }

    public void setToggleApplicationEnabledAction(ActionListener actionListener) {
        appsPanel.setToggleEnabledAction(actionListener);
    }

    public void setClearApplicationDataAction(ActionListener actionListener) {
        appsPanel.setClearDataAction(actionListener);
    }

    public void setClearApplicationCacheAction(ActionListener actionListener) {
        appsPanel.setClearCacheAction(actionListener);
    }

    public void setExportApplicationApkAction(ActionListener actionListener) {
        appsPanel.setExportApkAction(actionListener);
    }

    public void setInstallApplicationsAction(ActionListener actionListener) {
        appsPanel.setInstallAction(actionListener);
    }

    public void setRefreshSystemUsersAction(ActionListener actionListener) {
        systemPanel.setRefreshUsersAction(actionListener);
    }

    public void setCreateSystemUserAction(ActionListener actionListener) {
        systemPanel.setCreateUserAction(actionListener);
    }

    public void setSwitchSystemUserAction(ActionListener actionListener) {
        systemPanel.setSwitchUserAction(actionListener);
    }

    public void setDeleteSystemUserAction(ActionListener actionListener) {
        systemPanel.setDeleteUserAction(actionListener);
    }

    public void setApplySystemAppLanguagesAction(ActionListener actionListener) {
        systemPanel.setApplyAppLanguagesAction(actionListener);
    }

    public void setApplySystemGesturesAction(ActionListener actionListener) {
        systemPanel.setApplyGesturesAction(actionListener);
    }

    public void setRefreshSystemKeyboardsAction(ActionListener actionListener) {
        systemPanel.setRefreshKeyboardsAction(actionListener);
    }

    public void setEnableSystemKeyboardAction(ActionListener actionListener) {
        systemPanel.setEnableKeyboardAction(actionListener);
    }

    public void setSetSystemKeyboardAction(ActionListener actionListener) {
        systemPanel.setSetKeyboardAction(actionListener);
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

    public Language getSelectedLanguage() {
        return settingsPanel.getSelectedLanguage();
    }

    public void setSelectedLanguage(Language language) {
        settingsPanel.setSelectedLanguage(language);
    }

    public boolean isAutoRefreshOnFocusSelected() {
        return settingsPanel.isAutoRefreshOnFocusSelected();
    }

    public void setAutoRefreshOnFocusSelected(boolean selected) {
        settingsPanel.setAutoRefreshOnFocusSelected(selected);
    }

    public boolean isUseCustomAdbPathSelected() {
        return settingsPanel.isUseCustomAdbPathSelected();
    }

    public void setUseCustomAdbPathSelected(boolean selected) {
        settingsPanel.setUseCustomAdbPathSelected(selected);
    }

    public String getCustomAdbPath() {
        return settingsPanel.getCustomAdbPath();
    }

    public void setCustomAdbPath(String path) {
        settingsPanel.setCustomAdbPath(path);
    }

    public void setLanguage(Language language) {
        setTitle(Messages.appTitle());
        if (settingsPanel.getSelectedLanguage() != language) {
            setSelectedLanguage(language);
        }

        deviceLabel.setText(Messages.text("main.device.label"));
        homeButton.setToolTipText(Messages.text("navigation.home.tooltip"));
        displayButton.setToolTipText(Messages.text("navigation.display.tooltip"));
        appsButton.setToolTipText(Messages.text("navigation.apps.tooltip"));
        systemButton.setToolTipText(Messages.text("navigation.system.tooltip"));
        settingsButton.setToolTipText(Messages.text("navigation.settings.tooltip"));
        wirelessButton.setToolTipText(Messages.text("navigation.wireless.tooltip"));
        powerButton.setToolTipText(Messages.text("navigation.power.tooltip"));
        refreshButton.setToolTipText(Messages.text("navigation.refresh.tooltip"));
        for (Map.Entry<DevicePowerAction, JMenuItem> entry : powerMenuItems.entrySet()) {
            entry.getValue().setText(Messages.text(entry.getKey().messageKey()));
        }

        homePanel.refreshTexts();
        displayPanel.refreshTexts();
        appsPanel.refreshTexts();
        systemPanel.refreshTexts();
        settingsPanel.refreshTexts();
        wirelessDialog.refreshTexts();
        appInstallDialog.refreshTexts();
        setTheme(currentTheme);
    }

    public void showHomeScreen() {
        cardLayout.show(contentPanel, HOME_TAB);
        homeButton.setSelected(true);
        displayButton.setSelected(false);
        appsButton.setSelected(false);
        systemButton.setSelected(false);
        settingsButton.setSelected(false);
        updateNavigationStyles();
    }

    public void showDisplayScreen() {
        cardLayout.show(contentPanel, DISPLAY_TAB);
        homeButton.setSelected(false);
        displayButton.setSelected(true);
        appsButton.setSelected(false);
        systemButton.setSelected(false);
        settingsButton.setSelected(false);
        updateNavigationStyles();
    }

    public void showAppsScreen() {
        cardLayout.show(contentPanel, APPS_TAB);
        homeButton.setSelected(false);
        displayButton.setSelected(false);
        appsButton.setSelected(true);
        systemButton.setSelected(false);
        settingsButton.setSelected(false);
        updateNavigationStyles();
    }

    public void showSystemScreen() {
        cardLayout.show(contentPanel, SYSTEM_TAB);
        homeButton.setSelected(false);
        displayButton.setSelected(false);
        appsButton.setSelected(false);
        systemButton.setSelected(true);
        settingsButton.setSelected(false);
        updateNavigationStyles();
    }

    public void showSettingsScreen() {
        cardLayout.show(contentPanel, SETTINGS_TAB);
        homeButton.setSelected(false);
        displayButton.setSelected(false);
        appsButton.setSelected(false);
        systemButton.setSelected(false);
        settingsButton.setSelected(true);
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
        displayPanel.setDeviceDetails(details);
    }

    public void clearDeviceDetails() {
        homePanel.clearDeviceDetails();
        displayPanel.clearDeviceDetails();
    }

    public void setApplications(List<InstalledApp> applications, String selectedPackageName) {
        appsPanel.setApplications(applications, selectedPackageName);
    }

    public void setApplicationsLoading(boolean loading, String statusText) {
        appsPanel.setApplicationsLoading(loading, statusText);
    }

    public void updateApplication(InstalledApp application) {
        appsPanel.updateApplication(application);
    }

    public void updateApplications(List<InstalledApp> applications) {
        appsPanel.updateApplications(applications);
    }

    public void clearApplications() {
        appsPanel.clearApplications();
    }

    public String getSelectedApplicationPackage() {
        return appsPanel.getSelectedPackageName();
    }

    public List<String> getVisibleApplicationPackages(int extraRows) {
        return appsPanel.getVisibleApplicationPackages(extraRows);
    }

    public AppDetails getCurrentApplicationDetails() {
        return appsPanel.getCurrentDetails();
    }

    public void setApplicationDetails(AppDetails details) {
        appsPanel.setApplicationDetails(details);
    }

    public void clearApplicationDetails() {
        appsPanel.clearApplicationDetails();
    }

    public void showApplicationDetailsLoading(String packageName) {
        appsPanel.showApplicationDetailsLoading(packageName);
    }

    public Integer getRequestedDisplayWidth() {
        return displayPanel.getRequestedWidth();
    }

    public Integer getRequestedDisplayHeight() {
        return displayPanel.getRequestedHeight();
    }

    public Integer getRequestedDisplayDensity() {
        return displayPanel.getRequestedDensity();
    }

    public Integer getRequestedDisplayScreenOffTimeout() {
        return displayPanel.getRequestedScreenOffTimeout();
    }

    public String getRequestedDisplayScreenOffTimeoutLabel() {
        return displayPanel.getRequestedScreenOffTimeoutLabel();
    }

    public boolean hasRequestedDisplayScreenOffTimeout() {
        return displayPanel.hasRequestedScreenOffTimeout();
    }

    public boolean isDeviceDarkModeSelected() {
        return displayPanel.isDeviceDarkModeSelected();
    }

    public ScrcpyLaunchRequest getScrcpyLaunchRequest() {
        return displayPanel.getScrcpyLaunchRequest();
    }

    public boolean shouldLoadScrcpyApplications() {
        return displayPanel.shouldLoadScrcpyApplications();
    }

    public boolean usesScrcpyCameraSource() {
        return displayPanel.usesScrcpyCameraSource();
    }

    public void setScrcpyStatus(ScrcpyStatus status) {
        displayPanel.setScrcpyStatus(status);
    }

    public void setScrcpyFeedback(String message, boolean error) {
        displayPanel.setScrcpyFeedback(message, error);
    }

    public void setScrcpyBusy(boolean busy) {
        displayPanel.setScrcpyBusy(busy);
    }

    public void setScrcpyDeviceAvailable(boolean available) {
        displayPanel.setScrcpyDeviceAvailable(available);
    }

    public void setScrcpyRecordPath(String path) {
        displayPanel.setScrcpyRecordPath(path);
    }

    public void setScrcpyLaunchRequest(ScrcpyLaunchRequest request) {
        displayPanel.setScrcpyLaunchRequest(request);
    }

    public void setScrcpyAvailableApps(List<InstalledApp> applications) {
        displayPanel.setScrcpyAvailableApps(applications);
    }

    public void setScrcpyAvailableCameras(List<ScrcpyCamera> cameras) {
        displayPanel.setScrcpyAvailableCameras(cameras);
    }

    public void setSystemState(SystemState state) {
        systemPanel.setSystemState(state);
    }

    public void clearSystemState() {
        systemPanel.clearSystemState();
    }

    public void setSystemBusy(boolean busy) {
        systemPanel.setBusy(busy);
    }

    public boolean isSelectedSystemKeyboardEnabled() {
        return systemPanel.isSelectedKeyboardEnabled();
    }

    public void setSystemDeviceAvailable(boolean available) {
        systemPanel.setDeviceAvailable(available);
    }

    public void setSystemStatus(String message, boolean error) {
        systemPanel.setStatus(message, error);
    }

    public Integer getSelectedSystemUserId() {
        return systemPanel.getSelectedUserId();
    }

    public String getNewSystemUserName() {
        return systemPanel.getNewUserName();
    }

    public boolean isShowAllAppLanguagesSelected() {
        return systemPanel.isShowAllAppLanguagesSelected();
    }

    public boolean isGesturalNavigationSelected() {
        return systemPanel.isGesturalNavigationSelected();
    }

    public String getSelectedKeyboardId() {
        return systemPanel.getSelectedKeyboardId();
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
        fileChooser.setDialogTitle(Messages.text("filechooser.saveScreenshot.title"));
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                Messages.text("filechooser.saveScreenshot.filter"),
                "png"));
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

    public File chooseApkDestination(String defaultFileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(Messages.text("filechooser.saveApk.title"));
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                Messages.text("filechooser.saveApk.filter"),
                "apk"));
        fileChooser.setSelectedFile(new File(defaultFileName));

        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File selectedFile = fileChooser.getSelectedFile();
        if (!selectedFile.getName().toLowerCase().endsWith(".apk")) {
            File parentDirectory = selectedFile.getParentFile();
            selectedFile = parentDirectory == null
                    ? new File(selectedFile.getName() + ".apk")
                    : new File(parentDirectory, selectedFile.getName() + ".apk");
        }

        return selectedFile;
    }

    public File chooseScrcpyRecordingDestination() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(Messages.text("filechooser.saveRecording.title"));
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                Messages.text("filechooser.saveRecording.filter"),
                "mp4",
                "mkv"));
        fileChooser.setSelectedFile(new File(defaultRecordingName()));

        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File selectedFile = fileChooser.getSelectedFile();
        String lowerName = selectedFile.getName().toLowerCase();
        if (!lowerName.endsWith(".mp4") && !lowerName.endsWith(".mkv")) {
            File parentDirectory = selectedFile.getParentFile();
            selectedFile = parentDirectory == null
                    ? new File(selectedFile.getName() + ".mp4")
                    : new File(parentDirectory, selectedFile.getName() + ".mp4");
        }

        return selectedFile;
    }

    public File chooseAdbExecutable() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(Messages.text("filechooser.adb.title"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setAcceptAllFileFilterUsed(true);

        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        return fileChooser.getSelectedFile();
    }

    public void setCaptureEnabled(boolean enabled) {
        homePanel.setCaptureEnabled(enabled);
    }

    public void setSaveCaptureEnabled(boolean enabled) {
        homePanel.setSaveCaptureEnabled(enabled);
    }

    public void setDeviceSelectorEnabled(boolean enabled) {
        deviceSelector.setEnabled(enabled);
        deviceSelector.repaint();
    }

    public void setPowerActionsEnabled(boolean enabled) {
        powerButton.setEnabled(enabled);
        for (JMenuItem menuItem : powerMenuItems.values()) {
            menuItem.setEnabled(enabled);
        }
        stylePowerButton();
    }

    public void setRefreshEnabled(boolean enabled) {
        refreshButton.setEnabled(enabled);
        styleRefreshButton();
    }

    public void setApplicationsEnabled(boolean enabled) {
        appsPanel.setApplicationsEnabled(enabled);
    }

    public void setApplicationActionsEnabled(boolean enabled) {
        appsPanel.setApplicationActionsEnabled(enabled);
    }

    public void setDisplayControlsEnabled(boolean enabled) {
        displayPanel.setDisplayControlsEnabled(enabled);
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

        ThemedComboBoxUI.apply(deviceSelector, theme);
        deviceSelector.setPreferredSize(new Dimension(360, 38));
        deviceSelector.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        deviceSelector.setUI(new ThemedComboBoxUI(theme));
        deviceRenderer.applyTheme(theme);

        homePanel.applyTheme(theme);
        displayPanel.applyTheme(theme);
        appsPanel.applyTheme(theme);
        systemPanel.applyTheme(theme);
        settingsPanel.applyTheme(theme);
        wirelessDialog.applyTheme(theme);
        appInstallDialog.applyTheme(theme);
        styleNavigationButton(homeButton);
        styleNavigationButton(displayButton);
        styleNavigationButton(appsButton);
        styleNavigationButton(systemButton);
        styleNavigationButton(settingsButton);
        stylePowerButton();
        styleWirelessButton();
        styleRefreshButton();
        stylePowerMenu();

        revalidate();
        repaint();
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                Messages.text("dialog.info.title"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                Messages.text("dialog.error.title"),
                JOptionPane.ERROR_MESSAGE);
    }

    public boolean confirmAction(String title, String message) {
        return JOptionPane.showConfirmDialog(
                this,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public boolean isAppsScreenVisible() {
        return appsButton.isSelected();
    }

    public boolean isDisplayScreenVisible() {
        return displayButton.isSelected();
    }

    public boolean isSystemScreenVisible() {
        return systemButton.isSelected();
    }

    private void buildFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1240, 820));
        setSize(new Dimension(1440, 900));
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());

        addTopBar();
        addContent();
        installTabNavigationShortcuts();
    }

    private void addTopBar() {
        configureNavigationButton(homeButton, ToolbarIcon.Type.HOME);
        configureNavigationButton(displayButton, ToolbarIcon.Type.DISPLAY);
        configureNavigationButton(appsButton, ToolbarIcon.Type.APPS);
        configureNavigationButton(systemButton, ToolbarIcon.Type.SYSTEM);
        configureNavigationButton(settingsButton, ToolbarIcon.Type.SETTINGS);
        configurePowerButton();
        configureWirelessButton();
        configureRefreshButton();
        buildPowerMenu();

        navigationGroup.add(homeButton);
        navigationGroup.add(displayButton);
        navigationGroup.add(appsButton);
        navigationGroup.add(systemButton);
        navigationGroup.add(settingsButton);
        navigationTabsPanel.add(homeButton);
        navigationTabsPanel.add(displayButton);
        navigationTabsPanel.add(appsButton);
        navigationTabsPanel.add(systemButton);
        navigationTabsPanel.add(settingsButton);

        deviceSelectorPanel.add(deviceLabel);
        deviceSelectorPanel.add(deviceSelector);
        deviceSelectorPanel.add(powerButton);
        deviceSelectorPanel.add(wirelessButton);
        deviceSelectorPanel.add(refreshButton);

        topBar.add(navigationTabsPanel, BorderLayout.WEST);
        topBar.add(deviceSelectorPanel, BorderLayout.EAST);

        getContentPane().add(topBar, BorderLayout.NORTH);
    }

    private void addContent() {
        contentPanel.add(homePanel, HOME_TAB);
        contentPanel.add(displayPanel, DISPLAY_TAB);
        contentPanel.add(appsPanel, APPS_TAB);
        contentPanel.add(systemPanel, SYSTEM_TAB);
        contentPanel.add(settingsPanel, SETTINGS_TAB);
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        showHomeScreen();
    }

    private void configureNavigationButton(JToggleButton button, ToolbarIcon.Type iconType) {
        button.putClientProperty("iconType", iconType);
        button.setUI(new BasicToggleButtonUI());
        button.setFocusable(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        button.setPreferredSize(new Dimension(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT));
        button.setText("");
        button.addChangeListener(event -> updateNavigationStyles());
    }

    private void configureRefreshButton() {
        refreshButton.setUI(new BasicButtonUI());
        refreshButton.setFocusable(false);
        refreshButton.setFocusPainted(false);
        refreshButton.setRolloverEnabled(true);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.setPreferredSize(new Dimension(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT));
        refreshButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        refreshButton.getModel().addChangeListener(event -> styleRefreshButton());

        deviceSelector.setFocusable(false);
        deviceSelector.setMaximumRowCount(12);
    }

    private void configurePowerButton() {
        powerButton.setUI(new BasicButtonUI());
        powerButton.setFocusable(false);
        powerButton.setFocusPainted(false);
        powerButton.setRolloverEnabled(true);
        powerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        powerButton.setPreferredSize(new Dimension(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT));
        powerButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        powerButton.getModel().addChangeListener(event -> stylePowerButton());
        powerButton.addActionListener(event -> {
            if (powerButton.isEnabled()) {
                powerMenu.show(powerButton, 0, powerButton.getHeight());
            }
        });
    }

    private void configureWirelessButton() {
        wirelessButton.setUI(new BasicButtonUI());
        wirelessButton.setFocusable(false);
        wirelessButton.setFocusPainted(false);
        wirelessButton.setRolloverEnabled(true);
        wirelessButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        wirelessButton.setPreferredSize(new Dimension(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT));
        wirelessButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        wirelessButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        wirelessButton.getModel().addChangeListener(event -> styleWirelessButton());
    }

    private void buildPowerMenu() {
        powerMenu.removeAll();
        powerMenuItems.clear();
        for (DevicePowerAction action : DevicePowerAction.values()) {
            JMenuItem menuItem = new JMenuItem(Messages.text(action.messageKey()));
            menuItem.setActionCommand(action.actionCommand());
            powerMenu.add(menuItem);
            powerMenuItems.put(action, menuItem);
        }
    }

    private void updateNavigationStyles() {
        styleNavigationButton(homeButton);
        styleNavigationButton(displayButton);
        styleNavigationButton(appsButton);
        styleNavigationButton(systemButton);
        styleNavigationButton(settingsButton);
    }

    private void styleNavigationButton(JToggleButton button) {
        boolean selected = button.isSelected();
        boolean hovered = button.getModel().isRollover() && button.isEnabled();
        java.awt.Color baseBackground = selected ? currentTheme.secondarySurface() : currentTheme.surface();
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(hovered && !selected
                ? ThemeUtils.blend(baseBackground, currentTheme.selectionBackground(), 0.22d)
                : baseBackground);
        button.setForeground(selected ? currentTheme.actionBackground() : currentTheme.textSecondary());
        button.setBorder(BorderFactory.createMatteBorder(
                0,
                0,
                3,
                0,
                selected ? currentTheme.actionBackground() : button.getBackground()));
        button.setIcon(new ToolbarIcon(
                (ToolbarIcon.Type) button.getClientProperty("iconType"),
                20,
                button.getForeground()));
    }

    private void styleRefreshButton() {
        boolean hovered = refreshButton.getModel().isRollover() && refreshButton.isEnabled();
        refreshButton.setOpaque(true);
        refreshButton.setContentAreaFilled(true);
        refreshButton.setBackground(hovered
                ? ThemeUtils.blend(currentTheme.surface(), currentTheme.selectionBackground(), 0.24d)
                : currentTheme.surface());
        refreshButton.setForeground(refreshButton.isEnabled()
                ? currentTheme.actionBackground()
                : currentTheme.textSecondary());
        refreshButton.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, refreshButton.getBackground()));
        refreshButton.setIcon(new ToolbarIcon(ToolbarIcon.Type.REFRESH, 20, refreshButton.getForeground()));
    }

    private void stylePowerButton() {
        boolean hovered = powerButton.getModel().isRollover() && powerButton.isEnabled();
        powerButton.setOpaque(true);
        powerButton.setContentAreaFilled(true);
        powerButton.setBackground(hovered
                ? ThemeUtils.blend(currentTheme.surface(), currentTheme.selectionBackground(), 0.24d)
                : currentTheme.surface());
        powerButton.setForeground(powerButton.isEnabled()
                ? currentTheme.actionBackground()
                : currentTheme.textSecondary());
        powerButton.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, powerButton.getBackground()));
        powerButton.setIcon(new ToolbarIcon(ToolbarIcon.Type.POWER, 20, powerButton.getForeground()));
    }

    private void styleWirelessButton() {
        boolean hovered = wirelessButton.getModel().isRollover() && wirelessButton.isEnabled();
        wirelessButton.setOpaque(true);
        wirelessButton.setContentAreaFilled(true);
        wirelessButton.setBackground(hovered
                ? ThemeUtils.blend(currentTheme.surface(), currentTheme.selectionBackground(), 0.24d)
                : currentTheme.surface());
        wirelessButton.setForeground(wirelessButton.isEnabled()
                ? currentTheme.actionBackground()
                : currentTheme.textSecondary());
        wirelessButton.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, wirelessButton.getBackground()));
        wirelessButton.setText("+");
    }

    private void stylePowerMenu() {
        powerMenu.setBorder(BorderFactory.createLineBorder(currentTheme.border(), 1));
        powerMenu.setBackground(currentTheme.surface());
        for (JMenuItem menuItem : powerMenuItems.values()) {
            menuItem.setOpaque(true);
            menuItem.setBackground(currentTheme.surface());
            menuItem.setForeground(menuItem.isEnabled() ? currentTheme.textPrimary() : currentTheme.textSecondary());
            menuItem.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            menuItem.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        }
    }

    private void installTabNavigationShortcuts() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK), "tabs.next");
        inputMap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "tabs.previous");

        actionMap.put("tabs.next", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                cycleTabs(1);
            }
        });
        actionMap.put("tabs.previous", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                cycleTabs(-1);
            }
        });
    }

    private void cycleTabs(int delta) {
        int nextIndex = Math.floorMod(currentTabIndex() + delta, 5);
        switch (nextIndex) {
            case 0 -> showHomeScreen();
            case 1 -> showDisplayScreen();
            case 2 -> showAppsScreen();
            case 3 -> showSystemScreen();
            case 4 -> showSettingsScreen();
            default -> showHomeScreen();
        }
    }

    private int currentTabIndex() {
        if (displayButton.isSelected()) {
            return 1;
        }
        if (appsButton.isSelected()) {
            return 2;
        }
        if (systemButton.isSelected()) {
            return 3;
        }
        if (settingsButton.isSelected()) {
            return 4;
        }
        return 0;
    }

    private String defaultScreenshotName() {
        return Messages.text("filechooser.screenshot.prefix")
                + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + ".png";
    }

    private String defaultRecordingName() {
        return Messages.text("filechooser.recording.prefix")
                + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + ".mp4";
    }
}
