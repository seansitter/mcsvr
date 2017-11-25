package net.seansitter.mcsvr.cache;

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
