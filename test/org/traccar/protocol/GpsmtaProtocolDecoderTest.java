package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

public class GpsmtaProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        GpsmtaProtocolDecoder decoder = new GpsmtaProtocolDecoder(new GpsmtaProtocol());

        verifyPosition(decoder, text(
                "864528021249771 1446116686 49.85073 24.004438 0 217 6 338 00 59 27 0 0"));

        verifyPosition(decoder, text(
                "359144048138856 1442932957 49.85064 24.003979 1 0 40 0 10 110 26 0 0"));

    }

}
