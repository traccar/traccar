package org.traccar.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Xexun3FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var channel = channel(new LengthFieldBasedFrameDecoder(1024, 1, 2, 3, 0));

        verify(channel, binary("fc0040032006086104508038701564216913f223403693012f635344405c829142b302f7427f33331a2e000000a40011046a1055ffff1f0000000000ffffff04ff09ff1a30cf"),
                binary("fc0040032006086104508038701564216913f223403693012f635344405c829142b302f7427f33331a2e000000a40011046a1055ffff1f0000000000ffffff04ff09ff1a30cf"));

    }

}
