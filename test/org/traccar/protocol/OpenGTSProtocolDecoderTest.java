package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class OpenGTSProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        OpenGTSProtocolDecoder decoder = new OpenGTSProtocolDecoder(new OpenGTSProtocol());

        verifyPosition(decoder, text(
                "4711/022789000688081/$GPRMC,133343,A,5308.56325,N,1029.12850,E,0.000000,0.000000,290316,,*2A"));
    }

}
