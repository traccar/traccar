package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class RadshidProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new RadshidProtocolDecoder(null));

       verifyPositions(decoder, binary(
                "01001E9BFD01E00100000000000000659D4C795151635B04DF000000130001E275E00015896CF81E04D54A052401300E0F1E020B067F000000A33D0000000000000000000000001C6B000B29A5A514A5CDD9"));

    }

}