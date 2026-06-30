package com.meshrelief.core.mesh;

import com.meshrelief.core.model.Packet;
import com.meshrelief.core.model.PacketType;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MeshRouterTest {

    @Test
    public void sendChatMessage_createsPacketWithExpectedFields() {
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
        router.getNeighborTable().upsertNeighbor(
                "neighbor-1",
                "Peer One",
                "WIFI_DIRECT",
                "wifi-direct-primary",
                System.currentTimeMillis()
        );

        boolean sent = router.sendChatMessage("hello");

        assertTrue(sent);
        assertNotNull(sender.lastPacket);
        assertEquals(PacketType.CHAT, sender.lastPacket.getType());
        assertEquals("local-device", sender.lastPacket.getSourceId());
        assertEquals("neighbor-1", sender.lastPacket.getDestinationId());
        assertEquals(4, sender.lastPacket.getTtl());
        assertEquals("You", listener.lastSenderName);
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
                "WIFI_DIRECT",
                "wifi-direct-primary",
                System.currentTimeMillis()
        );

        Packet packet = new Packet(
                "packet-2",
                "neighbor-1",
                "local-device",
                4,
                1L,
                PacketType.CHAT,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        router.onPacketReceived(packet, "neighbor-1");

        assertEquals("Peer One", listener.lastSenderName);
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
                0,
                1L,
                PacketType.CHAT,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        router.onPacketReceived(packet, "neighbor-1");

        assertEquals(0, listener.messageCount);
    }

    @Test
    public void onPacketReceived_helloUpdatesNeighborTable() {
        TestListener listener = new TestListener();
        MeshRouter router = new MeshRouter(
                "local-device",
                "Local Device",
                listener,
                new SeenPacketCache(1000L),
                new NeighborTable()
        );

        Packet packet = new Packet(
                "packet-4",
                "neighbor-1",
                null,
                4,
                1L,
                PacketType.HELLO,
                "Peer One".getBytes(StandardCharsets.UTF_8)
        );

        router.onPacketReceived(packet, "neighbor-1");

        NeighborConnection neighbor = router.getNeighborTable().getNeighbor("neighbor-1");
        assertNotNull(neighbor);
        assertEquals("Peer One", neighbor.getDisplayName());
    }

    @Test
    public void onPacketReceived_forwardingSafelySkippedWithSingleNeighbor() {
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
        router.getNeighborTable().upsertNeighbor(
                "neighbor-1",
                "Peer One",
                "WIFI_DIRECT",
                "wifi-direct-primary",
                System.currentTimeMillis()
        );

        Packet packet = new Packet(
                "packet-5",
                "neighbor-1",
                "remote-device",
                4,
                1L,
                PacketType.CHAT,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        router.onPacketReceived(packet, "neighbor-1");

        assertNull(sender.lastPacket);
    }

    private static class TestPacketSender implements PacketSender {
        private final boolean ready;
        private Packet lastPacket;

        private TestPacketSender(boolean ready) {
            this.ready = ready;
        }

        @Override
        public void sendPacket(Packet packet) {
            this.lastPacket = packet;
        }

        @Override
        public boolean isReady() {
            return ready;
        }
    }

    private static class TestListener implements MeshRouterListener {
        private String lastSenderName;
        private String lastMessage;
        private boolean lastOutgoing;
        private int messageCount;

        @Override
        public void onChatMessage(String senderName, String message, boolean outgoing) {
            this.lastSenderName = senderName;
            this.lastMessage = message;
            this.lastOutgoing = outgoing;
            this.messageCount++;
        }

        @Override
        public void onConnectionEstablished(String neighborName) {}

        @Override
        public void onConnectionClosed() {}

        @Override
        public void onError(String error) {}
    }
}
