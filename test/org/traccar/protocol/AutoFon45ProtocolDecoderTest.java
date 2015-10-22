package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.ProtocolDecoderTest;
import org.traccar.helper.ChannelBufferTools;


public class AutoFon45ProtocolDecoderTest extends ProtocolDecoderTest {
    @Test
    public void testDecode() throws Exception {
        AutoFon45ProtocolDecoder decoder = new AutoFon45ProtocolDecoder(new AutoFon45Protocol());

        verifyNothing(decoder, binary(
                "41032125656985547543619173484002123481"));

        verifyPosition(decoder, binary(
                "023E00001E004D411EFA01772F185285009C48041F1E366C2961380F26B10B00911C"));
    }
}
