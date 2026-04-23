package com.adbmanager.control;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.SwingWorker;

import com.adbmanager.logic.model.AppBackgroundMode;
import com.adbmanager.logic.model.AppDetails;
import com.adbmanager.logic.model.AppInstallRequest;
import com.adbmanager.logic.model.AppInstallResult;
import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.InstalledApp;
import com.adbmanager.view.Messages;
import com.adbmanager.view.swing.AppInstallDialog;
import com.adbmanager.view.swing.MainFrame;

final class ApplicationsController {

    private static final int MAX_BACKGROUND_APP_ENRICHMENTS = 50;
    private static final int VISIBLE_APP_ENRICHMENT_EXTRA_ROWS = 8;

    @FunctionalInterface
    private interface ApplicationTask {
        void run() throws Exception;
    }

    private final SwingControllerContext context;
    private final Runnable reloadDisplayApplications;

    ApplicationsController(SwingControllerContext context, Runnable reloadDisplayApplications) {
        this.context = context;
        this.reloadDisplayApplications = reloadDisplayApplications;
    }

    void resetState() {
        state().applicationsLoadedSerial = null;
        state().currentSelectedPackageName = null;
        resetApplicationEnrichmentState();
    }

    void clearViewState() {
        view().setApplicationsLoading(false, "");
        view().clearApplications();
        view().clearApplicationDetails();
        view().setApplicationActionsEnabled(false);
    }

    void clearUnavailableState() {
        resetState();
        clearViewState();
        view().setApplicationsEnabled(false);
    }

    void ensureApplicationsLoaded() {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isApplicationsAvailable(selectedDevice)) {
            clearUnavailableState();
            return;
        }

        if (context.isPowerActionPendingFor(selectedDevice.serial())) {
            resetState();
            view().setApplicationsLoading(true, Messages.text("apps.loading.afterPowerAction"));
            view().setApplicationsEnabled(false);
            view().setApplicationActionsEnabled(false);
            return;
        }

        if (state().loadingApplications) {
            return;
        }

        if (Objects.equals(state().applicationsLoadedSerial, selectedDevice.serial())) {
            view().setApplicationsEnabled(true);
            view().setApplicationActionsEnabled(view().getCurrentApplicationDetails() != null);
            return;
        }

