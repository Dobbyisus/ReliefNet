package com.meshrelief.core.transport;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WiFiDirectManager wifiDirectManager;
    private PeerDiscoveryListener listener;

    public WiFiDirectBroadcastReceiver(
            WifiP2pManager manager,
            WifiP2pManager.Channel channel,
            WiFiDirectManager wifiDirectManager,
            PeerDiscoveryListener listener) {

        this.manager = manager;
        this.channel = channel;
        this.wifiDirectManager = wifiDirectManager;
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION
                .equals(action)) {

            int state = intent.getIntExtra(
                    WifiP2pManager.EXTRA_WIFI_STATE,
                    -1
            );
            if (state ==
                    WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                return;
            }
        }

        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
                .equals(action)) {

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

            manager.requestPeers(
                    channel,
                    peers -> {
                        if (listener != null) {
                            listener.onPeersDiscovered(
                                    peers.getDeviceList()
                            );
                        }
                    }
            );
        }

        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
                .equals(action)) {

            NetworkInfo networkInfo =
                    intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_NETWORK_INFO
                    );

            if (networkInfo != null
                    && networkInfo.isConnected()) {
                wifiDirectManager.requestConnectionInfo();
            }
        }
    }
}
