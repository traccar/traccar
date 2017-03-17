package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class StarLinkProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        StarLinkProtocolDecoder decoder = new StarLinkProtocolDecoder(new StarLinkProtocol());

        verifyPosition(decoder, text(
                "$SLU006968,06,375153,170117051824,01,170117051823,+3203.2073,+03448.1360,000.0,300,085725,1,1,0,0,0,0,0,0,10422,36201,12.655,04.085,,0,0,0,99*45"));

        verifyPosition(decoder, text(
                "$SLU006968,06,375155,170117052615,24,170117052613,+3203.2079,+03448.1369,000.0,300,085725,1,1,0,0,0,0,0,0,10422,36201,14.290,04.083,,1,1*5B"));

        verifyPosition(decoder, text(
                "$SLU006968,06,375156,170117052616,34,170117052614,+3203.2079,+03448.1369,000.0,300,085725,1,1,0,0,0,0,0,0,10422,36201,14.277,04.084,1,1,1,1*F3"));

        verifyPosition(decoder, text(
                "$SLU006968,06,375154,170117052613,04,170117052612,+3203.2079,+03448.1369,000.0,300,085725,1,1,0,0,0,0,0,0,10422,36201,14.287,04.084,,1,0*5B"));

    }

}
