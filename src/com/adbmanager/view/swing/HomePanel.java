package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;

import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.view.Messages;

public class HomePanel extends JPanel {

    private static final String FIELD_STATE = "state";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_MANUFACTURER = "manufacturer";
    private static final String FIELD_BRAND = "brand";
    private static final String FIELD_PRODUCT = "product";
    private static final String FIELD_CODENAME = "codename";
    private static final String FIELD_ARCHITECTURE = "architecture";
    private static final String FIELD_BATTERY = "battery";
    private static final String FIELD_SERIAL = "serial";
    private static final String FIELD_SOC = "soc";

    private final JButton captureButton = new JButton();
    private final JButton saveScreenshotButton = new JButton();
    private final ScreenshotPreviewPanel screenshotPreviewPanel = new ScreenshotPreviewPanel();

    private final JPanel summaryPanel = new JPanel(new BorderLayout());
    private final JPanel capturePanel = new JPanel(new BorderLayout(0, 18));
    private final JPanel summaryContent = new JPanel();
    private final JPanel heroPanel = new JPanel(new BorderLayout(16, 0));
    private final JLabel heroIconLabel = new JLabel();
    private final JPanel heroTextPanel = new JPanel();
    private final JLabel heroTitleLabel = new JLabel("-");
    private final JLabel heroSubtitleLabel = new JLabel("-");
    private final JPanel chipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JLabel stateChipLabel = createChipLabel();
    private final JLabel platformChipLabel = createChipLabel();
    private final JLabel batteryChipLabel = createChipLabel();

    private final JPanel factsPanel = new JPanel(new GridBagLayout());
    private final Map<String, FactCard> factCards = new LinkedHashMap<>();

    private final JPanel metricsPanel = new JPanel(new GridLayout(1, 2, 12, 0));
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
        heroSubtitleLabel.setText(asHtml(secondaryDeviceTitle(details), 290));
        heroIconLabel.setIcon(createHeroIcon());

        stateChipLabel.setText(Messages.stateLabel(details.state()));
        platformChipLabel.setText(details.apiLevel().equals("-")
                ? "Android " + details.androidVersion()
                : "Android " + details.androidVersion() + " / API " + details.apiLevel());
        batteryChipLabel.setText(Messages.text("home.field.battery") + ": " + details.batteryLabel());

        setFactValue(FIELD_STATE, Messages.stateLabel(details.state()));
        setFactValue(FIELD_TYPE, Messages.deviceTypeLabel(details.deviceType()));
        setFactValue(FIELD_MANUFACTURER, details.manufacturer());
        setFactValue(FIELD_BRAND, details.brand());
        setFactValue(FIELD_PRODUCT, details.productName());
        setFactValue(FIELD_CODENAME, details.codename());
        setFactValue(FIELD_ARCHITECTURE, details.architecture());
        setFactValue(FIELD_BATTERY, details.batteryLabel());
        setFactValue(FIELD_SERIAL, details.serial());
        setFactValue(FIELD_SOC, details.soc());

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
        heroSubtitleLabel.setText(asHtml(Messages.text("home.summary.empty"), 290));
        heroIconLabel.setIcon(createHeroIcon());

        stateChipLabel.setText(Messages.text("common.noData"));
        platformChipLabel.setText(Messages.text("common.noData"));
        batteryChipLabel.setText(Messages.text("home.field.battery") + ": -");

        for (FactCard factCard : factCards.values()) {
            factCard.setValue("-");
        }

