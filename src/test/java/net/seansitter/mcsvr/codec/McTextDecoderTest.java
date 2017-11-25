package net.seansitter.mcsvr.codec;

import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.*;

import io.netty.util.CharsetUtil;
import net.seansitter.mcsvr.domain.command.DeleteCommand;
import net.seansitter.mcsvr.domain.command.GetCommand;
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
        writeString("delete some_key noreply");
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
}
