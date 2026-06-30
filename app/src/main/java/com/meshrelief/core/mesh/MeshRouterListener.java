package com.meshrelief.core.mesh;

public interface MeshRouterListener {
    void onChatMessage(String senderName, String message, boolean outgoing);
    void onConnectionEstablished(String neighborName);
    void onConnectionClosed();
    void onError(String error);
}
