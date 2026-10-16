package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class TeltonikaFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var channel = channel(inject(new TeltonikaFrameDecoder()));

        verifyDecode(channel, binary("FF000F313233343536373839303132333435"),
                binary("ff"),
                binary("000F313233343536373839303132333435"));

        verifyDecode(channel, binary("000F313233343536373839303132333435"),
                binary("000F313233343536373839303132333435"));

    }

}
