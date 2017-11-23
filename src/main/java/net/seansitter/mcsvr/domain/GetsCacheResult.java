package net.seansitter.mcsvr.domain;

import net.seansitter.mcsvr.cache.CacheEntry;

import java.util.List;
import java.util.Optional;

public class GetsCacheResult extends GetCacheResult {
    public GetsCacheResult(List<CacheEntry> cacheEntries) {
        super(cacheEntries);
    }

    public GetsCacheResult(Optional<CacheEntry> entry) {
        super(entry);
    }
}
