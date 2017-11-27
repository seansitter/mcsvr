package net.seansitter.mcsvr.cache;

import org.junit.Test;
import static net.seansitter.mcsvr.cache.CacheUtil.*;
import static org.junit.Assert.*;

public class CacheUtilTest {
    @Test
    public void testIsExpired() {
        long time = getCurrTime();

        assertTrue(CacheUtil.isExpired(time - 5, time));
        assertFalse(CacheUtil.isExpired(time, time));
        assertFalse(CacheUtil.isExpired(time + 5, time));
        assertFalse(CacheUtil.isExpired(CacheUtil.SECS_IN_30_DAYS - 1, time));
        assertFalse(CacheUtil.isExpired(CacheUtil.SECS_IN_30_DAYS, time));
        assertTrue(CacheUtil.isExpired(CacheUtil.SECS_IN_30_DAYS + 1, time));
    }
}
