package com.adbmanager.control;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.SwingWorker;

import com.adbmanager.logic.model.DeviceDirectoryListing;
import com.adbmanager.logic.model.FileTransferProgress;
import com.adbmanager.logic.model.InstalledApp;
import com.adbmanager.logic.model.WirelessEndpointDiscovery;
import com.adbmanager.logic.model.WirelessPairingQrPayload;

final class SwingControllerState {

    boolean syncingDeviceSelector;
    boolean loadingDevices;
    boolean loadingApplications;
    boolean loadingApplicationDetails;
    boolean loadingScrcpyStatus;
    boolean loadingScrcpyApplications;
    boolean loadingScrcpyCameras;
    boolean loadingSystemState;
    boolean loadingControlState;
    boolean loadingFiles;
    boolean preparingScrcpy;
    boolean launchingScrcpy;
    boolean applyingSystemAction;
    boolean applyingControlAction;
    boolean applyingFileAction;
    String currentSelectedSerial;
    String applicationsLoadedSerial;
    String filesLoadedSerial;
    String scrcpyApplicationsLoadedSerial;
    String scrcpyCamerasLoadedSerial;
    String currentSelectedPackageName;
    String currentFilesDirectory;
    SwingWorker<Void, java.util.List<InstalledApp>> applicationEnrichmentWorker;
    final Set<String> enrichedApplicationPackages = new HashSet<>();
    final Set<String> pendingApplicationPackages = new LinkedHashSet<>();
    int totalApplicationPackagesToEnrich;
    boolean autoRefreshOnFocus = true;
    WirelessPairingQrPayload currentQrPayload;
    SwingWorker<Void, WirelessEndpointDiscovery> wirelessEndpointDiscoveryWorker;
    SwingWorker<DeviceDirectoryListing, FileTransferProgress> filesTransferWorker;
    boolean cancellingFileTransfer;
    String pendingPowerActionSerial;
    long pendingPowerActionUntilMs;
}
