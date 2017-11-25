package net.seansitter.mcsvr.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import net.seansitter.mcsvr.cache.CacheEntry;
import net.seansitter.mcsvr.cache.CacheValue;
import net.seansitter.mcsvr.cache.ResponseStatus;
import net.seansitter.mcsvr.domain.result.*;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

public class McTextEncoderTest {
    McTextEncoder encoder;
    ByteBuf out;
    int flags = 5;
    int casUniq = 12;

    @Before
    public void setup() {
        encoder = new McTextEncoder();
        out = Unpooled.buffer();
    }

    @Test
    public void testEncodeGetResult() {
        String k1 = "some_key";
        String v1 = "some_value";
        String k2 = "other_key";
        String v2 = "other_value";
        encoder.doEncode(
                newGetCacheResult(
                        newCacheEnrty(k1, v1),
                        newCacheEnrty(k2, v2)
                ),
                out
        );

        StringBuilder sb = new StringBuilder();
        sb.append("VALUE " + k1 + " " + flags + " " + byteLen(v1) + "\r\n");
        sb.append(v1+"\r\n");
        sb.append("VALUE " + k2 + " " + flags + " " + byteLen(v2) + "\r\n");
        sb.append(v2+"\r\n");
        sb.append("END\r\n");

        assertEquals("encoding is expected", sb.toString(), out.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testEncodeGetsResult() {
        String k1 = "some_key";
        String v1 = "some_value";
        String k2 = "other_key";
        String v2 = "other_value";
        encoder.doEncode(
                newGetsCacheResult(
                        newCacheEnrty(k1, v1),
                        newCacheEnrty(k2, v2)
                ),
                out
        );

        StringBuilder sb = new StringBuilder();
        sb.append("VALUE " + k1 + " " + flags + " " + byteLen(v1) + " " + casUniq + "\r\n");
        sb.append(v1+"\r\n");
        sb.append("VALUE " + k2 + " " + flags + " " + byteLen(v2) + " " + casUniq + "\r\n");
        sb.append(v2 + "\r\n");
        sb.append("END\r\n");

        assertEquals("encoding is expected", sb.toString(), out.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testServerErrorResult() {
        String errMsg = "something bad";
        ErrorResult er = new ErrorResult(ResponseStatus.ErrorStatus.SERVER_ERROR, errMsg);
        encoder.doEncode(er, out);
        assertEquals("got server error status result", "SERVER_ERROR "+errMsg+"\r\n", out.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testClientErrorResult() {
        String errMsg = "something bad";
        ErrorResult er = new ErrorResult(ResponseStatus.ErrorStatus.CLIENT_ERROR, errMsg);
        encoder.doEncode(er, out);
        assertEquals("got client error status result", "CLIENT_ERROR "+errMsg+"\r\n", out.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testErrorResult() {
        ErrorResult er = new ErrorResult(ResponseStatus.ErrorStatus.ERROR);
        encoder.doEncode(er, out);
        assertEquals("got error status result", "ERROR\r\n", out.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testStoredResult() {
        StoreCacheResult sr = new StoreCacheResult(ResponseStatus.StoreStatus.STORED);
        encoder.doEncode(sr, out);
        assertEquals("got stored result", "STORED\r\n", out.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testNotStoredResult() {
        StoreCacheResult sr = new StoreCacheResult(ResponseStatus.StoreStatus.NOT_STORED);
        encoder.doEncode(sr, out);
        assertEquals("got not stored result", "NOT_STORED\r\n", out.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testExistsResult() {
        StoreCacheResult sr = new StoreCacheResult(ResponseStatus.StoreStatus.EXISTS);
        encoder.doEncode(sr, out);
        assertEquals("got exists result", "EXISTS\r\n", out.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testNotFoundResult() {
        StoreCacheResult sr = new StoreCacheResult(ResponseStatus.StoreStatus.NOT_FOUND);
        encoder.doEncode(sr, out);
        assertEquals("got not found result", "NOT_FOUND\r\n", out.toString(CharsetUtil.UTF_8));
    }

    private GetsCacheResult newGetsCacheResult(CacheEntry<CacheValue>... values) {
        List<CacheEntry<CacheValue>> results = Arrays.asList(values);
        return new GetsCacheResult(results);
    }

    private GetCacheResult newGetCacheResult(CacheEntry<CacheValue>... values) {
        List<CacheEntry<CacheValue>> results = Arrays.asList(values);
        return new GetCacheResult(results);
    }

    private CacheEntry<CacheValue> newCacheEnrty(String key, String payload) {
        return newCacheEnrty(key, payload, flags, casUniq);
    }

    private CacheEntry<CacheValue> newCacheEnrty(String key, String payload, long flag, long casUnique) {
        long time = getTime();
        CacheValue v = new CacheValue(payload.getBytes(CharsetUtil.UTF_8), flag, time, time + 15, casUnique);
        return new CacheEntry<>(key, v);
    }

    private long getTime() {
        return System.currentTimeMillis() / 1000;
    }

    private int byteLen(String s) {
        return s.getBytes(CharsetUtil.UTF_8).length;
    }
}
