package net.seansitter.mcsvr.domain.result;

import net.seansitter.mcsvr.cache.CacheImpl;

public class StoreCacheResult implements StatusCacheResult {
    private final CacheImpl.StoreStatus status;

    public StoreCacheResult(CacheImpl.StoreStatus status) {
        this.status = status;
    }

    private final CacheImpl.StoreStatus getStatus() {
        return status;
    }

    @Override
    public String getStatusString() {
        return status.toString();
    }
}
