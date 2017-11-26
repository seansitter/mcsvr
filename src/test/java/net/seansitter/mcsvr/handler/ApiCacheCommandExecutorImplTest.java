package net.seansitter.mcsvr.handler;

import io.netty.util.CharsetUtil;
import net.seansitter.mcsvr.cache.Cache;
import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheValue;
import net.seansitter.mcsvr.cache.ResponseStatus;
import net.seansitter.mcsvr.domain.command.DeleteCommand;
import net.seansitter.mcsvr.domain.command.GetCommand;
import net.seansitter.mcsvr.domain.command.StoreCommand;
import net.seansitter.mcsvr.domain.result.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ApiCacheCommandExecutorImplTest {
    ApiCacheCommandExecutor cmdExec;
    Cache cache;

    @Before
    public void setup() {
       cache = mock(Cache.class);
       cmdExec = new ApiCacheCommandExecutorImpl(cache);
    }

    @Test
    public void testGets() {
        long t = getTime();

        GetCommand c = GetCommand.newBuilder()
                .withName("gets")
                .withKey("first_key")
                .withKey("second_key")
                .build();

        //List<CacheEntry<CacheValue>>
        CacheValue v1 = new CacheValue("first_value".getBytes(CharsetUtil.UTF_8), 0, t, t+15, 101);
        CacheEntry<CacheValue> e1 = new CacheEntry<>("first_key", v1);
        CacheValue v2 = new CacheValue("second_value".getBytes(CharsetUtil.UTF_8), 0, t, t+16, 102);
        CacheEntry<CacheValue> e2 = new CacheEntry<>("second_key", v2);

        when(cache.get(c.getKeys())).thenReturn(Arrays.asList(e1, e2));
        CacheResult r = cmdExec.execute(c);

        assertTrue("check we got a gets cache result", r instanceof GetsCacheResult);

        GetsCacheResult gr = (GetsCacheResult)r;
        List<CacheEntry<CacheValue>> resEntries = gr.getCacheEntries();

        assertEquals(2, resEntries.size());
        assertEquals("first_key", resEntries.get(0).getKey());
        assertEquals("second_key", resEntries.get(1).getKey());
    }

    @Test
    public void testGet() {
        long t = getTime();

        GetCommand c = GetCommand.newBuilder()
                .withName("gets")
                .withKey("first_key")
                .withKey("second_key")
                .build();

        CacheValue v1 = new CacheValue("first_value".getBytes(CharsetUtil.UTF_8), 0, t, t+15, 101);
        CacheEntry<CacheValue> e1 = new CacheEntry<>("first_key", v1);
        CacheValue v2 = new CacheValue("second_value".getBytes(CharsetUtil.UTF_8), 0, t, t+16, 102);
        CacheEntry<CacheValue> e2 = new CacheEntry<>("second_key", v2);

        when(cache.get(c.getKeys())).thenReturn(Arrays.asList(e1, e2));
        CacheResult r = cmdExec.execute(c);

        assertTrue("check we got a gets cache result", r instanceof GetCacheResult);

        GetCacheResult gr = (GetCacheResult)r;
        List<CacheEntry<CacheValue>> resEntries = gr.getCacheEntries();

        assertEquals(2, resEntries.size());
        assertEquals("first_key", resEntries.get(0).getKey());
        assertEquals("second_key", resEntries.get(1).getKey());
    }

    @Test
    public void testSet() {
        long t = getTime();

        StoreCommand c = StoreCommand.newBuilder()
                .withExpTime(t+15)
                .withFlags(20)
                .withIsNoReploy(false)
                .withPayload("hello".getBytes(CharsetUtil.UTF_8))
                .withKey("first_key")
                .withName("set")
                .build();

        when(cache.set(c.getKey(), c.getPayload(), c.getExpTime(), c.getFlags())).thenReturn(ResponseStatus.StoreStatus.STORED);
        CacheResult r = cmdExec.execute(c);

        assertTrue("check we got a store cache result", r instanceof StoreCacheResult);
        StoreCacheResult sr = (StoreCacheResult)r;

        assertEquals("check store succeeded", sr.getStatus(), ResponseStatus.StoreStatus.STORED);
    }

    @Test
    public void testCas() {
        long t = getTime();

        StoreCommand c = StoreCommand.newBuilder()
                .withExpTime(t+15)
                .withFlags(20)
                .withIsNoReploy(false)
                .withPayload("hello".getBytes(CharsetUtil.UTF_8))
                .withKey("first_key")
                .withName("cas")
                .withCasUnique(15)
                .build();

        when(cache.cas(c.getKey(), c.getPayload(), c.getExpTime(), c.getCasUnique(), c.getFlags())).thenReturn(ResponseStatus.StoreStatus.STORED);
        CacheResult r = cmdExec.execute(c);

        assertTrue("check we got a store cache result", r instanceof StoreCacheResult);
        StoreCacheResult sr = (StoreCacheResult)r;

        assertEquals("check store succeeded", sr.getStatus(), ResponseStatus.StoreStatus.STORED);
    }

    @Test
    public void testDelete() {
        DeleteCommand c = DeleteCommand.newBuilder().withKey("some_key").withIsNoReply(false).build();

        when(cache.deleteKey("some_key")).thenReturn(ResponseStatus.DeleteStatus.DELETED);
        CacheResult r = cmdExec.execute(c);

        assertTrue("check we got a delete cache result", r instanceof DeleteCacheResult);
        DeleteCacheResult dr = (DeleteCacheResult)r;
        assertEquals("statis is deleted", dr.getStatus(), ResponseStatus.DeleteStatus.DELETED);
    }

    long getTime() {
        return System.currentTimeMillis() / 1000;
    }
}
