package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class KenjiProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new KenjiProtocolDecoder(null));

        verifyPosition(decoder, text(
                ">C800000,M005004,O0000,I0002,D124057,A,S3137.2783,W05830.2978,T000.0,H254.3,Y240116,G06*17"),
                position("2016-01-24 12:40:57.000", true, -31.62131, -58.50496));
    }    

}
