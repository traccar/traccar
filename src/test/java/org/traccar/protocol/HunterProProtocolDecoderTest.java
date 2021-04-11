package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class HunterProProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new HunterProProtocolDecoder(null);

        verifyPosition(decoder, text(
                ">0002<$GPRMC,170559.000,A,0328.3045,N,07630.0735,W,0.73,266.16,200816,,,A77, s000078015180\",0MD"));

    }

}
