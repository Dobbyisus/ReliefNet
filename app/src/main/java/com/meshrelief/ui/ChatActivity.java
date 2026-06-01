package com.meshrelief.ui;

import android.app.Activity;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;

import androidx.core.app.ActivityCompat;

import com.meshrelief.core.transport.WiFiDirectBroadcastReceiver;
import com.meshrelief.core.transport.WiFiDirectManager;

public class ChatActivity extends Activity {

    private static final int REQUEST_NEARBY_WIFI = 100;
    private static final int REQUEST_LOCATION = 101;

    private WiFiDirectManager wifiDirectManager;
    private WiFiDirectBroadcastReceiver receiver;
    private IntentFilter intentFilter;

    private void requestPermissionsIfNeeded() {

        System.out.println("requestPermissionsIfNeeded entered");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            boolean nearbyGranted =
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.NEARBY_WIFI_DEVICES)
                            == PackageManager.PERMISSION_GRANTED;

            System.out.println(
                    "NEARBY_WIFI_DEVICES granted = "
                            + nearbyGranted);

            if (!nearbyGranted) {

                System.out.println(
                        "Requesting NEARBY_WIFI_DEVICES permission");

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.NEARBY_WIFI_DEVICES
                        },
                        REQUEST_NEARBY_WIFI
                );
            }

        } else {

            boolean locationGranted =
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;

            System.out.println(
                    "ACCESS_FINE_LOCATION granted = "
                            + locationGranted);

            if (!locationGranted) {

                System.out.println(
                        "Requesting ACCESS_FINE_LOCATION permission");

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        REQUEST_LOCATION
                );
            }
        }

        System.out.println("requestPermissionsIfNeeded exited");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        System.out.println("ChatActivity onCreate");

        // Initialize WiFi Direct manager
        wifiDirectManager =
                new WiFiDirectManager(this);

        // Create intent filter
        intentFilter = new IntentFilter();

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

        registerReceiver(receiver, intentFilter);

        System.out.println("Receiver registered");

        requestPermissionsIfNeeded();

        // Handle already-granted permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NEARBY_WIFI_DEVICES)
                    == PackageManager.PERMISSION_GRANTED) {

                System.out.println(
                        "Permission already granted - starting discovery");

                wifiDirectManager.discoverPeers();
            }

        } else {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                System.out.println(
                        "Permission already granted - starting discovery");

                wifiDirectManager.discoverPeers();
            }
        }

        System.out.println("ON CREATE FINISHED");
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        System.out.println(
                "onRequestPermissionsResult called");

        boolean allGranted = true;

        for (int result : grantResults) {

            if (result != PackageManager.PERMISSION_GRANTED) {

                allGranted = false;

                System.out.println(
                        "Permission denied: " + result);

                break;
            }
        }

        if (allGranted) {

            System.out.println(
                    "Permissions granted - starting discovery");

            wifiDirectManager.discoverPeers();
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        unregisterReceiver(receiver);

        System.out.println("Receiver unregistered");
    }
}