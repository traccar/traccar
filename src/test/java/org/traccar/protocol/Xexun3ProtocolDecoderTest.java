package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Xexun3ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Xexun3ProtocolDecoder(null));

        verifyNull(decoder, binary(
                "fc000b03200108610450803870158318cf"));

        verifyPosition(decoder, binary(
                "fc0040032006086104508038701564216913f223403693012f635344405c829142b302f7427f33331a2e000000a40011046a1055ffff1f0000000000ffffff04ff09ff1a30cf"));

        verifyPosition(decoder, binary(
                "fc004903203e086259608092620164226aa936754049aff0ff6a6735401c97df94ded82041ab3333400d1c00000000000d006a18630000147e0e0007fdffffffffffffff6aa9367501010000a066cf"))
                .course(1.3);

    }

}
