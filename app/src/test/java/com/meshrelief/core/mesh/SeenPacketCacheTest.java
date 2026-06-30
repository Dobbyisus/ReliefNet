package com.meshrelief.core.mesh;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SeenPacketCacheTest {

    @Test
    public void markIfNew_firstPacketAccepted() {
        SeenPacketCache cache = new SeenPacketCache(1000L);
        assertTrue(cache.markIfNew("packet-1"));
    }

    @Test
    public void markIfNew_duplicatePacketRejected() {
        SeenPacketCache cache = new SeenPacketCache(1000L);
        cache.markIfNew("packet-1");

        assertFalse(cache.markIfNew("packet-1"));
    }

    @Test
    public void markIfNew_expiredPacketAcceptedAgain() throws Exception {
        SeenPacketCache cache = new SeenPacketCache(10L);
        cache.markIfNew("packet-1");
        Thread.sleep(30L);

        assertTrue(cache.markIfNew("packet-1"));
    }
}
