package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Cursor;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;

import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.logic.model.DisplayInfo;
import com.adbmanager.logic.model.DisplayOverrideSuggestion;
import com.adbmanager.logic.model.InstalledApp;
import com.adbmanager.logic.model.ScrcpyCamera;
import com.adbmanager.logic.model.ScrcpyLaunchRequest;
import com.adbmanager.logic.model.ScrcpyStatus;
import com.adbmanager.view.Messages;

public class DisplayPanel extends JPanel {

    private static final String FIELD_DEVICE_TYPE = "deviceType";
    private static final String FIELD_CURRENT_RESOLUTION = "currentResolution";
    private static final String FIELD_PHYSICAL_RESOLUTION = "physicalResolution";
    private static final String FIELD_DENSITY = "density";
    private static final String FIELD_PHYSICAL_DENSITY = "physicalDensity";
    private static final String FIELD_SMALLEST_WIDTH = "smallestWidth";
    private static final String FIELD_SCREEN_TIMEOUT = "screenTimeout";
    private static final String FIELD_REFRESH_RATE = "refreshRate";
    private static final String FIELD_SUPPORTED_REFRESH_RATES = "supportedRefreshRates";

    private final JLabel titleLabel = new JLabel();
    private final JPanel headerPanel = new JPanel(new BorderLayout());
    private final JPanel headerActionsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
    private final JPanel metricsPanel = new JPanel(new BorderLayout());
    private final JPanel metricsContent = new JPanel();
    private final JPanel controlsPanel = new JPanel(new BorderLayout());
    private final JPanel controlsContent = new JPanel();
    private final JPanel suggestionsCard = new JPanel(new BorderLayout(12, 0));
    private final JPanel darkModePanel = new JPanel(new BorderLayout(12, 0));
    private final JPanel scrcpyPanelContainer = new JPanel(new BorderLayout());
    private final ScrcpyLauncherPanel scrcpyPanel = new ScrcpyLauncherPanel();
    private final JLabel inputTitleLabel = new JLabel();
    private final JLabel originalAspectLabel = new JLabel();
    private final JLabel customAspectLabel = new JLabel();
    private final JPanel ratioPanel = new JPanel();
    private final JLabel widthLabel = new JLabel();
    private final JLabel heightLabel = new JLabel();
    private final JLabel densityLabel = new JLabel();
    private final JLabel timeoutLabel = new JLabel();
    private final JLabel darkModeTitleLabel = new JLabel();
    private final JCheckBox darkModeToggle = new JCheckBox();
    private final JTextField widthField = new JTextField();
    private final JTextField heightField = new JTextField();
    private final JTextField densityField = new JTextField();
    private final JTextField timeoutField = new JTextField();
    private final JButton applyButton = new JButton();
    private final JButton resetButton = new JButton();
    private final JPanel suggestionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    private final List<JButton> suggestionButtons = new ArrayList<>();
    private final List<JPanel> rowPanels = new ArrayList<>();
    private final List<JLabel> dynamicValueLabels = new ArrayList<>();
    private final Map<String, JLabel> fieldLabels = new LinkedHashMap<>();
    private final Map<String, JLabel> valueLabels = new LinkedHashMap<>();
    private DeviceDetails currentDetails;
    private AppTheme theme = AppTheme.LIGHT;
    private boolean syncingDarkModeToggle;
    private ActionListener deviceDarkModeAction = event -> {
    };

    public DisplayPanel() {
        buildPanel();
        bindAspectRatioPreview();
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
        clearDeviceDetails();
    }

    public void setApplyDisplayAction(ActionListener actionListener) {
        applyButton.addActionListener(actionListener);
    }

    public void setResetDisplayAction(ActionListener actionListener) {
        resetButton.addActionListener(actionListener);
    }

    public void setDeviceDarkModeAction(ActionListener actionListener) {
        deviceDarkModeAction = actionListener == null ? event -> {
        } : actionListener;
    }

    public void setPrepareScrcpyAction(ActionListener actionListener) {
        scrcpyPanel.setPrepareAction(actionListener);
    }

    public void setLaunchScrcpyAction(ActionListener actionListener) {
        scrcpyPanel.setLaunchAction(actionListener);
    }

    public void setBrowseScrcpyRecordPathAction(ActionListener actionListener) {
        scrcpyPanel.setBrowseRecordPathAction(actionListener);
    }

