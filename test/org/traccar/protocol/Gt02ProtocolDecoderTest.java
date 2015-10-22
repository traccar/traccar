package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.ProtocolDecoderTest;
import org.traccar.helper.ChannelBufferTools;

public class Gt02ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Gt02ProtocolDecoder decoder = new Gt02ProtocolDecoder(new Gt02Protocol());

        verifyPosition(decoder, binary(
                "68682500000123456789012345000110010101010101026B3F3E026B3F3E000000000000000000010D0A"));

        verifyNothing(decoder, binary(
                "6868110603035889905101276600001a0402292d0d0a"));

        verifyPosition(decoder, binary(
                "68682500a403588990510127660001100e09060a1d1b00ade1c90b79ea3000011b000000000000050d0a"));

    }

}
