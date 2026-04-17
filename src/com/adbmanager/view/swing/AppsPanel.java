package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.datatransfer.StringSelection;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

import com.adbmanager.logic.model.AppDetails;
import com.adbmanager.logic.model.AppBackgroundMode;
import com.adbmanager.logic.model.AppPermission;
import com.adbmanager.logic.model.InstalledApp;
import com.adbmanager.view.Messages;

public class AppsPanel extends JPanel {

    public interface PermissionToggleHandler {
        void onToggle(String packageName, String permission, boolean granted);
    }

    public interface BackgroundModeChangeHandler {
        void onChange(String packageName, AppBackgroundMode mode);
    }

    private static final String DETAILS_EMPTY_KEY = "empty";
    private static final String DETAILS_LOADING_KEY = "loading";
    private static final String DETAILS_CONTENT_KEY = "details";

    private static final String FIELD_NAME = "name";
    private static final String FIELD_PACKAGE = "package";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_VERSION = "version";
    private static final String FIELD_TARGET_SDK = "targetSdk";
    private static final String FIELD_MIN_SDK = "minSdk";
    private static final String FIELD_INSTALLER = "installer";
    private static final String FIELD_STORAGE = "storage";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_CACHE = "cache";

    private final java.awt.CardLayout detailsCardLayout = new java.awt.CardLayout();
    private final JLabel titleLabel = new JLabel();
    private final JPanel listPanel = new JPanel(new BorderLayout(0, 18));
    private final JPanel detailsPanel = new JPanel(detailsCardLayout);
    private final JPanel listHeaderPanel = new JPanel();
    private final JPanel filtersPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
    private final JPanel searchFieldPanel = new JPanel(new BorderLayout(8, 0));
    private final JPanel listStatusPanel = new JPanel(new BorderLayout());
    private final JLabel searchLabel = new JLabel();
    private final JLabel listStatusLabel = new JLabel();
    private final JLabel visibleCountLabel = new JLabel();
    private final JTextField searchField = new JTextField();
    private final JButton clearSearchButton = new JButton("x");
    private final JButton installButton = new JButton();
    private final JCheckBox userAppsFilter = new JCheckBox();
    private final JCheckBox systemAppsFilter = new JCheckBox();
    private final JCheckBox disabledAppsFilter = new JCheckBox();
    private final InstalledAppsTableModel tableModel = new InstalledAppsTableModel();
    private final JTable appsTable = new JTable(tableModel);
    private final TableRowSorter<InstalledAppsTableModel> rowSorter = new TableRowSorter<>(tableModel);
    private final JScrollPane tableScrollPane = new JScrollPane(appsTable);
    private final JSplitPane contentSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    private final JPanel detailsEmptyPanel = new JPanel();
    private final JLabel detailsEmptyTitleLabel = new JLabel();
    private final WrappingTextArea detailsEmptySubtitleLabel = new WrappingTextArea();
    private final JPanel detailsLoadingPanel = new JPanel();
    private final JLabel detailsLoadingTitleLabel = new JLabel();
    private final WrappingTextArea detailsLoadingSubtitleLabel = new WrappingTextArea();

    private final JPanel detailsContentPanel = new JPanel(new BorderLayout(0, 18));
    private final JPanel headerAndActionsPanel = new JPanel();
    private final JPanel appHeaderPanel = new JPanel(new BorderLayout(18, 0));
    private final JLabel appIconLabel = new JLabel();
    private final JPanel appHeaderTextPanel = new JPanel();
    private final JLabel appNameTitleLabel = new JLabel("-");
    private final JLabel appPackageTitleLabel = new JLabel("-");
    private final JPanel actionsCard = new JPanel(new BorderLayout(0, 12));
    private final JPanel actionButtonsPanel = new JPanel(new GridLayout(0, 4, 8, 8));
    private final JPanel backgroundModeCard = new JPanel(new BorderLayout(0, 10));
    private final JPanel backgroundModeHeaderPanel = new JPanel(new BorderLayout(10, 0));
    private final JLabel backgroundModeTitleLabel = new JLabel();
    private final JLabel backgroundModeValueLabel = new JLabel("-");
    private final JSlider backgroundModeSlider = new JSlider(0, 2, 1);
    private final JPanel backgroundModeLegendPanel = new JPanel(new GridLayout(1, 3, 8, 0));
    private final JLabel backgroundUnrestrictedLabel = new JLabel();
    private final JLabel backgroundOptimizedLabel = new JLabel();
    private final JLabel backgroundRestrictedLabel = new JLabel();
    private final JPanel infoAndPermissionsPanel = new JPanel();
    private final JPanel infoPanel = new JPanel();
    private final JPanel permissionsCard = new JPanel(new BorderLayout(0, 14));
    private final JPanel permissionsPanel = new JPanel();
    private final JScrollPane detailsScrollPane = new JScrollPane(detailsContentPanel);
    private final JScrollPane permissionsScrollPane = new JScrollPane(permissionsPanel);

    private final Map<String, JLabel> fieldLabels = new LinkedHashMap<>();
    private final Map<String, JLabel> valueLabels = new LinkedHashMap<>();
    private final List<JCheckBox> filterCheckBoxes = new ArrayList<>();
    private final List<JCheckBox> permissionCheckBoxes = new ArrayList<>();
    private final List<JButton> actionButtons = new ArrayList<>();

    private final JButton openButton = new JButton();
    private final JButton stopButton = new JButton();
    private final JButton uninstallButton = new JButton();
    private final JButton toggleEnabledButton = new JButton();
    private final JButton clearDataButton = new JButton();
    private final JButton clearCacheButton = new JButton();
    private final JButton exportApkButton = new JButton();

    private AppTheme theme = AppTheme.LIGHT;
    private AppDetails currentDetails;
    private boolean syncingSelection;
    private boolean syncingPermissions;
    private boolean syncingBackgroundMode;
    private int savedDividerLocation = -1;
    private Runnable selectionAction = () -> {
    };
    private Runnable visibleApplicationsChangedAction = () -> {
    };
    private PermissionToggleHandler permissionToggleHandler = (packageName, permission, granted) -> {
    };
    private BackgroundModeChangeHandler backgroundModeChangeHandler = (packageName, mode) -> {
    };

    public AppsPanel() {
        buildPanel();
        bindFilters();
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
        clearApplications();
        clearApplicationDetails();
    }

    public void setApplicationSelectionAction(Runnable action) {
        selectionAction = action == null ? () -> {
        } : action;
    }

    public void setPermissionToggleHandler(PermissionToggleHandler handler) {
        permissionToggleHandler = handler == null ? (packageName, permission, granted) -> {
        } : handler;
    }

    public void setBackgroundModeChangeHandler(BackgroundModeChangeHandler handler) {
        backgroundModeChangeHandler = handler == null ? (packageName, mode) -> {
        } : handler;
    }

