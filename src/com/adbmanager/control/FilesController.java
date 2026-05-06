package com.adbmanager.control;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import javax.swing.SwingWorker;

import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.DeviceDirectoryListing;
import com.adbmanager.logic.model.DeviceFileEntry;
import com.adbmanager.logic.model.FileTransferProgress;
import com.adbmanager.logic.model.InstalledApp;
import com.adbmanager.view.Messages;
import com.adbmanager.view.swing.MainFrame;

final class FilesController {

    @FunctionalInterface
    private interface FileTask {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface FileTransferTask {
        void run(Consumer<FileTransferProgress> progressCallback) throws Exception;
    }

    private final SwingControllerContext context;

    FilesController(SwingControllerContext context) {
        this.context = context;
    }

    void resetState() {
        state().filesLoadedSerial = null;
        state().currentFilesDirectory = null;
    }

    void clearViewState() {
        view().clearFilesListing();
        view().setFilesStatus("", false);
        view().clearFilesTransferProgress();
        view().setFilesTransferCancelable(false);
    }

    void clearUnavailableState() {
        view().setFilesDeviceAvailable(false);
        clearViewState();
    }

    void refreshDirectory(boolean showErrors, boolean forceReload, String preferredPath) {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isFilesAvailable(selectedDevice)) {
            clearUnavailableState();
            return;
        }

        if (state().loadingFiles || (state().applyingFileAction && !showErrors)) {
            return;
        }

        String requestedSerial = selectedDevice.serial();
        String targetPath = normalizePath(preferredPath == null || preferredPath.isBlank()
                ? state().currentFilesDirectory
                : preferredPath);

        if (!forceReload
                && Objects.equals(state().filesLoadedSerial, requestedSerial)
                && targetPath != null
                && Objects.equals(state().currentFilesDirectory, targetPath)) {
            view().setFilesDeviceAvailable(true);
            context.updateFilesBusyState();
            return;
        }

        state().loadingFiles = true;
        view().setFilesDeviceAvailable(true);
        view().setFilesStatus(Messages.text("files.status.loading"), false);
        view().clearFilesTransferProgress();
        view().setFilesTransferCancelable(false);
        context.updateFilesBusyState();

        new SwingWorker<DeviceDirectoryListing, Void>() {
            @Override
            protected DeviceDirectoryListing doInBackground() throws Exception {
                return model().listSelectedDeviceDirectory(targetPath);
            }

            @Override
            protected void done() {
                try {
                    DeviceDirectoryListing listing = get();
                    if (!Objects.equals(requestedSerial, FilesController.this.state().currentSelectedSerial)) {
                        return;
                    }

                    FilesController.this.state().filesLoadedSerial = requestedSerial;
                    FilesController.this.state().currentFilesDirectory = listing.currentPath();
                    view().setFilesListing(listing);
                    view().setFilesStatus(Messages.format("files.status.ready", listing.currentPath()), false);
                } catch (Exception exception) {
                    if (!Objects.equals(requestedSerial, FilesController.this.state().currentSelectedSerial)) {
                        return;
                    }

                    view().setFilesStatus(
                            context.extractErrorMessage(exception, Messages.text("error.files.load")),
                            true);
                    if (showErrors) {
                        context.handleError(Messages.text("error.files.load"), exception);
                    }
                } finally {
                    FilesController.this.state().loadingFiles = false;
                    context.updateFilesBusyState();
                }
            }
        }.execute();
    }

    void navigateToParentDirectory() {
        String parentPath = view().getParentFilesDirectory();
        if (parentPath != null && !parentPath.isBlank()) {
            refreshDirectory(true, true, parentPath);
        }
    }

    void openSelectedDirectory() {
        DeviceFileEntry selectedEntry = view().getSelectedFileEntry();
        if (selectedEntry != null && selectedEntry.directory()) {
            refreshDirectory(false, true, selectedEntry.path());
        }
    }

    void createDirectory() {
        String currentDirectory = view().getCurrentFilesDirectory();
        if (currentDirectory == null || currentDirectory.isBlank()) {
            view().showError(Messages.text("error.files.deviceRequired"));
            return;
        }

        String directoryName = view().promptText(
                Messages.text("files.dialog.createFolder.title"),
                Messages.text("files.dialog.createFolder.message"),
                Messages.text("files.dialog.createFolder.default"));
        if (directoryName == null) {
            return;
        }

        runFilesCommand(
                Messages.text("error.files.createFolder"),
                Messages.format("files.status.folderCreated", directoryName.trim()),
                true,
                () -> model().createSelectedDeviceDirectory(currentDirectory, directoryName));
    }

