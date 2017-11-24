package net.seansitter.mcsvr;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.InetSocketAddress;

public class MCServer {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(MCServer.class);

        Injector injector = Guice.createInjector(new McServerConfig());
        try {
            logger.info("starting memcache server");
            MCServer mcServer = injector.getInstance(MCServer.class);
            mcServer.start();
        }
        catch (Exception e) {
            logger.error("failed to start server: ", e);
        }
    }

    private final Provider<ChannelOutboundHandler> encoder;
    private final Provider<ChannelInboundHandler> decoder;
    private final Provider<ChannelInboundHandler> commandHandler;
    private final Provider<ChannelInboundHandler> errorHandler;

    @Inject
    public MCServer(@Named("encoder") Provider<ChannelOutboundHandler> encoder,
                    @Named("decoder") Provider<ChannelInboundHandler> decoder,
                    @Named("commandHandler") Provider<ChannelInboundHandler> commandHandler,
                    @Named("errorHandler") Provider<ChannelInboundHandler> errorHandler) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.commandHandler = commandHandler;
        this.errorHandler = errorHandler;
    }

    public void start() throws Exception {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(11211))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(decoder.get());
                            ch.pipeline().addLast(encoder.get());
                            ch.pipeline().addLast(commandHandler.get());
                            ch.pipeline().addLast(errorHandler.get());
                        }
                    });
            ChannelFuture bindFuture = bootstrap.bind().sync();
            bindFuture.channel().closeFuture().sync();
        }
        finally {
            eventLoopGroup.shutdownGracefully().sync();
        }
    }
}
