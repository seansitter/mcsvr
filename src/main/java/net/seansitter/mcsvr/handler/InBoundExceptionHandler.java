package net.seansitter.mcsvr.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.seansitter.mcsvr.domain.result.ErrorResult;
import net.seansitter.mcsvr.exception.ClientException;
import net.seansitter.mcsvr.exception.InvalidCommandException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import static net.seansitter.mcsvr.cache.ResponseStatus.ErrorStatus;

/**
 * Exceptions in the pipeline will be handler here
 */
public class InBoundExceptionHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (ExceptionUtils.indexOfThrowable(cause, InvalidCommandException.class) >= 0) {
            // ErrorStatus.ERROR - poorly name, per protocol means invalid command
            ctx.write(new ErrorResult(ErrorStatus.ERROR));
        }
        else if(ExceptionUtils.indexOfThrowable(cause, ClientException.class) >= 0) {
            ctx.write(new ErrorResult(ErrorStatus.CLIENT_ERROR, cause.getMessage()));
        }
        else { // all other exceptions are server exceptions
            // not sure what is appropriate reason here
            ctx.write(new ErrorResult(ErrorStatus.SERVER_ERROR, "the server encountered an error"));
        }

        cause.printStackTrace();

        // always close connection on error
        ctx.close();
    }
}
