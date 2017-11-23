package net.seansitter.mcsvr.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheValue;
import net.seansitter.mcsvr.domain.*;

import java.nio.charset.Charset;

public class McTextEncoder extends MessageToByteEncoder<CacheResult> {
    @Override
    protected void encode(ChannelHandlerContext ctx, CacheResult msg, ByteBuf out) throws Exception {
        doEncode(msg, out);
    }

    protected void doEncode(CacheResult msg, ByteBuf out) {
        if (msg instanceof GetsCacheResult) {
            writeGetsCacheResult((GetCacheResult)msg, out);
        }
        else if (msg instanceof GetCacheResult) {
            writeGetCacheResult((GetCacheResult)msg, out);
        }
        else if (msg instanceof StatusCacheResult) {
            writeStatusCacheResult((StatusCacheResult)msg, out);
        }
    }

    private void writeGetCacheResult(GetCacheResult r, ByteBuf out) {
        r.getCacheEntries().forEach(e -> writeGetCacheEntry(e, out));
        writeEnd(out);
    }

    private void writeGetsCacheResult(GetCacheResult r, ByteBuf out) {
        r.getCacheEntries().forEach(e -> writeGetsCacheEntry(e, out));
        writeEnd(out);
    }

    private void writeGetCacheEntry(CacheEntry cacheEntry, ByteBuf out) {
        writeCacheValue(cacheEntry, out);
        writeCrlf(out);
        writeCachePayload(cacheEntry, out);
        writeCrlf(out);
    }

    private void writeGetsCacheEntry(CacheEntry cacheEntry, ByteBuf out) {
        writeCacheValue(cacheEntry, out);
        writeString(String.format(" %d", cacheEntry.getValue().getSize()), out);
        writeCrlf(out);
        writeCachePayload(cacheEntry, out);
        writeCrlf(out);
    }

    private void writeCacheValue(CacheEntry cacheEntry, ByteBuf out) {
        CacheValue v = cacheEntry.getValue();
        writeString(String.format("VALUE %s %d %d", cacheEntry.getKey(), v.getFlag(), v.getSize()), out);
    }

    private void writeCachePayload(CacheEntry cacheEntry, ByteBuf out) {
        out.writeBytes(cacheEntry.getValue().getPayload());
    }

    private void writeStatusCacheResult(StatusCacheResult r, ByteBuf out) {
        writeString(r.getStatusString(), out);
        writeCrlf(out);
    }

    private void writeEnd(ByteBuf out) {
        writeString("END", out);
        writeCrlf(out);
    }

    private void writeString(String s, ByteBuf out) {
        out.writeCharSequence(s, Charset.forName("utf-8"));
    }

    private void writeCrlf(ByteBuf out) {
        writeString("\r\n", out);
    }
}
