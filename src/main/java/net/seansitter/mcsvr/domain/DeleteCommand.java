package net.seansitter.mcsvr.domain;

public class DeleteCommand implements ApiCommand, WriteCommand {
    public static final String name = "delete";

    public final String key;

    public DeleteCommand(String key) {
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
