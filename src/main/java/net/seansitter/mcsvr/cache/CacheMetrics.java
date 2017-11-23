package net.seansitter.mcsvr.cache;

import java.util.List;

public class CacheMetrics extends CacheEventListenerAdapter {
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private long cacheSize = 0;
    private long cacheItems = 0;

    private Object hitsLock = new Object();
    private Object missLock = new Object();

    public Object mutLock = new Object();

    @Override
    public void cacheHit(CacheEntry entry) {
        synchronized (hitsLock) {
            cacheHits += 1;
        }
    }

    @Override
    public void cacheMiss(String key) {
        synchronized (missLock) {
            cacheMisses += 1;
        }
    }

    @Override
    public void putEntry(CacheEntry entry) {
        synchronized (mutLock) {
            cacheSize += entry.getValue().getSize();
            cacheItems += 1;
        }
    }

    @Override
    public void deleteEntry(CacheEntry entry) {
        synchronized (mutLock) {
            cacheSize -= entry.getValue().getSize();
            cacheItems -= 1;
        }
    }

    @Override
    public void destroyEntries(List<CacheEntry> entries, int decrSzBy) {
        synchronized (mutLock) {
            cacheSize -= 1;
            cacheItems -= entries.size();
        }
    }
}
