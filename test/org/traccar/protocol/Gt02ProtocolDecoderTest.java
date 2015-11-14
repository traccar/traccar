package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

public class Gt02ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Gt02ProtocolDecoder decoder = new Gt02ProtocolDecoder(new Gt02Protocol());

        verifyAttributes(decoder, binary(
                "68681a060303588990500037252de91a010a171a191b171915191e10000d0a"));

        verifyPosition(decoder, binary(
                "68682500000123456789012345000110010101010101026B3F3E026B3F3E000000000000000000010D0A"),
                position("2001-01-01 01:01:01.000", true, -22.54610, -22.54610));

        verifyAttributes(decoder, binary(
                "6868110603035889905101276600001a0402292d0d0a"));

        verifyPosition(decoder, binary(
                "68682500a403588990510127660001100e09060a1d1b00ade1c90b79ea3000011b000000000000050d0a"));

    }

}
