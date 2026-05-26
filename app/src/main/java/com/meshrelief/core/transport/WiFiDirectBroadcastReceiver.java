package com.meshrelief.core.transport;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;

import android.net.NetworkInfo;

import android.net.wifi.p2p.WifiP2pManager;

import androidx.core.app.ActivityCompat;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    public WiFiDirectBroadcastReceiver(
            WifiP2pManager manager,
            WifiP2pManager.Channel channel) {

        this.manager = manager;
        this.channel = channel;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            int state = intent.getIntExtra(
                    WifiP2pManager.EXTRA_WIFI_STATE,
                    -1
            );

            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {

                System.out.println("WiFi Direct enabled");

            } else {

                System.out.println("WiFi Direct disabled");
            }
        }

        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                return;
            }

            manager.requestPeers(channel, peers -> {

                System.out.println("Peers discovered:");

                for (WifiP2pDevice device : peers.getDeviceList()) {

                    System.out.println(device.deviceName);
                }
            });
        }

        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            NetworkInfo networkInfo = intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_NETWORK_INFO
            );

            if (networkInfo != null && networkInfo.isConnected()) {

                System.out.println("Connected to peer");

            } else {

                System.out.println("Disconnected from peer");
            }
        }

    }
}

