package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class EskyProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        EskyProtocolDecoder decoder = new EskyProtocolDecoder(new EskyProtocol());

        verifyPosition(decoder, text(
                "EO;0;864906029196626;R;0+170808155352+0.00000+0.00000+0.00+0+0x1+0+0+0+1233"));

    }

}
