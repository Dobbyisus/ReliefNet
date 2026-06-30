package com.meshrelief.core.mesh;

import com.meshrelief.core.connection.MessageListener;
import com.meshrelief.core.model.Packet;
import com.meshrelief.core.model.PacketType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MeshRouter implements MessageListener {

    private static final int DEFAULT_TTL = 4;
    private static final long SEEN_CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final String WIFI_DIRECT_TRANSPORT = "WIFI_DIRECT";
    private static final String PRIMARY_CONNECTION_REF = "wifi-direct-primary";

    private final String localDeviceId;
    private final String localDisplayName;
    private final MeshRouterListener listener;
    private final SeenPacketCache seenPacketCache;
    private final NeighborTable neighborTable;

    private PacketSender packetSender;

    public MeshRouter(String localDeviceId, String localDisplayName, MeshRouterListener listener) {
        this(
                localDeviceId,
                localDisplayName,
                listener,
                new SeenPacketCache(SEEN_CACHE_TTL_MS),
                new NeighborTable()
        );
    }

    public MeshRouter(
            String localDeviceId,
            String localDisplayName,
            MeshRouterListener listener,
            SeenPacketCache seenPacketCache,
            NeighborTable neighborTable) {

        this.localDeviceId = localDeviceId;
        this.localDisplayName = localDisplayName;
        this.listener = listener;
        this.seenPacketCache = seenPacketCache;
        this.neighborTable = neighborTable;
    }

    public void attachPacketSender(PacketSender packetSender) {
        this.packetSender = packetSender;
    }

    public boolean sendChatMessage(String message) {
        if (packetSender == null || !packetSender.isReady()) {
            notifyError("Not connected to peer");
            return false;
        }

        Packet packet = buildPacket(
                PacketType.CHAT,
                resolveCurrentDestinationId(),
                message.getBytes(StandardCharsets.UTF_8)
        );

        seenPacketCache.markIfNew(packet.getPacketId());
        packetSender.sendPacket(packet);

        if (listener != null) {
            listener.onChatMessage("You", message, true);
        }

        return true;
    }

    @Override
    public void onPacketReceived(Packet packet, String senderId) {
        if (packet == null || packet.getPacketId() == null) {
            notifyError("Received invalid packet");
            return;
        }

        if (!seenPacketCache.markIfNew(packet.getPacketId())) {
            return;
        }

        if (packet.getType() == PacketType.HELLO) {
            handleHelloPacket(packet);
            return;
        }

        if (packet.getTtl() <= 0) {
            return;
        }

        if (isForLocalNode(packet)) {
            deliverLocally(packet);
            return;
        }

        attemptForward(packet, senderId);
    }

    @Override
    public void onConnectionEstablished() {
        if (listener != null) {
            listener.onConnectionEstablished(null);
        }

        if (packetSender == null || !packetSender.isReady()) {
            return;
        }

        Packet helloPacket = buildPacket(
                PacketType.HELLO,
                null,
                localDisplayName.getBytes(StandardCharsets.UTF_8)
        );

        seenPacketCache.markIfNew(helloPacket.getPacketId());
        packetSender.sendPacket(helloPacket);
    }

    @Override
    public void onConnectionClosed() {
        neighborTable.clear();

        if (listener != null) {
            listener.onConnectionClosed();
        }
    }

    @Override
    public void onError(String error) {
        notifyError(error);
    }

    public NeighborTable getNeighborTable() {
        return neighborTable;
    }

    private Packet buildPacket(PacketType type, String destinationId, byte[] payload) {
        return new Packet(
                UUID.randomUUID().toString(),
                localDeviceId,
                destinationId,
                DEFAULT_TTL,
                System.currentTimeMillis(),
                type,
                payload
        );
    }

    private void handleHelloPacket(Packet packet) {
        String displayName = new String(packet.getPayload(), StandardCharsets.UTF_8);

        neighborTable.upsertNeighbor(
                packet.getSourceId(),
                displayName,
                WIFI_DIRECT_TRANSPORT,
                PRIMARY_CONNECTION_REF,
                System.currentTimeMillis()
        );

        if (listener != null) {
            listener.onConnectionEstablished(displayName);
        }
    }

    private void deliverLocally(Packet packet) {
        if (packet.getType() != PacketType.CHAT) {
            return;
        }

        String message = new String(packet.getPayload(), StandardCharsets.UTF_8);
        NeighborConnection neighbor = neighborTable.getNeighbor(packet.getSourceId());
        String senderName = neighbor != null
                ? neighbor.getDisplayName()
                : packet.getSourceId();

        if (listener != null) {
            listener.onChatMessage(senderName, message, false);
        }
    }

    private void attemptForward(Packet packet, String senderId) {
        if (packetSender == null || !packetSender.isReady()) {
            return;
        }

        if (neighborTable.size() <= 1) {
            return;
        }

        Packet forwardedPacket = packet.copy();
        forwardedPacket.setTtl(packet.getTtl() - 1);

        if (forwardedPacket.getTtl() <= 0) {
            return;
        }

        NeighborConnection neighbor = neighborTable.getFirstConnectedNeighbor();
        if (neighbor == null || neighbor.getNeighborId().equals(senderId)) {
            return;
        }

        packetSender.sendPacket(forwardedPacket);
    }

    private boolean isForLocalNode(Packet packet) {
        return packet.getDestinationId() == null
                || localDeviceId.equals(packet.getDestinationId());
    }

    private String resolveCurrentDestinationId() {
        NeighborConnection neighbor = neighborTable.getFirstConnectedNeighbor();
        return neighbor != null ? neighbor.getNeighborId() : null;
    }

    private void notifyError(String error) {
        if (listener != null) {
            listener.onError(error);
        }
    }
}
