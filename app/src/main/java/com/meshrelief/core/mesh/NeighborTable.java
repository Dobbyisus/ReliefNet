package com.meshrelief.core.mesh;

import com.meshrelief.core.p2p.PeerStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NeighborTable {

    private final Map<String, NeighborConnection> neighbors = new HashMap<>();

    public synchronized void upsertNeighbor(
            String neighborId,
            String displayName,
            String transportType,
            String connectionRef,
            long lastSeen) {

        NeighborConnection neighbor = neighbors.get(neighborId);
        if (neighbor == null) {
            neighbor = new NeighborConnection(
                    neighborId,
                    displayName,
                    PeerStatus.CONNECTED,
                    transportType,
                    connectionRef,
                    lastSeen
            );
            neighbors.put(neighborId, neighbor);
            return;
        }

        neighbor.setDisplayName(displayName);
        neighbor.setStatus(PeerStatus.CONNECTED);
        neighbor.setLastSeen(lastSeen);
    }

    public synchronized NeighborConnection getNeighbor(String neighborId) {
        return neighbors.get(neighborId);
    }

    public synchronized NeighborConnection getFirstConnectedNeighbor() {
        for (NeighborConnection neighbor : neighbors.values()) {
            if (neighbor.getStatus() == PeerStatus.CONNECTED) {
                return neighbor;
            }
        }

        return null;
    }

    public synchronized Collection<NeighborConnection> getNeighbors() {
        return Collections.unmodifiableCollection(neighbors.values());
    }

    public synchronized int size() {
        return neighbors.size();
    }

    public synchronized void markDisconnected(String neighborId) {
        NeighborConnection neighbor = neighbors.get(neighborId);
        if (neighbor != null) {
            neighbor.setStatus(PeerStatus.DISCONNECTED);
        }
    }

    public synchronized void clear() {
        neighbors.clear();
    }
}
