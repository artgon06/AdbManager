package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.TitledBorder;

import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.view.Messages;

public class HomePanel extends JPanel {

    private final JButton captureButton = new JButton("Hacer captura");
    private final JButton saveScreenshotButton = new JButton("Guardar captura");
    private final ScreenshotPreviewPanel screenshotPreviewPanel = new ScreenshotPreviewPanel();
    private final JPanel summaryPanel = new JPanel(new BorderLayout());
    private final JPanel capturePanel = new JPanel(new BorderLayout(0, 18));
    private final JPanel summaryContent = new JPanel();
    private final JPanel ramPanel = new JPanel();
    private final Map<String, JLabel> valueLabels = new LinkedHashMap<>();
    private final List<JPanel> rowPanels = new ArrayList<>();
    private final List<JLabel> keyLabels = new ArrayList<>();
    private final List<JLabel> dynamicValueLabels = new ArrayList<>();
    private final JLabel ramUsageTitleLabel = new JLabel("RAM en uso");
    private final JLabel ramUsageValueLabel = new JLabel("-");
    private final JLabel ramTotalLabel = new JLabel("Total: -");
    private final JProgressBar ramUsageBar = new JProgressBar(0, 100);
    private BufferedImage currentScreenshot;
    private AppTheme theme = AppTheme.LIGHT;

    public HomePanel() {
        setLayout(new GridBagLayout());
        buildSummaryPanel();
        buildCapturePanel();
        add(summaryPanel, buildSummaryConstraints());
        add(capturePanel, buildCaptureConstraints());
        applyTheme(AppTheme.LIGHT);
        clearDeviceDetails();
        clearScreenshot();
    }

    public void setDeviceDetails(DeviceDetails details) {
        valueLabels.get("Estado").setText(Messages.stateLabel(details.state()));
        valueLabels.get("Serial").setText(details.serial());
        valueLabels.get("Fabricante").setText(details.manufacturer());
        valueLabels.get("Marca").setText(details.brand());
        valueLabels.get("Modelo").setText(details.model());
        valueLabels.get("Nombre en clave").setText(details.codename());
        valueLabels.get("Producto").setText(details.productName());
        valueLabels.get("Android").setText(details.androidVersion());
        valueLabels.get("API").setText(details.apiLevel());
        valueLabels.get("SoC").setText(details.soc());

        if (details.hasRamInfo()) {
            ramUsageValueLabel.setText(details.usedRamLabel());
            ramTotalLabel.setText("Total: " + details.totalRamLabel());
            ramUsageBar.setValue(details.ramUsagePercent());
            ramUsageBar.setString(details.usedRamLabel() + " / " + details.totalRamLabel());
        } else {
            ramUsageValueLabel.setText("-");
            ramTotalLabel.setText("Total: -");
            ramUsageBar.setValue(0);
            ramUsageBar.setString("Sin datos");
        }
    }

    public void clearDeviceDetails() {
        for (JLabel valueLabel : valueLabels.values()) {
            valueLabel.setText("-");
        }
        ramUsageValueLabel.setText("-");
        ramTotalLabel.setText("Total: -");
        ramUsageBar.setValue(0);
        ramUsageBar.setString("Sin datos");
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

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        setBackground(theme.background());

        summaryPanel.setBackground(theme.surface());
        capturePanel.setBackground(theme.background());
        summaryContent.setBackground(theme.surface());
        ramPanel.setBackground(theme.secondarySurface());

        for (JPanel rowPanel : rowPanels) {
            rowPanel.setBackground(theme.surface());
        }

        for (JLabel keyLabel : keyLabels) {
            keyLabel.setForeground(theme.textSecondary());
            keyLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        }

        for (JLabel valueLabel : dynamicValueLabels) {
            valueLabel.setForeground(theme.textPrimary());
            valueLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        }

        ramUsageTitleLabel.setForeground(theme.textSecondary());
        ramUsageTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        ramUsageValueLabel.setForeground(theme.textPrimary());
        ramUsageValueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        ramTotalLabel.setForeground(theme.textSecondary());
        ramTotalLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        ramUsageBar.setBackground(theme.background());
        ramUsageBar.setForeground(theme.actionBackground());
        ramUsageBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        ramUsageBar.setStringPainted(true);
        ramUsageBar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        ramUsageBar.setString(ramUsageBar.getString() == null ? "Sin datos" : ramUsageBar.getString());
        ramUsageBar.setForeground(theme.actionBackground());

        summaryPanel.setBorder(createSectionBorder("RESUMEN", theme));
        screenshotPreviewPanel.applyTheme(theme);
        updateActionButtonsStyle();
        repaint();
    }

