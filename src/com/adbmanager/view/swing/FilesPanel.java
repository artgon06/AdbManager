package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.AbstractTableModel;

import com.adbmanager.logic.model.DeviceDirectoryListing;
import com.adbmanager.logic.model.DeviceFileEntry;
import com.adbmanager.view.Messages;

public class FilesPanel extends JPanel {

    public interface FileDropHandler {
        void onDrop(List<File> files);
    }

    public interface DragExportHandler {
        File prepareTempDirectory() throws Exception;
        void cleanupTempDirectory(File tempDir);
    }

    private final JPanel contentPanel = new JPanel(new BorderLayout(0, 12));
    private final JPanel toolbarPanel = new JPanel(new BorderLayout(12, 0));
    private final JPanel actionsPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
    private final JPanel pathPanel = new JPanel(new BorderLayout(10, 0));
    private final JLabel pathLabel = new JLabel();
    private final JTextField pathField = new JTextField();
    private final JButton upButton = new JButton();
    private final JButton refreshButton = new JButton();
    private final JButton createFolderButton = new JButton();
    private final JButton uploadButton = new JButton();
    private final JButton downloadButton = new JButton();
    private final JButton renameButton = new JButton();
    private final JButton copyButton = new JButton();
    private final JButton deleteButton = new JButton();
    private final JButton cancelTransferButton = new JButton("\u00d7");
    private final JLabel statusLabel = new JLabel(" ");
    private final JPanel footerPanel = new JPanel();
    private final JPanel statusRowPanel = new JPanel();
    private final Component cancelTransferSpacer = Box.createHorizontalStrut(8);
    private final JProgressBar transferProgressBar = new JProgressBar(0, 100);

    private final DeviceFilesTableModel tableModel = new DeviceFilesTableModel();
    private final JTable filesTable = new JTable(tableModel);
    private final JScrollPane tableScrollPane = new JScrollPane(filesTable);
    private final JPopupMenu contextMenu = new JPopupMenu();
    private final JMenuItem openFolderMenuItem = new JMenuItem();
    private final JMenuItem downloadMenuItem = new JMenuItem();
    private final JMenuItem renameMenuItem = new JMenuItem();
    private final JMenuItem copyMenuItem = new JMenuItem();
    private final JMenuItem deleteMenuItem = new JMenuItem();

    private final List<JButton> actionButtons = new ArrayList<>();
    private AppTheme theme = AppTheme.LIGHT;
    private DeviceDirectoryListing currentListing = new DeviceDirectoryListing(null, null, List.of());
    private boolean deviceAvailable;
    private boolean busy;
    private boolean transferCancelable;
    private Runnable selectionAction = () -> {
    };
    private Runnable openDirectoryAction = () -> {
    };
    private FileDropHandler fileDropHandler = files -> {
    };
    private DragExportHandler dragExportHandler = null;

    public FilesPanel() {
        buildPanel();
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
        clearDirectory();
    }

    public void setNavigateUpAction(ActionListener actionListener) {
        upButton.addActionListener(actionListener);
    }

    public void setRefreshAction(ActionListener actionListener) {
        refreshButton.addActionListener(actionListener);
    }

    public void setPathSubmitAction(ActionListener actionListener) {
        pathField.addActionListener(actionListener);
    }

    public void setCreateFolderAction(ActionListener actionListener) {
        createFolderButton.addActionListener(actionListener);
    }

    public void setUploadAction(ActionListener actionListener) {
        uploadButton.addActionListener(actionListener);
    }

    public void setDownloadAction(ActionListener actionListener) {
        downloadButton.addActionListener(actionListener);
        downloadMenuItem.addActionListener(event -> downloadButton.doClick());
    }

    public void setRenameAction(ActionListener actionListener) {
        renameButton.addActionListener(actionListener);
        renameMenuItem.addActionListener(event -> renameButton.doClick());
    }

    public void setCopyAction(ActionListener actionListener) {
        copyButton.addActionListener(actionListener);
        copyMenuItem.addActionListener(event -> copyButton.doClick());
    }