    public void setRefreshScrcpyCamerasAction(ActionListener actionListener) {
        scrcpyPanel.setRefreshCamerasAction(actionListener);
    }

    public void setScrcpyLaunchTargetChangeAction(ActionListener actionListener) {
        scrcpyPanel.setLaunchTargetChangeAction(actionListener);
    }

    public void setScrcpyStartAppToggleAction(ActionListener actionListener) {
        scrcpyPanel.setStartAppToggleAction(actionListener);
    }

    public ScrcpyLauncherPanel getScrcpyLauncherPanel() {
        return scrcpyPanel;
    }

    public Integer getRequestedWidth() {
        return parsePositiveInteger(widthField.getText());
    }

    public Integer getRequestedHeight() {
        return parsePositiveInteger(heightField.getText());
    }

    public Integer getRequestedDensity() {
        return parsePositiveInteger(densityField.getText());
    }

    public Integer getRequestedScreenOffTimeout() {
        return parsePositiveTimeoutMillis(timeoutField.getText());
    }

    public String getRequestedScreenOffTimeoutLabel() {
        return formatTimeoutSeconds(timeoutField.getText());
    }

    public boolean hasRequestedScreenOffTimeout() {
        return !timeoutField.getText().trim().isEmpty();
    }

    public boolean isDeviceDarkModeSelected() {
        return darkModeToggle.isSelected();
    }

    public ScrcpyLaunchRequest getScrcpyLaunchRequest() {
        return scrcpyPanel.getLaunchRequest();
    }

    public boolean shouldLoadScrcpyApplications() {
        return scrcpyPanel.shouldLoadApplications();
    }

    public boolean usesScrcpyCameraSource() {
        return scrcpyPanel.usesCameraSource();
    }

    public void setScrcpyStatus(ScrcpyStatus status) {
        scrcpyPanel.setScrcpyStatus(status);
    }

    public void setScrcpyFeedback(String message, boolean error) {
        scrcpyPanel.setFeedback(message, error);
    }

    public void setScrcpyBusy(boolean busy) {
        scrcpyPanel.setBusy(busy);
    }

    public void setScrcpyDeviceAvailable(boolean available) {
        scrcpyPanel.setDeviceAvailable(available);
    }

    public void setScrcpyRecordPath(String path) {
        scrcpyPanel.setRecordPath(path);
    }

    public void setScrcpyLaunchRequest(ScrcpyLaunchRequest request) {
        scrcpyPanel.applyLaunchRequest(request);
    }

    public void setScrcpyAvailableApps(List<InstalledApp> applications) {
        scrcpyPanel.setAvailableApps(applications);
    }

    public void setScrcpyAvailableCameras(List<ScrcpyCamera> cameras) {
        scrcpyPanel.setAvailableCameras(cameras);
    }

    public void setDisplayControlsEnabled(boolean enabled) {
        widthField.setEnabled(enabled);
        heightField.setEnabled(enabled);
        densityField.setEnabled(enabled);
        timeoutField.setEnabled(enabled);
        applyButton.setEnabled(enabled);
        resetButton.setEnabled(enabled);
        darkModeToggle.setEnabled(enabled && currentDetails != null && currentDetails.displayInfo().hasDarkModeState());
        for (JButton button : suggestionButtons) {
            button.setEnabled(enabled);
            styleSuggestionButton(button);
        }
        styleActionButtons();
        styleDarkModeToggle();
    }

    public void setDeviceDetails(DeviceDetails details) {
        currentDetails = details;
        DisplayInfo displayInfo = details.displayInfo();

        valueLabels.get(FIELD_DEVICE_TYPE).setText(Messages.deviceTypeLabel(details.deviceType()));
        valueLabels.get(FIELD_CURRENT_RESOLUTION).setText(displayInfo.resolutionLabel());
        valueLabels.get(FIELD_PHYSICAL_RESOLUTION).setText(displayInfo.physicalResolutionLabel());
        valueLabels.get(FIELD_DENSITY).setText(displayInfo.densityLabel());
        valueLabels.get(FIELD_PHYSICAL_DENSITY).setText(displayInfo.physicalDensityLabel());
        valueLabels.get(FIELD_SMALLEST_WIDTH).setText(displayInfo.smallestWidthLabel());
        valueLabels.get(FIELD_SCREEN_TIMEOUT).setText(displayInfo.screenOffTimeoutLabel());
        valueLabels.get(FIELD_REFRESH_RATE).setText(displayInfo.refreshRateLabel());
        valueLabels.get(FIELD_SUPPORTED_REFRESH_RATES).setText(displayInfo.supportedRefreshRatesLabel());

        setRequestedDisplayValues(
                displayInfo.widthPx(),
                displayInfo.heightPx(),
                displayInfo.densityDpi(),
                displayInfo.screenOffTimeoutMs());
        syncingDarkModeToggle = true;
        try {
            darkModeToggle.setSelected(Boolean.TRUE.equals(displayInfo.darkModeEnabled()));
        } finally {
            syncingDarkModeToggle = false;
        }
        originalAspectLabel.setText(Messages.format("display.aspect.original", displayInfo.physicalAspectRatioLabel()));
        rebuildSuggestionButtons(displayInfo);
        updateAspectRatioPreview();
        styleDarkModeToggle();
        scrcpyPanel.setDeviceDetails(details);
    }

