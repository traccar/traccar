package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class MotorProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new MotorProtocolDecoder(null));

        verifyPosition(decoder, text(
                "341200007E7E00007E7E020301803955352401161766210162090501010108191625132655351234567F12345F"));

    }

}
