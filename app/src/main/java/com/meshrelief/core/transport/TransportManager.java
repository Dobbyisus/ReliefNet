package com.meshrelief.core.transport;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;

import com.meshrelief.core.p2p.PeerManager;

/**
 * Singleton manager for WiFi Direct Transport.
 * Ensures all activities share the same transport and peer manager instances.
 * This prevents creating multiple independent discovery processes.
 */
public class TransportManager {
    private static TransportManager instance;
    private WifiDirectTransport transport;
    private PeerManager peerManager;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private Context applicationContext;

    /**
     * Private constructor for singleton pattern.
     */
    private TransportManager() {
    }

    /**
     * Gets the singleton instance.
     *
     * @return TransportManager singleton instance
     */
    public static synchronized TransportManager getInstance() {
        if (instance == null) {
            instance = new TransportManager();
        }
        return instance;
    }

    /**
     * Initializes the transport with WiFi P2P manager and context.
     * Only initializes once - subsequent calls are ignored if already initialized.
     *
     * @param context Android application context
     * @param wifiP2pManager WiFi P2P manager from system service
     * @param channel WiFi P2P communication channel
     */
    public synchronized void initialize(Context context, WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel) {
        // Only initialize once
        if (transport != null) {
            System.out.println("TransportManager already initialized, reusing existing instance");
            return;
        }

        if (context == null || wifiP2pManager == null || channel == null) {
            throw new IllegalArgumentException("Context, WifiP2pManager, and Channel cannot be null");
        }

        this.applicationContext = context;
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
        this.peerManager = new PeerManager();

        // Create transport with shared peer manager
        this.transport = new WifiDirectTransport(context, wifiP2pManager, channel, peerManager);

        System.out.println("TransportManager initialized successfully");
    }

    /**
     * Starts the transport if not already running.
     */
    public synchronized void startTransport() {
        if (transport == null) {
            throw new IllegalStateException("TransportManager not initialized. Call initialize() first.");
        }

        if (!transport.isRunning()) {
            transport.start();
            System.out.println("Transport started");
        }
    }

    /**
     * Gets the shared transport instance.
     *
     * @return WifiDirectTransport instance, or null if not initialized
     */
    public WifiDirectTransport getTransport() {
        return transport;
    }

    /**
     * Gets the shared peer manager instance.
     *
     * @return PeerManager instance, or null if not initialized
     */
    public PeerManager getPeerManager() {
        return peerManager;
    }

    /**
     * Gets the WiFi P2P manager.
     *
     * @return WifiP2pManager instance
     */
    public WifiP2pManager getWifiP2pManager() {
        return wifiP2pManager;
    }

    /**
     * Gets the WiFi P2P channel.
     *
     * @return WifiP2pManager.Channel instance
     */
    public WifiP2pManager.Channel getChannel() {
        return channel;
    }

    /**
     * Checks if transport is initialized and running.
     *
     * @return true if initialized and running
     */
    public boolean isTransportRunning() {
        return transport != null && transport.isRunning();
    }

    /**
     * Stops the transport and cleans up resources.
     */
    public synchronized void stopTransport() {
        if (transport != null) {
            transport.stop();
            System.out.println("Transport stopped");
        }
    }

    /**
     * Resets the singleton for testing or app restart.
     */
    public synchronized void reset() {
        if (transport != null) {
            transport.stop();
        }
        transport = null;
        peerManager = null;
        wifiP2pManager = null;
        channel = null;
        applicationContext = null;
        System.out.println("TransportManager reset");
    }
}

