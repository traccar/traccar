package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

import java.nio.ByteOrder;

public class AdmProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        AdmProtocolDecoder decoder = new AdmProtocolDecoder(new AdmProtocol());

        verifyNull(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "010042033836313331313030323639343838320501000000000000000000000000000000000000000000000000000000000000000000000000000000000000000073"));

        verifyPosition(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "01002680336510002062A34C423DCF8E42A50B1700005801140767E30F568F2534107D220000"));

        verifyPosition(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "010022003300072020000000000000000044062A330000000000107F10565D4A8310"));

        verifyPosition(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "0100268033641080207AA34C424CCF8E4239030800005B01140755E30F560000F00F70220000"));

        verifyPosition(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "01002680336510002062A34C423DCF8E42A50B1700005801140767E30F568F2534107D220000"));

        verifyPosition(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "01002200333508202000000000000000007F0D9F030000000000E39A1056E24A8210"));

    }

}
