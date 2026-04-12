package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;

import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.logic.model.InstalledApp;
import com.adbmanager.logic.model.ScrcpyCamera;
import com.adbmanager.logic.model.ScrcpyLaunchRequest;
import com.adbmanager.logic.model.ScrcpyStatus;
import com.adbmanager.view.Messages;

public class ScrcpyLauncherPanel extends JPanel {

    private final ScrollableContentPanel content = new ScrollableContentPanel();
    private final JScrollPane scrollPane = new JScrollPane(content);

    private final WrappingTextArea introLabel = new WrappingTextArea();
    private final JPanel statusCard = new JPanel();
    private final JLabel availabilityLabel = new JLabel();
    private final JLabel availabilityValueLabel = new JLabel("-");
    private final JLabel versionLabel = new JLabel();
    private final JLabel versionValueLabel = new JLabel("-");
    private final JLabel locationLabel = new JLabel();
    private final WrappingTextArea locationValueLabel = new WrappingTextArea("-");
    private final JLabel feedbackLabel = new JLabel();
    private final JButton prepareButton = new JButton();

    private final JPanel sourceCard = new JPanel();
    private final JLabel targetLabel = new JLabel();
    private final JComboBox<ScrcpyLaunchRequest.LaunchTarget> targetCombo = new JComboBox<>(
            ScrcpyLaunchRequest.LaunchTarget.values());
    private final JCheckBox fullscreenCheck = new JCheckBox();
    private final JCheckBox readOnlyCheck = new JCheckBox();
    private final WrappingTextArea hintLabel = new WrappingTextArea();

    private final JPanel optionsCard = new JPanel();
    private final JLabel maxSizeLabel = new JLabel();
    private final JTextField maxSizeField = new JTextField();
    private final JLabel maxFpsLabel = new JLabel();
    private final JTextField maxFpsField = new JTextField();
    private final JPanel virtualDisplayPanel = new JPanel(new GridBagLayout());
    private final JLabel virtualDisplayTitleLabel = new JLabel();
    private final JLabel virtualWidthLabel = new JLabel();
    private final JLabel virtualHeightLabel = new JLabel();
    private final JLabel virtualDpiLabel = new JLabel();
    private final JTextField virtualWidthField = new JTextField();
    private final JTextField virtualHeightField = new JTextField();
    private final JTextField virtualDpiField = new JTextField();
    private final JPanel cameraPanel = new JPanel(new GridBagLayout());
    private final JLabel cameraTitleLabel = new JLabel();
    private final JLabel cameraIdLabel = new JLabel();
    private final JComboBox<Object> cameraCombo = new JComboBox<>();
    private final JButton refreshCamerasButton = new JButton();
    private final JLabel cameraWidthLabel = new JLabel();
    private final JLabel cameraHeightLabel = new JLabel();
    private final JTextField cameraWidthField = new JTextField();
    private final JTextField cameraHeightField = new JTextField();
    private final JLabel audioLabel = new JLabel();
    private final JComboBox<ScrcpyLaunchRequest.AudioSource> audioCombo = new JComboBox<>(
            ScrcpyLaunchRequest.AudioSource.values());
    private final JLabel keyboardLabel = new JLabel();
    private final JComboBox<ScrcpyLaunchRequest.InputMode> keyboardCombo = new JComboBox<>(
            ScrcpyLaunchRequest.InputMode.values());
    private final JLabel mouseLabel = new JLabel();
    private final JComboBox<ScrcpyLaunchRequest.InputMode> mouseCombo = new JComboBox<>(
            ScrcpyLaunchRequest.InputMode.values());

    private final JPanel startAppCard = new JPanel();
    private final JCheckBox startAppCheck = new JCheckBox();
    private final JComboBox<Object> startAppCombo = new JComboBox<>();

    private final JPanel recordCard = new JPanel();
    private final JCheckBox recordCheck = new JCheckBox();
    private final JTextField recordPathField = new JTextField();
    private final JButton browseRecordButton = new JButton();

    private final JPanel launchActionsPanel = new JPanel(new GridLayout(1, 1, 0, 0));
    private final JButton launchButton = new JButton();