        loadApplications();
    }

    void onApplicationSelected() {
        if (state().loadingApplications || state().loadingApplicationDetails) {
            return;
        }

        String packageName = view().getSelectedApplicationPackage();
        if (packageName == null || packageName.isBlank()) {
            state().currentSelectedPackageName = null;
            view().clearApplicationDetails();
            return;
        }

        if (packageName.equals(state().currentSelectedPackageName) && view().getCurrentApplicationDetails() != null) {
            return;
        }

        loadApplicationDetails(packageName);
    }

    void toggleApplicationPermission(String packageName, String permission, boolean granted) {
        runApplicationCommand(
                packageName,
                Messages.text("error.apps.permission"),
                null,
                true,
                () -> model().setSelectedDeviceApplicationPermission(packageName, permission, granted));
    }

    void changeApplicationBackgroundMode(String packageName, AppBackgroundMode mode) {
        runApplicationCommandAndReloadDetails(
                packageName,
                Messages.text("error.apps.backgroundMode"),
                () -> model().setSelectedDeviceApplicationBackgroundMode(packageName, mode));
    }

    void openSelectedApplication() {
        String packageName = selectedApplicationPackage();
        if (packageName == null) {
            return;
        }

        runApplicationCommand(
                packageName,
                Messages.text("error.apps.open"),
                null,
                false,
                () -> model().openSelectedDeviceApplication(packageName));
    }

    void stopSelectedApplication() {
        String packageName = selectedApplicationPackage();
        if (packageName == null) {
            return;
        }

        runApplicationCommand(
                packageName,
                Messages.text("error.apps.stop"),
                null,
                false,
                () -> model().stopSelectedDeviceApplication(packageName));
    }

    void uninstallSelectedApplication() {
        String packageName = selectedApplicationPackage();
        if (packageName == null) {
            return;
        }

        if (!view().confirmAction(
                Messages.text("apps.confirm.uninstall.title"),
                Messages.format("apps.confirm.uninstall.message", packageName))) {
            return;
        }

        runApplicationCommand(
                packageName,
                Messages.text("error.apps.uninstall"),
                Messages.format("info.app.uninstalled", packageName),
                true,
                () -> {
                    model().uninstallSelectedDeviceApplication(packageName);
                    state().currentSelectedPackageName = null;
                });
    }

    void toggleSelectedApplicationEnabled() {
        AppDetails details = view().getCurrentApplicationDetails();
        if (details == null) {
            view().showError(Messages.text("error.apps.noSelection"));
            return;
        }

        boolean enable = details.app().disabled();
        if (!enable && !view().confirmAction(
                Messages.text("apps.confirm.disable.title"),
                Messages.format("apps.confirm.disable.message", details.displayName()))) {
            return;
        }

        runApplicationCommand(
                details.app().packageName(),
                Messages.text("error.apps.toggle"),
                Messages.format("info.app.stateUpdated", details.app().packageName()),
                true,
                () -> model().setSelectedDeviceApplicationEnabled(details.app().packageName(), enable));
    }

    void clearSelectedApplicationData() {
        String packageName = selectedApplicationPackage();
        if (packageName == null) {
            return;
        }

        if (!view().confirmAction(
                Messages.text("apps.confirm.clearData.title"),
                Messages.format("apps.confirm.clearData.message", packageName))) {
            return;
        }

        runApplicationCommand(
                packageName,
                Messages.text("error.apps.clearData"),
                Messages.format("info.app.dataCleared", packageName),
                true,
                () -> model().clearSelectedDeviceApplicationData(packageName));
    }

    void clearSelectedApplicationCache() {
        String packageName = selectedApplicationPackage();
        if (packageName == null) {
            return;
        }

        if (!view().confirmAction(
                Messages.text("apps.confirm.clearCache.title"),
                Messages.format("apps.confirm.clearCache.message", packageName))) {
            return;
        }

        runApplicationCommand(
                packageName,
                Messages.text("error.apps.clearCache"),
                Messages.format("info.app.cacheCleared", packageName),
                true,
                () -> model().clearSelectedDeviceApplicationCache(packageName));
    }

    void exportSelectedApplicationApk() {
        AppDetails details = view().getCurrentApplicationDetails();
        if (details == null) {
            view().showError(Messages.text("error.apps.noSelection"));
            return;
        }

        File outputFile = view().chooseApkDestination(details.app().defaultApkFileName());
        if (outputFile == null) {
            return;
        }

        runApplicationCommand(
                details.app().packageName(),
                Messages.text("error.apps.export"),
                Messages.format("info.app.apk.saved", outputFile.getAbsolutePath()),
                false,
                () -> model().exportSelectedDeviceApplicationApk(details.app().packageName(), outputFile));
    }

    void openApplicationInstallDialog() {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isApplicationsAvailable(selectedDevice)) {
            view().showError(Messages.text("error.apps.deviceRequired"));
            return;
        }

        AppInstallDialog dialog = view().getAppInstallDialog();
        if (!dialog.isBusy()) {
            dialog.clearLog();
            dialog.showStatus(Messages.text("apps.install.status.ready"), false);
        }
        dialog.open();
    }

    void installSelectedPackages() {
        AppInstallDialog dialog = view().getAppInstallDialog();
        if (dialog.isBusy()) {
            return;
        }

        AppInstallRequest request = dialog.getInstallRequest();
        if (!request.hasInputs()) {
            dialog.showStatus(Messages.text("error.apps.install.noFiles"), true);
            return;
        }

        dialog.clearLog();
        dialog.showStatus(Messages.text("apps.install.status.installing"), false);
        dialog.setBusy(true);

        new SwingWorker<AppInstallResult, String>() {
            @Override
            protected AppInstallResult doInBackground() throws Exception {
                return model().installSelectedDevicePackages(request, this::publish);
            }

            @Override
            protected void process(List<String> chunks) {
                for (String chunk : chunks) {
                    dialog.appendLog(chunk);
                }
            }

            @Override
            protected void done() {
                try {
                    AppInstallResult result = get();
                    dialog.appendLog(Messages.format(
                            "apps.install.summary",
                            result.successCount(),
                            result.failureCount()));

                    boolean hasFailures = result.failureCount() > 0;
                    dialog.showStatus(
                            Messages.text(hasFailures
                                    ? "apps.install.status.completedWithIssues"
                                    : "apps.install.status.completed"),
                            hasFailures);

                    if (result.successCount() > 0) {
                        ApplicationsController.this.state().applicationsLoadedSerial = null;
                        ApplicationsController.this.state().scrcpyApplicationsLoadedSerial = null;
                        if (view().isAppsScreenVisible()) {
                            loadApplications();
                        }
                        reloadDisplayApplications.run();
                    }
                } catch (Exception exception) {
                    dialog.showStatus(
                            context.extractErrorMessage(exception, Messages.text("error.apps.install")),
                            true);
                    dialog.appendLog(context.extractErrorMessage(exception, Messages.text("error.apps.install")));
                } finally {
                    dialog.setBusy(false);
                }
            }
        }.execute();
    }

    void refreshVisibleApplicationSummaries() {
        if (!view().isAppsScreenVisible()) {
            return;
        }

        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isApplicationsAvailable(selectedDevice)
                || !Objects.equals(
                        state().applicationsLoadedSerial,
                        selectedDevice == null ? null : selectedDevice.serial())) {
            return;
        }

        prioritizeApplicationSummaries(
                state().currentSelectedPackageName,
                view().getVisibleApplicationPackages(VISIBLE_APP_ENRICHMENT_EXTRA_ROWS));

        if (!state().loadingApplications) {
            startApplicationEnrichmentIfNeeded(selectedDevice.serial());
        }
    }

    private void loadApplications() {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isApplicationsAvailable(selectedDevice)) {
            clearUnavailableState();
            return;
        }

        String requestedSerial = selectedDevice.serial();
        if (context.isPowerActionPendingFor(requestedSerial)) {
            resetState();
            view().setApplicationsLoading(true, Messages.text("apps.loading.afterPowerAction"));
            view().setApplicationsEnabled(false);
            view().setApplicationActionsEnabled(false);
            return;
        }

        String preferredPackage = state().currentSelectedPackageName;
        state().loadingApplications = true;
        resetApplicationEnrichmentState();
        view().setApplicationsLoading(true, Messages.text("apps.loading.list"));
        view().setApplicationsEnabled(false);
        view().setApplicationActionsEnabled(false);

        new SwingWorker<List<InstalledApp>, Void>() {
            @Override
            protected List<InstalledApp> doInBackground() throws Exception {
                return model().getSelectedDeviceApplications();
            }

            @Override
            protected void done() {
                try {
                    List<InstalledApp> applications = get();
                    if (!Objects.equals(requestedSerial, ApplicationsController.this.state().currentSelectedSerial)) {
                        return;
                    }

                    ApplicationsController.this.state().applicationsLoadedSerial = requestedSerial;
                    view().setApplications(applications, preferredPackage);
                    if (applications.isEmpty()) {
                        view().setApplicationsLoading(false, "");
                        ApplicationsController.this.state().currentSelectedPackageName = null;
                        view().clearApplicationDetails();
                    } else {
                        queueAllApplicationSummaries(applications);
                        startApplicationEnrichmentIfNeeded(requestedSerial);
                        String selectedPackage = view().getSelectedApplicationPackage();
                        if (selectedPackage != null && !selectedPackage.isBlank()) {
                            loadApplicationDetails(selectedPackage);
                        }
                    }
                } catch (Exception exception) {
                    if (!context.shouldSuppressApplicationLoadError(requestedSerial, exception)) {
                        context.handleError(Messages.text("error.apps.load"), exception);
                    }
                    resetApplicationEnrichmentState();
                    view().setApplicationsLoading(false, "");
                    view().clearApplications();
                    view().clearApplicationDetails();
                    ApplicationsController.this.state().applicationsLoadedSerial = null;
                } finally {
                    ApplicationsController.this.state().loadingApplications = false;
                    if (Objects.equals(
                            requestedSerial,
                            ApplicationsController.this.state().currentSelectedSerial) && view().isAppsScreenVisible()) {
                        view().setApplicationsEnabled(true);
                        refreshVisibleApplicationSummaries();
                    }
                }
            }
        }.execute();
    }

    private void loadApplicationDetails(String packageName) {
        Device selectedDevice = model().getSelectedDevice().orElse(null);
        if (!context.isApplicationsAvailable(selectedDevice)) {
            view().clearApplicationDetails();
            return;
        }

        String requestedSerial = selectedDevice.serial();
        String requestedPackage = packageName;
        state().currentSelectedPackageName = packageName;
        state().loadingApplicationDetails = true;
        view().setApplicationActionsEnabled(false);
        view().showApplicationDetailsLoading(requestedPackage);

        new SwingWorker<AppDetails, Void>() {
            @Override
            protected AppDetails doInBackground() throws Exception {
                return model().getSelectedDeviceApplicationDetails(requestedPackage);
            }

            @Override
            protected void done() {
                String selectedPackageNow = view().getSelectedApplicationPackage();
                try {
                    AppDetails details = get();
                    if (!Objects.equals(requestedSerial, ApplicationsController.this.state().currentSelectedSerial)
                            || !Objects.equals(requestedPackage, selectedPackageNow)) {
                        return;
                    }

                    ApplicationsController.this.state().currentSelectedPackageName = details.app().packageName();
                    ApplicationsController.this.state().enrichedApplicationPackages.add(details.app().packageName());
                    ApplicationsController.this.state().pendingApplicationPackages.remove(details.app().packageName());
                    view().updateApplication(details.toListEntry().withFlags(
                            details.app().systemApp(),
                            details.app().disabled()));
                    view().setApplicationDetails(details);
                } catch (Exception exception) {
                    if (Objects.equals(requestedPackage, selectedPackageNow)) {
                        context.handleError(Messages.text("error.apps.details"), exception);
                        view().clearApplicationDetails();
                    }
                } finally {
                    ApplicationsController.this.state().loadingApplicationDetails = false;
                    if (!Objects.equals(
                            requestedSerial,
                            ApplicationsController.this.state().currentSelectedSerial)) {
                        return;
                    }

                    String nextSelectedPackage = view().getSelectedApplicationPackage();
                    if (nextSelectedPackage != null
                            && !nextSelectedPackage.isBlank()
                            && !Objects.equals(nextSelectedPackage, requestedPackage)) {
                        loadApplicationDetails(nextSelectedPackage);
                        return;
                    }

                    view().setApplicationActionsEnabled(view().getCurrentApplicationDetails() != null);
                }
            }
        }.execute();
    }

    private void runApplicationCommand(
            String packageName,
            String errorMessage,
            String successMessage,
            boolean reloadApplications,
            ApplicationTask task) {
        if (packageName == null || packageName.isBlank()) {
            view().showError(Messages.text("error.apps.noSelection"));
            return;
        }

        view().setApplicationActionsEnabled(false);
        view().setApplicationsEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                task.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (successMessage != null && !successMessage.isBlank()) {
                        view().showInfo(successMessage);
                    }

                    if (reloadApplications) {
                        ApplicationsController.this.state().applicationsLoadedSerial = null;
                        loadApplications();
                    } else if (packageName.equals(view().getSelectedApplicationPackage())) {
                        view().setApplicationsEnabled(true);
                        view().setApplicationActionsEnabled(view().getCurrentApplicationDetails() != null);
                    }
                } catch (Exception exception) {
                    context.handleError(errorMessage, exception);
                    if (reloadApplications) {
                        ApplicationsController.this.state().applicationsLoadedSerial = null;
                        loadApplications();
                    } else {
                        view().setApplicationsEnabled(true);
                        view().setApplicationActionsEnabled(view().getCurrentApplicationDetails() != null);
                    }
                }
            }
        }.execute();
    }

    private void runApplicationCommandAndReloadDetails(
            String packageName,
            String errorMessage,
            ApplicationTask task) {
        if (packageName == null || packageName.isBlank()) {
            view().showError(Messages.text("error.apps.noSelection"));
            return;
        }

        String requestedSerial = state().currentSelectedSerial;
        view().setApplicationActionsEnabled(false);
        view().setApplicationsEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                task.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (!Objects.equals(requestedSerial, ApplicationsController.this.state().currentSelectedSerial)) {
                        return;
                    }

                    view().setApplicationsEnabled(true);
                    loadApplicationDetails(packageName);
                } catch (Exception exception) {
                    context.handleError(errorMessage, exception);
                    view().setApplicationsEnabled(true);
                    view().setApplicationActionsEnabled(view().getCurrentApplicationDetails() != null);
                }
            }
        }.execute();
    }

    private String selectedApplicationPackage() {
        String packageName = view().getSelectedApplicationPackage();
        if (packageName == null || packageName.isBlank()) {
            view().showError(Messages.text("error.apps.noSelection"));
            return null;
        }
        return packageName;
    }

    private void queueAllApplicationSummaries(List<InstalledApp> applications) {
        resetApplicationEnrichmentState();
        state().totalApplicationPackagesToEnrich = applications == null ? 0 : applications.size();
        prioritizeApplicationSummaries(
                state().currentSelectedPackageName,
                view().getVisibleApplicationPackages(VISIBLE_APP_ENRICHMENT_EXTRA_ROWS));
        if (applications == null) {
            return;
        }

        for (InstalledApp application : applications) {
            if (application != null) {
                queueApplicationSummary(application.packageName());
            }
        }
    }

    private void prioritizeApplicationSummaries(String selectedPackageName, List<String> visiblePackages) {
        List<String> prioritizedPackages = new ArrayList<>();
        if (selectedPackageName != null && !selectedPackageName.isBlank()) {
            prioritizedPackages.add(selectedPackageName);
        }
        if (visiblePackages != null) {
            for (String packageName : visiblePackages) {
                if (packageName != null
                        && !packageName.isBlank()
                        && !prioritizedPackages.contains(packageName)) {
                    prioritizedPackages.add(packageName);
                }
            }
        }

        if (prioritizedPackages.isEmpty() || state().pendingApplicationPackages.isEmpty()) {
            for (String packageName : prioritizedPackages) {
                queueApplicationSummary(packageName);
            }
            return;
        }

        java.util.Set<String> reorderedPackages = new java.util.LinkedHashSet<>();
        for (String packageName : prioritizedPackages) {
            if (!state().enrichedApplicationPackages.contains(packageName)) {
                reorderedPackages.add(packageName);
            }
        }
        for (String packageName : state().pendingApplicationPackages) {
            if (!state().enrichedApplicationPackages.contains(packageName)) {
                reorderedPackages.add(packageName);
            }
        }
        state().pendingApplicationPackages.clear();
        state().pendingApplicationPackages.addAll(reorderedPackages);
    }

    private void queueApplicationSummary(String packageName) {
        if (packageName == null
                || packageName.isBlank()
                || state().enrichedApplicationPackages.contains(packageName)) {
            return;
        }

        state().pendingApplicationPackages.add(packageName);
    }

    private void startApplicationEnrichmentIfNeeded(String requestedSerial) {
        if (state().applicationEnrichmentWorker != null) {
            return;
        }

        if (state().pendingApplicationPackages.isEmpty()) {
            view().setApplicationsLoading(false, "");
            return;
        }

        List<String> packageNames = new ArrayList<>();
        while (!state().pendingApplicationPackages.isEmpty()
                && packageNames.size() < MAX_BACKGROUND_APP_ENRICHMENTS) {
            String packageName = state().pendingApplicationPackages.iterator().next();
            state().pendingApplicationPackages.remove(packageName);
            if (!state().enrichedApplicationPackages.contains(packageName)) {
                packageNames.add(packageName);
            }
        }

        if (packageNames.isEmpty()) {
            if (state().pendingApplicationPackages.isEmpty()) {
                view().setApplicationsLoading(false, "");
            } else {
                startApplicationEnrichmentIfNeeded(requestedSerial);
            }
            return;
        }

        int initialLoadedCount = state().enrichedApplicationPackages.size();
        view().setApplicationsLoading(true, Messages.format(
                "apps.loading.metadata.progress",
                initialLoadedCount,
                Math.max(initialLoadedCount + packageNames.size(), state().totalApplicationPackagesToEnrich)));

        state().applicationEnrichmentWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                if (isCancelled()
                        || !Objects.equals(requestedSerial, ApplicationsController.this.state().currentSelectedSerial)) {
                    return null;
                }

                try {
                    List<InstalledApp> applications = model().getSelectedDeviceApplicationSummaries(packageNames);
                    if (!isCancelled()
                            && Objects.equals(
                                    requestedSerial,
                                    ApplicationsController.this.state().currentSelectedSerial)) {
                        publish(applications);
                    }
                } catch (Exception ignored) {
                }
                return null;
            }

            @Override
            protected void process(List<List<InstalledApp>> chunks) {
                if (!Objects.equals(requestedSerial, ApplicationsController.this.state().currentSelectedSerial)) {
                    return;
                }

                for (List<InstalledApp> chunk : chunks) {
                    List<InstalledApp> validApplications = chunk == null ? List.of() : chunk.stream()
                            .filter(Objects::nonNull)
                            .toList();
                    if (validApplications.isEmpty()) {
                        continue;
                    }

                    for (InstalledApp application : validApplications) {
                        ApplicationsController.this.state().enrichedApplicationPackages.add(application.packageName());
                        ApplicationsController.this.state().pendingApplicationPackages.remove(application.packageName());
                    }
                    view().updateApplications(validApplications);
                }

                view().setApplicationsLoading(true, Messages.format(
                        "apps.loading.metadata.progress",
                        ApplicationsController.this.state().enrichedApplicationPackages.size(),
                        Math.max(
                                ApplicationsController.this.state().enrichedApplicationPackages.size()
                                        + ApplicationsController.this.state().pendingApplicationPackages.size(),
                                ApplicationsController.this.state().totalApplicationPackagesToEnrich)));
            }

            @Override
            protected void done() {
                if (ApplicationsController.this.state().applicationEnrichmentWorker != this) {
                    return;
                }

                ApplicationsController.this.state().applicationEnrichmentWorker = null;
                if (!Objects.equals(requestedSerial, ApplicationsController.this.state().currentSelectedSerial)) {
                    return;
                }

                if (!ApplicationsController.this.state().pendingApplicationPackages.isEmpty()) {
                    startApplicationEnrichmentIfNeeded(requestedSerial);
                    return;
                }

                view().setApplicationsLoading(false, "");
            }
        };

        state().applicationEnrichmentWorker.execute();
    }

    private void cancelApplicationEnrichment() {
        if (state().applicationEnrichmentWorker != null) {
            state().applicationEnrichmentWorker.cancel(true);
            state().applicationEnrichmentWorker = null;
        }
    }

    private void resetApplicationEnrichmentState() {
        cancelApplicationEnrichment();
        state().enrichedApplicationPackages.clear();
        state().pendingApplicationPackages.clear();
        state().totalApplicationPackagesToEnrich = 0;
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
