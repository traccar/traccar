package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class RaveonProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new RaveonProtocolDecoder(null);

        verifyPosition(decoder, text(
                "$PRAVE,0001,0001,3308.9051,-11713.1164,195348,1,10,168,31,13.3,3,-83,0,0,,1003.4*66"));

    }

}
