package net.seansitter.mcsvr.codec;

/**
 * Utility class for codecs
 */
public class McCodecUtil {
    public boolean hasPayload(String cmd) {
        return !(cmd.equalsIgnoreCase("get") ||
                cmd.equalsIgnoreCase("gets") ||
                cmd.equalsIgnoreCase("delete"));
    }
}
