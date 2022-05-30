package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class JidoProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new JidoProtocolDecoder(null));

        verifyPosition(decoder, text(
                "*12345678910101000,01,A,130517,160435,1820.5845,N,07833.2478,E,20,0,067,045,05,28,26,1,075,Y,1,0000,0000,0000,59"));

        verifyPosition(decoder, text(
                "*12345678910101000,03,130517,160435,1820.5845,N,07833.2478,E,1,58"));

    }

}
