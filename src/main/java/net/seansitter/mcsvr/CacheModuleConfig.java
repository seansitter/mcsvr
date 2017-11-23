package net.seansitter.mcsvr;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import net.seansitter.mcsvr.cache.Cache;
import net.seansitter.mcsvr.cache.CacheImpl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class configures the dependency injection for the project
 */
public class CacheModuleConfig extends AbstractModule {
    @Override
    protected void configure() {
        bind(Cache.class).to(CacheImpl.class).in(Singleton.class);
        bind(ApiCacheCommandExecutor.class).to(ApiCacheCommandExecutorImpl.class).in(Singleton.class);
    }

    @Provides
    ExecutorService provideExecutorService() {
        return Executors.newSingleThreadExecutor();
    }
}
