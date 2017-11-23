package net.seansitter.mcsvr;

import net.seansitter.mcsvr.domain.ApiCommand;
import net.seansitter.mcsvr.domain.CacheResult;

public interface ApiCacheCommandExecutor {
    CacheResult execute(ApiCommand command);
}
