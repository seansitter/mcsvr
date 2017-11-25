package net.seansitter.mcsvr.cache;

public class CacheValue {
    private final byte[] payload;
    private final long flag;
    private final long casUnique;
    private final CacheValueStats stats;

    public CacheValue(byte[] payload, long flag, long createdAt, long expiresAt, long casUnique) {
        this.payload = payload;
        this.flag = flag;
        this.casUnique = casUnique;
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
}
