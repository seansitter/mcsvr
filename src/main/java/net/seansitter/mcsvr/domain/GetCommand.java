package net.seansitter.mcsvr.domain;

public class GetCommand implements ApiCommand, RetrieveCommand {
    private static final String name = "get";
    private final String key;

    public GetCommand(String key) {
        this.key = key;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }
}
