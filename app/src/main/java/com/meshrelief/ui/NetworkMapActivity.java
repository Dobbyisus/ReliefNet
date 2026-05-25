package com.meshrelief.ui;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.meshrelief.R;
import com.meshrelief.core.p2p.Peer;
import com.meshrelief.core.p2p.PeerManager;
import com.meshrelief.core.transport.TransportManager;
import com.meshrelief.core.transport.WifiDirectTransport;

import java.util.List;
import java.util.ArrayList;

public class NetworkMapActivity extends Activity {
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private TextView networkMapInfo;
    private Handler updateHandler;
    private WifiDirectTransport transport;
    private PeerManager peerManager;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private boolean isActivityRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_map);

        networkMapInfo = findViewById(R.id.networkMapInfo);
        updateHandler = new Handler(Looper.getMainLooper());

        // Ensure required runtime permissions are present before using WiFi Direct
        if (!checkPermissions()) {
            // permission request started; result handled in onRequestPermissionsResult
            return;
        }

        // Get transport from singleton TransportManager (shared across all activities)
        try {
            TransportManager transportManager = TransportManager.getInstance();
            transport = transportManager.getTransport();
            peerManager = transportManager.getPeerManager();
            
            if (transport == null || peerManager == null) {
                networkMapInfo.setText("Error: Transport not initialized in ChatActivity");
            }
        } catch (Exception e) {
            networkMapInfo.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }

        isActivityRunning = true;
        startNetworkUpdates();
    }

    private void startNetworkUpdates() {
        updateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isActivityRunning) {
                    updateNetworkMap();
                    updateHandler.postDelayed(this, 3000); // Update every 3 seconds
                }
            }
        }, 1000);
    }

    private void updateNetworkMap() {
        if (transport == null) {
            networkMapInfo.setText("Transport not initialized");
            return;
        }

        try {
            List<Peer> discoveredPeers = transport.getDiscoveredPeers();
            List<Peer> connectedPeers = transport.getConnectedPeers();

            StringBuilder sb = new StringBuilder();
            sb.append("=== Network Topology ===\n\n");
            sb.append("Discovered Peers: ").append(discoveredPeers.size()).append("\n");
            sb.append("Connected Peers: ").append(connectedPeers.size()).append("\n\n");

            sb.append("--- Discovered Peers ---\n");
            for (Peer peer : discoveredPeers) {
                String status = connectedPeers.contains(peer) ? "[Connected]" : "[Discovered]";
                sb.append("• ").append(peer.getName()).append("\n");
                sb.append("  ID: ").append(peer.getId()).append("\n");
                sb.append("  Status: ").append(status).append("\n");
                sb.append("  Last Seen: ").append(peer.getLastSeen()).append("\n\n");
            }

            if (discoveredPeers.isEmpty()) {
                sb.append("No peers discovered yet. Make sure other devices are nearby with WiFi Direct enabled.\n");
            }

            networkMapInfo.setText(sb.toString());

        } catch (Exception e) {
            networkMapInfo.setText("Error updating network map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityRunning = false;
        updateHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityRunning = true;
        // Re-acquire shared transport in case it was initialized after this activity was created
        try {
            TransportManager transportManager = TransportManager.getInstance();
            if (transport == null) {
                transport = transportManager.getTransport();
                peerManager = transportManager.getPeerManager();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        startNetworkUpdates();
    }

    /**
     * Request and check runtime permissions required for WiFi Direct.
     */
    private boolean checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CHANGE_WIFI_STATE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_WIFI_STATE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.INTERNET);
        }

        // Android 13+ requires NEARBY_WIFI_DEVICES permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_LOCATION_PERMISSION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                // Re-acquire transport now that permissions are available
                try {
                    TransportManager transportManager = TransportManager.getInstance();
                    if (transport == null) {
                        transport = transportManager.getTransport();
                        peerManager = transportManager.getPeerManager();
                    }
                    startNetworkUpdates();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                networkMapInfo.setText("Permissions denied - cannot use WiFi Direct");
                Toast.makeText(this, "Location and WiFi permissions are required for WiFi Direct", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityRunning = false;
        updateHandler.removeCallbacksAndMessages(null);
    }
}
