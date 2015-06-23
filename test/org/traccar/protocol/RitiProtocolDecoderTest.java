package org.traccar.protocol;

import java.nio.ByteOrder;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import static org.traccar.helper.DecoderVerifier.verify;
import org.traccar.helper.TestDataManager;

public class RitiProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        RitiProtocolDecoder decoder = new RitiProtocolDecoder(new RitiProtocol());

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "3b2864a3056300006d40000003000000000000000000000000244750524d432c3231313734332e3030302c412c313335372e333637352c4e2c31303033362e363939322c452c302e30302c2c3031303931342c2c2c412a37380d0a00000000000000000000000000000000040404"))));

    }

}
