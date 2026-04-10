package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;

import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.logic.model.DisplayInfo;
import com.adbmanager.logic.model.DisplayOverrideSuggestion;
import com.adbmanager.view.Messages;

public class DisplayPanel extends JPanel {

    private static final String FIELD_DEVICE_TYPE = "deviceType";
    private static final String FIELD_CURRENT_RESOLUTION = "currentResolution";
    private static final String FIELD_PHYSICAL_RESOLUTION = "physicalResolution";
    private static final String FIELD_DENSITY = "density";
    private static final String FIELD_PHYSICAL_DENSITY = "physicalDensity";
    private static final String FIELD_SMALLEST_WIDTH = "smallestWidth";
    private static final String FIELD_REFRESH_RATE = "refreshRate";
    private static final String FIELD_SUPPORTED_REFRESH_RATES = "supportedRefreshRates";

    private final JLabel titleLabel = new JLabel();
    private final JPanel metricsPanel = new JPanel(new BorderLayout());
    private final JPanel metricsContent = new JPanel();
    private final JPanel controlsPanel = new JPanel(new BorderLayout());
    private final JPanel controlsContent = new JPanel();
    private final JLabel inputTitleLabel = new JLabel();
    private final JLabel originalAspectLabel = new JLabel();
    private final JLabel customAspectLabel = new JLabel();
    private final JLabel widthLabel = new JLabel();
    private final JLabel heightLabel = new JLabel();
    private final JLabel densityLabel = new JLabel();
    private final JLabel darkModeTitleLabel = new JLabel();
    private final JCheckBox darkModeToggle = new JCheckBox();
    private final JTextField widthField = new JTextField();
    private final JTextField heightField = new JTextField();
    private final JTextField densityField = new JTextField();
    private final JButton applyButton = new JButton();
    private final JButton resetButton = new JButton();
    private final JPanel suggestionButtonsPanel = new JPanel(new GridLayout(0, 1, 0, 10));
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

    public Integer getRequestedWidth() {
        return parsePositiveInteger(widthField.getText());
    }

    public Integer getRequestedHeight() {
        return parsePositiveInteger(heightField.getText());
    }

    public Integer getRequestedDensity() {
        return parsePositiveInteger(densityField.getText());
    }

    public boolean isDeviceDarkModeSelected() {
        return darkModeToggle.isSelected();
    }

    public void setDisplayControlsEnabled(boolean enabled) {
        widthField.setEnabled(enabled);
        heightField.setEnabled(enabled);
        densityField.setEnabled(enabled);
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
        valueLabels.get(FIELD_REFRESH_RATE).setText(displayInfo.refreshRateLabel());
        valueLabels.get(FIELD_SUPPORTED_REFRESH_RATES).setText(displayInfo.supportedRefreshRatesLabel());

        setRequestedDisplayValues(
                displayInfo.widthPx(),
                displayInfo.heightPx(),
                displayInfo.densityDpi());
        syncingDarkModeToggle = true;
        try {
            darkModeToggle.setSelected(Boolean.TRUE.equals(displayInfo.darkModeEnabled()));
        } finally {
            syncingDarkModeToggle = false;
        }
        styleDarkModeToggle();
        originalAspectLabel.setText(Messages.format("display.aspect.original", displayInfo.physicalAspectRatioLabel()));
        rebuildSuggestionButtons(displayInfo);
        updateAspectRatioPreview();
    }

    public void clearDeviceDetails() {
        currentDetails = null;
        for (JLabel valueLabel : valueLabels.values()) {
            valueLabel.setText("-");
        }
        setRequestedDisplayValues(null, null, null);
        syncingDarkModeToggle = true;
        try {
            darkModeToggle.setSelected(false);
        } finally {
            syncingDarkModeToggle = false;
        }
        darkModeToggle.setEnabled(false);
        styleDarkModeToggle();
        originalAspectLabel.setText(Messages.format("display.aspect.original", "-"));
        customAspectLabel.setText(Messages.format("display.aspect.custom", "-"));
        rebuildSuggestionButtons(DisplayInfo.empty());
    }

