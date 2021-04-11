package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class DwayProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new DwayProtocolDecoder(null);

        verifyPosition(decoder, text(
                "AA55,36,10024,1,171025,161055,36.0294,-79.7881,201, 2.5,111,1000,0000,00000,3578,0,0,0,D"));

        verifyPosition(decoder, text(
                "AA55,115,318,1,171024,195059,28.0153,-82.4761,3, 1.0,319,1000,0000,00000,4244,0,0,0,D"));

        verifyPosition(decoder, text(
                "AA55,117,318,1,171025,153758,28.0152,-82.4759,19, 0.6,319,1000,0000,10000,4242,0,0,0,D"));

        verifyPosition(decoder, text(
                "AA55,1,123456,1,140101,101132,22.5500,113.6770,75,70.5,320,1100,0011,1110,3950,33000,24000,12345678"));

        verifyNull(decoder, text(
                "AA55,HB"));

    }

}
