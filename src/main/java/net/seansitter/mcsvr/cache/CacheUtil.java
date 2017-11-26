package net.seansitter.mcsvr.cache;

public class CacheUtil {
    private static final int SECS_IN_30_DAYS = 60*60*24*30;

    /**
     * Normalizes ttl per memcache protocol.
     * Values less than number of seconds in 30 days are treated as absolute.
     *
     * @param ttl
     * @param currTime
     * @return
     */
    public static long normalizeTtl(long ttl, long currTime) {
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
    public static CacheValue newCacheValue(byte[] value, long ttl, long flag, long casUnique) {
        return newCacheValue(value, ttl, flag, casUnique, getCurrTime());
    }

    public static CacheValue newCacheValue(byte[] value, long ttl, long flag, long casUnique, long createdAt) {
        long nTtl = normalizeTtl(ttl, createdAt);
        return new CacheValue(value, flag, createdAt, nTtl, casUnique);
    }

    /**
     * Heler to get current time in epoch seconds
     *
     * @return
     */
    public static long getCurrTime() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * Helper to determine if an cache value is expired
     *
     * @param value
     * @return
     */
    public static boolean isExpired(CacheValue value) {
        return isExpired(value, getCurrTime());
    }

    /**
     * Helper to determine if an cache value is expired
     *
     * @param value
     * @param currTime the time relative to the expiration
     * @return
     */
    public static boolean isExpired(CacheValue value, long currTime) {
        return value.getExpiresAt() != 0 && value.getExpiresAt() < currTime;
    }

    /**
     * Converts a cache entry with a payload into a cache entry with stats only,
     * for when no payload is necessary, like for listeners
     *
     * @param key
     * @param value
     * @return
     */
    public static CacheEntry<CacheValueStats> newStatsEntry(String key, CacheValue value) {
        return new CacheEntry<>(key, value.getStats());
    }
}
