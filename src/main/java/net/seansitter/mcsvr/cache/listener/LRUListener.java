package net.seansitter.mcsvr.cache.listener;

import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheEventListenerAdapter;

import java.util.List;

public class LRUListener extends CacheEventListenerAdapter {
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
