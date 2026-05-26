package com.meshrelief.core.p2p;

import java.util.HashMap;
import java.util.Map;

public class PeerManager {

    private final Map<String, Peer> peers = new HashMap<>();

    // Add peer
    public void addPeer(Peer peer) throws Exception {
        if (peer == null) {
            throw new IllegalArgumentException("Peer cannot be null");
        }

        if (peers.containsKey(peer.getId())) {
            throw new Exception("Peer already exists");
        }

        peers.put(peer.getId(), peer);
    }

    // Get peer
    public Peer getPeer(String id) {
        return peers.get(id);
    }

    // Check peer
    public boolean hasPeer(String id) {
        return peers.containsKey(id);
    }

    // Remove peer
    public void removePeer(String id) {
        peers.remove(id);
    }

    // Total peers
    public int getPeerCount() {
        return peers.size();
    }

    // Update peer status
    public void updatePeerStatus(String id, PeerStatus status) throws Exception {

        Peer peer = peers.get(id);

        if (peer == null) {
            throw new Exception("Peer not found");
        }

        Peer updatedPeer = new Peer(
                peer.getId(),
                peer.getName(),
                status
        );

        peers.put(id, updatedPeer);
    }

    // Get all peers
    public Map<String, Peer> getAllPeers() {
        return peers;
    }

    // Clear all
    public void clear() {
        peers.clear();
    }
}