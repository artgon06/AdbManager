package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;

import com.adbmanager.logic.model.ControlState;
import com.adbmanager.logic.model.DeviceSoundMode;
import com.adbmanager.view.Messages;

public class ControlPanel extends JPanel {

    private static final Font BASE_TEXT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final ScrollableContentPanel content = new ScrollableContentPanel();
    private final JScrollPane scrollPane = new JScrollPane(content);

    private final JPanel quickActionsPanel = new JPanel();
    private final JButton backButton = new JButton();
    private final JButton homeButton = new JButton();
    private final JButton recentsButton = new JButton();
    private final JButton powerButton = new JButton();
    private final JButton volumeUpButton = new JButton();
    private final JButton volumeDownButton = new JButton();
    private final JButton assistantButton = new JButton();
    private final JButton cameraButton = new JButton();

    private final JPanel mediaPanel = new JPanel();
    private final JLabel brightnessLabel = new JLabel();
    private final JLabel brightnessValueLabel = new JLabel("-");
    private final JSlider brightnessSlider = new JSlider(0, 255, 128);
    private final JLabel volumeLabel = new JLabel();
    private final JLabel volumeValueLabel = new JLabel("-");
    private final JSlider volumeSlider = new JSlider(0, 15, 5);
    private final JLabel autoApplyHintLabel = new JLabel();

    private final JPanel soundModePanel = new JPanel();
    private final JLabel soundModeLabel = new JLabel();
    private final JToggleButton soundNormalButton = new JToggleButton();
    private final JToggleButton soundVibrateButton = new JToggleButton();
    private final JToggleButton soundSilentButton = new JToggleButton();
    private final ButtonGroup soundModeGroup = new ButtonGroup();

    private final JPanel textInputPanel = new JPanel();
    private final PlaceholderTextField textInputField = new PlaceholderTextField();
    private final JButton sendTextButton = new JButton();

    private final JPanel tvRemotePanel = new JPanel();
    private final JButton tvUpButton = new JButton();
    private final JButton tvLeftButton = new JButton();
    private final JButton tvOkButton = new JButton();
    private final JButton tvRightButton = new JButton();
    private final JButton tvDownButton = new JButton();
    private final JButton tvBackButton = new JButton();
    private final JButton tvHomeButton = new JButton();
    private final JButton tvRecentsButton = new JButton();
    private final JButton tvMenuButton = new JButton();
    private final JButton tvPlayPauseButton = new JButton();
    private final JButton tvChannelUpButton = new JButton();
    private final JButton tvChannelDownButton = new JButton();
    private final JButton tvGuideButton = new JButton();
    private final JButton tvInfoButton = new JButton();
    private final JButton tvMuteButton = new JButton();

    private final JLabel statusLabel = new JLabel(" ");
    private final List<JButton> actionButtons = new ArrayList<>();
    private final Set<JButton> primaryButtons = new HashSet<>();
    private final List<JToggleButton> soundModeButtons = new ArrayList<>();
    private final Map<JButton, ToolbarIcon.Type> buttonIcons = new HashMap<>();

    private AppTheme theme = AppTheme.LIGHT;
    private boolean deviceAvailable;
    private boolean busy;
    private boolean syncingControlState;
    private int lastCommittedBrightness = -1;
    private int lastCommittedVolume = -1;
    private ControlState currentState = ControlState.empty();

    private ActionListener brightnessCommitAction = event -> {
    };
    private ActionListener volumeCommitAction = event -> {
    };
    private ActionListener soundModeAction = event -> {
    };

    public ControlPanel() {
        buildPanel();
        bindValuePreview();
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
        clearControlState();
    }

    public void setRefreshAction(ActionListener actionListener) {
        // Deprecated in UI: kept for API compatibility.
    }

