package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class SnapperFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var channel = channel(inject(new SnapperFrameDecoder()));

        verifyDecode(channel, binary("4b0341a6b0c608000040000005000000000000007d5e14010068656c6c6f"),
                binary("4b0341a6b0c608000040000005000000000000007d5e14010068656c6c6f"));

        verifyDecode(channel, binary("5012"),
                binary("5012"));

    }

}
