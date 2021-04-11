package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class VnetProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new VnetProtocolDecoder(null);

        verifyNull(decoder, binary(
                "24240000140029111909062986818303379282604c452e322e30302ea32b020f0000d3552323"));

        verifyPosition(decoder, binary(
                "242433001200290615174213211489861061060690070B0001020304700005001E382323"));

    }

}
