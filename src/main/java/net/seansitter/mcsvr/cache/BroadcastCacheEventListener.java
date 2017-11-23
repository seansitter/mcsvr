package net.seansitter.mcsvr.cache;

import java.util.LinkedList;
import java.util.List;

public class BroadcastCacheEventListener implements CacheEventListener {
    private final LinkedList<CacheEventListener> listeners = new LinkedList<>();

    public void addListener(CacheEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void cacheHit(CacheEntry entry) {
        listeners.stream().forEach(l -> l.cacheHit(entry));
    }

    @Override
    public void cacheMiss(String key) {
        listeners.stream().forEach(l -> l.cacheMiss(key));
    }

    @Override
    public void putEntry(CacheEntry entry) {
        listeners.stream().forEach(l -> l.putEntry(entry));
    }

    @Override
    public void updateEntry(CacheEntry entry) {
        listeners.stream().forEach(l -> l.updateEntry(entry));
    }

    @Override
    public void deleteEntry(CacheEntry entry) {
        listeners.stream().forEach(l -> l.deleteEntry(entry));
    }

    @Override
    public void destroyEntries(List<CacheEntry> entries, int decrSzBy) {
        listeners.stream().forEach(l -> l.destroyEntries(entries, decrSzBy));
    }
}
