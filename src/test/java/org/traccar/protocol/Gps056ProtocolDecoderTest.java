package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Gps056ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new Gps056ProtocolDecoder(null);

        verifyNull(decoder, buffer(
                "$$25LOGN_118624620337829462.1#"));

        verifyPosition(decoder, binary(
                "242435314750534C5F30323836333037313031353034353834391D0A0E091A0A0B1112C34E1E23230A45FF00000000000000000000000023"));

        verifyAttributes(decoder, binary(
                "2424323853594E435F313138363330373130313530343538343900000000000023"));

    }

}
