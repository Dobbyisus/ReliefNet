package com.meshrelief.core.connection;

import android.net.wifi.p2p.WifiP2pInfo;

import com.meshrelief.core.model.Packet;

/**
 * Manages socket connections for 2-device P2P communication.
 * Decides role (server or client) based on Group Owner status.
 * Supports bidirectional messaging.
 */
public class ConnectionHandler implements MessageListener {

    private SocketServer server;
    private SocketClient client;
    private boolean isInitialized = false;
    private boolean isGroupOwner = false;
    private MessageListener externalListener;

    public ConnectionHandler(MessageListener externalListener) {
        this.externalListener = externalListener;
    }

    /**
     * Initializes connection based on WiFi Direct info.
     * If this device is Group Owner: start server
     * If this device is client: connect to server
     *
     * @param info The WiFi P2P connection info
     */
    public void initialize(WifiP2pInfo info) {
        if (isInitialized) {
            return;
        }

        if (!info.groupFormed) {
            return;
        }

        isGroupOwner = info.isGroupOwner;

        if (info.isGroupOwner) {
            // This device is the Group Owner - start server
            server = new SocketServer(this);
            server.start();
        } else {
            // This device is client - connect to server
            client = new SocketClient(this);
            client.connect(info.groupOwnerAddress);
        }

        isInitialized = true;
    }

    /**
     * Sends a message through the appropriate socket.
     *
     * @param packet The packet to send
     */
    public void sendPacket(Packet packet) {
        if (isGroupOwner) {
            if (server != null && server.isClientConnected()) {
                server.sendPacket(packet);
            }
        } else {
            if (client != null && client.isConnected()) {
                client.sendPacket(packet);
            }
        }
    }

    /**
     * Checks if socket communication is ready.
     *
     * @return true if server is listening or client is connected
     */
    public boolean isReady() {
        if (isGroupOwner) {
            return server != null && server.isClientConnected();
        } else {
            return client != null && client.isConnected();
        }
    }

    /**
     * Cleans up resources.
     */
    public void cleanup() {
        if (server != null) server.stop();
        if (client != null) client.disconnect();
        isInitialized = false;
    }

    // MessageListener callbacks - forward to external listener
    @Override
    public void onPacketReceived(Packet packet, String senderId) {
        if (externalListener != null) {
            externalListener.onPacketReceived(packet, senderId);
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
    public void onError(String error) {
        if (externalListener != null) {
            externalListener.onError(error);
        }
    }
}
