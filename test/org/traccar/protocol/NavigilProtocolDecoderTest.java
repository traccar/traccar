package org.traccar.protocol;

import java.nio.ByteOrder;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class NavigilProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        NavigilProtocolDecoder decoder = new NavigilProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        //byte[] buf1 = {0x40,0x4E,0x54,0x43,0x01,0x00,0x00,0x00,0x7B,0x00,0x00,0x00,0x13,0x00,0x44,0x34,0x2A,0x3E,0x53,0x3A,0x38,0x36,0x31,0x37,0x38,0x35,0x30,0x30,0x35,0x32,0x30,0x35,0x30,0x37,0x39};
        //assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, buf1)));

    }

}
