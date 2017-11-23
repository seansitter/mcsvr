package net.seansitter.mcsvr.cache;

public class ResponseStatus {
    public enum StoreStatus {
        STORED("STORED"),
        NOT_STORED("NOT_STORED"),
        EXISTS("EXISTS"),
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
}
