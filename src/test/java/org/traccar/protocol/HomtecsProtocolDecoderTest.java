package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class HomtecsProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new HomtecsProtocolDecoder(null);

        verifyNull(decoder, text(
                "MDS0001_R6d1821f7,170323,143601.00,04,,,,,,,,,"));

        verifyPosition(decoder, text(
                "MDS0001_R6d1821f7,170323,143621.00,06,5105.29914,N,11400.52675,W,0.223,198.41,1,2.12,1042.3"));

        verifyPosition(decoder, text(
                "strommabus939_R01272028,160217,191003.00,06,5540.12292,N,01237.49814,E,0.391,,1,1.27,1.2"));

    }

}
