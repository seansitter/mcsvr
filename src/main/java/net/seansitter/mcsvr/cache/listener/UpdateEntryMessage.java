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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UpdateEntryMessage)) {
            return false;
        }

        UpdateEntryMessage v = (UpdateEntryMessage)o;
        if (v.szChange != szChange) {
            return false;
        }
        if (oldEntry != null && v.oldEntry == null) {
            return false;
        }
        if (v.oldEntry != null && !v.oldEntry.equals(v.oldEntry)) {
            return false;
        }
        if (newEntry != null && v.newEntry == null) {
            return false;
        }
        if (v.newEntry != null && !v.newEntry.equals(v.newEntry)) {
            return false;
        }

        return true;
    }
}
