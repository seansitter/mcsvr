package net.seansitter.mcsvr.cache;

import net.seansitter.mcsvr.cache.listener.*;

import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;

public class CacheImpl implements Cache {
    private final Logger logger = LoggerFactory.getLogger(CacheImpl.class);

    private static final int SECS_IN_30_DAYS = 60*60*24*30;

    private final Map<String, CacheValue> cache; // the backing cache
    private final int reapInterval; // thread reaper interval in seconds
    private final AtomicLong casCounter; // atomic counter for cas unique value
    private final ReadWriteLock lock; // in non-test this needs to be reentrant
    private final CacheEventListener eventListener;
    private final ScheduledExecutorService schedExecutor; // executor for the reaper
    private final boolean disableReaper;

    @Inject
    public CacheImpl(@Named("cache") Map<String, CacheValue> cache,
                     @Named("reapInterval") Integer reapInterval,
                     @Named("cacheLock") ReadWriteLock lock,
                     @Named("cacheCleanup") ScheduledExecutorService schedExecutor,
                     @Named("disableReaper") boolean disableReaper,
                     CacheEventListener eventListener) {
        this.cache = cache;
        this.reapInterval = reapInterval;
        // This will be an unfair lock, lock is much faster, slight order penalty
        // the system makes no guarantees about the order of operations from unique connections relative to each other
        // rather, operations from a single connection should be totally ordered
        this.lock = lock;
        this.schedExecutor = schedExecutor;
        this.eventListener = eventListener;
        this.disableReaper = disableReaper;
        this.casCounter = new AtomicLong(0);
    }

    /**
     * Starts asynchronous work
     */
    public void start() {
        if (!disableReaper) {
            logger.info("starting reaper...");
            scheduleCleanup();
        }
        else {
            logger.info("reaper has been disabled");
        }
    }

