package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class MobilogixProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new MobilogixProtocolDecoder(null);

        verifyNull(decoder, text(
                "[2020-12-01 14:00:22,T1,1,V1.1.1,201951132031,,,12345678,724108005415815,359366080211420"));

        verifyPosition(decoder, text(
                "[2020-12-01 12:01:09,T3,1,V1.1.1,201951132031,3B,12.99,022,-23.563410,-46.588055,0,0"));

        verifyPosition(decoder, text(
                "[2021-09-30 20:06:35,T21,1,V1.3.5,201950130047,37,14.97,092,-23.494715,-46.851341,0,240,4.08,0,19516,4431,0.78,724,10,09111,00771,31,4680"));

    }

}
