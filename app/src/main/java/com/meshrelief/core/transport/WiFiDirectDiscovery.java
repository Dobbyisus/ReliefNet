package com.meshrelief.core.transport;

import android.annotation.SuppressLint;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Looper;

import com.meshrelief.core.p2p.Peer;
import com.meshrelief.core.p2p.PeerDiscovery;
import com.meshrelief.core.p2p.PeerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WiFi Direct Discovery implementation.
 * Discovers nearby WiFi Direct peers and converts them to Peer objects.
 * Integrates with PeerDiscovery interface for mesh network.
 * Periodically restarts discovery as WiFi Direct discovery stops after ~120 seconds.
 */
public class WiFiDirectDiscovery implements PeerDiscovery {
    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;
    
    private final ConcurrentHashMap<String, Peer> discoveredPeers = new ConcurrentHashMap<>();
    private boolean isDiscovering = false;
    private Handler discoveryHandler;
    private static final long DISCOVERY_RESTART_INTERVAL = 100000; // 100 seconds - WiFi Direct discovery lasts ~120 seconds
    private static final long DISCOVERY_REQUEST_INTERVAL = 90000; // Request peer list every 90 seconds

    /**
     * Creates a WiFiDirectDiscovery instance.
     *
     * @param wifiP2pManager WiFiP2pManager for WiFi Direct operations
     * @param channel communication channel
     */
    public WiFiDirectDiscovery(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel) {
        if (wifiP2pManager == null || channel == null) {
            throw new IllegalArgumentException("WifiP2pManager and Channel cannot be null");
        }
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
        this.discoveryHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Starts WiFi Direct peer discovery.
     * Must be called from main thread or Handler.
     */
    @Override
    public void startDiscovery() {
        if (isDiscovering) {
            return; // Already discovering
        }

        try {
            // First request - get current peer list
            requestPeerList();
            performDiscovery();
            isDiscovering = true;
            System.out.println("WiFi Direct discovery started");

            // Schedule periodic discovery restart
            scheduleDiscoveryRestart();

        } catch (Exception e) {
            System.err.println("Error starting discovery: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Performs the actual discovery call.
     */
    @SuppressLint("MissingPermission")
    private void performDiscovery() {
        try {
            wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    System.out.println("WiFi Direct discovery scan initiated successfully");
                }

                @Override
                public void onFailure(int reason) {
                    String reasonStr = getFailureReason(reason);
                    System.err.println("WiFi Direct discovery scan failed: " + reasonStr);
                }
            });

        } catch (Exception e) {
            System.err.println("Error during discovery: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Schedules periodic discovery restart.
     */
    private void scheduleDiscoveryRestart() {
        if (!isDiscovering) {
            return;
        }

        // Restart discovery every 100 seconds (before it naturally stops at ~120)
        discoveryHandler.postDelayed(() -> {
            if (isDiscovering) {
                System.out.println("Restarting WiFi Direct discovery...");
                performDiscovery();
                scheduleDiscoveryRestart();
            }
        }, DISCOVERY_RESTART_INTERVAL);

        // Request peer list periodically
        discoveryHandler.postDelayed(() -> {
            if (isDiscovering) {
                System.out.println("Periodically requesting peer list...");
                requestPeerList();
                // Reschedule this task
                schedulePeriodicPeerListRequest();
            }
        }, DISCOVERY_REQUEST_INTERVAL);
    }

    /**
     * Reschedules periodic peer list request.
     */
    private void schedulePeriodicPeerListRequest() {
        if (!isDiscovering) {
            return;
        }

        discoveryHandler.postDelayed(() -> {
            if (isDiscovering) {
                requestPeerList();
                schedulePeriodicPeerListRequest();
            }
        }, DISCOVERY_REQUEST_INTERVAL);
    }

    /**
     * Stops WiFi Direct peer discovery.
     */
    @Override
    public void stopDiscovery() {
        if (!isDiscovering) {
            return;
        }

        try {
            // Remove all scheduled tasks
            if (discoveryHandler != null) {
                discoveryHandler.removeCallbacksAndMessages(null);
            }

            wifiP2pManager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    System.out.println("WiFi Direct discovery stopped successfully");
                    isDiscovering = false;
                }

                @Override
                public void onFailure(int reason) {
                    String reasonStr = getFailureReason(reason);
                    System.err.println("Failed to stop discovery: " + reasonStr);
                    isDiscovering = false;
                }
            });

        } catch (Exception e) {
            System.err.println("Error stopping discovery: " + e.getMessage());
            isDiscovering = false;
        }
    }


    /**
     * Gets currently discovered peers.
     *
     * @return list of discovered peers
     */
    @Override
    public List<Peer> getDiscoveredPeers() {
        return new ArrayList<>(discoveredPeers.values());
    }

    /**
     * Requests current peer list from WiFi Direct.
     * Called by broadcast receiver when peers are available.
     */
    @SuppressLint("MissingPermission")
    public void requestPeerList() {
        try {
            wifiP2pManager.requestPeers(channel, peers -> {
                if (peers != null) {
                    onPeersAvailable(peers);
                }
            });

        } catch (Exception e) {
            System.err.println("Error requesting peer list: " + e.getMessage());
        }
    }

    /**
     * Handles available peers from WiFi Direct scan.
     * Converts WifiP2pDevice objects to Peer objects.
     *
     * @param deviceList list of discovered WiFi P2P devices
     */
    public synchronized void onPeersAvailable(WifiP2pDeviceList deviceList) {
        discoveredPeers.clear();

        for (WifiP2pDevice device : deviceList.getDeviceList()) {
            String peerId = device.deviceAddress; // MAC address as ID
            String peerName = device.deviceName;
            
            PeerStatus status = device.status == WifiP2pDevice.CONNECTED ? 
                    PeerStatus.ACTIVE : PeerStatus.DISCOVERED;

            // Extract signal strength from device (if available)
            double signalStrength = -1.0; // WiFi Direct doesn't directly provide RSSI
            
            Peer peer = new Peer(
                    peerId,
                    peerName,
                    status,
                    System.currentTimeMillis(),
                    signalStrength,
                    "DeviceType: " + getDeviceType(device)
            );

            discoveredPeers.put(peerId, peer);
            System.out.println("Peer discovered: " + peerName + " (" + peerId + ") - Status: " + status);
        }

        System.out.println("Total peers discovered: " + discoveredPeers.size());
    }

    /**
     * Updates peer status when connection changes.
     * Called by connection manager.
     *
     * @param peerId the peer ID (MAC address)
     * @param newStatus the new connection status
     */
    public synchronized void updatePeerStatus(String peerId, PeerStatus newStatus) {
        Peer peer = discoveredPeers.get(peerId);
        if (peer != null) {
            Peer updated = peer.withStatus(newStatus);
            discoveredPeers.put(peerId, updated);
            System.out.println("Peer status updated: " + peerId + " → " + newStatus);
        }
    }

    /**
     * Removes a peer from discovered list.
     *
     * @param peerId the peer ID
     */
    public synchronized void removePeer(String peerId) {
        discoveredPeers.remove(peerId);
        System.out.println("Peer removed from discovery: " + peerId);
    }

    /**
     * Checks if a peer is discovered.
     *
     * @param peerId the peer ID
     * @return true if peer is in discovery list
     */
    public boolean hasPeer(String peerId) {
        return discoveredPeers.containsKey(peerId);
    }

    /**
     * Gets a specific discovered peer.
     *
     * @param peerId the peer ID
     * @return the Peer, or null if not found
     */
    public Peer getPeer(String peerId) {
        return discoveredPeers.get(peerId);
    }

    /**
     * Helper method to get human-readable device type.
     */
    private String getDeviceType(WifiP2pDevice device) {
        switch (device.primaryDeviceType) {
            case "10-0050F204-5": return "Computer";
            case "1-0050F204-1": return "Camera";
            case "7-0050F204-1": return "Display";
            default: return device.primaryDeviceType;
        }
    }

    /**
     * Helper method to convert WiFi Direct error codes to readable strings.
     */
    private String getFailureReason(int reason) {
        switch (reason) {
            case WifiP2pManager.P2P_UNSUPPORTED:
                return "P2P is not supported";
            case WifiP2pManager.ERROR:
                return "Internal error";
            case WifiP2pManager.BUSY:
                return "Framework is busy";
            default:
                return "Unknown error (" + reason + ")";
        }
    }
}
