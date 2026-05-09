package com.meshrelief.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.meshrelief.R;
import com.meshrelief.core.model.Packet;
import com.meshrelief.core.p2p.Peer;
import com.meshrelief.core.p2p.PeerManager;
import com.meshrelief.core.transport.Transport;
import com.meshrelief.core.transport.TransportManager;
import com.meshrelief.core.transport.WifiDirectTransport;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends Activity implements WifiDirectTransport.PacketListener {
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private static final int REQUEST_WIFI_PERMISSION = 1002;
    private static final long PEER_UPDATE_INTERVAL = 3000; // Update UI every 3 seconds

    private TextView statusText;
    private ListView messageList;
    private EditText messageInput;
    private Button sendButton;
    private Button peerListButton;
    private Button networkMapButton;
    private Button refreshButton;

    private ArrayAdapter<String> messageAdapter;
    private List<String> messages = new ArrayList<>();

    private PeerManager peerManager;
    private WifiDirectTransport transport;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private Handler peerUpdateHandler;
    private boolean isActivityRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize views
        statusText = findViewById(R.id.statusText);
        messageList = findViewById(R.id.messageList);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        peerListButton = findViewById(R.id.peerListButton);
        networkMapButton = findViewById(R.id.networkMapButton);
        refreshButton = findViewById(R.id.refreshButton);

        // Setup message list
        messageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        messageList.setAdapter(messageAdapter);

        peerUpdateHandler = new Handler(Looper.getMainLooper());

        // Check permissions before initializing WiFi Direct
        if (checkPermissions()) {
            initializeWiFiDirect();
        }

        // Setup button listeners
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        peerListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChatActivity.this, PeerListActivity.class));
            }
        });

        networkMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChatActivity.this, NetworkMapActivity.class));
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshPeerStatus();
            }
        });

        addMessage("MeshRelief Chat Started");
        updateStatus("Initializing WiFi Direct...");
        isActivityRunning = true;
    }

    private void initializeWiFiDirect() {
        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        if (wifiP2pManager == null) {
            updateStatus("WiFi Direct not supported");
            return;
        }

        channel = wifiP2pManager.initialize(this, getMainLooper(), null);
        
        try {
            // Use singleton TransportManager for shared discovery across all activities
            TransportManager transportManager = TransportManager.getInstance();
            
            // Initialize only if not already initialized
            if (!transportManager.isTransportRunning()) {
                transportManager.initialize(this, wifiP2pManager, channel);
                transportManager.startTransport();
            }
            
            transport = transportManager.getTransport();
            peerManager = transportManager.getPeerManager();
            transport.setPacketListener(this);
            
            updateStatus("WiFi Direct transport started - Discovering peers...");

            // Start periodic peer updates
            startPeerStatusUpdates();
        } catch (Exception e) {
            updateStatus("Failed to start transport: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startPeerStatusUpdates() {
        peerUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isActivityRunning && transport != null) {
                    refreshPeerStatus();
                    peerUpdateHandler.postDelayed(this, PEER_UPDATE_INTERVAL);
                }
            }
        }, PEER_UPDATE_INTERVAL);
    }

    private void refreshPeerStatus() {
        if (transport == null) {
            return;
        }

        try {
            List<Peer> discoveredPeers = transport.getDiscoveredPeers();
            List<Peer> connectedPeers = transport.getConnectedPeers();

            String statusMsg = "WiFi Direct transport started - Discovering peers...\n" +
                    "Discovered: " + discoveredPeers.size() + " | Connected: " + connectedPeers.size();

            if (!discoveredPeers.isEmpty()) {
                statusMsg += "\nPeers: ";
                for (Peer p : discoveredPeers) {
                    statusMsg += p.getName() + ", ";
                }
            }

            updateStatus(statusMsg);
        } catch (Exception e) {
            System.err.println("Error refreshing peer status: " + e.getMessage());
        }
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        // Get first available peer
        List<Peer> peers = transport.getConnectedPeers();
        if (peers.isEmpty()) {
            Toast.makeText(this, "No connected peers", Toast.LENGTH_SHORT).show();
            return;
        }

        Peer targetPeer = peers.get(0);
        Packet packet = new Packet(
            "msg-" + System.currentTimeMillis(),
            "local-device", // TODO: get actual device ID
            targetPeer.getId(),
            10, // TTL
            System.currentTimeMillis(),
            text.getBytes()
        );

        transport.send(packet, targetPeer);
        addMessage("Sent: " + text);
        messageInput.setText("");
    }

    @Override
    public void onPacketReceived(Packet packet, String fromPeerId) {
        String message = new String(packet.getPayload());
        runOnUiThread(() -> addMessage("From " + fromPeerId + ": " + message));
    }

    private void addMessage(String message) {
        messages.add(message);
        messageAdapter.notifyDataSetChanged();
        messageList.setSelection(messages.size() - 1);
    }

    private void updateStatus(String status) {
        runOnUiThread(() -> statusText.setText("Status: " + status));
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityRunning = false;
        peerUpdateHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityRunning = true;
        startPeerStatusUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityRunning = false;
        peerUpdateHandler.removeCallbacksAndMessages(null);
        if (transport != null) {
            transport.stop();
        }
    }

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
                initializeWiFiDirect();
            } else {
                updateStatus("Permissions denied - WiFi Direct cannot start");
                Toast.makeText(this, "Location and WiFi permissions are required for WiFi Direct", Toast.LENGTH_LONG).show();
            }
        }
    }
}
