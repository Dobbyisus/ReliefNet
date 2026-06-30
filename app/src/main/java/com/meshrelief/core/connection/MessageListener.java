package com.meshrelief.core.connection;

import com.meshrelief.core.mesh.NodeRole;
import com.meshrelief.core.model.Packet;

/**
 * Listener for packet events and transport state.
 */
public interface MessageListener {
    void onPacketReceived(Packet packet, String senderId);
    void onPeerDisconnected(String nodeId);
    void onConnectionEstablished();
    void onConnectionClosed();
    void onLocalNodeRoleChanged(NodeRole role);
    void onError(String error);
}
