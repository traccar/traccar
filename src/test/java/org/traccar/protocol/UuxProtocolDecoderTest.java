package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class UuxProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new UuxProtocolDecoder(null);

        verifyAttributes(decoder, binary(
                "81918c2d9e31395533443630363631041c0c16043030313030300007000000000000000000000000000000000000000000"));

    }

}
