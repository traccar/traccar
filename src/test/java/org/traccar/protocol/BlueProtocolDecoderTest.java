package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class BlueProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        BlueProtocolDecoder decoder = new BlueProtocolDecoder(null);

        verifyPosition(decoder, binary(
                "aa0055860080e3e79e0b840f800010320000000020010f0040008005ee197f113b26e800000000000000130c11091a2b005ac7a621120f0002000000b7000002000000000000001a3a0000000001f40000000000003f"));

    }

}
