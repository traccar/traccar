package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class RoboTrackFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var channel = channel(inject(new RoboTrackFrameDecoder()));

        verify(channel, binary("00524f424f545241434b00000000000000383638323034303032323533343136313233343536373839303132312e313261000000312e353761000000312e3030000000003e"),
                binary("00524f424f545241434b00000000000000383638323034303032323533343136313233343536373839303132312e313261000000312e353761000000312e3030000000003e"));

    }

}
