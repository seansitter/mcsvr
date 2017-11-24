package net.seansitter.mcsvr.cache;

import java.util.List;
import java.util.Optional;

public interface Cache {
    ResponseStatus.DeleteStatus deleteKey(String key);

    // this is not an api call
    List<CacheEntry> destroyKeys(List<String> keys);

    List<CacheEntry> get(List<String> keys);

    Optional<CacheEntry> get(String keys);

    ResponseStatus.StoreStatus cas(String key, byte[] value, long ttl, long casUnique, long flag);

    ResponseStatus.StoreStatus set(String key, byte[] value, long ttl, long flag);
}
