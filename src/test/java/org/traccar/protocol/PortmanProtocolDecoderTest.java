package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class PortmanProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new PortmanProtocolDecoder(null);

        verifyPosition(decoder, text(
                "$PTMLA,355854050074633,A,200612153351,N2543.0681W10009.2974,0,190,NA,C9830000,NA,108,8,2.66,16,GNA"));

    }

}
