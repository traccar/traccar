package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class TechtoCruzProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new TechtoCruzProtocolDecoder(null));

        verifyPosition(decoder, text(
                "$$A120,8612345678910,211005105836,A,FLEX,KCB 947C,000.0,0,-1.38047,S,36.93951,E,1648.4,243.140,21,28,12.1,3.7,0,1,0,0,0,*F6"));

        verifyNull(decoder, text(
                "$$A35,RESPO|G33|8612345678910|CRUZ,*E3"));

    }

}