    private void buildSummaryPanel() {
        summaryPanel.setPreferredSize(new Dimension(380, 0));

        summaryContent.setLayout(new BoxLayout(summaryContent, BoxLayout.Y_AXIS));
        summaryContent.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        summaryContent.add(createInfoRow("Estado", "Estado"));
        summaryContent.add(createInfoRow("Serial", "Serial"));
        summaryContent.add(createInfoRow("Fabricante", "Fabricante"));
        summaryContent.add(createInfoRow("Marca", "Marca"));
        summaryContent.add(createInfoRow("Modelo", "Modelo"));
        summaryContent.add(createInfoRow("Nombre en clave", "Nombre en clave"));
        summaryContent.add(createInfoRow("Producto", "Producto"));
        summaryContent.add(createInfoRow("Android", "Android"));
        summaryContent.add(createInfoRow("API", "API"));
        summaryContent.add(createInfoRow("SoC", "SoC"));
        summaryContent.add(Box.createVerticalStrut(18));
        summaryContent.add(buildRamPanel());
        summaryContent.add(Box.createVerticalGlue());

        summaryPanel.add(summaryContent, BorderLayout.CENTER);
    }

    private JPanel buildRamPanel() {
        ramPanel.setLayout(new BoxLayout(ramPanel, BoxLayout.Y_AXIS));
        ramPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        ramPanel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(ramUsageTitleLabel, BorderLayout.WEST);
        headerPanel.add(ramUsageValueLabel, BorderLayout.EAST);

        ramUsageBar.setPreferredSize(new Dimension(0, 18));
        ramUsageBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        ramUsageBar.setBorderPainted(false);

        ramPanel.add(headerPanel);
        ramPanel.add(Box.createVerticalStrut(10));
        ramPanel.add(ramUsageBar);
        ramPanel.add(Box.createVerticalStrut(8));
        ramPanel.add(ramTotalLabel);

        return ramPanel;
    }

    private JPanel createInfoRow(String key, String labelText) {
        JPanel rowPanel = new JPanel(new BorderLayout(14, 0));
        rowPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        rowPanel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel keyLabel = new JLabel(labelText);
        JLabel valueLabel = new JLabel("-");
        valueLabel.setHorizontalAlignment(JLabel.RIGHT);

        rowPanels.add(rowPanel);
        keyLabels.add(keyLabel);
        dynamicValueLabels.add(valueLabel);
        valueLabels.put(key, valueLabel);

        rowPanel.add(keyLabel, BorderLayout.WEST);
        rowPanel.add(valueLabel, BorderLayout.CENTER);
        return rowPanel;
    }

    private void buildCapturePanel() {
        JPanel topActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topActionsPanel.setOpaque(false);

        captureButton.setFocusPainted(false);
        captureButton.setPreferredSize(new Dimension(180, 42));
        topActionsPanel.add(captureButton);

        JPanel bottomActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomActionsPanel.setOpaque(false);

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
        constraints.weightx = 0.34;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(24, 24, 24, 18);
        return constraints;
    }

    private GridBagConstraints buildCaptureConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 0.66;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(24, 18, 24, 24);
        return constraints;
    }

    private void updateActionButtonsStyle() {
        styleActionButton(captureButton, true, captureButton.isEnabled());
        styleActionButton(saveScreenshotButton, false, saveScreenshotButton.isEnabled());
    }

    private void styleActionButton(JButton button, boolean primary, boolean enabled) {
        button.setOpaque(true);
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

    private TitledBorder createSectionBorder(String title, AppTheme theme) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 18),
                theme.textPrimary());
    }
}
