package com.meshrelief.core.transport;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;

/**
 * WiFi Direct Broadcast Receiver.
 * Listens for WiFi Direct state changes and peer discovery events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private final WiFiDirectManager wifiDirectManager;
    private final WiFiDirectDiscovery discoveryManager;
    private final WiFiDirectConnectionManager connectionManager;
    private boolean isRegistered = false;

    /**
     * Default constructor for Android manifest instantiation.
     */
    public WiFiDirectBroadcastReceiver() {
        this.wifiDirectManager = null;
        this.discoveryManager = null;
        this.connectionManager = null;
    }

    /**
     * Creates a WiFiDirectBroadcastReceiver instance.
     *
     * @param wifiDirectManager the WiFi Direct manager
     * @param discoveryManager the discovery manager
     * @param connectionManager the connection manager
     */
    public WiFiDirectBroadcastReceiver(
            WiFiDirectManager wifiDirectManager,
            WiFiDirectDiscovery discoveryManager,
            WiFiDirectConnectionManager connectionManager) {
        if (wifiDirectManager == null || discoveryManager == null || connectionManager == null) {
            throw new IllegalArgumentException("All managers cannot be null");
        }
        this.wifiDirectManager = wifiDirectManager;
        this.discoveryManager = discoveryManager;
        this.connectionManager = connectionManager;
    }

    /**
     * Registers this receiver with the given context.
     *
     * @param context Android context
     */
    public void register(Context context) {
        if (isRegistered) {
            return; // Already registered
        }

        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

            // Use the appropriate flag for API 31+ (0x0000002 = RECEIVER_EXPORTED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+ - use context method
                context.registerReceiver(this, intentFilter, 0x0000002); // RECEIVER_EXPORTED
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31-32 - use int flag
                context.registerReceiver(this, intentFilter, 0x0000002); // RECEIVER_EXPORTED
            } else {
                // API 30 and below
                context.registerReceiver(this, intentFilter);
            }

            isRegistered = true;
            System.out.println("WiFiDirectBroadcastReceiver registered");

        } catch (Exception e) {
            System.err.println("Error registering receiver: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Unregisters this receiver from the given context.
     *
     * @param context Android context
     */
    public void unregister(Context context) {
        if (!isRegistered) {
            return;
        }

        try {
            context.unregisterReceiver(this);
            isRegistered = false;
            System.out.println("WiFiDirectBroadcastReceiver unregistered");

        } catch (Exception e) {
            System.err.println("Error unregistering receiver: " + e.getMessage());
        }
    }

    /**
     * Receives WiFi Direct broadcasts.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            handleWifiP2pStateChanged(intent);

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            handlePeersChanged(intent);

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            handleConnectionChanged(intent);

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            handleThisDeviceChanged(intent);
        }
    }

    /**
     * Handles WiFi P2P state change (enabled/disabled).
     */
    private void handleWifiP2pStateChanged(Intent intent) {
        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            System.out.println("WiFi Direct enabled");
            wifiDirectManager.setWifiP2pEnabled(true);

        } else if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
            System.out.println("WiFi Direct disabled");
            wifiDirectManager.setWifiP2pEnabled(false);
        }
    }

    /**
     * Handles peer list changes.
     */
    private void handlePeersChanged(Intent intent) {
        System.out.println("Peer list changed");
        discoveryManager.requestPeerList();
    }

    /**
     * Handles connection state changes.
     */
    private void handleConnectionChanged(Intent intent) {
        android.net.NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            System.out.println("WiFi Direct connected");
            // Connection established, update connection info
            requestConnectionInfo();

        } else {
            System.out.println("WiFi Direct disconnected");
        }
    }

    /**
     * Handles this device info changes.
     */
    private void handleThisDeviceChanged(Intent intent) {
        android.net.wifi.p2p.WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

        if (device != null) {
            String status = getDeviceStatus(device.status);
            System.out.println("This device changed - Name: " + device.deviceName + 
                    ", Address: " + device.deviceAddress + 
                    ", Status: " + status);
        }
    }

    /**
     * Requests current connection information.
     */
    private void requestConnectionInfo() {
        try {
            // Delegate to WiFiDirectManager which has access to WifiP2pManager and Channel
            System.out.println("Requesting connection information via WiFiDirectManager");
            wifiDirectManager.requestConnectionInfo();

        } catch (Exception e) {
            System.err.println("Error requesting connection info: " + e.getMessage());
        }
    }

    /**
     * Helper method to convert device status code to string.
     */
    private String getDeviceStatus(int status) {
        switch (status) {
            case android.net.wifi.p2p.WifiP2pDevice.CONNECTED:
                return "CONNECTED";
            case android.net.wifi.p2p.WifiP2pDevice.INVITED:
                return "INVITED";
            case android.net.wifi.p2p.WifiP2pDevice.FAILED:
                return "FAILED";
            case android.net.wifi.p2p.WifiP2pDevice.AVAILABLE:
                return "AVAILABLE";
            case android.net.wifi.p2p.WifiP2pDevice.UNAVAILABLE:
                return "UNAVAILABLE";
            default:
                return "UNKNOWN";
        }
    }
}
