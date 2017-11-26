package net.seansitter.mcsvr.cache;

/**
 * Represents a key, value tuple of a cache entry. Can be parameterized
 * with the cache value, or the stats on a cache value.
 * CacheEntry<CacheValue> is stored in the cache
 * CacheEntry<CacheValueStats> is sent to cache event listeners
 *
 * This avoids having to retain the payload longer then necessary.
 *
 * @param <T>
 */
public class CacheEntry<T> {
    private final String key;
    private final T value;

    public CacheEntry(String key, T value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }
}
