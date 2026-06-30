package com.meshrelief.core.mesh;

import com.meshrelief.core.connection.MessageListener;
import com.meshrelief.core.model.Packet;
import com.meshrelief.core.model.PacketType;
import com.meshrelief.core.model.SerializationException;
import com.meshrelief.core.p2p.PeerStatus;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class MeshRouter implements MessageListener {

    private static final int DEFAULT_TTL = 4;
    private static final long SEEN_CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final String WIFI_DIRECT_TRANSPORT = "WIFI_DIRECT";

    private final String localDeviceId;
    private final String localDisplayName;
    private final MeshRouterListener listener;
    private final SeenPacketCache seenPacketCache;
    private final NeighborTable neighborTable;

    private PacketSender packetSender;
    private NodeRole localRole = NodeRole.GROUP_CLIENT;
    private String currentGroupId;
    private boolean connected;

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
        this.currentGroupId = localDeviceId;
    }

    public void attachPacketSender(PacketSender packetSender) {
        this.packetSender = packetSender;
    }

    public boolean sendChatMessage(String message, String recipientNodeId) {
        if (packetSender == null || !packetSender.isReady()) {
            notifyError("Not connected to group");
            return false;
        }

        String recipientLabel = recipientNodeId == null ? "All" : resolveName(recipientNodeId);
        Packet packet = buildPacket(
                PacketType.CHAT,
                recipientNodeId,
                currentGroupId,
                message.getBytes(StandardCharsets.UTF_8)
        );

        seenPacketCache.markIfNew(packet.getPacketId());

        if (localRole == NodeRole.GROUP_OWNER) {
            routeLocalOutboundFromGroupOwner(packet);
        } else {
            packetSender.sendPacket(packet);
        }

        if (listener != null) {
            listener.onChatMessage("You", recipientLabel, message, true);
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

        if (packet.getType() == PacketType.MEMBER_SNAPSHOT) {
            handleMemberSnapshot(packet);
            return;
        }

        if (packet.getTtl() <= 0) {
            return;
        }

        if (!currentGroupId.equals(packet.getDestinationGroupId())) {
            // Future forwarding hook for other groups.
            return;
        }

        if (localRole == NodeRole.GROUP_OWNER) {
            routeGroupPacket(packet);
            return;
        }

        deliverLocally(packet);
    }

    @Override
    public void onPeerDisconnected(String nodeId) {
        if (nodeId == null) {
            return;
        }

        neighborTable.markDisconnected(nodeId);
        if (localRole == NodeRole.GROUP_OWNER) {
            emitMemberSnapshot();
            pushMembersUpdate();
        }
    }

    @Override
    public void onConnectionEstablished() {
        connected = true;

        if (localRole == NodeRole.GROUP_OWNER) {
            ensureLocalMember();
            notifyGroupState("Hosting group");
            pushMembersUpdate();
            return;
        }

        notifyGroupState("Joined group");
        sendHelloPacket();
    }

    @Override
    public void onConnectionClosed() {
        connected = false;
        neighborTable.clear();
        if (localRole != NodeRole.GROUP_OWNER) {
            currentGroupId = localDeviceId;
        }
        notifyGroupState("Not connected");
        pushMembersUpdate();
    }

    @Override
    public void onLocalNodeRoleChanged(NodeRole role) {
        this.localRole = role;

        if (role == NodeRole.GROUP_OWNER) {
            currentGroupId = localDeviceId;
            ensureLocalMember();
            notifyGroupState("Hosting group");
            pushMembersUpdate();
        } else {
            notifyGroupState(connected ? "Joined group" : "Not connected");
        }
    }

    @Override
    public void onError(String error) {
        notifyError(error);
    }

    public void onHostingRequested() {
        localRole = NodeRole.GROUP_OWNER;
        currentGroupId = localDeviceId;
        connected = true;
        ensureLocalMember();
        notifyGroupState("Hosting group");
        pushMembersUpdate();
    }

    public NeighborTable getNeighborTable() {
        return neighborTable;
    }

    public String getLocalDeviceId() {
        return localDeviceId;
    }

    private void handleHelloPacket(Packet packet) {
        try {
            ControlPayloadCodec.HelloPayload helloPayload =
                    ControlPayloadCodec.decodeHello(packet.getPayload());

            neighborTable.upsertNeighbor(
                    packet.getSourceNodeId(),
                    helloPayload.getDisplayName(),
                    currentGroupId,
                    helloPayload.getRole(),
                    WIFI_DIRECT_TRANSPORT,
                    packet.getSourceNodeId(),
                    System.currentTimeMillis()
            );

            if (localRole == NodeRole.GROUP_OWNER) {
                emitMemberSnapshot();
                pushMembersUpdate();
            }
        } catch (SerializationException e) {
            notifyError(e.getMessage());
        }
    }

    private void handleMemberSnapshot(Packet packet) {
        try {
            ControlPayloadCodec.MemberSnapshot snapshot =
                    ControlPayloadCodec.decodeMemberSnapshot(packet.getPayload());

            currentGroupId = snapshot.getGroupId();
            List<NeighborConnection> members = new ArrayList<>(snapshot.getMembers());
            boolean selfPresent = false;

            for (NeighborConnection member : members) {
                if (localDeviceId.equals(member.getNodeId())) {
                    selfPresent = true;
                }
            }

            if (!selfPresent) {
                members.add(createLocalMember(NodeRole.GROUP_CLIENT));
            }

            neighborTable.replaceAll(members);
            connected = true;
            notifyGroupState("Joined group");
            pushMembersUpdate();
        } catch (SerializationException e) {
            notifyError(e.getMessage());
        }
    }

    private void routeLocalOutboundFromGroupOwner(Packet packet) {
        if (packet.getDestinationNodeId() == null) {
            packetSender.broadcastToGroup(packet, localDeviceId);
            return;
        }

        if (localDeviceId.equals(packet.getDestinationNodeId())) {
            return;
        }

        packetSender.sendToNode(packet, packet.getDestinationNodeId());
    }

    private void routeGroupPacket(Packet packet) {
        if (packet.getDestinationNodeId() == null) {
            deliverLocally(packet);
            packetSender.broadcastToGroup(packet, packet.getSourceNodeId());
            return;
        }

        if (localDeviceId.equals(packet.getDestinationNodeId())) {
            deliverLocally(packet);
            return;
        }

        packetSender.sendToNode(packet, packet.getDestinationNodeId());
    }

    private void deliverLocally(Packet packet) {
        if (packet.getType() != PacketType.CHAT) {
            return;
        }

        String message = new String(packet.getPayload(), StandardCharsets.UTF_8);
        String senderName = resolveName(packet.getSourceNodeId());
        String recipientName = packet.getDestinationNodeId() == null
                ? "All"
                : localDeviceId.equals(packet.getDestinationNodeId())
                ? "You"
                : resolveName(packet.getDestinationNodeId());

        if (listener != null) {
            listener.onChatMessage(senderName, recipientName, message, false);
        }
    }

    private void sendHelloPacket() {
        if (packetSender == null || !packetSender.isReady()) {
            return;
        }

        try {
            Packet helloPacket = buildPacket(
                    PacketType.HELLO,
                    null,
                    currentGroupId,
                    ControlPayloadCodec.encodeHello(localDisplayName, currentGroupId, NodeRole.GROUP_CLIENT)
            );

            seenPacketCache.markIfNew(helloPacket.getPacketId());
            packetSender.sendPacket(helloPacket);
        } catch (SerializationException e) {
            notifyError(e.getMessage());
        }
    }

    private void emitMemberSnapshot() {
        if (packetSender == null) {
            return;
        }

        try {
            List<NeighborConnection> members = new ArrayList<>();
            ensureLocalMember();
            Collection<NeighborConnection> currentMembers = neighborTable.getNeighbors();
            members.addAll(currentMembers);

            Packet snapshotPacket = buildPacket(
                    PacketType.MEMBER_SNAPSHOT,
                    null,
                    currentGroupId,
                    ControlPayloadCodec.encodeMemberSnapshot(currentGroupId, members)
            );

            packetSender.broadcastToGroup(snapshotPacket, null);
        } catch (SerializationException e) {
            notifyError(e.getMessage());
        }
    }

    private void ensureLocalMember() {
        NodeRole role = localRole == null ? NodeRole.GROUP_CLIENT : localRole;
        neighborTable.upsertNeighbor(
                localDeviceId,
                localDisplayName,
                currentGroupId,
                role,
                WIFI_DIRECT_TRANSPORT,
                localDeviceId,
                System.currentTimeMillis()
        );
    }

    private NeighborConnection createLocalMember(NodeRole role) {
        return new NeighborConnection(
                localDeviceId,
                localDisplayName,
                currentGroupId,
                role,
                PeerStatus.CONNECTED,
                WIFI_DIRECT_TRANSPORT,
                localDeviceId,
                System.currentTimeMillis()
        );
    }

    private Packet buildPacket(
            PacketType type,
            String destinationNodeId,
            String destinationGroupId,
            byte[] payload) {

        return new Packet(
                UUID.randomUUID().toString(),
                localDeviceId,
                destinationNodeId,
                currentGroupId,
                destinationGroupId,
                DEFAULT_TTL,
                System.currentTimeMillis(),
                type,
                payload
        );
    }

    private void pushMembersUpdate() {
        if (listener == null) {
            return;
        }

        List<NeighborConnection> members = new ArrayList<>(neighborTable.getNeighbors());
        listener.onMembersUpdated(members);
    }

    private void notifyGroupState(String statusText) {
        if (listener != null) {
            listener.onGroupStateChanged(
                    statusText,
                    localRole,
                    currentGroupId,
                    neighborTable.size(),
                    connected
            );
        }
    }

    private String resolveName(String nodeId) {
        if (nodeId == null) {
            return "All";
        }

        if (localDeviceId.equals(nodeId)) {
            return "You";
        }

        NeighborConnection neighbor = neighborTable.getNeighbor(nodeId);
        return neighbor != null ? neighbor.getDisplayName() : nodeId;
    }

    private void notifyError(String error) {
        if (listener != null) {
            listener.onError(error);
        }
    }
}
