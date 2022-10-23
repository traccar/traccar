package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class SwiftechProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new SwiftechProtocolDecoder(null));

        verifyPosition(decoder, text(
                "@@861551041946971,,0,102040,1023.9670,N,07606.8160,E,2.26,151220,A,0127,1,1,03962,00000,#"));

        verifyPosition(decoder, text(
                "@@864502036102531,,,070739,1100.7798,N,07657.7126,E,0.43,210813,A,1100,1,0,02700,05800,"));

    }

}
