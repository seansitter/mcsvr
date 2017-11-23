package net.seansitter.mcsvr.domain.command;

public class DeleteCommand implements ApiCommand, NoReplyCommand {
    public static final String name = "delete";

    private final String key;
    private final boolean isNoReply;

    private DeleteCommand(String key, boolean isNoReply) {
        this.key = key;
        this.isNoReply = isNoReply;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean isNoReply() {
        return isNoReply;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private boolean isNoReply = true;
        private String key;

        public Builder withKey(String key) {
            this.key = key;
            return this;
        }

        public Builder withIsNoReply(boolean isNoReply) {
            this.isNoReply = isNoReply;
            return this;
        }

        public DeleteCommand build() {
            return new DeleteCommand(key, isNoReply);
        }
    }
}
