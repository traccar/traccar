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
                "fc00490320e8086259608092620164226aa92f624049b01fff79c842401c97788f16414443084ccd400e19001c009f000e006a18630000173a2f0001a3ffffffffffffff6aa92f622101000030f4cf"),
                position("2026-09-15 11:43:30.000", true, 51.37597649999999, 7.147920833333334));

    }

}
