package org.traccar.protocol;

import java.nio.ByteOrder;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class GalileoProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        GalileoProtocolDecoder decoder = new GalileoProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        byte[] buf1 = {0x01,0x13,(byte)0x80,0x03,0x38,0x36,0x38,0x32,0x30,0x34,0x30,0x30,0x31,0x35,0x34,0x39,0x30,0x38,0x37,0x04,0x32,0x00,(byte)0x85,(byte)0x90};
        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, buf1)));

    }

}