    private final EnumComboRenderer enumRenderer = new EnumComboRenderer();
    private final ValueComboRenderer appRenderer = new ValueComboRenderer();
    private final ValueComboRenderer cameraRenderer = new ValueComboRenderer();

    private AppTheme theme = AppTheme.LIGHT;
    private DeviceDetails currentDeviceDetails;
    private boolean busy;
    private boolean deviceAvailable;
    private ActionListener launchTargetChangeAction = event -> {
    };
    private ActionListener startAppToggleAction = event -> {
    };

    public ScrcpyLauncherPanel() {
        buildPanel();
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
        setScrcpyStatus(ScrcpyStatus.missing());
        setFeedback("", false);
        setAvailableApps(List.of());
        setAvailableCameras(List.of());
        updateSourceSpecificControls();
    }

    public void setPrepareAction(ActionListener actionListener) {
        prepareButton.addActionListener(actionListener);
    }

    public void setLaunchAction(ActionListener actionListener) {
        launchButton.addActionListener(actionListener);
    }

    public void setBrowseRecordPathAction(ActionListener actionListener) {
        browseRecordButton.addActionListener(actionListener);
    }

    public void setRefreshCamerasAction(ActionListener actionListener) {
        refreshCamerasButton.addActionListener(actionListener);
    }

    public void setLaunchTargetChangeAction(ActionListener actionListener) {
        launchTargetChangeAction = actionListener == null ? event -> {
        } : actionListener;
    }

    public void setStartAppToggleAction(ActionListener actionListener) {
        startAppToggleAction = actionListener == null ? event -> {
        } : actionListener;
    }

    public void setDeviceAvailable(boolean deviceAvailable) {
        this.deviceAvailable = deviceAvailable;
        updateControlStates();
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        updateControlStates();
    }

    public void setScrcpyStatus(ScrcpyStatus status) {
        ScrcpyStatus currentStatus = Objects.requireNonNullElse(status, ScrcpyStatus.missing());
        availabilityValueLabel.setText(Messages.text(currentStatus.available()
                ? (currentStatus.managedInstallation() ? "scrcpy.status.managed" : "scrcpy.status.system")
                : "scrcpy.status.missing"));
        versionValueLabel.setText(currentStatus.version());
        locationValueLabel.setText(currentStatus.locationLabel());
    }

    public void setFeedback(String message, boolean error) {
        String normalized = message == null ? "" : message.trim();
        feedbackLabel.putClientProperty("error", error);
        feedbackLabel.setText(normalized.isBlank() ? " " : normalized);
        styleFeedbackLabel();
    }

    public void setRecordPath(String path) {
        recordPathField.setText(path == null ? "" : path.trim());
    }

    public void applyLaunchRequest(ScrcpyLaunchRequest request) {
        ScrcpyLaunchRequest safeRequest = request == null
                ? new ScrcpyLaunchRequest(
                        ScrcpyLaunchRequest.LaunchTarget.DEVICE_DISPLAY,
                        false,
                        null,
                        null,
                        false,
                        "",
                        false,
                        "",
                        ScrcpyLaunchRequest.AudioSource.DEFAULT,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "",
                        false,
                        ScrcpyLaunchRequest.InputMode.DEFAULT,
                        ScrcpyLaunchRequest.InputMode.DEFAULT)
                : request;

        targetCombo.setSelectedItem(safeRequest.launchTarget());
        fullscreenCheck.setSelected(safeRequest.fullscreen());
        readOnlyCheck.setSelected(safeRequest.readOnly());
        maxSizeField.setText(safeRequest.maxSize() == null ? "" : String.valueOf(safeRequest.maxSize()));
        maxFpsField.setText(safeRequest.maxFps() == null ? "" : formatDecimal(safeRequest.maxFps()));
        virtualWidthField.setText(safeRequest.virtualDisplayWidth() == null ? "" : String.valueOf(safeRequest.virtualDisplayWidth()));
        virtualHeightField.setText(safeRequest.virtualDisplayHeight() == null ? "" : String.valueOf(safeRequest.virtualDisplayHeight()));
        virtualDpiField.setText(safeRequest.virtualDisplayDpi() == null ? "" : String.valueOf(safeRequest.virtualDisplayDpi()));
        cameraWidthField.setText(safeRequest.cameraWidth() == null ? "" : String.valueOf(safeRequest.cameraWidth()));
        cameraHeightField.setText(safeRequest.cameraHeight() == null ? "" : String.valueOf(safeRequest.cameraHeight()));
        audioCombo.setSelectedItem(safeRequest.audioSource());
        keyboardCombo.setSelectedItem(safeRequest.keyboardMode());
        mouseCombo.setSelectedItem(safeRequest.mouseMode());
        recordCheck.setSelected(safeRequest.recordEnabled());
        recordPathField.setText(safeRequest.recordPath());
        startAppCheck.setSelected(safeRequest.startAppEnabled());
        setEditableComboValue(startAppCombo, safeRequest.startApp());
        setEditableComboValue(cameraCombo, safeRequest.cameraId());
        updateSourceSpecificControls();
        updateControlStates();
    }

