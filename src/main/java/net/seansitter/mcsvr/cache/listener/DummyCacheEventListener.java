package net.seansitter.mcsvr.cache.listener;

/**
 * Cache event listener which ignores all events
 */
public class DummyCacheEventListener implements CacheEventListener {
    @Override
    public void sendMessage(EventMessage message) { }
}
