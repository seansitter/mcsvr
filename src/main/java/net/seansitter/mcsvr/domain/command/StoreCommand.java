package net.seansitter.mcsvr.domain.command;

/**
 * Represents a set/cas text command
 */
public class StoreCommand implements ApiCommand {
    private final String name;
    private final String key;
    private final byte[] payload;
    private final int payloadLen;
    private final int flags; // protocol requires 16 bit unsigned, unsigned not available in java < 8
    private final long expTime;
    private final long casUnique;
    private final boolean isNoReply;

    private StoreCommand(String name, String key, int flags, long expTime, long casUnique, boolean isNoReply, byte[] payload) {
        this.name = name;
        this.key = key;
        this.payloadLen = payload.length;
        this.flags = flags;
        this.expTime = expTime;
        this.casUnique = casUnique;
        this.isNoReply = isNoReply;
        this.payload = payload;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public byte[] getPayload() {
        return payload;
    }

    public int getPayloadLen() {
        return payloadLen;
    }

    public int getFlags() {
        return flags;
    }

    public long getExpTime() {
        return expTime;
    }

    public long getCasUnique() {
        return casUnique;
    }

    @Override
    public boolean isNoReply() {
        return isNoReply;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(" "+flags);
        sb.append(" "+expTime);
        sb.append(" "+payloadLen);
        if (name.equals("cas")) {
            sb.append(" "+casUnique);
        }
        if (isNoReply) {
            sb.append(" noreply");
        }
        return sb.toString();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String key;
        private byte[] payload; // treat this as effectively immutable, avoid copy
        private int flags = 0; // protocol requires 16 bit unsigned, unsigned not available in java < 8
        private long expTime = 0;
        private long casUnique = 0;
        private boolean isNoReply = false;

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withKey(String key) {
            this.key = key;
            return this;
        }

        public Builder withPayload(byte[] payload) {
            this.payload = payload;
            return this;
        }

        public Builder withFlags(int flags) {
            this.flags = flags;
            return this;
        }

        public Builder withExpTime(long expTime) {
            this.expTime = expTime;
            return this;
        }

        public Builder withCasUnique(long casUnique) {
            this.casUnique = casUnique;
            return this;
        }

        public Builder withIsNoReploy(boolean isNoReply) {
            this.isNoReply = isNoReply;
            return this;
        }

        public StoreCommand build() {
            return new StoreCommand(name, key, flags, expTime, casUnique, isNoReply, payload);
        }
    }
}
