package net.seansitter.mcsvr;

import net.seansitter.mcsvr.domain.command.ApiCommand;
import net.seansitter.mcsvr.domain.result.CacheResult;

public interface ApiCacheCommandExecutor {
    CacheResult execute(ApiCommand command);
}