    public void clearDeviceDetails() {
        currentDetails = null;
        for (JLabel valueLabel : valueLabels.values()) {
            valueLabel.setText("-");
        }
        setRequestedDisplayValues(null, null, null, null);
        syncingDarkModeToggle = true;
        try {
            darkModeToggle.setSelected(false);
        } finally {
            syncingDarkModeToggle = false;
        }
        darkModeToggle.setEnabled(false);
        originalAspectLabel.setText(Messages.format("display.aspect.original", "-"));
        customAspectLabel.setText(Messages.format("display.aspect.custom", "-"));
        rebuildSuggestionButtons(DisplayInfo.empty());
        styleDarkModeToggle();
        scrcpyPanel.clearDeviceDetails();
    }

    public void refreshTexts() {
        titleLabel.setText(Messages.text("display.title"));
        fieldLabels.get(FIELD_DEVICE_TYPE).setText(Messages.text("display.field.deviceType"));
        fieldLabels.get(FIELD_CURRENT_RESOLUTION).setText(Messages.text("display.field.currentResolution"));
        fieldLabels.get(FIELD_PHYSICAL_RESOLUTION).setText(Messages.text("display.field.physicalResolution"));
        fieldLabels.get(FIELD_DENSITY).setText(Messages.text("display.field.density"));
        fieldLabels.get(FIELD_PHYSICAL_DENSITY).setText(Messages.text("display.field.physicalDensity"));
        fieldLabels.get(FIELD_SMALLEST_WIDTH).setText(Messages.text("display.field.smallestWidth"));
        fieldLabels.get(FIELD_SCREEN_TIMEOUT).setText(Messages.text("display.field.screenTimeout"));
        fieldLabels.get(FIELD_REFRESH_RATE).setText(Messages.text("display.field.refreshRate"));
        fieldLabels.get(FIELD_SUPPORTED_REFRESH_RATES).setText(Messages.text("display.field.supportedRefreshRates"));
        inputTitleLabel.setText(Messages.text("display.override.manual"));
        widthLabel.setText(Messages.text("display.override.width"));
        heightLabel.setText(Messages.text("display.override.height"));
        densityLabel.setText(Messages.text("display.override.density"));
        timeoutLabel.setText(Messages.text("display.override.timeout"));
        darkModeTitleLabel.setText(Messages.text("display.deviceDarkMode.title"));
        applyButton.setText(Messages.text("display.override.apply"));
        resetButton.setText(Messages.text("display.override.reset"));
        metricsPanel.setBorder(BorderFactory.createEmptyBorder());
        controlsPanel.setBorder(BorderFactory.createEmptyBorder());
        scrcpyPanelContainer.setBorder(BorderFactory.createEmptyBorder());
        scrcpyPanel.refreshTexts();

        if (currentDetails == null) {
            clearDeviceDetails();
        } else {
            setDeviceDetails(currentDetails);
        }
    }

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        setBackground(theme.background());
        headerPanel.setBackground(theme.background());
        headerActionsPanel.setBackground(theme.background());
        titleLabel.setForeground(theme.textPrimary());
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));

        metricsPanel.setBackground(theme.background());
        metricsContent.setBackground(theme.background());
        controlsPanel.setBackground(theme.background());
        controlsContent.setBackground(theme.background());
        suggestionsCard.setBackground(theme.background());
        darkModePanel.setBackground(theme.background());
        suggestionButtonsPanel.setBackground(theme.background());
        scrcpyPanelContainer.setBackground(theme.background());

        for (JPanel rowPanel : rowPanels) {
            rowPanel.setBackground(theme.background());
        }

        for (JLabel keyLabel : fieldLabels.values()) {
            keyLabel.setForeground(theme.textSecondary());
            keyLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        }

        for (JLabel valueLabel : dynamicValueLabels) {
            valueLabel.setForeground(theme.textPrimary());
            valueLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        }

        inputTitleLabel.setForeground(theme.textPrimary());
        inputTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        originalAspectLabel.setForeground(theme.textSecondary());
        originalAspectLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        customAspectLabel.setForeground(theme.textSecondary());
        customAspectLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        styleFormLabel(widthLabel);
        styleFormLabel(heightLabel);
        styleFormLabel(densityLabel);
        styleFormLabel(timeoutLabel);
        darkModeTitleLabel.setForeground(theme.textPrimary());
        darkModeTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        styleDarkModeToggle();
        styleInputField(widthField);
        styleInputField(heightField);
        styleInputField(densityField);
        styleInputField(timeoutField);
        styleActionButtons();
        styleSuggestionsCard();

        for (JButton suggestionButton : suggestionButtons) {
            styleSuggestionButton(suggestionButton);
        }

        metricsPanel.setBorder(BorderFactory.createEmptyBorder());
        controlsPanel.setBorder(BorderFactory.createEmptyBorder());
        scrcpyPanelContainer.setBorder(BorderFactory.createEmptyBorder());
        scrcpyPanel.applyTheme(theme);
        repaint();
    }

    private void buildPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(28, 32, 28, 32));

        configureActionButton(resetButton);
        configureActionButton(applyButton);
        resetButton.setPreferredSize(new Dimension(92, 38));
        applyButton.setPreferredSize(new Dimension(92, 38));
        headerActionsPanel.setOpaque(false);
        headerActionsPanel.add(resetButton);
        headerActionsPanel.add(applyButton);
        headerPanel.setOpaque(false);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(headerActionsPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(26, 0, 0, 0));

        JPanel leftColumn = new JPanel(new GridBagLayout());
        leftColumn.setOpaque(false);

        buildMetricsPanel();
        buildControlsPanel();

        GridBagConstraints metricsConstraints = new GridBagConstraints();
        metricsConstraints.gridx = 0;
        metricsConstraints.gridy = 0;
        metricsConstraints.weightx = 1.0;
        metricsConstraints.weighty = 0.42;
        metricsConstraints.fill = GridBagConstraints.BOTH;
        metricsConstraints.insets = new Insets(0, 0, 8, 0);
        leftColumn.add(metricsPanel, metricsConstraints);

        GridBagConstraints controlsConstraints = new GridBagConstraints();
        controlsConstraints.gridx = 0;
        controlsConstraints.gridy = 1;
        controlsConstraints.weightx = 1.0;
        controlsConstraints.weighty = 0.58;
        controlsConstraints.fill = GridBagConstraints.BOTH;
        leftColumn.add(controlsPanel, controlsConstraints);

        content.add(leftColumn, buildLeftConstraints());

        add(content, BorderLayout.CENTER);
    }

    private void buildMetricsPanel() {
        metricsContent.setLayout(new GridBagLayout());
        metricsContent.setBorder(new EmptyBorder(0, 0, 18, 0));
        addInfoTile(FIELD_DEVICE_TYPE, 0, 0, 1, new Insets(0, 0, 18, 34));
        addDarkModeTile(2, 0, new Insets(0, 0, 18, 0));
        addInfoTile(FIELD_CURRENT_RESOLUTION, 0, 1, 1, new Insets(0, 0, 18, 34));
        addInfoTile(FIELD_PHYSICAL_RESOLUTION, 1, 1, 1, new Insets(0, 0, 18, 34));
        addInfoTile(FIELD_SMALLEST_WIDTH, 2, 1, 1, new Insets(0, 0, 18, 0));
        addInfoTile(FIELD_DENSITY, 0, 2, 1, new Insets(0, 0, 18, 34));
        addInfoTile(FIELD_PHYSICAL_DENSITY, 1, 2, 1, new Insets(0, 0, 18, 34));
        addInfoTile(FIELD_SCREEN_TIMEOUT, 2, 2, 1, new Insets(0, 0, 18, 0));
        addInfoTile(FIELD_REFRESH_RATE, 0, 3, 1, new Insets(0, 0, 0, 34));
        addInfoTile(FIELD_SUPPORTED_REFRESH_RATES, 1, 3, 2, new Insets(0, 0, 0, 0));
        metricsPanel.add(metricsContent, BorderLayout.CENTER);
    }

    private void addDarkModeTile(int gridX, int gridY, Insets insets) {
        darkModePanel.setOpaque(false);
        darkModeToggle.setOpaque(false);
        darkModeToggle.setFocusPainted(false);
        darkModeToggle.setFocusable(false);
        darkModeToggle.addActionListener(event -> {
            if (!syncingDarkModeToggle) {
                deviceDarkModeAction.actionPerformed(event);
            }
        });
        darkModePanel.add(darkModeTitleLabel, BorderLayout.WEST);
        darkModePanel.add(darkModeToggle, BorderLayout.EAST);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = gridX;
        constraints.gridy = gridY;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = insets;
        metricsContent.add(darkModePanel, constraints);
    }

    private void addInfoTile(String fieldKey, int gridX, int gridY, int gridWidth, Insets insets) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = gridX;
        constraints.gridy = gridY;
        constraints.gridwidth = gridWidth;
        constraints.weightx = gridWidth;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = insets;
        metricsContent.add(createInfoRow(fieldKey), constraints);
    }

    private void buildControlsPanel() {
        controlsContent.setLayout(new GridBagLayout());
        controlsContent.setBorder(new EmptyBorder(0, 0, 0, 0));
        inputTitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        suggestionButtonsPanel.setOpaque(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(18, 0, 0, 0));
        addInlineFormField(formPanel, widthLabel, widthField, 0);
        addInlineFormField(formPanel, heightLabel, heightField, 1);
        addInlineFormField(formPanel, densityLabel, densityField, 2);
        addInlineFormField(formPanel, timeoutLabel, timeoutField, 3);

        ratioPanel.setOpaque(false);
        ratioPanel.setLayout(new BoxLayout(ratioPanel, BoxLayout.X_AXIS));
        ratioPanel.setBorder(new EmptyBorder(16, 0, 0, 0));
        ratioPanel.add(originalAspectLabel);
        ratioPanel.add(Box.createHorizontalStrut(18));
        ratioPanel.add(customAspectLabel);
        ratioPanel.add(Box.createHorizontalGlue());

        suggestionsCard.add(suggestionButtonsPanel, BorderLayout.CENTER);

        addControlContent(inputTitleLabel, 0, 0.0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 10, 0));
        addControlContent(suggestionsCard, 1, 0.0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));
        addControlContent(formPanel, 2, 0.0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));
        addControlContent(ratioPanel, 3, 0.0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));
        addControlContent(Box.createGlue(), 4, 1.0, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));
        controlsPanel.add(controlsContent, BorderLayout.CENTER);
    }

    private void addControlContent(Component component, int gridY, double weightY, int fill, Insets insets) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = gridY;
        constraints.weightx = 1.0;
        constraints.weighty = weightY;
        constraints.fill = fill;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = insets;
        controlsContent.add(component, constraints);
    }

    private void addInlineFormField(JPanel formPanel, JLabel label, JTextField field, int pairIndex) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = pairIndex * 2;
        labelConstraints.gridy = 0;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(0, pairIndex == 0 ? 0 : 12, 0, 10);
        formPanel.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = pairIndex * 2 + 1;
        fieldConstraints.gridy = 0;
        fieldConstraints.weightx = 0.0;
        fieldConstraints.fill = GridBagConstraints.NONE;
        fieldConstraints.insets = new Insets(0, 0, 0, pairIndex == 3 ? 0 : 12);
        formPanel.add(field, fieldConstraints);

        if (pairIndex == 3) {
            GridBagConstraints glueConstraints = new GridBagConstraints();
            glueConstraints.gridx = 8;
            glueConstraints.gridy = 0;
            glueConstraints.weightx = 1.0;
            glueConstraints.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(Box.createHorizontalGlue(), glueConstraints);
        }
    }

    private JPanel createInfoRow(String fieldKey) {
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.Y_AXIS));
        rowPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        JLabel keyLabel = new JLabel();
        JLabel valueLabel = new JLabel("-");
        keyLabel.setAlignmentX(LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);
        rowPanels.add(rowPanel);
        fieldLabels.put(fieldKey, keyLabel);
        valueLabels.put(fieldKey, valueLabel);
        dynamicValueLabels.add(valueLabel);
        rowPanel.add(keyLabel);
        rowPanel.add(Box.createVerticalStrut(5));
        rowPanel.add(valueLabel);
        return rowPanel;
    }

    private void bindAspectRatioPreview() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateAspectRatioPreview();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateAspectRatioPreview();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateAspectRatioPreview();
            }
        };

        widthField.getDocument().addDocumentListener(listener);
        heightField.getDocument().addDocumentListener(listener);
    }

    private void rebuildSuggestionButtons(DisplayInfo displayInfo) {
        suggestionButtonsPanel.removeAll();
        suggestionButtons.clear();

        JLabel suggestionsLabel = new JLabel(Messages.text("display.override.suggestions"));
        suggestionsLabel.setForeground(theme.textSecondary());
        suggestionsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        suggestionsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        suggestionButtonsPanel.add(suggestionsLabel);

        for (DisplayOverrideSuggestion suggestion : buildSuggestions(displayInfo)) {
            JButton button = new JButton(suggestion.commandLabel());
            configureActionButton(button);
            button.addActionListener(event -> {
                widthField.setText(String.valueOf(suggestion.widthPx()));
                heightField.setText(String.valueOf(suggestion.heightPx()));
                densityField.setText(String.valueOf(suggestion.densityDpi()));
                updateAspectRatioPreview();
            });
            suggestionButtons.add(button);
            suggestionButtonsPanel.add(button);
        }

        if (suggestionButtons.isEmpty()) {
            JLabel emptyLabel = new JLabel(Messages.text("display.override.suggestions.empty"));
            emptyLabel.setForeground(theme.textSecondary());
            emptyLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            suggestionButtonsPanel.add(emptyLabel);
        }

        suggestionButtonsPanel.revalidate();
        suggestionButtonsPanel.repaint();
    }

    private List<DisplayOverrideSuggestion> buildSuggestions(DisplayInfo displayInfo) {
        if (!displayInfo.hasPhysicalResolution()) {
            return List.of();
        }

        int baseDensity = displayInfo.physicalDensityDpi() != null
                ? displayInfo.physicalDensityDpi()
                : displayInfo.densityDpi() == null ? 0 : displayInfo.densityDpi();
        if (baseDensity <= 0) {
            return List.of();
        }

        int physicalWidth = displayInfo.physicalWidthPx();
        int physicalHeight = displayInfo.physicalHeightPx();
        int smallestSide = displayInfo.physicalSmallestSidePx();
        int gcd = gcd(physicalWidth, physicalHeight);
        int ratioWidth = physicalWidth / gcd;
        int ratioHeight = physicalHeight / gcd;

        double[] scales = { 0.9d, 0.8d, 0.7d, 0.6d };
        Set<String> seen = new LinkedHashSet<>();
        List<DisplayOverrideSuggestion> suggestions = new ArrayList<>();

        for (double scale : scales) {
            int multiplier = (int) Math.round(Math.min(
                    (physicalWidth * scale) / ratioWidth,
                    (physicalHeight * scale) / ratioHeight));
            if (multiplier <= 0) {
                continue;
            }
            int width = ratioWidth * multiplier;
            int height = ratioHeight * multiplier;
            if (width >= physicalWidth || height >= physicalHeight || !seen.add(width + "x" + height)) {
                continue;
            }
            int targetSmallestSide = Math.min(width, height);
            int density = Math.max(1, (int) Math.round(baseDensity * (targetSmallestSide / (double) smallestSide)));
            suggestions.add(new DisplayOverrideSuggestion(width, height, density));
        }

        return suggestions;
    }

    private void setRequestedDisplayValues(
            Integer widthPx,
            Integer heightPx,
            Integer densityDpi,
            Integer timeoutMs) {
        widthField.setText(widthPx == null || widthPx <= 0 ? "" : String.valueOf(widthPx));
        heightField.setText(heightPx == null || heightPx <= 0 ? "" : String.valueOf(heightPx));
        densityField.setText(densityDpi == null || densityDpi <= 0 ? "" : String.valueOf(densityDpi));
        timeoutField.setText(formatTimeoutSeconds(timeoutMs));
    }

    private void updateAspectRatioPreview() {
        customAspectLabel.setText(Messages.format(
                "display.aspect.custom",
                DisplayInfo.aspectRatioLabel(getRequestedWidth(), getRequestedHeight())));
    }

    private void configureActionButton(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.getModel().addChangeListener(event -> {
            if (button == applyButton) {
                styleActionButton(button, true);
            } else {
                styleSuggestionButton(button);
            }
        });
    }

    private void styleFormLabel(JLabel label) {
        label.setForeground(theme.textSecondary());
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
    }

    private void styleDarkModeToggle() {
        darkModeToggle.setOpaque(false);
        darkModeToggle.setContentAreaFilled(false);
        darkModeToggle.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        darkModeToggle.setForeground(darkModeToggle.isEnabled() ? theme.textPrimary() : theme.textSecondary());
        darkModeToggle.setIcon(new ToggleSwitchIcon(theme, false, darkModeToggle.isEnabled()));
        darkModeToggle.setSelectedIcon(new ToggleSwitchIcon(theme, true, darkModeToggle.isEnabled()));
        darkModeToggle.setDisabledIcon(new ToggleSwitchIcon(theme, false, false));
        darkModeToggle.setDisabledSelectedIcon(new ToggleSwitchIcon(theme, true, false));
    }

    private void styleInputField(JTextField field) {
        field.setForeground(theme.textPrimary());
        field.setCaretColor(theme.textPrimary());
        field.setBackground(theme.secondarySurface());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        field.setColumns(5);
        field.setPreferredSize(new Dimension(84, 36));
        field.setMinimumSize(new Dimension(72, 36));
        field.setMaximumSize(new Dimension(96, 36));
    }

    private void styleSuggestionsCard() {
        suggestionsCard.setOpaque(true);
        suggestionsCard.setBackground(ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.58d));
        suggestionsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
    }

    private void styleActionButtons() {
        styleActionButton(applyButton, true);
        styleActionButton(resetButton, false);
    }

    private void styleSuggestionButton(JButton button) {
        styleActionButton(button, false);
    }

    private void styleActionButton(JButton button, boolean primary) {
        boolean hovered = button.isEnabled() && button.getModel().isRollover();
        if (button == applyButton) {
            button.setIcon(new ToolbarIcon(ToolbarIcon.Type.ENABLE, 16,
                    button.isEnabled() ? theme.actionForeground() : theme.textSecondary()));
        } else if (button == resetButton) {
            button.setIcon(new ToolbarIcon(ToolbarIcon.Type.REFRESH, 16,
                    button.isEnabled() ? theme.textPrimary() : theme.textSecondary()));
        } else {
            button.setIcon(null);
        }
        button.setIconTextGap(8);
        if (button.isEnabled()) {
            java.awt.Color background = primary
                    ? theme.actionBackground()
                    : ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.84d);
            if (hovered) {
                background = ThemeUtils.blend(background, theme.selectionBackground(), primary ? 0.18d : 0.24d);
            }
            button.setBackground(background);
            button.setForeground(primary ? theme.actionForeground() : theme.textPrimary());
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primary ? background : theme.border(), 1),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)));
            return;
        }

        button.setBackground(theme.secondarySurface());
        button.setForeground(theme.textSecondary());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.disabledBorder(), 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
    }

    private GridBagConstraints buildLeftConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 0);
        return constraints;
    }

    private GridBagConstraints buildRightConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 2.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 8, 0, 0);
        return constraints;
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

    private Integer parsePositiveTimeoutMillis(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            double seconds = Double.parseDouble(value.trim().replace(',', '.'));
            if (seconds <= 0d) {
                return null;
            }
            long millis = Math.round(seconds * 1000d);
            return millis > 0L && millis <= Integer.MAX_VALUE ? (int) millis : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String formatTimeoutSeconds(String rawValue) {
        Integer millis = parsePositiveTimeoutMillis(rawValue);
        return millis == null ? "" : formatTimeoutSeconds(millis);
    }

    private String formatTimeoutSeconds(Integer timeoutMs) {
        if (timeoutMs == null || timeoutMs <= 0) {
            return "";
        }
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.getDefault());
        DecimalFormat format = new DecimalFormat("0.###", symbols);
        return format.format(timeoutMs / 1000d);
    }

    private int gcd(int left, int right) {
        int a = Math.abs(left);
        int b = Math.abs(right);
        while (b != 0) {
            int tmp = a % b;
            a = b;
            b = tmp;
        }
        return a == 0 ? 1 : a;
    }
}
