package net.seansitter.mcsvr.domain;

import net.seansitter.mcsvr.cache.CacheImpl;

public class DeleteCacheResult {
    private final CacheImpl.DeleteStatus deleteStatus;

    public DeleteCacheResult(CacheImpl.DeleteStatus deleteStatus) {
        this.deleteStatus = deleteStatus;
    }
}