    public void setQuickKeyEventAction(ActionListener actionListener) {
        for (JButton button : List.of(
                backButton,
                homeButton,
                recentsButton,
                powerButton,
                volumeUpButton,
                volumeDownButton,
                assistantButton,
                cameraButton,
                tvUpButton,
                tvLeftButton,
                tvOkButton,
                tvRightButton,
                tvDownButton,
                tvBackButton,
                tvHomeButton,
                tvRecentsButton,
                tvMenuButton,
                tvPlayPauseButton,
                tvChannelUpButton,
                tvChannelDownButton,
                tvGuideButton,
                tvInfoButton,
                tvMuteButton)) {
            button.addActionListener(actionListener);
        }
    }

    public void setSendTextAction(ActionListener actionListener) {
        sendTextButton.addActionListener(actionListener);
    }

    public void setApplyBrightnessAction(ActionListener actionListener) {
        brightnessCommitAction = actionListener == null ? event -> {
        } : actionListener;
    }

    public void setApplyVolumeAction(ActionListener actionListener) {
        volumeCommitAction = actionListener == null ? event -> {
        } : actionListener;
    }

    public void setApplySoundModeAction(ActionListener actionListener) {
        soundModeAction = actionListener == null ? event -> {
        } : actionListener;
    }

    public void setTapAction(ActionListener actionListener) {
        // Removed from UI on purpose.
    }

    public void setSwipeAction(ActionListener actionListener) {
        // Removed from UI on purpose.
    }

    public void setKeyEventAction(ActionListener actionListener) {
        // Removed from UI on purpose.
    }

    public void setRawInputAction(ActionListener actionListener) {
        // Removed from UI on purpose.
    }

    public String getTextInputValue() {
        return textInputField.getText().trim();
    }

    public int getBrightnessValue() {
        return brightnessSlider.getValue();
    }

    public int getVolumeValue() {
        return volumeSlider.getValue();
    }

    public DeviceSoundMode getSelectedSoundMode() {
        if (soundNormalButton.isSelected()) {
            return DeviceSoundMode.NORMAL;
        }
        if (soundVibrateButton.isSelected()) {
            return DeviceSoundMode.VIBRATE;
        }
        if (soundSilentButton.isSelected()) {
            return DeviceSoundMode.SILENT;
        }
        return DeviceSoundMode.NORMAL;
    }

    public Integer getTapX() {
        return null;
    }

    public Integer getTapY() {
        return null;
    }

    public Integer getSwipeX1() {
        return null;
    }

    public Integer getSwipeY1() {
        return null;
    }

    public Integer getSwipeX2() {
        return null;
    }

    public Integer getSwipeY2() {
        return null;
    }

    public Integer getSwipeDurationMs() {
        return null;
    }

    public String getManualKeyEvent() {
        return "";
    }

    public String getRawInputCommand() {
        return "";
    }

    public void setDeviceAvailable(boolean available) {
        deviceAvailable = available;
        updateControlStates();
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        updateControlStates();
    }

    public void setStatus(String message, boolean error) {
        String normalized = message == null ? "" : message.trim();
        statusLabel.putClientProperty("error", error);
        statusLabel.setText(normalized.isBlank() ? " " : normalized);
        styleStatusLabel();
    }

    public void setControlState(ControlState controlState) {
        currentState = controlState == null ? ControlState.empty() : controlState;
        syncingControlState = true;
        try {
            brightnessSlider.setMaximum(Math.max(1, currentState.brightnessMax()));
            brightnessSlider.setValue(currentState.brightnessLevel());
            volumeSlider.setMaximum(Math.max(1, currentState.mediaVolumeMax()));
            volumeSlider.setValue(currentState.mediaVolumeLevel());

            DeviceSoundMode soundMode = currentState.soundMode();
            if (soundMode == DeviceSoundMode.VIBRATE) {
                soundVibrateButton.setSelected(true);
            } else if (soundMode == DeviceSoundMode.SILENT) {
                soundSilentButton.setSelected(true);
            } else {
                soundNormalButton.setSelected(true);
            }
        } finally {
            syncingControlState = false;
        }

        lastCommittedBrightness = brightnessSlider.getValue();
        lastCommittedVolume = volumeSlider.getValue();
        updateBrightnessLabel();
        updateVolumeLabel();
        styleSoundModeButtons();
    }

