package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class AvemaProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        AvemaProtocolDecoder decoder = new AvemaProtocolDecoder(null);

        verifyNull(decoder, text(
                "8,20180927150956,19.154864,49.124862,7,56,0,12,3,0.0,0,0.02,14.01,0,0,26,0,219-2,65534,10255884,0.01"));

        verifyPosition(decoder, text(
                "1130048939,20120224000129,121.447487,25.168025,0,0,0,0,3,0.0,1,0.02V,14.88V,0,1,24,4,46608,F8BC,F9AD,CID0000028"));

    }

}