    /**
     * Schedules the reaper thread
     */
    private void scheduleCleanup() {
        logger.info("scheduling reaper thread every "+reapInterval+" seconds");
        schedExecutor.scheduleAtFixedRate(
                newCleanupTask(),
                reapInterval * 1000,
                reapInterval * 1000,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new reaper runnable to cleanup expired items
     *
     * @return
     */
    protected Runnable newCleanupTask() {
        return () -> {
            long currTime = getCurrTime();
            logger.info("running reaper at: "+currTime);

            // first generate the list of expired keys with a read lock
            LinkedList<String> expKeys = new LinkedList<>();
            lock.readLock().lock();
            try {
                cache.forEach((k, v) -> {
                    // need to recheck since it could have gone away
                    if (v.getExpiresAt() < currTime) {
                        expKeys.add(k);
                    }
                });
            } finally {
                lock.readLock().unlock();
            }

            // no expired keys
            if (expKeys.size() == 0) {
                return;
            }

            destroyKeys(expKeys);
        };
    }

    /**
     * Removes a key from the cache
     *
     * @param key
     * @return
     */
    @Override
    public ResponseStatus.DeleteStatus deleteKey(String key) {
        // pre-empt taking a read lock
        if (null == key) {
            return ResponseStatus.DeleteStatus.NOT_FOUND;
        }

        // acquire read lock - try to pre-verify key in read
        lock.readLock().lock();
        try {
            CacheValue value = cache.get(key);
            if (null == value) {
                return ResponseStatus.DeleteStatus.NOT_FOUND; // no key
            }
        }
        finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock(); //  acquire write lock
        try {
            CacheValue value = cache.get(key);
            if (null == value) { // re-check, could have been deleted in the meantime
                return ResponseStatus.DeleteStatus.NOT_FOUND;
            }

            cache.remove(key); // actually remove the item
            eventListener.sendMessage(EventMessage.delete(newStatsEntry(key, value)));

            return ResponseStatus.DeleteStatus.DELETED;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * This is not a memcache api call - for maintainance only!
     *
     * @param keys list of keys to remove from the cache
     * @return
     */
    @Override
    public List<CacheEntry<CacheValueStats>> destroyKeys(List<String> keys) {
        // pre-empt taking a write lock
        if (null == keys || keys.isEmpty()) {
            // return empty list
            return Collections.unmodifiableList(new LinkedList<>());
        }

        // acquire write lock
        lock.writeLock().lock();
        LinkedList<CacheEntry<CacheValueStats>> deletedEntries = new LinkedList<>();
        try {
            int delSz = 0;
            int delCt = 0;
            for (int i=0; i < keys.size(); i++) {
                CacheValue value = cache.remove(keys.get(i));
                if (null != value) {
                    deletedEntries.add(newStatsEntry(keys.get(i), value));
                    delSz += value.getSize();
                    delCt += 1;
                }
            }
            logger.info("destroyed "+delCt+" items(s) totaling "+delSz+" bytes");

            eventListener.sendMessage(
                    EventMessage.newEventMessage(
                            Event.DESTROY_ENTRIES,
                            new DestroyEntriesMessage(Collections.unmodifiableList(deletedEntries), delSz))
            );
        }
        finally {
            lock.writeLock().unlock();
        }

        return Collections.unmodifiableList(deletedEntries);
    }

    /**
     * Gets a single key from the cache
     *
     * @param key
     * @return
     */
    @Override
    public Optional<CacheEntry<CacheValue>> get(String key) {
        // pre-empt taking a read lock
        if (null == key) {
            return Optional.empty();
        }

       // acquire read lock
       lock.readLock().lock();
       try {
           CacheValue value = cache.get(key);
           if (null == value ||
                   (value.getExpiresAt() > 0 && getCurrTime() > value.getExpiresAt())) // optimization in case expired
           {
               eventListener.sendMessage(EventMessage.cacheMiss(key));
               return Optional.empty();
           }

           CacheEntry<CacheValue> entry = new CacheEntry<>(key, value);
           eventListener.sendMessage(EventMessage.cacheHit(newStatsEntry(key, value)));

           return Optional.of(entry);
       }
       finally {
           lock.readLock().unlock();
       }
    }

    /**
     * Bulk gets values from the cache
     *
     * @param keys list of keys to get
     * @return
     */
    @Override
    public List<CacheEntry<CacheValue>> get(List<String> keys) {
        // pre-empt taking a read lock
        if (null == keys || keys.isEmpty()) {
            return new LinkedList<>();
        }

        // acquire read lock
        lock.readLock().lock();
        try {
             return keys
                     .stream()
                     .map(k -> get(k))
                     .filter(v -> v.isPresent())
                     .map(v -> v.get())
                     .collect(Collectors.toList());
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Sets a value in the cache only if cas unique value matches
     *
     * @param key
     * @param value
     * @param ttl
     * @param casUnique
     * @param flag
     * @return
     */
    @Override
    public ResponseStatus.StoreStatus cas(String key, byte[] value, long ttl, long casUnique, long flag) {
        // pre-empt taking a read lock
        if (null == key) {
            return ResponseStatus.StoreStatus.NOT_FOUND;
        }

        // first we'll test under a read lock, since this is relatively cheap
        lock.readLock().lock();
        try {
            CacheValue cacheValue = cache.get(key);
            if (null == cacheValue) {
                return ResponseStatus.StoreStatus.NOT_FOUND;
            }
            else if (casUnique != cacheValue.getCasUnique()) {
                return ResponseStatus.StoreStatus.EXISTS;
            }
        }
        finally {
            lock.readLock().unlock();
        }

        // found writeable item, acquire write lock
        lock.writeLock().lock();
        try {
            // need to re-test item since it may have been removed or updated
            CacheValue newValue = cache.get(key);
            // need to check if it changed since we acquired the read lock
            if (null == newValue || isExpired(newValue)) { // check expired since may not have been reaped
                return ResponseStatus.StoreStatus.NOT_FOUND;
            }
            else if (newValue.getCasUnique() == casUnique) {
                newValue = newCacheValue(value, ttl, flag);
                CacheValue oldValue = cache.put(key, newValue);

                // notify listeners
                eventListener.sendMessage(
                        EventMessage.update(newStatsEntry(key, oldValue), newStatsEntry(key, newValue)));

                return ResponseStatus.StoreStatus.STORED;
            }
            else {
                // key exists and casUnique doesn't match
                return ResponseStatus.StoreStatus.EXISTS;
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Sets a value in the cache, replacing if it exists
     *
     * @param key
     * @param value
     * @param ttl
     * @param flag from the memcache protocol
     * @return
     */
    @Override
    public ResponseStatus.StoreStatus set(String key, byte[] value, long ttl, long flag) {
        // acquire write lock
        lock.writeLock().lock();
        try {
            CacheValue newValue = newCacheValue(value, ttl, flag);
            CacheValue oldValue = cache.put(key, newValue);

            // notify listeners
            if (null == oldValue || isExpired(oldValue)) {
                // we didn't have this key or it was expired (not reaped), it's an put
                eventListener.sendMessage(EventMessage.put(newStatsEntry(key, newValue)));
            }
            else {
                // we didn't have this key, it's an update
                eventListener.sendMessage(
                        EventMessage.update(newStatsEntry(key, oldValue), newStatsEntry(key, newValue)));
            }
        }
        finally {
            // release write lock
            lock.writeLock().unlock();
        }

        return ResponseStatus.StoreStatus.STORED;
    }

    /**
     * Normalizes ttl per memcache protocol.
     * Values less than number of seconds in 30 days are treated as absolute.
     *
     * @param ttl
     * @param currTime
     * @return
     */
    private long normalizeTtl(long ttl, long currTime) {
        if (ttl > SECS_IN_30_DAYS) {
            return ttl;
        }
        if (ttl == 0) {
            return 0;
        }

        return currTime + ttl;
    }

    /**
     * Helper to create a new cache value
     *
     * @param value
     * @param ttl
     * @param flag flag from request, per memcache protocol
     * @return
     */
    private CacheValue newCacheValue(byte[] value, long ttl, long flag) {
        long createdAt = getCurrTime();
        long casUnique = casCounter.incrementAndGet();
        long nTtl = normalizeTtl(ttl, createdAt);
        return new CacheValue(value, flag, createdAt, nTtl, casUnique);
    }

    /**
     * Heler to get current time in epoch seconds
     *
     * @return
     */
    private long getCurrTime() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * Helper to determine if an cache value is expired
     *
     * @param value
     * @return
     */
    private boolean isExpired(CacheValue value) {
        return isExpired(value, getCurrTime());
    }

    /**
     * Helper to determine if an cache value is expired
     *
     * @param value
     * @param currTime the time relative to the expiration
     * @return
     */
    private boolean isExpired(CacheValue value, long currTime) {
        return value.getExpiresAt() < currTime;
    }

    /**
     * Converts a cache entry with a payload into a cache entry with stats only,
     * for when no payload is necessary, like for listeners
     *
     * @param key
     * @param value
     * @return
     */
    private CacheEntry<CacheValueStats> newStatsEntry(String key, CacheValue value) {
        return new CacheEntry<>(key, value.getStats());
    }
}
