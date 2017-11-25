package net.seansitter.mcsvr.codec;

import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.*;

import io.netty.util.CharsetUtil;
import net.seansitter.mcsvr.domain.command.DeleteCommand;
import net.seansitter.mcsvr.domain.command.GetCommand;
import net.seansitter.mcsvr.domain.command.StoreCommand;
import net.seansitter.mcsvr.exception.InvalidCommandException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class McTextDecoderTest {
    McTextDecoder decoder;
    ByteBuf buf;
    List<Object> out;

    @Before
    public void setup(){
        decoder = new McTextDecoder(new McCodecUtil());
        buf = buffer(128);
        out = new ArrayList<>();
    }

    @Test
    public void testGetSingle() {
        writeString("get some_key");
        writeCrlf();
        decoder.doDecode(buf, out);

        assertEquals("1 decoded item", 1, out.size());
        assertTrue("command is a get type", out.get(0) instanceof GetCommand);
        GetCommand c = (GetCommand)out.get(0);
        assertEquals("1 key in get command", 1, c.getKeys().size());
        assertTrue("correct key on get", c.getKeys().get(0).equals("some_key"));
        assertEquals("command name is get", "get", c.getName());
    }

    @Test
    public void testGetMultiple() {
        writeString("get first_key middle_key last_key");
        writeCrlf();

        decoder.doDecode(buf, out);
        GetCommand c = (GetCommand)out.get(0);

        assertEquals("3 keys in get command", 3, c.getKeys().size());
        assertTrue("correct 1st key on get", c.getKeys().get(0).equals("first_key"));
        assertTrue("correct 2nd key on get", c.getKeys().get(1).equals("middle_key"));
        assertTrue("correct 3rd key on get", c.getKeys().get(2).equals("last_key"));
    }

    @Test
    public void testDecodeTextLineMultPasses() {
        writeString("get first_ke");
        decoder.doDecode(buf, out);
        assertEquals("0 decoded items", 0, out.size());

        writeString("y middle_key last_key");
        decoder.doDecode(buf, out);
        assertEquals("0 decoded items", 0, out.size());

        writeCrlf();
        decoder.doDecode(buf, out);
        assertEquals("1 decoded items", 1, out.size());
    }

    @Test
    public void testGets() {
        writeString("gets some_key");
        writeCrlf();
        decoder.doDecode(buf, out);

        assertEquals("command name is gets", "gets", ((GetCommand)out.get(0)).getName());
    }

    @Test
    public void testDelete() {
        writeString("delete some_key");
        writeCrlf();
        decoder.doDecode(buf, out);

        assertTrue("command is a delete type", out.get(0) instanceof  DeleteCommand);
        assertEquals("command name is delete", "delete", ((DeleteCommand)out.get(0)).getName());
    }

    @Test
    public void testDeleteReply() {
        writeString("delete some_key");
        writeCrlf();
        decoder.doDecode(buf, out);

        assertFalse("command is reply", ((DeleteCommand)out.get(0)).isNoReply());
    }

    @Test
    public void testDeleteNoReply() {
        writeString("delete some_key noreply");
        writeCrlf();
        decoder.doDecode(buf, out);

        assertTrue("command is reply", ((DeleteCommand)out.get(0)).isNoReply());
    }

    @Test
    public void testSet() {
        String v = "this is the value";
        writeString("set some_key 2 5 "+byteLen(v));
        writeCrlf();
        writeString(v);
        writeCrlf();
        decoder.doDecode(buf, out);

        assertEquals("1 decoded item", 1, out.size());
        assertTrue("command is a store type", out.get(0) instanceof StoreCommand);

        StoreCommand c = (StoreCommand)out.get(0);
        assertEquals("command is set", "set", c.getName());
        assertEquals("set flag", 2, c.getFlags());
        assertEquals("set expiration", 5, c.getExpTime());
        assertEquals("set bytelen", byteLen(v), c.getContentLen());
        assertFalse("set is not noreply", c.isNoReply());
    }

    @Test
    public void testSetNoReply() {
        String v = "this is the value";
        writeString("set some_key 2 5 "+byteLen(v)+" noreply");
        writeCrlf();
        writeString(v);
        writeCrlf();
        decoder.doDecode(buf, out);

        StoreCommand c = (StoreCommand)out.get(0);
        assertTrue("set is noreply", c.isNoReply());
        assertEquals("buf has no readable bytes", 0, buf.readableBytes());
    }

    @Test
    public void testSetMultiWrite() {
        String v = "this is the value";
        writeString("set some_key 2 5 "+byteLen(v)+" noreply");
        writeCrlf();
        writeString("this is th");
        decoder.doDecode(buf, out);

        assertEquals("no command in output", 0, out.size());

        writeString("e value");
        writeCrlf();
        decoder.doDecode(buf, out);
        assertEquals("1 command in output", 1, out.size());
    }

    /**
     * Tests multiple commands in the buffer
     */
    @Test
    public void testMultiDecode() {
        String v = "this is the value";
        writeString("set some_key 2 5 "+byteLen(v)+" noreply");
        writeCrlf();
        writeString(v);
        writeCrlf();

        v = "this is the other value";
        writeString("set other_key 2 5 "+byteLen(v)+" noreply");
        writeCrlf();
        writeString(v);
        writeCrlf();

        decoder.doDecode(buf, out);
        assertEquals("2 commands in output", 2, out.size());
    }

    @Test
    public void testCasDecode() {
        String v = "this is the value";
        writeString("cas some_key 2 5 "+byteLen(v)+" 21");
        writeCrlf();
        writeString(v);
        writeCrlf();
        decoder.doDecode(buf, out);

        assertEquals("1 decoded item", 1, out.size());
        assertTrue("command is a store type", out.get(0) instanceof StoreCommand);

        StoreCommand c = (StoreCommand)out.get(0);
        assertEquals("command is cas", "cas", c.getName());
        assertEquals("cas flag", 2, c.getFlags());
        assertEquals("cas expiration", 5, c.getExpTime());
        assertEquals("cas bytelen", byteLen(v), c.getContentLen());
        assertEquals("cas unique value", 21, c.getCasUnique());
        assertFalse("cas is not noreply", c.isNoReply());
    }

    @Test
    public void testCasNoReply() {
        String v = "this is the value";
        writeString("cas some_key 2 5 "+byteLen(v)+" 21 noreply");
        writeCrlf();
        writeString(v);
        writeCrlf();
        decoder.doDecode(buf, out);

        StoreCommand c = (StoreCommand)out.get(0);
        assertTrue("cas is noreply", c.isNoReply());
    }

    /**
     * Tests multiple commands in the buffer
     */
    @Test(expected = InvalidCommandException.class)
    public void testInvalidCommand() {
        writeString("foo some_key");
        writeCrlf();
        decoder.doDecode(buf, out);
    }

    void writeString(String s) {
        writeString(s, buf);
    }

    void writeString(String s, ByteBuf out) {
        out.writeBytes(s.getBytes(CharsetUtil.UTF_8));
    }

    void writeCrlf() {
        writeCrlf(buf);
    }

    void writeCrlf(ByteBuf out) {
        out.writeBytes("\r\n".getBytes(CharsetUtil.UTF_8));
    }

    int byteLen(String s) {
        return s.getBytes(CharsetUtil.UTF_8).length;
    }
}
