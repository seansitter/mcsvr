package net.seansitter.mcsvr.domain;

import net.seansitter.mcsvr.cache.CacheEntry;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class GetsCacheResult implements CacheResult {
    private final List<CacheEntry> cacheEntries;

    public GetsCacheResult(List<CacheEntry> cacheEntries) {
        this.cacheEntries = cacheEntries;
    }

    public GetsCacheResult(Optional<CacheEntry> entry) {
        cacheEntries = new LinkedList<>();
        entry.ifPresent(e -> cacheEntries.add(e));
    }

    public List<CacheEntry> getCacheEntries() {
        return cacheEntries;
    }
}
