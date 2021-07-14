package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class BlackKiteProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new BlackKiteProtocolDecoder(null);

        verifyNull(decoder, binary(
                "01150003313131313131313131313131313131209836055605BA"));
        
        verifyPositions(decoder, binary(
                "0136000331313131313131313131313131313120523905563000010000000100000033000000003400004000004500004600005000005100009F76"));

    }

}
