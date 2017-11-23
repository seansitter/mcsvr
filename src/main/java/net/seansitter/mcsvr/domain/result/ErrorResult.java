package net.seansitter.mcsvr.domain.result;

import net.seansitter.mcsvr.cache.ResponseStatus;

public class ErrorResult implements StatusCacheResult {
    private final ResponseStatus.ErrorStatus status;
    private final String message;

    public ErrorResult(ResponseStatus.ErrorStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getStatusString() {
        return String.format("%s %s", status.toString(), message);
    }
}
