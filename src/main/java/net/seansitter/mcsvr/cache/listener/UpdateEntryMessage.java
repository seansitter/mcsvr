package net.seansitter.mcsvr.cache.listener;

import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheValueStats;

public class UpdateEntryMessage {
    public final CacheEntry<CacheValueStats> oldEntry;
    public final CacheEntry<CacheValueStats> newEntry;
    public final long szChange;

    public UpdateEntryMessage(CacheEntry<CacheValueStats> oldEntry, CacheEntry<CacheValueStats> newEntry, int szChange) {
        this.oldEntry = oldEntry;
        this.newEntry = newEntry;
        this.szChange = szChange;
    }
}
