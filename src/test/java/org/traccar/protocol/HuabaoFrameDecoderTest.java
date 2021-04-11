package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class HuabaoFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new HuabaoFrameDecoder();

        verifyFrame(
                binary("283734303139303331313138352c312c3030312c454c4f434b2c332c35323934333929"),
                decoder.decode(null, null, binary("283734303139303331313138352c312c3030312c454c4f434b2c332c35323934333929")));

        verifyFrame(
                binary("7e307e087d557e"),
                decoder.decode(null, null, binary("7e307d02087d01557e")));

    }

}
