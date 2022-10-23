package org.traccar.helper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class BufferUtilTest {

    @Test
    public void testReadSignedMagnitudeInt() {
        ByteBuf buf = Unpooled.wrappedBuffer(DataConverter.parseHex("80000001"));
        assertEquals(-1, BufferUtil.readSignedMagnitudeInt(buf));
    }

    @Test
    public void test1() {
        ByteBuf buf = Unpooled.copiedBuffer("abcdef", StandardCharsets.US_ASCII);
        assertEquals(2, BufferUtil.indexOf("cd", buf));
    }

    @Test
    public void test2() {
        ByteBuf buf = Unpooled.copiedBuffer("abcdef", StandardCharsets.US_ASCII);
        buf.readerIndex(1);
        buf.writerIndex(5);
        assertEquals(2, BufferUtil.indexOf("cd", buf));
    }

    @Test
    public void test3() {
        ByteBuf buf = Unpooled.copiedBuffer("abcdefgh", StandardCharsets.US_ASCII);
        buf.readerIndex(1);
        buf.writerIndex(7);
        assertEquals(3, BufferUtil.indexOf("de", buf, 2, 6));
    }

}
