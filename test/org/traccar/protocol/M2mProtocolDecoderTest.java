package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;

import org.junit.Test;
import org.traccar.ProtocolDecoderTest;
import org.traccar.helper.ChannelBufferTools;

public class M2mProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        M2mProtocolDecoder decoder = new M2mProtocolDecoder(new M2mProtocol());

        verifyNothing(decoder, binary(
                "235A3C2A2624215C287D70212A21254C7C6421220B0B0B"));

        verifyPosition(decoder, binary(
                "A6E12C2AAADA4628326B2059576E30202A2FE85D20200B"));

    }

}
