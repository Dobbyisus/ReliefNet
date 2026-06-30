package com.meshrelief.core.model;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PacketSerializerTest {

    @Test
    public void serializeDeserialize_chatPacket_roundTrips() throws Exception {
        Packet packet = new Packet(
                "packet-1",
                "source-1",
                "dest-1",
                "group-1",
                "group-1",
                4,
                123456789L,
                PacketType.CHAT,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        byte[] serialized = PacketSerializer.serialize(packet);
        Packet deserialized = PacketSerializer.deserialize(serialized);

        assertEquals(packet.getPacketId(), deserialized.getPacketId());
        assertEquals(packet.getSourceNodeId(), deserialized.getSourceNodeId());
        assertEquals(packet.getDestinationNodeId(), deserialized.getDestinationNodeId());
        assertEquals(packet.getSourceGroupId(), deserialized.getSourceGroupId());
        assertEquals(packet.getDestinationGroupId(), deserialized.getDestinationGroupId());
        assertEquals(packet.getTtl(), deserialized.getTtl());
        assertEquals(packet.getTimestamp(), deserialized.getTimestamp());
        assertEquals(packet.getType(), deserialized.getType());
        assertArrayEquals(packet.getPayload(), deserialized.getPayload());
    }

    @Test
    public void serializeDeserialize_memberSnapshotPacket_roundTrips() throws Exception {
        Packet packet = new Packet(
                "packet-2",
                "source-2",
                null,
                "group-2",
                "group-2",
                4,
                987654321L,
                PacketType.MEMBER_SNAPSHOT,
                "snapshot".getBytes(StandardCharsets.UTF_8)
        );

        byte[] serialized = PacketSerializer.serialize(packet);
        Packet deserialized = PacketSerializer.deserialize(serialized);

        assertEquals(packet.getType(), deserialized.getType());
        assertEquals(packet.getDestinationNodeId(), deserialized.getDestinationNodeId());
        assertEquals(packet.getSourceGroupId(), deserialized.getSourceGroupId());
        assertEquals(packet.getDestinationGroupId(), deserialized.getDestinationGroupId());
        assertArrayEquals(packet.getPayload(), deserialized.getPayload());
    }

    @Test(expected = SerializationException.class)
    public void deserialize_invalidVersion_rejected() throws Exception {
        PacketSerializer.deserialize(new byte[]{0x01});
    }

    @Test(expected = SerializationException.class)
    public void serialize_emptySourceId_rejected() throws Exception {
        Packet packet = new Packet(
                "packet-3",
                "",
                "dest-1",
                "group-1",
                "group-1",
                4,
                1L,
                PacketType.CHAT,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        PacketSerializer.serialize(packet);
    }

    @Test(expected = SerializationException.class)
    public void serialize_oversizedPayload_rejected() throws Exception {
        byte[] oversizedPayload = new byte[(1024 * 1024) + 1];
        Packet packet = new Packet(
                "packet-4",
                "source-4",
                "dest-4",
                "group-4",
                "group-4",
                4,
                1L,
                PacketType.CHAT,
                oversizedPayload
        );

        PacketSerializer.serialize(packet);
    }
}
