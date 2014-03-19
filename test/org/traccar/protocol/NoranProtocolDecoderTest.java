package org.traccar.protocol;

import java.nio.ByteOrder;
import org.traccar.helper.TestDataManager;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

public class NoranProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        NoranProtocolDecoder decoder = new NoranProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        int[] buf1 = {0x0f,0x00,0x00,0x00,0x4e,0x52,0x30,0x39,0x46,0x30,0x34,0x31,0x35,0x35,0x00};
        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertArray(buf1))));
        
        int[] buf2 = {0x22,0x00,0x08,0x00,0x01,0x0c,0x00,0x8a,0x00,0x7e,0x9d,0xaa,0x42,0x31,0x7b,0xdd,0x41,0xa7,0xf3,0xe2,0x38,0x4e,0x52,0x30,0x39,0x46,0x30,0x34,0x31,0x35,0x35,0x00,0x00,0x00};
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertArray(buf2))));

    }

}
