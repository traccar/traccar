package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

import static org.junit.Assert.assertEquals;

public class SabertekFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new SabertekFrameDecoder();

        assertEquals(
                binary("2c3939393939393939392c332c34302c36352c372c302c312c2d32352e3738313636362c32382e3235343730322c34302c3236382c313431342c382c35353632332c"),
                decoder.decode(null, null, binary("022c3939393939393939392c332c34302c36352c372c302c312c2d32352e3738313636362c32382e3235343730322c34302c3236382c313431342c382c35353632332c030d0a")));

    }

}
