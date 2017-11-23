package net.seansitter.mcsvr.cache;

import java.util.List;

public interface CacheEventListener {
    void cacheHit(CacheEntry entry);
    void cacheMiss(String key);
    void putEntry(CacheEntry entry);
    void updateEntry(CacheEntry entry);
    void deleteEntry(CacheEntry entry);
    void destroyEntries(List<CacheEntry> entries, int decrSzBy);
}
