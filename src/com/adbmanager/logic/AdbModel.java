package com.adbmanager.logic;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.adbmanager.logic.model.AppDetails;
import com.adbmanager.logic.model.AppBackgroundMode;
import com.adbmanager.logic.model.AdbToolInfo;
import com.adbmanager.logic.model.AppInstallRequest;
import com.adbmanager.logic.model.AppInstallResult;
import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.DeviceDetails;
import com.adbmanager.logic.model.InstalledApp;
import com.adbmanager.logic.model.SystemState;

public interface AdbModel {
    void refreshDevices() throws Exception;
    List<Device> getDevices();
    Optional<Device> getSelectedDevice();
    Device selectDeviceByIndex(int index);
    Device selectDeviceBySerial(String serial);
    Optional<DeviceDetails> getSelectedDeviceDetails() throws Exception;
    AdbToolInfo getAdbToolInfo() throws Exception;
    byte[] captureSelectedDeviceScreenshot() throws Exception;
    List<InstalledApp> getSelectedDeviceApplications() throws Exception;
    InstalledApp getSelectedDeviceApplicationSummary(String packageName) throws Exception;
    AppDetails getSelectedDeviceApplicationDetails(String packageName) throws Exception;
    void setSelectedDeviceApplicationPermission(String packageName, String permission, boolean granted) throws Exception;
    void setSelectedDeviceApplicationBackgroundMode(String packageName, AppBackgroundMode mode) throws Exception;
    void openSelectedDeviceApplication(String packageName) throws Exception;
    void stopSelectedDeviceApplication(String packageName) throws Exception;
    void uninstallSelectedDeviceApplication(String packageName) throws Exception;
    void setSelectedDeviceApplicationEnabled(String packageName, boolean enabled) throws Exception;
    void clearSelectedDeviceApplicationData(String packageName) throws Exception;
    void clearSelectedDeviceApplicationCache(String packageName) throws Exception;
    void exportSelectedDeviceApplicationApk(String packageName, File outputFile) throws Exception;
    AppInstallResult installSelectedDevicePackages(AppInstallRequest request, Consumer<String> progressCallback) throws Exception;
    Optional<SystemState> getSelectedDeviceSystemState() throws Exception;
    void createSelectedDeviceUser(String name) throws Exception;
    void removeSelectedDeviceUser(int userId) throws Exception;
    void switchSelectedDeviceUser(int userId) throws Exception;
    void setSelectedDeviceShowAllAppLanguages(boolean enabled) throws Exception;
    void setSelectedDeviceGesturalNavigation(boolean enabled) throws Exception;
    void enableSelectedDeviceKeyboard(String keyboardId) throws Exception;
    void setSelectedDeviceKeyboardEnabled(String keyboardId, boolean enabled) throws Exception;
    void setSelectedDeviceKeyboard(String keyboardId) throws Exception;
    void setSelectedDeviceDisplay(int widthPx, int heightPx, int densityDpi) throws Exception;
    void setSelectedDeviceScreenOffTimeout(int timeoutMs) throws Exception;
    void resetSelectedDeviceDisplay() throws Exception;
    void setSelectedDeviceDarkMode(boolean enabled) throws Exception;
    void pairWirelessDevice(String host, int pairingPort, String pairingCode) throws Exception;
    void pairWirelessDeviceWithQr(String serviceName, String password, int timeoutSeconds) throws Exception;
    void connectWirelessDevice(String host, int port) throws Exception;
}
