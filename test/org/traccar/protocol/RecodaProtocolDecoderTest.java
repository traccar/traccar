package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

import java.nio.ByteOrder;

public class RecodaProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        RecodaProtocolDecoder decoder = new RecodaProtocolDecoder(new RecodaProtocol());

        verifyNull(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "01100020480000000300000030393535360000000000000001000000303030303000000000000000000000000000000000000000006100004531313037353500ffffffffffff0000"));

        verifyNull(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "01200020100000000300000002000000"));

        verifyNull(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "0110000008000000"));

    }

}
