package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class EgtsFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new EgtsFrameDecoder();

        verifyFrame(
                binary("0100020B0025003A5701C91A003A5701CD6E68490202101700CBB4740F7617FD924364104F116A0000000000010300001EC2"),
                decoder.decode(null, null, binary("0100020B0025003A5701C91A003A5701CD6E68490202101700CBB4740F7617FD924364104F116A0000000000010300001EC2")));

        verifyFrame(
                binary("0100000b000300704300db0500006c27"),
                decoder.decode(null, null, binary("0100000b000300704300db0500006c270100000b0003007143009d0600003c7e")));
    }

}
