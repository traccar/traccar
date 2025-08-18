package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class CautelaProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new CautelaProtocolDecoder(null));

        verifyPosition(decoder, text(
                "20,010907000000,14,02,18,16.816667,96.166667,1325,S,*2E"));

    }

}
