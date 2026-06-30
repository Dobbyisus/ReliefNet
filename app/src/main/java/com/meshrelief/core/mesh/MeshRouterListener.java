package com.meshrelief.core.mesh;

import java.util.List;

public interface MeshRouterListener {
    void onChatMessage(String senderName, String recipientName, String message, boolean outgoing);
    void onGroupStateChanged(String statusText, NodeRole role, String groupId, int memberCount, boolean connected);
    void onMembersUpdated(List<NeighborConnection> members);
    void onError(String error);
}
