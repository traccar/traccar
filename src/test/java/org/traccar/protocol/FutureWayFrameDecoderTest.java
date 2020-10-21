package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class FutureWayFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        FutureWayFrameDecoder decoder = new FutureWayFrameDecoder();

        verifyFrame(
                binary("34313030303030303346323030303032302c494d45493a3335343832383130303132363436312c62617474657279206c6576656c3a362c6e6574776f726b20747970653a372c4353513a323336463432"),
                decoder.decode(null, null, binary("34313030303030303346323030303032302c494d45493a3335343832383130303132363436312c62617474657279206c6576656c3a362c6e6574776f726b20747970653a372c4353513a323336463432")));

    }

}
