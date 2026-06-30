package com.meshrelief.core.mesh;

import com.meshrelief.core.model.SerializationException;
import com.meshrelief.core.p2p.PeerStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ControlPayloadCodec {

    private ControlPayloadCodec() {}

    public static byte[] encodeHello(String displayName, String groupId, NodeRole role)
            throws SerializationException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            writeString(dos, displayName);
            writeString(dos, groupId);
            dos.writeByte(role.ordinal());
            dos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new SerializationException("Failed to encode HELLO payload", e);
        }
    }

    public static HelloPayload decodeHello(byte[] payload) throws SerializationException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(payload);
             DataInputStream dis = new DataInputStream(bais)) {

            String displayName = readString(dis);
            String groupId = readString(dis);
            NodeRole role = NodeRole.values()[dis.readUnsignedByte()];
            return new HelloPayload(displayName, groupId, role);
        } catch (EOFException e) {
            throw new SerializationException("Incomplete HELLO payload", e);
        } catch (Exception e) {
            throw new SerializationException("Failed to decode HELLO payload", e);
        }
    }

    public static byte[] encodeMemberSnapshot(String groupId, List<NeighborConnection> members)
            throws SerializationException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            writeString(dos, groupId);
            dos.writeInt(members.size());

            for (NeighborConnection member : members) {
                writeString(dos, member.getNodeId());
                writeString(dos, member.getDisplayName());
                writeString(dos, member.getGroupId());
                dos.writeByte(member.getRole().ordinal());
                dos.writeByte(member.getStatus().ordinal());
                dos.writeLong(member.getLastSeen());
            }

            dos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new SerializationException("Failed to encode member snapshot", e);
        }
    }

    public static MemberSnapshot decodeMemberSnapshot(byte[] payload) throws SerializationException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(payload);
             DataInputStream dis = new DataInputStream(bais)) {

            String groupId = readString(dis);
            int memberCount = dis.readInt();
            List<NeighborConnection> members = new ArrayList<>();

            for (int index = 0; index < memberCount; index++) {
                String nodeId = readString(dis);
                String displayName = readString(dis);
                String memberGroupId = readString(dis);
                NodeRole role = NodeRole.values()[dis.readUnsignedByte()];
                PeerStatus status = PeerStatus.values()[dis.readUnsignedByte()];
                long lastSeen = dis.readLong();

                members.add(new NeighborConnection(
                        nodeId,
                        displayName,
                        memberGroupId,
                        role,
                        status,
                        "WIFI_DIRECT",
                        null,
                        lastSeen
                ));
            }

            return new MemberSnapshot(groupId, members);
        } catch (EOFException e) {
            throw new SerializationException("Incomplete member snapshot payload", e);
        } catch (Exception e) {
            throw new SerializationException("Failed to decode member snapshot payload", e);
        }
    }

    private static void writeString(DataOutputStream dos, String value) throws Exception {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        dos.writeShort(bytes.length);
        dos.write(bytes);
    }

    private static String readString(DataInputStream dis) throws Exception {
        short length = dis.readShort();
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static final class HelloPayload {
        private final String displayName;
        private final String groupId;
        private final NodeRole role;

        public HelloPayload(String displayName, String groupId, NodeRole role) {
            this.displayName = displayName;
            this.groupId = groupId;
            this.role = role;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getGroupId() {
            return groupId;
        }

        public NodeRole getRole() {
            return role;
        }
    }

    public static final class MemberSnapshot {
        private final String groupId;
        private final List<NeighborConnection> members;

        public MemberSnapshot(String groupId, List<NeighborConnection> members) {
            this.groupId = groupId;
            this.members = members;
        }

        public String getGroupId() {
            return groupId;
        }

        public List<NeighborConnection> getMembers() {
            return members;
        }
    }
}
