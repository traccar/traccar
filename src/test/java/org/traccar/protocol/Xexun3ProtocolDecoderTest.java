package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Xexun3ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Xexun3ProtocolDecoder(null));

        verifyDecode(decoder, binary(
                "fc005c032014086259608092620164226aa8261dffffffffffffffffffffffffffffffff00000637004b000641000006913c66116aa8261d01060003000091c400ce5421ff6a1863000116061d000008ffffffffffffff6aa8261d01010000bbfccf"),
                position().location("2026-09-14T16:51:41.000Z", false, 0, 0));

        verifyDecode(decoder, binary(
                "fc000b03200108610450803870158318cf"));

        verifyDecode(decoder, binary(
                "fc00490320e8086259608092620164226aa92f624049b01fff79c842401c97788f16414443084ccd400e19001c009f000e006a18630000173a2f0001a3ffffffffffffff6aa92f622101000030f4cf"),
                position().location("2026-09-15T11:43:30.000Z", true, 51.37597, 7.14792));

        verifyDecode(decoder, binary(
                "fc00d003200108625960809262016a1863001120ff000006daffffff020000ff6abeacda000000006ea91341414132363039323330337c41414132363039323330327c5030320000000000898830300001364674357d4141412c4137363730452d4c4153435f4131323442303241373637304d362c415436353538522d354e2d33322d31433538303930315f5552414e5553352056352e332e322e302c747261636361722e63727970746f6e6f64652e64653a3532363440322c2c3239353035303930383031313338392c2c302c3236323033795acf"),
                position(Checks.ATTRIBUTES).deviceTime("2026-10-01T18:56:26.000Z"));

    }

}
