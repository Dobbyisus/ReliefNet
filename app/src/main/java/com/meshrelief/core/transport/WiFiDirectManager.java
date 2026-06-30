package com.meshrelief.core.transport;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import com.meshrelief.core.connection.ConnectionHandler;
import com.meshrelief.core.connection.MessageListener;
import com.meshrelief.core.mesh.PacketSender;
import com.meshrelief.core.model.Packet;

public class WiFiDirectManager
        implements WifiP2pManager.ConnectionInfoListener, PacketSender {

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final Context context;
    private final ConnectionHandler connectionHandler;

    public WiFiDirectManager(Context context, MessageListener messageListener) {
        this.context = context;
        this.manager = (WifiP2pManager)
                context.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = manager.initialize(
                context,
                context.getMainLooper(),
                null
        );
        this.connectionHandler = new ConnectionHandler(messageListener);
    }

    public void discoverPeers() {
        discoverPeers(new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {}

            @Override
            public void onFailure(int reason) {}
        });
    }

    public void discoverPeers(WifiP2pManager.ActionListener listener) {
        if (!hasRequiredPermission()) {
            listener.onFailure(-1);
            return;
        }

        manager.discoverPeers(channel, listener);
    }

    public void connect(WifiP2pDevice device) {
        connect(device, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {}

            @Override
            public void onFailure(int reason) {}
        });
    }

    public void connect(WifiP2pDevice device, WifiP2pManager.ActionListener listener) {
        if (!hasRequiredPermission() || device == null) {
            listener.onFailure(-1);
            return;
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        manager.connect(channel, config, listener);
    }

    public void createGroup(WifiP2pManager.ActionListener listener) {
        if (!hasRequiredPermission()) {
            listener.onFailure(-1);
            return;
        }

        manager.createGroup(channel, listener);
    }

    public void requestConnectionInfo() {
        manager.requestConnectionInfo(channel, this);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        connectionHandler.initialize(info);
    }

    public void disconnect() {
        connectionHandler.cleanup();
        manager.removeGroup(
                channel,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {}

                    @Override
                    public void onFailure(int reason) {}
                }
        );
    }

    public void handleGroupDisconnected() {
        connectionHandler.cleanup();
    }

    public WifiP2pManager getManager() {
        return manager;
    }

    public WifiP2pManager.Channel getChannel() {
        return channel;
    }

    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    @Override
    public void sendPacket(Packet packet) {
        connectionHandler.sendPacket(packet);
    }

    @Override
    public void sendToNode(Packet packet, String nodeId) {
        connectionHandler.sendToNode(packet, nodeId);
    }

    @Override
    public void broadcastToGroup(Packet packet, String excludeNodeId) {
        connectionHandler.broadcastToGroup(packet, excludeNodeId);
    }

    public boolean isConnectionReady() {
        return connectionHandler.isReady();
    }

    @Override
    public boolean isReady() {
        return isConnectionReady();
    }

    private boolean hasRequiredPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.NEARBY_WIFI_DEVICES)
                    == PackageManager.PERMISSION_GRANTED;
        }

        return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
}
