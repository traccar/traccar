package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class MaxPbFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new MaxPbFrameDecoder();

        verifyFrame(
                binary("55aa55aa1100b0f3020000000880d0a54c10f9b7cfaf02"),
                decoder.decode(null, null, binary("55aa55aa1100b0f3020000000880d0a54c10f9b7cfaf02")));

    }

}
