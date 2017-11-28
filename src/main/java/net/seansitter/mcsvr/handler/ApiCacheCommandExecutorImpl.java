package net.seansitter.mcsvr.handler;

import net.seansitter.mcsvr.cache.Cache;
import net.seansitter.mcsvr.domain.command.ApiCommand;
import net.seansitter.mcsvr.domain.command.DeleteCommand;
import net.seansitter.mcsvr.domain.command.GetCommand;
import net.seansitter.mcsvr.domain.command.StoreCommand;
import net.seansitter.mcsvr.domain.result.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * This class dispatches a command to a method call in the cache. It is used
 * in a handler at the end of the pipeline
 */
public class ApiCacheCommandExecutorImpl implements ApiCacheCommandExecutor {
    Logger logger = LoggerFactory.getLogger(ApiCacheCommandExecutorImpl.class);

    private final Cache cache;

    @Inject
    public ApiCacheCommandExecutorImpl(Cache cache) {
        this.cache = cache;
    }

    @Override
    public CacheResult execute(ApiCommand command) {
        logger.debug("executing command: "+command);

        CacheResult result = null;
        if (command.getName().equals("gets")) {
            result = executeGetsCommand((GetCommand)command);
        }
        else if (command.getName().equals("get")) {
            result = executeGetCommand((GetCommand)command);
        }
        else if (command.getName().equals("set")) {
            result = executeSetCommand((StoreCommand)command);
        }
        else if (command.getName().equals("delete")) {
            result = executeDeleteCommand((DeleteCommand)command);
        }
        else if (command.getName().equals("cas")) {
            result = executeCasCommand((StoreCommand)command);
        }

        if (null != result) {
            logger.debug("got result: "+result);
        }

        return result;
    }

    private CacheResult executeGetsCommand(GetCommand c) {
        return new GetsCacheResult(cache.get(c.getKeys()));
    }

    private CacheResult executeGetCommand(GetCommand c) {
        return new GetCacheResult(cache.get(c.getKeys()));
    }

    private CacheResult executeSetCommand(StoreCommand c) {
        return new StoreCacheResult(cache.set(c.getKey(), c.getPayload(), c.getExpTime(), c.getFlags()));
    }

    private CacheResult executeCasCommand(StoreCommand c) {
        return new StoreCacheResult(cache.cas(c.getKey(), c.getPayload(), c.getExpTime(), c.getCasUnique(), c.getFlags()));
    }

    private CacheResult executeDeleteCommand(DeleteCommand c) {
        return new DeleteCacheResult(cache.deleteKey(c.getKey()));
    }
}
