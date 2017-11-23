package net.seansitter.mcsvr;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.seansitter.mcsvr.domain.command.ApiCommand;
import net.seansitter.mcsvr.domain.command.NoReplyCommand;
import net.seansitter.mcsvr.domain.result.CacheResult;
import net.seansitter.mcsvr.domain.command.StoreCommand;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class CommandHandler extends SimpleChannelInboundHandler<ApiCommand> {

    private final ExecutorService executorService;
    private final ApiCacheCommandExecutor commandExecutor;


    public CommandHandler(ExecutorService executorService, ApiCacheCommandExecutor commandExecutor){
        this.executorService = executorService;
        this.commandExecutor = commandExecutor;
    }

    public CommandHandler(ApiCacheCommandExecutor commandExecutor){
        // This is cheating a bit, as its probably not the most efficient to create a new executor for every new instance
        // of this class, but THE MOST IMPORTANT THING is that for a given client, all operations are ordered for the
        // client connection. It would certainly be more efficient to have a thread pool which would cache threads
        // and where we could obtain a lease on a thread for the duration of a request. That way we would not be spinning
        // up and tearing down the machinery of threadpool / thread creation on every request. If I had all the time...
        //
        // For the purposes of this exercise, the executor provides a nice interface + built in queueing.
        this.executorService = Executors.newSingleThreadExecutor();
        this.commandExecutor = commandExecutor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ApiCommand command) throws Exception {
        // ctx can accept write in different thread
        executorService.execute(() -> {
            CacheResult result = commandExecutor.execute(command);
            if (command instanceof NoReplyCommand && ((NoReplyCommand)command).isNoReply()) {
                ctx.flush();
            }
            else {
                ctx.writeAndFlush(result);
            }
        });
    }
}
