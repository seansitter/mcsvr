package net.seansitter.mcsvr.domain;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class GetsCommand implements ApiCommand, RetrieveCommand {
    public static final String name = "gets";

    private final List<String> keys;

    public GetsCommand(List<String> keys) {
        this.keys = Collections.unmodifiableList(keys);
    }

    public List<String> getKeys() {
        return keys;
    }

    public static GetsCommandBuilder newBuilder() {
        return new GetsCommandBuilder();
    }

    @Override
    public String getName() {
        return name;
    }

    public static class GetsCommandBuilder {
        private final LinkedList<String> keys;

        private GetsCommandBuilder() {
            keys = new LinkedList<>();
        }

        public void withKey(String key) {
            keys.add(key);
        }

        public GetsCommand build() {
            return new GetsCommand(keys);
        }
    }
}
