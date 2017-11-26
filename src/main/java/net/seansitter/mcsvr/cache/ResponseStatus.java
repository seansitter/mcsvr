package net.seansitter.mcsvr.cache;

/**
 * The cache response status for non-retrieval operations
 */
public class ResponseStatus {
    /**
     * Status for store operations
     */
    public enum StoreStatus {
        // to indicate success
        STORED("STORED"),
        // to indicate the data was not stored, but not
        // because of an error. This normally means that the
        // condition for an "add" or a "replace" command wasn't met
        NOT_STORED("NOT_STORED"),
        // to indicate that the item you are trying to store with
        // a "cas" command has been modified since you last fetched it
        EXISTS("EXISTS"),
        // to indicate that the item you are trying to store
        // with a "cas" command did not exist
        NOT_FOUND("NOT_FOUND");

        private String status;
        StoreStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }
    }

    /**
     * Status for delete operations
     */
    public enum DeleteStatus {
        DELETED("DELETED"),
        NOT_FOUND("NOT_FOUND");

        private String status;
        DeleteStatus(String status) {
            this.status = status;
        }

        public String toString() {
            return status;
        }
    }

    /**
     * Error status
     */
    public enum ErrorStatus {
        ERROR("ERROR"),
        CLIENT_ERROR("CLIENT_ERROR"),
        SERVER_ERROR("SERVER_ERROR");

        private String status;
        ErrorStatus(String status) {
            this.status = status;
        }

        public String toString() {
            return status;
        }
    }
}
