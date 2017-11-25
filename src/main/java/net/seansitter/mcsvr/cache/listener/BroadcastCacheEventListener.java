package net.seansitter.mcsvr.cache.listener;

import java.util.LinkedList;

public class BroadcastCacheEventListener implements CacheEventListener {
    private final LinkedList<CacheEventListener> listeners = new LinkedList<>();

    public void addListener(CacheEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void sendMessage(EventMessage message) {
        listeners.stream().forEach(l -> l.sendMessage(message));
    }
}
