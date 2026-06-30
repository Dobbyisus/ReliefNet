package com.meshrelief.core.model;

public class Packet {
    private final String packetId;      // unique (for SeenCache)
    private final String sourceId;
    private final String destinationId; // null = broadcast
    private final String sourceGroupId;
    private final String destinationGroupId;
    private int ttl;
    private final long timestamp;
    private final PacketType type;
    private final byte[] payload;

    public Packet(
            String packetId,
            String sourceId,
            String destinationId,
            String sourceGroupId,
            String destinationGroupId,
            int ttl,
            long timestamp,
            PacketType type,
            byte[] payload) {

        this.packetId = packetId;
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.sourceGroupId = sourceGroupId;
        this.destinationGroupId = destinationGroupId;
        this.ttl = ttl;
        this.timestamp = timestamp;
        this.type = type;
        this.payload = payload;
    }

    public Packet copy() {
        return new Packet(
                this.packetId,
                this.sourceId,
                this.destinationId,
                this.sourceGroupId,
                this.destinationGroupId,
                this.ttl,
                this.timestamp,
                this.type,
                this.payload != null ? this.payload.clone() : null
        );
    }

    public String getPacketId() {
        return packetId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceNodeId() {
        return sourceId;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public String getDestinationNodeId() {
        return destinationId;
    }

    public String getSourceGroupId() {
        return sourceGroupId;
    }

    public String getDestinationGroupId() {
        return destinationGroupId;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public PacketType getType() {
        return type;
    }

    public byte[] getPayload() {
        return payload;
    }
}
