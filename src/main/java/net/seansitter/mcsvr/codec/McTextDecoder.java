package net.seansitter.mcsvr.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.seansitter.mcsvr.domain.command.ApiCommand;
import net.seansitter.mcsvr.domain.command.DeleteCommand;
import net.seansitter.mcsvr.domain.command.GetCommand;
import net.seansitter.mcsvr.domain.command.StoreCommand;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.util.List;

/**
 * This class implements the decoder for the memcache text protocol
 */
public class McTextDecoder extends ByteToMessageDecoder {
    private Object[] cmdLineObjs = null;
    private final McCodecUtil codecUtil;

    @Inject
    public McTextDecoder(McCodecUtil codecUtil) {
        this.codecUtil = new McCodecUtil();
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
            throws Exception {
        doDecode(in, out);
    }

    protected void doDecode(ByteBuf in, List<Object> out) {
        if (null == cmdLineObjs) {
            cmdLineObjs = parseCommand(in);
            if (null == cmdLineObjs) {
                return; // we don't have a full command yet, wait for more data
            }

            // if our command is retrieval we don't expect a payload so we're done
            if (!codecUtil.hasPayload((String)cmdLineObjs[0])) {
                ApiCommand cmd = cmdLineObjsToCmd(this.cmdLineObjs, null);
                out.add(cmd);
                reset();

                return;
            }
        }

        int byteLen = ((Integer)cmdLineObjs[4]).intValue();
        if (in.isReadable() && (in.readableBytes() >= byteLen + 2)) {
            byte[] payload = new byte[byteLen];
            in.readBytes(byteLen).getBytes(0, payload);
            ApiCommand cmd = cmdLineObjsToCmd(cmdLineObjs, payload);
            out.add(cmd);

            in.readBytes(2); // advance past \r\n
            reset();
        }
    }

    private void reset() {
        cmdLineObjs = null;
    }

    private Object[] parseCommand(ByteBuf in) {
        int rIdx = in.readerIndex();
        int endLnIdx = -1;
        for (int i = rIdx; i < rIdx+in.readableBytes()-1; i++) {
            // check current byte and lookahead to see if we are at end of text line
            if (in.getByte(i) == '\r' && in.getByte(i+1) == '\n') {
                // we have the text line
                endLnIdx = i+1;
                break;
            }
        }

        // didn't get a full text line
        if (endLnIdx < 0) {
            return null;
        }

        // don't include the cr-lf
        String cmdLine = in.readBytes((endLnIdx-1)-rIdx).toString(Charset.forName("utf-8"));
        in.readBytes(2);

        String[] cmdParts = cmdLine.split(" ");
        if (cmdParts.length < 2) {
            throw new DecoderException("Invalid text line");
        }

        String cmd = cmdParts[0];
        // if we have a retrieval / delete command, we are done
        if(cmd.equalsIgnoreCase("get") || cmd.equalsIgnoreCase("gets")) {
            if (cmdParts.length < 2) {
                throw new DecoderException("'"+cmd+"' command expects 1 or more key");
            }
            return cmdParts;
        }
        else if(cmd.equalsIgnoreCase("delete")) {
            if (cmdParts.length < 2) {
                throw new DecoderException("'delete' command expects 1 key");
            }
            return cmdParts;
        }
        else {
            if (cmd.equalsIgnoreCase("cas")) {
                return toCasArr(cmdParts);
            }
            else {
                return toStoreArr(cmdParts);
            }
        }
    }

    private Object[] toCasArr(String[] arr) {
        Object[] ret = new Object[arr.length];
        ret[0] = arr[0];
        ret[1] = arr[1];
        ret[2] = new Integer(Integer.parseUnsignedInt(arr[2])); // flags
        ret[3] = new Long(Long.parseUnsignedLong(arr[3])); // exptime
        ret[4] = new Integer(Integer.parseUnsignedInt(arr[4])); // bytes
        ret[5] = new Long(Long.parseUnsignedLong(arr[5])); // cas
        if (arr.length == 7) {
            ret[6] = arr[6];
        }
        return ret;
    }

    private Object[] toStoreArr(String[] arr) {
        Object[] ret = new Object[arr.length];
        ret[0] = arr[0];
        ret[1] = arr[1];
        ret[2] = new Integer(Integer.parseUnsignedInt(arr[2])); // flags
        ret[3] = new Long(Long.parseUnsignedLong(arr[3])); // exptime
        ret[4] = new Integer(Integer.parseUnsignedInt(arr[4])); // bytes
        if (arr.length == 6) {
            ret[5] = arr[5];
        }
        return ret;
    }

    private ApiCommand cmdLineObjsToCmd(Object[] cmdLineObjs, byte[] payload) {
        String cmd = (String)cmdLineObjs[0];

        if (cmd.equalsIgnoreCase("get") || cmd.equalsIgnoreCase("gets")) {
            GetCommand.Builder b = GetCommand.newBuilder();
                    b.withName(cmd.toLowerCase());
            for (int i = 1; i < cmdLineObjs.length; i++) { // more efficent - avoid creating extra lists
                b.withKey((String)cmdLineObjs[i]);
            }
            return b.build();
        }
        if (cmd.equalsIgnoreCase("delete")) {
            boolean isNoReply = (cmdLineObjs.length == 3 && ((String)cmdLineObjs[2]).equalsIgnoreCase("noreply"));

            return DeleteCommand.newBuilder()
                    .withKey((String)cmdLineObjs[1])
                    .withIsNoReply(isNoReply)
                    .build();
        }
        if (cmd.equalsIgnoreCase("set")) {
            boolean isNoReply = (cmdLineObjs.length == 6 && ((String)cmdLineObjs[5]).equalsIgnoreCase("noreply"));

            return StoreCommand.newBuilder()
                    .withName("set")
                    .withKey((String)cmdLineObjs[1])
                    .withFlags((Integer)cmdLineObjs[2])
                    .withExpTime((Long)cmdLineObjs[3])
                    .withIsNoReploy(isNoReply)
                    .withPayload(payload)
                    .build();
        }
        if (cmd.equalsIgnoreCase("cas")) {
            boolean isNoReply = (cmdLineObjs.length == 7 && ((String)cmdLineObjs[6]).equalsIgnoreCase("noreply"));

            return StoreCommand.newBuilder()
                    .withName("cas")
                    .withKey((String)cmdLineObjs[1])
                    .withFlags((Integer)cmdLineObjs[2])
                    .withExpTime((Long)cmdLineObjs[3])
                    .withIsNoReploy(isNoReply)
                    .withCasUnique((Long)cmdLineObjs[5])
                    .withPayload(payload)
                    .build();
        }
        return null;
    }
}
