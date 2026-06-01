package com.meshrelief.core.transport;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pConfig;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
public class WiFiDirectManager {

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private Context context;

    public WiFiDirectManager(Context context) {
        this.context = context;

        manager = (WifiP2pManager)
                context.getSystemService(Context.WIFI_P2P_SERVICE);

        channel = manager.initialize(
                context,
                context.getMainLooper(),
                null
        );
    }

    public void discoverPeers() {
        System.out.println("DISCOVER PEERS ENTERED");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {

                System.out.println("Nearby WiFi permission not granted");
                return;
            }

        } else {

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                System.out.println("Location permission not granted");
                return;
            }
        }

        System.out.println(
                "Manager = " + (manager != null)
        );

        System.out.println(
                "Channel = " + (channel != null)
        );


        manager.discoverPeers(channel,
                new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        System.out.println("Peer discovery started");
                    }

                    @Override
                    public void onFailure(int reason) {
                        System.out.println("Peer discovery failed: " + reason);
                    }
                });
    }

    public void connect(WifiP2pDevice device) {

        System.out.println("CONNECT ENTERED");

        boolean permissionGranted;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

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

            System.out.println("Required permission not granted");
            return;
        }

        if (device == null) {

            System.out.println("Device is null");
            return;
        }

        System.out.println(
                "Attempting connection to: "
                        + device.deviceName
                        + " | "
                        + device.deviceAddress
        );

        WifiP2pConfig config = new WifiP2pConfig();

        config.deviceAddress = device.deviceAddress;

        manager.connect(
                channel,
                config,
                new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {

                        System.out.println(
                                "Connection request accepted"
                        );
                    }

                    @Override
                    public void onFailure(int reason) {

                        System.out.println(
                                "Connection failed: " + reason
                        );
                    }
                }
        );
    }

    public void disconnect() {

        manager.removeGroup(channel,
                new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        System.out.println("Disconnected");
                    }

                    @Override
                    public void onFailure(int reason) {
                        System.out.println("Disconnect failed: " + reason);
                    }
                });
    }

    public WifiP2pManager getManager() {
        return manager;
    }

    public WifiP2pManager.Channel getChannel() {
        return channel;
    }


}