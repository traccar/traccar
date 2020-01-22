package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class BlueProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        BlueProtocolDecoder decoder = new BlueProtocolDecoder(null);

        verifyPosition(decoder, binary(
                "aa00550000813f6f840b840380001032000000002001030040008005ee1938113b26f300000000000000140114082833044d27602112030002000000b70000020000000000000000650000001601f4000000000000e4"));

        verifyPosition(decoder, binary(
                "aa0055860080e3e79e0b840f800010320000000020010f0040008005ee197f113b26e800000000000000130c11091a2b005ac7a621120f0002000000b7000002000000000000001a3a0000000001f40000000000003f"));

    }

}
