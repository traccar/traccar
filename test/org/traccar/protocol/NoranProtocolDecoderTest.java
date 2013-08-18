package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

public class NoranProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        NoranProtocolDecoder decoder = new NoranProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        //int[] buf1 = {0x00};
        //assertNotNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertArray(buf1))));

    }

}