    public void setDeleteAction(ActionListener actionListener) {
        deleteButton.addActionListener(actionListener);
        deleteMenuItem.addActionListener(event -> deleteButton.doClick());
    }

    public void setSelectionAction(Runnable action) {
        selectionAction = action == null ? () -> {
        } : action;
    }

    public void setOpenDirectoryAction(Runnable action) {
        openDirectoryAction = action == null ? () -> {
        } : action;
    }

    public void setFileDropHandler(FileDropHandler handler) {
        fileDropHandler = handler == null ? files -> {
        } : handler;
    }

    public void setDragExportHandler(DragExportHandler handler) {
        dragExportHandler = handler;
    }

    public void setDeviceAvailable(boolean deviceAvailable) {
        this.deviceAvailable = deviceAvailable;
        updateControlStates();
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        updateControlStates();
    }

    public void setStatus(String message, boolean error) {
        String normalized = message == null ? "" : message.trim();
        statusLabel.putClientProperty("error", error);
        statusLabel.setText(normalized.isBlank() ? " " : normalized);
        styleStatusLabel();
    }

    public void setTransferCancelAction(ActionListener actionListener) {
        cancelTransferButton.addActionListener(actionListener);
    }

    public void setTransferCancelable(boolean cancelable) {
        transferCancelable = cancelable;
        cancelTransferButton.setVisible(cancelable);
        cancelTransferSpacer.setVisible(cancelable);
        cancelTransferButton.setEnabled(cancelable);
        styleTransferCancelButton();
    }

    public void setTransferProgress(boolean visible, boolean indeterminate, int percent, String progressText) {
        transferProgressBar.setVisible(visible);
        transferProgressBar.setIndeterminate(visible && indeterminate);
        transferProgressBar.setValue(Math.max(0, Math.min(100, percent)));
        transferProgressBar.setString((progressText == null || progressText.isBlank()) ? " " : progressText.trim());
        transferProgressBar.setStringPainted(visible);
    }

    public void clearTransferProgress() {
        setTransferProgress(false, false, 0, " ");
    }

    public void setDirectoryListing(DeviceDirectoryListing listing) {
        currentListing = listing == null ? new DeviceDirectoryListing(null, null, List.of()) : listing;
        pathField.setText(currentListing.currentPath() == null ? "" : currentListing.currentPath());
        pathField.setCaretPosition(0);
        tableModel.setEntries(currentListing.entries());
        if (tableModel.getRowCount() > 0) {
            filesTable.setRowSelectionInterval(0, 0);
        }
        updateControlStates();
    }

    public void clearDirectory() {
        setDirectoryListing(new DeviceDirectoryListing(null, null, List.of()));
    }

    public String getCurrentDirectoryPath() {
        return currentListing.currentPath();
    }

    public String getEnteredDirectoryPath() {
        return pathField.getText() == null ? "" : pathField.getText().trim();
    }

    public String getParentDirectoryPath() {
        return currentListing.parentPath();
    }

    public DeviceFileEntry getSelectedEntry() {
        int viewRow = filesTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        return tableModel.getEntryAt(viewRow);
    }

    public List<DeviceFileEntry> getSelectedEntries() {
        int[] selectedRows = filesTable.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0) {
            return List.of();
        }

        List<DeviceFileEntry> entries = new ArrayList<>();
        for (int viewRow : selectedRows) {
            DeviceFileEntry entry = tableModel.getEntryAt(viewRow);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return List.copyOf(entries);
    }

    public List<String> getSelectedRemotePaths() {
        return getSelectedEntries().stream()
                .map(DeviceFileEntry::path)
                .toList();
    }

