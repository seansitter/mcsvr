package net.seansitter.mcsvr;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import net.seansitter.mcsvr.cache.*;
import net.seansitter.mcsvr.codec.McCodecUtil;
import net.seansitter.mcsvr.codec.McTextDecoder;
import net.seansitter.mcsvr.codec.McTextEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class configures the dependency injection for the project
 */
public class McServerConfig extends AbstractModule {
    @Override
    protected void configure() {
        bind(MCServer.class);

        // cache itself is a singleton
        bind(Cache.class).to(CacheImpl.class).in(Singleton.class);

        bind(ChannelOutboundHandler.class)
                .annotatedWith(Names.named("encoder"))
                .to(McTextEncoder.class);

        bind(ChannelInboundHandler.class)
                .annotatedWith(Names.named("decoder"))
                .to(McTextDecoder.class);

        bind(ChannelInboundHandler.class)
                .annotatedWith(Names.named("commandHandler"))
                .to(CommandHandler.class);

        bind(ChannelInboundHandler.class)
                .annotatedWith(Names.named("errorHandler"))
                .to(InBoundExceptionHandler.class);

        bind(ApiCacheCommandExecutor.class).to(ApiCacheCommandExecutorImpl.class);

        bind(CacheEventListener.class).to(DummyCacheEventListener.class);

        bind(McCodecUtil.class).in(Singleton.class);

        // the backing cache
        bind(new TypeLiteral<Map<String, CacheValue>>() {})
                .annotatedWith(Names.named("cache"))
                .to(new TypeLiteral<HashMap<String, CacheValue>>(){});
    }

    @Provides
    ExecutorService provideExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    @Provides
    @Named("cacheLock")
    ReentrantReadWriteLock provideCacheLock() {
        return new ReentrantReadWriteLock(false);
    }
}
