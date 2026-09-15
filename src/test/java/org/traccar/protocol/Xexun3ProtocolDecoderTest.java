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
                "fc004903209e086259608092620164226aa902104049afdf3f961804401c9792e7871ba0c209999a3f0919001100000012066a18630002131c38000019ffffffffffffff6aa9021001010000d7fccf"))
                .course(0.0);

    }

}
