package net.seansitter.mcsvr.domain.command;

public interface ApiCommand {
    String getName();
    boolean isNoReply();
}