    public void clearControlState() {
        currentState = ControlState.empty();
        setControlState(currentState);
    }

    public void refreshTexts() {
        titleLabel.setText(Messages.text("control.title"));
        subtitleLabel.setText(Messages.text("control.subtitle"));

        quickActionsPanel.setBorder(createSectionBorder(Messages.text("control.quick.title")));
        mediaPanel.setBorder(createSectionBorder(Messages.text("control.media.title")));
        soundModePanel.setBorder(createSectionBorder(Messages.text("control.sound.title")));
        textInputPanel.setBorder(createSectionBorder(Messages.text("control.text.title")));
        tvRemotePanel.setBorder(createSectionBorder(Messages.text("control.tv.title")));

        backButton.setText(Messages.text("control.quick.back"));
        homeButton.setText(Messages.text("control.quick.home"));
        recentsButton.setText(Messages.text("control.quick.recents"));
        powerButton.setText(Messages.text("control.quick.power"));
        volumeUpButton.setText(Messages.text("control.quick.volumeUp"));
        volumeDownButton.setText(Messages.text("control.quick.volumeDown"));
        assistantButton.setText(Messages.text("control.quick.assistant"));
        cameraButton.setText(Messages.text("control.quick.camera"));

        sendTextButton.setText(Messages.text("control.text.send"));
        textInputField.setPlaceholder(Messages.text("control.placeholder.text"));
        textInputField.setToolTipText(Messages.text("control.text.hint"));

        brightnessLabel.setText(Messages.text("control.media.brightness"));
        volumeLabel.setText(Messages.text("control.media.volume"));
        autoApplyHintLabel.setText(Messages.text("control.media.autoApplyHint"));

        soundModeLabel.setText(Messages.text("control.sound.mode"));
        soundNormalButton.setText(Messages.text(DeviceSoundMode.NORMAL.messageKey()));
        soundVibrateButton.setText(Messages.text(DeviceSoundMode.VIBRATE.messageKey()));
        soundSilentButton.setText(Messages.text(DeviceSoundMode.SILENT.messageKey()));

        tvUpButton.setText(Messages.text("control.tv.up"));
        tvLeftButton.setText(Messages.text("control.tv.left"));
        tvOkButton.setText(Messages.text("control.tv.ok"));
        tvRightButton.setText(Messages.text("control.tv.right"));
        tvDownButton.setText(Messages.text("control.tv.down"));
        tvBackButton.setText(Messages.text("control.tv.back"));
        tvHomeButton.setText(Messages.text("control.tv.home"));
        tvRecentsButton.setText(Messages.text("control.tv.recents"));
        tvMenuButton.setText(Messages.text("control.tv.menu"));
        tvPlayPauseButton.setText(Messages.text("control.tv.playPause"));
        tvChannelUpButton.setText(Messages.text("control.tv.channelUp"));
        tvChannelDownButton.setText(Messages.text("control.tv.channelDown"));
        tvGuideButton.setText(Messages.text("control.tv.guide"));
        tvInfoButton.setText(Messages.text("control.tv.info"));
        tvMuteButton.setText(Messages.text("control.tv.mute"));
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

        titleLabel.setForeground(theme.textPrimary());
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        subtitleLabel.setForeground(theme.textSecondary());
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        autoApplyHintLabel.setForeground(theme.textSecondary());
        autoApplyHintLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        styleSection(quickActionsPanel, Messages.text("control.quick.title"));
        styleSection(mediaPanel, Messages.text("control.media.title"));
        styleSection(soundModePanel, Messages.text("control.sound.title"));
        styleSection(textInputPanel, Messages.text("control.text.title"));
        styleSection(tvRemotePanel, Messages.text("control.tv.title"));

        styleFormLabel(brightnessLabel);
        styleFormLabel(volumeLabel);
        styleFormLabel(soundModeLabel);

        brightnessValueLabel.setForeground(theme.textPrimary());
        brightnessValueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        volumeValueLabel.setForeground(theme.textPrimary());
        volumeValueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        styleTextField(textInputField);
        styleSlider(brightnessSlider);
        styleSlider(volumeSlider);

        for (JButton button : actionButtons) {
            styleActionButton(button, primaryButtons.contains(button));
        }
        styleSoundModeButtons();
        styleStatusLabel();
        repaint();
    }

