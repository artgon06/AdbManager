package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.view.Messages;

public class HomePanel extends JPanel {

    private static final String FIELD_STATE = "state";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_MANUFACTURER = "manufacturer";
    private static final String FIELD_BRAND = "brand";
    private static final String FIELD_MODEL = "model";
    private static final String FIELD_PRODUCT = "product";
    private static final String FIELD_CODENAME = "codename";
    private static final String FIELD_ARCHITECTURE = "architecture";
    private static final String FIELD_BATTERY = "battery";
    private static final String FIELD_SERIAL = "serial";
    private static final int SUMMARY_COLUMNS = 3;

    private final JButton powerButton = new JButton();
    private final JButton captureButton = new JButton();
    private final JButton saveScreenshotButton = new JButton();
    private final ScreenshotPreviewPanel screenshotPreviewPanel = new ScreenshotPreviewPanel();

    private final JPanel summaryPanel = new JPanel(new BorderLayout());
    private final JPanel capturePanel = new JPanel(new BorderLayout(0, 18));
    private final JPanel summaryContent = new JPanel(new GridBagLayout());
    private final JPanel heroPanel = new JPanel(new BorderLayout(16, 0));
    private final JPanel heroTextPanel = new JPanel();
    private final JLabel heroTitleLabel = new JLabel("-");
    private final JLabel heroSubtitleLabel = new JLabel("-");
    private final JPanel chipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JLabel stateChipLabel = createChipLabel();
    private final JLabel platformChipLabel = createChipLabel();

    private final JPanel factsPanel = new JPanel(new GridBagLayout());
    private final Map<String, FactCard> factCards = new LinkedHashMap<>();

    private final JPanel metricsPanel = new JPanel(new GridBagLayout());
    private final JPanel batteryPanel = new JPanel();
    private final JLabel batteryTitleLabel = new JLabel();
    private final JLabel batteryValueLabel = new JLabel("-");
    private final JLabel batteryFooterLabel = new JLabel();
    private final JProgressBar batteryProgressBar = new JProgressBar(0, 100);
    private final JPanel ramPanel = new JPanel();
    private final JLabel ramTitleLabel = new JLabel();
    private final JLabel ramValueLabel = new JLabel("-");
    private final JLabel ramFooterLabel = new JLabel();
    private final JProgressBar ramProgressBar = new JProgressBar(0, 100);

    private final JPanel storagePanel = new JPanel();
    private final JLabel storageTitleLabel = new JLabel();
    private final JLabel storageValueLabel = new JLabel("-");
    private final JLabel storageFooterLabel = new JLabel();
    private final JProgressBar storageProgressBar = new JProgressBar(0, 100);

    private BufferedImage currentScreenshot;
    private DeviceDetails currentDetails;
    private AppTheme theme = AppTheme.LIGHT;

    public HomePanel() {
        setLayout(new GridBagLayout());
        buildSummaryPanel();
        buildCapturePanel();
        add(summaryPanel, buildSummaryConstraints());
        add(capturePanel, buildCaptureConstraints());
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
        clearDeviceDetails();
        clearScreenshot();
    }

