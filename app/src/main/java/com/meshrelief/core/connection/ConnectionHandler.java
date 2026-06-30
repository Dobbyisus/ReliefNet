package com.meshrelief.core.connection;

import android.net.wifi.p2p.WifiP2pInfo;

import com.meshrelief.core.mesh.NodeRole;
import com.meshrelief.core.mesh.PacketSender;
import com.meshrelief.core.model.Packet;

/**
 * Manages GO or client socket transport for Wi-Fi Direct communication.
 */
public class ConnectionHandler implements MessageListener, PacketSender {

    private SocketServer server;
    private SocketClient client;
    private boolean isInitialized = false;
    private NodeRole localRole = NodeRole.GROUP_CLIENT;
    private final MessageListener externalListener;

    public ConnectionHandler(MessageListener externalListener) {
        this.externalListener = externalListener;
    }

    public void initialize(WifiP2pInfo info) {
        if (!info.groupFormed) {
            return;
        }

        NodeRole newRole = info.isGroupOwner
                ? NodeRole.GROUP_OWNER
                : NodeRole.GROUP_CLIENT;
        this.localRole = newRole;

        if (externalListener != null) {
            externalListener.onLocalNodeRoleChanged(newRole);
        }

        if (isInitialized) {
            return;
        }

        if (info.isGroupOwner) {
            server = new SocketServer(this);
            server.start();
        } else {
            client = new SocketClient(this);
            client.connect(info.groupOwnerAddress);
        }

        isInitialized = true;
    }

    @Override
    public void sendPacket(Packet packet) {
        if (localRole == NodeRole.GROUP_OWNER) {
            if (server != null) {
                server.sendPacket(packet);
            }
        } else if (client != null && client.isConnected()) {
            client.sendPacket(packet);
        }
    }

    @Override
    public void sendToNode(Packet packet, String nodeId) {
        if (localRole == NodeRole.GROUP_OWNER) {
            if (server != null) {
                server.sendToNode(packet, nodeId);
            }
            return;
        }

        sendPacket(packet);
    }

    @Override
    public void broadcastToGroup(Packet packet, String excludeNodeId) {
        if (localRole == NodeRole.GROUP_OWNER) {
            if (server != null) {
                server.broadcastToGroup(packet, excludeNodeId);
            }
            return;
        }

        sendPacket(packet);
    }

    @Override
    public boolean isReady() {
        if (localRole == NodeRole.GROUP_OWNER) {
            return server != null && server.isRunning();
        }

        return client != null && client.isConnected();
    }

    public void cleanup() {
        if (server != null) server.stop();
        if (client != null) client.disconnect();
        server = null;
        client = null;
        isInitialized = false;
    }

    @Override
    public void onPacketReceived(Packet packet, String senderId) {
        if (externalListener != null) {
            externalListener.onPacketReceived(packet, senderId);
        }
    }

    @Override
    public void onPeerDisconnected(String nodeId) {
        if (externalListener != null) {
            externalListener.onPeerDisconnected(nodeId);
        }
    }

    @Override
    public void onConnectionEstablished() {
        if (externalListener != null) {
            externalListener.onConnectionEstablished();
        }
    }

    @Override
    public void onConnectionClosed() {
        if (externalListener != null) {
            externalListener.onConnectionClosed();
        }
    }

    @Override
    public void onLocalNodeRoleChanged(NodeRole role) {
        if (externalListener != null) {
            externalListener.onLocalNodeRoleChanged(role);
        }
    }

    @Override
    public void onError(String error) {
        if (externalListener != null) {
            externalListener.onError(error);
        }
    }
}