    private void buildPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        header.add(titleLabel);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitleLabel);
        add(header, BorderLayout.NORTH);

        content.setOpaque(false);
        content.setLayout(new GridBagLayout());
        content.setBorder(new EmptyBorder(16, 0, 0, 0));

        buildQuickActionsPanel();
        buildMediaPanel();
        buildSoundModePanel();
        buildTextInputPanel();
        buildTvRemotePanel();

        JPanel leftColumn = new JPanel();
        leftColumn.setOpaque(false);
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.add(quickActionsPanel);
        leftColumn.add(Box.createVerticalStrut(12));
        leftColumn.add(mediaPanel);
        leftColumn.add(Box.createVerticalStrut(12));
        leftColumn.add(soundModePanel);
        leftColumn.add(Box.createVerticalStrut(12));
        leftColumn.add(textInputPanel);
        leftColumn.add(Box.createVerticalGlue());

        JPanel rightColumn = new JPanel();
        rightColumn.setOpaque(false);
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.add(tvRemotePanel);
        rightColumn.add(Box.createVerticalGlue());

        GridBagConstraints leftConstraints = new GridBagConstraints();
        leftConstraints.gridx = 0;
        leftConstraints.gridy = 0;
        leftConstraints.weightx = 0.52;
        leftConstraints.weighty = 1.0;
        leftConstraints.fill = GridBagConstraints.BOTH;
        leftConstraints.insets = new Insets(0, 0, 0, 8);
        content.add(leftColumn, leftConstraints);

        GridBagConstraints rightConstraints = new GridBagConstraints();
        rightConstraints.gridx = 1;
        rightConstraints.gridy = 0;
        rightConstraints.weightx = 0.48;
        rightConstraints.weighty = 1.0;
        rightConstraints.fill = GridBagConstraints.BOTH;
        rightConstraints.insets = new Insets(0, 8, 0, 0);
        content.add(rightColumn, rightConstraints);

        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        statusLabel.setBorder(new EmptyBorder(12, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void buildQuickActionsPanel() {
        quickActionsPanel.setLayout(new BoxLayout(quickActionsPanel, BoxLayout.Y_AXIS));
        quickActionsPanel.setAlignmentX(LEFT_ALIGNMENT);
        quickActionsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        backButton.setActionCommand("KEYCODE_BACK");
        homeButton.setActionCommand("KEYCODE_HOME");
        recentsButton.setActionCommand("KEYCODE_APP_SWITCH");
        powerButton.setActionCommand("KEYCODE_POWER");
        volumeUpButton.setActionCommand("KEYCODE_VOLUME_UP");
        volumeDownButton.setActionCommand("KEYCODE_VOLUME_DOWN");
        assistantButton.setActionCommand("KEYCODE_ASSIST");
        cameraButton.setActionCommand("KEYCODE_CAMERA");

        JPanel navigationRow = new JPanel(new GridLayout(1, 3, 10, 0));
        navigationRow.setOpaque(false);
        navigationRow.setAlignmentX(LEFT_ALIGNMENT);
        navigationRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        navigationRow.add(backButton);
        navigationRow.add(homeButton);
        navigationRow.add(recentsButton);

        JPanel hardwareRow = new JPanel(new GridLayout(1, 5, 10, 0));
        hardwareRow.setOpaque(false);
        hardwareRow.setAlignmentX(LEFT_ALIGNMENT);
        hardwareRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        hardwareRow.add(powerButton);
        hardwareRow.add(volumeUpButton);
        hardwareRow.add(volumeDownButton);
        hardwareRow.add(assistantButton);
        hardwareRow.add(cameraButton);

        configureActionButton(backButton, false, ToolbarIcon.Type.OPEN);
        configureActionButton(homeButton, false, ToolbarIcon.Type.HOME);
        configureActionButton(recentsButton, false, ToolbarIcon.Type.APPS);
        configureActionButton(powerButton, false, ToolbarIcon.Type.POWER);
        configureActionButton(volumeUpButton, false, ToolbarIcon.Type.ADD);
        configureActionButton(volumeDownButton, false, ToolbarIcon.Type.CLEAR_CACHE);
        configureActionButton(assistantButton, false, ToolbarIcon.Type.SETTINGS);
        configureActionButton(cameraButton, false, ToolbarIcon.Type.DISPLAY);

        quickActionsPanel.add(navigationRow);
        quickActionsPanel.add(Box.createVerticalStrut(10));
        quickActionsPanel.add(hardwareRow);
    }

    private void buildMediaPanel() {
        mediaPanel.setLayout(new GridBagLayout());
        mediaPanel.setAlignmentX(LEFT_ALIGNMENT);
        mediaPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 8, 10);
        mediaPanel.add(brightnessLabel, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        mediaPanel.add(brightnessSlider, constraints);

        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 10, 8, 0);
        mediaPanel.add(brightnessValueLabel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 8, 10);
        mediaPanel.add(volumeLabel, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        mediaPanel.add(volumeSlider, constraints);

        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 10, 8, 0);
        mediaPanel.add(volumeValueLabel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(2, 0, 0, 0);
        mediaPanel.add(autoApplyHintLabel, constraints);
    }

    private void buildSoundModePanel() {
        soundModePanel.setLayout(new BorderLayout(10, 0));
        soundModePanel.setAlignmentX(LEFT_ALIGNMENT);
        soundModePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JPanel controlsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        controlsPanel.setOpaque(false);

        configureToggleButton(soundNormalButton, DeviceSoundMode.NORMAL);
        configureToggleButton(soundVibrateButton, DeviceSoundMode.VIBRATE);
        configureToggleButton(soundSilentButton, DeviceSoundMode.SILENT);

        controlsPanel.add(soundNormalButton);
        controlsPanel.add(soundVibrateButton);
        controlsPanel.add(soundSilentButton);

        soundModePanel.add(soundModeLabel, BorderLayout.WEST);
        soundModePanel.add(controlsPanel, BorderLayout.CENTER);
    }

    private void buildTextInputPanel() {
        textInputPanel.setLayout(new BorderLayout(10, 0));
        textInputPanel.setAlignmentX(LEFT_ALIGNMENT);
        textInputPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        configureActionButton(sendTextButton, true, ToolbarIcon.Type.OPEN);
        textInputPanel.add(textInputField, BorderLayout.CENTER);
        textInputPanel.add(sendTextButton, BorderLayout.EAST);
    }

    private void buildTvRemotePanel() {
        tvRemotePanel.setLayout(new BoxLayout(tvRemotePanel, BoxLayout.Y_AXIS));
        tvRemotePanel.setAlignmentX(LEFT_ALIGNMENT);
        tvRemotePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        tvUpButton.setActionCommand("KEYCODE_DPAD_UP");
        tvLeftButton.setActionCommand("KEYCODE_DPAD_LEFT");
        tvOkButton.setActionCommand("KEYCODE_DPAD_CENTER");
        tvRightButton.setActionCommand("KEYCODE_DPAD_RIGHT");
        tvDownButton.setActionCommand("KEYCODE_DPAD_DOWN");
        tvBackButton.setActionCommand("KEYCODE_BACK");
        tvHomeButton.setActionCommand("KEYCODE_HOME");
        tvRecentsButton.setActionCommand("KEYCODE_APP_SWITCH");
        tvMenuButton.setActionCommand("KEYCODE_MENU");
        tvPlayPauseButton.setActionCommand("KEYCODE_MEDIA_PLAY_PAUSE");
        tvChannelUpButton.setActionCommand("KEYCODE_CHANNEL_UP");
        tvChannelDownButton.setActionCommand("KEYCODE_CHANNEL_DOWN");
        tvGuideButton.setActionCommand("KEYCODE_GUIDE");
        tvInfoButton.setActionCommand("KEYCODE_INFO");
        tvMuteButton.setActionCommand("KEYCODE_VOLUME_MUTE");

        configureActionButton(tvUpButton, false, ToolbarIcon.Type.ARROW_UP);
        configureActionButton(tvLeftButton, false, ToolbarIcon.Type.ARROW_LEFT);
        configureActionButton(tvOkButton, true, ToolbarIcon.Type.ENABLE);
        configureActionButton(tvRightButton, false, ToolbarIcon.Type.ARROW_RIGHT);
        configureActionButton(tvDownButton, false, ToolbarIcon.Type.ARROW_DOWN);
        configureActionButton(tvBackButton, false, ToolbarIcon.Type.OPEN);
        configureActionButton(tvHomeButton, false, ToolbarIcon.Type.HOME);
        configureActionButton(tvRecentsButton, false, ToolbarIcon.Type.APPS);
        configureActionButton(tvMenuButton, false, ToolbarIcon.Type.SETTINGS);
        configureActionButton(tvPlayPauseButton, false, ToolbarIcon.Type.REFRESH);
        configureActionButton(tvChannelUpButton, false, ToolbarIcon.Type.ADD);
        configureActionButton(tvChannelDownButton, false, ToolbarIcon.Type.CLEAR_CACHE);
        configureActionButton(tvGuideButton, false, ToolbarIcon.Type.DISPLAY);
        configureActionButton(tvInfoButton, false, ToolbarIcon.Type.SYSTEM);
        configureActionButton(tvMuteButton, false, ToolbarIcon.Type.STOP);

        tvRemotePanel.add(buildTvDpadPanel());
        tvRemotePanel.add(Box.createVerticalStrut(12));
        tvRemotePanel.add(buildTvRow(List.of(tvBackButton, tvHomeButton, tvRecentsButton, tvMenuButton)));
        tvRemotePanel.add(Box.createVerticalStrut(10));
        tvRemotePanel.add(buildTvRow(List.of(tvPlayPauseButton, tvMuteButton, tvInfoButton, tvGuideButton)));
        tvRemotePanel.add(Box.createVerticalStrut(10));
        tvRemotePanel.add(buildTvRow(List.of(tvChannelUpButton, tvChannelDownButton)));
    }

    private JPanel buildTvDpadPanel() {
        JPanel dpadPanel = new JPanel(new GridBagLayout());
        dpadPanel.setOpaque(false);
        dpadPanel.setAlignmentX(LEFT_ALIGNMENT);
        dpadPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        Dimension dpadButtonSize = new Dimension(120, 44);
        tvUpButton.setPreferredSize(dpadButtonSize);
        tvLeftButton.setPreferredSize(dpadButtonSize);
        tvOkButton.setPreferredSize(new Dimension(140, 44));
        tvRightButton.setPreferredSize(dpadButtonSize);
        tvDownButton.setPreferredSize(dpadButtonSize);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.gridx = 1;
        constraints.gridy = 0;
        dpadPanel.add(tvUpButton, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        dpadPanel.add(tvLeftButton, constraints);

        constraints.gridx = 1;
        constraints.gridy = 1;
        dpadPanel.add(tvOkButton, constraints);

        constraints.gridx = 2;
        constraints.gridy = 1;
        dpadPanel.add(tvRightButton, constraints);

        constraints.gridx = 1;
        constraints.gridy = 2;
        dpadPanel.add(tvDownButton, constraints);
        return dpadPanel;
    }

    private JPanel buildTvRow(List<JButton> buttons) {
        JPanel row = new JPanel(new GridLayout(1, buttons.size(), 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        for (JButton button : buttons) {
            row.add(button);
        }
        return row;
    }

    private void bindValuePreview() {
        ChangeListener sliderListener = event -> {
            updateBrightnessLabel();
            updateVolumeLabel();
            if (syncingControlState) {
                return;
            }

            if (!brightnessSlider.getValueIsAdjusting() && brightnessSlider.getValue() != lastCommittedBrightness) {
                lastCommittedBrightness = brightnessSlider.getValue();
                brightnessCommitAction.actionPerformed(
                        new ActionEvent(brightnessSlider, ActionEvent.ACTION_PERFORMED, "brightness.commit"));
            }
            if (!volumeSlider.getValueIsAdjusting() && volumeSlider.getValue() != lastCommittedVolume) {
                lastCommittedVolume = volumeSlider.getValue();
                volumeCommitAction.actionPerformed(
                        new ActionEvent(volumeSlider, ActionEvent.ACTION_PERFORMED, "volume.commit"));
            }
        };
        brightnessSlider.addChangeListener(sliderListener);
        volumeSlider.addChangeListener(sliderListener);
    }

    private void updateControlStates() {
        boolean enabled = deviceAvailable && !busy;

        for (JButton button : actionButtons) {
            button.setEnabled(enabled);
            styleActionButton(button, primaryButtons.contains(button));
        }
        for (JToggleButton button : soundModeButtons) {
            button.setEnabled(enabled);
        }

        textInputField.setEnabled(enabled);
        styleTextField(textInputField);
        sendTextButton.setEnabled(enabled);
        styleActionButton(sendTextButton, true);

        brightnessSlider.setEnabled(enabled);
        volumeSlider.setEnabled(enabled);
        styleSoundModeButtons();
        styleSlider(brightnessSlider);
        styleSlider(volumeSlider);
    }

    private void configureActionButton(JButton button, boolean primary, ToolbarIcon.Type iconType) {
        button.setUI(new BasicButtonUI());
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.getModel().addChangeListener(event -> styleActionButton(button, primary));
        button.setFont(BASE_TEXT_FONT);
        actionButtons.add(button);
        if (iconType != null) {
            buttonIcons.put(button, iconType);
        }
        if (primary) {
            primaryButtons.add(button);
        }
    }

    private void configureToggleButton(JToggleButton button, DeviceSoundMode mode) {
        button.setActionCommand(mode.name());
        button.setUI(new BasicToggleButtonUI());
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(BASE_TEXT_FONT);
        button.addActionListener(event -> {
            styleSoundModeButtons();
            if (!syncingControlState && button.isSelected()) {
                soundModeAction.actionPerformed(event);
            }
        });
        soundModeGroup.add(button);
        soundModeButtons.add(button);
    }

    private void styleSection(JPanel panel, String title) {
        panel.setOpaque(true);
        panel.setBackground(theme.background());
        panel.setBorder(createSectionBorder(title));
    }

    private void styleFormLabel(JLabel label) {
        label.setForeground(theme.textSecondary());
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
    }

    private void styleTextField(JTextField field) {
        field.setForeground(field.isEnabled() ? theme.textPrimary() : theme.textSecondary());
        field.setCaretColor(theme.textPrimary());
        field.setBackground(field.isEnabled() ? theme.secondarySurface() : theme.surface());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(field.isEnabled() ? theme.border() : theme.disabledBorder(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.setFont(BASE_TEXT_FONT);
        field.repaint();
    }

    private void styleSlider(JSlider slider) {
        slider.setOpaque(false);
        slider.setForeground(slider.isEnabled() ? theme.actionBackground() : theme.textSecondary());
        slider.setBackground(theme.background());
    }

    private void styleActionButton(JButton button, boolean primary) {
        boolean hovered = button.isEnabled() && button.getModel().isRollover();
        if (button.isEnabled()) {
            Color background = primary
                    ? theme.actionBackground()
                    : ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.84d);
            if (hovered) {
                background = ThemeUtils.blend(background, theme.selectionBackground(), primary ? 0.18d : 0.24d);
            }
            Color foreground = primary ? theme.actionForeground() : theme.textPrimary();
            button.setBackground(background);
            button.setForeground(foreground);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primary ? background : theme.border(), 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            applyButtonIcon(button, foreground);
            return;
        }

        button.setBackground(theme.secondarySurface());
        button.setForeground(theme.textSecondary());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.disabledBorder(), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        applyButtonIcon(button, theme.textSecondary());
    }

    private void applyButtonIcon(JButton button, Color color) {
        ToolbarIcon.Type iconType = buttonIcons.get(button);
        if (iconType == null) {
            button.setIcon(null);
            return;
        }
        button.setIcon(new ToolbarIcon(iconType, 14, color));
        button.setHorizontalTextPosition(JButton.RIGHT);
        button.setIconTextGap(8);
    }

    private void styleSoundModeButtons() {
        for (JToggleButton button : soundModeButtons) {
            boolean selected = button.isSelected();
            boolean hovered = button.getModel().isRollover() && button.isEnabled();
            Color background = selected
                    ? theme.actionBackground()
                    : ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.84d);
            if (hovered) {
                background = ThemeUtils.blend(background, theme.selectionBackground(), selected ? 0.16d : 0.24d);
            }
            button.setOpaque(true);
            button.setBackground(button.isEnabled() ? background : theme.surface());
            button.setForeground(button.isEnabled()
                    ? (selected ? theme.actionForeground() : theme.textPrimary())
                    : theme.textSecondary());
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(button.isEnabled()
                            ? (selected ? background : theme.border())
                            : theme.disabledBorder(), 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        }
    }

    private void styleStatusLabel() {
        boolean error = Boolean.TRUE.equals(statusLabel.getClientProperty("error"));
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        statusLabel.setForeground(error ? new Color(223, 76, 76) : theme.textSecondary());
    }

    private void updateBrightnessLabel() {
        brightnessValueLabel.setText(brightnessSlider.getValue() + " / " + brightnessSlider.getMaximum());
    }

    private void updateVolumeLabel() {
        volumeValueLabel.setText(volumeSlider.getValue() + " / " + volumeSlider.getMaximum());
    }

    private TitledBorder createSectionBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 18),
                theme.textPrimary());
    }

    private static final class PlaceholderTextField extends JTextField {

        private String placeholder = "";
        private Color placeholderColor = new Color(130, 138, 150);

        PlaceholderTextField() {
            setFont(BASE_TEXT_FONT);
        }

        void setPlaceholder(String placeholder) {
            this.placeholder = placeholder == null ? "" : placeholder;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (placeholder.isBlank() || !getText().isEmpty() || isFocusOwner()) {
                return;
            }
            Graphics2D g2d = (Graphics2D) graphics.create();
            g2d.setFont(getFont());
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setColor(isEnabled() ? placeholderColor : placeholderColor.darker());
            Insets insets = getInsets();
            int baseline = getHeight() / 2 + g2d.getFontMetrics().getAscent() / 2 - 2;
            g2d.drawString(placeholder, insets.left + 2, baseline);
            g2d.dispose();
        }
    }
}
