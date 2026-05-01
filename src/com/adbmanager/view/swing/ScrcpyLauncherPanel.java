package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicToggleButtonUI;

import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.logic.model.InstalledApp;
import com.adbmanager.logic.model.ScrcpyCamera;
import com.adbmanager.logic.model.ScrcpyLaunchRequest;
import com.adbmanager.logic.model.ScrcpyStatus;
import com.adbmanager.view.Messages;

public class ScrcpyLauncherPanel extends JPanel {

    private final ScrollableContentPanel content = new ScrollableContentPanel();
    private final JScrollPane scrollPane = new JScrollPane(content);
    private final JPanel launchFooterPanel = new JPanel(new BorderLayout());

    private final WrappingTextArea introLabel = new WrappingTextArea();
    private final JPanel introActionsPanel = new JPanel(new BorderLayout(16, 0));
    private final JPanel topActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
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
    private final JPanel targetCardsPanel = new JPanel(new GridLayout(1, 3, 14, 0));
    private final JLabel targetLabel = new JLabel();
    private final JComboBox<ScrcpyLaunchRequest.LaunchTarget> targetCombo = new JComboBox<>(
            ScrcpyLaunchRequest.LaunchTarget.values());
    private final JCheckBox fullscreenCheck = new JCheckBox();
    private final JCheckBox turnScreenOffCheck = new JCheckBox();
    private final JCheckBox readOnlyCheck = new JCheckBox();
    private final WrappingTextArea hintLabel = new WrappingTextArea();

    private final JPanel optionsCard = new JPanel();
    private final JPanel imageOptionsPanel = new JPanel();
    private final JPanel ioOptionsPanel = new JPanel();
    private final JPanel virtualDisplaySection = new JPanel();
    private final JPanel cameraSection = new JPanel();
    private final JLabel imageSectionLabel = new JLabel();
    private final JLabel ioSectionLabel = new JLabel();
    private final JLabel maxSizeLabel = new JLabel();
    private final JTextField maxSizeField = new JTextField();
    private final JLabel maxFpsLabel = new JLabel();
    private final JTextField maxFpsField = new JTextField();
    private final JPanel videoTuningPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
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

    private final JButton launchButton = new JButton();

    private final EnumComboRenderer enumRenderer = new EnumComboRenderer();
    private final ValueComboRenderer appRenderer = new ValueComboRenderer();
    private final ValueComboRenderer cameraRenderer = new ValueComboRenderer();
    private final Map<ScrcpyLaunchRequest.LaunchTarget, JToggleButton> targetButtons = new EnumMap<>(
            ScrcpyLaunchRequest.LaunchTarget.class);

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
        turnScreenOffCheck.setSelected(safeRequest.turnScreenOff());
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
                turnScreenOffCheck.isSelected(),
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
        updateTargetButtonTexts();
        fullscreenCheck.setText(Messages.text("scrcpy.option.fullscreen"));
        turnScreenOffCheck.setText(Messages.text("scrcpy.option.turnScreenOff"));
        readOnlyCheck.setText(Messages.text("scrcpy.option.readOnly"));
        imageSectionLabel.setText(Messages.text("scrcpy.section.image"));
        ioSectionLabel.setText(Messages.text("scrcpy.section.io"));
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
        launchFooterPanel.setBackground(theme.background());
        if (scrollPane.getVerticalScrollBar() != null) {
            scrollPane.getVerticalScrollBar().setUI(new ThemedScrollBarUI(theme));
            scrollPane.getVerticalScrollBar().setUnitIncrement(24);
            scrollPane.getVerticalScrollBar().setBlockIncrement(96);
        }
        if (scrollPane.getHorizontalScrollBar() != null) {
            scrollPane.getHorizontalScrollBar().setUI(new ThemedScrollBarUI(theme));
            scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        }

