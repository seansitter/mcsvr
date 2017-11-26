package net.seansitter.mcsvr.cache.listener;

import net.seansitter.mcsvr.cache.Cache;
import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheValueStats;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import static org.mockito.Mockito.*;

public class LRUManagerListenerTest {
    String DEFAULT_KEY = "thekey";
    int DEFAULT_SIZE = 10;
    Cache mockCache;
    BlockingQueue<EventMessage> blockingQueue = spy(new SynchronousQueue<>());
    LRUManagerListener managerListener;
    LRUManagerListener.LRUManager lruManager;

    @Before
    public void setup() throws InterruptedException {
        mockCache = mock(Cache.class);
        managerListener = new LRUManagerListener(mockCache, blockingQueue, 35, 30);
        lruManager = spy(managerListener.getLruManager());
        managerListener.setLruManager(lruManager);
        managerListener.lazyStartConsumer();
    }

    @Test
    public void testManagerSentMessage() throws InterruptedException {
        EventMessage m = EventMessage.cacheMiss(DEFAULT_KEY);
        managerListener.sendMessage(m);
        verify(blockingQueue).offer(m); // verify it was published to consumer
    }

    @Test
    public void testPutMessageRouting() throws InterruptedException {
        CacheEntry<CacheValueStats> e = defCacheEntry();

        // verify put
        lruManager.sendMessage(EventMessage.put(e));
        verify(lruManager).newEntry(e);
    }

    @Test
    public void testCacheHitMessageRouting() throws InterruptedException {
        // verify update
        CacheEntry<CacheValueStats> e = defCacheEntry();
        lruManager.sendMessage(EventMessage.cacheHit(e));

        verify(lruManager).touchEntry(e);
    }

    @Test
    public void testUpdateMessageRouting() throws InterruptedException {
        // verify update
        CacheEntry<CacheValueStats> e = defCacheEntry();
        CacheEntry<CacheValueStats> newEntry = newCacheEntry(DEFAULT_KEY, DEFAULT_SIZE+1);
        lruManager.sendMessage(EventMessage.update(e, newEntry));

        verify(lruManager).touchEntry(newEntry);
    }

    @Test
    public void testDeleteMessageRouting() throws InterruptedException {
        // verify update
        CacheEntry<CacheValueStats> e = defCacheEntry();
        lruManager.sendMessage(EventMessage.put(e));
        lruManager.sendMessage(EventMessage.delete(e));

        verify(lruManager).deleteEntry(e);
    }

    @Test
    public void testDestroyMessageRouting() throws InterruptedException {
        // verify update
        CacheEntry<CacheValueStats> e = defCacheEntry();
        lruManager.sendMessage(EventMessage.put(e));
        lruManager.sendMessage(EventMessage.destroy(e));

        verify(lruManager).deleteEntry(e);
    }

    @Test
    public void testPutEntry() {
        CacheEntry<CacheValueStats> e1 = newCacheEntry("first_key", 10);
        lruManager.sendMessage(EventMessage.put(e1));

        // check size
        assertEquals("cache is expected size", e1.getValue().size, lruManager.currSize());

        CacheEntry<CacheValueStats> e2 = newCacheEntry("second_key", 8);
        lruManager.sendMessage(EventMessage.put(e2));
        assertEquals("cache is expected size", e1.getValue().size + e2.getValue().size, lruManager.currSize());

        CacheEntry<CacheValueStats> e3 = newCacheEntry("third_key", 15);
        lruManager.sendMessage(EventMessage.put(e3));
        assertEquals("cache is expected size", e1.getValue().size + e2.getValue().size + e3.getValue().size, lruManager.currSize());

        assertLruList(e3, e2, e1);
    }

    @Test
    public void testUpdateEntryOrder() {
        List<CacheEntry<CacheValueStats>> el = putThreeValueList();
        assertLruList(el.get(0), el.get(1), el.get(2));
        lruManager.sendMessage(EventMessage.update(el.get(1), el.get(1)));
        assertLruList(el.get(1), el.get(0), el.get(2));
        lruManager.sendMessage(EventMessage.update(el.get(2), el.get(2)));
        assertLruList(el.get(2), el.get(1), el.get(0));
        lruManager.sendMessage(EventMessage.update(el.get(0), el.get(0)));
        assertLruList(el.get(0), el.get(2), el.get(1));
    }

