package net.seansitter.mcsvr.cache;

import java.util.List;

public abstract class CacheEventListenerAdapter implements CacheEventListener {
    @Override
    public void cacheHit(CacheEntry entry) { }

    @Override
    public void cacheMiss(String key) { }

    @Override
    public void putEntry(CacheEntry entry) { }

    @Override
    public void updateEntry(CacheEntry entry) { }

    @Override
    public void deleteEntry(CacheEntry entry) { }

    @Override
    public void destroyEntries(List<CacheEntry> entries, int decrSzBy){ }
}
