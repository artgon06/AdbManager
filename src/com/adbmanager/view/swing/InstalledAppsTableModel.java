package com.adbmanager.view.swing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.adbmanager.logic.model.InstalledApp;
import com.adbmanager.view.Messages;

public class InstalledAppsTableModel extends AbstractTableModel {

    private final List<InstalledApp> applications = new ArrayList<>();
    private final String[] columnKeys = {
            "apps.column.name",
            "apps.column.package",
            "apps.column.storage"
    };

    public void setApplications(List<InstalledApp> newApplications) {
        applications.clear();
        applications.addAll(newApplications == null ? List.of() : newApplications);
        sortApplications();
        fireTableDataChanged();
    }

    public void updateApplication(InstalledApp application) {
        updateApplications(application == null ? List.of() : List.of(application));
    }

    public void updateApplications(List<InstalledApp> updatedApplications) {
        if (updatedApplications == null || updatedApplications.isEmpty()) {
            return;
        }

        boolean changed = false;
        for (InstalledApp application : updatedApplications) {
            if (application == null) {
                continue;
            }

            boolean replaced = false;
            for (int index = 0; index < applications.size(); index++) {
                if (applications.get(index).packageName().equals(application.packageName())) {
                    applications.set(index, application);
                    replaced = true;
                    changed = true;
                    break;
                }
            }

            if (!replaced) {
                applications.add(application);
                changed = true;
            }
        }

        if (!changed) {
            return;
        }

        sortApplications();
        fireTableDataChanged();
    }

    public List<InstalledApp> getApplications() {
        return List.copyOf(applications);
    }

    public InstalledApp getApplicationAt(int modelRow) {
        if (modelRow < 0 || modelRow >= applications.size()) {
            return null;
        }
        return applications.get(modelRow);
    }

    public void refreshTexts() {
        fireTableStructureChanged();
    }

    private void sortApplications() {
        applications.sort(Comparator
                .comparing(InstalledApp::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(InstalledApp::packageName, String.CASE_INSENSITIVE_ORDER));
    }

    @Override
    public int getRowCount() {
        return applications.size();
    }

    @Override
    public int getColumnCount() {
        return columnKeys.length;
    }

    @Override
    public String getColumnName(int column) {
        return Messages.text(columnKeys[column]);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        InstalledApp application = applications.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> application.displayName();
            case 1 -> application.packageName();
            case 2 -> application.storageLabel();
            default -> "";
        };
    }
}
