package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

public class AtrackProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        AtrackProtocolDecoder decoder = new AtrackProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        int[] buf1 = {0xfe,0x02,0x00,0x01,0x41,0x04,0xd8,0xf1,0x96,0x82,0x00,0x01};
        assertNotNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertArray(buf1))));

    }

}
