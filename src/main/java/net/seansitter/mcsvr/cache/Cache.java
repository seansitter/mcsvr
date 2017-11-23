package net.seansitter.mcsvr.cache;

import java.util.List;
import java.util.Optional;

public interface Cache {
    CacheImpl.DeleteStatus deleteKey(String key);

    // this is not an api call
    List<CacheEntry> destroyKeys(List<String> keys);

    Optional<CacheEntry> get(String key);

    List<CacheEntry> gets(List<String> keys);

    CacheImpl.StoreStatus cas(String key, byte[] value, long ttl, long casUnique, long flag);

    CacheImpl.StoreStatus set(String key, byte[] value, long ttl, long flag);
}