    public void refreshTexts() {
        pathLabel.setText(Messages.text("files.path.label"));
        upButton.setText(Messages.text("files.action.up"));
        refreshButton.setText(Messages.text("files.action.refresh"));
        createFolderButton.setText(Messages.text("files.action.createFolder"));
        uploadButton.setText(Messages.text("files.action.upload"));
        downloadButton.setText(Messages.text("files.action.download"));
        renameButton.setText(Messages.text("files.action.rename"));
        copyButton.setText(Messages.text("files.action.copy"));
        deleteButton.setText(Messages.text("files.action.delete"));

        upButton.setToolTipText(Messages.text("files.action.up"));
        refreshButton.setToolTipText(Messages.text("files.action.refresh"));
        createFolderButton.setToolTipText(Messages.text("files.action.createFolder"));
        uploadButton.setToolTipText(Messages.text("files.action.upload"));
        downloadButton.setToolTipText(Messages.text("files.action.download"));
        renameButton.setToolTipText(Messages.text("files.action.rename"));
        copyButton.setToolTipText(Messages.text("files.action.copy"));
        deleteButton.setToolTipText(Messages.text("files.action.delete"));

        openFolderMenuItem.setText(Messages.text("files.context.open"));
        downloadMenuItem.setText(Messages.text("files.action.download"));
        renameMenuItem.setText(Messages.text("files.action.rename"));
        copyMenuItem.setText(Messages.text("files.action.copy"));
        deleteMenuItem.setText(Messages.text("files.action.delete"));

        tableModel.refreshTexts();
        contentPanel.setBorder(createSectionBorder(Messages.text("files.title")));
        styleTransferCancelButton();
    }

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        setBackground(theme.background());
        contentPanel.setBackground(theme.background());
        toolbarPanel.setBackground(theme.background());
        actionsPanel.setBackground(theme.background());
        pathPanel.setBackground(theme.background());

        styleSection(contentPanel, Messages.text("files.title"));
        styleButton(upButton, false, ToolbarIcon.Type.ARROW_UP);
        styleButton(refreshButton, false, ToolbarIcon.Type.REFRESH);
        styleButton(createFolderButton, false, ToolbarIcon.Type.NEW_FOLDER);
        styleButton(uploadButton, true, ToolbarIcon.Type.UPLOAD);
        styleButton(downloadButton, false, ToolbarIcon.Type.DOWNLOAD);
        styleButton(renameButton, false, ToolbarIcon.Type.EDIT);
        styleButton(copyButton, false, ToolbarIcon.Type.COPY);
        styleButton(deleteButton, false, ToolbarIcon.Type.UNINSTALL);

