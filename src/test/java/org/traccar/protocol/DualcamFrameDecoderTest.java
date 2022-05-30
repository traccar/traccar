package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class DualcamFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new DualcamFrameDecoder());

        verifyFrame(
                binary("000000050001403a4abaa31444000400"),
                decoder.decode(null, null, binary("000000050001403a4abaa31444000400")));

        verifyFrame(
                binary("00010006000000110000"),
                decoder.decode(null, null, binary("00010006000000110000")));

    }

}
