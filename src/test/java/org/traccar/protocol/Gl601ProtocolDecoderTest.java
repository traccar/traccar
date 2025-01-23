package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Gl601ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Gl601ProtocolDecoder(null));

        verifyPositions(decoder, binary(
                "2b000048000861261070000944c031000c000031677e5c900049120052162900ed28bf02b5fcdf677e5c90000c0701610005e211550e0500559609010000314d800212630043d724"));

        verifyPositions(decoder, false, binary(
                "2d000027000861261070000944c031000c000010677e5b50004120026105010f404a0000453a24"));

    }

}
