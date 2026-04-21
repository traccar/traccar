package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class NeosProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new NeosProtocolDecoder(null));

        verifyPosition(decoder, text(
                ">12345678,1,1,070201,144111,W05829.2613,S3435.2313,,00,034,25,00,126-000,0,3,11111111*2d!\r\n"));

    }

}
