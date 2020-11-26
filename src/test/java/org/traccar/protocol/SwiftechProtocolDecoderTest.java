package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class SwiftechProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        SwiftechProtocolDecoder decoder = new SwiftechProtocolDecoder(null);

        verifyPosition(decoder, text(
                "@@864502036102531,,,070739,1100.7798,N,07657.7126,E,0.43,210813,A,1100,1,0,02700,05800,"));

    }

}
