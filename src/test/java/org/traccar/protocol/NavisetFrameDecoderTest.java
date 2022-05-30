package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class NavisetFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new NavisetFrameDecoder());

        verifyFrame(
                binary("1310e4073836383230343030353935383436362a060716"),
                decoder.decode(null, null, binary("1310e4073836383230343030353935383436362a060716")));

    }

}
