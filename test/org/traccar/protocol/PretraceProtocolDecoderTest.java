package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class PretraceProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        PretraceProtocolDecoder decoder = new PretraceProtocolDecoder(new PretraceProtocol());

        verifyPosition(decoder, text(
                "(867967021915915U1110A1701201500102238.1700N11401.9324E000264000000000009001790000000,&P11A4,F1050^47"));

    }

}
