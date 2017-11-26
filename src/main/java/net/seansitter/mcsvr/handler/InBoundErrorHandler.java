package net.seansitter.mcsvr.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import net.seansitter.mcsvr.domain.result.ErrorResult;
import net.seansitter.mcsvr.exception.ClientException;
import net.seansitter.mcsvr.exception.InvalidCommandException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.seansitter.mcsvr.cache.ResponseStatus.ErrorStatus;

/**
 * Exceptions and timeouts in the pipeline will be handler here
 */
public class InBoundErrorHandler extends ChannelInboundHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger(InBoundErrorHandler.class);

    /**
     * This is the global exception handler for the pipeline.
     *
     * @param ctx
     * @param cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (ExceptionUtils.indexOfThrowable(cause, InvalidCommandException.class) >= 0) {
            // ErrorStatus.ERROR - poorly name, per protocol means invalid command
            ctx.write(new ErrorResult(ErrorStatus.ERROR));
            logger.info("client error - invalid command");
        }
        else if(ExceptionUtils.indexOfThrowable(cause, ClientException.class) >= 0) {
            ctx.write(new ErrorResult(ErrorStatus.CLIENT_ERROR, cause.getMessage()));
            logger.info("client error - " + cause.getMessage());
        }
        else { // all other exceptions are server exceptions
            // not sure what is appropriate reason here
            ctx.write(new ErrorResult(ErrorStatus.SERVER_ERROR, "the server encountered an error"));
            logger.error("received a server error", cause);
        }

        // always close connection on error
        ctx.close();
    }

    /**
     * Handles server / client timeout errors.
     * Server taking to long could be excess load. Cutting off client can help shed load.
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                // just close idle client connection
                ctx.close();
                logger.info("closed idle client connection");
            } else if (e.state() == IdleState.WRITER_IDLE) {
                // if the server took too long, write server error message and close socket
                ctx.writeAndFlush(new ErrorResult(ErrorStatus.SERVER_ERROR, "server took too long"))
                        .addListener(ChannelFutureListener.CLOSE);
                logger.info("sent server error to client and closed connection");
            }
        }
    }
}
