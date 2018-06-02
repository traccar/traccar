package org.traccar.helper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class StringFinderTest {
    
    @Test
    public void testFind() {

        ByteBuf buf = Unpooled.copiedBuffer("hello world", StandardCharsets.US_ASCII);

        // TODO update test
        /*assertEquals(-1, buf.indexOf(0, buf.writerIndex(), new StringFinder("bar")));
        assertEquals(6, buf.indexOf(0, buf.writerIndex(), new StringFinder("world")));
        assertEquals(-1, buf.indexOf(0, buf.writerIndex(), new StringFinder("worlds")));
        assertEquals(0, buf.indexOf(0, buf.writerIndex(), new StringFinder("hell")));*/

    }

}
