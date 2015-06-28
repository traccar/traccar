package org.traccar.protocol;

import org.junit.Test;
import static org.traccar.helper.DecoderVerifier.verify;

public class Tr900ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Tr900ProtocolDecoder decoder = new Tr900ProtocolDecoder(new Tr900Protocol());

        verify(decoder.decode(null, null, null,
                ">00001001,4,1,150626,131252,W05830.2978,S3137.2783,,00,348,18,00,003-000,0,3,11111011*3b!"));

        verify(decoder.decode(null, null, null,
                ">12345678,1,1,070201,144111,W05829.2613,S3435.2313,,00,034,25,00,126-000,0,3,11111111*2d!"));

        verify(decoder.decode(null, null, null,
                ">00001001,4,1,150626,131252,W05830.2978,S3137.2783,,00,348,18,00,003-000,0,3,11111011*3b!\r\n"));

    }

}
