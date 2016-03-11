package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class HomtecsProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        HomtecsProtocolDecoder decoder = new HomtecsProtocolDecoder(new HomtecsProtocol());

        verifyPosition(decoder, text(
                "strommabus939_R01272028,160217,191003.00,06,5540.12292,N,01237.49814,E,0.391,,1,1.27,1.2"));

    }

}
