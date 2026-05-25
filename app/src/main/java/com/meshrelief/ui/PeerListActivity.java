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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.meshrelief.R;
import com.meshrelief.core.p2p.Peer;
import com.meshrelief.core.p2p.PeerManager;
import com.meshrelief.core.transport.TransportManager;
import com.meshrelief.core.transport.WifiDirectTransport;

import java.util.ArrayList;
import java.util.List;

public class PeerListActivity extends Activity {
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private TextView peerCountText;
    private ListView peerListView;
    private ArrayAdapter<String> peerAdapter;
    private List<String> peerNames = new ArrayList<>();
    private List<Peer> currentDiscoveredPeers = new ArrayList<>();
    private Handler updateHandler;
    private WifiDirectTransport transport;
    private PeerManager peerManager;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private boolean isActivityRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_list);

        peerCountText = findViewById(R.id.peerCountText);
        peerListView = findViewById(R.id.peerListView);
        updateHandler = new Handler(Looper.getMainLooper());

        peerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, peerNames);
        peerListView.setAdapter(peerAdapter);

        // Allow tapping a peer to attempt connection
        peerListView.setOnItemClickListener((parent, view, position, id) -> {
            if (transport == null) {
                peerCountText.setText("Transport not initialized");
                return;
            }

            if (position < 0 || position >= currentDiscoveredPeers.size()) {
                return;
            }

            Peer peer = currentDiscoveredPeers.get(position);
            try {
                transport.connectToPeer(peer.getId());
                peerCountText.setText("Connecting to: " + peer.getName());
            } catch (Exception e) {
                peerCountText.setText("Failed to connect: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Ensure required runtime permissions are present before using WiFi Direct
        if (!checkPermissions()) {
            // Permission request started; onRequestPermissionsResult will re-acquire transport when granted
            return;
        }

        // Get transport from singleton TransportManager (shared across all activities)
        try {
            TransportManager transportManager = TransportManager.getInstance();
            transport = transportManager.getTransport();
            peerManager = transportManager.getPeerManager();
            
            if (transport == null || peerManager == null) {
                peerCountText.setText("Error: Transport not initialized in ChatActivity");
            }
        } catch (Exception e) {
            peerCountText.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }

        isActivityRunning = true;
        startPeerUpdates();
    }

    private void startPeerUpdates() {
        updateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isActivityRunning) {
                    updatePeerList();
                    updateHandler.postDelayed(this, 2000); // Update every 2 seconds
                }
            }
        }, 2000);
    }

    private void updatePeerList() {
        if (transport == null) {
            peerCountText.setText("Peer List: Transport not initialized");
            return;
        }

        try {
            List<Peer> discoveredPeers = transport.getDiscoveredPeers();
            List<Peer> connectedPeers = transport.getConnectedPeers();

            peerNames.clear();
            currentDiscoveredPeers.clear();
            for (Peer peer : discoveredPeers) {
                String status = connectedPeers.contains(peer) ? " [CONNECTED]" : " [DISCOVERED]";
                peerNames.add(peer.getName() + " (" + peer.getId() + ")" + status);
                currentDiscoveredPeers.add(peer);
            }

            peerAdapter.notifyDataSetChanged();
            peerCountText.setText("Discovered Peers: " + discoveredPeers.size() + " | Connected: " + connectedPeers.size());

        } catch (Exception e) {
            peerCountText.setText("Error updating peers: " + e.getMessage());
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

        startPeerUpdates();
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
                    startPeerUpdates();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                peerCountText.setText("Permissions denied - cannot use WiFi Direct");
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
