package net.seansitter.mcsvr.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheValue;
import net.seansitter.mcsvr.domain.result.CacheResult;
import net.seansitter.mcsvr.domain.result.GetCacheResult;
import net.seansitter.mcsvr.domain.result.GetsCacheResult;
import net.seansitter.mcsvr.domain.result.StatusCacheResult;

import java.nio.charset.Charset;

/**
 * This class implements the encoder for the memcache text protocol
 */
public class McTextEncoder extends MessageToByteEncoder<CacheResult> {
    @Override
    protected void encode(ChannelHandlerContext ctx, CacheResult msg, ByteBuf out) throws Exception {
        doEncode(msg, out);
    }

    protected void doEncode(CacheResult msg, ByteBuf out) {
        if (msg instanceof GetsCacheResult) {
            writeGetsCacheResult((GetsCacheResult)msg, out);
        }
        else if (msg instanceof GetCacheResult) {
            writeGetCacheResult((GetCacheResult)msg, out);
        }
        else if (msg instanceof StatusCacheResult) {
            writeStatusCacheResult((StatusCacheResult)msg, out);
        }
    }

    protected void writeGetCacheResult(GetCacheResult r, ByteBuf out) {
        r.getCacheEntries().forEach(e -> writeGetCacheEntry(e, out));
        writeEnd(out);
    }

    protected void writeGetsCacheResult(GetCacheResult r, ByteBuf out) {
        r.getCacheEntries().forEach(e -> writeGetsCacheEntry(e, out));
        writeEnd(out);
    }

    protected void writeGetCacheEntry(CacheEntry cacheEntry, ByteBuf out) {
        writeCacheValue(cacheEntry, out);
        writeCrlf(out);
        writeCachePayload(cacheEntry, out);
        writeCrlf(out);
    }

    protected void writeGetsCacheEntry(CacheEntry cacheEntry, ByteBuf out) {
        writeCacheValue(cacheEntry, out);
        writeString(String.format(" %d", cacheEntry.getValue().getCasUnique()), out);
        writeCrlf(out);
        writeCachePayload(cacheEntry, out);
        writeCrlf(out);
    }

    protected void writeCacheValue(CacheEntry cacheEntry, ByteBuf out) {
        CacheValue v = cacheEntry.getValue();
        writeString(String.format("VALUE %s %d %d", cacheEntry.getKey(), v.getFlag(), v.getSize()), out);
    }

    protected void writeCachePayload(CacheEntry cacheEntry, ByteBuf out) {
        out.writeBytes(cacheEntry.getValue().getPayload());
    }

    protected void writeStatusCacheResult(StatusCacheResult r, ByteBuf out) {
        writeString(r.getStatusString(), out);
        writeCrlf(out);
    }

    protected void writeEnd(ByteBuf out) {
        writeString("END", out);
        writeCrlf(out);
    }

    protected void writeString(String s, ByteBuf out) {
        out.writeCharSequence(s, Charset.forName("utf-8"));
    }

    protected void writeCrlf(ByteBuf out) {
        writeString("\r\n", out);
    }
}
