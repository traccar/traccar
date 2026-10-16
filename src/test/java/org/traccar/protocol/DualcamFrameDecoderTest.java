package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class DualcamFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var channel = channel(inject(new DualcamFrameDecoder()));

        verifyDecode(channel, binary("000000050001403a4abaa31444000400"),
                binary("000000050001403a4abaa31444000400"));

        verifyDecode(channel, binary("00010006000000110000"),
                binary("00010006000000110000"));

    }

}
