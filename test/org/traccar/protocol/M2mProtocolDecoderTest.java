package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

public class M2mProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        M2mProtocolDecoder decoder = new M2mProtocolDecoder(new TestDataManager());

        int[] buf1 = {0x23,0x5A,0x3C,0x2A,0x26,0x24,0x21,0x5C,0x28,0x7D,0x70,0x21,0x2A,0x21,0x25,0x4C,0x7C,0x64,0x21,0x22,0x0B,0x0B,0x0B};
        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertArray(buf1))));

        int[] buf2 = {0xA6,0xE1,0x2C,0x2A,0xAA,0xDA,0x46,0x28,0x32,0x6B,0x20,0x59,0x57,0x6E,0x30,0x20,0x2A,0x2F,0xE8,0x5D,0x20,0x20,0x0B};
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertArray(buf2))));

    }

}
