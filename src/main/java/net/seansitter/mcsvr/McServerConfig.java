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
import net.seansitter.mcsvr.cache.listener.*;
import net.seansitter.mcsvr.codec.*;
import net.seansitter.mcsvr.handler.*;
import net.seansitter.mcsvr.jmx.*;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class configures the dependency injection for the project
 */
public class McServerConfig extends AbstractModule {
    Logger logger = LoggerFactory.getLogger(McServerConfig.class);

    private final String[] args;

    private static final int DEFAULT_REAP_INTERVAL_S = 30;
    private static final int DEFAULT_SERVER_PORT = 11211;
    private static final int DEFAULT_MAX_CACHE_BYTES = Integer.MAX_VALUE;
    private static final int DEFAULT_CLIENT_TO = 0;
    private static final int DEFAULT_SERVER_TO = 0;
    private static final int DEFAULT_LRU_RECOVER_PCT = 20;

    public McServerConfig(String[] args) {
        this.args = args;
    }

    @Override
    protected void configure() {
        bind(McServer.class);

        // cache itself is a singleton
        bind(Cache.class).to(CacheImpl.class).in(Singleton.class);

        // the backing cache
        bind(new TypeLiteral<Map<String, CacheValue>>() {})
                .annotatedWith(Names.named("cache"))
                .to(new TypeLiteral<HashMap<String, CacheValue>>(){});

        // codec util
        bind(McCodecUtil.class).in(Singleton.class);

        // request encoder
        bind(ChannelOutboundHandler.class)
                .annotatedWith(Names.named("encoder"))
                .to(McTextEncoder.class);

        // request decoder
        bind(ChannelInboundHandler.class)
                .annotatedWith(Names.named("decoder"))
                .to(McTextDecoder.class);

        // netty command handler
        bind(ChannelInboundHandler.class)
                .annotatedWith(Names.named("commandHandler"))
                .to(CommandHandler.class);

        // netty error handler
        bind(ChannelInboundHandler.class)
                .annotatedWith(Names.named("errorHandler"))
                .to(InBoundErrorHandler.class);

        // cache (concrete) event listeners
        bind(CacheMetricsListener.class).in(Singleton.class);
        bind(LRUManagerListener.class);

        // command executor
        bind(ApiCacheCommandExecutor.class).to(ApiCacheCommandExecutorImpl.class);

        // jmx management
        bind(CacheMetricsJmxMBean.class).to(CacheMetricsJmx.class);
        bind(MCServerManagement.class);

        bind(new TypeLiteral<BlockingQueue<EventMessage>>(){})
                .annotatedWith(Names.named("lruProducerQueue"))
                .to(new TypeLiteral<LinkedBlockingQueue<EventMessage>>(){});
    }

    @Provides
    @Named("cmdSnglThrdExec")
    ExecutorService provideExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    @Provides
    @Named("cacheLock")
    ReadWriteLock provideCacheLock() {
        return new ReentrantReadWriteLock(false); // unfair lock, see CacheImpl
    }

    @Provides
    @Named("cacheCleanup")
    ScheduledExecutorService provideCacheCleanupExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Provides
    CacheEventListener provideCacheEventListener(CacheMetricsListener metrics, LRUManagerListener lru) {
        BroadcastCacheEventListener l = new BroadcastCacheEventListener();
        l.addListener(lru);
        l.addListener(metrics);
        return l;
    }

    @Provides
    @Named("cacheMetrics")
    CacheMetrics provideCacheMetrics(CacheMetricsListener l) {
        return l;
    }

    @Provides
    @Singleton
    CommandLine provideCmdlnConfig(Options options) throws ParseException {
        return new DefaultParser().parse(options, args);
    }

    @Provides
    Options provideCmdlnOptions() {
        Options opts = new Options();
        opts.addOption("help", "show help message");
        opts.addOption("port", true, "server port");
        opts.addOption("maxCacheBytes", true, "the max cache size in bytes");
        opts.addOption("reapInterval", true, "number of seconds between reaper sweeps");
        opts.addOption("idleTimeout", true, "number of seconds before idle connection is closed");
        opts.addOption("serverTimeout", true, "number of seconds before server response times out");
        opts.addOption("lruRecoverPct", true, "percent of max size to recover on lru sweep");
        return opts;
    }

    @Provides
    @Named("svrPort")
    Integer provideSvrPort(CommandLine cmdLine) throws NumberFormatException {
        return cmdLine.hasOption("port") ?
                Integer.parseInt(cmdLine.getOptionValue("port")) : DEFAULT_SERVER_PORT;
    }

    @Provides
    @Named("reapInterval")
    Integer provideReapInterval(CommandLine cmdLine) {
        return cmdLine.hasOption("reapInterval") ?
                Integer.parseInt(cmdLine.getOptionValue("reapInterval")) : DEFAULT_REAP_INTERVAL_S;
    }

    @Provides
    @Named("maxCacheBytes")
    Integer provideMaxCacheBytes(CommandLine cmdLine) {
        int mb = cmdLine.hasOption("maxCacheBytes") ?
                Integer.parseInt(cmdLine.getOptionValue("maxCacheBytes")) : DEFAULT_MAX_CACHE_BYTES;
        logger.info("cache has a max size of " + mb + " bytes");
        return mb;
    }

    @Provides
    @Named("idleTimeout")
    Integer provideIdleTimeout(CommandLine cmdLine) {
        return cmdLine.hasOption("idleTimeout") ?
                Integer.parseInt(cmdLine.getOptionValue("idleTimeout")) : DEFAULT_CLIENT_TO;
    }

    @Provides
    @Named("serverTimeout")
    Integer provideServerTimeout(CommandLine cmdLine) {
        return cmdLine.hasOption("serverTimeout") ?
                Integer.parseInt(cmdLine.getOptionValue("serverTimeout")) : DEFAULT_SERVER_TO;
    }

    @Provides
    @Named("lruRecoverPct")
    Integer provideLruRecoverPct(CommandLine cmdLine, @Named("maxCacheBytes") int maxBytes) {
        int rp = cmdLine.hasOption("lruRecoverPct") ?
                Integer.parseInt(cmdLine.getOptionValue("lruRecoverPct")) : DEFAULT_LRU_RECOVER_PCT;
        logger.info("lru will attempt to recover " + rp + "% when the cache exceeds " + maxBytes + " bytes");
        return rp;
    }
}