    public void setAvailableApps(List<InstalledApp> applications) {
        Object currentSelection = startAppCombo.getEditor().getItem();
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        for (InstalledApp application : applications) {
            model.addElement(new AppOption(application.packageName(), application.displayName()));
        }
        startAppCombo.setModel(model);
        startAppCombo.getEditor().setItem(currentSelection);
    }

    public void setAvailableCameras(List<ScrcpyCamera> cameras) {
        Object currentSelection = cameraCombo.getEditor().getItem();
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        for (ScrcpyCamera camera : cameras) {
            model.addElement(camera);
        }
        cameraCombo.setModel(model);
        cameraCombo.getEditor().setItem(currentSelection);
    }

    public void setDeviceDetails(DeviceDetails details) {
        currentDeviceDetails = details;
        updateHintLabel();
    }

    public void clearDeviceDetails() {
        currentDeviceDetails = null;
        updateHintLabel();
    }

    public boolean usesCameraSource() {
        return getSelectedLaunchTarget() == ScrcpyLaunchRequest.LaunchTarget.CAMERA;
    }

    public boolean shouldLoadApplications() {
        return startAppCheck.isSelected() && !usesCameraSource();
    }

    public ScrcpyLaunchRequest getLaunchRequest() {
        return new ScrcpyLaunchRequest(
                getSelectedLaunchTarget(),
                fullscreenCheck.isSelected(),
                parsePositiveInteger(maxSizeField.getText()),
                parsePositiveDouble(maxFpsField.getText()),
                recordCheck.isSelected(),
                recordPathField.getText(),
                startAppCheck.isSelected(),
                selectedComboValue(startAppCombo),
                (ScrcpyLaunchRequest.AudioSource) audioCombo.getSelectedItem(),
                parsePositiveInteger(virtualWidthField.getText()),
                parsePositiveInteger(virtualHeightField.getText()),
                parsePositiveInteger(virtualDpiField.getText()),
                parsePositiveInteger(cameraWidthField.getText()),
                parsePositiveInteger(cameraHeightField.getText()),
                selectedComboValue(cameraCombo),
                readOnlyCheck.isSelected(),
                (ScrcpyLaunchRequest.InputMode) keyboardCombo.getSelectedItem(),
                (ScrcpyLaunchRequest.InputMode) mouseCombo.getSelectedItem());
    }

