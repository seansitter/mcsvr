package net.seansitter.mcsvr.domain.command;

public interface ApiCommand extends Command {
    String getName();
    boolean isNoReply();
}