    @Test
    public void testPutUpdateEntry() {
        List<CacheEntry<CacheValueStats>> el = putThreeValueList();
        lruManager.sendMessage(EventMessage.cacheHit(el.get(1)));
        assertLruList(el.get(1), el.get(0), el.get(2));
        lruManager.sendMessage(EventMessage.cacheHit(el.get(2)));
        assertLruList(el.get(2), el.get(1), el.get(0));
        lruManager.sendMessage(EventMessage.cacheHit(el.get(0)));
        assertLruList(el.get(0), el.get(2), el.get(1));
    }

    @Test
    public void testPutHitEntry() {
        List<CacheEntry<CacheValueStats>> el = putThreeValueList();
        lruManager.sendMessage(EventMessage.cacheHit(el.get(1)));
        assertLruList(el.get(1), el.get(0), el.get(2));
        lruManager.sendMessage(EventMessage.cacheHit(el.get(2)));
        assertLruList(el.get(2), el.get(1), el.get(0));
        lruManager.sendMessage(EventMessage.cacheHit(el.get(0)));
        assertLruList(el.get(0), el.get(2), el.get(1));
    }

    @Test
    public void testListStats() {
        List<CacheEntry<CacheValueStats>> el = putThreeValueList();
        assertEquals("checking list has 3 items", 3, itemCount(lruManager.getLRUList()));
        assertEquals("checking list size", el.get(0).getValue().size + el.get(1).getValue().size + el.get(2).getValue().size,
                totalSize(lruManager.getLRUList()));
    }

    @Test
    public void testDeleteEntry() {
        List<CacheEntry<CacheValueStats>> el = putThreeValueList();
        lruManager.sendMessage(EventMessage.delete(el.get(1)));
        assertEquals("list size is now 2", 2, itemCount(lruManager.getLRUList()));
        assertLruList(el.get(0), el.get(2));
        assertEquals("checking list size", lruManager.currSize(), el.get(0).getValue().size + el.get(2).getValue().size);
    }

    @Test
    public void testSizeAfterUpdateDecr() {
        List<CacheEntry<CacheValueStats>> el = putThreeValueList();
        CacheEntry<CacheValueStats> oldE = el.get(1);
        CacheEntry<CacheValueStats> newE = new CacheEntry<>(
                oldE.getKey(),
                new CacheValueStats(
                        oldE.getValue().createdAt,
                        oldE.getValue().expiresAt,
                        oldE.getValue().size - 3
                )
        );
        lruManager.sendMessage(EventMessage.update(oldE, newE));
        assertEquals("checking size after update",
                el.get(0).getValue().size + el.get(1).getValue().size + el.get(2).getValue().size - 3,
                totalSize(lruManager.getLRUList())
                );
    }

    @Test
    public void testSizeAfterUpdateIncr() {
        List<CacheEntry<CacheValueStats>> el = putThreeValueList();
        CacheEntry<CacheValueStats> oldE = el.get(1);
        CacheEntry<CacheValueStats> newE = new CacheEntry<>(
                oldE.getKey(),
                new CacheValueStats(
                        oldE.getValue().createdAt,
                        oldE.getValue().expiresAt,
                        oldE.getValue().size + 3
                )
        );
        lruManager.sendMessage(EventMessage.update(oldE, newE));
        assertEquals("checking size after update",
                el.get(0).getValue().size + el.get(1).getValue().size + el.get(2).getValue().size + 3,
                totalSize(lruManager.getLRUList())
        );
    }

    @Test
    public void testDestroy() {
        List<CacheEntry<CacheValueStats>> el = putThreeValueList();
        lruManager.sendMessage(EventMessage.destroy(el.get(0), el.get(2)));
        assertLruList(el.get(1));
        assertEquals(el.get(1).getValue().size, totalSize(lruManager.getLRUList()));
    }

