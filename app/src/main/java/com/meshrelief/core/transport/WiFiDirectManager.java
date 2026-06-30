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

public class WiFiDirectManager
        implements WifiP2pManager.ConnectionInfoListener {

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private Context context;
    private ConnectionHandler connectionHandler;
    private MessageListener messageListener;

    public WiFiDirectManager(Context context, MessageListener messageListener) {

        this.context = context;
        this.messageListener = messageListener;

        manager = (WifiP2pManager)
                context.getSystemService(
                        Context.WIFI_P2P_SERVICE
                );

        channel = manager.initialize(
                context,
                context.getMainLooper(),
                null
        );

        this.connectionHandler = new ConnectionHandler(messageListener);
    }

    public void discoverPeers() {
        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.TIRAMISU) {

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }

        } else {

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        manager.discoverPeers(
                channel,
                new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {}

                    @Override
                    public void onFailure(int reason) {}
                }
        );
    }

    public void connect(WifiP2pDevice device) {
        boolean permissionGranted;

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.TIRAMISU) {

            permissionGranted =
                    ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.NEARBY_WIFI_DEVICES)
                            == PackageManager.PERMISSION_GRANTED;

        } else {

            permissionGranted =
                    ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;
        }

        if (!permissionGranted) {
            return;
        }

        if (device == null) {
            return;
        }

        WifiP2pConfig config =
                new WifiP2pConfig();

        config.deviceAddress =
                device.deviceAddress;

        manager.connect(
                channel,
                config,
                new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {}

                    @Override
                    public void onFailure(int reason) {}
                }
        );
    }

    public void requestConnectionInfo() {
        manager.requestConnectionInfo(
                channel,
                this
        );
    }

    @Override
    public void onConnectionInfoAvailable(
            WifiP2pInfo info) {
        // Initialize socket connection based on role
        if (connectionHandler != null) {
            connectionHandler.initialize(info);
        }
    }

    public void disconnect() {

        if (connectionHandler != null) {
            connectionHandler.cleanup();
        }

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

    public WifiP2pManager getManager() {
        return manager;
    }

    public WifiP2pManager.Channel getChannel() {
        return channel;
    }

    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    /**
     * Sends a message to the connected peer.
     *
     * @param message The message to send
     */
    public void sendMessage(String message) {
        if (connectionHandler != null) {
            connectionHandler.sendMessage(message);
        }
    }

    /**
     * Checks if a peer connection is established and ready.
     *
     * @return true if socket communication is ready
     */
    public boolean isConnectionReady() {
        return connectionHandler != null && connectionHandler.isReady();
    }
}
