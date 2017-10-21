package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class GenxProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        GenxProtocolDecoder decoder = new GenxProtocolDecoder(new GenxProtocol());

        verifyPosition(decoder, text(
                "000036004130,08/31/2017 17:24:13,45.47275,-73.65491,5,19,117,1.14,147,ON,1462,0,6,N,0,0.000,-95.0,-1.0,0,0.0000,0.0000,0.000,0,0.00,0.00,0.00,NA,U,UUU,0,-95.0,U"));

        verifyPosition(decoder, text(
                "000036004130,08/31/2017 17:24:37,45.47257,-73.65506,3,0,117,1.14,124,ON,1489,0,5,N,0,0.000,-95.0,-1.0,0,0.0000,0.0000,0.000,0,0.00,0.00,0.00,NA,U,UUU,0,-95.0,U"));

        decoder.setReportColumns("1,2,3,4");

        verifyPosition(decoder, text(
                "000036035855,04/16/2017 21:19:07,45.46485,-73.65424,24,32,61:213,342.51,157,ON,20984,0,12,O,18,0.000,95.0,24.0,1990,64.0894,0.0219,316.009,71,0.00,16.78,5.10,NA,U,UUU,0,-95.0,U"));

    }

}
