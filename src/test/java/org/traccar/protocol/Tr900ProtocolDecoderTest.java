package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Tr900ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new Tr900ProtocolDecoder(null);

        verifyPosition(decoder, text(
                ">00001001,4,1,150626,131252,W05830.2978,S3137.2783,,00,348,18,00,003-000,0,3,11111011*3b!"),
                position("2015-06-26 13:12:52.000", true, -31.62131, -58.50496));

        verifyPosition(decoder, text(
                ">12345678,1,1,070201,144111,W05829.2613,S3435.2313,,00,034,25,00,126-000,0,3,11111111*2d!"));

        verifyPosition(decoder, text(
                ">00001001,4,1,150626,131252,W05830.2978,S3137.2783,,00,348,18,00,003-000,0,3,11111011*3b!\r\n"));

    }

}
