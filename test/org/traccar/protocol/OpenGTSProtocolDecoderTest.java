package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class OpenGTSProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        OpenGTSProtocolDecoder decoder = new OpenGTSProtocolDecoder(new OpenGTSProtocol());

        verifyPosition(decoder, text(
		"GET /gprmc/Data?id=123456789012345&dev=dev_name&acct=account&batt=0&code=0xF020&alt=160.5&gprmc=$GPRMC,191555,A,5025.46624,N,3030.39937,E,0.000000,0.000000,200218,,*2F "));
    }

}
