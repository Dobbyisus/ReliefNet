package com.meshrelief.ui;

import android.app.Activity;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.meshrelief.R;
import com.meshrelief.core.p2p.Peer;
import com.meshrelief.core.p2p.PeerManager;
import com.meshrelief.core.transport.TransportManager;
import com.meshrelief.core.transport.WifiDirectTransport;

import java.util.ArrayList;
import java.util.List;

public class PeerListActivity extends Activity {
    private TextView peerCountText;
    private ListView peerListView;
    private ArrayAdapter<String> peerAdapter;
    private List<String> peerNames = new ArrayList<>();
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
            for (Peer peer : discoveredPeers) {
                String status = connectedPeers.contains(peer) ? " [CONNECTED]" : " [DISCOVERED]";
                peerNames.add(peer.getName() + " (" + peer.getId() + ")" + status);
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
        startPeerUpdates();
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
