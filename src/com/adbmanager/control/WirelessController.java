package com.adbmanager.control;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingWorker;

import com.adbmanager.logic.model.AdbToolInfo;
import com.adbmanager.logic.model.Device;
import com.adbmanager.logic.model.WirelessEndpointDiscovery;
import com.adbmanager.logic.model.WirelessPairingResult;
import com.adbmanager.logic.model.WirelessPairingQrPayload;
import com.adbmanager.view.Messages;
import com.adbmanager.view.swing.MainFrame;
import com.adbmanager.view.swing.SimpleQrCodeGenerator;
import com.adbmanager.view.swing.WirelessConnectionDialog;

final class WirelessController {

    private record PairingCompletion(WirelessPairingResult result, Device connectedDevice) {
    }

    private final SwingControllerContext context;
    private final Runnable refreshDevices;

    WirelessController(SwingControllerContext context, Runnable refreshDevices) {
        this.context = context;
        this.refreshDevices = refreshDevices;
    }

    void openAssistant() {
        WirelessConnectionDialog dialog = view().getWirelessConnectionDialog();
        state().currentQrPayload = null;
        dialog.clearQrPayload();
        dialog.resetSessionFields();
        dialog.setBusy(false);
        dialog.showStatus(Messages.text("wireless.status.loading"), false);
        dialog.open();
        startEndpointDiscovery();

        new SwingWorker<AdbToolInfo, Void>() {
            @Override
            protected AdbToolInfo doInBackground() throws Exception {
                return model().getAdbToolInfo();
            }

            @Override
            protected void done() {
                try {
                    AdbToolInfo toolInfo = get();
                    dialog.setToolInfo(toolInfo);
                    dialog.showStatus(Messages.text("wireless.status.ready"), false);
                } catch (Exception exception) {
                    dialog.setToolInfo(new AdbToolInfo("-", "-", false, false));
                    dialog.showStatus(Messages.text("wireless.status.capabilitiesError"), true);
                }
            }
        }.execute();
    }

    void pairDeviceByCode() {
        WirelessConnectionDialog dialog = view().getWirelessConnectionDialog();
        Integer pairingPort = dialog.getPairPort();
        String host = dialog.getPairHost();
        String pairingCode = dialog.getPairCode();

        if (pairingPort == null) {
            dialog.showStatus(Messages.text("error.wireless.invalidPort"), true);
            return;
        }

        dialog.showStatus(Messages.text("wireless.status.pairing"), false);
        dialog.setBusy(true);

        new SwingWorker<PairingCompletion, Void>() {
            @Override
            protected PairingCompletion doInBackground() throws Exception {
                Set<String> knownWirelessSerials = connectedWirelessSerials();
                WirelessPairingResult result = model().pairWirelessDevice(host, pairingPort, pairingCode);
                return new PairingCompletion(
                        result,
                        waitForConnectedWirelessDevice(knownWirelessSerials, result));
            }

            @Override
            protected void done() {
                try {
                    PairingCompletion completion = get();
                    applyPairingResult(dialog, completion, Messages.text("wireless.status.paired"));
                } catch (Exception exception) {
                    dialog.showStatus(
                            context.extractErrorMessage(exception, Messages.text("error.wireless.pair")),
                            true);
                } finally {
                    dialog.setBusy(false);
                }
            }
        }.execute();
    }

    void connectWirelessDevice() {
        WirelessConnectionDialog dialog = view().getWirelessConnectionDialog();
        Integer connectPort = dialog.getConnectPort();
        if (connectPort == null) {
            dialog.showStatus(Messages.text("error.wireless.invalidConnectPort"), true);
            return;
        }

        dialog.showStatus(Messages.text("wireless.status.connecting"), false);
        dialog.setBusy(true);

        new SwingWorker<Device, Void>() {
            @Override
            protected Device doInBackground() throws Exception {
                Set<String> knownWirelessSerials = connectedWirelessSerials();
                model().connectWirelessDevice(dialog.getConnectHost(), connectPort);
                return waitForConnectedWirelessDevice(knownWirelessSerials, null);
            }

            @Override
            protected void done() {
                try {
                    Device connectedDevice = get();
                    dialog.showStatus(Messages.text("wireless.status.connected"), false);
                    closeDialogAfterWirelessConnection(dialog, connectedDevice);
                    refreshDevices.run();
                } catch (Exception exception) {
                    dialog.showStatus(
                            context.extractErrorMessage(exception, Messages.text("error.wireless.connect")),
                            true);
                } finally {
                    dialog.setBusy(false);
                }
            }
        }.execute();
    }

