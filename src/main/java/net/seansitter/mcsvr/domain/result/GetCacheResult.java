package net.seansitter.mcsvr.domain.result;

import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheValue;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GetCacheResult implements CacheResult {
    private final List<CacheEntry<CacheValue>> cacheEntries;

    public GetCacheResult(List<CacheEntry<CacheValue>> cacheEntries) {
        this.cacheEntries = cacheEntries;
    }

    public GetCacheResult(Optional<CacheEntry> entry) {
        cacheEntries = new LinkedList<>();
        entry.ifPresent(e -> cacheEntries.add(e));
    }

    @Override
    public String toString() {
        return "get"+toStringBase();
    }

    String toStringBase() {
        return " found "+cacheEntries.size()+" keys: "+String.join(" ",
                cacheEntries.stream().map(e -> e.getKey()).collect(Collectors.toList()));
    }

    public List<CacheEntry<CacheValue>> getCacheEntries() {
        return cacheEntries;
    }
}