    public void setDeviceDetails(DeviceDetails details) {
        currentDetails = details;

        heroTitleLabel.setText(primaryDeviceTitle(details));
        heroSubtitleLabel.setText(asLeftHtml(secondaryDeviceTitle(details), 420));

        stateChipLabel.setText(Messages.stateLabel(details.state()));
        platformChipLabel.setText(details.apiLevel().equals("-")
                ? "Android " + details.androidVersion()
                : "Android " + details.androidVersion() + " / API " + details.apiLevel());

        setFactValue(FIELD_STATE, Messages.stateLabel(details.state()));
        setFactValue(FIELD_TYPE, Messages.deviceTypeLabel(details.deviceType()));
        setFactValue(FIELD_MODEL, details.model());
        setFactValue(FIELD_MANUFACTURER, details.manufacturer());
        setFactValue(FIELD_BRAND, details.brand());
        setFactValue(FIELD_PRODUCT, details.productName());
        setFactValue(FIELD_CODENAME, details.codename());
        setFactValue(FIELD_ARCHITECTURE, details.architecture());
        setFactValue(FIELD_BATTERY, details.batteryLabel());
        setFactValue(FIELD_SERIAL, details.serial());
        setBatteryMetric(details.batteryLabel());

        if (details.hasRamInfo()) {
            ramValueLabel.setText(details.usedRamLabel());
            ramFooterLabel.setText(Messages.format("home.metric.total", details.totalRamLabel()));
            ramProgressBar.setValue(details.ramUsagePercent());
            ramProgressBar.setString(details.usedRamLabel() + " / " + details.totalRamLabel());
        } else {
            resetMetric(ramValueLabel, ramFooterLabel, ramProgressBar);
        }

        if (details.hasStorageInfo()) {
            storageValueLabel.setText(details.usedStorageLabel());
            storageFooterLabel.setText(Messages.format("home.metric.total", details.totalStorageLabel()));
            storageProgressBar.setValue(details.storageUsagePercent());
            storageProgressBar.setString(details.usedStorageLabel() + " / " + details.totalStorageLabel());
        } else {
            resetMetric(storageValueLabel, storageFooterLabel, storageProgressBar);
        }
    }

    public void clearDeviceDetails() {
        currentDetails = null;
        heroTitleLabel.setText(Messages.appName());
        heroSubtitleLabel.setText(asLeftHtml(Messages.text("home.summary.empty"), 420));

        stateChipLabel.setText(Messages.text("common.noData"));
        platformChipLabel.setText(Messages.text("common.noData"));

        for (FactCard factCard : factCards.values()) {
            factCard.setValue("-");
        }

        resetMetric(ramValueLabel, ramFooterLabel, ramProgressBar);
        resetMetric(storageValueLabel, storageFooterLabel, storageProgressBar);
        resetMetric(batteryValueLabel, batteryFooterLabel, batteryProgressBar);
    }

    public void setCaptureAction(ActionListener actionListener) {
        captureButton.addActionListener(actionListener);
    }

    public void setPowerButtonAction(ActionListener actionListener) {
        powerButton.addActionListener(actionListener);
    }

    public void setSaveCaptureAction(ActionListener actionListener) {
        saveScreenshotButton.addActionListener(actionListener);
    }

    public void setCaptureEnabled(boolean enabled) {
        captureButton.setEnabled(enabled);
        updateActionButtonsStyle();
    }

    public void setPowerEnabled(boolean enabled) {
        powerButton.setEnabled(enabled);
        updateActionButtonsStyle();
    }

    public void setSaveCaptureEnabled(boolean enabled) {
        saveScreenshotButton.setEnabled(enabled);
        updateActionButtonsStyle();
    }

    public void setScreenshot(BufferedImage image) {
        if (currentScreenshot != null && currentScreenshot != image) {
            currentScreenshot.flush();
        }
        currentScreenshot = image;
        screenshotPreviewPanel.setScreenshot(image);
        setSaveCaptureEnabled(image != null);
    }

    public void clearScreenshot() {
        if (currentScreenshot != null) {
            currentScreenshot.flush();
        }
        currentScreenshot = null;
        screenshotPreviewPanel.clearScreenshot();
        setSaveCaptureEnabled(false);
    }

    public BufferedImage getCurrentScreenshot() {
        return currentScreenshot;
    }

