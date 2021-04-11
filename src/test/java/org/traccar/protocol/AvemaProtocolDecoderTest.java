package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class AvemaProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new AvemaProtocolDecoder(null);

        verifyAttribute(decoder, text(
                "1000000000,20190527072358,121.646024,25.062135,0,0,0,0,10,0.0,1,0.02,12.32,0,0,15,2,466-5,10275,0,0.01,65EB812A000104E0,8000001234,NormanChang"),
                Position.KEY_DRIVER_UNIQUE_ID, "65EB812A000104E0");

        verifyNotNull(decoder, text(
                "1000000000,20190522093835,121.645898,25.062268,0,0,0,0,3,0.0,1,0.02,11.48,0,0,19,4,466-5,65534,56589841,0.01"));

        verifyNotNull(decoder, text(
                "8,20180927150956,19.154864,49.124862,7,56,0,12,3,0.0,0,0.02,14.01,0,0,26,0,219-2,65534,10255884,0.01"));

        verifyPosition(decoder, text(
                "1130048939,20120224000129,121.447487,25.168025,0,0,0,0,3,0.0,1,0.02V,14.88V,0,1,24,4,46608,F8BC,F9AD,CID0000028"));

    }

}
