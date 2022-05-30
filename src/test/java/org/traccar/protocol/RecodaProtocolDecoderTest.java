package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class RecodaProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new RecodaProtocolDecoder(null));

        verifyNull(decoder, binary(
                "01100020480000000300000030393535360000000000000001000000303030303000000000000000000000000000000000000000006100004531313037353500ffffffffffff0000"));

        verifyNull(decoder, binary(
                "01200020100000000300000002000000"));

        verifyNull(decoder, binary(
                "0110000008000000"));

    }

}