    @Test
    public void adviseCacheToDestroy2() {
        lruManager.setMaxSz(30);
        lruManager.setLruRecoverPct(40);
        CacheEntry<CacheValueStats> e1 = newCacheEntry("first_key", 9);
        CacheEntry<CacheValueStats> e2 = newCacheEntry("second_key", 8);
        CacheEntry<CacheValueStats> e3 = newCacheEntry("third_key", 15); // 15 should be left
        lruManager.sendMessage(EventMessage.put(e1));
        lruManager.sendMessage(EventMessage.put(e2));
        lruManager.sendMessage(EventMessage.put(e3));

        verify(mockCache).destroyKeys(Arrays.asList(e1.getKey(), e2.getKey()));
    }

    @Test
    public void adviseCacheToDestroy1() {
        lruManager.setMaxSz(30);
        lruManager.setLruRecoverPct(40);
        CacheEntry<CacheValueStats> e1 = newCacheEntry("first_key", 12);
        CacheEntry<CacheValueStats> e2 = newCacheEntry("second_key", 8);
        CacheEntry<CacheValueStats> e3 = newCacheEntry("third_key", 15); // 15 should be left
        lruManager.sendMessage(EventMessage.put(e1));
        lruManager.sendMessage(EventMessage.put(e2));
        lruManager.sendMessage(EventMessage.put(e3));

        verify(mockCache).destroyKeys(Arrays.asList(e1.getKey()));
    }

    List<CacheEntry<CacheValueStats>> putThreeValueList() {
        CacheEntry<CacheValueStats> e1 = newCacheEntry("first_key", 10);
        CacheEntry<CacheValueStats> e2 = newCacheEntry("second_key", 8);
        CacheEntry<CacheValueStats> e3 = newCacheEntry("third_key", 15);

        lruManager.sendMessage(EventMessage.put(e1));
        lruManager.sendMessage(EventMessage.put(e2));
        lruManager.sendMessage(EventMessage.put(e3));

        // return in order of the list
        return Arrays.asList(e3, e2, e1);
    }

    void assertLruList(CacheEntry<CacheValueStats>... entries) {
        List<CacheEntry<CacheValueStats>> entryList = Arrays.asList(entries);
        LRUManagerListener.LRUManager.LRUNode currNode = lruManager.getLRUList().head;
        int i = 0;

        while(currNode != null) {
            if (i < entryList.size()) {
                assertTrue("checking order on key " + i, entryList.get(i).getKey().equals(currNode.key));
            }
            i += 1;
            currNode = currNode.next;
        }

        assertEquals("checking list sizes", i, entryList.size());
    }

    CacheEntry<CacheValueStats> defCacheEntry() {
        return newCacheEntry(DEFAULT_KEY, DEFAULT_SIZE);
    }

    CacheEntry<CacheValueStats> newCacheEntry(String key, int sz) {
        long time = System.currentTimeMillis() / 1000;
        return new CacheEntry<>(key, new CacheValueStats(time, time + 15, sz));
    }

    long totalSize(List<CacheEntry<CacheValueStats>> entries) {
        return entries.stream().mapToLong(e -> e.getValue().size).sum();
    }

    int itemCount(LRUManagerListener.LRUManager.LRUList l) {
        LRUManagerListener.LRUManager.LRUNode n = l.head;
        int i = 0;
        while (n != null) {
            i += 1;
            n = n.next;
        }
        return i;
    }

    long totalSize(LRUManagerListener.LRUManager.LRUList l) {
        LRUManagerListener.LRUManager.LRUNode n = l.head;
        long sz = 0;
        while (n != null) {
            sz += n.cacheStats.size;
            n = n.next;
        }
        return sz;
    }

    CacheEntry<CacheValueStats> itemAt(LRUManagerListener.LRUManager.LRUList l, int idx) {
        LRUManagerListener.LRUManager.LRUNode n = l.head;
        LRUManagerListener.LRUManager.LRUNode f = null;
        int i = 0;
        while (n != null) {
            if (i == idx) {
                f = n;
            }
            i += 1;
            n = n.next;
        }
        if (null == f) {
            return null;
        }
        return new CacheEntry<>(f.key, f.cacheStats);
    }
}
