package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class OpenGtsDatagramProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

    	OpenGtsDatagramProtocolDecoder decoder = new OpenGtsDatagramProtocolDecoder(null);

        verifyPosition(decoder, text(
                "999000000000003/account/$GPRMC,082202.0,A,5006.747329,N,01416.512315,E,0.0,,131018,1.2,E,A*2E"));

        verifyPosition(decoder, text(
                "gprmc_999000000000003/account/$GPRMC,143013.0,A,5006.728217,N,01416.437869,E,0.0,329.6,281017,1.2,E,A*0E"));

        verifyPosition(decoder, text(
                "123456789012345/account/$GPRMC,191555,A,5025.46624,N,3030.39937,E,0.000000,0.000000,200218,,*2F"));

    }
}
