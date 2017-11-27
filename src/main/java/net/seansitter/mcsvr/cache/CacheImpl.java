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

import static net.seansitter.mcsvr.cache.CacheUtil.*;

/**
 * The implementation of the actual cache
 */
public class CacheImpl implements Cache {
    private final Logger logger = LoggerFactory.getLogger(CacheImpl.class);

    private final Map<String, CacheValue> cache; // the backing cache
    private final int reapInterval; // thread reaper interval in seconds
    private final AtomicLong casCounter; // atomic counter for cas unique value
    private final ReadWriteLock lock; // in non-test this needs to be reentrant
    private final CacheEventListener eventListener;
    private final ScheduledExecutorService schedExecutor; // executor for the reaper
    private long relTime = 0;

    @Inject
    public CacheImpl(@Named("cache") Map<String, CacheValue> cache,
                     @Named("reapInterval") Integer reapInterval,
                     @Named("cacheLock") ReadWriteLock lock,
                     @Named("cacheCleanup") ScheduledExecutorService schedExecutor,
                     CacheEventListener eventListener) {
        this.cache = cache;
        this.reapInterval = reapInterval;
        // This will be an unfair lock, lock is much faster, slight order penalty
        // the system makes no guarantees about the order of operations from unique connections relative to each other
        // rather, operations from a single connection should be totally ordered
        this.lock = lock;
        this.schedExecutor = schedExecutor;
        this.eventListener = eventListener;
        this.casCounter = new AtomicLong(0);
    }

    /**
     * This allows us to synchronize the time for testing
     *
     * @param relTime
     */
    protected void setRelTime(long relTime) {
        this.relTime = relTime;
    }

    /**
     * Starts asynchronous work
     */
    public void start() {
        if (reapInterval > 0) {
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
                newReaperTask(),
                reapInterval * 1000,
                reapInterval * 1000,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new reaper runnable to cleanup expired items
     *
     * @return
     */
    protected Runnable newReaperTask() {
        return () -> {
            // so we have a consistent time for the duration of the sweep
            long currTime = getCurrTime();
            logger.info("running reaper at: " + currTime);

            // first generate the list of expired keys with a read lock
            LinkedList<String> expKeys = new LinkedList<>();
            lock.readLock().lock();
            try {
                cache.forEach((k, v) -> {
                    if (isExpired(v, currTime)) {
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
        logger.debug("got delete request for key: "+key);

        // pre-empt taking a read lock
        if (null == key) {
            return ResponseStatus.DeleteStatus.NOT_FOUND;
        }

        // acquire read lock - try to pre-verify key in read
        lock.readLock().lock();
        try {
            CacheValue value = cache.get(key);
            if (null == value || isExpired(value, getCurrTime())) { // reaper will get it if expired
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
        logger.debug("got destroy request for keys: "+keys.toString());

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

    public Optional<CacheEntry<CacheValue>> get(String key, long currTime) {
        // pre-empt taking a read lock
        if (null == key) {
            return Optional.empty();
        }

       // acquire read lock
       lock.readLock().lock();
       try {
           CacheValue value = cache.get(key);
           if (null == value || isExpired(value, currTime)) { // if its expired, reaper will handle it
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
     * Gets a single key from the cache
     *
     * @param key
     * @return
     */
    @Override
    public Optional<CacheEntry<CacheValue>> get(String key) {
        logger.debug("got get request for keys: "+key.toString());
        return get(key, getCurrTime());
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

        // so we have a consistent time for each get expiration check
        long currTime = getCurrTime();

        // acquire read lock
        lock.readLock().lock();
        try {
             return keys
                     .stream()
                     .map(k -> get(k, currTime))
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
        logger.debug("got cas request for keys: "+key);
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
            long time = getCurrTime();
            // need to re-test item since it may have been removed or updated
            CacheValue newValue = cache.get(key);
            // need to check if it changed since we acquired the read lock
            if (null == newValue || isExpired(newValue, time)) { // check expired since may not have been reaped
                return ResponseStatus.StoreStatus.NOT_FOUND;
            }
            else if (newValue.getCasUnique() != casUnique) {
                // key exists and casUnique doesn't match
                return ResponseStatus.StoreStatus.EXISTS;
            }
            else  {
                newValue = newCacheValue(value, ttl, flag, casCounter.incrementAndGet());
                CacheValue oldValue = cache.put(key, newValue);

                // notify listeners
                eventListener.sendMessage(
                    EventMessage.update(newStatsEntry(key, oldValue), newStatsEntry(key, newValue))
                );

                return ResponseStatus.StoreStatus.STORED;
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
        logger.debug("got get request for keys: "+key);

        // pre-empt attempting to store expired ttl
        // note we can't do this in cas because we need the actual state for the response
        long time = getCurrTime();
        if (isExpired(ttl, getCurrTime())) {
            logger.error("attempt to store item with expired ttl, client clock not synced? " + ttl + " < " + time);
            return ResponseStatus.StoreStatus.NOT_STORED; // why store a cache item thats already expired!
        }

        // acquire write lock
        lock.writeLock().lock();
        try {
            // get the new time
            time = getCurrTime();

            // re-check ttl
            if (isExpired(ttl, time)) {
                logger.error("attempt to store item with expired ttl, client clock not synced? " + ttl + " < " + time);
                return ResponseStatus.StoreStatus.NOT_STORED; // why store a cache item thats already expired!
            }

            CacheValue newValue = newCacheValue(value, ttl, flag, casCounter.incrementAndGet());
            CacheValue oldValue = cache.put(key, newValue);

            // notify listeners
            if (null == oldValue) {
                // we didn't have this key or it was expired (not reaped), it's an put
                eventListener.sendMessage(EventMessage.put(newStatsEntry(key, newValue)));
            }
            else if (isExpired(oldValue, time)) {
                eventListener.sendMessage(EventMessage.delete(newStatsEntry(key, oldValue)));
                eventListener.sendMessage(EventMessage.put(newStatsEntry(key, oldValue)));
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

    protected long getCurrTime() {
        return getCurrTime(0);
    }

    protected long getCurrTime(long delta) {
        long t = relTime > 0 ? relTime : CacheUtil.getCurrTime();
        return t + delta;
    }

    protected boolean isExpired(long ttl) {
        return CacheUtil.isExpired(ttl, getCurrTime());
    }

    protected boolean isExpired(long ttl, long currTime) {
        return CacheUtil.isExpired(ttl, currTime);
    }

    protected boolean isExpired(CacheValue v, long currTime) {
        return CacheUtil.isExpired(v, currTime);
    }

    protected boolean isExpired(CacheValue v) {
        return CacheUtil.isExpired(v, getCurrTime());
    }
}
