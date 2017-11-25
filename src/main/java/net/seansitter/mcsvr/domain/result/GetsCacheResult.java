package net.seansitter.mcsvr.domain.result;

import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheValue;

import java.util.List;
import java.util.Optional;

public class GetsCacheResult extends GetCacheResult {
    public GetsCacheResult(List<CacheEntry<CacheValue>> cacheEntries) {
        super(cacheEntries);
    }

    public GetsCacheResult(Optional<CacheEntry> entry) {
        super(entry);
    }
}
