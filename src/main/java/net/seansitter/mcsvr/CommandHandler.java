package net.seansitter.mcsvr;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.seansitter.mcsvr.domain.command.ApiCommand;
import net.seansitter.mcsvr.domain.result.CacheResult;
import net.seansitter.mcsvr.domain.command.StoreCommand;

import java.util.concurrent.ExecutorService;

public class CommandHandler extends SimpleChannelInboundHandler<ApiCommand> {

    private final ExecutorService executorService;
    private final ApiCacheCommandExecutor commandExecutor;


    public CommandHandler(ExecutorService executorService, ApiCacheCommandExecutor commandExecutor){
        this.executorService = executorService;
        this.commandExecutor = commandExecutor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ApiCommand command) throws Exception {
        // ctx can accept write in different thread
        executorService.execute(() -> {
            CacheResult result = commandExecutor.execute(command);
            if (command instanceof StoreCommand && ((StoreCommand) command).isNoReply()) {
                ctx.flush();
            }
            else {
                ctx.writeAndFlush(result);
            }
        });
    }
}
