package com.adbmanager.control;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingWorker;

import com.adbmanager.logic.model.AdbToolInfo;
import com.adbmanager.logic.model.WirelessEndpointDiscovery;
import com.adbmanager.logic.model.WirelessPairingResult;
import com.adbmanager.logic.model.WirelessPairingQrPayload;
import com.adbmanager.view.Messages;
import com.adbmanager.view.swing.MainFrame;
import com.adbmanager.view.swing.SimpleQrCodeGenerator;
import com.adbmanager.view.swing.WirelessConnectionDialog;

final class WirelessController {

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

        new SwingWorker<WirelessPairingResult, Void>() {
            @Override
            protected WirelessPairingResult doInBackground() throws Exception {
                return model().pairWirelessDevice(host, pairingPort, pairingCode);
            }

            @Override
            protected void done() {
                try {
                    WirelessPairingResult result = get();
                    applyPairingResult(dialog, result, Messages.text("wireless.status.paired"));
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

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                model().connectWirelessDevice(dialog.getConnectHost(), connectPort);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    dialog.showStatus(Messages.text("wireless.status.connected"), false);
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

        new SwingWorker<WirelessPairingResult, Void>() {
            @Override
            protected WirelessPairingResult doInBackground() throws Exception {
                return model().pairWirelessDeviceWithQr(
                        WirelessController.this.state().currentQrPayload.serviceName(),
                        WirelessController.this.state().currentQrPayload.password(),
                        45);
            }

            @Override
            protected void done() {
                try {
                    WirelessPairingResult result = get();
                    applyPairingResult(dialog, result, Messages.text("wireless.status.qrPaired"));
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
            WirelessPairingResult result,
            String connectedStatus) {
        if (result != null && result.hasConnectEndpoint()) {
            dialog.setConnectEndpoint(result.connectEndpoint().host(), result.connectEndpoint().port());
        }

        if (result != null && result.connectedAutomatically()) {
            dialog.showStatus(Messages.text("wireless.status.pairedConnected"), false);
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
