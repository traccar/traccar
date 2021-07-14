package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class GpsmtaProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new GpsmtaProtocolDecoder(null);

        verifyPosition(decoder, text(
                "3085a94ef459 1446536867 49.81621 24.054207 1 0 22 0 10 12 24 0 0"));

        verifyPosition(decoder, text(
                "864528021249771 1446116686 49.85073 24.004438 0 217 6 338 00 59 27 0 0"));

        verifyNotNull(decoder, text(
                "359144048138856 1442932957 -49.85064 -24.003979 1 0 40 0 10 110 26 0 0"));

    }

}
