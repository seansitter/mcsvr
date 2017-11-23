package net.seansitter.mcsvr.domain;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class GetCommand implements ApiCommand, RetrieveCommand {
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
