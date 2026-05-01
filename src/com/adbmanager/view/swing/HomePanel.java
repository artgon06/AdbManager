package com.adbmanager.view.swing;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Locale;
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
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;

import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.view.Messages;

public class HomePanel extends JPanel {
    private static final int OUTER_TOP_PADDING = 30;
    private static final int OUTER_LEFT_PADDING = 30;
    private static final int OUTER_BOTTOM_PADDING = 30;
    private static final int COLUMN_GAP = 30;

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_MANUFACTURER = "manufacturer";
    private static final String FIELD_BRAND = "brand";
    private static final String FIELD_MODEL = "model";
    private static final String FIELD_PRODUCT = "product";
    private static final String FIELD_CODENAME = "codename";
    private static final String FIELD_ANDROID = "android";
    private static final String FIELD_SOC = "soc";
    private static final String FIELD_ARCHITECTURE = "architecture";
    private static final int SUMMARY_PREFERRED_WIDTH = 620;
    private static final int FACTS_HORIZONTAL_GAP = 31;
    private static final int FACTS_VERTICAL_GAP = 10;

    private final JButton powerButton = new JButton();
    private final JButton captureButton = new JButton();
    private final JButton saveScreenshotButton = new JButton();
    private final ScreenshotPreviewPanel screenshotPreviewPanel = new ScreenshotPreviewPanel();

    private final JPanel summaryPanel = new JPanel(new BorderLayout());
    private final JPanel capturePanel = new JPanel(new BorderLayout(0, 18));
    private final JPanel summaryContent = new JPanel(new GridBagLayout());
    private final JPanel heroPanel = new JPanel(new BorderLayout());
    private final JPanel heroTitlePanel = new JPanel();
    private final JLabel heroStateLabel = new JLabel();
    private final JLabel heroTitleLabel = new JLabel("-");

    private final JPanel factsPanel = new FactsPanel();
    private final Map<String, FactCard> factCards = new LinkedHashMap<>();

    private final JPanel metricsPanel = new JPanel(new java.awt.GridLayout(0, 3, 10, 10));
    private final JPanel batteryPanel = new MetricCardPanel();
    private final JLabel batteryTitleLabel = new JLabel();
    private final JLabel batteryValueLabel = new JLabel("-");
    private final JLabel batteryFooterLabel = new JLabel();
    private final JProgressBar batteryProgressBar = new MetricProgressBar();
    private final JPanel ramPanel = new MetricCardPanel();
    private final JLabel ramTitleLabel = new JLabel();
    private final JLabel ramValueLabel = new JLabel("-");
    private final JLabel ramFooterLabel = new JLabel();
    private final JProgressBar ramProgressBar = new MetricProgressBar();

    private final JPanel storagePanel = new MetricCardPanel();
    private final JLabel storageTitleLabel = new JLabel();
    private final JLabel storageValueLabel = new JLabel("-");
    private final JLabel storageFooterLabel = new JLabel();
    private final JProgressBar storageProgressBar = new MetricProgressBar();

    private BufferedImage currentScreenshot;
    private DeviceDetails currentDetails;
    private AppTheme theme = AppTheme.LIGHT;

    public HomePanel() {
        setLayout(new HomeLayout());
        buildSummaryPanel();
        buildCapturePanel();
        add(summaryPanel);
        add(capturePanel);
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
        clearDeviceDetails();
        clearScreenshot();
    }

