package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class ExtremTracProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        ExtremTracProtocolDecoder decoder = new ExtremTracProtocolDecoder(new ExtremTracProtocol());

        verifyPosition(decoder, text(
                "$GPRMC,10000000001,092313.299,A,2238.8947,N,11355.2253,E,0.00,311.19,010307,0,,"));

        verifyPosition(decoder, text(
                "$GPRMC,00000000000,092244.000,A,0000.0000,S,00000.0000,E,0.00,0.00,101016,0,,8000,0"));

        verifyNothing(decoder, text(
                "$GPRMC,092313.299,A,2238.8947,N,11355.2253,E,0.00,311.19,010307,0,,1111,1111"));

        verifyNothing(decoder, text(
                "$GPRMC,092313.299,A,2238.8947,N,11355.2253,E,0.00,311.19,010307,0,,"));

        verifyNothing(decoder, text(
                "$GPRMC,100936.000,A,0000.0000,S,00000.0000,E,0.00,0.00,101016,0,,8000,0"));

    }

}
