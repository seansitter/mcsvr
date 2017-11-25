package net.seansitter.mcsvr.cache.listener;

import net.seansitter.mcsvr.cache.Cache;
import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheValueStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class represents the LRU manager. It serialized all events to a thread in order to prevent
 * the event broadcaster, and thus the cache itself, from blocking.
 */
public class LRUManagerListener implements CacheEventListener {
    private final Logger logger = LoggerFactory.getLogger(CacheMetricsListener.class);

    @Inject
    public LRUManagerListener(Cache cache, @Named("maxCacheBytes") int maxCacheBytes) {
        consumer = new LRUManager(cache, queue, maxCacheBytes);
    }

    private LRUManager consumer; // consumes cache events via blocking queue
    private volatile boolean startedConsumer; // flag needs to be volatile for visibility
    private final BlockingQueue<EventMessage> queue = new LinkedBlockingQueue<>(); // blocking queue for consumer

    @Override
    public void sendMessage(EventMessage message) {
        lazyStartConsumer();

        if (!queue.offer(message)) {
            // we couldn't add an item to the queue, it must be full!
            logger.error("failed to send message to LRU consumer - full queue?");
        }
    }

    /**
     * Start the lru consumer thread on demand
     */
    private void lazyStartConsumer() {
        if (!startedConsumer) { // double check optimization avoids unnecessary lock
            synchronized (this) {
                if (!startedConsumer) {
                    consumer.start();
                    startedConsumer = true;
                }
            }
        }
    }

    /**
     * This class encapsulates a separate thread which receives cache events through a queue.
     * Executing in a single thread, there is no need to synchronize members.
     *
     * We don't need synchronization here because all operations happen on the same thread!
     */
    public static class LRUManager implements CacheEventListener {
        private final Logger logger = LoggerFactory.getLogger(LRUManager.class);

        private final BlockingQueue<EventMessage> queue; // serialize all events through queue
        private final Cache cache; // so we can order cleanup
        private final int maxSz;
        private final HashMap<String, LRUNode> lruMap = new HashMap<>(); // we want node lookup to be O(1)
        private LRUList lruList; // head of the lease is mru, tail is lru

        private long currSz = 0;

        protected LRUManager(Cache cache, BlockingQueue queue, int maxSz) {
            this.queue = queue;
            this.cache = cache;
            this.maxSz = maxSz;
        }

        protected void start() {
            new Thread(() -> {
                while(true) {
                    try {
                        sendMessage(queue.take());
                    }
                    catch (Exception e) {
                        logger.error("caught exception in lru manager", e);
                    }
                }
            }, "lru-manager-thread").start();
        }

        @Override
        public void sendMessage(EventMessage message) {
            if (message.event == Event.PUT_ENTRY) {
                newEntry((CacheEntry<CacheValueStats>) message.data);
            }
            else if (message.event == Event.CACHE_HIT) {
                touchEntry((CacheEntry<CacheValueStats>)message.data);
            }
            else if (message.event == Event.UPDATE_ENTRY) {
                touchEntry(((UpdateEntryMessage)message.data).newEntry);
            }
            else if (message.event == Event.DELETE_ENTRY) {
                deleteEntry((CacheEntry<CacheValueStats>)message.data);
            }
            else if (message.event == Event.DESTROY_ENTRIES) {
                destroyEntries((DestroyEntriesMessage) message.data);
            }
        }

        protected void destroyEntries(DestroyEntriesMessage msg) {
            msg.entries.forEach(e -> deleteEntry(e));
            logger.info("destroyed items, new cache size is "+currSz+" bytes");
        }

        protected void deleteEntry(CacheEntry<CacheValueStats> e) {
            LRUNode n = lruMap.get(e.getKey());

            if (null == n) {
                // uh-oh, expected item in list
                logger.info("expected a non-null key for lru delete!");
                return;
            }

            currSz -= e.getValue().size;

            // if n is head, point head to next
            if (lruList.head == n) {
                lruList.head = n.next;
            }

            // if n is tail, point tail to prev
            if (lruList.tail == n) {
                lruList.tail = n.prev;
            }

            // removed item from lru list
            if (null != n.prev) {
                n.prev.next = n.next;
                n.next = null;
            }
            if (null != n.next) {
                n.next.prev = n.prev;
                n.prev = null;
            }

            lruMap.remove(e.getKey());
        }

        /**
         * Creates a new entry in the lru for new nodes, puts the item at the
         *
         * @param e
         */
        protected void newEntry(CacheEntry<CacheValueStats> e) {
            LRUNode n = new LRUNode(e.getKey(), e.getValue());

            if (lruMap.containsKey(e.getKey())) {
                logger.error("lru attempted to put a node that already existed for key: "+e.getKey());
                return;
            }

            lruMap.put(e.getKey(), n);

            if (lruList == null) {
                lruList = new LRUList();
                lruList.head = lruList.tail = n;
            }
            else {
                n.next = lruList.head;
                lruList.head.prev = n;
                lruList.head = n;
            }

            currSz += e.getValue().size;

            cleanupLru();
        }

        /**
         * Cleans up nodes so that currSz < maxSz
         */
        protected void cleanupLru() {
            // check size < max
            if (currSz > maxSz) {
                long recoverSz = currSz - maxSz;
                logger.info("cache size is "+currSz+" bytes, over-size by "+recoverSz+" bytes, attempting to recover");
                // advise cache to destroy lru nodes up to maxSz
                cache.destroyKeys(findLruNodes(recoverSz));
            }
        }

        /**
         * Finds the lease reacently used items whose size totals at least recoverSz
         *
         * @param recoverSz
         * @return
         */
        protected List<String> findLruNodes(long recoverSz) {
            if (null == lruList) {
                logger.error("expected a non-null LRU list!");
                return new LinkedList<>();
            }

            List<String> keys = new LinkedList<>();
            LRUNode n = lruList.tail;
            int szAcc = 0;

            do {
                szAcc += n.cacheStats.size;
                keys.add(n.key);
            }
            while (szAcc < recoverSz && null != (n = n.prev)); // work backwards from the tail

            logger.info("found "+keys.size()+" lease recently used nodes totalling "+szAcc+" bytes");
            return keys;
        }

        /**
         * This method is concerned with moving a used node to the head of the lru list
         *
         * @param e
         */
        protected void touchEntry(CacheEntry<CacheValueStats> e) {
            LRUNode n;

            if (null == (n = lruMap.get(e.getKey()))) {
                logger.error("expected to find key '"+e.getKey()+"' in LRU mAP!");
                return;
            }

            // if we're not already the head
            if (lruList.head != n) {
                if (lruList.tail == n) {
                    lruList.tail = n.prev;
                }

                // remove the node
                if (null != n.prev) n.prev.next = n.next;
                if (null != n.next) n.next.prev = n.prev;

                // move node to head
                n.prev = null;
                n.next = lruList.head;
                lruList.head.prev = n;
                lruList.head = n;
            }
        }

        protected LRUList getLRUList() {
            return lruList;
        }

        /**
         * The tail of the lru list is the least recently used cache item, head is most recently used
         */
        protected class LRUList {
            private LRUNode tail = null; // tail is least recently used
            private LRUNode head = null; // head is most recently used
        }

        protected class LRUNode {
            private final String key;
            private final CacheValueStats cacheStats;

            private LRUNode prev;
            private LRUNode next;

            private LRUNode(String key, CacheValueStats cacheStats) {
                this.key = key;
                this.cacheStats = cacheStats;
            }
        }
    }
}