    void connectSelectedUsbDeviceOverTcpip() {
        view().setTcpipEnabled(false);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return model().connectSelectedUsbDeviceOverTcpip(5555);
            }

            @Override
            protected void done() {
                try {
                    String endpoint = get();
                    view().showInfo(Messages.format("wireless.status.tcpipConnected", endpoint));
                    refreshDevices.run();
                } catch (Exception exception) {
                    context.handleError(Messages.text("error.wireless.tcpip"), exception);
                } finally {
                    com.adbmanager.logic.model.Device selectedDevice = model().getSelectedDevice().orElse(null);
                    view().setTcpipEnabled(context.isTcpipAvailable(selectedDevice));
                }
            }
        }.execute();
    }

    void generateQrPayload() {
        WirelessConnectionDialog dialog = view().getWirelessConnectionDialog();
        try {
            state().currentQrPayload = WirelessPairingQrPayload.random();
            dialog.setQrPayload(
                    state().currentQrPayload,
                    SimpleQrCodeGenerator.generate(state().currentQrPayload.qrPayload(), 7, 5));
            dialog.showStatus(Messages.text("wireless.status.qrGenerated"), false);
        } catch (Exception exception) {
            dialog.showStatus(
                    context.extractErrorMessage(exception, Messages.text("error.wireless.qrGenerate")),
                    true);
        }
    }

    void pairDeviceByQr() {
        WirelessConnectionDialog dialog = view().getWirelessConnectionDialog();
        if (state().currentQrPayload == null) {
            dialog.showStatus(Messages.text("error.wireless.qrPayload"), true);
            return;
        }

        dialog.showStatus(Messages.text("wireless.status.waitingForQr"), false);
        dialog.setBusy(true);

        new SwingWorker<PairingCompletion, Void>() {
            @Override
            protected PairingCompletion doInBackground() throws Exception {
                Set<String> knownWirelessSerials = connectedWirelessSerials();
                WirelessPairingResult result = model().pairWirelessDeviceWithQr(
                        WirelessController.this.state().currentQrPayload.serviceName(),
                        WirelessController.this.state().currentQrPayload.password(),
                        45);
                return new PairingCompletion(
                        result,
                        waitForConnectedWirelessDevice(knownWirelessSerials, result));
            }

            @Override
            protected void done() {
                try {
                    PairingCompletion completion = get();
                    applyPairingResult(dialog, completion, Messages.text("wireless.status.qrPaired"));
                } catch (Exception exception) {
                    dialog.showStatus(
                            context.extractErrorMessage(exception, Messages.text("error.wireless.qrPair")),
                            true);
                } finally {
                    dialog.setBusy(false);
                }
            }
        }.execute();
    }

    void cancelEndpointDiscovery() {
        if (state().wirelessEndpointDiscoveryWorker != null) {
            state().wirelessEndpointDiscoveryWorker.cancel(true);
            state().wirelessEndpointDiscoveryWorker = null;
        }
    }

    private void applyPairingResult(
            WirelessConnectionDialog dialog,
            PairingCompletion completion,
            String connectedStatus) {
        WirelessPairingResult result = completion == null ? null : completion.result();
        if (result != null && result.hasConnectEndpoint()) {
            dialog.setConnectEndpoint(result.connectEndpoint().host(), result.connectEndpoint().port());
        }

        if ((result != null && result.connectedAutomatically())
                || (completion != null && completion.connectedDevice() != null)) {
            dialog.showStatus(Messages.text("wireless.status.pairedConnected"), false);
            closeDialogAfterWirelessConnection(dialog, completion == null ? null : completion.connectedDevice());
            refreshDevices.run();
            return;
        }

        if (result != null && result.hasConnectEndpoint()) {
            dialog.showStatus(Messages.format(
                    "wireless.status.pairedManualConnect",
                    result.connectEndpoint().endpoint()), false);
            return;
        }

        dialog.showStatus(connectedStatus, false);
    }

    private Set<String> connectedWirelessSerials() {
        Set<String> serials = new HashSet<>();
        for (Device device : model().getDevices()) {
            if (isConnectedWirelessDevice(device)) {
                serials.add(device.serial());
            }
        }
        return serials;
    }

    private Device waitForConnectedWirelessDevice(
            Set<String> knownWirelessSerials,
            WirelessPairingResult pairingResult) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
        while (System.nanoTime() < deadline) {
            model().refreshDevices();

            Device preferredDevice = findPreferredConnectedWirelessDevice(pairingResult);
            if (preferredDevice != null) {
                selectDevice(preferredDevice);
                return preferredDevice;
            }

            for (Device device : model().getDevices()) {
                if (isConnectedWirelessDevice(device)
                        && (knownWirelessSerials == null || !knownWirelessSerials.contains(device.serial()))) {
                    selectDevice(device);
                    return device;
                }
            }

            Thread.sleep(1000L);
        }
        return null;
    }

    private Device findPreferredConnectedWirelessDevice(WirelessPairingResult pairingResult) {
        if (pairingResult == null || !pairingResult.hasConnectEndpoint()) {
            return null;
        }

        String preferredHost = pairingResult.connectEndpoint().host();
        int preferredPort = pairingResult.connectEndpoint().port();
        String preferredEndpoint = pairingResult.connectEndpoint().endpoint();
        for (Device device : model().getDevices()) {
            if (!isConnectedWirelessDevice(device)) {
                continue;
            }

            String serial = device.serial() == null ? "" : device.serial();
            if (serial.equals(preferredEndpoint)
                    || serial.contains(preferredHost)
                    || (preferredPort > 0 && serial.contains(":" + preferredPort))) {
                return device;
            }
        }
        return null;
    }

    private void selectDevice(Device device) {
        if (device != null && device.serial() != null && !device.serial().isBlank()) {
            model().selectDeviceBySerial(device.serial());
        }
    }

    private boolean isConnectedWirelessDevice(Device device) {
        if (device == null || !Messages.STATUS_CONNECTED.equals(device.state())) {
            return false;
        }

        String serial = device.serial() == null ? "" : device.serial().trim();
        return serial.contains(":")
                || serial.contains("_adb")
                || serial.startsWith("adb-");
    }

    private void closeDialogAfterWirelessConnection(WirelessConnectionDialog dialog, Device connectedDevice) {
        if (connectedDevice != null) {
            selectDevice(connectedDevice);
        }
        cancelEndpointDiscovery();
        dialog.setVisible(false);
    }

    private void startEndpointDiscovery() {
        cancelEndpointDiscovery();
        WirelessConnectionDialog dialog = view().getWirelessConnectionDialog();

        state().wirelessEndpointDiscoveryWorker = new SwingWorker<>() {
            private WirelessEndpointDiscovery lastDiscovery = WirelessEndpointDiscovery.empty();

            @Override
            protected Void doInBackground() throws Exception {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                while (!isCancelled() && dialog.isVisible() && System.nanoTime() < deadline) {
                    try {
                        WirelessEndpointDiscovery discovery = model().discoverWirelessEndpoints();
                        if (!Objects.equals(discovery, lastDiscovery)) {
                            lastDiscovery = discovery;
                            publish(discovery);
                        }
                    } catch (Exception ignored) {
                    }

                    Thread.sleep(1500L);
                }
                return null;
            }

            @Override
            protected void process(List<WirelessEndpointDiscovery> chunks) {
                if (!dialog.isVisible() || chunks == null || chunks.isEmpty()) {
                    return;
                }

                WirelessEndpointDiscovery latest = chunks.get(chunks.size() - 1);
                if (latest == null) {
                    return;
                }

                if (latest.hasPairingEndpoint()) {
                    dialog.suggestPairEndpoint(latest.pairingEndpoint().host(), latest.pairingEndpoint().port());
                }
                if (latest.hasConnectEndpoint()) {
                    dialog.suggestConnectEndpoint(latest.connectEndpoint().host(), latest.connectEndpoint().port());
                }
            }

            @Override
            protected void done() {
                if (WirelessController.this.state().wirelessEndpointDiscoveryWorker == this) {
                    WirelessController.this.state().wirelessEndpointDiscoveryWorker = null;
                }
            }
        };

        state().wirelessEndpointDiscoveryWorker.execute();
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
