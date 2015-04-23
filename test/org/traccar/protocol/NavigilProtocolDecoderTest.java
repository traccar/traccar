package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import java.nio.ByteOrder;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import static org.traccar.helper.DecoderVerifier.verify;

public class NavigilProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        NavigilProtocolDecoder decoder = new NavigilProtocolDecoder(null);

        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "01004300040020000000f60203080200e7cd0f510c0000003b00000000000000"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "0100b3000f0024000000f4a803080200ca0c1151ef8885f0b82e6d130400c00403000000"))));

    }

}
