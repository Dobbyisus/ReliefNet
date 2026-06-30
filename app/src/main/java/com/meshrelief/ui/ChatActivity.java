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
import com.meshrelief.core.mesh.NeighborConnection;
import com.meshrelief.core.mesh.NodeRole;
import com.meshrelief.core.transport.PeerDiscoveryListener;
import com.meshrelief.core.transport.WiFiDirectBroadcastReceiver;
import com.meshrelief.core.transport.WiFiDirectManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    private TextView connectionStatusView;
    private TextView roleStatusText;
    private TextView groupInfoText;
    private TextView memberCountText;
    private TextView listSectionTitle;
    private TextView currentRecipientText;
    private EditText messageInputView;
    private Button sendButtonView;
    private Button hostGroupButton;
    private Button discoverGroupsButton;
    private Button leaveGroupButton;
    private Button talkToAllButton;
    private ListView messagesListView;

    private ArrayAdapter<String> peerAdapter;
    private ArrayAdapter<String> messagesAdapter;
    private final ArrayList<String> peerEntries = new ArrayList<>();
    private final ArrayList<String> messages = new ArrayList<>();
    private final ArrayList<WifiP2pDevice> peerDevices = new ArrayList<>();
    private final ArrayList<NeighborConnection> memberEntries = new ArrayList<>();

    private String localDeviceId;
    private String selectedRecipientId;
    private String selectedRecipientLabel = "All";
    private boolean connected;
    private boolean hosting;
    private NodeRole currentRole = NodeRole.GROUP_CLIENT;
    private String currentGroupId = "Not assigned";

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

        localDeviceId = getOrCreateDeviceId();

        peerListView = findViewById(R.id.peerListView);
        connectionStatusView = findViewById(R.id.connectionStatus);
        roleStatusText = findViewById(R.id.roleStatusText);
        groupInfoText = findViewById(R.id.groupInfoText);
        memberCountText = findViewById(R.id.memberCountText);
        listSectionTitle = findViewById(R.id.listSectionTitle);
        currentRecipientText = findViewById(R.id.currentRecipientText);
        messageInputView = findViewById(R.id.messageInput);
        sendButtonView = findViewById(R.id.sendButton);
        hostGroupButton = findViewById(R.id.hostGroupButton);
        discoverGroupsButton = findViewById(R.id.discoverGroupsButton);
        leaveGroupButton = findViewById(R.id.leaveGroupButton);
        talkToAllButton = findViewById(R.id.talkToAllButton);
        messagesListView = findViewById(R.id.messagesListView);

        peerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                peerEntries
        );
        peerListView.setAdapter(peerAdapter);

        messagesAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                messages
        );
        messagesListView.setAdapter(messagesAdapter);

        meshRouter = new MeshRouter(
                localDeviceId,
                resolveDisplayName(),
                this
        );
        wifiDirectManager = new WiFiDirectManager(this, meshRouter);
        meshRouter.attachPacketSender(wifiDirectManager);

        peerListView.setOnItemClickListener((parent, view, position, id) -> {
            if (connected || hosting) {
                handleMemberSelection(position);
                return;
            }

            WifiP2pDevice device = peerDevices.get(position);
            wifiDirectManager.connect(device, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> connectionStatusView.setText("Status: Joining group"));
                }

                @Override
                public void onFailure(int reason) {
                    runOnUiThread(() -> Toast.makeText(
                            ChatActivity.this,
                            "Failed to join group",
                            Toast.LENGTH_SHORT
                    ).show());
                }
            });
        });

        sendButtonView.setOnClickListener(v -> {
            String messageText = messageInputView.getText().toString().trim();
            if (!messageText.isEmpty()
                    && meshRouter.sendChatMessage(messageText, selectedRecipientId)) {
                messageInputView.setText("");
            }
        });

        hostGroupButton.setOnClickListener(v -> {
            hosting = true;
            meshRouter.onHostingRequested();
            wifiDirectManager.createGroup(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        connectionStatusView.setText("Status: Hosting group");
                        refreshPeerSection();
                    });
                }

                @Override
                public void onFailure(int reason) {
                    runOnUiThread(() -> {
                        hosting = false;
                        meshRouter.onLocalNodeRoleChanged(NodeRole.GROUP_CLIENT);
                        meshRouter.onConnectionClosed();
                        connectionStatusView.setText("Status: Not connected");
                        Toast.makeText(
                                ChatActivity.this,
                                "Failed to host group",
                                Toast.LENGTH_SHORT
                        ).show();
                        refreshPeerSection();
                    });
                }
            });
        });

        discoverGroupsButton.setOnClickListener(v -> wifiDirectManager.discoverPeers(
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            if (!connected && !hosting) {
                                connectionStatusView.setText("Status: Discovering groups");
                            }
                        });
                    }

                    @Override
                    public void onFailure(int reason) {
                        runOnUiThread(() -> Toast.makeText(
                                ChatActivity.this,
                                "Failed to discover groups",
                                Toast.LENGTH_SHORT
                        ).show());
                    }
                }
        ));

        leaveGroupButton.setOnClickListener(v -> {
            wifiDirectManager.disconnect();
            hosting = false;
            connected = false;
            meshRouter.onLocalNodeRoleChanged(NodeRole.GROUP_CLIENT);
            selectedRecipientId = null;
            selectedRecipientLabel = "All";
            refreshUi();
        });

        talkToAllButton.setOnClickListener(v -> {
            selectedRecipientId = null;
            selectedRecipientLabel = "All";
            currentRecipientText.setText("Talking to: All");
        });

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
        refreshUi();
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
            if (connected || hosting) {
                return;
            }

            peerDevices.clear();
            peerEntries.clear();

            for (WifiP2pDevice device : peers) {
                peerDevices.add(device);
                peerEntries.add(device.deviceName + " (Join)");
            }

            peerAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onChatMessage(String senderName, String recipientName, String message, boolean outgoing) {
        runOnUiThread(() -> {
            messages.add(senderName + " -> " + recipientName + ": " + message);
            messagesAdapter.notifyDataSetChanged();
            messagesListView.smoothScrollToPosition(messages.size() - 1);
        });
    }

    @Override
    public void onGroupStateChanged(
            String statusText,
            NodeRole role,
            String groupId,
            int memberCount,
            boolean connected) {

        runOnUiThread(() -> {
            this.currentRole = role;
            this.currentGroupId = groupId;
            this.connected = connected;

            if (!connected && role != NodeRole.GROUP_OWNER) {
                hosting = false;
            }

            connectionStatusView.setText("Status: " + statusText);
            refreshUi();
        });
    }

    @Override
    public void onMembersUpdated(List<NeighborConnection> members) {
        runOnUiThread(() -> {
            memberEntries.clear();
            memberEntries.addAll(members);

            boolean recipientStillPresent = selectedRecipientId == null;
            for (NeighborConnection member : members) {
                if (member.getNodeId().equals(selectedRecipientId)) {
                    recipientStillPresent = true;
                    break;
                }
            }

            if (!recipientStillPresent) {
                selectedRecipientId = null;
                selectedRecipientLabel = "All";
                messages.add("[System] Selected member left the group. Switched to All.");
                messagesAdapter.notifyDataSetChanged();
            }

            refreshUi();
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            connectionStatusView.setText("Status: Error - " + error);
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        wifiDirectManager.disconnect();
    }

    private void handleMemberSelection(int position) {
        if (position < 0 || position >= memberEntries.size()) {
            return;
        }

        NeighborConnection member = memberEntries.get(position);
        if (localDeviceId.equals(member.getNodeId())) {
            Toast.makeText(this, "You are already selected locally", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedRecipientId = member.getNodeId();
        selectedRecipientLabel = member.getDisplayName();
        currentRecipientText.setText("Talking to: " + selectedRecipientLabel);
    }

    private void refreshUi() {
        roleStatusText.setText("Role: " + formatRole(currentRole));
        groupInfoText.setText("Group: " + currentGroupId);
        memberCountText.setText("Members: " + memberEntries.size());
        currentRecipientText.setText("Talking to: " + selectedRecipientLabel);
        messageInputView.setEnabled(connected || hosting);
        sendButtonView.setEnabled(connected || hosting);
        leaveGroupButton.setEnabled(connected || hosting);
        refreshPeerSection();
    }

    private void refreshPeerSection() {
        peerEntries.clear();

        if (connected || hosting) {
            listSectionTitle.setText("Group Members");
            for (NeighborConnection member : memberEntries) {
                peerEntries.add(formatMemberEntry(member));
            }
        } else {
            listSectionTitle.setText("Nearby Hosts");
            for (WifiP2pDevice device : peerDevices) {
                peerEntries.add(device.deviceName + " (Join)");
            }
        }

        peerAdapter.notifyDataSetChanged();
    }

    private String formatMemberEntry(NeighborConnection member) {
        StringBuilder builder = new StringBuilder();
        builder.append(member.getDisplayName());

        if (localDeviceId.equals(member.getNodeId())) {
            builder.append(" [You]");
        }

        if (member.getRole() == NodeRole.GROUP_OWNER) {
            builder.append(" [GO]");
        }

        return builder.toString();
    }

    private String formatRole(NodeRole role) {
        if (role == NodeRole.GROUP_OWNER) {
            return "Group Owner";
        }

        if (role == NodeRole.GROUP_RELAY) {
            return "Group Relay";
        }

        return "Group Member";
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
