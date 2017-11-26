package net.seansitter.mcsvr.cache;

import io.netty.util.CharsetUtil;
import org.junit.Test;
import static org.junit.Assert.*;

public class CacheValueTest {
    @Test
    public void testCacheValueStats() {
        byte[] payload = "hello".getBytes(CharsetUtil.UTF_8);
        CacheValue v = new CacheValue(payload, 10, 11, 12, 13);
        CacheValueStats s = v.getStats();

        assertEquals(11, s.createdAt);
        assertEquals(12, s.expiresAt);
        assertEquals(payload.length, s.size);
    }

    @Test
    public void testCacheValuePayloadSize() {
        byte[] payload = "hello".getBytes(CharsetUtil.UTF_8);
        CacheValue v = new CacheValue(payload, 10, 11, 12, 13);
        assertEquals(v.getSize(), payload.length);
        assertEquals(10, v.getFlag());
        assertEquals(11, v.getCreatedAt());
        assertEquals(12, v.getExpiresAt());
        assertEquals(13, v.getCasUnique());
    }
}
