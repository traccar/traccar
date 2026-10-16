package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class SabertekFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var channel = channel(inject(new SabertekFrameDecoder()));

        verifyDecode(channel, binary("022c3939393939393939392c332c34302c36352c372c302c312c2d32352e3738313636362c32382e3235343730322c34302c3236382c313431342c382c35353632332c030d0a"),
                binary("2c3939393939393939392c332c34302c36352c372c302c312c2d32352e3738313636362c32382e3235343730322c34302c3236382c313431342c382c35353632332c"));

    }

}
