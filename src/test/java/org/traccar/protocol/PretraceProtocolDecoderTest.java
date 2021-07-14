package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class PretraceProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new PretraceProtocolDecoder(null);

        verifyPosition(decoder, text(
                "(867967021915915U1110A1701201500102238.1700N11401.9324E000264000000000009001790000000,&P11A4,F1050^47"));

        verifyPosition(decoder, text(
                "(864244029498838U1110A1509250653072238.1641N11401.9213E000196000000000406002990000000,&P195%,T1050,F14A5,R104C51E47B^30"));

    }

}
