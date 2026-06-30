package com.meshrelief.core.connection;

import com.meshrelief.core.model.Packet;

/**
 * Listener for socket messages and connection events.
 */
public interface MessageListener {
    void onPacketReceived(Packet packet, String senderId);
    void onConnectionEstablished();
    void onConnectionClosed();
    void onError(String error);
}

