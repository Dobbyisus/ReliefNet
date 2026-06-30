package com.meshrelief.ui;

import android.Manifest;
import android.app.Activity;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.meshrelief.R;
import com.meshrelief.core.mesh.MeshRouter;
import com.meshrelief.core.mesh.MeshRouterListener;
import com.meshrelief.core.transport.PeerDiscoveryListener;
import com.meshrelief.core.transport.WiFiDirectBroadcastReceiver;
import com.meshrelief.core.transport.WiFiDirectManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class ChatActivity extends Activity implements PeerDiscoveryListener, MeshRouterListener {

    private static final int REQUEST_NEARBY_WIFI = 100;
    private static final int REQUEST_LOCATION = 101;
    private static final String IDENTITY_PREFS = "meshrelief_identity";
    private static final String DEVICE_ID_KEY = "device_id";

    private WiFiDirectManager wifiDirectManager;
    private MeshRouter meshRouter;
    private WiFiDirectBroadcastReceiver receiver;
    private IntentFilter intentFilter;

    private ListView peerListView;
    private ArrayAdapter<String> peerAdapter;
    private final ArrayList<String> peerNames = new ArrayList<>();
    private final ArrayList<WifiP2pDevice> peerDevices = new ArrayList<>();

    private TextView connectionStatusView;
    private EditText messageInputView;
    private Button sendButtonView;
    private ListView messagesListView;
    private ArrayAdapter<String> messagesAdapter;
    private final ArrayList<String> messages = new ArrayList<>();

    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean nearbyGranted =
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.NEARBY_WIFI_DEVICES)
                            == PackageManager.PERMISSION_GRANTED;

            if (!nearbyGranted) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.NEARBY_WIFI_DEVICES},
                        REQUEST_NEARBY_WIFI
                );
            }
        } else {
            boolean locationGranted =
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;

            if (!locationGranted) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION
                );
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        peerListView = findViewById(R.id.peerListView);
        peerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                peerNames
        );
        peerListView.setAdapter(peerAdapter);
        peerListView.setOnItemClickListener((parent, view, position, id) -> {
            WifiP2pDevice device = peerDevices.get(position);
            wifiDirectManager.connect(device);
        });

        connectionStatusView = findViewById(R.id.connectionStatus);
        messageInputView = findViewById(R.id.messageInput);
        sendButtonView = findViewById(R.id.sendButton);
        messagesListView = findViewById(R.id.messagesListView);
        messageInputView.setEnabled(false);
        sendButtonView.setEnabled(false);

        messagesAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                messages
        );
        messagesListView.setAdapter(messagesAdapter);

        sendButtonView.setOnClickListener(v -> {
            String messageText = messageInputView.getText().toString().trim();
            if (!messageText.isEmpty() && meshRouter.sendChatMessage(messageText)) {
                messageInputView.setText("");
            }
        });

        meshRouter = new MeshRouter(
                getOrCreateDeviceId(),
                resolveDisplayName(),
                this
        );
        wifiDirectManager = new WiFiDirectManager(this, meshRouter);
        meshRouter.attachPacketSender(wifiDirectManager);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        receiver = new WiFiDirectBroadcastReceiver(
                wifiDirectManager.getManager(),
                wifiDirectManager.getChannel(),
                wifiDirectManager,
                this
        );

        registerReceiver(receiver, intentFilter);
        requestPermissionsIfNeeded();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NEARBY_WIFI_DEVICES)
                    == PackageManager.PERMISSION_GRANTED) {
                wifiDirectManager.discoverPeers();
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                wifiDirectManager.discoverPeers();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
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

    @Override
    public void onChatMessage(String senderName, String message, boolean outgoing) {
        runOnUiThread(() -> {
            messages.add(senderName + ": " + message);
            messagesAdapter.notifyDataSetChanged();
            messagesListView.smoothScrollToPosition(messages.size() - 1);
        });
    }

    @Override
    public void onConnectionEstablished(String neighborName) {
        runOnUiThread(() -> {
            connectionStatusView.setText("Status: Connected");
            connectionStatusView.setTextColor(0xFF00AA00);
            messageInputView.setEnabled(true);
            sendButtonView.setEnabled(true);

            if (neighborName != null) {
                Toast.makeText(this, "Connection established!", Toast.LENGTH_SHORT).show();
                messages.add("[System] Connected to " + neighborName);
            }

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
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        wifiDirectManager.disconnect();
    }

    private String getOrCreateDeviceId() {
        SharedPreferences preferences = getSharedPreferences(IDENTITY_PREFS, MODE_PRIVATE);
        String deviceId = preferences.getString(DEVICE_ID_KEY, null);

        if (deviceId != null && !deviceId.trim().isEmpty()) {
            return deviceId;
        }

        String newDeviceId = UUID.randomUUID().toString();
        preferences.edit().putString(DEVICE_ID_KEY, newDeviceId).apply();
        return newDeviceId;
    }

    private String resolveDisplayName() {
        String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.trim();
        String model = Build.MODEL == null ? "" : Build.MODEL.trim();
        String displayName = (manufacturer + " " + model).trim();
        return displayName.isEmpty() ? "ReliefNet Device" : displayName;
    }
}
