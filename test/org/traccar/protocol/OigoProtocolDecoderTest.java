package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class OigoProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        OigoProtocolDecoder decoder = new OigoProtocolDecoder(new OigoProtocol());

        verifyPosition(decoder, binary(
                "7e004200000014631000258257000000ffff02d0690e000220690e0002200696dbd204bdfde31a070000b307101135de106e05f500000000010908010402200104ffff8001"));

        verifyPosition(decoder, binary(
                "7e004200000014631000258257000000ffff02d1690e00051f690e00051f0696dbd204bdfde31a070000b307100f35c0106305f500000000010908010402200104ffff8001"));

        verifyPosition(decoder, binary(
                "7e004200000014631000258257000000ffff0d82691300001669130000160696dbd804bdfdbb1a0800000007101035a2106905f500000000010908010402200104ffff8001"));

    }

}
