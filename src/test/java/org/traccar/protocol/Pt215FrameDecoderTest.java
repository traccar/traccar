package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Pt215FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Pt215FrameDecoder decoder = new Pt215FrameDecoder();

        verifyFrame(
                binary("58580d010359339075799656010d0a"),
                decoder.decode(null, null, binary("58580d010359339075799656010d0a")));

        verifyFrame(
                binary("5858071340010819640d0a"),
                decoder.decode(null, null, binary("5858071340010819640d0a")));

        verifyFrame(
                binary("585815101309160d0f0c9902b7015405f0e82404347afff7000d0a"),
                decoder.decode(null, null, binary("585815101309160d0f0c9902b7015405f0e82404347afff7000d0a")));

    }

}