    public void refreshTexts() {
        introLabel.setText(Messages.text("scrcpy.intro"));
        availabilityLabel.setText(Messages.text("scrcpy.status.availability"));
        versionLabel.setText(Messages.text("scrcpy.status.version"));
        locationLabel.setText(Messages.text("scrcpy.status.location"));
        prepareButton.setText(Messages.text("scrcpy.status.prepare"));

        targetLabel.setText(Messages.text("scrcpy.target.label"));
        fullscreenCheck.setText(Messages.text("scrcpy.option.fullscreen"));
        readOnlyCheck.setText(Messages.text("scrcpy.option.readOnly"));
        maxSizeLabel.setText(Messages.text("scrcpy.option.maxSize"));
        maxFpsLabel.setText(Messages.text("scrcpy.option.maxFps"));
        virtualDisplayTitleLabel.setText(Messages.text("scrcpy.virtual.title"));
        virtualWidthLabel.setText(Messages.text("scrcpy.virtual.width"));
        virtualHeightLabel.setText(Messages.text("scrcpy.virtual.height"));
        virtualDpiLabel.setText(Messages.text("scrcpy.virtual.dpi"));
        cameraTitleLabel.setText(Messages.text("scrcpy.camera.title"));
        cameraIdLabel.setText(Messages.text("scrcpy.camera.id"));
        refreshCamerasButton.setText(Messages.text("scrcpy.camera.refresh"));
        cameraWidthLabel.setText(Messages.text("scrcpy.camera.width"));
        cameraHeightLabel.setText(Messages.text("scrcpy.camera.height"));
        audioLabel.setText(Messages.text("scrcpy.audio.label"));
        keyboardLabel.setText(Messages.text("scrcpy.input.keyboard"));
        mouseLabel.setText(Messages.text("scrcpy.input.mouse"));
        startAppCheck.setText(Messages.text("scrcpy.startApp.toggle"));
        startAppCombo.setToolTipText(Messages.text("scrcpy.startApp.hint"));
        recordCheck.setText(Messages.text("scrcpy.record.toggle"));
        browseRecordButton.setText(Messages.text("scrcpy.record.browse"));
        launchButton.setText(Messages.text("scrcpy.launch"));
        cameraCombo.setToolTipText(Messages.text("scrcpy.camera.hint"));
        updateHintLabel();
    }

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        setBackground(theme.background());
        content.setBackground(theme.background());
        scrollPane.setBackground(theme.background());
        scrollPane.getViewport().setBackground(theme.background());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        if (scrollPane.getVerticalScrollBar() != null) {
            scrollPane.getVerticalScrollBar().setUI(new ThemedScrollBarUI(theme));
            scrollPane.getVerticalScrollBar().setUnitIncrement(24);
            scrollPane.getVerticalScrollBar().setBlockIncrement(96);
        }
        if (scrollPane.getHorizontalScrollBar() != null) {
            scrollPane.getHorizontalScrollBar().setUI(new ThemedScrollBarUI(theme));
            scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        }

        introLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 14), theme.textSecondary());

        styleCard(statusCard);
        styleCard(sourceCard);
        styleCard(optionsCard);
        styleCard(startAppCard);
        styleCard(recordCard);

        styleLabel(availabilityLabel);
        styleLabel(versionLabel);
        styleLabel(locationLabel);
        styleValueLabel(availabilityValueLabel);
        styleValueLabel(versionValueLabel);
        locationValueLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 14), theme.textPrimary());
        styleFeedbackLabel();

        styleLabel(targetLabel);
        styleCheckBox(fullscreenCheck);
        styleCheckBox(readOnlyCheck);
        hintLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 13), theme.textSecondary());

        styleLabel(maxSizeLabel);
        styleLabel(maxFpsLabel);
        styleLabel(virtualDisplayTitleLabel);
        styleLabel(virtualWidthLabel);
        styleLabel(virtualHeightLabel);
        styleLabel(virtualDpiLabel);
        styleLabel(cameraTitleLabel);
        styleLabel(cameraIdLabel);
        styleLabel(cameraWidthLabel);
        styleLabel(cameraHeightLabel);
        styleLabel(audioLabel);
        styleLabel(keyboardLabel);
        styleLabel(mouseLabel);

        styleTextField(maxSizeField);
        styleTextField(maxFpsField);
        styleTextField(virtualWidthField);
        styleTextField(virtualHeightField);
        styleTextField(virtualDpiField);
        styleTextField(cameraWidthField);
        styleTextField(cameraHeightField);
        styleTextField(recordPathField);

        styleComboBox(targetCombo, enumRenderer);
        styleComboBox(audioCombo, enumRenderer);
        styleComboBox(keyboardCombo, enumRenderer);
        styleComboBox(mouseCombo, enumRenderer);
        styleComboBox(startAppCombo, appRenderer);
        styleComboBox(cameraCombo, cameraRenderer);
        refreshComboEditors();

        styleCheckBox(startAppCheck);
        styleCheckBox(recordCheck);
        styleButtons();
        repaint();
    }

    private void buildPanel() {
        setLayout(new BorderLayout());
        content.setLayout(new GridBagLayout());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(theme.background());

        buildStatusCard();
        buildSourceCard();
        buildOptionsCard();
        buildStartAppCard();
        buildRecordCard();
        buildLaunchActions();

        addContentRow(introLabel, 0, 0.0, new Insets(0, 0, 14, 0));
        addContentRow(statusCard, 1, 0.0, new Insets(0, 0, 12, 0));
        addContentRow(sourceCard, 2, 0.0, new Insets(0, 0, 12, 0));
        addContentRow(optionsCard, 3, 0.0, new Insets(0, 0, 12, 0));
        addContentRow(startAppCard, 4, 0.0, new Insets(0, 0, 12, 0));
        addContentRow(recordCard, 5, 0.0, new Insets(0, 0, 12, 0));
        addContentRow(launchActionsPanel, 6, 0.0, new Insets(0, 0, 0, 0));

        GridBagConstraints fillerConstraints = new GridBagConstraints();
        fillerConstraints.gridx = 0;
        fillerConstraints.gridy = 7;
        fillerConstraints.weightx = 1.0;
        fillerConstraints.weighty = 1.0;
        fillerConstraints.fill = GridBagConstraints.BOTH;
        content.add(Box.createGlue(), fillerConstraints);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void addContentRow(Component component, int rowIndex, double weightY, Insets insets) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = rowIndex;
        constraints.weightx = 1.0;
        constraints.weighty = weightY;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = insets;
        content.add(component, constraints);
    }

    private void buildStatusCard() {
        statusCard.setLayout(new BoxLayout(statusCard, BoxLayout.Y_AXIS));
        statusCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        configureButton(prepareButton);
        statusCard.add(createKeyValueRow(availabilityLabel, availabilityValueLabel));
        statusCard.add(Box.createVerticalStrut(8));
        statusCard.add(createKeyValueRow(versionLabel, versionValueLabel));
        statusCard.add(Box.createVerticalStrut(8));
        statusCard.add(createKeyValueRow(locationLabel, locationValueLabel));
        statusCard.add(Box.createVerticalStrut(10));
        statusCard.add(feedbackLabel);
        statusCard.add(Box.createVerticalStrut(12));
        statusCard.add(prepareButton);
    }

    private void buildSourceCard() {
        sourceCard.setLayout(new BoxLayout(sourceCard, BoxLayout.Y_AXIS));
        sourceCard.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel targetRow = new JPanel(new BorderLayout(10, 0));
        targetRow.setOpaque(false);
        targetRow.add(targetLabel, BorderLayout.WEST);
        targetRow.add(targetCombo, BorderLayout.CENTER);

        JPanel togglesRow = new JPanel(new GridLayout(1, 2, 10, 0));
        togglesRow.setOpaque(false);
        togglesRow.add(fullscreenCheck);
        togglesRow.add(readOnlyCheck);

        targetCombo.addActionListener(event -> {
            updateSourceSpecificControls();
            launchTargetChangeAction.actionPerformed(event);
        });

        sourceCard.add(targetRow);
        sourceCard.add(Box.createVerticalStrut(12));
        sourceCard.add(togglesRow);
        sourceCard.add(Box.createVerticalStrut(10));
        sourceCard.add(hintLabel);
    }

    private void buildOptionsCard() {
        optionsCard.setLayout(new BoxLayout(optionsCard, BoxLayout.Y_AXIS));
        optionsCard.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel videoTuningPanel = new JPanel(new GridBagLayout());
        videoTuningPanel.setOpaque(false);
        addFormField(videoTuningPanel, maxSizeLabel, maxSizeField, 0);
        addFormField(videoTuningPanel, maxFpsLabel, maxFpsField, 1);

        virtualDisplayPanel.setOpaque(false);
        addFormField(virtualDisplayPanel, virtualWidthLabel, virtualWidthField, 0);
        addFormField(virtualDisplayPanel, virtualHeightLabel, virtualHeightField, 1);
        addFormField(virtualDisplayPanel, virtualDpiLabel, virtualDpiField, 2);

        cameraCombo.setEditable(true);
        configureButton(refreshCamerasButton);
        JPanel cameraIdRow = new JPanel(new BorderLayout(8, 0));
        cameraIdRow.setOpaque(false);
        cameraIdRow.add(cameraCombo, BorderLayout.CENTER);
        cameraIdRow.add(refreshCamerasButton, BorderLayout.EAST);

        cameraPanel.setOpaque(false);
        addLabelOnly(cameraPanel, cameraTitleLabel, 0);
        addFormField(cameraPanel, cameraIdLabel, cameraIdRow, 1);
        addFormField(cameraPanel, cameraWidthLabel, cameraWidthField, 2);
        addFormField(cameraPanel, cameraHeightLabel, cameraHeightField, 3);

        JPanel audioInputPanel = new JPanel(new GridBagLayout());
        audioInputPanel.setOpaque(false);
        addFormField(audioInputPanel, audioLabel, audioCombo, 0);
        addFormField(audioInputPanel, keyboardLabel, keyboardCombo, 1);
        addFormField(audioInputPanel, mouseLabel, mouseCombo, 2);

        optionsCard.add(videoTuningPanel);
        optionsCard.add(Box.createVerticalStrut(12));
        optionsCard.add(virtualDisplayTitleLabel);
        optionsCard.add(Box.createVerticalStrut(8));
        optionsCard.add(virtualDisplayPanel);
        optionsCard.add(Box.createVerticalStrut(12));
        optionsCard.add(cameraPanel);
        optionsCard.add(Box.createVerticalStrut(12));
        optionsCard.add(audioInputPanel);
    }

    private void buildStartAppCard() {
        startAppCard.setLayout(new BoxLayout(startAppCard, BoxLayout.Y_AXIS));
        startAppCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        startAppCombo.setEditable(true);
        startAppCheck.addActionListener(event -> {
            updateSourceSpecificControls();
            startAppToggleAction.actionPerformed(event);
        });
        startAppCard.add(startAppCheck);
        startAppCard.add(Box.createVerticalStrut(10));
        startAppCard.add(startAppCombo);
    }

    private void buildRecordCard() {
        recordCard.setLayout(new BoxLayout(recordCard, BoxLayout.Y_AXIS));
        recordCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        configureButton(browseRecordButton);

        JPanel pathRow = new JPanel(new BorderLayout(8, 0));
        pathRow.setOpaque(false);
        pathRow.add(recordPathField, BorderLayout.CENTER);
        pathRow.add(browseRecordButton, BorderLayout.EAST);

        recordCheck.addActionListener(event -> updateControlStates());

        recordCard.add(recordCheck);
        recordCard.add(Box.createVerticalStrut(10));
        recordCard.add(pathRow);
    }

    private void buildLaunchActions() {
        launchActionsPanel.setOpaque(false);
        configureButton(launchButton);
        launchActionsPanel.add(launchButton);
    }

    private JPanel createKeyValueRow(JLabel keyLabel, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(keyLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private JPanel createKeyValueRow(JLabel keyLabel, WrappingTextArea valueLabel) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(keyLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private void addLabelOnly(JPanel parent, JLabel label, int rowIndex) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = rowIndex;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 10, 0);
        parent.add(label, constraints);
    }

    private void addFormField(JPanel parent, JLabel label, JComponent field, int rowIndex) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = rowIndex;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(0, 0, 10, 10);
        parent.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = rowIndex;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(0, 0, 10, 0);
        parent.add(field, fieldConstraints);
    }

    private ScrcpyLaunchRequest.LaunchTarget getSelectedLaunchTarget() {
        Object selectedItem = targetCombo.getSelectedItem();
        return selectedItem instanceof ScrcpyLaunchRequest.LaunchTarget launchTarget
                ? launchTarget
                : ScrcpyLaunchRequest.LaunchTarget.DEVICE_DISPLAY;
    }

    private void updateSourceSpecificControls() {
        boolean cameraMode = usesCameraSource();
        boolean virtualDisplayMode = getSelectedLaunchTarget() == ScrcpyLaunchRequest.LaunchTarget.VIRTUAL_DISPLAY;

        virtualDisplayTitleLabel.setVisible(virtualDisplayMode);
        virtualDisplayPanel.setVisible(virtualDisplayMode);
        cameraPanel.setVisible(cameraMode);

        startAppCheck.setEnabled(!busy && !cameraMode);
        startAppCombo.setEnabled(!busy && startAppCheck.isSelected() && !cameraMode);

        readOnlyCheck.setEnabled(!busy && !cameraMode);
        keyboardCombo.setEnabled(!busy && !cameraMode && !readOnlyCheck.isSelected());
        mouseCombo.setEnabled(!busy && !cameraMode && !readOnlyCheck.isSelected());
        maxSizeField.setEnabled(!busy && !cameraMode);
        refreshCamerasButton.setEnabled(!busy && cameraMode && deviceAvailable);
        cameraCombo.setEnabled(!busy && cameraMode);
        cameraWidthField.setEnabled(!busy && cameraMode);
        cameraHeightField.setEnabled(!busy && cameraMode);
        refreshComboEditors();
        updateHintLabel();
        revalidate();
        repaint();
    }

    private void updateHintLabel() {
        String hintKey;
        if (usesCameraSource()) {
            hintKey = "scrcpy.hint.camera";
        } else if (currentDeviceDetails != null && parseApiLevel(currentDeviceDetails.apiLevel()) < 30) {
            hintKey = "scrcpy.hint.audioLimited";
        } else {
            hintKey = "scrcpy.hint.default";
        }
        hintLabel.setText(Messages.text(hintKey));
    }

    private int parseApiLevel(String apiLevel) {
        if (apiLevel == null || apiLevel.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(apiLevel.trim());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private void updateControlStates() {
        boolean allowEditing = !busy;
        prepareButton.setEnabled(!busy);
        launchButton.setEnabled(!busy && deviceAvailable);

        targetCombo.setEnabled(allowEditing);
        fullscreenCheck.setEnabled(allowEditing);
        audioCombo.setEnabled(allowEditing);
        recordCheck.setEnabled(allowEditing);
        recordPathField.setEnabled(allowEditing && recordCheck.isSelected());
        browseRecordButton.setEnabled(allowEditing && recordCheck.isSelected());
        updateSourceSpecificControls();
        refreshComboEditors();
        styleButtons();
    }

    private void styleCard(JPanel panel) {
        panel.setOpaque(true);
        panel.setBackground(theme.background());
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                new EmptyBorder(14, 14, 14, 14)));
    }

    private void styleLabel(JLabel label) {
        label.setForeground(theme.textSecondary());
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
    }

    private void styleValueLabel(JLabel label) {
        label.setForeground(theme.textPrimary());
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    }

    private void styleFeedbackLabel() {
        boolean error = Boolean.TRUE.equals(feedbackLabel.getClientProperty("error"));
        feedbackLabel.setForeground(error ? new java.awt.Color(214, 80, 80) : theme.actionBackground());
        feedbackLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
    }

    private void styleCheckBox(JCheckBox checkBox) {
        checkBox.setOpaque(true);
        checkBox.setBackground(theme.background());
        checkBox.setForeground(theme.textPrimary());
        checkBox.setFocusPainted(false);
        checkBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    }

    private void styleTextField(JTextField textField) {
        textField.setForeground(theme.textPrimary());
        textField.setDisabledTextColor(theme.textSecondary());
        textField.setCaretColor(theme.textPrimary());
        textField.setBackground(textField.isEnabled() ? theme.secondarySurface() : theme.surface());
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        textField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
    }

    private void styleComboBox(JComboBox<?> comboBox, DefaultListCellRenderer renderer) {
        ThemedComboBoxUI.apply(comboBox, theme);
        comboBox.setUI(new ThemedComboBoxUI(theme));
        comboBox.setRenderer(renderer);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        comboBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        comboBox.setFocusable(false);
        comboBox.setBackground(theme.secondarySurface());
        comboBox.setForeground(theme.textPrimary());

        if (comboBox.isEditable() && comboBox.getEditor().getEditorComponent() instanceof JTextField editorField) {
            styleEditableComboEditor(comboBox, editorField);
        }
    }

    private void refreshComboEditors() {
        refreshEditableComboEditor(targetCombo);
        refreshEditableComboEditor(audioCombo);
        refreshEditableComboEditor(keyboardCombo);
        refreshEditableComboEditor(mouseCombo);
        refreshEditableComboEditor(startAppCombo);
        refreshEditableComboEditor(cameraCombo);

        styleTextField(maxSizeField);
        styleTextField(maxFpsField);
        styleTextField(virtualWidthField);
        styleTextField(virtualHeightField);
        styleTextField(virtualDpiField);
        styleTextField(cameraWidthField);
        styleTextField(cameraHeightField);
        styleTextField(recordPathField);
    }

    private void refreshEditableComboEditor(JComboBox<?> comboBox) {
        if (comboBox.isEditable() && comboBox.getEditor().getEditorComponent() instanceof JTextField editorField) {
            styleEditableComboEditor(comboBox, editorField);
        }
    }

    private void styleEditableComboEditor(JComboBox<?> comboBox, JTextField editorField) {
        boolean enabled = comboBox.isEnabled();
        editorField.setOpaque(true);
        editorField.setEnabled(enabled);
        editorField.setEditable(enabled);
        editorField.setDisabledTextColor(theme.textSecondary());
        editorField.setBackground(enabled ? theme.secondarySurface() : theme.surface());
        editorField.setForeground(enabled ? theme.textPrimary() : theme.textSecondary());
        editorField.setCaretColor(theme.textPrimary());
        editorField.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        editorField.setSelectionColor(theme.selectionBackground());
        editorField.setSelectedTextColor(theme.selectionForeground());
        editorField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    }

    private void styleButtons() {
        styleButton(prepareButton, false);
        styleButton(refreshCamerasButton, false);
        styleButton(browseRecordButton, false);
        styleButton(launchButton, true);
    }

    private void configureButton(JButton button) {
        button.setRolloverEnabled(true);
        button.getModel().addChangeListener(event -> styleButtons());
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
        button.setPreferredSize(new Dimension(0, 38));

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
            return;
        }

        button.setBackground(theme.secondarySurface());
        button.setForeground(theme.textSecondary());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.disabledBorder(), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    }

    private String selectedComboValue(JComboBox<?> comboBox) {
        Object selectedItem = comboBox.getEditor().getItem();
        if (selectedItem instanceof AppOption appOption) {
            return appOption.packageName();
        }
        if (selectedItem instanceof ScrcpyCamera camera) {
            return camera.id();
        }
        return selectedItem == null ? "" : selectedItem.toString().trim();
    }

    private Integer parsePositiveInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Double parsePositiveDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(value.trim().replace(',', '.'));
            return parsed > 0d ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void setEditableComboValue(JComboBox<?> comboBox, String value) {
        Object safeValue = value == null ? "" : value.trim();
        if (comboBox.isEditable()) {
            comboBox.getEditor().setItem(safeValue);
            return;
        }
        comboBox.setSelectedItem(safeValue);
    }

    private String formatDecimal(Double value) {
        if (value == null) {
            return "";
        }
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.0001d) {
            return String.valueOf((long) rounded);
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private final class EnumComboRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setOpaque(true);
            setBackground(isSelected ? theme.selectionBackground() : (index == -1 ? theme.secondarySurface() : theme.surface()));
            setForeground(isSelected ? theme.selectionForeground() : theme.textPrimary());

            if (value instanceof ScrcpyLaunchRequest.LaunchTarget launchTarget) {
                setText(Messages.text(launchTarget.messageKey()));
            } else if (value instanceof ScrcpyLaunchRequest.AudioSource audioSource) {
                setText(Messages.text(audioSource.messageKey()));
            } else if (value instanceof ScrcpyLaunchRequest.InputMode inputMode) {
                setText(Messages.text(inputMode.messageKey()));
            }
            return this;
        }
    }

    private final class ValueComboRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setOpaque(true);
            setBackground(isSelected ? theme.selectionBackground() : (index == -1 ? theme.secondarySurface() : theme.surface()));
            setForeground(isSelected ? theme.selectionForeground() : theme.textPrimary());

            if (value instanceof AppOption appOption) {
                setText(appOption.toString());
            } else if (value instanceof ScrcpyCamera camera) {
                setText(camera.displayLabel());
            }
            return this;
        }
    }

    private record AppOption(String packageName, String label) {
        @Override
        public String toString() {
            return label + " · " + packageName;
        }
    }
}
