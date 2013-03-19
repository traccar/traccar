package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class GalileoProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        GalileoProtocolDecoder decoder = new GalileoProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        /*byte[] buf1 = {0x00};
        assertNotNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(buf1)));*/

    }

}
