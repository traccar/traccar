package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

public class HuabaoProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        HuabaoProtocolDecoder decoder = new HuabaoProtocolDecoder(new HuabaoProtocol());

        verifyNothing(decoder, binary(
                "7e0100002d007089994489002800000000000000000048422d523033474244000000000000000000000031393036373531024142433030303030d17e"));

    }

}
