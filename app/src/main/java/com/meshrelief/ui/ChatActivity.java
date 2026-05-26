package com.meshrelief.ui;

import android.app.Activity;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;

import com.meshrelief.core.transport.WiFiDirectBroadcastReceiver;
import com.meshrelief.core.transport.WiFiDirectManager;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;



public class ChatActivity extends Activity {

    private void requestPermissionsIfNeeded() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.NEARBY_WIFI_DEVICES
                        },
                        100
                );
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    101
            );
        }
    }

    private WiFiDirectManager wifiDirectManager;

    private WiFiDirectBroadcastReceiver receiver;

    private IntentFilter intentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Initialize WiFi Direct manager
        wifiDirectManager =
                new WiFiDirectManager(this);

        // Create intent filter
        intentFilter = new IntentFilter();

        // Add WiFi Direct actions
        intentFilter.addAction(
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION
        );

        intentFilter.addAction(
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
        );

        intentFilter.addAction(
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
        );

        // Create receiver
        receiver =
                new WiFiDirectBroadcastReceiver(
                        wifiDirectManager.getManager(),
                        wifiDirectManager.getChannel()
                );

        // Register receiver
        registerReceiver(receiver, intentFilter);

        requestPermissionsIfNeeded();
        // Start peer discovery
        wifiDirectManager.discoverPeers();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        unregisterReceiver(receiver);
    }
}