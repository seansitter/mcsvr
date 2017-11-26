package net.seansitter.mcsvr.handler;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import net.seansitter.mcsvr.cache.ResponseStatus;
import net.seansitter.mcsvr.domain.result.ErrorResult;
import net.seansitter.mcsvr.exception.ClientException;
import net.seansitter.mcsvr.exception.InvalidCommandException;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InBoundErrorHandlerTest {
    ChannelHandlerContext ctx;
    InBoundErrorHandler errorHandler;

    @Before
    public void setup() {
        ctx = mock(ChannelHandlerContext.class);
        errorHandler = new InBoundErrorHandler();
    }

    @Test
    public void testInvalidCommand() {
        errorHandler.exceptionCaught(ctx, new InvalidCommandException("foo"));
        verify(ctx).writeAndFlush(new ErrorResult(ResponseStatus.ErrorStatus.ERROR));
    }

    @Test
    public void testClientError() {
        String msg = "client messed up";
        errorHandler.exceptionCaught(ctx, new ClientException(msg));
        verify(ctx).writeAndFlush(new ErrorResult(ResponseStatus.ErrorStatus.CLIENT_ERROR, msg));
    }

    @Test
    public void testServerError() {
        String msg = "the server encountered an error";
        errorHandler.exceptionCaught(ctx, new Exception(msg));
        verify(ctx).writeAndFlush(new ErrorResult(ResponseStatus.ErrorStatus.SERVER_ERROR, msg));
    }

    @Test
    public void testWriterIdle() throws Exception {
        IdleStateEvent s = mock(IdleStateEvent.class);
        when(s.state()).thenReturn(IdleState.WRITER_IDLE);
        ChannelFuture f = mock(ChannelFuture.class);
        when(ctx.writeAndFlush(any())).thenReturn(f);
        errorHandler.userEventTriggered(ctx, s);
        verify(ctx).writeAndFlush(new ErrorResult(ResponseStatus.ErrorStatus.SERVER_ERROR, "server took too long"));
        verify(f).addListener(ChannelFutureListener.CLOSE);
    }

    @Test
    public void testReaderIdle() throws Exception {
        IdleStateEvent s = mock(IdleStateEvent.class);
        when(s.state()).thenReturn(IdleState.READER_IDLE);
        errorHandler.userEventTriggered(ctx, s);
        verify(ctx).close();
    }
}
