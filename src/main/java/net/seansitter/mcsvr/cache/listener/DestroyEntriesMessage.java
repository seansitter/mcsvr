package net.seansitter.mcsvr.cache.listener;

import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheValueStats;

import java.util.LinkedList;
import java.util.List;

public class DestroyEntriesMessage {
    public final List<CacheEntry<CacheValueStats>> entries;
    public final int szChange;

    public DestroyEntriesMessage(List<CacheEntry<CacheValueStats>> entries, int szChange) {
        this.entries = entries == null ? new LinkedList<>() : entries;
        this.szChange = szChange;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DestroyEntriesMessage)) {
            return false;
        }

        DestroyEntriesMessage dm = (DestroyEntriesMessage)o;
        if (dm.szChange != szChange) {
            return false;
        }
        if (!entries.equals(dm.entries)) {
            return false;
        }

        return true;
    }
}
