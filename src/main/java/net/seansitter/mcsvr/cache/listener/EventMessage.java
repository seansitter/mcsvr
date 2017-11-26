package net.seansitter.mcsvr.cache.listener;

import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheValueStats;

import java.util.Arrays;
import java.util.List;

public class EventMessage {
    public final Event event;
    public final Object data;

    private EventMessage(Event event, Object data) {
        this.event = event;
        this.data = data;
    }

    public static EventMessage newEventMessage(Event event, Object data) {
         return new EventMessage(event, data);
    }

    public static EventMessage cacheHit(CacheEntry<CacheValueStats> cacheEntry) {
        return new EventMessage(Event.CACHE_HIT, cacheEntry);
    }

    public static EventMessage cacheMiss(String key) {
        return new EventMessage(Event.CACHE_MISS, key);
    }

    public static EventMessage put(CacheEntry<CacheValueStats> cacheEntry) {
        return new EventMessage(Event.PUT_ENTRY, cacheEntry);
    }

    public static EventMessage update(CacheEntry<CacheValueStats> oldEntry, CacheEntry<CacheValueStats> newEntry) {
        int szChange = oldEntry.getValue().size - newEntry.getValue().size;
        UpdateEntryMessage msg = new UpdateEntryMessage(oldEntry, newEntry, szChange);
        return new EventMessage(Event.UPDATE_ENTRY, msg);
    }

    public static EventMessage delete(CacheEntry<CacheValueStats> cacheEntry) {
        return new EventMessage(Event.DELETE_ENTRY, cacheEntry);
    }

    public static EventMessage destroy(CacheEntry<CacheValueStats>... cacheEntryList) {
        return destroy(Arrays.asList(cacheEntryList).stream().mapToInt(e -> e.getValue().size).sum(), cacheEntryList);
    }

    public static EventMessage destroy(int sz, CacheEntry<CacheValueStats>... cacheEntryList) {
        List<CacheEntry<CacheValueStats>> e = Arrays.asList(cacheEntryList);
        return new EventMessage(Event.DESTROY_ENTRIES, new DestroyEntriesMessage(e, sz));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EventMessage)) {
            return false;
        }

        EventMessage om = (EventMessage)o;
        if (event == null && om.event != null) {
            return false;
        }
        if (event != null && !event.equals(om.event)) {
            return false;
        }
        if (data == null && om.data != null) {
            return false;
        }
        if (data != null && !data.equals(om.data)) {
            return false;
        }

        return true;
    }
}
