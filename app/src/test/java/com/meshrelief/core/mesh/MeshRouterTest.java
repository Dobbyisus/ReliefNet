package com.meshrelief.core.mesh;

import com.meshrelief.core.model.Packet;
import com.meshrelief.core.model.PacketType;
import com.meshrelief.core.p2p.PeerStatus;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MeshRouterTest {

    @Test
    public void sendChatMessage_clientCreatesPacketWithSelectedTarget() {
        TestListener listener = new TestListener();
        TestPacketSender sender = new TestPacketSender(true);
        MeshRouter router = new MeshRouter(
                "local-device",
                "Local Device",
                listener,
                new SeenPacketCache(1000L),
                new NeighborTable()
        );
        router.attachPacketSender(sender);
        router.onLocalNodeRoleChanged(NodeRole.GROUP_CLIENT);

        boolean sent = router.sendChatMessage("hello", "neighbor-1");

        assertTrue(sent);
        assertNotNull(sender.lastPacket);
        assertEquals(PacketType.CHAT, sender.lastPacket.getType());
        assertEquals("local-device", sender.lastPacket.getSourceNodeId());
        assertEquals("neighbor-1", sender.lastPacket.getDestinationNodeId());
        assertEquals("local-device", sender.lastPacket.getSourceGroupId());
        assertEquals("local-device", sender.lastPacket.getDestinationGroupId());
        assertEquals("You", listener.lastSenderName);
        assertEquals("neighbor-1", listener.lastRecipientName);
        assertEquals("hello", listener.lastMessage);
        assertTrue(listener.lastOutgoing);
    }

    @Test
    public void onPacketReceived_duplicatePacketDropped() {
        TestListener listener = new TestListener();
        MeshRouter router = new MeshRouter(
                "local-device",
                "Local Device",
                listener,
                new SeenPacketCache(1000L),
                new NeighborTable()
        );

        Packet packet = new Packet(
                "packet-1",
                "neighbor-1",
                "local-device",
                "local-device",
                "local-device",
                4,
                1L,
                PacketType.CHAT,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        router.onPacketReceived(packet, "neighbor-1");
        router.onPacketReceived(packet, "neighbor-1");

        assertEquals(1, listener.messageCount);
    }

    @Test
    public void onPacketReceived_packetForLocalNodeDelivered() {
        TestListener listener = new TestListener();
        MeshRouter router = new MeshRouter(
                "local-device",
                "Local Device",
                listener,
                new SeenPacketCache(1000L),
                new NeighborTable()
        );
        router.getNeighborTable().upsertNeighbor(
                "neighbor-1",
                "Peer One",
                "local-device",
                NodeRole.GROUP_CLIENT,
                "WIFI_DIRECT",
                "wifi-direct-primary",
                System.currentTimeMillis()
        );

        Packet packet = new Packet(
                "packet-2",
                "neighbor-1",
                "local-device",
                "local-device",
                "local-device",
                4,
                1L,
                PacketType.CHAT,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        router.onPacketReceived(packet, "neighbor-1");

        assertEquals("Peer One", listener.lastSenderName);
        assertEquals("You", listener.lastRecipientName);
        assertEquals("hello", listener.lastMessage);
        assertFalse(listener.lastOutgoing);
    }

    @Test
    public void onPacketReceived_packetWithZeroTtlDropped() {
        TestListener listener = new TestListener();
        MeshRouter router = new MeshRouter(
                "local-device",
                "Local Device",
                listener,
                new SeenPacketCache(1000L),
                new NeighborTable()
        );

        Packet packet = new Packet(
                "packet-3",
                "neighbor-1",
                "other-device",
                "group-1",
                "group-1",
                0,
                1L,
                PacketType.CHAT,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        router.onPacketReceived(packet, "neighbor-1");

        assertEquals(0, listener.messageCount);
    }

    @Test
    public void onPacketReceived_helloUpdatesNeighborTable() throws Exception {
        TestListener listener = new TestListener();
        MeshRouter router = new MeshRouter(
                "local-device",
                "Local Device",
                listener,
                new SeenPacketCache(1000L),
                new NeighborTable()
        );
        router.onLocalNodeRoleChanged(NodeRole.GROUP_OWNER);

        Packet packet = new Packet(
                "packet-4",
                "neighbor-1",
                null,
                "pending-group",
                "local-device",
                4,
                1L,
                PacketType.HELLO,
                ControlPayloadCodec.encodeHello("Peer One", "pending-group", NodeRole.GROUP_CLIENT)
        );

        router.onPacketReceived(packet, "neighbor-1");

        NeighborConnection neighbor = router.getNeighborTable().getNeighbor("neighbor-1");
        assertNotNull(neighbor);
        assertEquals("Peer One", neighbor.getDisplayName());
        assertEquals("local-device", neighbor.getGroupId());
    }

    @Test
    public void onPacketReceived_memberSnapshotReplacesMembers() throws Exception {
        TestListener listener = new TestListener();
        MeshRouter router = new MeshRouter(
                "local-device",
                "Local Device",
                listener,
                new SeenPacketCache(1000L),
                new NeighborTable()
        );

        List<NeighborConnection> snapshotMembers = java.util.Arrays.asList(
                new NeighborConnection(
                        "go-1",
                        "Owner",
                        "group-1",
                        NodeRole.GROUP_OWNER,
                        PeerStatus.CONNECTED,
                        "WIFI_DIRECT",
                        null,
                        System.currentTimeMillis()
                ),
                new NeighborConnection(
                        "peer-2",
                        "Peer Two",
                        "group-1",
                        NodeRole.GROUP_CLIENT,
                        PeerStatus.CONNECTED,
                        "WIFI_DIRECT",
                        null,
                        System.currentTimeMillis()
                )
        );

        Packet packet = new Packet(
                "packet-5",
                "go-1",
                null,
                "group-1",
                "group-1",
                4,
                1L,
                PacketType.MEMBER_SNAPSHOT,
                ControlPayloadCodec.encodeMemberSnapshot("group-1", snapshotMembers)
        );

        router.onPacketReceived(packet, "go-1");

        assertNotNull(router.getNeighborTable().getNeighbor("go-1"));
        assertNotNull(router.getNeighborTable().getNeighbor("peer-2"));
        assertNotNull(router.getNeighborTable().getNeighbor("local-device"));
    }

    @Test
    public void groupOwnerBroadcastRoutesThroughBroadcastApi() {
        TestListener listener = new TestListener();
        TestPacketSender sender = new TestPacketSender(true);
        MeshRouter router = new MeshRouter(
                "local-device",
                "Local Device",
                listener,
                new SeenPacketCache(1000L),
                new NeighborTable()
        );
        router.attachPacketSender(sender);
        router.onLocalNodeRoleChanged(NodeRole.GROUP_OWNER);

        boolean sent = router.sendChatMessage("hello everyone", null);

        assertTrue(sent);
        assertNotNull(sender.broadcastPacket);
        assertEquals("local-device", sender.broadcastExcludeNodeId);
    }

    private static class TestPacketSender implements PacketSender {
        private final boolean ready;
        private Packet lastPacket;
        private Packet directPacket;
        private Packet broadcastPacket;
        private String sendToNodeId;
        private String broadcastExcludeNodeId;

        private TestPacketSender(boolean ready) {
            this.ready = ready;
        }

        @Override
        public void sendPacket(Packet packet) {
            this.lastPacket = packet;
        }

        @Override
        public void sendToNode(Packet packet, String nodeId) {
            this.directPacket = packet;
            this.sendToNodeId = nodeId;
        }

        @Override
        public void broadcastToGroup(Packet packet, String excludeNodeId) {
            this.broadcastPacket = packet;
            this.broadcastExcludeNodeId = excludeNodeId;
        }

        @Override
        public boolean isReady() {
            return ready;
        }
    }

    private static class TestListener implements MeshRouterListener {
        private String lastSenderName;
        private String lastRecipientName;
        private String lastMessage;
        private boolean lastOutgoing;
        private int messageCount;

        @Override
        public void onChatMessage(String senderName, String recipientName, String message, boolean outgoing) {
            this.lastSenderName = senderName;
            this.lastRecipientName = recipientName;
            this.lastMessage = message;
            this.lastOutgoing = outgoing;
            this.messageCount++;
        }

        @Override
        public void onGroupStateChanged(String statusText, NodeRole role, String groupId, int memberCount, boolean connected) {}

        @Override
        public void onMembersUpdated(List<NeighborConnection> members) {}

        @Override
        public void onError(String error) {}
    }
}
