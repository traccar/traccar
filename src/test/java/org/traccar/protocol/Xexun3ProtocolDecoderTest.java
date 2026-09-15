package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Xexun3ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Xexun3ProtocolDecoder(null));

        verifyPosition(decoder, binary(
                "fc005c032014086259608092620164226aa8261dffffffffffffffffffffffffffffffff00000637004b000641000006913c66116aa8261d01060003000091c400ce5421ff6a1863000116061d000008ffffffffffffff6aa8261d01010000bbfccf"),
                position("2026-09-14 16:51:41.000", false, 0, 0));

        verifyNull(decoder, binary(
                "fc000b03200108610450803870158318cf"));

        verifyPosition(decoder, binary(
                "fc0040032006086104508038701564216913f223403693012f635344405c829142b302f7427f33331a2e000000a40011046a1055ffff1f0000000000ffffff04ff09ff1a30cf"));

    }

}
