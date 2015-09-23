package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;

public class AutoFon45ProtocolDecoderTest extends ProtocolDecoderTest {
    @Test
    public void testDecode() throws Exception {
        AutoFon45ProtocolDecoder decoder = new AutoFon45ProtocolDecoder(new AutoFon45Protocol());

        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "41032125656985547543619173484002123481"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "023E00001E004D411EFA01772F185285009C48041F1E366C2961380F26B10B00911C"))));
    }
}
