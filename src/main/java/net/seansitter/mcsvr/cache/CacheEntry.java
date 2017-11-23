package net.seansitter.mcsvr.cache;

public class CacheEntry {
    private final String key;
    private final CacheValue value;

    public CacheEntry(String key, CacheValue value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public CacheValue getValue() {
        return value;
    }
}
