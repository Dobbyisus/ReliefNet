package com.meshrelief.core.connection;

import android.net.wifi.p2p.WifiP2pInfo;

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
            System.out.println("Connection already initialized");
            return;
        }

        if (!info.groupFormed) {
            System.err.println("Group not formed yet");
            return;
        }

        System.out.println("Initializing connection...");
        System.out.println("Is Group Owner: " + info.isGroupOwner);
        System.out.println("Group Owner Address: " + info.groupOwnerAddress);

        isGroupOwner = info.isGroupOwner;

        if (info.isGroupOwner) {
            // This device is the Group Owner - start server
            System.out.println("Starting as SERVER (Group Owner)");
            server = new SocketServer(this);
            server.start();
        } else {
            // This device is client - connect to server
            System.out.println("Starting as CLIENT (connecting to Group Owner)");
            client = new SocketClient(this);
            client.connect(info.groupOwnerAddress);
        }

        isInitialized = true;
    }

    /**
     * Sends a message through the appropriate socket.
     *
     * @param message The message to send
     */
    public void sendMessage(String message) {
        System.out.println(
                "ConnectionHandler.sendMessage(): "
                        + message
        );
        System.out.println(
                "isGroupOwner = "
                        + isGroupOwner
        );
        if (isGroupOwner) {
            if (server != null && server.isClientConnected()) {
                server.sendMessage(message);
            } else {
                System.err.println("Server: no client connected");
            }
        } else {
            if (client != null && client.isConnected()) {
                client.sendMessage(message);
            } else {
                System.err.println("Client: not connected to server");
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
    public void onMessageReceived(String message) {
        if (externalListener != null) {
            externalListener.onMessageReceived(message);
        }
    }

    @Override
    public void onConnectionEstablished() {
        System.out.println("Connection established");
        if (externalListener != null) {
            externalListener.onConnectionEstablished();
        }
    }

    @Override
    public void onConnectionClosed() {
        System.out.println("Connection closed");
        if (externalListener != null) {
            externalListener.onConnectionClosed();
        }
    }

    @Override
    public void onError(String error) {
        System.err.println("Connection error: " + error);
        if (externalListener != null) {
            externalListener.onError(error);
        }
    }
}
