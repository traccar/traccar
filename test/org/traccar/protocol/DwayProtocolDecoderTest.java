package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class DwayProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        DwayProtocolDecoder decoder = new DwayProtocolDecoder(new DwayProtocol());

        verifyPosition(decoder, text(
                "AA55,1,123456,1,140101,101132,22.5500,113.6770,75,70.5,320,1100,0011,1110,3950,33000,24000,12345678"));

        verifyNull(decoder, text(
                ">H12345678"));

    }

}
