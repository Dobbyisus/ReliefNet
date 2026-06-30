package com.meshrelief.core.mesh;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class SeenPacketCache {

    private final long ttlMillis;
    private final Map<String, Long> seenPackets = new LinkedHashMap<>();

    public SeenPacketCache(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    public synchronized boolean markIfNew(String packetId) {
        long now = System.currentTimeMillis();
        evictExpired(now);

        if (seenPackets.containsKey(packetId)) {
            return false;
        }

        seenPackets.put(packetId, now);
        return true;
    }

    public synchronized boolean hasSeen(String packetId) {
        long now = System.currentTimeMillis();
        evictExpired(now);
        return seenPackets.containsKey(packetId);
    }

    public synchronized void clear() {
        seenPackets.clear();
    }

    private void evictExpired(long now) {
        Iterator<Map.Entry<String, Long>> iterator = seenPackets.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > ttlMillis) {
                iterator.remove();
            }
        }
    }
}
