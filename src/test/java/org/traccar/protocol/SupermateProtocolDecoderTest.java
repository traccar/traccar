package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class SupermateProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new SupermateProtocolDecoder(null));

        verifyPosition(decoder, text(
                "2:359672050130411:1:*,00000000,XT,A,10031b,140b28,80ad4c72,81ba2d2c,06ab,238c,020204010000,12,0,0000,0003e6"));

        verifyPosition(decoder, text(
                "2:359672050130411:2:*,00000000,UP,A,10031b,140a1c,80ad4bf6,81ba2dc3,0000,0000,020204000000,14,0,0000,0003e6"));

        verifyPosition(decoder, text(
                "2:359672050130411:1:*,00000000,BJ,A,10031b,140c2f,80ad5012,81ba1f27,0f4c,2e18,020204014000,14,0,0000,0003ed"));

    }

}