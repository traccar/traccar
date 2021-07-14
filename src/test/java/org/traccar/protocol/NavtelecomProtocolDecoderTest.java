package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class NavtelecomProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new NavtelecomProtocolDecoder(null);

        verifyNull(decoder, binary(
                "404e5443010000000000000013004e452a3e533a383636373935303331343130363839"));

    }

}
