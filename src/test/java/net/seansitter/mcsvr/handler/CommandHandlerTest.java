package net.seansitter.mcsvr.handler;

import io.netty.channel.ChannelHandlerContext;
import net.seansitter.mcsvr.domain.command.ApiCommand;
import net.seansitter.mcsvr.domain.result.CacheResult;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.*;

public class CommandHandlerTest {
    ExecutorService executorService;
    ApiCacheCommandExecutor commandExecutor;
    CommandHandler cmdHandler;
    ChannelHandlerContext ctx;

    @Before
    public void setup() {
        executorService = mock(ExecutorService.class);
        commandExecutor = mock(ApiCacheCommandExecutor.class);
        cmdHandler = new CommandHandler(executorService, commandExecutor);
        ctx = mock(ChannelHandlerContext.class);
    }

    @Test
    public void testCommandWithReply() throws Exception {
        ApiCommand cmd = mock(ApiCommand.class);
        when(cmd.isNoReply()).thenReturn(false);

        CacheResult cacheResult = mock(CacheResult.class);
        when(commandExecutor.execute(cmd)).thenReturn(cacheResult);

        doAnswer(i -> {
            ((Runnable)i.getArgument(0)).run();
            return null;
        }).when(executorService).execute(any());

        cmdHandler.channelRead0(ctx, cmd);
        verify(ctx).writeAndFlush(cacheResult);
    }

    @Test
    public void testCommandNoReply() throws Exception {
        ApiCommand cmd = mock(ApiCommand.class);
        when(cmd.isNoReply()).thenReturn(true);

        CacheResult cacheResult = mock(CacheResult.class);
        when(commandExecutor.execute(cmd)).thenReturn(cacheResult);

        doAnswer(i -> {
            ((Runnable)i.getArgument(0)).run();
            return null;
        }).when(executorService).execute(any());

        cmdHandler.channelRead0(ctx, cmd);
        verify(ctx).flush();
    }
}
