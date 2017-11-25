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
import net.seansitter.mcsvr.cache.Cache;
import net.seansitter.mcsvr.jmx.MCServerManagement;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import java.net.InetSocketAddress;

public class MCServer {
    private final Logger logger = LoggerFactory.getLogger(MCServer.class);

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(MCServer.class);

        Injector injector = Guice.createInjector(new McServerConfig(args));
        CommandLine cmdLn = injector.getInstance(CommandLine.class);

        if (cmdLn.hasOption("help")) {
            new HelpFormatter().printHelp("mcsvr", injector.getInstance(Options.class));
            System.exit(0);
        }

        try {
            injector.getInstance(MCServerManagement.class).start();
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        } catch (NotCompliantMBeanException e) {
            e.printStackTrace();
        } catch (InstanceAlreadyExistsException e) {
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            e.printStackTrace();
        }

        try {
            logger.info("starting memcache server");
            MCServer mcServer = injector.getInstance(MCServer.class);
            mcServer.start();
        }
        catch (Exception e) {
            logger.error("failed to start server: ", e);
        }
    }

    private final Cache cache;
    private final int port;
    private final Provider<ChannelOutboundHandler> encoder;
    private final Provider<ChannelInboundHandler> decoder;
    private final Provider<ChannelInboundHandler> commandHandler;
    private final Provider<ChannelInboundHandler> errorHandler;

    @Inject
    public MCServer(@Named("svrPort") int port,
                    Cache cache,
                    @Named("encoder") Provider<ChannelOutboundHandler> encoder,
                    @Named("decoder") Provider<ChannelInboundHandler> decoder,
                    @Named("commandHandler") Provider<ChannelInboundHandler> commandHandler,
                    @Named("errorHandler") Provider<ChannelInboundHandler> errorHandler) {
        this.cache = cache;
        this.port = port;
        this.encoder = encoder;
        this.decoder = decoder;
        this.commandHandler = commandHandler;
        this.errorHandler = errorHandler;
    }

    public void start() throws Exception {

        // starting cache will schedule expired item cleanup thread
        cache.start();

        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // these all need to be providers because we need a new instance on each invocation
                            ch.pipeline().addLast(decoder.get());
                            ch.pipeline().addLast(encoder.get());
                            ch.pipeline().addLast(commandHandler.get());
                            ch.pipeline().addLast(errorHandler.get());
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
