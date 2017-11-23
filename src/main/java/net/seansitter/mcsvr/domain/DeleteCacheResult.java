package net.seansitter.mcsvr.domain;

import net.seansitter.mcsvr.cache.CacheImpl;

public class DeleteCacheResult implements StatusCacheResult {
    private final CacheImpl.DeleteStatus status;

    public DeleteCacheResult(CacheImpl.DeleteStatus status) {
        this.status = status;
    }

    public CacheImpl.DeleteStatus getStatus() {
        return status;
    }

    @Override
    public String getStatusString() {
        return status.toString();
    }
}
