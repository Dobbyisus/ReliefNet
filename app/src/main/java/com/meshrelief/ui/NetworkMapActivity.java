package com.meshrelief.ui;

import android.app.Activity;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import com.meshrelief.R;
import com.meshrelief.core.p2p.Peer;
import com.meshrelief.core.p2p.PeerManager;
import com.meshrelief.core.transport.TransportManager;
import com.meshrelief.core.transport.WifiDirectTransport;

import java.util.List;

public class NetworkMapActivity extends Activity {
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
        startNetworkUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityRunning = false;
        updateHandler.removeCallbacksAndMessages(null);
        if (transport != null) {
            transport.stop();
        }
    }
}
