package com.meshrelief.ui;

import android.app.Activity;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;

import androidx.core.app.ActivityCompat;

import com.meshrelief.R;
import com.meshrelief.core.connection.MessageListener;
import com.meshrelief.core.transport.WiFiDirectBroadcastReceiver;
import com.meshrelief.core.transport.WiFiDirectManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import com.meshrelief.core.transport.PeerDiscoveryListener;

import java.util.Collection;

public class ChatActivity extends Activity implements PeerDiscoveryListener, MessageListener {

    private static final int REQUEST_NEARBY_WIFI = 100;
    private static final int REQUEST_LOCATION = 101;

    private WiFiDirectManager wifiDirectManager;
    private WiFiDirectBroadcastReceiver receiver;
    private IntentFilter intentFilter;

    // Peer discovery UI
    private ListView peerListView;
    private ArrayAdapter<String> peerAdapter;
    private ArrayList<String> peerNames = new ArrayList<>();
    private ArrayList<WifiP2pDevice> peerDevices = new ArrayList<>();

    // Connection and messaging UI
    private TextView connectionStatusView;
    private EditText messageInputView;
    private Button sendButtonView;
    private ListView messagesListView;
    private ArrayAdapter<String> messagesAdapter;
    private ArrayList<String> messages = new ArrayList<>();

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
        setContentView(R.layout.activity_chat);

        // Peer discovery UI
        peerListView = findViewById(R.id.peerListView);
        peerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                peerNames
        );
        peerListView.setAdapter(peerAdapter);
        peerListView.setOnItemClickListener(
                (parent, view, position, id) -> {
                    WifiP2pDevice device = peerDevices.get(position);
                    System.out.println("Selected peer: " + device.deviceName);
                    wifiDirectManager.connect(device);
                }
        );

        // Connection and messaging UI
        connectionStatusView = findViewById(R.id.connectionStatus);
        messageInputView = findViewById(R.id.messageInput);
        sendButtonView = findViewById(R.id.sendButton);
        messagesListView = findViewById(R.id.messagesListView);

        messagesAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                messages
        );
        messagesListView.setAdapter(messagesAdapter);

        // Send button listener
        sendButtonView.setOnClickListener(v -> {
            String messageText = messageInputView.getText().toString().trim();
            if (!messageText.isEmpty()) {
                if (wifiDirectManager.isConnectionReady()) {
                    messages.add("You: " + messageText);
                    messages.add(
                            "[DEBUG] Ready = "
                                    + wifiDirectManager.isConnectionReady()
                    );
                    messagesAdapter.notifyDataSetChanged();
                    messagesListView.smoothScrollToPosition(
                            messages.size() - 1
                    );
                    wifiDirectManager.sendMessage(messageText);
                    messageInputView.setText("");
                } else {
                    Toast.makeText(this, "Not connected to peer", Toast.LENGTH_SHORT).show();
                }
            }
        });

        System.out.println("ChatActivity onCreate");

        // Initialize WiFi Direct manager with message listener
        wifiDirectManager = new WiFiDirectManager(this, this);

        // Create intent filter
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Create receiver
        receiver = new WiFiDirectBroadcastReceiver(
                wifiDirectManager.getManager(),
                wifiDirectManager.getChannel(),
                wifiDirectManager,
                this
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

                System.out.println("Permission already granted - starting discovery");
                wifiDirectManager.discoverPeers();
            }

        } else {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                System.out.println("Permission already granted - starting discovery");
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

        System.out.println("onRequestPermissionsResult called");

        boolean allGranted = true;

        for (int result : grantResults) {

            if (result != PackageManager.PERMISSION_GRANTED) {

                allGranted = false;
                System.out.println("Permission denied: " + result);
                break;
            }
        }

        if (allGranted) {

            System.out.println("Permissions granted - starting discovery");
            wifiDirectManager.discoverPeers();
        }
    }

    @Override
    public void onPeersDiscovered(Collection<WifiP2pDevice> peers) {

        runOnUiThread(() -> {

            peerNames.clear();
            peerDevices.clear();

            for (WifiP2pDevice device : peers) {

                peerNames.add(device.deviceName);
                peerDevices.add(device);
            }

            peerAdapter.notifyDataSetChanged();
        });
    }

    // MessageListener callbacks

    @Override
    public void onMessageReceived(String message) {
        runOnUiThread(() -> {
            messages.add("Peer: " + message);
            messagesAdapter.notifyDataSetChanged();
            messagesListView.smoothScrollToPosition(messages.size() - 1);
        });
    }

    @Override
    public void onConnectionEstablished() {
        runOnUiThread(() -> {
            connectionStatusView.setText("Status: Connected ✓");
            connectionStatusView.setTextColor(0xFF00AA00);
            Toast.makeText(this, "Connection established!", Toast.LENGTH_SHORT).show();
            messages.add("[System] Connected to peer");
            messagesAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onConnectionClosed() {
        runOnUiThread(() -> {
            connectionStatusView.setText("Status: Disconnected");
            connectionStatusView.setTextColor(0xFFCC0000);
            messages.add("[System] Connection closed");
            messagesAdapter.notifyDataSetChanged();
            messageInputView.setEnabled(false);
            sendButtonView.setEnabled(false);
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            connectionStatusView.setText("Status: Error - " + error);
            connectionStatusView.setTextColor(0xFFFF0000);
            messages.add("[Error] " + error);
            messagesAdapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        unregisterReceiver(receiver);
        wifiDirectManager.disconnect();
        System.out.println("Receiver unregistered");
    }
}