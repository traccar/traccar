package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Xexun2FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var channel = channel(inject(new Xexun2FrameDecoder()));

        verifyDecode(channel, binary("faaf0014000286147503139003400032f2b001002f4260b0d6a0008019104a3378323130333135317c323130333132303100704020308715758089502023015648643670faaf"),
                binary("faaf0014000286147503139003400032f2b001002f4260b0d6a0008019104a3378323130333135317c323130333132303100704020308715758089502023015648643670faaf"));

        verifyDecode(channel, binary("FAAF123456FBBF01123456FBBF02123456FAAF"),
                binary("FAAF123456FAAF123456FBBF123456FAAF"));

    }

}
