package net.seansitter.mcsvr.cache;

import net.seansitter.mcsvr.cache.listener.CacheEventListener;

import com.google.inject.name.Named;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;

public class CacheImpl implements Cache {
    private static final int SECS_IN_30_DAYS = 60*60*24*30;

    private final Map<String, CacheValue> cache;
    private final AtomicLong casCounter;
    private final ReadWriteLock lock; // in non-test this needs to be reentrant
    private final CacheEventListener eventListener;
    private final ScheduledExecutorService schedExecutor;

    @Inject
    public CacheImpl(@Named("cache") Map<String, CacheValue> cache,
                     @Named("cacheLock") ReadWriteLock lock,
                     @Named("cacheCleanup") ScheduledExecutorService schedExecutor,
                     CacheEventListener eventListener) {
        this.cache = cache;
        // This will be an unfair lock, lock is much faster, slight order penalty
        // the system makes no guarantees about the order of operations from unique connections relative to each other
        // rather, operations from a single connection should be totally ordered
        this.lock = lock;
        this.schedExecutor = schedExecutor;
        this.eventListener = eventListener;
        this.casCounter = new AtomicLong(0);
    }

    public void start() {
        scheduleCleanup();
    }

    private void scheduleCleanup() {
        schedExecutor.scheduleAtFixedRate(
                newCleanupTask(),
                10000,
                10000,
                TimeUnit.MILLISECONDS);
    }

    protected Runnable newCleanupTask() {
        return () -> {
            long currTime = getCurrTime();

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

            // remove each expired key in a read lock
            lock.writeLock().lock();
            try {
                if (expKeys.size() > 0) {
                    expKeys.forEach(k -> {
                        if (cache.containsKey(k)) {
                            cache.remove(k);
                        }
                    });
                }
            } finally {
                lock.writeLock().unlock();
            }
        };
    }

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
            eventListener.deleteEntry(new CacheEntry(key, value));

            return ResponseStatus.DeleteStatus.DELETED;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    // this is not an api call
    @Override
    public List<CacheEntry> destroyKeys(List<String> keys) {
        // pre-empt taking a write lock
        if (null == keys || keys.isEmpty()) {
            // return empty list
            return Collections.unmodifiableList(new LinkedList<>());
        }

        // acquire write lock
        lock.writeLock().lock();
        LinkedList<CacheEntry> deletedEntries = new LinkedList<>();
        try {
            int delSz = 0;
            for (int i=0; i < keys.size(); i++) {
                CacheValue value = cache.remove(keys.get(i));
                if (null != value) {
                    deletedEntries.add(new CacheEntry(keys.get(i), value));
                    delSz += value.getSize();
                }
            }

            eventListener.destroyEntries(deletedEntries, delSz);
        }
        finally {
            lock.writeLock().unlock();
        }

        return Collections.unmodifiableList(deletedEntries);
    }

    @Override
    public Optional<CacheEntry> get(String key) {
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
               eventListener.cacheMiss(key);
               return Optional.empty();
           }

           CacheEntry entry = new CacheEntry(key, value);
           eventListener.cacheHit(entry);

           return Optional.of(entry);
       }
       finally {
           lock.readLock().unlock();
       }
    }

    @Override
    public List<CacheEntry> get(List<String> keys) {
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
            CacheValue cacheValue = cache.get(key);
            // need to check if it changed since we acquired the read lock
            if (null == cacheValue) {
                return ResponseStatus.StoreStatus.NOT_FOUND;
            }
            else if (cacheValue.getCasUnique() == casUnique) {
                cacheValue = newCacheValue(value, ttl, flag);
                cache.put(key, cacheValue);
                // notify listeners
                eventListener.updateEntry(new CacheEntry(key, cacheValue));
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

    @Override
    public ResponseStatus.StoreStatus set(String key, byte[] value, long ttl, long flag) {
        // acquire write lock
        lock.writeLock().lock();
        try {
            CacheValue cacheValue = newCacheValue(value, ttl, flag);
            cache.put(key, cacheValue);
            eventListener.putEntry(new CacheEntry(key, cacheValue));
        }
        finally {
            // release write lock
            lock.writeLock().unlock();
        }

        return ResponseStatus.StoreStatus.STORED;
    }

    private long normalizeTtl(long ttl, long currTime) {
        if (ttl > SECS_IN_30_DAYS) {
            return ttl;
        }
        if (ttl == 0) {
            return 0;
        }

        return currTime + ttl;
    }

    private CacheValue newCacheValue(byte[] value, long ttl, long flag) {
        long createdAt = getCurrTime();
        long casUnique = casCounter.incrementAndGet();
        return new CacheValue(value, flag, createdAt, normalizeTtl(ttl, createdAt), casUnique);
    }

    private long getCurrTime() {
        return System.currentTimeMillis() / 1000;
    }
}
