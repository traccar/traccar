package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.TestDataManager;

import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;

public class TramigoProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        TramigoProtocolDecoder decoder = new TramigoProtocolDecoder(new TestDataManager(), null, null);
        
        //verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertArray(
        //        new int[] {0x68,0x68,0x25,0x00,0x00,0x01,0x23,0x45,0x67,0x89,0x01,0x23,0x45,0x00,0x01,0x10,0x01,0x01,0x01,0x01,0x01,0x01,0x02,0x6B,0x3F,0x3E,0x02,0x6B,0x3F,0x3E,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x01,0x0D,0x0A}))));

    }

}