        introLabel.applyTheme(theme, new Font("Inter", Font.PLAIN, 14), theme.textSecondary());
        introActionsPanel.setBackground(theme.background());
        topActionsPanel.setBackground(theme.background());
        videoTuningPanel.setBackground(theme.background());
        virtualDisplaySection.setBackground(theme.background());
        cameraSection.setBackground(theme.background());

        styleCard(statusCard);
        styleCard(sourceCard);
        styleCard(optionsCard);
        styleCard(imageOptionsPanel);
        styleCard(ioOptionsPanel);
        styleCard(startAppCard);
        styleCard(recordCard);

        styleLabel(availabilityLabel);
        styleLabel(versionLabel);
        styleLabel(locationLabel);
        styleValueLabel(availabilityValueLabel);
        styleValueLabel(versionValueLabel);
        locationValueLabel.applyTheme(theme, new Font("Inter", Font.PLAIN, 14), theme.textPrimary());
        styleFeedbackLabel();

        styleLabel(targetLabel);
        styleTargetButtons();
        styleCheckBox(fullscreenCheck);
        styleCheckBox(turnScreenOffCheck);
        styleCheckBox(readOnlyCheck);
        hintLabel.applyTheme(theme, new Font("Inter", Font.PLAIN, 13), theme.textSecondary());

        styleSectionTitle(imageSectionLabel);
        styleSectionTitle(ioSectionLabel);
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
        introLabel.setAlignmentX(LEFT_ALIGNMENT);
        introLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        buildStartAppCard();
        buildRecordCard();
        buildStatusCard();
        buildSourceCard();
        buildOptionsCard();
        buildTopActions();
        buildLaunchFooter();

        addContentRow(introActionsPanel, 0, 0.0, new Insets(0, 0, 14, 0));
        addContentRow(statusCard, 1, 0.0, new Insets(0, 0, 12, 0));
        addContentRow(sourceCard, 2, 0.0, new Insets(0, 0, 12, 0));
        addContentRow(optionsCard, 3, 0.0, new Insets(0, 0, 12, 0));

        GridBagConstraints fillerConstraints = new GridBagConstraints();
        fillerConstraints.gridx = 0;
        fillerConstraints.gridy = 4;
        fillerConstraints.weightx = 1.0;
        fillerConstraints.weighty = 1.0;
        fillerConstraints.fill = GridBagConstraints.BOTH;
        content.add(Box.createGlue(), fillerConstraints);

