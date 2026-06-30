package com.meshrelief.core.mesh;

import com.meshrelief.core.p2p.PeerStatus;

public class NeighborConnection {

    private final String nodeId;
    private String displayName;
    private String groupId;
    private NodeRole role;
    private PeerStatus status;
    private final String transportType;
    private String connectionRef;
    private long lastSeen;

    public NeighborConnection(
            String nodeId,
            String displayName,
            String groupId,
            NodeRole role,
            PeerStatus status,
            String transportType,
            String connectionRef,
            long lastSeen) {

        this.nodeId = nodeId;
        this.displayName = displayName;
        this.groupId = groupId;
        this.role = role;
        this.status = status;
        this.transportType = transportType;
        this.connectionRef = connectionRef;
        this.lastSeen = lastSeen;
    }

    public String getNeighborId() {
        return nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public NodeRole getRole() {
        return role;
    }

    public void setRole(NodeRole role) {
        this.role = role;
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

    public void setConnectionRef(String connectionRef) {
        this.connectionRef = connectionRef;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
}
