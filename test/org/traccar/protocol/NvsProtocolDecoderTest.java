package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class NvsProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        NvsProtocolDecoder decoder = new NvsProtocolDecoder(new NvsProtocol());

        verifyNothing(decoder, binary(
                "0012333537303430303630303137383234312e38"));

        verifyNothing(decoder, binary(
                "0012313233343536373839303132333435312E31"));

        verifyPositions(decoder, binary(
                "CCCCCCCC00FE00007048860DDF79020446a6f1ce010f14f650209cca80006f00d6040004010300030101150316030001460000015d0046a6f1dc0d0f14ffe0209cc580006e00c7050001010300030101150316010001460000015e0046a6f1ea0e0f150f00209cd20000950108040000010300030101150016030001460000015d0046a6f1ff0b0f150a50209cccc000930068040000010300030101150016030001460000015b006123"));

    }

}
