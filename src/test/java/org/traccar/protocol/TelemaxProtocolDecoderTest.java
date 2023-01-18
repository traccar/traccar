package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class TelemaxProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new TelemaxProtocolDecoder(null));

        verifyNull(decoder, text(
                "%067374070128"));

        verifyPositions(decoder, text(
                "Y000007C6999999067374074649003C00A7018074666F60D66818051304321900000000C5"));

    }

}
