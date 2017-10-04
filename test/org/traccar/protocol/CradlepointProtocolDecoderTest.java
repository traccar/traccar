package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class CradlepointProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        CradlepointProtocolDecoder decoder = new CradlepointProtocolDecoder(new CradlepointProtocol());

        verifyPosition(decoder, text(
                "353547063544681,170515,3613.25,N,11559.14,W,0.0,,,,,,,,"));

        verifyPosition(decoder, text(
                "353547060558130,170519,4337.17,N,11612.34,W,0.0,294.7,,,,,,,"));

        verifyPosition(decoder, text(
                "+12084014675,162658,4337.174385,N,11612.338373,W,0.0,,Verizon,,-71,-44,-11,,"));

    }

}
