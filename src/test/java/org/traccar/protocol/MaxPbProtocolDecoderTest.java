package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class MaxPbProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new MaxPbProtocolDecoder(null));

        verifyNull(decoder, binary(
                "55aa55aa1100b0f3020000000880d0a54c10f9b7cfaf02"));

        verifyPositions(decoder, binary(
                "55aa55aaa800eee7010000000880d0a54c10df9cbc9f0f22940108b6b706107f18dfd6b4c7052215080010011d388a16f4253827cee528cb09300838002a131003180a280b301a381c40861e48015002582a3210080112060801200138011a04080218023a2b0a1b080110ad731802200028af1e3000380148f7fa0150f41f5884d404120608011000200d1a0408001000421b0a0c0800100018002002288ad22e1205080010a8241a0408001000"));

    }

}