        resetMetric(ramValueLabel, ramFooterLabel, ramProgressBar);
        resetMetric(storageValueLabel, storageFooterLabel, storageProgressBar);
    }

    public void setCaptureAction(ActionListener actionListener) {
        captureButton.addActionListener(actionListener);
    }

    public void setSaveCaptureAction(ActionListener actionListener) {
        saveScreenshotButton.addActionListener(actionListener);
    }

    public void setCaptureEnabled(boolean enabled) {
        captureButton.setEnabled(enabled);
        updateActionButtonsStyle();
    }

    public void setSaveCaptureEnabled(boolean enabled) {
        saveScreenshotButton.setEnabled(enabled);
        updateActionButtonsStyle();
    }

    public void setScreenshot(BufferedImage image) {
        currentScreenshot = image;
        screenshotPreviewPanel.setScreenshot(image);
        setSaveCaptureEnabled(image != null);
    }

    public void clearScreenshot() {
        currentScreenshot = null;
        screenshotPreviewPanel.clearScreenshot();
        setSaveCaptureEnabled(false);
    }

    public BufferedImage getCurrentScreenshot() {
        return currentScreenshot;
    }

    public void refreshTexts() {
        captureButton.setText(Messages.text("home.capture"));
        saveScreenshotButton.setText(Messages.text("home.saveCapture"));

        factCards.get(FIELD_STATE).setTitle(Messages.text("home.field.state"));
        factCards.get(FIELD_TYPE).setTitle(Messages.text("home.field.deviceType"));
        factCards.get(FIELD_MANUFACTURER).setTitle(Messages.text("home.field.manufacturer"));
        factCards.get(FIELD_BRAND).setTitle(Messages.text("home.field.brand"));
        factCards.get(FIELD_PRODUCT).setTitle(Messages.text("home.field.product"));
        factCards.get(FIELD_CODENAME).setTitle(Messages.text("home.field.codename"));
        factCards.get(FIELD_ARCHITECTURE).setTitle(Messages.text("home.field.architecture"));
        factCards.get(FIELD_BATTERY).setTitle(Messages.text("home.field.battery"));
        factCards.get(FIELD_SERIAL).setTitle(Messages.text("home.field.serial"));
        factCards.get(FIELD_SOC).setTitle(Messages.text("home.field.soc"));

        ramTitleLabel.setText(Messages.text("home.ram.inUse"));
        storageTitleLabel.setText(Messages.text("home.storage.inUse"));

        summaryPanel.setBorder(createSectionBorder(Messages.text("home.summary.title")));
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

        summaryPanel.setBackground(theme.surface());
        summaryContent.setBackground(theme.surface());
        capturePanel.setBackground(theme.background());
        chipPanel.setBackground(theme.surface());
        factsPanel.setBackground(theme.surface());
        metricsPanel.setBackground(theme.surface());

        styleSurfaceCard(heroPanel, true);
        styleHeroIcon(theme);

        heroTextPanel.setOpaque(false);
        heroTitleLabel.setForeground(theme.textPrimary());
        heroTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        heroSubtitleLabel.setForeground(theme.textSecondary());
        heroSubtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        styleChip(stateChipLabel);
        styleChip(platformChipLabel);
        styleChip(batteryChipLabel);

        for (FactCard factCard : factCards.values()) {
            factCard.applyTheme(theme);
        }

        styleMetricCard(ramPanel, ramTitleLabel, ramValueLabel, ramFooterLabel, ramProgressBar);
        styleMetricCard(storagePanel, storageTitleLabel, storageValueLabel, storageFooterLabel, storageProgressBar);

        summaryPanel.setBorder(createSectionBorder(Messages.text("home.summary.title")));
        screenshotPreviewPanel.applyTheme(theme);
        updateActionButtonsStyle();
        repaint();
    }

    private void buildSummaryPanel() {
        summaryPanel.setPreferredSize(new Dimension(430, 0));
        summaryContent.setLayout(new BoxLayout(summaryContent, BoxLayout.Y_AXIS));
        summaryContent.setBorder(new EmptyBorder(18, 18, 18, 18));

        summaryContent.add(buildHeroPanel());
        summaryContent.add(Box.createVerticalStrut(14));
        summaryContent.add(buildFactsPanel());
        summaryContent.add(Box.createVerticalStrut(14));
        summaryContent.add(buildMetricsPanel());
        summaryContent.add(Box.createVerticalGlue());

        summaryPanel.add(summaryContent, BorderLayout.CENTER);
    }

    private JPanel buildHeroPanel() {
        heroPanel.setAlignmentX(LEFT_ALIGNMENT);

        heroIconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        heroIconLabel.setVerticalAlignment(SwingConstants.CENTER);
        heroIconLabel.setPreferredSize(new Dimension(72, 72));

        heroTextPanel.setLayout(new BoxLayout(heroTextPanel, BoxLayout.Y_AXIS));
        heroTextPanel.add(Box.createVerticalStrut(2));
        heroTextPanel.add(heroTitleLabel);
        heroTextPanel.add(Box.createVerticalStrut(6));
        heroTextPanel.add(heroSubtitleLabel);
        heroTextPanel.add(Box.createVerticalStrut(12));

        chipPanel.add(stateChipLabel);
        chipPanel.add(platformChipLabel);
        chipPanel.add(batteryChipLabel);
        heroTextPanel.add(chipPanel);

        heroPanel.add(heroIconLabel, BorderLayout.WEST);
        heroPanel.add(heroTextPanel, BorderLayout.CENTER);
        return heroPanel;
    }

    private JPanel buildFactsPanel() {
        addFactCard(FIELD_STATE, false, 0, 0, 1);
        addFactCard(FIELD_TYPE, false, 1, 0, 1);
        addFactCard(FIELD_MANUFACTURER, false, 0, 1, 1);
        addFactCard(FIELD_BRAND, false, 1, 1, 1);
        addFactCard(FIELD_PRODUCT, false, 0, 2, 1);
        addFactCard(FIELD_CODENAME, false, 1, 2, 1);
        addFactCard(FIELD_ARCHITECTURE, false, 0, 3, 1);
        addFactCard(FIELD_BATTERY, false, 1, 3, 1);
        addFactCard(FIELD_SERIAL, true, 0, 4, 2);
        addFactCard(FIELD_SOC, true, 0, 5, 2);
        return factsPanel;
    }

    private JPanel buildMetricsPanel() {
        metricsPanel.add(createMetricPanel(ramPanel, ramTitleLabel, ramValueLabel, ramProgressBar, ramFooterLabel));
        metricsPanel.add(createMetricPanel(
                storagePanel,
                storageTitleLabel,
                storageValueLabel,
                storageProgressBar,
                storageFooterLabel));
        return metricsPanel;
    }

    private JPanel createMetricPanel(
            JPanel container,
            JLabel titleLabel,
            JLabel valueLabel,
            JProgressBar progressBar,
            JLabel footerLabel) {
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setAlignmentX(LEFT_ALIGNMENT);
        container.setBorder(new EmptyBorder(14, 14, 14, 14));

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
        constraints.weightx = gridWidth == 2 ? 1.0 : 0.5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 10, gridX == 0 && gridWidth == 1 ? 10 : 0);
        if (gridWidth == 2) {
            constraints.insets = new Insets(0, 0, 10, 0);
        }
        factsPanel.add(factCard.panel(), constraints);
    }

    private void buildCapturePanel() {
        JPanel topActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topActionsPanel.setOpaque(false);

        captureButton.setUI(new BasicButtonUI());
        captureButton.setFocusPainted(false);
        captureButton.setPreferredSize(new Dimension(180, 42));
        topActionsPanel.add(captureButton);

        JPanel bottomActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomActionsPanel.setOpaque(false);

        saveScreenshotButton.setUI(new BasicButtonUI());
        saveScreenshotButton.setFocusPainted(false);
        saveScreenshotButton.setPreferredSize(new Dimension(180, 42));
        bottomActionsPanel.add(saveScreenshotButton);

        capturePanel.add(topActionsPanel, BorderLayout.NORTH);
        capturePanel.add(screenshotPreviewPanel, BorderLayout.CENTER);
        capturePanel.add(bottomActionsPanel, BorderLayout.SOUTH);
    }

    private GridBagConstraints buildSummaryConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.37;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(24, 24, 24, 18);
        return constraints;
    }

    private GridBagConstraints buildCaptureConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 0.63;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(24, 18, 24, 24);
        return constraints;
    }

    private void updateActionButtonsStyle() {
        styleActionButton(captureButton, true);
        styleActionButton(saveScreenshotButton, false);
    }

    private void styleActionButton(JButton button, boolean primary) {
        boolean enabled = button.isEnabled();
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));

        if (enabled) {
            button.setBackground(primary ? theme.actionBackground() : theme.surface());
            button.setForeground(primary ? theme.actionForeground() : theme.textPrimary());
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primary ? theme.actionBackground() : theme.border(), 1),
                    BorderFactory.createEmptyBorder(8, 18, 8, 18)));
            return;
        }

        button.setBackground(theme.secondarySurface());
        button.setForeground(theme.textSecondary());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.disabledBorder(), 1),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));
    }

    private void styleHeroIcon(AppTheme theme) {
        heroIconLabel.setOpaque(true);
        heroIconLabel.setBackground(theme.secondarySurface());
        heroIconLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        heroIconLabel.setIcon(createHeroIcon());
    }

    private void styleChip(JLabel label) {
        label.setOpaque(true);
        label.setBackground(theme.surface());
        label.setForeground(theme.textPrimary());
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
    }

    private void styleSurfaceCard(JPanel panel, boolean elevated) {
        panel.setOpaque(true);
        panel.setBackground(elevated ? theme.secondarySurface() : theme.surface());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
    }

    private void styleMetricCard(
            JPanel panel,
            JLabel titleLabel,
            JLabel valueLabel,
            JLabel footerLabel,
            JProgressBar progressBar) {
        styleSurfaceCard(panel, false);

        titleLabel.setForeground(theme.textSecondary());
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        valueLabel.setForeground(theme.textPrimary());
        valueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));

        footerLabel.setForeground(theme.textSecondary());
        footerLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        progressBar.setBackground(theme.background());
        progressBar.setForeground(theme.actionBackground());
        progressBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        progressBar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
    }

    private void resetMetric(JLabel valueLabel, JLabel footerLabel, JProgressBar progressBar) {
        valueLabel.setText("-");
        footerLabel.setText(Messages.format("home.metric.total", "-"));
        progressBar.setValue(0);
        progressBar.setString(Messages.text("common.noData"));
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

    private Icon createHeroIcon() {
        return new ToolbarIcon(ToolbarIcon.Type.DISPLAY, 24, theme.actionBackground());
    }

    private String primaryDeviceTitle(DeviceDetails details) {
        return details.model() == null || details.model().isBlank() || "-".equals(details.model())
                ? details.serial()
                : details.model();
    }

    private String secondaryDeviceTitle(DeviceDetails details) {
        String manufacturer = normalizeDisplayValue(details.manufacturer());
        String brand = normalizeDisplayValue(details.brand());
        String soc = normalizeDisplayValue(details.soc());

        StringBuilder builder = new StringBuilder();
        if (!manufacturer.isBlank()) {
            builder.append(manufacturer);
        }
        if (!brand.isBlank() && !brand.equalsIgnoreCase(manufacturer)) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(brand);
        }
        if (!soc.isBlank()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(soc);
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
                new Font(Font.SANS_SERIF, Font.BOLD, 18),
                theme.textPrimary());
    }

    private String asHtml(String text, int width) {
        return "<html><body style='width:" + width + "px'>" + text + "</body></html>";
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
            titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            valueLabel.setForeground(theme.textPrimary());
            valueLabel.setFont(new Font(Font.SANS_SERIF, wide ? Font.PLAIN : Font.BOLD, wide ? 12 : 15));
        }
    }
}
