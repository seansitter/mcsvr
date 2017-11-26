package net.seansitter.mcsvr.cache.listener;

import org.junit.Test;

import static org.mockito.Mockito.*;

public class BroadcastCacheEventListenerTest {
    @Test
    public void testMessagesSent() {
        BroadcastCacheEventListener bl = new BroadcastCacheEventListener();

        CacheEventListener l1 = mock(CacheEventListener.class);
        CacheEventListener l2 = mock(CacheEventListener.class);
        bl.addListener(l1);
        bl.addListener(l2);

        EventMessage eventMessage = EventMessage.newEventMessage(Event.CACHE_HIT, new Object());
        bl.sendMessage(eventMessage);
        verify(l1).sendMessage(eventMessage);
    }
}
