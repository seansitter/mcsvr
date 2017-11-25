package net.seansitter.mcsvr.cache.listener;

import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheValueStats;

import java.util.List;

public class DestroyEntriesMessage {
    public final List<CacheEntry<CacheValueStats>> entries;
    public final int szChange;

    public DestroyEntriesMessage(List<CacheEntry<CacheValueStats>> entries, int szChange) {
        this.entries = entries;
        this.szChange = szChange;
    }
}