        pathLabel.setForeground(theme.textSecondary());
        pathLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        pathField.setForeground(theme.textPrimary());
        pathField.setCaretColor(theme.textPrimary());
        pathField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        pathField.setEditable(true);
        pathField.setBackground(theme.secondarySurface());
        pathField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(9, 10, 9, 10)));
        transferProgressBar.setForeground(theme.actionBackground());
        transferProgressBar.setBackground(theme.secondarySurface());
        transferProgressBar.setBorder(BorderFactory.createLineBorder(theme.border(), 1));
        transferProgressBar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        statusRowPanel.setOpaque(false);
        styleTransferCancelButton();

        styleTable();
        styleScrollPane(tableScrollPane);
        styleContextMenu();
        styleStatusLabel();
        repaint();
    }

    private void buildPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(18, 18, 18, 18));

        contentPanel.setBorder(createSectionBorder(Messages.text("files.title")));
        contentPanel.setOpaque(true);
        footerPanel.setOpaque(false);
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        statusRowPanel.setOpaque(false);
        statusRowPanel.setLayout(new BoxLayout(statusRowPanel, BoxLayout.X_AXIS));

        buildToolbar();
        buildTable();
        buildContextMenu();

        contentPanel.add(toolbarPanel, BorderLayout.NORTH);
        contentPanel.add(tableScrollPane, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        statusLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        cancelTransferButton.setVisible(false);
        cancelTransferButton.setBorder(new EmptyBorder(10, 0, 0, 0));
        cancelTransferButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        cancelTransferButton.getModel().addChangeListener(event -> styleTransferCancelButton());
        statusRowPanel.add(cancelTransferButton);
        cancelTransferSpacer.setVisible(false);
        statusRowPanel.add(cancelTransferSpacer);
        statusRowPanel.add(statusLabel);
        transferProgressBar.setVisible(false);
        transferProgressBar.setPreferredSize(new Dimension(0, 18));
        footerPanel.add(statusRowPanel);
        footerPanel.add(Box.createVerticalStrut(8));
        footerPanel.add(transferProgressBar);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void buildToolbar() {
        actionButtons.add(upButton);
        actionButtons.add(refreshButton);
        actionButtons.add(createFolderButton);
        actionButtons.add(uploadButton);
        actionButtons.add(downloadButton);
        actionButtons.add(renameButton);
        actionButtons.add(copyButton);
        actionButtons.add(deleteButton);

        for (JButton button : actionButtons) {
            if (button == createFolderButton) {
                button.putClientProperty("iconSize", 19);
            }
            configureButton(button);
            actionsPanel.add(button);
        }

        pathPanel.add(pathLabel, BorderLayout.WEST);
        pathPanel.add(pathField, BorderLayout.CENTER);

        JPanel toolbarContent = new JPanel();
        toolbarContent.setOpaque(false);
        toolbarContent.setLayout(new BoxLayout(toolbarContent, BoxLayout.Y_AXIS));
        actionsPanel.setAlignmentX(LEFT_ALIGNMENT);
        pathPanel.setAlignmentX(LEFT_ALIGNMENT);
        toolbarContent.add(actionsPanel);
        toolbarContent.add(Box.createVerticalStrut(10));
        toolbarContent.add(pathPanel);
        toolbarPanel.add(toolbarContent, BorderLayout.CENTER);
    }

    private void buildTable() {
        filesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        filesTable.setRowHeight(34);
        filesTable.setFillsViewportHeight(true);
        filesTable.setShowVerticalLines(false);
        filesTable.setShowHorizontalLines(true);
        filesTable.setAutoCreateRowSorter(false);
        filesTable.setDefaultRenderer(Object.class, new FilesTableCellRenderer());
        filesTable.getSelectionModel().addListSelectionListener(createSelectionListener());
        filesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && !event.isConsumed()) {
                    DeviceFileEntry selectedEntry = getSelectedEntry();
                    if (selectedEntry != null && selectedEntry.directory()) {
                        openDirectoryAction.run();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent event) {
                maybeShowContextMenu(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                maybeShowContextMenu(event);
            }
        });

        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        TransferHandler transferHandler = createTransferHandler();
        filesTable.setTransferHandler(transferHandler);
        filesTable.setDragEnabled(true);
        tableScrollPane.setTransferHandler(transferHandler);
    }

    private void buildContextMenu() {
        openFolderMenuItem.addActionListener(event -> {
            DeviceFileEntry selectedEntry = getSelectedEntry();
            if (selectedEntry != null && selectedEntry.directory()) {
                openDirectoryAction.run();
            }
        });

        contextMenu.add(openFolderMenuItem);
        contextMenu.add(downloadMenuItem);
        contextMenu.add(renameMenuItem);
        contextMenu.add(copyMenuItem);
        contextMenu.add(deleteMenuItem);
    }

    private void maybeShowContextMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }

        Point point = event.getPoint();
        int row = filesTable.rowAtPoint(point);
        if (row >= 0 && !filesTable.isRowSelected(row)) {
            filesTable.setRowSelectionInterval(row, row);
        }

        DeviceFileEntry selectedEntry = getSelectedEntry();
        openFolderMenuItem.setEnabled(selectedEntry != null && selectedEntry.directory() && !busy);
        downloadMenuItem.setEnabled(selectedEntry != null && !busy);
        renameMenuItem.setEnabled(selectedEntry != null && filesTable.getSelectedRowCount() == 1 && !busy);
        copyMenuItem.setEnabled(selectedEntry != null && filesTable.getSelectedRowCount() == 1 && !busy);
        deleteMenuItem.setEnabled(selectedEntry != null && !busy);
        contextMenu.show(filesTable, event.getX(), event.getY());
    }

    private ListSelectionListener createSelectionListener() {
        return event -> {
            if (!event.getValueIsAdjusting()) {
                selectionAction.run();
                updateControlStates();
            }
        };
    }

    private TransferHandler createTransferHandler() {
        return new TransferHandler("files") {
            private File tempDirectory;
            private volatile List<File> exportedFiles;

            @Override
            public int getSourceActions(JComponent c) {
                // Allow drag only when exactly one item (file or directory) is selected
                if (!deviceAvailable || busy || getSelectedEntries().size() != 1) {
                    return NONE;
                }
                return COPY;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                if (dragExportHandler == null || getSelectedEntries().isEmpty()) {
                    return null;
                }

                try {
                    setStatus(Messages.text("files.status.preparingExport"), false);
                    tempDirectory = dragExportHandler.prepareTempDirectory();
                    File[] files = tempDirectory.listFiles();
                    if (files == null || files.length == 0) {
                        cleanupTempDirectory();
                        setStatus(Messages.text("error.files.download"), true);
                        return null;
                    }
                    exportedFiles = List.of(files);
                    setStatus(Messages.text("files.status.ready"), false);
                    return new FileListTransferable(exportedFiles);
                } catch (Exception e) {
                    cleanupTempDirectory();
                    setStatus(Messages.text("error.files.export"), true);
                    return null;
                }
            }

            @Override
            public boolean canImport(TransferSupport support) {
                return deviceAvailable
                        && !busy
                        && support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                // Ignore drops que provienen de la transferencia interna (misma ventana)
                // y drops que apunten a archivos temporales creados para exportar (temp dir).
                try {
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (files != null) {
                        // Si alguno de los ficheros está dentro del directorio temporal de exportación,
                        // tratamos el drop como interno y lo ignoramos.
                        if (tempDirectory != null) {
                            for (File f : files) {
                                if (f != null && f.getAbsolutePath().startsWith(tempDirectory.getAbsolutePath())) {
                                    return false;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Si falla, seguimos con el comportamiento normal.
                }

                try {
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (files == null || files.isEmpty()) {
                        return false;
                    }
                    fileDropHandler.onDrop(files);
                    return true;
                } catch (Exception exception) {
                    return false;
                }
            }

            @Override
            public void exportDone(JComponent c, Transferable t, int action) {
                if (action == MOVE || action == COPY) {
                    cleanupTempDirectory();
                }
                exportedFiles = null;
                setStatus(" ", false);
            }

            private void cleanupTempDirectory() {
                if (tempDirectory != null && dragExportHandler != null) {
                    try {
                        dragExportHandler.cleanupTempDirectory(tempDirectory);
                    } catch (Exception e) {
                    }
                    tempDirectory = null;
                }
            }
        };
    }

    private static class FileListTransferable implements Transferable {
        private final List<File> files;

        FileListTransferable(List<File> files) {
            this.files = files;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.javaFileListFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return new ArrayList<>(files);
        }
    }

    private void updateControlStates() {
        boolean enabled = deviceAvailable && !busy;
        boolean hasListing = currentListing.currentPath() != null && !currentListing.currentPath().isBlank();
        boolean hasSelection = getSelectedEntry() != null;
        boolean singleSelection = filesTable.getSelectedRowCount() == 1;

        filesTable.setEnabled(enabled);
        pathField.setEnabled(deviceAvailable);
        pathField.setEditable(enabled && hasListing);
        upButton.setEnabled(enabled && hasListing && currentListing.hasParent());
        refreshButton.setEnabled(enabled && hasListing);
        createFolderButton.setEnabled(enabled && hasListing);
        uploadButton.setEnabled(enabled && hasListing);
        downloadButton.setEnabled(enabled && hasSelection);
        renameButton.setEnabled(enabled && singleSelection);
        copyButton.setEnabled(enabled && singleSelection);
        deleteButton.setEnabled(enabled && hasSelection);

        applyTheme(theme);
    }

    private void configureButton(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(8);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.getModel().addChangeListener(event -> {
            ToolbarIcon.Type iconType = (ToolbarIcon.Type) button.getClientProperty("iconType");
            boolean primary = Boolean.TRUE.equals(button.getClientProperty("primary"));
            styleButton(button, primary, iconType);
        });
    }

    private void styleButton(JButton button, boolean primary, ToolbarIcon.Type iconType) {
        button.putClientProperty("primary", primary);
        button.putClientProperty("iconType", iconType);
        int iconSize = button.getClientProperty("iconSize") instanceof Integer size ? size : 16;
        boolean enabled = button.isEnabled();
        boolean hovered = enabled && button.getModel().isRollover();

        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        Color background;
        Color foreground;
        if (enabled) {
            background = primary
                    ? theme.actionBackground()
                    : ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.84d);
            if (hovered) {
                background = ThemeUtils.blend(background, theme.selectionBackground(), primary ? 0.18d : 0.22d);
            }
            foreground = primary ? theme.actionForeground() : theme.textPrimary();
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primary ? background : theme.border(), 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        } else {
            background = theme.surface();
            foreground = theme.textSecondary();
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(theme.disabledBorder(), 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        }

        button.setBackground(background);
        button.setForeground(foreground);
        button.setIcon(new ToolbarIcon(iconType, iconSize, foreground));
    }

    private void styleTransferCancelButton() {
        boolean enabled = transferCancelable;
        boolean hovered = enabled && cancelTransferButton.getModel().isRollover();

        cancelTransferButton.setUI(new BasicButtonUI());
        cancelTransferButton.setFocusPainted(false);
        cancelTransferButton.setFocusable(false);
        cancelTransferButton.setRolloverEnabled(true);
        cancelTransferButton.setCursor(enabled
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
        cancelTransferButton.setOpaque(true);
        cancelTransferButton.setContentAreaFilled(true);
        cancelTransferButton.setBorderPainted(true);
        cancelTransferButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        cancelTransferButton.setMargin(new Insets(0, 0, 0, 0));
        cancelTransferButton.setPreferredSize(new Dimension(18, 18));
        cancelTransferButton.setMinimumSize(new Dimension(18, 18));
        cancelTransferButton.setMaximumSize(new Dimension(18, 18));
        cancelTransferButton.setToolTipText(Messages.text("files.action.cancelTransfer"));

        if (enabled) {
            Color background = hovered
                    ? ThemeUtils.blend(theme.surface(), new Color(214, 80, 80), 0.24d)
                    : theme.surface();
            cancelTransferButton.setBackground(background);
            cancelTransferButton.setForeground(theme.textSecondary());
            cancelTransferButton.setBorder(BorderFactory.createLineBorder(theme.border(), 1));
        } else {
            cancelTransferButton.setBackground(theme.background());
            cancelTransferButton.setForeground(theme.textSecondary());
            cancelTransferButton.setBorder(BorderFactory.createLineBorder(theme.disabledBorder(), 1));
        }
    }

    private void styleTable() {
        filesTable.setBackground(theme.background());
        filesTable.setForeground(theme.textPrimary());
        filesTable.setSelectionBackground(theme.selectionBackground());
        filesTable.setSelectionForeground(theme.selectionForeground());
        filesTable.setGridColor(theme.border());
        filesTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        JTableHeader tableHeader = filesTable.getTableHeader();
        tableHeader.setReorderingAllowed(false);
        tableHeader.setOpaque(true);
        tableHeader.setBackground(theme.secondarySurface());
        tableHeader.setForeground(theme.textSecondary());
        tableHeader.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        tableHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, theme.border()));
        tableHeader.setDefaultRenderer(new FilesTableHeaderRenderer());
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(theme.border(), 1));
        JViewport viewport = scrollPane.getViewport();
        viewport.setBackground(theme.background());
        if (scrollPane.getVerticalScrollBar() != null) {
            scrollPane.getVerticalScrollBar().setUI(new ThemedScrollBarUI(theme));
            scrollPane.getVerticalScrollBar().setUnitIncrement(24);
            scrollPane.getVerticalScrollBar().setBlockIncrement(96);
        }
        if (scrollPane.getHorizontalScrollBar() != null) {
            scrollPane.getHorizontalScrollBar().setUI(new ThemedScrollBarUI(theme));
            scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        }
        if (scrollPane.getColumnHeader() != null) {
            scrollPane.getColumnHeader().setBackground(theme.secondarySurface());
        }
    }

    private void styleContextMenu() {
        contextMenu.setBorder(BorderFactory.createLineBorder(theme.border(), 1));
        contextMenu.setBackground(theme.surface());
        openFolderMenuItem.setIcon(new ToolbarIcon(ToolbarIcon.Type.FOLDER, 16, theme.textPrimary()));
        downloadMenuItem.setIcon(new ToolbarIcon(ToolbarIcon.Type.DOWNLOAD, 16, theme.textPrimary()));
        renameMenuItem.setIcon(new ToolbarIcon(ToolbarIcon.Type.EDIT, 16, theme.textPrimary()));
        copyMenuItem.setIcon(new ToolbarIcon(ToolbarIcon.Type.COPY, 16, theme.textPrimary()));
        deleteMenuItem.setIcon(new ToolbarIcon(ToolbarIcon.Type.UNINSTALL, 16, theme.textPrimary()));
        for (Component component : contextMenu.getComponents()) {
            if (component instanceof JMenuItem menuItem) {
                menuItem.setOpaque(true);
                menuItem.setBackground(theme.surface());
                menuItem.setForeground(menuItem.isEnabled() ? theme.textPrimary() : theme.textSecondary());
                menuItem.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                menuItem.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                menuItem.setIconTextGap(10);
            }
        }
    }

    private void styleStatusLabel() {
        boolean error = Boolean.TRUE.equals(statusLabel.getClientProperty("error"));
        statusLabel.setForeground(error ? new Color(214, 80, 80) : theme.textSecondary());
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
    }

    private void styleSection(JPanel panel, String title) {
        panel.setBackground(theme.background());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(theme.border(), 1),
                        title,
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font(Font.SANS_SERIF, Font.BOLD, 18),
                        theme.textPrimary()),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
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

    private String resolveTypeLabel(DeviceFileEntry entry) {
        if (entry == null) {
            return Messages.text("common.noData");
        }

        String rawType = entry.rawType() == null ? "" : entry.rawType().toLowerCase(Locale.ROOT);
        if (entry.directory()) {
            return Messages.text("files.type.directory");
        }
        if (rawType.contains("symbolic")) {
            return Messages.text("files.type.link");
        }
        if (rawType.contains("file")) {
            return Messages.text("files.type.file");
        }
        return Messages.text("files.type.other");
    }

    private final class DeviceFilesTableModel extends AbstractTableModel {
        private static final int COLUMN_NAME = 0;
        private static final int COLUMN_TYPE = 1;
        private static final int COLUMN_SIZE = 2;
        private static final int COLUMN_MODIFIED = 3;

        private final List<DeviceFileEntry> entries = new ArrayList<>();
        private final String[] columnKeys = {
                "files.column.name",
                "files.column.type",
                "files.column.size",
                "files.column.modified"
        };

        @Override
        public int getRowCount() {
            return entries.size();
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
            DeviceFileEntry entry = entries.get(rowIndex);
            return switch (columnIndex) {
                case COLUMN_NAME -> entry.name();
                case COLUMN_TYPE -> resolveTypeLabel(entry);
                case COLUMN_SIZE -> entry.sizeLabel();
                case COLUMN_MODIFIED -> entry.modifiedLabel();
                default -> "";
            };
        }

        public DeviceFileEntry getEntryAt(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= entries.size()) {
                return null;
            }
            return entries.get(rowIndex);
        }

        public void setEntries(List<DeviceFileEntry> newEntries) {
            entries.clear();
            if (newEntries != null) {
                entries.addAll(newEntries);
            }
            fireTableDataChanged();
        }

        public void refreshTexts() {
            fireTableStructureChanged();
        }
    }

    private final class FilesTableCellRenderer extends DefaultTableCellRenderer {
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

            DeviceFileEntry entry = tableModel.getEntryAt(row);
            if (column == 0 && entry != null) {
                setIcon(new ToolbarIcon(
                        entry.directory() ? ToolbarIcon.Type.FOLDER : ToolbarIcon.Type.FILES,
                        16,
                        getForeground()));
                setIconTextGap(10);
            } else {
                setIcon(null);
            }

            if (column == 2 || column == 3) {
                setHorizontalAlignment(SwingConstants.RIGHT);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }

            return this;
        }
    }

    private final class FilesTableHeaderRenderer extends DefaultTableCellRenderer {
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
}
