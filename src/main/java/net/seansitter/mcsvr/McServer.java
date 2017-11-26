package net.seansitter.mcsvr;

import com.google.inject.Provider;
import com.google.inject.name.Named;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import net.seansitter.mcsvr.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.InetSocketAddress;

public class McServer {
    private final Logger logger = LoggerFactory.getLogger(McServer.class);

    private final Cache cache;
    private final int port;
    private final Provider<ChannelOutboundHandler> encoder;
    private final Provider<ChannelInboundHandler> decoder;
    private final Provider<ChannelInboundHandler> commandHandler;
    private final Provider<ChannelInboundHandler> errorHandler;
    private final int idleTimeout;
    private final int serverTimeout;

    @Inject
    public McServer(@Named("svrPort") int port,
                    Cache cache,
                    @Named("encoder") Provider<ChannelOutboundHandler> encoder,
                    @Named("decoder") Provider<ChannelInboundHandler> decoder,
                    @Named("commandHandler") Provider<ChannelInboundHandler> commandHandler,
                    @Named("errorHandler") Provider<ChannelInboundHandler> errorHandler,
                    @Named("idleTimeout") int idleTimeout,
                    @Named("serverTimeout") int serverTimeout) {
        this.cache = cache;
        this.port = port;
        this.encoder = encoder;
        this.decoder = decoder;
        this.commandHandler = commandHandler;
        this.errorHandler = errorHandler;
        this.idleTimeout = idleTimeout;
        this.serverTimeout = serverTimeout;
    }

    public void start() throws Exception {

        // starting cache will schedule expired item cleanup thread
        cache.start();
        logger.info("idle timeout is "+idleTimeout+" seconds");
        logger.info("server timeout is "+serverTimeout+" seconds");

        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // if the connection is idle for 10s on either read or write, disconnect
                            ch.pipeline().addLast("idleStateHandler",
                                    new IdleStateHandler(idleTimeout, serverTimeout, 0));

                            // these all need to be providers because we need a new instance on each invocation/**/
                            ch.pipeline().addLast("decoderHandler", decoder.get());
                            ch.pipeline().addLast("encoderHandler", encoder.get());
                            ch.pipeline().addLast("commandHandler", commandHandler.get());
                            ch.pipeline().addLast("errorHandler", errorHandler.get());
                        }
                    });
            logger.info("started memcache server on port: "+port);
            ChannelFuture bindFuture = bootstrap.bind().sync();
            bindFuture.channel().closeFuture().sync();
        }
        finally {
            eventLoopGroup.shutdownGracefully().sync();
        }
    }
}
