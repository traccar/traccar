package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Jt808FrameEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var channel = channel(new Jt808FrameEncoder());

        verifyEncode(channel, binary("7e307e087d557e"),
                binary("7e307d02087d01557e"));

    }

}
