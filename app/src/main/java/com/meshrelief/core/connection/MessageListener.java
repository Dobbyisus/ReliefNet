package com.meshrelief.core.connection;

/**
 * Listener for socket messages and connection events.
 */
public interface MessageListener {
    void onMessageReceived(String message);
    void onConnectionEstablished();
    void onConnectionClosed();
    void onError(String error);
}

