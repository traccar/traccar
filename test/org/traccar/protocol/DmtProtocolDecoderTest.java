package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

import java.nio.ByteOrder;

public class DmtProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        DmtProtocolDecoder decoder = new DmtProtocolDecoder(new DmtProtocol());

        verifyNothing(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "025500310038f90100333533333233303831363639373330003839363130313435363839393333303030303835002202010900000000"));

        verifyPositions(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "0255043D003D004746000096D684020B001502D48402F043F4EC2A6909452B001F00050011230302080000000000000A00060F041D0001FE0F021E0005000003BF08"));

    }

}
