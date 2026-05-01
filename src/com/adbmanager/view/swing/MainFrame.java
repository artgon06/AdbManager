package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicToggleButtonUI;

import com.adbmanager.logic.model.AppDetails;
import com.adbmanager.logic.model.ControlState;
import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.logic.model.DeviceDirectoryListing;
import com.adbmanager.logic.model.DeviceFileEntry;
import com.adbmanager.logic.model.DevicePowerAction;
import com.adbmanager.logic.model.DeviceSoundMode;
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
    private static final String MIRRORING_TAB = "mirroring";
    private static final String CONTROL_TAB = "control";
    private static final String APPS_TAB = "apps";
    private static final String FILES_TAB = "files";
    private static final String SYSTEM_TAB = "system";
    private static final String SETTINGS_TAB = "settings";
    private static final int TOP_BAR_HEIGHT = 56;
    private static final int SIDE_BAR_WIDTH = 172;
    private static final int SIDE_BAR_COLLAPSED_WIDTH = 64;

    private final java.awt.CardLayout cardLayout = new java.awt.CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JPanel topBar = new JPanel(new BorderLayout());
    private final JPanel sideBar = new JPanel(new BorderLayout());
    private final JPanel navigationTabsPanel = new JPanel();
    private final JPanel settingsNavigationPanel = new JPanel(new BorderLayout());
    private final JPanel deviceSelectorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
    private final JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
    private final JPanel topBarRightSpacer = new JPanel();
    private final JLabel appTitleLabel = new JLabel();
    private final JButton sidebarToggleButton = new JButton();
    private final ButtonGroup navigationGroup = new ButtonGroup();
    private final DeviceComboBoxRenderer deviceRenderer = new DeviceComboBoxRenderer();
    private final JComboBox<Device> deviceSelector = new JComboBox<>();
    private final JLabel deviceLabel = new JLabel();
    private final JToggleButton homeButton = new JToggleButton();
    private final JToggleButton displayButton = new JToggleButton();
    private final JToggleButton mirroringButton = new JToggleButton();
    private final JToggleButton controlButton = new JToggleButton();
    private final JToggleButton appsButton = new JToggleButton();
    private final JToggleButton filesButton = new JToggleButton();
    private final JToggleButton systemButton = new JToggleButton();
    private final JToggleButton settingsButton = new JToggleButton();
    private final JButton wirelessButton = new JButton("+");
    private final JButton tcpipButton = new JButton();
    private final JButton refreshButton = new JButton();
    private final JPopupMenu powerMenu = new JPopupMenu();
    private final Map<DevicePowerAction, JMenuItem> powerMenuItems = new LinkedHashMap<>();
    private final HomePanel homePanel = new HomePanel();
    private final DisplayPanel displayPanel = new DisplayPanel();
    private final MirroringPanel mirroringPanel = new MirroringPanel(displayPanel.getScrcpyLauncherPanel());
    private final ControlPanel controlPanel = new ControlPanel();
    private final AppsPanel appsPanel = new AppsPanel();
    private final FilesPanel filesPanel = new FilesPanel();
    private final SystemPanel systemPanel = new SystemPanel();
    private final SettingsPanel settingsPanel = new SettingsPanel();
    private final WirelessConnectionDialog wirelessDialog = new WirelessConnectionDialog(this);
    private final AppInstallDialog appInstallDialog = new AppInstallDialog(this);
    private final KeyEventDispatcher tabNavigationDispatcher = event -> {
        if (!isActive() || event.getID() != KeyEvent.KEY_PRESSED || event.getKeyCode() != KeyEvent.VK_TAB) {
            return false;
        }
        if ((event.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0) {
            return false;
        }

        cycleTabs((event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0 ? -1 : 1);
        return true;
    };
    private AppTheme currentTheme = AppTheme.LIGHT;
    private boolean sidebarCollapsed;
    private int sidebarCurrentWidth = SIDE_BAR_WIDTH;
    private Timer sidebarAnimationTimer;

    public MainFrame() {
        super();
        buildFrame();
        setLanguage(Messages.getLanguage());
        setTheme(AppTheme.DARK);
    }

    public void showWindow() {
        setVisible(true);
    }

    @Override
    public void dispose() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(tabNavigationDispatcher);
        super.dispose();
    }

    public void setCaptureAction(ActionListener actionListener) {
        homePanel.setCaptureAction(actionListener);
    }

    public void setPowerButtonAction(ActionListener actionListener) {
        homePanel.setPowerButtonAction(actionListener);
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

    public void setMirroringAction(ActionListener actionListener) {
        mirroringButton.addActionListener(actionListener);
    }

    public void setControlAction(ActionListener actionListener) {
        controlButton.addActionListener(actionListener);
    }

    public void setRefreshControlAction(ActionListener actionListener) {
        controlPanel.setRefreshAction(actionListener);
    }

    public void setQuickControlKeyEventAction(ActionListener actionListener) {
        controlPanel.setQuickKeyEventAction(actionListener);
    }

    public void setControlTextAction(ActionListener actionListener) {
        controlPanel.setSendTextAction(actionListener);
    }

    public void setControlBrightnessAction(ActionListener actionListener) {
        controlPanel.setApplyBrightnessAction(actionListener);
    }

    public void setControlVolumeAction(ActionListener actionListener) {
        controlPanel.setApplyVolumeAction(actionListener);
    }

    public void setControlSoundModeAction(ActionListener actionListener) {
        controlPanel.setApplySoundModeAction(actionListener);
    }

    public void setControlTapAction(ActionListener actionListener) {
        controlPanel.setTapAction(actionListener);
    }

    public void setControlSwipeAction(ActionListener actionListener) {
        controlPanel.setSwipeAction(actionListener);
    }

    public void setControlKeyEventAction(ActionListener actionListener) {
        controlPanel.setKeyEventAction(actionListener);
    }

    public void setControlRawInputAction(ActionListener actionListener) {
        controlPanel.setRawInputAction(actionListener);
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

    public void setFilesAction(ActionListener actionListener) {
        filesButton.addActionListener(actionListener);
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

    public void setTcpipAction(ActionListener actionListener) {
        tcpipButton.addActionListener(actionListener);
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

    public void setFilesNavigateUpAction(ActionListener actionListener) {
        filesPanel.setNavigateUpAction(actionListener);
    }

    public void setFilesRefreshAction(ActionListener actionListener) {
        filesPanel.setRefreshAction(actionListener);
    }

    public void setFilesPathSubmitAction(ActionListener actionListener) {
        filesPanel.setPathSubmitAction(actionListener);
    }

    public void setFilesTransferCancelAction(ActionListener actionListener) {
        filesPanel.setTransferCancelAction(actionListener);
    }

    public void setFilesCreateFolderAction(ActionListener actionListener) {
        filesPanel.setCreateFolderAction(actionListener);
    }

    public void setFilesUploadAction(ActionListener actionListener) {
        filesPanel.setUploadAction(actionListener);
    }

    public void setFilesDownloadAction(ActionListener actionListener) {
        filesPanel.setDownloadAction(actionListener);
    }

    public void setFilesRenameAction(ActionListener actionListener) {
        filesPanel.setRenameAction(actionListener);
    }

    public void setFilesCopyAction(ActionListener actionListener) {
        filesPanel.setCopyAction(actionListener);
    }

    public void setFilesDeleteAction(ActionListener actionListener) {
        filesPanel.setDeleteAction(actionListener);
    }

    public void setFilesSelectionAction(Runnable action) {
        filesPanel.setSelectionAction(action);
    }

    public void setFilesOpenDirectoryAction(Runnable action) {
        filesPanel.setOpenDirectoryAction(action);
    }

    public void setFilesDropHandler(FilesPanel.FileDropHandler handler) {
        filesPanel.setFileDropHandler(handler);
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
        appTitleLabel.setText(Messages.appName());
        if (settingsPanel.getSelectedLanguage() != language) {
            setSelectedLanguage(language);
        }

        deviceLabel.setText(Messages.text("main.device.label"));
        refreshNavigationTexts();
        homeButton.setToolTipText(Messages.text("navigation.home.tooltip"));
        displayButton.setToolTipText(Messages.text("navigation.display.tooltip"));
        mirroringButton.setToolTipText(Messages.text("navigation.mirroring.tooltip"));
        controlButton.setToolTipText(Messages.text("navigation.control.tooltip"));
        appsButton.setToolTipText(Messages.text("navigation.apps.tooltip"));
        filesButton.setToolTipText(Messages.text("navigation.files.tooltip"));
        systemButton.setToolTipText(Messages.text("navigation.system.tooltip"));
        settingsButton.setToolTipText(Messages.text("navigation.settings.tooltip"));
        wirelessButton.setToolTipText(Messages.text("navigation.wireless.tooltip"));
        tcpipButton.setToolTipText(Messages.text("navigation.tcpip.tooltip"));
        refreshButton.setToolTipText(Messages.text("navigation.refresh.tooltip"));
        for (Map.Entry<DevicePowerAction, JMenuItem> entry : powerMenuItems.entrySet()) {
            entry.getValue().setText(Messages.text(entry.getKey().messageKey()));
        }

        homePanel.refreshTexts();
        displayPanel.refreshTexts();
        mirroringPanel.refreshTexts();
        controlPanel.refreshTexts();
        appsPanel.refreshTexts();
        filesPanel.refreshTexts();
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
        mirroringButton.setSelected(false);
        controlButton.setSelected(false);
        appsButton.setSelected(false);
        filesButton.setSelected(false);
        systemButton.setSelected(false);
        settingsButton.setSelected(false);
        updateNavigationStyles();
    }

    public void showDisplayScreen() {
        cardLayout.show(contentPanel, DISPLAY_TAB);
        homeButton.setSelected(false);
        displayButton.setSelected(true);
        mirroringButton.setSelected(false);
        controlButton.setSelected(false);
        appsButton.setSelected(false);
        filesButton.setSelected(false);
        systemButton.setSelected(false);
        settingsButton.setSelected(false);
        updateNavigationStyles();
    }

    public void showMirroringScreen() {
        cardLayout.show(contentPanel, MIRRORING_TAB);
        homeButton.setSelected(false);
        displayButton.setSelected(false);
        mirroringButton.setSelected(true);
        controlButton.setSelected(false);
        appsButton.setSelected(false);
        filesButton.setSelected(false);
        systemButton.setSelected(false);
        settingsButton.setSelected(false);
        updateNavigationStyles();
    }

    public void showControlScreen() {
        cardLayout.show(contentPanel, CONTROL_TAB);
        homeButton.setSelected(false);
        displayButton.setSelected(false);
        mirroringButton.setSelected(false);
        controlButton.setSelected(true);
        appsButton.setSelected(false);
        filesButton.setSelected(false);
        systemButton.setSelected(false);
        settingsButton.setSelected(false);
        updateNavigationStyles();
    }

    public void showAppsScreen() {
        cardLayout.show(contentPanel, APPS_TAB);
        homeButton.setSelected(false);
        displayButton.setSelected(false);
        mirroringButton.setSelected(false);
        controlButton.setSelected(false);
        appsButton.setSelected(true);
        filesButton.setSelected(false);
        systemButton.setSelected(false);
        settingsButton.setSelected(false);
        updateNavigationStyles();
    }

    public void showFilesScreen() {
        cardLayout.show(contentPanel, FILES_TAB);
        homeButton.setSelected(false);
        displayButton.setSelected(false);
        mirroringButton.setSelected(false);
        controlButton.setSelected(false);
        appsButton.setSelected(false);
        filesButton.setSelected(true);
        systemButton.setSelected(false);
        settingsButton.setSelected(false);
        updateNavigationStyles();
    }

    public void showSystemScreen() {
        cardLayout.show(contentPanel, SYSTEM_TAB);
        homeButton.setSelected(false);
        displayButton.setSelected(false);
        mirroringButton.setSelected(false);
        controlButton.setSelected(false);
        appsButton.setSelected(false);
        filesButton.setSelected(false);
        systemButton.setSelected(true);
        settingsButton.setSelected(false);
        updateNavigationStyles();
    }

    public void showSettingsScreen() {
        cardLayout.show(contentPanel, SETTINGS_TAB);
        homeButton.setSelected(false);
        displayButton.setSelected(false);
        mirroringButton.setSelected(false);
        controlButton.setSelected(false);
        appsButton.setSelected(false);
        filesButton.setSelected(false);
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

    public String getControlTextInput() {
        return controlPanel.getTextInputValue();
    }

    public int getControlBrightness() {
        return controlPanel.getBrightnessValue();
    }

    public int getControlVolume() {
        return controlPanel.getVolumeValue();
    }

    public DeviceSoundMode getControlSoundMode() {
        return controlPanel.getSelectedSoundMode();
    }

    public Integer getControlTapX() {
        return controlPanel.getTapX();
    }

    public Integer getControlTapY() {
        return controlPanel.getTapY();
    }

    public Integer getControlSwipeX1() {
        return controlPanel.getSwipeX1();
    }

    public Integer getControlSwipeY1() {
        return controlPanel.getSwipeY1();
    }

    public Integer getControlSwipeX2() {
        return controlPanel.getSwipeX2();
    }

    public Integer getControlSwipeY2() {
        return controlPanel.getSwipeY2();
    }

    public Integer getControlSwipeDurationMs() {
        return controlPanel.getSwipeDurationMs();
    }

    public String getControlManualKeyEvent() {
        return controlPanel.getManualKeyEvent();
    }

    public String getControlRawInputCommand() {
        return controlPanel.getRawInputCommand();
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

    public void setFilesListing(DeviceDirectoryListing listing) {
        filesPanel.setDirectoryListing(listing);
    }

    public void clearFilesListing() {
        filesPanel.clearDirectory();
    }

    public void setFilesBusy(boolean busy) {
        filesPanel.setBusy(busy);
    }

    public void setFilesDeviceAvailable(boolean available) {
        filesPanel.setDeviceAvailable(available);
    }

    public void setFilesStatus(String message, boolean error) {
        filesPanel.setStatus(message, error);
    }

    public void setFilesTransferProgress(boolean visible, boolean indeterminate, int percent, String progressText) {
        filesPanel.setTransferProgress(visible, indeterminate, percent, progressText);
    }

    public void clearFilesTransferProgress() {
        filesPanel.clearTransferProgress();
    }

    public void setFilesTransferCancelable(boolean cancelable) {
        filesPanel.setTransferCancelable(cancelable);
    }

    public String getCurrentFilesDirectory() {
        return filesPanel.getCurrentDirectoryPath();
    }

    public String getEnteredFilesDirectory() {
        return filesPanel.getEnteredDirectoryPath();
    }

    public String getParentFilesDirectory() {
        return filesPanel.getParentDirectoryPath();
    }

    public DeviceFileEntry getSelectedFileEntry() {
        return filesPanel.getSelectedEntry();
    }

    public List<DeviceFileEntry> getSelectedFileEntries() {
        return filesPanel.getSelectedEntries();
    }

    public List<String> getSelectedFilePaths() {
        return filesPanel.getSelectedRemotePaths();
    }

    public void setSystemState(SystemState state) {
        systemPanel.setSystemState(state);
    }

    public void setControlState(ControlState state) {
        controlPanel.setControlState(state);
    }

    public void clearControlState() {
        controlPanel.clearControlState();
    }

    public void setControlDeviceAvailable(boolean available) {
        controlPanel.setDeviceAvailable(available);
    }

    public void setControlBusy(boolean busy) {
        controlPanel.setBusy(busy);
    }

    public void setControlStatus(String message, boolean error) {
        controlPanel.setStatus(message, error);
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

    public List<File> chooseFilesToUpload() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(Messages.text("filechooser.upload.title"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setAcceptAllFileFilterUsed(true);

        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return List.of();
        }

        File[] selectedFiles = fileChooser.getSelectedFiles();
        if (selectedFiles == null || selectedFiles.length == 0) {
            File selectedFile = fileChooser.getSelectedFile();
            return selectedFile == null ? List.of() : List.of(selectedFile);
        }
        return List.of(selectedFiles);
    }

    public File chooseDownloadDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(Messages.text("filechooser.download.title"));
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        return fileChooser.getSelectedFile();
    }

    public String promptText(String title, String message, String initialValue) {
        Object value = JOptionPane.showInputDialog(
                this,
                message,
                title,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                initialValue);
        if (value == null) {
            return null;
        }
        return value.toString();
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
        homePanel.setPowerEnabled(enabled);
        for (JMenuItem menuItem : powerMenuItems.values()) {
            menuItem.setEnabled(enabled);
        }
    }

    public void setRefreshEnabled(boolean enabled) {
        refreshButton.setEnabled(enabled);
        styleRefreshButton();
    }

    public void setTcpipEnabled(boolean enabled) {
        tcpipButton.setEnabled(enabled);
        styleTcpipButton();
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
        sideBar.setBackground(theme.surface());
        sideBar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, theme.border()));
        brandPanel.setBackground(theme.surface());
        topBarRightSpacer.setBackground(theme.surface());
        topBarRightSpacer.setPreferredSize(new Dimension(brandPanel.getPreferredSize().width, TOP_BAR_HEIGHT));
        navigationTabsPanel.setBackground(theme.surface());
        settingsNavigationPanel.setBackground(theme.surface());
        deviceSelectorPanel.setBackground(theme.surface());
        appTitleLabel.setForeground(theme.textPrimary());
        appTitleLabel.setFont(new Font("Inter", Font.BOLD, 16));

        deviceLabel.setForeground(theme.textSecondary());
        deviceLabel.setFont(new Font("Inter", Font.BOLD, 14));

        ThemedComboBoxUI.apply(deviceSelector, theme);
        deviceSelector.setPreferredSize(new Dimension(360, 38));
        deviceSelector.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        deviceSelector.setUI(new ThemedComboBoxUI(theme));
        deviceRenderer.applyTheme(theme);

        homePanel.applyTheme(theme);
        displayPanel.applyTheme(theme);
        mirroringPanel.applyTheme(theme);
        controlPanel.applyTheme(theme);
        appsPanel.applyTheme(theme);
        filesPanel.applyTheme(theme);
        systemPanel.applyTheme(theme);
        settingsPanel.applyTheme(theme);
        wirelessDialog.applyTheme(theme);
        appInstallDialog.applyTheme(theme);
        styleNavigationButton(homeButton);
        styleNavigationButton(displayButton);
        styleNavigationButton(mirroringButton);
        styleNavigationButton(controlButton);
        styleNavigationButton(appsButton);
        styleNavigationButton(filesButton);
        styleNavigationButton(systemButton);
        styleNavigationButton(settingsButton);
        styleTopBarIconButton(sidebarToggleButton);
        styleWirelessButton();
        styleTcpipButton();
        styleRefreshButton();
        stylePowerMenu();
        applySidebarMode();

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

    public boolean isMirroringScreenVisible() {
        return mirroringButton.isSelected();
    }

    public boolean isControlScreenVisible() {
        return controlButton.isSelected();
    }

    public boolean isFilesScreenVisible() {
        return filesButton.isSelected();
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
        configureNavigationButton(mirroringButton, ToolbarIcon.Type.MIRRORING);
        configureNavigationButton(controlButton, ToolbarIcon.Type.CONTROL);
        configureNavigationButton(appsButton, ToolbarIcon.Type.APPS);
        configureNavigationButton(filesButton, ToolbarIcon.Type.FOLDER);
        configureNavigationButton(systemButton, ToolbarIcon.Type.SYSTEM);
        configureNavigationButton(settingsButton, ToolbarIcon.Type.SETTINGS);
        configureTopBarIconButton(sidebarToggleButton, ToolbarIcon.Type.SIDEBAR);
        configureWirelessButton();
        configureTcpipButton();
        configureRefreshButton();
        buildPowerMenu();
        homePanel.setPowerButtonAction(event -> {
            if (event.getSource() instanceof Component source && source.isShowing()) {
                powerMenu.show(source, 0, source.getHeight());
            }
        });

        navigationGroup.add(homeButton);
        navigationGroup.add(displayButton);
        navigationGroup.add(mirroringButton);
        navigationGroup.add(controlButton);
        navigationGroup.add(appsButton);
        navigationGroup.add(filesButton);
        navigationGroup.add(systemButton);
        navigationGroup.add(settingsButton);

        navigationTabsPanel.setLayout(new BoxLayout(navigationTabsPanel, BoxLayout.Y_AXIS));
        navigationTabsPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        navigationTabsPanel.add(homeButton);
        navigationTabsPanel.add(Box.createVerticalStrut(2));
        navigationTabsPanel.add(displayButton);
        navigationTabsPanel.add(Box.createVerticalStrut(2));
        navigationTabsPanel.add(mirroringButton);
        navigationTabsPanel.add(Box.createVerticalStrut(2));
        navigationTabsPanel.add(controlButton);
        navigationTabsPanel.add(Box.createVerticalStrut(2));
        navigationTabsPanel.add(appsButton);
        navigationTabsPanel.add(Box.createVerticalStrut(2));
        navigationTabsPanel.add(filesButton);
        navigationTabsPanel.add(Box.createVerticalStrut(2));
        navigationTabsPanel.add(systemButton);

        settingsNavigationPanel.setOpaque(false);
        settingsNavigationPanel.setBorder(new EmptyBorder(12, 12, 16, 12));
        settingsNavigationPanel.add(settingsButton, BorderLayout.NORTH);

        sideBar.setPreferredSize(new Dimension(SIDE_BAR_WIDTH, 0));
        sideBar.add(navigationTabsPanel, BorderLayout.CENTER);
        sideBar.add(settingsNavigationPanel, BorderLayout.SOUTH);

        sidebarToggleButton.addActionListener(event -> toggleSidebarMode());

        deviceSelectorPanel.add(wirelessButton);
        deviceSelectorPanel.add(deviceSelector);
        deviceSelectorPanel.add(tcpipButton);
        deviceSelectorPanel.add(refreshButton);

        brandPanel.add(sidebarToggleButton);
        brandPanel.add(appTitleLabel);
        topBar.add(brandPanel, BorderLayout.WEST);
        topBar.add(deviceSelectorPanel, BorderLayout.CENTER);
        topBar.add(topBarRightSpacer, BorderLayout.EAST);

        getContentPane().add(topBar, BorderLayout.NORTH);
        getContentPane().add(sideBar, BorderLayout.WEST);
        applySidebarMode();
    }

    private void addContent() {
        contentPanel.add(homePanel, HOME_TAB);
        contentPanel.add(displayPanel, DISPLAY_TAB);
        contentPanel.add(mirroringPanel, MIRRORING_TAB);
        contentPanel.add(controlPanel, CONTROL_TAB);
        contentPanel.add(appsPanel, APPS_TAB);
        contentPanel.add(filesPanel, FILES_TAB);
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
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(10);
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        button.addChangeListener(event -> updateNavigationStyles());
    }

    private void applySidebarMode() {
        sidebarCurrentWidth = sidebarCollapsed ? SIDE_BAR_COLLAPSED_WIDTH : SIDE_BAR_WIDTH;
        applySidebarWidth(sidebarCurrentWidth);
    }

    private void toggleSidebarMode() {
        sidebarCollapsed = !sidebarCollapsed;
        refreshNavigationTexts();
        updateNavigationStyles();
        int targetWidth = sidebarCollapsed ? SIDE_BAR_COLLAPSED_WIDTH : SIDE_BAR_WIDTH;
        if (sidebarAnimationTimer != null && sidebarAnimationTimer.isRunning()) {
            sidebarAnimationTimer.stop();
        }

        sidebarAnimationTimer = new Timer(12, event -> {
            int delta = targetWidth - sidebarCurrentWidth;
            if (Math.abs(delta) <= 2) {
                sidebarCurrentWidth = targetWidth;
                applySidebarWidth(sidebarCurrentWidth);
                ((Timer) event.getSource()).stop();
                return;
            }

            sidebarCurrentWidth += Math.max(2, Math.abs(delta) / 5) * Integer.signum(delta);
            applySidebarWidth(sidebarCurrentWidth);
        });
        sidebarAnimationTimer.start();
    }

    private void applySidebarWidth(int width) {
        sideBar.setPreferredSize(new Dimension(width, 0));
        sideBar.setMinimumSize(new Dimension(width, 0));
        navigationTabsPanel.setBorder(new EmptyBorder(12, sidebarCollapsed ? 9 : 12, 12, sidebarCollapsed ? 9 : 12));
        settingsNavigationPanel.setBorder(new EmptyBorder(12, sidebarCollapsed ? 9 : 12, 16, sidebarCollapsed ? 9 : 12));
        refreshNavigationTexts();
        updateNavigationStyles();
        sideBar.revalidate();
        sideBar.repaint();
    }

    private void refreshNavigationTexts() {
        setNavigationButtonText(homeButton, "navigation.home");
        setNavigationButtonText(displayButton, "navigation.display");
        setNavigationButtonText(mirroringButton, "navigation.mirroring");
        setNavigationButtonText(controlButton, "navigation.control");
        setNavigationButtonText(appsButton, "navigation.apps");
        setNavigationButtonText(filesButton, "navigation.files");
        setNavigationButtonText(systemButton, "navigation.system");
        setNavigationButtonText(settingsButton, "navigation.settings");
    }

    private void setNavigationButtonText(JToggleButton button, String key) {
        button.setText(sidebarCollapsed ? "" : Messages.text(key));
    }

    private void configureTopBarIconButton(JButton button, ToolbarIcon.Type iconType) {
        button.putClientProperty("iconType", iconType);
        button.setFocusable(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(32, 32));
        button.getModel().addChangeListener(event -> styleTopBarIconButton(button));
    }

    private void configureRefreshButton() {
        refreshButton.setFocusable(false);
        refreshButton.setFocusPainted(false);
        refreshButton.setRolloverEnabled(true);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.setPreferredSize(new Dimension(32, 32));
        refreshButton.getModel().addChangeListener(event -> styleRefreshButton());

        deviceSelector.setFocusable(false);
        deviceSelector.setMaximumRowCount(12);
    }

    private void configureWirelessButton() {
        wirelessButton.setFocusable(false);
        wirelessButton.setFocusPainted(false);
        wirelessButton.setRolloverEnabled(true);
        wirelessButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        wirelessButton.setPreferredSize(new Dimension(32, 32));
        wirelessButton.setFont(new Font("Inter", Font.BOLD, 24));
        wirelessButton.getModel().addChangeListener(event -> styleWirelessButton());
    }

    private void configureTcpipButton() {
        tcpipButton.setFocusable(false);
        tcpipButton.setFocusPainted(false);
        tcpipButton.setRolloverEnabled(true);
        tcpipButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tcpipButton.setPreferredSize(new Dimension(32, 32));
        tcpipButton.getModel().addChangeListener(event -> styleTcpipButton());
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
        styleNavigationButton(mirroringButton);
        styleNavigationButton(controlButton);
        styleNavigationButton(appsButton);
        styleNavigationButton(filesButton);
        styleNavigationButton(systemButton);
        styleNavigationButton(settingsButton);
    }

    private void styleNavigationButton(JToggleButton button) {
        boolean selected = button.isSelected();
        boolean hovered = button.getModel().isRollover() && button.isEnabled();
        int width = sidebarCollapsed ? SIDE_BAR_COLLAPSED_WIDTH - 18 : SIDE_BAR_WIDTH - 24;
        Dimension size = new Dimension(width, 34);
        button.setPreferredSize(size);
        button.setMaximumSize(size);
        button.setMinimumSize(size);
        button.setHorizontalAlignment(sidebarCollapsed ? SwingConstants.CENTER : SwingConstants.LEFT);
        button.setIconTextGap(sidebarCollapsed ? 0 : 10);
        java.awt.Color baseBackground = selected
                ? currentTheme.secondarySurface()
                : currentTheme.surface();
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(hovered && !selected
                ? ThemeUtils.blend(baseBackground, currentTheme.selectionBackground(), 0.22d)
                : baseBackground);
        button.setForeground(selected ? currentTheme.textPrimary() : currentTheme.textSecondary());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? currentTheme.secondarySurface() : currentTheme.surface(), 1),
                BorderFactory.createEmptyBorder(0, sidebarCollapsed ? 0 : 10, 0, sidebarCollapsed ? 0 : 10)));
        button.setIcon(new ToolbarIcon(
                (ToolbarIcon.Type) button.getClientProperty("iconType"),
                18,
                selected ? currentTheme.actionBackground() : button.getForeground()));
    }

    private void styleTopBarIconButton(JButton button) {
        // top-bar icon buttons are icon-only
        ButtonStyler.applyStandard(button, currentTheme, false, true, false);
        button.setText("");
        button.setIcon(new ToolbarIcon(
            (ToolbarIcon.Type) button.getClientProperty("iconType"),
            18,
            button.isEnabled() ? currentTheme.textSecondary() : currentTheme.disabledBorder()));
    }

    private void styleRefreshButton() {
        ButtonStyler.applyStandard(refreshButton, currentTheme, false, true, false);
        refreshButton.setIcon(new ToolbarIcon(ToolbarIcon.Type.REFRESH, 20,
            refreshButton.isEnabled() ? currentTheme.actionBackground() : currentTheme.textSecondary()));
    }

    private void styleWirelessButton() {
        ButtonStyler.applyStandard(wirelessButton, currentTheme, false, true, false);
        wirelessButton.setText("+");
    }

    private void styleTcpipButton() {
        ButtonStyler.applyStandard(tcpipButton, currentTheme, false, true, false);
        tcpipButton.setIcon(new ToolbarIcon(ToolbarIcon.Type.WIRELESS, 18,
            tcpipButton.isEnabled() ? currentTheme.actionBackground() : currentTheme.textSecondary()));
        tcpipButton.setText("");
    }

    private void stylePowerMenu() {
        powerMenu.setBorder(BorderFactory.createLineBorder(currentTheme.border(), 1));
        powerMenu.setBackground(currentTheme.surface());
        for (JMenuItem menuItem : powerMenuItems.values()) {
            menuItem.setOpaque(true);
            menuItem.setBackground(currentTheme.surface());
            menuItem.setForeground(menuItem.isEnabled() ? currentTheme.textPrimary() : currentTheme.textSecondary());
            menuItem.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            menuItem.setFont(new Font("Inter", Font.PLAIN, 14));
        }
    }

    private void installTabNavigationShortcuts() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(tabNavigationDispatcher);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(tabNavigationDispatcher);

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
        int nextIndex = Math.floorMod(currentTabIndex() + delta, 8);
        switch (nextIndex) {
            case 0 -> homeButton.doClick();
            case 1 -> displayButton.doClick();
            case 2 -> mirroringButton.doClick();
            case 3 -> controlButton.doClick();
            case 4 -> appsButton.doClick();
            case 5 -> filesButton.doClick();
            case 6 -> systemButton.doClick();
            case 7 -> settingsButton.doClick();
            default -> homeButton.doClick();
        }
    }

    private int currentTabIndex() {
        if (displayButton.isSelected()) {
            return 1;
        }
        if (mirroringButton.isSelected()) {
            return 2;
        }
        if (controlButton.isSelected()) {
            return 3;
        }
        if (appsButton.isSelected()) {
            return 4;
        }
        if (filesButton.isSelected()) {
            return 5;
        }
        if (systemButton.isSelected()) {
            return 6;
        }
        if (settingsButton.isSelected()) {
            return 7;
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
