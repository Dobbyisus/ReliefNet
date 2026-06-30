package com.meshrelief.core.mesh;

import com.meshrelief.core.p2p.PeerStatus;

public class NeighborConnection {

    private final String neighborId;
    private String displayName;
    private PeerStatus status;
    private final String transportType;
    private final String connectionRef;
    private long lastSeen;

    public NeighborConnection(
            String neighborId,
            String displayName,
            PeerStatus status,
            String transportType,
            String connectionRef,
            long lastSeen) {

        this.neighborId = neighborId;
        this.displayName = displayName;
        this.status = status;
        this.transportType = transportType;
        this.connectionRef = connectionRef;
        this.lastSeen = lastSeen;
    }

    public String getNeighborId() {
        return neighborId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public PeerStatus getStatus() {
        return status;
    }

    public void setStatus(PeerStatus status) {
        this.status = status;
    }

    public String getTransportType() {
        return transportType;
    }

    public String getConnectionRef() {
        return connectionRef;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
}
