package net.seansitter.mcsvr.domain;

import net.seansitter.mcsvr.cache.CacheImpl;

public class StoreCacheResult implements CacheResult {
    final CacheImpl.StoreStatus status;

    public StoreCacheResult(CacheImpl.StoreStatus status) {
        this.status = status;
    }
}