    public void refreshTexts() {
        titleLabel.setText(Messages.text("display.title"));

        fieldLabels.get(FIELD_DEVICE_TYPE).setText(Messages.text("display.field.deviceType"));
        fieldLabels.get(FIELD_CURRENT_RESOLUTION).setText(Messages.text("display.field.currentResolution"));
        fieldLabels.get(FIELD_PHYSICAL_RESOLUTION).setText(Messages.text("display.field.physicalResolution"));
        fieldLabels.get(FIELD_DENSITY).setText(Messages.text("display.field.density"));
        fieldLabels.get(FIELD_PHYSICAL_DENSITY).setText(Messages.text("display.field.physicalDensity"));
        fieldLabels.get(FIELD_SMALLEST_WIDTH).setText(Messages.text("display.field.smallestWidth"));
        fieldLabels.get(FIELD_REFRESH_RATE).setText(Messages.text("display.field.refreshRate"));
        fieldLabels.get(FIELD_SUPPORTED_REFRESH_RATES).setText(Messages.text("display.field.supportedRefreshRates"));

        inputTitleLabel.setText(Messages.text("display.override.manual"));
        widthLabel.setText(Messages.text("display.override.width"));
        heightLabel.setText(Messages.text("display.override.height"));
        densityLabel.setText(Messages.text("display.override.density"));
        darkModeTitleLabel.setText(Messages.text("display.deviceDarkMode.title"));
        darkModeToggle.setText("");
        applyButton.setText(Messages.text("display.override.apply"));
        resetButton.setText(Messages.text("display.override.reset"));

        metricsPanel.setBorder(createSectionBorder(Messages.text("display.metrics.title"), theme));
        controlsPanel.setBorder(createSectionBorder(Messages.text("display.override.title"), theme));

        if (currentDetails == null) {
            clearDeviceDetails();
        } else {
            setDeviceDetails(currentDetails);
        }
    }

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        setBackground(theme.background());
        titleLabel.setForeground(theme.textPrimary());

        metricsPanel.setBackground(theme.surface());
        metricsContent.setBackground(theme.surface());
        controlsPanel.setBackground(theme.surface());
        controlsContent.setBackground(theme.surface());
        suggestionButtonsPanel.setBackground(theme.surface());

        for (JPanel rowPanel : rowPanels) {
            rowPanel.setBackground(theme.surface());
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
        darkModeTitleLabel.setForeground(theme.textPrimary());
        darkModeTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        styleDarkModeToggle();
        styleInputField(widthField);
        styleInputField(heightField);
        styleInputField(densityField);
        styleActionButtons();

        for (JButton suggestionButton : suggestionButtons) {
            styleSuggestionButton(suggestionButton);
        }

        metricsPanel.setBorder(createSectionBorder(Messages.text("display.metrics.title"), theme));
        controlsPanel.setBorder(createSectionBorder(Messages.text("display.override.title"), theme));

        repaint();
    }

