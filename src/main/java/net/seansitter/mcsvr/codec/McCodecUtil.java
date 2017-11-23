package net.seansitter.mcsvr.codec;

public class McCodecUtil {
    public boolean hasPayload(String cmd) {
        return !(cmd.equalsIgnoreCase("get") ||
                cmd.equalsIgnoreCase("gets") ||
                cmd.equalsIgnoreCase("delete"));
    }
}
