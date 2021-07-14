package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class ThinkRaceProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new ThinkRaceProtocolDecoder(null);

        verifyNull(decoder, binary(
                "48415349483031343730303134382C8000100134363030303134363139363239343806FF"));

        verifyPosition(decoder, binary(
                "48415349483031343730303134382C90001755701674D70155466406CBB813003D24A410F5000000770B4C"));

    }

}
