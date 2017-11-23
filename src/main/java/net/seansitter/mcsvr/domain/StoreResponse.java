package net.seansitter.mcsvr.domain;

public class StoreResponse implements Response {
    public static final StoreResponse STORED = new StoreResponse("STORED");
    public static final StoreResponse NOT_STORED = new StoreResponse("NOT_STORED");
    public static final StoreResponse EXISTS = new StoreResponse("EXISTS");
    public static final StoreResponse NOT_FOUND = new StoreResponse("NOT_FOUND");

    private final String response;

    private StoreResponse(String response) {
        this.response = response;
    }
}
