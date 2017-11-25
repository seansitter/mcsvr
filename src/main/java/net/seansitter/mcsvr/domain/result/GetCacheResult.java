package net.seansitter.mcsvr.domain.result;

import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheValue;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class GetCacheResult implements CacheResult {
    private final List<CacheEntry<CacheValue>> cacheEntries;

    public GetCacheResult(List<CacheEntry<CacheValue>> cacheEntries) {
        this.cacheEntries = cacheEntries;
    }

    public GetCacheResult(Optional<CacheEntry> entry) {
        cacheEntries = new LinkedList<>();
        entry.ifPresent(e -> cacheEntries.add(e));
    }

    public List<CacheEntry<CacheValue>> getCacheEntries() {
        return cacheEntries;
    }
}