    private void buildPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(28, 28, 28, 28));

        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        add(titleLabel, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(22, 0, 0, 0));

        buildMetricsPanel();
        buildControlsPanel();

        content.add(metricsPanel, buildMetricsConstraints());
        content.add(controlsPanel, buildControlsConstraints());

        add(content, BorderLayout.CENTER);
    }

    private void buildMetricsPanel() {
        metricsContent.setLayout(new BoxLayout(metricsContent, BoxLayout.Y_AXIS));
        metricsContent.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        metricsContent.add(createInfoRow(FIELD_DEVICE_TYPE));
        metricsContent.add(createInfoRow(FIELD_CURRENT_RESOLUTION));
        metricsContent.add(createInfoRow(FIELD_PHYSICAL_RESOLUTION));
        metricsContent.add(createInfoRow(FIELD_DENSITY));
        metricsContent.add(createInfoRow(FIELD_PHYSICAL_DENSITY));
        metricsContent.add(createInfoRow(FIELD_SMALLEST_WIDTH));
        metricsContent.add(createInfoRow(FIELD_REFRESH_RATE));
        metricsContent.add(createInfoRow(FIELD_SUPPORTED_REFRESH_RATES));
        metricsContent.add(Box.createVerticalGlue());

        metricsPanel.add(metricsContent, BorderLayout.CENTER);
    }

    private void buildControlsPanel() {
        controlsContent.setLayout(new BoxLayout(controlsContent, BoxLayout.Y_AXIS));
        controlsContent.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel ratioPanel = new JPanel();
        ratioPanel.setOpaque(false);
        ratioPanel.setLayout(new BoxLayout(ratioPanel, BoxLayout.Y_AXIS));
        ratioPanel.add(originalAspectLabel);
        ratioPanel.add(Box.createVerticalStrut(4));
        ratioPanel.add(customAspectLabel);

        JPanel darkModePanel = new JPanel(new BorderLayout(12, 0));
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

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        addFormField(formPanel, widthLabel, widthField, 0);
        addFormField(formPanel, heightLabel, heightField, 1);
        addFormField(formPanel, densityLabel, densityField, 2);

        JPanel actionsPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        actionsPanel.setOpaque(false);
        configureActionButton(applyButton);
        configureActionButton(resetButton);
        actionsPanel.add(applyButton);
        actionsPanel.add(resetButton);

        suggestionButtonsPanel.setOpaque(false);

        controlsContent.add(inputTitleLabel);
        controlsContent.add(Box.createVerticalStrut(12));
        controlsContent.add(formPanel);
        controlsContent.add(Box.createVerticalStrut(10));
        controlsContent.add(ratioPanel);
        controlsContent.add(Box.createVerticalStrut(18));
        controlsContent.add(darkModePanel);
        controlsContent.add(Box.createVerticalStrut(18));
        controlsContent.add(actionsPanel);
        controlsContent.add(Box.createVerticalStrut(18));
        controlsContent.add(suggestionButtonsPanel);
        controlsContent.add(Box.createVerticalGlue());

        controlsPanel.add(controlsContent, BorderLayout.CENTER);
    }

    private void addFormField(JPanel formPanel, JLabel label, JTextField field, int rowIndex) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = rowIndex;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(0, 0, 12, 12);
        formPanel.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = rowIndex;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(0, 0, 12, 0);
        formPanel.add(field, fieldConstraints);
    }

    private JPanel createInfoRow(String fieldKey) {
        JPanel rowPanel = new JPanel(new BorderLayout(14, 0));
        rowPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        JLabel keyLabel = new JLabel();
        JLabel valueLabel = new JLabel("-");
        valueLabel.setHorizontalAlignment(JLabel.RIGHT);

        rowPanels.add(rowPanel);
        fieldLabels.put(fieldKey, keyLabel);
        valueLabels.put(fieldKey, valueLabel);
        dynamicValueLabels.add(valueLabel);

        rowPanel.add(keyLabel, BorderLayout.WEST);
        rowPanel.add(valueLabel, BorderLayout.CENTER);
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
        suggestionButtonsPanel.add(suggestionsLabel);
        suggestionButtonsPanel.add(Box.createVerticalStrut(10));

        for (DisplayOverrideSuggestion suggestion : buildSuggestions(displayInfo)) {
            JButton button = new JButton(suggestion.commandLabel());
            configureActionButton(button);
            button.addActionListener(event -> {
                setRequestedDisplayValues(suggestion.widthPx(), suggestion.heightPx(), suggestion.densityDpi());
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
            if (width >= physicalWidth || height >= physicalHeight) {
                continue;
            }

            if (!seen.add(width + "x" + height)) {
                continue;
            }

            int targetSmallestSide = Math.min(width, height);
            int density = Math.max(1, (int) Math.round(baseDensity * (targetSmallestSide / (double) smallestSide)));
            suggestions.add(new DisplayOverrideSuggestion(width, height, density));
        }

        return suggestions;
    }

    private void setRequestedDisplayValues(Integer widthPx, Integer heightPx, Integer densityDpi) {
        widthField.setText(widthPx == null || widthPx <= 0 ? "" : String.valueOf(widthPx));
        heightField.setText(heightPx == null || heightPx <= 0 ? "" : String.valueOf(heightPx));
        densityField.setText(densityDpi == null || densityDpi <= 0 ? "" : String.valueOf(densityDpi));
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
        button.setPreferredSize(new Dimension(0, 42));
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
        darkModeToggle.setToolTipText(Messages.text(darkModeToggle.isSelected()
                ? "display.deviceDarkMode.enabled"
                : "display.deviceDarkMode.disabled"));
    }

    private void styleInputField(JTextField field) {
        field.setForeground(theme.textPrimary());
        field.setCaretColor(theme.textPrimary());
        field.setBackground(theme.secondarySurface());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
    }

    private void styleActionButtons() {
        styleActionButton(applyButton, true);
        styleActionButton(resetButton, false);
    }

    private void styleSuggestionButton(JButton button) {
        styleActionButton(button, false);
    }

    private void styleActionButton(JButton button, boolean primary) {
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        if (button.isEnabled()) {
            button.setBackground(primary ? theme.actionBackground() : theme.surface());
            button.setForeground(primary ? theme.actionForeground() : theme.textPrimary());
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primary ? theme.actionBackground() : theme.border(), 1),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)));
            return;
        }

        button.setBackground(theme.secondarySurface());
        button.setForeground(theme.textSecondary());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.disabledBorder(), 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
    }

    private GridBagConstraints buildMetricsConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.48;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 12);
        return constraints;
    }

    private GridBagConstraints buildControlsConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 0.52;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 12, 0, 0);
        return constraints;
    }

    private TitledBorder createSectionBorder(String title, AppTheme theme) {
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
