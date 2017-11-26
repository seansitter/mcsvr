package net.seansitter.mcsvr.domain.command;

/**
 * Interface for a text protocol command
 */
public interface ApiCommand {
    String getName();
    boolean isNoReply();
}
