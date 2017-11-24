package net.seansitter.mcsvr.cache;

import net.seansitter.mcsvr.cache.listener.CacheEventListener;

import com.google.inject.name.Named;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class CacheImpl implements Cache {
    private static final int SECS_IN_30_DAYS = 60*60*24*30;

    private final Map<String, CacheValue> cache;
    private final AtomicLong casCounter;
    private final CacheEventListener eventListener;
    private final ReentrantReadWriteLock lock;

    @Inject
    public CacheImpl(@Named("cache") Map<String, CacheValue> cache,
                     @Named("cacheLock") ReentrantReadWriteLock lock,
                     CacheEventListener eventListener) {
        this.cache = cache;
        // unfair lock is much faster, slight order penalty
        // the system makes no guarantees about the order of operations from unique connections relative to each other
        // rather, operations from a single connection should be totally ordered
        this.lock = lock;
        this.eventListener = eventListener;
        this.casCounter = new AtomicLong(0);
    }

    public void start() {

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
            lock.readLock().unlock(); // need to first release read lock since can't upgrade to write
            lock.writeLock().lock(); //  acquire write lock
            try {
                value = cache.get(key);
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
        finally {
            // release write lock

            if (lock.getReadHoldCount() > 0) {
                lock.readLock().unlock();
            }
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

        lock.readLock().lock();
        try {
            CacheValue cacheValue = cache.get(key);
            if (null == cacheValue) {
                return ResponseStatus.StoreStatus.NOT_FOUND;
            }
            else if (casUnique == cacheValue.getCasUnique()) {
                lock.readLock().unlock();
                lock.writeLock().lock();

                try {
                    cacheValue = cache.get(key);
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
            else {
                return ResponseStatus.StoreStatus.EXISTS;
            }
        }
        finally {
            // release write lock
            if (lock.getReadHoldCount() > 0) {
                lock.readLock().unlock();
            }
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
