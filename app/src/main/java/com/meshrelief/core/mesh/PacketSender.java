package com.meshrelief.core.mesh;

import com.meshrelief.core.model.Packet;

public interface PacketSender {
    void sendPacket(Packet packet);
    boolean isReady();
}
