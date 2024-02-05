package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VtfmsFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new VtfmsFrameDecoder());

        assertEquals(
                buffer("(863071010087648,0HK44,00,000,14,2,9,,A,114946,180313,11.0244,076.9768,282,000,00000,00000,K,0000128,1,12.8,,200,2.501,,4.001,0,0,0,0,0,0,0,,)105"),
                decoder.decode(null, null, buffer("(863071010087648,0HK44,00,000,14,2,9,,A,114946,180313,11.0244,076.9768,282,000,00000,00000,K,0000128,1,12.8,,200,2.501,,4.001,0,0,0,0,0,0,0,,)105")));

    }

}
