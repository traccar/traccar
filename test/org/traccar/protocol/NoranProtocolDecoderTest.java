package org.traccar.protocol;

import java.nio.ByteOrder;
import org.traccar.helper.TestDataManager;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

public class NoranProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        NoranProtocolDecoder decoder = new NoranProtocolDecoder(null);

        assertNull(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "0f0000004e52303946303431353500"))));

        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "22000800010c008a007e9daa42317bdd41a7f3e2384e523039463034313535000000"))));

        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "34000800010c0000000000001c4291251143388d17c24e523039423131303930000031342d31322d32352030303a33333a303700"))));

    }

}