    public void refreshTexts() {
        powerButton.setToolTipText(Messages.text("navigation.power.tooltip"));
        captureButton.setText("");
        captureButton.setToolTipText(Messages.text("home.capture"));
        saveScreenshotButton.setText("");
        saveScreenshotButton.setToolTipText(Messages.text("home.saveCapture"));

        factCards.get(FIELD_STATE).setTitle(Messages.text("home.field.state"));
        factCards.get(FIELD_TYPE).setTitle(Messages.text("home.field.deviceType"));
        factCards.get(FIELD_MODEL).setTitle(Messages.text("home.field.model"));
        factCards.get(FIELD_MANUFACTURER).setTitle(Messages.text("home.field.manufacturer"));
        factCards.get(FIELD_BRAND).setTitle(Messages.text("home.field.brand"));
        factCards.get(FIELD_PRODUCT).setTitle(Messages.text("home.field.product"));
        factCards.get(FIELD_CODENAME).setTitle(Messages.text("home.field.codename"));
        factCards.get(FIELD_ARCHITECTURE).setTitle(Messages.text("home.field.architecture"));
        factCards.get(FIELD_BATTERY).setTitle(Messages.text("home.field.battery"));
        factCards.get(FIELD_SERIAL).setTitle(Messages.text("home.field.serial"));

        ramTitleLabel.setText(Messages.text("home.ram.inUse"));
        storageTitleLabel.setText(Messages.text("home.storage.inUse"));
        batteryTitleLabel.setText(Messages.text("home.field.battery"));

        summaryPanel.setBorder(BorderFactory.createEmptyBorder());
        screenshotPreviewPanel.refreshTexts();

        if (currentDetails == null) {
            clearDeviceDetails();
        } else {
            setDeviceDetails(currentDetails);
        }
    }

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        setBackground(theme.background());

        summaryPanel.setBackground(theme.background());
        summaryContent.setBackground(theme.background());
        capturePanel.setBackground(theme.background());
        chipPanel.setBackground(theme.background());
        factsPanel.setBackground(theme.background());
        metricsPanel.setBackground(theme.background());

        styleSurfaceCard(heroPanel, true);

        heroTextPanel.setOpaque(false);
        heroTitleLabel.setForeground(theme.textPrimary());
        heroTitleLabel.setFont(new Font("Inter", Font.BOLD, 28));
        heroSubtitleLabel.setForeground(theme.textSecondary());
        heroSubtitleLabel.setFont(new Font("Inter", Font.PLAIN, 14));

        styleChip(stateChipLabel);
        styleChip(platformChipLabel);

        for (FactCard factCard : factCards.values()) {
            factCard.applyTheme(theme);
        }

        styleMetricCard(batteryPanel, batteryTitleLabel, batteryValueLabel, batteryFooterLabel, batteryProgressBar,
                new Color(40, 205, 98));
        styleMetricCard(ramPanel, ramTitleLabel, ramValueLabel, ramFooterLabel, ramProgressBar, new Color(40, 205, 98));
        styleMetricCard(storagePanel, storageTitleLabel, storageValueLabel, storageFooterLabel, storageProgressBar,
                new Color(255, 69, 78));

