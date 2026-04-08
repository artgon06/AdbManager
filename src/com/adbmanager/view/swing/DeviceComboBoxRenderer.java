package com.adbmanager.view.swing;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import com.adbmanager.logic.model.Device;
import com.adbmanager.view.Messages;

public class DeviceComboBoxRenderer extends DefaultListCellRenderer {

    private AppTheme theme = AppTheme.LIGHT;

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
    }

    @Override
    public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        setBackground(isSelected ? theme.selectionBackground() : theme.surface());
        setForeground(isSelected ? theme.selectionForeground() : theme.textPrimary());

        if (value instanceof Device device) {
            setText(buildLabel(device));
        }

        return this;
    }

    private String buildLabel(Device device) {
        String identity = firstNonBlank(device.model(), device.device(), device.serial());
        return Messages.stateLabel(device.state()) + " | " + device.serial() + " | " + identity;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "-";
    }
}
