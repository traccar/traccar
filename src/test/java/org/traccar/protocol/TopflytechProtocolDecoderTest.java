package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class TopflytechProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new TopflytechProtocolDecoder(null));

        verifyPosition(decoder, text(
                "(880316890094910BP00XG00b600000000L00074b54S00000000R0C0F0014000100f0130531152205A0706.1395S11024.0965E000.0251.25"));

    }

}
