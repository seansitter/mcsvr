package net.seansitter.mcsvr;

import net.seansitter.mcsvr.cache.Cache;
import net.seansitter.mcsvr.domain.*;

public class ApiCacheCommandExecutorImpl implements ApiCacheCommandExecutor {
    private final Cache cache;

    public ApiCacheCommandExecutorImpl(Cache cache) {
        this.cache = cache;
    }

    @Override
    public CacheResult execute(ApiCommand command) {
        if (command.getName().equals("gets")) {
            return executeGetsCommand((GetsCommand)command);
        }
        else if (command.getName().equals("get")) {
            return executeGetCommand((GetCommand)command);
        }
        else if (command.getName().equals("set")) {
            return executeSetCommand((StoreCommand)command);
        }
        return null;
    }

    private CacheResult executeGetsCommand(GetsCommand c) {
        return new GetsCacheResult(cache.gets(c.getKeys()));
    }

    private CacheResult executeGetCommand(GetCommand c) {
        return new GetsCacheResult(cache.get(c.getKey()));
    }

    private CacheResult executeSetCommand(StoreCommand c) {
        return new StoreCacheResult(cache.set(c.getKey(), c.getPayload(), c.getExpTime(), c.getFlags()));
    }
}
