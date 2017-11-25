package net.seansitter.mcsvr.domain.result;

import net.seansitter.mcsvr.cache.ResponseStatus;

public class DeleteCacheResult implements StatusCacheResult {
    private final ResponseStatus.DeleteStatus status;

    public DeleteCacheResult(ResponseStatus.DeleteStatus status) {
        this.status = status;
    }

    public ResponseStatus.DeleteStatus getStatus() {
        return status;
    }

    @Override
    public String getStatusString() {
        return status.toString();
    }
}
