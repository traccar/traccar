package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Jt808FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var channel = channel(inject(new Jt808FrameDecoder()));

        verify(channel, binary("283734303139303331313138352c312c3030312c454c4f434b2c332c35323934333929"),
                binary("283734303139303331313138352c312c3030312c454c4f434b2c332c35323934333929"));

        verify(channel, binary("7e307d02087d01557e"),
                binary("7e307e087d557e"));

        verify(channel, binary("24247e307d02087d01557e"),
                binary("7e307e087d557e"));

    }

}
