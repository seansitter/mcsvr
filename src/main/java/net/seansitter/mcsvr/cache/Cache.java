package net.seansitter.mcsvr.cache;

import java.util.List;
import java.util.Optional;

public interface Cache {
    // starts the cache (thread reaper, etc)
    void start();

    ResponseStatus.DeleteStatus deleteKey(String key);

    // this is not an api call
    List<CacheEntry<CacheValueStats>> destroyKeys(List<String> keys);

    List<CacheEntry<CacheValue>> get(List<String> keys);

    Optional<CacheEntry<CacheValue>> get(String keys);

    ResponseStatus.StoreStatus cas(String key, byte[] value, long ttl, long casUnique, long flag);

    ResponseStatus.StoreStatus set(String key, byte[] value, long ttl, long flag);
}
