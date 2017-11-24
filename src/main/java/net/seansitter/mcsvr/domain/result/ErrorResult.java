package net.seansitter.mcsvr.domain.result;

import static net.seansitter.mcsvr.cache.ResponseStatus.ErrorStatus;

public class ErrorResult implements StatusCacheResult {
    private final ErrorStatus status;
    private final String message;

    public ErrorResult(ErrorStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public ErrorResult(ErrorStatus status) {
        this(status, null);
    }

    @Override
    public String getStatusString() {
        // ERROR type never sends a reason per spec
        if (status.equals(ErrorStatus.ERROR) || null == message) {
            return this.status.toString();
        }

        return String.format("%s %s", status.toString(), message);
    }
}
