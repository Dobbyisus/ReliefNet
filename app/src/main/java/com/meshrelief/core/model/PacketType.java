package com.meshrelief.core.model;

public enum PacketType {
    CHAT((byte) 0x01),
    HELLO((byte) 0x02);

    private final byte code;

    PacketType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static PacketType fromCode(byte code) throws SerializationException {
        for (PacketType type : values()) {
            if (type.code == code) {
                return type;
            }
        }

        throw new SerializationException("Unsupported packet type: " + code);
    }
}
