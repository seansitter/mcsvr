package net.seansitter.mcsvr.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheValue;
import net.seansitter.mcsvr.domain.CacheResult;
import net.seansitter.mcsvr.domain.GetsCacheResult;
import net.seansitter.mcsvr.domain.StoreCacheResult;

import java.nio.charset.Charset;

public class McTextEncoder extends MessageToByteEncoder<CacheResult> {
    @Override
    protected void encode(ChannelHandlerContext ctx, CacheResult msg, ByteBuf out) throws Exception {
        if (msg instanceof GetsCacheResult) {
            writeGetsCacheResult((GetsCacheResult)msg, out);
        }
        if (msg instanceof StoreCacheResult) {
            writeStoreCacheResult((StoreCacheResult)msg, out);
        }
    }

    private void writeGetsCacheResult(GetsCacheResult r, ByteBuf out) {
        r.getCacheEntries().forEach(e -> writeCacheEntry(e, out));
        out.writeCharSequence("END\r\n", Charset.forName("utf-8"));
    }

    private void writeCacheEntry(CacheEntry cacheEntry, ByteBuf out) {
        CacheValue v = cacheEntry.getValue();
        out.writeCharSequence(
                String.format("VALUE %s %d %d",
                        cacheEntry.getKey(), v.getFlag(), v.getSize()),
                Charset.forName("utf-8"));
        out.writeCharSequence("\r\n", Charset.forName("utf-8"));
        out.writeBytes(v.getPayload());
        out.writeCharSequence("\r\n", Charset.forName("utf-8"));
    }

    private void writeStoreCacheResult(StoreCacheResult r, ByteBuf out) {

    }
}
