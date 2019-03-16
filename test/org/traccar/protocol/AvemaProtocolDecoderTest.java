package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class AvemaProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        AvemaProtocolDecoder decoder = new AvemaProtocolDecoder(null);

        verifyPosition(decoder, text(
                "1130048939,20120224000129,121.447487,25.168025,0,0,0,0,3,0.0,1,0.02V,14.88V,0,1,24,4,46608,F8BC,F9AD,CID0000028"));

    }

}
