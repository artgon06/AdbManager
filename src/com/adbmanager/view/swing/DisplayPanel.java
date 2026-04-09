package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.logic.model.DisplayInfo;
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
    private final JPanel scrcpyPanel = new JPanel(new BorderLayout());
    private final JPanel scrcpyContent = new JPanel();
    private final JLabel scrcpyDescriptionLabel = new JLabel();
    private final JLabel scrcpyNoteLabel = new JLabel();
    private final JPanel chipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
    private final Map<String, JLabel> fieldLabels = new LinkedHashMap<>();
    private final Map<String, JLabel> valueLabels = new LinkedHashMap<>();
    private final List<JPanel> rowPanels = new ArrayList<>();
    private final List<JLabel> dynamicValueLabels = new ArrayList<>();
    private final List<JLabel> roadmapChips = new ArrayList<>();
    private DeviceDetails currentDetails;
    private AppTheme theme = AppTheme.LIGHT;

    public DisplayPanel() {
        buildPanel();
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
        clearDeviceDetails();
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
    }

    public void clearDeviceDetails() {
        currentDetails = null;
        for (JLabel valueLabel : valueLabels.values()) {
            valueLabel.setText("-");
        }
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

        metricsPanel.setBorder(createSectionBorder(Messages.text("display.metrics.title"), theme));
        scrcpyPanel.setBorder(createSectionBorder(Messages.text("display.scrcpy.title"), theme));
        scrcpyDescriptionLabel.setText(asHtml(Messages.text("display.scrcpy.description")));
        scrcpyNoteLabel.setText(asHtml(Messages.text("display.scrcpy.note")));

        String[] chipKeys = {
                "display.scrcpy.feature.launch",
                "display.scrcpy.feature.virtualDisplays",
                "display.scrcpy.feature.cameras",
                "display.scrcpy.feature.microphone",
                "display.scrcpy.feature.recording",
                "display.scrcpy.feature.customFlags"
        };
        for (int index = 0; index < roadmapChips.size(); index++) {
            roadmapChips.get(index).setText(Messages.text(chipKeys[index]));
        }

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
        scrcpyPanel.setBackground(theme.surface());
        scrcpyContent.setBackground(theme.surface());
        chipPanel.setBackground(theme.surface());

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

        scrcpyDescriptionLabel.setForeground(theme.textPrimary());
        scrcpyDescriptionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        scrcpyNoteLabel.setForeground(theme.textSecondary());
        scrcpyNoteLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        for (JLabel chipLabel : roadmapChips) {
            chipLabel.setOpaque(true);
            chipLabel.setBackground(theme.secondarySurface());
            chipLabel.setForeground(theme.textPrimary());
            chipLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(theme.border(), 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            chipLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        }

        metricsPanel.setBorder(createSectionBorder(Messages.text("display.metrics.title"), theme));
        scrcpyPanel.setBorder(createSectionBorder(Messages.text("display.scrcpy.title"), theme));

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
        buildScrcpyPanel();

        content.add(metricsPanel, buildMetricsConstraints());
        content.add(scrcpyPanel, buildScrcpyConstraints());

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

    private void buildScrcpyPanel() {
        scrcpyContent.setLayout(new BoxLayout(scrcpyContent, BoxLayout.Y_AXIS));
        scrcpyContent.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        chipPanel.setOpaque(false);

        for (int index = 0; index < 6; index++) {
            JLabel chipLabel = new JLabel();
            roadmapChips.add(chipLabel);
            chipPanel.add(chipLabel);
        }

        scrcpyContent.add(scrcpyDescriptionLabel);
        scrcpyContent.add(Box.createVerticalStrut(18));
        scrcpyContent.add(chipPanel);
        scrcpyContent.add(Box.createVerticalStrut(18));
        scrcpyContent.add(scrcpyNoteLabel);
        scrcpyContent.add(Box.createVerticalGlue());

        scrcpyPanel.add(scrcpyContent, BorderLayout.CENTER);
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

    private GridBagConstraints buildMetricsConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.5;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 12);
        return constraints;
    }

    private GridBagConstraints buildScrcpyConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 0.5;
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

    private String asHtml(String text) {
        return "<html><body style='width: 340px'>" + text + "</body></html>";
    }
}
