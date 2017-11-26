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

    /**
     * Convenience for testing, yes should probably override hashcode
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ErrorResult)) {
            return false;
        }
        ErrorResult oe = (ErrorResult)o;

        if(null == oe.status && null != status) {
            return false;
        }
        if(null == oe.message && null != message) {
            return false;
        }

        return (null == oe.message || oe.message.equals(message)) && (null == oe.status || oe.status.equals(status));
    }
}