    public void setVisibleApplicationsChangedAction(Runnable action) {
        visibleApplicationsChangedAction = action == null ? () -> {
        } : action;
    }

    public void setOpenAction(ActionListener actionListener) {
        openButton.addActionListener(actionListener);
    }

    public void setStopAction(ActionListener actionListener) {
        stopButton.addActionListener(actionListener);
    }

    public void setUninstallAction(ActionListener actionListener) {
        uninstallButton.addActionListener(actionListener);
    }

    public void setToggleEnabledAction(ActionListener actionListener) {
        toggleEnabledButton.addActionListener(actionListener);
    }

    public void setClearDataAction(ActionListener actionListener) {
        clearDataButton.addActionListener(actionListener);
    }

    public void setClearCacheAction(ActionListener actionListener) {
        clearCacheButton.addActionListener(actionListener);
    }

    public void setExportApkAction(ActionListener actionListener) {
        exportApkButton.addActionListener(actionListener);
    }

    public void setInstallAction(ActionListener actionListener) {
        installButton.addActionListener(actionListener);
    }

    public void setApplications(List<InstalledApp> applications, String selectedPackageName) {
        syncingSelection = true;
        try {
            tableModel.setApplications(applications);
            applyFiltersInternal(false);
            selectPackage(selectedPackageName);
        } finally {
            syncingSelection = false;
        }
        restoreSplitPaneLocation();
        SwingUtilities.invokeLater(visibleApplicationsChangedAction);
    }

    public void updateApplication(InstalledApp application) {
        updateApplications(application == null ? List.of() : List.of(application));
    }

    public void updateApplications(List<InstalledApp> applications) {
        if (applications == null || applications.isEmpty()) {
            return;
        }

        String selectedPackage = getSelectedPackageName();
        syncingSelection = true;
        try {
            tableModel.updateApplications(applications);
            applyFiltersInternal(false);
            String preferredPackage = selectedPackage;
            if ((preferredPackage == null || preferredPackage.isBlank())
                    && applications.size() == 1
                    && applications.get(0) != null) {
                preferredPackage = applications.get(0).packageName();
            }
            selectPackage(preferredPackage);
        } finally {
            syncingSelection = false;
        }
        restoreSplitPaneLocation();
        SwingUtilities.invokeLater(visibleApplicationsChangedAction);
    }

    public void clearApplications() {
        setApplications(List.of(), null);
    }

    public String getSelectedPackageName() {
        int viewRow = appsTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }

