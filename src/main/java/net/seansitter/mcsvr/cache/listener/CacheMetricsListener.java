package net.seansitter.mcsvr.cache.listener;

import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheMetrics;
import net.seansitter.mcsvr.cache.CacheValueStats;

import java.util.concurrent.atomic.AtomicLong;

public class CacheMetricsListener implements CacheEventListener, CacheMetrics {
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong cacheSize = new AtomicLong(0);
    private final AtomicLong cacheItems = new AtomicLong(0);

    @Override
    public void sendMessage(EventMessage message) {
        if (message.event == Event.CACHE_HIT) {
            cacheHit();
        } else if (message.event == Event.CACHE_MISS) {
            cacheMiss();
        } else if (message.event == Event.PUT_ENTRY) {
            putEntry((CacheEntry<CacheValueStats>) message.data);
        } else if (message.event == Event.UPDATE_ENTRY) {
            updateEntry((UpdateEntryMessage) message.data);
        } else if (message.event == Event.DELETE_ENTRY) {
            deleteEntry((CacheEntry<CacheValueStats>) message.data);
        } else if (message.event == Event.DESTROY_ENTRIES) {
            destroyEntries((DestroyEntriesMessage) message.data);
        }
    }

    @Override
    public long getHits() {
        return cacheHits.get();
    }

    @Override
    public long getMisses() {
        return cacheMisses.get();
    }

    @Override
    public long getItems() {
        return cacheItems.get();
    }

    @Override
    public long getSize() {
        return cacheSize.get();
    }

    public void cacheHit() {
        cacheHits.incrementAndGet();
    }

    public void cacheMiss() {
        cacheMisses.incrementAndGet();
    }

    public void putEntry(CacheEntry<CacheValueStats> entryMessage) {
        cacheSize.addAndGet(entryMessage.getValue().size);
        cacheItems.incrementAndGet();
    }

    public void updateEntry(UpdateEntryMessage updateEntryMessage) {
        cacheSize.addAndGet(updateEntryMessage.szChange);
    }

    public void deleteEntry(CacheEntry<CacheValueStats> entryMessage) {
        // not concerned about invariants here
        cacheSize.addAndGet((-1) * entryMessage.getValue().size);
        cacheItems.decrementAndGet();
    }

    public void destroyEntries(DestroyEntriesMessage destroyEntriesMessage) {
        // not concerned about invariants here
        cacheSize.addAndGet((-1) * destroyEntriesMessage.entries.size());
        cacheItems.addAndGet((-1) * destroyEntriesMessage.entries.size());
    }
}