        summaryPanel.setBorder(BorderFactory.createEmptyBorder());
        screenshotPreviewPanel.applyTheme(theme);
        updateActionButtonsStyle();
        repaint();
    }

    private void buildSummaryPanel() {
        summaryPanel.setPreferredSize(new Dimension(620, 0));
        summaryContent.setBorder(new EmptyBorder(16, 16, 16, 16));
        addSummarySection(buildHeroPanel(), 0, 0.0, new Insets(0, 0, 10, 0), GridBagConstraints.HORIZONTAL);
        addSummarySection(buildFactsPanel(), 1, 0.58, new Insets(0, 0, 10, 0), GridBagConstraints.BOTH);
        addSummarySection(buildMetricsPanel(), 2, 0.42, new Insets(0, 0, 0, 0), GridBagConstraints.BOTH);

        summaryPanel.add(summaryContent, BorderLayout.CENTER);
    }

    private void addSummarySection(JPanel panel, int row, double weightY, Insets insets, int fill) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 1.0;
        constraints.weighty = weightY;
        constraints.fill = fill;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = insets;
        summaryContent.add(panel, constraints);
    }

    private JPanel buildHeroPanel() {
        heroPanel.setPreferredSize(new Dimension(0, 132));

        heroTextPanel.setLayout(new BoxLayout(heroTextPanel, BoxLayout.Y_AXIS));
        heroTitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        heroTitleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        heroSubtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        heroSubtitleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        heroTextPanel.add(heroTitleLabel);
        heroTextPanel.add(Box.createVerticalStrut(6));
        heroTextPanel.add(heroSubtitleLabel);
        heroTextPanel.add(Box.createVerticalStrut(12));

        ((FlowLayout) chipPanel.getLayout()).setAlignment(FlowLayout.LEFT);
        chipPanel.add(stateChipLabel);
        chipPanel.add(platformChipLabel);
        chipPanel.setAlignmentX(LEFT_ALIGNMENT);
        heroTextPanel.add(chipPanel);

        heroPanel.add(heroTextPanel, BorderLayout.CENTER);
        return heroPanel;
    }

    private JPanel buildFactsPanel() {
        factsPanel.removeAll();
        addFactCard(FIELD_STATE, false, 0, 0, 1);
        addFactCard(FIELD_TYPE, false, 1, 0, 1);
        addFactCard(FIELD_BATTERY, false, 2, 0, 1);
        addFactCard(FIELD_MODEL, false, 0, 1, 1);
        addFactCard(FIELD_MANUFACTURER, false, 1, 1, 1);
        addFactCard(FIELD_BRAND, false, 2, 1, 1);
        addFactCard(FIELD_ARCHITECTURE, false, 0, 2, 1);
        addFactCard(FIELD_PRODUCT, false, 1, 2, 1);
        addFactCard(FIELD_CODENAME, false, 2, 2, 1);
        addFactCard(FIELD_SERIAL, true, 0, 3, 3);
        return factsPanel;
    }

    private JPanel buildMetricsPanel() {
        metricsPanel.removeAll();
        addMetricCard(createMetricPanel(
                batteryPanel,
                batteryTitleLabel,
                batteryValueLabel,
                batteryProgressBar,
                batteryFooterLabel), 0, 0, 1, new Insets(0, 0, 10, 10));
        addMetricCard(createMetricPanel(
                ramPanel,
                ramTitleLabel,
                ramValueLabel,
                ramProgressBar,
                ramFooterLabel), 1, 0, 1, new Insets(0, 0, 10, 0));
        addMetricCard(createMetricPanel(
                storagePanel,
                storageTitleLabel,
                storageValueLabel,
                storageProgressBar,
                storageFooterLabel), 0, 1, 2, new Insets(0, 0, 0, 0));
        return metricsPanel;
    }

    private void addMetricCard(JPanel panel, int gridX, int gridY, int gridWidth, Insets insets) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = gridX;
        constraints.gridy = gridY;
        constraints.gridwidth = gridWidth;
        constraints.weightx = gridWidth;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = insets;
        metricsPanel.add(panel, constraints);
    }

    private JPanel createMetricPanel(
            JPanel container,
            JLabel titleLabel,
            JLabel valueLabel,
            JProgressBar progressBar,
            JLabel footerLabel) {
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setAlignmentX(LEFT_ALIGNMENT);
        container.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.setOpaque(false);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(valueLabel, BorderLayout.EAST);

        progressBar.setStringPainted(true);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 18));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

        container.add(headerPanel);
        container.add(Box.createVerticalStrut(10));
        container.add(progressBar);
        container.add(Box.createVerticalStrut(8));
        container.add(footerLabel);
        return container;
    }

    private void addFactCard(String key, boolean wide, int gridX, int gridY, int gridWidth) {
        FactCard factCard = new FactCard(wide);
        factCards.put(key, factCard);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = gridX;
        constraints.gridy = gridY;
        constraints.gridwidth = gridWidth;
        constraints.weightx = gridWidth;
        constraints.weighty = wide ? 0.9 : 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        int rightInset = gridX + gridWidth < SUMMARY_COLUMNS ? 10 : 0;
        constraints.insets = new Insets(0, 0, 10, rightInset);
        factsPanel.add(factCard.panel(), constraints);
    }

    private void buildCapturePanel() {
        JPanel topActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topActionsPanel.setOpaque(false);

        powerButton.setFocusPainted(false);
        powerButton.setRolloverEnabled(true);
        powerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        powerButton.getModel().addChangeListener(event -> stylePowerButton());
        powerButton.setPreferredSize(new Dimension(32, 32));
        topActionsPanel.add(powerButton);
        topActionsPanel.add(Box.createHorizontalStrut(10));

        captureButton.setFocusPainted(false);
        captureButton.setRolloverEnabled(true);
        captureButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        captureButton.getModel().addChangeListener(event -> styleActionButton(captureButton, true));
        captureButton.setPreferredSize(new Dimension(32, 32));
        topActionsPanel.add(captureButton);
        topActionsPanel.add(Box.createHorizontalStrut(10));

        saveScreenshotButton.setFocusPainted(false);
        saveScreenshotButton.setRolloverEnabled(true);
        saveScreenshotButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveScreenshotButton.getModel().addChangeListener(event -> styleActionButton(saveScreenshotButton, false));
        saveScreenshotButton.setPreferredSize(new Dimension(32, 32));
        topActionsPanel.add(saveScreenshotButton);

        capturePanel.add(topActionsPanel, BorderLayout.NORTH);
        capturePanel.add(screenshotPreviewPanel, BorderLayout.CENTER);
    }

    private GridBagConstraints buildSummaryConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.46;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(24, 24, 24, 18);
        return constraints;
    }

    private GridBagConstraints buildCaptureConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 0.54;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(24, 18, 24, 24);
        return constraints;
    }

    private void updateActionButtonsStyle() {
        stylePowerButton();
        styleActionButton(captureButton, true);
        styleActionButton(saveScreenshotButton, false);
    }

    private void stylePowerButton() {
        boolean enabled = powerButton.isEnabled();
        powerButton.setText("");
        powerButton.setIcon(new ToolbarIcon(
                ToolbarIcon.Type.POWER,
                18,
                enabled ? theme.actionBackground() : theme.textSecondary()));
        ButtonStyler.applyStandard(powerButton, theme, false, true, false);
    }

    private void styleActionButton(JButton button, boolean primary) {
        boolean enabled = button.isEnabled();
        button.setFont(new Font("Inter", Font.BOLD, 15));
        button.setIcon(new ToolbarIcon(
                button == captureButton ? ToolbarIcon.Type.CAMERA : ToolbarIcon.Type.EXPORT,
                18,
                enabled
                        ? (primary ? theme.actionForeground() : theme.textPrimary())
                        : theme.textSecondary()));
        // determine flags: these buttons in HomePanel are icon-only
        ButtonStyler.applyStandard(button, theme, primary, true, false);
    }

    private void styleChip(JLabel label) {
        label.setOpaque(true);
        label.setBackground(ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.88d));
        label.setForeground(theme.textPrimary());
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        label.setFont(new Font("Inter", Font.BOLD, 12));
    }

    private void styleSurfaceCard(JPanel panel, boolean elevated) {
        panel.setOpaque(true);
        panel.setBackground(elevated
                ? theme.background()
                : ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.52d));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(elevated ? 14 : 12, elevated ? 14 : 12, elevated ? 14 : 12, elevated ? 14 : 12)));
    }

    private void styleMetricCard(
            JPanel panel,
            JLabel titleLabel,
            JLabel valueLabel,
            JLabel footerLabel,
            JProgressBar progressBar,
            Color accentColor) {
        styleSurfaceCard(panel, false);

        titleLabel.setForeground(theme.textSecondary());
        titleLabel.setFont(new Font("Inter", Font.BOLD, 12));

        valueLabel.setForeground(theme.textPrimary());
        valueLabel.setFont(new Font("Inter", Font.BOLD, 18));

        footerLabel.setForeground(theme.textSecondary());
        footerLabel.setFont(new Font("Inter", Font.PLAIN, 12));

        progressBar.setBackground(ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.72d));
        progressBar.setForeground(accentColor);
        progressBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        progressBar.setFont(new Font("Inter", Font.BOLD, 11));
    }

    private void resetMetric(JLabel valueLabel, JLabel footerLabel, JProgressBar progressBar) {
        valueLabel.setText("-");
        footerLabel.setText(Messages.format("home.metric.total", "-"));
        progressBar.setValue(0);
        progressBar.setString(Messages.text("common.noData"));
    }

    private void setBatteryMetric(String batteryLabel) {
        String normalized = batteryLabel == null || batteryLabel.isBlank() ? "-" : batteryLabel.trim();
        batteryValueLabel.setText(normalized);
        batteryFooterLabel.setText(" ");
        Integer percent = parseBatteryPercent(normalized);
        if (percent == null) {
            batteryProgressBar.setValue(0);
            batteryProgressBar.setString(Messages.text("common.noData"));
            return;
        }
        batteryProgressBar.setValue(Math.max(0, Math.min(100, percent)));
        batteryProgressBar.setString(percent + "%");
    }

    private Integer parseBatteryPercent(String label) {
        if (label == null) {
            return null;
        }
        String digits = label.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void setFactValue(String key, String value) {
        FactCard factCard = factCards.get(key);
        if (factCard == null) {
            return;
        }
        factCard.setValue(value);
    }

    private JLabel createChipLabel() {
        return new JLabel("-");
    }

    private String primaryDeviceTitle(DeviceDetails details) {
        String marketingName = details.marketingName();
        if (marketingName != null && !marketingName.isBlank() && !"-".equals(marketingName)) {
            return marketingName;
        }
        return details.model() == null || details.model().isBlank() || "-".equals(details.model())
                ? details.serial()
                : details.model();
    }

    private String secondaryDeviceTitle(DeviceDetails details) {
        String manufacturer = normalizeDisplayValue(details.manufacturer());
        String soc = normalizeDisplayValue(details.soc());
        String model = normalizeDisplayValue(details.model());

        StringBuilder builder = new StringBuilder();
        if (!manufacturer.isBlank()) {
            builder.append(manufacturer);
        }
        if (!soc.isBlank()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(soc);
        }
        if (!model.isBlank()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(model);
        }
        return builder.isEmpty() ? Messages.text("home.summary.empty") : builder.toString();
    }

    private String normalizeDisplayValue(String value) {
        return value == null || value.isBlank() || "-".equals(value) ? "" : value.trim();
    }

    private TitledBorder createSectionBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Inter", Font.BOLD, 18),
                        theme.textPrimary());
    }

    private String asHtml(String text, int width) {
        return "<html><body style='width:" + width + "px'>" + text + "</body></html>";
    }

    private String asLeftHtml(String text, int width) {
        return "<html><div style='width:" + width + "px; text-align:left'>" + text + "</div></html>";
    }

    private final class FactCard {

        private final JPanel panel = new JPanel();
        private final JLabel titleLabel = new JLabel();
        private final JLabel valueLabel = new JLabel("-");
        private final boolean wide;

        private FactCard(boolean wide) {
            this.wide = wide;
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setAlignmentX(LEFT_ALIGNMENT);
            panel.add(titleLabel);
            panel.add(Box.createVerticalStrut(6));
            panel.add(valueLabel);
        }

        private JPanel panel() {
            return panel;
        }

        private void setTitle(String title) {
            titleLabel.setText(title);
        }

        private void setValue(String value) {
            String normalized = value == null || value.isBlank() ? "-" : value.trim();
            if (wide && normalized.length() > 28) {
                valueLabel.setText(asHtml(normalized, 300));
            } else {
                valueLabel.setText(normalized);
            }
            valueLabel.setToolTipText(normalized);
        }

        private void applyTheme(AppTheme theme) {
            styleSurfaceCard(panel, false);
            titleLabel.setForeground(theme.textSecondary());
            titleLabel.setFont(new Font("Inter", Font.BOLD, 11));
            valueLabel.setForeground(theme.textPrimary());
            valueLabel.setFont(new Font("Inter", wide ? Font.PLAIN : Font.BOLD, wide ? 13 : 15));
        }
    }
}
