package net.seansitter.mcsvr.domain.result;

import net.seansitter.mcsvr.cache.ResponseStatus;

public class StoreCacheResult implements StatusCacheResult {
    private final ResponseStatus.StoreStatus status;

    public StoreCacheResult(ResponseStatus.StoreStatus status) {
        this.status = status;
    }

    private final ResponseStatus.StoreStatus getStatus() {
        return status;
    }

    @Override
    public String getStatusString() {
        return status.toString();
    }
}
