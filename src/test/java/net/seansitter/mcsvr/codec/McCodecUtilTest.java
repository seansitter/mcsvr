package net.seansitter.mcsvr.codec;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class McCodecUtilTest {
    McCodecUtil codecUtil;

    @Before
    public void setup() {
        codecUtil = new McCodecUtil();
    }

    @Test
    public void tesHasNoPayload() {
        assertFalse(codecUtil.hasPayload("get"));
        assertFalse(codecUtil.hasPayload("gets"));
        assertFalse(codecUtil.hasPayload("delete"));
    }

    @Test
    public void tesHasPayload() {
        assertTrue(codecUtil.hasPayload("set"));
        assertTrue(codecUtil.hasPayload("cas"));
    }
}
