package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Xexun2FrameEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var channel = channel(new Xexun2FrameEncoder());

        verifyEncode(channel, binary("FAAF123456FAAF123456FBBF123456FAAF"),
                binary("FAAF123456FBBF01123456FBBF02123456FAAF"));

    }

}
