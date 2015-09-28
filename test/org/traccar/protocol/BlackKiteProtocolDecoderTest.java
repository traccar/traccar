package org.traccar.protocol;

import java.nio.ByteOrder;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import static org.traccar.helper.DecoderVerifier.verify;

public class BlackKiteProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        BlackKiteProtocolDecoder decoder = new BlackKiteProtocolDecoder(new BlackKiteProtocol());

        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "01150003313131313131313131313131313131209836055605BA"))));
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "0136000331313131313131313131313131313120523905563000010000000100000033000000003400004000004500004600005000005100009F76"))));

    }

}