    public void setDeviceDetails(DeviceDetails details) {
        currentDetails = details;

        updateHeroStateAppearance(details.state());
        heroTitleLabel.setText(primaryDeviceTitle(details));

        setFactValue(FIELD_TYPE, Messages.deviceTypeLabel(details.deviceType()));
        setFactValue(FIELD_MODEL, details.model());
        setFactValue(FIELD_MANUFACTURER, details.manufacturer());
        setFactValue(FIELD_BRAND, details.brand());
        setFactValue(FIELD_PRODUCT, details.productName());
        setFactValue(FIELD_CODENAME, details.codename());
        setFactValue(FIELD_ANDROID, details.apiLevel().equals("-")
                ? details.androidVersion()
                : details.androidVersion() + " (API " + details.apiLevel() + ")");
        setFactValue(FIELD_SOC, details.soc());
        setFactValue(FIELD_ARCHITECTURE, details.architecture());
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
        updateHeroStateAppearance(null);
        heroTitleLabel.setText(Messages.appName());

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

        factCards.get(FIELD_TYPE).setTitle(Messages.text("home.field.deviceType"));
        factCards.get(FIELD_MODEL).setTitle(Messages.text("home.field.model"));
        factCards.get(FIELD_MANUFACTURER).setTitle(Messages.text("home.field.manufacturer"));
        factCards.get(FIELD_BRAND).setTitle(Messages.text("home.field.brand"));
        factCards.get(FIELD_PRODUCT).setTitle(Messages.text("home.field.product"));
        factCards.get(FIELD_CODENAME).setTitle(Messages.text("home.field.codename"));
        factCards.get(FIELD_ANDROID).setTitle(Messages.text("home.field.android"));
        factCards.get(FIELD_SOC).setTitle(Messages.text("home.field.soc"));
        factCards.get(FIELD_ARCHITECTURE).setTitle(Messages.text("home.field.architecture"));

        ramTitleLabel.setText(Messages.text("home.ram.inUse").toUpperCase(Locale.ROOT));
        storageTitleLabel.setText(Messages.text("home.storage.inUse").toUpperCase(Locale.ROOT));
        batteryTitleLabel.setText(Messages.text("home.field.battery").toUpperCase(Locale.ROOT));

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
        factsPanel.setBackground(theme.background());
        metricsPanel.setBackground(theme.background());

        styleSurfaceCard(heroPanel, true);
        heroPanel.setBorder(BorderFactory.createEmptyBorder());
        heroTitlePanel.setOpaque(false);
        heroStateLabel.setIconTextGap(6);
        updateHeroStateAppearance(currentDetails == null ? null : currentDetails.state());
        heroStateLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 13));
        heroTitleLabel.setForeground(theme.textPrimary());
        heroTitleLabel.setFont(new Font("Inter", Font.BOLD, 32));

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
        summaryPanel.setPreferredSize(new Dimension(SUMMARY_PREFERRED_WIDTH, 0));
        summaryContent.setBorder(new EmptyBorder(0, 0, 0, 0));
        addSummarySection(buildHeroPanel(), 0, 0.0, new Insets(0, 0, 20, 0), GridBagConstraints.HORIZONTAL);
        addSummarySection(buildFactsPanel(), 1, 0.0, new Insets(0, 0, 10, 0), GridBagConstraints.HORIZONTAL);
        addSummarySection(buildMetricsPanel(), 2, 0.0, new Insets(0, 0, 0, 0), GridBagConstraints.HORIZONTAL);
        addSummarySection(createVerticalGlue(), 3, 1.0, new Insets(0, 0, 0, 0), GridBagConstraints.BOTH);

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

    private JPanel createVerticalGlue() {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        return spacer;
    }

    private JPanel buildHeroPanel() {
        JPanel topActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topActionsPanel.setOpaque(false);

        powerButton.setFocusPainted(false);
        powerButton.setRolloverEnabled(true);
        powerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        powerButton.getModel().addChangeListener(event -> stylePowerButton());
        powerButton.setPreferredSize(new Dimension(32, 32));
        topActionsPanel.add(powerButton);
        topActionsPanel.add(Box.createHorizontalStrut(10));

        saveScreenshotButton.setFocusPainted(false);
        saveScreenshotButton.setRolloverEnabled(true);
        saveScreenshotButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveScreenshotButton.getModel().addChangeListener(event -> styleActionButton(saveScreenshotButton, false));
        saveScreenshotButton.setPreferredSize(new Dimension(32, 32));
        topActionsPanel.add(saveScreenshotButton);
        topActionsPanel.add(Box.createHorizontalStrut(10));

        captureButton.setFocusPainted(false);
        captureButton.setRolloverEnabled(true);
        captureButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        captureButton.getModel().addChangeListener(event -> styleActionButton(captureButton, true));
        captureButton.setPreferredSize(new Dimension(32, 32));
        topActionsPanel.add(captureButton);

        heroTitlePanel.setLayout(new BoxLayout(heroTitlePanel, BoxLayout.Y_AXIS));
        heroTitlePanel.setOpaque(false);
        heroTitlePanel.add(heroStateLabel);
        heroTitlePanel.add(Box.createVerticalStrut(4));
        heroTitlePanel.add(heroTitleLabel);

        heroTitleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        heroPanel.add(heroTitlePanel, BorderLayout.WEST);
        heroPanel.add(topActionsPanel, BorderLayout.EAST);
        return heroPanel;
    }

    private JPanel buildFactsPanel() {
        factsPanel.removeAll();
        addFactCard(FIELD_TYPE, false);
        addFactCard(FIELD_MANUFACTURER, false);
        addFactCard(FIELD_BRAND, false);
        addFactCard(FIELD_MODEL, false);
        addFactCard(FIELD_ANDROID, false);
        addFactCard(FIELD_SOC, false);
        addFactCard(FIELD_ARCHITECTURE, false);
        addFactCard(FIELD_PRODUCT, false);
        addFactCard(FIELD_CODENAME, false);
        return factsPanel;
    }

    private JPanel buildMetricsPanel() {
        metricsPanel.removeAll();

        JPanel battery = createMetricPanel(
                batteryPanel,
                batteryTitleLabel,
                batteryValueLabel,
                batteryProgressBar,
                batteryFooterLabel);
        battery.setMinimumSize(new Dimension(15, 0));
        battery.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel ram = createMetricPanel(
                ramPanel,
                ramTitleLabel,
                ramValueLabel,
                ramProgressBar,
                ramFooterLabel);
        ram.setMinimumSize(new Dimension(15, 0));
        ram.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel storage = createMetricPanel(
                storagePanel,
                storageTitleLabel,
                storageValueLabel,
                storageProgressBar,
                storageFooterLabel);
        storage.setMinimumSize(new Dimension(15, 0));
        storage.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        metricsPanel.add(battery);
        metricsPanel.add(ram);
        metricsPanel.add(storage);

        return metricsPanel;
    }


    private JPanel createMetricPanel(
            JPanel container,
            JLabel titleLabel,
            JLabel valueLabel,
            JProgressBar progressBar,
            JLabel footerLabel) {
        container.removeAll();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setAlignmentX(LEFT_ALIGNMENT);
        container.setBorder(new EmptyBorder(10, 10, 10, 10));

        progressBar.setStringPainted(false);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 10));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));

        valueLabel.setVisible(true);
        footerLabel.setVisible(false);

        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);
        progressBar.setAlignmentX(LEFT_ALIGNMENT);

        container.add(titleLabel);
        container.add(Box.createVerticalStrut(2));
        container.add(valueLabel);
        container.add(Box.createVerticalStrut(5));
        container.add(progressBar);
        return container;
    }

    private void addFactCard(String key, boolean wide) {
        FactCard factCard = new FactCard(wide);
        factCards.put(key, factCard);
        factsPanel.add(factCard.panel());
    }

    private void buildCapturePanel() {
        capturePanel.add(screenshotPreviewPanel, BorderLayout.CENTER);
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

    private void styleSurfaceCard(JPanel panel, boolean elevated) {
        panel.setOpaque(true);
        panel.setBackground(elevated
                ? theme.background()
                : ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.52d));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(elevated ? 0 : 12, elevated ? 0 : 12, elevated ? 0 : 12,
                        elevated ? 0 : 12)));
    }

    private void styleMetricCard(
            JPanel panel,
            JLabel titleLabel,
            JLabel valueLabel,
            JLabel footerLabel,
            JProgressBar progressBar,
            Color accentColor) {
        if (panel instanceof MetricCardPanel metricCardPanel) {
            metricCardPanel.applySurface(
                    theme == AppTheme.DARK ? new Color(0x202020) : new Color(0xE0E0E0),
                    metricBorderStops());
        }
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        titleLabel.setForeground(withAlpha(theme == AppTheme.DARK ? new Color(0xF0F0F0) : new Color(0x101010), 0.75f));
        titleLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 11));

        valueLabel.setForeground(theme == AppTheme.DARK ? new Color(0xF0F0F0) : new Color(0x101010));
        valueLabel.setFont(new Font("Inter", Font.BOLD, 13));

        footerLabel.setForeground(theme.textSecondary());
        footerLabel.setFont(new Font("Inter", Font.PLAIN, 12));

        progressBar.setBackground(theme == AppTheme.DARK ? new Color(0x282828) : new Color(0xE4E4E4));
        progressBar.setForeground(accentColor);
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        progressBar.setFont(new Font("Inter", Font.BOLD, 11));
    }

    private Color[] metricBorderStops() {
        if (theme == AppTheme.LIGHT) {
            return new Color[] {
                    new Color(0xE0, 0xE0, 0xE0),
                    new Color(0xC8, 0xC8, 0xC8),
                    new Color(0xE0, 0xE0, 0xE0)
            };
        }
        return new Color[] {
                new Color(0x60, 0x60, 0x60),
                new Color(0x30, 0x30, 0x30),
                new Color(0x60, 0x60, 0x60)
        };
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

    private String formatHeroState(String state) {
        if (state == null || state.isBlank()) {
            return Messages.text("state.unknown").toUpperCase(Locale.ROOT);
        }
        return Messages.stateLabel(state).toUpperCase(Locale.ROOT);
    }

    private void updateHeroStateAppearance(String state) {
        Color stateColor = colorForState(state);
        heroStateLabel.setText(formatHeroState(state));
        heroStateLabel.setForeground(stateColor);
        heroStateLabel.setIcon(new ColorDotIcon(6, stateColor));
    }

    private Color colorForState(String state) {
        if (Messages.STATUS_CONNECTED.equals(state)) {
            return new Color(0x34C759);
        }
        if (Messages.STATUS_UNAUTHORIZED.equals(state)) {
            return new Color(0xFFC600);
        }
        if (Messages.STATUS_OFFLINE.equals(state)) {
            return new Color(0xFF383C);
        }
        if (Messages.STATUS_CONNECTING.equals(state)) {
            return new Color(0x0A84FF);
        }
        if (Messages.STATUS_RECOVERY.equals(state)) {
            return new Color(0xBF5AF2);
        }
        return new Color(0x8E8E93);
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

    private String asHtml(String text, int width) {
        return "<html><body style='width:" + width + "px'>" + text + "</body></html>";
    }

    private static final class ColorDotIcon implements Icon {

        private final int size;
        private final Color color;

        private ColorDotIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            try {
                graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                graphics2d.setColor(color);
                graphics2d.fillOval(x, y, size - 1, size - 1);
            } finally {
                graphics2d.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    static final class GradientRoundedBorder extends AbstractBorder {

        private final Color[] borderStops;
        private final int radius;

        private GradientRoundedBorder(Color[] borderStops, int radius) {
            this.borderStops = borderStops;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            try {
                graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float[] fractions = { 0.0f, 0.5f, 1.0f };
                graphics2d.setPaint(new LinearGradientPaint(0, 0, width, height, fractions, borderStops));
                graphics2d.setStroke(new BasicStroke(1f));
                float arc = Math.max(2f, radius * 2f);
                graphics2d.draw(new RoundRectangle2D.Float(
                        x + 0.5f,
                        y + 0.5f,
                        width - 2f,
                        height - 2f,
                        arc,
                        arc));
            } finally {
                graphics2d.dispose();
            }
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public Insets getBorderInsets(Component component, Insets insets) {
            insets.set(1, 1, 1, 1);
            return insets;
        }
    }

    private static final class MetricCardPanel extends JPanel {

        private static final int CORNER_RADIUS = 8;

        private Color fillColor = new Color(0x202020);
        private Color[] borderStops = {
                new Color(0x60, 0x60, 0x60),
                new Color(0x30, 0x30, 0x30),
                new Color(0x60, 0x60, 0x60)
        };

        private MetricCardPanel() {
            setOpaque(false);
        }

        private void applySurface(Color fillColor, Color[] borderStops) {
            this.fillColor = fillColor;
            this.borderStops = borderStops;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            try {
                graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float arc = CORNER_RADIUS * 2f;
                graphics2d.setColor(fillColor);
                graphics2d.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1f, getHeight() - 1f, arc, arc));
            } finally {
                graphics2d.dispose();
            }
            super.paintComponent(graphics);
        }

        @Override
        protected void paintBorder(Graphics graphics) {
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            try {
                graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float[] fractions = { 0.0f, 0.5f, 1.0f };
                graphics2d.setPaint(new LinearGradientPaint(0, 0, getWidth(), getHeight(), fractions, borderStops));
                graphics2d.setStroke(new BasicStroke(1f));
                float arc = CORNER_RADIUS * 2f;
                graphics2d.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 2f, getHeight() - 2f, arc, arc));
            } finally {
                graphics2d.dispose();
            }
        }
    }

    private static final class MetricProgressBar extends JProgressBar {

        private static final int BAR_HEIGHT = 10;
        private static final int CORNER_RADIUS = 3;

        private MetricProgressBar() {
            super(0, 100);
            setOpaque(false);
            setBorderPainted(false);
            setStringPainted(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            try {
                graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = Math.min(BAR_HEIGHT, getHeight());
                int y = (getHeight() - height) / 2;
                float arc = CORNER_RADIUS * 2f;

                Color topTrack = backgroundTrackTop();
                Color bottomTrack = backgroundTrackBottom();
                graphics2d.setPaint(new LinearGradientPaint(
                        0, y, 0, y + height,
                        new float[] { 0.0f, 1.0f },
                        new Color[] { topTrack, bottomTrack }));
                graphics2d.fill(new RoundRectangle2D.Float(0, y, width, height, arc, arc));

                int progressWidth = (int) Math
                        .round((getPercentComplete() == 0.0 ? 0.0 : getPercentComplete()) * width);
                if (progressWidth > 0) {
                    graphics2d.setPaint(new LinearGradientPaint(
                            0, y, 0, y + height,
                            new float[] { 0.0f, 1f / 3f, 1.0f },
                            new Color[] {
                                    new Color(0x2AC751),
                                    new Color(0x30E55D),
                                    new Color(0x146128)
                            }));
                    graphics2d.fill(new RoundRectangle2D.Float(0, y, progressWidth, height, arc, arc));
                }
            } finally {
                graphics2d.dispose();
            }
        }

        private Color backgroundTrackTop() {
            Color background = getBackground();
            if (background.equals(new Color(0xE4E4E4))) {
                return new Color(0xE4E4E4);
            }
            return new Color(0x282828);
        }

        private Color backgroundTrackBottom() {
            Color background = getBackground();
            if (background.equals(new Color(0xE4E4E4))) {
                return new Color(0xDCDCDC);
            }
            return new Color(0x383838);
        }
    }

    private final class HomeLayout implements LayoutManager2 {

        @Override
        public void addLayoutComponent(String name, Component component) {
        }

        @Override
        public void addLayoutComponent(Component component, Object constraints) {
        }

        @Override
        public void removeLayoutComponent(Component component) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Insets insets = parent.getInsets();
            Dimension summaryPreferred = summaryPanel.getPreferredSize();
            Dimension capturePreferred = capturePanel.getPreferredSize();
            int width = insets.left + OUTER_LEFT_PADDING + summaryPreferred.width + COLUMN_GAP + capturePreferred.width
                    + insets.right;
            int height = insets.top + OUTER_TOP_PADDING
                    + Math.max(summaryPreferred.height, capturePreferred.height)
                    + OUTER_BOTTOM_PADDING + insets.bottom;
            return new Dimension(width, height);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public Dimension maximumLayoutSize(Container target) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        @Override
        public float getLayoutAlignmentX(Container target) {
            return 0.0f;
        }

        @Override
        public float getLayoutAlignmentY(Container target) {
            return 0.0f;
        }

        @Override
        public void invalidateLayout(Container target) {
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            int availableWidth = Math.max(0, parent.getWidth() - insets.left - insets.right);
            int availableHeight = Math.max(0, parent.getHeight() - insets.top - insets.bottom);

            int contentX = insets.left + OUTER_LEFT_PADDING;
            int contentY = insets.top + OUTER_TOP_PADDING;
            int contentHeight = Math.max(0, availableHeight - OUTER_TOP_PADDING - OUTER_BOTTOM_PADDING);
            int previewWidth = screenshotPreviewPanel.preferredWidthForHeight(contentHeight);
            int summaryWidth = Math.max(0, availableWidth - OUTER_LEFT_PADDING - COLUMN_GAP - previewWidth);

            summaryPanel.setBounds(contentX, contentY, summaryWidth, contentHeight);
            capturePanel.setBounds(contentX + summaryWidth + COLUMN_GAP, contentY, previewWidth, contentHeight);
        }
    }

    private static final class WrapLayout implements LayoutManager {

        private final int horizontalGap;
        private final int verticalGap;

        private WrapLayout(int horizontalGap, int verticalGap) {
            this.horizontalGap = horizontalGap;
            this.verticalGap = verticalGap;
        }

        @Override
        public void addLayoutComponent(String name, java.awt.Component component) {
        }

        @Override
        public void removeLayoutComponent(java.awt.Component component) {
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension minimumSize = layoutSize(target, false);
            return minimumSize;
        }

        @Override
        public void layoutContainer(Container target) {
            synchronized (target.getTreeLock()) {
                Insets insets = target.getInsets();
                int availableWidth = Math.max(0, target.getWidth() - insets.left - insets.right);
                int x = insets.left;
                int y = insets.top;
                int rowHeight = 0;

                for (int index = 0; index < target.getComponentCount(); index++) {
                    java.awt.Component component = target.getComponent(index);
                    if (!component.isVisible()) {
                        continue;
                    }

                    Dimension componentSize = component.getPreferredSize();
                    if (x > insets.left && x + componentSize.width > insets.left + availableWidth) {
                        x = insets.left;
                        y += rowHeight + verticalGap;
                        rowHeight = 0;
                    }

                    component.setBounds(x, y, componentSize.width, componentSize.height);
                    x += componentSize.width + horizontalGap;
                    rowHeight = Math.max(rowHeight, componentSize.height);
                }
            }
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth <= 0 && target.getParent() != null) {
                    targetWidth = target.getParent().getWidth();
                }
                if (targetWidth <= 0) {
                    targetWidth = SUMMARY_PREFERRED_WIDTH;
                }

                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right);
                int rowWidth = 0;
                int rowHeight = 0;
                int requiredWidth = 0;
                int requiredHeight = insets.top + insets.bottom;

                for (int index = 0; index < target.getComponentCount(); index++) {
                    if (!target.getComponent(index).isVisible()) {
                        continue;
                    }

                    Dimension componentSize = preferred
                            ? target.getComponent(index).getPreferredSize()
                            : target.getComponent(index).getMinimumSize();

                    if (rowWidth > 0 && rowWidth + horizontalGap + componentSize.width > maxWidth) {
                        requiredWidth = Math.max(requiredWidth, rowWidth);
                        requiredHeight += rowHeight + verticalGap;
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    if (rowWidth > 0) {
                        rowWidth += horizontalGap;
                    }
                    rowWidth += componentSize.width;
                    rowHeight = Math.max(rowHeight, componentSize.height);
                }

                requiredWidth = Math.max(requiredWidth, rowWidth);
                requiredHeight += rowHeight;

                return new Dimension(
                        requiredWidth + insets.left + insets.right,
                        requiredHeight);
            }
        }
    }

    private final class FactsPanel extends JPanel {

        private FactsPanel() {
            super(new WrapLayout(FACTS_HORIZONTAL_GAP, FACTS_VERTICAL_GAP));
            setOpaque(true);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferredSize = super.getPreferredSize();
            return new Dimension(Math.min(preferredSize.width, SUMMARY_PREFERRED_WIDTH), preferredSize.height);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D graphics2d = (Graphics2D) graphics.create();
            try {
                graphics2d.setColor(theme == AppTheme.DARK ? new Color(0x303030) : new Color(0xE0E0E0));
                for (int index = 1; index < getComponentCount(); index++) {
                    java.awt.Component previous = getComponent(index - 1);
                    java.awt.Component current = getComponent(index);
                    if (!previous.isVisible() || !current.isVisible()) {
                        continue;
                    }

                    if (previous.getY() != current.getY()) {
                        continue;
                    }

                    int previousRight = previous.getX() + previous.getWidth();
                    int dividerX = previousRight + ((current.getX() - previousRight) / 2);
                    int dividerY = current.getY();
                    int dividerHeight = Math.max(previous.getHeight(), current.getHeight());
                    graphics2d.fillRect(dividerX, dividerY, 1, dividerHeight);
                }
            } finally {
                graphics2d.dispose();
            }
        }
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
            panel.add(Box.createVerticalStrut(2));
            panel.add(valueLabel);
        }

        private JPanel panel() {
            return panel;
        }

        private void setTitle(String title) {
            titleLabel.setText(title == null ? "" : title.toUpperCase(Locale.ROOT));
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
            panel.setOpaque(false);
            panel.setBorder(BorderFactory.createEmptyBorder());
            titleLabel.setForeground(
                    withAlpha(theme == AppTheme.DARK ? new Color(0xF0F0F0) : new Color(0x101010), 0.75f));
            titleLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
            valueLabel.setForeground(theme == AppTheme.DARK ? new Color(0xF0F0F0) : new Color(0x101010));
            valueLabel.setFont(new Font("Inter", Font.BOLD, 13));
        }
    }

    private Color withAlpha(Color color, float alpha) {
        int normalizedAlpha = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), normalizedAlpha);
    }
}
