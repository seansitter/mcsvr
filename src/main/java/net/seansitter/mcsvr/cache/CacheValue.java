package net.seansitter.mcsvr.cache;

public class CacheValue {
    private final byte[] payload;
    private final long flag;
    private final long createdAt;
    private final long expiresAt;
    private final long casUnique;

    public CacheValue(byte[] payload, long flag, long createdAt, long expiresAt, long casUnique) {
        this.payload = payload;
        this.flag = flag;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.casUnique = casUnique;
    }

    public byte[] getPayload() {
        return payload;
    }

    public long getFlag() {
        return flag;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public long getCasUnique() {
        return casUnique;
    }

    public long getSize() {
        return payload.length;
    }
}
