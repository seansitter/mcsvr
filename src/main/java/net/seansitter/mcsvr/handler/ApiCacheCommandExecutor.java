package net.seansitter.mcsvr.handler;

import net.seansitter.mcsvr.domain.command.ApiCommand;
import net.seansitter.mcsvr.domain.result.CacheResult;

public interface ApiCacheCommandExecutor {
    CacheResult execute(ApiCommand command);
}