    void uploadFilesToCurrentDirectory() {
        List<File> localFiles = view().chooseFilesToUpload();
        if (localFiles == null || localFiles.isEmpty()) {
            return;
        }
        uploadLocalFiles(localFiles);
    }

    void uploadDroppedFiles(List<File> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        uploadLocalFiles(files);
    }

    void downloadSelectedFiles() {
        List<String> selectedPaths = view().getSelectedFilePaths();
        if (selectedPaths.isEmpty()) {
            view().showError(Messages.text("error.files.noSelection"));
            return;
        }

        File destinationDirectory = view().chooseDownloadDirectory();
        if (destinationDirectory == null) {
            return;
        }

        runFilesTransferCommand(
                Messages.text("error.files.download"),
                Messages.format("files.status.downloaded", selectedPaths.size()),
                false,
                progress -> model().pullSelectedDevicePaths(selectedPaths, destinationDirectory, progress));
    }

    File downloadSelectedFilesToTemp() throws Exception {
        List<String> selectedPaths = view().getSelectedFilePaths();
        if (selectedPaths.isEmpty()) {
            throw new IllegalStateException(Messages.text("error.files.noSelection"));
        }

        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isFilesAvailable(selectedDevice)) {
            throw new IllegalStateException(Messages.text("error.files.deviceRequired"));
        }

