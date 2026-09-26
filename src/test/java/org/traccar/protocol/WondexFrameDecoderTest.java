package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class WondexFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var channel = channel(inject(new WondexFrameDecoder()));

        verifyDecode(channel, binary("f0d70b0001ca9a3b"));

        channel = channel(inject(new WondexFrameDecoder()));

        verifyDecode(channel, binary("313034343938393630312c32303133303332333039353531352c31332e3537323737362c35322e3430303833382c302c3030302c37322c302c320d0a"),
                binary("313034343938393630312c32303133303332333039353531352c31332e3537323737362c35322e3430303833382c302c3030302c37322c302c32"));

        verifyDecode(channel, binary("d0d70b0001ca9a3b"),
                binary("d0d70b0001ca9a3b"));

    }

}