        InstalledApp application = tableModel.getApplicationAt(appsTable.convertRowIndexToModel(viewRow));
        return application == null ? null : application.packageName();
    }

    public AppDetails getCurrentDetails() {
        return currentDetails;
    }

    public List<String> getVisibleApplicationPackages(int extraRows) {
        if (appsTable.getRowCount() <= 0) {
            return List.of();
        }

        Rectangle visibleRect = appsTable.getVisibleRect();
        int firstVisibleRow = visibleRect.height > 0
                ? appsTable.rowAtPoint(new Point(0, Math.max(0, visibleRect.y)))
                : 0;
        if (firstVisibleRow < 0) {
            firstVisibleRow = 0;
        }

        int lastVisibleRow = visibleRect.height > 0
                ? appsTable.rowAtPoint(new Point(0, Math.max(0, visibleRect.y + visibleRect.height - 1)))
                : Math.min(appsTable.getRowCount() - 1, extraRows);
        if (lastVisibleRow < 0) {
            lastVisibleRow = Math.min(appsTable.getRowCount() - 1, firstVisibleRow + extraRows);
        }

        lastVisibleRow = Math.min(appsTable.getRowCount() - 1, lastVisibleRow + Math.max(0, extraRows));
        List<String> packageNames = new ArrayList<>();
        for (int row = firstVisibleRow; row <= lastVisibleRow; row++) {
            InstalledApp application = tableModel.getApplicationAt(appsTable.convertRowIndexToModel(row));
            if (application != null && !packageNames.contains(application.packageName())) {
                packageNames.add(application.packageName());
            }
        }
        return packageNames;
    }

    public void setApplicationsLoading(boolean loading, String statusText) {
        String normalizedText = statusText == null ? "" : statusText.trim();
        listStatusLabel.putClientProperty("loading", loading);
        listStatusLabel.setText(normalizedText);
        listStatusLabel.setVisible(!normalizedText.isBlank());
        styleStatusLabel();
    }

    public void showApplicationDetailsLoading(String packageName) {
        currentDetails = null;
        detailsLoadingTitleLabel.setText(Messages.text("apps.loading.details"));
        detailsLoadingSubtitleLabel.setText(
                Messages.format("apps.loading.details.subtitle", packageName == null || packageName.isBlank() ? "-" : packageName));
        updateBackgroundModePresentation(null);
        setApplicationActionsEnabled(false);
        detailsCardLayout.show(detailsPanel, DETAILS_LOADING_KEY);
        restoreSplitPaneLocation();
    }

    public void setApplicationDetails(AppDetails details) {
        currentDetails = Objects.requireNonNull(details);

        appNameTitleLabel.setText(details.displayName());
        appPackageTitleLabel.setText(details.app().packageName());
        appIconLabel.setIcon(createApplicationIcon(details));
        appIconLabel.setText("");

        setValueText(FIELD_NAME, details.displayName());
        setValueText(FIELD_PACKAGE, details.app().packageName());
        setValueText(FIELD_STATE, details.app().disabled()
                ? Messages.text("state.disabled")
                : Messages.text("state.enabled"));
        setValueText(FIELD_TYPE, details.app().systemApp()
                ? Messages.text("apps.type.system")
                : Messages.text("apps.type.user"));
        setValueText(FIELD_VERSION, details.versionName() + " (" + details.versionCode() + ")");
        setValueText(FIELD_TARGET_SDK, details.targetSdk());
        setValueText(FIELD_MIN_SDK, details.minSdk());
        setValueText(FIELD_INSTALLER, details.installerPackage());
        setValueText(FIELD_STORAGE, details.totalStorageLabel());
        setValueText(FIELD_CODE, details.codeSizeLabel());
        setValueText(FIELD_DATA, details.dataSizeLabel());
        setValueText(FIELD_CACHE, details.cacheSizeLabel());

        updateBackgroundModePresentation(details.backgroundMode());
        rebuildPermissions(details.permissions());
        updateToggleButtonText(details.app().disabled());
        setApplicationActionsEnabled(true);
        detailsCardLayout.show(detailsPanel, DETAILS_CONTENT_KEY);
        restoreSplitPaneLocation();
    }

    public void clearApplicationDetails() {
        currentDetails = null;
        appNameTitleLabel.setText("-");
        appPackageTitleLabel.setText("-");
        appIconLabel.setIcon(createFallbackIcon("-", "-"));
        appIconLabel.setText("");

        for (String fieldKey : valueLabels.keySet()) {
            setValueText(fieldKey, "-");
        }
        updateBackgroundModePresentation(null);
        rebuildPermissions(List.of());
        updateToggleButtonText(false);
        setApplicationActionsEnabled(false);
        detailsCardLayout.show(detailsPanel, DETAILS_EMPTY_KEY);
        restoreSplitPaneLocation();
    }

    public void setApplicationsEnabled(boolean enabled) {
        appsTable.setEnabled(enabled);
        searchField.setEnabled(enabled);
        clearSearchButton.setEnabled(enabled && !searchField.getText().isBlank());
        installButton.setEnabled(enabled);
        for (JCheckBox filterCheckBox : filterCheckBoxes) {
            filterCheckBox.setEnabled(enabled);
        }
        styleInstallButton();
    }

    public void setApplicationActionsEnabled(boolean enabled) {
        boolean hasDetails = currentDetails != null;
        for (JButton button : actionButtons) {
            boolean buttonEnabled = enabled && hasDetails;
            if (buttonEnabled
                    && currentDetails != null
                    && currentDetails.app().disabled()
                    && button == stopButton) {
                buttonEnabled = false;
            }
            button.setEnabled(buttonEnabled);
            styleActionButton(button, buttonEnabled);
        }

        for (JCheckBox permissionCheckBox : permissionCheckBoxes) {
            permissionCheckBox.setEnabled(enabled
                    && hasDetails
                    && Boolean.TRUE.equals(permissionCheckBox.getClientProperty("changeable")));
            stylePermissionCheckBox(permissionCheckBox);
        }

        backgroundModeSlider.setEnabled(enabled && hasDetails);
        styleBackgroundModeSlider();
        updateBackgroundModePresentation(hasDetails ? currentDetails.backgroundMode() : null);
    }

    public void refreshTexts() {
        titleLabel.setText(Messages.text("apps.title"));
        searchLabel.setText(Messages.text("apps.search.label"));
        searchField.setToolTipText(Messages.text("apps.search.placeholder"));
        installButton.setToolTipText(Messages.text("apps.install.open"));
        userAppsFilter.setText(Messages.text("apps.filter.user"));
        systemAppsFilter.setText(Messages.text("apps.filter.system"));
        disabledAppsFilter.setText(Messages.text("apps.filter.disabled"));
        updateVisibleCountLabel();

        tableModel.refreshTexts();
        appsTable.setRowSorter(rowSorter);
        appsTable.setDefaultRenderer(Object.class, new AppsTableCellRenderer());
        if (appsTable.getColumnModel().getColumnCount() >= 3) {
            appsTable.getColumnModel().getColumn(2).setCellRenderer(new StorageCellRenderer());
        }
        appsTable.getTableHeader().setReorderingAllowed(false);
        listPanel.setBorder(createSectionBorder(Messages.text("apps.list.title")));
        detailsPanel.setBorder(createSectionBorder(Messages.text("apps.details.title")));
        permissionsCard.setBorder(BorderFactory.createCompoundBorder(
                createSectionBorder(Messages.text("apps.permissions.title")),
                BorderFactory.createEmptyBorder(12, 14, 14, 14)));
        actionsCard.setBorder(BorderFactory.createCompoundBorder(
                createSectionBorder(Messages.text("apps.actions.title")),
                BorderFactory.createEmptyBorder(12, 14, 14, 14)));
        backgroundModeCard.setBorder(BorderFactory.createCompoundBorder(
                createSectionBorder(Messages.text("apps.energy.title")),
                BorderFactory.createEmptyBorder(12, 14, 14, 14)));

        detailsEmptyTitleLabel.setText(Messages.text("apps.details.empty.title"));
        detailsEmptySubtitleLabel.setText(Messages.text("apps.details.empty.subtitle"));
        detailsLoadingTitleLabel.setText(Messages.text("apps.loading.details"));
        detailsLoadingSubtitleLabel.setText(Messages.text("apps.loading.details.subtitle"));

        fieldLabels.get(FIELD_NAME).setText(Messages.text("apps.field.name"));
        fieldLabels.get(FIELD_PACKAGE).setText(Messages.text("apps.field.package"));
        fieldLabels.get(FIELD_STATE).setText(Messages.text("apps.field.state"));
        fieldLabels.get(FIELD_TYPE).setText(Messages.text("apps.field.type"));
        fieldLabels.get(FIELD_VERSION).setText(Messages.text("apps.field.version"));
        fieldLabels.get(FIELD_TARGET_SDK).setText(Messages.text("apps.field.targetSdk"));
        fieldLabels.get(FIELD_MIN_SDK).setText(Messages.text("apps.field.minSdk"));
        fieldLabels.get(FIELD_INSTALLER).setText(Messages.text("apps.field.installer"));
        fieldLabels.get(FIELD_STORAGE).setText(Messages.text("apps.field.storage"));
        fieldLabels.get(FIELD_CODE).setText(Messages.text("apps.field.code"));
        fieldLabels.get(FIELD_DATA).setText(Messages.text("apps.field.data"));
        fieldLabels.get(FIELD_CACHE).setText(Messages.text("apps.field.cache"));

        openButton.setText(Messages.text("apps.action.open"));
        stopButton.setText(Messages.text("apps.action.stop"));
        uninstallButton.setText(Messages.text("apps.action.uninstall"));
        clearDataButton.setText(Messages.text("apps.action.clearData"));
        clearCacheButton.setText(Messages.text("apps.action.clearCache"));
        exportApkButton.setText(Messages.text("apps.action.exportApk"));
        updateToggleButtonText(currentDetails != null && currentDetails.app().disabled());
        backgroundModeTitleLabel.setText(Messages.text("apps.energy.title"));
        backgroundUnrestrictedLabel.setText(Messages.text("apps.energy.unrestricted"));
        backgroundOptimizedLabel.setText(Messages.text("apps.energy.optimized"));
        backgroundRestrictedLabel.setText(Messages.text("apps.energy.restricted"));

        updateActionButtonIcons();

        if (currentDetails == null) {
            clearApplicationDetails();
        } else {
            setApplicationDetails(currentDetails);
        }
    }

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        setBackground(theme.background());
        titleLabel.setForeground(theme.textPrimary());
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));

        listPanel.setBackground(theme.background());
        listHeaderPanel.setBackground(theme.background());
        filtersPanel.setBackground(theme.background());
        searchFieldPanel.setBackground(theme.secondarySurface());
        detailsPanel.setBackground(theme.background());
        detailsEmptyPanel.setBackground(theme.background());
        detailsLoadingPanel.setBackground(theme.background());
        detailsContentPanel.setBackground(theme.background());
        headerAndActionsPanel.setBackground(theme.background());
        appHeaderPanel.setBackground(theme.background());
        appHeaderTextPanel.setBackground(theme.background());
        actionsCard.setBackground(theme.background());
        actionButtonsPanel.setBackground(theme.background());
        backgroundModeCard.setBackground(theme.background());
        backgroundModeHeaderPanel.setBackground(theme.background());
        backgroundModeLegendPanel.setBackground(theme.background());
        infoAndPermissionsPanel.setBackground(theme.background());
        infoPanel.setBackground(theme.background());
        permissionsCard.setBackground(theme.background());
        permissionsPanel.setBackground(theme.background());

        searchLabel.setForeground(theme.textSecondary());
        searchLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        visibleCountLabel.setForeground(theme.textSecondary());
        visibleCountLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        visibleCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        styleStatusLabel();

        styleSearchField();
        styleInstallButton();
        styleFilterCheckBox(userAppsFilter);
        styleFilterCheckBox(systemAppsFilter);
        styleFilterCheckBox(disabledAppsFilter);

        listPanel.setBorder(createSectionBorder(Messages.text("apps.list.title")));
        detailsPanel.setBorder(createSectionBorder(Messages.text("apps.details.title")));
        permissionsCard.setBorder(BorderFactory.createCompoundBorder(
                createSectionBorder(Messages.text("apps.permissions.title")),
                BorderFactory.createEmptyBorder(12, 14, 14, 14)));
        actionsCard.setBorder(BorderFactory.createCompoundBorder(
                createSectionBorder(Messages.text("apps.actions.title")),
                BorderFactory.createEmptyBorder(12, 14, 14, 14)));
        backgroundModeCard.setBorder(BorderFactory.createCompoundBorder(
                createSectionBorder(Messages.text("apps.energy.title")),
                BorderFactory.createEmptyBorder(12, 14, 14, 14)));

        detailsEmptyTitleLabel.setForeground(theme.textPrimary());
        detailsEmptyTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        detailsEmptySubtitleLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 15), theme.textSecondary());
        detailsLoadingTitleLabel.setForeground(theme.textPrimary());
        detailsLoadingTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        detailsLoadingSubtitleLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 15), theme.textSecondary());

        appNameTitleLabel.setForeground(theme.textPrimary());
        appNameTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        appPackageTitleLabel.setForeground(theme.textSecondary());
        appPackageTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        backgroundModeTitleLabel.setForeground(theme.textSecondary());
        backgroundModeTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        backgroundModeValueLabel.setForeground(theme.textPrimary());
        backgroundModeValueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        for (JLabel fieldLabel : fieldLabels.values()) {
            fieldLabel.setForeground(theme.textSecondary());
            fieldLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        }

        for (JLabel valueLabel : valueLabels.values()) {
            valueLabel.setForeground(theme.textPrimary());
            valueLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        }

        styleTable();
        styleScrollPane(tableScrollPane);
        styleScrollPane(detailsScrollPane, false);
        styleScrollPane(permissionsScrollPane);
        contentSplitPane.setUI(new ThemedSplitPaneUI(theme));
        contentSplitPane.setBackground(theme.background());
        contentSplitPane.setBorder(new EmptyBorder(22, 0, 0, 0));
        styleBackgroundModeSlider();

        for (JCheckBox permissionCheckBox : permissionCheckBoxes) {
            stylePermissionCheckBox(permissionCheckBox);
        }
        setApplicationActionsEnabled(currentDetails != null);

        revalidate();
        repaint();
    }

    private void buildPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(28, 28, 28, 28));

        add(titleLabel, BorderLayout.NORTH);

        contentSplitPane.setLeftComponent(buildListPanel());
        contentSplitPane.setRightComponent(buildDetailsPanel());
        contentSplitPane.setOpaque(false);
        contentSplitPane.setBorder(new EmptyBorder(22, 0, 0, 0));
        contentSplitPane.setResizeWeight(0.46d);
        contentSplitPane.setContinuousLayout(true);
        contentSplitPane.setDividerSize(10);
        contentSplitPane.setOneTouchExpandable(false);
        contentSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, event -> {
            int dividerLocation = contentSplitPane.getDividerLocation();
            if (dividerLocation > 0) {
                savedDividerLocation = dividerLocation;
            }
        });
        listPanel.setMinimumSize(new Dimension(420, 240));
        detailsPanel.setMinimumSize(new Dimension(520, 240));
        add(contentSplitPane, BorderLayout.CENTER);
        restoreSplitPaneLocation();
    }

    private JPanel buildListPanel() {
        listHeaderPanel.setLayout(new BoxLayout(listHeaderPanel, BoxLayout.Y_AXIS));
        listHeaderPanel.setBorder(new EmptyBorder(18, 18, 0, 18));

        JPanel searchRow = new JPanel(new BorderLayout(12, 0));
        searchRow.setOpaque(false);

        searchField.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        searchField.setOpaque(false);
        searchField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        searchField.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "clear-search");
        searchField.getActionMap().put("clear-search", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                clearSearchField();
            }
        });

        clearSearchButton.setUI(new BasicButtonUI());
        clearSearchButton.setFocusable(false);
        clearSearchButton.setFocusPainted(false);
        clearSearchButton.setRolloverEnabled(true);
        clearSearchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearSearchButton.getModel().addChangeListener(event -> styleSearchField());
        clearSearchButton.setPreferredSize(new Dimension(28, 28));
        clearSearchButton.addActionListener(event -> clearSearchField());

        installButton.setUI(new BasicButtonUI());
        installButton.setFocusable(false);
        installButton.setFocusPainted(false);
        installButton.setRolloverEnabled(true);
        installButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        installButton.setPreferredSize(new Dimension(40, 40));
        installButton.setText("");
        installButton.getModel().addChangeListener(event -> styleInstallButton());

        searchFieldPanel.add(searchField, BorderLayout.CENTER);
        searchFieldPanel.add(clearSearchButton, BorderLayout.EAST);

        searchRow.add(searchLabel, BorderLayout.WEST);
        searchRow.add(searchFieldPanel, BorderLayout.CENTER);
        searchRow.add(installButton, BorderLayout.EAST);

        userAppsFilter.setSelected(true);
        systemAppsFilter.setSelected(false);
        disabledAppsFilter.setSelected(false);
        filterCheckBoxes.add(userAppsFilter);
        filterCheckBoxes.add(systemAppsFilter);
        filterCheckBoxes.add(disabledAppsFilter);

        filtersPanel.setOpaque(false);
        filtersPanel.add(userAppsFilter);
        filtersPanel.add(systemAppsFilter);
        filtersPanel.add(disabledAppsFilter);

        listStatusPanel.setOpaque(false);
        listStatusPanel.setMinimumSize(new Dimension(0, 22));
        listStatusPanel.setPreferredSize(new Dimension(0, 22));
        listStatusPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        listStatusPanel.add(listStatusLabel, BorderLayout.CENTER);
        listStatusPanel.add(visibleCountLabel, BorderLayout.EAST);

        listStatusLabel.setVisible(false);
        listStatusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        listStatusLabel.setMinimumSize(new Dimension(0, 22));
        listStatusLabel.setPreferredSize(new Dimension(0, 22));
        listStatusLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        listStatusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        listHeaderPanel.add(searchRow);
        listHeaderPanel.add(Box.createVerticalStrut(14));
        listHeaderPanel.add(filtersPanel);
        listHeaderPanel.add(Box.createVerticalStrut(10));
        listHeaderPanel.add(listStatusPanel);

        appsTable.setRowSorter(rowSorter);
        appsTable.setFillsViewportHeight(true);
        appsTable.setRowHeight(38);
        appsTable.setShowHorizontalLines(true);
        appsTable.setShowVerticalLines(false);
        appsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        appsTable.setDefaultRenderer(Object.class, new AppsTableCellRenderer());
        appsTable.getSelectionModel().addListSelectionListener(createSelectionListener());
        appsTable.getTableHeader().setReorderingAllowed(false);
        tableScrollPane.getVerticalScrollBar().addAdjustmentListener(
                event -> SwingUtilities.invokeLater(visibleApplicationsChangedAction));

        tableScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 18, 18, 18));

        listPanel.add(listHeaderPanel, BorderLayout.NORTH);
        listPanel.add(tableScrollPane, BorderLayout.CENTER);
        return listPanel;
    }

    private JPanel buildDetailsPanel() {
        buildDetailsEmptyPanel();
        buildDetailsLoadingPanel();
        buildDetailsContentPanel();

        detailsPanel.add(detailsEmptyPanel, DETAILS_EMPTY_KEY);
        detailsPanel.add(detailsLoadingPanel, DETAILS_LOADING_KEY);
        detailsPanel.add(detailsScrollPane, DETAILS_CONTENT_KEY);
        return detailsPanel;
    }

    private void buildDetailsEmptyPanel() {
        detailsEmptyPanel.setLayout(new BoxLayout(detailsEmptyPanel, BoxLayout.Y_AXIS));
        detailsEmptyPanel.setBorder(new EmptyBorder(36, 36, 36, 36));
        detailsEmptyTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailsEmptySubtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        detailsEmptyPanel.add(Box.createVerticalGlue());
        detailsEmptyPanel.add(detailsEmptyTitleLabel);
        detailsEmptyPanel.add(Box.createVerticalStrut(12));
        detailsEmptyPanel.add(detailsEmptySubtitleLabel);
        detailsEmptyPanel.add(Box.createVerticalGlue());
    }

    private void buildDetailsLoadingPanel() {
        detailsLoadingPanel.setLayout(new BoxLayout(detailsLoadingPanel, BoxLayout.Y_AXIS));
        detailsLoadingPanel.setBorder(new EmptyBorder(36, 36, 36, 36));
        detailsLoadingTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailsLoadingSubtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        detailsLoadingPanel.add(Box.createVerticalGlue());
        detailsLoadingPanel.add(detailsLoadingTitleLabel);
        detailsLoadingPanel.add(Box.createVerticalStrut(12));
        detailsLoadingPanel.add(detailsLoadingSubtitleLabel);
        detailsLoadingPanel.add(Box.createVerticalGlue());
    }

    private void buildDetailsContentPanel() {
        detailsContentPanel.setBorder(new EmptyBorder(18, 18, 18, 18));

        headerAndActionsPanel.setLayout(new BoxLayout(headerAndActionsPanel, BoxLayout.Y_AXIS));
        appIconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        appIconLabel.setVerticalAlignment(SwingConstants.CENTER);
        appIconLabel.setPreferredSize(new Dimension(72, 72));
        appIconLabel.setMinimumSize(new Dimension(72, 72));

        appHeaderTextPanel.setLayout(new BoxLayout(appHeaderTextPanel, BoxLayout.Y_AXIS));
        appHeaderTextPanel.add(Box.createVerticalGlue());
        appHeaderTextPanel.add(appNameTitleLabel);
        appHeaderTextPanel.add(Box.createVerticalStrut(6));
        appHeaderTextPanel.add(appPackageTitleLabel);
        appHeaderTextPanel.add(Box.createVerticalGlue());

        appHeaderPanel.add(appIconLabel, BorderLayout.WEST);
        appHeaderPanel.add(appHeaderTextPanel, BorderLayout.CENTER);

        actionButtons.add(openButton);
        actionButtons.add(stopButton);
        actionButtons.add(uninstallButton);
        actionButtons.add(toggleEnabledButton);
        actionButtons.add(clearDataButton);
        actionButtons.add(clearCacheButton);
        actionButtons.add(exportApkButton);

        configureActionButton(openButton, ToolbarIcon.Type.OPEN, true);
        configureActionButton(stopButton, ToolbarIcon.Type.STOP, false);
        configureActionButton(uninstallButton, ToolbarIcon.Type.UNINSTALL, false);
        configureActionButton(toggleEnabledButton, ToolbarIcon.Type.DISABLE, false);
        configureActionButton(clearDataButton, ToolbarIcon.Type.CLEAR_DATA, false);
        configureActionButton(clearCacheButton, ToolbarIcon.Type.CLEAR_CACHE, false);
        configureActionButton(exportApkButton, ToolbarIcon.Type.EXPORT, true);

        for (JButton button : actionButtons) {
            actionButtonsPanel.add(button);
        }

        JPanel actionsBody = new JPanel(new BorderLayout());
        actionsBody.setOpaque(false);
        actionsBody.add(actionButtonsPanel, BorderLayout.NORTH);
        actionsCard.add(actionsBody, BorderLayout.CENTER);

        headerAndActionsPanel.add(appHeaderPanel);
        headerAndActionsPanel.add(Box.createVerticalStrut(18));
        headerAndActionsPanel.add(actionsCard);
        headerAndActionsPanel.add(Box.createVerticalStrut(18));
        headerAndActionsPanel.add(buildBackgroundModeCard());

        infoAndPermissionsPanel.setLayout(new BoxLayout(infoAndPermissionsPanel, BoxLayout.Y_AXIS));
        infoAndPermissionsPanel.add(buildInfoPanel());
        infoAndPermissionsPanel.add(Box.createVerticalStrut(18));
        infoAndPermissionsPanel.add(buildPermissionsCard());
        infoAndPermissionsPanel.add(Box.createVerticalGlue());

        detailsContentPanel.add(headerAndActionsPanel, BorderLayout.NORTH);
        detailsContentPanel.add(infoAndPermissionsPanel, BorderLayout.CENTER);
    }

    private JPanel buildInfoPanel() {
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.add(createInfoRow(FIELD_NAME));
        infoPanel.add(createInfoRow(FIELD_PACKAGE));
        infoPanel.add(createInfoRow(FIELD_STATE));
        infoPanel.add(createInfoRow(FIELD_TYPE));
        infoPanel.add(createInfoRow(FIELD_VERSION));
        infoPanel.add(createInfoRow(FIELD_TARGET_SDK));
        infoPanel.add(createInfoRow(FIELD_MIN_SDK));
        infoPanel.add(createInfoRow(FIELD_INSTALLER));
        infoPanel.add(createInfoRow(FIELD_STORAGE));
        infoPanel.add(createInfoRow(FIELD_CODE));
        infoPanel.add(createInfoRow(FIELD_DATA));
        infoPanel.add(createInfoRow(FIELD_CACHE));
        return infoPanel;
    }

    private JPanel buildBackgroundModeCard() {
        backgroundModeHeaderPanel.add(backgroundModeTitleLabel, BorderLayout.WEST);
        backgroundModeHeaderPanel.add(backgroundModeValueLabel, BorderLayout.EAST);

        backgroundModeSlider.setOpaque(false);
        backgroundModeSlider.setMinimum(0);
        backgroundModeSlider.setMaximum(2);
        backgroundModeSlider.setValue(AppBackgroundMode.OPTIMIZED.sliderValue());
        backgroundModeSlider.setSnapToTicks(true);
        backgroundModeSlider.setPaintTicks(true);
        backgroundModeSlider.setMajorTickSpacing(1);
        backgroundModeSlider.setMinorTickSpacing(1);
        backgroundModeSlider.setFocusable(false);
        backgroundModeSlider.addChangeListener(event -> {
            if (syncingBackgroundMode || currentDetails == null || backgroundModeSlider.getValueIsAdjusting()) {
                return;
            }
            AppBackgroundMode selectedMode = AppBackgroundMode.fromSliderValue(backgroundModeSlider.getValue());
            if (selectedMode != currentDetails.backgroundMode()) {
                backgroundModeChangeHandler.onChange(currentDetails.app().packageName(), selectedMode);
            }
        });

        backgroundUnrestrictedLabel.setHorizontalAlignment(SwingConstants.LEFT);
        backgroundOptimizedLabel.setHorizontalAlignment(SwingConstants.CENTER);
        backgroundRestrictedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        backgroundModeLegendPanel.add(backgroundUnrestrictedLabel);
        backgroundModeLegendPanel.add(backgroundOptimizedLabel);
        backgroundModeLegendPanel.add(backgroundRestrictedLabel);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(backgroundModeSlider, BorderLayout.CENTER);
        centerPanel.add(backgroundModeLegendPanel, BorderLayout.SOUTH);

        backgroundModeCard.add(backgroundModeHeaderPanel, BorderLayout.NORTH);
        backgroundModeCard.add(centerPanel, BorderLayout.CENTER);
        return backgroundModeCard;
    }

    private JPanel buildPermissionsCard() {
        permissionsPanel.setLayout(new BoxLayout(permissionsPanel, BoxLayout.Y_AXIS));
        permissionsScrollPane.setPreferredSize(new Dimension(0, 280));
        permissionsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        permissionsCard.add(permissionsScrollPane, BorderLayout.CENTER);
        return permissionsCard;
    }

    private JPanel createInfoRow(String fieldKey) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        JLabel fieldLabel = new JLabel();
        JLabel valueLabel = new JLabel("-");
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        configureCopyableValueLabel(valueLabel);

        fieldLabels.put(fieldKey, fieldLabel);
        valueLabels.put(fieldKey, valueLabel);

        row.add(fieldLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private void bindFilters() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applyFiltersInternal(true);
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applyFiltersInternal(true);
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applyFiltersInternal(true);
            }
        };

        searchField.getDocument().addDocumentListener(listener);
        userAppsFilter.addActionListener(event -> applyFiltersInternal(true));
        systemAppsFilter.addActionListener(event -> applyFiltersInternal(true));
        disabledAppsFilter.addActionListener(event -> applyFiltersInternal(true));
    }

    private ListSelectionListener createSelectionListener() {
        return event -> {
            if (event.getValueIsAdjusting() || syncingSelection) {
                return;
            }
            selectionAction.run();
        };
    }

    private void rebuildPermissions(List<AppPermission> permissions) {
        syncingPermissions = true;
        try {
            permissionsPanel.removeAll();
            permissionCheckBoxes.clear();

            if (permissions.isEmpty()) {
                JLabel emptyLabel = new JLabel(Messages.text("apps.permissions.empty"));
                emptyLabel.setForeground(theme.textSecondary());
                emptyLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                permissionsPanel.add(emptyLabel);
            } else {
                for (AppPermission permission : permissions) {
                    JCheckBox permissionCheckBox = createPermissionCheckBox(permission);
                    permissionCheckBoxes.add(permissionCheckBox);
                    permissionsPanel.add(permissionCheckBox);
                    permissionsPanel.add(Box.createVerticalStrut(6));
                }
            }
        } finally {
            syncingPermissions = false;
        }

        permissionsPanel.revalidate();
        permissionsPanel.repaint();
    }

    private JCheckBox createPermissionCheckBox(AppPermission permission) {
        JCheckBox checkBox = new JCheckBox(permission.name());
        checkBox.setOpaque(true);
        checkBox.setSelected(permission.granted());
        checkBox.putClientProperty("permission", permission.name());
        checkBox.putClientProperty("changeable", permission.changeable());
        String tooltip = permission.name();
        if (permission.appOpControlled()) {
            tooltip += " [" + permission.appOp() + "]";
        } else if (!permission.flags().isBlank()) {
            tooltip += " [" + permission.flags() + "]";
        }
        checkBox.setToolTipText(tooltip);
        stylePermissionCheckBox(checkBox);
        checkBox.addItemListener(event -> {
            if (syncingPermissions || currentDetails == null) {
                return;
            }
            permissionToggleHandler.onToggle(currentDetails.app().packageName(), permission.name(), checkBox.isSelected());
        });
        return checkBox;
    }

    private void applyFiltersInternal(boolean notifySelection) {
        String previousPackage = getSelectedPackageName();
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

        rowSorter.setRowFilter(new RowFilter<InstalledAppsTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends InstalledAppsTableModel, ? extends Integer> entry) {
                InstalledApp application = tableModel.getApplicationAt(entry.getIdentifier());
                if (application == null) {
                    return false;
                }

                if (!userAppsFilter.isSelected() && application.userApp()) {
                    return false;
                }
                if (!systemAppsFilter.isSelected() && application.systemApp()) {
                    return false;
                }
                if (!disabledAppsFilter.isSelected() && application.disabled()) {
                    return false;
                }
                if (query.isBlank()) {
                    return true;
                }

                return application.displayName().toLowerCase(Locale.ROOT).contains(query)
                        || application.packageName().toLowerCase(Locale.ROOT).contains(query);
            }
        });

        clearSearchButton.setVisible(!query.isBlank());
        clearSearchButton.setEnabled(searchField.isEnabled() && !query.isBlank());
        String selectedPackage = selectPackage(previousPackage);
        updateVisibleCountLabel();

        if (notifySelection && !syncingSelection && !Objects.equals(previousPackage, selectedPackage)) {
            SwingUtilities.invokeLater(selectionAction);
        }
        SwingUtilities.invokeLater(visibleApplicationsChangedAction);
    }

    private void clearSearchField() {
        if (!searchField.getText().isEmpty()) {
            searchField.setText("");
        }
    }

    private String selectPackage(String packageName) {
        if (appsTable.getRowCount() <= 0) {
            appsTable.clearSelection();
            return null;
        }

        if (packageName != null && !packageName.isBlank()) {
            for (int row = 0; row < appsTable.getRowCount(); row++) {
                int modelRow = appsTable.convertRowIndexToModel(row);
                InstalledApp application = tableModel.getApplicationAt(modelRow);
                if (application != null && application.packageName().equals(packageName)) {
                    appsTable.setRowSelectionInterval(row, row);
                    appsTable.scrollRectToVisible(appsTable.getCellRect(row, 0, true));
                    return application.packageName();
                }
            }
        }

        appsTable.setRowSelectionInterval(0, 0);
        return getSelectedPackageName();
    }

    private void styleSearchField() {
        searchFieldPanel.setBackground(theme.secondarySurface());
        searchFieldPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 6)));
        searchField.setForeground(theme.textPrimary());
        searchField.setCaretColor(theme.textPrimary());
        clearSearchButton.setOpaque(true);
        clearSearchButton.setContentAreaFilled(true);
        clearSearchButton.setBackground(clearSearchButton.getModel().isRollover()
                ? ThemeUtils.blend(theme.secondarySurface(), theme.selectionBackground(), 0.2d)
                : theme.secondarySurface());
        clearSearchButton.setForeground(theme.textSecondary());
        clearSearchButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    }

    private void styleInstallButton() {
        boolean hovered = installButton.getModel().isRollover() && installButton.isEnabled();
        installButton.setOpaque(true);
        installButton.setContentAreaFilled(true);
        installButton.setBackground(hovered
                ? ThemeUtils.blend(theme.secondarySurface(), theme.selectionBackground(), 0.24d)
                : theme.secondarySurface());
        installButton.setForeground(installButton.isEnabled() ? theme.actionBackground() : theme.textSecondary());
        installButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(installButton.isEnabled() ? theme.border() : theme.disabledBorder(), 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        installButton.setIcon(new ToolbarIcon(ToolbarIcon.Type.ADD, 16, installButton.getForeground()));
    }

    private void styleStatusLabel() {
        listStatusLabel.setForeground(Boolean.TRUE.equals(listStatusLabel.getClientProperty("loading"))
                ? theme.actionBackground()
                : theme.textSecondary());
        listStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
    }

    private void updateVisibleCountLabel() {
        visibleCountLabel.setText(Messages.format("apps.count.visible", appsTable.getRowCount()));
    }

    private void styleFilterCheckBox(JCheckBox checkBox) {
        checkBox.setOpaque(true);
        checkBox.setBackground(theme.background());
        checkBox.setForeground(theme.textPrimary());
        checkBox.setFocusPainted(false);
        checkBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    }

    private void stylePermissionCheckBox(JCheckBox checkBox) {
        checkBox.setBackground(theme.background());
        checkBox.setForeground(checkBox.isEnabled() ? theme.textPrimary() : theme.textSecondary());
        checkBox.setFocusPainted(false);
        checkBox.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        checkBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    }

    private void styleTable() {
        appsTable.setBackground(theme.background());
        appsTable.setForeground(theme.textPrimary());
        appsTable.setSelectionBackground(theme.selectionBackground());
        appsTable.setSelectionForeground(theme.selectionForeground());
        appsTable.setGridColor(theme.border());
        appsTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        JTableHeader tableHeader = appsTable.getTableHeader();
        tableHeader.setOpaque(true);
        tableHeader.setBackground(theme.secondarySurface());
        tableHeader.setForeground(theme.textSecondary());
        tableHeader.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        tableHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, theme.border()));
        tableHeader.setDefaultRenderer(new AppsTableHeaderRenderer());
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        styleScrollPane(scrollPane, true);
    }

    private void styleScrollPane(JScrollPane scrollPane, boolean bordered) {
        scrollPane.setBorder(bordered ? BorderFactory.createLineBorder(theme.border(), 1) : BorderFactory.createEmptyBorder());
        JViewport viewport = scrollPane.getViewport();
        viewport.setBackground(theme.background());
        scrollPane.getVerticalScrollBar().setUI(new ThemedScrollBarUI(theme));
        scrollPane.getHorizontalScrollBar().setUI(new ThemedScrollBarUI(theme));
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        scrollPane.getVerticalScrollBar().setBlockIncrement(96);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        if (scrollPane.getColumnHeader() != null) {
            scrollPane.getColumnHeader().setBackground(theme.secondarySurface());
        }
    }

    private void styleBackgroundModeSlider() {
        backgroundModeSlider.setBackground(theme.background());
        backgroundModeSlider.setForeground(backgroundModeSlider.isEnabled() ? theme.actionBackground() : theme.textSecondary());
        updateBackgroundModeLegendColors(currentDetails == null ? null : currentDetails.backgroundMode());
    }

    private void configureActionButton(JButton button, ToolbarIcon.Type iconType, boolean primary) {
        button.putClientProperty("iconType", iconType);
        button.putClientProperty("primary", primary);
        button.setUI(new BasicButtonUI());
        button.setFocusable(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(8);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.getModel().addChangeListener(event -> styleActionButton(button, button.isEnabled()));
        button.setPreferredSize(new Dimension(0, 36));
        button.setMargin(new Insets(0, 0, 0, 0));
    }

    private void updateActionButtonIcons() {
        openButton.setIcon(createActionIcon(ToolbarIcon.Type.OPEN, openButton.isEnabled(), true));
        stopButton.setIcon(createActionIcon(ToolbarIcon.Type.STOP, stopButton.isEnabled(), false));
        uninstallButton.setIcon(createActionIcon(ToolbarIcon.Type.UNINSTALL, uninstallButton.isEnabled(), false));
        toggleEnabledButton.setIcon(createActionIcon(
                currentDetails != null && currentDetails.app().disabled() ? ToolbarIcon.Type.ENABLE : ToolbarIcon.Type.DISABLE,
                toggleEnabledButton.isEnabled(),
                false));
        clearDataButton.setIcon(createActionIcon(ToolbarIcon.Type.CLEAR_DATA, clearDataButton.isEnabled(), false));
        clearCacheButton.setIcon(createActionIcon(ToolbarIcon.Type.CLEAR_CACHE, clearCacheButton.isEnabled(), false));
        exportApkButton.setIcon(createActionIcon(ToolbarIcon.Type.EXPORT, exportApkButton.isEnabled(), true));
    }

    private Icon createActionIcon(ToolbarIcon.Type iconType, boolean enabled, boolean primary) {
        Color iconColor = enabled
                ? (primary ? theme.actionForeground() : theme.textPrimary())
                : theme.textSecondary();
        return new ToolbarIcon(iconType, 16, iconColor);
    }

    private void styleActionButton(JButton button, boolean enabled) {
        boolean primary = Boolean.TRUE.equals(button.getClientProperty("primary"));
        boolean hovered = enabled && button.getModel().isRollover();
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        if (enabled) {
            Color background = primary
                    ? theme.actionBackground()
                    : ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.84d);
            if (hovered) {
                background = ThemeUtils.blend(background, theme.selectionBackground(), primary ? 0.16d : 0.24d);
            }
            button.setBackground(background);
            button.setForeground(primary ? theme.actionForeground() : theme.textPrimary());
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primary ? background : theme.border(), 1),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        } else {
            button.setBackground(theme.secondarySurface());
            button.setForeground(theme.textSecondary());
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(theme.disabledBorder(), 1),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        }

        updateActionButtonIcons();
    }

    private void updateToggleButtonText(boolean disabled) {
        toggleEnabledButton.setText(disabled
                ? Messages.text("apps.action.enable")
                : Messages.text("apps.action.disable"));
        updateActionButtonIcons();
    }

    private void updateBackgroundModePresentation(AppBackgroundMode mode) {
        AppBackgroundMode safeMode = mode == null ? AppBackgroundMode.OPTIMIZED : mode;
        syncingBackgroundMode = true;
        try {
            backgroundModeSlider.setValue(safeMode.sliderValue());
        } finally {
            syncingBackgroundMode = false;
        }

        backgroundModeValueLabel.setText(mode == null ? "-" : Messages.text(safeMode.messageKey()));
        updateBackgroundModeLegendColors(mode);
    }

    private void updateBackgroundModeLegendColors(AppBackgroundMode mode) {
        AppBackgroundMode selectedMode = mode == null ? null : mode;
        styleBackgroundModeLegendLabel(backgroundUnrestrictedLabel, selectedMode == AppBackgroundMode.UNRESTRICTED);
        styleBackgroundModeLegendLabel(backgroundOptimizedLabel, selectedMode == AppBackgroundMode.OPTIMIZED);
        styleBackgroundModeLegendLabel(backgroundRestrictedLabel, selectedMode == AppBackgroundMode.RESTRICTED);
    }

    private void styleBackgroundModeLegendLabel(JLabel label, boolean selected) {
        label.setForeground(selected ? theme.actionBackground() : theme.textSecondary());
        label.setFont(new Font(Font.SANS_SERIF, selected ? Font.BOLD : Font.PLAIN, 13));
    }

    private void restoreSplitPaneLocation() {
        SwingUtilities.invokeLater(() -> {
            if (savedDividerLocation > 0) {
                contentSplitPane.setDividerLocation(savedDividerLocation);
            } else {
                contentSplitPane.setDividerLocation(0.46d);
            }
        });
    }

    private Icon createApplicationIcon(AppDetails details) {
        if (details.iconImage() != null) {
            Image scaled = details.iconImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }
        return createFallbackIcon(details.displayName(), details.app().packageName());
    }

    private Icon createListApplicationIcon(InstalledApp application) {
        if (application == null) {
            return null;
        }

        if (application.iconImage() != null) {
            Image scaled = application.iconImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }

        Icon fallbackIcon = createFallbackIcon(application.displayName(), application.packageName());
        if (fallbackIcon instanceof ImageIcon imageIcon) {
            Image scaled = imageIcon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }
        return fallbackIcon;
    }

    private Icon createFallbackIcon(String displayName, String packageName) {
        int size = 64;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(fallbackIconColor(packageName));
            g2d.fillRoundRect(0, 0, size, size, 18, 18);

            String monogram = buildMonogram(displayName, packageName);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            java.awt.FontMetrics metrics = g2d.getFontMetrics();
            int textX = (size - metrics.stringWidth(monogram)) / 2;
            int textY = (size - metrics.getHeight()) / 2 + metrics.getAscent();
            g2d.drawString(monogram, textX, textY);
        } finally {
            g2d.dispose();
        }
        return new ImageIcon(image);
    }

    private void configureCopyableValueLabel(JLabel valueLabel) {
        valueLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        valueLabel.setToolTipText(Messages.text("apps.copy.tooltip"));
        valueLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                copyToClipboard(valueLabel.getText());
            }
        });
    }

    private void setValueText(String fieldKey, String value) {
        JLabel valueLabel = valueLabels.get(fieldKey);
        if (valueLabel == null) {
            return;
        }

        String normalizedValue = value == null || value.isBlank() ? "-" : value.trim();
        valueLabel.setText(normalizedValue);
        valueLabel.setToolTipText(Messages.text("apps.copy.tooltip") + ": " + normalizedValue);
    }

    private void copyToClipboard(String value) {
        String normalizedValue = value == null ? "" : value.trim();
        if (normalizedValue.isBlank() || "-".equals(normalizedValue)) {
            return;
        }

        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(normalizedValue), null);
    }

    private Color fallbackIconColor(String packageName) {
        int hash = Math.abs(Objects.requireNonNullElse(packageName, "").hashCode());
        int red = 70 + (hash % 90);
        int green = 90 + ((hash / 31) % 90);
        int blue = 140 + ((hash / 127) % 90);
        return new Color(Math.min(red, 200), Math.min(green, 210), Math.min(blue, 235));
    }

    private String buildMonogram(String displayName, String packageName) {
        String source = displayName == null || displayName.isBlank() ? packageName : displayName;
        if (source == null || source.isBlank()) {
            return "A";
        }

        String[] words = source.trim().split("[\\s._-]+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (!word.isBlank() && Character.isLetterOrDigit(word.charAt(0))) {
                builder.append(Character.toUpperCase(word.charAt(0)));
            }
            if (builder.length() == 2) {
                break;
            }
        }
        return builder.isEmpty() ? "A" : builder.toString();
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

    private class AppsTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setBackground(isSelected ? theme.selectionBackground() : theme.background());
            setForeground(isSelected ? theme.selectionForeground() : theme.textPrimary());
            setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            if (column == 0) {
                InstalledApp application = tableModel.getApplicationAt(table.convertRowIndexToModel(row));
                setIcon(createListApplicationIcon(application));
                setIconTextGap(10);
            } else {
                setIcon(null);
            }
            return this;
        }
    }

    private final class AppsTableHeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            setBackground(theme.secondarySurface());
            setForeground(theme.textSecondary());
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, theme.border()),
                    BorderFactory.createEmptyBorder(0, 10, 0, 10)));
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            return this;
        }
    }

    private final class StorageCellRenderer extends AppsTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (component instanceof JLabel label) {
                label.setHorizontalAlignment(SwingConstants.RIGHT);
            }
            return component;
        }
    }
}
