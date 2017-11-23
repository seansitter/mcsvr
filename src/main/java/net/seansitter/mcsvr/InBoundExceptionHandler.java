package net.seansitter.mcsvr;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.seansitter.mcsvr.domain.result.ErrorResult;
import net.seansitter.mcsvr.exception.ClientException;
import net.seansitter.mcsvr.exception.InvalidCommandException;

import static net.seansitter.mcsvr.cache.ResponseStatus.ErrorStatus;

public class InBoundExceptionHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof InvalidCommandException) {
            // ErrorStatus.ERROR - poorly name, per protocol means invalid command
            ctx.write(new ErrorResult(ErrorStatus.ERROR));
        }
        else if(cause instanceof ClientException) {
            ctx.write(new ErrorResult(ErrorStatus.CLIENT_ERROR, cause.getMessage()));
        }
        else {
            ctx.write(new ErrorResult(ErrorStatus.SERVER_ERROR, "server error"));
        }

        cause.printStackTrace();

        // always close connection on error
        ctx.close();
    }
}
