package org.traccar.protocol;

import java.nio.ByteOrder;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class BlackKiteProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        BlackKiteProtocolDecoder decoder = new BlackKiteProtocolDecoder(new BlackKiteProtocol());

        verifyNothing(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "01150003313131313131313131313131313131209836055605BA"));
        
        verifyPositions(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "0136000331313131313131313131313131313120523905563000010000000100000033000000003400004000004500004600005000005100009F76"));

    }

}
