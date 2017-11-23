package net.seansitter.mcsvr.domain.result;

import net.seansitter.mcsvr.cache.CacheEntry;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class GetCacheResult implements CacheResult {
    private final List<CacheEntry> cacheEntries;

    public GetCacheResult(List<CacheEntry> cacheEntries) {
        this.cacheEntries = cacheEntries;
    }

    public GetCacheResult(Optional<CacheEntry> entry) {
        cacheEntries = new LinkedList<>();
        entry.ifPresent(e -> cacheEntries.add(e));
    }

    public List<CacheEntry> getCacheEntries() {
        return cacheEntries;
    }
}
