package net.seansitter.mcsvr.cache;

import java.util.Arrays;

/**
 * Represents a value in the cache
 */
public class CacheValue {
    private final byte[] payload;
    private final long flag;
    private final long casUnique;
    private final CacheValueStats stats;

    public CacheValue(byte[] payload, long flag, long createdAt, long expiresAt, long casUnique) {
        this.payload = payload;
        this.flag = flag;
        this.casUnique = casUnique; // unique value assigned by the cache for cas operations
        this.stats = new CacheValueStats(createdAt, expiresAt, payload.length);
    }

    public byte[] getPayload() {
        return payload;
    }

    public long getFlag() {
        return flag;
    }

    public long getCasUnique() {
        return casUnique;
    }

    public long getCreatedAt() {
        return stats.createdAt;
    }

    public long getExpiresAt() {
        return stats.expiresAt;
    }

    public int getSize() {
        return stats.size;
    }

    public CacheValueStats getStats() {
        return stats;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CacheValue)) {
            return false;
        }

        CacheValue v = (CacheValue)o;
        if (v.flag != flag) {
            return false;
        }
        if (payload != null && v.payload == null) {
            return false;
        }
        if (payload != null && !Arrays.equals(v.payload, payload)) {
            return false;
        }
        if (stats != null && v.stats == null) {
            return false;
        }
        if (stats != null && !stats.equals(v.stats)) {
            return false;
        }

        return true;
    }
}
