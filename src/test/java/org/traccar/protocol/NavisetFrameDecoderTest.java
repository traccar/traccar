package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class NavisetFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var channel = channel(inject(new NavisetFrameDecoder()));

        verifyDecode(channel, binary("1310e4073836383230343030353935383436362a060716"),
                binary("1310e4073836383230343030353935383436362a060716"));

    }

}
