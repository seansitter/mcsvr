package net.seansitter.mcsvr.domain.command;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a get/gets text command
 */
public class GetCommand implements ApiCommand {
    private final String name;
    private final List<String> keys;

    private GetCommand(String name, List<String> keys) {
        this.name = name;
        this.keys = keys;
    }

    public List<String> getKeys() {
        return keys;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isNoReply() {
        return false;
    }

    @Override
    public String toString() {
        return name+" "+String.join(" ", keys);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private LinkedList<String> keys = new LinkedList<>();

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withKey(String key) {
            keys.add(key);
            return this;
        }

        public Builder withKeys(List<String> keys) {
            Collections.copy(this.keys, keys);
            return this;
        }

        public GetCommand build() {
            return new GetCommand(name, keys);
        }
    }
}
