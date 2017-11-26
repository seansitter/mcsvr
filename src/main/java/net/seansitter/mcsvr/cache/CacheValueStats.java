package net.seansitter.mcsvr.cache;

/**
 * Stats for a single cache value
 */
public class CacheValueStats {
    public final long createdAt;
    public final long expiresAt;
    public final int size;

    public CacheValueStats(long createdAt, long expiresAt, int size) {
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.size = size;
    }
}