        File tempDir = Files.createTempDirectory("adbmanager-drag-").toFile();
        try {
            model().pullSelectedDevicePaths(selectedPaths, tempDir, progress -> {
            });
            File[] files = tempDir.listFiles();
            if (files == null || files.length == 0) {
                tempDir.delete();
                throw new IllegalStateException(Messages.text("error.files.download"));
            }
            return tempDir;
        } catch (Exception e) {
            deleteDirectory(tempDir);
            throw e;
        }
    }

    void cleanupTempDirectory(File tempDir) {
        if (tempDir != null && tempDir.exists()) {
            deleteDirectory(tempDir);
        }
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    void renameSelectedFile() {
        DeviceFileEntry selectedEntry = view().getSelectedFileEntry();
        if (selectedEntry == null) {
            view().showError(Messages.text("error.files.noSelection"));
            return;
        }

        String newName = view().promptText(
                Messages.text("files.dialog.rename.title"),
                Messages.text("files.dialog.rename.message"),
                selectedEntry.name());
        if (newName == null) {
            return;
        }

        runFilesCommand(
                Messages.text("error.files.rename"),
                Messages.format("files.status.renamed", newName.trim()),
                true,
                () -> model().renameSelectedDevicePath(selectedEntry.path(), newName));
    }

    void copySelectedFile() {
        DeviceFileEntry selectedEntry = view().getSelectedFileEntry();
        if (selectedEntry == null) {
            view().showError(Messages.text("error.files.noSelection"));
            return;
        }

        String copyName = view().promptText(
                Messages.text("files.dialog.copy.title"),
                Messages.text("files.dialog.copy.message"),
                buildCopyNameSuggestion(selectedEntry));
        if (copyName == null) {
            return;
        }

        String parentPath = parentDirectoryOf(selectedEntry.path());
        String destinationPath = joinRemotePath(parentPath, copyName);
        runFilesCommand(
                Messages.text("error.files.copy"),
                Messages.format("files.status.copied", copyName.trim()),
                true,
                () -> model().copySelectedDevicePath(selectedEntry.path(), destinationPath));
    }

    void deleteSelectedFiles() {
        List<DeviceFileEntry> selectedEntries = view().getSelectedFileEntries();
        if (selectedEntries.isEmpty()) {
            view().showError(Messages.text("error.files.noSelection"));
            return;
        }

        String confirmationMessage = selectedEntries.size() == 1
                ? Messages.format("files.confirm.delete.single", selectedEntries.get(0).name())
                : Messages.format("files.confirm.delete.multiple", selectedEntries.size());
        if (!view().confirmAction(Messages.text("files.confirm.delete.title"), confirmationMessage)) {
            return;
        }

        List<String> selectedPaths = selectedEntries.stream()
                .map(DeviceFileEntry::path)
                .filter(Objects::nonNull)
                .toList();

        runFilesCommand(
                Messages.text("error.files.delete"),
                Messages.format("files.status.deleted", selectedEntries.size()),
                true,
                () -> {
                    for (String path : selectedPaths) {
                        model().deleteSelectedDevicePath(path);
                    }
                });
    }

    void cancelTransfer() {
        if (state().filesTransferWorker == null || state().filesTransferWorker.isDone()) {
            view().setFilesTransferCancelable(false);
            return;
        }

        state().cancellingFileTransfer = true;
        view().setFilesTransferCancelable(false);
        view().setFilesStatus(Messages.text("files.status.canceling"), false);
        model().cancelCurrentFileTransfer();
        state().filesTransferWorker.cancel(true);
    }

    private void uploadLocalFiles(List<File> files) {
        String currentDirectory = view().getCurrentFilesDirectory();
        if (currentDirectory == null || currentDirectory.isBlank()) {
            view().showError(Messages.text("error.files.deviceRequired"));
            return;
        }

        runFilesTransferCommand(
                Messages.text("error.files.upload"),
                Messages.format("files.status.uploaded", files.size()),
                true,
                progress -> model().pushToSelectedDeviceDirectory(files, currentDirectory, progress));
    }

    private void runFilesCommand(
            String errorMessage,
            String successMessage,
            boolean refreshAfter,
            FileTask task) {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isFilesAvailable(selectedDevice)) {
            view().showError(Messages.text("error.files.deviceRequired"));
            return;
        }

        String requestedSerial = selectedDevice.serial();
        String currentDirectory = normalizePath(view().getCurrentFilesDirectory());
        state().applyingFileAction = true;
        view().setFilesStatus(Messages.text("files.status.executing"), false);
        context.updateFilesBusyState();

        new SwingWorker<DeviceDirectoryListing, Void>() {
            @Override
            protected DeviceDirectoryListing doInBackground() throws Exception {
                task.run();
                if (refreshAfter) {
                    return model().listSelectedDeviceDirectory(currentDirectory);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    DeviceDirectoryListing listing = get();
                    if (!Objects.equals(requestedSerial, FilesController.this.state().currentSelectedSerial)) {
                        return;
                    }

                    if (refreshAfter && listing != null) {
                        FilesController.this.state().filesLoadedSerial = requestedSerial;
                        FilesController.this.state().currentFilesDirectory = listing.currentPath();
                        view().setFilesListing(listing);
                    }
                    view().setFilesStatus(successMessage, false);
                } catch (Exception exception) {
                    if (Objects.equals(requestedSerial, FilesController.this.state().currentSelectedSerial)) {
                        view().setFilesStatus(context.extractErrorMessage(exception, errorMessage), true);
                        context.handleError(errorMessage, exception);
                    }
                } finally {
                    FilesController.this.state().applyingFileAction = false;
                    context.updateFilesBusyState();
                }
            }
        }.execute();
    }

    private void runFilesTransferCommand(
            String errorMessage,
            String successMessage,
            boolean refreshAfter,
            FileTransferTask task) {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isFilesAvailable(selectedDevice)) {
            view().showError(Messages.text("error.files.deviceRequired"));
            return;
        }

        String requestedSerial = selectedDevice.serial();
        String currentDirectory = normalizePath(view().getCurrentFilesDirectory());
        state().applyingFileAction = true;
        state().cancellingFileTransfer = false;
        view().setFilesStatus(Messages.text("files.status.executing"), false);
        view().setFilesTransferProgress(true, true, 0, Messages.text("files.status.executing"));
        view().setFilesTransferCancelable(true);
        context.updateFilesBusyState();

        state().filesTransferWorker = new SwingWorker<DeviceDirectoryListing, FileTransferProgress>() {
            @Override
            protected DeviceDirectoryListing doInBackground() throws Exception {
                task.run(this::publish);
                if (refreshAfter) {
                    return model().listSelectedDeviceDirectory(currentDirectory);
                }
                return null;
            }

            @Override
            protected void process(List<FileTransferProgress> chunks) {
                if (!Objects.equals(requestedSerial, FilesController.this.state().currentSelectedSerial)
                        || chunks == null
                        || chunks.isEmpty()) {
                    return;
                }

                FileTransferProgress latestProgress = chunks.get(chunks.size() - 1);
                view().setFilesTransferProgress(
                        true,
                        latestProgress.indeterminate(),
                        latestProgress.percent(),
                        formatTransferProgress(latestProgress));
            }

            @Override
            protected void done() {
                try {
                    DeviceDirectoryListing listing = get();
                    if (!Objects.equals(requestedSerial, FilesController.this.state().currentSelectedSerial)) {
                        return;
                    }

                    if (refreshAfter && listing != null) {
                        FilesController.this.state().filesLoadedSerial = requestedSerial;
                        FilesController.this.state().currentFilesDirectory = listing.currentPath();
                        view().setFilesListing(listing);
                    }
                    view().setFilesStatus(successMessage, false);
                    view().clearFilesTransferProgress();
                    view().setFilesTransferCancelable(false);
                } catch (CancellationException exception) {
                    if (Objects.equals(requestedSerial, FilesController.this.state().currentSelectedSerial)) {
                        view().setFilesStatus(Messages.text("files.status.cancelled"), false);
                        view().clearFilesTransferProgress();
                        view().setFilesTransferCancelable(false);
                    }
                } catch (Exception exception) {
                    if (Objects.equals(requestedSerial, FilesController.this.state().currentSelectedSerial)) {
                        boolean cancelled = FilesController.this.state().cancellingFileTransfer || isCancelled();
                        if (cancelled) {
                            view().setFilesStatus(Messages.text("files.status.cancelled"), false);
                        } else {
                            view().setFilesStatus(context.extractErrorMessage(exception, errorMessage), true);
                            context.handleError(errorMessage, exception);
                        }
                        view().clearFilesTransferProgress();
                        view().setFilesTransferCancelable(false);
                    }
                } finally {
                    FilesController.this.state().filesTransferWorker = null;
                    FilesController.this.state().cancellingFileTransfer = false;
                    FilesController.this.state().applyingFileAction = false;
                    view().setFilesTransferCancelable(false);
                    context.updateFilesBusyState();
                }
            }
        };
        state().filesTransferWorker.execute();
    }

    private String formatTransferProgress(FileTransferProgress progress) {
        if (progress == null) {
            return " ";
        }

        String transferred = InstalledApp.formatBytes(progress.transferredBytes());
        String total = progress.totalBytes() > 0L ? InstalledApp.formatBytes(progress.totalBytes()) : "?";
        String speed = progress.bytesPerSecond() > 0L
                ? InstalledApp.formatBytes(progress.bytesPerSecond()) + "/s"
                : "-";

        if (progress.indeterminate()) {
            return transferred + " · " + speed;
        }
        return progress.percent() + "% · " + transferred + " / " + total + " · " + speed;
    }

    private String normalizePath(String path) {
        if (path == null) {
            return null;
        }

        String normalized = path.trim().replace('\\', '/');
        if (normalized.isBlank()) {
            return null;
        }

        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String parentDirectoryOf(String path) {
        String normalized = normalizePath(path);
        if (normalized == null || normalized.isBlank() || "/".equals(normalized)) {
            return "/";
        }

        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        return normalized.substring(0, lastSlash);
    }

    private String joinRemotePath(String parentPath, String name) {
        String parent = normalizePath(parentPath);
        String child = name == null ? "" : name.trim();
        if (parent == null || parent.isBlank() || "/".equals(parent)) {
            return "/" + child;
        }
        return parent + "/" + child;
    }

    private String buildCopyNameSuggestion(DeviceFileEntry entry) {
        String baseName = entry == null ? "" : entry.name();
        if (baseName == null || baseName.isBlank()) {
            return Messages.text("files.copy.defaultName");
        }

        if (entry.directory()) {
            return Messages.format("files.copy.directoryName", baseName);
        }

        int lastDot = baseName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < baseName.length() - 1) {
            String prefix = baseName.substring(0, lastDot);
            String extension = baseName.substring(lastDot);
            return Messages.format("files.copy.fileName", prefix, extension);
        }

        return Messages.format("files.copy.directoryName", baseName);
    }

    private SwingControllerState state() {
        return context.state;
    }

    private MainFrame view() {
        return context.view;
    }

    private com.adbmanager.logic.AdbModel model() {
        return context.model;
    }
}