        add(scrollPane, BorderLayout.CENTER);
        add(launchFooterPanel, BorderLayout.SOUTH);
    }

    private void buildTopActions() {
        introActionsPanel.setOpaque(false);
        topActionsPanel.setOpaque(false);
        configureButton(prepareButton);
        prepareButton.setPreferredSize(new Dimension(150, 38));
        topActionsPanel.add(prepareButton);
        introActionsPanel.add(introLabel, BorderLayout.CENTER);
        introActionsPanel.add(topActionsPanel, BorderLayout.EAST);
    }

    private void buildLaunchFooter() {
        configureButton(launchButton);
        launchFooterPanel.setOpaque(true);
        launchFooterPanel.setBorder(new EmptyBorder(12, 0, 0, 0));
        launchButton.setPreferredSize(new Dimension(0, 46));
        launchFooterPanel.add(launchButton, BorderLayout.CENTER);
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
        statusCard.setBorder(new EmptyBorder(12, 12, 12, 12));
        availabilityValueLabel.setAlignmentX(LEFT_ALIGNMENT);
        versionValueLabel.setAlignmentX(LEFT_ALIGNMENT);
        locationValueLabel.setAlignmentX(LEFT_ALIGNMENT);
        feedbackLabel.setAlignmentX(LEFT_ALIGNMENT);
        feedbackLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, feedbackLabel.getPreferredSize().height));
        statusCard.add(createKeyValueRow(availabilityLabel, availabilityValueLabel));
        statusCard.add(Box.createVerticalStrut(8));
        statusCard.add(createKeyValueRow(versionLabel, versionValueLabel));
        statusCard.add(Box.createVerticalStrut(8));
        statusCard.add(createKeyValueRow(locationLabel, locationValueLabel));
        statusCard.add(Box.createVerticalStrut(10));
        statusCard.add(feedbackLabel);
    }

    private void buildSourceCard() {
        sourceCard.setLayout(new BoxLayout(sourceCard, BoxLayout.Y_AXIS));
        sourceCard.setBorder(new EmptyBorder(12, 12, 12, 12));
        targetLabel.setAlignmentX(LEFT_ALIGNMENT);
        targetCardsPanel.setOpaque(false);
        targetCardsPanel.setAlignmentX(LEFT_ALIGNMENT);
        targetCardsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));
        addTargetCard(ScrcpyLaunchRequest.LaunchTarget.DEVICE_DISPLAY, ToolbarIcon.Type.DISPLAY);
        addTargetCard(ScrcpyLaunchRequest.LaunchTarget.VIRTUAL_DISPLAY, ToolbarIcon.Type.MIRRORING);
        addTargetCard(ScrcpyLaunchRequest.LaunchTarget.CAMERA, ToolbarIcon.Type.CAMERA);
        targetCombo.setAlignmentX(LEFT_ALIGNMENT);
        targetCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        targetCombo.setVisible(false);
        hintLabel.setAlignmentX(LEFT_ALIGNMENT);
        hintLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel togglesRow = new JPanel(new GridLayout(1, 2, 10, 0));
        togglesRow.setOpaque(false);
        togglesRow.setAlignmentX(LEFT_ALIGNMENT);
        togglesRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        togglesRow.add(fullscreenCheck);
        togglesRow.add(turnScreenOffCheck);

        targetCombo.addActionListener(event -> {
            updateTargetButtonSelection();
            updateSourceSpecificControls();
            launchTargetChangeAction.actionPerformed(event);
        });

        sourceCard.add(targetLabel);
        sourceCard.add(Box.createVerticalStrut(6));
        sourceCard.add(targetCardsPanel);
        sourceCard.add(Box.createVerticalStrut(12));
        sourceCard.add(togglesRow);
        sourceCard.add(Box.createVerticalStrut(10));
        sourceCard.add(hintLabel);
    }

    private void addTargetCard(ScrcpyLaunchRequest.LaunchTarget target, ToolbarIcon.Type iconType) {
        JToggleButton button = new JToggleButton();
        button.putClientProperty("target", target);
        button.putClientProperty("iconType", iconType);
        button.setUI(new BasicToggleButtonUI());
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(JToggleButton.CENTER);
        button.setHorizontalTextPosition(JToggleButton.CENTER);
        button.setVerticalTextPosition(JToggleButton.BOTTOM);
        button.setIconTextGap(10);
        button.addActionListener(event -> targetCombo.setSelectedItem(target));
        button.getModel().addChangeListener(event -> styleTargetButton(button));
        targetButtons.put(target, button);
        targetCardsPanel.add(button);
    }

    private void buildOptionsCard() {
        optionsCard.setLayout(new GridBagLayout());
        optionsCard.setBorder(new EmptyBorder(12, 12, 12, 12));

        videoTuningPanel.setOpaque(false);
        videoTuningPanel.setAlignmentX(LEFT_ALIGNMENT);
        addFlowFormField(videoTuningPanel, maxSizeLabel, maxSizeField, 112);
        addFlowFormField(videoTuningPanel, maxFpsLabel, maxFpsField, 92);

        virtualDisplayPanel.setOpaque(false);
        virtualDisplayPanel.setAlignmentX(LEFT_ALIGNMENT);
        addCompactFormField(virtualDisplayPanel, virtualWidthLabel, virtualWidthField, 0, 0, 92);
        addCompactFormField(virtualDisplayPanel, virtualHeightLabel, virtualHeightField, 1, 0, 92);
        addCompactFormField(virtualDisplayPanel, virtualDpiLabel, virtualDpiField, 2, 0, 92);

        cameraCombo.setEditable(true);
        configureButton(refreshCamerasButton);
        JPanel cameraIdRow = new JPanel(new BorderLayout(8, 0));
        cameraIdRow.setOpaque(false);
        cameraIdRow.add(cameraCombo, BorderLayout.CENTER);
        cameraIdRow.add(refreshCamerasButton, BorderLayout.EAST);

        cameraPanel.setOpaque(false);
        cameraPanel.setAlignmentX(LEFT_ALIGNMENT);
        addLabelOnly(cameraPanel, cameraTitleLabel, 0);
        addWideFormField(cameraPanel, cameraIdLabel, cameraIdRow, 1);
        addCompactFormField(cameraPanel, cameraWidthLabel, cameraWidthField, 0, 4, 92);
        addCompactFormField(cameraPanel, cameraHeightLabel, cameraHeightField, 1, 4, 92);

        JPanel audioInputPanel = new JPanel(new GridBagLayout());
        audioInputPanel.setOpaque(false);
        audioInputPanel.setAlignmentX(LEFT_ALIGNMENT);
        addWideFormField(audioInputPanel, audioLabel, audioCombo, 0);
        addWideFormField(audioInputPanel, keyboardLabel, keyboardCombo, 1);
        addWideFormField(audioInputPanel, mouseLabel, mouseCombo, 2);

        imageOptionsPanel.setLayout(new BoxLayout(imageOptionsPanel, BoxLayout.Y_AXIS));
        imageOptionsPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        imageSectionLabel.setAlignmentX(LEFT_ALIGNMENT);
        virtualDisplaySection.setLayout(new BoxLayout(virtualDisplaySection, BoxLayout.Y_AXIS));
        virtualDisplaySection.setOpaque(false);
        virtualDisplaySection.setAlignmentX(LEFT_ALIGNMENT);
        virtualDisplaySection.add(virtualDisplayTitleLabel);
        virtualDisplaySection.add(Box.createVerticalStrut(8));
        virtualDisplaySection.add(virtualDisplayPanel);
        cameraSection.setLayout(new BorderLayout());
        cameraSection.setOpaque(false);
        cameraSection.setAlignmentX(LEFT_ALIGNMENT);
        cameraSection.add(cameraPanel, BorderLayout.CENTER);

        imageOptionsPanel.add(imageSectionLabel);
        imageOptionsPanel.add(Box.createVerticalStrut(12));
        imageOptionsPanel.add(videoTuningPanel);
        imageOptionsPanel.add(Box.createVerticalStrut(12));
        imageOptionsPanel.add(virtualDisplaySection);
        imageOptionsPanel.add(Box.createVerticalStrut(12));
        imageOptionsPanel.add(cameraSection);
        imageOptionsPanel.add(Box.createVerticalStrut(12));
        imageOptionsPanel.add(recordCard);

        ioOptionsPanel.setLayout(new BoxLayout(ioOptionsPanel, BoxLayout.Y_AXIS));
        ioOptionsPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        ioSectionLabel.setAlignmentX(LEFT_ALIGNMENT);
        readOnlyCheck.setAlignmentX(LEFT_ALIGNMENT);
        ioOptionsPanel.add(ioSectionLabel);
        ioOptionsPanel.add(Box.createVerticalStrut(12));
        ioOptionsPanel.add(readOnlyCheck);
        ioOptionsPanel.add(Box.createVerticalStrut(12));
        ioOptionsPanel.add(audioInputPanel);
        ioOptionsPanel.add(Box.createVerticalStrut(12));
        ioOptionsPanel.add(startAppCard);

        GridBagConstraints imageConstraints = new GridBagConstraints();
        imageConstraints.gridx = 0;
        imageConstraints.gridy = 0;
        imageConstraints.weightx = 1.0;
        imageConstraints.weighty = 1.0;
        imageConstraints.fill = GridBagConstraints.BOTH;
        imageConstraints.insets = new Insets(0, 0, 0, 12);
        optionsCard.add(imageOptionsPanel, imageConstraints);

        GridBagConstraints ioConstraints = new GridBagConstraints();
        ioConstraints.gridx = 1;
        ioConstraints.gridy = 0;
        ioConstraints.weightx = 1.0;
        ioConstraints.weighty = 1.0;
        ioConstraints.fill = GridBagConstraints.BOTH;
        optionsCard.add(ioOptionsPanel, ioConstraints);
    }

    private void buildStartAppCard() {
        startAppCard.setLayout(new BoxLayout(startAppCard, BoxLayout.Y_AXIS));
        startAppCard.setBorder(new EmptyBorder(12, 12, 12, 12));
        startAppCard.setAlignmentX(LEFT_ALIGNMENT);
        startAppCombo.setEditable(true);
        startAppCombo.setAlignmentX(LEFT_ALIGNMENT);
        startAppCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        startAppCheck.setAlignmentX(LEFT_ALIGNMENT);
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
        recordCard.setBorder(new EmptyBorder(12, 12, 12, 12));
        recordCard.setAlignmentX(LEFT_ALIGNMENT);
        configureButton(browseRecordButton);

        JPanel pathRow = new JPanel(new BorderLayout(8, 0));
        pathRow.setOpaque(false);
        pathRow.setAlignmentX(LEFT_ALIGNMENT);
        pathRow.add(recordPathField, BorderLayout.CENTER);
        pathRow.add(browseRecordButton, BorderLayout.EAST);

        recordCheck.addActionListener(event -> updateControlStates());
        recordCheck.setAlignmentX(LEFT_ALIGNMENT);

        recordCard.add(recordCheck);
        recordCard.add(Box.createVerticalStrut(10));
        recordCard.add(pathRow);
    }

    private JPanel createKeyValueRow(JLabel keyLabel, JLabel valueLabel) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        keyLabel.setAlignmentX(LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);
        row.add(keyLabel);
        row.add(Box.createVerticalStrut(4));
        row.add(valueLabel);
        return row;
    }

    private JPanel createKeyValueRow(JLabel keyLabel, WrappingTextArea valueLabel) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        keyLabel.setAlignmentX(LEFT_ALIGNMENT);
        keyLabel.setVerticalAlignment(JLabel.TOP);
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);
        valueLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        row.add(keyLabel);
        row.add(Box.createVerticalStrut(4));
        row.add(valueLabel);
        return row;
    }

    private void addLabelOnly(JPanel parent, JLabel label, int rowIndex) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = rowIndex * 2;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 6, 0);
        parent.add(label, constraints);
    }

    private void addFormField(JPanel parent, JLabel label, JComponent field, int rowIndex) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = rowIndex * 2;
        labelConstraints.gridwidth = 2;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.insets = new Insets(0, 0, 6, 0);
        parent.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 0;
        fieldConstraints.gridy = rowIndex * 2 + 1;
        fieldConstraints.gridwidth = 2;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(0, 0, 10, 0);
        parent.add(field, fieldConstraints);
    }

    private void addWideFormField(JPanel parent, JLabel label, JComponent field, int rowIndex) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = rowIndex * 2;
        labelConstraints.gridwidth = 4;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.insets = new Insets(0, 0, 6, 0);
        parent.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 0;
        fieldConstraints.gridy = rowIndex * 2 + 1;
        fieldConstraints.gridwidth = 4;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(0, 0, 10, 0);
        parent.add(field, fieldConstraints);
    }

    private void addFlowFormField(JPanel parent, JLabel label, JComponent field, int fieldWidth) {
        label.setAlignmentX(LEFT_ALIGNMENT);
        field.setPreferredSize(new Dimension(fieldWidth, 36));
        field.setMinimumSize(new Dimension(Math.max(72, fieldWidth - 16), 36));
        field.setMaximumSize(new Dimension(fieldWidth + 12, 36));
        parent.add(label);
        parent.add(field);
    }

    private void addCompactFormField(
            JPanel parent,
            JLabel label,
            JComponent field,
            int columnIndex,
            int rowIndex,
            int fieldWidth) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = columnIndex * 2;
        labelConstraints.gridy = rowIndex;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(0, columnIndex == 0 ? 0 : 16, 0, 8);
        parent.add(label, labelConstraints);

        field.setPreferredSize(new Dimension(fieldWidth, 36));
        field.setMinimumSize(new Dimension(Math.max(72, fieldWidth - 16), 36));
        field.setMaximumSize(new Dimension(fieldWidth + 12, 36));

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = columnIndex * 2 + 1;
        fieldConstraints.gridy = rowIndex;
        fieldConstraints.anchor = GridBagConstraints.WEST;
        fieldConstraints.fill = GridBagConstraints.NONE;
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
        boolean visibilityChanged = virtualDisplaySection.isVisible() != virtualDisplayMode
                || cameraSection.isVisible() != cameraMode
                || startAppCard.isVisible() == cameraMode;

        virtualDisplaySection.setVisible(virtualDisplayMode);
        cameraSection.setVisible(cameraMode);
        startAppCard.setVisible(!cameraMode);
        readOnlyCheck.setVisible(!cameraMode);
        maxSizeLabel.setVisible(!cameraMode);
        maxSizeField.setVisible(!cameraMode);
        videoTuningPanel.revalidate();
        videoTuningPanel.repaint();

        startAppCheck.setEnabled(!busy && !cameraMode);
        startAppCombo.setEnabled(!busy && startAppCheck.isSelected() && !cameraMode);

        readOnlyCheck.setEnabled(!busy && !cameraMode);
        turnScreenOffCheck.setEnabled(!busy && !cameraMode);
        keyboardCombo.setEnabled(!busy && !cameraMode && !readOnlyCheck.isSelected());
        mouseCombo.setEnabled(!busy && !cameraMode && !readOnlyCheck.isSelected());
        maxSizeField.setEnabled(!busy && !cameraMode);
        refreshCamerasButton.setEnabled(!busy && cameraMode && deviceAvailable);
        cameraCombo.setEnabled(!busy && cameraMode);
        cameraWidthField.setEnabled(!busy && cameraMode);
        cameraHeightField.setEnabled(!busy && cameraMode);
        refreshComboEditors();
        updateTargetButtonSelection();
        styleTargetButtons();
        updateHintLabel();
        if (visibilityChanged) {
            revalidate();
            repaint();
        }
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
                new EmptyBorder(12, 12, 12, 12)));
    }

    private void styleLabel(JLabel label) {
        label.setForeground(theme.textSecondary());
        label.setFont(new Font("Inter", Font.BOLD, 13));
    }

    private void styleSectionTitle(JLabel label) {
        label.setForeground(theme.textPrimary());
        label.setFont(new Font("Inter", Font.BOLD, 20));
    }

    private void styleValueLabel(JLabel label) {
        label.setForeground(theme.textPrimary());
        label.setFont(new Font("Inter", Font.PLAIN, 14));
    }

    private void styleFeedbackLabel() {
        boolean error = Boolean.TRUE.equals(feedbackLabel.getClientProperty("error"));
        feedbackLabel.setForeground(error ? new java.awt.Color(214, 80, 80) : theme.actionBackground());
        feedbackLabel.setFont(new Font("Inter", Font.BOLD, 13));
    }

    private void styleCheckBox(JCheckBox checkBox) {
        checkBox.setOpaque(true);
        checkBox.setBackground(theme.background());
        checkBox.setForeground(theme.textPrimary());
        checkBox.setFocusPainted(false);
        checkBox.setFont(new Font("Inter", Font.PLAIN, 14));
    }

    private void styleTextField(JTextField textField) {
        textField.setForeground(theme.textPrimary());
        textField.setDisabledTextColor(theme.textSecondary());
        textField.setCaretColor(theme.textPrimary());
        textField.setBackground(textField.isEnabled() ? theme.secondarySurface() : theme.surface());
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        textField.setFont(new Font("Inter", Font.PLAIN, 14));
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
    }

    private void styleComboBox(JComboBox<?> comboBox, DefaultListCellRenderer renderer) {
        ThemedComboBoxUI.apply(comboBox, theme);
        comboBox.setUI(new ThemedComboBoxUI(theme));
        comboBox.setRenderer(renderer);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        comboBox.setFont(new Font("Inter", Font.PLAIN, 14));
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
        ThemedComboBoxUI.styleEditableEditor(comboBox, theme);
        editorField.setFont(new Font("Inter", Font.PLAIN, 14));
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
        if (button == prepareButton) {
            button.setIcon(new ToolbarIcon(ToolbarIcon.Type.DOWNLOAD, 16,
                    button.isEnabled() ? theme.textPrimary() : theme.textSecondary()));
        } else if (button == launchButton) {
            button.setIcon(new ToolbarIcon(ToolbarIcon.Type.MEDIA_PLAY_PAUSE, 16,
                    button.isEnabled() ? theme.actionForeground() : theme.textSecondary()));
        }
        boolean iconOnly = button.getIcon() != null && (button.getText() == null || button.getText().isBlank());
        boolean hasIconAndText = button.getIcon() != null && button.getText() != null && !button.getText().isBlank();
        ButtonStyler.applyStandard(button, theme, primary, iconOnly, hasIconAndText);
    }

    private void updateTargetButtonTexts() {
        for (Map.Entry<ScrcpyLaunchRequest.LaunchTarget, JToggleButton> entry : targetButtons.entrySet()) {
            entry.getValue().setText(targetButtonText(entry.getKey()));
        }
    }

    private String targetButtonText(ScrcpyLaunchRequest.LaunchTarget target) {
        String title = Messages.text(target.messageKey());
        String descriptionKey = switch (target) {
            case DEVICE_DISPLAY -> "scrcpy.target.display.description";
            case VIRTUAL_DISPLAY -> "scrcpy.target.virtual.description";
            case CAMERA -> "scrcpy.target.camera.description";
        };
        return "<html><center><b>" + title + "</b><br><span style='font-size:10px'>"
                + Messages.text(descriptionKey)
                + "</span></center></html>";
    }

    private void updateTargetButtonSelection() {
        ScrcpyLaunchRequest.LaunchTarget selectedTarget = getSelectedLaunchTarget();
        for (Map.Entry<ScrcpyLaunchRequest.LaunchTarget, JToggleButton> entry : targetButtons.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == selectedTarget);
        }
    }

    private void styleTargetButtons() {
        for (JToggleButton button : targetButtons.values()) {
            styleTargetButton(button);
        }
    }

    private void styleTargetButton(JToggleButton button) {
        boolean selected = button.isSelected();
        boolean hovered = button.isEnabled() && button.getModel().isRollover();
        java.awt.Color background = selected
                ? theme.secondarySurface()
                : ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.62d);
        if (hovered) {
            background = ThemeUtils.blend(background, theme.selectionBackground(), 0.24d);
        }
        java.awt.Color foreground = button.isEnabled()
                ? theme.textPrimary()
                : theme.textSecondary();
        java.awt.Color accent = selected ? theme.actionBackground() : theme.border();
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFont(new Font("Inter", Font.PLAIN, 13));
        button.setIcon(new ToolbarIcon(
                (ToolbarIcon.Type) button.getClientProperty("iconType"),
                34,
                selected ? theme.actionBackground() : theme.textPrimary()));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
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
            ThemedComboBoxUI.applyRendererColors(this, list, theme, isSelected, index);

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
            ThemedComboBoxUI.applyRendererColors(this, list, theme, isSelected, index);

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
