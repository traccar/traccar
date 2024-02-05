package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class RaceDynamicsProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new RaceDynamicsProtocolDecoder(null));

        verifyNull(decoder, text(
                "$GPRMC,12,260819,100708,862549040661129,"));

        verifyPositions(decoder, text(
                "$GPRMC,15,04,H,#,100632,A,1255.5106,N,07738.2954,E,001,260819,0887,06,1,00011,%,0000000000000000,000,000,0,0,1,0713,0,416,0,255,000,0,000,3258,000,000,00,0000,000,00000,0,F3VF01,%,#,#,100633,A,1255.5107,N,07738.2955,E,001,260819,0887,06,1,00012,%,0000000000000000,000,000,0,0,1,0713,0,416,0,255,000,0,000,3453,000,000,00,0000,000,00000,0,F3VF01,%,#,#,100634,A,1255.5106,N,07738.2964,E,001,260819,0887,06,1,00013,%,0000000000000000,000,000,0,0,1,0713,0,392,0,255,000,0,000,3651,000,000,00,0000,000"));

    }

}
