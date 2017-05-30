package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class FlexCommProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        FlexCommProtocolDecoder decoder = new FlexCommProtocolDecoder(new FlexCommProtocol());

        verifyPosition(decoder, text(
                "7E27865067022408382201705241211301024932197006712794000910022481008234100040002C5002A2200011000000006306941827"));

    }

}
