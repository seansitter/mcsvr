package net.seansitter.mcsvr;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.seansitter.mcsvr.cache.Cache;
import net.seansitter.mcsvr.cache.CacheImpl;
import net.seansitter.mcsvr.codec.McTextDecoder;
import net.seansitter.mcsvr.codec.McTextEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MCServer {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(MCServer.class);

        try {
            new MCServer().start();
        }
        catch (Exception e) {
            logger.error("failed to stat server: ", e);
        }
    }

    public void start() throws Exception {
        Cache cache = new CacheImpl();

        ExecutorService execSvc = Executors.newCachedThreadPool();
        ApiCacheCommandExecutor cmdExec = new ApiCacheCommandExecutorImpl(cache);

        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(11211))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new McTextDecoder());
                            ch.pipeline().addLast(new McTextEncoder());
                            ch.pipeline().addLast(new CommandHandler(execSvc, cmdExec));
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
