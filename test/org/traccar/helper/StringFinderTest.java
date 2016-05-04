package org.traccar.helper;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class StringFinderTest {
    
    @Test
    public void testFind() {

        ChannelBuffer buf = ChannelBuffers.copiedBuffer("hello world", StandardCharsets.US_ASCII);

        Assert.assertEquals(-1, buf.indexOf(0, buf.writerIndex(), new StringFinder("bar")));
        Assert.assertEquals(6, buf.indexOf(0, buf.writerIndex(), new StringFinder("world")));
        Assert.assertEquals(-1, buf.indexOf(0, buf.writerIndex(), new StringFinder("worlds")));
        Assert.assertEquals(0, buf.indexOf(0, buf.writerIndex(), new StringFinder("hell")));

    }

}
