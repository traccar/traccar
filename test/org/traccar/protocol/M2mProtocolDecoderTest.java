package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

public class M2mProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        M2mProtocolDecoder decoder = new M2mProtocolDecoder(new M2mProtocol());

        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "235A3C2A2624215C287D70212A21254C7C6421220B0B0B"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "A6E12C2AAADA4628326B2059576E30202A2FE85D20200B"))));

    }

}
