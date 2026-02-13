package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class OpenGtsProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new OpenGtsProtocolDecoder(null));

        verifyPosition(decoder, request(
                "/?id=999000000000003&gprmc=$GPRMC,082202.0,A,5006.747329,N,01416.512315,E,0.0,,131018,1.2,E,A*2E"));

        verifyPosition(decoder, request(
                "/?id=gprmc_999000000000003&gprmc=$GPRMC,143013.0,A,5006.728217,N,01416.437869,E,0.0,329.6,281017,1.2,E,A*0E"));

        verifyPosition(decoder, request(
                "/?id=123456789012345&dev=dev_name&acct=account&batt=0&code=0xF020&alt=160.5&gprmc=$GPRMC,191555,A,5025.46624,N,3030.39937,E,0.000000,0.000000,200218,,*2F"));

    }

}
