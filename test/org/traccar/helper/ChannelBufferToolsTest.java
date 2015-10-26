package org.traccar.helper;

import org.jboss.netty.buffer.ChannelBuffers;

import static org.junit.Assert.*;
import org.junit.Test;

public class ChannelBufferToolsTest {
    
    @Test
    public void testReadHexInteger() {
        byte[] buf = {0x01, (byte) 0x90, 0x34};
        int result = ChannelBufferTools.readHexInteger(
                ChannelBuffers.wrappedBuffer(buf), 5);
        assertEquals(1903, result);
    }

}
