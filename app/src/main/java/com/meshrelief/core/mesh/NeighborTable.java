package com.meshrelief.core.mesh;

import com.meshrelief.core.p2p.PeerStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NeighborTable {

    private final Map<String, NeighborConnection> neighbors = new HashMap<>();

    public synchronized void upsertNeighbor(
            String nodeId,
            String displayName,
            String groupId,
            NodeRole role,
            String transportType,
            String connectionRef,
            long lastSeen) {

        NeighborConnection neighbor = neighbors.get(nodeId);
        if (neighbor == null) {
            neighbor = new NeighborConnection(
                    nodeId,
                    displayName,
                    groupId,
                    role,
                    PeerStatus.CONNECTED,
                    transportType,
                    connectionRef,
                    lastSeen
            );
            neighbors.put(nodeId, neighbor);
            return;
        }

        neighbor.setDisplayName(displayName);
        neighbor.setGroupId(groupId);
        neighbor.setRole(role);
        neighbor.setStatus(PeerStatus.CONNECTED);
        neighbor.setConnectionRef(connectionRef);
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

    public synchronized Collection<NeighborConnection> getConnectedNeighbors() {
        Map<String, NeighborConnection> connected = new HashMap<>();
        for (Map.Entry<String, NeighborConnection> entry : neighbors.entrySet()) {
            if (entry.getValue().getStatus() == PeerStatus.CONNECTED) {
                connected.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.unmodifiableCollection(connected.values());
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

    public synchronized void replaceAll(Collection<NeighborConnection> members) {
        neighbors.clear();
        for (NeighborConnection member : members) {
            neighbors.put(member.getNodeId(), member);
        }
    }
}
