package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class RoboTrackProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new RoboTrackProtocolDecoder(null));

        verifyNull(decoder, binary(
                "00524f424f545241434b00000000000000383638323034303032323533343136313233343536373839303132312e313261000000312e353761000000312e3030000000003e"));

        verifyPosition(decoder, binary(
                "03e020bb5a034409034862120210a9e105000000000000b9"));

        verifyPosition(decoder, binary(
                "03f120bb5a30460903426312021798e105000000000000cd"));

    }

}
