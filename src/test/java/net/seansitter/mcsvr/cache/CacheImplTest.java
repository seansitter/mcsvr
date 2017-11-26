package net.seansitter.mcsvr.cache;

import io.netty.util.CharsetUtil;
import net.seansitter.mcsvr.cache.listener.CacheEventListener;
import net.seansitter.mcsvr.cache.listener.EventMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class CacheImplTest {
    final String DEFKEY = "some_key";
    final String DEFVAL = "some_value";
    final byte[] DEFVAL_B = valueOf(DEFVAL);
    final long DEFFLAG = 15;
    final long DEFTTL = 0;
    final long DEFCASUNQ = 1;
    final long NOW = getTime();
    final CacheValue DEFCVAL = CacheUtil.newCacheValue(DEFVAL_B, DEFTTL, DEFFLAG, DEFCASUNQ, NOW);
    final CacheEntry<CacheValue> DEFCENTRY = new CacheEntry(DEFKEY, DEFCVAL);
    final CacheEntry<CacheValueStats> DEFCSTATENTRY = new CacheEntry<>(DEFKEY, DEFCVAL.getStats());

    ReadWriteLock lock;
    Lock readLock, writeLock;
    Map<String, CacheValue> map;
    CacheEventListener eventListener;
    ScheduledExecutorService executorService;
    CacheImpl cache;

    @Before
    public void setup() {
        lock = mock(ReadWriteLock.class);
        readLock = mock(ReentrantReadWriteLock.ReadLock.class);
        writeLock = mock(ReentrantReadWriteLock.WriteLock.class);
        when(lock.readLock()).thenReturn(readLock);
        when(lock.writeLock()).thenReturn(writeLock);
        map = spy(new HashMap<>());
        eventListener = mock(CacheEventListener.class);
        executorService = mock(ScheduledExecutorService.class);
        cache = new CacheImpl(map, 0, lock, executorService, eventListener);
        cache.setRelTime(NOW);
    }

    @Test
    public void testRelTime1() throws InterruptedException {
        cache.setRelTime(0);
        long t1 = cache.getCurrTime();
        Thread.sleep(1500);
        long t2 = cache.getCurrTime();
        assertNotEquals(t1, t2);
    }

    @Test
    public void testRelTime2() throws InterruptedException {
        cache.setRelTime(NOW);
        long t1 = cache.getCurrTime();
        Thread.sleep(1500);
        long t2 = cache.getCurrTime();
        assertEquals(t1, t2);
        assertEquals(t1, NOW);
    }

    @Test
    public void testSetGet() {
        byte[] payload = valueOf(DEFKEY);

        cache.set(DEFKEY, DEFVAL_B, DEFTTL, DEFFLAG);
        Optional<CacheEntry<CacheValue>> res = cache.get(DEFKEY);

        assertTrue("got an entry back", res.isPresent());

        CacheEntry<CacheValue> e = res.get();
        assertEquals("keys are equal", DEFKEY, e.getKey());
        assertEquals(DEFCVAL, e.getValue());
    }

     // TESTING LOCKS

    @Test
    public void testSetWriteLocks() {
        long ttl = getTime(15);

        byte[] payload = valueOf(DEFKEY);

        cache.set(DEFKEY, payload, ttl, 15);
        verify(writeLock, times(1)).lock();
        verify(writeLock, times(1)).unlock();
    }

    @Test
    public void testGetReadLocks() {
        cache.get(DEFKEY);
        verify(readLock, times(1)).lock();
        verify(readLock, times(1)).unlock();
    }

    @Test
    public void testDelReadWriteLocksNoKey() {
        cache.deleteKey(DEFKEY);
        verify(readLock, times(1)).lock();
        verify(readLock, times(1)).unlock();
        verify(writeLock, never()).lock();
        verify(writeLock, never()).unlock();
    }

    @Test
    public void testDelReadWriteLockKey() {
        cache.set(DEFKEY, valueOf("whatever"), 0, 15);
        verify(writeLock, times(1)).lock();
        verify(writeLock, times(1)).unlock();
        verify(readLock, never()).lock();
        verify(readLock, never()).unlock();

        cache.deleteKey(DEFKEY);
        verify(readLock, times(1)).lock();
        verify(readLock, times(1)).unlock();
        verify(writeLock, times(2)).lock();
        verify(writeLock, times(2)).unlock();
    }

    @Test
    public void testCasLocks() {

    }

    // TESTING EXPIRATION

    @Test
    public void testZeroExpiration() throws InterruptedException {
        cache.set(DEFKEY, DEFVAL_B, 0, 15);
        Thread.sleep(2000);
        Optional<CacheEntry<CacheValue>> res = cache.get(DEFKEY);
        assertEquals("expiration is 0", 0, res.get().getValue().getExpiresAt());
        assertFalse("key is not expired", cache.isExpired(res.get().getValue()));
    }

    @Test
    public void testNonZeroNotExpired() throws InterruptedException {
        long ttl = getTime(5);
        cache.set(DEFKEY, DEFVAL_B, ttl, 15);
        Thread.sleep(2000);
        Optional<CacheEntry<CacheValue>> res = cache.get(DEFKEY);
        assertEquals("expiration is "+ttl, ttl, res.get().getValue().getExpiresAt());
        assertFalse("key is expired", cache.isExpired(res.get().getValue()));
    }

    @Test
    public void testRelativeExpiration() throws InterruptedException {
        long ttl = getTime(5);
        cache.set(DEFKEY, DEFVAL_B, 5, 15);
        Thread.sleep(2000);
        Optional<CacheEntry<CacheValue>> res = cache.get(DEFKEY);
        assertEquals("expiration is "+ttl, ttl, res.get().getValue().getExpiresAt());
        assertFalse("key is expired", cache.isExpired(res.get().getValue()));
    }

    @Test
    public void testRelativeRelExpirationExpired() throws InterruptedException {
        cache.setRelTime(0);
        cache.set(DEFKEY, DEFVAL_B, 2, 15);
        Thread.sleep(3000);
        Optional<CacheEntry<CacheValue>> res = cache.get(DEFKEY);
        assertFalse("expired item is not present", res.isPresent());
    }

    @Test
    public void testRelativeAbsExpirationExpired() throws InterruptedException {
        cache.setRelTime(0);
        cache.set(DEFKEY, DEFVAL_B, getTime(2), 15);
        Thread.sleep(3000);
        Optional<CacheEntry<CacheValue>> res = cache.get(DEFKEY);
        assertFalse("expired item is not present", res.isPresent());
    }

    // TESTING EVENT LISTENER

    @Test
    public void testMissEventListener() {
        cache.get(DEFKEY);
        verify(eventListener, only()).sendMessage(EventMessage.cacheMiss(DEFKEY));
    }

    @Test
    public void testHitEventListener() {
        setDefaultValueInCache();
        reset(eventListener);
        cache.get(DEFKEY);
        verify(eventListener, only()).sendMessage(EventMessage.cacheHit(DEFCSTATENTRY));
    }

    @Test
    public void testSetPutEventListener() {
        setDefaultValueInCache();
        verify(eventListener, only()).sendMessage(EventMessage.put(DEFCSTATENTRY));
    }

    @Test
    public void testSetUpdateEventListener() {
        setDefaultValueInCache();
        reset(eventListener);

        byte[] newPayload = valueOf("new value");
        CacheValue newValue = CacheUtil.newCacheValue(newPayload, DEFTTL, DEFFLAG, DEFCASUNQ);
        cache.set(DEFKEY, newPayload, DEFTTL, DEFFLAG);

        verify(eventListener, only()).sendMessage(
                EventMessage.update(DEFCSTATENTRY, new CacheEntry<>(DEFKEY, newValue.getStats()))
        );
    }

    @Test
    public void testSetUpdateNoChange() {
        setDefaultValueInCache();
        reset(eventListener);
        setDefaultValueInCache();

        verify(eventListener, only()).sendMessage(
                EventMessage.update(DEFCSTATENTRY, DEFCSTATENTRY)
        );
    }

    @Test
    public void testDeleteEventListener() {
        setDefaultValueInCache();
        reset(eventListener);
        cache.deleteKey(DEFKEY);
        verify(eventListener, only()).sendMessage(EventMessage.delete(DEFCSTATENTRY));
    }

    @Test
    public void testDeleteExpiredEventListener() throws InterruptedException {
        cache.setRelTime(0);

        String newKey = "other_key";
        long ttl = getTime() + 1;
        cache.set(newKey, DEFVAL_B, ttl, DEFFLAG);
        Thread.sleep(2000);

        reset(eventListener);
        cache.deleteKey(newKey);
        verifyNoMoreInteractions(eventListener); // delete doesn't happen on an expired key, reaper will destroy key
    }

    @Test
    public void testDestroyOneOfTwoEventListener() {
        setDefaultValueInCache();
        reset(eventListener);
        cache.destroyKeys(Arrays.asList(DEFKEY, "missing_key"));
        verify(eventListener, only())
                .sendMessage(EventMessage.destroy(DEFCSTATENTRY));
    }

    @Test
    public void testDestroyTwoEventListener() {
        setDefaultValueInCache();
        String newKey = "other_key";
        CacheValue newValue = CacheUtil.newCacheValue(DEFVAL_B, DEFTTL, DEFFLAG, DEFCASUNQ+1, NOW);
        cache.set(newKey, DEFVAL_B, DEFTTL, DEFFLAG);

        reset(eventListener);
        cache.destroyKeys(Arrays.asList(DEFKEY, newKey));
        verify(eventListener, only())
                .sendMessage(EventMessage.destroy(
                        DEFCSTATENTRY, new CacheEntry<>(newKey, newValue.getStats())));
    }

    @Test
    public void testCasUpdateEventListener() {
        setDefaultValueInCache();
        CacheEntry<CacheValue> v = cache.get(DEFKEY).get();
        reset(eventListener);
        CacheValue newValue = CacheUtil.newCacheValue(valueOf("new_value"), DEFTTL, DEFFLAG, DEFCASUNQ+1, NOW);
        cache.cas(DEFKEY, valueOf("new_value"), DEFTTL, v.getValue().getCasUnique(), DEFFLAG);

        verify(eventListener, only()).sendMessage(
                EventMessage.update(DEFCSTATENTRY, new CacheEntry<>(DEFKEY, newValue.getStats())));
    }

    @Test
    public void testCasNoUpdateEventListener() {
        setDefaultValueInCache();
        cache.set(DEFKEY, valueOf("second_value"), DEFTTL, DEFFLAG);
        CacheEntry<CacheValue> v = cache.get(DEFKEY).get();
        reset(eventListener);

        // this should not be set because we have the original cas uniqu value
        cache.cas(DEFKEY, valueOf("third_value"), DEFTTL, DEFCASUNQ, DEFFLAG);

        verifyNoMoreInteractions(eventListener);
    }

    @Test
    public void testCasUniqueIncr() {
        setDefaultValueInCache();
        setDefaultValueInCache();
        assertEquals("checking cas unique increments",2, cache.get(DEFKEY).get().getValue().getCasUnique());
    }

    void setDefaultValueInCache() {
        cache.set(DEFKEY, DEFVAL_B, DEFTTL, DEFFLAG);
    }

    byte[] valueOf(String s) {
        return s.getBytes(CharsetUtil.UTF_8);
    }

    long getTime(int delta) {
        return getTime() + delta;
    }

    long getTime() {
        return System.currentTimeMillis() / 1000;
    }
}
